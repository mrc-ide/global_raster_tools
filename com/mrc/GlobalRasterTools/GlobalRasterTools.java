package com.mrc.GlobalRasterTools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class contains a set of methods for handling rasteration of landscan-like population datasets, with ESRI
 * (GADM) Shapefiles, including rasterising with contention-handling, look-up, and finding centroids. It uses
 * Java's polygon filling algorithms, which seem fairly efficient - at the cost of using a lot of RAM.
 *  
 * @author      Wes Hinsley
 * @version     0.1
 * @since       0.1
*/

public class GlobalRasterTools {

  private BufferedImage buffer = null;
  private Graphics2D g2d = null;
  public ArrayList<String> unit_names = new ArrayList<String>();
  public ArrayList<String> unit_numbers = new ArrayList<String>();
  public ArrayList<ArrayList<GlobalRasterTools.DPolygon>> unit_shapes = new ArrayList<ArrayList<GlobalRasterTools.DPolygon>>();
  public int[][] map = null;
  
  /**
   * The width of the global raster in pixels
   */
  public static final int WIDTH = 43200;
  /**
   * The height of the global raster in pixels
   */
  public static final int HEIGHT = 21600;
  /**
   * The expected resolution of population data in pixels per degree.
   */
  public static final double RESOLUTION = 120.0;
  /**
   * The internal resolution used for rasterising polygons in pixels per degree. This is used partly to reduce the size of really detailed polygons to something
   * more manageable, and also is the resolution used for resolving contention, where the shape with the most area is assumed to "own" that pixel.
   */
  public static final int INT_SCALER = 2400;
  
  
  
  public GlobalRasterTools() {}
  
  
  /**
   * Clear image buffer and garbage collect. 
   */
  public void clearBuffer() {
    if (buffer!=null) {
      g2d = null;
      buffer = null;
      System.gc();
    }
  }
  
  /**
   * Prepare black canvas for the polygons to be rasteriseed onto.
   */
  
  public void initBuffer() {
    if (buffer!=null) {
      clearBuffer();
    }
    buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    g2d = (Graphics2D) buffer.getGraphics();
    g2d.setColor(Color.BLACK);
    g2d.fillRect(0,0,WIDTH, HEIGHT);
  }
  
  /**
   * A polygon with clockwise/anti-clockwise direction of points recorded. 
   * Anti-clockwise polygons are often marked as exclusions in shape files (for example, excluding Lesotho from the South Africa polygon). Need to remember that 
   * since latitude is more negative as you move North, the shapes get flipped when rasterised, and this also flips the direction of points. 
   */
  public class DPolygon extends Polygon {
    private static final long serialVersionUID = 1L;
    boolean clockwise;
  }
  
  /**
   * Convert longitude into a pixel number. where 0 is the pixel starting at -180 degrees.
   * <p>
   *
   * @param  x    Longitude
   * @return The x-index of the pixel in which the longitude falls, where 0 is the pixel with left-boundary -180 degrees.
   */
  
  static int getDefaultXForLon(double x) {
    return (int) Math.floor((x + 180) * RESOLUTION);
  }

  /**
   * Convert latitude into a pixel number.
   * <p>
   *
   * @param  y    Latitude
   * @return The y-index of the pixel in which the latitude falls, where 0 is the Northern-most possible pixel.
   */
  static int getDefaultYForLat(double y) {
    return (WIDTH-1) + (int) ((-90 - y) * RESOLUTION);
  }

  
  public void loadPolygonFolder(String folder, int max_level) throws Exception {
    loadPolygonFolder(folder, max_level, null);
  }
  
  public void loadPolygonFolder(String folder, int max_level, List<String> countries) throws Exception {
    File[] fs = new File(folder).listFiles();
    for (int i = 0; i < fs.length; i++) {
      if (fs[i].getName().indexOf("adm0.shp") > 0) {
        // Find most detailed shapes, <= max_level
        int level = 0;
        String best = fs[i].getPath();
        String search = best.replace("adm" + String.valueOf(level), "adm" + String.valueOf(1 + level));
        while ((new File(search).exists()) && (level < max_level)) {
          best = search;
          level++;
          search = best.replace("adm" + String.valueOf(level), "adm" + String.valueOf(1 + level));
        }
        
        System.out.println("Loading " + best);
        loadPolygons(level, best, best.substring(0, best.length() - 3) + "dbf", countries);
      }
    }
 
    for (int j = 0; j < unit_names.size(); j++) {
      String s = unit_names.get(j);
      String[] bits = s.split("\t");
      String s2 = unit_numbers.get(j);
      if (bits.length <= max_level) {
        while (bits.length <= max_level) {
          s += "\t" + bits[bits.length - 1];
          bits = s.split("\t");
          s2 += "\t1";
        }
        unit_numbers.set(j, s2);
        unit_names.set(j, s);
      }
    }
  }  
  
  public void clearPolygons() {
    unit_names.clear();
    unit_numbers.clear();
    for (int i=0; i<unit_shapes.size(); i++) {
      unit_shapes.get(i).clear();
    }
    unit_shapes.clear();
  }
  
  /**
   * Load ESRI shapefiles at a given admin unit level
   * <p>
   *
   * @param  level    The maximum admin unit-level for which to load shapes.
   * @param  shpFile  Path to the .shp file to load
   * @param  dbfFile  Path to the .dbf file to load
   * @param  countries List of GADM country names to filter by. (Or set to null for no filter). 
   * @throws Exception for any file-io related exceptions.
   */
  public void loadPolygons(int level, String shpFile, String dbfFile, List<String> countries) throws Exception {
    
    DataInputStream dbf = new DataInputStream(new BufferedInputStream(new FileInputStream(dbfFile)));
    DataInputStream shp = new DataInputStream(new BufferedInputStream(new FileInputStream(shpFile)));
    ArrayList<String> f_names = new ArrayList<String>();
    ArrayList<Character> f_types = new ArrayList<Character>();
    ArrayList<Integer> f_lengths = new ArrayList<Integer>();
    /*byte file_type = */ dbf.readByte();
    /*byte mod_yy = */ dbf.readByte();
    /*byte mod_mm = */ dbf.readByte();
    /*byte mod_dd = */ dbf.readByte();
    /*int no_recs = */ Integer.reverseBytes(dbf.readInt());
    /*short first_rec = */ Short.reverseBytes(dbf.readShort());
    int rec_length = Short.reverseBytes(dbf.readShort());
    for (int i = 0; i < 16; i++) dbf.readByte();
    /*byte flags = */ dbf.readByte();
    /*byte codepage = */ dbf.readByte();
    for (int i = 0; i < 2; i++) dbf.readByte();
    char starter = (char) dbf.readByte();
    while (starter != 0x0D) {
      char[] title = new char[11];
      title[0] = (starter == 0) ? ' ' : (char) starter;
      for (int i = 1; i <= 10; i++) {
        char ch = (char) dbf.readByte();
        title[i] = (ch == 0) ? ' ' : ch;
      }
      char type = (char) dbf.readByte();
      /*int displ = */Integer.reverseBytes(dbf.readInt());
      int field_length = dbf.readByte();
      if (field_length < 0) field_length = 256 + field_length;
      /*byte dec_places = */ dbf.readByte();
      /*byte field_flags = */ dbf.readByte();
      /*int auto_incr_next = */ Integer.reverseBytes(dbf.readInt());
      /*int auto_incr_step = */ dbf.readByte();
      dbf.readDouble(); // Skip 8 bytes
      f_names.add(new String(title));
      f_types.add(new Character(type));
      f_lengths.add(new Integer(field_length));
      starter = (char) dbf.readByte();
    }
   

    /*int file_code = */ shp.readInt(); // File code - hex value 0x0000270a = 9994 decimal
    for (int i = 0; i < 5; i++) shp.readInt(); // 5 unused bytes
    int file_size = 2 * shp.readInt(); // File size obviously - but measured in 16-bit words. I double it for bytes.
    /* int version = */ Integer.reverseBytes(shp.readInt()); // Version. Who knows why little-endian takes over here...
    /* int shape_type = */ Integer.reverseBytes(shp.readInt()); // Shape type;
    /* double min_x = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    /* double min_y = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    /* double max_x = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    /* double max_y = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    /* double min_z = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    /* double max_z = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    /* double min_m = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    /* double max_m = */ Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
    
    int file_pointer = 100; // Header is exactly 100 bytes. 9 ints, 6 doubles = 9*4 + 8*8 = 36 + 64 = 100

    while (file_pointer < file_size) {
      // Read record header
      /*byte del = */dbf.readByte();
      String[] entry_names = new String[level+1];
      String[] entry_nums = new String[level+1];
      for (int j = 0; j < f_names.size(); j++) {
        char type = f_types.get(j).charValue();
        int length = f_lengths.get(j).intValue();
        if ((type == 'C') || (type == 'F') || (type == 'N') || (type == 'D')) {
          byte[] string = new byte[length];
          for (int k = 0; k < length; k++) {
            byte ch = dbf.readByte();
            string[k] = ch;
          }
          String utf_string = new String(string, Charset.forName("UTF-8"));
          utf_string=utf_string.replace("\r", " ");
          utf_string=utf_string.replace("\n", " ");          
          
          for (int k=0; k<=level; k++) {
            if (f_names.get(j).trim().equals("ID_"+String.valueOf(k))) {
              entry_nums[k]=utf_string.trim();
            }
            if (f_names.get(j).trim().equals("NAME_"+String.valueOf(k))) {
              entry_names[k]=utf_string.trim();
            }
            if (f_names.get(j).trim().equals("NAME_ENGLI") && (k==0)) {
              entry_names[k]=utf_string.trim();
            }
            
          }
        } 
      }

      /*int rec_no = */ shp.readInt();
      rec_length = shp.readInt() * 2; // Again, I just prefer bytes to 16-bit words.
      file_pointer += 8;

      // Actual record

      int rec_pointer = 0; // Keep track of progress through variable length record
      while (rec_pointer < rec_length) {
        int rec_shape_type = Integer.reverseBytes(shp.readInt());
        rec_pointer += 4;
        file_pointer += 4;

        if (rec_shape_type == 5) { // Polygon. MBR, Number of parts, Number of points, Parts, Points
          // double rec_min_x =
          Double.longBitsToDouble(Long.reverseBytes(shp.readLong())); // Bounding box comes next
          // double rec_min_y =
          Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
          // double rec_max_x =
          Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
          // double rec_max_y =
          Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
          rec_pointer += 32;
          file_pointer += 32;


          int no_parts = Integer.reverseBytes(shp.readInt()); // Number of separate "polygons"
          int no_points = Integer.reverseBytes(shp.readInt()); // Number of points
          
          rec_pointer += 8;
          file_pointer += 8;

          int[] part_start = new int[no_parts];
          for (int i = 0; i < no_parts; i++) part_start[i] = Integer.reverseBytes(shp.readInt()); // Indexes - first point.

          rec_pointer += (4 * no_parts);
          file_pointer += (4 * no_parts);
          ArrayList<DPolygon> polys = new ArrayList<DPolygon>();

          for (int i = 0; i < no_parts; i++) {
            int no_points_in_part = 0;
            if (i < no_parts - 1) no_points_in_part = part_start[i + 1] - part_start[i];
            else no_points_in_part = no_points - part_start[i];
            DPolygon dpoly = new DPolygon();
            for (int j = 0; j < no_points_in_part; j++) {
              double p_x = Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
              double p_y = Double.longBitsToDouble(Long.reverseBytes(shp.readLong()));
              int p_x_int = (int) Math.round(INT_SCALER * p_x);
              int p_y_int = (int) Math.round(INT_SCALER * p_y);
              if (dpoly.npoints == 0) dpoly.addPoint(p_x_int, p_y_int);
              else if ((p_x_int != dpoly.xpoints[dpoly.npoints - 1]) || (p_y_int != dpoly.ypoints[dpoly.npoints - 1])) dpoly.addPoint(p_x_int, p_y_int);
              rec_pointer += 16;
              file_pointer += 16;
            }
            double sum = ((360.0 + dpoly.xpoints[0]) - (360.0 + dpoly.xpoints[dpoly.npoints- 1])) * (360.0 + dpoly.ypoints[0] + dpoly.ypoints[dpoly.npoints - 1]);
            for (int j = 1; j < dpoly.npoints; j++) {
              sum += ((360.0 + dpoly.xpoints[j]) - (360.0 + dpoly.xpoints[j - 1])) * (360.0 + dpoly.ypoints[j] + dpoly.ypoints[j - 1]);
            }
            dpoly.clockwise = (sum > 0); // Normally would be other way round, but our
                                         // y-axis is inverted.
            
            polys.add(dpoly);
          }
          if ((countries==null) || (countries.contains(entry_names[0]))) { 
            unit_shapes.add(polys);
            unit_names.add(String.join("\t", entry_names));
            unit_numbers.add(String.join("\t",entry_nums));
          } else System.out.println("Skipping unwanted "+entry_names[0]);
          
        } else if (rec_shape_type == 0) {

        } else System.out.println("Shape type " + rec_shape_type + " not implemented");
      }
    }
    shp.close();
    dbf.close();
  }
  
  /**
   * Rasterise one admin unit (ie, one list of polygons)
   * <p>
   * Longer description. If there were any, it would be
   * here.
   * <p>
   * And even more explanations to follow in consecutive
   * paragraphs separated by HTML paragraph breaks.
   *
   * @param  polys              The list of polygons to rasterise
   * @param  unit_ids           Id of polygon in each pixel of final image.
   * @param  contention_indexes Pointer into contentions array, if this pixel is claimed by more than one admin unit.
   * @param  contentions        For each pixel where contention occurs, list the unit ids that were claiming that pixel.
   * @param  code               The id of the admin unit being rasterised 
   * @param  null_unit          The id that represents NULL (pixels which no shape file has claimed)  
   */
  public void rasterisePoly(ArrayList<DPolygon> polys, int[][] unit_ids, int[][] contention_indexes, ArrayList<ArrayList<Integer>> contentions, int code, int null_unit) {
    if (buffer==null) {
      initBuffer();
    }
    
    // Find bounds of this shape first.
    int maxx = -999;
    int maxy = -999;
    int minx = 50000;
    int miny = 50000;
    int black = Color.black.getRGB();
    g2d.setColor(Color.WHITE);
    for (int i = 0; i < polys.size(); i++) {
      DPolygon p = polys.get(i);
      if (p.clockwise) {
        DPolygon p2 = new DPolygon();
        for (int j = 0; j < p.npoints; j++) {
          p2.addPoint((int)((180.0 + ((double)p.xpoints[j] / INT_SCALER)) * RESOLUTION),
                      (int)((90.0  - ((double)p.ypoints[j] / INT_SCALER)) * RESOLUTION));
          minx = Math.min(minx, p2.xpoints[p2.npoints-1]);
          maxx = Math.max(maxx, p2.xpoints[p2.npoints-1]);
          miny = Math.min(miny, p2.ypoints[p2.npoints-1]);
          maxy = Math.max(maxy, p2.ypoints[p2.npoints-1]);           
        }
       
        g2d.fillPolygon(p2);
      }
    }
    g2d.setColor(Color.BLACK);
    for (int i=0; i<polys.size(); i++) {
      DPolygon p = polys.get(i);
      if (!p.clockwise) {
        DPolygon p2 = new DPolygon();
        for (int j=0; j<p.npoints; j++) {
          p2.addPoint((int)((180.0 + ((double)p.xpoints[j] / INT_SCALER)) * RESOLUTION),
                      (int)((90.0 -  ((double)p.ypoints[j] / INT_SCALER)) * RESOLUTION));
          minx=Math.min(minx, p2.xpoints[p2.npoints-1]);
          maxx=Math.max(maxx, p2.xpoints[p2.npoints-1]);
          miny=Math.min(miny, p2.ypoints[p2.npoints-1]);
          maxy=Math.max(maxy, p2.ypoints[p2.npoints-1]);          
        }
        g2d.fillPolygon(p2);
      }
    }

    maxx = Math.min(WIDTH - 1, maxx);
    maxy = Math.min(HEIGHT - 1, maxy);
    for (int i = minx; i <= maxx; i++) {
      for (int j = miny; j <= maxy; j++) {
        if (buffer.getRGB(i, j)!=black) {
          if (unit_ids[i][j] == null_unit) unit_ids[i][j] = code;
          else if (unit_ids[i][j] != code) {
            if (contention_indexes[i][j] == -1) {
              ArrayList<Integer> ai = new ArrayList<Integer>();
              ai.add(code);
              ai.add(unit_ids[i][j]);
              contention_indexes[i][j] = contentions.size();
              contentions.add(ai);
            } else {
              ArrayList<Integer> ai = contentions.get(contention_indexes[i][j]);
              if (!ai.contains(code)) ai.add(code);
              if (!ai.contains(unit_ids[i][j])) ai.add(unit_ids[i][j]); 
            }
          }
          buffer.setRGB(i, j, black);
        }
      }
    }
  }
  
  
  public void makeMap() throws Exception {
    map = null;
    System.gc();
    map = new int[WIDTH][HEIGHT];
    int[][] contention_indexes = new int[43200][21600];
    
    System.out.println("Getting contentious memory");
    ArrayList<ArrayList<Integer>> contentions = new ArrayList<ArrayList<Integer>>();

    for (int i = 0; i < 43200; i++) {
      for (int j = 0; j < 21600; j++) {
        map[i][j] = -1;
        contention_indexes[i][j] = -1;
      }
    }

    for (int i = 0; i < unit_shapes.size(); i++) {
      System.out.println(i + "\t" + unit_names.get(i) + "\t" + unit_numbers.get(i));
      rasterisePoly(unit_shapes.get(i), map, contention_indexes, contentions, i, -1);
    }
    resolveContentions(unit_shapes, map, contention_indexes, contentions);
    contentions.clear();
  }
  
  /**
   * Resolve all the contentious pixel issues in the rasterisation process.
   * <p>
   * 
   * 
   * @param  all_polygons       The list of all shapes.
   * @param  map                The 2-D array representing the map image
   * @param  contention_indexes The record where all contentious pixels were recorded
   * @param  contentions        For each pixel where contention occurs, list the unit ids that were claiming that pixel.
   */
  public void resolveContentions(ArrayList<ArrayList<DPolygon>> all_polygons,int[][] map, int[][] contention_indexes, ArrayList<ArrayList<Integer>> contentions) {
    Rectangle cell = new Rectangle();
    for (int i = 0; i < WIDTH; i++) {
      for (int j = 0; j < HEIGHT; j++) {
        if (contention_indexes[i][j]>=0) {
          int xleft = (int) (((i / RESOLUTION) - 180.0) * INT_SCALER);
          int ybottom = (int) (((((WIDTH-1) - j) / RESOLUTION) - 90) * INT_SCALER);
          int ytop = (int) (ybottom - (INT_SCALER / RESOLUTION));
          int rectsize = (int) (INT_SCALER / RESOLUTION);
          cell.setBounds(xleft, ytop, rectsize, rectsize);
          int max_score = 0;
          int best_id = -1;
          ArrayList<Integer> cs = contentions.get(contention_indexes[i][j]);

          for (int k = 0; k < cs.size(); k++) {
            ArrayList<DPolygon> adp = all_polygons.get(cs.get(k));
            for (int _l = 0; _l < adp.size(); _l++) {
              DPolygon dp = adp.get(_l);
              if (dp.clockwise) {
                if (dp.intersects(cell)) {
                  int score = 0;
                  double spacer = INT_SCALER / 8.0;
                  for (int p = 0; p <= 8; p++) {
                    for (int q = 0; q <= 8; q++) {
                      if (dp.contains(xleft + (p * spacer), ytop + (q * spacer))) score++;
                    }
                  }
                  if (score > max_score) {
                    max_score = score;
                    best_id = cs.get(k);
                  }
                }
              }
            }
          }
          // System.out.println("Contention at "+i+","+j+" from "+unit_ids[i][j]+" to "+best_id);
          map[i][j] = best_id;
        }
      }
    }
  }
  
  /**
   * Find the nearest unit_id to a given co-ordinate.
   * <p>
   * Use this when raster population data claims there are people in a certain pixel, yet no shapefile
   * contains that pixel. This is frequently the case on boundaries with coastlines.
   * 
   * @param  i         horizontal co-ordinate 
   * @param  j         vertical co-ordinate
   * @param  unit_ids  the rasterised array of ids.
   * @param  null_unit the id that represents "no unit here"
   * @return the id of the nearest non-null unit id
   */  
  
  /**
   * @param i
   * @param j
   * @return
   */
  public int getNearest_ints(int i, int j) {
    int correction=-1;
    int radius = 1;
    
    int mx=map.length;
    int my=map[0].length;
    boolean found = false;
    while (!found) { // First do the cross shapes - nearest points.
      if (map[(i+(mx-radius))%mx][j] != -1) {       correction=((int) map[(i+(mx-radius))%mx][j]); found = true; }
      else if (map[(i+radius)%mx][j] != -1) {       correction=((int) map[(i+radius)%mx][j]);      found = true; }
      else if (map[i][(j+radius)%my] != -1) {       correction=((int) map[i][(j+radius)%my]);      found = true; }
      else if (map[i][(j+(my-radius))%my] != -1) {  correction=((int) map[i][(j+(my-radius))%my]); found = true; }
      
      else { // Do the other points in the surrounding square,
               // starting with those nearest the cross.
        for (int r = 1; r < radius; r++) {
          if (!found) {
            if (map[(i+(mx-radius))%mx][(j+r)%my] != -1)      {      correction=((int) map[(i+(mx-radius))%mx][(j+r)%my]);      found = true; } 
            else if (map[(i+r)%mx][(j+radius)%my] != -1) {           correction=((int) map[(i+r)%mx][(j+radius)%my]);           found = true; } 
            else if (map[(i+radius)%mx][(j+(my-r))%my] != -1) {      correction=((int) map[(i+radius)%mx][(j+(my-r))%my]);      found = true; } 
            else if (map[(i+(mx-r))%mx][(j+(my-radius))%my] != -1) { correction=((int) map[(i+(mx-r))%mx][(j+(my-radius))%my]); found = true; } 
            else if (map[(i+(mx-radius))%mx][(j+(my-r))%my] != -1) { correction=((int) map[(i+(mx-radius))%mx][(j+(my-r))%my]); found = true; } 
            else if (map[(i+(mx-r))%mx][(j+radius)%my] != -1) {      correction=((int) map[(i+(mx-r))%mx][(j+radius)%my]);      found = true; } 
            else if (map[(i+radius)%mx][(j+r)%my] != -1) {           correction=((int) map[(i+radius)%mx][(j+r)%my]);           found = true; } 
            else if (map[(i+r)%mx][(j+(my-radius))%my] != -1) {      correction=((int) map[(i+r)%mx][(j+(my-radius))%my]);      found = true; }
          }
        }
        if (!found) {
          if (map[(i+(mx-radius))%mx][(j+radius)%my] != -1) {           correction=((int) map[(i+(mx-radius))%mx][(j+radius)%my]);      found = true; } 
          else if (map[(i+radius)%mx][(j+(my-radius))%my] != -1) {      correction=((int) map[(i+radius)%mx][(j+(my-radius))%my]);      found = true; } 
          else if (map[(i+radius)%mx][(j+radius)%my] != -1) {           correction=((int) map[(i+radius)%mx][(j+radius)%my]);           found = true; } 
          else if (map[(i+(mx-radius))%mx][(j+(my-radius))%my] != -1) { correction=((int) map[(i+(mx-radius))%mx][(j+(my-radius))%my]); found = true; }  
        }
      }
      radius++;
    }
    return correction;
  }
    
  /**
   * Find the nearest unit_id to a given longitude and latitude.
   * <p>
   * Use this when raster population data claims there are people in a certain pixel, yet no shapefile
   * contains that pixel. This is frequently the case on boundaries with coastlines.
   * 
   * @param  lon       longitude 
   * @param  lat       latitude
   * @param  unit_ids  the rasterised array of ids.
   * @param  null_unit the id that represents "no unit here"
   * @return the id of the nearest non-null unit id
   */ 
  public int getNearest(double lon, double lat) {
    return getNearest_ints(getDefaultXForLon(lon), getDefaultYForLat(lat));
  }
  
  /**
   * Calculate the centroid, either population-weighted or not, of a unit.
   * <p>
   * 
   * @param  id           id of the unit for which to find centroid 
   * @param  pop_weighted true, if you want population-weighted centroid, false for "normalised population" weighted.
   * @param  pop          rasterised population
   * @return an array of three doubles: [0] longitude of centroid, [1] latitude of centroid, [2] The population, or the number of cells in the centroid, depending on pop_weighted.
   */ 
  public double[] getCentroid(int id, boolean pop_weighted, int[][] pop) {
    
    double[] final_result = new double[3]; // Return longitude and latitude.
    
    // First, find extents of this admin code.
    
    int min_x = WIDTH + 1;
    int max_x = -1;
    int min_y = HEIGHT + 1;
    int max_y = -1;
    for (int i = 0; i < WIDTH; i++) {
      for (int j = 0; j < HEIGHT; j++) {
        if (map[i][j] == id) {
          min_x = Math.min(min_x, i);
          max_x = Math.max(max_x, i);
          min_y = Math.min(min_y, j);
          max_y = Math.max(max_y, j);
        }
      }
    }

    // Now calculate totals of population and area.
    
    ArrayList<Double> longitudes = new ArrayList<Double>();
    ArrayList<int[]> pops = new ArrayList<int[]>();    
    
    int grand_pop_total = 0;
    int grand_area_total = 0;
    for (int j = min_y; j <= max_y; j++) {
      int pop_total = 0;
      int area_total = 0;
      for (int i = min_x; i <= max_x; i++) {
        if ((map[i][j] == id) && (pop[i][j] > 0)) {
          pop_total += pop[i][j];
          grand_pop_total += pop[i][j];
          area_total++;
          grand_area_total++;
        }
      }
      
      // So pop_total is total population on this y-line.
      // area_total is number of "live" cells on this y-line.
      // grand_pop_total is accumulating total population for this admin unit.
      // grand_area_total is doing the same for area.

      // Now, we either need to count half of population, or half of live cells, depending on which weighting.
      
      double total = (pop_weighted) ? (pop_total / 2) : (area_total / 2);
      
      for (int i = min_x; i <= max_x; i++) {
        if ((map[i][j] == id) && (pop[i][j] > 0)) {
          int cell_weight = pop_weighted ? pop[i][j] : 1;
          if (total >= cell_weight) {
            total -= cell_weight;
          } else {
            double xpos = i;
            if (pop_weighted) xpos += (total / pop[i][j]);
            else xpos += total; // Either total = 0 or 0.5 for area weighted.
            longitudes.add(-180 + (xpos / RESOLUTION)); // Linear interpolate between pixels.
            int[] iresult = new int[3];
            iresult[0] = j;
            iresult[1] = pop_total;
            iresult[2] = area_total;
            pops.add(iresult);
            i = max_x + 1;
          }
        }
      }
      // So we've now added (lon,lat,pop_total_on_line,area_total_of_line) to the POINTS array. 
    }
    
    // And we now have a list of points which present the mid-pont of each line.
    // Half way down this line will be the mid-point of the shape.
    
    double remember_total = (pop_weighted) ? (grand_pop_total / 2) : (grand_area_total / 2);
    for (int j = 0; j < longitudes.size(); j++) {
      int cell_weight = (pop_weighted) ? pops.get(j)[1] : pops.get(j)[2];
      if (remember_total >= cell_weight) {
        remember_total -= cell_weight;
      } else {
        double ypos = pops.get(j)[0] - 1; // Northern edge of cell.
        if (pop_weighted) ypos += (remember_total / pops.get(j)[1]);
        else ypos += (remember_total);
        final_result[0] = longitudes.get(j);
        final_result[1] = (90 - (1.0 / RESOLUTION)) - (ypos / RESOLUTION);
        final_result[2] = (pop_weighted) ? grand_pop_total : grand_area_total;
        j = longitudes.size();
      }
    }
    longitudes.clear();
    pops.clear();
    return final_result;
  }
  
  /**
   * Load a previously saved raster map file
   * <p>
   * 
   * @param  file           the filename 
   * @throws Exception      if any exception occurs.
   */
  public void loadMapFile(String file) throws Exception {
    if (map==null) map = new int[WIDTH][HEIGHT];
    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(file))));
    for (int j = 0; j < 21600; j++) {
      for (int i = 0; i < 43200; i++) {
        map[i][j] = dis.readInt();
      }
    }
    dis.close();
  }
  
  /**
   * Save the current raster map file
   * <p>
   * 
   * @param  file           the filename 
   * @throws Exception      if any exception occurs.
   */
  public void saveMapFile(String file) throws Exception {
    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(file))));
    for (int j = 0; j < 21600; j++) {
      for (int i = 0; i < 43200; i++) {
        dos.writeInt(map[i][j]);
      }
    }
    dos.close();
  }
  
  /**
   * Save the meta-data about admin units to a file
   * <p>
   * 
   * @param  file           the filename 
   * @throws Exception      if any exception occurs.
   */
  public void saveUnits(String file) throws Exception {
    PrintWriter PW = new PrintWriter(new File(file));
    for (int i = 0; i < unit_numbers.size(); i++) {
      PW.println(i + "\t" + unit_names.get(i) + "\t" + unit_numbers.get(i));
    }
    PW.close();
  }
  
  /**
   * Load the meta-data about admin units from a file
   * <p>
   * 
   * @param  file           the filename 
   * @throws Exception      if any exception occurs.
   */
  public void loadUnits(String file) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(file));
    unit_numbers.clear();
    unit_names.clear();
    String s = br.readLine();
    int max_level = ((s.split("\t").length-1) / 2) - 1;
    while (s!=null) {
      if (s.length()>5) {
        String[] bits = s.split("\t");
        if (max_level==0) {
          unit_names.add(bits[1]);
          unit_numbers.add(bits[2]);
        } else if (max_level==1) {
          unit_names.add(bits[1]+"\t"+bits[2]);
          unit_numbers.add(bits[3]+"\t"+bits[4]);
        } else if (max_level==2) {
          unit_names.add(bits[1]+"\t"+bits[2]+"\t"+bits[3]);
          unit_numbers.add(bits[4]+"\t"+bits[5]+"\t"+bits[6]);
        } else if (max_level==3) {
          unit_names.add(bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+bits[4]);
          unit_numbers.add(bits[5]+"\t"+bits[6]+"\t"+bits[7]+"\t"+bits[8]);
        } else if (max_level==4) {
          unit_names.add(bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+bits[4]+"\t"+bits[5]);
          unit_numbers.add(bits[6]+"\t"+bits[7]+"\t"+bits[8]+"\t"+bits[9]+"\t"+bits[10]);
        } else if (max_level==5) {
          unit_names.add(bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+bits[4]+"\t"+bits[5]+"\t"+bits[6]);
          unit_numbers.add(bits[7]+"\t"+bits[8]+"\t"+bits[9]+"\t"+bits[10]+"\t"+bits[11]+"\t"+bits[12]);
        }
      }
      s = br.readLine();
    }
    br.close();
  }
  
  /**
   * A helper function to extract a number of files from a GADM zip, to a given path, and
   * <p>
   * 
   * @param  zipfile           the zip to extract files from
   * @param  files             a list of files to extract 
   * @param  output_path       the path in which to drop files
   * @returns    true if successful, otherwise false.
   */
  
  private static boolean extractFromZip(final String zipfile, String[] files, String output_path) {
    boolean success=false;
    try {
      ZipInputStream zis = new ZipInputStream(new FileInputStream(zipfile));
      ZipEntry entry;
      byte[] buffer = new byte[16384];
      boolean done=false;
      int done_files=0;
      while ((!done) && ((entry = zis.getNextEntry())!=null)) {
        for (int i=0; i<files.length; i++) {
          if (files[i]!=null) {
            if (files[i].equals(entry.getName())) {
              FileOutputStream output = new FileOutputStream(output_path+File.separator+entry.getName());
              int len=0;
              while ((len=zis.read(buffer))>0) output.write(buffer,0,len);
              output.close();
              files[i]=null;
              done_files++;
              if (done_files==files.length) {
                done=true;
                success=true;
              }
              i=files.length;
            }
          }
        }
      }
      zis.close();
      
    } catch (Exception e) {}
    return success;
  }

  /**
   * Download a URL to a file
   * <p>
   * 
   * @param  filename       the filename to write
   * @param  urlString      the URL to download 
   * @throws MalformedURLException      if the URL is malformed!
   * @throws IOException on any other IO exception
   */
  
  private static void saveUrl(final String filename, final String urlString) throws MalformedURLException, IOException {
    BufferedInputStream in = null;
    FileOutputStream fout = null;
    try {
      URL url = new URL(urlString);
      in = new BufferedInputStream(url.openStream());
      fout = new FileOutputStream(filename);
      final byte data[] = new byte[1024];
      int count;
      while ((count = in.read(data, 0, 1024)) != -1) {
          fout.write(data, 0, count);
      }
    } finally {
      if (in != null) {
        in.close();
      }
      if (fout != null) {
        fout.close();
      }
    }
  }

  
  /**
   * Download a full set of Shapefiles from GADM
   * <p>
   * 
   * @param  folder         folder to download into 
   * @throws Exception      if any exception occurs.
   */
  public void downloadShapeFiles(String folder) throws Exception {
    // Utility code, to download the full set of shapefiles from GADM.
      
    String url = "http://www.gadm.org/country";
    String web = "http://biogeo.ucdavis.edu/data/gadm2.8/shp/";
    BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
    String s=in.readLine();
    String code;
    while (s!=null) {
      if (s.trim().equals("<select name=\"cnt\">")) {
        s = in.readLine();
        while (s.indexOf("</select>")==-1) {
          if (s.trim().length()>0) {
            s = s.substring(s.indexOf("\"")+1);
            s = s.substring(0,s.indexOf("\""));
            code = s.substring(0,3);
            saveUrl(folder+code+".zip",web+code+"_adm_shp.zip");
            int lev=0;
            while (extractFromZip(folder+code+".zip",
                new String[] {code+"_adm"+lev+".shp",code+"_adm"+lev+".dbf"}, folder)) lev++;
            new File(folder+code+".zip").delete();
          }
          s = in.readLine();
        }
      }
      s = in.readLine();
    }
  }
   
}

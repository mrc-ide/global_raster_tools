package jobs.e18_wpop_adm2;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_adm2 {
  private final static String gadm_path = "E:/Data/Boundaries/GADM3_6/";
  private final static String out_path = "E:/Jobs/WPop_ADM2/";
  private final static String wpop_file = "E:/Data/Census/WorldPop/global/worldpop2020global.bil";
  
  private final static int max_level = 2;
  private final static String[] popa_countries = new String[] { "Nigeria"};
  private final static String[] gadm36_countries = new String[] { "Nigeria"};
  private final static String index_file_out = "wpop_nga_adm"+max_level+"_index.txt";
  private final static String pop_file_out = "wpop_nga_adm"+max_level+".txt";
 
  static void update_ids(String[] name_bits, String[] num_bits, String[] current_admin, int[] admin_no) {
    for (int lev = 0; lev < max_level; lev++) {
      if (!current_admin[lev].equals(num_bits[lev])) {
        admin_no[lev]++;
        for (int i=lev + 1; i<max_level; i++) {
          admin_no[i] = 1;
        }
        admin_no[max_level] = 0;
        for (int i=0; i<=max_level; i++) current_admin[i] = num_bits[i];
        return;
      } 
    }
  }
  
  public void run() throws Exception {
    final String[] codes = new String[gadm36_countries.length];
    int start = 10;
    if (codes.length > 89) start = 100;
    for (int i=0; i<codes.length; i++) codes[i]=String.valueOf(start + i); 
    
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(gadm_path, max_level, "3.6");
    if (!new File(out_path+"map.bin").exists()) {
      System.out.println("Making map");
      GRT.makeMap();
      System.out.println("Saving map");
      GRT.saveMapFile(out_path+"map.bin");
      GRT.saveUnits(out_path+"units.txt");
    } else {
      GRT.loadUnits(out_path+"units.txt");
      System.out.println("Reading map");
      GRT.loadMapFile(out_path+"map.bin");
    }
    
    System.out.println("Creating Mask");
    
    // Get list of admin units we're interested in.
    
    ArrayList<Integer> pop_countries = new ArrayList<Integer>();
    ArrayList<String> pop_units_str = new ArrayList<String>();
    
    String[] current_admin = new String[max_level + 1];
    for (int i=0; i <= max_level; i++)
      current_admin[i] = GRT.unit_numbers.get(0).split("\t")[i];
    int[] admin_no = new int[max_level + 1];
    admin_no[max_level]=0;
    for (int i=0; i<max_level; i++) admin_no[i]=1;
    ArrayList<String> index_out = new ArrayList<String>();
    
    PrintWriter PW = new PrintWriter(out_path + index_file_out);
    for (int j=0; j<GRT.unit_names.size(); j++) {
      String[] name_bits = GRT.unit_names.get(j).split("\t");
      String[] num_bits = GRT.unit_numbers.get(j).split("\t");
      
      update_ids(name_bits, num_bits, current_admin, admin_no);
      admin_no[max_level]++;  
      
      boolean found=false;
      for (int i=0; i<gadm36_countries.length; i++) {
        if (name_bits[0].equals(gadm36_countries[i])) {
          found=true;
          pop_countries.add(Integer.parseInt(codes[i]));
          String thing = codes[i];
          for (int k=1; k<=max_level; k++) thing += ((admin_no[k]<10)?"0":"")+String.valueOf(admin_no[k]); 
          pop_units_str.add(thing);
          thing = "";
          for (int k=1; k<=max_level; k++) thing+="\t"+name_bits[k];
          index_out.add(pop_units_str.get(pop_units_str.size()-1)+"\t"+popa_countries[i]+thing);
          i = gadm36_countries.length;
        }
      }
      if (!found) pop_units_str.add("");
    }
    
    // Sort and write...
    
    Collections.sort(index_out);
    for (int i=0; i<index_out.size(); i++) PW.println(index_out.get(i));
    PW.close();
    System.out.println("Loading landscan");
    float[][] pop_raster = GRT.loadFloatGrid(wpop_file, 43200, 21600, 43200, 18720, 0, 720);
    
    System.out.println("Writing");
    PW = new PrintWriter(out_path+pop_file_out);
    String s;
    for (int j=0; j<21600; j++) {
      if (j%100==0) System.out.println(j+"/21600");
      for (int i=0; i<43200; i++) {
      if ((GRT.map[j][i]>=0) && (Math.round(pop_raster[j][i])>0)) {
          s = pop_units_str.get(GRT.map[j][i]);
          if (s.length()>0) {
            PW.print((float)(-180.0+((i/43200.0)*360.0))+"\t");
            PW.print((float)(89.9916666667-((j/21600.0)*180.0))+"\t");
            PW.print((int)Math.round(pop_raster[j][i])+"\t");
            PW.println(s.substring(0,2)+"\t"+s);
          }
        }
      }
    }
    PW.close();
  }
  
  public void createIsleOfWightAscii() throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(out_path + "wpop_uk_adm2_index.txt"));
    String s = br.readLine();
    String admin="";
    while (s!=null) {
      String[] bits = s.split("\t");
      if (bits[3].equals("Isle of Wight")) {
        admin = bits[0];
        break;
      }
      s = br.readLine();
    }
    br.close();
    br = new BufferedReader(new FileReader(out_path + "wpop_uk_adm2.txt"));
    s = br.readLine();
    int minx = Integer.MAX_VALUE;
    int maxx = Integer.MIN_VALUE;
    int miny = Integer.MAX_VALUE;
    int maxy = Integer.MIN_VALUE;
    double minlon = Double.MAX_VALUE;
    double minlat = Double.MAX_VALUE;
    
    System.out.println("Finding bounds");
    while (s!=null) {
      String[] bits = s.split("\t");
      if (bits[4].equals(admin)) {
        double lon = Double.parseDouble(bits[0]);
        double lat = Double.parseDouble(bits[1]);
        minlat = Math.min(minlat,  lat);
        minlon = Math.min(minlon,  lon);
        int lon_cell = 21600 + (int) Math.round(lon * 120.0);
        int lat_cell = 10800 - (int) Math.round(lat * 120.0);
        minx = Math.min(minx,  lon_cell);
        maxx = Math.max(maxx,  lon_cell);
        miny = Math.min(miny,  lat_cell);
        maxy = Math.max(maxy,  lat_cell);
      }
      s = br.readLine();
    }
    
    br.close();
    System.out.println("Bounds: x: "+minx+" .. "+maxx+", y: "+miny+" .. "+maxy);
    
    int[][] grid = new int[1+(maxy-miny)][1+(maxx-minx)];
    for (int j=0; j<grid.length; j++)
      for (int i=0; i<grid[j].length; i++)
        grid[j][i]=-999;
    int max_pop=0;
    int total_pop=0;
    br = new BufferedReader(new FileReader(out_path + "wpop_uk_adm2.txt"));
    s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      if (bits[4].equals(admin)) {
        int pop = Integer.parseInt(bits[2]);
        max_pop = Math.max(pop,  max_pop);
        total_pop += pop;
        double lon = Double.parseDouble(bits[0]);
        double lat = Double.parseDouble(bits[1]);
        int lon_cell = 21600 + (int) Math.round(lon * 120.0);
        int lat_cell = 10800 - (int) Math.round(lat * 120.0);
        grid[lat_cell-miny][lon_cell-minx] = pop;
      }
      s = br.readLine();
    }
    
    br.close();
    System.out.println("Total population = "+total_pop);
    int[] cols = new int[256];
    for (int i=0; i<256; i++) cols[i]=new Color(i,i,i).getRGB();
    int blue = new Color(0,0,128).getRGB();
    BufferedImage bi = new BufferedImage(grid[0].length+1, grid.length+1, BufferedImage.TYPE_3BYTE_BGR);
    for (int j=0; j<grid.length; j++)
      for (int i=0; i<grid[j].length; i++)
        if (grid[j][i]<0) bi.setRGB(i, j, blue);
        else bi.setRGB(i,  j,  cols[(int) (255.0*(grid[j][i]/(double)max_pop))]);
    ImageIO.write(bi,  "PNG", new File(out_path+ "iow.png"));
    
    PrintWriter PW = new PrintWriter(new File(out_path + "iow_ascii.txt"));
    PW.println("NCOLS     "+grid[0].length);
    PW.println("NROWS     "+grid.length);
    PW.println("CELLSIZE  0.00833333333333333333");
    PW.println("XLLCORNER "+minlon);
    PW.println("YLLCORNER "+minlat);
    PW.println("NODATA    -9999");
    for (int j=0; j<grid.length; j++) {
      for (int i=0; i<grid[j].length; i++) {
        if (grid[j][i]<0) PW.print("0");
        else PW.print(grid[j][i]);
        if (i<grid[j].length-1) PW.print(" ");
      }
      PW.println();
    }
    PW.close();
  }
       
  public static void main(String[] args) throws Exception {
    create_adm2 CAD2 = new create_adm2();
    CAD2.run();
    //CAD2.createIsleOfWightAscii();
  }
}

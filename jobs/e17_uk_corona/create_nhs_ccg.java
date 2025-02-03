package jobs.e17_uk_corona;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_nhs_ccg {
  private final static String landscanPath = "E:/Data/Census/Landscan2018/";
  private final static String shapePath = "X:/Shapefiles/NHS_CCG_2019/";
  private final static String outPath = "E:/Jobs/UK-Corona/";
  private final static String index_file_out = "pop_nhs_ccg_index.txt";
  private final static String pop_file_out = "pop_nhs_ccg.txt";
  
  public void run() throws Exception {
    final String[] codes = new String[1];
    codes[0] = "10";
    int start = 10;
    if (codes.length > 89) start = 100;
    for (int i=0; i<codes.length; i++) codes[i]=String.valueOf(start + i); 
    
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(shapePath, 0, "NHS_CCG_19");
    if (!new File(outPath+"map.bin").exists()) {
      System.out.println("Making map");
      GRT.makeMap();
      System.out.println("Saving map");
      GRT.saveMapFile(outPath+"map.bin");
      GRT.saveUnits(outPath+"units.txt");
    } else {
      GRT.loadUnits(outPath+"units.txt");
      System.out.println("Reading map");
      GRT.loadMapFile(outPath+"map.bin");
    }
    
    System.out.println("Creating Mask");
    
    // Get list of admin units we're interested in.
    
    PrintWriter PW = new PrintWriter(outPath + index_file_out);
    ArrayList<String> pop_units_str = new ArrayList<String>();
    
    for (int j=1; j<=GRT.unit_names.size(); j++) {
      String[] name_bits = GRT.unit_names.get(j-1).split("\t");
      String admin = ""+((j<100)?"0":"")+((j<10)?"0":"")+j;
      PW.println("10"+admin+"\tEngland\t"+name_bits[0]);
      pop_units_str.add(10+admin);
    }
    
    // Sort and write...
 
    PW.close();
    System.out.println("Loading landscan");
    float[][] landscan = GRT.loadFloatGrid(landscanPath+"lspop.flt", 43200,21600, 43200,21600, 0, 0);
    
    System.out.println("Writing");
    PW = new PrintWriter(outPath+pop_file_out);
    String s;
    ArrayList<String[]> popfile = new ArrayList<String[]>();
    for (int j=0; j<21600; j++) {
      if (j%100==0) System.out.println(j+"/21600");
      for (int i=0; i<43200; i++) {
        if ((GRT.map[j][i]>=0) && (landscan[j][i]>0)) {
          s = pop_units_str.get(GRT.map[j][i]);
          if (s.length()>0) {
            String out = "";
            out+=((float)(-180.0+((i/43200.0)*360.0))+"\t");
            out+=((float)(89.9916666667-((j/21600.0)*180.0))+"\t");
            out+=((int)landscan[j][i]+"\t");
            out+=(s.substring(0,2)+"\t"+s);
            PW.println(out);
            popfile.add(out.split("\t"));
          }
        }
      }
    }
    
    PW.close();
    
    int minlat = 49; // South
    int maxlat = 61; // North
    int minlon = -9;
    int maxlon = 2;
    int ysize = ((maxlat-minlat)+1)*120;
    int xsize = ((maxlon-minlon)+1)*120;
    int white = Color.white.getRGB();
    int black = Color.black.getRGB();
    int grey = Color.DARK_GRAY.getRGB();
    BufferedReader br = new BufferedReader(new FileReader(outPath+index_file_out));
    s = br.readLine();
    int[] pops = new int[GRT.unit_names.size()+1];
    while (s!=null) {
      String[] bits = s.split("\t");
      BufferedImage bi = new BufferedImage(xsize, ysize, BufferedImage.TYPE_3BYTE_BGR);
      for (int y=0; y<ysize; y++) for (int x=0; x<xsize; x++) bi.setRGB(x, y, black);
      for (int i=0; i<popfile.size(); i++) {
        String[] p = popfile.get(i);
        int xpix = (int) Math.round((Double.parseDouble(p[0]) - minlon) * 120.0);
        int ypix = (int) Math.round((maxlat - (Double.parseDouble(p[1]))) * 120.0);
        if (popfile.get(i)[4].equals(bits[0])) {
          bi.setRGB(xpix,ypix, white);
          pops[Integer.parseInt(bits[0])-10001]+=Integer.parseInt(popfile.get(i)[2]);
        } else {
          bi.setRGB(xpix, ypix, grey);
        }          
      }
      System.out.println(bits[0]);
      Graphics g = (Graphics2D) bi.getGraphics();
      g.setColor(Color.WHITE);
      g.setFont(new Font("Calibri", Font.BOLD, 48));
      s=s.replaceAll("\t", "      ");
      g.drawString(s, 50, ysize-100);
      ImageIO.write(bi,  "PNG",  new File(outPath+"/pngs/"+bits[0]+".png"));
      s = br.readLine();
    }
    br.close();
    br = new BufferedReader(new FileReader(outPath+index_file_out));
    PW = new PrintWriter(new FileWriter(outPath+index_file_out+"2"));
    s = br.readLine();
    int i=0;
    while (s!=null) {
      PW.println(s+"\t"+pops[i]);
      i++;
      s=br.readLine();
    }
    br.close();
    PW.close();
  }
  
  public static void main(String[] args) throws Exception {
    create_nhs_ccg CCG= new create_nhs_ccg();
    CCG.run();
  }
}

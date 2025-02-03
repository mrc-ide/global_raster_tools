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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_nhs_stp {
  private final static String landscanPath = "E:/Data/Census/Landscan2018/";
  private final static String shapePath = "X:/Shapefiles/NHS_STPs_2019/";
  private final static String outPath = "E:/Jobs/UK-Corona/";
  private final static String index_file_out = "pop_nhs_stps_index.txt";
  private final static String pop_file_out = "pop_nhs_stps.txt";
  
  static final List<String> regions = Arrays.asList(new String[] {"London", "South East", "South West", "East of England", "Midlands", "North East and Yorkshire", "North West", });
  int[][] members = new int[][] {{23,24,25,26,27}, {28,29,30,31,38,40}, {32,33,34,35,36,37,39}, {17,18,19,20,21,22}, {6,7,8,9,10,11,12,13,14,15,16}, {1,2,5,42}, {3,4,41}};
  
  public void run() throws Exception {
    final String[] codes = new String[1];
    codes[0] = "10";
    int start = 10;
    if (codes.length > 89) start = 100;
    for (int i=0; i<codes.length; i++) codes[i]=String.valueOf(start + i); 
    
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(shapePath, 0, "NHS_STP_19");
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
    ArrayList<String> index_file = new ArrayList<String>();
    for (int j=0; j<GRT.unit_names.size(); j++) {
      String[] name_bits = GRT.unit_names.get(j).split("\t");
      String[] num_bits = GRT.unit_numbers.get(j).split("\t");
      int num = Integer.parseInt(num_bits[0]);
      int freg=-1;
      int funit=-1;
      for (int i=0; i<members.length; i++) {
        for (int k=0; k<members[i].length; k++) {
          if (members[i][k]==num) {
            freg=i+1;
            funit=k+1;
            break;
          }
        }
      }
      if (freg==-1) { System.out.println("Couldn't find "+name_bits[0]); System.exit(0); }
      String unit = "100"+freg+((funit<10)?"0":"")+funit;
      index_file.add(unit+"\tEngland\t"+regions.get(freg-1)+"\t"+name_bits[0]);
      pop_units_str.add(unit);
    }
    
    Collections.sort(index_file);
    for (int i=0; i<index_file.size(); i++) PW.println(index_file.get(i));
    
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
          //pops[Integer.parseInt(bits[0])-100001]+=Integer.parseInt(popfile.get(i)[2]);
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
  
  public void convert_stp() throws Exception {
    // Actually we want separate region and STP files.
    HashMap<String, String> conv_stp = new HashMap<String, String>();
    int unit=1;
    for (int i=1; i<=members.length; i++) {
      for (int j=1; j<=members[i-1].length; j++) {
        String from = "100"+i+((j<10?"0":"")+j);
        String to = "10"+((unit<10)?"0":"")+unit+"00";
        conv_stp.put(from,to);
        System.out.println(from +" -> " +to);
        unit++;
      }
    }
    BufferedReader br = new BufferedReader(new FileReader(outPath+pop_file_out));
    PrintWriter PW = new PrintWriter(new File(outPath+pop_file_out+"_stp"));
    String s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      PW.println(bits[0]+"\t"+bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+conv_stp.get(bits[4]));
      s = br.readLine();
    }
    br.close();
    PW.close();
    br = new BufferedReader(new FileReader(outPath+index_file_out));
    PW = new PrintWriter(new File(outPath+index_file_out+"_stp"));
    s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      String uname = bits[2]+"_"+bits[3];
      uname = uname.replaceAll(" ", "_");
      PW.println(conv_stp.get(bits[0])+"\t"+bits[1]+"\t"+uname);
      s = br.readLine();
    }
    br.close();
    PW.close();
  }
  
  public void convert_reg() throws Exception {
    // Actually we want separate region and STP files.
    
    BufferedReader br = new BufferedReader(new FileReader(outPath+pop_file_out));
    PrintWriter PW = new PrintWriter(new File(outPath+pop_file_out+"_reg"));
    String s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      PW.println(bits[0]+"\t"+bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+bits[4].substring(0,4)+"00");
      s = br.readLine();
    }
    br.close();
    PW.close();
    br = new BufferedReader(new FileReader(outPath+index_file_out));
    PW = new PrintWriter(new File(outPath+index_file_out+"_reg"));
    s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      PW.println(bits[0].substring(0,4)+"00"+"\t"+bits[1]+"\t"+bits[2].replaceAll(" ", "_"));
      s = br.readLine();
    }
    br.close();
    PW.close();
  }

  
  public static void main(String[] args) throws Exception {
    create_nhs_stp STP = new create_nhs_stp();
    //STP.run();
    //STP.convert_stp();
    STP.convert_reg();
  }
}

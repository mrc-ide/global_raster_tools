package jobs.e17_uk_corona;
// Add Scotland and Wales to NHS STP 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;

public class Add_ScoWal {
  public static void main(String[] args) throws Exception {
    System.out.println("Find bounds");
    BufferedReader stp = new BufferedReader(new FileReader("x:/Europe_Spatial_Data/Shapefiles/NHS_STPs_2019/pop_nhs_stps.txt"));
    String s = stp.readLine();
    int maxx = Integer.MIN_VALUE;
    int maxy = maxx;
    int minx = Integer.MAX_VALUE;
    int miny = minx;
    while (s!=null) {
      String[] bits = s.split("\t");
      int x = (int) Math.round((180.0 + Double.parseDouble(bits[0]))*120.0);
      int y =(int) Math.round((90.0 - Double.parseDouble(bits[1]))*120.0);
      maxx = Math.max(maxx, x);
      minx = Math.min(minx, x);
      maxy = Math.max(maxy,  y);
      miny = Math.min(miny,  y);
      s = stp.readLine();
    }
    stp.close();
    System.out.println("X: "+minx+".."+maxx+", Y: "+miny+".."+maxy);
    System.out.println("Load STP mask");
    stp = new BufferedReader(new FileReader("x:/Europe_Spatial_Data/Shapefiles/NHS_STPs_2019/pop_nhs_stps.txt"));
    s = stp.readLine();
    boolean[][] filled = new boolean[1+(maxx-minx)][1+(maxy-miny)];
    for (int x=0; x<filled.length; x++) 
      for (int y=0; y<filled[x].length; y++) 
        filled[x][y]=false;
    
    while (s!=null) {
      String[] bits = s.split("\t");
      int x = (int) Math.round((180.0 + Double.parseDouble(bits[0]))*120.0);
      int y =(int) Math.round((90.0 - Double.parseDouble(bits[1]))*120.0);
      filled[x-minx][y-miny]=true;
      s = stp.readLine();
    }
    stp.close();
    int count=0;
    for (int x=0; x<filled.length; x++) 
      for (int y=0; y<filled[x].length; y++) 
        count+=(filled[x][y]?1:0);
    System.out.println("Filled cells = "+count);
    System.out.println("Rewrite");
    
    stp = new BufferedReader(new FileReader("x:/Europe_Spatial_Data/Shapefiles/NHS_STPs_2019/pop_nhs_stps.txt"));
    PrintWriter PW = new PrintWriter(new File("x:/Europe_Spatial_Data/Shapefiles/NHS_STPs_2019/pop_nhs_stps_gb.txt"));
    s = stp.readLine();
    while (s!=null) {
      PW.println(s);
      s = stp.readLine();
    }
    stp.close();
    HashMap<String, String> translate = new HashMap<String, String>();
    // Map from the Scotland and Wales (11,x1,y1) and (12,x2,y2) into (10,08,x) and (10,09,x2)
    int[] scot = new int[] {11,6,2,6,7};
    int[] wales = new int[] {7,15};
    for (int i=1; i<=scot.length; i++) {
      for (int j=1; j<=scot[i-1]; j++) {
        String from = "110"+i+((j<10)?"0":"")+j;
        String to = "10080"+i;
        translate.put(from, to);
        System.out.println(from+" -> "+to);
      }
    }
    
    for (int i=1; i<=wales.length; i++) {
      for (int j=1; j<=wales[i-1]; j++) {
        String from = "120"+i+((j<10)?"0":"")+j;
        String to = "10090"+i;
        translate.put(from, to);
        System.out.println(from+" -> "+to);
      }
    }
    
    // Read GB data (NHS Stp takes precedence if collisions)
    BufferedReader gb = new BufferedReader(new FileReader("x:/Europe_Spatial_Data/Uk_regions/pop_uk_regions.txt"));
    s = gb.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      // If Scotland or Wales
      if (bits[3].equals("11") || bits[3].equals("12")) {

        // If not STP already
        
        int x = (int) Math.round((180.0 + Double.parseDouble(bits[0]))*120.0);
        int y =(int) Math.round((90.0 - Double.parseDouble(bits[1]))*120.0);
        boolean ok = false;
        if ((x<minx) || (x>=maxx) || (y<miny) || (y>=maxy)) ok = true;
        else ok = !filled[x-minx][y-miny];
        
        if (ok) {
          bits[4] = translate.get(bits[4]);
          bits[3] = bits[4].substring(0,2);
          PW.println(bits[0]+"\t"+bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+bits[4]);
        }
      }
      s = gb.readLine();
    }
    gb.close();
    PW.close();
    
    // Update index file
    
    BufferedReader br = new BufferedReader(new FileReader("x:/Europe_Spatial_Data/Shapefiles/NHS_STPs_2019/pop_nhs_stps_index.txt"));
    PW = new PrintWriter(new File("x:/Europe_Spatial_Data/Shapefiles/NHS_STPs_2019/pop_nhs_stps_gb_index.txt"));
    s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      PW.println(bits[0]+"\tUnited_Kingdom\t"+bits[2].replaceAll(" ", "_")+"\t"+bits[3].replaceAll(" ", "_"));
      s = br.readLine();
    }
    br.close();
    br = new BufferedReader(new FileReader("x:/Europe_Spatial_Data/Uk_regions/pop_uk_index_regions.txt"));
    s = br.readLine();
    String remember_last = "Banana";
    while (s!=null) {
      String[] bits = s.split("\t");
      if (!bits[0].substring(0,4).equals(remember_last)) {
        remember_last = bits[0].substring(0,4);
        if (bits[0].startsWith("11") || bits[0].startsWith("12")) {
          PW.println(translate.get(bits[0])+"\tUnited_Kingdom\t"+bits[1].replaceAll(" ", "_")+"\t"+bits[2].replaceAll(" ", "_"));
        }
      }
      s = br.readLine();
    }
    PW.close();
    br.close();
  }
}
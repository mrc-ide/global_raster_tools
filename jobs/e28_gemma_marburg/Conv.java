package jobs.e28_gemma_marburg;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Conv {
  final static String _outPath = "E:/jobs/Gemma/";
  final static String _shapePath = "E:/jobs/Gemma";  
  final static String landscan = "E:/Data/Census/Landscan2021/landscan2021.bil";
  
  public void run(String suffix) throws Exception {
    String outPath = _outPath+suffix+"/";
    String shapePath = _outPath+suffix+"/";
    System.out.println("Loading polygons");
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(shapePath, 2, "4.1");
    for (int i=0; i<GRT.unit_names.size(); i++) {
      System.out.println(GRT.unit_names.get(i));
    }
    System.out.println("Making map");
    if (!new File(outPath+"map.bin").exists()) {
      GRT.makeMap();
      GRT.saveMapFile(outPath+"map.bin");
      GRT.saveUnits(outPath+"units.txt");
    } else {
      GRT.loadUnits(outPath+"units.txt");
      GRT.loadMapFile(outPath+"map.bin");
    }
    /* Make some hideous shape images */
    //System.out.println("Making hideous images");
    //int[] extents = GRT.getGlobalExtent(GRT.map, 0, Integer.MAX_VALUE);
    //for (int i=0; i<GRT.unit_names.size(); i++) {
//      GRT.hideousShapePNG(GRT.map, outPath+"shapes_"+i+".png", extents, i, i+": "+GRT.unit_names.get(i));
    //}
    
    System.out.println("Loading landscan");
    int[][] ls2021 = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
       
    
    HashMap<String, Integer> country_lookup = new HashMap<String, Integer>();
    country_lookup.put("Burundi",  6);
    country_lookup.put("Cameroon",  7);
    country_lookup.put("Equatorial Guinea", 14);
    country_lookup.put("Gabon",  17);
    country_lookup.put("Rwanda",  37);
    country_lookup.put("Tanzania",  45);
    country_lookup.put("Uganda",  48);
    
    // Assign 5/6-digit ids (cc1122)
    
    ArrayList<String> csids = new ArrayList<String>();
    
    String old_country = "";
    String old_adm1 = "";
    String old_adm2 = "";
    int adm1_int = -1;
    int adm2_int = -1;
    
    for (int i=0; i<GRT.unit_names.size(); i++) {
      String[] s = GRT.unit_names.get(i).split("\t");
      if (!old_country.equals(s[0])) {
        old_country = s[0];
        old_adm1 = "";
        old_adm2 = "";
        adm1_int = -1;
        adm2_int = -1;
      }
      
      int country = country_lookup.get(s[0]);
      if (!old_adm1.equals(s[1])) {
        adm1_int++;
        adm2_int = 0;
        if (adm1_int > 99) throw new Exception("More than 99 admin 1 units");
        old_adm1 = s[1];
        old_adm2 = s[2];
      } else {
        if (!old_adm2.equals(s[2])) {
          adm2_int++;
          if (adm2_int == 100) {
            adm1_int ++;
            adm2_int = 0;
            if (adm1_int > 99) throw new Exception("More than 99 admin 1 units");
          }
          old_adm2 = s[2];
        }
      }
      csids.add(String.valueOf(country)+(adm1_int < 10 ? "0" : "") + String.valueOf(adm1_int) + (adm2_int < 10 ? "0" : "") + String.valueOf(adm2_int));
    }
    
    System.out.println("Writing output");
    PrintWriter PW = new PrintWriter(new FileWriter(outPath+"pop.txt"));          
    for (int j=0; j<21600; j++) {
      double lon = 90 - ((j + 1.0) / 120.0);
      for (int i=0; i<43200; i++) {
        double lat = -180 + (i / 120.0);
        if (GRT.map[j][i] != -1) {
          if (ls2021[j][i] > 0) {
            int unit = GRT.map[j][i];
            String[] names = GRT.unit_names.get(unit).split("\t");
            PW.println((float)lon+"\t"+(float)lat+"\t"+ls2021[j][i]+"\t"+String.valueOf(country_lookup.get(names[0]))+"\t"+csids.get(GRT.map[j][i]));
          }
        }
      }
    }
    PW.close();
    
    PW = new PrintWriter(new FileWriter(outPath+"units.txt"));
    for (int i=0; i<GRT.unit_names.size(); i++) {
      String[] s = GRT.unit_names.get(i).split("\t");
      PW.println(csids.get(i)+"\t"+s[0].replaceAll(" ",  "_")+"\t"+s[1].replaceAll(" ", "_")+"\t"+s[2].replaceAll(" ",  "_"));
    }
    PW.flush();
    System.out.println("Done");
  }
 
 public static void main(String[] args) throws Exception {
    Conv conv = new Conv();
     conv.run("east");
     conv.run("west");  
  }
}
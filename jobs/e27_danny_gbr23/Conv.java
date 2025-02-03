package jobs.e27_danny_gbr23;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Conv {
  static int level = 2;
  static final String wd = "E:/Jobs/Danny/";
  static final String mapFile = wd+"map"+level+".bin";
  static final String unitsFile = wd+"units"+level+".txt";
  static String shp = "E:/Data/Boundaries/GADM3_6/gadm36_GBR_"+level+".shp";
  static String dbf = "E:/Data/Boundaries/GADM3_6/gadm36_GBR_"+level+".dbf";
  
  class GMobData {
    String date;
    int retail;
    int grocery;
    int parks;
    int transit;
    int workplace;
    int residential;
    GMobData() {}
  }
  
  public static void main(String[] args) throws Exception {
    Conv C = new Conv();
    C.work();
  }
  
  void work() throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygons(level, shp, dbf, null, "GADM3_6");
    if (!new File(mapFile).exists()) {
      GRT.saveUnits(unitsFile);
      System.out.println("Making map");
      GRT.makeMap();
      System.out.println("Saving map");
      GRT.saveMapFile(mapFile);
      System.exit(0);
        
    } else {
      GRT.loadUnits(unitsFile);
      //System.out.println("Reading map "+mapFile);
      //GRT.loadMapFile(mapFile);
      //int[] extents = GRT.getGlobalExtent(GRT.map, 0, 32767);
      //GRT.hideousShapePNG(GRT.map, wd+"GBR"+level+".png", extents, -1, "GBR - Level "+level);
      
      String[] unit_code = new String[GRT.unit_names.size()];
      String[] unit_name = new String[GRT.unit_names.size()];
      String prev_lev1 = "";

      int uk = 54;
      int t1 = 0;
      int t2 = 0;
      for (int i=0; i<GRT.unit_names.size(); i++) {
        String[] names = GRT.unit_names.get(i).split("\t");
        String next_lev1 = names[1];
        String next_lev2 = (level == 2) ? names[2] : names[2]+"_"+names[3];
        next_lev2 = next_lev2.replaceAll(" ",  "-");
        if (!prev_lev1.equals(next_lev1) || (t2 == 100)) {
          t1++;
          t2 = 0; 
        }
        prev_lev1 = next_lev1;
        String code = uk+(t1<10 ? "0":"")+ t1 + (t2 < 10 ? "0":"")+ t2;
        unit_code[i] = code;
        unit_name[i] = "\tUnited_Kingdom\t"+names[1]+"\t"+names[2]+"\t"+((level == 3) ? names[3] : "");
        t2++;
      }
      /*
      PrintWriter PW = new PrintWriter(new File(wd+"gbr_pop"+level+".txt"));
      int[][] pop = GRT.loadIntGrid("E:/Data/Census/Landscan2020/Landscan2020.bil", 43200,21600,43200,21600,0,0);
      float top = 90 - (1/120);
      for (int j=0; j<21600; j++) {
        for (int i=0; i<43200; i++) {
          if (GRT.map[j][i] >= 0) {
            PW.println(String.valueOf(-180 + (i / 120.0)) + "\t" + String.valueOf(top - (j / 120.0)) + " \t" + pop[j][i] +"\t 54 \t"+unit_code[GRT.map[j][i]]); 
          }
        }
      }
      PW.close();
      PW = new PrintWriter(new File(wd+"gbr_admin"+level+".txt"));
      for (int i=0; i<unit_code.length; i++) PW.println(unit_code[i]+"\t"+unit_name[i]);
      PW.close();
      */
      
      BufferedReader br = new BufferedReader(new FileReader(wd+"gm_hash_level"+level+".csv"));
      br.readLine(); // header - 0 = GADM, 1 = GM
      HashMap<String, String> gadm_to_gm = new HashMap<String, String>();
      String s = br.readLine();
      while (s != null) {
        String[] bits = s.split("\t");
        gadm_to_gm.put(bits[0], bits[1]);
        s = br.readLine();
      }
      br.close();
      
      
      br = new BufferedReader(new FileReader(wd+"google_mobility.csv"));
      br.readLine(); // header - 0 = GADM, 1 = GM
      s = br.readLine();
      HashMap<String, ArrayList<GMobData>> gmob = new HashMap<String, ArrayList<GMobData>>();
      while (s != null) {
        String[] bits = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        String reg = bits[2].trim();
        if (bits[3].length() > 0) reg += " # "+bits[3].trim();
        reg = reg.replaceAll("\"",  "");
        if (!gmob.containsKey(reg)) {
          gmob.put(reg,  new ArrayList<GMobData>());
        }
        GMobData gmd = new GMobData();
        gmd.date = bits[8];
        gmd.retail = (bits.length > 9) && (bits[9].length() > 0) ? Integer.parseInt(bits[9]) : -999;
        gmd.grocery = (bits.length > 10) && (bits[10].length() > 0) ? Integer.parseInt(bits[10]) : -999;
        gmd.parks = (bits.length > 11) && (bits[11].length() > 0) ? Integer.parseInt(bits[11]) : -999;
        gmd.transit = (bits.length > 12) && (bits[12].length() > 0) ? Integer.parseInt(bits[12]) : -999;
        gmd.workplace = (bits.length > 13) && (bits[13].length() > 0) ? Integer.parseInt(bits[13]) : -999;
        gmd.residential = (bits.length > 14) && (bits[14].length() > 0) ? Integer.parseInt(bits[14]) : -999;
        gmob.get(reg).add(gmd);
              
        s = br.readLine();
      }
      br.close();
      PrintWriter[] PWS = new PrintWriter[6];
      PWS[0] = new PrintWriter(new File(wd+"gmob_retail_level"+level+".txt"));
      PWS[1] = new PrintWriter(new File(wd+"gmob_grocery_level"+level+".txt"));
      PWS[2] = new PrintWriter(new File(wd+"gmob_parks_level"+level+".txt"));
      PWS[3] = new PrintWriter(new File(wd+"gmob_transit_level"+level+".txt"));
      PWS[4] = new PrintWriter(new File(wd+"gmob_workplace_level"+level+".txt"));
      PWS[5] = new PrintWriter(new File(wd+"gmob_residential_level"+level+".txt"));
      GregorianCalendar gc = new GregorianCalendar();
      gc.set(GregorianCalendar.DATE, 15);
      gc.set(GregorianCalendar.MONTH, GregorianCalendar.FEBRUARY);
      gc.set(GregorianCalendar.YEAR, 2020);
      for (int i=0; i<6; i++) PWS[i].print("unit");
      while(true) {
        int m = 1 + gc.get(GregorianCalendar.MONTH);
        int d = gc.get(GregorianCalendar.DATE);
        for (int i=0; i<6; i++) 
          PWS[i].print("\t"+gc.get(GregorianCalendar.YEAR) + ((m < 10) ? "0" : "") + m + ((d < 10) ? "0" : "") + d);
        gc.add(GregorianCalendar.DATE, 1);
        if ((gc.get(GregorianCalendar.YEAR) == 2022) &&
            (gc.get(GregorianCalendar.MONTH) == 9) &&
            (gc.get(GregorianCalendar.DATE) == 16)) break;
      }
      for (int i=0; i<6; i++) PWS[i].print("\n");
      
      for (int i=0; i<unit_name.length; i++) {
        String [] bits = unit_name[i].split("\t");
        String gadm = (level == 2) ? bits[3] : bits[3] + " # " + bits[4]; 
        String gm = gadm_to_gm.get(gadm);
        if (gm == null) {
          System.out.println("NO HASH MATCH FOR "+gadm);
          System.exit(0);
        }
        ArrayList<GMobData> gmdat = gmob.get(gm);
        if (gmdat == null) {
          System.out.println("No GM Data for "+gm);
          System.exit(0);
        }
        
        for (int j=0; j<6; j++) PWS[j].print(unit_code[i]);
        gc.set(GregorianCalendar.DATE, 15);
        gc.set(GregorianCalendar.MONTH, GregorianCalendar.FEBRUARY);
        gc.set(GregorianCalendar.YEAR, 2020);
        for (GMobData gmd : gmdat) {
          String dateString = gc.get(GregorianCalendar.YEAR)+"-";
          int m = gc.get(GregorianCalendar.MONTH) + 1;
          int d = gc.get(GregorianCalendar.DATE);
          dateString += ((m < 10) ? "0" : "") + m + "-" + ((d < 10) ? "0" : "") + d;
          while(!gmd.date.equals(dateString)) {
            System.out.println("Missing date "+dateString+" on unit "+gm);
            PWS[0].print("\t-999");
            PWS[1].print("\t-999");
            PWS[2].print("\t-999");
            PWS[3].print("\t-999");
            PWS[4].print("\t-999");
            PWS[5].print("\t-999");
            gc.add(GregorianCalendar.DATE,  1);
            m = gc.get(GregorianCalendar.MONTH) + 1;
            d = gc.get(GregorianCalendar.DATE);
            dateString = gc.get(GregorianCalendar.YEAR) +"-" + ((m < 10) ? "0" : "") + m + "-" + ((d < 10) ? "0" : "") + d;
          }
          PWS[0].print("\t" + gmd.retail);
          PWS[1].print("\t" + gmd.grocery);
          PWS[2].print("\t" + gmd.parks);
          PWS[3].print("\t" + gmd.transit);
          PWS[4].print("\t" + gmd.workplace);
          PWS[5].print("\t" + gmd.residential);
          gc.add(GregorianCalendar.DATE,  1);
        }
        for (int j=0; j<6; j++) PWS[j].print("\n");
      }
      for (int i=0; i<6; i++) PWS[i].close();
    }
  }
}

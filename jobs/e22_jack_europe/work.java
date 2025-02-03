package jobs.e22_jack_europe;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class work {
  private final static String gadm_path = "E:/Data/Boundaries/GADM3_6/";
  private final static String out_path = "E:/Jobs/WPop_ADM2/";
  private final static String[] popa_countries = new String[] { "France", "Portugal", "Spain"};
  private final static String[] gadm36_countries = new String[] { "France", "Portugal", "Spain"};
  
  static void update_ids(String[] name_bits, String[] num_bits, String[] current_admin, int[] admin_no, int max_level) {
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
  
  public void run(int year, int max_level) throws Exception {
    System.out.println("Working on "+year+", level "+max_level);
    String wpop_file = "E:/Data/Census/WorldPop/global/worldpop"+year+"global.bil";
    final String[] codes = new String[gadm36_countries.length];
    int start = 10;
    if (codes.length > 89) start = 100;
    for (int i=0; i<codes.length; i++) codes[i]=String.valueOf(start + i); 
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(gadm_path, max_level, Arrays.asList(gadm36_countries), "3.6");
    if (!new File(out_path+"map"+year+"_"+max_level+".bin").exists()) {
      System.out.println("Making map");
      GRT.makeMap();
      System.out.println("Saving map");
      GRT.saveMapFile(out_path+"map"+year+"_"+max_level+".bin");
      GRT.saveUnits(out_path+"units"+year+"_"+max_level+".txt");
    } else {
      GRT.loadUnits(out_path+"units"+year+"_"+max_level+".txt");
      System.out.println("Reading map");
      GRT.loadMapFile(out_path+"map"+year+"_"+max_level+".bin");
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
   
    String index_file_out = "wpop_"+year+"_adm"+max_level+"_index.txt";
    //String pop_file_out = "wpop_"+year+"_adm"+max_level+".txt";
   
    PrintWriter PW = new PrintWriter(out_path + index_file_out);
    for (int j=0; j<GRT.unit_names.size(); j++) {
      String[] name_bits = GRT.unit_names.get(j).split("\t");
      String[] num_bits = GRT.unit_numbers.get(j).split("\t");
      
      update_ids(name_bits, num_bits, current_admin, admin_no, max_level);
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
          if (name_bits.length <= max_level) {
            System.out.println("WHY ME?");
          }
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
    System.out.println("Loading raster");
    float[][] pop_raster = GRT.loadFloatGrid(wpop_file, 43200, 21600, 43200, 18720, 0, 720);
    int[][] pop_raster_i = new int[pop_raster.length][pop_raster[0].length];
    System.out.println("Convert to ints");
    
    for (int i=0; i<pop_raster.length; i++)
      for (int j=0; j<pop_raster[i].length; j++)
        pop_raster_i[i][j] = (int) Math.round(pop_raster[i][j]);
    pop_raster = null;
    
    System.out.println("Centroids");
    
    int[] extents = GRT.getGlobalExtent(GRT.map, 0, Integer.MAX_VALUE);
    GRT.hideousShapePNG(GRT.map, out_path+"hideous_"+year+"_"+max_level+".png", extents, -1, "");
    /*
    PW = new PrintWriter(out_path+"centroids_"+year+"_"+max_level+".txt");
    PW.print("Unit\t");
    for (int i=0; i<=max_level; i++) PW.print("N"+i+"\t");
    PW.println("Pop\tPop_Cent_Lon\tPop_Cent_Lat\tArea_Cent_Lon\tArea_Cent_Lat");
    for (int i=0; i<GRT.unit_names.size(); i++) {
      System.out.println("Centroid "+i);
      double[] centr_pop = GRT.getCentroid(i, true, pop_raster_i, extents);
      double[] centr_area = GRT.getCentroid(i,  false, pop_raster_i, extents);
      String[] names = GRT.unit_names.get(i).split("\t");
      if ((centr_pop==null) || (centr_area==null)) {
        PW.print(i+"\t");
        for (int j=0; j<=max_level; j++) PW.print(names[j]+"\t");
        PW.println("0\tNA\tNA\tNA\tNA");
  
      } else {
        PW.print(i+"\t");
        for (int j=0; j<=max_level; j++) PW.print(names[j]+"\t");
        PW.println(centr_pop[2]+"\t"
                      +centr_pop[0]+"\t"+centr_pop[1]+"\t"+centr_area[0]+"\t"+centr_area[1]);
      }
      PW.flush();
    }
    PW.close();
    */
    pop_raster_i = null;
    System.gc();
  }
  
  public static void main(String[] args) throws Exception {
    work W = new work();
    // So we want... 2006: Portugal level 1 & 2
    //               2007: France level 2 & 3   and Spain level 1 & 2
    //W.run(2006, 1);
    //W.run(2006, 2);
    //W.run(2007, 1);
    //W.run(2007, 2);
    W.run(2007, 3);
  }
}

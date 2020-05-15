package jobs.e18_wpop_adm2;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_adm2 {
  private final static String gadm_path = "E:/Data/Boundaries/GADM3_6/";
  private final static String out_path = "E:/Jobs/WPop_ADM2/";
  private final static String wpop_file = "E:/Data/Census/WorldPop/global/worldpop2020global.bil";
  
  private final static int max_level = 1;
  private final static String[] popa_countries = new String[] { "United Kingdom"};
  private final static String[] gadm36_countries = new String[] { "United Kingdom"};
  private final static String index_file_out = "wpop_uk_adm"+max_level+"_index.txt";
  private final static String pop_file_out = "wpop_uk_adm"+max_level+".txt";
 
  
  
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
  
  public static void main(String[] args) throws Exception {
    create_adm2 CAD2 = new create_adm2();
    CAD2.run();
  }
}

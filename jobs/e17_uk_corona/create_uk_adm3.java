package jobs.e17_uk_corona;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_uk_adm3 {
  private final static String landscanPath = "E:/Data/Census/Landscan2018/";
  private final static String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  private final static String outPath = "E:/Jobs/UK-Corona/";
  
  /*private final static String[] codes = new String[] { "12","13"};
  private final static String[] popa_countries = new String[] { "France", "Italy"};
  private final static String[] gadm36_countries = new String[] { "France", "Italy"};
  private final static String index_file_out = "pop_fr_it_index.txt";
  private final static String pop_file_out = "pop_fr_it.txt";
  private final static int max_level = 2;
  */
  
  private final static String[] codes = new String[] { "44"};
  private final static String[] popa_countries = new String[] { "United Kingdom"};
  private final static String[] gadm36_countries = new String[] { "United Kingdom"};
  private final static String index_file_out = "pop_uk_adm3_index.txt";
  private final static String pop_file_out = "pop_uk_adm3.txt";
  private final static int max_level = 3;
      
   
  
  public void run() throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(gadmPath, max_level, "3.6");
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
    
    ArrayList<Integer> uk_units = new ArrayList<Integer>();
    ArrayList<Integer> popUk_countries = new ArrayList<Integer>();
    ArrayList<String> popUk_units_str = new ArrayList<String>();
    String current_adm0=GRT.unit_numbers.get(0).split("\t")[0];
    String current_adm1=GRT.unit_numbers.get(0).split("\t")[1];
    String current_adm2=GRT.unit_numbers.get(0).split("\t")[2];
    String current_adm3=GRT.unit_numbers.get(0).split("\t")[3];
    int adm1_no=1;
    int adm2_no=0;
    int adm3_no=0;
   
    PrintWriter PW = new PrintWriter(outPath + index_file_out);
    for (int j=0; j<GRT.unit_names.size(); j++) {
      String[] name_bits = GRT.unit_names.get(j).split("\t");
      String[] num_bits = GRT.unit_numbers.get(j).split("\t");
      if (!current_adm0.equals(num_bits[0])) {
        // It's a new country
        adm1_no=1;
        adm2_no=1;
        adm3_no=0;
        current_adm0 = num_bits[0];
        current_adm1 = num_bits[1];
        current_adm2 = num_bits[2];
        current_adm3 = num_bits[3];
        
      } else if (!current_adm1.equals(num_bits[1])) {
        // Same country, new ADM1
        adm1_no++;
        adm2_no=1;
        adm3_no=0;
        current_adm1 = num_bits[1];
        current_adm2 = num_bits[2];
        current_adm3 = num_bits[3];
        
      } else if (!current_adm2.equals(num_bits[2])) {
      // Same ADM1, new ADM2.
        current_adm2 = num_bits[2];
        current_adm3 = num_bits[3];
        adm2_no++;
        adm3_no=0;
      }
      adm3_no++;
      boolean found=false;
      
      for (int i=0; i<gadm36_countries.length; i++) {
        if (name_bits[0].equals(gadm36_countries[i])) {
          found=true;
          uk_units.add(j);
          popUk_countries.add(Integer.parseInt(codes[i]));
          popUk_units_str.add(codes[i] + 
                                  ((adm1_no<10)?"0":"")+String.valueOf(adm1_no)+
                                  ((adm2_no<10)?"0":"")+String.valueOf(adm2_no)+
                                  ((adm3_no<10)?"0":"")+String.valueOf(adm3_no));
          PW.println(popUk_units_str.get(popUk_units_str.size()-1)+"\t"+popa_countries[i]+"\t"+name_bits[1]+"\t"+name_bits[2]+"\t"+name_bits[3]);
          i = gadm36_countries.length;
        }
      }
      if (!found) popUk_units_str.add("");
    }
    PW.close();
    System.out.println("Loading landscan");
    float[][] landscan = GRT.loadFloatGrid(landscanPath+"lspop.flt", 43200,21600, 43200,21600, 0, 0);
    
    System.out.println("Writing");
    PW = new PrintWriter(outPath+pop_file_out);
    String s;
    for (int j=0; j<21600; j++) {
      if (j%100==0) System.out.println(j+"/21600");
      for (int i=0; i<43200; i++) {
        if ((GRT.map[j][i]>=0) && (landscan[j][i]>0)) {
          s = popUk_units_str.get(GRT.map[j][i]);
          if (s.length()>0) {
            PW.print((float)(-180.0+((i/43200.0)*360.0))+"\t");
            PW.print((float)(89.9916666667-((j/21600.0)*180.0))+"\t");
            PW.print((int)landscan[j][i]+"\t");
            PW.println(s.substring(0,2)+"\t"+s);
          }
        }
      }
    }
    
    PW.close();
  }
  /*
  public void easyIds() throws Exception {
    HashMap<String, String> translate = new HashMap<String, String>();
    int index = 1;
    BufferedReader br = new BufferedReader(new FileReader(new File(outPath+"popch_index.txt")));
    PrintWriter PW = new PrintWriter(new File(outPath+"popch_index2.txt"));
    String s = br.readLine();
    while (s!=null) {
      // 010101 United Kingdom  England Barnsley
      String[] bits = s.split("\t");
      String index_st = String.valueOf(index);
      while (index_st.length()!=3) index_st = "0" + index_st;
      index_st = "01" + index_st;
      PW.println(index_st+"\t"+bits[1]+"\t"+bits[2]+"_"+bits[3]);
      translate.put(bits[0], index_st);
      s = br.readLine();
      index++;
    }
    br.close();
    PW.close();
    br = new BufferedReader(new FileReader(new File(outPath+"popUk.txt")));
    PW = new PrintWriter(new File(outPath+"popUk2.txt"));
    s = br.readLine();
    while (s!=null) {
      // -0.85   60.833332       2       01      010427
      String[] bits = s.split("\t");
      PW.println(bits[0]+"\t"+bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+translate.get(bits[4]));
      s = br.readLine();
    }
    br.close();
    PW.close();
    
  }
  */
  
  public void chinaIDS() throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(outPath +"popch_adm1.txt"));
    PrintWriter PW = new PrintWriter(new File(outPath + "fixed.txt"));
    String s = br.readLine();
    while (s!=null) {
      PW.println(s.substring(0, s.length()-2));
      s = br.readLine();
    }
    br.close();
    PW.close();
  }
  
  public static void main(String[] args) throws Exception {
    create_uk_adm3 CPUK = new create_uk_adm3();
    CPUK.run();
    //CPUK.easyIds();
    //CPUK.chinaIDS();
  }
}

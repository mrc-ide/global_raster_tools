package jobs.e6_Pop_Africa_2016;

import java.io.File;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_pop_africa {
  private final static String landscanPath = "E:/Data/Census/Landscan2016/";
  private final static String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  private final static String outPath = "E:/PopAfrica2016/";
   
  
  public void run() throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(gadmPath, 2, null);
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
    
    String[] codes = new String[] { "01", "02", "03", "04", "05", "06", "07",
        "09", "10", "11", "22", "12", "50", "13", "14", "15", "16", "17", "18",
        "19", "20", "21", "23", "24", "25", "26", "27", "28", "29", "31", "32",
        "33", "34", "35", "37", "38", "40", "41", "42", "53", "43", "44", "45",
        "46", "47", "48", "49", "51", "52" };

    String[] popa_countries = new String[] { "Algeria", "Angola", "Benin",
        "Botswana", "Burkina_Faso", "Burundi", "Cameroon",
        "Central_African_Republic", "Chad", "Congo", "Cote_d'Ivoire",
        "Djibouti", "DRC", "Egypt", "Equatorial_Guinea", "Eritrea", "Ethiopia",
        "Gabon", "Gambia", "Ghana", "Guinea", "Guinea-Bissau", "Kenya",
        "Lesotho", "Liberia", "Libya", "Malawi", "Mali", "Mauritania",
        "Morocco", "Mozambique", "Namibia", "Niger", "Nigeria", "Rwanda",
        "Senegal", "Sierra_Leone", "Somalia", "South_Africa", "South_Sudan",
        "Sudan", "Swaziland", "Tanzania", "Togo", "Tunisia", "Uganda",
        "Western_Sahara", "Zambia", "Zimbabwe" };
    
    String[] gadm36_countries = new String[] { "Algeria", "Angola", "Benin",
        "Botswana", "Burkina Faso", "Burundi", "Cameroon",
        "Central African Republic", "Chad", "Republic of Congo", "Côte d'Ivoire",
        "Djibouti", "Democratic Republic of the Congo", "Egypt", "Equatorial Guinea", "Eritrea", "Ethiopia",
        "Gabon", "Gambia", "Ghana", "Guinea", "Guinea-Bissau", "Kenya",
        "Lesotho", "Liberia", "Libya", "Malawi", "Mali", "Mauritania",
        "Morocco", "Mozambique", "Namibia", "Niger", "Nigeria", "Rwanda",
        "Senegal", "Sierra Leone", "Somalia", "South Africa", "South Sudan",
        "Sudan", "Swaziland", "Tanzania", "Togo", "Tunisia", "Uganda",
        "WesternSahara", "Zambia", "Zimbabwe" };
    

    String[] iso3 = new String[] { "DZA", "AGO", "BEN", "BWA", "BFA", "BDI",
        "CMR", "CAF", "TCD", "COG", "CIV", "DJI", "COD", "EGY", "GNQ", "ERI",
        "ETH", "GAB", "GMB", "GHA", "GIN", "GNB", "KEN", "LSO", "LBR", "LBY",
        "MWI", "MLI", "MRT", "MAR", "MOZ", "NAM", "NER", "NGA", "RWA", "SEN",
        "SLE", "SOM", "ZAF", "SSD", "SDN", "SWZ", "TZA", "TGO", "TUN", "UGA",
        "ESH", "ZMB", "ZWE" };
    
    // Compute a Mask for African countries.
    
    System.out.println("Creating Africa Mask");
    
    // Get list of admin units we're interested in.
    
    ArrayList<Integer> africa_units = new ArrayList<Integer>();
    ArrayList<Integer> popAfrica_countries = new ArrayList<Integer>();
    ArrayList<String> popAfrica_units_str = new ArrayList<String>();
    String current_adm0="";
    String current_adm1="";
    int adm1_no=1;
    int adm2_no=0;
   
    PrintWriter PW = new PrintWriter(outPath + "popAfrica_index.txt");
    for (int j=0; j<GRT.unit_names.size(); j++) {
      String[] name_bits = GRT.unit_names.get(j).split("\t");
      String[] num_bits = GRT.unit_numbers.get(j).split("\t");
      if (!current_adm0.equals(num_bits[0])) {
        // It's a new country
        adm1_no=1;
        adm2_no=0;
        current_adm0 = num_bits[0];
      } else if (!current_adm1.equals(num_bits[1])) {
        // Same country, new ADM1
        adm1_no++;
        adm2_no=0;
        current_adm1 = num_bits[1];
      }
      // Each line is a new ADM2.
      adm2_no++;
      boolean found=false;
      
      for (int i=0; i<gadm36_countries.length; i++) {
        if (name_bits[0].equals(gadm36_countries[i])) {
          found=true;
          africa_units.add(j);
          popAfrica_countries.add(Integer.parseInt(codes[i]));
          popAfrica_units_str.add(codes[i] + 
                                  ((adm1_no<10)?"0":"")+String.valueOf(adm1_no)+
                                  ((adm2_no<10)?"0":"")+String.valueOf(adm2_no));
          PW.println(popAfrica_units_str.get(popAfrica_units_str.size()-1)+"\t"+popa_countries[i]+"\t"+name_bits[1]+"\t"+name_bits[2]);
          i = gadm36_countries.length;
        }
      }
      if (!found) popAfrica_units_str.add("");
    }
    PW.close();
    System.out.println("Loading landscan");
    float[][] landscan = new float[21600][43200];
    FileChannel fc = FileChannel.open(Paths.get(landscanPath+"lspop.flt"));
    for (int j=0; j<21600; j++) {
      MappedByteBuffer mbb = fc.map(MapMode.READ_ONLY, (long) (4L*j*43200L), 43200L*4L);
      for (int i = 0; i <43200; i++) {
        landscan[j][i] = Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(mbb.getFloat())));
      }
      mbb.clear();
    }
    fc.close();
    System.out.println("Writing");
    PW = new PrintWriter(outPath+"popAfrica.txt");
    String s;
    for (int j=0; j<21600; j++) {
      if (j%100==0) System.out.println(j+"/21600");
      for (int i=0; i<43200; i++) {
        if ((GRT.map[j][i]>=0) && (landscan[j][i]>0)) {
          s = popAfrica_units_str.get(GRT.map[j][i]);
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
  
  public static void main(String[] args) throws Exception {
    create_pop_africa CPA = new create_pop_africa();
    CPA.run();
  }
}

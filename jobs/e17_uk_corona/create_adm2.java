package jobs.e17_uk_corona;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_adm2 {
  private final static String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  private final static String outPath = "E:/Jobs/UK-Corona/";
  
  //private final static String[] codes = new String[] { "10","""};
  
  // Europe
  /*
  
  private final static int max_level = 1;
  private final static String[] popa_countries = new String[] { "Albania", "Andorra", "Austria", "Belarus", "Belgium", "Bosnia and Herzegovina", "Bulgaria", "Croatia",
                                                                "Czech Republic", "Denmark", "Estonia", "Finland", "France", "Germany", "Gibraltar", "Greece", "Hungary",
                                                                "Iceland", "Ireland", "Italy","Kosovo", "Latvia", "Liechtenstein", "Lithuania", "Luxembourg", "Macedonia", 
                                                                "Malta", "Moldova", "Monaco", "Montenegro", "Netherlands", "Norway", "Poland", "Portugal", "Romania", "Russia",
                                                                "San Marino", "Serbia", "Slovakia", "Slovenia", "Spain", "Sweden", "Switzerland", "Ukraine", "United Kingdom",
                                                                "Vatican City"};
  
  private final static String[] gadm36_countries = new String[] { "Albania", "Andorra", "Austria", "Belarus", "Belgium", "Bosnia and Herzegovina", "Bulgaria", "Croatia",
      "Czech Republic", "Denmark", "Estonia", "Finland", "France", "Germany", "Gibraltar", "Greece", "Hungary",
      "Iceland", "Ireland", "Italy", "Kosovo", "Latvia", "Liechtenstein", "Lithuania", "Luxembourg", "Macedonia", 
      "Malta", "Moldova", "Monaco", "Montenegro", "Netherlands", "Norway", "Poland", "Portugal", "Romania", "Russia",
      "San Marino", "Serbia", "Slovakia", "Slovenia", "Spain", "Sweden", "Switzerland", "Ukraine", "United Kingdom",
      "Vatican City"};
      
  private final static String index_file_out = "pop_eur_adm"+max_level+"_index.txt";
  private final static String pop_file_out = "pop_eur_adm"+max_level+".txt";

  */
  // USA/Canada/ US territories
  /*
  
  private final static int max_level = 1;
  private final static String[] popa_countries = new String[] { "Canada", "United States", "American Samoa", "Guam", "Puerto Rico", "Virgin Islands, U.S."};
  private final static String[] gadm36_countries = new String[] { "Canada", "United States", "American Samoa", "Guam", "Puerto Rico", "Virgin Islands, U.S."};
  private final static String index_file_out = "wpop_us_more_adm"+max_level+"_index.txt";
  private final static String pop_file_out = "wpop_us_more_adm"+max_level+".txt";
  
  */
  // Africa starts here...
  /*
  private final static int max_level = 1;
  private final static String[] popa_countries = new String[] { "Nigeria"};
  private final static String[] gadm36_countries = new String[] { "Nigeria"};
  private final static String index_file_out = "wpop_nga_adm"+max_level+"_index.txt";
  private final static String pop_file_out = "wpop_nga_adm"+max_level+".txt";
  */
  // Bermuda, hey ho.
  
  private final static int max_level = 1;
  private final static String[] popa_countries = new String[] { "Bermuda"};
  private final static String[] gadm36_countries = new String[] { "Bermuda"};
  private final static String index_file_out = "wpop_bmu_adm"+max_level+"_index.txt";
  private final static String pop_file_out = "wpop_bmu_adm"+max_level+".txt";
  
  
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
    
    ArrayList<Integer> pop_countries = new ArrayList<Integer>();
    ArrayList<String> pop_units_str = new ArrayList<String>();
    
    String[] current_admin = new String[max_level + 1];
    for (int i=0; i <= max_level; i++)
      current_admin[i] = GRT.unit_numbers.get(0).split("\t")[i];
    int[] admin_no = new int[max_level + 1];
    admin_no[max_level]=0;
    for (int i=0; i<max_level; i++) admin_no[i]=1;
    ArrayList<String> index_out = new ArrayList<String>();
    
    PrintWriter PW = new PrintWriter(outPath + index_file_out);
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
    float[][] pop_raster = GRT.loadFloatGrid("E:/Data/Census/WorldPop/global/worldpop2020global.bil", 43200, 21600, 43200, 18720, 0, 720);
    
    System.out.println("Writing");
    PW = new PrintWriter(outPath+pop_file_out);
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
  
  // All good so far. Now we want to convert ADM2 to ADM1, applying regional groups to UK, which is odd.
  
  static final List<String> eng_names = Arrays.asList(new String[] {
      "East Midlands", "East of England", "London","North East","North West","South East","South West","West Midlands","Yorkshire and the Humber"});
  
  static final List<String> eng_em = Arrays.asList(new String[] {"Derbyshire", "Derby", "Nottinghamshire", "Nottingham", "Lincolnshire", "Leicestershire", 
                                                                    "Leicester", "Rutland", "Northamptonshire"}); 
  static final List<String> eng_ee = Arrays.asList(new String[] {"Thurrock", "Southend-on-sea", "Essex", "Hertfordshire", "Luton", "Bedford", "Bedfordshire", 
                                                                    "Central Bedfordshire", "Cambridgeshire", "Peterborough", "Norfolk", "Suffolk",
                                                                    "Southend-on-Sea"});
  static final List<String> eng_l = Arrays.asList(new String[] {"Greater London"});
  
  static final List<String> eng_ne = Arrays.asList(new String[] {"Northumberland", "Tyne and Wear", "County Durham", "Darlington", "Hartlepool",
                                                                    "Stockton-on-Tees", "Redcar and Cleveland", "Middlesbrough", "Durham",
                                                                    "Gateshead", "Newcastle upon Tyne", "North Tyneside", "South Tyneside",
                                                                    "Sunderland"});
  
  static final List<String> eng_nw = Arrays.asList(new String[] {"Cheshire East", "Cheshire West and Chester", "Halton", "Warrington", "Cumbria",
                                                                    "Greater Manchester", "Lancashire", "Blackpool", "Blackburn with Darwen", "Merseyside",
                                                                    "Bolton", "Bury", "Knowsley", "Manchester", "Oldham", "Rochdale",
                                                                    "Saint Helens", "Salford", "Sefton", "Stockport", "Tameside", "Trafford", "Wigan",
                                                                    "Wirral"});
  
  static final List<String> eng_se = Arrays.asList(new String[] {"Berkshire", "Buckinghamshire", "Milton Keynes", "East Sussex", "Brighton and Hove",
                                                                    "Hampshire", "Southampton", "Portsmouth", "Isle of Wight", "Kent", "Medway",
                                                                    "Oxfordshire", "Surrey", "West Sussex", "Bracknell", "Bracknell Forest",
                                                                    "Reading", "Slough", "West Berkshire", "Windsor and Maidenhead", "Wokingham"});
  
  static final List<String> eng_sw = Arrays.asList(new String[] {"Bath and North East Somerset", "North Somerset", "Bristol", "South Gloucestershire",
                                                                    "Gloucestershire", "Swindon", "Wiltshire", "Dorset", "Bournemouth", "Somerset", "Devon", 
                                                                    "Torbay", "Cornwall", "Plymouth", "Isles of Scilly", "Poole"});
  
  static final List<String> eng_wm = Arrays.asList(new String[] {"Herefordshire", "Shropshire", "Telford and Wrekin", "Staffordshire",
                                                                    "Stoke-on-Trent", "Warwickshire", "West Midlands", "Worcestershire",
                                                                    "Birmingham", "Coventry", "Dudley", "Sandwell", "Solihull", "Walsall",
                                                                    "Wolverhampton"});
  
  static final List<String> eng_yh = Arrays.asList(new String[] {"South Yorkshire", "West Yorkshire", "North Yorkshire", "York", "East Riding of Yorkshire",
                                                                 "Kingston upon Hull", "North Lincolnshire", "North East Lincolnshire",
                                                                 "Barnsley", "Bradford", "Calderdale", "Doncaster", "Kirklees", "Leeds",
                                                                 "Rotherham", "Sheffield", "Wakefield"});
  
  
  // Wales
  
  static final List<String> wal_names = Arrays.asList(new String[] {"East Wales", "West Wales and The Valleys"});
  
  
  static final List<String> wal_ew = Arrays.asList(new String[] {"Monmouthshire", "Newport", "Cardiff", "Vale of Glamorgan",
                                                                   "Flintshire", "Wrexham", "Powys"});
  
  static final List<String> wal_wwtv = Arrays.asList(new String[] {"Anglesey", "Conwy", "Denbighshire", "Gwynedd", 
      "Ceredigion", "Carmarthenshire", "Pembrokeshire",
      "Merthyr Tydfil", "Rhondda, Cynon, Taff",
      "Blaenau Gwent", "Caerphilly", "Torfaen",
      "Bridgend", "Neath Port Talbot", "Swansea"});

  
  // Scotland
  
  static final List<String> sco_names = Arrays.asList(new String[] {
      "North Eastern Scotland", "Highlands and Islands", "Eastern Scotland", "West Central Scotland", "Southern Scotland"});
  
  static final List<String> sco_nes = Arrays.asList(new String[] {"Aberdeen", "Aberdeenshire"});
  
  static final List<String> sco_hi = Arrays.asList(new String[] {"Argyll and Bute", "Eilean Siar", "Highland", "Moray", "Orkney Islands", 
                                                                 "Shetland Islands"});

   
  static final List<String> sco_es = Arrays.asList(new String[] {"Angus", "Clackmannanshire", "Dundee", "East Lothian", "Edinburgh", "Falkirk", 
                                                                 "Fife", "Midlothian", "Perthshire and Kinross", "Stirling",
                                                                 "West Lothian"});
  
  static final List<String> sco_wcs = Arrays.asList(new String[] {"East Dunbartonshire", "Glasgow", "West Dunbartonshire", "Inverclyde",
                                                                  "East Renfrewshire", "Renfrewshire", "North Lanarkshire"});
  
  static final List<String> sco_ss = Arrays.asList(new String[] {"Scottish Borders", "Dumfries and Galloway","East Ayrshire",
                                                                 "North Ayrshire", "South Ayrshire", "South Lanarkshire"});
  
  // Northern Ireland
  
  static final List<String> ni_names = Arrays.asList(new String[] {"Northern Ireland"});
  
  static final List<String> ni_ni = Arrays.asList(new String[] {"Antrim and Newtownabbey", "Armagh, Banbridge and Craigavon", "Belfast", "Causeway Coast and Glens", 
                                                                "Derry and Strabane","Fermanagh and Omagh","Lisburn and Castlereagh","Mid and East Antrim", 
                                                                "Mid Ulster","Newry, Mourne and Down","North Down and Ards"});
  
  
  int lookup(String place, ArrayList<List<String>> region_members) {
    int count=0;
    int index=-1;
    for (int i=0; i<region_members.size(); i++) {
      if (region_members.get(i).contains(place)) {
        count++;
        index=i;
      }
    }
    if (count==0) return -1;
    else if (count>1) return -1;
    else return index+1;
  }
  
  void part2() throws Exception {
    ArrayList<List<String>> eng_all = new ArrayList<List<String>>();
    eng_all.add(eng_em); eng_all.add(eng_ee); eng_all.add(eng_l); eng_all.add(eng_ne); eng_all.add(eng_nw); eng_all.add(eng_se); eng_all.add(eng_sw);
    eng_all.add(eng_wm); eng_all.add(eng_yh);
    
    ArrayList<List<String>> wal_all = new ArrayList<List<String>>();
    wal_all.add(wal_ew); wal_all.add(wal_wwtv);
    
    ArrayList<List<String>> sco_all = new ArrayList<List<String>>();
    sco_all.add(sco_nes); sco_all.add(sco_hi); sco_all.add(sco_es); sco_all.add(sco_wcs); sco_all.add(sco_ss);
    
    ArrayList<List<String>> ni_all = new ArrayList<List<String>>();
    ni_all.add(ni_ni);

    
    HashMap<String, String> translate = new HashMap<String, String>();
    BufferedReader br = new BufferedReader(new FileReader(outPath+index_file_out));
    PrintWriter PW = new PrintWriter(new File(outPath+index_file_out+"2"));
    String s = br.readLine();
    boolean written_uk_units = false;
    String uk = "";
    int unit = 0;
    while (s!=null) {
      String[] bits = s.split("\t");
      if (!bits[1].equals("United Kingdom")) {
        translate.put(bits[0], bits[0].substring(0,4));
        PW.println(bits[0].substring(0,4)+"\t"+bits[1]+"\t"+bits[2]);
      } else {
        if (!written_uk_units) {
          uk = bits[0].substring(0,2);
          for (int i=0; i<eng_names.size(); i++) {
            unit++;
            PW.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+eng_names.get(i));
            System.out.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+eng_names.get(i));
          }
          for (int i=0; i<ni_names.size(); i++) {
            unit++;
            PW.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+ni_names.get(i));
            System.out.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+ni_names.get(i));
            
          }
          for (int i=0; i<sco_names.size(); i++) {
            unit++;
            PW.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+sco_names.get(i));
            System.out.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+sco_names.get(i));
            
           }
          for (int i=0; i<wal_names.size(); i++) {
            unit++;
            PW.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+wal_names.get(i));
            System.out.println(uk + ((unit<10)?"0":"")+unit+"\tUnited_Kingdom\t"+wal_names.get(i));
          }
          written_uk_units = true;
        }
        
        // Now add to lookup table, without writing new data.
        int region = -1;
        if (bits[2].equals("England")) region = lookup(bits[3], eng_all);
        else if (bits[2].equals("Northern Ireland")) region = eng_names.size() + lookup(bits[3], ni_all);
        else if (bits[2].equals("Scotland")) region = eng_names.size()+ ni_names.size() + lookup(bits[3], sco_all);
        else if (bits[2].equals("Wales")) region = eng_names.size() + ni_names.size() + sco_names.size() + lookup(bits[3], wal_all);
          
        System.out.println(bits[2]+"\t"+bits[3] +" Moves into "+region);
        translate.put(bits[0], uk + ((region<10)?"0":"")+region);
        
      }
      s = br.readLine();
    }
    br.close();
    PW.close();
    
    br = new BufferedReader(new FileReader(outPath+pop_file_out));
    PW = new PrintWriter(new File(outPath+pop_file_out+"2"));
    s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      String repl = translate.get(bits[4]);
      if (repl==null) {
        System.out.println("NULL! "+bits[4]);
      }
      PW.println(bits[0]+"\t"+bits[1]+"\t"+bits[2]+"\t"+bits[3]+"\t"+repl);
      s = br.readLine();
    }
    br.close();
    PW.close();
    
    // Finally, remove duplicates, and replace spaces with underscore in index file.
    
    br = new BufferedReader(new FileReader(outPath+index_file_out+2));
    PW = new PrintWriter(new File(outPath+index_file_out+"3"));
    String previous = "Banana";
    s = br.readLine();
    while (s!=null) {
      System.out.println(s);
      if (!s.equals(previous)) {
        String[] bits = s.split("\t");
        PW.println(bits[0]+"\t"+bits[1].replaceAll(" ", "_")+"\t"+bits[2].replaceAll(" ",  "_"));
        previous = s;
      }
      s = br.readLine();
    }
    br.close();
    PW.close();
  }
  
  void cleanupExtraUS() throws Exception {
    HashMap<String, String> map = new HashMap<String,String>();
    map.put("1102","560100");
    for (int i=1201; i<=1204; i++) map.put(String.valueOf(i), "560200");
    for (int i=1301; i<=1319; i++) map.put(String.valueOf(i), "560300");
    map.put("1112","560400");
    for (int i=1401; i<=1478; i++) map.put(String.valueOf(i), "560500");
    for (int i=1501; i<=1503; i++) map.put(String.valueOf(i), "560600");
    BufferedReader br = new BufferedReader(new FileReader(outPath+pop_file_out));
    PrintWriter PW = new PrintWriter(new File(outPath+pop_file_out+"2"));
    String s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      if (map.containsKey(bits[4])) {
        PW.println(bits[0]+"\t"+bits[1]+"\t"+bits[2]+"\t56\t"+map.get(bits[4]));
      }
      s = br.readLine();
    }
    br.close();
    PW.close();
    PW = new PrintWriter(new File(outPath+index_file_out+"2"));
    PW.println("560100\tUnited_States\tAlaska");
    PW.println("560200\tUnited_States\tAmerican_Samoa");
    PW.println("560300\tUnited_States\tGuam");
    PW.println("560400\tUnited_States\tHawaii");
    PW.println("560500\tUnited_States\tPuerto_Rico");
    PW.println("560600\tUnited_States\tVirgin_Islands_US");
    PW.close();
    
  }
  
  public static void main(String[] args) throws Exception {
    create_adm2 CAD2 = new create_adm2();
    CAD2.run();
    //
    
    // Clean up extra American File...
    //CAD2.cleanupExtraUS();
    System.exit(0);
    
    
    //CAD2.part2();
    BufferedImage bi = new BufferedImage(4320,2160, BufferedImage.TYPE_3BYTE_BGR);
    BufferedReader br = new BufferedReader(new FileReader("X:\\Europe_Spatial_Data\\MSGH\\wpop_usacan.txt"));
    String s = br.readLine();
    HashMap<Integer, Integer> cols = new HashMap<Integer, Integer>();
    while (s!=null) {
      String[] bits = s.split("\t");
      double lon = Double.parseDouble(bits[0]);
      double lat = Double.parseDouble(bits[1]);
      int xpix = (int) Math.round((lon+180)*12);
      int ypix = (int) Math.round((90-lat)*12);
      Integer unit = Integer.parseInt(bits[4]);
      if (!cols.containsKey(unit)) {
        cols.put(unit,  new Color((int)(Math.random()*255),(int)(Math.random()*255),(int)(Math.random()*255)).getRGB());
      }
      bi.setRGB(xpix,  ypix,  cols.get(unit));
      s = br.readLine();
    }
    br.close();
    ImageIO.write(bi,  "PNG",  new File("E:/usacan.png"));
    
  }
}

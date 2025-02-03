package jobs.e17_uk_corona;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class extract_europe_country {
  
  public static void main(String[] args) throws Exception {
    
    // Change these 3.
    
    String country="Sweden";
    String code = "SWE";
    String country_code = "55";
    String dest_code = "55";
       
    BufferedReader br = new BufferedReader(new FileReader("X:/Europe_Spatial_Data/All_Europe/pop_eur_adm1.txt"));
    PrintWriter PW = new PrintWriter(new File("X:/Europe_Spatial_Data/All_Europe/"+country+"/pop_"+code+"_adm1.txt"));
    String s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      if (bits[3].equals(country_code)) {
        String admin = dest_code + bits[4].substring(2) + "01";
        PW.println(bits[0]+"\t"+bits[1]+"\t"+bits[2]+"\t"+dest_code+"\t"+admin);
      }
      s = br.readLine();
    }
    PW.close();
    br.close();
    
    br = new BufferedReader(new FileReader("X:/Europe_Spatial_Data/All_Europe/pop_eur_adm1_index.txt"));
    PW = new PrintWriter(new File("X:/Europe_Spatial_Data/All_Europe/"+country+"/pop_"+code+"_adm1_index.txt"));
    s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      if (bits[0].startsWith(country_code)) {
        String admin = dest_code+bits[0].substring(2)+"01";
        PW.println(admin+"\t"+bits[1]+"\t"+bits[2]);
      }
      s=br.readLine();
    }
    
    PW.close();
    br.close();
    
  }
}

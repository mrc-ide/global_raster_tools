package jobs.e17_uk_corona;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class FixPosNegLon {
  public static void main(String[] args) throws Exception {
    int pop=0;
    BufferedReader br = new BufferedReader(new FileReader("C:/Files/Dev/covid-sim/data/populations/wpop_eur_russiafix.txt"));
    PrintWriter PW = new PrintWriter(new File("C:/Files/Dev/covid-sim/data/populations/wpop_eur_russiafix2.txt"));
    String s = br.readLine();
    while (s!=null) {
      String[] bits = s.split("\t");
      int country = Integer.parseInt(bits[3]);
      if (country==45) {
        double lon = Double.parseDouble(bits[0]);
        if (lon<0) {
          System.out.println(s);
          pop += Integer.parseInt(bits[2]);
        } else PW.println(s);
      } else PW.println(s);
      s = br.readLine();
    }
    System.out.println("Pop to remove = "+pop);
    br.close();
    PW.close();
  }
}

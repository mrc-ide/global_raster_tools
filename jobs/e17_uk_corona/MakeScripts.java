package jobs.e17_uk_corona;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MakeScripts {
  public static void main(String[] args) throws Exception {

    // 26/03/2020 22:19 489,294,060 wpop_eur.txt
    // 26/03/2020 22:12 21,099 wpop_eur_index.txt
    // 26/03/2020 22:21 188,346,867 wpop_usacan.txt
    // 26/03/2020 22:24 1,981 wpop_usacan_index.txt

    String path = "X:/Europe_spatial_data/MSGH/";

    System.out.println("Loading...");
    ArrayList<String[]> units = new ArrayList<String[]>();
    BufferedReader br = new BufferedReader(new FileReader(path + "wpop_eur_index.txt"));
    String s = br.readLine();
    while (s != null) {
      units.add(s.split("\t"));
      s = br.readLine();
    }
    br.close();
    br = new BufferedReader(new FileReader(path + "wpop_usacan_index.txt"));
    s = br.readLine();
    while (s != null) {
      units.add(s.split("\t"));
      s = br.readLine();
    }
    br.close();
    int[] country_pops = new int[66];
    br = new BufferedReader(new FileReader(path + "wpop_eur.txt"));
    s = br.readLine();
    while (s != null) {
      String[] bits = s.split("\t");
      country_pops[Integer.parseInt(bits[3])] += Integer.parseInt(bits[2]);
      s = br.readLine();
    }
    br.close();
    br = new BufferedReader(new FileReader(path + "wpop_usacan.txt"));
    s = br.readLine();
    while (s != null) {
      String[] bits = s.split("\t");
      country_pops[Integer.parseInt(bits[3])] += Integer.parseInt(bits[2]);
      s = br.readLine();
    }
    br.close();
    int[] extra_us_territory_pops = new int[6];
    br = new BufferedReader(new FileReader(path + "wpop_us_terr.txt"));
    s = br.readLine();
    while (s != null) {
      String[] bits = s.split("\t");
      extra_us_territory_pops[(Integer.parseInt(bits[4])/100)-5601] += Integer.parseInt(bits[2]);
      s = br.readLine();
    }
    br.close();
    br= new BufferedReader(new FileReader(path + "holidays/holidays.csv"));
    ArrayList<String[]> holidays = new ArrayList<String[]>();
    s = br.readLine();
    while (s != null) {
      while (s.endsWith("\t")) s = s.substring(0, s.length()-1);
      String[] bits = s.split("\t");
      holidays.add(bits);
      s = br.readLine();
    }
    br.close();

    ArrayList<String[]> hhsize = new ArrayList<String[]>();
    br = new BufferedReader(new FileReader(path + "hhsize_distrib/europe_hhsize_2018.csv"));
    s = br.readLine();
    while (s != null) {
      hhsize.add(s.split("\t"));
      s = br.readLine();
    }
    br.close();

    ArrayList<String[]> fte = new ArrayList<String[]>();
    br = new BufferedReader(new FileReader(path + "fte/education2.csv"));
    s = br.readLine();
    while (s != null) {
      fte.add(s.split("\t"));
      s = br.readLine();
    }
    br.close();

    ArrayList<String[]> teachers = new ArrayList<String[]>();
    br = new BufferedReader(new FileReader(path + "teachers/teachers.csv"));
    s = br.readLine();
    while (s != null) {
      teachers.add(s.split("\t"));
      s = br.readLine();
    }
    br.close();

    ArrayList<String[]> employ = new ArrayList<String[]>();
    br = new BufferedReader(new FileReader(path + "work/employment.csv"));
    s = br.readLine();
    while (s != null) {
      employ.add(s.split("\t"));
      s = br.readLine();
    }
    br.close();

    BufferedReader br_pop = new BufferedReader(new FileReader(path + "pop_distrib/europe_pops.txt"));
    String pop = br_pop.readLine();
    while (pop != null) {
      String[] pop_bits = pop.split("\t");
      if (!pop_bits[0].equals("United_States")) {
        System.out.println(pop_bits[0]);
        PrintWriter PW = new PrintWriter(new File(path + "final/" + pop_bits[0] + "_admin.txt"));
        String country = pop_bits[0];
        if (country.indexOf("(") >= 0) {
          country = country.substring(0, country.indexOf("("));
        }
        PW.println("[Number of seed locations]");
        PW.println("1");
        PW.println();
        PW.println("[Initial number of infecteds]");
        PW.println("100");
        PW.println();
        PW.println();
        PW.println("[Administrative unit to seed initial infection into]");
        PW.println("0");
        PW.println();
        PW.println();
        PW.println("[Population size]");
        boolean done = false;
        for (int i = 0; i < units.size(); i++) {
          if (units.get(i)[1].equals(country)) {
            PW.println(country_pops[Integer.parseInt(units.get(i)[0]) / 10000]);
            done = true;
            break;
          }
        }
        if (!done) {
          if (country.equals("Alaska")) PW.println(extra_us_territory_pops[0]);
          else if (country.equals("American_Samoa")) PW.println(extra_us_territory_pops[1]);
          else if (country.equals("Guam")) PW.println(extra_us_territory_pops[2]);
          else if (country.equals("Hawaii")) PW.println(extra_us_territory_pops[3]);
          else if (country.equals("Puerto_Rico")) PW.println(extra_us_territory_pops[4]);
          else if (country.equals("Virgin_Islands_US")) PW.println(extra_us_territory_pops[5]);
          else {
            System.out.println("ERROR, pop for " + country);
            System.exit(0);
          }
        }
        PW.println("[ONS pop proj]");
        PW.println();
        PW.println("[Fix population size at specified value]");
        PW.println("1");
        PW.println();
        PW.println("[Number of countries to include]");
        PW.println("1");
        PW.println();
        PW.println("[List of names of countries to include]");
        PW.println(country);
        PW.println();
        PW.println("[Number of level 1 administrative units to include]");
        PW.println(0); // WHY IS THIS ZERO in the example?
        PW.println();
        PW.println("[List of level 1 administrative units to include]");
        boolean first = true;
        for (int i = 0; i < units.size(); i++) {
          if (units.get(i)[1].equals(country)) {
            if (!first) PW.print("\t");
            PW.print(units.get(i)[2]);
            first = false;
          }
        }
        PW.println();
        PW.println();
        PW.println("[Codes and country/province names for admin units]");
        for (int i = 0; i < units.size(); i++) {
          if (units.get(i)[1].equals(country)) {
            PW.println(units.get(i)[0] + "\t" + units.get(i)[1] + "\t" + units.get(i)[2]);
          }
        }
        PW.println();
        PW.println("[Mask for level 1 administrative units]");
        PW.println("100000");
        PW.println();
        PW.println("[Divisor for level 1 administrative units]");
        PW.println("100");
        PW.println();
        PW.println("[Divisor for countries]");
        PW.println("100000");
        PW.println();
        PW.println("[Correct administrative unit populations]");
        PW.println("0");
        PW.println();
        PW.println("[Age distribution of population]");
        PW.print(pop_bits[1]);
        for (int i = 2; i < pop_bits.length; i++) PW.print("\t" + pop_bits[i]);
        PW.println();
        PW.println("[ONS proj for 2020]");
        PW.println();
        PW.println();
        PW.println("[Household size distribution]");
        done = false;
        for (int i = 0; i < hhsize.size(); i++) {
          if (hhsize.get(i)[0].equals(country)) {
            PW.print(hhsize.get(i)[1]);
            for (int j = 2; j < hhsize.get(i).length; j++) PW.print("\t" + hhsize.get(i)[j]);
            PW.println();
            done = true;
          }
        }
        if (!done) {
          System.out.println("ERROR, hh for " + country);
          System.exit(0);
        }
        PW.println("(ONS 2020 with IPUMS and national statistics)");
        PW.println("");
        PW.println("[Correct age distribution after household allocation to exactly match specified demography]");
        PW.println("1");
        PW.println("");
        PW.println("[Include places]");
        PW.println("1");
        PW.println();
        PW.println("[Place overlap matrix]");
        PW.println("1 0 0 0");
        PW.println("0 1 0 0");
        PW.println("0 0 1 0");
        PW.println("0 0 0 1");
        PW.println();
        PW.println("(note this isn't used - currently assume identity matrix)");
        PW.println();
        PW.println("[Number of types of places]");
        PW.println("4");
        PW.println();
        int row_fted = -1;
        for (int i = 0; i < fte.size(); i++) {
          if (fte.get(i)[0].equals(country)) {
            row_fted = i;
            break;
          }
        }
        if (row_fted == -1) {
          System.out.println("Error, ft-ed for " + country);
          System.exit(0);
        }

        int row_tch = -1;
        for (int i = 0; i < teachers.size(); i++) {
          if (teachers.get(i)[0].equals(country)) {
            row_tch = i;
            break;
          }
        }
        if (row_tch == -1) {
          System.out.println("Error, no teachers for " + country);
          System.exit(0);
        }

        int row_emp = -1;
        for (int i = 0; i < employ.size(); i++) {
          if (employ.get(i)[0].equals(country)) {
            row_emp = i;
            break;
          }
        }
        if (row_emp == -1) {
          System.out.println("Error, no employment for " + country);
          System.exit(0);
        }
      
        String ed3_5 = fte.get(row_fted)[1];
        String ed5_11 = fte.get(row_fted)[2];
        String ed11_16 = fte.get(row_fted)[3];
        String ed16_18 = fte.get(row_fted)[4];
        String ed18_21 = fte.get(row_fted)[5];
        String uni21_65 = fte.get(row_fted)[6]; // Includes teachers and students
      
      
        String wp18_21 = String.valueOf(1.0 - Double.parseDouble(ed18_21));
        String wp16_18 = String.valueOf(1.0 - Double.parseDouble(ed16_18));
      
        String teachers_p = String.valueOf(teachers.get(row_tch)[1]);
        String teachers_s = String.valueOf(teachers.get(row_tch)[2]);
        String teachers_u = String.valueOf(teachers.get(row_tch)[3]);
      
        String wp21_65 = String.valueOf(Double.parseDouble(employ.get(row_emp)[4])- (
                         Double.parseDouble(teachers_u) + Double.parseDouble(teachers_p) + Double.parseDouble(teachers_s)));

        PW.println("[Proportion of age group 1 in place types]");
        PW.println(ed5_11 + "\t" + ed11_16 + "\t0\t" + wp18_21);
        PW.println();
        PW.println("[Minimum age for age group 1 in place types]");
        PW.println("5\t11\t18\t16\t");
        PW.println();
        PW.println("[Maximum age for age group 1 in place types]");
        PW.println("11\t16\t65\t18\t");
        PW.println();
        PW.println("[Proportion of age group 2 in place types]");
        PW.println(ed3_5 + "\t" + ed16_18 + "\t" + ed18_21 + "\t" + wp16_18);
        PW.println();
        PW.println("[Minimum age for age group 2 in place types]");
        PW.println("3\t16\t18\t18");
        PW.println();
        PW.println("[Maximum age for age group 2 in place types]");
        PW.println("5\t18\t21\t21");
        PW.println();
        PW.println("[Proportion of age group 3 in place types]");
        PW.println(teachers_p + "\t" + teachers_s + "\t" + uni21_65 + "\t" + wp21_65);
        PW.println();
        PW.println("[Minimum age for age group 3 in place types]");
        PW.println("21\t21\t21\t21");
        PW.println();
        PW.println("[Maximum age for age group 3 in place types]");
        PW.println("65\t65\t65\t65\t");
        PW.println();
        PW.println("[Kernel shape params for place types]");
        PW.println("3 3 3 3");
        PW.println("");
        PW.println("");

        PW.println("[Kernel scale params for place types]");
        PW.println("4000  4000  4000  4000");
        PW.println("");

        PW.println("[Mean size of place types]");
        PW.println("230 1010  3300  14.28");
        PW.println("");

        PW.println("(inc teachers)");
        PW.println("");

        PW.println("[Number of closest places people pick from (0=all) for place types]");
        PW.println("3 3 6 0");
        PW.println("");

        PW.println("[Param 1 of place group size distribution]");
        PW.println("25  25  100 10");
        PW.println("");

        PW.println("[Power of place size distribution]");
        PW.println("0 0 0 1.34");
        PW.println("");

        PW.println("[Offset of place size distribution]");
        PW.println("0 0 0 5.35");
        PW.println("");

        PW.println("[Maximum of place size distribution]");
        PW.println("0 0 0 5927");
        PW.println("");

        PW.println("[Kernel type]");
        PW.println("2");
        PW.println("");

        PW.println("[Kernel scale]");
        PW.println("4000");
        PW.println("");

        PW.println("[Kernel Shape]");
        PW.println("3");
        PW.println("");

        PW.println("===================================");

        PW.println("[Include holidays]");
        PW.println("0");
        PW.println("");

        PW.println("[Proportion of places remaining open during holidays by place type]");
        PW.println("0 0 1 1");
        PW.println("");
        
        int row_hol=-1;
        for (int i = 0; i < units.size(); i++) {
          if (holidays.get(i)[0].equals(country)) {
            row_hol=i;
            break;
          }
        }
        
        if (row_hol==-1) { System.out.println("Error - couldn't find "+country+" in holiday file"); System.exit(0);; }
        String[] bits = holidays.get(row_hol);
        int no_hol = (bits.length - 1)/2; // Format: Country, <start+1, duration>
        
        PW.println("[Number of holidays]");
        PW.println(no_hol * 2);
        PW.println("");

        PW.println("[Holiday start times]");

        // Sort by start date...
        
        int[] hol_start = new int[no_hol];
        int[] hol_dur = new int[no_hol];
        for (int i=0; i<no_hol; i++) {
          hol_start[i] = Integer.parseInt(bits[1+(i*2)])-1;
          hol_dur[i] = Integer.parseInt(bits[2+(i*2)]);
        }
        
        for (int i=0; i<no_hol - 1; i++) {
          int earliest = i;
          for (int j=i+1; j<no_hol; j++) {
            if (hol_start[j]<hol_start[earliest]) {
              earliest = j;
            }
          }
          if (i!=earliest) {
            int temp = hol_start[i];
            hol_start[i] = hol_start[earliest];
            hol_start[earliest] = temp;
            temp = hol_dur[i];
            hol_dur[i] = hol_dur[earliest];
            hol_dur[earliest] = temp;
          }
        }
        
        for (int i=0; i<no_hol; i++) PW.print(hol_start[i]+"\t");
        for (int i=0; i<no_hol; i++) PW.print(364+hol_start[i]+"\t");
        PW.println();
        PW.println("");
        PW.println("");

        PW.println("[Holiday durations]");
        for (int i=0; i<no_hol; i++) PW.print(hol_dur[i]+"\t");
        for (int i=0; i<no_hol; i++) PW.print(hol_dur[i]+"\t");
        PW.println("");
        PW.println("");
        PW.println("===================================");
        PW.println("");
        PW.close();
        br.close();
      }
      pop = br_pop.readLine();
    }
    br_pop.close();
  }
}

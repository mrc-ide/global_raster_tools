package jobs.e29_country_gadm41;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Hashtable;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Work {
  private final static String gadm_path = "Y:/Shapefiles/GADM/4.1/countries";
  private final static String out_path = "D:/Jobs/e29/";
  private final static String ls_path = "Y:/Landscan/2022/landscan-global-2022.bil"; 
  
public void run(String[] args) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    if (!new File(out_path+"map.bin").exists()) {
    	System.out.println("Load polys");
      GRT.loadPolygonFolder(gadm_path, 2, "4.1");
      System.out.println("Make map");
      GRT.makeMap();
      System.out.println("Save map");
      GRT.saveMapFile(out_path+"map.bin");
      GRT.saveUnits(out_path+"units.txt");
    } else {
      GRT.loadUnits(out_path+"units.txt");
      System.out.println("Load map");
      GRT.loadMapFile(out_path+"map.bin");
    }
    
    int[] unit_pops = new int[GRT.unit_names.size()];
    System.out.println("Load landscan");
    int[][] landscan = GRT.loadIntGrid(ls_path, 43200, 21600, 43200, 21600, 0, 0);
    System.out.println("Sum pops");
    for (int j=0; j<21600; j++)
      for (int i=0; i<43200; i++)
      	if (GRT.map[j][i] >= 0) unit_pops[GRT.map[j][i]] += landscan[j][i];

    BufferedReader br = new BufferedReader(new FileReader(out_path+"units.txt"));
    PrintWriter PW = new PrintWriter(new File(out_path+"unit_pops.txt"));
    String header = br.readLine();
    PW.println(header+"ls2022");
    String s = br.readLine();
    int i=0;
    while (s!= null) {
    	PW.println(s + "\t" + unit_pops[i]);
    	s = br.readLine();
    	i++;
    }
    br.close();
    PW.close();
  }

  // Rwanda extras

  public void rwanda() throws Exception {
  	GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadUnits(out_path+"units.txt");
    Hashtable<Integer, Integer> trans = new Hashtable<Integer, Integer>();
    String last_adm1 = "";
    String last_adm2 = "";
    int last_adm1_id = 0;
    int last_adm2_id = 0;
    int min_rwanda = Integer.MAX_VALUE;
    int max_rwanda = Integer.MIN_VALUE;
    PrintWriter PW = new PrintWriter(out_path+"rwanda_units.txt");
    for (int id = 0; id < GRT.unit_names.size(); id++) {
    	String unit = GRT.unit_names.get(id);
    	String[] bits = unit.split("\t");
    	if (bits[0].equals("Rwanda")) {
    		min_rwanda = Math.min(min_rwanda,  id);
    		max_rwanda = Math.max(max_rwanda,  id);
    		if (!last_adm1.equals(bits[1])) {
    		  last_adm1 = bits[1];
    		  last_adm1_id++;
    		  last_adm2_id = 1;
    	  } else if (!last_adm2.equals(bits[2])) {
      		last_adm2 = bits[2];
    		  last_adm2_id++;
    	  }
    	  int new_id = 370000 + (last_adm1_id * 100) + (last_adm2_id);
    	  trans.put(id, new_id);
    	  PW.println(new_id + "\t" + unit);
    	}
    }
    PW.close();
    PW = new PrintWriter(out_path+"rwanda_pop.txt");
    int[][] landscan = GRT.loadIntGrid(ls_path, 43200, 21600, 43200, 21600, 0, 0);
    GRT.loadMapFile(out_path+"map.bin");
    for (int j=0; j<21600; j++) {
    	float lat = (90.f - ((j+1.0f)/120.0f));
    	for (int i=0; i<43200; i++) {
    		if ((GRT.map[j][i] >= min_rwanda) && (GRT.map[j][i] <= max_rwanda)) {
    			float lon = -180.0f + (i / 120.0f);
    			PW.println(lon+","+lat+","+landscan[j][i]+",37,"+trans.get(GRT.map[j][i]));
    		}
    	}
    }
    PW.close();
  }

  public static void main(String[] args) throws Exception {
    Work W = new Work();
    W.rwanda();

  }
}

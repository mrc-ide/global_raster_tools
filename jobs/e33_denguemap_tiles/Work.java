package jobs.e33_denguemap_tiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Work { 
  	
	void convert_file(GlobalRasterTools GRT, File fin, File fout,
			int resolution) throws Exception {
		System.out.println(fin);
	  BufferedReader br = new BufferedReader(new FileReader(fin));
	  PrintWriter PW = new PrintWriter(new FileWriter(fout));
	  String header = br.readLine();
	  PW.println(header + ",\"ID_0\",\"ID_1\",\"ID_2\"");
	  String[] bits = header.split(",");
	  if (!bits[1].equals("\"latitude\"") || !bits[2].equals("\"longitude\"")) {
	  	System.out.println("Unexpected header, "+fin.getName());
	  	System.out.println("bits[1] = "+bits[1]);
	  	System.out.println("bits[2] = "+bits[2]);
	  	System.exit(0);
	  }
	  String s = br.readLine();
	  while (s != null) {
	  	bits = s.split(",");
	  	float lon = Float.parseFloat(bits[2]);
	  	float lat = Float.parseFloat(bits[1]);
	  	int lon_ll = (int) Math.round((180.0f + lon) * 120.0f);
	  	int lat_ll = (int) Math.round((89.99167 - lat) * 120.0f);
	  	HashMap<Integer, Integer> pixels = new HashMap<Integer, Integer>();
	  	int best = -1;
	  	int best_unit = -1;
	  	for (int j = lat_ll; j > lat_ll - resolution; j--) {
	  		for (int i = lon_ll; i < lon_ll + resolution; i++) {
	  			int pix = GRT.map[j][i];
	  			if (pix >= 0) {
	  				if (!pixels.containsKey(pix)) pixels.put(pix,  0);
	  				int count = pixels.get(pix) + 1;
	  				pixels.put(pix,  count);
	  				if (count > best) {
	  					best = count;
	  				  best_unit = pix;
	  				}
	  			}
	  		}
	  	}
	  	pixels.clear();
	  	if (best_unit == -1) {
	  		best_unit = GRT.getNearest(GRT.map, lon, lat);
	  		System.out.println("No adm for "+lon+", "+lat+" - nearest = "+best_unit);
	  		
	  	}
	    String[] unit = GRT.unit_numbers.get(best_unit).split("\t");
	  	s += ","+unit[0]+","+unit[1]+","+unit[2];
	  	PW.println(s);
	  	s = br.readLine();
	  }
  
	  br.close();
	  PW.close();
  
	}
	
	
	void add_admin_units(GlobalRasterTools GRT, String tilePath, 
			                 String outPath, int resolution) throws Exception {
		
		new File(outPath).mkdirs();
		File[] files = new File(tilePath).listFiles();
		for (File f : files) {
			if ((f.getName().endsWith(".txt")) && (f.getName().startsWith("tile_"))) {
				convert_file(GRT, f, new File(outPath+f.getName()), resolution);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
    Work W = new Work();
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadMapFile("D:/data/denguemap/map.bin");
    GRT.loadUnits("D:/data/denguemap/units.txt");
    
    String tiles = "//wpia-hn/CMCCORMACK_14/DENV_Maps/output/"+
                   "env_variables/Wes/tile_set_5km_ls2015/";
    
    W.add_admin_units(GRT, tiles, tiles + "adm/", 5);

    tiles = "//wpia-hn/CMCCORMACK_14/DENV_Maps/output/"+
        "env_variables/Wes/tile_set_5km_ls2022/";
    
    W.add_admin_units(GRT, tiles, tiles + "adm/", 5);

    tiles = "//wpia-hn/CMCCORMACK_14/DENV_Maps/output/"+
        "env_variables/Wes/tile_set_20km_ls2022/";
    
    W.add_admin_units(GRT, tiles, tiles + "adm/", 20);
  }
}

package jobs.e30_denguemap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Work {
  static String outPath;
  static String mapPath;
  static String gadmPath = "D:\\data\\gadm\\";
  static String tilePath;
  static String lsPath2023;
  static String lsPath2015;
  
  public void multirow_export(String inpath, String outpath) throws Exception {
  	int resolution = 5;
 	  GlobalRasterTools GRT = new GlobalRasterTools();
    int[][] lscan2023 = null;
    lscan2023 = GRT.loadIntGrid(lsPath2023, 43200, 21600, 43200, 20880, 0, 0);
    GRT.loadMapFile("D:/data/DengueMap/map.bin");
    GRT.loadUnits("D:/data/DengueMap/units.txt");

    BufferedReader br = new BufferedReader(new FileReader(inpath));
  	PrintWriter PW = new PrintWriter(new FileWriter(outpath));
 	  String header = br.readLine();
 	  if (!header.equals("\"\",\"cell\",\"latitude\",\"longitude\",\"landscan_2023\",\"ID_0\",\"ID_1\",\"ID_2\",\"best\"")) {
 	  	System.out.println("Unexpected header");
 	  	System.exit(0);
 	  }
 	  PW.println(header);

 	  String s = br.readLine();
  	while (s != null) {
  	 	String[] bits = s.split(",");
  	  float lon = Float.parseFloat(bits[3]);
  	  float lat = Float.parseFloat(bits[2]);
 	  	int lon_ll = (int) Math.round((180.0f + lon) * 120.0f);
 	  	int lat_ll = (int) Math.round((89.99167 - lat) * 120.0f);

 	  	HashMap<Integer, Integer> pixels = new HashMap<Integer, Integer>();
 	  	int best = -1;
 	  	int best_unit = -1;
 	  	for (int j = lat_ll; j > lat_ll - resolution; j--) {
 	  		for (int i = lon_ll; i < lon_ll + resolution; i++) {
 	  			int pix = GRT.map[j][i];
 	  			int pop = lscan2023[j][i];
  	  		if (pix >= 0) {
  	  			if (!pixels.containsKey(pix)) pixels.put(pix,  0);
  	  			int count = pixels.get(pix) + pop;
  	  			pixels.put(pix,  count);
  	  		}
  	  	}
  	  }
 	  	for (Map.Entry<Integer, Integer> pix : pixels.entrySet()){
 	  		bits[4] = String.valueOf(pix.getValue());
 	  		String[] unit = GRT.unit_numbers.get(pix.getKey()).split("\t");
 	  		bits[5] = unit[0];
 	  		bits[6] = unit[1];
 	  		bits[7] = unit[2];
 	  		PW.println(String.join(",", bits));
 	  	}
 	  	pixels.clear();
 	  	s = br.readLine();
 	  }
   
 	  br.close();
 	  PW.close();
  }
  
  public void convertTilekm(GlobalRasterTools GRT, File f, int delta) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(f));
    PrintWriter PW = new PrintWriter(new File(outPath + f.getName()));
    String s = br.readLine(); // header
    PW.println(s + ",\"ID_0\",\"ID_1\",\"ID_2\"");
    s = br.readLine();
    while (s != null) {
      String[] bits = s.split(",");
      double lat = Double.parseDouble(bits[1]);
      double lon = Double.parseDouble(bits[2]);
      int px = (int) Math.round((180.0 + lon) * 120.0);
      int py = (int) Math.round((89.99167 - lat) * 120.0);
      if ((px < 0) || (py < 0)) {
        System.out.println("lon=" + lon + ", lat=" + lat);
      } else {
      	int unit = GRT.map[py][px];
      	if (unit >= 0) {
      		bits = GRT.unit_numbers.get(unit).split("\t");
      	} else {
      		bits = GRT.unit_numbers.get(GRT.getNearest(lon, lat)).split("\t");
      	}
      
        PW.println(s + "," + bits[0] + "," + bits[1] + "," + bits[2]);
      }
      s = br.readLine();
        
    }
    br.close();
    PW.close();
  }
  
  public void convertTile(GlobalRasterTools GRT, int[][] lscan2015, int[][] lscan2023, File f) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(f));
    PrintWriter PW = new PrintWriter(new File(outPath + f.getName()));
    String s = br.readLine(); // header
    PW.println(s + ",\"ID_0\",\"ID_1\",\"ID_2\",\"landscan_2015\",\"landscan_2023\"");
    s = br.readLine();
    while (s != null) {
      String[] bits = s.split(",");
      double lat = Double.parseDouble(bits[1]);
      double lon = Double.parseDouble(bits[2]);
      lon -= (1/240.0);
      lat -= (1/240.0);
      int px = (int) Math.round((180.0 + lon) * 120.0);
      int py = (int) Math.round((89.99167 - lat) * 120.0);
      if ((px < 0) || (py < 0) || (px>=43200) || (py>=21600)) {
        System.out.println(f+": "+"lon=" + lon + ", lat=" + lat);
      } else {
        if (GRT.map[py][px] >= 0) bits = GRT.unit_numbers.get(GRT.map[py][px]).split("\t");
        else bits = GRT.unit_numbers.get(GRT.getNearest(lon, lat)).split("\t");
        PW.println(s + "," + bits[0] + "," + bits[1] + "," + bits[2] + "," + lscan2015[py][px]+","+lscan2023[py][px]);
      }
      s = br.readLine();
    }
    br.close();
    PW.close();
  }

  class FileThreader extends Thread {
    int thread;
    int no_threads;
    GlobalRasterTools _grt;
    int[][] _lscan2015, _lscan2023;
    int _mode = -1;

    public FileThreader(GlobalRasterTools _g, int[][] lscan2015, int[][] lscan2023, int t, int m, int mode) {
      thread = t;
      no_threads = m;
      _grt = _g;
      _lscan2015 = lscan2015;
      _lscan2023 = lscan2023;
      _mode = mode;
    }

    public void run() {
      try {
        File[] fs = new File(tilePath).listFiles();

        for (int i = thread; i < fs.length; i+= no_threads) {
          if (fs[i].getName().startsWith("tile_")) {
             System.out.println(fs[i].getPath());
             try {
            	 if (_mode == 1) convertTile(_grt, _lscan2015, _lscan2023, fs[i]);
            	 else if (_mode == 2) convertTilekm(_grt, fs[i], 9);
            	 else if (_mode == 3) convertTilekm(_grt, fs[i], 2);
             } catch (Exception e) {
               e.printStackTrace();
             }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void convertAllTiles(GlobalRasterTools _grt, int[][] lscan2015, int[][] lscan2023, int mode) throws Exception {
    int n_threads = Runtime.getRuntime().availableProcessors();
    n_threads = 1;
    FileThreader[] ft = new FileThreader[n_threads];
    for (int i = 0; i < n_threads; i++) ft[i] = new FileThreader(_grt, lscan2015, lscan2023, i, n_threads, mode);
    for (int i = 0; i < n_threads; i++) ft[i].run();
    for (int i = 0; i < n_threads; i++) ft[i].join();
  }
  
  public void plotNAs() throws Exception {
  	BufferedImage bi = new BufferedImage(21600,10800,BufferedImage.TYPE_3BYTE_BGR);
  	int white = new Color(255, 255, 255).getRGB();
  	long total_pop = 0;
  	for (int i=0; i<=350; i++) {
  	  File f = new File("//wpia-hn/Dengue/nas3_dengue/data/processed/fullres/altitude/new/alt_global_raw_set"+i+".txt");
  	  if (!f.exists()) {
  	  	continue;
  	  }
  	  System.out.println(i);
  	  BufferedReader br = new BufferedReader(new FileReader(f));
  	  String s = br.readLine();
  	  s = br.readLine();
  	  
  	  while (s!=null) {
  	  	String[] bits = s.split("\\s+");
  	  	double lat = Double.parseDouble(bits[1]);
  	  	double lon = Double.parseDouble(bits[2]);
  	  	String alt = bits[3];
  	  	//String pop = bits[bits.length - 4];
  	  	if (alt.equals("NA")) {
  	  		int lati = (int) Math.round(60 * (89.9967 - lat));
  	  		int loni = (int) Math.round(60 * (180 + lon));
  	  		if (loni < 0) {
  	  			System.out.println("Invalid loni - "+loni);
  	  			loni = 0;
  	  		}
  	  		if (loni >= 21600) {
  	  			System.out.println("Invalid loni - "+loni);
  	  			loni = 21599;
  	  		}
  	  		if (lati < 0) {
  	  			System.out.println("Invalid lati - "+loni);
  	  			lati = 0;
  	  		}
  	  		if (lati >= 10800) {
  	  			System.out.println("Invalid lati - "+loni);
  	  			lati = 10799;
  	  		}
  	  		bi.setRGB(loni,  lati, white);
  	  		//if (!pop.equals("NA")) total_pop += Integer.parseInt(pop);
  	  	}
  	  	s = br.readLine();
  	  }
  	  br.close();
  	  System.out.println("total-pop = "+total_pop);
  	}
  	ImageIO.write(bi,  "PNG",  new File("D:/alt_na_new.png"));
  	System.out.println("Total pop = "+total_pop);
  }
  
  public void plot_alt() throws Exception {
  	BufferedImage bi = new BufferedImage(4320, 1800, BufferedImage.TYPE_3BYTE_BGR);
  	int red = new Color(255,0,0).getRGB();
  	DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(
  			"W:/nas3_dengue/Data/WorldClim-Altitude/alt.bil")));
  	short maxd = 0;
  	short mind = 0;
  	short[][] alt = new short[18000][43200];
  	for (int j=0; j<18000; j++) {
  		if (j % 1000 == 0) System.out.println(j);
  		for (int i=0; i<43200; i++) {
  			short d = Short.reverseBytes(dis.readShort());
  			alt[j][i] = d;
  			if (d == -9999) {
  				bi.setRGB(i/10,  j/10,  red);
  			} else {
  				mind = (short) Math.min(d,  mind);
  			  maxd = (short) Math.max(d,  maxd);
  			}
  		}
  	}
  	dis.close();
  	System.out.println("Min= "+mind+",max = "+maxd);
  	ImageIO.write(bi, "PNG", new File("D:/alt_na_orig.png"));
  }

  
  public void run(String[] args, int mode) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    int[][] lscan2023 = null;
    int[][] lscan2015 = null;    
    if (mode == 1) {
    	System.out.println("Loading landscan 2023");
      lscan2023 = GRT.loadIntGrid(lsPath2023, 43200, 21600, 43200, 20880, 0, 0);
      System.out.println("Loading landscan 2015");
      lscan2015 = GRT.loadIntGrid(lsPath2015, 43200, 21600, 43200, 20880, 0, 0);
    } 
        
    if (!new File(gadmPath+"gadm41_ZWE_2.shp").exists()) GRT.downloadShapeFiles(gadmPath, "4.1");
    
    if (!new File(mapPath+"map.bin").exists()) {
      GRT.loadPolygonFolder(gadmPath, 2, "4.1");
      GRT.makeMap();
      GRT.saveMapFile(mapPath+"map.bin");
      GRT.saveUnits(mapPath+"units.txt");
    } else {
    	System.out.println("Loading map");
      GRT.loadUnits(mapPath+"units.txt");
      GRT.loadMapFile(mapPath+"map.bin");
    }
    System.out.println("Converting");
    convertAllTiles(GRT, lscan2015, lscan2023, mode);

   
  }
  
  public static void main(String[] args) throws Exception {
    Work lt = new Work();
    // Nov 2024 - append admin units 0,1,2 and LS_2023 onto 1km tiles.

    
    /*
    outPath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_1km_adm_ls2015_2023/";
    mapPath = "D:/data/DengueMap/";
    tilePath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_1km/";
    
    lsPath2015 = "Y:/landscan/2015/landscan-global-2015.bil";
    lt.run(args, 1);
    System.exit(0);
    */
    lsPath2023 = "Y:/landscan/2023/landscan-global-2023.bil";
    // And append adm units 0,1,2 onto 20km tiles
    // tile_set_20km_landscan_2015_adm
    // tile_set_20km_landscan_2023_adm
    // tile_set_5km_landscan_2015_adm
    // tile_set_5km_landscan_2023_adm
    
    //outPath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_20km_landscan_2015_adm/";
    //mapPath = "D:/data/DengueMap/";
    //tilePath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_20km_landscan_2015/";
    //lt.run(args,  2);
    
    //outPath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_20km_landscan_2023_adm/";
    //mapPath = "D:/data/DengueMap/";
    //tilePath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_20km_landscan_2023/";
    //lt.run(args,  2);
    
    //outPath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_5km_landscan_2015_adm/";
    //mapPath = "D:/data/DengueMap/";
    //tilePath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_5km_landscan_2015/";
    //lt.run(args,  3);
    
    //outPath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_5km_landscan_2023_adm/";
    //mapPath = "D:/data/DengueMap/";
    //tilePath = "X:/DENV_Maps/output/env_variables/Wes/tile_set_5km_landscan_2023/";
    //lt.run(args,  3);
    
    // And this one is for Neil - we take a 5km file including  
    //   X cell latitude longitude landscan_2023 ID_0    ID_1      ID_2 best
    // long and lat are lower-left-corners of a 1/24 grid.
    
    outPath = "X:/Denv_Maps/output/env_variables/Wes/Pixel_Estimates/Pixel_Estimates_5km_MultiUnit.csv";
    tilePath = "X:/Denv_Maps/output/env_variables/Wes/Pixel_Estimates/Pixel_Estimates_5km.csv";
    lt.multirow_export(tilePath, outPath);
  }
}

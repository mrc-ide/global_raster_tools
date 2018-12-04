package jobs.e1_LorenzoTiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class LorenzoTiles {
  //static String outPath = "\\\\fi--didenas5\\dengue\\dengue_risk_mapping\\output\\env_variables\\tile_set_2_20km\\gadm\\";
  static String outPath = "E:\\Jobs\\Lorenzo\\";
  static String gadmPath = "E:\\Data\\Boundaries\\GADM3_6\\";
  //static String tilePath = "\\\\fi--didenas5\\dengue\\dengue_risk_mapping\\output\\env_variables\\tile_set_2\\";
  static String tilePath = "\\\\fi--didenas5\\dengue\\dengue_risk_mapping\\output\\env_variables\\tile_set_2_20km\\";
  
  public void convertTile(GlobalRasterTools GRT, File f) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(f));
    PrintWriter PW = new PrintWriter(new File(outPath + f.getName()));
    String s = br.readLine(); // header
    PW.println(s + ",\"ID_0\",\"ID_1\",\"ID_2\"");
    s = br.readLine();
    while (s != null) {
      String[] bits = s.split(",");
      double lat = Double.parseDouble(bits[1]);
      double lon = Double.parseDouble(bits[2]);
      int px = (int) ((180.0 + lon) * GlobalRasterTools.RESOLUTION);
      int py = (int) ((90.0 - lat) * GlobalRasterTools.RESOLUTION);
      if ((px < 0) || (py < 0)) {
        System.out.println("lon=" + lon + ", lat=" + lat);
      }
      if (GRT.map[py][px] >= 0) bits = GRT.unit_numbers.get(GRT.map[py][px]).split("\t");
      else bits = GRT.unit_numbers.get(GRT.getNearest(lon, lat)).split("\t");

      // Work out adm0,adm1,adm2
      PW.println(s + "," + bits[0] + "," + bits[1] + "," + bits[2]);
      s = br.readLine();
    }
    br.close();
    PW.close();
  }

  class FileThreader extends Thread {
    int thread;
    int no_threads;
    GlobalRasterTools _grt;

    public FileThreader(GlobalRasterTools _g, int t, int m) {
      thread = t;
      no_threads = m;
      _grt = _g;
    }

    public void run() {
      try {
        int x = thread;
        File[] fs = new File(tilePath).listFiles();

        for (int i = 0; i < fs.length; i++) {
          if (fs[i].getName().startsWith("tile_")) {
            if (x == 0) {
              System.out.println(fs[i].getPath());
              try {
                convertTile(_grt,fs[i]);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
            x = (x + 1) % no_threads;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void convertAllTiles(GlobalRasterTools _grt) throws Exception {
    int n_threads = Runtime.getRuntime().availableProcessors();
    FileThreader[] ft = new FileThreader[n_threads];
    for (int i = 0; i < n_threads; i++) ft[i] = new FileThreader(_grt, i, n_threads);
    for (int i = 0; i < n_threads; i++) ft[i].run();
    for (int i = 0; i < n_threads; i++) ft[i].join();
  }

  
  public void run(String[] args) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    
    if (!new File(gadmPath+"ZWE_adm2.shp").exists()) GRT.downloadShapeFiles(gadmPath, "2.8");
    GRT.loadPolygonFolder(gadmPath, 2, "2.8");
    
    if (!new File(outPath+"map.bin").exists()) {
      GRT.makeMap();
      GRT.saveMapFile(outPath+"map.bin");
      GRT.saveUnits(outPath+"units.txt");
    } else {
      GRT.loadUnits(outPath+"units.txt");
      GRT.loadMapFile(outPath+"map.bin");
    }
    
    if (args.length == 0) {
      convertAllTiles(GRT);

    } else {
      convertTile(GRT, new File(tilePath + "tile_" + Integer.parseInt(args[0]) + ".txt"));
    }

  }
  
  public static void main(String[] args) throws Exception {
    LorenzoTiles lt = new LorenzoTiles();
    lt.run(args);
    
  }
}

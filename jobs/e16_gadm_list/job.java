package jobs.e16_gadm_list;

import java.io.File;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class job {
  static String gadmPath = "E:\\Data\\Boundaries\\GADM3_6\\";
  static String outPath = ".";
  public static void main(String[] args) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    
    if (!new File(gadmPath+"gadm36_ZWE_2.shp").exists()) GRT.downloadShapeFiles(gadmPath, "3.6");
    GRT.loadPolygonFolder(gadmPath, 5, "3.6");
    GRT.saveUnits(outPath+"units.txt");
    
  }
}

package jobs.e14_kelly_colombia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Work {
  static String workDir = "E:/Jobs/Kelly_Colombia/";
  static String shapePath = "E:/Data/Boundaries/Kelly_Colombia/g2015_2014_2/";
  static String shapeFile = "g2015_2014_2.shp";
  static String dbfFile = "g2015_2014_2.dbf";
  static String landscan = "E:/Data/Census/Landscan2016/lspop.flt";

    
   
  
  /// THE WORK ///
  
  public void run(String[] args) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygons(2, shapePath+shapeFile, shapePath+dbfFile, new ArrayList<String>(Arrays.asList(new String[] {"Colombia"})), "Kelly-Colombia"); 
        
    if (!new File(workDir+"map.bin").exists()) {
      GRT.makeMap();
      GRT.saveMapFile(workDir+"map.bin");
      GRT.saveUnits(workDir+"units.txt");
    } else {
      GRT.loadUnits(workDir+"units.txt");
      GRT.loadMapFile(workDir+"map.bin");
    }
    
    BufferedReader br = new BufferedReader(new FileReader(workDir+"units.txt"));
    String s = br.readLine();
    PrintWriter PW = new PrintWriter(new File(workDir+"centroids.txt"));
    PW.println(s+"\tCentroid_lon\tCentroid_lat\tls2016_pop");
    
    int[][] ls2016 = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
    System.out.print("Getting extents...");
    int[] extents = GRT.getGlobalExtent(GRT.map,  0,  GRT.unit_names.size());
    System.out.println(extents[0]+","+extents[1]+","+extents[2]+","+extents[3]);
    for (int i=0; i<GRT.unit_names.size(); i++) {
      double[] centroid = GRT.getCentroid(i,true,ls2016,extents);
      s = br.readLine();
      PW.println(s+"\t"+centroid[0]+"\t"+centroid[1]+"\t"+centroid[2]);
      System.out.println(s+"\t"+centroid[0]+"\t"+centroid[1]+"\t"+centroid[2]);
      
    }
    br.close();
    PW.close();
  }

  public static void main(String[] args) throws Exception {
    Work w = new Work();
    w.run(args);
  }
}

package jobs.e23_adm1_centroids;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class work {
  private final static String landscanPath = "E:/Data/Census/Landscan2019/";
  private final static String landscan = landscanPath+"lspop2019.bil";

  private final static String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  private final static String outPath = "E:/jobs/adm1_centroids/";
  
  public void run() throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(gadmPath, 1, "3.6");
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
    
    BufferedReader br = new BufferedReader(new FileReader(outPath+"units.txt"));
    String s = br.readLine();
    PrintWriter PW = new PrintWriter(new File(outPath+"centroids.txt"));
    PW.println(s+"\tpop_centroid_lon\tpop_centroid_lat\tspace_centroid_lon\tspace_centroid_lat\tls2019_pop");
    String country = "NULL";
    int[][] ls2019 = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
    int[] country_extents = null;
    for (int i=0; i<GRT.unit_names.size(); i++) {
      s = br.readLine();
      String[] ubits = GRT.unit_names.get(i).split("\t");
      if (!country.equals(ubits[0])) {
        int j = i;
        while ((j < GRT.unit_names.size()-1) && (GRT.unit_names.get(j+1).split("\t")[0].equals(ubits[0]))) j++;
        System.out.print("E ");
        country_extents = GRT.getGlobalExtent(GRT.map, i, j);
        country = ubits[0];
      } else System.out.print("  ");
      System.out.print(" e ");
      int[] extents = GRT.getGlobalExtent(GRT.map, i, i, country_extents);
      System.out.print(" P ");
      double[] pcentroid = GRT.getCentroid(i, true,ls2019, extents);
      System.out.print(" L ");
      double[] lcentroid = GRT.getCentroid(i, false, ls2019, extents);
      
      if (pcentroid == null) {
        System.out.println("PCENTROID = NULL");
      }
      if (lcentroid == null) {
        System.out.println("LCENTROID = NULL");
      }
      
      PW.println(s+"\t"+pcentroid[0]+"\t"+pcentroid[1]+"\t"+lcentroid[0]+"\t"+lcentroid[1]+"\t"+pcentroid[2]);
      System.out.println(s+"\t"+pcentroid[0]+"\t"+pcentroid[1]+"\t"+lcentroid[0]+"\t"+lcentroid[1]+"\t"+pcentroid[2]);
    }
    br.close();
    PW.close();
  }
  
  public static void main(String[] args) throws Exception {
    new work().run();
  }
}

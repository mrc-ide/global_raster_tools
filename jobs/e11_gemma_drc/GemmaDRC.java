package jobs.e11_gemma_drc;

import java.io.File;
import java.io.PrintWriter;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class GemmaDRC {
  final static String outPath = "E:/jobs/Gemma-20190709/out/";
  final static String shapePath = "E:/jobs/Gemma-20190709/Shapefiles/";  
  final static String landscan = "E:/Data/Census/Landscan2017/lspop.flt";
  final static String shapename = "ZS_merged_MSF_WHO";
  
  public void run(String[] args) throws Exception {
   
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadPolygons(1, shapePath+shapename+".shp", shapePath+shapename+".dbf", null, "Gemma-DRC");
        
    if (!new File(outPath+"map.bin").exists()) {
      GRT.makeMap();
      GRT.saveMapFile(outPath+"map.bin");
      GRT.saveUnits(outPath+"units.txt");
    } else {
      GRT.loadUnits(outPath+"units.txt");
      GRT.loadMapFile(outPath+"map.bin");
    }
    
    /* Make some hideous shape images */
    System.out.println("Making hideous images");
    int[] extents = GRT.getGlobalExtent(GRT.map, 0, Integer.MAX_VALUE);
    for (int i=0; i<GRT.unit_names.size(); i++) {
      GRT.hideousShapePNG(GRT.map, outPath+"shapes_"+i+".png", extents, i, i+": "+GRT.unit_names.get(i));
    }
    
    System.out.println("Loading landscan");
    int[][] ls2016 = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
    
    System.out.println("Making a picture of Landscan data");
    GRT.spatialMap(GRT.map, ls2016, outPath+"ls.png", extents, -9999, true, false,null,null,null);
    
    PrintWriter PW = new PrintWriter(outPath+"spatial_data.csv");
    PW.println("long,lat,pop,country_code,admin_code");
    for (int j=extents[2]; j<=extents[3]; j++) {
      for (int i=extents[0]; i<=extents[1]; i++) {
        int code = GRT.map[j][i];
        if (code!=-1) {
          String s = "";
          if (code<100) s+="0";
          if (code<10) s+="0";
          s+=String.valueOf(code);
          if (ls2016[j][i]>=0) {
            PW.println((float) (-180.0+(i/120.0))+","+(float) ((10799.0/120.0) - (j/120.0))+","+ls2016[j][i]+",50,50"+s);
          }
        }
      }
    }
    PW.close();
    
    PW = new PrintWriter(outPath+"centroids.csv");
    PW.println("Unit\tN0\tPop\tPop_Cent_Lon\tPop_Cent_Lat\tArea_Cent_Lon\tArea_Cent_Lat");
    for (int i=0; i<GRT.unit_names.size(); i++) {
      System.out.println("Centroid "+i);
      double[] centr_pop = GRT.getCentroid(i, true, ls2016, extents);
      double[] centr_area = GRT.getCentroid(i,  false, ls2016, extents);
      String[] names = GRT.unit_names.get(i).split("\t");
      if ((centr_pop==null) || (centr_area==null)) {
        PW.println(i+"\t"+names[0]+"0\tNA\tNA\tNA\tNA");
  
      } else {
      PW.println(i+"\t"+names[0]+"\t"+centr_pop[2]+"\t"
                      +centr_pop[0]+"\t"+centr_pop[1]+"\t"+centr_area[0]+"\t"+centr_area[1]);
      }
      PW.flush();
    }
    PW.close();
    ls2016 = null;
    System.gc();
  }
 
 public static void main(String[] args) throws Exception {
    GemmaDRC gdrc = new GemmaDRC();
    gdrc.run(args);
    
  }
}
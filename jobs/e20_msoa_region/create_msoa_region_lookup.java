package jobs.e20_msoa_region;

import java.io.PrintWriter;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class create_msoa_region_lookup {
  private final static String MSOAPath = "E:/Data/Boundaries/MSOA/";
  private final static String MSOAFile = "Middle_Layer_Super_Output_Areas__December_2011__Boundaries_EW_BFC";
  private final static String regionPath = "E:/Data/Boundaries/DH_Region/";
  private final static String regionFile = " NHSEngRegionsandDevolved";
  private final static String outPath = "E:/Jobs/MSOA/";
  
  public void run() throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    //GRT.loadPolygonFolder(regionPath, 0, "DH_REGION");
    //GRT.makeMap();
    //GRT.saveMapFile(outPath+"region_map.bin");
    //GRT.saveUnits(outPath+"region_units.txt");
    //int[] extents = GRT.getGlobalExtent(GRT.map, 0, 9);
    //GRT.hideousShapePNG(GRT.map, outPath+"regions.png", extents, 0,"");
    //System.exit(0);
    
    GlobalRasterTools GRT2 = new GlobalRasterTools();
    //GRT2.loadPolygonFolder(MSOAPath,  0,  "MSOA");
    //GRT2.makeMap();
    //GRT2.saveMapFile(outPath+"msoa_map.bin");
    //GRT2.saveUnits(outPath+"msoa_units.txt");
    
    //GRT2.hideousShapePNG(GRT2.map, outPath+"msoa.png", extents_msoa, 0,"");
    
    GRT2.loadMapFile(outPath+"msoa_map.bin");
    GRT2.loadUnits(outPath+"msoa_units.txt");
    GRT.loadMapFile(outPath+"region_map.bin");
    GRT.loadUnits(outPath+"region_units.txt");
    int[] extents_msoa = GRT2.getGlobalExtent(GRT2.map, 0, GRT2.unit_names.size());
    int[][] msoa_regions = new int[GRT2.unit_names.size()][GRT.unit_names.size()];    
    
    
    for (int j=extents_msoa[2]; j<=extents_msoa[3]; j++) {
      System.out.println(j);
      for (int i=extents_msoa[0]; i<=extents_msoa[1]; i++) {
        int msoa_id = GRT2.map[j][i];
        int region_id = GRT.map[j][i];
        if ((region_id>=0) && (msoa_id>=0)) {
          msoa_regions[msoa_id][region_id]++;
        }
      }
    }
    PrintWriter PW = new PrintWriter(outPath+"lookup.tsv");
    PW.println("msoa_num\tmsoa_name\tregion_num\tregion_name\tNotes");
    for (int i=0; i<GRT2.unit_names.size(); i++) {
      PW.print(GRT2.unit_numbers.get(i)+"\t"+GRT2.unit_names.get(i)+"\t");
      int count = 0;
      int best = 0;
      for (int j=0; j<GRT.unit_names.size(); j++) {
        if (msoa_regions[i][j]>0) count++;
        if (msoa_regions[i][j]>msoa_regions[i][best]) best=j;
      }
      if (count == 0) {
        PW.print("NULL\t");
      } else if (count ==1) {
        PW.print(GRT.unit_numbers.get(best)+"\t"+GRT.unit_names.get(best)+"\t");
      } else {
        PW.print(GRT.unit_numbers.get(best)+"\t"+GRT.unit_names.get(best)+"\t");
        for (int j=0; j<GRT.unit_names.size(); j++) {
          if (msoa_regions[i][j]>0) PW.print(GRT.unit_numbers.get(j)+"\t"+GRT.unit_names.get(j)+"\t"+msoa_regions[i][j]+"\t");
        }
      }
      PW.println();
    }
    PW.close();
  }
  
  public static void main(String[] args) throws Exception {
    create_msoa_region_lookup CMRL = new create_msoa_region_lookup();
    CMRL.run();
  }
}

package jobs.e12_neil_compare_drc_landscan;

import java.awt.Color;
import java.util.ArrayList;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Work {
  final static String[] paths = new String[] {"E:/Data/Census/Landscan2014",
                                              "E:/Data/Census/Landscan2015",
                                              "E:/Data/Census/Landscan2016",
                                              "E:/Data/Census/Landscan2017"};
  
  final static String[] flt_files = new String[] {"lspop2014.flt", "lspop2015.flt", "lspop.flt", "lspop.flt"};
  final static String[] hdr_files = new String[] {"lspop2014.hdr", "lspop2015.hdr", "lspop.hdr", "lspop.hdr"};
  final static String[] out_files = new String[] {"drc_ls2014.png", "drc_ls2015.png", "drc_ls2016.png", "drc_ls2017.png"};
  
  String outputPath = "E:/Jobs/Neil_Landscan_Comp";
  
  
  
  public void run(String[] args) throws Exception {
   
    // Hack to save time - I already have a DRC mask
    GlobalRasterTools GRT = new GlobalRasterTools();
    System.out.println("Loading map mask");
    GRT.loadMapFile(outputPath+"/DRC.bin");
    int[] extents = GRT.getGlobalExtent(GRT.map,  0, Integer.MAX_VALUE);
    
    int xrange = 1+(extents[1]-extents[0]);
    int yrange = 1+(extents[3]-extents[2]);
    int[][] cropped_map = new int[yrange][xrange];
    for (int i=extents[0]; i<=extents[1]; i++) {
      for (int j=extents[2]; j<=extents[3]; j++) {
        cropped_map[j-extents[2]][i-extents[0]] = GRT.map[j][i];
      }
    }
    GRT.map = null;

    int d_max=0;
    ArrayList<int[][]> frames = new ArrayList<int[][]>();
    for (int f=0; f<=3; f++) {
      int[][] buffer = new int[yrange][xrange];
      System.out.println("Loading "+flt_files[f]);
      int[][] ls = GRT.loadIntGrid(paths[f]+"/"+flt_files[f],43200,21600,43200,21600,0,0);
      System.out.println("ls dim: "+ls.length+","+ls[0].length);
      for (int i=extents[0]; i<=extents[1]; i++) {
        for (int j=extents[2]; j<=extents[3]; j++) {
          buffer[j-extents[2]][i-extents[0]] = ls[j][i];
          if (ls[j][i]>d_max) d_max=ls[j][i];
        }
      }
      ls=null;
      System.gc();
      System.out.println("d_max = "+d_max);
      frames.add(buffer);
      buffer = null;
    }
    extents[1] -= extents[0];
    extents[0] = 0;
    extents[3] -= extents[2];
    extents[2] = 0;
    
    Color[] col_scale = new Color[256];
    for (int i=0; i<256; i++) {
      col_scale[i] = new Color(i,0,255-i);
    }
    
    for (int f=0; f<=3; f++) {
      GRT.spatialMap(cropped_map, frames.get(f), outputPath+"/"+out_files[f], extents, -9999, true, false, Color.WHITE, col_scale, (float)d_max);
    }
  }
 
 public static void main(String[] args) throws Exception {
    Work W = new Work();
    W.run(args);
    
  }
}
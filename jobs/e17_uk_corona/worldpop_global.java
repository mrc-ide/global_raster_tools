package jobs.e17_uk_corona;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class worldpop_global {
  public static void main(String[] args) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    float[][] wp = GRT.loadFloatGrid("E:/Data/Census/WorldPop/global/worldpop2020global.bil", 43200, 21600, 43200, 18720, 0, 2879);
    
  }
}

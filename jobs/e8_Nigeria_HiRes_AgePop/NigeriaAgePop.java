package jobs.e8_Nigeria_HiRes_AgePop;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class NigeriaAgePop {
  
  /* List of GADM strings to identify the countries we're interested in */
  static final String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  static final String outPath = "E:/Jobs/NigeriaPop/";
  static final String dataPath = "E:/Data/Census/WorldPop/AgeStrat/Nigeria/";
  static final List<String> countries = Arrays.asList(new String[] {"Nigeria"});
  static final List<String> codes = Arrays.asList(new String[] {"NGA"});
  static final List<String> admin1 = Arrays.asList(new String[] {"Abia", "Adamawa", "Akwa", "Anambra", "Bauchi", "Bayelsa",
      "Benue","Borno","Cross River", "Deltea", "Ebonyi", "Edo", "Ekiti", "Enugu", "Federal Capital Territory",
      "Gombe", "Imo", "Jigawa", "Kaduna", "Kano", "Katsina", "Kebbi", "Kogi", "Kwara", "Lagos", "Nassarawa", "Niger",
      "Ogun", "Ondo", "Osun", "Oyo", "Plateau", "Rivers", "Sokoto", "Taraba", "Yobe", "Zamfara"});
  
  public NigeriaAgePop() {}
  
  public static void main(String[] args) throws Exception {
    //if (args.length<1) {
//      System.out.println("Usage: java -Xmx8g jobs.e8_Nigeria_HiRes_AgePop.NigeriaAgePop Path-to-Bil-Files");
      //System.exit(0);
    //}
    
    NigeriaAgePop NAP = new NigeriaAgePop();
    NAP.run();
    System.exit(0);
  }
  
  
  public void run() throws Exception {
    int WID = 14414;
    int HEI = 11543;
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.setResolution(1200);
    GRT.setHeight(HEI);
    GRT.setWidth(WID);
    GRT.setLonWL(2.6684975073115);
    GRT.setLatNT(13.8945639611761);

    if (!new File(gadmPath+"gadm36_NGA_1.shp").exists()) GRT.downloadShapeFiles(gadmPath, "3.6");
    GRT.loadPolygonFolder(gadmPath, 1, countries, "3.6");
    
    if (!new File(outPath+"map.bin").exists()) {
      GRT.makeMap();
      GRT.saveMapFile(outPath+"map.bin");
      GRT.saveUnits(outPath+"units.txt");
    } else {
      GRT.loadUnits(outPath+"units.txt");
      GRT.loadMapFile(outPath+"map.bin");
    }
    
    String[] ages_from = new String[] {"00","05","10","15","20","25","30","35","40","45","50","55","60","65"};
    String[] ages_to = new String[] {"05","10","15","20","25","30","35","40","45","50","55","60","65","99"};
    int RED = Color.RED.getRGB();
    float[][] pop = new float[GRT.unit_numbers.size()][ages_from.length];
    BufferedImage bi = new BufferedImage(WID, HEI, BufferedImage.TYPE_3BYTE_BGR);
    for (int i=0; i<ages_from.length; i++) {
      String file = dataPath+"NGA_"+ages_from[i]+"_"+ages_to[i]+".bil";
      System.out.println("Reading "+file);
      FileChannel fc = FileChannel.open(Paths.get(file));
      for (int j = 0; j < HEI; j++) {
        MappedByteBuffer mbb = fc.map(MapMode.READ_ONLY, (long) (4L*j*WID), WID*4L);
        for (int x = 0; x < WID; x++) {
          float f = Float.intBitsToFloat(Integer.reverseBytes(Float.floatToIntBits(mbb.getFloat())));
          if (f>=0.01) bi.setRGB(x, j, RED);
          if (f>=0.01) {
            
            int unit = GRT.map[j][x];//GRT.getNearest_ints(GRT.map,  x,  j,  -1);
            if (unit == -1) {
              unit = GRT.getNearest_ints(GRT.map, x, j, -1);
              GRT.map[j][x]=unit;
            }
            pop[unit][i] += f;
          }
        }
        mbb.clear();
      }
      fc.close();
    }
    ImageIO.write(bi, "PNG",  new File(outPath+"bil.png"));
    PrintWriter PW = new PrintWriter(outPath+"output.txt");
    PW.println("Country\tUnit\tPop0_5\tPop5_10\tPop10_15\tPop15_20\tPop20_25\tPop25_30\tPop30_35\tPop35_40\tPop40_45\tPop45_50\tPop50_55\tPop55_60\tPop60_65\tPop65_99");
    for (int i=0; i<GRT.unit_names.size(); i++) {
      PW.print("Nigeria\t"+GRT.unit_names.get(i));
      for (int j=0; j<ages_from.length; j++) PW.print("\t"+Math.round(pop[i][j]));
      PW.println("");
    }
    PW.close();

    
    /*
    System.out.println("Making hideous images");
    int[] extents = GRT.getGlobalExtent(GRT.map, -1);
    for (int i=0; i<GRT.unit_names.size(); i++) {
      GRT.hideousShapePNG(GRT.map, outPath+"shapes_"+i+".png", extents, i, i+": "+GRT.unit_names.get(i));
    }
    */
    
    
   
    /*
    GRT.loadUnits(unitfile);
    GRT_to_me = new int[GRT.unit_names.size()];
    for (int i=0; i<GRT.unit_names.size(); i++) GRT_to_me[i]=-1;
    
    for (int i=0; i<me_to_GRT.length; i++) {
      int index=-1;
      for (int j=0; j<GRT.unit_names.size(); j++) {
        if (GRT.unit_names.get(j).startsWith(countries.get(i))) {
          index = j;
          break;
        }
      }
      if (index==-1) {
        System.out.println("Can't find "+countries.get(i));
      }
      me_to_GRT[i] = GRT.unit_names.indexOf(countries.get(i));
      if (index==-1) System.out.println("Can't find "+countries.get(i));
      GRT_to_me[index]=i;
    }
    
    
    System.out.println("Reading map");
    GRT.loadMapFile(mapfile);
    System.out.println("Creating back layer");
    
    int white = Color.white.getRGB();
    int grey = new Color(160,160,160).getRGB();
    int black = new Color(0,0,0).getRGB();
    
    System.out.println("Prepare");
    int scale_down = 20;
    
    int h = GRT.map.length / scale_down;
    int w = GRT.map[0].length / scale_down;
    BufferedImage png = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    int[][] pixmap = new int[w][h];
    for (int j=0; j<h; j++) for (int i=0; i<w; i++) {
      png.setRGB(i, j, white);
      pixmap[i][j]=-1;
    }
    
*/
  }
}

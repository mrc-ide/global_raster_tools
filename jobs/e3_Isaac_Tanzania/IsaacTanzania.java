package jobs.e3_Isaac_Tanzania;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class IsaacTanzania {
  final static String outPath = "e:\\Isaac\\Output\\";
  final static String gadmPath = "E:\\Isaac\\Shapes\\";
  final static String dataPath = "E:\\Isaac\\Data\\";
  final static String landscan = "E:\\Data\\Census\\Landscan2016\\lspop.flt";
  final static String modis_landcover = "\\\\fi--didenas3\\Dengue\\Data\\MODIS\\MCD12Q1.006\\2016.01.01\\";
  
  public void append_data(String file, String field, float[][] more_data, int[] extents) throws Exception {
    PrintWriter PW = new PrintWriter(file+"2");
    BufferedReader br = new BufferedReader(new FileReader(file));
    String s = br.readLine();
    PW.println(s+","+field);
    for (int j=extents[2]; j<=extents[3]; j++) {
      for (int i=extents[0]; i<=extents[1]; i++) {
        s = br.readLine();
        PW.println(s+","+more_data[j][i]);
      }
    }
    PW.close();
    br.close();
    new File(file).delete();
    new File(file+"2").renameTo(new File(file));
  } 

  public void run(String[] args) throws Exception {
   
    GlobalRasterTools GRT = new GlobalRasterTools();
    
    GRT.loadPolygonFolder(gadmPath, 1, "2.8");
    
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
    int[] extents = GRT.getGlobalExtent(GRT.map, -1);
    for (int i=0; i<GRT.unit_names.size(); i++) {
      GRT.hideousShapePNG(GRT.map, outPath+"shapes_"+i+".png", extents, i, i+": "+GRT.unit_names.get(i));
    }
    System.out.println("Loading landscan");
    int[][] ls2016 = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
    System.out.println("Making a picture of Landscan data");
    GRT.spatialMap(GRT.map, ls2016, outPath+"ls.png", extents, -9999, true, false,null,null,null);
    PrintWriter PW = new PrintWriter(outPath+"spatial_data.csv");
    PW.println("Lot,Lat,id,pop");
    for (int j=extents[2]; j<=extents[3]; j++) {
      for (int i=extents[0]; i<=extents[1]; i++) {
        PW.println((float) (-180.0+(i/120.0))+","+(float) (89.991666666667 - (j/120.0))+","+GRT.map[j][i]+","+ls2016[j][i]);
      }
    }
    PW.close();
    ls2016 = null;
    System.gc();
    
    // Add poverty data
    
    float[][] more_data = GRT.loadFloatGrid(dataPath+"tza_pov_125.bil",  43200, 21600, 2401, 3001, 24600, 9600);
    GRT.spatialMap(GRT.map, more_data, outPath+"pov_125.png", extents, -3.4e38f, false, true, null, null, null);
    append_data(outPath+"spatial_data.csv","pov_125",more_data, extents);
    more_data=null;
    System.gc();
    more_data = GRT.loadFloatGrid(dataPath+"tza_pov_200.bil",  43200, 21600, 2401, 3001, 24600, 9600);
    GRT.spatialMap(GRT.map, more_data, outPath+"pov_200.png", extents, -3.4e38f, false, true, null, null, null);
    append_data(outPath+"spatial_data.csv","pov_200",more_data, extents);
    more_data=null;
    System.gc();
    
    // Add Modis landcover
    
    byte[][] modis = new byte[43200][86400];
    double vd,hd,lat,lon;
    byte c;
    for (int h=0; h<36; h++) {
      String hh = ((h<10)?"0":"")+h;
      for (int v=0; v<18; v++) {
        String vv = ((v<10)?"0":"")+v;
        File f = new File(modis_landcover+"MCD12Q1.A2016001.h"+hh+"v"+vv+".006_lct1.bin");
        if (f.exists()) {
          System.out.println("Loading tile "+f);
          DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
          for (int j=0; j<2400; j++) {
            for (int i=0; i<2400; i++) {
              c = dis.readByte();
              if (c!=-1) {
                vd = v + (j+0.5)/2400;
                hd = h + (i+0.5)/2400;
                lat = 90.0 - 10.0*vd;
                lon = (10.0*hd-180.0)/Math.sin(10.0*vd*Math.PI/180.0);
                if ((lat>=-90) && (lat<90) && (lon>=-180) && (lon<180))
                  modis[(int)(240*(90-lat))][(int)(240*(180+lon))] = c;
              }
            }
          }
          dis.close();
        }
      }
    }
    
    int[] extents2 = new int[4];
    for (int i=0; i<4; i++) extents2[i]=extents[i]*2;
    GRT.modisMap(GRT.map, modis, outPath+"landcover.png",extents2);
    PW = new PrintWriter(outPath+"spatial_data.csv2");
    BufferedReader br = new BufferedReader(new FileReader(outPath+"spatial_data.csv"));
    String s = br.readLine();
    PW.println(s+","+"land_cover");
    for (int j=extents[2]; j<=extents[3]; j++) {
      for (int i=extents[0]; i<=extents[1]; i++) {
        s = br.readLine();
        if (modis[j*2][i*2]!=255) PW.println(s+","+modis[j*2][i*2]);
        else {
          double[] opts = new double[18];
          opts[modis[(j*2)-1][i*2]]+=1;
          opts[modis[(j*2)+1][i*2]]+=1;
          opts[modis[(j*2)][(i*2)+1]]+=1;
          opts[modis[(j*2)][(i*2)-1]]+=1;
          opts[modis[(j*2)-1][(i*2)-1]]+=0.707;
          opts[modis[(j*2)-1][(i*2)+1]]+=0.707;
          opts[modis[(j*2)+1][(i*2)-1]]+=0.707;
          opts[modis[(j*2)+1][(i*2)+1]]+=0.707;
          byte best=0;
          for (byte m=1; m<18; m++) if (opts[m]>opts[best]) best=m;
          PW.println(s+","+best);
          
        }
      }
    }
    PW.close();
    br.close();
    new File(outPath+"spatial_data.csv").delete();
    new File(outPath+"spatial_data.csv2").renameTo(new File(outPath+"spatial_data.csv"));
 }
 
 public static void main(String[] args) throws Exception {
    IsaacTanzania it = new IsaacTanzania();
    it.run(args);
    
  }
}

package jobs.e4_Sarah_Tanzania;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

import ucar.ma2.ArrayByte;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class SarahTanzania {
  final static String outPath = "e:\\Jobs\\SarahTanzania\\output\\";
  // Download Tanzania level 2 into gadmPath.
  final static String gadmPath = "E:\\Jobs\\SarahTanzania\\shapes\\";
  //final static String landscan = "\\\\fi--didef3\\Census\\Landscan2016\\lspop.flt";
  //final static String landscan = "\\\\fi--didef3\\Census\\Landscan2015\\lspop2015.flt";
  //final static String landscan = "\\\\fi--didef3\\Census\\Landscan2014\\Population\\lspop2014.flt";
  final static String landscan = "E:/Data/Census/Landscan2017/lspop.flt";
  final static String year = "2018";
  final static String modis_landcover = "\\\\fi--didenas3\\Dengue\\Data\\MODIS\\MCD12Q1.006\\"+year+".01.01\\";
  
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
    
    GRT.loadPolygonFolder(gadmPath, 2, "3.6");
    
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
    //int[][] lscan = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
    // For 2011 or earlier
    int[][] lscan = GRT.loadIntGrid(landscan, 43200, 21600, 43200, 20880, 0, 720);
    System.out.println("Making a picture of Landscan data");
    GRT.spatialMap(GRT.map, lscan, outPath+"ls.png", extents, -9999, true, false, null, null, null);
    PrintWriter PW = new PrintWriter(outPath+"spatial_data.csv");
    PW.println("Lon,Lat,id,pop");
    for (int j=extents[2]; j<=extents[3]; j++) {
      for (int i=extents[0]; i<=extents[1]; i++) {
        PW.println((float) (-180.0+(i/120.0))+","+(float) (89.991666666667 - (j/120.0))+","+GRT.map[j][i]+","+Math.max(0, lscan[j][i]));
      }
    }
    PW.close();
    lscan = null;
    System.gc();
    
    // Add Modis landcover
    
    byte[][] modis = new byte[43200][86400];
    double vd,hd,lat,lon;
    byte c;
    for (int h=0; h<36; h++) {
      String hh = ((h<10)?"0":"")+h;
      for (int v=0; v<18; v++) {
        String vv = ((v<10)?"0":"")+v;
        String f = modis_landcover+"MCD12Q1.A"+year+"001.h"+hh+"v"+vv+".006.hdf";
        
        if (new File(f).exists()) {
          System.out.println("Loading tile "+f);
          NetcdfFile ncfile = NetcdfDataset.openFile(f, null);
          Variable LCT1 = ncfile.findVariable("MCD12Q1/Data_Fields/LC_Type1");
          ArrayByte.D2 data = (ArrayByte.D2) LCT1.read();
          
          for (int j=0; j<2400; j++) {
            for (int i=0; i<2400; i++) {
              c = data.get(j,i);
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
          ncfile.close();
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
    String[] bits;
    for (int j=extents[2]; j<=extents[3]; j++) {
      for (int i=extents[0]; i<=extents[1]; i++) {
        s = br.readLine();
        bits = s.split(",");
        int unit = Integer.parseInt(bits[2]);
        if (((unit>=59) && (unit<=64)) || ((unit>=98) && (unit<=104))) {
          if (modis[j*2][i*2]!=0) PW.println(s+","+modis[j*2][i*2]);
          else { 
            int nearest = GRT.getNearest_ints(modis,i,j,0);
            PW.println(s+","+nearest);
          }
        }
      }
    }
    PW.close();
    br.close();
    new File(outPath+"spatial_data.csv").delete();
    new File(outPath+"spatial_data.csv2").renameTo(new File(outPath+"spatial_data.csv"));
 }
 
 public static void main(String[] args) throws Exception {
    SarahTanzania it = new SarahTanzania();
    it.run(args);
    
  }
}

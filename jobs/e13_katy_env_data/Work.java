package jobs.e13_katy_env_data;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.mrc.GlobalRasterTools.CSVFile;
import com.mrc.GlobalRasterTools.GlobalRasterTools;

import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayShort;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class Work {
  static String workDir = "E:/Jobs/katy_env/";
  static String input = workDir + "dat_5.csv";
  static String gadmPath = "E:\\Data\\Boundaries\\GADM3_6\\";

    
   
  public int[][] landcover_stats(int year, int[][] land, GlobalRasterTools GRT) throws Exception {
    
    byte[][] landcover = new byte[43200][86400];
    double vd,hd,lat,lon;
    for (int h=0; h<=35; h++) {
      for (int v=0; v<=17; v++) {
        String file = "W:/Data/MODIS/MCD12Q1.006/"+year+".01.01/MCD12Q1.A"+year+"001.h"+((h<10)?0:"")+h+"v"+((v<10)?"0":"")+v+".006.hdf";
        if (new File(file).exists()) {
          System.out.println(file);
          NetcdfFile ncfile = NetcdfDataset.openFile(file, null);
          Variable LCT1 = ncfile.findVariable("MCD12Q1/Data_Fields/LC_Type1");
          ArrayByte.D2 data = (ArrayByte.D2) LCT1.read();
          for (int i=0; i<2400; i++) {
            for (int j=0; j<2400; j++) {
              int d = data.get(j,i);
              if (d>0) {
                vd = v + (j+0.5)/2400;
                hd = h + (i+0.5)/2400;
                lat = 90.0 - 10.0*vd;
                lon = (10.0*hd-180.0)/Math.sin(10.0*vd*Math.PI/180.0);
                if ((lat>=-90) && (lat<90) && (lon>=-180) && (lon<180))
                  landcover[(int)(240*(90-lat))][(int)(240*(180+lon))] = (byte) d;
              }
            }
            ncfile.close();
          }
        }
      }
    }
    System.out.println("Scanning...");
    int[] correction = new int[18];
    for (int j=0; j<21600; j++) {
      for (int i=0; i<43200; i++) {
        int id = GRT.map[j][i];
        if (id>=0) {
          byte data = landcover[j*2][i*2];
          if ((data>=1) && (data<=17)) {
            land[data][id]++;
            
          } else {
            Arrays.fill(correction,  0);
            byte d2 = landcover[(j*2)+1][i*2]; if ((d2>=1) && (d2<=17)) correction[d2]+=2;
            d2 = landcover[(j*2)][(i*2)+1];    if ((d2>=1) && (d2<=17)) correction[d2]+=2;
            d2 = landcover[(j*2)-1][i*2];      if ((d2>=1) && (d2<=17)) correction[d2]+=2;
            d2 = landcover[(j*2)][(i*2)-1];    if ((d2>=1) && (d2<=17)) correction[d2]+=2;
            d2 = landcover[(j*2)+1][(i*2)+1];  if ((d2>=1) && (d2<=17)) correction[d2]+=1;
            d2 = landcover[(j*2)+1][(i*2)-1];  if ((d2>=1) && (d2<=17)) correction[d2]+=1;
            d2 = landcover[(j*2)-1][(i*2)-1];  if ((d2>=1) && (d2<=17)) correction[d2]+=1;
            d2 = landcover[(j*2)-1][(i*2)+1];  if ((d2>=1) && (d2<=17)) correction[d2]+=1;
            int best_val=-1;
            int best_lc=-1;
            for (d2=1; d2<=17; d2++) {
              if (correction[d2]>best_val) {
                best_val=correction[d2];
                best_lc=d2;
              }
            }
            if (best_lc!=-1) land[best_lc][id]++;
          }
        }
      }
    }
    return land;
  }
  
  public double[][] evi_mir_oneday(String f, ArrayList<ArrayList<Integer>> unit_pixels, double[][] results) throws Exception {
    short[][] evi = new short[21600][43200];
    short[][] mir = new short[21600][43200];
    for (int j=0; j<21600; j++) {
      Arrays.fill(evi[j], Short.MIN_VALUE);
      Arrays.fill(mir[j], Short.MIN_VALUE);
    }
    
    int[] cols = new int[256];
    for (int i=0; i<=255; i++) cols[i] = (new Color(i,i,i)).getRGB();
    for (int h=0; h<=35; h++) {
      String hs = ((h<10)?"0":"")+h;
      for (int v=0; v<=17; v++) {
        String vs = ((v<10)?"0":"")+v;
        String f1 = f+"h"+hs+"v"+vs+".006.hdf";
        if (new File(f1).exists()) {
          System.out.println(f1);
          NetcdfFile ncfile = NetcdfDataset.openFile(f1, null);
          Variable v_evi = ncfile.findVariable("MODIS_Grid_16DAY_1km_VI/Data_Fields/1_km_16_days_EVI");
          ArrayShort.D2 d_evi = (ArrayShort.D2) v_evi.read();
          Variable v_mir = ncfile.findVariable("MODIS_Grid_16DAY_1km_VI/Data_Fields/1_km_16_days_MIR_reflectance");
          ArrayShort.D2 d_mir = (ArrayShort.D2) v_mir.read(); 
          double vd, hd, lat, lon;
          
          for (int i=0; i<1200; i++) {
            for (int j=0; j<1200; j++) {
              vd = v + (j+0.5)/1200;
              hd = h + (i+0.5)/1200;
              lat = 90.0 - 10.0*vd;
              lon = (10.0*hd-180.0)/Math.sin(10.0*vd*Math.PI/180.0);
              if ((lat>=-90) && (lat<90) && (lon>=-180) && (lon<180)) {
                evi[(int)(120*(90-lat))][(int)(120*(180+lon))] = d_evi.get(j,i);
                mir[(int)(120*(90-lat))][(int)(120*(180+lon))] = d_mir.get(j,i);
              }
            }
            ncfile.close();
          }
        }
      }
    }
    
    System.out.println("Updating unit stats");
    
    for (int i=0; i<unit_pixels.size(); i++) {
      ArrayList<Integer> pixels = unit_pixels.get(i);
      for (int p=0; p<pixels.size(); p++) {
        int px = pixels.get(p);
        int py = px / 43200;
        px = px % 43200;
        double one_evi = evi[py][px];
        if (one_evi>=-2000) {
          one_evi = one_evi * 0.0001;
          if (one_evi<results[i][0]) results[i][0]=one_evi;
          if (one_evi>results[i][1]) results[i][1]=one_evi;
          results[i][2]+=one_evi;
          results[i][3]++;
        }
        double one_mir = mir[py][px];
        if (one_mir>=0) {
          one_mir = one_mir * 0.0001;
          if (one_mir<results[i][4]) results[i][4]=one_mir;
          if (one_mir>results[i][5]) results[i][5]=one_mir;
          results[i][6]+=one_mir;
          results[i][7]++;
        }
      }
    }
    return results;
  }
  
  public double[][] evi_mir_stats(int year, ArrayList<ArrayList<Integer>> unit_pixels, double[][] collected_data) throws Exception {
    GregorianCalendar gc = new GregorianCalendar();
    gc.setTimeZone(TimeZone.getTimeZone("GMT"));
    gc.set(GregorianCalendar.HOUR,  0);
   
    
    gc.set(GregorianCalendar.YEAR,  year);
    for (int month=1; month<=12; month++) {
      gc.set(GregorianCalendar.MONTH, month-1);
      String sm = ((month<10)?"0":"")+month;
      for (int day=1; day<=31; day++) {
        gc.set(GregorianCalendar.DATE, day);
        String sd = ((day<10)?"0":"")+day;
        int day_year = gc.get(GregorianCalendar.DAY_OF_YEAR);
        String sday_year = (day_year<10?"0":"")+(day_year<100?"0":"")+day_year;
        String f1 = "W:/Data/MODIS/MOD13A2.006/"+year+"."+sm+"."+sd;
        if (new File(f1).exists()) {
          f1 = f1 + "/MOD13A2.A"+year+sday_year+".";
          collected_data = evi_mir_oneday(f1, unit_pixels, collected_data);
        }
        String f2 = "W:/Data/MODIS/MYD13A2.006/"+year+"."+sm+"."+sd;
        if (new File(f2).exists()) {
          f2 = f2 + "/MYD13A2.A"+year+sday_year+".";
          collected_data = evi_mir_oneday(f2, unit_pixels, collected_data);
        }
      }
    }
    return collected_data;
  }
  
  /// THE WORK ///
  
  public void run(String[] args) throws Exception {
    
    CSVFile csv = CSVFile.read(input, true);
    GlobalRasterTools GRT = new GlobalRasterTools();
    if (!new File(gadmPath+"gadm36_ZWE_1.shp").exists()) GRT.downloadShapeFiles(gadmPath, "3.6");
    GRT.loadPolygonFolder(gadmPath, 1, CSVFile.unique(csv.getCol("country")), "3.6");
        
    if (!new File(workDir+"map.bin").exists()) {
      GRT.makeMap();
      GRT.saveMapFile(workDir+"map.bin");
      GRT.saveUnits(workDir+"units.txt");
    } else {
      GRT.loadUnits(workDir+"units.txt");
      GRT.loadMapFile(workDir+"map.bin");
    }
    
    // For each unit we're interested in, get a list of pixels that are in that unit.
    
    System.out.print("Calculating pixels for each region ...");
    ArrayList<ArrayList<Integer>> unit_pixels = new ArrayList<ArrayList<Integer>>();
    for (int n=0; n<GRT.unit_names.size(); n++) unit_pixels.add(new ArrayList<Integer>());
    for (int j=0; j<21600; j++) {
      for (int i=0; i<43200; i++) {
        if (GRT.map[j][i]>=0) unit_pixels.get(GRT.map[j][i]).add((43200*j)+i);
      }
    }
    System.out.println("done");
    
    int[][] lc = new int[18][GRT.unit_numbers.size()];
   
    double[][] evi_mir_data = new double[unit_pixels.size()][8];
    for (int i=0; i<unit_pixels.size(); i++) {
      evi_mir_data[i][0]=Double.MAX_VALUE;
      evi_mir_data[i][1]=Double.NEGATIVE_INFINITY;
      evi_mir_data[i][4]=Double.MAX_VALUE;
      evi_mir_data[i][5]=Double.NEGATIVE_INFINITY;
    }
    for (int year=2003; year<=2006; year++) {
      System.out.println("Year "+year);
      //evi_mir_data = evi_mir_stats(year,  unit_pixels, evi_mir_data);
      lc = landcover_stats(year, lc, GRT);
    }
    BufferedReader br = new BufferedReader(new FileReader(input));
    PrintWriter PW = new PrintWriter(new File(workDir+"output.csv"));
    String header = br.readLine();
    header = header+",EVI.mean,EVI.min,EVI.max,MIR.mean,MIR.min,MIR.max,LC1,LC2,LC3,LC4,LC5,LC6,LC7,LC8,LC9,LC10,LC11,LC12,LC13,LC14,LC15,LC16,LC17";
    PW.println(header); // Col headers
    String s = br.readLine();
    int r=0;
    while (s!=null) {
      PW.print(s+",");
      if ((csv.get("country", r)+"\t"+csv.get("Province", r)).equals(GRT.unit_names.get(r))) {
        evi_mir_data[r][2] /= (double) evi_mir_data[r][3];
        evi_mir_data[r][6] /= (double) evi_mir_data[r][7];
        PW.print(evi_mir_data[r][2]+","+evi_mir_data[r][0]+","+evi_mir_data[r][1]+",");
        PW.print(evi_mir_data[r][6]+","+evi_mir_data[r][4]+","+evi_mir_data[r][5]+",");
        int total=lc[1][r];
        for (int j=2; j<18; j++) {
          total+=lc[j][r];
        }
        PW.print(lc[1][r]/(double)total);
        for (int j=2; j<18; j++) {
          PW.print(","+lc[j][r]/(double)total);
        }
        PW.print("\n");
        s = br.readLine();
        r++;
      } else {
        System.out.println("Unit mismatch - Expected "+csv.get("country", r)+"\t"+csv.get("Provice", r)+", Shapefile says "+GRT.unit_names.get(r));
        s = br.readLine();
        r++;
      }
    }
    PW.close();
    br.close();
  }

  public static void main(String[] args) throws Exception {
    Work w = new Work();
    w.run(args);
  }
}

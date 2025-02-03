package jobs.e24_era5_tori;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

import net.sourceforge.jgrib.GribFile;
import net.sourceforge.jgrib.GribRecord;

public class ERA5 {
  static final String workingDir = "D:/era5-2324/";

  ArrayList<Float> lon = new ArrayList<Float>();
  ArrayList<Float> lat = new ArrayList<Float>();
  ArrayList<Integer> from_year = new ArrayList<Integer>();
  ArrayList<Integer> to_year = new ArrayList<Integer>();
  
  Process[] processes = new Process[3];
  
  void parse_input(String f) throws Exception {
    int LON = 3;
    int LAT = 4;
    int YEARS = 9;
    BufferedReader br = new BufferedReader(new FileReader(f));
    String[] headers = br.readLine().split(",");
    if ((!headers[LON].equals("longitude")) || (!headers[LAT].equals("latitude")) || (!headers[YEARS].equals("Years_climate"))) {
      System.out.println("Problems in csv format");
      System.exit(1);
    }
    String s = br.readLine();
    while (s!=null) {
      String[] bits = s.split(",");
      if (bits.length != headers.length) {
        System.out.println("Error parsing "+s);
        System.exit(1);
      }
      lon.add(Math.round(10.0*Float.parseFloat(bits[LON])) / 10.0f); 
      lat.add(Math.round(10.0*Float.parseFloat(bits[LAT])) / 10.0f);
      String[] yrs = bits[YEARS].split("_");
      from_year.add(Math.max(1950, Integer.parseInt(yrs[0])));
      to_year.add(Math.min(2020, Integer.parseInt(yrs[1])));
      s = br.readLine();
    }
    br.close();
  }
     
  void writeDownload(String variable, String year, String month, String day, String time, int proc, String file) throws Exception {
    PrintWriter PW = new PrintWriter(new File(workingDir + "fetch"+proc+".py"));
    PW.println("import cdsapi");
    PW.println("c = cdsapi.Client()");
    PW.println("c.retrieve('reanalysis-era5-land', {");
    PW.println("             'variable': "+variable+",");
    PW.println("             'year' : '"+year+"',");
    PW.println("             'month' : '"+month+"',");
    PW.println("             'day' : '"+day+"',");
    if (time!=null) PW.println("             'time' : "+time+",");
    PW.println("             'format': 'grib'");
    PW.println("            }, '"+workingDir+file+"')");
    PW.close();
  }
 
  String leadingZero(int m) {
    return ((m<10)?"0":"")+String.valueOf(m);
  }

  private void pull_data(int min_year, int max_year) throws Exception {
    GregorianCalendar gc = new GregorianCalendar();
    gc.setTimeInMillis(System.currentTimeMillis());
    gc.set(GregorianCalendar.DAY_OF_MONTH,  15);

    
    for (int year = min_year; year <= max_year; year++) {
      gc.set(GregorianCalendar.YEAR, year);
      for (int month = 1; month <= 12; month++) {
        gc.set(GregorianCalendar.DAY_OF_MONTH,  1);
        gc.set(GregorianCalendar.MONTH, month - 1);
        int no_days = gc.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
        for (int day = 1; day <= no_days; day++) {
          gc.set(GregorianCalendar.DAY_OF_MONTH, day);
          for (int type=0; type<=1; type++) {
            int proc = -1;
            while (proc == -1) {
              for (int p=0; p<processes.length; p++) {
                if ((processes[p] == null) || (!processes[p].isAlive())) {
                  proc = p;
                  break;
                }
              }
              if (proc == -1) Thread.sleep(10000);
            }
          
            if (type == 0) {
              writeDownload("'total_precipitation'", String.valueOf(year), leadingZero(month), leadingZero(day), "'00:00'",
                  proc, "precip_"+year+leadingZero(month)+leadingZero(day)+".grib"); 
            } else {
              writeDownload("['2m_temperature', '2m_dewpoint_temperature']", String.valueOf(year), leadingZero(month), leadingZero(day), 
                  "['00:00', '01:00', '02:00', '03:00', '04:00', '05:00', '06:00', '07:00', '08:00', '09:00', '10:00', '11:00', "+
                  "'12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00', '23:00']", proc, 
                  "temp_"+year+leadingZero(month)+leadingZero(day)+".grib");
            }
            PrintWriter PW = new PrintWriter(new FileWriter(workingDir+"run"+proc+".bat"));
            PW.println("python -u " + workingDir + "fetch"+proc+".py > " + workingDir+"out"+proc+".txt 2>&1");
            PW.close();
            processes[proc] = Runtime.getRuntime().exec(workingDir+"run"+proc+".bat");

          }
        }
      }
    }
  }
  
  class OneDay {
    float[][][] tm = new float[24][3600][1801];
    float[][] tm_mean = new float[3600][1801];
    float[][] tm_max = new float[3600][1801];
    float[][] tm_min = new float[3600][1801];
    float[][][] dp = new float[24][3600][1801];
    float[][][] rh = new float[24][3600][1801];
    float[][] rh_max = new float[3600][1801];
    float[][] rh_min = new float[3600][1801];
    float[][] rh_mean = new float[3600][1801];
    float[][] precip = new float[3600][1801];
    
    void parse_record(int hour, GribRecord g) throws Exception {
      if (g.getDescription().equals("2 metre temperature")) {
        for (int x=0; x<3600; x++) {
          for (int y=0; y<1801; y++) {
            tm[hour][x][y] = g.getValue(x,  y) - 273.16f;
          }
        }
        System.out.print("+");
      } else if (g.getDescription().equals("2 metre dewpoint temperature")) {
        for (int x=0; x<3600; x++) {
          for (int y=0; y<1801; y++) {
            dp[hour][x][y] = g.getValue(x,  y) - 273.16f;
          }
        }
      } else if (g.getDescription().equals("Total precipitation")) {
        for (int i=0; i<3600; i++) {
          for (int j=0; j<1801; j++) {
            precip[i][j] = g.getValue(i, j);
            if (precip[i][j] > 1E20) {
              precip[i][j] = Float.NaN;
            }
          }
        }
      } else {
        System.out.println("ERROR - variable not recognised - "+g.getDescription());
      } 
    }
    
    void calc_rh() {
      for (int h=0; h<24; h++) {
        for (int x=0; x<3600; x++) {
          for (int y=0; y<1801; y++) {
            if ((tm[h][x][y] > 1E18) || (dp[h][x][y] > 1E18)) {
              rh[h][x][y] = Float.POSITIVE_INFINITY;
            } else {
              rh[h][x][y] = (float) (100.0*(Math.exp((17.625 * dp[h][x][y]) / (243.04 + dp[h][x][y])) / 
                                          Math.exp((17.625 * tm[h][x][y]) / (243.04 + tm[h][x][y]))));
            }
          }
        }
      }
    }
    
    void calc_min_max_mean() {
      for (int i=0; i<3600; i++) {
        for (int j=0; j<1801; j++) {
          tm_min[i][j] = Float.MAX_VALUE;
          tm_max[i][j] = Float.NEGATIVE_INFINITY;
          int tm_counter=0;
          tm_mean[i][j] = 0;
          rh_min[i][j] = Float.MAX_VALUE;
          rh_max[i][j] = Float.NEGATIVE_INFINITY;
          int rh_counter = 0;
          rh_max[i][j] = 0;
          for (int h=0; h<24; h++) {
            if (tm[h][i][j] < 1E20) {
              tm_counter = tm_counter + 1;
              tm_mean[i][j] = tm_mean[i][j] + tm[h][i][j];
              tm_max[i][j] = Math.max(tm_max[i][j], tm[h][i][j]);
              tm_min[i][j] = Math.min(tm_min[i][j], tm[h][i][j]);
            }
            if ((rh[h][i][j] >= 0) && (rh[h][i][j] <= 100)) {
              if (rh[h][i][j] < 1E20) {
                rh_counter = rh_counter + 1;
                rh_mean[i][j] = rh_mean[i][j] + rh[h][i][j];
                rh_max[i][j] = Math.max(rh_max[i][j], rh[h][i][j]);
                rh_min[i][j] = Math.min(rh_min[i][j], rh[h][i][j]);
              }
            }
          }
          tm_mean[i][j] = tm_mean[i][j] / (float) tm_counter;
          rh_mean[i][j] = rh_mean[i][j] / (float) rh_counter;
          if (tm_counter == 0) {
            tm_mean[i][j] = Float.NaN;
            tm_max[i][j] = Float.NaN;
            tm_min[i][j] = Float.NaN;
          }
          if (rh_counter == 0) {
            rh_mean[i][j] = Float.NaN;
            rh_max[i][j] = Float.NaN;
            rh_min[i][j] = Float.NaN;
          }
        }
      }
    }
    
    void find_nearest(int xp, int yp) {
      int range = 0;
      int count = 0;
      float maxt = 0, mint = 0, meant = 0, maxrh = 0, minrh = 0, meanrh = 0, prec = 0;
      while (count == 0) {
        range++;
        for (int i2 = xp - range; i2 <= xp + range; i2++) {
          for (int j2 = yp - range; j2 <= yp + range; j2++) {
            if (!Float.isNaN(tm_mean[i2][j2])) {
              count++;
              maxt += tm_max[i2][j2];
              mint += tm_min[i2][j2];
              meant += tm_mean[i2][j2];
              maxrh += rh_max[i2][j2];
              minrh += rh_min[i2][j2];
              meanrh += rh_mean[i2][j2];
              prec += precip[i2][j2];
            }
          }
        }
      }
      tm_mean[xp][yp] = meant / count;
      tm_max[xp][yp] = maxt / count;
      tm_min[xp][yp] = mint / count;
      rh_mean[xp][yp] = meanrh / count;
      rh_max[xp][yp] = maxrh / count;
      rh_min[xp][yp] = minrh / count;
      precip[xp][yp] = prec / count;
    }
  }
  
  OneDay get_day_data(String path, int year, int month, int day) throws Exception {
    OneDay D = new OneDay();
    
    String grib_file = path + File.separator + "temp_"+year+((month < 10)?"0":"") + month + ((day < 10) ? "0":"") + day + ".grib";
    System.out.print(year+"-"+((month < 10)?"0":"") + month + "-" + ((day < 10) ? "0":"") + day + " : ");
    if (!new File(grib_file).exists()) {
      return null;
    }
    GribFile grb = new GribFile(grib_file);
    int no_recs = grb.getRecordCount();
    for (int i=1; i<= no_recs; i++) {
      GribRecord grecord = grb.getRecord(i);
      Calendar cal = grecord.getTime();
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      D.parse_record(hour, grecord);
    }
    
    D.calc_rh();
    D.calc_min_max_mean();    
    
    // Min, max, mean
        
    // Total precipitation at 00:00 is the total precipitation in the previous 24 hours.
    // The counter is then reset, and at 01:00 the numbers will be zero and rising again.
    // Note that months start at zero in GregorianCalendar.
    
    GregorianCalendar gc = new GregorianCalendar();
    gc.setTimeInMillis(System.currentTimeMillis());
    gc.set(GregorianCalendar.DATE, 5);
    gc.set(GregorianCalendar.MONTH, 5);
    gc.set(GregorianCalendar.YEAR, year);
    gc.set(GregorianCalendar.MONTH, month - 1);
    gc.set(GregorianCalendar.DATE, day);
    gc.add(GregorianCalendar.DATE,  1);
    year = gc.get(GregorianCalendar.YEAR);
    month = gc.get(GregorianCalendar.MONTH) + 1;
    day = gc.get(GregorianCalendar.DATE);
    
    grib_file = path + File.separator + "precip_"+year+((month < 10)?"0":"") + month + ((day < 10) ? "0":"") + day + ".grib";
    grb = new GribFile(grib_file);
    no_recs = grb.getRecordCount();
    if (no_recs != 1) {
      System.out.println("Preciperror");
    }
    D.parse_record(0, grb.getRecord(1));
    System.out.print("p");
    return D;

  }
  
  void summarise_days(String path, int year, int month, int day) throws Exception {
    PrintWriter PW = new PrintWriter(new File(path + File.separator +  "out_"+year+((month < 10)?"0":"") + month + ((day < 10) ? "0":"") + day + ".txt"));
    OneDay D = get_day_data(path, year, month, day);  
   
    if (D == null) {
      return;
    }
    
    PW.println("data_id,lon,lat,temp_mean,temp_min,temp_max,rh_mean,rh_min,rh_max,precip");
    
    // co-ords in grid: x pixels 0..3599: 1800 is for -180 .. > -179.9   1801 is <= -179.9 > 179.8
    // x: pixel 0 = >=0, <0.1  1799 
    // x: pixel 1799 = >=179.9
    
    // y: pixel 0 = +89.9-90 
    //    pixel 1800 = -90 .. -89.9
    //    pixel 900 = 0.. 
    
    for (int i=0; i<lon.size(); i++) {
      float x = lon.get(i);
      float y = lat.get(i);
      int xp,yp;
      if (x >=0) xp = Math.round(x * 10);
      else xp = Math.round(10 * (180 + x));
      yp = Math.round(10* (90 - y));
      
      // ERA 5 does data over land. It's possible some of these points are slightly in the sea, so loop around to find them...
      // I don't think we'll ever do this on -180...
      
      if (Float.isNaN(D.tm_mean[xp][yp])) {
        D.find_nearest(xp, yp);
      }
      
      PW.println(String.valueOf(i+1)+","+lon.get(i)+","+lat.get(i)+"," + D.tm_mean[xp][yp] +"," +D.tm_min[xp][yp] + "," + D.tm_max[xp][yp] + ","+
                 D.rh_mean[xp][yp]+","+D.rh_min[xp][yp]+","+D.rh_max[xp][yp] + ", " + D.precip[xp][yp]);
      
    }
    PW.close();
  }
  
  void save_picture(float[][] data, String file) throws Exception {
    BufferedImage bi = new BufferedImage(3600, 1801, BufferedImage.TYPE_3BYTE_BGR);
    float maxx = Float.NEGATIVE_INFINITY;
    float minx = Float.MAX_VALUE;
    for (int j=0; j<1801; j++) {
      for (int i=0; i<3600; i++) {
        if (data[i][j] < 1E24) {
          maxx = Math.max(maxx, data[i][j]);
          minx = Math.min(minx, data[i][j]);
        }
      }
    }
    System.out.println("values: "+minx+" ... "+maxx);
    int[] cols = new int[255];
    for (int i=0; i<255; i++) cols[i] = new Color(i, i, i).getRGB();
    for (int j=0; j<1801; j++) {
      for (int i=0; i<3600; i++) {
        if (data[i][j] < 1E24) {
          bi.setRGB(i, j, cols[(int)(Math.floor(254*(data[i][j]-minx)/(maxx-minx)))]);
        }
      }
    }
    ImageIO.write(bi,  "png",  new File(file));
  }
  
  public void do_loc_data(String path, int year1, int year2) throws Exception {
    for (int i=0; i<lon.size(); i++) {
      int id = i + 1;
      PrintWriter PW = new PrintWriter(new File(path+File.separator+"loc_"+id+".csv"));
      PW.println("date,temp_mean,temp_min,temp_max,rh_mean,rh_min,rh_max,precip");
      PW.close();
    }
    for (int y=year1; y<=year2; y++) {
      for (int m=1; m<=12; m++) {
        String ms = ((m<10) ? "0":"")+m;
        for (int d=1; d<=31; d++) {
          String ds = ((d<10) ? "0":"")+d;
          File f = new File(path+File.separator+"out_"+y+ms+ds+".txt");
          if (f.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(f));
            br.readLine();
            for (int i=0; i<lon.size(); i++) {
              String s = br.readLine();
              String[] bits = s.split(",");
              int id = Integer.parseInt(bits[0]);
              //data_id,lon,lat,temp_mean,temp_min,temp_max,rh_mean,rh_min,rh_max,precip
              PrintWriter PW = new PrintWriter(new FileOutputStream(path+File.separator+"loc_"+id+".csv", true));
              PW.println(y+"-"+ms+"-"+ds+","+bits[3]+","+bits[4]+","+bits[5]+","+bits[6]+","+bits[7]+","+bits[8]+","+bits[9]);
              PW.close();
            }
            br.close();
          }
        }
      }
    }
  }
  
  class AdStat {
    double mint_lo, mint_avg, mint_hi, meant_lo, meant_avg, meant_hi, maxt_lo, maxt_avg, maxt_hi;
    double prec_lo, prec_avg, prec_hi;
    double minr_lo, minr_avg, minr_hi, meanr_lo, meanr_avg, meanr_hi, maxr_lo, maxr_avg, maxr_hi;
    
    int count;
    void init() {
      count = 0;
      mint_lo = Double.POSITIVE_INFINITY;
      mint_avg = 0;
      mint_hi = Double.NEGATIVE_INFINITY;
      meant_lo = Double.POSITIVE_INFINITY;
      meant_avg = 0;
      meant_hi = Double.NEGATIVE_INFINITY;
      maxt_lo = Double.POSITIVE_INFINITY;
      maxt_avg = 0;
      maxt_hi = Double.NEGATIVE_INFINITY;
      minr_lo = Double.POSITIVE_INFINITY;
      minr_avg = 0;
      minr_hi = Double.NEGATIVE_INFINITY;
      meanr_lo = Double.POSITIVE_INFINITY;
      meanr_avg = 0;
      meanr_hi = Double.NEGATIVE_INFINITY;
      maxr_lo = Double.POSITIVE_INFINITY;
      maxr_avg = 0;
      maxr_hi = Double.NEGATIVE_INFINITY;
      prec_lo = Double.POSITIVE_INFINITY;
      prec_avg = 0;
      prec_hi = Double.NEGATIVE_INFINITY;
          
    }
    
    AdStat() {
      init();
    }
    
    void update(OneDay D, int x, int y) {
      count++;
      mint_lo = Math.min(mint_lo, D.tm_min[x][y]);
      mint_avg += D.tm_min[x][y];
      mint_hi = Math.max(mint_hi, D.tm_min[x][y]);
      meant_lo = Math.min(meant_lo, D.tm_mean[x][y]);
      meant_avg += D.tm_mean[x][y];
      meant_hi = Math.max(meant_hi, D.tm_mean[x][y]);
      maxt_lo = Math.min(maxt_lo, D.tm_max[x][y]);
      maxt_avg += D.tm_max[x][y];
      maxt_hi = Math.max(maxt_hi, D.tm_max[x][y]);
      
      minr_lo = Math.min(minr_lo, D.rh_min[x][y]);
      minr_avg += D.rh_min[x][y];
      minr_hi = Math.max(minr_hi, D.rh_min[x][y]);
      meanr_lo = Math.min(meanr_lo, D.rh_mean[x][y]);
      meanr_avg += D.rh_mean[x][y];
      meanr_hi = Math.max(meanr_hi, D.rh_mean[x][y]);
      maxr_lo = Math.min(maxr_lo, D.rh_max[x][y]);
      maxr_avg += D.rh_max[x][y];
      maxr_hi = Math.max(maxr_hi, D.rh_max[x][y]);
      
      prec_lo = Math.min(prec_lo, D.precip[x][y]);
      prec_avg += D.precip[x][y];
      prec_hi = Math.max(prec_hi, D.precip[x][y]);
            
    }
    
    void finalise() {
      mint_avg /= (double) count;
      meant_avg /= (double) count;
      maxt_avg /= (double) count;
      prec_avg /= (double) count;
      minr_avg /= (double) count;
      meanr_avg /= (double) count;
      maxr_avg /= (double) count;
    }
  }
  
  void do_admins(GlobalRasterTools GRT, String path, int from_year, int to_year, int from_id, int to_id) throws Exception {

    // ERA5 is 3600 x 1801
    
    // x=0    => lon = 0
    // x=1799 => lon = 179.9
    // x=1800 => lon = -180.0
    // x=3599 => lon = -0.1
    
    // y=0     => lat = 90 
    // y = 900 => lat = 0 
    // y = 1800 => lat = -90
    
    ArrayList<ArrayList<Point>> pixels = new ArrayList<ArrayList<Point>>();
    PrintWriter[] PW = new PrintWriter[GRT.unit_names.size()];
    for (int u=0; u < GRT.unit_names.size(); u++) {
      if ((u < from_id) || (u > to_id)) {
        pixels.add(new ArrayList<Point>());
        continue;
      }
      PW[u] = new PrintWriter(new File(path + File.separator +  "out_unit_"+u+".txt"));
      PW[u].println("year,month,day,"+
                    "min_tmp_lo,min_tmp_avg,min_tmp_hi,"+
                    "mean_tmp_lo, mean_tmp_avg,mean_tmp_hi,"+
                    "max_tmp_lo,max_tmp_avg,max_tmp_hi,"+
                    "min_rh_lo,min_rh_avg,min_rh_hi,"+
                    "mean_rh_lo,mean_rh_avg,mean_rh_hi,"+
                    "max_rh_lo,max_rh_avg,max_rh_hi,"+
                    "prec_lo,prec_avg,prec_hi");
      
      ArrayList<Point> unit_pixels = new ArrayList<Point>();
      System.out.println(GRT.unit_names.get(u));
      int t=0;
      int era5_lon = 0;
      int era5_lat = 0;

      for (int era5_i=0; era5_i<3600; era5_i++) {
        if (era5_i >= 1800) {
          era5_lon = (int) (12 * (era5_i - 1800));
        } else {
          era5_lon = 21600 + (era5_i * 12);
        }
        for (int era5_j=1; era5_j<1799; era5_j++) {
          era5_lat = era5_j * 12;
          int map_ad = GRT.map[era5_lat][era5_lon];
          if (map_ad == u) {
            t++;
            unit_pixels.add(new Point(era5_i, era5_j));
          }
        }
      }
      pixels.add(unit_pixels);
      System.out.println(t+" pixels");
    }
    
    
    AdStat stat = new AdStat();
    
    for (int year = from_year; year <= to_year; year++) {
      for (int month=1; month<=12; month++) {
        String ms = ((month<10) ? "0":"") + month;
        for (int day=1; day<=31; day++) {
          String ds = ((day<10) ? "0":"") + day;
          System.out.println(year+"-"+ms+"-"+ds);
          OneDay D = get_day_data(path, year, month, day);
          if (D == null) continue;
          for (int u=0; u<GRT.unit_names.size(); u++) {
            if ((u < from_id) || (u > to_id)) continue;
            ArrayList<Point> unit_pixels = pixels.get(u);
            stat.init();
            for (int p=0; p<unit_pixels.size(); p++) {
              Point pix = unit_pixels.get(p);
              if (Float.isNaN(D.tm_mean[pix.x][pix.y])) {
                D.find_nearest(pix.x, pix.y);
              }
              stat.update(D, pix.x, pix.y);
            }
            stat.finalise();
            PW[u].println(year+","+month+","+day+","+
                          stat.mint_lo+","+stat.mint_avg+","+stat.mint_hi+","+
                          stat.meant_lo+","+stat.meant_avg+","+stat.meant_hi+","+
                          stat.maxt_lo+","+stat.maxt_avg+","+stat.maxt_hi+","+
                          stat.minr_lo+","+stat.minr_avg+","+stat.minr_hi+","+
                          stat.meanr_lo+","+stat.meanr_avg+","+stat.meanr_hi+","+
                          stat.maxr_lo+","+stat.maxr_avg+","+stat.maxr_hi+","+
                          stat.prec_lo+","+stat.prec_avg+","+stat.prec_hi);
            PW[u].flush();
            
          }
        }
      }
    }
         
    
    for (int u=0; u < GRT.unit_names.size(); u++) {
      if ((u < from_id) || (u > to_id)) continue;
      PW[u].close();
    }
  }
  
 
  public static void main(String[] args) throws Exception {
    //args = new String[] {"U:/Wes/Climate/export", "211117_FOI_climate_df.csv", "locs", "1950", "2019"};
    //args = new String[] {"U:/Wes/Climate/", "malaysia", "shp", "2012", "2019"};
    //args = new String[] {"U:/Wes/Climate/", "brazil", "shp", "2013", "2020"};
    //args = new String[] {"U:/Wes/Climate/", "-", "pull", "2021", "2022"};
    args = new String[] {"D:/era5-2324/", "-", "pull", "2023", "2024"};
    System.out.println("Args: path csv pull fromyear toyear");
    System.out.println(" or : path csv dayconv yyyy mm dd");
    System.out.println(" or : path csv locs fromyear toyear (Create separate loc time-series from many out_yyyymmdd.txt)");
    System.out.println(" or : path shpfile shp fromyear toyear  (Create a file per admin units in shape file");

    ERA5 E = new ERA5();
            
    if (args[2].equals("pull")) {
      int min_year, max_year;
      if (args[1].equals("-")) {
        min_year = Integer.parseInt(args[3]);
        max_year = Integer.parseInt(args[4]);
        System.out.println(min_year+"-"+max_year);
      } else {
        E.parse_input(args[0] + File.separator + args[1]);// "U:/Wes/Climate/211117_FOI_climate_df.csv");
        min_year = E.from_year.get(0);
        max_year = E.to_year.get(0);
        for (int i = 1; i < E.from_year.size(); i++) {
          min_year = Math.min(min_year, E.from_year.get(i));
          max_year = Math.max(max_year, E.to_year.get(i));
        }
      }
      E.pull_data(min_year, max_year);
      System.exit(0);
    
    } else if (args[2].equals("dayconv")) {
      E.parse_input(args[0] + File.separator + args[1]);// "U:/Wes/Climate/211117_FOI_climate_df.csv");
      int year = Integer.parseInt(args[3]);
      int month = Integer.parseInt(args[4]);
      int day = Integer.parseInt(args[5]);
      E.summarise_days(args[0], year, month, day);
    
    } else if (args[2].equals("locs")) {
      E.parse_input(args[0] + File.separator + args[1]);// "U:/Wes/Climate/211117_FOI_climate_df.csv");
      E.do_loc_data(args[0], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    
    } else if (args[2].equals("shp")) {
     
      GlobalRasterTools GRT = new GlobalRasterTools();
      String path = args[0];
      String shp = path + File.separator + args[1];
      String dbf = shp+".dbf";
      shp = shp + ".shp";
      
      GRT.loadPolygons(2, shp, dbf, null, "");
      if (!new File(path+args[1]+".bin").exists()) {
        GRT.saveUnits(path+args[1]+"_units.txt");
        System.out.println("Making map");
        GRT.makeMap();
        System.out.println("Saving map");
        GRT.saveMapFile(path+args[1]+".bin");
        System.exit(0);
        
      } else {
        GRT.loadUnits(path+args[1]+"_units.txt");
        System.out.println("Reading map "+path+args[1]);
        GRT.loadMapFile(path+args[1]+".bin");
        //int[] extents = GRT.getGlobalExtent(GRT.map, 0, 32767);
        //GRT.hideousShapePNG(GRT.map, path+args[1]+".png", extents, -1, args[1]);
      }
      E.do_admins(GRT, path, Integer.parseInt(args[3]), Integer.parseInt(args[4]), 615, 798); 
    }
  }
}

package jobs.e32_rhys_region_climate;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

import net.sourceforge.jgrib.GribFile;
import net.sourceforge.jgrib.GribRecord;

public class Work { 
  
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
  }
  
  OneDay get_day_data(String tpath, String ppath) throws Exception {
    OneDay D = new OneDay();
    
    if (!new File(tpath).exists()) {
      return null;
    }
    GribFile grb = new GribFile(tpath);
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
    
   
    grb = new GribFile(ppath);
    no_recs = grb.getRecordCount();
    if (no_recs != 1) {
      System.out.println("Preciperror");
    }
    D.parse_record(0, grb.getRecord(1));
    System.out.println("p");
    return D;

  }

	static String workingDir = "D:/Jobs/e32/";
  static String landscan = "Y:/landscan/2020/landscan-global-2020.bil";
   
  public static void main(String[] args) throws Exception {
    Work W = new Work();
    GlobalRasterTools GRT = new GlobalRasterTools();
    /*
    GRT.loadPolygons(0, workingDir + "NHS_England_Regions_April_2020_FEB_EN.shp",
    		workingDir + "NHS_England_Regions_April_2020_FEB_EN.dbf", null, "NHS_REG_20");
  	GRT.makeMap();
  	GRT.saveMapFile(workingDir + "nhs_regions_2020.bin");
  	GRT.saveUnits(workingDir + "nhs_regions_2020.txt");
  	
  	PrintWriter PW = new PrintWriter(workingDir+"output.csv");
  	PW.println("lon_ll,lat_ll,unit,ls2020");
  		
  	int[][] ls2020 = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
  	for (int j=0; j<21600; j++) {
  		float ll_y = (90.0f - (1/120.0f)) - (j / 120.0f);
  		for (int i=0; i<43200; i++) {
  			if (GRT.map[j][i] != -1) {
  				float ll_x = (-180.0f + (i / 120.0f));
  	  		PW.println(ll_x+","+ll_y+","+GRT.map[j][i]+","+ls2020[j][i]);
  			}
  		}
  	}
  	PW.close();*/
    
    GRT.loadMapFile(workingDir + "nhs_regions_2020.bin");
    GRT.loadUnits(workingDir + "nhs_regions_2020.txt");
    
  	int[] extents = GRT.getGlobalExtent(GRT.map,  0, 999);
  	System.out.println(extents[0]+","+extents[1]+","+extents[2]+","+extents[3]);

  	String temp_path = "Y:/ERA5/ERA5_Land/original/temp_and_dewpoint_temp/";
  	String precip_path = "Y:/ERA5/ERA5_Land/original/total_precipitation/";
  	for (int year = 2000; year <= 2020; year++) {
      PrintWriter PW = new PrintWriter(new File(workingDir+"clim"+year+".txt"));
      PW.println("lon,lat,year,month,day,precip,temp_mean,temp_min,temp_max,rh_mean,rh_min,rh_max");
  		for (int month = 1; month <= 12; month++) {
  			for (int day = 1; day <= 31; day++) {
  				String tfile = temp_path+"temp_"+year+((month < 10)?"0":"")+month+
  			                ((day < 10)?"0":"")+day+".grib";
  				String pfile = precip_path+"precip_"+year+((month < 10)?"0":"")+month+
              ((day < 10)?"0":"")+day+".grib";
  				System.out.print(year+"-"+month+"-"+day+":");
  				OneDay D = W.get_day_data(tfile, pfile);
  				if (D == null) {
  					continue;
  				}
  				
  				// 3600 x 1801 - 0.1.. lon = 0.0 to 179.9 ... -180 .. -0.1
  				//                     lat = 90.0 to -90.0
  				
  				// We want: > range(d$lon_ll)
  				// Lon: [1] -6.375000  1.758331   ~ -6.4 to 1.8
  				// Lat: [1] 49.88334 55.80833     ~ 49.8 to 55.9   = 341..402
  				
  				// Lon: 0 to 1.8 = pixels 0 to 18 
  				// Lon: -6.4 to -0.1 = pixels      3536..3599
  				
  				for (int j=341; j<=402; j++) {
  					float lat = (90.0f - (j / 10.0f));
  					String lats = String.format("%.1f", lat);
						for (int i=3536; i<=3618; i++) {
  						int i2 = i % 3600;
  						float lon = (i2 / 10.0f);
  						if (lon >= 180) lon -= 360;
  						String lons = String.format("%.1f", lon);
  						if (!Float.isNaN(D.precip[i2][j])) {
  						  PW.print(lons+","+lats+","+year+","+month+","+day+",");
  						  PW.print(D.precip[i2][j]+",");
  						  PW.print(D.tm_mean[i2][j]+",");
  						  PW.print(D.tm_min[i2][j]+",");
  						  PW.print(D.tm_max[i2][j]+",");
  						  PW.print(D.rh_mean[i2][j]+",");
  						  PW.print(D.rh_min[i2][j]+",");  							
  						  PW.println(D.rh_max[i2][j]);
  						}
  					}
  				}
  				PW.flush();
  			}
  		}
  		PW.close();
  	} 
  }
}

package jobs.e34_euro_wnv;

import java.io.File;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

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
    
    void find_nearest(int xp, int yp) {
      int range = 0;
      int count = 0;
      float maxt = 0, mint = 0, meant = 0, maxrh = 0, minrh = 0, meanrh = 0, prec = 0;
      while (count == 0) {
        range++;
        for (int i2 = xp - range; i2 <= xp + range; i2++) {
          for (int j2 = yp - range; j2 <= yp + range; j2++) {
          	int i3 = i2;
          	if (i3 < 0) i3 += 3600; 
          	if (i3 >= 3600) i3 -= 3600;
          	int j3 = j2;
          	if (j3 < 0) j3 = 0;
          	if (j3 > 1800) j3 = 1080;
          	
          	if (!Float.isNaN(tm_mean[i3][j3])) {
              count++;
              maxt += tm_max[i3][j3];
              mint += tm_min[i3][j3];
              meant += tm_mean[i3][j3];
              maxrh += rh_max[i3][j3];
              minrh += rh_min[i3][j3];
              meanrh += rh_mean[i3][j3];
              prec += precip[i3][j3];
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
    System.out.print("p ");
    return D;

  }

	static String workingDir = "D:/Jobs/e34/";
  static String landscan = "Y:/landscan/2023/landscan-global-2023.bil";
   
  public static void main(String[] args) throws Exception {
    Work W = new Work();
    /*
    for (int level = 0; level <= 3; level++) {
      GlobalRasterTools GRT = new GlobalRasterTools();
      GRT.loadPolygons(level, workingDir + "NUTS_RG_20M_2024_4326.shp",
    		  workingDir + "NUTS_RG_20M_2024_4326.dbf", null, "EURO_NUTS");
  	
      GRT.makeMap();
  	  GRT.saveMapFile(workingDir + "euro_2024_"+level+".bin");
  	  GRT.saveUnits(workingDir + "euro_2024_"+level+".txt");
  	  int[] extents = GRT.getGlobalExtent(GRT.map, 0, 99999);
  	  GRT.hideousShapePNG(GRT.map, workingDir + "/"+level+"/img", extents, level, landscan);
    }
    
  	System.exit(0);
  	*/
    
    GlobalRasterTools GRT = new GlobalRasterTools();
    GRT.loadMapFile(workingDir + "euro_2024_3.bin");
    GRT.loadUnits(workingDir + "euro_2024_3.txt");
    int[][] ls2023 = GRT.loadIntGrid(landscan,43200,21600,43200,21600,0,0);
    
  	PrintWriter PW = new PrintWriter(workingDir+"ls2023.csv");
  	PW.println("lon_ll,lat_ll,unit,ls2020");
  	
  	HashMap<Integer, HashMap<Long, Integer>>  pop_in_unit_by_ll = new HashMap<Integer, HashMap<Long, Integer>>();
  	for (int j=0; j<21600; j++) {
  		float ll_y = (90.0f - (1/120.0f)) - (j / 120.0f);
  		for (int i=0; i<43200; i++) {
  			if ((GRT.map[j][i] != -1) && (ls2023[j][i] > 0)) {
  				int unit = GRT.map[j][i];
  				long ls = (43200*j) + i;
  				if (!pop_in_unit_by_ll.containsKey(unit)) {
  					pop_in_unit_by_ll.put(unit, new HashMap<Long, Integer>());
  				}
  				pop_in_unit_by_ll.get(unit).put(ls, ls2023[j][i]);
  				float ll_x = (-180.0f + (i / 120.0f));
  	  		PW.println(ll_x+","+ll_y+","+GRT.map[j][i]+","+ls2023[j][i]);
  			}
  		}
  	}
  	PW.close();
  	
  	for (int i=0; i<GRT.unit_names.size(); i++) {
  		if (!pop_in_unit_by_ll.containsKey(i)) {
  			System.out.println("Warning, no population points for "+GRT.unit_names.get(i));
  			pop_in_unit_by_ll.put(i,  new HashMap<Long, Integer>());
  		}
  	}
    
  	
    String temp_path = "Y:/ERA5/ERA5_Land/original/temp_and_dewpoint_temp/";
  	String precip_path = "Y:/ERA5/ERA5_Land/original/total_precipitation/";
  	String modis_path = "Y:/MODIS/processed/";
  	
  	for (int year = 2002; year <= 2024; year++) {
      PrintWriter PW_era = new PrintWriter(new File(workingDir+"era5_"+year+".txt"));
      PW_era.println("unit,year,month,day,precip,temp_mean,temp_min,temp_max,rh_mean,rh_min,rh_max");
      PrintWriter PW_modis = new PrintWriter(new File(workingDir+"modis_"+year+".txt"));
      PW_modis.println("unit,year,month,day,mir,evi");
  		for (int month = 1; month <= 12; month++) {
  			for (int day = 1; day <= 31; day++) {
  				String date8 = year+((month < 10)?"0":"")+month+(((day < 10)?"0":"")+day);
  				// ERA 5
  				
  				String tfile = temp_path+"temp_"+date8+".grib";
  				String pfile = precip_path+"precip_"+date8+".grib";
  				
  				System.out.print(date8+" : ");
  				OneDay D = W.get_day_data(tfile, pfile);
  				if (D != null) {
  					System.out.print(" E ");
  				
    				// 3600 x 1801 - 0.1.. lon = 0.0 to 179.9 ... -180 .. -0.1
    				//                     lat = 90.0 to -90.0
  				
    				// We want: > range(d$lon_ll)
  				  // Lon: [1] -6.375000  1.758331   ~ -6.4 to 1.8
  				  // Lat: [1] 49.88334 55.80833     ~ 49.8 to 55.9   = 341..402
  				
  				  // Lon: 0 to 1.8 = pixels 0 to 18 
  				  // Lon: -6.4 to -0.1 = pixels      3536..3599
  					
  					for (int i=0; i<GRT.unit_names.size(); i++) {
  						float total_pop = 0;
  						float total_precip = 0;
  						float total_temp_mean = 0;
  						float total_temp_min = 0;
  						float total_temp_max = 0;
  						float total_rh_mean = 0;
  						float total_rh_min = 0;
  						float total_rh_max = 0;
  						
  						for (Map.Entry<Long, Integer> entry : pop_in_unit_by_ll.get(i).entrySet()) {
  					    long ls = entry.getKey();
  					    int x = (int) (ls % 43200);
  					    int y = (int) (ls / 43200);
  					    float pop = (float) entry.getValue();
  					    total_pop += pop;
  					    // middle of landscan square is...
  					    float lon = (float) ((-180.0 + (x / 120.0)) + (1 / 240.0));
  					    float lat = (float) ((90.0 - (y / 120.0)) - (1 / 240.0));
  					    
  					    int xp,yp;
  				      if (lon >=0) xp = Math.round(lon * 10);
  				      else xp = Math.round(10 * (180 + lon));
  				      yp = Math.round(10* (90 - lat));
  				      
  				      if (Float.isNaN(D.tm_mean[xp][yp])) {
  				        D.find_nearest(xp, yp);
  				      }
  				      
  				      total_precip += (pop * D.precip[xp][yp]);
  				      total_temp_mean += (pop * D.tm_mean[xp][yp]);
  				      total_temp_max += (pop * D.tm_max[xp][yp]);
  				      total_temp_min += (pop * D.tm_min[xp][yp]);
  				      total_rh_mean += (pop * D.rh_mean[xp][yp]);
  				      total_rh_max += (pop * D.rh_max[xp][yp]);
  				      total_rh_min += (pop * D.rh_min[xp][yp]);
  						}
  					  total_precip /= total_pop;
			        total_temp_mean /= total_pop;
			        total_temp_max /= total_pop;
			        total_temp_min /= total_pop;
			        total_rh_mean /= total_pop;
			        total_rh_max /= total_pop;
			        total_rh_min /= total_pop;
			        PW_era.println(i+","+year+","+month+","+day+","+total_precip+","+total_temp_mean+","+total_temp_min+","+total_temp_max+","+total_rh_mean+","+total_rh_min+","+total_rh_max);
  				  }
  					PW_era.flush();
  				}
  				
  				// MODIS
  				
  				String evi_fn = modis_path + date8+"_EVI.bin";
  				String mir_fn = modis_path + date8+"_MIR.bin";
  				if (new File(evi_fn).exists() && new File(mir_fn).exists()) {
  					System.out.print("  M\n");
  					short[][] evi_data = GRT.loadShortGrid(evi_fn, 43200, 21600, 43200, 21600, 0, 0);
            short[][] mir_data = GRT.loadShortGrid(mir_fn, 43200, 21600, 43200, 21600, 0, 0);
  					for (int i=0; i<GRT.unit_names.size(); i++) {
  						float total_pop = 0;
  						float total_evi = 0;
  						float total_mir = 0;

  						for (Map.Entry<Long, Integer> entry : pop_in_unit_by_ll.get(i).entrySet()) {
  					    long ls = entry.getKey();
  					    int pop = entry.getValue();
  							total_pop += pop;
  					    int x = (int) (ls % 43200);
  					    int y = (int) (ls / 43200);
  					    short mir = mir_data[y][x];
            		short evi = evi_data[y][x];
            		if (mir == 0) {
            			short mir2 = 0;
            			float count = 0;
            			for (int yy=y-1; yy<= y+1; yy++)
            				for (int xx=x-1; xx<=x+1; xx++)
            					if (mir_data[yy][xx] !=0) {
            						float weight = ((xx!=x) && (yy!=y)) ? 0.7f : 1.0f;
              				  mir2 += weight * mir_data[yy][xx];
              				  count += weight;
            					}
              		mir = (short) (mir2 / count);
            		}
            		
            		if (evi == 0) {
            			short evi2 = 0;
            			float count = 0;
            			for (int yy=y-1; yy<= y+1; yy++)
            				for (int xx=x-1; xx<=x+1; xx++)
            					if (evi_data[yy][xx] !=0) {
            						float weight = ((xx!=x) && (yy!=y)) ? 0.7f : 1.0f;
              				  evi2 += weight * evi_data[yy][xx];
            				    count += weight;
            					}
            		  evi = (short) (evi2 / count);
          		  }
            		total_evi += (evi * pop);
            		total_mir += (mir * pop);
  						}
  						total_evi /= total_pop;
  						total_mir /= total_pop;
  			      PW_modis.println(i+","+year+","+month+","+day+","+total_mir+","+total_evi);
  					}
  				}
  				PW_modis.flush();
  				System.out.println();
  			}
  		}
  		PW_era.close();
  		PW_modis.close();
  	} 
  	
  	// ELEVATION
  	
    System.out.println("EL-E-VA-TION, woohoo");
    
    String elevData = "Y:\\WorldClim\\Altitude\\wc2.1_30s_elev.bil";
    short[][] elev = GRT.loadShortGrid(elevData, 43200, 21600, 43200, 21600, 0, 0);
    HashMap<Integer, Long> total_elev = new HashMap<>();
    HashMap<Integer, Long> elev_samples = new HashMap<>();

    for (int i=0; i<GRT.unit_names.size(); i++) {
      for (Map.Entry<Long, Integer> entry : pop_in_unit_by_ll.get(i).entrySet()) {
	      long ls = entry.getKey();
	      int pop = entry.getValue();
	      int x = (int) (ls % 43200);
	      int y = (int) (ls / 43200);
	      if (!total_elev.containsKey(i)) {
	      	total_elev.put(i, 0L);
	      	elev_samples.put(i, 0L);
	      }
	      total_elev.put(i, total_elev.get(i) + (pop * elev[y][x]));
	      elev_samples.put(i, elev_samples.get(i) + pop);
      }
    }
    
    PW = new PrintWriter(new File(workingDir+"pop_elevation.csv"));
    PW.println("unit,pop_ls2023,pop_weighted_elevation");
    for (int i=0; i<GRT.unit_names.size(); i++) {
    	if (total_elev.containsKey(i)) {
        PW.println(i+","+elev_samples.get(i)+","+((float)total_elev.get(i) / (float)(elev_samples.get(i))));
    	} else {
    		PW.println(i+",NA,NA");
    	}
    }
    PW.close();
  }
}

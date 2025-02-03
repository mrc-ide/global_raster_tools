package jobs.e31_ilaria_srilanka_nepal;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class Work {
  static String processedModis = "Y:/Modis/Processed";
  static String mapFile = "D:/data/nepal_srilanka_gadm/map.bin";
  static String unitFile = "D:/data/nepal_srilanka_gadm/units.txt";
  static String shapeFiles = "D:/data/nepal_srilanka_gadm/";
  static String gpwPath = "Y:/GPW";
  static String elevData = "Y:/WorldClim/Altitude/wc2.1_30s_elev.bil";

  public int[][] load_gpw4() throws Exception {
    int[][] data = new int[21600][43200];
    int[] xllcorner = new int[] {0, 10800, 21600, 32400, 0, 10800, 21600, 32400};
    int[] yllcorner = new int[] {0, 0, 0, 0, 10800, 10800, 10800, 10800};
    //float mx = 0;
    for (int f=1; f<=8; f++) {
      System.out.println("GPW "+f+"/8");
      BufferedReader br = new BufferedReader(new FileReader(new File(
        gpwPath+"/gpw_v4_population_count_rev11_2015_30_sec_"+f+".asc")));
      for (int j = 0; j < 6; j++) br.readLine();
      for (int j = 0; j < 10800; j++) {
        String[] s = br.readLine().split("\\s+");
        for (int i=0; i<10800; i++) {
          float v = Float.parseFloat(s[i]);
          if (v < 1) data[j + yllcorner[f - 1]][i + xllcorner[f - 1]] = 0;
          else data[j + yllcorner[f - 1]][i + xllcorner[f - 1]] = (int) Math.round(v);
         // mx = Math.max(mx,  Math.round(v));
        }
      }
    }
    /*
    mx = (float) Math.log(mx);
    int[] cols = new int[256];
    for (int i=0; i<255; i++) cols[i] = new Color(i, i, i).getRGB();
    BufferedImage bi = new BufferedImage(2160, 1080, BufferedImage.TYPE_3BYTE_BGR);
    for (int j=0; j<1080; j++) {
      for (int i=0; i<2160; i++) {
      float pix = data[j*20][i*20];
      if (pix > 0) {
        pix = (float) (255.0 * (Math.log(pix) / mx));
        bi.setRGB(i,  j,  cols[(int)pix]);
      }
      }
    }
    ImageIO.write(bi,  "PNG",  new File(gpwPath+"/test.png"));
    */
    return data;

  }

  public void run(String[] args) throws Exception {

    GlobalRasterTools GRT = new GlobalRasterTools();
    
    if (!new File(mapFile).exists()) {
    	GRT.loadPolygonFolder(shapeFiles,  2, "4.1");
      GRT.makeMap();
      /*
      ArrayList<String> new_nums = new ArrayList<String>();
      ArrayList<String> new_names = new ArrayList<String>();
      
      HashMap<String, Integer> squash = new HashMap<String, Integer>();
      HashMap<Integer, Integer> remap = new HashMap<Integer, Integer>();
      for (int i=0; i<GRT.unit_names.size(); i++) {
      	String[] s = GRT.unit_names.get(i).split("\t");
      	String[] n = GRT.unit_numbers.get(i).split("\t");
      	System.out.println("\nProcessing "+s[1]+" -> "+s[2]);
      	if (!squash.containsKey(s[1])) {
      		System.out.println("  -  "+s[1]+" NOT found in squash. names row id = "+new_names.size());
      		new_names.add(s[0]+"\t"+s[1]);
      		new_nums.add(n[0]+"\t"+n[1]);
      		System.out.println("  +  "+s[1]+" => "+squash.size() +" in squash hash");
      		squash.put(s[1], squash.size());
      		remap.put(i, squash.get(s[1]));
      	}
      	else {
      		System.out.println("  *  "+s[1]+" Found in squash. Remap "+i+" => "+squash.get(s[1]));
      		remap.put(i, squash.get(s[1]));
      	}
      }
      
      for (int j=0; j<21600; j++) {
      	if (j % 1000 == 0) System.out.println(j);
      	for (int i=0; i<43200; i++) {
      		 
      		if (remap.containsKey(GRT.map[j][i]))
      			GRT.map[j][i] = remap.get(GRT.map[j][i]);
      	}
      }
      
      GRT.unit_names = new_names;
      GRT.unit_numbers = new_nums;
      */  
      GRT.saveMapFile(mapFile);
      GRT.saveUnits(unitFile);
    } else {
      GRT.loadUnits(unitFile);
      GRT.loadMapFile(mapFile);
    }
    /*
    int[] extents = GRT.getGlobalExtent(GRT.map,  0, GRT.unit_names.size());
    for (int i=0; i<GRT.unit_names.size(); i++) {
    	String name = GRT.unit_names.get(i).split("\t")[1];
      GRT.hideousShapePNG(GRT.map, shapeFiles+"/nepal"+((i<10)?"0":"")+i+".png", extents, i, name+" "+i+"/"+GRT.unit_names.size());
    }
    */
    ArrayList<HashSet<int[]>> points = new ArrayList<HashSet<int[]>>();
    for (int n=0; n<GRT.unit_names.size(); n++) {
  	  HashSet<int[]> unit_points = new HashSet<int[]>();
      System.out.println(n);
      for (int j=0; j<21600; j++) 
      	for (int i=0; i<43200; i++)
      		if (GRT.map[j][i] == n)
    			  unit_points.add(new int[] {i, j});
    			 
    		 
      points.add(unit_points);
    }
    
    int[][] gpw = load_gpw4();
    int[] cols = new int[256];
    for (int i=0; i<256; i++) cols[i] = new Color(i, i, i).getRGB();
    PrintWriter PW = new PrintWriter(shapeFiles + "/export_29Oct2024.csv");
    PW.println("y,m,d,lon,lat,unit,pop,mir,evi");
        
    for (int year = 2024; year <= 2024; year++) {
      for (int month = 1; month <= 12; month++) {
        for (int day = 1; day <= 31; day++) {
          String mirf = ""+year+(month<10?0:"")+month+(day<10?0:"")+day+"_MIR";
          String evif = ""+year+(month<10?0:"")+month+(day<10?0:"")+day+"_EVI";
          if (new File(processedModis+"/"+evif+".bin").exists()) {
            System.out.println(evif);
            short[][] evi_data = GRT.loadShortGrid(processedModis+"/"+evif+".bin", 43200, 21600, 43200, 21600, 0, 0);
            short[][] mir_data = GRT.loadShortGrid(processedModis+"/"+mirf+".bin", 43200, 21600, 43200, 21600, 0, 0);
            
            BufferedImage bi_evi = new BufferedImage(2160, 1080, BufferedImage.TYPE_3BYTE_BGR);
            BufferedImage bi_mir = new BufferedImage(2160, 1080, BufferedImage.TYPE_3BYTE_BGR);
            for (int j=0; j<1080; j++) {
              for (int i=0; i<2160; i++) {
                short pix = evi_data[j*20][i*20];
                if ((pix >= -2000) && (pix <= 10000)) {
                  pix = (short) (255.0 * ((pix + 2000.0) / 12000.0));
                  bi_evi.setRGB(i,  j,  cols[pix]);
                }
                pix = mir_data[j*20][i*20];
                if ((pix >= -2000) && (pix <= 10000)) {
                  pix = (short) (255.0 * ((pix + 2000.0) / 12000.0));
                  bi_mir.setRGB(i,  j,  cols[pix]);
                }
              }
            }
            ImageIO.write(bi_evi, "PNG", new File(shapeFiles+"/"+evif+".png"));
            ImageIO.write(bi_mir, "PNG", new File(shapeFiles+"/"+mirf+".png"));
            
            for (int unit=0; unit<points.size(); unit++) {
            	String[] names = GRT.unit_names.get(unit).split("\t");
            	String name = names[0]+"_"+names[1]+"_"+names[2];
            	HashSet<int[]> unit_points = points.get(unit);
            	for (int[] xy : unit_points) {
            		int lx = xy[0];
            		int ly = xy[1];
            		double dlx = -180.0 + (lx / 120.0) + (1/240);
            		double dly = (90.0 - (1/240.0)) - (ly / 120.0);
            		short mir = mir_data[ly][lx];
            		short evi = evi_data[ly][lx];
            		if (mir == 0) {
            			short mir2 = 0;
            			float count = 0;
            			for (int y=ly-1; y<= ly+1; y++)
            				for (int x=lx-1; x<=lx+1; x++)
            					if (mir_data[y][x] !=0) {
            						float weight = ((lx!=x) && (ly!=y)) ? 0.7f : 1.0f;
              				  mir2 += weight * mir_data[ly-1][lx];
              				  count += weight;
            					}
              		mir = (short) (mir2 / count);
            		}
            		
            		if (evi == 0) {
            			short evi2 = 0;
            			float count = 0;
            			for (int y=ly-1; y<= ly+1; y++)
            				for (int x=lx-1; x<=lx+1; x++)
            					if (evi_data[y][x] !=0) {
            						float weight = ((lx!=x) && (ly!=y)) ? 0.7f : 1.0f;
              				  evi2 += weight * evi_data[y][x];
            				    count += weight;
            					}
            		  evi = (short) (evi2 / count);
          		  }
            		
            		PW.println(year+","+month+","+day+","+dlx+","+dly+","+name+","+gpw[ly][lx]+","+mir+","+evi);
            	}
            }
          }
          PW.flush();
        }
      }
    }
    PW.close();
    
    System.out.println("EL-E-VA-TION, woohoo");
    // Paste on elevation... 
    short[][] elev = GRT.loadShortGrid(elevData, 43200, 21600, 43200, 21600, 0, 0);
    BufferedReader br = new BufferedReader(new FileReader(shapeFiles+"/export_29Oct2024.csv"));
    PW = new PrintWriter(new File(shapeFiles+"/export_29Oct2024_elev.csv"));
    PW.println(br.readLine()+",altitutde");
    String s = br.readLine();
    while (s!= null) {
    	String[] bits = s.split(",");
    	float lon = Float.parseFloat(bits[3]);
    	float lat = Float.parseFloat(bits[4]);
    	int x = (int) Math.round((lon + 180.0) * 120.0);
    	int y = (int) Math.round((89.99167 - lat) * 120.0);
    	PW.println(s+","+elev[y][x]);
    	s = br.readLine();
    }
    PW.close();
    br.close();

  }

  public static void main(String[] args) throws Exception {
    Work lt = new Work();
    lt.run(args);

  }
}

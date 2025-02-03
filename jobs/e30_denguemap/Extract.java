package jobs.e30_denguemap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;
// Extra helper to get a list of 20x20km landscan cells that
// contain multiple admin units, and list populations for 
// each admin unit in that cell. (ie, duplicate (lon,lat), with different pop/admin)
public class Extract {
  static String csvIn = "D:\\data\\denguemap\\Pixel_Esimates_16_July.csv";
  static String csvOut = "D:\\data\\denguemap\\result.csv";
  static String mapFile = "D:\\data\\denguemap\\map.bin";
  static String unitsFile = "D:\\data\\denguemap\\units.txt";
  static String lsPath = "D:\\data\\denguemap\\landscan-global-2022.bil";
  static String csvImg = "D:\\data\\denguemap\\csvimg.png";
  static String lsImg = "D:\\data\\denguemap\\lsimg.png";

  public static void main(String[] args) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    System.out.println("Loading landscan");
    int[][] lscan = GRT.loadIntGrid(lsPath, 43200, 21600, 43200, 21600, 0, 0);
    long tot=0;
    for (int j=4560; j<=4580; j++) {
    	System.out.print(j+" : ");
    	for (int i=379; i<=399; i++) {
    		System.out.print(lscan[j][i]+",");
    		tot+=lscan[j][i];
    	}
    	System.out.println();
    }
    System.out.println("tot="+tot);
    System.out.println("Loading units");
    GRT.loadUnits(unitsFile);
    System.out.println("Loading map");
    GRT.loadMapFile(mapFile);
    BufferedImage csvPng = new BufferedImage(2160, 1080, BufferedImage.TYPE_3BYTE_BGR);
    BufferedImage lsPng = new BufferedImage(2160, 1080, BufferedImage.TYPE_3BYTE_BGR);
    int[] cols = new int[256];
    for (int i=0; i<256; i++) {
    	cols[i] = new Color(i, i, i).getRGB();
    }
    
    BufferedReader br = new BufferedReader(new FileReader(csvIn));
    // cell,latitude,longitude,landscan_2022,country,ID_0,admin 1,ID_1,admin 2,ID_2,FOI
    // previous also: mean_FOI,sd_FOI,lCI_FOI,uCI_FOI,median_FOI,mean_p9,sd_p9,lCI_p9,uCI_p9,median_p9
    PrintWriter PW = new PrintWriter(csvOut);
    String header = br.readLine();
    header = header.replaceAll(",",  "\t");
    header += "\tunit_landscan_2022";
    PW.println(header);
    String s = br.readLine();
    int line = 0;
    HashMap<Integer, Integer> unitPop = new HashMap<Integer, Integer>();
    double maxp=Math.log(14399290);
    long tot1 = 0;
    long tot2 = 0;
    while (s != null) {
    	line++;
    	if ((line % 1000) == 0) System.out.println(line);
    	String[] ss = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    	float lat = Float.parseFloat(ss[1]);
    	float lon = Float.parseFloat(ss[2]);
    	int ls2022 = Integer.parseInt(ss[3]);
    	tot1 += ls2022;
    	int xLL = (int) Math.round((180 + lon) * 120.0);
    	int yLL = (int) Math.round(((89.99167 - lat) * 120.0));
    	int cellpop = 0;
    	unitPop.clear();
    	for (int x = 0; x < 20; x++) {
    		for (int y = 0; y < 20; y++) {
    			int unit = GRT.map[yLL - y][xLL + x];
    			int pop = lscan[yLL - y][xLL + x];
    			if (unit >= 0) {
    			  if (!unitPop.containsKey(unit)) {
    			  	unitPop.put(unit, 0);
    			  }
    			  unitPop.put(unit, unitPop.get(unit) + pop);
    			  cellpop += pop;
    			}
    		}
    	}
    	tot2 += cellpop;
    	if (cellpop > 0) {
    		lsPng.setRGB(xLL / 20, yLL / 20, cols[(int) Math.max(0, ((Math.log(cellpop) / maxp) * 255.0))]);
    	}
    	csvPng.setRGB(xLL / 20,  yLL / 20, cols[(int) Math.max(0, ((Math.log(ls2022) / maxp) * 255.0))]);
    	
    	if (cellpop != ls2022) {
    		System.out.println("Count error, lon = "+lon+", lat = "+lat+", csv = "+ls2022+", ls2022 = "+cellpop+", xLL = "+xLL+", yLL = "+yLL);
    	}
    	
    	
    	// The actual work.
    	for (Integer unit : unitPop.keySet()) {
    		int onePop = unitPop.get(unit);
    		String[] names = GRT.unit_names.get(unit).split("\t");
    		String[] ids = GRT.unit_numbers.get(unit).split("\t");
    	   PW.println(ss[0]+"\t"+ss[1]+"\t"+ss[2]+"\t"+ss[3]+"\t"+   // cell, latitude, longitude, country
    	              names[0]+"\t"+ids[0]+"\t"+names[1]+"\t"+ids[1]+"\t"+names[2]+"\t"+ids[2]+"\t"+ss[10]+"\t"+onePop);
    	}
    
    	s = br.readLine();
    }
    System.out.println("Totals: csv = "+tot1+", ls = "+tot2);
    ImageIO.write(csvPng, "PNG",  new File(csvImg));
    ImageIO.write(lsPng, "PNG",  new File(lsImg));
    br.close();
    PW.close();
  }
}

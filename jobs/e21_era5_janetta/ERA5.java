package jobs.e21_era5_janetta;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

import net.sourceforge.jgrib.GribFile;
import net.sourceforge.jgrib.GribRecord;

public class ERA5 {
  static final String workingDir = "E:/Jobs/Era/";
  static final String all_times = "['00:00','01:00','02:00','03:00','04:00','05:00','06:00','07:00','08:00','09:00','10:00','11:00','12:00',"
                                 + "'13:00','14:00','15:00','16:00','17:00','18:00','19:00','20:00','21:00','22:00','23:00']";
  static final int WIDTH = 3600;
  static final int HEIGHT = 1800;
  static final int MONTHS = 12;
  static final int HOURS = 24;
  static final String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  GlobalRasterTools GRT = null;
  
  ArrayList<String> country_list = new ArrayList<String>();
  ArrayList<String> code_list = new ArrayList<String>();
  ArrayList<Integer> months = new ArrayList<Integer>();
  
  // Returns data [width][height][hours]
  
  void writeDownload(String variable, String year, String month, String day, String time, String result) throws Exception {
    PrintWriter PW = new PrintWriter(new File(workingDir + "fetch.py"));
    PW.println("import cdsapi");
    PW.println("c = cdsapi.Client()");
    PW.println("c.retrieve('reanalysis-era5-land', {");
    PW.println("             'variable': '"+variable+"',");
    PW.println("             'year' : '"+year+"',");
    PW.println("             'month' : '"+month+"',");
    PW.println("             'day' : '"+day+"',");
    PW.println("             'time' : "+time+",");
    PW.println("             'format': 'grib'");
    PW.println("            }, '"+workingDir+result+"')");
    PW.close();
  
  }
 
  String leadingZero(int m) {
    return ((m<10)?"0":"")+String.valueOf(m);
  }
  
  GribFile getGrib(String file, int expected_records) throws Exception {
    GribFile grb = null;
    boolean success = false;
    while (!success) {
      grb = null;
      if (new File(workingDir+file).exists()) {
        new File(workingDir+file).delete();
      }
      
      Process p = Runtime.getRuntime().exec("python " + workingDir + "fetch.py 1 >" + workingDir + "python_out.txt 2>&1");

      // The download from the API sometimes stalls forever... timeout after 60 seconds and retry.
      
      p.waitFor(60, TimeUnit.SECONDS);
      
      try {
        success = (p.exitValue() == 0);
        if (success) {
          grb = new GribFile(workingDir + file);
          success = (grb.getRecordCount() == expected_records);
          System.out.print(success?"/":"x");
        }
      } catch (Exception e) {
        System.out.print("x");
      }
    }
    return grb;
  }
  
  float[][][] fetchGribDay(String variable, int year, int month, int day) throws Exception {
    float[][][] data;
    int expected_records;
    System.out.print("Fetching " + variable + " for " + year + "/" + month + "/" + day + "... ");
    
    // For 2m_temperature, we ask for all the hours in the day, and we get them by querying that day - hours 0-23.
    
    data = new float[HOURS][WIDTH][HEIGHT];
    writeDownload(variable, String.valueOf(year), leadingZero(month), leadingZero(day), all_times, "download1.grib");
    expected_records = 24;
    
    GribFile grb = getGrib("download1.grib", expected_records);
    System.out.print(" reading grib ... ");
    
    for (int rec = 1; rec <= expected_records; rec++) {
      GribRecord grecord = grb.getRecord(rec);
      GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
      gc.setTimeInMillis(grecord.getTime().getTimeInMillis());
      int hour = gc.get(GregorianCalendar.HOUR_OF_DAY);
      
      for (int lon = 0; lon < WIDTH; lon++) {
        for (int lat = 0; lat < HEIGHT; lat++) {
          data[hour][lon][lat] = grecord.getValue(lon,  lat);
        }
      }
    }
    
    System.out.println(" done");
    return data;
  }
  
  void testPlot(float[][][] data, String file, int month1, int month2) throws Exception {
    float min = Float.POSITIVE_INFINITY;
    float max = Float.NEGATIVE_INFINITY;
    for (int month = month1 - 1; month < month2; month++) {
      for (int i = 0; i < WIDTH; i++) {
        for (int j = 0; j < HEIGHT; j++) {
          if (!Double.isInfinite(data[month][i][j])) {
            min = Math.min(min,  data[month][i][j]);
            if (data[month][i][j]>max) {
              //System.out.println("Max: "+i+", "+j+", month = "+month+", val = "+data[month][i][j]);
            }
            max = Math.max(max,  data[month][i][j]);
          }
        }
      }
    }
    System.out.println("Min = "+min+", Max = "+max);

    int[] cols = new int[256];
    for (int i = 0; i <= 255; i++) cols[i] = new Color(i, i, i).getRGB();
    
    for (int month = month1 - 1; month < month2; month++) {
      BufferedImage bi = new BufferedImage(WIDTH / 10, HEIGHT / 10, BufferedImage.TYPE_3BYTE_BGR);
      for (int i = 0; i < WIDTH; i += 10) {
        for (int j = 0; j < HEIGHT; j += 10) {
          if (!Double.isInfinite(data[month][i][j])) {
            int scale = (int) (255 * (data[month][i][j] - min) / (max - min));
            bi.setRGB(i / 10, j / 10, cols[scale]);
          }
        }
      }
      ImageIO.write(bi,  "PNG", new File(file + "_" + ((month + 1 < 10)?"0":"") + (month+1) + ".png"));
    }
  }
  
  public void saveFloats(float[][] data, String file) throws Exception {
    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    for (int i = 0; i < WIDTH; i++)
      for (int j = 0; j < HEIGHT; j++)
        dos.writeFloat(data[i][j]);
    dos.close();
  }
  
  public void saveFloats(double[][] data, String file) throws Exception {
    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    for (int i = 0; i < WIDTH; i++)
      for (int j = 0; j < HEIGHT; j++)
        dos.writeFloat((float)data[i][j]);
    dos.close();
  }
  
  public float[][] loadFloats(String file) throws Exception {
    float[][] data = new float[WIDTH][HEIGHT];
    DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    for (int i = 0; i < WIDTH; i++)
      for (int j = 0; j < HEIGHT; j++)
        data[i][j] = dis.readFloat();
    dis.close();
    return data;
  }
  
  public void doTemperature(int year, int month1, int month2) throws Exception {
    float[][][] max_temp = new float[MONTHS][WIDTH][HEIGHT];
    float[][][] min_temp = new float[MONTHS][WIDTH][HEIGHT];
    double[][][] avg_temp = new double[MONTHS][WIDTH][HEIGHT];
    int[][][] samples = new int[MONTHS][WIDTH][HEIGHT];

    for (int m = month1 - 1; m < month2; m++) {
      for (int i = 0; i < WIDTH; i++) {
        for (int j = 0; j < HEIGHT; j++) {
          samples[m][i][j]=0;
          max_temp[m][i][j] = Float.NEGATIVE_INFINITY;
          min_temp[m][i][j] = Float.POSITIVE_INFINITY;
          avg_temp[m][i][j] = Float.NEGATIVE_INFINITY;
        }
      }
    }
    
    GregorianCalendar gc = new GregorianCalendar();
    gc.set(GregorianCalendar.DAY_OF_MONTH,  1);
    gc.set(GregorianCalendar.YEAR,  year);
    for (int month = month1 - 1; month < month2; month++) {
      gc.set(GregorianCalendar.MONTH,  month);
      int month_length = gc.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
      for (int day = 1; day <= month_length; day++) {
        float[][][] data = fetchGribDay("2m_temperature", year, month + 1, day);
        for (int i = 0; i < WIDTH; i++) {
          for (int j = 0; j < HEIGHT; j++) {
            for (int hour = 0; hour < HOURS; hour++) {
              float val = data[hour][i][j];
              if (val < 1000) {
                samples[month][i][j]++;
                max_temp[month][i][j] = Math.max(max_temp[month][i][j], val);
                min_temp[month][i][j] = Math.min(min_temp[month][i][j], val);
                if (Double.isInfinite(avg_temp[month][i][j])) avg_temp[month][i][j] = 0;
                avg_temp[month][i][j] += val;
              }
            }
          }
        }
      }
      for (int i = 0; i < WIDTH; i++) {
        for (int j = 0; j < HEIGHT; j++) {
          if (samples[month][i][j] > 0) {
            avg_temp[month][i][j] = avg_temp[month][i][j] / (double) samples[month][i][j];
          } else {
            System.out.println("No samples for "+month+","+i+","+j);
          }
        }
      }
      saveFloats(min_temp[month], workingDir + "min_temp_" + year + "_" + ((month + 1 < 10) ? "0" : "") + String.valueOf(month + 1) + ".bin");
      saveFloats(max_temp[month], workingDir + "max_temp_" + year + "_" + ((month + 1 < 10) ? "0" : "") + String.valueOf(month + 1) + ".bin");
      saveFloats(avg_temp[month], workingDir + "avg_temp_" + year + "_" + ((month + 1 < 10) ? "0" : "") + String.valueOf(month + 1) + ".bin");
    }
  }
  
  public void makeImages(String file, int month1, int month2) throws Exception {
    System.out.print(file+"  ");
    float[][][] data = new float[MONTHS][WIDTH][HEIGHT];
    for (int month = month1; month <= month2; month++) {
      String fname = workingDir + file+"_"+((month<10)?"0":"")+month+".bin";
      data[month-1] = loadFloats(fname);
    }
    testPlot(data, workingDir + file, month1, month2);
  }
  
  public void makeAllImages(int year, int month1, int month2) throws Exception {
    makeImages("min_temp_"+year, month1, month2);
    makeImages("max_temp_"+year, month1, month2);
    makeImages("avg_temp_"+year, month1, month2);
  }
  
  public void prepareMapping() throws Exception {
    GRT = new GlobalRasterTools();
    GRT.loadPolygonFolder(gadmPath, 0, "3.6");
    if (!new File(workingDir+"map.bin").exists()) {
      System.out.println("Making map");
      GRT.makeMap();
      System.out.println("Saving map");
      GRT.saveMapFile(workingDir+"map.bin");
      GRT.saveUnits(workingDir+"units.txt");
    } else {
      GRT.loadUnits(workingDir+"units.txt");
      System.out.println("Reading map");
      GRT.loadMapFile(workingDir+"map.bin");
    }
  }
  
  public void doMapping(int year, String outfile, int no_months) throws Exception {
    
    // Look up all the admin codes we are interested in....
    
    HashMap<Integer, Integer> wanted = new HashMap<Integer, Integer>();
    HashSet<Integer> not_wanted = new HashSet<Integer>();
    int[] pixel_count = new int[country_list.size()];
    ArrayList<ArrayList<Integer>>pixels = new ArrayList<ArrayList<Integer>>();
    for (int i=0; i<country_list.size(); i++) pixels.add(new ArrayList<Integer>());
    
    for (int j=0; j<1800; j++) {
      for (int i=0; i<3600; i++) {
        int ls_x = ((i*12)+21600) % 43200;
        int ls_y = j*12;
        int admin_code = GRT.map[ls_y][ls_x];
        if (admin_code == -1) continue;
        if (not_wanted.contains(admin_code)) continue;
        if (!wanted.containsKey(admin_code)) {
          String country = GRT.unit_names.get(admin_code);
          if (!country_list.contains(country)) {
            not_wanted.add(admin_code);
            System.out.println("Not wanted "+admin_code+" - "+country);
            continue;
          }
          wanted.put(admin_code, country_list.indexOf(country));
        }
        int index = wanted.get(admin_code);
        pixel_count[index]++;
        pixels.get(index).add(i);
        pixels.get(index).add(j);
      }
    }
    
    for (int i=0; i<country_list.size(); i++) {
      System.out.println(country_list.get(i)+", pixels = "+pixels.get(i).size()/2);
    }
        
    // Lets test the pixels
    
    BufferedImage bi = new BufferedImage(3600, 1800, BufferedImage.TYPE_3BYTE_BGR);
    for (int i=0; i<pixels.size(); i++) {
      int c = new Color((int)(Math.random()*255),128+(int)(Math.random()*127),(int)(Math.random()*255)).getRGB();
      for (int j=0; j<pixels.get(i).size(); j+=2)
        bi.setRGB(pixels.get(i).get(j), pixels.get(i).get(j+1), c);
    }
    ImageIO.write(bi, "PNG", new File(workingDir+"eramap.png"));
    
    // We've got the pixels. Now we want for each month, to add up the values...
    
    // Order: JAN min_temp, avg_temp, max_temp, - repeat for 12 months. 
    //       
    String[] files = new String[] {"min_temp", "avg_temp", "max_temp"};
    String[] mons = new String[] {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

    double[][] data = new double[country_list.size()][no_months * files.length];
    
    for (int month = 0; month<no_months; month++) {
      System.out.print(year+" "+mons[month]+" ______\r");
      System.out.print(year+" "+mons[month]+" ");
      for (int file = 0; file<files.length; file++) {
        float[][] eradata = loadFloats(workingDir+files[file]+"_"+year+"_"+leadingZero(month+1)+".bin");
        for (int country=0; country < country_list.size(); country++) {
          int pix=0;
          int hits=0;
          for (int i=0; i<pixel_count[country]; i++) {
            int x = pixels.get(country).get(pix++);
            int y = pixels.get(country).get(pix++);
            if (!Float.isInfinite(eradata[x][y])) {
              data[country][(file*no_months)+month]+=eradata[x][y];
              hits++;
            }
          }
          pixel_count[country]=hits;
          
          data[country][(file*no_months)+month]/=(double) hits;
        }
        System.out.print("#");
      }
      System.out.println("");
    }
    
    PrintWriter PW = new PrintWriter(new File(workingDir + outfile));
    PW.print("Country\tCode\tPixels\t");
    
    for (int f=0; f<files.length; f++) {
      for (int m=0; m<no_months; m++) {
        PW.print(files[f]+"_"+mons[m]+"\t");
      }
    }
    PW.println();
    for (int i=0; i<country_list.size(); i++) {
      PW.print(country_list.get(i)+"\t"+code_list.get(i)+"\t"+pixel_count[i]+"\t");
      for (int f=0; f<files.length * no_months; f++) {
        PW.print(String.valueOf(data[i][f] - 273.15)+"\t"); 
      }
      PW.println();
    }
    PW.close();
  }
  
  public void getMeta() throws Exception {
    System.out.println(new File("x").getAbsolutePath());
    BufferedReader br = new BufferedReader(new FileReader("jobs/e21_era5_janetta/date_of_fifth_death.csv"));
    br.readLine(); // Header
    String s = br.readLine();
    s = br.readLine();
    while (s!=null) {
      String[] bits = s.split(",");
      if ((bits.length>=3) && (!bits[2].trim().equals(""))) {
        country_list.add(bits[0]);
        code_list.add(bits[1]);
        System.out.println("*"+code_list.get(code_list.size()-1)+"*\t*"+country_list.get(country_list.size()-1)+"*");
        String[] dates = bits[2].split("-");
        months.add(Integer.parseInt(dates[1]));
      }
      s = br.readLine();
    }
    br.close();
  }
  
  public static void main(String[] args) throws Exception {
    ERA5 E = new ERA5();
    E.getMeta();
    E.prepareMapping();
    for (int i=2020; i>=2020; i--) {
      //E.doTemperature(i, 1,8);
      E.doMapping(i, "climate"+i+".txt", 8);
    }
    E.makeImages("max_temp_2020", 1 ,8);
    E.makeImages("min_temp_2020", 1 ,8);
    E.makeImages("avg_temp_2020", 1 ,8);
  }
}

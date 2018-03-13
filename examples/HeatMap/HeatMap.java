package examples.HeatMap;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class HeatMap {
  
  /* List of GADM strings to identify the countries we're interested in */
  
  static final List<String> countries = Arrays.asList(new String[] {
      "Afghanistan", "Angola", "Armenia", "Azerbaijan", "Burundi", "Benin", "Burkina Faso", "Bangladesh",
      "Bolivia", "Bhutan", "Central African Republic", "Côte d'Ivoire", "Cameroon", "Democratic Republic of the Congo",
      "Republic of Congo", "Comoros", "Cuba", "Djibouti", "Eritrea", "Ethiopia", "Georgia", "Ghana", "Guinea",
      "Gambia", "Guinea-Bissau", "Guyana", "Honduras", "Haiti", "Indonesia", "India", "Kenya", "Kyrgyzstan", "Cambodia",
      "Kiribati", "Laos", "Liberia", "Sri Lanka", "Lesotho", "Moldova", "Madagascar", "Mali", "Myanmar", "Mongolia",
      "Mozambique", "Mauritania", "Malawi", "Niger", "Nigeria", "Nicaragua", "Nepal", "Pakistan", "Papua New Guinea",
      "North Korea", "Rwanda", "Sudan", "Senegal", "Solomon Islands", "Sierra Leone", "Somalia", "South Sudan",
      "Sao Tome and Principe", "Chad", "Togo", "Tajikistan", "East Timor", "Tanzania", "Uganda", "Ukraine", "Uzbekistan",
      "Vietnam", "Yemen", "Zambia", "Zimbabwe", "Albania", "Bosnia and Herzegovina", "Belize", "China", "Cape Verde", 
      "Egypt", "Fiji", "Micronesia", "Guatemala",
      "Iraq", "Morocco", "Marshall Islands", "Philippines", "Paraguay", "Palestina", "El Salvador", "Swaziland",
      "Syria", "Turkmenistan", "Tonga", "Tunisia", "Tuvalu", "Vanuatu", "Samoa", "Kosovo"});
  
  static final List<String> codes = Arrays.asList(new String[] {
      "AFG","AGO","ARM","AZE","BDI","BEN","BFA","BGD","BOL","BTN","CAF","CIV","CMR","COD","COG","COM","CUB",
      "DJI","ERI","ETH","GEO","GHA","GIN","GMB","GNB","GUY","HND","HTI","IDN","IND","KEN","KGZ","KHM","KIR",
      "LAO","LBR","LKA","LSO","MDA","MDG","MLI","MMR","MNG","MOZ","MRT","MWI","NER","NGA","NIC","NPL","PAK",
      "PNG","PRK","RWA","SDN","SEN","SLB","SLE","SOM","SSD","STP","TCD","TGO","TJK","TLS","TZA","UGA","UKR",
      "UZB","VNM","YEM","ZMB","ZWE","ALB","BIH","BLZ","CHN","CPV","EGY","FJI","FSM","GTM","IRQ","MAR","MHL",
      "PHL","PRY","PSE","SLV","SWZ","SYR","TKM","TON","TUN","TUV","VUT","WSM","XK"});
  

  int grads = 255;
  Color[] cols = new Color[255];
  
  int[] me_to_GRT = new int[codes.size()];
  int[] GRT_to_me;
  GlobalRasterTools GRT = new GlobalRasterTools();
  int first_year, last_year;
  double min_val, val_range;
  
  String csvfile = null;
  String outgif = null;
  String mapfile = null;
  String unitfile = null;
  int frame_pause = 0;
  boolean loop = false;
  
  String group = "";
  String disease = "";
  String scenario = "";
  String outcome = "";
  
  double[][] data = new double[98][];
  boolean[] country_in_use = new boolean[codes.size()];
  
  public String[] readLine(BufferedReader br) throws Exception {
    String s = br.readLine();
    if (s!=null) {
      String[] tokens = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
      for (int i=0; i<tokens.length; i++) {
        tokens[i]=tokens[i].replace("\"", "");
      }
      return tokens;
    } else return null;
  }
    
  public void loadData(String file) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(file));
    List<String> header = Arrays.asList(readLine(br));
    
    int country_column = header.indexOf("country");
    int year_column = header.indexOf("year");
    int value_column = header.indexOf("value");
    int outcome_column = header.indexOf("outcome_code");
    int scenario_column = header.indexOf("scenario");
    int group_column = header.indexOf("modelling_group");
    int disease_column = header.indexOf("disease");
    
    // Do some checks first.
    String[] ss = readLine(br);
    first_year = Integer.MAX_VALUE;
    last_year = Integer.MIN_VALUE;
    double max_val = Double.NEGATIVE_INFINITY;
    while (ss!=null) {
      if (codes.indexOf(ss[country_column])==-1) {
        System.out.println("Error - country "+ss[country_column]+" not found");
        System.exit(-1);
      } else {
        country_in_use[codes.indexOf(ss[country_column])]=true;
      }
      first_year = Math.min(first_year, Integer.parseInt(ss[year_column]));
      last_year = Math.max(last_year,  Integer.parseInt(ss[year_column]));
      min_val = Math.min(min_val,  Integer.parseInt(ss[value_column]));
      max_val = Math.max(max_val, Integer.parseInt(ss[value_column]));
      ss = readLine(br);
    }
    br.close();
    for (int i=0; i<data.length; i++) {
      data[i] = new double[1+(last_year-first_year)];
    }
    br = new BufferedReader(new FileReader(file));
    br.readLine();
    ss = readLine(br);
    boolean first_row = true;
    while (ss!=null) {
      if (first_row) {
        if (outcome_column>=0) outcome = ss[outcome_column];
        if (scenario_column>=0) scenario = ss[scenario_column];
        if (group_column>=0) group = ss[group_column];
        if (disease_column>=0) disease = ss[disease_column];
        first_row=false;
      }
      
      data[codes.indexOf(ss[country_column])][Integer.parseInt(ss[year_column])-first_year]=Double.parseDouble(ss[value_column]);
      ss = readLine(br);
    }
    
    int no_countries = 0;
    for (int i=0; i<country_in_use.length; i++) {
      if (country_in_use[i]) no_countries++;
    }
    System.out.println("Found "+no_countries+" countries");
    System.out.println("Year "+first_year+" - "+last_year);
    System.out.println("Values "+min_val+" - "+max_val);
    val_range = max_val-min_val;
  }
  
  public HeatMap() {}
  
  public void niceGraphics(Graphics2D gg) {
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    gg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    gg.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    gg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_NORMALIZE);
  }

  
  public static void main(String[] args) throws Exception {
    if (args.length<5) {
      System.out.println("Usage: java -Xmx8g HeatMap input.csv output.gif map.bin units.txt 500 true");
      System.out.println("                    (500 = 500ms between frames");
      System.out.println("                    (true = LOOP, false = DON'T LOOP)\n");
      System.out.println("Input CSV must contain header, and columns");
      System.out.println(" (necessary)    country, year, value");
      System.out.println(" (optional)     outcome_code, scenario, modelling_group, disease");
      System.exit(0);
    }
    
    HeatMap HM = new HeatMap();
    HM.csvfile = args[0];
    HM.outgif = args[1];
    HM.mapfile = args[2];
    HM.unitfile = args[3];
    HM.frame_pause = Integer.parseInt(args[4]);
    HM.loop = Boolean.parseBoolean(args[5]);
    
    HM.run();
    System.exit(0);
  }
  
  public void initCols() {
    double frac,part;
    for (int i=0; i<grads; i++) {
      frac = (double)i/grads;
      
      if (frac<(0.1)) {
        cols[i]=new Color(0,0,(int) (128+(127*10*frac)));
        
      } else if (i<3.0*grads/10) {
        part=frac-(1/10.0);
        cols[i]=new Color(0,(int) (255*part*5),255);
        
      } else if (i<grads/2.0) {
        part=frac-(3.0/10.0);
        cols[i]=new Color(0,255,(int) (255-(255*part*5)));
        
      } else if (i<7.0*grads/10) {
        part=frac-(1/2.0);
        cols[i]=new Color((int) (255*part*5),255,0);
        
      } else if (i<9.0*grads/10) {
        part=frac-(7/10.0);
        cols[i]=new Color(255,(int) (255-(255*part*5)),0);
        
      } else  {
        part=frac-(0.9);
        cols[i]=new Color(255-(int) (128*part*10),0,0);
      } 
    }

  }
  
  public void run() throws Exception {
    initCols();
    loadData(csvfile);
    GRT.loadUnits(unitfile);
    GRT_to_me = new int[GRT.unit_names.size()];
    for (int i=0; i<GRT.unit_names.size(); i++) GRT_to_me[i]=-1;
    for (int i=0; i<me_to_GRT.length; i++) {
      int index = GRT.unit_names.indexOf(countries.get(i));
      me_to_GRT[i] = GRT.unit_names.indexOf(countries.get(i));
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
    
    // Plot the world
    
    System.out.println("Plot");
    
    for (int j=0; j<GRT.map.length; j++) {
      for (int i=0; i<GRT.map[j].length; i++) {
        if (GRT.map[j][i]>=0) {
          png.setRGB(i/scale_down, j/scale_down, grey);
          if (GRT_to_me[GRT.map[j][i]]>=0) {
            pixmap[i/scale_down][j/scale_down]=GRT_to_me[GRT.map[j][i]];
          }
        } 
      }
    }
    
    System.out.println("Boundary thickening");
    
    // Make boundary lines a bit thicker.
    
    for (int j=0; j<GRT.map.length-1; j++) {
      for (int i=0; i<GRT.map[j].length-1; i++) {
        int c1=GRT.map[j][i];
        int c2=GRT.map[j+1][i+1];
        int c3=GRT.map[j][i+1];
        int c4=GRT.map[j+1][i];
        int max = Math.max(c1, Math.max(c2, Math.max(c3,c4)));
        if (c1==-1) c1=max;
        if (c2==-1) c2=max;
        if (c3==-1) c3=max;
        if (c4==-1) c4=max;
        if ((c1!=c2) || (c1!=c3) || (c1!=c4) || (c2!=c3) || (c2!=c4) || (c3!=c4)) {
          png.setRGB(i/scale_down, j/scale_down, black);
          pixmap[i/scale_down][j/scale_down]=-1;
        }
      }
    }
    
    Graphics2D g = (Graphics2D) png.createGraphics();
    niceGraphics(g);
    Font f = new Font("Calibri", Font.PLAIN, 48);
    g.setFont(f);
    g.setColor(Color.BLACK);
    FontMetrics fm = g.getFontMetrics(f);
    if (group.length()>0) {
      g.drawString("Group:", 1000-fm.stringWidth("Group:"),800);
      g.drawString(group, 1025,800);
    
    }
    if (disease.length()>0) {
      g.drawString("Disease:", 1000-fm.stringWidth("Disease:"),850);
      g.drawString(disease, 1025, 850);
    }
    if (scenario.length()>0) {
      g.drawString("Scenario:", 1000-fm.stringWidth("Scenario:"), 900);
      g.drawString(scenario, 1025, 900);
    }
    
    if (outcome.length()>0) g.drawString(outcome, 235,725);
     
    for (int y=0; y<=500; y++) {
      g.setColor(cols[(int)(254.0*(1.0-(y/500.0)))]);
      g.drawLine(75,450+y,90,450+y);
    }
    f = new Font("Calibri", Font.PLAIN, 32);
    g.setFont(f);
    g.setColor(Color.black);
    g.drawLine(92, 450, 93, 950);
    g.drawLine(93, 450, 92, 950);
    for (int y = 0; y <= 500; y += 100) {
      g.drawLine(95, y + 450, 105,y + 450);
      double v = min_val+((1.0-(y / 500.0))*val_range);
      int iv = (int) (Math.round(v));
      g.drawString(String.valueOf(iv), 120, y + 462);

    }
     
    ImageIO.write(png, "PNG", new File("back.png"));
    
    // Save animated gif...
    
    ImageOutputStream output = new FileImageOutputStream(new File(outgif));
    GifSequenceWriter writer = new GifSequenceWriter(output, png.getType(), frame_pause, loop);
    
    for (int year = first_year; year<=last_year; year++) {
      System.out.println("Writing "+year);
      if (year!=first_year) {
        png = ImageIO.read(new File("back.png"));
        g.dispose();
        g = (Graphics2D) png.createGraphics();
        niceGraphics(g);
      }
      f = new Font("Calibri", Font.BOLD, 64);
      g.setFont(f);
      g.setColor(Color.BLACK);
      g.drawString(String.valueOf(year), 2000,75);

      for (int i=0; i<w; i++) {
        for (int j=0; j<h; j++) {
          if (pixmap[i][j]!=-1) {
            double val = data[pixmap[i][j]][year-first_year];
            int col = cols[(int)Math.round(254.0*(val - min_val)/val_range)].getRGB();
            png.setRGB(i,j,col);
          }
        }
      }
      writer.writeToSequence(png);
    }
    writer.close();
    output.close();
  }
}

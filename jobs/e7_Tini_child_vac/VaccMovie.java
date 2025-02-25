package jobs.e7_Tini_child_vac;

import java.awt.Color;
import java.awt.Font;
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

import com.mrc.GifSequenceWriter.GifSequenceWriter;
import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class VaccMovie {
  
  /* List of GADM strings to identify the countries we're interested in */
  static final String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  static final String outPath = "E:/Jobs/Tini-ChildVac/";
  static final List<String> countries = Arrays.asList(new String[] {
      "Afghanistan", "Angola", "Armenia", "Azerbaijan", "Burundi", "Benin", "Burkina Faso", "Bangladesh",
      "Bolivia", "Bhutan", "Central African Republic", "C�te d'Ivoire", "Cameroon", "Democratic Republic of the Congo",
      "Republic of Congo", "Comoros", "Cuba", "Djibouti", "Eritrea", "Ethiopia", "Georgia", "Ghana", "Guinea",
      "Gambia", "Guinea-Bissau", "Guyana", "Honduras", "Haiti", "Indonesia", "India", "Kenya", "Kyrgyzstan", "Cambodia",
      "Kiribati", "Laos", "Liberia", "Sri Lanka", "Lesotho", "Moldova", "Madagascar", "Mali", "Myanmar", "Mongolia",
      "Mozambique", "Mauritania", "Malawi", "Niger", "Nigeria", "Nicaragua", "Nepal", "Pakistan", "Papua New Guinea",
      "North Korea", "Rwanda", "Sudan", "Senegal", "Solomon Islands", "Sierra Leone", "Somalia", "South Sudan",
      "S�o Tom� and Pr�ncip", "Chad", "Togo", "Tajikistan", "Timor-Leste", "Tanzania", "Uganda", "Ukraine", "Uzbekistan",
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
  String col_scheme = null;
  String o_maxval = null;
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
    int year_column = header.indexOf("year_birth");
    int value_column = header.indexOf("exp_n_antigens");
    
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
      min_val = Math.min(min_val,  Double.parseDouble(ss[value_column]));
      max_val = Math.max(max_val, Double.parseDouble(ss[value_column]));
      ss = readLine(br);
    }
    if (!o_maxval.equals("auto")) max_val=Double.parseDouble(o_maxval); 
    br.close();
    for (int i=0; i<data.length; i++) {
      data[i] = new double[1+(last_year-first_year)];
    }
    br = new BufferedReader(new FileReader(file));
    br.readLine();
    ss = readLine(br);
    while (ss!=null) {
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
    max_val = 7.0;
    min_val = 0.0;
    val_range = max_val-min_val;
  }
  
  public VaccMovie() {}
  
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
    if (args.length<7) {
      System.out.println("Usage: java -Xmx8g jobs.e7_Tini_child.vac.VaccMovie input.csv output.gif map.bin units.txt 500 true jet auto");
      System.out.println("                    500 = 500ms between frames");
      System.out.println("                    true = LOOP, false = DON'T LOOP\n");
      System.out.println("                    jet  = colour scheme. (blue, jet, purple)");
      System.out.println("                    auto = auto max val. Otherwise specify (eg 50000)");
      
      System.out.println("Input CSV must contain header, and columns");
      System.out.println(" (necessary)    country, year_birth, exp_n_antigens");
      
      System.exit(0);
    }
    
    VaccMovie VM = new VaccMovie();
    VM.csvfile = args[0];
    VM.outgif = args[1];
    VM.mapfile = args[2];
    VM.unitfile = args[3];
    VM.frame_pause = Integer.parseInt(args[4]);
    VM.loop = Boolean.parseBoolean(args[5]);
    VM.col_scheme = args[6];
    VM.o_maxval = args[7];
    
    VM.run();
    System.exit(0);
  }
  
  public void initCols() {
    if (col_scheme.toUpperCase().equals("BLUE")) initCols_Linear(0,0,32,128,128,255);
    else if (col_scheme.toUpperCase().equals("JET")) initCols_Jet();
    else if (col_scheme.toUpperCase().equals("PURPLE")) initCols_Linear(32,0,64,192,0,255);
    else if (col_scheme.toUpperCase().equals("YF")) initCols_YF();
  }
  
  public void initCols_YF() {
    double frac;
    int[] r = new int[] {254,249,228,198,162,120,77,47,23,0,0};
    int[] g = new int[] {254,253,244,231,215,198,176,147,121,97,69};
    int[] b = new int[] {228,195,171,154,137,121,99,77,62,52,41};
    for (int i=0; i<grads; i++) {
      frac = (double)i/grads;
      int i1 = (int) (frac*10);
      double mfrac = 10*(frac-(i1/10.0));
      cols[i]=new Color((int)(r[i1]+((r[i1+1]-r[i1])*mfrac)),(int)(g[i1]+((g[i1+1]-g[i1])*mfrac)),(int)(b[i1]+((b[i1+1]-b[i1])*mfrac)));
    }
  }


  
  public void initCols_Linear(int r1,int g1,int b1,int r2,int g2,int b2) {
    double frac;
    for (int i=0; i<grads; i++) {
      frac = (double)i/grads;
      cols[i]=new Color((int)(r1+((r2-r1)*frac)),(int)(g1+((g2-g1)*frac)),(int)(b1+((b2-b1)*frac)));
    }
  }

  
  public void initCols_Jet() {
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
   
    if (!new File(gadmPath+"gadm36_ZWE_2.shp").exists()) GRT.downloadShapeFiles(gadmPath, "3.6");
    GRT.loadPolygonFolder(gadmPath, 0, "3.6");
    
    if (!new File(outPath+"map.bin").exists()) {
      GRT.makeMap();
      GRT.saveMapFile(outPath+"map.bin");
      GRT.saveUnits(outPath+"units.txt");
    } else {
      GRT.loadUnits(outPath+"units.txt");
      GRT.loadMapFile(outPath+"map.bin");
    }
    initCols();
    loadData(csvfile);
    GRT.loadUnits(unitfile);
    GRT_to_me = new int[GRT.unit_names.size()];
    for (int i=0; i<GRT.unit_names.size(); i++) GRT_to_me[i]=-1;
    
    for (int i=0; i<me_to_GRT.length; i++) {
      int index=-1;
      for (int j=0; j<GRT.unit_names.size(); j++) {
        if (GRT.unit_names.get(j).startsWith(countries.get(i))) {
          index = j;
          break;
        }
      }
      if (index==-1) {
        System.out.println("Can't find "+countries.get(i));
      }
      me_to_GRT[i] = GRT.unit_names.indexOf(countries.get(i));
      if (index==-1) System.out.println("Can't find "+countries.get(i));
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
     
    for (int y=0; y<=504; y++) {
      g.setColor(cols[(int)(254.0*(1.0-(y/504.0)))]);
      g.drawLine(75,450+y,90,450+y);
    }
    f = new Font("Calibri", Font.PLAIN, 32);
    g.setFont(f);
    g.setColor(Color.black);
    g.drawLine(92, 450, 93, 950);
    g.drawLine(93, 450, 92, 950);
    for (int y = 0; y <= 504; y += 72) {
      g.drawLine(95, y + 450, 105,y + 450);
      double v = min_val+((1.0-(y / 504.0))*val_range);
      float iv = (float) (Math.round(v*100)/100.0);
      String ivs = String.valueOf((int)iv);
      //if (ivs.indexOf(".")==ivs.length()-2) ivs+="0";
      g.drawString(ivs, 120, y + 462);

    }
    g.setFont(new Font("Calibri", Font.PLAIN, 48));
    g.drawString("Expected number", 220, 750);
    g.drawString("of vaccines",  220, 825);
    g.drawString("per child", 220, 900);
    
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
            int colr = (int)Math.round(254.0*(val - min_val)/val_range);
            if (colr<0) colr=0;
            if (colr>=cols.length) colr=cols.length-1;
            int col = cols[colr].getRGB();
            png.setRGB(i,j,col);
          }
        }
      }
      ImageIO.write(png, "PNG",  new File("out"+year+".png"));
      writer.writeToSequence(png);
    }
    writer.close();
    output.close();
  }
}

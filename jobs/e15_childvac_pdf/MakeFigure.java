package jobs.e15_childvac_pdf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.pdf.PDFGraphics2D;

import com.mrc.GlobalRasterTools.GlobalRasterTools;
import com.mrc.GlobalRasterTools.GlobalRasterTools.DPolygon;

public class MakeFigure {
  
  /* List of GADM strings to identify the countries we're interested in */
  static final String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  static final String outPath = "E:/Jobs/Tini-ChildVac/";
  static final String wd = "C:/Files/Dev/Eclipse/shape_raster_tools/jobs/e15_childvac_pdf/";
  
  static final List<String> countries = Arrays.asList(new String[] {
      "Afghanistan", "Angola", "Armenia", "Azerbaijan", "Burundi", "Benin", "Burkina Faso", "Bangladesh",
      "Bolivia", "Bhutan", "Central African Republic", "Côte d'Ivoire", "Cameroon", "Democratic Republic of the Congo",
      "Republic of Congo", "Comoros", "Cuba", "Djibouti", "Eritrea", "Ethiopia", "Georgia", "Ghana", "Guinea",
      "Gambia", "Guinea-Bissau", "Guyana", "Honduras", "Haiti", "Indonesia", "India", "Kenya", "Kyrgyzstan", "Cambodia",
      "Kiribati", "Laos", "Liberia", "Sri Lanka", "Lesotho", "Moldova", "Madagascar", "Mali", "Myanmar", "Mongolia",
      "Mozambique", "Mauritania", "Malawi", "Niger", "Nigeria", "Nicaragua", "Nepal", "Pakistan", "Papua New Guinea",
      "North Korea", "Rwanda", "Sudan", "Senegal", "Solomon Islands", "Sierra Leone", "Somalia", "South Sudan",
      "São Tomé and Príncipe", "Chad", "Togo", "Tajikistan", "Timor-Leste", "Tanzania", "Uganda", "Ukraine", "Uzbekistan",
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
  
  static final List<String> disease = Arrays.asList(new String[] {
      "HepB", "Hib", "HPV", "JE", "Measles", "MenA", "PCV", "Rota", "Rubella", "YF"
  });
  
  Color[] disease_cols = new Color[] {
      new Color(155,2,65), new Color(211,62,79), new Color(243,108,67), new Color(251,173,96),
      new Color(253,222,137), new Color(232,232,150), new Color(169,218,169), new Color(102,193,164),
      new Color(49,135,190), new Color(93,78,161)};
  
  GlobalRasterTools GRT = new GlobalRasterTools();
  
  double[][] n_antigen_data = new double[98][19];
  double[] deaths_data = new double[98];
  double[] dalys_data = new double[98];
  double[][][] deaths_disease_data = new double[2][10][31];
  double[][][] dalys_disease_data = new double[2][10][31];
  
  public void drawStack(VectorGraphics g, double[][] dat, int x, int y, double max, String title, int size, int margin) {
    g.setFont(new Font("Calibri", Font.PLAIN, 850));
    g.setColor(Color.BLACK);
    g.drawLine(x, y, x, y+size);
    g.drawLine(x, y+size, x+size, y+size);
    int space = size-(2*margin);
    int xsize = (int) (space/31);
    for (int i=0; i<31; i++) {
      int ypos = 0;
      for (int j=disease.size()-1; j>=0; j--) {
        int hei = (int) ((dat[j][i]/max)*space);
        g.setColor(disease_cols[j]);
        g.fillRect(x+margin+(i*xsize), ((y+size)-(margin+ypos))-hei, (int) (0.7*xsize), hei);
        ypos = ypos + hei;
      }
      if (i % 10 == 0) {
        g.setColor(Color.BLACK);
        g.setLineWidth(32);
        g.drawLine(x+margin+(i*xsize)+(0.35*xsize), y+size,x+margin+(i*xsize)+(0.35*xsize), y+size+200);
        g.drawString(String.valueOf(2000+i), x+margin+(i*xsize)-700, y+size+900);
      }
    }
    FontMetrics fm = g.getFontMetrics();
    g.drawString(title, x+margin+(size/2)-(fm.stringWidth(title)/2), y+size+2000);
    
  }
  
  public void drawKey(VectorGraphics g, int x, int y) {
    g.setFont(new Font("Calibri", Font.PLAIN, 700));
    for (int d=0; d<10; d++) {
      g.setColor(disease_cols[d]);
      g.fillRect(x,  y + 1000 + (d * 850), 700, 700);
      g.setColor(Color.BLACK);
      g.drawString(disease.get(d), x + 900, y + 1570 + (d * 850));
    }
    g.setFont(new Font("Calibri", Font.PLAIN, 1000));
  }
  
  public void drawMapKey(VectorGraphics g, int x, int y, Color[] col, String[] labs, int xsize) {
    g.setFont(new Font("Calibri", Font.PLAIN, 850));
    FontMetrics fm = g.getFontMetrics();
    g.setColor(Color.BLACK);
    g.fillRect(x,y,511*xsize,25);
    for (int i=0; i<512; i++) {
      g.setColor(col[i]);
      g.fillRect(x+(i*xsize),y-620,20,400);
      if ((i%64 == 0) || (i==511)) {
        g.setColor(Color.BLACK);
        if ((i%128 == 0) || (i==511)) g.fillRect(x+(i*xsize), y,20,300);
        else g.fillRect(x+(i*xsize), y,20,150);
        
        if ((i%128 == 0) || (i==511)) g.drawString(labs[(int)(Math.round(i/64.0))], x+(i*xsize)-(fm.stringWidth(labs[(int)(Math.round(i/64.0))])/2), y+1000);
      }
    }
  }
  
  public void loadData() throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(wd+"number_of_antigens.csv"));
    String s = br.readLine(); // header;
    s = br.readLine();
    while (s!=null) {
      s = s.replaceAll("\"", "");
      String[] bits = s.split(",");
      if (codes.indexOf(bits[1])==-1) {
        System.out.println("Error finding country "+bits[1]);
      }
      n_antigen_data[codes.indexOf(bits[1])][Integer.parseInt(bits[2])-2000] = Double.parseDouble(bits[3]);
      s = br.readLine();
    }
    br.close();
    
    br = new BufferedReader(new FileReader(wd+"Figure_3_deaths_averted_proportion_map.csv"));
    s = br.readLine(); // header;
    s = br.readLine();
    while (s!=null) {
      s = s.replaceAll("\"", "");
      String[] bits = s.split(",");
      deaths_data[codes.indexOf(bits[1])] = Double.parseDouble(bits[2]);
      s = br.readLine();
    }
    br.close();
    
    br = new BufferedReader(new FileReader(wd+"Figure_3_dalys_averted_proportion_map.csv"));
    s = br.readLine(); // header;
    s = br.readLine();
    while (s!=null) {
      s = s.replaceAll("\"", "");
      String[] bits = s.split(",");
      dalys_data[codes.indexOf(bits[1])] = Double.parseDouble(bits[2]);
      s = br.readLine();
    }
    br.close();
    
    br = new BufferedReader(new FileReader(wd+"Figure_3_deaths_averted_allage.csv"));
    s = br.readLine(); // header;
    s = br.readLine();
    while (s!=null) {
      s = s.replaceAll("\"", "");
      String[] bits = s.split(",");
      deaths_disease_data[bits[5].equals("A")?0:1][disease.indexOf(bits[0])]
                         [Integer.parseInt(bits[1])-2000]
                          = Double.parseDouble(bits[3])/1000000;
      s = br.readLine();
    }
    br.close();
    
    br = new BufferedReader(new FileReader(wd+"Figure_3_dalys_averted_allage.csv"));
    s = br.readLine(); // header;
    s = br.readLine();
    while (s!=null) {
      s = s.replaceAll("\"", "");
      String[] bits = s.split(",");
      dalys_disease_data[bits[5].equals("A")?0:1][disease.indexOf(bits[0])]
                         [Integer.parseInt(bits[1])-2000]
                          = Double.parseDouble(bits[3])/1000000;
      s = br.readLine();
    }
    br.close();
        
  }
  
  public MakeFigure() {}
  
  public static void main(String[] args) throws Exception {
    MakeFigure MF = new MakeFigure();
    MF.run();
    System.exit(0);
  }
  
  public void drawSideways(VectorGraphics p, String s, int x, int y, int fsize) {
    Graphics2D g2 = (Graphics2D) p;
    Font font = new Font("Calibri", Font.PLAIN, fsize);
    AffineTransform affineTransform = new AffineTransform();
    affineTransform.rotate(Math.toRadians(-90), 0, 0);
    Font rotatedFont = font.deriveFont(affineTransform);
    g2.setFont(rotatedFont);
    g2.drawString(s, x, y);
    g2.dispose();
  }
  
  private void drawArrowLine(Graphics2D g2d, int x, int y, int x2, int y2) {
    g2d.drawLine(x, y, x2, y2);
    Polygon triangle = new Polygon();
    triangle.addPoint(0, 150);
    triangle.addPoint(-150, -150);
    triangle.addPoint(150, -150);
    AffineTransform affineTransform = new AffineTransform();
    double angle = Math.atan2(y2 - y, x2 - x);
    affineTransform.translate(x2, y2);
    affineTransform.rotate((angle-Math.PI/2d));  
    Graphics2D g = (Graphics2D) g2d.create();
    g.setTransform(affineTransform);   
    g.fill(triangle);
    g.dispose();
  }
  
  public void run() throws Exception {
    Color[] col = new Color[512];
    for (int i=0; i<256; i++) {
      col[i] = new Color(255,i,0);
      col[i+256] = new Color(255-i, 255-i, i);
    }
    
    loadData();
    if (!new File(gadmPath+"gadm36_ZWE_2.shp").exists()) GRT.downloadShapeFiles(gadmPath, "3.6");
    int res = 90;
    int p23_map_space = 2500;
    GRT.setIntScale(res);
    GRT.loadPolygonFolder(gadmPath, 0, "3.6");
    
    Properties p = new Properties();
    p.setProperty("PageSize","A4");
    VectorGraphics pdf = null, pdf2 = null, pdf3 = null;
    int spacing = 104*res;
    //pdf = new EMFGraphics2D(new File(wd+"figure.emf"), new Dimension(322*res,650*res));
    //pdf2 = new EMFGraphics2D(new File(wd+"figure2.emf"), new Dimension(322*res, 280*res));
    //pdf3 = new EMFGraphics2D(new File(wd+"figure3.emf"), new Dimension(322*res, 280*res));
    
    pdf = new PDFGraphics2D(new File(wd+"figure1.pdf"), new Dimension(38640, 77280));
    pdf2 = new PDFGraphics2D(new File(wd+"figure3.pdf"), new Dimension(38640, 77280));
    pdf3 = new PDFGraphics2D(new File(wd+"figureS2.pdf"), new Dimension(38640, 77280));
    
    pdf.setProperties(p);
    pdf2.setProperties(p);
    pdf3.setProperties(p); 
    
    pdf.startExport();
    pdf2.startExport();
    pdf3.startExport();
    pdf.setColor(Color.BLACK);
    
    // Shrink the Atlantic ocean.
    
    for (int i=0; i<GRT.unit_shapes.size(); i++) {
      ArrayList<DPolygon> dps = GRT.unit_shapes.get(i);
      for (int j=0; j<dps.size(); j++) {
        DPolygon dp = dps.get(j);
        for (int k=0; k<dp.npoints; k++) {
          if (dp.xpoints[k]>(-18*res)) dp.xpoints[k]-=(18*res);
        }
      }
    }
    
    // That was surprisingly easy...
    
    for (int i=0; i<GRT.unit_shapes.size(); i++) {
      String country = GRT.unit_names.get(i);
      if (!(country.equals("Antarctica") || country.equals("Greenland") || country.equals("Canada") || country.equals("Russia"))) {
        int index = countries.indexOf(GRT.unit_names.get(i));
        ArrayList<DPolygon> dps = GRT.unit_shapes.get(i);
        System.out.println(country);
        
        for (int j=0; j<dps.size(); j++) {
           
          DPolygon dp = dps.get(j);
          Polygon simp = new Polygon();
          
          simp.addPoint(dp.xpoints[0], dp.ypoints[0]);
          int m=0;
          for (int k=1; k<dp.npoints; k++) {
            if ((simp.xpoints[m]!=dp.xpoints[k]) && (simp.ypoints[m]!=dp.ypoints[k])) {
              simp.addPoint(dp.xpoints[k], dp.ypoints[k]);
              m++;
            }
          }
          
          if (simp.npoints>100) { // Filter tiny islands
            for (int k=0; k<simp.npoints; k++) {
              simp.xpoints[k]=simp.xpoints[k]+(180*res);
              simp.ypoints[k]=(180*res)-(simp.ypoints[k]+(90*res));
            }
            
            Rectangle bounds = simp.getBounds();
            if ((bounds.y+bounds.height>(35*res)) && (bounds.y<(127*res)) && (bounds.x+bounds.width>(53*res)) && (bounds.x<335*res)) {
              for (int k=0; k<simp.npoints; k++) {
                simp.xpoints[k]=simp.xpoints[k]-(52*res);
              }
              if (index!=-1) {
                for (int k=0; k<3; k++) {
                  int y = (k==0)?2000:(k==1)?2008:(k==2)?2018:0;
                  double val = n_antigen_data[index][y-2000];
                  int c = (int) (511.0 * (val/8.0));
                  pdf.setColor(col[c]);
                  pdf.fillPolygon(simp);
                  pdf.setColor(Color.BLACK);
                  pdf.setLineWidth(4);
                  pdf.drawPolygon(simp);
                  
                  // Maps for second/third figures
                  
                  if (k==1) {
                    for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+p23_map_space;
                    val = deaths_data[index];
                    c = (int) (511.0 * ((val-0.1)/0.8));
                    pdf2.setColor(col[c]);;
                    pdf2.fillPolygon(simp);
                    pdf2.setColor(Color.BLACK);
                    pdf2.setLineWidth(4);
                    pdf2.drawPolygon(simp);
                    val = dalys_data[index];
                    c = (int) (511.0 * ((val-0.1)/0.8));
                    pdf3.setColor(col[c]);;
                    pdf3.fillPolygon(simp);
                    pdf3.setColor(Color.BLACK);
                    pdf3.setLineWidth(4);
                    pdf3.drawPolygon(simp);
                    for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]-p23_map_space;
                    

                  }
                  for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+spacing;
                }
              } else {
                for (int k=0; k<3; k++) {
                  pdf.setColor(Color.BLACK);
                  pdf.setLineWidth(4);
                  pdf.drawPolygon(simp);
                  if (k==1) {
                    for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+p23_map_space;
                    pdf2.setColor(Color.BLACK);
                    pdf2.setLineWidth(4);
                    pdf2.drawPolygon(simp);
                    pdf3.setColor(Color.BLACK);
                    pdf3.setLineWidth(4);
                    pdf3.drawPolygon(simp);
                    for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]-p23_map_space;
                  }
                  for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+spacing;
                }
              }
            }
              
          }
        }
      } else {
        System.out.println("Skipped "+country);
      }
    }
    
    pdf.setFont(new Font("Calibri", Font.PLAIN, 1100));
    pdf.setColor(Color.BLACK);
    pdf.drawString("2000", 0, 96*res);
    pdf.drawString("2010", 0, (96*res)+spacing);
    pdf.drawString("2018", 0, (96*res)+spacing+spacing);
    pdf.drawString("A", 0, 34*res);
    pdf.drawString("B", 0, (34*res)+spacing);
    pdf.drawString("C", 0, (34*res)+spacing+spacing);
    pdf.drawString("D", 0, (34*res)+spacing+spacing+spacing);
    pdf.setFont(new Font("Calibri", Font.PLAIN, 850));
    
    int top_colmap = (25*res)+spacing+spacing+spacing;
    int left_col_map = 155*res; 
    
    drawMapKey(pdf, left_col_map, top_colmap, col, new String[] {"0", "", "2", "", "4", "", "6", "", "8"}, 11);
    
    // D - the graph
    
    Color[] vcols = new Color[] {Color.BLACK, new Color(0,100,0), new Color(82,23,136), new Color(255,163,9),
                                 new Color(255,60,60), new Color(187,187,187), new Color(212,114,44), new Color(177,253,61),
                                 Color.BLUE, new Color(135,205,234)};
    
    double[][] cov = new double[10][19];
    for (int i=0; i<10; i++) for (int j=0; j<19; j++) cov[i][j]=Double.NEGATIVE_INFINITY;
    
    BufferedReader br = new BufferedReader(new FileReader(wd+"combined_coverage_data_per_disease.csv"));
    br.readLine();
    String s = br.readLine();
    
    while (s!=null) {
      s = s.replaceAll("\"", "");
      String[] bits = s.split(",");
      int j=disease.indexOf(bits[2]);
      if (!bits[3].equals("NA")) 
        cov[j][Integer.parseInt(bits[0])-2000] = Double.parseDouble(bits[3]);
      s = br.readLine();
    }
    br.close();
    
    pdf.setLineWidth(5);
    int top = (44*res)+(3*spacing);
    int bottom = (124*res)+(3*spacing);
    int left = 44*res;
    int right = 233 * res;
    pdf.drawLine(left,top,left,bottom);
    pdf.drawLine(left,bottom,right-500,bottom);
    
    // x-axis
    
    int graph_width = (right-left)-400;
    int step = (graph_width-400)/19;
    for (int i=0; i<19; i++) {
      
      if (i%5 == 0) {
        pdf.drawString(String.valueOf(2000+i), left-500+(i*step), bottom+1400);
        pdf.drawLine(left+500+(i*step), bottom,left+500+(i*step), bottom+350);
      } else {
        pdf.drawLine(left+500+(i*step), bottom,left+500+(i*step), bottom+150);  
      }
    }
    
    int graph_height = (bottom-top)-500;
    int stepy = (graph_height-500)/4;
    String[] ylabs = new String[] {"0", "25", "50", "75", "100"};    
    for (int i=0; i<5; i++) {
      pdf.drawLine(left,  (bottom-500) - (stepy*i), left-200, (bottom-500)-(stepy * i));
      FontMetrics fm = pdf.getFontMetrics();
      pdf.drawString(ylabs[i], (left-500)-fm.stringWidth(ylabs[i]), (bottom-200) - (stepy*i));
    }
    
    int sx,sy;
    pdf.setLineWidth(128);
    
    for (int i=0; i<disease.size(); i++) {
      pdf.setColor(vcols[i]);
      
      sx = -1;
      sy = -1;
      for (int j=0; j<19; j++) {
        if (cov[i][j]>=0) {
          int sx2 = left+500+(j*step);
          int sy2 = (int) Math.round((bottom-500)-((4*stepy)*cov[i][j]));
          if (sx>=0) pdf.drawLine(sx, sy, sx2, sy2);
          sx = sx2;
          sy = sy2;
        }
      }
    }
    
    // Do key in sorted order...
    
    pdf.setFont(new Font("Calibri", Font.PLAIN, 700));
    boolean[] done = new boolean[cov.length];
    for (int i=0; i<done.length; i++) done[i]=false;
    
    for (int i=0; i<cov.length; i++) {
      int best=0;
      while (done[best]) best++;
      for (int j=best+1; j<10; j++)
        if ((!done[j]) && (cov[j][18]>cov[best][18])) best=j;
      
      done[best]=true;
      pdf.setColor(Color.black);
      pdf.drawString(disease.get(best),  right+2000,  top + 500 + (800 * i));
      pdf.setColor(vcols[best]);
      pdf.setLineWidth(96);
      drawArrowLine(pdf, right+1700, top+250 + (800*i), right-400, (int) Math.round((bottom-500)-((4*stepy)*cov[best][18])));
      
    }
    
    
    pdf.setFont(new Font("Calibri", Font.PLAIN, 850));
    pdf.setColor(Color.BLACK);
    //pdf.drawString("Year",left+(96*res), bottom+2750);
    drawSideways(pdf, "Coverage (%)", left-2200, bottom-1500, 850);
        
    // Now the stacks for page 2 and 3...
    int stack_size = 10000;
    int stack_margin = 500;
    drawStack(pdf2, deaths_disease_data[0], 2500, 1500, 5.0, "Calendar year", stack_size, stack_margin);
    drawStack(pdf2, deaths_disease_data[1], 3500 + stack_size, 1500, 5.0, "Year of birth", stack_size, stack_margin);
    drawKey(pdf2, 24000, 1500);
    drawStack(pdf3, dalys_disease_data[0], 2500, 1500, 300.0, "Calendar year", stack_size, stack_margin);
    drawStack(pdf3, dalys_disease_data[1], 3500 + stack_size, 1500, 300.0, "Year of birth", stack_size, stack_margin);
    drawKey(pdf3, 24000, 1500);
    
    pdf2.setColor(Color.BLACK);
    pdf2.setFont(new Font("Calibri", Font.PLAIN, 850));
    pdf2.setLineWidth(64);
    bottom = (1500 + stack_size) - stack_margin;
    int data_space = stack_size - (2 * stack_margin);
    for (int y=0; y<=5; y++) {
      pdf2.drawLine(2500,bottom-(y*(data_space/5)), 2300, bottom-(y*(data_space/5)));
      pdf2.drawString(String.valueOf(y), 1600, 200+bottom-(y*(data_space/5)));
    }
    drawSideways(pdf2, "Deaths averted (millions)", 800, 10500, 850);
    
    pdf3.setColor(Color.BLACK);
    pdf3.setFont(new Font("Calibri", Font.PLAIN, 750));
    pdf3.setLineWidth(64);
    FontMetrics f = pdf3.getFontMetrics();
    for (int y=0; y<=300; y+=50) {
      int xleft = f.stringWidth(String.valueOf(y));
      pdf3.drawLine(2500,bottom-(y*(data_space/300)), 2300, bottom-(y*(data_space/300)));
      pdf3.drawString(String.valueOf(y), 2100-xleft, 200+bottom-(y*(data_space/300)));
    }
    drawSideways(pdf3, "Dalys averted (millions)", 500, 10500, 850);
    pdf2.setFont(new Font("Calibri", Font.PLAIN, 1100));
    pdf3.setFont(new Font("Calibri", Font.PLAIN, 1100));
    pdf2.drawString("A", 3000,1000);
    pdf3.drawString("A", 3000,1000);
    pdf2.drawString("B", 14000,1000);
    pdf3.drawString("B", 14000,1000);
    pdf2.drawString("C", 0, 15000);
    pdf3.drawString("C", 0, 15000);
    drawMapKey(pdf2, 12000, 24000, col, new String[] {"0.1", "", "0.3", "", "0.5", "", "0.7", "", "0.9"}, 17);    
    drawMapKey(pdf3, 12000, 24000, col, new String[] {"0.1", "", "0.3", "", "0.5", "", "0.7", "", "0.9"}, 17);
    pdf.endExport();
    pdf2.endExport();
    pdf3.endExport();
    
   
  }
  
 }

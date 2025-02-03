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
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.freehep.graphicsio.pdf.PDFGraphics2D;

import com.mrc.GlobalRasterTools.CSVFile;
import com.mrc.GlobalRasterTools.GlobalRasterTools;
import com.mrc.GlobalRasterTools.GlobalRasterTools.DPolygon;

public class MakeFigure {
  
  /* List of GADM strings to identify the countries we're interested in */
  static final String gadmPath = "E:/Data/Boundaries/GADM3_6/";
  static final String wd = "C:/Files/Dev/Eclipse/shape_raster_tools/jobs/e15_childvac_pdf/";
  
  static final List<String> countries = Arrays.asList(new String[] {
      "Afghanistan", "Angola", "Armenia", "Azerbaijan", "Burundi", "Benin", "Burkina Faso", "Bangladesh",
      "Bolivia", "Bhutan", "Central African Republic", "Côte d'Ivoire", "Cameroon", "Democratic Republic of the Congo","Republic of Congo", "Comoros", 
      "Cuba", "Djibouti", "Eritrea", "Ethiopia", "Georgia", "Ghana", "Guinea", "Gambia", 
      "Guinea-Bissau", "Guyana", "Honduras", "Haiti", "Indonesia", "India", "Kenya", "Kyrgyzstan", 
      "Cambodia", "Kiribati", "Laos", "Liberia", "Sri Lanka", "Lesotho", "Moldova", "Madagascar", 
      "Mali", "Myanmar", "Mongolia", "Mozambique", "Mauritania", "Malawi", "Niger", "Nigeria", 
      "Nicaragua", "Nepal", "Pakistan", "Papua New Guinea","North Korea", "Rwanda", "Sudan", "Senegal", 
      "Solomon Islands", "Sierra Leone", "Somalia", "South Sudan", "São Tomé and Príncipe", "Chad", "Togo", "Tajikistan", 
      "Timor-Leste", "Tanzania", "Uganda", "Ukraine", "Uzbekistan", "Vietnam", "Yemen", "Zambia", 
      "Zimbabwe", "Albania", "Bosnia and Herzegovina", "Belize", "China", "Cape Verde", "Egypt", "Fiji", 
      "Micronesia", "Guatemala", "Iraq", "Morocco", "Marshall Islands", "Philippines", "Paraguay", "Palestina", 
      "El Salvador", "Swaziland", "Syria", "Turkmenistan", "Tonga", "Tunisia", "Tuvalu", "Vanuatu", 
      "Samoa", "Kosovo"});
  
  static final List<String> codes = Arrays.asList(new String[] {
      "AFG", "AGO", "ARM", "AZE", "BDI", "BEN", "BFA", "BGD", 
      "BOL", "BTN", "CAF", "CIV", "CMR", "COD", "COG", "COM", 
      "CUB", "DJI", "ERI", "ETH", "GEO", "GHA", "GIN", "GMB", 
      "GNB", "GUY", "HND", "HTI", "IDN", "IND", "KEN", "KGZ", 
      "KHM", "KIR", "LAO", "LBR", "LKA", "LSO", "MDA", "MDG", 
      "MLI", "MMR", "MNG", "MOZ", "MRT", "MWI", "NER", "NGA", 
      "NIC", "NPL", "PAK", "PNG", "PRK", "RWA", "SDN", "SEN", 
      "SLB", "SLE", "SOM", "SSD", "STP", "TCD", "TGO", "TJK", 
      "TLS", "TZA", "UGA", "UKR", "UZB", "VNM", "YEM", "ZMB", 
      "ZWE", "ALB", "BIH", "BLZ", "CHN", "CPV", "EGY", "FJI", 
      "FSM", "GTM", "IRQ", "MAR", "MHL", "PHL", "PRY", "PSE", 
      "SLV", "SWZ", "SYR", "TKM", "TON", "TUN", "TUV", "VUT", 
      "WSM", "XK"});
  
  static final List<String> all_disease = Arrays.asList(new String[] {
      "HepB", "Hib", "HPV", "JE", "Measles", "MenA", "PCV", "Rota", "Rubella", "YF"
  });
  
  static final List<String> u5_disease = Arrays.asList(new String[] {
      "HepB", "Hib", "JE", "Measles", "MenA", "PCV", "Rota", "YF"
  });
  
  Color[] all_disease_cols = new Color[] {
      new Color(155, 2,   65),  new Color(211, 62,  79),  new Color(243, 108, 67),  new Color(251, 173, 96),
      new Color(253, 222, 137), new Color(232, 232, 150), new Color(169, 218, 169), new Color(102, 193, 164),
      new Color(49,  135, 190), new Color(93,  78,  161)};
  
  Color[] u5_disease_cols = new Color[] {
      new Color(213,61,77), new Color(243,108,66), new Color(251,172,95), new Color(253,223,138),
      new Color(226,230,149), new Color(170,220,163), new Color(101,193,163), new Color(49,135,188)};
  
  GlobalRasterTools GRT = new GlobalRasterTools();
  
  double[][] n_antigen_data = new double[98][19];
  double[] deaths_data = new double[98];
  double[] dalys_data = new double[98];
  double[][][] deaths_disease_data = new double[2][10][31];
  double[][][] dalys_disease_data = new double[2][10][31];
  double[][][] u5_deaths_disease_data = new double[2][8][31];
  double[][][] u5_dalys_disease_data = new double[2][8][31];
  
  public void drawStack(VectorGraphics g, double[][] dat, int x, int y, double max, String title, int size, int margin, Color[] dcols, List<String> dnames) {
    g.setFont(new Font("Calibri", Font.PLAIN, 850));
    g.setColor(Color.BLACK);
    g.drawLine(x, y, x, y + size);
    g.drawLine(x, y + size, x + size, y + size);
    int space = size - (2 * margin);
    int xsize = (int) (space / 31);
    
    for (int i = 0; i < 31; i++) {
      int ypos = 0;
      for (int j = dnames.size()-1; j >= 0; j--) {
        int hei = (int) ((dat[j][i] / max) * space);
        g.setColor(dcols[j]);
        g.fillRect(x + margin + (i * xsize), 
                   ((y + size)-(margin + ypos))-hei, 
                   (int) (0.7 * xsize), 
                   hei);
        ypos += hei;
      }
    
      if (i % 10 == 0) {
        g.setColor(Color.BLACK);
        g.setLineWidth(32);
        g.drawLine(x + margin + (i * xsize) + (0.35 * xsize), 
                   y + size, 
                   x + margin + (i * xsize) + (0.35 * xsize), 
                   y + size + 200);
        g.drawString(String.valueOf(2000 + i), x + margin + (i * xsize) - 700, y + size + 900);
      }
    }
    FontMetrics fm = g.getFontMetrics();
    g.drawString(title, x + margin + (size / 2)-(fm.stringWidth(title) / 2), y + size + 2000);
  }
  
  public void drawKey(VectorGraphics g, int x, int y, Color[] dcols, List<String> dnames) {
    g.setFont(new Font("Calibri", Font.PLAIN, 700));
    for (int d = 0; d < dcols.length; d++) {
      g.setColor(dcols[d]);
      g.fillRect(x,  y + 1000 + (d * 850), 700, 700);
      g.setColor(Color.BLACK);
      g.drawString(dnames.get(d), x + 900, y + 1570 + (d * 850));
    }
    g.setFont(new Font("Calibri", Font.PLAIN, 1000));
  }
  
  public void drawMapKey(VectorGraphics g, int x, int y, Color[] col, String[] labs, int xsize) {
    g.setFont(new Font("Calibri", Font.PLAIN, 850));
    FontMetrics fm = g.getFontMetrics();
    g.setColor(Color.BLACK);
    g.fillRect(x, y, 511 * xsize, 25);
    for (int i = 0; i < 512; i++) {
      g.setColor(col[i]);
      g.fillRect(x + (i * xsize), y - 620, 20, 400);
      if ((i % 64 == 0) || (i == 511)) {
        g.setColor(Color.BLACK);
        if ((i % 128 == 0) || (i == 511)) g.fillRect(x + (i * xsize), y, 20, 300);
        else g.fillRect( x + (i * xsize), y, 20, 150);
        
        if ((i % 128 == 0) || (i == 511)) {
          g.drawString(labs[(int)(Math.round(i / 64.0))], 
                       x + (i * xsize) - (fm.stringWidth(labs[(int)(Math.round(i / 64.0))]) / 2), 
                       y + 1000);
        }
      }
    }
  }
  
  public void loadData() throws Exception {
    CSVFile csv = CSVFile.read(wd + "number_of_antigens.csv", true);
    for (int i = 0; i < csv.getRowCount(); i++)
      n_antigen_data[codes.indexOf(csv.getCol("country").get(i))]
                    [Integer.parseInt(csv.getCol("year_birth").get(i))-2000] =
                      Double.parseDouble(csv.getCol("exp_n_antigens").get(i));
    
    csv = CSVFile.read(wd + "Figure_3(c)_deaths_averted_proportion_map.csv", true);
    for (int i = 0; i < csv.getRowCount(); i++)
      deaths_data[codes.indexOf(csv.getCol("country").get(i))] = 
                   Double.parseDouble(csv.getCol("impact_proportion").get(i));
    
    
    csv = CSVFile.read(wd + "Figure_S2(c)_dalys_averted_proportion_map.csv", true);
    for (int i = 0; i < csv.getRowCount(); i++)
      dalys_data[codes.indexOf(csv.getCol("country").get(i))] = 
                  Double.parseDouble(csv.getCol("impact_proportion").get(i));

    csv = CSVFile.read(wd + "Figure_3(a,b)_deaths_averted.csv", true);
    for (int i = 0; i < csv.getRowCount(); i++)
      deaths_disease_data[csv.getCol("view").get(i).equals("A") ? 0 : 1]
                         [all_disease.indexOf(csv.getCol("disease").get(i))]
                         [Integer.parseInt(csv.getCol("year").get(i)) - 2000] = 
                           Double.parseDouble(csv.getCol("value").get(i)) / 1000000.0;
    
    csv = CSVFile.read(wd + "Figure_S2(a,b)_dalys_averted_allage.csv", true);
    for (int i = 0; i < csv.getRowCount(); i++)
      dalys_disease_data[csv.getCol("view").get(i).equals("A") ? 0 : 1]
                        [all_disease.indexOf(csv.getCol("disease").get(i))]
                        [Integer.parseInt(csv.getCol("year").get(i)) - 2000] = 
                          Double.parseDouble(csv.getCol("value").get(i)) / 1000000.0;
    
    csv = CSVFile.read(wd + "Figure_S3(a)_deaths_averted_under5.csv", true);
    for (int i = 0; i < csv.getRowCount(); i++)
      u5_deaths_disease_data[csv.getCol("view").get(i).equals("A") ? 0 : 1]
                            [u5_disease.indexOf(csv.getCol("disease").get(i))]
                            [Integer.parseInt(csv.getCol("year").get(i)) - 2000] = 
                              Double.parseDouble(csv.getCol("value").get(i)) / 1000000.0;

    csv = CSVFile.read(wd + "Figure_S3(c)_dalys_averted_under5.csv", true);
    for (int i = 0; i < csv.getRowCount(); i++)
      u5_dalys_disease_data[csv.getCol("view").get(i).equals("A") ? 0 : 1]
                            [u5_disease.indexOf(csv.getCol("disease").get(i))]
                            [Integer.parseInt(csv.getCol("year").get(i)) - 2000] = 
                              Double.parseDouble(csv.getCol("value").get(i)) / 1000000.0;
        
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
    if (!new File(gadmPath+"gadm36_ZWE_0.shp").exists()) GRT.downloadShapeFiles(gadmPath, "3.6");
    int res = 90;
    int p23_map_space = 2500;
    GRT.setIntScale(res);
    GRT.loadPolygonFolder(gadmPath, 0, "3.6");
    
    Properties p = new Properties();
    p.setProperty("PageSize","A4");
    VectorGraphics vec[] = new VectorGraphics[5];
    int spacing = 104*res;
    
    for (int format=0; format<=1; format++) {
      if (format==0) {
        vec[0] = new PDFGraphics2D(new File(wd+"figure1.pdf"), new Dimension(38640, 77280));
        vec[1] = new PDFGraphics2D(new File(wd+"figure3.pdf"), new Dimension(38640, 77280));
        vec[2] = new PDFGraphics2D(new File(wd+"figureS2.pdf"), new Dimension(38640, 77280));
        vec[3] = new PDFGraphics2D(new File(wd+"figureS3a.pdf"), new Dimension(38640, 77280));
        vec[4] = new PDFGraphics2D(new File(wd+"figureS3b.pdf"), new Dimension(38640, 77280));
      } else {
        vec[0] = new EMFGraphics2D(new File(wd+"figure1.emf"), new Dimension(38640, 77280));
        vec[1] = new EMFGraphics2D(new File(wd+"figure3.emf"), new Dimension(38640, 77280));
        vec[2] = new EMFGraphics2D(new File(wd+"figureS2.emf"), new Dimension(38640, 77280));
        vec[3] = new EMFGraphics2D(new File(wd+"figureS3a.emf"), new Dimension(38640, 77280));
        vec[4] = new EMFGraphics2D(new File(wd+"figureS3b.emf"), new Dimension(38640, 77280));    
      }
    
    
      for (int i=0; i<5; i++) {
        vec[i].setProperties(p);;
        vec[i].startExport();
        
      }
    
    
      vec[0].setColor(Color.BLACK);
    
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
      // Now some filtering of countries we don't want to plot - 
    
      for (int i=0; i<GRT.unit_shapes.size(); i++) {
        String country = GRT.unit_names.get(i);
        if (!(country.equals("Antarctica") || country.equals("Greenland") || country.equals("Canada") || country.equals("Russia"))) {
          int index = countries.indexOf(GRT.unit_names.get(i));
          ArrayList<DPolygon> dps = GRT.unit_shapes.get(i);
        
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
                    vec[0].setColor(col[c]);
                    vec[0].fillPolygon(simp);
                    vec[0].setColor(Color.BLACK);
                    vec[0].setLineWidth(4);
                    vec[0].drawPolygon(simp);
                    
                    // Maps for second/third figures
                  
                    if (k==1) {
                      for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+p23_map_space;
                      val = deaths_data[index];
                      c = (int) (511.0 * ((val-0.1)/0.8));
                      for (int gr=1; gr<=2; gr++) {
                        vec[gr].setColor(col[c]);;
                        vec[gr].fillPolygon(simp);
                        vec[gr].setColor(Color.BLACK);
                        vec[gr].setLineWidth(4);
                        vec[gr].drawPolygon(simp);
                      }
                      for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]-p23_map_space;
                    }
                    for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+spacing;
                  }
                } else {
                  for (int k=0; k<3; k++) {
                    vec[0].setColor(Color.BLACK);
                    vec[0].setLineWidth(4);
                    vec[0].drawPolygon(simp);
                    if (k==1) {
                      for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+p23_map_space;
                      for (int gr=1; gr<=2; gr++) {
                        vec[gr].setColor(Color.BLACK);
                        vec[gr].setLineWidth(4);
                        vec[gr].drawPolygon(simp);
                      }
                      for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]-p23_map_space;
                    }
                    for (m=0; m<simp.npoints; m++) simp.ypoints[m]=simp.ypoints[m]+spacing;
                  }
                }
              }
            }
          }
        } 
      }
    
    
      vec[0].setFont(new Font("Calibri", Font.PLAIN, 1100));
      vec[0].setColor(Color.BLACK);
      vec[0].drawString("2000", 0, 96*res);
      vec[0].drawString("2010", 0, (96*res)+spacing);
      vec[0].drawString("2018", 0, (96*res)+spacing+spacing);
      vec[0].drawString("A", 0, 34*res);
      vec[0].drawString("B", 0, (34*res)+spacing);
      vec[0].drawString("C", 0, (34*res)+spacing+spacing);
      vec[0].drawString("D", 0, (34*res)+spacing+spacing+spacing);
      vec[0].setFont(new Font("Calibri", Font.PLAIN, 850));
    
      int top_colmap = (25*res)+spacing+spacing+spacing;
      int left_col_map = 155*res; 
    
      drawMapKey(vec[0], left_col_map, top_colmap, col, new String[] {"0", "", "2", "", "4", "", "6", "", "8"}, 11);
    
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
        int j=all_disease.indexOf(bits[2]);
        if (!bits[3].equals("NA")) 
          cov[j][Integer.parseInt(bits[0])-2000] = Double.parseDouble(bits[3]);
        s = br.readLine();
      }
      br.close();
    
      vec[0].setLineWidth(5);
      int top = (44*res)+(3*spacing);
      int bottom = (124*res)+(3*spacing);
      int left = 44*res;
      int right = 233 * res;
      vec[0].drawLine(left,top,left,bottom);
      vec[0].drawLine(left,bottom,right-500,bottom);
    
      // x-axis
    
      int graph_width = (right-left)-400;
      int step = (graph_width-400)/19;
      for (int i=0; i<19; i++) {
      
        if (i%5 == 0) {
          vec[0].drawString(String.valueOf(2000+i), left-500+(i*step), bottom+1400);
          vec[0].drawLine(left+500+(i*step), bottom,left+500+(i*step), bottom+350);
        } else {
          vec[0].drawLine(left+500+(i*step), bottom,left+500+(i*step), bottom+150);  
        }
      }
    
      int graph_height = (bottom-top)-500;
      int stepy = (graph_height-500)/4;
      String[] ylabs = new String[] {"0", "25", "50", "75", "100"};    
      for (int i=0; i<5; i++) {
        vec[0].drawLine(left,  (bottom-500) - (stepy*i), left-200, (bottom-500)-(stepy * i));
        FontMetrics fm = vec[0].getFontMetrics();
        vec[0].drawString(ylabs[i], (left-500)-fm.stringWidth(ylabs[i]), (bottom-200) - (stepy*i));
      }
    
      int sx,sy;
      vec[0].setLineWidth(128);
    
      for (int i=0; i<all_disease.size(); i++) {
        vec[0].setColor(vcols[i]);
      
        sx = -1;
        sy = -1;
        for (int j=0; j<19; j++) {
          if (cov[i][j]>=0) {
            int sx2 = left+500+(j*step);
            int sy2 = (int) Math.round((bottom-500)-((4*stepy)*cov[i][j]));
            if (sx>=0) vec[0].drawLine(sx, sy, sx2, sy2);
            sx = sx2;
            sy = sy2;
          }
        }
      }
    
      // Do key in sorted order...
    
      vec[0].setFont(new Font("Calibri", Font.PLAIN, 700));
      boolean[] done = new boolean[cov.length];
      for (int i=0; i<done.length; i++) done[i]=false;
    
      for (int i=0; i<cov.length; i++) {
        int best=0;
        while (done[best]) best++;
        for (int j=best+1; j<10; j++)
          if ((!done[j]) && (cov[j][18]>cov[best][18])) best=j;
      
        done[best]=true;
        vec[0].setColor(Color.black);
        vec[0].drawString(all_disease.get(best),  right+2000,  top + 500 + (800 * i));
        vec[0].setColor(vcols[best]);
        vec[0].setLineWidth(96);
        drawArrowLine(vec[0], right+1700, top+250 + (800*i), right-400, (int) Math.round((bottom-500)-((4*stepy)*cov[best][18])));
      
      }
    
      vec[0].setFont(new Font("Calibri", Font.PLAIN, 850));
      vec[0].setColor(Color.BLACK);
      drawSideways(vec[0], "Coverage (%)", left-2200, bottom-1500, 850);
        
      //  Now the stacks for page 2 and 3...
      
      int stack_size = 10000;
      int stack_margin = 500;
      drawStack(vec[1], deaths_disease_data[0], 2500, 1500, 5.0, "Calendar year", stack_size, stack_margin, all_disease_cols, all_disease);
      drawStack(vec[1], deaths_disease_data[1], 3500 + stack_size, 1500, 5.0, "Year of birth", stack_size, stack_margin, all_disease_cols, all_disease);
      drawKey(vec[1], 24000, 1500, all_disease_cols, all_disease);
      drawStack(vec[2], dalys_disease_data[0], 2500, 1500, 300.0, "Calendar year", stack_size, stack_margin, all_disease_cols, all_disease);
      drawStack(vec[2], dalys_disease_data[1], 3500 + stack_size, 1500, 300.0, "Year of birth", stack_size, stack_margin, all_disease_cols, all_disease);
      drawKey(vec[2], 24000, 1500, all_disease_cols, all_disease);
    
      drawStack(vec[3], u5_deaths_disease_data[0], 2500, 1500, 3.0, "Calendar year", stack_size, stack_margin, u5_disease_cols, u5_disease);
      drawStack(vec[3], u5_deaths_disease_data[1], 3500 + stack_size, 1500, 3.0, "Year of birth", stack_size, stack_margin, u5_disease_cols, u5_disease);
      drawKey(vec[3], 24000, 3200, u5_disease_cols, u5_disease);
      drawStack(vec[4], u5_dalys_disease_data[0], 2500, 1500, 200.0, "Calendar year", stack_size, stack_margin, u5_disease_cols, u5_disease);
      drawStack(vec[4], u5_dalys_disease_data[1], 3500 + stack_size, 1500, 200.0, "Year of birth", stack_size, stack_margin, u5_disease_cols, u5_disease);
      drawKey(vec[4], 24000, 3200, u5_disease_cols, u5_disease);
   
    
      vec[1].setColor(Color.BLACK);
      vec[1].setFont(new Font("Calibri", Font.PLAIN, 850));
      vec[1].setLineWidth(64);
      bottom = (1500 + stack_size) - stack_margin;
      int data_space = stack_size - (2 * stack_margin);
      for (int y=0; y<=5; y++) {
        vec[1].drawLine(2500,bottom-(y*(data_space/5)), 2300, bottom-(y*(data_space/5)));
        vec[1].drawString(String.valueOf(y), 1600, 200+bottom-(y*(data_space/5)));
      }
      drawSideways(vec[1], "Deaths averted (millions)", 800, 10500, 850);
    
      vec[3].setColor(Color.BLACK);
      vec[3].setFont(new Font("Calibri", Font.PLAIN, 850));
      vec[3].setLineWidth(64);
      String[] nums = new String[] {"0", "0.5", "1.0", "1.5", "2.0", "2.5"};
      FontMetrics f = vec[3].getFontMetrics();
      for (int y=0; y<=5; y++) {
        vec[3].drawLine(2500,bottom-(y*(data_space/5.0)), 2300, bottom-(y*(data_space/5.0)));
        vec[3].drawString(nums[y], 2000-f.stringWidth(nums[y]), 200+bottom-(y*(data_space/5.0)));
      }
      drawSideways(vec[3], "Deaths averted (millions)", 500, 10500, 850);
    
      vec[2].setColor(Color.BLACK);
      vec[2].setFont(new Font("Calibri", Font.PLAIN, 750));
      vec[2].setLineWidth(64);
      f = vec[2].getFontMetrics();
      for (int y=0; y<=300; y+=50) {
        int xleft = f.stringWidth(String.valueOf(y));
        vec[2].drawLine(2500,bottom-(y*(data_space/300)), 2300, bottom-(y*(data_space/300)));
        vec[2].drawString(String.valueOf(y), 2100-xleft, 200+bottom-(y*(data_space/300)));
      }
      drawSideways(vec[2], "DALYs averted (millions)", 500, 10500, 850);
    
      vec[4].setColor(Color.BLACK);
      vec[4].setFont(new Font("Calibri", Font.PLAIN, 750));
      vec[4].setLineWidth(64);
      f = vec[4].getFontMetrics();
      for (int y=0; y<=200; y+=50) {
        int xleft = f.stringWidth(String.valueOf(y));
        vec[4].drawLine(2500,bottom-(y*(data_space/200)), 2300, bottom-(y*(data_space/200)));
        vec[4].drawString(String.valueOf(y), 2100-xleft, 200+bottom-(y*(data_space/200)));
      }
      drawSideways(vec[4], "DALYs averted (millions)", 500, 10500, 850);
    
      for (int gr=0; gr<=4; gr++) {
        if (gr > 0) {
          vec[gr].setFont(new Font("Calibri", Font.PLAIN, 1100));
          vec[gr].drawString("A", 3000,1000);
          vec[gr].drawString("B", 14000,1000);
          if (gr < 3) {
            vec[gr].drawString("C", 0, 15000);
            drawMapKey(vec[gr], 12000, 24000, col, new String[] {"0.1", "", "0.3", "", "0.5", "", "0.7", "", "0.9"}, 17);
          }
        }
        vec[gr].endExport();
      }
    }
  }
}

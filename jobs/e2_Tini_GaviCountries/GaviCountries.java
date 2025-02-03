 package jobs.e2_Tini_GaviCountries;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

public class GaviCountries {
  
  /* List of GADM strings to identify the countries we're interested in */
  
  final List<String> gavi73_countries = Arrays.asList(new String[] {
      "Afghanistan", "Angola", "Armenia", "Azerbaijan", "Burundi", "Benin", "Burkina Faso", "Bangladesh",
      "Bolivia", "Bhutan", "Central African Republic", "Côte d'Ivoire", "Cameroon", "Democratic Republic of the Congo",
      "Republic of Congo", "Comoros", "Cuba", "Djibouti", "Eritrea", "Ethiopia", "Georgia", "Ghana", "Guinea",
      "Gambia", "Guinea-Bissau", "Guyana", "Honduras", "Haiti", "Indonesia", "India", "Kenya", "Kyrgyzstan", "Cambodia",
      "Kiribati", "Laos", "Liberia", "Sri Lanka", "Lesotho", "Moldova", "Madagascar", "Mali", "Myanmar", "Mongolia",
      "Mozambique", "Mauritania", "Malawi", "Niger", "Nigeria", "Nicaragua", "Nepal", "Pakistan", "Papua New Guinea",
      "North Korea", "Rwanda", "Sudan", "Senegal", "Solomon Islands", "Sierra Leone", "Somalia", "South Sudan",
      "São Tomé and Príncipe", "Chad", "Togo", "Tajikistan", "Timor-Leste", "Tanzania", "Uganda", "Ukraine", "Uzbekistan",
      "Vietnam", "Yemen", "Zambia", "Zimbabwe"});
  
  final List<String> gavi98_extras = Arrays.asList(new String[] {
      "Albania", "Bosnia and Herzegovina", "Belize", "China", "Cape Verde", "Egypt", "Fiji", "Micronesia", "Guatemala",
      "Iraq", "Morocco", "Marshall Islands", "Philippines", "Paraguay", "Palestina", "El Salvador", "Swaziland",
      "Syria", "Turkmenistan", "Tonga", "Tunisia", "Tuvalu", "Vanuatu", "Samoa", "Kosovo" });
  
  final List<String> gavi112_extras = Arrays.asList(new String[] {
      "Belarus", "Colombia", "Algeria", "Ecuador", "Iran", "Jamaica", "Jordan", "Macedonia", "Namibia", "Peru", "Serbia",
      "Thailand", "South Africa", "Venezuela" });
  
  static String outPath = "E:\\Jobs\\Tini-GaviCountries\\";
  static String gadmPath = "E:\\Data\\Boundaries\\GADM3_6\\";

  
  public static void main(String[] args) throws Exception {
    GaviCountries GC = new GaviCountries();
    GC.run();
  }
  
  public void run() throws Exception {

    new File(outPath).mkdirs();
    new File(gadmPath).mkdirs();
    
    GlobalRasterTools GRT = new GlobalRasterTools();
    if (!new File(gadmPath+"gadm36_ZWE_0.shp").exists()) {
      System.out.println("Downloading GADM shapes...");
      GRT.downloadShapeFiles(gadmPath,"3.6");
    }
                                       
    GRT.loadPolygonFolder(gadmPath, 0, "3.6");
    
    if (!new File(outPath+"map.bin").exists()) {
      System.out.println("Making map");
      GRT.makeMap();
      System.out.println("Saving map");
      GRT.saveMapFile(outPath+"map.bin");
      GRT.saveUnits(outPath+"units.txt");
    } else {
      GRT.loadUnits(outPath+"units.txt");
      System.out.println("Reading map");
      GRT.loadMapFile(outPath+"map.bin");
    }
    
    ArrayList<Integer> c73 = new ArrayList<Integer>();
    ArrayList<Integer> c98 = new ArrayList<Integer>();
    ArrayList<Integer> c112 = new ArrayList<Integer>();
    
    for (int i=0; i<GRT.unit_names.size(); i++) {
      if (gavi73_countries.contains(GRT.unit_names.get(i).split("\t")[0])) {
        c73.add(i);
        System.out.println(GRT.unit_names.get(i));
      }
      if (gavi98_extras.contains(GRT.unit_names.get(i).split("\t")[0])) c98.add(i);
      if (gavi112_extras.contains(GRT.unit_names.get(i).split("\t")[0])) c112.add(i);
    }
    
    System.out.println("Test length: c73 = "+c73.size()+", c98 = "+c98.size()+", c112 = "+c112.size());
    
    int white = Color.white.getRGB();
    int grey = new Color(160,160,160).getRGB();
    int black = new Color(0,0,0).getRGB();
    int cc73 = new Color(213,195,144).getRGB();
    int cc98 = new Color(230,210,172).getRGB();
    int cc112 = new Color(255,240,222).getRGB();
    
    
    int h = GRT.map.length / 10;
    int w = GRT.map[0].length / 10;
    BufferedImage png = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    for (int j=0; j<h; j++) for (int i=0; i<w; i++) png.setRGB(i, j, white);
    
    for (int j=0; j<GRT.map.length; j++) {
      for (int i=0; i<GRT.map[j].length; i++) {
        if (GRT.map[j][i]>=0) {
          if (c73.contains(GRT.map[j][i])) png.setRGB(i/10, j/10, cc73);
          else if (c98.contains(GRT.map[j][i])) png.setRGB(i/10, j/10, cc98);
          else if (c112.contains(GRT.map[j][i])) png.setRGB(i/10, j/10, cc112);
          else png.setRGB(i/10, j/10, grey);
        } 
      }
    }
    
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
          png.setRGB(i/10, j/10, black);
        }
      }
    }
    ImageIO.write(png, "PNG", new File(outPath+"tinimap.png"));
  }

}

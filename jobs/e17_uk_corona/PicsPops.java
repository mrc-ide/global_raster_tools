package jobs.e17_uk_corona;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class PicsPops {
  public static void main(String[] args) throws Exception {
    if (args.length!=2) {
      System.out.println("Two args: input pop file, and input index file");
      System.exit(1);
    }
    String popFile = args[0];
    String indexFile = args[1];
    int minlat = 49; // South
    int maxlat = 61; // North
    int minlon = -9;
    int maxlon = 2;
    int ysize = ((maxlat-minlat) + 1) * 120;
    int xsize = ((maxlon-minlon) + 1) * 120;
    int white = Color.white.getRGB();
    int black = Color.black.getRGB();
    int grey = Color.DARK_GRAY.getRGB();
    
    ArrayList<String> index = new ArrayList<String>();
    BufferedReader br = new BufferedReader(new FileReader(indexFile));
    String s = br.readLine();
    while (s!=null) {
      index.add(s);
      s=br.readLine();
    }
    br.close();
    
    ArrayList<String[]> popf = new ArrayList<String[]>();
    br = new BufferedReader(new FileReader(popFile));
    s = br.readLine();
    while (s!=null) {
      popf.add(s.split("\t"));
      s=br.readLine();
    }
    br.close();
    
    PrintWriter PW = new PrintWriter(new File("index_pops_out.txt"));
    
    for (int u=0; u<index.size(); u++) {
      String entry_s = index.get(u);
      String[] entry = entry_s.split("\t");
      long pop=0;
      BufferedImage bi = new BufferedImage(xsize, ysize, BufferedImage.TYPE_3BYTE_BGR);
      for (int y=0; y<ysize; y++) for (int x=0; x<xsize; x++) bi.setRGB(x, y, black);
      for (int i=0; i<popf.size(); i++) {
        String[] popline = popf.get(i);
        int xpix = (int) Math.round((Double.parseDouble(popline[0]) - minlon) * 120.0);
        int ypix = (int) Math.round((maxlat - (Double.parseDouble(popline[1]))) * 120.0);
        if ((xpix>=0) && (xpix < xsize) && (ypix >= 0) && (ypix < ysize)) {
          if (popline[4].equals(entry[0])) {
            bi.setRGB(xpix,ypix, white);
            pop += Integer.parseInt(popline[2]);
          } else {
            bi.setRGB(xpix, ypix, grey);
          }
        }          
      }
      System.out.println(entry[0]);
      Graphics g = (Graphics2D) bi.getGraphics();
      g.setColor(Color.WHITE);
      g.setFont(new Font("Calibri", Font.BOLD, 48));
      entry_s=entry_s.replaceAll("\t", "      ");
      g.drawString(entry_s, 50, ysize-100);
      ImageIO.write(bi,  "PNG",  new File(entry[0]+".png"));
      PW.println(entry_s+"\t"+pop);
    }
    PW.close();
  }
}

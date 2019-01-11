package jobs.e9_Global_Sim_Movie;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.GregorianCalendar;

import javax.imageio.ImageIO;

import com.mrc.GlobalRasterTools.GlobalRasterTools;

// We have a load of PNG files, size (eg) 2160 x 1080.
// Combine into a movie, overlaying onto a Landscan image
// And add some calendar information.

public class Movie {
  GlobalRasterTools GRT;
  String png_path = "\\\\fi--didenas1\\Test\\Results\\Feb05\\mov\\";
  String ls_path = "E:/Data/Census/Landscan2008/lspop2008.flt";
  String job_path = "E:/Jobs/Gsim-Movie/";
  int scale_down = 20;
  int out_w = 43200/scale_down;
  int out_h = 21600/scale_down;
  
  
  BufferedImage back;
  BufferedImage[] bis;
  Graphics[] gs;
  Graphics2D[] ngs;
  GregorianCalendar[] gcs;
  int[] cols = new int[256];
  String[] months = {"JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"};
  int RED = Color.RED.getRGB();
  int GREEN = Color.GREEN.getRGB();
  
  
  class ProcThread extends Thread {
    int id, max;
    char col_mode;
    ProcThread(int i, int m, char cm) { id = i; max = m; col_mode = cm;}
    
    public void run() {
      try {
        int f = id;
        String png = png_path+"mov"+String.format("%06d", f)+".png";
        while (new File(png).exists()) {
          BufferedImage frame = ImageIO.read(new File(png));
          int BLACK = frame.getRGB(0, 0);
          for (int j=0; j<out_h; j++) {
            for (int i=0; i<out_w; i++) {
              if (frame.getRGB(i, j)!=BLACK) {
                if (col_mode=='r') {
                  bis[id].setRGB(i, j, RED);
                
                } else if (col_mode=='t') {
                  bis[id].setRGB(i, j, cols[new Color(bis[id].getRGB(i,j)).getRed()]);
                
                } else if (col_mode=='c') {
                  if (frame.getRGB(i, j)!=GREEN) {
                    bis[id].setRGB(i, j, RED);
                  }
                
                } else if (col_mode=='i') {
                  if (frame.getRGB(i, j)!=GREEN) {
                    bis[id].setRGB(i, j, cols[new Color(bis[id].getRGB(i,j)).getRed()]);
                  }
                
                } else {
                  bis[id].setRGB(i, j, frame.getRGB(i,j));
                }
              }
            }
          }
          
          ngs[id].drawString(months[gcs[id].get(GregorianCalendar.MONTH)], 250,950);
          ngs[id].drawString(String.format("%02d", gcs[id].get(GregorianCalendar.DAY_OF_MONTH)), 380,950);
          ImageIO.write(bis[id], "PNG", new File(job_path+String.format("%06d", f)+(col_mode!='_'?col_mode:"")+".png"));
          if (id==0) System.out.println(f);
          f+=max;
          gcs[id].add(GregorianCalendar.HOUR, 6*max);
          png = png_path+"mov"+String.format("%06d", f)+".png";
          gs[id].drawImage(back, 0, 0, null);
        }
      } catch (Exception ex) { ex.printStackTrace(); }
    }
  }
  
  
  public void run(char col_mode) throws Exception {
    GlobalRasterTools GRT = new GlobalRasterTools();
    float[][] ls = GRT.loadFloatGrid(ls_path, 43200,21600,43200,20880,0,720);
    float max=0;
    for (int j=0; j<21600; j++) for (int i=0; i<43200; i++)
      if (ls[j][i]>max) max=ls[j][i];
    max = (float) Math.log(max);
    for (int i=0; i<=255; i++) cols[i] = (new Color(i,i,i)).getRGB(); 
    back = new BufferedImage(out_w, out_h, BufferedImage.TYPE_3BYTE_BGR);
    for (int j=0; j<21600; j+=scale_down) {
      for (int i=0; i<43200; i+=scale_down) {
        float m2=0;
        for (int jj=j; jj<j+scale_down; jj++) {
          for (int ii=i; ii<i+scale_down; ii++) {
            if (ls[jj][ii]>m2) m2=ls[jj][ii];
          }
        }
        if (m2>0) {
          m2 = (float) Math.log(m2);
          back.setRGB(i/scale_down, j/scale_down, cols[(int)(255.0*(m2/max))]);
        }
      }
    }
    
    if ((col_mode=='t') || (col_mode=='i')) {
      for (int i=0; i<=255; i++) cols[i] = (new Color(i,0,0)).getRGB(); 
    }
    int cores = Runtime.getRuntime().availableProcessors();
    bis = new BufferedImage[cores];
    gs = new Graphics[cores];
    ngs = new Graphics2D[cores];
    gcs = new GregorianCalendar[cores];
    
    ProcThread[] pt = new ProcThread[cores];
    for (int i=0; i<cores; i++) {
      bis[i] = new BufferedImage(out_w, out_h, BufferedImage.TYPE_3BYTE_BGR);
      gs[i]= bis[i].getGraphics();
      ngs[i] = bis[i].createGraphics();
      GRT.niceGraphics(ngs[i]);
      ngs[i].setColor(Color.WHITE);
      ngs[i].setFont(new Font("Calibri", Font.BOLD, 54));
      gcs[i] = new GregorianCalendar();
      gcs[i].set(GregorianCalendar.YEAR, 2009);
      gcs[i].set(GregorianCalendar.MONTH, 0);
      gcs[i].set(GregorianCalendar.DAY_OF_MONTH, 1);
      gcs[i].set(GregorianCalendar.HOUR, 0);
      gcs[i].set(GregorianCalendar.MINUTE, 0);
      gcs[i].set(GregorianCalendar.SECOND, 0);
      gcs[i].add(GregorianCalendar.HOUR, 6*i);
      gs[i].drawImage(back, 0, 0, null);
      pt[i] = new ProcThread(i, cores, col_mode);
      pt[i].start();
    }
    for (int i=0; i<cores; i++) pt[i].join();
  }
  
  public static void main(String[] args) throws Exception {
    Movie m = new Movie();
    //m.run('_');
    //m.run('r');
    //m.run('t');
    m.run('c');
    m.run('i');
    
  }
  
}

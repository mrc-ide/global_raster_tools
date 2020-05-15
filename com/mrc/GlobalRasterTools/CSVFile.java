package com.mrc.GlobalRasterTools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CSVFile {
  private ArrayList<String> colnames;
  private ArrayList<ArrayList<String>> data;
  
  private CSVFile(boolean header) {
    if (header) colnames = new ArrayList<String>();
    else colnames = null;
    data = new ArrayList<ArrayList<String>>();
  }
  
  public static CSVFile read(String file, boolean header) throws Exception {
    int no_cols = 0;
    CSVFile csv = new CSVFile(header);
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
    if (header) {
      String s = br.readLine();
      String[] bits = s.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
      for (int i=0; i<bits.length; i++) csv.colnames.add(bits[i].replaceAll("\"", ""));
      no_cols = bits.length;
    }
    String s = br.readLine();
    while (s!=null) {
      String[] bits = s.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
      if ((csv.data.size()==0) && (!header)) no_cols = bits.length; 
      if (no_cols!=bits.length) {
        System.out.println("Warning: column count mismatch at line "+(int)(csv.data.size()+1+(header?1:0)));
      }
      ArrayList<String> row = new ArrayList<String>();
      for (int i=0; i<bits.length; i++) {
        row.add(bits[i].replaceAll("\"", ""));
      }
      
      csv.data.add(row);
      s = br.readLine();
    }
    br.close();
    return csv;
  }
  
  public int getColIndex(String col) {
    return colnames.indexOf(col);
  }
  
  public String get(String col, int row)  {
    int colno = getColIndex(col);
    if (colno==-1) {
      System.out.println("Column "+col+" not found");
      return null;
    }
    return data.get(row).get(colno);
  }
  
  public ArrayList<String> getCol(int col) {
    ArrayList<String> result = new ArrayList<String>();
    for (int i=0; i<data.size(); i++) {
      result.add(data.get(i).get(col));
    }
    return result;
  }
  
  public ArrayList<String> getCol(String col) {
    return getCol(getColIndex(col));
  }
  
  public ArrayList<String> getRow(int r) {
    return data.get(r);
  }
    
  public String[] getColNames() {
    return colnames.toArray(new String[0]);
  }
  
  public static String print(ArrayList<String> arr, String sep) {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<arr.size(); i++) {
      sb.append(arr.get(i));
      if (i<arr.size()-1) sb.append(sep);
    }
    return sb.toString();
  }
  
  public static String print(String[] arr, String sep) {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<arr.length; i++) {
      sb.append(arr[i]);
      if (i<arr.length-1) sb.append(sep);
    }
    return sb.toString();
  }
  
  public static <T> ArrayList<T> unique(ArrayList<T> arr) {
    ArrayList<T> n = new ArrayList<T>();
    for (T element : arr) {
      if (!n.contains(element)) n.add(element);
    }
    return n;
  }
  
  public int getRowCount() { return data.size(); }
  
  public int getColCount() { return colnames.size(); }
  
}
  

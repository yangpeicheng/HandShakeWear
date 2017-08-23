package com.ypc.handshakewear.csvUtils;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by yangpc on 2017/7/7.
 * 写CSV文件，目录为Android根目录
 */

public class CsvOutput {
    private final String BASEDIR=android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"data";
    private CSVWriter mCSVWriter=null;
    private String mFilePath=null;
    public CsvOutput(){
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        String file=String.format("%d-%d.csv",cal.get(Calendar.MINUTE),cal.get(Calendar.SECOND));
        mFilePath=BASEDIR+ File.separator+file;
    }
    public CsvOutput(String filename){
        mFilePath=BASEDIR+ File.separator+filename;
        File file=new File(BASEDIR);
        if(!file.exists()){
            file.mkdir();
        }
        //mFilePath="/sdcard/"+File.separator+filename;
    }
    public void writeData(String[] value){
        if(mCSVWriter==null){
            File f=new File(mFilePath);
            if(f.exists() && !f.isDirectory())
                f.delete();
            try {
                mCSVWriter = new CSVWriter(new FileWriter(mFilePath,true));
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        if(value==null||mCSVWriter==null)
            return;
        mCSVWriter.writeNext(value);
    }
    public void closeFile(){
        if(mCSVWriter!=null) {
            try {
                mCSVWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

package com.realtek.Utils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DateStringFormat{
	public DateStringFormat(){
		
	}
	public String dateFormate(String date,String formatString) {
		// TODO Auto-generated method stub				
		SimpleDateFormat df_ori_exif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		SimpleDateFormat df_des = new SimpleDateFormat(formatString);
	    java.util.Date date_parse = null;
	     try {
	    	 date_parse = df_ori_exif.parse(date);
	    	
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     String formateDate1 = null;
	     formateDate1 = df_des.format(date_parse);
	     return formateDate1;
	}
	public String getFileCreateDate(File _file,String formatString) {
        File file = _file;
        Date last_date = new Date(file.lastModified());
        SimpleDateFormat df_des = new SimpleDateFormat(formatString);
        String last_modify_date=df_des.format(last_date);
 		return last_modify_date;
    }
	

}
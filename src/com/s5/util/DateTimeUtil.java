package com.s5.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.threeten.bp.OffsetDateTime;

public class DateTimeUtil {

	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static String getDefaultDateString(Date date) {
		return new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(date);
	}

	/*public static String formatDateTime(Date date, int timezoneOffset) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, timezoneOffset);
		return getDefaultDateString(cal.getTime());
	}*/
	
	/*public static String formatDateTime(Date date, int timezoneOffset,String format) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, timezoneOffset);
		return new SimpleDateFormat(format).format(cal.getTime());
	}*/
	
	/*public static long formatDateTimeInMili(Date date, int timezoneOffset) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, timezoneOffset);
		return cal.getTimeInMillis();
	}*/
	
	/* @param param 
	 * date yymmddHHMMSS format e.g. 091221102631 
	 * @return 
	 * yyyy-MM-dd HH:mm:ss format date
	 * 
	public static String parseDateTime(String dateString,String format) throws Exception{
	    Date date = new SimpleDateFormat(format).parse(dateString);
		return getDefaultDateString(date);
	}
	*/
	
	public static String parseDateTimeNew(Date dateString,String format) throws Exception{
//	    Date date = new SimpleDateFormat(format).parse(dateString);
		return new SimpleDateFormat(format).format(dateString);
	}
	
	
	/*public static Date formatDateTime(String dateString) throws Exception{
		return new SimpleDateFormat(DEFAULT_DATE_FORMAT).parse(dateString);
	}*/
	
	public static String getGMTDate() {
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date=new Date();
        String gmtDate =sdf.format(date);
		return gmtDate;
	}
	
	/*public static String toGMTDate(String date) throws Exception {
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date1 = sdf.parse(date);
        String gmtDate =sdf.format(date1);
		return gmtDate;
	}*/
	
	/*public static String toUTCDate(String date) throws Exception {
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date1 = sdf.parse(date);
        String utcDate =sdf.format(date1);
		return utcDate;
	}*/
	
/*	public static String toISTDate(String date) throws Exception {
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("IST"));
        Date date1 = sdf.parse(date);
        String istDate =sdf.format(date1);
		return istDate;
	}*/
	
	// Calculate date from 2000-01-01 in seconds
	/*public static String formatDateTime(long date) throws Exception {
		Date dateString = new Date((946684800 + date) * 1000);	//946684800 = 2000-01-01
		return(DateTimeUtil.getDefaultDateString(dateString));
	}*/
	
	public static boolean compairBothMessageDates(String oldDate,String newDate) {
		boolean isNew = true;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
			Date dt1 = sdf.parse(oldDate);
			Date dt2 = sdf.parse(newDate);
			if(dt1.after(dt2)) {
				isNew = false;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return isNew;
	}
	
	public static Date getDefaultFutureDate(int year) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR,year);
		cal.set(Calendar.MONTH,1);
		cal.set(Calendar.DAY_OF_YEAR,1);
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		return cal.getTime();
		}
	
	public static Date getDefaultDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR,1900);
		cal.set(Calendar.MONTH,1);
		cal.set(Calendar.DAY_OF_YEAR,1);
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		return cal.getTime();
		}
	public static Date getDateFromOffsetDateTime(OffsetDateTime date) { //2018-07-09T12:16:12Z
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR,date.getYear());
		cal.set(Calendar.MONTH,date.getMonthValue());
		cal.set(Calendar.DAY_OF_YEAR,date.getDayOfYear());
		cal.set(Calendar.HOUR_OF_DAY,date.getHour());
		cal.set(Calendar.MINUTE,date.getMinute());
		cal.set(Calendar.SECOND,date.getSecond());
		cal.set(Calendar.MILLISECOND,0);
		return cal.getTime();
		}
	public static void main(String [] areg) {
		OffsetDateTime off = OffsetDateTime.parse("2021-09-21T11:57:41Z");
		System.out.println(DateTimeUtil.getDateFromOffsetDateTime(off));

		
		
	}
}

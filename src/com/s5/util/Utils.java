package com.s5.util;

import java.io.InputStream;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class Utils {

	public static byte[] readBytes(InputStream in) {
		try{
		    byte[] data = new byte[512];
		        in.read(data);
		    return data;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getRowMessageBytesString(byte []dataRecord) {
		StringBuffer localStringBuffer = new StringBuffer();
		try{
		for (byte bt : dataRecord) {
			int i = bt & 0xFF;
			localStringBuffer.append((Integer.toHexString(i) + " ").toUpperCase());
		}
		}catch(Exception ex){}
		return localStringBuffer.toString();
	}
	
	public static String bytesToHexString(byte[] bytes) {
	    StringBuilder sb = new StringBuilder(bytes.length * 2);
	 
	    Formatter formatter = new Formatter(sb);
	    for (byte b : bytes) {
	        formatter.format("%02x", b);
	    }
	    formatter.close();
	    return sb.toString();
	}
	
	public static double parseLatitude(String coord,String d) {
        double _lat = Double.parseDouble(coord);
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
    	    lat = formatDecimalValue("#.######",lat);
            return d.equals("S")? -lat : lat;
        } else {
            return 90.0; // invalid latitude
        }
	}
	
	public static double parseLongitude(String coord,String d) {
		double _lon = Double.parseDouble(coord);
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            lon = formatDecimalValue("#.######",lon);
            return d.equals("W")? -lon : lon;
        } else {
            return 180.0; // invalid longitude
        }
	}
	
	public static byte[] hexToBytes(String hexString, int lenght) {
		HexBinaryAdapter adapter = new HexBinaryAdapter();
		while (hexString.length() % 2 != 0) {
			hexString = "0" + hexString;
		}
		byte[] bytes = adapter.unmarshal(hexString);
		if (bytes.length == lenght)
			return bytes;
		else {
			byte[] byteArray = new byte[lenght];
			for (int i = 0; i < lenght; i++) {
				if (bytes.length + i < lenght) {
					byteArray[i] = (byte) 0;
				} else {
					byteArray[i] = bytes[i - (lenght - bytes.length)];
				}
			}
			return byteArray;
		}
	}
	
	public static double formatDecimalValue(String format,double value){
		DecimalFormat decimalFormatter = new DecimalFormat(format);
		return Double.valueOf(decimalFormatter.format(value));
	}
	
	//knots to kilometers per hour
	public static double knotsToKmh(double speeed){
		double kmh = speeed*1.852;
		kmh = formatDecimalValue("#.#",kmh);
		return kmh;
	}
	
	public static String trimToByteString(String message){
		String[] str1 = message.split(" ");
		String messageText = "";
		for(int i = 0;i<str1.length;i++){
			if(str1[i].length() < 2){
				messageText += "0"+str1[i];
			}else{
				messageText += str1[i];
			}
		}
		return messageText;
	}
	
	public static int getIntegerValue(double value){
		return (int)value;
	}
	
	public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
	
	public static String reverseHexMessageFromByteArray(byte[] packet,int from,int count){
    	
    	byte[] newArray = new byte[count]; 
    	System.arraycopy(packet, from, newArray, 0, count);
    	
    	ArrayUtils.reverse(newArray);
    	String message = Utils.bytesToHexString(newArray);
    	return message;
    }
	
	public static List<byte[]> splitByteArrayByPattern(byte[] input,byte[] pattern) {
	    List<byte[]> byteList = new ArrayList<byte[]>();
	    int blockStart = 0;
	    for(int i=0; i<input.length; i++) {
	       if(isMatch(pattern,input,i)) {
	    	   byteList.add(Arrays.copyOfRange(input, blockStart, i));
	          blockStart = i+pattern.length;
	          i = blockStart;
	       }
	    }
	    byteList.add(Arrays.copyOfRange(input, blockStart, input.length ));
	    return byteList;
	}
	
	public static List<byte[]> splitByteArrayByLength(final byte[] data, final int size){
		final int length = data.length;
		List<byte[]> byteList = new ArrayList<byte[]>();
		int stopIndex = 0;
		
		for (int startIndex = 0; startIndex + size <= length; startIndex += size){
			stopIndex += size;
			byteList.add(Arrays.copyOfRange(data, startIndex, stopIndex));
		}
		
//		if (stopIndex < length)
//			byteList.add(Arrays.copyOfRange(data, stopIndex, length));

  		return byteList;
	}
	
	public static boolean isMatch(byte[] pattern, byte[] input, int pos) {
	    for(int i=0; i< pattern.length; i++) {
	        if(pattern[i] != input[pos+i]) {
	            return false;
	        }
	    }
	    return true;
	}
	
	public static String convertByteArrayToString(byte[] input){
		return new String(input);
	}
	
	public static String littleEndianHexToHex(final String hex) {
	    String hexLittleEndian = "";
	    if (hex.length() % 2 != 0) return hexLittleEndian;
	    for (int i = hex.length() - 2; i >= 0; i -= 2) {
	        hexLittleEndian += hex.substring(i, i + 2);
	    }
	    return hexLittleEndian;
	}
	
	public static double parseBinaryLatLng(String hexLatLng) {
	    double value = ((double)(new BigInteger(Utils.hexToBytes(hexLatLng, 4))).intValue()/1000000.0);
	    return value;
	}
	
	public static double hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        double val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            double d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
	public static long hex2Long(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        long val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            long d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
	
	public static  byte[] hexToBytes(String hexString) {
	     HexBinaryAdapter adapter = new HexBinaryAdapter();	     
	     byte[] bytes = adapter.unmarshal(hexString);
	     return bytes;
	}
	
	public static float parseFloat(String value) {
		float floatValue = 0f;
		if(StringUtils.isNotBlank(value)){
			try {
			floatValue = Float.parseFloat(value);
			} catch(Exception e) {
				
			}
		}
			
		return floatValue;
	}
	
}

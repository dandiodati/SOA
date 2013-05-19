/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;
import java.text.*;


/**
 * The DateUtils class provides general-purpose date/time manipulation methods.
 */
public final class DateUtils
{
	/**
	 * Fixed time values in milliseconds
	 */
	public final static long SECOND           = 1000;
	public final static long MINUTE           = 60000;
	public final static long HOUR             = 3600000;
	public final static long DAY              = 86400000;
	
	/**
	 * Use this value to compensate actual hours for the zero time point in JAVA
	 * which is 01/01/1970 04:00. Useful when need to display time only, such as
	 * foe example '00 h 25 min 45 sec' and original value was taken in msec.
	 */ 
	public final static long ZERO_TIME_OFFSET = 57600000;
	
	
	// Class only has static methods, so don't allow instances to be created!
	private DateUtils()
	{
		// NOT TO BE USED !!!
	}


	/**
	 * Get the current date/time in cannonical format.
	 *
	 * @return  A string containing the current time.
	 */
	public static final String getCurrentTime()
	{
		return (new Date()).toString();
	}
	
	
	/**
	 * Get current date/time in specified format
	 * 
	 * @param	format  input string format
	 * @return	current date
	 */
	public static String getCurrentTime(String format)
		throws java.text.ParseException
	{
		if(StringUtils.hasValue(format))
		{
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return(sdf.format(new Date()));
		}
		else
		{
			// if format is not set return current time
			return getCurrentTime();
		}
	}


	/**
	 * Get the current date/time in cannonical format.
	 *
	 * @return  current time value in msec.
	 */
	public static final long getCurrentTimeValue()
	{
		return (new Date()).getTime();
	}
	
	
	/**
	 * Given input date/time string returns only date string in specified format.
	 * 
	 * @param		datetime string 
	 * @param		inFormat  input date/time string format
	 * @param		outFormat    output date string format
	 * 
	 * @returns		date string in output format
	 * 
	 * @exception	ParseException if processing fails or invalid input parameters
	 */
	public static String getDateString(String datetime, String inFormat, String outFormat)
		throws java.text.ParseException
	{
		if(StringUtils.hasValue(datetime) && StringUtils.hasValue(inFormat))
			return formatTime(datetime, inFormat, outFormat);
		else
			throw new java.text.ParseException("ERROR: Input parameters are not set.", 0);
	}
	
	
	/**
	 * Given input date/time string returns only time string in specified format.
	 * 
	 * @param		datetime string 
	 * @param		inFormat  input date/time string format
	 * @param		outFormat    output time string format
	 * 
	 * @returns		time string in output format
	 * 
	 * @exception	ParseException if processing fails or invalid input parameters
	 */
	public static String getTimeString(String datetime, String inFormat, String outFormat)
		throws java.text.ParseException
	{
		if(StringUtils.hasValue(datetime) && StringUtils.hasValue(inFormat))
		{
			SimpleDateFormat formatter = new SimpleDateFormat(inFormat);
			Date date = formatter.parse(datetime);
			
			// Check if there are hours, minutes or seconds set
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			if (cal.get(Calendar.HOUR) > 0 ||
				cal.get(Calendar.MINUTE) > 0 ||
				cal.get(Calendar.SECOND) > 0)
			{
				formatter.applyPattern(outFormat);
				return formatter.format(date);
			}
			else
				return "";
		}
		else
			throw new java.text.ParseException("ERROR: Input format parameter not set.", 0);
	}
	
	
	/**
	 * Given input date as long value returns it as string in specified format.
	 * 
	 * @param		date  value in msec
	 * @param		format  input string format
	 * 
	 * @returns		date value in msec
	 * 
	 * @exception	ParseException if processing fails
	 */
	public static String formatTime(long msec, String format)
		throws java.text.ParseException
	{
		if(StringUtils.hasValue(format))
		{
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return (sdf.format(new Date(msec)));
		}
		else
		{
			// if format is not set return current time
			return (new Date(msec)).toString();
		}
	}
	
	
	/**
	 * Given input date string in given format returns its long value.
	 * 
	 * @param		date  input date string
	 * @param		format  input string format
	 * 
	 * @returns		date value in msec
	 * 
	 * @exception	ParseException if processing fails
	 */
	public static long formatTime(String date, String format)
		throws java.text.ParseException
	{
		if(StringUtils.hasValue(date) && StringUtils.hasValue(format))
		{
			SimpleDateFormat formatter = new SimpleDateFormat(format);
			Date day = formatter.parse(date);
			return (day.getTime());
		}
		else
		{
			// if any of the input parameters is not set return current time
			return getCurrentTimeValue();
		}
	}

	
	/** 
	 * Given input date string converts it to the new format.
	 * 
	 * @param		date  input date string
	 * @param		inFormat  input string format
	 * @param		outFormat    output string format
	 * 
	 * @returns		date string in format
	 * 
	 * @exception	ParseException if processing fails
	 */
	public static String formatTime(String date, String inFormat, String outFormat)
		throws java.text.ParseException
	{
		if(StringUtils.hasValue(date) &&
		   StringUtils.hasValue(inFormat))
		{
			SimpleDateFormat inFormatter = new SimpleDateFormat(inFormat);
			Date day = inFormatter.parse(date);
			
			SimpleDateFormat outFormatter = null;
			if(StringUtils.hasValue(outFormat))
				outFormatter = new SimpleDateFormat(outFormat);
			else
				outFormatter = new SimpleDateFormat();
			
			return(outFormatter.format(day));
		}
		else
		{
			// if any of the input parameters is not set return current time
			// in canonical format for default locale
			return getCurrentTime();
		}
	}
	
	
	/**
	 * Returns current date/time in ORACLE format
	 * 
	 * @return	current date
	 * 
	 * @exception	ParseException if format fails
	 */
	public static String getDBCurrentTime()
		throws java.text.ParseException
	{
		String dbFormat = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(dbFormat);
		return (sdf.format(new Date()));
	}


	/**
	 * Get a string containing the current time in a format
	 * suitable for use in filenames(YYYYMMDDHHMMSSMSEC).
	 *
	 * @return  A string containing the current time.
	 */
	public static final String getDateToMsecAsString()
	{
		Calendar c = Calendar.getInstance();

		c.setTime(new Date());

		StringBuffer sb = new StringBuffer();

		int temp;

		sb.append(c.get(Calendar.YEAR));

		temp = c.get(Calendar.MONTH);
		temp ++;  // Java month is between 0 and 11.
		if(temp < 10)
			sb.append('0');
		sb.append(temp);

		temp = c.get(Calendar.DAY_OF_MONTH);
		if(temp < 10)
			sb.append('0');
		sb.append(temp);

		temp = c.get(Calendar.HOUR_OF_DAY);
		if(temp < 10)
			sb.append('0');
		sb.append(temp);

		temp = c.get(Calendar.MINUTE);
		if(temp < 10)
			sb.append('0');
		sb.append(temp);

		temp = c.get(Calendar.SECOND);
		if(temp < 10)
			sb.append('0');
		sb.append(temp);

		sb.append(c.get(Calendar.MILLISECOND));

		return(sb.toString());
	}  


	/**
	 * TESTING ONLY. 
	 *
	 * @param	args Array of parameters passed to the application
	 *			via the command line.
	 */
	public static void main (String[] args)
	{
		try
		{
			Debug.log(Debug.UNIT_TEST, "\n=======================");
			Debug.log(Debug.UNIT_TEST, "Current Time in Msec = " + getCurrentTimeValue());
			Debug.log(Debug.UNIT_TEST, "\n");
			
			Debug.log(Debug.UNIT_TEST, "One Hour = " + formatTime(1000*60*60, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "One Minute = " + formatTime(1000*60, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "One Second = " + formatTime(1000, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "Absolute Zero = " + formatTime(0, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "\n");
			
			Debug.log(Debug.UNIT_TEST, "One Hour With Offset = " + formatTime(1000*60*60 - ZERO_TIME_OFFSET, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "One Minute With Offset = " + formatTime(1000*60 - ZERO_TIME_OFFSET, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "One Second With Offset = " + formatTime(1000 - ZERO_TIME_OFFSET, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "Absolute Zero With Offset = " + formatTime(0 - ZERO_TIME_OFFSET, "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "Absolute Zero Time With Offset = " + formatTime(0 - ZERO_TIME_OFFSET, "MM/dd/yyyy HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "\n");
			
			Debug.log(Debug.UNIT_TEST, "One Hour As Number = " + formatTime("01:00:00", "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "One Minute As Number = " + formatTime("00:01:00", "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "One Second As Number = " + formatTime("00:00:01", "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "Absolute Zero As Number = " + formatTime("00:00:00", "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "\n");
			
			Debug.log(Debug.UNIT_TEST, "Current Date = " + getDateString(formatTime(getCurrentTimeValue(), "dd/MM/yyyy"), "dd/MM/yyyy", "dd/MM/yyyy"));
			Debug.log(Debug.UNIT_TEST, "Current Time = " + getTimeString(formatTime(getCurrentTimeValue(), "dd/MM/yyyy HH:mm:ss"), "dd/MM/yyyy HH:mm:ss", "HH:mm:ss"));
			Debug.log(Debug.UNIT_TEST, "=======================\n");
		}
		catch (Exception e)
		{
			Debug.log(Debug.MSG_ERROR, e.toString());
		}
	}
}

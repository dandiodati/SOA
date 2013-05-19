///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.nightfire.framework.message.MessageException;

/**
* This is a utility for converting a formatted date String from
* one format and time zone to another format and time zone. 
*/
public class TimeZoneUtil {

   /**
   * The time zone for the input date. We are converting from this time zone.
   */
   private TimeZone inputZone;

   /**
   * The time zone for the output date. We are converting to this time zone. 
   */
   private TimeZone outputZone;

   /**
   * The date format of the incoming date string. This is used to parse the
   * input date. 
   */
   private String inputFormat;


   /**
   * The date format of the output date string. This is used to format the
   * output date.
   */
   private String outputFormat;   

   /**
   * This is used to indicate the default local time zone. 
   */
   public static final String LOCAL_TIME = "LOCAL";

   /**
   * Constructor.
   *
   * @param inputTimeZone the time zone that input dates will be in.
   *                      This should be "LOCAL" for the local time zone.
   * @param inputDateFormat this is the simple date format of the input
   *                        date string. This is used to parse the
   *                        input string.
   * @param outputTimeZone The output date string will be in this time zone.
   * @param outputDateFormat The output date string will be in this format.
   *
   *
   */
   public TimeZoneUtil(String inputTimeZone,
                       String inputDateFormat,
                       String outputTimeZone,
                       String outputDateFormat){

      inputZone  = getTimeZone( inputTimeZone );
      outputZone = getTimeZone( outputTimeZone );

      inputFormat  = inputDateFormat;
      outputFormat = outputDateFormat;

   }

   /**
   * This gets the TimeZone object for the given string. "LOCAL" results
   * in the default time zone getting returned.
   */
   public static TimeZone getTimeZone(String timeZone){

      if ( timeZone.equalsIgnoreCase(LOCAL_TIME) ) {

         return TimeZone.getDefault();

      }

      return TimeZone.getTimeZone( timeZone );

   }

   // Parse the time from the input string using the input time format and
   // input time zone. Change the time zone and return the reformatted string.
   public String convert( String inputTime ) throws MessageException {

      SimpleDateFormat inputParser = new SimpleDateFormat( inputFormat );
      inputParser.setTimeZone( inputZone );

      Date date = null;

      try {

        date = inputParser.parse( inputTime );

      }
      catch(ParseException e) {

        throw new MessageException( "Input time [" +
                                    inputTime +
                                    "] does not match format ["+
                                    inputFormat+"]");

      }

      return convert( outputZone, outputFormat, date );

   }

   /**
   * This gets the current time in the given time zone and format.
   *
   * @param timeZone the time zone for the returned string.
   * @param format the format that the date string will be returned in. 
   */
   public static String getCurrentTime(String timeZone, String format){

      return convert(timeZone, format, new Date());

   }

   /**
   * Returns the given date adjusted to the given time zone and in the
   * given date format.
   */
   public static String convert(String timeZone,
                                String format,
                                Date date){

      TimeZone zone = getTimeZone(timeZone);
      return convert(zone, format, date);

   }

   /**
   * Returns the given date adjusted to the given time zone and in the
   * given date format.
   */
   public static String convert(TimeZone zone,
                                String format,
                                Date date){

      SimpleDateFormat formatter = new SimpleDateFormat( format );
      formatter.setTimeZone( zone );

      return formatter.format( date );

   }

   /**
   * This creates a Date object based on the given string in the
   * given time zone and format. 
   */
   public static Date parse(String timeZone,
                            String format,
                            String dateString)
                            throws MessageException{                            

      TimeZone zone = getTimeZone(timeZone);

      return parse( zone, format, dateString );

   }

   /**
   * This creates a Date object based on the given string in the
   * given time zone and format. 
   */
   public static Date parse(TimeZone zone,
                            String format,
                            String dateString)
                            throws MessageException{

      SimpleDateFormat parser = new SimpleDateFormat( format );
      parser.setTimeZone( zone );

      Date date = null;

      try {

         date = parser.parse( dateString );

      }
      catch(ParseException e) {

         throw new MessageException( "Could not parse time [" +
                                     dateString +
                                     "]. Format does not match ["+
                                     format+"]");

      }

      return date;

   }
   /**
    * This method convert local time to GMT time.
    * @param inputTime
    * @return
    * @throws MessageException
    */
   public static String convertTime ( String inputTime ) throws MessageException {
   	
   	TimeZone inputTimeZone = TimeZone.getDefault();

   	TimeZone outputTimeZone = TimeZone.getTimeZone( "GMT" );
   	
   	String inputFormat = "MM-dd-yyyy-hhmmssa";

   	String outputFormat = "MM-dd-yyyy-hhmmssa";

   	SimpleDateFormat sdf = new SimpleDateFormat( inputFormat );
   	
   	sdf.setTimeZone( inputTimeZone );

   	Date date = null;
   	try {

   	    date = sdf.parse( inputTime );

   	} catch (ParseException e) {

   	    throw new MessageException( "Source time, [" + inputTime + "], cannot be parsed.");
   	}

   	sdf  = new SimpleDateFormat( outputFormat );
   	sdf.setTimeZone( outputTimeZone );

   	return sdf.format( date );
   }

}
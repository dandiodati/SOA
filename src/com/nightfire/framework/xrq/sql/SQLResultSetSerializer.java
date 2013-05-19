package com.nightfire.framework.xrq.sql;

import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.locale.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.*;


import java.text.*;
import java.util.*;

import java.sql.*;
import org.w3c.dom.*;
import java.io.*;





public class SQLResultSetSerializer implements RecordSerializer
{



   private String dateTimeFormat;
   private java.text.SimpleDateFormat formatter;


   private String[] columns = null;

   private ResultSet results;
   private ResultSetMetaData metaInfo;


  
   // indicates if there are any more records
   private boolean hasNext = false;

   protected SQLResultSetSerializer()
   {}

   public SQLResultSetSerializer(ResultSet sqlResults, String dateFormat) throws FrameworkException
   {

       this.results = sqlResults;

       try {
         metaInfo = this.results.getMetaData();
         columns = new String[metaInfo.getColumnCount()];

         for(int i = 0; i < columns.length; i++ )
         {
           columns[i] = metaInfo.getColumnName(i+1);
         }

       } catch (SQLException se) {
          String error = "SQLResultsSetSerializer: failed to obtain column names: " + DBInterface.getSQLErrorMessage(se);
          Debug.error(error);
          throw new FrameworkException(error);
       }

        setDateTimeFormat(dateFormat);
   }


    /**
     * returns an array of all column names. Returns null if not available.
     * @return An array of column names.
     * NOTE: This array should not be altered or changed.
     */
    public String[] getColumnNames()
    {
       return columns;
    }

    /**
     * Sets an array of column names to be used.
     *
     */
    public void setColumnNames( String [] names)
    {
       columns = names;
    }


     /**
     * converts a record from a serialized string into a xml dom.
     * @param dom the Document to add the xml to.
     * @param xmlParentLoc The parent location for the parent node of the added xml.
     * @param serializedStr the seriallized string to convert to xml.
     *
     * @exception throws a procesing exception if an error occurs.
     *
     */
    public void toXML(Document dom, String xmlParentLoc, String serializedStr) throws FrameworkException
    {

       try {

          ParsingTokenizer toker = new ParsingTokenizer(new StringReader(serializedStr));
          toker.setTrimming(false);
          toker.addDelimiter(DELIM);

          int curType, prevType = -1;
          XMLMessageGenerator gen = new XMLMessageGenerator(dom);

          if (!gen.getParser().exists(xmlParentLoc) )
             gen.create(xmlParentLoc);
          
          MessageContext context = gen.getContext(xmlParentLoc);

          int i = 0;
          boolean done = false;

          while ( true ) {

             curType = toker.nextToken();


             if (curType == ParsingTokenizer.RETRIEVED_TOKEN ) {
                String tok = toker.getToken();

                gen.setValue(context, columns[i], tok);

                i++;

              // missing a token at the beginning.
             } else if ( curType == ParsingTokenizer.RETRIEVED_DELIMITER && ( prevType == -1 || prevType == curType) ) {
                i++;

             } else if ( curType == ParsingTokenizer.EOF) {
                //missing a token at the end.
                break;
             }

             prevType = curType;
          }
       } catch (FrameworkException fe) {
          String error = StringUtils.getClassName(this)+ ": Failed to convert serialized string to xml: " + fe.getMessage();
          Debug.error(error);
          throw new FrameworkException (error);
       }

    }



   /**
   * Indicates that another record is available.
   * @return boolean true if there is another record, otherwise false.
   */
  public boolean hasNext()
  {

    // the has next variable allows multiple calls to hasNext and prevents the
    // the movement to the next sql statement.
    // Otherwise if multiple calls to hasNext was called, with out a next call, then we
    // would lose rows.
    if (hasNext)
      return true;

    try {
     return( hasNext = results.next() );
    } catch (SQLException e) {
       Debug.error("SQLResultsSetSerializer: Failed to determine if this was the last record: " + DBInterface.getSQLErrorMessage(e) );
    }

    return hasNext;
  }



  /**
   * returns the next record from the database. The hasNext method must be called before
   * each call to this next method. A next call without a hasNext will cause an exeception.
   * NOTE: Each returned record is a set of fields seaparated by DELIM.
   * @param returns the next serialized String db record.
   */
   public String next()
   {

      try {
         return( formatRec() );
      } catch (SQLException e) {
       Debug.error("SQLResultsSetSerializer: Failed to get next record: " + DBInterface.getSQLErrorMessage(e) );
      } finally {
         hasNext = false;
      }

      return null;

   }

   /*
    *  Cleans up the result set and closes the prepared statement.
    * This method should only be called by the creating QueryExecutor class.
    */
   protected void cleanup() throws FrameworkException
   {


      try {
        if ( results != null ) {
           results.close();
        }
      } catch (SQLException se) {
         String err = "SQLResultSetSerializer: Failed to close sql results set: " + DBInterface.getSQLErrorMessage(se);

         Debug.error(err);
         throw new FrameworkException(err);
      }

   }

   /**
     * sets the time format used for conversion of dates.
     *
     * @param datetime - the date time format.
     * For more information on date time format strings, see
     * {@link java.text.SimpleDateFormat}.
     */
    private final void setDateTimeFormat(String dateTime)
    {
       this.dateTimeFormat = dateTime;
       formatter = new java.text.SimpleDateFormat();
       formatter.applyPattern(dateTimeFormat);
    }

   /**
    * converts a row in the database into a serialized string. ( fields separated by DELIM).
    * Each value from the database is converted to the corresponding string value.
    * Each date/time is converted from the database value into the specified locale format (dateFormat
    * passed into the constructor).
    */
   private String formatRec() throws SQLException
   {
      StringBuffer serialBuffer = new StringBuffer();

      convertDBData(serialBuffer, 1);

      for (int i = 2; i <= columns.length; i++ ) {
          serialBuffer.append(DELIM);
          convertDBData(serialBuffer, i);
      }


      String str = serialBuffer.toString();

      if ( Debug.isLevelEnabled(Debug.IO_STATUS) )
         Debug.log(Debug.IO_STATUS, "SQLResultSetSerializer.formatRec: serialized record: " + str);

      return str;

   }

   // converts values from the db into their string equivalent.
   private void convertDBData(StringBuffer buffer, int columnLoc) throws SQLException
   {

          int colType = metaInfo.getColumnType(columnLoc);

          // Any binary data has to be a binary form of a string, such as a xml.
          // True binary data is not supported. Any binary field are converted to a string.


         	if ( colType == Types.LONGVARBINARY || colType == Types.VARBINARY ||
						colType == Types.BINARY || colType == Types.BLOB || colType == Types.CLOB )
					{
                        try
                        {
                            buffer.append( DBLOBUtils.getCLOB( results, columnLoc ) );
                        }
                        catch ( Exception e )
                        {
                            throw new SQLException( e.toString() );
                        }
					}
          // all time and date types are converted into the locale specific date/time format.
          //
					else if (colType == Types.DATE || colType == Types.TIME ||
							 colType == Types.TIMESTAMP)
					{
						java.sql.Timestamp ts = results.getTimestamp(columnLoc);

						if (ts != null)
						{
							java.util.Date date = new java.util.Date(ts.getTime());
              buffer.append( formatter.format(date) );
						}

					}
          // else it is assumed that the field is a varchar or number and try to convert it.
					else
					{
             String value = results.getString(columnLoc);
             if (value != null ) 
                buffer.append( value );
          }


   }


} 

/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/XMLMessageLogger.java $
 */

package com.nightfire.adapter.messageprocessor;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * This is a generic class for logging XML messages to the database. All the
 * columns that are to be updated are specified in the persistentproperties for
 * this class. Only the column with the name Message that actually contains the
 * XML message is not specified in the persistent properties. There is a persistent
 * property logMessage to specify if the message is to be logged or not. One more special
 * feature of this class is that for updating any column with the current sysdate,
 * the DEFAULT value for that column in the persistent properties is specified as "SYSDATE",
 * with the OPTIONAL value set to TRUE and LOCATION set to FALSE. Similar values can be
 * set when the value is a known beforehand and not extracted from the XML document
 */
public class XMLMessageLogger extends MessageProcessorBase
{

    private String inputMessage;
    private NVPair[] dataForDB;

    private String tableName = null;
    /**
     * This is the number of columns as specified in the properties
     */
    private int noOfColumns = 0;
    /**
     * This is the total number of columns as specified in the properties and
     * any other columns like the MESSAGE, MESSAGE_KEY etc.
     */
    private int totalNoOfColumns = 0;
    private String logMessage = null;
    private String separator = null;
    private boolean requiresTransactionalLogging = true;  // May be set to false by a property.
    /**
     * Whether to validate the XML input while parsing it. If the corresponding
     * property - VALIDATE_XML is set to true in the persistent properties, it
     * can sometimes considerably slow down the processing due to XML validation time
     */
    private String validateXML = null;

    /**
     * Prefix of property names to be used, to fetch properties from the persistentproperties
     * if noOfColumns > 0
     */
    private static final String SCORE = "_";
    private static final String COMPONENT_VAL = "COMPONENT" + SCORE;
    private static final String LOCATION_VAL = "LOCATION" + SCORE;
    private static final String OPTIONAL_VAL = "OPTIONAL" + SCORE;
    private static final String DEFAULT_VAL = "DEFAULT" + SCORE;

    /**
     * Persistent properties' names
     */
    private static final String TABLE_NAME_PROP = "TABLE_NAME";
    private static final String NO_OF_COLS_PROP = "NO_OF_COLS_TO_UPDATE";
//    private static final String NAME_PROP = "NAME";
//    private static final String NEXT_PROC_NAME_PROP = "NEXT_PROCESSOR_NAME";
    private static final String LOG_MESSAGE_PROP = "LOG_MESSAGE";
    private static final String VALIDATE_XML_PROP = "VALIDATE_XML";
    private static final String SEPARATOR_PROP = "SEPARATOR";
    private static final String MESSAGE = "MESSAGE";
    private static final String DUMMY = "dummy";
    private static String TRANSACTIONAL_LOGGING = "TRANSACTIONAL_LOGGING";
    /**
     * Initializes this object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {
         //Still to check for white spaces in column names and table names properties.
        // Call the abstract super class's initialize method. This initializes
        // the adapterProperties hashtable defined in the super class and
        // retrieves the name and toProcessorNames values from the properties.

        super.initialize(key, type);

        if ( Debug.isLevelEnabled ( Debug.OBJECT_LIFECYCLE ) )
            Debug.log(Debug.OBJECT_LIFECYCLE, "XMLMessageLogger: Initializing.....");

        //Initialize a string buffer to contain all errors
        StringBuffer strBuf = new StringBuffer();

        String noOfColumnsStr = (String) adapterProperties.get(NO_OF_COLS_PROP);
        try
        {
          noOfColumns = Integer.parseInt (noOfColumnsStr);
        }
        catch (NumberFormatException nfe)
        {
          strBuf.append (": Number of columns value is of invalid type ");
        }

        tableName = (String) adapterProperties.get(TABLE_NAME_PROP);
        if (tableName == null)
          strBuf.append (": Table name not set ");

        logMessage = (String) adapterProperties.get(LOG_MESSAGE_PROP);
        if (logMessage == null)
          strBuf.append (": Log message not set ");
        else
        {
          if (logMessage.equalsIgnoreCase("TRUE"))
            totalNoOfColumns = noOfColumns + 1;
          else
            totalNoOfColumns = noOfColumns;
        }

        validateXML = (String) adapterProperties.get(VALIDATE_XML_PROP);
        if (validateXML == null)
          strBuf.append (": Validate XML not set");

        separator = (String) adapterProperties.get(SEPARATOR_PROP);
        if (separator == null)
          strBuf.append (": SEPARATOR not set");

        //Only COMPONENT and OPTIONAL values are checked for nulls, as the DEFAULT
        //and LOCATION values can be nulls.
        for (int colNo=1; colNo <= noOfColumns; colNo++)
        {
          if  ( ( (String)adapterProperties.get (COMPONENT_VAL + colNo) ) == null )
               strBuf.append (": " + COMPONENT_VAL + colNo + " is null");

          if ( ( (String)adapterProperties.get (OPTIONAL_VAL + colNo) ) == null )
              strBuf.append (": " + OPTIONAL_VAL + colNo + " is null");

        }

        String transactionalLoggingRequired = (String) adapterProperties.get(TRANSACTIONAL_LOGGING);

        if (transactionalLoggingRequired != null && transactionalLoggingRequired.equalsIgnoreCase("FALSE")) {
            requiresTransactionalLogging = false;
        }
        
        if (strBuf.length()!=0)
          throw new ProcessingException ("ERROR: XMLMessageLogger " + strBuf.toString());

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "XMLMessageLogger.initialize: Properties found:");

    }


    /**
     * Process the input message (DOM or String) and (optionally) return
     * a name / value pair.
     *
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  Optional NVPair containing a Destination name and a Document,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    public NVPair[] execute ( MessageProcessorContext mpcontext, Object input )
    throws MessageException, ProcessingException {

        XMLMessageParser inputParser = null;

        if(input == null) {

            return null;

        } else if ( !(input instanceof String) ) {

            throw new ProcessingException("ERROR: XMLMessageLogger: " +
                                       "The input must be a string.");
        }

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log(Debug.MSG_STATUS, "XMLMessageLogger: processing the message. ");

        if (totalNoOfColumns == 0)
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log(Debug.MSG_STATUS, "XMLMessageLogger: Exiting as nothing to update");
          return formatNVPair((String)input);
        }

        if ( validateXML.equalsIgnoreCase ("FALSE" ) )
          XMLMessageBase.enableValidation(false);
        else
          //default value is true, but set it just in case some other component has
          //set it to false.
          XMLMessageBase.enableValidation(true);

        inputMessage = (String) input;

        inputParser = getParser( inputMessage );

        extractMessageData( inputParser );

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log (Debug.MSG_STATUS, "XMLMessageLogger: Completed extracting data");

        try {

            if (requiresTransactionalLogging) {

                if ( Debug.isLevelEnabled ( Debug.MAPPING_DATA ) )
                    Debug.log( Debug.MAPPING_DATA,
                          "XMLMessageLogger.process: Transactional database logging used.");

                DBLog.logAsciiMessageToDatabase(tableName, dataForDB, mpcontext.getDBConnection() );

            } else {

                if ( Debug.isLevelEnabled ( Debug.MAPPING_DATA ) )
                Debug.log( Debug.MAPPING_DATA,
                          "XMLMessageLogger.process: Database logging is not transactional.");

                DBLog.logAsciiMessageToDatabase(tableName, dataForDB);
            }

        } catch (DatabaseException e) {
            Debug.log(Debug.ALL_ERRORS, "ERROR: XMLMessageLogger: " +
                                       "Attempt to log to database failed with error message: " +
                                       e.getMessage());

            throw new ProcessingException("ERROR: XMLMessageLogger: " +
                                       "Attempt to log to database failed with error message: " +
                                       e.getMessage());
        }   finally {

            if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
                Debug.log( Debug.DB_STATUS, "XMLMessageLogger: clearing prepared statements for " +
                  tableName + " table.");
            try {
              DBLog.clearPreparedStatementsForTable(tableName);
            }
            catch (DatabaseException e) {
                if ( Debug.isLevelEnabled ( Debug.DB_ERROR ) )
                    Debug.log( Debug.DB_ERROR, "XMLMessageLogger.process: " +
                      "Could not clear prepared statements, " + e.getMessage());
            }
        }

        return formatNVPair((String)input);
     }


    /**
     * A utility method for creating an XMLMessageParser.
     * with a DOM representing the given XML String.
     *
     * @param  xmlToParse The input XML.
     *
     * @exception MessageException thrown if bad message
     *
     * @exception ProcessingException thrown if processing fails
     *
     * @return  The desired parser
     */
    private static XMLMessageParser getParser(String xmlToParse) throws MessageException, ProcessingException {

        MessageParser parser;

        try {

           parser =
            (XMLMessageParser) MessageParserFactory.create(Message.XML_TYPE);

        } catch (MessageParserException e) {

            throw new ProcessingException(e.getMessage());
        }

        try {

            parser.parse(xmlToParse);

        } catch(MessageParserException e) {
                throw new MessageException(e.getMessage());
        }

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "XMLMessageLogger.getParser: " +
                  "Successfully parsed xml file. ");
        return (XMLMessageParser)parser;
    }

    /**
     * Extract data from the XML input message and prepare for logging it to the database
     * @param inputParser Parser for the XML docuemnt to extract data from
     * @exception ProcessingException thrown if processing fails
     *
     */
    private void extractMessageData(XMLMessageParser inputParser) throws ProcessingException
    {
        // Set up the NVPair array which will be used to pass the data to the database:
        //The data to be passed to the database is the values specified in the
        //persistentproperties and the inputMessage if logMessage flag is set to true.
        dataForDB = new NVPair[totalNoOfColumns];
        String comp_val = null;
        String loc_val = null;
        String default_val = null;
        String opt_val = null;

        //Get data from the persistent properties.
        for ( int colNo = 1; colNo <= noOfColumns ; colNo++ )
        {
          comp_val = (String)adapterProperties.get (COMPONENT_VAL + colNo);
          loc_val = (String)adapterProperties.get (LOCATION_VAL + colNo);
          default_val = (String)adapterProperties.get (DEFAULT_VAL + colNo);
          opt_val = (String)adapterProperties.get (OPTIONAL_VAL + colNo);

          if ( Debug.isLevelEnabled ( Debug.DB_STATUS) )
            Debug.log ( Debug.DB_STATUS, "XMLMessageLogger: Values are: " +
              COMPONENT_VAL + colNo + "=" + comp_val + " " +
              LOCATION_VAL + colNo + "=" + loc_val + " " +
              DEFAULT_VAL + colNo + "=" + default_val + " " +
              OPTIONAL_VAL + colNo + "=" + opt_val );

          //If a component (column) is optional, check if it exists first,
          //then try to use the default value for it, if any
          if (opt_val.equalsIgnoreCase("TRUE"))
          {
            boolean isvalueSet = false;
            if (loc_val != null)
            {
              //The loc_val currently might be consisted of more than one possible XML
              //paths separated by "SEPARATOR". The getAppropriateXMLPath, gets back the XML
              //path that is applicable to the current XML or null if none is applicable.
              //So we need to check for null again.
              loc_val = getAppropriateXMLPath (inputParser, loc_val);
              if (loc_val != null)
              {
                if (inputParser.exists(loc_val) )
                {
                  try
                  {
                    dataForDB[colNo-1] = new NVPair( comp_val, inputParser.getValue(loc_val) );
                    isvalueSet = true;
                  }
                  catch (MessageParserException e)
                  {
                    Debug.log( Debug.ALL_ERRORS, "ERROR: XMLMessageLogger: " + LOCATION_VAL + colNo
                    + " for "+ COMPONENT_VAL + colNo + " is invalid, "+ e.getMessage());
                    throw new ProcessingException(e.getMessage());
                  }
                }//if exists
              }; //loc_val != null
              //else the component has no value in the XML document, but this is ok,
              //as the component is an optional element/attribute, so do nothing.

            }//if not null

            //the following "if" is used CURRENTLY only in case of setting some date value = SYSDATE,
            //basically supporting DBLog class feature.
            //For that the component is <colname>, optional = true, location = null, default = SYSDATE
            //This can also be used to set any other component's value whose value is known before hand,
            //and not extracted from the XML document.
            if ((!isvalueSet) && (default_val != null))
            {
              dataForDB[colNo-1] = new NVPair (comp_val, default_val);
                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log (Debug.MSG_STATUS, "XMLMessageLogger: Setting default value "+comp_val+ " " + default_val);
            }
            else
            if (!isvalueSet)
            //set null value anyways
            {
              dataForDB[colNo-1] = new NVPair (comp_val, "NULL");
            }
          }//for opt_val = true
          else
          {
            try
            {
              //Check if location specified for component is not null
              if (loc_val == null)
              {
                Debug.log(Debug.ALL_ERRORS, "ERROR: XMLMessageLogger: " + LOCATION_VAL + colNo
                + " for " + comp_val + " is null");
                throw new ProcessingException("ERROR: XMLMessageLogger: " + LOCATION_VAL + colNo
                + " for " + comp_val + " is null");
              }
                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log(Debug.MSG_STATUS, "XMLMessageLogger: Setting values for "+comp_val );
              //The loc_val currently might be consisted of more than one possible XML
              //paths separated by "SEPARATOR". The getAppropriateXMLPath, gets back the XML
              //path that is applicable to the current XML or null if none is applicable.
              //So we need to check for null again.              
              loc_val = getAppropriateXMLPath (inputParser, loc_val);
              if (loc_val == null)
              {
                Debug.log(Debug.ALL_ERRORS, "ERROR: XMLMessageLogger: " + LOCATION_VAL + colNo
                + " for " + comp_val + " cannot be located. Please check XML message and/or properties");
                throw new ProcessingException("ERROR: XMLMessageLogger: " + LOCATION_VAL + colNo
                + " for " + comp_val + " cannot be located. Please check XML message and/or properties");
              }
              dataForDB[colNo-1] = new NVPair( comp_val, inputParser.getValue(loc_val) );
            }
            catch (MessageParserException e)
            {
              Debug.log( Debug.ALL_ERRORS, "ERROR: XMLMessageLogger: " +
                      e.getMessage());
              throw new ProcessingException(e.getMessage());
            }
          }//else
        }//for
        //Log values not specified in the persistent properties, but are input to
        //this class and logging is decided by a property flag.
        //Currently only input message is logged
        if (logMessage.equalsIgnoreCase ("TRUE"))
          dataForDB[noOfColumns] = new NVPair( MESSAGE, inputMessage );

        printValues();
    }

   /**
    * Check values to be inserted into the database. The values can be nulls
    * if the fields are optional
    *
    */
    private void printValues ()
    {
    // Check for null values:
      for (int i = 0; i < dataForDB.length ; i++)
      {
          if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "XMLMessageLogger.extractMessageData: " +
                   "Extracted NAME = [" + dataForDB[i].name + "], " +
                   "VALUE = [" + (String) dataForDB[i].value + "] from the input message");
      }//for

      if (logMessage.equalsIgnoreCase ("TRUE"))
          if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log(Debug.MSG_STATUS,"XMLMessageLogger.extractMessageData: " +
                  "Length of  message = " + inputMessage.length() + " bytes.");

     }//printValues

   /**
    * Get the appropriate XML path for this COMPONENT in this XML document from
    * the list of possible paths specified in the corresponding LOCATION
    * @param parser XMLMessageParser for the input XML message
    * @param pathList The list of possible XML paths for the corresponding COMPONENT
    * @return String The actual XML path from pathList for the corresponding COMPONENT
    * or null if no match found.
    */
    private String getAppropriateXMLPath ( XMLMessageParser parser, String pathList )
    {
      String retValue = null;
      StringTokenizer strTok = new StringTokenizer (pathList, separator, false);
      while ( strTok.hasMoreTokens ( ) )
      {
        String tempPath = strTok.nextToken ( );
        if ( parser.exists ( tempPath ) )
        {
          retValue = new String ( tempPath );
          return retValue;
        };//if
      };//while
      return null;
    };//getAppropriateXMLPath
    
     /**
     * Remove the java sql prepared statement from the cache
     */
     private void clearPreparedStatement() {

        //Total number of columns of the database to be updated = totalNoOfColumns
        NVPair[] dataForTemplate = new NVPair[totalNoOfColumns];

        //For the columns specified in the persistentproperty table, get column names
        for ( int colNo = 1; colNo <= noOfColumns; colNo++ )
        {
          dataForTemplate [colNo -1] =
          new NVPair ((String)adapterProperties.get (COMPONENT_VAL + colNo), DUMMY);
        }

        //For the columns not specified in the persistent property table, get column names
        //Currently only other column is the message column
        if (logMessage.equalsIgnoreCase ("TRUE"))
          dataForTemplate[noOfColumns] = new NVPair(MESSAGE,  DUMMY);

         if ( Debug.isLevelEnabled ( Debug.DB_DATA) )
            Debug.log(Debug.DB_DATA, "XMLMessageLogger: clearing prepared statement ");

        try {

            DBLog.clearPreparedStatement(tableName, dataForTemplate);

            if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
                Debug.log(Debug.DB_STATUS, "XMLMessageLogger: Cleared prepared statement.");

        } catch (DatabaseException e) {
            Debug.log(Debug.ALL_ERRORS, "ERROR: XMLMessageLogger: " +
                      "Could not clear prepared statement " + e.toString());
        }
    }

    //Following method is used only for testing purposes to generate context
    private MessageProcessorContext getContext () throws ProcessingException
    {
        MessageProcessorContext context = new MPC();
        return context;
    }

    //Following class is used only for testing purposes
    public class MPC extends MessageProcessorContext
    {
      public MPC () throws ProcessingException
      {
        try
        {
            if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
                Debug.log( Debug.DB_STATUS, "XMLMessageLogger: -----Initializng MessageProcessorContext-----");
          setDBConnection (DBConnectionPool.getInstance().acquireConnection( ));
          getDBConnection().setAutoCommit(true);
        }
        catch ( Exception dbe)
        {
          Debug.log( Debug.ALL_ERRORS, "ERROR: XMLMessageLogger: database error, can not get connection " + dbe);
        }
      }
    }

    /**
     * Main method for testing purposes.
     * @param args The arguments to this class for testing purposes. The number of
     * arguments should be 6 and are <KEY> <TYPE> <Input xml document file name as
     * test.xml, should be located in current directory> <DBNAME> <DBUSER> <DBPASSWD>
     */
    public static void main (String[] args) {

        Debug.enableAll();
        Debug.showLevels();

        if (args.length != 6)
        {
          Debug.log (Debug.ALL_ERRORS, "XMLMessageLogger: USAGE:  "+
          "jre -nojit -classpath %CLASSPATH%  "+
          "com.nightfire.adapter.messageprocessor.XMLMessageLogger "+
          "COVAD_ORDER OUT_LOGGER dsl.xml jdbc:oracle:thin:@192.168.10.158:1521:orcl b36nt b36nt");
        }
        String ilecAndOSS = args[0];
        String propertyType = args[1];
        String MESSAGEFILE = args[2];


        // Read in message file and set up database:
        FileCache messageFile = new FileCache();
        String inputMessage = null;
        try {

            inputMessage = messageFile.get(MESSAGEFILE);

        } catch (FrameworkException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "FileCache failure: " + e.getMessage());
        }

        try {

            DBInterface.initialize(args[3], args[4], args[5]);

        } catch (DatabaseException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "Database initialization failure: " + e.getMessage());
        }

        XMLMessageLogger logger = new XMLMessageLogger();

        try {

            logger.initialize(ilecAndOSS, propertyType);

        } catch (ProcessingException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "call to initialize failed with message: " + e.getMessage());
        }

        MessageProcessorContext context = null;

        try {
            context = logger.getContext();
        }
        catch (Exception e) {
            Debug.log(Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "Can't create context: " + e.getMessage());
        }

        try {
            logger.execute(context,inputMessage);

        }
        catch (MessageException e) {
            Debug.log(Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "call to process failed with message: " + e.getMessage());
        }
        catch (ProcessingException e) {
            Debug.log(Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "call to process failed with message: " + e.getMessage());
        }

        Debug.log( Debug.DB_STATUS, "XMLMessageLogger.main: clearing prepared statement");
        try {
            logger.execute( null, null );
             
        }
        catch (MessageException e) {

            Debug.log( Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "null call to process failed with message: " + e.getMessage());
        }
        catch (ProcessingException e) {

            Debug.log( Debug.MAPPING_ERROR, "XMLMessageLogger.main: " +
                      "null call to process failed with message: " + e.getMessage());
        }

    }


}

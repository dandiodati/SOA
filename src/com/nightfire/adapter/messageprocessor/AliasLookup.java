/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.messageprocessor;

import org.w3c.dom.*;
import java.util.*;
import java.sql.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.spi.common.driver.*;

/**
 * This class is used to look up mappings in the database for a node value in the
 * input XML message and to replace the current node value with the mapped
 * value. If either the xml node to be mapped does not exist or if the database mapped
 * value does not exist, the xml message is returned unmodified.
 */
public class AliasLookup extends MessageProcessorBase
{
    /**
     * Cache for storing results of query executed so far
     */
    private static Map map = new HashMap ( );

    /**
     * XML node name in "." delimited format, whose value is to be replaced
     */
    private String xmlNodeName = null;

    /**
     * Database table to fetch values from
     */
    private String tableName = null;

    /**
     * Column containing the current xml node's value
     */
    private String currentValueColumn = null;

    /**
     * Column containing the replacement value
     */
    private String newValueColumn = null;

    /**
     * Location of XML message to process
     */
    private String inputMessageLocation = null;

    private static final String INPUT_MESSAGE_LOCATION_PROP = "INPUT_MESSAGE_LOCATION";
//    private static final String NEXT_PROCESSOR_NAME_PROP = "NEXT_PROCESSOR_NAME";
    private static final String XML_NODE_NAME_PROP = "XML_NODE_NAME";
    private static final String TABLE_NAME_PROP = "TABLE_NAME";
    private static final String CURRENT_VALUE_COLUMN_PROP = "CURRENT_VALUE_COLUMN";
    private static final String NEW_VALUE_COLUMN_PROP = "NEW_VALUE_COLUMN";

    /**
     * Initializes this object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        super.initialize(key, type);

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "AliasLookup: Initializing.....");

        //Initialize a string buffer to contain all errors
        StringBuffer strBuf = new StringBuffer();

        inputMessageLocation = getPropertyValue ( INPUT_MESSAGE_LOCATION_PROP );
        //inputMessageLocation can be null. The MessageProcessorBase "get" methods
        //handle the null location appropriately. Null implies that the value is
        //is the default object in the MessageObject passed into the process method.

        if ( toProcessorNames == null )
            strBuf.append ( "Property value for NEXT_PROCESSOR_NAME is null\n" );

        tableName = getRequiredPropertyValue ( TABLE_NAME_PROP, strBuf );
        if ( tableName != null )
          tableName = tableName.toUpperCase ( );

        xmlNodeName = getRequiredPropertyValue ( XML_NODE_NAME_PROP, strBuf );

        currentValueColumn = getRequiredPropertyValue ( CURRENT_VALUE_COLUMN_PROP, strBuf );
        if ( currentValueColumn != null )
          currentValueColumn = currentValueColumn.toUpperCase ( );

        newValueColumn = getRequiredPropertyValue ( NEW_VALUE_COLUMN_PROP, strBuf );
        if ( newValueColumn != null )
          newValueColumn = newValueColumn.toUpperCase ( );

        if (strBuf.length()!=0)
        {
          Debug.log( Debug.ALL_ERRORS, "ERROR: AliasLookup.initialize: " + strBuf.toString( ) );
          throw new ProcessingException ("ERROR: AliasLookup.initialize: " + strBuf.toString( ) );
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "AliasLookup.initialize: Properties found:");

    }//initialize ( key, type )


    /**
     * Process the input message (DOM or String) and (optionally) return
     * a name / value pair.
     *
     * @param  messageObject  Input message object containing the message to process.
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
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject messageObject )
    throws MessageException, ProcessingException
    {

        //Usual null round processing of Driver. This is to check if all the
        //processors are done with their processing.
        if ( messageObject == null )
        {
            return null;
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "AliasLookup: Processing the message... ");

        Document inputDocument = null;

        try
        {
            inputDocument = getDOM ( inputMessageLocation, mpcontext, messageObject );
        }
        catch ( ProcessingException e )
        {
            Debug.log (Debug.ALL_ERRORS, "ERROR: AliasLookup: " +
                                       "The input must be a String/Document. " + e.getMessage ( ) );

            throw new ProcessingException("ERROR: AliasLookup: " +
                                       "The input must be a String/Document. " + e.getMessage ( ) );
        }

        Document resultDocument = setAliasValue ( inputDocument );

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Finished processing" );

        //Place the resultant DOM message into MessageObject. Downstream processors
        //do not have to reparse message.
        set ( inputMessageLocation, mpcontext, messageObject, resultDocument );
        return formatNVPair ( messageObject );

     }//process

    /**
     * Look up the mappings in the database and set the new value in the XML message
     *
     * @param  doc Document Represents the message
     *
     * @return Document Modified XML message
     *
     * @exception ProcessingException when processing fails
     *
     * @exception MessageException when processing fails
     */
    private Document setAliasValue ( Document doc )
                            throws ProcessingException, MessageException
    {

      XMLMessageParser parser = null;
      try {
          parser = new XMLMessageParser ( doc );
      } catch ( MessageParserException e )
      {
          Debug.log ( Debug.ALL_ERRORS, "AliasLookup: " + e.getMessage ( ) );
          throw new ProcessingException ( "AliasLookup: " + e.getMessage ( ) );
      }

      //If xml node whose mapped value in the database is to be fetched,
      //does not exist, then return unmodified message
      if ( ! ( parser.exists ( xmlNodeName ) ) )
      {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Returning unmodified message because node [" +
            xmlNodeName + "] does not exist" );
          return doc;
      }

      String currentValue = null;

      try
      {
        currentValue = ( parser.getValue ( xmlNodeName ) ).toUpperCase ( );
      }
      catch ( MessageParserException mpe )
      {
        Debug.log (Debug.ALL_ERRORS, "ERROR: AliasLookup: " + mpe.getMessage ( ) );

        throw new MessageException ("ERROR: AliasLookup: " + mpe.getMessage ( ) );
      }
      String hashKey = createHashKey ( currentValue );

      //Check if the cache has this hashKey with a value associated with it.
      String newValue = ( String ) map.get ( hashKey );

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "AliasLookup: Value for hashKey [" + hashKey +
                  "] is [" + newValue + "]" );

      //Hashkey value not in cache, so fetch current value of xml node. Get its
      //replacement value from the database and cache it
      if ( newValue == null )
      {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "AliasLookup: Getting value for " + xmlNodeName +
                  " from " + tableName );


        newValue = getValueFromDB ( currentValue );
        if ( newValue != null )
        {
          //Cache result
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log (Debug.MSG_STATUS, "AliasLookup: Caching value for hashKey [" + hashKey +
                  "] = [" + newValue + "]" );

          synchronized ( map )
          {
              map.put ( hashKey, newValue );
          }
        }
        //else nothing to cache
      }
      //Set new value in the XML now
      if ( newValue != null )
      {
        XMLMessageGenerator generator = null;
        try
        {
          generator = ( XMLMessageGenerator )parser.getGenerator ( );
        }
        catch ( MessageParserException mpe )
        {
          Debug.log (Debug.ALL_ERRORS, "ERROR: AliasLookup: " +
                  mpe.getMessage ( ) );
          throw new ProcessingException("ERROR: AliasLookup: " +
                  mpe.getMessage ( ) );
        }

        try
        {
          generator.setValue ( xmlNodeName, newValue );
        }
        catch ( MessageGeneratorException mge )
        {
          Debug.log (Debug.ALL_ERRORS, "ERROR: AliasLookup: " +
                  "Unable to set value of [" + xmlNodeName + "] in input XML: "+ mge.getMessage ( ) );

          throw new ProcessingException("ERROR: AliasLookup: " +
                  "Unable to set value of [" + xmlNodeName + "] in input XML: "+ mge.getMessage ( ) );
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Returning modified message");

        return generator.getDocument ( );
      }
      else
      //No value to be set in the XML message. Return original message as Document
      {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Returning original message");
        return doc;
      }
    }//setValue

    /**
     * Create a hashKey to lookup in the cache
     *
     * @return Hashkey composed from the input value, tableName,
     * currentValueColumn, newValueColumn
     *
     */
    private String createHashKey ( String value )
    {
      String CONCATENATOR = "_";
      String key = value
                   + CONCATENATOR + tableName
                   + CONCATENATOR + currentValueColumn
                   + CONCATENATOR + newValueColumn;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Hashkey being used is [" + key + "]" );

      return key;

    }//createHashKey

    /**
     * Get new value for XML node xmlNodeName from database table tableName
     *
     * @param value Current value of xml node to be used for looking up in the database
     *
     * @return String New value for the xml node from the database or null if no
     *         replacement value found.
     *
     */
    private String getValueFromDB ( String value ) throws ProcessingException
    {
      Connection dbConn = null;
      PreparedStatement stmnt = null;
      ResultSet resultSet = null;
      String newValue = null;

      try
      {
        dbConn = DBInterface.acquireConnection ( );
        String sqlQuery = "SELECT " + newValueColumn +
                        " FROM " + tableName +
                        " WHERE UPPER(" + currentValueColumn +
                        ") = '" + value + "'";

          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Query created is " + sqlQuery );

        synchronized ( dbConn )
        {
          stmnt = DBInterface.getPreparedStatement ( dbConn, sqlQuery );
          resultSet = DBInterface.executeQuery ( stmnt );

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log ( Debug.MSG_STATUS, "AliasLookup: Finished executing query" );
                Debug.log ( Debug.MSG_STATUS, "AliasLookup: Getting result of query now" );
            }

          boolean resultStatus = resultSet.next ( );

          if ( resultStatus == false )
          //No matching row found. Nothing to do.
          {
              if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log ( Debug.MSG_STATUS, "AliasLookup: No matching value found for [" +
                xmlNodeName + "]'s value [" + value + "] in table [" + tableName + "]" );
          }
          else
          {
            //Got one matching row
              if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log ( Debug.MSG_STATUS, "AliasLookup: Got matching result" );
                newValue = resultSet.getString ( newValueColumn );

            //Found more than one matching rows for the query specified. This is invalid.
            if ( resultSet.next ( ) == true )
            {
              Debug.log (Debug.ALL_ERRORS, "ERROR: AliasLookup: " +
                  "Found more than one matching value for [" + xmlNodeName +
                  "]'s value [" + value + "] in table [" + tableName + "]" );

              throw new ProcessingException("ERROR: AliasLookup: " +
                  "Found more than one matching value for [" + xmlNodeName +
                  "]'s value [" + value + "] in table [" + tableName + "]" );
            }//if
          }//Matching result

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log ( Debug.MSG_STATUS, "AliasLookup: Releasing resources");

          stmnt.close ( );
          resultSet.close ( );
        }//synchronized

          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Releasing database connection");

        DBInterface.releaseConnection ( dbConn );
      }//try
      catch ( SQLException e )
      {
        Debug.log ( Debug.ALL_ERRORS, "ERROR: AliasLookup: " + e.toString () );
        throw new ProcessingException ( "ERROR: AliasLookup: " + e.toString () );
      }
      catch ( DatabaseException de )
      {
        Debug.log ( Debug.ALL_ERRORS, "ERROR: AliasLookup: " + de.toString () );
        throw new ProcessingException ( "ERROR: AliasLookup: " + de.toString () );
      }
      finally
      {
          try
          {
              if ( resultSet != null )
                  resultSet.close ( );
              if ( stmnt != null )
                  stmnt.close ( );
          }
          catch ( SQLException sqle )
          {
              Debug.log( Debug.ALL_ERRORS, "AliasLookup: " + DBInterface.getSQLErrorMessage(sqle) );
          }
      }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "AliasLookup: Returning value [" + newValue + "]" );

      return newValue;

    }//getValueFromDB

}//end of Class

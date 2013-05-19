/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.adapter.messageprocessor;


import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.adapter.util.*;
import com.nightfire.spi.common.driver.*;


/**
 * Strips white space from XML structure to make them more easily consumable 
 * by maps.
 */
public class XMLCompactorAdapter extends MessageProcessorBase
{

    private static String COMPACT_ATTRIBUTE_VALUES_TAG = "COMPACT_ATTRIBUTE_VALUES";
    private boolean compactAttributeValue = true;

    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     * @exception ProcessingException Thrown when Initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize(key, type);

        String compactAttributeValueString  = (String) adapterProperties.get(COMPACT_ATTRIBUTE_VALUES_TAG);
        if(null != compactAttributeValueString)
        {
            compactAttributeValue = Boolean.valueOf ( compactAttributeValueString  ).booleanValue();
        }

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "XMLCompactorAdapter: Compact Attribute Nodes [" + compactAttributeValue +"]");
    }


    /**
     * Process the input DOM object-tree containing SQL operation.
     *
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  Result of SQL operation in DOM object-tree form.
     *
     * @exception  MessageException  Thrown if bad message
     *
     * @exception  ProcessingException Thrown if processing fails.
     */
    public synchronized NVPair[] execute ( MessageProcessorContext mpcontext, Object input ) 
                                                                     throws MessageException,
                                                                           ProcessingException
    {
        if(input == null)
        {
            return null;
        }
        if ( !(input instanceof String) )
        {
            throw new ProcessingException( "ERROR: XMLCompatorAdapter: Invalid input type [" +
                                        input.getClass().getName() + "]." );
        }
        XMLStringCompactor compactor = new XMLStringCompactor(compactAttributeValue);
        String ret = compactor.compact(input);
        return formatNVPair(ret);
    }

    //Following method is used only for testing purposes to generate context
    private MessageProcessorContext getContext ()
    {
        MessageProcessorContext context = null;
        try
	{
            context = new MPC();
        }
        catch(Exception e)
	{
            Debug.log(Debug.ALL_ERRORS, e.getMessage());
        }
        return context;
    }

    //Following class is used only for testing purposes
    public class MPC extends MessageProcessorContext
    {
      public MPC () throws ProcessingException
      {
        try
        {
            if (Debug.isLevelEnabled(Debug.DB_STATUS))
                Debug.log( Debug.DB_STATUS, "XMLCompactorAdapter: -----Initializng MessageProcessorContext-----");
          setDBConnection (DBConnectionPool.getInstance().acquireConnection( ));
          getDBConnection().setAutoCommit(true);
        }
        catch ( Exception dbe)
        {
          Debug.log( Debug.DB_ERROR, "ERROR: XMLCompactorAdapter: database error, can not get connection " + dbe);
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

    /* Following is a sample command to execute this program
    jre -nojit -classpath %CLASSPATH% com.nightfire.adapter.messageprocessor.XMLCompactorAdapter
    COVAD_ORDER OUT_LOGGER dsl.xml jdbc:oracle:thin:@192.168.10.158:1521:orcl
    b36nt b36nt
    */

        Debug.enableAll();
        Debug.showLevels();

        if (args.length != 6)
        {
          Debug.log (Debug.ALL_ERRORS, "XMLCompactorAdapter: USAGE:  "+
          "jre -nojit -classpath %CLASSPATH%  "+
          "com.nightfire.adapter.messageprocesseor.XMLCompactorAdapter "+
          "COVAD_ORDER OUT_LOGGER dsl.xml jdbc:oracle:thin:@192.168.10.158:1521:orcl b36nt b36nt");
        }
        String ilecAndOSS = args[0];
        String propertyType = args[1];
        String MESSAGEFILE = args[2];


        // Read in message file and set up database:
        FileCache messageFile = new FileCache();
        String inputMessage = null;
//        Document inputDom = null;
        try {

            inputMessage = messageFile.get(MESSAGEFILE);

        } catch (FrameworkException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLCompactorAdapter.main: " +
                      "FileCache failure: " + e.getMessage());
        }

        try {

            DBInterface.initialize(args[3], args[4], args[5]);

        } catch (DatabaseException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLCompactorAdapter.main: " +
                      "Database initialization failure: " + e.getMessage());
        }

        XMLCompactorAdapter adapter = new XMLCompactorAdapter();

        try {

            adapter.initialize(ilecAndOSS, propertyType);

        } catch (ProcessingException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLCompactorAdapter.main: " +
                      "call to initialize failed with message: " + e.getMessage());
        }

        MessageProcessorContext context = adapter.getContext();

        try {
            adapter.execute(context,inputMessage);

        }
        catch (MessageException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLCompactorAdapter.main: " +
                      "call to process failed with message: " + e.getMessage());
        }
        catch (ProcessingException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLCompactorAdapter: " +
                      "call to process failed with message: " + e.getMessage());
        }
    }
}

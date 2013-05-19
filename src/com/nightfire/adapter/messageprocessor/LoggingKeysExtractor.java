/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/LoggingKeysExtractor.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;
import java.sql.*;
import java.text.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;


/**
 * This is a generic message-processor for extracting logging keys.
 * 
 */
public class LoggingKeysExtractor extends MessageProcessorBase
{
    /**
     * Property indicating where the keys locate.
     */
    public static final String LOGGING_KEY_LOCATION_PROP = "LOGGING_KEY_LOCATION";

    /**
     * Property that separates the logging items.
     */
    public static final String ITEM_DELIMITER_PROP = "ITEM_DELIMITER";

    /**
     * Property that separates rows.
     */
    public static final String ROW_DELIMITER_PROP = "ROW_DELIMITER";


    private String inputLocation = null;
    private String itemDelimiter = null;
    private String rowDelimiter = null;
    
    
    /**
     * Constructor.
     */
    public LoggingKeysExtractor ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating message-processor: LoggingKeysExtractor." );
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );
        
        // Get configuration properties specific to this processor.
        Debug.log( Debug.SYSTEM_CONFIG, "LoggingKeysExtractor: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );
        
        inputLocation = getPropertyValue( LOGGING_KEY_LOCATION_PROP );
        

        itemDelimiter = getRequiredPropertyValue( ITEM_DELIMITER_PROP, errorBuffer );
        Debug.log(Debug.SYSTEM_CONFIG, "LoggingKeysExtractor: itemDelimiter ---->" + itemDelimiter);
        
        rowDelimiter = getRequiredPropertyValue( ROW_DELIMITER_PROP, errorBuffer );

        Debug.log(Debug.SYSTEM_CONFIG, "LoggingKeysExtractor: rowDelimiter---->" + rowDelimiter);

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "LoggingKeysExtractor: Initialization done." );
    }


    /**
     * Extract data values from the context/input, and use them to
     * insert a row into the configured database table.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
        throws MessageException, ProcessingException 
    {
        if ( inputObject == null )
            return null;

        Debug.log( Debug.MSG_STATUS, "LoggingKeysExtractor: processing ... " );
        
        if(toProcessorNames == null)
        {
            // Make sure this is at least processor to receive results.
            String errMsg = "ERROR: No next processors available.";
            Debug.log(null, Debug.ALL_ERRORS, errMsg);
            throw new ProcessingException(errMsg);
        }
        
        //else continue...

        // Separating the message.
        String[] rows = separateMessage( mpContext, inputObject );
        
        int nRows = rows.length;
        int processorsNo = toProcessorNames.length;
        NVPair[] contents = new NVPair [ nRows * processorsNo];

        for(int i=0; i<nRows; i++)
        {
            MessageObject mObj = extractLoggingKeys(rows[i]);

            // Sending the message objects to the next processors.
            for (int j = 0; j < processorsNo; j++)
            {

                contents[(i*(processorsNo))+j] = new NVPair ( toProcessorNames[j], mObj );
                Debug.log(null, Debug.MSG_STATUS, "NEXT PROCESSOR-->"+toProcessorNames[j]+"\n"+
                          "MESSAGE OBJECT CONTENT------------>"+mObj.describe());
            }

        }

        return contents;

    }

    
    /**
     * Separate the message into rows.
     *
     * @param mpcontext The message context.
     * @param intputObject The input object.
     *
     * @return The rows that is contained in the logging message.
     *
     * @exception MessageException  thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private String[] separateMessage ( MessageProcessorContext context, MessageObject inputObject )
    throws ProcessingException, MessageException
    {
        Debug.log (Debug.MSG_STATUS, "Separating messages..." );

        String input = getString( inputLocation, context, inputObject);

        try
        {
            
            NFStringTokenizer rowSt = new NFStringTokenizer(input, rowDelimiter);

            int nRows = rowSt.countTokens();
            Debug.log(Debug.MSG_STATUS, "Total number of rows --> [" + nRows + "]...");

            // Skipping empty rows since empty rows will return 1 token.
            ArrayList realRows = new ArrayList();
            
            for (int i=0; i< nRows; i++)
            {
                String theRow = rowSt.nextToken();
                if (StringUtils.hasValue(theRow))
                {
                    realRows.add(theRow);
                }
            }
        
            nRows = realRows.size();
            Debug.log(Debug.MSG_STATUS, "After skipping empty rows, the number of rows --> [" + nRows + "]...");

            String[] rows = new String[nRows];
            realRows.toArray(rows);

            return rows;
        }
        catch (FrameworkException fe)
        {
            throw new ProcessingException("ERROR: Failed to extract tokens.\n" + fe.getMessage());
        }
        
    }


    /**
     * Extracts logging keys and store them in the message object.
     *
     * @param row The row that contains logging key.
     *
     * @return The message object that contains logging keys.
     *
     * @exception ProcessingException  thrown if required value can't be found.
     */
    private MessageObject extractLoggingKeys ( String row ) throws ProcessingException
    {
        Debug.log (Debug.MSG_STATUS, "Extracting logging keys from row [" + row + "] by tokens [" + itemDelimiter + "] ..." );

        try
        {
            NFStringTokenizer st = new NFStringTokenizer(row, itemDelimiter);

            int nTokens = st.countTokens();

            if ( nTokens % 2 != 0 )
            {
                throw new ProcessingException("ERROR: Row [" + row + "] doesn't contain even items.");
            }
        
            int nPairs = nTokens / 2;

            MessageObject messageObject = new MessageObject();
        
            for (int i=0; i< nPairs; i++)
            {
                String key = st.nextToken();
                String value = st.nextToken();

                messageObject.set(key, value);
            
            }
        
            return messageObject;
        
        }
        catch (FrameworkException fe)
        {
            throw new ProcessingException("ERROR: Failed to extract tokens.\n" + fe.getMessage());
        }
        
    }

}



/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/FormattingObjectMessageProcessor.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.transformer.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * Transform an incoming XML Format Object in the configured
 * ouput document type.
 */
public class FormattingObjectMessageProcessor extends MessageProcessorBase
{
    // defines the location of the input message that needs to be transformed
    // if this property is null that the message object is used.
    private final static String INPUT_LOC_PROP = "INPUT_LOCATION";

    // defines the output location of the transformed message
    // if this property is null then it is returned as a  message object.
    private final static String OUTPUT_LOC_PROP = "OUTPUT_LOCATION";

    
    /* Format to which to ouput the incoming FO XML.
     *
     * Valid format type Strings are: 
     * <br/>
     * <ul>
     * <li> PDF </li>
     * <li> AWT </li>
     * <li> MIF </li>
     * <li> XML </li>
     * <li> PRINT </li>
     * <li> PCL </li>
     * <li> PS </li>
     * <li> TXT </li>
     * <li> SVG </li>
     * </ul>
     */
    private final static String OUTPUT_FORMAT_PROP = "OUTPUT_FORMAT";

    // Name of the file to which to log FOP library logging. 
    private final static String FOP_LOG_FILE_NAME_PROP = "FOP_LOG_FILE_NAME";

    // Boolean flag to enable FOP library logging. 
    private final static String ENABLE_FOP_LOGGING_PROP = "ENABLE_FOP_LOGGING";

    private String inputLoc = null;
    private String outputLoc = null;
    private String outputFormat = null;
    private String fopLogFileName = null;
    private boolean enableFOPLogging = false;

    /**
     * Transform an incoming XML Format Object in the configured
     * ouput document type.
     * 
     * Properties:
     *
     *   INPUT_LOC defines the location of the input file to transform (can be in the context or messageobject).
     *   It not defined then the message object is used.
     *
     *   OUTPUT_LOC defines the location for the transformed xml message (can be in the context or message object).
     *   It not defined then the transformed xml is returned as a NVPair.
     *
     *   OUTPUT_FORMAT defines the desired document format for output.
     * 
     *   FOP_LOG_FILE_NAME defines the log file to which to write FOP specific debugging.
     * 
     *   ENABLE_FOP_LOGGING true or false to enable FOP specific debug logging.
     *
     * @param  context  The  message context.
     *
     * @param  input  Input message object to process.
     *
     * @return  An array of name-value pair objects, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {
        if (msgObj == null)
            return null;

        // note this will only let us support an input of xml, and
        // not anything else.
        //
        String input = getString(inputLoc, context, msgObj);

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        try
        {
            if(enableFOPLogging)
            {
                FormattingObjectsUtil.formatXML(input, result, outputFormat, fopLogFileName);        
            }
            else
            {
                FormattingObjectsUtil.formatXML(input, result, outputFormat);        
            }
        }
        catch(MessageException me)
        {
            throw me;
        }
        catch(FrameworkException fe)
        {
            throw new ProcessingException(fe);
        }

        // If the outputLoc is null, calling set() would push the
        // output into the messageObj, which is the original
        // input message and may still be referenced by other processors
        // which have not yet executed.

        // We only want to push the result into the input message if the
        // processor is specifically configured to with the output loc
        // property, not by default.
        if(null != outputLoc)
        {
            Debug.log(Debug.MSG_STATUS, "Setting result of FO transformation at OUTPUT_LOCATION");
            set(outputLoc, context, msgObj, result.toByteArray());
        }
        else
        {
            Debug.log(Debug.MSG_STATUS, "Returning result of FO transformation to the next processors.");
            msgObj = new MessageObject(result.toByteArray());
        }
        return( formatNVPair( msgObj ) );
    }


    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
  public void initialize ( String key, String type ) throws ProcessingException
  {
     super.initialize(key,type);

      inputLoc = getPropertyValue(INPUT_LOC_PROP);
      outputLoc = getPropertyValue(OUTPUT_LOC_PROP);
      fopLogFileName = getPropertyValue(FOP_LOG_FILE_NAME_PROP);
      String enableFOPLoggingString = getPropertyValue(ENABLE_FOP_LOGGING_PROP);
      if(StringUtils.hasValue(enableFOPLoggingString))
      {
          try
	  {
              enableFOPLogging = getBoolean(enableFOPLoggingString);
          }
          catch(MessageException e)
          {
              throw new ProcessingException("Invalid boolean value for [" + 
                                            ENABLE_FOP_LOGGING_PROP + "] property. [" + 
                                            enableFOPLoggingString + "] " + e.getMessage());
          }
      }
      outputFormat = getRequiredPropertyValue(OUTPUT_FORMAT_PROP);
      // verify that the format is valid. 
      try
      {
          FormattingObjectsUtil.getIntFormatType(outputFormat);
      }
      catch(FrameworkException e)
      {
          throw new ProcessingException("Invalid value for [" + ENABLE_FOP_LOGGING_PROP + "] property. [" + 
                                        outputFormat + "] " + e.getMessage());
      }
  }

}




/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //adapter/main/com/nightfire/adapter/messageprocessor/XSLTMessageProtocolAdapter.java#1 $
 */

package com.nightfire.adapter.messageprocessor;


import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.transformer.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * Default message protocol adapter for translating XML to DOM Document, and vice-versa.
 *
 *
 */
public class XSLTMessageProtocolAdapter extends MessageProcessorBase
{
    /**
     * Directory into which XSL input and output XML messages should be written to (optional).
     */
    public static final String XSL_IO_LOG_DIR_PROP = "XSL_IO_LOG_DIR";

    /*
     * Boolean flag, which if true, will trigger this MessageProcessor to
     * treat the name of the StyleSheet as a Compiled Translet class name
     * and apply it appropriately
     */
    private final static String STYLE_SHEET_IS_COMPILED_PROP = "STYLE_SHEET_IS_COMPILED";
    /*
     * name of the Style Sheet to use, or, if compiled, name of the Class
     * the Style Sheet was compiled to.
     */
    private final static String STYLE_SHEET = "STYLE_SHEET";
    private final static String STYLE_SHEET_LOCATION_PROP = "STYLE_SHEET_LOCATION";

    // defines the location of the input message that needs to be transformed
    // if this property is null that the message object is used.
    private final static String INPUT_LOC_PROP = "INPUT_LOCATION";

    // defines the output location of the transformed message
    // if this property is null then it is returned as a  message object.
    private final static String OUTPUT_LOC_PROP = "OUTPUT_LOCATION";

    // If set to true, it indicates that the transformation needs to be done via
    // String to String transformation, rather than DOM to DOM, even when
    // Document input is received.
    private final static String TRANSFORM_AS_STRING_PROP = "TRANSFORM_AS_STRING";
    /**
     * The archive containing the compiled stylesheet
     */
    private final static String XSL_JAR_FILE_PROP = "XSL_JAR_FILE";


    private String xslFileName = null;
    private boolean styleSheetIsCompiled = false;
    private boolean transformAsString = false;
    private String inputLoc = null;
    private String outputLoc = null;
    private String xslJar = null;

    /**
     * Processes the input message or context information and optionally returns
     * a value.
     * If a STYLE_SHEET property is defined then it uses it for the conversion, otherwise
     * it looks for STYLE_SHEET_LOCATION_PROP  where is will try to get the style sheet name.
     * If STYLE_SHEET_LOCATION is not defined then this processor acts a passthrough.
     * If the STYLE_SHEET_IS_COMPILED property is TRUE, the style sheet name will be treated
     * as the name of a compiled Translet class.
     *
     * Other properties:
     *   INPUT_LOC defines the location of the input file to transform (can be in the context or messageobject).
     *   It not defined then the message object is used.
     *
     *   OUTPUT_LOC defines the location for the transformed xml message (can be in the context or message object).
     *   It not defined then the transformed xml is returned as a NVPair.
     *
     * @param  context  The  message context.
     *
     * @param  msgObj  Input message object to process.
     *
     * @return  An array of name-value pair objects, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  ProcessingException  Thrown if processing fails due to system errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {
        if (msgObj == null)
            return null;

        if( !StringUtils.hasValue(xslFileName) )
        {
            String xslLocation = getPropertyValue( STYLE_SHEET_LOCATION_PROP );

            if( !StringUtils.hasValue(xslLocation) )
            {
               // if no style sheet or location was defined then act as a passthrough
               return formatNVPair( msgObj);

            }

            xslFileName = getString( xslLocation, context, msgObj);
        }



        XSLMessageTransformer t = null;
        String outputMethod = null;

        if ( StringUtils.hasValue(xslJar) ) {
            //this will evaluate using a class reloading cache

            String[] cp = { xslJar };
            t = MessageTransformerCache.getInstance().get(xslFileName, cp );
        }
        else
        {
            t = MessageTransformerCache.getInstance().get(xslFileName,
                                                styleSheetIsCompiled);
        }

        outputMethod = t.getOutputConfiguration(XSLMessageTransformer.OUTPUT_METHOD_PROP);
        if (!(outputMethod.equals(XSLMessageTransformer.OUTPUT_METHOD_XML)))
        {
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            {
                Debug.log( Debug.MSG_STATUS, "Output method for the Stylesheet is not XML, so transforming via String conversion");
            }
            transformAsString = true;
        }

		//Transforming the input based on the object type of the input message object
	    //whether the input is of type String or Document type.

		Object input = get(inputLoc, context, msgObj);
		Object result = null;

        if ( input instanceof String )
        {
            String msg = (String) input;
            result = t.transform(msg);
        }
        else if ( input instanceof Document )
        {
            Document msg = (Document) input;
            if (transformAsString)
            {
                result = t.transform(XMLLibraryPortabilityLayer.convertDomToString(msg));
            }
            else
                result = t.transform(msg);
        }

        if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
        {         
			String inStr = null, outStr = null;		
	
			if ( input instanceof Document )
            {
                inStr = XMLLibraryPortabilityLayer.convertDomToString((Document)input);
            }
            else if ( input instanceof String )
            {
                inStr = (String) input;
            }

			if ( result instanceof Document )
            {
                outStr = XMLLibraryPortabilityLayer.convertDomToString((Document)result);
            }
            else if ( result instanceof String )
            {
                outStr = (String) result;
            }

			Debug.log( Debug.MSG_DATA, "Input message:\n" + inStr + "\nXSL stylsheet ["
                       + xslFileName + "]\n" + "Resulting output:\n" + outStr );
        }
        // Log the input and output to file-system, if configured to do so.
    	logXSLTransformation( adapterProperties, result, input );

        // If the outputLoc is null, calling set() would push the
        // output into the messageObj, which is the original
        // input message and may still be referenced by other processors
        // which have not yet executed.

        // We only want to push the result into the input message if the
        // processor is specifically configured to with the output loc
        // property, not by default.
        if(null != outputLoc)
        {
            Debug.log(Debug.MSG_STATUS, "Setting result of XSL transformation at OUTPUT_LOCATION");
            set(outputLoc, context, msgObj, result);
        }
        else
        {
            Debug.log(Debug.MSG_STATUS, "Returning result of XSL transformation to the next processors.");
            msgObj = new MessageObject(result);
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

      //Get properties and transformer
      xslJar = getPropertyValue( XSL_JAR_FILE_PROP );
      
      if ( StringUtils.hasValue(xslJar) ) {
          File f = new File(xslJar);
          
          if ( !f.exists() || 
                !( xslJar.toLowerCase().endsWith(".jar") ) )
          {
              throw new ProcessingException("Value ["+xslJar+"] for property ["+
                                             XSL_JAR_FILE_PROP + "] does not exist " + 
                                             "or is not a .jar file.");
                                             
          }          
      }
              
          
      xslFileName  = getPropertyValue( STYLE_SHEET );
      String styleSheetIsCompiledString  = getPropertyValue(STYLE_SHEET_IS_COMPILED_PROP);
      if(StringUtils.hasValue(styleSheetIsCompiledString))
      {
          try
          {
              styleSheetIsCompiled = StringUtils.getBoolean(styleSheetIsCompiledString);
          }
          catch(FrameworkException e)
          {
              throw new ProcessingException("[" + STYLE_SHEET_IS_COMPILED_PROP +
                                            "] property could not be converted to a boolean value." +
                                            e.getMessage());
          }
      }


        String transformAsStringStr = getPropertyValue(TRANSFORM_AS_STRING_PROP);
        if(StringUtils.hasValue(transformAsStringStr))
        {
            try
            {
                transformAsString = StringUtils.getBoolean(transformAsStringStr);
            }
            catch(FrameworkException e)
            {
                throw new ProcessingException("[" + TRANSFORM_AS_STRING_PROP +
                                              "] property could not be converted to a boolean value." +
                                              e.getMessage());
            }
        }


      inputLoc = getPropertyValue(INPUT_LOC_PROP);
      outputLoc = getPropertyValue(OUTPUT_LOC_PROP);
  }


    /**
     * Utility to log XSL transformation input and output XML messages.
     *
     * @param  props  Configuration properties containing optional XSL
     *                logging directory location.
     * @param  xslOutput  The XML output resulting from XSL transformation.
     * @param  xslInput  The XML input to the XSL transformation.
     */
    private void logXSLTransformation ( Map props, Object xslOutput, Object xslInput )
    {
        if ( props == null )
            return;

        try
        {
            // Get the name of the directory to log the XSL inputs and outputs to.
            String dir = (String)props.get( XSL_IO_LOG_DIR_PROP );

            // If the directory was given, the user wants to log the XML values.
            if ( StringUtils.hasValue( dir ) )
            {
                String output = null, input = null;
                
                if ( xslOutput instanceof Document )
                   output = XMLLibraryPortabilityLayer.convertDomToString((Document)xslOutput);
                else if (xslOutput instanceof String )
                    output = (String) xslOutput;

                if ( xslInput instanceof Document )
                   input = XMLLibraryPortabilityLayer.convertDomToString((Document)xslInput);
                else if (xslInput instanceof String )
                    input = (String) xslInput;

                // Make sure the given directory ends with a path delimiter.
                if ( !dir.endsWith( "/" ) )
                    dir += "/";

                // Build the file names.
                String inFileName = null;
                String outFileName = null;

                // Use the current system time to construct a unique file name.
                Date d = new Date( );

                long id = d.getTime( );

                // Loop until we arrive at unique file names.
                do
                {
                    String idStr = String.valueOf( id );

                    inFileName = dir + "xslInput_" + idStr + ".xml";
                    File f = new File( inFileName );

                    // If the named file exists, file name wasn't unique, so increment id and try again.
                    if ( f.exists() )
                        id ++;
                    else
                    {
                        // We have a unique input file name, so construct the
                        // corresponding output file name and move on.
                        outFileName = dir + "xslOutput_" + idStr + ".xml";

                        break;
                    }
                }
                while ( true );

                // Write out the XSL input and output XML data to disk.
                FileUtils.writeFile( inFileName, input );
                FileUtils.writeFile( outFileName, output );
            }
        }
        catch ( Exception e )
        {
            // In general, this method is used for testing, so we don't
            // want to do anything other than log exceptions on errors.
            Debug.error( e.toString() );

            Debug.error( Debug.getStackTrace( e ) );
        }
    }

    public static void main(String args[])
    {
        try
        {
            Debug.enableAll();
            XSLMessageTransformer t = null;
            String xslFileName = "E:\\b39\\testsprint\\maps\\ReqMain.xsl";

            // Sprint
    //        String[] xslPath = {"E:\\b39\\gateways\\sprint-lsr-order\\lib\\maps.jar"};
    //        String xslClassName = "com.nightfire.sprint_lsr_order.ReqMain";
            String fileSource = "E:\\b39\\testsprint\\sprint850.xml";
            String fileResult = "E:\\b39\\testsprint\\sprintout.xml";
            String msg = FileUtils.readFile(fileSource);
            t = MessageTransformerCache.getInstance().get(xslFileName, false);


    /*BS
            String[] xslPath = {"E:/work/testbench/maps.jar"};
            String xslClassName = "com.nightfire.bs_lsog6_lsr_order.ReqMain";
            String fileSource = "E:\\work\\testbench\\runtime\\850.xml";
            String fileResult = "E:\\work\\testbench\\runtime\\abc2-template.xml";
    */

    // compiled maps
    //         t = MessageTransformerCache.getInstance().get(xslClassName, xslPath);
            System.out.println("getOutputMethod: " + t.getOutputConfiguration("method"));
            String result = t.transform(msg);
            FileUtils.writeFile(fileResult, result);
        }
        catch(Exception e)
        {
            System.out.println("Dev exception: " + e.getMessage());
        }
    }

}




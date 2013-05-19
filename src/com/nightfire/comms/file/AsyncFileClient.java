/*
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //gateway/main/com/nightfire/spi/common/communications/file/AsyncFileClient.java#2
 */

package com.nightfire.comms.file;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import java.io.*;


import com.nightfire.spi.common.driver.MessageProcessorContext;

public class AsyncFileClient extends MessageProcessorBase {

    // public static final String SOURCE_ID = "$Id: //comms/R4.4/com/nightfire/comms/file/AsyncFileClient.java#1 $";

    //Directory where the File Needs to be placed
    private String tgtDir;

    //Target File Name
    private String tgtFile;

    //Target File Suffix
    private String tgtSuffix;

    //Cached time stamp
    private static String cachedTimeStamp = null;

    // Flag for append or overwrite file
    private boolean appendFlag;


    //Property name in context for FTP file name
    private String contextFileNameLoc;

    // Flag indicating whether data should be treated as binary or not (default).
    private boolean binaryMode = false;

    // Property for setting the encoding type
    private String encodingType;


    /**
      * Constructor used by Factory to create this object.
     */
    public AsyncFileClient(){

    }

    /**
     * Initializes the instances
     *
     * @param Key  to load properties from Persitent Properties
     *
     * @param Type to load Properties from Persistent Properties
     *
     * @exception ProcessingException Thrown if initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Initializing the asynchronous file client");

        super.initialize(key,type);

        tgtDir = addFileSep(getRequiredPropertyValue(TARGET_DIR_PROP));

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE ,"AsyncFileClient:initialize The Target Directory Obtained from Properties is " +tgtDir);

        tgtFile = getPropertyValue(TARGET_FILE_PROP);

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "AsyncFileClient:initialize The Target File Obtained from Properties is " +tgtFile);

        tgtSuffix = getPropertyValue(TARGET_SUFFIX_PROP);

        if (tgtSuffix != null) {

            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE,
                      "AsyncFileClient.initialize: The suffix for the target filename obtained from Properties is " +
                      tgtSuffix );
        }


        String strTemp = getPropertyValue( APPEND_FLAG_PROP );
        if ( StringUtils.hasValue(strTemp))
        {
            try {
                appendFlag = getBoolean(strTemp);
            } catch ( FrameworkException e ) {
                throw new ProcessingException ( e.getMessage ( ) );
            }
        }
        else
        {
            appendFlag = false;
        }

          contextFileNameLoc = getPropertyValue(CONTEXT_FILE_NAME_LOC_PROP);

          if ( !StringUtils.hasValue(tgtFile) && !StringUtils.hasValue(contextFileNameLoc) )
              throw new ProcessingException( "ERROR: At least one of target-file-name or context-file-name-location must be configured." );


        encodingType = getPropertyValue(ENCODING_TYPE_PROP);
        if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "AsyncFileClient:initialize The Encoding Type Obtained from Properties is " +encodingType);


        String binaryModeStrValue = getPropertyValue(BINARY_MODE_PROP);

        //Binary mode is set to false by default if the BINARY MODE PROPERTY
        // is null or has an empty value.
        if (StringUtils.hasValue(binaryModeStrValue) )
        {
            try {
                binaryMode = StringUtils.getBoolean(binaryModeStrValue);
            } catch (FrameworkException fe) {
                String error = "Property [" + BINARY_MODE_PROP + "] is set incorrectly :" + fe.getMessage();
                Debug.error( error );
                throw new ProcessingException(error);
            }
        }
    }

    // adds file sep if it does not exist
    private String addFileSep(String sep) {
       if (!sep.endsWith(java.io.File.separator) )
          sep = sep +  java.io.File.separator;
      return sep;
    }

    /**
     * Create unique time stamp for output file name. This method caches the time stamp
     * used last time. If the current time value is equal to or small than the cached
     * time value, the cached time value is then incremented by 1 and returned as
     * the current time stamp.
     *
     * @return String Time stamp used in generating output file names
     *
     */
    private static synchronized String getTimeStamp() {

      //This would be used for file name

      String currentTimeStamp = DateUtils.getDateToMsecAsString ( );

      if( cachedTimeStamp == null ) { //First time to process

        cachedTimeStamp = currentTimeStamp;

      } else {

        long currentTime = Long.valueOf(currentTimeStamp).longValue();
        long cachedTime = Long.valueOf(cachedTimeStamp).longValue();

        if( currentTime <= cachedTime) {
          cachedTimeStamp = String.valueOf(cachedTime + 1);
        } else {
          cachedTimeStamp = currentTimeStamp;
        }
      }

      return cachedTimeStamp;

    }

    /**
     * Writes the input message (DOM or String) to a file and (optionally) returns
     * a value.
     *
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  NVPair[] Name value Pair or null
     *
     * @exception  MessageException  Thrown if processing fails.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public NVPair[] execute ( MessageProcessorContext mpcontext,java.lang.Object input ) throws MessageException, ProcessingException
    {

	if(input==null) return null;

        String appendTag = null;
        //Get FTP file name
        String fileName = null;
        if (StringUtils.hasValue(contextFileNameLoc)) {
            //Get FTP file name from context
            fileName = tgtDir + mpcontext.getString(contextFileNameLoc);
        }
        else {

            appendTag = getTimeStamp();

            fileName = tgtDir +tgtFile + appendTag;
        }

        if ( tgtSuffix != null ) {

            fileName += tgtSuffix;
        }

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS,
                      "The AsyncFileClient is writing to the file " + fileName);
	    try
                {
                    if ( binaryMode )
                    {

                        try
                        {
							if ( appendFlag )
								FileUtils.appendBinaryFile( fileName, Converter.getString( input ).getBytes(encodingType));
							else
								FileUtils.writeBinaryFile( fileName, Converter.getString( input ).getBytes(encodingType) );
                        }
                        catch(UnsupportedEncodingException e)
                        {
							Debug.log(Debug.ALL_ERRORS, "AsyncFileClient could not write to file , unsupported Encoding Type " +e.getMessage());
							throw new ProcessingException(e.getMessage());
                        }

					}
                    else
                    {
                        if ( appendFlag )
                            FileUtils.appendFile( fileName, Converter.getString( input ) );
                        else
                            FileUtils.writeFile( fileName, Converter.getString( input ) );
                    }

                    //If next processor is the Templar component for USWEST, then send back
                    //file name, else null
                    if (toProcessorNames != null)
                        {
                            //Return file name
                            String fileName0 = null;

                            if (StringUtils.hasValue(contextFileNameLoc)) {
                              fileName0 = mpcontext.getString(contextFileNameLoc);
                            }
                            else {
                              fileName0 = tgtFile + appendTag;
                            }
                            return formatNVPair(fileName0);
                        }

                }
            catch (FrameworkException fe)
                {
                    Debug.log(Debug.ALL_ERRORS, "AsyncFileClient could not write to file , reason " +fe.getMessage());
	              throw new ProcessingException(fe.getMessage());
                }

        return null;

    }

    /**
     * Maps on Property Key to load properties from Persistent Property Table
     */
    public static final String TARGET_DIR_PROP = "TARGET_DIR";

    /**
     * Maps on Property Key to load properties from Persistent Property Table
     */
    public static final String TARGET_FILE_PROP = "TARGET_FILE";

    /**
     * Maps on Property Key to load properties from Persistent Property Table
     */
    public static final String TARGET_SUFFIX_PROP = "TARGET_SUFFIX";

    // Property name in message-processor context containing name of FTP file.
    public static final String CONTEXT_FILE_NAME_LOC_PROP  = "CONTEXT_FILE_NAME_LOC";

    /**
     * Property flag indicating whether file should be appended to or not.
     */
    public static final String APPEND_FLAG_PROP = "APPEND_FLAG";

    /**
     *  Whether to use binary mode for writing files (default is not).
     */
    public static final String BINARY_MODE_PROP = "BINARY_MODE";

    /**
     *  Property to set encoding Type
     */
    public static final String ENCODING_TYPE_PROP = "ENCODING_TYPE";


}
/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.file;

import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.driver.*;
import java.util.*;
import java.io.*;


public class AsyncFileServer extends PollComServerBase 
{
    public static final String DIR_SEP = File.separator;

    /**
     *  Maps to  Received File Dir Property in PersistentProperty Table
     */
    public static final String RECEIVED_FILE_PROP      = "RECEIVED_FILE_DIR";
    
    /**
     *  Maps to Processed File Dir Property in PersistentProperty Table
     */
    public static final String PROCESSED_FILE_PROP     = "PROCESSED_FILE_DIR";

    /**
     *  Maps to Error File Dir Property in PersistentProperty Table
     */
    public static final String ERROR_FILE_PROP         = "ERROR_FILE_DIR";

    /**
     *  Maps to Exception File Dir Property in PersistentProperty Table
     */
    public static final String EXCEPTION_FILE_PROP     = "EXCEPTION_FILE_DIR";

    /**
     *  Whether to use binary mode for reading files
     */
    public static final String BINARY_MODE_PROP        = "BINARY_MODE";

    /**
     * Property giving location in context to find name of current file being processed (optional).
     */
    public static final String CONTEXT_FILE_NAME_LOC_PROP  = "CONTEXT_FILE_NAME_LOC";

    /**
     * This optional property is used to provide a character encoding to
     * use when converting the content of a polled file from binary to
     * characters. The presence of this property implies that files
     * will be read in binary format, and then converted to a String
     * using the configured encoding. 
     */
    public static final String CHAR_ENCODING_PROP_NAME = "CHARACTER_ENCODING";

    // Used to contain configuration properties on a per-customer basis.
    private class PerCIDConfiguration
    {
        private List recvFileDirs;

        private String
        procFileDir                                    = null,
            file                                           = null,
            errorFileDir                                   = null,
            exceptionFileDir                               = null,
            binaryModeStrValue                             = null;

        private boolean binaryMode                         = false;

        // Name of the current file being processed.
        private String currentFileName                     = null;

        //Property name in context for FTP file name
        private String contextFileNameLoc                  = null;

        /**
         * The character encoding, if any, to use when converting a files binary
         * content to a String.
         */
        private String characterEncoding;

        /**
         * The value of StringUtils.hasValue( characterEncoding ). 
         */
        private boolean useEncoding;
    }


    /**
     * Constructor
     *
     */
    public AsyncFileServer (String key, String type)
        throws ProcessingException
    {
        super(key,type);
    }


    /**
     * Load the CID-specific configuration properties from the database.
     * (Implicit key is thread-local CID set on the current CustomerContext.)  The
     * object returned is a leaf class-specific configuration container.
     * This method should overridden in the leaf-class, and shouldn't be
     * called directly.  Instead, leaf-classes should call getConfiguration(),
     * which will call this method if it can't find a previously-loaded and
     * cached configuration object.
     *
     * @return  The customer-specific configuration, or null if unavailable.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    protected Object loadConfiguration ( ) throws FrameworkException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Initializing the asynchronous file server.");

        // Re-initialize properties so that we will load any per-customer ones as
        // specified by the CID on the thread-local customer context.
        initialize( key, type );

        PerCIDConfiguration pcc = new PerCIDConfiguration( );

        StringBuffer errBuf = new StringBuffer();

        pcc.procFileDir = getRequiredPropertyValue(PROCESSED_FILE_PROP,errBuf);
        pcc.errorFileDir = getRequiredPropertyValue(ERROR_FILE_PROP,errBuf);

        pcc.exceptionFileDir = getPropertyValue(EXCEPTION_FILE_PROP);

        if (!StringUtils.hasValue(pcc.exceptionFileDir) )
            pcc.exceptionFileDir = pcc.errorFileDir;

        //Check if files are to be retrieved in binary mode
        pcc.binaryModeStrValue = getPropertyValue(BINARY_MODE_PROP);

        //Binary mode is set to false by default if the BINARY MODE PROPERTY
        // is null or has an empty value.
        if (StringUtils.hasValue(pcc.binaryModeStrValue) )
        {
            try {
                pcc.binaryMode = StringUtils.getBoolean(pcc.binaryModeStrValue);
            } catch (FrameworkException fe) {
                String error = "Property [" + BINARY_MODE_PROP + "] is set incorrectly :" + fe.getMessage();
                Debug.log(this,Debug.ALL_ERRORS, error);
                throw new ProcessingException(error);
            }
        }

        // get the optional character encoding to use
        pcc.characterEncoding = getPropertyValue( CHAR_ENCODING_PROP_NAME );
        pcc.useEncoding = StringUtils.hasValue( pcc.characterEncoding );

        if( pcc.useEncoding && Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
        {
           Debug.log(Debug.SYSTEM_CONFIG,
                     "Files will be read using ["+pcc.characterEncoding+
                     "] encoding.");
        }

        // try to first get multiple recv directories
        String recvFileDir;
            
        pcc.recvFileDirs = new ArrayList();
            
        for (int i = 0;true; i++)
        {
            recvFileDir = getPropertyValue(PersistentProperty.getPropNameIteration(RECEIVED_FILE_PROP, i));

            if (!StringUtils.hasValue(recvFileDir) )
                break;
            
            pcc.recvFileDirs.add(addFileSeparator(recvFileDir));
        }

        // if there are no recv directories then there needs to be a single recv directory.
        if (pcc.recvFileDirs.size() == 0) {
            pcc.recvFileDirs.add(addFileSeparator(getRequiredPropertyValue(RECEIVED_FILE_PROP,errBuf) ) );
        }
        
        // if there are any missing required fields throw an exception
        if (errBuf.length() > 0 )
            throw new ProcessingException(errBuf.toString() );

        // Location of fileName in context (optional)
        pcc.contextFileNameLoc = getPropertyValue( CONTEXT_FILE_NAME_LOC_PROP );

        return pcc;
    }
    

    /**
     * adds a file separator to the end of a dir path if it does not exist.
     */
    private String addFileSeparator(String dir) {
       if (!dir.endsWith(DIR_SEP) ) {
          dir = dir + DIR_SEP;
       }
       return dir;
    }

    /**
     * gets called when the timer goes off. This method calls processFiles on each of the
     * recv directories.
     */
    protected void processRequests() throws ProcessingException
    {
        try
        {
            PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

            Iterator iter = pcc.recvFileDirs.iterator();

            while (iter.hasNext() ) {
                String dir = (String) iter.next();
                processFiles(dir);
            }
        }
        finally
        {
            // Reset customer context.
            try
            {
                CustomerContext.getInstance().cleanup( );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );

                throw new ProcessingException( e );
            }
        }
    }


    /**
     * Processes files in given directory
     *
     * @param       inbox -- Receive file directory
     * @exception   ProcessingException thrown when fails
     */
    private void processFiles(String inbox) throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        // Get all files in receive file directory and process them.
        String[] files = null;

        try
        {
            files = FileUtils.getAvailableFileNames(inbox);
        }
        catch (FrameworkException e)
        {
            throw new ProcessingException("Cannot read input directory. \n" + e.getMessage());
        }

        if (files.length > 0)
        {
            String cid = null;

            try 
            {
                cid = CustomerContext.getInstance().getCustomerID( );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( e );
            }

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "There are [" + files.length +
                                            "] files to process in directory [" + inbox + "].");
            // For each file found in the current directory ...
            for (int Ix = 0;  Ix < files.length;  Ix++)
            {
                pcc.file = inbox + files[Ix];

                try
                {
                    // Make sure that file isn't being updated before we begin to process it.
                    if (FileUtils.getFileStatistics(pcc.file).lastModified >=
                        (DateUtils.getCurrentTimeValue() - (DEFAULT_TIMER_WAIT_TIME * 1000)))
                    {
                        if (Debug.isLevelEnabled(Debug.IO_STATUS))
                            Debug.log(Debug.IO_STATUS, "File [" + pcc.file + "] is being modified.\n" +
                                                       "Processing of this file will be skipped.");
                        continue;
                    }

                    if (Debug.isLevelEnabled(Debug.IO_STATUS))
                        Debug.log(Debug.IO_STATUS, "Processing file: " + pcc.file);

                    //Process request
                    try
                    {
                        // Remember file name so that it can be placed into driver's context
                        // during configureDriver() call.
                        pcc.currentFileName = files[Ix];

                        if( pcc.useEncoding )
                        {
                            byte[] bytes = FileUtils.readBinaryFile(pcc.file);

                            try
                            {
                            
                                process( null,
                                         new String(bytes, pcc.characterEncoding) );

                            }
                            catch(java.io.UnsupportedEncodingException ueex)
                            {
                                Debug.error( "["+pcc.characterEncoding+
                                             "] encoding is not supported. "+
                                             "The default character encoding "+
                                             "will be used: "+ueex.toString() );

                                CustomerContext.getInstance().setCustomerID( cid );

                                // use default encoding
                                process( null, new String(bytes) );

                            }

                        }
                        else if (pcc.binaryMode)
                        {
                            // Read file's contents - as binary
                            process( null,  FileUtils.readBinaryFile(pcc.file) );

                        }
                        else
                        {
                            // Read file's contents - as ascii text
                            process( null, FileUtils.readFile(pcc.file) );
                        }

                        //Move the File to a Processed Directory
                        FileUtils.moveFile(pcc.procFileDir,pcc.file);
                    }
                    catch (ProcessingException e)
                    {
                        //Moves the message to Error directory
                        FileUtils.moveFile(pcc.exceptionFileDir, pcc.file);
                    }
                    catch (MessageException e)
                    {
                        //Moves the message to Error directory
                        FileUtils.moveFile(pcc.errorFileDir, pcc.file);
                    }
                    finally
                    {
                        CustomerContext.getInstance().setCustomerID( cid );
                    }
                }
                catch (FrameworkException e)
                {
                    try {
                       FileUtils.moveFile(pcc.exceptionFileDir, pcc.file);
                    } catch (FrameworkException fe) {
                      Debug.log(Debug.ALL_ERRORS,"Could not write " + pcc.file + " to " + pcc.exceptionFileDir + " folder: " + fe.getMessage());
                    }
                    throw new ProcessingException("ERROR: Cannot process input file. \n" + e.getMessage());
                }
            }
        }
    }


    /**
     * Callback allowing sub-classes to place name of file in context.
     *
     * @param  driver  Message-processing driver to configure.
     *
     * @exception  ProcessingException  Thrown if processing can't be performed.
     *
     * @exception  MessageException  Thrown if message is bad.
     */
    protected void configureDriver ( MessageProcessingDriver driver )
        throws ProcessingException, MessageException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if ( StringUtils.hasValue( pcc.contextFileNameLoc ) )
        {
            MessageProcessorContext mpc = driver.getContext( );

            mpc.set( pcc.contextFileNameLoc, pcc.currentFileName );
        }
    }
}

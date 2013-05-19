/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.ftp.java;


import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.communications.*;

import java.util.regex.*;


import com.nightfire.comms.util.ftp.*;
import com.ibm.bsf.*;


/*
 * This class is intended to be the end point for an SPI incoming message.
 * The the FTP server is pooled periodically  and any file found is
 * downloaded and passed on to a Message Processing Driver (MPD) chain as a
 * String or byte []. If an exception is thrown by the MPD, then a copy of the file is
 * stored in a directory pointed to by EXCEPTION_DIR_PROP property.
 */
public class FTPPoller extends PollComServerBase implements FTPConstants
{
    /**
     * Maximum file size allowed to be processed (in KB).
     */
    public static final String FILE_SIZE_ALLOWED_PROP          = "FILE_SIZE_ALLOWED";


    /**
     * Index of 'size' in result of ls command.
     */
    public static final String SIZE_INDEX_IN_LISTING_PROP          = "SIZE_INDEX_IN_LISTING";

   /**
     * Path to exception directory. If any errors occur during ftp transfer
     * files are placed here to be retried later.
     *
     */
    public static final String EXCEPTION_DIR_PROP              = "EXCEPTION_DIR";

    /**
     * Path to processed directory
     *
     */
    public static final String PROCESSED_DIR_PROP              = "PROCESSED_DIR";

    /**
     * Specifies path or error directory
     *
     */
    public static final String ERROR_FILE_PROP                 = "ERROR_FILE_DIR";

    
    /**
        * The message-processor context location
        * to place the current file name getting processed at.
        *
        */
       public static final String CONTEXT_FTP_FILE_NAME_LOC_PROP  = "CONTEXT_FTP_FILE_NAME_LOC";


    /**
        * Index position of file size in ls command output
        */
       public static final int DEFAULT_SIZE_INDEX_IN_LISTING =3;

      /**
     * The value of 1 KB. Used while determining the size of input message object.
     */
    public static final int KB = 1024;

    
    
    /**
     * Regular expression value used for matching file listings.
     * This can be used to filter out specific files from the remote
     * server that should be processed.
     *
     */
    public static final String REMOTE_LINE_FILTER_PROP = "REMOTE_LINE_FILTER";

    // Max file size in bytes.
    private long maxFileSizeAllowed=0;

    // index of Size as a result of ls command
    private int indexOfSize = DEFAULT_SIZE_INDEX_IN_LISTING;


    // Used to contain configuration properties on a per-customer basis.
    private class PerCIDConfiguration
    {
        private String exceptionDir;    //path to exception directory
        private String errorFileDir;    //path to error directory
        private String processedDir;    //path to processed directory

        private String script, scriptPath;
        //private String getScript, getScriptPath;
        //private String delScript, delScriptPath;

      
        // Name of the current file being processed.
        private String currentFTPFileName                          = null;

        //location in context for FTP file nameprivate FtpDataConnect ftp;
        private String contextFTPFileNameLoc                       = null;

        private boolean singleConnect = false;
    
        private BSFManager scriptMgr = new BSFManager();
    
        private FtpDataConnect ftp;
    
    
        private String charEncoding = null;
   
        private CommUtils.FileBean fileBean = new CommUtils.FileBean();
    
        private int socTimeOut = DEFAULT_TIMEOUT;

        private String userId, userPasswd;
    
        private String host;
    
        private int port;
    
        private Pattern filter = null;
        private String filterStr = null;

        private String lock = null;
    
   
        // used to indicate if saved files should be deleted from remote server
        private boolean deleteSavedFiles = false;


        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "CONFIGURATION: " )
                .append( "exception-dir [" ).append( exceptionDir )
                .append( "], error-dir [" ).append( errorFileDir )
                .append( "], processed-dir [" ).append( processedDir )
                .append( "], script-path [" ).append( scriptPath )
                .append( "], current-ftp-file-name [" ).append( currentFTPFileName )
                .append( "], context-FTP-file-name-loc [" ).append( contextFTPFileNameLoc )
                .append( "], single-connect? [" ).append(singleConnect  )
                .append( "], char-encoding [" ).append( charEncoding )
                .append( "], soc-timeout [" ).append( socTimeOut )
                .append( "], user-id [" ).append( userId )
                .append( "], host [" ).append( host )
                .append( "], port [" ).append( port )
                .append( "], filter-str [" ).append( filterStr )
                .append( "], delete-saved-files? [" ).append( deleteSavedFiles ).append( "]" );

            return( sb.toString() );
        }
    }

    
    /**
     * Constructor
     *
     */
    public FTPPoller (String key, String type) throws ProcessingException
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
            Debug.log(Debug.OBJECT_LIFECYCLE, "Initializing the FTPPoller...");

        // Re-initialize properties so that we will load any per-customer ones as
        // specified by the CID on the thread-local customer context.
        initialize( key, type );

        try
        {
            String maxFileSizeAllowedString  = getPropertyValue( FILE_SIZE_ALLOWED_PROP);

            String indexOfSizeString = getPropertyValue( SIZE_INDEX_IN_LISTING_PROP);

            if ( StringUtils.hasValue(indexOfSizeString))
            {
                 indexOfSize = StringUtils.getInteger(indexOfSizeString);

                 if ( Debug.isLevelEnabled( Debug.MSG_DATA) )
                    Debug.log(Debug.MSG_DATA, "'Index of Size in the ls command output' property [" + SIZE_INDEX_IN_LISTING_PROP + "] is configured as [" + indexOfSize + "]");
            }
            else
                if ( Debug.isLevelEnabled( Debug.MSG_DATA) )
                   Debug.log(Debug.MSG_DATA, "'Index of Size in the ls command output' property is not configured.  Using default configuration [" + indexOfSize + "]");


            if ( StringUtils.hasValue(maxFileSizeAllowedString))
            {
                    maxFileSizeAllowed = Long.parseLong(maxFileSizeAllowedString);
                    //converting KB to bytes
                    maxFileSizeAllowed = maxFileSizeAllowed *KB;

                    if ( Debug.isLevelEnabled( Debug.MSG_DATA) )
                        Debug.log(Debug.MSG_DATA, "Maximum file size value property [" + FILE_SIZE_ALLOWED_PROP + "] is configured as [" + maxFileSizeAllowed + "]");
            }
            else
            if ( Debug.isLevelEnabled( Debug.ALL_WARNINGS) )
                            Debug.log( Debug.ALL_WARNINGS, "Maximum file size value not set for FTP Poller, hence setting to 0");
        }
        catch(Exception e)
        {
            if ( Debug.isLevelEnabled( Debug.ALL_ERRORS) )
                Debug.log( Debug.ALL_ERRORS, "Maximum file size value not set for FTP Poller, hence setting to 0");
        }
        // Replace any previously-loaded FTP-COMMON properties with customer-specific equivalents.
        properties.putAll( PersistentProperty.getProperties( key, "FTP_COMMON" ) );

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Current configuration (including customer-specific):\n"
                       + PropUtils.suppressPasswords( properties.toString() ) );

        PerCIDConfiguration pcc = new PerCIDConfiguration( );

        StringBuffer errBuf = new StringBuffer();

        pcc.errorFileDir = getRequiredPropertyValue(ERROR_FILE_PROP,errBuf);
        pcc.errorFileDir = CommUtils.addTrailingPathSep(pcc.errorFileDir);

        pcc.exceptionDir = getRequiredPropertyValue(EXCEPTION_DIR_PROP,errBuf);
        pcc.exceptionDir = CommUtils.addTrailingPathSep(pcc.exceptionDir);

        pcc.processedDir = getRequiredPropertyValue(PROCESSED_DIR_PROP, errBuf);
        pcc.processedDir = CommUtils.addTrailingPathSep(pcc.processedDir);


        String  sslModeStr = getRequiredPropertyValue(SSL_MODE_PROP,errBuf);
        boolean sslMode = StringUtils.getBoolean(sslModeStr, false);


        SSLConfigInfo sslConfig = null;

        if (sslMode) {
            sslConfig = new SSLConfigInfo();
            sslConfig.setKeyStoreType(getPropertyValue(KEYSTORE_TYPE_PROP));
            sslConfig.setKeyStore(getRequiredPropertyValue(KEYSTORE_PATH_PROP, errBuf));
            sslConfig.setKeyStorePassPhrase(getRequiredPropertyValue(PASSPHRASE_PROP, errBuf));
            sslConfig.setTrustStore(getRequiredPropertyValue(TRUSTSTORE_PATH_PROP, errBuf));
            sslConfig.setSSLContextVersion(getRequiredPropertyValue(SSL_VERSION_PROP,errBuf));
        }


        pcc.scriptPath = getRequiredPropertyValue(SCRIPT_PROP, errBuf);


        pcc.contextFTPFileNameLoc = getPropertyValue(CONTEXT_FTP_FILE_NAME_LOC_PROP);

        String scmStr = getRequiredPropertyValue(SINGLE_CONNECT_MODE_PROP, errBuf);

        pcc.singleConnect = StringUtils.getBoolean(scmStr, false);

        pcc.charEncoding = getPropertyValue(CHAR_ENCODING_PROP);

        String socTimeOutStr   = getPropertyValue(CONNECT_TIMEOUT_PROP);
        if (StringUtils.hasValue(socTimeOutStr) )
            pcc.socTimeOut = Integer.parseInt(socTimeOutStr) * 1000;

        pcc.host = getRequiredPropertyValue(REMOTE_HOST_PROP);
        try {
            pcc.port = StringUtils.getInteger(getRequiredPropertyValue(REMOTE_PORT_PROP));
        }
        catch (FrameworkException e) {
            errBuf.append("Invalid value for port property : " + e.getMessage() );
        }

        pcc.userId = getRequiredPropertyValue(USER_IDENTITY_PROP);
        pcc.userPasswd = getRequiredPropertyValue(USER_PASSWORD_PROP);

        String deleteSavedStr = getRequiredPropertyValue(DELETE_DOWNLOADED_PROP);
        pcc.deleteSavedFiles = StringUtils.getBoolean(deleteSavedStr, false);

        pcc.filterStr = getPropertyValue(REMOTE_LINE_FILTER_PROP);


        if ( StringUtils.hasValue(pcc.filterStr) ) {
            try {
                pcc.filter = Pattern.compile(pcc.filterStr);
            }
            catch (PatternSyntaxException e) {
                errBuf.append("Invalid regular expression for " + REMOTE_LINE_FILTER_PROP +":" + e.getMessage());
            }
        }


        // note this key formation must be the same as FTPClient's
        pcc.lock = pcc.host +"-" + pcc.port;


        // if there are any missing required properties throw an exception with the missing props.
        if (errBuf.length() > 0 )
           throw new ProcessingException(errBuf.toString());

        try {

            if ( sslMode )
                pcc.ftp = FtpDataConnectFactory.createSSLFTP(sslConfig);
            else
                pcc.ftp = FtpDataConnectFactory.createFTP();


             pcc.ftp.setDefaultTimeout(pcc.socTimeOut);

             FtpDataBean ftpBean = new FtpDataBean(pcc.ftp);
             pcc.scriptMgr.declareBean(SCRIPT_FTPBEAN, ftpBean, FtpDataBean.class);
             pcc.scriptMgr.declareBean(SCRIPT_PROPSBEAN, new PropsBean(properties), PropsBean.class);
             pcc.scriptMgr.declareBean(SCRIPT_FILEBEAN, pcc.fileBean, CommUtils.FileBean.class);

             pcc.script = FileUtils.readFile(pcc.scriptPath);

             // load the script
             pcc.scriptMgr.eval("javascript", pcc.scriptPath, 0, 0, pcc.script);

             if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                 Debug.log( Debug.SYSTEM_CONFIG, pcc.describe() );

             return pcc;

        } catch (BSFException e){
            throw new ProcessingException("Failed to setup scripting support : " + e.getMessage() );
        } catch (FrameworkException f ){
            throw new ProcessingException("Failed to read script files : " + f.getMessage() );
        }
    }


    /**
     * Polls the remote server for any message files, sends them for further processing.
     */
    public void processRequests() throws ProcessingException
    {
        try
        {
            processRequestsImpl( );
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
            }
        }

    }


    /**
     * Polls the remote server for any message files, sends them for further processing.
     */
    public void processRequestsImpl() throws ProcessingException
    {

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS,"\n *** Checking the remote FTPServer for files.");

        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        // getting list of files.
        String[] filesToGet = listFiles();

        // first check if any file has been found
        if(filesToGet == null || filesToGet.length == 0) {
            if ( Debug.isLevelEnabled( Debug.MSG_DATA) )
                Debug.log(Debug.MSG_DATA,"The number of files to get is 0.");
            return;
        }

        if ( Debug.isLevelEnabled( Debug.MSG_DATA) )
            Debug.log(Debug.MSG_DATA,"The number of files to get is " + filesToGet.length);

        byte[]  data = null;
        boolean dataSaved = false;

        String cid = null;

        try
        {
            cid = CustomerContext.getInstance().getCustomerID( );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e );
        }

        // For each file found in the current directory ...
        for (int i = 0; i < filesToGet.length;  i++)
        {
            data = getFile(filesToGet[i]);
            dataSaved = false;

            // used for the call back method (configureDriver(...) )
            // to get the current file name.
            pcc.currentFTPFileName = filesToGet[i];

            //now try to process
            try
            {
                if (data != null)
                {
                    // NOTE: The process() method on the base class calls CustomerContext.cleanup(), which resets
                    // the customer-id.  This may cause subsequent operations in this invocation of processRequests()
                    // to fail, so we reset it back to the proper cid value until we're done.
                    try
                    {
                        if (pcc.ftp.getTransferMode() == FtpDataConnect.BIN_MODE)
                        {
                            if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
                                Debug.log(Debug.IO_STATUS,"checkForFiles: processing byte[]...");
                            process(null, data);
                        }
                        else
                        {
                            if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
                                Debug.log(Debug.IO_STATUS,"checkForFiles: processing String...");
                            process(null, CommUtils.convertByteToString(data, pcc.charEncoding));
                        }
                    }
                    finally
                    {
                        CustomerContext.getInstance().setCustomerID( cid );
                    }

                    if (StringUtils.hasValue(pcc.processedDir)) {
                        // Save the processed file if PROCESSED_DIR is set.
                        try {
                            CommUtils.saveFile(filesToGet[i], pcc.processedDir, data, pcc.ftp, pcc.charEncoding);
                            dataSaved = true;

                        }
                        catch(Exception e) {
                            Debug.error("Could not save file to [" + pcc.processedDir +"]");
                        }
                    }
                }
            }
            catch (ProcessingException pe)
            {
                // If we encounter a ProcessingException, save the file in the ExceptionDir

                Debug.log(Debug.ALL_ERRORS,StringUtils.getClassName(this) +
                                           ": Cannot process file [" + filesToGet[i] + "].");

                Debug.logStackTrace(pe);

                CommUtils.saveFile(filesToGet[i], pcc.exceptionDir, data, pcc.ftp, pcc.charEncoding);
                dataSaved = true;


            }
            catch (Exception me)
            {
                // If we encounter a Exception, gateway received a message it could not
                // process and should be placed in the ErrorDirectory.
                Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) +
                                            ": Cannot process file [" + filesToGet[i] + "].");

                Debug.logStackTrace(me);
                CommUtils.saveFile(filesToGet[i], pcc.errorFileDir, data, pcc.ftp, pcc.charEncoding);
                dataSaved = true;

            }


            if (dataSaved && pcc.deleteSavedFiles)
            {
                // if the file has been saved and the flag to
                // delete remote downloaded files is true, then
                // we can safely delete the file from the server
                deleteFile(filesToGet[i]);
            }

        }

        if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log(Debug.MSG_LIFECYCLE, "FTPPoller: Finished checking for files.");
    }


    // gets a file list and filters the results
    private String[] listFiles() throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        String[] fileList  = null;

        try
        {
            if (pcc.singleConnect) {
                synchronized (KeyHash.getLock(pcc.lock)) {
                    fileList = connectAndList();
                }
            } else {
                fileList = connectAndList();
            }

            return filterFiles(fileList);
        }
        catch (Exception e)
        {
            throw new ProcessingException("Failed to list files: " + e.getMessage());
        }
    }


    /**
     * Filters out a list of files based on a regular expression.
     * Needs to have a group within the filter to indicate the name of the file.
     *
     *
     * @param listing A full listing obtained from the {@link #ls() } command.
     * @return A list of file names only.
     * @exception Exception if an error occurs
     */
    private String[] filterFiles(String[] listing) throws Exception
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        if (listing == null || listing.length == 0) {
            if ( Debug.isLevelEnabled( Debug.MSG_DATA) )
                Debug.log(Debug.MSG_DATA, "There are no files available, matching zero files.");
            return null;
        }
        else if (pcc.filter == null) {
            Debug.warning("No remote filter was set so can't match any files.");
            return null;
        }

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS, "Filtering file list with filter [" + pcc.filterStr +"]");
        ArrayList list = new ArrayList();

        for(int i = 0; i < listing.length; i++ ) {

            Matcher m = pcc.filter.matcher(listing[i]);

            if ( m.groupCount() == 0)
                throw new Exception("No group for file name in regular expression. Please modify your regular expression "
                                       + "to indicate the name of the file in a listing. Use ( and ) to group the file name.");

            if (m.matches())
            {
                String name = m.group(1);
                if (StringUtils.hasValue(name) )
                {
                    if ( Debug.isLevelEnabled( Debug.MSG_STATUS) )
                        Debug.log(Debug.MSG_STATUS, "Matched file [" + name + "], from listing [" + listing[i] + "]");

                    // check for file Size, if fileSize in invalid, skip polling this file
                    // if maxFileSizeAllowed is 0 i.e.  not configured, add the file to valid file list.
                    // if maxFileSizeAllowed is configured, additionally check for validFileSize using isValidFileSize() method. 
                    if (maxFileSizeAllowed == 0 || isValidFileSize(name, listing[i]))
                    {
                        if ( Debug.isLevelEnabled( Debug.MSG_STATUS) )
                            Debug.log(Debug.MSG_STATUS, "Adding file [" + name + "] to the list of valid files to be polled");
                        list.add(name);
                    }
                }
            }
        }

        if ( list.size() > 0 ) {
            return (String[]) list.toArray(new String[list.size()]);
        }
        else {
            return null;
        }
    }

    /**
     * Check the file size to aid determining whether the file should be polled or not
     *
     * @param  fileName  Message-processing driver to configure.
     * @param  lsListing Listing of the ls command as a String
     * @exception  ProcessingException  Thrown if processing can't be performed.
     *
     * @return  boolean indicating whether (depending on the fileSize) the file is valid or not for polling
     */
    private boolean isValidFileSize(String fileName, String lsListing) throws ProcessingException
    {
       long fileSize = 0;

        try
        {
            // split the lsListing based on whiteSpace
            String[] lsListingTokens = lsListing.split("\\s+");

            // result of ls listing is as
            //-rw-r--r--   1 engg     other        447 Oct  9 15:44  batch
            // index of 'size' is 5
            // This output is variable in various systems hence is allowed to be configured.
            if ( StringUtils.hasValue(lsListingTokens[indexOfSize]))
            {
                // get file size
                fileSize = Long.parseLong(lsListingTokens[indexOfSize]);
            }

            if ( Debug.isLevelEnabled( Debug.MSG_DATA) )
                Debug.log(Debug.MSG_DATA, "isValidFileSize: Size of file [" + fileName + "] is [" + fileSize + " bytes]");
        }
        catch(Exception e)
        {
            Debug.logStackTrace(e);
            throw new ProcessingException (e);
        }

        if ( maxFileSizeAllowed!=0 && fileSize>=maxFileSizeAllowed)
        {
            if ( Debug.isLevelEnabled( Debug.IO_WARNING) )
                            Debug.log(Debug.IO_WARNING,"Skipped processing for file [" + fileName + "] as file size ["+fileSize
                                    +" bytes] is greater than the configured max allowed file size [" + maxFileSizeAllowed + " bytes]");
            return false;
        }

        return true;
    }

    // this method will always disconnect
    private String[] connectAndList() throws Exception

    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        String[] files = null;

          try {
               pcc.ftp.connect(pcc.host, pcc.port);
               pcc.ftp.login(pcc.userId,pcc.userPasswd);
               files = (String[])CommUtils.execScript("ls()",pcc.scriptPath +" ls()", pcc.scriptMgr);
               pcc.ftp.logout();
          }
          finally {
              pcc.ftp.disconnect();
          }

          return files;
    }



    private byte[] getFile(String name) throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        try {

            if (pcc.singleConnect) {
                synchronized (KeyHash.getLock(pcc.lock)) {
                    return connectAndGet(name);
                }
            } else {
                return connectAndGet(name);
            }

        }
        catch (Exception e) {
            throw new ProcessingException("Failed to get file ["+ name + "]: " + e.getMessage());
        }
    }

    // this method will always disconnect
    private byte[] connectAndGet(String name) throws Exception
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        // set the name of the file so that the script can access it
        pcc.fileBean.setName(name);

        byte[] data = null;

        try {
              pcc.ftp.connect(pcc.host, pcc.port);
              pcc.ftp.login(pcc.userId,pcc.userPasswd);

              data = (byte[])CommUtils.execScript("get()",pcc.scriptPath +" get()", pcc.scriptMgr);
              pcc.ftp.logout();
          }
          finally {
              pcc.ftp.disconnect();
          }

        return data;
    }



    private void  deleteFile(String name) throws ProcessingException
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        try {

            if (pcc.singleConnect) {
                synchronized (KeyHash.getLock(pcc.lock)) {
                    connectAndDel(name);
                }
            } else {
                connectAndDel(name);
            }

        }
        catch (Exception e) {
            throw new ProcessingException("Failed to delete file ["+ name + "]: " + e.getMessage());
        }
    }


    // this method will always disconnect
    private void connectAndDel(String name) throws Exception
    {
        PerCIDConfiguration pcc = (PerCIDConfiguration)getConfiguration( true );

        // set the name of the file so that the script can access it
        pcc.fileBean.setName(name);

        try {
            pcc.ftp.connect(pcc.host, pcc.port);
            pcc.ftp.login(pcc.userId,pcc.userPasswd);
            CommUtils.execScript("del()",pcc.scriptPath +"del()", pcc.scriptMgr);
            pcc.ftp.logout();
          }
          finally {
              pcc.ftp.disconnect();
          }
    }


    /**
     * Callback allowing sub-classes to provide additional driver configuration.
     *
     * @param  driver  Message-processing driver to configure.
     *
     * @exception  ProcessingException  Thrown if processing can't be performed.
     *
     * @exception  MessageException  Thrown if message is bad.
     */
    protected void configureDriver (MessageProcessingDriver driver)
        throws ProcessingException, MessageException
    {
        PerCIDConfiguration pcc = null;

        String cidTimerName = PersistentProperty.getPropNameIteration( CID_TIMER_PREFIX, 0 );

        if(getPropertyValue( cidTimerName ) != null)
        {
                pcc=(PerCIDConfiguration)getConfiguration( true);
        }
        else
        {
                pcc=(PerCIDConfiguration)getConfiguration(CustomerContext.DEFAULT_CUSTOMER_ID,true);
        }

        if(StringUtils.hasValue(pcc.contextFTPFileNameLoc) )
        {
            MessageProcessorContext mpc = driver.getContext();
            mpc.set(pcc.contextFTPFileNameLoc, pcc.currentFTPFileName);
        }
    }



     // Standalone testing
    // verify that downloaded files match in name and content with the
    // files in the remote server. Copies of the downloaded files are
    // in the directories pointed to by EXCEPTION_DIR_PROP and
    // EXCEPTION_DIR_BAK_PROP.  Ignore the "CommunicationExceptions".

    public static void main(String argv[])
    {
        Debug.enableAll();

        // Get a DB connection
        /*
        try
        {
                DBInterface.initialize(argv[0],argv[1],argv[2]);
        }
        catch(DatabaseException dbe)
        {
            Debug.log(Debug.ALL_ERRORS, " Your database connection is a joke !");
            dbe.printStackTrace();
        }
        */
        if (argv.length < 3 ) {
            Debug.error("Usage : FTPPoller <testcsv> <key> <type>");
            System.exit(1);

        }

        try
        {
            PersistentProperty.loadPropertiesIntoMemory(argv[0]);

            FTPPoller rw = new FTPPoller(argv[1],argv[2]);
            rw.processRequests();

        }
        catch(Exception pe)
        {
            pe.printStackTrace();
        }
    }
 
    private class PropsBean
    {
        public PropsBean ( Map properties )
        {
            this.properties = properties;
        }

        public String get(String propName)
        {
            String value = PropUtils.getPropertyValue(properties, propName);

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "PropsBean.get(): [" + propName + "] = [" + PropUtils.suppressPasswordValue( propName, value ) + "]." );

            return value;
        }

        private Map properties;
    }

    
}

/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.ftp.ssl;

import java.io.*;
import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.communications.*;

import com.nightfire.comms.util.*;

import com.nightfire.comms.util.ftp.FtpException;
import com.nightfire.comms.util.ftp.ssl.*;


/*
 * This class is intended to be the end point for an SPI incoming message.
 * The the FTP server is pooled periodically  and any file found is
 * downloaded and passed on to a Message Processing Driver (MPD) chain as a
 * String. If an exception is thrown by the MPD, then a copy of the file is
 * stored in a directory pointed to by EXCEPTION_DIR_PROP property.
 */
public class FTPPoller extends PollComServerBase
{
    //Specifies path or error directory
    public static final String ERROR_FILE_PROP                 = "ERROR_FILE_DIR";

    //The name of this component
    public static final String CLIENT_NAME_PROP                = "NAME";

    //The IP of the remote FTP(VAN) host
    public static final String REMOTE_HOST_PROP                = "REMOTE_HOST";

    //The port the remote FTP(VAN) host is listening for requests
    public static final String REMOTE_PORT_PROP                = "REMOTE_PORT";

    //UserId for the remote host
    public static final String USER_IDENTITY_PROP              = "USERID";

    //Password of the remote host
    public static final String USER_PASSWORD_PROP              = "PASSWD";

    //Secured Socket Layer (SSL) Mode
    public static final String SSL_MODE_PROP                   = "SSL_MODE";

    //Whether file being retrieved is an EDI message (required)
    public static final String EDI_MODE_PROP                   = "EDI_MODE";

    //Whether to use binary mode for retrieval of files
    public static final String BINARY_MODE_PROP                = "BINARY_MODE";

    //The remote directory to retrieve files from.
    public static final String REMOTE_DIR_PROP                 = "REMOTE_DIR";

    //Whether to bypass firewall 
    public static final String FTP_PASSIVE_MODE_PROP           = "FTP_PASSIVE_MODE";

    //Path to runtime directory
    public static final String RUNTIME_DIR_PROP                = "RUNTIME_DIR";

    //Path to temp directory
    public static final String TEMP_DIR_PROP                   = "TEMP_DIR";

    //Name of private key file
    public static final String SSL_PRIVATE_FILE_PROP           = "SSL_PRIVATE_FILE";

    //Name of filter to use for file matching
    public static final String REMOTE_FILTER_NAME_PROP         = "REMOTE_FILTER_NAME_";

    //Value of filter to use for file matching
    public static final String REMOTE_FILTER_VALUE_PROP        = "REMOTE_FILTER_VALUE_";

    //Path to exception directory
    public static final String EXCEPTION_DIR_PROP              = "EXCEPTION_DIR";

    //Path to processed directory
    public static final String PROCESSED_DIR_PROP              = "PROCESSED_DIR";

    //Whether Poller will call processAsynch with String or Byte[]
    public static final String RETURN_BYTE_PROP                = "RETURN_BYTE";

    //The concrete implementation of FtpSslCommInterface to use
    public static final String CLASSNAME                       = "FTP_CLIENT_CLASS";

    //Specifies a VAN specific property
    public static final String EDI_NAME_PROP                   = "EDI_NAME";

    //Whether to activate connection level and transfer level logging
    public static final String FTP_CONNECT_LAYER_DEBUG_PROP    = "FTP_CONNECT_LAYER_DEBUG";

    // optional field which sets time out if it exists
    public static final String LOGIN_TIMEOUT_PROP              = "LOGIN_TIMEOUT";

    // Property name in message-processor context containing name of FTP file.
    public static final String CONTEXT_FTP_FILE_NAME_LOC_PROP  = "CONTEXT_FTP_FILE_NAME_LOC";

    // Property giving line filter regular expression value.
    public static final String REMOTE_LINE_FILTER_PROP = "REMOTE_LINE_FILTER";


    //FTP Poller
    FtpSslCommInterface ftpssl;
    private String classname;       //fully qualified class that implements FtpSslCommInterface
    private String remoteDir;       //remote directory to retrieve files
    private String exceptionDir;    //path to exception directory
    private String errorFileDir;    //path to error directory
    private String processedDir;    //path to processed directory
    private boolean returnByte;     //whether to pass processAsynch with String or byte[]

    //Connection Information holds information used to establish Ftp Ssl (VAN) Connection
    private ConnectionInfo conInfo                             = null;
    private boolean ediMode;         //whether file retrieved is EDI
    private boolean binaryMode;      //whethet to use ftp binary mode

    //FileInfo Template that remote files are compared for match
    private FileInfo template                                  = null;


    // Name of the current file being processed.
    private String currentFTPFileName                          = null;

    //Property name in context for FTP file name
    private String contextFTPFileNameLoc                       = null;


    /**
     * Constructor
     *
     */
    public FTPPoller (String key, String type) throws ProcessingException
    {
        super(key,type);
        Debug.log(Debug.NORMAL_STATUS, "Initializing the FTPSSLPoller...");

        conInfo = new ConnectionInfo();

        StringBuffer errors = new StringBuffer();
        //configure transfer mode from Persistent Properties

        classname = getRequiredPropertyValue(CLASSNAME,errors);

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller Setting classname: " + classname);
        try {
           // must be called first
           setTransferModeFromProps(errors);

           //configure ConnectionInformation from Persistent Properties
           setConnectionInfoFromProps(errors);

           //configure FTP Poller processing attributes from Persistent Properties
           setProcessingDetailsFromProps(errors);
        } catch (FrameworkException fe) {
           Debug.log(this,Debug.ALL_ERRORS,"FtpPoller: One of the properties has a bad value " + fe.getMessage() );
           throw new ProcessingException(fe + " : " + errors.toString());
        }

        // if there are any missing required properties throw an exception with the missing props.
        if (errors.length() > 0 )
           throw new ProcessingException(errors.toString());

        //construct instance of FTP Component that is used for ftp connectivity
        try
        {
            ftpssl = (FtpSslCommInterface) ObjectFactory.create(classname);
        }
        catch (FrameworkException fe)
        {
            throw new ProcessingException(StringUtils.getClassName(this) + ": " + fe.toString());
        }

        //build Template from Persistent Properties
        buildFtpSslFilterTemplate();

        //location of FTPFileName in context (optional)
        contextFTPFileNameLoc = getPropertyValue(CONTEXT_FTP_FILE_NAME_LOC_PROP);
    }

    
    /**
     * Polls the remote server for any message files, sends them for further processing.
     */
    public void processRequests() throws ProcessingException
    {

        byte[] message = null;

        if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
            Debug.log(Debug.IO_STATUS,"Checking the remote FTPServer for files.");

        FileInfo[] filesToBeRetrieved = null;



        // Get a list of files that match the FileInfo template we build from properties
        try 
        {
            // Make sure access to FTP server is serial
            synchronized (ftpssl.getClass())
            {
                ftpssl.setTransferMode(binaryMode, ediMode);
                ftpssl.login(conInfo);
                filesToBeRetrieved = ftpssl.getFileList(remoteDir,template);
                ftpssl.logout();
            }
        }
        catch (FtpException e)
        {
            Debug.logStackTrace(e);

            //make sure we're logged out
            try 
            {
                ftpssl.logout();
            }
            catch (Exception e1)
            {
                throw new ProcessingException(StringUtils.getClassName(this) + 
                    ": Cannot logout from FTP Server: " + e1.getMessage());
            }

            throw new ProcessingException(e);
        }

        // first check if any file  have been found
        if(filesToBeRetrieved == null) 
            return;

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS,"The number of files in the mbox is " + filesToBeRetrieved.length);

        // For each file found in the current directory ...
        for (int Ix = 0;  Ix < filesToBeRetrieved.length;  Ix ++)
        {
            try 
            {
                synchronized (ftpssl.getClass())
                {
                   message = ftpssl.connectGet(conInfo, binaryMode, ediMode, remoteDir, filesToBeRetrieved[Ix].name, false) ;
                }
           }
            catch (FtpException ftpe)
            {
                if (Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Unable to retrieve filename: " +
                                               filesToBeRetrieved[Ix].name + ftpe.getMessage());
                
                //make sure we log out, VAN will only allow (1) Connection at a time
                try {ftpssl.logout();}
                catch (Exception e)
                {
                    throw new ProcessingException(StringUtils.getClassName(this) + 
                        ": Cannot logout from FTP Server: " + e.getMessage());
                }
            }



            //now try to process
            try
            {
                currentFTPFileName = filesToBeRetrieved[Ix].name;
                if (message != null) 
                {
                    if (returnByte) 
                    {
                        if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
                            Debug.log(Debug.IO_STATUS,"checkForFiles: processing byte[]...");
                        
                        process(null, message);
                    }
                    else
                    {
                        String s = new String (message);
                        
                        if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
                            Debug.log(Debug.IO_STATUS,"checkForFiles: processing String...");
                        
                        process(null, s);
                    }
                }
                if (StringUtils.hasValue(processedDir))
                {
                    // Save the processed file if PROCESSED_DIR is set.
                    saveFile(filesToBeRetrieved[Ix].name, processedDir,  message);
                }
            }
            catch (ProcessingException pe)
            {
                // If we encounter a ProcessingException, save the file in the ExceptionDir
                Debug.logStackTrace(pe);
                Debug.log(Debug.ALL_ERRORS,StringUtils.getClassName(this) +
                                           ": Cannot process file [" + filesToBeRetrieved[Ix].name + "].");
                
                saveFile(filesToBeRetrieved[Ix].name, exceptionDir,  message);
            }
            catch (Exception me)
            {
                // If we encounter a Exception, SE received a message it could not 
                // process and should be placed in the ErrorDirectory.
                Debug.logStackTrace(me);
                Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                            ": Cannot process file [" + filesToBeRetrieved[Ix].name + "].");
                
                saveFile(filesToBeRetrieved[Ix].name, errorFileDir, message);
            }

            if (StringUtils.hasValue(processedDir))
            {
                // Delete file from server if PROCESSED_DIR is set.
                // Now since the message should be saved to processed, exception, error dir,
                // we can safely remove the file from the server.
                try
                {
                    //want to make sure access to FTP server is serial
                    synchronized (ftpssl.getClass())
                    {
                        if ( Debug.isLevelEnabled( Debug.MSG_LIFECYCLE) )
                            Debug.log(Debug.MSG_LIFECYCLE,"checkForFiles: Deleting file [" + filesToBeRetrieved[Ix].name + "] from FTP server...");
                        ftpssl.login(conInfo);
                        ftpssl.deleteFile(remoteDir, filesToBeRetrieved[Ix].name);
                        ftpssl.logout();
                    }
                }
                catch (FtpException e)
                {
                    if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                        Debug.log(Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) + 
                                                       ": Cannot delete file from FTP server.");
                    Debug.logStackTrace(e);
                    
                    //make sure we're logged out
                    try 
                    {
                        ftpssl.logout();
                    }
                    catch (Exception e1)
                    {
                        throw new ProcessingException(StringUtils.getClassName(this) + 
                            ": Cannot logout from FTP Server: " + e1.getMessage());
                    }
                    
                    throw new ProcessingException(e);
                }
            }
            
        }

        if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log(Debug.MSG_LIFECYCLE, StringUtils.getClassName(this) + ": Finished checking for files.");
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
        if(StringUtils.hasValue(contextFTPFileNameLoc) )
        {
            MessageProcessorContext mpc = driver.getContext();
            mpc.set(contextFTPFileNameLoc, currentFTPFileName);
        }
    }


    /**
     * Writes file to appropriate directory/filename as String or byte[]
     *
     * @param  fileName   Name of file
     *
     * @param  dir        Directory
     *
     * @param message     Contents of file
     *
     */
    private void saveFile(String fileName, String dir, Object message)
        throws ProcessingException
    {
        try
        {
            if (message instanceof String)
            {
                if (dir.endsWith(File.separator)) 
                {
                    FileUtils.writeFile(dir + fileName, (String) message);
                }
                else 
                {
                    FileUtils.writeFile(dir + File.separator + fileName, (String) message );
                }
            }
            else if (message instanceof byte[])
            {
                if (exceptionDir.endsWith(File.separator)) 
                {
                    FileUtils.writeBinaryFile(dir + fileName, (byte[]) message);
                }
                else 
                {
                    FileUtils.writeBinaryFile(dir + File.separator + fileName, (byte[]) message );
                }
            }
            else
                throw new ProcessingException(StringUtils.getClassName(this) + ": Cannot write to [" +
                    dir + "] message is not a String or byte[].");
        }
        catch (FrameworkException fe)
        {
            throw new ProcessingException(StringUtils.getClassName(this) + 
                ": Cannot write file." + fe.toString());
        }
    }



    /**
     * Configures FTP Poller processing atrributes
     *
     * @exception ProcessingException Thrown when initialization fails
     */
    private void setProcessingDetailsFromProps(StringBuffer errBuf) throws FrameworkException
    {


        errorFileDir = getRequiredPropertyValue(ERROR_FILE_PROP,errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller Setting directory for error files " + errorFileDir);

        returnByte = StringUtils.getBoolean(getRequiredPropertyValue(RETURN_BYTE_PROP, errBuf));
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller  Setting return byte array: " + returnByte);

        exceptionDir = getRequiredPropertyValue(EXCEPTION_DIR_PROP,errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller Setting execption directory: " + exceptionDir);

        // This property is optional to be compatible with previous versions.
        // If it is not set, the file on server will not be deleted and processed file will not be saved.
        processedDir = getPropertyValue(PROCESSED_DIR_PROP);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller Setting processed directory: " + processedDir);

        remoteDir = getRequiredPropertyValue(REMOTE_DIR_PROP,errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller Setting file remote working directory: " + remoteDir);
    }


    /**
     * Configures Transfer Mode attributes
     *
     * @exception ProcessingException Thrown when initialization fails
     */
    private void setTransferModeFromProps(StringBuffer errBuf) throws FrameworkException
    {
        conInfo.sslMode = StringUtils.getBoolean(getRequiredPropertyValue(SSL_MODE_PROP, errBuf) );
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting sslMode: " + conInfo.sslMode);

        ediMode = StringUtils.getBoolean(getRequiredPropertyValue(EDI_MODE_PROP, errBuf) );
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller Setting file content type EDI_MODE: " + ediMode);

        binaryMode = StringUtils.getBoolean(getRequiredPropertyValue(BINARY_MODE_PROP, errBuf) );

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller Setting file content type BINARY_MODE: " + binaryMode);


        // these values are used for checks so make sure they are not null

        if (errBuf.length() > 0 )
           throw new ProcessingException(errBuf.toString());

        // the van always requires binary mode to be true.
        if (ediMode && !binaryMode) {
           errBuf.append(BINARY_MODE_PROP + " property has to be true when " + EDI_MODE_PROP + " property is true.\n");
        }
    }


    /**
     * Configures Connection Information Object
     *
     * @exception ProcessingException Thrown when initialization fails
     */
    private void setConnectionInfoFromProps(StringBuffer errBuf) throws FrameworkException
    {
        conInfo.host = getRequiredPropertyValue(REMOTE_HOST_PROP, errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting host: " + conInfo.host);

        String temp = getPropertyValue(REMOTE_PORT_PROP);
        if (StringUtils.hasValue(temp) )
           conInfo.port = (short) StringUtils.getInteger(temp);

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting port: " + conInfo.port);

        conInfo.userid =  getRequiredPropertyValue(USER_IDENTITY_PROP,errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting userid: " + conInfo.userid);

        conInfo.password = getRequiredPropertyValue(USER_PASSWORD_PROP, errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
        	Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting password: ");

        temp = getPropertyValue(FTP_PASSIVE_MODE_PROP);
        if (StringUtils.hasValue(temp ))
           conInfo.passive =  StringUtils.getBoolean(temp);

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting passive: " + conInfo.passive);

        if (conInfo.sslMode )
           conInfo.runtimeDir = getRequiredPropertyValue(RUNTIME_DIR_PROP,errBuf);
        else
           conInfo.runtimeDir = " ";

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting runtimeDir: " + conInfo.runtimeDir);

        conInfo.tempDir = getRequiredPropertyValue(TEMP_DIR_PROP,errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting tempDir: " + conInfo.tempDir);

        if (conInfo.sslMode )
           conInfo.privateFileName = getRequiredPropertyValue(SSL_PRIVATE_FILE_PROP,errBuf);
        else
           conInfo.privateFileName = " ";

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting privateFileName: " + conInfo.privateFileName);

        if (conInfo.sslMode )
           conInfo.ediName = getRequiredPropertyValue(EDI_NAME_PROP,errBuf);
        else
           conInfo.ediName = " ";

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting ediName: " + conInfo.ediName);

        temp = getPropertyValue(FTP_CONNECT_LAYER_DEBUG_PROP);
        if (StringUtils.hasValue(temp) ) {
           conInfo.ftpConnectLayerDebug = StringUtils.getBoolean(temp);
        }

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpPoller -> ConnectionInfo: Setting ftpConnectLayerDebug: " + conInfo.ftpConnectLayerDebug);

        temp = getPropertyValue(LOGIN_TIMEOUT_PROP);
        if( StringUtils.hasValue(temp) ) {
           conInfo.loginTimeout = StringUtils.getInteger(temp);
        }

    }


    private void buildFtpSslFilterTemplate()
    {
        //loop through all the filter properties and extract
        Hashtable filters = new Hashtable();
        // Prime the pump with the first values, even though we'll get them again.
        String filterKey;
        String filterValue;

        for (int i = 0; true ; i++)
        {
            filterKey = getPropertyValue(REMOTE_FILTER_NAME_PROP + i);
            filterValue = getPropertyValue(REMOTE_FILTER_VALUE_PROP + i);

            if (!StringUtils.hasValue(filterKey) && !StringUtils.hasValue(filterValue) )
               break;

            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "FTPPoller -> Adding filter: [" + filterKey  + "," + filterValue + "]");
            
            filters.put(filterKey, filterValue);
        }

        //if no filters were set then construct default FileInfo
        if (filters.isEmpty()) 
        {
            template = new FileInfo();
            template.setFileType(FileInfo.FILE);
        }
        else 
        {
            template = new FileInfo(filters);
            template.setFileType(FileInfo.FILE);
        }

        template.lineRegExp = getPropertyValue( REMOTE_LINE_FILTER_PROP );
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
        try
        {
            DBInterface.initialize(argv[0],argv[1],argv[2]);
        }
        catch(DatabaseException dbe)
        {
            Debug.log(Debug.ALL_ERRORS, " Your database connection is a joke !");
            dbe.printStackTrace();
        }

        try
        {
            FTPPoller rw = new FTPPoller("FTPTEST_ORDER","FTP_POLLER");
        }
        catch(ProcessingException pe)
        {
            pe.printStackTrace();
        }
    }
}

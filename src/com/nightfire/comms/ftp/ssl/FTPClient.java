/*
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.ftp.ssl;

import java.io.*;
import java.sql.*;
import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;


import com.nightfire.comms.util.ftp.FtpException;
import com.nightfire.comms.util.ftp.ssl.*;

/*
 * The FTP client class that usually works as the end point
 * for an outgoing message in a SPI. The filname is generated
 * from concatenation of  TARGET_FILE_PROP and the cuurent time in msec
 * obtained from the Nightfire DateUtils
 */

public class FTPClient extends MessageProcessorBase {
    
    private String filePrefix;        //prefix of file name to be transfered
    private String fileSuffix="";        //suffix of file name to be transfered
    private String workingDir;        //remote directory on FTP Server
    private String classname;         //fully qualified classname of concrete implementation of
                                      //FtpSslCommInterface

    private ConnectionInfo conInfo;   //holds connection information
    private boolean ediMode;          //whether the file contains edi
    private boolean binaryMode;       //whether the file is a binary file, for PGP support
    private FtpSslCommInterface ftpssl; // reference to ftp comm connector


    //Property name in context for FTP file name
    private String contextFTPFileNameLoc;

    //Property name in context for FTP file name suffix
    private String contextFTPFileNameSuffixLoc;


    /**
     * Constructor used by Factory to create this object
     */
    public FTPClient()
    {

    }

    /**
     * Creates a ConnectionInfo object and populates it from the data specified in the
     * Persistent Properties.
     *
     * @param Key  to load properties from Persistent Properties
     *
     * @param Type to load Properties from Persistent Properties
     *
     * @exception ProcessingException Thrown if initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        conInfo = new ConnectionInfo();


        Debug.log(Debug.NORMAL_STATUS, "Initializing the FTPClient.");
        super.initialize(key,type);


        StringBuffer errors = new StringBuffer();
        classname = getRequiredPropertyValue(CLASSNAME,errors);

         try {
           // must be called first
           setTransferModeFromProps(errors);

           //configure ConnectionInformation from Persistent Properties
           setConnectionInfoFromProps(errors);

           //configure FTP Poller processing attributes from Persistent Properties
           setProcessingDetailsFromProps(errors);
        } catch (FrameworkException fe) {
           Debug.log(this,Debug.ALL_ERRORS,"FtpClient: One of the properties has a bad value " + fe.getMessage() + " : " + errors.toString() );
           throw new ProcessingException(fe.getMessage() + " : " + errors.toString());
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



    }

     /**
     * Configures FTP Client processing atrributes
     *
     * @exception ProcessingException Thrown when initialization fails
     */
    private void setProcessingDetailsFromProps(StringBuffer errBuf) throws FrameworkException
    {
       //location of FTPFileName in context (optional)
       contextFTPFileNameLoc = getPropertyValue(CONTEXT_FTP_FILE_NAME_LOC_PROP);

       if (!StringUtils.hasValue(contextFTPFileNameLoc) )
          filePrefix = getRequiredPropertyValue(TARGET_FILE_PROP,errBuf);

       if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
           Debug.log(Debug.IO_STATUS ,"FTPClient -> Setting filePrefix: " + filePrefix);

       //location of FTPFileName suffix in the context (optional)
       contextFTPFileNameSuffixLoc = getPropertyValue(CONTEXT_FTP_FILE_NAME_SUFFIX_LOC_PROP);

       if (!StringUtils.hasValue(contextFTPFileNameSuffixLoc) )
          fileSuffix = getPropertyValue(FILE_SUFFIX_PROP);

       if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
           Debug.log(Debug.IO_STATUS,"FTPClient -> Setting fileSuffix: " + fileSuffix);


       workingDir = getRequiredPropertyValue(REMOTE_DIR_PROP,errBuf);
         if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
           Debug.log(Debug.IO_STATUS ,"FTPClient -> Setting file content type workingDir: " + workingDir );

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
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting host: " + conInfo.host);

        String temp = getPropertyValue(REMOTE_PORT_PROP);
        if (StringUtils.hasValue(temp) )
           conInfo.port = (short) StringUtils.getInteger(temp);

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting port: " + conInfo.port);

        conInfo.userid =  getRequiredPropertyValue(USER_IDENTITY_PROP,errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting userid: " + conInfo.userid);

        conInfo.password = getRequiredPropertyValue(USER_PASSWORD_PROP, errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
        	Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting password..");

        temp = getPropertyValue(FTP_PASSIVE_MODE_PROP);
        if (StringUtils.hasValue(temp ))
           conInfo.passive =  StringUtils.getBoolean(temp);

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting passive: " + conInfo.passive);

        if (conInfo.sslMode )
           conInfo.runtimeDir = getRequiredPropertyValue(RUNTIME_DIR_PROP,errBuf);
        else
           conInfo.runtimeDir = " ";

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting runtimeDir: " + conInfo.runtimeDir);

        conInfo.tempDir = getRequiredPropertyValue(TEMP_DIR_PROP,errBuf);
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting tempDir: " + conInfo.tempDir);

        if (conInfo.sslMode )
           conInfo.privateFileName = getRequiredPropertyValue(SSL_PRIVATE_FILE_PROP,errBuf);
        else
           conInfo.privateFileName = " ";

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting privateFileName: " + conInfo.privateFileName);

        if (conInfo.sslMode )
           conInfo.ediName = getRequiredPropertyValue(EDI_NAME_PROP,errBuf);
        else
           conInfo.ediName = " ";

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting ediName: " + conInfo.ediName);

        temp = getPropertyValue(FTP_CONNECT_LAYER_DEBUG_PROP);
        if (StringUtils.hasValue(temp) ) {
           conInfo.ftpConnectLayerDebug = StringUtils.getBoolean(temp);
        }

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting ftpConnectLayerDebug: " + conInfo.ftpConnectLayerDebug);

        temp = getPropertyValue(LOGIN_TIMEOUT_PROP);
        if( StringUtils.hasValue(temp) ) {
           conInfo.loginTimeout = StringUtils.getInteger(temp);
        }

    }
    
    /**
     * Verifies input is of type byte[] and if so will try to FTP using FtpSslCommInterface.
     *
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  NVPair[] Name value Pair or null 
     *
     * @exception  ProcessingException  Thrown if processing fails due bad connection.
     *
     * @exception  MessageException  Thrown if bad message
     */

    public NVPair[] execute ( MessageProcessorContext mpcontext, java.lang.Object input ) throws MessageException, ProcessingException
    {

        InputStream is  = null;
        String transFileName = null;
        byte[] buf;


	      if(input==null) return null;

        // try to do an ftp put if the input is of type string or byte[]
        if ( input instanceof byte[] || input instanceof String )
        {
          if ( input instanceof String ) {
            if ( Debug.isLevelEnabled( Debug.EDI_DATA) )
                Debug.log(Debug.EDI_DATA, "The input is of String converting to byte[]...");
            buf = ((String) input ).getBytes();
            if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
                Debug.log(Debug.IO_STATUS, "FTPClient -> Constructing InputStream...");
            is = new ByteArrayInputStream(buf);
          }
          else {
             if ( Debug.isLevelEnabled( Debug.EDI_DATA) )
                 Debug.log(Debug.EDI_DATA, "The input is of byte[] type");
             if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
                 Debug.log(Debug.IO_STATUS, "FTPClient -> Constructing InputStream...");
             is = new ByteArrayInputStream((byte[]) input);

          }
        }

        //not a byte[] or String so bail....
        else
        {
          throw new ProcessingException ("ERROR: FTPClient: Input given is not of type byte[].");
        }

        //if InputStream is null we got trouble
        if (is == null)
           throw new ProcessingException ("ERROR: FTPClient, Problem building ByteArrayInputStream.");


         //Get FTP file name
         if (StringUtils.hasValue(contextFTPFileNameLoc) ) {
            //Get FTP file name from context
            transFileName = (String) mpcontext.get(contextFTPFileNameLoc);
         }
         else {
            //Build fileName
            String appendTag = DateUtils.getDateToMsecAsString ();

            transFileName = filePrefix + appendTag;
         }

         //Build file suffix now
         if (StringUtils.hasValue(contextFTPFileNameSuffixLoc))
         {
            transFileName += mpcontext.getString(contextFTPFileNameSuffixLoc);
         }
         else
         if (StringUtils.hasValue(fileSuffix))
         {
            transFileName += fileSuffix;
         }

         //else send file to FTP Server
         if ( Debug.isLevelEnabled( Debug.IO_STATUS) )
             Debug.log(Debug.IO_STATUS, "FTPClient ->  FTP'ing..");


         try {
             ftpssl.connectSend(conInfo, binaryMode, ediMode, workingDir, transFileName, is);
         }

         catch (FtpException ftpe)
         {
             Debug.log(Debug.ALL_ERRORS, "FTPClient ->ERROR : FTP ERROR " + ftpe.toString() );
             throw ( new ProcessingException(ftpe.getMessage()));
         }

         if ( Debug.isLevelEnabled( Debug.MSG_LIFECYCLE) )
             Debug.log(Debug.MSG_LIFECYCLE," FtpClient -> FTP successfully completed");
         return formatNVPair(input);
    }

    /**
     * Configures Transfer Mode attributes
     *
     * @exception ProcessingException Thrown when initialization fails
     */
    private void setTransferModeFromProps(StringBuffer errBuf) throws FrameworkException {

        conInfo.sslMode = StringUtils.getBoolean(getRequiredPropertyValue(SSL_MODE_PROP, errBuf) );
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient -> ConnectionInfo: Setting sslMode: " + conInfo.sslMode);

        ediMode = StringUtils.getBoolean(getRequiredPropertyValue(EDI_MODE_PROP, errBuf) );
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient Setting file content type EDI_MODE: " + ediMode);

        binaryMode = StringUtils.getBoolean(getRequiredPropertyValue(BINARY_MODE_PROP, errBuf) );

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "FtpClient Setting file content type BINARY_MODE: " + binaryMode);


        // these values are used for checks so make sure they are not null

        if (errBuf.length() > 0 )
           throw new ProcessingException(errBuf.toString());

        // the van always requires binary mode to be true.
        if (ediMode && !binaryMode) {
           errBuf.append(BINARY_MODE_PROP + " property has to be true when " + EDI_MODE_PROP + " property is true.\n");
        }

    }


    // returns value of number property.
    // returns def if there is no property called prop
    private int getIntProp(String prop, int def)
    {
       String s = (String) adapterProperties.get(prop);

       if (s == null)
          return def;
       else
          return (Integer.parseInt(s) );

    }


    /*
    *   stand alone test
    *   writes two files to the remote directory, one of them
    *   is a binary file of 512 bytes that contain all the 8-bit
    *   ASCII characters.  The second file is a string that contains
    *   all the printable characters in a PC keyboard and is 95 bytes.
    */

    static public void main(String argv[])
    {

      Debug.enableAll();


      // get a DB connection
      try
      {
        DBInterface.initialize(argv[0],argv[1],argv[2]);
      }
      catch( DatabaseException dbe)
      {
        Debug.log(Debug.ALL_ERRORS, " Your database connection is a joke !");
        dbe.printStackTrace();
      }

      FTPClient rw = new FTPClient();

      String[] pn = {"NOBODY"};
      rw.toProcessorNames = pn;
          
          try
          {

            byte[] ba = new byte[512];
            for(int i = 0; i< 512 ; i +=2 )
            {
              ba[i] = (byte) i;
              ba[i+1] = (byte) i;
            }

            // initialize
            rw.initialize("FTPTEST_ORDER","FTP_CLIENT");
            NVPair[] nvp =  rw.execute(null,ba);
            String s = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ~!@#$%^&*()_+`-={}|[]:;<>?,./\\\"";
            byte[] test = s.getBytes();
            nvp =  rw.execute(null, test);

          }
          catch(MessageException me)
          {
            me.printStackTrace();
          }
          catch(ProcessingException pe)
          {
            pe.printStackTrace();
          }
    }

    //Prefix of name of files to be transfered
    public static final String TARGET_FILE_PROP = "TARGET_FILE";

    //Suffix of name of files to be transfered
    public static final String FILE_SUFFIX_PROP = "FILE_SUFFIX";

    //The IP of the remote FTP(VAN) host
    public static final String REMOTE_HOST_PROP  = "REMOTE_HOST";

    //The port the remote FTP(VAN) host is listening for requests
    public static final String REMOTE_PORT_PROP  = "REMOTE_PORT";

    //UserId for the remote host
    public static final String USER_IDENTITY_PROP  = "USERID";

    //Password of the remote host
    public static final String USER_PASSWORD_PROP  = "PASSWD";

    //Secured Socket Layer (SSL) Mode
    public static final String SSL_MODE_PROP  = "SSL_MODE";

    //Whether file being retrieved is an EDI message (required)
    public static final String EDI_MODE_PROP  = "EDI_MODE";

    //Whether to use binary mode for retrieval of files
    public static final String BINARY_MODE_PROP  = "BINARY_MODE";

    //Path to remote directory
    public static final String REMOTE_DIR_PROP   = "REMOTE_DIR";

    //Whether to bypass firewall
    public static final String FTP_PASSIVE_MODE_PROP  = "FTP_PASSIVE_MODE";

    //Path to runtime directory
    public static final String RUNTIME_DIR_PROP  = "RUNTIME_DIR";

    //Path to temp directory, (note needed by Compress FTP)
    public static final String TEMP_DIR_PROP  = "TEMP_DIR";

    //Name of private key file
    public static final String SSL_PRIVATE_FILE_PROP  = "SSL_PRIVATE_FILE";

    //The concrete implementation of FtpSslCommInterface to use
    public static final String CLASSNAME = "FTP_CLIENT_CLASS";

    //Specifies a VAN specific property
    public static final String EDI_NAME_PROP = "EDI_NAME";

    //Whether to activate connection level and transfer level logging
    public static final String FTP_CONNECT_LAYER_DEBUG_PROP = "FTP_CONNECT_LAYER_DEBUG";

    // optional field which sets time out if it exists
    public static final String LOGIN_TIMEOUT_PROP  = "LOGIN_TIMEOUT";

    // Property name in message-processor context containing name of FTP file.
    public static final String CONTEXT_FTP_FILE_NAME_LOC_PROP  = "CONTEXT_FTP_FILE_NAME_LOC";

    // Property name in message-processor context containing suffix of FTP file name.
    public static final String CONTEXT_FTP_FILE_NAME_SUFFIX_LOC_PROP  = "CONTEXT_FTP_FILE_NAME_SUFFIX_LOC";

    // Flag for selecting FTP file name
    public static final String USE_CONTEXT_FTP_FILE_NAME_PROP = "USE_CONTEXT_FTP_FILE_NAME";
    
}

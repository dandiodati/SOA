/*
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.ftp.java;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.ibm.bsf.*;


import com.nightfire.comms.util.ftp.*;



/*
 * The FTP client class that usually works as the end point
 * for an outgoing message in a SPI. The filename is generated
 * from concatenation of FILE_PREFIX_PROP,current time in msec, and 
 * FILE_SUFFIX_PROP or the name is taken from the context.
 *
 */

public class FTPClient extends MessageProcessorBase implements FTPConstants {
    
 /**
     * The message-processor context location
     * to obtain the file suffix.
     *
     */
    public static final String CONTEXT_FTP_FILE_NAME_SUF_LOC_PROP  = "CONTEXT_FTP_FILE_NAME_SUFFIX_LOC";


    public static final String FILE_PREFIX_PROP = "FILE_PREFIX";
    public static final String FILE_SUFFIX_PROP = "FILE_SUFFIX";
    
    private String filePrefix;        //prefix of file name to be transfered
    private String fileSuffix="";        //suffix of file name to be transfered
    private String remoteDir;        //remote directory on FTP Server
    private String remoteDirLoc;
    
  
    //Property name in context for FTP file name suffix
    private String contextFTPFileNameSuffixLoc;

    private String script, scriptPath;
    //private String getScript, getScriptPath;
    //private String delScript, delScriptPath;

      
    // Name of the current file being processed.
    private String currentFTPFileName                          = null;

    //location in context for FTP file name
    private String contextFTPFileNameLoc                       = null;

    private boolean singleConnect = false;
    
    private BSFManager scriptMgr;
    

    private FtpDataConnect ftp;
    
    private String charEncoding = null;

    private String userId, userPasswd;

    private String host;
    
    private int port;

    private String lock  = null;
    
    
    
    private CommUtils.FileBean fileBean = new CommUtils.FileBean();

    private int socTimeOut = DEFAULT_TIMEOUT;

       
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
      
        scriptMgr = new BSFManager();
        

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Initializing the FTPClient.");
        
        super.initialize(key,type);

        StringBuffer errBuf = new StringBuffer();

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
         
             
        scriptPath = getRequiredPropertyValue(SCRIPT_PROP, errBuf);
          
        
        contextFTPFileNameLoc = getPropertyValue(CONTEXT_FTP_FILE_NAME_LOC_PROP);        
        if (!StringUtils.hasValue(contextFTPFileNameLoc) )
            filePrefix = getRequiredPropertyValue(FILE_PREFIX_PROP,errBuf);
 

        remoteDirLoc = getPropertyValue(CONTEXT_REMOTE_DIR_LOC_PROP);
        
        if (!StringUtils.hasValue(remoteDirLoc) )
            remoteDir = getRequiredPropertyValue(REMOTE_DIR_PROP, errBuf);

 
        contextFTPFileNameSuffixLoc = getPropertyValue(CONTEXT_FTP_FILE_NAME_SUF_LOC_PROP);


       if (!StringUtils.hasValue(contextFTPFileNameSuffixLoc) )
          fileSuffix = getPropertyValue(FILE_SUFFIX_PROP);

        
        String scmStr = getRequiredPropertyValue(SINGLE_CONNECT_MODE_PROP, errBuf);
        
        singleConnect = StringUtils.getBoolean(scmStr, false);

        String socTimeOutStr   = getPropertyValue(CONNECT_TIMEOUT_PROP);
        if (StringUtils.hasValue(socTimeOutStr) )
            socTimeOut = Integer.parseInt(socTimeOutStr) * 1000;

        host = getRequiredPropertyValue(REMOTE_HOST_PROP);
        try {
            port = StringUtils.getInteger(getRequiredPropertyValue(REMOTE_PORT_PROP, errBuf));
        }
        catch (FrameworkException e) {
            errBuf.append("Invalid value for port property : " + e.getMessage() );
        }
        
        userId = getRequiredPropertyValue(USER_IDENTITY_PROP);
        userPasswd = getRequiredPropertyValue(USER_PASSWORD_PROP);
               
        charEncoding = getPropertyValue(CHAR_ENCODING_PROP);

        // note this key formation must be the same as FTPClient's
        lock = host +"-" + port;

        // if there are any missing required properties throw an exception with the missing props.
        if (errBuf.length() > 0 )
           throw new ProcessingException(errBuf.toString());


 try {
                        
            if ( sslMode)
                ftp = FtpDataConnectFactory.createSSLFTP(sslConfig);
            else
                ftp = FtpDataConnectFactory.createFTP();
            

             ftp.setDefaultTimeout(socTimeOut);
             
             FtpDataBean ftpBean = new FtpDataBean(ftp);
             scriptMgr.declareBean(SCRIPT_FTPBEAN, ftpBean, FtpDataBean.class);
             scriptMgr.declareBean(SCRIPT_PROPSBEAN, new PropsBean(), PropsBean.class);
             scriptMgr.declareBean(SCRIPT_FILEBEAN, fileBean, CommUtils.FileBean.class);

             script = FileUtils.readFile(scriptPath);
          
             // load the script
             scriptMgr.eval("javascript", scriptPath, 0, 0, script);
                           
             
        } catch (BSFException e){
            throw new ProcessingException("Failed to setup scripting support : " + e.getMessage() );
        } catch (FrameworkException f ){
            throw new ProcessingException("Failed to read script files : " + f.getMessage() );
        }


    }

  
    
    /*
     * Ftps a file to a remote server.
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
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
     {
        
         if (msgObj == null) 
             return null;


         String transFileName = null;
	    
        byte[] data = CommUtils.convertToByte(msgObj.get(), charEncoding);

        //Get FTP file name from context or 
        // create the file name from the defined prefix and a date
         if (StringUtils.hasValue(contextFTPFileNameLoc) )
            transFileName = (String) context.get(contextFTPFileNameLoc);
         else 
             transFileName = filePrefix + DateUtils.getDateToMsecAsString ();

         //add file suffix if needed
         if (StringUtils.hasValue(contextFTPFileNameSuffixLoc))
             transFileName += context.getString(contextFTPFileNameSuffixLoc);
         else if (StringUtils.hasValue(fileSuffix))
            transFileName += fileSuffix;

         if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
             Debug.log(Debug.MSG_LIFECYCLE, "FTPClient: Sending file [" + transFileName +"]");


         // if the remote dir is set via the context replace
         // the remote dir with the current value.
         // otherwise use the default remoteDir that was set in the initialize
         // method
         if (StringUtils.hasValue(remoteDirLoc))
             remoteDir = (String) context.get(remoteDirLoc);

         // Re-get the user/password and resolve any context references.
         userId = getRequiredResolvedPropertyValue( USER_IDENTITY_PROP, context, msgObj );
         userPasswd = getRequiredResolvedPropertyValue( USER_PASSWORD_PROP, context, msgObj );
         
         putFile(transFileName, data, remoteDir);
         
         if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
             Debug.log(Debug.MSG_LIFECYCLE," FTPClient: FTP successfully completed");
         return formatNVPair(msgObj);
    }


    private void putFile(String name, byte[] data, String dir) throws ProcessingException
    {
         
        try {
            
            if (singleConnect) {
                synchronized (KeyHash.getLock(lock)) {
                    connectAndPut(name, data, dir);
                }
            } else {
                connectAndPut(name, data, dir);
            }
                
        }
        catch (FtpException e) { 
            throw new ProcessingException("Failed to put file ["+ name + "]: " + e.getMessage());
        }
              
    }

    // this method will always disconnect
    private void connectAndPut(String name, byte[] data, String dir) throws FtpException
    {

        // set the name of the file so that the script can access it
        fileBean.setName(name);
        fileBean.setData(data);
        fileBean.setDir(dir);
        
         
        try {

            ftp.connect(host, port);

            ftp.login(userId,userPasswd);

            CommUtils.execScript("put()", scriptPath +" put()", scriptMgr); 

            ftp.logout();
          }
          finally {
              ftp.disconnect();
          } 
    }


    private class PropsBean 
    {
        public String get(String propName)
        {
            return getPropertyValue(propName);
        }
    }

   
}

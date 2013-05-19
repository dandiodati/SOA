/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.ftp.java;


public interface FTPConstants
{
    
    public static final String SCRIPT_FTPBEAN = "FTP";
    public static final String SCRIPT_PROPSBEAN = "Props";
    public static final String SCRIPT_FILEBEAN = "File";

 
    /**
     * The IP of the remote FTP host
     *
     */
    public static final String REMOTE_HOST_PROP                = "REMOTE_HOST";


    /**
     * The port the remote FTP host is listening for requests
     *
     */
    public static final String REMOTE_PORT_PROP                = "REMOTE_PORT";

      
    /**
     * UserId for the remote host
     *
     */
    public static final String USER_IDENTITY_PROP              = "USERID";

    
    /**
     * Password of the remote host
     *
     */
    public static final String USER_PASSWORD_PROP              = "PASSWD";

    
    /**
     * Indicates if ssl should be used.
     */
    public static final String SSL_MODE_PROP                   = "SSL_MODE";

    /**
     * Indicates the type of char encoding to use for text files
     * The default is standard ASCII encoding.
     *
     */
    public static final String CHAR_ENCODING_PROP              ="CHAR_ENCODING";
    
    /**
     * The script to use to perform file listings, get, delete
     *
     */
    public static final String SCRIPT_PROP             = "FTP_SCRIPT";

        
    /**
     * The remote directory to retrieve files from.
     *
     */
    public static final String REMOTE_DIR_PROP              = "REMOTE_DIR";


    /**
     * The message-processor context location
     * to place the current file name getting processed at.
     *
     */
    public static final String CONTEXT_REMOTE_DIR_LOC_PROP  = "CONTEXT_REMOTE_DIR_LOC";


    //Whether to bypass firewall 
    //   public static final String PASSIVE_MODE_PROP        = "PASSIVE_MODE";

   //Whether to use binary mode or ascii
   // public static final String BIN_MODE_PROP        = "BIN_MODE";

       
    /**
     * The message-processor context location
     * to place the current file name getting processed at.
     *
     */
    public static final String CONTEXT_FTP_FILE_NAME_LOC_PROP  = "CONTEXT_FTP_FILE_NAME_LOC";

    
   
   /**
    * The passphrase used to protect the keystore which holds
    * user certificates and private keys.
    * Note all private keys must use the same password.
    *
    */
    public static final String PASSPHRASE_PROP      = "KEYSTORE_PASSPHRASE";

   /**
    * The location of the keystore.
    */
    public static final String KEYSTORE_PATH_PROP   = "KEYSTORE_PATH";

  /**
    * The location of the trusted keystore.
    */
    public static final String TRUSTSTORE_PATH_PROP   = "TRUSTSTORE_PATH";

   /**
    * The type of keystore
    */
    public static final String KEYSTORE_TYPE_PROP   = "KEYSTORE_TYPE";

    
    /**
     * The ssl version to use during handshake.
     *
     */
    public static final String SSL_VERSION_PROP          ="SSL_VERSION";
    

    /**
     * Indicates that this poller to run in single connect mode. 
     * This forces all ftp connections in this gateway
     * to run in a serialized fashion. Some ftp servers do not
     * support multiple connections to the same account at the same time.
     *
     * For example connections to the VAN should have this set to true.
     *
     */
    public static final String SINGLE_CONNECT_MODE_PROP = "SINGLE_CONNECT_MODE";

    /**
     * Indicates the number of seconds before a socket times out.
     *
     */
    public static final String CONNECT_TIMEOUT_PROP = "CONNECTION_TIMEOUT";

    /**
     * Indicates if downloaded files should be deleted from the remote server
     *
     */
    public static final String DELETE_DOWNLOADED_PROP = "DELETE_DOWNLOADED_FILES";
 
    /**
     * Hold the regular expression to filter remote files on
     *
     */
    public static final String REMOTE_LINE_FILTER_PROP = "REMOTE_LINE_FILTER";

    /**
     * Default number of seconds(120) for a timeout.
     */ 
    public static final int DEFAULT_TIMEOUT = 120000;
    
}

/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */
 

package com.nightfire.comms.util.ftp.ssl;

/**
 * This class holds connection info for establishing a connection to an ftp server.
 * @param  host  The FTP server host
 * @param  ediName  A name used as a hash into the certificate.
 *                  use the name NIGHTFIRE_CONNECT
 * @param  controlport  FTP port to connect to. (default is 21)
 * @param  dataPort     The ftp data port to transfer data over.  (default is 20)
 * @param  account   The account used by van only
 * @param  userid    The user id
 * @param  password    The user password
 * @param  sslMode     true turns on ssl, false turns off ssl. (default is true)
 * @param  passive     true, turns on passive ftp operation. Use if you need to connection
 *                     through a fire wall.  (default is off)
 * @param  runtimeDir the runtime directory which contains the certificate and private key.
 * @param  tempDir    a temp directory used by the api
 * @param privateFileName the file name of the private key file
 * @param ftpConnectLayerDebug - used to turn on the Easy Access C lib debuging.
 * @param ftpLayerLog -the ftp level log
 * @param xferLayerLog - the easy access layer log.
 * @param  obj         An object used for future parameters that may need to be passed in.
 */

 public class ConnectionInfo
{
   public String ediName;
   public String host;
   public short port;
   public String userid;
   public String password;
   public boolean sslMode;
   public boolean passive;
   public String runtimeDir;
   public String tempDir;               // temp directory of where to store temp files.
   public String privateFileName;


   public boolean ftpConnectLayerDebug;
   public String ftpLayerLog;
   public String xferLayerLog;

   public int loginTimeout;


  public ConnectionInfo()
  {
   ediName = null;
   host = null;
   port = 21;
   userid = null;
   password = null;
   sslMode  = true;
   passive  = true;
   runtimeDir = null;

   privateFileName = null;
   ftpConnectLayerDebug = false;
   ftpLayerLog = "eaFtpLayer.log";
   xferLayerLog = "eaXferLayer.log";


   loginTimeout = 1800; // default in seconds


  }

}

/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp.ssl;

import java.io.InputStream;
import com.nightfire.framework.util.*;
import com.nightfire.comms.util.*;

/**
 * This class is the provides the java side of the JNI interface into a C API.
 */

public class FtpSslLayerInterconnect
{

   static {
      System.loadLibrary("FtpSslLayerInterconnectNative");
   }

/**
 * creates an instance of the ftpObj object. This is used as a reference to
 * the active connection. This needs to be called before any other call.
 *
 * @returns 0 if the call fails otherwise returns a pointer to the ftpObj.
 *  this pointer needs to be passed into all other functions.
 *
 * The passing of this pointer was needed so that the C++ side of the JNI
 * interface can keep a reference to an object that needs to be passed into
 * other functions.
 */
  public native int  createFtpObj(      String ediName, int networkStyle, String runtimeDir,
                                        String tempDir, String privateFileName,
                                        String dirSep,
                                        String ftpLayerLog,
                                        String xferLayerLog,
                                        boolean ftpLayerDebug);


  /**
   * makes a connection to the server. createFtpObj has to be called before this.
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param hostip the host to connect to
   * @param hostip2 a backup server to connect to
   * @param controlPort the port to use for the connection
   * @param ssl true to activate ssl, false to turn ssl off.
   * @param passive true to turn passive mode on
   *
   */
  public native int connect(int ftpObj, String hostip, String hostip2, short controlPort, boolean ssl,
                             boolean passive);

  /**
   * login into server
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param userId the user id to user during login
   * @param passwd the password to use
   * @param newPasswd the new password to change to. Used by VAN
   * @param proxyLogin use to log into a proxy first( set to false for our usage)
   */
  public native int login(int ftpObj, String userId, String passwd, String newPasswd, boolean proxyLogin);

  /**
   * log out of server
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   */
  public native int logout(int ftpObj );

  /**
   * gets the last error message after one of these methods fail
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   */
  public native String getLastMessage(int ftpObj);

  /**
   * gets  directory listing and writes it to a file.
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * dirFile the file to write the directory listing to.
   */
  public native int dirList(int ftpObj, String dirFile);

  /**
   * changes directories for a put
   *
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param userId used for the van to change directories.
   * @param userClass the directory or class in case of a VAN
   * @param edi if true then sending to a van, other sending to a regular ftp site
   */
  public native int cdForPut(int ftpObj, String userId, String userClass, boolean edi);

  /**
   * changes directories for a get
   *
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param userId used for the van to change directories.
   * @param userClass the directory or class in case of a VAN
   * @param edi if true then sending to a van, other sending to a regular ftp site
   */
  public native int cdForGet(int ftpObj, String userId, String userClass, boolean edi);

  /**
   * deletes a file
   *
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param fileName file to delete from server
   * @param userClass the directory or class in case of a VAN
   */
  public native int delete(int ftpObj, String fileName, String userClass);

   /**
   * gets a file
   *
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param clientFileName writes the file obtained from the server to this file.
   * @param serverFileName the name of the file on the server to obtain.
   * @param ascii true if using ascii mode, otherwise false
   */
  public native int get(int ftpObj, String clientFileName, String serverFileName, boolean ascii);

  /**
   * sends a file
   *
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param clientFileName file to send to server
   * @param serverFileName the name to give file sent to the server.
   * @param ascii true if using ascii mode, otherwise false
   */
  public native int put(int ftpObj, String clientFileName, String serverFileName, boolean ascii);

  /**
   * sets transfer mode for a get
   *
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param ascii true if using ascii mode, otherwise false
   * @param edi true if getting edi, else false
   */
  public native int setGetMode(int ftpObj, boolean ascii, boolean edi);

  /**
   * sets transfer mode for a put
   *
   * @param ftpObj a reference to the ftp obj obtained from createFtpObj.
   * @param ascii true if using ascii mode, otherwise false
   * @param secure true if using a secure connection
   */
  public native int setPutMode(int ftpObj, boolean secure, boolean ascii);

}

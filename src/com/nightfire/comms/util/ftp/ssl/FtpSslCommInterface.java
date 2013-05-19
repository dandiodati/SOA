/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp.ssl;

import java.io.InputStream;
import com.nightfire.framework.util.*;
import com.nightfire.comms.util.*;
import com.nightfire.comms.util.ftp.FtpException;

/**
 *  Ftp ssl interface
 */
public  interface  FtpSslCommInterface
{
  /**
  * Connects and logs into the ftp server.
  * <b> NOTE you need to call setTransferType first before establishing a connectiom
  * if you don't want the default settings. </b>
  *
  * @param ci information for establishing the connection
  *
  * @exception   FtpException
  */
    public  void login
                        (
                            ConnectionInfo ci
                        )
                        throws FtpException ;


  /**
  * loges current user off of the ftp server.
  *
  *
  * @exception   FtpException if there is an error logging out.
  */
    public  void logout()
                        throws FtpException ;


  /**
  * Set the data transfer type
  * indicates if the current file is binary or txt
  * and if it is edi.
  *
  *
  * The default type is set to false(text) and edi is set to false.
  * <b>NOTE: To connect to the van you need to set bin to true, and edi to true.</b>
  * @param  edi  true if using edi, else false
  * @param  type  binary if true, else txt
  * @exception   FtpException
  */
    public  void setTransferMode(boolean type, boolean edi)
    throws FtpException ;

    /**
  * get the data transfer type
  *
  * @returns true if binary mode, else false
  * @exception   FtpException
  */
    public  boolean isBinTransferMode()
    throws FtpException ;

     /**
  * get the file type mode
  *
  *   The default mode is true
  * @param  edi  true if using edi, else false
  * @exception   FtpException
  */
    public  boolean isEdiTransferMode()
    throws FtpException ;

  /**
  * Writes a file to the FTP server.
  *   1. change working directory.
  *   2. set transfer type (bin or ascii)
  *   3. send msg, creating file with fileName on server
  *   4. change working directory back to previous dir on server.
  *
  * @param  fileName    The name of the file to create on the server
  * @param  workingDir    The directory where the file would be created.
  * @param  msg   The InputStream that is the content of the file (user has to close stream)
  *
  *
  * @exception   FtpException
  */
    public  void sendFile
                        (
                            String workingDir,
                            String serverFileName,
                            InputStream msg
                        )
                        throws FtpException ;

  /**
  * Writes a file to the FTP server. Stateless call.
  *
  *   1. login into ftp server
  *   2. change working directory.
  *   3. set transfer type (bin or ascii)
  *   4. send msg, creating file with fileName on server
  *   5. logout
  *
  * @param ci information for establishing the connection
  * @param  type        transfer type, true for binary and false for ascii
  * @param  fileName    The name of the file to create on the server
  * @param  workingDir    The directory where the file would be created.
  * @param  msg   The InputStream that is the content of the file (user has to close stream)
  *
  * @exception   FtpException
  */
    public void connectSend
                        (
                            ConnectionInfo ci,
                            boolean type,
                            boolean edi,
                            String workingDir,
                            String fileName,
                            InputStream msg
                        )
                        throws FtpException ;
  /**
  * Gets matched file list from the FTP server
  *
  * @param  workingDir  The directory which would be looked into
  *
  * @param  template    The template for choosing files(uses match by example). So
  *                     by default it will match everything including directories.
  *                     <I>1. When using a normal ftp server, can match by filename, user,group, file type,
  *                     permission, or size.
  *                     2. When using the VAN, you can match by account, userid, or file type.</I>
  *
  * @return             The array of FileInfos
  *
  * @exception          FtpException
  */
    public  FileInfo[] getFileList
                        (
                            String workingDir,
                            FileInfo template
                        )
                        throws FtpException;

  /**
  * Read a file from the FTP server
  *   1. change working directory.
  *   2. set transfer type (bin or ascii)
  *   3. get file called fileName
  *   4. change working directory back to root on server.
  *
  * @param  workingDir    The name of the directory that contains the file
  * @param  fileName    The name of the file to be retrieved
  * @return       The FileInfo.
  *
  * @exception   FtpException
  */
    public  byte[] getFile
                        (
                            String workingDir,
                            String serverFileName
                        )
                        throws FtpException ;

 /**
  * Read a file from the FTP server. Stateless call.
  *   1. login into ftp server
  *   2. change working directory.
  *   3. set transfer type (bin or ascii)
  *   4. get file named fileName
  *   5. logout
  * @param  ci information for establishing the connection
  * @param  type        transfer type, true for binary and false for ascii
  * @param  workingDir    The name of the directory that contains the file
  * @param  fileName    The name of the file to be retrieved
  * @param delete       if true, then it deletes the file from the server after retrieving it.
  *                     if false, it leaves the file on the server.
  * @return       The FileInfo.
  *
  * @exception   FtpException
  */
    public  byte[] connectGet
                        (
                            ConnectionInfo ci,
                            boolean type,
                            boolean edi,
                            String workingDir,
                            String fileName,
                            boolean delete
                        )
                        throws FtpException ;

  /**
  * Deletes the file from the FTP server
  *
  * @param  workingDir    The directory that contains the file
  *
  * @param  fileName    The name of the file to be deleted
  *
  * @exception   FtpException  if there was an error deleting file.
  */
    public  void deleteFile
                        (
                            String workingDir,
                            String fileName
                        )
                        throws FtpException;

}

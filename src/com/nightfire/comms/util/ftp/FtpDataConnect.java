/*
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp;

import java.io.InputStream;
import com.nightfire.framework.util.*;
import com.nightfire.comms.util.*;
import com.nightfire.comms.util.ftp.FtpException;

/**
 *  Ftp data connect interface
 */
public  interface  FtpDataConnect
{
 
    public static final short ASCII_MODE = 0;
    public static final short BIN_MODE = 1;
 
    /**
     * Connect to a remote ftp server
     *
     * @param site The ip address on name of server to connect to.
     * @exception FtpException if an error occurs
     */
    public void connect(String site) throws FtpException;
    
    /**
     * Connect to a remote ftp server
     *
     * @param site The ip address on name of server to connect to.
     * @param port The ftp port to connect at.
     * @exception FtpException if an error occurs
     */
    public void connect(String site, int port) throws FtpException;
    
    
    /**
     * Logs into an ftp server
     *
     * @param user The user name
     * @param passwd The password
     * @exception FtpException if an error occurs during login
     */
    public void login(String user, String passwd) throws FtpException;

    /**
     * Logs current user off of the ftp server.
     *
     *
     * @exception   FtpException if there is an error logging out.
     */
    public  void logout() throws FtpException;
  

    /**
     * Logs current user and disconnects from the server
     *
     * @exception   FtpException if there is an error disconnecting out.
     */
    public  void disconnect() throws FtpException;


    
    /**
     * Sets the file type transfer mode.
     *
     * @param mode One of the static modes defined(ASCII_MODE or BIN_MODE)
     */
    public  void setTransferMode(short mode) throws FtpException;
    

    /**
     * Returns the current transfer mode.
     *
     * @param mode One of the static modes defined(ASCII_MODE or BIN_MODE)
     */
    public short getTransferMode();
  

    
    /**
     * Change the current directory on a remote server
     *
     * @param dir The directory to change to.
     * @exception FtpException if an error occurs
     */
    public void changeDir(String dir) throws FtpException;

    /**
     * Places a single file onto the remote ftp server with the specified name.
     *
     *
     * @param fileName The file name to use.
     * @param  msg   The message as a byte array.
     *
     *
     * @exception   FtpException
     */
    public  void putFile ( String fileName, byte[] msg)
                        throws FtpException;

    /**
     * Gets a file list from the FTP server
     *
     * @return             The array of with one file listing per line.
     *
     * @exception          FtpException
     */
    public  String[] getFileList ()
                        throws FtpException;

    /**
     * Gets a file list from the FTP server
     *
     * @param path The dir or file to get a listing for.
     * @return             The array of with one file listing per line.
     *
     * @exception          FtpException
     */
    public String[] getFileList(String path) throws FtpException;
    

    /**
     * Gets a name list from the FTP server
     *
     * @return             The array of with one file name per entry.
     *
     * @exception          FtpException
     */
    public  String[] getNameList ()
                        throws FtpException;


    /**
     * Get a file from the FTP server.
    
     * @param  fileName    The name of the file to be retrieved
     * @return       An array of bytes that can hold a binary or ascci data.
     *
     * @exception   FtpException If there is an error obtaining the file.
     */
    public  byte[] getFile(String fileName) throws FtpException ;

 
    /**
     * Deletes the file from the FTP server
     *
     * @param  fileName    The name of the file to be deleted
     *
     * @exception   FtpException  If there was an error deleting the file.
     */
    public  void deleteFile (String fileName) throws FtpException;

    /**
     * Prints the current working directory
     *
     * @return The directory path
     * @exception FtpException if an error occurs
     */
    public String printWorkingDir() throws FtpException;

    /**
     * Sets ftp PASV mode to true.
     * This forces ftp to user a single port for control information and
     * data. The default is active mode which uses two different ports.
     * This can also be enabled to allow easier access through a firewall.
     * <b>NOTE: This must be called after logging onto a remote server.</B>
     *
     * @param bool If true passive move is turned on, other wise active mode
     * is used.
     */
    public void setPassiveMode(boolean bool);

    /**
     * Indicates if passive mode is set.
     *
     * @return true if passive mode is on otherwise false.
     */
    public boolean isPassiveMode();

    /**
     * Sends a NOOP command to remote server. This is useful for
     * preventing timeouts.
     *
     * @return true if the successfully completed,false if not.
     * @exception FtpException if an error occurs
     */
    public boolean sendNoOp() throws FtpException;


    /**
     * Sends a site specific command to a remove ftp server.
     *
     * @param args The arguments to a site command.
     * @exception FtpException if an error occurs
     */
    public void sendSiteCommand(String args) throws FtpException;

    /**
     * Send a ftp command
     *
     * @param command The ftp command to issue.
     * @param args The arguments to the command.
     * @return The reply code returned by the server.
     * @exception FtpException if an error occurs
     */
    public int sendCommand(String command, String args) throws FtpException;
    

    /**
     * Sets the default timeout for ftp connections.
     *
     * <b>NOTE: This must be called before calling connect(...);</b>
     *
     * @param millisec The time in milliseconds.
     */
    public void setDefaultTimeout(int millisec);
    
    
    /**
     * Returns the default timeout for the current object.
     *
     * @return an <code>int</code> value
     */
    public int getDefaultTimeout();
    
}

/*
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp;

import java.io.InputStream;
import com.nightfire.framework.util.*;
import com.nightfire.comms.util.*;
import com.nightfire.comms.util.ftp.FtpException;
import com.ibm.bsf.*;
import java.util.*;



/**
 * This is a ftp bean that can be plugged into the BSF framework
 * for scripting support. It provides a wrapper around
 * a FtpDataConnect object.
 */
public class FtpDataBean
{
    private FtpDataConnect fdc;
    
    public FtpDataBean(FtpDataConnect fdc)
    {
        this.fdc = fdc;    
    }
    
      
    /**
     * Changes the connection to BINARY transfer mode.
     *
     * @exception FtpException if an error occurs
     */
    public void bin() throws FtpException
    {
        fdc.setTransferMode(FtpDataConnect.BIN_MODE);
    }
    
    
    /**
     * Changes the connection to ASCII transfer mode.
     *
     * @exception FtpException if an error occurs
     */
    public void ascii() throws FtpException
    {
        fdc.setTransferMode(FtpDataConnect.ASCII_MODE);
    }
    
    
    /**
     * Changes the current working directory.
     *
     * @param dir The directory to change to.
     * @exception FtpException if an error occurs
     */
    public void cd(String dir) throws FtpException
    {
       
        fdc.changeDir(dir);
       
    }
    

    /**
     * Places a file on the remote server.
     *
     * @param fileName The file name to use.
     * @param msg The data for a file.
     * @exception FtpException if an error occurs
     */
    public void put (String fileName, byte[] msg) throws FtpException
    {
        fdc.putFile(fileName, msg);
    }
    

    
    /**
     * Does a file listing on the remote server.
     *
     * @return a <code>String[]</code> with file listings or null
     * if there are no files.
     * @exception FtpException if an error occurs
     */
    public String[] ls () throws FtpException
    {
       
        
        String[] lines = fdc.getFileList();
        
        
        if (lines == null || lines.length == 0)
            return null;
      
        return lines;
        
    }
    

    /**
     * Obtains a file from the remote server.
     *
     * @param fileName The name of the file to obtain.
     * @return The data of the file.
     * @exception FtpException if an error occurs
     */
    public byte[] get(String fileName) throws FtpException
    {
        return fdc.getFile(fileName);
    }
    

    /**
     * Deletes a file from the remote server.
     *
     * @param fileName The file name to delete.
     * @exception FtpException if an error occurs
     */
    public  void del (String fileName) throws FtpException
    {
        fdc.deleteFile(fileName);
    }
    

    /**
     * Returns the current working directory on the remote server.
     *
     * @return a <code>String</code> value
     * @exception FtpException if an error occurs
     */
    public String pwd() throws FtpException
    {
        return fdc.printWorkingDir();
    }
    

    /**
     * Allows custom ftp commands to be issues to the remote server.
     *
     * @param command The command to issues.
     * @param args Arguments to the command.
     * @exception FtpException if an error occurs
     */
    public void quote(String command, String args) throws FtpException
    {
        fdc.sendCommand(command, args);
    }
    

    /**
     * Changes the remote server to passive mode.
     * 
     *
     * @exception FtpException if an error occurs
     */
    public void pasv() throws FtpException
    {
        fdc.setPassiveMode(true);
    }

       
  
 }

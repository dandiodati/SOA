/*
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp;

import java.io.InputStream;
import com.nightfire.framework.util.*;
import com.nightfire.comms.util.*;
import com.nightfire.comms.util.ftp.FtpException;
import org.apache.commons.net.ftp.*;
import java.io.*;
import java.net.*;
import org.apache.commons.net.*;
import org.apache.commons.net.ftp.parser.DefaultFTPFileEntryParserFactory;





/**
 *  Normal ftp implementation of the
 */
public class FtpDataClient implements FtpDataConnect
{

    protected FTPClientExt ftpC;
    private short transferMode;
    private boolean pasv = false;

    public FtpDataClient()
    {
        transferMode = ASCII_MODE;
        ftpC = new FTPClientExt();//FTP.pasv();
        ftpC.addProtocolCommandListener(new CommandListener() );

    }

    public void setDefaultTimeout(int millisec)
    {
        if ( Debug.isLevelEnabled(Debug.MSG_STATUS) && millisec > 0 )
            Debug.log(Debug.MSG_STATUS, "Setting ftp timeout to [" + ((double)millisec/1000) +"] seconds");

        ftpC.setDefaultTimeout(millisec);
    }

    public int getDefaultTimeout()
    {
        return ftpC.getDefaultTimeout();

    }


    /**
     * Connect to a remote ftp server
     *
     * @param site The ip address on name of server to connect to.
     * @exception FtpException if an error occurs
     */
    public void connect(String site) throws FtpException
    {
        connect(site,-1);
    }

    /**
     * Connect to a remote ftp server
     *
     * @param site The ip address on name of server to connect to.
     * @param port The ftp port to connect at.
     * @exception FtpException if an error occurs
     */
    public void connect(String site, int port) throws FtpException
    {
        try {
            //int timeout = ftpC.getDefaultTimeout();

            // if no port is specified set it to default
            if ( port == -1)
                port = FTP.DEFAULT_PORT;

             Debug.log(Debug.MSG_STATUS, "Connecting to server [" + site +"] and port ["+ port +"]");


            ftpC.connect(site, port);


            int reply = ftpC.getReplyCode();


            if(!FTPReply.isPositiveCompletion(reply)) {
                ftpC.disconnect();
                Debug.error("FTP server refused connection.");
            }

            //ftpC.setSoTimeout(timeout);


        }
        catch (IOException i) {
            if (ftpC.isConnected() ) {
                try {
                    ftpC.disconnect();
                }
                catch (IOException ie){
                    Debug.warning("Could not disconnect " + ie.getMessage());
                }

            }

            throw new FtpException(i.getMessage());

        }


        // set the default transfer mode to ascii when first connecting
        transferMode = ASCII_MODE;


    }


    /**
     * Logs into an ftp server
     *
     * @param user The user name
     * @param passwd The password
     * @exception FtpException if an error occurs during login
     */
    public void login(String user, String passwd) throws FtpException
    {
        boolean success;

        try {
             Debug.log(Debug.MSG_STATUS, "Loging into server, username [" + user +"] and passwd [******]");
            success = ftpC.login(user,passwd);

            if (!success)
                throw new IOException("Unsuccessful operation.");

        }
        catch (IOException i){
            Debug.error("Failed to log in to server.");

            throw new FtpException(i.getMessage());
        }
    }






    /**
     * Logs current user off of the ftp server.
     *
     *
     * @exception   FtpException if there is an error logging out.
     */
    public  void logout() throws FtpException
    {
        boolean success ;

        try {
             Debug.log(Debug.MSG_STATUS, "Logging out of server");
            success = ftpC.logout();

            if (!success)
                throw new IOException("Unsuccessful operation.");

        }
        catch (IOException i){
            Debug.error("Failed to log out of server.");

            throw new FtpException(i.getMessage());
        }
    }

    /**
     * Logs current user and disconnects from the server
     *
     * @exception   FtpException if there is an error disconnecting out.
     */
    public  void disconnect() throws FtpException
    {

        try {
            if (ftpC.isConnected() ) {
                Debug.log(Debug.MSG_STATUS, "Disconnecting from server");
                ftpC.disconnect();
            }


        }
        catch (IOException i){
            Debug.error("Failed to disconnect from server.");

            throw new FtpException(i.getMessage());
        }
    }


    /**
     * Sets the file type transfer mode.
     *
     * @param mode One of the static modes defined(ASCII_MODE or BIN_MODE)
     */
    public  void setTransferMode(short mode) throws FtpException
    {

        boolean success;

        try {
            if (mode == ASCII_MODE) {
                success = ftpC.setFileType(FTP.ASCII_FILE_TYPE);
                transferMode = ASCII_MODE;
                 Debug.log(Debug.MSG_STATUS, "Setting transfer mode to ASCII");
            }
            else if (mode == BIN_MODE) {
                success = ftpC.setFileType(FTP.IMAGE_FILE_TYPE);
                transferMode = BIN_MODE;
                Debug.log(Debug.MSG_STATUS, "Setting transfer mode to BINARY");
            }
            else
                throw new FtpException("Invalid transfer mode set");


            if (!success)
                throw new IOException("Unsuccessful operation.");

        }
        catch (IOException i){
            Debug.error("Failed to set transfer mode on server.");

            throw new FtpException(i.getMessage());
        }


    }


    /**
     * Returns the current transfer mode.
     *
     * @param mode One of the static modes defined(ASCII_MODE or BIN_MODE)
     */
    public short getTransferMode()
    {
        return transferMode;
    }





    /**
     * Change the current directory on a remote server
     *
     * @param dir The directory to change to.
     * @exception FtpException if an error occurs
     */
    public void changeDir(String dir) throws FtpException
    {

        boolean success;

        try {
            //Debug.log(Debug.MSG_STATUS, "Changing directory to [" + dir+"]");

            success = ftpC.changeWorkingDirectory(dir);

            if (!success)
                throw new IOException("Unsuccessful operation.");

        }
        catch (IOException i){
            Debug.error("Failed to change directories on server: " + i.getMessage() );

            throw new FtpException(i.getMessage());
        }
    }


    /**
     * Places a single file onto the remote ftp server with the specified name.
     *
     *
     * @param fileName The file name to use.
     * @param  msg The message as a byte array.
     *
     *
     * @exception   FtpException
     */
    public  void putFile ( String fileName, byte[] msg)
        throws FtpException
    {
         BufferedInputStream buf = new BufferedInputStream(
                                        new ByteArrayInputStream(msg)
                                        );
         boolean success;

        try {
            Debug.log(Debug.MSG_STATUS, "Putting file [" + fileName +"] on server.");

            success = ftpC.storeFile(fileName, buf);

            if (!success)
                throw new IOException("Unsuccessful operation.");

        }
        catch (IOException i){
            Debug.error("Failed to place file [" + fileName+ "] on remote server: " + i.getMessage() );

            throw new FtpException(i.getMessage());
        } finally {
            try {
                buf.close();
            }
            catch (IOException i) {
                Debug.warning("Could not close stream while sending a file : " + i.getMessage() );
            }
        }

    }


    public  String[] getFileList ()
                        throws FtpException
    {
        return getFileList(null);
    }


   public  String[] getFileList (String path)
                        throws FtpException
    {
        FTPFile[] files = null;
        String[] lines = null;

        try {
			if (StringUtils.hasValue(ftpC.getSystemName()) 
						&& ftpC.getSystemName().equalsIgnoreCase("UNKNOWN Type: L8"))
			{
				
				FTPFileListParser pFactory = (FTPFileListParser) new DefaultFTPFileEntryParserFactory()
							.createFileEntryParser("org.apache.commons.net.ftp.parser.EnterpriseUnixFTPEntryParser");
				
				if ( path == null){
					files = ftpC.listFiles(pFactory);
				}
				else{
					files = ftpC.listFiles(pFactory,path);
				}
			}
			else 
			{
				if ( path == null)
					files = ftpC.listFiles();
				else
					files = ftpC.listFiles(path);
			}
        }
        catch (IOException i){
            Debug.error("Failed to do file listing on server");

            throw new FtpException(i.getMessage());
        }

		int length = 0;
        for(int i = 0; i < files.length;i++) {
			if (files[i] != null) {
				length++;
			}
		}

        if(Debug.isLevelEnabled(Debug.IO_STATUS) )
           Debug.log(Debug.IO_STATUS, " Out of ["+files.length+"] files, [" + length +"] are valid files and [" + length +"] are skipped");

        if(files != null)
            lines = new String[length];
        else
            return null;



        if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS, " Raw Listings :\n");

        for(int i = 0, j = 0; i < files.length;i++) {
			if (files[i] != null) {
               try{
                    lines[j++] = files[i].getRawListing();

				if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
					Debug.log(Debug.MSG_STATUS, "[" + lines[i] +"]\n");
			}
               catch(ArrayIndexOutOfBoundsException aoe)
               {
                   Debug.log(Debug.ALL_ERRORS, " Error in processing file from listing, hence skipping current file: " +aoe.getMessage() +"\n");
               }
            }
        }

        return lines;

    }


    public  String[] getNameList ()
                        throws FtpException
    {
        String[] names = null;

        //Debug.log(Debug.MSG_STATUS, "Getting file name list.");
        try {
            names = ftpC.listNames();

        }
        catch (IOException i){
            Debug.error("Failed to do file name listing on server");

            throw new FtpException(i.getMessage());
        }

        return names;

    }


    public  byte[] getFile(String fileName) throws FtpException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BufferedOutputStream buf = new BufferedOutputStream(baos);
        byte[] data = null;
        boolean success;

        try {
            Debug.log(Debug.MSG_STATUS, "Getting file [" + fileName +"] from server.");
            success = ftpC.retrieveFile(fileName, buf);
 			buf.flush();
           if (!success)
                throw new IOException("Unsuccessful operation.");

            data = baos.toByteArray();
        }
        catch (IOException i){
            Debug.error("Failed to retrieve file [" + fileName+ "] on remote server: " + i.getMessage() );

            throw new FtpException(i.getMessage());
        } finally {
            try {
                buf.close();
            }
            catch (IOException i) {
                Debug.warning("Could not close stream while retrieving a file : " + i.getMessage() );
            }
        }
        return data;

    }



    public  void deleteFile (String fileName) throws FtpException
    {

        boolean success = false;

        try {
            Debug.log(Debug.MSG_STATUS, "Deleting file [" + fileName +"] from server.");
            success = ftpC.deleteFile(fileName);

            if (!success)
                throw new IOException("Unsuccessful operation.");


        }
        catch (IOException i){
            Debug.error("Failed to delete file on server: " + i.getMessage() );

            throw new FtpException(i.getMessage());
        }

    }



    public String printWorkingDir() throws FtpException
    {
        String pwd;

        try {
            pwd = ftpC.printWorkingDirectory();

        }
        catch (IOException i){
            Debug.error("Failed to print working directory on server.");

            throw new FtpException(i.getMessage());
        }

        return pwd;

    }



    public void setPassiveMode(boolean bool)
    {

      if (bool) {
          ftpC.enterLocalPassiveMode();
          pasv = true;
      }
      else {
          ftpC.enterLocalActiveMode();
          pasv = false;
      }

    }



    public boolean isPassiveMode()
    {
        return pasv;
    }



    public boolean sendNoOp() throws FtpException
    {
        boolean result;

        try {
            //Debug.log(Debug.MSG_STATUS, "Sending NOOP");

            result = ftpC.sendNoOp();

        }
        catch (IOException i){
            Debug.error("Error while sending NOOP command " + i.getMessage() );

            throw new FtpException(i.getMessage());
        }

        return result;

    }


    public void sendSiteCommand(String args) throws FtpException
    {

        boolean success;

        try {
            // Debug.log(Debug.MSG_STATUS, "Sending SITE command, args [" + args+"]");
            success = ftpC.sendSiteCommand(args);

            if (!success)
                throw new IOException("Unsuccessful operation.");
        }
        catch (IOException i){
            Debug.error("Error while sending SITE command " + i.getMessage() );

            throw new FtpException(i.getMessage());
        }

    }


    public int sendCommand(String command, String args) throws FtpException
    {
        int code;

        try {
            //Debug.log(Debug.MSG_STATUS, "Sending command [" + command +"], args [" + args+"]");
            code = ftpC.sendCommand(command, args);

            if(FTPReply.isNegativePermanent(code))
                throw new IOException("Unsuccessful operation [" + code +"].");
        }
        catch (IOException i){
            Debug.error("Error while sending command [" + command +"] " + i.getMessage() );

            throw new FtpException(i.getMessage());
        }

        return code;


    }

    protected void fireCommandSentEvent(String command, String message)
    {
        ftpC.getEventPusher().fireCommandSent(command, message);

    }

    protected void fireReplyReceivedEvent(int code, String message)
    {
        ftpC.getEventPusher().fireReplyReceived(code, message);
    }




    /**
     * This class listens for all command issues to the server and
     * replys from the server.
     *
     */
    private class CommandListener implements ProtocolCommandListener
    {
        public void protocolCommandSent(ProtocolCommandEvent e)
        {
            String message = e.getMessage();

            // mask out the password so that it does not show up in the log file
            if ( e.getCommand().equalsIgnoreCase("PASS") )
                message = "PASS ********";

            Debug.log(Debug.MSG_STATUS, "Sending command [" + message+"]");
        }

        public void protocolReplyReceived(ProtocolCommandEvent e)
        {

            int code = e.getReplyCode();

            if(FTPReply.isNegativePermanent(code) ||
               FTPReply.isNegativeTransient(code) )
                Debug.warning("Negative Reply Received: " + e.getMessage());
        else
            Debug.log(Debug.MSG_STATUS,"Positive Reply Received: " + e.getMessage() );
        }
    }


    /**
     * Inner class which adds support for a factory method (doAfterConnect)
     * and allows modification of the socket used.
     *
     */
    protected final class FTPClientExt extends FTPClient
    {
        public ProtocolCommandSupport getEventPusher()
        {
            return  _commandSupport_;
        }


    }

}


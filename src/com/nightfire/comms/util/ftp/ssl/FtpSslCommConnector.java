/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp.ssl;

import java.util.*;
import java.io.*;

import com.nightfire.framework.util.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.comms.util.*;
import com.nightfire.comms.util.ftp.FtpException;

/**
 *  The class provides ftp ssl connectivity. refer to FtpSslCommInterface for
 * description of methods
 */
public class FtpSslCommConnector implements FtpSslCommInterface
{
  protected FtpSslLayerInterconnect interConn; //JNI interface
  private boolean binMode = false;    // connection type set to false for ascii
  private boolean ediMode = false;
  protected boolean connection = false;  // indicates whether there is a cuurent connection or not.
  protected String dirSep   = null;
  protected ConnectionInfo cInfo;
  private int ftpObj;

  private static int MILLIPERSEC = 1000;   // millseconds per sec

  private boolean transferTypeSet = false;
  //private static final int IGNSTYLE = 2;

  private static final int NOTCREATED = 0;

  private static final int FAILED = 0;

  private static final String ROOT = "/";
  // network settings for easy access api
  private static final int NETWORKSTYLE_FTP_SSL = 3;
  private static final int NETWORKSTYLE_FTP = 0;
  private static final int NETWORKSTYLE_VAN = 4;
  private int networkStyle = NETWORKSTYLE_FTP_SSL;

  /**
   * Default constructor
   * sets the file separator for local file system and
   * initilizes ftpobj to null.
   */
  public FtpSslCommConnector()
  {
     interConn = new FtpSslLayerInterconnect();
     dirSep = System.getProperty("file.separator");

     ftpObj = NOTCREATED;
  }


  /**
  * Connects and logs into the ftp server.
  *
  * @param ci information for establishing the connection
  * Call setFileTransferType before executing.
  *
  * @exception   FtpException
  */
    public  void login( ConnectionInfo ci )
    throws FtpException
    {

      String ended;

      synchronized(this.getClass()) {

      Debug.log(this, Debug.IO_STATUS, "FtpSsslCommConnector::login -logging into server " + ci.host + " as user " + ci.userid);

            if (ci.sslMode == true) {
               networkStyle =  NETWORKSTYLE_VAN;
            } else {
               networkStyle = NETWORKSTYLE_FTP;
            }

             // creates a ftp connection object to get a reference to the C side of the jni

             ftpObj = interConn.createFtpObj(  ci.ediName, networkStyle, ci.runtimeDir,
                                        ci.tempDir, ci.privateFileName,
                                        dirSep, ci.ftpLayerLog,
                                        ci.xferLayerLog,
                                        ci.ftpConnectLayerDebug);

           // checks if the call had an error
           if (ftpObj == NOTCREATED) {
              cInfo = null;
              throw new FtpException("FtpSslCommConnector: ERROR creating FTP object: " +
                    interConn.getLastMessage(ftpObj) );

           }

          cInfo = ci;

            // change all file separators to correct format and add separator at the end
           cInfo.tempDir = checkFileSep(cInfo.tempDir);
           cInfo.runtimeDir = checkFileSep(cInfo.runtimeDir);
           cInfo.ftpLayerLog = checkFileSep(cInfo.ftpLayerLog);
           cInfo.xferLayerLog = checkFileSep(cInfo.xferLayerLog);
           cInfo.privateFileName = checkFileSep(cInfo.privateFileName);


            if (cInfo == null | ftpObj == NOTCREATED) {
                throw new FtpException("FtpSslCommConnector:: login -Connection info is null!");
              }

             // make a connection to the server
             //Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:: login thread - connecting to server");
             int ret = interConn.connect(ftpObj, cInfo.host, cInfo.host, cInfo.port , cInfo.sslMode,
                                   cInfo.passive);

            // check for error
             if (ret == FAILED) {
              // connection = false;
              ended = "FtpSslCommConnector: ERROR connecting to ftp server: " +
                        interConn.getLastMessage(ftpObj);
                 throw new FtpException(ended);
              }

               Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:: sending user name and password");
              //login into server
              ret = interConn.login(ftpObj, cInfo.userid, cInfo.password, new String(""), false);

              //again check for error
              if (ret == FAILED) {
                 // connection = false;
                 ended = "FtpSslCommConnector: ERROR logging into ftp server: " +
                        interConn.getLastMessage(ftpObj);
                 throw new FtpException(ended);
              }

               Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:: Successfully connected to server");

      }
    }

   /**
  * logges current user off of the ftp server.
  *
  *
  * @exception   FtpException if there is an error logging out.
  */
    public  void logout()
    throws FtpException
    {

       synchronized(this.getClass()) {
       // check if we are logged in
       if (ftpObj == NOTCREATED ) {
         throw new FtpException("FtpSslConnector::ERROR not logged in to server.");
       }

       Debug.log(this, Debug.IO_STATUS, "FtpSsslCommConnector::logout -logging out of server " + cInfo.host);

       //log out
       int ret = interConn.logout(ftpObj);
       //if error throw exception else reset connection info object and ftp connection obj.
       if (ret == FAILED) {
          throw new FtpException("FtpSslCommConnector: ERROR logging out: " +
              interConn.getLastMessage(ftpObj) );
       }
       else {
           cInfo = null;
           ftpObj = NOTCREATED;
       }

       }
    }



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
    throws FtpException
    {
          binMode = type;
          ediMode = edi;
    }
    /**
  * get the data transfer type
  *
  * @returns true if binary mode, else false
  * @exception   FtpException
  */
    public  boolean isBinTransferMode()
    throws FtpException
    {
       return(binMode);
    }

     /**
  * get the file type mode
  *
  *   The default mode is true
  * @param  edi  true if using edi, else false
  * @exception   FtpException
  */
    public  boolean isEdiTransferMode()
    throws FtpException
    {
       return(ediMode);
    }


  /**
  * Writes a file to the FTP server.
  *   1. change working directory.
  *   2. set transfer type (bin or ascii)
  *   3. send msg, creating file with fileName on server
  *   4. change working directory back to root on server.
  *
  * @param  fileName    The name of the file to create on the server
  * @param  workingDir    The directory where the file would be created.
  * @param  msg   The InputStream that is the content of the file.
  *  InputStream is not closed by this method.
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
    throws FtpException
    {

      synchronized(this.getClass()) {
      int ret;

      //check if a connection has been established
       if (ftpObj == NOTCREATED ) {
         throw new FtpException("FtpSslConnector::ERROR not logged in to server.");
       }

      // remove ending file separator
      if (workingDir.endsWith(ROOT) ) {
         workingDir = workingDir.substring(0, workingDir.length() - 1);
      }

      String sourceFile = writeTempFile(cInfo.tempDir, "TMP_" + serverFileName, msg);

      // change into working dir
      cd(workingDir,true);

      //set transfer mode
      ret = interConn.setPutMode(ftpObj,cInfo.sslMode, !binMode);

      if (ret == FAILED) {
         throw new FtpException("FtpSslCommConnector:sendFile -Error setting transfer mode: "
           + interConn.getLastMessage(ftpObj) );
      }

      Debug.log(this, Debug.IO_STATUS, "FtpSslCommConnector::sendFile - Sending file: " + serverFileName);
      //send file
      ret = interConn.put(ftpObj, sourceFile, serverFileName, !binMode );

      if (ret == FAILED) {
         throw new FtpException("FtpSslCommConnector:sendFile -Error sending file: "
           + interConn.getLastMessage(ftpObj) );
      }

      // remove temp file
      try {
         FileUtils.removeFile(sourceFile);
      } catch (FrameworkException fe) {
         throw new FtpException("FtpSslCommConnector:sendFile - ERROR removing temp file " + sourceFile + ": " + fe.toString() );
      }

          cdBack(workingDir, true);
      }
    }

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
  * @param  msg   The InputStream that is the content of the file (note user has to close stream)
  *
  * @exception   FtpException
  */
    public void  connectSend
                        (
                            ConnectionInfo ci,
                            boolean bin,
                            boolean edi,
                            String workingDir,
                            String serverFileName,
                            InputStream msg
                        )
                        throws FtpException
    {
       synchronized(this.getClass()) {

          FtpSslCommConnector fsc = new FtpSslCommConnector();
          try {

             fsc.setTransferMode(bin, edi);
             fsc.login(ci);
             fsc.sendFile(workingDir,serverFileName, msg);
             fsc.logout();
          } catch (FtpException f) {
                try {
                   fsc.logout();
                } catch (FtpException f2) {
                   Debug.log(this,Debug.IO_WARNING, "FtpSslCommConnector::ConnectSend - Not logged in");
                }
               throw new FtpException (f.toString());
          }


       }

    }
  /**
  * Gets matched file list from the FTP server
  *
  * @param  workingDir  The directory which would be looked into
  *
  * @param  template    The template for choosing files
  *
  * @return             The array of FileInfos
  *
  * @exception          FtpException
  */
    public  FileInfo[] getFileList( String workingDir, FileInfo template )
    throws FtpException
    {
       int rtn;

       synchronized(this.getClass()) {
        if (ftpObj == NOTCREATED ) {
         throw new FtpException("FtpSslConnector::ERROR not logged in to server.");
       }

       FileInfo fileInfo = null;

       String lstFile = createUniqueFile(cInfo.tempDir + "DIRLIST");

       cd(workingDir, false);

       // get a list of all files and directories from a file.
       rtn = interConn.dirList(ftpObj, lstFile);

       if (rtn == FAILED) {
         throw new FtpException("FtpSslCommConnector:getFileList -Error getting file list "
           + " : " + interConn.getLastMessage(ftpObj) );
       }

       Debug.log(this,Debug.IO_STATUS, "ftpSslCommConnector::getFileList - Reading file list obtained from server");

       String fileStr;
       try {
          fileStr = FileUtils.readFile(lstFile);
       } catch (FrameworkException fe) {
          throw new FtpException("FtpSSLCommConnector::getFileList - ERROR reading dir temp file " + lstFile +
                                  ": " + fe.toString() );
       }

           cdBack(workingDir, false);
         // if edi then parse a VAN file listing, else parse a normal file listing
         if (ediMode) {
            return ( parseIGNFileList(fileStr, template) );
         }
         else {
            return (parseNormalFileList(fileStr, template) );
         }

       }
    }

    /**
     * Parsing a file listing that you would get from a regular ftp or
     * ftp ssl server.
     * @param list a string of the file listing contents.
     * @param template the template to match against.
     */
    protected FileInfo[] parseNormalFileList(String list, FileInfo template)
    throws FtpException {

       FileInfo fileInfo;

       StringTokenizer tok = new StringTokenizer(list,"\n");

       Vector vect = new Vector();

       //FileInfo fileArray[] = new FileInfo[tok.countTokens()];

       //loop over every line in the file
       while (tok.hasMoreTokens() ) {


          String line = tok.nextToken();

          StringTokenizer lt = new StringTokenizer(line);

          // skip a line if it doesn't have the correct number of tokens.
          if (lt.countTokens() < 9) {
             Debug.log(this, Debug.IO_WARNING, "WARNING normal format of file list line is nonstandard: " + line);
             continue;
          }
          fileInfo = new FileInfo();


          String tmpPerm = lt.nextToken();
          fileInfo.setFileType( tmpPerm.charAt(0) );    //set file type
          fileInfo.permission = tmpPerm.substring(1);    // set permission without file type
          lt.nextToken(); // skip token
          fileInfo.user       = lt.nextToken();

          fileInfo.group      = lt.nextToken();

          try {
             fileInfo.size       = Integer.parseInt( lt.nextToken() );
          } catch (NumberFormatException n) {
             Debug.log(this,Debug.ALL_ERRORS, "FtpSslCommConnector::parseNormalFileList - Error converting size, setting to zero");
             fileInfo.size = 0;
          }
          fileInfo.month      = lt.nextToken();

          try {
             fileInfo.day        = Integer.parseInt( lt.nextToken() );
           } catch (NumberFormatException n2) {
             Debug.log(this,Debug.ALL_ERRORS, "FtpSslCommConnector::parseNormalFileList - Error converting day, setting to zero");
             fileInfo.size = 0;
          }
          fileInfo.time       = lt.nextToken();
          fileInfo.name       = lt.nextToken();
          fileInfo.regExpName = fileInfo.name;

          // check if the file matches the template
          if ( !matchFile(template, fileInfo) ) 
              continue;

          // Check if the entire line matches the line regular expression.
          if ( !match( line, template.lineRegExp ) )
              continue;

          // Still here?  We have a match.
          vect.addElement(fileInfo);
       }


         // convert object array into fileinfo array
         // this should be changed so that it returns a vector in the future.
         // so that it removes this unnessary step
          Object[] temp = vect.toArray();
          FileInfo array[] = new FileInfo[temp.length];
          for(int j = 0; j < temp.length; j++ ) {
             array[j] = (FileInfo) temp[j];
          }
         return ( array );
    }

      /**
     * Parsing a file listing that you would get from a VAN
     * @param list a string of the file listing contents.
     * @param template the template to match against.
     */
     protected FileInfo[] parseIGNFileList(String list, FileInfo template)
     throws FtpException  {

       FileInfo fileInfo;

       StringTokenizer tok = new StringTokenizer(list,"\n");

       Vector vect = new Vector();



       // loop over every line
       while (tok.hasMoreTokens() ) {


          String line = tok.nextToken();
          StringTokenizer lt = new StringTokenizer(line);

          if (template.lineRegExp != null) {
             // Check if the entire line matches the line regular expression.
             if ( !match( line, template.lineRegExp ) )
                 continue;
             // found a match create a file info object with the name of the file.
             fileInfo = new FileInfo();
             fileInfo.name = lt.nextToken();
          }  else {
             //skip header info
             if (line.indexOf("Filename") >= 0 || line.indexOf("Sender") >= 0 ||
                 line.indexOf("Class") >= 0 || line.indexOf("Filename") >= 0 ||
                 line.indexOf("Size") >= 0 || line.indexOf("Date") >= 0
                 || line.indexOf("Time") >= 0 )  {
                 continue;
             }

             // skip a line if it doesn't have the correct number of tokens.

             //IGN should have a format of :
             // error:
             // D0B39DA3391CFDBBE4E2._IE     *SYSTEM**ERRMSG*          0000000177 000217 234114 errmsg.txt 1 0
             // or normal line:
             // FFB39D86FF2CA235E4E2._IE     NFSW    NFSWSA1  #E2      0000000549 000217 213457 "865tc012.txt" 1  1
             // FFB39D9CEABFC8C0E4E2._IE     NFSW    NFSWSA1  EDI      0000000549 000217 231301 "865tc012.txt" 1 0
             // where the fourth token class may or may not be there
             // we ignore class and the last two tokens
             int tokCount = lt.countTokens();

             if (tokCount != 6 && tokCount != 7 && tokCount != 10 && tokCount != 9) {
                Debug.log(this, Debug.IO_WARNING, "WARNING IGN format of file list line is nonstandard: " + line);
                continue;
             }

             fileInfo = new FileInfo();




             fileInfo.setFileType( FileInfo.FILE );    //set file type to file

             fileInfo.name       = lt.nextToken();
             fileInfo.regExpName = fileInfo.name;
             fileInfo.account       = lt.nextToken();
             fileInfo.user       = lt.nextToken();

             if (tokCount == 7 || tokCount == 10) {
               lt.nextToken();        // skip class
             }
              String size = lt.nextToken();

              size = removeZeros(size);

             try {
                fileInfo.size       = Integer.parseInt(size);
             } catch (NumberFormatException n) {
                Debug.log(this,Debug.ALL_ERRORS, "FtpSslCommConnector::parseIGNFileList - Error converting size, setting to zero");
                fileInfo.size = 0;
             }
             fileInfo.month        = lt.nextToken();

             try {
                fileInfo.day        = Integer.parseInt( removeZeros(fileInfo.month) );
             } catch (NumberFormatException n2) {
                Debug.log(this,Debug.ALL_ERRORS, "FtpSslCommConnector::parseIGNFileList - Error converting month, setting to zero");
                fileInfo.size = 0;
             }
             fileInfo.time       =  lt.nextToken();


             // check if the file matches the template
             if ( !matchFile(template, fileInfo) )
                 continue;
          }



          // Still here?  We have a match.
          vect.addElement(fileInfo);
       }



         // convert object array into fileinfo array
         // this should be changed so that it returns a vector in the future.
         // so that it removes this unnessary step
          Object[] temp = vect.toArray();
          FileInfo array[] = new FileInfo[temp.length];
          for(int j = 0; j < temp.length; j++ ) {
             array[j] = (FileInfo) temp[j];
          }
         return ( array );
    }

    /**
     * removing leading zeros from a string
     */
    protected String removeZeros(String str) {

       int start = 0;

       if (str.startsWith("0")) {
          while ( start < str.length() && str.charAt(start) == '0') {
             start++;
          }
          str = str.substring(start);
       }

       return (str);
    }


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
                        throws FtpException
    {
        synchronized(this.getClass()) {
        int ret;

        // make sure we are logged in
       if (ftpObj == 0 ) {
         throw new FtpException("FtpSslConnector::ERROR not logged in to server.");
       }


      byte bytes[];

       if (workingDir.endsWith(ROOT) ) {
         workingDir = workingDir.substring(0, workingDir.length() - 1);
      }


      String clientFile = cInfo.tempDir + createUniqueFile("TMP_"+serverFileName);


      cd(workingDir,false);

      // set transfer mode
      ret = interConn.setGetMode(ftpObj, !binMode, ediMode);
      if (ret == 0) {
         throw new FtpException("FtpSslCommConnector:getFile -Error setting transfer mode: "
            + interConn.getLastMessage(ftpObj) );
      }
       // get file
      Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:getFile - getting file: " + serverFileName);
      ret = interConn.get(ftpObj, clientFile, serverFileName, !binMode );

      if (ret == 0) {
         throw new FtpException("FtpSslCommConnector:getFile -Error getting file: "
         + interConn.getLastMessage(ftpObj) );
      }

      // read file according to mode we are in.
      try {
         if (binMode) {
            bytes = FileUtils.readBinaryFile(clientFile);
            FileUtils.removeFile(clientFile);
         } else {
            String temp = FileUtils.readFile(clientFile);
            FileUtils.removeFile(clientFile);
            bytes = temp.getBytes();
         }
      } catch (FrameworkException fe) {
         throw new FtpException("FtpSslCommConnector::getFile - ERROR getting reading file " + clientFile
                                 + ": " + fe.toString() );
      }

      cdBack(workingDir, false);

        return( bytes);
      }

    }

   /**
  * Read a file from the FTP server. Stateless call.
      1. login into ftp server
  *   2. change working directory.
  *   3. set transfer type (bin or ascii)
  *   4. get file named fileName
  *   5. logout
  * @param  ci information for establishing the connection
  * @param  type        transfer type, true for binary and false for ascii
  * @param  workingDir    The name of the directory that contains the file
  * @param  fileName    The name of the file to be retrieved
  * @param delete       if true, then tries to deletes the file from the server after retrieving it.
  *                     if false, it leaves the file on the server.
  *                     connecting to van's automatically do this, so set this to false
  *                     when connecting to a van.
  * @return       The FileInfo.
  *
  * @exception   FtpException
  */
    public byte[] connectGet
                        (
                            ConnectionInfo ci,
                            boolean type,
                            boolean edi,
                            String workingDir,
                            String serverFileName,
                            boolean delete
                        )
                        throws FtpException
    {
       synchronized(this.getClass()) {

          FtpSslCommConnector fsc = new FtpSslCommConnector();
          byte bytes[];

          try {

             fsc.setTransferMode(type, edi);
             fsc.login(ci);
             bytes = fsc.getFile(workingDir,serverFileName);

             if (delete) {
                fsc.deleteFile(workingDir, serverFileName );
             }

             fsc.logout();
          } catch (FtpException f) {
             try {
                fsc.logout();
             } catch (FtpException f2) {
                Debug.log(this,Debug.IO_WARNING, "FtpSslCommConnector::ConnectGet - Not logged in");
             }
             throw new FtpException (f.toString());
          }




          return(bytes);
       }
    }

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
                        throws FtpException

    {
       synchronized(this.getClass()) {
       int ret;

       if (ftpObj == 0 ) {
         throw new FtpException("FtpSslConnector::ERROR not logged in to server.");
       }


       if (workingDir.endsWith(ROOT) ) {
         workingDir = workingDir.substring(0, workingDir.length() - 1);
      }

      cd(workingDir,false);

      ret = interConn.delete(ftpObj, fileName, new String("") );

      if (ret == 0) {
         throw new FtpException("FtpSslCommConnector:sendFile -Error deleting file "
         + fileName + ": "  + interConn.getLastMessage(ftpObj) );
      }


      cdBack(workingDir, false);
      }
    }

/**
  * This is match by example. So with nothing in the template it matches everything.
  * There you can narrow it down by seting values in the template.
  *
  * <b>NOTE: You can currently only match by user, group, name, size, type and permission.</b>
  *
  * <pre>USAGE:
  * 1. Integer values are matches exactly.
  * 2. String values are considered a match if a
  * substring matches. Ex: filename.name = Dantheman. template.name = Dan.
  *                         Dan is a substring of filename.name so this is a match.
  * 3. If you want any value to match leave the default values in the FileInfo object.
  * 4. If you pass a null template in the method, all files match.
  * 5. If you the fileName is null then false is always returned.
  * <pre>
  *
  * @param  template    The template FileInfo against which
  *                     the other FileInfo would be matched.
  * @param  fileName          The file that is to be compared.
  * @return Boolean,    true if fn matches template
  *
  * @exception   FtpException
  */
    protected  boolean matchFile(FileInfo template, FileInfo fileName )
    throws FtpException
    {

      // if the template is null then return true
      // if the  file name that is to be compared with the
      // template is null then return false
      if (fileName == null) return false;
      if (template == null ) return true;

      // if template.account is null or (fileName.account != null and if template.account matches fileName.account )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile - template, filename account :" + template.account+","+fileName.account);
      if ( template.account != null && (fileName.account == null || fileName.account.indexOf(template.account) < 0 ) )
      {
        return false;
      }

      // if template.user is null or (fileName.user != null and if template.user matches fileName.user )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile - template, filename user :" + template.user+","+fileName.user);
      if ( template.user != null && (fileName.user == null || fileName.user.indexOf(template.user) < 0 ) )
      {
        return false;
      }

      // if template.name is null or (fileName.name != null and if template.name matches fileName.name )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile - template, filename name :" + template.name+","+fileName.name);
      if ( template.name != null && (fileName.name == null || fileName.name.indexOf(template.name) < 0 ) )
      {
        return false;
      }

      // if template.regExpName is null or (fileName.regExpName != null and if template.regExpName matches fileName.regExpName )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile - template, filename regular expression name :" + template.regExpName+","+fileName.regExpName);
      try
      {
          if ( template.regExpName != null && (fileName.regExpName == null || !RegexUtils.match ( template.regExpName, fileName.regExpName ) ) )
          {
              return false;
          }
      }
      catch ( FrameworkException e )
      {
          //We should never be here if the "if" condition above is correct.
          Debug.log ( Debug.ALL_WARNINGS, "FtpSslCommConnector::matchFile - regular expression name matching error : " + e.getMessage ( ) );
      }

       // if template.size < 0 or (fileName.size >= 0 and if template.size == fileName.size )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile- template, filename size :" + template.size+","+fileName.size);
      if ( template.size >= 0 && (fileName.size < 0 || fileName.size != template.size ) )
      {
        return false;
      }

      // if template.group is null or (fileName.group != null and if template.group matches fileName.group )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile - template, filename group :" + template.group+","+fileName.group);
      if ( template.group != null && (fileName.group == null || fileName.group.indexOf(template.group) < 0 ) )
      {
        return false;
      }

       // if template.type == FileInfo.ANY or (fileName.type != FileInfo.ANY and if template.type == fileName.type )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile- template, filename type :" + template.getFileType() +","+fileName.getFileType());
      if ( template.getFileType() != FileInfo.ANY && (fileName.getFileType() == FileInfo.ANY || fileName.getFileType() != template.getFileType() ) )
      {
        return false;
      }

       // if template.permission is null or (fileName.permission != null and if template.permission matches fileName.permission )
      // then a match
      Debug.log(Debug.IO_STATUS,"FtpSslCommConnector::matchFile - template, filename permission :" + template.permission+","+fileName.permission);
      if ( template.permission != null && (fileName.permission == null || fileName.permission.indexOf(template.permission) < 0 ) )
      {
        return false;
      }

      return true;
    }


    /**
     * Test to see if candidate string matches regular expression.
     *
     * @param  candidate  The candidate value to test.
     * @param  regExp  The regular expression to compare the candidate against.

     * @return  'true' if match occurs, otherwise 'false'.
     */
    private boolean match ( String candidate, String regExp )
    {
        if ( (candidate == null) || (regExp == null) )
            return true;
        
        try
        {
            if ( RegexUtils.match( regExp, candidate ) )
                return true;
        }
        catch ( FrameworkException e )
        {
            // Normally, we should never get here.
            Debug.log ( Debug.ALL_WARNINGS, "FtpSslCommConnector: Regular expression name matching error: " 
                        + e.getMessage() );
        }

        return false;
    }


    /**
     * This changes all of the file separators into the platform specific version. ( gotten
     * from System.getProperty("file.separator") ) This is required not for java, but
     * for the underlining C library that the FtpSslLayerInterconnect class is using.
     *
     * Also adds a file separator at the end of the path if one doesn't exist.
     *
     * @param dir the string the contains dir path.
     * @returns a the same dir path with the correct file separator for this platform
     */
    protected  String checkFileSep(String dir)
    {
       int loc;
       String newStr = dir;

          if ( (loc = dir.indexOf(ROOT) ) >= 0 && dir.charAt(loc) != dirSep.charAt(0) ) {
             newStr = dir.replace(dir.charAt(loc), dirSep.charAt(0) );
          }
          else if ( (loc = dir.indexOf("\\") ) >= 0 && dir.charAt(loc) != dirSep.charAt(0) ) {
             newStr = dir.replace(dir.charAt(loc), dirSep.charAt(0) );
          }

          if ( newStr != null && !newStr.endsWith(dirSep) )
             newStr = newStr + dirSep;

        return(newStr);
    }

    /**
     * creates a unique file name for different threads
     * @param file name
     * @returns an modified file name to be unique among multiple threads.
     */
    private String createUniqueFile(String name)
    {
      String thread = Thread.currentThread().toString();
      thread = thread.replace(':', '_');
      thread = thread.replace('[', '_');
      thread = thread.replace(',', '_');
      thread = thread.replace(']', '_');
      thread = thread.replace(';', '_');
      thread = thread.replace(' ', '_');
      thread = thread.replace('\'', '_');

      while (thread.indexOf("Thread") > -1 || thread.indexOf("thread") > -1 ) {
         thread = StringUtils.replaceSubstringsIgnoreCase(thread, "Thread", "T");
      }
      return(name + thread);
    }

    /**
     * writes a file into the tmpDir from the inputStream.
     * used because Easy Access api is only file based.
     * if binary mode is on for this class then it treats the input stream as
     * binary, otherwise the stream is treated as text.
     *
     * @param tmpDir the temporary diretory to write the file to
     * @param fileName the fileName to write to the tmpDir. createUniqueFile is called on it.
     * @param i the inputStream which contains the file's contents.
     *
     * NOTE user has to close i
     */
    protected String writeTempFile(String tmpDir, String fileName, InputStream i)
    throws FtpException
    {

       if( !tmpDir.endsWith(dirSep) ) {
          tmpDir = tmpDir + dirSep;
       }

       String sourceFile = tmpDir + createUniqueFile(fileName);
       BufferedReader bf = null;

       try {
          if (binMode) {        // use bin mode to write file
             int numBytes = i.available();
             byte bytes[] = new byte[numBytes];
             int numRead = i.read(bytes);

             if (numRead != numBytes) {
                throw new FrameworkException("FtpSslCommConnecter::writeTempFile - ERROR writing temp file: " + sourceFile);
             }
             FileUtils.writeBinaryFile(sourceFile, bytes);

          } else {         // use ascii mode to write file
              bf = new BufferedReader(new InputStreamReader(i) );
              StringBuffer sb = new StringBuffer( );

               char[] inArray = new char [ 1024 ];

               // While stream contents can be read, place what's read in buffer.
               do
               {
                  int numRead = bf.read( inArray, 0, inArray.length );

                  // If EOF, we're done.
                  if ( numRead == -1 )
                      break;

                  // Append what we just got to end of buffer.
                  sb.append( inArray, 0, numRead );
               }
               while ( true );
               FileUtils.writeFile(sourceFile, sb.toString() );
          }


        } catch (IOException ioe) {
             throw new FtpException("FtpSslCommConnector::writeTempFile -ERROR reading file stream : " + ioe.toString() );
        } catch (FrameworkException fwe) {
             throw new FtpException("FtpSslCommConnector::writeTempFile - ERROR writing tempFile file stream : " + fwe.toString() );
        } finally {
            try {
               //i.close(); user should close
               if( bf != null) {
                  bf.close();
               }
            } catch(IOException ioe2) {
               throw new FtpException("FtpSslCommConnector::writeTempFile - ERROR couldn't close file stream:" + ioe2.toString() );
            }
        }

        return (sourceFile);
    }

   /**
    * cd  back up directories back to previous dir.
    *
    * @param workingDir the working dir
    * @param pu used to determine if we are changing for a put or get ftp command (EA API specific)
    */
    protected void cdBack(String workingDir, boolean put)
    throws FtpException    {

       int ret, ret2;



       if (! workingDir.endsWith(ROOT) )  {
          workingDir = workingDir + ROOT;
       }

      // if working dir is ., then don't go up at all.
      // if edi mode, then a van is being used and we should not move up.
      if (workingDir.equals("./") || ediMode == true ) {
         return;
      }

       //if starts at root, just cd back to root directly and return
       if (workingDir.startsWith(ROOT) ) {
          cdToRoot(put);
          return;
       }

       StringTokenizer tok  = new StringTokenizer(workingDir, ROOT);
       int dirCount = tok.countTokens();

       Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:cdBack cd up " + dirCount + " directories.");

          for(int i = 0; i < dirCount; i++ ) {
             Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:cdBack - going up a directory");
             if (!put) {
                ret = interConn.cdForGet(ftpObj,cInfo.userid, "..", ediMode);
                ret2 = interConn.cdForGet(ftpObj,cInfo.userid, ".", ediMode);     //required to trick the ea API
                                                                    // into thinking that the current
                                                                    //directory is no longer ".."
             } else {
                ret = interConn.cdForPut(ftpObj,cInfo.userid, "..", ediMode);
                ret2 = interConn.cdForPut(ftpObj,cInfo.userid, ".", ediMode);
             }

             if (ret == 0 || ret2 == 0) {
                throw new FtpException("FtpSslCommConnector:cdBack -Error doing cd to "
                + " : " + interConn.getLastMessage(ftpObj) );
             }
          }
    }


     /**
      * change directory into working dir
      */
     protected void cd (String workingDir, boolean put)
     throws FtpException    {

       int ret;
       String nextDir;

       // remove end separator if it exists
       if (workingDir.endsWith(ROOT) )  {
          workingDir = workingDir.substring(0, workingDir.length() -1 );
       }

       //change to root directory first if prefixed with file separator
       if (workingDir.startsWith(ROOT) ) {
          workingDir = workingDir.substring(1);
          cdToRoot(put);
       }

       // if working dir is ., then don't move.
      if (workingDir.equals(".") ) {
         return;
      }




       StringTokenizer tok  = new StringTokenizer(workingDir, ROOT);
       int dirCount = tok.countTokens();

       Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:cd - going down " + dirCount + " directories.");
          // loop over each dir
          while (tok.hasMoreTokens() ) {
             nextDir = tok.nextToken();

             Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:cd - going into dir " + nextDir);

             if (!put) {
                ret = interConn.cdForGet(ftpObj,cInfo.userid, nextDir, ediMode);
             } else {
                // when EDI mode is set, the B Trade lib assumes too much for
                // us (it issues a cd userid/class command instead of the
                // special cd edi command), so we need to always set ediMode
                // to false for a cd command for a put
                //
                // now the b trade lib(version 1.49) executes the correct command
                ret = interConn.cdForPut(ftpObj,cInfo.userid, nextDir, ediMode);
             }
             if (ret == 0) {
                throw new FtpException("FtpSslCommConnector:cd - Error doing cd to "
                 + " : " + interConn.getLastMessage(ftpObj) );
             }
          }
    }

     /**
      * change to root directory
      */
     private void cdToRoot (boolean put)
     throws FtpException    {

       int ret;

       Debug.log(this,Debug.IO_STATUS, "FtpSslCommConnector:cdToRoot - changing to root");

             if (!put) {
                ret = interConn.cdForGet(ftpObj, cInfo.userid, ROOT, ediMode);
             } else {
                ret = interConn.cdForPut(ftpObj, cInfo.userid, ROOT, ediMode);
             }
             if (ret == 0) {
                throw new FtpException("FtpSslCommConnector:cd - Error doing cd to "
                 + " : " + interConn.getLastMessage(ftpObj) );
             }
    }


}

/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.email;

import java.io.*;
import java.util.*;
import javax.mail.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.communications.*;


public class AsyncEmailServer extends PollComServerBase
{
    public static final String
        HOST                                = "MAIL_HOST",
        USER                                = "MAIL_USER",
        PASSWORD                            = "MAIL_PASSWORD",
        PROTOCOL                            = "MAIL_PROTOCOL",
        TARGET_DIR                          = "TARGET_DIR",
        FILE_PREFIX                         = "FILE_PREFIX",
        PROCESSED_FOLDER                    = "Processed",
        ERROR_FOLDER                        = "Error",
        EXCEPTION_FOLDER                    = "Exception",
        NO_ATTACHMENT_FOLDER                = "Other";

        // the above directories should be properties, but leaving it alone for backwards
        // compatibility

    private String file                     = null;
    private String protocol                 = null;
    private String host                     = null;
    private String user                     = null;
    private String password                 = null;
    private String targetDir                = null;
    private String filePrefix               = null;

    
    /**
     * Constructor
     *
     */
    public AsyncEmailServer (String key, String type) throws ProcessingException 
    {
        super(key,type);
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, "Initializing the Asynchronous Email server.");

        StringBuffer errBuf = new StringBuffer();
        host       = getRequiredPropertyValue(HOST, errBuf);
        protocol   = getRequiredPropertyValue(PROTOCOL, errBuf);
        user       = getRequiredPropertyValue(USER, errBuf);
        password   = getRequiredPropertyValue(PASSWORD, errBuf);
        filePrefix = getRequiredPropertyValue(FILE_PREFIX, errBuf);
        targetDir  = addFileSeparator(getRequiredPropertyValue(TARGET_DIR, errBuf) );

        if (errBuf.length() > 0)
           throw new ProcessingException(errBuf.toString());
    }

     /**
     * adds a file separator to the end of a dir path if it does not exist.
     */
    private String addFileSeparator(String dir) {
       if (!dir.endsWith(File.separator) ) {
          dir = dir + File.separator;
       }
       return dir;
    }
    
    /**
     * Polls a mail server for mail, writes write them to a file.
     */
    public void processRequests() throws ProcessingException
    {
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Checking the MAIL SERVER for messages...");
        
        Map messageList         = null;
        GetEmailClient emailClient    = new GetEmailClient();
        javax.mail.Message currentMsg = null;
        String fileName               = null;
        javax.mail.Message[] moveMsgs = new javax.mail.Message[1];
        boolean attachmentFailed      = false;

        try
        {
            emailClient.logon(protocol, host, -1, user, password);
        }
        catch (ProcessingException pe) 
        {
            throw new ProcessingException("Problem connecting to Mail Server: " + pe.getMessage() );
        }

        try 
        {
            messageList = emailClient.getMail();
        }
        catch (ProcessingException pe)
        {
            throw new ProcessingException("Unable to retrieve messages from mail server: " + pe.getMessage());

        }
        
        //if the message list returned is null or has no messages then logout and return immediately
        if (messageList == null || messageList.size() == 0)
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "There are no messages to process.");
            
            try
            {
                emailClient.logout();
            }
            catch (javax.mail.MessagingException jme)
            {
                Debug.log(Debug.ALL_WARNINGS, "Problem logging off Mail Server: " + jme.getMessage() );
            }

            return;
        }
        
        try
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "Retrieved: [" + messageList.size() + "] messages.");

            //loop through the messages
            Iterator iter = messageList.keySet().iterator();


            while(iter.hasNext())
            {
                currentMsg = (javax.mail.Message) iter.next();
                moveMsgs[0] = currentMsg;

                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "Current Message:\n" + currentMsg);

                //get the attachments
                List attachments = (List)messageList.get(currentMsg);

                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "\n\t number of attachments: " + attachments.size());

                //if the there are no attachments move the message to the 'Other' folder
                if (attachments.isEmpty())
                {
                    try
                    {
                        emailClient.moveMessage(moveMsgs, NO_ATTACHMENT_FOLDER);
                        currentMsg.setFlag(Flags.Flag.DELETED, true);
                    }
                    catch (Exception e)
                    {
                        Debug.log(Debug.ALL_WARNINGS, "WARNING: Unable to move message [" + currentMsg + " to " 
                                  + NO_ATTACHMENT_FOLDER + "folder : "+ e.getMessage() );
                    }
                    // skip the current message since there are no attachments
                   continue;
                }

                //else loop through the attachments
                Iterator attachIter = attachments.iterator();
                attachmentFailed = false;
                while(attachIter.hasNext())
                {
                    fileName = targetDir + filePrefix + "_" +
                               DateUtils.getDateToMsecAsString();
                    //sleep a bit so timestamp on file will change
                    try 
                    {Thread.currentThread().sleep(100);}
                    catch(InterruptedException e) {}

                    Object curAttachment = attachIter.next();

                    if (null == curAttachment)
                    {
                            Debug.log(Debug.MSG_STATUS, "Current attachment contains no value, skipping");
                        //continue on to the next attachment
                        continue;
                    }

                    if (curAttachment instanceof String)
                    {

                        if (!StringUtils.hasValue((String)curAttachment))
                            continue;

                        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                            Debug.log (Debug.MSG_STATUS, "Current attachment: " + curAttachment);

                        try
                        {
                            processAttachment(fileName,curAttachment, emailClient, currentMsg);
                        }
                        catch (FrameworkException fe)
                        {
                           //processAttachment already logs errors
                           // skip the current message
                           attachmentFailed = true;
                           break;

                        }
                    }
                    else if (curAttachment instanceof byte[])
                    {
                         if (((byte[])curAttachment).length == 0 )
                            continue;

                          if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                            Debug.log (Debug.MSG_STATUS, "Current binary attachment size: " + ((byte[])curAttachment).length + " bytes");
                        
                       try
                        {
                            processAttachment(fileName,curAttachment, emailClient, currentMsg);
                        }
                        catch (FrameworkException fe)
                        {
                           //processAttachment already logs errors
                           // skip the current message
                           attachmentFailed = true;
                           break;
                        }
                    }
                    else
                    {
                        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        {
                            Debug.log(Debug.ALL_WARNINGS, GetEmailClient.pr("This is an unknown attachment type"));
                            Debug.log(Debug.ALL_WARNINGS, GetEmailClient.pr("---------------------------"));
                        }
                        // skip current unknown type of attachment
                        continue;
                    }
                }

                // if any attachments failed then don't move the message to processed
                // skip to the next message
                if ( attachmentFailed )
                   continue;

                //when done with all the attachments for a given email message mark the message for delete
                try 
                {
                    if (!currentMsg.isSet(Flags.Flag.DELETED))
                    {
                        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                            Debug.log(Debug.MSG_STATUS, "Moving message to PROCESSED Folder...");
                        
                        emailClient.moveMessage(moveMsgs, PROCESSED_FOLDER);
                    }
                    
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "Marking message for delete...");
                    
                    currentMsg.setFlag(Flags.Flag.DELETED, true);
                    javax.mail.Message expungedMessages[] = emailClient.folder.expunge();
                }
                catch (javax.mail.MessagingException mailEx)
                {
                    Debug.log(Debug.ALL_ERRORS, "ERROR: Problem marking message for deletion: " + mailEx.getMessage());
                }
            }
        }
        catch (Exception e)
        {
            Debug.logStackTrace(e);
            throw new ProcessingException (StringUtils.getClassName(this) +
                ": Failed processing emails.\n" + e.toString());
        }
        finally
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "Logging off Mail Server...");

            try 
            {
                emailClient.logout();
            }
            catch (javax.mail.MessagingException mailEx) 
            {
                throw new ProcessingException("Cannot logout from mail server.\n" + mailEx.getMessage());
            }
        }
    }


    /**
     * handles processing a single attachment. It
     * @param fileName the File name to write the attachment to
     * @param attachment the current email attachment
     * @param emailClient The email client
     * @param curEmailMessage The email message which contains the attachment.
     */
    protected void processAttachment(String fileName, Object attachment, GetEmailClient emailClient, 
                                     javax.mail.Message curEmailMessage ) throws FrameworkException
    {

    javax.mail.Message[] tempEmail = new javax.mail.Message[1];
    tempEmail[0] = curEmailMessage;

       // Send it through gateway.
       try
       {
          if ( attachment instanceof byte[] )
             FileUtils.writeBinaryFile(fileName, (byte[]) attachment);
          else if (attachment instanceof String)
             FileUtils.writeFile(fileName,(String) attachment);
          else
             throw new FrameworkException("Message attachment type is unknown");

          process(null, attachment);
       }
       catch  (MessageException me)
       {
          Debug.log(Debug.ALL_ERRORS,"Invalid data : " + me.getMessage() );

          try
          {
              emailClient.moveMessage(tempEmail, ERROR_FOLDER);
              curEmailMessage.setFlag(Flags.Flag.DELETED, true);
          }
          catch (Exception e)
          {
            Debug.log(Debug.ALL_ERRORS, "Unable to move message [" +
                      curEmailMessage + " to " + ERROR_FOLDER+ ": " + e.getMessage());
             throw new FrameworkException(e);
          }

          //skip the whole message
          throw me;
       }
       catch  (Exception exp)
       {
          Debug.log(Debug.ALL_ERRORS,"Processing Error : " + exp.getMessage() );

          try
          {
            emailClient.moveMessage(tempEmail, EXCEPTION_FOLDER);
            // do we want to set this to true?
            curEmailMessage.setFlag(Flags.Flag.DELETED, true);
          }
          catch (Exception e)
          {
            Debug.log(Debug.ALL_ERRORS, "Unable to move message [" +
              curEmailMessage + " to " + EXCEPTION_FOLDER + ": " + e.getMessage());
              throw new FrameworkException(e);
          }

          throw new ProcessingException(exp);
       }
    }


}

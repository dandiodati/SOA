package com.nightfire.comms.email;

//////////////////////////////// JDK packages ////////////////////////////////
import java.util.*;

//////////////////////////////// NightFire packages //////////////////////////
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.email.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;


public class SMTPEmailClient extends MessageProcessorBase
{
    /**
     * Directory to which email is written out if send operation is successful
     */
    private String processedDir;

    /**
     * Directory to which email is written out if send operation is unsuccessful
     */
    private String errorDir;

    /**
     * Prefix for the file name when the email is written out to a directory after send operation
     */
    private String targetFilePrefix;

    /**
     * Suffix for the file name when the email is written out to a directory after send operation
     */
    private String targetFileSuffix;

    /**
     * The SMTP server address
     */
    private String smtpServerAddr;

    /**
     * Sender email address
     */
    private String senderEmailAddr;

    /**
     * Recipient email address
     */
    private String recipientEmailAddrList;

    /**
     * Subject for the email
     */
    private String subject;

    /**
     * Subject Location in the context
     */
    private String subjectLocation;

    /**
     * cc email address
     */
    private String ccEmailAddrList;

    /**
     * bcc email address
     */
    private String bccEmailAddrList;

    /**
     * List of additional headers to be added
     */
    private String headerNamesList;

    /**
     * Separator for multiple values in headers' list, recipientEmailaddr, ccEmailAddr, bccEmailAddr
     */
    private String separator;

    /**
     * Whether input message is to be an inline message - String value of inline
     */
    private String inlineStr;

    /**
     * Whether input message is to be an inline message - boolean value - true/false
     */
    private boolean inline;

    /**
     * Whether input message is to be an attachment message - String value of inline
     */
    private String attachmentStr;

    /**
     * Whether input message is to be an attachment message - boolean value - true/false
     */
    private boolean attachment;

    /**
     * Type of attachment - "text/plain",...
     */
    private String attachmentType;

    /**
     * Name of service class to instantiate for composing and sending email
     */
    private String emailClientServiceClass;

    /**
     * Debug option for SMTP server - true/false - String value
     */
    private String debugFlagStr;

    /**
     * Debug option for SMTP server - true/false - boolean value
     */
    private boolean debugFlag;

    /**
     * Print message for this class - true/false - String value
     */
    private String printMessageStr;

    /**
     * Print message for this class - true/false - boolean value
     */
    private boolean printMessage;

    /**
     * List of header names
     */
    private String headerName [ ];

    /**
     * List of header name values
     */
    private String headerValue [ ];

    /**
     * Utils class reference to be used to compose and send email
     */
    private EmailInterface emailInterfaceObj = null;

   /**
    * Following are properties that are fetched from the persistentproperty table
    */
    private static final String PROCESSED_DIR_PROP = "PROCESSED_FILE_DIR";
    private static final String TARGET_FILE_PROP = "TARGET_FILE";
    private static final String TARGET_SUFFIX_PROP = "TARGET_SUFFIX";
    private static final String ERROR_DIR_PROP = "ERROR_FILE_DIR";
    private static final String SMTP_SERVER_PROP = "SMTP_SERVER";
    private static final String SENDER_PROP = "SENDER";
    private static final String RECIPIENT_PROP = "RECIPIENT";
    private static final String SUBJECT_PROP = "SUBJECT";
    private static final String SUBJECT_LOCATION_PROP = "SUBJECT_LOCATION";
    private static final String CC_PROP = "CC";
    private static final String BCC_PROP = "BCC";
    private static final String HEADER_NAMES_PROP = "HEADER_NAMES";
    private static final String SEPARATOR_PROP = "SEPARATOR";
    private static final String INLINE_PROP = "INLINE";
    private static final String ATTACHMENT_PROP = "ATTACHMENT";
    private static final String ATTACHMENT_TYPE_PROP = "ATTACHMENT_TYPE";
    private static final String EMAIL_CLIENT_SERVICE_CLASS_PROP = "EMAIL_CLIENT_SERVICE_CLASS";
    private static final String DEBUG_FLAG_PROP = "DEBUG_FLAG";
    private static final String PRINT_MESSAGE_PROP = "PRINT_MESSAGE";
    private static final String DUMMY = "DUMMY";

   /**
     * Constructor
     */
    public SMTPEmailClient()
    {
    }//SMTPEmailClient constructor

    /**
     * Initialize the instances
     * @param Key  to load properties from Persistent Properties
     * @param Type to load Properties from Persistent Properties
     * @exception ProcessingException thrown if initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
      if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log( Debug.OBJECT_LIFECYCLE, "Initializing the SMTPEmailClient..." );

      try
      {
        super.initialize ( key, type );
      }
      catch(ProcessingException pe)
      {
        Debug.log ( Debug.ALL_ERRORS, "ERROR: SMTPEmailClient: Failed initialization" );
        throw pe;
      }

      //Initialize a string buffer to contain all errors
      StringBuffer strBuf = new StringBuffer();

         processedDir = (String) adapterProperties.get(PROCESSED_DIR_PROP);
         if (processedDir == null)
            strBuf.append(" : PROCESSED_DIR_PROP not set");

         errorDir = (String) adapterProperties.get(ERROR_DIR_PROP);
         if (errorDir == null)
            strBuf.append(" : ERROR_DIR_PROP not set");

         targetFilePrefix = (String) adapterProperties.get(TARGET_FILE_PROP);
         if (targetFilePrefix == null)
            strBuf.append(" : TARGET_FILE_PROP not set");

         targetFileSuffix = (String) adapterProperties.get(TARGET_SUFFIX_PROP);

         smtpServerAddr = (String) adapterProperties.get(SMTP_SERVER_PROP);
         if (smtpServerAddr == null)
            strBuf.append(" : SMTP_SERVER_PROP not set");

         senderEmailAddr = (String) adapterProperties.get(SENDER_PROP);
         if (senderEmailAddr == null)
          strBuf.append(" : SENDER_PROP not set");

         subjectLocation = getPropertyValue( SUBJECT_LOCATION_PROP );

         subject = (String) adapterProperties.get(SUBJECT_PROP);

         recipientEmailAddrList = (String) adapterProperties.get(RECIPIENT_PROP);

         ccEmailAddrList = (String) adapterProperties.get(CC_PROP);

         bccEmailAddrList = (String) adapterProperties.get(BCC_PROP);

         if ( ( recipientEmailAddrList == null ) && ( ccEmailAddrList == null )
              && ( bccEmailAddrList == null ) )
          strBuf.append(" : Either of RECIPIENT_PROP, CC_PROP or BCC_PROP has to be set");

         separator = (String) adapterProperties.get(SEPARATOR_PROP);
         if (separator == null)
            strBuf.append(" : SEPARATOR_PROP not set");

         headerNamesList = (String) adapterProperties.get(HEADER_NAMES_PROP);

         if (headerNamesList != null)
         {
          if (headerNamesList.trim().length() != 0)
          {
            headerName = getTokens (headerNamesList);
            if (headerName != null)
            {
              headerValue = new String [ headerName.length ];
              for (int i = 0; i < headerName.length; i++)
              {
                String headerValueStr = null;
                headerValueStr = ( String ) adapterProperties.get ( headerName [ i ] );
                if ( headerValueStr != null )
                {
                  headerValue [ i ] = new String ( headerValueStr );
                }
                else
                {
                  strBuf.append(" : Header name " + headerName[i] +
                  "'s value not set properly");
                }

              }//for
            }//if not null
            else
            {
              strBuf.append(" : HEADER_NAMES_PROP not set properly");
            }

           }//if headerNamesList.trim().length() != 0
         }//if headerNamesList != null

         inlineStr = (String) adapterProperties.get(INLINE_PROP);
         if (inlineStr == null)
            strBuf.append(" : INLINE_PROP not set");
         else
         {
            if ( inlineStr.equalsIgnoreCase ("TRUE") )
              inline = true;
            else
            if ( inlineStr.equalsIgnoreCase ("FALSE") )
              inline = false;
            else
              strBuf.append (" : INLINE_PROP value not TRUE or FALSE");
         }

         attachmentStr = (String) adapterProperties.get(ATTACHMENT_PROP);
         if (attachmentStr == null)
            strBuf.append(" : ATTACHMENT_PROP not set");
         else
         {
            if ( attachmentStr.equalsIgnoreCase ("TRUE") )
              attachment = true;
            else
            if ( attachmentStr.equalsIgnoreCase ("FALSE") )
              attachment = false;
            else
              strBuf.append (" : ATTACHMENT_PROP value not TRUE or FALSE");
         }

         if ( ( attachmentStr != null ) && ( inlineStr != null ) )
         {
            if ( ( inline == false ) && ( attachment == false ) )
           {
             strBuf.append (" : Either of INLINE_PROP or ATTACHMENT_PROP has to be TRUE");
           }
         }

         if ( ( attachmentStr != null ) && ( attachment == true ) && (attachmentType == null) )
         {
            attachmentType = (String) adapterProperties.get(ATTACHMENT_TYPE_PROP);
            if (attachmentType == null)
              strBuf.append(" : ATTACHMENT_TYPE_PROP not set");
         }

         emailClientServiceClass = (String) adapterProperties.get(EMAIL_CLIENT_SERVICE_CLASS_PROP);
         if (emailClientServiceClass == null)
            strBuf.append(" : EMAIL_CLIENT_SERVICE_CLASS_PROP not set");

         debugFlagStr = (String) adapterProperties.get(DEBUG_FLAG_PROP);
         if (debugFlagStr == null)
            strBuf.append(" : DEBUG_FLAG_PROP not set");
         else
         {
            if ( debugFlagStr.equalsIgnoreCase ("TRUE") )
              debugFlag = true;
            else
            if ( debugFlagStr.equalsIgnoreCase ("FALSE") )
              debugFlag = false;
            else
              strBuf.append (" : DEBUG_FLAG_PROP value not TRUE or FALSE");
         }

         printMessageStr = (String) adapterProperties.get(PRINT_MESSAGE_PROP);
         if (printMessageStr == null)
            strBuf.append(": PRINT_MESSAGE_PROP not set");
         else
         {
            if ( printMessageStr.equalsIgnoreCase ("TRUE") )
              printMessage = true;
            else
            if ( printMessageStr.equalsIgnoreCase ("FALSE") )
              printMessage = false;
            else
              strBuf.append (" : PRINT_MESSAGE_PROP value not TRUE or FALSE");
         }

      if (strBuf.length()!=0)
      {
        Debug.log ( Debug.ALL_ERRORS, "ERROR: SMTPEmailClient: Initialization failed : " +
              strBuf.toString ( ) );
        throw new ProcessingException ( "ERROR: SMTPEmailClient: Initialization failed : " +
              strBuf.toString ( ) );
      }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Initialized SMTPEmailClient" );
    }//initialize

    /**
     * Fetch all tokens separated by separator
     * @param tokenList String containing all headers separated by "SEPARATOR"
     * @return String[] Individual header names as a String
     */
    private String[] getTokens (String tokenList)
        throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Extracting tokens from list" );
        StringTokenizer strTok = new StringTokenizer (tokenList, separator, false);
        String[] retVal = null;
        retVal = new String [ strTok.countTokens ( ) ];
        for (int i=0; i<retVal.length; i++)
        {
          try
          {
            retVal[i] = new String ( strTok.nextToken ( ).trim ( ) );
          }
          catch (NoSuchElementException nsee)
          {
            Debug.log ( Debug.ALL_ERRORS, "ERROR: SMTPEmailClient: " +
              nsee.getMessage ( ) );
            throw new ProcessingException ( "ERROR: SMTPEmailClient: " +
              nsee.getMessage ( ) );
          }
        }
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Finished extracting tokens from list" );
        return retVal;
    }//getTokens

    /**
     * Process the outbound message and (optionally) return
     * a value
     * @param  input Outbound message
     * @param context The MessageProcessorContext to be used for transactional database
     * operations
     * @return NVPair[] Null if successful, else exception is thrown
     * @exception  MessageException  thrown if bad message.
     * @exception  ProcessingException  thrown if processing fails.
     */
    public NVPair[] execute ( MessageProcessorContext context, java.lang.Object input )
      throws MessageException, ProcessingException
    {
      String inputMessage = null;
      //Compose message using the persistent properties and the SMTPEmailUtils class's methods as follows:
      if ( input == null )
      {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: The input is null");
        return null;
       }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Processing...");
      //Fetch the message
      inputMessage = Converter.getString(input);

      try
      {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Composing and sending email" );

          // Obtain subject from Context location, if available.
          String subjectLocValue = null;
          if (StringUtils.hasValue(subjectLocation))
            subjectLocValue = getString( subjectLocation, context, new MessageObject(input) );

          // If subject value found at specified location
          // then prefer it over static subject value passed as direct message processor property.
          if (StringUtils.hasValue(subjectLocValue))
          {
              if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                  Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Found subject value [" + subjectLocValue + "] at location [" + subjectLocation + "]."
                        + " Using it instead of value passed as SUBJECT." );
              subject = subjectLocValue;
          }

        composeAndSendEmail ( inputMessage );

        //if control returns here, it means message is sent successfully
        //Put a copy of the message body into the directory specified by PROCESSED_DIR
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Writing file to processed directory" );
        writeToFile ( inputMessage, processedDir );
      }
      catch ( ProcessingException pe )
      {
        try
        {
          //Put a copy of the message body into the directory specified by the ERROR_DIR.
          writeToFile ( inputMessage, errorDir );
          throw pe;
        }
        catch ( ProcessingException e )
        {
          throw e;
        }
      }
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          Debug.log( Debug.MSG_STATUS, "SMTPEmailClient: Processing over...." );
      return formatNVPair(input);
    }//process

    /**
     * Write the inputMessage to the directory specified by targetDir
     * @param inputMessage to be written out to file
     * @param targetDir Directory to be written out to
     * @exception  ProcessingException  thrown if message cannot be written out to file
     */
    private void writeToFile ( String inputMessage, String targetDir )
      throws ProcessingException
    {
      String appendTag = DateUtils.getDateToMsecAsString ( );
      String fileName = targetDir + targetFilePrefix + appendTag;
      if ( targetFileSuffix != null )
        fileName = fileName + targetFileSuffix;
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          Debug.log ( Debug.MSG_STATUS, "SMTPEmailClient: Writing to the file " + fileName );
	    try
      {
        FileUtils.writeFile ( fileName, inputMessage );
      }
      catch ( FrameworkException fe )
      {
	      Debug.log ( Debug.ALL_ERRORS, "ERROR: SMTPEmailClient: Cannot write to directory " +
        targetDir + ", " + fe.getMessage () );
	      throw new ProcessingException ( "ERROR: SMTPEmailClient: Cannot write to directory " +
        targetDir + ", " + fe.getMessage () );
      }
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          Debug.log ( Debug.MSG_STATUS, "SMTPEmailClient: Finished writing to file " + fileName );
    }//writeToFile

    /**
     * Compose and send the email using the utils classes
     * @param Message to be sent
     * @exception  ProcessingException  thrown if message composition fails
     */
    private void composeAndSendEmail ( String inputMessage )
      throws ProcessingException

    {
      //Create utils class to compose and send email

      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
          Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Creating service class");
      try
      {
        emailInterfaceObj = ( EmailInterface ) ObjectFactory.create ( emailClientServiceClass );

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Created service class");
            Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Initializing server settings..");
        }

        emailInterfaceObj.initServer ( smtpServerAddr, debugFlag );
	            
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting sender");
        emailInterfaceObj.setSender ( senderEmailAddr );
        String tokens [] = null;

        //Add TO recipients
        if ( recipientEmailAddrList != null )
        {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting TO recipients");
          tokens = getTokens ( recipientEmailAddrList );
          for ( int i = 0; i < tokens.length; i++ )
          {
            emailInterfaceObj.addRecipient ( tokens [i] );
          }
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log(Debug.MSG_STATUS,"SMTPEmailClient: The message will be sent to ["+ recipientEmailAddrList +"]");
        }//if

        //Add CC recipients
        if ( ccEmailAddrList != null )
        {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting CC recipients");
          tokens = getTokens ( ccEmailAddrList );
          for ( int i = 0; i < tokens.length; i++ )
          {
            emailInterfaceObj.addCC ( tokens [i] );
          }
        }//if

        //Add BCC recipients
        if ( bccEmailAddrList != null )
        {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting BCC recipients");
          tokens = getTokens ( bccEmailAddrList );
          for ( int i = 0; i < tokens.length; i++ )
          {
            emailInterfaceObj.addBCC ( tokens [i] );
          }
        }//if

        //Add additional headers if headers exist
        if ( ( headerName != null ) && ( headerValue != null ) )
        {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting additional headers");
          for ( int i = 0; i < headerName.length; i++ )
          {
            emailInterfaceObj.addHeader ( headerName [i], headerValue [i] );
          }//for
        }//if

        if ( subject != null )
        {
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting SUBJECT field");
          emailInterfaceObj.setSubject ( subject );
        }

        if ( attachment == true )
        {
          String inputMessageArray [] = new String [1];
          inputMessageArray [0] = new String ( inputMessage );
          String attachmentTypeArray [] = new String [1];
          attachmentTypeArray [0] = new String ( attachmentType );

          if ( inline == true )
          {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting inline text");
            //message is to be set as an attachment and inline text,
            //where attachmenttype specifies the type of attachment.
            emailInterfaceObj.setMessageContent ( inputMessage,
            inputMessageArray, attachmentTypeArray );
          }
          else
          {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting attachments");
            //message is to be set as an attachment only
            emailInterfaceObj.setMessageContent ( inputMessageArray,
            attachmentTypeArray );
          }
        }//attachment is true
        else
        if ( inline == true )
        {
          //message is to be set as a inline text only
          if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Setting inline text");
          emailInterfaceObj.setMessageContent ( ( String ) inputMessage);
        }

        //print message
        if ( printMessage == true )
          emailInterfaceObj.printMessage ( );

        //Send message using SMTPEmailUtils's sendEmail method as follows:
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Sending email...");
        emailInterfaceObj.sendEmail ();
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "SMTPEmailClient: Email sent successfully");
      }
      catch (FrameworkException fe)
      {
	      Debug.log( Debug.ALL_ERRORS, "ERROR: SMTPEmailClient: " +
        fe.getMessage () );
	      throw new ProcessingException ("ERROR: SMTPEmailClient: " +
        fe.getMessage () );
      }      
    }//composeAndSendEmail

} //End of SMTPEmailClient

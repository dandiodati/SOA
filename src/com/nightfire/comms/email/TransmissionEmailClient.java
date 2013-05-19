/**
 * Copyright(c) 2000-2006 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: //comms/R3.8.1/com/nightfire/comms/email/TransmissionEmailClient.java#1$
 */

package com.nightfire.comms.email;

// JDK import
import java.util.ArrayList;
import java.util.StringTokenizer;

// Nightfire import
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.ObjectFactory;
import com.nightfire.framework.email.EmailInterface;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;

// thidparty imports
import org.w3c.dom.Node;
import org.w3c.dom.Document;

// Nightfire import for testing TransmissionEmailClient
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;

// JDK import for testing
import java.util.Hashtable;

/**
 * This Email client is developed to process Transmission Info XML and
 * attachment in the E-mail transmission.
 * It takes properties from "PersistentProperty" table &
 * "TRANSMISSION_INFO" XML, attachment, attachment name,
 * and e-mail subject from context/input, process it and
 * compile all the email transmission properties and send email.
 *
 * Following email headers will be generated from Transmission Info XML
 *
 * 1) Subject   : Subject Text
 * 2) To        : Name (TelephoneNumber) <E-mail Address>
 * 3) CC        : CC e-mail address list
 * 4) From      : E-mail address
 * 5) Reply-To  : Name (TelephoneNumber) <E-mail Address>
 *
 * Following format would be used for message body, if present
 *
 *          Sent from:
 *          <Originator Name>
 *          Tel No:<Originator Telephone Number>
 *
 * Attachment content will be sent with email as inline message or
 * as attachment based on configuration.
 *
 * Currenly email attachment is mandatory as message body
 * might not be generated. Also only one attachment is supported in
 * this client.
 *
 * The dtd for Transmission Information is located in perforce at
 * //techpubs/dtds/transmission_info/transmission_info.dtd
 */
public class TransmissionEmailClient extends MessageProcessorBase
{
    /**
     * Abbreviated class name for use in logging
     */
    private String loggingClassName;

    /**
     * The SMTP server address
     */
    private String smtpServerAddr;

    /**
     * Subject Location in the context/input
     */
    private String subjectLocation;

    /**
     * Whether input attachment is to be an inline message
     * in the email body - String value of inline
     */
    private String inlineStr;

    /**
     * Whether input attachment is to be an inline message
     * in the email body - boolean value - true/false
     */
    private boolean inline;

    /**
     * Name of service class to instantiate for composing and sending email
     */
    private String emailClientServiceClass;

    /**
     * Transmission Info XML location in the context/input; e.g. @context.TRANSMISSION_INFO,...
     */
    private String transmissionInfoLocation;

    /**
     * Type of attachment - e.g. "text/plain",...
     */
    private String attachmentType;

    /**
     * Attachment location in the context/input; e.g. @context.ATTACHMENT,...
     */
    private String attachmentLocation;

    /**
     * Default name to be used for attachment
     */
    private String defaultAttachmentName;

    /**
     * Location of attachment name in the context/input; e.g. @context.ATTACHMENT_NAME,...
     */
    private String attachmentNameLocation;

    /**
     * Inner class reference used to handle batch processing
     */
    private TransmissionInfo transmissionInfo;

    /**
     * New Line character used for line separation in the email body
     */
    private static final String NEW_LINE = System.getProperty("line.separator");

    /**
     * Enclosing character for tokens
     */
    private static final char TOKEN_WRAPPER = '%';

    /**
     * Enclosing function name to make parameter value in UPPER case
     */
    private static final String UPPER_CASE_FUNC = "upper(";

    /**
     * Enclosing function name to make parameter value in lower case
     */
    private static final String LOWER_CASE_FUNC = "lower(";

    /**
     * Enclosing character for tokens
     */
    private static final char CLOSING_PARENTHESES = ')';

   /**
    * Following are the properties that are fetched from the persistentproperty table
    */
    private static final String SMTP_SERVER_PROP                = "SMTP_SERVER";
    private static final String INLINE_PROP                     = "INLINE";
    private static final String EMAIL_CLIENT_SERVICE_CLASS_PROP = "EMAIL_CLIENT_SERVICE_CLASS";
    private static final String ATTACHMENT_TYPE_PROP            = "ATTACHMENT_TYPE";
    private static final String DEFAULT_ATTACHMENT_NAME_PROP    = "DEFAULT_ATTACHMENT_NAME";

    /**
     * Following constants are used to store the context/input locations of properties
     */
    private static final String SUBJECT_LOCATION_PROP           = "SUBJECT_LOCATION";
    private static final String TRANSMISSION_INFO_LOCATION_PROP = "TRANSMISSION_INFO_LOCATION";
    private static final String ATTACHMENT_LOCATION_PROP        = "ATTACHMENT_LOCATION";
    private static final String ATTACHMENT_NAME_LOCATION_PROP   = "ATTACHMENT_NAME_LOCATION";

    /**
     * Following are the properties that are fetched from Transmission Info XML
     */
    private static final String EMAIL_INFO_XPATH        = "email_info.";
    private static final String TRANSMISSION_INFO_XPATH = EMAIL_INFO_XPATH + "transmit_info.";
    private static final String RESPONSE_INFO_XPATH     = EMAIL_INFO_XPATH + "response_info.";
    private static final String ORIGINATOR_XPATH        = EMAIL_INFO_XPATH + "originator.";
    private static final String RECIPIENT_XPATH         = EMAIL_INFO_XPATH + "recipient.";
    private static final String NEUSTAR_XPATH           = EMAIL_INFO_XPATH + "neustar.";
    private static final String NAME_NODE               = "name";
    private static final String TELNO_NODE              = "telno";
    private static final String CC_NODE                 = "cc";

    private static final String TO_EMAIL_ADDRESS_XPATH  = TRANSMISSION_INFO_XPATH + "to_email_address";
    private static final String FROM_EMAIL_ADDRESS_XPATH= TRANSMISSION_INFO_XPATH + "from_email_address";
    private static final String COVER_TEXT_XPATH        = TRANSMISSION_INFO_XPATH + "cover_text";
    private static final String MESSAGE_TEXT_XPATH      = TRANSMISSION_INFO_XPATH + "message_text";
    private static final String CC_CONTAINER_XPATH      = TRANSMISSION_INFO_XPATH + "cccontainer";
    private static final String REPLY_TO_XPATH          = RESPONSE_INFO_XPATH + "reply-to";
    private static final String ORIGINATOR_NAME_XPATH   = ORIGINATOR_XPATH + NAME_NODE;
    private static final String ORIGINATOR_TELNO_XPATH  = ORIGINATOR_XPATH + TELNO_NODE;
    private static final String RECIPIENT_NAME_XPATH    = RECIPIENT_XPATH + NAME_NODE;
    private static final String RECIPIENT_TELNO_XPATH   = RECIPIENT_XPATH + TELNO_NODE;
    private static final String NEUSTAR_NAME_XPATH      = NEUSTAR_XPATH + NAME_NODE;
    private static final String NEUSTAR_TELNO_XPATH     = NEUSTAR_XPATH + TELNO_NODE;

   /**
    * Constructor
    */
    public TransmissionEmailClient()
    {
       loggingClassName = StringUtils.getClassName( this );
    }

    /**
     * Initialize the instances
     * @param key  to load properties from Persistent Properties
     * @param type to load Properties from Persistent Properties
     * @exception ProcessingException thrown if initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, loggingClassName + ": Initializing ..." );

        try
        {
            super.initialize ( key, type );
        }
        catch(ProcessingException pe)
        {
            Debug.log ( Debug.DB_ERROR, loggingClassName + ": ERROR: Failed initialization" );
            throw pe;
        }

        /* Initialize a string buffer to contain errors while initializing properties */
        StringBuffer errorMsg = new StringBuffer();

        smtpServerAddr = getRequiredPropertyValue( SMTP_SERVER_PROP, errorMsg );

        emailClientServiceClass = getRequiredPropertyValue( EMAIL_CLIENT_SERVICE_CLASS_PROP, errorMsg );

        transmissionInfoLocation = getRequiredPropertyValue( TRANSMISSION_INFO_LOCATION_PROP, errorMsg );

        inlineStr = getRequiredPropertyValue( INLINE_PROP, errorMsg );

        try
        {
            inline = StringUtils.getBoolean( inlineStr );
        }
        catch ( FrameworkException fe )
        {
            Debug.log ( Debug.ALL_WARNINGS, "INLINE_PROP can only be TRUE or FALSE" );
            errorMsg.append ( "INLINE_PROP can only be TRUE or FALSE\n" );
        }

        attachmentLocation = getPropertyValue( ATTACHMENT_LOCATION_PROP );

        /* If inline is false and the attachment is given (attachment location is given) then get the other attachment properties */
        if ( !inline && StringUtils.hasValue(attachmentLocation) )
        {
            attachmentType = getRequiredPropertyValue( ATTACHMENT_TYPE_PROP, errorMsg );

            defaultAttachmentName = getPropertyValue( DEFAULT_ATTACHMENT_NAME_PROP );

            attachmentNameLocation = getPropertyValue( ATTACHMENT_NAME_LOCATION_PROP );

            /**
             * If inline is false then one of attachmentNameLocation
             * or defaultAttachmentName would be required.
             */
            if ( !StringUtils.hasValue(attachmentNameLocation)
                    && !StringUtils.hasValue(defaultAttachmentName) )
            {
                Debug.log ( Debug.ALL_WARNINGS, "One of the DEFAULT_ATTACHMENT_NAME or ATTACHMENT_NAME_LOCATION is required\n" );
                errorMsg.append ( "One of the DEFAULT_ATTACHMENT_NAME or ATTACHMENT_NAME_LOCATION is required\n" );
            }
        }

        subjectLocation = getPropertyValue( SUBJECT_LOCATION_PROP );

        /* throw an exception if errorMsg is not empty */
        if ( 0 != errorMsg.length() )
        {
            Debug.log ( Debug.ALL_ERRORS,
                    "The following errors occured while initializing " + loggingClassName + ":\n" + errorMsg.toString() );
            throw new ProcessingException (
                    "The following errors occured while initializing " + loggingClassName + ":\n" + errorMsg.toString() );
        }

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, loggingClassName + ": Initialized" );
    }

    /**
     * Process the Transmission Info XML set in the context and send mail
     *
     * @param  context The MessageProcessorContext to be used for transactional database
     * @param  input Outbound message
     * @return  NVPair[] Null if successful, else exception is thrown
     * @exception  MessageException  thrown if bad message.
     * @exception  ProcessingException  thrown if processing fails.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input )
      throws MessageException, ProcessingException
    {
      /* Composing message using the persistent properties and the SMTPEmailUtils class's methods */
        if ( null == input )
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, loggingClassName + ": The input is null");
            return null;
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Processing....");

        transmissionInfo = new TransmissionInfo ();

        transmissionInfo.process( context, input );

        // Flush out all dynamic data
        transmissionInfo = null;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Processing over...." );

        return formatNVPair(input);
    }

    /**
     * This Inner Class is developed to handle batch processing for this e-mail client,
     * such that after initializing TransmissionEmailClient once, each time process method
     * is called, fresh e-mail properties are loaded and processed, except few which
     * were set in initialize method.
     */
    private class TransmissionInfo
    {
        /**
         * Abbreviated class name for use in logging
         */
        private String loggingInnerClassName;

        /**
         * Sender email address
         */
        private String senderEmailAddr;

        /**
         * Recipient email address
         */
        private String recipientEmailAddress;

        /**
         * Subject for the email
         */
        private String subject;

        /**
         * Message body of the email
         */
        private String message;

        /**
         * cc email address list
         */
        private ArrayList ccEmailAddrList;

        /**
         * List of additional headers to be added
         */
        private String additionalHeaders [ ][ ];

        /**
         * Utils class reference to be used to compose and send email
         */
        private EmailInterface emailInterfaceObj = null;

        /**
         * Attachment content to be sent with email
         */
        private Object attachment;

        /**
         * Name of attachment to be used in the email
         */
        private String attachmentName;

        /**
         * Message text used for sent-from
         */
        private static final String MSG_TEXT_SENT_FROM = "Sent from:";

        /**
         * Message text used for Telephone Number
         */
        private static final String MSG_TEXT_TEL_NO = "Tel No:";

        /**
         * Constructor
         */
        public TransmissionInfo()
        {
            loggingInnerClassName = StringUtils.getClassName( this );
        }

        /**
         * Process e-mail properties for TransmissionEmailClient
         * @param  context The MessageProcessorContext to be used for transactional database
         * @param  input Outbound message
         * @exception  MessageException  thrown if bad message.
         * @exception  ProcessingException  thrown if processing fails.
         */
        public void process( MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException
        {
            /* get the subject text from context location */
            if ( StringUtils.hasValue (subjectLocation) )
                subject = getString( subjectLocation, context, input );

            /* get the attachment content, if its location is given */
            if ( StringUtils.hasValue(attachmentLocation) )
            {
                /* Get the attachment from context location */
                attachment = get( attachmentLocation, context, input );

                if ( null == attachment  )
                {
                    Debug.log( Debug.MSG_ERROR, "Attachment content not found at: " + attachmentLocation);
                    throwMessageException ( loggingInnerClassName + ": ERROR: Attachment content not found at: " + attachmentLocation );
                }

                /* If the attachment is not inline then get the attachment name */
                if ( !inline )
                {
                    /* Get the attachment name from context location */
                    if ( StringUtils.hasValue (attachmentNameLocation) )
                        attachmentName = getString( attachmentNameLocation, context, input );

                    /* Use default attachment name, if not set in the context location */
                    if( !StringUtils.hasValue(attachmentName) )
                        attachmentName = defaultAttachmentName;
                }
            }

            /**
             * Get the Transmission Info XML from context.
             */
            String transmissionInfoString = getString( transmissionInfoLocation, context, input );

            /**
             * Convert the Transmission Info XML string to Document.
             */
            Document transmissionInfoDOM = XMLLibraryPortabilityLayer.convertStringToDom(transmissionInfoString);

            if ( null == transmissionInfoDOM )
            {
                Debug.log( Debug.MSG_ERROR, "Transmission Info not found at: " + transmissionInfoLocation);
                throwMessageException ( loggingInnerClassName + ": ERROR: Transmission Info not found at: " + transmissionInfoLocation );
            }

            /* Process Transmission Info and set email properties */
            processTransmissionInfo( transmissionInfoDOM, context, input );

            try
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, loggingInnerClassName + ": Composing and sending email" );
                composeAndSendEmail ( );
            }
            catch ( ProcessingException pe )
            {
                throw pe;
            }
        }

        /**
         * Parse the Transmission Info XML and set corresponding email properties
         * @param  contextTransmissionInfoDOM An XML Document of Transmision Info
         * @exception  MessageException thrown if Transmission Info XML parsing fails
         */
        private void processTransmissionInfo( Document contextTransmissionInfoDOM, MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException
        {
            XMLPlainGenerator transmissionInfo = new XMLPlainGenerator( contextTransmissionInfoDOM );

            if ( Debug.isLevelEnabled ( Debug.XML_DATA ) )
                Debug.log( Debug.XML_DATA, loggingInnerClassName + ": TransmissionInfo XML: " + transmissionInfo.describe());

            /* Take cover_text as the subject for the mail if present in the Transmission Info */
            if ( transmissionInfo.exists(COVER_TEXT_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(COVER_TEXT_XPATH) ) )
            {
                subject = transmissionInfo.getValue(COVER_TEXT_XPATH);
                /* Replace the tokens present in the subject (cover text) by their values from context */
                subject = replaceTokens( subject, context, input );
            }

            /* Set the message text, if present in the Transmission Info */
            if ( transmissionInfo.exists(MESSAGE_TEXT_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(MESSAGE_TEXT_XPATH) ) )
            {
                message = transmissionInfo.getValue(MESSAGE_TEXT_XPATH);
                /* Replace the tokens present in the message text by their values from context */
                message = replaceTokens( message, context, input );
            }

            if ( StringUtils.hasValue(message) )
                message = message + NEW_LINE + NEW_LINE + generateMessageBody ( transmissionInfo );
            else
                message = generateMessageBody ( transmissionInfo );

            senderEmailAddr = generateFromAddress( transmissionInfo );

            recipientEmailAddress = generateRecipientAddress( transmissionInfo );

            ccEmailAddrList = generateCCAddrList( transmissionInfo );

            additionalHeaders = generateHeaders( transmissionInfo );
        }

        /**
         * Replace tokens in input target String with values set in the context.
         * Expected token is like %TOKEN_NAME%,
         * i.e. Token Name enclosed within TOKEN_WRAPPER character (currently '%')
         *
         * @param  target the target string for token replacement.
         * @param  context The MessageProcessorContext to be used for transactional database
         * @param  input Outbound message
         * @return  String the target String with tokens replaced by their values.
         * @exception  MessageException thrown if fails to replace values for tokens.
         * @exception  ProcessingException thrown if fails to replace values for tokens.
         */
        private String replaceTokens( String target, MessageProcessorContext context, MessageObject input )
            throws MessageException, ProcessingException
        {
            String strToken = null;
            String fullTokenName = null;
            String actualTokenName = null;
            String tokenValue = null;

            /* Tokenize the input target string into individual words. */
            StringTokenizer st = new StringTokenizer( target );

            /* Process each word of the message text to find tokens and obtain their values from context. */
            while ( st.hasMoreTokens() )
            {
                strToken = st.nextToken();

                /**
                 * Process each word of the target String for multiple tokens, if present.
                 * Check whether TOKEN_WRAPPER is present in the word (for opening token expression).
                 * If present then only process further.
                 */
                while ( strToken.indexOf(TOKEN_WRAPPER) != -1 )
                {
                    /* Take the string after starting TOKEN_WRAPPER */
                    strToken = strToken.substring( strToken.indexOf(TOKEN_WRAPPER) + 1 );

                    /* Check for closing TOKEN_WRAPPER, if present then string has a valid token name */
                    if ( strToken.indexOf(TOKEN_WRAPPER) != -1 )
                    {
                        /* Get the token name */
                        fullTokenName = strToken.substring( 0, strToken.indexOf(TOKEN_WRAPPER) );

                        if ( StringUtils.hasValue(fullTokenName) )
                        {
                            boolean isUpper = false;
                            boolean isLower = false;

                            /* If the token is enclosed with upper() or lower() function then get the actual token name. */
                            if ( fullTokenName.toLowerCase().indexOf(UPPER_CASE_FUNC) != -1 && fullTokenName.indexOf(CLOSING_PARENTHESES) == fullTokenName.length()-1 )
                            {
                                actualTokenName = fullTokenName.substring( UPPER_CASE_FUNC.length(), fullTokenName.indexOf(CLOSING_PARENTHESES) );
                                isUpper = true;
                            }
                            else if( fullTokenName.toLowerCase().indexOf(LOWER_CASE_FUNC) != -1 && fullTokenName.indexOf(CLOSING_PARENTHESES) == fullTokenName.length()-1 )
                            {
                                actualTokenName = fullTokenName.substring( LOWER_CASE_FUNC.length(), fullTokenName.indexOf(CLOSING_PARENTHESES) );
                                isLower = true;
                            }
                            else
                                actualTokenName = fullTokenName;

                            /* If exists, get the value of token from MessageProcessorContext */
                            if ( exists(CONTEXT_START + actualTokenName, context, input) )
                                tokenValue = getString( CONTEXT_START + actualTokenName, context, input );
                            else
                                tokenValue = null;

                            /* Change the case of token value if it is required in upper or lower,
                               otherwise value will be unchanged.*/
                            if ( tokenValue != null )
                                if (isUpper)
                                    tokenValue = tokenValue.toUpperCase();
                                else if (isLower)
                                    tokenValue = tokenValue.toLowerCase();

                            /* if not empty, replace the token by its value in the target */
                            if( StringUtils.hasValue(tokenValue) )
                            {
                                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                                    Debug.log( Debug.MSG_STATUS, loggingInnerClassName + ": Replacing token [" + fullTokenName + "] with value: [" + tokenValue + "]");
                                target = StringUtils.replaceSubstrings( target, TOKEN_WRAPPER + fullTokenName + TOKEN_WRAPPER, tokenValue );
                            }
                        }

                        /**
                         * Advance the current word to next starting TOKEN_WRAPPER.
                         * If no more TOKEN_WRAPPER are there then empty string will be returned.
                         */
                        strToken = strToken.substring( strToken.indexOf(TOKEN_WRAPPER) + 1);
                    }
                }
            }
            return target;
        }

        /**
         * Generate the message body.
         * The generated message body would have following format
         *
         * Sent from:
         * <Originator Name>
         * Tel No:<Originator Tel. No.>
         *
         * @return String  the generated message body.
         * @exception  MessageException  thrown if fails to retrieve originator's name or tel. no.
         */
        private String generateMessageBody( XMLPlainGenerator transmissionInfo )
            throws MessageException
        {
            StringBuffer messageBody = new StringBuffer();

            if ( transmissionInfo.exists(ORIGINATOR_NAME_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(ORIGINATOR_NAME_XPATH) ) )
            {
                messageBody.append(MSG_TEXT_SENT_FROM).append(NEW_LINE);

                messageBody.append(transmissionInfo.getValue(ORIGINATOR_NAME_XPATH)).append(NEW_LINE);

                if ( transmissionInfo.exists(ORIGINATOR_TELNO_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(ORIGINATOR_TELNO_XPATH) ) )
                    messageBody.append(MSG_TEXT_TEL_NO).append(transmissionInfo.getValue(ORIGINATOR_TELNO_XPATH)).append(NEW_LINE);
            }

            return messageBody.toString();
        }

        /**
         * Generate From address.
         * @return String  the generated From address.
         * @exception  MessageException  thrown if fails to retrieve sender's email address.
         */
        private String generateFromAddress( XMLPlainGenerator transmissionInfo )
            throws MessageException
        {
            StringBuffer senderAddress = new StringBuffer();

            if ( transmissionInfo.exists(FROM_EMAIL_ADDRESS_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(FROM_EMAIL_ADDRESS_XPATH) ) )
                senderAddress.append(transmissionInfo.getValue(FROM_EMAIL_ADDRESS_XPATH));
            else
            {
                Debug.log( Debug.MSG_ERROR, "Sender email address not found in the transmission info.");
                throwMessageException ( loggingInnerClassName + ": ERROR: Sender email address not found in the transmission info.");
            }

            return senderAddress.toString();
        }

        /**
         * Generate Recipient's address in "RecipientName (Telephone number) <RecipientEmailAddress>" format.
         * @return String  the generated Recipient's address.
         * @exception  MessageException  thrown if fails to retrieve recipient's name, telephone number or email address.
         */
        private String generateRecipientAddress( XMLPlainGenerator transmissionInfo )
            throws MessageException
        {
            StringBuffer recipientAddress = new StringBuffer();

            if ( transmissionInfo.exists(RECIPIENT_NAME_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(RECIPIENT_NAME_XPATH) ) )
            {
                recipientAddress.append(transmissionInfo.getValue(RECIPIENT_NAME_XPATH));

                /**
                 * If the Name of Recipient is present in the transmissio Info
                 * then only check and add Recipient's telphone number
                 */
                if ( transmissionInfo.exists(RECIPIENT_TELNO_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(RECIPIENT_TELNO_XPATH) ) )
                    recipientAddress.append(" (").append(transmissionInfo.getValue(RECIPIENT_TELNO_XPATH)).append(") ");
            }

            if ( transmissionInfo.exists(TO_EMAIL_ADDRESS_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(TO_EMAIL_ADDRESS_XPATH) ) )
                recipientAddress.append(" <").append(transmissionInfo.getValue(TO_EMAIL_ADDRESS_XPATH)).append(">");
            else
            {
                Debug.log( Debug.MSG_ERROR, "Recipient's email address not found in the transmission info: ");
                throwMessageException ( loggingInnerClassName + ": ERROR: Recipient's email address not found in the transmission info: ");
            }

            return recipientAddress.toString();
        }

        /**
         * Generate CC email address list.
         * @return ArrayList  the generated CC email address list.
         * @exception  MessageException  thrown if fails to retrieve CC email addresses.
         */
        private ArrayList generateCCAddrList( XMLPlainGenerator transmissionInfo )
            throws MessageException
        {
            ArrayList ccAddresses = null ;
            Node[] ccNodeList = null;

            /* get the CC nodes under cccontainer */
            if ( transmissionInfo.exists(CC_CONTAINER_XPATH) )
                ccNodeList = transmissionInfo.getChildren(CC_CONTAINER_XPATH);
            else
                return null;

            int ccAddressesCount = ccNodeList.length;

            if( 0 == ccAddressesCount )
            {
                Debug.log( Debug.MSG_ERROR, "Atleast one CC address is required in the transmission info." );
                throwMessageException ( loggingInnerClassName + ": ERROR: Atleast one CC address is required in the transmission info." );
            }

            ccAddresses = new ArrayList();

            for ( int i = 0; i < ccAddressesCount; i++ )
            {
                if( ccNodeList[i].getNodeName().equals( CC_NODE ) && StringUtils.hasValue ( transmissionInfo.getValue( ccNodeList[i] ) ) )
                    ccAddresses.add( transmissionInfo.getValue( ccNodeList[i] ) );
                else
                    Debug.log( Debug.MSG_WARNING, "Found invalid node name under CC-list or blank CC value in the transmissionInfo XML." );
            }

            if ( 0 == ccAddresses.size() )
            {
                Debug.log( Debug.MSG_ERROR, "Invalid CC-List found in the transmission info." );
                throwMessageException ( loggingInnerClassName + ": ERROR: Invalid CC-List found in the transmission info." );
            }

            return ccAddresses;
        }

        /**
         * Generate email headers.
         * @exception  MessageException  thrown if fails to retrieve any header information.
         */
        private String[][] generateHeaders( XMLPlainGenerator transmissionInfo )
            throws MessageException
        {
            String[][] headerNamesValueList = null;

            /* Set the Reply-To header name */
            if ( transmissionInfo.exists(REPLY_TO_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(REPLY_TO_XPATH) ) )
            {
                /* Currently only "Reply-To" is the extra header in the email Transmission Info. */
                headerNamesValueList = new String[1][2];

                headerNamesValueList[0][0] = transmissionInfo.getNode(REPLY_TO_XPATH).getNodeName();

                StringBuffer headerValue = new StringBuffer();
                /* Set the Reply-To header value in "Reply-ToName (Reply-To Telephone number) <Reply-ToEmailAddress>" format */
                if ( transmissionInfo.exists(NEUSTAR_NAME_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(NEUSTAR_NAME_XPATH) ) )
                {
                    headerValue.append( transmissionInfo.getValue(NEUSTAR_NAME_XPATH) );
                    if ( transmissionInfo.exists(NEUSTAR_TELNO_XPATH) && StringUtils.hasValue ( transmissionInfo.getValue(NEUSTAR_TELNO_XPATH) ) )
                        headerValue.append( " (" ).append( transmissionInfo.getValue(NEUSTAR_TELNO_XPATH) ).append( ") ");
                }

                headerValue.append( " <" ).append( transmissionInfo.getValue(REPLY_TO_XPATH) ).append( ">" );

                headerNamesValueList[0][1] = headerValue.toString();
            }

            return headerNamesValueList;
        }

        /**
         * Compose and send the email using the utils classes
         * @exception  ProcessingException  thrown if message composition fails
         */
        private void composeAndSendEmail ( )
          throws ProcessingException
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log ( Debug.MSG_STATUS, loggingInnerClassName + ": Creating service class");

            try
            {
                /* Initialize email interface object to compose and send email */
                emailInterfaceObj = ( EmailInterface ) ObjectFactory.create ( emailClientServiceClass );

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log ( Debug.MSG_STATUS, loggingInnerClassName + ": Created service class");
                    Debug.log ( Debug.MSG_STATUS, loggingInnerClassName + ": Initializing server settings..");
                }

                emailInterfaceObj.initServer ( smtpServerAddr );

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log ( Debug.MSG_STATUS, loggingInnerClassName + ": Setting sender");

                emailInterfaceObj.setSender ( senderEmailAddr );

                /* Add TO recipients */
                if ( StringUtils.hasValue(recipientEmailAddress) )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log ( Debug.MSG_STATUS, loggingInnerClassName + ": Setting TO recipients");

                    emailInterfaceObj.addRecipient ( recipientEmailAddress );

                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log ( Debug.MSG_STATUS, loggingInnerClassName + ": The message will be sent to ["+ recipientEmailAddress +"]");
                }

                /* Add CC recipients */
                if ( null != ccEmailAddrList )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log ( Debug.MSG_STATUS, loggingInnerClassName + ": Setting CC recipients");

                    for ( int i = 0; i < ccEmailAddrList.size(); i++ )
                        emailInterfaceObj.addCC ( (String) ccEmailAddrList.get(i) );
                }

                /* Add additional headers (if exist) */
                if ( null != additionalHeaders )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Setting additional headers");

                    for ( int i = 0; i < additionalHeaders.length; i++ )
                        emailInterfaceObj.addHeader ( additionalHeaders [i][0], additionalHeaders [i][1] );
                }

                /* Add Subject for the email */
                if ( null != subject )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Setting SUBJECT field");

                    emailInterfaceObj.setSubject ( subject );
                }

                /**
                 * If attachment is to be made inline, append it at the end of email body.
                 * Otherwise set it as attachment.
                 */
                if ( true == inline )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Setting attachment as inline message in the e-mail");

                    String messageBody = "";

                    if ( StringUtils.hasValue( message ) )
                        messageBody = message + NEW_LINE + NEW_LINE;

                    if ( null != attachment )
                        messageBody = messageBody + (String) attachment;

                    emailInterfaceObj.setMessageContent ( messageBody );
                }
                else if ( null != attachment )
                {
                    Object inputAttachmentArray [] = new Object [1];
                    inputAttachmentArray [0] = attachment;

                    String attachmentTypeArray [] = new String [1];
                    attachmentTypeArray [0] = attachmentType;

                    String attachmentNameArray [] = new String [1];
                    attachmentNameArray [0] = attachmentName;

                    if ( StringUtils.hasValue( message ) )
                    {
                        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                            Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Setting message and attachment");
                        emailInterfaceObj.setMessageContent ( message, inputAttachmentArray, attachmentTypeArray, attachmentNameArray );
                    }
                    else
                    {
                        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                            Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Setting attachment");
                        emailInterfaceObj.setMessageContent ( inputAttachmentArray, attachmentTypeArray, attachmentNameArray );
                    }
                }
                else if ( StringUtils.hasValue( message ) )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Setting message only");
                    emailInterfaceObj.setMessageContent ( message );
                }
                else
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log (Debug.ALL_WARNINGS, loggingInnerClassName + ": Setting blank message, as there is no message body or attachment.");
                    emailInterfaceObj.setMessageContent ( "" );
                }

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Sending email...");

                /* Sending email */
                emailInterfaceObj.sendEmail ();

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log (Debug.MSG_STATUS, loggingInnerClassName + ": Email sent successfully");
            }
            catch (FrameworkException fe)
            {
                Debug.log( Debug.MSG_ERROR, loggingInnerClassName + ": ERROR: " + fe.getMessage () );
                throw new ProcessingException ( loggingInnerClassName + ": ERROR: " + fe.getMessage () );
            }
        }
    }

    /**
     * main method to test TransmissionEmailClient locally.
     */
    public static void main(String[] args)
    {
        /* Initialize Debug logging. */
        Hashtable htLogTable = new Hashtable();
        htLogTable.put(Debug.DEBUG_LOG_LEVELS_PROP,"ALL");
        htLogTable.put(Debug.LOG_FILE_NAME_PROP,"transmissionEmailClient.log");
        htLogTable.put(Debug.MAX_DEBUG_WRITES_PROP,"10000");

        /* Turn on maximum diagnostic logging. */
        Debug.showLevels();
        Debug.configureFromProperties(htLogTable);
        Debug.enable( Debug.ALL_ERRORS );
        Debug.enable( Debug.ALL_WARNINGS );

        if ( 3 != args.length )
        {
            System.out.println("USAGE: \"TransmissionEmailClient jdbc:oracle:thin:@<DB_SERVER_IP>:1521:<SID> <DB_USER_NAME> <DB_PASSWORD>\"");
            Debug.log (Debug.ALL_ERRORS, "USAGE: \"TransmissionEmailClient jdbc:oracle:thin:@<DB_SERVER_IP>:1521:<SID> <DB_USER_NAME> <DB_PASSWORD>\"");
            return;
        }

        try
        {
            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             Debug.log( Debug.MAPPING_ERROR, "TransmissionEmailClient.main: " +
                      "Database initialization failure: " + e.getMessage());
        }

        TransmissionEmailClient testTEC = new TransmissionEmailClient();

        try
        {
            MessageProcessorContext mpx = new MessageProcessorContext();

            String strTransInfo1 = "<?xml version=\"1.0\"?><transmission_info> \n" +
                    "  <mechanism value=\"email\" /> \n" +
                    "  <email_info>   \n" +
                    "    <transmit_info>     \n" +
                    "      <to_email_address value=\"sbahauddin@impetus.co.in\" />     \n" +
                    "      <from_email_address value=\"sbahauddin@impetus.co.in\" />     \n" +
                    "      <cover_text value=\"Please process this order in a timely manner %uppER(ORDTYPE)%:(%LoWeR(TRADINGPARTNER)%):(%SIZE%):(%THRESHOLD%).\" />     \n" +
                    "      <message_text value=\"Please be advised that the Clearinghouse queue size for %lower(ORDTYPE)% to trading partner (%UPPER(TRADINGPARTNER)%) is currently at (%lower(SIZE)%) and has exceeded the threshold limit of (%upper(THRESHOLD)%).\" />     \n" +
                    "      <cccontainer>       \n" +
                    "        <cc value=\"abc\" />     \n" +
                    "      </cccontainer>   \n" +
                    "    </transmit_info>   \n" +
                    "    <response_info>     \n" +
                    "      <reply-to value=\"sbahauddin@gmail.com\" />   \n" +
                    "    </response_info>   \n" +
                    "    <originator>     \n" +
                    "      <name value=\"XO Porting Center (West)\" />     \n" +
                    "      <telno value=\"5105551212\" />    \n" +
                    "    </originator>   \n" +
                    "    <recipient>     \n" +
                    "      <name value=\"Porting Center\" />     \n" +
                    "      <telno value=\"7035551213\" />    \n" +
                    "    </recipient>   \n" +
                    "    <neustar>     \n" +
                    "      <name value=\"Tier I OMS Support\" />     \n" +
                    "      <telno value=\"5714345173\" />    \n" +
                    "    </neustar> \n" +
                    "  </email_info>\n" +
                    "</transmission_info>";

            mpx.set("TRANSMISSIONINFO",strTransInfo1);

            String strSubject = "PON1-VER1";
            mpx.set("SUBJECT", strSubject);

            String strAttachment = "Hello! This is a sample attachment text.";
            mpx.set("ATTACHMENT", strAttachment);

            String attName = "ATTACHMENT_NAME_FROM_CONTEXT.rtf";
            mpx.set("ATT_NAME", attName);

            mpx.set("ORDTYPE", "Test_Order");
            mpx.set("TRADINGPARTNER", "Test_TP");
            mpx.set("SIZE", "100000");
            mpx.set("THRESHOLD", "99999");

            MessageObject msob = new MessageObject();
            msob.set("TEST");

            /* Load Properties for Transmission Email Client */
            testTEC.initialize("LSR_FAX","TransmissionEmailClient");
            testTEC.process(mpx, msob);

            /* To test batch processing call process method again with different properties */
            String strTransInfo2 = "<transmission_info> \n" +
                    "  <mechanism value=\"email\" /> \n" +
                    "  <email_info>   \n" +
                    "    <transmit_info>     \n" +
                    "      <to_email_address value=\"sbahauddin@impetus.co.in\" />     \n" +
                    "      <from_email_address value=\"sbahauddin@impetus.co.in\" />     \n" +
                    "      <cccontainer>       \n" +
                    "        <cc value=\"abc\" />     \n" +
                    "      </cccontainer>   \n" +
                    "    </transmit_info>   \n" +
                    "    <response_info>     \n" +
                    "      <reply-to value=\"bpo_center@neustar.biz\" />   \n" +
                    "    </response_info>   \n" +
                    "  </email_info>\n" +
                    "</transmission_info>";

            mpx.set("EMAIL_INFO", strTransInfo2);

            String strSubject2 = "PON1-VER1";
            mpx.set("SUBJECT", strSubject2);

            String strAttachment2 = "Hello! This is a sample attachment text.";
            mpx.set("ATTACHMENT", strAttachment2);

            MessageObject msob2 = new MessageObject();
            msob2.set("TEST2");

            // To put time difference between two test mails
            Thread.sleep(1000);

            //testTEC.process(mpx, msob2);
        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }
} //End of TransmissionEmailClient

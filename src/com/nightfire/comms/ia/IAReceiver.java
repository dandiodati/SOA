/*
 * Copyright(c) 2004, Neustar Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia;

// jdk imports
import java.io.*;
import java.sql.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import javax.net.ssl.*;


// third party imports

// NightFire imports
import com.nightfire.adapter.messageprocessor.EDITradingPartnerExtractor;
import com.nightfire.common.ProcessingException;
import com.nightfire.comms.ia.asn.ASNDefinition;
import com.nightfire.comms.ia.asn.ASNFactory;
import com.nightfire.comms.ia.asn.ASNData;
import com.nightfire.comms.ia.asn.msg.*;
import com.nightfire.framework.db.PropertyChainUtil;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.monitor.ThreadMonitor;

/**
 * As noted in {@link IAServer IAServer}, IAReceiver is responsible for
 * receiving a message from a remote IA.  Each IAReceiver runs in a separate
 * thread, and that thread then utilizes IAServer to finish processing.
 */
public class IAReceiver implements Runnable
{
    /**
     * The prefix for retrieving ASNFactory configuration OIDs.
     */
    private static final String ASN_OID_PREFIX = "IA_MSG_OID_";

    /**
     * The prefix for retrieving ASNFactory configuration class names.
     */
    private static final String ASN_CLASS_PREFIX = "IA_MSG_CLASS_";

    /**
     * The property name that indicates whether or not to send receipts.
     */
    private static final String SEND_RECEIPT_PROP_NAME = "SEND_RECEIPT";

    /**
     * The property name that points to IA Client properties.
     */
    private static final String IA_CLIENT_PROPS_TYPE = "IA_CLIENT_TYPE";

    /**
     * The property name that specifies the Customer ID.
     * Used only for dynamic client settings.
     */
    private static final String CUSTOMER_ID_PROP_NAME = "CUSTOMER_ID";

    /**
     * The property name that specifies the destination host to connect to.
     * Used only for dynamic client settings.
     */
    private static final String DEST_HOST_PROP_NAME = "DEST_IA_HOST";

    /**
     * The property name that specifies the destination port to connect to.
     * Used only for dynamic client settings.
     */
    private static final String DEST_PORT_PROP_NAME = "DEST_IA_PORT_NUMBER";

    /**
     * The property name that specifies the remote host to connect to.
     */
    private static final String REMOTE_HOST_PROP_NAME = "REMOTE_IA_HOST";

    /**
     * The property name that specifies the remote port to connect to.
     */
    private static final String REMOTE_PORT_PROP_NAME =
        "REMOTE_IA_PORT_NUMBER";

    /**
     * The property name that contains the OID for a basic receipt.
     */
    private static final String RECEIPT_OID_PROP_NAME = "BASIC_RECEIPT_OID";

    /**
     * The property name for the error dir.
     */
    private static final String ERROR_DIR_PROP_NAME = "ERROR_DIR";

    /**
     * The property name for the exception dir.
     */
    private static final String EXCEPTION_DIR_PROP_NAME = "EXCEPTION_DIR";

    /**
     * The property name for persisted data file base names.
     */
    private static final String FILE_BASE_PROP_NAME =
        "EXCEPTION_FILE_BASE_NAME";

    /**
     * The property name for binary persisted data file extensions.
     */
    private static final String FILE_EXTENSION_PROP_NAME =
        "EXCEPTION_BINARY_EXTENSION";

     /**
     * The property name for flag to use default IA Client.
     */
    private static final String USE_DYNAMIC_CLIENTS_PROP = "USE_DYNAMIC_CLIENTS";

    /**
     * The property name for the character encoding to use.
     */
    private static final String CHAR_ENCODING_PROP_NAME =
        "CHARACTER_ENCODING";

    /**
     * IAClient NUMBER_PERSISTENT_CONNECTIONS Property: The property name for specifying the socket cache size
     */
    private static final String NUMBER_PERSISTENT_CONNECTIONS_PROP = "NUMBER_PERSISTENT_CONNECTIONS";

    /**
     * The ASN definitions.
     */
    private static ASNDefinition[] definitions = null;

    /**
     * Indicates whether to send a receipt for EDI messages or not.
     */
    private static boolean sendReceipt = false;

    /**
     * The remote host to connect to for receipts.
     */
    private static String remoteHost = null;

    /**
     * The remote port to connect to for receipts.
     */
    private static int remotePort = 0;

    /**
     * The destination host IP to be obtained from hashtable.
     */
    private String destHostIp = null;

    /**
     * The destination port to be obtained from hashtable.
     */
    private int destHostPort = 0;

    /**
     * The OID for a basic receipt.
     */
    private static String receiptOID = null;

    /**
     * Error dir
     */
    private static String errorDir = null;

    /**
     * Exception dir
     */
    private static String exceptionDir = null;

    /**
     * File base name
     */
    private static String fileBase = null;

    /**
     * File extension
     */
    private static String fileExt = null;

    /**
     * The character encoding
     */
    private static String enc = null;

    /**
     * The socket used for receiving data.
     */
    private Socket socket = null;

    /**
     * The IAServer which will process any received data.
     */
    private IAServer server = null;

    /**
     * The IAServer which will process any received data.
     */
    private static boolean useDynamicClients = false;

    /**
     * Size of socket cache for Issue3
     */
    private static int numConnections = 8;

    /**
     * The hashtable to store dynamic ia client information.
     */
    private static Hashtable clients = new Hashtable();

    /**
    * The name of the table where the IA receipt destination info will
    * be looked up if it cannot be located in the persistent properties.
    */
    private static final String LOOKUP_TABLE = "IA_RECEIPT_DEST";

    /**
     * Select query for looking up the the dynamic IA receipt destination
     * info if it is not specified in the properties. This lookup
     * is provided for convenience of deploying IA Servers in the
     * clearinghouse environment (specifically those such as SBC that
     * dynamically lookup the receipt destination).
     *
     * @see #lookupDestinationInfo
     */
    private static final String DEST_INFO_QUERY =
                                  "SELECT DEST_IA_HOST, DEST_IA_PORT FROM "+
                                  LOOKUP_TABLE+
                                  " WHERE TPID = ?";

     /**
      * Object lock for benchmarking
      */
     private static Object lock = new Object();    
      
     /**
      * The IA Issue version for this server
      */
     private String iaIssueVersion;

      
    /**
     * The number of requests processed by this server
     * for benchmarking
     */
     private static long count;

    /**
     * The time this server was started
     */
    private static long startTime = System.currentTimeMillis();


    /**
     * This is the constructor.  It takes a socket over which communication
     * will occur and a reference to the IAServer which accepted the
     * connection.  That IAServer will later be used to process any received
     * messages.
     *
     * @param socket   The socket to receive data over
     * @param server   The IAServer which will process any received data.
     */
    public IAReceiver(Socket socket, IAServer server, String iaIssueVersion )
    {
        // initialize the data
        this.socket = socket;
        this.server = server;
    	this.iaIssueVersion = iaIssueVersion;
    }

    /**	
     * The run method is the entry point for a new thread.  This is where
     * processing occurs.
     */
    public void run()
    {
        // input stream for the socket
        InputStream is = null;	

        // high level exception block to catch all exceptions for the thread
        try
        {
           
	    if ( iaIssueVersion.equals(IAServer.IA_ISSUE2) )	
            {
                // perform the handshake instead of the server
                
                server.getSSLLayer().performHandshake(socket);
            }

            // get an input stream for the socket
            is = new ReadFullyInputStream(new BufferedInputStream(socket.getInputStream()));

            // get the remote IP from the socket
            // sourceHostIp = socket.getInetAddress().getHostAddress();

            // get a factory to read the data
            ASNFactory factory = getFactory();

            if ( iaIssueVersion.equals(IAServer.IA_ISSUE2) )
            {
            
                // read the data
                ASNData data = factory.instanceFor(is);

                // handle it
                handleMessage(data);
            }
            
            //In IA3 we keep the socket open.
            else
            {
                while ( true )
                {                    
                    // read the data
                    ASNData data = factory.instanceFor(is);	

                    if ( data == null) break;	
		    
		    // handle it
                    handleMessage(data);                         
                }
                                
            } 

	    if ( Debug.isLevelEnabled(Debug.BENCHMARK) )	
	    {
		    Debug.log( Debug.BENCHMARK, "IAReceiver requests [" 
				    		+ IAReceiver.addOneToCount() + "], " 
                        			+ " ELAPSED TIME [" 
						+ ( System.currentTimeMillis( ) - startTime) 
						+ "] msec." );
	    }
        }
        catch(Exception e)
        {
            Debug.log(Debug.ALL_ERRORS,	
                      "An exception occured in IAReceiver: " + e.toString());
        }
        finally
        {
            // close the input stream
            if (is != null)
            {
                try { is.close(); }
                catch (IOException ex)
                {
                    Debug.log(Debug.ALL_ERRORS, 
			"An error occured in IAReceiver trying to close the socket connection: "
                         + ex.toString());
                }
            }

            // close the socket
            if (socket != null)
            {
                try { socket.close(); }
                catch (IOException ex)
                {
                    Debug.log(Debug.ALL_ERRORS,
      "An error occured in IAReceiver trying to close the socket connection: "
                              + ex.toString());
                }
            }

	    //tell the server to forget about this IAReceiver
	    if ( iaIssueVersion.equals(IAServer.IA_ISSUE3) )
	    {
		    server.removeReceiver(this);
	    }
    
        }
    }

       /**
     * Handles received messages.
     *
     * @param data  The received message to process.
     */
    private void handleMessage(ASNData data)
    {
             
        Debug.log( Debug.MSG_DATA, "IAReceiver received object: " + data );
        
                // see if this is an edi message
        if (data instanceof IABasicEDI)
        {
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE,
                      "Received a Basic EDI message.");

            // get the data
            if (StringUtils.hasValue(enc))
                ((IABasicEDI)data).setEncoding(enc);
            String msgData = ((IABasicEDI)data).getEDIContent();

            // process it
            server.processData(msgData, enc);
              // see if we need to send a receipt
            if (sendReceipt)
            {
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "Sending a receipt.");
                
                try
                {
                    sendISAReceipt(msgData);
                }
                catch(Exception e)
                {
                    Debug.log(Debug.ALL_ERRORS,
                 "An exception occured in IAReceiver while sending a receipt: "
                              + e.toString());
                }
            }
            else
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "Not sending a receipt.");

        }
        else if (data instanceof IABasicReceipt)
        {
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE, "Received a Basic Receipt.");

            if (Debug.isLevelEnabled(Debug.MSG_DATA))
            {
                String isa = ((IABasicReceipt)data).getISASegment();
                String ts = ((IABasicReceipt)data).getDateTimeStampString();

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                {
                    Debug.log(Debug.MSG_DATA, "ISA:        [" + isa + "]");
                    Debug.log(Debug.MSG_DATA, "Time stamp: [" + ts + "]");
                }
            }
        }
        else if (data instanceof IAStatus || data instanceof IaStatusMessage )
        {
            //we accept status message but do nothing with them.
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE, "Received a status message.");
        }
        else if ( data instanceof BasicMessage )
        {
            
            // get the data
            if (StringUtils.hasValue(enc))
                ((BasicMessage)data).setEncoding(enc);
            
            String msgData = ((BasicMessage)data).getEDIContent();

            // process it
            server.processData(msgData, enc);
            if (sendReceipt)
            {
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "Sending a receipt.");
                
                try
                {
                    //send back an Issue3 Receipt Message
                    sendISAReceiptIssue3(msgData);
                }
                catch(Exception e)
                {
                    Debug.log(Debug.ALL_ERRORS,
                 "An exception occured in IAReceiver while sending a receipt: "
                              + e.toString());
                }
            }
            else
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "Not sending a receipt.");

            
        }      
        else if ( data instanceof IaReceiptMessage )
        {
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE, "Received a Issue3 Basic Receipt.");

            if (Debug.isLevelEnabled(Debug.MSG_DATA))
            {
                String isa = ((IaReceiptMessage)data).getISASegment();
                String ts = ((IaReceiptMessage)data).getDateTimeStampString();

                Debug.log(Debug.MSG_DATA, "ISA:        [" + isa + "]");
                Debug.log(Debug.MSG_DATA, "Time stamp: [" + ts + "]");
            }
        }        
        else
        {
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE, "Received a message of type ["
                      + data.getClass().getName() + "].");
        }
    
    }


    /**
     * Returns a prepared ASNFactory for reading an ASN.1 message from a
     * stream using DER encoding.
     *
     * @return    A prepared ASNFactory
     *
     * @exception ProcessingException  Thrown if the ASNFactory cannot be
     *                                 configured.
     */
    private ASNFactory getFactory() throws ProcessingException
    {
        // create a factory
        ASNFactory factory = new ASNFactory(fileBase, fileExt, exceptionDir,
                                            errorDir);

        // add the definintions to the factory
        for (int i = 0; i < definitions.length; i++)
            factory.addDefinition(definitions[i]);

        // return the factory
        return factory;
    }

    /**
     * Initializes data from properties for the receiver.
     *
     * @param key  The property key to use for initialization
     * @param type The property type to use for initialization
     *
     * @exception   ProcessingException  Thrown if there is a problem with the
     *                                  properties.
     */
    public static void initialize(String key, String type)
        throws ProcessingException
    {
        // get the properties
        StringBuffer errBuff = new StringBuffer();
        Hashtable props = loadProps(key, type, errBuff);

        // get the error directory
        errorDir = getRequiredProp(props, ERROR_DIR_PROP_NAME, errBuff);

        // get the exception directory
        exceptionDir = getRequiredProp(props, EXCEPTION_DIR_PROP_NAME,
                                       errBuff);

        // get the file base name
        fileBase = getRequiredProp(props, FILE_BASE_PROP_NAME, errBuff);

        // get the file extension
        fileExt = getRequiredProp(props, FILE_EXTENSION_PROP_NAME, errBuff);

        // get the receipt flag
        String sendTxt = getRequiredProp(props, SEND_RECEIPT_PROP_NAME,
                                         errBuff);
        try
        {
            sendReceipt = StringUtils.getBoolean(sendTxt);
        }
        catch(Exception e)
        {
            errBuff.append("Could not convert property [")
                .append(SEND_RECEIPT_PROP_NAME)
                .append("] to a boolean value: ")
                .append(e.toString())
                .append("\n");
        }

        // get the default client flag
        String useNew = (String) props.get(USE_DYNAMIC_CLIENTS_PROP);
        try
        {
            if (StringUtils.hasValue( useNew ))
                useDynamicClients = StringUtils.getBoolean(useNew);
        }
        catch(Exception e)
        {
            errBuff.append("Could not convert property [")
                .append(USE_DYNAMIC_CLIENTS_PROP)
                .append("] to a boolean value: ")
                .append(e.toString())
                .append("\n");
        }

        // get an enumeration of OIDs
        int i = 0;
        String oid = null;
        ArrayList defs = new ArrayList();
        while ( StringUtils.hasValue(
                    (oid = (String)props.get(ASN_OID_PREFIX + i)) ) )
        {
            // validate the oid
            checkOID(oid, ASN_OID_PREFIX + i, errBuff);

            // get a class name
            String className = getRequiredProp(props, ASN_CLASS_PREFIX + i,
                                               errBuff);

            // create a definition
            ASNDefinition def = new ASNDefinition(oid, className);

            // add it to the list
            defs.add(def);

            // move to the next OID
            i++;
        }

        // make sure we had at least one definition
        if (defs.size()==0)
            errBuff.append("No IA Message definitions were found.\n");

        // get the client properties
            String clientType = getRequiredProp(props, IA_CLIENT_PROPS_TYPE,
                                            errBuff);
            Hashtable clientProps = loadProps(key, clientType, errBuff);

        // get the max allowable connections in the socket pool
        // this is a statically configured property on the iaclient property set.
            String numConnectionsStr = (String)clientProps.get(NUMBER_PERSISTENT_CONNECTIONS_PROP);
            
        if ( StringUtils.hasValue(numConnectionsStr) )
          {
              try
              {
                      numConnections = Integer.parseInt(numConnectionsStr);
              }
              catch ( NumberFormatException nfe )
              {
                      Debug.log(Debug.ALL_WARNINGS, "Invalid value ["+numConnectionsStr+"] for property ["
                                                    + NUMBER_PERSISTENT_CONNECTIONS_PROP + "] it should be an valid integer, "
                                                    + "using default value: " + numConnections );
              }
        }

        //If static configuration is set(original behaviour).
        if ( !useDynamicClients )
        {
            // get the remote host to connect to
            // this is a statically configured property on the iaclient property set.
            remoteHost = getRequiredProp(clientProps, REMOTE_HOST_PROP_NAME,
                                     errBuff);

            // get the remote port to connect to
            // this is a statically configured property on the iaclient property set.
            String portStr = getRequiredProp(clientProps, REMOTE_PORT_PROP_NAME,
                                         errBuff);
            try
            {
                remotePort = Integer.parseInt(portStr);
            }
            catch (NumberFormatException e)
            {
                errBuff.append("The value [" + portStr + "] of property [" +
                             REMOTE_PORT_PROP_NAME + "] is not an integer.\n");
            }
        }//static configuration
        else
        // Get the list of client properties when dynmic client is set to true.
        {
            int j = 0;

            while(true)
            {

                // Get destination information from props
                //For each customer id that we receive a message for, there should be a configuration
                //to indicate the destination host&port to send the receipt to.
                //These properties are configured on the IAServer.`
                String destPortStr = (String) props.get( DEST_PORT_PROP_NAME + "_" + j );
                String destHost = (String) props.get( DEST_HOST_PROP_NAME + "_" + j );
                String customerId = (String) props.get( CUSTOMER_ID_PROP_NAME + "_" + j );

                int destPort = 0;
                //We stop fetching properties when we encounter no more source properties.
                if ( !StringUtils.hasValue( customerId ) )
                    break;
                else
                {
                    //the destination host and port should both be set.
                    if ( ( StringUtils.hasValue( destPortStr ) ) && ( StringUtils.hasValue( destHost ) ) )
                    {
                        try
                        {
                             destPort = Integer.parseInt( destPortStr );
                        }
                        catch (NumberFormatException e)
                        {
                            errBuff.append(
                            "The value [" ).append( destPortStr ).append(
                            "] of property [" ).append( DEST_PORT_PROP_NAME
                            ).append( "] is not an integer.\n" );
                        }//catch
                    }//no value is null.
                    else
                    {
                        errBuff.append ( "The iterative property set [" ).append(j).append(
                        "] for dynamic client configuration has destination port/host information missing." );
                    }

                }//let us try fetching the property values.

                customerId = customerId.trim();
                clients.put( customerId, new DestinationInfo(destHost, destPort) );
                j++;

            }//while
        }//if useDynamicClients

        // get the OID for basic receipts
        receiptOID = getRequiredProp(props, RECEIPT_OID_PROP_NAME, errBuff);
        checkOID(receiptOID, RECEIPT_OID_PROP_NAME, errBuff);

        // get the optional character encoding
        enc = (String)props.get(CHAR_ENCODING_PROP_NAME);

        // throw an exception of any errors occured
        if (errBuff.length() != 0)
            throw new ProcessingException(
              "The following errors occured while initializing IAReceiver:\n"
              + errBuff.toString());

        // set up the array of definitions
        definitions = new ASNDefinition[defs.size()];
        definitions = (ASNDefinition[])defs.toArray(definitions);
    }

    /**
     * Retrieves a required property
     *
     * @param props   The properties to use
     * @param name    The name of the property
     * @param errBuff The buffer to write any error messages to
     *
     * @return        The value of the property
     */
    private static String getRequiredProp(Hashtable props, String name,
                                   StringBuffer errBuff)
    {
        String val = (String)props.get(name);

        if (!StringUtils.hasValue(val))
            errBuff.append("The required property [" + name +
                           "] could not be found.\n");

        return val;
    }

    /**
     * Loads the properties for a given key and type.
     *
     * @param key     The property key to load
     * @param type    The property type to load
     * @param errBuff The buffer to store any error messages in
     *
     * @return        A hashtable with the properties, or an empy hash table
     *                if an error occured.
     */
    private static Hashtable loadProps(String key, String type,
                                       StringBuffer errBuff)
    {
        Hashtable props = null;

        try
        {
            PropertyChainUtil propChain = new PropertyChainUtil();
            props = propChain.buildPropertyChains(key, type);
        }
        catch(Exception e)
        {
            errBuff.append(
                "An error occured trying to load properties with key [" +
                key + "] and type [" + type + "]: " + e.toString() + "\n");
        }
        finally
        {
            if (props == null)
                props = new Hashtable();
        }

        return props;
    }

    /**
     * Verifies a given string is an OID.
     *
     * @param oid       The string to check
     * @param propName  The name of the property the value came from.  This
     *                  is used if an error message is written.
     * @param errBuff   The buffer to write any errors to
     */
    private static void checkOID(String oid, String propName,
                                 StringBuffer errBuff)
    {
        for (int i = 0; i < oid.length(); i++)
        {
            // each character must be a digit or a dot (.)
            char c = oid.charAt(i);

            if ( (!Character.isDigit(c)) && (c != '.') )
            {
                errBuff.append("[" + oid + "] from the property [" +
                       propName + "] is not a valid OID.  An OID must " +
                       "contain a string of dot-separated (.) digits.\n");

                return;
            }
        }
    }

    /**
     * Sends an IA Receipt to a remote host.
     *
     * @param msgData The message data to send the receipt for
     *
     * @exception     ProcessingException  Thrown if anything goes wrong while
     *                                     sending the message.
     * @exception     MessageException     Thrown if there is a problem with
     *                                     the message data.
     */
    private void sendISAReceipt(String msgData)
        throws ProcessingException, MessageException
    {

        //Thread Monitoring
		ThreadMonitor.ThreadInfo tmti = null;

        // get the ISA segment from the EDI response
        String isa = EDITradingPartnerExtractor.getISA( msgData );

        // extract the customer's trading partner ID from the ISA segment
        String cid = EDITradingPartnerExtractor.getCustomerID( isa );

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
        {
            Debug.log(Debug.MSG_DATA,
                      "Customer ID extracted from response EDI: [" + cid + "]");
        }

        // connect to the remote host
        Socket socket = null;
        OutputStream os = null;
        try
        {
            // create a receipt message
            IABasicReceipt receipt = new IABasicReceipt(receiptOID);
            if (StringUtils.hasValue(enc))
                receipt.setEncoding(enc);

            // set the ISA segment
            receipt.setISASegment(isa);

            // set the timestamp
            receipt.setDateTimeStamp(new Date());

            // debugging info
            if (Debug.isLevelEnabled(Debug.MSG_DATA))
            {
                Debug.log(Debug.MSG_DATA, "OID: [" + receiptOID + "]");
                Debug.log(Debug.MSG_DATA, "ISA Segment: [" + isa + "]");
                Debug.log(Debug.MSG_DATA, "Date Time Stamp: ["
                          + receipt.getDateTimeStampString() + "]");
            }


            // Set up the remote Host value
            initRemoteIp(cid);

            // open the socket
            Debug.log(Debug.IO_STATUS,
                      "IAReceiver: Opening new socket to destination ip [" +
                      destHostIp + "] and port [" + destHostPort +"]");

            if ( iaIssueVersion.equals(IAServer.IA_ISSUE2) )
	    {
            	socket = server.getSSLLayer().getSocket( destHostIp, destHostPort );
	    }
	    else
	    {
		//use JSSE 
            	SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
            	socket = factory.createSocket( destHostIp, destHostPort );
	    }

        //Start a monitor with Socket endpoint info
            tmti = ThreadMonitor.start( "IAReceiver: Sending EDI receipt to TP. \n  Remote endpoint info : " +socket.toString()
                                + "\n  Local endpoint info :" + socket.getLocalSocketAddress());

            os = new SnoopOutputStream (new BufferedOutputStream(socket.getOutputStream() ));

            Debug.log(Debug.IO_STATUS, "IAReceiver: Got output stream, sending receipt");
            // encode the message
            receipt.encode(ASNData.DER_CODING, os);	 
           
	    Debug.log(Debug.IO_STATUS, "IAReceiver: Encoded receipt sent.");
    
        }
        catch (IOException e)
        {
            throw new ProcessingException(e.toString());
        }
        finally
        {
            // close the input stream
            if (os != null)
            {
                try { os.close(); }
                catch (IOException ex)
                {
                    Debug.log(Debug.ALL_ERRORS,
                               "An error occured in IAReceiver trying to close the output Stream: "
                               + ex.toString());
                }
            }

            // disconnect
            if (socket != null)
            {
                try { socket.close(); }
                catch (IOException ex)
                {
                    Debug.log(Debug.ALL_ERRORS,
                        "An error occured in IAReceiver trying to close the " +
                        "socket connection after sending a receipt: " +
                         ex.toString());
                }
            }
            ThreadMonitor.stop(tmti);
        }
    } 

    /**
     * Sends an IA Receipt to a remote host.
     *
     * @param msgData The message data to send the receipt for
     *
     * @exception     ProcessingException  Thrown if anything goes wrong while
     *                                     sending the message.
     * @exception     MessageException     Thrown if there is a problem with
     *                                     the message data.
     */
    private void sendISAReceiptIssue3(String msgData)
        throws ProcessingException, MessageException
    {
        //Thread Monitoring
		ThreadMonitor.ThreadInfo tmti = null;

        // get the ISA segment from the EDI response
        String isa = EDITradingPartnerExtractor.getISA( msgData );

        // extract the customer's trading partner ID from the ISA segment
        String cid = EDITradingPartnerExtractor.getCustomerID( isa );

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
        {
            Debug.log(Debug.MSG_DATA,
                      "Customer ID extracted from response EDI: [" + cid + "]");
        }

        // connect to the remote host
	IASocket iaSocket = null;
        Socket socket = null;
        OutputStream os = null;
	EnhancedMessage msg = null;

        try
        {
            // create a receipt message
            IaReceiptMessage receipt = new IaReceiptMessage();
            if (StringUtils.hasValue(enc))
                receipt.setEncoding(enc);

            // set the ISA segment
            receipt.setISASegment(isa);

            // set the timestamp
            receipt.setDateTimeStamp(new Date());

            // debugging info
            if (Debug.isLevelEnabled(Debug.MSG_DATA))
            {
                Debug.log(Debug.MSG_DATA, "ISA Segment: [" + isa + "]");
                Debug.log(Debug.MSG_DATA, "Date Time Stamp: ["
                          + receipt.getDateTimeStampString() + "]");
            }
            
 	    ByteArrayOutputStream tmpOs = new ByteArrayOutputStream();

	    receipt.encode(ASNData.DER_CODING, tmpOs);
	    byte[] derMsg = tmpOs.toByteArray();
        
	    msg = new EnhancedMessage( derMsg );
            	  
            
            // Set up the remote Host value
            initRemoteIp(cid);

            // open the socket
            if(Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "IAReceiver: Opening new socket to destination ip [" + destHostIp
			    		+ "] and port [" + destHostPort +"]");
    
            //use JSSE
	    iaSocket = IASocketConnectionPool.getInstance( destHostIp, destHostPort,numConnections ).acquireSocket();
	    iaSocket.setLastUsedTime();
	    socket = iaSocket.socket;

        //Start a monitor with Socket endpoint info
            tmti = ThreadMonitor.start( "IAReceiver: Sending EDI ReceiptIssue3 to TP. \n  Remote endpoint info : " +socket.toString()
                                + "\n  Local endpoint info :" + socket.getLocalSocketAddress());

            os = new SnoopOutputStream (new BufferedOutputStream(socket.getOutputStream() ));

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "IAReceiver: Got output stream, sending receipt");
            
	    // encode the message
            msg.encode(ASNData.DER_CODING, os);

	    //courtesy flush to keep things moving.
	    os.flush();

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "IAReceiver: Encoded receipt sent."); 
    
        }
 	catch (Exception e)
        {
	      
	     if ( iaSocket != null )
	     {
		     //mark the IASocket as invalid
		     iaSocket.invalidate();		    
	     }		     
	     try
	     {
		     //put back in the cache for cleanup
		     IASocketConnectionPool.getInstance( destHostIp, destHostPort).releaseSocket(iaSocket);
		     
		     Debug.log(Debug.IO_STATUS, "IAReceiver: Second attempt sending receipt ...");
		     
		     //get SSLSocket from cache
		     iaSocket = IASocketConnectionPool.getInstance( destHostIp, destHostPort,numConnections ).acquireSocket();
		     iaSocket.setLastUsedTime();
		     socket = iaSocket.socket;
            
		     os = new SnoopOutputStream (new BufferedOutputStream(socket.getOutputStream() ));
		    		     
		     // encode the message
            	     msg.encode(ASNData.DER_CODING, os);

	    	     //courtesy flush
	    	     os.flush();
	    
           if(Debug.isLevelEnabled(Debug.MSG_DATA))
               Debug.log(Debug.MSG_DATA, "IAReceiver: Second attempt, encoded receipt sent.");
	     }
	     catch (Exception e2)
	     {
		     Debug.log(Debug.IO_STATUS, "IAReceiver: Second attempt to send receipt failed.");
		     Debug.logStackTrace(e2);
		     
		     //mark the IASocket as invalid
		     iaSocket.invalidate();	     
		     
		     throw new ProcessingException(e2.toString());
	     }
        }       
        finally
        {
            // release socket
            if (socket != null)
            {
                try 
		{ 
		    IASocketConnectionPool.getInstance(destHostIp,destHostPort).releaseSocket(iaSocket);
		}
                catch (Exception ex)
                {
                    Debug.log(Debug.ALL_ERRORS,
                        "An error occured in IAReceiver trying to release the " +
                        "socket connection after sending a receipt: " +
                         ex.toString());
                }
            }
            ThreadMonitor.stop(tmti);
        }
    } 

    /**
     * Checks the remote IP with the client IPs in the HashTable
     * and return if matches.
     *
     * @param cid the customer's trading partner ID as retrieved from
     *            the EDI response. 
     * @throws ProcessingException
     */

    private void initRemoteIp(String cid) throws ProcessingException
    {
        if( !useDynamicClients )
        {
            destHostIp = remoteHost;
            destHostPort = remotePort;
            return;
        }

        DestinationInfo destination;

        if( clients.containsKey(cid) )
        {
            if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
               Debug.log(Debug.MSG_DATA,
                         "Attempting to retrieve IA receipt destination info from properties.");
            }

            destination = (DestinationInfo) clients.get( cid );
        }
        else
        {
            // try to lookup the customer ID in the database table
            if( Debug.isLevelEnabled(Debug.MSG_DATA) ){
               Debug.log(Debug.MSG_DATA,
                         "Attempting to retrieve IA receipt destination info from ["+
                         LOOKUP_TABLE+"] table.");
            }

            destination = lookupDestinationInfo( cid );
        }

        if(destination == null)
        {
            throw new ProcessingException (
               "No destination info found to reply to messages for customer trading partner ID [" +
               cid +
               "]. Check the configuration of IAServer and the contents of the ["+
               LOOKUP_TABLE+"]." );
        }

        destHostPort = destination.destPort;
        destHostIp = destination.destIP;

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
        {
            Debug.log(Debug.MSG_DATA, "Destination IP: [" + destHostIp + "]");
            Debug.log(Debug.MSG_DATA, "Destination Port: [" + destHostPort + "]");
        }

    }//initRemoteIp


    /**
    * This method checks the CUSTOMER_LOOKUP table to see if it contains
    * any IA Host and Port information for the given customer ID.
    * This method of looking up the host information is provided for
    * the convenience of deplaying the IA Server in a clearinghouse
    * environment. Instead of needing to add a new set a properties
    * (CUSTOMER_ID, DEST_IA_HOST, and DEST_IA_PORT_NUMBER) for
    * each new customer, instead one entry should be added to the table
    * per customer trading partner ID allowing this method to query for
    * the receipt destination address and port.
    *
    * @param cid the customer's trading partner ID as extracted from the
    *        EDI response.
    * @return the destination address and port where the response receipt
    *         should be sent. null will be returned if no info can be found for
    *         the given ID, or if any type of error occurs while
    *         executing the lookup query.
    */
    private static DestinationInfo lookupDestinationInfo(String cid)
    {
       DestinationInfo info = null;

       try{

          Connection conn = DBConnectionPool.getInstance().acquireConnection();

          try{

             PreparedStatement pstmt = conn.prepareStatement( DEST_INFO_QUERY );
             ResultSet results = null;

             try{

                pstmt.setString(1, cid);
                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                	Debug.log(Debug.NORMAL_STATUS, "IAReceiver: Executing SQL: \n"+DEST_INFO_QUERY );

                results = pstmt.executeQuery();
                
                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                	Debug.log(Debug.NORMAL_STATUS, "IAReceiver: Finished Executing SQL...");

                if( results.next() ){

                   String host = results.getString(1);
                   String port = results.getString(2);

                   if(host == null){
                      throw new Exception("IA receipt destination host is null.");
                   }

                   if(port == null){
                      throw new Exception("IA receipt destination port is null.");
                   }

                   int portNum = Integer.parseInt(port);
                   info = new DestinationInfo(host, portNum);

                }
                else{

                   Debug.error("No entry was found in the "+LOOKUP_TABLE+
                               " table for the customer trading partner ID ["+
                               cid+"]");

                }

             }
             finally{

                if(results != null){
                   results.close();
                }

                if(pstmt != null){
                   pstmt.close();
                }

             }

          }
          catch(Exception ex){

             // This can happen in the case of bad DB connectivity or
             // in the case where the CUSTOMER_LOOKUP table has not
             // been installed.
             Debug.error("An error occured while trying to execute the query ["+
                         DEST_INFO_QUERY+"] for trading partner ID ["+cid+"]: "+
                         ex);

          }
          finally{

             try{
                // return the connection to the pool
                DBConnectionPool.getInstance().releaseConnection(conn);
             }
             catch(Exception ex){
                Debug.error(ex.toString());
                Debug.logStackTrace(ex);
             }

          }

       }
       catch(Exception ex){

          Debug.error("Could not acquire database connection to perform IA destination info query: "+ex);

       }

       return info;

    }

   private static synchronized long addOneToCount()
   {
       return count++;
   }


  /*
   * This method places the input stream at the end of the stream.
   * This will cause the current read to end, which hopefully will be at
   * between IA messages.   This will break the run loop and cause the socket
   * to be closed. 
   */
   public void shutdown() throws IOException
   {
	  Debug.log( Debug.MSG_STATUS, 
		"Calling shutdown input on socket for IAReceiver: " + this );
	  
	  if ( socket != null ) socket.shutdownInput();
   }
    

    private static class DestinationInfo
    {
        public DestinationInfo(String destIP, int destPort)
        {
            this.destIP = destIP;
            this.destPort = destPort;
        }

        public String describe()
        {
            return new StringBuffer("Destination IP [").append(destIP).append(
                    "] Destination Port [").append(destPort).append("]").toString();
        }

        public String toString()
        {
           return describe();
        }

        private final String destIP;

        private final int destPort;

    }
}



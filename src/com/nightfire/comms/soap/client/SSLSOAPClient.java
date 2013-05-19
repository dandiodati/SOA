/*
 * SSLSOAPClient.java
 *
 */
package com.nightfire.comms.soap.client;

import org.apache.axis.utils.XMLUtils;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.soap.MessageFactoryImpl;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPHeader;


import javax.xml.rpc.JAXRPCException;
import javax.xml.rpc.soap.SOAPFaultException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.soap.*;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;

import com.nightfire.comms.soap.ssl.SSLUtils;
import com.nightfire.comms.soap.SOAPUtils;

import java.net.URL;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;


import org.w3c.dom.*;


public class SSLSOAPClient extends SOAPMessaging {

    /**
     * This property specify whether to use input message root as Header SOAPAction.
     *
     */
    public static final String USE_MSGROOT_AS_SOAPACTION_HEADER_PROP= "USE_MSGROOT_AS_SOAPACTION_HEADER";

    /**
     * The name of the ssl configuration xml file along with path
     *
     */
    public static final String SSL_CONFIG_FILE_PROP = "SSL_CONFIG_FILE";


    protected String sslConfigFile;
    private Boolean isSoapActionHeaderAsRoot = false;
    private static final String SOAP_ACTION_HEADER = "SOAPAction";
    private List<String> reportFaultsAsExceptions = new ArrayList<String>();


    /**
     * Loads the SSL property's values into memory
     * @param  key   Property Key to use for locating initialization properties.
     * @param  type  Property Type to use for locating initialization properties.
     * @throws ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException {
        super.initialize(key, type);

        sslConfigFile = getRequiredPropertyValue(SSL_CONFIG_FILE_PROP);

        String useHeaderSoapActionAsRootVal = getPropertyValue(USE_MSGROOT_AS_SOAPACTION_HEADER_PROP);

        StringBuffer errorBuffer = new StringBuffer( );
        if ( StringUtils.hasValue( useHeaderSoapActionAsRootVal ) )
        {
            try {
                isSoapActionHeaderAsRoot = getBoolean( useHeaderSoapActionAsRootVal );
            }
            catch ( FrameworkException e )
            {
                errorBuffer.append ( "Property value for " + USE_MSGROOT_AS_SOAPACTION_HEADER_PROP +
                  " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        SSLUtils.initializeOnce(sslConfigFile);

        // Following Fault messages would be treated as Exceptions
        reportFaultsAsExceptions.add(SOAPUtils.SOAP_FAULT_STR_SOCKET_EXCEPTION);
        reportFaultsAsExceptions.add(SOAPUtils.SOAP_FAULT_STR_HANDSHAKE_EXCEPTION);
        reportFaultsAsExceptions.add(SOAPUtils.SOAP_FAULT_STR_CONNECTION_EXCEPTION);

        // Following are the additional Faults to be treated as Exception.  These are available in the file.
        List soapFaultsFromFile = SOAPUtils.loadSOAPFaultsToConvert();
        if (soapFaultsFromFile != null)
            reportFaultsAsExceptions.addAll(soapFaultsFromFile);
    }

    /*
     * Generate SOAP request and send it to the SOAP Server, finally set the response at the context
     *
     * @param  context The context
     *
     * @param  msgObj
     *
     * @return  NVPair[] Name value Pair or null
     *
     * @exception  ProcessingException  Thrown if processing fails due bad connection.
     *
     * @exception  MessageException  Thrown if bad message
     */
    public NVPair[] process( MessageProcessorContext context, MessageObject msgObj )
    throws MessageException, ProcessingException {

         if(msgObj == null)
            return null;

        super.process(context, msgObj);

        Document responseDoc;
        String responseStr;
        SOAPMessage msg;

        try {
            log(Debug.NORMAL_STATUS, "SSLSOAPClient : Request :" + getElementAsString(envelope));

            msg = sendSOAPMessage(getPreparedSOAPMessage(envelope), targetUrl);

            // get and set the SOAP header element values in context from the received SOAP Message
            if (headerProperties.size()>0)
                getAndSetHeaderElements(msg,context);

            // get the SOAP Body as string from the received SOAP Message
            responseStr = getSOAPBodyAsString(msg.getSOAPPart().getEnvelope().getBody());
            byte respBytes[] = responseStr.getBytes();
            responseDoc = XMLUtils.newDocument(new ByteArrayInputStream(respBytes));

            XMLMessageParser resParser = new XMLMessageParser(responseDoc);
            log(Debug.NORMAL_STATUS, "SSLSOAPClient : Response: " + resParser.getGenerator().generate());

            //set the SOAP response at context
            set(soapResponseLoc,context, msgObj, responseDoc);

        } catch (SOAPFaultException e) {
            Debug.error("SSLSOAPClient" + ":" + "SOAP Fault Error : " + e.getFaultString() );
            Iterator iter = e.getDetail().getDetailEntries();
            while (iter.hasNext() ) {
                Debug.error("SSLSOAPClient" + ":" + iter.next().toString() );
            }
            throw new MessageException("SOAP Fault Error: " + e.getFaultString() );
        } catch (JAXRPCException e2 ) {
            Debug.error("SSLSOAPClient" + ":" + "SOAP RPC Error : " + e2.getMessage());
            Throwable detail = e2.getLinkedCause();
            // if the linked exception is null just point to the top level one.
            if ( detail == null )
                detail = e2;
            Debug.error("SSLSOAPClient" + ":" + detail.getMessage() );
            Debug.logStackTrace(detail);
            throw new ProcessingException("SOAP RPC Error: " + e2.getMessage() );
        } catch(SOAPException se) {
            Debug.logStackTrace(se);
            Debug.error("SSLSOAPClient" + ":" + "SOAP Request failed: soapexception" + se.getMessage() );
            throw new ProcessingException("SOAP Request Failed : " + se.getMessage());
        } catch(IOException io) {
            Debug.error("SSLSOAPClient" + ":" + "IOException occured : " + io.getMessage() );
            Debug.error("SSLSOAPClient" + ":" + Debug.getStackTrace(io));
            throw new ProcessingException(io.getMessage());
        } catch(Exception e4) {
            Debug.logStackTrace(e4);
            Debug.error("SSLSOAPClient" +":" + "SOAP Request failed: " + e4.getMessage() );
            throw new ProcessingException("SOAP Request Failed : " + e4.getMessage());
        }

        return formatNVPair(msgObj);
    }




    /**
     * Prepare and return the SOAPMessage to be send at the SOAP Server
     * @param envelope SOAPEnvelope
     * @return SOAPMessage
     * @throws javax.xml.soap.SOAPException
     * @throws java.io.IOException
     */
    public SOAPMessage getPreparedSOAPMessage(SOAPEnvelope envelope) throws Exception {

        String soapReq;

        // Return the signed SOAP XML as a String
        soapReq = XMLUtils.ElementToString(envelope.getAsDOM());

        MessageFactoryImpl messageFactory = (MessageFactoryImpl) org.apache.axis.soap.MessageFactoryImpl.newInstance();
        SOAPMessage message = messageFactory.createMessage();
        //Create objects for the message parts
        javax.xml.soap.SOAPPart soapPart = message.getSOAPPart();

        //Populate the Message
        byte domByte[] = soapReq.getBytes();
        StreamSource preparedMsgSrc = new StreamSource(new ByteArrayInputStream(domByte));
        soapPart.setContent(preparedMsgSrc);

        //log the message
        if(Debug.isLevelEnabled(Debug.XML_DATA))
        {
            ByteArrayOutputStream messageAsString = new ByteArrayOutputStream();
            message.writeTo(messageAsString);
            String soapMessage = new String(messageAsString.toByteArray());
            SOAPMessaging.log(Debug.XML_DATA, "SSLSOAPClient : SOAPMessage to be sent: \n" + soapMessage);
        }
        return message;
    }


    /**
     * Send the SOAP message to the destination SOAP server
     * @param message SOAPMessage
     * @param target The URL of the destination SOAP server
     * @throws javax.xml.soap.SOAPException
     * @return response received by the SOAP server as SOAP Message
     */
    public SOAPMessage sendSOAPMessage(SOAPMessage message, URL target) throws ProcessingException, SOAPException {
        SOAPMessage response;
        SOAPConnection connection = null;

        SOAPMessaging.log(Debug.NORMAL_STATUS,"SSLSOAPClient : Creating the connection ");

        try
        {
            //Get the soap Factory instance
            SOAPConnectionFactory soapConnFactory = org.apache.axis.soap.SOAPConnectionFactoryImpl.newInstance();
            //Create the connection
            connection = soapConnFactory.createConnection();

            //Send the SOAP message     to the specified target url
            SOAPMessaging.log(Debug.NORMAL_STATUS,"SSLSOAPClient : Sending SOAP message ");

            //Adding SOAPAction for calling proper action in webservice.
            if(isSoapActionHeaderAsRoot){
                MimeHeaders headers = message.getMimeHeaders();
                String rootNodeName = message.getSOAPBody().getFirstChild().getLocalName();
                headers.addHeader(SOAP_ACTION_HEADER, rootNodeName);
                log(Debug.NORMAL_STATUS, "SSLSOAPClient: SOAPAction header value set as ["+ rootNodeName+"]");
            }

            response = connection.call(message, target);

            // Check whether it is a Fault Response, and if the fault needs to be reported as an Exception (as was in Axis 1.1)
            SOAPBody  msgBody = response.getSOAPBody();
            if (msgBody.hasFault())  // The response is a SOAP Fault
            {
                // Fault String in the response fault message
                String msgFaultStr = msgBody.getFault().getFaultString();

                // If fault needs to be handled as Exception, throw Exception
                if (transformFaultAsException(msgFaultStr))
                {
                    SOAPMessaging.log(Debug.NORMAL_STATUS,"SSLSOAPClient : Fault Message [" + getSOAPBodyAsString(response.getSOAPPart().getEnvelope().getBody()) + "]");
                    SOAPMessaging.log(Debug.NORMAL_STATUS,"SSLSOAPClient : Converting SOAP-Fault to SOAP-Exception [" + msgFaultStr + "]");
                    throw ( new SOAPException (msgFaultStr));
                }
            }
        }
        finally
        {
            if(connection!=null)
                //Close the connection
                connection.close();
        }
        return response;
        }

    /**
     * Check against a FaultList and determine whether the fault message
     * needs to be treated as an Exception
     * @param msgFaultStr the Fault String from the response message
     * @return boolean whether the message fault String is available in the master list 
     */

    public boolean transformFaultAsException (String msgFaultStr)
    {
        for (String faultStr: reportFaultsAsExceptions)
        {
            SOAPMessaging.log(Debug.NORMAL_STATUS,"SSLSOAPClient : transformFaultAsException(): comparing Message Fault String [" + msgFaultStr + "] against predefined Fault String [" + faultStr  + "]");

            if (msgFaultStr.contains(faultStr))
            {
                return true;
            }
        }

        return false;
    }

/**
     * Get and set the soap header nodes in the context
     * @param msg SOAPMessage
     * @param ctx MessageProcessorContext
     * @throws javax.xml.soap.SOAPException ProcessingException  MessageException
     */
    public void getAndSetHeaderElements(SOAPMessage msg,MessageProcessorContext ctx)
            throws SOAPException, ProcessingException, MessageException
    {
        SOAPMessaging.log(Debug.NORMAL_STATUS,"SSLSOAPClient : setting values from soap header in context");
        // get SOAP header from the SOAP Message
        SOAPHeader header = (SOAPHeader) msg.getSOAPPart().getEnvelope().getHeader();

        Iterator iterator  = header.getChildElements();
        MessageElement headElement = null;

        String node =null;
        String value =null;

        // read all the nodes present in header and set the node-value pair in hashtable.
        while (iterator.hasNext())
        {
              headElement = (org.apache.axis.message.MessageElement) iterator.next();
              node = headElement.getName();
              value = headElement.getValue();
              if (headerProperties.contains(node))
                       ctx.set(node,value);
        }
    }

    // For Unit Testing
    public static void main (String args[])
    {
        try
        {
            Debug.enableAll();
            SSLSOAPClient ssc = new SSLSOAPClient();

            /*
            reportFaultsAsExceptions.add("java.net.SocketException");
            reportFaultsAsExceptions.add("java.net.ConnectException");
            reportFaultsAsExceptions.add("javax.net.ssl.SSLHandshakeException");


            List soapFaultsFromFile = SOAPUtils.loadSOAPFaultsToConvert();
            if (soapFaultsFromFile != null)
                reportFaultsAsExceptions.addAll(soapFaultsFromFile);

            Debug.log(Debug.NORMAL_STATUS, "Dev: available exceptions")  ;
            if (reportFaultsAsExceptions.size() != 0)
            {
                for (String faultObject : reportFaultsAsExceptions)
                {
                    SOAPMessaging.log(Debug.NORMAL_STATUS,"[" + faultObject + "]");
                }
            }
            */


            SOAPMessage msg = ssc.sendSOAPMessage(ssc.getPreparedSOAPMessage(new SOAPEnvelope()), new URL("http://localhost/axis") );

            // get the SOAP Body as string from the received SOAP Message
            String responseStr = ssc.getSOAPBodyAsString(msg.getSOAPPart().getEnvelope().getBody());
            Debug.log (Debug.NORMAL_STATUS, "Response : " + responseStr);

        }
        catch(Exception e)
        {
            e.printStackTrace();
            Debug.log(Debug.NORMAL_STATUS, "Dev exc: " + e.getMessage());
        }
    }

}// End of Class

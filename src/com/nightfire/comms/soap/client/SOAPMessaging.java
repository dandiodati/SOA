/*
 * SOAPMessaging.java
 *
 */

package com.nightfire.comms.soap.client;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.db.PersistentProperty;


import javax.xml.soap.SOAPBody;


import org.apache.axis.utils.XMLUtils;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPBodyElement;



import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import java.net.URL;
import java.net.MalformedURLException;


public abstract class SOAPMessaging extends MessageProcessorBase {

    /**
     * The location in the context/message obj to obtain the request
     *
     */
    public static final String CONTEXT_REQUEST_LOC_PROP  = "SOAP_REQUEST_LOC";

    /**
     * The remote SOAP service address
     *
     */
    public static final String END_POINT_URL_PROP  = "END_POINT_URL";

    /**
     * The remote SOAP service address locaiton in context/message obj
     *
     */
    public static final String END_POINT_URL_LOC_PROP  = "END_POINT_URL_LOC";

    /**
     * The location in the context/message obj to place the response.
     *
     */
    public static final String SOAP_RESPONSE_LOC_PROP = "SOAP_RESPONSE_LOC";

    /**
        * The name of the soap operation which needs to be accessed
        *
        */
    public static final String SOAP_ACTION_PROP= "SOAP_ACTION";

    /**
        * The name of the soap operation which needs to be accessed
        *
        */
    public static final String HEADER_ELEMENT_PROP= "HEADER_ELEMENT";



    //Instance Variable to hold common property value
    protected String endPointUrl;
    protected String ctxEndPointUrlLoc;
    protected String soapResponseLoc;
    protected String ctxSoapRequestLoc;
    protected URL targetUrl;
    protected String soapAction;
    protected List headerProperties;

    protected SOAPEnvelope envelope;

    private static final HashMap props = new HashMap();

    protected StringBuffer errBuf;

    static
    {
            props.put(XMLLibraryPortabilityLayer.NAMESPACE_AWARE_FLAG, "true");
    }

    public SOAPMessaging()
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating SOAPMessaging message-processor." );
        headerProperties= new LinkedList( );
        envelope = new SOAPEnvelope();
        errBuf = new StringBuffer();

    }

    /**
     * Loads the property's values into memory which are common to both Digital Signature and Basic Authentication security implementation
     * @param  key   Property Key to use for locating initialization properties.
     * @param  type  Property Type to use for locating initialization properties.
     * @throws ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException {
        super.initialize(key, type);

        //set the context location of the request that has originated
        ctxSoapRequestLoc = getRequiredPropertyValue(CONTEXT_REQUEST_LOC_PROP,errBuf);

        //set the target URL
        endPointUrl = getPropertyValue(END_POINT_URL_PROP);

        //set the context location for the target URL
        ctxEndPointUrlLoc = getPropertyValue(END_POINT_URL_LOC_PROP);

        //set the SOAP Response Location
        soapResponseLoc = getRequiredPropertyValue(SOAP_RESPONSE_LOC_PROP, errBuf);

        //set the SOAP method name
        soapAction = getPropertyValue(SOAP_ACTION_PROP);

        // Loop until all header element configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String headerElement = getPropertyValue( PersistentProperty.getPropNameIteration( HEADER_ELEMENT_PROP, Ix ) );

            if (!StringUtils.hasValue(headerElement))
                break;

            headerProperties.add(headerElement);
        }
        if (!StringUtils.hasValue(endPointUrl) && !StringUtils.hasValue(ctxEndPointUrlLoc))
       {
           errBuf.append("ERROR: At least one of endpoint-url or endpoint-url-location must be configured.");
       }
       if(errBuf.length() > 0)
       {
           Debug.error( errBuf.toString());
           throw new ProcessingException( errBuf.toString());
       }

    }


    /**
     * Create SOAP Body and add it to SOAP Envelope
     * @param context The context
     * @param msgObj
     * @throws com.nightfire.framework.message.MessageException if bad message
     * @throws com.nightfire.common.ProcessingException if processing fails due bad connection.
     * @return NVPair[] Name value Pair or null
     */
    public NVPair[] process( MessageProcessorContext context, MessageObject msgObj )
    throws MessageException, ProcessingException {

        String xmlToSend;

        //get the xml to be send
        xmlToSend = getString(ctxSoapRequestLoc, context, msgObj);

        SOAPMessaging.log(Debug.XML_STATUS, "SOAPMessaging" + ":" + "xmlToSend:" + xmlToSend);

        // add method name to input xml if SOAP action element is configured
        if ( StringUtils.hasValue(soapAction))
        {
            xmlToSend = addSoapAction(xmlToSend);
        }

       // First use the targetUrl from the configured context location; if not available use one configured in the END_POINT_URL property.
        try
        {
            if(StringUtils.hasValue(ctxEndPointUrlLoc))
                targetUrl = new URL(getString(ctxEndPointUrlLoc, context, msgObj));
            else
                targetUrl = new URL(endPointUrl);
        }
        catch(MalformedURLException e) {
            Debug.error("SOAPMessaging" + ": Cannot parse Endpoint URL - Please check the configured SOAP URL" + e.getMessage());
            throw new ProcessingException(e);
        }

        SOAPMessaging.log(Debug.NORMAL_STATUS, "SOAPMessaging" + ":" + "URL of destination SOAP server:" + targetUrl);

        // need to reparse the xml with namespace support
        XMLMessageParser parser = new XMLMessageParser();
        parser.configure(props);
        // Parse the XML Request submitted byte user
        parser.parse(xmlToSend);
        Element bodyElement = parser.getDocument().getDocumentElement();

        addBodyElement(bodyElement);
        
        return null;
    }

 /**
     * Add the SOAP method name to the SOAP Envelope
     * @param xmlToSend
     * @throws com.nightfire.framework.message.MessageException if bad message
     * @return newXml
     */

    private String addSoapAction(String xmlToSend) throws MessageException
    {
       String newXml=xmlToSend;
       if ( StringUtils.hasValue(soapAction))
       {
            // remove the first <?xml ...?> tag from request xml
            int idx=xmlToSend.indexOf(">");
            String newXmlTemp = xmlToSend.substring(idx+1);
            newXml = "<"+soapAction+">"+newXmlTemp+"</"+soapAction+">";
            XMLPlainGenerator xpg1 = new XMLPlainGenerator(newXml);
            newXml = xpg1.describe();
            SOAPMessaging.log(Debug.XML_STATUS, "SOAPMessaging : Returning xml after adding SOAP Action: " + newXml);
        }
        return newXml;
    }

     /**
     * Add SOAPBodyElement in the SOAPEnvelope
     * @param element Element
     * @throws com.nightfire.common.ProcessingException
     */
    public void addBodyElement(Element element) throws ProcessingException{
        try {
            envelope.addBodyElement(new SOAPBodyElement(element));
        } catch (Exception e) {
            Debug.error("SOAPMessaging : Can't add body element to envelop: " + e.getMessage());
            throw new ProcessingException(e);
        }
    }

    /**
     * Return SOAPBody as String
     * @param body SOAPBody
     * @return SOAPBody as String
     * @throws com.nightfire.common.ProcessingException
     */
    public String getSOAPBodyAsString(SOAPBody body) throws ProcessingException {
        if(body != null)
        {
            try {
                Iterator iterator  = body.getChildElements();

                MessageElement bodyElement = null;

                while (iterator.hasNext()) {
                    Object element = iterator.next();
                    bodyElement = (org.apache.axis.message.MessageElement)element;
                }

                return bodyElement.toString();
            } catch(Exception e) {
                Debug.error("SOAPMessaging : Can't convert SOAP body to String : " + e.getMessage() );
                throw new ProcessingException(e);
            }
        }
        return null;
    }

    /**
     * Returns SOAPEnvelope as String
     * @param envelope SOAPEnvelope
     * @return SOAPEnvelope as String
     * @throws com.nightfire.common.ProcessingException in case the conversion fails.
     */
    public String getElementAsString(SOAPEnvelope envelope) throws ProcessingException {
        try {
            return XMLUtils.ElementToString(envelope.getAsDOM());

        } catch (Exception e) {
            Debug.error("SOAPMessaging : Can't convert Envelope to String : " + e.getMessage() );
            throw new ProcessingException("Can't convert Envelope to String : " + e.getMessage());
        }
    }



    /**
     * Log the specified message if the log level is enabled
     * @param status log level
     * @param msg message to be logged
     */
    public static void log(int status, String msg) {
        if ( Debug.isLevelEnabled(status) )
            Debug.log(status, msg);
    }
}

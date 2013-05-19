/*
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.soap;

import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.rpc.soap.*;
import javax.xml.rpc.JAXRPCException;
import java.rmi.RemoteException;


import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;

import com.nightfire.framework.message.common.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;

import javax.xml.namespace.QName;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import org.apache.axis.message.*;


/*
 * This is a simple soap messaging client which can send a SOAP message xml
 * to a specified remote host.
 *
 */

public class SOAPMessagingClient extends MessageProcessorBase
{
      
  
    /**
     * A file path with an xml request to send.
     *
     */
    public static final String REQUEST  = "SOAP_REQUEST_XML";
    
    
    /**
     * The location in the context/message obj to obtain the request
     *
     */
    public static final String REQUEST_LOC  = "SOAP_REQUEST_LOC";

    /**
     * The location in the context/message obj to place the response.
     *
     */
    public static final String RESPONSE_LOC = "SOAP_RESPONSE_LOC";
    

    /**
     * The remote SOAP service address
     *
     */
    public static final String END_POINT_URL  = "END_POINT_URL";

    /**
     * The remote SOAP service address locaiton in context/message obj
     *
     */
    public static final String END_POINT_URL_LOC  = "END_POINT_URL_LOC";

    private String respLoc;
    private String reqLoc;
    private String request;
    private URL targetUrl;
    private String targetUrlLoc;
    
    private static final FileCache reqCache = new FileCache();
    
    
   private static final HashMap props = new HashMap();
       
    static 
    {
        props.put(XMLLibraryPortabilityLayer.NAMESPACE_AWARE_FLAG, "true");
    }
    
        
    /**
     *
     * @param Key  to load properties from Persistent Properties
     *
     * @param Type to load Properties from Persistent Properties
     *
     * @exception ProcessingException Thrown if initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
      
     
        super.initialize(key,type);

        StringBuffer errBuf = new StringBuffer();

        respLoc = getRequiredPropertyValue(RESPONSE_LOC,errBuf);
        
        String targetUrlStr = getPropertyValue(END_POINT_URL);
        
        try {
            if ( StringUtils.hasValue(targetUrlStr) )
                targetUrl = new URL(targetUrlStr);         
        }
        catch (MalformedURLException e) {
            errBuf.append(END_POINT_URL + " property contains an invalid URL : " + e.getMessage() );
        }
        
        targetUrlLoc = getPropertyValue(END_POINT_URL_LOC);

        request = getPropertyValue (REQUEST);
        
        reqLoc = getPropertyValue(REQUEST_LOC);
        
        if (!StringUtils.hasValue(request) &&
            !StringUtils.hasValue(reqLoc)) {
            errBuf.append("At least one of the following properties are required : " + REQUEST + " or " + REQUEST_LOC);
            
        }

       if (targetUrl == null &&
            !StringUtils.hasValue(targetUrlLoc)) {
            errBuf.append("At least one of the following properties are required : " + END_POINT_URL + " or " + END_POINT_URL_LOC);
            
        }
        
       
          // if there are any missing required properties throw an exception with the missing props.
        if (errBuf.length() > 0 )
           throw new ProcessingException(errBuf.toString());

    }

  
    
    /*
     * Ftps a file to a remote server.
     *
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  NVPair[] Name value Pair or null 
     *
     * @exception  ProcessingException  Thrown if processing fails due bad connection.
     *
     * @exception  MessageException  Thrown if bad message
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
     {
         
         if (msgObj == null) 
             return null;


         URL target = null;
         String xmlToSend = null;
         
         if ( StringUtils.hasValue(reqLoc) )
             xmlToSend = getString(reqLoc,context, msgObj);
         else {
             try {
                 xmlToSend = reqCache.get(request);
             }
             catch (FrameworkException e ) {
                 throw new ProcessingException(e);
             }
         }

         if ( StringUtils.hasValue(targetUrlLoc) ) {
             
             String targetStr = getString(targetUrlLoc,context, msgObj);
             try {
                 target = new URL(targetStr);
             }
             catch (MalformedURLException e) {
                 String err =END_POINT_URL_LOC + " property contains an invalid URL : " + e.getMessage();
                 Debug.error(err);
                 throw new ProcessingException(err);
             }
         }
         
       
         else {
             target = targetUrl;
         }
         
         
         Call call = null;
         

         Service  service = new Service();
         try {
             call    = (Call) service.createCall();
         }
         catch (Exception e) {
             Debug.error("SOAPMessagingClient : Failed to create soap call object: " + e.getMessage() );
             throw new ProcessingException(e);
             
         }
         
        
        call.setTargetEndpointAddress( target );
        SOAPBodyElement[] input = new SOAPBodyElement[1];

        
        // need to reparse the xml with namespace support                
        XMLMessageParser parser = new XMLMessageParser();
        parser.configure(props);
        parser.parse(xmlToSend);

        Element el = parser.getDocument().getDocumentElement();
	    
        input[0] = new SOAPBodyElement(el);
        
        
        if ( Debug.isLevelEnabled(Debug.XML_STATUS) ) {
            Debug.log(Debug.XML_STATUS, "SOAPMessagingClient: Trying to send SOAP body xml : " + parser.getGenerator().generate() );
        }
        
        Document responseDoc = null;
        
        try {
            
           Vector          elems = (Vector) call.invoke( input );
           SOAPBodyElement elem  = null ;
         
           elem = (SOAPBodyElement) elems.get(0);
           responseDoc    = elem.getAsDocument();
        }
        catch ( SOAPFaultException e) {
            Debug.error("SOAP Fault Error : " + e.getFaultString() );
            Iterator iter = e.getDetail().getDetailEntries();
            while (iter.hasNext() )
            {
                Debug.error(iter.next().toString() );
            }
                   
            throw new MessageException("SOAP Fault Error: " + e.getFaultString() );
        }
        catch (JAXRPCException e2 ) {
            Debug.error("SOAP RPC Error : " + e2.getMessage());
            Throwable detail = e2.getLinkedCause();
            // if the linked exception is null just point to the top level one.
            if ( detail == null )
                detail = e2;
            Debug.error(detail.getMessage() );
            Debug.logStackTrace(detail);   
            throw new ProcessingException("SOAP RPC Error: " + e2.getMessage() );
        }
       catch (RemoteException e3 ) {
            Debug.error("RMI Error : " + e3.getMessage());
            Throwable detail = e3.detail;
            // detail may be null.
            if ( detail == null)
                detail = e3;
            Debug.error(detail.getMessage() );
            Debug.logStackTrace(detail );    
            throw new ProcessingException("RMI Error" + e3.getMessage() );
       } catch (Exception e4) {
           Debug.error("SOAP Request failed: " + e4.getMessage() );
           throw new ProcessingException("SOAP Request Failed : " + e4.getMessage());
       }
        
        
        
        if ( responseDoc != null && Debug.isLevelEnabled(Debug.XML_STATUS)) {
             XMLMessageParser resParser = new XMLMessageParser(responseDoc);
             Debug.log(Debug.XML_STATUS,"SOAPMessagingClient: Got response: " + resParser.getGenerator().generate());
        }
        

        set(respLoc,context, msgObj, responseDoc);
             
            
        return formatNVPair(msgObj);
    }
   
}

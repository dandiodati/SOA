/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: $
 */
package com.nightfire.comms.soap;


import java.util.Properties;
import java.util.StringTokenizer;

import com.nightfire.framework.util.*;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;

import com.nightfire.comms.soap.ssl.*;


/**
 * Test code for testing the SOAPRequestHandler SOAP service.
 */
public class SOAPRequestHandlerStubClient
{

  	public static final String PROCESS_ASYNC   = "processAsync";
    public static final String PROCESS_SYNC    = "processSync";
    public static final String DEBUG_PROP      = "debug";
    public static final String METHOD_NAME_PROP  = "method";
    public static final String HEADER_PROP      = "header";
    public static final String BODY_PROP      = "message";
    public static final String SOAP_SERVICE_URL_PROP = "soapServiceURL";

    /**
     * Constructor
     */
    public SOAPRequestHandlerStubClient(String soapServiceURL)
    {
        if ( StringUtils.hasValue ( soapServiceURL ) )
            soapRequestHandlerURL	= soapServiceURL;
    }

    /**
     * Command-line interface
     */
    public static void main (String args[]) throws Exception
    {

        if ( args.length != 1 )
        {
            System.out.println("Usage: java -Dnfi.home=<nfi_home> com.nightfire.comms.soap.SOAPRequestHandlerStubClient <property file>");
            System.exit(-1);
        }

        Debug.configureFromProperties();

        //used for all actions
        String method = null;
        String header = null;
        String message = null;
        String url = null;

        Properties soapProps = new Properties();

        try
        {
            FileUtils.loadProperties( soapProps, args[0] );
            SOAPRequestHandlerStubClient.enableDebug(soapProps.getProperty( DEBUG_PROP ));
            method = soapProps.getProperty( METHOD_NAME_PROP );
            url = soapProps.getProperty( SOAP_SERVICE_URL_PROP );
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        try
        {
            header = FileUtils.readFile( soapProps.getProperty( HEADER_PROP ) );
            message = FileUtils.readFile( soapProps.getProperty( BODY_PROP ) );

            SOAPRequestHandlerStubClient stub = new SOAPRequestHandlerStubClient( url );
            if ( method.equalsIgnoreCase(PROCESS_ASYNC) )
            {
                stub.processAsync(header, message);
            }
            else if ( method.equalsIgnoreCase(PROCESS_SYNC) )
            {
                stub.processSync(header, message);
            }
            else
            {
                throw new Exception("ERROR: Method [" + method + "] not supported by SOAPRequestHandler interface.");
            }

            String firstHeader = header;

            // If additional body files are given (as indicated by numeric suffix), send them as well.
            for ( int Ix = 0;  true;  Ix ++ )
            {
                String bodyFileName = soapProps.getProperty( BODY_PROP + "." + Ix );

                if ( bodyFileName == null )
                    break;

                message = FileUtils.readFile( bodyFileName );

                String headerFileName = soapProps.getProperty( HEADER_PROP + "." + Ix );

                if ( headerFileName != null )
                    header = FileUtils.readFile( headerFileName );
                else
                    header = firstHeader;

                if ( method.equalsIgnoreCase(PROCESS_ASYNC) )
                {
                    stub.processAsync(header, message);
                }
                else if ( method.equalsIgnoreCase(PROCESS_SYNC) )
                {
                    stub.processSync(header, message);
                }
                else
                {
                    throw new Exception("ERROR: Method [" + method + "] not supported by SOAPRequestHandler interface.");
                }
            }
        }

        catch (Exception e)
        {
            System.exit(-1);
        }
    }

    static public void enableDebug(String levels) throws Exception
    {
        if ( levels == null )
            return;

        if ( levels.equalsIgnoreCase("all") )
        {
            Debug.enableAll();
            return;
        }

        StringTokenizer st = new StringTokenizer(levels, " ");

        while ( st.hasMoreTokens() )
        {
            Debug.enable( Integer.parseInt(st.nextToken() ) );
        }
    }

    /**
     * Method to process sync request
     *
     * @param header Message header.
     * @param body Message body.
     *
     * @return String[] containing the response header and response body.
     *
     * @exception Exception Thrown if request data is bad.
     */
    public String[] processSync (String header,String body) throws Exception
    {
        try
        {
            SSLUtils.initializeOnce();

            SOAPRequestHandlerServiceLocator serviceLocator = new SOAPRequestHandlerServiceLocator();

            SOAPRequestHandlerSoapBindingStub stub = (SOAPRequestHandlerSoapBindingStub) serviceLocator.getSOAPRequestHandler(new java.net.URL(soapRequestHandlerURL));

			if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE) )
                Debug.log(Debug.OBJECT_LIFECYCLE, "processSync called...");

            if ( Debug.isLevelEnabled( Debug.XML_DATA) )
                Debug.log(Debug.XML_DATA, "contacting server with header ["+header+"]\n" + "and message ["+body+"]\n");

            String[] xmlmess = stub.processSync(header, body);
            if ( Debug.isLevelEnabled( Debug.XML_DATA) )
                Debug.log(Debug.XML_DATA,"processSync returned: header=" + xmlmess[0] + "body =" + xmlmess[1] );
            
            System.err.println("processSync returned: header=" + xmlmess[0] + "\n\nbody =" + xmlmess[1] );
            return xmlmess;

				 
        }
        catch (java.net.MalformedURLException e)
        {
            e.printStackTrace();        
		      	throw new Exception("Invalid SOAP Service URL: " + e );
        }
        catch (java.rmi.RemoteException e)
        {
            e.printStackTrace();
    			  throw new Exception("SOAP service failed: " + e );
        }
	}

    /**
     * Method to process async request
     *
     * @param header Message header.
     * @param body Message body.
     *
     * @return String[] containing the response header and response body.
     *
     * @exception Exception Thrown if request data is bad.
     */
    public void processAsync (String header,String body) throws Exception
    {
        try
        {
            SSLUtils.initializeOnce();

            SOAPRequestHandlerServiceLocator serviceLocator = new SOAPRequestHandlerServiceLocator();

            SOAPRequestHandlerSoapBindingStub stub = (SOAPRequestHandlerSoapBindingStub) serviceLocator.getSOAPRequestHandler(new java.net.URL(soapRequestHandlerURL));

            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE) )
                Debug.log(Debug.OBJECT_LIFECYCLE, "processAsync called...");

            if ( Debug.isLevelEnabled( Debug.XML_DATA) )
                Debug.log(Debug.XML_DATA, "contacting server with header ["+header+"]\n" + "and message ["+body+"]\n");
            stub.processAsync(header, body);
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE) )
                Debug.log(Debug.OBJECT_LIFECYCLE,"processAsyc returned: success.");
        }
        catch (java.net.MalformedURLException e)
        {
            e.printStackTrace();
			      throw new Exception("ERROR: Invalid SOAP Service URL: " + e );
        }
        catch (java.rmi.RemoteException e)
        {
            e.printStackTrace();
            throw new Exception("ERROR: SOAP service failed: " + e );
        }
	  }

    /**
     * Default url.
     */
	  private String soapRequestHandlerURL = "http://localhost:8080/axis/services/SOAPRequestHandler";
}

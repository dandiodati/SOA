/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: $
 */
package com.nightfire.spi.neustar_soa.portps;
import com.nightfire.framework.util.Debug;


/**
 * Test code for testing the SOAPRequestHandler SOAP service.
 */
public class PORTPSServiceStubClient
{
	
    public static final String GET_HISTORY = "NO";    
	private String PortPSServiceURL = null;
	private String username = null;
	private String password = null;
	private String tn = null;
    
    /**
     * Initialization
     */
    
    public void initialize(String username,String password,String tn,String url)
    {
    	this.username = username;
    	this.password =password;
    	this.tn =tn;
    	this.PortPSServiceURL =url;
    }


    /**
     * Method to process sync request
     *        
     * @return PortingResponseType
     *
     * @exception Exception Thrown if request data is bad.
     */
    public PortingResponseType processSync () throws Exception
    {
    	PortingResponseType portingResType =null;
        try
        {
            PortpsServiceLocator serviceLocator = new PortpsServiceLocator();

            PortpsServiceBindingStub stub = (PortpsServiceBindingStub) serviceLocator.getPortpsServiceBinding(new java.net.URL(PortPSServiceURL));                      
                      			 
            java.lang.String[] TNList = new String[1];
            TNList[0] = tn;
            
            PortingRequestType ptype = new PortingRequestType(username,password,GET_HISTORY,TNList);                             	
            portingResType = stub.getPortingInformation(ptype);            
            
          }
        catch (java.net.MalformedURLException e)
        {
        	throw new Exception("Invalid SOAP Service URL: " + e.getMessage() );
        }
        catch (java.rmi.RemoteException e)
        {    	
        	throw new Exception("SOAP service failed: " + e.getMessage() );
        }
        catch (Exception e)
        {    	
        	throw new Exception("PortPS service failed: " + e.getMessage() );
        }
        return portingResType;
	}
    

    
	
	  
	  
}

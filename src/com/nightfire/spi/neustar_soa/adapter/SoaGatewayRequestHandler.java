/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 */
package com.nightfire.spi.neustar_soa.adapter;

import java.rmi.*;
import java.util.*;


import org.omg.CORBA.*;

import com.nightfire.framework.corba.*;
import com.nightfire.framework.debug.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.security.tpr.*;
import com.nightfire.spi.common.*;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.webgui.core.DataHolder;
import com.nightfire.webgui.core.ExternalNotifier;
import com.nightfire.framework.util.SeqIdGenerator;
import com.nightfire.comms.soap.*;


/**
 * Proxy to route requests to gateway communication servers.
 */
public class SoaGatewayRequestHandler implements SOAPRequestHandlerInternal
{
	public static final String REQUEST_HANDLER_NAME = "REQUEST_HANDLER_NAME";
    public static final String IOR_ACCESS = "IOR_ACCESS";
    public static final String OSAGENT_ADDR = CommonConfigUtils.CORBA_SERVER_ADDR_PROP;
    public static final String OSAGENT_PORT = CommonConfigUtils.CORBA_SERVER_PORT_PROP;
       
    public static final String NO_TPR_REQUESTS          = "NO_TPR_REQUESTS";
    public static final String INPUT_SOURCE_API_VALUE = "A";
     
  //Added to bypass request router and validate router gateway
    
    private static final String DOWN_SERVER_RETRY_TIME = "DOWN_SERVER_RETRY_TIME";
    
    private static final String NOTIFICATION_CHANNEL_NAME = "NOTIFICATION_CHANNEL_NAME";
    private static final String USE_EVENTCHANNEL_FLAG = "USE_EVENTCHANNEL_FOR_GUI_NOTIFICATIONS";
    private static final String DEFAULT_CHANNEL_NAME = "NightFire.UI.Notifications";
    private static final String ACT_IVR_USER="activruser";
    private static final String ACT_ARRIS_USER="actarrisuser";
    
    public static final String SERVER_NAME_PREFIX = "Nightfire.SPI.Full_Neustar_SOA.Neustar_SOA_Request";
    
    private static final String SERVER_NAMES_KEY = "SERVER_NAMES_KEY";  
    
    public static final String SOA_VALIDATE_GATEWAY_NAME = "SOA_VALIDATE_REQUEST";

    private SeqIdGenerator idGenerator = null;

    private String iorAccess;
    
   //Added to bypass request router and validate router gateway
    private String serverNamesKey = null;
    private List serverNames;
    private int serverCount;
    private int totalServerCount;
    private volatile int roundRobinCounter;
    private long globalStartTime;
    private long downServerRetryTime;   
    private String   notificationChannelName;
    private boolean  useEventChannelForNotifications = true;

 
  /**
   * Initialize this proxy class with information required to locate the gateway servers.
   * The SOAPRequestHandlerSoapBindingImpl class invokes this method in a synchronized block.
   */
    public void initialize ( ) throws FrameworkException
    {
        log.debug("SoaGatewayRequestHandler initializing...");
        
        Properties initParameters = ServiceInitializer.getInitParameters();
        
        //Added to bypass request router and validate router gateway
        
        serverNamesKey = initParameters.getProperty( SERVER_NAMES_KEY);
        
        downServerRetryTime =  Long.parseLong(initParameters.getProperty( DOWN_SERVER_RETRY_TIME ));
        
        globalStartTime = System.currentTimeMillis( );
               
		serverNames = getServerNames(serverNamesKey);
		// check to see that at least one server name was
		// discovered
		// in the
		// properties
		if (serverNames.size() == 0) {
			throw new FrameworkException(
					"No SERVER_NAME"
							+ " properties were found under property key ["
							+ serverNamesKey
							+ "] ");
		}
		serverCount = serverNames.size();
		totalServerCount = serverCount;
		roundRobinCounter = 0;
		
                
        String orbAgentPort = initParameters.getProperty( OSAGENT_PORT );
        
        String orbAgentAddr = initParameters.getProperty( OSAGENT_ADDR );
        
        requestHandlerName = initParameters.getProperty( REQUEST_HANDLER_NAME );
 
        String noTPRRequestsProp = initParameters.getProperty( NO_TPR_REQUESTS );

        // get the ior-access method from the context; default is DB.
        iorAccess = initParameters.getProperty( IOR_ACCESS, ObjectLocator.IOR_ACCESS_DB );
        
        notificationChannelName = initParameters.getProperty(NOTIFICATION_CHANNEL_NAME);
        String str = initParameters.getProperty(USE_EVENTCHANNEL_FLAG);
        if(StringUtils.hasValue(str))
        {
            try
            {
                useEventChannelForNotifications = StringUtils.getBoolean(str);
            }
            catch (FrameworkException fe)
            {
                // Use default.
                useEventChannelForNotifications = true;
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("SoaGatewayRequestHandler(): Application parameter [" + NOTIFICATION_CHANNEL_NAME + "] has value [" + notificationChannelName + "].");
            log.debug("SoaGatewayRequestHandler(): Application parameter [" + USE_EVENTCHANNEL_FLAG + "] has value [" + useEventChannelForNotifications + "].");
        }

        if ( !StringUtils.hasValue( notificationChannelName ) )
        {
            notificationChannelName = DEFAULT_CHANNEL_NAME;
        }
        if (log.isDebugEnabled())
            log.debug("SoaGatewayRequestHandler(): Value to be used for [" + NOTIFICATION_CHANNEL_NAME + "] is [" + notificationChannelName + "].");


        if ( !StringUtils.hasValue(orbAgentAddr) || !StringUtils.hasValue(orbAgentPort) )
        {
            throw new FrameworkException( "ORB agent address and port properties need to be specified." );
        }
        else
        {
            log.debug( "SoaGatewayRequestHandler ORBAgentAddress[" +
                    orbAgentAddr + "], ORBAgentPort[" + orbAgentPort +
                    ", noTPRRequests[" + noTPRRequestsProp +
                    "]" + ", iorAccess[" + iorAccess + "].");

        }
        try
        {
          if (StringUtils.hasValue(noTPRRequestsProp))
          {
            noTPRRequests = getNoTPRRequests(noTPRRequestsProp);

          }
            Properties orbProperties = new Properties();

            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_ADDR_PROP, orbAgentAddr);

            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_PORT_PROP, orbAgentPort);

            CorbaPortabilityLayer cpl = new CorbaPortabilityLayer(null, orbProperties);

            log.debug("SoaGatewayRequestHandler: Successfully created a CorbaPortabilityLayer object.");
            
            orb = cpl.getORB();
            
            objectLocator = new ObjectLocator(cpl.getORB());

            idGenerator = new SeqIdGenerator();

            log.debug("SoaGatewayRequestHandler: Successfully created an ObjectLocator object.");
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: SoaGatewayRequestHandler: Failed to initialize supporting Corba objects:\n" + e.getMessage();

            log.error(errorMessage);

            throw new FrameworkException(errorMessage);
        }
        
        log.debug("SoaGatewayRequestHandler initialized.");
        
     }

  private List getNoTPRRequests(String noTPRRequestsProp)
  {
    List result = new ArrayList();

    StringTokenizer st = new StringTokenizer(noTPRRequestsProp, "|");
    while (st.hasMoreElements())
    {
      result.add(st.nextElement());
    }

    return result;
  }

  /**
     * Proxy method for processSync method on RequestHandlerOperations and EJB invocations.
     *
     * @param header - the Nightfire style header.
     * @param message - the message to be submitted.
     *
     * @return String - the response from the synchronous call, with
     *                  String[0] = response header and
     *                  String[1] = response body.
     *
     * @exception java.rmi.RemoteException - Should be thrown if there is a
     *                        error processing the request.
     */
    public String[] process( SOAPRequestHandlerInternal.RequestContext context ) throws RemoteException
    {
      XMLMessageParser headerParser = context.headerParser;
      String body = context.body;
      String methodName = context.methodName;
      
      if ( log.isDebugEnabled() )            
         log.debug( "SoaGatewayRequestHandler.process invoked." );
      
      //The header we receive here is already in the gateway request header format.

      //Verify that the combination of interface version, supplier, request type(transaction) are valid.
      validateHeader( context );

      //Add node in header (like InputSource).
      setNodesInHeader(context);
      
      String serverName = null;
      String requestType = null;
      String cid = null;
      String uid = null;

      try
      {
          if ( headerParser.exists( HeaderNodeNames.REQUEST_NODE ) )
        	  requestType = headerParser.getValue( HeaderNodeNames.REQUEST_NODE );
          
          if ( headerParser.exists( HeaderNodeNames.CUSTOMER_ID_NODE ) )
        	  cid = headerParser.getValue( HeaderNodeNames.CUSTOMER_ID_NODE );
   
          if ( headerParser.exists( HeaderNodeNames.USER_ID_NODE ) )
        	  uid = headerParser.getValue( HeaderNodeNames.USER_ID_NODE );

      }
      catch (FrameworkException e)
      {
          SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, e.toString() );
      }
      String[] response = new String[2];
      response[0] = context.header;
      if (requestType != null && requestType.equals("query-messages")) {
			try {
				serverName = requestHandlerName;
				RequestHandler requestHandler = getHandler(serverName);
				response[1] = sendMessage(context.header, body, methodName,requestHandler, cid, uid);
			} catch (FrameworkException idex) {
				SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, idex.toString() );
			}
		} else {
			
    	    response[1] = send ( context.header, body, methodName, cid, uid);
    	    
      }
      if ( log.isDebugEnabled() )
        log.debug( "GatewayRequestHandler.process done." );
      return response;
  }
    /**
     * Send the message to the gateway server specified by serverName, by invoking the method "methodName"
     * on it. The header and body are passed in as method arguments.
     */
    private String send (String header, String body, String methodName, String cid, String uid)
    throws RemoteException
    {
   
    	String response = null;
		if(serverCount < totalServerCount && System.currentTimeMillis() - globalStartTime > downServerRetryTime) {
			log.debug("ServerCount:" + serverCount);				
			log.debug("TotalServerCount :"+ totalServerCount);			
			synchronized (SoaGatewayRequestHandler.class) {
					globalStartTime = System.currentTimeMillis();
					serverNames.clear();
					serverNames = getServerNames(serverNamesKey);
					if (serverNames.size() == 0) {
						throw new RemoteException(
								"No SERVER_NAME"
										+ " properties were found under property key ["
										+ serverNamesKey
										+ "] ");
					}
					serverCount = serverNames.size();
					roundRobinCounter = 0;
			}
		}
		try
		{
			// get the index of the next server this client should try
			int serverIndex = getNextIndex();
			// This loop tries all servers in the serverNames list
			// until it gets a success.
			for (int count = 0; count < serverCount; count++) {
				String serverName = serverNames.get(serverIndex).toString();
				try {
					org.omg.CORBA.Object corbaObject = objectLocator.find(serverName, iorAccess);
					
					RequestHandler requestHandler = RequestHandlerHelper.narrow(corbaObject);
					
					return sendMessage(header, body, methodName,requestHandler, cid, uid);
					
				} catch (Exception ohNo) {					
					if (ohNo.getMessage().contains(	"Could not resolve CORBA name")
							|| ohNo.getMessage().contains("Retries exceeded, couldn't reconnect to")) {
						    Debug.error("Could not deliver request to server ["	+ serverName + "]: ");
						    synchronized (SoaGatewayRequestHandler.class) {
							objectLocator.removeFromCache(serverName);
							serverNames.remove(serverName);
							serverCount = serverNames.size();
							roundRobinCounter = 0;
							count--;
						}
					}else {
						if (ohNo.getMessage().contains("SOAP-ENV:Server.ProcessingException"))
						    SOAUtility.throwSOAPFaultException(SOAPConstants.PROCESSING_FAULT, ohNo.toString());
						else if (ohNo.getMessage().contains("SOAP-ENV:Server.NullPointerException"))
							SOAUtility.throwSOAPFaultException(SOAPConstants.NULL_FAULT, ohNo.toString());
						else if (ohNo.getMessage().contains("SOAP-ENV:Server.SecurityException"))
							SOAUtility.throwSOAPFaultException(SOAPConstants.SECURITY_FAULT, ohNo.toString());
						else if (ohNo.getMessage().contains("SOAP-ENV:Server.CommunicationsException"))
							SOAUtility.throwSOAPFaultException(SOAPConstants.COMMUNICATIONS_FAULT, ohNo.toString());
						else if (ohNo.getMessage().contains("SOAP-ENV:Server.MethodNotSupported"))
							SOAUtility.throwSOAPFaultException(SOAPConstants.METHOD_NOT_SUPPORTED_FAULT, ohNo.toString());
						else 
							SOAUtility.throwSOAPFaultException(SOAPConstants.MESSAGE_FAULT, ohNo.toString());
					}
				}
				if (serverCount > 0) {
					// try the next server
					serverIndex++;
					serverIndex %= serverCount;
					// this assures that the next index is incremented and
					// that the load-balancing is evenly distributed even
					// in the case where a server may be down
					getNextIndex();
				}
			}
			// If we get here, then we have tried all known servers,
			// and none of them were available.
			SOAPUtils.throwSOAPFaultException(SOAPConstants.PROCESSING_FAULT,"None of the servers were available to service the request: ");
		}
		catch ( Exception ex )
        {
			if (ex.getMessage().contains("SOAP-ENV:Server.ProcessingException"))
			    SOAUtility.throwSOAPFaultException(SOAPConstants.PROCESSING_FAULT, ex.toString());
			else if (ex.getMessage().contains("SOAP-ENV:Server.NullPointerException"))
				SOAUtility.throwSOAPFaultException(SOAPConstants.NULL_FAULT, ex.toString());
			else if (ex.getMessage().contains("SOAP-ENV:Server.SecurityException"))
				SOAUtility.throwSOAPFaultException(SOAPConstants.SECURITY_FAULT, ex.toString());
			else if (ex.getMessage().contains("SOAP-ENV:Server.CommunicationsException"))
				SOAUtility.throwSOAPFaultException(SOAPConstants.COMMUNICATIONS_FAULT, ex.toString());
			else if (ex.getMessage().contains("SOAP-ENV:Server.MethodNotSupported"))
				SOAUtility.throwSOAPFaultException(SOAPConstants.METHOD_NOT_SUPPORTED_FAULT, ex.toString());
			else 
				SOAUtility.throwSOAPFaultException(SOAPConstants.MESSAGE_FAULT, ex.toString());
        }
        
        if ( response == null )
            response = "<Body></Body>";
        return response;
	}

    private void setNodesInHeader(SOAPRequestHandlerInternal.RequestContext context) throws RemoteException
    {
        String head = context.header;
        String subdomainId = context.subDomainId;
        
        XMLMessageGenerator gen;
        boolean inputSourceNodePresent, subDomainIdNodePresent;
        try{
            inputSourceNodePresent = (! context.headerParser.exists(HeaderNodeNames.INPUT_SOURCE_NODE));
            subDomainIdNodePresent = (! context.headerParser.exists(HeaderNodeNames.SUBDOMAIN_ID_NODE ));

            if (inputSourceNodePresent || subDomainIdNodePresent)
            {
                gen =  new XMLMessageGenerator(XMLLibraryPortabilityLayer.convertStringToDom(head));

                if (inputSourceNodePresent)
                {
                gen.setAttributeValue(HeaderNodeNames.INPUT_SOURCE_NODE,HeaderNodeNames.VALUE_ATTRIBUTE,INPUT_SOURCE_API_VALUE);
                if ( log.isDebugEnabled() )
                    log.debug("Adding InputSource node in header with value " + INPUT_SOURCE_API_VALUE);
                }
                if (subDomainIdNodePresent)
                {
                    gen.setAttributeValue(HeaderNodeNames.SUBDOMAIN_ID_NODE,HeaderNodeNames.VALUE_ATTRIBUTE, subdomainId);
                    if ( log.isDebugEnabled() )
                        log.debug("Adding SubDomainId node in header as [" + subdomainId + "]");
                }

                String msgId = idGenerator.getStrNextId();
                if ( log.isDebugEnabled() )
                    log.debug("Adding unique message id to header:"+msgId);

                gen.setValue(CustomerContext.MESSAGE_ID,msgId);

                context.header = XMLLibraryPortabilityLayer.convertDomToString(gen.getDocument());
                context.headerParser = (XMLMessageParser) gen.getParser();
            }
            else
                if ( log.isDebugEnabled() )
                    log.debug("Input source node found in header");
        }
         catch(MessageException e)
        {
        	 
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, "MessageException Could not set node in header, " +e.toString());
        }
        catch(FrameworkException e)
       {
           SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT,"FrameworkException Could not set node in header," +e.toString());
       }
    }
    /**
     * Validate the header data specified for this request.
     * @param context The context containing the request information.
     *
     * @exception RemoteException if the interface version is invalid.
     */
    private void validateHeader( SOAPRequestHandlerInternal.RequestContext context ) throws RemoteException
    {
        boolean isValid = false;
        String supplier = null;
        String transaction = null;
        String interfaceVersion = null;
        String customerID = context.customerID;

        //First get key header information.
        try
        {
            XMLMessageParser parser = context.headerParser;
            supplier = parser.getValue( HeaderNodeNames.SUPPLIER_NODE );
            transaction = parser.getValue( HeaderNodeNames.REQUEST_NODE );

            if (noTPRRequests.contains(transaction))
            {
              if ( log.isDebugEnabled() ) {
                log.debug("Skipping interface version validation for request [" + transaction + "].");
              }
                isValid = true;
            }
            else
            {
                if ( parser.exists( HeaderNodeNames.INTERFACE_VERSION_NODE ) )
                    interfaceVersion = parser.getValue( HeaderNodeNames.INTERFACE_VERSION_NODE );
                else
                    throw new FrameworkException( "Missing node InterfaceVersion in header." );

                //Now check with the TradingParternerRelationship component if the header information is valid.
                isValid = TradingPartnerRelationship.getInstance(customerID).validateInterfaceVersion(supplier, transaction, interfaceVersion);
            }

        }
        catch (FrameworkException e)
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT,"Could not validate header, " + e.toString() );
        }

        //If not valid, throw exception
        if ( !isValid )
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.SECURITY_FAULT, "Customer[" +
            context.customerID + "] doesn't have permission for this combination of request[" +
            transaction + "], supplier[" + supplier + "] and interfaceVersion[" + interfaceVersion + "]." );
        }
        else
        {
        //log success
            if ( log.isDebugEnabled() ) {
                log.debug ( "Customer[" + context.customerID + "] has permission for request[" +
                transaction + "], supplier[" + supplier + "] and interfaceVersion[" + interfaceVersion + "]." );
            }
        }
    }//validateHeader

    /**
     * Send the message to the gateway server specified by serverName, by invoking the method "methodName"
     * on it. The header and body are passed in as method arguments.
     */
	private String sendMessage(String header, String body, String methodName,
			RequestHandler requestHandler, String cid, String uid)
    throws RemoteException
    {
  
        String response = null;
        
        DataHolder requestData = null;
        
        DataHolder responseData = null;

        try
        {
        	 requestData = new DataHolder(header, body);
        	
             if (methodName.equals(SOAPConstants.PROCESS_ASYNC))
            {
                log.debug("sendRequest(): Executing RequestHandler's processAsync() ...");

                requestHandler.processAsync( header, body );
                
                responseData = new DataHolder();

            }
            else
            if (methodName.equals(SOAPConstants.PROCESS_SYNC))
            {
                log.debug("sendRequest(): Executing RequestHandler's processSync() ...");

                StringHolder responseBody = new StringHolder();
                requestHandler.processSync( header, body, responseBody);

                response = responseBody.value;
                responseData = new DataHolder(null, responseBody.value);
            }
            else
            {
                String errorMessage = "ERROR: SoaGatewayRequestHandler: Failed to send the request, unrecognizable methodName [" +
                methodName + "].";

                log.error(errorMessage);

                SOAPUtils.throwSOAPFaultException ( SOAPConstants.MESSAGE_FAULT, errorMessage );
            }
            log.debug( "SoaGatewayRequestHandler: Sent message to gateway." );
        }
        catch (InvalidDataException idex)
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.MESSAGE_FAULT, idex.errorMessage );
        }
        catch (CorbaServerException csex)
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, csex.errorMessage );
        }
        catch ( NullResultException nrex )
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, nrex.errorMessage );
        }
        catch ( Exception e )
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, e.toString() );
        }

        if ( response == null )
            response = "<Body></Body>";
         
        //If we reach here, it means that the send operation was successful. So we need
        //to check if a notification has to be sent to the upstream system.
		try {
			if (uid != null && (uid.equals(ACT_IVR_USER)|| uid.equals(ACT_ARRIS_USER)))
				
				new ExternalNotifier().notify(notificationChannelName,
						requestData, responseData, cid, uid, log, orb,
						objectLocator, useEventChannelForNotifications);
		} catch (Exception e) {
			if (log.isDebugEnabled())
				log.debug("SoaGatewayRequestHandler(): Exception occured while sending notification to channel ["
								+ notificationChannelName + "].");
			SOAPUtils.throwSOAPFaultException(SOAPConstants.PROCESSING_FAULT, e.toString());
		}
		return response;
	}
    
    
    private RequestHandler getHandler( String serverName ) throws FrameworkException
    {

       return getHandler(true, serverName);
    }

    /**
     * This method tries to obtain the request handler
     * if the handler obtain from the ObjectLocator is bad,
     * then it will flush the cache and try once more.
     * @param firstTry indicates that this is the first try to obtain a request handler.
     *     true- will cause this method to be called once more if there is an error with the request handler,
     *     false- will cause this method to fail on the first try.
     *
     */
    private RequestHandler getHandler(boolean firstTry, String serverName) throws FrameworkException
    {

        try
        {
            // locate the server using the iorAccess method defined in the deployment descriptor.
            org.omg.CORBA.Object corbaObject = objectLocator.find(serverName, iorAccess);

            if (corbaObject == null) {
                String errorMessage = "ERROR: SoaGatewayRequestHandler: Failed to locate the Corba object [" +
                serverName + "] via ObjectLocator.";

                log.error(errorMessage);

                throw new FrameworkException(errorMessage);
            }

            RequestHandler requestHandler = RequestHandlerHelper.narrow(corbaObject);

            if (requestHandler == null) {
                String errorMessage = "ERROR: SoaGatewayRequestHandler: The Corba object [" +
                serverName + "] obtained via ObjectLocator is not of type RequestHandler.";

                log.error(errorMessage);

                throw new FrameworkException(errorMessage);
            }

            // this call will throw an NotFound exception if the
            // requesthandler is not longer valid.
            //
            if ( requestHandler._non_existent() ) {
               String warn = "SoaGatewayRequestHandler: Request handler is no longer valid will try to obtain another one.";
               log.warn(warn);
               throw new FrameworkException(warn);
            }

            if (log.isDebugEnabled()) {
                log.debug("SoaGatewayRequestHandler: Successfully located the RequestHandler object [" +
                serverName + "].");
            }

            // reset the server name and count back
            
            return requestHandler;

        }
        catch (Exception e)
        {

            if (firstTry)
            {

                if (log.isDebugEnabled()) 
                    log.debug("SoaGatewayRequestHandler: Trying again with serverName with name[" + serverName + "].");

                objectLocator.removeFromCache(serverName);
                return getHandler(false, serverName);
            }
            else
            {
                if (log.isDebugEnabled()) 
                    log.debug("sendRequest(): Failed to get serverName again with name[" + serverName + "].");
                    
                    throw new FrameworkException(e);
             }
        }
    }

    
    
     /**
     * Initialize the logger provided by the GUI infrastructure.
     */
    protected static DebugLogger log =  DebugLogger.getLogger("/axis", SoaGatewayRequestHandler.class);

    /**
     * Server that implements the request handler interface. Typically this is the router name.
     * This value is set up only once. This is used only used if the input message does not
     * explicitly specify a server name. If a server name is specified, it gets used, else
     * the header gets passed on to the router, which will use it to locate the server.
     */
    protected String requestHandlerName = null;

    /**
     * Object locator used to find the server.
     * This value is set up only once.
     */
    protected ObjectLocator objectLocator = null;
    
    private ORB orb;

    private List noTPRRequests = new ArrayList();
    
   /**
    *
    * @param key String
    * @return List
    */
   public static List getServerNames( String key){

	   List<String> serverNames = new ArrayList<String>();
	      
	      if(StringUtils.hasValue(key)){
	    	  
	    	  StringTokenizer serverNameTokens = new StringTokenizer(key,",");
		      
		      while(serverNameTokens.hasMoreTokens()){
		    	  
		    	  String tokenVal = serverNameTokens.nextToken().trim();
		    	  
		    	  if(StringUtils.hasValue(tokenVal) && tokenVal.length() >= 20){
		    		 
		    		  if(tokenVal.substring(0, 20).equals(SOA_VALIDATE_GATEWAY_NAME)){
			    		  
			    		  if(tokenVal.length() > 20){
			    		  
			    			  serverNames.add(SERVER_NAME_PREFIX+tokenVal.substring(20));
			    		  
			    		  }else if(tokenVal.length() == 20){
			    		  
			    			  serverNames.add(SERVER_NAME_PREFIX);
			    		  
			    		  }
			    	  }else{
			    		  if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
				    		  Debug.log(Debug.ALL_ERRORS, "Inside getServerNames(): Init param SERVER_NAMES_KEY is not configured with proper " +
					    			  "Validate gateway instance name. It has value["+tokenVal+"]");
				    	  }
			    	  }
		    	  }else{
		    		  if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
			    		  Debug.log(Debug.ALL_ERRORS, "Inside getServerNames(): Init param SERVER_NAMES_KEY is not configured with proper " +
				    			  "Validate gateway instance name. It has value ["+tokenVal+"]");
			    	  }
		    	  }
		    	  
		      }
		  }
	      return serverNames;
   }

   
   /**
    * This gets the next server index to try first. This changing index
    * implements the round robin functionality.
    *
    * @return int the index of the server to try next.
    */
   private int getNextIndex() throws FrameworkException {
		try {
			int index = roundRobinCounter % serverCount;
			roundRobinCounter++;
			roundRobinCounter %= serverCount;
			return index;
		} catch (Exception e) {
			throw new FrameworkException(e);
		}
	}

}
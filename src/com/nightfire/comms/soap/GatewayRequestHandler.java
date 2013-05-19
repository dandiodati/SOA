/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 */
package com.nightfire.comms.soap;

import java.rmi.*;
import java.util.*;
import java.net.MalformedURLException;
import javax.xml.rpc.ServiceException;

import org.omg.CORBA.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.PropertyException;
import com.nightfire.framework.debug.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.message.common.xml.XMLGenerator;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.security.tpr.*;
import com.nightfire.spi.common.*;
import com.nightfire.framework.util.SeqIdGenerator;
import com.nightfire.framework.repository.rules.RuleEvaluator;
import com.nightfire.common.ProcessingException;
import com.nightfire.comms.soap.ws.WSRequestHandlerPortTypeServiceLocator;
import com.nightfire.comms.soap.ws.WSRequestHandlerPortType;

/**
 * Proxy to route requests to gateway communication servers.
 */
public class GatewayRequestHandler implements SOAPRequestHandlerInternal
{
    public static final String REQUEST_HANDLER_NAME = "REQUEST_HANDLER_NAME";
    public static final String IOR_ACCESS = "IOR_ACCESS";
    public static final String OSAGENT_ADDR         = CommonConfigUtils.CORBA_SERVER_ADDR_PROP;
    public static final String OSAGENT_PORT         = CommonConfigUtils.CORBA_SERVER_PORT_PROP;
    public static final String SERVER_NAME          = "ServerName";

    public static final String NO_TPR_REQUESTS          = "NO_TPR_REQUESTS";

    public static final String INPUT_SOURCE_API_VALUE = "A";
    
    private static final String ALTERNATE_ROUTER_COUNT = "ALTERNATE_ROUTER_COUNT";
    private static final String COMMON_PROPERTIES = "COMMON_PROPERTIES";
    private static final String GWS_URL_PROP = "GWS_URL";
    private static final String ASSUMED_ROUTER_NAME = "Nightfire.Router";
    /**
     * The category in repository that contains the configuration for non-TPR requests
     * (for which TPR entries are not available).
     */
    private static final String NO_TPR_CONFIG_CATEGORY = "noTPRConfig";

    private static final String NO_TPR_CONFIG_GWS_ENABLED_PROP = "gws_enabled";

    //public static final String QUERY_MESSAGES       = "query-messages";
    private SeqIdGenerator idGenerator = null;

    private String iorAccess;

    private int           altRouterCount;
    private int           currentRouterCount;

  /**
   * Initialize this proxy class with information required to locate the gateway servers.
   * The SOAPRequestHandlerSoapBindingImpl class invokes this method in a synchronized block.
   */
    public void initialize ( ) throws FrameworkException
    {
        if (log.isDebugEnabled())
            log.debug("GatewayRequestHandler initializing...");
        Properties initParameters = ServiceInitializer.getInitParameters();

        //At this point the servlet context has been initialized.
        String orbAgentPort = initParameters.getProperty( OSAGENT_PORT );
        String orbAgentAddr = initParameters.getProperty( OSAGENT_ADDR );
        requestHandlerName = initParameters.getProperty( REQUEST_HANDLER_NAME );
        String noTPRRequestsProp = initParameters.getProperty( NO_TPR_REQUESTS );

        // get the ior-access method from the context; default is DB.
        iorAccess = initParameters.getProperty( IOR_ACCESS, ObjectLocator.IOR_ACCESS_DB );

        if ( !StringUtils.hasValue(orbAgentAddr) || !StringUtils.hasValue(orbAgentPort) )
        {
            throw new FrameworkException( "ORB agent address and port properties need to be specified." );
        }
        else
        {
            if (log.isDebugEnabled())
                log.debug( "GatewayRequestHandler ORBAgentAddress[" +
                    orbAgentAddr + "], ORBAgentPort[" + orbAgentPort +
                    "], requestHandlerName[" + requestHandlerName + "], noTPRRequests[" + noTPRRequestsProp +
                    "]" + ", iorAccess[" + iorAccess + "].");

        }
        try
        {
          if (StringUtils.hasValue(noTPRRequestsProp))
          {
            noTPRRequests = getNoTPRRequests(noTPRRequestsProp);

          }
            Properties            orbProperties = new Properties();

            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_ADDR_PROP, orbAgentAddr);

            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_PORT_PROP, orbAgentPort);

            CorbaPortabilityLayer cpl           = new CorbaPortabilityLayer(null, orbProperties);

            if (log.isDebugEnabled())
                log.debug("GatewayRequestHandler: Successfully created a CorbaPortabilityLayer object.");

            objectLocator                       = new ObjectLocator(cpl.getORB());

            idGenerator = new SeqIdGenerator();

            if (log.isDebugEnabled())
                log.debug("GatewayRequestHandler: Successfully created an ObjectLocator object.");
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: GatewayRequestHandler: Failed to initialize supporting Corba objects:\n" + e.getMessage();

            log.error(errorMessage);

            throw new FrameworkException(errorMessage);
        }

        // get the router count property.
        try 
        {
            Hashtable properties = PersistentProperty.getProperties(COMMON_PROPERTIES, ALTERNATE_ROUTER_COUNT);
            String altRouterCountStr = (String) properties.get(ALTERNATE_ROUTER_COUNT);
            
            if (log.isDebugEnabled())
                log.debug("GatewayRequestHandler(): Value to be used for [" + ALTERNATE_ROUTER_COUNT + "] is [" + altRouterCountStr + "].");

            if(StringUtils.hasValue(altRouterCountStr))
            {
                this.altRouterCount = Integer.parseInt(altRouterCountStr);
            }
        } 
        catch (Exception e)
        {
            log.warn("Error while reading PeristentProperty ["+COMMON_PROPERTIES+" "+ALTERNATE_ROUTER_COUNT+"]."+e.getMessage());
            log.warn("Ignoring the warning and setting the alternate count to 0");
            this.altRouterCount = 0;
        }
        
        if (log.isDebugEnabled())
            log.debug("GatewayRequestHandler initialized.");
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
     * @param context - RequestContext object containing request header parser and body.
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

      if (log.isDebugEnabled())
          log.debug( "GatewayRequestHandler.process invoked." );

      //The header we receive here is already in the gateway request header format.

      //Verify that the combination of interface version, supplier, request type(transaction) are valid.
      validateHeader( context );

      //Add node in header (like InputSource).
      setNodesInHeader(context);

      String serverName = null;
      try
      {
          if ( headerParser.exists( SERVER_NAME ) )
              serverName = headerParser.getValue( SERVER_NAME );

          if ( !StringUtils.hasValue(serverName) )
              serverName = requestHandlerName;

      }
      catch (FrameworkException e)
      {
          SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, e.toString() );
      }

      boolean isGWSEnabled = getGWSEnabledFlag(context);

      String[] response = new String[2];
      response[0] = context.header;
      response[1] = sendMessage ( context.customerID, context.header, body, methodName, serverName, isGWSEnabled );

      if (log.isDebugEnabled())
          log.debug( "GatewayRequestHandler.process done." );

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
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, e.toString());
        }
        catch(FrameworkException e)
       {
           SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, e.toString());
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
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT,
                                                "Could not validate header, " + e.toString() );
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
    private String sendMessage (String customerId, String header, String body, String methodName, String serverName, boolean isGWSEnabled )
    throws RemoteException
    {
        if (log.isDebugEnabled())
            log.debug( "GatewayRequestHandler: Sending message to gateway: header[" + header +
                  "], body[" + body + "], methodName[" + methodName + "], serverName[" + serverName + "]." );

        String response = null;

        try
        {
            // To send the message via corba
            RequestHandler requestHandler = null;

            if (!isGWSEnabled)
                requestHandler = getHandler( serverName );

            if (methodName.equals(SOAPConstants.PROCESS_ASYNC))
            {
                if (isGWSEnabled)
                {
                    if (log.isDebugEnabled())
                        log.debug("sendRequest(): Executing Gateway Web-Service processAsync() ...");

                    sendSOAPRequest(customerId, methodName, header, body, null);
                }
                else
                {
                    if (log.isDebugEnabled())
                        log.debug("sendRequest(): Executing RequestHandler's processAsync() ...");

                    requestHandler.processAsync( header, body );
                }

            }
            else
            if (methodName.equals(SOAPConstants.PROCESS_SYNC))
            {
                StringHolder responseBody = new StringHolder();
                if (isGWSEnabled)
                {
                    if (log.isDebugEnabled())
                        log.debug("sendRequest(): Executing Gateway Web-Service processSync() ...");

                    sendSOAPRequest(customerId, methodName, header, body, responseBody);
                }
                else
                {
                    if (log.isDebugEnabled())
                        log.debug("sendRequest(): Executing RequestHandler's processSync() ...");

                    requestHandler.processSync( header, body, responseBody);
                }

                response = responseBody.value;
            }
            else
            {
                String errorMessage = "ERROR: GatewayRequestHandler: Failed to send the request, unrecognizable methodName [" +
                methodName + "].";

                log.error(errorMessage);

                SOAPUtils.throwSOAPFaultException ( SOAPConstants.MESSAGE_FAULT, errorMessage );
            }
            if (log.isDebugEnabled())
                log.debug( "GatewayRequestHandler: Sent message to gateway." );
        }
        catch ( FrameworkException e )
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, e.toString() );
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

        return response;
    }

    /**
     * Obtains the Gateway web-service flag value
     * @param context The context containing the request information.
     *
     * @return boolean true if Gateway Web-service is enabled, otherwise false
     */
    private boolean getGWSEnabledFlag( SOAPRequestHandlerInternal.RequestContext context )
    {
        String supplier = null;
        String transaction = null;
        String interfaceVersion = null;
        String customerID = context.customerID;
        boolean isGWSEnabled = false;

        //First get key header information.
        try
        {
            XMLMessageParser parser = context.headerParser;
            supplier = parser.getValue( HeaderNodeNames.SUPPLIER_NODE );
            transaction = parser.getValue( HeaderNodeNames.REQUEST_NODE );

            // For non-TPR request gws_enabled flag configuration shall be found
            // under noTPRConfig repository
            if (noTPRRequests.contains(transaction))
            {
                try
                {
                    Map noTPRConfigMap = RuleEvaluator.getInstance(NO_TPR_CONFIG_CATEGORY).evaluateRule(parser.getXMLDocumentAsString(),context.body);
                    if (noTPRConfigMap != null && !noTPRConfigMap.isEmpty())
                    {
                        String isGWSEnabledStr = (String) noTPRConfigMap.get(NO_TPR_CONFIG_GWS_ENABLED_PROP);
                        isGWSEnabled = StringUtils.getBoolean(isGWSEnabledStr,false);
                        if ( log.isDebugEnabled() ) {
                            log.debug("GatewayRequestHandler#getGWSEnabledFlag(): Obtained non-TPR configuartion " + NO_TPR_CONFIG_GWS_ENABLED_PROP + " = [" + isGWSEnabledStr + "]." +
                                    " Setting isGWSEnabled = [" + isGWSEnabled + "]");
                        }
                    }
                }
                catch (FrameworkException fe)
                {
                    log.warn("GatewayRequestHandler#getGWSEnabledFlag(): Default isGWSEnabled 'false' shall be used, as configuration could not be loaded/found for non-TPR transaction [" +transaction + "] " +
                            "from cateogory [" +NO_TPR_CONFIG_CATEGORY +"]: " +fe.getMessage());
                }

                if (log.isDebugEnabled())
                    log.debug("GatewayRequestHandler#getGWSEnabledFlag(): " +
                            "obtained isGWSEnabled [" + isGWSEnabled + "] for non-TPR transaction [" + transaction + "]");
            }
            else // TPR table has GWS_ENABLED column which indicates whether given combination is for Gateway web-service
            {
                if ( parser.exists( HeaderNodeNames.INTERFACE_VERSION_NODE ) )
                    interfaceVersion = parser.getValue( HeaderNodeNames.INTERFACE_VERSION_NODE );
                else
                {
                    log.error("GatewayRequestHandler#getGWSEnabledFlag(): Missing node InterfaceVersion in header.");
                    throw new FrameworkException( "Missing node InterfaceVersion in header." );
                }

                isGWSEnabled = TradingPartnerRelationship.getInstance(customerID).isGWSEnabled(supplier, transaction, interfaceVersion);

                if (log.isDebugEnabled())
                    log.debug("GatewayRequestHandler#getGWSEnabledFlag(): isGWSEnabled obtained from TPR table [" + isGWSEnabled + "] " +
                            "for CustomerId-supplier-transaction-interfaceVersion [" + customerID + "-" + supplier + "-" + transaction + "-" + interfaceVersion + "]");
            }
        }
        catch (FrameworkException e)
        {
            log.warn("GatewayRequestHandler#getGWSEnabledFlag(): Setting isGWSEnabled to 'false' as Gateway Web-service enabled flag value could not be obtained." + e.toString() );
            isGWSEnabled = false;
        }

        if ( log.isDebugEnabled() ) {
            log.debug ( "GatewayRequestHandler#getGWSEnabledFlag(): Returning isGWSEnabled [" + isGWSEnabled + "]" );
        }

        return isGWSEnabled;
    }//validateHeader

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
                String errorMessage = "ERROR: GatewayRequestHandler: Failed to locate the Corba object [" +
                serverName + "] via ObjectLocator.";

                log.error(errorMessage);

                throw new FrameworkException(errorMessage);
            }

            RequestHandler requestHandler = RequestHandlerHelper.narrow(corbaObject);

            if (requestHandler == null) {
                String errorMessage = "ERROR: GatewayRequestHandler: The Corba object [" +
                serverName + "] obtained via ObjectLocator is not of type RequestHandler.";

                log.error(errorMessage);

                throw new FrameworkException(errorMessage);
            }

            // this call will throw an NotFound exception if the
            // requesthandler is not longer valid.
            //
            if ( requestHandler._non_existent() ) {
               String warn = "GatewayRequestHandler: Request handler is no longer valid will try to obtain another one.";
               log.warn(warn);
               throw new FrameworkException(warn);
            }

            if (log.isDebugEnabled()) {
                log.debug("GatewayRequestHandler: Successfully located the RequestHandler object [" +
                serverName + "].");
            }

            // reset the server name and count back
            reset(serverName);
            return requestHandler;

        }
        catch (Exception e)
        {

            if (firstTry)
            {

                if (log.isDebugEnabled()) 
                    log.debug("GatewayRequestHandler: Trying again with serverName with name[" + serverName + "].");

                objectLocator.removeFromCache(serverName);
                return getHandler(false, serverName);
            }
            else
            {
                if (log.isDebugEnabled()) 
                    log.debug("sendRequest(): Failed to get serverName again with name[" + serverName + "].");

                if (currentRouterCount < altRouterCount && serverName.contains(ASSUMED_ROUTER_NAME))
                {
                    currentRouterCount++;
                    serverName = ASSUMED_ROUTER_NAME+currentRouterCount;

                    if (log.isDebugEnabled()) 
                        log.debug("sendRequest(): Updated the serverName to  [" + serverName + "] and trying again...");

                    return getHandler(serverName);
                }
                else
                {
                    reset(serverName);
                    throw new FrameworkException(e);
                }

            }

        }
    }

    /**
     * This method sends the request via SOAP.
     *
     * @param methodName name of method like sync or async
     * @param header String: the Nightfire style header.
     * @param request String: the message to be submitted.
     * @param responseBody to return response in case of "processSync"
     * @throws com.nightfire.common.ProcessingException on error
     * @throws RemoteException on error
     */
    private void sendSOAPRequest(String customerId, String methodName, String header, String request, StringHolder responseBody)
        throws ProcessingException,RemoteException
    {
        if( log.isDebugDataEnabled())
        {
            log.info("GatewayRequestHandler.sendSOAPRequest(): called Header:\n" + header);
            log.info("GatewayRequestHandler.sendSOAPRequest(): called Request:\n" + request);
        }

        try
        {
            // stores the target url for web service.
            String endPointURL = getGWSURL(customerId, new XMLPlainGenerator(header));

            if( log.isDebugDataEnabled())
                log.debugData( "GatewayRequestHandler.sendSOAPRequest(): endPointURL [" + endPointURL +"]");

            java.net.URL url = new java.net.URL( endPointURL );

            long bindStartTime = System.currentTimeMillis( );

            // Construct a service.
            WSRequestHandlerPortTypeServiceLocator service = new WSRequestHandlerPortTypeServiceLocator( );

            // Use the service to get a stub to the service.
            WSRequestHandlerPortType client = service.getWSRequestHandlerPort( url );

            if( log.isBenchmarkEnabled())
                log.debugData( "GatewayRequestHandler.sendSOAPRequest(): Bind to web service URL.  ELAPSED TIME [" +
                                (System.currentTimeMillis() - bindStartTime) + "] msec." );

            if( log.isDebugDataEnabled())
                log.debugData( "GatewayRequestHandler.sendSOAPRequest(): Invoking method [" + methodName + "], with the following header [" + header + "] and request body: [" + request + "]");

            long methodStartTime = System.currentTimeMillis( );

            if ( methodName.equalsIgnoreCase( "manage" ) )
            {
                throw new ProcessingException( "ERROR: This method [" + methodName + "] is not supported!" );
            }
            else if ( methodName.equalsIgnoreCase( "processAsync" ) )
            {
                //throw new ProcessingException( "ERROR: This method [" + methodName + "] is not supported!" );

                // Now with 3.15.2 GatewayWebService shall support processAsync as well. Although it shall use
                // processSync method to process the message but will not return any response to caller.
                client.processSync( header, request );
            }
            else if ( methodName.equalsIgnoreCase( "processSync" ) )
            {
                String response = client.processSync( header, request );
                responseBody = new StringHolder(response);

                if( log.isDebugDataEnabled())
                    log.debugData( "Response: " + response );
            }
            else
            {
                log.error("ERROR: This method [" + methodName + "] is unknown!" );
                throw new ProcessingException( "ERROR: This method [" + methodName + "] is unknown!" );
            }

            long elapsedTime = System.currentTimeMillis() - methodStartTime;

            if( log.isDebugDataEnabled())
                log.debugData ( "GatewayRequestHandler.sendSOAPRequest(): [" + new Date().toString() + "] Web service call succeeded.  ELAPSED TIME ["
                    + elapsedTime + "] msec." );
        }
        catch (PropertyException e)
        {
            log.error("GatewayRequestHandler.sendSOAPRequest() Error: Unable to retrieve persistent property", e);
            throw new ProcessingException( e );
        }
        catch (MessageException e)
        {
            log.error("GatewayRequestHandler.sendSOAPRequest() Error: Unable to parse the xml: ", e);
            throw new ProcessingException( e );
        }
        catch (MalformedURLException e)
        {
            log.error("GatewayRequestHandler.sendSOAPRequest() Error: Unable to locate URL: ", e);
            throw new ProcessingException(e);
        }
        catch (ServiceException e)
        {
            log.error("GatewayRequestHandler.sendSOAPRequest() Error: Unable to obtain stub for the service: ", e);
            throw new ProcessingException(e );
        }

    }

    /**
     * Obtain Gateway web-service url for given header
     * @param customerId information
     * @param headerParser Header Dom parser
     * @return string
     * @throws PropertyException on error
     * @throws MessageException on error
     */
    private String getGWSURL(String customerId, XMLGenerator headerParser) throws PropertyException, MessageException
    {
        String transaction = null;
        String supplier = null;
        String gwsURL = null;
        String gwsURLPropNameDefault = GWS_URL_PROP;
        String gwsURLPropNameWithSuppTrans= null;
        String gwsURLPropNameWithTrans = null;
        String gwsURLPropName = null;
        Hashtable commonProps;

        try{
            CustomerContext.getInstance().setCustomerID(customerId);
        }
        catch (FrameworkException fe){
            log.warn("GatewayRequestHandler.getGWSURL(): Could not set customer context with customer id [" + customerId + "]. Hence Gateway Web-service URL shall be used with DEFAULT customerId.");
        }

        try
        {
            supplier    = headerParser.getValue(HeaderNodeNames.SUPPLIER_NODE);
            transaction = headerParser.getValue(HeaderNodeNames.REQUEST_NODE);
        }
        catch (MessageException e)
        {
            log.warn("GatewayRequestHandler.getGWSURL(): Could not obtain request/supplier node value from header. supplier ["
                    + supplier + "], transaction [" + transaction + "] shall be used to form GWS URL name.");
        }

        try
        {
            // Form property name using supplier and transaction (in upper case) in GWS property name e.g GWS_URL_ATT_LSR_ORDER
            if (StringUtils.hasValue(supplier) && StringUtils.hasValue(transaction))
                gwsURLPropNameWithSuppTrans = gwsURLPropNameDefault + "_" + supplier.toUpperCase() + "_" + transaction.toUpperCase();

            // Form property name using transaction (in upper case) in GWS property name e.g GWS_URL_WIRELESS_NPORT
            if (StringUtils.hasValue(transaction) )
                gwsURLPropNameWithTrans = gwsURLPropNameDefault + "_" + transaction.toUpperCase();

            commonProps = PersistentProperty.getProperties(COMMON_PROPERTIES, COMMON_PROPERTIES);

            if (commonProps!= null)
            {
                if (StringUtils.hasValue(gwsURLPropNameWithSuppTrans) && commonProps.keySet().contains(gwsURLPropNameWithSuppTrans))
                {
                    gwsURL = (String) commonProps.get(gwsURLPropNameWithSuppTrans);
                    gwsURLPropName = gwsURLPropNameWithSuppTrans;
                }

                if (!StringUtils.hasValue(gwsURL))
                {
                    if (StringUtils.hasValue(gwsURLPropNameWithTrans) && commonProps.keySet().contains(gwsURLPropNameWithTrans))
                    {
                        gwsURL = (String) commonProps.get(gwsURLPropNameWithTrans);
                        gwsURLPropName = gwsURLPropNameWithTrans;
                    }
                    else
                    {
                        gwsURL = (String) commonProps.get(gwsURLPropNameDefault);
                        gwsURLPropName = gwsURLPropNameDefault;
                    }
                }
            }

            if (log.isDebugEnabled())
                log.debug("GatewayRequestHandler.getGWSURL(): propertyname [" + gwsURLPropName + "] " +
                        "is used to obtain Gateway Web-service URL.");
        }
        catch(PropertyException pe)
        {
            log.error("GatewayRequestHandler.getGWSURL(): Could not obtain Gateway web-service URL with " +
                    "propertyname [" + gwsURLPropName + "]. " + pe.getMessage());
            log.trace(pe);
            throw pe;
        }
        finally
        {
            try{
                CustomerContext.getInstance().cleanup();
            } catch (FrameworkException ignore){ }
        }

        if (StringUtils.hasValue (gwsURL)){
            if (log.isDebugEnabled())
                log.debug("GatewayRequestHandler.getGWSURL(): obtained Gateway Web-Service URL [" + gwsURL + "]");
        }
        else{
            log.error("GatewayRequestHandler.getGWSURL(): Unable to retrieve value for property [" + GWS_URL_PROP + "]");
            throw new PropertyException("GatewayProtocolAdapter.getGWSURL(): Unable to retrieve value for property [" + GWS_URL_PROP + "]");
        }

        return gwsURL;
    }

    private void reset(String serverName)
    {
            // reset requestHandler back to Router.
            if(serverName.contains(ASSUMED_ROUTER_NAME))
            {
                if (log.isDebugEnabled())
                    log.debug("GatewayRequestHandler: Resetting RequestHandler name back to[" + ASSUMED_ROUTER_NAME + "].");

                currentRouterCount = 0;
            }
    }
    /**
     * Initialize the logger provided by the GUI infrastructure.
     */
    protected static DebugLogger log =  DebugLogger.getLogger("/axis", GatewayRequestHandler.class);

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

    private List noTPRRequests = new ArrayList();

}

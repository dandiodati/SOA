/**
 * SOAPRequestHandlerSoapBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter and then edited.
 *
 */

package com.nightfire.comms.soap;
import java.rmi.*;

import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.*;
import com.nightfire.framework.debug.*;

import com.nightfire.security.common.PasswordService;
import com.nightfire.spi.common.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.monitor.ThreadMonitor;

import com.nightfire.security.*;


/**
 * Class implementing the SOAPRequestHandler wsdl to receive requests and forward them
 * to the appropriate implementation of SOAPRequestHandlerInternal interface which in turn would
 * forward the request to the appropriate gateway servers or manager instance(communication
 * with only one manager is currently supported).
 */
public class SOAPRequestHandlerSoapBindingImpl implements com.nightfire.comms.soap.SOAPRequestHandler
{
    public static final String PROCESS_SYNC = SOAPConstants.PROCESS_SYNC;
    public static final String PROCESS_ASYNC = SOAPConstants.PROCESS_ASYNC;

    public static final String MANAGER = "Manager";

    /**
     * Initialize the manager proxy class that implements SOAPRequestHandlerInternal.
     */
    private void initializeManagerClient ( ) throws FrameworkException
    {
        if (log.isDebugEnabled())
            log.debug("Manager client initialization");
        if ( managerClient == null )
        {
            synchronized( SOAPRequestHandlerSoapBindingImpl.class )
            {
                if ( managerClient == null )
                {
                    managerClient = (SOAPRequestHandlerInternal) ObjectFactory.create( ServiceInitializer.getManagerClientClassName() );
                    managerClient.initialize();
                    if (log.isDebugEnabled())
                        log.debug("Manager client initialized");
                    return;
                }
            }
        }
        if (log.isDebugEnabled())
            log.debug("Manager client already initialized");
    }

    /**
     * Initialize the gateway proxy class that implements SOAPRequestHandlerInternal.
     */
    private void initializeGatewayClient ( ) throws FrameworkException
    {
        if (log.isDebugEnabled())
            log.debug("Gateway client initialization");
        if ( gatewayClient == null )
        {
            synchronized( SOAPRequestHandlerSoapBindingImpl.class )
            {
                if ( gatewayClient == null )
                {
                    gatewayClient = (SOAPRequestHandlerInternal) ObjectFactory.create( ServiceInitializer.getGatewayClientClassName() );
                    gatewayClient.initialize();
                    if (log.isDebugEnabled())
                        log.debug("Gateway client initialized");
                    return;
                }
            }
        }
        if (log.isDebugEnabled())
            log.debug("Gateway client already initialized");

    }
    
    /**
     * Proxy method for processSync method on RequestHandlerOperations and EJB invocations.
     *
     * @param header - the Nightfire style header.
     * @param body - the message to be submitted.
     *
     * @return String - the response from the synchronous call, with
     *                  String[0] = response header and
     *                  String[1] = response body.
     *
     * @exception java.rmi.RemoteException - Should be thrown if there is a
     *                        error processing the request.
     */
    public String[] processSync( String header, String body ) throws java.rmi.RemoteException
    {
        String[] response = process (header, body, PROCESS_SYNC );

        if (log.isDebugDataEnabled())
            log.debug("processSync: Returning Header[" + response[0] + "]\n" + "Body [" + response[1] + "]");

        return response;
    }

    /**
     * Proxy method for processAsync method on RequestHandlerOperations.
     *
     * @param header  - the Nightfire style header.
     * @param body - the message to be submitted.
     *
     * @exception java.rmi.RemoteException - Should be thrown if there is a
     *                        error processing the request.
     */
    public void processAsync(String header, String body) throws java.rmi.RemoteException
    {
        process (header, body, PROCESS_ASYNC );
    }

    /**
     * This is where all the process methods converge.
     */
    private String[] process ( String header, String body, String methodName ) throws java.rmi.RemoteException
    {
        ThreadMonitor.ThreadInfo tmti = null;

        //This method just routes the message
        try
        {
            long metricsStartTime = System.currentTimeMillis( );

            if (log.isDebugEnabled())
                log.debug("Processing started");

            tmti = ThreadMonitor.start( "SOAPRequestHandlerSoapBindingImpl.process(): Processing API request with header [" + header + "]" +
                    " and method name [" + methodName + "] on [" + new java.util.Date(metricsStartTime) + "]");

            XMLMessageParser headerParser = new XMLMessageParser( header );

            SOAPRequestHandlerInternal.RequestContext context = new SOAPRequestHandlerInternal.RequestContext();
            context.headerParser = headerParser;
            context.header = header;
            context.body = body;
            context.methodName = methodName;
            context.password = getUserPassword( headerParser );

            // Removing password node/ encoding flag from request header as per SOX requirement.
            removePassword(headerParser);
            header = XMLLibraryPortabilityLayer.convertDomToString(headerParser.getDocument());

            if (log.isDebugDataEnabled())
                log.debug(methodName + ": Header [" + header + "]\n" + "Body [" + body + "]");

            setupSecurity( context );

            // after successful authentication, remove password from context
            context.header = header;
            context.password = null;
            
            //Forward the request either to the manager or the gateway as indicated by the message
            //header.
            String response[] = null;
            String requestNodeValue = getRequiredValue( headerParser, HeaderNodeNames.REQUEST_NODE );
            if ( requestNodeValue.equals( MANAGER ) )
            {
                initializeManagerClient();
                response = managerClient.process ( context );
            }
            else
            {
                initializeGatewayClient();
                response = gatewayClient.process ( context );
            }
            if (log.isDebugEnabled())
                log.debug("Processing done.");

            if ( methodName.equals(SOAPConstants.PROCESS_SYNC) &&
                 response == null )
            {
                SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT,
                "Processing returned null response for sync call which is invalid" );
            }

            MetricsAgent.logSyncAPI( metricsStartTime, ComServerBase.getHeaderMetrics( headerParser.getDocument() ) );

            return response;

        }
        catch ( MessageException e )
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.MESSAGE_FAULT,
            "Malformed data error: " + e.toString() );
        }
        catch ( RemoteException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT,
            "Processing failed: " + e.toString() );
        }
        finally
        {
            ThreadMonitor.stop( tmti );
        }

        //for compiling reasons.
        return null;
    }//process

    /**
     * Populate the context with the header information. Also authenticate the user via
     * the SecurityService.
     *
     * @param context Request information.
     */
    private void setupSecurity ( SOAPRequestHandlerInternal.RequestContext context ) throws RemoteException
    {
        try
        {
            XMLMessageParser headerParser = context.headerParser;
            context.userID = getRequiredValue( headerParser, CustomerContext.USER_ID_NODE );
            context.customerID = getRequiredValue( headerParser, CustomerContext.CUSTOMER_ID_NODE );

            //Populate customer context for the metrics logging.
            CustomerContext.getInstance().setCustomerID( context.customerID );
            CustomerContext.getInstance().setUserID( context.userID );

            //START AUTHENTICATION HOOK FOR SOAP API          
            try
            {
               SecurityService securityService = SecurityService.getInstance( context.customerID );

                //Validated user's type with API request type A.
                securityService.validateUserType(context.userID,SecurityService.REQUEST_TYPE.A);

               securityService.authenticate(context.userID, context.password );
                context.subDomainId = securityService.getSubDomainId ( context.userID );
                CustomerContext.getInstance().setSubDomainId ( context.subDomainId );
            }
            catch (Exception e)
            {
                String errorMessage = "Failed to authenticate cid ["+ context.customerID +"], user ["+ context.userID +"]: " + e.getMessage();

                log.error( errorMessage );

                if (e instanceof InvalidUserTypeException)
                    SOAPUtils.throwSOAPFaultException ( SOAPConstants.SECURITY_FAULT, "The user name and password entered are associated with the " +
                            "GUI and are not valid in the API. Please use an API login or obtain one from your domain administrator." );
                else if (e instanceof UnsuccessfulLoginAttemptExceeded)
                    SOAPUtils.throwSOAPFaultException ( SOAPConstants.SECURITY_FAULT, "The maximum number of unsuccessful login attempts has been " +
                            "reached. Please ask your domain administrator to change your password." );

                // for all other authentication exceptions.
                SOAPUtils.throwSOAPFaultException ( SOAPConstants.SECURITY_FAULT, "Invalid username/password or domain." );
            }
            //END AUTHENTICATION HOOK FOR SOAP API

        }
        catch ( FrameworkException e )
        {
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.SECURITY_FAULT,
            "Could not extract security information from header: " + e.toString() );
        }

    }

    /**
     * Get the value for the node "nodeName" located in the xml associated with the parser.
     * If the value is null or an empty string, throw an exception.
     *
     * @return String nodeName's value attribute's value.
     *
     * @throws MessageException If value is not found or is null or empty string.
     */
    private String getRequiredValue ( XMLMessageParser parser, String nodeName ) throws MessageException
    {
        String value = null;

        if ( parser.nodeExists( nodeName ) )
            value = parser.getValue( nodeName );
        else
            throw new MessageException( "Could not locate node [" + nodeName + "]." );

        if ( !StringUtils.hasValue( value ) )
            throw new MessageException( "Invalid value [" + value + "] retrieved for node [" +
            nodeName + "]." );

        return value;
    }

    private String getUserPassword ( XMLMessageParser parser ) throws MessageException
    {
        String password = null;
        boolean isPasswordEncoded = false;

        if ( parser.nodeExists( CustomerContext.USER_PASSWORD_NODE ) )
            password = parser.getValue( CustomerContext.USER_PASSWORD_NODE );
        else
            throw new MessageException( "Could not locate node [" + CustomerContext.USER_PASSWORD_NODE + "]." );

        if ( !StringUtils.hasValue( password ) )
            throw new MessageException( "Invalid value retrieved for node [" +
            CustomerContext.USER_PASSWORD_NODE + "]." );

        if ( parser.nodeExists( CustomerContext.USER_PASSWORD_ENCODING_NODE ))
            isPasswordEncoded = StringUtils.getBoolean( parser.getValue(CustomerContext.USER_PASSWORD_ENCODING_NODE), false );

        if (isPasswordEncoded)
            password = PasswordService.getInstance().decodeUsingBase64(password);

        return password;
    }

    private void removePassword ( XMLMessageParser parser )
    {
        try
        {
            if ( parser.nodeExists( CustomerContext.USER_PASSWORD_NODE ) )
                parser.removeNode( CustomerContext.USER_PASSWORD_NODE );

            if ( parser.nodeExists( CustomerContext.USER_PASSWORD_ENCODING_NODE ))
                parser.removeNode( CustomerContext.USER_PASSWORD_ENCODING_NODE );
        }
        catch (MessageException me)
        {
            Debug.warning("Ignoring exception in removing User Password/ User Password Encoding flag: " + me.getMessage());
        }
    }

    /**
     * Initialize the logger provided by the GUI infrastructure.
     */        
    protected static DebugLogger log =  DebugLogger.getLogger("/axis", SOAPRequestHandlerSoapBindingImpl.class);

    /**
     * Manager proxy object reference.
     */
    protected static SOAPRequestHandlerInternal managerClient = null;

    /**
     * Gateway proxy object reference.
     */
    protected static SOAPRequestHandlerInternal gatewayClient = null;

}//SOAPRequestHandlerSOAPBindingImpl


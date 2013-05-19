/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //spi/neustar-soa/main/com/nightfire/spi/neustar_soa/adapter/SoaGatewayProtocolAdapter.java $
 */

package com.nightfire.spi.neustar_soa.adapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.HttpSession;
import org.w3c.dom.*;
import org.omg.CORBA.StringHolder;
import org.omg.CORBA.ORB;
import com.nightfire.idl.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.rules.ErrorCollection;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.mgrcore.im.query.*;
import com.nightfire.webgui.manager.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.SessionInfoBean;
import com.nightfire.framework.debug.*;
import com.nightfire.spi.common.communications.ComServerBase;
import com.nightfire.framework.monitor.*;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.security.domain.DomainProperties;


/**
 * <p><strong>SoaGatewayProtocolAdapter</strong> is the Gateway's implementation of
 * ProtocolAdapter interface.  It is used by the GUI framework to send requests
 * to the processing layer.</p>
 */

public class SoaGatewayProtocolAdapter extends ProtocolAdapterBase
{
    private static final String OSAGENT_ADDR         = CommonConfigUtils.CORBA_SERVER_ADDR_PROP;
    private static final String OSAGENT_PORT         = CommonConfigUtils.CORBA_SERVER_PORT_PROP;
    private static final String IOR_ACCESS           = "IOR_ACCESS";
    private static final String SVC_HANDLER_ACTION   = "svcHandlerAction";
    public static final String SERVER_NAME_PREFIX = "Nightfire.SPI.Full_Neustar_SOA.Neustar_SOA_Request";
    
    public static final String SOA_VALIDATE_GATEWAY_NAME = "SOA_VALIDATE_REQUEST";

    public static final String SERVICE_HANDLER_CONFIG_CATEGORY = "SERVICE_HANDLER_CONFIG_CATEGORY";
    public static final String SERVICE_HANDLER_AUTHORIZATION_FLAG = "SERVICE_HANDLER_AUTHORIZATION_FLAG";

    private static final String GATEWAY_TYPE_NODE         = "GatewayType";
    private static final String NOTIFICATION_CHANNEL_NAME = "NOTIFICATION_CHANNEL_NAME";
    private static final String USE_EVENTCHANNEL_FLAG = "USE_EVENTCHANNEL_FOR_GUI_NOTIFICATIONS";
    private static final String DEFAULT_CHANNEL_NAME = "NightFire.UI.Notifications";
    private static final String async = "async";
    
    //Added to bypass request router and validate router gateway
    private static final String SERVER_NAMES_KEY = "SERVER_NAMES_KEY";  
    private static final String DOWN_SERVER_RETRY_TIME = "DOWN_SERVER_RETRY_TIME";
    
    //Introduced in SOA 5.6.7 release
    private static final String GET_BPEL_SPID_FLAG_QUERY = "SELECT BPELSPIDFLAG FROM SOA_NANC_SUPPORT_FLAGS WHERE SPID = ?";
    private static final String BPELSPIDFLAG = "BPELSPIDFLAG";
    private static final String INITSPID_NODE = "InitSPID";
    
    
    private String        svcHandlerAction;
    private String        osagentAddr;
    private String        osagentPort;
    private String        iorAccess;
    private boolean       notificationEnabled = false;

	//if blockGUINotification is true then GUI Notification will not be posted to Upstream Customer.
	//blockGUINotification is introduced in SOA 5.6.7 release and default value is false
	private boolean       blockGUINotification = false;
	
    private boolean       isSvcAction = false;
    private String        notificationChannelName;
    private boolean       useEventChannelForNotifications = true;
    private ORB orb;
    private ObjectLocator objectLocator;
   // Global counter indicating how many outstanding gateway requests there are.
    private static int outstandingRequestCount = 0;
    private SeqIdGenerator idGenerator;
    
   //Added to bypass request router and validate router gateway
    private String serverNamesKey = null;
    private List serverNames;
    private int serverCount;
    private int totalServerCount;
    private volatile int roundRobinCounter;
    private long globalStartTime;
    private long downServerRetryTime;
    
    public void init(Properties servletInitParams, ServletContext context) throws ServletException
    {
        String webAppName = ServletUtils.getWebAppContextPath(context);
        log = DebugLogger.getLogger(webAppName, getClass());
        
        //Added to bypass request router and validate router gateway
        serverNamesKey = servletInitParams.getProperty( SERVER_NAMES_KEY);
        
        downServerRetryTime =  Long.parseLong(servletInitParams.getProperty( DOWN_SERVER_RETRY_TIME ));
        
       	globalStartTime = System.currentTimeMillis( );
       	        
		serverNames = getServerNames(serverNamesKey);
		// check to see that at least one server name was discovered in the
		// properties
		if (serverNames.size() == 0) {
			throw new ServletException("No SERVER_NAME"
					+ " properties were found under property key ["
					+ serverNamesKey + "]");
		}
		serverCount = serverNames.size();
		totalServerCount = serverCount;
		roundRobinCounter = 0;
		
        osagentAddr        = servletInitParams.getProperty(OSAGENT_ADDR);

        osagentPort        = servletInitParams.getProperty(OSAGENT_PORT);

        iorAccess          = servletInitParams.getProperty(IOR_ACCESS);

        im_config = servletInitParams.getProperty(SERVICE_HANDLER_CONFIG_CATEGORY);

        String authReq = servletInitParams.getProperty(SERVICE_HANDLER_AUTHORIZATION_FLAG);

        try
        {
           idGenerator = new SeqIdGenerator();
        }
        catch(Exception e)
        {
           throw new ServletException("Unable to intiliaze unique id generation:"+e.getMessage());
        }

        if (StringUtils.hasValue(authReq))
        {
            try
            {
                auth = StringUtils.getBoolean(authReq);
            }
            catch (FrameworkException fe)
            {
                // Use default.
            }
        }


        if (log.isDebugEnabled())
        {
            log.debug("SoaGatewayProtocolAdapter(): Servlet initialization parameter [" + OSAGENT_ADDR + "] has value [" + osagentAddr + "].");

            log.debug("SoaGatewayProtocolAdapter(): Servlet initialization parameter [" + OSAGENT_PORT + "] has value [" + osagentPort + "].");

            log.debug("SoaGatewayProtocolAdapter(): Servlet initialization parameter [" + IOR_ACCESS + "] has value [" + iorAccess + "].");

            log.debug("SoaGatewayProtocolAdapter(): Servlet initialization parameter [" + IOR_ACCESS + "] has value [" + im_config + "].");
            log.debug("SoaGatewayProtocolAdapter(): Servlet initialization parameter [" + IOR_ACCESS + "] has value [" + auth + "].");
        }

        if (!StringUtils.hasValue(osagentAddr) ||!StringUtils.hasValue(osagentPort))
        {
            String errorMessage = "ERROR: SoaGatewayProtocolAdapter.SoaGatewayProtocolAdapter(): Servlet initialization parameters [" +
            OSAGENT_ADDR + ", " +  OSAGENT_PORT + ", " + IOR_ACCESS +
            "] must exist and have valid values.";

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }

        if (!StringUtils.hasValue(iorAccess))
        {
            iorAccess = ObjectLocator.IOR_ACCESS_NS;
        }
        else
        {
            // ObjectLocator expects the ior-access value to be in upper-case.

            iorAccess = iorAccess.toUpperCase();
        }

        try
        {
            Properties orbProperties = new Properties();

            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_ADDR_PROP, osagentAddr);

            orbProperties.put(CorbaPortabilityLayer.ORB_AGENT_PORT_PROP, osagentPort);

            CorbaPortabilityLayer cpl = new CorbaPortabilityLayer(null, orbProperties);
            
            log.debug("SoaGatewayProtocolAdapter(): Successfully created a CorbaPortabilityLayer object.");

            orb = cpl.getORB();
            objectLocator = new ObjectLocator(orb);

            log.debug("SoaGatewayProtocolAdapter(): Successfully created an ObjectLocator object.");
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: SoaGatewayProtocolAdapter.SoaGatewayProtocolAdapter(): Failed to initialize supporting Corba objects:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
        /**
         * Get data from the application context properties.
         */
        Properties contextProps = (Properties ) context.getAttribute ( ServletConstants.CONTEXT_PARAMS ) ;

        notificationChannelName = contextProps.getProperty(NOTIFICATION_CHANNEL_NAME);
        String str = contextProps.getProperty(USE_EVENTCHANNEL_FLAG);
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
            log.debug("SoaGatewayProtocolAdapter(): Application parameter [" + NOTIFICATION_CHANNEL_NAME + "] has value [" + notificationChannelName + "].");
            log.debug("SoaGatewayProtocolAdapter(): Application parameter [" + USE_EVENTCHANNEL_FLAG + "] has value [" + useEventChannelForNotifications + "].");
        }

        if ( !StringUtils.hasValue( notificationChannelName ) )
        {
            notificationChannelName = DEFAULT_CHANNEL_NAME;
        }
        if (log.isDebugEnabled())
            log.debug("SoaGatewayProtocolAdapter(): Value to be used for [" + NOTIFICATION_CHANNEL_NAME + "] is [" + notificationChannelName + "].");

    }
	/**
     * This method sends a request to the processing layer.
     *
     * @param  requestData  Request DataHolder.
     * @param  session  HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  Response DataHolder returned from the processing layer.
     */
    public DataHolder sendRequest(DataHolder requestData, HttpSession session) throws ServletException
    {

        //First decide what type of request it is: service handler or gateway request.
        SessionInfoBean sessionBean = (SessionInfoBean) session.getAttribute(ServletConstants.SESSION_BEAN);

        String cid = sessionBean.getCustomerId();
        String uid = sessionBean.getUserId();
        XMLGenerator headerParser;
        String action;

        try
        {
            headerParser = new XMLPlainGenerator(requestData.getHeaderDom());

            action       = headerParser.getValue(PlatformConstants.ACTION_NODE);

            if (headerParser.exists(SVC_HANDLER_ACTION))
                svcHandlerAction = headerParser.getValue(SVC_HANDLER_ACTION);
            else
                svcHandlerAction = "";

        }
        catch (Exception e)
        {
            String errorMessage = "Failed to parse the constructed request header for action value:\n" + e.getMessage();

            log.error("sendRequest(): " + errorMessage);

            throw new ServletException(errorMessage);
        }

        ServletContext context  = session.getServletContext();

        List svcHanderActionlookup = (List)context.getAttribute(ManagerServletConstants.SVC_HANDLER_ACTIONS);

        boolean svcHandlerRequest = StringUtils.getBoolean(svcHandlerAction, false);
        if (!svcHandlerRequest && (svcHanderActionlookup != null) && svcHanderActionlookup.contains(action))
        {
            svcHandlerRequest = true;
        }

        //Format request header to remove action node if it a gateway request
        //and to remove the gatewaytype node for any type of request.
        //And also add customer information to the header.

        String requestType = async;
        try
        {

            if ( headerParser.exists( GATEWAY_TYPE_NODE ) )
            {
                requestType = headerParser.getValue( GATEWAY_TYPE_NODE );
                headerParser.remove( GATEWAY_TYPE_NODE );
            }

            String msgId = null;
            if ( headerParser.exists( CustomerContext.MESSAGE_ID ) )
            {
                msgId = headerParser.getValue(CustomerContext.MESSAGE_ID);
                if ( log.isDebugEnabled() )
                    log.debug("Adding unique message id to header:"+msgId);

                CustomerContext.getInstance().setMessageId(msgId);
            }else
            {
                msgId = idGenerator.getStrNextId();
                if ( log.isDebugEnabled() )
                    log.debug("Adding unique message id to header:"+msgId);

                CustomerContext.getInstance().setMessageId(msgId);
            }


            if ( !svcHandlerRequest ) {

                if ( headerParser.exists( PlatformConstants.ACTION_NODE )) {

                    //send request to Synchrnous gateway if action is validate, save, suspend or resume
                        if (headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.VALIDATE_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.SAVE_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.SUSPEND_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.RESUME_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.ABANDON_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.CANCEL_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.TEMPLATE_SAVE_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.TEMPLATE_EDIT_REQUEST) ||
                                    headerParser.getValue(PlatformConstants.ACTION_NODE).equals(PlatformConstants.TEMPLATE_DELETE_REQUEST)) {

                        requestType = "sync";

                    } else {

                        //remove action node if action type is other than save and validate
                        headerParser.remove( PlatformConstants.ACTION_NODE );
                    }

                }
            }

            isSvcAction = svcHandlerRequest;
            Document doc = headerParser.getDocument();
            addCustomerInfo( doc );

            //We can set the information back on the DataHolder object because on a back button
            //operation, we are accessing an original copy of the message.
            requestData.setHeader ( doc );
        }
        catch ( FrameworkException e )
        {
            throw new ServletException( "Could not manipulate request header before sending out: " +e );
        }

        //Now actually route the request.
        if (svcHandlerRequest)
        {
            if (log.isDebugEnabled())
                log.debug ( "Sending request to service handler associated with action [" + action + "]." );
            return sendSvcHandlerRequest(action, requestData, cid, uid );
        }
        else
        {
            if (log.isDebugEnabled())
                log.debug ( "Sending request to request handler of type [" + requestType + "]."  );

            return send( action, requestData, cid, uid, requestType );
 
        }
    }//sendRequest
    
    /**
     * Sends the given header and message to the next server. If the
     * server is not available, each of the other configured servers
     * will be tried until an available server is found. If all known
     * servers are down, then a ServletException will be thrown.
     *
     * @param action             name of action
     * @param requestData        data holder object
     * @param cid                customer id
     * @param uid                user id
     * @param requestType        sync or async
     * @return Response          object returned from the service handler.
     * @throws ServletException  Thrown when an error occurs during processing.
     */
    public DataHolder send( String action, DataHolder requestData, String cid, String uid, String requestType )
    throws ServletException
    {
		if ((serverCount < totalServerCount)&& (System.currentTimeMillis() - globalStartTime > downServerRetryTime)) {
			log.debug("ServerCount:" + serverCount);				
			log.debug("TotalServerCount :"+ totalServerCount);
			synchronized (SoaGatewayProtocolAdapter.class) {
				globalStartTime = System.currentTimeMillis();
				serverNames.clear();
				serverNames = getServerNames(serverNamesKey);
				if (serverNames.size() == 0) {
					throw new ServletException("No SERVER_NAME"
							+ " properties were found under property key ["
							+ serverNamesKey + "]" );
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
				if (Debug.isLevelEnabled(Debug.IO_STATUS)) {
					Debug.log(Debug.IO_STATUS,"Attempting to locate server number ["+ serverIndex + "] named [" + serverName+ "]");
				}
				try {
					org.omg.CORBA.Object corbaObject = objectLocator.find(serverName, iorAccess);
					
					RequestHandler requestHandler = RequestHandlerHelper.narrow(corbaObject);
					
					return sendRequestHandlerRequest(action, requestData, cid,uid, requestType, requestHandler);
					
				} catch (Exception ohNo) {
					if (ohNo.getMessage().contains(	"Could not resolve CORBA name")
							|| ohNo.getMessage().contains("Retries exceeded, couldn't reconnect to")) {
					        synchronized (SoaGatewayProtocolAdapter.class) {
							Debug.error("Could not deliver request to server ["	+ serverName + "]: ");
							objectLocator.removeFromCache(serverName);
							serverNames.remove(serverName);
							serverCount = serverNames.size();
							roundRobinCounter = 0;
							count--;
						}
					}else
						throw new ServletException(ohNo.toString() );
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
			throw new ServletException("None of the servers were available to service the request: ");
		}
        catch ( Exception e )
        {
        	throw new ServletException(e.toString() );
        }		

	}



    /**
     * Process the request via the gateway route.
     *
     * @param action             name of action
     * @param requestData        data holder object
     * @param cid                customer id
     * @param uid                user id
     * @param requestType        sync or async
     * @param requestHandler     this will be deciding factor to send corba call
     * @return Response          object returned from the service handler.
     * @throws ServletException  Thrown when an error occurs during processing.
     */
    protected DataHolder sendRequestHandlerRequest( String action, DataHolder requestData, String cid, String uid, String requestType,RequestHandler requestHandler)
    throws ServletException
    {
        long startTime = 0;

        if(log.isBenchmarkEnabled())
            startTime = System.currentTimeMillis();

        long metricsStartTime = System.currentTimeMillis( );

        String outcome = MetricsAgent.FAIL_STATUS;

        DataHolder responseData = null;

        ThreadMonitor.ThreadInfo tmti = ThreadMonitor.start( "Gateway request with header\n: " + requestData.getHeaderStr() );

        try {

            outstandingRequestCount ++;
            if (requestType.equals(async))
            {
                log.debug("sendRequestHandlerRequest(): Executing RequestHandler's processAsync() ...");

                requestHandler.processAsync( requestData.getHeaderStr(), requestData.getBodyStr());

                responseData = new DataHolder();
            }
            else if (requestType.equals("sync"))
            {
                StringHolder responseBody = new StringHolder();
                
                log.debug("sendRequestHandlerRequest(): Executing RequestHandler's processSync()...");
                requestHandler.processSync( requestData.getHeaderStr(), requestData.getBodyStr(), responseBody);
  
                responseData = new DataHolder(null, responseBody.value);
            }
            else if (requestType.equals("syncWithHeader"))
            {
                log.debug("sendRequestHandlerRequest(): Executing RequestHandler's processSynchronous() ...");

                StringHolder responseHeader = new StringHolder();
                StringHolder responseBody   = new StringHolder();

                requestHandler.processSynchronous( requestData.getHeaderStr(),
                requestData.getBodyStr(), responseHeader, responseBody);

                responseData = new DataHolder(responseHeader.value, responseBody.value);
            }
            else
            {
                String errorMessage = "ERROR: SoaGatewayProtocolAdapter.sendRequestHandlerRequest(): Failed to send the request, unrecognizable request type [" + requestType + "].";

                log.error(errorMessage);

                throw new ServletException(errorMessage);
            }

            outcome = MetricsAgent.PASS_STATUS;
        }
        catch (InvalidDataException e)
        {
            // corba exceptions don't show up in the getMessage(), so we need to do a to string
            log.error("sendRequestHandlerRequest(): An InvalidDataException occured while sending the request:\n" + e.toString());
            return handleError(e, e.errorMessage, requestData);
        }
        catch (CorbaServerException e) 
        {
            // corba exceptions don't show up in the getMessage(), so we need to do a to string
            log.error("A ProcessingException occured while sending the request:\n" + e.toString());

            try
            {
                XMLPlainGenerator gen = new XMLPlainGenerator(PlatformConstants.BODY_NODE);
                gen.setValue(ServletConstants.ERR_BODY_DOC_NAME, e.errorMessage);

                return formatErrRespCode(requestData, gen.getOutput(), ServletConstants.PROC_EXCEPTION_RESPONSE_CODE);

            }
            catch (Exception ex)
            {
                log.error("Failed to send request: " + ex.getMessage(), ex );
                // since we getMessage does not work on these exceptions
                // we return the error message on the corba error
                throw new ServletException("Failed to send request: " + ex.getMessage());

            }
        }
        catch (ServletException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String errorMessage = "Failed to send the request:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
        finally
        {	
        	ThreadMonitor.stop( tmti );
            if (log.isBenchmarkEnabled())
            {
                log.benchmark("sendRequestHandlerRequest(): ELAPSED TIME is [" + (System.currentTimeMillis() - startTime) + "] msec for processing request.");
            }

            outstandingRequestCount --;

            try
            {
                CustomerContext cc = CustomerContext.getInstance( );

                cc.setCustomerID( cid );
                cc.setUserID( uid );

                if ( MetricsAgent.isOn( MetricsAgent.SYNC_API_CATEGORY ) )
                    MetricsAgent.logSyncAPI( metricsStartTime, action + "." + 
                                             ComServerBase.getHeaderMetrics(requestData.getHeaderStr()) + "." + outcome 
                                             + ",outstanding-request-handlers=[" + outstandingRequestCount + "]"
                                             + ",session-count=[" + SessionManager.getSessionCount() + "]," 
                                             + QueryEngine.getConnectionUsage() );

                notificationEnabled = DomainProperties.getInstance ().isNotificationEnabled ();

                if (log.isDebugEnabled())
                    log.debug("SoaGatewayProtocolAdapter(): Notification enabled?[" + notificationEnabled + "]." );

            }
            catch ( Exception e )
            {
                log.warn( e.getMessage() );
            }
            finally
            {
                try
                {
                    CustomerContext.getInstance().cleanup( );
                }
                catch ( Exception e )
                {
                    log.warn( e.getMessage() );
                }
            }

           
        }
        
        //Blocking the posting of GUI Notification to upstream customer for SVQueryRequest for Customer 
        //which have BPELSPIDFLAG value is 1.
        blockGUINotification = false;
        try
        {
        	XMLMessageParser xmp = new XMLMessageParser(requestData.getBodyStr());
        	
        	if(xmp.exists(SOAConstants.REQUEST_BODY_PATH + "." + SOAConstants.SV_QUERY_REQUEST)){
        	
	        	String initSpid = null;
	        	
	        	if(xmp.exists(SOAConstants.REQUEST_HEADER_PATH +"."+INITSPID_NODE )){
	        		//get the initSPID value from Request body xml.
	        		initSpid = xmp.getValue(SOAConstants.REQUEST_HEADER_PATH +"."+INITSPID_NODE );
	        		if(log.isDebugEnabled())
	        			log.debug("SoaGatewayProtocolAdapter(): InitSPID value is ["+initSpid+"]");
	        	}
	        	
	        	Connection dbConn = null;
	        	PreparedStatement  pstmt = null;
	        	ResultSet rs = null;
	        	String bpelSpidFlagVal = null;
	        	try{
		        	dbConn = DBConnectionPool.getInstance().acquireConnection();
					
					if (dbConn == null) {
						// Throw the exception to the driver.
						throw new FrameworkException("SoaGatewayProtocolAdapter:" + " DB connection is not available");
					}
					
					pstmt = dbConn.prepareStatement(GET_BPEL_SPID_FLAG_QUERY);
					if(log.isDebugEnabled())
	        			log.debug("SoaGatewayProtocolAdapter: Query to fetch the BPELSPIDFLAG value" +
	        					" ["+GET_BPEL_SPID_FLAG_QUERY+"]");
					
					pstmt.setString(1, initSpid);
					
					rs = pstmt.executeQuery();
					
					if (rs.next()) {
						bpelSpidFlagVal = rs.getString(BPELSPIDFLAG);
					}
					if(log.isDebugEnabled())
	        			log.debug("SoaGatewayProtocolAdapter: BPELSPIDFLAG value fetched from DB" +
	        					" ["+bpelSpidFlagVal+"]");
					
					if("1".equals(bpelSpidFlagVal)){
						if(log.isDebugEnabled())
		        			log.debug("SoaGatewayProtocolAdapter: SPID belongs to the BPEL customer and submitted request is SvQueryRequest," +
		        					" so do not post the GUI Notification to the UpStream Customer.");
						blockGUINotification = true;
					}else{
						blockGUINotification = false;
					}
					
	        	}catch(Exception e){
	        		log.error("SoaGatewayProtocolAdapter: Error while fetching the value of BPELSPIDFLAG value from " +
	        				"Database: " +e.getMessage());
	        	}finally{
	        		
	        		try {
	    				if (rs != null) {
	    					rs.close();
	    				}
	    			} catch (Exception e) {
	    				log.error("SoaGatewayProtocolAdapter: Error while closing the result set resources");
	    			}
	        		try{
	    				if (pstmt != null) {
	    					pstmt.close();
	    				}
	    			} catch (Exception e) {
	    				log.error("SoaGatewayProtocolAdapter: Error while closing the prepared statement resources");
	    			}
	    			try{
	    				if (dbConn != null) {

	    					log.debug("SoaGatewayProtocolAdapter: Releasing the accquired connection");
	    					DBConnectionPool.getInstance().releaseConnection(dbConn);
	    				}
	    			} catch (Exception e) {
	    				log.error("SoaGatewayProtocolAdapter: Error while closing the data base resources");
	    			}
	        		
	        	}
				
        	}
        	if(log.isDebugEnabled())
        		log.debug("SoaGatewayProtocolAdapter(): Value for blockGUINotification is " +
        				"["+blockGUINotification+"]");
        	
        	//If we reach here, it means that the send operation was successful. So we need
            //to check if a notification has to be sent to the upstream system.
            // Bypassing GUI notifications if in service group def the UOMSppurt is true.
        	
        	if (notificationEnabled && !blockGUINotification){
        		if(log.isDebugEnabled())
        			log.debug("Sending GUI Notification to Upstream customer");
        		
                new ExternalNotifier().notify( notificationChannelName, requestData, responseData,
                cid, uid, log, orb, objectLocator, useEventChannelForNotifications);
        	}    
                
        }
        catch ( Exception e )
        {
        	if (log.isDebugEnabled())
                log.debug("SoaGatewayProtocolAdapter(): Exception occured while sending notification to channel [" + notificationChannelName + "]." );
            throw new ServletException( "Could not send GUI Notification to event channel" );
        }
        return responseData;

    }//sendRequestHandlerRequest

    /**
     * handles/formats the error to throw biz rule
     *
     * @param e            Exception object
     * @param msg          error xml
     * @param requestData  data in xml
     * @return             DataHolder object
     * @throws ServletException on error
     */
    private DataHolder handleError(Throwable e, String msg, DataHolder requestData)
        throws ServletException
    {
        if(msg != null && msg.indexOf(ErrorCollection.ROOT) > -1 && msg.indexOf(ErrorCollection.CONTAINER) > -1)
        {
            String ERROR_END = "</Errors>";

            int endIdx = (msg.indexOf(ERROR_END) > -1) ? msg.indexOf(ERROR_END) + ERROR_END.length() : msg.length();

            msg = msg.substring(msg.indexOf("<" + ErrorCollection.ROOT), endIdx);

            if(log.isDebugDataEnabled())
                log.debugData("Got business rule errors: " + msg);

            return formatErrRespCode(requestData, msg);
        } else
        {
            log.error("Failed to send request: " +msg, e);
            throw new ServletException("Failed to send request: " + msg);
        }
    }

    /**
     * Add the customer information to the header.
     *
     * @param header Document obj
     * @return DOM object
     * @throws FrameworkException on error
     */
    protected Document addCustomerInfo ( Document header ) throws FrameworkException
    {
        CustomerContext cc = null;
        try
        {
            cc = CustomerContext.getInstance ();
            return( cc.propagate( header ) );
        }
        finally
        {
            // Reset customer context.
            try
            {
                String subDomainId = null;
                if (isSvcAction) subDomainId = cc.getSubDomainId ();
                cc.cleanup( );
                if (isSvcAction) cc.setSubDomainId ( subDomainId );
            }
            catch ( Exception e )
            {
                log.error( e.getMessage() );
            }
        }
    }
    //Added function to bypass request router and validate router gateway
    /**
    *
    * @param key String
    * @param log DebugLogger
    * @return List
    */
    public static List<String> getServerNames( String key){

	      List<String> serverNames = new ArrayList<String>();
	      
	      if(StringUtils.hasValue(key)){
	    	  
	    	  StringTokenizer serverNameTokens = new StringTokenizer(key,",");
		      
		      while(serverNameTokens.hasMoreTokens()){
		    	  
		    	  String tokenVal = serverNameTokens.nextToken().trim();
		    	  
		    	  if(StringUtils.hasValue(tokenVal) && tokenVal.length() >= 20){
		    		  
		    		  if (tokenVal.substring(0, 20).equals(SOA_VALIDATE_GATEWAY_NAME)){
		    		  
			    		  if(tokenVal.length() > 20){
			    		  
			    			  serverNames.add(SERVER_NAME_PREFIX+tokenVal.substring(20));
			    		  
			    		  }else if(tokenVal.length() == 20){
			    		  
			    			  serverNames.add(SERVER_NAME_PREFIX);
			    		  
			    		  }
		    		  }else{
		    			  if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
		    				  Debug.log(Debug.ALL_ERRORS, "Inside getServerNames(): Init param SERVER_NAMES_KEY is not configured with proper " +
				    			  "Validate gateway instance name and value is ["+tokenVal+"]");
		    			  }
		    		  }
		    	  }else{
		    		
			    	  if(Debug.isLevelEnabled(Debug.ALL_ERRORS)){
			    		  Debug.log(Debug.ALL_ERRORS, "Inside getServerNames(): Init param SERVER_NAMES_KEY is not configured with proper " +
				    			  "Validate gateway instance name and value is ["+tokenVal+"]");
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
   private int getNextIndex() throws ServletException {

		try {
			int index = roundRobinCounter % serverCount;

			roundRobinCounter++;
			roundRobinCounter %= serverCount;

			return index;
		} catch (Exception ex) {
			throw new ServletException("None of the servers were available to service the request:");
		}
	}


}
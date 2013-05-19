package com.nightfire.comms.handlers;

import java.util.LinkedList;
import java.util.Properties;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.debug.DebugConfigurator;
import com.nightfire.framework.debug.DebugLogger;
import com.nightfire.framework.message.*;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.common.*;
import com.nightfire.comms.servicemgr.helper.MessageProcessorExecutor;
import com.nightfire.comms.soap.GWSRequestControlHelper;
import com.nightfire.spi.common.communications.ComServerBase;
import com.nightfire.spi.common.driver.*;


/**
 * Implemenation of the gateway web service interface.
 */
public abstract class RequestHandlerBase 
{

    /**
     * driver key to execute the MessageProcessingChain 
     */
    private String driverKey;

    /**
     * driver type to execute the MessageProcessingChain 
     */
    private String driverType;
    
    /**
     * instance id representing the GWS instance that is processing the message
     */
    private String instanceId;

	/**
     * abstract method which initializes the driverKey and driverType.  
     * @param header information of CH header
     * @throws ProcessingException when driver key and type is found null.
     */
    protected abstract void initializeDriverKeyType(String header, String request) throws ProcessingException;
    
    /**
     * This method check the status of gateway. If gateway is down then it should throw ProcessingException else not.  
     * @throws ProcessingException when gateway is down.
     */
    
    public  void  checkGatewayStatus()throws ProcessingException 
    {
    	

	    GWSRequestControlHelper helper = new GWSRequestControlHelper(getDriverKey(),getDriverType());
		    if(helper.isGatewayDown())
		    {
		    	Debug.log( Debug.NORMAL_STATUS, "Since gateway of key ["+getDriverKey()+"] and type ["+getDriverType()+"] down so throwing the ProcessingException");
		    	throw new ProcessingException(helper.getMessage());
		    }
		    else
		    {
		    	Debug.log( Debug.NORMAL_STATUS, "Gateway is up so allowing the message processing");
		    }
    }


    /**
     * manages the request handler processing. Sub classes must provide theire own
     * implementation of manage accordingly.
     * 
     * @param task String
     * @return String
     * @throws ProcessingException on error
     */
    public String manage ( String task ) throws ProcessingException
    {
        throw new ProcessingException("Manage method is not currently supported for this request type.");
    }

    /**
     * Type used to return header and message as a unit.
     */
    public class HeaderMessageType 
    {
        private String header;
        private String message;

        public HeaderMessageType ( String header, String message ) 
        {
            this.header = header;
            this.message = message;
        }

        public String getHeader() {
            return header;
        }

        public String getMessage() {
            return message;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }


    /**
     * executes the message processor chain asynchronously. 
     * 
     * @param header information of CH header
     * @param request information of CH request (body)
     * @throws MessageException on error
     * @throws ProcessingException on error
     */
    public void processAsync ( String header, String request ) throws MessageException, ProcessingException
    {
        HeaderMessageType ro = process( header, request );

        if ( ro.message != null )
            Debug.warning( "Asynchronous Process call returned the following data: [" + ro.message + "]");
    }


    /**
     * synchronously process the message processor chain.
     * 
     * @param header information of CH header
     * @param request information of CH request (body)
     * @return response and response header in HeaderMessageType.
     * @throws MessageException on error
     * @throws ProcessingException on error
     */
    public String processSync ( String header, String request ) throws MessageException, ProcessingException
    {
        HeaderMessageType ro = process( header, request );

        return( ro.message );
    }

    /**
     * process the request by processing message processor chain.
     * 
     * @param header information of CH header
     * @param request information of CH request (body)
     * @return HeaderMessageType object containing transformed request and header
     * @throws MessageException on error
     * @throws ProcessingException on error
     */
    protected HeaderMessageType process ( String header, String request ) throws MessageException, ProcessingException
    {
        ThreadMonitor.ThreadInfo tmti = null;
        long metricsStartTime = System.currentTimeMillis( );
        String outcome = MetricsAgent.FAIL_STATUS;
        
        // get the driverKey and driverType 
        initializeDriverKeyType(header, request );
        //Check the status of gateway of driverKey and driverType 	   
 	    checkGatewayStatus();
        tmti = ThreadMonitor.start("Executing Message Processor Driver with DriverKey["+getDriverKey()+"]" +
                " and DriverType["+getDriverType()+"] for request header["+header+"]");
        
        // configure the custom log for this request.

        // Configure logger name with '/' prefix.
        // Based on '/' prefix, specific loggers (like web application and GWS loggers) 
        // need to be filtered to render on JManage GUI's 'Log4jConfiguration' page.
        // Hence, GWS loggers need to be configured with '/' prefix.
        
        if(StringUtils.hasValue(getDriverKey()))
            configureLogger("/"+getDriverKey());

        // pre-process and transform the the header and request.
        HeaderMessageType data = preProcess( header, request);

        header = data.getHeader();
        request = data.getMessage();

        try
        {
            MessageProcessingDriver driver;

            if(StringUtils.hasValue(header))
            {
                // propagate the headers to both customer and message 
                // processor context.
                CustomerContext.getInstance().propagate( header );
                String val = null;
                try {
                    CustomerContext cc = CustomerContext.getInstance();
                    val = cc.getCustomerID();

                    if (StringUtils.hasValue ( cc.getUserID () ))
                       val += ":"+cc.getUserID ();

                    if (StringUtils.hasValue ( cc.getSubDomainId (), true ))
                       val += ":"+cc.getSubDomainId ();

                    if (StringUtils.hasValue ( cc.getUniqueIdentifier ()) )
                       val += ":"+cc.getUniqueIdentifier ();

                    DebugLogger.setDiagnosticInfo(DebugConfigurator.CID_KEY, val);
                }
                catch (FrameworkException e) {
                    Debug.log( Debug.ALL_WARNINGS, " exception occurred while setting debug logging diagnostic information as <CUSTOMERID>:<USERID>.");
                }
                driver = new MessageProcessingDriver( header );
            }
            else
            {
                driver = new MessageProcessingDriver();
            }

            if (Debug.isLevelEnabled(Debug.MSG_DATA))
            {
                Debug.log( Debug.MSG_DATA, "process request header is: [" + header  + "]");
                Debug.log( Debug.MSG_DATA, "process request body is: [" + request  + "]");
            }

            // Initialize the gateway driver flow and execute it.
            driver.initialize( driverKey, driverType );

            MessageProcessorContext context = driver.getContext();

            if(StringUtils.hasValue(instanceId))
            {
            	/* propagate to MPC that shall eventually land in JMS Message Header */
            	context.set(MessageProcessorExecutor.PROCESSED_BY_GWS, instanceId);
            }
            
            // process the request.
            Object result = driver.process( request );

            String response = null;

            // Responses will always be strings.
            if ( result != null )
                response = Converter.getString( result );

            String respHeader = null;

            // Look for a response header.  If not found, return the request header from the context in case it was modified.
            if (context.exists(MessageProcessingDriver.CONTEXT_RESPONSE_HEADER_NAME) )
                respHeader = context.getString(MessageProcessingDriver.CONTEXT_RESPONSE_HEADER_NAME);
            else if (context.exists(MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME) )
                respHeader = context.getString(MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME);

            if (Debug.isLevelEnabled(Debug.MSG_DATA))
            {
                Debug.log( Debug.MSG_DATA, "process response header is: [" + respHeader + "]");
                Debug.log( Debug.MSG_DATA, "process response body is: [" + response + "]");
            }

            // process the response header and response.
            HeaderMessageType responseData = postProcess(respHeader, response);
            respHeader = responseData.getHeader();
            response = responseData.getMessage();
            
            outcome = MetricsAgent.PASS_STATUS;
            
            return( new HeaderMessageType( respHeader, response ) );
        }
        catch ( MessageException me )
        {
            Debug.warning( me.toString() );
            
/* 
  			String errMsg = me.getMessage( );         
    		throw new RequestHandlerBaseException(errMsg
                , new RequestHandlerBaseExceptionType(RequestHandlerBaseExceptionType._InvalidDataError)
                , header, errMsg);
*/
            throw me;    
        }
        catch ( Exception e )
        {
            Debug.error( e.toString() );
            Debug.log( Debug.ALL_ERRORS, "Exception occured :"+Debug.getStackTrace(e));
            throw new ProcessingException( e );
        }
        finally
        {
            // Log processing time metric.
            if ( MetricsAgent.isOn( MetricsAgent.GATEWAY_CATEGORY ) )
                MetricsAgent.logGateway( metricsStartTime, driverKey, driverType + "," 
                                         + ComServerBase.getHeaderMetrics(header) + "." + outcome );

            // Reset customer context.
            try
            {
                CustomerContext.getInstance().cleanup( );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );
            }
            
            // stop monitoring the thread.
            ThreadMonitor.stop(tmti);
        }
    }

    /**
     * transforms the exception message and customize message by applying map.
     * 
     * @param errMsg errorMessage 
     * @return transformed error message
     * @throws MessageException on error
     */
    protected String transformExceptionMessage(String errMsg) throws MessageException
    {
        return errMsg;
    }

    /**
     * process the response header and response to customized format.
     * 
     * @param respHeader header after getting response
     * @param response body after getting response
     * @return HeaderMessageType object containing transformed response and header
     * @throws MessageException on error
     */
    protected HeaderMessageType postProcess(String respHeader, String response) throws MessageException 
    {
        return new HeaderMessageType(respHeader,response);
    }

    /**
     * process the request header and response to customized format.
     * 
     * @param header information of CH header
     * @param request information of CH request (body)
     * @return HeaderMessageType object containing transformed request and header
     * @throws MessageException on error
     */
    protected HeaderMessageType preProcess(String header, String request) throws MessageException
    {
        return new HeaderMessageType(header,request);
    }

    /**
     * @return returns the configured driver key. 
     */
    public String getDriverKey() {
        return driverKey;
    }


    /**
     * sets the driver key.
     * @param driverKey as String
     */
    public void setDriverKey(String driverKey) {
        this.driverKey = driverKey;
    }


    /**
     * @return returns the configured driver type. 
     */
    public String getDriverType() {
        return driverType;
    }

    /**
     * sets the driver type.
     * @param driverType as String
     */
    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }
    
    /**
     * get the instance id
     * @return String
     */
    public String getInstanceId() {
		return instanceId;
	}

    /**
     * set the instance id
     * @param instanceId
     */
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
  
    /**
     * maintains a list of loggers which were already configured.
     */
    private static LinkedList<String> loggers = new LinkedList<String>();

    /**
     * returns true if the logger identified by driverKey is already configured.
     * @param driverKey
     * @return
     */
    private static boolean isLoggerConfigured(String driverKey)
    {
        return loggers.contains(driverKey);
    }
    
    /**
     * This method configures the logger for the given driverKey. It creates a logs in 
     * /logs/[driverkey].log. Also, the log level and max file backup can be set in common properties 
     * with property name set as driver key name.
     * 
     * @param driverKey
     * @throws FrameworkException
     */
    private void configureLogger (String driverKey )
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, 
                    "WSRequestHandler: configureLogger ["+driverKey+"]");

        // if already configured return
        if( isLoggerConfigured(driverKey) )
        {
            if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, 
                        "MessageProcessorExecutor: configureLogger driver already configured.. ");
            
            DebugLogger.getLogger(driverKey, this.getClass());
            return;
        }

        loggers.add(driverKey);

        driverKey = driverKey.trim();
        
        // Removing '/' from driver key
        String orgDriverKey = driverKey.substring(driverKey.indexOf("/") + 1);
        	
        String logFileName = orgDriverKey;
       
        Debug.log(Debug.NORMAL_STATUS, "Redirecting Logs to ./logs/"+logFileName+".log");
        
        Properties props = new Properties();
        
        String logLevelProperty = orgDriverKey.toLowerCase();

        // default property to all log level.
        String debugLogLevel = "ALL";
        String logFileCount = "5";
        String maxDebugWrites = "100000";
        Properties initProps=MultiJVMPropUtils.getInitParameters();
        
        try
        {
        	logFileName = PersistentProperty.get(COMMON_PROPERTIES,logLevelProperty, Debug.LOG_FILE_NAME_PROP);
            props.put(Debug.LOG_FILE_NAME_PROP,"./logs/"+logFileName);
        }
        catch(Exception e)
        {
            // IGNORE
            Debug.log(Debug.MSG_STATUS, "Could not configure LOG_FILE to configured logLevelProperty["+logLevelProperty+"], configuring it to ["+logFileName+"]");
            props.put(Debug.LOG_FILE_NAME_PROP,"./logs/"+logFileName+".log");
        }
        try
        {
            debugLogLevel = PersistentProperty.get(COMMON_PROPERTIES,logLevelProperty, Debug.DEBUG_LOG_LEVELS_PROP);
        }
        catch(Exception e)
        {
            // IGNORE
            Debug.log(Debug.MSG_STATUS, "Could not configure DEBUG_LOG_LEVELS to configured logLevelProperty["+logLevelProperty+"], configuring it to ALL");
        }
        try
        {
            logFileCount = PersistentProperty.get(COMMON_PROPERTIES,logLevelProperty,Debug.MAX_LOG_FILE_COUNT_PROP);
        }
        catch(Exception e)
        {
            // IGNORE
            Debug.log(Debug.MSG_STATUS, "Could not configure MAX_LOG_FILE_COUNT to configured logFileCount["+logLevelProperty+"], configuring it to 5");
        }
        try
        {
            maxDebugWrites = PersistentProperty.get(COMMON_PROPERTIES, COMMON_PROPERTIES, Debug.MAX_DEBUG_WRITES_PROP);
        }
        catch(Exception e)
        {
            // IGNORE
            Debug.log(Debug.MSG_STATUS, "Could not configure MAX_DEBUG_WRITES to configured logLevelProperty["+logLevelProperty+"], configuring it to 100000");
        }

        props.put(Debug.DEBUG_LOG_LEVELS_PROP,debugLogLevel);
        props.put(DebugConfigurator.MAX_FILE_BACKUP_PROP, logFileCount);
        props.put(Debug.MAX_DEBUG_WRITES_PROP, maxDebugWrites);
        if (initProps != null && initProps.getProperty(Debug.BASE_LOG_DIRECTORY_PROP)!=null)
        	props.put(Debug.BASE_LOG_DIRECTORY_PROP, initProps.getProperty(Debug.BASE_LOG_DIRECTORY_PROP));

        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        	Debug.log(Debug.NORMAL_STATUS, "Driver property: ["+props+"]");
        
        try
        {
            DebugConfigurator.configure(props, driverKey);
        }
        catch (FrameworkException e)
        {
            Debug.log(Debug.MSG_WARNING,"Failed to configure logs with driverkey["+driverKey+"]"+e.getMessage());
            return;
        }
        DebugLogger.getLogger(driverKey, this.getClass());
    }

   private static final String  COMMON_PROPERTIES = "COMMON_PROPERTIES";

}
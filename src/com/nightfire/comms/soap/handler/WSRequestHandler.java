package com.nightfire.comms.soap.handler;

import java.util.Hashtable;
import java.util.Properties;
import java.util.*;
import org.w3c.dom.Node;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.db.PropertyChainUtil;
import com.nightfire.framework.debug.DebugConfigurator;
import com.nightfire.framework.debug.DebugLogger;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.transformer.MessageTransformerCache;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.flowidentifier.FlowIdentifierHelper;
import com.nightfire.spi.common.HeaderNodeNames;
import com.nightfire.comms.handlers.RequestHandlerBase;

public class WSRequestHandler extends RequestHandlerBase
{

    /**
     * Property naming XSL file providing request header transformation.
     */
    public static final String REQUEST_HEADER_XFORM_FILE_NAME_PROP = "requestHeaderTransformFileName";

    /**
     * Property naming XSL file providing request body transformation.
     */
    public static final String REQUEST_BODY_XFORM_FILE_NAME_PROP = "requestBodyTransformFileName";

    /**
     * Property naming XSL file providing response body transformation.
     */
    public static final String RESPONSE_BODY_XFORM_FILE_NAME_PROP = "responseBodyTransformFileName";

    /**
     * Property naming XSL file providing response header transformation.
     */
    public static final String RESPONSE_HEADER_XFORM_FILE_NAME_PROP = "responseHeaderTransformFileName";

    /**
     * Property naming XSL file providing exception XML transformation.
     */
    public static final String EXCEPTION_XFORM_FILE_NAME_PROP = "exceptionTransformFileName";

    /**
     * Represents driver key property.
     */
    public static final String DRIVER_KEY = "DRIVER_KEY";

    /**
     * Represents driver type property
     */
    public static final String DRIVER_TYPE = "DRIVER_TYPE";

    /**
     * Represents common properties property.
     */
    public static final String COMMON_PROPERTIES = "COMMON_PROPERTIES";

    /**
     * Constant to represent the flow (send/receive).
     */
    private static final String FLOW_SUFFIX = "FlowSuffix";
    /**
     * Constant to represent the flow (send/receive).
     */
    private static final String ACTION = "Action";

    /**
     * Name of property indicating the name of the log file.
     */
    public static final String LOG_FILE_NAME_PROP = "LOG_FILE";
    /**
     * Name of property indicating the log level.
     */
    public static final String LOG_LEVEL_PROP = "DEBUG_LOG_LEVELS";

    private static final Object VALIDATE = "save";

    private static final Object SAVE = "validate";

    private static final Object SUBMIT = "submit";

    /**
     * perform the flush-cache functionality to reload changes.
     *
     * @param task String
     * @return String
     * @throws ProcessingException on error
     */
    public String manage ( String task ) throws ProcessingException
    {
        if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "manage() called with task argument value of [" + task + "] ..." );

        // For now, we ignore task argument and just perform the flush-cache functionality to reload changes.
        // In the future, the task string could be examined to determine what specific administrative task 
        // was being requested.

        if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Flushing cache via manage() WS call ..." );

        // Synchronize on class to get global WS interface locking when we're doing management activities.
        synchronized ( this.getClass() )
        {
            try
            {
                // Indicate that we're trying to update configuration so that all new requests will block.
                manageIsActive = true;

                // Wait until any outstanding processing requests complete.
                while ( activeProcessRequests > 0 )
                {
                    if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                        Debug.log( Debug.SYSTEM_CONFIG, "Waiting for [" + activeProcessRequests
                                + "] processing requests to complete ..." );

                    // Sleep for a second before checking again.
                    Thread.sleep( 1000 );
                }

                // Flush any caches for registered objects (ex: rules).
                CacheManager.getManager().flushCache( );
            }
            catch(Exception e)
            {
                Debug.error("Error: while managing the WS "+ e.getMessage() );
                throw new ProcessingException(e);
            }
            finally
            {
                manageIsActive = false;
            }
        }

        if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Done flushing cache via manage() WS call." );

        // Use this value to return any relevant admin outcome status message.
        String returnValue = "ok";

        if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "manage() call returns [" + returnValue + "]." );

        return returnValue;
    }

    /**
     * executes the message processor chain asynchronously.
     *
     * @param header information of CH header
     * @param request information of CH request (body)
     * @throws MessageException on error
     * @throws ProcessingException on error
     */
    public String processSync ( String header, String request ) throws MessageException, ProcessingException
    {
        HeaderMessageType ro = null;
        try
        {
            activeProcessRequests ++;

            // If we're in the middle of a management activity ...
            if ( manageIsActive )
            {
                if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                    Debug.log( Debug.SYSTEM_CONFIG, "Waiting for management activity to complete before continuing with processSync() call ..." );

                // Synchronize on class to get global WS interface locking when we're doing management activities.
                // (Lock has already been acquired in manage() method if flag is true, so we'll wait here until it returns.)
                synchronized ( WSRequestHandler.class )
                {
                    ro = process( header, request );
                }
            }
            else
            {
                // Skip expensive global synchronization where not needed.
                ro = process( header, request );
            }
        }
        finally
        {
            activeProcessRequests --;
        }

        return( ro.getMessage() );
    }

    /**
     * synchronously process the message processor chain.
     *
     * @param header information of CH header
     * @param request information of CH request (body)
     * @throws MessageException on error
     * @throws ProcessingException on error
     */
    public void processAsync ( String header, String request ) throws MessageException, ProcessingException
    {
        HeaderMessageType ro = null;
        try
        {
            activeProcessRequests ++;

            // If we're in the middle of a management activity ...
            if ( manageIsActive )
            {
                if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                    Debug.log( Debug.SYSTEM_CONFIG, "Waiting for management activity to complete before continuing with processAsync() call ..." );

                // Synchronize on class to get global WS interface locking when we're doing management activities.
                // (Lock has already been acquired in manage() method if flag is true, so we'll wait here until it returns.)
                synchronized ( WSRequestHandler.class )
                {
                    ro = process( header, request );
                }
            }
            else
            {
                // Skip expensive global synchronization where not needed.
                ro = process( header, request );
            }
        }
        finally
        {
            activeProcessRequests --;
        }
        if ( ro.getMessage() != null )
            Debug.warning( "Asynchronous gateway call returned the following data:\n" + ro.getMessage() );
    }


    /**
     * Type used to return property key and type as unit.
     */
    private class PropertyInfo
    {
        String propertyKey;
        String propertyType;

        public PropertyInfo(String propertyKey, String propertyType)
        {
            super();
            this.propertyKey = propertyKey;
            this.propertyType = propertyType;
        }
    }

    /**
     * gets the property key and property type from the header.
     *
     * @param header information of CH header
     * @return property info populated with its key and type.
     * @throws ProcessingException on error
     */
    private PropertyInfo getPropertyInfo(String header) throws ProcessingException
    {
        String request=null,flowSuffix=null,action=null;
        try
        {
            XMLMessageParser p = new XMLMessageParser( header );

            request = p.getValue( HeaderNodeNames.REQUEST_NODE );
            flowSuffix = p.getValue(FLOW_SUFFIX);
            action = p.exists(ACTION)? p.getValue(ACTION):null;

            if(StringUtils.hasValue(request)&& StringUtils.hasValue(flowSuffix))
            {
               if(StringUtils.hasValue(action) && (action.equals(SAVE)||action.equals(VALIDATE)))
                {
                    // Get Property information for save/validate action
                    return new PropertyInfo(COMMON_PROPERTIES, request + "_" + action);
                }
                else if (!StringUtils.hasValue(action) || action.equals(SUBMIT))
                {
                    // Get Property information for submit action
                    return new PropertyInfo(COMMON_PROPERTIES, request + flowSuffix);
                }
                else
                {
                    // generate a BR error xml.
                    String errorXML = "<?xml version=\"1.0\"?>"+
                                        "<Errors>" +
                                            "<ruleerrorcontainer>" +
                                                "<ruleerror>" +
                                                    "<RULE_ID value=\"\"/>"+
                                                    "<MESSAGE value=\"Currently this action not supported\" />"+
                                                    "<CONTEXT value=\"\" />"+
                                                    "<CONTEXT_VALUE value=\"\" />"+
                                                "</ruleerror>" +
                                            "</ruleerrorcontainer>" +
                                        "</Errors>";
                    throw new ProcessingException(errorXML);
                }
            }
            else
            {
                throw new ProcessingException("No flowsuffix found for request ["+request+"], flowSuffix["+flowSuffix+"] and action["+action+"]");
            }
        }
        catch (MessageParserException mpe)
        {
            throw new ProcessingException("Unable to parse header [" + header + "] and get the request header" +
                    "with parameters request ["+request+"], flowSuffix["+flowSuffix+"] and action["+action+"]");
        }

    }

    /**
     * initialize the driverKey and driverType.
     */
    protected void initializeDriverKeyType(String header, String request) throws ProcessingException {

        // Identify flow from repository configuration files
        Map<String, String> driverFlowInfo = null;

        try {

            driverFlowInfo = FlowIdentifierHelper.identifyFlowFromCfg(header, request);

        } catch (FrameworkException e) {
              throw new ProcessingException("Could not identify driver from information (driver key and type) " + Debug.getStackTrace(e));

            
        }

        String driverKey = null;
        String driverType = null;

        // Support for existing functionality for ICP:
        // If driverFlowInfo is found as null ( i.e. no flow identifier configuration file is in repository),
        // fetch driver flow information from common properties

        if (driverFlowInfo == null || driverFlowInfo.isEmpty() ){

            PropertyInfo property = getPropertyInfo(header);

              if(property == null)
               {
                Debug.log(Debug.MSG_ERROR, "Could not fetch property key and type for header");
                throw new ProcessingException("No configuration found for header");
               }

             checkConfiguration(property.propertyKey, property.propertyType);

            // Get driver key and type from common properties
             driverKey = getProperty(DRIVER_KEY);
             driverType = getProperty(DRIVER_TYPE);
        }

        else{
            // Get driver key and type from flow identifier configuration files
            driverKey = driverFlowInfo.get(DRIVER_KEY.toLowerCase());
            driverType = driverFlowInfo.get(DRIVER_TYPE.toLowerCase());            
        }

        if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                 Debug.log( Debug.NORMAL_STATUS, "Fetched DriverKey [" + driverKey + "] DriverType[" + driverType+"].");

        super.setDriverKey(driverKey);
        super.setDriverType(driverType);

        if(!StringUtils.hasValue(driverKey)||!StringUtils.hasValue(driverType))
        {
            throw new ProcessingException("Could not configure DriverKey and DriverType");
        }


    }

    /**
     * Get a named optional property value.
     *
     * @param  name  Name of property.
     *
     * @return  Property value, or null if unavailable.
     */
    private String getProperty ( String name )
    {   if(configProps == null){
    	  return null;
    	}
        return( (String)configProps.get( name ) );
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

        // Apply stylesheet transformation to request header XML if configured to do so. 
        String xslFileName = getProperty( REQUEST_HEADER_XFORM_FILE_NAME_PROP );

        if ( StringUtils.hasValue( xslFileName ) )
        {
            header = MessageTransformerCache.getInstance().get(xslFileName).transform( header );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Transformed gateway request header is:\n" + header );
        }

        xslFileName = getProperty( REQUEST_BODY_XFORM_FILE_NAME_PROP );

        if ( StringUtils.hasValue( xslFileName ) )
        {
            request = MessageTransformerCache.getInstance().get(xslFileName).transform( request );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Transformed gateway request body is:\n" + request );
        }

        // remove the FLOW_SUFFIX header so that it's not propagated further.

        XMLMessageParser parser = new XMLMessageParser( header );

        // Also, add TradingPartner value in header with value same as Supplier.
        String tpValue = parser.getValue(HeaderNodeNames.SUPPLIER_NODE);

        XMLPlainGenerator xpg = new XMLPlainGenerator(parser.getDocument());
        Node newNode = xpg.create("TradingPartner");

        if (Debug.isLevelEnabled(Debug.XML_DATA))
            Debug.log( Debug.XML_DATA, "WSRequestHandler TPNodeValue ["+tpValue+"].");

        XMLMessageParser.setNodeAttributeValue(newNode, "value", tpValue);

        header = xpg.describe();

        if (Debug.isLevelEnabled(Debug.XML_DATA))
            Debug.log( Debug.XML_DATA, "WSRequestHandler preprocessed headers["+header+"].");

        return super.preProcess(header, request);
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
        // Apply stylesheet transformation to response body XML if configured to do so. 
        String xslFileName = getProperty( RESPONSE_HEADER_XFORM_FILE_NAME_PROP );

        if ( (respHeader != null) && StringUtils.hasValue( xslFileName ) )
        {
            respHeader = MessageTransformerCache.getInstance().get(xslFileName).transform( respHeader );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Transformed gateway response header is:\n" + respHeader );
        }

        xslFileName = getProperty( RESPONSE_BODY_XFORM_FILE_NAME_PROP );

        if ( (response != null) && StringUtils.hasValue( xslFileName ) )
        {
            response = MessageTransformerCache.getInstance().get(xslFileName).transform( response );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Transformed gateway response body is:\n" + response );
        }

        return super.postProcess(respHeader, response);
    }

    /**
     * transforms the exception message and customize message by applying map.
     *
     * @param errMsg errorMessage
     * @return error transformed message
     * @throws MessageException on error
     */
    protected String transformExceptionMessage(String errMsg) throws MessageException
    {
        if ( errMsg.startsWith("<?xml ") )
        {
            String xslFileName = getProperty( EXCEPTION_XFORM_FILE_NAME_PROP );

            if ( StringUtils.hasValue( xslFileName ) )
            {
                errMsg = MessageTransformerCache.getInstance().get(xslFileName).transform( errMsg );

                Debug.warning( "Transformed gateway exception is:\n" + errMsg );
            }
        }
        return errMsg;
    }

    /**
     * Check that configuration is loaded and if not, load it now.
     *
     * @param propertyKey key to be used to load the properties.
     * @param propertyType type used to load the properties.
     * @throws ProcessingException Thrown on any configuration errors.
     */
    private void checkConfiguration (String propertyKey, String propertyType) throws ProcessingException
    {
        // Lock to make sure that we don't do more than once at the same time.
        synchronized( this.getClass() )
        {
            // Only need to configure once.
            try
            {
                // Start with all logging enabled.
                Debug.enableAll( );
                Debug.showLevels( );
                FrameworkException.showStackTrace( );

                PropertyChainUtil propUtil = new PropertyChainUtil( );

                configProps = propUtil.buildPropertyChains(propertyKey,propertyType);


                // Reset Debug stuff so that it can be configured via properties.
                Debug.disableAll( );

                // set the appropriate logs
                String logfile = getProperty(LOG_FILE_NAME_PROP);
                String loglevel = getProperty(LOG_LEVEL_PROP);

                if(StringUtils.hasValue(logfile))
                {
                    if(!StringUtils.hasValue(loglevel))
                    {
                        loglevel = "all";
                    }

                    Debug.log(Debug.MSG_STATUS, "Redirecting Logs to "+logfile);
                    Debug.log(Debug.MSG_STATUS, "Setting  loglevel = "+loglevel);

                    Properties props = new Properties();
                    props.put(LOG_FILE_NAME_PROP,logfile);
                    props.put(LOG_LEVEL_PROP,loglevel);

                    try
                    {
                        DebugConfigurator.configure(props,logfile);
                    }
                    catch (FrameworkException e)
                    {
                        Debug.log(Debug.MSG_WARNING, "Could not configure logs properly "+e.getMessage());
                    }

                    DebugLogger.getLogger(logfile,this.getClass());
                }

                // If thread logging is enabled, turn on thread-id logging in messages.
                if ( Debug.isLevelEnabled( Debug.THREAD_BASE ) )
                    Debug.enableThreadLogging( );

                // Display stack trace when exceptions are created.
                if ( Debug.isLevelEnabled( Debug.EXCEPTION_STACK_TRACE ) )
                    FrameworkException.showStackTrace( );

                String repRootDir = getProperty( RepositoryManager.NF_REPOSITORY_ROOT_PROP );

                if ( StringUtils.hasValue( repRootDir ) )
                    System.setProperty( RepositoryManager.NF_REPOSITORY_ROOT_PROP, repRootDir );

            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );

                throw new ProcessingException( e.getMessage() );
            }

        }

    }

    // Configuration properties loaded from the database.
    private static Hashtable configProps;

    // Globable variable that indicates whether any management activity is currently occurring or not.
    // 'volatile' qualifier is critical to ensure that all threads see same value.
    private static volatile boolean manageIsActive = false;

    // Globable variable that indicates whether any processing activity is currently occurring or not.
    // 'volatile' qualifier is critical to ensure that all threads see same value.
    private static volatile int activeProcessRequests = 0;


}

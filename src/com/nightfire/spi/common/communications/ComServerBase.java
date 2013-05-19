/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.spi.common.communications;

import java.util.*;
import org.w3c.dom.*;

import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.spi.common.supervisor.*;

/**
 * Base class for communications server objects.
 */
public abstract class ComServerBase implements Runnable
{
    /**
     * Property key value used to access properties stored in PersistentProperty table.
     */
    public static final String MPD_KEY_PROP            = "DRIVER_KEY";

    /**
     * Property type value used to access stored in PersistentProperty table.
     */
    public static final String MPD_TYPE_PROP           = "DRIVER_TYPE";

    /**
     * Property indicating the time to wait (in seconds) for the driver
     * to return from processing before logging warnings to the debug logs.
     */
    public static final String WATCH_TIME_PROP = "WATCH_TIME";

    /**
     * Property indicating whether parent thread should be interrupted if
     * watch time is exceeded (true) or not (false - default).
     */
    public static final String WATCH_SHOULD_INTERRUPT_PARENT_FLAG_PROP = "WATCH_SHOULD_INTERRUPT_PARENT_FLAG";

    /**
     * Communication server STARTUP event message constant.
     */
    public static final String STARTUP_TAG             = "STARTUP";

    /**
     * Communication server SHUTDOWN event message constant.
     */
    public static final String SHUTDOWN_TAG            = "SHUTDOWN";


    /**
     * Properties table
     */
    protected Map properties                             = null;

    // The key and type fo the comm server
    protected String key;
    protected String type;

    private String customerIdentifierConfig = null;

    // -1 = not set.
    private int watchTime = -1;

    // Flag indicating whether watcher should interrupt parent
    // if watch time is exceeded or not (default=false).
    private boolean watcherInterruptsParent = false;

    /**
     * Class that is used to generate unique id for (request/respone) messages.
     */
    private SeqIdGenerator idGenerator = null;

    /**
     * DriverUniqueIdentifierReader to read Unique Identifier per <driverKey-driverType>.
     */
    private DriverUniqueIdentifierReader driverUniqueIdentifierReader;

    /**
     * Default Constructor is private.
     * Prevents instantiation of derived classes without arguments
     */
    private ComServerBase()
    {
    }


    /**
     * Constructor that creates comm server object and loads its properties.
     *
     * @param  key  Key value used to access configuration properties.
     * @param  type  Type value used to access configuration properties.
     *
     * @exception  ProcessingException  Thrown on initialization errors.
     */
    protected ComServerBase(String key, String type) throws ProcessingException
    {
      initialize(key, type);
      this.key = key;
      this.type = type;
    }

      /**
       * Called to initialize a comm server object.
       *
       * @param  key   Property-key to use for locating initialization properties.
       *
       * @param  type  Property-type to use for locating initialization properties.
       *
       * @exception ProcessingException when initialization fails
       */
       protected void initialize ( String key, String type ) throws ProcessingException
       {

        if (!StringUtils.hasValue(key) || !StringUtils.hasValue(type))
        {
            Debug.log (Debug.ALL_ERRORS, "Cannot initiate Communication Server. " +
                                    "Invalid key and type specified. key/type [" + key + "/" + type + "]");
            throw new ProcessingException("Cannot initiate Communication Server. " +
                "Invalid key and type specified.");
        }

        try
        {
            properties = (new PropertyChainUtil()).buildPropertyChains(key, type);

            customerIdentifierConfig = getPropertyValue( CustomerContext.CUSTOMER_ID_PROP );

            String temp = getPropertyValue( WATCH_TIME_PROP );

            if ( StringUtils.hasValue( temp ) )
                watchTime = StringUtils.getInteger( temp );

            temp = getPropertyValue( WATCH_SHOULD_INTERRUPT_PARENT_FLAG_PROP );

            if ( StringUtils.hasValue( temp ) )
                watcherInterruptsParent = StringUtils.getBoolean( temp );

            idGenerator = new SeqIdGenerator();

            driverUniqueIdentifierReader = new DriverUniqueIdentifierReader(key, type);
        }
        catch (Exception e)
        {
            throw new ProcessingException(e.getMessage());
        }

        Debug.log(Debug.SYSTEM_CONFIG, StringUtils.getClassName(this) + ": Loaded communications " +
                                       "server base properties with key [" + key + "], type [" + type + "].");
    }


    /**
     * Method called to process requests.
     *
     * @param  header    Non-message administrative information.
     * @param  request   The request message in one of two formats:
     *                   A String for stream-based messages.
     *                   A DOM document for structured messages.
     * @return  The response message  in one of two formats:
     *          A String for stream-based messages.
     *          A DOM document for structured messages.
     *          NOTE: The header is retrieved from the response header location (defined by
     *          MessageProcessingDriver.CONTEXT_RESPONSE_HEADER_NAME). If this location does not exist in
     *          the MessageProcessorContext then the request header location (defined by
     *          MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME) is returned instead.
     *          If a request header does not exist, then a null header is returned.
     *
     * @exception  ProcessingException  Thrown if processing can't be performed.
     * @exception  MessageException  Thrown if message is invalid.
     */
    protected ResponseObject process (String header, Object request)
        throws ProcessingException, MessageException
    {
        return process(header,
                       request,
                       getRequiredPropertyValue(MPD_KEY_PROP),
                       getRequiredPropertyValue(MPD_TYPE_PROP));
    }


    /**
     * Method called to process requests.
     *
     * @param   header      Non-message administrative information.
     * @param   request     The request message in one of two formats:
     *                      A String for stream-based messages.
     *                      A DOM document for structured messages.
     * @param   drvKey      Driver key value used to load driver configuration properties.
     * @param   drvType     Driver type value used to load driver configuration properties.
     *
     * @return  ResponseObject object which contains the result header and message.
     *          The header or message can be one of two formats:
     *          A String for stream-based messages.
     *          A DOM document for structured messages.
     *
     *          NOTE: The header is retrieved from the response header location (defined by
     *          MessageProcessingDriver.CONTEXT_RESPONSE_HEADER_NAME). If this location does not exist in
     *          the MessageProcessorContext then the request header location (defined by
     *          MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME) is returned instead.
     *          If a request header does not exist, then a null header is returned.
     *
     * @exception  ProcessingException  Thrown if processing can't be performed.
     * @exception  MessageException  Thrown if message is invalid.
     */
    protected ResponseObject process (String header, Object request, String drvKey, String drvType)
        throws ProcessingException, MessageException
    {
        long metricsStartTime = System.currentTimeMillis();

        String outcome = MetricsAgent.FAIL_STATUS;

        ResponseObject res = new ResponseObject();

        if (!StringUtils.hasValue(drvKey) || !StringUtils.hasValue(drvType))
        {
            Debug.log(Debug.ALL_ERRORS,StringUtils.getClassName(this) +
                ": ERROR: Invalid driver key [" + drvKey + "] or type [" + drvType + "] properties.");

            throw new ProcessingException(StringUtils.getClassName(this) +
                ": ERROR: Invalid driver key [" + drvKey + "] or type [" + drvType + "] properties.");
        }

        MessageProcessingDriver driver = null;

        try
        {
            // Process "push" type server requests that have a header.
            if (StringUtils.hasValue(header))
            {
                driver = new MessageProcessingDriver( CustomerContext.getInstance().propagate( header ) );
            }
            else
            {
                // In the async/pull-style comm servers, a customer-id value may be set on the properties.
                if ( StringUtils.hasValue( customerIdentifierConfig ) )
                    CustomerContext.getInstance().setCustomerID( customerIdentifierConfig );

                driver = new MessageProcessingDriver();
            }

            driver.initialize(drvKey, drvType);

            configureDriver(driver);

            if(CustomerContext.getInstance().getMessageId()==null)
            {
                String reqId = idGenerator.getStrNextId();
                CustomerContext.getInstance().setMessageId(reqId);
            }

            if(CustomerContext.getInstance().getUniqueIdentifier()==null)
            {
                String uniqueIdentifier = null;

                if (request instanceof String)
                    uniqueIdentifier = driverUniqueIdentifierReader.getUniqueIdentifier((String)request);
                else if (request instanceof Document)
                    uniqueIdentifier = driverUniqueIdentifierReader.getUniqueIdentifier((Document)request);

                CustomerContext.getInstance().setUniqueIdentifier(uniqueIdentifier);
            }

            TimedWatcher watcher = null;

            // If a watch time was set, initialize and start the watcher.
            if ( watchTime > 0 )
            {
                watcher = new TimedWatcher( watchTime, "Executing driver [" + drvKey + ":" + drvType + "].",
                                            watcherInterruptsParent );

                watcher.watch( );
            }

            try
            {
                res.message = driver.process(request);
            }
            finally
            {
                // Make sure any watcher is cleaned up.
                if ( watcher != null )
                    watcher.cancel( );
            }

            MessageProcessorContext context = driver.getContext( );

            // look for a response header. if one does not exit return the request header
            // from the context in case it was modified.
            if (context.exists(MessageProcessingDriver.CONTEXT_RESPONSE_HEADER_NAME) )
               res.header = context.get(MessageProcessingDriver.CONTEXT_RESPONSE_HEADER_NAME);
            else if (context.exists(MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME) )
               res.header = context.get(MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME);

            outcome = MetricsAgent.PASS_STATUS;

            return res;
        }
        catch (ProcessingException pe)
        {
            throw pe;
        }
        catch (MessageException me)
        {
            throw me;
        }
        catch (Exception e)
        {
            // Re-throw any unknown exceptions as processing exceptions.
            throw new ProcessingException(e);
        }
        catch ( Throwable reallyBadError )
        {
            // Exceptions of type 'Error' indicate a JVM-level system failure that is
            // most likely unrecoverable, so we'll just log the message and rethrow it.
            Debug.log( Debug.ALL_ERRORS, "ERROR: The following system-level error occurred:\n"
                       + reallyBadError.toString() );

            Debug.log( Debug.ALL_ERRORS, "Stack trace:\n" + Debug.getStackTrace(reallyBadError) );

            throw new Error( reallyBadError.toString() );
        }
        finally
        {
            if ( MetricsAgent.isOn( MetricsAgent.GATEWAY_CATEGORY ) )
                MetricsAgent.logGateway( metricsStartTime, drvKey, drvType + "," 
                                         + getHeaderMetrics(header) + "." + outcome );

            // Reset customer context.
            try
            {
                CustomerContext.getInstance().cleanup( );
            }
            catch ( Exception e )
            {
                Debug.error( e.toString() );
            }
        }
    }


    /**
     * Extract metrics-relevant data from header, if it exists, and return it.
     *
     * @param   header  Incoming header, or null if absent.
     *
     * @return  Metrics-relevant header data.
     */
    public static String getHeaderMetrics ( Object header )
    {
        String result = "";

        try
        {
            if ( header != null )
            {
                StringBuffer sb = new StringBuffer( );

                XMLMessageParser p = null;

                if ( header instanceof String )
                    p = new XMLMessageParser( (String)header );
                else
                    p = new XMLMessageParser( (Document)header );

                if ( p.valueExists( HeaderNodeNames.SUPPLIER_NODE ) )
                {
                    if ( sb.length() > 0 )
                        sb.append( "." );
                    sb.append( p.getValue( HeaderNodeNames.SUPPLIER_NODE ) );
                }

                if ( p.valueExists( HeaderNodeNames.REQUEST_NODE ) )
                {
                    if ( sb.length() > 0 )
                        sb.append( "." );
                    sb.append( p.getValue( HeaderNodeNames.REQUEST_NODE ) );
                }

                if ( p.valueExists( HeaderNodeNames.SUBREQUEST_NODE ) )
                {
                    if ( sb.length() > 0 )
                        sb.append( "." );
                    sb.append( p.getValue( HeaderNodeNames.SUBREQUEST_NODE ) );
                }

                if ( p.valueExists( HeaderNodeNames.SUBTYPE_NODE ) )
                {
                    if ( sb.length() > 0 )
                        sb.append( "." );
                    sb.append( p.getValue( HeaderNodeNames.SUBTYPE_NODE ) );
                }

                result = sb.toString( );
            }
        }
        catch ( Exception e )
        {
            Debug.warning( "Couldn't extract metrics information from request header: " + e.toString() );
        }

        return result;
    }


    /**
     * Get the value for the named property from the
     * persistent properties (if it exists).
     *
     * @param propName The property whose value is to be returned
     *
     * @return  Property's value.
     *
     * @exception ProcessingException Thrown if property does not exist
     */
    protected String getRequiredPropertyValue (String propName)
        throws ProcessingException
    {
        try
        {
            return PropUtils.getRequiredPropertyValue(properties, propName);
        }
        catch (FrameworkException e)
        {
            throw new ProcessingException(e);
        }
    }


    /**
     * Get the named property value from the persistent
     * properties (if it exists)
     *
     * @param propName The property whose value is to be returned.
     *
     * @param errorMsg Container for any errors that occur during proessing. Error
     *  messages are appended to this container instead of throwing exceptions.
     *
     * @return  Property's value.
     */
    protected String getRequiredPropertyValue (String propName, StringBuffer errorMsg)
    {
        return PropUtils.getRequiredPropertyValue(properties, propName, errorMsg);
    }


    /**
     * Get the named property value for from the persistent properties
     *
     * @param propName The property whose value is to be returned
     *
     * @return Value of property, or null if not available.
     */
    protected String getPropertyValue (String propName)
    {
        return PropUtils.getPropertyValue(properties, propName);
    }


    /**
     * Callback allowing sub-classes to provide additional driver configuration.
     * Default implementation is to provide no additional driver configuration.
     *
     * @param  driver  Message-processing driver to configure.
     *
     * @exception  ProcessingException  Thrown if processing can't be performed.
     *
     * @exception  MessageException  Thrown if message is bad.
     */
    protected void configureDriver (MessageProcessingDriver driver) throws ProcessingException, MessageException
    {
        Debug.log(Debug.MSG_STATUS, "No additional driver configuration is required.");
    }


    /**
     * Shuts-down the server object.
     * Abstract method. Must be implemented by child classes.
     */
    public abstract void shutdown ();


    public class ResponseObject {
       public Object header;
       public Object message;

       public ResponseObject() {
          header = null;
          message = null;
       }

       public ResponseObject(Object header, Object message) {
          this.header = header;
          this.message = message;
       }
    }
}

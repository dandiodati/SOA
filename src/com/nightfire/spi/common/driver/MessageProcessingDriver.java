/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.spi.common.driver;

import java.util.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.monitor.*;
import com.nightfire.framework.jmx.cmn.Stat;
import com.nightfire.framework.jmx.cmn.StatFactory;
import com.nightfire.framework.jmx.helper.StatHelper;


/**
 * Driver for gateway message processing involving adapters, transforms
 * and communications objects.  Objects of this type are created by
 * communications server objects when requests are received.
 */
public class MessageProcessingDriver
{
    /**
     * Name of the request header in the message-processor context.
     */
	public static final String CONTEXT_REQUEST_HEADER_NAME = "REQUEST_HEADER";


    /**
     * Name of the response header in the message-processor context.
     */
	public static final String CONTEXT_RESPONSE_HEADER_NAME = "RESPONSE_HEADER";


    /**
     * The property name prefix indicating the name of a MessageProcessor class.
     */
	public static final String MP_CLASS_NAME_TAG_PROP = "CLASS";

    /**
     * The property name prefix indicating the persistent property key
     * for a MessageProcessor class.
     */
	public static final String MP_KEY_PROP = "KEY";

    /**
     * The property name prefix indicating the persistent property type
     * for a MessageProcessor class.
     */
	public static final String MP_TYPE_PROP = "TYPE";

    /**
     * Name of the first processor to which the driver will send message.
     */
	public static final String ROOT_MESSAGE_PROCESSOR = "ROOT";

    /**
     * Name in a NVPair object returned by aMessageProcessor object which
     * indicates that the associated value is to be returned to the
     * communications server object that originated the synchronous request.
     */
	public static final String TO_COMM_SERVER = "COMM_SERVER";

    /**
     * Flag indicating whether driver is asynchronous or synchronous.
     * Valid values are "TRUE" and "FALSE".
     */
	public static final String ASYNC_FLAG_PROP = "ASYNC_FLAG";

    /**
     * Flag indicating whether driver should return value as a string.
     * Default is to return whatever last message-processor returns.
     */
	public static final String RETURN_STRING_PROP = "RETURN_STRING";

    /**
     * Flag indicating whether driver should collect all exceptions to return as
     * a group at the end of processing (true), or terminate processing when first
     * exception is encountered.
     */
	public static final String DEFER_EXCEPTIONS_FLAG_PROP = "DEFER_EXCEPTIONS_FLAG";

    /**
     * Fully-qualified (package + class) name of message-processor class to be
     * invoked on any errors that occur during message-processor execution.
     */
	public static final String ERROR_PROCESSOR_CLASS_NAME_PROP = "ERROR_PROCESSOR_CLASS_NAME";

    /**
     * Property key for error-handler.
     */
	public static final String ERROR_PROCESSOR_KEY_PROP = "ERROR_PROCESSOR_KEY";

    /**
     * Property type for error-handler.
     */
	public static final String ERROR_PROCESSOR_TYPE_PROP = "ERROR_PROCESSOR_TYPE";

    /**
     * Name used to access exception in the message-processor-context.
     */
	public static final String ERROR_PROCESSOR_EXCEPTION_LOC = "ERROR_PROCESSOR_EXCEPTION_LOCATION";

    /**
     * Name used to get the location where the container of error-context objects will be
     * placed in the message-procesor context if exceptions are thrown during processing.
     * The object at this location will be a LinkedList of ErrorContext objects, with the
     * list containing errors ordered from the most recent (at the head) to the oldest (at the tail).
     * If not configured, the default location will be used.
     */
	public static final String ERROR_CONTEXT_LOC_PROP = "ERROR_CONTEXT_LOCATION";

    /**
     * The default location where the list of error-context objects will
     * be placed in the message-processor context, if no configured
     * value is specified.
     */
	public static final String DEFAULT_ERROR_CONTEXT_LOC = "DEFAULT_ERROR_CONTEXT_LOCATION";


    /**
     * For testing only - Indicates the name of a property file containing
     * name/value pairs to be placed in the driver's context in the main() test
     * driver method.
     */
	public static final String TEST_CONTEXT_PROPERTIES_FILE_NAME = "TEST_CONTEXT_PROPERTIES_FILE_NAME";


    /**
     * Class used to associate an exception with the message-processor and the
     * input that resulted in the exception being thrown during processor execution.
     */
    public static class ErrorContext
    {
        /**
         * The message-processor that threw the exception.
         */
        public MessageProcessor processor;

        /**
         * The input to the message-processor that resulted in the thrown exception.
         */
        public MessageObject input;

        /**
         * The exception that was thrown.
         */
        public Exception error;


        public ErrorContext ( MessageProcessor processor, MessageObject input, Exception error )
        {
            this.processor = processor;
            this.input     = input;
            this.error     = error;
        }
    }


    /**
     * Gateway Driver unit-test driver application entry point.
     *
     * @param  args  Command-line arguments:
     *               arg[0] = db-name.
     *               arg[1] = db-user.
     *               arg[2] = db-password.
     *               arg[3] = Driver property key.
     *               arg[4] = Driver property type.
     *               arg[5] = Name of file containing driver request.
     *               arg[6] = orb-initialization-required flag (true/false).
     *               arg[7] = Number of times to execute the test (Optional - default is 1).
     *               arg[8] = File containing XML request header (Optional).
     */
    public static void main ( String[] args )
    {
        if ( args.length < 7 )
        {
            System.err.println( "\n\nUSAGE: MessageProcessingDriver <db-name> <db-user> <db-password> "
                                + "<driver-prop-key> <driver-prop-type> <request-file-name> <orb-init-required> "
                                + "[<execution-count>] [<request-header-file-name>].\n\n" );

            System.exit( -1 );
        }

        Debug.enableAll( );
        Debug.showLevels( );
        Debug.configureFromProperties( );

        // If thread logging is enabled, turn on thread-id logging in messages.
        if (Debug.isLevelEnabled(Debug.THREAD_BASE))
            Debug.enableThreadLogging();

        // Display stack trace when exceptions are created.
        if (Debug.isLevelEnabled( Debug.EXCEPTION_STACK_TRACE))
            FrameworkException.showStackTrace();

        String dbName          = args[0];
        String dbUser          = args[1];
        String dbPassword      = args[2];
        String propKey         = args[3];
        String propType        = args[4];
        String requestFileName = args[5];
        String orbInitRequired = args[6];

        try
        {
            // Initialize the database connections.
            Debug.log( Debug.UNIT_TEST, "Initializing database ..." );

            DBInterface.initialize( dbName, dbUser, dbPassword );


            // Create the CORBA ORB.
            if ( orbInitRequired.equalsIgnoreCase("true") || orbInitRequired.equalsIgnoreCase("yes") )
            {
                Debug.log( Debug.UNIT_TEST, "Creating CORBA orb ..." );

                org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init( args, null );
            }

            // Determine how many iterations should be executed.
            int executionCount = 1;

            if ( args.length > 7 )
                executionCount = Integer.parseInt( args[7] );

            // Load the request to be passed to the driver.
            String request = FileUtils.readFile( requestFileName );

            Debug.log( Debug.UNIT_TEST, "\nREQUEST:\n" + request );

            for ( int Ix = 0;  Ix < executionCount;  Ix ++ )
            {
                Debug.log( Debug.UNIT_TEST, "Executing test iteration [" + (Ix + 1) + "]." );

                // Create the driver, configure it and pass the request to it for processing.
                MessageProcessingDriver driver = null;

                if ( args.length > 8 )
                    driver = new MessageProcessingDriver( FileUtils.readFile( args[8] ) );
                else
                    driver = new MessageProcessingDriver( );

                driver.initialize( propKey, propType );

                Object result = null;

                String contextPropsFileName = System.getProperty( TEST_CONTEXT_PROPERTIES_FILE_NAME );

                if ( StringUtils.hasValue( contextPropsFileName ) )
                {
                    MessageProcessorContext mpc = driver.getContext( );

                    Properties ctxProps = FileUtils.loadProperties( null, contextPropsFileName );

                    Debug.log( Debug.UNIT_TEST, "Populating driver-context with the following name/value pairs:\n" + ctxProps );

                    Enumeration iter = ctxProps.propertyNames( );

                    while ( iter.hasMoreElements() )
                    {
                        String name = (String)iter.nextElement( );

                        mpc.set( name, ctxProps.getProperty( name ) );
                    }

                    result = driver.process( mpc, request );

                }
                else
                    result = driver.process( request );

                Debug.log( Debug.UNIT_TEST, "\nRESPONSE:\n" + result );

                Performance.logMemoryUsage( Debug.UNIT_TEST, "After driver process() call." );
            }

            DBInterface.closeConnection( );
        }
        catch ( Exception e )
        {
            System.err.println( "\n\nERROR: " + e.toString() );

            e.printStackTrace( );
        }
    }


    /**
     * Constructs an unconfigured MessageProcessingDriver object.
     * To configure it, call its <code>initialize</code> method.
     */
	public MessageProcessingDriver ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating MessageProcessingDriver ..." );

        processors = new HashMap( );
	}


    /**
     * Constructs an unconfigured MessageProcessingDriver object.
     *
     * @param  requestHeader  XML text stream containing the request header.
     *
     * @exception  FrameworkException  Thrown on XML parsing errors.
     */
	public MessageProcessingDriver ( String requestHeader ) throws FrameworkException
    {
        this( new XMLMessageParser(requestHeader).getDocument() );
	}


    /**
     * Constructs an unconfigured MessageProcessingDriver object.
     *
     * @param  requestHeader  XML DOM Document containing the request header.
     *
     * @exception  FrameworkException  Thrown on XML parsing errors.
     */
	public MessageProcessingDriver ( Document requestHeader ) throws FrameworkException
    {
        this( );

        MessageProcessorContext context = getContext( );

        context.set( CONTEXT_REQUEST_HEADER_NAME, requestHeader );
	}


    /**
     * Get the message-processor context associated with the driver.
     *
     * @return  The message-processor context.
     *
     * @exception  ProcessingException  Thrown if context isn't available.
     */
    public MessageProcessorContext getContext ( ) throws ProcessingException
    {
        if ( mpContext == null )
        {
            try
            {
                if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                    Debug.log( Debug.MSG_LIFECYCLE, "Creating new context to pass to message-processors ..." );

                // Get a new context to pass to each message-processor.
                mpContext = new MessageProcessorContext( );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "ERROR: Could not create message-processor context:\n"
                                               + e.toString() );
            }
        }

        return mpContext;
    }


    /**
     * Set the message-processor context on the driver.
     *
     * @param  context  The message-processor context.
     *
     * @exception  ProcessingException  Thrown if context is invalid.
     */
    public void setContext ( MessageProcessorContext context ) throws ProcessingException
    {
        if ( context == null )
        {
            throw new ProcessingException( "ERROR: Attempt was made to set a null context on driver." );
        }

        if ( mpContext == context )
        {
            throw new ProcessingException( "ERROR: Attempt was made to set driver's context with one it already has." );
        }

        if ( mpContext != null )
        {
            Debug.log( Debug.ALL_WARNINGS, "WARNING: Replacing pre-existing context on driver with a new one." );

            mpContext.rollback( );
        }

        if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log( Debug.MSG_LIFECYCLE, "Setting context on driver ..." );

        mpContext = context;
    }


    /**
     * Sets the value of <code>properties</code> for this message processing driver.
     *
     * @param  props  Hash table containing property values.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
	public void setProperties ( Hashtable props ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Setting properties on driver ..." );

        properties = props;
	}


    /**
     * Returns the current value of <code>asyncFlag</code>.
     *
     * @return  Current value of <code>asyncFlag</code>.
     */
	public boolean isAsync ( )
    {
		return asyncFlag;
	}


    /**
     * Sets the value of <code>loadConfig</code> flag for this driver.
     * Setting this flag causes the driver to skip loading properties for
     * any message processors that it creates.
     *
     * @param  flag   New value for <code>loadConfig</code> flag.
     */
	public void setLoadConfig ( boolean flag )
    {
		loadConfig = flag;

        if ( Debug.isLevelEnabled ( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Setting load-configuration flag value to ["
                       + loadConfig + "]." );
	}


    /**
     * Initializes the driver by loading its properties and creating the
     * indicated message-processors.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
	public void initialize ( String key, String type ) throws ProcessingException
    {
        if ( Debug.isLevelEnabled ( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Initializing driver via property key ["
                       + key + "], type [" + type + "]." );

        driverConfigKey = key;
        driverConfigType = type;

		try
        {
            PropertyChainUtil propChain = new PropertyChainUtil( );

			setProperties( propChain.buildPropertyChains( key, type ) );
		}
		catch ( Exception e )
        {
			throw new ProcessingException( "ERROR: Could not initialize driver properties:\n"
                                           + e.toString() );
		}

    //Adding try-catch block, because often times, the driver configuration is missing,
    //but the error thrown back is that the ASYNC_FLAG is missing. It is nice to know
    //which driver chain is being referred to in that case.
    try
    {
		    initialize( );
    }
    catch ( ProcessingException e )
    {
        throw new ProcessingException( "ERROR: Driver key[" + key + "], type[" + type +
        "] failed to initialize:\n" +
        e.toString() );
    }
	}


	/**
	 * Parses properties and creates necessary message chains.
     *
     * @exception  ProcessingException  Thrown if processing fails.
	 */
	public void initialize ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Initializing driver ..." );

		// Initialize the request type (synchronous/asynchronous).
		String strTemp = (String)properties.get( ASYNC_FLAG_PROP );

		if ( !StringUtils.hasValue( strTemp ) )
        {
			throw new ProcessingException( "ERROR: Missing required driver property [" +
                                           ASYNC_FLAG_PROP + "]." );
		}

        try
        {
            asyncFlag = StringUtils.getBoolean( strTemp );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Invalid value for driver property [" +
                                           ASYNC_FLAG_PROP + "]." );
        }

        if ( Debug.isLevelEnabled ( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Asynchronous flag value [" + asyncFlag + "]." );

		// Initialize the defer-exceptions flag, if available.
		strTemp = (String)properties.get( DEFER_EXCEPTIONS_FLAG_PROP );

        try
        {
            if ( StringUtils.hasValue( strTemp ) )
                deferExceptionsFlag = StringUtils.getBoolean( strTemp );
        }
        catch ( Exception e )
        {
            throw new ProcessingException( "ERROR: Invalid value for driver property [" +
                                           DEFER_EXCEPTIONS_FLAG_PROP + "]." );
        }

        if ( Debug.isLevelEnabled ( Debug.SYSTEM_CONFIG ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Defer exceptions flag value [" + deferExceptionsFlag + "]." );


        errorHandlerClassName = getProperty( ERROR_PROCESSOR_CLASS_NAME_PROP );

        // if an error-handler is configured, get the error-context location.
        if ( StringUtils.hasValue( errorHandlerClassName ) )
        {
            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Error-handler class name [" + errorHandlerClassName + "]." );

            errorContextLocation = (String)properties.get( ERROR_CONTEXT_LOC_PROP );

            // Use default if no configured value was given.
            if ( !StringUtils.hasValue( errorContextLocation ) )
                errorContextLocation = DEFAULT_ERROR_CONTEXT_LOC;

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Location at which the error-context will be placed in the message-processor context ["
                           + errorContextLocation + "]." );
        }
        else
            Debug.log( Debug.SYSTEM_CONFIG, "No error-handler was configured on driver." );

        // Create the message-processor objects as configured by the properties.
        createProcessors( );
	}


    /**
     * Processes the input message (DOM or String) and (optionally) returns
     * a value.
     *
     * @param  request  Input message to process.
     *
     * @return  Optional output message, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails.
     */
    public Object process ( Object request ) throws MessageException, ProcessingException
    {
        return( process( getContext(), request ) );
    }


    /**
     * Processes the input message (DOM or String) and (optionally) returns
     * a value.
     *
     * @param  context  Context to execute message-processor against.
     * @param  request  Input message to process.
     *
     * @return  Optional output message, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails.
     */
    public Object process ( MessageProcessorContext context, Object request ) throws MessageException, ProcessingException
    {
        long startTime = startTimer( Debug.BENCHMARK );

        //Create stat object to collect execution stats of subflow for monitoring purpose.
        //Stat object may be 'null' in case of corresponding  driverConfigKey and driverConfigType
        //does not require to be monitored.

        Stat stat = StatFactory.newDriverSubFlowStat(driverConfigKey,driverConfigType);

        // Create a container of all messages to be processed.
        LinkedList messageStack = new LinkedList( );

        // Create a new message object to pass to the first message-processor
        MessageObject message = getMessageObject( request );

        // Initialize outstanding messages-to-be-processed with the input request, and
        // indicate that 'root' message-processor should start the processing.
        push( messageStack, new NVPair( ROOT_MESSAGE_PROCESSOR, message ) );

        // Get the initial message-processor tree that tracks the data flow.
        MessageProcessorTree rootProcessor
            = (MessageProcessorTree)processors.get( ROOT_MESSAGE_PROCESSOR );

        MessageProcessorTree candidate = null;

        NVPair input = null;

        Object response = null;

        String previousThreadName = null;

        ThreadMonitor.ThreadInfo tmti = null;

        try
        {
           tmti = ThreadMonitor.start( "Executing message-processing driver for configuration ["
                                       + driverConfigKey + ":" + driverConfigType + "]." );

            // Give the thread a unique name so that any debug log messages associated with this
            // driver can be correlated with the current thread of execution.
            previousThreadName = renameThread( null );

            // Loop until we get a response, or no more messages are available to process.
            do
            {
                if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                {
                    Debug.log( Debug.MSG_DATA, "Outstanding messages (in processing order):\n"
                               + describe( messageStack ) );
                }

                // If no outstanding messages are available ...
                if ( messageStack.isEmpty() )
                {
                    // Get a candiate message-processor to give a null input, in order to see
                    // if any cached results are available for further processing.
                    candidate = rootProcessor.getCandidate( );

                    // No candidate available, so we're done processing original request from
                    // the parent server object.
                    if ( candidate == null )
                    {
                        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_STATUS, "All message-processors are done processing." );

                        break;
                    }

                    // Construct null input with candidate's name.
                    input = new NVPair( candidate.getProcessorName(), null );

                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Passing null input to candidate processor ..." );
                }
                else
                {
                    // Available input will determine candidate message processor to execute.
                    candidate = null;

                    // Get next message to be processed from stack of outstanding messages.
                    input = pop( messageStack );
                }

                // Execute the message-processor indicated by the input name against the input value.
                NVPair[] results = executeProcessor( processors, context, input );

                // If message-processor returned any responses ...
                if ( resultsAvailable( results ) )
                {
                    // Push any responses onto stack.
                    Object returnValue = push( messageStack, results );

                    // If we've located the response destined for the parent
                    // communications server object, we're done.
                    if ( returnValue != null )
                    {
                        response = returnValue;

                        break;
                    }
                }
                else
                {
                    // If no results were returned by the execute() method called with a null input argument
                    // against the candidate message-processor, mark the processor as done.
                    if ( candidate != null )
                        candidate.setDone( );
                }

                if ( Debug.isLevelEnabled( Debug.MSG_LIFECYCLE ) )
                {
                    Debug.log( Debug.MSG_LIFECYCLE, "Current message-processor data flow:\n"
                               + rootProcessor.describe() );
                }
            }
            while ( true );

            // If no deferred-exceptions are cached, commit any changes.
            if ( batchedExceptions == null )
            {
                // Processing was successful, so all work should be commited.
                
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Committing all work performed against context." );

                context.commit( );
            }
        }
        catch ( Exception e )
        {
            response = null;

            //Populate exception into stat, if monitoring is enabled. 
            //if(stat!=null)
               // stat.setException(e);

            response = executeErrorHandler( context, request, e );

            Object test = getResponseValue( response );

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Error-handler returned a [" + getRelativeClassName( test ) + "]." );

            // If error-handler returned a non-null result that isn't an exception, skip
            // throwing of exception to parent server object - returning result instead.
            if ( (test == null) || (test instanceof Exception) )
            {
                // Since an error occurred, we must roll back any outstanding uncommited work.
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Rolling-back all work performed against context." );

                context.rollback( );

                // The error-handler returned an exception, which will be thrown in place
                // of the exception(s) that caused its invocation.
                if ( test instanceof Exception )
                {
                    e = (Exception)test;

                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_STATUS, "Exception-handler return the following exception to the driver, " +
                               "which will be thrown to driver's invoker:\n" + e.toString() );
                }

                if ( e instanceof MessageException )
                    throw (MessageException)e;

                if ( e instanceof ProcessingException )
                    throw (ProcessingException)e;

                throw new ProcessingException( "ERROR: Driver failed to process message:\n"
                                               + e.toString() );
            }
            else
            {
                // Error-handler has provided error-recovery, so commit results.
                context.commit( );

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Error-handler returned a non-null value, " +
                           "which will now be returned to parent server object." );
            }
        }
        catch ( Error reallyBadError )
        {

            // Exceptions of type 'Error' indicate a JVM-level system failure that is
            // most likely unrecoverable, so we'll just log the message and rethrow it.
            Debug.log( Debug.ALL_ERRORS, "ERROR: The following system-level error occurred:\n"
                       + reallyBadError.toString() );

            Debug.log( Debug.ALL_ERRORS, "Stack trace:\n" + Debug.getStackTrace(reallyBadError) );

            throw reallyBadError;
        }
        catch ( Throwable t )
        {
            Debug.log( Debug.ALL_ERRORS, t.toString() );

            Debug.log( Debug.ALL_ERRORS, Debug.getStackTrace( t ) );

            throw new Error( t.toString() );
        }
        finally
        {
            // Tell each message-processor to clean up after itself.
            cleanupProcessors( );

            if ( Debug.isLevelEnabled ( Debug.BENCHMARK ) )
                stopTimer( Debug.BENCHMARK, startTime, "Time for driver to process request ["
                           + driverConfigKey + ":" + driverConfigType + "]." );

            // Rename the thread back to its original value before returning.
            renameThread( previousThreadName );

            ThreadMonitor.stop( tmti );

            // Collect execution stats of driver sub-flow in case of monitoring is enabled.
            // (stat != null) it self represents monitoring is enabled.
            StatHelper.collectChainStats(stat);
        }

        // Check to see if we have any deferred exceptions and, if so, throw then now.
        Object temp = checkForExceptions( context, request );

        // If error-handler was available, and it performed error recovery (as indicated
        // by non-null value returned), return the given value.
        if ( temp != null )
            response = temp;

        return( getDriverResponse( response ) );
    }


    /**
     * Pop next value from the linked-list stack of values to be processed.
     *
     * @return  Name/value-pair on top of stack.
     */
    private final NVPair pop ( LinkedList messageStack )
    {
        return( (NVPair)messageStack.removeLast( ) );
    }


    /**
     * Push value onto the linked-list stack of values to be processed.
     *
     * @param  messageStack  Linked-list containing all outstanding messages to be processed.
     * @param  value  Name/value-pair to push on stack.
     */
    private final void push ( LinkedList messageStack, NVPair value )
    {
        messageStack.add( value );
    }


    /**
     * Push any result values onto the linked-list stack of values to be processed.
     *
     * @param  messageStack  Linked-list containing all outstanding messages to be processed.
     * @param  results  Values to push onto stack.
     *
     * @return  Object to be returned to comm-server parent of this driver, or null if
     *          response wasn't encountered.
     */
    private Object push ( LinkedList messageStack, NVPair[] results )
    {
        if ( resultsAvailable( results ) )
        {
            for ( int Ix = 0;  Ix < results.length;  Ix ++ )
            {
                // Don't process any null values here, as they're handled by the caller.
                if ( (results[Ix] == null) || (results[Ix].value == null) || (results[Ix].name == null) )
                {
                    if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                        Debug.log( Debug.MSG_DATA, "Encountered a null return value destined for message-processor ["
                                   + results[Ix].name + "], which will be skipped." );

                    continue;
                }

                // If message-processor returned a value with "NOBODY", ignore it now.
                if ( results[Ix].name.equals( MessageProcessorBase.NO_NEXT_PROCESSOR ) )
                {
                    if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                        Debug.log( Debug.MSG_DATA, "Skipping output with name ["
                                   + MessageProcessorBase.NO_NEXT_PROCESSOR + "]." );

                    continue;
                }

                // If we're synchronous and we've just found the message destined for the communications
                // server object (i.e., the response), we're done.
                if ( !asyncFlag && results[Ix].name.equals(TO_COMM_SERVER) )
                {
                    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                        Debug.log( Debug.MSG_STATUS, "Encountered result named [" + TO_COMM_SERVER
                                   + "], so returning it to originating server object." );

                    return( results[Ix].value );
                }

                // Push the result onto the outstanding-messages stack.
                push( messageStack, results[Ix] );
            }
        }

        return null;
    }


    /**
     * Locate the message-processor indicated by the name/value-pair, and execute it against the pair.
     *
     * @param  processors  Map containing all available message-processors.
     * @param  context  Context to execute message-processor against.
     * @param  input  Name/value-pair indicated processor to execute, and value to execute it against.
     *
     * @return  Array of name/value-pairs returned by message-processor.
     *
     * @exception  MessageException, ProcessingException  Thrown if processing fails.
     */
    private NVPair[] executeProcessor ( Map processors, MessageProcessorContext context, NVPair input )
                             throws MessageException, ProcessingException
    {
        if ( !StringUtils.hasValue(input.name) )
        {
            throw new ProcessingException( "ERROR: Message-processor returned a name/value-pair with a null name." );
        }

        MessageProcessorTree mpt = (MessageProcessorTree)processors.get( input.name );

        if ( mpt == null )
        {
            throw new ProcessingException( "ERROR: Driver can't find message-processor with name ["
                                           + input.name + "]." );
        }

        MessageObject mo = null;

        MessageProcessor mp = mpt.getProcessor( );

        Stat stat= null;
        try
        {
            Debug.log( Debug.NORMAL_STATUS, "Executing message-processor named: " + mp.getName());

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, context.describe( ) );

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Executing message-processor named [" + mp.getName() + "] of type ["
                           + getRelativeClassName(mp) + "]\nagainst input of type ["
                           + describeInputMessage(input.value) + "]." + BOLD_LINE );

            mo = getMessageObject( input.value );

            long startTime = startTimer( Debug.BENCHMARK );

            //Create stat object to collect execution stats of message processor for monitoring.
            //Stat object may be 'null' in case of corresponding message processor does'nt require to be monitored.
            //If message object is null then skip, since message processor wouldn't execute.
            if(mo!=null)
                stat= StatFactory.newMessageProcessorStat(driverConfigKey,mp.getName(),mp.getClass());

            NVPair[] results = mp.process( context, mo );

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, BOLD_LINE );

            if ( Debug.isLevelEnabled ( Debug.BENCHMARK ) )
                stopTimer( Debug.BENCHMARK, startTime, "Done executing message-processor named ["
                           + mp.getName() + "] of type [" + getRelativeClassName( mp ) + "]." );

            Performance.logMemoryUsage( Debug.MEM_USAGE, "" );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
            {
                Debug.log( Debug.MSG_DATA, "Results returned by message-processor:\n"
                           + describe( results ) );
            }

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, context.describe( ) );

            // Remember relationship between parent indicated by input and children indicated
            // by corresponding responses.
            if ( resultsAvailable( results ) )
                mpt.attach( results );

            Debug.log( Debug.NORMAL_STATUS, "Done executing message-processor named: " + mp.getName());

            return results;
        }
        catch ( Exception e )
        {
            // do not log BR error message again, as it should have been already logged by RuleProcessor
            if (ExceptionUtils.isBRError(e))
            {
                Debug.log( Debug.ALL_ERRORS, "ERROR: message-processor execution failed for [" + mp.getName() + "] of type ["
                       + getRelativeClassName(mp) + "]\nagainst input of type [" + describeInputMessage(input.value) + "]");
            }
            else // log other exception messages as usual
            {
                Debug.log( Debug.ALL_ERRORS, "ERROR: message-processor execution failed for [" + mp.getName() + "] of type ["
                       + getRelativeClassName(mp) + "]\nagainst input of type [" + describeInputMessage(input.value) + "]:\n"
                       + e.toString() );
                Debug.log( Debug.ALL_ERRORS, Debug.getStackTrace( e ) );
            }


            //if(stat!=null)
             //   stat.setException(e);

            // Remember the context in which the error occurred - if configured to
            // do so - for subsequent error-handler processing.
            rememberError( mp, mo, e );

            // If the defer-exceptions flag is 'true', cache the exception to throw
            // back to communications server parent at end of processing.
            if ( deferExceptionsFlag )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Caching exception to throw at end of processing ..." );

                if ( batchedExceptions == null )
                    batchedExceptions = new LinkedList( );

                batchedExceptions.add( e );

                return null;
            }
            else
            {
                if ( e instanceof MessageException )
                    throw (MessageException)e;

                if ( e instanceof ProcessingException )
                    throw (ProcessingException)e;

                throw new ProcessingException( "ERROR: Message-processor failed to process message:\n"
                                               + e.toString() );
            }
        }
        finally
        {
           // Collect execution stats of message processor in case of monitoring is enabled.
           // (stat != null) it self represents monitoring is enabled.
           StatHelper.collectMessageProcessorStats(stat);
        }
    }


	/**
	 * Create the message-processor objects as configured by the driver's properties.
     *
     * @exception  ProcessingException  Thrown on any errors.
	 */
	private void createProcessors ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log(Debug.MSG_LIFECYCLE, "Creating message-processors ..." );

        String propName = null;

        // Create a read-only version of the container of processors to give to
        // the tree nodes.
        Map constantProcessors = Collections.unmodifiableMap( processors );

        // Loop until no more message-processor related properties are found.
        for ( int Ix = 0;  true;  Ix ++ )
        {
            // Get the name of the message-processor class.
            propName = PersistentProperty.getPropNameIteration( MP_CLASS_NAME_TAG_PROP, Ix );

            String className = getProperty( propName );

            // If we can't find the current property-name iteration, we're done.
            if ( !StringUtils.hasValue( className ) )
                break;


            // Get the persistent-property key for the message-processor class.
            propName = PersistentProperty.getPropNameIteration( MP_KEY_PROP, Ix );

            String propKey = getProperty( propName );


            // Get the persistent-property type for the message-processor class.
			propName = PersistentProperty.getPropNameIteration( MP_TYPE_PROP, Ix );

			String propType = getProperty( propName );


            // Create the message-processor object.
            MessageProcessor mp = createMessageProcessor( className, propKey, propType );

			String name = mp.getName( );

            if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                Debug.log( Debug.MSG_LIFECYCLE, "Message-processor name [" + name + "]." );

            // All message-processors must have a name, so driver knows where to deliver
            // named input messages.
			if ( name == null )
            {
				throw new ProcessingException( "ERROR: Message-processor ["
                                               + mp.getClass().getName() + "] has null name." );
			}

            // Place the newly-created message-processor in the internal container, wrapped by a
            // tree node which is used to remember inter-processor relationships.
			MessageProcessorTree obj = (MessageProcessorTree)processors.put( name, new MessageProcessorTree( constantProcessors, mp ) );

            // Make sure that there are no other message-processors with the same name.
			if ( obj != null )
            {
				throw new ProcessingException( "ERROR: Driver encountered two message-processors with same name ["
                                               + name + "] during initialization: [" + obj.getProcessor().getClass().getName()
                                               + "] and [" + mp.getClass().getName() + "]." );
			}
        }

        // Log all available message-processors.
        if(Debug.isLevelEnabled( Debug.MSG_LIFECYCLE))
        {
            Debug.log(Debug.MSG_LIFECYCLE, "Done creating message-processors. Count: ["
                       + processors.size() + "]\nAvailable message-processors:\n" + describeProcessors() );
        }

        Object obj = processors.get( ROOT_MESSAGE_PROCESSOR );

        // Driver must know the 'root' message-processor to which the initial request from
        // its parent communications server object should be passed.
        if ( obj == null )
        {
            throw new ProcessingException( "ERROR: Driver's configuration must name one message-processor as ["
                                           + ROOT_MESSAGE_PROCESSOR + "]." );
        }
    }


	/**
	 * Create a single message-processor object.
     *
     * @param  className  Fully-qualified (package + class) name of message-processor to create.
     * @param  propKey    Persistent property key used to configure message-processor.
     * @param  propType   Persistent property type used to configure message-processor.
     *
     * @exception  ProcessingException  Thrown on any errors.
	 */
	private MessageProcessor createMessageProcessor ( String className, String propKey, String propType ) throws ProcessingException
    {
        try
        {
            if ( !StringUtils.hasValue(propKey) || !StringUtils.hasValue(propType) )
            {
                if ( Debug.isLevelEnabled ( Debug.MSG_LIFECYCLE ) )
                    Debug.log( Debug.MSG_LIFECYCLE, "Creating message-processor of type ["
                               + className + "] without property key/type values." );

                return( MessageProcessorFactory.create( className, propKey, propType, false ) );
            }
            else
            {
                if ( Debug.isLevelEnabled ( Debug.MSG_LIFECYCLE ) )
                    Debug.log( Debug.MSG_LIFECYCLE, "Creating message-processor of type ["
                               + className + "]\nusing property key [" + propKey
                               + "] and type [" + propType + "]." );

                return( MessageProcessorFactory.create( className, propKey, propType, loadConfig ) );
            }
        }
        catch ( Throwable t )
        {
            Debug.log( Debug.ALL_ERRORS, t.toString() );

            Debug.log( Debug.ALL_ERRORS, Debug.getStackTrace( t ) );

            throw new ProcessingException( t.toString() );
        }
    }


	/**
	 * Call cleanup() against all message-processors.
     *
     * @exception  ProcessingException  Thrown on any errors.
	 */
	private void cleanupProcessors ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log( Debug.MSG_LIFECYCLE, "Cleaning-up after message-processors ..." );

        MessageProcessorTree mpt = null;
        MessageProcessor     mp = null;
        String               errorMessage = null;

        // Loop over all message-processors, calling cleanup() against each one.
        Iterator iter = processors.values().iterator( );

        while ( iter.hasNext() )
        {
            try
            {
                mpt = (MessageProcessorTree)iter.next( );

                mp = mpt.getProcessor( );

                if ( Debug.isLevelEnabled ( Debug.MSG_LIFECYCLE ) )
                    Debug.log( Debug.MSG_LIFECYCLE, "Cleaning up message-processor ["
                               + mp.getName() + "] of type [" + mp.getClass().getName() + "]." );

                mp.cleanup( );
            }
            catch ( Exception e )
            {
                // Save error instead of throwing exception immediately to give all
                // message-processors a chance to clean up after themselves.
                if ( errorMessage == null )
                    errorMessage = "";
                else
                    errorMessage += "\n";

                errorMessage += "ERROR: Cleanup failed for message-processor ["
                    + mp.getClass().getName() + "]  of type ["
                    + mp.getClass().getName() + "]:\n" + e.toString();

                Debug.log( Debug.ALL_ERRORS, errorMessage );

                Debug.log( Debug.ALL_ERRORS, Debug.getStackTrace( e ) );
            }
        }

        if ( errorMessage != null )
        {
            throw new ProcessingException( errorMessage );
        }

        if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log( Debug.MSG_LIFECYCLE, "Done cleaning-up after message-processors." );
    }


    /**
     * Check to see if any batched message-processor exceptions are available, and
     * throw an exception with their contents if so.
     *
     * @param  context  Current message-processor context object.
     * @param  request  Original request message passed to driver from server.
     *
     * @return  Non-null value returned by error-handler message-processor, if available.
     *
     * @exception  MessageException, ProcessingException  Thrown if available.
     */
    private Object checkForExceptions ( MessageProcessorContext context, Object request ) throws MessageException, ProcessingException
    {
        // If there are no exceptions, do nothing.
        if ( batchedExceptions == null )
            return null;

        Exception error = null;

        // If we only have one exception, just throw it.
        if ( batchedExceptions.size() == 1 )
        {
            error = (Exception)batchedExceptions.get( 0 );
        }
        else
        {
            // We have more than one exception, so combine their
            // messages into an exception of the appropriate type.
            StringBuffer sb = new StringBuffer( );

            boolean messageError = false;
            boolean firstOne     = true;

            Iterator iter = batchedExceptions.iterator( );

            while ( iter.hasNext() )
            {
                Exception e = (Exception)iter.next( );

                if ( firstOne )
                    firstOne = false;
                else
                    sb.append( '\n' );

                sb.append( e.getMessage() );

                if ( e instanceof MessageException )
                    messageError = true;
            }

            if ( messageError == true )
                error =  new MessageException( sb.toString() );
            else
                error = new ProcessingException( sb.toString() );
        }

        // See if error-handler is available, and if it can provide error recovery.
        Object response = executeErrorHandler( context, request, error );

        Object test = getResponseValue( response );

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Error-handler returned a [" + getRelativeClassName( test ) + "]." );

        // If error-handler returned a non-null result that isn't an exception, skip
        // throwing of exception to parent server object - returning result instead.
        if ( (test == null) || (test instanceof Exception) )
        {
            context.rollback( );

            // The error-handler returned an exception, which will be thrown in place
            // of the exception(s) that caused its invocation.
            if ( test instanceof Exception )
            {
                error = (Exception)test;

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Exception-handler return the following exception to the driver, " +
                           "which will be thrown to driver's invoker:\n" + error.toString() );
            }
            else
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Throwing cached message-processor exceptions now:\n"
                           + error.toString() );

            // No error recovery was achieved, so just throw batched exception.
            if ( error instanceof MessageException )
                throw (MessageException)error;

            if ( error instanceof ProcessingException )
                throw (ProcessingException)error;

            throw new ProcessingException( error.getMessage() );
        }
        else
        {
            context.commit( );

            return response;
        }
    }


	/**
	 * Execute an error handler message-processor, if driver
     * is configured to have one.
     *
     * @param  context  Current message-processor context object.
     * @param  request  Original request message passed to driver from server.
     * @param  error    Exception that was thrown, causing error handler to be invoked.
     *
     * @return  Value returned by message-processor.
	 */
    private Object executeErrorHandler ( MessageProcessorContext context, Object request, Exception error )
    {
        // Do nothing if error-handler wasn't configured.
        if ( !StringUtils.hasValue( errorHandlerClassName ) )
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "No error-handling message-processor is indicated by property ["
                           + ERROR_PROCESSOR_CLASS_NAME_PROP + "]." );

            return null;
        }

        NVPair[] result = null;

        try
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Executing error-handler message-processor [" + errorHandlerClassName + "]." );

            String errLoc = getProperty( ERROR_PROCESSOR_EXCEPTION_LOC );

            if ( StringUtils.hasValue( errLoc ) )
                context.set( errLoc, error );
            else
            {
                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "No context error location was given via property ["
                               + ERROR_PROCESSOR_EXCEPTION_LOC + "]." );
            }

            // Add error-context container to message-processor-context at indicated location
            // so that error-handler can access it, if required.
            if ( StringUtils.hasValue( errorContextLocation ) && (errorContextContainer != null) )
            {
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Placing list of error-context objects of size ["
                               + errorContextContainer.size()
                               + "] into message-processor context at location [" + errorContextLocation + "]." );

                context.set( errorContextLocation, errorContextContainer );
            }

            String propKey = getProperty( ERROR_PROCESSOR_KEY_PROP );

            String propType = getProperty( ERROR_PROCESSOR_TYPE_PROP );

            MessageProcessor mp = createMessageProcessor( errorHandlerClassName, propKey, propType );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, context.describe( ) );

            // Execute message-processor.
            result = mp.process( context, getMessageObject(request) );

            mp.cleanup( );
        }
        catch ( Throwable t )
        {
            Debug.log( Debug.ALL_ERRORS, t.toString() );

            Debug.log( Debug.ALL_ERRORS, Debug.getStackTrace( t ) );
        }
        finally
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Done executing error-handler message-processor." );
        }

        // Extract single value from returned array (possibly encapsulated in MessageObject 'shell').
        if ( resultsAvailable( result ) )
            return( result[0].value );
        else
            return null;
    }


	/**
	 * Get a MessageObject-wrapped input.
     *
     * @param  input  The input to encapsulate in a MessageObject.
     *
     * @return  MessageObject containing input, or null if input is null.
	 */
    private MessageObject getMessageObject ( Object input )
    {
        // Make sure that all non-null object inputs are encapsulated in message objects.
        if ( input == null )
            return null;

        if ( input instanceof MessageObject )
            return( (MessageObject)input );
        else
            return( new MessageObject( input ) );
    }


	/**
	 * Get driver's response from the given argument.
     *
     * @param  response  The object to extract the driver's response from.
     *
     * @return  The driver's response.
     *
     * @exception  ProcessingException  Thrown on any conversion errors.
	 */
    private Object getDriverResponse ( Object response ) throws ProcessingException
    {
        if ( response == null )
            return null;

        try
        {
            // Extract value from message-object, if it is encapsulated in one.
            if ( response instanceof MessageObject )
                response = ((MessageObject)response).get( );

            // If configuration indicates that driver should return a string to caller,
            // perform any necessary conversion.
            String flag = getProperty( RETURN_STRING_PROP );

            if ( StringUtils.hasValue( flag ) && (StringUtils.getBoolean( flag ) == true) )
                response = Converter.getString( response );
        }
        catch ( Exception e )
        {
            String errMsg = "ERROR: Could not convert driver's response to appropriate format:\n"
                + e.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        return response;
    }


	/**
	 * Describe the outstanding messages to be processed.
     *
     * @param  messageStack  Name/value-pair list to describe.
     *
     * @return  String describing contents of argument.
	 */
	private String describe ( LinkedList messageStack )
    {
        StringBuffer sb = new StringBuffer( );

        ListIterator iter = messageStack.listIterator( messageStack.size() );

        while ( iter.hasPrevious() )
        {
            NVPair pair = (NVPair)iter.previous( );

            sb.append( "\tName [" );
            sb.append( pair.name );
            sb.append( "], type [" );
            sb.append( describeInputMessage(pair.value) );
            sb.append( "]\n" );
        }

        return( sb.toString() );
    }


	/**
	 * Get a human-readable description of name/value-pair array.
     *
     * @param  messages  Name/value-pair array to describe.
     *
     * @return  String describing contents of argument.
	 */
	private String describe ( NVPair[] messages )
    {
        StringBuffer sb = new StringBuffer( );

        if ( resultsAvailable( messages ) )
        {
            for ( int Ix = 0;  Ix < messages.length;  Ix ++ )
            {
                sb.append( "\tName [" );
                sb.append( messages[Ix].name );
                sb.append( "], type [" );
                sb.append( describeInputMessage(messages[Ix].value) );
                sb.append( "]\n" );
            }
        }
        else
            sb.append( "NONE" );

        return( sb.toString() );
    }


	/**
	 * Get a human-readable description of the given input message.
     *
     * @param  input  Object being used as input to message-processor.
     *
     * @return  String describing contents of argument, or null if null.
	 */
	private String describeInputMessage ( Object input )
    {
        if ( input == null )
            return null;

        StringBuffer sb = new StringBuffer( );

        sb.append( getRelativeClassName( input ) );

        if ( input instanceof MessageObject )
        {
            sb.append( '[' );

            try
            {
                sb.append( getRelativeClassName( ((MessageObject)input).get() ) );
            }
            catch ( Exception e )
            {
                sb.append( (String)null );
            }

            sb.append( ']' );
        }

        return( sb.toString() );
    }


	/**
	 * Describe message-processor names and types.
     *
     * @return  String describing available message-processors.
	 */
	private String describeProcessors ( )
    {
        StringBuffer sb = new StringBuffer( );

        Iterator iter = processors.values().iterator( );

        while ( iter.hasNext() )
        {
            MessageProcessorTree mpt = (MessageProcessorTree)iter.next( );

            MessageProcessor mp = mpt.getProcessor( );

            sb.append( "\tName [" );
            sb.append( mp.getName() );
            sb.append( "], type [" );
            sb.append( mp.getClass().getName() );
            sb.append( "]\n" );
        }

        return( sb.toString() );
    }


	/**
	 * Get the named property value.
     *
     * @param  name  Property name.
     *
     * @return  Property value, or null if not found.
	 */
    private final String getProperty ( String name )
    {
        return( (String)properties.get( name ) );
    }


    /**
     * Start the timer if the logging-level is enabled.
     *
     * @param  level  Logging-level at which output message will appear.
     */
    private final long startTimer ( int level )
    {
        if ( Debug.isLevelEnabled( level ) )
            return( System.currentTimeMillis() );
        else
            return( -1 );
    }


    /**
     * Stop the timer and log the elapsed time and message if the logging-level is enabled.
     *
     * @param  level      Logging-level at which output message will appear.
     * @param  startTime  Start-time bracketing code to benchmark.
     * @param  msg        Message to log.
     */
    private final void stopTimer ( int level, long startTime, String msg )
    {
        if ( Debug.isLevelEnabled( level ) )
        {
            long stopTime = System.currentTimeMillis( );

            Debug.log( level, "ELAPSED TIME [" + (stopTime - startTime)
                       + "] msec:  " + msg );
        }
    }


    /**
     * Test if any messages are available to be processed.
     *
     * @param  messages  Array of name/value-pairs to test.
     *
     * @return  'true' if messages is non-null with non-zero length, otherwise 'false'.
     */
    private final boolean resultsAvailable ( NVPair[] messages )
    {
        return( (messages != null) && (messages.length > 0) );
    }


    /**
     * Gets the relative name of the class for the given object.
     *
     * @param  obj  Object to get relative class name for.
     *
     * @return  Name of class without any package prefix.
     */
    protected static final String getRelativeClassName ( Object obj )
    {
        if ( obj == null )
            return null;

        String className = obj.getClass().getName( );

        int index = className.lastIndexOf( '.' );

        if ( index == -1 )
            return className;
        else
            return( className.substring( index + 1 ) );
    }


    /**
     * Remember all message-processor related information associated with an error.
     *
     * @param  processor  The message-processor that threw the exception.
     * @param  input  The input to the message-processor that resulted in the exception.
     * @param  error  The exception that was thrown.
     */
    private final void rememberError ( MessageProcessor processor, MessageObject input, Exception error )
    {
        // We only want to remember errors if configured to do so.
        if ( StringUtils.hasValue( errorContextLocation ) )
        {
            // Perform lazy initialization of error container.
            if ( errorContextContainer == null )
                errorContextContainer = new LinkedList( );

            // NOTE: Errors are added to the front of the list, so users iterating over
            // the list will see an error order from the most recent, to the oldest.
            errorContextContainer.addFirst( new ErrorContext( processor, input, error ) );

            if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                Debug.log( Debug.SYSTEM_CONFIG, "Remembering error-context object for subsequent error-handling.  " +
                           "Current remembered-exception count [" + errorContextContainer.size() + "]." );
        }
        else
            if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log( Debug.SYSTEM_CONFIG, "Driver not configured to remember error-contexts, so skipping it ..." );
    }


	/**
	 * Get the response value, extracting it from an encapsulating
     * message-object, if required.
     *
     * @param  response  The object to extract the response value from.
     *
     * @return  The extracted response value.
	 */
    private Object getResponseValue ( Object response )
    {
        if ( response == null )
            return null;

        try
        {
            // Extract value from message-object, if it is encapsulated in one.
            if ( response instanceof MessageObject )
                return( ((MessageObject)response).get( ) );
            else
                return response;
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, e.toString() );

            return response;
        }
    }


	/**
	 * Rename the current thread of execution.
     *
     * @param  previousThreadName  The name of the thread prior to any
     *                             previous renaming.  If null, the
     *                             thread will be renamed to include
     *                             its hash code value in the name in
     *                             order to make the name unique.
     *
     * @return  The thread's name prior to renaming.
	 */
    private String renameThread ( String previousThreadName )
    {
        String currentThreadName = Thread.currentThread().getName( );

        try
        {
            if ( StringUtils.hasValue( previousThreadName ) )
            {
                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Renaming thread [" +  currentThreadName
                               + "] back to [" + previousThreadName + "]." );

                Thread.currentThread().setName( previousThreadName );
            }
            else
            {
                // Adding a thread's hash code to its current name should make the name unique.
                String uniqueThreadName = currentThreadName + "-" + this.hashCode( );

                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Renaming thread [" + currentThreadName
                               + "] to [" + uniqueThreadName + "]." );

                Thread.currentThread().setName( uniqueThreadName );
            }
        }
        catch ( Exception e )
        {
            Debug.warning( "Thread renaming failed:\n" + e.toString() );
        }

        return currentThreadName;
    }


    // A collection of MessageProcessorTree nodes containing message-processors.
    private Map processors;

	private Hashtable properties;

    private MessageProcessorContext mpContext;

	private boolean asyncFlag;

    // Default value is to throw exceptions immediately.
    private boolean deferExceptionsFlag = false;

    // Container for any exception messages.
    private LinkedList batchedExceptions;

    // Name of the message-processor providing error-handler functionality (optional).
    private String errorHandlerClassName = null;

    // Container for ErrorContext objects.
    private LinkedList errorContextContainer;

    // The location in the context where the list of ErrorContext objects will be placed.
    private String errorContextLocation = null;

	// Flag to denote whether message processor's initialize method will be called.
	private boolean loadConfig = true;

    private String driverConfigKey;
    private String driverConfigType;

    private static final String BOLD_LINE =
        "\n===============================================================================\n";
}

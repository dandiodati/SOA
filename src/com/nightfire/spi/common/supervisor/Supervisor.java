
/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.spi.common.supervisor;

import java.util.*;

import com.nightfire.common.*;
import com.nightfire.servers.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.communications.*;


/**
 * Supervisor class is a singleton managing all Communication Servers in the gateway.
 */
public class Supervisor
{
    /**
     * The property tag that contains the class name of individial comm server.
     */
    public static final String CLASS_NAME_TAG     = "COMM_SERVER_CLASS";
    
    /**
     * The property tag that specifies Communication Server property key.
     */
    public static final String KEY_TAG            = "COMM_SERVER_KEY";
    
    /**
     * The property tag that specifies Communication Server property type.
     */
    public static final String TYPE_TAG           = "COMM_SERVER_TYPE";
    
    /**
     * The name of the thread group that all comm server threads are started in.
     */
    public static final String COMM_SERVER_THREAD_GROUP_NAME = "NFCommServers";


    /**
     * The constant that represents a start-up action.
     */
    private static final int STARTUP_ACTION       = 0;
    
    /**
     * The constant that represents a shutdown action.
     */
    private static final int SHUTDOWN_ACTION      = 1;

    private static Supervisor supervisor          = new Supervisor();
    private ArrayList servers                     = new ArrayList();
    private Map properties                        = null;
    private ServerBase loadingServer              = null;
    private int thread_cnt                        = 0;
    private CommServerThreadGroup commServerThreadGroup = null;
    
    
    /**
     * Singleton class.
     * Protected constructor
     */
    protected Supervisor() 
    {
        commServerThreadGroup = new CommServerThreadGroup( Thread.currentThread().getThreadGroup(), 
                                                           COMM_SERVER_THREAD_GROUP_NAME );
    }


    /**
     * Gets the Singleton instance.
     *
     * @return The singleton supervisor instance.
     *
     */
    public static Supervisor getSupervisor() 
    {
        return supervisor;
    }


    /**
     * Sets the corba server that starts the supervisor.
     *
     * @param 	The corba server that starts the supervisor.
     *
     */
    protected void setLoadingServer (ServerBase theLoader) 
    {
        loadingServer = theLoader;
    }


    /**
     * Gets the corba server that starts the supervisor.
     *
     * @return 	The corba server that starts the supervisor.
     *
     */
    protected ServerBase getLoadingServer () 
    {
        return loadingServer;
    }
    

    /**
     * Gets the CorbaPortabilityLayer class instance.
     * This class should be used in place of the BOA.
     * @return  The cpl instance.
     */
    public CorbaPortabilityLayer getCPL ()
    {
        if (loadingServer != null) 
            return loadingServer.getCorbaPortabilityLayer();

        Debug.log(Debug.ALL_WARNINGS, "WARNING: Loading server is not set.");
        return null;
    }
    

    /**
     * Loads configuration properties and performs initialization tasks.
     * 
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public void initialize(String key, String type) throws ProcessingException 
    {
        try 
        {
            properties = PersistentProperty.getProperties(key, type);
        }
        catch (PropertyException pe) 
        {
            throw new ProcessingException(pe);
        }
        
        for (int counter = 0; true; counter++) 
        {
            String theTag, theClassName, theKey, theType;
            
            theTag = PersistentProperty.getPropNameIteration(CLASS_NAME_TAG, counter);
            theClassName = getPropertyValue(theTag);

            if (!StringUtils.hasValue(theClassName)) 
            {
                // This processor will be skipped.
                if (Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) +
                                               ": Created [" + counter + "] communication servers.");
                break;
            }
            
            theTag = PersistentProperty.getPropNameIteration(KEY_TAG, counter);
            theKey = getRequiredPropertyValue(theTag);
            
            theTag = PersistentProperty.getPropNameIteration(TYPE_TAG, counter);
            theType = getRequiredPropertyValue(theTag);
            
            if (StringUtils.hasValue(theKey) && StringUtils.hasValue(theType))
            {
                // Do initialize only when key and type are not null.
                ComServerBase commServer = ComServerFactory.createServer(theClassName, theKey, theType);
                servers.add(commServer);
            }
            else
                throw new ProcessingException("Invalid key or type specified for " +
                    "communications server [" + theClassName + "].");
        }
    }
    

    /**
     * Starts the comm servers so that they are ready to processing request.
     * 
     * @exception  ProcessingException  Thrown if startup fails.
     */
    public void startup () 
    {
        try
        {
            manageServers(servers, STARTUP_ACTION);
        }
        catch ( Throwable t )
        {
            Debug.error( t.toString() + "\n" + Debug.getStackTrace( t ) );

            if ( t instanceof Error )
                throw (Error)t;
        }
    }


    /**
     * Shuts down the comm servers to stop to serving requests.
     */
    public void shutdown () 
    {
        try
        {
            manageServers(servers, SHUTDOWN_ACTION);
        }
        catch ( Throwable t )
        {
            Debug.error( t.toString() + "\n" + Debug.getStackTrace( t ) );

            if ( t instanceof Error )
                throw (Error)t;
        }
    }

    
    /**
     * Return the value for the property propName from the 
     * persistent properties (if it exists)
     *
     * @param propName The property whose value is to be returned
     *
     * @return String Value of propName
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
     * Return the value for the property propName from the persistent 
     * properties (if it exists)
     *
     * @param propName The property whose value is to be returned
     *
     * @param errorMsg Container for any errors that occur during proessing. Error
     *  messages are appended to this container instead of throwing exceptions
     *
     * @return String Value of propName
     */
    protected String getRequiredPropertyValue (String propName, StringBuffer errorMsg)
    {
        return PropUtils.getRequiredPropertyValue(properties, propName, errorMsg);
    }
    

    /**
     * Return the value for the property propName from the persistent properties
     *
     * @param propName The property whose value is to be returned
     *
     * @return String Value of propName
     */
    protected String getPropertyValue (String propName)
    {
        return PropUtils.getPropertyValue(properties, propName);
    }
    
    
    /**
     * Manipulates all comm servers objects.
     */
    private void manageServers(ArrayList theServers, int op)
    {
        for (Iterator iter = theServers.iterator(); iter.hasNext();) 
        {
            ComServerBase theServer = (ComServerBase) iter.next();
            manageServer(theServer, op);
        }
    }
    

    /**
     * Manipulates a single comm server object.
     */
    private void manageServer(ComServerBase theServer, int op) 
    {
        switch (op) 
        {
        case STARTUP_ACTION:
            try
            {
                Thread theThread = new Thread( commServerThreadGroup, theServer, 
                                               StringUtils.getClassName(theServer) + ",ID-" + (thread_cnt++) );
                theThread.setDaemon(true);
                theThread.start();
            }
            catch ( Exception e )
            {
                Debug.error( "ERROR: Communications server startup failed:\n" 
                             + e.toString() );

                Debug.error( Debug.getStackTrace( e ) );
            }
            break;
            
        case SHUTDOWN_ACTION:
            try
            {
                Debug.log( Debug.OBJECT_LIFECYCLE, "Shutting-down communications server object of type [" 
                           + theServer.getClass().getName() + "] ..." );

                theServer.shutdown();
                thread_cnt--;
            }
            catch ( Exception e )
            {
                Debug.error( "ERROR: Communications server shutdown operation failed:\n" 
                             + e.toString() );

                Debug.error( Debug.getStackTrace( e ) );
            }
            break;
            
        default:
            Debug.log(this, Debug.ALL_ERRORS, "ERROR: Unknown action passed to manageServer: " + op + ".");
        }
    }


    // A thread group used primarily to monitor uncaught exceptions
    // in the communications server threads.
    private class CommServerThreadGroup extends ThreadGroup
    {
        /**
         * Constructs a new thread group. The parent of this new group is 
         * the thread group of the currently running thread. 
         *
         * @param   name   The name of the new thread group.
         */
        public CommServerThreadGroup ( String name ) 
        {
            super( name );
            
            Debug.log( Debug.THREAD_LIFECYCLE, "Creating thread group ["  + 
                       name + "] of type [" + getClass().getName() + "]." );
        }
        
        
        /**
         * Creates a new thread group. The parent of this new group is the 
         * specified thread group. 
         * 
         * The checkAccess method of the parent thread group is 
         * called with no arguments; this may result in a security exception. 
         *
         * @param     parent   The parent thread group.
         * @param     name     The name of the new thread group.
         *
         * @exception  NullPointerException  If the thread group argument is null.
         * @exception  SecurityException  If the current thread cannot create a
         *                                thread in the specified thread group.
         */
        public CommServerThreadGroup ( ThreadGroup parent, String name ) 
        {
            super( parent, name );
            
            Debug.log( Debug.THREAD_LIFECYCLE, "Creating thread group ["  + 
                       name + "] of type [" + getClass().getName() + "]." );
        }
        
        
        /**
         * Called by the Java Virtual Machine when a thread in this 
         * thread group stops because of an uncaught exception.
         *
         * @param  t  The thread that is about to exit.
         * @param  e  The uncaught exception.
         */
        public void uncaughtException ( Thread t, Throwable e )
        {
            Debug.error( "ERROR: Thread [" + t.toString() + 
                         "] exiting due to the following uncaught exception:\n" + e.toString() );
            
            Debug.error( Debug.getStackTrace( e ) );
            
            super.uncaughtException( t, e );
        }
    }
}

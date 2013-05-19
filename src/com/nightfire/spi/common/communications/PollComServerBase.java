/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.spi.common.communications;

import java.util.*;
import java.io.File;
import com.nightfire.framework.db.*;
import com.nightfire.framework.timer.*;
import java.awt.event.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.communications.*;

/**
 * Server base class implementing timer-based polling suitable for
 * asynchronous message processing.
 */
public abstract class PollComServerBase extends ComServerBase implements ActionListener
{
    /**
     * Default time (in seconds) to wait between successive fires 
     */
    public static final int DEFAULT_TIMER_WAIT_TIME    = 30;

    /**
     *  Maps to timer property in PersistentProperty table.
     */
    public static final String TIMER                   = "TIMER";

    /**
     *  Maps to iterated timer property in PersistentProperty table.
     */
    public static final String CID_TIMER_PREFIX    = "CID_TIMER";

    /**
     *  Maps to iterated customer-id property in PersistentProperty table.
     */
    public static final String CUSTOMER_ID_PREFIX = TIMER + "_" + CustomerContext.CUSTOMER_ID_PROP;
    

    /**
     * Constructor that creates comm server object and loads its properties.
     *
     * @param  key  Key value used to access configuration properties.
     * @param  type  Type value used to access configuration properties.
     *
     * @exception  ProcessingException  Thrown on initialization errors.
     */
    public PollComServerBase (String key, String type) throws ProcessingException 
    {
        super(key,type);
        
        // First handle the default timer for backwards-compatibility.
        String time = getPropertyValue( TIMER );

        if ( StringUtils.hasValue( time ) )
        {
            timers.add( new CustomerTimer( getIntegerProperty(TIMER, DEFAULT_TIMER_WAIT_TIME) ) );
        }

        // Now, load configurations for any customer-specific timers.
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String cidTimerName = PersistentProperty.getPropNameIteration( CID_TIMER_PREFIX, Ix );

            time = getPropertyValue( cidTimerName );

            if ( StringUtils.hasValue( time ) )
            {
                timers.add( new CustomerTimer( getRequiredPropertyValue( PersistentProperty.getPropNameIteration( CUSTOMER_ID_PREFIX, Ix ) ),
                                               getIntegerProperty( cidTimerName, DEFAULT_TIMER_WAIT_TIME ) ) );
            }
            else
                break;
        }

        if ( timers.size() < 1 )
        {
            throw new ProcessingException( "No timer configuration information was provided." );
        }

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "Configured timers:\n" + timers.toString() );
    }


    /**
     * Executes the server object in a separate supervisor thread.
     * The implementation in this class starts the polling timers.
     */
    public void run() 
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + ": Initializing...");

        Iterator iter = timers.iterator( );

        while ( iter.hasNext() )
        {
            CustomerTimer ct = (CustomerTimer)iter.next( );

            try
            {
                ct.startTimer( this );
            }
            catch( Exception e )
            {
                Debug.error( e.toString() );
            }
        }
    }

    
    /**
     * Shuts-down the polling timers.
     */
    public void shutdown() 
    {
        Iterator iter = timers.iterator( );

        while ( iter.hasNext() )
        {
            CustomerTimer ct = (CustomerTimer)iter.next( );

            try
            {
                ct.stopTimer( );
            }
            catch( Exception e )
            {
                Debug.error( e.toString() );
            }
        }
    }
    
    
    /**
     * Called by Timer object when it expires.
     *
     * @param ActionEvent Event of an action listener
     */
    public void actionPerformed (ActionEvent ae) 
    {
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.IO_STATUS, StringUtils.getClassName(this) + 
                                       ": Processing timer expiration event at [" +
                                       DateUtils.getCurrentTime() + "].");

        CustomerTimer ct = null;

        Object actionSource = ae.getSource( );

        // Get the customer-timer information associated with the timer that called this action.
        if ( (actionSource != null) && (actionSource instanceof com.nightfire.framework.timer.Timer) )
        {
            com.nightfire.framework.timer.Timer sourceTimer = (com.nightfire.framework.timer.Timer)actionSource;

            Iterator iter = timers.iterator( );

            while ( iter.hasNext() )
            {
                CustomerTimer candidate = (CustomerTimer)iter.next( );

                if ( candidate.timer == sourceTimer )
                {
                    ct = candidate;

                    break;
                }
            }
        }
        else
            Debug.warning( "Action event source wasn't a timer." );


        // Temporarily stop the timer to prevent this method from being called
        // again while we're servicing current timer event.
        if ( ct != null )
            ct.timer.stop( );

        String previousCID = null;

        try
        {
            if ( (ct != null) && (ct.customerId != null) )
                previousCID = CustomerContext.getInstance().setCustomerID( ct.customerId );

            if( Debug.isLevelEnabled(Debug.MSG_STATUS) && (ct != null) )
                Debug.log( Debug.MSG_STATUS, "Processing timer event for [" + ct.toString() + "]." );

            processRequests();
        }
        catch (Exception pe)
        {
            Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                      ": Failed requests processing.\n" +
                      pe.getMessage());
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
            // Always reset the customer-id if it was previously set.
            if ( (ct != null) && (ct.customerId != null) )
            {
                try
                {
                    CustomerContext.getInstance().setCustomerID( previousCID );
                }
                catch( Exception e )
                {
                    Debug.error( e.toString() );
                }
            }
        }

        // Restart the timer so we'll be called again.
        if ( ct != null )
            ct.timer.restart( );
    }


    /**
     * Return the integer value for the property from the persistent properties
     * If property is not set, zero, not numeric or negative the default valuewill be returned.
     *
     * @param propName The property whose value is to be returned
     *
     * @param defaultValue  The default value to use if the property isn't available.
     *
     * @return  The integer property value.
     */
    protected int getIntegerProperty (String propName, int defaultValue)
    {
        String value = getPropertyValue(propName);

        if (StringUtils.hasValue(value)) 
        {
            try 
            {
                int result = Integer.parseInt(value);
                
                if (result > 0)
                    return result;
            }
            catch (NumberFormatException nfe) 
            {
                Debug.log(Debug.ALL_ERRORS, StringUtils.getClassName(this) + 
                                            ": Invalid integer value [" + value
                                            + "] for configuration property [" +
                                            propName + "].");
            }
        }

        return defaultValue;
    }
    

    /**
     * Get the customer-specific configuration, if available.
     * (Implicit key is thread-local CID set on the current CustomerContext.)  The
     * object returned is a leaf class-specific configuration container.
     *
     * @param  required  Flag indicating whether object is required or not.
     *
     * @return  The customer-specific configuration, or null if unavailable.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    protected synchronized Object getConfiguration ( boolean required ) throws ProcessingException
    {
        String cid;
        try{
             cid = CustomerContext.getInstance().getCustomerID( );
        }
        catch(FrameworkException fe)
        {
            throw new ProcessingException( fe );
        }
        return getConfiguration(cid, true);
    }

    /**
     * Get the customer-specific configuration, if available.
     * (Implicit key is thread-local CID set on the current CustomerContext.)  The
     * object returned is a leaf class-specific configuration container.
     *
     * @param  required  Flag indicating whether object is required or not.
     *         cid Customer identifier for which configuration is to be fetched.
     *
     * @return  The customer-specific configuration, or null if unavailable.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    protected synchronized Object getConfiguration ( String cid, boolean required ) throws ProcessingException
    {
        Object config = null;

        try
        {
            config = perCIDConfig.get( cid );

            if ( config == null )
            {
                config = loadConfiguration( );

                perCIDConfig.put( cid, config );
            }
         }
        catch ( Exception e )
        {
            throw new ProcessingException( e );
        }

        if ( (config == null) && required )
        {
            throw new ProcessingException( "Missing required per-CID configuration object." );
        }

        return config;
    }
    /**
     * Load the CID-specific configuration properties from the database.
     * (Implicit key is thread-local CID set on the current CustomerContext.)  The
     * object returned is a leaf class-specific configuration container.
     * This method should overridden in the leaf-class, and shouldn't be
     * called directly.  Instead, leaf-classes should call getConfiguration(),
     * which will call this method if it can't find a previously-loaded and
     * cached configuration object.
     *
     * @return  The customer-specific configuration, or null if unavailable.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    protected Object loadConfiguration ( ) throws FrameworkException
    {
        // Typically, leaf-classes should perform the following steps: 
        // Call CommServerBase.initialize( key, type ) to load any CID-specific properties.
        // Get the relevant property values and use them to create a leaf class-specific
        // configuration object, which is then returned by this method.

        // NOTE: This method should not call anything that requires a previously-loaded
        // customer-specific configuration in order to prevent infinite recursions and
        // resulting java.lang.StackOverflowError errors.

        Debug.warning( "Class [" + StringUtils.getClassName( this ) 
                       + "] doesn't load any CID-specific configuration properties for [" 
                       + key + ":" + type + "]." );

        return null;
    }


    // Used to encapsulate all configuration relevant to a customer-specific timer.
    private class CustomerTimer
    {
        public final String customerId; // Customer associated with timer (null if none).
        public final int repeatPeriod;  // Time in seconds between timer actions.

        public com.nightfire.framework.timer.Timer timer;

        private ActionListener listener;  // Who handles the timer expiration.

        public CustomerTimer( int period )
        {
            this( null, period );
        }

        public CustomerTimer( String cid, int period )
        {
            customerId= cid;
            repeatPeriod = period;
        }

        public void startTimer ( ActionListener listener )
        {
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, " Starting timer [" + toString() + "] ...");

            timer = new com.nightfire.framework.timer.Timer( repeatPeriod, listener );

            this.listener = listener;
        }

        public void stopTimer ( ) throws Exception
        {
            if ( timer != null )
            {
                if (Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log(Debug.IO_STATUS, " Stopping timer [" + toString() + "] ...");

                timer.stop( );

                if ( listener != null )
                    timer.removeListener( listener );

                timer.shutDown( );
            }
        }

        public String toString ( )
        {
            return( "CustomerTimer: CID [" + customerId + "], period [" + repeatPeriod + "] secs." );
        }
    }    


    /**
     * Callback invoked during timer expiration processing to provide
     * polling server class-specific processing.
     * Each child must implement this method.
     *
     * @exception  ProcessingException  Thrown on initialization errors.
     */
    protected abstract void processRequests() throws ProcessingException;

    
    // Container for default and customer-specific timers.
    private List timers = new LinkedList( );

    // Container of CID-specific configurations, keyed by CID.
    private Map perCIDConfig = new HashMap( );
}

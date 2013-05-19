/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.adapter.messageprocessor;


import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;


/**
 * Message-processor used to execute driver chains, which is useful 
 * for packaging sub-flows for treatment as first-class components.
 */
public class DriverMessageProcessor extends MessageProcessorBase
{
    /**
     * Property giving location where driver's input can be found.  
     * (Optional - Default input is current message-processor's input.)
     */
    public static final String INPUT_MESSAGE_LOCATION_PROP = "INPUT_MESSAGE_LOCATION";
    
    /**
     * Property giving driver's configuration key value.  
     * (Optional - Value may also be obtained from the context during execution.)
     */
    public static final String DEFAULT_DRIVER_KEY_PROP = "DEFAULT_DRIVER_KEY";
    
    /**
     * Property giving the location of the driver's configuration key value in the context.
     * (Optional - Value may also be obtained from default configuration value.)
     */
    public static final String DRIVER_KEY_CTX_LOC_PROP = "DRIVER_KEY_CTX_LOC";
    
    /**
     * Property giving driver's configuration type value.  
     * (Optional - Value may also be obtained from the context during execution.)
     */
    public static final String DEFAULT_DRIVER_TYPE_PROP = "DEFAULT_DRIVER_TYPE";
    
    /**
     * Property giving the location of the driver's configuration type value in the context.
     * (Optional - Value may also be obtained from default configuration value.)
     */
    public static final String DRIVER_TYPE_CTX_LOC_PROP = "DRIVER_TYPE_CTX_LOC";
    
    /**
     * Property indicating whether processor should enter into parent driver transaction,
     * or create a separate one.
     * (Optional - Default is 'true'.)
     */
    public static final String USE_PARENT_TRANSACTION_PROP = "USE_PARENT_TRANSACTION";

    /**
     * Property prefix giving target location for driver context initialization item.
     * (Optional - Default is to place target item in same location as source.)
     * Part of iterated property set.
     */
    public static final String TARGET_CTX_VALUE_LOC_PREFIX = "TARGET_CTX_VALUE_LOC";
    
    /**
     * Property prefix giving source location for driver context item initialization (optional).
     * Part of iterated property set.
     */
    public static final String SOURCE_CTX_VALUE_LOC_PREFIX = "SOURCE_CTX_VALUE_LOC";
    
    /**
     * Property prefix giving value of required flag for driver context item initialization.
     * (Optional - Default value is 'true'.)
     * Part of iterated property set.
     */
    public static final String SOURCE_CTX_REQUIRED_FLAG_PREFIX = "SOURCE_CTX_REQUIRED_FLAG";
    
    /**
     * Property prefix giving default value for driver context item initialization.  (Optional)
     * Part of iterated property set.
     */
    public static final String SOURCE_CTX_DEFAULT_VALUE_PREFIX = "SOURCE_CTX_DEFAULT_VALUE";
    
    /**
     * Property prefix giving target location for driver context item
     * that will be returned to parent driver's context from sub-driver's context.
     * (Optional - Default is to place target item in same location as source.)
     * Part of iterated property set.
     */
    public static final String RETURN_TARGET_CTX_VALUE_LOC_PREFIX = "RETURN_TARGET_CTX_VALUE_LOC";
    
    /**
     * Property prefix giving source location for driver context item 
     * that will be returned to parent driver's context from sub-driver's context.
     * Part of iterated property set.
     */
    public static final String RETURN_SOURCE_CTX_VALUE_LOC_PREFIX = "RETURN_SOURCE_CTX_VALUE_LOC";
    
    /**
     * Property prefix giving value of required flag for driver context item
     * that will be returned to parent driver's context from sub-driver's context.
     * (Optional - Default value is 'true'.)
     * Part of iterated property set.
     */
    public static final String RETURN_SOURCE_CTX_REQUIRED_FLAG_PREFIX = "RETURN_SOURCE_CTX_REQUIRED_FLAG";
    
    /**
     * Property prefix giving default value for driver context item 
     * that will be returned to parent driver's context from sub-driver's context.  (Optional)
     * Part of iterated property set.
     */
    public static final String RETURN_SOURCE_CTX_DEFAULT_VALUE_PREFIX = "RETURN_SOURCE_CTX_DEFAULT_VALUE";
    

    /**
     * Constructor.
     */
    public DriverMessageProcessor ( )
    {
        loggingClassName = StringUtils.getClassName( this );
    }
    
    
    /**
     * Called to initialize a message processor object.
     * 
     * @param  key   Property-key used to locate initialization properties.
     * @param  type  Property-type used to locate initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize( key, type );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Initializing ..." );
        
        StringBuffer errors = new StringBuffer( );
        
        // If input location is not given, the message-processor's input will be used.
        inputLocation = getPropertyValue( INPUT_MESSAGE_LOCATION_PROP );

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Input location [" + inputLocation + "]." );
        
        // Get the literal driver configuration values, or the locations 
        // where they can be found in the context.
        defaultDriverPropKey = getPropertyValue( DEFAULT_DRIVER_KEY_PROP );
        driverPropKeyCtxLoc = getPropertyValue( DRIVER_KEY_CTX_LOC_PROP );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Default driver key [" + defaultDriverPropKey 
                       + "], Context driver key [" + driverPropKeyCtxLoc + "]." );

        if ( !StringUtils.hasValue(defaultDriverPropKey) && !StringUtils.hasValue(driverPropKeyCtxLoc) )
            errors.append( loggingClassName + ": Missing driver property key configuration information." );
        
        defaultDriverPropType = getPropertyValue( DEFAULT_DRIVER_TYPE_PROP );
        driverPropTypeCtxLoc = getPropertyValue( DRIVER_TYPE_CTX_LOC_PROP );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Default driver type [" + defaultDriverPropType 
                       + "], Context driver type [" + driverPropTypeCtxLoc + "]." );

        if ( !StringUtils.hasValue(defaultDriverPropType) && !StringUtils.hasValue(driverPropTypeCtxLoc) )
            errors.append( loggingClassName + ": Missing driver property type configuration information." );

        // Determine the parent-child driver transaction relationship.
        useParentTransaction = true;
        
        String temp = getPropertyValue( USE_PARENT_TRANSACTION_PROP );
        
        if ( StringUtils.hasValue( temp ) )
        {
            try
            {
                useParentTransaction = StringUtils.getBoolean( temp );
            }
            catch ( Exception e )
            {
                errors.append( e.getMessage() );
            }
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Using parent driver transaction? [" + useParentTransaction + "]." );
        
        // While iterated properties are present, get all of the sub-driver context configuation items.
        // These properties indicate any values whose references should be copied from the parent-driver context
        // to the sub-driver context.
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String childCtxValueLoc = getPropertyValue( PersistentProperty.getPropNameIteration( TARGET_CTX_VALUE_LOC_PREFIX, Ix ) );
            String parentCtxValueLoc = getPropertyValue( PersistentProperty.getPropNameIteration( SOURCE_CTX_VALUE_LOC_PREFIX, Ix ) );
            String parentCtxReqFlag = getPropertyValue( PersistentProperty.getPropNameIteration( SOURCE_CTX_REQUIRED_FLAG_PREFIX, Ix ) );
            String parentCtxDefaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( SOURCE_CTX_DEFAULT_VALUE_PREFIX, Ix ) );
            
            // One of the two locations must have a value, or we're done.
            if ( !StringUtils.hasValue( parentCtxValueLoc ) && !StringUtils.hasValue( childCtxValueLoc ) )
            {
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Got [" + Ix + "] source-target sub-driver context mappings." );

                break;
            }

            // Initialize the container of context configurations in a lazy fashion.
            if ( contextConfigurations == null )
                contextConfigurations = new LinkedList( );
            
            // Default is that source item is required.
            boolean required = true;
            
            if ( StringUtils.hasValue( parentCtxReqFlag ) )
            {
                try
                {
                    required = StringUtils.getBoolean( parentCtxReqFlag );
                }
                catch ( Exception e )
                {
                    errors.append( e.getMessage() );
                }
            }
            
            // If no target location was given, use the source location as the target location.
            if ( !StringUtils.hasValue( childCtxValueLoc ) )
                childCtxValueLoc = parentCtxValueLoc;

            ContextConfig cc = new ContextConfig( childCtxValueLoc, parentCtxValueLoc, required, parentCtxDefaultValue );

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Sub-driver context configuration item: " + cc.describe() );

            // Remember the context configuration item for later driver execution.
            contextConfigurations.add( cc );
        }
        

        // While iterated properties are present, get all of the parent-driver context configuation items.
        // These properties indicate any values whose references should be copied back from the sub-driver
        // context to the parent-driver context.
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String parentCtxValueLoc = getPropertyValue( PersistentProperty.getPropNameIteration( RETURN_TARGET_CTX_VALUE_LOC_PREFIX, Ix ) );
            String childCtxValueLoc = getPropertyValue( PersistentProperty.getPropNameIteration( RETURN_SOURCE_CTX_VALUE_LOC_PREFIX, Ix ) );
            String childCtxReqFlag = getPropertyValue( PersistentProperty.getPropNameIteration( RETURN_SOURCE_CTX_REQUIRED_FLAG_PREFIX, Ix ) );
            String childCtxDefaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( RETURN_SOURCE_CTX_DEFAULT_VALUE_PREFIX, Ix ) );
            
            // One of the two locations must have a value, or we're done.
            if ( !StringUtils.hasValue( childCtxValueLoc ) && !StringUtils.hasValue( parentCtxValueLoc ) )
            {
                if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                    Debug.log( Debug.MSG_STATUS, "Got [" + Ix + "] source-target parent-driver context mappings." );

                break;
            }

            // Initialize the container of context configurations in a lazy fashion.
            if ( returnContextConfigurations == null )
                returnContextConfigurations = new LinkedList( );
            
            // Default is that source item is required.
            boolean required = true;
            
            if ( StringUtils.hasValue( childCtxReqFlag ) )
            {
                try
                {
                    required = StringUtils.getBoolean( childCtxReqFlag );
                }
                catch ( Exception e )
                {
                    errors.append( e.getMessage() );
                }
            }
            
            // If no target location was given, use the source location as the target location.
            if ( !StringUtils.hasValue( parentCtxValueLoc ) )
                parentCtxValueLoc = childCtxValueLoc;

            ContextConfig cc = new ContextConfig( parentCtxValueLoc, childCtxValueLoc, required, childCtxDefaultValue );

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Parent-driver context configuration item: " + cc.describe() );

            // Remember the context configuration item for later driver execution.
            returnContextConfigurations.add( cc );
        }


        // If any configuration errors were present, signal that fact to caller.
        if ( errors.length() > 0 )
        {
            throw new ProcessingException( errors.toString() );
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Done initializing." );
    }
    
    
    /**
     * Execute the indicated driver chain as configured.
     * 
     * @param  context  The  message context.
     * @param  input  Input message object to process.
     *
     * @return  An array of name-value pair objects, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input )
        throws MessageException, ProcessingException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Processing ..." );

        // We're not batching, so there's nothing to do for null inputs.
        if ( input == null )
            return null;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Creating sub-driver ..." );

        // Create and initialize the driver to execute.
        MessageProcessingDriver driver = new MessageProcessingDriver( );

        String key  = getConfigValue( context, driverPropKeyCtxLoc, defaultDriverPropKey );
        String type = getConfigValue( context, driverPropTypeCtxLoc, defaultDriverPropType );

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Initializing sub-driver ..." );

        driver.initialize( key, type );

        // Default is to use parent context, unless configured to do otherwise.
        MessageProcessorContext subDriverContext = context;

        // This keeps track of whether the parent context is the transaction
        // owner. This is used to reset the context as the transaction owner
        // (if necessary) when processing is complete. 
        boolean parentOwnsTransaction = context.isTransactionOwner();

        // If any source/target context configurations are given, or the configuration has indicated that a
        // separate transaction should be used, the sub-driver should create and use a separate context object.
        if ( hasConfigurations( contextConfigurations ) || hasConfigurations( returnContextConfigurations ) 
             || !useParentTransaction )
        {
            // Get a new context from the sub-driver.
            subDriverContext = driver.getContext( );
            
            // Perform any further parent-to-sub-driver context initializations, as configured.
            if ( hasConfigurations( contextConfigurations ) || hasConfigurations( returnContextConfigurations ) )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Populating sub-driver context from parent-driver context." );
            
                configureContext( subDriverContext, context, contextConfigurations );
            }
            else
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Sub-driver context using all data from parent-driver context." );

                subDriverContext.inheritParentData( context );
            }

            // If the sub-driver should piggy-back on the parent driver's transaction, copy the 
            // database connection reference from the parent context to the child context.
            // This also marks the child context as not owning the transaction.
            if ( useParentTransaction )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "Sub-driver is inheriting transaction from parent-driver context." );

                subDriverContext.inheritParentTransaction( context );
            }
        }
        else
        {
            // No special configuration is present, so use the parent driver context
            // when executing the child driver.
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Sub-driver is using parent-driver's context 'as is'." );

            // Give the sub-driver the parent driver's context to use.
            driver.setContext( context );

            // Disable commit/rollback against context.
            context.setTransactionOwner( false );
        }

        Object result = null;
        
        try
        {
            // Default input to driver is this processor's input.
            Object request = input;
        
            // If input location was given in configuration, get the 
            // driver's input from the indicated location.
            if ( StringUtils.hasValue( inputLocation ) )
                request = get( inputLocation, context, input );

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Executing sub-driver ..." );
            
            // Execute the driver.
            result = driver.process( request );
        }
        finally
        {
            // Reset the transaction ownership flag on the parent
            // context in case the sub-driver was executed against it.
            context.setTransactionOwner( parentOwnsTransaction );
        }
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Successfully executed sub-driver.\n"+
                    "Populating parent-driver context from sub-driver context." );
        
        // Copy back any context items from the sub-driver to the parent-driver, as configured.
        if ( context != subDriverContext )
            configureContext( context, subDriverContext, returnContextConfigurations );
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Done processing." );
        
        // Return any results as name/value-pairs.
        return( formatNVPair( result ) );
    }
    
    
    /**
     * Configure the target context using the source context and context configuration items.
     * 
     * @param  targetContext  The context acting as the target for configuration items.
     * @param  sourceContext  The context acting as the source of configuration items.
     * @param  contextConfigs  A list of context configuration items.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    private void configureContext ( MessageProcessorContext targetContext, MessageProcessorContext sourceContext, 
                                    List contextConfigs )
        throws MessageException, ProcessingException
    {
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Configuring context ..." );
        
        if ( targetContext == sourceContext )
        {
            throw new ProcessingException( "ERROR: Source and target contexts can't be the same for configuration copy operation." );
        }

        // There's nothing to do if no context configurations were given.
        if ( !hasConfigurations( contextConfigs ) )
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "No additional context configuration required from source context." );
            
            return;
        }
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Configuring target context from source context ..." );
        
        // Iterate over context configurations.
        Iterator iter = contextConfigs.iterator( );
        
        // While context configurations are available ...
        while ( iter.hasNext() )
        {
            ContextConfig cc = (ContextConfig)iter.next( );
            
            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Current context configuration item: " + cc.describe() );
            
            Object value = null;
            
            // If the source-location was given and it exists in the source context, get
            // the corresponding value to place in the target context.
            if ( StringUtils.hasValue( cc.sourceLoc ) && sourceContext.exists( cc.sourceLoc ) )
                value = sourceContext.get( cc.sourceLoc );
            
            // If the value wasn't available in the source context, use the default value (if available).
            if ( value == null )
                value = cc.defaultValue;
            
            // If the value was found, set it in the configured location in the target context.
            if ( value != null )
                targetContext.set( cc.targetLoc, value );
            else
            {
                // Value wasn't found, so signal error if configuration indicates that it is required.
                if ( cc.requiredFlag )
                    throw new MessageException( "ERROR: Missing context configuration item: " + cc.describe() );
                else
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log( Debug.MSG_STATUS, "Skipping absent context configuration, since it isn't required." );
            }
        }
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, loggingClassName + ": Done configuring target context." );
    }
    

    /**
     * Determine if given list has configuration items to process.
     * 
     * @param  configs  List of context configuration items.
     *
     * @return  'true' if list is non-empty, otherwise 'false'.
     */
    private boolean hasConfigurations ( List configs )
    {
        if ( (configs == null) || (configs.size() == 0) )
            return false;
        else
            return true;
    }


    /**
     * Get a configuration item from either the context or a default property configuration value.
     * NOTE: The context value takes precedence over the default value if both are present.
     * 
     * @param  context  The context to get the configuration value from.
     * @param  contextLocation  The location in the context where the configuration value is located.
     * @param  defaultValue  The value to use if the configuration item isn't present in the context.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    private String getConfigValue ( MessageProcessorContext context, String contextLocation, String defaultValue )
        throws MessageException, ProcessingException
    {
        String value = defaultValue;
        
        if ( StringUtils.hasValue( contextLocation ) && context.exists( contextLocation ) )
            value = context.getString( contextLocation );
        
        if ( !StringUtils.hasValue( value ) )
        {
            throw new ProcessingException( "ERROR: Couldn't obtain configuration item from context location [" 
                                           + contextLocation + "], and no default was given in properties." );
        }
        
        return value;
    }
    
    
    // A class used to collect context configuration information in one place.
    private static class ContextConfig
    {
        public final String targetLoc;
        public final String sourceLoc;
        public final boolean requiredFlag;
        public final String defaultValue;
        
        public ContextConfig ( String targetLoc, String sourceLoc, boolean requiredFlag, String defaultValue )
        {
            this.targetLoc    = targetLoc;
            this.sourceLoc    = sourceLoc;
            this.requiredFlag = requiredFlag;
            this.defaultValue = defaultValue;
        }
        
        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );
            
            sb.append( "target-location [" );
            sb.append( targetLoc );
            sb.append( "], source-location [" );
            sb.append( sourceLoc );
            sb.append( "], required? [" );
            sb.append( requiredFlag );
            sb.append( "], default-value [" );
            sb.append( defaultValue );
            sb.append( "]" );
            
            return( sb.toString() );
        }
    }
    
    
    private String inputLocation;
    
    private String defaultDriverPropKey;
    private String driverPropKeyCtxLoc;
    private String defaultDriverPropType;
    private String driverPropTypeCtxLoc;

    // Default is to use parent transaction.
    private boolean useParentTransaction = true;

    private List contextConfigurations;
    private List returnContextConfigurations;
    
    // Abbreviated class name for use in logging.
    private String loggingClassName;
}

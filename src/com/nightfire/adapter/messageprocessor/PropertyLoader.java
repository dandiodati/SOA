package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.lang.reflect.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;


/**
 * This class searches in the persistent properties for iterating property sets
 * each of which must contain a CONTEXT_LOCATION_#, a CONTEXT_VALUE_#, and a
 * DESCRIPTION_#. The # signifies a number in a sequence starting from 0 where
 * all of the properties of the same # number are directly associated.
 * 
 * Within each set of numbered properties, the value of the CONTEXT_VALUE property
 * will be put into the context at the value of the CONTEXT_LOCATION property.
 * The value of the DESCRIPTION property is not used by this processor, but acts
 * as a place where a business analyst building or maintaining a gateway can
 * place a comment describing the purpose of the particular value being placed
 * in the context for each iterating property set.
 */


public class PropertyLoader extends MessageProcessorBase
{
    /**
     * The (iterative) property where the location in the context where a value
     * will be placed is specified.
     */
 	  public static final String CONTEXT_LOCATION_PROP = "CONTEXT_LOCATION";

    /**
     * The (iterative) property where the value which will be placed in the 
     * context is specified. 
     */
 	  public static final String CONTEXT_VALUE_PROP = "CONTEXT_VALUE";
    /**
     * The (iterative) property where the description of the meaining of the particular
     * value being placed in the context can be specified.
     */
 	  public static final String DESCRIPTION_PROP = "DESCRIPTION";

    /**
     * The character which seperates the name of an iterating style property
     * from the index of the interating property.
     */
 	  public static final char ITERATING_PROPERTY_DELIMITER = '_';

    /**
     * Flag used to ensure an instance of this processor will be called only once.
     */
    private boolean processed = false;

   /**
    * Retrieves properties from the database.
    *
    * @param key   A string that represents the name of this processor.
    *
    * @param type  A string which specifies the type of this processor.
    *
    * @exception ProcessingException   Thrown if the specified properties
    *                               cannot be found or are invalid.
    */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        //Load properties
        if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "PropertyLoader: Initializing.....");
        super.initialize ( key, type );
    } //initialize

    /**
     * This class searches in the persistent properties for iterating property sets
     * each of which must contain a CONTEXT_LOCATION_#, a CONTEXT_VALUE_#, and a
     * DESCRIPTION_#. The # signifies a number in a sequence starting from 0 where
     * all of the properties of the same # number are directly associated.
     * 
     * Within each set of numbered properties, the value of the CONTEXT_VALUE property
     * will be put into the context at the value of the CONTEXT_LOCATION property.
     * The value of the DESCRIPTION property is not used by this processor, but acts
     * as a place where a business analyst building or maintaining a gateway can
     * place a comment describing the purpose of the particular value being placed
     * in the context for each iterating property set.
     *
     * Input message is not modified.
     *
     * @param  input  Input message to process.
     *
     * @param  context The context
     *
     * @return  Optional NVPair containing a Destination name and a Document,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     */
     public NVPair[] process ( MessageProcessorContext context, MessageObject input )
         throws ProcessingException, MessageException
    {

        if( !processed )
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log ( Debug.MSG_STATUS, "PropertyLoader: Processing.....");

            int propCount = 0;
            NVPair [] propertySet = null;
            String contextLocation = null;
            String contextValue = null;

            // process sets of iterating properties for as long as we can find them.
            while ( true ) 
            {
                contextLocation = null;
                contextValue = null;
                propertySet = PersistentProperty.getGroupedProperties(adapterProperties, 
                                                 "" + ITERATING_PROPERTY_DELIMITER + propCount);  
                if((null == propertySet) || (0 == propertySet.length))
                {
                    // we have no more sets of properties, so stop here. 
                    break;
                }
                else
                {
                    // we don't know what order the property set is in, 
                    // so loop through them and extract the values for 
                    // CONTEXT_LOCATION and CONTEXT_VALUE
                    for(int i = 0 ; i < propertySet.length ; i++)
                    {
                        if(propertySet[i].name.equals(CONTEXT_LOCATION_PROP + 
                                                      ITERATING_PROPERTY_DELIMITER + propCount))
                        {
                            contextLocation = (String)propertySet[i].value;
                        }
                        else if(propertySet[i].name.equals(CONTEXT_VALUE_PROP +
                                                           ITERATING_PROPERTY_DELIMITER + propCount))
                        {
                            contextValue = (String)propertySet[i].value;
                        }
                    }
                    propCount ++;
                    // CONTEXT_START + an empty string us not a valid context location
                    // an empty string is a valid value in the context.
                    if( !StringUtils.hasValue(contextLocation) || (null == contextValue))
                    {
                        Debug.log(Debug.ALL_WARNINGS, "Couldn't extract context location " +
                                  "or context value from properties group [" +
                                  propCount + "]. Skipping the property set");
                        continue;
                    }
                }
                //Load the value in the context
                try 
                {
                    set( CONTEXT_START + contextLocation, context, null, contextValue );
                }
                catch (ProcessingException e)
                {
                    Debug.log ( Debug.ALL_ERRORS, "ERROR: PropertyLoader: process: Cannot set context. " + e.getMessage () );
                    throw new ProcessingException( "ERROR: PropertyLoader: process: Cannot set context. " + e.getMessage () );
                }
                catch (MessageException e)
                {
                    Debug.log ( Debug.ALL_ERRORS, "ERROR: PropertyLoader: process: Cannot set context. " + e.getMessage () );
                    throw new MessageException( "ERROR: PropertyLoader: process: Cannot set context. " + e.getMessage () );
                }

            } // end while true

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log ( Debug.MSG_STATUS, "PropertyLoader: Finished processing.");

            processed = true;

        }//end if(!processed)
        else
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "This processor has already been used. Skipping the Loading operations");
        }
        // send the result on through the chain according to properties.
        if(input == null)
        {
             return null;
        }
        return formatNVPair ( input );

    } //process

} //PropertyLoader

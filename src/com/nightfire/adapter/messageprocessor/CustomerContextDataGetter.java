package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.PersistentProperty;

import com.nightfire.spi.common.driver.*;
import org.w3c.dom.Document;

import java.util.HashMap;

/**
 * This takes an input location(s) and sets value from customer context
 * to the specified location(s). This is used
 * where the data from Customer Context is required.
*/
public class CustomerContextDataGetter extends MessageProcessorBase
{

    // Property prefix giving location of input data.
    public static final String INPUT_LOC_PREFIX_PROP = "INPUT_LOC";

    // Property prefix giving location where fetched data is to be set.
    public static final String OUTPUT_LOC_PREFIX_PROP = "OUTPUT_LOC";

    // Property prefix for default value to be used.
    public static final String DEFAULT_PREFIX_PROP = "DEFAULT";

    public static HashMap<String, String> ioLocs, inputLocsDefaultVal;

    /**
    * Initializes the properties.
    *
    * @param  key   Property-key to use for locating initialization properties.
    * @param  type  Property-type to use for locating initialization properties.
    * @exception ProcessingException when initialization fails
    */
    public void initialize(String key, String type) throws ProcessingException
    {
        super.initialize(key, type);

        StringBuffer errorBuffer = new StringBuffer( );
        ioLocs = new HashMap<String, String>();
        inputLocsDefaultVal = new HashMap<String, String>();

        //Loop until all configuration properties have been read.
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String inputLoc, outputLoc, defaultValue;

            inputLoc = getPropertyValue( PersistentProperty.getPropNameIteration( INPUT_LOC_PREFIX_PROP, Ix ) );
            outputLoc = getPropertyValue( PersistentProperty.getPropNameIteration( OUTPUT_LOC_PREFIX_PROP, Ix ) );

            defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_PREFIX_PROP, Ix ) );
            //if neither input or output locations are found, we are done.
            if ( !StringUtils.hasValue( inputLoc ) && !StringUtils.hasValue( outputLoc ) )
                break;

            // if either of the io location is missing then continue with next value
            if ( !StringUtils.hasValue( inputLoc ) || !StringUtils.hasValue( outputLoc ))
            {
                errorBuffer.append( "ERROR: Missing required input/output-location configuration item." );
                continue;
            }

            if ( StringUtils.hasValue( inputLoc ) )
            {
                ioLocs.put(inputLoc, outputLoc);
                inputLocsDefaultVal.put(inputLoc, StringUtils.hasValue(defaultValue) ? defaultValue : "");
            }

        }//for

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
        {
            Debug.log(Debug.SYSTEM_CONFIG, "CustomerContextDataGetter initialized.");
        }
    }

    /**
    * This takes the the input location(s)
    * retrieve them from the customerContext and
    * sets that value at specified location(s).
    */
    public NVPair[] process(MessageProcessorContext context, MessageObject input )
                        throws MessageException, ProcessingException
    {
        // the traditional message processor response to a null input
        if(input == null) return null;

        try
        {
            Document doc = null;

            // on passing null documemt CustomerContext will create new one
            Document ctxHeader = CustomerContext.getInstance().propagate(doc);
            XMLPlainGenerator xmlPG = new XMLPlainGenerator(ctxHeader);

            for (String inputLoc : ioLocs.keySet())
            {
                String outputLoc = ioLocs.get(inputLoc);
                String defaultValue = inputLocsDefaultVal.get(inputLoc);

                String value;

                if (StringUtils.hasValue(inputLoc))
                {
                    try
                    {
                        value = xmlPG.getValue(inputLoc);

                        // if no value obtained from context then default would be set
                        if (!StringUtils.hasValue(value) && StringUtils.hasValue(defaultValue))
                            value = defaultValue;

                        if (!StringUtils.hasValue(value)) value = "";

                        set(outputLoc, context, input, value);

                        Debug.log(Debug.MSG_STATUS, "Value obtained from CustomerContext for location [" + inputLoc + "]" +
                            ", is [" + value + "]" +
                            ", storing at location [" + outputLoc + "]" +
                            ", against default value [" + defaultValue + "].");
                    }
                    catch (MessageException e)
                    {
                        Debug.warning("Unable to retrieve the value for [" + inputLoc + "], skipping it...");
                    }
                }
                else Debug.warning("Unable to obtain inputLoc so skipping this one and continue with next...");
            }//for
        }
        catch(FrameworkException fex)
        {
            throw new ProcessingException("Could not set the customer context data to given output location(s) as per input location(s)." + fex.getMessage());
        }

        return ( formatNVPair( input ) );
    }

    public static void main(String[] args)
    {
        try
        {
            CustomerContext cc = CustomerContext.getInstance();
            cc.setCustomerID("CH");
            cc.setInterfaceVersion("LSOG6");
            cc.setSubDomainId("SubDomain");
            cc.setUserID("test");
            cc.setUserPassword("test");

            Document doc = null;
            Document ctxHeader = cc.propagate(doc); // on passing null documemt CustomerContext will create one
            String ctxHeaderStr = XMLLibraryPortabilityLayer.convertDomToString(ctxHeader);
            System.out.println("XMLLibraryPortabilityLayer.convertDomToString(ctxHeader) = " + ctxHeaderStr);
            XMLPlainGenerator xmlPG = new XMLPlainGenerator(ctxHeader);
            // Obtain CustomerId and SubDoaminId from context. 
            System.out.println("xmlPG.getValue(CustomerIdentifier) [" + xmlPG.getValue("CustomerIdentifier") + "]");
            System.out.println("xmlPG.getValue(SubDomainId) [" + xmlPG.getValue("SubDomainId") + "]");

            try
            {
                System.out.println("xmlPG.getValue(xyz) [" + xmlPG.getValue("xyz") + "]");
            }
            catch (MessageException e)
            {
                Debug.warning( "Unable to retrieve the value form [xyz], setting it to empty string");
                System.out.println("xmlPG.getValue(xyz) [default]");
            }

        }
        catch (FrameworkException e)
        {
            e.printStackTrace(); 
        }
    }
}
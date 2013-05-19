/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import java.util.*;
import java.text.*;
import java.io.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.message.*;
import com.nightfire.adapter.converter.*;
import com.nightfire.adapter.converter.messageprocessor.*;

/**
 * This is a message processor for applying the XMLToEDIConverter within a Gateway.
 */
public class XMLtoEDIConverterProcessor extends AbstractConverterProcessor
{
    /**
     * Property from which to get the Segment Separator.
     *
     */
    public static final String SEGMENT_SEPARATOR_PROP = "SEGMENT_SEPARATOR";

    /**
     * Property from which to get the Element Separator.
     *
     */
    public static final String ELEMENT_SEPARATOR_PROP = "ELEMENT_SEPARATOR";

    /**
     * Property from which to get the Composite Element Separator.
     *
     */
    public static final String COMPOSITE_ELEMENT_SEPARATOR_PROP = "COMPOSITE_ELEMENT_SEPARATOR";

    /**
     * Property designating from where to get the Segment Separator.
     *
     */
    public static final String SEGMENT_SEPARATOR_LOCATION_PROP = 
                               "SEGMENT_SEPARATOR_LOCATION";

    /**
     * Property designating from where to get the Element Separator.
     *
     */
    public static final String ELEMENT_SEPARATOR_LOCATION_PROP = 
                               "ELEMENT_SEPARATOR_LOCATION";

    /**
     * Property designating from where to get the Composite Element Separator.
     *
     */
    public static final String COMPOSITE_ELEMENT_LOCATION_SEPARATOR_PROP = 
                              "COMPOSITE_ELEMENT_SEPARATOR_LOCATION";

    /**
     * Property Value which designates a Line Feed.
     *
     */
    public static final String LF_PROPERTY_VALUE = "\\n";

    /**
     * Property Value which designates a Carriage Return.
     *
     */
    public static final String CR_PROPERTY_VALUE = "\\r";

    /**
     * Property Value which designates a Carriage Return / Line Feed.
     *
     */
    public static final String CRLF_PROPERTY_VALUE = "\\r\\n";

    /**
     * Member variable to hold the location from where  to retrieve the  Segment Separator.
     *
     */
    private String segmentSeparatorLocation;

    /**
     * Member variable to hold the location from where to retrieve the  Element Separator.
     *
     */
    private String elementSeparatorLocation;

    /**
     * Member variable to hold the location from where to retrieve the Composite Element Separator.
     *
     */
    private String compositeElementSeparatorLocation;

    /**
     * Member variable to hold the configured Segment Separator.
     *
     */
    private String segmentSeparator;

    /**
     * Member variable to hold the configured Element Separator.
     *
     */
    private String elementSeparator;

    /**
     * Member variable to hold the configured Composite Element Separator.
     *
     */
    private String compositeElementSeparator;

    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        Debug.log( Debug.SYSTEM_CONFIG, "XMLtoEDIConverterProcessor: Initializing..." );

        StringBuffer errorBuffer = new StringBuffer( );

        // null out the separator context locations in case this MP has been cached.        
        segmentSeparatorLocation = null;
        elementSeparatorLocation = null;
        compositeElementSeparatorLocation = null;

        // Attempt to get the delimiter context locations from the properties.
        // context locations are optional.
        segmentSeparatorLocation = getPropertyValue( SEGMENT_SEPARATOR_LOCATION_PROP );
        elementSeparatorLocation = getPropertyValue(ELEMENT_SEPARATOR_LOCATION_PROP );
        compositeElementSeparatorLocation = 
                 getPropertyValue(COMPOSITE_ELEMENT_LOCATION_SEPARATOR_PROP);

        // For each context location that does not exist, get the value from the 
        // persistent properties itself.
        if(null == segmentSeparatorLocation)
        {
            segmentSeparator = getRequiredPropertyValue( SEGMENT_SEPARATOR_PROP, errorBuffer );
        }
        if(null == elementSeparatorLocation)
        {
            elementSeparator = getRequiredPropertyValue(ELEMENT_SEPARATOR_PROP , errorBuffer );
        }
        if(null == compositeElementSeparatorLocation)
        {
            compositeElementSeparator = getRequiredPropertyValue(COMPOSITE_ELEMENT_SEPARATOR_PROP , 
                                                                 errorBuffer );
        }

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "XMLtoEDIConverterProcessor: Initialization done." );
    }


    /**
     * Build a converter to be used by the process() method of the parent class.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The converter to be used by the process() method of the parent class.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public Converter getConverter (MessageProcessorContext context, MessageObject message)
                        throws MessageException, ProcessingException 
    {
        getSeparatorsFromContext(context, message);
        Debug.log( Debug.MSG_STATUS, "XMLtoEDIConverterProcessor: processing ... " );
        XMLtoEDIConverter converter = new XMLtoEDIConverter();
        if(segmentSeparator.equals(CR_PROPERTY_VALUE))
        {
            segmentSeparator = EDIMessageConstants.CR;
        }
        else if(segmentSeparator.equals(LF_PROPERTY_VALUE))
        {
            segmentSeparator = EDIMessageConstants.LF;
        }
        else if(segmentSeparator.equals(CRLF_PROPERTY_VALUE))
        {
            segmentSeparator = EDIMessageConstants.CRLF;
        }
        converter.setSegmentSeparator(segmentSeparator);
        converter.setElementSeparator(elementSeparator);
        converter.setCompositeElementSeparator(compositeElementSeparator);
        return converter;
    }
    
    /**
     * For each separator which has a location specified,
     * retrieve the value for it.
     *
     * @param mpContext The MessageProcessorContext object needed for retrieval if 
     *                  the separator is specified as comming from the 
     *                  context.
     * @param inputObject The input message needed for retrieval if 
     *                  the separator is specified as comming from the 
     *                  input message.
     * @exception MessageException if an error occurs
     * @exception ProcessingException if an error occurs
     */
    private void getSeparatorsFromContext(MessageProcessorContext mpContext, MessageObject inputObject)
	throws MessageException, ProcessingException
    {
        // For each context location that exists, get the value from the 
        // from the context.
        if(null != segmentSeparatorLocation)
        {
            segmentSeparator = (String) get(segmentSeparatorLocation, mpContext, inputObject);
        }
        if(null != elementSeparatorLocation)
        {
            elementSeparator = (String) get(elementSeparatorLocation, mpContext, inputObject);
        }
        if(null != compositeElementSeparatorLocation)
        {
            compositeElementSeparator = (String) get(compositeElementSeparatorLocation, mpContext, inputObject);
        }
    }
}



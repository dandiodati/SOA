package com.nightfire.adapter.messageprocessor;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;

/**
  * Extracts one or more strings from specified input locations
  * and puts the concatenated string at a specified output location
  */
public class StringConcatenator extends MessageProcessorBase
{
    /**
     * Loads the required property values into memory
     *
     * @param  key   Property Key to use for locating initialization properties.
     * @param  type  Property Type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, "StringConcatenator: Initializing ...");

        //initialize the base class to enable invocation of other utility methods
        super.initialize(key, type);
        
        //the location where the concatenated string will be put
        outputLocation = getRequiredPropertyValue ( OUTPUT_LOCATION_PROP );

        //optional separator character to separate individual strings during concatenation
        separator = getPropertyValue ( SEPARATOR_PROP );

        //Used to store the locations data
        locations = new LinkedList();
        
        //loop until all locations and default values have been extracted
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String required = getPropertyValue ( PersistentProperty.getPropNameIteration ( REQUIRED_PROP, Ix ) );
            String stringLocation = getPropertyValue( PersistentProperty.getPropNameIteration( STRING_LOCATION_PROP , Ix ) ) ;
            String staticValue  = getPropertyValue( PersistentProperty.getPropNameIteration( STATIC_STRING_VALUE_PROP , Ix ) );

            //stop when no more properties are specified
            if ( !StringUtils.hasValue( required ) && !StringUtils.hasValue ( stringLocation ) && !StringUtils.hasValue ( staticValue ) )
                break;

            try
            {
                //create a new locations data and add it to the list
                LocationsData ld = new LocationsData(  Ix , stringLocation , staticValue, required );
                
                locations.add( ld );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "Could not create the locations data description:\n" + e.toString() );
            }

        }

    }


  /**
   * Sets a concatenated string into the specified location ( context or message object )
   *
   * @param context The MessageProcessorContext with required information
   *
   * @param msgObj The MesageObject to be sent to the next processor
   *
   * @return NVPair[] Name-Value pair array of next processor name and the MessageObject passed in
   *
   * @exception ProcessingException Thrown if processing fails
   */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.BENCHMARK))
            Debug.log(Debug.BENCHMARK, "StringConcatenator: processing ...");

        if ( msgObj == null )
        {
            return null;
        }

        //buffer to hold all the partial strings
        StringBuffer concatBuffer = new StringBuffer();
        String partialString;
        
        Iterator iter = locations.iterator( );
        
        while ( iter.hasNext() )
        {
            partialString = null;
            
            LocationsData ld = ( LocationsData ) iter.next( );

            //check if the location is valid
            if ( ld.location !=null && exists( ld.location, context, msgObj ) )
                partialString = getString ( ld.location, context, msgObj ) ;    

            //if the string can't be extracted, use the static value
            if ( partialString == null )
            {
                partialString  = ld.staticValue;
            }

            //complain when the string is required and it isn't found yet
            if ( ( ld.required == true)  && ( partialString == null ) )
            {
                if ( ld.location != null )
                {
                    throw new ProcessingException("StringConcatenator: Required string value could not be located at [" + ld.location +"]");
                }
                else 
                {    
                    throw new ProcessingException("StringConcatenator: Required string value missing for property [" +
                                                  PersistentProperty.getPropNameIteration( STATIC_STRING_VALUE_PROP , ld.index ) +"]" ); 
                }
            }

            if(Debug.isLevelEnabled(Debug.UNIT_TEST))
                Debug.log(Debug.UNIT_TEST, "StringConcatenator: Concatenating the partial string [" + partialString  +"]" );

            if ( partialString != null )
            {
                //Add the partial string
                concatBuffer.append ( partialString );

                //Add the separator if exists 
                if ( StringUtils.hasValue ( separator ) && ( ld.index != locations.size() -1 ) )
                {
                    concatBuffer.append ( separator );
                }
                
            }
            
        }

        String concatenatedString = concatBuffer.toString();
        
        if(Debug.isLevelEnabled(Debug.BENCHMARK))
        Debug.log( Debug.BENCHMARK, "StringConcatenator: Setting the concatenated String ["
                   +concatenatedString +"] to the location [" +outputLocation +"]" );
        
        //set the concatenated string to the context or to the message object
        this.set( outputLocation , context, msgObj, concatenatedString );

        //return the message object
        return( formatNVPair ( msgObj ) );
        

    }
    
    /**
     * Class LocationsData stores the input locations and default values
     */
    private static class LocationsData
    {

        public final int index;
        public final String location;
        public final String staticValue;
        public final boolean required;
        
        public LocationsData ( int index, String location, String staticValue, String required )
            throws FrameworkException
        {
            
            this.index        = index;
            this.location     = location;
            this.staticValue  = staticValue;
            this.required     = StringUtils.getBoolean( required );

        }

    }

    /*
     * Property prefix to determine whether the string to be concatenated is required or not
     */
    private static final String REQUIRED_PROP = "REQUIRED";
    /*
     * Property prefix to be used as the location to extract the string to be concatenated
     */
    private static final String STRING_LOCATION_PROP = "STRING_VALUE_LOCATION";
    /*
     * Property prefix to be used as the static value for the string to be concatenated
     */
    private static final String STATIC_STRING_VALUE_PROP = "STATIC_STRING_VALUE";
    
    /*
     * Property used for the output location for the concatenated string
     */
    private static final String OUTPUT_LOCATION_PROP = "CONCATENATED_OUTPUT_LOCATION";
    /*
     * Property used for the separator in between the strings to be concatenated
     */
    private static final String SEPARATOR_PROP = "SEPARATOR";

    private String separator;
    private String outputLocation;
    private List locations;
} 

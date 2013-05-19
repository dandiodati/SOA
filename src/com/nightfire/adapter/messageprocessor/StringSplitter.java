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
public class StringSplitter extends MessageProcessorBase
{
    

    /*
     * Property prefix to determine whether the split string is required
     */
    private static final String END_OF_STRING = "END_OF_STRING";

    /*
     * Property prefix to determine whether the split string is required
     */
    private static final String REQUIRED_PROP = "REQUIRED";
    
    
    /*
     * Property prefix to be used as the location to extract the original string to be split
     */
    private static final String INPUT_LOCATION_PROP = "INPUT_LOCATION";
    
    /*
     * Property used for the output location for the split string
     */
    private static final String OUTPUT_LOCATION_PROP = "OUTPUT_LOCATION";

    /*
     * Property used to determine whether to split the string by using sub string
     */
    private static final String SUBSTRING_FROM_PROP = "SUBSTRING_FROM";

    /*
     * Property used to determine whether to split the string by using sub string
     */
    private static final String SUBSTRING_TO_PROP = "SUBSTRING_TO";

    /*
     * Property prefix for the separator used to split the strings
     */
    private static final String TOKENIZER_PROP = "TOKENIZER";

    private String tokenizer;
    private String inputLocation;
    private List locations;
    private boolean isAnyOutputRequired = false;


    /**
     * Loads the required property values into memory
     *
     * @param  key   Property Key to use for locating initialization properties.
     * @param  type  Property Type to use for locating initialization properties.
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        if ( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) )
            Debug.log(Debug.OBJECT_LIFECYCLE, "StringSplitter: Initializing ...");

        //initialize the base class to enable invocation of other utility methods
        super.initialize(key, type);
        
        //the location where the concatenated string will be put
        inputLocation = getRequiredPropertyValue ( INPUT_LOCATION_PROP );

        //optional separator character to separate individual strings during concatenation
        tokenizer = getPropertyValue ( TOKENIZER_PROP );

        //Used to store the locations data
        locations = new LinkedList();
        
        //loop until all locations and default values have been extracted
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String required = getPropertyValue ( PersistentProperty.getPropNameIteration ( REQUIRED_PROP, Ix ) );
            String outputLocation = getPropertyValue( PersistentProperty.getPropNameIteration( OUTPUT_LOCATION_PROP , Ix ) ) ;
	    String fromPosition = getPropertyValue ( PersistentProperty.getPropNameIteration ( SUBSTRING_FROM_PROP, Ix ) );
	    String toPosition = getPropertyValue ( PersistentProperty.getPropNameIteration ( SUBSTRING_TO_PROP, Ix ) );

            //stop when no more properties are specified
            if ( !StringUtils.hasValue( required ) && !StringUtils.hasValue ( outputLocation ) &&
		 !StringUtils.hasValue ( fromPosition ) && !StringUtils.hasValue ( toPosition ) )
                break;

            try
            {
                //create a new locations data and add it to the list
                LocationsData ld = new LocationsData(  Ix , outputLocation ,  fromPosition , 
						       toPosition, required );

                if (ld.required)
                    isAnyOutputRequired = true;

                locations.add( ld );
            }
            catch ( Exception e )
            {
                Debug.log(Debug.ALL_ERRORS, "Could not create the locations data description:\n" + e.toString() );
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
        if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
            Debug.log(Debug.BENCHMARK, "StringSplitter: processing ...");

        if ( msgObj == null )
        {
            return null;
        }

        //Check whether the input location and input value are valid
        if ( inputLocation==null || !exists( inputLocation, context, msgObj ) || !StringUtils.hasValue(getString ( inputLocation, context, msgObj )))
        {
            // Throw exception only when there is some output location which is mandatory, otherwise skip the input processing and let driver chain go forward.
            if (isAnyOutputRequired)
            {
                Debug.log(Debug.ALL_ERRORS, "StringSplitter: Given input at input-location [" + inputLocation+ "] is invalid. There is at least one mandatory output, hence throwing Exception." );
                throw new ProcessingException ("StringSplitter: Given input/ input-location is invalid " );
            }
            else
            {
                Debug.log(Debug.ALL_WARNINGS, "StringSplitter: Given input/ input-location is invalid. Skipping input processing as none of the output is mandatory.");
                return( formatNVPair ( msgObj ) );
            }
        }

        // Check whether input value at the given location is valid 
        String originalString = getString ( inputLocation, context, msgObj );
        
        String splitString = null;
        Iterator iter = locations.iterator( );
        
        while ( iter.hasNext() )
        {
	    
            LocationsData ld = ( LocationsData ) iter.next( );


            if ( ld.subStringTo.equals(END_OF_STRING) )
            {
                splitString = originalString.substring ( Integer.parseInt (ld.subStringFrom),																		originalString.length() );
            }
            else {
                splitString = originalString.substring ( Integer.parseInt ( ld.subStringFrom ),
                                    Integer.parseInt ( ld.subStringTo ) );
            }

            //complain when the string is required and it isn't found yet
            if ( ( ld.required == true)  && ( splitString == null ) )
            {
                Debug.log(Debug.ALL_ERRORS, "StringSplitter: could not get the sub string of " + originalString );
                throw new ProcessingException("StringSplitter: could not get the sub string of " + originalString );
            }

            //set the concatenated string to the context or to the message object
            this.set( ld.outputLocation , context, msgObj, splitString );
	
        }
        

        //return the message object
        return( formatNVPair ( msgObj ) );
        

    }
    
    /**
     * Class LocationsData stores the input locations and default values
     */
    private static class LocationsData
    {

        public final int index;
        public final String outputLocation;
        public final String subStringFrom;
        public final String subStringTo;
        public final boolean required;
        
        public LocationsData ( int index, String outputLocation, 
			       String subStringFrom, String subStringTo,  String required )
            throws FrameworkException
        {
            
            this.index        = index;
            this.outputLocation     = outputLocation;
            this.subStringFrom  = subStringFrom;
	    this.subStringTo  = subStringTo;
            this.required     = StringUtils.getBoolean( required );

        }

    }

} 

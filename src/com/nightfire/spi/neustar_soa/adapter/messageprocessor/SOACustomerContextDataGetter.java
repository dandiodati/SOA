package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.spi.common.driver.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * This takes an input location(s) and sets value from customer context
 * to the specified location(s). This is used
 * where the data from Customer Context is required.
*/
public class SOACustomerContextDataGetter extends MessageProcessorBase
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
            Debug.log(Debug.SYSTEM_CONFIG, "SOACustomerContextDataGetter initialized.");
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
    	
    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        	Debug.log( Debug.MSG_STATUS, "SOACustomerContextDataGetter: processing ... " );
    	
        // the traditional message processor response to a null input
        if(input == null) return null;

        try
        {
            for (String inputLoc : ioLocs.keySet())
            {
                String outputLoc = ioLocs.get(inputLoc);
                String defaultValue = inputLocsDefaultVal.get(inputLoc);

                String value;

                if (StringUtils.hasValue(inputLoc))
                {
                    try
                    {
                        value = (String)CustomerContext.getInstance().get(inputLoc);

                        // if no value obtained from context then default would be set
                        if (!StringUtils.hasValue(value) && StringUtils.hasValue(defaultValue))
                            value = defaultValue;

                        if (!StringUtils.hasValue(value)) 
                        	value = "";

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

    public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "D:\\logmap.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		try {
			DBInterface.initialize(
					"jdbc:oracle:thin:@192.168.148.34:1521:NOIDADB",
					"soadb", "soadb");

		} catch (DatabaseException e) {

			Debug.log(null, Debug.MAPPING_ERROR, ": "
					+ "Database initialization failure: " + e.getMessage());

		}

		SOACustomerContextDataGetter sOACustomerContextDataGetter = new SOACustomerContextDataGetter();
		
		
		
		try {
			CustomerContext.getInstance().set("newDueDate", "04-21-2010-114500AM");
			
			sOACustomerContextDataGetter.initialize("SOA_VALIDATE_REQUEST","SOACustomerContextDataGetter");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("spid", "1111");
			mob.set("useCustomerId", "ACME_GW");
			mob.set("userId", "example");
			mob.set("subrequest", "SvCreateRequest");
			mob.set("passTN","540-001-2367");
			mob.set("failedTN",	"");
			mob.set("inputMessage", "<?xml version=\"1.0\"?>"
					+ "<SOAMessage>"
					+ "<UpstreamToSOA>"
					+ "<UpstreamToSOAHeader>"
					+ "<InitSPID value=\"1111\" />"
					+ "<DateSent value=\"09-01-2006-035600AM\" />"
					+ "<Action value=\"submit\" />"
					+ "</UpstreamToSOAHeader>"
					+ "<UpstreamToSOABody>"
					+ "<SvCreateRequest>"
					
					+ "<Subscription> "
					
					+ "<Tn value=\"305-591-3696\" />"
										
					+ "</Subscription>"		
					
					+ "<LnpType value=\"lspp\" />"
					+ "<Lrn value=\"9874563210\" />"
					+ "<NewSP value=\"A111\" />"
					+ "<OldSP value=\"1111\" />"
					+ "<NewSPDueDate value=\"08-12-2006-073800PM\" />" 
					
					+ "</SvCreateRequest>" 
					+ "</UpstreamToSOABody>" 
					+ "</UpstreamToSOA>" 
					+ "</SOAMessage>");
			
			sOACustomerContextDataGetter.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());

		} catch (FrameworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}

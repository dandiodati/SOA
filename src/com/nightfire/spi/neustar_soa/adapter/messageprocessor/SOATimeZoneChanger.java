/**
 * This message processor takes times from locations in the
 * MessageObject/MessageProcessorContext, changes the time zone
 * and/or format and puts them into locations in the
 * MessageObject/MessageProcessorContext.
 * 
 * @author Ravi Madan Sharma
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair
 * @see			com.nightfire.spi.common.driver.MessageObject
 * @see			com.nightfire.spi.common.driver.MessageProcessorContext
 * @see			com.nightfire.spi.common.driver.MessageProcessorBase

 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ravi.M			07/19/2004			Created
	2 			Ravi.M			07/19/2004			Review comments incorporated
	3			Ravi.M			07/29/2004			Formal review comments 
													incorporated.

		
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

/**
 * This message processor takes times from locations in the
 * MessageObject/MessageProcessorContext, changes the time zone
 * and/or format and puts them into locations in the
 * MessageObject/MessageProcessorContext.
 */
public class SOATimeZoneChanger extends MessageProcessorBase {

	
	/**
	 * Value For Input and Output Time Zone.
	 */
	private TimeZone inputTimeZone;

	private TimeZone outputTimeZone;

	/**
	 * Value For Input and Output Format. 
	 */
	private String inputFormat = null;

	private String outputFormat = null;

	private List getSetList;

	/**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;

	/**
	 * Constructor.
	 */
	public SOATimeZoneChanger() 
	{
		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
				Debug.log(
				Debug.OBJECT_LIFECYCLE,
				"Creating SOATimeZoneChanger message-processor.");
		}

		getSetList = new ArrayList();

	}

   /**
	* Initializes this object via its persistent properties.
	*
	* @param  key   String Property-key to use for locating initialization properties.
	* @param  type  String Property-type to use for locating initialization properties.
	*
	* @exception ProcessingException when initialization fails
	*/
	public void initialize( String key, String type )
		throws ProcessingException 
		{
		// Call base class method to load the properties.

		super.initialize( key, type );
        
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "SOATimeZoneChanger: Initializing...");
		}

		StringBuffer errorBuffer = new StringBuffer();

		String timeZone = getRequiredPropertyValue( 
							SOAConstants.INPUT_TIME_ZONE_PROP, errorBuffer );

		if (timeZone.equalsIgnoreCase( SOAConstants.LOCAL_TIMEZONE )) {
			inputTimeZone = TimeZone.getDefault();
		} else {
			inputTimeZone = TimeZone.getTimeZone(timeZone);
		}

		timeZone = getRequiredPropertyValue( 
						SOAConstants.OUTPUT_TIME_ZONE_PROP, errorBuffer );

		if (timeZone.equalsIgnoreCase( SOAConstants.LOCAL_TIMEZONE )) {
			outputTimeZone = TimeZone.getDefault();
		} else {
			outputTimeZone = TimeZone.getTimeZone( timeZone );
		}

		inputFormat = getRequiredPropertyValue( 
							SOAConstants.INPUT_FORMAT_PROP, errorBuffer );

		outputFormat =
			getRequiredPropertyValue( SOAConstants.OUTPUT_FORMAT_PROP, errorBuffer );
			
		String 	inputLoc = null;
		String outputLoc = null;

		//	Loop until all location properties have been read.
		for (int Ix = 0; true; Ix++) {
		  inputLoc =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.INPUT_LOC_PREFIX_PROP,
						Ix));
		  outputLoc =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.OUTPUT_LOC_PREFIX_PROP,
						Ix));
			// allow the user to input only the input location 
			// if the output location is the same as the input:
			if (!StringUtils.hasValue( outputLoc )) {
				outputLoc = inputLoc;
			}

			//if neither input or output locations are found, we are done.
			if (!StringUtils.hasValue( inputLoc )
				&& !StringUtils.hasValue( outputLoc ))
				{
					break;
				}

			try {
				// Create a new TimeZoneChangerConfig object and 
				// add it to the list.
				SOATimeZoneChangerConfig tzcc =
					new SOATimeZoneChangerConfig( inputLoc, outputLoc );
                
				if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
					Debug.log(Debug.SYSTEM_CONFIG, tzcc.describe());

				getSetList.add(tzcc);
			} catch (FrameworkException e) {
				throw new ProcessingException(
					"ERROR: Could not create getSetValues:\n" + e.toString());
			}

		} //for

		// Make sure at least 1 get-set configuration has been initialized.
		if (getSetList.size() < 1)
		{
		
			errorBuffer.append(
				"ERROR: No get-set configuration values were given.");
		}

		// If any of the required properties are absent, indicate 
		// error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"SOATimeZoneChanger: Initialization done.");
		}
	}

	/**
	 * Extract values from one set of locations from the 
	 * context/input/default value
	 * and associate them with new locations in the context/input.
	 *
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 */
	public NVPair[] process(
		MessageProcessorContext mpContext,
		MessageObject inputObject)
		throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;

		if (inputObject == null)
		{
			
			return null;
			
		}
		
		this.mpContext = mpContext;

		this.inputObject = inputObject;

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "SOATimeZoneChanger: processing ... ");
		}
        try
        {
        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
        
		// Iterate over get-set configurations.
		Iterator iter = getSetList.iterator();
		
		String convertedTime = null;
		
		SOATimeZoneChangerConfig tzcc = null;

		while (iter.hasNext()) {
			 tzcc =
				(SOATimeZoneChangerConfig) iter.next();

			String sourceValue = null;

			if (StringUtils.hasValue(tzcc.inputLoc))
				sourceValue = getValue(tzcc.inputLoc);

			// If we still don't have a value signal error to caller.
			if ( SOAUtility.isNull(sourceValue) ){
				continue;
			}

			convertedTime = convertTime(sourceValue);
			

			// Put the result in its configured location.
			set(tzcc.outputLoc, mpContext, inputObject, convertedTime);

		}
        }
        finally
        {
        	ThreadMonitor.stop(tmti);
        }
		// Always return input value to provide pass-through semantics.
		return (formatNVPair(inputObject));
	}


	/**
	 * Extract first value available from the given locations.
	 *
	 * @param  locations  A set of '|' delimited XML locations to check.
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  The requested value.
	 *
	 * @exception  MessageException  Thrown on non-processing errors.
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	protected String getValue(
		String locations)
		throws MessageException, ProcessingException {
		StringTokenizer st =
			new StringTokenizer(locations, MessageProcessorBase.SEPARATOR);

			String tok = null;
		while (st.hasMoreTokens()) {
			 tok = st.nextToken();
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					Debug.MSG_STATUS,
					"Checking location [" + tok + "] for value ...");
			}

			if (exists(tok, mpContext, inputObject))
			{
				return ((String)get(tok, mpContext, inputObject));
			}
		}

		return null;
	}

	
	/**
	 * Parse the time from the input string using the input time format and
	 * input time zone. Change the time zone and return the reformatted string.
	 * 
	 * @param  inputTime The String input Time.
	 * 
	 * @return  The String with the formated date.
	 * 
	 */
	private String convertTime( String inputTime ) throws ProcessingException {

		SimpleDateFormat sdf = new SimpleDateFormat(inputFormat);

		sdf.setTimeZone(inputTimeZone);

		Date date = null;
		try {

			date = sdf.parse(inputTime);

		} catch (ParseException e) {

			throw new ProcessingException (
				"Source time, [" + inputTime + "], cannot be parsed.");
		}

		sdf = new SimpleDateFormat(outputFormat);
		sdf.setTimeZone(outputTimeZone);

		return sdf.format(date);
	}

	/**
	 * Class encapsulating one get-set's worth of configuration information.
	 */
	private static class SOATimeZoneChangerConfig {
		public final String inputLoc;
		public final String outputLoc;

		public SOATimeZoneChangerConfig(String inputLoc, String outputLoc)
			throws FrameworkException {
			this.inputLoc = inputLoc;
			this.outputLoc = outputLoc;
		}

		public String describe() {
			StringBuffer sb = new StringBuffer();

			sb.append("Description: Location to fetch value from [");
			sb.append(inputLoc);

			sb.append("], location to set value to  [");
			sb.append(outputLoc);

			sb.append("].");

			return (sb.toString());
		} //describe

	} //TimeZoneChangerConfig

	//--------------------------For Testing---------------------------------//

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "E:\\logmap.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length != 3) {

			Debug.log(
				Debug.ALL_ERRORS,
				"SOATimeZoneChanger: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.1.7:1521:cprod soa soa ");

			return;

		}
		try {

			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {

			Debug.log(
				null,
				Debug.MAPPING_ERROR,
				": " + "Database initialization failure: " + e.getMessage());

		}

		SOATimeZoneChanger stzchanger = new SOATimeZoneChanger();

		try {
			stzchanger.initialize( "NEUSTAR_SOA", "SOATimeZoneChanger");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("OUTPUT_TIME_ZONE", "GMT");
			mob.set("INPUT_TIME_ZONE", "LOCAL");
			mob.set("INPUT_LOC_0", "07-19-2004-050000AM");
			mob.set("INPUT_LOC_1", "07-18-2004-050000AM");

			stzchanger.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());

		}
	} //end of main method

}
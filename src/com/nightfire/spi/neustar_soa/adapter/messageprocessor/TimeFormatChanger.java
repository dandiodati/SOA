/**
 * This processor takes date from locations in the
 * MessageObject/MessageProcessorContext, changes the Date format 
 * and puts them into locations in the
 * MessageObject/MessageProcessorContext.
 * 
 * 
 * @author Ravi M Sharma
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.adapter.messageprocessor.MessageProcessorBase
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair 
 * @see			com.nightfire.common.ProcessingException
 * @see			com.nightfire.framework.util.StringUtils
 * @see			com.nightfire.framework.util.FrameworkException
 * @see			com.nightfire.spi.common.driver.MessageObject
 * @see			com.nightfire.spi.common.driver.MessageProcessorContext
 * @see			com.nightfire.spi.common.driver.MessageProcessorBase
 * @see			com.nightfire.framework.message.MessageException
 * @see			com.nightfire.framework.db.PersistentProperty
 * @see			com.nightfire.framework.db.DBInterface
 * @see			com.nightfire.framework.db.DatabaseException
 */

 /**	 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ravi.M			05/31/2004			Created
	2			Ravi.M			06/01/2004			Review comments incorporated
	3			Ravi.M			07/29/2004			Formal review comments 
													incorporated.

  
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;


public class TimeFormatChanger extends MessageProcessorBase {

		
	/**
	 *The varaible used for outputFormat.
     */
	private String outputFormat;
	
	/**
	 *The Link list used for storing the input location values.
     */
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
	public TimeFormatChanger() {
		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
			Debug.log(
				Debug.OBJECT_LIFECYCLE,
				"Creating TimeFormatChanger message-processor.");
		}

		getSetList = new ArrayList();
	}

	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key   Property-key to use for locating initialization properties.
	 * @param  type  Property-type to use for locating initialization properties.
	 *
	 * @exception ProcessingException when initialization fails
	 */
	public void initialize(String key, String type)
		throws ProcessingException {
		// Call base class method to load the properties.
		super.initialize(key, type);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log(Debug.SYSTEM_CONFIG, "TimeFormatChanger: Initializing...");
		}

		StringBuffer errorBuffer = new StringBuffer();

		outputFormat =
			getRequiredPropertyValue(SOAConstants.OUTPUT_FORMAT_PROP, errorBuffer);
			
		String inputLocation = null;
		String outputLocation = null;

		//Loop until all location properties have been read.
		for (int Ix = 0; true; Ix++) {
			
		 inputLocation =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.INPUT_LOC_PREFIX_PROP,
						Ix));
						
			 outputLocation =
				getPropertyValue(
					PersistentProperty.getPropNameIteration(
						SOAConstants.OUTPUT_LOC_PREFIX_PROP,
						Ix));
						
			// allow the user to input only the input location if the output 
			//location is the same as the input:
			if (!StringUtils.hasValue(outputLocation)) {
				outputLocation = inputLocation;
			}

			//if neither input or output locations are found, we are done.
			if (!StringUtils.hasValue(inputLocation)
				&& !StringUtils.hasValue(outputLocation))
				{
					break;
				}

			try {
				// Create a new TimeFormatChangerConfig object and add it to the list.
				TimeFormatChangerConfig tfcc =
					new TimeFormatChangerConfig(inputLocation, outputLocation);

				if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
				{
				    Debug.log(Debug.SYSTEM_CONFIG, tfcc.describe());
				}

				getSetList.add(tfcc);
				
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

		// If any of the required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"TimeFormatChanger: Initialization done.");
		}
	}

	/**
	 * Extract values from one set of locations from the context/input/default value
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
        if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
		 Debug.log(Debug.MSG_STATUS, "TimeFormatChanger:processing ...");
		}
        try
        {
        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
        
		// Iterate over get-set configurations.
		Iterator iter = getSetList.iterator();
		
		TimeFormatChangerConfig tfcc = null;

		while (iter.hasNext()) {
			 tfcc =
				(TimeFormatChangerConfig) iter.next();

			String sourceValue = null;
			if ((tfcc.inputLoc).indexOf(SOAConstants.CONTAINER_CHILDNODE_INDICATOR) != -1) {
				setChildNodeVal(tfcc.inputLoc, mpContext, inputObject);
				continue;
			}

			if (StringUtils.hasValue(tfcc.inputLoc)){
			
				sourceValue = getValue(tfcc.inputLoc);
			}

			// If we still don't have a value signal error to caller.
			if ( sourceValue == null || sourceValue.equals("")) {
				continue;
			}
			// Check If Date format is MM-DD-YYYY-HHMMSS(AM/PM) (In seconds format)
			if(sourceValue.length()>17){
				
				// get the date in MM-DD-YYYY-HHMM format
				String date_1 = sourceValue.substring(0, 15);
				
				// get the AM/PM value from date
				String date_2 = sourceValue.substring(sourceValue.length()-2);
				
				sourceValue = date_1+date_2;
			}
				
			String formattedDate = formatDate( sourceValue );
			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(
				Debug.MSG_STATUS,
				"TimeFormatChanger:formattedDate format" + formattedDate);
			}

			// Put the result in its configured location.
			set(tfcc.outputLoc, mpContext, inputObject, formattedDate);
		}
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS, "TimeFormatChanger:processing done");
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
	 * This method sets the conatiner child nodes with coneverted datetime.
	 * The output location is same as input Location.
	 * @param  inputLoc  a set of '|' delimited XML locations to check.
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @exception  MessageException  Thrown on non-processing errors.
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private void setChildNodeVal(
		String inputLoc,
		MessageProcessorContext mpContext,
		MessageObject inputObject)
		throws ProcessingException, MessageException {
		int index = inputLoc.indexOf(SOAConstants.CONTAINER_CHILDNODE_INDICATOR);
		//Counter for container child nodes.
		int j = 0;

		String inputNode = "";

		String inputInnerNode = "";

		if (index != -1) {
		
			inputInnerNode = inputLoc.substring(0, index);
			
			inputNode =
				inputInnerNode
					+ SOAConstants.OPEN_PAREN
					+ j
					+ SOAConstants.CLOSED_PAREN
					+ inputLoc.substring(index 
					+ SOAConstants.CHILDNODE_INDICATOR_LENGTH);

			while (this.exists(inputNode, mpContext, inputObject)) {
			
				inputLoc = inputNode;

				if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				{
					Debug.log(
						Debug.MSG_STATUS,
						"The input location is :" + inputNode);
				}

				String sourceValue = getValue(inputLoc);

				if (Debug.isLevelEnabled(Debug.MSG_STATUS))
					Debug.log(
						Debug.MSG_STATUS,
						"The Val for date is  :" +  sourceValue);

				//increment the counter for [*] nodes.
				j = j + 1;

				//Looking for next child node inside the container node.
				inputNode =
					inputInnerNode
						+ SOAConstants.OPEN_PAREN
						+ j
						+ SOAConstants.CLOSED_PAREN
						+ inputLoc.substring(index 
						+ SOAConstants.CHILDNODE_INDICATOR_LENGTH);

				if (sourceValue == null || sourceValue.equals("")) {
					continue;
				}

				String formattedDate = formatDate(sourceValue);

				if (Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(
						Debug.MSG_STATUS,
						"The Formatted date is :" + formattedDate);
				}

				// Put the result in its configured location.
				set(inputLoc, mpContext, inputObject, formattedDate);
                if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(
						Debug.MSG_STATUS,
						"Set the value in the output node.");
				}

			}
		}

		return;
	}
	
	/**
	 * This method Parses the date from the input string using the input date format 
	 * and change the date format as required format and return the reformatted string.
	 * @param inputTime 
	 *
	 * @exception  MessageException.
	 */
	private String formatDate(String inputDate) throws ProcessingException {

		SimpleDateFormat sdf 
				= new SimpleDateFormat( SOAConstants.INPUT_FORMAT );

		Date date = null;
		try {

			if (inputDate.length() == SOAConstants.PORTED_TN_LENGTH ) {

				date = sdf.parse(inputDate);

				sdf = new SimpleDateFormat(outputFormat);

				inputDate = sdf.format(date);
			}

		} catch (ParseException e) {

			throw new ProcessingException(
				"Source date, [" + inputDate + "], cannot be parsed.");
		}

		return inputDate;
	}

	/**
	 * Class encapsulating one get-set's worth of configuration information.
	 */
	private static class TimeFormatChangerConfig {
		
		public final String inputLoc;

		public final String outputLoc;

		public TimeFormatChangerConfig(String inputLoc, String outputLoc)
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
		props.put("DEBUG_LOG_LEVELS", "all");
		props.put("LOG_FILE", "d:\\logmap.txt");
		Debug.showLevels();
		Debug.configureFromProperties(props);
		if (args.length != 3) {
			Debug.log(
				Debug.ALL_ERRORS,
				"TimeFormatChanger: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.1.246:1521:soa ravim ravim ");
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

		TimeFormatChanger tfc = new TimeFormatChanger();

		try {

			tfc.initialize("FullSoaTest", "NeuStar");

			MessageProcessorContext mpx = new MessageProcessorContext();
			MessageObject mob = new MessageObject();

			mob.set("OldSPDueDate", "04-20-2004-0500PM");
			mob.set("NewSPDueDate", "04-19-2004-0500PM");
			mob.set("DateSent", "04-18-2004-0500PM");

			tfc.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {
			System.out.println(pex.getMessage());
		} catch (MessageException mex) {
			System.out.println(mex.getMessage());
		}
	} //end of main method	

} //end of class TimeFormatChanger

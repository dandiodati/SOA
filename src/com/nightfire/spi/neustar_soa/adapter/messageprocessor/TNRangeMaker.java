/**
 * The purpose of this program is format the Porting TN as 
 * 17 digit number (XXX-XXX-XXXX-XXXX) from TN and EndStation values. 
 * This also get NPA, NXX, DASHX, NPA-NXX, NPA-NXX-X, STARTTN and ENDTN values and put all 
 * these values into context for further processing
 * 
 * @author Ravi M Sharma
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see			com.nightfire.adapter.messageprocessor.MessageProcessorBase
 * @see			com.nightfire.framework.util.Debug
 * @see			com.nightfire.framework.util.NVPair 
 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ravi.M			05/12/2004			Created
	2			Ravi.M			05/18/2004			Review Comments Incorporated
	3			Ravi.M			05/31/2004			Changes have been made to 
													generate NPA-NXX and 
													NPA-NXX-X values from 
													TN and put the same into 
													context
	4			Ravi.M			07/29/2004			Formal review comments 
													incorporated.

  
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.Debug;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;

public class TNRangeMaker extends MessageProcessorBase {

	
	/*
	 * This variable used to get value for TN.
	 */
	private String inputLocTn = null;

	/*
	 * 	This variable used to get value for EndStation.
	 */
	private String inputLocEndStation = null;

	/*
	 * 	This variable used to get value for PortedTn.
	 */
	private String outputLocPortedTn = null;

	/*
	 * 	This variable used to get value for StartTn.
	 */
	private String outputLocStartTn = null;
	
	
	/*
	 * 	This variable used to get value for EndTn.
	 */
	private String outputLocEndTn = null;
	
	/*
	 * 	This variable used to get value for StartPortedTn.
	 */
	private String outputLocStartPortedTn = null;
	
	/*
	 * 	This variable used to get value for EndPortedTn.
	 */
	private String outputLocEndPortedTn = null;

	/*
	 * 	This variable used to get value for NPA.
	 */
	private String outputLocNPA = null;

	/*
	 * 	This variable used to get value for NXX.
	 */
	private String outputLocNXX = null;

	/*
	 * 	This variable used to get value for DASHX.
	 */
	private String outputLocDASHX = null;

	/*
	 *  This variable used to get value for NPA_NXX.
	 */
	private String outputLocNpaNxx = null;

	/*
	 *  This variable used to get value for NPA_NXX_X.
	 */
	private String outputLocNpaNxxX = null;

	/*
	 * 	This variable used to get value for tnDelimiter.
	 */
	private String tnDelimiter = null;

	/*
	 * 	This variable used to get value for tnRange.
	 */
	private String tnRange = null;

	/*
	 * 	This variable used for processing the value for PortedTn.
	 */
	private String portedTn = null;

	/*
	 * 	This variable used for processing the value for StartTn.
	 */
	private String startTn = null;

	/*
	 * 	This variable used for processing the value for EndTn.
	 */
	private String endTn = null;
	
	/*
	 * 	This variable used for processing the value for StartPortedTn.
	 */
	private String startPortedTn = null;

	/*
	 * 	This variable used for processing the value for EndPortedTn.
	 */
	private String endPortedTn = null;


	/*
	 * 	This variable used for processing the value for NPA.
	 */
	private String npa = null;

	/*
	 * 	This variable used for processing the value for NXX.
	 */
	private String nxx = null;

	/*
	 * 	This variable used for processing the value for DASHX.
	 */
	private String dashx = null;

	/*
	 *  This variable used for processing the value for NPA_NXX.
	 */
	private String npa_nxx = null;

	/*
	 *  This variable used for processing the value for NPA_NXX_X.
	 */
	private String npa_nxx_x = null;


	private MessageProcessorContext mpContext = null;

	private	MessageObject inputObject = null;
    		
	/**
	* Constructor.
	*/
	public TNRangeMaker() {
		if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) ){
		Debug.log(
			Debug.OBJECT_LIFECYCLE,
			"Creating TNRangeMaker message-processor.");
		}
	}

	/**
	* Initializes this object via its persistent properties.
	*
	* @param  key   Property-key to use for locating initialization properties.
	* @param  type  Property-type to use for
	* locating initialization properties.
	*
	* @exception ProcessingException when initialization fails
	*/
	public void initialize(String key, String type)
		throws ProcessingException {

		// Call base class method to load the properties.
		super.initialize(key, type);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log(Debug.SYSTEM_CONFIG, "TNRangeMaker: Initializing...");
		}

		StringBuffer errorBuffer = new StringBuffer();

		inputLocTn = getRequiredPropertyValue( 
								SOAConstants.INPUT_LOC_TN_PROP, errorBuffer);

		inputLocEndStation = getPropertyValue( 
								SOAConstants.INPUT_LOC_END_STATION_PROP);

		outputLocPortedTn =	getRequiredPropertyValue(
				SOAConstants.OUTPUT_LOC_PORTED_TN_PROP, errorBuffer);

		outputLocStartTn = getRequiredPropertyValue(
				SOAConstants.OUTPUT_LOC_START_TN_PROP, errorBuffer);

		outputLocEndTn = getRequiredPropertyValue(
					SOAConstants.OUTPUT_LOC_END_TN_PROP, errorBuffer);

		outputLocNPA = getRequiredPropertyValue(
					SOAConstants.OUTPUT_LOC_NPA_PROP, errorBuffer);

		outputLocNXX = getRequiredPropertyValue(
						SOAConstants.OUTPUT_LOC_NXX_PROP, errorBuffer);

		outputLocDASHX = getRequiredPropertyValue(
					SOAConstants.OUTPUT_LOC_DASHX_PROP, errorBuffer);

		outputLocNpaNxx = getRequiredPropertyValue(
					SOAConstants.OUTPUT_LOC_NPA_NXX_PROP, errorBuffer);

		outputLocNpaNxxX = getRequiredPropertyValue( 
					SOAConstants.OUTPUT_LOC_NPA_NXX_X, errorBuffer);
				
		outputLocStartPortedTn = getPropertyValue(
					SOAConstants.OUTPUT_LOC_START_PORTED_TN_PROP);
			
		outputLocEndPortedTn = getPropertyValue(
					SOAConstants.OUTPUT_LOC_END_PORTED_TN_PROP);

		tnDelimiter = getPropertyValue(SOAConstants.TN_DELIMITER_PROP);

		if (!StringUtils.hasValue(tnDelimiter))
		{
			tnDelimiter = SOAConstants.DEFAULT_DELIMITER;
		}

		// If any of the required properties are absent,
		//indicate error to caller.
		if (errorBuffer.length() > 0) {

			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log(Debug.SYSTEM_CONFIG, "TNRangeMaker: Initialization done.");
		}
	}

	/**
	* Calls the makeTnRange method
	* Also sets the portedTn,npa,nxx,dashx,starttn,endtn 
	* into context or MessageObject.
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
		if (inputObject == null || inputObject.getDOM() == null) {
			
			return null;
			
		}
		
		this.mpContext = mpContext;
		
		this.inputObject = inputObject;
		

		String tnValue =  getValue(inputLocTn);

		String endStationValue =
			 getValue(inputLocEndStation);

		// Does no processing if the Tn value is not available..
		if (!StringUtils.hasValue(tnValue)){
			return formatNVPair(inputObject);
		}
        if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
		Debug.log(
			Debug.MSG_STATUS,
			"TNRangeMaker:Making TN Range and Ported TN.");
		}
		
		XMLMessageParser parser = null;
		try
		{
		tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
		
		int childElement = 0;
		
		String notification;
		
		Document doc = (Document) inputObject.getDOM();

		parser = new XMLMessageParser(doc);

		XMLMessageGenerator generator = new XMLMessageGenerator(doc);

		if(endStationValue == null ){
			
			if(generator.nodeExists("SOAToUpstream")){
				
				notification = (String)generator.getNode(
				"SOAToUpstream.SOAToUpstreamBody.0").getNodeName();
				 
	        	try {

	        		String rootNode = "SOAToUpstream.SOAToUpstreamBody." + notification
	    					+ ".Subscription.TnSvIdList";
	    			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	    			Debug.log(
	    					Debug.MSG_STATUS,
	    					"Root Node = " + rootNode);
					}
	    			
	    			if (parser.exists(rootNode)) {

	    				childElement = parser.getChildCount(rootNode);
	    			}
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
	    			Debug.log(
	    					Debug.MSG_STATUS,
	    					"child Element = " + childElement);
					}

	    			endStationValue = getEndStation(tnValue,childElement); 

	    		} catch (ProcessingException e) {

	    			Debug.log(Debug.ALL_ERRORS, e.toString());

	    			// Re-throw the exception to the driver.
	    			throw new ProcessingException(e.toString());
	    		}
			}						        	       
        }
        
		makeTnRange(tnValue, endStationValue);
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(
				Debug.MSG_STATUS,
				"TNRangeMaker: PortedTn with value = " + portedTn);
			
			Debug.log(
				Debug.MSG_STATUS,
				"TNRangeMaker: StartTn with value = " + startTn);
			Debug.log(
				Debug.MSG_STATUS,
				"TNRangeMaker: EndTn with value = " + endTn);


			Debug.log(
					Debug.MSG_STATUS,
					"TNRangeMaker: StartPortedTn with value = " + startPortedTn);

			Debug.log(
					Debug.MSG_STATUS,
					"TNRangeMaker: EndPortedTn with value = " + endPortedTn);

			Debug.log(Debug.MSG_STATUS, "TNRangeMaker: NPA with value = " + npa);

			Debug.log(Debug.MSG_STATUS, "TNRangeMaker: NXX with value = " + nxx);

			Debug.log(
				Debug.MSG_STATUS,
				"TNRangeMaker: NPA-NXX with value = " + npa_nxx);

			Debug.log(
				Debug.MSG_STATUS,
				"TNRangeMaker: DASHX with value = " + dashx);

			Debug.log(
				Debug.MSG_STATUS,
				"TNRangeMaker: NPA-NXX-X with value = " + npa_nxx_x);

			Debug.log(
				Debug.MSG_STATUS,
				"TNRangeMaker: Set the PortedTn values.");

        }			
			
		
		//set(outputLocTnRange, mpContext, inputObject, tnRange);
		set(outputLocPortedTn, mpContext, inputObject, portedTn);

		
		set(outputLocStartTn, mpContext, inputObject, startTn);

		
		set(outputLocEndTn, mpContext, inputObject, endTn);
		
		if( outputLocStartPortedTn != null)
		{
			
			
			set(outputLocStartPortedTn, mpContext, inputObject, startPortedTn);
			
		}
		
		if(outputLocEndPortedTn != null)
		{
		
			set(outputLocEndPortedTn, mpContext, inputObject, endPortedTn);
			
		}
		
		
		set(outputLocNPA, mpContext, inputObject, npa);

		
		set(outputLocNXX, mpContext, inputObject, nxx);

		
		set(outputLocNpaNxx, mpContext, inputObject, npa_nxx);

		
		set(outputLocDASHX, mpContext, inputObject, dashx);
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		Debug.log(
			Debug.MSG_STATUS,
			"TNRangeMaker: Set the NPA_NXX_X values.");
		}

		set(outputLocNpaNxxX, mpContext, inputObject, npa_nxx_x);
		}
		finally
		{
			ThreadMonitor.stop(tmti);
		}
		// Always return input value to provide pass-through semantics.
		return (formatNVPair(inputObject));
	}

	/**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 * @param  mpContext The context
	 * @param  inputObject  Input message to process.
	 *
	 * @return  object
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
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
				return (String)(get(tok, mpContext, inputObject));
			}
		}

		return null;
	}

	/**
	 * This method formats the TN as 17 digit number (XXX-XXX-XXXX-XXXX) 
	 * and gets NPA, NXX, DASHX, STARTTN and ENDTN values from the given
	 * TN and EndStation values.
	 * @param  tnValue  as a string.
	 * @param endStationValue as a string
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 *
	 */
	private void makeTnRange(String tnValue, String endStationValue)
		throws ProcessingException {

		
		try {

			StringBuffer tn = new StringBuffer(tnValue);

			// if endStation value exist with TN
			if (endStationValue != null && (!endStationValue.equals(""))) {

				tn.insert(12, tnDelimiter).append(endStationValue);

				portedTn = tn.toString();

			} else {

				tnRange = tn.substring(8).toString();

				portedTn =
					tn.insert(12, tnDelimiter).append(tnRange).toString();

			}

			StringTokenizer tokenizeRange =
				new StringTokenizer(portedTn, tnDelimiter);

			npa = tokenizeRange.nextToken();

			nxx = tokenizeRange.nextToken();

			npa_nxx = npa.concat(tnDelimiter).concat(nxx);

			startTn = tokenizeRange.nextToken();
			
			startPortedTn = npa.concat(tnDelimiter).concat(nxx).concat(tnDelimiter).concat(startTn);

			endTn = tokenizeRange.nextToken();
			
			endPortedTn = npa.concat(tnDelimiter).concat(nxx).concat(tnDelimiter).concat(endTn);

			dashx = startTn.substring(0, 1);

			npa_nxx_x = npa_nxx.concat(tnDelimiter).concat(dashx);

			if (Debug.isLevelEnabled(Debug.UNIT_TEST))
				Debug.log(
					Debug.UNIT_TEST,
					"Npa-->"
						+ npa
						+ " NXX->"
						+ nxx
						+ "DashX-> "
						+ dashx
						+ " Starttn->"
						+ startTn
						+ " EndTn -> "
						+ endTn
						+ " StartPortedTN ->"
						+ startPortedTn
						+ " EndPortedTN ->"
						+ endPortedTn
						+ " NpaNxxX ->"
						+ npa_nxx_x
						+ " NpaNxxX ->"
						+ npa_nxx_x);

		} catch (NoSuchElementException nse) {
			//will never happen as this processor is after the rule processor.
			throw new ProcessingException(
				"TNRangeMaker: Error: The Telephone"
					+ "Number  is not  correct ");
		}

	}
	
	
	/**
	 * This method process svids and  gives EndStation
	 *
	 * @return  EndStation .
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */

	private String getEndStation(String tn,int childElement)throws ProcessingException
	{

		String startTN = tn.substring(tn.lastIndexOf(
								SOAConstants.DEFAULT_DELIMITER )+ 1 );

		int endStation = -1 ;
		try
		{
			int diff = 0;
			if( childElement > 1 )
			{
				diff = childElement - 1 ;
			}

			endStation = Integer.parseInt(startTN) + diff ;
		}catch( NumberFormatException nbrfex ){
			throw new ProcessingException("Invalid StartSvid and/or EndSvid: "
																	 + nbrfex);
		}

      // this makes sure that the end station is always 4 digits and
      // pads the String with zeros if the end station has less than three
      // digits (i.e. is less than 1000).
      String endStationValue = StringUtils.padNumber(endStation, 4, true, '0');

		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log( Debug.MSG_STATUS, " Value of EndStation : " +
													endStationValue+ "." );
		}
     return endStationValue;
	}

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
				"TNRangeMaker: USAGE:  "
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

		TNRangeMaker trm = new TNRangeMaker();

		try {

			trm.initialize("NEUSTAR_SOA", "TNRangeMaker");

			MessageProcessorContext mpx = new MessageProcessorContext();
			MessageObject mob = new MessageObject();

			mob.set("TN", "123-111-4321");
			mob.set("END_TN", "4322");

			trm.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {
			System.out.println(pex.getMessage());
		} catch (MessageException mex) {
			System.out.println(mex.getMessage());
		}
	} //end of main method
}
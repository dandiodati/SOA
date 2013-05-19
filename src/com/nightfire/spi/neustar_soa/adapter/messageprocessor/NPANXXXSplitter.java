/**
 * The purpose of this processor is to split NpaNxxDashx into  
 * Npa,Nxx and Dashx values and put them into context.
 * This  processor will take NpaNxxDashx value as input.
 * 
 * @author Ravi.Ganapavarapu
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.resource.ResourceException;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.FrameworkException;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.framework.util.StringUtils;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorBase;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
  *  
 */

/** 
	Revision History
	---------------------
	Rev#		Created By  	 Date				    Reason
	-----       -----------     ----------			--------------------------
	1			Ravi.G			 06/30/2004 	   Created
	2           Ravi.G           07/01/2004        Modified and Review Comments 
												   incorporated	 
	3			Ravi.G           07/02/2004		   getValue method added	
	4			Ravi.G			 07/29/2004		   Formal review comments incorporated.
	5			Abhijit			 10/06/2004		   Declare separate variable for 
												   npaNxxX location and value.


 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.StringTokenizer;
import java.util.Properties;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NPANXXXSplitter extends MessageProcessorBase {

	
	/**
	*The  location of  npaNxxX
	*/
	private String npaNxxXLoc = null;

	/**
	*The  value of  npaNxxX
	*/
	private String npaNxxXValue = null;

	/**
	*The variable is used to get value of  npa
	*/

	private String outputLocNpa = null;

	/**
	*The variable is used to get value of  nxx
	*/

	private String outputLocNxx = null;

	/**
	*The variable is used to get value of  dashX
	*/

	private String outputLocDashX = null;

	/**
	*The Value of nxx
	*/

	private String npa = null;

	/**
	*The Value of nxx
	*/

	private String nxx = null;

	/**
	*The Value of dashX
	*/

	private String dashX = null;

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
	public NPANXXXSplitter() {
		Debug.log(
			Debug.OBJECT_LIFECYCLE,
			"Creating NPANXXXSplitter message-processor.");
	}

	/**
	*Initializes this object via its persistent properties.
	*
	*@param  key String  Property-key to use for locating initialization 
	*properties.
	*param  type String Property-type to use for locating initialization 
	*properties.
	*
	*@exception ProcessingException when initialization fails.
	*/

	public void initialize(String key, String type)
		throws ProcessingException {

		//Call base class method to load properties 

		super.initialize(key, type);

		//Get Configuration Properties  specific to this processor.
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log(Debug.SYSTEM_CONFIG, "NPANXXX: Initializing...");
		}

		StringBuffer errorBuffer = new StringBuffer();

		npaNxxXLoc = getRequiredPropertyValue(
						SOAConstants.INPUT_LOC_NPA_NXX_X_PROP, errorBuffer);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"The location of NPANXXX is [" + npaNxxXLoc + "].");
		}

		outputLocNpa = getRequiredPropertyValue(
					SOAConstants.OUTPUT_LOC_NPA_PROP, errorBuffer);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"The location of npa from RequiredProperty Value is ["
					+ outputLocNpa
					+ "].");
		}

		outputLocNxx = getRequiredPropertyValue(
						SOAConstants.OUTPUT_LOC_NXX_PROP, errorBuffer);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"The location of  nxx from RequiredProperty  is [" + outputLocNxx + "].");
		}

		outputLocDashX = getRequiredPropertyValue(
							SOAConstants.OUTPUT_LOC_DASHX_PROP, errorBuffer);
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"The location of dashX from RequiredProperty is ["
					+ outputLocDashX
					+ "].");
		}

		//If any of the required properties are absent,indicate error to caller

		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);
			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"NPANXXXSplitting: Initialization done.");
		}

	}

	/**
	*This method will extract the data values from the input, 
	*and splits the  input value to npa,nxx and dashx and put them into context. 
	*
	*@param  context MessageProcessorContext the context.
	*@param  object MessageObject Input message to process.
	*
	*@return   NVPair[] The given input, or null.
	*
	*@exception  ProcessingException thrown if processing fails.
	*@exception  MessageException  thrown if message is bad.
	*/
	public NVPair[] process( MessageProcessorContext context,
								MessageObject object)
							throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;

		if (object == null) {
			
			return null;
			
		}
		
		this.mpContext = context;

		this.inputObject = object;
		
		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			npaNxxXValue = getValue( npaNxxXLoc );
	
			if (npaNxxXValue != null && !npaNxxXValue.equals("")) {
	            
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(
						Debug.MSG_STATUS,
						"NPANXXXSplitter:Splitting NPA,NXX and DASHX  from NPANXXX ");
				}
	
				if (npaNxxXValue.length() == 7) 
				{
					
					npa = npaNxxXValue.substring(0, 3);
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(
							Debug.MSG_STATUS,
							"NPANXXXSplitter: NPA with value = " + npa);
					}
	
					nxx = npaNxxXValue.substring(3, 6);
					
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(
							Debug.MSG_STATUS,
							"NPANXXXSplitter: NXX with value = " + nxx);
					}
					dashX = npaNxxXValue.substring(6);
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(
							Debug.MSG_STATUS,
							"NPANXXXSplitter:DASHX with value = " + dashX);
					}
	
					//set npa in  the context
					super.set(outputLocNpa, context, object, npa);
	                if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(
							Debug.MSG_STATUS,
							"NPANXXXSplitter : Set the NPA value.." + npa);
					}
	
					//set nxx in the context
					super.set(outputLocNxx, context, object, nxx);
	                if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(
							Debug.MSG_STATUS,
							"NPANXXXSplitter : Set the NXX value.." + nxx);
					}
	
					//set dashX in the context
	
					super.set(outputLocDashX, context, object, dashX);
	                if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(
							Debug.MSG_STATUS,
							"NPANXXXXSplitter : Set the DASHX value.." + dashX);
					}
				} else {
	                if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
						Debug.log(
							Debug.MSG_STATUS,
							"NPANXXXSplitter : Input NPANXXX value is not Valid");
					}
	
					return formatNVPair(object);
	
				}
	
			} else {
				return null;
			}
		}finally{
			ThreadMonitor.stop(tmti);	
		}
		return (formatNVPair(object));

	}

	/**
	* This method tokenizes the input string and return an
	* object for exsisting value in context or messageobject.
	*
	* @param  locations as a string
	* @param  mpContext The context
	* @param  inputObject  Input message to process.
	*
	* @return  String
	*
	* @exception  ProcessingException  thrown if processing fails.
	* @exception  MessageException  thrown if message is bad.
	*/

	protected String getValue(String locations)
		throws MessageException, ProcessingException {

		StringTokenizer st =
			new StringTokenizer(locations, MessageProcessorBase.SEPARATOR);

		String tok = null;
			
		while (st.hasMoreTokens()) {
			tok = st.nextToken();

            if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				Debug.log(
					Debug.MSG_STATUS,
					"Checking location [" + tok + "] for value...");
			}

			if (exists(tok, mpContext, inputObject)) {
				return ( (String) get(tok, mpContext, inputObject));
			}
		}

		return null;
	}
	/**
	* For Testing
	*/

	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "E:\\logmap.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length != 3) {

			Debug.log(
				Debug.ALL_ERRORS,
				"NpaNXXXSplitter: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.1.246:1521:soa ravig ravig");

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

		NPANXXXSplitter npaNxxXSplitter = new NPANXXXSplitter();

		try {

			npaNxxXSplitter.initialize("Neustar SOA", "NpaNxxDashxSplitter");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("NPA_NXX_X", "1239588");

			npaNxxXSplitter.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());
		}
	}
}

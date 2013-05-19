/**
 * The purpose of this class is to separate the group action 
 * request into individual request.
 * 
 * @author Abhijit Talukdar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Abhijit			07/05/2004			Created

*/

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import java.util.StringTokenizer;
import java.util.ArrayList;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.Message;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.message.generator.MessageGeneratorFactory;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;

import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class SOALrnGttMapping extends MessageProcessorBase {
	/*
	 *  Private Static members
	 */
	private static int GATEWAY_BASE = Debug.USER_BASE;
	private static int GATEWAY_LIFECYCLE = GATEWAY_BASE;

	/*
	 * private members
	 */ 
	private String inputLocation = null;


	/**
	 * Called to initialize this Child of MessageAdapter.
	 *
	 * @param  key   Property-key to use for locating initialization properties.
	 * @param  type  Property-type to use for locating initialization properties.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	public void initialize(String key, String type)
		throws ProcessingException {

		super.initialize(key, type);
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, "Initializing the GTT request separator.");
		}

		StringBuffer errorBuffer = new StringBuffer();
	
		inputLocation =	getRequiredPropertyValue(
				"INPUT_LOC", errorBuffer);
											
	  //If any of the required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"SOARequestSeparator: Initialization done.");
		}
	}

	/**
	 * Process the input message (DOM or String) and (optionally) return
	 * a name / value pair.
	 *
	 * @param  input  Input message to process.
	 *
	 * @param  mpcontext The context
	 *
	 * @return  Optional NVPair containing a Destination name and a Document, 
	 *          or null if none.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 *
	 * @exception  MessageException  Thrown if bad message
	 */
	public NVPair[] process(
		MessageProcessorContext mpContext,
		MessageObject inputObject)
		throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;

		XMLMessageParser inputParser = null;		

		NVPair[] contents = null;
		
		if (inputObject == null) {
			return null;
		}		

		if (toProcessorNames == null) {
			// Make sure this is at least processor to receive results.
			String errMsg = "ERROR: No next processors available.";
			Debug.log(null, Debug.ALL_ERRORS, errMsg);
			throw new ProcessingException(errMsg);
		}

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(
				Debug.MSG_STATUS,
				"The GTT request separator is processing the message. ");
		}
        
        try
        {
	        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        
			// Set up the parser for retreiving data from the incoming Message.
			
			Document doc = getDOM(inputLocation, mpContext, inputObject);
	
			inputParser = new XMLMessageParser(doc);
	
			String lrns = null;
	
			String lrnNode = "UpstreamToSOA.UpstreamToSOABody."
							+ "GTTCreateRequest.Lrn";
			
			if (inputParser.exists(lrnNode))
			{
				lrns = inputParser.getValue(lrnNode);
			}
	
			StringTokenizer st = new StringTokenizer(lrns,
					SOAConstants.DEFAULT_LOCATION_SEPARATOR);
	
			int nRecords = st.countTokens();
	
			int processorsNo = toProcessorNames.length;
	
			contents = new NVPair[(nRecords +1) * processorsNo];
			
			//Document[] individualRequest = new Document[nRecords];
	
			MessageObject mObj = null;
	
			for (int i=0; i <= nRecords; i++)
			{
				String lrn = "false";
	
				if (i!=0 && st.hasMoreTokens())
				{
					lrn = st.nextToken();				
				}			
	
				Node parentNode = inputParser.getNode("UpstreamToSOA");
	
							Node[] children = XMLMessageBase.getChildNodes(parentNode);
	
				XMLMessageGenerator generator =	new XMLMessageGenerator("SOAMessage");
	
				generator.setValue( "UpstreamToSOA."+children[0].getNodeName(), children[0]);
	
				generator.setValue( "UpstreamToSOA."+children[1].getNodeName(), children[1]);
	
				generator.setValue( lrnNode, lrn);
	
				mObj = new MessageObject(generator.getDocument());
	
				// Sending the message objects to the next processors.
				for (int j = 0; j < processorsNo; j++)				
				{
	
					contents[(i * (processorsNo)) + j] =
						new NVPair(toProcessorNames[j], mObj);
					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug.log(
							null,
							Debug.MSG_STATUS,
							"NEXT PROCESSOR-->"
								+ toProcessorNames[j]
								+ "\n"
								+ "MESSAGE OBJECT CONTENT------------>"
								+ mObj.describe());
					}
	
				}						
			}
        }finally
		{
			ThreadMonitor.stop(tmti);
		}
		return contents;
	}		
}

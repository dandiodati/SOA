/**
 * The purpose of this class is to take a batch of XML
 * Order messages, and split them into individual 
 * messages
 * 
 * @author Ravi M Sharma
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
	1			Ravi.M			07/05/2004			Created
	2			Ravi.M			07/06/2004			Review Comments Incorporated.
	3			Ravi.M			07/29/2004			Formal review comments 
													incorporated.

*/

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

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


public class SOAXMLBatchSeparator extends MessageProcessorBase {
	/*
	 *  Private Static members
	 */
	private static int GATEWAY_BASE = Debug.USER_BASE;
	private static int GATEWAY_LIFECYCLE = GATEWAY_BASE;

	/*
	 * private members
	 */ 
	private String outputRootNode = null;
	private String inputLocation = null;
	private String outputBaseNode = null;
	private String batchBaseNode = null;

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
		  Debug.log(Debug.MSG_STATUS, "Initializing the SOA batch separator.");
		}

		StringBuffer errorBuffer = new StringBuffer();

		outputRootNode = getRequiredPropertyValue(
					SOAConstants.OUTPUT_ROOT_NODE_PROP, errorBuffer);
								
		inputLocation =	getRequiredPropertyValue(
				SOAConstants.INPUT_MESSAGE_LOCATION_PROP, errorBuffer);
				
		outputBaseNode = getRequiredPropertyValue(
					SOAConstants.OUTPUT_BASE_NODE_PROP, errorBuffer);
					
		batchBaseNode =	getRequiredPropertyValue(
					SOAConstants.BATCH_BASE_NODE_PROP, errorBuffer);

	  //If any of the required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log(
				Debug.SYSTEM_CONFIG,
				"SOAXMLBatchSeparator: Initialization done.");
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
		MessageProcessorContext mpcontext,
		MessageObject inputObject)
		throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;

		XMLMessageParser inputParser = null;

		if (inputObject == null) {
			return null;
		}

		if (toProcessorNames == null) {
			// Make sure this is at least processor to receive results.
			String errMsg = "ERROR: No next processors available.";
			Debug.log(null, Debug.ALL_ERRORS, errMsg);
			throw new ProcessingException(errMsg);
		}
        if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
			Debug.log(
				Debug.MSG_STATUS,
				"The SOA batch separator is processing the message. ");
		}
        try
	        {
	        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	        
			// Set up the parser for retreiving data from the incoming Message.
			
			Document doc = getDOM(inputLocation, mpcontext, inputObject);
	
			inputParser = new XMLMessageParser(doc);
	
			// Get all documents.
			Document[] outDocs = getAllDocuments(inputParser);
	
			int nRecords = outDocs.length;
	
			int processorsNo = toProcessorNames.length;
			
			NVPair[] contents = new NVPair[nRecords * processorsNo];
			
			MessageObject mObj = null;
	
			for (int i = 0; i < nRecords; i++)
			{
				 mObj = new MessageObject(outDocs[i]);
	
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
			return contents;
	    }
        finally
        {
        	ThreadMonitor.stop(tmti);
        }
		
	}

	/**
	 * Extract the nodes representing individual XML messages
	 * from a batch and return them in document.
	 *
	 * @return The documents that represent individal XML messages.
	 *
	 * @param  inputParser  Input parser containing the batch.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 */
	private Document[] getAllDocuments(XMLMessageParser inputParser)
		throws MessageException {
		Node parentNode = null;
		parentNode = inputParser.getNode(batchBaseNode);
		Node[] children = XMLMessageBase.getChildNodes(parentNode);
		Document[] results = new Document[children.length];

		//Traverses the xml in reverse order. 
		for (int j = children.length - 1, i = 0; j >= 0; j--, i++) {
			Node child = children[i];

			results[j] = generateOutputDocument(child);

		}
		return results;
	}

	/**
	 * Process the input Document Node to generate the out going DOM
	 * End result will be a document represing an XML Message
	 *  
	 * @param  input  Input parser with incomming data.
	 *
	 * @return  Document with outgoing data.
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 *             This is is a processing Exception because it 
	 *             is caused by bad configuration, or ciritical 
	 *             utility failiar, not message content or structure.
	 */
	private Document generateOutputDocument(Node input)
		throws MessageException {
		//outputRootNode is the DOCTYPE for the new xml document.
		if (outputRootNode.equalsIgnoreCase(".")) {
            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(
					this,
					Debug.MSG_STATUS,
					"Base for the new document is "
						+ "the root, so setting output doc type to  "
						+ "be the same type as " 
						+ "previously split node.");
			}

			outputRootNode = input.getNodeName();
		}

		Document outDoc = null;

		XMLMessageGenerator generator =
			(XMLMessageGenerator) MessageGeneratorFactory.create(
				Message.XML_TYPE,
				outputRootNode);

		generator.setValue(outputBaseNode, input);
		outDoc = generator.getDocument();

		return outDoc;
	}

}

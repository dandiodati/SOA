/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/XMLBatchSeparator.java#1 $
 */
 
package com.nightfire.adapter.messageprocessor;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.Message;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.message.generator.MessageGeneratorFactory;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * The purpose of this class is to take a batch of XML
 * Order messages, and split them into individual 
 * messages, log them, and feed them one at a time to a
 * downstream consumer.
 */
public class XMLBatchSeparator extends MessageProcessorBase
{
	
    // Private Static Strings
    private static String OUTPUT_ROOT_NODE_PROP = "OUTPUT_ROOT_NODE";
    private static String INPUT_MESSAGE_LOCATION_PROP = "INPUT_MESSAGE_LOCATION";
    private static String OUTPUT_BASE_NODE_PROP = "OUTPUT_BASE_NODE";
    private static String BATCH_BASE_NODE_PROP = "BATCH_BASE_NODE";

	/**
     * Property indicating the order in which the XML need to process.
     */
    public static final String IS_PROCESS_ORDER_ASC_PROP = "IS_PROCESS_ORDER_ASC";

    // Private Static members
    private static int GATEWAY_BASE = Debug.USER_BASE;
    private static int GATEWAY_LIFECYCLE = GATEWAY_BASE;

    // private members
    private String outputRootNode = null;
    private String inputLocation = null;
    private String outputBaseNode = null;
    private String batchBaseNode = null;
    private boolean isProcessingOrderAscending = false;

    /**
     * Called to initialize this Child of MessageAdapter.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize(key, type);

        Debug.log(Debug.MSG_STATUS, "Initializing the batch separator.");
        
        StringBuffer errorBuffer = new StringBuffer( );
        
        outputRootNode = getRequiredPropertyValue(OUTPUT_ROOT_NODE_PROP, errorBuffer );
        inputLocation = getRequiredPropertyValue(INPUT_MESSAGE_LOCATION_PROP, errorBuffer);
        outputBaseNode = getRequiredPropertyValue(OUTPUT_BASE_NODE_PROP, errorBuffer);
        batchBaseNode = getRequiredPropertyValue(BATCH_BASE_NODE_PROP, errorBuffer);
        
		String strTemp = getPropertyValue( IS_PROCESS_ORDER_ASC_PROP );

        if ( StringUtils.hasValue( strTemp ) )
        {
            try {
                isProcessingOrderAscending = getBoolean( strTemp );
            }
            catch ( FrameworkException e )
            {
                errorBuffer.append ( "Property value for " + IS_PROCESS_ORDER_ASC_PROP +
                  " is invalid. " + e.getMessage ( ) + "\n" );
            }
        }

        Debug.log( Debug.SYSTEM_CONFIG, "The Order of processing the XML is Ascending ? ["
                   + isProcessingOrderAscending + "]." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "XMLBatchSeparator: Initialization done." );
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
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject inputObject ) throws MessageException, ProcessingException
    {
        
        XMLMessageParser inputParser = null;

        if(inputObject == null)
        {
            return null;
        }
        
        if(toProcessorNames == null)
        {
            // Make sure this is at least processor to receive results.
            String errMsg = "ERROR: No next processors available.";
            Debug.log(null, Debug.ALL_ERRORS, errMsg);
            throw new ProcessingException(errMsg);
        }
        
        Debug.log(Debug.MSG_STATUS, "The batch separator is processing the message. ");
                
        // Set up the parser for retreiving data from the incoming Message.
        Debug.log(this, GATEWAY_LIFECYCLE, "Creating XML Message Parser...");
        Document doc = getDOM(inputLocation, mpcontext, inputObject);
        
        inputParser = new XMLMessageParser(doc);

        // Get all documents.
        Document[] outDocs = getAllDocuments(inputParser);

        int nRecords = outDocs.length;
        Debug.log(this, GATEWAY_LIFECYCLE, "Total number records found is [" + nRecords + "].");

        int processorsNo = toProcessorNames.length;
        NVPair[] contents = new NVPair [ nRecords * processorsNo];

        for(int i=0; i<nRecords; i++)
        {
            MessageObject mObj = new MessageObject(outDocs[i]);

            // Sending the message objects to the next processors.
            for (int j = 0; j < processorsNo; j++)
            {

                contents[(i*(processorsNo))+j] = new NVPair ( toProcessorNames[j], mObj );
                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log(null, Debug.MSG_STATUS, "NEXT PROCESSOR-->"+toProcessorNames[j]+"\n"+
                              "MESSAGE OBJECT CONTENT------------>"+mObj.describe());
                }
                    
            }

        }

        return contents;
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
        throws MessageException
    {
        Node parentNode = inputParser.getNode(batchBaseNode);
        Node[] children = XMLMessageBase.getChildNodes(parentNode);

        Document[] results = new Document[children.length];
        
        //Traverses the xml in ascending order.
        if ( isProcessingOrderAscending )
        {
            for (int j = children.length - 1, i = 0; j >= 0; j--, i++) {
                Node child = children[i];

                results[j] = generateOutputDocument(child);
                
            }
        }
        //Traverses the xml in descending order.
        else 
        {
            for(int i = 0 ; i < children.length ; i++)
            {
                Node child = children[i];
                
                results[i] = generateOutputDocument(child);
            }
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
        throws MessageException
    {
        //outputRootNode is the DOCTYPE for the new xml document.
        if( outputRootNode.equalsIgnoreCase(".") )
        {
            Debug.log(this, Debug.MSG_STATUS, "Base for the new document is " + 
                      "the root, so setting output doc type to be the same type as " + 
                      "previously split node.");
            outputRootNode = input.getNodeName();
        }

        Document outDoc = null;

        XMLMessageGenerator generator = (XMLMessageGenerator)
            MessageGeneratorFactory.create(Message.XML_TYPE, outputRootNode);
        
        generator.setValue(outputBaseNode, input);
        outDoc = generator.getDocument();

        return outDoc;
    }
    
}


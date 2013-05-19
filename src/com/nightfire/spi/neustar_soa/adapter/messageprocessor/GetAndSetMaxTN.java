/**
 * The purpose of this processor is to get the input message as XML to find out
 * the MaxTN and SVID then set that data into the context.
 * 
 * @author D. Subbarao
 * @version 3.3
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorBase;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants;
 *  
 */

 /** 
 Revision History
 ---------------------
 Rev#		Modified By 	Date				Reason
 -----       -----------     ----------			--------------------------
 1			D.Subbarao		09/13/2005			Created
 2			D.Subbarao		10/17/2005			Modified and added input 
  												and output messages.
 3			D.Subbarao		10/18/2005			Modified.  	
 4			D.Subbarao		12/13/2005			Eleminated some input messages.
 5 			D.Subbarao		08/02/2006			Remove unnessary variables and add
 												a condition in process()
 												and a conversion functions modified
 												ex:Integer.parseInt instead Long.parseLong
 												for reducing memory.	
  												
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class GetAndSetMaxTN extends MessageProcessorBase {
    
    /**
     * The value of inputMessage for maxTN.
     */
    private String inputMsgForMaxTnAndSvId = null;
   
    /**
     * The value of inputMessage for NodeName.
     */
    
    private String inputMsgNodeName=null;
    
    /**
     * The value of outputMessage for maxTN.
     */
    private String outputMsgForMaxTn = null;
    /**
     * The value of outputMessage for SvId.
     */
    private String outputMsgForSvId = null;
    
    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException{
        
        try{
        //	Call base class method to load the properties.
        super.initialize(key,type);
        //		
        //Get configuration properties specific to this processor.
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
          Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMaxTN : Initializing..." );
		}
        
        String errorString="";	
        
        StringBuffer errorBuffer = new StringBuffer( );

        // This contains the input message context for MaxTN and SvID.
        inputMsgForMaxTnAndSvId = getRequiredPropertyValue(
                SOAConstants.INPUT_MSG_LOC, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Input message is [" 
					+ inputMsgForMaxTnAndSvId + "]." );
		}
        
        // This contains the input message context for NodeName.
        inputMsgNodeName = getRequiredPropertyValue(
                SOAConstants.INPUT_NODE_NAME, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Input Node Name is [" 
					+ inputMsgNodeName + "]." );
		}
        
        // This contains the output message context for MaxTn.
        outputMsgForMaxTn = getPropertyValue( 
                SOAConstants.OUTPUT_MESSAGE_MAXTN,  errorString );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Output message for MaxTN is [" 
					+ outputMsgForMaxTn + "]." );
		}
        
        errorBuffer.append(errorString);
        
        // This contains the output message context for SvId.
        outputMsgForSvId = getPropertyValue( 
                SOAConstants.OUTPUT_MESSAGE_SVID,  errorString );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Output message for SvId is [" 
					+ outputMsgForSvId + "]." );
		}
        
        errorBuffer.append(errorString);
         
        // If any of the required properties are absent,indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );
            
            Debug.log( Debug.ALL_ERRORS, errMsg );
            
            throw new ProcessingException( errMsg );
        }
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMaxTN for MaxTN and SvID : " +
					"Initialization has been done." );
			}
        }
        catch(ProcessingException pe){
            throw pe;
        }
    }//	initialize end.
    
    /**
     * This method will extract the data values are MaxTn and SvId from the 
     * inputMessage and set these messages into the context.
     *
     * @param  context The context will obtain the output messages.
     * @param  messageObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception ProcessingException thrown if processing fails.
     * @exception MessageException thrown if message is bad.
     */
    
    public NVPair[] process ( MessageProcessorContext context, 
            MessageObject messageObject )   throws MessageException,
            ProcessingException{
    	
    	ThreadMonitor.ThreadInfo tmti = null;
        try{        
        
        if ( messageObject == null ) {
            return null;
        }
        // 	This contains the inputMessage for SvId and MaxTN.
        String inputMsg = getString( inputMsgForMaxTnAndSvId, context,
                messageObject );
        
        XMLMessageParser domMsg = new XMLMessageParser(inputMsg);
        
        Document outMsg = domMsg.getDocument();
        
        Element firstRoot = outMsg.getDocumentElement();
        
             
        // 	This contains the elements are containing with data tag.
        NodeList dataElements = firstRoot.getElementsByTagName(inputMsgNodeName);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){		
			Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMaxTN starting to get MaxTN " +
					"and SvID:");
		}
        
        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] processing started..." );
        
            for(int dataElemCount=dataElements.getLength()-1;dataElemCount>=0;
            												dataElemCount--){
                // 	This contains the elements are containing in data tag.
                NodeList kidsList=dataElements.item(dataElemCount).getChildNodes();
      
                             
                for(int kidsCount=kidsList.getLength()-1;kidsCount>=0;
                									kidsCount--){
                    
                    if(kidsList.item(kidsCount)!=null){
                        
                        Node node=kidsList.item(kidsCount);
                        
                        String  elemName=node.getNodeName();
                        
                        NamedNodeMap attrMap = node.getAttributes();
                        	
                        if(elemName.equals(SOAConstants.KEY_NODE_CHILD_MAXTN)){

                          String portingTn= attrMap.getNamedItem
                            (SOAConstants.NODE_ATTRIBUTE).getNodeValue();

                         if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){ 
							 Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMaxTN," +
							  "The value of MaxTN element is set into MessageObject:"+
											  portingTn);
						 }
                         
						String tempPortingTn=portingTn.substring(0,
                                  	portingTn.length()-4);
                          
                       String nextTn = portingTn.substring(portingTn.length()-4);
                                             
                       if (Integer.parseInt(nextTn) != 9999)
                      
                           portingTn = tempPortingTn + StringUtils.padNumber
                            ( Integer.parseInt(nextTn)+1, SOAConstants.TN_LINE,
								 true, '0' );
								 
                           // set the maxTN into the output messgae location.
							super.set( outputMsgForMaxTn, context, messageObject,
                                  portingTn);
                          
                        }
                        if(elemName.equals(SOAConstants.KEY_NODE_CHILD_SVID)){

                            // set the Svid into the output messgae location.
                            super.set( outputMsgForSvId, context, messageObject, 
                                  attrMap.getNamedItem
                                 (SOAConstants.NODE_ATTRIBUTE).getNodeValue());
                           if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
                              Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMaxTN The value of SvId element is set into MessageObject:"+ attrMap.getNamedItem("value").getNodeValue());
                           }
                           kidsCount=-1;dataElemCount=-1;
                        }     
                    }
                 }
            }              
            // this gets the maxTN from the output messgae location.
            String messageMaxTn = getString(outputMsgForMaxTn,context,
                    messageObject);
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"GetAndSetMaxTN outMessage for MaxTN:"
						+ messageMaxTn );
			}
            // this gets the SvId from the output messgae location.
            String messageSvId = getString(outputMsgForSvId,context,messageObject);
            if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,"GetAndSetMaxTN outMessage for SvID:"
						+  messageSvId );
			}
        }
        catch(MessageException me){
            throw me;
        }
        catch(ProcessingException pe){
            throw pe;
        }
        finally
        {
        	ThreadMonitor.stop( tmti );
        }
        return( formatNVPair( messageObject ) );
    }
    
    
    public static void main(String[] args) {
        
        // This generates log file will be containing the log information 
        // of the process.
        Properties props = new Properties();
        
        props.put( "DEBUG_LOG_LEVELS", "ALL" );
        
        props.put( "LOG_FILE", "E:\\logmap.txt" );
        
        Debug.showLevels( );
        
        Debug.configureFromProperties( props );
        
        if (args.length != 3)
        {
            
            Debug.log (Debug.ALL_ERRORS, "GetAndSetMessageForMaxTn and SvID:"+
            "USAGE: jdbc:oracle:thin:@192.168.198.42:1521:NSOA rameshc rameshc");
            
            return;
            
        }
        try
        {
            // Initializes the resources for database.
            DBInterface.initialize( args[0], args[1], args[2] );
            
        }
        catch (DatabaseException e)
        {
            
            Debug.log( null, Debug.MAPPING_ERROR, ": " +
                    "Database initialization failure: " + e.getMessage() );
        }
        
        try
        {
            GetAndSetMaxTN getAndsetMsg = new GetAndSetMaxTN();
            
            // Initializes the resources for Message Context.
            getAndsetMsg.initialize("FULL_NEUSTAR_SOA","GetAndSetMaxTN");
          
            MessageProcessorContext mpx = new MessageProcessorContext();
            
            MessageObject mob = new MessageObject();
            // This sets the input messages for processing for MaxTN and SvID.
            mob.set("inputMessages",
                    "<?xml version=\"1.0\"?><SOAMessage>"+
                      "<SOAToUpstream> <SOAToUpstreamHeader>"+
                      " <DateSent value=\"12-12-2005-042337PM\" />"+
                      "  <RegionId value=\"0003\" />"+
                       " </SOAToUpstreamHeader>"+
                       " <SOAToUpstreamBody><SvQueryReply>"+
                         "<QueryStatus><SOARequestStatus value=\"success\" />"+
                            "</QueryStatus><MaxQueryResultsExceeded value=\"true\" />"+
                            "<SvList> <SvData><SvId value=\"108193\" />"+
                                "<Tn value=\"530-002-0007\" /></SvData>" +
                             "</SvList>" +
                             "<SvList> <SvData><SvId value=\"108194\" />"+
                             "<Tn value=\"530-002-0021\" /> </SvData>" +
                             "</SvList>"+
                             "</SvQueryReply></SOAToUpstreamBody>"+
                            "</SOAToUpstream></SOAMessage>");                                
            
            getAndsetMsg.process(mpx,mob);
            
            //This describes the context.
            Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());
            
        }
        catch(ProcessingException pex)
        {
            
            System.out.println(pex.getMessage());
            
        }
        catch(MessageException mex)
        {
            
            System.out.println(mex.getMessage());
            
        }
        
    }
}

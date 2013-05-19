/**
 * The purpose of this processor is to get the input message as XML to find out
 * the node count then set that data into the context.
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
 1			D.Subbarao		10/17/2005			Created
 2			D.Subbarao		10/18/2005			Modified
 3			D.Subbarao		12/13/2005			Modified and Added a new method
 												returnNodeValue.
 4			D.Subbarao		12/14/2005			Modified.												
 
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
 
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class NodeCount extends MessageProcessorBase {
    
    /**
     * The value of inputMessage for NodeCount.
     */
    private String inputMsgForNodeCount = null;
    
    /**
     * The value of inputMessage for noOfRecord. 
     */
    private String inputMsgNoOfRecords=null;
    
    /**
     * The value of inputMessage for NodeName.
     */
    private String inputMsgNodeName=null;
    
    /**
     *  The value of inputMessage for PortingTN.
     */
    private String inputMsgPortingTN=null;
    
    /**
     * The value of outputMessage for recordCount.
     */
    private String outputMsgForRecCount=null;
    
    // This holds the maxTN which will be extracted in SvQueryResponse.
    private String maxTn=null;

    private static final String TRUE="TRUE";
    
    private static final String FALSE="FALSE";
    
    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails.
     */
    public void initialize ( String key, String type )
    throws ProcessingException{
        
        try{
	        //	Call base class method to load the properties.
	        super.initialize(key,type);
	        //		
	        //Get configuration properties specific to this processor.
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
	          Debug.log( Debug.SYSTEM_CONFIG, "NodeCount : Initializing..." );
			}
	        
	        String errorString ="";
	        
	        StringBuffer errorBuffer = new StringBuffer( );
	
	        // This contains the input message context for nodeCount.
	        inputMsgForNodeCount = getRequiredPropertyValue(
	                SOAConstants.INPUT_MSG_LOC, errorBuffer );
	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Input message is [" 
						+ inputMsgForNodeCount + "]." );
			}
	        
	        // This contains the input message context for NoOfRecords.
	        inputMsgNoOfRecords = getRequiredPropertyValue(
	                SOAConstants.NUMBER_OF_RECORD, errorBuffer );
	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Input message for no.of record:[" 
						+ inputMsgNoOfRecords + "]." );
			}
	        
	        // This contains the input message context for NodeName.
	        inputMsgNodeName = getRequiredPropertyValue(
	                SOAConstants.INPUT_NODE_NAME, errorBuffer );
	        
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Input message for node name: [" 
						+ inputMsgNodeName + "]." );
			}

	        // This contains the input message context for PortingTn.
	        inputMsgPortingTN = getRequiredPropertyValue(
	                SOAConstants.INPUT_PORTINGTN, errorBuffer );

	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Input message for PortingTn: [" 
						+ inputMsgPortingTN + "]." );
			}
	        
	        // This contains the output message context for Record Count.
	        outputMsgForRecCount = getPropertyValue( 
	                SOAConstants.OUTPUT_RECORD_COUNT, errorString );

	        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "Output message for SvId is [" 
						+ outputMsgForRecCount + "]." );
	        }
	        // If any of the required properties are absent,indicate error to caller.
	        if ( errorBuffer.length() > 0 )
	        {
	            String errMsg = errorBuffer.toString( );
	            
	            Debug.log( Debug.ALL_ERRORS, errMsg );
	            
	            throw new ProcessingException( errMsg );
	        }
				if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log( Debug.SYSTEM_CONFIG, "NodeCount for node count : " +
						"Initialization has been done." );
				}
	        }catch(ProcessingException pe){
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
        
        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
        
        // 	This contains the inputMessage for SvId and MaxTN.
        String inputMsg = getString( inputMsgForNodeCount, context, 
                messageObject );
        
        // This contains the input message context for PortingTn.
        inputMsgPortingTN = getString(inputMsgPortingTN,context,messageObject);
        
        XMLMessageParser domMsg = new XMLMessageParser(inputMsg);
        
        Document outMsg = domMsg.getDocument();
        
        Element firstRoot = outMsg.getDocumentElement();

        // 	This contains the elements are containing with data tag.
        NodeList dataElements = firstRoot.getElementsByTagName(inputMsgNodeName);
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
         Debug.log( Debug.SYSTEM_CONFIG, "NodeCount is starting to get node count");
		}
       
        int recordsCount=dataElements.getLength();

        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS, "The no of records are sent in SvQueryResponse " +
					" from npac:"+ recordsCount);
		}
        		
        if(recordsCount==Integer.parseInt(inputMsgNoOfRecords)) {
        
            // This will return the maximum portingtn from SvQueryResponse.
		    getMaxPortingTn(dataElements);
		    
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "NodeCount,The retuned maxTN value is:" + 
						maxTn);
			}
	
		    if(maxTn!=null) {
		        
		        int maxPortingTn=Integer.parseInt(maxTn)+1;

		        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS, "MaxTN in the SvQueryReponse " +
							" which is sent from NPAC:" + maxPortingTn);
				}

		        // It extracts the ending range of porting tn from input context.
		        	
		        inputMsgPortingTN=inputMsgPortingTN.substring(
		                			inputMsgPortingTN.length()-4,
		                			inputMsgPortingTN.length());
		        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					Debug.log(Debug.MSG_STATUS, "MaxTN in the SvQueryRequest " +
							" which is sent from SOA:" + inputMsgPortingTN);
				}
		        
		        if(maxPortingTn<Integer.parseInt(inputMsgPortingTN)){
		            
		         //This sets the true when record count matches with the input message.
		            super.set( outputMsgForRecCount, context, messageObject,TRUE);
		            
		        }
		        else{
		            
		            // This sets the false when record count exceeds.
		            super.set( outputMsgForRecCount, context, messageObject,FALSE);
		        }
		        
		        String messageRecSta=getString(outputMsgForRecCount,context,
		                messageObject);
		        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log( Debug.SYSTEM_CONFIG, "NodeCount,outMessage for Query " +
							"RecordCount matches with the input message:" + messageRecSta );
				}
		     }
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
        	ThreadMonitor.stop(tmti);
        }
        
        return( formatNVPair( messageObject ) );
    }
 /**
 * This will be used to get the value of node in the SvQueryResponse which 
 * will be sent from NPAC.
 * 
 * @param dataElements contains the elements of SvQueryResponse XML.
 */    
 private void getMaxPortingTn(NodeList dataElements){
    
    try{
		    if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
	          Debug.log(Debug.SYSTEM_CONFIG,"Returning the children of data tag");
			}
	        
	        if(dataElements!=null)
	        for(int dataElemCount=dataElements.getLength()-1;dataElemCount>=0;
	        											dataElemCount--){
	            // 	This contains the elements are containing in data tag.
	            NodeList kidsList=dataElements.item(dataElemCount).getChildNodes();
	            
	            if(kidsList!=null)
	            for(int kidsCount=kidsList.getLength()-1;kidsCount>=0;kidsCount--){
	                
	                if(kidsList.item(kidsCount)!=null){
	                    
	                    Node node=kidsList.item(kidsCount);
	                    
	                    Node data = node.getFirstChild();
	                    
	                    String  childNodeNm=node.getNodeName();
	                    
	                    if(childNodeNm!=null) 
	                    if(childNodeNm.equals(SOAConstants.
	                    		KEY_NODE_CHILD_SUB_MAXTN))  {
	                        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
								Debug.log(Debug.SYSTEM_CONFIG,"Returning the first " +
										" child of data tag:" + childNodeNm );
							}
	                        
	                        maxTn=data.getNodeValue();
	                        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){        
								Debug.log(Debug.SYSTEM_CONFIG,"Returning the value of" +
									" first child of data tag:" + maxTn );
							}
	                    
	                    	maxTn=maxTn.substring(maxTn.length()-4,
	                    	        maxTn.length());
	                        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){        
								Debug.log( Debug.SYSTEM_CONFIG, "NodeCount," +
								 "The value of MaxTN element is set into MessageObject:"
															+ maxTn);
							}
	                        kidsCount=-1;dataElemCount=-1;
	                    }     
	                  }
	              }
	            }
     }
     catch(Exception ex){
         ex.getStackTrace();
     }
    }              
    /**
     * 
     * @param args
     */
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

         Debug.log (Debug.ALL_ERRORS, "NodeCount is failed:"+
        "USAGE: jdbc:oracle:thin:@192.168.198.23:1521:ORCL subba subba");
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
            NodeCount nodeCount = new NodeCount();
            
            // Initializes the resources for Message Context.
            nodeCount.initialize("FULL_NEUSTAR_SOA","NodeCount");
          
            MessageProcessorContext mpx = new MessageProcessorContext();
            
            MessageObject mob = new MessageObject();
            // This sets the input messages for processing for MaxTN and SvID.
            mob.set("input_portingtn","540-001-1000-1500");
            mob.set("inputMessages","<?xml version=\"1.0\"?>" +
                    "<SOAMessage xmlns=\"urn:neustar:lnp:soa:1.0\" "+
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                    " <messageHeader><session_id>1134064800</session_id> " +
                     "<invoke_id>11308</invoke_id><npac_region_id>3</npac_region_id>"+ 
                     "<customer_id>1111</customer_id>"+
                     "<message_date_time>2005-12-12T11:30:18Z</message_date_time>"+ 
                    "</messageHeader><messageContent><NPACtoSOA>" +
                    "<SubscriptionVersionQueryReply>"+ 
                     " <query_status><status>success</status></query_status> "+
                     "    <version_list>" +
                      "<data> "+
                      "   <subscription_version_id>108341</subscription_version_id>"+ 
                      "  <subscription_version_tn>5300021280</subscription_version_tn>"+ 
                       "</data> "+
                       "<data> "+
                       "   <subscription_version_id>108341</subscription_version_id>"+ 
                       "  <subscription_version_tn>5300021280</subscription_version_tn>"+ 
                        "</data> "+
                        "<data> "+
                        "   <subscription_version_id>108341</subscription_version_id>"+ 
                        "  <subscription_version_tn>5300021280</subscription_version_tn>"+ 
                         "</data> "+
                         "<data> "+
                         "   <subscription_version_id>108341</subscription_version_id>"+ 
                         "  <subscription_version_tn>5300021280</subscription_version_tn>"+ 
                          "</data> "+
                          "<data> "+
                          "   <subscription_version_id>108341</subscription_version_id>"+ 
                          "  <subscription_version_tn>5300021280</subscription_version_tn>"+ 
                           "</data> "+
                           "<data> "+
                           "   <subscription_version_id>108341</subscription_version_id>"+ 
                           "  <subscription_version_tn>5300021280</subscription_version_tn>"+ 
                            "</data> "+ 
                       "<data> "+
                     "<subscription_version_id>108342</subscription_version_id>"+ 
                     "<subscription_version_tn>5300021287</subscription_version_tn>"+ 
                     "</data> </version_list></SubscriptionVersionQueryReply>"+ 
                       "</NPACtoSOA></messageContent></SOAMessage>");
            
            nodeCount.process(mpx,mob);
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

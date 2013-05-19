package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class SOALSRMessageBatchSeprator extends MessageProcessorBase{
	
    /**
     * Property indicating the order in which the XML need to process.
     */
    public static final String IS_PROCESS_ORDER_ASC_PROP = "IS_PROCESS_ORDER_ASC";
    
    public static final String TN_SVID_LIST_NODE = "TnSvIdList";
    
    public static final String TNSVIDLIST_XPATH_SVCreateNotification 
    		= SOAConstants.SOA_TO_UPSTREAM_BODY_PATH +"." +	SOAConstants.SV_CREATE_NOTIFICATION + 
    			"."+ SOAConstants.SUBSCRIPTION_NODE + "."+TN_SVID_LIST_NODE;
    
    public static final String TNSVIDLIST_XPATH_SVAttributeChangeNotification 
    		= SOAConstants.SOA_TO_UPSTREAM_BODY_PATH +"." +	SOAConstants.SV_ATR_CHANGE_NOTIFICATION +
    			"."+ SOAConstants.SUBSCRIPTION_NODE + "."+TN_SVID_LIST_NODE; 

    public static final String TN_SVID_NODE = "TnSvId";
    
    public static final String DELAY_PROP = "DELAY";
    
        
    // private members
    /*
     * It contains input_xml object
     */
    private String inputTnLoc = null;
	private String inputEndStationLoc = null;
	private String inputStartSvIdLoc = null;
	private String delay = null;
	
	private String inputTn = null;
	private String inputEndStation = null;
	private String inputStartSvId = null;
	
	
	/*
     * It contains Hash Map of TN and SvId of received notification. 
     */
    private Map<String, String> tn_svId = null;
    
    /*
     * It contains List of Tns 
     */
    private List<String> tnList = null;
    
    /**
	 * This variable contains  MessageProcessorContext object
	 */
	private MessageProcessorContext mpContext = null;
	
	/**
	 * This variable contains  MessageObject object
	 */
	private MessageObject inputObject = null;

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
        
        inputTnLoc = getRequiredPropertyValue( 
				SOAConstants.INPUT_LOC_TN_PROP, errorBuffer);

		inputEndStationLoc = getPropertyValue( 
				SOAConstants.INPUT_LOC_END_STATION_PROP);

		delay = getPropertyValue(DELAY_PROP);
		
		
		inputStartSvIdLoc = getRequiredPropertyValue( 
				SOAConstants.INPUT_LOC_SVID_PROP, errorBuffer);

		// If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }

        Debug.log( Debug.SYSTEM_CONFIG, "SOALSRMessageBatchSeprator: Initialization done." );
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
        
        ThreadMonitor.ThreadInfo tmti = null;
        
        if(inputObject == null)
        {
            return null;
        }
        
        this.mpContext = mpcontext;
		
		this.inputObject = inputObject;
		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			if(delay != null){
			//Putting delay for 30 second to start the processing of dequeued messages. 
			//Fix for TD #12786
			try{
				if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		        	Debug.log(Debug.MSG_STATUS, "Sleeping for 30 seconds to start processing ...");
		        
				Thread.currentThread().sleep(Integer.parseInt(delay)*1000);        
		        
		        }catch(Exception e){
		        	Debug.log(Debug.ALL_ERRORS, "Error in thread sleep.");        
		        }
			}
			
			if(toProcessorNames == null)
	        {
	            // Make sure this is at least processor to receive results.
	            String errMsg = "ERROR: No next processors available.";
	            Debug.log(Debug.MSG_STATUS, errMsg);
	            throw new ProcessingException(errMsg);
	        }
	        
	      //get the number of processors connected to the current message processor
	        int processorsNo = toProcessorNames.length;
	        	        
	        //Creating document object to parse the input XML
	        Document soaMessage_doc = inputObject.getDOM();
	        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        	Debug.log(Debug.MSG_STATUS,"Incoming XML \n[" +XMLLibraryPortabilityLayer.
	        			convertDomToString(soaMessage_doc)+"]");
	        
	        XMLMessageParser xmp = new XMLMessageParser(soaMessage_doc);
	        
	        XMLMessageGenerator xmg = null;
	        String notification = null;
	        
	        boolean flag = xmp.exists(TNSVIDLIST_XPATH_SVCreateNotification) || 
	        						xmp.exists(TNSVIDLIST_XPATH_SVAttributeChangeNotification);
	        if(flag){
	        	
	        	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        		Debug.log(Debug.MSG_STATUS, "Notification is coming with TNSvIdList Node");
	        	
	        	if(xmp.exists(TNSVIDLIST_XPATH_SVCreateNotification))
	        		notification = SOAConstants.SV_CREATE_NOTIFICATION;
	        	
	        	if(xmp.exists(TNSVIDLIST_XPATH_SVAttributeChangeNotification))
	        		notification = SOAConstants.SV_ATR_CHANGE_NOTIFICATION;
	        	
	        	NodeList ndList = soaMessage_doc.getElementsByTagName(TN_SVID_NODE);
	            
	        	int nodeLstLen = ndList.getLength();
	        	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        		Debug.log(Debug.MSG_STATUS,"Number of SvId nodes are:"+nodeLstLen);
		        
	        	//create NVPair array of size tnList list to send one by one notification with 
		        //single TN and SVID.
	        	
	        	NVPair[] contents_SvId_list = new NVPair [ nodeLstLen];
		        MessageObject mObj_svIdList = null;
		        
				tn_svId = new HashMap<String, String>();
				tnList = new ArrayList<String>();
				
	        	for(int i=0;i<nodeLstLen;i++){
	        		
					Node tnSvIdNode = ndList.item(i);
					NodeList svIdNodeList = tnSvIdNode.getChildNodes();
					String tn =null; 
					String svId =null;
					
					for (int count = 0; count < svIdNodeList.getLength(); count++) {
						Node node = svIdNodeList.item(count);
						//Tn node found
						if(node.getNodeName().equalsIgnoreCase(SOAConstants.TN_NODE)){
							tn = XMLMessageBase.getNodeValue(node);
						}
						//if SvId node found
						if(node.getNodeName().equalsIgnoreCase(SOAConstants.SVID_NODE)){
							svId = XMLMessageBase.getNodeValue(node);
						}
					}
					
					//added tn and svId in hashmap
					tn_svId.put(tn,svId);
					tnList.add(tn);
				
	        	}
	        	//generate new notification xml
	        	xmg = new XMLMessageGenerator(soaMessage_doc);
	        	
	        	//remover TnSvIdList node from XML
	        	String path = null;
				
		        path= SOAConstants.SOA_TO_UPSTREAM_BODY_PATH +"." + notification +"." + 
						SOAConstants.SUBSCRIPTION_NODE +"."+TN_SVID_LIST_NODE;
		        
		       if(path != null)
	        		xmg.removeNode(path);
	        	
	        	if(tnList != null){
		        	Iterator<String> tnItr = tnList.iterator();
		        	int i = 0;
		        	//iterate over TnList
		        	while(tnItr.hasNext()){
		        	
		        		String tn = (String)tnItr.next();
		        		String svId = (String)tn_svId.get(tn);
		        		//set tn and SvId in notification XML 
		        		xmg.setValue(path +"." +TN_SVID_NODE+ "." +SOAConstants.TN_NODE, tn);
		        		xmg.setValue(path +"." +TN_SVID_NODE+ "." +SOAConstants.SVID_NODE, svId);
												
						mObj_svIdList = new MessageObject(xmg.generate());
		        		for (int j = 0; j < processorsNo; j++)
			            {
			        		contents_SvId_list[(i*(processorsNo))+j] = new NVPair ( toProcessorNames[j], mObj_svIdList );
			                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			                {
			                    Debug.log(Debug.MSG_STATUS, "NEXT PROCESSOR-->"+toProcessorNames[j]+"\n"+
			                              "MESSAGE OBJECT CONTENT------------>"+mObj_svIdList.describe());
			                }
			                    
			            }
		        		i++;
		        	}
	        	}
	        	
	        	return contents_SvId_list;
	        
	        }else{
	        	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
	        		Debug.log(Debug.MSG_STATUS, "Notification is coming with TNSvIdRange Node");
	        	
	        	//get the input TN Value from property
		        inputTn =  getValue(inputTnLoc);
		        
		        //get the input TN End Station Value from property
		        inputEndStation = getValue(inputEndStationLoc);
		        
		        //get the start SVID Value from property
		        inputStartSvId =  getValue(inputStartSvIdLoc);
		        
		       	//Does no processing if the Tn value is not available..
				if (!StringUtils.hasValue(inputTn)){
					return formatNVPair(inputObject);
				}
				
				//get List of TNS based on the provided TN and End Station of notification
				tnList = SOAUtility.getSvRangeTnList(inputTn, inputEndStation);
				
		        //get hash map of TN and SVID 
				tn_svId = getTNSvIdMap(inputStartSvId,tnList);
				
		        
		        //get the number of TNS present in TN Range
		     	int nRecords = tnList.size();
		     	
		     	
		        
		        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		        	Debug.log(Debug.MSG_STATUS,"Number of processors"+processorsNo);
		        
		        //create NVPair array of size tnList list to send one by one notification with 
		        //single TN and SVID.
		        NVPair[] contents = new NVPair [ nRecords];
		
		        Iterator<String > tnListItr = tnList.iterator();
		        
		        MessageObject mObj = null;
		        int i = 0;
		        //iterate over TN List to create SV notification xmls
		        while(tnListItr.hasNext())
		        {
		        	
		        	//get the TN value from list
		        	String tn = (String)tnListItr.next();
		        	
		        	//get the SVID value from map with key TN
		        	String svId = (String)tn_svId.get(tn);
		        	
		        	//set TN to Start TN node of notification
		        	NodeList nodeList = soaMessage_doc.getElementsByTagName(SOAConstants.TN_NODE);
		        	Debug.log(Debug.MSG_STATUS, "TN To populate in notification"+tn);
		        	nodeList.item(0).getAttributes().item(0).setTextContent(tn);
		        	
		        	//set TN to End station node of notification
		        	nodeList = soaMessage_doc.getElementsByTagName(SOAConstants.TN_ENDSTATION_PROP);
		        	nodeList.item(0).getAttributes().item(0).setTextContent(tn.substring(8));
		        	
		        	//set SVID to Start SVID node of notification
		        	nodeList = soaMessage_doc.getElementsByTagName(SOAConstants.START_SVID);
		        	Debug.log(Debug.MSG_STATUS, "SVID To populate in notification"+svId);
		        	
		        	nodeList.item(0).getAttributes().item(0).setTextContent(svId);	        	
		        	
		        	//set SVID to End SVID node of notification
		        	nodeList = soaMessage_doc.getElementsByTagName(SOAConstants.END_SVID);
		        	nodeList.item(0).getAttributes().item(0).setTextContent(svId);
		        	
		        	//get new object containing modified notification XML
		        	String doc = new String();
		        	doc = XMLLibraryPortabilityLayer.convertDomToString(soaMessage_doc);
		        	
		        	mObj = new MessageObject(doc);
		        	
		        	for (int j = 0; j < processorsNo; j++)
		            {
		                contents[(i*(processorsNo))+j] = new NVPair ( toProcessorNames[j], mObj );
		                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
		                {
		                    Debug.log(Debug.MSG_STATUS, "NEXT PROCESSOR-->"+toProcessorNames[j]+"\n"+
		                              "MESSAGE OBJECT CONTENT------------>"+mObj.describe());
		                }
		                    
		            }
		        	i++;
		        }
		        return contents;
	        }
	        //return array of NVPair objects containing all notifications as input object
	        
	        
		}finally
        {
        	ThreadMonitor.stop(tmti);
        }
    }

    /**
	 * This method tokenizes the input string and return an
	 * object for exsisting value in context or messageobject.
	 *
	 * @param  locations as a string
	 *
	 * @return  object
	 *
	 * @exception  ProcessingException  Thrown if processing fails.
	 * @exception  MessageException  Thrown if message is bad.
	 
	 */
    protected String getValue(String locations) throws MessageException,
	ProcessingException {
		StringTokenizer st = new StringTokenizer(locations,
				DBMessageProcessorBase.SEPARATOR);
		
		String tok = null;
		
		// While tokens are available ...
		while (st.hasMoreTokens()) {
			tok = st.nextToken();
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "Checking location [" + tok	+ "] for value...");
			
			// if the value of token exists in context or messageobject.
			if (exists(tok, mpContext, inputObject)) {
				return ((String) get(tok, mpContext, inputObject));
			}
		}
		
		return null;
	}
	
    /**
     * This Method returns map of TN and SVID where TN is key and SVID is value
     */
    private Map<String,String> getTNSvIdMap(String startSvId, List<String> tnList){
    	
    	Map<String,String> tnsvIdMap = new HashMap<String,String>();
    	
    	int svIdValue = Integer.parseInt(startSvId);
    	
    	if(tnList != null){
    		Iterator<String> tnListItr = tnList.iterator();
    		while(tnListItr.hasNext()){
    			
    			String tn = (String)tnListItr.next();
    			tnsvIdMap.put(tn,String.valueOf(svIdValue));
    			
    			svIdValue ++;
    		}
    	}
    	
    	if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
    		Debug.log(Debug.MSG_STATUS, "Returned TN SVID Map "+tnsvIdMap);
    	}
    	return tnsvIdMap;
    }
	
    
    public static void main(String[] args) throws Exception
    {

      System.setProperty("NF_REPOSITORY_ROOT","d:\\dev\\basicsoa\\repository");
        Properties props = new Properties();
        props.put( "DEBUG_LOG_LEVELS", "ALL" );
        props.put( "LOG_FILE", "d:\\QueueProducer.log" );
        Debug.showLevels( );
        Debug.configureFromProperties( props );

        /*if (args.length != 3)
        {
          System.out.println("JMSQueueProducer: USAGE:  "+
          " jdbc:oracle:thin:@192.168.97.45:1521:ORA10G SOADB b31 ");
          return;
        }*/
            DBInterface.initialize("jdbc:oracle:thin:@192.168.148.34:1521:NOIDADB", "SOADB", "SOADB");

            String xmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAMessage><SOAToUpstream><SOAToUpstreamHeader>" +
            		"<DateSent value=\"09-30-2010-100933AM\"/><RegionId value=\"0003\"/></SOAToUpstreamHeader><SOAToUpstreamBody>" +
            		"<SvAttributeChangeNotification><Subscription><TnSvIdList><TnSvId><Tn value=\"305-591-4371\"/>" +
            		"<SvId value=\"932084\"/></TnSvId><TnSvId><Tn value=\"305-591-4372\"/><SvId value=\"932085\"/>" +
            		"</TnSvId><TnSvId><Tn value=\"305-591-4373\"/><SvId value=\"932087\"/></TnSvId><TnSvId><Tn value=\"305-591-4374\"/>" +
            		"<SvId value=\"932088\"/></TnSvId></TnSvIdList></Subscription><SPID value=\"1111\"/><ModifiedData>" +
            		"<NewSPDueDate value=\"09-30-2010-103000AM\"/></ModifiedData></SvAttributeChangeNotification></SOAToUpstreamBody>" +
            		"</SOAToUpstream></SOAMessage>";
            //FileUtils.readFile( "d:\\JMSTextMessage.xml" );
            
            Document doc =  XMLLibraryPortabilityLayer.convertStringToDom(xmlText);
            MessageObject input = new MessageObject(doc);
            MessageProcessorContext ctx = new MessageProcessorContext();
            SOALSRMessageBatchSeprator producer = new SOALSRMessageBatchSeprator();
            // whats the key and type of mp ???
            //key FULL_NEUSTAR_SOA_RESPONSE
            //type EnqueueNotification
            producer.initialize("SOA_LSR_RECEIVE_REQUEST","BatchSeparator");
            try{
            producer.process(ctx,input);
            }catch(Exception e)
            {
                  System.out.println("rolling back...");
                  ctx.getDBConnection().commit();
                  producer.cleanup();
            }
            
    }

}

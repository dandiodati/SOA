/**
 * $Header: //spi/neustar_soa/adapter/messageprocessor/NodeValueReplacer.java#1 $
 */
	package com.nightfire.spi.neustar_soa.adapter.messageprocessor;


import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.NancValueMapping;


public class NodeValueReplacer extends MessageProcessorBase{	
	
	/*
	 * Property indicating the node name which value is t0 be replace.
	 */
	public static final String NODE_NAME = "NODE_NAME";
	
	/*
	 * Property indicating the node name which value is t0 be replace.
	 */
	public static final String FILE_PATH = "FILE_PATH";
	
	private List locations = null;
	private static String filePath = null;
	private Document dom = null;
	private static String path =null;
	private static HashMap <String , String > mappedValues = new HashMap<String, String>();
	
	/**
	 * Initializes this adapter with persistent properties
	 * 
	 * @param key
	 *            Property-key to use for locating initialization properties.
	 * 
	 * @param type
	 *            Property-type to use for locating initialization properties.
	 * 
	 * @exception ProcessingException
	 *                when initialization is not successful.
	 */
	public void initialize(String key, String type) throws ProcessingException {
		StringBuffer errorBuffer = new StringBuffer( );
	
		super.initialize(key, type);
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		 Debug.log(Debug.MSG_STATUS, StringUtils.getClassName(this)+ ": Initializing...");
		}
		
		filePath = getRequiredPropertyValue(FILE_PATH, errorBuffer );
		
		//Used to store the output locations and node names.ork
        locations = new LinkedList();
        
        for (int Ix = 0; true; Ix++) {
			
            String nodeName = getPropertyValue(PersistentProperty.getPropNameIteration(NODE_NAME, Ix));

			if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				Debug.log(Debug.MSG_STATUS, StringUtils.getClassName(this)
						+ ": NodeName["+nodeName+"]");
			}

			//stop processing when no more properties are specified
            if (!StringUtils.hasValue(nodeName))
                break;

            try {
                //create a new locations data and add it to the list
                LocationsData ld = new LocationsData(Ix, nodeName);
                locations.add(ld);

            } catch (Exception e) {
					
                throw new ProcessingException("Could not create the locations data description:\n" + e.toString());
            }
        }
        
        if ( locations.size() < 1 )
            errorBuffer.append( "At least one INPUT_NODE_NAME and OUTPUT_LOCATION must be specified." );

        // If any of the required properties are absent, indicate error to caller.
        if ( errorBuffer.length() > 0 )
        {
            String errMsg = errorBuffer.toString( );

            Debug.log( Debug.ALL_ERRORS, errMsg );

            throw new ProcessingException( errMsg );
        }
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
			Debug.log(Debug.MSG_STATUS, StringUtils.getClassName(this)
					+ ": Initialization is done.");
		}
		
	}
	/**
	 * Extract data values from the context/input
	 * 
	 * @param context
	 *            The context
	 * @param msgObj
	 *            Input message to process.
	 * 
	 * @return input message
	 * 
	 * @exception ProcessingException
	 *                Thrown if processing fails.
	 */
	public NVPair[] process(MessageProcessorContext context,
			MessageObject inputObj) throws MessageException,
			ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		if (inputObj == null || inputObj.getDOM() == null) {
			return null;
		}
		if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS,"Processing...");
		}
		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			path = getString(filePath);
			File mappingFile = new File(path);
			
			dom = inputObj.getDOM();
			Iterator iter = locations.iterator();
			
			while (iter.hasNext()) {
				try{
					LocationsData ld = (LocationsData) iter.next();
					if (ld.nodename != null){
						
						String nodeName = ld.nodename; 
						
						String xmlNodeValue = null;
						String mapNodeValue = null;
						
						NodeList ndl = dom.getElementsByTagName(nodeName);
							if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
								Debug.log(Debug.MSG_STATUS, " Total number of"+nodeName+ "node(s) found in the xml is:" + ndl.getLength());
							}
						
						//check for node existence in the input xml.
						for (int i = 0; i < ndl.getLength(); i++) {
						
						if (ndl.item(i) == null){
							if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
								Debug.log(Debug.MSG_STATUS, "["+nodeName+"] is " +"not present in INPUT XML, so skipping the process. ");
							}
						}
						else{
	
							if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
								Debug.log(Debug.MSG_STATUS, "["+nodeName+"] is present in INPUT XML ");
								Debug.log(Debug.MSG_STATUS, "Starts populating the INPUT XML with mapped values");
							}
							xmlNodeValue = ndl.item(i).getAttributes().item(0).getNodeValue();											
							
							if(NancValueMapping.isNancMapCached()){
								
								//retrieveing cached hash map 							
								mappedValues =NancValueMapping.getNancValueMapping();
															
							}else{
								//if map is not cached							
								mappedValues = NancValueMapping.loadAndGetMappingToHashMap(mappingFile); // load hashmap from file and return loaded hashmap					
							}
							
							mapNodeValue = mappedValues.get(xmlNodeValue);
			
							if(mapNodeValue == null){
								if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
								  Debug.log(Debug.MSG_STATUS, " : Node [" +xmlNodeValue+ "] is not present in the Mapping configuration property file.");
								}
							
							}else{
	                            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
									Debug.log(Debug.MSG_STATUS, " : Node [" +xmlNodeValue+ "] is present in the Mapping configuration property file and " +
													"the mapped Node value is ["+ mapNodeValue +"]");
								}
								/*
								 * Replacing the INPUT XML Node value with mapped value
								 */
								ndl.item(i).getAttributes().item(0).setTextContent(mapNodeValue);
								
								}						
						}
						}
						inputObj.set(dom);
					}				
				} catch (Exception te) {
						Debug.log(Debug.EXCEPTION_STACK_TRACE, StringUtils.getClassName(this)+ te.getMessage());
				}
			}
		}finally{
			ThreadMonitor.stop(tmti);
		}
		return (formatNVPair(inputObj));
		
		
	}
	
	
	private static class LocationsData {

        public final int index;
        public final String nodename;

        public LocationsData(int index, String staticValue) throws FrameworkException {

            this.index = index;
            this.nodename = staticValue;

        }

    }
	public static void main(String[] args) {

		/* Initialize Debug logging. */
		Hashtable htLogTable = new Hashtable();
		htLogTable.put(Debug.DEBUG_LOG_LEVELS_PROP, "ALL");
		htLogTable.put(Debug.LOG_FILE_NAME_PROP,
				"c:/runtime/NodeValueReplacer.log");
		htLogTable.put(Debug.MAX_DEBUG_WRITES_PROP, "10000");

		/* Turn on maximum diagnostic logging. */
		Debug.showLevels();
		Debug.configureFromProperties(htLogTable);
		Debug.enable(Debug.ALL_ERRORS);
		Debug.enable(Debug.ALL_WARNINGS);
		Debug.enable(Debug.MSG_STATUS);
		Debug.enable(Debug.EXCEPTION_STACK_TRACE);
		
		try {

			/* Database properties */
			DBInterface.initialize(
					"jdbc:oracle:thin:@192.168.150.249:1521:NOIDADB",
					"PRAVEENSOA", "PRAVEENSOA");
			Debug.log(Debug.MSG_STATUS,
							"NodeValueReplacer");
		} catch (DatabaseException e) {
			Debug.log(Debug.MAPPING_ERROR, "Database initialization failure: "
					+ e.getMessage());
		}
		
		String inputString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<SOAMessage>" +
				" <SvType value=\"VoIP\"/>" +
				"<SvType value=\"VoIP\"/>" +
				"<SvType value=\"VoIP\"/>" +
				"</SOAMessage>";
		
		MessageObject msob = new MessageObject(inputString);
		MessageProcessorContext mpx;
		try {
			mpx = new MessageProcessorContext();
			NodeValueReplacer nvr = new NodeValueReplacer();
			nvr.initialize("SOA_FORMAT_REQUEST", "SOAReqNodeValueReplacer");
			
				nvr.process(mpx, msob);
		}catch (ProcessingException e) {
				e.printStackTrace();
		}catch (MessageException e) {
				e.printStackTrace();
		}
		
	}
	
}

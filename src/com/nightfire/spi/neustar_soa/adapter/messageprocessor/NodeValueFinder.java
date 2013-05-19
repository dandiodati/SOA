package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;


/**
 * Extracts  xml strings from specified input locations and finds one or more
 * given node values to put them in one or more output locations.
 */

public class NodeValueFinder extends MessageProcessorBase {

    /*
     * Property prefix to be used as the input  location to extract the XML.
     */
    public static final String INPUT_LOCATION_PROP = "INPUT_LOCATION";
    /*
     * Property prefix to be used as the static value for the output location.
     * the result.
     */
    public static final String RESULT_LOCATION_PROP = "RESULT_LOCATION";
    /*
     * Property prefix to be used as the static value for the input node name 
     * 
     */
    public static final String INPUT_NODE_NAME_PROP = "INPUT_NODE_NAME";

    private String inputLocation;
    private List locations = null;
    /**
     * Loads the required property values into memory
     * 
     * @param key
     *            Property Key to use for locating initialization properties.
	 *            
     * @param type
     *            Property Type to use for locating initialization properties.
	 *            
     * @exception ProcessingException
     *                when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException {
        
        StringBuffer errorBuffer = new StringBuffer( );

        if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
			Debug.log(Debug.MSG_STATUS, "NodeValueFinder: Initializing ...");
		}

        super.initialize(key, type);
		
		//the location of the input xml.
        inputLocation = getRequiredPropertyValue(INPUT_LOCATION_PROP);

        //Used to store the output locations and node names.
        locations = new LinkedList();
        
        //loop until all output locations and node name values have been
        // extracted
        for (int Ix = 0; true; Ix++) {
			
        
            String nodeName = getPropertyValue(PersistentProperty.getPropNameIteration(INPUT_NODE_NAME_PROP, Ix));
            
			String outputLocation = getPropertyValue(PersistentProperty.getPropNameIteration(RESULT_LOCATION_PROP,Ix));
			
            //stop when no more properties are specified
            if (!StringUtils.hasValue(nodeName) && !StringUtils.hasValue(outputLocation))
                break;

            try {
                //create a new locations data and add it to the list
                LocationsData ld = new LocationsData(Ix, outputLocation, nodeName);
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

    }

    /**
     * 
     * Parse the input xml and extract the value of the given node and put this
     * value to the specified output location.
     * 
     * @param context
     *            The MessageProcessorContext with required information
     * 
     * @param msgObj
     *            The MesageObject to be sent to the next processor
     * 
     * @return NVPair[] Name-Value pair array of next processor name and the
     *         MessageObject passed in
     * 
     * @exception ProcessingException
     *                Thrown if processing fails.
     */
    public NVPair[] process(MessageProcessorContext context, MessageObject msgObj) throws MessageException, ProcessingException {
    	
    	ThreadMonitor.ThreadInfo tmti = null;
        String outputValue = null;
        Element element = null;
        NodeList nodelist = null;
        Document doc = null;
        String inputStr = null;
       		
        Debug.log(Debug.BENCHMARK, "NodeValueFinder: processing ...");

        if (msgObj == null) {
				
            return null;
        }
        try{
	        tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
	
	        //read the input string from the input context location.
	        inputStr = getString(inputLocation, context, msgObj);
			
	        //create XMLPlainGenerator object.
	        XMLPlainGenerator xpgDoc = new XMLPlainGenerator(inputStr);
	
	        if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
	          Debug.log(Debug.MSG_STATUS, "XML representation of the input string is: " + xpgDoc.describe());
			}
	        
	        //get the document object
	        doc = xpgDoc.getDocument();
	
	        element = doc.getDocumentElement();
			
			Iterator iter = locations.iterator();
	
			
	        while (iter.hasNext()) {
	
	            LocationsData ld = (LocationsData) iter.next();
	
	            //check if the location is valid
	
	            if (ld.location != null && ld.nodename != null) {
	
	                if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
	                  Debug.log(Debug.MSG_STATUS, "Extracting values: output location [ " + ld.location + " ]" + " and " + "input node [ " + ld.nodename + " ]");
					}
	
	                try {
	
	                    nodelist = element.getElementsByTagName(ld.nodename);
	
	                    Element temp = (Element) nodelist.item(0); //node
	                    
	                    if(temp == null || temp.getAttribute("value").equals("")){                    	
	                    	outputValue = "Null";
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
	                    	  Debug.log(Debug.MSG_STATUS, "Value of node [ " + ld.nodename + " ] " + " is " + " [ " + outputValue + " ]");
							}
	                    	this.set(ld.location, context, msgObj, outputValue);
	                    }
	                    else{
	                    	outputValue = temp.getAttribute("value");  //value
							if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
	                          Debug.log(Debug.MSG_STATUS, "Value of node [ " + ld.nodename + " ] " + " is " + " [ " + outputValue + " ]");
							}
	    					this.set(ld.location, context, msgObj, outputValue);
	                    }                    
						
	                } catch (Exception e) {						
	                    throw new ProcessingException("Exception in setting the value at the given output location:\n" + e.toString());
	                }
	            }
	
	        }
        }finally{
	        	ThreadMonitor.stop(tmti);	
	        }
        return (formatNVPair(msgObj));

    }

    private static class LocationsData {

        public final int index;
        public final String location;
        public final String nodename;

        public LocationsData(int index, String location, String staticValue) throws FrameworkException {

            this.index = index;
            this.location = location;
            this.nodename = staticValue;

        }

    }

}


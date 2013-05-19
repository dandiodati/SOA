package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.w3c.dom.Document;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

public class SOAInvalidXMLFileWriter extends MessageProcessorBase{

	/**
	 * Property indicating the location of batch directory.
	 */
	public static final String EXCEPTION_DIR = "EXCEPTION_DIR";
	/**
	 * Property indicating the location of SPID.
	 */
	public static final String SPID_LOCATION = "SPID_LOCATION";
	/**
	 * Property indicating the location of INPUT MESSAGE.
	 */
	public static final String INPUT_MESSAGE = "INPUT_MESSAGE";
	/**
	 * Property indicating the location of REQUEST HEADER.
	 */
	public static final String REQUEST_HEADER = "REQUEST_HEADER";
	
	
	private String expdir_property = null;
	private String spid_property = null;
	private String header_property = null;
	private String inputmessage_property = null;
	private String expdirValue = null;
	private String spidValue = null;
	private String inputXml = null;	
	private String bodyFileName = null;
	private String headerFileName = null;
	private String headerXml = null;
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
	 * @throws MessageException
	 */
	public void initialize(String key, String type) throws ProcessingException {

		super.initialize(key, type);
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS, ": Initialing...");
		}

		StringBuffer errorBuffer = new StringBuffer();

		expdir_property = getRequiredPropertyValue(EXCEPTION_DIR, errorBuffer);
		spid_property = getRequiredPropertyValue(SPID_LOCATION, errorBuffer);
		inputmessage_property = getRequiredPropertyValue(INPUT_MESSAGE,errorBuffer);
		header_property = getPropertyValue(REQUEST_HEADER);

		/*
		 * If any of the required properties are absent, indicate error to
		 * caller.
		 */
		if (errorBuffer.length() > 0) {
			String errMsg = errorBuffer.toString();
			Debug.log(Debug.ALL_ERRORS, errMsg);

			throw new ProcessingException(errMsg);
		}
	}
	/**
	 * 
	 * @param context
	 *            The context
	 * @param msgObj
	 *            Input message to process.
	 * 
	 * @return The given input, or null.
	 * 
	 * @exception ProcessingException:
	 *                Thrown if processing fails.
	 */
	
	public NVPair[] process(MessageProcessorContext context,
			MessageObject msgObj) throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;

		if (msgObj == null)
			return null; // Return null if no MessageObject.
        if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
		  Debug.log(Debug.MSG_STATUS," : Processing...");
		}
		
		boolean flag=false;
		
		try{
			
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			/* Getting the actual values of the properties */
			expdirValue = getString(expdir_property);
			spidValue = getString(spid_property, context, msgObj);
			Object input = get(inputmessage_property, context, msgObj);
			
			if(header_property != null){
				headerXml = getString(header_property, context, msgObj);
				flag=true;
			}
			
			if(input instanceof Document)
	        {
				inputXml = XMLLibraryPortabilityLayer.convertDomToString((Document) get( inputmessage_property, context, msgObj));
	        }
	        else
	        {
	        	inputXml=(String)input;
	        }
			try{
				File dir = new File(expdirValue);
				if(!dir.isDirectory()){
					//Create new exception folder, If does not exist
					dir.mkdir();
	
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					  Debug.log(Debug.MSG_STATUS,": Exception folder directory does not exist , SO creating it.");
					}
				}
				//get the current time
				SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss_SS");
				String dateString = sdf.format(new Date());
				
				if(spidValue.equals("null") || !StringUtils.hasValue(spidValue)){
					
					if(flag){
						headerFileName = expdirValue + "/" + dateString + "_Header.xml";
					    if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						    Debug.log(Debug.MSG_STATUS, ": Invalid header XML is "+ headerXml.toString());
						}
					}				 
					bodyFileName = expdirValue + "/" + dateString + "_Body.xml";
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){				
					  Debug.log(Debug.MSG_STATUS,": Generated, " +"body file name["+ bodyFileName+"]");
					}
				}
				else{
					
					if(flag){
						headerFileName = expdirValue + "/" + spidValue + "_" + dateString + "_Header.xml";
	
						if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
						   Debug.log(Debug.MSG_STATUS, ": Invalid header XML is "+ headerXml.toString());
						}
					}				
					bodyFileName = expdirValue +"/" + spidValue + "_" + dateString + "_Body.xml";
	
					if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
					   Debug.log(Debug.MSG_STATUS, ": Generated, " +"body file name["+ bodyFileName+"");
					}
				}
	            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
				   Debug.log(Debug.MSG_STATUS, ": Invalid body XML is "+ inputXml.toString());
				}
				
				//Writing the invalid input XML to exception folder
				if(flag){
					FileUtils.writeFile( headerFileName, headerXml.toString());
				}
				FileUtils.writeFile( bodyFileName, inputXml.toString());		
			}
			catch(FrameworkException fe){
				
				Debug.log(Debug.ALL_ERRORS, ": Could write the input message to exception folder" + fe.getMessage());
			}
			catch(Exception e){
				
				Debug.log(Debug.ALL_ERRORS, ": Could not process the invalid input XML" + e.getMessage());
			}
		}finally
		{
			ThreadMonitor.stop(tmti);
		}
		
		return formatNVPair(msgObj);
	}	
}

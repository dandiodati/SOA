/**
 * $Header: //spi/neustar_soa/adapter/messageprocessor/XMLNodeValueReplacer.java#1 $
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.io.StringWriter;
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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/*
 * This purpose of this class is to replace the value separated tokens inside
 * the INPUT MESSAGE XML nodes.
 */
public class XMLNodeValueReplacer extends MessageProcessorBase {

	/*
	 * Property indicating the location for INPUT MESSAGE.
	 */
	public static final String XML_LOCATION_PROP = "INPUT_LOCATION";

	/*
	 * Property indicating the location for INPUT MESSAGE.
	 */
	public static final String NODE_NAME_PROP = "NODE_NAME";

	/*
	 * Property indicating the location for INPUT MESSAGE.
	 */
	public static final String TOKEN_REPLACER_PROP = "TOKEN_REPLACER";

	/*
	 * Property indicating the location for INPUT MESSAGE.
	 */
	public static final String TOKEN_TOBE_REPLACED_PROP = "TOKEN_TOBE_REPLACED";

	/*
	 * Property indicating the output location.
	 */
	
	private static String inputXml = null;
	String xml = null;
	private List locations = null;
	
	private static String className = "XMLNodeValueReplacer";

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
		super.initialize(key, type);

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, className + ": Initialing...");
		
		inputXml = getRequiredPropertyValue(XML_LOCATION_PROP);
		
		locations = new LinkedList();
		
		for (int Ix = 0; true; Ix++) {
			
			String nodeName = getPropertyValue(PersistentProperty.getPropNameIteration(NODE_NAME_PROP, Ix));							  
			
			String tokenReplacer = getPropertyValue(PersistentProperty.getPropNameIteration(TOKEN_REPLACER_PROP, Ix));
			
			String tokenToBeReplaced = getPropertyValue(PersistentProperty.getPropNameIteration(TOKEN_TOBE_REPLACED_PROP, Ix));

			// stop when no more properties are specified
			if (!StringUtils.hasValue(nodeName) && !StringUtils.hasValue(tokenReplacer))
				break;

			try {

				LocationsData ld = new LocationsData(Ix, nodeName,  tokenReplacer, tokenToBeReplaced);				
				
				// Add the properties value to list.
				locations.add(ld);				 
				
			} catch (Exception e) {

				throw new ProcessingException("Could not create the locations data description:\n" + e.toString());
			}
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, className + ": Initialization is done.");
	}

	/**
	 * Extract data values from the context/input Replace the tokens of node
	 * value with token replacer string.
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
	public NVPair[] process(MessageProcessorContext context, MessageObject obj) throws MessageException, ProcessingException {
		
		ThreadMonitor.ThreadInfo tmti = null;
		if (obj == null || obj.getDOM() == null) {
			return null;

		}
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, className + " : Processing...");

		Document dom = null;
		NodeList ndl = null;
		Element element = null;
		boolean flag = false;
		try
		{
		tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
		
		xml = getString(inputXml, context, obj);

		if (inputXml.equalsIgnoreCase("INPUT_MESSAGE")) {

			flag = true;
			obj.set(xml);
			dom = obj.getDOM();

		} else {
		
			dom = XMLLibraryPortabilityLayer.convertStringToDom(xml);
		}
		
		for (Iterator itr = locations.iterator(); itr.hasNext();) {
			LocationsData lData = (LocationsData) itr.next();

			try {
				if (lData.replacer != null && lData.nodeName != null && lData.tokenName != null) {
		
					ndl = dom.getElementsByTagName(lData.nodeName);
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						
						Debug.log(Debug.MSG_STATUS, className + " : Tn node value"
								+ ndl.item(0).getAttributes().getNamedItem("value").getTextContent().toString());
					}					
					if (ndl.getLength() > 0) {
					
						element = (Element) ndl.item(0);
					}
					
					if (element == null || element.getAttribute("value").equals("")) {

						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, "The node [" + lData.nodeName + "] is not present " + "in the given input XML.");
					} 
					else 
					{
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, "The given [" + lData.nodeName + "] is present " + "in the given input XML.");
						
						if (!(element.getAttribute("value").equals(""))) {
							
								
								String replacedAttrValue = element.getAttribute("value").replaceAll(" ","");
								
								String replacedAttrValueWithComma = replacedAttrValue.replaceAll(lData.tokenName.toString() + "{1,}", lData.replacer.toString()).trim();
								
								String str = replacedAttrValueWithComma.replaceAll(lData.replacer.toString() + "{2,}", lData.replacer.toString()).trim();
																								
								if(str.startsWith(lData.replacer)){
									str = str.substring(1, str.length());									
								}
								if(str.endsWith(lData.replacer)){
									str = str.substring(0, str.length()-1);
								}
								
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
									Debug.log(Debug.MSG_STATUS, "Formatted TN node value -[" + str+"]");
								element.setAttribute("value", str);

								obj.set(dom);
							}
						else {
							if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
								Debug.log(Debug.MSG_STATUS, "The given node [" + lData.nodeName + "] of input XML "
									+ "does not contain VALUE attributes.");
						}
					}
				}
			} catch (Exception ex) {
				if ( Debug.isLevelEnabled( Debug.ALL_ERRORS )){
					
					Debug.log(Debug.ALL_ERRORS, "could not replce the token replacer[" + lData.replacer + "] in " + "the node [" + lData.nodeName
							+ "] in the given input XML.");
				}
			}	
			
		}
		}
		finally
		{
			ThreadMonitor.stop(tmti);
		}
		return formatNVPair(obj);
	}

	private static class LocationsData {

		public final int index;
		public final String replacer;
		public final String nodeName;
		public final String tokenName;

		public LocationsData(int index, String nodeName, String replacer, String tokenName) throws FrameworkException {

			this.index = index;
			this.nodeName = nodeName;
			this.replacer = replacer;
			this.tokenName = tokenName;
		}

	}

}

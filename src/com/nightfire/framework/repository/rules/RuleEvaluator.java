package com.nightfire.framework.repository.rules;

import org.w3c.dom.Document;

import com.nightfire.framework.repository.rules.config.*;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.parser.MessageParserException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.Debug;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Rule evaluator class evaluates rules of configuration file (named as
 * <request>.xml) located under given category. Once rules are evaluated and
 * corresponding output is returned.
 */
public class RuleEvaluator {

	/**
	 * Constructor loads all configuration files placed under given category.
	 */
	private RuleEvaluator(String category) throws FrameworkException {

		this.category = category;

		loader = new ConfigFilesLoader();

		// Load configuration files based on given category
		loader.load(category);

		// Populate config containers
		cfgContainer = new ConfigContainer();

		// Populate container
		cfgContainer.setContainer(loader.getConfigContainer());

	}

	private static Map<String, RuleEvaluator> ruleEvaluatorObjs = new HashMap<String, RuleEvaluator>();

	/**
	 * Method returns singleton instance of this class for each category.
	 * 
	 * @return singleton object of RuleEvaluator based on supplied category
	 */
	public static RuleEvaluator getInstance(String category)
			throws FrameworkException {
		
		
		if (ruleEvaluatorObjs.containsKey(category)) {

			return ruleEvaluatorObjs.get(category);
		}

		synchronized (RuleEvaluator.class) {

			if (ruleEvaluatorObjs.containsKey(category)) {

				return ruleEvaluatorObjs.get(category);
			}
			
			ruleEvaluator = new RuleEvaluator(category);

			ruleEvaluatorObjs.put(category, ruleEvaluator);
		}

		return ruleEvaluator;
	}

	/**
	 * Method evaluates rules and returns output properties. Each rule is
	 * evaluated either on body or header (based on 'applyOn' flag in request
	 * specific configuration file)
	 * 
	 * @param header
	 *            xml
	 * @param body
	 *            xml
	 * @return output properties
	 * @throws FrameworkException
	 */
	public Map<String, String> evaluateRule(String header, String body)
			throws FrameworkException {

		Document headerDocument = null;
		Document bodyDocument = null;

		try {
			
			headerDocument = XMLLibraryPortabilityLayer
					.convertStringToDom(header);

			bodyDocument = XMLLibraryPortabilityLayer.convertStringToDom(body);

		} catch (MessageException e) {

			throw new FrameworkException(e);
		}
		return evaluateRule(headerDocument, bodyDocument);
	}

	/**
	 * Method evaluates rules and returns output properties. Each rule is
	 * evaluated either on body or header (based on 'applyOn' flag in request
	 * specific configuration file)
	 * 
	 * @param header
	 * @param body
	 * @return output properties
	 * @throws FrameworkException
	 */
	public Map<String, String> evaluateRule(Document header, Document body)
			throws FrameworkException {

		Document xmlDoc = null;

		// Get request name from header

		String requestName = getRequestName(header);

		// Get config object from container
		RequestConfig configObj = cfgContainer.getRequestCfg(this.category,
				requestName.toLowerCase());
		
		if (Debug.isLevelEnabled(Debug.XML_BASE)) {
			Debug.log(Debug.XML_BASE, "Configuration for request [ "
					+ requestName + "] " + "is found : ["
					+ configObj + "]");
		}

		if (configObj == null) {
			
			if (Debug.isLevelEnabled(Debug.XML_BASE)) {
				Debug.log(Debug.XML_BASE, "Configuration for request [ "+ requestName + "] is ["+configObj+"] ");
			}
			
			return null;
		}

		List<RuleConfig> ruleCfgList = configObj.getRuleCfgList();

		for (RuleConfig ruleConfig : ruleCfgList) {

			String applyOnFlag = ruleConfig.getApplyOnFlag();

			if (HEADER.equals(applyOnFlag)) {

				xmlDoc = header;

			} else {

				xmlDoc = body;
			}

			// Evaluate rule
			if (evaluateRule(ruleConfig, xmlDoc)) {
				return ruleConfig.getAllOutputProps();
			}

		}

		return null;

	}

	/**
	 * Method evaluates xpath and returns corresponding result
	 * 
	 * @param ruleCfg
	 * @param xmlDoc
	 * @return
	 * @throws FrameworkException
	 */
	public boolean evaluateRule(RuleConfig ruleCfg, Document xmlDoc)
			throws FrameworkException {

		// Get configured xpath
		String xpath = ruleCfg.getXpath();

		boolean result;

		try {

			ParsedXPath parseXpathForParam = new ParsedXPath(xpath);

			// Evaluate xpath
			result = parseXpathForParam.getBooleanValue(xmlDoc
					.getDocumentElement());

		} catch (FrameworkException e) {
			Debug.log(Debug.ALL_ERRORS, Debug.getStackTrace(e));
			throw new FrameworkException(
					"Failed to evaluate rule's xpath expressions: \n"
							+ Debug.getStackTrace(e));
		}

		if (Debug.isLevelEnabled(Debug.XML_BASE)) {
			Debug.log(Debug.XML_BASE, "Evaluated rule:  name ["
					+ ruleCfg.getRuleName() + "]," + " xpath [" + xpath
					+ "],  result [" + result + "], output properties [" + ruleCfg.getAllOutputProps()+ "]");
		}

		return result;
	}

	/**
	 * Get value of Request attribute.
	 * 
	 * @param headerDocument
	 * @return
	 * @throws FrameworkException
	 */
	private static String getRequestName(Document headerDocument)
			throws FrameworkException {
		String requestName = null;

		try {

			XMLMessageParser parser = new XMLMessageParser(headerDocument);

			requestName = parser.getAttributeValue(REQUEST_SUBNODE,
					REQUEST_SUBNODE_ATTRIBUTE);

			if (!StringUtils.hasValue(requestName)) {
				throw new FrameworkException(
						"Not a valid value for 'Request' attribute in Header xml");
			}

		} catch (MessageParserException e) {
			throw new FrameworkException("Failed to parse Header xml :"
					+ Debug.getStackTrace(e));
		}

		return requestName;
	}

	private ConfigFilesLoader loader;

	private static RuleEvaluator ruleEvaluator;
	private static final String REQUEST_SUBNODE = "Request";
	private static final String HEADER = "header";
	private static final String REQUEST_SUBNODE_ATTRIBUTE = "value";
	private ConfigContainer cfgContainer;
	private String category;

}

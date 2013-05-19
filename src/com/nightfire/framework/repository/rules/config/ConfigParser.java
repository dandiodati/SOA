package com.nightfire.framework.repository.rules.config;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 * Class parses request specific configuration.
 */
public class ConfigParser {

	/**
	 * Method to get singleton instance of ConfigParser
	 * 
	 * @return instance of ConfigParser
	 */

	public static ConfigParser getInstance() {

		if (parseObj != null) {
			return parseObj;
		}

		synchronized (ConfigParser.class) {

			if (parseObj == null) {
				parseObj = new ConfigParser();
			}
		}

		return parseObj;
	}

	/**
	 * Method to parse request specific configuration file
	 * 
	 * @param document
	 * @return RequestConfig
	 */
	public RequestConfig parse(Document document) {

		RequestConfig requestCfg = new RequestConfig();

		// Parsing 'rule' nodes
		NodeList ruleList = document.getDocumentElement().getElementsByTagName(
				CONFIG_NODE_RULE);

		for (int i = 0; i < ruleList.getLength(); i++) {

			RuleConfig ruleConfig = new RuleConfig();

			Node ruleNode = ruleList.item(i);

			if (ruleNode instanceof Element) {

				Element ruleNodeElem = (Element) ruleNode;

				// Getting and set 'rule' node's attribute (name) value
				ruleConfig.setRuleName(ruleNodeElem
						.getAttribute(CONFIG_RULE_ATTRIBUTE_NAME));

				// Getting and set 'rule' node's attribute (applyOn) value
				ruleConfig.setApplyOnFlag(ruleNodeElem
						.getAttribute(CONFIG_RULE_ATTRIBUTE_APPLY_ON));

				// Getting 'xpath' node of 'rule' node
				NodeList xpathNodeLst = ruleNodeElem
						.getElementsByTagName(CONFIG_NODE_XPATH);
				Node xpathNode = xpathNodeLst.item(0);

				if (xpathNode instanceof Element) {

					Element xpathElem = (Element) xpathNode;

					// Setting 'xpath' node's text value
					ruleConfig.setXpath(xpathElem.getTextContent());
				}

				// Getting 'output' node of 'rule' node
				NodeList outputNodeLst = ruleNodeElem
						.getElementsByTagName(CONFIG_NODE_OUTPUT);
				Node outputNode = outputNodeLst.item(0);

				NodeList outputChildNodeLst = outputNode.getChildNodes();

				for (int j = 0; j < outputChildNodeLst.getLength(); j++) {

					Node outputChildNode = outputChildNodeLst.item(j);

					if (outputChildNode instanceof Element) {

						Element outPutChildElem = (Element) outputChildNode;

						// Adding into map:
						// key = Child node name of output
						// vale = Attribute's value of child node of output

						ruleConfig.addOutputProp(outPutChildElem.getNodeName(),
								outPutChildElem
										.getAttribute(CONFIG_ATTRIBUTE_VALUE));
					}
				}
			}

			// Adding rule config object into request config list
			requestCfg.addRuleCfg(ruleConfig);
		}

		return requestCfg;

	}

	private static final String CONFIG_NODE_RULE = "rule";
	private static final String CONFIG_RULE_ATTRIBUTE_NAME = "name";
	private static final String CONFIG_NODE_OUTPUT = "output";
	private static final String CONFIG_NODE_XPATH = "xpath";
	private static final String CONFIG_RULE_ATTRIBUTE_APPLY_ON = "applyOn";
	private static final String CONFIG_ATTRIBUTE_VALUE = "value";
	private static ConfigParser parseObj;

}

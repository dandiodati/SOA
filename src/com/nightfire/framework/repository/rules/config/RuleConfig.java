package com.nightfire.framework.repository.rules.config;

import java.util.Map;
import java.util.HashMap;

/**
 * Class containing configuration for 'rule' nodes defined in
 * <install-root>/repository/DEFAULT/<category>/<request>.xml file.
 */
public class RuleConfig {

	public RuleConfig() {
		output = new HashMap<String, String>();
	}

	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public String getApplyOnFlag() {
		return applyOnFlag;
	}

	public void setApplyOnFlag(String applyOnFlag) {
		this.applyOnFlag = applyOnFlag;
	}

	public void getOutputProp(String propName) {

		output.get(propName);
	}

	public Map<String, String> getAllOutputProps() {

		return output;
	}

	public void addOutputProp(String propName, String propValue) {

		output.put(propName, propValue);
	}

	public String toString() {

		StringBuilder strBuilder = new StringBuilder();

		strBuilder.append("\n rule (name) :").append(ruleName).append("\n");
		strBuilder.append(" rule (applyOn) : ").append(applyOnFlag).append("\n");
		strBuilder.append(" xpath : ").append(xpath).append("\n");
		strBuilder.append(" output : ").append(output).append("\n");

		return strBuilder.toString();
	}

	private String applyOnFlag;
	private String ruleName;
	private String xpath;
	private Map<String, String> output;
}

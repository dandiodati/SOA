package com.nightfire.framework.repository.rules.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Class containing configuration for <install-root>/repository/DEFAULT/<category>/<request>.xml
 * file.
 */
public class RequestConfig {

	public RequestConfig() {

		ruleCfgList = new ArrayList<RuleConfig>();
	}

	public void addRuleCfg(RuleConfig ruleCfg) {
		ruleCfgList.add(ruleCfg);
	}

	public List<RuleConfig> getRuleCfgList() {
		return ruleCfgList;
	}

	public String toString() {

		StringBuilder strBuilder = new StringBuilder();

		strBuilder.append(ruleCfgList);

		return strBuilder.toString();
	}

	private List<RuleConfig> ruleCfgList;
}

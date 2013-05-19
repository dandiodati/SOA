package com.nightfire.webgui.core.tag.util;

// JDK import

// Nightfire import
import com.nightfire.webgui.core.tag.TagConstants;
import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.message.common.xml.XMLGenerator;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.message.MessageException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.jsp.PageContext;

import org.w3c.dom.Document;

/**
 * This class provides means to generate java-script based client-side rules on the GUI
 */
public class RuleGenerator
{
    private StringBuffer ruleScript;
    private StringBuffer callAllScript;
    private String ruleFileLocation = "";

    public static final String NODE_RULES_CONTAINER = "RulesContainer";
    public static final String NODE_CONDITION = "Condition";
    public static final String NODE_ACTION = "Action";
    public static final String NODE_SEP = ".";
    public static final String NODE_RULE_INCLUDES_CONTAINER = "RuleIncludesContainer";
    public static final String NODE_RULE_INCLUDES = "RuleInclude";

     // Constructor.
    public RuleGenerator() {}

    public String createRulesScript (PageContext pageContext, String ruleResourceName) throws ServletException {
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS, "RuleGenerator: trying to locate resource: " + ruleResourceName);
        }

        try
        {
            ServletContext servletContext = pageContext.getServletContext();
            Object resourceRes = ServletUtils.getLocalResource(servletContext, ruleResourceName, ServletConstants.XML_DOC_DATA, true);

            if (!StringUtils.hasValue(ruleResourceName))
                return null;

            ruleFileLocation = ruleResourceName.substring(0, ruleResourceName.lastIndexOf("/") + 1);
            XMLGenerator  rulesGenerator  = new XMLPlainGenerator((Document)resourceRes);

            // if no rules-resource exist, return null
            if (rulesGenerator == null)
                return null;

            ruleScript = new StringBuffer();
            ruleScript.append("<Script language=\"JavaScript\"> var actionsToBeExecuted;").append(TagConstants.NL);
            callAllScript = new StringBuffer("function callAllRules(obj){ actionsToBeExecuted = new Array();").append(TagConstants.NL);

            // Generate Rule Java Script for the main rule file
            int lastIndex = createRuleScript(rulesGenerator, 0);

            // Now, check whether there are any includes.  If there are any includes, evaluate them.
            Debug.log (Debug.MAPPING_LIFECYCLE, "RuleGenerator: Evaluating includes");
            int numberOfIncludes = 0;
            if (rulesGenerator.exists( NODE_RULE_INCLUDES_CONTAINER ))
            {
                numberOfIncludes = rulesGenerator.getChildren(NODE_RULE_INCLUDES_CONTAINER).length;
            }

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log( Debug.MSG_STATUS, "RuleGenerator: Number of rule includes: " + numberOfIncludes);
            }

            // include rules from the includes files.
            for (int j = 0; j < numberOfIncludes; j++)
            {
                if (rulesGenerator.exists( NODE_RULE_INCLUDES_CONTAINER + NODE_SEP + j ))
                {
                    String fileName = rulesGenerator.getValue( NODE_RULE_INCLUDES_CONTAINER + NODE_SEP + j );
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    {
                        Debug.log( Debug.MSG_STATUS, "RuleGenerator: included file: " + fileName);
                    }

                    if (!(fileName.startsWith("/WEB-INF")))
                    {
                        fileName = ruleFileLocation + fileName;
                        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        {
                            Debug.log( Debug.MSG_STATUS, "RuleGenerator: Converted file path: " + fileName);
                        }

                    }

                    Object includedResource = ServletUtils.getLocalResource(servletContext, fileName, ServletConstants.XML_DOC_DATA, true);
                    XMLGenerator  includedRulesGenerator  = new XMLPlainGenerator((Document) includedResource);

                    // if no rules-resource exist, return null
                    if (includedRulesGenerator == null)
                        return null;

                    lastIndex = createRuleScript(includedRulesGenerator, lastIndex);
                }
            }

            callAllScript.append("for(var i = 0; i < actionsToBeExecuted.length; i++) eval ( actionsToBeExecuted[i] );} ").append(TagConstants.NL);
            ruleScript.append(callAllScript).append(TagConstants.NL);
            ruleScript.append("</Script>");
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log( Debug.MSG_STATUS, "RuleGenerator: Complete Script " + ruleScript);
            }

        return ruleScript.toString();
        }
        catch (Exception e)
        {
            Debug.error("RuleGenerator: " + e.getMessage() + "\n" + e.getLocalizedMessage() );
        }

         return null;
    }

    // Creates the Rule Script for the given rule-file, input as an XMLGenerator.
    public int createRuleScript(XMLGenerator rulesGenerator, int currentIndex)
    {
        try {
            int numberOfRules = rulesGenerator.getChildren(NODE_RULES_CONTAINER).length;
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log( Debug.MSG_STATUS, "RuleGenerator: Number of rules: " + numberOfRules);
            }

            int actionIndex = currentIndex;
            int ruleIndex = currentIndex;
            for (int i = 0; i < numberOfRules; i++, ruleIndex++)
            {
                String condition= "";
                String action = "";
                if (rulesGenerator.exists( NODE_RULES_CONTAINER + NODE_SEP + i + NODE_SEP + NODE_CONDITION ))
                {
                    condition = rulesGenerator.getValue( NODE_RULES_CONTAINER + NODE_SEP + i + NODE_SEP + NODE_CONDITION );
                    condition = updateConditionForJavaScript(condition);
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    {
                        Debug.log( Debug.MSG_STATUS, "RuleGenerator: Updated condition: " + condition);
                    }

                }

                if (rulesGenerator.exists( NODE_RULES_CONTAINER + NODE_SEP + i + NODE_SEP + NODE_ACTION ))
                {
                    action = rulesGenerator.getValue( NODE_RULES_CONTAINER + NODE_SEP + i + NODE_SEP + NODE_ACTION );
                    action = updateActionForJavaScript(action);
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    {
                        Debug.log( Debug.MSG_STATUS, "RuleGenerator: Updated action: " + action);
                    }

                }
                if(StringUtils.hasValue(action))
                {
                    boolean conditionExists= StringUtils.hasValue(condition);
                    ruleScript.append("function rule").append((ruleIndex + 1)).append("(obj){").append(TagConstants.NL);

                if(conditionExists)
                {
                    ruleScript.append("if ( ").append(condition).append(" )").append(TagConstants.NL);
                }

                    ruleScript.append("{ actionsToBeExecuted[").append(actionIndex).append("]=\"").append(action).append("\"; }").append(TagConstants.NL);
                    actionIndex++;

                    if(conditionExists)
                    {
                        ruleScript.append("else {  undo (\"").append(action).append("\"); }").append(TagConstants.NL);
                    }

                    ruleScript.append("}").append(TagConstants.NL);
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    {
                        Debug.log( Debug.MSG_STATUS, "RuleGenerator: Script created " + ruleScript);
                    }

                    callAllScript.append("rule").append((ruleIndex + 1)).append("(obj);\n");
                }
            }
             return ruleIndex;
        } catch (MessageException e) {
            Debug.error("RuleGenerator: " + e.getMessage() + "\n" + e.getLocalizedMessage() );
        }
        return -1;
    }

    // modify the rule-condition, in specific manner for a given rule
    public String updateConditionForJavaScript (String conditionStr)
    {
        int conditionIndex = conditionStr.indexOf("(");
        String condition = conditionStr.substring(0, conditionIndex);
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS, "RuleGenerator - updateConditionForJavaScript: Extracted Condition : " + condition);
        }
        return conditionStr;
    }

    // modify the rule-action, in specific manner for a given rule
    public String updateActionForJavaScript (String actionStr)
    {
        int actionIndex = actionStr.indexOf("(");
        String action = actionStr.substring(0, actionIndex);
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log( Debug.MSG_STATUS, "RuleGenerator - updateActionForJavaScript: Extracted Action : " + action);
        }

        if (action.startsWith("populateUsingQuery"))
        {
            String remainingStr = actionStr.substring(actionIndex + 1, actionStr.length());
            return (action + "( obj, " + remainingStr);
        }
         return actionStr;
    }
}
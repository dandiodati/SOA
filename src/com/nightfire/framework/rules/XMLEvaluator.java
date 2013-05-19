package com.nightfire.framework.rules;

import com.nightfire.framework.message.util.xml.XPathAccessor;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This provides a common interface for
* Evaluators that take XML input.
*/
public interface XMLEvaluator extends Evaluator{

    /**
    * This method allows an XML Evaluator to evaluator a parsed XML message.
    * This eliminates the need to parse an XML String multiple times
    * when it is being passed to several different Evaluators.
    *
    * @param parsedMessage An XPathAccessor containing the parsed input
    *                      XML.
    * @param errors Any errors that occur will be added to this collection.
    *
    * @return true if the <code>parsedMessage</code> evaluated successfully,
    *         false otherwise.
    *
    */
    public abstract boolean evaluate(XPathAccessor parsedMessage,
                                     ErrorCollection errors);


    /**
    * This method allows an XML Evaluator to evaluator a parsed XML message.
    * This eliminates the need to parse an XML String multiple times
    * when it is being passed to several different Evaluators.
    *
    * @param parsedMessage An XPathAccessor containing the parsed input
    *                      XML.
    * @param errors Any errors that occur will be added to this collection.
    * @param ruleContext  RuleContext object shared by the rules
    *
    * @return true if the <code>parsedMessage</code> evaluated successfully,
    *         false otherwise.
    *
    */
    public abstract boolean evaluate(XPathAccessor parsedMessage,
                                     ErrorCollection errors, RuleContext ruleContext);


    /**
     * Perform any necessary cleanup of resources, caches, etc.
     */
    public void cleanup();

}

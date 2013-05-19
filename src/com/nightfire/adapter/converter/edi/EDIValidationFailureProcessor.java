/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import java.util.*;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.util.*;
import org.w3c.dom.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.rules.*;
import com.nightfire.spi.common.driver.*;

/**
 * This class is responsible for extracting information about Rule Errors
 * which occured in validating XEDI messages, and marking the 
 * Transactions in which Rule Errors occured as bad transactions.
 *
 * To achieve this, the value of the Context Node in the Rule Error XML 
 * is extracted, preprocesed, and used as a dotted notation path for
 * accessing the proper node to change from a trans Node to a 
 * badTransaction Node. 
 * 
 * The context value for an XEDI Rule Error will follow this form:
 *
 * /root/envelope[1]/funcgroupContainer/funcgroup[2]/transContainer/trans[1]/REF
 *
 * The Context Node value is processed in several steps to make it a useable 
 * dotted notation path. 
 * 
 * First, it is truncated after root node portion from the front and before 
 * the trans node portion from the back. The previous example would look as
 * follows after this step:
 *
 * envelope[1]/funcgroupContainer/funcgroup[2]/transContainer/trans[1]
 * 
 * Second, the XPath markup (delimiters and index designaters) is replaced with 
 * NightFire standard markup of the same meaning. The previous example would look as
 * follows after this step:
 *
 * envelope(1).funcgroupContainer.funcgroup(2).transContainer.trans(1)
 * 
 * Third, The indexes are decremented by 1 each in order to convert the XPath
 * standard 1 indexed notation into NightFire standard 0 index notation. 
 * The previous example would look as follows after this step:
 *
 * envelope(0).funcgroupContainer.funcgroup(1).transContainer.trans(0)
 * 
 * Once the dotted notation path is created from the Context Value, the path
 * is used to change the name of the designated Node from trans to 
 * badTransaction. All children of the designated Node are retained.
 */
public class EDIValidationFailureProcessor extends MessageProcessorBase
{
    /**
     * The number of indices in the Context String
     * about which we care in marking Transactions
     * as bad. 
     * These consist of the Envelope index, the 
     * Functional Group index, and the Transaction Index.
     */
    public static final int RELEVANT_INDEX_COUNT = 3;

    /**
     * This is the String which designates the root Node in
     * the XPATH expression. Since our dotted notation assumes the 
     * root node, this must be removed for the Context to be 
     * used as a dotted notation path.
     *
     */
    public static final String XPATH_ROOT_STRING = "/root/";

    /**
     * This is the length of the XPath root String. This is used
     * in removing the XPath root String from the Context String. 
     *
     */
    public static final int XPATH_ROOT_LENGTH = XPATH_ROOT_STRING.length();

    /**
     * The Node name of the transContainer Node. Used in parsing and 
     * processing the Context String. 
     *
     */
    public static final String TRANSCONTAINER_STRING = "transContainer";

    /**
     * The Node name of the trans Node. Used in parsing and 
     * processing the Context String. 
     *
     */
    public static final String TRANS_STRING = "trans";

    /**
     * The default location from which to obtain the Rule Errors XML.
     *
     */
    public static final String DEFAULT_RULE_ERROR_LOCATION = "@context.EDIRuleErrors";

    /**
     * The propery from which to obtain the Rule Error location, if it 
     * is to be different from the default.
     *
     */
    public static final String RULE_ERROR_LOCATION_PROP = "RULE_ERROR_LOCATION"; 

    /**
     * The Node name of the container in which Rule Errors exists.
     *
     */
    public static final String ERROR_CONTAINER = ErrorCollection.CONTAINER;

    /**
     * The Node name of the Rules Error in the Rules Error XML.
     *
     */
    public static final String ERROR_NODE = ERROR_CONTAINER + "." + ErrorCollection.NODE;

    /**
     * The Name of the Context node from which the location of the 
     * Error can be extracted.
     *
     */
    public static final String ERROR_CONTEXT = ErrorCollection.CONTEXT;

    /**
     * Member variable containing the location from which to obtain the  
     * Rules Error XML.
     *
     */
    private String ruleErrorLocation = DEFAULT_RULE_ERROR_LOCATION;

    /**
     * Initializes this object.
     * Determine the location from which to obtain the Rules Error XML.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        super.initialize(key, type);
        String location = getPropertyValue(RULE_ERROR_LOCATION_PROP);
        if(null != location)
        {
            ruleErrorLocation = location;
        }

    }//initialize ( key, type )


    /**
     * Using the value of the Context Nodes in the Rule Error XML
     * as a designator for a specific Node in the Input message, 
     * Return a copy of the Input Message in an NVPair Array where 
     * all of the Nodes designated by Rule Error Context Nodes have
     * been replaced with badTransaction Nodes. 
     * 
     * NOTE: This implementation of Process assumes that the original 
     * Input Message which is to be populated with badTransaction
     * Nodes is the Input Object.
     * 
     * @param  input  MessageObject containing the value to be processed
     *
     * @param  mpcontext The context
     *
     * @return  Optional NVPair containing a Destination name and a MessageObject,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input )
    throws MessageException, ProcessingException
    {
        // Usual null round processing of Driver.
        // a null return value signals to the driver that this processor has
        // completed its work. As long as the Driver gets a non null value from
        // the execute method, it will continue to call the execute method.
        // This processor only returns one value, so if it gets no input, it is finished.
        if ( input == null )
        {
            return null;
        }

        String ruleErrors = null;
        try
        {
            ruleErrors = (String) get(ruleErrorLocation, mpcontext, input);
        }
        catch(MessageException e)
        {
            // do nothing. This is to catch the case where 
            // the rules errors are not present. 
            // there is no generic exists method on MessageProcessor
            // only the ones on the context and message object specifically.
        }
        Document ret = input.getDOM();
        if(null != ruleErrors)
        {
            // create a copy of the Input Message to work on.
            XMLMessageParser originalParser= new XMLMessageParser(ret);
            ret = originalParser.getDocumentClone();
            // replace all designated trans Nodes with badTransaction Nodes. 
            markBadTransactions(ruleErrors, ret);
        }

        return ( formatNVPair ( ret ) );
     }//process

    /**
     * Iterate over the Rule Errors and Use each one to mark
     * the associated trans node in the input XML as bad.
     *
     * @param ruleErrors DOM Containing the parsed Rules Errors.
     * @param orginalEDIXML DOM Containing the input message.
     * @exception MessageException if an error occurs
     */
    private void markBadTransactions(String ruleErrors, Document originalEDIXML )
    throws MessageException
    {
        Debug.log(Debug.MSG_STATUS, "Marking transactions as bad based on Rule Errors.");
        ErrorCollection errors = ErrorCollection.toErrorCollection(ruleErrors);
        Iterator errorIterator = errors.getIterator();
        XMLMessageGenerator gen = new XMLMessageGenerator(originalEDIXML);
        while(errorIterator.hasNext())
        {
            RuleError error = (RuleError) errorIterator.next();
            markBadTrans(gen, error);
        }
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Post Proccessed XML EDI [" + gen.generate() + "]");
        }
    }

    /**
     * Extract the value of the Context Node, make it suitable 
     * for use as a dotted notation path, and use that path 
     * to replace the designated trans node with a badTransaction
     * node.
     *
     * @param ruleErrors DOM Containing the parsed Rules Errors.
     * @param orginalEDIXML DOM Containing the input message.
     * @param index Index designating which Rule Error to work with.
     * @exception MessageException if an error occurs
     */
    private void markBadTrans(XMLMessageGenerator gen, RuleError error)
    throws MessageException
    {
        Debug.log(Debug.MSG_STATUS, "Marking one transaction as bad.");

        String errorContext = error.getContext();

        String transactionPath = truncateContext(errorContext);

        transactionPath = replaceMarkup(transactionPath);

        transactionPath = zeroIndexPath(transactionPath);

        insertBadTransNode(gen, transactionPath);

        Debug.log(Debug.MSG_STATUS, "Completed marking one transaction as bad.");
    }

    /**
     * Remove the XPATH root designater, and all notation 
     * after the trans node.
     *
     * @param transactionContext String Context extracted from a 
     * Rules Error against an XEDI message.
     * @return The truncated String.
     */
    private String truncateContext(String transactionContext)
    {
        Debug.log(Debug.MSG_STATUS, "Truncating the Context String.");
        // this will be our marker for where the extraneous tail information
        // starts.
        int index = 0; 
        for(int i = 0 ; i < RELEVANT_INDEX_COUNT ; i++)
        {
            index = transactionContext.indexOf(']', (index + 1));
        }
        // found three indexes
        Debug.log(Debug.MSG_STATUS, "Completed Truncating the Context String.");
        // return the String with head and tail lopped off.
        return transactionContext.substring(XPATH_ROOT_LENGTH, index + 1);
    }

    /**
     * Replace the XPath markup (delimiters and index designaters) with 
     * NightFire standard markup of the same meaning. 
     *
     * @param xpath The XPath Expression on which to substitute markup.
     * @return The dotted notation path with NF standard markup.
     */
    private String replaceMarkup(String xpath)
    {
        Debug.log(Debug.MSG_STATUS, "Replacing Markup.");
        String dottedNotation = xpath;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Transaction Path before replacement [" + xpath + "]");
        }
        dottedNotation = dottedNotation.replace('/', '.');
        dottedNotation = dottedNotation.replace('[', '(');
        dottedNotation = dottedNotation.replace(']', ')');
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Transaction Path after replacement [" + 
                                        dottedNotation + "]");
        }        
        Debug.log(Debug.MSG_STATUS, "Completed Replacing Markup.");
        return dottedNotation;
    }

    /**
     * Decremented the indices by 1 each in order to convert the XPath
     * standard 1 indexed notation into NightFire standard 0 index notation. 
     * NOTE: This method assumes that everything in between parens is a numeric 
     * index. 
     * 
     * This class could, at some future date, be implemented to truncate the 
     * XPath to the trans node, then use the XPath API to retrieve the Transactio
     * Set Node and operate on that.
     *
     * @param path The dotted notation path with 1 based indices.
     * @return The dotted notation path with 0 based indices.
     */
    private String zeroIndexPath(String path)
    {
        Debug.log(Debug.MSG_STATUS, "Decrementing Indices.");
        // accumulator for the altered dotted notation path.
        StringBuffer newPath = new StringBuffer();
        // index to track the place in the original path.
        int index = 0;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Transaction Path before zero indexing [" + path + "]");
        }

        while(-1 != path.indexOf('(', index))
        {
            int numberStartIndex = path.indexOf('(', index) + 1;
            int numberEndIndex = path.indexOf(')', numberStartIndex);
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "Found index starting at [" + numberStartIndex + "]");
            }
            newPath.append(path.substring(index, numberStartIndex));
            String numberString = path.substring(numberStartIndex, numberEndIndex);
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "Index value [" + numberString + "]");
            }

            int newIndex;
            try
            {
                newIndex = Integer.parseInt(numberString) - 1;
            }
            catch(NumberFormatException e)
            {
                String message = "Failed to decrement the indices on the " + 
		    "path [" + path + "]. " + e.getMessage();
                Debug.error(message);
                return null;
            }
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "New Index value [" + newIndex + "]");
            }
            // add the decremented index and the paren to the path.
            newPath.append(newIndex);
            newPath.append(')');
            // stop counting with digit index, move the index up
            // to account for the index which was just added to the path. 
            index = numberEndIndex;
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Transaction Path after zero indexing [" + newPath + "]");
        }
        Debug.log(Debug.MSG_STATUS, "Completed Decrementing Indices.");
        return newPath.toString();
    }

    /**
     * Decremented the indices by 1 each in order to convert the XPath
     * standard 1 indexed notation into NightFire standard 0 index notation. 
     * NOTE: This method assumes that everything in between parens is a numeric 
     * index. 
     * 
     * This class could, at some future date, be implemented to truncate the 
     * XPath to the trans node, then use the XPath API to retrieve the Transactio
     * Set Node and operate on that.
     *
     * @param path The dotted notation path with 1 based indices.
     * @return The dotted notation path with 0 based indices.
    private String zeroIndexPath(String path)
    {
        Debug.log(Debug.MSG_STATUS, "Decrementing Indices.");
        // accumulator for the altered dotted notation path.
        StringBuffer newPath = new StringBuffer();
        // index to track the place in the original path.
        int index = 0;
        int length = path.length();

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Transaction Path before zero indexing [" + path + "]");
        }

        while(index < length)
        {
            char currentCharacter = path.charAt(index);
            // Start of an index?
            if(currentCharacter != '(')
            {
                // not dealing with an index, so just accumulate the character.
                newPath.append(currentCharacter);
            }
            else // we have an open paren which means we have the start
                 // of an index.
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log(Debug.MSG_STATUS, "Found index starting at [" + index + "]");
                }
                // accumulate the paren.
                newPath.append(currentCharacter);
                // start counting with digitIndex
                int digitIndex = index + 1;
                String numberString = "";
                currentCharacter = path.charAt(digitIndex);
                // accumulate digits till the closing paren in the number String.
                // until we see the end of the index paren.
                while(currentCharacter != ')')
                {
                    numberString += currentCharacter;
                    digitIndex ++;
                    currentCharacter = path.charAt(digitIndex);
                }
                
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log(Debug.MSG_STATUS, "Index value [" + numberString + "]");
                }
                // get a number from the accumulated number String and decrement it.
                int newIndex;
                try
                {
                    newIndex = Integer.parseInt(numberString) - 1;
                }
                catch(NumberFormatException e)
                {
                    String message = "Failed to decrement the indices on the " + 
			"path [" + path + "]. " + e.getMessage();
                    Debug.error(message);
                    return null;
                }
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log(Debug.MSG_STATUS, "New Index value [" + newIndex + "]");
                }
                // add the decremented index and the paren to the path.
                newPath.append(newIndex);
                newPath.append(currentCharacter);
                // stop counting with digit index, move the index up
                // to account for the index which was just added to the path. 
                index = digitIndex;
            }
            index ++;
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Transaction Path after zero indexing [" + newPath + "]");
        }
        Debug.log(Debug.MSG_STATUS, "Completed Decrementing Indices.");
        return newPath.toString();
    }
     */
    
    /**
     * Replace the designated trans node with a badTransaction Node,
     * retaining all children of the original node.
     *
     * @param gen Generator containing the trans Node to replace.
     * @param transactionPath Dotted notation path to the trans Node
     *                        which is to be replaced.
     * @exception MessageException if an error occurs
     */
    private void insertBadTransNode(XMLMessageGenerator gen, String transactionPath)
    throws MessageException
    {
        Debug.log(Debug.MSG_STATUS, "Inserting the badTransaction Node.");
        // extract the original trans Node, and its Parent.
        Node transNode = gen.getNode(transactionPath);
        Node parent = transNode.getParentNode();
        // create the new badTransaction Node.
        org.w3c.dom.Element badTransNode = gen.createElement(EDIMessageConstants.BAD_TRANSACTION_NODE); 

        // preserve all children of the original trans Node.
        NodeList nl = transNode.getChildNodes( );
        
        int numChildren = nl.getLength( );
           
        // copy all the children to the new badTransaction Node.
        for ( int Ix = 0;  Ix < numChildren;  Ix ++ )
        {
            // Copy the source node before insertion.
            Node childCopy = gen.copyNode( gen.getDocument(), nl.item( Ix ) );
            
            badTransNode.appendChild( childCopy );
        }

        // replace the trans Node with the new badTransaction Node. 
        parent.replaceChild(badTransNode, transNode);

        Debug.log(Debug.MSG_STATUS, "Completed Inserting the badTransaction Node.");
    }
}


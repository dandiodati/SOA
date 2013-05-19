/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.adapter.converter.*;
import com.nightfire.adapter.converter.util.*;
import org.w3c.dom.*;

/**
 * This class is reponsible for generating an EDI Message
 * From an XML Message which is valid XEDI.
 * Note: This class uses member variables to perform some of
 * its processing, so instances may not be safely used in more
 * than one thread. 
 * Instances of this class are very light weight
 * so a new instance should be used for each conversion performed.
 * This could be implented to use a event based approache with SAX
 * or a node walking approach. 
 */
public class XMLtoEDIConverter extends XMLToStringConverter{

    /**
     * Index value for a Grouped Element which is invalid. 
     * Used to designate that the current Element is not in a group.
     */
    public static final int INVALID_GROUP_START_INDEX = -1;

    /**
     * The number of Elements in an Element Pair.
     */
    public static final int ELEMENT_PAIR_SIZE = 2;

    /**
     * The number of Elements in an Element Triple.
     */
    public static final int ELEMENT_TRIPLE_SIZE = 3;

    /**
     * The length of the String which holds the index of a Data Element.
     */
    public static final int COUNTER_LENGTH = EDIMessageConstants.FIRST_ELEMENT_NODE_SUFFIX.length();

    /**
     * The String to emit between Segments. 
     */
    String segmentSeparator = null;

    /**
     * The String to emit between Data Elements. 
     */
    String elementSeparator = null;

    /**
     * The String to emit between Composite Elements. 
     */
    String compositeElementSeparator = null;

    /**
     * Count of the Functional Groups converted. 
     * For use in populating trailed data in the resulting EDI Message. 
     */
    int funcGroupCount = 0;

    /**
     * Count of the Transaction Sets converted. 
     * For use in populating trailed data in the resulting EDI Message. 
     */
    int transactionSetCount = 0;

    /**
     * Count of the Segments converted. 
     * For use in populating trailed data in the resulting EDI Message. 
     */
    int segmentCount = 0;

    /**
     * Specify the value to use for a Segment Separator in the generated EDI Message.
     *
     * @param sep The value to use for a Segment Separator in the generated EDI Message.
     */
    public void setSegmentSeparator(String sep)
    {
        segmentSeparator = sep;
    }

    /**
     * Specify the value to use for a Element Separator in the generated EDI Message.
     *
     * @param sep The value to use for a Element Separator in the generated EDI Message.
     */
    public void setElementSeparator(String sep)
    {
        elementSeparator = sep;
    }

    /**
     * Specify the value to use for a Composite Element Separator in the generated EDI Message.
     *
     * @param sep The value to use for a Composite Element Separator in the generated EDI Message.
     */
    public void setCompositeElementSeparator(String sep)
    {
        compositeElementSeparator = sep;
    }

    /**
     * Implementation of the ConvertToString method of XMLToStringConverter
     * NOTE This implementation only handles an XEDI message containing a 
     * single envelope (ISA to IEA).
     * @param input DOM containing the XEDI Message for conversion to EDI.
     * @return The resulting EDI Message.
     * @exception ConverterException if an error occurs
     */
    public String convertToString(Document input) throws ConverterException{

       StringBuffer result = new StringBuffer();
       convert( getParser(input), result );
       return result.toString();

    }

    /**
     * Convert the XEDI Message to an EDI String.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     */
    private void convert(XMLMessageParser input, StringBuffer result)
    {
        addEnvelope(input, result);
    }

    /**
     * Extract the envelope information from the XEDI and emit it to the EDI String.
     * Delegate the Functional Group information extraction.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     */
    private void addEnvelope(XMLMessageParser input, StringBuffer result)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding Envelope information and Fucntional Groups.");
        }        

        addElements(input, result, EDIMessageConstants.ENVELOPE_NODE + "." + 
                                   EDIMessageConstants.ISA);
        addFunctionalGroups(input, result);
        addElements(input, result, EDIMessageConstants.ENVELOPE_NODE + "." + 
                                   EDIMessageConstants.IEA);
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "EDI String: \n" + result);
        }        
    }

    /**
     * Iterate over the Functional Groups to extract the information
     * from them to emit to the EDI Message.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     */
    private void addFunctionalGroups(XMLMessageParser input, StringBuffer result)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding Functional Groups.");
        }        

        String accessPath = EDIMessageConstants.ENVELOPE_NODE + "." + 
                            EDIMessageConstants.FUNC_GROUP_CONTAINER_NODE;
        try
        {
            funcGroupCount = input.getChildCount(accessPath);
            for(int i = 0 ; i < funcGroupCount ; i++)
            {
                addFunctionalGroup(input, result, accessPath + "." + i);
            }
        }
        catch(MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, "Failed in generating Functional Groups. " + 
                                        e.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding Functional Groups.");
        }        
    }

    /**
     * Extract the Functional Group information from the XEDI and emit it to the EDI String.
     * Delegate the Transaction Set information extraction.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     * @param accessPath The dotted notation XML path location from which to extract the 
     *                   information for this specific EDI structure.
     */
    private void addFunctionalGroup(XMLMessageParser input, StringBuffer result, String accessPath)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding One Functional Group.");
        }        

        addElements(input, result, accessPath + "." + EDIMessageConstants.GS);
        addTransactionSets(input, result, accessPath + "." + 
                           EDIMessageConstants.TRANSACTION_SET_CONTAINER_NODE_INDEX);
        addElements(input, result, accessPath + "." + EDIMessageConstants.GE);

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding One Functional Group.");
        }         
    }

    /**
     * Iterate over the Transaction Sets to extract the information
     * from them to emit to the EDI Message.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     * @param accessPath The dotted notation XML path location from which to extract the 
     *                   information for this specific EDI structure.
     */
    private void addTransactionSets(XMLMessageParser input, StringBuffer result, String accessPath)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding Transaction Sets.");
        }        

        try
        {
            transactionSetCount = input.getChildCount(accessPath);
            for(int i = 0 ; i < transactionSetCount ; i++)
            {
                addTransactionSet(input, result, accessPath + "." + i);
            }
        }
        catch(MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, "Failed in adding Transaction Sets. " + 
                                        e.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding Transaction Sets.");
        }        
    }

    /**
     * Extract the Transaction Set information from the XEDI and emit it to the EDI String.
     * Delegate the Segment information extraction.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     * @param accessPath The dotted notation XML path location from which to extract the 
     *                   information for this specific EDI structure.
     */
    private void addTransactionSet(XMLMessageParser input, StringBuffer result, String accessPath)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding one Transaction Set.");
        }        

        addSegments(input, result, accessPath);
        // reset the segment count for the next transaction set.
        segmentCount = 0;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding one Transaction Set.");
        }        
    }

    /**
     * Add all Segments in a Transaction Set.
     * Some Segments are in XEDI Loops.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     * @param accessPath The dotted notation XML path location from which to extract the 
     *                   information for this specific EDI structure.
     */
    private void addSegments(XMLMessageParser input, StringBuffer result, String accessPath)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding the Segments.");
            Debug.log(Debug.MSG_STATUS, "Access Path for adding segments [" + accessPath + "]");
        }        

        try
        {
            // iterate over the children. Children may be either Segments or Loops of Segments.
            int count = 0;
            count = input.getChildCount(accessPath);
            for(int i = 0 ; i < count ; i++)
            {
                Node node = input.getNode(accessPath + "." + i);
                // check to see if the child is a loop.
                String nodeName = node.getNodeName();
                if(nodeName.startsWith(EDIMessageConstants.LOOP_NODE_PREFIX))
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    {
                        Debug.log(Debug.MSG_STATUS, "Found a Loop.");
                    }        
                    // iterate over the Segments in the Loop.
                    int loopCount = input.getChildCount(accessPath + "." + i);
                    for(int j = 0 ; j < loopCount ; j++)
                    {
                        addSegments(input, result, accessPath + "." + i + "." + j);
                    }
                }
                else // not a Loop, so it is an individual Segment.
                {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log(Debug.MSG_STATUS, "Found an individual Segment.");
                }        
		  addSegment(input, result, accessPath + "." + i);                  
                }
            }
        }
        catch(MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, "Failed in generating Segments. " + 
                                        e.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding the Segments.");
        }        
    }

    /**
     * Add a single Segment. 
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     * @param accessPath The dotted notation XML path location from which to extract the 
     *                   information for this specific EDI structure.
     */
    private void addSegment(XMLMessageParser input, StringBuffer result, String accessPath)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding one Segment.");
        }        

        // increment the segment count.
        segmentCount ++;
        addElements(input, result, accessPath); 

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding one Segment.");
        }        
    }

    /**
     * Iterate over the Data Elements. Extract the information from them
     * and emit it to the EDI Message. 
     * Delegate extraction of Composite and Grouped Element information.
     *
     * @param input a <code>XMLMessageParser</code> value
     * @param result a <code>StringBuffer</code> value
     * @param accessPath a <code>String</code> value
     */
    private void addElements(XMLMessageParser input, StringBuffer result, String accessPath)
    { 
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding Data Elements.");
        }        

        try
        {
            Node node = input.getNode(accessPath);
            String nodeName = node.getNodeName();
            // the first Data Element of the Segment is always the name of the Segment.
            result.append(nodeName);
            // initialize to an invalid value so that we can tell if this is the first
            int groupedElementStartCount = INVALID_GROUP_START_INDEX;
            String lastElementStringIndex = EDIMessageConstants.FIRST_ELEMENT_NODE_SUFFIX;
            String currentElementStringIndex = EDIMessageConstants.FIRST_ELEMENT_NODE_SUFFIX;
            int currentElementIndex = 1;
            int lastElementIndex = 0;
            // Iterate through the Data Elements. 
            int elementCount = input.getChildCount(accessPath);
            for(int i = 0 ; i < elementCount ; i++)
            {
                // output element separators for all elements between the last numbered
                // one and this one.
                Node elementNode = input.getNode(accessPath + "." + i);
                String elementNodeName = elementNode.getNodeName();
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log(Debug.MSG_STATUS, "Encountered Element Node named [" + 
                                                elementNodeName + "]");
                }        

                // If we have encountered a set of Grouped Elements, determine the size of the 
                // group and delegate the extraction.
                if(elementNodeName.equals(EDIMessageConstants.ELEMENT_PAIR_CONTAINER_NODE) || 
                   elementNodeName.equals(EDIMessageConstants.ELEMENT_TRIPLE_CONTAINER_NODE))
                {
                    Debug.log(Debug.MSG_STATUS, "Found Grouped Elements.");
                    // all grouped elements have to have the same numbers scheme, so 
                    // we only provide a number for them if this is the first group.
                    if(INVALID_GROUP_START_INDEX  == groupedElementStartCount)
                    {
                        groupedElementStartCount = i;
                    }
                    int elementPairCount = input.getChildCount(accessPath + "." + i);
                    for(int j = 0 ; j < elementPairCount ; j++)
                    {
                        Node elemGroupNode = input.getNode(accessPath + "." + i + "." + j);
                        int elementGroupSize = 0;
                        // for performance, only determine what kind of group this is
                        // once. 
                        if(0 == elementGroupSize)
                        {
                            String elemGroupNodeName = elemGroupNode.getNodeName();
                            if(elemGroupNodeName.equals(EDIMessageConstants.ELEMENT_PAIR_NODE))
                            {
                                elementGroupSize = ELEMENT_PAIR_SIZE;
                            }
                            else
                            {
                                elementGroupSize = ELEMENT_TRIPLE_SIZE;
                            }
                            lastElementIndex += elementGroupSize;
                        }
                        // If this is the first group in the segment, pad before it.
                        if(0 == j)
                        {
                            addElementGroup(input, result, accessPath + "." + i + "." + j, 
                                            groupedElementStartCount, true);
                        }
                        // Otherwise, do not pad before it. 
                        else
                        {
                            addElementGroup(input, result, accessPath + "." + i + "." + j, 
                                            groupedElementStartCount, false);
                        }
                    }
                }
                else // we have found a single Element.
                {
                    Debug.log(Debug.MSG_STATUS, "Found a Single Element.");
                    // calculate the Index from the name, so that missing elements can be generated
                    int nameLength = elementNodeName.length();
                    currentElementStringIndex = elementNodeName.substring(nameLength - 
                                                                          COUNTER_LENGTH, nameLength);
                    try
                    {
                        currentElementIndex = Integer.parseInt(currentElementStringIndex);
                    }
                    catch(NumberFormatException e)
                    {
                        String message = "Badly formed XEDI. Element with expected count as " + 
                                         "the last two characters of the name lacks numbers. " + 
                                         "[" + currentElementStringIndex + "] in the current " + 
                                         "access path [" + accessPath + "]." + e.getMessage();
                        Debug.error(message);
                        // Converters are suposed to be best effort. 
                        // skip the rest of this Segment.
                        break;
                    }
                    // generate empty elements.
                    for(int j = lastElementIndex ; j < currentElementIndex ; j++)
                    {
                        result.append(elementSeparator);
                    }
                    lastElementIndex = currentElementIndex;

                    // output the element
                    // fill in the number of contained items in the first data element
                    // (func groups, transaction sets, or segments as appropriate
                    if(nodeName.equals(EDIMessageConstants.ISA) && 
                       EDIMessageConstants.COMPOSITE_ELEMENT_DELIMITER_DATA_ELEMENT_INDEX == i)
                    {
                        Debug.log(Debug.MSG_STATUS, "Element is ISA16 element");
                        result.append(compositeElementSeparator);
                    }
                    else if(nodeName.equals(EDIMessageConstants.IEA) && 
                            EDIMessageConstants.FUNC_GROUP_COUNT_DATA_ELEMENT_INDEX == i)
                    {
                        Debug.log(Debug.MSG_STATUS, "Element is IEA01 element");
                        result.append(funcGroupCount);
                    }
                    else if(nodeName.equals(EDIMessageConstants.GE) && 
                            EDIMessageConstants.TRANS_SET_COUNT_DATA_ELEMENT_INDEX == i)
                    {
                        Debug.log(Debug.MSG_STATUS, "Element is  GE01 element");
                        result.append(transactionSetCount);
                    }
                    else if(nodeName.equals(EDIMessageConstants.SE) && 
                            EDIMessageConstants.SEGMENT_COUNT_DATA_ELEMENT_INDEX == i)
                    {
                        Debug.log(Debug.MSG_STATUS, "Element is SE01 element");
                        result.append(segmentCount);
                    }
                    // composite element?
                    else if(elementNode.hasChildNodes())
                    {
                        Debug.log(Debug.MSG_STATUS, "Element is Composite Element");
                        addCompositeElements(input, result, accessPath + "." + i);
                    }
                    else
                    {
                        Debug.log(Debug.MSG_STATUS, "Element is data containing element");
                        result.append(input.getValue(accessPath + "." + i));
                    }
                }
            }                
            // append a Segment Separator if there were any Data Elements in this Segment.
            if(elementCount > 0)
            {
                result.append(segmentSeparator);
            }
        }
        catch(MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, e.toString());
        }
    }


    /**
     * Extract information from the Element Group and emit it to the EDI Message.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     * @param accessPath The dotted notation XML path location from which to extract the 
     *                   information for this specific EDI structure.
     * @param groupedElementStartCount The index of this Grouped Element Container
     *                                 within the Segment.
     * @param prePadSeparators Should we emit element separators before this group, becuase it 
     *                         is the first group in the container.
     */
    private void addElementGroup(XMLMessageParser input, StringBuffer result, String accessPath, 
                                 int groupedElementStartCount, boolean prePadSeparators)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding Grouped Elements.");
        }        

        int lastElementIndex = groupedElementStartCount;
        int currentElementIndex = 1;
        String lastElementStringIndex = EDIMessageConstants.FIRST_ELEMENT_NODE_SUFFIX;
        String currentElementStringIndex = EDIMessageConstants.FIRST_ELEMENT_NODE_SUFFIX;

        try
        {
            Node node = input.getNode(accessPath);
            int elementCount = input.getChildCount(accessPath);
            for(int i = 0 ; i < elementCount ; i++)
            {
                // output element separators for all elements between the last numbered
                // one and this one.
                Node elementNode = input.getNode(accessPath + "." + i);
                String elementNodeName = elementNode.getNodeName();
                // calculate the Index from the name, so that missing elements can be generated
                int nameLength = elementNodeName.length();
                currentElementStringIndex = elementNodeName.substring(nameLength - COUNTER_LENGTH, nameLength);
                try
                {
                    currentElementIndex = Integer.parseInt(currentElementStringIndex);
                }
                catch(NumberFormatException e)
                {
                    String message = "Badly formed XEDI. Element with expected count as " + 
                                     "the last two characters of the name lacks numbers. " + 
                                     "[" + currentElementStringIndex +  "] in the current " + 
                                         "access path [" + accessPath + "]." + e.toString();
                    Debug.error(message);
                    // Converters are suposed to be best effort. 
                    // skip the rest of this Element Group.
                    break;
                }
                // fill in separators before this element, in case there are missing
                // elements.
                if(prePadSeparators)
                {
                    for(int j = lastElementIndex ; j < currentElementIndex ; j++)
                    {
                        result.append(elementSeparator);
                    }
                }
                else
                {
                    result.append(elementSeparator);
                }
                lastElementIndex = currentElementIndex;

                // output the element
                if(elementNode.hasChildNodes())
                {
                    Debug.log(Debug.MSG_STATUS, "Element is Composite Element");
                    addCompositeElements(input, result, accessPath + "." + i);
                }
                else
                {
                    Debug.log(Debug.MSG_STATUS, "Element is data containing element");
                    result.append(input.getValue(accessPath + "." + i));
                }
            }
        }
        catch(MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, "Failed in adding Grouped Elements. " + e.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding Grouped Elements.");
        }        
    }


    /**
     * Extract information from the Composite Elements and emit it to the EDI Message.
     *
     * @param input XMLMessageParser containing the XEDI Message for conversion to EDI.
     * @param result The resulting EDI Message.
     * @param accessPath The dotted notation XML path location from which to extract the 
     *                   information for this specific EDI structure.
     */
    private void addCompositeElements(XMLMessageParser input, StringBuffer result, String accessPath)
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Adding Composite Elements.");
        }        

        int currentCompositeIndex = 1;
        int lastCompositeIndex = 1;
        String lastCompositeStringIndex = EDIMessageConstants.FIRST_ELEMENT_NODE_SUFFIX;
        String currentCompositeStringIndex = EDIMessageConstants.FIRST_ELEMENT_NODE_SUFFIX;

        try
        {
            Node node = input.getNode(accessPath);

            int compositeElementCount = input.getChildCount(accessPath);
            for(int i = 0 ; i < compositeElementCount ; i++)
            {
                Node compositeNode = input.getNode(accessPath + "." + i);
                String compositeNodeName = compositeNode.getNodeName();
                // calculate the Index from the name, so that missing elements can be generated
                int nameLength = compositeNodeName.length();
                currentCompositeStringIndex = compositeNodeName.substring(nameLength - COUNTER_LENGTH, nameLength);
                try
                {
                    currentCompositeIndex = Integer.parseInt(currentCompositeStringIndex);
                }
                catch(NumberFormatException e)
                {
                    String message = "Badly formed XEDI. Element with expected count as " + 
                                     "the last two characters of the name lacks numbers. " + 
                                     "[" + currentCompositeStringIndex + "] " + e.toString();
    	            Debug.error(message);
                    // Converters are suposed to be best effort.
                    // skip the rest of this Composite Element.
                    break;
                }
                // fill in separators for missing elements before this one.
                for(int j = lastCompositeIndex ; j < currentCompositeIndex ; j++)
                {
                    result.append(compositeElementSeparator);
                }
                lastCompositeIndex = currentCompositeIndex;

                // output the element
                result.append(input.getValue(accessPath + "." + i));
            }

        }
        catch(MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, "Failed in adding Composite Elements. " + e.getMessage());
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Completed adding Composite Elements.");
        }        
    }
} 






/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import com.nightfire.framework.util.*;

/**
 * This class is responsible for tracking and 
 * processing contextual information for the 
 * purpose of generating XEDI XML from Node
 * of an AST representing a parsed EDI message.
 *
 */
public class EDINodeVisitorContext 
{

    /**
     * Currently not in a group. 
     *
     */
    public static int GROUP_TYPE_NONE = 0;

    /**
     * Currently in a an Element Pair group
     *
     */
    public static int GROUP_TYPE_PAIR = 2;

    /**
     * currently in an Element Triple group.
     *
     */
    public static int GROUP_TYPE_TRIPLE = 3;

    /**
     * The current heirarchical path based on 
     * the input of previous nodes. 
     *
     */
    private String contextPath;

    /**
     * The dotted notation path where the XEDI XML Node 
     * for the current Node should be generated.
     *
     */
    private String outputPath;

    /**
     * The text from the Node. Will be added to the context 
     * path given to children of the current Node.
     *
     */
    private String nodeText;

    /**
     * The name of the current Data Element. 
     *
     */
    private String elementName;

    /**
     * The name of the current Segment.
     *
     */
    private String segmentName;

    /**
     * The mane of the current group element container. 
     *
     */
    private String groupElementContainerName;

    /**
     * Is the current element empty?
     *
     */
    private boolean isEmptyElement;

    /**
     * Is the current Element in a group?
     *
     */
    private boolean inGroup;

    /**
     * Does the Current node have children?
     *
     */
    private boolean nodeIsAParent;

    /**
     * The index of the last Element Node visited.
     *
     */
    private int lastElementIndex;

    /**
     * Count of the number of empty elements previously 
     * visited in the current Segment. 
     *
     */
    private int emptyElementCount;

    /**
     * Index of the last Composite Element visited
     * withing the current Element. 
     *
     */
    private int lastCompositeElementIndex;

    /**
     * Cound of the number of empty Composite Elemnts
     * visited within the current Element. 
     *
     */
    private int emptyCompositeElementCount;

    /**
     * The group type of the group in which the current
     * element is contained. 
     *
     */
    private int groupType;

    /**
     * The number of groups previously visited within the
     * current group container. 
     *
     */
    private int groupCount;

    /**
     * The count of the elements within the current element group.
     *
     */
    private int groupElementCount;

    /**
     * The number of empty elements previously visited within the
     * current group. 
     *
     */
    private int emptyGroupElementCount;

    /**
     * The position within the Segment at which the current set
     * of element groups begins. This dictates the indices of the 
     * elements in the resulting XEDI. 
     *
     */
    private int groupElementStartIndex;

    /**
     * The index of the group element container Node.
     * Since element groups which empty are not generated
     * the the XEDI, this index will be altered when 
     * used to generate the XEDI. 
     *
     */
    private int elementGroupContainerIndex;

    /**
     * Creates a new <code>EDINodeVisitorContext</code> instance.
     *
     */
    public EDINodeVisitorContext()
    {
        initialize();
    }

    /**
     * Set up initial values of data members so that the 
     * context instance reflects a state of having not 
     * visited any Nodes.
     *
     */
    public void initialize()
    {
        contextPath = null;
        outputPath = null;
        nodeText = null;
        elementName = null;
        segmentName = null;
        isEmptyElement = false;
        nodeIsAParent = false;
        inGroup = false;
        lastElementIndex = 0;
        emptyElementCount = 0;
        lastCompositeElementIndex = 0;
        emptyCompositeElementCount = 0;
        groupType = GROUP_TYPE_NONE;
        groupElementContainerName = null;
        groupCount = 0;
        groupElementCount = 0;
        emptyGroupElementCount = 0;
        groupElementStartIndex = 0;
        elementGroupContainerIndex = 0;
    }

    /**
     * Accessor for the context path.
     *
     * @param path The context Path.
     */
    public void setContextPath(String path) 
    {
        contextPath = path;
    }

    /**
     * Accessor for the context path.
     *
     * @return The context path.
     */
    public String  getContextPath() 
    {
        return contextPath;
    }

    /**
     * Accessor for the Node text. 
     *
     * @param text The Node text.
     */
    public void setNodeText(String text) 
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Setting nodeText to [" + text + "]");
        }
        nodeText = text;

        if(StringUtils.hasValue(nodeText))
        {
            isEmptyElement = false;
        }
        else
        {
            isEmptyElement = true;
        }
    }

    /**
     * Accessor for the Node text.
     *
     * @return The Node text.
     */
    public String getNodeText() 
    {
        return nodeText;
    }

    /**
     * Accessor for the Segment Name.
     *
     * The Segment name.
     */
    public String getSegmentName() 
    {
        return segmentName;
    }

    /**
     * Accessor for the output path.
     *
     * @return The output path.
     */
    public String getOutputPath() 
    {
        return outputPath;
    }

    /**
     * Set whither the Nodee have children.
     *
     * @param isAParent true if the Node has children. 
     */
    public void setIsNodeAParent(boolean isAParent)
    {
        nodeIsAParent = isAParent;
    }

    /**
     * Does the Node have children?
     *
     * @return true if the Node has chilren. 
     */
    public boolean isNodeAParent()
    {
        return nodeIsAParent;
    }

    /**
     * Is the current element to be generated to XEDI?
     *
     * @return true if the current element should be 
     *         generated to XEDI.
     */
    public boolean isElementToGenerate() 
    {
        return !isEmptyElement && !nodeIsAParent;
    }

    /**
     * Set the context instance to state as if it has
     * has not visited any nodes. 
     *
     */
    public void clear()
    {
        initialize();
    }

    /**
     * Perform the processing needed to prepare for generating XEDI from 
     * an Element Node.
     *
     */
    public void enterElement()
    {
        Debug.log(Debug.MSG_STATUS, "Entering an Element Node.");
        elementName = nodeText;
        // first find out if this is a group, and if so, set up
        // for group element processing. 
        determineGrouping();



        String adjustedPath = contextPath;
        // the two digit String to add onto the end of the Node 
        // name when generating the element XEDI Node.
        String indexSuffix = null;
        // the index in the Segment of the current data element.
        int elementIndex = 0;
        // the index of the XEDI which will be set in the event the 
        // element is not grouped.
//        int elementNodeIndex = 0;
        // XEDI node index, used set for either an element or a group element.
        int outputNodeIndex = 0;

        // in a group
        if(!inGroup)
        {
            if(isEmptyElement)
            {
                Debug.log(Debug.MSG_STATUS, "Element is empty.");
                                            emptyElementCount++;
            }
            else
            {
                // plus 1 for the first element which is the segment name
	        elementIndex = lastElementIndex + 1;


                // The XML node index needs to be adjusted down for previous empty 
                // elements and up for previous element groups.
                outputNodeIndex = elementIndex - emptyElementCount + groupCount - 1;

                indexSuffix = doubleDigitIndexString(elementIndex);
            }
        }
        else // in a group
        {
            // empty element
            if(isEmptyElement)
            {
                Debug.log(Debug.MSG_STATUS, "Group Element is empty.");
                                               emptyGroupElementCount++;
            }
	    else // not an empty element
	    {
                // the Node Index for groupElements doesn't have to be adjusted. 
                outputNodeIndex = groupElementCount;

                indexSuffix = doubleDigitIndexString(groupElementStartIndex + groupElementCount);

                if(0 < emptyElementCount)
                {
                   adjustedPath = adjustPathForEmptyElements(contextPath);
                }

                if (0 <  emptyGroupElementCount)
                {
                    Debug.log(Debug.MSG_STATUS, "There are empty group elements before this element");
                    adjustedPath = adjustPathForEmptyGroupElements(adjustedPath);
                }
            }
        }
      
        elementName = segmentName + indexSuffix;

        adjustedPath = adjustedPath + "." + elementName + "(" + outputNodeIndex + ")";


        // if the data element has children, it has composite elements. 
        if(nodeIsAParent)
        {
            Debug.log(Debug.MSG_STATUS, "Element has children so it must have composite elements.");
            // we need to alter the NodeText which will be added to the 
            // the contextPath for those children.
            nodeText = segmentName + indexSuffix;
        }
        
        Debug.log(Debug.MSG_STATUS, "last element index [" + lastElementIndex + "]");
        Debug.log(Debug.MSG_STATUS, "index to insert the value in the XEDI [" + 
                                    outputNodeIndex + "]");
        Debug.log(Debug.MSG_STATUS, "current count of empty elements in the segment [" + 
                                    emptyElementCount + "]");
        Debug.log(Debug.MSG_STATUS, "count of element groups in this segment [" + groupCount + "]");
        Debug.log(Debug.MSG_STATUS, "count of elements in this elem group [" + 
                                    groupElementCount + "]");
        Debug.log(Debug.MSG_STATUS, "first index for naming grouped elements [" + 
                                    groupElementStartIndex + "]");
        Debug.log(Debug.MSG_STATUS, "name to give the element node [" + elementName + "]");
        Debug.log(Debug.MSG_STATUS, "number to add to the element name [" + indexSuffix + "]");

        Debug.log(Debug.MSG_STATUS, "AdjustedPath [" + adjustedPath + "]");


        outputPath = adjustedPath;

        lastElementIndex ++;
        groupElementCount ++;
        emptyCompositeElementCount = 0;
        lastCompositeElementIndex = 0;                  
    }

    /**
     * Perform the processing needed to prepare for generating XEDI from 
     * an Element Group Node.
     *
     */
    public void enterElementGroup()
    {
        // if we weren't previously in a group, 
        // this is the first group in the container.
        if(false == inGroup)
        {
	    Debug.log(Debug.MSG_STATUS, "Entering the first group in a container.");
            groupElementStartIndex = lastElementIndex + 1;
            groupCount = 0;
            emptyGroupElementCount = 0;
            elementGroupContainerIndex = lastElementIndex;
        }
        inGroup = true;
        groupCount ++;
        groupElementCount = 0;
        groupType = GROUP_TYPE_NONE;
        emptyCompositeElementCount = 0;
        lastCompositeElementIndex = 0;

    }

    /**
     * Perform the processing needed to prepare for generating XEDI from 
     * an Composite Element Node.
     *
     */
    public void enterCompositeElement()
    {
        if(StringUtils.hasValue(nodeText))
        {
            // plus 1 for the first element which is the segment name
	    int compositeElementIndex = lastCompositeElementIndex + 1;
            // node index will be less due to not emitted empty elements.
            int compositeElementNodeIndex =  compositeElementIndex - emptyCompositeElementCount;

            String indexSuffix = doubleDigitIndexString(compositeElementIndex);

            String adjustedPath = adjustPathForEmptyElementsBeforeComposites(contextPath);

            outputPath = adjustedPath + "." + elementName + "_" + indexSuffix + "(" + 
                         compositeElementNodeIndex + ")";
        }
        else
        {
            emptyCompositeElementCount ++;
        }
        lastCompositeElementIndex ++;
    }

    /**
     * Perform the processing needed to prepare for generating XEDI from 
     * an Segment Node.
     */
    public void enterSegment()
    {
        segmentName = nodeText;
        elementName = null;
        isEmptyElement = false;
        inGroup = false;
        lastElementIndex = 0;
        emptyElementCount = 0;
        lastCompositeElementIndex = 0;
        emptyCompositeElementCount = 0;
        groupType = GROUP_TYPE_NONE;
        groupElementContainerName = null;
        groupCount = 0;
        groupElementCount = 0;
        emptyGroupElementCount = 0;
        groupElementStartIndex = 0;
        elementGroupContainerIndex = 0;
    }

    /**
     * Perform the processing needed to prepare for generating XEDI from 
     * an Bad Transaction  Node.
     *
     */
    public void enterBadTransaction()
    {
        // Do nothing. Currently entering a badTransaction node 
        // requires no book keeping because entering a Segment clears all
        // contextual information. 
    }

    /**
     * Describe <code>enterOtherNode</code> method here.
     *
     */
    public void enterOtherNode()
    {
        // Do nothing. Currently entering a node not delineated previously
        // requires no book keeping because entering a Segment clears all
        // contextual information. 
    }

    /**
     * Given a small integer (less than 100), 
     * return a String version of the integer
     * padded to 2 digits. 
     *
     * @param index The int to be padded.
     * @return The 2 digit String.
     */
    private String doubleDigitIndexString(int index)
    {
        if(index < 10)
        {
            return "0" + index;
        }
        return String.valueOf(index);        
    }

    /**
     * By examining the context path for the current Element,
     * Determine what sort of group the current Element is in, 
     * if any, and prepare for Element processing as appropriate.
     *
     */
    private void determineGrouping()
    {
        if(-1 != contextPath.indexOf(EDIMessageConstants.ELEMENT_PAIR_CONTAINER_NODE))
        {
            Debug.log(Debug.MSG_STATUS, "Element is in a group.");
            inGroup = true;
            groupType = GROUP_TYPE_PAIR;
            groupElementContainerName = EDIMessageConstants.ELEMENT_PAIR_CONTAINER_NODE;
        }
        else if(-1 != contextPath.indexOf(EDIMessageConstants.ELEMENT_TRIPLE_CONTAINER_NODE))
        {
            Debug.log(Debug.MSG_STATUS, "Element is in a group.");
            inGroup = true;
            groupType = GROUP_TYPE_TRIPLE;
            groupElementContainerName = EDIMessageConstants.ELEMENT_TRIPLE_CONTAINER_NODE;
        }
        else
        {
            Debug.log(Debug.MSG_STATUS, "Element is not in a group.");
            inGroup = false;
            groupCount = 0;
            groupType = GROUP_TYPE_NONE;
        }
    }

    /**
     * Adjust the path which will be used to generate XEDI 
     * based on the fact that empty Element nodes which 
     * exist in the AST are not generated to the XEDI.
     *
     * @param path The original path.
     * @return The adjusted path. 
     */
    private String adjustPathForEmptyElementsBeforeComposites(String path)
    {
        int newElementIndex = (lastElementIndex - emptyElementCount) - 1;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS,"lastElementIndex: " + lastElementIndex);
            Debug.log(Debug.MSG_STATUS,"newElementIndex: " +
                                          newElementIndex);
        }
        // must lower index of the elem path in case there were uninserted
        // absent optional data elements (emptyElements) before the coming composite elements..
        // plus one to the length of elementName for the paren.
        String adjustedPath = path.substring(0, path.indexOf( elementName + "(") + 
                                                elementName.length() + 1);
        adjustedPath = adjustedPath + newElementIndex + ")";    
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS,"adjustedPath: " + adjustedPath);
        }
        return adjustedPath;
    }

    /**
     * Adjust the path which will be used to generate XEDI 
     * based on the fact that empty Element nodes which 
     * exist in the AST are not generated to the XEDI.
     *
     * @param path The original path.
     * @return The adjusted path. 
     */
    private String adjustPathForEmptyElements(String path)
    {
        int newElementGroupContainerIndex = elementGroupContainerIndex - emptyElementCount;
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS,"elementGroupContainerIndex: " + elementGroupContainerIndex);
            Debug.log(Debug.MSG_STATUS,"elementGroupContainerName: " + groupElementContainerName);
            Debug.log(Debug.MSG_STATUS,"newElementGroupContainerIndex: " +
                                          newElementGroupContainerIndex);
        }
        // must lower index of the elemContainer path in case there were uninserted
        // absent optional data elements (emptyElements) before the grouped elements.
        // plus one to the length of groupElementContainerName for the paren.
        String adjustedPath = path.substring(0, path.indexOf( groupElementContainerName + "(") + 
                                                              (groupElementContainerName.length() + 1));
        adjustedPath = adjustedPath + newElementGroupContainerIndex; 
        adjustedPath = adjustedPath + path.substring(path.indexOf(")." + 
                      EDIMessageConstants.ELEMENT_PAIR_NODE + "("), 
        path.length());    
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS,"adjustedPath: " + adjustedPath);
        }
               return adjustedPath;
    }

    /**
     *  This method adjusts the elemPair index based on the number of 
     *  emptyGroupElements in the current elemPairContainer
     *  because Element Groups which exist in the AST but which are 
     *  empty, are not generated to the XEDI.
     *
     *  @param path - the path the be modified
     * 
     *  @return String - the modified path
     *                   or the original path if we encounter an error
     */
    private String adjustPathForEmptyGroupElements(String path) {
       
        String newPath = path;
        String tmp = path.substring(path.indexOf(EDIMessageConstants.ELEMENT_PAIR_NODE + "(") );
//        int openParen = tmp.indexOf('(');
        int closeParen = tmp.indexOf(')');
       
        //here we assume that the skipped nodes come in groups of size groupType, 
        // so for each set of skipped nodes we need to decrement the elemPair index by 1.
        // Also adjust for 0 indicing of the dotted notation.
	int newGroupCount = (groupCount - ( (emptyGroupElementCount)/groupType )) -1;
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Group type [" + groupType + "]");        
            Debug.log(Debug.MSG_STATUS, "Group count [" + groupCount + "]");        
            Debug.log(Debug.MSG_STATUS, "Empty group elements [" + emptyGroupElementCount + "]");        
            Debug.log(Debug.MSG_STATUS, "New group count [" + newGroupCount + "]");        
        }
        newPath = path.substring(0, path.indexOf(EDIMessageConstants.ELEMENT_PAIR_NODE + "("))
                + EDIMessageConstants.ELEMENT_PAIR_NODE + "(" + (newGroupCount) 
                + tmp.substring(closeParen); 
              
        return newPath;
     
    }
}







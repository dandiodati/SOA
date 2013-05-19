/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import antlr.collections.AST;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;

import com.nightfire.adapter.converter.*;
import com.nightfire.adapter.converter.util.XMLVisitor;

/**
 * This class is responsible for generating the XEDI XML
 * appropriate for the particular Node it is visiting.
 * The data necessary for perfoming this function is apportioned
 * into three areas. 
 *
 * The first area is the data from the 
 * Node itself.
 *
 * The second is the currentPath variable
 * given with the Node and which is a dotted notation
 * path String denoting where in the AST hierarchy the 
 * Node exists, as built from the additions provided by
 * this class each time it visits a Node. 
 * 
 * The third is the contextual information gathered from 
 * visits to previous Nodes. This information is required
 * for adding index indicators to the XEDI which
 * is generated.
 *
 * The majority of the processing of these three data
 * sources is defered to an instance of
 * EDINodeVisitorContext. 
 */
public class EDINodeVisitor extends XMLVisitor{

    /**
     * Smart contextual object which stores data
     * from previously visited Nodes and applies
     * that data to the data from the current Node
     * to provide the correct context specific 
     * information for generating XEDI for the
     * current Node.
     *
     */
    private EDINodeVisitorContext context = null;


    /**
     * Creates a new <code>EDINodeVisitor</code> instance.
     *
     * @param gen a <code>XMLMessageGenerator</code> value
     */
    public EDINodeVisitor(XMLMessageGenerator gen){

        super(gen);

        context = new EDINodeVisitorContext();
    }

    /**
     * Generate XEDI for the given Node. 
     * Applies contextual information gathered 
     * from previously visited Node. 
     *
     * @param node The current Node for which to generate 
     *             XEDI.
     * @param currentPath The aggregated path information
     *                    from previously visited Nodes.
     * @return The String which should be added to the currentPath
     *         given to any child Nodes of the current Node.
     */
    public String visit(AST node, String currentPath){

        String ret = null;
        // populate the context with necessary data.
        context.setContextPath(currentPath);
        context.setNodeText(node.getText());
        // check to see if the node has children
        if(null != node.getFirstChild())
        {
            context.setIsNodeAParent(true);
        }
        else
        {
            context.setIsNodeAParent(false);
        }

        try{

           switch(node.getType())
           {
               case EDITokenTypes.ELEM           : 
                                                 Debug.log(Debug.MSG_STATUS, "Found ELEM node.");
                                                 ret = elem(node, context);  
                                           
                                                 break;

      	       case EDITokenTypes.ELEMGROUP      : 
                                                 Debug.log(Debug.MSG_STATUS, "Found ELEMGROUP node.");
                                                 ret = elemGroup(node, context);
 
                                                 break;

	       case EDITokenTypes.COMPELEM       : 
                                                 Debug.log(Debug.MSG_STATUS, "Found COMPELEM node.");
                                                 ret = compositeElem(node, context);
                                             
                                                 break;

               case EDITokenTypes.SEGMENT        :  
                                                 Debug.log(Debug.MSG_STATUS, "Found SEGMENT node.");
                                                 ret = segment(node, context);
                                             
                                                 break;

  	       case EDITokenTypes.BAD_TRANSACTION : 
                                                  Debug.log(Debug.MSG_STATUS, 
                                                  "Found BAD_TRANSACTION node.");
                                                  ret = badTransaction(node, context);
                                            
                                                  break;

  	       default                            : 
                                                  Debug.log(Debug.MSG_STATUS, "Found general node.");
                                                  context.enterOtherNode();
                                                  ret = context.getNodeText();
                                                  break;

           }

      }
      catch(MessageException mex)
      {
         // do nothing. Converters must be fault tolerant and attempt to
         // convert a fully as possible, rather than abort. 
         Debug.log(Debug.ALL_ERRORS, "Could not convert value ["+ret+
                                     "] to XML location ["+currentPath+"] " + mex.getMessage());

      }
      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
      {    
          Debug.log(Debug.MSG_STATUS, "addition to path [" + ret + "]");    
      }
      return ret;

   }
   

    /**
     * Generate the XEDI as appropiate for this Element Node.
     *
     * @param node The current Node for which to generate 
     *             XEDI.
     * @param context The context object for recording and processing 
     *                information from previously visited Nodes.
     * @return The String which should be added to the currentPath
     *         given to any child Nodes of the current Node.
     * @exception MessageException if an error occurs
     */
    private String elem(AST node, EDINodeVisitorContext context)
   throws MessageException
   {
       context.enterElement();
       String currentPath = context.getContextPath();       
       String ret = context.getNodeText();

       // if the data element has children, it has composite elements. 
       if(context.isElementToGenerate())
       {
           Debug.log(Debug.MSG_STATUS, 
                     "No composite elements, setting value for element on generator."); 
           generator.setValue(context.getOutputPath(), ret);
       }

       return ret;
   }



    /**
     * Generate the XEDI as appropiate for this Element Group Node.
     *
     * @param node The current Node for which to generate 
     *             XEDI.
     * @param context The context object for recording and processing 
     *                information from previously visited Nodes.
     * @return The String which should be added to the currentPath
     *         given to any child Nodes of the current Node.
     * @exception MessageException if an error occurs
     */
    private  String elemGroup(AST node, EDINodeVisitorContext context)
    throws MessageException
    {
        context.enterElementGroup();
        String ret = context.getNodeText();
        return ret;
    }


    /**
     * Generate the XEDI as appropiate for this Composite Element Node.
     *
     * @param node The current Node for which to generate 
     *             XEDI.
     * @param context The context object for recording and processing 
     *                information from previously visited Nodes.
     * @return The String which should be added to the currentPath
     *         given to any child Nodes of the current Node.
     * @exception MessageException if an error occurs
     */
    private String compositeElem(AST node, EDINodeVisitorContext context)
    throws MessageException
    {
        context.enterCompositeElement();
        String currentPath = context.getContextPath();       
        String ret = context.getNodeText();

        if(context.isElementToGenerate())
        {
            generator.setValue(context.getOutputPath(), ret);
        }

       return ret;
    }


    /**
     * Generate the XEDI as appropiate for this Segment Node.
     *
     * @param node The current Node for which to generate 
     *             XEDI.
     * @param context The context object for recording and processing 
     *                information from previously visited Nodes.
     * @return The String which should be added to the currentPath
     *         given to any child Nodes of the current Node.
     * @exception MessageException if an error occurs
     */
    private String segment(AST node, EDINodeVisitorContext context)
   throws MessageException
   {
       context.enterSegment();
       String ret = context.getNodeText();

       return ret;

   }

    /**
     * Generate the XEDI as appropiate for this Bad Transaction Node.
     *
     * @param node The current Node for which to generate 
     *             XEDI.
     * @param context The context object for recording and processing 
     *                information from previously visited Nodes.
     * @return The String which should be added to the currentPath
     *         given to any child Nodes of the current Node.
     * @exception MessageException if an error occurs
     */
    private String badTransaction(AST node, EDINodeVisitorContext context)
   throws MessageException
   {
       context.enterBadTransaction();
       String currentPath = context.getContextPath();
       String ret = context.getNodeText();
       generator.setValue(currentPath + "." + ret, "");
       return ret;

   }









}







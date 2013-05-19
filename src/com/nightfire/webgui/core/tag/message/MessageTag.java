/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.framework.message.common.xml.*;

import com.nightfire.webgui.core.*;
import com.nightfire.framework.constants.PlatformConstants;

import java.util.ArrayList;
import java.util.Properties;
import javax.servlet.jsp.*;

import javax.servlet.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;

// third party imports
import org.w3c.dom.*;



/**
 * This is a root message tag which sets up information for inner tags
 * to build and display part of a message request or response.
 *
 */
public class MessageTag extends VariableBodyTagBase
{

    /**
     * Object which hold the current xml message data with its values.
     * Used to obtain the values of fields and determine number
     * of repeating structures.
     *
     */
    private XMLGenerator message;

    private boolean readOnly = false;


     /**
     * Set the message data object for this request.
     * @param message The XMLGenerator  object.
     * @throws JspException on error
     */
    public void setMessage(Object message) throws JspException
    {
       this.message = (XMLGenerator)  TagUtils.getDynamicValue("message",message, XMLGenerator.class,this,pageContext);
    }

    /**
     * indicates if this displayed message should be read only.
     * @param bool true makes it readonly, false makes it read write.
     * This is a tag attribute.
     * @throws JspException on error
     */
    public void setReadOnly(String bool) throws JspException
    {
       bool = (String) TagUtils.getDynamicValue("readOnly",bool, String.class,this,pageContext);

       readOnly = false;
       try {
          if ( StringUtils.hasValue(bool) )
             readOnly = StringUtils.getBoolean(bool);
       } catch (FrameworkException e) {
          log.warn("Not a valid value for ReadOnly attribute [" + bool + "],\n " + e.getMessage() + " defaulting to false.");
       }
    }

    /**
     * indicates if this message is readonly.
     * @return true if it is read only or false if it is read write.
     */
    public boolean isMsgReadOnly()
    {
       return readOnly;
    }



    /**
     * returns the xml message data associated with this
     * request or response.
     * @return XMLGenerator used to access the xml message.
     */
    public XMLGenerator getMessageData()
    {
       return message;
    }


    /**
     * Called when a new tag is encountered after the attributes have been
     * set.
     * Handles the adding and removing of xml nodes to the message, via
     * {@link com.nightfire.webgui.core.ServletConstants$NF_FIELD_ACTION_TAG} header field.
     * If the action is {@link com.nightfire.webgui.core.tag.TagConstants#ADD_NODE_ACTION_NAME} then node node specified in
     * {@link com.nightfire.webgui.core.tag.TagConstants#XML_PATH_TAG} is added to the current message.
     * else if the action is {@link com.nightfire.webgui.core.tag.TagConstants#DELETE_NODE_ACTION_NAME} then node node specified in
     * {@link com.nightfire.webgui.core.tag.TagConstants#XML_PATH_TAG} is deleted from the current message.
     * Html for the message is also generated here.
     *
     */
    public void doInitBody() throws JspException
    {

        StringBuffer buf = new StringBuffer();

        ServletContext servletContext = pageContext.getServletContext();
        //add BASIC_REPLICATE_DISPLAY to constants file
        String webAppName = (String)servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
        Properties initParameters=null;
		try {
			initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
		} catch (FrameworkException fe) {
			log.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
			throw new JspException("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
		}
        String basic = initParameters.getProperty("BASIC_REPLICATE_DISPLAY");

        boolean isReplicateDisplay = TagUtils.isReplicateDisplayTrue(basic);


        ServletRequest req = pageContext.getRequest();
        String action = req.getParameter(ServletConstants.NF_FIELD_ACTION_TAG);
        String node   = req.getParameter(TagConstants.XML_PATH_TAG);
        //String displayReplicate = "display";

        try {

           if (message == null ) {
              message = new XMLPlainGenerator(PlatformConstants.BODY_NODE);
              log.debug(" Creating an empty messageBody since a null one was passed in.");
           }


           if ( StringUtils.hasValue(action) ) {
              if ( action.equals(TagConstants.ADD_NODE_ACTION_NAME) ) {
                  if(isReplicateDisplay) {

                      callReplicateNodes(req);
                  }

                 else {
                     if (StringUtils.hasValue(node)) {
                         log.debug("Creating node [" + node + "]");
                         message.create(node);
                     }
                 }
              } else if ( action.equals(TagConstants.DELETE_NODE_ACTION_NAME) ) {
                 if (StringUtils.hasValue(node) && message.exists(node) ) {
                    log.debug("Removing node [" + node + "]");
                    message.remove(node);
                 }
              }
           }


        } catch (MessageException e) {
           String err= "MessageTag: Failed to set create/delete node [" + node + "]," + e.getMessage();
           log.error(err);
           throw new JspTagException(err);
        }


        log.debug("performing doInitBody");
        try
        {
           buf.append("<table cellpadding=\"0\" cellspacing=\"0\" border =\"0\" width=\"100%\"><tr><td class=\"")
              .append(TagConstants.CLASS_LEFT_PADDING)
              .append("\">")
              .append("</td><td>").append(TagConstants.NL);

         bodyContent.println(buf.toString());

        }
        catch (Exception ex)
        {
            String err = "MessageTag: Failed to write start of tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspTagException(err);
        }


    }

    /**
     * If displayReplicat is not null && emptry then display thr input text field
     * for number of copies and the checkbox for replication
     *
     * @param req ServletRequest
     * @throws JspTagException on error
     */
    protected void callReplicateNodes(ServletRequest req) throws JspTagException
    {
        int nNodeCount =0;
        //This will be the new node which already has the incremented index
        String node   = req.getParameter(TagConstants.XML_PATH_TAG);

        //TODO do we add this to ServletCOnstants || TagConstants
        //This will be the actual node requested for replication
        String curNode = req.getParameter("NFH_XMLSrcPath");

        //curNode: Request.lsr_order.ls(0).servicedetailscontainer.servicedetails(0).NFH_XMLSrcPath
        //so needs to be parsed to truncate NFH_XMLSrcPath
        String parsedCurNode ="";
        if(curNode != null && !curNode.equals("")){
            if(curNode.indexOf("NFH_XMLSrcPath") != -1){
                int index = curNode.lastIndexOf("NFH_XMLSrcPath");
                parsedCurNode = curNode.substring(0, index-1);
            }
        }
        String sReplicate = req.getParameter("NFH_ReplicateData");
        String sNodeCount = req.getParameter("NFH_AddNodeCount");
        if(sNodeCount == null || sNodeCount.trim().equals(""))
            sNodeCount = "1";
        nNodeCount = Integer.parseInt(sNodeCount);


        try {
            if((Boolean.valueOf(sReplicate))){
                log.debug("Getting  node [" + parsedCurNode + "]");
                if (StringUtils.hasValue(parsedCurNode)) {
                    log.debug("Replicating node [" + parsedCurNode + "]"  + " for [" + nNodeCount + "] times");
                    replicateNode(parsedCurNode, nNodeCount);
                }
            }
            else {
                if(StringUtils.hasValue(node)){
                    log.debug("Creating node [" + node + "]");
                    //createNodes(node, nNodeCount);
                    createNodes(parsedCurNode, nNodeCount);
                }
            }

        }
        catch(MessageException e) {
            String err = "MessageTag: Failed to call replicat nodes: " + e.getMessage();
            log.error(err);
            log.error("",e);
            throw new JspTagException(err);

        }

    }


    /**
     * If displayReplicate is not emptry but the checkbox has not been checked then just copy the
     * empty nodes
     *
     * @param node String: The node to be copied
     * @param nNodeCount int: Number of times to be copied
     * @throws JspTagException on error
     */

    protected void createNodes(String node, int nNodeCount) throws JspTagException{
        try{
            Node curNode = message.getNode(node);

             Node parentNode = curNode.getParentNode();
             int count = XMLGeneratorUtils.getChildCount(
                            //parentNode, curNode.getNodeName());
                            parentNode, curNode.getNodeName());
             int lastNodeIndex = (count + nNodeCount)-1;
             //code review comments by Dan
             //This already creates the nodes between count && lastIndex
              String newNodePath = curNode.getNodeName() +
                    							"(" + lastNodeIndex + ")";
              curNode = message.create(parentNode, newNodePath);

        }//end try
        catch(MessageException e){
             String err = "Failed to create nodes: " + e.getMessage();
             log.error(err, e);
             throw new JspTagException(err);
         }
    }



    /**
     *  creates/repliactes nodes based on the String input "srcNodePath" argument. The
     * number of replicates will be based on the int input "numerOfCopies", finally returns
     * an arraylist with the newly created nodes i.e Node[]
     *
     * @param srcNodePath The path to the actual node that needs to be replicated
     * @param numberOfCopies The number of copies to be replicated
     * @return ArrayList The arraylist of newly replictaed nodes
     * @throws MessageException on error
     */
    protected  Node[] replicateNode(String srcNodePath, int numberOfCopies)
            throws MessageException
    {
        ArrayList newNodesList = new ArrayList();
        if(srcNodePath == null || srcNodePath.trim().equals(""))
            return (Node [])newNodesList.toArray(new Node[newNodesList.size()]);

        try{

            Node srcNode = message.getNode(srcNodePath);
            Node parentNode = srcNode.getParentNode();
            for(int i=0; i < numberOfCopies; i++)
            {
                int count = XMLGeneratorUtils.getChildCount(parentNode, srcNode.getNodeName()) ;
                //Code review comments by Dan
                String newNodePath = srcNode.getNodeName() + "(" +
                                        count + ")";
                Node newNode = message.create(parentNode, newNodePath);
                message.copyChildren(newNode, srcNode);
                // sending an empty arraylist, so that we can check
                // that if any node is repeating then to remove it
                // with in any sub section
                setIDForNode(newNode, new ArrayList<String>());
                newNodesList.add(newNode);
            }

        }
        catch(Exception e){
            String err = "Failed to replicate node: " + e.getMessage();
            log.error(err);
            log.error("",e);

        }

        //return newNodesList;
        return (Node [])newNodesList.toArray(new Node[newNodesList.size()]);

    }//end replicateNode


    /**
    * Iterates thru the node and sets the attribute named "id"
    * @param newNode Node
    * @throws JspTagException on error
    */
    protected void setIDForNode(Node newNode) throws JspTagException{
        setIDForNode(newNode, null);
    }

    /**
    * Iterates thru the node and sets the attribute named "id"
    * @param newNode Node
    * @param nodesList ArrayList object that holds the number of nodes that need to be removed.
    * @throws JspTagException on error
    */
    protected void setIDForNode(Node newNode, ArrayList<String> nodesList) throws JspTagException
    {
      if(newNode != null)
      {
          try {
              if (!newNode.hasChildNodes()) {
                  //set this only if the value attribute is present
                  //if (message.getAttribute(newNode, "value") != null)
                  //code review comments by Dan
                  if(message.isValueNode(newNode) )
                  {
                      String newNodeName = newNode.getNodeName();
                      String xPath = message.getXMLPath(newNode);
                      // seeking for (1) only, as we will remove if node repeats
                      // under same section/sub-section/repeating-sub-section
                      String ONE = "(1)";
                      String xPath2 = (xPath.endsWith(ONE)) ? xPath.substring(0, xPath.length()-ONE.length()) : xPath;

                      message.setAttribute(newNode, "id", xPath);

                      if (nodesList != null)
                      {
                          boolean remove = (nodesList.contains(xPath)) || (nodesList.contains(xPath2));

                          if (remove)
                          {
                              if (log.isDebugEnabled())
                              {
                                  log.debug ("Returning from this point after removing node with name [" + newNodeName
                                      + "] and with path [" + xPath + "] as node with this name already exists in this section.");
                              }
                              // if we found any such node which is already processed/present in this section
                              // then removing that node and consider the latest one.
                              message.remove(xPath);
                              return;
                          }

                          // adding node name in array list to check in next iteration
                          nodesList.add(xPath);
                          if (log.isDebugEnabled())
                          {
                              log.debug ("Node added with name [" + newNodeName
                                  + "] and with path [" + xPath + "] to check for more existance");
                          }
                      }
                  }
              } else {

                  NodeList childList = newNode.getChildNodes();
                  for (int i = 0; i < childList.getLength(); i++) {
                      Node node = childList.item(i);
                      setIDForNode(node, nodesList);
                  }
              }
          } catch (Exception e) {
              String err = " Failed to set ID for node: " + e.getMessage();
              log.error(err, e);
              throw new JspTagException(err);

          }
       }
    }

    /**
     * Called when body evaluation is complete
     * Finishes and html generation, and writes the html message content
     * to the current page or stores it into a variable.
     */
    public int doAfterBody() throws JspException
    {
        log.debug("performing doAfterBody");
        try
        {

           StringBuffer buf = new StringBuffer();

           buf.append("</td></tr></Table>").append(TagConstants.NL);
           bodyContent.println(buf.toString() );


           if (varExists() ) {
              String str = bodyContent.getString();
              setVarAttribute(str);
           } else {
              bodyContent.writeOut(this.getPreviousOut());
           }



        }
        catch (Exception ex)
        {
            String err = "MessageTag: Failed to write end of tag: " + ex.getMessage();
            log.error(err);
            log.error("",ex);
            throw new JspException(err);
        }

        return SKIP_BODY;

    }


    public void release()
    {
       super.release();

       message = null;
       readOnly = false;
    }



}

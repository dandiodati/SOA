/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.*;

import java.io.IOException;
import java.util.*;
import javax.servlet.http.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.*;

import org.w3c.dom.*;


import com.nightfire.framework.util.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.MessageException;



/**
 * Base class for all Form and Section Tags.
 * This class must exist within a {@link MessageTag} JSP tag.
 *
 */
public abstract class FormSectionBaseTag extends MessageContainerBaseTag
{

     /**
     * This custom field indicates field name in a repeatable section.
     * This is an optional field and if this custom value is present then
     * it's value should be used instead of "section name # index".
     */
    public static final String INDEXFIELD ="IndexField";

    /**
    * This custom field indicates file name of the GUI Rules file,
    * for the section. This is an optional field
    */
   public static final String RULES_FILE_NAME ="rules-file";


     /**
     * Creates add and delete links for repeating forms and sections
     * surrouned by table row tags ("<tr>...</tr>")
     * used by repeating MessagePart objects.
     *
     * @param buf     The string buffer to append html to.
     * @param jScriptSuffix The suffix used for any javascript functions.
     * @param addImage the image to use for the add button
     * @param delImage the image to use for the delete button
     *
     */
    protected void createAddDelete(StringBuffer buf,
                                 String jScriptSuffix, String addImage, String delImage)
        throws IOException, FrameworkException
    {
        String curName = curPart.getDisplayName();
        XMLGenerator message = getMessageData();
        String addPath,deletePath;
        // for deleting we want the exact path to the node including indexes
        deletePath = indexedCurXmlPath;

        // remove the last current index from the xml path
        // and so that the index can be changed
        // if we fail to get a new index then no node will be created
        int lastIndex = indexedCurXmlPath.lastIndexOf("(");
        addPath = indexedCurXmlPath.substring(0, lastIndex);
        addPath = addNewXmlPathIndex(getMessageData(), addPath);

        ServletContext servletContext = pageContext.getServletContext();
        //add BASIC_REPLICATE_DISPLAY to constants file
        String webAppName = (String)servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
        Properties initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
        String basic = initParameters.getProperty(ServletConstants.BASIC_REPLICATE_DISPLAY);

         //TEST ONLY MODIFY LATER, if this is empty then display normally else display textfield & checkbox
         //String displayReplicate = "display";
         String currentPath ="";
         String addCountParams = "";
         String replicateParams = "";
         String xmlSrcParams = "";
         String maxOccursParams = "";
         String currChildrenParams = "";
         String currentChildren= "";

         boolean isReplicateDisplay = TagUtils.isReplicateDisplayTrue(basic);

         if(isReplicateDisplay) {
             currentPath = indexedCurXmlPath;//For the addSection method
             addCountParams =  currentPath + "." + "NFH_AddNodeCount";
             replicateParams =  currentPath + "." + "NFH_ReplicateData";
             xmlSrcParams = currentPath + "." + "NFH_XMLSrcPath";
             maxOccursParams = currentPath + "." + "NFH_MAXOccurs";
             currChildrenParams = currentPath + "." + "NFH_CURRChildren";
             currentChildren= "";
             //to get the current existing children
             if(message != null){
                 if(message.exists(currentPath)) {

                     Node node = message.getNode(currentPath);
                     if (node != null) {
                         Node parentNode = node.getParentNode();
                         int childCnt = XMLGeneratorUtils.getChildCount(parentNode,
                                 node.getNodeName());
                         currentChildren = String.valueOf(childCnt);
                     }
                 }
             }
         }

            buf.append("<td class=\"")
            .append(TagConstants.CLASS_GOTO_ICONS);
            buf.append("\"><a href=\"javascript:add").append(jScriptSuffix).append("(&quot;");
            buf.append(addPath + "&quot;");
            if(isReplicateDisplay) {
                  addReplicateParams(buf, addCountParams, replicateParams,
                                     xmlSrcParams, maxOccursParams, currChildrenParams);
            }
            buf.append(")\" onMouseOut=\"window.status='';")
            .append("return true;\" onMouseOver=\"window.status='Add New ")
            .append(curName)
            .append("';return true;\"><img border=\"0\" src=\"")
            .append(contextPath + "/" +  addImage)
            .append("\" alt=\"Add New ")
            .append(curName)
            .append("\"/></a>");
            if(!isReplicateDisplay)
                buf.append("&nbsp");
            if(isReplicateDisplay) {
                addReplicateGUI(buf, currentPath, addCountParams, replicateParams,
                            xmlSrcParams, maxOccursParams, currChildrenParams, currentChildren);
            }
            buf.append("<a href=\"javascript:delete" + jScriptSuffix+"(&quot;")
            .append(deletePath)
            .append("&quot;)\" onMouseOut=\"window.status='';return true;")
            .append("\" onMouseOver=\"window.status='Delete This ")
            .append(curName)
            .append("';return true;\"><img border=\"0\" src=\"")
            .append(contextPath + "/" + delImage )
            .append("\" alt=\"Delete This ")
            .append(curName)
            .append("\"/></a></td>");

    }

    /**
     * If displayReplicate is true then call the addSectionForBasic method
     * which expects the path for node counts, checkbox etc
     *
     * @param buf StringBuffer
     * @param addCountParams String
     * @param replicateParams String
     * @param xmlSrcParams String
     * @param maxOccursParams String
     * @param currChildrenParams String
     */
    protected void addReplicateParams(StringBuffer buf, String addCountParams, String replicateParams,
                                      String xmlSrcParams, String maxOccursParams, String currChildrenParams){

        buf.append(",")
        .append("&quot;" + addCountParams +"&quot;" )
        .append(",")
        .append("&quot;" + replicateParams + "&quot;" )
        .append(",")
        .append("&quot;" + xmlSrcParams + "&quot;")
        .append(",")
        .append("&quot;" + maxOccursParams + "&quot;")
        .append(",")
        .append("&quot;" + currChildrenParams + "&quot;");


    }//end createReplicate

    /**
     * If displayReplicate is true then display the input text field for copies
     * and the checkbox for replication
     *
     * @param buf StringBuffer
     * @param currentPath String
     * @param addCountParams String
     * @param replicateParams String
     * @param xmlSrcParams String: hidden
     * @param maxOccursParams String: hidden
     * @param currChildrenParams String: hidden
     * @param currentChildren String: hidden
     */
    protected void addReplicateGUI(StringBuffer buf, String currentPath, String addCountParams, String replicateParams,
                                        String xmlSrcParams, String maxOccursParams, String currChildrenParams, String currentChildren){
        String maxOccurs = curPart.getMaxOccurs();
        buf.append("</td>")

        //text field
        .append("<td class=\""+ TagConstants.CLASS_FIELD_LABEL_REPLICATE+ "\">")
        .append("Copies:")
        .append("<input type=\"")
        .append("text\" ")
        .append(" name=\"")
        .append(addCountParams + "\"")
        .append(" value=\"")
        .append("\" ")
        .append(" size=\"")
        .append("1\"")
        .append(" maxlength=\"")
        .append("5\" ")
        .append("onBlur=\"entryValidation(this, new ValidateInfo('")
        .append(addCountParams + "', 'DECIMAL', -1, '') );\" ")
        .append(">")
        .append("</td>")
        //end of textfield

        //check box
        .append("<td class=\""+ TagConstants.CLASS_FIELD_LABEL_REPLICATE+ "\">")
        .append("Replicate:")
        .append("<input type=\"")
        .append("checkbox\" ")
        .append(" name=\"")
        .append(replicateParams+"\"")
        .append(" value=\"")
        .append("\" ")
        .append(">")

        //xmlsrc hidden
        .append("<input type=\"")
        .append("hidden\" ")
        .append("name=\"")
        .append(currentPath + "." + "NFH_XMLSrcPath" + "\"")
        .append(" value=\"")
        .append(currentPath + "." + "NFH_XMLSrcPath")
        .append("\" " +">")
        //.append("</td>")

        //maxCounts hidden
        .append("<input type=\"")
        .append("hidden\" ")
        .append("name=\"")
        .append(currentPath + "." + "NFH_MAXOccurs" + "\"")
        .append(" value=\"")
        .append(maxOccurs)
        .append("\" " +">")

         //current children count
        .append("<input type=\"")
        .append("hidden\" ")
        .append("name=\"")
        .append(currentPath + "." + "NFH_CURRChildren" + "\"")
        .append(" value=\"")
        .append(currentChildren)
        .append("\" " +">")
        .append("</td>")

         //GOTO part
         .append("<td class=\"")
         .append(TagConstants.CLASS_GOTO_ICONS)
         .append(" \" >");

    }//end replicateGUI


    /**
     * Creates a goto drop down menu html surrouned by table cell tags ("<td>...</td>")
     * used by repeating MessagePart objects.
     *
     * @param buf The string buffer to place html code at
     * @param cssClass the html CSS class type to add
     * @param jScriptSuffix The suffix used for any javascript functions.
     */
    protected void createGotoMenu(StringBuffer buf,
                              String cssClass, String jScriptSuffix)
        throws IOException, FrameworkException
    {
        XMLGenerator message = getMessageData();

        /*  Release 3.8 has feature to show a field value instead of section name in goto menu.
            This fetuare is applicable on read-only meta files. 
            New custom value "IndexField" has been introduced and value of "IndexField" should be 
            field name. Custom values are optional fields so if "IndexField" is present, 
            get name of the field and display value of field instead of sectionindex.
        
        */
       
        String indexedFieldName = "";
        boolean showFieldValue = false;
        if(curPart instanceof Section )
        {
            indexedFieldName = ((Section)curPart).getCustomValue(INDEXFIELD);
            showFieldValue = StringUtils.hasValue(indexedFieldName) && isReadOnly();
        }

        buf.append("<td class=\"").append(cssClass)
            .append("\">Go To: <select name=\"")
            .append(curXmlPath)
            .append("\" onChange=\"goto" + jScriptSuffix+"(this)\">")
            .append(NL);


        int i = 0;

        // we need to remove the last index, so
        // that we can change it


        int lastIndex = indexedCurXmlPath.lastIndexOf("(");
        String curPath = indexedCurXmlPath.substring(0, lastIndex);

        String indexedPath = curPath + "(" + i + ")";

        do {

            if (i == xmlChildIndex || getCount() == 0)
                buf.append("<option selected=\"selected\" value=\"");
            else
                buf.append("<option value=\"");

            buf.append(i).append("\">");
            if(showFieldValue)
            {
                String indexFieldValue = "Unknown "+ (i+1);
                if(message.exists(indexedPath+"."+indexedFieldName))
                {
                    if(StringUtils.hasValue(message.getValue(indexedPath+"."+indexedFieldName)))
                    {
                        indexFieldValue = message.getValue(indexedPath+"."+indexedFieldName);
                    }
                }
 
                buf.append(indexFieldValue);
                 
            }
            else
            {        
                buf.append(curPart.getDisplayName()).append(" #").append(i+1);
            }
            buf.append("</option>").append(NL);

            indexedPath = curPath + "(" + (++i) + ")";

            boolean test =  message.exists(indexedPath);

        } while (message.exists(indexedPath) );

        buf.append("</select></td>");
    }

}

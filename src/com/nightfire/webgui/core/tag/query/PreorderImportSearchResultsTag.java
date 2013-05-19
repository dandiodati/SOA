 
/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.query;

import com.nightfire.webgui.core.tag.message.*;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;

import com.nightfire.webgui.core.*;
import com.nightfire.framework.constants.PlatformConstants;

import java.io.*;
import javax.servlet.jsp.*;

import javax.servlet.jsp.tagext.*;
import javax.servlet.http.*;
import javax.servlet.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.xrq.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;


import org.w3c.dom.*;

import java.util.*;



/**
 * Creates the status search results based on the preorder import query response.
 *
 */
public class PreorderImportSearchResultsTag extends VariableTagBase
{

    /**
     * Custom value for a field indicating if a search can be sorted by it.
     * Default is true.
     */
    public static final String SORTABLE ="sortable";
    public static final String ALIAS_KEY_PREFIX  = "LSRPreorder";

    /**
     * The current message part which needs to be set by a child class before
     * calling doStartTag
     */
    private MessageContainer curPart;

    /**
     * Holds primary key of the database table.
     */
    private String pKey = null;

    
    private String containerPath;

    private String contextPath;

    private boolean ignoreCase = true;
 
    public Map serviceGroupForAlias;

    /**
     * The aliasDescriptor which can be used for field value alias lookup.
     */
    private AliasDescriptor aliasDescriptor;


     /**
     * Object which hold the current xml message data with its values.
     * Used to obtain the values of fields and determine number
     * of repeating structures.
     *
     */
    private XMLGenerator message;

    private String  sortField;
    private String  sortDir;
    
    private String svcGroup;

    /**
     * The max control size
     */
    private int maxCtrlSize = com.nightfire.webgui.core.tag.message.BodyTag.DEFAULT_CTRL_SIZE;

    private String actionDecidingValue = "";
    


    /**
     * Ignore the case of results field data nodes. All field in a record are converted to
     * uppercase if true.
     * (default is true)
     */
    public void setIgnoreCase(String bool)
    {
       ignoreCase = true;

       try {
          if ( StringUtils.hasValue(bool) )
             ignoreCase = StringUtils.getBoolean(bool);
       } catch (FrameworkException e) {
          log.warn("Not a valid value for ignoreCase attribute [" + bool + "],\n " + e.getMessage() + " defaulting to true.");
       }
    }

     /**
     * Sets the current messageContainer for this form
     * @param msgCont The MessageContainer object.
     *
     */
    public void setMsgPart(Object msgCont)  throws JspException
    {
       curPart = (MessageContainer)TagUtils.getDynamicValue("msgPart",msgCont, MessageContainer.class,this,pageContext);
    }

     /**
     * Set the message data object for this request.
     * @param message The XMLGenerator  object.
     */
    public void setMessage(Object message) throws JspException
    {
       this.message = (XMLGenerator) TagUtils.getDynamicValue("message",message, XMLGenerator.class,this,pageContext);
    }


    /**
     * Specified the path to the container which holds each result record.
     * If not specified defaults to {@link com.nightfire.framework.xrq.XrqConstants#RECORD_CONTAINER_NODE}
     */
    public void setContainerPath(String path)
    {
       containerPath = path;
    }

    /**
     * Sets field that the results are currently sorted on.
     * @param String the sort field xmlPath
     *
     */
    public void setSortField(String sortField)  throws JspException
    {
       this.sortField = (String)TagUtils.getDynamicValue("sortField",sortField, String.class,this,pageContext);
    }


    /**
     * Sets direction that the results are currently sorted in.
     * @param String Possible values are DESC or ASC.
     *
     */
    public void setSortDir(String sortDir)  throws JspException
    {
       this.sortDir = (String)TagUtils.getDynamicValue("sortDir",sortDir, String.class,this,pageContext);
    }
     /**
     * Sets the service group for the service.
     * @param String the service group.
     *
     */
    public void setSvcGroup(String svcGroup)  throws JspException
    {
       this.svcGroup = (String)TagUtils.getDynamicValue("svcGroup",svcGroup, String.class,this,pageContext);
    }


    public int doStartTag() throws JspException
    {
		super.doStartTag();
        serviceGroupForAlias = new HashMap();
        serviceGroupForAlias.put("ASROrder","ASRPreorder");
        serviceGroupForAlias.put("LSROrder", "LSRPreorder");
        serviceGroupForAlias.put("LSROrder", "LSRPreorder");
      
        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);

        StringBuffer buf = new StringBuffer();


         HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
         contextPath = request.getContextPath();

        if (message == null) {
           String err = "PreorderImportSearchResultsTag: message attribute was null.";
           log.error(err);
           throw new JspTagException(err);
        }


        try {

        if (!StringUtils.hasValue(containerPath) )
           containerPath = XrqConstants.RECORD_CONTAINER_NODE;

		Node container = message.getNode(containerPath);

        // holds display fields listed by the meta file.
        // this is any fields that are not hidden.
        List fields = new ArrayList();

        // hold key fields listed in the meta file which is all fields
        List keyFields = new ArrayList();

        // separated the fields into key fields and display fields
        separateFields(curPart, keyFields, fields);
        
        setActionDecidingValue(fields);

        // create hidden field for key fields and primary keys
        //createHiddenKeyFields(buf, pKeys, keyFields);
		createHiddenKeyFields(buf,  keyFields);

        buf.append("<table class=\"" + TagConstants.CLASS_SEARCHRESULTS_TABLE +"\" WIDTH=\"100%\" CELLPADDING=\"0\" CELLSPACING=\"0\" border=\"0\">");

        // build column header
        buildSearchResultsHeader(buf,fields);

        //build result rows of records
        //buildSearchResultsRows(buf, container, pKeys,keyFields,fields, curPart);

		//build result rows of records
        buildSearchResultsRows(buf, container, keyFields, fields, curPart);
        
		 buf.append("</table>");
         
           if ( varExists() )
              setVarAttribute(buf.toString() );
           else
              pageContext.getOut().println(buf.toString());

        } catch (Exception e) {
           String err= "PreorderImportSearchResultsTag: Failed to create html : " + e.getMessage();
           log.error(err);
           log.error("",e);
           throw new JspTagException(err);
        }

        return SKIP_BODY;

    }

 
	/**
     * creates hidden fields for primary  keys and detail keys
     * so that javascript can set the values.
     */
     private void createHiddenKeyFields(StringBuffer buf, List keyFields)
     {
        // add hidden fields for key fields so that they can be set by javascript
        Iterator iter = keyFields.iterator();
        while (iter.hasNext() ) {
           Field f = (Field) iter.next();

           String name = f.getXMLPath();
           
           buf.append("<input type=\"hidden\" value=\"\" name=\"" + ServletConstants.NF_FIELD_PREFIX + f.getXMLPath() +"\"/>");
        }

     } 


    /**
     * separates out the fields under the specified message part
     * into key fields and display fields.
     * @param part The message container to get the fields from
     * @param keyFields The list to add key fields to.
     * @param fields The list to add display fields to.
     */
    private void separateFields(MessageContainer part, List keyFields, List fields)
    {

        Iterator iter = part.getChildren().iterator();
      
        // build a list of all key fields( fields that are sent to the detail)
        // and all fields displayed in the list
        
        while (iter.hasNext() ) {
           Field f = (Field) iter.next();

           keyFields.add(f);
           if (log.isDebugEnabled() )
               log.debug("Adding detail field [" + f.getID() + "]");
           

           if (!TagUtils.isHidden(f) ) {
             
               fields.add(f);
               if (log.isDebugEnabled() )
                   log.debug("Adding display field [" + f.getID() + "]");
           }
        }
    }

    /**
     * Builds the search resultsheader which displays the name of each column.
     * table.
     * @param buf the buffer to append the html to.
     * @param fields The display fields to add to the results header.
     *
     */
    private void buildSearchResultsHeader(StringBuffer buf, List fields) throws MessageException
    {

        buf.append("<tr>");
        buf.append("<td class=\"EMPTY\" VALIGN=\"CENTER\" ALIGN=\"CENTER\">&nbsp;</td>");

        // loop over all fields to be displayed and build the field results header
        Iterator iter = fields.iterator();

        while (iter.hasNext() ) {
           Field f = (Field) iter.next();

           if(displayField(f))
           {
               DataType type = f.getDataType();
               
               boolean sortable = true;
               boolean ascending = false;

               String sortStr = f.getCustomValue(SORTABLE);

               // if sortable custom value has a value then set it otherwise
               // default to true
               if (StringUtils.hasValue(sortStr) )
                  sortable = ServletUtils.getBoolean( sortStr, true );


               boolean curSortField = false;
               
               // if there is a value the get the boolean value of it.
               // else if it is a date the default to descending order
               // else default to ascending order           

               if(sortField != null && sortField.equals(f.getXMLPath() ) ) {
                   curSortField = true;
                   if(sortDir != null && sortDir.equals("DESC"))
                       ascending = false;
                   else
                       ascending = true;
               }
               
                if (log.isDebugEnabled() )
                       log.debug("Header display name "+f.getDisplayName());

               if(!sortable) {
                  buf.append( "<th class=\"").append(TagConstants.CLASS_NONSORTABLE).append("\">" );
                  buf.append(f.getDisplayName());
                  buf.append("</th>");
               }
               else {
                  buf.append( "<th class=\"").append(TagConstants.CLASS_SORTABLE).append("\">" );

                  buf.append("<A  onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('");

                  buf.append("Sort By ").append( TagUtils.escapeAttr(f.getDisplayName()));
                  buf.append(" ');\"");
                  buf.append(" href=\"javascript:reorderSearch('");
                  buf.append(convertCase(f.getXMLPath()));
                  buf.append("', '");
                  buf.append(!ascending);
                  buf.append("', '").append(f.getParent().getID()).append("');\">");
                  buf.append(f.getDisplayName());


                  if(curSortField) {
                      buf.append("&nbsp;<img border=\"0\" src=\"");
                      if(ascending)
                          buf.append(contextPath +"/"+ TagConstants.IMG_SORT_ASC);
                      else
                          buf.append(contextPath +"/"+ TagConstants.IMG_SORT_DESC);
                      buf.append("\"/>");
                  }

                  buf.append("</A>");              
               } 
            
               buf.append("</th>");
           }//if displayField returns true
        }

         buf.append("</tr>");


    }


    private String convertCase(String str)
    {
       if (ignoreCase)
          return str.toUpperCase();
       else
          return str;
    }

    /**
     * builds the rows of records returned in the query page.
     * @param buf the html buffer to add html to
     * @param container The container node which hold the records.
     * @param pKeys the primary keys that need to be added to
     * the view detail link.
     * @param keyFields Other key fields that need to be added
     * to the detail link so that they get passed on.
     * @param fields The display fields to add to each row.
     */
    private void buildSearchResultsRows(StringBuffer buf, Node container, List keyFields, List fields, MessageContainer curPart) throws MessageException
    {

        int count = message.getChildCount(container);

        // now loop over all the records returned in a request
        // build a results table row for each record
    
        for ( int i = 0; i < count; i++ ) {

            buf.append("<form name=\"preorderRow_").append(i).append("\" method=\"post\">");            
    
           Node record = null;
           buf.append("<tr ");
           if (i%2 == 1) 
               buf.append("class=\"oddRow\" ");

           buf.append("onMouseOver=\"highLightRow(this);\" onMouseOut=\"dehighLightRow(this);\">");

           if (message.exists(containerPath + "." + i) )
              record = message.getNode(containerPath + "." + i);
           else
              break;
            
    

            buf.append("<td ");
            buf.append("VALIGN=\"CENTER\" ALIGN=\"CENTER\">");
            buf.append("<A><IMG  SRC=\"" +contextPath +"/"+ TagConstants.IMG_VIEW_BUTTON + "\"     onClick=\"javascript:getPreorderDetailPage('preorderRow_").append( i );
            buf.append("');\" style=\"cursor:hand\" ></A>    </td>");

  

           // loop over each field that needs to be displayed
           // and add there values to the current row
           Iterator fieldIter = fields.iterator();

           while ( fieldIter.hasNext() ) {

               Field f = (Field)fieldIter.next();

                if(displayField(f))
                {
                   String name = convertCase(f.getXMLPath());
                   String value ="&nbsp;";
                   DataType dataType = f.getDataType();
                    
                   value = getValue(f, record, name);

                    // get an alias value for a field
                    // each alias has the field path as a ALIAS_KEY_PREFIX followed by the field name.
                     
                     String aliasKey = (String)serviceGroupForAlias.get(svcGroup) + "."+name;
                     if (log.isInfoEnabled())
                     {
                         log.info("PreorderImportSearchResultsTag: Getting aliased value for [" + value 
                                    + "] with key [" + aliasKey + "]..." );
                          
                     }


                   // if name of field  = serviceType and datatype = enumeration then display drop down
                   if(name.equalsIgnoreCase(actionDecidingValue) && dataType.getType() ==  DataType.TYPE_ENUMERATED)
                   {
                       
                        buf.append("<td ");
                        buf.append("VALIGN=\"CENTER\" ALIGN=\"CENTER\">");
                        buf.append("<font face=\"Arial,Helvetica\" size=\"1\">");
                        constructDropDown(buf, dataType);

                        buf.append("&nbsp;<a href=\"javascript:performAction('preorderRow_").append(i).append("')\"");
                        buf.append("onMouseOut=\"displayStatus('')\" onMouseOver=\"return displayStatus('");
                        buf.append("Perform Action')\">");
                        buf.append("<img style=\"vertical-align:-20%\" alt=\"Perform Action\" ");
                        buf.append(" border=\"0\"  src=\"").append("images/ETCButton.gif"/*TagConstants.IMG_ETC*/).append("\" />");
                        
                        buf.append("</font>");
                        buf.append("</td>");
                   }
                   else  
                   {
                        buf.append("<td ");

                        buf.append("VALIGN=\"CENTER\" ALIGN=\"CENTER\">");
                        buf.append("<font face=\"Arial,Helvetica\" size=\"1\">");
                        buf.append(aliasDescriptor.getAlias(pageContext.getRequest(), aliasKey, value, true));
                        buf.append("</font>");
                        buf.append("</td>");
                  
                   }
                     
                    String chooseAction = f.getCustomValue("chooseAction");
                
                    if( chooseAction!=null) 
                    {
                        if(!actionDecidingValue.equals("&nbsp;"))
                       {
                            buf.append("<input type=\"hidden\" name=\"").append(ServletConstants.NF_FIELD_PREFIX +name).append("\" value=\"");
                       
                            buf.append(actionDecidingValue);
                       
                            buf.append("\" >");
                       }
                    
                    }
                    else
                    {
                       if(!value.equals("&nbsp;"))
                       {
                       
                            buf.append("<input type=\"hidden\" name=\"").append(ServletConstants.NF_FIELD_PREFIX +name).append("\" value=\"");
                            buf.append(value);
                       
                            buf.append("\" >");
                       }

                    }


                }                
     
           }//end of while
            
            // loop over each hidden field that needs to be displayed
            // and add there values to the current row
            
            Iterator keyFieldIter = keyFields.iterator();

            while ( keyFieldIter.hasNext() ) {

                
                Field f = (Field)keyFieldIter.next();

                String name = convertCase(f.getXMLPath());
                String value ="&nbsp;";
                
               value = getValue(f, record, name);
               
            
 
                
                    if(!value.equals("&nbsp;"))
                    {
                        buf.append("<input type=\"hidden\" name=\"").append(ServletConstants.NF_FIELD_PREFIX +name).append("\" value=\"");
                       
                        buf.append(value);
                    
                        buf.append("\" >");
                    } 
              

           }


            buf.append("</tr>"); 

            buf.append("</form>") ;

        }//end of result row count

    }// end of buildSearchResultsRows()


    /**
     * Gets the value of a field from the message and looks up aliases.

     * @param xmlParentNode -The xml parent node, used for repeating access.
     * @param fieldPath - The path of the xml node relative to xmlParentPath
     *
     */
    private String getValue(Field field, Node xmlParentNode, String fieldPath)
    {

        String value = "&nbsp;";


        try {

           if ( message.exists(xmlParentNode, fieldPath)  ) {
              value = message.getValue(xmlParentNode, fieldPath);

           }
           else if ( StringUtils.hasValue(field.getCustomValue(com.nightfire.webgui.core.tag.message.BodyTag.DEFAULT_VAL) ) )
              value = field.getCustomValue(com.nightfire.webgui.core.tag.message.BodyTag.DEFAULT_VAL);
           

        } catch (FrameworkException e) {
          // did not find node return empty
          log.debug("Node [" + message.getXMLPath(xmlParentNode) + "." + fieldPath + "] does not exist.");
        }

        return value;

    }

    public void release()
    {
       super.release();
       containerPath = null;
       message = null;
       pKey = null;
       curPart = null;
    }

    private boolean displayField(Field f)
    {
        String name = convertCase(f.getXMLPath());
        
        
        if(f.getCustomValue("isAction")== null )
        {
            return true;
        }
        else if(name.equalsIgnoreCase(actionDecidingValue) && (f.getCustomValue("isAction")).equalsIgnoreCase("true") )
        {
            return true;
        }   
         
        return false;
    }

    private void setActionDecidingValue(List fields) throws MessageException
    {
        Node record = null;
        String value = "";     
        
        if (message.exists(containerPath + ".0") )
          record = message.getNode(containerPath + ".0");
        
      
        // loop over each field that needs to be displayed
        // and add there values to the current row
        Iterator fieldIter = fields.iterator();
        
        while ( fieldIter.hasNext() ) {

            Field f = (Field)fieldIter.next();
            String name = convertCase(f.getXMLPath());
            value ="&nbsp;";
               
            value = getValue(f, record, name);
            String chooseAction = f.getCustomValue("chooseAction");
            if( chooseAction!=null && chooseAction.equalsIgnoreCase("true"))
            {
                actionDecidingValue = value ;    
                break;
            }
                
        }
    
    }

    private void constructDropDown(StringBuffer buf, DataType dataType)
    {

        OptionSource optionSource = dataType.getOptionSource();
        String[] optionValues = null;
        String[] optionDisplayValues = null;
        
        if (optionSource != null)
        {
            optionValues        = optionSource.getOptionValues();

            optionDisplayValues = optionSource.getDisplayValues();
        }
                       

        buf.append("<Select name=\"Action\">");


        if (optionValues != null)
        {
            for (int optIdx = 0; optIdx < optionValues.length; optIdx++)
            {
                buf.append("<option value=\"");
                buf.append(optionValues[optIdx]);	
                buf.append("\">");
                
                // Set the display name if one exists.  Otherwise use the option value
                // as the display name.

                String displayValue = optionValues[optIdx];

                if ((optionDisplayValues != null) && (optionDisplayValues.length > 0))
                {
                    displayValue = optionDisplayValues[optIdx];
                }

                // Truncate the display name length if it's greater that the max-control
                // size.  Otherwise the field alignment will be out-of-whack.

                if (displayValue.length() > maxCtrlSize)
                {
                    displayValue = displayValue.substring(0, maxCtrlSize - 3) + "...";
                }

                buf.append(displayValue);

                buf.append("</option>");
            }
        }
        
        buf.append("</Select>");

    }
}

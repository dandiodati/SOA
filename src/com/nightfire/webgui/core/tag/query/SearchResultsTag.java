/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.query;

import org.w3c.dom.*;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.message.BodyTag;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.*;

import javax.servlet.jsp.*;
import javax.servlet.http.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.xrq.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;


import java.util.*;


/**
 * Creates the status search results based on the query response.
 *
 */
public class SearchResultsTag extends VariableTagBase
{

    /**
     * Detail field custom value to indicate this this field is a detail field
     */


    /**
     * Custom value for a field indicating if a search can be sorted by it.
     * Default is true.
     */
    public static final String SORTABLE ="sortable";

    /**
     * Custom value for a field indicating if field is Anchor and value of this
     * this field would be the Anchoring value
     */
     public static final String ANCHOR_CUSTOM_VALUE = "anchor";

    /**
     * Semi Colon character
     */
     public static final String SEMI_COLON_CHAR = ";";

    /**
     * Custom value to Show supplier name from database
     */
     public static final String SHOW_DB_SUPPLIER_NAME_CUSTOM_VALUE = "show-db-supplier-name";

    /**
     * Custom value to decide if Show supplier name from database or not
     * TODO: similarly RESTRICT_DB_SUPPLIER_NAME_FOR can be made.
     */
     public static final String SUPPORT_DB_SUPPLIER_NAME_FOR_CUSTOM_VALUE = "support-db-supplier-name-for";

    /**
     * The current message part which needs to be set by a child class before
     * calling doStartTag
     */
    private MessageContainer curPart;

    /**
     * Holds primary key of the database table.
     */
    private String pKey = null;

    /**
     * Holds list of actions allowed.
     */
    private String actions = null;

    /**
     * javascript function to get the selected Action out of the list of actions (Used only when the actions attribute is populated).
     */
    private String jsActionSelector = null;


    private String containerPath;

    private String contextPath;

    private boolean ignoreCase = true;
    /**
     * It allows to check whether order is bundled or not.
     */
    private boolean isBundled = true;


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

    private String [] showDBSuppFor;
    private boolean isDBSupplierNameSupported;

    /**
     * Sets field that whether the search results are for bundled or unbundled Orders.
     * @param bool String the sort field xmlPath
     * @throws JspException on error
     *
     */
    public void setIsBundled(String bool)  throws JspException
    {
       isBundled = true;
       try {
          if ( StringUtils.hasValue(bool) )
             isBundled = StringUtils.getBoolean(bool);
       } catch (FrameworkException e) {
          log.warn("Not a valid value for isBundled attribute [" + bool + "],\n " + e.getMessage() + " defaulting to true.");
       }
    }




    /**
     * Ignore the case of results field data nodes. All field in a record are converted to
     * uppercase if true.
     * (default is true)
     *
     * @param bool flag if to ignore case
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
     * @throws JspException on error
     */
    public void setMsgPart(Object msgCont)  throws JspException
    {
       curPart = (MessageContainer)TagUtils.getDynamicValue("msgPart",msgCont, MessageContainer.class,this,pageContext);
    }

     /**
     * Set the message data object for this request.
     * @param message The XMLGenerator object.
     * @throws JspException on error
     */
    public void setMessage(Object message) throws JspException
    {
       this.message = (XMLGenerator) TagUtils.getDynamicValue("message",message, XMLGenerator.class,this,pageContext);
    }


    /**
     * Sets the primary key for this tag.
     * This is the primary key of the database table used
     * to build the query.
     * @deprecated This setter should no longer be used. Primary keys should
     * be indicated in the meta file. This is left for backwards compatibility.
     *
     * @param pKey The primary key.
     * @throws JspException on error
     */
    public void setPrimaryKey(String pKey) throws JspException
    {
       this.pKey = (String)TagUtils.getDynamicValue("primaryKey", pKey, String.class,this,pageContext);
    }


    /**
     * Specified the path to the container which holds each result record.
     * If not specified defaults to {@link com.nightfire.framework.xrq.XrqConstants#RECORD_CONTAINER_NODE}
     *
     * @param path container path
     */
    public void setContainerPath(String path)
    {
       containerPath = path;
    }

     /**
     * Comma-separated list of actions to create dropdown.
     * if not specified, View button would displayed by default.
     * 
     * @param actions multiple actions as CSV
     */
    public void setActions(String actions)
    {
       this.actions = actions;
    }

     /**
     * JavaScript method to be called to set the selected action (used only when actions attribute is populated).
     * The action dropdown name would be passed as a parameter to this method.
     *
     * @param jsActionSelector java script action selector
     */
    public void setJsActionSelector(String jsActionSelector)
    {
       this.jsActionSelector = jsActionSelector;
    }


    /**
     * Sets field that the results are currently sorted on.
     * @param sortField String the sort field xmlPath
     * @throws JspException on error
     */
    public void setSortField(String sortField)  throws JspException
    {
       this.sortField = (String)TagUtils.getDynamicValue("sortField",sortField, String.class,this,pageContext);
    }


    /**
     * Sets direction that the results are currently sorted in.
     * @param sortDir String Possible values are DESC or ASC.
     * @throws JspException on error
     */
    public void setSortDir(String sortDir)  throws JspException
    {
       this.sortDir = (String)TagUtils.getDynamicValue("sortDir",sortDir, String.class,this,pageContext);
    }


    /**
     * start point
     * @return int
     * @throws JspException on error
     */
    public int doStartTag() throws JspException
    {
      super.doStartTag();

        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, PageContext.APPLICATION_SCOPE);

        StringBuffer buf = new StringBuffer();



         HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
         contextPath = request.getContextPath();

        if (message == null) {
           String err = "SearchResultsTag: message attribute was null.";
           log.error(err);
           throw new JspTagException(err);
        }


        try 
        {

            if (!StringUtils.hasValue(containerPath) )
               containerPath = XrqConstants.RECORD_CONTAINER_NODE;

            Node container = message.getNode(containerPath);


            // holds display fields listed by the meta file.
            // this is any fields that are not hidden.
            List<Field> fields = new ArrayList<Field>();

            // hold key fields listed in the meta file which is all fields
            List<Field> keyFields = new ArrayList<Field>();

            // get a set of the primary keys  names
            Set pKeys   = getPKeysSet(pKey);

            // separated the fields into key fields and display fields
            separateFields(curPart, keyFields, fields);

            // create hidden field for key fields and primary keys
            createHiddenKeyFields(buf, pKeys, keyFields);

            buf.append("<table class=\"" + TagConstants.CLASS_SEARCHRESULTS_TABLE +"\" WIDTH=\"100%\" CELLPADDING=\"0\" CELLSPACING=\"0\" border=\"0\">");

            boolean lockingSupported = lockingSupported();

            // build column header
            buildSearchResultsHeader(buf,fields, lockingSupported);

            //build result rows of records
            buildSearchResultsRows(buf, container, pKeys,keyFields,fields, curPart, lockingSupported);

            buf.append("</table>");

           if ( varExists() )
              setVarAttribute(buf.toString() );
           else
              pageContext.getOut().println(buf.toString());

        } catch (Exception e) {
           String err= "SearchResultsTag: Failed to create html : " + e.getMessage();
           log.error(err);
           log.error("",e);
           throw new JspTagException(err);
        }

        return SKIP_BODY;

    }


    /**
     * creates hidden fields for primary  keys and detail keys
     * so that javascript can set the values.
     *
     * @param buf string buffer to add the html in
     * @param pKeys primary keys
     * @param keyFields main fields to pass as param
     */
     private void createHiddenKeyFields(StringBuffer buf, Set pKeys, List keyFields)
     {

        // add hidden fields for the primary keys so that they can be set by javascript
         // only needed for the old setPrimaryKey setter method
         for (Object pKey : pKeys)
         {
             buf.append("<input type=\"hidden\" value=\"\" name=\"")
                 .append(ServletConstants.NF_FIELD_PREFIX)
                 .append(pKey)
                 .append("\"/>");
         }

        // add hidden fields for key fields so that they can be set by javascript
         for (Object keyField : keyFields)
         {
             Field f = (Field) keyField;

             String name = f.getXMLPath();
             // if the current key is not a primary key that add another hidden field.
             if (!pKeys.contains(name))
                 buf.append("<input type=\"hidden\" value=\"\" name=\"")
                    .append(ServletConstants.NF_FIELD_PREFIX)
                    .append(f.getXMLPath())
                    .append("\"/>");
         }

     }


    /**
     * Separates out the fields under the specified message part
     * into key fields and display fields.
     * Fields under FieldGroup will be displayed added individually.
     * 
     * @param part The message container to get the fields from
     * @param keyFields The list to add key fields to.
     * @param fields The list to add display fields to.
     */
    private void separateFields(MessageContainer part, List<Field> keyFields, List<Field> fields)
    {

        // build a list of all key fields( fields that are sent to the detail)
        // and all fields displayed in the list
        for (Object child : part.getChildren())
        {
            boolean isField = true;
            Object obj = child;
            Iterator fgChildIter = null;

            // checking if the coming object is of type FieldGroup
            if (obj instanceof FieldGroup)
                fgChildIter = ((FieldGroup) obj).getChildren().iterator();

            // iterating just once in case of field or children in case of FieldGroup
            while (isField || (fgChildIter != null && fgChildIter.hasNext()))
            {
                Field field = null;
                if (fgChildIter != null && fgChildIter.hasNext())
                {
                    field = (Field) fgChildIter.next();
                }
                else
                {
                    field = (Field) obj;
                }

                keyFields.add(field);
                if (log.isDebugEnabled())
                    log.debug("Adding detail field [" + field.getId() + "]");


                if (!TagUtils.isHidden(field))
                {
                    fields.add(field);
                    if (log.isDebugEnabled())
                        log.debug("Adding display field [" + field.getId() + "]");

                }
                isField = false;
            }
        }
    }

    /**
     * check if lock node exists
     * @return boolean
     */
    private boolean lockingSupported()
    {
        return (message.exists(containerPath + ".0." + ServletConstants.LOCKED_NODE));
    }

    /**
     * Builds the search resultsheader which displays the name of each column.
     * table.
     * @param buf the buffer to append the html to.
     * @param fields The display fields to add to the results header.
     * @param  lockingSupported  Indicates whether locking feature is enabled.
     * @throws MessageException on error
     */
    private void buildSearchResultsHeader(StringBuffer buf, List fields, boolean lockingSupported) throws MessageException
    {
        // Need to make checkbox header first and it must not be sortable.
        // Also, only one column is supported to be checkbox
        StringBuffer otherSB = new StringBuffer ();
        StringBuffer chkboxSB = new StringBuffer ();

        buf.append("<tr>");
        otherSB.append("<td class=\"EMPTY\" VALIGN=\"CENTER\" ALIGN=\"CENTER\">&nbsp;</td>");

        if (lockingSupported)
        {
            otherSB.append("<td class=\"EMPTY\" VALIGN=\"CENTER\" ALIGN=\"CENTER\">&nbsp;</td>");
        }

        // loop over all fields to be displayed and build the field results header
        for (Object field : fields)
        {
            Field f = (Field) field;
            DataType type = f.getDataType();

            boolean sortable = true;
            boolean ascending = false;

            String sortStr = f.getCustomValue(SORTABLE);

            // if sortable custom value has a value then set it otherwise
            // default to true
            if (StringUtils.hasValue(sortStr))
                sortable = ServletUtils.getBoolean(sortStr, true);


            boolean curSortField = false;

            // if there is a value the get the boolean value of it.
            // else if it is a date the default to descending order
            // else default to ascending order

            if (sortField != null && sortField.equals(f.getXMLPath()))
            {
                curSortField = true;
                ascending = (sortDir != null && sortDir.equals("ASC"));
            }

            if (!sortable)
            {
                if (f.getDataType().getType() == DataType.TYPE_CHECK_BOX)
                {
                    chkboxSB.append("<th></th>");
                }
                else
                {
                    otherSB.append("<th class=\"").append(TagConstants.CLASS_NONSORTABLE).append("\">");
                    otherSB.append(f.getDisplayName());
                    otherSB.append("</th>");
                }
            }
            else
            {
                otherSB.append("<th class=\"").append(TagConstants.CLASS_SORTABLE).append("\">");

                otherSB.append("<A  onMouseOut=\"return displayStatus('');\" onMouseOver=\"return displayStatus('");

                otherSB.append("Sort By ").append(TagUtils.escapeAttr(f.getDisplayName()));
                otherSB.append(" ');\"");
                otherSB.append(" href=\"javascript:reorderSearch('");
                otherSB.append(convertCase(f.getXMLPath()));
                otherSB.append("', '");
                otherSB.append(!ascending);
                otherSB.append("', '").append(f.getParent().getId()).append("');\">");
                otherSB.append(f.getDisplayName());


                if (curSortField)
                {
                    otherSB.append("&nbsp;<img border=\"0\" src=\"");
                    if (ascending)
                        otherSB.append(contextPath).append("/").append(TagConstants.IMG_SORT_ASC);
                    else
                        otherSB.append(contextPath).append("/").append(TagConstants.IMG_SORT_DESC);
                    otherSB.append("\"/>");
                }
                otherSB.append("</A></th>");
            }
        }

        otherSB.append("</tr>");

        buf.append (chkboxSB).append (otherSB);
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
     * @param curPart MessageContainer object
     * @param  lockingSupported  Indicates whether locking feature is enabled.
     * @throws MessageException on error
     */
    private void buildSearchResultsRows(StringBuffer buf, Node container, Set pKeys, List keyFields, List fields, MessageContainer curPart, boolean lockingSupported) throws MessageException
    {

        // Need to make checkbox item first and it must not be sortable.
        // Also, only one column is supported to be checkbox

        int count = message.getChildCount(container);

        // now loop over all the records returned in a request
        // build a results table row for each record
        for ( int i = 0; i < count; i++ )
        {
            StringBuffer otherSB = new StringBuffer ();
            StringBuffer chkboxSB = new StringBuffer ();
            HashMap<String, String> paramsHM = new HashMap<String, String>();

            Node record = null;
            buf.append("<tr ");
            if (i%2 == 1)
               buf.append("class=\"oddRow\" ");

            buf.append("onMouseOver=\"highLightRow(this);\" onMouseOut=\"dehighLightRow(this);\">");

            if (message.exists(containerPath + "." + i) )
              record = message.getNode(containerPath + "." + i);
            else
              break;


            // create the view link and add key fields to be passed to
            // the detail

            // Checks if the order is bundled or unbundled and processing it differently in each of these cases.
            // Checking checkbox condition only in case it is not a bundle.
            if ( isBundled)
            {
                otherSB.append("<td ");
                otherSB.append("VALIGN=\"CENTER\" ALIGN=\"CENTER\">");

                if(actions != null)
                {
                    String dropdownName= "SelectedAction_" + i;
                    buildActionsDropDown(actions,otherSB,dropdownName);
                    otherSB.append("<A><INPUT type=\"image\" name=\"Details\" style=\"vertical-align:-20%\" value=\"Details\" SRC=\"");
                    otherSB.append(contextPath);
                    otherSB.append("/").append(TagConstants.IMG_ETC).append("\" onClick=\"");
                    if (jsActionSelector != null)
                    {
                        otherSB.append("javascript:");
                        otherSB.append(jsActionSelector);
                        otherSB.append("(document.get_detail.");
                        otherSB.append(dropdownName);
                        otherSB.append(".value);");
                    }
                    otherSB.append("javascript:setNVFormFields(document.get_detail, '");
                }
                else{
                    otherSB.append("<A><INPUT type=\"image\" name=\"Details\" value=\"Details\" SRC=\"");
                    otherSB.append(contextPath);
                    otherSB.append("/" + TagConstants.IMG_VIEW_BUTTON + "\" onClick=\"javascript:setNVFormFieldsWSubmit(document.get_detail, '");
                }
                // now build the link which takes us to the detail

                // build part of detail link with primary keys
                // only needed for the old setPrimaryKey setter method
                buildPKeysLink(pKeys, otherSB, record);
                if (pKeys.size() > 0 )
                      otherSB.append("&");


                // build the rest of the link with other detail key fields
                Iterator keyIter = keyFields.iterator();
                while ( keyIter.hasNext() ) {
                  Field f = (Field)keyIter.next();
                  String name = f.getXMLPath();
                  // if the this key is one of the primary keys then skip it
                  if (pKeys.contains(name) )
                     continue;

                  String value = "";

                  if ( message.exists(record, convertCase(name) ) )
                  {
                      value = message.getValue(record,convertCase(name) );
                      value = encodeSpecialChar(value);
                  }

                    otherSB.append(ServletConstants.NF_FIELD_PREFIX).append(name).append("=").append(value);
                    paramsHM.put(name, value);
                  if (keyIter.hasNext() )
                     otherSB.append("&");
                }

                otherSB.append("', '").append( curPart.getId());
                otherSB.append("');\"></A></td>");
            }
            //unbundled order handling-
            // It calls function getServiceHistoryDetail() existing in SearchResults.jsp, this function forwards control to the unbundled-message-detail-redirect.jsp
            else
            {
                otherSB.append("<td VALIGN=\"CENTER\" ALIGN=\"CENTER\">");
                otherSB.append("<A><IMG  SRC=\"");
                otherSB.append(contextPath);
                otherSB.append("/").append(TagConstants.IMG_VIEW_BUTTON).append("\" onClick=\"javascript:getServiceHistoryDetail(");
                otherSB.append(i);
                otherSB.append(");\" style=\"cursor:hand\" ></A></td>");
            }


            // Next to the View button, we want to display the Lock or Unlock icon
            // to indicate whether this order has already been locked by some user.

            if (lockingSupported)
            {
                String lockStatus = message.getValue(record, ServletConstants.LOCKED_NODE);
                String lockImage  = TagConstants.IMG_UNLOCK;

                if (StringUtils.getBoolean(lockStatus, false))
                   lockImage = TagConstants.IMG_LOCK;

                otherSB.append("<td valign=\"center\" align=\"center\"><img src=\"");
                otherSB.append(contextPath);
                otherSB.append("/");
                otherSB.append(lockImage);
                otherSB.append("\" border=\"0\"/></td>");
            }

            // loop over each field that needs to be displayed
            // and add there values to the current row
            for (Object field : fields)
            {
                Field f = (Field) field;
                boolean isChkbox = f.getDataType().getType() == DataType.TYPE_CHECK_BOX;
                String name = convertCase(f.getXMLPath());

                String value = "&nbsp;";

                value = getValue(f, record, name);
                String anchorValue = f.getCustomValue(ANCHOR_CUSTOM_VALUE);    //get the anchor value

                if (isChkbox)
                    chkboxSB.append("<td VALIGN=\"CENTER\" ALIGN=\"CENTER\"><font face=\"Arial,Helvetica\" size=\"1\">");
                else
                    otherSB.append("<td VALIGN=\"CENTER\" ALIGN=\"CENTER\"><font face=\"Arial,Helvetica\" size=\"1\">");
                // If field is anchor then append the anchor value to the field
                if (StringUtils.hasValue(anchorValue))
                {
                    value = getValue(f, record, name);
                    if (!value.equals("&nbsp;"))
                    {
                        anchorValue = anchorValue.replaceAll("<value>", value);
                        if (isChkbox)
                            chkboxSB.append("<A href=\"").append(anchorValue).append(";\">").append(value).append("</A>");
                        else
                            otherSB.append("<A href=\"").append(anchorValue).append(";\">").append(value).append("</A>");
                    }
                }
                else if (isChkbox)
                {
                    String[] paramsToPass = StringUtils.getArray(f.getCustomValue(BodyTag.DEFAULT_VAL), SEMI_COLON_CHAR);
                    String chkBoxVal = "";
                    String temp = null;
                    int len = paramsToPass.length;

                    for (int j = 0; j < len; j++)
                    {
                        temp = paramsHM.get(paramsToPass[j]);

                        if (StringUtils.hasValue(temp))
                            chkBoxVal = chkBoxVal + temp;

                        chkBoxVal = chkBoxVal + SEMI_COLON_CHAR;
                    }

                    // remove ending semi colon
                    if (chkBoxVal.length() > 1)
                        chkBoxVal = chkBoxVal.substring(0, chkBoxVal.length() - 1);

                    chkboxSB.append(createCheckboxHtml(name, chkBoxVal));
                }
                else
                    otherSB.append(value);

                if (isChkbox) chkboxSB.append("</font></td>");
                else otherSB.append("</font></td>");

            }
            otherSB.append("</tr>");
            buf.append (chkboxSB).append (otherSB);
        }
    }

    /**
     * Construct a simple unchecked check box with specified value
     *
     * @param name of the checkbox
     * @param value String
     * @return html of a checkbox
     */
    private String createCheckboxHtml ( String name, String value )
    {
        return ("<input type=\"checkbox\" value=\"" + value + "\" name=\"" + name + "\" />\n");
    }

    /**
     * URL encode any special characters found in the input string.
     *
     * @param  value  The string which may contain special characters.
     * @return  New string with all special characters URL encoded.
     */
    private String encodeSpecialChar(String value)
    {
        String newValue = value;

        if (newValue.indexOf("%") != -1)
        {
            newValue = StringUtils.replaceSubstrings ( newValue, "%", "%25");
        }

        if (newValue.indexOf("&") != -1)
        {
            newValue = StringUtils.replaceSubstrings ( newValue, "&", "%26");
        }

        if (newValue.indexOf("'") != -1)
        {
            newValue = StringUtils.replaceSubstrings ( newValue, "'", "%27");
        }

        if (newValue.indexOf("\"") != -1)
        {
            newValue = StringUtils.replaceSubstrings ( newValue, "\"", "%22");
        }

        if (newValue.indexOf("=") != -1)
        {
            newValue = StringUtils.replaceSubstrings ( newValue, "=", "%3D");
        }

        return newValue;
    }

    /**
     * sets any primary keys with there values on the
     * buffer.
     *
     * only needed for the old setPrimaryKey setter method
     *
     * @param pKeys primary keys
     * @param buf The string buffer to add the html to.
     * @param record the current xml record node.
     * @return A set containing all the primary key names.
     * @throws MessageException on error
     */
    private void buildPKeysLink(Set pKeys, StringBuffer buf, Node record) throws MessageException
    {

       Iterator iter = pKeys.iterator();

       while (iter.hasNext() ) {

         String tok = (String)iter.next();

           // add the primary key
           buf.append(ServletConstants.NF_FIELD_PREFIX).append(tok).append("=").append(message.getValue(record, convertCase(tok)));

          if (iter.hasNext() )
             buf.append("&");
       }



    }

     /**
     * Builds a set of primary keys
     * only needed for the old setPrimaryKey setter method
     *
     * @param pKeyStr A string of primary keys separated by commas
     * @return A set containing all the primary key names.
     */
    private Set getPKeysSet(String pKeyStr)
    {

       Set<String> keys = new HashSet<String>();
       if ( StringUtils.hasValue(pKeyStr) ) {

          StringTokenizer toker = new StringTokenizer(pKeyStr,",");
          while (toker.hasMoreTokens() )
              keys.add( toker.nextToken().trim() );
       }


       return keys;

    }

     /**
     * Create dropdown with list of actions.
     *
     * @param actions comma-separated list of actions.
     * @param buf output stringbuffer.
     * @param dropDownName name of the dropdown.
     */
    private void buildActionsDropDown(String actions, StringBuffer buf, String dropDownName)
    {
        StringTokenizer toker = new StringTokenizer(actions,",");
        buf.append("<select style=\"font-size:9pt;vertical-align:5%\" name=\"").append(dropDownName).append("\" >");
        while (toker.hasMoreTokens() ) {
            String action=toker.nextToken().trim();
            buf.append("<option value=\"").append(action).append("\">").append(action).append("</option>");
        }
        buf.append("</select>");
    }
     /**
     * Gets the value of a field from the message and looks up aliases.
     * @param field Field obj
     * @param xmlParentNode The xml parent node, used for repeating access.
     * @param fieldPath The path of the xml node relative to xmlParentPath
     *
     * @return string
     */
    private String getValue(Field field, Node xmlParentNode, String fieldPath)
    {

        String value = "&nbsp;";


        try {

           if ( message.exists(xmlParentNode, fieldPath)  ) {
              value = message.getValue(xmlParentNode, fieldPath);

             // get an alias value for a field
             // each alias has the field path as a prefix followed by the field value.

             String aliasKey = field.getFullXMLPath();

             if (log.isInfoEnabled())
             {
                 log.info("SearchResultsTag: Getting aliased value for [" + value
                            + "] with key [" + aliasKey + "]..." );

             }

             value = processCustomValues (field, xmlParentNode, fieldPath, value);
             value = aliasDescriptor.getAlias(pageContext.getRequest(), aliasKey, value, true);
           }


        } catch (FrameworkException e) {
          // did not find node return empty
          log.debug("Node [" + message.getXMLPath(xmlParentNode) + "." + fieldPath + "] does not exist.");
        }

        return value;

    }

    /**
     * In this method we will process all the custom values for a field
     * if need to perform any processing while displaying the result.
     * 
     * @param field         Field obj
     * @param xmlParentNode The xml parent node, used for repeating access.
     * @param fieldPath     The path of the xml node relative to xmlParentPath
     * @param value         From the bean
     * @return value        After evaluating custom values
     */
    private String processCustomValues(Field field, Node xmlParentNode, String fieldPath, String value)
    {
        value = processDBSuppCV (field, value);
        value = processTZCV (field, value);
        return value;
    }


    /**
     * this method process the {@link BodyTag.USE_CLIENT_TIMEZONE} custom value
     * @param field Field
     * @param value bean value
     * @return string
     */
    private String processTZCV(Field field, String value)
    {
        boolean timezone = TagUtils.isDateConversionReq(field);
        if (timezone)
        {
            // Converting date-time to client's local time zone
            if (StringUtils.hasValue(value))
                value = TagUtils.convertTimeZone(pageContext, value, TagUtils.CONVERT_TO_CLIENT_TZ, field.getCustomValue(BodyTag.USE_CLIENT_TIMEZONE));
        }

        return value;
    }


    /**
     * this method process the {@link SHOW_DB_SUPPLIER_NAME_CUSTOM_VALUE} custom value
     * @param field Field
     * @param value bean value
     * @return string
     */
    private String processDBSuppCV(Field field, String value)
    {
        // the name of the supplier to be shown for few specific products
        // so one custom-value used to get allowable product names
        // then one custom-value is for which field it has to be displayed.
        if (showDBSuppFor == null)
        {
            if (StringUtils.hasValue(field.getCustomValue(SUPPORT_DB_SUPPLIER_NAME_FOR_CUSTOM_VALUE)))
            {
                showDBSuppFor = StringUtils.getArray ( field.getCustomValue(SUPPORT_DB_SUPPLIER_NAME_FOR_CUSTOM_VALUE), ",");
                isDBSupplierNameSupported = Arrays.asList(showDBSuppFor).contains(value);
            }
        }
        if (isDBSupplierNameSupported)
        {
            boolean showDBSuppName = StringUtils.getBoolean(field.getCustomValue(SHOW_DB_SUPPLIER_NAME_CUSTOM_VALUE), false);
            if (showDBSuppName)
                value = TagUtils.getSupplierDBValue (value, false);
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

}

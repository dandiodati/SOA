/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.message;


import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.util.TagFunctions;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.beans.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.locale.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.security.domain.DomainProperties;

import java.util.*;
import java.text.*;
import java.lang.Class;
import java.lang.String;
import java.lang.reflect.Method;
import javax.servlet.jsp.*;
import javax.servlet.*;

import org.w3c.dom.*;


/**
 * Creates the html field body elements and then calls tahe provided
 * HtmlElementLayout class to finish generating the html.
 *
 * Used with in a Section or Form tag.
 *
 */
public class BodyTag extends MsgInnerBaseTag
{
    /**
     * Maximum control size
     */
    public static final int MAX_CTRL_SIZE = 100;

    /**
     * default Maximum control size for SPID
     */
    public static final int MAX_SPID_CTRL_SIZE = 4;

    /**
     * Default control size
     */
    public static final int DEFAULT_CTRL_SIZE = 40;

    /**
     * Default number of lines in a text area
     */
    public static final int DEFAULT_CTRL_LINES = 3;

    /**
     * Defined a number-of-columns attribute that allows to define number of rows to be displayed
     * for a Text Area field.
     */
    public static final String NO_OF_COLUMNS_VALUE = "number-of-columns";

    /**
     * This is per field property and if this is specified and has value then
     * the message specified as the value of this custom property will
     * used in alert message.
     */
    public static final String VALIDATION_ERR_MSG_CUSTOM_VALUE = "validation-error-msg";

    private static final String NULL_STRING_VALUE = "null";

    /**
     * Default HTML form name.
     */
    private static final String DEFAULT_FORM_NAME = "message";


    private static final String NL = TagConstants.NL;

    /**
     * The HTMLElement that holds an empty line for to provide padding
     * for the HtmlLayout class.
     */
    public static final HtmlElement SKIP_LINE =
        new HtmlElement(1,true,false);



    /**
     * The HtmlElementLayout class that handles generated the final
     * html to layout the elements on the page.
     *
     * @see com.nightfire.webgui.core.tag.message.support.HtmlElementLayout
     */
    private HtmlElementLayout layout;

    public static final String COPY_ORDER_ACTION = "copy-order";

    /**
     * Defined a gui action, which will cause certain GUI fields to be un-editable.
     * The action corresponding to the GUI Edit-Order feature is "send-order".
     */
    public static final String EDIT_ORDER_ACTION = "send-order";

    /**
     * Defined a gui action, which will convert an order/transaction from one type to other.
     */
    public static final String CONVERT_ORDER_ACTION = "convert-order";

    /**
     * Defined an attribute in GUI XML which contains any html properties for a
     * field.
     *
     * This could be used at place where there is a requirement
     * to make a field readonly/disable etc. out of some fields
     *
     * The string that will be generated in the XML, which will describe the
     * html attributes of that field will be attached as it is in the field
     * generation script.
     *
     * Ex.
     * <......>
     * <PON value="ponValue" html-attributes="readonly='true'" />
     * <......>
     *
     * Now the field that will be generated will be like this
     * <input type="text" name="PON" value="ponValue" readonly='true' />
     */
    public static final String HTML_ATTRIBUTES_VALUE = "html-attributes";


    /**
     * Defined an "has-html-attributes" attribute that can be specified as a custom value on a Field object.
     * The field with this attribute will be checked to have a string
     * defining html attributes.
     */
    public static final String HAS_HTML_ATTRIBUTES_CUSTOM_VALUE = "has-html-attributes";

    /**
     * Define a validation for the multiple text boxes control. 
     */
    public static final String VALIDATE_RANGE_CUSTOM_VALUE = "validate-range";

    /**
     * Defined a gui action, which will cause certain GUI fields to be blanked out.
     */
    public static final String CREATE_TEMPLATE_FROM_ORDER_ACTION = "create-template";

    /**
     * Defined a default value that can be specified as a custom value on a field.
     */
    public static final String DEFAULT_VAL = "defaultValue";

    /**
     * Defined an editable attribute that can be specified as a custom value on a Field object.
     */
    public static final String EDITABLE_CUSTOM_VALUE = "editable";

    /**
     * Defined an editable attribute that can be specified as a custom value on a Field object.
     */
    public static final String EDITABLE_IF_NO_SD_CUSTOM_VALUE = "editable-if-no-subdomain";

    /**
     * Defined an query-criteria attribute that can be specified as a custom value on a Field object.
     */
    public static final String QUERY_CRITERIA_CUSTOM_VALUE = "query-criteria";
    public static final String QUERY_CRITERIA_TOKENS_CUSTOM_VALUE = "query-tokens";


    /**
     * Defined an query-method attribute that can be specified as a custom value on a Field object.
     */
    public static final String QUERY_METHOD_CUSTOM_VALUE = "query-method";

    /**
     * Defined an attribute that can be specified to select which timezone has to be displayed.
     */
    public static final String USE_CLIENT_TIMEZONE = "use-client-timezone";

    /**
     * Defined an auto populate attribute that can be specified as a custom value on a Field object to populate from header.
     */
    public static final String GET_HEADER_VALUE = "get-header-value";
    /**
    * Defined an auto populate attribute that can be specified as a custom value on a Field object.
    */
     public static final String GET_BODY_VALUE = "get-body-value";
    /**
     * Defined an on-change-event-template attribute that can be specified as a custom value on a Field object to populate from body.
     */
    public static final String ON_CHANGE_EVENT_TEMPLATE_CUSTOM_VALUE = "on-change-event-template";

    /**
     * Defined a blank-out-for-copy attribute that allows for a field to be blanked out when
     * creating a copy of an order/transaction.
     */
    public static final String BLANK_OUT_FOR_COPY_CUSTOM_VALUE = "blank-out-for-copy";

    /**
     * Defined a regenerate-for-copy attribute that allows for a field to be regenerate out when
     * creating a copy of an order/transaction.
     */
    public static final String REGENERATE_FOR_COPY_CUSTOM_VALUE = "regenerate-for-copy";

    /**
     * Defined a disable-for-copy attribute that allows for a field to be disabled when
     * creating a copy of an order/transaction.
     */
    public static final String DISABLE_FOR_COPY_CUSTOM_VALUE = "disable-for-copy";

    /**
     * Defined a disable-for-convert attribute that allows for a field to be disabled when
     * converting an order/transaction from one type to other.
     */
    public static final String DISABLE_FOR_CONVERT_CUSTOM_VALUE = "disable-for-convert";

    /**
     * Below are the attributes that are required in creation of auto generation
     * of request number.
     */
    public static final String PROHIBIT_IF_HOSTID_CUSTOM_VALUE = "prohibit-if-host-id";
    public static final String HOSTID_CUSTOM_VALUE = "populate-host-id";
    public static final String JULIAN_DATE_CUSTOM_VALUE = "populate-julian-date";
    public static final String SEQUENCE_VALUE_CUSTOM_VALUE = "populate-sequence-value";

    /**
     * Defined a "disable-for-actions" attribute that returns true for a field to be disabled when
     * given action(s) matched with present action.
     */
    public static final String DISABLE_FOR_ACTIONS_CUSTOM_VALUE = "disable-for-actions";


    /**
     * Defined a disable-for-send attribute that allows for a field to be disabled when
     * editing an order/transaction.
     */
    public static final String PROHIBIT_FOR_EDIT_CUSTOM_VALUE = "prohibit-for-edit";

    /**
     * Defined an auto-increment-field attribute that allows for a field to be hidden when
     * creating a supplement-order.
     * This is currently implemented for "TEXT" fields only.
     */
    public static final String AUTO_INCREMENT_FIELD_VALUE = "auto-increment-field";

    /**
     * Defined a gui-display-control-key attribute that allows for a field display to be
     * controlled - hidden or displayed.
     */
    public static final String GUI_DISPLAY_CONTROL_KEY_CUSTOM_VALUE = "gui-display-control-key";

    /**
     * Defined a number-of-rows attribute that allows to define number of rows to be displayed
     * for a Text Area field.
     */
    public static final String NO_OF_ROWS_VALUE = "number-of-rows";

    /**
     * Defined with default-for-auto-ver attribute which represents the default value of VER field for the ILEC
     * for a field that is disabled due to AUTOINCREMENTVERLSR.
     *This is currently implemented for "TEXT" fields only.
     */
    public static final String DEFAULT_FOR_AUTO_VER_VALUE = "default-for-auto-ver";

    /**
     * Defined a custom value attribute that allows a drop list to be multi selected.
     */
        public static final String MULTIPLE_SELECT= "multiple-select";

    /**
     * Defined a custom value attribute that enables tool tip.
     * Currently it is supported for drop down list & text box only.
     */
        public static final String IS_TOOLTIP_ENABLED= "tooltip-enabled";

    /**
     * Defined a custom value attribute that allows a drop list to be multi selected.
     */
        public static final String REMOVE_NF_PREFIX= "remove-nf-prefix";

    /**
     * Below two custom value defined to create tabular format for a field group.
     */
        public static final String TABULAR_LABELS_CUSTOM_VALUE = "tabular-labels";
        public static final String TABULAR_HEADERS_CUSTOM_VALUE = "tabular-headers";

    /**
     * Defined to identify the type of FieldGroup.
     */
        public static final String FIELD_GROUP_TYPE_CUSTOM_VALUE = "field-group-type";

    /**
     * Defined one of the value of {@link FIELD_GROUP_TYPE_CUSTOM_VALUE}.
     */
        public static final String FIELD_GROUP_TYPE_VALUE_MULTIPLE = "multiple";

    /**
     * Defined the separators to be used in case of multple controls creation.
     * This would be a CSV (Comma Separated Value)
     */
        public static final String MULTI_CONTROL_SEPARATORS_CUSTOM_VALUE = "mc-separators";

    /**
     * If the value of this custom-value is true then the border of
     * FieldGroup will be shown else not. (Default true)
     */
        public static final String SHOW_BORDER_CUSTOM_VALUE = "show-border";

    /**
     * Defined a custom value attribute that allows a drop list to be sorted.
     */
        public static final String SORTED_CUSTOM_VALUE= "sorted";

    /**
     * Defined the size of custom value attribute defined above.
     */
        public static final String MULTIPLE_SELECT_SIZE = "multiple-select-size";

    /**
     * Represents the custom value to create two radio buttons.
     */
        public static final String CREATE_TWO_RADIO_BUTTONS_CUSTOM_VALUE = "CREATE_TWO_RADIO_BUTTONS";

    /**
     * Represents the custom value to highlight the background of required field.
     */
        public static final String HIGHLIGHT_BACKGROUND_CUSTOM_VALUE = "highlight-background";

    /**
     * Represents the custom value to insert column break after a field.
     */
        public static final String COLUMN_BREAK_CUSTOM_VALUE = "column-break";

    /**
     * Nullable property for a field.
     */
        public static final String NULLABLE_CUSTOM_VALUE = "nullable";

    /**
         * Fields supporting isNull.
         */
        public static final String NULL_CLAUSE_FIELDS = "NULL_CLAUSE_FIELDS";

    /**
     * Represents the custom value to insert column break after a field id.
     */
        public static final String COLUMN_BREAK_FIELDID_CUSTOM_VALUE = "column-break-field-id";

    /**
     * Defined the default size of custom value attribute defined above.
     */
        public static final String DEFAULT_MULTIPLE_SELECT_SIZE= "2";

    /**
     * Meta Resource Name Header Value
     */
        public static final String META_RESOURCE_NAME= "metaResource";

    /**
     * Custom value which indicates that a field should always be required for all actions.
     * This allows a meta file to indicate the minimum set of required fields
     * for doing a save or any other action that must always be filled in.
     * This attribute only applies to fields
     * that have a usage of required.
     */
    public static final String ALWAYS_REQUIRED = "always-required";

    /**
     * SPID option source parameters
     */
    private static final String SPID_LOOKUP_WIN_TITLE = "lookup_window_title";
    private static final String SPID_LATEST_UPDATED_TIME_QUERY_CRITERA = "latest_updated_time_query_criteria";
    private static final String SPID_HEADING_COL_PREFIX = "heading_col_";
    private static final String SPID_SIZE_FILTER_TEXTBOX_COL_PREFIX = "size_filter_textbox_col_";
    private static final String SPID_IS_SEARCH_FIELD = "is_search_field";
    private static final String SPID_COL_ONE_IDX = "0";
    private static final String SPID_COL_TWO_IDX = "1";

  /**
   * Indicates that a required enumerated drop down menu
   * should show an empty value.
   * By default this is always true.
   * Valid values are true, false, yes, no, t, f, n, y.
   */
    public static final String SHOW_EMPTY_VALUE = "showEmptyValue";

    public static final String FROM_SUFFIX = TagConstants.PATH_SEP + "From";

    public static final String TO_SUFFIX   = TagConstants.PATH_SEP + "To";

    /**
     * The max control size
     */
    private int maxCtrlSize = DEFAULT_CTRL_SIZE;

    protected String formName;

    private MessageContainerBaseTag parentTag;

    private XMLBean requestBean;

    private MessageContainer msgPart;

    private XMLGenerator message;

    private boolean entryValidation = true;

    private boolean helpCreation = true;

    private boolean isCopyAction = false;

    private boolean isEditAction = false;

    private boolean isConvertAction = false;

    private boolean isCreateTemplateFromOrderAction = false;

    private List<HtmlElement> hiddenFields;

    private List<String> fieldsToSort;

    /* Will hold the name of the fields that need to convert as per client time zone */
    protected List<String> fieldsToConvertTZ;

    protected List<String> fieldsToDisable;

    private String fieldLabelMode;

    private DomainProperties domainProperties = null;

    private final String AUTOGENERATED = "Auto-Generated";

    protected final String CALLALLRULES = "callAllRules(this);";

    protected final String FIELDS_TO_CONVERT_TZ = "FieldsToConvertTZ";

    protected final String SERVER_TIMEZONE = "ServerTimezone";

    private final String COMMA = ",";
    
    private String resourceName;
    
    public String actionNodeValue;

    public String serverTimeZone;

    boolean highLightbk = false;
    /**
     * Sets the default HTML form name containing the relevant fields.
     *
     * @param  formName  Form name.
     *
     * @exception  JspException  Thrown when an error has occurred.
     */
    public void setFormName(String formName) throws JspException
    {
        setDynAttribute("formName", formName, String.class);
    }

    /**
     * Gets the default HTML form name containing the relevant fields.
     *
     * @return  Form name.
     */
    public String getFormName()
    {
        if (!StringUtils.hasValue(formName))
        {
            formName = DEFAULT_FORM_NAME;
        }

        return formName;
    }

    /**
     * Set the layout used by this body.
     * @param layout Obj
     * @throws JspException on error
     */
    public void setLayout(Object layout) throws JspException
    {
       setDynAttribute("layout",layout, HtmlElementLayout.class);
    }


    /**
     * Indicates if entry validation is enabled or disabled.
     * @return true if it is enabled otherwise false.
     */
    public boolean isEntryValidationEnabled()
    {
       return entryValidation;
    }

    /**
     * Get the maximum control size
     * @return int
     */
    public int getMaxCtrlSize()
    {
        return maxCtrlSize;
    }

    /**
     * Set the maximum control size
     * @param size int
     */
    public void setMaxCtrlSize(int size)
    {
        maxCtrlSize = size;
    }


    /**
     * Set help creation to true or false
     * by default this is true.
     *
     * @param bool String
     */
    public void setEnableHelpCreation(String bool)
    {
       this.helpCreation = ServletUtils.getBoolean(bool, true);
    }

    /**
     * enables field entry validation.
     * If true the neccessary javascript is generated to perform validation of fields.
     *
     * @param bool true to enable or false to disable.
     *
     */
    public void setEnableEntryValidation(String bool)
    {
       try {
          entryValidation = StringUtils.getBoolean(bool);
       } catch (FrameworkException e) {
          log.warn("Invalid value for attribute entryValidation, using default of false : " + e.getMessage() );
          entryValidation = false;
       }
    }




     /**
     * Obtains message and other needed information
     * from super class.
     * This method must call super.doStartTag first.
     *
     */
    public int doStartTag() throws JspException
    {
       super.doStartTag();
       initializeDomainProperties();

       requestBean = (XMLBean)pageContext.findAttribute(ServletConstants.REQUEST_BEAN);

       if (requestBean != null)
       {
           actionNodeValue = requestBean.getHeaderValue("Action");

           resourceName = requestBean.getHeaderValue(META_RESOURCE_NAME);

           isCopyAction  = COPY_ORDER_ACTION.equals(actionNodeValue);

           isEditAction  = EDIT_ORDER_ACTION.equals(actionNodeValue);

           isConvertAction  = CONVERT_ORDER_ACTION.equals(actionNodeValue);

           isCreateTemplateFromOrderAction = CREATE_TEMPLATE_FROM_ORDER_ACTION.equals(actionNodeValue);
       }

       this.formName = (String)getDynAttribute("formName");
       this.layout = (HtmlElementLayout) getDynAttribute("layout");


       hiddenFields = new ArrayList<HtmlElement>();
       fieldsToSort = new ArrayList<String>();
       fieldsToConvertTZ = new ArrayList<String>();
       fieldsToDisable = new ArrayList<String>();

       fieldLabelMode = PropUtils.getPropertyValue(props, TagConstants.DISPLAY_FIELD_MODE_PROP, TagConstants.DISPLAYNAME_LABEL);

       // This will update the time zone fields in request bean that requires conversion
       updateBeanTZFields();


       if (layout == null) {
          log.error("A HtmlElementLayout object needs to be specified.");
          throw new JspTagException("BodyTag: A HtmlElementLayout object needs to be specified.");
       }

       message = getMessageData();

       if (log.isDebugEnabled() )
          log.debug(" Using layout [" + layout.getClass().getName() + "]");

       // get the current section message part.
       try
        {
           parentTag = (MessageContainerBaseTag) this.getParent();
           msgPart = parentTag.getCurrentPart();

        }
        catch (Exception e)
        {
            String err = StringUtils.getClassName(this) + " This class needs to have an ancestor tag which implements MessageContainerBaseTag. " + e.getMessage();
            log.error(err);
            log.error("",e);
            throw new JspException(err);
        }


       // body tag has no body so skip it instead of evaluate it.
       return SKIP_BODY;
    }

    /**
     *  Generates each html element and calls the doLayout tag
     * of the layout class.
     *
     * The follow structure is passed to the HtmlElementLayout class:
     *
     * <pre>
     *   HtmlContainer (root messagePart which is a Section or Form)
     *      HtmlContainer (fields set holder) one or more of these exist
     *         HtmlElement (field)  one or more of these exist
     *         HtmlFieldGrpContainer(field group). zero or more of these exist
     *            HtmlElement (field)  one or more of these exist
     *      HtmlSubSecContainer (sub section container ) one or more of these exist
     *         HtmlContainer   (one for each existing sub section) one or more of these exist.
     *            HtmlElement (field) one or more of these exist.
     * </pre>
     *
     * @see com.nightfire.webgui.core.tag.message.support
     *
     */
    public int doEndTag() throws JspException
    {


        try {

           boolean createNewFieldSet = true;

           HtmlContainer holder = new HtmlContainer();

           Iterator childIter =  msgPart.getChildren().iterator();

           HtmlContainer fieldGrouper = null;
           // loop over all children
           // if the child is a Field
           // check if we have to create a new field container.
           // if we do create one, otherwise
           // add all fields to the current field container.
           // as soon as we find another type of MessagePart
           // then reset the field container flag so a new
           // container will be created for any fields later on.

           while ( childIter.hasNext()) {
              MessagePart p = (MessagePart)childIter.next();
              if ( p instanceof Field || p instanceof FieldGroup) {
                 if (createNewFieldSet) {
                    fieldGrouper = (HtmlContainer) holder.add(new HtmlContainer() );
                    createNewFieldSet = false;
            log.debug("Creating a new Field Container for Fields.");
                 }
                 createHTML( p, fieldGrouper);
              }  else {
                 createNewFieldSet = true;
                 createHTML( p , holder);
              }

           }

            String html = layout.doLayout(holder);

            pageContext.getOut().print(html);

            // hidden fields are now added since they were not part or the laid out fields.
            if ( hiddenFields.size()  > 0 ) {
              StringBuffer hiddenFieldsBuf = new StringBuffer();
                for (HtmlElement elem : hiddenFields)
                {
                    hiddenFieldsBuf.append(elem.getHTML()).append(TagConstants.NL);
                }
              pageContext.getOut().print(hiddenFieldsBuf.toString());
            }

            if (fieldsToSort.size() > 0)
            {
                String str = fieldsToSort.toString().substring (1, fieldsToSort.toString().length ()-1);
                pageContext.getOut().print("<input type=\"hidden\" name=\"FieldsToSort\" value=\"" + str + "\" >");
            }
            if (fieldsToConvertTZ.size() > 0)
            {
                String str = fieldsToConvertTZ.toString().substring (1, fieldsToConvertTZ.toString().length ()-1);

                pageContext.getOut().print("<input type=\"hidden\" name=\"" + ServletConstants.NF_FIELD_PREFIX + FIELDS_TO_CONVERT_TZ + "\" value=\"" + str + "\" >" + "<input type=\"hidden\" name=\"" + ServletConstants.NF_FIELD_PREFIX + SERVER_TIMEZONE + "\" value=\"" + serverTimeZone + "\" >");
            }
            
            String disableString = "";
            if (fieldsToDisable.size() > 0)
            {
                Iterator iter = fieldsToDisable.iterator();

                while (iter.hasNext())
                {
                    disableString = disableString + "|" + (String)iter.next();
                }
            }

            // if serverTimeZone is NULL (or Null string) then make it empty string.
            if (serverTimeZone == null || (StringUtils.hasValue(serverTimeZone) && serverTimeZone.equalsIgnoreCase(NULL_STRING_VALUE) ))
                serverTimeZone = "";

            pageContext.getOut().print("<input type=\"hidden\" name=\"" + ServletConstants.NF_FIELD_PREFIX + NULL_CLAUSE_FIELDS + "\" value=\"" + disableString + "\" >" + "<input type=\"hidden\" name=\"" + ServletConstants.NF_FIELD_PREFIX + SERVER_TIMEZONE + "\" value=\"" + serverTimeZone + "\" >");


            return EVAL_PAGE;
        }
        catch (Exception ex)
        {

            log.error("",ex);
            log.error("Error in Body Tag: " + ex.getMessage(), ex);
            throw new JspException(ex);
        }
    }

    public void release()
    {
        super.release();

        layout = null;
        msgPart         = null;
        maxCtrlSize = MAX_CTRL_SIZE;
        parentTag   = null;
        message = null;
        helpCreation = true;
    }

    /**
     * Creates the html elements for
     * fields, field groups, and repeating sub sections.
     *
     * @param part     The message part to create the HtmlElement for. (
     *                 Supports RepeatingSubSection, FieldGroup, and Field.
     * @param holder   The parent container to place the generated HtmlElement into.
     * @throws FrameworkException if there is an error accessing the MessagePart.
     * @see com.nightfire.webgui.core.meta
     *
     */
    private void createHTML(MessagePart part, HtmlContainer holder) throws FrameworkException
    {
        // see what kind of part this is
        if (part instanceof RepeatingSubSection) {
            createSubSection((RepeatingSubSection)part, holder);
        } else if (part instanceof FieldGroup) {
            createFieldGrp((FieldGroup)part, holder);
        } else {
            Field field = (Field)part;
            String path =  parentTag.updateXmlPathIndex(field.getParent().getFullXMLPath());
            createField(field, holder, path, null);
        }
    }

    /**
     * returns a new HtmlSubSectionContainer which contains a HtmlContainer for each repeating
     * row. Each row then has a set of HtmlElements to represent the fields.
     * The top HtmlContainer holds all subsections, and a header subsection name (wrapped in <tr>..</tr>)
     * and a footer add button (wrapped in <tr>...</tr>).
     * Each SubSection is a HtmlContainer where the header has
     * the delete button code (wrapped in <td>...</td>), and the children are the fields
     * in that section.
     * Each field in a subsection are also wrapped by (<td>...</td>).
     *
     * @param section repeating sub section
     * @param holder html holder
     * @throws FrameworkException on error
     */
    private void createSubSection(RepeatingSubSection section, HtmlContainer holder)
        throws FrameworkException
    {

        log.debug("Creating Repeating Sub Section.");

        ServletContext servletContext = pageContext.getServletContext();
        //add BASIC_REPLICATE_DISPLAY to constants file
        String webAppName = (String)servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
        Properties initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
        String basic  = initParameters.getProperty(ServletConstants.BASIC_REPLICATE_DISPLAY);
        boolean isReplicateDisplay = TagUtils.isReplicateDisplayTrue(basic);

        holder = (HtmlContainer) holder.add(new HtmlSubSecContainer() );
        // get the list of fields
        int partCount = section.getChildren().size();
        // create each entry
        int sectionNum = 0;

        String subSectionPath = parentTag.updateXmlPathIndex(section.getFullXMLPath() );
        String xmlPath = subSectionPath + "(" + sectionNum + ")";


        holder.append(NL)
            .append("<tr><td class=\"")
            .append(TagConstants.CLASS_SUBSECTION_HEADING).append("\">")
            .append(TagUtils.escapeHtmlSpaces(section.getDisplayName())).append("</td>");

        if(!isReadOnly()) {
            if(isReplicateDisplay)
                addReplicateGUI(holder, subSectionPath);
            else
                addNoReplicateGUI(holder, subSectionPath);
        }
        holder.append("</tr>").append(NL);

        // sub-section headings occupy two lines
        holder.add(SKIP_LINE);
        holder.add(SKIP_LINE);

        HtmlContainer newHolder;


        // loop over each repeating section
        do
        {
           Node xmlPathNode = null;

           // the first time this may be null
           if (message.exists(xmlPath) )
              xmlPathNode = message.getNode(xmlPath);

            newHolder = new HtmlContainer();
            holder.add(newHolder);

            if (isReadOnly() ) {
                newHolder.append("<td></td>");

            } else {
                // allow the section to be deleted
                newHolder.append("<td><a href=\"javascript:deleteSubSection(&quot;")
                    .append(xmlPath).append("&quot;)\" onMouseOut=\"")
                    .append("window.status='';return true;\" onMouseOver=\"")
                    .append("window.status='Delete Subsection';return true;\">")
                    .append("<img border=\"0\" src=\"")
                    .append(contextPath + "/" + TagConstants.IMG_DEL_ROW)
                    .append("\" alt=\"Delete Subsection\"/></a></td>");
           }


            for (int i = 0; i < partCount;i++ ) {
               Field childPart = (Field) section.getChild(i);

                // add the field
                HtmlElement fieldHtml = createField(childPart, newHolder, xmlPath, xmlPathNode);

                // wrap the element in a table data cell
                if ( fieldHtml != null) {
                   fieldHtml.prepend("<td>");
                   fieldHtml.append("</td>");
                }

            }


            xmlPath = subSectionPath + "(" + (++sectionNum) + ")";
        } while (message.exists(xmlPath));

    }

    /**
     * Displays the form without the replicable groups GUI,
     *
     * @param holder html holder
     * @param subSectionPath String
     */
    protected void addNoReplicateGUI(HtmlContainer holder, String subSectionPath) {
            // add button
         String addPath = addNewXmlPathIndex(getMessageData(), subSectionPath);
         holder.appendFooter("<tr class=\"").appendFooter(TagConstants.
                                CLASS_BTN_BOX);
         holder.appendFooter("\"><td>");
         holder.appendFooter("<a href=\"javascript:addSubSection(&quot;");
         holder.appendFooter(addPath + "&quot;");
         holder.appendFooter(")\" onMouseOut=\"");
         holder.appendFooter("window.status='';return true;\" onMouseOver=\"");
         holder.appendFooter(
                                "window.status='Add Subsection';return true;\"><img border=\"");
         holder.appendFooter("0\" src=\"");
         holder.appendFooter(contextPath + "/" + TagConstants.IMG_ADD_ROW);
         holder.appendFooter("\" alt=\"Add Subsection\"/></a></td></tr>");
         holder.appendFooter(NL);
    }

    /**
     * It displayReplicate is true then this method display the
     * the text box for number of copies and checkbox for replication
     *
     * @param holder html holder
     * @param subSectionPath String
     */
    protected void addReplicateGUI(HtmlContainer holder, String subSectionPath) {
            // add button
        String addPath = addNewXmlPathIndex(getMessageData(), subSectionPath);
        String curPath = subSectionPath;
        //these need to be added to the constant files
        String addCount = curPath + "." + "NFH_AddNodeCount";
        String replicateNode = curPath + "." + "NFH_ReplicateData";
        String xmlSrcPath = curPath + "." + "NFH_XMLSrcPath";
        String maxOccursParams = curPath + "." + "NFH_MAXOccurs";
        String currChildrenParams = curPath + "." + "NFH_CURRChildren";
        String maxOccurs = msgPart.getMaxOccurs();

        String currentChildren= "";
        XMLGenerator message = getMessageData();

        //to get the current existing children
        try{
            if (message != null) {
                if (message.exists(curPath)) {
                    Node node = message.getNode(curPath);
                    if (node != null) {
                        Node parentNode = node.getParentNode();
                        int childCnt = XMLGeneratorUtils.getChildCount(parentNode,
                                node.getNodeName());
                        currentChildren = String.valueOf(childCnt);
                    }
                }
            }
        }
        catch(Exception ex) {
            String err = "Failed to add replicate GUI in body tag: " + ex.getMessage();
            log.error(err, ex);

        }

        holder.append("<td>")
                .append("<a href=\"javascript:addSubSection(&quot;").append(addPath).append("&quot;")
                .append(",")
                .append("&quot;" + addCount + "&quot;")
                .append(",")
                .append("&quot;" + replicateNode + "&quot;")
                .append(",")
                .append("&quot;" + xmlSrcPath + "&quot;")
                .append(",")
                .append("&quot;" + maxOccursParams + "&quot;")
                .append(",")
                .append("&quot;" + currChildrenParams + "&quot;")

                .append(")\" onMouseOut=\"")
                .append("window.status='';return true;\" onMouseOver=\"")
                .append("window.status='Add Subsection';return true;\"><img border=\"")
                .append("0\" src=\"")
                .append(contextPath + "/" + TagConstants.IMG_ADD_ROW )
                .append("\" alt=\"Add Subsection\"/></a></td>")

                .append("<td class=\"" + TagConstants.CLASS_FIELD_LABEL_REPLICATE + "\">")
                .append( "Copies:" )
                .append("<input type=\" text \" name = \"")
                .append(addCount + "\" maxlength=\"5\" size=\"1\" value=\"\" ")
                .append("onBlur=\"entryValidation(this, new ValidateInfo('")
                // the last argument is for empty error message
                .append(addCount + "', 'DECIMAL', -1, '', '') );\" >")
                .append("</td>")

                .append("<td class=\""+ TagConstants.CLASS_FIELD_LABEL_REPLICATE+ "\">")
                .append( "Replicate:")
                .append("<input type=\"checkbox\" name=\"")
                .append(replicateNode + "\" value=\"\" >")
                .append("</td>")

                .append("<td>")
                .append("<input type=\"hidden\" name=\"")
                .append(xmlSrcPath + "\" value=\"")
                .append(xmlSrcPath + "\" >")
                .append("</td>")

                .append("<td>")
                //maxCounts hidden
                .append("<input type=\"")
                .append("hidden\" ")
                .append("name=\"")
                .append(maxOccursParams + "\"")
                .append(" value=\"")
                .append(maxOccurs)
                .append("\" " +">")
                .append("</td>")

                //current children count
                .append("<td>")
                .append("<input type=\"")
                .append("hidden\" ")
                .append("name=\"")
                .append(currChildrenParams + "\"")
                .append(" value=\"")
                .append(currentChildren)
                .append("\" " +">")
                .append("</td>");

    }//end addReplicateGUI


    /**
     * returns a new HtmlFieldGrpContainer which containers a set of fields(HtmlElements).
     * @param grp The field group object
     * @param holder The holder to add the html elements to.
     * @throws FrameworkException on error
     */
    protected void createFieldGrp(FieldGroup grp, HtmlContainer holder)
        throws FrameworkException
    {
        log.debug("Creating Field Group");

        if (!(TagUtils.isDisplayFieldGroup (grp, pageContext, domainProperties)))
        {
            log.debug("Dev: Skipping Creation of Field Group");
            return;
        }

        HtmlContainer parent = holder;
        holder = (HtmlContainer) holder.add(new HtmlFieldGrpContainer() );


        /*********Code for column break in FieldGroup starts*************/
        String columnBreakField = "";
        MessagePart parentMsg = grp.getParent();

        // find the first section for help
        // if there is not section find the first form for help
        while (parentMsg != null && !( parentMsg instanceof Section || parentMsg instanceof Form) )
            parentMsg = parentMsg.getParent();

        // checking for column-break-field-id property for the Section
        if (parentMsg instanceof Section)
        {
            Section temp = (Section) parentMsg;
            if (StringUtils.hasValue (temp.getCustomValue ( COLUMN_BREAK_FIELDID_CUSTOM_VALUE )))
                columnBreakField = temp.getCustomValue ( COLUMN_BREAK_FIELDID_CUSTOM_VALUE );
        }
        // If fieldGroup has column break custom value set as true then
        // set columnBreak for container as true.
        String hasColumnBreak = grp.getCustomValue ( COLUMN_BREAK_CUSTOM_VALUE );

        if (StringUtils.getBoolean(hasColumnBreak, false) || (TagUtils.stripIdSuffix (grp.getId ()).equals (columnBreakField)))
        {
            parent.setColumnBreak(true);
            holder.setColumnBreak(true);
        }
        /*********Code for column break in FieldGroup ends*************/

        holder.setSpanning(false);

        // field group heading occupys one line
        holder.add(SKIP_LINE);


        // get the list of fields

        int partCount = grp.getChildren().size();
        boolean showBorder = StringUtils.getBoolean(grp.getCustomValue(SHOW_BORDER_CUSTOM_VALUE), true);
        String fgcss = showBorder ? " class=\"" + TagConstants.CLASS_FIELDGRP_TABLE + "\" ": "";

        holder.append(NL)
            .append("<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"")
            .append(fgcss).append("><tr>").append(NL);

        boolean isTabular = StringUtils.hasValue (grp.getCustomValue (TABULAR_HEADERS_CUSTOM_VALUE)) &&
                            StringUtils.hasValue (grp.getCustomValue (TABULAR_LABELS_CUSTOM_VALUE));
        
        boolean isMultiControl = StringUtils.hasValue (grp.getCustomValue (FIELD_GROUP_TYPE_CUSTOM_VALUE)) &&
                                 grp.getCustomValue (FIELD_GROUP_TYPE_CUSTOM_VALUE).equals(FIELD_GROUP_TYPE_VALUE_MULTIPLE);

        if (log.isDebugEnabled())
            log.debug ("Field Group type: isTabular [" + isTabular + "], isMultiControl [" + isMultiControl + "]");

        if ( isTabular )
        {
            createTabularFieldGrp (grp, holder);
        }
        else if ( isMultiControl )
        {
            createMultiControlFieldGrp (grp, holder);
        }
        else
        {
            holder.append("<td class=\"" + TagConstants.CLASS_FIELDGRP_HEADING + "\">" + TagUtils.escapeHtmlSpaces(grp.getDisplayName()) + "</td></tr>").append(NL);

            String xmlPath = parentTag.updateXmlPathIndex(grp.getFullXMLPath());

            Node xmlPathNode = null;

               // the first time this may be null
             if (message.exists(xmlPath) )
                xmlPathNode = message.getNode(xmlPath);

            // add all fields to this field group
            for (int i = 0; i < partCount;i++ )
            {
                Object child= grp.getChild(i);
                HtmlElement fieldHtml = null;
                /*
                     The below if part handles the
                     scenario if a FieldGroup contains
                     another FieldGroup.
                 */
                if(child instanceof FieldGroup)
                {
                    FieldGroup childPart = (FieldGroup)child;
                    HtmlContainer newHolder = new HtmlFieldGrpContainer();                    
                    createFieldGrp(childPart, newHolder);
                    fieldHtml = new HtmlElement();
                    fieldHtml.append(newHolder.getHTML());
                    holder.add(fieldHtml);

                }
                else{
                    Field childPart = (Field)child;

                    // add the field
                    fieldHtml = createField(childPart, holder, xmlPath, xmlPathNode);
                }

                // wrap the element in a table data cell
                if ( fieldHtml != null ) {
                    fieldHtml.prepend("<tr><td>");
                    fieldHtml.append("</td></tr>");
                }
            }
        }

        holder.appendFooter("</table>").appendFooter(NL).appendFooter(NL);

    }

    /**
     * Creates the html for a field.  Overloaded method
     *
     * @param field Field
     * @param con html holder
     * @param xmlParentPath String
     * @param xmlParentNode Node
     * @return html elem
     * @throws FrameworkException on error
     */
    private HtmlElement createField(Field field, HtmlContainer con,
                                    String xmlParentPath, Node xmlParentNode)
        throws  FrameworkException
    {
        return createField(field, con, xmlParentPath, xmlParentNode, true);
    }

    /**
     * This function creates the hyperlink on the label
     * and prepare the js link that will display online HELP.
     *
     * @param field Field object
     * @param elem Html element containing field
     * @param fieldName name of the field to be used to prepare label (can be different than field id)
     */
    private void createHelp (Field field, HtmlElement elem, String fieldName)
    {
        //String fieldName = TagUtils.getFieldLabel(field, fieldLabelMode);
        String fieldFullName = field.getFullName();

        String columnBreakField = "";
        MessagePart parent = field.getParent();

        // find the first section for help
        // if there is not section find the first form for help
        while (parent != null && !( parent instanceof Section || parent instanceof Form) )
            parent = parent.getParent();

        // checking for highlight and column-break-field-id
        // property for the Section
        if (parent instanceof Section)
        {
            Section temp = (Section) parent;
            String highLightVal = temp.getCustomValue ( HIGHLIGHT_BACKGROUND_CUSTOM_VALUE );
            highLightbk = StringUtils.getBoolean (highLightVal, false);
            if (StringUtils.hasValue (temp.getCustomValue ( COLUMN_BREAK_FIELDID_CUSTOM_VALUE )))
                columnBreakField = temp.getCustomValue ( COLUMN_BREAK_FIELDID_CUSTOM_VALUE );
        }
        // If field has column break custom value set as true or
        // the id given at section level is of present element then
        // set columnBreak for this element and its container as true.
        String hasColumnBreak = field.getCustomValue ( COLUMN_BREAK_CUSTOM_VALUE );
        if (StringUtils.getBoolean(hasColumnBreak, false) || (TagUtils.stripIdSuffix (field.getId ()).equals (columnBreakField)))
        {
            elem.setColumnBreak(true);
        }

        elem.append("<a TABINDEX=\"-1\"");
        if (helpCreation)
        {
            String supplier = null;
            String service = null;
            try
            {
                // Get the supplier from the resource name.
                supplier = ServletUtils.getResourceSupplier ( resourceName );
                service = ServletUtils.getResourceService ( resourceName );
            }
            catch ( ServletException e )
            {
                log.warn ( "Could not obtain Supplier value from resource [ " + resourceName + " ]" );
            }

            elem.append(" href=\"");
            String helpURL = TagUtils.normalizeID(parent.getId()) + "Help.html#" + TagUtils.normalizeID(field.getId());
            elem.append("javascript:showHelp('");
            elem.append(contextPath).append(helpDir + "/" + TagUtils.normalizeID(field.getRoot().getId()) );

            // Check if supplier value is returned from the resource name then append it in the help dir path.
            if( StringUtils.hasValue ( service ) && StringUtils.hasValue ( supplier ))
                elem.append("/").append(service ).append("-").append(supplier );
            else if( StringUtils.hasValue ( supplier ) )
                elem.append("/").append(supplier );

            elem.append("', '").append(helpURL).append("')\"");
        }

        elem.append(" onMouseOut=\"return displayStatus('');\" ").append("onMouseOver=\"return displayStatus('");

        // escape any special characters
        escapeAttr(elem, fieldFullName );
        elem.append("');\"").append(">").append(fieldName).append("</a>");
    }

    /**
     * Creates label for the field
     *
     * @param field Field obj
     * @param elem HtmlElement obj
     */
    private void createLabel (Field field, HtmlElement elem)
    {
        createLabel (field, elem, null);
    }

    /**
     * Creates label for the field
     *
     * @param field Field obj
     * @param elem HtmlElement obj
     * @param fieldLabel optional label to be use if traditional field name is not be displayed (ex. TABULAR and MULTIPLE cases)
     */
    private void createLabel (Field field, HtmlElement elem, String fieldLabel)
    {
        String fieldName = TagUtils.getFieldLabel(field, fieldLabelMode);

        /* In case of tabular and multiple controls the field label will be used */
        if (StringUtils.hasValue(fieldLabel))
            fieldName = fieldLabel;

        elem.append("<Table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\"><tr><td class=\"");

        // set the class to indicate required, conditional, or optional
        int reqType = field.getDataType().getUsage();

        if ( isProhibited (field))
            elem.append(TagConstants.CLASS_FIELD_LABEL_PROHIBITED);
        else if (reqType == DataType.REQUIRED) {
           String alwaysRequired = field.getCustomValue(ALWAYS_REQUIRED);
           if (StringUtils.getBoolean(alwaysRequired, false)) {
               fieldName = "* " + fieldName;
               elem.append(TagConstants.CLASS_FIELD_LABEL_REQUIRED_ALWAYS);
           } else
               elem.append(TagConstants.CLASS_FIELD_LABEL_REQUIRED);
        }
        else if (reqType == DataType.CONDITIONAL)
           elem.append(TagConstants.CLASS_FIELD_LABEL_CONDITIONAL);
        else
           elem.append(TagConstants.CLASS_FIELD_LABEL_OPTIONAL);

        // if the field is always required prepend it with a *
        elem.append("\">");

        // fieldName need to pass as it may change while label creation (like in case of "always-required")
        createHelp (field, elem, fieldName);

        elem.append("</td>");
    }

    /**
     * Creates the html for a field.  It
     * also creates the help link. This method calls out to  createOptionField,
     * createAreaField, and createInputField, so those methods should be overwritten
     * in most cases.
     *
     *
     * @param field The field to create the html for.
     * @param con The HtmlContainer to add this field to.
     * @param xmlParentPath - The full path to the parent xml node for this field.
     * @param xmlParentNode - The node of the xml parent node for this field. Improves multiple
     *     access to the field. Can be null.
     * @param createLabel label will be created if true (false used in case of TABULAR)
     *
     * @return The new HtmlElement that was added to the passed in HtmlContainer.
     * @throws FrameworkException on error
     */
    private HtmlElement createField(Field field, HtmlContainer con,
                                    String xmlParentPath, Node xmlParentNode, boolean createLabel)
        throws  FrameworkException
    {
        // if the NancVersion does not match, skip the display of field
        if (!(TagUtils.isDisplayField(field, pageContext, domainProperties)))
            return null;

        HtmlElement newElem = new HtmlElement();

        // if it is a hidden field just return null
        // since it is not a field that needs to be displayed

        if ( TagUtils.isHidden(field) ) {
           createFieldFormElement(field, newElem, xmlParentPath, xmlParentNode);
           return null;
        }

        // create a new element and add it to the current container
        HtmlElement elem = con.add(newElem);


        log.debug("Creating Field");

        String fieldName = TagUtils.getFieldLabel(field, fieldLabelMode);
        String fieldFullName = field.getFullName();

        if (!StringUtils.hasValue(fieldFullName) )
           fieldFullName = fieldName;

        String abbrev = field.getAbbreviation();

        if (StringUtils.hasValue(abbrev) ) {
            // append field abbreviation to field name for help.
            if ( !fieldFullName.equalsIgnoreCase(abbrev) ) {
                fieldFullName = new StringBuffer(fieldFullName).append(" (").append(abbrev).append(")").toString();
            }
        }

        // create the label if createLabel is true, else we will create only field without any <table>, <tr> and <td>
        if (createLabel)
        {
            createLabel (field, elem);
        }
        elem.append("<td>");

        String createRadio = field.getCustomValue ( CREATE_TWO_RADIO_BUTTONS_CUSTOM_VALUE );
        if (StringUtils.hasValue ( createRadio ))
        {
            createTwoRadioControls (field, elem, xmlParentPath, createRadio);
        }

        // create the form element
        createFieldFormElement(field, elem, xmlParentPath, xmlParentNode);

        if (createLabel)
        {
            elem.append("</td>");
            elem.append("</tr></Table>");
        }

        // setting column break to container if html elements is already set to TRUE
        if (elem.hasColumnBreak()) con.setColumnBreak(true);

        return elem;
    }




    /**
     * Creates form input element HTML for a Field
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field.
     * @throws FrameworkException on error
     */
    private void createFieldFormElement(Field field, HtmlElement elem,
                                    String xmlParentPath, Node xmlParentNode)
        throws  FrameworkException
    {
        // create the control
        DataType dType = field.getDataType();
        int type = dType.getType();

        if (TagUtils.isHidden(field) )
            createHiddenField(field, elem, xmlParentPath, xmlParentNode);
        else if (type == DataType.TYPE_RADIO_BUTTON)
            createRadioField(field, elem, xmlParentPath,xmlParentNode);
        else if (type == DataType.TYPE_CHECK_BOX)
            createCheckboxField(field, elem, xmlParentPath,xmlParentNode);
        else if (type == DataType.TYPE_ENUMERATED)
            createOptionField(field, elem, xmlParentPath,xmlParentNode);
        else if (type == DataType.TYPE_TEXT_AREA)
            createAreaField(field, elem, xmlParentPath,xmlParentNode);
        else if ( !isReadOnly() && type == DataType.TYPE_SPID )
            createSPIDField(field, elem, xmlParentPath,xmlParentNode);
        else
            createInputField(field, elem, xmlParentPath, xmlParentNode);
    }

    /**
     * This convenient method determines whether the value of a Field
     * needs to be blanked out in the case of a copy action
     * or in case of create-template action.
     *
     * @param  field  Field object.
     * @return  true if blank-out is required, false otherwise.
     */
    private boolean blankOutValueForCopy(Field field)
    {
        return blankOutValueForCopy (field.getXMLPath (), field.getCustomValue(BLANK_OUT_FOR_COPY_CUSTOM_VALUE));
    }

    /**
     * This convenient method determines whether the value of a Field
     * needs to be blanked out in the case of a copy action
     * or in case of create-template action.
     *
     * @param xmlPath String
     * @param blankOutString String
     * @return  true if blank-out is required, false otherwise.
     */
    private boolean blankOutValueForCopy (String xmlPath, String blankOutString)
    {
        if (isCopyAction || isCreateTemplateFromOrderAction)
        {
            boolean blankOut       = StringUtils.getBoolean(blankOutString, false);

            XMLBean responseBean   = (XMLBean)pageContext.findAttribute(ServletConstants.RESPONSE_BEAN);

            // We need to also check whether this field is being displayed after an
            // action has been performed.  In which case, we do not want to blank
            // out the field, but show the new entered value instead.

            if (blankOut && (responseBean == null))
            {
                if (log.isDebugEnabled())
                {
                    if (isCopyAction)
                        log.debug("blankOutValueForCopy(): Blanking out field [" + xmlPath + "] for copy action ...");
                    else
                        log.debug("blankOutValueForCopy(): Blanking out field [" + xmlPath + "] for create-template action ...");
                }

                return true;
            }
        }

        return false;
    }

    /**
     * This convenient method determines whether a Field needs to be
     * disabled in the case of a copy action.
     *
     * @param  field  Field object.
     * @return  true if disabling is required, false otherwise.
     */
    private boolean disableFieldForCopy(Field field)
    {
        return processField (field, DISABLE_FOR_COPY_CUSTOM_VALUE, actionNodeValue, COPY_ORDER_ACTION, false);
    }

    /**
     * This convenient method determines whether a Field needs to be
     * disabled in the case of a column name AutoGenReqNo in DOMAIN table
     * is set to true (i.e. "Y").
     *
     * @param  field  Field object.
     * @return  true if disabling is required, false otherwise.
     */
    private boolean disableIfHasHostId(Field field)
    {
        if (StringUtils.getBoolean(field.getCustomValue(PROHIBIT_IF_HOSTID_CUSTOM_VALUE), false))
        {
            return (domainProperties != null && domainProperties.isAutoGenReqNoEnabled());
        }
        return false;
    }

    /**
     * This convenient method determines whether a Field needs to be
     * disabled in the case of a convert order action.
     *
     * @param  field  Field object.
     * @return  true if disabling is required, false otherwise.
     */
    private boolean disableFieldForConvert(Field field)
    {
        return processField (field, DISABLE_FOR_CONVERT_CUSTOM_VALUE, actionNodeValue, CONVERT_ORDER_ACTION, false);
    }

    /**
     * This convenient method determines whether a Field needs to be
     * disabled in the case of an edit action.
     *
     * @param  field  Field object.
     * @return  true if disabling is required, false otherwise.
     */
    private boolean disableFieldForEdit(Field field)
    {
        return processField (field, PROHIBIT_FOR_EDIT_CUSTOM_VALUE, actionNodeValue, EDIT_ORDER_ACTION, false);
    }

    /**
    * This convenient method determines whether a Field meets
    * criteria by the user action and passed in action as custom value.
    *
    * @param  field  Field object.
    * @param customProperty to check with custom value
    * @param actionToCheck name of the action to check
    * @param compareAction name of the action to compare
    * @param defaultValue return in case the action is not matching criteria
    * @return  true if disabling is required, false otherwise.
    */
   private boolean processField (Field field, String customProperty, String actionToCheck, String compareAction, boolean defaultValue)
   {
       if (StringUtils.hasValue(compareAction) && StringUtils.hasValue(actionToCheck) && compareAction.equals(actionToCheck))
       {
           String  customValue = field.getCustomValue(customProperty);
           boolean status      = StringUtils.getBoolean(customValue, false);

           if (log.isDebugEnabled())
           {
               log.debug("processField(): field [" + field.getXMLPath()
                        + "] for custom property [" + customProperty
                        + "], action [" + compareAction
                        + "], check against action [" + actionToCheck
                        + "] has status [" + status
                        + "].");
           }

           return status;
       }

       return defaultValue;
   }

 /**
    * This convenient method determines whether a Field needs to be
    * disabled in the case of given actions as custom value.
    *
    * @param  field  Field object.
    * @return  true if disabling is required, false otherwise.
    */
   private boolean disableFieldForActions(Field field)
   {
       String  disableActions = field.getCustomValue(DISABLE_FOR_ACTIONS_CUSTOM_VALUE);

       if (!StringUtils.hasValue(disableActions))
            return false;

       String[] disableActionsArr = StringUtils.getArray(disableActions, COMMA);
       List disableActionsList = Arrays.asList(disableActionsArr);

       return disableActionsList.contains(actionNodeValue);
   }
    /**
     * This convenient method determines whether a Field needs to be
     * auto populated on the basis of data present in header
     * part of request bean.
     *
     * @param  field  Field object.
     * @return  true if auto populating and disabling is required, false otherwise.
     */
    private String getHeaderNodeValue(Field field)
    {
        String headerNode = field.getCustomValue(GET_HEADER_VALUE);
        log.debug( "getHeaderNodeValue(): HeaderNode for field [" + field.getId() + "]: " + headerNode);
        String headerValue = null;

        if (StringUtils.hasValue(headerNode))
        {
            headerValue = requestBean.getHeaderValue(headerNode);
            log.debug( "getHeaderNodeValue(): headerNode value for field [" + field.getId() + "]: " + headerValue);
            if (StringUtils.hasValue (headerValue))
                return headerValue;
         }

        return "";
    }
        /**
     * This convenient method determines whether a Field needs to be
     * auto populated on the basis of data present in body
     * part of request bean.
     *
     * @param  field  Field object.
     * @return  value for auto populating .
     */
    private String getBodyNodeValue(Field field)
    {
        String bodyNode = field.getCustomValue(GET_BODY_VALUE);
        log.debug( "getBodyNodeValue(): BodyNode for field [" + field.getId() + "]: " + bodyNode);
        String bodyValue = null;

        if (StringUtils.hasValue(bodyNode))
        {
            bodyValue = requestBean.getBodyValue(bodyNode);
            log.debug( "getBodyNodeValue(): bodyNode value for field [" + field.getId() + "]: " + bodyValue);
            if (StringUtils.hasValue (bodyValue))
                return bodyValue;
         }

        return "";
    }

    /**
     * This convenient method retrieves the value for the custom key "default-for-auto-ver".
     * Needs to be used in conjunction with the 'auto-increment-ver' attribute.
     *
     * @param field Field object.
     * @return value of field to be checked.
     */
    private String getDefaultForAutoVer(Field field)
    {
        String returnVal = "";
        try
        {
            String autoVerValue = field.getCustomValue(DEFAULT_FOR_AUTO_VER_VALUE);
            if (autoVerValue != null) {
                returnVal= autoVerValue;
            }
        }
        catch (Exception e)
        {
            log.error("getDefaultForAutoVer(): Error in retrieving the value for customkey [" + DEFAULT_FOR_AUTO_VER_VALUE + "] Error: " + e.toString());
        }

        if (log.isDebugEnabled()) {
            log.debug("getDefaultForAutoVer(): Returning the value of customkey [" + DEFAULT_FOR_AUTO_VER_VALUE +
                "] as [" + returnVal + "].");
        }

        return returnVal;
    }

    /**
     * This convenient method retrieves the value for the Custom Key "number-of-rows".
     * If the Custom Key is not defined, the method returns the default number of rows.
     *
     * @param field Field object.
     * @return value of the key 'number-of-rows', if configured with appropriate value; DEFAULT_CTRL_LINES otherwise.
     */
    private int getNumberOfRows(Field field)
    {
        int returnVal = DEFAULT_CTRL_LINES;
        try
        {
            String numberOfRows = field.getCustomValue(NO_OF_ROWS_VALUE);
            if (StringUtils.hasValue(numberOfRows) && StringUtils.isDigits(numberOfRows)) {
                returnVal = StringUtils.getInteger(numberOfRows);
            }
        }
        catch (Exception e)
        {
            log.error("getNumberOfRows(): Error in retrieving the value for customkey [" + NO_OF_ROWS_VALUE + "] Error: " + e.toString());
        }

        if (log.isDebugEnabled()) {
            log.debug("getNumberOfRows(): Returning the value of customkey [" + NO_OF_ROWS_VALUE +
                "] as [" + returnVal + "].");
        }
        return returnVal;
    }

    /**
     * Creates just the select control for a Field
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field(May be null).
     * @throws FrameworkException on error
     */
    protected void createOptionField(Field field, HtmlElement elem,
                                    String xmlParentPath, Node xmlParentNode)
        throws  FrameworkException
    {
        String  xmlFieldPath = field.getXMLPath();

        String  fieldName    = getName(field, xmlParentPath);

        // Checking only drop down fields for the sorting
        // custom-value as only this control could be sorted.
        if (TagUtils.isSorted (field))
            fieldsToSort.add (TagUtils.stripIdSuffix (field.getId ()));

        boolean disabled     =  isProhibited (field);

        if (log.isDebugEnabled())
        {
            log.debug("Creating Option Field [" + field.getId() + "], with XML path [" + xmlFieldPath + "] ...");
        }

        String value = getValue(field, xmlParentPath, xmlFieldPath, xmlParentNode );
        boolean multiSelect= ServletUtils.getBoolean(field.getCustomValue(MULTIPLE_SELECT), false);
        boolean isTooltipEnabled= ServletUtils.getBoolean(field.getCustomValue(IS_TOOLTIP_ENABLED), false);
        boolean hasOnChange = false;

        if (multiSelect)
        {

            if (StringUtils.getBoolean(field.getCustomValue(REMOVE_NF_PREFIX), false)){
                elem.append("<select name=\"").append(fieldName).append("\"");
                elem.append(" multiple ");
            }
            else
            {
                elem.append("<select name=\"").append(ServletConstants.NF_FIELD_PREFIX).append(fieldName).append("\"");
                elem.append("multiple = multiple ");
            }

            String size;
            String sizeVal = field.getCustomValue(MULTIPLE_SELECT_SIZE);
            if (StringUtils.hasValue(sizeVal)&& StringUtils.isDigits(sizeVal)){
                size = sizeVal;
            }
            else{
                size = DEFAULT_MULTIPLE_SELECT_SIZE;
            }
            elem.append("size = " + size);
            if(!disabled && StringUtils.getBoolean(field.getCustomValue(REMOVE_NF_PREFIX), false))
                elem.append(" onBlur=\"setMultipleSelect('" + fieldName +"')\" " );

        }
        else
        {
            elem.append("<select name=\"").append(ServletConstants.NF_FIELD_PREFIX).append(fieldName).append("\"");
        }

        if (disabled)
        {
            //Set the onBlur function for option fields on viewonly page so that option fields would be non editable.
            if (multiSelect)
            {
              elem.append(" onBlur=\"setValue(this, '");
              if (StringUtils.hasValue(value) )
                 elem.append(value);
              elem.append("');\"");
            }
            createDisabledCode(field, elem, value);
            if (isTooltipEnabled)
            {
                elem.append(" title=\"").append(value).append("\"");
            }
        }
        else
        {
            editableOptionCheck(field, elem, fieldName);
            hasOnChange = isEditable (field);
        }

        String onChangeEventTemplate = field.getCustomValue(ON_CHANGE_EVENT_TEMPLATE_CUSTOM_VALUE);

        if (StringUtils.hasValue(onChangeEventTemplate))
        {
            generateOptionOnChangeHandler(elem, onChangeEventTemplate);
            hasOnChange = true;
        }
        if (!hasOnChange) // as already attached in generateOptionOnChangeHandler or editableOptionCheck
            elem.append(" onChange=\"").append(CALLALLRULES).append("\" ");

        if (isTooltipEnabled)
        {
            elem.append(" onmouseover=\"javascript:this.title=this.options(this.selectedIndex).title;\"");
        }

        elem.append(getRequiredFieldStyle (field) ).append(" >").append(NL);

        // Include an empty option is will be always added except for the case when it is explicitly stated NOT to.
        boolean showEmpty = ServletUtils.getBoolean(field.getCustomValue(SHOW_EMPTY_VALUE), true);

        if (showEmpty || ((field.getDataType().getUsage() != DataType.REQUIRED) && (!multiSelect)))
        {
             elem.append("<option value=\"\"></option>").append(NL);
        }

        String   queryCriteria       = field.getCustomValue(QUERY_CRITERIA_CUSTOM_VALUE);
        String   queryCriteriaTokens = field.getCustomValue(QUERY_CRITERIA_TOKENS_CUSTOM_VALUE);

        String[] optionValues        = null;

        String[] optionDisplayValues = null;

        String[][] optionFields      = null;

          if (StringUtils.hasValue(queryCriteria))
        {
              if(!disabled || StringUtils.getBoolean(field.getCustomValue(REMOVE_NF_PREFIX),false))
              {
                  optionFields        = TagUtils.queryOptionList(queryCriteria, message.getDocument(), pageContext, queryCriteriaTokens);
              }
        }

        String queryMethod = field.getCustomValue(QUERY_METHOD_CUSTOM_VALUE);

        /*
        * Fetch the enumerated field using the query method only if option-list values are
        * null and if there exists a custom value specified for QUERY_METHOD_CUSTOM_VALUE
        * parameter; and if the enumerated field is not disabled.
        */

        if (!disabled && StringUtils.hasValue(queryMethod) && optionFields == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("createOptionField(): Obtained customkey for the field as [" + QUERY_METHOD_CUSTOM_VALUE +"] with value ["+  queryMethod +"]");
            }

            // Custom value passed for QUERY_METHOD_CUSTOM_VALUE would in the format of method name
            // followed by comma separated strings in braces: <CustomMethodName>(param1,param2,param3,..)
            String queryMethodName =  queryMethod.substring(0,queryMethod.indexOf("("));

            String queryMethodParams = null;

            //checking if the method parameters are passed in the meta file, and only if they
            // are passed the queryMethod parameters are extracted.

            if(queryMethod.indexOf(")") - queryMethod.indexOf("(") > 1)

              queryMethodParams = queryMethod.substring(queryMethod.indexOf("(")+1,queryMethod.indexOf(")"));

            try
            {
                  //Invoking the query method dynamically i.e, invoking the method which is not known until runtime

                   Class tagFunctionsClass = TagFunctions.class;

                   Class argumentsType[] = new Class[1];

                   argumentsType[0] = String.class;

                   Method method = tagFunctionsClass.getMethod(queryMethodName,argumentsType);

                   Object[] arguments = new Object[] {queryMethodParams};
                   
                   TagFunctions obj = new TagFunctions();

                   obj.setPageContext(pageContext);

                   optionFields= (String[][]) method.invoke(obj,arguments);
            }
            catch (NoSuchMethodException e)
            {
                   log.error("createOptionField(): No such query method. Failed to obtain the Option list using the query method.\n" + e.getMessage());
            }
            catch (Exception e)
            {
                   log.error("createOptionField(): Failed to obtain the Option list using the query method.\n" + e.getMessage());
            }
        }

        if (optionFields == null)
        {
            log.debug("createOptionField(): Getting option values from meta Options section ...");

            OptionSource optionSource = field.getDataType().getOptionSource();

            if (optionSource != null)
            {
                optionValues        = optionSource.getOptionValues();

                optionDisplayValues = optionSource.getDisplayValues();
            }
        }
        else
        {
            optionValues        = optionFields[0];

            optionDisplayValues = optionFields[1];
        }

        boolean selectOption = false;

        if (optionValues != null)
        {
            for (int i = 0; i < optionValues.length; i++)
            {
                elem.append("<option value=\"");

                elem.append(optionValues[i]);

                boolean selectMultiple=  false;

                String defaultSelection = field.getCustomValue(DEFAULT_VAL);

                if (multiSelect)
                {
                        StringTokenizer strToken = new StringTokenizer(value,"|");

                        while(strToken.hasMoreTokens())
                        {
                                String token = strToken.nextToken();

                                if(token.equals(optionValues[i])) {
                                        boolean isThisADefaultVal = (defaultSelection!=null && defaultSelection.length() >0 && defaultSelection.equalsIgnoreCase("All") && defaultSelection.indexOf(token)!=-1);

                                        if(! isThisADefaultVal)
                                                selectMultiple = true;

                                }
                        }

                }

                // If this option matches the value to display then set it as selected.

                if (((value != null) && value.equals(optionValues[i])) || selectMultiple || isSingleValueOption (optionValues, showEmpty) )
                {

                    if (!blankOutValueForCopy(field))
                    {
                        elem.append("\" selected=\"true");
                    }                                                                                                                                                                                                                                                                     

                    selectOption = true;

                }

                elem.append("\" ");

                // Set the display name if one exists.  Otherwise use the option value
                // as the display name.

                String displayValue = optionValues[i];

                if ((optionDisplayValues != null) && (optionDisplayValues.length > i))
                {
                    displayValue = optionDisplayValues[i];
                }

                //Obtaining an alias of displayValue if the alias is defined.

                displayValue = aliasDescriptor.getAlias(pageContext.getRequest(), field.getFullXMLPath() , displayValue, true);

                if (isTooltipEnabled)
                {
                    String toolTipValue = displayValue;

                    if (toolTipValue.indexOf("\"") != -1)
                    {
                        toolTipValue = StringUtils.replaceSubstrings(toolTipValue, "\"","&quot;");
                    }

                    elem.append("title=\"").append(toolTipValue).append("\"");
                }

                elem.append(">");

                // Truncate the display name length if it's greater that the max-control
                // size.  Otherwise the field alignment will be out-of-whack.

                if (displayValue.length() > maxCtrlSize)
                {
                    displayValue = displayValue.substring(0, maxCtrlSize - 3) + "...";
                }

                elem.append(displayValue);

                elem.append("</option>").append(NL);
            }
        }

        // if none of the values in the drop down were selected and there
        // is a value then append it to the end of the list and select it.
        // This is done in case a value returned in a response message has
        // a value that is not provided in the meta file.

         if (StringUtils.hasValue(value) && !selectOption) {
             String trimmedDisplayValue = value;
             if (value.length() > maxCtrlSize)
             {
                trimmedDisplayValue = value.substring(0, maxCtrlSize - 3) + "...";
             }
             if (isTooltipEnabled)
                elem.append("<option selected title=\"").append(value).append("\" value=\"").append( value );
             else
                elem.append("<option selected value=\"").append( value );
            elem.append("\">").append( trimmedDisplayValue ).append("</option>");
         }

         // If this is an editable drop-down and is not read-only, add the last editable option.

         if (!disabled && isEditable(field))
         {
             elem.append("<option id=\"EditMe\" name=\"EditMe\" value=\"\" class=\"Editable\">--?--</option>");
         }

        // finish the control
        elem.append("</select>");
    }

    /**
     * This method can be overridden in the extending tags to check
     * option values.
     *
     * @param options String[] of options values
     * @param showEmpty - boolean indicates whether to show empty options
     * @return false
     */
    protected boolean isSingleValueOption (String[] options, boolean showEmpty)
    {
        return false;
    }

    /**
     * Checks to see if this drop-down is editable.  If editable, generate
     * appropriate html attributes for the SELECT element.
     *
     * @param  field      Meta field object.
     * @param  html       Html string to append to.
     * @param  fieldName  Meta field object name.
     */
    protected void editableOptionCheck(Field field, HtmlElement html, String fieldName)
    {
        if (isEditable(field))
        {
            log.debug("editableOptionCheck(): Making option drop-down editable ...");

            html.append(" id=\"").append(fieldName).append("\"");

            html.append(" onKeyDown=\"if (isAllowableKeyCode()) return true;\""); // 9 keyCode represents TAB

            html.append(" onKeyUp=\"return false\" onKeyPress=\"fnKeyPressHandler('");

            html.append(fieldName).append("')\" onChange=\"").append(CALLALLRULES).append("fnChangeHandler('");

            html.append(fieldName).append("')\"");
        }
    }

    /**
     * Generate Javascript onChange event-handler for the option element.  This allows
     * for template prepopulation based on option selection.  The queryCriteria parameter
     * specifies the query statement required to retrieve the template fields.
     *
     * @param  html           Html string to append to.
     * @param  queryCriteria  The criteria portion of the RepositoryManager's query category-criteria.
     */
    protected void generateOptionOnChangeHandler(HtmlElement html, String queryCriteria)
    {
        html.append(" onChange=\"").append(CALLALLRULES).append(" optionOnChangeHandler('");

        html.append(queryCriteria);

        html.append("')\"");
    }

    /**
     * gets the value of a field from the message.
     * Tries to get the fieldPath under the node xmlParentNode. If xmlParentNode
     * is null then xmlParentPath is used instead.
     * Also provides alias mappings of values.
     *
     * @param field Field
     * @param xmlParentPath The xml path of the parent node.
     * @param fieldPath - The path of the xml node relative to xmlParentPath
     * @param xmlParentNode -The xml parent node, used for repeating access. (May be null).
     * @return String
     */
    protected String getValue(Field field, String xmlParentPath, String fieldPath, Node xmlParentNode)
    {
        String value = "";

        String path = xmlParentPath + TagConstants.PATH_SEP + fieldPath;

        if (log.isDebugEnabled())
        {
            log.debug("getValue(): Checking for field [" + path + "] in the message object ...");
        }

        try {

           if ( xmlParentNode == null && message.exists(path))
           {
                value = message.getValue(path);

                if (log.isDebugEnabled())
                {
                    log.debug("getValue(): Field [" + path + "] exists with value [" + value + "].");
                }
           }
           else if ( message.exists(xmlParentNode, fieldPath)  )
              value = message.getValue(xmlParentNode, fieldPath);
           else if ( StringUtils.hasValue(field.getCustomValue( DEFAULT_VAL) ) )
              value = field.getCustomValue(DEFAULT_VAL);


          // get an alias value for a field
          // each alias has the field path as a prefix followed by the field value.

          value = aliasDescriptor.getAlias(pageContext.getRequest(), field.getFullXMLPath(), value, true);


        } catch (FrameworkException e) {
          // did not find node return empty
          log.debug("Node [" + path + "] does not exist.");
        }

        return TagUtils.performHTMLEncoding(value);

    }

   /**
     * This convenient method retrieves the value for the Custom Key "number-of-columns".
     * If the Custom Key is not defined, the method returns the default number of columns.
     *
     * @param field Field object.
     * @return value of the key 'number-of-columns', if configured with appropriate value; DEFAULT_CTRL_SIZE otherwise.
     */
    private int getNumberOfColumns(Field field)
    {
        int returnVal = DEFAULT_CTRL_SIZE;
        try
        {
            String numberOfColumns = field.getCustomValue(NO_OF_COLUMNS_VALUE);
            if (StringUtils.hasValue(numberOfColumns) && StringUtils.isDigits(numberOfColumns)) {
                returnVal = StringUtils.getInteger(numberOfColumns);
            }
        }
        catch (Exception e)
        {
            log.error("getNumberOfColumns(): Error in retrieving the value for customkey [" + NO_OF_COLUMNS_VALUE + "] Error: " + e.toString());
        }

        if (log.isDebugEnabled()) {
            log.debug("getNumberOfColumns(): Returning the value of customkey [" + NO_OF_COLUMNS_VALUE + "] as [" + returnVal + "].");
        }
        return returnVal;
    }

    /**
     * This convenient method determines whether a Field has any HTML attributes to be
     * passed in the final output. This checks for custom property "has-html-attributes".
     *
     * This custom property will only work
     *     - if field is of type "TEXT" or "TEXTAREA".
     *
     * This custom property will NOT work
     *     - if field is "HIDDEN".
     *     - if field is not of type "TEXT" or "TEXTAREA" i.e. range, date, drop down (select control), option button etc.
     *
     * @param  field Field object for which custom property presence would be checked.
     * @return true if custom property has value as "true" else "false".
     */
    private boolean hasHtmlAttributes (Field field)
    {
        String  hasHtmlAttributesCustomProperty = field.getCustomValue (HAS_HTML_ATTRIBUTES_CUSTOM_VALUE);
        boolean hasHtmlAttributesCustomValue    = StringUtils.getBoolean (hasHtmlAttributesCustomProperty, false);

        if (log.isDebugEnabled()) {
            log.debug("hasHtmlAttributes(): For field [" + field.getXMLPath() + "] value of custom property [" + hasHtmlAttributesCustomProperty + "] has boolean value [" + hasHtmlAttributesCustomValue + "] ");
        }
        
        return hasHtmlAttributesCustomValue;
    }

    /**
     * gets the HTML attributes of a field from the message XML.
     * Tries to get the fieldPath under the node xmlParentNode.
     * If xmlParentNode is null then xmlParentPath will be used.
     * Also provides alias mappings of values.
     * 
     * @param field Field object.
     * @param xmlParentPath The xml path of the parent node.
     * @return html attributes
     */
    protected String getHtmlAttributes (Field field, String xmlParentPath)
    {
        String fieldPath = field.getXMLPath ();
        
        String htmlAttributes = "";

        String path = xmlParentPath + TagConstants.PATH_SEP + fieldPath;

        if (log.isDebugEnabled())
        {
            log.debug("getHtmlAttributes(): Checking for field [" + path + "] in the message object ...");
        }

        try {

            if (message.exists(path))
            {
                htmlAttributes = message.getAttribute(path, HTML_ATTRIBUTES_VALUE );

                if (log.isDebugEnabled())
                {
                    log.debug("getHtmlAttributes(): Field [" + path + "] exists with html attributes [" + htmlAttributes + "].");
                }
            }
        } catch (FrameworkException e) {
            // did not find node return empty
            log.debug("Node [" + path + "] does not exist.");
        }

        if (StringUtils.hasValue ( htmlAttributes ))
        {
            htmlAttributes = TagUtils.performHTMLEncoding(htmlAttributes);
        }
        else htmlAttributes = "";

        if (log.isDebugEnabled())
        {
            log.debug("getHtmlAttributes(): html attributes obtained as [" + htmlAttributes + "].");
        }
        return htmlAttributes;

    }

    /**
     * This funtions checks if custom property "has-html-attributes"
     * is true for the field and the value of "html-attributes" is not
     * an empty string then the attributes are append in the HtmlElement
     * in format "<SPACE><htmlAttributes><SPACE>"
     *  
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     */
    private void appendHtmlAttributes ( Field field, HtmlElement elem, String xmlParentPath )
    {
        if (hasHtmlAttributes (field))
        {
            String htmlAttributes =  getHtmlAttributes (field, xmlParentPath);
            if (StringUtils.hasValue ( htmlAttributes ))
                elem.append(" " + htmlAttributes + " ");
        }
    }

    /**
     * Creates just the text area for a Field
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field(May be null).
     * @throws FrameworkException on error
     */
    protected void createAreaField(Field field, HtmlElement elem,
                                    String xmlParentPath, Node xmlParentNode)
        throws FrameworkException
    {
        log.debug("Creating Text Area Field");

        String xmlFieldPath = field.getXMLPath();
        String path = xmlParentPath + TagConstants.PATH_SEP + xmlFieldPath;

        String value = getValue(field, xmlParentPath, xmlFieldPath, xmlParentNode);

        // Read the Custom Key 'number-of-rows'.
        // The method returns DEFAULT_CTRL_LINES, if custom-key is not configured
        // Used to display the number of rows for this Text Area. Same is set as lineCount for this Html element.
        int numberOfRows = getNumberOfRows(field);
        int numberOfColumns = getNumberOfColumns(field);

        elem.setLineCount(numberOfRows);

        elem.append("<textarea");

        if (isProhibited (field))
           createDisabledCode(field, elem, value);
        else if ( entryValidation )
           createValidationCode(field, elem);

        appendHtmlAttributes (field, elem, xmlParentPath);
        elem.append("rows=\"").append(String.valueOf(numberOfRows) );
        elem.append("\" cols=\"").append(String.valueOf(numberOfColumns) ).append("\" name=\"");
        elem.append(ServletConstants.NF_FIELD_PREFIX);

        elem.append(getName(field,xmlParentPath) );

        elem.append("\" " + getRequiredFieldStyle (field) + ">");

        if (StringUtils.hasValue(value) && !blankOutValueForCopy(field))
        {
            elem.append( value );
        }

        elem.append("</textarea>");
    }

    /**
     * This method returns true if the field satisfy below conditions
     * @param field Field obj
     * @return boolean
     */
    private boolean isProhibited(Field field)
    {
        // if this message is read only or the field is prohibited disable this field, and turn off entry validation (not needed)
        // or if this field is not read only, then add entry validation support if turned on
       return  isReadOnly() ||
               field.getDataType().getUsage() == DataType.PROHIBITED ||
               disableFieldForCopy(field)       ||
               disableFieldForEdit(field)       ||
               disableFieldForActions(field)    ||
               disableIfHasHostId(field)        ||
               disableFieldForConvert(field);
    }

    /**
     * This method returns true if the field satisfy below conditions.
     * Also, it is assumed that this would be called for DataType.TYPE_ENUMERATED data type
     * @param field Field object
     * @return boolean
     */
    private boolean isEditable (Field field)
    {
        boolean editable = false;

        if (field != null)
        {
            editable = StringUtils.getBoolean(field.getCustomValue(EDITABLE_CUSTOM_VALUE), false);
            boolean editableSD = StringUtils.getBoolean(field.getCustomValue(EDITABLE_IF_NO_SD_CUSTOM_VALUE), false);
            if (editableSD)
            {
                String subDomain = TagUtils.getSubDomain(pageContext.getSession());
                editable = editable || !StringUtils.hasValue(subDomain);
            }
        }
        
        return  editable;
    }

    /**
   * Sets the correct date format on a date field.
   * this will be obtained from the locale information if not specified, on
   * the data type. If the dtype is not a date field then it will
   * not be modified.
   * @param dtype The datatype of a a field to check.
   */
  protected void setDateFormat(DataType dtype)
  {
    String format = dtype.getFormat();
    if (!StringUtils.hasValue(format) ) {
      if ( dtype.getType() == DataType.TYPE_DATE_TIME || dtype.getType() == DataType.TYPE_DATE_OPTIONAL_TIME)
        format = NFLocale.getDateTimeFormat();
      else if ( dtype.getType() == DataType.TYPE_DATE )
        format = NFLocale.getDateFormat();
      else if ( dtype.getType() == DataType.TYPE_TIME )
        format = NFLocale.getTimeFormat();
     else if( dtype.getType() == DataType.TYPE_REL_DATE_OPTIONAL_TIME)
           format = NFLocale.getDateTimeFormat();
      dtype.setFormat(format);
    }

  }

    /**
     * Generated an jscript code for a onBlur event to perform validation
     * of a field. Will generated html in the form of " onBlur='...' ".
     * This internally calls over-rided <b>createValidationCode</b> method
     *
     * @param field - The field to generate validation code for
     * @param elem the HtmlElement representing this field.
     */
    protected void createValidationCode(Field field, HtmlElement elem)
    {
        DataType typeInfo = field.getDataType();
        String format = typeInfo.getFormat();

        elem.append(" onBlur=\"").append(CALLALLRULES).append("entryValidation(this, new ValidateInfo('");
        elem.append(field.getId ());
        elem.append("', '");
        elem.append(typeInfo.getTypeName());
        elem.append("', ");
        elem.append(typeInfo.getMinLen ());
        elem.append(", '");
        if (StringUtils.hasValue(format)) {
          // if this is a text field then regular expressions are used
          // so add another escape for the js code
          if ( typeInfo.getType() == DataType.TYPE_TEXT)
            elem.append(StringUtils.replaceSubstrings(format, "\\", "\\\\"));
          else
            elem.append(format);
        }

        elem.append("', '");

        if (StringUtils.hasValue(field.getCustomValue (VALIDATION_ERR_MSG_CUSTOM_VALUE))) {
            elem.append(TagUtils.filterSpecialChars(field.getCustomValue (VALIDATION_ERR_MSG_CUSTOM_VALUE)));
        }

        elem.append("') );\" ");

    }

    /**
     * Making the Html Elements readonly in case of creating the disabled code
     *
     * <b>NOTE: createValidationCode should not be called if this method is used.</b>
     *
     * @param f field type
     * @param value - The value of the field to to be disabled
     * @param elem the HtmlElement representing this field.
     */
    protected void createDisabledCode (Field f, HtmlElement elem, String value)
    {
        int dType = f.getDataType().getType();
        boolean createDisCode = ((dType == DataType.TYPE_ENUMERATED) ||
                                (dType == DataType.TYPE_RADIO_BUTTON) ||
                                (dType == DataType.TYPE_CHECK_BOX));

        // If the field is of type multiple select then
        // then field would not be disabled.
        if (createDisCode && dType == DataType.TYPE_ENUMERATED)
            createDisCode = !ServletUtils.getBoolean(f.getCustomValue(MULTIPLE_SELECT), false);

        createDisabledCode ( createDisCode, elem, value);
    }

    /**
     * Making the Html Elements readonly in case of creating the disabled code
     *
     * <b>NOTE: createValidationCode should not be called if this method is used.</b>
     *
     * @param value - The value of the field to to be disabled
     * @param elem the HtmlElement representing this field.
     */
    protected void createDisabledCode(HtmlElement elem, String value)
    {
        createDisabledCode ( false, elem, value);
    }

    /**
     * Making the Html Elements readonly in case of creating the disabled code
     * and disabling the drop downs
     *
     * <b>NOTE: createValidationCode should not be called if this method is used.</b>
     *
     * @param createDisabled to create readonly or onBlur javascript.
     * @param value - The value of the field to be disabled
     * @param elem the HtmlElement representing this field.
     */
    protected void createDisabledCode(boolean createDisabled, HtmlElement elem, String value)
    {
        if (createDisabled)
        {
            elem.append(" disabled class=\"").append(TagConstants.CLASS_DISABLED_INPUT).append("\" ");
        }
        else
        {
            elem.append(" readonly class=\"").append(TagConstants.CLASS_DISABLED_INPUT).append("\" ");
        }
    }

    /**
     * Creates the input control for a hidden field
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field(May be null).
     * @throws FrameworkException on error
     */
    protected void createHiddenField(Field field, HtmlElement elem,
                                    String xmlParentPath, Node xmlParentNode)
        throws  FrameworkException
    {
       log.debug("Creating hidden Input Field");

       String xmlFieldPath = field.getXMLPath();
       String value = getValue(field, xmlParentPath, xmlFieldPath, xmlParentNode);


        // create the input field
        elem.append("<input type=\"hidden\"");

        elem.append(" name=\"").append(ServletConstants.NF_FIELD_PREFIX);

        elem.append(getName(field, xmlParentPath) );


        value = callCVsForHiddenField (field, getName(field, xmlParentPath), value);

        // include the value, if we have one
        if (StringUtils.hasValue(value))
        {
            elem.append("\" value=\"").append(value);
        }

        elem.append("\"/>");

        hiddenFields.add(elem);
    }

    /**
     * Creates tabular format
     *
     * @param grp The part to create the HTML for
     * @param holder  The HtmlContainer which holds the html for future processing
     * @throws FrameworkException on error
     */
    protected void createTabularFieldGrp (FieldGroup grp, HtmlContainer holder) throws  FrameworkException
    {
        if (log.isDebugEnabled())
            log.debug("createTabularFieldGrp (): Creating tabular control ...");

        String [] rowLabels = StringUtils.getArray ( grp.getCustomValue ( TABULAR_LABELS_CUSTOM_VALUE ), COMMA);
        String [] columnHeaders = StringUtils.getArray ( grp.getCustomValue ( TABULAR_HEADERS_CUSTOM_VALUE ), COMMA);
        int totalCols = columnHeaders.length;
        // default number of columns i.e. 2 will be
        // considered if columnHeaders has no length 
        if (totalCols == 0) totalCols = 2;

        int partCount = grp.getChildren().size();

        String xmlPath = parentTag.updateXmlPathIndex(grp.getFullXMLPath());

        Node xmlPathNode = null;

           // the first time this may be null
         if (message.exists(xmlPath) )
            xmlPathNode = message.getNode(xmlPath);

        // <tr> already started from createFieldGrp method

        /*
        * Now we are creating rows of the tabular field group
        * where each <tr> will contain a <table>.
        * If there is are three columns then first one
        * will be used as a label with 40% width 
        * and rest will be used remaining.
        * */
        holder.append("<td style=\"padding-left: 0px\"><table width=\"100%\" border=\"0\"><tr>");
        // here 60% width of the table will be used for all columns except label
        int fieldColsWidth = 60 / (totalCols-1);
        // here creating the number of columns
        for (int num = 0; num < totalCols; num++)
        {
            // as column headers are optional
            String header = ""; 
            String widthStyle = num == 0 ? "style=\"width=40%\"" : "style=\"width=" + fieldColsWidth + "%\"";
            if (columnHeaders.length > num && StringUtils.hasValue(columnHeaders [num]))
                header = columnHeaders [num];

            if (log.isDebugEnabled())
                log.debug("createTabularFieldGrp (): Creating Header [" + header + "]");

            holder.append("<td ").append(widthStyle).append(" class=\"").append(TagConstants.CLASS_FIELDGRP_HEADING + "\">").append(header).append("</td>");
        }
        holder.append("</tr></table></td></tr><tr>");

        int lblsLen = rowLabels.length;
        // controls per line excluding row label
        int cols = totalCols - 1;
        int idx = 0;

        if (log.isDebugEnabled())
            log.debug("createTabularFieldGrp (): Total columns without label to be build with fields ["+cols+"], total labels ["+lblsLen+"] and child count ["+partCount+"]");

        HtmlContainer tempHolder = new HtmlContainer ();
        for (int j = 0; j < lblsLen; j++)
        {
            if (log.isDebugEnabled())
                log.debug("createTabularFieldGrp (): Creating label [" + rowLabels[j] + "]");
            
            HtmlElement fieldHtml = new HtmlElement ();
            // createLabel method will create <table><tr><td> and </td> but does not close <tr> and <table>
            // that will be done here at end
            createLabel ((Field) grp.getChild(idx), fieldHtml, rowLabels[j]);
            // enclosing LABEL in <td></td>
            fieldHtml.prepend("<td>");
            tempHolder.append("</tr><tr>");
            tempHolder.append (fieldHtml.getHTML());

            /*
                This loop will draw the controls in table format.
                The controls will be displayed from TOP LEFT to BOTTOM RIGHT i.e
                if per row two controls are to be displayed then the first two controls
                will be displayed in 1st row, next two in 2nd row and so on.
            */
            for (int k = 0; k < cols; k++)
            {
                if (log.isDebugEnabled())
                    log.debug("createTabularFieldGrp (): Creating Field ["+idx+"]");

                Field childPart = (Field) grp.getChild(idx);
                
                if (log.isDebugEnabled())
                    log.debug("createTabularFieldGrp (): Creating Field with Id ["+childPart.getId ()+"]");
                idx++;

                // add the field
                fieldHtml = new HtmlElement ();
                createFieldFormElement (childPart, fieldHtml, xmlPath, xmlPathNode);

                // this <td> will be the part of <table> created by createLabel method
                // wrap the element in a table data cell
                fieldHtml.prepend("<td width=\""+fieldColsWidth+"%\">");
                fieldHtml.append("</td>");
                
                tempHolder.append (fieldHtml.getHTML());
            }
            tempHolder.append ("</tr></table>"); // <tr> and <table> from createLabel method ends here
        }
        holder.append(tempHolder.getHTML ());
        holder.append("</tr>"); // <tr> from createFieldGrp method ends here
    }

    /**
     * Creates multiple controls in a single row
     * and label of field group is used to display.
     *
     * @param grp The part to create the HTML for
     * @param holder  The HtmlContainer which holds the html for future processing
     * @throws FrameworkException on error
     */
    protected void createMultiControlFieldGrp (FieldGroup grp, HtmlContainer holder) throws  FrameworkException
    {
        if (log.isDebugEnabled())
            log.debug("createMultiControlFieldGrp (): Creating multi control field group ...");

        String [] fieldSeparators = StringUtils.getArray ( grp.getCustomValue ( MULTI_CONTROL_SEPARATORS_CUSTOM_VALUE ), COMMA);

        int partCount = grp.getChildren().size();

        holder.append("<td style=\"padding-left: 0px\">"); // <tr> already started from createFieldGrp method
        HtmlElement tempElem = new HtmlElement();

        // Label of the group will decide by the first child of the group
        Field firstField = (Field) grp.getChild (0);
        // createLabel method will create <table><tr><td> and </td> but does not close <tr> and <table>
        // that will be done here at end
        createLabel (firstField, tempElem, TagUtils.getFieldGroupLabel(grp, fieldLabelMode));
        holder.append (tempElem.getHTML());

        // this <td> will be the part of <table> created by createLabel method
        // <table> for inner multiple elements starts here

        boolean showBorder = StringUtils.getBoolean(grp.getCustomValue("show-border"), true);
        holder.append("<td style=\"padding-left: 0px\"><table");
        // this is to align the fields when there is no border with other fields
        if(!showBorder)
            holder.append(" cellpadding=\"0\" cellspacing=\"0\" ");
        holder.append("><tr>");
        

        String css = "inLabels";
        if (isProhibited (firstField))
            css = "inLabelsProhibited";
        HtmlContainer tempHolder = new HtmlContainer();
        // Only fields are supposed to be present inside it.
        for (int num = 0; num < partCount; num++) 
        {
            if (log.isDebugEnabled())
                log.debug("createMultiControlFieldGrp (): Creating Field [" + num + "]");

            Field childPart = (Field) grp.getChild (num);

            if (log.isDebugEnabled())
                log.debug("createMultiControlFieldGrp (): Creating Field with Id [" + childPart.getId () + "]");

            String path =  parentTag.updateXmlPathIndex(childPart.getParent().getFullXMLPath());
            HtmlElement newElem = new HtmlElement();
            createFieldFormElement (childPart, newElem, path, null);
            newElem.prepend("<td>");
            newElem.append("</td>");

            tempHolder.append (newElem.getHTML());
            // separator should not display after last elem i.e. partCount - 1
            String separator = "";
            if (num < partCount-1)
            {
                if (fieldSeparators.length > num) separator = fieldSeparators[num];
                tempHolder.append("<td class=\"").append(css).append("\">").append(separator).append("</td>");
            }
        }

        holder.append (tempHolder.getHTML());
        holder.append ("</tr></table>"); // <tr> and <table> for inner multiple elements ends here
        holder.append("</td></tr></table>"); // <td> <tr> and <table> from createLabel method ends here
        holder.append("</td></tr>"); // <td> and <tr> from createFieldGrp method ends here
    }

    /**
     * Creates just the radio control for a Field
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field(May be null).
     * @throws FrameworkException on error
     */
    protected void createRadioField (Field field, HtmlElement elem, String xmlParentPath, Node xmlParentNode) throws  FrameworkException
    {
        DataType dType = field.getDataType ();
        OptionSource optionSource = dType.getOptionSource();
        String[] optionValues        = null;
        String[] optionDisplayValues = null;

        if (optionSource == null)
        {
            throw new FrameworkException ( "Error: Options values are missing to create radio buttons." );            
        }

        if (log.isDebugEnabled())
            log.debug("createRadioField (): Getting option values from meta Options section ...");

        optionValues        = optionSource.getOptionValues();
        optionDisplayValues = optionSource.getDisplayValues();

        if (optionValues == null )
        {
            throw new FrameworkException ( "Error: Options values are NULL." );
        }

        if (optionValues.length == 0 )
        {
            throw new FrameworkException ( "Error: Either options values are not present or data incorrect." );            
        }

        int totalOptions = optionValues.length;
        boolean prohibited = false;

        if (isProhibited (field))
            prohibited = true;

        for (int num = 0; num < totalOptions; num++)
        {
            String optionValue = optionValues[num];
            String optionDisplayValue = "";
            if (optionDisplayValues != null)
                optionDisplayValue = optionDisplayValues[num];
            
            String name = ServletConstants.NF_FIELD_PREFIX + field.getFullName ();
            String value = getValue(field, xmlParentPath, field.getXMLPath(), xmlParentNode);

            String checked = value.equals ( optionValue ) ? "checked" : "";
            elem.append("<input class=\"radio\" type=\"radio\" value=\"" + optionValue + "\" onclick=\"").append(CALLALLRULES).append("\" name=\"" + name + "\" " + checked + " ");

            if ( prohibited )
                createDisabledCode(field, elem, value);

            elem.append(getRequiredFieldStyle (field) + "/>");
            elem.append(optionDisplayValue);
        }
    }

    /**
     * Creates just the checkbox control for a Field
     * Right now support to create a single checkbox is been only provided.
     * But the user has to provide two option source and the second value
     * will be used if user has not checked the checkbox.
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field(May be null).
     * @throws FrameworkException on error
     */
    protected void createCheckboxField (Field field, HtmlElement elem, String xmlParentPath, Node xmlParentNode) throws  FrameworkException
    {
        OptionSource optionSource = field.getDataType().getOptionSource();
        String[] optionValues        = null;
        String[] optionDisplayValues = null;

        if (optionSource == null)
        {
            throw new FrameworkException ( "Error: Options values are missing to create checkbox buttons." );
        }

        if (log.isDebugEnabled())
            log.debug("createCheckboxField (): Getting option values from meta Options section ...");

        optionValues        = optionSource.getOptionValues();
        optionDisplayValues = optionSource.getDisplayValues();

        if (optionValues == null)
        {
            throw new FrameworkException ( "Error: Options values are NULL." );
        }

        if (optionValues.length == 0 || optionValues.length > 2)
        {
            throw new FrameworkException ( "Error: Either options values are not present or data incorrect." );
        }

        //int totalOptions = optionValues.length;
        // Commenting loop as we are only creating a single checkbox 
        int num = 0;

        //for (int num = 0; num < totalOptions; num++)
        {
            String optionValue = optionValues[num];
            String optionDisplayValue = "";
            if (optionDisplayValues != null)
                optionDisplayValue = optionDisplayValues[num];
            
            String hiddenName = ServletConstants.NF_FIELD_PREFIX + getName (field, xmlParentPath);
            String name = getName (field, xmlParentPath) + "_checkbox";
            String value = getValue(field, xmlParentPath, field.getXMLPath(), xmlParentNode);
            String hiddenValue = value.equals ( optionValue ) ? optionValue : optionValues[num+1];
            String checked = optionValue.equals ( hiddenValue ) ? "checked" : "";

            /**
             * Creating hidden field with the control name which will be used to hold the value of checkbox till it is checked.
             */
            elem.append("<input type=\"hidden\" value=\"" + hiddenValue + "\" name=\"" + hiddenName + "\" > ");
            elem.append("<input class=\"checkbox\" type=\"checkbox\" value=\"").append(optionValue).append("\" OnClick=\"").append(CALLALLRULES).append("handleCheckbox (this, '" + hiddenName + "', '" + optionValues[num+1] + "')\" name=\"" + name + "\" " + checked + " ");

            boolean prohibited = false;

            if (isProhibited (field))
                prohibited = true;

            if ( prohibited )
                createDisabledCode(field, elem, value);

            elem.append(getRequiredFieldStyle (field) + "/>");
            elem.append(optionDisplayValue);
        }
    }

    /**
     * Creates SPID field as one input control for a Field
     * one image displayed next to input control and one readonly text box
     * to display the SP Name, this readonly field will not be submitted.
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field(May be null).
     * @throws FrameworkException throws if error comes.
     */
    protected void createSPIDField(Field field, HtmlElement elem,
                                    String xmlParentPath, Node xmlParentNode) throws FrameworkException
    {
        if (log.isDebugEnabled())
            log.debug("Creating a SPID Lookup Field...");

        /********************** Start: normal Lookup parameters *************************/

        // whether wrong SPID values in the field even if matching is not found in the result-set obtained using query-criteria.
        // default is false, which would clear any mismatching SPID value with result-set.
        boolean editable = StringUtils.getBoolean(field.getCustomValue(EDITABLE_CUSTOM_VALUE), false);

        // Name of the field (SPID is the Datatype)
        String name = ServletConstants.NF_FIELD_PREFIX + getName(field, xmlParentPath);

        // Used for identifying unique SPID field on same page
        String fieldID = name.substring(name.lastIndexOf(".")+1 );

        // Max length of the field, defaulted to 4 in case not given in Data Type
        String maxLen = getMaxLen(field);
        if(!StringUtils.hasValue(maxLen))
            maxLen = String.valueOf(MAX_SPID_CTRL_SIZE);

        // to pre-populate the SPID & SPName value, if any. e.g. while edit/clone/printable-view
        String xmlFieldPath = field.getXMLPath();
        String prePopulateSPID = getValue(field, xmlParentPath, xmlFieldPath, xmlParentNode);

        boolean prohibited = false;

        if (isProhibited (field))
            prohibited = true;
        /********************** End: normal Lookup parameters *************************/

        // check whether Lookup is enabled for the current CUSTOMER
        // If not needed then DO NOT execute any of the logic to fetch, format, cache SPID data
        boolean islookupEnabledForCurrCust = isLookupEnabledForCustomer();
        boolean isSPIDLookupDataAvailable = false;

        /********************** Start: Lookup specific parameters *************************/
        String queryCriteria    = field.getCustomValue(QUERY_CRITERIA_CUSTOM_VALUE);
        String queryCriteriaTokens    = field.getCustomValue(QUERY_CRITERIA_TOKENS_CUSTOM_VALUE);
        String[][] queryResult  = null;

        String latestUpdatedTimeQueryCriteria = null;
        String[] optionCol_0 = null;
        String[] optionCol_1 = null;

        // Configurable lookup popup window parameters
        String fieldNameForSPName, spidNamePair=null, nameSPIDPair=null, lookupWindowTitle=null, cachedSPData=null,
                headingCol_0=null, headingCol_1=null, sizeFilterTextboxCol_0=null, sizeFilterTextboxCol_1=null, strIsSearchField=null, lookupKey=null;
        boolean isSearchField = false;
        /********************** End: Lookup specific parameters *************************/

        // Name of the second input box for lookup field (description, which would be disabled/readonly on the GUI)
        fieldNameForSPName = name + "SPName";

        if (islookupEnabledForCurrCust)
        {
            // if lookup is enabled for the customer
            // query-criteria & latest_updated_time_query_criteria are mandatory parameters
            if ( !StringUtils.hasValue(queryCriteria) )
            {
                log.error( "Error: missing/empty mandatory configuration parameter " +
                        "[" + QUERY_CRITERIA_CUSTOM_VALUE + "] for SPID field [" + field.getFullName() + "]" );
            }
            else
            {
                // Tokens to be replaced in field specific Lookup Data with in Javascript code containing
                // lookupDataSortedBySPIDWithNoFltr, lookupDataSortedBySPNameWithNoFltr, lookupDataSortedBySPIDDescWithNoFltr, lookupDataSortedBySPNameDescWithNoFltr
                spidNamePair = fieldID + "SrtSPID";
                nameSPIDPair = fieldID + "SrtSPName";

                if (log.isDebugEnabled())
                    log.debug("createSPIDField(): Getting option values from meta Options section ...");

                OptionSource optionSource = field.getDataType().getOptionSource();

                if (optionSource == null)
                {
                    log.error("createSPIDField(): Error: missing/empty mandatory configuration parameter " +
                                "[" + SPID_LATEST_UPDATED_TIME_QUERY_CRITERA + "] for SPID field [" + field.getFullName() + "]");
                }
                else
                {
                    optionCol_0 = optionSource.getOptionValues();
                    optionCol_1 = optionSource.getDisplayValues();

                    if (optionCol_0 == null || optionCol_1 == null )
                    {
                        log.error("createSPIDField(): Error: missing/empty mandatory configuration parameter " +
                                    "[" + SPID_LATEST_UPDATED_TIME_QUERY_CRITERA + "] for SPID field [" + field.getFullName() + "]");
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                            log.debug("createSPIDField(): Getting SPID configuration parameters from available Option Source...");

                        HashMap optionValueMap = getOptionValueMap(optionCol_0, optionCol_1);

                        latestUpdatedTimeQueryCriteria  = (String) optionValueMap.get(SPID_LATEST_UPDATED_TIME_QUERY_CRITERA);

                        // query-criteria & latest_updated_time_query_criteria are mandatory parameters
                        // if lookup is enabled for the customer
                        if ( !StringUtils.hasValue(latestUpdatedTimeQueryCriteria) )
                        {
                            log.error( "missing/empty mandatory configuration parameter " +
                                    "[" + SPID_LATEST_UPDATED_TIME_QUERY_CRITERA + "] for SPID field [" + field.getFullName() + "]" );
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                                log.debug("createSPIDField(): Obtained " + SPID_LATEST_UPDATED_TIME_QUERY_CRITERA + " = " + latestUpdatedTimeQueryCriteria
                                    + ". Getting remaining SPID configuration parameters from available Option Source...");

                            lookupWindowTitle               = (String) optionValueMap.get(SPID_LOOKUP_WIN_TITLE);

                            headingCol_0                    = (String) optionValueMap.get(SPID_HEADING_COL_PREFIX + SPID_COL_ONE_IDX);

                            headingCol_1                    = (String) optionValueMap.get(SPID_HEADING_COL_PREFIX + SPID_COL_TWO_IDX);

                            sizeFilterTextboxCol_0          = (String) optionValueMap.get(SPID_SIZE_FILTER_TEXTBOX_COL_PREFIX + SPID_COL_ONE_IDX);

                            sizeFilterTextboxCol_1          = (String) optionValueMap.get(SPID_SIZE_FILTER_TEXTBOX_COL_PREFIX + SPID_COL_TWO_IDX);

                            strIsSearchField                = (String) optionValueMap.get(SPID_IS_SEARCH_FIELD);


                            // Default Lookup window title would be "<Field Display Name> + Lookup"
                            if ( !StringUtils.hasValue(lookupWindowTitle) )
                                lookupWindowTitle = field.getDisplayName() + " Lookup";

                            // Default first Column heading in the lookup window would be "<Field Display Name>"
                            if ( !StringUtils.hasValue(headingCol_0) )
                                headingCol_0 = field.getDisplayName();

                            // Default size of first lookup Column would be Maximum Length of the field
                            if ( ! (StringUtils.hasValue(sizeFilterTextboxCol_0) && StringUtils.isDigits(sizeFilterTextboxCol_0)) )
                                sizeFilterTextboxCol_0 = maxLen;

                            // Default second Column heading in the lookup window would be "<Field Display Name> + Description"
                            if ( !StringUtils.hasValue(headingCol_1) )
                                headingCol_1 = field.getDisplayName() + " Description";

                            // Default size of second lookup Column would be twice the Maximum Length of the field
                            if ( ! (StringUtils.hasValue(sizeFilterTextboxCol_1) && StringUtils.isDigits(sizeFilterTextboxCol_1)) )
                                sizeFilterTextboxCol_1 = String.valueOf( 2 * StringUtils.getInteger(maxLen) );

                            isSearchField = StringUtils.getBoolean(strIsSearchField, false);

                            if (log.isDebugEnabled())
                            {
                                log.debug("SPID property: latest_updated_time_query_criteria = " + latestUpdatedTimeQueryCriteria);
                                log.debug("SPID property: lookup_window_title = " + lookupWindowTitle);
                                log.debug("SPID property: headingCol_0 = " + headingCol_0);
                                log.debug("SPID property: headingCol_1 = " + headingCol_1);
                                log.debug("SPID property: sizeFilterTextboxCol_0 = " + sizeFilterTextboxCol_0);
                                log.debug("SPID property: sizeFilterTextboxCol_1 = " + sizeFilterTextboxCol_1);
                                log.debug("SPID property: is_search_field = " + isSearchField);
                            }

                            lookupKey = TagUtils.getLookupFieldDataKey(queryCriteria, latestUpdatedTimeQueryCriteria, log);

                            if (log.isDebugEnabled())
                                log.debug("SPID lookup data key: " + lookupKey);

                            cachedSPData = TagUtils.getSPDataFromCache( lookupKey, latestUpdatedTimeQueryCriteria, pageContext, queryCriteriaTokens );

                            if ( !StringUtils.hasValue(cachedSPData) )
                            {
                                if (log.isDebugEnabled())
                                    log.debug("SPID lookup data not found in cache with key [" + lookupKey + "]. Trying to create same using query-criteria [" + queryCriteria + "]");

                                if (StringUtils.hasValue(queryCriteria))
                                {
                                    queryResult = TagUtils.queryOptionList(queryCriteria, message.getDocument(), pageContext, queryCriteriaTokens);
                                }
                                else
                                {
                                    log.error("BodyTag.createSPIDField: No queryCriteria specified for SPID field ["+field.getFullName()+"].");
                                }

                                if (queryResult == null)
                                {
                                    log.error("BodyTag.createSPIDField: No results returned from queryCriteria for SPID field ["+field.getFullName()+"].");
                                }
                                else
                                {
                                    cachedSPData = TagUtils.setSPDataInCache( lookupKey, queryResult );
                                }
                            }

                            if (StringUtils.hasValue(cachedSPData))
                            {
                                cachedSPData = StringUtils.replaceSubstrings( cachedSPData, TagUtils.SORTED_BY_SPID, spidNamePair);
                                cachedSPData = StringUtils.replaceSubstrings( cachedSPData, TagUtils.SORTED_BY_SPNAME, nameSPIDPair);
                                elem.append( cachedSPData );

                                // the lookup is enabled and caching Data is also available
                                isSPIDLookupDataAvailable = true;
                            }

                            if (log.isDebugEnabled())
                                log.debug("SPID lookup data for field [" + field.getFullName() + "] = " + cachedSPData);
                        }
                    }
                }
            }
        }

        elem.append("<input type=\"text\"");

        appendHtmlAttributes (field, elem, xmlParentPath);

        if ( prohibited )
            createDisabledCode(field, elem, prePopulateSPID);

        if(isSearchField)
        {
            elem.append(" size=\"10\"");
        }
        else
        {
            elem.append(" size=\"").append(getSize(field));
            elem.append("\" maxlength=\"").append(maxLen).append("\"");
        }

        elem.append(" onBlur=\"");

        if( isSPIDLookupDataAvailable )
            elem.append("populateSPName(" + spidNamePair).append(", this, document.").append(getFormName()).append("['").append(fieldNameForSPName).append("']," + editable + ", "+maxLen+");");

        elem.append(" callAllRules (this);\" ");

        elem.append(getRequiredFieldStyle (field)).append(" ");

        elem.append(" name=\"").append( name ).append("\" value=\"");

        if ( StringUtils.hasValue( prePopulateSPID ) )
        {
            elem.append(prePopulateSPID);
        }

        elem.append("\"/>");

        if( isSPIDLookupDataAvailable )
        {
            elem.append("&nbsp;<A onMouseOver=\"return displayStatus('Lookup " + headingCol_0 + "');\""
                              +" onMouseOut=\"return displayStatus('');\""
                              +" HREF=\"javascript:doNothing()\""
                              +" onClick=\"showSPLookup(document."
                              + getFormName() +"['" + name +"'],"
                              + getFormName() +"['" + fieldNameForSPName +"']"
                              + ", '" + lookupWindowTitle + "'"
                              + ", '" + headingCol_0 + "'"
                              + ", '" + headingCol_1 + "'"
                              + ", " + sizeFilterTextboxCol_0 + ""
                              + ", " + sizeFilterTextboxCol_1 + ""
                              + ", " + maxLen + ""
                              + ");\">"
                              +"<IMG SRC=\"" + TagUtils.getWebAppContextPath ( pageContext ) + "/images/Lookup.gif\" BORDER=0>"
                           +"</A>");
        }
        else
        {
            elem.append("&nbsp;<A onMouseOver=\"return displayStatus('No " + headingCol_0 + " lookup data to display');\""
                              +" onMouseOut=\"return displayStatus('');\""
                              +" HREF=\"javascript:doNothing()\""
                              +" onClick=\"javascript:doNothing();\">"
                              +"<IMG SRC=\"" + TagUtils.getWebAppContextPath ( pageContext ) + "/images/Lookup.gif\" BORDER=0>"
                           +"</A>");
        }
        elem.append("<input type=\"text\" tabindex=\"-1\" id=\"").append(fieldNameForSPName)
                     .append("\" size=\"20\" disabled name=\"").append(fieldNameForSPName).append("\"/>");

        if ( isSPIDLookupDataAvailable )

        {
            // JS call to populate SP name for the first time when page is displayed.
            elem.append("<script language='javascript'>");
            elem.append("populateSPName(").append(spidNamePair).append(", document.").append(getFormName()).append("['").append(name).append("'],").append(" document.").append(getFormName()).append("['").append(fieldNameForSPName).append("'], true, "+maxLen+");");
            elem.append("</script>");
        }
    }

    private boolean isLookupEnabledForCustomer() throws FrameworkException
    {
        // To stop script generation for customers (confirgured) to avoid GUI slowness
        // This code has to remove once the solution of slowness is provided
        // here we will take all the customers from persistent property table
        // for whom disabling of SPID control client side validation is desired
        // for this:
        // key: WEBAPPS
        // PropertyType: COMMON
        // PropertyName: EXCLUDE_SPID_FOR_CUSTOMER_<N>, where N is number 0, 1, 2 ...
        // PropertyValue: CUSTOMERID, like (BHN_PROD)

        boolean lookupEnabledForCustomer = true;
        String spidDisabledCID;
        String cid = CustomerContext.getInstance().getCustomerID();
        String SPID_DATA_DISABLED_FOR_CID_PROP = "EXCLUDE_SPID_FOR_CUSTOMER";

        for ( int Ix = 0;  true;  Ix ++ )
        {
            spidDisabledCID = PropUtils.getPropertyValue(props, PersistentProperty.getPropNameIteration ( SPID_DATA_DISABLED_FOR_CID_PROP, Ix ) );

            //stop when no more properties are specified
            if ( !StringUtils.hasValue( spidDisabledCID ) )
                break;

            if(cid.equals(spidDisabledCID))
            {
                 lookupEnabledForCustomer = false;
                 break;
            }
        }

        return lookupEnabledForCustomer;
    }

    protected HashMap getOptionValueMap( String[] optionValues, String[] optionDisplayValues )
    {
        HashMap retMap = new HashMap();

        if (log.isDebugEnabled())
            log.debug("getOptionSources(): creating option value map for given options/values arrays.");

        if (optionValues != null && optionDisplayValues != null && optionValues.length == optionDisplayValues.length)
        {
            for (int i =0; i < optionValues.length; i++)
            {
                if ( StringUtils.hasValue(optionValues[i]) )
                    retMap.put(optionValues[i], optionDisplayValues[i]);
            }
        }

        return retMap;
    }

    /**
     * Creates just the input control for a Field
     *
     * @param field The part to create the HTML for
     * @param elem  The element which holds the html for future processing
     * @param xmlParentPath The current full xml path for the parent of this field
     * @param xmlParentNode The parent node of this field(May be null).
     * @throws FrameworkException throws if error comes
     */
    protected void createInputField(Field field, HtmlElement elem, String xmlParentPath, Node xmlParentNode) throws  FrameworkException
    {
        DataType dType = field.getDataType();

        if ( (dType.isInstance(TagConstants.DATA_TYPE_RANGE)) ||
             (dType.isInstance(TagConstants.DATA_TYPE_RANGE_OPTIONAL)) ||
             (dType.isInstance(TagConstants.DATA_TYPE_REL_RANGE_OPTIONAL)) )

        {
            createRangeFields(field, elem, xmlParentPath);
        }
        else
        {
            String xmlFieldPath = field.getXMLPath();


            String value = getValue(field, xmlParentPath, xmlFieldPath, xmlParentNode);

            // value is being checked against empty after taking the value
            // as for receive side the supplier value from the bean is
            // not coming properly.
            if (!(StringUtils.hasValue(value)))
            {
                value = getHeaderNodeValue(field);
            }
            if (!(StringUtils.hasValue(value)))
            {
                value = getBodyNodeValue(field);
            }

            // set the correct format if this is a date field
            setDateFormat(dType);

            /**
             *This condition tests to create the hidden control and
             * apply the Label 'Auto-Generated' instead of creating the control
            */
            if (TagUtils.isAutoIncremented(field, pageContext, domainProperties) && (!isReadOnly()))
            {
                elem = createHiddenControl(field, elem, xmlParentPath, value );
            }
            else
            {
                elem.append("<input type=\"text\"");

                boolean prohibited = false;
                boolean isTooltipEnabled = ServletUtils.getBoolean(field.getCustomValue(IS_TOOLTIP_ENABLED), false);

                if ( isProhibited (field))
                    prohibited = true;

                if ( prohibited )
                {
                    createDisabledCode(field, elem, value);
                    if (isTooltipEnabled)
                    {
                        elem.append(" title=\"").append(value).append("\" ");
                    }
                }
                else if ( entryValidation )
                    createValidationCode(field, elem);

                appendHtmlAttributes (field, elem, xmlParentPath);
                
                elem.append("size=\"").append(getSize(field));

                elem.append("\" maxlength=\"").append(getMaxLen(field));

                String name = ServletConstants.NF_FIELD_PREFIX + getName(field, xmlParentPath) ;

                elem.append("\" name=\"").append( name ).append("\" value=\"");

                // If there is a value and the field doesn't need to be blanked out
                // in the case of a copy process, display the value.

               String fieldName = ServletConstants.NF_FIELD_PREFIX + getName(field, xmlParentPath);

                value = callCVsForInputField (field, fieldName, value);

                if (!blankOutValueForCopy(field))
                {
                    String queryCriteria = field.getCustomValue(QUERY_CRITERIA_CUSTOM_VALUE);
                    String queryCriteriaTokens = field.getCustomValue(QUERY_CRITERIA_TOKENS_CUSTOM_VALUE);

                    if ( StringUtils.hasValue(queryCriteria)) // no need to convert date if populated via query
                    {
                        elem.append(TagUtils.getQuerySingleRowVal(queryCriteria, pageContext, queryCriteriaTokens));
                    }
                    else if (StringUtils.hasValue(value))
                    {
                        elem.append(value);
                    }
                }

                if (!prohibited && isTooltipEnabled)
                {
                    elem.append("\" onmouseover=\"javascript:this.title=this.value");
                }

/*              need to check what is required with this custom value for.
                if (StringUtils.hasValue (field.getCustomValue (VALIDATE_RANGE_CUSTOM_VALUE)))
                    elem.append("\"" +  getRequiredFieldStyle (field) + "/>");
                else elem.append("\"" +  getRequiredFieldStyle (field) + "/>");
*/
                elem.append("\"" +  getRequiredFieldStyle (field) + "/>");

                //Add calender if its a date field.
                if ( !prohibited  )
                {
                    if ( dType.getType() == DataType.TYPE_DATE_TIME || dType.getType() == DataType.TYPE_DATE_OPTIONAL_TIME ||
                        dType.getType() == DataType.TYPE_DATE || dType.getType() == DataType.TYPE_REL_DATE_OPTIONAL_TIME)
                        createCalendar(elem, dType, name, TagUtils.isDateConversionReq(field) && TagUtils.isClientTimezoneSupported(pageContext));
                }
            }
        }
    }

    /**
     * Call list of functions based on custom values and over write
     * present value of the input field.
     *
     * If specific conditions failed then calling functions
     * will return the passed value.
     *
     * At present this is similar to InputField so calling the
     * same function inside it.
     *
     *
     * @param field      Field object
     * @param fieldName  name of the field (some times differ then usual name)
     * @param value      present value of the field
     * @return           value modified
     */
    private String callCVsForHiddenField(Field field, String fieldName, String value)
    {
        return callCVsForInputField (field, fieldName, value); 
    }

    /**
     * Call list of functions based on custom values and over write
     * present value of the input field.
     *
     * If specific conditions failed then calling functions
     * will return the passed value.
     *
     *
     * @param field      Field object
     * @param fieldName  name of the field (some times differ then usual name)
     * @param value      present value of the field
     * @return           value modified
     */
    private String callCVsForInputField(Field field, String fieldName, String value)
    {
        value = handleTimezone(field, fieldName, value);
        value = TagUtils.populateHostId(pageContext, domainProperties, field, value, isCopyAction);
        value = TagUtils.populateJulianDate(pageContext, field, value, isCopyAction);
        value = TagUtils.populateSequenceValue(pageContext, field, value, isCopyAction);
        
        return value;
    }

    /**
     * This creates the style for the field.
     *
     * @param field html control
     * @return String style
     */
    private String getRequiredFieldStyle (Field field)
    {
        String str = "";
        String validRangeCustomVal = field.getCustomValue (VALIDATE_RANGE_CUSTOM_VALUE);
        boolean isRequired = false;
        if (field != null)
        {
            str = "";

            if (field.getDataType ().getUsage () == DataType.REQUIRED && highLightbk && !isReadOnly ())
            {
                str = "behavior: REQUIRED";
                isRequired = true;
            }

            if (StringUtils.hasValue (validRangeCustomVal))
                str += "; filter: '" + validRangeCustomVal + "," + getValidRngDisplayName (field, validRangeCustomVal) + "' ";

            if (StringUtils.hasValue (str))
            {
                str = " style=\" " + str + "\" ";
                if (isRequired) str = "class=\"requiredStyle\"" + str;
            }
        }

        return str;
    }

    /**
     * This method converts the time zone and adds the name of the field
     * to the list.
     *
     * @param field html control
     * @param fieldName as String name of the field
     * @param value as String the value of the field
     * @return String the value to be returned
     */
    public String handleTimezone(Field field, String fieldName, String value)
    {
        try
        {
            if(TagUtils.isClientTimezoneSupported(pageContext) && TagUtils.isDateConversionReq(field))
            {
                String customValue = field.getCustomValue(BodyTag.USE_CLIENT_TIMEZONE);
                if (fieldsToConvertTZ.size()==0)
                    serverTimeZone = customValue;

                int clientOffset = StringUtils.getInteger((String)pageContext.getSession().getAttribute(SecurityFilter.CLIENT_TZ_OFFSET));
                //Adjusting the offset
                clientOffset = TagUtils.getAdjustedOffset(clientOffset, field.getCustomValue(BodyTag.USE_CLIENT_TIMEZONE));
                boolean isDSTimeZone = false;
                if (StringUtils.hasValue(customValue) && "EST".equalsIgnoreCase(customValue))
                {
                    isDSTimeZone = true;    
                }
                String add = "convertToServerTZ(frm, '" + fieldName + "','"+ NFLocale.getDateTimeFormat() + "'," + clientOffset + ","+isDSTimeZone+")";
                fieldsToConvertTZ.add(add);
                if (StringUtils.hasValue(value))
                {
                    // here the date will be converted to client's local time zone
                    value = TagUtils.convertTimeZone(pageContext, value, TagUtils.CONVERT_TO_CLIENT_TZ, field.getCustomValue(BodyTag.USE_CLIENT_TIMEZONE));
                }
            }
        }
        catch(FrameworkException fe)
        {
            log.warn("Unable to convert time zone. Returning same value as [" + value + "]");
        }
        return value;
    }

    /**
     * This method converts the date time fields as per clients timezone.
     *
     */
     public void updateBeanTZFields()
     {
         if (requestBean != null && StringUtils.hasValue(requestBean.getBodyValue(FIELDS_TO_CONVERT_TZ)))
         {
            String serverTZ= requestBean.getBodyValue(SERVER_TIMEZONE);
            String toConvertFull = requestBean.getBodyValue(FIELDS_TO_CONVERT_TZ);
            String toConvert;
            String []convertValArr;
            convertValArr = StringUtils.getArray(toConvertFull, ",'");
            for (String aConvertValArr : convertValArr)
            {
                toConvert = aConvertValArr;
                if (StringUtils.hasValue(toConvert))
                {
                    int count = 0;
                    for (int i=0; i < toConvert.length() && count >= 0; i++ )
                    {
                        if (toConvert.charAt(i) == '(')
                            count++;
                        else if (toConvert.charAt(i) == ')')
                            count--;
                    }
					if (count != 0)
					{
						log.debug("Bypassing for the fieldname ["+toConvert+"], as it is not required.");
					}
                    else if (StringUtils.hasValue(requestBean.getBodyValue(toConvert)))
                    {
                        String value = requestBean.getBodyValue(toConvert);
                        // Converting time zone to Server Time Zone
                        value = TagUtils.convertTimeZone(pageContext, value, TagUtils.CONVERT_TO_SERVER_TZ,serverTZ);
                        try
                        {
                            requestBean.setBodyValue(toConvert, value);
                        }
                        catch (ServletException e)
                        {
                            log.warn("Could not set value in the bean for field " + FIELDS_TO_CONVERT_TZ);
                        }
                    }
                }
            }
            try
            {
               if (StringUtils.hasValue(requestBean.getBodyValue(FIELDS_TO_CONVERT_TZ)))
                    requestBean.setBodyValue(FIELDS_TO_CONVERT_TZ, "");
            }
            catch (ServletException e)
            {
               log.warn("Could not make empty in the bean for field " + FIELDS_TO_CONVERT_TZ);
            }
         }
     }

    /**
     * This returns the string containing the field's display
     * name while validating ranges.
     *
     * @param field that has custom value
     * @param validRangeCustomVal value of the custom value
     * @return String
     */
    private String getValidRngDisplayName(Field field, String validRangeCustomVal)
    {
        String displayNameStr = "";
        String[] fieldNames = StringUtils.getArray(validRangeCustomVal, COMMA);
        List fldList = Arrays.asList(fieldNames);
        MessagePart parentMsg = field.getParent();

        if (parentMsg != null && parentMsg instanceof FieldGroup )
        {
            FieldGroup grp = (FieldGroup) parentMsg;
            int partCount = grp.getChildren().size();
            for (int i = 0; i < partCount;i++ )
            {
                Object child= grp.getChild(i);
                if (child instanceof Field)
                {
                    Field fld = (Field) child;
                    if (fldList.contains (TagUtils.stripIdSuffix(fld.getId())))
                    {
                        if (StringUtils.hasValue(displayNameStr))
                            displayNameStr += ",";
                        displayNameStr += StringUtils.hasValue(fld.getDisplayName()) ? fld.getDisplayName() : TagUtils.stripIdSuffix(fld.getId());
                    }
                }
            }
        }
        
        return displayNameStr;
    }

    /**
     * Creates RANGE type fields.
     *
     * @param  field          The part to create the HTML for
     * @param  elem           The element which holds the html for future processing
     * @param  xmlParentPath  The current full xml path for the parent of this field
     */
    protected void createRangeFields(Field field, HtmlElement elem, String xmlParentPath)
    {
        log.debug("Creating Date field as a range.");

        String fieldPath = field.getXMLPath();

        String fromValue = getValue(field, xmlParentPath, fieldPath + FROM_SUFFIX, null);
        String toValue   = getValue(field, xmlParentPath, fieldPath + TO_SUFFIX, null);

        elem.append("<Table cellpadding=\"0\" cellspacing=\"0\">");
        elem.append("<tr>");
        elem.append("<td style=\"font-weight: bold;\">from</td>");
        elem.append("<TD align=\"left\">");
        elem.append("<INPUT type=\"text\"");

        String fromPath = getName(field, xmlParentPath) + FROM_SUFFIX;
        String toPath   = getName(field, xmlParentPath) + TO_SUFFIX;

        createRangeInput(field, elem, fromPath, fromValue);

        elem.append("</TD>");

        elem.append("<TD>&nbsp;&nbsp;&nbsp;</TD>");
        elem.append("<td style=\"font-weight: bold;\">to</td>");
        elem.append("<TD align=\"left\">");
        elem.append("<INPUT type=\"text\"");

        createRangeInput(field, elem, toPath, toValue);

        elem.append("</TD>");
        elem.append("</tr>");
        elem.append("</Table>");
    }

    protected void createRangeInput(Field field, HtmlElement elem, String fullPath, String value)
    {
        DataType dtype = field.getDataType();

        // set the correct format if this is a date field
        setDateFormat(dtype);

        if ( isEntryValidationEnabled() )
           createValidationCode(field, elem);

           elem.append("size=\"");

           elem.append(getSize(field));

           elem.append("\" maxlength=\"").append(getMaxLen(field));

           // append the just the xml path of the field
           String name = ServletConstants.NF_FIELD_PREFIX + fullPath;
           elem.append("\" name=\"").append( name );

           if (StringUtils.hasValue(value))
               elem.append("\" value=\"").append( value );

           elem.append("\"" +  getRequiredFieldStyle (field) + "/>");

           String dname = dtype.getTypeName();

           if (!isReadOnly() && dtype.getUsage() != DataType.PROHIBITED && (dname.equals("DATE") || dname.equals("DATE TIME") || dname.equals("REL DATE OPTIONAL TIME") || dname.equals("DATE OPTIONAL TIME")))

               createCalendar(elem, dtype, name, TagUtils.isDateConversionReq(field) && TagUtils.isClientTimezoneSupported(pageContext));
    }

  /**
   * Creates a calendar code for datetime and date fields.
   * Assumes that there is always a format in dtype and this is a date field.
   *
   * If one is not set then use the {$link #setDateFormat(DataType)} method
   * set set the time to the current locale format.
   *
   * @param elem HtmlElement to append the link code to.
   * @param dtype The dataType for the date field.
   * @param name The name of the html input field that should be linked to
   * this calendar.
   */
  protected void createCalendar(HtmlElement elem, DataType dtype, String name)
  {
      createCalendar(elem, dtype, name, false);
  }
    
    /**
     * Creates a calendar code for datetime and date fields.
     * Assumes that there is always a format in dtype and this is a date field.
     *
     * If one is not set then use the {$link #setDateFormat(DataType)} method
     * set set the time to the current locale format.
     *
     * @param elem HtmlElement to append the link code to.
     * @param dtype The dataType for the date field.
     * @param name The name of the html input field that should be linked to
     * this calendar.
     * @param useClientTZ decides if calendar need to display in local time zone
     */
  protected void createCalendar(HtmlElement elem, DataType dtype, String name, boolean useClientTZ)
  {
    //This is used to see if it is basic or manager
    ServletContext servletContext = pageContext.getServletContext();
    String webAppName = (String)servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
    Properties initParameters=null;
	try {
		initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
	} catch (FrameworkException fe) {
		log.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
		throw new RuntimeException("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
	}
    String basic  = initParameters.getProperty(ServletConstants.BASIC_REPLICATE_DISPLAY);
    boolean isBasic = TagUtils.isReplicateDisplayTrue(basic);
    String localMgr = initParameters.getProperty(ServletConstants.LOCALMGR_RELATIVE_DATE);
    boolean isLocalMgr = TagUtils.isLocalMgrTrue(localMgr);


    int dateType = dtype.getType();

    String displayCalFormat = "";

    if ( dateType == DataType.TYPE_DATE ) {

    } else if (dateType == DataType.TYPE_DATE_TIME || dateType == DataType.TYPE_DATE_OPTIONAL_TIME || dateType == DataType.TYPE_REL_DATE_OPTIONAL_TIME ) {


      displayCalFormat = NFLocale.getTimeFormat();
    }


    //If GUI is for basic or local manager then display with the radio else just display the calendar GUI
    if( ( (dateType == DataType.TYPE_REL_DATE_OPTIONAL_TIME ) &&(isBasic || isLocalMgr) ) )
        elem.append("&nbsp;<A onMouseOver=\"return displayStatus('Show Calendar');\" onMouseOut=\"return displayStatus('');\" HREF=\"javascript:doNothing()\" onClick=\"showPopup(document." + getFormName() +"['" + name +"'],'" + useClientTZ + "', '" + displayCalFormat + "', '"
         + dtype.getFormat()+ "','" +TagUtils.getWebAppContextPath( pageContext ) +"/pages/common/calendar.jsp'"+ ",'" +TagUtils.getWebAppContextPath( pageContext ) +"/pages/common/Relative-Date-Selector.jsp');\"><IMG SRC=\"" + TagUtils.getWebAppContextPath ( pageContext ) + "/images/Calendar.gif\" BORDER=0></A>");

      else
        elem.append("&nbsp;<A onMouseOver=\"return displayStatus('Show Calendar');\" onMouseOut=\"return displayStatus('');\" HREF=\"javascript:doNothing()\" onClick=\"showCalendar(document." + getFormName() +"['" + name +"'],'" + useClientTZ + "', '" + displayCalFormat + "', '"
        + dtype.getFormat()+ "','" +TagUtils.getWebAppContextPath( pageContext ) +"/pages/common/calendar.jsp');\"><IMG SRC=\"" + TagUtils.getWebAppContextPath ( pageContext ) + "/images/Calendar.gif\" BORDER=0></A>");

  }

    /**
     * Returns the size of the passed in field.
     * This can overwritten to change the logic of obtaining the size.
     *
     * @param f - The field to get the size of.
     * @return size
     */
    protected String getSize(Field f)
    {
       // get some field information
        DataType dtype = f.getDataType();
        int maxLen = f.getDataType().getMaxLen();
        String format = f.getDataType().getFormat();



        if (maxLen == DataType.UNSPECIFIED)
        {
          if (StringUtils.hasValue(format) ) {
            if ( dtype.getType() == DataType.TYPE_DATE_TIME ||
                 dtype.getType() == DataType.TYPE_DATE ||
                 dtype.getType() == DataType.TYPE_TIME ||
                 dtype.getType() == DataType.TYPE_DATE_OPTIONAL_TIME ||
                 dtype.getType() == DataType.TYPE_REL_DATE_OPTIONAL_TIME) {

              String sample = new SimpleDateFormat(format).format(new Date() );
              maxLen = sample.length();
              dtype.setMaxLen(maxLen );

            }
            else
              maxLen = format.length();
          }
          else
            maxLen = DEFAULT_CTRL_SIZE;
        }


        if (maxLen > maxCtrlSize)
          return String.valueOf(maxCtrlSize);
        else
          return String.valueOf(maxLen);
    }

    /**
     * Returns the max length of the passed in field.
     * This can overwritten to change the logic of obtaining the max length.
     *
     * @param f - The field to get the max length of.
     * @return max len
     */
    protected String getMaxLen(Field f)
    {
        int maxLen = f.getDataType().getMaxLen();

        if (maxLen == DataType.UNSPECIFIED)
            return "";
        else
            return String.valueOf(maxLen);

    }

    /**
     * Returns the namePath of the passed in field.
     * This is the xml path that will be used as the name of the field input plus
     * the id of the meta Field object.
     * This can overwritten to change the logic of obtaining the max length.
     *
     * @param field - The field to get the max length of.
     * @param xmlParentPath - The xml path to the parent of this field.
     * @return name
     */
    protected String getName(Field field, String xmlParentPath)
    {
       return getName (field.getXMLPath (), xmlParentPath);
    }

    /**
     * Returns the namePath of the passed in field.
     * This is the xml path that will be used as the name of the field input plus
     * the id of the meta Field object.
     * This can overwritten to change the logic of obtaining the max length.
     *
     * @param xmlPath String
     * @param xmlParentPath - The xml path to the parent of this field.
     * @return name
     */
    protected String getName(String xmlPath, String xmlParentPath)
    {
       return xmlParentPath + TagConstants.PATH_SEP + xmlPath;
    }

    /**
     * Initialise the security properties of Domain
     */
    protected void initializeDomainProperties ()
    {
        try
        {
            // DomainProperties intialization
            domainProperties =  DomainProperties.getInstance(TagUtils.getCustomerIdFromSessionBean (pageContext.getSession()));

            if (log.isDebugEnabled()) {
                log.debug("intializeDomainProperties: Intialized DomainProperties successfully.");
            }
        }
        catch (Exception e)
        {
            log.error("intializeDomainProperties: Intialization of DomainProperties failed. Error: " + e.toString());
        }
    }

    /**
     * Method to generate a hidden control of a given field.
     *
     * @param field Field
     * @param elem html elem
     * @param xmlParentPath String
     * @param value String
     * @return HtmlElement obj
     */
    protected HtmlElement createHiddenControl(Field field, HtmlElement elem, String xmlParentPath, String value )
    {
        String valueToSend    = value;

        String valueToDisplay    = value;

        if (!StringUtils.hasValue(valueToSend))
        {
            valueToSend    = getDefaultForAutoVer(field);

            valueToDisplay    = AUTOGENERATED;
        }

        elem.append("<p class=\"").append(TagConstants.CLASS_AUTO_GENERATED).append("\">").append(valueToDisplay).append("</p>");

        elem.append("<input type=\"hidden\"");

        String name = ServletConstants.NF_FIELD_PREFIX + getName(field, xmlParentPath);

        elem.append(" name=\"").append(name).append("\" value=\"");

        elem.append(valueToSend);

        elem.append("\"/>");

        return elem;
    }

    /**
     * This creates a pair of radio buttons for a field that has the associated
     * custom value 'CREATE_TWO_RADIO_BUTTONS'.
     *
     * @param field Field
     * @param elem html elem
     * @param xmlParentPath String
     * @param labelsAndValues - Comma separated values that contains labels and values of radio buttons
     * @return elem
     */
    protected HtmlElement createTwoRadioControls (Field field, HtmlElement elem, String xmlParentPath, String labelsAndValues)
    {

        StringTokenizer strToken = new StringTokenizer(labelsAndValues, ",");
        String[] lblsVals = new String [strToken.countTokens ()];
        int item=0;

        while(strToken.hasMoreTokens())
        {
            String token = strToken.nextToken();
            if (StringUtils.hasValue ( token ))
            {
                lblsVals [item] = token;
            }
            item++;
        }

        String FIRST_LBL = lblsVals [0];
        String FIRST_VAL = lblsVals [1];
        String SECOND_LBL = lblsVals [2];
        String SECOND_VAL = lblsVals [3];

        String GRP_SUFFIX = "_Radio_Grp";
        String GRP_NAME = ServletConstants.NF_FIELD_PREFIX + getName(field, xmlParentPath) + GRP_SUFFIX;

        String fieldPath = field.getXMLPath () + GRP_SUFFIX;

        String checkedVal = getValue (field, xmlParentPath, fieldPath, null);
        if (!StringUtils.hasValue ( checkedVal ))
            checkedVal = FIRST_VAL;

        String checked = (checkedVal.equals ( FIRST_VAL )) ? " checked " : "";
        elem.append ( "<input type=\"radio\" value=\"" + FIRST_VAL + "\" " + checked + " name=\"" + GRP_NAME + "\" ><span style=\"font-weight: bold;\">" + FIRST_LBL + "</span>" );

        checked = (checkedVal.equals ( SECOND_VAL )) ? " checked " : "";
        elem.append ( "<input type=\"radio\" value=\"" + SECOND_VAL + "\" " + checked + " name=\"" + GRP_NAME + "\" ><span style=\"font-weight: bold;\">" + SECOND_LBL + "</span>" );

        elem.append ( "<br>" );
        return elem;
    }
}

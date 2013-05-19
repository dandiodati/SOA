/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.query;


import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.tag.message.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.locale.*;
import com.nightfire.framework.message.common.xml.*;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.w3c.dom.*;


import com.nightfire.framework.util.*;



/**
 * Creates the html field body elements and then calls the provided
 * HtmlElementLayout class to finish generating the html.
 *
 * Used with in a Section or Form tag.
 *
 */
public class SearchBodyTag extends com.nightfire.webgui.core.tag.message.BodyTag
{
    /**
     * Default HTML form name.
     */
    public static final String DEFAULT_FORM_NAME = "searchForm";


    public static final String DECIMAL_WILD_SUFFIX = " - WILDCARD";

    public static final String DOT = ".";
    public static final String FIXED_RELATIVE = "Fixed_Relative";
    public static final String FIXED_DATE = "Fixed_Date";
    public static final String RELATIVE_DATE = "Relative_Date";
    public static final String NULL_DATE = "Null_Date";
    public static final String FROM_FORMAT = ".From_format";
    public static final String TO_FORMAT = ".To_format";

    /**
     * Return the name of fields as just the path of the field (id)
     */
    protected String getName(Field f, String xmlParentPath)
    {
       return f.getXMLPath();
    }

    /**
     * returns a an empty max length/ so that multiple values can be entered
     * except only where the FieldGroup is of type multiple.
     */
    protected String getMaxLen(Field f)
    {
        MessageContainer parent = f.getParent();
        if (parent instanceof FieldGroup)
        {
            FieldGroup fg = (FieldGroup) parent;
            String fgType = fg.getCustomValue(FIELD_GROUP_TYPE_CUSTOM_VALUE);
            if (StringUtils.hasValue(fgType) && fgType.equals(FIELD_GROUP_TYPE_VALUE_MULTIPLE))
                return super.getMaxLen (f);
        }

        // For spid data types return the max length from meta file.
        if ( f.getDataType().getType() == (DataType.TYPE_SPID))
        {
            return super.getMaxLen(f);            
        }

        return "";
    }


     /**
     * gets the value of a field from the message.
     * Tries to get the fieldPath under the node xmlParentNode. If xmlParentNode
     * is null then xmlParentPath is used instead.
     * Also provides alias mappings of values.
     * @param xmlParentPath The xml path of the parent node.
     * @param fieldPath - The path of the xml node relative to xmlParentPath
     * @param xmlParentNode -The xml parent node, used for repeating access. (May be null).
     *
     */
    protected String getValue(Field field, String xmlParentPath, String fieldPath, Node xmlParentNode)
    {

        String value = "";

        String path = null;

        path = fieldPath;

        XMLGenerator message = getMessageData();



        try {

           if ( xmlParentNode == null && message.exists(path)){
               value = message.getValue(path);

           }
           else if ( message.exists(xmlParentNode, fieldPath)  ) {
              value = message.getValue(xmlParentNode, fieldPath);

         }
           else if ( StringUtils.hasValue(field.getCustomValue(DEFAULT_VAL) ) ) {
              value = field.getCustomValue(DEFAULT_VAL);

          }


          // get an alias value for a field
          // each alias has the field path as a prefix followed by the field value.
          //
          String temp = aliasDescriptor.getAlias(pageContext.getRequest(), field.getFullXMLPath() , value, true);

        } catch (FrameworkException e) {
          // did not find node return empty
          log.debug("Node [" + path + "] does not exist.");
        }

        return TagUtils.performHTMLEncoding ( value );

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
     * Creates RANGE type fields.
     *
     * @param  field          The part to create the HTML for
     * @param  elem           The element which holds the html for future processing
     * @param  xmlParentPath  The current full xml path for the parent of this field
     */
    protected void createRangeFields(Field field, HtmlElement elem, String xmlParentPath)
    {

        //This is used to see if this  is for basic or manager
        ServletContext servletContext = pageContext.getServletContext();
        String webAppName = (String)servletContext.getAttribute(ServletConstants.WEB_APP_NAME);
        Properties initParameters=null;
		try {
			initParameters = MultiJVMPropUtils.getInitParameters(servletContext, webAppName);
		} catch (FrameworkException fe) {
			log.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
			throw new RuntimeException("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
		}
        String basic = initParameters.getProperty(ServletConstants.BASIC_REPLICATE_DISPLAY);
        boolean isBasic = TagUtils.isReplicateDisplayTrue(basic);

        String localMgr = initParameters.getProperty(ServletConstants.LOCALMGR_RELATIVE_DATE);
         boolean isLocalMgr = TagUtils.isLocalMgrTrue(localMgr);

        String fieldPath = field.getXMLPath();




        String fromPath = fieldPath + FROM_SUFFIX;
        String toPath   = fieldPath + TO_SUFFIX;

        String fromValue = getValue(field, xmlParentPath, fromPath , null);
        String toValue   = getValue(field, xmlParentPath, toPath, null);

        log.debug("Creating Date field as a range.");

        elem.append("<Table cellpadding=\"0\" cellspacing=\"0\">");
        //only if field type is date then add then else do not
        DataType dType = field.getDataType();


        //Display GUI with radio buttons onlyfor basic
       //if ( dType.getType() == DataType.TYPE_REL_DATE_OPTIONAL_TIME  && isBasic) {
    if( ( (dType.getType() == DataType.TYPE_REL_DATE_OPTIONAL_TIME ) &&(isBasic || isLocalMgr) ) ) {

        //create radios here
        String name = getName(field, xmlParentPath);

        /**
         * As we are grouping the radio buttons we do not need to obtain the name of both the
         * radio buttons, since in group all the radio button shares the common name
         */
        String fixedRelativeRadio = ServletConstants.NF_FIELD_PREFIX + fieldPath + DOT +  FIXED_RELATIVE;

        String fromFormat = ServletConstants.NF_FIELD_PREFIX + fieldPath + FROM_FORMAT;
        String toFormat = ServletConstants.NF_FIELD_PREFIX + fieldPath + TO_FORMAT;

        String fromFormatValue = getValue(field, xmlParentPath, fieldPath + FROM_FORMAT, null);
        String toFormatValue = getValue(field, xmlParentPath, fieldPath + TO_FORMAT, null);

        String fixedRelativeRadioValue = getValue(field, xmlParentPath, fieldPath + DOT + FIXED_RELATIVE, null);

        // Providing the initial value in case fixedRelativeRadioValue is empty
        if(fixedRelativeRadioValue.trim().equals(""))
            fixedRelativeRadioValue=FIXED_DATE;

        elem.append("<tr>");

        String isNullableCV = field.getCustomValue(NULLABLE_CUSTOM_VALUE);
        boolean isNullable = StringUtils.getBoolean(isNullableCV, false);

        if(isNullable)
        {
            elem.append("<TD align=\"left\" colspan = \"8\">");
            elem.append("<INPUT type=\"radio\"");
            elem.append("\" name=\"").append(fixedRelativeRadio);
            elem.append("\" value=\"").append(FIXED_DATE);
            if(StringUtils.hasValue(fixedRelativeRadioValue) && fixedRelativeRadioValue.equalsIgnoreCase(FIXED_DATE))
                elem.append("\" CHECKED \"");
            elem.append("\" OnClick=\"handleNullableField (this, '").append(name).append("',false)");
            elem.append("\"/>");
            elem.append("<b>Fixed Date</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");

            elem.append("<INPUT type=\"radio\"");
            elem.append("\" name=\"").append(fixedRelativeRadio);
            elem.append("\" value=\"").append(RELATIVE_DATE);
            if(StringUtils.hasValue(fixedRelativeRadioValue) && fixedRelativeRadioValue.equalsIgnoreCase(RELATIVE_DATE))
                elem.append("\" CHECKED \"");
            elem.append("\" OnClick=\"handleNullableField (this, '").append(name).append("',false)");
            elem.append("\"/>");
            elem.append("<b>Relative Date</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");

            elem.append("<INPUT type=\"radio\"");
            elem.append("\" name=\"").append(fixedRelativeRadio);
            elem.append("\" value=\"").append(NULL_DATE);
            if(StringUtils.hasValue(fixedRelativeRadioValue) && fixedRelativeRadioValue.equalsIgnoreCase(NULL_DATE))
            {
                elem.append("\" CHECKED \"");
                fieldsToDisable.add(name);
            }
            elem.append("\" OnClick=\"handleNullableField (this, '").append(name).append("',true)");
            elem.append("\"/>");
            elem.append("<b>Null Date</b>");

        }
        else
        {
            elem.append("<TD align=\"left\">");
            elem.append("<INPUT type=\"radio\"");
            elem.append("\" name=\"").append(fixedRelativeRadio);

            elem.append("\" value=\"").append(FIXED_DATE);
            if(StringUtils.hasValue(fixedRelativeRadioValue) && fixedRelativeRadioValue.equalsIgnoreCase(FIXED_DATE))
                elem.append("\" CHECKED \"");
            elem.append("\"/>");
            elem.append("</TD>");


            elem.append("<td style=\"font-weight: bold;\">Fixed Date</td>");
            elem.append("<TD>&nbsp;&nbsp;&nbsp;</TD>");
            elem.append("<TD align=\"left\">");
            elem.append("<INPUT type=\"radio\"");

            elem.append("\" name=\"").append(fixedRelativeRadio);
            elem.append("\" value=\"").append(RELATIVE_DATE);

            if(StringUtils.hasValue(fixedRelativeRadioValue) && fixedRelativeRadioValue.equalsIgnoreCase(RELATIVE_DATE))
                elem.append("\" CHECKED \"");


            elem.append("\"/>");
            elem.append("</TD>");
            elem.append("<td style=\"font-weight: bold;\">Relative Date</td>"); 
        }
        elem.append("</tr>");



        ////////////////////////
        elem.append("<tr>");

        elem.append("<TD >");
        elem.append("<INPUT type=\"hidden\"");
        elem.append("\" name=\"").append(fromFormat);
        //add value here later by parsing the XML
        if(StringUtils.hasValue(fromFormatValue))
            //elem.append("\" value=\"");
             elem.append("\" value=\"").append(fromFormatValue);
        else
             elem.append("\" value=\"");
        elem.append("\"/>");
        elem.append("</TD>");

        elem.append("<TD >");
        elem.append("<INPUT type=\"hidden\"");
        elem.append("\" name=\"").append(toFormat);
        if(StringUtils.hasValue(toFormatValue))
            //elem.append("\" value=\"");
            elem.append("\" value=\"").append(toFormatValue);
        else
             elem.append("\" value=\"");
        //add value here later by parsing the XML
        //elem.append("\" value=\"");
        elem.append("\"/>");
        elem.append("</TD>");

        elem.append("</tr>");
    }


        //end create radios

        elem.append("<tr>");
        elem.append("<td style=\"font-weight: bold;\">from</td>");
        elem.append("<TD align=\"left\">");
        elem.append("<INPUT type=\"text\"");

        //createRangeInput(field, elem, fromPath, fromValue);
        createRangeInput(field, elem, fieldPath, FROM_SUFFIX, fromValue, TO_SUFFIX);

        elem.append("</TD>");

        elem.append("<TD>&nbsp;&nbsp;&nbsp;</TD>");
        elem.append("<td style=\"font-weight: bold;\">to</td>");
        elem.append("<TD align=\"left\">");
        elem.append("<INPUT type=\"text\"");
        //createRangeInput(field, elem, toPath, toValue);
        createRangeInput(field, elem, fieldPath, TO_SUFFIX, toValue, FROM_SUFFIX);

        elem.append("</TD>");
        elem.append("</tr>");
        elem.append("</Table>");
    }

    /**
     *  Creates RANGE type fields.
     * @param field  The part to create the HTML for
     * @param elem   The element which holds the html for future processing
     * @param fieldPath  the current full xml path for the parent of this field
     * @param fieldPathExt1  determines the range field on with the java-script is being created ('FROM_SUFFIX' or 'TO_SUFFIX').
     * @param value   value of the current field
     * @param fieldPathExt2 Determines the Field on the other side of the range ('TO_SUFFIX' or 'FROM_SUFFIX').
     *
     * @see  #createRangeValidationCode(Field field, HtmlElement elem, String fieldPath, String fieldPathExt)
     */
    protected void createRangeInput(Field field, HtmlElement elem, String fieldPath, String fieldPathExt1, String value, String fieldPathExt2)

    {
        DataType dtype = field.getDataType();

        // set the correct format if this is a date field
        setDateFormat(dtype);

        String name = ServletConstants.NF_FIELD_PREFIX + fieldPath+fieldPathExt1;
        value = handleTimezone(field, name, value);

        if ( isEntryValidationEnabled() )
           createRangeValidationCode(field, elem,fieldPath, fieldPathExt2);

           elem.append("size=\"");

           elem.append(getSize(field));

           elem.append("\" maxlength=\"").append(getMaxLen(field));

           // append the just the xml path of the field
           elem.append("\" name=\"").append( name );

           if (StringUtils.hasValue(value))
               elem.append("\" value=\"").append(value);

           elem.append("\"/>");
           String dname = dtype.getTypeName();

           if (!isReadOnly() && dtype.getUsage() != DataType.PROHIBITED && (dname.equals("DATE") || dname.equals("DATE TIME") || dname.equals("REL DATE OPTIONAL TIME") || dname.equals("DATE OPTIONAL TIME")))
               createCalendar(elem, dtype, name, TagUtils.isDateConversionReq(field) && TagUtils.isClientTimezoneSupported(pageContext));
    }

    /**
     * This method facilitiates to validate RANGE type fields.Internally uses 'rangeFromValidate()' and 'rangeToValidate()'
     * javaScript functions (defined in validate.js) to validate RANGE fields.
     * TODO: currently it support only validation of Date range. 'rangeValidate()' (validate.js) function must be updated
     * for any other datatype range validations.
     * @param field  The part to create the HTML for
     * @param elem   The element which holds the html for future processing
     * @param fieldPath  the current full xml path for the parent of this field
     * @param fieldPathExt  determines the field postion i.e., FROM or TO.
     */
    protected void createRangeValidationCode(Field field, HtmlElement elem, String fieldPath, String fieldPathExt){


        DataType typeInfo = field.getDataType();

        String format = typeInfo.getFormat();
        if(fieldPathExt.equals(TO_SUFFIX)){
        elem.append(" onBlur=\"rangeFromValidation(this, new ValidateInfo('");
        }else{elem.append(" onBlur=\"rangeToValidation(this, new ValidateInfo('");}
        elem.append(field.getID());
        elem.append("', '");
        elem.append(typeInfo.getTypeName()+((typeInfo.getType() == DataType.TYPE_DECIMAL)?DECIMAL_WILD_SUFFIX:""));
        elem.append("', ");
        elem.append(typeInfo.getMinLen());
        elem.append(", '");
        if (StringUtils.hasValue(format)) {
          // if this is a text field then regular expressions are used
          // so add another escape for the js code
          if ( typeInfo.getType() == DataType.TYPE_TEXT)
            elem.append(StringUtils.replaceSubstrings(format,"\\", "\\\\"));
          else
            elem.append(format);
        }

        elem.append("', '");

        if (StringUtils.hasValue(field.getCustomValue (VALIDATION_ERR_MSG_CUSTOM_VALUE))) {
            elem.append(TagUtils.filterSpecialChars(field.getCustomValue (VALIDATION_ERR_MSG_CUSTOM_VALUE)));
        }

        elem.append("'), document.searchForm['"+ServletConstants.NF_FIELD_PREFIX + fieldPath+fieldPathExt+"'] );\" ");
    }


    /**
     * Overloads parent method to allow wild cards in decimal types.
     */
    protected void createValidationCode(Field field, HtmlElement elem)
    {

        DataType typeInfo = field.getDataType();

        String format = typeInfo.getFormat();

        // if this is a decimal data type then allow wild cards.
        // otherwise call super.createValidationCode.
        if (typeInfo.getType() != DataType.TYPE_DECIMAL) {
            super.createValidationCode(field, elem);
        }else {
            elem.append(" onBlur=\"").append(CALLALLRULES).append("entryValidation(this, new ValidateInfo('");
            elem.append(field.getID());
            elem.append("', '");
            elem.append(typeInfo.getTypeName() + DECIMAL_WILD_SUFFIX);
            elem.append("', ");
            elem.append(typeInfo.getMinLen());
            elem.append(", '");

            if (StringUtils.hasValue(format))
                elem.append(format);

            elem.append("') );\" ");

        }
    }

    /**
     *  This method is used for checking whether single option is present
     * in a drop-down.
     *
     * @param options
     * @param showEmpty - boolean indicates whether to show empty options
     * @return true if single value is present in the options
     */
    protected boolean isSingleValueOption (String[] options, boolean showEmpty)
    {
        if (options.length == 1 && !showEmpty)
            return true;

        return false;
    }

}

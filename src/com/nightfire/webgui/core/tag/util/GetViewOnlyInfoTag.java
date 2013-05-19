/*
 * Copyright(c) 2001 NeuStar Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.tag.util;

import com.nightfire.webgui.core.tag.VariableTagBase;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.tag.message.BodyTag;
import com.nightfire.webgui.core.beans.NFBean;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.webgui.core.tag.TagConstants;
import com.nightfire.webgui.core.AliasDescriptor;
import com.nightfire.webgui.core.ServletConstants;

import javax.servlet.jsp.JspException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.IOException;

/**
 * This class prepares the read-only summary view of the fields
 * present in a message bean in a tabular format. The field information
 * is taken from the msgPart input object. The number of elements
 * displayed per row is configurable and default value is set to 3.
 */
public class GetViewOnlyInfoTag extends VariableTagBase
{
    private NFBean bean = null;
    private MessagePart msgPart = null;
    private int elementsPerLine = 3;

    private static final String ALL = "All";
    private static final String TO = "To";
    private static final String FROM = "From";
    private static final String DOT = ".";
    private static final String PIPE = "|";
    private static final String COMMA = ",";
    private static final String DASH = "-";
    private static final String SPACE = " ";
    private static final String NULL_STRING_VALUE = "null";

    private String nullClauseFields;

    private AliasDescriptor aliasDescriptor;

    /**
     * Sets the beanName to obtain NFBean
     * @param beanName obj
     * @throws JspException on error
     */
    public void setBean ( Object beanName ) throws JspException
    {
        this.bean = (NFBean) TagUtils.getDynamicValue ("RequestBean", beanName, NFBean.class, this, pageContext);
    }

    /**
     * Sets the msgPartName variable to obtain MessagePart
     * @param msgPartName obj
     * @throws JspException on error
     */
    public void setMsgPart ( Object msgPartName ) throws JspException
    {
        this.msgPart = (MessagePart) TagUtils.getDynamicValue ("curPartSearch", msgPartName, MessagePart.class, this, pageContext);
    }

    /**
     * Sets the elementsPerLine to display on GUI
     * Default value is taken as 3.
     * @param elementsPerLine String
     */
    public void setElementsPerLine ( String elementsPerLine )
    {
        if (StringUtils.hasValue ( elementsPerLine ))
        {
            try
            {
                this.elementsPerLine = StringUtils.getInteger ( elementsPerLine );
            }
            catch (FrameworkException e)
            {
                this.elementsPerLine = 3;
                log.error("GetViewOnlyInfoTag.setElementsPerLine(): Setting elementsPerLine as [3] due to error: " + e.getMessage ());
            }
        }
    }

    /**
     * Provides HTML to display the bean information
     * as labels in well defined format.
     *
     * @return int val
     * @throws JspException
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();

        nullClauseFields = getValueFromBean(BodyTag.NULL_CLAUSE_FIELDS);
        aliasDescriptor = (AliasDescriptor) pageContext.getAttribute(ServletConstants.ALIAS_DESCRIPTOR, pageContext.APPLICATION_SCOPE);
        StringBuffer html = new StringBuffer ("<table class=\"SummaryInfo\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
        int colWidth = 100 / elementsPerLine;

        for (int cols = 0; cols < elementsPerLine; cols++)
        {
            html.append ( "<col width=\"").append(colWidth).append("%\">" );
        }

        try
        {
            MessagePart mp = msgPart;
            MessageContainer mc = (MessageContainer) mp.getRoot().getMessagePart(mp.getId());

            List childList = mc.getChildren();
            LinkedHashMap<String, String> labelValueMap = getLabelValuePairs(childList);

            int childListSize = labelValueMap.size();
            Iterator iterator = labelValueMap.keySet().iterator ();
            int elem = 0;

            while ( iterator.hasNext () )
            {
                html.append ( "<tr class=\"SummaryInfo\">" );

                for (int cols = 0; cols < elementsPerLine && elem < childListSize; cols++, elem++ )
                {
                    String key = (String) iterator.next();
                    String value = labelValueMap.get( key );

                    html.append ( "<td><b>" );
                    html.append ( key );
                    html.append ( ":</b>&nbsp;" );
                    html.append ( value );
                    html.append ( "</td>" );
                }

                html.append ( "</tr>" );
            }

            html.append ( "</table>" );

            pageContext.getOut().println(html.toString());
        }
        catch (IOException e)
        {
            String errorMessage = "Failed in getting Search Information: " + e.getMessage();
            log.error("GetViewOnlyInfoTag.doStartTag(): " + errorMessage);
            throw new JspException(errorMessage);
        }
        catch (Exception e)
        {
            log.error("GetViewOnlyInfoTag.doStartTag(): " + e.getMessage ());
            throw new JspException(e.getMessage ());
        }

        return SKIP_BODY;
    }

    /**
     * This method checks whether the input bean value is
     * in multi-valued (PIPE separated) and if it is multi-valued
     * then obtain corresponding display values and return the String.
     *
     * @param beanVal String
     * @param field Field
     * @return String
     */
    private String getDisplayValue (String beanVal, Field field)
    {

        StringBuffer returnBeanVal = new StringBuffer(beanVal);

        /**
         * Getting display Values only in case of enumeration
         */
        if (isEnumerated (field))
        {
            StringTokenizer token = new StringTokenizer ( beanVal, PIPE );

            String[] optionValues = null;
            String[] optionDisplay = null;

            /**
             * Populating the optionValues and optiondisplay if field is not query-oriented
             */
            String queryMethod = field.getCustomValue ( BodyTag.QUERY_METHOD_CUSTOM_VALUE );
            String queryCriteria = field.getCustomValue ( BodyTag.QUERY_CRITERIA_CUSTOM_VALUE );
            if (!StringUtils.hasValue ( queryMethod ) && !StringUtils.hasValue ( queryCriteria ))
            {
                optionValues = field.getDataType ().getOptionSource ().getOptionValues ();
                optionDisplay = field.getDataType ().getOptionSource ().getDisplayValues ();
            }

            /**
             * making empty the returnBeanVal so that it can store
             * the display values of the selected values.
             */
            returnBeanVal = new StringBuffer("");
            while (token.hasMoreTokens ())
            {
                if (StringUtils.hasValue ( returnBeanVal.toString () ))
                    returnBeanVal.append (COMMA);
                String tokenVal = token.nextToken ();
                String displayVal = getOptionDisplayValue (optionValues, tokenVal, optionDisplay);
                displayVal = aliasDescriptor.getAlias(pageContext.getRequest(), field.getFullXMLPath() , displayVal, true);
                returnBeanVal.append (displayVal);
            }
        }
        if (log.isDebugEnabled ())
        {
            log.info ( "getDisplayValue(): returning the value ["+returnBeanVal+"] for field ["+field.getId ()+"]." );
        }
        return returnBeanVal.toString ();
    }

    /**
     * Checks whether the input value (val) is present
     * in the array of values (valArr) and if present
     * then it returns the correnponding display value
     * from array of display values (displayArr) otherwise
     * returns the coming value (val)
     *
     * @param valArr String []
     * @param val String
     * @param displayArr String []
     * @return String
     */
    private String getOptionDisplayValue ( String[] valArr, String val, String[] displayArr)
    {
        String returnVal = val;
        if (valArr != null && displayArr != null)
        {
            int arrLen = valArr.length;
            for (int elem = 0; elem < arrLen; elem++)
            {
                /* matching the exact values (case sensitive) */
                if (valArr [elem].equals ( val ))
                {
                    returnVal = displayArr [elem];
                    break;
                }
            }
        }
        if (log.isDebugEnabled ())
        {
            log.info ( "getOptionDisplayValue(): returning the display value ["+returnVal+"] for value ["+val+"]." );
        }
        return returnVal;
    }

    /**
     * Return true if dataTypeName is of type Enumerated
     * @param field Field
     * @return boolean
     */
    private boolean isEnumerated ( Field field )
    {
        return ( field.getDataType().getType() == DataType.TYPE_ENUMERATED);
    }

    /**
     * Return true if field has range data type
     * @param field Field
     * @return boolean
     */
    private boolean isRangeDataType ( Field field )
    {
        DataType dType = field.getDataType();

        return  ( (dType.isInstance(TagConstants.DATA_TYPE_RANGE)) ||
        	 (dType.isInstance(TagConstants.DATA_TYPE_RANGE_OPTIONAL)) ||
        	 (dType.isInstance(TagConstants.DATA_TYPE_REL_RANGE_OPTIONAL)) );

    }

    /**
     * Return String value of a given fieldId in a bean
     * @param fieldId String
     * @return boolean
     */
    private String getValueFromBean (String fieldId)
    {
        String returnVal = "";
        if (bean != null)
        {
            returnVal = bean.getBodyValue (fieldId);
        }
        return returnVal;
    }

    /**
     * Get the Label-Value pair from the list of fields
     * @param list List obj
     * @return LinkedHashMap
     */
    private LinkedHashMap<String, String> getLabelValuePairs(List list)
    {
        if (list == null)
            return null;

        int noOfChildren = list.size();
        LinkedHashMap<String, String> labelValueMap = new LinkedHashMap<String, String>();
        Object fieldClass = null;
        Object fieldGrpClass = null;
        try
        {
            fieldClass = Class.forName ("com.nightfire.webgui.core.meta.Field");
            fieldGrpClass = Class.forName ("com.nightfire.webgui.core.meta.FieldGroup");
        }
        catch (ClassNotFoundException e)
        {
            log.error ( "Unable to find class Field in webgui package." );
        }

        for ( int elem = 0; elem < noOfChildren; elem++ )
        {
            Object obj = list.get ( elem );
            if (obj.getClass () != fieldClass)
            {
                if (obj.getClass () == fieldGrpClass)
                {
                    FieldGroup fieldGrp = (FieldGroup) obj;
                    // FieldGroup can only contain Fields
                    List children = fieldGrp.getChildren ();
                    int fldGrpElems = children.size ();

                    for ( int n = 0; n < fldGrpElems; n++ )
                    {
                        Field tempFld = (Field) children.get (n);
                        updateInfo (tempFld, labelValueMap);
                    }
                }

                // move to next field
                continue;
            }

            Field field = (Field) obj;
            updateInfo (field, labelValueMap);

        }

        return labelValueMap;
    }

    /**
     * Return true if field's parent is
     * Field Group of type multiple 
     *
     * @param field Field
     * @return boolean
     */
    private boolean isMultiControl ( Field field )
    {
        MessageContainer parent = field.getParent();
        if (parent instanceof FieldGroup)
        {
            FieldGroup fg = (FieldGroup) parent;
            String fgType = fg.getCustomValue(BodyTag.FIELD_GROUP_TYPE_CUSTOM_VALUE);
            if (StringUtils.hasValue(fgType) && fgType.equals(BodyTag.FIELD_GROUP_TYPE_VALUE_MULTIPLE))
                return true;
        }

        return false;
    }

    /**
     * This method update the red only information for the
     * FieldGroup of type multiple.
     * 
     * @param field Field object
     * @param labelValueMap LinkedHashMap
     */
    private void updateMap (Field field, LinkedHashMap<String, String> labelValueMap)
    {
        // this function will only be called if isMultiControl
        // will return true, so directly casting parent to FieldGroup
        FieldGroup fg = (FieldGroup) field.getParent();
        // This should come single time only
        if (!labelValueMap.containsKey (fg.getId()))
        {
            List childs = fg.getChildren();

            StringBuffer sb = new StringBuffer();

            // FieldGroup of type mulitple can only contain Field as children
            int len = childs.size();
            for (int fld = 0; fld < len; fld++)
            {
                Field f = (Field) childs.get(fld);
                String val = getValueFromBean ( TagUtils.stripIdSuffix(f.getId()));
                val = getTZConvertedVal( field, val );

                if (fld != 0 && StringUtils.hasValue (val))
                {
                    sb.append (DASH);
                }

                sb.append(val);
            }
            String lbl = fg.getId();
            if (StringUtils.hasValue(fg.getDisplayName()))
                lbl = fg.getDisplayName();

            labelValueMap.put(lbl, sb.toString());
        }
    }
    
    /**
     * Update the LinkedHashMap with field label and
     * value.
     *
     * @param field Field
     * @param labelValueMap LinkedHashMap
     */
    private void updateInfo (Field field, LinkedHashMap<String, String> labelValueMap)
    {

        /* Skip any hidden field */
        if ( !TagUtils.isHidden (field) )
        {
            String fieldId = TagUtils.stripIdSuffix ( field.getId () );

            String fieldVal = getValueFromBean ( fieldId );
            fieldVal = getTZConvertedVal( field, fieldVal );

            if ( StringUtils.hasValue( fieldVal ) )
                fieldVal = getDisplayValue (fieldVal, field);

            /*In case abbreviation does not exists the display name would be used*/
            String fieldLabel = StringUtils.hasValue (field.getAbbreviation ()) ?
                            field.getAbbreviation () : field.getDisplayName ();

            /**
             * Checking for Enumarated and Range data types
             * to handle their values and passing the coming
             * value for other data types
             */
            if ( !StringUtils.hasValue ( fieldVal ) && isEnumerated ( field ))
                fieldVal = ALL;

            if ( isRangeDataType( field ) && StringUtils.hasValue(nullClauseFields) && nullClauseFields.indexOf(fieldId) != -1)
                {
                    labelValueMap.put ( fieldLabel, NULL_STRING_VALUE );
                }
            else if ( isRangeDataType( field ) )
            {
                String fromVal = getValueFromBean ( fieldId + DOT + FROM );
                fromVal = getTZConvertedVal( field, fromVal );
                String fromLabel = fieldLabel + "." + FROM;

                labelValueMap.put ( fromLabel, fromVal );

                String toVal = getValueFromBean ( fieldId + DOT + TO );
                toVal = getTZConvertedVal( field, toVal );
                String toLabel = fieldLabel + "." + TO;

                labelValueMap.put ( toLabel, toVal );
            }
            else if (isMultiControl (field))
            {
                updateMap (field, labelValueMap);
            }
            else {
                labelValueMap.put ( fieldLabel, fieldVal );
            }
        }
    }

    /**
     * Check if field has custom value BodyTag.USE_CLIENT_TIMEZONE
     * then return the converted value.
     * @param field Field object
     * @param fieldVal present value of the field from bean
     * @return converted value
     */
    private String getTZConvertedVal(Field field, String fieldVal)
    {
        String returnVal = "";
            // Converting date-time to client's local time zone
        if (TagUtils.isDateConversionReq(field))
            returnVal = TagUtils.convertTimeZone(pageContext, fieldVal, TagUtils.CONVERT_TO_CLIENT_TZ, field.getCustomValue(BodyTag.USE_CLIENT_TIMEZONE));

        if (!StringUtils.hasValue(returnVal))
            returnVal = fieldVal;
        
        return returnVal;
    }
}
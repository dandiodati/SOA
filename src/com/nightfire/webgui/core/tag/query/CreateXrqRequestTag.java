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
import com.nightfire.framework.xrq.sql.clauses.*;
import com.nightfire.framework.xrq.*;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;
import org.w3c.dom.*;

import java.util.*;



/**
 * This tag creates an xrq request from a list of fields in a meta file.
 * This creates a simple xrq request by creating an AND with all the
 * possible fields. This tag outputs the columns to select in a variable
 * defined by varSelectFields and outputs the xrq where clause in the variable defined by
 * var. If var is not defined the the xrq where clause gets written to the current page's
 * output stream.
 */
public class CreateXrqRequestTag extends VariableTagBase
{

    
    /**
     * This is a custom value for a field which indicates that 
     * this field is a primary key.
     *
     */
    public static final String PKEY_INDICATOR = "primaryKey";
    

     /**
     * The current message part which needs to be set by a child class before
     * calling doStartTag
     */
    private MessageContainer curPart;

    /**
     * Holds primary key of the database table.
     * initialize to a empty string so that the  
     * string buffer in doStartTag does not throw a null pointer if
     * primary keys where not passed in a setter method.
     */
    private String pKey = ""; 




    /**
     * The generator which hold the xml message data.
     */
    private XMLGenerator message;

    private String varSelection;

    private boolean usePkeysOnly = false;
    


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
     * Sets the primary key for this tag.
     * This is the primary key of the database table used
     * to build the query.
     * @param pKey The primary key.
     * @depreciated This setter should no longer be used. Primary keys should
     * be indicated in the meta file. This is left for backwards compatibility.
     *
     */
    public void setPrimaryKey(String pKey) throws JspException
    {
       this.pKey = (String)TagUtils.getDynamicValue("primaryKey", pKey, String.class,this,pageContext);
    }

    /**
     * Indicates to only use primary keys to build the where clause.
     * 
     *
     * @param bool a <code>String</code> value
     * @exception JspException if an error occurs
     */
    public void setUsePrimaryKeysOnly(String bool) throws JspException
    {
        usePkeysOnly = StringUtils.getBoolean(bool, false);
    }


    /**
     * Sets the variable name for the select columns
     * @param varName the variable name.
     *
     */
    public void setVarSelectFields(String varName)
    {
       varSelection = varName;
    }


    public int doStartTag() throws JspException
    {
        super.doStartTag();

        StringBuffer selectFields = new StringBuffer(pKey);
        if (StringUtils.hasValue(pKey) )
            selectFields.append(", ");
        
        if ( curPart == null) {
           log.error("Message Part is null");
           throw new JspTagException("Message Part is null");
        }

        Iterator iter = curPart.getChildren().iterator();
        
        // build a list of all key fields( fields that are sent to the detail)
        // and all fields displayed in the list
        while (iter.hasNext() ) {

           Field f = (Field) iter.next();
           String name = f.getXMLPath();
          
           selectFields.append(name );

           if ( iter.hasNext() )
               selectFields.append(", ");
        }

        VariableSupportUtil.setVarObj(varSelection,selectFields.toString(), scope, pageContext);


        try {

           XMLPlainGenerator xrqOut = new XMLPlainGenerator("AND");


           iter = curPart.getChildren().iterator();

           // build a list of all key fields( fields that are sent to the detail)
           // and all fields displayed in the list
           if (message != null) {
              int index = 0;
              while (iter.hasNext() ) {
                 Field f = (Field) iter.next();
                 DataType type  = f.getDataType();
                 boolean isPkey = StringUtils.getBoolean(f.getCustomValue(PKEY_INDICATOR), false );
                 // only add the fields if the use primary key flag is true and this
                 // is a primary key or if use primary key flag is off
                 if ((usePkeysOnly && isPkey) || !usePkeysOnly ) {
                     
                    log.debug("Looking at meta field ["+ f.getXMLPath()+  "]");
                    if (type.isInstance(TagConstants.DATA_TYPE_RANGE)) {
                       createRange(index, f, xrqOut, message);
                    } else {
                       createCompare(index, f, xrqOut, message);
                    }
                    index++;
                 }
                 
              }
           }  else
             log.debug("Message is null, creating empty clause");


           String output = xrqOut.getOutput();

           // remove the xml version declaration on the top of the xml message
           // since this is going to be embeded in another xml message

           int end = output.indexOf("?>");
           output = output.substring(end+2);
           


           if ( log.isDebugEnabled() )
              log.debug("Creating SELECT fields [" + selectFields.toString() + "]\n Creating WHERE statement \n [" + output +"]");


           if ( varExists() )
              this.setVarAttribute(output);
           else
              pageContext.getOut().println(output);


        } catch (Exception e) {
           String err= "CreateXrqRequestTag: Failed to create xrq xml : " + e.getMessage();
           log.error(err);
           throw new JspTagException(err);
        }

        return SKIP_BODY;


    }


    /**
     * creates a compare xrq xml.
     */
    private void createCompare(int index, Field field, XMLPlainGenerator xrqOut, XMLGenerator message) throws MessageException
    {
       Node compare = xrqOut.create("COMPARE(" + index+ ")");

       String fieldName =  field.getXMLPath();

       xrqOut.setValue(compare,Compare.FIELD_NODE, fieldName );
       xrqOut.setValue(compare,Compare.TYPE_NODE, "=" );
       
       DataType type = field.getDataType();
       
       if (message.exists(fieldName) )
       {
          xrqOut.setValue(compare,Compare.FIELD_VALUE_NODE, message.getValue(fieldName) );

       }
       // if this is a decimal do not add quotes
       if (type.getType() == DataType.TYPE_DECIMAL) 
           xrqOut.setValue(compare,Compare.LITERAL_INDICATOR_NODE, "true" );    
    }

    /**
     * creates a range xrq xml.
     */
    private void createRange(int index, Field field, XMLPlainGenerator xrqOut, XMLGenerator message) throws MessageException
    {
       Node range = xrqOut.create("RANGE(" + index+ ")");

       String fieldName =  field.getXMLPath();
       String fieldNameFrom =  fieldName +".from";
       String fieldNameTo =  fieldName +".to";

       String format = field.getDataType().getFormat();

       if (!StringUtils.hasValue(format) )
          format = XrqConstants.DATE_FLAG;
       else
          format = format;


       xrqOut.setValue(range,Range.FIELD_NODE, fieldName );
       xrqOut.setValue(range,XrqConstants.DATE_FORMAT_NODE, format );

       if (message.exists(fieldNameFrom) )
          xrqOut.setValue(range,Range.FIELD_FROM_NODE, message.getValue(fieldNameFrom) );


       if (message.exists(fieldNameTo) )
          xrqOut.setValue(range,Range.FIELD_TO_NODE, message.getValue(fieldNameTo) );


    }



    public void release()
    {
       super.release();
       curPart = null;
       message = null;
       varSelection = null;
    }



}

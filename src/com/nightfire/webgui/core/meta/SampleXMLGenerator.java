/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports
import java.text.*;
import java.util.*;

// third party imports
import org.w3c.dom.*;

// nightfire imports

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.MessageGeneratorException;

/**
 * Generates a sample XML from a schema
 */
public class SampleXMLGenerator
{
    /** Generate a sample XML to use for a template */
    public static final int STYLE_TEMPLATE = 0;

    /** Generate a sample XML to use for style sheet testing */
    public static final int STYLE_TEST_XSL = 1;

    /** Generate a sample XML with empty values **/
    public static final int STYLE_EMPTY_FIELD_VALUES = 2;

    /** The GUI infrastructure apparently has a hard-coded document element
        name */
    private static final String ROOT_NODE_NAME = "Body";

    /** The Message to use */
    private Message msg;

    /** The counter that helps us generate values **/
    private int nextValue = 1;

    /** The sample style to create */
    private int style;

    /** Keeps track of ids used */
    private HashSet ids = new HashSet();

    /** The number of instances of a container to create when it repeats */
    private int repeatCount = 2;

    private XMLPlainGenerator repeatCountTemplate = null;

     

    /**
     * Create a generator
     *
     * @param file  The ToolFile to use for generation
     * @param style The style to generate, must be STYLE_TEMPLATE, or
     *              STYLE_TEST_XSL.
     */
    public SampleXMLGenerator(Message msg, int style)
    {
        this.msg   = msg;
        this.style = style;
    }

    /**
     * Create a generator
     *
     * @param msg a <code>Message</code> value
     * @param style The style to generate, must be STYLE_TEMPLATE, or
     *              STYLE_TEST_XSL.
     * @param template Uses this xml file to determine the number of instances
     * of repeating nodes. If the template does not have the matching structure then
     * the default (set by setRepeatCount()) repeating count will be used.
     */
    public SampleXMLGenerator(Message msg, int style, Document template)
    {
        this(msg,style, new XMLPlainGenerator(template)); 
    }

    /**
     * Create a generator
     *
     * @param msg a <code>Message</code> value
     * @param style The style to generate, must be STYLE_TEMPLATE, or
     *              STYLE_TEST_XSL.
     * @param template Uses this xml file to determine the number of instances
     * of repeating nodes. If the template does not have the matching structure then
     * the default (set by setRepeatCount()) repeating count will be used.
     */
    public SampleXMLGenerator(Message msg, int style, String template) throws MessageGeneratorException
    {

        this.msg   = msg;
        this.style = style;

        try {           
            repeatCountTemplate = new XMLPlainGenerator(template);
        }
        catch (MessageGeneratorException e) {
            throw e;
        }
        
    }


    /**
     * Create a generator
     *
     * @param msg a <code>Message</code> value
     * @param style The style to generate, must be STYLE_TEMPLATE, or
     *              STYLE_TEST_XSL.
     * @param template Uses this xml file to determine the number of instances
     * of repeating nodes. If the template does not have the matching structure then
     * the default (set by setRepeatCount()) repeating count will be used.
     */
    public SampleXMLGenerator(Message msg, int style, XMLPlainGenerator template)
    {
        this(msg,style);
        repeatCountTemplate = template;
    }


    /**
     * Returns the default number of instances that are added when repeating
     * containers (Repeatable Forms, Sections, or Repeating Sub Sections)
     * are encountered.
     */
    public int getRepeatCount()
    {
        return repeatCount;
    }

    /**
     * Returns the number of instances to add for repeating containers 
     * (Repeatable Forms, Sections, or Repeating Sub Sections) based on
     * a xml path.
     * Returns the default count if no node exists (getRepeatCount()).
     *
     * @param parent The parent node of the repeating child node.
     * @param childName The name of the child node that is repeating.
     * @return The number of instances.
     */
    public int getRepeatCount(String parentPath, String childName) 
    {


        // if the child name is a sub path then move the path part to
        // the parent path.
        // for example. lets say that 
        // parentPath = Request.lsr_preorder.RequestBody.loop_qualification
        // and childName = SupplierLSRPreorderRequest.WTNcontainer.WTN
        // we would change it to 
        //  parentPath = Request.lsr_preorder.RequestBody.loop_qualification
        //  .SupplierLSRPreorderRequest.WTNcontainer
        // and childName = WTN
        int index = childName.lastIndexOf(".");
        
        if (index > 0 ) {
            parentPath = parentPath + "." + childName.substring(0, index);
            childName = childName.substring(index+1);
        }


       int count = repeatCount;
        

       if(repeatCountTemplate != null && 
          repeatCountTemplate.exists(parentPath) ) {
           try {
               Node parent = repeatCountTemplate.getNode(parentPath);
           
               count = repeatCountTemplate.getChildCount(parent, childName);
           }
           catch (MessageException e) {
               count = 0;
           }
           

           if ( count < 1)
               count = repeatCount;
       }
       
       return count;
        
    }

    /**
     * Sets the number of instances that are added when repeating
     * containers (Repeatable Forms, Sections, or Repeating Sub Sections)
     * are encountered.
     */
    public void setRepeatCount(int count)
    {
        repeatCount = count;
        if ( repeatCount < 1)
            repeatCount = 1;
        
    }

    /**
     * Generates a sample XML
     *
     * @exception FrameworkException Thrown if there is an error with the
     *                               schema.
     */
    public Document generate() throws FrameworkException
    {
        ids.clear();

        // generate the message
        return buildDoc(msg);
    }

    /**
     * Builds the document from a ToolMessage
     */
    private Document buildDoc(Message msg) throws FrameworkException
    {
        XMLPlainGenerator gen  = new XMLPlainGenerator(ROOT_NODE_NAME);
        Node msgElem = gen.getNode(gen.getDocument().getDocumentElement(),
                                   msg.getXMLPath(), true);

        // traverse the schema
        Iterator iter = msg.getChildren().iterator();
        while (iter.hasNext())
        {
            MessagePart part = (MessagePart)iter.next();
            partToDoc(part, msgElem, gen);
        }

        return gen.getDocument();
    }

    /**
     * Adds in the elements for a given message part.
     */
    private void partToDoc(MessagePart part, Node parentElem,
                           XMLPlainGenerator gen)
        throws FrameworkException
    {
        if ((part instanceof Form) || (part instanceof Section))
            repeatableContainerToDoc((MessageContainer)part, parentElem, gen);
        else if (part instanceof RepeatingSubSection)
            subSectionToDoc((RepeatingSubSection)part, parentElem, gen);
        else if (part instanceof Field)
            fieldToDoc((Field)part, parentElem, gen);
        else if (part instanceof MessageContainer)
            containerToDoc((MessageContainer)part, parentElem, gen);
        else
        {
            // this probably indicates an invalid schema, it isn't a
            // fatal error, but we'll output a comment to clearly indicate
            // to the user that something isn't right
            String msg = "ERROR: Unexpected type [" +
                part.getClass().getName() + "] at: " + showPath(part);

            Comment cmt = parentElem.getOwnerDocument().createComment(msg);
            parentElem.appendChild(cmt);
        }
    }


    
        

    /**
     * Adds in the elements for a repeatable container.
     */
    private void repeatableContainerToDoc(MessageContainer container,
                                          Node parentElem,
                                          XMLPlainGenerator gen)
        throws FrameworkException
    {

        String parentXMLPath = gen.getXMLPath(parentElem);
        
        String xmlPath = container.getXMLPath();
        
        // if this is not repeating default to just one
        int count = 1;
        
        // if this is repeating then find out home many times to repeat
        if (container.getRepeatable())
            count = getRepeatCount(parentXMLPath, xmlPath);

        int idx = 0;
        for (; count > 0; count--)
        {
            // create the element
            Node newParent = parentElem;
            if (xmlPath != null)
                newParent = gen.getNode(parentElem,
                                        xmlPath + '(' + (idx++) + ')', true);

            // add the children
            Iterator iter = container.getChildren().iterator();
            while (iter.hasNext())
            {
                MessagePart part = (MessagePart)iter.next();
                partToDoc(part, newParent, gen);
            }
        }
    }

    /**
     * Adds in the elements for a given repeating sub-section.
     */
    private void subSectionToDoc(RepeatingSubSection subSection,
                                 Node parentElem, XMLPlainGenerator gen)
        throws FrameworkException
    {
        String parentXMLPath = gen.getXMLPath(parentElem);

        String xmlPath = subSection.getXMLPath();
        
        int count = getRepeatCount(parentXMLPath, xmlPath);
        

        int idx = 0;
        for (; count > 0; count--)
        {
            // create the element
            Node newParent = gen.getNode(parentElem,
                xmlPath + '(' + (idx++) + ')', true);

            // add the children
            Iterator iter = subSection.getChildren().iterator();
            while (iter.hasNext())
            {
                MessagePart part = (MessagePart)iter.next();
                partToDoc(part, newParent, gen);
            }
        }
    }

    /**
     * Adds in the elements for a non-repeatable container.
     */
    private void containerToDoc(MessageContainer container, Node parentElem,
                                XMLPlainGenerator gen)
        throws FrameworkException
    {
        // create the element
        String xmlPath = container.getXMLPath();
        Node newParent = parentElem;
        if (xmlPath != null)
            newParent = gen.getNode(parentElem, xmlPath, true);

        // add the children
        Iterator iter = container.getChildren().iterator();
        while (iter.hasNext())
        {
            MessagePart part = (MessagePart)iter.next();
            partToDoc(part, newParent, gen);
        }
    }
      
    /**
     * Adds in the elements for a given field.
     */
    private void fieldToDoc(Field field, Node parentElem,
                            XMLPlainGenerator gen)
        throws FrameworkException
    {
        // create the element
        Element fieldElem =
            (Element)gen.getNode(parentElem, field.getXMLPath(), true);

        // set the value
        if(style == STYLE_EMPTY_FIELD_VALUES)
           fieldElem.setAttribute("value", "");
        else
           fieldElem.setAttribute("value", getOutputValue(field.getDataType()));

        // set the id (only for XSL testing)
        if (style == STYLE_TEST_XSL)
        {
            // pick a unique id
            String baseId = gen.getXMLPath(fieldElem);
            String xmlId = baseId;
            int counter = 1;
            while (ids.contains(xmlId))
                xmlId = baseId + "#" + (++counter);
            ids.add(xmlId);

            // set the chosen id
            fieldElem.setAttribute("id", xmlId);
        }
    }

    /**
     * Creates a path string representing the path to a MessagePart
     */
    private String showPath(MessagePart part)
    {
        StringBuffer buff = new StringBuffer();
        ArrayList path = new ArrayList();
        MessagePart mp = part;

        while (mp != null)
        {
            path.add(mp);
            mp = mp.getParent();
        }

        int len = path.size();
        for (int i = len - 1; i >= 0; i--)
            buff.append('/').append( ((MessagePart)path.get(i)).getID() );

        return buff.toString();
    }

    /**
     * Gets a value to use in the output for a DataType
     */
    private String getOutputValue(DataType type)
    {
        switch (type.getType())
        {
        case DataType.TYPE_ENUMERATED:
            return getEnumValue(type);

        case DataType.TYPE_DATE:
            return getDateValue(type);

        case DataType.TYPE_DECIMAL:
            return getDecimalValue(type);

        default:
            return getTextValue(type);
        }
    }

    /**
     * Gets a text value for use in the output
     */
    private String getTextValue(DataType type)
    {
        // go with a format string first
        String fmt = type.getFormat();
        int maxLen = type.getMaxLen();

        if (!StringUtils.hasValue(fmt))
        {
            // otherwise, make one up
            int len = type.getMinLen();
            if (len == DataType.UNSPECIFIED)
                len = 2;
            if ( (maxLen != DataType.UNSPECIFIED) &&
                 ( (len + 4) > maxLen) && (maxLen > 4) )
                len = maxLen - 4;

            fmt = "'DATA'" + buildString('0', len);
        }

        DecimalFormat df = new DecimalFormat(fmt);
        String val = df.format(nextValue++);

        if ((maxLen != DataType.UNSPECIFIED) && (val.length() > maxLen))
            return val.substring(0, maxLen);
        else
            return val;
    }

    /**
     * Gets a date value for use in the output
     */
    private String getDateValue(DataType type)
    {
        // go with a format string first
        String fmt = type.getFormat();

        if (!StringUtils.hasValue(fmt))
            // otherwise, make one up
            fmt = "yyyy-MM-dd-hhmma";

        SimpleDateFormat df = new SimpleDateFormat(fmt);
        String val = df.format(new Date());

        int maxLen = type.getMaxLen();
        if ((maxLen != DataType.UNSPECIFIED) && (val.length() > maxLen))
            return val.substring(0, maxLen);
        else
            return val;
    }

    /**
     * Gets a decimal value for use in the output
     */
    private String getDecimalValue(DataType type)
    {
        // go with a format string first
        String fmt = type.getFormat();

        if (!StringUtils.hasValue(fmt))
        {
            // otherwise, make one up
            int len = type.getMinLen();
            if (len == DataType.UNSPECIFIED)
                len = 2;
            fmt = buildString('0', len);
        }

        DecimalFormat df = new DecimalFormat(fmt);
        String val = df.format(nextValue++);

        int maxLen = type.getMaxLen();
        if ((maxLen != DataType.UNSPECIFIED) && (val.length() > maxLen))
            return val.substring(0, maxLen);
        else
            return val;
    }

    /**
     * Gets an enum value for use in the output
     */
    private String getEnumValue(DataType type)
    {
        String val = null;
        try
        {
            val = type.getOptionSource().getOptionValues()[0];
        }
        catch (Exception ex)
        {
            // fall through
        }
        
        if (val != null)
            return val;
        else
            return "";
    }

    /**
     * Builds a string of count length, using the character c.
     *
     * @param c     The character to use in the string
     * @param count The number of times to repeat the character
     */
    private String buildString(char c, int count)
    {
        StringBuffer buff = new StringBuffer(count);
        for (; count > 0; count--)
            buff.append(c);

        return buff.toString();
    }
}

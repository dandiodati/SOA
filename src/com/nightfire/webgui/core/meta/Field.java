/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;

/**
 * Field is the most granular message part.  It contains no other message
 * parts.  Fields may occur within {@link Section Sections},
 * {@link FieldGroup FieldGroups}, and
 * {@link RepeatingSubSection RepeatingSubSections}.
 */
public class Field extends MessagePart
{
    /**
     * Data type information
     */
    protected DataType dataType = null;

    /**
     * HashMap containing any custom name / value mappings that are part
     * of the Field
     */
    protected HashMap customValues = new HashMap();

    /**
     * Unmodifiable view of customValues
     */
    protected Map readOnlyCustomValues = null;

    /**
     * Default constructor
     */
    public Field()
    {
    }

    /**
     * Returns the DataType associated with the Field
     */
    public DataType getDataType()
    {
        return dataType;
    }

    /**
     * Returns the custom value associated with <code>name</code>.
     *
     * @param name  The name of the custom value to return
     */
    public String getCustomValue(String name)
    {
       return (String) customValues.get(name);
    }


    /**
     * Returns all custom values for this field.
     *
     * @return Map of custom values.
     */
    public Map getCustomValues()
    {
        if (readOnlyCustomValues == null)
            readOnlyCustomValues = Collections.unmodifiableMap(customValues);

        return readOnlyCustomValues;
    }

    /**
     * Read this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     * @param path   Any path information to prepend to the definition
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readFromXML(Node ctx, Message msg, PathElement path)
        throws FrameworkException
    {
        // read the common sections first
        super.readFromXML(ctx, msg, path);

        MessagePaths xpaths = msg.getXPaths();

        // data type
        List nodes = xpaths.dataTypePath.getNodeList(ctx);
        if (nodes.size() > 0)
        {
            Node n = (Node)nodes.get(0);
            
            // create a new type
            dataType = new DataType();

            // load it
            dataType.readFromXML(n, msg);
        }

 
        // custom values
        List custVals = xpaths.customPath.getNodeList(ctx);
        int len = custVals.size();
        for (int i = 0; i < len; i++)
        {
            Node custNode = (Node)custVals.get(i);
            String custName = xpaths.customNamePath.getValue(custNode);
            String custVal  = xpaths.customValuePath.getValue(custNode);
            
            customValues.put(custName,custVal);

        }
    }

    /**
     * Read extensions to this object from an XML document
     *
     * @param ctx    The context node (Form element) in the XML document
     * @param msg    The containing message object
     * @param path   Any path information to prepend to the definition
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readExtensions(Node ctx, Message msg, PathElement path)
        throws FrameworkException
    {
        // update common sections first
        super.readExtensions(ctx, msg, path);

        MessagePaths xpaths = msg.getXPaths();
        DataType oldDataType = dataType;

        // data type
        List nodes = xpaths.dataTypePath.getNodeList(ctx);
        if (nodes.size() > 0)
        {
            Node n = (Node)nodes.get(0);
            
            // create a new type
            dataType = new DataType();

            // load it
            dataType.readFromXML(n, msg);

            // set the old data type as the base
            // -- unless the base type was set explicitly
            if (dataType.getBaseType() == null)
                dataType.setBaseType(oldDataType);
        }

        
        // custom values
        // CR 18680: Since this field is redefined in the extension, it should have 
        // definie its own customer values.
        customValues.clear();
        List custVals = xpaths.customPath.getNodeList(ctx);
        int len = custVals.size();
        for (int i = 0; i < len; i++)
        {
            Node custNode = (Node)custVals.get(i);
            String custName = xpaths.customNamePath.getValue(custNode);
            String custVal  = xpaths.customValuePath.getValue(custNode);
            
            customValues.put(custName, custVal);
        }
    }
    
    public Object clone() throws CloneNotSupportedException
    {
        Field newField = (Field) super.clone();

        // CR 1868): clone custom values also.
        if (this.customValues != null)
        {
            newField.customValues = (HashMap) this.customValues.clone();
        }
        
        return newField;
    }
}

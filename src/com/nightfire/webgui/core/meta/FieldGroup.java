/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.FrameworkException;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;

/**
 * FieldGroup is a collection of Fields which are grouped together and
 * appear with a title when displayed on a Section.  It's sort of a like what
 * Microsoft refers to as a &quot;control group.&quot; FieldGroup may only
 * occur within a {@link Section}.
 * <p>
 * FieldGroups contain {@link Field Fields}.
 */
public class FieldGroup extends MessageContainer
{
    /**
     * Default constructor
     */
    public FieldGroup()
    {
    }

    /**
     * HashMap containing any custom name / value mappings that are part
     * of the FieldGroup
     */
    protected HashMap customValues = new HashMap();

    /**
     * Unmodifiable view of customValues
     */
    protected Map readOnlyCustomValues = null;

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
     * Returns all custom values for this fieldGroup.
     *
     * @return Map of custom values.
     */
    public Map getCustomValues()
    {
        if (readOnlyCustomValues == null)
            readOnlyCustomValues = Collections.unmodifiableMap(customValues);

        return readOnlyCustomValues;
    }

    public void readFromXML(Node ctx, Message msg, PathElement path)
        throws FrameworkException
    {
        // read the common sections first
        super.readFromXML(ctx, msg, path);

        MessagePaths xpaths = msg.getXPaths();

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

        // custom values
        // CR 18680: Since this field is redefined in the extension, it should have
        // define its own custome values.
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
        FieldGroup newFieldGrp = (FieldGroup) super.clone();

        // CR 1868): clone custom values also.
        if (this.customValues != null)
        {
            newFieldGrp.customValues = (HashMap) this.customValues.clone();
        }

        return newFieldGrp;
    }}

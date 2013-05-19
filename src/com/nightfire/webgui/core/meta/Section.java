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
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;

/**
 * Section is a MessagePart occuring within a {@link Form}.  It is grouping
 * of fields wich are presented together to a user.
 * <p>
 * Sections may contain any of the following: {@link Field Fields},
 * {@link FieldGroup FieldGroups},
 * {@link RepeatingSubSection RepeatingSubSections}.
 */
public class Section extends MessageContainer
{
    /*
     * Location in the XML document
     */
    protected static final String ID_FIELD_CONTAINER_LOC = "IdFieldcontainer";

    /**
     * A list of field names which provide the id for this section (sort
     * of like a primary key, but nothing enforceable -- it's used only for
     * display purposes)
     */
    protected String[] idFields = null;
    
    /**
     * HashMap containing any custom name / value mappings that are part
     * of the Section
     */
    protected HashMap customValues = new HashMap();

    /**
     * Unmodifiable view of customValues
     */
    protected Map readOnlyCustomValues = null;

    /**
     * Default constructor
     */
    public Section()
    {
    }

    /**
     * Returns the list of ID Fields for the Section
     */
    public String[] getIdFields()
    {
        return idFields;
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
     * Returns all custom values for this section.
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
     * @param ctx    The context node (Section element) in the XML document
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

        // id fields
        idFields = xpaths.idFieldsPath.getValues(ctx);

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

        // id fields
        String[] newIdFields = xpaths.idFieldsPath.getValues(ctx);
        if (newIdFields.length != 0)
            idFields = newIdFields;

        // custom values
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
}

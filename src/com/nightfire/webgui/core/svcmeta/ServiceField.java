/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.svcmeta;

// jdk imports
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.util.xml.DOMWriter;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.core.meta.*;


/**
 * ServiceField is a {@link Field} that is service related.
 */
public class ServiceField extends Field
{
    /**
     * Read this object from an XML document
     *
     * @param ctx      The context node (Field element) in the XML document
     * @param buildCtx The build context
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readFromXML(Node ctx, BuildContext buildCtx)
        throws FrameworkException
    {
        path = new PathElement();
        path.part = this;

        BuildPaths xpaths = buildCtx.xpaths;

        // read the common sections first

        // name
        id = getString(xpaths.idPath, ctx);
        path.name = stripIdSuffix(id);
        buildCtx.registerMessagePart(this);

        // displayName
        displayName = getString(xpaths.displayNamePath, ctx);

        // fullName
        fullName = getString(xpaths.fullNamePath, ctx);

        // abbreviation
        abbreviation = getString(xpaths.abbreviationPath, ctx);

        // helpText node
        List helpNodes = xpaths.helpPath.getNodeList(ctx);
        if (helpNodes.size() > 0)
        {
            Node helpNode = (Node)helpNodes.get(0);
            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }

        // data type
        List nodes = xpaths.dataTypePath.getNodeList(ctx);
        if (nodes.size() > 0)
        {
            Node n = (Node)nodes.get(0);
            
            // create a new type
            dataType = new ServiceDataType();

            // load it
            ((ServiceDataType)dataType).readFromXML(n, buildCtx);
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
   * Set a custom value on this bundle field.
   *
   * @param name The custom value name
   * @param value The value to set
   */
  public void setCustomValue(String name, String value)
    {
      customValues.put(name, value);
    }
  

    /**
     * Strips an ID string of any # character and anything that follows.  To
     * allow unique ids to be used for parts with the same XML element name,
     * # is allowed in an id, but not retained as part of the XML path
     */
    private String stripIdSuffix(String id)
    {
        if (id == null)
            return id;

        int invalidIdx = id.indexOf('#');

        if (invalidIdx == 0)
            return "";

        if (invalidIdx > -1)
            return id.substring(0, invalidIdx);

        return id;
    }

    /**
     * Make a copy of this ServiceField with the specified data type.
     *
     */
    public ServiceField copy() throws FrameworkException
    {

      try {
         ServiceField newField = (ServiceField) clone();

         newField.dataType = getDataType();

         newField.path  = path.copy();
         newField.setParent(this.getParent());
         Message msg = getRoot();
         if (msg != null)
            newField.generateUniqueId(msg);

         return newField;
      } catch (CloneNotSupportedException ce) {
        throw new FrameworkException("Failed to clone ServiceField : " + ce.getMessage() );
      }

    }


    public void setDataType(DataType dType)
    {
       dType.setBaseType(this.dataType);
       this.dataType = dType;
    }

    public void setParentPath( MessagePart.PathElement path)
    {
       this.path.parent = path;
    }


}

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
 * ServiceDataType is a {@link DataType} that occurs inside a {@link ServiceField}.
 */
public class ServiceDataType extends DataType
{
    /**
     * Read this object from an XML document
     *
     * @param ctx      The context node (Form element) in the XML document
     * @param buildCtx The build context
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public void readFromXML(Node ctx, BuildContext buildCtx)
        throws FrameworkException
    {
        BuildPaths xpaths = buildCtx.xpaths;

        // minLen
        minLen = getInt(xpaths.minLengthPath, ctx);

        // maxLen
        maxLen = getInt(xpaths.maxLengthPath, ctx);

        // usage
        String strUsage = getString(xpaths.usagePath, ctx);
        if (strUsage != null)
            usage = USAGE_LIST.indexOf(strUsage);

        // format
        format = getString(xpaths.formatPath, ctx);

        // optionSource
        List optNodes = xpaths.enumPath.getNodeList(ctx);
        if (optNodes.size() > 0)
        {
            Node newCtx = (Node)optNodes.get(0);
            optionSource = new SimpleOptSource();
            ((SimpleOptSource)optionSource).readFromXML(newCtx, buildCtx);
        }

        // examples
        examples = xpaths.examplesPath.getValues(ctx);

        // name
        name = getString(xpaths.idPath, ctx);
        if (name != null)
            buildCtx.registerDataType(this);

        // type & baseType
        String strType = getString(xpaths.baseTypePath, ctx);
        if (strType != null)
        {
            // see if this is a fundamental type
            int idxType = TYPE_LIST.indexOf(strType);
            if (idxType != -1)
            {
                type = idxType;
            }
            else
            {
                // this should be the name of a base type
                baseType = buildCtx.getDataType(strType);

                if (baseType == null)
                    log.warn(
                              "No match for base data type \"" + strType
                              + "\" at \"" + buildCtx.toXPath(ctx) + "\".");
            }
        }
    }
        
    /**
     * A simple implementation of an OptionSource.
     */
    private class SimpleOptSource implements OptionSource
    {
        /** Option values */
        private String[] optionValues = null;
        
        /** Display values */
        private String[] displayValues = null;

        /** Descriptions */
        private String[] descriptions = null;
        
        /**
         * Returns the list of option values from this source.
         *
         * @return An array of option text values
         */
        public String[] getOptionValues()
        {
            return optionValues;
        }

        /**
         * Returns the list of display values associated with the options.
         * The length of the list returned must match the length of the
         * list returned by getOptionValues().
         *
         * @return An array of option display values
         */
        public String[] getDisplayValues()
        {
            return displayValues;
        }

        /**
         * Returns the list of descriptions (help text) associated with the
         * options.  The length of the list returned must match the length
         * of the list returned by getOptionValues().
         *
         * @return An array of option descriptions
         */
        public String[] getDescriptions()
        {
            return descriptions;
        }

        /**
         * Read this object from an XML document
         *
         * @param ctx    The context node (Options element) in the XML
         *               document
         * @param msg    The containing message object
         *
         * @exception FrameworkException Thrown if the XML document is
         *                               invalid or there is an error
         *                               accessing it
         */
        public void readFromXML(Node ctx, Message msg)
            throws FrameworkException
        {
        }

        /**
         * Read this object from an XML document
         *
         * @param ctx      The context node (Options element) in the XML
         *                 document
         * @param buildCtx The build context
         *
         * @exception FrameworkException Thrown if the XML document is
         *                               invalid or there is an error
         *                               accessing it
         */
        public void readFromXML(Node ctx, BuildContext buildCtx)
            throws FrameworkException
        {
            BuildPaths xpaths = buildCtx.xpaths;
            
            optionValues  = xpaths.optionValuesPath.getValues(ctx);
            displayValues = xpaths.displayValuesPath.getValues(ctx);
            descriptions  = xpaths.descriptionsPath.getValues(ctx);
            
            if ( (optionValues.length != displayValues.length) ||
                 (optionValues.length != descriptions.length) )
                throw new FrameworkException("Count of option values ("
                    + optionValues.length + "), display values ("
                    + displayValues.length + "), and descriptions ("
                    + descriptions.length + ") must match.");
        }
    }
}

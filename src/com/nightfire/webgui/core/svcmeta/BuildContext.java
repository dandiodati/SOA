/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.svcmeta;

// jdk imports
import java.io.*;
import java.net.*;
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.util.xml.DOMWriter;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.debug.*;



    /**
     * Maintains context information while constructing a Service object from
     * an XML document
     */
    public class BuildContext
    {
        /** Index of data types */
        private HashMap dataTypes = new HashMap();

        /** Index of message parts */
        private HashMap parts = new HashMap();

        /** Index of actions */
        private HashMap actions = new HashMap();

        /** Index of message types */
        private HashMap msgTypes = new HashMap();

   

        /** XPaths used for reading in a BundleDef */
        public BuildPaths xpaths;

        /** The base URL the file was loaded from */
        public URL url = null;

        /** The encoding used to load the URL contents */
        public String encoding = null;

        protected DebugLogger log;

        /**
         * Constructor
         */
        public BuildContext() throws FrameworkException
        {
            log = DebugLogger.getLoggerLastApp(getClass() );

            xpaths = new BuildPaths();
        }

        /**
         * Registers an messagetype definition
         */
        public void registerMessageType(MessageTypeDef def)
        {
            String name = def.getName();
            if (!StringUtils.hasValue(name))
                return;

            if (msgTypes.containsKey(name))
                log.warn("Overriding Message Type with name [ "
                          + name + "].");
            msgTypes.put(name, def);
        }

        /**
         * Returns a registered message type, or null if not found
         */
        public MessageTypeDef getMessageType(String name)
        {
            if (!msgTypes.containsKey(name))
                return null;

            return (MessageTypeDef)msgTypes.get(name);
        }

        /**
         * Registers an action definition
         */
        public void registerAction(ActionDef def)
        {
            String name = def.getActionName();
            if (!StringUtils.hasValue(name))
                return;

            if (actions.containsKey(name))
                log.warn("Overriding Action with name [ "
                          + name + "].");

            actions.put(name, def);
        }

        /**
         * Returns a registered action, or null if not found
         */
        public ActionDef getAction(String name)
        {
            if (!actions.containsKey(name))
                return null;

            return (ActionDef)actions.get(name);
        }

   

        /**
         * Registers a global data type
         */
        public void registerDataType(DataType type)
        {
            String name = type.getName();
            if (!StringUtils.hasValue(name))
                return;

            if (dataTypes.containsKey(name))
                log.warn(
                          "Overriding Data Type with name [ " + name + "].");

            dataTypes.put(name, type);
        }

        /**
         * Returns a registered data type, or null if not found.
         */
        public DataType getDataType(String name)
        {
            if (!dataTypes.containsKey(name))
                return null;

            return (DataType)dataTypes.get(name);
        }


        /**
         * Registers a message part
         */
        public void registerMessagePart(MessagePart part)
        {
            String id = part.getID();
            if (!StringUtils.hasValue(id))
                return;

            if (parts.containsKey(id))
                log.warn(
                          "Overriding Message Part with id [ " + id + "].");

            parts.put(id, part);
        }

        /**
         * Returns a registered data type, or null if not found.
         */
        public MessagePart getMessagePart(String id)
        {
            if (!parts.containsKey(id))
                return null;

            return (MessagePart)parts.get(id);
        }

        /**
         * Gets the String value from a ParsedXPath, returning null if no value
         * is present
         */
        public String getString(ParsedXPath xpath, Node ctx)
            throws FrameworkException
        {
            List nodes = xpath.getNodeList(ctx);
            if (nodes.size() < 1)
                return null;
            else
                return ((Node)nodes.get(0)).getNodeValue();
        }

        /**
         * Gets the integer value from a ParsedXPath, returning -1 if no value
         * is present
         */
        public int getInt(ParsedXPath xpath, Node ctx)
            throws FrameworkException
        {
            String strVal = getString(xpath, ctx);
            if (strVal == null)
                return -1;
            return StringUtils.getInteger(strVal);
        }

        /**
         * Utility for getting the XPath to a given node
         */
        public String toXPath(Node n)
        {
            StringBuffer buff = new StringBuffer();
            toXPath(buff, n);
            return buff.toString();
        }

        /**
         * Adds the name of a node to a buffer where an XPath expression
         * is being built.
         */
        private void toXPath(StringBuffer buff, Node n)
        {
            // first get the parent
            Node parent = n.getParentNode();
            if ((parent != null) && !(parent instanceof Document))
                toXPath(buff, parent);

            buff.append('/');

            buff.append(n.getNodeName());
            int idx = getNodeIndex(n);
            if (idx != -1)
                buff.append('[').append(idx).append(']');
        }

        /**
         * Determines any index to use with a node.  A return value of
         * -1 indicates it is the only sibling with that name.
         */
        private int getNodeIndex(Node n)
        {
            int count = 0;

            // look at preceding siblings
            Node sib = n.getPreviousSibling();
            while (sib != null)
            {
                if (sib.getNodeName().equals(n.getNodeName()))
                    count++;
                sib = sib.getPreviousSibling();
            }

            if (count > 0)
                return count;

            // look at following siblings
            sib = n.getNextSibling();
            while (sib != null)
            {
                if (sib.getNodeName().equals(n.getNodeName()))
                    return 0;
                sib = sib.getNextSibling();
            }

            return -1;
        }

        public static final class InvalidSvcException extends FrameworkException
        {


            public InvalidSvcException(String msg)
            {
                super(msg);
            }
        }
    }

 



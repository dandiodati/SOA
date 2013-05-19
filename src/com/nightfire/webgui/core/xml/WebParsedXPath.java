/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.xml;

// jdk imports
import java.util.Iterator;
import java.util.List;

// third party imports
import org.jaxen.BaseXPath;
import org.jaxen.FunctionCallException;
import org.jaxen.JaxenException;
import org.jaxen.Navigator;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.saxpath.SAXPathException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// nightfire imports
import com.nightfire.framework.message.util.xml.NamespaceMap;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.message.common.xml.*;

/**
 * WebParsedXPath is an extension of ParsedXPath which allows navigation over
 * an XMLGeneratorGroup through the use of the document function.  You can
 * access another document in the group by using &quot;group&quot; as the
 * protocol and the document id as the host in the URL.  For example:
 * &quot;<i>document('group://OtherDoc/')/a/b/c</i>&quot; returns the value
 * at path /a/b/c from the OtherDoc document in the group.
 */
public class WebParsedXPath extends ParsedXPath
{
    // constants
    
    // member variables

    /**
     * The protocol in a URI that specifies getting a document from the
     * group
     */
    protected static final String GROUP_PROTOCOL = "group://";

    /**
     * XPath implementation
     */
    protected NFXPath xpath = null;

    /**
     * Navigator
     */
    protected WebDOMNavigator webNavInstance =
        new WebDOMNavigator();

    /**
     * The group to use for resolving calls to document()
     */
    protected XMLGeneratorGroup group;

    // public methods

    /**
     * Create a new ParsedXPath from the string expression <code>path</code>.
     * This constructor assumes the expression is of type SELECT.
     *
     * @param path The path to parse
     *
     * @exception FrameworkException Thrown in case of a syntax or other error
     */
    public WebParsedXPath(String path) throws FrameworkException
    {
        this(path, null, new NamespaceMap());
    }

    /**
     * Create a new ParsedXPath from the string expression <code>path</code>.
     * This constructor assumes the expression is of type SELECT.
     *
     * @param path The path to parse
     * @param group The XMLGeneratorGroup to use for document() calls, may
     *              be null.
     *
     * @exception FrameworkException Thrown in case of a syntax or other error
     */
    public WebParsedXPath(String path, XMLGeneratorGroup group)
        throws FrameworkException
    {
        this(path, group, new NamespaceMap());
    }

    /**
     * Create a new ParsedXPath from the string expression <code>path</code>
     * using prefix resolver <code>resolver</code>.  This constructor assumes
     * the expression is of type SELECT.
     *
     * @param path The path to parse
     * @param resolver The PrefixResolver to use
     * @param group The XMLGeneratorGroup to use for document() calls, may
     *              be null.
     *
     * @exception FrameworkException Thrown in case of a syntax or other error
     */
    public WebParsedXPath(String path, XMLGeneratorGroup group,
                          NamespaceMap resolver)
        throws FrameworkException
    {
        super();

        try
        {
            this.group = group;

            init(new WebXPath(path), resolver);
        }
        catch (FrameworkException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new FrameworkException("Error creating XPath for [" +
                                         path + "]: " + ex);
        }
    }

    // private and protected methods

    // inner classes

    /**
     * Our implementation of XPath, provides special handling of namespaces
     */
    protected class WebXPath extends NFXPath
    {
        /**
         * Constructor
         *
         * @param xpathExpr  The XPath expression
         *
         * @exception JaxenException Thrown if there is a syntax error
         */
        public WebXPath(String xpathExpr) throws JaxenException,
                                                 SAXPathException
        {
            super(xpathExpr);
        }

        /**
         * Returns the navigator to use
         */
        public Navigator getNavigator()
        {
            return webNavInstance;
        }
    }

    /**
     * Our navigator providing special namespace handling
     */
    protected class WebDOMNavigator extends NFDOMNavigator
    {
        /**
         * Constructor
         */
        public WebDOMNavigator()
        {
            super();
        }

        /**
         * Retrieves a document by URI
         */
        public Object getDocument(String uri) throws FunctionCallException
        {
            if (!StringUtils.hasValue(uri))
                return super.getDocument(uri);

            // check for an attempt to get a document from the group
            if (uri.startsWith(GROUP_PROTOCOL))
            {
                // get the group name
                String grpName = uri.substring(GROUP_PROTOCOL.length());
                if (grpName.charAt(grpName.length() - 1) == '/')
                    grpName = grpName.substring(0, grpName.length() - 1);
                
                if (group == null)
                    return null;
                if (!group.exists(grpName))
                    return null;
                try
                {
                    return group.getGenerator(grpName).getDocument();
                }
                catch (Exception ex)
                {
                    return null;
                }
            }
            else
                return super.getDocument(uri);
        }
    }
}

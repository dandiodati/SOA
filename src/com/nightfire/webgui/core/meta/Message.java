/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports

import java.util.*;
import java.io.*;
import java.net.*;

// third-party imports
import org.w3c.dom.*;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

// nightfire imports
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.PropertyException;


/**
 * Message is the definition of a request and/or response message that may be
 * exchanged with a NightFire gateway.  It contains information about the
 * structure of the XML message as well as additional information about how
 * to display it to the user.
 * <p>
 * A Message contains one or more {@link Form Forms}.
 */
public class Message extends MessageContainer
{
    /*
     * XML Values
     */
    protected static final String MESSAGE_NAME     = "nf:Message";
    protected static final String FORM_NAME        = "nf:Form";
    protected static final String SECTION_NAME     = "nf:Section";
    protected static final String FIELD_NAME       = "nf:Field";
    protected static final String FIELD_GROUP_NAME = "nf:FieldGroup";
    protected static final String REPEATING_SUB_SECTION = "nf:RepeatingSubSection";
    protected static final String DATA_TYPE_NAME   = "nf:DataType";
    protected static final String REF_ATTR_NAME    = "ref";

    private static HashMap commonResourceCache;
    private static HashMap commonResourceModifiedCache;
    private static HashMap commonElemsCache;

    private static boolean develMode;

    /**
     * Globally-defined data types, stored by name
     */
    protected HashMap dataTypes = new HashMap();

    /**
     * Globally-defined message parts, stored by id
     */
    protected HashMap parts = new HashMap();

    /**
     * Globally-defined readonly required fields
     * set later on
     */
    private Map ro_reqFields;
  

    /**
     * XPaths used for building message parts
     */
    protected MessagePaths paths;

    /**
     * Old constructor for an XMLMessageParser
     *    log.debug(" Checking");
     * @param parser XMLMessageParser containing the serialized data model
     *
     * @param FrameworkException Thrown if there is an error with the
     *                           XML document
     *
     * @deprecated {@link #Message(URL)} is now the preferred constructor
     *             for a Message.
     */
    public Message(XMLMessageParser parser) throws FrameworkException
    {
        

        // initialize members (which throw exceptions)
        paths = new MessagePaths();
        
     
        
        // load any shared data types
        HashSet visited = new HashSet();
        loadDefinitions(parser.getDocument(), visited, null, null, false);

        // load the rest
        Node top = paths.messageRootPath.getNode(parser.getDocument());
        path = new PathElement();
        path.name = getString(paths.idPath, top);
        readFromXML(top, this, path);

        if (log.isDebugEnabled())
            log.debug("Loaded Message definition [" + id + "].");
    }

    /**
     * Constructor for a URL.  This creates a Message from the definition at
     * url using a default character encoding a default URL resolution.
     *
     * @param url URL to the document to load
     *
     * @param FrameworkException Thrown if there is an error with the
     *                           XML document
     */
    public Message(URL url) throws FrameworkException
    {
       this(url, (URIResolver)null);
    }

    /**
     * Constructor for a URL and character encoding.  This uses default
     * URL resolution.
     *
     * @param url  URL to the document to load
     * @param enc  The character encoding to use for loading
     *
     * @param FrameworkException Thrown if there is an error with the
     *                           XML document
     *
     * @deprecated Any specified character encoding for a Message is ingored
     *             in favor of the character encoding specified in the
     *             XML document.
     */
    public Message(URL url, String enc) throws FrameworkException
    {
        this(url, (URIResolver)null);
    }

    /**
     * Constructor for a URL, character encoding, and URIResolver.
     *
     * @param url  URL to the document to load
     * @param enc  The character encoding to when reading this url's content.
     * @param resolver The URIResolver to use
     *
     * @param FrameworkException Thrown iparam.NFH_ReadOnlyf there is an error with the
     *                           XML document
     *
     * @deprecated Any specified character encoding for a Message is ingored
     *             in favor of the character encoding specified in the
     *             XML document.
     */
    public Message(URL url, String enc, URIResolver resolver)
        throws FrameworkException
    {
        this(url, resolver);
    }

    /**
     * Constructor for a URL, and URIResolver.
     *
     * @param url  URL to the document to load
     * @param resolver The URIResolver to use
     *
     * @exception FrameworkException Thrown if there is an error with the
     *                           XML document
     */
    public Message(URL url, URIResolver resolver)
        throws FrameworkException
    {
        long start = System.currentTimeMillis();

        // read the document from the url
        MsgResource res = readDocument(null, url.toString(), resolver);

        if (res != null)
        {
            // initialize members (which throw exceptions)
            paths = new MessagePaths();

            // load any shared data types
            HashSet visited = new HashSet();

            loadDefinitions(res.doc, visited, res.href, resolver, false);

            // load the rest
            Node top = paths.messageRootPath.getNode(res.doc);
            path = new PathElement();
            path.name = getString(paths.idPath, top);
            readFromXML(top, this, path);

            long stop = System.currentTimeMillis();

            if (log.isDebugEnabled()) {

                log.debug("Loaded Message definition [" + id + "]. Message definition creation time: " + (stop - start) + " milliseconds.");
            }
        }
        else
            log.warn("Constructor: MsgResource is null for resource: [" + url.toString() + "], so skipping [" + url.toString() + "]" );
    }

    /**
     * Default constructor
     */
    protected Message() throws FrameworkException
    {
        // initialize members (which throw exceptions)
        paths = new MessagePaths();
    }

    /**
     * Obtains a named data type
     */
    public DataType getDataType(String name)
    {
        if (dataTypes.containsKey(name))
            return (DataType)dataTypes.get(name);
        else
            return null;
    }

    /**
     * Obtains a message part by ID or by the full xml path.
     * @return The specified MessagePart or null if not found.
     */
    public MessagePart getMessagePart(String id)
    {
        if (parts.containsKey(id))
            return (MessagePart)parts.get(id);
        else {
            return getPartAtPath(id);
        }
    }

    /**
     * Returns a map of all required fields in this message object.
     * @return A map with all required fields.
     */
    public Map getRequiredFields()
    {
        if ( ro_reqFields == null) {
            Map temp = new HashMap();
            findRequiredFields(temp,this);
            ro_reqFields = Collections.unmodifiableMap(temp);
        }
        
        return ro_reqFields;
    }
 
    /**
     * Recursively find all current required fields for this message
     *
     * @param reqFields a <code>Map</code> value
     * @param part a <code>MessagePart</code> value
     */
    private void findRequiredFields(Map reqFields, MessagePart part)
    {
        if (part instanceof Field) {
            Field f = (Field)part;
            DataType d= f.getDataType();
            
            if (d != null && d.getUsage() == DataType.REQUIRED ) 
                reqFields.put(part.getID() , part);
        }
        else if (part instanceof MessageContainer) {
            MessageContainer container = (MessageContainer) part;
            Iterator childIter = container.getChildren().iterator();
            while (childIter.hasNext() ) {
                findRequiredFields(reqFields, (MessagePart)childIter.next());
            }
        } 
    }
    
        
            

    /**
     * Obtains a message part by XML Path (dot-separated).
     *
     * @return The specified MessagePart or null if not found
     */
    public MessagePart getPartAtPath(String id)
    {
        if ((path == null) || (id == null))
            return null;

        // get the name of the first child in the path
        String searchName = id;
        String nextName = null;
        int idx = id.indexOf('.');
        if (idx != -1)
        {
            searchName = id.substring(0, idx);
            if (idx < id.length() - 1)
                nextName = id.substring(idx + 1);
        }

        // see if this includes the root of the path
        PathElement elem;
        if (searchName.equals(path.name))
            elem = path.getChild(nextName);
        else
            elem = path.getChild(id);

        return (elem == null) ? null : elem.part;
    }


    /**
     * Returns the MessagePaths object associated with the message (for
     * use by classes which extend MessagePart).
     */
    public MessagePaths getXPaths()
    {
        return paths;
    }

    /**
     * Obtains a message part from an XML document
     *
     * @param ctx     The context node of the message part to obtain
     * @param curPath Any path information to prepend to the definition
     * @param parent  The parent for the parts
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public MessagePart[] readPartsFromXML(Node ctx, PathElement curPath, MessageContainer parent)
        throws FrameworkException
    {
        return readPartsFromXML (ctx, curPath, parent, false, null);
    }

    /**
     * Obtains a message part from an XML document
     *
     * @param ctx     The context node of the message part to obtain
     * @param curPath Any path information to prepend to the definition
     * @param parent  The parent for the parts
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    public MessagePart[] readPartsFromXML(Node ctx, PathElement curPath, MessageContainer parent, boolean isCommon, String url)
        throws FrameworkException
    {
        MessagePart part = null;

        // see if this is a reference
        String refName = ((Element)ctx).getAttribute(REF_ATTR_NAME);

        if (StringUtils.hasValue(refName))
        {
            part = getMessagePart(refName);

            // if part is null, it means we couldn't find the named part
            if (part == null)
            {
                log.warn("readPartsFromXML: Invalid reference name, no such " + "message part: [" + refName + "], so skipping [" + refName + "] reference.");
                return null;
            }

            // we need a unique copy
            if (part instanceof ComplexType)
                return ((ComplexType)part).copyChildren(parent, curPath, this);
            else
                part = part.copy(parent, curPath, this);
            
            MessagePart[] parts = {part};
            return parts;
        }

        // see if this is an extension of an existing type
        String extBase = getString(paths.extensionPath, ctx);
        if (StringUtils.hasValue(extBase))
        {
            part = getMessagePart(extBase);

            // if part is null, it means we couldn't find the named part
            if (part == null)
                throw new MessageException("Invalid extension base, no such " + "message part: [" + extBase + "]");

            // we need a unique copy
            part = part.copy(parent, curPath, this, true);

            // load the extensions
            part.readExtensions(ctx, this, part.path);
            
            if (part instanceof ComplexType)
            {
                List list = ((MessageContainer)part).getChildren();
                MessagePart[] newKids = new MessagePart[list.size()];
                return (MessagePart[])list.toArray(newKids);
            }

            MessagePart[] parts = {part};
            return parts;
        }

        String nodeType = getString(paths.NFTypePath, ctx);
        // set up the path step
        String elemName = stripIdSuffix(getString(paths.idPath, ctx));
        PathElement step = curPath;
        if (StringUtils.hasValue(elemName))
        {
            if (curPath != null)
                step = curPath.getChild(elemName);
            if (step == null)
            {
                step = new PathElement();
                step.name = elemName;
                step.parent = curPath;
                if (curPath != null)
                    curPath.addChild(step);
            }
        }


        String key = "";
        if (StringUtils.hasValue(url))
            key = new StringBuilder().append(url).append("-").append((step != null && StringUtils.hasValue(step.getNFPath())) ? step.getNFPath() : "").append("-").append(getString(paths.idPath, ctx)).toString();


        // the type to load depends on the node name
        if (nodeType == null)
        {
            // this is XML structure information not related to GUI rendering
            return readChildrenFromXML(ctx, this, step, parent);
        }
        else if (isCommon && StringUtils.hasValue(key) && presentInElemsCache(key))
            {
                part = (MessagePart) getCommonElems(key);
                if (log.isDebugEnabled())
                    log.debug("Returning message part from cache for key [" + key + "] node type [" + nodeType + "], isCommon [" + isCommon + "].");
                MessagePart[] parts = {part};
                registerPart(part);
                return parts;
            }
        else if (nodeType.equals(FORM_NAME))
            part = new Form();
        else if (nodeType.equals(SECTION_NAME))
            part = new Section();
        else if (nodeType.equals(FIELD_GROUP_NAME))
            part = new FieldGroup();
        else if (nodeType.equals(REPEATING_SUB_SECTION))
            part = new RepeatingSubSection();
        else if (nodeType.equals(FIELD_NAME))
            part = new Field();
        else
            throw new MessageException("Unknown message part: [" + nodeType + "]");
        // load it
        part.setParent(parent);
        part.readFromXML(ctx, this, step);

        MessagePart[] parts = {part};

        if (isCommon && StringUtils.hasValue(key))
        {
            if (!presentInElemsCache(key))
            {
                if (log.isDebugEnabled())
                    log.debug("Adding message part in cache with key [" + key + "], url [" + part + "]");
                addInElemsCache(key, part);
            }
        }

        return parts;
    }

    /**
     * Adds a part with an id
     *
     * @param part The part to add
     */
    public void registerPart(MessagePart part)
    {
        // get the id
        String id = part.getID();

        // add it to the collection
        if (parts.containsKey(id))
            log.warn("Overriding definition with id [" + id + "].");

        parts.put(id, part);
    
     }

    public void registerDataType(DataType dataType)    
    {
        // get the id
        String name = dataType.getName();

        if (name == null)
            return;

        // add it to the collection
        if (dataTypes.containsKey(name))
            log.warn(
                      "Overriding data type definition with name ["
                      + name + "].");
        dataTypes.put(name, dataType);
    }


    /**
     * Loads the definitions (shared data types, forms, etc.) from an XML
     * document
     *
     * @param ctx      The document to read definitions from
     * @param visited  The list of visited resources (used to prevent getting
     *                 stuck in an infinite loop)
     * @param url      The base URL
     * @param resolver The URIResolver to use
     *
     * @exception FrameworkException Thrown if the XML document is invalid
     *                               or there is an error accessing it
     */
    protected void loadDefinitions(Node ctx, HashSet visited,
                                   String url, URIResolver resolver, boolean isCommon)
        throws FrameworkException
    {
        List nodes;

        // all of these definitions are optional, so just skip ahead if the
        // node isn't found

        // start with external references
        nodes = paths.includesPath.getNodeList(ctx);
        MsgResource res = null;
        int len = nodes.size();
        if (len > 0)
        {
            // walk through the schemas
            for (int i = 0; i < len; i++)
            {
                // see if we have imported it already
                String resourceName = ((Node)nodes.get(i)).getNodeValue();
                if (visited.contains(resourceName))
                    // skip it
                    continue;
                else
                    visited.add(resourceName);

                // get the XML document
                res = readDocument(url, resourceName, resolver);

                // load it
                if (res != null)
                    loadDefinitions(res.doc, visited, res.href, resolver, isCommonResource(url, resourceName, resolver));
                else
                    log.warn("loadDefinitions: MsgResource is null for resource: [" + resourceName + "] in [" + url + "], so skipping [" + resourceName + "]" );
            }
        }

        // do data types
        nodes = paths.globalDataTypesPath.getNodeList(ctx);
        len = nodes.size();
        if (len > 0)
        {
            loadDataTypes(nodes);
        }

        // do complex types
        nodes = paths.globalComplexTypesPath.getNodeList(ctx);
        len = nodes.size();
        if (len > 0)
        {
            loadComplexTypes(nodes);
        }

        // then other elements
        nodes = paths.anonElemsPath.getNodeList(ctx);
        len = nodes.size();
        if (len > 0)
        {
            loadElemDefs(nodes, ctx, isCommon, url);
        }
    }

    /**
     * Load data type definitions
     *
     * @param nodes The definitions to load up
     */
    protected void loadDataTypes(List nodes) throws FrameworkException
    {
        int len = nodes.size();
        for (int i = 0; i < len; i++)
        {
            // create a data type
            DataType dt = new DataType();

            // initialize it
            dt.readFromXML((Node)nodes.get(i), this);
        }
    }

    /**
     * Load complex type definitions
     *
     * @param nodes The definitions to load up
     */
    protected void loadComplexTypes(List nodes) throws FrameworkException
    {
        int len = nodes.size();
        for (int i = 0; i < len; i++)
        {
            // create a complex type
            ComplexType ct = new ComplexType();

            // initialize it
            ct.readFromXML((Node)nodes.get(i), this, null);
        }
    }

    /**
     * Load anonymous element definitions
     *
     * @param nodes The definitions to load up
     */
    protected void loadElemDefs(List nodes, Node res, boolean isCommon, String url) throws FrameworkException
    {
        int len = nodes.size();
        // as all node are of same type

        for (int i = 0; i < len; i++)
        {
            // create the parts
            readPartsFromXML((Node)nodes.get(i), null, null, isCommon, url);
        }
    }

    /**
     * Loads an XML document given an optional base URI, a URI, and
     * an optional URIResolver.
     *
     * @param urlBase The base url for the url(Can be null if not known).
     * @param urlStr The url string that needs to be resolved.
     * @param resolver The resolver that can be used to resolve the urlStr.
     */
    protected MsgResource readDocument(String urlBase, String urlStr, URIResolver resolver )
        throws FrameworkException
    {
        try
        {
            if (isCommonResource (urlBase, urlStr, resolver))
                return getCommonResource (urlBase, urlStr, resolver);
            
            if (resolver != null)
            {
                Source src = resolver.resolve(urlStr, urlBase);

                // if the resolver returns null, we try to resolve it later
                // at the bottom of this method
                if (src != null)
                {
                    if (!(src instanceof StreamSource))
                        throw new IllegalArgumentException("URI resolvers " +
                            "provided to the Message class must return " +
                            "StreamSources.");

                    return new MsgResource(src.getSystemId(), ((StreamSource)src).getInputStream());
                }

            }

            // no resolver or it failed, try to resolve it here
            URL url = (urlBase == null) ? new URL(urlStr) :
                                          new URL(new URL(urlBase), urlStr);

            URLConnection con = url.openConnection();
            con.setDefaultUseCaches(false);

            return new MsgResource(url.toString(), con.getInputStream());
        }
        catch (FrameworkException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            // if the URL resource is not resolvable then return null instead of exception
            if (ex instanceof TransformerException)
                return null;
            else
                throw new FrameworkException(ex);
        }
    }

    /**
     * Return true if the resource obtained on passed parameters is present in common cache
     * else false.
     *
     * @param url resource url
     * @param resourceName name
     * @param resolver object
     * @return boolean as result
     * @throws FrameworkException on error
     */
    public boolean isCommonResource(String url, String resourceName, URIResolver resolver) throws FrameworkException
    {
        String logInfo = "isCommonResource: ";

        if (commonResourceCache == null)
        {
            loadCommonResource (url, resourceName, resolver);
        }

        String key = url;
        try {
            Source tempSrc = resolver.resolve(resourceName, url);
            if (tempSrc != null)
                key = tempSrc.getSystemId();
        }
        catch (TransformerException e)
        {
            Debug.warning (Debug.WARNING_MSG_PREFIX + logInfo + "Unable to resolve Url [" + url + "] and resourceName [" + resourceName + "].");
        }

        if (!StringUtils.hasValue(key)) key = "";

        boolean result = commonResourceCache.containsKey(key);
        if (log.isDebugEnabled())
            log.debug("Present in common resource [" + result + "] with obtained key [" + key + "] using parameters url [" + url + "] and resourceName [" + resourceName + "].");

        return result;
    }

    /**
     * Gets the common resource from the cache based on parameters passed.
     *
     * @param url resource url
     * @param resourceName name
     * @param resolver object
     * @return MsgReource object containing common resource
     * @throws FrameworkException on error
     */
    public MsgResource getCommonResource (String url, String resourceName, URIResolver resolver) throws FrameworkException
    {
        String logInfo = "getCommonResource: ";

        String key = url;
        Source src = null;
        try
        {
            src = resolver.resolve(resourceName, url);
            if (src != null)
            {
                key = src.getSystemId();
            }
        }
        catch (TransformerException e) {
            Debug.error (logInfo + "TransformerException; Unable to resolve url." + e.getMessage());
        }
        catch (Exception e) {
            Debug.error (logInfo + "Exception; Unable to resolve url." + e.getMessage());
        }

        if (develMode)
        {
            try {
                URL urltemp = new URL (src.getSystemId());

                URLConnection con = urltemp.openConnection();
                con.setDefaultUseCaches(false);
                long lastModified = con.getLastModified();
                long lastModifiedCache = 0;
                if (commonResourceModifiedCache.get(src.getSystemId()) != null)
                {
                    lastModifiedCache = (Long) commonResourceModifiedCache.get(src.getSystemId());
                    if (lastModified != lastModifiedCache)
                    {
                        if (Debug.isLevelEnabled(Debug.MSG_DATA))
                            Debug.log (Debug.MSG_DATA, "Resource Changed; reloading cache for url [" + url + "], resourceName [" + resourceName + "]");
                        updateCommonCache (src.getSystemId(), src, lastModified);
                        // clearing elements cache
                        clearElemsCache();
                    }
                }
            }
            catch (IOException e) {
                Debug.warning (logInfo + "Error while updaing cache. Skipping updation. Error Message: " + e.getMessage());
            }
        }

        if (commonResourceCache == null)
        {
            loadCommonResource(url, resourceName, resolver);
        }

        if (!StringUtils.hasValue(key)) key = "";

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log (Debug.MSG_DATA, logInfo + "Returning MsgResource for key ["+ key +"]");

        return (MsgResource) commonResourceCache.get(key);

    }

    /**
     * Loads the common resource.
     *
     * @param url resource url
     * @param resourceName name
     * @param resolver object
     * @throws FrameworkException on error
     */
    private void loadCommonResource(String url, String resourceName, URIResolver resolver) throws FrameworkException
    {
        if (commonResourceCache == null)
        {
            if (commonResourceModifiedCache == null)
            {
                commonResourceModifiedCache = new HashMap();
            }

            commonResourceCache     = new HashMap();
            String commonResourceLoc = "";
            File commonResources [] = null;

            String COMMON_RESOURCE_PROP = "COMMON_RESOURCE_LOCATION";
            Properties webAppsProps = null;

            try
            {
                webAppsProps = PropUtils.getProperties("WEBAPPS", "COMMON", "COMMON");
                String isProdMode = PropUtils.getPropertyValue(webAppsProps, "PRODUCTION_MODE");
                // making ! as development_mode is opposite to production_mode
                develMode = !StringUtils.getBoolean(isProdMode, false);
            }
            catch (Exception e)
            {
                Debug.warning ("Unable to obtain web apps properties or production mode property from PersistentProperty, considering defaults.");
                webAppsProps = null;
                develMode = false;
            }

            for ( int Ix = 0;  true;  Ix ++ )
            {
                try
                {
                    if (webAppsProps != null)
                    {
                        commonResourceLoc = PropUtils.getPropertyValue(webAppsProps, PersistentProperty.getPropNameIteration ( COMMON_RESOURCE_PROP, Ix ) );

                        //stop when no more properties are specified
                        if ( !StringUtils.hasValue( commonResourceLoc ) )
                            break;

                        if (StringUtils.hasValue (commonResourceLoc))
                            commonResources = FileUtils.getAvailableFileList (commonResourceLoc);
                    }
                }
                catch (PropertyException e)
                {
                    Debug.warning ("loadCommonResource: PropertyException; Unable to obtain the common resource location from persistent property. Making commonResources null.");
                    commonResources = null;
                }
                catch (FrameworkException e)
                {
                    Debug.warning ("loadCommonResource: FrameworkException; Unable to obtain files under location [" + commonResourceLoc + "]. Making commonResources null.");
                    commonResources = null;
                }
                catch (Exception e)
                {
                    Debug.warning ("loadCommonResource: Exception; Either Unable to obtain files under location [" + commonResourceLoc + "] or Unable to obtain the common resource location from persistent property. Making commonResources null.");
                    commonResources = null;
                }

                if (commonResources != null)
                {
                    int len = commonResources.length;
                    for (int src = 0; src < len; src++)
                    {
                        try
                        {
                            String href = commonResources[src].toURL().toString();
                            addToCommonCache (href, null, resolver);
                        }
                        catch (Exception e)
                        {
                            Debug.error ("loadCommonResource: Exception; Unable to convert url for [" + commonResources[src] + "]." + e.getMessage());
                        }
                    }
                    if (log.isDebugEnabled())
                        log.debug("develMode obtained as [" + develMode + "], Common resource loaded from location [" + commonResourceLoc + "], and commonResourceCache [" + commonResourceCache + "].");
                }
            }
        }
    }

    /**
     * Code to add the resource to common cache
     *
     * @param href path of the resource
     * @param resourceName base of the resource (can be null)
     * @param resolver URIResolver object
     */
    private void addToCommonCache(String href, String resourceName, URIResolver resolver)
    {
        try
        {
            Source srcobj = resolver.resolve(href, resourceName);
            long lastModified = 0;
            if (srcobj == null)
            {
                // no resolver or it failed, try to resolve it here
                URL urltemp = new URL (href);

                URLConnection con = urltemp.openConnection();
                con.setDefaultUseCaches(false);
                lastModified = con.getLastModified();
                srcobj = new StreamSource(con.getInputStream(), urltemp.toString());
            }

            if (srcobj != null)
            {
                String TOMCAT_WEBAPPS = "/tomcat/webapps/";
                String JNDI_LOCALHOST = "jndi:/localhost/";

                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "Source system id obtained is [" + srcobj.getSystemId() + "].");

                String sysId = srcobj.getSystemId();

                if (href.indexOf(TOMCAT_WEBAPPS) > -1)
                    sysId = JNDI_LOCALHOST + href.substring (href.indexOf(TOMCAT_WEBAPPS) + TOMCAT_WEBAPPS.length());

                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "Source system id after processing is [" + sysId + "].");

                updateCommonCache (sysId, srcobj, lastModified);
            }
        }
        catch (Exception e)
        {
            Debug.error ("addToCommonCache: Exception; Unable to convert url for [" + href + "]." + e.getMessage());
        }
    }

    /**
     * Update the common cache and modified time of the resource.
     * Not checking the key as the resource may be reloaded
     * after modification.
     *
     * @param sysId path of the resource
     * @param srcobj Source object
     * @param lastModified long value that holds the last modified time
     * @throws MessageException on error
     */
    private void updateCommonCache(String sysId, Source srcobj, long lastModified) throws MessageException
    {
        if (StringUtils.hasValue(sysId) && srcobj != null && lastModified != 0)
        {
            synchronized(commonResourceCache)
            {
                commonResourceCache.put (sysId, new MsgResource (srcobj.getSystemId(), ((StreamSource)srcobj).getInputStream()));
                commonResourceModifiedCache.put (sysId, lastModified);
            }
            if (Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "updateCommonCache: Reloaded resource with params sysId ["+ sysId +"], lastModified [" + lastModified + "]");
        }
    }

    /**
     * Return true if the message part provided in the key is
     * present in the element cache.
     * @param key to search MessagePart for.
     * @return boolean
     * @throws FrameworkException on error
     */
    public boolean presentInElemsCache (String key) throws FrameworkException
    {
        return commonElemsCache != null && commonElemsCache.containsKey(key);
    }

    /**
     * Adds entry to element cache
     *
     * @param key url of the message part
     * @param value MessagePart object
     * @throws FrameworkException on error
     */
    public void addInElemsCache (String key, Object value) throws FrameworkException
    {
        if (commonElemsCache == null)
            commonElemsCache = new HashMap();

        synchronized (commonElemsCache )
        {
            commonElemsCache.put (key, value);
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log (Debug.MSG_STATUS, "addInElemsCache: Updated elements cache with key [" + key + "].");
        }
    }

    /**
     * Empties element cache
     * @throws FrameworkException on error
     */
    public void clearElemsCache () throws FrameworkException
    {
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log (Debug.MSG_STATUS, "clearElemsCache: Flushing elements cache.");
        synchronized (commonElemsCache) {
            commonElemsCache.clear();
        }
    }


    /**
     * Retrieve the element from cache based on url (key)
     *
     * @param key the actual url
     * @return MessagePart object corresponding to key
     */
    public Object getCommonElems(String key)
    {
        return commonElemsCache.get (key);
    }

    /**
     * Unit test
     */
    public static void main(String[] args)
    {
        try
        {
            if (args.length != 1)
                throw new FrameworkException("usage: java "
                    + Message.class.getName() + " filename");

            Debug.configureFromProperties();

            // load the file
            File file = new File(args[0]);

            // go to work
            long start = System.currentTimeMillis();
            Message msg = new Message(file.toURL());
            long stop = System.currentTimeMillis();

            System.out.println("Load completed successfully. ("
                               + (stop - start) + " milliseconds)");
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
            System.err.println("Message: " + ex.toString());
            System.exit(-1);
        }
    }

    /**
     * MsgResource is a Message in document form with associated URL
     * information.
     */
    protected class MsgResource
    {
        public String href  = null;
        public Document doc = null;

        /**
         * Constructor
         */
        public MsgResource()
        {
        }

        /**
         * Constructor for an href and a doc
         */
        public MsgResource(String href, Document doc)
        {
            this.href = href;
            this.doc  = doc;
        }
        
        /**
         * Constructor for an href and an InputStream
         */
        public MsgResource(String href, InputStream stream)
            throws MessageException
        {
            this.href = href;

            InputStream is = (stream instanceof BufferedInputStream)
                ? stream : new BufferedInputStream(stream);
            this.doc  = XMLLibraryPortabilityLayer.convertStreamToDom(is);
        }
    }
}

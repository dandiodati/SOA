/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.manager.svcmeta;

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
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.framework.debug.*;


/**
 * BundleDef represents the definition of a bundle.  It contains information
 * about what service components are allowed in a bundle and how many of
 * them are allowed.
 */
public class BundleDef
{
    /** Name of DataType definition elements */
    private static final String DATA_TYPE_ELEM = "DataType";

    /** Name of Field definition elements */
    private static final String FIELD_ELEM = "Field";

    /** Identifies this bundle */
    private String id;

    /** Display name for the bundle */
    private String displayName;

    /** Full name for the bundle */
    private String fullName;

    /** Help text for the bundle */
    private String helpText;

    /** &quot;Meta data&quot; name for the bundle */
    private String metaDataName;

    /** List of components that may occur in the bundle */
    private ArrayList components = new ArrayList();

    /** map to track ids to components **/
    private HashMap componentIds = new HashMap();

    /** Unmodifiable view of components */
    private List ro_components = null;

    /** Custom name /value mappings on the bundle definition */
    private HashMap customValues = new HashMap();

    /** Unmodifiable view of customValues */
  private Map readOnlyCustomValues = null;

  /** Action definitions available on this component */
  private ArrayList actionDefs = new ArrayList();
  /** Read-only view of actionDefs */
  private List ro_actionDefs;

  private DebugLogger log;

    /**
     * Constructor for a URL.  This constructor loads a document from the
     * URL using the system default character encoding.
     *
     * @param url  The URL of the document to load
     *
     * @param FrameworkException Thrown if the document cannot be loaded
     */
    public BundleDef(URL url) throws FrameworkException
    {
      log = DebugLogger.getLoggerLastApp(getClass() );

        // prepare our context for building
        BundleDefBuildContext buildCtx = new BundleDefBuildContext();
        buildCtx.url = url;

        // read the document from the url
        log.debug("Loading bundle def at [" + url.toString() +"]");

        Document doc = readFully(url);

        readFromXML(doc, buildCtx);
    }

    /**
     * Returns this bundle definition's ID.
     * @deprecated  This method does not conform to the Java Bean naming standard.  Use getId() instead.
     */
    public String getID()
    {
        return id;
    }

    
    /**
     * Returns this bundle definition's id.
     *
     * @return a <code>String</code> value
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns the display name for the bundle
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the full name for the bundle
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Returns the help text for the bundle
     */
    public String getHelpText()
    {
        return helpText;
    }

    /**
     * Returns the meta data name for the bundle
     */
    public String getMetaDataName()
    {
        return metaDataName;
    }

    /**
     * Obtains a particular component by index
     *
     * @param idx  The index of the component to return
     */
    public ComponentDef getComponent(int idx)
    {
        return (ComponentDef)components.get(idx);
    }

    /**
     * Obtains a particular component by id
     *
     * @param id  The id of the component to return
     */
    public ComponentDef getComponent(String id)
    {
        return (ComponentDef)componentIds.get(id);
    }

    /**
     * Obtains a list of all the components
     */
    public List getComponents()
    {
        if (ro_components == null)
            ro_components = Collections.unmodifiableList(components);

        return ro_components;
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
     * Returns a list of the actions available on components of this type.
     * The items in the list are instances of {@link ActionDef}.
     */
    public List getActionDefs()
    {
      if (ro_actionDefs == null)
          ro_actionDefs = Collections.unmodifiableList(actionDefs);

      return ro_actionDefs;
    }

    /**
     * Reads in this bundle definition from an XML document
     *
     * @param doc       The document to read from
     * @param buildCtx  The BundleDefBuildContext to use
     *
     * @exception FrameworkException Thrown if a definition cannot be loaded
     *                               from doc
     */
    public void readFromXML(Document doc, BundleDefBuildContext buildCtx)
        throws FrameworkException
    {
        BuildPaths xpaths = buildCtx.xpaths;

        List l = xpaths.bundleRootPath.getNodeList(doc);
        if (l.size() != 1)
            throw new BundleDefBuildContext.InvalidSvcException("The document at URL [" + buildCtx.url
                + "] is not a complete bundle definition");

        Node root = (Node)l.get(0);

        // include any referenced files
        HashSet visited = new HashSet();
        visited.add(buildCtx.url);
        handleIncludes(doc, buildCtx, visited);

        // load any gui definitions
        loadGUIDefs(doc, buildCtx);

        // load any Message Types defs, actions depend on this
        loadMessageTypes(doc, buildCtx);

        // load any actions
        loadActions(doc, buildCtx);

         // load global svc defs after all other defs
         // since it could use gui defs, actions or message types
        loadGlobalSvcDefs(doc, buildCtx);

        // get the id
        id = buildCtx.getString(xpaths.idPath, root);

        // display name
        displayName = buildCtx.getString(xpaths.displayNamePath, root);

        // full name
        fullName = buildCtx.getString(xpaths.fullNamePath, root);

        // help text
        List helpNodes = xpaths.helpPath.getNodeList(root);
        if (helpNodes.size() > 0)
        {
            Node helpNode = (Node)helpNodes.get(0);

            helpText = DOMWriter.toString(helpNode.getFirstChild(), true);
        }

        // meta data name
        metaDataName = buildCtx.getString(xpaths.metaDataNamePath, root);

        // get all the service components
        Iterator compNodes = xpaths.componentsPath.getNodeList(root)
            .iterator();
        while (compNodes.hasNext())
        {
            Node comp = (Node)compNodes.next();
            ComponentDef def = null;

            if ( xpaths.refPath.nodeExists(comp) ) {
              // obtain a reference to a globally defined component defintion
              String refName  = (String)xpaths.refPath.getValue(comp);
              def = buildCtx.getSvcDef(refName);
              if ( def == null) {
                String error = "Invalid service component reference [" + refName +"] in bundle definition, " +  buildCtx.url;
                log.error(error);
                throw new FrameworkException(error);
              }

            }
            else {
              // create a new component definition
              def = new ComponentDef();
              def.readFromXML(comp, buildCtx);
            }

            if (def != null) {
              def.setBundleDef(this);
              components.add(def);
              componentIds.put(def.getId(), def);
            }

        }

        // allowableActions
        if (xpaths.allowableActionsPath.nodeExists(root)) {
          Iterator iter = xpaths.allowableActionsPath.getNodeList(root)
              .iterator();
          while (iter.hasNext()) {
            Node n = (Node) iter.next();
            String name = n.getNodeValue();
            ActionDef def = buildCtx.getAction(name);
            if (def == null)
              log.error(
                  "Could not locate AllowableActions with name [" + name
                  + "], referenced at [" + buildCtx.toXPath(root)
                  + "].");
            else
              actionDefs.add(def);
          }
        }

        // custom values
        List custVals = xpaths.customPath.getNodeList(root);
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
     * Reads in any included files
     *
     * @param doc       The document to read from
     * @param buildCtx  The BundleDefBuildContext to use
     * @param visited   Set of visited URLs
     *
     * @exception FrameworkException Thrown if a actions cannot be loaded
     *                               from doc
     */
    private void handleIncludes(Document doc, BundleDefBuildContext buildCtx,
                                HashSet visited)
        throws FrameworkException
    {
        List l = buildCtx.xpaths.includesPath.getNodeList(doc);

        Iterator iter = l.iterator();
        while (iter.hasNext())
        {

            Node n = (Node)iter.next();

            // determine the URL
            String path = n.getNodeValue();
            URL url;
            try
            {
                url = new URL(buildCtx.url, path);
            }
            catch (MalformedURLException ex)
            {
                log.error("Could not include file at ["
                          + path + "]: " + ex);
                continue;
            }

            // only include the file if we haven't already
            if (!visited.contains(url))
            {
                visited.add(url);

                // read in the XML
                Document newDoc = readFully(url);


                // update the context to reflect the new base URL
                URL lastUrl = buildCtx.url;
                buildCtx.url = url;

                // now do any additional includes
                handleIncludes(newDoc, buildCtx, visited);

                // load gui definitions
                loadGUIDefs(newDoc, buildCtx);

                // load any Message Types defs, actions depend on this
                loadMessageTypes(newDoc, buildCtx);

                // load actions
                loadActions(newDoc, buildCtx);


                // load global svc defs after all other defs
                // since it could use gui defs, actions or message types
                loadGlobalSvcDefs(newDoc, buildCtx);

                // reset the context
                buildCtx.url = lastUrl;
            }
        }
    }

    /**
     * Reads in action definitions from an XML document
     *
     * @param doc       The document to read from
     * @param buildCtx  The BundleDefBuildContext to use
     *
     * @exception FrameworkException Thrown if a actions cannot be loaded
     *                               from doc
     */
    private void loadActions(Document doc, BundleDefBuildContext buildCtx)
        throws FrameworkException
    {
        List l = buildCtx.xpaths.actionsPath.getNodeList(doc);

        Iterator iter = l.iterator();
        while (iter.hasNext())
        {
            Node n = (Node)iter.next();

            // create a new action definition
            ActionDef def = new ActionDef();
            def.readFromXML(n, buildCtx);

            // register it
            buildCtx.registerAction(def);
        }
    }

/**
     * Reads in any globally defined service component defintitions
     * (which exist under the SVCDefinitions node).
     *
     * @param doc       The document to read from
     * @param buildCtx  The BundleDefBuildContext to use
     *
     * @exception FrameworkException Thrown if a actions cannot be loaded
     *                               from doc
     */
    private void loadGlobalSvcDefs(Document doc, BundleDefBuildContext buildCtx)
        throws FrameworkException
    {
        List l = buildCtx.xpaths.svcDefsPath.getNodeList(doc);

        Iterator iter = l.iterator();
        while (iter.hasNext())
        {
            Node n = (Node)iter.next();

            // create a new action definition
            ComponentDef def = new ComponentDef();
            def.readFromXML(n, buildCtx);

            // register it
            buildCtx.registerSvcDef(def);
        }
    }


    /**
     * Reads in action definitions from an XML document
     *
     * @param doc       The document to read from
     * @param buildCtx  The BundleDefBuildContext to use
     *
     * @exception FrameworkException Thrown if a actions cannot be loaded
     *                               from doc
     */
    private void loadMessageTypes(Document doc, BundleDefBuildContext buildCtx)
        throws FrameworkException
    {
        List l = buildCtx.xpaths.messageTypesPath.getNodeList(doc);


        Iterator iter = l.iterator();
        while (iter.hasNext())
        {
            Node n = (Node)iter.next();
            // create a new message type definition
            MessageTypeDef def = new MessageTypeDef();
            def.readFromXML(n, buildCtx);

            // register it
            buildCtx.registerMessageType(def);
        }
    }

    /**
     * Reads in data type and field definitions from an XML document
     *
     * @param doc       The document to read from
     * @param buildCtx  The BundleDefBuildContext to use
     *
     * @exception FrameworkException Thrown if a actions cannot be loaded
     *                               from doc
     */
    private void loadGUIDefs(Document doc, BundleDefBuildContext buildCtx)
        throws FrameworkException
    {
        List l = buildCtx.xpaths.guiDefsPath.getNodeList(doc);

        Iterator iter = l.iterator();
        while (iter.hasNext())
        {
            Node n = (Node)iter.next();

            if ( DATA_TYPE_ELEM.equals(n.getNodeName()) )
            {
                // create a new data type
                ServiceDataType type = new ServiceDataType();
                type.readFromXML(n, buildCtx);
            }
            else if ( FIELD_ELEM.equals(n.getNodeName()) )
            {
                // create a new field
                ServiceField field = new ServiceField();
                field.readFromXML(n, buildCtx);
            }
            else
                log.error("Unknown definition ["
                          + n.getNodeName() + "] at [" + buildCtx.toXPath(n)
                          + "].");
        }
    }

    /**
     * Reads from a stream specified by a URL and returns the result as a
     * xml document.
     *
     * @param url       URL to the document to load
     * @return The xml document
     */
    private Document readFully(URL url)
        throws FrameworkException
    {
      InputStream r = null;
      Document doc = null;

      try {
        URLConnection  connection   = url.openConnection();
        connection.setUseCaches(false);

        r = new BufferedInputStream(connection.getInputStream() );

        doc = XMLLibraryPortabilityLayer.convertStreamToDom(r);

        }
        catch (IOException ex) {
            log.error("Failed to read url [" + url.toString() +"], " + ex.getMessage());

            throw new FrameworkException(ex);
          } finally {
            try {
              if ( r != null)
                r.close();
            }
            catch (Exception e) {
              log.error("Failed to close stream : " + e.getMessage() );
            }
          }

        // return the xml
        return doc;


      }



}

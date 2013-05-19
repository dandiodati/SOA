/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.gateway.svcmeta;

// jdk imports
import java.io.*;
import java.net.*;
import java.util.*;

// third-party imports
import org.w3c.dom.*;

// nightfire imports
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.debug.*;
import com.nightfire.webgui.core.svcmeta.*;


/**
 * ServiceDefContainer represents a list of service definition known to the application.
 * It loads the list of service defintions.
 */
public class ServiceDefContainer
{
    /** Name of DataType definition elements */
    private static final String DATA_TYPE_ELEM = "DataType";

    /** Name of Field definition elements */
    private static final String FIELD_ELEM = "Field";


    /** List of service definitions */
    private List serviceList = new ArrayList();

    /** map to track ids to services **/
    private Map serviceMap = new HashMap();

    /** Unmodifiable view of services */
    private List ro_serviceList = null;

    private DebugLogger log;

    /**
     * Constructor for a URL.  This constructor loads one or more document from the
     * given URL using the system default character encoding.
     *
     * @param urls  The URLs of the documents to load
     *
     * @throws FrameworkException Thrown if the document cannot be loaded
     */
    public ServiceDefContainer(URL[] urls) throws FrameworkException
    {
      log = DebugLogger.getLoggerLastApp(getClass() );

      ro_serviceList      = Collections.unmodifiableList(serviceList);

      if (urls == null || urls.length <= 0)
      {
          throw new FrameworkException("Cannot load service definition with empty URLs.");
      }

      for (int i=0; i<urls.length; i++)
      {
          try {
              if (log.isInfoEnabled())
              {
                  log.info("Loading services from [" + urls[i] + "].");
              }

              Map loadedServiceMap = loadServices(urls[i]);

              if (log.isInfoEnabled())
              {
                  log.info("Loaded [" + loadedServiceMap.size() +
                           "] services from [" + urls[i] + "].");
              }

              for (Iterator it = loadedServiceMap.keySet().iterator(); it.hasNext(); )
              {
                  ServiceDef service = (ServiceDef)loadedServiceMap.get(it.next());
                  addService(service);
              }

          }
          catch (InvalidServiceDefException e)
          {
              log.warn( "Skipping service def: " + e .getMessage() );
              continue;
          }
      }
  }

    /**
     * Obtains a particular service by name
     *
     * @param name The name of the service to return
     * @return ServiceDef
     */
    public ServiceDef getService(String name)
    {
        return (ServiceDef)serviceMap.get(name);
    }

    /**
     * Obtains a list of all the services
     *
     * @return List
     */
    public List getServices()
    {
        return ro_serviceList;
    }

    /**
     * Adds a service definition
     *
     * @param def ServiceDef
     * @throws FrameworkException
     */
    protected void addService(ServiceDef def) throws FrameworkException
    {
        String name = def.getID();
        if (!StringUtils.hasValue(name))
        {
            throw new FrameworkException("Cannot add service with empty name.");
        }

        else if (serviceMap.containsKey(name))
        {
            throw new
                FrameworkException("Service with name [ "
                      + name + "] already exists.");
        }

        serviceList.add(def);
        serviceMap.put(name, def);
    }

    /**
     * Reads from a stream specified by a URL and returns a list of services
     * defined in the URL.
     *
     * @param url URL to the document to load
     * @return The list of service definitions
     * @throws FrameworkException
     */
    protected Map loadServices(URL url)
        throws FrameworkException
    {
        // prepare our context for building
        ServiceGroupBuildContext buildCtx = new ServiceGroupBuildContext();
        buildCtx.url = url;

        Document doc = buildCtx.readFully(url);
        readFromXML(doc, buildCtx);

        return buildCtx.getServices();
    }

    /**
      * Reads in this service definition from an XML document
      *
      * @param doc       The document to read from
      * @param buildCtx  The BuildContext to use
      *
      * @exception FrameworkException Thrown if a definition cannot be loaded
      *                               from doc
      */
     protected void readFromXML(Document doc, ServiceGroupBuildContext buildCtx)
         throws FrameworkException
     {
         BuildPaths xpaths = buildCtx.xpaths;

         List nodes = xpaths.serviceRootPath.getNodeList(doc);

         if (nodes.size() == 0)
         {
             throw new InvalidServiceDefException("The document at URL [" + buildCtx.url
                 + "] does not contain any service definition.");
         }

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

         // Load the services
         for (Iterator it = nodes.iterator(); it.hasNext(); )
         {
             Node serviceNode = (Node) it.next();

             // load the service definition
             ServiceDef service = new ServiceDef();
             service.readFromXML(serviceNode, buildCtx);

             // Add to context
             buildCtx.registerService(service);

         }
     }

     /**
      * Reads in any included files
      *
      * @param doc       The document to read from
      * @param buildCtx  The BuildContext to use
      * @param visited   Set of visited URLs
      *
      * @exception FrameworkException Thrown if a actions cannot be loaded
      *                               from doc
      */
     private void handleIncludes(Document doc, ServiceGroupBuildContext buildCtx,
                                 Set visited)
         throws FrameworkException
     {
         List nodes = buildCtx.xpaths.includesPath.getNodeList(doc);

         Iterator iter = nodes.iterator();
         while (iter.hasNext())
         {
             Node node = (Node)iter.next();

             // determine the URL
             String path = node.getNodeValue();
             URL url;
             try
             {
                 url = new URL(buildCtx.url, path);
             }
             catch (MalformedURLException ex)
             {
                 log.error("Could not include file at ["
                           + path + "]: " + ex);
                 throw new FrameworkException(ex);
             }

             // only include the file if we haven't already
             if (!visited.contains(url))
             {
                 visited.add(url);

                 // read in the XML
                 Document newDoc = buildCtx.readFully(url);

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

                 // reset the context
                 buildCtx.url = lastUrl;
             }
         }
     }

     /**
      * Reads in action definitions from an XML document
      *
      * @param doc       The document to read from
      * @param buildCtx  The BuildContext to use
      *
      * @exception FrameworkException Thrown if a actions cannot be loaded
      *                               from doc
      */
     private void loadActions(Document doc, ServiceGroupBuildContext buildCtx)
         throws FrameworkException
     {
         List nodes = buildCtx.xpaths.actionsPath.getNodeList(doc);

         Iterator iter = nodes.iterator();
         while (iter.hasNext())
         {
             Node node = (Node)iter.next();

             // create a new action definition
             ActionDef def = new ActionDef();
             def.readFromXML(node, buildCtx);

             // register it
             buildCtx.registerAction(def);
         }
     }

     /**
      * Reads in action definitions from an XML document
      *
      * @param doc       The document to read from
      * @param buildCtx  The BuildContext to use
      *
      * @exception FrameworkException Thrown if a actions cannot be loaded
      *                               from doc
      */
     private void loadMessageTypes(Document doc, ServiceGroupBuildContext buildCtx)
         throws FrameworkException
     {
         List nodes = buildCtx.xpaths.messageTypesPath.getNodeList(doc);


         Iterator iter = nodes.iterator();
         while (iter.hasNext())
         {
             Node node = (Node)iter.next();
             // create a new message type definition
             MessageTypeDef def = new MessageTypeDef();
             def.readFromXML(node, buildCtx);

             // register it
             buildCtx.registerMessageType(def);
         }
     }

     /**
      * Reads in data type and field definitions from an XML document
      *
      * @param doc       The document to read from
      * @param buildCtx  The BuildContext to use
      *
      * @exception FrameworkException Thrown if a actions cannot be loaded
      *                               from doc
      */
     private void loadGUIDefs(Document doc, ServiceGroupBuildContext buildCtx)
         throws FrameworkException
     {
         List nodes = buildCtx.xpaths.guiDefsPath.getNodeList(doc);

         Iterator iter = nodes.iterator();
         while (iter.hasNext())
         {
             Node node = (Node)iter.next();

             if ( DATA_TYPE_ELEM.equals(node.getNodeName()) )
             {
                 // create a new data type
                 ServiceDataType type = new ServiceDataType();
                 type.readFromXML(node, buildCtx);
             }
             else if ( FIELD_ELEM.equals(node.getNodeName()) )
             {
                 // create a new field
                 ServiceField field = new ServiceField();
                 field.readFromXML(node, buildCtx);
             }
             else
             {
                 log.error("Unknown definition ["
                           + node.getNodeName() + "] at [" + buildCtx.toXPath(node)
                           + "].");
             }
         }
     }

     public static void main(String[] args) throws Exception
     {
          Properties debugProps = new Properties();
          debugProps.put(Debug.DEBUG_LOG_LEVELS_PROP, "all");
          debugProps.put(Debug.LOG_FILE_NAME_PROP, "test.log");

          DebugConfigurator.configure(debugProps, null);
          //Debug.configureFromProperties(debugProps);

          Debug.showLevels();
          Debug.log(Debug.UNIT_TEST, "ServiceDefContainer: Start testing...");

          //For Testing
          /*
          args = new String[] {
              "TestMeta/DL.xml"
          };
          */

          URL[] urls = getURLs(args);

          ServiceDefContainer container = new ServiceDefContainer(urls);

          List services = container.getServices();

          //Debug.setThreadLogRedirector(null);
          Debug.log(Debug.UNIT_TEST, "Loaded the following services: [" +
                    services  + "].");

          Debug.log(Debug.UNIT_TEST, "Testing getActions() with the these services...");
          for (Iterator it= services.iterator(); it.hasNext(); )
          {
              ServiceDef service = (ServiceDef) it.next();

              if (!service.getID().equals("Loop"))
              {
                  continue;
              }

              try
              {
                List actions = service.getActions("Initial", "request");

                actions = service.getActions("Initial", "foc");

                actions = service.getActions("Done", "request");

                actions = service.getActions("Done", "foc");
              }
              catch (FrameworkException fe)
              {
                  Debug.log(Debug.UNIT_TEST, "getActions() failed for service [" + service.getID() + "].");
                  Debug.logStackTrace(fe);
              }

          }

          Debug.log(Debug.UNIT_TEST, "ServiceDefContainer: Done.");
      }

      // Used for unit test.
    private static URL[] getURLs(String[] paths) throws MalformedURLException,
        FrameworkException
    {
        ArrayList list = new ArrayList();

        for (int i=0; i<paths.length; i++)
        {
            if (FileUtils.getFileStatistics(paths[i]).isDirectory)
            {
                String[] files = FileUtils.getAvailableFileNames(paths[i]);
                for (int j=0; j<files.length; j++)
                {
                    if (files[j].endsWith(".xml"))
                    {
                        list.add(new URL("file", "localhost",
                                         paths[i] + "/" + files[j]));
                    }
                }

            }
            else
            {
                list.add(new URL("file", "localhost", paths[i]));
            }
        }

        URL[] urls = (URL []) list.toArray(new URL[list.size()]);
        return urls;
    }

      public static final class InvalidServiceDefException extends FrameworkException
      {
          public InvalidServiceDefException(String msg)
          {
              super(msg);
          }
      }

}

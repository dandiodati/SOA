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
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.util.xml.DOMWriter;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.debug.*;
import com.nightfire.webgui.core.svcmeta.*;


/**
 * ServiecGroupDefContainer managers the list of service groups in an application.
 *  It loads the service groups.
 */
public class ServiceGroupDefContainer
{
    /** List of service groups */
    private ArrayList serviceGroupList = new ArrayList();

    /** map to track ids to service groups **/
    private HashMap serviceGroupMap = new HashMap();

    /** Unmodifiable view of service groups */
    private List ro_serviceGroupList = null;

    /** Used to access the list of services */
    private ServiceDefContainer serviceDefContainer = null;

  private DebugLogger log;

    /**
     * Constructor for a URL.  This constructor loads a document from the
     * URL using the system default character encoding.
     *
     * @param url  The URL of the document to load
     * @param serviceDefContainer  The container that contains a list of loaded service definitions
     *
     * @param FrameworkException Thrown if the document cannot be loaded
     */
    public ServiceGroupDefContainer(URL url, ServiceDefContainer serviceDefContainer) throws FrameworkException
    {
      log = DebugLogger.getLoggerLastApp(getClass() );

      if (serviceDefContainer == null)
      {
          throw new FrameworkException("Cannot construct service group definition container with a null service definition container.");
      }

      this.serviceDefContainer = serviceDefContainer;

      ro_serviceGroupList      = Collections.unmodifiableList(serviceGroupList);

        // prepare our context for building
        ServiceGroupBuildContext buildCtx = new ServiceGroupBuildContext();
        buildCtx.url = url;

        // read the document from the url
        if (log.isDebugEnabled())
        {
            log.debug("Loading service group definitions at [" + url.toString() +
                      "]");
        }

        Document doc = buildCtx.readFully(url);

        readFromXML(doc, buildCtx);
    }

    /**
     * Obtains a particular service group by id
     *
     * @param id  The id of the service group to return
     */
    public ServiceGroupDef getServiceGroup(String id)
    {
        return (ServiceGroupDef)serviceGroupMap.get(id);
    }

    /**
     * Obtains a list of all the service groups
     */
    public List getServiceGroups()
    {
        return ro_serviceGroupList;
    }

    /**
     * Reads in this service group definitions from an XML document
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

        // Load service groups
        List l = buildCtx.xpaths.serviceGroupsPath.getNodeList(doc);

        Iterator iter = l.iterator();
        while (iter.hasNext())
        {
            Node n = (Node)iter.next();

            // create a new action definition
            ServiceGroupDef def = new ServiceGroupDef(serviceDefContainer);
            def.readFromXML(n, buildCtx);

            serviceGroupList.add(def);
            serviceGroupMap.put(def.getID(), def);
        }
    }

    public static void main(String[] args) throws Exception
    {
        //For Testing
        /*
        args = new String[] {
            "TestMeta/DL.xml",
            "TestMeta" // This is the folder containing service definitions
        };
        */

        Properties debugProps = new Properties();
        debugProps.put(Debug.DEBUG_LOG_LEVELS_PROP, "all");
        debugProps.put(Debug.LOG_FILE_NAME_PROP, "ServiceGroupTest.log");

        DebugConfigurator.configure(debugProps, null);

        URL[] defFolders = getURLs(args);
        ServiceDefContainer serviceDefContainer = new ServiceDefContainer(defFolders);

        URL url = new URL("file", "localhost", args[0]);

        ServiceGroupDefContainer container = new ServiceGroupDefContainer(url, serviceDefContainer);

        Debug.log(Debug.UNIT_TEST, "Loaded the following services: [" +
                  container.getServiceGroups() + "].");
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

}

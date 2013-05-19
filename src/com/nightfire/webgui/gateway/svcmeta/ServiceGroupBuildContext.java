package com.nightfire.webgui.gateway.svcmeta;


import java.io.*;
import java.net.*;
import java.util.*;

// third-party imports
import org.w3c.dom.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.svcmeta.*;
import com.nightfire.framework.debug.*;

/**
 * Maintains context information while constructing a ServiceDef from
 * an XML document. Regists loaded fields, message types, actions and service defs
 * during the build phase.
 *
 */
public class ServiceGroupBuildContext extends BuildContext
{

    private DebugLogger log;
    private Map serviceMap = new HashMap();

    /**
     * Constructor
     *
     * @throws FrameworkException
     */
    protected ServiceGroupBuildContext() throws FrameworkException
    {
        super();
    }

    /**
     * Reads from a stream specified by a URL and returns the result as a xml
     * document.
     *
     * @param url URL to the document to load
     * @return The xml document
     * @throws FrameworkException
     */
    public Document readFully(URL url)
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

    /**
     * Returns all registered service definitions
     *
     * @return The list of services with their names as keys
     */
    protected Map getServices()
    {
        return serviceMap;
    }

    /**
     * Registers an service definition
     *
     * @param def ServiceDef
     */
    protected void registerService(ServiceDef def)
      {
          String name = def.getID();
          if (!StringUtils.hasValue(name))
              return;

          if (serviceMap.containsKey(name))
              log.warn("Overriding service definition with id [ "
                        + name + "].");

          serviceMap.put(name, def);
      }

    /**
     * Returns a registered service definition , or null if
     * not found
     *
     * @param name The name of the desired service
     *
     * @return ServiceDef
     */
    protected ServiceDef getService(String name)
    {
        return (ServiceDef)serviceMap.get(name);
    }

}


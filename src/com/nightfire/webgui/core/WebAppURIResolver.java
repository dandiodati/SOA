/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/WebAppURIResolver.java#1 $
 */

package com.nightfire.webgui.core;

import  java.lang.*;
import  java.util.*;

import  javax.servlet.http.*;
import javax.servlet.ServletContext;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import com.nightfire.framework.util.*;

import java.io.*;
import java.net.*;
import com.nightfire.framework.debug.*;

import com.nightfire.framework.repository.*;





/**
 * This class handles resolving URIs for a webapp.
 * All full paths are resolved relative to the current webapp's root context.
 *
 * And Multi-level includes are resolved relative to their parent include.
 *
 * NOTE: This class is not thread safe.
 * Each thread should use a new instance of it, since it does very little processing.
 *
 *
 */
public class WebAppURIResolver implements URIResolver
{

   private ServletContext context;


   private URL baseURL = null;
   private String contextRootPath = "";

    private Set resolvedLinks;
    


   /**
    * indicates that this url is relative to another context
    */
   public static final String CONTEXT_PREFIX ="context://";


  private DebugLogger log;
  

   public WebAppURIResolver(ServletContext context)
   {
      this.context = context;
       String webAppName = ServletUtils.getWebAppContextPath(context);
       log = DebugLogger.getLogger(webAppName, WebAppURIResolver.class);
       resolvedLinks = new HashSet();
       
   }


    
    /**
     * Returns a set of all resolved urls that were created by this URIResolver.
     *
     *
     * @return a <code>Set</code> value
     */
    public Set getAllResolvedURIs()
    {
        return resolvedLinks;
        
    }
    

   public Source resolve(String href, String base) throws TransformerException
   {
      if ( log.isDebugEnabled() )
         log.debug("Trying to create URI, base [" + base +"], href [" + href+ "]");


      InputStream in = null;
      URL urlHref = null;

      try {

          // if this is a reference to the repository
          if (href.startsWith(RepositoryManager.URL_PREFIX) ||
              (base != null && base.startsWith(RepositoryManager.URL_PREFIX)) ) {
              URIResolver rResolver = new RepositoryURIResolver();
              Source s = rResolver.resolve(href, base);
              // add the respository urls to the resolvedLinks
              
              URL context = new URL (RepositoryManager.URL_PREFIX, "localhost", -1, "/", RepositoryManager.URL_HANDLER);
              resolvedLinks.add(new URL(context,s.getSystemId(),RepositoryManager.URL_HANDLER));
              return s;
              
         }
      


          // base can be null, when there are multiple includes
          // amoung xsl scripts. Only the first include will have a base.
          // Each include after the first will has a null base
          // and gets resolved based on the last href.




         //if there is a base then set it as the initial root path
         // otherwise set it to null
         if ( StringUtils.hasValue(base) ) {
            int index = base.lastIndexOf("/");
            if ( index > -1 )
               baseURL = new URL(base.substring(0, index + 1) );
         }

        // if the href starts with context:// then extract the context
        // and set the root path.
        // otherwise check if this is a complete url and return null if it is
        //
        if ( href.startsWith(CONTEXT_PREFIX) ) {


           int afterPrefix = CONTEXT_PREFIX.length();
           int contextRootEnd = href.indexOf("/", afterPrefix);
           contextRootPath = href.substring(afterPrefix, contextRootEnd );
           href = href.substring(contextRootEnd);
           if ( log.isDebugEnabled() )
              log.debug("Trying to get context:  [" + contextRootPath + "], href [" + href + "]");

           context = context.getContext("/" + contextRootPath);

            if ( context == null)
              throw new TransformerException("Invalid webapp context provided or cross context access is denied");


        } else if ( href.indexOf(":")  > -1 )  {
          // if href is a full url (http://...)
          // then let the caller resolve it.
            try {

                resolvedLinks.add(new URL(href));
            }
            catch (MalformedURLException e) {
                log.warn("Failed to convert to url, skipping url as a resource.");
            }
            
          return null;
       }



        // if the href is relative and there is a base set then convert it
        // to a full url and load the resource
        // else make the href a full path relative to the current webapp

        if (!href.startsWith("/") )  {
          if ( baseURL != null ) {
             urlHref  = new URL(baseURL, href );
          } else  {
             href = "/" + href;
             urlHref  = context.getResource(href);
          }
        } else {
          // else if this is a full web app path then set the root path for future includes
          // and obtain the web app resource
           int index = href.lastIndexOf("/");
           String webAppRootPath = href.substring(0, index + 1);
           href = href.substring(index + 1);
           baseURL  = context.getResource(webAppRootPath);
           urlHref  = new URL(baseURL, href);
        }

        if ( log.isDebugEnabled() ) {
           String urlBaseStr = null;
           if ( baseURL != null)
              urlBaseStr = baseURL.toString();
           log.debug("Created webapp url [" + urlHref.toString() + "], current base url [" + urlBaseStr +"]");
        }

        resolvedLinks.add(urlHref);
        URLConnection con = urlHref.openConnection();
        con.setDefaultUseCaches(false);
        in = con.getInputStream();

      } catch (Exception e ) {
         throw new TransformerException("Failed to load url: " + e.getMessage());
      }

      if (in == null)
         throw new TransformerException("Failed to load href.");

       String boxId = "";

       if (context != null)
       {
           	String boxIdVal = "";

           	String webAppName = (String)context.getAttribute(ServletConstants.WEB_APP_NAME);
           	Properties initParameters=null;
   			try {
   				initParameters = MultiJVMPropUtils.getInitParameters(context, webAppName);
   			} catch (FrameworkException fe) {
   				log.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
   				throw new TransformerException("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
   			}
           
   			if (initParameters.getProperty(ServletConstants.MANAGER_BOX_IDENTIFIER) != null)
   				boxIdVal = initParameters.getProperty(ServletConstants.MANAGER_BOX_IDENTIFIER);
   			
   			if (StringUtils.hasValue(boxIdVal))
   				boxId = boxIdVal;
   }

       if (!(boxId.equals(ServletConstants.WNP_MANAGER_BOX_IDENTIFIER) || boxId.equals(ServletConstants.IMM_MANAGER_BOX_IDENTIFIER)))
            return new StreamSource(in, urlHref.toString());

       return new StreamSource(in);
   }
}

/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 *
 */

package com.nightfire.framework.repository;

import java.io.*;
import java.net.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import com.nightfire.framework.util.*;
import java.util.StringTokenizer;



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
public class RepositoryURIResolver implements URIResolver
{

 



   public Source resolve(String href, String base) throws TransformerException
   {
      if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
          Debug.log(Debug.MSG_STATUS,"RepositoryURIResolver: Trying to create URI, base [" + base +"], href [" + href+ "]");


      URL url = null;
      
      InputStream in = null;
      try {


        // if the href starts with context:// then extract the context
        // and set the root path.
        // otherwise check if this is a complete url and return null if it is
        //
          
          URL context = new URL (RepositoryManager.URL_PREFIX, "localhost", -1, "/", RepositoryManager.URL_HANDLER);
        if ( href.startsWith(RepositoryManager.URL_PREFIX) ) {
            url = new URL(context, href, RepositoryManager.URL_HANDLER);
            
        }
        else if (base != null && base.startsWith(RepositoryManager.URL_PREFIX)) {

            //URL baseURL = new URL (context, base, RepositoryManager.URL_HANDLER);

            
            URL baseURL = null;
            
             
            //criteria paths will have '.' so replace with a '/'
            // only in the base since the href is going to resolved
            // on the base
            String newBase = base.replaceAll("\\.","/");
                
                
            // now we let the url resolve any relative paths.
            baseURL = new URL (context, newBase, RepositoryManager.URL_HANDLER);
                
            url = new URL(baseURL, href, RepositoryManager.URL_HANDLER);
                
            String path = url.getPath();
                
            //rip off any repository suffixes
            path = RepositoryManager.getMetaDataNameFromFileName(path);
                
                    
            StringBuffer newPath = new StringBuffer("/");
                
            // the new url will only be using '/', so we change
            // the first path location (the category path) back to using
            // '.'.

            String parts[] = path.split("/");
            boolean first = true;
            
            for (int i = 0; i < parts.length; i++) {

                // there may be blanks (since the path starts with a /)
                // skip the empty strings
                if(StringUtils.hasValue(parts[i])) {
                    if(first) {
                        newPath.append(parts[i]);
                        first = false;
                    }
                    else if (i == (parts.length - 1) )
                        newPath.append("/").append(parts[i]);
                    else
                        newPath.append(".").append(parts[i]);
                }
                   
            }
                                         
                
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                          newPath.toString(), RepositoryManager.URL_HANDLER);

        }
        
        else
            return null;
        
      } catch (MalformedURLException e) {
          Debug.error("Failed to convert to url, skipping url as a resource.");
          return null;
      }
            
      if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
          Debug.log(Debug.MSG_STATUS,"Formed a new url [" + url +"]");
      
      try {
          
          URLConnection con = url.openConnection();
          in = con.getInputStream();
      }
      catch (IOException i) {
          throw new TransformerException(i.getMessage());
      }
      

      if (in == null)
          throw new TransformerException("Failed to load href.");


      return new StreamSource(in, url.toString());

   }


}

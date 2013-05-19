/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  java.io.*;
import  java.net.*;
import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.http.*;
import  javax.servlet.jsp.*;

import  org.w3c.dom.*;

import  com.nightfire.framework.constants.*;
import  com.nightfire.framework.message.MessageException;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import  com.nightfire.framework.repository.*;
import  com.nightfire.framework.util.*;
import  com.nightfire.security.*;
import com.nightfire.security.tpr.TradingPartnerRelationship;
import com.nightfire.security.tpr.TPRException;
import  com.nightfire.webgui.core.beans.*;
import  com.nightfire.framework.debug.*;
import  com.nightfire.webgui.core.resource.*;

/**
 * <p><strong>ServletUtils</strong> provides a collection of utility
 * functions for use by servlet-related components.</p>
 */

public final class ServletUtils implements ServletConstants
{

    public static final String TRANSFORM_DIR_IN = "in";
    public static final String TRANSFORM_DIR_OUT = "out";
    public static final String TRANSFORM_DIR_NONE = null;


    /**
     * This convenient method creates and returns a new object base on the given
     * class-name and expected class-type.
     *
     * @param  className     The class name of the object to be created.
     * @param  expectedType  The expected type of the class.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  The newly created object.
     */
    public static final Object createObject(String className, String expectedType) throws ServletException
    {
        return createObject(className, null, expectedType);
    }

    /**
     * This convenient method creates and returns a new object base on the given
     * class-name, arguments, and expected class-type.
     *
     * @param  className        The class name of the object to be created.
     * @param  constructorArgs  Arguments to pass to the constructor.
     * @param  expectedType     The expected type of the class.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  The newly created object.
     */
    public static final Object createObject(String className, Object[] constructorArgs, String expectedType) throws ServletException
    {

        if (!StringUtils.hasValue(className))
        {
            String errorMessage = "ERROR: ServletUtils.createObject(): The class name of the object to be created is missing or invalid: [" + className + "].";

            //log.error(errorMessage);

            throw new ServletException(errorMessage);
        }

        try
        {
            return ObjectFactory.create(className, constructorArgs, Class.forName(expectedType));
        }
        catch (FrameworkException e)
        {
            String errorMessage = "ERROR: ServletUtils.createObject(): ObjectFactory is unable to create an object of class [" + className + "].\n" + e.getMessage();

            //log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
        catch (ClassNotFoundException e)
        {
            String errorMessage = "ERROR: ServletUtils.createObject(): Unable to create a Class object for class [" + expectedType + "]." + e.getMessage();

            //log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }

    /**
     * This static method sends a request to the Repository Manager.
     *
     * @param  request  Request XML.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  Response XML returned from the Repository Manager.
     */
    public static final String sendMetaDataRequest(String request) throws ServletException
    {
        // Currently, the call to Repository Manager will just be a straight API
        // call on the RepositoryManager object, which is assumed to be running in
        // the same Java process.

      //  if (log.isDebugEnabled())
      //  {
      //       log.debug("sendMetaDataRequest(): Sending the following request to Repository Manager:\n" + request);
      //  }

        try
        {
            RepositoryManager repositoryManager = RepositoryManager.getInstance();

            String            response          = repositoryManager.getMetaDataFromPath(request);

            //  if (log.isDebugEnabled())
            //  {
            //     log.debug("sendMetaDataRequest(): Repository Manager replies with the following response:\n" + response);
            // }

            return response;
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ServletUtils.sendMetaDataRequest(): Failed to have the Repository Manager process the request.\n" + e.getMessage();

            // log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }


    /**
     * This method dispatches the request to the indicated "target" resource for
     * further processing.  The "target" resource is usually another JSP page.
     *
     * @param  context Destination ServletContext to go to.
     * @param  req     ServletRequest object.
     * @param  resp    ServletResponse object.
     * @param  target  Usually a JSP page, to continue with the processing.
     *
     * @exception  ServletException  Thrown if unable to dispatch the request to
     *                               the indicated resource.
     */
    public static final void dispatchRequest(ServletContext context, ServletRequest req, ServletResponse resp, String target) throws ServletException
    {
        RequestDispatcher dispatcher;
        String contextName =  null;
        DebugLogger log = null;

        try
        {
            if (context != null) {
               dispatcher = context.getRequestDispatcher(target);
               contextName = context.getServletContextName();
               String webAppName = ServletUtils.getWebAppContextPath(context);
               log = DebugLogger.getLogger(webAppName, ServletUtils.class);
            } else {
               dispatcher = req.getRequestDispatcher(target);
               log = DebugLogger.getLoggerLastApp(ServletUtils.class);
            }
            if (log.isDebugEnabled()) {
                log.debug("dispatchRequest(): Dispatching request processing task to resource [" + target + "], context [" + contextName + "]");
            }

            dispatcher.forward(req, resp);
        }
        catch (Exception e)
        {
            Exception root = getRootException(e);


            String errorMessage = "Failed to dispatch request processing task to resource [" + target + "], context [" + contextName + "].\n";

            if ( root instanceof ServletException) {
                throw ((ServletException) root);
            }
            else {
                throw new ServletException(errorMessage, e);
            }

        }
    }



    /**
     * Utility method that obtains the lowest root cause that
     * is a servlet exception or jsp exception and does not have a null
     * root cause.
     *
     * @param  t  A Throwable instance to check.
     *
     * @return ServletException, JspException, or null
     */
    private static Exception getRootException(Throwable t)
    {
        Throwable rootCause = null;
        Exception lastGoodEx = null;
        Exception result = null;

        DebugLogger log = DebugLogger.getLoggerLastApp(ServletUtils.class);

        if (t instanceof ServletException) {
            result = (ServletException) t;
            rootCause = ((ServletException)result).getRootCause();
        }


        if (rootCause == null)
            return result;

        // traverse down root causes until we find
        // the last root cause that is not null
        while (rootCause instanceof ServletException) {
            // shift the pointer to the servlet or jsp exception which
            // has the root cause.

            result = ((ServletException)rootCause);
            rootCause = ((ServletException)result).getRootCause();

            if (rootCause == null)
                break;

        }

        return result;


    }

     /**
     * This method dispatches the request to the indicated "target" resource for
     * further processing.  The "target" resource is usually another JSP page.
     *
     * @param  req     ServletRequest object.
     * @param  resp    ServletResponse object.
     * @param  target  Usually a JSP page, to continue with the processing.
     *
     * @exception  ServletException  Thrown if unable to dispatch the request to
     *                               the indicated resource.
     */
    public static final void dispatchRequest(ServletRequest req, ServletResponse resp, String target) throws ServletException
    {

        dispatchRequest(null,req,resp,target);
    }

    /**
     * This method tokenizes a string based on the specified delimiter and returns
     * an array of string tokens.
     *
     * @param  string     String to be tokenized.
     * @param  delimiter  Delimiter to used.
     *
     * @return  An array of string tokens.
     */
    public static final String[] getStringTokens(String string, String delimiter)
    {
        if (!StringUtils.hasValue(string))
        {
            return null;
        }

        StringTokenizer tokenizer  = new StringTokenizer(string, delimiter);

        int             tokenCount = tokenizer.countTokens();

        if (tokenCount == 0)
        {
            return null;
        }

        String[]        tokens     = new String[tokenCount];

        int             count      = 0;

        while (tokenizer.hasMoreTokens())
        {
            tokens[count] = tokenizer.nextToken();

            count++;
        }

        return tokens;
    }




    /**
     * Generates a new message ID to be used for caching of request data.  The
     * generated ID also gets set in the session cache, to be used for the next
     * generation.
     *
     * @param  session  The HttpSession object.
     *
     * @return  A new ID.
     */
    public static final int generateMessageID(HttpSession session)
    {
        synchronized(session)
        {
            int     id        = 0;

            Integer currentID = (Integer)session.getAttribute(ServletConstants.MESSAGE_ID);

            if (currentID != null)
            {
                id = currentID.intValue() + 1;
            }

            session.setAttribute(ServletConstants.MESSAGE_ID, new Integer(id));

            //  if (log.isDebugEnabled())
            // {
            //     log.debug("generateMessageID(): Generated a new message ID with value [" + id + "].");
            // }

            return id;
        }
    }


    /**
     * Splits the data fields into separate header-data and the body-data
     * Map objects.
     * Values can be of type String or String[]. If they are of type String[] index 0 is used.
     *
     * @param  requestData  A map of all data fields to be transformed.  Only header
     *                      and body fields (distinguished by their prefixes) are
     *                      added to the output maps.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  An XMLMessageData object containing a header-data and a body-data
     *          XMLMessageParser objects.
     */
    public static final HeaderBodyData splitData(Map requestData) throws ServletException
    {

            Iterator            iterator        = requestData.entrySet().iterator();

            Map header = new HashMap();
            Map body  = new HashMap();


            while (iterator.hasNext())
            {
                Map.Entry entry = (Map.Entry)iterator.next();

                String    field = (String)entry.getKey();

                Object valueObj = (Object) entry.getValue();
                String    value;

                if (valueObj instanceof String)
                   value = (String)valueObj;
                else if (valueObj instanceof String[] )
                   value = ((String[])valueObj)[0];
                else
                   throw new ServletException ("ServletUtils: Unknown value type");

                value = removeNewLine (value);
                
                if (field.startsWith(ServletConstants.NF_FIELD_HEADER_PREFIX))
                {
                    String nodeName = field.substring(ServletConstants.NF_FIELD_HEADER_PREFIX.length());

                    //     if (log.isDebugEnabled())
                    //  {
                    //     log.debug("splitData(): Adding header node [" + nodeName + "], with value [" + value + "] ...");
                    // }

                    header.put(nodeName, value);
                }
                else if (field.startsWith(ServletConstants.NF_FIELD_PREFIX))
                {
                    String nodeName = field.substring(ServletConstants.NF_FIELD_PREFIX.length());

                    //  if (log.isDebugEnabled())
                    //  {
                    //     log.debug("splitData(): Adding body node [" + nodeName + "], with value [" + value + "] ...");
                    //  }

                    body.put(nodeName, value);
                }
            }

       return new HeaderBodyData(header, body);


    }

    /**
     * Strips off the line separators or single linefeed or carriage return and put space.
     *
     * @param str String to operate
     * @return processed String
     */
    public static String removeNewLine(String str)
    {
        str = StringUtils.replaceSubstrings(str, "\n", " ");
        str = StringUtils.replaceSubstrings(str, "\r", " ");
        return str;
    }

    /**
     * Reads and returns the data content pointed to by the specified URL.
     *
     *
     * @param  url        The URL of the desire data.
     * @param  useCaches  Indicates whether to use URLConnection's caches.
     * @param  enc The specified character encoding. (if null then system default is used).
     * @exception  FrameworkException  Thrown when an error occurs during processing.
     *
     * @return  Data content.
     */
    public static final String getURLContent(URL url, boolean useCaches, String enc) throws FrameworkException
    {
        Reader r = null;
        int bytesRead = 0;
        char[] buff = new char[1024];
        StringBuffer sb = new StringBuffer();

        try
        {
            URLConnection  connection   = url.openConnection();
            connection.setUseCaches(useCaches);

            // open the URL
            // use the specified encoding if it is not null,
            // otherwise use system default.
            if (StringUtils.hasValue(enc) )
               r = new BufferedReader(new InputStreamReader(connection.getInputStream(), enc));
            else
               r = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            // read the whole thing
            while ( (bytesRead = r.read(buff)) != -1)
            {
                sb.append(buff, 0, bytesRead);
            }
        }
        catch (IOException ex)
        {
            throw new FrameworkException(ex);
        }
        finally
        {
            try
            {
                if (r != null)
                r.close();
            }
            catch (Exception ex)
            {
              // log.warn("Could not close url ["
              //            + url + "]: " + ex);
            }
        }

        String dataContent = sb.toString();

        //  if (log.isDebugEnabled())
        //   log.debug("getURLContent(): The data content of URL [" + url.toString() + "] is:\n" + dataContent);

        // return the string
        return dataContent;

    }




    /**
     * Copies (deep clone) all children from one parent node to another
     *  and skips any attributes with the name of 'id'.
     *
     * @param dest   The node to copy the children to
     * @param source The node to copy the children from
     */
    public static final void copyChildrenNoIds(Node dest, Node source) throws MessageException
    {

       Document doc = null;

       // if the dest is a document calling getOwnerDocument will
       // cause problems when we try to append
       // later
       if (dest instanceof Document)
          doc = (Document) dest;
       else
          doc = dest.getOwnerDocument();


       Node child = source.getFirstChild();
       Node n = null;

       while (child != null)
       {
          // make a copy
          n = copyAndStripIds(doc, child);

          //  add it
          dest.appendChild(n);

          child = child.getNextSibling();
       }
    }




    /**
     * Does a deep copy from source to dest, and skips any attributes with the name of 'id'.
     * @param dest - The destination node to add the copy to.
     * @param source - The source node to copy. This can be in another document.
     *
     */
    public static final void copyNoIds(Node dest, Node source)  throws MessageException
    {

       Document doc = null;

        // if the dest is a document calling getOwnerDocument will
       // cause problems when we try to append
       // later
       if (dest instanceof Document)
          doc = (Document) dest;
       else
          doc = dest.getOwnerDocument();

       Node n = copyAndStripIds(doc, source);

        dest.appendChild(n);

    }


    // helper method to get the session
    // if it returns null we can assume that
    // the object is a servlet context
    private static final HttpSession getSession(Object req)
    {

        if (req instanceof HttpServletRequest)
            return ((HttpServletRequest)req).getSession();
        else if ( req instanceof HttpSession)
            return (HttpSession)req;
        else
            return null;

    }



     /**
     * Obtains a required local resource from the resource data cache.
     * @param req The associated request object, or session, or context.
     * NOTE: The following objects can be passed in, ordered from best to worse:
     *       <OL type=A>
     *           <LI>{@link javax.servlet.http.HttpServletRequest}</LI>
     *           <LI>{@link javax.servlet.http.HttpSession}</LI>
     *           <LI>{@link javax.servlet.ServletContext}</LI>
     *      </OL>
     * @param resourcePath The resource url path relative to the current webapp. This must start with '/'
     * @param resourceType The type of resource to load.
     *
     * @exception throws an exception if the resource is missing or the resource can not be loaded.
     *
     * @see com.nightfire.webgui.core.resource.ResourceDataCache
     */

    public static final Object getLocalResource(Object req, String resourcePath, String resourceType) throws ServletException
    {
        return getLocalResource(req, resourcePath, resourceType, true);
    }





    /**
     * Returns the real resource path to an object.
     *
     * @param req The associated request object, or session, or context.
     * NOTE: The following objects can be passed in, ordered from best to worse:
     *       <OL type=A>
     *           <LI>{@link javax.servlet.http.HttpServletRequest}</LI>
     *           <LI>{@link javax.servlet.http.HttpSession}</LI>
     *           <LI>{@link javax.servlet.ServletContext}</LI>
     *      </OL>
     * @param resourcePath a <code>String</code> value
     * @return a <code>String</code> value
     * @exception ServletException if an error occurs
     */
    public static final String getLocalResourceFullPath(Object req, String resourcePath) throws ServletException
    {
        HttpSession session = getSession(req);


        URLConverter converter = null;

        if ( session == null)
            converter = (URLConverter) ((ServletContext)req).getAttribute(ServletConstants.DEFAULT_RESOURCE_URL_CONVERTER);
        else
            converter = (URLConverter) session.getAttribute(ServletConstants.RESOURCE_URL_CONVERTER);


        if ( converter == null ) {
            throw new ServletException("URLConverter could not be located.");
        }


        try {
            return converter != null ? converter.getURL(resourcePath).toString() : null;
        }
        catch (MalformedURLException e){
            throw new ServletException(e);
        }

    }

    /**
     * Checks if the specified resource is loaded.
     *
     * @param req The associated request object, or session, or context.
     * NOTE: The following objects can be passed in, ordered from best to worse:
     *       <OL type=A>
     *           <LI>{@link javax.servlet.http.HttpServletRequest}</LI>
     *           <LI>{@link javax.servlet.http.HttpSession}</LI>
     *           <LI>{@link javax.servlet.ServletContext}</LI>
     *      </OL>
     * @param resourcePath a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static final boolean isLocalResourceLoaded(Object req, String resourcePath)
    {

        HttpSession session = getSession(req);
        ServletContext context = null;


        URLConverter converter = null;


        if ( session == null) {
            converter = (URLConverter) ((ServletContext)req).getAttribute(ServletConstants.DEFAULT_RESOURCE_URL_CONVERTER);
            context = (ServletContext)req;
        } else {
            converter = (URLConverter) session.getAttribute(ServletConstants.RESOURCE_URL_CONVERTER);
            context = session.getServletContext();

        }

        DebugLogger log = getLogger(context);


        if ( converter == null ) {
            log.error("Resource URLConverter could not be located, returning false");
            return false;

        }



        try {

           ResourceDataCache resourceDataCache = (ResourceDataCache)context.getAttribute(ServletConstants.RESOURCE_DATA_CACHE);

           if ( resourceDataCache == null ) {
               log.error("ResourceDataCache could not be located.");
               throw new ServletException("ResourceDataCache could not be located.");
           }

           return resourceDataCache.isResourceLoaded(resourcePath, converter);


        } catch (Exception e) {
            return false;
        }

    }

  /**
     * Obtains a local resource from the resource data cache.
     * @param req The associated request object, or session, or context.
     * NOTE: The following objects can be passed in, ordered from best to worse:
     *       <OL type=A>
     *           <LI>{@link javax.servlet.http.HttpServletRequest}</LI>
     *           <LI>{@link javax.servlet.http.HttpSession}</LI>
     *           <LI>{@link javax.servlet.ServletContext}</LI>
     *      </OL>
     * @param resourcePath The resource url path relative to the current webapp. This must start with '/'
     * @param resourceType The type of resource to load.
     * @param isRequired - Indicates if a an exception should be thrown if the resource does not exist.
     *   true - throws a an exception if the resource is missing.
     *   false - returns a null resource if the resource is missing.
     * @exception throws an exception if the resource is missing(only if required) or the resource can not be loaded.

     * @see com.nightfire.webgui.core.resource.ResourceDataCache
     */
    public static final Object getLocalResource(Object req, String resourcePath, String resourceType, boolean isRequired) throws ServletException
    {


        URL resourceURL = null;

        Object resourceRes = null;

        HttpSession session = getSession(req);
        ServletContext context = null;


        URLConverter converter = null;


        if ( session == null) {
            converter = (URLConverter) ((ServletContext)req).getAttribute(ServletConstants.DEFAULT_RESOURCE_URL_CONVERTER);
            context = (ServletContext)req;
         } else {
            converter = (URLConverter) session.getAttribute(ServletConstants.RESOURCE_URL_CONVERTER);
            context = session.getServletContext();
        }

        DebugLogger log = getLogger(context);


        if ( converter == null ) {
            log.error("Resource URLConverter could not be located.");
            throw new ServletException("URLConverter could not be located.");
        }


        long startTime = Benchmark.startTiming(log);

        try {

           ResourceDataCache resourceDataCache = (ResourceDataCache)context.getAttribute(ServletConstants.RESOURCE_DATA_CACHE);

           if ( resourceDataCache == null ) {
               log.error("ResourceDataCache could not be located.");
               throw new ServletException("ResourceDataCache could not be located.");
           }

            if ( log.isDebugEnabled() ) {
                log.debug("getLocalResource() : Trying to load resource [" + resourcePath + "] resource Type [" + resourceType +"]");

            }


           resourceRes = resourceDataCache.getResourceData(resourcePath, resourceType, converter);


        } catch (FrameworkException e) {
            if (isRequired) {
                String errorMessage = "getLocalResource(): Could not load resource [" + resourcePath + "]: " + e.getMessage();
                log.error(errorMessage);
                throw new ServletException(errorMessage,e);
            }
        }



        if (resourceRes == null) {
            if (isRequired ) {
                String errorMessage = "getLocalResource(): Resource [" + resourcePath + "] does not exist.";

                log.error(errorMessage);

                throw new ServletException(errorMessage);
            } else if (log.isDebugEnabled()) {
                log.debug("getLocalResource(): Resource [" + resourcePath + "] does not exist and is not required.  Returning a null ...");
            }
        }

        Benchmark.stopTiming(log, startTime, "Time taken to obtain resource reference.");

        return resourceRes;

    }


  /**
     * Obtains a local resource from the resource data cache.
     * @param req The associated request object, or session, or context.
     * NOTE: The following objects can be passed in, ordered from best to worse:
     *       <OL type=A>
     *           <LI>{@link javax.servlet.http.HttpServletRequest}</LI>
     *           <LI>{@link javax.servlet.http.HttpSession}</LI>
     *           <LI>{@link javax.servlet.ServletContext}</LI>
     *      </OL>
     * @param resourceDirPath The resource url path relative to the current webapp. This must start with '/', and points to a directory with multiple resources.
     * @param resourceType The type of resource to load.
     * @param isRequired - Indicates if a an exception should be thrown if the resource does not exist.
     *   true - throws a an exception if the resource is missing.
     *   false - returns a null resource if the resource is missing.
     * @exception throws an exception if the resource is missing(only if required) or the resource can not be loaded.

     * @see com.nightfire.webgui.core.resource.ResourceDataCache
     */
    public static final Set getLocalResources(Object req, String resourceDirPath, String resourceType) throws ServletException
    {


        Set resources = null;

       HttpSession session = getSession(req);
        ServletContext context = null;


        URLConverter converter = null;


        if ( session == null) {
            converter = (URLConverter) ((ServletContext)req).getAttribute(ServletConstants.DEFAULT_RESOURCE_URL_CONVERTER);
            context = (ServletContext)req;
        } else {
            converter = (URLConverter) session.getAttribute(ServletConstants.RESOURCE_URL_CONVERTER);
            context = session.getServletContext();
        }

        DebugLogger log = getLogger(context);


        if ( converter == null ) {
            log.error("Resource URLConverter could not be located.");
            throw new ServletException("URLConverter could not be located.");
        }


        try {


           ResourceDataCache resourceDataCache = (ResourceDataCache)context.getAttribute(ServletConstants.RESOURCE_DATA_CACHE);

           if ( resourceDataCache == null ) {
               log.error("ResourceDataCache could not be located.");
               throw new ServletException("ResourceDataCache could not be located.");
           }

            if ( log.isDebugEnabled() ) {
                log.debug("getLocalResource() : Trying to load resources under [" + resourceDirPath + "], resource Type [" + resourceType +"]");
            }

           resources = resourceDataCache.getResources(resourceDirPath, resourceType, converter);

        } catch (FrameworkException e) {
            throw new ServletException(e);
        }

        if(log.isDebugEnabled() )
            log.debug("Loaded ["+ resources.size() +"] resources from path [" + resourceDirPath +"]");

        return resources;

    }


    /**
     * Builds a path to a meta resource based on values in the parameters.
     * @param service The service type for the resource.
     * @param supplier The supplier for the service.
     * @param version The supplier version. This can be null to use the default current version.
     * @param messageType The Message type of the resource file. This indicates the final name
     *                    of the resource, without the extension if any
     *                     (example: requestHeader, request, focaccept).
     * @return The path to the resource. Assumes all resources live under /WEB-INF/resources.
     * @exception ServletException if a required parameter is missing or an invalid type is passed in.
     * @exception throws an exception if the resource is missing(only if required) or the resource can not be loaded.
     *
     * For example a call of getMetaResourcePath("Loop", "SBC", null, "focaccept") will return a path of /WEB-INF/resources/Loop-SBC/meta/focaccept.xml.
     *
     * @see com.nightfire.webgui.core.resource.ResourceDataCache
     */
    public static final String getMetaResourcePath(String service, String supplier, String version, String messageType) throws ServletException
    {
        DebugLogger log = DebugLogger.getLoggerLastApp(ServletUtils.class);

        if (!StringUtils.hasValue(supplier) ||
            !StringUtils.hasValue(messageType) ||
            !StringUtils.hasValue(service)) {
            throw new ServletException("Invalid input passed to getMetaResourcePath, service [" + service +"], supplier [" + supplier +"], version [" + version +"], message type [" + messageType +"]");
        }

        supplier = getValidGUISupplier(supplier, service);

        StringBuffer buf = new StringBuffer(ServletConstants.RESOURCE_ROOT_PATH).append(service).append("-").append(supplier);
        if(StringUtils.hasValue(version))
            buf.append("-").append(version);

        buf.append( "/" );
        buf.append(ServletConstants.META_DIRECTORY);

        buf.append( "/" );
        buf.append(messageType).append(".xml");

        String retValue = buf.toString();

        if(log.isDebugEnabled() )
            log.debug( "getMetaResourcePath: Returning resource path [" + retValue + "]." );

        return retValue;
    }
    /**
     * This method returns the supplier name from the resource name passed.
     * @param resourceName : Name of the GUI Meta resource
     * @return Supplier name
     */
    public static String getResourceSupplier(String resourceName) throws ServletException
    {
        String supplier = null;
        HashMap resourceElementsMap = new HashMap();

        // Get the Map containing service, supplier and interfaceversion values.
        resourceElementsMap = getResourceElements ( resourceName );

        // Get the supplier value form resource name if exist. 
        if( resourceElementsMap != null && resourceElementsMap.containsKey ( SUPPLIER ) )
            supplier = (String) resourceElementsMap.get( SUPPLIER );

        return supplier;
    }

    /**
     * This method returns the supplier name from the resource name passed.
     * @param resourceName : Name of the GUI Meta resource
     * @return Supplier name
     */
    public static String getResourceService(String resourceName) throws ServletException
    {
        String service = null;
        HashMap resourceElementsMap = new HashMap();

        // Get the Map containing service, supplier and interfaceversion values.
        resourceElementsMap = getResourceElements ( resourceName );

        // Get the supplier value form resource name if exist.
        if( resourceElementsMap != null && resourceElementsMap.containsKey ( SERVICE_TYPE ) )
            service = (String) resourceElementsMap.get( SERVICE_TYPE );

        return service;
    }

    /**
     * This method return the map containing service, supplier and interfaceversion
     * from the resource name passed.
     * @param resourceName
     * @return Map
     */
    public static HashMap getResourceElements(String resourceName) throws ServletException
    {
        // Hashmap for storing elements of Resource - Service, Supplier, Interface-version
        HashMap resourceElementsMap = new HashMap();

        // Check whether the input is a valid resource
        if (
                ( ! StringUtils.hasValue ( resourceName ) ) ||
                ( StringUtils.indexOfIgnoreCase ( resourceName, RESOURCE_DIRECTORY ) == -1 )
           )
                throw new ServletException ( "Invalid input passed to getResourceElements, resourceName [ " + resourceName + " ]" );

        // Get substring beginning with 'resources'.  Remove extraneous path from resource name.
        String resourceSubstring = resourceName.substring ( resourceName.indexOf ( RESOURCE_DIRECTORY ) );

        // Tokenize based on directory-separator.  One of these tokens would be of the form service-supplier-interfaceVersion.
        StringTokenizer tokenizer = new StringTokenizer ( resourceSubstring, "/" );

        while( tokenizer.hasMoreTokens () )
        {
            String resourceElement = tokenizer.nextToken ();

            // '-' separates the three elements of the resource.
            if ( resourceElement.indexOf( RESOURCE_ELEMENT_SEPERATOR ) != -1 )
            {
                // Tokeinize based on element-separator to get individual element
                StringTokenizer hyphenTokenizer = new StringTokenizer ( resourceElement, RESOURCE_ELEMENT_SEPERATOR );
                if ( hyphenTokenizer.countTokens() == 3 )
                {
                    resourceElementsMap.put ( SERVICE_TYPE, hyphenTokenizer.nextToken() );
                    resourceElementsMap.put ( SUPPLIER, hyphenTokenizer.nextToken() );
                    resourceElementsMap.put ( INTERFACE_VER, hyphenTokenizer.nextToken() );

                    return resourceElementsMap;
                }
            }
        }

        return null;
    }


    /**
     * Builds a path to a template resource based on values in the parameters.
     * @param service The service type for the resource.
     * @param supplier The supplier for the service.
     * @param version The supplier version. This can be null to use the default current version.
     * @param messageType The Message type of the resource file. This indicates the final name
     *                    of the resource, without the extension if any
     *                     (example: requestHeader, request, focaccept).
     * @return The path to the resource. Assumes all resources live under /WEB-INF/resources.
     * @exception ServletException if a required parameter is missing or an invalid type is passed in.
     * @exception throws an exception if the resource is missing(only if required) or the resource can not be loaded.
     *

     * @see com.nightfire.webgui.core.resource.ResourceDataCache
     */
    public static final String getTemplateResourcePath(String service, String supplier, String version, String messageType) throws ServletException
    {
        DebugLogger log = DebugLogger.getLoggerLastApp(ServletUtils.class);

        if (!StringUtils.hasValue(supplier) ||
            !StringUtils.hasValue(messageType) ||
            !StringUtils.hasValue(service)) {
            throw new ServletException("Invalid input passed to getTemplateResourcePath, service [" + service +"], supplier [" + supplier +"], version [" + version +"], message type [" + messageType +"]");
        }

        supplier = getValidGUISupplier(supplier, service);

        StringBuffer buf = new StringBuffer(ServletConstants.RESOURCE_ROOT_PATH).append(service).append("-").append(supplier);
        if(StringUtils.hasValue(version))
            buf.append("-").append(version);

        buf.append( "/" );
        buf.append(ServletConstants.TEMPLATE_DIRECTORY);

        buf.append( "/" );
        buf.append(messageType).append(".xml");

        String retValue = buf.toString();

        if(log.isDebugEnabled() )
            log.debug( "getTemplateResourcePath: Returning resource path [" + retValue + "]." );

        return retValue;
    }



    
    /**
     * Builds a path to a transformer resource based on values in the parameters.
     * @param service The service type for the resource.
     * @param supplier The supplier for the service.
     * @param version The supplier version. This can be null to use the default current version.
     * @param dir The direction of the transformation.
     * This accepts the following constants:
     * <list>
     *   <ul>TRANFORM_DIR_IN - Transform from external to gui xml</ul>
     *   <ul>TRANFORM_DIR_OUT - Transform from gui to external xml</ul>
     *   <ul>TRANSFORM_DIR_NONE - Transform from gui to another gui xml</ul>
     * @param messageType The Message type of the resource file. This indicates the final name
     *                    of the resource, without the extension if any
     *                     (example: requestHeader, request, focaccept).
     * @return The path to the resource. Assumes all resources live under /WEB-INF/resources.
     * @exception ServletException if a required parameter is missing or an invalid type is passed in.
     * @exception throws an exception if the resource is missing(only if required) or the resource can not be loaded.
     *
     * For example a call of getTransformResourcePath("Loop", "SBC", null, "in","focaccept") will return a path of /WEB-INF/resources/Loop-SBC/xsl/focacceptIncoming.xsl.
     *
     * @see com.nightfire.webgui.core.resource.ResourceDataCache
     */
    public static final String getTransformResourcePath(String service, String supplier, String version, String dir, String messageType) throws ServletException
    {
        DebugLogger log = DebugLogger.getLoggerLastApp(ServletUtils.class);

        if (!StringUtils.hasValue(supplier) ||
            !StringUtils.hasValue(messageType) ||
            !StringUtils.hasValue(service)) {
            throw new ServletException("Invalid input passed to getTransformResourcePath, service [" + service +"], supplier [" + supplier +"], version [" + version +"], message type [" + messageType +"]");
        }

        supplier = getValidGUISupplier(supplier, service);

        StringBuffer buf = new StringBuffer(ServletConstants.RESOURCE_ROOT_PATH).append(service).append("-").append(supplier);
        if(StringUtils.hasValue(version))
            buf.append("-").append(version);

        buf.append( "/" );
        buf.append(ServletConstants.XSL_DIRECTORY);

        buf.append( "/" );
        buf.append(messageType);

        if ( dir == TRANSFORM_DIR_NONE)
            buf.append(".xsl");
        else if ( dir.startsWith(TRANSFORM_DIR_IN) )
            buf.append("Incoming.xsl");
        else if ( dir.startsWith(TRANSFORM_DIR_OUT))
            buf.append("Outgoing.xsl");


        String retValue = buf.toString();

        if(log.isDebugEnabled() )
            log.debug( "getTransformResourcePath: Returning resource path [" + retValue + "]." );

        return retValue;
    }





    private static final Node copyAndStripIds(Document doc, Node source) throws MessageException
    {
        // make a copy while stripping off id attributes
         if ( (doc == null) || (source == null) )
        {
            return null;
        }

        Node newNode = null;

        // Create appropriate type of node via Document methods, given source node's type.
        switch ( source.getNodeType() )
        {
        case Node.ELEMENT_NODE:
            newNode = doc.createElement( source.getNodeName() );

            NamedNodeMap nnm = source.getAttributes( );

            for ( int Ix = 0;  Ix < nnm.getLength();  Ix ++ )
            {
                Node n = nnm.item( Ix );
                if ( !n.getNodeName().equals("id") )
                   ((Element)newNode).setAttribute( n.getNodeName(), n.getNodeValue() );
            }
            break;


        case Node.TEXT_NODE:
            newNode = doc.createTextNode( source.getNodeValue() );
            break;


        case Node.CDATA_SECTION_NODE:
            newNode = doc.createCDATASection( source.getNodeValue() );
            break;


        case Node.ATTRIBUTE_NODE:
            if ( !source.getNodeName().equals("id") )
               newNode = doc.createAttribute( source.getNodeName() );
            break;


        case Node.PROCESSING_INSTRUCTION_NODE:
            newNode = doc.createProcessingInstruction( source.getNodeName(), source.getNodeValue() );
            break;


        case Node.COMMENT_NODE:
            newNode = doc.createComment( source.getNodeValue() );
            break;


        case Node.ENTITY_REFERENCE_NODE:
            newNode = doc.createEntityReference( source.getNodeName() );
            break;


        default:
            throw new MessageException( "ERROR: Unknown node type encountered during copy operation [" +
                                        source.getNodeType() + "]" );
        }

        // If the source node has child nodes, recursively copy them
        // to newly-created target node.
        if ( (newNode != null) && source.hasChildNodes() )
        {
            Node child = source.getFirstChild();

            while ( child != null )
            {
                newNode.insertBefore( copyAndStripIds( doc, child ), null );

                child = child.getNextSibling( );
            }
        }

        return newNode;

    }


    /**
     * Returns the a boolean from a string.
     * If the boolean can not be determined then
     * it returns false
     * @param bool The string holding the boolean value.
     * @return The boolean representation of the bool parameter or false
     * if bool is null, emtpy, or invalid.
     *
     * @see com.nightfire.framework.util.StringUtils#getBoolean
     */
    public static final boolean getBoolean(String bool)
    {
       return getBoolean(bool, false);
    }

    /**
     * Returns the a boolean from a string.
     * If the boolean can not be determined then
     * it returns a default boolean.
     * @param bool The string holding the boolean value.
     * @param defaultBool The default boolean if bool is null, emtpy, or invalid.
     * @return The boolean representation of the bool parameter.
     *
     * @see com.nightfire.framework.util.StringUtils#getBoolean
     */
    public static final boolean getBoolean(String bool, boolean defaultBool)
    {
        return StringUtils.getBoolean(bool, defaultBool);
    }



    /**
     * A utility data structure which holds a header-data and a body-data
     * XMLMessageParser objects.
     */
    public static final class HeaderBodyData
    {
        public Map headerData;

        public Map bodyData;

        public HeaderBodyData(Map headerMap, Map bodyMap)
        {
            this.headerData = headerMap;

            this.bodyData   = bodyMap;
        }
    }




  /**
   * Converts an nightfire xml path (root.node.childNode)
   * to an Xpath syntax(//root/node/childNode)
   *
   * @param path A Nightfire path
   * @return A xpath version of the path.
   */
  public static final String changetoXpathIndexes(String path)
    {
      path = path.replace('.', '/');
      path = "//" + StringUtils.replaceSubstrings(path,"|", "|//");

       for (int sdx = path.indexOf('('); sdx > -1; sdx = path.indexOf('(') ) {
          int edx = path.indexOf(')', sdx +1 );
          int index = Integer.parseInt(path.substring(sdx +1 , edx) );
          index += 1;
          path = path.substring(0, sdx) + "[" + index + "]" + path.substring(edx +1);
       }

       return path;
    }

    /**
     * Returns the context path of the current webapp.
     * If the context path can not be obtained then an empty string is
     * returned.
     */
    public static final String getWebAppContextPath(ServletRequest sreq)
    {
      HttpServletRequest req = (HttpServletRequest)sreq;

      String webApp = "";
      webApp = req.getContextPath();
      if (!StringUtils.hasValue(webApp) )
        webApp = getWebAppContextPath(req.getSession().getServletContext() );

      return webApp;
    }

    /**
     * Returns the context path of the current webapp.
     * If the context path can not be obtained then an empty string is
     * returned.
     */
  public static final String getWebAppContextPath(ServletContext context)
  {

    String webappName = (String)context.getAttribute(ServletConstants.WEB_APP_NAME);
    if ( StringUtils.hasValue(webappName) )
      return webappName;


    try {
      String url = context.getResource("/").toString();
      if ( url.endsWith("/") )
        url = url.substring(0, url.length() - 1);

      webappName = url.substring(url.lastIndexOf("/") + 1 );

    }
    catch (Exception e) {
    }



    return "/" + webappName;

  }

    /**
     * This utility method allows GUI resources to perform user authorization
     * against NF Security Service.
     *
     * @param  session  HttpSession object.
     * @param  action   Action to authorize.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  true if the user is authorized to perform the action, false otherwise.
     */
    public static boolean isAuthorized(HttpSession session, String action) throws ServletException
    {
 ServletContext context = session.getServletContext();

        SessionInfoBean          sessionBean     = (SessionInfoBean)session.getAttribute(SESSION_BEAN);

        DebugLogger     logger          = getLogger(context);
        String          userName        = sessionBean.getUserId();
        String webAppName = (String)context.getAttribute(ServletConstants.WEB_APP_NAME);
        Properties initParameters;
		try {
			initParameters = MultiJVMPropUtils.getInitParameters(context,webAppName);
		} catch (FrameworkException fe) {
			logger.error("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
			throw new ServletException("Failed to get initParameters from Tomcat JVM Instance specific Properties file",fe);
		}        
        String       boxIdentifier = initParameters.getProperty(ServletConstants.BOX_IDENTIFIER);
        String   cid     = sessionBean.getCustomerId();
        String subDId = "";
        SecurityService securityService = null;

          //TODO: this is stubbed out until security stuff is ready
        try
        {
            securityService = SecurityService.getInstance(cid);
            subDId = SecurityService.getInstance(cid).getSubDomainId ( userName );
        }
        catch (com.nightfire.security.SecurityException e)
        {
            String errorMessage = "Failed to obtain a SecurityService instance:\n" + e.getMessage();

            logger.error("isAuthorized(): " + errorMessage);

            throw new ServletException(errorMessage);
        }

        boolean isAuthorized = securityService.authorize(userName, action);

        if( StringUtils.hasValue(boxIdentifier)  && isAuthorized /*if user does not have permission then no need to check for boxes*/)
        {
            // converting action to upper case
            String upperCaseAction = action.toUpperCase();

            if(boxIdentifier.equalsIgnoreCase(ServletConstants.SOA_BOX_IDENTIFIER))
            {
                // checking if "SOAADMIN" word is present or not  
                if( upperCaseAction.indexOf("SOAADMIN") != -1)
                {
                     // check if user belongs to any subdomain then return false
                     if(StringUtils.hasValue(subDId))
                     {
                         isAuthorized = false;
                     }
                }
                //returning true if permission has "SOA" word in it
                // or the upper case action is equal to one the mentioned below
                // and as the action is in upper case so permission name
                // should be in upper case (these are common permissions of Basic and SOA)
                else
                {
                     isAuthorized = upperCaseAction.indexOf(ServletConstants.SOA_BOX_IDENTIFIER) != -1 ||
                                    upperCaseAction.equals("BASICGUIANYMODIFY") ||
                                    upperCaseAction.equals("BASICGUIANYVIEW") ||
                                    upperCaseAction.equals("REPORTVIEW") ||
                                    upperCaseAction.equals("PORTPSVIEW");
                }
            }
            else  if(boxIdentifier.equalsIgnoreCase(ServletConstants.CH_BOX_IDENTIFIER))
            {
                if(upperCaseAction.indexOf(ServletConstants.SOA_BOX_IDENTIFIER) != -1)
                {
                    isAuthorized = false;
                }
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("isAuthorized(): User [" + userName + "]'s authorization status for action [" + action + "] is [" + isAuthorized + "].");
        }

        return isAuthorized;
    }

    /**
     * This methods obtains the first cookie with the specified name from the
     * request object.
     *
     * @param  request  HttpServletRequest object.
     * @param  name     Name of the cookie.
     *
     * @return  First cookie with the specified name.
     */
    public static Cookie getCookie(HttpServletRequest request, String name)
    {
        Cookie[] cookies = request.getCookies();

        if (cookies != null)
        {
            for (int i = 0; i < cookies.length; i++)
            {
                String cookieName = cookies[i].getName();

                if (cookieName.equals(name))
                {
                    return cookies[i];
                }
            }
        }

        return null;
    }

    /**
     * An internal convenient method for obtaining a logger for outputing log
     * statements.
     *
     * @param  context  ServletContext object.
     *
     * @return  DebugLogger object.
     */
    private static DebugLogger getLogger(ServletContext context)
    {
        if (context != null)
        {
            String webAppName = getWebAppContextPath(context);

            return DebugLogger.getLogger(webAppName, ServletUtils.class);
        }

        return DebugLogger.getLoggerLastApp(ServletUtils.class);
    }
    /**
     * Obtains the Gateway Supplier correspoding to the provided
     * trading partner. And if there is no Gateway Supplier corresponding
     * to it then same value is returned
     *
     * @param  tradingPartnerName
     * @param  serviceName
     *
     * @return  String object (either valid Gateway Supplier or input as it is).
     */
    private static String getValidGUISupplier (String tradingPartnerName, String serviceName)
    {
        String gwSuppName = tradingPartnerName;
        DebugLogger log = DebugLogger.getLoggerLastApp(ServletUtils.class);

        try
        {
            CustomerContext customerContext = CustomerContext.getInstance();
            TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(customerContext.getCustomerID());
            if (tpr.isTradingPartner(tradingPartnerName, serviceName))
            {
                gwSuppName = tpr.getGatewaySupplier (tradingPartnerName, tpr.getValidTransaction ( serviceName ), serviceName);
            }
            if(log.isDebugEnabled() )
                log.debug( "ServletUtils.getValidGUISupplier: For trading Partner ["+tradingPartnerName+"] " +
                    "& service ["+serviceName+"] the gateway Supplier is ["+gwSuppName+"]" );
        }
        catch ( TPRException e )
        {
            if(log.isDebugEnabled() )
                log.error( e.getMessage () );
        }
        catch ( FrameworkException e )
        {
            if(log.isDebugEnabled() )
                log.error( e.getMessage () );
        }
        return gwSuppName;
    }
}

/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/resource/ResourceDataCache.java#1 $
 */

package com.nightfire.webgui.core.resource;

import  java.net.*;
import  java.util.*;
import java.util.concurrent.locks.*;

import  com.nightfire.framework.util.*;
import com.nightfire.framework.debug.*;
import java.io.IOException;



/**
 * This class provides the lookup of any webapp resources.  The two available
 * options are to always use the cached data or to use the most up-to-date data
 * based on its time-stamp.  It also supports the lookup based on either the
 * file-path or the URL object.  Note that this class is thread-safe.
 */
 
public final class ResourceDataCache
{
 
    private String dataRoot;


    private boolean                    develMode;

    private HashMap                    cache;
    
    private Map                        resourceChangeListeners;

    private ResourceTransformerFactory factory = null;
    

    private String enc = null;
    
    private DebugLogger log;


    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    


    /**
     * Indicates an XML Document resource type (org.w3c.dom.Document).
     * @see DefaultResourceFactory
     */
    public static final String XML_DOC_DATA       = DefaultResourceFactory.XML_DOC_DATA;
    

    /**
     * Indicates a resource which holds properties.
     * @see DefaultResourceFactory
     */
    public static final String PROPERTY_DATA       = DefaultResourceFactory.PROPERTY_DATA;
    

    
    
    /**
     * Constructor.
     *
     * @param  dataRoot       Root directory relative to where all resource files
     *                        reside.  If null, directory where the process gets
     *                        spawned is used.
     *
     * @param  develMode  Indicates that this should be run in development mode. If false then production mode is used.
     * @param enc The character encoding to use for all url streams. Null indicates
     * to use the system default.
     */
    public ResourceDataCache(String dataRoot, boolean develMode, String enc)
    {
        this(dataRoot, develMode, 0, enc);
    }
    

    /**
     * Constructor.
     *
     * @param  dataRoot       Root directory relative to where all resource files
     *                        reside.  If null, directory where the process gets
     *                        spawned is used.
     *
     * @param  develMode  Indicates that this should be run in development mode. If false then production mode is used.
     * @param reloadCheckSec The number of seconds when resources are checked for modification. This defaults to 900 seconds (15 minutes).
     * @param enc The character encoding to use for all url streams. Null indicates
     * to use the system default.
     * @param idleCleanupTime Resource idle time in sec after which the said resource is removed from cache to cleanup memory.
     */
    public ResourceDataCache(String dataRoot, boolean develMode, long reloadCheckSec, String enc, long idleCleanupTime)
    {
        log = DebugLogger.getLoggerLastApp(getClass());
      
        this.enc      = enc;
        
        this.dataRoot = dataRoot;

        this.develMode     = develMode;
        
        if (log.isDebugEnabled())
        {
            log.debug("Resource-data root directory is [" + dataRoot + "].");
            
            log.debug("Production mode enabled [" + !develMode + "]");
        }
                

                
        cache                   = new HashMap();

        factory                 = new DefaultResourceFactory();

        resourceChangeListeners = new HashMap();


        // start up the reloader class for production mode
        ResourceReloader loader = new ResourceReloader(reloadCheckSec, idleCleanupTime);        
        loader.start();
        
    }
    /**
     * Constructor.
     *
     * @param  dataRoot       Root directory relative to where all resource files
     *                        reside.  If null, directory where the process gets
     *                        spawned is used.
     *
     * @param  develMode  Indicates that this should be run in development mode. If false then production mode is used.
     * @param reloadCheckSec The number of seconds when resources are checked for modification. This defaults to 900 seconds (15 minutes).
     * @param enc The character encoding to use for all url streams. Null indicates
     * to use the system default.
     */
    public ResourceDataCache(String dataRoot, boolean develMode, long reloadCheckSec, String enc)
    {
             this(dataRoot, develMode, reloadCheckSec, enc, 0);
    }


    /**
     * If true then resources are checked for modification one every resource
     * access. If false, then a reloader thread is used to reloaded 
     * resources at specified intervals.
     *
     * @param  enabled  The flag indicating whether automatic reloading is
     *                        on or off.
     *
     * Note: When the flag is set to true, every resource request will check whether 
     *       the underlining resource has changed.
     */
    public void setDevelMode(boolean enabled) 
    {
        this.develMode = enabled;
    }

    /**
     * Get the flag indicating if auto update is enabled.
     *
     * 
     * Note: When the flag is true, every resource request will check whether 
     *       the underlining resource has changed.
     */
    public boolean getDevelMode() 
    {
        return develMode;
    }

    /**
     * Sets the ResourceTransformerFactory object to use.
     *
     * @param  factory  The ResourceTransformerFactory object to use.
     */
    public void setResourceTransformerFactory(ResourceTransformerFactory factory)
    {
       this.factory = factory;
    }


    public boolean isResourceLoaded(Object path, URLConverter converter)
    {
        URL url = null;
        
        try {

            if (converter == null) {
                log.warn("Converter was null, returning false");
                return false;
            }
            url = converter.getURL(path);
            
        }
        catch (MalformedURLException e ) {
            log.error("Invalid path object url could not be formed, returning false: " + e.getMessage());
            
            return false;
        }

        return isResourceLoaded(url);

    }


    public boolean isResourceLoaded(URL url)
    {

        if ( url == null)
            return false;
        
        boolean result = cache.containsKey(url.toString());

        // if the cache contains the entry
        // update enabled is on
        // then we also check of the resource has changed.
        // if it has then we return false
        // to indicate the the latest resource is not loaded.
        // 
        if (develMode && result) {
            try {
                ResourceInfo entry = (ResourceInfo)cache.get(url.toString());
                URLConnection connection = url.openConnection();
                
                if (connection.getLastModified() != entry.lastModified) 
                    result = false;                
            }
            catch (Exception e) {
                result = false;
            }
        }
       
        if ( log.isDebugEnabled() ) {
            String urlPath = url == null ? "" : url.toString();
            log.debug("Testing for real resource location [" + urlPath + "], result [" + result +"]");
        }
        return result;
        

    }


    /**
     * Obtains the resource-data based on the specified url.
     *
     * @param path An object that contains information about the location of the 
     *             resource
     * @param  type  The type of the requested resource. {@link DefaultFactory}
     *
     * @param converter A object which knows how to convert the path object into a URL.
     * @return  The requested resource.
     * @exception  FrameworkException  Thrown when an error occurs during processing.
     *
     */
    public Object getResourceData(Object path, String type, URLConverter converter) throws FrameworkException
    {
        
        
        try {
            if (converter == null) 
                throw new FrameworkException("Null converter passeding in");
            
            URL url  = converter.getURL(path);   
            if ( log.isDebugEnabled() ) {
                String urlPath = url == null ? "" : url.toString();
                log.debug("Looking for real resource location [" + urlPath + "]");
            }

            return getResourceData(url, type);
        }
        catch (MalformedURLException e) {
            throw new FrameworkException(e.toString());
        }
        
    }

    /**
     * Obtains multiple resources under the given path object of the given
     * type.
     * Note that all resources under the pathObj must be of the same type
     * or an exception will occur.
     *
     * @param path The object describing the path with multiple resources.
     * @param  type  The type of the requested resources. {@link DefaultFactory}
     *
     * @param converter A converter to obtain and convert all resources under
     * the given path object into specific resource objects.
     * @return  A set of resource objects.
     * @exception  FrameworkException  Thrown when an error occurs during processing.
     *
     */
    public Set getResources(Object path, String type, URLConverter converter) throws FrameworkException
    {

        Set resources = new HashSet();
        
       try {
           Set urls = converter.getResourceURLs(path);
           Iterator iter = urls.iterator();
           while (iter.hasNext() ) {
               URL u = (URL) iter.next();
               resources.add(getResourceData(u, type));
           }
       }
       catch (MalformedURLException e) {
           throw new FrameworkException(e.toString());
       }
       
       return resources;
       
        
    }
    
    
    /**
     * Obtains the resource-data based on the specified url.
     *
     * @param  url   The url of the requested resource.
     * @param  type  The type of the requested resource. {@link DefaultResourceFactory}
     *
     * @exception  FrameworkException  Thrown when an error occurs during processing.
     *
     * @return  The requested resource.
     */
    public Object getResourceData(URL url, String type) throws FrameworkException
    {

        
        long startTime = Benchmark.startTiming(log);

        // if the url is null throw an exception
        if (url == null)
            throw new FrameworkException("Resource url is null.");
        
        String       key   = url.toString();

        ResourceInfo entry = null;

        try {
            lock.readLock().lock();
            
            entry = (ResourceInfo)cache.get(key);
            
        }
        finally {
            lock.readLock().unlock();
        }
        

        if (entry == null)
        {
            log.debug("getResourceData(): An entry with key [" + key + "] does not exist in the cache.  Creating a new resource ...");
            
            try {
  
                lock.writeLock().lock();
                entry = (ResourceInfo)cache.get(key);
                // if the entry is not null now another thread beat us to the
                // update
                if (entry != null)
                    return entry.resource;
                
                
                URLConnection connection = url.openConnection();

                entry                    = addToCache(url, connection, type, key);

                notifyResourceChangeListeners(entry, null);

                if (log.isDebugEnabled())
                {
                    log.debug("getResourceData(): Added a new entry to the cache, with key [" + key + "] and value:\n" + entry.getDescription());
                }

                return entry.resource;
                
            }
            catch (Exception e) {
                log.error("getResourceData(): Failed to add a new entry into the cache: " + e.getMessage());
                log.error("",e);
                throw new FrameworkException(e);

            } finally {
                lock.writeLock().unlock();
                
            }
            
        }
        
            
        if (develMode) 
        {
            try
            {
                URLConnection connection = url.openConnection();

                lock.writeLock().lock();
                
 
                if (isResourceChanged(entry, url)) {
                        if (log.isDebugEnabled())
                            {
                                log.debug("getResourceData(): The requested resource[or one of the referred resources] has been modified.  Updating the cached resource with key [" + key + "] ...");
                            }
                    
                        ResourceInfo previousEntry = entry;

                        entry                      = addToCache(url, connection, type, key);

                        notifyResourceChangeListeners(entry, previousEntry);
                    }
               
 
                
            }
            catch (Exception e)
            {
                log.error("getResourceData(): Failed to update the modified resource in the cache: " + e.getMessage());
                log.error("",e);
                throw new FrameworkException(e);
            }
            finally {
                lock.writeLock().unlock();
            }
            
        }


        Benchmark.stopTiming(log, startTime, "Time taken to load resource.");

        //update last access time of the returned resource with current time so as to save resource from being removed from cache.
        entry.lastAccessed=System.currentTimeMillis();

        if(log.isDebugEnabled() )
            log.debug("Returning resource: " + entry.getDescription());
            
        return entry.resource;
    }


    private boolean isResourceChanged(ResourceInfo entry, URL url) 
    {
        try {
            
            URLConnection connection = url.openConnection();
        
            if (connection.getLastModified() != entry.lastModified)
                return true;
        
            List refs = entry.refs;
        
            if ( refs != null && refs.size() > 0) {
                Iterator iter = refs.iterator();
                while(iter.hasNext()) {
                    RefResource ref = (RefResource) iter.next();
                    if( ref.url.openConnection().getLastModified() != ref.lastModified)
                        return true;
                }
            }
        
        }
        catch (IOException e) {
            log.error("Could not open connection for main resource [" + url + "]:" + e.getMessage());
            return false;
        }
        
        

        return false;
    }
    
            

         

    /**
     * Convenient method for adding an entry to the cache.
     *
     * @param  url        The url of the resource.
     * @param  connection The URLConnection object.
     * @param  type       The type of the resource. {@link DefaultFactory}
     * @param  key        The key to lookup the cached resource.
     *
     * @exception  Exception  Thrown when an error occurs during processing
     *
     * @return  The ResourceInfo object which had been added to the cache.
     */
    private ResourceInfo addToCache(URL url, URLConnection connection, String type, String key) throws Exception
    {

        // before using any connections make sure all caching is off.

        
        connection.setDefaultUseCaches(false);
        

        connection.connect();
        long                lastModified    = connection.getLastModified();
   
        ResourceTransformer transformer = factory.create(type);

       
        
        // if we still can't create the transformer throw and exception
        if (transformer == null) {
           String error = "ResourceDataCache failed to create resource [" +url.toString() +"], resource type [" + type +"]";
           log.error(error);
           throw new FrameworkException( error);
        }
        if ( log.isDebugEnabled() )
          log.debug("Created resource [" + type +"]");

        Object              transformedData = transformer.transformData(url, enc);

        ResourceInfo        entry           = new ResourceInfo(type, key, transformedData, lastModified);

        List addUrlResources = transformer.getReferredResources(url, enc);

        
        if (addUrlResources != null) {
            List refs = new ArrayList();
            Iterator iter = addUrlResources.iterator();    
            while(iter.hasNext() ) {
                URL refUrl = (URL)iter.next();
                // if the referenced resource equals the main resource don't
                // include it
                if ( !refUrl.toString().equals(url.toString())) {
                    URLConnection c = refUrl.openConnection();
                    long refModified    = c.getLastModified();
                    refs.add(new RefResource(refUrl, refModified));
                }
                
            }
            
            entry.refs = refs;
        }
        
        cache.put(key, entry);

        return entry;
    }
  
    /**
     * This method adds a ResourceChangeListener object to its interal list of
     * interested party to be notified when a resource changes.
     *
     * @param  listener  A ResourceChangeListener object.
     */
    public void addResourceChangeListener(ResourceChangeListener listener)
    {
        synchronized (resourceChangeListeners)
        {
            if ((listener != null) && !resourceChangeListeners.containsKey(listener))
            {                
                resourceChangeListeners.put(listener, null);   
            }
        }
    }
     
    /**
     * This method adds a ResourceChangeListener object to its interal list of
     * interested party to be notified when a resource changes.
     *
     * @param  listener      A ResourceChangeListener object.
     * @param  resourceType  The type of resource to register for notification.
     */
    public void addResourceChangeListener(ResourceChangeListener listener, String resourceType)
    {
        synchronized (resourceChangeListeners)
        {
            if (listener != null)
            {
                if (resourceChangeListeners.containsKey(listener))
                {
                    log.warn("addResourceChangeListener(ResourceChangeListener, String): The same ResourceChangeListener instance already exists in the list with resource type of [" + resourceChangeListeners.get(listener) + "].  Replacing it with resourceType [" + resourceType + "] ...");
                }
                
                resourceChangeListeners.put(listener, resourceType);   
            }
        }
    }
    
    /**
     * This method removes a ResourceChangeListener object from its interal list
     * of interested party to be notified when a resource changes.
     *
     * @param  listener  A ResourceChangeListener object.
     */
    public void removeResourceChangeListener(ResourceChangeListener listener)
    {
        synchronized (resourceChangeListeners)
        {
            if (listener != null)
            {
                resourceChangeListeners.remove(listener);   
            }
        }
    }
  
    /**
     * Notifies all the registered ResourceChangeListener instances of the updated
     * resource.
     *
     * @param  newResourceInfo  New resource information.
     * @param  oldResourceInfo  Previous resource information.
     */
    private void notifyResourceChangeListeners(ResourceInfo newResourceInfo, ResourceInfo oldResourceInfo)
    {        
        Set listenerSet = resourceChangeListeners.entrySet();

        if (listenerSet != null)
        {
            Object[]            listeners = listenerSet.toArray();
            
            ResourceChangeEvent event     = null;
            
            if (log.isDebugEnabled())
            {
                log.debug("notifyResourceChangeListeners(): There are [" + listeners.length + "] listeners to be notified of the change in resource [" + newResourceInfo.id + "].");
            }
            
            if (listeners.length > 0)
            {

                if ( oldResourceInfo == null )
                   event = new ResourceChangeEvent(newResourceInfo.id, newResourceInfo.type, newResourceInfo.resource, null);
                else
                   event = new ResourceChangeEvent(newResourceInfo.id, newResourceInfo.type, newResourceInfo.resource, oldResourceInfo.resource);
            }
            
            for (int i = 0; i < listeners.length; i++)
            {
                Map.Entry              entry        = (Map.Entry)listeners[i];
                
                ResourceChangeListener listener     = (ResourceChangeListener)entry.getKey();
                
                String                 resourceType = (String)entry.getValue();
                
                // Only notify the listener which did not specified a type of resource
                // to listen to, or the listener whose specified resource type matches
                // the changed resource.
                
                if (!StringUtils.hasValue(resourceType) || resourceType.equals(newResourceInfo.type))
                {
                    listener.resourceChange(event);      
                }
            }
        }
    }

    public String describe() 
    {
        Map tempMap = new HashMap(cache);
        StringBuffer buf = new StringBuffer();
        buf.append("Resource contents: \n");
        
        Iterator iter = tempMap.values().iterator();
        while (iter.hasNext()) {
            ResourceInfo ri = (ResourceInfo)iter.next();
            buf.append(ri.getDescription() +"\n");
        }
        
        return buf.toString();
    }
    
    
    /**
     * A data structure which wraps the actual resource and its time-stamp.  This is
     * what gets returned to the invoker of the getResourceData(...) method. 
     */
    private static final class ResourceInfo
    {        
        public String type;

        public String id;
        
        public Object resource;

        public long   lastModified;

        public long   lastAccessed;

        public List refs;
        



        /**
         * Constructor.
         *
         * @param  type          Resource type.
         * @param  id            Resource identification.
         * @param  resource      Resource object.
         * @param  lastModified  Resource's last time-stamp.
         */
        public ResourceInfo(String type, String id, Object resource, long lastModified)
        {
            this.type         = type;
            
            this.id           = id;
            
            this.resource     = resource;
            
            this.lastModified = lastModified;

            // Initally set new resources lastAccessed to current time.
            this.lastAccessed = System.currentTimeMillis();
        }
        
        
        /**
         * Returns a formatted string representation of this object.
         *
         * @return  Formatted string representation.
         */
        public String getDescription()
        {
            StringBuffer description = new StringBuffer();
            
            description.append("\n  ResourceInfo:");
            description.append("\n     Id [" + id + "]");
            description.append("\n     Resource Type [" + type + "]");
            description.append("\n     Resource Class [" + resource.getClass().getName() + "]");
            description.append("\n     Last Modified [" + lastModified + "]");
            description.append("\n     Last Accessed [" + lastAccessed + "]");

            if (refs !=null && refs.size() > 0) {
                Iterator iter = refs.iterator();
                while (iter.hasNext() ) {
                    
                    RefResource r = (RefResource) iter.next();
                    
                    description.append("\n     ->Referred Resource:");
                    description.append("\n        Id [" + r.url.toString() + "]");
                    description.append("\n        Last Modified [" + r.lastModified + "]");
                }
            }
            
            return description.toString();
        }

        public String toString() 
        {
            return getDescription();
        }
        
    }


    /**
     * An included or referenced resource. This resource is not called directly but
     * included to referred to by a main resource. This class is used to 
     * track an included resource so that if it changes the main resource can be 
     * reloaded.
     *
     */
    private final class RefResource 
    {

        public URL url;

        public long   lastModified;

        public RefResource(URL url, long lastModified) 
        {
            this.url = url;
            this.lastModified = lastModified;
        }
    }



  private final class ResourceReloader extends Thread
  {
    private String appName;

    private long delay = 900000;
    // Default Cleanup time (3 hours)
    private long idleCleanupTime = 10800000;
    private boolean interrupted = false;
    DebugLogger log;
     

    ResourceReloader(long checkTime, long idleCleanupTime) 
    {
               
      log = DebugLogger.getLoggerLastApp(getClass());
        
      if (idleCleanupTime > 0)
      {
         this.idleCleanupTime = idleCleanupTime * 1000;
      }
        
      if ( checkTime > 0 )
         delay = checkTime * 1000;

       setDaemon(true);
       
    }
    
    public void run() 
    {
      try {
        Thread.currentThread().setPriority( Thread.MIN_PRIORITY );
        
        boolean changed = false;
        
        HashMap tmpCache = null;

        
        
        while (!interrupted) {

            log.info("Checking for changed resources.");  

            //synchronized (this) { 
            try {
                lock.readLock().lock();
                
                tmpCache = new HashMap(cache);
            } 
            finally {
                lock.readLock().unlock();
            }
            
            
        
            int beforeSize  = tmpCache.size();
            
            Iterator iter = tmpCache.values().iterator();
            
            while (iter.hasNext() ) {
                ResourceInfo ri = (ResourceInfo) iter.next();
                try {
                    
                    URL url = new URL(ri.id);
                    //log.debug("Checking resource: " + ri);
                    
                    if (isResourceChanged(ri, url)) {
                        log.info("Resource changed, removing to force a reload: " + ri);
                        changed = true;
                        iter.remove();
                    }

                    //Remove resource from cache if it is idle for more than idleCleanupTime
                    if(System.currentTimeMillis() - ri.lastAccessed > idleCleanupTime)
                    {
                        log.info("Resource not used recently, removing to free up memory: " + ri);
                        changed = true;
                        iter.remove();

                    }
                }
                catch (MalformedURLException e) {
                    // if the id is not a valid url then skip it.
                    // This should never occur since urls are always used
                    // as the key.
                }
                
            }
      
            

            // if the some resource changed them
            // perform an intersection with the 
            // the main cache, to remove changed resources.
            if (changed) {
                log.debug("Number of changed or not recently used resources [" + (beforeSize - tmpCache.size()) +"]");             
                //synchronized (this) { 
                try {
                    lock.writeLock().lock();
                    
                    cache.entrySet().retainAll(tmpCache.entrySet());
               
                } 
                finally {
                    lock.writeLock().unlock();
                }

                
            }
            else {
                log.debug("No resources changed");
            }
            
            
                
            synchronized (this) {
                wait(delay);
            }
        }
      
      } catch (InterruptedException ie) {
        log.warn("Thread got interrupted, exiting");
        interrupted = true;
      }
      
    }
   
  }


}

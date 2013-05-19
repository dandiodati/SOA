/**
 * Copyright (c) 1997 - 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header:$
 */

package com.nightfire.framework.util;

import java.util.*;
import java.lang.reflect.*;
import java.net.*;
import java.io.*;

import com.nightfire.framework.cache.*;

/**
 * The ObjectFactory class allows users to create an object instance, given its
 * fully-qualified name (package + class name).
 */
public final class ObjectFactory
{
    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className  Name of the class (fully-qualified by package path).
     *
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created.
     */
    public static final Object create ( String className ) throws FrameworkException
    {
        return( create( className, null, null ) );
    }


    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className     Name of the class (fully-qualified by package path).
     * @param  expectedType  The expected type of the class.
     *
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created or is of the wrong type.
     */
    public static Object create ( String className, Class expectedType ) throws FrameworkException
    {
        return( create( className, null, expectedType ) );
    }


    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className        Name of the class (fully-qualified by package path).
     * @param  constructorArgs  Arguments to pass to constructor, or null if using no-arg constructor.
     *
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created,
     *                                 or constructor args are wrong.
     */
    public static Object create ( String className, Object[] constructorArgs ) throws FrameworkException
    {
        return( create( className, constructorArgs, null ) );
    }


    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className        Name of the class (fully-qualified by package path).
     * @param  constructorArgs  Arguments to pass to constructor, or null if using no-arg constructor.
     * @param  expectedType     The expected type of the class.
     *
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created, constructor args are wrong,
     *                                 or is of the wrong type.
     */
    public static Object create ( String className, Object[] constructorArgs, Class expectedType ) throws FrameworkException
    {

       Class[] argTypes = null;

       if( constructorArgs != null )
       {

          int len = constructorArgs.length;

          argTypes = new Class [ len ];

          for ( int Ix = 0;  Ix < len;  Ix ++ )
          {
              argTypes[Ix] = constructorArgs[Ix].getClass( );
          }

       }

       return create(className, argTypes, constructorArgs, expectedType, null); 

    }


    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className        Name of the class (fully-qualified by package path).
     * @param  constructorArgTypes  Arguments used in selecting the appropriate constructor to call.
     *                              Useful if the actual arguments don't exactly match types used in selecting
     *                              constructor due to inheritance relationship which throws-off reflection API.
     * @param  constructorArgs  Arguments to pass to constructor, or null if using no-arg constructor.
     * @param  expectedType     The expected type of the class. 
     * @param  path             Path to the directory or archive (.jar or .zip)
     *                          from which the class should
     *                          be loaded.
     *
     *                     outside of the specified path.
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created, constructor args are wrong,
     *                                 or is of the wrong type.
     */
    public static Object create ( String className,
                                  Class[] constructorArgTypes,
                                  Object[] constructorArgs,
                                  Class expectedType,
                                  String[] path)
                                  throws FrameworkException
    {
        return create(className, constructorArgTypes, constructorArgs,
                      expectedType, path, null);
    }
    
        

    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className        Name of the class (fully-qualified by package path).
     * @param  constructorArgTypes  Arguments used in selecting the appropriate constructor to call.
     *                              Useful if the actual arguments don't exactly match types used in selecting
     *                              constructor due to inheritance relationship which throws-off reflection API.
     * @param  constructorArgs  Arguments to pass to constructor, or null if using no-arg constructor.
     * @param  expectedType     The expected type of the class. 
     * @param  path             Path to the directory or archive (.jar or .zip)
     *                          from which the class should
     *                          be loaded.
     *
     * @param parentLoader A parent class loader used to find external classes or classes
     *                     outside of the specified path.
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created, constructor args are wrong,
     *                                 or is of the wrong type.
     */
    public static Object create ( String className,
                                  Class[] constructorArgTypes,
                                  Object[] constructorArgs,
                                  Class expectedType,
                                  String[] path, 
                                  ClassLoader parentLoader)
                                  throws FrameworkException
    {
        Object obj  = null;
        Class  type = null;
     
        try
        {

            if ( path == null || path.length == 0 ) {
                // Get the Class object representing the named object type using default class loader
                type = Class.forName( className );
            }
            else {
                // Get the Class object using file class loader
                type = loadClass(className, path, parentLoader);
            }

            // If no constructor arguments were given, call the no-arg constructor.
            if ( (constructorArgs == null) || (constructorArgs.length == 0) )
            {
                obj = type.newInstance( );
            }
            else
            {
                if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                {
                Debug.log( Debug.OBJECT_LIFECYCLE, "\tLocating constructor taking [" +
                           constructorArgTypes.length + "] arguments for class [" + type.getName() +"]");
                
                
                for ( int i = 0; i < constructorArgTypes.length; i++) {
                    Debug.log( Debug.OBJECT_LIFECYCLE, "\t" + constructorArgTypes[i].getName() + "\n" );
                  }
                }

                // Locate appropriate constructor using Class object and argument signature.
                Constructor ctor = type.getDeclaredConstructor( constructorArgTypes );
                
                

                // Create the object using the obtained constructor and arguments.
                obj = ctor.newInstance( constructorArgs );
            }
        }
        catch ( Exception e )
        {
            String errMsg = null;

            if ( e instanceof InvocationTargetException )
                errMsg = ((InvocationTargetException)e).getTargetException().toString( );
            else
                errMsg = e.toString( );

            throw new FrameworkException( "ERROR: Factory could not create instance of class [" +
                                          className + "]:\n" + errMsg );
        }

        // Check that types agree, if caller requests it.
        if ( expectedType != null )
        {
            if ( !expectedType.isInstance( obj ) )
            {
                throw new FrameworkException( "ERROR: Class [" + className + "] of type [" +
                                              obj.getClass().getName() + "] is not of type [" +
                                              expectedType.getName() + "]." );
            }
        }

        // Save reference to newly-created object's class object in global (static) container
        // to get around problems due to class garbage collection.
        saveClassReference( obj.getClass().getName(), obj.getClass() );

        return obj;
    }


    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className        Name of the class (fully-qualified by package path).
     * @param  constructorArgTypes  Arguments used in selecting the appropriate constructor to call.
     *                              Useful if the actual arguments don't exactly match types used in selecting
     *                              constructor due to inheritance relationship which throws-off reflection API.
     * @param  constructorArgs  Arguments to pass to constructor, or null if using no-arg constructor.
     * @param  path             Path to the directory or archive (.jar or .zip)
     *                          from which the class should
     *                          be loaded.     
     *
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created, constructor args are wrong,
     *                                 or is of the wrong type.
     */
    public static Object create ( String className,
                                  Class[] constructorArgTypes,
                                  Object[] constructorArgs,
                                  String[] path)
                                  throws FrameworkException
    {
        return create( className, constructorArgTypes, constructorArgs, null, path );
    }

    /**
     * Create an object instance, given its fully-qualified (including package) name.
     *
     * @param  className        Name of the class (fully-qualified by package path).
     * @param  constructorArgTypes  Arguments used in selecting the appropriate constructor to call.
     *                              Useful if the actual arguments don't exactly match types used in selecting
     *                              constructor due to inheritance relationship which throws-off reflection API.
     * @param  constructorArgs  Arguments to pass to constructor, or null if using no-arg constructor.
     * @param  expectedType     The expected type of the class.
     *
     * @return  The newly-created object.
     *
     * @exception  FrameworkException  Thrown if the object can't be created, constructor args are wrong,
     *                                 or is of the wrong type.
     */
    public static Object create ( String className, Class[] constructorArgTypes, Object[] constructorArgs, Class expectedType )
                                   throws FrameworkException
    {
        return create(className, constructorArgTypes, constructorArgs, expectedType, null );
    }



   /**
    * Loads a classes bytes codes into the JVM given the fully qualified name and path
    * assuming the path given is <b>NOT</b> in the CLASSPATH.
    *
    * @param className  Name of the class (fully-qualified by package path).
    * @param  path      Paths to the directories or archives (.jar or .zip)
    *                   from which the class should be loaded.
    *
    * @exception FrameworkException if unable to construct a URL from the path or
    *                               class is not found.
    */
    public static Class loadClass ( String className, String[] path ) throws FrameworkException
    {
        return loadClass(className, path, null);
    }
    

    private static Map cachedPaths = new HashMap();
    /**
     * Loads a classes bytes codes into the JVM given the fully qualified name and path
     * assuming the path given is <b>NOT</b> in the CLASSPATH.
     *
     * @param className  Name of the class (fully-qualified by package path).
     * @param  path      Paths to the directories or archives (.jar or .zip)
     *                   from which the class should be loaded.
     *
     * @param parentLoader The parent class loader to use to find other classes not in the specified path.
     * @return a <code>Class</code> value
     * @exception FrameworkException if unable to construct a URL from the path or
     *                               class is not found.
     */
    public static Class loadClass ( String className, String[] path, ClassLoader parentLoader ) throws FrameworkException
    {

        // first check to see if we have a cached version
        // of the class available
        Class result = reloadableClasses.get(className);
        if(result != null){
           if( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) ){
              Debug.log( Debug.OBJECT_LIFECYCLE,
                         "Reloadable class ["+className +
                         "] retrieved from cache." );
           }
           return result;
        }

        URL[] urlPath = new URL[path.length];
        
        //make a local copy
               
        boolean isArchive = false;
        
        
         if ( Debug.isLevelEnabled(Debug.IO_STATUS) )
            Debug.log(Debug.IO_STATUS, "Attempting to load class ["+className+
                                        "] using custom class loader.");
            
        for ( int i= 0; i < path.length; i++ ) {
         String processedPath = (String)cachedPaths.get(path[i]);
            if(processedPath==null)
            {
                if( (path[i].endsWith(".jar") ||
                   path[i].endsWith(".zip") )){
                   isArchive = true;
                   processedPath = "jar:file:"+path[i]+"!/";
              }
              else if ( !path[i].endsWith(File.separator) ) {

                  processedPath= path[i]+File.separator;
                  processedPath = "file:"+processedPath;
                  isArchive = false;
              }
                else {
                    processedPath = "file:"+path[i];
                      isArchive = false;
            }
            cachedPaths.put(path,processedPath);
        }


          if ( Debug.isLevelEnabled(Debug.IO_STATUS) )
            Debug.log(Debug.IO_STATUS, "Adding URL to CLASSPATH ["+processedPath+"]");


           try{
               
               URL tmpURL =   new URL(processedPath);
               
               //This is the work around suggested by fix to java.sun.com bug 4386865
               //which ensures the archive is not being cached.  If this is not
               //here then jar or zip will not be re-read even if changed on file 
               //system.
                if ( isArchive ) {
                    JarURLConnection con = (JarURLConnection) tmpURL.openConnection();
                    con.setDefaultUseCaches(false);                 
               }
               urlPath[i]  = tmpURL;
            }
            catch(MalformedURLException malx){
               throw new FrameworkException("Could not locate class ["+className+
                                            "]. Classpath not valid: "+
                                            malx.getMessage() );
            }
            catch(IOException ioex){
               throw new FrameworkException("Could not change default cacheing " +
                                            " on URLConnection: " +
                                            ioex.getMessage() );
            }
        }
        
        
        
        URLClassLoader loader;
        
        if ( parentLoader == null ) 
            loader = new URLClassLoader(urlPath);
        else
            loader = new URLClassLoader(urlPath, parentLoader);

        Class loaded = null;

        try{
           loaded = loader.loadClass(className);           
           //This is a hack for java.sun.com bug 4405789, which tries to encourage the 
           //garbage collector to clean up our class loader and related resources.
           //Jars and zip files will not be re-read, even if changed on file
           //system until the classloader that first read the resource is garbaged 
           //collected.
           loader = null;
           //System.gc();
        }
        catch(ClassNotFoundException cnfex){
            StringBuffer classpath = new StringBuffer();

            for(int i=0;i<path.length;i++)
                classpath.append(path[i]+"\n");


           throw new FrameworkException("Could not find class ["+className+
                                        "] in path "+ classpath.toString() );
        }

        
        if ( Debug.isLevelEnabled(Debug.IO_STATUS) )
            Debug.log(Debug.IO_STATUS, "Successfully loaded class ["+className+
                                        "].");

        reloadableClasses.put(loaded);

        return loaded;
    }

    /**
     * Log the types of all objects dynamically created thus far.
     */
    public static synchronized void log ( )
    {

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        {
        StringBuffer sb = new StringBuffer( );

        Enumeration classNames = loadedObjectClasses.keys( );

        sb.append( "Objects dynamically created by object factory:\n " );

        while ( classNames.hasMoreElements() )
        {
            sb.append( '\t' );
            sb.append( (String)classNames.nextElement() );
            sb.append( '\n' );
        }

        Debug.log( Debug.OBJECT_LIFECYCLE, sb.toString() );
    }
    }


    /**
     * Saves named class objects to the global (static) cache to prevent class garbage colection
     * after all references to dynamically-loaded objects have been lost.
     *
     * @param  className   Name of the class (fully-qualified by package path).
     * @param  classValue  Class object.
     */
    private static final void saveClassReference ( String className, Class classValue )
    {
        if ( !loadedObjectClasses.containsKey( className ) )
        {
            Debug.log( Debug.OBJECT_LIFECYCLE, "Object factory saving reference to class object for [" + className + "]." );

            loadedObjectClasses.put( className, classValue );
        }
    }


    // Class only has static methods, so don't allow instances to be created!
    private ObjectFactory ( )
    {
        // NOT TO BE USED !!!
    }


    // Hash table containing references to all class objects for objects
    // that have been dynamically created by the object factory.
    private static Hashtable loadedObjectClasses = new Hashtable( );

    /**
    * This cache is used to store and flush Class objects that were loaded
    * by a custom ClassLoader instead of the default java class loader.
    * This collection is distinct from the loadedObjectClasses, because,
    * unlike the loadedObjectClasses table which is meant to keep the
    * loaded Classes from getting garbage collected, we specifically
    * want to be able to flush this collection of classes so that they
    * can be reloaded from their (possible updated) class or jar file. 
    *
    */
    private static ClassCacheFlusher reloadableClasses; 


    /*
     * Some JVMs garbage collect the class objects for object instances that were dynamically
     * loaded but are no longer referenced.  This causes problems for classes that rely on
     * one-time static initialization of static data members because the static initializers
     * get called multiple times (once per class loading).
     * The work-around is to maintain a reference to the class object for each class that
     * was dynamically loaded by the object factory.
     */
    static
    {
        if ( loadedObjectClasses == null )
            loadedObjectClasses = new Hashtable( );

        // Save a reference to this class so it's class doesn't get garbage collected.
        saveClassReference( ObjectFactory.class.getName(), ObjectFactory.class );

        // register the collection of "reloadable" classes so that
        // they can be flushed and then reloaded.
        reloadableClasses = new ClassCacheFlusher();
        CacheManager.getRegistrar().register( reloadableClasses );

    }


    /**
    * This class is used to keep track of manually loaded classes (i.e.
    * classes that were loaded using a custom ClassLoader instead
    * of the default java class loader). This cache serves two purposes:
    * 1) it keeps track of the classes that have been manually loaded so
    * far, so that they do not need to be manually reloaded every time
    * "create" is called, and 2) it allows this collection of manually loaded
    * classes to be flushed so that they will then be reloaded. 
    */
    private static class ClassCacheFlusher implements CachingObject{

       private Map cache = new Hashtable();

       /**
       * Puts a class into the cache.
       *
       * @param loaded The loaded Class to be put into this cache.
       */
       public void put(Class loaded){

          put( loaded.getName(), loaded );

       }

       /**
       * Puts a class with the given name into the cache.
       *
       * @param classname the name of the class
       * @param loaded The loaded Class to be put into this cache.
       */
       public void put(String classname, Class loaded){

          if( Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE) ){
             Debug.log( Debug.OBJECT_LIFECYCLE,
                        "Object factory saving reference to reloadable class ["+
                        classname + "]" );
          }

          cache.put( classname, loaded );

       }
       
       /**
       * Gets the class with the given name or null if no class with that
       * name is available.
       */
       public Class get(String classname){

          return (Class) cache.get( classname );

       }

       /**
       * Method invoked by the cache-flushing infrastructure
       * to indicate that the cache should be emptied. Flushing
       * will force the Class objects in this cache to be reloaded
       * by the ObjectFactory. 
       *
       * @exception FrameworkException never thrown.
       */
       public void flushCache() throws FrameworkException{


          if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
          Debug.log( Debug.OBJECT_LIFECYCLE,
                     "Object factory clearing cached reloadable classes.");
          cache.clear();

       }       

       public String toString(){

          return ObjectFactory.class.getName()+":"+cache.toString();

       }

    }

}



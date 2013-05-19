/**
*
* Copyright (c) 2001 - 2002 Nightfire Software, Inc. All rights reserved.
*
* This class creates and caches Evaluator instances.
*
*/

package com.nightfire.framework.rules;

import java.util.*;
import com.nightfire.framework.util.*;


public class EvaluatorPool extends FactoryCacheBase{

  /**
  * A singleton instance of the EvaluatorPool. 
  */
  private static EvaluatorPool singleton = new EvaluatorPool();

  protected EvaluatorPool(){

     super();

  }

  /**
  * This method is defined from the parent class to create new in Evaluator
  * instances.
  *
  * @param initializer This should be a String that is the name of an Evaluator
  *                    class that we want to create.
  * @return The created object.
  *
  * @throws FrameworkException if the object could not be instantiated.
  */
  protected Object createObject( Object initializer ) throws FrameworkException{

     if(initializer instanceof InitializerObject){
         InitializerObject data = (InitializerObject) initializer;
         return( ObjectFactory.create( data.getClassname(), null, null, null, data.getPaths(),
                                       data.getParentClassLoader()) );
     }

     return ObjectFactory.create( initializer.toString() );

  }


  /**
  * Gets the singleton EvaluatorPool instance.
  *
  * @return the EvaluatorPool instance.
  *
  */
  public static EvaluatorPool getInstance(){

     return singleton;

  }

  /**
  * This gets an instance of the Evaluator with the given class name.
  * The Evaluator should
  * be released using <code>releaseEvaluator</code> when it is no
  * longer needed. If a pool for the given class name does not yet
  * exist, a new one will be created.
  *
  * @param classname the name of the Evaluator class
  *
  * @return an instance of Evaluator of the given type, acquired from a pool.
  *
  * @throws ClassCastException if the class with the given name is not
  *         an instance of Evaluator.
  * @throws FrameworkException if an instance of the specified class
  *         can not be retrieved from the pool.
  */
  public Evaluator acquireEvaluator(String classname) throws FrameworkException,
                                                             ClassCastException{

     return (Evaluator) get(classname);

  }


  /**
  * This gets an instance of the Evaluator with the given class name.
  * The Evaluator should
  * be released using <code>releaseEvaluator</code> when it is no
  * longer needed. If a pool for the given class name does not yet
  * exist, a new one will be created.
  *
  * @param classname the name of the Evaluator class
  *
  * @param  path  Paths to the directoriesor archives (.jar or .zip)
  *               from which the class should be loaded.
  *
  * @return an instance of Evaluator of the given type, acquired from a pool.
  *
  * @throws ClassCastException if the class with the given name is not
  *         an instance of Evaluator.
  * @throws FrameworkException if an instance of the specified class
  *         can not be retrieved from the pool.
  */
  public Evaluator acquireEvaluator(String classname, String[] path )
                                                      throws FrameworkException,
                                                             ClassCastException{
      return acquireEvaluator(classname, path, null);
             
  }


    /**
     * This gets an instance of the Evaluator with the given class name.
     * The Evaluator should
     * be released using <code>releaseEvaluator</code> when it is no
     * longer needed. If a pool for the given class name does not yet
     * exist, a new one will be created.
     *
     * @param classname the name of the Evaluator class
     *
     * @param  path  Paths to the directoriesor archives (.jar or .zip)
     *               from which the class should be loaded.
     *
     * @param parentLoader The parent class loader used to load external classes.
     * @return an instance of Evaluator of the given type, acquired from a pool.
     *
     * @exception FrameworkException if an instance of the specified class
     *         can not be retrieved from the pool.
     * @exception ClassCastException if the class with the given name is not
     *         an instance of Evaluator.
     */
    public Evaluator acquireEvaluator(String classname, String[] path,
                                      ClassLoader parentLoader)
                                                      throws FrameworkException,
                                                             ClassCastException{
     return (Evaluator) get(classname,
                            new InitializerObject(classname, path, parentLoader) );

  }

  /**
  * Overrides the parent class so that the key (the Evaluator class name)
  * is also used as the initialization object when creating a new
  * Evaluator.
  *
  * @key the name of the Evaluator class to get.
  *
  *
  */
  public Object get ( String key ) throws FrameworkException{
     return( get( key, key ) );
  }

  /**
  * This puts an Evaluator instance back into the cache from which it
  * was acquired.
  *
  * @param evaluator the Evaluator instance to be released.
  * @param classname The Evaluator class name. This is used as a key for
  *                  putting the Evaluator back in the right cache.
  *
  * @throws FrameworkException if the evaluator can not be put back into the cache.
  */
  public void releaseEvaluator(Evaluator evaluator)
                               throws FrameworkException{

     String classname = evaluator.getClass().getName();
     put(classname, evaluator);

  }

  /**
  * This object contains an Evaluator class name and an optional
  * path from which to load the class. An instance of this object is used
  * to instantiate new Evaluator instances.
  */
  private class InitializerObject{

     /**
     * The Evaluator class name. 
     */
     private String classname;

     /**
     * An array of paths in which to look for the Evalutor class. This
     * is used when the Evaluator class needs to be reloaded. 
     */
     private String[] paths;

     private ClassLoader parentLoader = null;
      

     public InitializerObject(String evaluatorClassname){

        this(evaluatorClassname, null);

     }


     public InitializerObject(String evaluatorClassname,
                              String[] classpaths,
                              ClassLoader parentLoader){

        classname = evaluatorClassname;
        paths     = classpaths;
        this.parentLoader = parentLoader;
        

     }

     public InitializerObject(String evaluatorClassname,
                              String[] classpaths){

         this(evaluatorClassname, classpaths, null);
      
     }

     public String getClassname(){

        return classname;

     }

     public String[] getPaths(){

        return paths;

     }

     public ClassLoader getParentClassLoader()
     {
         return parentLoader;
     }
      
  }

}
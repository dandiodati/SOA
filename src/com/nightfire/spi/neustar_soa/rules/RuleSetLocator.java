////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.nightfire.framework.cache.*;
import com.nightfire.framework.rules.*;
import com.nightfire.framework.util.*;

/**
 * This class is used to dynamically locate RuleSetEvaluator implementations
 * in a given classpath.
 */
public abstract class RuleSetLocator {

   /**
    * The extension used to identify class files.
    */
   public static final String CLASS_SUFFIX = ".class";

   /**
    * The file filter used in looking for custom rule set evaluator classes.
    */
    protected static ClassAndDirectoryFilter ruleSetFilter =
                                                new ClassAndDirectoryFilter();

   /**
    * This maps a classpath to the names of the rule set evaluator
    * classes found in that classpath.
    */
    protected static EvaluatorNameCache evaluatorNameCache =
                                          new EvaluatorNameCache();

   /**
    * Used to cached the elements of a classpath. The
    * key for this table is the entire classpath string, and the
    * contents of the table are string arrays containing the tokenized
    * elements of that classpath.
    */
    private static Hashtable tokenizedClasspaths = new Hashtable();


    /**
     * The gets the class names for the rule set evaluator classes
     * that can be found in the given classpath.
     */
    public static List getRuleSetEvaluators(String classpath){
        return getRuleSetEvaluators(classpath, null);
    }
    


    /**
     * The gets the class names for the rule set evaluator classes
     * that can be found in the given classpath.
     */
    public static List getRuleSetEvaluators(String classpath, ClassLoader parentLoader){

        // check to see if we have already discovered and cached the
        // evaluator class names in the given classpath
        List names = evaluatorNameCache.get( classpath );

        if( names != null ){

          if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

                Debug.log(Debug.MSG_STATUS,
                          "The following list of cached rule set evaluators "+
                          names+
                          " will be used for classpath ["+
                          classpath+"]");

          }

          return names;

        }

        // create a list to hold the results
        names = new LinkedList();

        String[] classpathElements = getClasspathTokens(classpath);
        int count = classpathElements.length;

        for(int i = 0; i < count; i++){

           File file = new File( classpathElements[i] );

           if( ! file.exists() ){

              Debug.warning( "Classpath element ["+classpathElements[i]+
                             "] does not exist.");

           }
           else if ( file.isDirectory() ){

              File[] files = file.listFiles( ruleSetFilter );

              // for all child directories, recursively find
              // any RuleSetEvalutator implementations
              for (int k = 0; k < files.length; k++ ) {

                   try{

                      getRuleSetEvaluators(files[k],
                                           "",
                                           names,
                                           classpathElements,
                                           parentLoader);

                   }
                   catch( FrameworkException fex ) {

                       Debug.error("Unable to initialize rule set classes "+
                                   "in directory ["+
                                   files[k].getPath() + "]: " +
                                   fex.getMessage() );

                   }

              }

           }
           else if( file.getName().toLowerCase().endsWith(".jar") ||
                    file.getName().toLowerCase().endsWith(".zip") ) {

               try
               {
                   getRuleSetEvaluators(file, names, classpathElements, parentLoader);
               }
               catch( FrameworkException fex ) {

                   Debug.error("Unable to initialize rule set classes "+
                               "in archive ["+file.getPath()+ "]: " +
                               fex.getMessage() );

               }

           }

        }

        evaluatorNameCache.add(classpath, names);

        return names;

    }

   /**
    * This tokenizes the given classpath string and returns its
    * elements as a string array.
    *
    * @param classpath String
    * @return String[]
    */
    public static String[] getClasspathTokens(String classpath){

       String[] result = (String[]) tokenizedClasspaths.get( classpath );

       if( result == null ){

           StringTokenizer tokenizer =
                   new StringTokenizer(classpath, File.pathSeparator);

           int tokenCount = tokenizer.countTokens();

           result = new String[tokenCount];

           for(int i = 0; i < tokenCount; i++){

              result[i] = tokenizer.nextToken();

           }

           tokenizedClasspaths.put( classpath, result );

       }

       return result;

    }



    

    /**
     * This recurses the current file and its subdirectories (if any)
     * looking for class files that extend the RuleSetEvaluatorBase class.
     *
     * @param current the current file being checked.
     * @param path the current path used in creating the package for a class.
     *        (e.g. "rules.Test").
     * @param evaluators the list to which evaluator class names will be added as
     *                   they are found.
     * @param cp a <code>String[]</code> value
     * @param parentLoader The parent class loader used to load external classes.
     * @exception FrameworkException if an error occurs
     */
    protected static void getRuleSetEvaluators(File current,
                                               String path,
                                               List evaluators,
                                               String[] cp,
                                               ClassLoader parentLoader)
                                               throws FrameworkException {

        String name = current.getName();

        if( current.isDirectory() ) {

            File[] files = current.listFiles( ruleSetFilter );

            if( StringUtils.hasValue(path) ){
                path += ".";
            }

            path += name;

            for(int i = 0; i < files.length; i++){
                getRuleSetEvaluators(files[i], path, evaluators, cp, parentLoader);
            }

        }
        // we won't directly load inner classes (classes containing a $ in
        // their name)
        else if(name.endsWith(CLASS_SUFFIX) && ( name.lastIndexOf("$") == -1 ) ) {

            name = name.substring( 0, name.length() - CLASS_SUFFIX.length() );

            String evaluatorName = path + "." + name;

            // load the named class from the given classpath
            Class evaluatorClass = ObjectFactory.loadClass( evaluatorName, cp, parentLoader );

            // but only add classes that are not abstract and
            // extend RuleSetEvaluatorBase
            if(  !isAbstract(evaluatorClass) &&
                 RuleSetEvaluatorBase.class.isAssignableFrom( evaluatorClass ) ) {

                evaluators.add(evaluatorName);

            }

        }

    }

    /**
     * This looks for rule set evaluators in the given jar file
     * and adds any instances it finds to the list
     * of evaluators.
     *
     * @param jarFile File a reference to the jar file to be searched.
     * @param evaluators List the list where the results of the
     *                        search will be added.
     * @param cp String[] the classpath to use when loading the classes.
     *
     * @throws FrameworkException if an error occurs while trying to
     *                            read the archive or while trying
     *                            to load the classes.
     */
    protected static void getRuleSetEvaluators(File jarFile,
                                               List evaluators,
                                               String[] cp,
                                               ClassLoader parentLoader)
                                               throws FrameworkException{

         try{

            ZipFile jar = new ZipFile(jarFile);

            Enumeration entries = jar.entries();

            while (entries.hasMoreElements()) {

               ZipEntry next = (ZipEntry) entries.nextElement();

               String name = next.getName();

               // replace the file separators with dots for package names
               name = name.replace('/', '.');
               name = name.replace('\\', '.');

               getRuleSetEvaluator(name,
                                   evaluators,
                                   cp, 
                                   parentLoader);

            }

         }
         catch(FrameworkException fex){

            // just rethrow
            throw fex;

         }
         catch(Exception ex){

            throw new FrameworkException("Could not access file ["+
                                         jarFile.getAbsolutePath()+
                                         "]: "+ex);

         }

    }

    /**
     * This method loads the named evaluator class from the given classpath
     * if the named class is a rules set evaluator and is not abstract,
     * then it will get added to the given list of evaluators.
     *
     * @param name String the name of the class to load.
     * @param evaluators List the list to which any rule set
     *                             class names will be added.
     * @param cp String[] the classpath that will be searched for rule
     *                    set evaluators.
     * @throws FrameworkException if a class cannot be loaded.
     */
    protected static void getRuleSetEvaluator(String name,
                                              List evaluators,
                                              String[] cp,
                                              ClassLoader parentLoader)
                                              throws FrameworkException{

      if( name.endsWith(CLASS_SUFFIX) && ( name.lastIndexOf("$") == -1 ) ) {

        String evaluatorName = name.substring(0, name.length() -
                                              CLASS_SUFFIX.length());

        // load the named class from the given classpath
        Class evaluatorClass = ObjectFactory.loadClass(evaluatorName, cp, parentLoader);

        // but only add classes that are not abstract and
        // extend RuleSetEvaluatorBase
        if( !isAbstract(evaluatorClass) &&
            RuleSetEvaluatorBase.class.isAssignableFrom(evaluatorClass)) {

           // No need to add the same evaluator twice. Even if it occurs
           // twice in the classpath, it should only be executed once.
           if( ! evaluators.contains( evaluatorName ) ){

              evaluators.add(evaluatorName);

           }

        }

      }

    }


    /**
     * Convenience method for checking to see if a class is abstract.
     *
     * @param c Class the class to be checked.
     * @return boolean whether it is abstract.
     */
    public static boolean isAbstract(Class c){

       return java.lang.reflect.Modifier.isAbstract( c.getModifiers() );

    }


   /**
    * This filter accepts only directories and class files.
    */
    protected static class ClassAndDirectoryFilter implements FileFilter{

     /**
      * Tests whether or not the specified file is a directory or a
      * rule set evaluator class file.
      *
      * @return  <code>true</code> if and only if <code>file</code>
      *          should be included
      */
        public boolean accept(File file){

            String name = file.getName();
            return file.isDirectory() || name.endsWith(CLASS_SUFFIX);

        }

    }

    /**
     * This is used to cache the names of the rule set evaluator classes
     * found in a particular classpath.
     */
    private static class EvaluatorNameCache implements CachingObject{

       private Hashtable cache = new Hashtable();

       public EvaluatorNameCache(){

          CacheManager.getRegistrar().register( this );

       }

       public void add(String classpath,
                       List evaluatorClassNames){

          cache.put(classpath,
                    evaluatorClassNames);

       }

       public List get(String classpath){

          return (List) cache.get(classpath);

       }

       public void flushCache(){

          cache.clear();

       }

    }

}

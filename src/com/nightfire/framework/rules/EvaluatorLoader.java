package com.nightfire.framework.rules;

import java.net.*;
import java.io.*;

import com.nightfire.framework.util.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class is used to reload Evaluator classes without having to
* restart the JVM. 
*/
public class EvaluatorLoader{


    /**
    * This reloads the Evaluator class with the given name from the given
    * file path. 
    * The catch here is that the given path should <b>NOT</b> be in the
    * CLASSPATH. If the class is in the CLASSPATH, the default Java ClassLoader
    * will be used to load the class and return it's cached instance.
    *
    * @param name The name of the Evaluator implementation class to load.
    * @param path The path to the directory or archive (.jar or .zip)
    *             from which the Evaluator
    *             implementation class should
    *             be loaded.
    *
    * @throws FrameworkException If the named class cannot be found in the
    *                            given path, or if the named class is not
    *                            an Evaluator implementation, or if there
    *                            is a error while trying to instantiate the
    *                            found class.
    * 
    * @return A new instance of the named Evaluator class.
    */
    public static Evaluator reload(String name, String path)
                                   throws FrameworkException {
        
        return reload(name, path, null);
    }
    
    /**
     * This reloads the Evaluator class with the given name from the given
     * file path. 
     * The catch here is that the given path should <b>NOT</b> be in the
     * CLASSPATH. If the class is in the CLASSPATH, the default Java ClassLoader
     * will be used to load the class and return it's cached instance.
     *
     * @param name The name of the Evaluator implementation class to load.
     * @param path The path to the directory or archive (.jar or .zip)
     *             from which the Evaluator
     *             implementation class should
     *             be loaded.
     *
     * @param parentLoader The parent classloader to use for loading classes outside of the current loader.
     *                     If null then the standard classloader is used.
     * @return A new instance of the named Evaluator class.
     * @exception FrameworkException If the named class cannot be found in the
     *                            given path, or if the named class is not
     *                            an Evaluator implementation, or if there
     *                            is a error while trying to instantiate the
     *                            found class.
     * 
     */
    public static Evaluator reload(String name, String path, ClassLoader parentLoader)
                                   throws FrameworkException {

        if(!(path.endsWith(".jar") ||
             path.endsWith(".zip") ||
             path.endsWith(File.separator))){

               path += File.separator;

        }

        path = "file:"+path;

        Debug.log(Debug.IO_STATUS, "Reloading class ["+name+
                                   "] from URL ["+path+"]");

        URL[] urlPath;
        try{
           urlPath = new URL[]{new URL(path)};
        }
        catch(MalformedURLException malx){
           throw new FrameworkException("Could not locate class ["+name+
                                        "]. ["+path+
                                        "] is not a valid classpath: "+
                                        malx.getMessage() );
        }

        URLClassLoader loader;

        if (parentLoader == null)
            loader = new URLClassLoader(urlPath);
        else
            loader = new URLClassLoader(urlPath, parentLoader);

        Evaluator result = null;
        Class loaded;
        
        try{
           loaded = loader.loadClass(name);
        }
        catch(ClassCastException ccex){
           throw new FrameworkException(name+
                                        " is not an instance of Evaluator");
        }
        catch(ClassNotFoundException cnfex){
           throw new FrameworkException("Could not find class ["+name+
                                        "] in path ["+path+"]");
        }


        try{
          result = (Evaluator) loaded.newInstance();
        }
        catch(Exception ex){
           throw new FrameworkException("Could not create instance of class ["+
                                        loaded+
                                        "]: "+
                                        ex.getMessage() );
        }


        return result;

    }

    public static void main(String[] args){

       String usage = "java "+EvaluatorLoader.class.getName()+" <Evaluator Class Name> <class directory>";

       if(args.length != 2){

          System.err.println(usage);
          System.exit(-1);

       }

       try{

          BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  
          Evaluator instance;
          String line;

          do{

             instance = reload(args[0], args[1]);
             System.out.println("Loaded instance: "+instance);
             System.out.print("Enter 'q' to quit or any other key to reload Evaluator:");
             line = br.readLine();

          }while(!line.startsWith("q"));

       }
       catch(Exception ex){
          ex.printStackTrace();
       }

    }

}
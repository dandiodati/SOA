package com.nightfire.framework.util;

import java.io.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* A wrapper around Sun's Java SDK compiler (javac). This class can be used to
* compile java source files at runtime. 
*/
public class JavaCompiler {

   /**
   * The name of the CLASSPATH System property.
   */
   private static String CLASSPATH_PROPERTY = "java.class.path";

   /**
   * Compiles the java file with the given file name.
   *
   * @param fileName the name of the .java source file to be compiled.
   * @param outputDirectory
   *        the directory where the compiled class files will be placed.
   *        This directory will be created if it doesn't currently exist.
   * @param output the output (error messages) from the compiler will be
   *               appended to this buffer.
   *
   * @return true if the file compiled successfully, false otherwise.
   *
   * @exception FrameworkException Thrown if the compiler class <code>
   *            sun.tools.javac.Main</code> cannot be found.
   * @exception FileNotFoundException Thrown if the given source file can not
   *                                  be found.
   */
   public static boolean compile(String fileName,
                                 String outputDirectory,
                                 StringBuffer output)
                                 throws FrameworkException,
                                        FileNotFoundException{

      File source = new File(fileName);
      if( !source.exists() ){
         throw new FileNotFoundException("The source file ["+fileName+
                                         "] could not be found.");
      }

      // Make sure the output directory exists, creating it if neccessary
      checkDirectory(outputDirectory);

      // Get the classpath including our output directory
      String classpath = getClasspathWith(outputDirectory);

      String[] args = {"-classpath", classpath, "-d", outputDirectory, fileName};

      return compile(args, output);

   }

   /**
   * Runs the javac compiler with the given arguments.
   *
   * @param args the command-line arguments for the SDK's javac compiler.
   * @param output the output (error messages) from the compiler will be
   *               appended to this buffer.
   *
   * @return true if compilation was successfully, false otherwise.
   *
   * @exception FrameworkException Thrown if the compiler class <code>
   *            sun.tools.javac.Main</code> cannot be found.
   */
   public static boolean compile(String[] args, StringBuffer output)
                                 throws FrameworkException{

      boolean success = false;

      OutputStream out = new StringBufferOutputStream(output);

      try{

         sun.tools.javac.Main compiler = new sun.tools.javac.Main(out, "javac");
         success = compiler.compile(args);

      }
      catch(NoClassDefFoundError err){

         throw new FrameworkException("The Sun compiler class could not be found. Please confirm that the Java SDK's tools.jar archive is in your CLASSPATH.");

      }

      return success;

   }

   /**
   * An OutputStream that writes its output to a StringBuffer.
   */
   static class StringBufferOutputStream extends OutputStream{

         StringBuffer buffer;

         StringBufferOutputStream(StringBuffer buffer){

            super();
            this.buffer = buffer;

         }

         public void write(int b)
                    throws IOException{

            byte[] bytes = new byte[1];
            bytes[0] = (byte)b;
            buffer.append(new String(bytes));

         }

   }

   /**
   * Gets the current CLASSPATH and prepends the given path to it.
   */
   private static String getClasspathWith(String path){


      String classpath = System.getProperty(CLASSPATH_PROPERTY);

      // Make sure to include our output directory into the javac classpath
      classpath = path + File.pathSeparator + classpath;


      return classpath;

   }

   /**
   * Checks to see if the named directory exists, if not, then it
   * attempts to create it.
   *
   * @param pathName the directory to check or create.
   *
   * @exception FrameworkException thrown if the given path was not found
   *            and could not be created.
   */
   public static void checkDirectory(String pathName) throws FrameworkException{

      try{

          File dir = new File(pathName);

          if(! dir.exists() ){

             Debug.log(Debug.IO_STATUS, "Creating path ["+pathName+"]");
             dir.mkdirs();

          }

      }
      catch(Exception ex){

         throw new FrameworkException("Could not access and create directory ["+
                                      pathName+"]: "+
                                      ex );

      }

   }   

   /**
   * Compiles a named file and places the output in the given directory.
   */
   public static void main(String[] args){

      String usage = "usage: java "+JavaCompiler.class.getName()+" <java file name> <output directory>"; 

      if(args.length != 2){

         System.err.println(usage);
         System.exit(-1);

      }

      StringBuffer output = new StringBuffer();

      System.out.print("Result: ");

      try{

          if( compile(args[0], args[1], output) ){

             System.out.println("success");

          }
          else{

             System.out.println("failure\nReasons:");
             System.out.println(output);

          }

      }
      catch(Exception ex){

         System.err.println("error\nReasons:"+ex);

      }

   }

} 
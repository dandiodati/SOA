package com.nightfire.framework.rules;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.cache.*;

/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* The RuleEngine provides functionality for locating and executing
* implementations of the <code>com.nightfire.framework.rules.Evaluator</code>
* interface. These implementations usually enforce a rule or set of rules,
* and they are used to evaluate a given input message to see if it
* is valid or not.
*/
public class RuleEngine {

  /**
  * The usage message for this class's command-line interface.
  */
  private static String usage =
           "Usage:\njava "+RuleEngine.class.getName()+
           " [-verbose] [-reload <reload classpath>] [-repeat <count>] <Evaluator class name> <input file>\n\n"+
           "\t-verbose   turn verbose logging on\n"+
           "\t-reload    The Rule Engine will loop to test class reloading.\n"+
           "\t           Classes will be reloaded from the give \"reload classpath\".\n"+
           "\t-repeat    The number of times that the evaluation should be excuted.\n"+
           "\t           This is useful for performance testing.\n\n"+
           "\tnotes:     This evaluates the contents of the given input file using\n"+
           "\t           the Evaluator with the given class name. The given \n"+
           "\t           Evaluator class must implement the\n"+
           "\t           "+Evaluator.class.getName()+"\n"+
           "\t           interface and must be in the current CLASSPATH or \n"+
           "\t           the given reload classpath.\n";


  /**
  * Creates or acquires an instance of an Evaluator as defined by the given
  * <code>definition</code> and
  * uses this instance to evaluate the validity of the given source
  * message.
  *
  * @param definition An implementation of EvaluatorDefinition that will provide
  *                   the class name of the Evaluator that should
  *                   be instantiated and used to evaluate the given
  *                   <code>source</code> message.
  * @param source The input message that will be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  * @throws FrameworkException if an Evaluator with the defined class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
  public static boolean evaluate(EvaluatorDefinition definition,
                                 String source,
                                 ErrorCollection errors)
                                 throws FrameworkException{

     return evaluate( definition.getFullEvaluatorClassName(), source, errors, null );

  }

  /**
  * Creates or acquires an instance of an Evaluator as defined by the given
  * <code>definition</code> and
  * uses this instance to evaluate the validity of the given source
  * message.
  *
  * @param definition An implementation of EvaluatorDefinition that will provide
  *                   the class name of the Evaluator that should
  *                   be instantiated and used to evaluate the given
  *                   <code>source</code> message.
  * @param source The input message that will be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  * @param reloadPath
  *               If this param is not null, it indicates the path from which
  *               the named Class should be reloaded.
  *               The only time the class would need to be reloaded is
  *               when it has been recompiled since this virtual machine
  *               started, and the latest version of the class is desired.
  *
  * @throws FrameworkException if an Evaluator with the defined class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
  public static boolean evaluate(EvaluatorDefinition definition,
                                 String source,
                                 ErrorCollection errors,
                                 String reloadPath)
                                 throws FrameworkException{

     return evaluate( definition.getFullEvaluatorClassName(), source, errors, reloadPath );

  }

  /**
  * Creates or acquires an instance of an Evaluator with the given class name and
  * uses this instance to evaluate the validity of the given source
  * message.
  *
  * @param evaluatorClassName The name of the Evaluator class that should
  *                           be instantiated and used to evaluate the given
  *                           <code>source</code> message.
  * @param source The input message that will be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  *
  * @throws FrameworkException if an Evaluator with the given class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
  public static boolean evaluate(String evaluatorClassName,
                                 String source,
                                 ErrorCollection errors)
                                 throws FrameworkException{

     return evaluate(evaluatorClassName, source, errors, null);

  }

  /**
  * Creates or acquires an instance of an Evaluator with the given class name and
  * uses this instance to evaluate the validity of the given source
  * message.
  *
  * @param evaluatorClassName The name of the Evaluator class that should
  *                           be instantiated and used to evaluate the given
  *                           <code>source</code> message.
  * @param source The input message that will be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  * @param reloadPath
  *               If this param is not null, it indicates the path from which
  *               the named Class should be reloaded.
  *               The only time the class would need to be reloaded is
  *               when it has been recompiled since this virtual machine
  *               started, and the latest version of the class is desired.
  *
  * @throws FrameworkException if an Evaluator with the given class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
   public static boolean evaluate(String evaluatorClassName,
                                 String source,
                                 ErrorCollection errors,
                                 String reloadPath)
                                 throws FrameworkException{

     return evaluate(evaluatorClassName, source, errors, reloadPath, null);

  }

  /**
  * Creates or acquires an instance of an Evaluator with the given class name and
  * uses this instance to evaluate the validity of the given source
  * message.
  *
  * @param evaluatorClassName The name of the Evaluator class that should
  *                           be instantiated and used to evaluate the given
  *                           <code>source</code> message.
  * @param source The input message that will be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  * @param reloadPath
  *               If this param is not null, it indicates the path from which
  *               the named Class should be reloaded.
  *               The only time the class would need to be reloaded is
  *               when it has been recompiled since this virtual machine
  *               started, and the latest version of the class is desired.
  * @param ruleContext  RuleContext object shared by the rules
  *
  * @throws FrameworkException if an Evaluator with the given class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
   public static boolean evaluate(String evaluatorClassName,
                                 String source,
                                 ErrorCollection errors,
                                 String reloadPath,
								 RuleContext ruleContext)
                                 throws FrameworkException{

     boolean success = false;

     Evaluator evaluator = null;

     boolean reload = StringUtils.hasValue(reloadPath);

     if(reload){

        // Reload the class because it may have been recompiled
        evaluator = EvaluatorLoader.reload(evaluatorClassName, reloadPath);

     }
     else{
        // Don't reload, just get our Evaluator from the pool
        evaluator = EvaluatorPool.getInstance().acquireEvaluator(evaluatorClassName);
     }

     try{
        success = evaluator.evaluate(source, errors, ruleContext);
     }
     catch(Exception ex){

        // Catch any exception that the evaluator failed to handle
        RuleError error = new RuleError(null, "", RuleUtils.getExceptionMessage(ex));
        errors.addError(error);

     }
     finally{
        // Return the Evaluator instance back to its pool
        if(!reload){
           EvaluatorPool.getInstance().releaseEvaluator(evaluator);
        }

     }

     return success;

   }


 /**
  * Creates or acquires an instance of an Evaluator with the given class name and
  * uses this instance to evaluate the validity of the given source
  * message.  This should be used by any resources that requires Evaluators to be
  * loaded/reloaded into the current JVM upon cache flushing.
  *
  * @param evaluatorClassName The name of the Evaluator class that should
  *                           be instantiated and used to evaluate the given
  *                           <code>source</code> message.
  * @param source The input rmessage that will be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  * @param  classPath Path to the directory or archive (.jar or .zip)
  *                   from which the class should be loaded.
  *
  *
  * @throws FrameworkException if an Evaluator with the given class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
  public static boolean evaluateWithClassReLoading(String evaluatorClassName,
                                 String source,
                                 ErrorCollection errors,
                                 String classPath)
                                 throws FrameworkException{

     boolean success = false;

     Evaluator evaluator = null;

     boolean reload = StringUtils.hasValue(classPath);

     if(reload){

        // Use to create object instances in EvaluatorPool.
       String[] cp = { classPath };
       evaluator = EvaluatorPool.getInstance().acquireEvaluator(evaluatorClassName, cp );

     }
     else{
        // Don't allow
        throw new FrameworkException("Evaluator class ["+evaluatorClassName+
                                        "] could not be loaded, no path "
                                        + "given.");
     }

     try{
        success = evaluator.evaluate(source, errors);
     }
     catch(Exception ex){

        // Catch any exception that the evaluator failed to handle
        RuleError error = new RuleError(null, "", RuleUtils.getExceptionMessage(ex));
        errors.addError(error);

     }
     finally{
        EvaluatorPool.getInstance().releaseEvaluator(evaluator);
     }

     return success;

  }

  /**
  *
  * Acquires an instance of an Evaluator with the given class name and
  * uses this instance to evaluate the validity of the given, parsed source
  * message.
  *
  * This version of the <code>evaluate</code> method is provided for
  * optimization purposes. If it is
  * known that the input is XML and that all of the Evaluators
  * being used are XMLEvaluators, then it makes sense to just
  * pre-parse the XML once and pass it as a parameter instead
  * of parsing it repeatedly for each Evaluator executed.
  *
  * @param evaluatorClassName The name of the Evaluator class that should
  *                           be instantiated and used to evaluate the given
  *                           <code>source</code> message.
  * @param parsedSource A parsed XML input message to be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  *
  * @throws FrameworkException if an Evaluator with the given class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
  public static boolean evaluate(String evaluatorClassName,
                                 XPathAccessor parsedSource,
                                 ErrorCollection errors) throws FrameworkException{

     return ( evaluate( evaluatorClassName, parsedSource, errors, null ) );
  }


 /**
  *
  * Acquires an instance of an Evaluator with the given class name and
  * uses this instance to evaluate the validity of the given, parsed source
  * message.
  *
  * This version of the <code>evaluate</code> method is provided for
  * optimization purposes. If it is
  * known that the input is XML and that all of the Evaluators
  * being used are XMLEvaluators, then it makes sense to just
  * pre-parse the XML once and pass it as a parameter instead
  * of parsing it repeatedly for each Evaluator executed.
  *
  * @param evaluatorClassName The name of the Evaluator class that should
  *                           be instantiated and used to evaluate the given
  *                           <code>source</code> message.
  * @param parsedSource A parsed XML input message to be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  * @param path Paths to the directory or archive (.jar or .zip)
  *                   from which the class should be loaded.
  *
  * @throws FrameworkException if an Evaluator with the given class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
  public static boolean evaluate(String evaluatorClassName,
                                 XPathAccessor parsedSource,
                                 ErrorCollection errors, String[] path) throws FrameworkException{
      return evaluate(evaluatorClassName, parsedSource, errors, path, (RuleContext)null);
  }
    

 /**
  *
  * Acquires an instance of an Evaluator with the given class name and
  * uses this instance to evaluate the validity of the given, parsed source
  * message.
  *
  * This version of the <code>evaluate</code> method is provided for
  * optimization purposes. If it is
  * known that the input is XML and that all of the Evaluators
  * being used are XMLEvaluators, then it makes sense to just
  * pre-parse the XML once and pass it as a parameter instead
  * of parsing it repeatedly for each Evaluator executed.
  *
  * @param evaluatorClassName The name of the Evaluator class that should
  *                           be instantiated and used to evaluate the given
  *                           <code>source</code> message.
  * @param parsedSource A parsed XML input message to be evaluated.
  * @param errors Any RuleErrors that occur while executing the Evaluator
  *               will be added to this accumulating parameter.
  * @param path Paths to the directory or archive (.jar or .zip)
  *                   from which the class should be loaded.
  *
  * @throws FrameworkException if an Evaluator with the given class name
  *                            could not be found, or if it was found and
  *                            it was not an instance of Evaluator.
  *
  * @return true if the Evaluator found the input message to be valid, false
  *         otherwise.
  */
  public static boolean evaluate(String evaluatorClassName,
                                 XPathAccessor parsedSource,
                                 ErrorCollection errors, String[] path, RuleContext ruleContext) throws FrameworkException{
      return evaluate(evaluatorClassName, parsedSource, errors, path, null, ruleContext);
  }

    /**
     * Acquires an instance of an Evaluator with the given class name and
     * uses this instance to evaluate the validity of the given, parsed source
     * message.
     *
     * This version of the <code>evaluate</code> method is provided for
     * optimization purposes. If it is
     * known that the input is XML and that all of the Evaluators
     * being used are XMLEvaluators, then it makes sense to just
     * pre-parse the XML once and pass it as a parameter instead
     * of parsing it repeatedly for each Evaluator executed.
     *
     * @param evaluatorClassName The name of the Evaluator class that should
     *                           be instantiated and used to evaluate the given
     *                           <code>source</code> message.
     * @param parsedSource A parsed XML input message to be evaluated.
     * @param errors Any RuleErrors that occur while executing the Evaluator
     *               will be added to this accumulating parameter.
     * @param path a <code>String[]</code> value
     * @param parentLoader A parent loader to load external classes.
     * @return true if the Evaluator found the input message to be valid, false
     *         otherwise.
     * @exception FrameworkException if an Evaluator with the given class name
     *                            could not be found, or if it was found and
     *                            it was not an instance of Evaluator.
     *
     */
    public static boolean evaluate(String evaluatorClassName,
                                 XPathAccessor parsedSource,
                                 ErrorCollection errors, String[] path,
                                 ClassLoader parentLoader) throws FrameworkException{


     return evaluate(evaluatorClassName, parsedSource, errors, path, parentLoader, null);

  }

    /**
     * Acquires an instance of an Evaluator with the given class name and
     * uses this instance to evaluate the validity of the given, parsed source
     * message.
     *
     * This version of the <code>evaluate</code> method is provided for
     * optimization purposes. If it is
     * known that the input is XML and that all of the Evaluators
     * being used are XMLEvaluators, then it makes sense to just
     * pre-parse the XML once and pass it as a parameter instead
     * of parsing it repeatedly for each Evaluator executed.
     *
     * @param evaluatorClassName The name of the Evaluator class that should
     *                           be instantiated and used to evaluate the given
     *                           <code>source</code> message.
     * @param parsedSource A parsed XML input message to be evaluated.
     * @param errors Any RuleErrors that occur while executing the Evaluator
     *               will be added to this accumulating parameter.
     * @param path a <code>String[]</code> value
     * @param parentLoader A parent loader to load external classes.
     * @return true if the Evaluator found the input message to be valid, false
     *         otherwise.
     * @exception FrameworkException if an Evaluator with the given class name
     *                            could not be found, or if it was found and
     *                            it was not an instance of Evaluator.
     *
     */
    public static boolean evaluate(String evaluatorClassName,
                                 XPathAccessor parsedSource,
                                 ErrorCollection errors, String[] path,
                                 ClassLoader parentLoader, RuleContext ruleContext) throws FrameworkException{

     boolean success = false;
     Evaluator evaluator = null;

     if ( path == null ) {
        // This is optimized, so don't reload, just get our Evaluator from the pool
        evaluator = EvaluatorPool.getInstance().acquireEvaluator(evaluatorClassName);
     }
     else {
        // This is optimized, so don't reload, just get our Evaluator
        // from class reloading pool.
         evaluator = EvaluatorPool.getInstance().acquireEvaluator(evaluatorClassName, path, parentLoader);
     }

     try{

        if(evaluator instanceof XMLEvaluator){

           success = ((XMLEvaluator) evaluator).evaluate(parsedSource, errors, ruleContext);

        }
        else{

           throw new FrameworkException("Evaluator class ["+evaluatorClassName+
                                        "] is not an instance of XMLEvaluator.");

        }

     }
     catch(Exception ex){

        // Catch any exception that the evaluator failed to handle
        RuleError error = new RuleError(null, "", RuleUtils.getExceptionMessage(ex));
        errors.addError(error);

     }
     finally{
        // Return the Evaluator instance back to its pool
        EvaluatorPool.getInstance().releaseEvaluator(evaluator);
     }

     return success;

  }


  /**
  * This takes the name of an Evaluator class and the name of an input file,
  * and evaluates the contents of the file using the named evaluator.
  * Any RuleErrors that occur will get logged to the console.
  * The process will exit with a code of 0 if evaluation was successful,
  * and -1 if it fails.
  *
  */
  public static void main(String[] args){

      int result = RuleConstants.SUCCESS;

      boolean verbose = false;

      // whether or not to loop and reload rule evaluator classes
      boolean reload  = false;

      String evaluatorClass  = null;
      String evalInputFile   = null;
      String reloadPath = null;

      // The number of times to repeat the execution.
      // By default, only perform the evaluation once.
      int count = 1;

      for(int i = 0; i < args.length; i++){

         // Check for options
         if( args[i].startsWith("-") ){

           if(args[i].equals("-verbose")){
              verbose = true;
           }
           else if(args[i].equals("-reload")){

              reload = true;
              try{
                 reloadPath = args[++i];
              }
              catch(IndexOutOfBoundsException ioob){
                 exit("The -reload option requires a reload path argument.");
              }

              System.out.println("Reload path: ["+reloadPath+"]");

           }
           else if(args[i].equals("-repeat")){

              String repeatArg = null;

              try{
                 repeatArg = args[++i];
                 count = Integer.parseInt(repeatArg);
              }
              catch(IndexOutOfBoundsException oobex){
                 exit("The -repeat option requires count argument.");
              }
              catch(NumberFormatException nfex){
                 exit("The -repeat option requires a numeric count argument. ["+
                      repeatArg+"] is not a valid integer value.");
              }

           }
           else{
              // Someone is trying some unknown option or is asking for help
              // using -? or the like
              exit("");
           }

         }
         else{

            if(evaluatorClass == null){
               evaluatorClass = args[i];
               System.out.println("Evaluator:  ["+evaluatorClass+"]");
            }
            else if(evalInputFile == null){
               evalInputFile = args[i];
               System.out.println("Input file: ["+evalInputFile+"]");
            }
            else exit("Unexpected command-line parameter: "+args[i]);

         }

      }

      if(evaluatorClass == null){
         exit("No Evaluator class name was specified.");
      }

      // Check to make sure we were given at least one input file to work with
      if(evalInputFile == null){
         exit("No input file was specified.");
      }

      if(verbose) Debug.enableAll();
      // More refined debugging can be managed by setting the DEBUG_LOG_LEVELS
      // property in the environment
      else Debug.configureFromProperties( System.getProperties() );

      try{

          Properties dbProps = FileUtils.loadProperties( null, "db.properties" );


          DBInterface.initialize( dbProps.getProperty( "DBNAME" ), dbProps.getProperty( "DBUSER" ), 
                                  Crypt.decrypt( dbProps.getProperty( "DBPASSWORD" ) ) );

         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         String xml = FileUtils.readFile(evalInputFile);
         ErrorCollection errors;
         String line = "";

         do{

             int totalTime = 0;

             for(int i = 0; i < count; i++){

                if(i > 0){
                   System.out.println("Beginning evaluation number: "+
                                      (i+1));
                }

                long start = System.currentTimeMillis();
                boolean success = false;

                errors = new ErrorCollection();

                if ( StringUtils.hasValue(reloadPath) )
                   success = RuleEngine.evaluateWithClassReLoading(evaluatorClass, xml, errors, reloadPath);
                else
                    success = RuleEngine.evaluate(evaluatorClass, xml, errors);

                long stop  = System.currentTimeMillis();
                long delta = stop-start;
                System.out.println("Evaluation time: "+delta+" ms");
                totalTime += delta;
                if(i > 0){
                   System.out.println("Average evaluation time: "+
                                     (totalTime/(i+1))+" ms");
                }

                if( success ){
                   System.out.println("Evaluation completed successfully.\n");
                }
                else{

                    // Debug.log( Debug.NORMAL_STATUS, "Evaluation failed:\n\n"+errors);
                    System.out.println("Evaluation failed:\n\n"+errors);
                   result = RuleConstants.FAILED;

                }

             }

             if(reload){

                System.out.println("\nPlease enter \"r\" to reload the rule classes and repeat the test,\nor enter any other key to exit:");
                line = br.readLine();

                if ( line.equalsIgnoreCase("r") ) {
                       CacheClearingManager manager = CacheManager.getManager();
                       manager.flushCache();
                }

             }

          } while(reload && line.equalsIgnoreCase("r"));

         CacheManager.getManager().flushCache( );
      }
      catch(FrameworkException fex){

         System.err.println("ERROR: "+fex.getMessage());
         result = RuleConstants.FAILED;

      }
      catch(Exception ex){

         ex.printStackTrace();
         result = RuleConstants.FAILED;

      }
      finally
      {
          try
          {
              DBInterface.closeConnection( );
          }
          catch( Exception e )
          {
              Debug.error( e.toString() );
          }
      }

      System.exit(result);

  }

  /**
  * Utility method to print usage and an error message and then exit.
  */
  private static void exit(String errorMessage){

     System.err.println(usage);
     System.err.println();
     System.err.println(errorMessage);
     System.exit(RuleConstants.FAILED);

  }

}

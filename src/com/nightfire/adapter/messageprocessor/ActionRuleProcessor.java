////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001 - 2004 Nightfire Software, Inc. All rights reserved.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.adapter.messageprocessor;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.util.xml.CachingXPathAccessor;
import com.nightfire.framework.message.util.xml.XPathAccessor;
import com.nightfire.framework.rules.ErrorCollection;
import com.nightfire.framework.rules.RuleEngine;
import com.nightfire.framework.rules.RuleSetEvaluatorBase;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.ObjectFactory;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.HeaderNodeNames;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessingDriver;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * This processor enforces a rule or set of rules by running
 * an input message against a set of <code>Evaluator</code>
 * implementations that determine if the message is valid.
 *
 * @see com.nightfire.framework.rules.Evaluator
 */
public class ActionRuleProcessor extends MessageProcessorBase {

    /**
     * The identifier used when in log messages by this class.
     */
    private static final String CLASSNAME = "ActionRuleProcessor: ";
    
    /**
     * The file filter used in looking for custom rule set evaluator classes.
     */
    protected static ClassAndDirectoryFilter ruleSetFilter =
        new ClassAndDirectoryFilter();

    /**
     * This is used to determine if a file name is a java class.
     */
    public static final String CLASS_SUFFIX = ".class";

    /**
     * The property that indicates the directory, jar file or zip file
     * containing the evaluator classes.
     */
    public static final String NF_RULE_CLASSPATH_PROP = "NF_RULE_CLASSPATH";

   /**
    * This property flags whether this rule processor is enabled or not.
    */
    public static final String ENABLED_PROP = "ENABLED";

   /**
    * The name of an iterative property that will specify which
    * Evaluator classes should be used to evaluate the input message.
    * If this property is used, then only the classes listed here will be loaded.
    */
    public static final String EVALUATOR_CLASS_PROP = "EVALUATOR_CLASS";

   /**
    * The property that indicates the location of the input message in
    * the MessageProcessorContext or MessageObject.
    */
    public static final String INPUT_MESSAGE_LOCATION_PROP = "INPUT_MESSAGE_LOCATION";

   /**
    * A list of Evaluator class names, loaded from the EVALUATOR_CLASS properties,
    * that will be used to evaluate the input message.
    */
    private List evaluatorNames;

    /**
     * Custom class path String array built from value specified
     * in NF_RULE_CLASSPATH.
     */
    protected String[] customClasspath = null;


   /**
    * The location of the input message in the message object or the context.
    */
    protected String inputMessageLocation;

    /**
     * This is used to cache the evaluator names found in a particular
     * classpath.
     */
    private static EvaluatorNameCache evaluatorNameCache =
       new  EvaluatorNameCache();

    /**
     * Flag indicating whether this component in enabled or not. Default is true.
     */
    private boolean enabled = true;

    /**
     * Classpath for Nightfire rule classes
     */
    String nfRuleClasspath = null;

   /**
    * Gets the input message location and Evaluator class names from
    * this processor's properties.
    *
    * @param key  The persistent property key.
    * @param type The persistent property type.
    */
    public void initialize(String key, String type) throws ProcessingException{

        super.initialize(key, type);

        // create list to hold the names of the rules evaluators to be executed
        evaluatorNames = new ArrayList();

        inputMessageLocation = getRequiredPropertyValue(INPUT_MESSAGE_LOCATION_PROP);

        //We need to support two different properties to set the CLASSPATH to the rules
        //use NF_RULE_CLASSPATH has precedence if both exist.

        String ruleClassPath = getPropertyValue(NF_RULE_CLASSPATH_PROP);

        if ( StringUtils.hasValue(ruleClassPath) )
        {
            nfRuleClasspath = ruleClassPath;
        }

        // Get the list of Evaluator class names
        int i = 0;
        String evalClass = getPropertyValue( PersistentProperty.getPropNameIteration(EVALUATOR_CLASS_PROP, i) );

        while(evalClass != null){

            evaluatorNames.add(evalClass);
            evalClass = getPropertyValue( PersistentProperty.getPropNameIteration(EVALUATOR_CLASS_PROP, ++i) );

        }

        // Determine whether we should enable or disable this component.
        String enabledProp = getPropertyValue(ENABLED_PROP);
        if(enabledProp != null){
            try {
                enabled = StringUtils.getBoolean( enabledProp );
            }
            catch(FrameworkException fex){

                Debug.warning( "The value ["+ enabledProp + 
                        "] for property [" + ENABLED_PROP +
                "] is not a valid boolean value. Using default value [true]." );
            }
        }
    }

   /**
    * This method gets the input message from the configured location
    * and validates the message using each of the configured Evaluators.
    * The input String must be XML.
    * Any rule errors that occur are accumulated and thrown as a
    * MessageException.
    *
    * @throws MessageException if the input message fails validation
    *         by any of the Evaluators, then the list of rule errors
    *         will be thrown as a MessageException.
    * @throws ProcessingException if any exceptions (such as not
    *                             being able to locate a specified
    *                             Evaluator) arise while trying to
    *                             validate the message.
    *
    * @return If the input is successfully validated, it is passed
    *         on unharmed to this processor's next processors as
    *         the value(s) of the returned NVPair array. If a
    *         null MessageObject is given, a null is returned.
    *
    */
    public NVPair[] process( MessageProcessorContext context, MessageObject input)
    throws ProcessingException, MessageException {

        // The message processor gag reflex: if you
        // feed me a null, I spit it back at you.
        if(input == null){
            return null;
        }

        // if this processor is not enabled,
        // then forward the input message unchanged.
        if ( !enabled ) {
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "ENABLED flag set to [false] -> skip processing.");
            
            return formatNVPair(input);
        }

        // Check if rules should be skipped for this particular request.
        if ( !applyRulesPerHeader( context ) ) {
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, "Header indicates that usage-rules shouldn't be applied, so skipping them." );
            
            return formatNVPair(input);
        }

        // be sure to load up the customCustom
        customClasspath = getCustomClasspath(context, input);

        if(evaluatorNames.size() == 0 )
        {
            Debug.warning( CLASSNAME +
                    "No evaluators were specified by [" + EVALUATOR_CLASS_PROP +
                    "] properties or found in the classpath [" +  nfRuleClasspath +
                    "], so the [" + getName() + "] processor will be disabled.");

            return formatNVPair(input);
        }

        boolean success = true;
        ErrorCollection errors = new ErrorCollection();

        // Get the input message
        Document message = getDOM(inputMessageLocation, context, input);

        XPathAccessor messageAccessor = null;
        try{
            messageAccessor = new CachingXPathAccessor( message );
        }
        catch(FrameworkException fex){

            throw new MessageException ( "Could not parse input message: " + fex );        
        }

        /*
         * Step through each Evaluator name, and evaluate 
         * the parsed input message using each of these evaluators.
         */
        success = executeRules( evaluatorNames, messageAccessor,
                errors, customClasspath );

        if(!success){
            throw new MessageException ( errors.toString() );        
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, CLASSNAME + "The input message is valid.");
        
        // Set the modified message before returning.
        try
        {
            set(inputMessageLocation, context, input, message );
        }
        catch ( FrameworkException fe )
        {
            throw new ProcessingException( "Could not set modified Document, " + fe );
        }
        
        // if we have made it this far, return the (probably)modified message.
        return formatNVPair(input);
    }

    /**
     *  Check to see whether header flag value indicates that rules should be executed or not.
     *
     *  @param context   Message-processor context containing header.
     *
     * @return  Flag indicating whether rules should be applied (true) or not (false).
     *
     * @exception ProcessingException thrown on any errors.
     */
    private boolean applyRulesPerHeader ( MessageProcessorContext context ) throws ProcessingException
    {
        try
        {
            // Absence of header indicates we want to apply business rules.
            if ( !context.exists( MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME ) )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "No header in context, so will apply usage-rules." );

                return true;
            }

            XMLMessageParser p = new XMLMessageParser( context.getDOM( MessageProcessingDriver.CONTEXT_REQUEST_HEADER_NAME ) );

            // Absence of flag in header indicates we want to apply business rules.
            if ( !p.valueExists( HeaderNodeNames.APPLY_BUSINESS_RULES_NODE ) )
            {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log( Debug.MSG_STATUS, "No [" + HeaderNodeNames.APPLY_BUSINESS_RULES_NODE
                                           + "] value in header, so will apply usage-rules." );

                return true;
            }

            boolean flag = StringUtils.getBoolean( p.getValue( HeaderNodeNames.APPLY_BUSINESS_RULES_NODE ) );

            if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Applly-business-rules flag value in header is [" + flag + "]." );

            return flag;
        }
        catch ( Exception e )
        {
            throw new ProcessingException( e.getMessage() );
        }
    }



    /**
     *  Helper method to execute a list of RuleSets that extend
     *  <code>RuleSetEvaluatorBase</code>.
     *
     *  @param evaluatorNames   the list of class names
     *  @param parseMessage     the message to execute the rules against
     *  @param errors           collection to store errors to report
     *  @param path             the directory, jar file or zip file containing
     *                          the evaluator classes
     *
     * @return boolean          whether this list of evaluators was successful or
     *                          not.
     *
     * @exception ProcessingException thrown if unable anything goes wrong loading
     *                                or executing a rule.
     */
    private boolean executeRules(List evaluatorNames,
                                 XPathAccessor parsedMessage,
                                 ErrorCollection errors,
                                 String[] cp )
                                 throws ProcessingException{

      boolean result = true;
      int count = evaluatorNames.size();

      for(int i = 0; i < count; i++)
      {

        String evaluatorClassName = evaluatorNames.get(i).toString();

          if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            Debug.log(Debug.MSG_STATUS,
            CLASSNAME+
            "Evaluating message using evaluator class ["+
            evaluatorClassName+"]");
          }

          try
          {
            boolean success = RuleEngine.evaluate(evaluatorClassName,
                                                  parsedMessage, errors, cp);

            if( success ){

              if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
                Debug.log(Debug.MSG_STATUS,
                CLASSNAME+
                evaluatorClassName+
                ": validation was successful.");
              }

            }
            else{
              result = false;
              if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
              Debug.log(Debug.MSG_STATUS,
              CLASSNAME+
              evaluatorClassName+
              ": validation failed.");
              }
            }

          }
          catch(FrameworkException fex){
            // throw any exceptions
            throw new ProcessingException(fex.getMessage());
          }
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
    */
    protected void getRuleSetEvaluators(File current,
                                        String path,
                                        List discoveredEvaluators,
                                        String[] cp )
                                        throws FrameworkException {

        String name = current.getName();

        if( current.isDirectory() ) {

            File[] files = current.listFiles( ruleSetFilter );

            if( StringUtils.hasValue(path) ){
                path += ".";
            }

            path += name;

            for(int i = 0; i < files.length; i++){
                getRuleSetEvaluators(files[i], path, discoveredEvaluators, cp);
            }

        }
        // we won't directly load inner classes (classes containing a $ in
        // their name)
        else if(name.endsWith(CLASS_SUFFIX) && ( name.lastIndexOf("$") == -1 ) ) {

            name = name.substring( 0, name.length() - CLASS_SUFFIX.length() );

            String evaluatorName = path + "." + name;

            // load the named class from the given classpath
            Class evaluatorClass = ObjectFactory.loadClass( evaluatorName, cp );

            // but only add classes that are not abstract and
            // extend RuleSetEvaluatorBase
            if(  !isAbstract(evaluatorClass) &&
                 RuleSetEvaluatorBase.class.isAssignableFrom( evaluatorClass ) ) {

                discoveredEvaluators.add(evaluatorName);
            }
        }
    }

    /**
     * This looks for rule set evaluators in the given jar file
     * and adds any instances it finds to the list
     * of discoveredEvaluators.
     *
     * @param jarFile File a reference to the jar file to be searched.
     * @param discoveredEvaluators List the list where the results of the
     *                             search will be added.
     * @param cp String[] the classpath to use when loading the classes.
     *
     * @throws FrameworkException if an error occurs while trying to
     *                            read the archive or while trying
     *                            to load the classes.
     */
    protected void getRuleSetEvaluators(File jarFile,
                                        List discoveredEvaluators,
                                        String[] cp )
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
                                   discoveredEvaluators,
                                   cp);

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
     * @param discoveredEvaluators List the list to which any rule set
     *                             class names will be added.
     * @param cp String[] the classpath that will be searched for rule
     *                    set evaluators.
     * @throws FrameworkException if a class cannot be loaded.
     */
    protected void getRuleSetEvaluator(String name,
                                       List discoveredEvaluators,
                                       String[] cp)
                                       throws FrameworkException{

      if( name.endsWith(CLASS_SUFFIX) && ( name.lastIndexOf("$") == -1 ) ) {

        String evaluatorName = name.substring(0, name.length() -
                                              CLASS_SUFFIX.length());

        //If the evaluator names are listed, then load only those listed in the
        //EVALUATOR_CLASS_PROP property. Otherwise load everything.
        if ( ( evaluatorNames.size() > 0 ) && ( !evaluatorNames.contains( evaluatorName ) ) )
            return;
        //else go ahead and add it.

        // load the named class from the given classpath
        Class evaluatorClass = ObjectFactory.loadClass(evaluatorName, cp);

        // but only add classes that are not abstract and
        // extend RuleSetEvaluatorBase
        if (!isAbstract(evaluatorClass) &&
            RuleSetEvaluatorBase.class.isAssignableFrom(evaluatorClass)) {

          discoveredEvaluators.add(evaluatorName);

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
    * This method initializes the customClasspath attribute used
    * for loading rules. This checks to see if the NF_RULE_CLASSPATH
    * is being passed in dynamically in either the context or the
    * input message. If found, this dynamic value will be used. If not found,
    * then the peristent property value of NF_RULE_CLASSPATH will be used
    * instead.
    *
    * This callout is provided so that child classes such as the
    * the ConditionalProcessorBase can take advantage of this
    * functionality.
    *
    * @param context the context passed to the process() method.
    *                This is checked for a possible NF_CLASSPATH_PROP value.
    * @param input the input passed to the process() method.
    *              This is also checked for a possible NF_CLASSPATH_PROP value.
    */
    protected String[] getCustomClasspath(MessageProcessorContext context,
                                          MessageObject input )
                                          throws ProcessingException,
                                                 MessageException {

        if(customClasspath != null){
           return customClasspath;
        }

        // check to see if the NF_RULE_CLASSPATH property's value
        // was passed to us dynamically via the context
        String dynamicRuleClasspath = getProperty(context, input,
                                                  NF_RULE_CLASSPATH_PROP);

        // If there was an NF_RULE_CLASSPATH in the context, and
        // it is different from the NF_RULE_CLASSPATH specified in
        // the persistent properties, then use that classpath.
        if( StringUtils.hasValue(dynamicRuleClasspath) &&
            ! dynamicRuleClasspath.equals(nfRuleClasspath) )
        {

           if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

                Debug.log(Debug.MSG_STATUS,
                          "The value of the "+NF_RULE_CLASSPATH_PROP+
                          " was retrieved dynamically from the context: ["+
                          dynamicRuleClasspath+"]");

           }

           nfRuleClasspath = dynamicRuleClasspath;

        }

        if ( StringUtils.hasValue(nfRuleClasspath) ) {

            StringBuffer errors = new StringBuffer();

            customClasspath = loadClassesWithCustomClassLoader(nfRuleClasspath,
                                                               errors );

            if ( errors.length() != 0 ) {
                throw new ProcessingException("Could not build classpath: " +
                                               errors.toString() );
            }

        }

        return customClasspath;

    }

    /**
     *  This utility method builds an array representation of the classpath.
     *  It parses the classpath and for each entry:
     *
     *  1. ensures directory, jar or zip file exists on file system
     *  2. if it is a directory
     *      recurses through immediate child directories
     *      calls <code>getRuleSetEvaluators</code> which will explicitly load
     *      all classes (except inner classes) it finds into JVM.
     *  3. finds all rule set evaluators in any jar files in the given path
     *
     *   Errors are collected and sent back to caller.

     *
     *  @param cp       the classpath that contains the rules to be evaluated
     *                  assume incoming classpath has valid path separator
     *                  for the OS running this MP.
     *
     *  @param errors   a container to collect errors
     *
     *  @return String[]    the classpath converted into String[]
     *
     */
    protected String[] loadClassesWithCustomClassLoader(String cp,
                                                        StringBuffer errors) {

        // check to see if we have already discovered and cached the
        // evaluator class names in the given classpath
        EvaluatorNames names = evaluatorNameCache.get(cp);

        if( names != null ){

          List cachedNames = names.getNames();

          if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

                Debug.log(Debug.MSG_STATUS,
                          "The following list of cached rule set evaluators "+
                          cachedNames+
                          " will be used for classpath ["+
                          cp+"]");

          }

          // add the cached names to the list of rule sets that
          // this processor will execute
          addEvaluatorNames( cachedNames );

          return names.getClasspath();

        }

        StringTokenizer tokenizer =
                new StringTokenizer(cp, File.pathSeparator);

        int tokenCount = tokenizer.countTokens();

        String[] classpath = new String[tokenCount];

        List directories = new ArrayList(tokenCount);
        List jars = new ArrayList(tokenCount);

        int i = 0;

        //Pass 1:
        //Build the complete classpath in case there are dependencies
        while (tokenizer.hasMoreTokens())
        {
                String element = tokenizer.nextToken();
                File f = new File(element);

                //
                // Existence test.
                //
                if (!f.exists()) {
                        errors.append("\n'" + element + "' " +
                                "does not exist.");
                }
                //
                // Validity test.
                //
                else if ( f.isDirectory() )
                {
                    //add to classpath
                    classpath[i++] = element;

                    //add to list of directories
                    directories.add(f);

                }
                else if ( (element.toLowerCase().endsWith(".jar")) ||
                                  (element.toLowerCase().endsWith(".zip")) )
                {
                    //add to custom classpath
                    classpath[i++] = element;

                    // add to list of jars
                    jars.add( f );

                }
                else
                {
                    errors.append("\n'" + element + "' " +
                                "is not a directory, .jar file, or .zip file.");
                }
        }

        // the list of rule set evaluator names discovered in the classpath
        List discoveredNames = new ArrayList();

        int dirCount = directories.size();
        // Recurse through the directories in the classpath
        // and find the RuleSetEvaluators
        for( int j = 0; j < dirCount; j++ )
        {

            File dir = (File) directories.get(j);
            File[] files = dir.listFiles( ruleSetFilter );
            File childDir = null;

            // for all child directories, recursively load
            // RuleSetEvalutator classes
            try
            {
                for ( int k = 0; k < files.length; k++ ) {
                    childDir = files[k];
                    getRuleSetEvaluators(childDir,
                                         "",
                                         discoveredNames,
                                         classpath );
                }
            }

            catch ( FrameworkException fex ) {
                errors.append("Unable to initialize rule set classes "+
                              "in directory ["+childDir + "]" +
                              fex.getMessage() );
            }

        }

        // check the jar files for implementations of RuleSetEvaluators
        int jarCount = jars.size();

        for(int n = 0; n < jarCount; n++){

            File jar = (File) jars.get(n);

            try
            {
               getRuleSetEvaluators(jar, discoveredNames, classpath);
            }
            catch ( FrameworkException fex ) {
                errors.append("Unable to initialize rule set classes "+
                              "in archive ["+jar+ "]" +
                              fex.getMessage() );
            }

        }

        // add the names found to the list of evaluators that this
        // processor will execute
        addEvaluatorNames( discoveredNames );

        // cache the discovered class names for next time
        evaluatorNameCache.add(cp, classpath, discoveredNames);

        return classpath;

    }

    /**
     * Adds the given list of Evaluator class names to the list of evaluators
     * that this processor will execute. The class names will only be added
     * if they do not already exist in the list of evaluators. This
     * keeps rule sets from being executed twice.
     *
     * @param names List a list of string evaluator class names.
     */
    private void addEvaluatorNames( List names ){

        int nameCount = names.size();

        for( int i = 0; i < nameCount; i++ ){

           String evaluatorName = (String) names.get(i);

           if( ! evaluatorNames.contains(evaluatorName) ){

              evaluatorNames.add(evaluatorName);
           }
        }
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
                       String[] classpathTokens,
                       List evaluatorClassNames){

          cache.put(classpath,
                    new EvaluatorNames(classpathTokens, evaluatorClassNames) );

       }

       public EvaluatorNames get(String classpath){

          EvaluatorNames result = (EvaluatorNames) cache.get(classpath);


          return result;

       }

       public void flushCache(){

          cache.clear();

       }

    }

    /**
     * This class contains a classpath and the list of evaluator class
     * names found in that classpath.
     */
    private static class EvaluatorNames{

       private String[] classpath;

       private List names;

       public EvaluatorNames(String[] classpath, List names){

          this.classpath = classpath;
          this.names = names;

       }

       public String[] getClasspath(){

          return classpath;

       }

       public List getNames(){

          return names;

       }

       public String toString(){

          return names.toString();

       }

    }

}

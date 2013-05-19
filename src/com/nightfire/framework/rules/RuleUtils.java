package com.nightfire.framework.rules;

import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import org.apache.xerces.parsers.XMLParser;

import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.ErrorHandler;

import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.parsers.DOMParser;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;


/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This class defines utility methods used in the rules package.
*
*/
public class RuleUtils {

   public static final String VALIDATION_FEATURE = "http://xml.org/sax/features/validation";

   /**
   * "?>" used to locate the end of the XML document header.
   */
   public static final String XML_HEADER_ENDING = "?>";

   /**
   * The .class file extension.
   */
   public static final String CLASS_SUFFIX = ".class";

   /**
   * This is the name of the inner class generated for all RuleSets.
   * This is used in locating RuleSet class files.
   */
   public static final String TRIGGER_CLASS_INDICATOR = "$Trigger";

   /**
   * @return <code>ex.getMessage()</code> if the Exception's
   *         message is not null,
   *         <code>ex.toString()</code> otherwise.
   */
   public static String getExceptionMessage(Exception ex){

      return StringUtils.hasValue( ex.getMessage() ) ? ex.getMessage() :
                                                       ex.toString();

   }

   /**
   * Replaces .'s in a given package name with the system-specific file
   * separator.
   */
   public static String packageToPath(String packageName){

      return packageName.replace('.', java.io.File.separatorChar);

   }

   /**
   * This checks to see if a file exists for the given path and throws
   * a FileNotFoundException if the file does not exist.
   *
   * @param filename The path to the file whose existence will be checked.
   *
   * @throws FileNotFoundException if the file does not exist.
   */
   public static void fileExists(String filename) throws FileNotFoundException{

       File file = new File(filename);
       if( !file.exists() ){
          throw new FileNotFoundException("File ["+filename+"] does not exist.");
       }

   }

   /**
   * This is a utility for making a given classname valid by removing
   * whitespace, dots, quotes, parens, and any other non-alphanumeric chars
   * and replacing them with underscores.
   */
   public static String makeAlphaNumeric(String classname){

      if(classname == null) return "";

      StringBuffer result = new StringBuffer();

      char current;
      for(int i = 0; i < classname.length(); i++){

         current = classname.charAt(i);
         if( Character.isLetterOrDigit( current ) ){
            result.append(current);
         }
         else{
            result.append("_");
         }

      }

      return result.toString();

   }


   /**
   * This takes an XML and validates it using a given DTD file.
   * This method plugs a &lt;!DOCTYPE tag into the given XML
   * that references the given <code>dtdFileName</code>. This XML
   * is then parsed and validated using that DTD.
   *
   * @param xml The source XML string to be validated.
   * @param dtdFileName The name of the DTD file that will be used to
   *                    validate the XML.
   *
   * @throws MessageException if the XML is not valid or can't be parsed,
   *                          this exception will be thrown
   *                          with a message describing the problem.
   */
   public static void validateXML(String xml, String dtdFileName)
                      throws MessageException{

       XMLMessageParser getRootNode = new XMLMessageParser(xml);
       String docName = getRootNode.getDocument().getDocumentElement().getNodeName();

       dtdFileName = dtdFileName.replace(File.separatorChar, '/');

       if( dtdFileName.indexOf("/") == -1 ){
          dtdFileName = "./"+dtdFileName;
       }

       int endOfXMLHeader = xml.indexOf(XML_HEADER_ENDING);
       endOfXMLHeader += XML_HEADER_ENDING.length();
       StringBuffer validateMe = new StringBuffer(xml.substring(0, endOfXMLHeader));
       validateMe.append("\n");
       validateMe.append(XMLLibraryPortabilityLayer.DTD_REF_TOKEN);
       validateMe.append(" ");
       validateMe.append(docName);
       validateMe.append(" SYSTEM \"file:");
       validateMe.append(dtdFileName);
       validateMe.append("\">");
       validateMe.append(xml.substring(endOfXMLHeader) );

       if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION, "Validating:\n"+validateMe.toString());
       }


       XMLParser parser = new ValidatingParser();
       ErrorChecker errorHandler = new ErrorChecker(validateMe.toString());
       ((DOMParser)parser).setErrorHandler(errorHandler);
       Reader input = new StringReader(validateMe.toString());
       try{
          parser.parse(new XMLInputSource(null, null, null, input, null));
       }
       catch(Exception ex){

          throw new MessageException(getExceptionMessage(ex));

       }

       if( errorHandler.isError() ){

          if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
             Debug.log(Debug.RULE_EXECUTION, "XML is not valid:\n"+errorHandler.getErrorMessage());
          }

          throw new MessageException(errorHandler.getErrorMessage());

       }
       else if(Debug.isLevelEnabled(Debug.RULE_EXECUTION)){
          Debug.log(Debug.RULE_EXECUTION, "XML is valid.");
       }

   }

   /**
   * This is a DOM Parser that is set to always validate parsed XML.
   */
   static class ValidatingParser extends org.apache.xerces.parsers.DOMParser{

      public ValidatingParser(){

         super();
         try{
	     super.setFeature(VALIDATION_FEATURE, true);
         }
         catch(Exception ex){

            Debug.log(Debug.ALL_ERRORS,
                      "Could not set DOM Parser to validate: "+
                      getExceptionMessage(ex));

         }

      }

   }

   /**
   * This class collects error messages that occur during while
   * an XML is being parsed and validated. This class is then
   * used to check to see if any errors occured.
   */
   static class ErrorChecker implements org.xml.sax.ErrorHandler{

     /**
     * The accumulation of all the error messages so far.
     */
     private StringBuffer errors = new StringBuffer();

     /**
     * The XML source that is being parsed.
     */
     private String src;

     /**
     * This constructs an error checker.
     *
     * @param xml the XML source that is being parsed and checked by this
     *            ErrorChecker.
     */
     public ErrorChecker(String xml){

        src = xml;

     }

     /**
     * Defines ErrorHandler.
     */
     public void error(SAXParseException ex) throws SAXException{

        addToErrors(ex);

     }

     /**
     * Defines ErrorHandler.
     */
     public void fatalError(SAXParseException ex) throws SAXException{

        addToErrors(ex);

     }


     /**
     * Defines ErrorHandler.
     */
     public void warning(SAXParseException ex) throws SAXException{

        Debug.log(Debug.ALL_WARNINGS, "Line: ["+ex.getLineNumber()+
                                      "] Column: ["+ex.getColumnNumber()+
                                      "]: "+getExceptionMessage(ex));

     }

     /**
     * Clears the error message buffer.
     */
     public void reset(){

        errors = new StringBuffer();

     }

     /**
     * Returns true if there have been any errors.
     */
     public boolean isError(){

        return errors.length() > 0;

     }

     /**
     * Gets the contents of the accumulated error message.
     */
     public String getErrorMessage(){

        return errors.toString();

     }

     /**
     * A utility method for getting a particular line of the XML source.
     */
     private String getLine(int line){

        StringTokenizer lines = new StringTokenizer(src, "\n");
        String result = null;
        String current;

        for(int count = 1; count <= line && lines.hasMoreTokens(); count++){

           current = lines.nextToken();

           if(count == line){
              result = current;
              break;
           }

        }

        return result;

     }

     /**
     * This adds an error message to the errors buffer.
     */
     private void addToErrors(SAXParseException ex){

        if(errors.length() > 0) errors.append("\n");
        errors.append("\n");
        errors.append("Line: [");
        errors.append(ex.getLineNumber());
        errors.append("] Column: [");
        errors.append(ex.getColumnNumber());
        errors.append("]:\n");
        String badLine = getLine(ex.getLineNumber() );
        if(badLine != null){
           errors.append(badLine);
           errors.append("\n");
        }
        errors.append( getExceptionMessage(ex) );

     }

   }


    /**
    * This tokenizes the given classpath by the system's file separator,
    * returning the results as an array.
    *
    * @param classpath a Java classpath delimited by the current platform's path
    *                  separator.
    * @returns an array of the paths found in the classpath.
    */
    public static String[] splitClasspath(String classpath){

        StringTokenizer tokenizer = new StringTokenizer(classpath,
                                                        File.pathSeparator);

        String[] paths = new String[tokenizer.countTokens()];

        int i = 0;

        while (tokenizer.hasMoreTokens()){
           paths[i++] = tokenizer.nextToken();
        }

        return paths;

    }

    /**
    * This searches the classpath for any RuleSetEvaluator implementations and
    * returns an array of their classnames.
    *
    * @param classpath a Java classpath delimited by the current platform's path
    *                  separator.
    * @returns an array of the full classnames of any RuleSetEvaluator
    *          subclasses found in the given classpath.
    */
    public static String[] getRuleSetEvaluators(String classpath) throws FrameworkException {

       return getRuleSetEvaluators( splitClasspath(classpath) );

    }

    /**
    * This searches the classpath for any RuleSetEvaluator implementations and
    * returns an array of their classnames.
    *
    * @param classpath each element of this array is a single file path
    *                  making up the classpath that should be searched
    *                  for RuleSetEvalurators.
    * @returns an array of the full classnames of any RuleSetEvaluator
    *          subclasses found in the given classpath.
    */
    public static String[] getRuleSetEvaluators(String[] classpath) throws FrameworkException {

          return getRuleSetEvaluators (classpath, null);

    }
 
   /**
    * This searches the classpath for any RuleSetEvaluator implementations and
    * returns an array of their classnames.
    *
    * @param classpath each element of this array is a single file path
    *                  making up the classpath that should be searched
    *                  for RuleSetEvalurators.
    * @param parentLoader The parent class loader used to load external classes.
    * @returns an array of the full classnames of any RuleSetEvaluator
    *          subclasses found in the given classpath.
    */
    public static String[] getRuleSetEvaluators(String[] classpath, ClassLoader parentLoader) throws FrameworkException {

       // The Set implementation prevents a rule set class from being
       // added to this collection and ultimately executed more than
       // once. It is possible that a rule set class will
       // pop up more than once in a given classpath, but of course,
       // only the first class will be used.
       Set evaluatorNames = new HashSet();

       for(int i = 0; i < classpath.length; i++){

          File currentPath = new File(classpath[i]);

          if( ! currentPath.exists() ){
             String err = "Path ["+currentPath.getAbsolutePath()+
                 "] found in classpath does not exist.";
             Debug.error(err);

             throw new FrameworkException(err);

          }
          else{

             if( currentPath.isDirectory() ){

                File[] files = currentPath.listFiles();

                for(int j = 0; j < files.length; j++){

                   getRuleSetEvaluators( files[j],
                                         "",
                                         evaluatorNames,
                                         classpath, 
                                         parentLoader );

                }

             }
             // check to see if path is a jar or zip file
             else if( classpath[i].endsWith(".jar") ||
                      classpath[i].endsWith(".zip") ){

                try{

                   ZipFile archive = new ZipFile(currentPath);
                   Enumeration entries = archive.entries();

                   while(entries.hasMoreElements()){

                      ZipEntry entry = (ZipEntry) entries.nextElement();
                      String name = entry.getName();

                      // Check any class files to see if they are Evaluator classes.
                      // Don't directly load inner classes (ignore classes with $ in the name)
                      if( name.endsWith( TRIGGER_CLASS_INDICATOR +
                                         CLASS_SUFFIX ) ){

                         // remove $Trigger from the class name to
                         // get the name of the Rule Set class
                         name = StringUtils.replaceSubstrings( name,
                                                               TRIGGER_CLASS_INDICATOR,
                                                               "" );

                         // check to see if that rule set class exists
                         entry = archive.getEntry( name );

                         if( entry != null ){

                           // replace file separators with dots
                           name = name.replace('/', '.');
                           name = name.replace('\\', '.');

                           // remove .class extension from file name
                           name = name.substring(0,
                                                 name.length() -
                                                 CLASS_SUFFIX.length());

                           try {

                             if (isRuleSetEvaluator(name, classpath, parentLoader)) {

                               evaluatorNames.add(name);

                             }

                           }
                           catch (FrameworkException fex) {

                             Debug.warning("Class [" + name +
                                           "] could not be created: " + fex);

                           }

                        }

                      }

                   }

                }
                catch(IOException ioex){

                   Debug.error("Could not read archive ["+
                               currentPath.getPath()+
                               "]: "+ioex);

                }

             }

          }

       }

       // convert list of evaluator names to array and return it
       String[] results = new String[evaluatorNames.size()];
       evaluatorNames.toArray(results);

       return results;

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
    * @param cp the array of paths making up the classpath.
    */
    private static void getRuleSetEvaluators(File current,
                                             String path,
                                             Set evaluators,
                                             String[] cp,
                                             ClassLoader parentLoader )
    {

        String name = current.getName();

        if( current.isDirectory() ) {

            File[] files = current.listFiles();

            if( StringUtils.hasValue(path) ){
                path += ".";
            }

            path += name;

            for(int i = 0; i < files.length; i++){
                getRuleSetEvaluators(files[i], path, evaluators, cp, parentLoader);
            }

        }
        // look for Rule Set classes by finding their Trigger class
        else if( name.endsWith( TRIGGER_CLASS_INDICATOR + CLASS_SUFFIX ) ){

            name = name.substring( 0, name.length() - CLASS_SUFFIX.length() );

            // remove $Trigger from the class name to
            // get the name of the Rule Set class
            name = StringUtils.replaceSubstrings( name,
                                                  TRIGGER_CLASS_INDICATOR,
                                                  "" );

            String evaluatorName = path + "." + name;

            try{

               // only add classes that are instances of RuleSetEvaluatorBase
               if( isRuleSetEvaluator( evaluatorName, cp, parentLoader ) )
               {
                   evaluators.add(evaluatorName);
               }

            }
            catch(FrameworkException fex){

               Debug.warning( "Class ["+name+
                              "] could not be created: "+fex );

            }

        }

    }

  /**
    * Checks to see if the given class name is a Rule Set Evaluator.
    *
    * @param evaluatorName The full class name of the evalutor class to
    *                      be checked.
    * @param classpath the classpath from which the evaluator will be
    *                  loaded.
    *
    * @returns true is the named class is a subclass of RuleSetEvaluator.
    * @throws FrameworkException if the given evaluator class name
    *                            could not be created with the given classpath
    *                            or if the named class was found, but
    *                            is not an instance of RuleEvaluator.
    */
    public static boolean isRuleSetEvaluator(String evaluatorName,
                                             String[] classpath,
                                             ClassLoader parentLoader)
                                             throws FrameworkException{

         boolean isRuleSet = false;

         Evaluator eval =
            EvaluatorPool.getInstance().acquireEvaluator(evaluatorName,
                                                         classpath, 
                                                         parentLoader);

         // check to see if class is instance of RuleSetEvaluatorBase
         isRuleSet =
           RuleSetEvaluatorBase.class.isAssignableFrom(eval.getClass());


         EvaluatorPool.getInstance().releaseEvaluator(eval);


         return isRuleSet;

    }


}

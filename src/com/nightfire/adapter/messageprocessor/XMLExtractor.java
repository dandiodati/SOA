package com.nightfire.adapter.messageprocessor;


import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;

/* This component extract a Node from the input xml message specified
 * by NODE_TO_RETURN property and generates the xml message in String form
 * with the root node being the NEW_ROOT_NODE value, to be returned to the
 * next processor.
 */
public class XMLExtractor extends MessageProcessorBase
{
    //Node the extract
    String nodeToReturn = null;

    //Name of new root node
    String newRootNode = null;
    private static String NODE_TO_RETURN_PROP = "NODE_TO_RETURN";
    private static String NEW_ROOT_NODE_PROP = "NEW_ROOT_NODE";

    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
      super.initialize ( key, type );

        if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log ( Debug.OBJECT_LIFECYCLE, "XMLExtractor: Initializing" );

      nodeToReturn = ( String ) adapterProperties.get ( NODE_TO_RETURN_PROP );
      newRootNode = ( String ) adapterProperties.get ( NEW_ROOT_NODE_PROP );

      StringBuffer errMsg = new StringBuffer ( );
      if ( nodeToReturn == null )
        errMsg.append ( " : Property NODE_TO_RETURN is null" );
      if ( newRootNode == null )
        errMsg.append ( " : Property NEW_ROOT_NODE is null" );

      if ( errMsg.length () > 0 )
      {
        Debug.log ( Debug.ALL_ERRORS, "ERROR: XMLExtractor: Initialization failed with errors " +
          errMsg.toString ( ) );
        throw new ProcessingException ( "ERROR: XMLExtractor: Initialization failed with errors " +
          errMsg.toString ( ) );
      }

        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "XMLExtractor: Initialization over" );
    }//initialize


    /**
     * Process the input message (DOM or String) and (optionally) return
     * a value.
     *
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  Optional output message, or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if bad message
     */
    public NVPair[] execute ( MessageProcessorContext mpcontext, Object input )
    throws MessageException, ProcessingException
    {

      if ( input == null )
        return null;

      String inputMessage = null;
      try
      {
        inputMessage = (String) input;
      }
      catch ( ClassCastException e )
      {
        Debug.log ( Debug.ALL_ERRORS, "ERROR: XMLExtractor: " +
          "Input is not of String type" );
        throw new ProcessingException ( "ERROR: XMLExtractor: " +
          "Input is not of String type" );
      }

      //input is of String type
      //Create an XMLMessageParser and extract only node specified by nodeName
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "XMLExtractor: Processing input : " + inputMessage );

      XMLMessageParser parser = new XMLMessageParser ( inputMessage );
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "XMLExtractor: Created parser" );

      Node childNode = null;
      if ( parser.nodeExists ( nodeToReturn ) )
      {
        childNode = parser.getNode ( nodeToReturn );
        XMLMessageGenerator generator = generateOutputGenerator ( childNode );
        inputMessage = generator.generate ( );
      }
      else
      {
          if (Debug.isLevelEnabled(Debug.MSG_WARNING))
          {
              Debug.log ( Debug.MSG_WARNING, "XMLExtractor: [" + nodeToReturn + "] does not exist " );
              Debug.log ( Debug.MSG_WARNING, "XMLExtractor: Input message would be returned unmodified" );
          }
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "XMLExtractor: Returning message : " + inputMessage );

      return ( formatNVPair ( (String)inputMessage ) );
    }//process

    /**
     * Process the input Document Node to generate the out going DOM
     * End result will be a document representing a Message
     *
     * @param  input  Input parser with incomming data.
     *
     * @return  Generator with outgoing data.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
    private XMLMessageGenerator generateOutputGenerator(Node input)
    throws ProcessingException
    {
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "XMLExtractor: Copying Node to new DOM.");

        XMLMessageGenerator generator = null;
         try
         {
          generator = new XMLMessageGenerator( newRootNode );
         }
         catch ( MessageGeneratorException e )
         {
             Debug.log( Debug.ALL_ERRORS, "XMLExtractor: Could not create generator. " + e.getMessage() );
             throw new ProcessingException( "ERROR: XMLExtractor: Could not create generator." + e.getMessage() );
         }

         Document outDoc = generator.getDocument();
         Node root = outDoc.getDocumentElement();
         Node newNode = null;
         try
         {
             NodeList children = input.getChildNodes();
             // Extract Children nodes of original Root
             // and add them to a new Root -- Necessary
             // with the IBM DOM implementation.
             for(int i = 0 ; i < children.getLength() ; i++)
	           {
                 Node child = children.item(i);
                 newNode = XMLMessageBase.copyNode(outDoc, child);
                 XMLMessageBase.log(newNode);
                 root.appendChild(newNode);
	           }
	       }
         catch(MessageException e)
	       {
             Debug.log( Debug.ALL_ERRORS, "XMLExtractor: Failed copying " +
                      newRootNode + " node. " + e);
             throw new ProcessingException( "ERROR: XMLExtractor: " + e.getMessage() );
	       }

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "XMLExtractor: Copied Node to new DOM.");

        return generator;
    }


    /**
     * Main method for testing purposes.
     * @param args The arguments to this class for testing purposes. The number of
     * arguments should be 6 and are <KEY> <TYPE> <Input xml document file name as
     * test.xml, should be located in current directory> <DBNAME> <DBUSER> <DBPASSWD>
     */
    public static void main (String[] args) {

        Debug.enableAll();
        Debug.showLevels();

        if (args.length != 6)
        {
          Debug.log (Debug.ALL_ERRORS, "XMLExtractor: USAGE:  "+
          "jre -nojit -classpath %CLASSPATH%  "+
          "com.nightfire.adapter.messageprocessor.XMLExtractor "+
          "BAN_AV_EDI XML_EXTRACTOR resp.xml jdbc:oracle:thin:@192.168.10.158:1521:orcl testUpgrade testUpgrade");
        }
        String ilecAndOSS = args[0];
        String propertyType = args[1];
        String MESSAGEFILE = args[2];


        // Read in message file and set up database:
        FileCache messageFile = new FileCache();
        String inputMessage = null;
        try {

            inputMessage = messageFile.get(MESSAGEFILE);

        } catch (FrameworkException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLExtractor.main: " +
                      "FileCache failure: " + e.getMessage());
        }

        try {

            DBInterface.initialize(args[3], args[4], args[5]);

        } catch (DatabaseException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLExtractor.main: " +
                      "Database initialization failure: " + e.getMessage());
        }

        XMLExtractor logger = new XMLExtractor();

        try {

            logger.initialize(ilecAndOSS, propertyType);

        } catch (ProcessingException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLExtractor.main: " +
                      "call to initialize failed with message: " + e.getMessage());
        }

        try {
            logger.execute(null,inputMessage);

        }
        catch (MessageException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLExtractor.main: " +
                      "call to process failed with message: " + e.getMessage());
        }
        catch (ProcessingException e) {
            Debug.log( Debug.MAPPING_ERROR, "XMLExtractor.main: " +
                      "call to process failed with message: " + e.getMessage());
        }

        Debug.log( Debug.DB_STATUS, "XMLExtractor.main: clearing prepared statement");
        try {
             logger.execute(null,null);
        }
        catch (MessageException e) {

            Debug.log( Debug.MAPPING_ERROR, "XMLExtractor.main: " +
                      "null call to process failed with message: " + e.getMessage());
        }
        catch (ProcessingException e) {

            Debug.log( Debug.MAPPING_ERROR, "XMLExtractor.main: " +
                      "null call to process failed with message: " + e.getMessage());
        }

    }//main

}//end of class XMLExtractor

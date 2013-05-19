package com.nightfire.adapter.messageprocessor;

/**********************JDK PACKAGES*******************************/
import java.util.*;
import org.w3c.dom.*;

/**********************NightFire PACKAGES**************************/
import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;

/**
 * This class takes two XML inputs ( request, template ) and combines them, to
 * produce the resultant XML. If a template is not needed for a particular
 * message type, message subtype and supplier name combination, then the correspoding
 * entry should be absent in the properties.
 */

public class TemplateMessageProcessor extends MessageProcessorBase
{
 /**
  * Location of xml message to be processed
  */
  private String inputXMLLocation = null;

 /**
  * Location of processed xml message
  */
  private String outputXMLLocation = null;

 /**
  * Location of message type
  */
  private String msgTypeLocation = null;

 /**
  * Location of message sub type
  */
  private String msgSubTypeLocation = null;

 /**
  * Location of supplier name
  */
  private String supplierNameLocation = null;

 /**
  * Whether empty nodes in processed XML are to be deleted
  */
  private boolean deleteEmptyNodes = false;

 /**
  * Whether empty trees in processed XML are to be deleted
  */
  private boolean deleteEmptyTrees = true;

 /**
  * Map for mapping the template file name to the message type, message sub type
  * and the supplier name
  */
  private Map map = null;

 /**
  * Concatenator used for creating hashkey for Map
  */
  private static final String CONCATENATOR = "_";

 /**********************PERSISTENT PROPERTIES*******************************/
 /**
  * Location of xml message to be processed
  */
  private static final String INPUT_XML_LOCATION_PROP = "INPUT_XML_LOCATION";

 /**
  * Location of processed xml message
  */
  private static final String OUTPUT_XML_LOCATION_PROP = "OUTPUT_XML_LOCATION";

 /**
  * Location of message type
  */
  private static final String MESSAGE_TYPE_LOCATION_PROP = "MESSAGE_TYPE_LOCATION";

 /**
  * Location of message sub type
  */
  private static final String MESSAGE_SUBTYPE_LOCATION_PROP = "MESSAGE_SUBTYPE_LOCATION";

 /**
  * Location of supplier name
  */
  private static final String SUPPLIER_NAME_LOCATION_PROP = "SUPPLIER_NAME_LOCATION";

 /**
  * Prefix for message type property. Actual property would be "MESSAGE_TYPE_x".
  */
  private static final String MESSAGE_TYPE_PREFIX_PROP = "MESSAGE_TYPE";

 /**
  * Prefix for message sub type property. Actual property would be "MESSAGE_SUBTYPE_x"
  */
  private static final String MESSAGE_SUBTYPE_PREFIX_PROP = "MESSAGE_SUBTYPE";

 /**
  * Prefix for message supplier name property. Actual property would be "SUPPLIER_NAME_x"
  */
  private static final String SUPPLIER_NAME_PREFIX_PROP = "SUPPLIER_NAME";

 /**
  * Prefix for template file name property. Actual property would be "TEMPLATE_FILE_x"
  */
  private static final String TEMPLATE_FILE_PREFIX_PROP = "TEMPLATE_FILE";

 /**
  * Whether empty nodes in processed XML are to be deleted
  */
  private static final String DELETE_EMPTY_NODES_PROP = "DELETE_EMPTY_NODES";

 /**
  * Whether empty trees in processed XML are to be deleted
  */
  private static final String DELETE_EMPTY_TREES_PROP = "DELETE_EMPTY_TREES";

 /**
  * Retrieves properties from the database. These properties specify
  * the information for processing.
  *
  * @param key   A string which acts as a pointer to service provider
  *              and order type information in the database.
  *
  * @param type  A string which specifies the type of processor this is.
  *
  * @exception ProcessingException   Thrown if the specified properties
  *                               cannot be found or are invalid.
  */
  public void initialize ( String key, String type ) throws ProcessingException
  {
     super.initialize ( key, type );

     if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "TemplateMessageProcessor: Initializing.....");

    //Initialize a string buffer to contain all errors
    StringBuffer errorMessage = new StringBuffer();

    //Check is values fetched are correct and assign them to local variables

    //Input XML location can be null
    inputXMLLocation = getPropertyValue ( INPUT_XML_LOCATION_PROP );

    //Output XML location can be null
    outputXMLLocation = getPropertyValue ( OUTPUT_XML_LOCATION_PROP );

    msgTypeLocation = getRequiredPropertyValue ( MESSAGE_TYPE_LOCATION_PROP, errorMessage );
    msgSubTypeLocation = getRequiredPropertyValue ( MESSAGE_SUBTYPE_LOCATION_PROP, errorMessage );
    supplierNameLocation = getRequiredPropertyValue ( SUPPLIER_NAME_LOCATION_PROP, errorMessage );

    String deleteEmptyNodesStr = getRequiredPropertyValue ( DELETE_EMPTY_NODES_PROP, errorMessage );
    try
    {
      deleteEmptyNodes = getBoolean ( deleteEmptyNodesStr );
    }
    catch ( FrameworkException pe )
    {
      errorMessage.append ( pe.getMessage ( ) + "\n" );
    }

    String deleteEmptyTreesStr = getRequiredPropertyValue ( DELETE_EMPTY_TREES_PROP, errorMessage );
    try
    {
      deleteEmptyTrees = getBoolean ( deleteEmptyTreesStr );
    }
    catch ( FrameworkException pe )
    {
      errorMessage.append ( pe.getMessage ( ) + "\n" );
    }

    //Create the Map to contain the mapping between the request type and the
    //template file name
    map = new HashMap ( );

    //Number of template files names mapped so far
    int msgTypesCnt = 0;

    //Map template file name to the combination of message type, message sub type
    //and supplier name.
    //The message types are assumed to be listed in contiguous number ordering

    while ( true )
    {
      //If message type is present, only then look up the message sub type, supplier name and template file
      String msgTypeProp = PersistentProperty.getPropNameIteration ( MESSAGE_TYPE_PREFIX_PROP, msgTypesCnt );
      String msgTypeValue = getPropertyValue ( msgTypeProp );
      if ( msgTypeValue != null )
      //The message type is specified and hence there should be a sub type and a
      //supplierName and a template file name corresponding to it. If absent, an error is signaled.
      {
        String msgSubTypeProp = PersistentProperty.getPropNameIteration ( MESSAGE_SUBTYPE_PREFIX_PROP, msgTypesCnt );
        String msgSubTypeValue = getRequiredPropertyValue ( msgSubTypeProp, errorMessage );

        String supplierNameProp = PersistentProperty.getPropNameIteration ( SUPPLIER_NAME_PREFIX_PROP, msgTypesCnt );
        String supplierNameValue = getRequiredPropertyValue ( supplierNameProp, errorMessage );

        String templateFileProp = PersistentProperty.getPropNameIteration ( TEMPLATE_FILE_PREFIX_PROP, msgTypesCnt );
        String templateFileValue = getRequiredPropertyValue ( templateFileProp, errorMessage );

        //Map template file name to the message information
        if ( ( msgSubTypeValue != null ) && ( supplierNameValue != null ) &&
             ( templateFileValue != null ) )
        {
          map.put ( msgTypeValue +
                    CONCATENATOR + msgSubTypeValue +
                    CONCATENATOR + supplierNameValue,
                    templateFileValue );
        }//if values exist
      }//message type is not null
      else
      {
      //There are no more message types specified, so exit
        break;
      }
      ++msgTypesCnt;
    }//while

    if ( errorMessage.length () != 0 )
    //Some error occurred during processing above.
    {
      Debug.log ( Debug.ALL_ERRORS, "ERROR: TemplateMessageProcessor: " +
                "Failed initialization " + errorMessage.toString ( ) );
      throw new ProcessingException ( "ERROR: TemplateMessageProcessor: " +
                "Failed initialization " + errorMessage.toString ( ) );
    }

  } //initialize

 /**
  * Process the input message (DOM or String) and (optionally) return
  * a name / value pair.
  *
  * @param  input  Input message to process.
  *
  * @param  mpcontext The context that stores control information
  *
  * @return  Optional NVPair containing a Destination name and a Document,
  *          or null if none.
  *
  * @exception  ProcessingException  Thrown if processing fails.
  *
  * @exception  MessageException  Thrown if bad message.
  */
  public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input )
   throws MessageException, ProcessingException
  {
     if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log ( Debug.MSG_STATUS, "TemplateMessageProcessor: Processing.....");

    //Usual processing during null round of MessageProcessingDriver
    if ( input == null )
      return null;

    //Get template-file name
    String currentTemplateName = getTemplateName ( mpcontext, input );
    if ( currentTemplateName == null )
    {
        //return unmodified input as there is no template to process with
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log ( Debug.MSG_STATUS, "TemplateMessageProcessor: Returning unmodified input " +
                                            "as no template is specified." );

        return ( formatNVPair ( input ) );
    }

    //Get the request xml message for processing
    //Call super class's method to extract from a Hashtable
    Document doc = getDOM ( get ( inputXMLLocation, mpcontext, input ) );

    //Got XML message for processing
     if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log ( Debug.MSG_STATUS, "TemplateMessageProcessor: Got XML message for processing");

    //Process the request XML and the template. The caching of the template is
    //handled by this call.
    doc = XMLTemplatePopulator.populateDocument ( doc, currentTemplateName, deleteEmptyNodes, deleteEmptyTrees );

     if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log ( Debug.MSG_STATUS, "TemplateMessageProcessor: Finished processing inputs");

    set ( outputXMLLocation, mpcontext, input, doc );

    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "TemplateMessageProcessor: Describing return object " +
        input.describe ( ) );

    return ( formatNVPair ( input ) );

  } //end of process

 /**
  * Retrieve the template file name for the message being processed from the map
  *
  * @param mpcontext Context that stores control information like message type etc.
  *
  * @param input This is the input passed to the process method. This is not used in
  * this method, but is present for future use if it is decided to use the input for
  * storing control information too.
  *
  * @exception MessageException  Thrown if the message type, message sub type or supplier
  * name information is missing or cannot be accessed
  *
  * @exception ProcessingException Thrown if any other processing error occurs
  */
  private String getTemplateName ( MessageProcessorContext mpcontext, MessageObject input )
          throws ProcessingException, MessageException
  {
    //Get current message type to obtain corresponding template name from the properties
    String currentMsgType = getString ( get ( msgTypeLocation, mpcontext, input ) );
    String currentMsgSubType = getString ( get ( msgSubTypeLocation, mpcontext, input ) );
    String currentSupplierName = getString ( get ( supplierNameLocation, mpcontext, input ) );
    if ( ( currentMsgType == null ) || ( currentMsgSubType == null ) || ( currentSupplierName == null ) )
    {
      Debug.log ( Debug.ALL_ERRORS, "ERROR: TemplateMessageProcessor: " +
                "Either of Message type/sub type/supplier name of current message is not defined" );
      throw new ProcessingException ( "ERROR: TemplateMessageProcessor: " +
                "Either of Message type/sub type/supplier name of current message is not defined" );
    }
    String currentTemplateKey = new String ( currentMsgType +
                                      CONCATENATOR + currentMsgSubType +
                                      CONCATENATOR + currentSupplierName );
    String currentTemplateName = ( String ) map.get ( currentTemplateKey );

    //Got template name
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
    {
        if ( currentTemplateName == null )
            Debug.log ( Debug.MSG_STATUS, "TemplateMessageProcessor: No template specified for " +
            "the combination of Message type = [" + currentMsgType + "], Message sub-type = [" +
            currentMsgSubType + "] and Supplier name = [" + currentSupplierName + "]" );
        else
            Debug.log ( Debug.MSG_STATUS, "TemplateMessageProcessor: Template name is [" +
            currentTemplateName + "]" );
    }//if MSG_STATUS level is enabled

    //Return templateName;
    return currentTemplateName;

  }//end of getTemplateName

} //TemplateMessageProcessor
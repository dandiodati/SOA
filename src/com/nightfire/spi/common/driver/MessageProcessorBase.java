package com.nightfire.spi.common.driver;

import org.w3c.dom.*;
import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;

/**
 * Base class for all message-processors/adapters. This class provides utility 
 * methods for fetching properties, transforming objects from one form to another and
 * transparent access to data within the input/Context.
 */
public abstract class MessageProcessorBase implements MessageProcessor
{
 /**
  * Valid prefix from a data-access name in the context.
  */
  protected static final String CONTEXT = "context";

 /**
  * Valid prefix from a data-access name in the message-processor's input.
  */
  protected static final String MESSAGE = "message";

 /**
 * If the value of this constant is passed as the <code>name</code> parameter
 * of the <code>get</code> method, the return value will be the whole
 * contents of the input MessageObject.
 * This is the same behavior as when the name parameter is null. 
 *
 */
 public static final String INPUT_MESSAGE = "INPUT_MESSAGE";

 /**
  * Name of current processor
  */
	protected String name;

 /**
  * Names of next processors that get the return value from this processor
  */
	protected String[] toProcessorNames;

 /**
  * Properties for this processor
  */
	protected Hashtable adapterProperties;

 /**
  * Separator for denoting more than one next processor
  */
	public static final String SEPARATOR = "|";

 /**
  * Current processor name property as defined in the persistent properties
  */
 	public static final String NAME_PROP = "NAME";

 /**
  * Next processor name property as defined in the persistent properties
  */
	public static final String NEXT_PROCESSOR_NAME_PROP = "NEXT_PROCESSOR_NAME";

    /**
     * Next processor name indicating that there isn't a next processor.
     */
    public static final String NO_NEXT_PROCESSOR = "NOBODY";


    /**
     * Delimiter used in constructing a dynamic configuration property name from
     * the message-processor's name and the static property name.
     */
	public static final String DYNAMIC_PROPERTY_NAME_DELIMITER = "_";


    /**
   * String that specifies that the context is being processed
   */
  protected static final String CONTEXT_START = MessageProcessorContext.DATA_ACCESS_NAME_PREFIX +
  CONTEXT + MessageProcessorContext.DATA_ACCESS_NAME_DELIMITER;

    /**
     * Length of CONTEXT_START string.
     */
    protected static final int CONTEXT_START_LEN = CONTEXT_START.length( );

    /**
     * String that specifies that the message-processor's input is being processed
     */
    protected static final String MESSAGE_START = MessageProcessorContext.DATA_ACCESS_NAME_PREFIX +
        MESSAGE + MessageProcessorContext.DATA_ACCESS_NAME_DELIMITER;

    /**
     * Length of MESSAGE_START string.
     */
    protected static final int MESSAGE_START_LEN = MESSAGE_START.length( );

  /**
   * Get the value associated with the named item.  Structured item names are
   * separated by delimiter values ('.').  If the first name in a
   * structured name begins with the character '@', the corresponding data-access
   * object (message, context) is looked up, and the remaining part of the name 
   * is used for further processing of this data-access object.
   *
   * @param  name  Name of item (delimited text for nested value).  The data-access method
   *   is the token following the starting '@' character. The remaining part of the
   *   name indicates components within the item accessible by the data-access object.
   *   If the given name is null or equal to the INPUT_MESSAGE constant, then
   *   the value returned will be the entire contents of the input
   *   MessageObject.
   *
   * @param context One of the possible data-access objects for getting the named item's value
   *
   * @param input One of the possible data-access objects for getting the named item's value
   *
   * @return  Named item's value.
   *
   * @exception  MessageException  Thrown if data is invalid
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  protected Object get ( String name, MessageProcessorContext context, MessageObject input )
  throws MessageException, ProcessingException
  {
    /* There are two types of root data-access objects.
     * One is the context that is referenced as "@context",
     * The other is the input that is default ( has no special reference mechanism )
     */
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Name passed to get is " + name );

    Object retObj = null;
    if ( name == null || name.equals(INPUT_MESSAGE) )
    {
        if ( input == null )
        {
            throwMessageException( "ERROR: Attempt to invoke method against null message-object." );
        }

      //Fetch the single value contained in the MessageObject and return it
      retObj = input.get ( ) ;
    }
    else
    {
      name = name.trim ( );

      //Decide here if the information is contained in the input object or
      //in the MessageContext.

      //Check if name starts with "@context."

      if ( name.startsWith ( CONTEXT_START ) )
      //The data-access is in the context
      {
        String typeStr = name.substring ( CONTEXT_START_LEN );
        retObj = getContextData ( typeStr, context );
      }
      else
      //The data-access is in the input
      {
        retObj = getInputData ( getInputName(name), input );
      }
    }//name is not null

    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
    {
      Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Value fetched successfully for [" +
                  name + "] is [" + retObj.toString () + "]");
    }
    return retObj;

  }//get


  /**
   * Get the value associated with the named item and return it as a string.  Structured 
   * item names are separated by delimiter values ('.').  If the first name in a
   * structured name begins with the character '@', the corresponding data-access
   * object (message, context) is looked up, and the remaining part of the name is used for further
   * processing of this data-access object.
   *
   * @param  name  Name of item (delimited text for nested value).  The data-access
   *   is the token following the starting '@' character. The remaining part of the
   *   name indicates components within the item accessible by the data-access object.
   *
   * @param context One of the possible data-access objects for getting the named item's value
   *
   * @param input One of the possible data-access objects for getting the named item's value
   *
   * @return Named item's value in String form.
   *
   * @exception  MessageException  Thrown if data is invalid.
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  protected String getString ( String name, MessageProcessorContext context, MessageObject input )
  throws MessageException, ProcessingException
  {
    Object tempObj = get ( name, context, input );
    String tempStr = getString ( tempObj );
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Value fetched successfully for [" +
                    name + "] is [" + tempStr + "]");
    return tempStr;
  }//getString

  /**
   * Get the value associated with the named item and return its DOM value.  Structured 
   * item names are separated by delimiter values ('.').  If the first name in a
   * structured name begins with the character '@', the corresponding data-access
   * object (message, context) is looked up, and the remaining part of the name is 
   * used for further processing of this data-access object.
   *
   * @param  name  Name of item (delimited text for nested value).  The data-access object
   *   is the token following the starting '@' character. The remaining part of the
   *   name indicates components within the item accessible by the data-access object.
   *
   * @param context One of the possible data-access objects for getting the named item's value
   *
   * @param input One of the possible data-access objects for getting the named item's value
   *
   * @return Named item's value in XML DOM document form.
   *
   * @exception  MessageException  Thrown if data is invalid.
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  protected Document getDOM ( String name, MessageProcessorContext context, MessageObject input )
  throws MessageException, ProcessingException
  {
    Object tempObj = get ( name, context, input );
    Document tempDocument = getDOM ( tempObj );
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Value fetched successfully for [" +
                    name + "] is [" + tempDocument + "]");
    return tempDocument;
  }//getDOM


  /**
   * Test to see if the value associated with the named item exists.  Structured item names are
   * separated by delimiter values ('.').  If the first name in a
   * structured name begins with the character '@', the corresponding data-access
   * object (message, context) is looked up, and the remaining part of the name is used for further
   * processing of this data-access object.
   *
   * @param  name  Name of item (delimited text for nested value).  The data-access object
   *   is the token following the starting '@' character. The remaining part of the
   *   name indicates components within the item accessible by the data-access object.
   *
   * @param context One of the possible data-access objects for getting the named item's value
   *
   * @param input One of the possible data-access objects for getting the named item's value
   *
   * @return  true if value exists (and is non-null),  else false.
   */
    protected boolean exists ( String name, MessageProcessorContext context, MessageObject input )
    {
        return( exists( name, context, input, true ) );
    }


  /**
   * Test to see if the node (possibly with a value) associated with the named item exists.  Structured
   * item names are separated by delimiter values ('.').  If the first name in a
   * structured name begins with the character '@', the corresponding data-access object
   * object (message, context) is looked up, and the remaining part of the name is used for further
   * processing of this data-access object.
   *
   * @param  name  Name of item (delimited text for nested value).  The data-access object
   *   is the token following the starting '@' character. The remaining part of the
   *   name indicates components within the item accessible by the data-access object.
   *
   * @param context One of the possible data-access objects for getting the named item's value
   *
   * @param input One of the possible data-access objects for getting the named item's value
   *
   * @param checkForValue  If 'true' test checks for node with a 'value' attribute.  If 'false',
   *                       node existence is sufficient.
   *
   * @return  boolean true if value exists (and is not null), else false
   */
    protected boolean exists ( String name, MessageProcessorContext context,
                               MessageObject input, boolean checkForValue )
    {
        /* There are two types of root data-access objects.
         * One is the context that is referenced as "@context",
         * The other is the input that is default ( has no special reference mechanism )
         */
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Name passed to exists is " + name );

        // Assume it's not available.
        boolean retBool = false;

        if ( name == null || name.equals(INPUT_MESSAGE) )
        {
            // If no name is given, caller wants to get value from message-object, so
            // it's sufficient to check that MO is non-null.
            if ( input != null )
                retBool = true;
        }
        else
        {
            name = name.trim( );

            //Decide here if the information is contained in the input object or
            //in the MessageContext.

            //Check if name starts with "@context."

            if ( name.startsWith( CONTEXT_START ) )
            {
                //The data-access is in the context
                retBool = context.exists( name.substring(CONTEXT_START_LEN ), checkForValue );
            }
            else
            {
                //The data-access is in the input
                if ( input != null )
                    retBool = input.exists( getInputName(name), checkForValue );
            }
        }//name is not null

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "MessageProcessorBase: Value for [" + name
                       + "] exists = [" + retBool + "]" );

        return retBool;
    }//exists


  /**
   * Set the value for the named item.  Structured item names are
   * separated by delimiter values ('.').  If the first name in a
   * structured name begins with the character '@', the corresponding data-access
   * object (message, context) is looked up, and the remaining part of the name is
   * used for further processing of this data-access object.
   *
   * @param  name  Name of item (delimited text for nested value).  The data-access object
   *   is the token following the starting '@' character. The remaining part of the
   *   name indicates components within the item accessible by the data-access object.
   *
   * @param context One of the possible data-access objects for setting the named item's value
   *
   * @param output One of the possible data-access objects for setting the named item's value
   *
   * @param  value Value to be set for the named item
   *
   * @exception  MessageException  Thrown if data is invalid.
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  protected void set ( String name, MessageProcessorContext context,
  MessageObject output, Object value ) throws MessageException, ProcessingException
  {
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Name passed to set is " + name );

    Object retObj = null;
    if ( name == null || name.equals(INPUT_MESSAGE) )
    {
        if ( output == null )
        {
            throwMessageException( "ERROR: Attempt to invoke method against null message-object." );
        }

      //Set the "value" as the only value for the MessageObject
      output.set ( value );
    }
    else
    {
      name = name.trim ( );

      //Decide here if the information is contained in the input object or
      //in the MessageContext.

      //Check if name starts with "@context."

      if ( name.startsWith ( CONTEXT_START ) )
      //The data-access whose value is to be set is in the context
      {
        String typeStr = name.substring ( CONTEXT_START_LEN );
        setContextData ( typeStr, context, value );
      }
      else
      //The data-access whose value is to be set is in the output
      {
        setInputData ( getInputName(name), output, value );
      }
    }//name is not null

    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Value set successfully for [" +
                    name + "]" );

  }//set

  /**
   * Get the value for the named item from the input.  Structured item names are
   * separated by delimiter values ('.').
   *
   * @param  name  Name of item (delimited text for nested value).
   *
   * @param input The object from which the named item's value should be obtained.
   *
   * @return Object Named item's value
   *
   * @exception  MessageException  Thrown if data is invalid.
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  private Object getInputData ( String name, MessageObject input ) throws MessageException, ProcessingException
  {
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: String passed to getInputData is " + name );

    if ( input == null )
    {
        throwMessageException( "ERROR: Attempt to invoke method against null message-object." );
    }

    return ( input.get ( name ) );
  }//getInputData

  /**
   * Set the value for the named item in the output. Structured item names are
   * separated by delimiter values ('.').
   *
   * @param  name  Name of item (delimited text for nested value).
   *
   * @param output The object to set the value on.
   *
   * @param value  Named item's value to be set.
   *
   * @exception  MessageException  Thrown if data is invalid.
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  private void setInputData ( String name, MessageObject output, Object value )
              throws MessageException, ProcessingException
  {
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Name passed to setInputData is " +
                    name );

    if ( output == null )
    {
        throwMessageException( "ERROR: Attempt to invoke method against null message-object." );
    }

    output.set ( name, value );
  }

  /**
   * Get the value for the named item from the context. Structured item names are
   * separated by delimiter values ('.').
   *
   * @param  name  Name of item (delimited text for nested value).
   *
   * @param context The data-access method for getting the named item's value
   *
   * @return Object Named item's value
   *
   * @exception  MessageException  Thrown if data is invalid.
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  private Object getContextData ( String name, MessageProcessorContext context )
                throws MessageException, ProcessingException
  {
      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Name passed to getContextData is " +
                      name );
    return ( context.get ( name ) );
  }

  /**
   * Set the value for the named item in the "context". Structured item names are
   * separated by delimiter values ('.').  If the first name in a
   * structured name begins with the character '@', the corresponding data-access
   * object (message, context) is looked up, and the remaining part of the name is used for further
   * processing of this data-access object.
   *
   * @param  name  Name of item (delimited text for nested value).  The data-access method
   *   is the token following the starting '@' character. The remaining part of the
   *   name indicates components within the item accessible by the data-access.
   *
   * @param context  The object from which the named item's value should be obtained.
   *
   * @param value Named item's value to be set.
   *
   * @exception  MessageException  Thrown if data is invalid.
   *
   * @exception  ProcessingException  Thrown if item can't be accessed.
   */
  private void setContextData ( String name, MessageProcessorContext context,
                Object value )
                throws MessageException, ProcessingException
  {
    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
        Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: Name passed to setContextData is " +
                    name );
    context.set ( name, value );
  }

  /**
   * Wrapper method for viewing the contents of the context.
   *
   * @param context The object whose contents are to be viewed.
   *
   * @return  A human-readable description of the context.
   */
  protected String describe ( MessageProcessorContext context )
  {
    return ( context.describe ( ) );
  }//describe ( MessageProcessorContext )

  /**
   * Method for viewing the contents of the input.
   *
   * @param input The object whose contents are to be viewed.
   *
   * @return  A human-readable description of the input.
   */
  protected String describe ( MessageObject input )
  {
    return ( input.describe ( ) );
  }//describe ( Object )

  /**
   * Get a String-equivalent of the input. The input can be of String orDOM type.
   * In case of DOM type an XML text message is returned.
   *
   * @param input The object to be transformed
   *
   * @return  String-equivalent of input.
   *
   * @exception MessageException Thrown if String instance cannot be generated from the input
   *
   * @exception  ProcessingException  Thrown if any other processing error occurs
   */
  protected String getString ( Object input ) throws MessageException, ProcessingException
  {
      String retStr = null;
      try
      {
          retStr = Converter.getString ( input );
      }
      catch ( FrameworkException e )
      {
          throwMessageException ( e.getMessage ( ) );
      }

      return retStr;

  }//getString

  /**
   * Return the boolean value that is passed in as a String. The input should be one of
   * [T, TRUE, YES, Y] to be true and one of [F, FALSE, NO, N] to be false
   *
   * @param input The object to be transformed
   *
   * @return  true/false depending on the value of the input
   *
   * @exception MessageException Thrown if input does not have a value as specified above
   *
   * @exception  ProcessingException  Thrown if any other processing error occurs
   */
  protected boolean getBoolean ( Object input ) throws MessageException, ProcessingException
  {
    boolean retBool = false;

    if ( input instanceof String )
    {
        try
        {
            retBool = StringUtils.getBoolean ( ( String ) input );
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: getBoolean returning [" +
                retBool + "]" );

        }
        catch ( FrameworkException fe )
        {
            throwMessageException ( "ERROR: MessageProcessorBase: " +
                     "getBoolean failed with error " + fe.toString ( ) );
        }
    }//if
    else
    {
        throwMessageException ( "ERROR: MessageProcessorBase: " +
          "Object passed to getBoolean not of type String");
    }//else

    return retBool;

  }//getBoolean

  /**
   * Return the integer value that is passed in as a String. The input should be a valid
   * integer.
   *
   * @param input The object to be transformed.
   *
   * @return int Integer value of the input.
   *
   * @exception MessageException Thrown if the integer value of the input cannot be obtained
   *
   * @exception  ProcessingException  Thrown if any other processing error occurs
   */
  protected int getInt ( Object input ) throws MessageException, ProcessingException
  {
    int retInt = -1;
    
    if ( input instanceof String )
    {
        try
        {
            retInt = StringUtils.getInteger ( ( String ) input );
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "MessageProcessorBase: getInteger returning " + retInt );

        }
        catch ( FrameworkException fe )
        {
            throwMessageException ( "ERROR: MessageProcessorBase: " +
                     "getInteger failed with error " + fe.toString ( ) );
        }

    }//if
    else
    {
        throwMessageException ( "ERROR: MessageProcessorBase: Object passed to getInteger not of type String");
    }

    return retInt;

  }//getInteger

  /**
   * Return a DOM instance of the input. The input can be of String or DOM type. In case of
   * a string value it should contain a valid XML message from which a DOM can be generated.
   *
   * @param input The object to be transformed.
   *
   * @return  XML DOM document equivalent of input
   *
   * @exception MessageException Thrown if DOM instance cannot be generated of the input
   *
   * @exception  ProcessingException  Thrown if any other processing error occurs
   */
  protected Document getDOM ( Object input ) throws MessageException, ProcessingException
  {
      Document doc = null;
      try
      {
          doc = Converter.getDOM ( input );
      }
      catch ( FrameworkException e )
      {
          throwMessageException ( e.getMessage ( ) );
      }

      return doc;

  }//getDOM

  /**
   * Dummy implementation of the cleanup method that does nothing.  Provided so that
   * the processors do not have to implement it if no processing is necessary.
   *
   * @exception ProcessingException Thrown if processing fails
   */
  public void cleanup() throws ProcessingException
  {
    /**
     * Nothing being done here. This method absolves the message processor of
     * implementing the cleanup method, if not needed by the processor.
     */
  }

  /**
   * Return the current processor name.
   *
   * @return Name of current processor.
   */
	public String getName()
  {
    return name;
	}

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
    int propertyChainCount;
    PropertyChainUtil propChain = new PropertyChainUtil();
    
    if ( Debug.isLevelEnabled ( Debug.SYSTEM_CONFIG ) )
        Debug.log( Debug.SYSTEM_CONFIG,
                   "Looking for all message protocol adapter properties with key [" +
                   key + "], type [" + type + "]." );

    try
    {
      synchronized(PropertyChainUtil.class)
      {
          adapterProperties = propChain.buildPropertyChains(key, type);
      }
    }
    catch ( PropertyException pe )
    {
      throw new ProcessingException( pe.toString() );
    }

    if (adapterProperties == null)
    {
	    Debug.log( null, Debug.ALL_WARNINGS,
	              "No adapter properties loaded with key [" +
	               key + "], type [" + type + "]:" );
    }
    else
    {
        if ( Debug.isLevelEnabled ( Debug.SYSTEM_CONFIG ) )
        {
            Debug.log( Debug.SYSTEM_CONFIG,
                       "Loaded adapter properties with key [" +
                       key + "], type [" + type + "]:" );

            Debug.log( Debug.SYSTEM_CONFIG, PropUtils.suppressPasswords( adapterProperties.toString() ) );
        }
    }

		initialize();
  }//initialize ( key, type )

  /**
   * Verify the current processor name and parse the next processor names.
   *
   * @exception ProcessingException Thrown if properties are not set
   */
  private void initialize( ) throws ProcessingException
  {
    name = (String) adapterProperties.get(NAME_PROP);
    String toProcessorName = (String) adapterProperties.get(NEXT_PROCESSOR_NAME_PROP);
    Debug.log( this, Debug.UNIT_TEST, "Name: " + name );
    Debug.log( this, Debug.UNIT_TEST, "Next Processor Name: " + toProcessorName );

    if (name == null)
    {
      throw new ProcessingException("ERROR: MessageProcessorBase: " +
      "\"" + NAME_PROP + "\" must be specified in configuration.");
    }

    if (toProcessorName == null)
    {
      toProcessorNames = null;
      return;
    }

    StringTokenizer st = new StringTokenizer(toProcessorName, SEPARATOR);

    LinkedList nextProcList = new LinkedList( );

    while ( st.hasMoreTokens() )
    {
        String tok = st.nextToken( );

        if ( NO_NEXT_PROCESSOR.equalsIgnoreCase( tok ) )
            Debug.log( Debug.SYSTEM_CONFIG, "Skipping " + NO_NEXT_PROCESSOR + " in output list." );
        else
        {
            Debug.log( Debug.SYSTEM_CONFIG, "Next processor name: [" + tok + "]." );

            nextProcList.add( tok );
        }
    }

    if ( nextProcList.size() > 0 )
        toProcessorNames = (String[])nextProcList.toArray( new String[ nextProcList.size() ] );
    else
        toProcessorNames = null;
  }//initialize ( )


  /**
   * Return an NVPair array consisting of name/value-pairs with the inidvidual names
   * being one of NEXT_PROCESSOR_NAMES and value being the object passed in.
   * All values in the NVPair array are a reference to the object passed in to
   * this method.
   *
   * @param input The object to be sent to the next processors
   *
   * @return Name-Value pair array of next processor names and the object passed in
   *
   * @exception ProcessingException Thrown if processing fails
   */
	protected NVPair[] formatNVPair ( Object obj ) throws ProcessingException
	{
    if ( toProcessorNames == null )
    {
      return null;
    }

    NVPair[] result = new NVPair [ toProcessorNames.length ];
    for ( int i=0; i< toProcessorNames.length; i++ )
    {
      result[i] = new NVPair ( toProcessorNames[i], obj );
    }

    return result;
  }//formatNVPair

  /**
   * Return the value for the named property from the persistent properties.
   *
   * @param propName The property whose value is to be returned
   *
   * @return Value of named property, or null if not available.
   */
  protected String getPropertyValue ( String propName )
  {
    return ( ( String ) adapterProperties.get ( propName ) );
  }


    /**
     * Return the optional value for the named property from the persistent properties.  If the 
     * property value begins with a special token indicating a context or input data item, the 
     * property value is resolved against the appropriate object to get the final value.
     *
     * @param propName The property whose value is to be returned
     * @param context One of the possible data-access objects for getting the named item's value
     * @param input One of the possible data-access objects for getting the named item's value
     *
     * @return Fully resolved value of named property, or null if not available.
     *
     * @exception  MessageException  Thrown if data is invalid
     * @exception  ProcessingException  Thrown if item can't be accessed.
     */
    protected String getResolvedPropertyValue ( String propName, MessageProcessorContext context, 
                                                MessageObject input )
        throws MessageException, ProcessingException
    {
        String propValue = getPropertyValue( propName );

        if ( propValue == null )
            return null;

        if ( propValue.startsWith( CONTEXT_START ) || propValue.startsWith( MESSAGE_START ) )
            propValue = (String)get( propValue, context, input );

        return propValue;
    }


    /**
     * Return the required value for the named property from the persistent properties.  If the 
     * property value begins with a special token indicating a context or input data item, the 
     * property value is resolved against the appropriate object to get the final value.
     *
     * @param propName The property whose value is to be returned
     * @param context One of the possible data-access objects for getting the named item's value
     * @param input One of the possible data-access objects for getting the named item's value
     *
     * @return Fully resolved value of named property, or null if not available.
     *
     * @exception  MessageException  Thrown if data is invalid
     * @exception  ProcessingException  Thrown if item can't be accessed.
     */
    protected String getRequiredResolvedPropertyValue ( String propName, MessageProcessorContext context, 
                                                        MessageObject input )
        throws MessageException, ProcessingException
    {
        String propValue = getRequiredPropertyValue( propName );

        if ( propValue.startsWith( CONTEXT_START ) || propValue.startsWith( MESSAGE_START ) )
            propValue = (String)get( propValue, context, input );

        return propValue;
    }


    /**
   * Return the value for the named property from the persistent properties.
   *
   * @param propName The property whose value is to be returned
   * @param defaultValue default string to return if the property is empty or null.
   *
   * @return Value of named property, or defaultValue if the propName value is empty or null.
   */
  protected String getPropertyValue ( String propName, String defaultValue )
  {
    return ( PropUtils.getPropertyValue(adapterProperties, propName, defaultValue) );
  }

  /**
   * Test to see if the named property exists in the persistent properties.
   *
   * @param propName The property to be verified for existence.
   *
   * @return  true if the property exists, else false.
   */
  protected boolean propertyExists ( String propName )
  {
    if ( adapterProperties.get ( propName ) == null )
      return false;
    else
      return true;
  }

  /**
   * Return the value for the named required property from the 
   * persistent properties (if it exists).
   *
   * @param propName The property whose value is to be returned
   *
   * @return Property's value.
   *
   * @exception ProcessingException Thrown if property does not exist.
   */
  protected String getRequiredPropertyValue ( String propName ) throws ProcessingException
  {
    String propValue = getPropertyValue ( propName );
    if ( ! ( StringUtils.hasValue ( propValue ) ) )
    {
      throw new ProcessingException ("Required property value for " + propName + " is null");
    }
    else
    {
      return ( String ) propValue;
    }
  }//getRequiredPropertyValue

  /**
   * Return the value for the named property from the persistent properties
   * (if it exists).
   *
   * @param propName The property whose value is to be returned
   *
   * @param errorMsg  Container for any errors that occur during proessing. Error
   *  messages are appended to this container instead of throwing exceptions
   *
   * @return Named property's value.
   */
  protected String getRequiredPropertyValue ( String propName, StringBuffer errorMsg )
  {
    String propValue = null;
    try
    {
      propValue = getRequiredPropertyValue ( propName );
    }
    catch ( ProcessingException pe)
    {
      errorMsg.append ("Property value for " + propName + " is null\n");
      Debug.log ( Debug.ALL_WARNINGS, "Property value for " + propName + " is null\n");
    }
    return propValue;
  }//getRequiredPropertyValue


    /**
     * Return the value for the named property from the runtime context (dynamic configuration), 
     * or the persistent properties (static configuration) if absent from the context.
     * The name used to obtain the property from the context has the following format:
     *   <msg-proc-name>_<property-name>
     * where the 'msg-proc-name' is the configured name of the message-processor.  The name
     * used to obtain the property value from the persistent properties matches the argument.
     *
     * @param context  The  message context.
     * @param input  The input value to this message-processor.
     * @param propName The property whose value is to be returned
     *
     * @return Value of named property, or null if not available.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     * @exception  ProcessingException  Thrown if any other processing error occurs.
     */
	protected String getProperty ( MessageProcessorContext context, MessageObject input, String propertyName ) 
        throws MessageException, ProcessingException
    {
        // NOTE: The input parameter is currently unused.

        String dynamicPropName = getName() + DYNAMIC_PROPERTY_NAME_DELIMITER + propertyName;

        String propValue = null;

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Checking message-processor context for optional property named [" 
                        + dynamicPropName + "]." );

        if ( context.exists( dynamicPropName ) )
        {
            propValue = context.getString( dynamicPropName );

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "Obtained optional property [" + dynamicPropName 
                            + "] = [" + propValue + "] from message-processor context." );

            return propValue;
        }
        else
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "Optional property [" + dynamicPropName 
                            + "] wasn't found in message-processor context." );
        }

        propValue = getPropertyValue( propertyName );

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Persistent properties returned optional property [" 
                        + dynamicPropName + "] = [" + propValue + "]." );

        return propValue;
    }


    /**
     * Return the value for the named property from the runtime context (dynamic configuration), 
     * or the persistent properties (static configuration) if absent from the context.
     * The name used to obtain the property from the context has the following format:
     *   <msg-proc-name>_<property-name>
     * where the 'msg-proc-name' is the configured name of the message-processor.  The name
     * used to obtain the property value from the persistent properties matches the argument.
     *
     * @param  context  The  message context.
     * @param input  The input value to this message-processor.
     * @param propName The property whose value is to be returned
     *
     * @return Property's value.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     * @exception  ProcessingException  Thrown if any other processing error occurs.
     */
	protected String getRequiredProperty ( MessageProcessorContext context, MessageObject input, String propertyName ) 
        throws MessageException, ProcessingException
    {
        // NOTE: The input parameter is currently unused.

        String dynamicPropName = getName() + DYNAMIC_PROPERTY_NAME_DELIMITER + propertyName;

        String propValue = null;

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Checking message-processor context for required property named [" 
                        + dynamicPropName + "]." );

        if ( context.exists( dynamicPropName ) )
        {
            propValue = context.getString( dynamicPropName );

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "Obtained required property [" + dynamicPropName 
                            + "] = [" + propValue + "] from message-processor context." );

            return propValue;
        }
        else
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "Required property [" + dynamicPropName 
                            + "] wasn't found in message-processor context." );
        }

        propValue = getRequiredPropertyValue( propertyName );

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "Persistent properties returned required property [" 
                        + dynamicPropName + "] = [" + propValue + "]." );

        return propValue;
    }


    /**
     * Modify the input name by stripping off "@message.", 
     * if present.
     *
     * @param  name  String giving input location.
     *
     * @return  The possibly-altered name.
     */
    public static final String getInputName ( String name )
    {
        if ( !StringUtils.hasValue( name ) )
            return name;

        // Strip of "@message.", if present.
        if ( name.startsWith( MESSAGE_START ) )
            name = name.substring( MESSAGE_START_LEN );

        return name;
    }


    /**
     * Processes the input message or context information and optionally returns
     * a value.  The following driver-processor interaction rules apply:
     *		Non-null INPUT and non-null OUTPUT	-	Driver sends output to next message-processor in chain.
     *		Non-null INPUT and null OUTPUT		-	Processor is batching inputs for subsequent release.  Driver skips 
     *                                              processors in chain that follow the current one.
     *		Null INPUT and non-null OUTPUT		-	Driver has no more inputs for this processor.  Batching processor is emiting batched 
     *                                              output at this point, which will be sent by driver to next message-processor in chain.
     *		Null INPUT and null OUTPUT			-	Driver has no more inputs for this processor.  Processor indicates to driver
     *                                              that it is done processing.
     *
     * @param  context  The  message context.
     *
     * @param  input  Input message object to process.
     *
     * @return  An array of name-value pair objectss, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject msgObj )
        throws MessageException, ProcessingException
    {
        // NOTE: All new message-processors should implement this method instead of
        // the execute() method!  This implementation is provided to ease the
        // transition from the old-style processing to the new paradigm.

        NVPair[] nvp = null;

        if ( msgObj != null )
        {
            // Call old-style processing method with object extracted from MessageObject.
            nvp = execute( context, msgObj.get() );
        }
        else
        {
            nvp = execute( context, null );
        }

        // Make sure that all returned values are encapsulated in a MessageObject.
        if ( nvp != null )
        {
            for ( int Ix = 0;  Ix < nvp.length;  Ix ++ )
            {
                if ( (nvp[Ix].name != null) && (nvp[Ix].value != null) && !(nvp[Ix].value instanceof MessageObject) )
                    nvp[Ix] = new NVPair( nvp[Ix].name, new MessageObject( nvp[Ix].value ) );
            }
        }
        
        return nvp;
    }


    /**
     * Old-style processing method provided for backward compatibility.  New
     * processors should call process() instead of this method.
     *
     * @deprecated	Replace with call to process() method as time permits.
     *
     * @param  context  The  message context.
     *
     * @param  input  Input object to process.
     *
     * @return  A list of name and value pairs, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails.
     */
    public NVPair[] execute ( MessageProcessorContext context, Object obj )
        throws MessageException, ProcessingException
    {
        throw new ProcessingException( "ERROR: Message-processor needs to implement process() method!!" );
    }


    /**
     * Logs the error message argument and throws a message exception.
     *
     * @exception  MessageException  Always thrown with error message argument.
     */
    protected final void throwMessageException ( String errMsg ) throws MessageException
    {
          Debug.log( Debug.ALL_ERRORS, errMsg );

          throw new MessageException( errMsg );
    }

}//end of Class MessageProcessorBase

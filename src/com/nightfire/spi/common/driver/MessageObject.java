/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.spi.common.driver;

import java.util.*;
import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;

import com.nightfire.common.*;


/**
 * A message object encapsulates the input data that is passed 
 * to every MessageProcessor's process() method call as the second argument.
 * It contains utility methods for data access and conversion.
 */
public class MessageObject
{
    /**
     * Create a message-object.
     */
    public MessageObject ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating MessageObject ..." );
    }


    /**
     * Create a message-object, assigning it a value in the process.
     *
     * @param value Value to be contained by this message-object
     */
    public MessageObject ( Object value )
    {
        this( );

        set( value );
    }


    /**
     * Assign the message-object the value object
     *
     * @param  value  Value to be assigned to this message-object
     */
    public void set ( Object value )
    {
        if ( value == null )
        {
            Debug.log( Debug.ALL_WARNINGS, "WARNING: Attempt was made to assign message-object a null value - ignoring." );

            return;
        }

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Setting message-object's internal value with object of type [" 
                       + value.getClass().getName() + "]." );

        object = value;
    }


    /**
     * Assign the named-item to the message-object.
     *
     * @param  name   Name of item.
     *
     * @param  value  Value to assign to named item.
     *
     * @exception  MessageException  Thrown if named item value cannot be set
     *
     * @exception ProcessingException Thrown on configuration errors.
     */
    public void set ( String name, Object value ) throws MessageException, ProcessingException
    {
        name = MessageProcessorBase.getInputName( name );

        // Check that the input arguments are non-null.
      if ( !StringUtils.hasValue(name) )
      {
          throwProcessingException( "ERROR: MessageObject: Input argument 'name' to set() on message-object is null." );
      }

      if ( value == null )
      {
          throwMessageException( "ERROR: MessageObject: Input argument 'value' to set() on message-object is null." );
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log( Debug.MSG_STATUS, "Setting [" + name + "] on message-object with value of type ["
                     + value.getClass().getName() + "]." );

      // If the internal object value is null, create a new map to contain values.
      if ( object == null )
      {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))  
            Debug.log ( Debug.MSG_STATUS, "MessageObject: Creating Map container for storage.");

        set( new HashMap() );
      }

      setImplementation( name, value );

    }//set


    /**
     * Get the object contained in this message-object.
     *
     * @return  The object contained in this message-object
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown if any other processing error occurs
     */
    public Object get ( ) throws MessageException, ProcessingException
    {
      if ( object == null )
      {
          throwMessageException( "ERROR: MessageObject: There is no value available in message-object." );
      }

      return object;

    }//get


    /**
     * Get the object contained in this message-object in String form.
     *
     * @return The object contained in this message-object as String
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown if any other processing error occurs
     */
    public String getString ( ) throws MessageException, ProcessingException
    {
        String string = null;
        try
        {
            string = Converter.getString ( get ( ) );
        }
        catch ( FrameworkException e )
        {
            throwMessageException ( e.getMessage ( ) );
        }

        return string;

    }//getString


    /**
     * Get the object contained in this message-object in XML DOM Document form.
     *
     * @return  The object contained in this message-object as an XML DOM Document.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown if any other processing error occurs
     */
    public Document getDOM ( ) throws MessageException, ProcessingException
    {
        Document doc = null;
        try
        {
            doc = Converter.getDOM ( get ( ) );
        }
        catch ( FrameworkException e )
        {
            throwMessageException ( e.getMessage ( ) );
        }

        return doc;

    }//getDOM


    /**
     * Get the value associated with the named item.  Structured item names are
     * separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name
     * is used for further processing of this data-access object.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  Named item's value in String form.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown if any other processing error occurs
     */
    public String getString ( String name ) throws MessageException, ProcessingException
    {
        String string = null;
        try
        {
            string = Converter.getString ( get ( name ) );
        }
        catch ( FrameworkException e )
        {
            throwMessageException ( e.getMessage ( ) );
        }

        return string;

    }//getString

    /**
     * Get the value associated with the named item.  Structured item names are
     * separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name
     * is used for further processing of this data-access object.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  Named item's value in XML DOM Document form.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown if any other processing error occurs
     */
    public Document getDOM ( String name ) throws MessageException, ProcessingException
    {
        Document doc = null;
        try
        {
            doc = Converter.getDOM ( get ( name ) );
        }
        catch ( FrameworkException e )
        {
            throwMessageException ( e.getMessage ( ) );
        }

        return doc;

    }//getDOM


    /**
     * Get the value associated with the named item.  Structured item names are
     * separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name
     * is used for further processing of this data-access ojbect.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  Named item's value.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown on configuration errors.
     */
    public Object get ( String name ) throws MessageException, ProcessingException
    {
        name = MessageProcessorBase.getInputName( name );

      if ( !StringUtils.hasValue(name) )
      {
          throwProcessingException( "ERROR: MessageObject: Name input argument to get() on message-object is null" );
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageObject: Name of input to get is [" + name + "]." );

      Object value = getImplementation( name );

      //Check if return value has a valid value
      if ( value == null )
      {
          throwMessageException( "ERROR: MessageObject:  Can't find value named [" + name + "]." );
      }

      return value;
    }//get


    /**
     * Test to see if there is a value associated with the named item.
     * Structured item names are separated by delimiter values ('.').
     * If the first name in a structured name begins with the character '@',
     * the corresponding data-access object (message, context) is looked up,
     * and the remaining part of the name is passed to it for further processing.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  'true' if named item exist, otherwise 'false'.
     */

    public boolean exists ( String name )
    {
        return( exists( name, true ) );
    }


    /**
     * Test to see if there is a node (possibly with a value) associated with the
     * named item.  Structured item names are separated by delimiter values ('.').
     * If the first name in a structured name begins with the character '@',
     * the corresponding data-access object (message, context) is looked up,
     * and the remaining part of the name is passed to it for further processing.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @param checkForValue  If 'true' test checks for node with a value attribute.  If 'false',
     *                       node existence is sufficient.
     *
     * @return  'true' if named item exist, otherwise 'false'.
     */

    public boolean exists ( String name, boolean checkForValue )
    {
        name = MessageProcessorBase.getInputName( name );

        boolean retBool = true;

        if( getImplementation( name, checkForValue ) == null )
            retBool = false;

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "MessageObject: Value named ["
                        + name + "] exists? [" + retBool + "]" );

        return retBool;
    }//exists


    /**
     * Get a description of the message object in human-readable form.
     *
     * @return  Human readable string representation of message object.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        sb.append( "\n****************** MESSAGE OBJECT DESCRIPTION: ********************************\n" );
        sb.append( "OBJECT TYPE: [" );
        sb.append( "MessageObject" );
        sb.append( "]\n\n" );
        if ( object instanceof Map )
            sb.append( "AVAILABLE KEYS AND VALUES:\n\n" );
        else
            sb.append( "AVAILABLE VALUES:\n\n" );
        sb.append( "[");
        if ( object == null )
            sb.append( (String)null );
        else
            sb.append( object.toString() );
        sb.append( "]\n\n");
        sb.append( "\n*******************************************************************************\n" );

        return( sb.toString() );
    }


    /**
     * Set the named value on the target object.  NOTE: This method
     * assumes that the name and value arguments are non-null.
     *
     * @param  name   Name of value to set.
     *
     * @param  value  Value to assign to named item.
     *
     * @exception  MessageException  Thrown is named item's value cannot be set.
     *
     * @exception ProcessingException Thrown if name is invalid.
     */
    protected final void setImplementation ( String name, Object value )
    throws MessageException, ProcessingException
    {
        name = MessageProcessorBase.getInputName( name );

        // If a target object isn't available, throw an exception;
        Object target = get( );

        String mapKeyName = null;
        int index         = INVALID_LOC;

        // If the internal object value is a map ...
        if ( target instanceof Map )
        {
            // Extract data-access name from argument passed-in.
            int mapKeyNameEnd = name.indexOf( MessageProcessorContext.DATA_ACCESS_NAME_DELIMITER );

            // If the name is a simple (non-compound/dotted-text name ...
            if ( mapKeyNameEnd == -1 )
            {
                // Use the entire name as the access key to place the value in the Map contianer.
                if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Placing object named [" + name + "] of type ["
                               + value.getClass().getName() + "] into Map container." );

                Object previous = ((Map)target).put( name, value );

                if ( previous != null )
                {
                    if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                        Debug.log( Debug.MSG_DATA, "Value replaces previous Map container value of type ["
                                   + previous.getClass().getName() + "]." );
                }

                return;  // We're done if name doesn't contain sub-component names.
            }

            // Extract map access key from the rest of the name.
            mapKeyName = name.substring( 0, mapKeyNameEnd );

            if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Getting object named [" + mapKeyName + "] from Map container." );

            // Get object from map.
            target = ((Map)target).get( mapKeyName );

            if ( target == null )
            {
                throwMessageException( "ERROR: MessageObject: No Map container value named ["
                                          + mapKeyName + "] is available." );
            }

            // Adjust name to just contain the sub-component name.
            name = name.substring( mapKeyNameEnd + 1 );
        } // End - if map.
        else  // If the internal object is an ordered collection
        if ( target instanceof List )
        {
            // Extract data-access name from argument passed-in.
            int indexEnd = name.indexOf( MessageProcessorContext.DATA_ACCESS_NAME_DELIMITER );

            // If the name is a simple (non-compound/dotted-text name ...
            if ( indexEnd == -1 )
            {
                index = getIndex( name );

                if ( (index == INVALID_LOC) || (index > ((List)target).size()) )
                {
                    throwMessageException( "ERROR: MessageObject: Index ["
                                              + name + "] is out of List container index range." );
                }

                if ( index == ((List)target).size() )
                    ((List)target).add( value );
                else
                {
                    Object previous = ((List)target).set( index, value );

                    if ( previous != null )
                    {
                        if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                            Debug.log( Debug.MSG_DATA, "Value replaces previous List container value of type ["
                                       + previous.getClass().getName() + "]." );
                    }
                }

                return;  // We're done if name doesn't contain sub-component names.
            }

            // Extract map access key from the rest of the name.
            index = getIndex( name.substring( 0, indexEnd ) );

            if ( (index == INVALID_LOC) || (index >= ((List)target).size()) )
            {
                throwMessageException( "ERROR: MessageObject: Index ["
                                          + name + "] is out of List container index range." );
            }

            if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Getting object at index [" + index + "] from List container." );

            // Get object from map.
            target = ((List)target).get( index );

            if ( target == null )
            {
                throwMessageException( "ERROR: MessageObject: No List container value is available at index ["
                                          + index + "]." );
            }

            // Adjust name to just contain the sub-component name.
            name = name.substring( indexEnd + 1 );
        } // End - if list.


        // Set node value on target.
        try
        {
            if ( !(value instanceof String) )
            {
                throwMessageException( "ERROR: XML node value must be of type String, not ["
                                          + value.getClass().getName() + "]." );
            }

            if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Value in set operation is of type ["
                           + target.getClass().getName() + "]." );

            Document doc = null;

            if ( target instanceof Document )
                doc = (Document)target;
            else
            if ( target instanceof String )
            {
                XMLMessageParser p = new XMLMessageParser( (String)target );

                doc = p.getDocument( );

                // A performance optimization:  If we parse an XML stream into a DOM tree,
                // replace the string representation with its DOM equivalent to prevent re-parsing.
                if ( mapKeyName != null )
                    ((Map)object).put( mapKeyName, doc );
                else
                if ( index != INVALID_LOC )
                    ((List)object).set( index, doc );
                else
                    set( doc );
            }
            else
            {
                // If value isn't a String or a DOM Document, indicate error to caller.
                throwMessageException( "ERROR: Can't set node value on non-XML object of type ["
                                               + target.getClass().getName() + "]." );
            }

            XMLMessageGenerator gen = new XMLMessageGenerator( doc );

            gen.setValue( name, (String)value );
        }
        catch ( Exception e )
        {
            throwMessageException( "ERROR: Could not set value on XML object:\n"
                                           + e.toString() );
        }
    }


    /**
     * Get a named sub-object from the internal object.  The name is in dotted-text
     * format.
     *
     * @param  name  Name of item (delimited text for nested value).
     *
     * @return  Named object's value, or null if not found.
     */
    protected final Object getImplementation ( String name )
    {
        return( getImplementation( name, true ) );
    }


    /**
     * Get a named sub-object (possibly with a value) from the internal object.
     * The name is in dotted-text format.
     *
     * @param  name  Name of item (delimited text for nested value).  
     *
     * @param checkForValue  If 'true' test checks for node with a value attribute.  If 'false',
     *                       node existence is sufficient.
     *
     * @return  Named object's value, or null if not found.
     */
    protected final Object getImplementation ( String name, boolean checkForValue )
    {
        name = MessageProcessorBase.getInputName( name );

        // If the internal object isn't available, or there's no name, return null.
        if ( (name == null) || (object == null) )
            return null;

        String mapKeyName = null;
        int index         = INVALID_LOC;

        Object source = object;

        // If the internal object is a map.
        if ( source instanceof Map )
        {
            // Extract data-access name from argument passed-in.
            int mapKeyNameEnd = name.indexOf( MessageProcessorContext.DATA_ACCESS_NAME_DELIMITER );
            
            // If the name isn't dotted-text, use the entire name as the map key.
            if ( mapKeyNameEnd == -1 )
            {
                if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Getting object named [" + name + "] from map." );

                return( ((Map)source).get( name ) );
            }

            // Extract map access key from the rest of the name.
            mapKeyName = name.substring( 0, mapKeyNameEnd ); 
            
            // Get object from map.
            source = ((Map)source).get( mapKeyName );
            
            if ( source == null )
                return null;

            // Adjust name to just contain the sub-component name.
            name = name.substring( mapKeyNameEnd + 1 );
        } // End - if map.
        else  // If the internal object is an ordered collection.
        if ( source instanceof List )
        {
            // Extract data-access name from argument passed-in.
            int indexEnd = name.indexOf( MessageProcessorContext.DATA_ACCESS_NAME_DELIMITER );
            
            // If the name is a simple (non-compound/dotted-text name ...
            if ( indexEnd == -1 )
            {
                index = getIndex( name );

                if ( (index == INVALID_LOC) || (index >= ((List)source).size()) )
                    return null;

                if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                    Debug.log( Debug.MSG_DATA, "Getting object at location [" + index + "] from list." );

                return( ((List)source).get( index ) );
            }

            // Extract map access key from the rest of the name.
            index = getIndex( name.substring( 0, indexEnd ) );
            
            if ( (index == INVALID_LOC) || (index >= ((List)source).size()) )
                return null;

            if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Getting object at index [" + index + "] from List container." );
            
            // Get object from map.
            source = ((List)source).get( index );
            
            if ( source == null )
                return null;
            
            // Adjust name to just contain the sub-component name.
            name = name.substring( indexEnd + 1 );
        } // End - if list.


        // Get node value from object.
        try
        {
            XMLMessageParser p = null;
        
            if ( source instanceof Document )
                p = new XMLMessageParser( (Document)source );
            else
            if ( source instanceof String )
            {
                p = new XMLMessageParser( (String)source );

                // A performance optimization:  If we parse an XML stream into a DOM tree,
                // replace the string representation with its DOM equivalent to prevent re-parsing.
                if ( mapKeyName != null )
                    ((Map)object).put( mapKeyName, p.getDocument() );
                else
                if ( index != INVALID_LOC )
                    ((List)object).set( index, p.getDocument() );
                else
                    set( p.getDocument() );
            }
            else
            {
                Debug.log( Debug.ALL_WARNINGS, "WARNING: Can't get XML node value from object of type [" 
                           + source.getClass().getName() + "]." );

                return null;
            }
            
            if ( p.exists( name ) )
            {
                if ( checkForValue )
                    return( p.getValue( name ) );
                else
                    return( p.getNode( name ) );
            }
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, e.toString() );
        }
        
        return null;
    }


    /**
     * Get integer equivalent of access key value, if possible.
     *
     * @param  accessKey  Candidate access key to convert to integer
     *
     * @return  Non-negative integer value, 
     *          or INVALID_LOC if not an integer
     */
    protected final int getIndex ( String accessKey )
    {
        try
        {
            int loc = Integer.parseInt( accessKey );
            
            if ( loc < 0 )
                return INVALID_LOC;
            else
                return loc;
        }
        catch ( Exception e )
        {
            return INVALID_LOC;
        }
    }
    
    
    /**
     * Logs the error message argument and throws a processing exception.
     *
     * @exception  ProcessingException  Always thrown with error message argument.
     */
    protected final void throwProcessingException ( String errMsg ) throws ProcessingException
    {
          Debug.log( Debug.ALL_ERRORS, errMsg );

          throw new ProcessingException( errMsg );
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

    // Invalid index location for list.
    private static final int INVALID_LOC = -1;


    /**
     * Data structure to store information that can be uniquely identified by
     * a data-access name
     */
    private Object object = null;

}

/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.spi.common.driver;

import java.util.*;
import java.sql.Connection;
import org.w3c.dom.*;

import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;

import com.nightfire.common.*;


/**
 * The context object object that is passed on to every
 * MessageProcessor.process() method call as the first argument
 * (adapters, transformers, communications, etc).
 */
public class MessageProcessorContext
{
    /**
     * Create a message-processor context object.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    public MessageProcessorContext ( ) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating MessageProcessorContext ..." );
        
        map = new HashMap( );
    }


    /**
     * Configure the context as the transaction owner (or not).
     *
     * @param  owner  Boolean indicating whether this context owns the transaction
     *                or not.
     *
     * @return  Previous transaction owner flag value.
     */
    public boolean setTransactionOwner ( boolean owner )
    {
        boolean oldValue = ownsTransaction;

        ownsTransaction = owner;

        if ( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            Debug.log( Debug.DB_STATUS, "Context owns transaction? [" + ownsTransaction + "]." );

        return oldValue;
    }


    /**
     * Has the current context acquired the connection?
     *
     * @return  Flag indicating whether connection has been acquired.
     *          'true' if the current message processor context has already acquired a db connection.
     *          'false' if the current message processor context has not acquired a db connection.
     */
    public boolean hasAcquiredDBConnection ( )
    {
        return ((dbConn == null) ? false : true );
    }


    /**
     * Is the current context the transaction owner?
     *
     * @return  Flag indicating transaction ownership.
     */
    public boolean isTransactionOwner ( )
    {
        return ownsTransaction;
    }


    /**
     * Piggy-back on the parent context's transaction.
     *
     * @param  parent  Parent context to extract database connection from.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    public void inheritParentTransaction ( MessageProcessorContext parent ) throws ProcessingException
    {
        if ( parent == null  )
        {
            throwProcessingException( "ERROR: Attempt was made to inherit transaction from null parent context." );
        }

        if ( parent == this )
        {
            throwProcessingException( "ERROR: Attempt was made to inherit a context's own transaction." );
        }

        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log( Debug.DB_STATUS, "Child context now inheriting transaction from parent context." );

        // Get the database connection from the parent context and place it on the child context.
        setDBConnection( parent.getDBConnection() );

        // If we're using the parent transaction, we don't own it.
        setTransactionOwner( false );
    }


    /**
     * Set the current context's map using the parent's map.
     *
     * @param  parent  Parent context to extract data from.
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    public void inheritParentData ( MessageProcessorContext parent ) throws ProcessingException
    {
        if ( parent == null  )
        {
            throwProcessingException( "ERROR: Attempt was made to inherit data from null parent context." );
        }
        
        if ( parent == this )
        {
            throwProcessingException( "ERROR: Attempt was made to inherit a context's own data." );
        }

        if(Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log( Debug.DB_STATUS, "Child context now inheriting data from parent context." );
        
        map = parent.getData( );
    }


    /**
     * Release the database connection associated with the context.
     */
    public void releaseDBConnection ( )
    {
        releaseDBConnection( true );
    }


    /**
     * Release the database connection associated with the context.
     *
     * @param  onlyIfOwner  Flag indicating that connection should only
     *                      be released if the current context owns it.
     */
    protected void releaseDBConnection ( boolean onlyIfOwner )
    {
        if ( dbConn != null )
        {
            // If this context doesn't own the transaction, and the flag indicates that
            // it should only release connections that it owns, log a warning and return.
            if ( !ownsTransaction && onlyIfOwner )
            {
                Debug.log( Debug.ALL_WARNINGS, "WARNING: Ignoring attempt to release a database connection unowned by the context." );
                
                return;
            }

            try
            {
                if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
                    Debug.log( Debug.DB_STATUS, "Releasing message-processor context's database connection ["
                               + dbConn + "]." );

                DBConnectionPool.getInstance().releaseConnection( dbConn );
                
                dbConn = null;
            }
            catch ( Exception e )
            {
                Debug.log( Debug.ALL_ERRORS, e.toString() );
            }
        }
    }


    /**
     * Get the database connection associated with the context.
     *
     * @return  Connection object.
     */
    public Connection getDBConnection ( ) throws ProcessingException
    {
        if ( dbConn == null )
        {
            try
            {
                setDBConnection( DBConnectionPool.getInstance().acquireConnection() );
            }
            catch( Exception e )
            {
                throwProcessingException( "ERROR: Failed to set database connection on message-processor context:\n"
                                          + e.toString() );
            }
        }

        if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
            Debug.log( Debug.DB_STATUS, "Returning message-processor context's database connection ["
                       + dbConn + "] to caller." );

        return dbConn;
    }


    /**
     * Sets the database connection on the context.
     *
     * @param  con  Database connection object.
     *
     * @exception  ProcessingException  Thrown if database connection is invalid.
     */
    protected void setDBConnection ( Connection con ) throws ProcessingException
    {
        if ( con == null  )
        {
            throwProcessingException( "ERROR: Attempt was made to set database connection on message-processor context with a null value." );
        }

        if ( con == dbConn  )
        {
            throwProcessingException( "ERROR: Attempt was made to set context's database connection with one it already has." );
        }

        // Release any pre-existing connections, if we own them.
        releaseDBConnection( true );

        dbConn = con;

        if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
            Debug.log( Debug.DB_STATUS, "Setting message-processor context's database connection [" + dbConn + "]." );
    }



    /**
     * Commits all activity against the context's database connection to the database.
     *
     * @exception  ProcessingException  Thrown if commit fails.
     */
    protected void commit ( ) throws ProcessingException
    {
        if ( !ownsTransaction )
        {
            if(Debug.isLevelEnabled(Debug.DB_STATUS))
                Debug.log( Debug.DB_STATUS, "Returning without committing transaction, since this context doesn't own it." );

            return;
        }

        try
        {
            if ( dbConn != null )
            {
                if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
                    Debug.log( Debug.DB_STATUS, "Committing all database activity against message-processor context connection [" 
                               + dbConn + "]." );

                DBConnectionPool.getInstance().commit( dbConn );
            }

        }
        catch( ResourceException re )
        {
            throwProcessingException( "ERROR: Commit against message-processor context's database connection failed:\n"
                                      + re.getMessage() );
        }
        finally
        {
            releaseDBConnection( false );
        }
    }


    /**
     * Roll back all activity against the context's database connection to the database.
     *
     * @exception  ProcessingException  Thrown if rollback fails.
     */
    protected void rollback ( ) throws ProcessingException
    {
        if ( !ownsTransaction )
        {
            if(Debug.isLevelEnabled(Debug.DB_STATUS))
                Debug.log( Debug.DB_STATUS, "Returning without rolling-back transaction, since this context doesn't own it." );

            return;
        }

        try
        {
            if ( dbConn != null )
            {
                if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
                    Debug.log( Debug.DB_STATUS, "Rolling-back all database activity against message-processor context connection ["
                               + dbConn + "]." );

                DBConnectionPool.getInstance().rollback( dbConn );
            }
        }
        catch( ResourceException re )
        {
            throwProcessingException( "ERROR: Rollback against message-processor context's database connection failed:\n"
                                      + re.getMessage() );
        }
        finally
        {
            releaseDBConnection( false );
        }
    }


    /**
     * Locate the data-access object (message, context) indicated by the first token 
     * in the structured name and assign "value" to it.
     *
     * @param  name   Name of data-access object.
     *
     * @param  value  Value to be assigned to named item.
     *
     * @exception  MessageException Thrown if named item value cannot be set
     *
     * @exception  ProcessingException Thrown on configuration errors.
     */
    public void set ( String name, Object value ) throws ProcessingException, MessageException
    {
      if ( ! ( StringUtils.hasValue (name) ) )
      {
          throwProcessingException( "ERROR: MessageProcessorContext: " +
                                    "set input name is null" );
      }

      if ( value == null )
      {
          throwMessageException( "ERROR: MessageProcessorContext: " +
                                    "set input value is null" );
      }

      setImplementation( name, value );

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

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: Setting value in context Map container with name [" + name + "]." );

      // If a target object isn't available, throw an exception;
      Object target = null;
      String mapKeyName = null;

      //Value is to be set in HashMap directly.
      // Extract data-access name from argument passed-in.
      int mapKeyNameEnd = name.indexOf( DATA_ACCESS_NAME_DELIMITER );
      if ( mapKeyNameEnd == -1 )
      {
        Object origValue = map.put( name, value );

        if ( origValue != null )
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: Original value [" +
                            origValue.toString( ) + "] of [" + name + "] is replaced by the new value [" +
                            value.toString ( ) + "]" );
        }//Value is being replaced

      }
      else
      //The name is not of form "<mapKeyName>". If it is followed by delimited '.'
      //sub components, then it implies that the target object is an XML object.
      {

        // Extract map access key from the rest of the name.
        mapKeyName = name.substring( 0, mapKeyNameEnd );

        if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
          Debug.log( Debug.MSG_DATA, "Getting object named [" + mapKeyName + "] from Map container." );

        // Get object from map.
        target = map.get( mapKeyName );

        if ( target == null )
        {
            throwMessageException( "ERROR: MessageObject: No Map container value named ["
                                          + mapKeyName + "] is available." );
        }

        // Adjust name to just contain the sub-component name.
        name = name.substring( mapKeyNameEnd + 1 );

        // Set node value on target.
        try
        {
            if ( !(value instanceof String) )
            {
                throwMessageException( "ERROR: XML node value must be of type String, not ["
                                          + value.getClass().getName() + "]." );
            }

            if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Target in set operation is of type ["
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
                map.put( mapKeyName, doc );
            }
            else
            {
                // If value isn't a String or a DOM Document, indicate error to caller.
                throwMessageException( "ERROR: Can't set node value on non-XML object of type ["
                                               + target.getClass().getName() + "]." );
            }

            XMLMessageGenerator gen = new XMLMessageGenerator( doc );

            if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, "Setting value [" + value +
                          "] at XML location [" + name + "] in context map with key [" +
                           mapKeyName + "]." );

            gen.setValue( name, (String)value );
        }
        catch ( Exception e )
        {
            throwMessageException( "ERROR: Could not set value on XML object:\n"
                                           + e.toString() );
        }
      }//if this is not a value directly to set on the map.

    }//setImplementation

    /**
     * Get the value associated with the named item.  Structured item names are
     * separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name is
     * used for further processing of this data-access object.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access.
     *
     * @return  Named-item's value.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception ProcessingException Thrown on configuration errors.
     */
    public Object get ( String name ) throws MessageException, ProcessingException
    {
        Object result = getImplementation( name );

        if ( result == null )
        {
            throwMessageException( "ERROR: Unable to get object named [" + name + "] from context." );
        }

        return result;
    }//get


    /**
     * Get the value associated with the named item.  Structured item names are
     * separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name is
     * used for further processing of this data-access object.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  Named-item's value, or null if not available.
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception ProcessingException Thrown on configuration errors.
     */
    protected Object getImplementation ( String name ) throws MessageException, ProcessingException
    {
        return( getImplementation( name, true ) );
    }

    /**
     * Get the node (possibly with a value) associated with the named item.  Structured item
     * names are separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name is used for further
     * processing of this data-access object.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @param checkForValue  If 'true' test checks for node with a value attribute.  If 'false',
     *                       node existence is sufficient.
     *
     * @return  Named-item's value, or null if not available.
     *
     * @exception  ProcessingException  Thrown on configuration errors.
     *
     * @exception  MessageException     Thrown if item cannot be accessed.
     */
    protected Object getImplementation ( String name, boolean checkForValue )
        throws ProcessingException, MessageException
    {
      if ( !StringUtils.hasValue(name) )
      {
          throwProcessingException( "ERROR: MessageProcessorContext: " +
                                    "Input name argument to get is null." );
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: Getting value for [" + name + "]." );

      Object value = null;

      // Extract data-access name from argument passed-in.
      int daNameEnd = name.indexOf( DATA_ACCESS_NAME_DELIMITER );

      if ( daNameEnd == -1 )
      {
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: Returning entire Map container entry for name [" + name + "]." );

        value = map.get ( name );
      }
      else  //Value within an XML is required
      {
        String daName = name.substring ( 0, daNameEnd );
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: Using Map container key [" + daName + "] to locate XML document." );
        value = map.get ( daName );

        String valueName = name.substring ( daNameEnd + 1 );
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: XML sub-component value to be fetched is named [" + valueName + "]." );

        try
        {
            if ( value != null )
            {
                //Create a parser to extract some node value from the XML at daName
                XMLMessageParser parser = null;
                
                if ( value instanceof String )
                    parser = new XMLMessageParser ( ( String )value );
                else
                if ( value instanceof Document )
                    parser = new XMLMessageParser ( ( Document )value );
                else
                {
                    Debug.log( Debug.ALL_WARNINGS, "WARNING: Attempt to extract XML node value from invalid type [" 
                               + value.getClass().getName() + "]." );
                }
                
                if ( (parser != null) && parser.exists( valueName ) )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: Fetching value from XML" );
                        
                    if ( checkForValue )
                        value = parser.getValue( valueName );
                    else
                        value = parser.getNode( valueName );
                }
                else
                    value = null;
            }
        }//try
        catch ( MessageException me )
        {
            throwMessageException( "ERROR: MessageProcessorContext: " +
                                      "Could not create XMLMessageParser from XML located at daName. " +
                                      me.getMessage ( ) );
        }//catch
      }//Access value within an XML

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: [" +
                      name + "]'s value is [" + value + "]");

      return value;
    }//get


    /**
     * Get the value associated with the named item and return its string-equivalent.  Structured 
     * item names are separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name is used for further
     * processing of this data-access object.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return Named-item's value in String form
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown if any other processing error occurs.
     */
    public String getString ( String name ) throws MessageException, ProcessingException
    {
      Object tempObj = get ( name );
      String tempStr = null;
      try
      {
          tempStr = Converter.getString ( tempObj );
      }
      catch ( FrameworkException e )
      {
          throwMessageException ( e.getMessage ( ) );
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: [" +
                      name + "]'s value is [" + tempStr + "]");
      return tempStr;
    }//getString

    /**
     * Get the value associated with the named item and return its DOM-equivalent.  Structured
     * item names are separated by delimiter values ('.').  If the first name in a
     * structured name begins with the character '@', the corresponding data-access
     * object (message, context) is looked up, and the remaining part of the name is
     * used for further processing of this data-access object.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  Named-item's value in in XML DOM Document form
     *
     * @exception  MessageException  Thrown if item can't be accessed.
     *
     * @exception  ProcessingException  Thrown if any other processing error occurs
     */
    public Document getDOM ( String name ) throws MessageException, ProcessingException
    {
      Object tempObj = get ( name );
      Document tempDocument = null;
      try
      {
          tempDocument = Converter.getDOM ( tempObj );
      }
      catch ( FrameworkException e )
      {
          throwMessageException ( e.getMessage ( ) );
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: [" +
                      name + "]'s value is [" + tempDocument + "]");
      return tempDocument;
    }//getDOM


    /**
     * Test to see if there is a value associated with the named item.  Structured
     * item names are separated by delimiter values ('.').
     * If the first name in a structured name begins with the character '@',
     * the corresponding data-access object (message, context) is looked up, and the remaining part of
     * the name is passed to it for further processing.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  Named-item's value.
     */
    public boolean exists ( String name )
    {
        return( exists( name, true ) );
    }


    /**
     * Test to see if there is a node (possibly with a value) associated with the named item.  
     * Structured item names are separated by delimiter values ('.').
     * If the first name in a structured name begins with the character '@',
     * the corresponding data-access object (message, context) is looked up, and the remaining part of
     * the name is passed to it for further processing.
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @param checkForValue  If 'true' test checks for node with a value attribute.  If 'false',
     *                       node existence is sufficient.
     *
     * @return  Named-item's value.
     */

    public boolean exists ( String name, boolean checkForValue )
    {
      boolean retBool = true;

      try
      {
          //Get the value for name
          Object value = getImplementation( name, checkForValue );
          
          if ( value == null )
              retBool = false;
      }
      catch ( FrameworkException pe )
      {
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: " + pe.getMessage ( ) );
        retBool = false;
      }

      if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log ( Debug.MSG_STATUS, "MessageProcessorContext: Value for [" + name +
                      "] exists is [" + retBool + "]" );

      return retBool;
    }//exists


    /**
     * Get the data container associated with this context.
     *
     * @return  The map containing the context's data.
     */
    protected Map getData ( )
    {
        return map;
    }


    /**
     * Get a description of the context in human-readable form.
     *
     * @return  Human readable string representation of message processor context.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        boolean dbConAvailable = true;

        if ( dbConn == null )
            dbConAvailable = false;

        sb.append( "\n****************** MESSAGE PROCESSOR CONTEXT DESCRIPTION: ******************\n" );
        sb.append( "CONTEXT TYPE: [" );
        sb.append( "MessageProcessorContext" );
        sb.append( "]\n" );
        sb.append( "DB CONNECTION AVAILABLE? [" );
        sb.append( dbConAvailable );
        sb.append( "]\n\n" );
        sb.append( "AVAILABLE KEYS AND VALUES:\n\n" );
        sb.append( "[");
        sb.append( PropUtils.suppressPasswords( map.toString() ) );
        sb.append( "]\n\n");

        sb.append( "\n*******************************************************************************\n" );

        return( sb.toString() );
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

    /**
     * Valid start character for a data-access name
     */
    public static final char DATA_ACCESS_NAME_PREFIX = '@';

    /**
     * Delimiter for data-access name and its sub components
     */
    public static final char DATA_ACCESS_NAME_DELIMITER = '.';

    /**
     * Data structure to store information that can be uniquely identified by
     * a data-access name
     */
    private Map map = null;

    /**
     * Database connection to enable transactional logging for processors
     */
    private Connection dbConn = null;

    // Flag indicating whether or not this context owns the database transaction.
    // Default is 'true' - i.e., This context instance owns the transaction.
    private boolean ownsTransaction = true;
}

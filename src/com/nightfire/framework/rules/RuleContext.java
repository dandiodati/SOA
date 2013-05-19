/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.rules;

import java.util.*;
import java.sql.*;
import java.sql.Connection;

import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;

/**
 * The context object  that is passed on to every
 * RuleEvaluator object call as the first argument
 * (adapters, transformers, communications, etc).
 */
public class RuleContext
{
    /**
     * Create a Rule context object.
     */
    public RuleContext ( ) 
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating RuleContext ..." );
        
        map = new HashMap( );
    }

    /**
     * Is the current context the Connection owner?
     *
     * @return  Flag indicating Connection ownership.
     */
    public boolean isDBConnectionOwner ( )
    {
        return ownsDBConnection;
    }


    /**
     * Release the database connection associated with the context.
     */
    public void releaseDBConnection ( )
    {
        if ( dbConn != null )
        {
            // If this context doesn't own the Connection, and the flag indicates that
            // it should only release connections that it owns, log a warning and return.
            if ( !ownsDBConnection )
            {
                Debug.log( Debug.DB_STATUS, "Ignoring attempt to release a database connection unowned by the Rule context." );
                
                return;
            }

            try
            {
                if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
                    Debug.log( Debug.DB_STATUS, "Releasing Rule context's database connection ["
                               + dbConn + "]." );

                DBConnectionPool.getInstance().releaseConnection( dbConn );
                
                dbConn = null;
            }
            catch ( FrameworkException e )
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
    public Connection getDBConnection ( ) throws FrameworkException
    {
        if ( dbConn == null )
        {
            try
            {
                dbConn = DBConnectionPool.getInstance().acquireConnection();
				ownsDBConnection = true;
            }
            catch( Exception e )
            {
                throw new FrameworkException( "ERROR: Failed to set database connection on Rule context:\n"
                                          + e.toString() );
            }
        }

        if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
            Debug.log( Debug.DB_STATUS, "Returning Rule context's database connection ["
                       + dbConn + "] to caller." );

        return dbConn;
    }


    /**
     * Sets the database connection on the context.
     *
     * @param  con  Database connection object.
     */
    public void setDBConnection ( Connection con ) 
    {
        if ( con != null  )
        {
			dbConn = con;
			if ( Debug.isLevelEnabled ( Debug.DB_STATUS ) )
				Debug.log( Debug.DB_STATUS, "Setting Rule context's database connection [" + dbConn + "]." );
		}
    }

    /**
     * Set the name and value in the map.
     *
     * @param  name   Name of data-access object.
     *
     * @param  value  Value to be assigned to named item.
     *
     * @exception  FrameworkException Thrown on configuration errors.
     */
    public void set ( String name, Object value ) throws FrameworkException
    {
      if ( ! ( StringUtils.hasValue (name) ) )
      {
          throw new FrameworkException( "ERROR: RuleContext: " +
                                    "set input name is null" );
      }

      if ( value == null )
      {
          throw new FrameworkException( "ERROR: RuleContext: " +
                                    "set input value is null" );
      }

      map.put( name, value );

    }

    /**
     * Get the value associated with the named item from the map.  
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access.
     *
     * @return  Named-item's value.
     */
    public Object get ( String name ) {
        Object result = map.get( name );
	    return result;
    }//get



    /**
     * Test to see if there is a value associated with the named item in the map.  
     *
     * @param  name  Name of item (delimited text for nested value).  The data-access
     *   object is the token following the starting '@' character. The remaining part of the
     *   name indicates components within the item accessible by the data-access object.
     *
     * @return  whether Named-item's value exists.
     */
    public boolean exists ( String name )
    {
        return( map.containsKey( name ) );
    }



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
     * Cleanup the rule context object
     *
     */
    public void cleanup ( )
    {
		//Release the DBConnection from RuleContext object if owned by it.
		releaseDBConnection();
        // clean up the map.
        if(map!=null)
             map.clear();
        map=null;
    }



    /**
     * Get a description of the context in human-readable form.
     *
     * @return  Human readable string representation of Rule context.
     */
    public String describe ( )
    {
        StringBuffer sb = new StringBuffer( );

        boolean dbConAvailable = true;

        if ( dbConn == null )
            dbConAvailable = false;

        sb.append( "\n****************** RULE CONTEXT DESCRIPTION: ******************\n" );
        sb.append( "CONTEXT TYPE: [" );
        sb.append( "RuleContext" );
        sb.append( "]\n" );
        sb.append( "DB CONNECTION AVAILABLE? [" );
        sb.append( dbConAvailable );
        sb.append( "]\n\n" );
        sb.append( "AVAILABLE KEYS AND VALUES:\n\n" );
        sb.append( "[");
        sb.append( map.toString() );
        sb.append( "]\n\n");

        sb.append( "\n*******************************************************************************\n" );

        return( sb.toString() );
    }

    /**
     * Data structure to store information that can be uniquely identified by
     * a data-access name
     */
    private HashMap map = null;

    /**
     * Database connection to be shared between variuos rules.
     */
    private Connection dbConn = null;

    // Flag indicating whether or not this context owns the database transaction.
    // Default is 'true' - i.e., This context instance owns the transaction.
    private boolean ownsDBConnection = false;

}

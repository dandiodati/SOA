/**
 *
 * Copyright (c) 2005 NeuStar, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.rules;

import java.util.*;
import java.sql.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.cache.*;

/**
 * Utility used to determine whether a named rule is disabled for 
 * a particular customer.
 */
public class DisabledRules implements CachingObject
{
    /**
     * Test to see if the named rule is disabled for the current customer.
     *                           
     * @param ruleClassName  The fully-qualified (package + class) name of the rule to check.
     *         
     * @return  'true' if rule is disabled, otherwise 'false'.
     */
    public static boolean isDisabled ( String ruleClassName )
    {
        try
        {
            return( isDisabled( CustomerContext.getInstance().getCustomerID(), ruleClassName ) );
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
        }

        return false;
    }


    /**
     * Test to see if the named rule is disabled for the indicated customer.
     *                           
     * @param customerId  Domain against which rule should be checked.
     * @param ruleClassName  The fully-qualified (package + class) name of the rule to check.
     *         
     * @return  'true' if rule is disabled, otherwise 'false'.
     */
    public static boolean isDisabled ( String customerId, String ruleClassName )
    {
        // NOTE: Added to remove customer-awareness to code that was previously customer-aware.
        customerId = CustomerContext.DEFAULT_CUSTOMER_ID;

        try
        {
            Set disabledRuleNames = getCustomerDisabledRules( customerId );

            if ( disabledRuleNames.contains( ruleClassName ) )
            {
                if ( Debug.isLevelEnabled( Debug.RULE_EXECUTION ) )
                    Debug.log( Debug.RULE_EXECUTION, "NOTE: Rule [" + ruleClassName 
                               + "] is disabled for customer [" + customerId + "]." );

                return true;
            }
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
        }

        return false;
    }


    /**
     * Method invoked by the cache-flushing infrastructure
     * to indicate that the cache should be emptied.
     *
     * @exception  FrameworkException  If cache cannot be cleared.
     */
    public void flushCache ( ) throws FrameworkException
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Flushing disabled-rules cache ..." );

        synchronized ( perCustomerDisabledRuleContainer )
        {
            perCustomerDisabledRuleContainer.clear( );
        }
    }


    /**
     * Get the set of disabled rule names for the indicated customer.
     *                           
     * @param customerId  Domain against which rule should be checked.
     *         
     * @return  Set of disabled rule names.
     *         
     * @exception FrameworkException  Thrown on any errors.
     */
    private static Set getCustomerDisabledRules ( String customerId ) throws FrameworkException
    {
        synchronized ( perCustomerDisabledRuleContainer )
        {
            Set s = (Set)perCustomerDisabledRuleContainer.get( customerId );

            // The absence of the set indicates that this customer's disabled rules haven't
            // been loaded from the database yet.
            if ( s == null )
            {
                // Load set of rule names from database.
                s = loadCustomerDisabledRules( customerId );

                // Remember just-loaded set in cache for next time.
                perCustomerDisabledRuleContainer.put( customerId, s );

                if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Disabled-rules cache contents:\n" 
                               + perCustomerDisabledRuleContainer.toString() );
            }

            return s;
        }
    }


    /**
     * Load the set of disabled rule names for the indicated customer
     * from the database.
     *                           
     * @param customerId  Domain against which rule should be checked.
     *         
     * @return  Set of disabled rule names.
     *         
     * @exception FrameworkException  Thrown on any errors.
     */
    private static Set loadCustomerDisabledRules ( String customerId )
    {
        Set ruleNames = new HashSet( );

        // NOTE: Added to remove customer-awareness to code that was previously customer-aware.
        // String sql = "SELECT RuleName FROM DisabledBusinessRules WHERE customerID = ? AND Status IN ( 'off', 'Off', 'OFF' )";
        String sql = "SELECT RuleName FROM DisabledBusinessRules WHERE Status IN ( 'off', 'Off', 'OFF' )";

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Loading disabled-rules into cache for customer-id [" + customerId + "] ..." );

        try
        {
            PreparedStatement pstmt = null;
            Connection dbConn = null;

            try
            {
                dbConn = DBConnectionPool.getInstance().acquireConnection( );

                if ( Debug.isLevelEnabled( Debug.DB_DATA ) )
                    Debug.log( Debug.DB_DATA, "Executing SQL:\n" + sql );

                pstmt = dbConn.prepareStatement( sql );

                // NOTE: Added to remove customer-awareness to code that was previously customer-aware.
                // pstmt.setString( 1, customerId );

                ResultSet rs = pstmt.executeQuery( );

                while ( rs.next() )
                    ruleNames.add( rs.getString( 1 ) );
            }
            finally
            {
                if ( pstmt != null )
                    pstmt.close( );

                DBConnectionPool.getInstance().releaseConnection( dbConn );
            }
        }
        catch ( Exception e )
        {
            Debug.warning( "Could not load disabled rule information for customer [" 
                           + customerId + "] from the database:\n" + e.toString() );
        }

        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Disabled-rules for customer-id [" 
                       + customerId + "]:\n" + ruleNames.toString() );

        return( Collections.unmodifiableSet( ruleNames ) );
    }


    // A single private instance of this class will be created to assist in cache flushing.
    private DisabledRules ( )
    {
        try
        {
            CacheManager.getRegistrar().register( this );
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
        }
    }


    // Used exclusively for cache flushing.
    private static DisabledRules flusher = new DisabledRules( );

    // A map of set objects indicating which rules are disabled (by name).
    // The access key for the map is the customer-id value.
    // This is also supplies the synchronization lock used when managing 
    // the data.
    private static Map perCustomerDisabledRuleContainer = new HashMap( );
}

/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */
package com.nightfire.framework.util;


import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.repository.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;


/**
 * Utility to get database configuration from commonSecurityDBConfig configuration file.
 */
public class SecurityDBConfigUtils extends CommonConfigUtils
{

   /**
     * The meta in repository that contains the common security database configuration.
     */
    public static final String COMMON_SECURITY_DB_CONFIG_META = "commonSecurityDBConfig";

    private SecurityDBConfigUtils()
    {
        super();
    }

    /**
     * Get the configured value value for a required configuration item.
     * If the item is not configured, a FrameworkException will be thrown.
     *
     * @param  itemName  Name of the configuration item whose value is to be retrieved.
     *
     * @return String The value for the given configuration item.
     *
     * @exception  com.nightfire.framework.util.FrameworkException  Thrown if the item is not configured.
     */
    public static String getRequiredValue ( String itemName ) throws FrameworkException
    {
        return getValue(itemName, true);
    }


    /**
     * Get the configured value for an optional configuration item.
     * If the item is not configured, null will be returned.
     *
     * @param  itemName  Name of the configuration item whose value is to be retrieved.
     *
     * @return String The value for the given configuration item.
     *
     * @exception  com.nightfire.framework.util.FrameworkException  Thrown if server name cannot be formed.
     */
    public static String getValue ( String itemName ) throws FrameworkException
    {
        return getValue(itemName, false);
    }


    /**
     * Get the all configured items and their values from the common secrity database configuration.
     *
     * @return  A map that contains all configured items and their values.
     *
     * @exception  com.nightfire.framework.util.FrameworkException  Thrown on errors.
     */
    public static Map getCommonConfigItems() throws FrameworkException
    {
        if ( securityConfigItems == null )
        {
            synchronized ( SecurityDBConfigUtils.class )
            {
                if ( securityConfigItems == null )
                {
                    String xmlDescription
                        = RepositoryManager.getInstance().getMetaData( COMMON_CONFIG_CATEGORY,
                                                                       COMMON_SECURITY_DB_CONFIG_META );

                    XMLMessageParser parser = new XMLMessageParser( xmlDescription );

                    Node[] items = XMLMessageBase.getChildNodes( parser.getDocument().getDocumentElement() );

                    securityConfigItems = new HashMap<String, String>( );

                    // Loop over configuration nodes.
                    for ( int Ix = 0;  Ix < items.length;  Ix ++ ) {
                        securityConfigItems.put( items[Ix].getNodeName(),
                                                          XMLMessageBase.getNodeValue( items[Ix] ) );
                    }

                    if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ) {
                        Debug.log(Debug.SYSTEM_CONFIG, "Common configuration values:\n"
                                   + PropUtils.suppressPasswords( securityConfigItems.toString() ) );
                    }    
                  }
            }
        }

        return securityConfigItems;
    }

    /**
     * Get the named value from the common configuration.
     *
     * @param  name  Name of item to retrieve.
     * @param  required  Flag indicating whether item is required or not.
     *
     * @return  Named item, or null if not found and required flag is false.
     *
     * @exception  com.nightfire.framework.util.FrameworkException  Thrown on errors.
     */
    protected static String getValue ( String name, boolean required ) throws FrameworkException
    {
    	String value = getValueSpecificToCurrentJVM(name);

    	if(!StringUtils.hasValue(value))
    	{
    		value = (String)getCommonConfigItems().get( name );
    	}

        if ( required && (value == null) )
        {
            throw new FrameworkException( "ERROR: Missing required common configuration item named ["
                                          + name + "]." );
        }

        return value;
    }
    
    /**
        * Return the database name.
        *
        * @return  The database name.
        *
        * @throw FrameworkException If error occurs.
        */
       public static String getDBName ( ) throws FrameworkException
       {
           return getValue(DB_NAME_PROP, true);
       }

       /**
        * Return the database user.
        *
        * @return  The database user.
        *
        * @throw FrameworkException If error occurs.
        */
       public static String getDBUser ( ) throws FrameworkException
       {
           return getValue(DB_USER_PROP, true);
       }

       /**
        * Return the database password password.
        *
        * @return  The database password.
        *
        * @throw FrameworkException If error occurs.
        */
       public static String getDBPassword ( ) throws FrameworkException
       {
           return getValue(DB_PASSWORD_PROP, true);
       }
    

    private static Map<String, String> securityConfigItems;
}
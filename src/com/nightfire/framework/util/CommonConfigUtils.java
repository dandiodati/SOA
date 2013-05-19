/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */
package com.nightfire.framework.util;


import java.util.*;
import java.io.File;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.repository.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;


/**
 * Common configuration-related utility functionality.
 */
public class CommonConfigUtils
{
    /**
     * The category in repository that contains the common configuration.
     */
    public static final String COMMON_CONFIG_CATEGORY = "commonConfig";

    /**
     * The meta in repository that contains the common configuration.
     */
    public static final String COMMON_CONFIG_META = "commonConfig";

    /**
     * Node in the common configuration giving the database driver name.
     */
    public static final String DB_DRIVER_PROP = DBInterface.DB_DRIVER_PROPERTY;

    /**
     * Node in the common configuration giving the database URL with the above JDBC driver.
     */
    public static final String DB_NAME_PROP = DBInterface.DB_NAME_PROPERTY;

    /**
     * Node in the common configuration giving the database user name.
     */
    public static final String DB_USER_PROP = DBInterface.DB_USER_PROPERTY;

    /**
     * Node in the common configuration giving the database user password.
     */
    public static final String DB_PASSWORD_PROP = DBInterface.DB_PASSWORD_PROPERTY;

    /**
     * Node in the common configuration giveing the CORBA server IP address.
     */
    public static final String CORBA_SERVER_ADDR_PROP = "ORBagentAddr";

    /**
     * Node in the common configuration giveing the CORBA server port.
     */
    public static final String CORBA_SERVER_PORT_PROP = "ORBagentPort";

    /**
     * Node in the common configuration giving the RMI registry server IP address.
     */
    public static final String RMI_SERVER_ADDR_PROP = "RMIServerAddr";

    /**
     * Node in the common configuration giving the RMI registry port.
     */
    public static final String RMI_SERVER_PORT_PROP = "RMIServerPort";

    /**
     * Node in the common configuration giving the URL that the J2EE application server is listening on
     */
    public static final String APPLICATION_SERVER_URL_PROP = "applicationServerURL";

    /**
     * Node in the common configuration giving the Initial Context Factory class to use for connecting to the J2EE application server JNDI service
     */
    public static final String INITIAL_CONTEXT_FACTORY_PROP = "initialContextFactoryName";

    /**
     * Property indicating whether Oracle Connection pool would be used.
     */
    public static final String USE_ORACLE_CONNECTION_POOL_PROP = "UseOracleConnPool";

    /**
     * Property indicating whether Oracle Connection pool would be used.
     */
     public static final String USE_COMMON_SECURITY_DATA_BASE_PROP = "UseCommonSecurityDB";

    /**
     * Sets how many connections are created in the oracle implicit connection cache at the time of initialization.
     */
    public static final String ORACLE_POOL_INIT_LIMIT_PROP = "OraclePoolInitLimit";

    /**
     * Sets the maximum number of connection instances the oracle connection cache can hold.
     */
    public static final String ORACLE_POOL_MAX_LIMIT_PROP = "OraclePoolMaxLimit";

    /**
     * While requesting a connection if there are already MaxLimit connections active
     * then waits for the specified number of seconds.
     */
    public static final String ORACLE_CONN_WAIT_TIME_OUT_PROP = "OracleConnWaitTimeout";

    /**
     * Sets the maximum time a physical connection can remain idle in a oracle connection cache.
     */
    public static final String ORACLE_CONN_INACTIVITY_TIME_OUT_PROP = "OracleConnInactivityTimeout";
    /**
     * Setting this property as 'true' causes the connection cache to test every connection
     * it retrieves against the underlying database.
     */
    public static final String ORACLE_VALIDATE_CONN_PROP = "OracleValidateConnection";

    /**
      * ONS configuration to support Oracle RAC.
     */
    public static final String ONS_CONFIG_FOR_ORACLE_RAC = "RACONSConfig";

    /**
     * Auto commit flag for oracle connections.
    */
   public static final String ORACLE_POOL_AUTO_COMMIT_FLAG = "OracleConnAutoCommit";

    /**
     * INSTALLROOT property name
     */
    private static final String INSTALLROOT_PROP = "INSTALLROOT";

    /**
     *  Check whether repository root is set or not.
     *  If not then set, set it with absolute path in case of install-root is in system property
     *  otherwise set it as relative path.
     */
    static
    {
        if(System.getProperty(RepositoryManager.NF_REPOSITORY_ROOT_PROP) == null) {

            if(System.getProperty(INSTALLROOT_PROP) != null)
               System.setProperty(RepositoryManager.NF_REPOSITORY_ROOT_PROP, System.getProperty(INSTALLROOT_PROP) + File.separator + RepositoryManager.URL_PREFIX);
            else
               System.setProperty(RepositoryManager.NF_REPOSITORY_ROOT_PROP, "." + File.separator + RepositoryManager.URL_PREFIX);

        }
    }

    protected CommonConfigUtils()
    {
    }

    /**
     * Get the configured value value for a required configuration item.
     * If the item is not configured, a FrameworkException will be thrown.
     *
     * @param  itemName  Name of the configuration item whose value is to be retrieved.
     *
     * @return String The value for the given configuration item.
     *
     * @exception  FrameworkException  Thrown if the item is not configured.
     */
    public static String getRequiredValue ( String itemName ) throws FrameworkException
    {
        return getValue(itemName, true);
    }


    /**
     * Get the configured value value for an optional configuration item.
     * If the item is not configured, null will be returned.
     *
     * @param  itemName  Name of the configuration item whose value is to be retrieved.
     *
     * @return String The value for the given configuration item.
     *
     * @exception  FrameworkException  Thrown if server name cannot be formed.
     */
    public static String getValue ( String itemName ) throws FrameworkException
    {
        return getValue(itemName, false);
    }


    /**
     * Get the all configured items and their values from the common configuration.
     *
     * @return  A map that contains all configured items and their values.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
    public static Map getCommonConfigItems() throws FrameworkException
    {
        if ( commonConfigItems == null )
        {
            synchronized ( CommonConfigUtils.class )
            {
                if ( commonConfigItems == null )
                {
                    String xmlDescription
                        = RepositoryManager.getInstance().getMetaData( COMMON_CONFIG_CATEGORY,
                                                                       COMMON_CONFIG_META );

                    XMLMessageParser parser = new XMLMessageParser( xmlDescription );

                    Node[] items = XMLMessageBase.getChildNodes( parser.getDocument().getDocumentElement() );

                    commonConfigItems = new HashMap( );

                    // Loop over configuration nodes.
                    for ( int Ix = 0;  Ix < items.length;  Ix ++ )
                        commonConfigItems.put( items[Ix].getNodeName(),
                                                          XMLMessageBase.getNodeValue( items[Ix] ) );

                    if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                    	Debug.log( Debug.SYSTEM_CONFIG, "Common configuration values:\n"
                                + PropUtils.suppressPasswords(commonConfigItems.toString() ));
                        
                }
            }
        }

        return commonConfigItems;
    }

    /**
     * Return the database driver.
     *
     * @return  The database driver.
     *
     * @throw FrameworkException If error occurs.
     */
    public static String getDBDriver ( ) throws FrameworkException
    {
        return getValue(DB_DRIVER_PROP, true);
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

    /**
     * Return the CORBA Server IP address.
     *
     * @return  The corba server IP address.
     *
     * @throw FrameworkException If error occurs.
     */
    public static String getCORBAServerAddr ( ) throws FrameworkException
    {
        return getValue(CORBA_SERVER_ADDR_PROP, true);
    }

    /**
     * Return the CORBA Server port.
     *
     * @return  The corba server port.
     *
     * @throw FrameworkException If error occurs.
     */
    public static String getCORBAServerPort ( ) throws FrameworkException
    {
        return getValue(CORBA_SERVER_PORT_PROP, true);
    }

    /**
     * Return the RMI Server IP address.
     *
     * @return  The corba server IP address.
     *
     * @throw FrameworkException If error occurs.
     */
    public static String getRMIServerAddr ( ) throws FrameworkException
    {
        return getValue(RMI_SERVER_ADDR_PROP, true);
    }

    /**
     * Return the CORBA Server port.
     *
     * @return  The corba server port.
     *
     * @throw FrameworkException If error occurs.
     */
    public static String getRMIServerPort ( ) throws FrameworkException
    {
        return getValue(RMI_SERVER_PORT_PROP, true);
    }

    /**
     * Return the application server URL.
     *
     * @return  The application server URL
     *
     * @throw FrameworkException If error occurs.
     */
    public static String getApplicationServerURL ( ) throws FrameworkException
    {
        return getValue(APPLICATION_SERVER_URL_PROP, true);
    }

    /**
     * Return the initial context factory class.
     *
     * @return  the initial context factory class
     *
     * @throw FrameworkException If error occurs.
     */
    public static String getInitialContextFactoryName ( ) throws FrameworkException
    {
        return getValue(INITIAL_CONTEXT_FACTORY_PROP, true);
    }

    /**
     * Get the named value from the common configuration.
     *
     * @param  name  Name of item to retrieve.
     * @param  required  Flag indicating whether item is required or not.
     *
     * @return  Named item, or null if not found and required flag is false.
     *
     * @exception  FrameworkException  Thrown on errors.
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
    
    protected static String getValueSpecificToCurrentJVM(String name) throws FrameworkException
    {
    	return MultiJVMPropUtils.getParameter(name);
    }


    private static Map commonConfigItems;
}

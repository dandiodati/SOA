/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/ServerBase.java#1 $
 */

package com.nightfire.servers;

import java.util.*;
import java.sql.*;

import org.omg.PortableServer.Servant;

import com.nightfire.idl.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.db.portability.DBPortabilityLayer;
import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.cache.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.jmx.cmn.JMXMonitoringManager;

/**
 * The ServerBase class is an abstract base class for CORBA servers, which takes
 * care of all of the CORBA-specific initialization tasks.
 */

public abstract class ServerBase
{
    // Visibroker Smart Agent address and port properties.

    public static final String ORB_AGENT_ADDR_PROP = "ORBagentAddr";
    public static final String ORB_AGENT_PORT_PROP = "ORBagentPort";

    // The property tag that contains the name of the corba server.

    public static final String NAME_TAG = "NAME";

    protected Hashtable serverProperties;

    private CorbaPortabilityLayer cpl;

    protected ObjectLocator objectLocator;

    protected String serverName;


    /**
     * Sub-class entry point for configuration of specific server being implemented.
     * Implementors should perform any initialization and then call BOA.object_is_ready()
     * against their CORBA interface objects before this method returns.
     *
     * @param args Array of strings passed in to main() as command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected abstract void initializeServer(String[] args) throws Exception;

    /**
     * Constructor - Performs CORBA initialization and then calls sub-class initialization routine.
     *
     * @param args Array of strings containing command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected ServerBase(String[] args) throws Exception
    {
        // Start with turn on all log.

        String configLogLevels = System.getProperty(Debug.DEBUG_LOG_LEVELS_PROP);
        if (StringUtils.hasValue(configLogLevels))
        {
            Debug.configureFromProperties(System.getProperties());
            MetricsAgent.configureFromProperties(System.getProperties());
        } 
        else {
            Debug.enable(0);
            Debug.enable(1);
        }

        // Verify command line syntax.

        checkCommandLineSyntax(args);

        // Initialize the database sub-system.

        try
        {

            Debug.log(Debug.NORMAL_STATUS, "ServerBase.ServerBase(): Start initializing database connection ...");

            String dbName = args[0];
            String dbUser = args[1];
            String dbPassword = args[2];
            String serverName = args[3];

            //Set db.properties into System Properties
            SetEnvironment.setSystemProperties(DBPROPERTIES, true);

            // Get all db connection properties from COMMON_PROPERTIES or SERVER_MANAGER (which are configured as system properties)
            Properties prop = getDBConnectionProps(dbName, dbUser, dbPassword, serverName);

            // Set DB Connection Pool Specific Properties into System Properties
            // If exist in SysteProperty do not overwrite.
            setPoolSpecificProps(prop);

            DBInterface.initialize(dbName, dbUser, dbPassword);

            // Initialize pools supporting all configured accounts.

            DBConnectionPool.initializePoolConfigurations();

            Debug.log(Debug.NORMAL_STATUS, "ServerBase.ServerBase(): Finished database initialization.\n");
        }
        catch (DatabaseException dbe)
        {
            Debug.logStackTrace(dbe);

            Debug.log(Debug.ALL_ERRORS, "ERROR: ServerBase.ServerBase(): Database sub-system is not initialized.");

            throw dbe;
        }

        // Load system configuration properties.

        String key = args[3];
        String type = args[4];

        initializeProp(key, type);

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "Starting configuration of specific server ...");

        initializeServerName();

        initializeCORBA(args);

        initializeServer(args);

        // Registering MBeans for monitoring purpose.
        initializeMonitoring();

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "Finished configuration of specific server.");
    }

   private ServerBase()
   {
        // NEVER TO BE USED!
    }

    protected void initializeServerName()
    {
        serverName = getProperty(NAME_TAG);

        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "ServerBase.initializeServerName(): Server's name is [" + serverName + "].");
        }
    }

    protected void exportToNS(String name, Servant servant) throws CorbaException
    {
        if (StringUtils.hasValue(name))
        {
            Debug.log(Debug.NORMAL_STATUS, "ServerBase.exportToNS(): Adding server [" + name + "] to Naming Service ...");

            getObjectLocator().add(name, cpl.getObjectReference(servant));
        }
    }

    /**
     * Default implementation for command line syntax checking.
     * Implementors should overwrite this if using different command line syntax.
     *
     * @param args Array of strings passed in to main() as command-line arguments.
     *
     * @exception  Exception Thrown if failed.
     */
    protected void checkCommandLineSyntax(String[] args) throws Exception
    {
    	if (args.length != 5)
    	{
            throw new Exception("ERROR: ServerBase.checkCommandLineSyntax: Command line syntax incorrect!\n" +
                    "Usage: java ServerBase <DBNAME> <DBUSER> <DBPASSWORD> <KEY> <TYPE>");
        }
    }

    /**
     * Blocks waiting for incoming request.
     *
     * @exception  Exception  Thrown if BOA.impl_is_ready() returns via exception.
     */
    protected void waitForRequests() throws Exception
    {
        Debug.log(Debug.NORMAL_STATUS, "ServerBase.waitForRequests(): Server is ready. Waiting for incoming requests ...");

        try
        {
            cpl.activatePOAManager();

            cpl.run();
        }
        catch (Exception e)
        {
            Debug.logStackTrace(e);

            Debug.log(Debug.ALL_ERRORS, "ServerBase.waitForRequests(): Server exiting ...  Reason:\n" + e.toString());

            throw e;
        }
    }

    /**
     * Call this during server shutdown to properly clean-up and release resources.
     * <B>NOTE:</B> This method needs to be called last because it shuts down the
     * orb.
     */
    protected void shutdown()
    {
        JMXMonitoringManager.getInstance().unregisterMbeans();

        Debug.log(Debug.NORMAL_STATUS, "ServerBase.shutdown(): Shutting-down database connection ...");

        try
        {
            DBInterface.closeConnection();

            Debug.log(Debug.NORMAL_STATUS, "ServerBase.shutdown(): Database connection is closed.");
        }
        catch (DatabaseException dbe)
        {
            Debug.logStackTrace(dbe);

            Debug.log(Debug.ALL_ERRORS, "ERROR: Failure occurred during database connection shutdown:\n" + dbe.toString());
        }

        if (Debug.isLevelEnabled(Debug.DB_STATUS))
            Debug.log(Debug.DB_STATUS, "ServerBase.shutdown(): Shutting-down corba connection ...");

        try
        {
            cpl.shutdown();
        }
        catch (Exception e)
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: ServerBase.shutdown(): Failed to shut down corba connection:\n" + e.toString());
        }
    }

    /**
     * Clean up all the caches in this server.
     *
     * @exception CacheClearingException If the caches in this server cannot be cleared.
     */
    public void flushCache() throws CacheClearingException
    {
        try
        {
            CacheManager.getManager().flushCache();
        }
        catch ( FrameworkException e )
        {
            String errorMessage = "Could not flush cache of server [" + serverName + "]:\n" +
                    e.getMessage();
            Debug.log(Debug.ALL_ERRORS, errorMessage);
            throw new CacheClearingException(errorMessage);
        }
    }

    /**
     * Get the CORBA portability layer object instance.
     *
     * @return The CORBA portability layer object instance.
     */
    public CorbaPortabilityLayer getCorbaPortabilityLayer()
    {
        return cpl;
    }

    /**
     * Get the ORB instance.
     *
     * @return The ORB instance.
     */
    public org.omg.CORBA.ORB getORB()
    {
        return cpl.getORB();
    }

    /**
     * This method tells corba that an object needs to be deactivated
     *
     * @param obj The corba object to deactivate.
     */
    public void corbaDeactivateObj(Servant obj)
    {
        // The following call on the solaris, will throw a COMM_FAILURE exception
        // to the client.  Therefore we catch the exception and continue execution.

        try
        {
            cpl.deactivateObject(obj);
        }
        catch (Exception e)
        {
            Debug.error("ERROR: ServerBase.corbaDeactivateObj(): Failed to deactivate the object:\n" + e.getMessage());
        }
    }

    /**
     * This method tells corba that an object is ready
     *
     * @param name Id of the object to activate.
     * @param obj  The corba object that is ready.
     */
    public void corbaObjectIsReady(String name, Servant obj)
    {
        try
        {
            cpl.activateObject(name, obj);
        }
        catch (Exception e)
        {
            Debug.error("ERROR: ServerBase.corbaObjectIsReady(): Failed to activate the object:\n" + e.getMessage());
        }
    }

    /**
     * Get the named property.
     *
     * @param name Property name.
     *
     * @return The named propery, or null if not found.
     */
    public final String getProperty(String name)
    {
        return (String) serverProperties.get(name);
    }

    /**
     * Performs CORBA-specific server initializations.
     *
     * @param args Array of strings containing command-line arguments passed to main().
     *
     * @exception  Exception Thrown if failed.
     */
    private void initializeCORBA(String[] args) throws Exception
    {
        Debug.log(Debug.NORMAL_STATUS, "ServerBase.initializeCORBA(): Start initializing CORBA sub-system ...");


        Properties props = null;

        String addr = getProperty(ORB_AGENT_ADDR_PROP);

        if (addr != null)
        {
            if (props == null)
            {
                props = new Properties();
            }

            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "ServerBase.initializeCORBA(): " + ORB_AGENT_ADDR_PROP + " [" + addr + "]");

            props.put(ORB_AGENT_ADDR_PROP, addr);
        }

        String port = getProperty(ORB_AGENT_PORT_PROP);

        if (port != null)
        {
            if (props == null)
            {
                props = new Properties();
            }

            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "ServerBase.initializeCORBA(): " + ORB_AGENT_PORT_PROP + " [" + port + "]");

            props.put(ORB_AGENT_PORT_PROP, port);
        }

        Properties serverEngineProps = getSEInitProperties();

        if (serverEngineProps == null)
        {
            cpl = new CorbaPortabilityLayer(args, props, null, serverName);
        }
        else
        {
            cpl = new CorbaPortabilityLayer(args, props, serverEngineProps, serverName);
        }

        Debug.log(Debug.NORMAL_STATUS, "ServerBase.initializeCORBA(): Finished initializing CORBA sub-system.");
    }

    /**
     * Get any properties destined to be passed to the BOA_init() method to set
     * up threading and connection policies.
     *
     * @return Properties object containing BOA initialization
     *         items, or null if none found.
     */
    private Properties getSEInitProperties()
    {
        Properties props = new Properties();

        // NOTE: The following properties are Visibroker-specific!

        String propValue = getProperty("OAid");

        if (propValue != null)
        {
            props.put("OAid", propValue);
        }

        propValue = getProperty("OAthreadMin");

        if (propValue != null)
        {
            props.put("OAthreadMin", propValue);
        }

        propValue = getProperty("OAthreadMax");

        if (propValue != null)
        {
            props.put("OAthreadMax", propValue);
        }

        propValue = getProperty("OAthreadMaxIdle");

        if (propValue != null)
        {
            props.put("OAthreadMaxIdle", propValue);
        }

        propValue = getProperty("OAthreadIdleTime");

        if (propValue != null)
        {
            props.put("OAthreadIdleTime", propValue);
        }

        propValue = getProperty("OAconnectionMax");

        if (propValue != null)
        {
            props.put("OAconnectionMax", propValue);
        }

        propValue = getProperty("OAconnectionMaxIdle");

        if (propValue != null)
        {
            props.put("OAconnectionMaxIdle", propValue);
        }

        propValue = getProperty("OAipAddr");

        if (propValue != null)
        {
            props.put("OAipAddr", propValue);
        }

        propValue = getProperty("OAport");

        if (propValue != null)
        {
            props.put("OAport", propValue);
        }

        if (props.size() == 0)
        {
            return null;
        }
        else
        {
            // If any configuration properties are set, make
            // sure that the BOA threading policy is also set.

            if (props.get("OAid") == null)
            {
                props.put("OAid", "TPool");
            }

            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "ServerBase.getSEInitProperties(): Initialization properties:\n" + PropUtils.suppressPasswords( props.toString()));

            return props;
        }
    }

    /**
     * Loads configuration properties and performs initialization tasks.
     *
     * @param key  Property-key to use for locating initialization properties.
     * @param type Property-type to use for locating initialization properties.
     *
     * @exception  Exception  Thrown if initialization fails.
     */
    private void initializeProp(String key, String type) throws Exception
    {
        PropertyChainUtil propUtil = new PropertyChainUtil();

        serverProperties = propUtil.buildPropertyChains(key, type);

        // Selectively enable diagnostic logging levels via value of property
        // 'DEBUG_LOG_LEVELS and log file from LOG_FILE.

        Debug.configureFromProperties(serverProperties);
        MetricsAgent.configureFromProperties(serverProperties);

        if (Debug.isLevelEnabled(Debug.IO_STATUS) && serverProperties != null)
            Debug.log(Debug.IO_STATUS, "ServerBase.initializeProp(): Loaded props:\n" + PropUtils.suppressPasswords( serverProperties.toString() ) );

        // If thread logging is enabled, turn on thread-id logging in messages.

        if (Debug.isLevelEnabled(Debug.THREAD_BASE))
        {
            Debug.enableThreadLogging();
        }

        // Display stack trace when exceptions are created.

        if (Debug.isLevelEnabled( Debug.EXCEPTION_STACK_TRACE))
        {
            FrameworkException.showStackTrace();
        }

        // Determine whether text read/written to files should be treated as UTF-8 or plain old ASCII.
        String utf8EncodingFlag = getProperty(PlatformConstants.USE_UTF8_ENCODING_PROP);

        if ( StringUtils.hasValue( utf8EncodingFlag ) )
        {
            FileUtils.useUTF8Encoding(StringUtils.getBoolean(utf8EncodingFlag));
        }

        // Put DBConnectionPool configuration items into the System properties.

        String maxDbCons = getProperty(DBConnectionPool.MAX_DBCONNECTION_SIZE_PROP);

        if (maxDbCons != null)
        {
            if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, "ServerBase.initializeProp(): Setting maximum number of database connections property to [" + maxDbCons + "] ...");

            Properties sysProps = System.getProperties();

            sysProps.put(DBConnectionPool.MAX_DBCONNECTION_SIZE_PROP, maxDbCons);

            System.setProperties(sysProps);

            DBConnectionPool.getInstance().setMaxPoolSize(Integer.parseInt(maxDbCons));
        }

        String initDbCons = getProperty(DBConnectionPool.INIT_DBCONNECTION_SIZE_PROP);

        if (initDbCons != null)
        {
            if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, "Setting initial number of database connections property to [" + initDbCons + "] ...");

            Properties sysProps = System.getProperties();

            sysProps.put(DBConnectionPool.INIT_DBCONNECTION_SIZE_PROP, initDbCons);

            System.setProperties(sysProps);
        }

        // Get the maximum time to wait for db connections, if set.

        String maxDbConWaitTime = getProperty(DBConnectionPool.MAX_DBCONNECTION_WAIT_TIME_PROP);

        if (maxDbConWaitTime != null)
        {
            if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, "ServerBase.initializeProp(): Setting maximum time to wait for database connections property to [" + maxDbConWaitTime + "] ...");

            Properties sysProps = System.getProperties();

            sysProps.put(DBConnectionPool.MAX_DBCONNECTION_WAIT_TIME_PROP, maxDbConWaitTime);

            System.setProperties(sysProps);

            DBConnectionPool.getInstance().setMaxResourceWaitTime(Integer.parseInt(maxDbConWaitTime));
        }

        if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
        {
            try
            {
                Debug.log(Debug.SYSTEM_CONFIG, "System properties:\n" + PropUtils.suppressPasswords( System.getProperties().toString() ) );
            }
            catch ( Exception e )
            {
                Debug.error(e.toString());
            }
        }
    }

    /**
     * Performs general server initializations. Will configure via command line
     * argument properties if available in format 'serverConfigFile=<filename>'
     *
     * @param args Array of strings containing command-line arguments passed to main().
     *
     * @exception Exception Thrown if failed.
     */
    private void initializeConfiguration ( String[] args ) throws Exception
    {
        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "ServerBase.initializeConfiguration(): Performing generic server configuration...");

        String configFile = args[0];

        Debug.log(Debug.NORMAL_STATUS, "ServerBase.initializeConfiguration(): Attempting to load server configuration file [" + configFile + "]");

        try
        {
            SetEnvironment.setSystemProperties(configFile, true);

            // Turn off all logging to allow selective enabling via properties.

            Debug.disableAll();
        }
        catch (Exception e)
        {
            Debug.logStackTrace(e);

            Debug.log(Debug.ALL_ERRORS, "ERROR: ServerBase.initializeConfiguration(): Could not configure from properties file:\n" + e.toString());
        }

        // Selectively enable diagnostic logging levels via value of property
        // 'DEBUG_LOG_LEVELS'.

        Debug.configureFromProperties();
        MetricsAgent.configureFromProperties();

        // If thread logging is enabled, turn on thread-id logging in messages.

        if (Debug.isLevelEnabled( Debug.THREAD_BASE))
        {
            Debug.enableThreadLogging();
        }

        // Display stack trace when exceptions are created.

        if (Debug.isLevelEnabled(Debug.EXCEPTION_STACK_TRACE))
        {
            FrameworkException.showStackTrace();
        }

        if (Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "ServerBase.initializeConfiguration(): Finished performing generic server configuration.\n");
    }

    protected final ObjectLocator getObjectLocator()
    {
        if (objectLocator == null)
        {
            objectLocator = new ObjectLocator(getORB());
        }

        return objectLocator;
    }

    /**
     * Fetch all db connection properties from COMMON_PROPERTIES or SERVER_MANAGER (which are configured as system properties)
     * @param dbName     Data base Name
     * @param dbUser     Data base User
     * @param dbPassword Data base Password
     * @param serverName Gateway name
     * @return Properties
     * @throws ResourceException
     */
    public Properties getDBConnectionProps(String dbName, String dbUser, String dbPassword, String serverName) throws ResourceException {

        Properties props = new Properties();
        HashMap<String, String> map = new HashMap<String, String>();

        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {

            DBPortabilityLayer.initialize();
            con = DriverManager.getConnection(dbName, dbUser, dbPassword);

            stmt = con.prepareStatement(DB_QUERY);

            stmt.setString(1, SERVER_MANAGER_KEY);
            stmt.setString(2, COMMON_PROPERTIES);
            stmt.setString(3, serverName);
            stmt.setString(4, COMMON_PROPERTIES);

            rs = stmt.executeQuery();

            List<String> dbConnPropLst = Arrays.asList(DB_CONNECTION_PROPERTIES);

            // Get all db connection specific properties(configured with COMMON_PROPERTIES)
            //   and system properties (configured with SERVER_MANAGER).
            while (rs.next()) {

                String keyValue = rs.getString(1);

                if (keyValue.equals(COMMON_PROPERTIES) && dbConnPropLst.contains(rs.getString(2)))
                    props.put(rs.getString(2), rs.getString(3));

                else if (keyValue.equals(SERVER_MANAGER_KEY) && rs.getString(2).startsWith(SYSTEM_PROPERTY))
                    map.put(rs.getString(2), rs.getString(3));
            }

            if (map.isEmpty())
                return props;

            // Override COMMON_PROPERTIES by SERVER_MANAGER System Properties.
            for (int i = 0; i < (map.size() / 2); i++) {

                String propname = map.get(SYSTEM_PROPERTY_NAME + i);

                if (!dbConnPropLst.contains(propname))
                    continue;

                String propValue = map.get(SYSTEM_PROPERTY_VALUE + i);

                props.put(propname, propValue);

            }
            return props;

        } catch (SQLException sql) {
            Debug.logStackTrace(sql);
            throw new ResourceException("ERROR: Failed to get properties from data base.");
        }

        finally {

            try {    // Close all resources
                if (rs != null)
                    rs.close();

                if (stmt != null)
                    stmt.close();

                if (con != null)
                    con.close();

            } catch (SQLException sqle) {

                Debug.log(Debug.ALL_ERRORS, "Failed to close Data base Resources. " + sqle.getStackTrace());
            }
        }
    }

    /**
     * Set properties into System Property
     * @param props
     */
    public void setPoolSpecificProps(Properties props) {

        if (props == null)
            return;

        Properties systemProps = System.getProperties();

        Enumeration keys = systemProps.keys();

        while ( keys.hasMoreElements() )
        {
            String key = (String) keys.nextElement();

            props.put(key, systemProps.get(key));
        }

        System.setProperties(props);

    }

    /**
     * Method initializes JMX Monitoring.
     */
    protected void initializeMonitoring()
    {
        try
        {
            String jmxConnectorServerNm = System.getProperty(SERVER_ID);

            /* This is to disable stack trace from coming in log files if RMI Registry is not up */
            FrameworkException.hideStackTrace();

            /* Start JMX infrastructure, register MBeans for monitoring */

            JMXMonitoringManager.getInstance().registerMbeans(jmxConnectorServerNm, true);
        }
        catch(Exception e)
        {
            Debug.warning("Failed to register JMX Connector Server for this gateway :"+e.getMessage());
            Debug.warning("JMX Monitoring infrastructure would not be available..");
        }
        finally
        {
            FrameworkException.showStackTrace();
        }
    }
    private static final String SERVER_ID = "ServerID";
    private static final String SYSTEM_PROPERTY_NAME = "SYSTEM_PROPERTY_NAME_";
    private static final String SYSTEM_PROPERTY_VALUE = "SYSTEM_PROPERTY_VALUE_";
    private static final String SYSTEM_PROPERTY = "SYSTEM_PROPERTY";
    private static final String SERVER_MANAGER_KEY = "SERVER_MANAGER";
    private static final String COMMON_PROPERTIES = "COMMON_PROPERTIES";
    private static final String DBPROPERTIES = "db.properties";
    protected static final String[] DB_CONNECTION_PROPERTIES = { DBConnectionPool.MAX_DBCONNECTION_SIZE_PROP,
            DBConnectionPool.MAX_DBCONNECTION_WAIT_TIME_PROP,
            DBConnectionPool.INIT_DBCONNECTION_SIZE_PROP,
            DBConnectionPool.IDLE_DBCONNECTION_CLEANUP_WAIT_TIME_PROP,
            DBConnectionPool.VALIDATE_CONNECTION_USING_SQL_PROP,
            DBConnectionPool.USE_ORACLE_CONNECTION_POOL_PROP,
            DBConnectionPool.ORACLE_CONNECTION_AUTO_COMMIT_PROP};

    private static final String DB_QUERY = "Select key, propertyname, propertyvalue from " +
            "PERSISTENTPROPERTY where key in (?, ?) and propertytype in (?, ?)";

}

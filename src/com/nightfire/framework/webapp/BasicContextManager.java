/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 */

package com.nightfire.framework.webapp;

import  java.io.*;
import  java.lang.*;
import  java.net.*;
import  java.util.*;
import java.sql.*;
import  javax.servlet.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.db.*;
import com.nightfire.framework.db.portability.DBPortabilityLayer;
import  com.nightfire.framework.message.MessageException;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.debug.*;
import  com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.rmi.RMIProxy;


/**
 * <p><strong>BasicBasicContextManager</strong> performs db, property, and debug initialization and
 * cleanup tasks during servlet-container startup and shutdown processes.
 * This class is used by different type of web applications so it should not
 * be gui only.
 * Child classes must call super.contextInitialized and super.contextDestroyed when they overwrite those methods.</p>
 */

public class  BasicContextManager implements ServletContextListener
{

	/**
     * Name for accessing name of instance properties file from CATALINA_OPTS     *  
     */
	public static final String INSTANCE_PROPERTIES_FILE = "INSTANCE_PROPERTIES_FILE";
	
    /**
     * Persistent property key of which contains the all properties for webapps.
     */
    public static final String WEBAPPS_PROP_KEY = "WEBAPPS";

    /**
     * Persistent property type of which contains the common properties for webapps.
     */
    public static final String COMMON_PROP_TYPE = "COMMON";

    /**
     * Name for accessing the context's Properties object containing all context
     * parameters.
     */
    public static final String CONTEXT_PARAMS   = "WEBAPP_CONTEXT_PARAMS";

    public static final String WEB_APP_NAME = "WEB_APP_NAME";



    protected ServletContext servletContext;
    protected DebugLogger log;

    protected boolean prodMode = true;
    protected Properties initParameters = null;

    private static boolean bootLoggingInitialized = false;

    private static Properties bootLogConfig = null;

    /**
     * Implementation of ServletContextListener's contextInitialized().  This
     * gets called by the servlet container to notify that the web application
     * is ready to process requests.
     *
     * @param  event  ServletContextEvent object.
     */
    public void contextInitialized(ServletContextEvent event)
    {
      servletContext = event.getServletContext();

      // CR 20444: We'll need to reinitialize the log level back to stdout for each webapps
      // to prevent boot log cross over webapps.
      try
      {
        DebugConfigurator.configure(getBootLogConfig(servletContext), null);
      }
      catch (FrameworkException fe)
      {
        servletContext.log("WARNING: Cannot initialize boot log file.");
      }

      String webAppName = getWebAppContextPath(servletContext);

      servletContext.log("contextInit: webAppName is [" + webAppName + "].");

      // used by ServletUtils.getWebAppContextPath
      servletContext.setAttribute(WEB_APP_NAME, webAppName);


      // Need to initialize database connection first to get further configurations from
      // persistent properties

      initializeDatabase();

      try {
        initParameters = getInitParameters(servletContext);
      }
      catch (ServletException e) {
        System.err.println("Failed to load properties for web app ["+ webAppName +"]:" + e.getMessage() );
      }



      String configFile = servletContext.getRealPath("/" +
                              DebugConfigurator.DEFAULT_ALT_CONFIG_FILE);

      try {
        DebugConfigurator.configureAndWatch(initParameters, webAppName, configFile);
      }
      catch (FrameworkException e) {
        System.err.println("BasicContextManager: Failed to setup debugging: " + e.getMessage() );
      }

      log = DebugLogger.getLogger(webAppName, getClass());
      // finish init of logging

      RemoteOperationsAdminImpl.initialize( initParameters );

      // initialize database connection pool
      initializeDBConnectionPool();



      log.info("getInitParameters(): The context parameters/properties to be used are as follows:\n" + PropUtils.suppressPasswords(initParameters.toString()));


      servletContext.setAttribute(CONTEXT_PARAMS, initParameters);


        String  prodModeStr = initParameters.getProperty(PlatformConstants.PRODUCTION_MODE);

        prodMode = StringUtils.getBoolean(prodModeStr, true);

        log.info("contextInitialized(): Production mode set to [" + prodMode + "]");
    }

    private static Properties getBootLogConfig(ServletContext context)
    {
      if (bootLogConfig == null)
      {
        synchronized (BasicContextManager.class)
        {
          if (bootLogConfig == null)
          {
            context.log("System Properties: " + System.getProperties());

            // Start with turn on all log.
            String configLogLevels = System.getProperty(Debug.DEBUG_LOG_LEVELS_PROP);
            if (!StringUtils.hasValue(configLogLevels))
            {
              configLogLevels = "0 1";
            }

            String bootLogFile = System.getProperty(Debug.LOG_FILE_NAME_PROP);
            if (!StringUtils.hasValue(bootLogFile))
            {
              bootLogFile = "console";
            }

            bootLogConfig = new Properties();
            bootLogConfig.put(DebugConfigurator.DEBUG_LOG_LEVELS, configLogLevels);
            bootLogConfig.put(Debug.LOG_FILE_NAME_PROP, bootLogFile);

            context.log("Got boot logging configuration [" + bootLogConfig + "].");
          }
        }
      }
      return bootLogConfig;
    }

    private void initializeDatabase()
    {
        // Sets up database support for bootstrapping.

        try
        {
            servletContext.log("Initializing database connection ...");

            String dbDriver   = CommonConfigUtils.getDBDriver();
            String dbName     = CommonConfigUtils.getDBName();
            String dbUser     = CommonConfigUtils.getDBUser();
            String dbPassword = CommonConfigUtils.getDBPassword();

            servletContext.log("initializeDatabase(): [" + DBInterface.DB_DRIVER_PROPERTY + "] has value [" + dbDriver + "].");
            servletContext.log("initializeDatabase(): [" + DBInterface.DB_NAME_PROPERTY + "] has value [" + dbName + "].");
            servletContext.log("initializeDatabase(): [" + DBInterface.DB_USER_PROPERTY + "] has value [" + dbUser + "].");

            // Database driver should be passed in as an argument to DBInterface's
            // initialize() since setting System properties may violate security
            // constraints.  Currently DBInterface does not provide this API.

            if (dbDriver != null)
            {
                System.setProperty(DBInterface.DB_DRIVER_PROPERTY, dbDriver);
            }

            // Set Oracle pool information as System properties
            if( StringUtils.getBoolean(CommonConfigUtils.getValue(CommonConfigUtils.USE_ORACLE_CONNECTION_POOL_PROP), false))
                 setSystemPropsForOraclePool(dbName, dbUser, dbPassword);

            DBInterface.initialize(dbName, dbUser, dbPassword);

            servletContext.log("Database initialization completed.");
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            System.err.println("BasicContextManager.initializeDatabase(): Failed to initialize database:\n" + e.getMessage());
        }

    }

    private void initializeDBConnectionPool()
    {
        // Sets up database pool.

        try
        {
            log.info("Initializing database connection pool ...");

            String maxDbCons = initParameters.getProperty(DBConnectionPool.MAX_DBCONNECTION_SIZE_PROP);

            String maxDbConWaitTime = initParameters.getProperty(DBConnectionPool.MAX_DBCONNECTION_WAIT_TIME_PROP);

            if (StringUtils.hasValue(maxDbCons)) {
                log.info("Setting maximum number of database connections property to [" + maxDbCons + "].");
                DBConnectionPool.getInstance().setMaxPoolSize(Integer.parseInt(maxDbCons));
            }

            if (StringUtils.hasValue(maxDbConWaitTime)) {
                log.info("Setting maximum time to wait for database connections property to [" + maxDbConWaitTime + "].");
                DBConnectionPool.getInstance().setMaxResourceWaitTime(Integer.parseInt(maxDbConWaitTime));
            }

            log.info("Database connection pool initialization completed.");
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            System.err.println("BasicContextManager.initializeDatabasePool(): Failed to initialize database:\n" + e.getMessage());
        }
    }

    /**
     * Implementation of ServletContextListener's contextInitialized().  This
     * gets called by the servlet container to notify that the servlet context
     * is about to be shutdown.
     *
     * @param  event  ServletContextEvent object.
     */
    public void contextDestroyed(ServletContextEvent event)
    {
       log.info("Destroying context [" + event.getServletContext().getServletContextName() +"]");

       RemoteOperationsAdminImpl.shutdown( );

       try
       {
            log.info("contextDestroyed(): unexporting all objects from JRMP..");
            RMIProxy.getInstance().unexportAll();
       }
       catch(Exception e)
       {
           log.error("contextDestroyed(): Failed to get handle of RMI Proxy \n" + e.getMessage(), e);
       }
       
       try
        {
            log.info("contextDestroyed(): Closing database connection ...");

            DBInterface.closeConnection();

            log.info("contextDestroyed(): Database connection has been closed.");
        }
        catch (Exception e)
        {
          log.error("contextDestroyed(): ERROR: Failed to close database connection:\n" + e.getMessage(), e);
        }
    }

    /**
     * Get the Properties object containing application initialization parameters.
     *
     * @param  servletContext  ServletContext object.
     *
     * @return  Properties object containing application initialization parameters.
     */
    private Properties getInitParameters(ServletContext servletContext) throws ServletException
    {
        // 1st, gather up all the context parameters into a Properties object for
        // ease of handling.

        servletContext.log("getInitParameters: Obtaining contextProperties from web.xml...");

        Properties  contextProperties = new Properties();

        Enumeration initParamNames    = servletContext.getInitParameterNames();

        while (initParamNames.hasMoreElements())
        {
            String initParamName  = (String)initParamNames.nextElement();

            String initParamValue = servletContext.getInitParameter(initParamName);

            if (initParamValue != null)
            {
                contextProperties.put(initParamName, initParamValue);
            }
        }

        servletContext.log("getInitParameters: Obtained contextProperties from web.xml: " + PropUtils.suppressPasswords(contextProperties.toString()));


        // Use the webAppName and COMMON_PROP_TYPE as the property tpes to load persistent properties
        Map props = null;
        String webAppName = (String) servletContext.getAttribute(WEB_APP_NAME);

        String webAppPropType = webAppName.substring(1);

        servletContext.log("getInitParameters: Obtaining configuration from persistent properties with property type [" +  webAppPropType + "]...");

        try
        {
            props = PropUtils.getProperties(WEBAPPS_PROP_KEY,
                                            webAppPropType, COMMON_PROP_TYPE);

            servletContext.log("getInitParameters: Obtained configuration from persistent properties with property type [" +  webAppPropType + "]: " + props);

        }
        catch (FrameworkException fe)
        {
            String errMsg = "Failed to load persistent properties for [" + webAppName + "].";
            servletContext.log(errMsg, fe);
            throw new ServletException(errMsg, fe);
        }

        // Merge persistent properties to context properties
        contextProperties = PropUtils.mergeMap(contextProperties, props);

        // Load common config items.
        try
        {
            props = CommonConfigUtils.getCommonConfigItems();
            servletContext.log("getInitParameters: Obtained common configuration items: " + PropUtils.suppressPasswords(props.toString()) );

        }
        catch (FrameworkException fe)
        {
            String errMsg = "Failed to retrieve common configuration items for [" + webAppName + "].";
            servletContext.log(errMsg, fe);
            throw new ServletException(errMsg, fe);
        }

        // Merge common configuration items to context properties
        contextProperties = PropUtils.mergeMap(contextProperties, props);

        // Merge properties from intance specific properties file to contextProperties
        try{
        	MultiJVMPropUtils.loadInitParamsFromPropFile(contextProperties, servletContext, webAppName);
        }catch(FrameworkException fe){
        	throw new ServletException(fe);
        }
        return contextProperties;
    }

    /**
     * Returns the context path of the current webapp.
     * If the context path can not be obtained then an empty string is
     * returned.
     */
  public static final String getWebAppContextPath(ServletContext context)
  {

    String webappName = (String)context.getAttribute(WEB_APP_NAME);
    if ( StringUtils.hasValue(webappName) )
      return webappName;


    try {
      String url = context.getResource("/").toString();
      if ( url.endsWith("/") )
        url = url.substring(0, url.length() - 1);

      webappName = url.substring(url.lastIndexOf("/") + 1 );

    }
    catch (Exception e) {
    }

    return "/" + webappName;

  }
    /**
    * Method sets configured properties for Oracle Connection Pool based on following priorities.
    * 1. First priority would be given on properties configuread in commomConfig.xml
    * 2. Second Priority would be given on Persistent property with key =  WEBAPPS and type = Specific webapp name
    * 3. Third Priority would be given on Persistent property with key =  WEBAPPS and type = COMMON
    */
  public void setSystemPropsForOraclePool(String dbName, String dbUser, String dbPassword) throws Exception{

       // Read props from commonConfig.xml file
       String maxLimit          = CommonConfigUtils.getValue(CommonConfigUtils.ORACLE_POOL_MAX_LIMIT_PROP);
       String initLimit         = CommonConfigUtils.getValue(CommonConfigUtils.ORACLE_POOL_INIT_LIMIT_PROP);
       String conmWaitTimeOut   = CommonConfigUtils.getValue(CommonConfigUtils.ORACLE_CONN_WAIT_TIME_OUT_PROP);
       String conInactivityTimeOut = CommonConfigUtils.getValue(CommonConfigUtils.ORACLE_CONN_INACTIVITY_TIME_OUT_PROP);
       String validateConn      = CommonConfigUtils.getValue(CommonConfigUtils.ORACLE_VALIDATE_CONN_PROP);
       String onsCfg            = CommonConfigUtils.getValue(CommonConfigUtils.ONS_CONFIG_FOR_ORACLE_RAC);
       String autoCommitFlag =  CommonConfigUtils.getValue(CommonConfigUtils.ORACLE_POOL_AUTO_COMMIT_FLAG);


       if (StringUtils.hasValue(maxLimit))
           System.setProperty(DBConnectionPool.MAX_DBCONNECTION_SIZE_PROP, maxLimit);

       if (StringUtils.hasValue(initLimit))
              System.setProperty(DBConnectionPool.INIT_DBCONNECTION_SIZE_PROP, initLimit);

       if (StringUtils.hasValue(conmWaitTimeOut))
              System.setProperty( DBConnectionPool.MAX_DBCONNECTION_WAIT_TIME_PROP, conmWaitTimeOut);

       if (StringUtils.hasValue(conInactivityTimeOut))
              System.setProperty(DBConnectionPool.IDLE_DBCONNECTION_CLEANUP_WAIT_TIME_PROP, conInactivityTimeOut);

       if (StringUtils.hasValue(validateConn))
              System.setProperty(DBConnectionPool.VALIDATE_CONNECTION_USING_SQL_PROP, validateConn);

       if (StringUtils.hasValue(onsCfg))
              System.setProperty(DBConnectionPool.ONS_CONFIG_FOR_ORACLE_RAC, onsCfg);

       if (StringUtils.hasValue(autoCommitFlag))
              System.setProperty(DBConnectionPool.ORACLE_CONNECTION_AUTO_COMMIT_PROP, autoCommitFlag);


       // if any one of the properties (maxLimit, initLimit, conmWaitTimeOut, conInactivityTimeOut, validateConn)
       // is not configured in commonConfig then fetch it from persistent property (with key 'WEBAPPS')

       if(! (StringUtils.hasValue(maxLimit) && StringUtils.hasValue(initLimit) && StringUtils.hasValue(conmWaitTimeOut)
               && StringUtils.hasValue(conInactivityTimeOut) && StringUtils.hasValue(validateConn) ) ){

            // Get web application name from context parameter
            String webAppName = (String) servletContext.getAttribute(WEB_APP_NAME);

            Properties dbProperties = getDBConnectionProps(dbName, dbUser, dbPassword, webAppName.substring(1));

            setSystemProperties( dbProperties );

       }

   }
   /**
     * Fetch all db connection properties from WEBAPPS
     * @param dbName  Data base Name
     * @param dbUser  Data base User
     * @param dbPassword  Data base Password
     * @param applicationName  Gateway name
     * @return Properties
     * @throws com.nightfire.framework.resource.ResourceException
    */
    public Properties getDBConnectionProps(String dbName, String dbUser, String dbPassword, String applicationName) throws ResourceException {

        Map<String, String> defaultProps = new HashMap<String, String>();
        Map<String, String> primaryProps = new HashMap<String, String>();

        Connection con= null;
        PreparedStatement stmt = null;
        ResultSet rs = null;                           

         try{

               DBPortabilityLayer.initialize();
               con = DriverManager.getConnection( dbName, dbUser, dbPassword );

               stmt = con.prepareStatement(DB_QUERY);

               stmt.setString(1, WEBAPPS_PROP_KEY);
               stmt.setString(2, COMMON_PROP_TYPE);
               stmt.setString(3, applicationName);

               rs = stmt.executeQuery();

               Properties sysProps = System.getProperties();

               while(rs.next()){

                    String propertyType =  rs.getString(1);
                    String propertyName = rs.getString(2);
                    String propertyValue =  rs.getString(3);
                    // In case of property already exist in System Props or property value is not configured skip it.
                    if (sysProps.containsKey(propertyName) || !StringUtils.hasValue(propertyValue) )
                      continue;

                    if(COMMON_PROP_TYPE.equals( propertyType ))
                         defaultProps.put(propertyName, propertyValue);
                    else
                         primaryProps.put(propertyName, propertyValue);

               }
               return PropUtils.mergeMap (primaryProps, defaultProps);

         }catch(SQLException sql){
                Debug.logStackTrace(sql);
                throw new ResourceException("ERROR: Failed to get properties from data base.");
          }

          finally{

               try{    // Close all resources
                        if(rs!= null)
                          rs.close();

                        if(stmt!= null)
                          stmt.close();

                        if(con!= null)
                             con.close();

                   } catch(SQLException sqle){

                      Debug.log(this, Debug.ALL_ERRORS, "Failed to close Data base Resources. " +sqle.getStackTrace());
                   }
            }
     }

     /**
     * Set properties into System Property and do not overwrite if already exist
     * @param props
     */
    public void setSystemProperties(Properties props){

        if (props == null)
            return;

        Properties systemProps = System.getProperties();

            Enumeration keys = systemProps.keys();

            while ( keys.hasMoreElements() )
            {
                String key = (String)keys.nextElement();

                if(!props.containsKey(key))
                {
                    props.put( key, systemProps.get(key) );
                }
            }

         System.setProperties( props );

    }

    private static String  DB_CONNECTION_PROPERTIES = "'"+DBConnectionPool.MAX_DBCONNECTION_SIZE_PROP +"','"+
                                                            DBConnectionPool.MAX_DBCONNECTION_WAIT_TIME_PROP + "','"+
                                                            DBConnectionPool.INIT_DBCONNECTION_SIZE_PROP +"','"+
                                                            DBConnectionPool.IDLE_DBCONNECTION_CLEANUP_WAIT_TIME_PROP +"','"+
                                                            DBConnectionPool.VALIDATE_CONNECTION_USING_SQL_PROP + "'";
    
    private static final String DB_QUERY =  "select propertytype, propertyname, propertyvalue from persistentproperty " +
                            "where key = ? and propertytype in ( ?, ? ) and propertyname in ("+ DB_CONNECTION_PROPERTIES +") ";


}

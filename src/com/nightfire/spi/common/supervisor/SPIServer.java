/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //gateway/R4.4/com/nightfire/spi/common/supervisor/SPIServer.java#1 $
 */
package com.nightfire.spi.common.supervisor;

import  java.util.*;

import  com.nightfire.idl.*;
import  com.nightfire.servers.*;
import  com.nightfire.framework.db.*;
import  com.nightfire.framework.util.*;
import  com.nightfire.framework.corba.*;


/**
 * SPI Server, starts the supervisor.
 */
public class SPIServer extends ServerBase implements ServerAdminOperations
{
    /**
     * The property tag that contains the key of the supervisor.
     */
    public static final String SUPERVISOR_KEY_TAG = "SUPERVISOR_KEY";
    
    /**
     * The property tag that contains the type of the supervisor.
     */
    public static final String SUPERVISOR_TYPE_TAG = "SUPERVISOR_TYPE";

    private String         name;
    
    private ServerAdminPOA theServer;
	
    // Will be set to 'true' when ServerAdmin.shutdown() is called against this gateway.
    private static boolean isShuttingDownFlag = false;


    /**
     * Entry point for starting up SPI services.
     *
     * @param  args  Array of strings containing command-line arguments.
     */
    public static void main(String[] args)
    {
        try 
        {
            SPIServer server = new SPIServer(args);
    
            Debug.log(Debug.IO_STATUS, "SPIServer.main(): SPI Server is now configured." );
        	
            server.waitForRequests();

            Debug.log(Debug.IO_STATUS, "SPIServer.main(): SPI Server has shutdown successfully." );
        }
        catch (Exception e)
        {
            Debug.error("ERROR: SPIServer.main(): SPI Server exiting ...\n:" + e.toString());
        }
    }

    /**
     * Shuts down the server and properly clean up and release resources.
     */
    public void shutdown()
    {
        isShuttingDownFlag = true;

        RemoteOperationsAdminImpl.shutdown( );

        try 
        {
            Debug.log(Debug.DB_STATUS, "SPIServer.shutdown(): Removing server [" + serverName + "] from Naming Service ...");
            
            getObjectLocator().remove(serverName);
        }
        catch (Exception e) 
        {
            Debug.error("ERROR: SPIServer.shutdown(): Failed to remove server [" + serverName + "] from Naming Service:\n" + e.toString());
        }
        
        Debug.log(Debug.MSG_STATUS, "SPIServer.shutdown(): Shutting-down supervisor ..." );
        
        Supervisor.getSupervisor().shutdown();
               
        corbaDeactivateObj(theServer);

        super.shutdown();
    }


    /**
     * Indicates whether gateway process has been told to shut down via Server Manager.
     *
     * @return  'true' if server has been told to shut down, otherwise 'false'.
     */
    public static boolean isShuttingDown ( )
    {
        if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
            Debug.log( Debug.OBJECT_LIFECYCLE, "Gateway is shutting-down? [" 
                       + isShuttingDownFlag + "]." );

        return isShuttingDownFlag;
    }


    /**
     * Constructor which creates the server.
     *
     * @param  args  Array of strings containing command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected SPIServer(String[] args) throws Exception
    {
        super(args);
    }

    /**
     * Default implementation for command line syntax checking.
     * Implementors should overwrite this if using different command line syntax.
     *
     * @param  args  Array of strings passed in to main() as command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected void checkCommandLineSyntax(String[] args) throws Exception
    {
        if (args.length != 5)
        {
	        throw new Exception("ERROR: Command line syntax incorrect!\n\n" +
                                "Usage: java SPIServer <DBNAME> <DBUSER> <DBPASSWORD> <KEY> <TYPE>\n" +
                                "where  DBNAME:     Name of DB connection\n" +
                                "       DBUSER:     Name of DB user\n" +
                                "       DBPASSWORD: The password of DB user\n" +
                                "       KEY       : Key for SPI server properties\n" +
                                "       TYPE      : Type for SPI server properties\n" +
                                "e.g.: java SPIServer jdbc:oracle:thin:@127.0.0.1:1521:ORCL scott tiger SPI_SERVER SPI_SERVER");
        }
    }
    
    /**
     * Configures this server and then makes objects known via BOA.object_is_ready() calls.
     *
     * @param  args  Array of strings passed in to main() as command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected void initializeServer(String[] args) throws Exception
    {         
        theServer = new ServerAdminPOATie(this);
        
        corbaObjectIsReady(serverName, theServer);
        
        exportToNS(serverName, theServer);        

        Supervisor supervisor = Supervisor.getSupervisor();
        
        supervisor.setLoadingServer(this);

        String key  = super.getProperty(SUPERVISOR_KEY_TAG);
        
        String type = super.getProperty(SUPERVISOR_TYPE_TAG);
		
        if ((key == null) || (type == null))
        {
            throw new Exception("ERROR: \"" + SUPERVISOR_KEY_TAG + "\" or \"" + SUPERVISOR_TYPE_TAG + "\" not specified " +
                                "in SPIServer[" + args[3] + ", " + args[4] + "].");
        }
        
        if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        {
    	    Debug.log(Debug.NORMAL_STATUS, "SPIServer.initializeServer(): Initializing Supervisor with key [" + key + "] and type [" + type + "] ...");
        }
        
        supervisor.initialize(key, type);

        supervisor.startup();

        RemoteOperationsAdminImpl.initialize( serverProperties );
    }
}


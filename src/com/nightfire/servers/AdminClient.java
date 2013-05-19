/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/servers/AdminClient.java#1 $
 */
package com.nightfire.servers;

import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.*;

import com.nightfire.idl.*;


/**
 * This admin server first binds to a server object with the name passed
 * from command line. Then call the server object's shutdown method.
 * The server object must implements the ServerAdmin.idl interface.
 *
 */
public class AdminClient extends ServerBase
{

	ServerAdmin handler;	
	
	String name;

    /**
     * Entry point for CORBA server execution.
     *
     * @param  args  Array of strings containing command-line arguments.
     */
    public static void main ( String[] args )
    {
        //Debug.showLevels();
        //Debug.enableAll();
        //Debug.hideLevels();
        //Debug.enable(Debug.ALL_WARNINGS);
        //Debug.enable(Debug.ALL_ERRORS);
        //Debug.enable(Debug.NORMAL_STATUS);


        Debug.log(Debug.NORMAL_STATUS, "Creating shutdown client ..." );
        
        try {
         
        	AdminClient server = new AdminClient( args );
	        
	        Debug.log(Debug.NORMAL_STATUS, "Shutdown client initialized. Bringing down the server \"" + server.name + "\" ..." );
        	
            server.shutdown( );
	        
	        Debug.log(Debug.NORMAL_STATUS, "Server \"" + server.name + "\" has been shut down successfully." );
        }
        catch ( Exception e )
        {
        	e.printStackTrace();
            Debug.log(Debug.ALL_ERRORS, "ERROR: Shutdown client exiting now ...\n" + e.toString() );
        }
    }


    /**
     * Call this during server shutdown to properly clean-up and release resources.
     */
    public void shutdown ( )
    {
    	// Shutdown DB connection.
    	
        super.shutdown();
    	
        Debug.log(Debug.NORMAL_STATUS, "Shutting down Server ..." );
        
        handler.shutdown();
        
    }


    /**
     * Constructor which creates the server. Skip DB connection.
     *
     * @param  args  Array of strings containing command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected AdminClient ( String[] args ) throws Exception
    {
        super( args );
    }



    /**
     * Default implementation for command line syntax checking.
     * Implementors should overwrite this if using different command line syntax.
     *
     * @param  args  Array of strings passed in to main() as command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected void checkCommandLineSyntax ( String[] args ) throws Exception
    {
        if (args.length != 5) {
		    throw new Exception("ERROR: Command line syntax incorrect!\n\n"
		    + "Usage: java AdminClient <DBNAME> <DBUSER> <DBPASSWORD> <KEY> <TYPE>\n"
			+ "    DBNAME:     Name of DB connection\n"
			+ "    DBUSER:     Name of DB user\n"
			+ "    DBPASSWORD: The password of DB user\n"
			+ "    KEY       : Key for server properties\n"
			+ "    TYPE      : Type for server properties\n"
		    + "e.g.: java AdminClient jdbc:oracle:thin:@127.0.0.1:1521:ORCL scott tiger SPI_SERVER SPI_SERVER\n"
			);
        }

    }
    /**
     * Configures this server and then locates and binds to the server object using naming service.
     *
     * @param  args  Array of strings passed in to main() as command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected void initializeServer ( String[] args ) throws Exception
    {
        // Locate the Server object in naming service.
		name = super.getProperty( NAME_TAG );

		if (name != null) {
            
            if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
	           Debug.log(Debug.MSG_LIFECYCLE, "Binding to server [" + name + "] ..." );
	        
	        ObjectLocator ol = new ObjectLocator(getORB());
	        org.omg.CORBA.Object theServer = ol.find(name);
	        
	        handler = ServerAdminHelper.narrow(theServer);
        
		    if (handler == null || handler._non_existent()) {
		    	throw new Exception("ERROR: Cannot find server [" + name + "]." );
		    }
	    }
	    else {
	    	throw new Exception("Cannot use the CLIENT to shutdown a server without name.");
	    }

	}
	
}

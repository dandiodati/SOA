package com.nightfire.router;

import com.nightfire.framework.util.*;
import com.nightfire.servers.*;
import com.nightfire.idl.*;


/**
 * This is the server component of the router which handles startup and shutdown request to the
 * router.
 * @author Dan Diodati
 */
public class RouterAdminServer extends ServerBase implements RouterConstants, ServerAdminOperations
{
    private ServerAdminPOA theServer;

    protected boolean testMode = false;

    //protected static String SUPERVISOR_KEY = "SUPERVISOR_KEY";
    //protected static String SUPERVISOR_TYPE = "SUPERVISOR_TYPE";

  /**
   * Constructor which takes command line args
   */
  protected RouterAdminServer ( String[] args ) throws Exception
    {
        super( args );
    }


  /**
     *  implementation for command line syntax checking.
     *
     * @param  args  Array of strings passed in to main() as command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected void checkCommandLineSyntax ( String[] args ) throws Exception
    {
        if (args.length < 5) {
	    throw new Exception("ERROR: Command line syntax incorrect!\n\n"
                                + "Usage: java RouterAdminServer <DBNAME> <DBUSER> <DBPASSWORD> <KEY> <TYPE>\n"
                                + "    DBNAME:     Name of DB connection\n"
                                + "    DBUSER:     Name of DB user\n"
                                + "    DBPASSWORD: The password of DB user\n"
                                + "    KEY       : Key for SPI server properties\n"
                                + "    TYPE      : Type for SPI server properties\n"
                                + "e.g.: java RouterAdminServer jdbc:oracle:thin:@127.0.0.1:1521:ORCL scott tiger SPI_SERVER SPI_SERVER\n"
                                );
        }

    }




    /**
     * Entry point for starting up the server.
     *
     * @param  args  Array of strings containing command-line arguments.
     */
    public static void main ( String[] args )
    {
        //Debug.showLevels();
        //Debug.enableAll();

        Debug.log(Debug.NORMAL_STATUS, "Starting Router ..." );

        try {
           
            
            RouterAdminServer server = new RouterAdminServer( args );
            
            server.waitForRequests( );

            Debug.log(Debug.NORMAL_STATUS,"Router shutdown successfully");
        }
        catch ( Exception e )
            {
                Debug.log( Debug.ALL_ERRORS, "Router going down ...\nMessage:" + e.toString() );
            }
    }


    /**
     * Shuts down the server and properly clean up and release resources.
     */
    public void shutdown ( )
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "RouterAdminServer.shutdown(): Removing [" + serverName + "] from Naming service ...");

       
        
        try {
            objectLocator.remove(serverName);
            objectLocator.removeFromCache(serverName);
        }
        catch (Exception e) {

            Debug.error("ERROR: RouterAdminServer.shutdown(): Could not remove name reference from naming service:\n" + e.toString());
        }

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, "Shutting-down supervisor ..." );
        
        if (!testMode) {
           RouterSupervisor.getSupervisor().shutdown();
        }

        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "RouterAdminServer.shutdown(): Shutting down Router Service ..." );

        corbaDeactivateObj(theServer);

        // Clean up DB connection, corba  and other resources.
        super.shutdown();
    }






    /**
     * Configures this server
     *
     * @param  args  Array of strings passed in to main() as command-line arguments.
     *
     * @exception Exception Thrown if failed.
     */
    protected void initializeServer ( String[] args ) throws Exception
    {
        RouterSupervisor theSup;
        // Get the name from Properties

        // Create the server CORBA object with the server name
        theServer =  new ServerAdminPOATie(this);
        
        corbaObjectIsReady(serverName, theServer);

        exportToNS(serverName, theServer);  

        // Creating the router supervisor.
        if (!testMode) {

           Debug.log(Debug.NORMAL_STATUS, "Starting supervisor" );

           theSup = (RouterSupervisor) RouterSupervisor.getSupervisor();
           theSup.setLoadingServer(this);

           // Initialize the supervisor.
           //System.out.println("routeradmin props = " + serverProperties );
           String key = PropUtils.getRequiredPropertyValue(serverProperties, SUPERVISOR_KEY );
           String type = PropUtils.getRequiredPropertyValue(serverProperties, SUPERVISOR_TYPE );

           Debug.log(Debug.NORMAL_STATUS, "Initializing RouterSupervisor with key[" + key + "], type[" + type + "]..." );

           theSup.initialize(key, type);

           // Start up the supervisor.
           theSup.startup();
        }
        
        RemoteOperationsAdminImpl.initialize( serverProperties );    
    }
}
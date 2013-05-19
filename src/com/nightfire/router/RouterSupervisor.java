/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import com.nightfire.spi.common.supervisor.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.common.*;

import com.nightfire.servers.ServerBase;

import com.nightfire.router.util.*;

import java.util.*;

/**
 * The RouterSupervisor is responsible for starting
 * main request servers in the router. In most cases
 * there will probably only be one RequestServer started.
 * This class also tracks Servers that are running
 * at one or more CORBA ORBs. It provides an interface
 * for checking availablity of servers.
 * @author Dan Diodati
 */
public class RouterSupervisor extends Supervisor implements RouterConstants
{
  private static final RouterSupervisor supervisor = new RouterSupervisor();

  public static final String ALT_ORBS_POA_NAME   = "ALTERNATE_ORBS";
  public static final String ALT_ORB_ADDR_PREFIX = "AltORBagentAddr";
  public static final String ALT_ORB_PORT_PREFIX ="AltORBagentPort";
  public static final String ALT_ORB_CONDITION_PREFIX ="AltORBRoutingCondition";
  public static final String ORB_ROUTING_REQUESTTYPE = "ORBRoutingRequestType";
  public static final String SPI_EVENT_CHANNEL   = "SPI_EVENT_CHANNEL";
  public static final String SEP                 = ":";
  public static final String DOT                 = ".";

  private static final RunningServers runningServers = new RunningServers(supervisor);

  private Map eventConnections;
  private Map objLocators;
  private String cosPrefix = null;
  
  private EventConnection defaultConnection;
  

  // inner class used to hold all event connections and related cpls.
  private final class EventConnection
  {
     EventPushConsumer consumer;
     CorbaPortabilityLayer cp;
  }

  // inner class used to hold info about object locators.
  private final class LocatorInfo
  {
     LocatorInfo(ObjectLocator loc, String addr, String port) {
        this.loc = loc;
        this.address = addr;
        this.port    = port;
     }

     ObjectLocator loc;
     String address;
     String port;
  }

//  private LinkedList eventConsumers;  // need to track event consumers to disconnect them.

//  private boolean finishedStarting;


  /**
   * returns a handle to an AvailiableServers object which is used
   * to access the current SPI servers that the router is tracking.
   * @return AvailableServers This object gives access to servers.
   *
   * <b>NOTE: MessageDirectors must use this method to gain access
   * to servers, RequestHandlers, UsageDescriptions,etc.</b>
   */
  public final static AvailableServers getAvailableServers() {
     return runningServers;
  }


  /**
     * Gets the Singleton instance.
     *
     * @return The singleton router supervisor instance.
     *
     */
	public static Supervisor getSupervisor() {
		return supervisor;
	}


  /**
     * Gets the Adminserver that started the supervisor.
     *
     * @return 	The AdminServer.
     *
     */
  public RouterAdminServer getAdminServer ( ) {
     return (RouterAdminServer)getLoadingServer();
  }

  /**
   * gets a property of the RouterSupervisor.
   * @param name The name of the property to obtain
   * @return String the property value or null if the property doesn't exist.
   */
  public String getProperty(String name)
  {
     return getPropertyValue(name);
  }

  /**
   * used to set the admin server
   */
  protected void setLoadingServer (ServerBase adminServer) {
     super.setLoadingServer(adminServer);
  }

  /**
   * initializes the RouterSupervisor. This never be called
   * by MessageDirector.
   * @param key The property key
   * @param type The property type
   */
  public void initialize(String key, String type) throws ProcessingException  {
    // use local caching so that different orb locations can be contacted.
    ObjectLocator.useLocalCache(true);
    
    super.initialize(key,type);
    
    if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "Starting RouterSupervisor...");

    eventConnections = new HashMap();
    objLocators   = new HashMap();

    List tObjLocatorsList = new LinkedList(); // temp list used just for intialization
    String channelName;
    StringBuffer errorBuf = new StringBuffer();


    channelName = getRequiredPropertyValue(SPI_EVENT_CHANNEL,errorBuf);
    cosPrefix = getRequiredPropertyValue(COS_NS_PREFIX_PROP,errorBuf);

    if (errorBuf.length() > 0 )
       throw new ProcessingException(errorBuf.toString());


    // ************ first register any event listeners first.
    if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        Debug.log(Debug.SYSTEM_CONFIG, "RouterSupervisor:: checking for alternative orb locations");
    // check for alterntivate orb locations

    addAltCorbaLocations(channelName, tObjLocatorsList);

    // register the default orb location
    if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        Debug.log(Debug.SYSTEM_CONFIG, "RouterSupervisor:: checking for default orb location");
     
    addDefaultOrbLocation(channelName, tObjLocatorsList );

     //*********   next check the naming service for any servers already running

     // get a listing from the naming service
     // Some server may have already registered by an event
     // but we want to make sure we don't miss any servers.
     // If we don't register event listeners first, then the following condition could cause the
     // router to miss a server:
     // 1. The router and a SPI is started.
     // 2. The router gets a naming service listing before the SPI adds its name.
     // 3. The SPI then adds its name and posts a startup event.
     // 4. The Router registers to the event channel and misses the startup event.
     // 5. The Router misses the SPI.
    
    if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
        Debug.log(Debug.SYSTEM_CONFIG,"RouterSupervisor:: checking NS for servers");
    
    try
    {
          checkNS(tObjLocatorsList, cosPrefix);
    }
    catch (Exception e)
    {
         Debug.warning("RouterSupervisor: Failed to look-up servers via NS.  Reason: [" + e.getMessage() + "]");
    }

        checkDB(cosPrefix);

      if (Debug.isLevelEnabled(Debug.MSG_STATUS) ) 
      {
        DescribeVisitor visitor = new DescribeVisitor();
        runningServers.traverseServerObjects(visitor);
      
        Debug.log(Debug.MSG_STATUS, "RouterSupervisor has references to the following servers: \n"+
                visitor.getDescription() );
      }
      
	}

    //checks default orb addresses and ports and registers event listeners
    // if any exist
    // tObjLocatorsList is a list where object locators can be added to so that those orb can be contacted.
    //
    private void addDefaultOrbLocation(String channelName, List tObjLocatorsList) throws ProcessingException
    {
        String addr = getLoadingServer().getProperty(ServerBase.ORB_AGENT_ADDR_PROP);
        
        String port = getLoadingServer().getProperty(ServerBase.ORB_AGENT_PORT_PROP);

        if (StringUtils.hasValue(addr) || StringUtils.hasValue(port))  
        {
            defaultConnection = registerEventListener(getLoadingServer().getCorbaPortabilityLayer(), channelName, addr, port);
            
            tObjLocatorsList.add(0, new LocatorInfo((ObjectLocator)objLocators.get(addr + SEP + port), addr, port));
        }
        else 
        {
            Debug.error("ERROR: RouterSupervisor.addDefaultOrbLocation(): Default properties [" +  ServerBase.ORB_AGENT_ADDR_PROP + 
                        "] and [" + ServerBase.ORB_AGENT_PORT_PROP + "] are not set.");
        }
    }

    //checks for any alternative orb addresses and ports and registers event listeners
    // if any exist
    // tObjLocatorsList is a list where object locators can be added to so that those orb can be contacted.
    //
    private void addAltCorbaLocations(String channelName, List tObjLocatorsList) //throws ProcessingException
    {
        // First, register any event listeners.

        for (int i = 0; true; i++) 
        {
            String addr = getPropertyValue(PersistentProperty.getPropNameIteration(ALT_ORB_ADDR_PREFIX, i));

            String port = getPropertyValue(PersistentProperty.getPropNameIteration(ALT_ORB_PORT_PREFIX, i));

            if (!StringUtils.hasValue(addr) && !StringUtils.hasValue(port))
            {
                break;
            }

            if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            {
                Debug.log(Debug.SYSTEM_CONFIG, "RouterSupervisor.addAltCorbaLocations(): Intializing an alternative ORB with address [" + addr + "] and port [" + port + "] ...");
            }

            try
            {
                CorbaPortabilityLayer altCPL = createCPL(addr, port);
                
                registerEventListener(altCPL, channelName, addr, port);
                
                altCPL.activatePOAManager();
            }
            catch (Exception e)
            {
                Debug.log(Debug.ALL_ERRORS, "ERROR: RouterSupervisor.addAltCorbaLocations(): Encountered an error while registering event listener for channel [" + channelName + "] at address and port [" + addr + ", " + port + "]:\n" + e.toString());
                
                continue;
            }
            
            // This is only done for initialization.
            
            tObjLocatorsList.add(new LocatorInfo((ObjectLocator)objLocators.get(addr + SEP + port), addr, port));
        }
    }

  // checks the naming service for any servers.
  // tObjectLocatorList is used
  // tObjLocatorsList is a list of object locators to get NS listings from
  private void checkNS(List tObjLocatorsList, String cosPrefix) throws ProcessingException{
     ObjectLocator loc = null;
     List cosList = new ArrayList();
//     String addr, port;
     LocatorInfo locInfo;
     // loop through all locators in the order that they were added
     Iterator tLocIter =  tObjLocatorsList.iterator();
     
     while (tLocIter.hasNext() ) {
        cosList.clear();  // set the cos list to zero

        try {

           locInfo =  (LocatorInfo)tLocIter.next();
           loc = locInfo.loc;
           if (loc == null) {
               if (Debug.isLevelEnabled(Debug.MSG_WARNING) )
                    Debug.log(Debug.MSG_WARNING, "Null locator in locator list, skipping");
              continue;
           }
           // then get a list of servers
           depthFirstServerRetrieve(cosList,loc,cosPrefix);

           //register all servers
           Iterator iter = cosList.iterator();
           String serverName;
           // loop through all servers for this specific orb location
           while (iter.hasNext()) {
             serverName = (String)iter.next();
             if (StringUtils.hasValue(serverName) ) {
                 
                 if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                    Debug.log(Debug.SYSTEM_CONFIG, "Registering server found in naming service: " + serverName);
                
                 registerServer( locInfo.address, locInfo.port, RunningServers.STARTUP_EVENT_ID, serverName );
             }  else 
             {
                Debug.log(Debug.ALL_WARNINGS, "Skipping empty server name");
             }
          }
        } catch (CorbaException ce) {

           // there is no way to distinguish between no servers found or some other error
           if ( ce.getStatusCode() != CorbaException.NAMING_SERVICE_UNAVAILABLE ) {
              Debug.log(Debug.ALL_WARNINGS, "WARNING: No servers were found with NS prefix: " + cosPrefix );
           }
           else
           {
               Debug.log(Debug.ALL_ERRORS, "Error obtaining server listing from Naming Service : " + ce.getMessage());
               throw new ProcessingException("Error obtaining server listing from Naming Service : " + ce.getMessage());
           }
        }
     }
   }

    /**
     * Checks the database for the list of available servers.
     * @param cosPrefix Servers with name starting with the 'cosPrefix' would be registered.
     */
    private void checkDB(String cosPrefix)
    {
        List serverNamesList = new ArrayList();
        String addr = getLoadingServer().getProperty(ServerBase.ORB_AGENT_ADDR_PROP);
        String port = getLoadingServer().getProperty(ServerBase.ORB_AGENT_PORT_PROP);

        try
        {
            // get the list from PersistentIOR
            serverNamesList = PersistentIOR.getAll(cosPrefix);
        }
        catch (DatabaseException dbe)
        {
            Debug.error("RouterSupervisor: Database Exception" + dbe.getMessage());
        }

        //register all servers
        Iterator iter = serverNamesList.iterator();
        String serverName;

        // loop through all servers for this specific orb location
        while (iter.hasNext())
        {
            serverName = (String)iter.next();
            if (StringUtils.hasValue(serverName) )
            {
                if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                {
                    Debug.log(Debug.SYSTEM_CONFIG, "Registering server found in database: " + serverName);
                }
                registerServer( addr, port, RunningServers.STARTUP_EVENT_ID, serverName );
            }
            else
            {
                Debug.log(Debug.ALL_WARNINGS, "Skipping empty server name");
            }
        }
    }

    //register event listeners to channels
    private EventConnection registerEventListener(CorbaPortabilityLayer cp, String channelName, String addr, String port) throws ProcessingException 
    {        
        EventListener eventListener = new EventListener();
        
        eventListener.ORBagentPort  = port;
        eventListener.ORBagentAddr  = addr;

        try 
        {
            EventConnection eventConn = new EventConnection();

            eventConn.cp              = cp;
            
            eventConn.consumer        = new EventPushConsumer(eventConn.cp.getORB(), channelName, false);

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "RouterSupervisor.registerEventListener(): Registering a Push Consumer to channel [" +
                                            channelName + "] at ORB address [" + addr + "] and port [" + port + "] ...");
            }
            
            eventConn.consumer.register(eventListener);
            
            eventConnections.put(addr + SEP + port, eventConn);

            objLocators.put(addr + SEP + port,  new ObjectLocator(eventConn.cp.getORB()));

            return eventConn;                
        } 
        catch (Exception e) 
        {
            throw new ProcessingException("ERROR: RouterSupervisor.registerEventListener(): Failed to connect to channel [" + channelName + 
                                          "] at ORB address [" + addr + "] and port [" + port + "]:" + e.toString());
        }
    }

    private CorbaPortabilityLayer createCPL(String addr, String port) throws CorbaException
    {
        Properties props = null;

        if (StringUtils.hasValue(addr))
        {
            if (props == null)
            {
                props = new Properties();
            }

            props.put(ORB_AGENT_ADDR, addr);
        }

        if (StringUtils.hasValue(port))
        {
            if (props == null)
            {
                props = new Properties();
            }

            props.put(ORB_AGENT_PORT, port);
        }
        
        return new CorbaPortabilityLayer(null, props, ALT_ORBS_POA_NAME);
    }

 /**
  * This registers a server with the router
  */
  private final void registerServer(String addr, String port, int type, String serverName)
  {

     if ( !serverName.startsWith(cosPrefix) ) {
        Debug.log(Debug.ALL_WARNINGS, "RouterSupervisor:: event type not supported, skipping : " + serverName);
        return;
     }

     if (serverName.equals(cosPrefix) ) {
         if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "RouterSupervisor: skipping root of naming service");
        return;
     }

     if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS, "RouterSupervisor:: updating internal server cache : " + serverName);

     ObjectLocator loc = (ObjectLocator) objLocators.get(addr+SEP+port);


     try {

        if ( loc == null) {
           throw new ProcessingException("Could not obtain an object locator for address[" +
                                          addr + "] and port[" + port +"]" );
        }


        runningServers.updateSPIInfo(addr, port, loc, type, serverName);

     } catch (ProcessingException pe) {
        Debug.log(Debug.ALL_ERRORS, "ERROR: Problems updating server [" + serverName + "] status:  " + pe.getMessage());
     }

  }

    /**
     * Shuts down supervisor
     */
    public void shutdown() 
    {
        if (Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "RouterSupervisor.shutdown(): Shutting down the Router Supervisor ...");
          
        Iterator iter = eventConnections.values().iterator();

        while (iter.hasNext())
        {
            EventConnection e = (EventConnection) iter.next();

            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            {
                Debug.log(Debug.OBJECT_LIFECYCLE, "RouterSupervisor.shutdown(): Before disconnecting ...");

                if (e == defaultConnection)
                    Debug.log(Debug.OBJECT_LIFECYCLE, "RouterSupervisor.shutdown(): Disconnecting the default ...");

                Debug.log(Debug.OBJECT_LIFECYCLE, "RouterSupervisor.shutdown(): About to disconnect ...");
            }

            e.consumer.disconnect();

            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, "RouterSupervisor.shutdown(): After disconnecting ...");
            
            if (e != defaultConnection)
            {
                e.cp.shutdown();
            }
        }

        super.shutdown();
    }

    // internal class used to monitor for events
    private final class EventListener implements PushConsumerCallBack
    {
        public  String ORBagentPort;
        public  String ORBagentAddr;
        
        private String serverName = null;
        
        private int    type;


        public final void processEvent(String event) 
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "EventListener.processEvent(): Received an event [" + event + "] from channel at address [" +
                                            ORBagentAddr + "] and port [" + ORBagentPort + "].");
            }

            if (event.endsWith(RunningServers.STARTUP_EVENT_SUF)) 
            {
                serverName = event.substring(0, event.indexOf(RunningServers.STARTUP_EVENT_SUF));
                
                type       = RunningServers.STARTUP_EVENT_ID;
            }
            else if (event.endsWith(RunningServers.SHUTDOWN_EVENT_SUF)) 
            {
                serverName = event.substring(0, event.indexOf(RunningServers.SHUTDOWN_EVENT_SUF));
                
                type       = RunningServers.SHUTDOWN_EVENT_ID;
            } 
            else
            {
                Debug.error("ERROR: EventListener.processEvent(): Received an unknown event type [" +  event + "].");
            }

            registerServer(ORBagentAddr, ORBagentPort, type, serverName);
        }
    }


   /**
    * does a depth first search on naming service context. Servers
    * can have a name that goes down any number of levels.\
    *
    * @param servers a list which ends up with all of the servers. Expanded from the root to leaf nodes.
    * @param loc The object locator used to get naming service listings.
    * @param cosPrefix The starting point to get a listing from. The value of this field
    * is started at some root point.
    * ALGORITHM: The prefix starts at some root point.A list is obtained using this prefix.
    * For each list the method calls its self with an new cosPrefix of the current cosPrefix with a listing appended to it.
    * When the search can't get a listing (reached a leaf node) that server name is added to the servers list.
    * is then added onto while doing a depth first search.

    */
   private void depthFirstServerRetrieve(List servers, ObjectLocator loc, String cosPrefix) throws CorbaException{
      ObjectLocator.NamingContextInfo[] list = null;

      if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
         Debug.log(Debug.MSG_STATUS, "Looking for nodes under prefix : " + cosPrefix);

      try {

         list =  loc.list(cosPrefix);
         //stop case we are at a leaf node and can not get a listing.
         if (list == null)  {

             if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                Debug.log(Debug.SYSTEM_CONFIG, "Adding server name: " + cosPrefix);

           servers.add(cosPrefix);
           return;
         }

      } catch ( CorbaException ce) {


         // throw the exception if the naming service is down.
         if (ce.getStatusCode() == CorbaException.NAMING_SERVICE_UNAVAILABLE )
            throw ce;

          // other stop case, we are at a leaf node and can not get a listing.
         if (Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
            Debug.log(Debug.SYSTEM_CONFIG, "Adding Server name: " + cosPrefix);

         servers.add(cosPrefix);
         return;

      }

      // search each of the nodes obtained in the ObjectLocator.NamingContextInfo[]  listing.
      for (int i = 0; i < list.length; i++ ) {
         depthFirstServerRetrieve(servers,loc, cosPrefix + DOT + list[i].name);
      }
   }
}
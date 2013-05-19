/*
 * Copyright NeuStar, Inc., 2006
 * This file contains confidential and proprietary information and may not be
 * distributed without prior written consent.
 */
package com.nightfire.framework.util;

import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.net.URI;

import com.nightfire.framework.monitor.*;
import com.nightfire.framework.rmi.RMIProxy;
import com.nightfire.framework.cache.*;

/**
 * RMI interface for objects supporting remote operations.
 */

public class  RemoteOperationsAdminImpl implements RemoteOperationsAdmin, RemoteThreadMonitor
{
   /**
    * Property indicating the name of the RMI server 
    * in "//<rmiregistry-host>:<rmiregistry-port>/<service-name>" format.
    */
   public static final String REMOTE_ADMIN_SERVER_PROP = "REMOTE_ADMIN_SERVER";

   /**
    * Property indicating the port that the RMI server should listen on.  (Optional - If not set
    * UnicastRemoteObject chooses a random port.)
    */
   public static final String REMOTE_ADMIN_SERVER_PORT_PROP = "REMOTE_ADMIN_SERVER_LISTEN_PORT";


   private RemoteOperationsAdminImpl ( )
   {
      Debug.log( Debug.SYSTEM_CONFIG, "Creating Remote Utility RMI object." );
   }


   /**
    * Initialize the remote system according to given configuration 
    * in the properties.  At a minimum, the server name should be set.
    * (This will also initialize the thread monitoring sub-system.)
    *
    * @param  props  Container of configuration properties.
    */
   public static void initialize ( Map props )
   {
      try
      {
         if ( rtm != null )
         {
            Debug.warning( "Remote Utility RMI Server is already initialized." );

            return;
         }

         // Make sure that the thread monitoring sub-system is initialized before
         // launching the server object providing remote access to it.
         ThreadMonitor.initialize( props );

         // If RMI server name is configured, start the Remote Thread Monitor RMI server object.
         serverName = (String)props.get( REMOTE_ADMIN_SERVER_PROP);

         if ( serverName != null )
         {
            Debug.log( Debug.SYSTEM_CONFIG, "Initializing Utility RMI Server [" + serverName + "] ..." );

            rtm = new RemoteOperationsAdminImpl( );

            String port = (String)props.get( REMOTE_ADMIN_SERVER_PORT_PROP );

            try 
            {
               Debug.log( Debug.SYSTEM_CONFIG, "Exporting Utility RMI Server object [" + serverName + "]." );

               if ( port == null )
               {
                  RMIProxy.getInstance().exportObject(rtm);
               }
               else
               {
                  Debug.log( Debug.SYSTEM_CONFIG, "Utility RMI Server object will listen on port [" + port + "]." );

                  RMIProxy.getInstance().exportObject(rtm,Integer.parseInt( port ) );
               }
            } 
            catch ( Exception e ) 
            {
               Debug.error( "Couldn't export RMI server [" + serverName + "]: " + e.toString() );

               rtm = null;

               return;
            }

            try 
            {
               if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                  Debug.log( Debug.SYSTEM_CONFIG, "Binding Remote Utility object [" + serverName + "] to RMI registry." );

               URI uri = new URI(serverName);
               
               // extract the object name
               String objName = uri.getPath().substring(1,uri.getPath().length());
               
               RMIProxy.getInstance().rebind(objName, rtm);
               
               if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                  Debug.log( Debug.SYSTEM_CONFIG, "Binded with name :"+objName );
              
                //Naming.rebind( serverName, rtm );   
            } 
            catch ( Exception e ) 
            {
               Debug.error( "Couldn't bind RMI server [" + serverName + "] to the RMI registry: " + e.toString() );
            }
         }

         if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "Done initializing Remote Utility Server." );
      }
      catch ( Exception e )
      {
         Debug.error( "Couldn't initialize rmote RMI Utility server due to the following error: " + e.toString() );

         rtm = null;
      }
   }


   /**
    * Shutdown the RMI utility server object, which removes its name from the RMI registry.
    * The RMI server won't shut down until all remote clients and the registry are unbound.
    */
   public static void shutdown ( )
   {
      Debug.log( Debug.SYSTEM_CONFIG, "Shutting down Remote Utility Server [" + serverName + "] ..." );

      if ( rtm == null )
      {
         Debug.warning( "Remote Utility server wasn't initialized." );

         return;
      }

      try 
      {
         Naming.unbind( serverName );  
      } 
      catch ( Exception e ) 
      {
         Debug.error( "Couldn't unbind Remote Utility server [" + serverName + "] from the registry: " + e.toString() );
      }

      try
      {
         UnicastRemoteObject.unexportObject( rtm, false );
      }
      catch( Exception e )
      {
         Debug.error( "Couldn't unexport Remote Utility server: " + e.toString() );
      }

      Debug.log( Debug.SYSTEM_CONFIG, "Done shutting down Remote Utility server [" + serverName + "]." );

      rtm = null;
   }


   /**
    * Describe the current set of threads with monitors in the remote application that 
    * have been processing for longer than the indicated time.
    *
    * @param  maxSeconds  The maximum time threshold (in seconds) above which any 
    *                     threads that are still processing should be reported on.
    * 
    * @return  A human-readable description of the threads that have been processing for
    *          longer than the indicated time.
    */
   public String report ( int maxSeconds ) throws java.rmi.RemoteException
   {
      String result = ThreadMonitor.describeThreads( maxSeconds );

      if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ) )
         Debug.log( Debug.NORMAL_STATUS, ThreadMonitor.ASTERISK_LINE + result + ThreadMonitor.ASTERISK_LINE );

      return result;
   }

   /**
    * Flushes cached resource which are registerd with singleton object
    * Cache Manager.
    *
    */
    public void flushCache() throws java.rmi.RemoteException
   {

        try
        {
            Debug.log( Debug.SYSTEM_CONFIG, "Flushing cached resources ..." );
            CacheManager.getManager().flushCache( );
            Debug.log( Debug.SYSTEM_CONFIG, "Done flushing cached resources." );

        }
        catch ( Exception e )
        {
            Debug.error( "Could not flush cached resources:\n" + e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
   }


   /**
    * Unit-test driver.
    *
    * @param  args  Command-line arguments:
    *               args[0] = name of the RMI server.
    */
   public static void main ( String[] args )
   {
      if ( args.length < 1 )
      {
         System.out.println( "\n\nUSAGE: RemoteOperationsAdminImpl <server-name>\n\n" );

         System.exit( -1 );
      }

      Debug.enableAll( );

      ThreadMonitor.ThreadInfo ti = ThreadMonitor.start( "Hello remote threaded world!" );

      try
      {
        /*
          if ( System.getSecurityManager() == null )
          System.setSecurityManager( new RMISecurityManager() );
        */
         System.out.println( "Creating server [" + args[0] + "]." );

         Hashtable config = new Hashtable( );

         config.put( ThreadMonitor.THREAD_MONITOR_LOG_LEVEL_PROP, "2" );
         config.put( ThreadMonitor.THREAD_MONITOR_MAX_TIME_THRESHOLD_PROP, "0" );
         config.put( ThreadMonitor.THREAD_MONITOR_SAMPLE_PERIOD_PROP, "5" );
         config.put( REMOTE_ADMIN_SERVER_PROP, args[0] );

         RemoteOperationsAdminImpl.initialize( config );

         // Stop main thread so that it doesn't exit and will
         // display wait time when server gets called.
         Object lock = new Object( );
  
         synchronized ( lock )
         {
            lock.wait( );
         }      
      }
      catch ( Exception e )
      {
         System.err.println( e );

         e.printStackTrace( );
      }
      finally
      {
         RemoteOperationsAdminImpl.shutdown( );

         ThreadMonitor.stop( ti );
      }
   }


   // Single remote monitor server instance for this JVM process.
   private static RemoteOperationsAdminImpl rtm = null;

   // Name of RMI server in "//<rmiregistry-host>:<rmiregistry-port>/<service-name>" format.
   private static String serverName;
}

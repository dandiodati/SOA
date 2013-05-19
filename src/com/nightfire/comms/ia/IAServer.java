/**
 * Copyright(c) 2000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia;

// JDK import
import java.io.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;
import java.security.KeyStore;
import javax.security.cert.X509Certificate;
import java.util.Properties;
import java.util.LinkedList;

// thidparty imports
import org.apache.log4j.PropertyConfigurator;

// Nightfire import
import com.nightfire.spi.common.communications.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.message.*;
import com.nightfire.adapter.util.*;
import com.nightfire.comms.util.*;
import com.nightfire.comms.soap.ssl.SSLUtils;
import  com.nightfire.framework.util.ThreadPoolExecutorService;

import com.nightfire.framework.monitor.*;



/**
* IAServer starts a ServerSocket and listens for client's request.
* When a new connection is accepted, it passes a Socket to IAReceiver.
* IAReceiver runs in a new thread and passes data to IAServer's processData
* method which builds a driver and processes the data. IAServer uses
* DataExceptionHandler to handle exception when processData method fails to
* process the data.
*/

public class IAServer extends ComServerBase {

  /**
  * SSLPortabilityLayer that wraps an SSL implementation
  */
  private SSLPortabilityLayer sslLayer;

  /**
  * DataExceptionHandler
  */
  private DataExceptionHandler dataExceptionHandler;

  /**
  * ServerSocket that IAServer creates
  */
  private ServerSocket serverSocket = null;

  /**
  * Server port number used by IAServer
  */
  private int portNum;

  /**
  * Flag used to interrupt thread
  */
  private volatile boolean stopThread = false;

  /**
  * Flag indicating if trimming trailing white spaces in data is needed
  */
  private boolean trimWhiteSpace = true;

  /**
  * Property name for server port number
  */
  private static final String SERVER_PORT_NUMBER_PROP = "IA_SERVER_PORT_NUMBER";

  /**
  * Property name for SSLPortabilityLayer type
  */
  private static final String SSL_TYPE_PROP = "SSL_TYPE";

  /**
  * Property name for flag used to trim trailing white spaces in data
  */
  private static final String TRIM_WHITE_SPACES_PROP = "TRIM_WHITE_SPACES";

  /**
  * DataExceptionHandler property name for file base name
  */
  private static final String EXCEPTION_FILE_BASE_NAME_PROP = "EXCEPTION_FILE_BASE_NAME";

  /**
  * DataExceptionHandler property name for file extension
  */
  private static final String EXCEPTION_FILE_EXTENSION_PROP = "EXCEPTION_FILE_EXTENSION";

  /**
  * DataExceptionHandler property name for exception directory
  */
  private static final String EXCEPTION_DIR_PROP = "EXCEPTION_DIR";

  /**
  * DataExceptionHandler property name for error directory
  */
  private static final String ERROR_DIR_PROP = "ERROR_DIR";
  
  /**
   * Property name to specify the IA Issue version this Server should use.
   */
  private static final String IA_ISSUE_VERSION_PROP = "IA_ISSUE_VERSION";
  
  /**
   * Property name to specify the IA Issue version this Server should use.
   */
  private static final String CLIENT_AUTH_PROP = "CLIENT_AUTH_ENABLED";
  
  
  /**
   * Constant for IA_ISSUE2
   */
  public static final String IA_ISSUE2 = "2";
    
   /**
   * Constant for IA_ISSUE3
   */
  public static final String IA_ISSUE3 = "3";
  

  /**
   * Used to track whether Log4j has been initialized
   */
  private static boolean log4jInit = false;
  
  private String sslLayerType;
  
  /**
   * IA Isssue version for this server.
   * Default is IA_ISSUE2
   */
  private String iaIssueVersion = IAServer.IA_ISSUE2;
  
  /**
   * Whether to enable client authoriztion for this server.
   * IA_ISSUE3 specific, i.e. is not used in IA_ISSUE2
   */
  private boolean clientAuth = true;

  
  private LinkedList receivers = new LinkedList();

/**
   * Whether to enable IAReceiver Thread Pooling for this server or not.
   * 
   */
  private static String THREAD_POOL_REQUIRED_PROP =   "THREAD_POOL_REQUIRED";

/**
   * Thread Pool will be off by default.
   * 
   */
  private boolean threadPoolRequired = false;

/**
   * If Thread Pool is on then MAX_POOL_SIZE and CORE_POOL_SIZE .
   * are required parameters for configuring Thread Pool.
   */
  private static String MAX_POOL_SIZE_PROP = "MAX_POOL_SIZE";
  
  private static String CORE_POOL_SIZE_PROP =   "CORE_POOL_SIZE";
  
  private int maxPoolSize = 0;
  
  private int corePoolSize = 0;

  /**
   * Thread Pool Executor.
   * 
   */
  private ThreadPoolExecutorService executor = null; 
 
  /**
  * Constructor
  *
  * @param  key   Property-key to use for locating initialization properties.
  *
  * @param  type  Property-type to use for locating initialization properties.
  *
  * @exception ProcessingException Thrown when initialization fails
  */
  public IAServer(String key, String type) throws ProcessingException, FrameworkException {

    super(key, type);

    StringBuffer errorMsg = new StringBuffer();
    String tmp;

    tmp = getPropertyValue(THREAD_POOL_REQUIRED_PROP);
    if ( StringUtils.hasValue(tmp) )
    {
        try 
        {
            threadPoolRequired = StringUtils.getBoolean(tmp);
        }
        catch ( FrameworkException fe )
        {
            threadPoolRequired = false;
            Debug.log(Debug.ALL_WARNINGS, "Invalid value ["+tmp+"] for property [" 
                                      + THREAD_POOL_REQUIRED_PROP + "], using default value ["
                                      + threadPoolRequired + "]" );
            errorMsg.append("Invalid value [" +tmp+"] for property ["+ THREAD_POOL_REQUIRED_PROP+"].");

        }
    }

    if(threadPoolRequired)                
    {
        //getting maximum pool size
        tmp = getPropertyValue(MAX_POOL_SIZE_PROP);

        try
        {
            maxPoolSize   =  StringUtils.getInteger(tmp);
        
        }
        catch(FrameworkException fe)
        {
            maxPoolSize = 0;
            Debug.log(Debug.ALL_WARNINGS,"Invalid value [" +tmp+"] for property ["+ MAX_POOL_SIZE_PROP+"].");
            errorMsg.append("Invalid value [" +tmp+"] for property ["+ MAX_POOL_SIZE_PROP+"].");
        }
        
        //getting core pool size
        tmp = getPropertyValue(CORE_POOL_SIZE_PROP);

        try
        {
            corePoolSize   =  StringUtils.getInteger(tmp);
        }
        catch(FrameworkException fe)
        {
            corePoolSize=0;
            Debug.log(Debug.ALL_WARNINGS, "Invalid value [" +tmp+"] for property ["+ CORE_POOL_SIZE_PROP+"].");
            errorMsg.append("Invalid value [" +tmp+"] for property ["+ CORE_POOL_SIZE_PROP+"].");
        }
        if(corePoolSize > maxPoolSize )
        {
            Debug.log(Debug.ALL_WARNINGS,  CORE_POOL_SIZE_PROP+" value should be less than "+MAX_POOL_SIZE_PROP+" value.");
            errorMsg.append("Invalid values for Pool Size. "+CORE_POOL_SIZE_PROP+" value should be less than "+MAX_POOL_SIZE_PROP+" value.");
        }
    }
    
    // turn on Cryptix debug logging
    initCryptixLogging();

    //get property values for DataExceptionhandler
    String excpBaseName = getRequiredPropertyValue(EXCEPTION_FILE_BASE_NAME_PROP, errorMsg);
    String excpExtension = getRequiredPropertyValue(EXCEPTION_FILE_EXTENSION_PROP, errorMsg);
    String excpDir = getRequiredPropertyValue(EXCEPTION_DIR_PROP, errorMsg);
    String errorDir = getRequiredPropertyValue(ERROR_DIR_PROP, errorMsg);
    tmp = getPropertyValue(IA_ISSUE_VERSION_PROP);

    //get IA Issue Version
    if ( StringUtils.hasValue(tmp) && isValidIssueVersion(tmp) )
    {
        iaIssueVersion = tmp;
    }
    else 
    {
        Debug.log(Debug.ALL_WARNINGS, "Invalid value ["+tmp+"] for property [" 
                                      + IA_ISSUE_VERSION_PROP + "], using default value ["
                                      + iaIssueVersion + "]" );
    }   
    
    
    //get whether Client Authorization should be enabled.
    tmp = getPropertyValue(CLIENT_AUTH_PROP);
    
    if ( StringUtils.hasValue(tmp) )
    {
        try 
        {
            clientAuth = StringUtils.getBoolean(tmp);
        }
        catch ( FrameworkException fe )
        {
            Debug.log(Debug.ALL_WARNINGS, "Invalid value ["+tmp+"] for property [" 
                                      + CLIENT_AUTH_PROP + "], using default value ["
                                      + clientAuth + "]" );
        }
    }

    //get trim white space flag
    try
    {
      trimWhiteSpace = StringUtils.getBoolean( getRequiredPropertyValue(TRIM_WHITE_SPACES_PROP, errorMsg) );
    } catch (FrameworkException e)
    {
      errorMsg.append ("Failed to convert trimWhiteSpace to boolean\n");
    }

    //get server port number
    try
    {
      portNum = Integer.parseInt( getRequiredPropertyValue(SERVER_PORT_NUMBER_PROP, errorMsg) );
    } catch (NumberFormatException e)
    {
      errorMsg.append ("Failed to convert port number to integer\n");
    }

    //get sslLayerType used for initializing sslLayer
    sslLayerType = getRequiredPropertyValue(SSL_TYPE_PROP, errorMsg);

    //throw an exception if errorMsg is not empty
    if (errorMsg.length() != 0)
      throw new ProcessingException (
        "The following errors occured while initializing IAServer:\n" + errorMsg.toString() );

      // redirect ssl logging to debug
     System.setOut(new StdOutPrintStream() );

     if ( iaIssueVersion.equals("2") )
     {
         
        //get SSLProtabilityLayer object and initialize it.
        sslLayer = new SSLPortabilityLayer();
        sslLayer.initialize(key, sslLayerType);
     }
     else
     {
        SSLUtils.initializeOnce();
     }

    //get DataExceptionhandler object and initialize it.
    dataExceptionHandler = new DataExceptionHandler(excpBaseName, excpExtension, excpDir, errorDir);

    //initialize IAReceiver
    IAReceiver.initialize(key, type);
    
    //initialize Thread Pool
    if(threadPoolRequired)
    {
        executor =  new ThreadPoolExecutorService(corePoolSize, maxPoolSize);
    }

    
  }

  /**
  * Return sslLayer
  */
  public SSLPortabilityLayer getSSLLayer()
  {
    return sslLayer;
  }

  /**
  * The run method starts the server socket and listens for client's request.
  * Upon receiving a request from client, it creates a socket, passes it
  * to IAReceiver, and starts the IAReceiver in a separate thread.
  */
  public void run()
  {
   
    long startTime = 0;
    ThreadMonitor.ThreadInfo tmti = null;

    try
    {
        
      Debug.log( Debug.BENCHMARK, "BEGIN " + "IAServer.creating socket..." );

      startTime = System.currentTimeMillis( );
      
      serverSocket = getServerSocket();
                   
      Debug.log(this, Debug.NORMAL_STATUS, "IAServer.run: server socket on port " +
        portNum + " created successfully");

      Debug.log( Debug.BENCHMARK, "END " + "IAServer.created socket" 
                        + " ELAPSED TIME [" + (System.currentTimeMillis() - startTime) + "] msec." );
    } 
    
    catch (Exception e)
    {
      Debug.log(this, Debug.ALL_ERRORS,
        "ERROR: IAServer.run: creating server socket on port " + portNum +
        " failed: " + e.getMessage());
      return;
    }

    while ( !stopThread )
    {
      try
      {
            startTime =  System.currentTimeMillis( );
            Debug.log( Debug.BENCHMARK, "BEGIN: IAServer.accepting socket ..." );
            Socket ss = accept(serverSocket);

            tmti = ThreadMonitor.start( "IAServer: Accepted socket to receive response.\n  Remote Socket info: " + ss.toString()
                + "\n  Local Server socket info: " + serverSocket.toString());

            Debug.log( Debug.BENCHMARK, "END: " + "IAServer.accepting socket");
            Debug.log(this, Debug.NORMAL_STATUS, "IAServer: accepted a connection from " +
              serverSocket.getInetAddress().toString() );
            IAReceiver receiver = new IAReceiver(ss, this, iaIssueVersion );      
            if(threadPoolRequired)
            {
                executor.execute(receiver);
            }
            else
            {
                Thread t = new Thread(receiver);
                t.start();
            }

            if ( iaIssueVersion.equals("3") )
            {
                receivers.add(receiver);
            }

      }
      catch (Exception e) 
      {
          if (stopThread)
          {
              // PLEASE DO NOT CHANGE THIS LOG LEVEL

              // This is logged as a status message because the server can
              // only be shut down by closing it's socket, which throws
              // an exception (even though it's not an error)
              Debug.log(this, Debug.NORMAL_STATUS,
                        "IAServer: stopping after exception " + e.toString());
          }
          else
          {
              // this is an exception other than one to stop the server
              Debug.log(this, Debug.ALL_ERRORS,
                        "IAServer caught an exception: " + e.toString());
              Debug.logStackTrace(e);
          }
      }
     /* catch (Throwable t) {
      * 
          Debug.error("IAServer: Fatal runtime error occured : " + t.getMessage());
          Debug.logStackTrace(t);        
      }*/
      finally {
          ThreadMonitor.stop( tmti );
      }


    }// while ( !stopThread )

    // clean up the socket
    try
    {
        Debug.log(this, Debug.IO_STATUS, "IAServer: closing server socket");
        serverSocket.close();
    }
    catch (IOException e)
    {
        Debug.log(this, Debug.IO_ERROR, e.toString());
    }
  }

  /**
  * Called to shut down the server
  */
  public void shutdown() {
    stopThread = true;

    if( serverSocket != null )
    {
      try
      {
        Debug.log(this, Debug.IO_STATUS, "IAServer is shutting down.");
        serverSocket.close();

        while ( !receivers.isEmpty() )
        {
            IAReceiver r = (IAReceiver) receivers.removeFirst();
            r.shutdown();
        }

        if ( iaIssueVersion.equals("3") )
        {
            IASocketConnectionPool.shutdown();
        }
        if(threadPoolRequired)
        {
            executor.shutdown();
        }

      } catch (IOException e)
      {
        Debug.log(this, Debug.IO_ERROR, e.toString());
      }
      catch (ResourceException re)
      {
        Debug.log(this, Debug.IO_ERROR, re.toString());
      }
    }

    try
    {
      // flush SSL logging
      System.out.flush();
    }
    catch (Exception ex)
    {
        // ignore
    }
  }

  /**
  * Called by IAReceiver to handle data once it is correctly received
  *
  * @param data  The data to process
  */
  public void processData(String data)
  {
      // process the data using the default character encoding 
      processData( data, null );
  }

  /**
  * Called by IAReceiver to handle data once it is correctly received
  *
  * @param data  The data to process
  * @param encoding The character encoding used for getting the given
  *                 data as a byte array. 
  */
  public void processData(String data, String encoding)
  {

    String header = "";

    if( data != null)
    {
      try
      {
        if( trimWhiteSpace )
            data = data.trim();
        process(header, data);
      }
      catch (Exception e)
      {
        dataExceptionHandler.handleException(e, data, encoding);
      }
    }
  }

  /**
   * Initializes the Log4j framework to capture Cryptix debug logs
   */
  public static void initCryptixLogging()
  {
      /**
       * Since Debug is changed to use Log4j, we just need to add configuration
       * for cryptix logging to the existing Log4j config.
       
   
*/
      // below code is no more required
      synchronized (IAServer.class)
      {
          if (log4jInit)
              return;

          /* these property settings send all Cryptix log events to our
             debug log using a default level of Debug.MSG_DATA */
          Properties props = new Properties();
          props.setProperty("log4j.rootCategory", "DEBUG, A1");
          props.setProperty("log4j.appender.A1",
                            "com.nightfire.framework.util.DebugAppender");
          props.setProperty("log4j.appender.A1.defaultLevel",
                            String.valueOf(Debug.MSG_DATA));
          props.setProperty("log4j.category.cryptix.asn1", "DEBUG");

          // configure logging
          PropertyConfigurator.configure(props);

          // only configure logging once
          log4jInit = true;
      }
  }

  /*
   * Called by IAReceiver to notify the IAServer it no longer needs to 
   * keep track of it
   */
  public void removeReceiver(IAReceiver r)
  {
	  
	  Debug.log( Debug.MSG_STATUS, "Size of receivers list before remove [" 
			  + receivers.size() +"]");
	  
	  receivers.remove( r );

 	  Debug.log( Debug.MSG_STATUS, "Size of receivers list after remove [" 
			  + receivers.size() +"]");
  }

  
  /*
   * This method return a ServerSocket using the appropriate library
   * based on the configured IA Issue version.
   *
   * @return ServerSocket
   *
   * @exception IOException if unable to create the ServerSocket.
   */
  protected ServerSocket getServerSocket() throws Exception
  {
       if ( iaIssueVersion.equals(IA_ISSUE2) )
      {
         //here we are using the PHAOS 
         return sslLayer.getServerSocket(portNum);
      }
      else
      {
        //using JSSE
        SSLServerSocketFactory ssf =
	    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
      
        ServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket( portNum );
      
         //set whether we require clients to authorized themselves to us.
        ((SSLServerSocket)serverSocket).setNeedClientAuth(clientAuth);
        
        return serverSocket;
      }
  }
  
  
   /*
   * This method return a ServerSocket using the appropriate library
   * based on the configured IA Issue version.
   *
   * @param serverSocket to accept
   * @return Socket the accepted socket
   *
   * @exception IOException if unable to create the ServerSocket.
   */
  protected Socket accept( ServerSocket serverSocket ) throws Exception
  {
    if ( iaIssueVersion.equals(IA_ISSUE2) )
    {
        //This is a wraps a PHAOS specific call which accepts the
        //serverSocket WITHOUT doing the SSL handshake.
        return sslLayer.accept( serverSocket );
    }
    else
    {      
       return serverSocket.accept();
    } 
  }
      
      
  
  /**
   *  Determines whether the value passed in is a valid IA Issue version
   *
   *  @param version - the version being verified
   *
   *  @return boolean - whether it is a valid version or not.
   */
  protected boolean isValidIssueVersion(String version)
  {
      if ( version.equals(IA_ISSUE2) || version.equals(IA_ISSUE3) )
          return true;
      else
          return false;
  }
}


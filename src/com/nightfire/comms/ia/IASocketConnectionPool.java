/**
 * Copyright (c) 2002 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: $
 *
 */

package com.nightfire.comms.ia;

// JDK import
import java.util.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;
import java.io.*;
import java.util.*;
import java.security.KeyStore;
import javax.security.cert.X509Certificate;
import com.sun.net.ssl.*;


import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.*;
import com.nightfire.comms.soap.ssl.SSLUtils;

import com.nightfire.comms.ia.asn.*;
import com.nightfire.comms.ia.asn.msg.*;

/**
 * A utility for managing pools of reuseable DB socket connections.
 */
class IASocketConnectionPool extends ResourcePool
{

    private static final byte[] TEST_IA_STATUS_MESSAGE = {00000000};
	
    public static final int DEFAULT_MAX_SIZE = 8;
    
    private String host = null;
    
    private int port;
	
    private static Hashtable instancePool = new Hashtable();

     /**
     * The SSL factory we are using to create SSL sockets
     */    
    private static SSLSocketFactory factory = null;
        
    static
    {

      boolean initialized = true;
      
      try
      {
	      
      	SSLUtils.initializeOnce();

      	factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
      }
      catch (Exception e)
      {
	      Debug.log( Debug.ALL_ERRORS, "Problem occured in creating IASocketConnectionPool.");
	     
	      Debug.logStackTrace(e);

	      initialized = false;
      	
      }

      if ( initialized )
      {

      	Runtime.getRuntime().addShutdownHook
      	(
                new Thread() 
                {  
                    public void run() 
                    { 
                           try 
                           {                               
                               IASocketConnectionPool.shutdown(); 
                           } 
                           catch (Exception x)
                           {
                               Debug.logStackTrace(x); 
			       Debug.log( Debug.ALL_WARNINGS, "Problem occured while shutting down IASocketConnectionPool: " 
				       				 + x.getMessage() );
                           }  
                    }
                }
          );
      }

    }
        
    /**
     * Constructs a DB socket connection pool with the specified parameters.
     *
     * @param    maxSize     Maximum pool size.
     *
     * @exception ResourceException Thrown if pool construction fails.
     */
    private IASocketConnectionPool( int maxSize ) throws ResourceException
    {
        //NOTE:  Since the host and port are not yet set on the object yet
	//we cannot prefill the cache, so the initSize MUST always = 0.
	super(maxSize, 0 );

 	Debug.log( Debug.MSG_STATUS, "Constructing pool ..." );
    } 
    

    private IASocketConnectionPool(String host, int port, int maxSize ) 
	    throws ResourceException
    {
	    this( maxSize );
	    
	    Debug.log( Debug.MSG_STATUS, "Constructing pool with id ["+
						host+":"+Integer.toString(port)+"] ... " );
	    this.host = host;
	    this.port = port;
	    

    }


    public static IASocketConnectionPool getInstance( String host, int port )
	    throws ResourceException
    {
	
	    return IASocketConnectionPool.getInstance(host, port, DEFAULT_MAX_SIZE );

    }


    public static IASocketConnectionPool getInstance(String host, int port, int maxSize )
	    throws ResourceException
    {
	
	Debug.log( Debug.MSG_STATUS, "Getting pool with id ["+
					host+":"+Integer.toString(port)+"]." );
	    
	    IASocketConnectionPool pool;
		synchronized ( IASocketConnectionPool.class )
		{
			pool = (IASocketConnectionPool) instancePool.get( host+":"+Integer.toString(port) );
			if ( pool == null )
			{				
				Debug.log( Debug.MSG_STATUS, "Creating pool with id ["+
					host+":"+Integer.toString(port)+"]." );
				
				pool = new IASocketConnectionPool( host, port, maxSize );
				
				//set number of seconds to wait for a resource
				pool.setMaxResourceWaitTime(60);
				
				//set the number of minutes to release unused resources
				pool.setIdleCleanupTime(60);
						
				instancePool.put(host+":"+Integer.toString(port), pool);
	}
	else
	{
		Debug.log( Debug.MSG_STATUS, "Found pool with id ["+
					host+":"+Integer.toString(port)+"]." );
	}
		}
	
	return pool;

    }

     /**
     * Destroys all resources in free pool and in used pool during garbage collection.
     *
     * @exception ResourceException Thrown if failed to destroy the pool.
     */
    public static void shutdown() throws ResourceException
    {
        Debug.log( Debug.DB_STATUS, "Shutting-down IASocketConnection pool ..." );

        synchronized (IASocketConnectionPool.class)
        {
            // Iterate over all of the connection pools in the container,
            // telling each one to destroy its connections.
            Iterator iter = instancePool.values().iterator( );

            while ( iter.hasNext() )
            {
                IASocketConnectionPool pool = (IASocketConnectionPool)iter.next( );

                if ( pool != null )
                {
                    try
                    {
                        Debug.log( Debug.SYSTEM_CONFIG, "Destroying connections in pool ["
                                   +pool.getPoolId() + "]." );

                        pool.destroyAll();
                    }
                    catch ( Exception e )
                    {
                        Debug.log( Debug.ALL_ERRORS, e.toString() );
                    }
                }
                // Remove the pool from the cache.
                iter.remove( );
            }
        }
    }  
    
    /**
     * Acquires a socket connection from pool.
     *
     * @return A socket connection from pool.
     *
     * @exception ResourceException  Thrown if socket connection can't be obtained.
     */
    public synchronized IASocket acquireSocket() throws ResourceException
    {
        long startTime = Performance.startTiming( Debug.BENCHMARK );

        IASocket socket = (IASocket) super.acquireResource();

        Performance.stopTiming( Debug.BENCHMARK, startTime, "Acquiring IA socket connection." );

        if ( Debug.isLevelEnabled( Debug.DB_STATUS ) )
        {
            try
            {
                Debug.log( Debug.DB_STATUS, "SSLSocket acquired against remote host [" 
                           + getPoolId() + "]." );
            }
            catch ( Exception e )
            {
                Debug.warning( e.toString() );
            }
        }

        return socket;
    }


    /**
     * Acquires a socket connection from pool within the specified time period.
     *
     * @param    timeout    Time in millis to wait for a db socket connection to be available.
     *
     * @return A socket connection from pool.
     *
     * @exception ResourceException  Thrown if socket connection can't be obtained.
     */
    public synchronized IASocket acquireSocket(long timeout) throws ResourceException
    {
        long startTime = Performance.startTiming( Debug.BENCHMARK );

        IASocket socket = (IASocket) super.acquireResource(timeout);

        Performance.stopTiming( Debug.BENCHMARK, startTime, "Acquiring IA socket connection." );

        if ( Debug.isLevelEnabled( Debug.DB_STATUS ) )
        {
            try
            {
                Debug.log( Debug.DB_STATUS, "SSLSocket acquired against remote host [" 
                           + getPoolId() + "]." );
            }
            catch ( Exception e )
            {
                Debug.warning( e.toString() );
            }
        }

        return socket;
    }


    /**
     * Puts back the socket connection to the resource pool.
     *
     * @param socket  The socket connection to be returned.
     *
     * @exception ResourceException Thrown on errors.
     */
    public synchronized void releaseSocket(IASocket socket) throws ResourceException
    {
        if (socket == null)
        {
            Debug.log( Debug.ALL_WARNINGS, "WARNING: Attempt was made to release a null DB socket connection - ignoring." );

            return;
        }

        if ( Debug.isLevelEnabled( Debug.DB_STATUS ) )
        {
            try
            {
                Debug.log( Debug.DB_STATUS, "Returning SSLSocket for remote host [" 
                           + getPoolId() + "]." );
            }
            catch ( Exception e )
            {
                Debug.warning( e.toString() );
            }
        }

        super.releaseResource(socket);
    }


    /**
     * Destroys all resources in free pool and in used pool. Overriden here to
     * make it accessible to the com.nightfire.framework.db package.
     *
     * @exception ResourceException Thrown if failed to destroy the pool.
     */
    protected void destroyAll() throws ResourceException
    {

        super.destroyAll();
    }


    /**
     * Creates a new db socket connection.
     *
     * @return The newly created socket connection.
     *
     * @exception ResourceException Thrown if failed to create a resource.
     */
    protected Object createResource ( ) throws ResourceException
    {
        try
        {
    
            Debug.log(Debug.MSG_STATUS, "host: " + host + ", port: " + port );
		
	    long startTime = Performance.startTiming( Debug.BENCHMARK );

	    SSLSocket socket = null;
	    try
	    {

            	socket = (SSLSocket) factory.createSocket( host, port );
            
	    }
	    catch (Exception e)
	    {
		    Debug.log(Debug.ALL_WARNINGS, "First attempt to create SSLSocket failed with reason: "
				    + e.getMessage() );
	    }
	    
	    if ( socket == null )
	    {
		Debug.log(Debug.MSG_STATUS, "Second attempt to create SSLSocket ...");
		
		socket = (SSLSocket) factory.createSocket( host, port );
	    }

	    
	    socket.setTcpNoDelay(true);

	    IASocket iaSocket = new IASocket( socket );
	    
            Performance.stopTiming( Debug.BENCHMARK, startTime, "Creating IA socket connection." );

            if ( Debug.isLevelEnabled( Debug.DB_STATUS ) )
            {
                try
                {
                    Debug.log( Debug.DB_STATUS, "SSLSocket created against remote host [" 
                           + getPoolId() + "]." );
                }
                catch ( Exception e )
                {
                    Debug.warning( e.toString() );
                }
            }

            return iaSocket;
        }
        catch ( IOException ioe )
        {
            Debug.logStackTrace( ioe );

            throw new ResourceException( "ERROR: Unable to open IA socket connection:\n" +
                                         ioe.getMessage() );
        }
    }


    /**
     * Checks whether the resource is still valid.
     *
     * @return true if valid.
     *
     * @exception  ResourceException  Thrown resource is not valid.
     */
    protected boolean validateResource(Object resource) throws ResourceException
    {        
     	boolean result = true;

	IASocket iaSocket = (IASocket) resource;
	
	if ( !iaSocket.isValid() || iaSocket.inactiveTimerExpired() 
			|| iaSocket.activeTimerExpired() )
		return false;
	

	return result;
		
    }


    /**
     * Destroys the resource. Closes the db socket connection.
     *
     * @param socket  The socket connection to be destroyed.
     *
     * @exception  ResourceException  Thrown if resource can't be destroyed.
     */
    protected void destroyResource(Object resource) throws ResourceException
    {
        Debug.log(Debug.DB_STATUS,
                StringUtils.getClassName(this) + ": Destroying the socket connection...");

        if (resource == null)
        {
            Debug.warning( "Attempt was made to destroy a null socket connection - ignoring." );

            return;
        }

        if (! (resource instanceof IASocket ))
        {
            throw new ResourceException("The object [" + resource + "] to be destroyed is not a socket connection.");
        }

        SSLSocket theSocket = ( (IASocket) resource).socket;
	
	if ( theSocket == null )
	{
       	    Debug.warning( "Attempt was made to destroy a null socket connection - ignoring." );

            return;

	}
	
        try
        {
            synchronized (theSocket)
            {
                OutputStream os = theSocket.getOutputStream();
		if ( os != null )
		{
			os.close();
		}
			
                theSocket.close();
                theSocket = null;
            }
        }
        catch (IOException e)
        {
            Debug.log(Debug.ALL_WARNINGS,
                StringUtils.getClassName(this) + ": Failed to close socket connection.");
            //Debug.logStackTrace(e);	   
        }
    }


    /**
     * Check a Socket against the pool. Throws an exception if the
     * socket connection doesn't belong to the pool.
     *
     * @exception  ResourceException  Thrown if socket connection doesn't belong to
     * the pool.
     */
    private void checkOwnership(IASocket socket) throws ResourceException {

        if( !isPoolResource(socket) ){

            throw new ResourceException("SSLSocket [" + socket + "] is not part of this pool");
        }
    }


    public String getPoolId()
    {
	    return host+":"+Integer.toString(port);
    }


}

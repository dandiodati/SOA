/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.socket;

import java.io.*;
import java.net.*;
import java.util.*;

import com.nightfire.framework.util.*;


/**
 * Socket listener API infrastructure for use in building TCP/IP socket server objects.
 */
public class SocketListener
{ 
    /**
     * Default value for the backlog queue size to use when creating the server socket.
     */
    public static final int DEFAULT_BACKLOG_QUEUE_SIZE = 10;


    /**
     * Create a socket listener.
     *
     * @param  listenPort  The port that the server socket should listen on.
     * @param  handlerClassName  The fully-qualified (package + class) name of the
     *                           class that will be used to service each request
     *                           (must implement the ClientSocketRequestHandler interface).
     * @param  handlerKey  Property key value used to configure the request-handler.
     * @param  handlerType  Property type value used to configure the request-handler.
     *
     * @exception  FrameworkException  Thrown if the server socket can't be created against the given port.
     */
    public SocketListener ( int listenPort, String handlerClassName, String handlerKey, String handlerType ) throws FrameworkException
    {
        this( null, listenPort, -1, handlerClassName, handlerKey, handlerType );
    }


    /**
     * Create a socket listener.
     *
     * @param  bindAddress  The IP address to bind to on a multi-homed host. 
     * @param  listenPort  The port that the server socket should listen on.
     * @param  backlog  The maximum queue length for incoming connection indications (a request to connect) 
     *                  is set to the backlog parameter. If a connection indication arrives when the queue 
     *                  is full, the connection is refused. 
     * @param  handlerClassName  The fully-qualified (package + class) name of the
     *                           class that will be used to service each request
     *                           (must implement the ClientSocketRequestHandler interface).
     * @param  handlerKey  Property key value used to configure the request-handler.
     * @param  handlerType  Property type value used to configure the request-handler.
     *
     * @exception  FrameworkException  Thrown if the server socket can't be created against the given port.
     */
    public SocketListener ( String bindAddress, int listenPort, int backlog, 
                            String handlerClassName, String handlerKey, String handlerType ) throws FrameworkException
    {
        try
        {
            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Creating server socket to listen on port [" 
                           + listenPort + "]." );
            
            if ( StringUtils.hasValue( bindAddress ) )
            {
                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                    Debug.log( Debug.IO_STATUS, "Server socket will listen at host address [" 
                               + bindAddress + "]." );
                
                if ( backlog < 0 )
                    backlog = DEFAULT_BACKLOG_QUEUE_SIZE;

                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                    Debug.log( Debug.IO_STATUS, "Server socket backlog queue size will be set to [" 
                               + backlog + "]." );

                listener = new ServerSocket( listenPort, backlog, InetAddress.getByName(bindAddress) );
            }
            else
            {
                if ( backlog > 0 )
                {
                    if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                        Debug.log( Debug.IO_STATUS, "Server socket backlog queue size will be set to [" 
                                   + backlog + "]." );
                    
                    listener = new ServerSocket( listenPort, backlog );
                }
                else
                    listener = new ServerSocket( listenPort );
            }

            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Socket listener created: " + listener.toString() );

            this.handlerClassName = handlerClassName;

            this.handlerKey = handlerKey;
            this.handlerType = handlerType;

            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Socket client request handler is [" + handlerClassName 
                           + "], configured via [" + handlerKey + "," + handlerType + "]" );

            // Create an easily-identifyable (in the logs) thread group to place request handler threads into.
            requestHandlerThreadGroup = new ThreadGroup( StringUtils.getClassName(this) );
        }
        catch ( Exception e )
        {
            throw new FrameworkException( "ERROR: Could not create server socket to listen on port [" 
                                          + listenPort + "]:\n" + e.toString() );
        }
    }


    /**
     * Block, waiting for client requests, each of which is serviced in its own thread.
     */
    public void listen ( )
    {
        Debug.log( Debug.IO_STATUS, "Socket listener now entering listen-loop ..." );

        try
        {
            while ( !shuttingDown )
            {
                Debug.log( Debug.IO_STATUS, "Socket listener waiting for client connections ..." );

                Socket clientSock = listener.accept( );

                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                    Debug.log( Debug.IO_STATUS, "Accepted client socket connection: " + clientSock.toString() );

                Thread t = new Thread( requestHandlerThreadGroup, 
                                       new RequestHandlerThread( clientSock, handlerClassName, handlerKey, handlerType ), 
                                       "request-handler-" + requestHandlerThreadCount ); 
                
                // NOTE: We don't make these threads daemons so that in the event of a shutdown, 
                // the server won't exit until all outstanding requests have been serviced.
                
                t.start( );

                requestHandlerThreadCount ++;
                
                if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
                    Debug.log( Debug.THREAD_STATUS, "Number of outstanding request-handler threads [" 
                               + requestHandlerThreadCount + "]." );
            }
        }
        catch ( Exception e )
        {
            Debug.warning( "Server socket encountered exception:\n" + e.toString() );
        }
        finally
        {
            try
            {
                Debug.log( Debug.IO_STATUS, "Closing server socket." );
                
                listener.close( );
            }
            catch ( Exception e )
            {
                Debug.warning( e.toString() );
            }
        }

        Debug.log( Debug.IO_STATUS, "Server socket is now exiting its listen-loop." );
    }


    /**
     * Shuts-down the socket listener.
     */
    public void shutdown ( )
    {
        Debug.log( Debug.IO_STATUS, "Shutting-down server socket ..." );

        shuttingDown = true;

        try
        {
            listener.close( );
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
        }
    }


    // A helper class used to execute a new handler class against a client request
    // in a separate thread.
    private class RequestHandlerThread implements Runnable
    {
        public RequestHandlerThread ( Socket client, String handlerClassName, String handlerKey, String handlerType )
        {
            Debug.log( Debug.IO_STATUS, "Creating request-handler thread." );

            clientSocket = client;

            this.handlerClassName = handlerClassName;

            this.handlerKey = handlerKey;

            this.handlerType = handlerType;
        }

        // Executes the request handler code in its own thread.
        public void run ( )
        {
            try
            {
                if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                    Debug.log( Debug.IO_STATUS, "Processing client socket request using [" + handlerClassName + "]." );

                ClientSocketRequestHandler handler = (ClientSocketRequestHandler)ObjectFactory.create( handlerClassName, 
                                                                                                       ClientSocketRequestHandler.class );
                handler.initialize( handlerKey, handlerType );

                handler.process( clientSocket );
            }
            catch ( Exception e )
            {
                Debug.error( "Client socket request handler failed:\n" 
                             + e.toString() + "\n" + Debug.getStackTrace( e ) );
            }
            finally
            {
                try
                {
                    Debug.log( Debug.IO_STATUS, "Closing client socket: " + clientSocket.toString() );
                
                    clientSocket.close( );
                }
                catch ( Exception e )
                {
                    Debug.log( Debug.IO_STATUS, e.toString() );
                }
            }

            requestHandlerThreadCount --;

            if ( Debug.isLevelEnabled( Debug.THREAD_STATUS ) )
                Debug.log( Debug.THREAD_STATUS, "Number of outstanding request-handler threads [" 
                           + requestHandlerThreadCount + "]." );
        }
        

        private Socket clientSocket;

        private String handlerClassName;

        private String handlerKey;

        private String handlerType;
    }


    /**
     *  Socket listener unit-test application entry point.
     *
     * @param  args  Command-line arguments:
     *               arg[0] = port to listen on.
     *               arg[1] = Name of class that will be used to service requests.
     */
    public static void main ( String[] args )
    {
        if ( args.length < 4 )
        {
            System.err.println( "\n\nUSAGE: SocketListener <listen-port> <request-handler-class-name> <handler-key> <handler-type>\n\n" );
            
            System.exit( -1 );
        }
        
        Debug.enableAll( );
        Debug.showLevels( );
        Debug.configureFromProperties( );

        if ( Debug.isLevelEnabled( Debug.THREAD_BASE ) )
            Debug.enableThreadLogging( );

        String port                    = args[0];
        String requestHandlerClassName = args[1];
        String requestHandlerKey       = args[2];
        String requestHandlerType      = args[3];

        SocketListener listener = null;

        try
        {
            listener = new SocketListener( Integer.parseInt(port), requestHandlerClassName, 
                                           requestHandlerKey, requestHandlerType );
            
            listener.listen( );
        }
        catch ( Exception e )
        {
            System.err.println( "\n\nERROR: " + e.toString() );

            e.printStackTrace( );
        }
        finally
        {
            if ( listener != null )
                listener.shutdown( );
        }
    }


    private boolean shuttingDown = false;

    private ServerSocket listener;

    private String handlerClassName;

    private String handlerKey;
    
    private String handlerType;

    private ThreadGroup requestHandlerThreadGroup;

    private int requestHandlerThreadCount;
}

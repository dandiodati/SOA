/*
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.socket;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import com.nightfire.spi.common.communications.*;
import com.nightfire.framework.util.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;


/*
 * This is an abstract class that sets up a connection-oriented socket client.
 * Socket creation runs in a separate thread. If a socket connection is set up
 * successfully, this thread will notify the main thread. If the main thread waits
 * for a predefined period of time without being notified, it will timeout and an
 * exception will be thrown.
 */

public abstract class SocketCommAdapter extends MessageProcessorBase
{
  protected String ipAddress = null;        // remote server IP address
  protected String portNum = null;          // remote server port number
  protected String timeoutString = null;
  protected Socket commSocket = null;       // client socket reference
  protected OutputStream commOut = null;    // output stream interface
  protected InputStream commIn = null;      // inputstream interface

    // public static final String SOURCE_ID = "$Id: //comms/com/nightfire/comms/socket/SocketCommAdapter.java#10 $";


  /**
  * Constructor
  */
  public SocketCommAdapter() { /* Empty */ }


  /**
  * Opens socket connection. Creates input and ouput stream interfaces.
  *
  * @param  ipAddress   IP address of the server.
  *
  * @param  portNum     Port number of the server.
  *
  * @param  timeoutString   Timeout for setting up socket connection (msec).
  *
  * @exception  ProcessingException   Thrown when connection fails.
  */
  public void connect(String ipAddress, String portNum, String timeoutString) throws ProcessingException
  {
      this.ipAddress = ipAddress;
      this.portNum = portNum;
      this.timeoutString = timeoutString;
      
      if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
          Debug.log( Debug.IO_STATUS, "Socket client connection configuration: address [" 
                     + ipAddress + "], port [" + portNum + "], time-out [" + timeoutString + "] msec." );

      try
      {
          int timeoutInt = Integer.parseInt(timeoutString);
          
          ClientSocketConnector connector = new ClientSocketConnector( ipAddress, portNum );
          
          NFObserver observer = new NFObserver( connector, timeoutInt );
          
          observer.executeNotifier( );
          
          commSocket = connector.getSocket( );
          
          if( commSocket == null ) 
          {
              // If the connection thread didn't notify the observer ...
              if ( !observer.isNotified() )
              {
                  // Get a handle to the connection thread and interrupt it.
                  Thread conThread = connector.getConnectionThread( );

                  try
                  {
                      Debug.log( Debug.IO_STATUS, "Interrupting client socket connection thread ..." );

                      conThread.interrupt( );
                  }
                  catch ( Exception e )
                  {
                      Debug.log( Debug.ALL_ERRORS, e.toString() );
                  }
              }

              String errMsg = "ERROR: SocketCommAdapter: Client socket connection could not be established to host [" 
                  + ipAddress + "], port [" + portNum + "].";
              
              Debug.log(Debug.ALL_ERRORS, errMsg );
              
              throw new ProcessingException( errMsg );
          }
          
          Debug.log(Debug.IO_STATUS, "SocketCommAdapter: Client socket connection established to [" + commSocket.toString() + "]." );
          
          //After the handle of the socket is obtained, proceed to get input and output stream interfaces.
          try 
          {
              commOut = commSocket.getOutputStream();
              commIn = commSocket.getInputStream();
              
              if( commOut == null )
                  Debug.log(Debug.ALL_ERRORS, "ERROR: SocketCommAdapter: Can't get output stream." );
              else
                  Debug.log(Debug.IO_STATUS, "SocketCommAdapter: Output stream created: " + commOut);
              
              if( commIn == null )
                  Debug.log(Debug.ALL_ERRORS, "ERROR: SocketCommAdapter: Can't get input stream" );
              else
                  Debug.log(Debug.IO_STATUS, "SocketCommAdapter: Input stream created: " + commIn);
          }
          catch (IOException e) 
          {
              String errMsg = "ERROR: SocketCommAdapter: Can't get I/O streams for the connection to " + ipAddress + ":" + portNum;

              Debug.log( Debug.ALL_ERRORS, errMsg );

              throw new ProcessingException( errMsg );
          }
      }
      catch ( ProcessingException pe )
      {
          throw pe;
      }
      catch ( Exception e )
      {
          throw new ProcessingException( e );
      }
  } // connect


  /**
  * Disconnects socket connection. close input and output stream interfaces.
  */
  public void disconnect()  // Should be called from cleanup()
  {
      if( commOut != null ) {
          try
          {
              commOut.close();

              commOut = null;
              
              Debug.log(Debug.IO_STATUS, "commOut closed.");
          }
          catch ( Exception e )
          {
              Debug.log( Debug.ALL_ERRORS, e.toString() );
          }
      }
      
      if( commIn != null ) {
          try
          {
              commIn.close();

              commIn = null;

              Debug.log(Debug.IO_STATUS, "commIn closed.");
          }
          catch ( Exception e )
          {
              Debug.log( Debug.ALL_ERRORS, e.toString() );
          }
      }
      
      if( commSocket != null ) {
          try
          {
              commSocket.close();

              commSocket = null;

              Debug.log(Debug.IO_STATUS, "commSocket closed.");
          }
          catch ( Exception e )
          {
              Debug.log( Debug.ALL_ERRORS, e.toString() );
          }
      }
  } // disconnect
    

    /**
     * Read up to 'length' bytes from the input stream and return
     * the resulting byte array.  This functionality is essentially
     * the same as that found in the DataInputStream.readFully() method
     * except that there is diagnostic logging and no exception is thrown
     * if end-of-stream is encountered.
     *
     * @param  length  The number of bytes to read.
     *
     * @return  A byte array containing the number of requested bytes.
     *          The array length may be less than the requested amount
     *          if an end-of-stream is hit.
     *
     * @exception  ProcessingException  Thrown on IO errors.
     */
    public byte[] read ( int length )
        throws ProcessingException
    {
        return( read( commIn, length ) );
    }


    /**
     * Read up to 'length' bytes from the input stream and return
     * the resulting byte array.  This functionality is essentially
     * the same as that found in the DataInputStream.readFully() method
     * except that there is diagnostic logging and no exception is thrown
     * if end-of-stream is encountered.
     *
     * @param  in  The input stream to read from.
     * @param  length  The number of bytes to read.
     *
     * @return  A byte array containing the number of requested bytes.
     *          The array length may be less than the requested amount
     *          if an end-of-stream is hit.
     *
     * @exception  ProcessingException  Thrown on IO errors.
     */
    public static byte[] read ( InputStream in, int length )
        throws ProcessingException
    {
        if ( (in == null) || (length < 1) )
        {
            throw new ProcessingException( "Invalid arguments to read() method." );
        }

        Debug.log( Debug.IO_STATUS, "Attempting to read ["
                   + length + "] bytes from input stream ..." );
        
        BufferedInputStream bufInStream = new BufferedInputStream( in );
        
        byte[] buf = new byte[ length ];
        
        int readSoFar = 0;

        // While we haven't hit the requested amount ...
        while ( readSoFar < length )
        {
            try
            {
                int currentRead = bufInStream.read( buf, readSoFar, length - readSoFar );
                
                if ( currentRead < 0 )
                {
                    Debug.log( Debug.IO_STATUS, "Hit END-OF-STREAM after reading ["
                               + readSoFar + "] bytes." );
                    
                    break;
                }
                
                readSoFar += currentRead;
                
                Debug.log( Debug.IO_STATUS, "Bytes-read status: last read ["
                           + currentRead + "], total so far [" + readSoFar + "]." );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( e );
            }
        }
        
        Debug.log( Debug.IO_STATUS, "Read a total of ["
                   + readSoFar + "] bytes from input stream." );

        // If we read less than was requested, return a shorter array.
        if ( readSoFar < length )
        {
            byte[] newBuf = new byte[ readSoFar ];

            for ( int Ix = 0;  Ix < readSoFar;  Ix ++ )
                newBuf[ Ix ] = buf[ Ix ];

            return newBuf;
        }

        return buf;
    }


    // This class provides the ability to attempt the client socket connection
    // in a separate thread, which allows for timeout management.
    private class ClientSocketConnector extends NFNotifier
    {
        public ClientSocketConnector ( String ipAddress, String portNum )
        {
            this.ipAddress = ipAddress;
            this.portNum   = portNum;
        }

        public Socket getSocket ( )
        {
            return commSocket;
        }

        public Thread getConnectionThread ( )
        {
            return connectionThread;
        }

        /**
         * Execute the notifier.
         *
         * @param NFObserver the observer
         */
        protected void executeNotifier ( NFObserver observer )
        {
            try
            {
                Debug.log(Debug.IO_STATUS, "SocketCommAdapter: Creating client socket connection to: address [" 
                          + ipAddress + "], port [" + portNum + "] ..." );
                
                // Remember thread so that it can be interrupted by parent.
                connectionThread = Thread.currentThread( );

                int port = Integer.parseInt( portNum );
                
                InetAddress addr = InetAddress.getByName( ipAddress );
                
                commSocket = new Socket( addr, port );
                
                Debug.log(Debug.IO_STATUS, "SocketCommAdapter: Successfully created client socket connection :" + commSocket.toString() );
            }
            catch ( Exception e )
            {
                Debug.log( Debug.ALL_ERRORS, "ERROR: Could not make client socket connection:\n" + e.toString() );
            }
        }
        
        private Thread connectionThread = null;
        private Socket commSocket = null;       // client socket reference
        private String ipAddress = null;        // remote server IP address
        private String portNum = null;          // remote server port number
    }
}

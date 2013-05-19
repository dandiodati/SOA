/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.socket;


import java.io.*;
import java.net.*;

import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;


/**
 * Basic client socket request handler that creates and invokes a driver
 * in response to requests.
 * 
 * The expected input data format is a stream containing an integer giving
 * the length of the remaining data (readable via DataInputStream.readInt()),
 * followed by a byte stream of the given length.  Any responses will be
 * returned using the same marshalling format.
 */
public class BasicClientSocketRequestHandler implements ClientSocketRequestHandler
{ 
    /**
     * Initialize the request handler.
     *
     * @param  key   Property-key to use in locating initialization properties.
     * @param  type  Property-type to use in locating initialization properties.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
	public void initialize ( String key, String type ) throws FrameworkException
    {
        driverKey = key;
        
        driverType = type;
    }
    

    /**
     * Process a request comming from a client over a socket.
     * NOTE: The invoking socket listener will close the socket
     * when the handler is done with it.
     *
     * @param  clientSocket  Socket used to interact with client.
     *
     * @exception  FrameworkException  Thrown on any errors.
     */
    public void process ( Socket clientSocket ) throws FrameworkException
    {
        try
        {
            // Extract the input from the socket connection.
            Object input = getInput( clientSocket );


            // Invoke a driver against the input.
            MessageProcessingDriver driver = new MessageProcessingDriver( );
            
            driver.initialize( driverKey, driverType );
            
            Object result = driver.process( input );
            

            // Return the response over the socket connectino, if available.
            setOutput( clientSocket, result );
        }
        catch ( FrameworkException fe )
        {
            throw fe;
        }
        catch ( Exception e )
        {
            throw new FrameworkException( e );
        }
    }
    
    
    /**
     * Unmarshall the input request from the socket to give to the driver.
     *
     * @param  clientSocket  Socket used to interact with client.
     *
     * @return  The input to pass to the Driver's process() method.
     *
     * @exception  FrameworkException  Thrown on any errors.
     */
    protected Object getInput ( Socket clientSocket ) throws FrameworkException
    {
        try
        {
            // Read the input from the socket.
            InputStream in = clientSocket.getInputStream( );
            
            DataInputStream din = new DataInputStream( in );
            
            int len = din.readInt( );
            
            String input = new String( SocketCommAdapter.read( in, len ) );
            
            if ( input.length() != len )
            {
                throw new FrameworkException( "ERROR: Expecting a request message of length [" 
                                              + len + "], but only got [" + input.length() + "] bytes." );
            }
            
            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Input length [" + len + "], value:\n" + input );

            return input;
        }
        catch ( FrameworkException fe )
        {
            throw fe;
        }
        catch ( Exception e )
        {
            throw new FrameworkException( e );
        }
    }


    /**
     * Marshall the output response from the driver to the socket.
     *
     * @param  clientSocket  Socket used to interact with client.
     * @param  result  The output to return.
     *
     * @exception  FrameworkException  Thrown on any errors.
     */
    protected void setOutput ( Socket clientSocket, Object result ) throws FrameworkException
    {
        try
        {
            // If a non-null result was returned, write it out to the socket.
            if ( result == null )
            {
                Debug.log( Debug.IO_STATUS, "No result available to return to client." );
                
                return;
            }
            
            String output = result.toString( );
            
            if ( Debug.isLevelEnabled( Debug.IO_STATUS ) )
                Debug.log( Debug.IO_STATUS, "Output length [" + output.length() + "], value:\n" + output );
            
            DataOutputStream dout = new DataOutputStream( clientSocket.getOutputStream() );
            
            dout.writeInt( output.length() );
            
            dout.writeBytes( output );
            
            dout.flush( );
        }
        catch ( Exception e )
        {
            throw new FrameworkException( e );
        }
    }
    
    
    private String driverKey;
    private String driverType;
}

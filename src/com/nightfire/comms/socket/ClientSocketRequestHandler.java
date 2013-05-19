/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.comms.socket;


import java.net.*;

import com.nightfire.framework.util.*;


/**
 * Interface that all code servicing client socket requests must implement 
 * in order to be associated with a socket listener.  A new request handler
 * will be used to service each client request.
 */
public interface ClientSocketRequestHandler
{ 
    /**
     * Initialize the request handler.
     *
     * @param  key   Property-key to use in locating initialization properties.
     * @param  type  Property-type to use in locating initialization properties.
     *
     * @exception  FrameworkException  Thrown on errors.
     */
	public void initialize ( String key, String type ) throws FrameworkException;


    /**
     * Process a request comming from a client over a socket.
     * NOTE: The invoking socket listener will close the socket
     * when the handler is done with it.
     *
     * @param  clientSocket  Socket used to interact with client.
     *
     * @exception  FrameworkException  Thrown on any errors.
     */
    public void process ( Socket clientSocket ) throws FrameworkException;
}

package com.nightfire.router.cmn;


import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.router.RouterConstants;
import com.nightfire.router.cfg.RouterConfig.MessageDirectorConfig;

/**
 * Interface to be implemented by new message directors.
 *    
 * @author hpirosha
 */
public interface MessageDirectorV2 {

    /**
     * Method to initialized message director with its config. 
     * @param cfg message director configuration
     * @throws ProcessingException
     */
    public void initialize(MessageDirectorConfig cfg) throws ProcessingException;

    /**
     * Determines if this message director can route a message.
     * @param header The request header.
     * @param message The request message.
     * @return true if this message director can route this message, otherwise
     *          returns false.
     * @throws MessageException if there is an problem with the xml header or message.
     */
    public boolean canRoute(Document header, Object message) throws MessageException;

    /**
     * processes an asynchronous, synchronous, or synchronousWithHeader request
     * @param header The request header
     * @param message The request message
     * @param reqType - indicates the type of request, refer to RouterConstants.
     * @see RouterConstants
     * @return Response object if there is a response returned, otherwise null is returned.
     * @throws ProcessingException if there is a processing error
     * @throws MessageException if the request message or header is invalid.
     */
     public Object processRequest(Document header, Object message, int reqType) throws ProcessingException,MessageException;
}

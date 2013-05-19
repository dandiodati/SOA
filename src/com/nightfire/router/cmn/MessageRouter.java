package com.nightfire.router.cmn;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.FrameworkException;


/**
 * Interface to be implemented by Message Routing Component. 
 */
public interface MessageRouter {

    public static String FILE_CONFIG_PARAM_NM = "routing-config-file-nm";
    
    /**
     * Route the passed in header and message to suitable Gateway(SPI).
     * 
     * A request using passed-in header and message should be formulated
     * and sent to the appropriate Gateway whose (COS) name should be made
     * using appropriate routing algorithm that introspects content in header/message. 
     * 
     * 
     * @param header  request header.
     * @param message request message.
     * @param reqType indicates the type of request 
     * @return Object response from the spi
     * @throws ProcessingException if something goes wrong during processing
     * @throws MessageException if the xml is bad.
     */
    public Object processRequest(String header, Object message, ReqType reqType) 
                throws ProcessingException,MessageException ;

    /**
     * 
     * @param configFileName
     * @throws FrameworkException
     */
    public void initialize(String configFileName) throws FrameworkException;
}

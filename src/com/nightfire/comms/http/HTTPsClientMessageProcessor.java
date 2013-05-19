package com.nightfire.comms.http;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.comms.util.http.ssl.*;
import com.nightfire.framework.message.common.xml.*;

import org.w3c.dom.*;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * Adapter to send HTTPS request and forwards the obtained response to one or more processors.
 *
 */
public class HTTPsClientMessageProcessor extends MessageProcessorBase 
{

    protected String clientName;
    protected String[] toProcessorNames;
    protected String toProcessorName;

    /**
     * Constructor
     *
     */
    public HTTPsClientMessageProcessor ()
    {
        super();
    }
    
    /**
     * Lets other objects query to check asynchronous messaging capability
     *
     * @return true overrides CommunicationsProtocolAdapterBase
     */
    public boolean supportsAsynchronousMessaging ( )
    {
        return false;
    }

    /**
     * Lets other objects query to check synchronous message processing
     * @return true
     */
    public boolean supportsSynchronousMessaging ( )
    {
        return true;
    }

    /**
     * Called to initialize a message processor object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException Thrown when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,
                    "HTTPsClientMessageProcessor: Initializing....");

        super.initialize(key,type);

        clientName = (String) adapterProperties.get(NAME_TAG);
        String toProcessorName = (String) adapterProperties.get(NEXT_PROCESSOR_NAME_TAG);

        if(Debug.isLevelEnabled(Debug.UNIT_TEST))
            Debug.log(Debug.UNIT_TEST, "HTTPsClientMessageProcessor: Name: " + clientName 
                                            +"Next ProcessorName: " + toProcessorName );
        
        if (clientName == null)
        {
            throw new ProcessingException("ERROR: HTTPsClientMessageProcessor: \"" + NAME_TAG + "\" must be specified in configuration.");
        }
        if (toProcessorName == null)
        {
            toProcessorNames = null;
            return;
        }
    }
    
    
    /**
     * Process the input message (DOM or String) and (optionally) return
     * a value.
     * 
     * @param  input  Input message to process.
     *
     * @param  mpcontext The context
     *
     * @return  Optional output message, or null if none.
     *
     * @exception  MessageException  Thrown if bad message
     *
     * @exception  ProcessingException  Thrown if processing fails.
     */
     
    public NVPair[] execute ( MessageProcessorContext mpcontext, java.lang.Object input ) 
                                                                  throws MessageException,
                                                                         ProcessingException
    {
        if (input==null)
        {
            return null;
        }

        if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS,"HTTPsClientMessageProcessor: Executing ..." );

        String httpresp = null;

        if (input instanceof Document)
        {
            try
            {
                httpresp  =  postMessage ( XMLLibraryPortabilityLayer.convertDomToString( (Document) input ) );
            }
            catch ( FrameworkException e)
            {
                throw new ProcessingException ( e.getMessage() );
            }
        }
        
        if (input instanceof String)
        {
            try 
            {
                httpresp = postMessage ( (String)input);
            }
            catch ( FrameworkException e)
            {
                throw new ProcessingException ( e.getMessage() );
            }
        }
        
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS)) 
            Debug.log(Debug.MSG_STATUS,"HTTPsClientMessageProcessor: Response from web server [" +httpresp +"]");

        // NVPair[] pair = new NVPair[1];
        //pair[0] = new NVPair( toProcessorName, httpresp );

        //return pair;
        return formatNVPair ( httpresp ) ;
    }

    private String postMessage( String message ) throws FrameworkException 
    {
        String url = (String) adapterProperties.get ( URL  );
        String response = null;
        String contentType = (String) adapterProperties.get ( CONTENT_TYPE );
        if (url==null || contentType==null)
        {
            throw new FrameworkException("HTTPsClientMessageProcessor: The property [" +  URL +"] or [" +
                                         CONTENT_TYPE +"] is not set in the persistent properties ");
        }

        HTTPsPostClient client = new HTTPsPostClient ( url, contentType );
        
        return client.post ( message );
        
    }
        
    
    /**
     * Returns name of this message processor.
     *
     * @return  Name of this message processor.
     */
    public String getName ( )
    {
        return clientName;
    }

    public static final String URL = "URL";

    public static final String CONTENT_TYPE = "CONTENT_TYPE";

    private static final String NAME_TAG = "NAME";

    private static final String NEXT_PROCESSOR_NAME_TAG = "NEXT_PROCESSOR_NAME";

}



package com.nightfire.comms.http;

import java.net.*;
import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.driver.*;

import com.nightfire.comms.util.http.HTTPUtils;

/**
 * Class that sends a HTTP POST request to a URL. The
 * content of the POST request is a string. 
 */
public class HTTPClient extends MessageProcessorBase
{

    private String proxyHost = null;
    private String proxyPort = null;
    private String postURLstring = null;
    private String enableURLEncodingFlag = null;
    private boolean urlEncode = false;
    private String mimeType = "text/xml";

    /**
     * Constructor
     *
     */
    public HTTPClient ()
    {
        super();
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
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG,"HTTPClient: Initializing ...");

        super.initialize(key,type);

        postURLstring = getRequiredPropertyValue ( POST_URL_PROP );

        enableURLEncodingFlag = getPropertyValue ( ENABLE_URL_ENCODING_PROP ) ;
        
        if ( enableURLEncodingFlag != null )
        {
            try
            {
                urlEncode = getBoolean ( enableURLEncodingFlag ) ;
            }
            catch ( FrameworkException fe )
            {
                throw new ProcessingException ( fe.getMessage() ) ;
            }
        }

        mimeType = getPropertyValue ( MIME_TYPE_PROP );

        setupHttpProxy();

    }
    
    
    private void setupHttpProxy() throws ProcessingException
    {
        //Setting up  the proxy host and port
        proxyHost = getPropertyValue(POST_PROXY_HOST_PROP);
        proxyPort = getPropertyValue(POST_PROXY_PORT_PROP);

        if ( proxyHost != null && proxyPort !=null )
        {
            if (Debug.isLevelEnabled(Debug.IO_STATUS))
                Debug.log(Debug.IO_STATUS, "HTTPClient: Setting proxy host =[" + proxyHost +"]" +
                      " and proxy port =[" + proxyPort +"]" );
            Properties p = System.getProperties();
            p.put("http.proxyHost",proxyHost);
            p.put("http.proxyPort",proxyPort);

            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "HTTPClient: System.properties : " + p);
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
     
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input )
        throws MessageException, ProcessingException
    {
        if (input==null)
        {
            return null;
        }
        
        String httpresp =  post ( input.getString() ) ;

        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"HTTPClient: Response :" +httpresp);

        return (formatNVPair(httpresp));
    }


    public String post ( String request ) throws ProcessingException 
    {

        if ( urlEncode )
        {
            request = URLEncoder.encode ( request ) ;
        }
        
        String response = null;
        
        try
        {
            response =  HTTPUtils.post ( postURLstring , mimeType , request ) ;
        }
        catch ( Exception e )
        {
            throw new ProcessingException ( e.getMessage() );
        }

        if ( response == null )
        {
            throw new ProcessingException("HTTPClient: Null response obtained after posting [" + request +"] to the URL =[" + postURLstring +"]" );
        }
        
        return response;
    }

    public static final String POST_URL_PROP  = "POST_URL";
    public static final String POST_PROXY_HOST_PROP  = "PROXY_HOST";
    public static final String POST_PROXY_PORT_PROP  = "PROXY_PORT";
    public static final String MIME_TYPE_PROP = "MIME_TYPE";
    public static final String ENABLE_URL_ENCODING_PROP = "ENABLE_URL_ENCODING";
}



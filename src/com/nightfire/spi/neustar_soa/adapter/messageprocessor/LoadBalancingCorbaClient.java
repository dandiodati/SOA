package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.*;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;

import com.nightfire.framework.db.PropertyChainUtil;
import com.nightfire.framework.db.PersistentProperty;

import com.nightfire.framework.corba.CorbaException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;

import com.nightfire.spi.neustar_soa.utils.RoundRobinCorbaClient;

public class LoadBalancingCorbaClient extends MessageProcessorBase {

    /**
     * The name of the property that indicates where the header to send
     * will be found.
     */
    public static final String HEADER_LOCATION_PROP = "HEADER_LOCATION";

    /**
     * The name of the property that indicates where the message to send
     * will be found.
     */
    public static final String MESSAGE_LOCATION_PROP = "MESSAGE_LOCATION";

    /**
     * This property indicates the name of the property key under which
     * this processor should look for server names.
     */
    public static final String SERVER_NAME_KEY_PROP = "SERVER_NAME_KEY";

    /**
     * This property indicates the prefix for the property types under which
     * this processor should look for server names.
     */
    public static final String SERVER_NAME_TYPE_PREFIX_PROP =
                                  "SERVER_NAME_TYPE_PREFIX";

    /**
     * This property indicates the down server retry time
     * this processor should look for .
     */
    public static final String DOWN_SERVER_RETRY_TIME_PROP =
                                  "DOWN_SERVER_RETRY_TIME";
    /**
     * The name used by properties that indicate the name of
     * available servers.
     */
    public static final String SERVER_NAME_PROP = "SERVER_NAME";

    /**
     * The default location from which the request header will be extracted
     * if the HEADER_LOCATION is not specified.
     */
    public static final String DEFAULT_HEADER_LOCATION =
                                  "@context.REQUEST_HEADER";

    /**
     * The default location from which the request header will be extracted
     * if the MESSAGE_LOCATION is not specified.
     */
    public static final String DEFAULT_MESSAGE_LOCATION = INPUT_MESSAGE;

    /**
     * Used to cache and reuse the client instances. If the clients were
     * not reused, then the load balancing would not work.
     */
    private static Map clientCache = new HashMap();

    /**
     * The client use to send load-balanced requests to a list of servers.
     */
    private RoundRobinCorbaClient client;

    /**
     * The location of the input header in the context or message object.
     */
    private String headerLocation;

    /**
     * The location of the input message in the context or message object.
     */
    private String messageLocation;

    /**
     *
     * @param key String
     * @param type String
     * @throws ProcessingException
     */
    public void initialize( String key, String type )
                            throws ProcessingException {

       super.initialize(key, type);

       String serverNameKey = getRequiredPropertyValue( SERVER_NAME_KEY_PROP );
       String propertyTypePrefix =
          getRequiredPropertyValue( SERVER_NAME_TYPE_PREFIX_PROP );
       long downServerRetryTime = Long.parseLong(
    			  getRequiredPropertyValue(DOWN_SERVER_RETRY_TIME_PROP));
       
       String clientCacheKey = serverNameKey+"-"+propertyTypePrefix;

       client = (RoundRobinCorbaClient) clientCache.get( clientCacheKey );

       // if no client yet exists for this key/type combination, then
       // create a new client instance
       if( client == null ){

          synchronized( clientCache ){

             client = (RoundRobinCorbaClient) clientCache.get( clientCacheKey );

             // check to see if some other thread has already created the client
             if( client == null ){

                List serverNames = getServerNames( serverNameKey,
                                                   propertyTypePrefix);

                // check to see that at least one server name was discovered in the
                // properties
                if (serverNames.size() == 0) {
                   throw new ProcessingException("No " + SERVER_NAME_PROP +
                                                 " properties were found under property key [" +
                                                 serverNameKey +
                                                 "] and property type prefix [" +
                                                 propertyTypePrefix + "]");
                }

                client = new RoundRobinCorbaClient(serverNames,serverNameKey,propertyTypePrefix,downServerRetryTime);

                clientCache.put(clientCacheKey, client);

             }

          }

       }

       // get optional input location values
       headerLocation = getPropertyValue(HEADER_LOCATION_PROP,
                                         DEFAULT_HEADER_LOCATION);

       messageLocation = getPropertyValue(MESSAGE_LOCATION_PROP,
                                          DEFAULT_MESSAGE_LOCATION);

    }

    /**
     * Get the header and message from their input locations and
     * send this request to the next server.
     *
     * @param context MessageProcessorContext
     * @param message MessageObject
     * @throws MessageException
     * @throws ProcessingException
     * @return NVPair[]
     */
    public NVPair[] process(MessageProcessorContext context,
                            MessageObject message)
                            throws MessageException,
                                   ProcessingException {
    	
    	ThreadMonitor.ThreadInfo tmti = null;
        // required response to a null message object
        if( message == null ){
           return null;
        }

        String header  = getString( headerLocation, context, message );
        String request = getString( messageLocation, context, message );

        String response = null;

        try{
           tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] sending \nHEADER\n------\n" + header + "\nBODY\n----\n" + request );
           response = client.send(header, request);
        }
        catch(CorbaException cbex){
            throw new ProcessingException( cbex );
        }
        finally
        {
        	ThreadMonitor.stop(tmti);
        }

        return formatNVPair( response );

    }

    /**
     *
     * @param key String
     * @param typePrefix String
     * @return List
     */
    public static List getServerNames( String key, String typePrefix ){

       List serverNames = new ArrayList();

       // check for just the prefix alone, this should always exist
       String serverName = getServerName( key, typePrefix );

       if(serverName != null){
          serverNames.add( serverName );
       }

       int index = 0;

       // we'll check at least indexes 0, 1, and 2, and continue beyond that
       // only if there are still server names to be found
       while(index <= 2 || serverName != null){

          serverName = getServerName( key, typePrefix+"_"+index );

          // if a server name was found, add it to the list
          if(serverName != null){
             serverNames.add( serverName );
          }

          index++;

       }

       return serverNames;

    }

    /**
     *
     * @param key String
     * @param type String
     * @return String
     */
    private static String getServerName( String key, String type ){

       String serverName = null;

       try{

          serverName = PersistentProperty.getProperty(key,
                                                      type,
                                                      SERVER_NAME_PROP);
          if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG,
						"Found server name ["+serverName+
						"] for key ["+key+"], type ["+type+"], and name ["+
						SERVER_NAME_PROP+"]");
		  }

       }
       catch(Exception ex){
          
		  if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			  Debug.log(Debug.SYSTEM_CONFIG,
						"Could not find property for key ["+
						key+"], type ["+type+"], and name ["+
						SERVER_NAME_PROP+"]");
		  }

       }

       return serverName;

    }

}

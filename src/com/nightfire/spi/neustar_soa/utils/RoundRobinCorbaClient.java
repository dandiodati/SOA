package com.nightfire.spi.neustar_soa.utils;

import java.util.*;

import org.omg.CORBA.ORB;
import org.omg.CORBA.StringHolder;

import com.nightfire.common.ProcessingException;

import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;
import com.nightfire.spi.neustar_soa.adapter.messageprocessor.LoadBalancingCorbaClient;

import com.nightfire.framework.corba.CorbaException;
import com.nightfire.framework.corba.CorbaPortabilityLayer;
import com.nightfire.framework.corba.ObjectLocator;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;

/**
 * This is the sync CORBA client that delivers incoming requests
 * to a list of CORBA servers.
 */
public class RoundRobinCorbaClient {

    /**
     * The list of server names to which this client will load balance its
     * requests.
     */
    private List serverNames;

    /**
     * The number of servers in the serverNames list.
     */
    private int serverCount;

    /**
     * This counter is incremented and mod'ed by the serverCount to
     * get the index of the next server name that should be tried.
     */
    private volatile int roundRobinCounter;

    /**
     * Used to locate the CORBA servers based on their name.
     */
    private static ObjectLocator serverLocator = null;
    
    /**
     * The total number of servers in the serverNames list.
     */
    private int totalServerCount;
    
    /**
     * Servers start time.
     */
    private long globalStartTime;
    
    /**
     * Down servers retry time.
     */
    private long downServerRetryTime;
    
    /**
     * Servers key.
     */
    private String serverNameKey = null;
    
    /**
     * Servers property type
     */
    private String propertyTypePrefix = null;
    

    /**
     * Constructor.
     *
     * @param serverNames List the list containing the string
     *                         server names of the CORBA servers that
     *                         should be tried in turn by this client.
     */
    public RoundRobinCorbaClient( List serverNames,String serverKey, String serverTypePrefix,long downServerRetryTimeValue ){

       this.serverNames = serverNames;
       serverCount = serverNames.size();
       roundRobinCounter = 0;
       this.totalServerCount = serverCount;
       this.globalStartTime = System.currentTimeMillis( );
       this.downServerRetryTime = downServerRetryTimeValue;
       this.serverNameKey = serverKey;
       this.propertyTypePrefix = serverTypePrefix;
    }

    /**
     * Sends the given header and message to the next server. If the
     * server is not available, each of the other configured servers
     * will be tried until an available server is found. If all known
     * servers are down, then a ProcessingException will be thrown.
     *
     * @param header String the header to send.
     * @param message String the XML message to send.
     * @throws MessageException thrown if a server finds the given message
     *                          to contain invalid data.
     * @throws ProcessingException thrown if a server is contacted but could
     *                             not process the request. Thrown if any sort
     *                             of internal error occurs.
     * @throws CorbaException thrown if none of the known servers were
     *                        available.
     * @return String the result returned from the call to an available server.
     */
    public String send( String header, String message )
                        throws MessageException,
                               ProcessingException,
                               CorbaException{

       // this will contain the response message
       StringHolder response = new StringHolder();

       // initialize the CORBA layer if necessary
       if( serverLocator == null ){
          initCorba();
       }
       if(serverCount < totalServerCount && System.currentTimeMillis() - globalStartTime > downServerRetryTime) {
    	    Debug.log(Debug.IO_STATUS,"ServerCount:" + serverCount);				
    	    Debug.log(Debug.IO_STATUS,"TotalServerCount :"+ totalServerCount);			
			synchronized (RoundRobinCorbaClient.class) {
					globalStartTime = System.currentTimeMillis();
					serverNames.clear();
					serverNames = LoadBalancingCorbaClient.getServerNames(serverNameKey,propertyTypePrefix);
					if (serverNames.size() == 0) {
						throw new ProcessingException(
								"No SERVER_NAME"
										+ " properties were found under property key ["
										+ serverNameKey
										+ "] and property type prefix ["
										+ propertyTypePrefix + "]");
					}
					serverCount = serverNames.size();
					roundRobinCounter = 0;
			}
		}
   
		// get the index of the next server this client should try
		int serverIndex = getNextIndex();

		// This loop tries all servers in the serverNames list
		// until it gets a success.
		for (int i = 0; i < serverCount; i++) {

			String serverName = serverNames.get(serverIndex).toString();

			if (Debug.isLevelEnabled(Debug.IO_STATUS)) {

				Debug.log(Debug.IO_STATUS,"Attempting to locate server number ["+ serverIndex + "] named [" + serverName + "]");
			}

			try {

				org.omg.CORBA.Object server = serverLocator.find(serverName);

				RequestHandler handler = RequestHandlerHelper.narrow(server);

				send(header, message, handler, response);

				return response.value;

			} catch (CorbaException findFailed) {
				if (findFailed.getMessage().contains("Could not resolve CORBA name")|| 
					findFailed.getMessage().contains("Retries exceeded, couldn't reconnect to")) 
				{
					Debug.error("Could not deliver request to server ["+ serverName + "]: ");
					synchronized (RoundRobinCorbaClient.class) {
						serverLocator.removeFromCache(serverName);
						serverNames.remove(serverName);
						serverCount = serverNames.size();
						roundRobinCounter = 0;
						i--;
					}
				}
				Debug.error("Could not locate server named [" + serverName + "]: " + findFailed);

			} catch (InvalidDataException badData) {

				throw new MessageException(badData.errorMessage);

			} catch (CorbaServerException processingEx) {

				throw new ProcessingException(processingEx.errorMessage);

			} catch (NullResultException nullResult) {

				throw new ProcessingException(nullResult.errorMessage);

			} catch (Exception ohNo) {
				if (ohNo.getMessage().contains(	"Could not resolve CORBA name")	|| 
					ohNo.getMessage().contains(	"Retries exceeded, couldn't reconnect to")) 
				{
					Debug.error("Could not deliver request to server ["	+ serverName + "]: ");
					synchronized (RoundRobinCorbaClient.class) {
						serverLocator.removeFromCache(serverName);
						serverNames.remove(serverName);
						serverCount = serverNames.size();
						roundRobinCounter = 0;
						i--;
					}
				}
				Debug.error("Could not deliver request to server ["	+ serverName + "]: " + ohNo);

			}
			if (serverCount > 0) {
				// try the next server
				serverIndex++;
				serverIndex %= serverCount;

				// this assures that the next index is incremented and
				// that the load-balancing is evenly distributed even
				// in the case where a server may be down
				getNextIndex();
			}

		}
		// If we get here, then we have tried all known servers,
		// and none of them were available.
		throw new CorbaException("None of the following servers were "
				+ "available to service the request: " + serverNames);
	
   }

    /**
     * This calls processSync() on the given RequestHandler instance.
     * This callout method allows a subclass to call the
     * processAsync() method instead.
     *
     * @param header String the header to be sent.
     * @param request String the actual message string to be sent
     * @param handler RequestHandler the request handler instance
     *                to which the request will be delivered.
     * @param response the output parameter into which the synchronous
     *                 reply will be placed.
     *
     */
    protected void send(String header,
                        String request,
                        RequestHandler handler,
                        StringHolder response)
                        throws InvalidDataException,
                               CorbaServerException,
                               NullResultException{

       handler.processSync(header, request, response);

    }

    /**
     * This gets the next server index to try first. This changing index
     * implements the round robin functionality.
     *
     * @return int the index of the server to try next.
     */
    private int getNextIndex()throws ProcessingException{
    	try {
			int index = roundRobinCounter % serverCount;

			roundRobinCounter++;
			roundRobinCounter %= serverCount;

			return index;
		}catch (Exception ex) {
            Debug.logStackTrace(ex);
            throw new ProcessingException("None of the servers were available to service the request:");

         }
    }

    /**
     * This gets a CORBA ORB and initializes the ObjectLocator instance for
     * looking up CORBA servers.
     *
     * @throws ProcessingException thrown if CORBA initialization fails.
     */
    private static synchronized void initCorba()
                                throws ProcessingException{

       // if the ObjectLocation reference is still null. i.e. if some
       // other thread hasn't beaten us to the punch
       if( serverLocator == null ){

          // get access to the CORBA ORB and use it to locate the servers
          try {

             CorbaPortabilityLayer cpl = new CorbaPortabilityLayer(null,
                null,
                null);
             ORB orb = cpl.getORB();
             serverLocator = new ObjectLocator(orb);

          }
          catch (Exception ex) {

             Debug.logStackTrace(ex);
             throw new ProcessingException("Could not initialize CORBA layer: " +
                                           ex);

          }

       }

    }
    
}

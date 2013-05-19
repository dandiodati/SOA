package com.nightfire.spi.neustar_soa.queue;

import java.util.ArrayList;
import java.util.List;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.Debug;
import com.nightfire.spi.neustar_soa.adapter.messageprocessor.LoadBalancingCorbaClient;
import com.nightfire.spi.neustar_soa.utils.RoundRobinAsyncCorbaClient;

public class SOARequestPollerServer extends NPACPollerServer{

   public SOARequestPollerServer(String key, String type)
						   throws ProcessingException{

	   super(key, type);
	   
   }

   protected Poller[] initPollers(String whereCondition,
								  int maxWorkerThreads,
								  long timer,
								  List npacComServerProperties)
								  throws ProcessingException{

	  SPID[] spids = getSPIDs( npacComServerProperties );

	  List newPollers = new ArrayList(spids.length+1);

	  // this is the client used to forward messages to the SOA server
	  RoundRobinAsyncCorbaClient client = initCorbaClient();

	  for(int i = 0; i < spids.length; i++){

		   try {
	
				SOARequestQueuePoller poller = new SOARequestQueuePoller(whereCondition,
														 				spids[i].toString(),
														 				null,
														 				maxWorkerThreads,
														 				timer,
														 				client);
	
			  	newPollers.add(poller);
	
		   }
		   catch (Exception ex) {
	
			  Debug.logStackTrace(ex);
			  throw new ProcessingException(
				 "Could not create poller for SPID [" +
				 spids[i] + "]: " + ex);
		   }
	  }

	  Poller[] pollerArray = new Poller[ newPollers.size() ];

	  newPollers.toArray(pollerArray);

	  return pollerArray;

   }

   private RoundRobinAsyncCorbaClient initCorbaClient()
									  throws ProcessingException{

	  String serverKey =
		 getRequiredPropertyValue(
		 LoadBalancingCorbaClient.SERVER_NAME_KEY_PROP);

	  String serverTypePrefix =
		 getRequiredPropertyValue(
		 LoadBalancingCorbaClient.SERVER_NAME_TYPE_PREFIX_PROP);
	  
	  long downServerRetryTime = Long.parseLong(
			  getRequiredPropertyValue(
						 LoadBalancingCorbaClient.DOWN_SERVER_RETRY_TIME_PROP));

	  List serverNames =
		 LoadBalancingCorbaClient.getServerNames(serverKey,
												 serverTypePrefix);
	 
	  if (serverNames.size() == 0) {
		 throw new ProcessingException("No " +
									   LoadBalancingCorbaClient.SERVER_NAME_PROP +
									   " properties were found under property key [" +
									   serverKey +
									   "] and property type prefix [" +
									   serverTypePrefix + "]");
	  }

	  // create the load-balancing corba client
	  RoundRobinAsyncCorbaClient client =
		 new RoundRobinAsyncCorbaClient(serverNames,serverKey,serverTypePrefix,downServerRetryTime);

	  return client;

   }

}
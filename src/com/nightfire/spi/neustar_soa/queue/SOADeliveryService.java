////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import com.nightfire.spi.neustar_soa.utils.RoundRobinAsyncCorbaClient;

import com.nightfire.mgrcore.queue.*;

import com.nightfire.framework.corba.CorbaException;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.Debug;

public class SOADeliveryService implements PushOperations {

   /**
    * The client used to deliver incoming NPAC messages to the SOA Server(s)
    * for processing.
    */
   private RoundRobinAsyncCorbaClient client;

   /**
    * Contructor.
    *
    * @param client RoundRobinAsyncCorbaClient the client used to deliver
    *               messages.
    */
   public SOADeliveryService( RoundRobinAsyncCorbaClient client ){

      this.client = client;

   }

   /**
    * This processes a message that was retrieved from the queue.
    *
    * @param message QueueMessage
    * @throws QueueException
    * @return String
    */
   public String push ( QueueMessage message ) throws QueueException{       

	   if(message instanceof SOAMessageType)
	   {		   

          SOAMessageType soaMessage = (SOAMessageType) message;

          try{
             client.send( "", soaMessage.getMessage() );
          }
          catch(CorbaException cex){

             Debug.error("Could not deliver message: "+cex.getMessage() );

             throw new ConnectivityDownException(cex.getMessage());

          }
          catch( FrameworkException fex ){

             Debug.error("Could not deliver message: ["+
                         soaMessage.getMessage()+"]: "+
                         fex.getMessage() );

             throw new QueueException( fex.getMessage() );

          }
          catch(Throwable unexpectedError){

             // An unexpected error here could kill the worker thread that is
             // processing queued messages. This catch-all will keep the
             // entire thread from dying.
             Debug.error("An unexpected error occured while delivering a SOA message: "+
                         unexpectedError);

             throw new QueueException( unexpectedError.toString() );

          }

       }

	   else if(message instanceof SOARequestMessageType)
	   {		   

          SOARequestMessageType soaMessage = (SOARequestMessageType) message;

          try{
             client.send( "", soaMessage.getMessage() );
          }
          catch(CorbaException cex){

             Debug.error("Could not deliver message: "+cex.getMessage() );

             throw new ConnectivityDownException(cex.getMessage());

          }
          catch( FrameworkException fex ){

             Debug.error("Could not deliver message: ["+
                         soaMessage.getMessage()+"]: "+
                         fex.getMessage() );

             throw new QueueException( fex.getMessage() );

          }
          catch(Throwable unexpectedError){

             // An unexpected error here could kill the worker thread that is
             // processing queued messages. This catch-all will keep the
             // entire thread from dying.
             Debug.error("An unexpected error occured while delivering a SOA message: "+
                         unexpectedError);

             throw new QueueException( unexpectedError.toString() );

          }

       }
	   else {
		   Debug.error("SOA Delivery Service received a message that is not an instance of "+
                      SOAMessageType.class.getName() );

	   }

       return null;

   }

}

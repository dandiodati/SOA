///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.adapter.Session;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

/**
* This class is used to wait for NewSession replies and kicks off
* the first AssociationRequest for that session when a successful reply is
* received.
*/
public class NewSessionReplyHandler extends NotificationHandler {

   /**
   * The status of the reply retrieved from the notification.
   */
   private String status;

   /**
   * The ID for the new session as retrieved from the reply message.
   */
   private String sessionID;

   /**
   * Constructor.
   *
   * @param adapter this is a reference to the NPACAdapter. This is
   *        used by
   */
   public NewSessionReplyHandler(NPACAdapter adapter,
                                 Session session){

      super(adapter, session);

   }

   /**
   * This is called when a notification is received to set the
   * notification that this handler will work with when its run()
   * method is called. It is assumed that this will get called before
   * the run method.
   */
   public int receiveNotification(XMLMessageParser parsedNotification){

      int ack = super.receiveNotification(parsedNotification);

      if(ack == NPACConstants.ACK_RESPONSE){

         try{

            // get the status of the response
            status = notification.getTextValue(
                                NPACConstants.NEW_SESSION_STATUS);

            if( status.equals(NPACConstants.SUCCESS_STATUS) ){

               // get the session ID from the notification
               sessionID =
                  notification.getTextValue(NPACConstants.NEW_SESSION_ID);

            }

         }
         catch(MessageException mex){

            Debug.error("Could not get data from new session reply: "+
                        mex);

            ack = NPACConstants.NACK_RESPONSE;

         }

      }

      return ack;

   }

   /**
   * This sends the first AssociationRequest for the session is the
   * NewSessionReply was successful. If not successful, then this retries
   * the original NewSession request.
   */
   public void run(){

      if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

         Debug.log(Debug.IO_STATUS,
                   "New session reply received with status: ["+
                   status+"]");

      }

      // if successful, then update the session
      if( status.equals(NPACConstants.SUCCESS_STATUS) ){

         if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

            Debug.log(Debug.IO_STATUS,
                      "Setting session ID: ["+
                      sessionID+"]");

         }

         // set the session ID
         session.setSessionID(sessionID);

         // initialize all associations for the new session
         for(int i = 0; i < NPACConstants.REGION_COUNT; i++){
            adapter.sendNextAssociationRequest(session, i);
         }

      }
      else{

         // if the response was a failure, try to reinitialize the session
         // again later
         adapter.retryNewSessionRequest(session);

      }

   }

   /**
   * This handles the case where a GatewayError has been sent in response
   * to our NewSessionRequest. This simply retries the NewSession request.
   *
   * @param error this is the parsed GatewayError.
   */
   protected void handleError(XMLMessageParser error){

      // retry the new session request later
      adapter.retryNewSessionRequest(session);

   }

   /**
    * This is called when the timer for this handler has expired before
    * a reply was received.
    */
   public void timeout(){
       if( !session.isAlive() ){
          adapter.sendNewSessionRequest(session);
       }
   }

   public String toString(){

      return "New Session Reply Handler";

   }

}

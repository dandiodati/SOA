///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;
import java.util.List;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.adapter.Session;

import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;

/**
* This is responsible for kicking off all required recovery/download requests
* for an association once a positive association reply is received.
*/
public class AssociationReplyHandler extends NotificationHandler {

   /**
   * The region for which the AssoicationRequest was sent.
   */
   private int currentRegion;

   /**
   * The connection status value as retrieved from the
   * AssociationReply.
   */
   private String replyStatus;

   /**
   * The time that the AssociationReply was sent as retrieved from the
   * AssociationReply header. This is used in determining
   * when recovery is complete for the Association.
   */
   private Date initializationTime;

   private boolean timeBasedStatus = false;

   /**
   * Constructor.
   *
   * @param adapter the NPACAdapter instance used to make call backs to
   *                the adapter to resend the request or to kick off
   *                the AssociationRequest for the next region.
   * @param session the session that the association belongs to.
   * @param region the region for the Association.
   */
   public AssociationReplyHandler(NPACAdapter adapter,
                                  Session session,
                                  int region){

      super(adapter, session);

      currentRegion = region;

   }

   /**
   * This receives the parsed notification from the NPACComServer
   * and retrieves the necessary fields from the XML.
   */
   public int receiveNotification(XMLMessageParser parsedNotification){

      int ack = super.receiveNotification(parsedNotification);

      try{

         replyStatus = parsedNotification.getTextValue(
                          NPACConstants.ASSOCIATION_REPLY_STATUS);

         String dateString =
            parsedNotification.getTextValue( NPACConstants.MESSAGE_DATE_TIME );

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

            Debug.log(Debug.MSG_STATUS,
                      "Association reply status: ["+replyStatus+"]");

            Debug.log(Debug.MSG_STATUS,
                      "Association message date/time: ["+dateString+"]");

            Debug.log(Debug.MSG_STATUS,
                      "Session status: "+
                       session.toString() );

         }

         initializationTime =
            TimeZoneUtil.parse(NPACConstants.UTC,
                               NPACConstants.UTC_TIME_FORMAT,
                               dateString);

      }
      catch(MessageException mex){

         Debug.error("Could not get status from association response: "+
                     mex);
         ack = NPACConstants.NACK_RESPONSE;

      }

      return ack;

   }

   /**
   * This is executed in a worker thread and determines what to
   * do next based on the AssociationReply. In the case of a
   * successful reply, this sends the Association request for the
   * next region.
   */
    public void run(){

      if( replyStatus.equals( NPACConstants.CONNECTED_STATUS ) ){

         // flag the association as connected
         session.setAssociationRecovering( currentRegion );

         // Send the first download recovery request for this association.
         // The date time that the association reply was received
         // is used in determining when recovery is complete.

		 timeBasedStatus = adapter.getRecoveryStatus( currentRegion );

		 Debug.log(Debug.NORMAL_STATUS, "timeBasedStatus value in run():"+ timeBasedStatus );

		 if (timeBasedStatus)
		 {
			 adapter.sendTimeBasedFirstDownloadRecoveryRequest(session,
											  currentRegion,
											  initializationTime);
			 Debug.log(Debug.NORMAL_STATUS, "timeBasedStatus value in if:"+ timeBasedStatus );
		 }
		 else
		 {
			adapter.sendFirstDownloadRecoveryRequest(session,
                                                  currentRegion,
                                                  initializationTime);	
			Debug.log(Debug.NORMAL_STATUS, "timeBasedStatus value in else:"+ timeBasedStatus );
		 }
      }
      else{

         Debug.error("Association request for region ["+
                     currentRegion+
                     "] returned a status of ["+
                     replyStatus+"].");

         handleFailure();

      }

   }

   /**
   * This handles the case where a GatewayError has been sent in response
   * to our AssociationRequest. The behavior here is exactly the same
   * as if a negative AssociationReply had been received. This
   * association request will be retried, and the next AssociationRequest will
   * sent for the next region.
   *
   * @param error this is the parsed GatewayError.
   */
   protected void handleError(XMLMessageParser error){

      handleFailure();

   }

   /**
   * This is the common behavior that is executed when a negative
   * AssociationReply is received or when a GatewayError is
   * received. This
   * association request will be retried.
   *
   */
   private void handleFailure(){

      // retry this association connect request later
      adapter.retryAssociationRequest(session, currentRegion);

   }  

   /**
    * This is called when the timer for this handler has expired before
    * a reply was received.
    */
   public void timeout(){
      adapter.sendAssociationConnectRequest(session, currentRegion);
   }

}

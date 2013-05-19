///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.TimerTask;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.adapter.Session;

import com.nightfire.framework.message.parser.xml.XMLMessageParser;

/**
* This is the base class for classes that are created to wait for
* reply notifications from the OSS Gateway.
*/
public abstract class NotificationHandler implements Runnable {

   /**
   * The parsed reply notification. This is set by receiveNotification().
   */
   protected XMLMessageParser notification;

   /**
   * This instance of the adapter is used to send the next required requests
   * and perform any retries when a response is received.
   */
   protected NPACAdapter adapter;

   /**
    * This timer task is set to timeout and cancel this handler in case a
    * reply is never received. This timer task will be cancelled if a reply is
    * received.
    */
   protected TimerTask timeoutTimer;

   /**
   * The session involved.
   */
   protected Session session;

   public NotificationHandler(NPACAdapter adapter,
                              Session session ){

      this.adapter = adapter;
      this.session = session;

   }

   /**
   * This is called when a notification is received to set the
   * notification that this handler will work with when its run()
   * method is called. It is assumed that this will get called before
   * the run method.
   */
   public int receiveNotification(XMLMessageParser parsedNotification){

      // cancel the timeout timer, if there is one
      if( timeoutTimer != null ){
         timeoutTimer.cancel();
      }

      notification = parsedNotification;

      return NPACConstants.ACK_RESPONSE;

   }

   /**
    * This is called when a GatewayError is received instead of the
    * expected response. This method cancels the timeout timer, and
    * calls handleError(error).
    *
    * @param error XMLMessageParser
    */
   public void receiveError(XMLMessageParser error){

      // cancel the timeout timer, if there is one
      if( timeoutTimer != null ){
         timeoutTimer.cancel();
      }

      handleError(error);

   }

   /**
   * Accesses the session that is waiting for the response notification.
   */
   public Session getSession(){

      return session;

   }

   /**
   * This method is called when a GatewayError was returned instead of
   * the expected response notification.
   */
   protected abstract void handleError(XMLMessageParser error);

   /**
    * This method will get called should the timeout timer expire while
    * waiting for a reply.
    */
   public abstract void timeout();

   /**
    * This sets a reference to the timeout timer task. This timer task
    * is set to timeout and cancel this handler in case a reply is never
    * received. This task will be cancelled if a reply is received.
    *
    * @param timer TimerTask
    */
   public void setTimeoutTimer(TimerTask timer){

      timeoutTimer = timer;

   }

   /**
    * Cancels the timeout timer (if any) associated with this task.
    */
   public void cancelTimeoutTimer(){

      if( timeoutTimer != null ){
         timeoutTimer.cancel();
      }

   }

}

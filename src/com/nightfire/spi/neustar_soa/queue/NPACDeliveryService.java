////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import com.nightfire.mgrcore.queue.*;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.NPACComServer;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

public class NPACDeliveryService implements PushOperations{

   /**
    * This takes a message and delivers it to the NPAC. This
    * implementation assumes that the NPACCommServer is running
    * within this same Java process.
    *
    * @param message Message to be pushed.
    *
    * @return String The receipt for the message being pushed (optional).
    * If no receipt is to be returned, a null is returned.
    *
    * @throws QueueException if the push operation fails.
    */
    public String push ( QueueMessage message ) throws QueueException{

       try{

          if (message instanceof NPACMessageType) {

             NPACMessageType npacMessage = (NPACMessageType) message;

             // get access to the NPAC Client
             NPACAdapter adapter = NPACComServer.getAdapter();

             String spid = npacMessage.getSPID();
             String sessionID = adapter.getSessionID(spid);

             if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
                Debug.log(Debug.MSG_STATUS,
                          "The session ID for SPID [" + spid +
                          "] is [" + sessionID + "]");
             }

             // No primary SPID or session object could be found for the
             // given SPID. This means that this SPID is not defined.
             if (sessionID == null) {
                throw new QueueException("The SPID [" + spid +
                                         "] is not configured!");
             }

             // Check to see if the session is down
             if (sessionID.equals(NPACConstants.UNINITIALIZED_SESSION_ID)) {

                throw new ConnectivityDownException( "The session for spid ["+
                                                     spid +
                                                     "] is down.");

             }

             // set session ID in the NPAC XML message
             String xml = npacMessage.getMessage();

             Integer invokeID = npacMessage.getInvokeID();

             try {

                XMLPlainGenerator parsedXML = new XMLPlainGenerator(xml);

                // Set the current session ID.
                parsedXML.setText(NPACConstants.SESSION_ID, sessionID);

                try {

                   // send the message to the OSS NPAC GW
                   int ack = adapter.send(spid,
                                          invokeID.toString(),
                                          parsedXML.getOutput());

                   if (ack == NPACConstants.NACK_RESPONSE) {

                      throw new QueueException("Send failed for message: " +
                                               message.describe());

                   }

                }
                catch (FrameworkException fex) {
                   throw new QueueException(fex.getMessage());
                }

             }
             catch (MessageException mex) {

                throw new QueueException("Could not parse XML: " +
                                         mex.getMessage());

             }

          }
          else {
             Debug.error(
                "NPAC Delivery Service received a message that is not an instance of " +
                NPACMessageType.class.getName());
          }

       }
       catch(QueueException quex){
          // catch and rethrow any queue exceptions
          throw quex;
       }
       catch(Throwable unexpectedError){

          // An unexpected error here could kill the worker thread that is
          // processing queued messages. This catch-all will keep the
          // entire thread from dying.
          Debug.error("An unexpected error occured while delivering an NPAC message: "+
                      unexpectedError);

          throw new QueueException( unexpectedError.toString() );

       }

       return null;

    }


}

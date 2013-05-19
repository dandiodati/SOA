/**
 * This is the Reply Handler, Once the SwimProcessingResultRecoveryReply for the 
 * Recovery data received by the NPAC COM server then it will initiate this reply 
 * handler.
 * 
 * @author Sreedhar K
 * @version 3.3
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.message.parser.xml.XMLMessageParser;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
 * @see com.nightfire.spi.neustar_soa.adapter.NPACConstants;
 * @see com.nightfire.spi.neustar_soa.adapter.Session;
 * @see com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;
 * @see java.util.List;
 */
   
/**
	Revision History
	---------------------
	Rev#        Modified By     Date                Reason
	-----       -----------     ----------          --------------------------
	1           Sreedhar         09/04/2005         Created
	2           Sreedhar         10/10/2005         modified to implement 
	                                                swim_more_data
*/ 

package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.adapter.Session;
import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;
import java.util.List;



public class RecoveryReplySwimProcessingRecoveryReplyHandler extends
        NotificationHandler {
    
	/*
	 * The status of the reply retrieved from the notification.
	 */
	private String status;
    
     /*
     * Spid involved in the request    
     */ 
    private String spid;
    
    /*
     * List of spids associated with current region
     */
    private List spids;
    
    /*
     * This is the index of the customer SPID in the spids list for
     * which we are currently performing recovery.
     */   
    private int spidIndex;    
    
    /*
     *  Revcovery complete time 
     */
    private Date recoveryCompleteTime;
    
    /*
     *  LasNotification time we got from NPAC
     */
    private Date lastNotificationTime;
    
    /*
     * action Id we populated in the reply.
     */
    private String swimActionID;
    
    /*
     * The region/association for which we are recovering.
     */
    private int region;
    
    /*
     * Recovery interval to send the recovery again.
     */
    private long recoveryInterval;
    
    /*
     * time we received from NPAC Swim Reply
     */
    private Date npacStopTime;
   
    // constructor   

    public RecoveryReplySwimProcessingRecoveryReplyHandler( 
                                         NPACAdapter adapter,
                                         Session session,
                                         List spids,
                                         int spidIndex,
                                         Date recoveryCompleteTime, 
                                         Date lastNotificationTime,
                                         String swimActionID, 
                                         int region,
                                         long recoveryInterval) {
        super(adapter, session);
        this.spids = spids;
        this.spidIndex = spidIndex;
        this.recoveryCompleteTime = recoveryCompleteTime;
        this.lastNotificationTime = lastNotificationTime;
        this.swimActionID = swimActionID;
        this.region = region;
        this.recoveryInterval= recoveryInterval;


    }
    
    /**
     * This is called when a notification is received to set the
     * notification that this handler will work with when its run()
     * method is called. It is assumed that this will get called before
     * the run method.
     * @param parsedNotification XMLMessageParser object received 
     *                            from notificaton.
     */
    
    public int receiveNotification(XMLMessageParser parsedNotification) {
               int ack = super.receiveNotification(parsedNotification);
		
		npacStopTime = null;

        if (ack == NPACConstants.ACK_RESPONSE) {

            try {

                // get the status of the response
                status = notification
                        .getTextValue(NPACConstants.
                                SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_STATUS);
                if(parsedNotification.exists(NPACConstants.
                        SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_STOPTIME)){
                
                String stopTime = notification.getTextValue(NPACConstants.
                        SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_STOPTIME);
                npacStopTime  = TimeZoneUtil.parse(NPACConstants.UTC,NPACConstants.UTC_TIME_FORMAT,stopTime);
                }

            } catch (MessageException mex) {

                Debug
                        .error("Could not get data from Swim Recovery Reply "
                                + mex);

                ack = NPACConstants.NACK_RESPONSE;

            }

        }

        return ack;

    }
    
    /**
     * This handles the case where a GatewayError has been sent in response
     * to our SwimRequest for service provider data. This simply retries the
     *  Swim request for Recovery request.
     *  
     * @param error this is the parsed GatewayError.
     */


    protected void handleError(XMLMessageParser error) {
        // create a retry task to retry the recovery request

        adapter.retryRecoverySwimRequest(session, region, spids, spidIndex, recoveryCompleteTime,
                lastNotificationTime, swimActionID, spid,recoveryInterval);

    }
    
    /**
     * This is called when the timer for this handler has expired before
     * a reply was received.
     */


    public void timeout() {
        adapter.sendRecoverySwimRequest(session, 
                                        region,
                                        spids,
                                        spidIndex,
                                        recoveryCompleteTime, 
                                        lastNotificationTime, 
                                        swimActionID, 
                                        spid, 
                                        recoveryInterval);
    }
    
    /**
     * This gives off the batched reply to Driver chanin for processing  for the swim
     *  request was successful. If not successful, then this retries
     * the original Swim request for Recovery request.
     */

    public void run() {
        if (Debug.isLevelEnabled(Debug.IO_STATUS)) {

            Debug.log(Debug.IO_STATUS,
                    " Recovery Request Swim Processing  status: [" + status
                            + "]");

        }

        // if successful, then update the session
        if (status.equals(NPACConstants.SUCCESS_STATUS)) {
            
            //adapter.process(notification);
            
            if(npacStopTime!= null  )
            {
                
                Date recoveryStartTime = npacStopTime;
               
                adapter.sendRecoveryRequest(session,
                                             region,
                                             spids,
                                             spidIndex,
                                             null,
                                             recoveryCompleteTime,
                                             recoveryStartTime,
                                             recoveryInterval,
                                             true);

            }else
            {
          
                adapter.sendRecoveryRequest(session,
                        region,
                        spids,
                        ++spidIndex,
                        null,
                        recoveryCompleteTime,
                        null,
                        recoveryInterval,
                        false);
            }

        } else {

			if(npacStopTime!= null  )
            {
                
                Date recoveryStartTime = npacStopTime;
               
                adapter.sendRecoveryRequest(session,
                                             region,
                                             spids,
                                             spidIndex,
                                             null,
                                             recoveryCompleteTime,
                                             recoveryStartTime,
                                             recoveryInterval,
                                             true);
			}
			else{

				adapter.retryRecoverySwimRequest(session, 
												 region,
												 spids, 
												 spidIndex,
												 recoveryCompleteTime,
												 lastNotificationTime,
												 swimActionID,
												 spid,
												 recoveryInterval);
			}
        }

    }

}

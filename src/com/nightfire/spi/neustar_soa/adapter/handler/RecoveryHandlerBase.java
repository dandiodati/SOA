////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.util.Date;
import java.util.List;

import com.nightfire.spi.neustar_soa.adapter.*;

import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

public abstract class RecoveryHandlerBase extends NotificationHandler
implements AssociationListener{
    
   /**
    * The region/association for which we are recovering.
    */
   protected int region;
   
   /**
    *  Revcovery complete time 
    */ 
   protected Date recoveryCompleteTime;
   
   /**
    *  LasNotification time we got from NPAC
    */
   protected Date lastNotificationTime;
   
   /**
    * Instance of lastNotificationTimeAccess object
    */
   protected LastNotificationTime lastNotificationTimeAccess;
   
   /**
    * This flag sets true if we populate swim_more_data or  
    * npac_stop_time in the reply messages from NPAc
    */
   protected boolean isTimeRangeRequest;

   /**
   * This is the length of the recovery query (in ms). This is
   * used when a "criteria-too-large" response is received to
   * make sure that the next recovery request is sent using
   * a smaller interval of time.
   */
   protected long recoveryInterval;

   /**
   * This is the value of the recovery status as retrieved from the
   * RecoveryResponse. This is extracted from the response by
   * receiveNotification() and used by the run() method in
   * determining how to handle the response.
   */
   protected String status;
   
   /**
    * action Id we populated in the reply.
    */   
   protected String swimActionID;
   
   /**
    * This variable strore what kind of DRR we are getting like 
    * network or service_prov
    */   
   protected String downloadRecoveryReplyType;

   /**
   * This is the list of customer SPIDs that require recovery for this
   * region.
   */
   protected List spids;

   /**
   * This is the index of the customer SPID in the spids list for
   * which we are currently performing recovery.
   */
   protected int spidIndex;

   /**
   * This flag is set to true if the status of the related association changes
   * while we are waiting for the RecoveryReply. If this flag is true,
   * then the RecoveryReply will be ignored.
   */
   private boolean aborted = false;

   public RecoveryHandlerBase(NPACAdapter adapter,
                              Session session,
                              int region,
                              List spids,
                              int spidIndex,
                              Date recoveryCompleteTime,
                              Date lastNotificationTime,
                              LastNotificationTime lastNotificationTimeAccess,
                              long interval,
                              String requestType,
                              boolean isTimeRangeRequest){

      super(adapter, session);

      this.region = region;
      this.spidIndex = spidIndex;
      this.recoveryCompleteTime = recoveryCompleteTime;
      this.lastNotificationTime = lastNotificationTime;
      this.recoveryInterval = interval;
      this.downloadRecoveryReplyType =requestType;
      this.isTimeRangeRequest = isTimeRangeRequest;
      this.spids= spids;
      this.lastNotificationTimeAccess = lastNotificationTimeAccess;
     
      // listen for changes to the related association
      session.addAssociationListener(this);

   }

   public int receiveNotification(XMLMessageParser parsedNotification){

      int ack = super.receiveNotification(parsedNotification);

      status = getStatus(parsedNotification);
      
      if(status != null && status.trim() !=NPACConstants.FAILED_STATUS ){
      swimActionID =getSwimActionId(parsedNotification);
   
      }
     
      if( Debug.isLevelEnabled( Debug.IO_STATUS ) ){

         Debug.log( Debug.IO_STATUS,
                    this+
                    " received recovery response status ["+status+"]" );

      }

      return ack;

   }

   public void run(){

      // stop listening for association changes
      session.removeAssociationListener(this);

      if(aborted){

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

             Debug.log(Debug.MSG_STATUS,
                       "A recovery response was received after the "+
                       "association was reinitialized. This response will "+
                       "be ignored.");

         }

         return;

      }

      // check that the reply had a status value
      if( status != null ){

         // if the request was successful or if there was not data to be found,
         // then proceed happily...
    	  
    	 if(status.equals(NPACConstants.SUCCESS_STATUS)){
    		 
    		 Debug.log(Debug.SYSTEM_CONFIG,"Status success");

             // pass the batched reply off to the driver chain for processing
              
         	 adapter.process(notification);
         	 
    	 }
    	 
         if( status.equals(NPACConstants.SUCCESS_STATUS) || status.equals(NPACConstants.NO_DATA_SELECTED_STATUS) ){
        	 
        	 Debug.log(Debug.SYSTEM_CONFIG,"Status "+ status);

            // we've received a successful response, send the swim request
           
            if(swimActionID != null){
            	
            	isTimeRangeRequest=false;
            	sendSwimRequest( downloadRecoveryReplyType,swimActionID );
            	
            }else {
            	
            	String  spid = spids.get(spidIndex).toString();
            	
            	if (downloadRecoveryReplyType != null
                        && downloadRecoveryReplyType
                                .equals(NPACConstants.DOWNLOAD_DATA_NET_WORK_DATA)) {
                    try
                    {
                    lastNotificationTimeAccess.setLastNetworkNotificationTime(spid,
                                                                              region,
                                                                 lastNotificationTime);
                    
                  }
                  catch(DatabaseException dbex){

                    Debug.error("Could not log last netowork notification time for "+
                                "download recovery reply: "+dbex);
                  }
                  
                  Debug.log(Debug.SYSTEM_CONFIG,"in side RecoveryHandlerBase() for Next SWIM N/W data recovery ::"
	                        +recoveryCompleteTime);
                  
                  if(isTimeRangeRequest){
                	  Debug.log(Debug.MSG_STATUS,"Time Range Normal SV Recovery Request");
                	  adapter.sendDownloadRecoveryRequestNetWorkData(session,
																	 region,
																	 spids,
																	 spidIndex,
																	 null,
																	 recoveryCompleteTime,
																	 lastNotificationTime,
																	 recoveryInterval,
																	 true);
                	  
                  }else{
                	  
                	  Debug.log(Debug.MSG_STATUS," SWIM N/W Recovery Request");
                	  adapter.sendDownloadRecoveryRequestNetWorkData(session,
																	 region,
																	 spids,
																	 ++spidIndex,
																	 null,
																	 recoveryCompleteTime,
																	 null,
																	 recoveryInterval,
																	 false);
                  }
            		
            	}else if (downloadRecoveryReplyType != null
                        && downloadRecoveryReplyType
                        .equals(NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA)){
             
             
            		try
            		{
            			lastNotificationTimeAccess.setLastServiceProvNotificationTime(spid,
	                                                                       region,
	                                                          lastNotificationTime);
	             
		           }
		           catch(DatabaseException dbex){
		
		             Debug.error("Could not log last ServiceProvider notification time for "+
		                         "download recovery reply: "+dbex);
		           }
	            		
		           
		           Debug.log(Debug.SYSTEM_CONFIG,"in side RecoveryHandlerBase() for Next SWIM SP recovery ::"
	                        +recoveryCompleteTime);
	                if(isTimeRangeRequest){
	                	
	                	Debug.log(Debug.MSG_STATUS,"Time Range Normal SP Recovery Request");
	                	adapter.sendDownloadRecoveryRequestServiceProvidersData(session,
														                         region,
														                         spids,
														                         spidIndex,
														                         null,
														                         recoveryCompleteTime,
														                         lastNotificationTime,
														                         recoveryInterval,
														                         true);
	                	
	                }else {
	                	Debug.log(Debug.MSG_STATUS," SWIM SP Recovery Request");
	                	adapter.sendDownloadRecoveryRequestServiceProvidersData (session,
														                         region,
														                         spids,
														                         ++spidIndex,
														                         null,
														                         recoveryCompleteTime,
														                         null,
														                         recoveryInterval,
														                         false);
	                }
	                
		           
            	}else if (downloadRecoveryReplyType!= null && 
            							downloadRecoveryReplyType.equals
            								(NPACConstants.RECOVERY_REQUEST)){
             
             
		           try
		           {
		             lastNotificationTimeAccess.setLastNotificationTime(spid, region,
		                                                          lastNotificationTime);
		             
		           }
		           catch(DatabaseException dbex){
		
		             Debug.error("Could not log last SubscriptionVersion notification time for "+
		                         "recovery reply: "+dbex);
		           }
		           Debug.log(Debug.SYSTEM_CONFIG,"in side RecoveryHandlerBase() for Next SWIM SV Notifications recovery ::"
	                        +recoveryCompleteTime);
		           
		           if(isTimeRangeRequest){
		        	   Debug.log(Debug.MSG_STATUS,"Time Range Normal SV Recovery Request");
		        	   adapter.sendRecoveryRequest(session,
					                               region,
					                               spids,
					                               spidIndex,
					                               null,
					                               recoveryCompleteTime,
					                               lastNotificationTime,
					                               recoveryInterval,
					                               true);
		           }else{
		           
		        	   Debug.log(Debug.MSG_STATUS," SWIM SV Recovery Request");
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
		           
            	}
            	
            }


         }
         
         else if(status.equals(NPACConstants.SWIM_MORE_DATA_STATUS))
         {
             isTimeRangeRequest=true;
             adapter.process(notification);
             if(downloadRecoveryReplyType!=null && downloadRecoveryReplyType.
                     equals(NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA))
             {
                 
                 adapter.sendDownloadRecoveryRequestServiceProvidersData
                                                       (session,
                                                        region,
                                                        spids,
                                                        spidIndex,
                                                        swimActionID,
                                                        recoveryCompleteTime,
                                                        lastNotificationTime,
                                                        recoveryInterval,
                                                        isTimeRangeRequest);
             }else if(downloadRecoveryReplyType!=null && 
                     downloadRecoveryReplyType.equals
                     (NPACConstants.DOWNLOAD_DATA_NET_WORK_DATA))
             {
                 adapter.sendDownloadRecoveryRequestNetWorkData
                                                    (session,
                                                     region,
                                                     spids,
                                                     spidIndex,                         
                                                     swimActionID,
                                                     recoveryCompleteTime,
                                                     lastNotificationTime,
                                                     recoveryInterval,
                                                     isTimeRangeRequest);
             }else if (downloadRecoveryReplyType!= null && 
                     downloadRecoveryReplyType.equals
                     (NPACConstants.RECOVERY_REQUEST))
             {
                 isTimeRangeRequest=false;
                 adapter.sendRecoveryRequest(session,
											 region,
											 spids,
											 spidIndex,
											 swimActionID,
											 recoveryCompleteTime,
											 lastNotificationTime,
											 recoveryInterval,
											 isTimeRangeRequest);
                 
             }
                     
             
         }
         else if( status.equals( NPACConstants.CRITERIA_TOO_LARGE ) ||
                  status.equals( NPACConstants.TIME_RANGE_INVALID )  ){

            // If we get a reply of criteria-too-large, we assume that
            // the number of notifications that would be recovered for
            // the current time interval was too large, and the NPAC
            // did not want to send it to us. We divide the recovery interval
            // by 2 and try again with the hopes that the number of
            // notifications found for that period will be small enough to
            // recover.
            if(recoveryInterval > 1000){

               // The last notification time is the "end" time
               // for our query interval, so we need to subtract
               // the previous recovery interval.
               long lastNotificationTimeMs = lastNotificationTime.getTime();
               lastNotificationTimeMs -= recoveryInterval;
               // then subtract one additional second that was added by the
               // sendRecoveryRequest() method to make sure we didn't
               // query the last notification time twice.
               lastNotificationTimeMs -= 1000;
               // reset the time
               lastNotificationTime.setTime(lastNotificationTimeMs);

               // divide the interval in half
               long smallerInterval = recoveryInterval/2;

               if( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){

                  Debug.log( Debug.MSG_STATUS,
                             "Retrying recovery request with an interval of ["+
                             smallerInterval+"] ms" );

               }

               sendSmallerInterval( smallerInterval );

            }
            else{

               // recovery failed for an interval of only one second
               smallestIntervalFailed();

            }

         }
         
         else{
             
             Debug.log(Debug.SYSTEM_CONFIG, "In side the handle error method");

            // the reply status was "failed"
            handleError();

         }

      }
     

   }

   protected void handleError(XMLMessageParser error){

      // stop listening for association changes
      session.removeAssociationListener(this);

      handleError();

   }

   protected abstract void handleError();
   
   protected abstract void sendSwimRequest(String recoveryReplyType,
                                           String swimActionID);
   

   //protected abstract void sendNextRequest();

   protected abstract void sendSmallerInterval(long smallerInterval);

   protected abstract void smallestIntervalFailed();

   /**
    * Utility method for getting the status value from a response.
    *
    * @param parser XMLMessageParser
    * @return String
    */
   protected abstract String getStatus(XMLMessageParser parser);
   
   /**
    * Utility method for getting the SWIM ActionIDtus value from a response.
    *
    * @param parser XMLMessageParser
    * @return String
    */
   
   protected abstract String getSwimActionId(XMLMessageParser parser);
   
   protected abstract String  getDownloadRecoveryReplyType
                                            (XMLMessageParser parser);

   /**
   * This method is called by the Session when the state of
   * an association changes (for example it goes down, completes
   * recovery, etc.).
   *
   * This reply handler will be aborted if the status of the association
   * changes while we are still waiting for a RecoveryReply. If the
   * association status has changed while we are waiting, this means that
   * the association was probably aborted. If this happens,
   * we need to disable/abort this handler, because a negative RecoveryReply
   * may still be received due to a timeout in the OSS Gateway.
   *
   * This defines the AssociationListener interface.
   *
   */
   public void associationStateChanged(AssociationEvent event){

      // check whether the change occured in this region
      if( region == event.getRegion() ){

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

            Debug.log(Debug.MSG_STATUS,
                      "Association status for region ["+region+
                      "] has changed. Recovery reply handler ["+
                      this+"] will be disabled.");

         }

         aborted = true;

      }

   }


   /**
    * This is called when the resend timer for this handler has expired before
    * a reply was received.
    */
   public abstract void timeout();

   /**
    * Allows subclassed to check whether this handler has been aborted due
    * to a change in the association's status.
    *
    * @return boolean
    */
   protected boolean isAborted(){
      return aborted;
   }

}

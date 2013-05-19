///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import com.nightfire.spi.neustar_soa.adapter.handler.*;
import com.nightfire.spi.neustar_soa.queue.NPACQueueUtils;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Set;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentSequence;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.FrameworkException;

import com.nightfire.framework.util.Debug;

/**
* This is the class that takes contains all of the methods for interacting
* with the NPAC's OSS Gateway.
*/
public class NPACAdapter{

   /**
   * This is the SOAP client used to send requests to the NPAC gateway.
   */
   private NPACClient client;

   /**
   * This maps secondary SPIDs to their primary SPID.
   */
   private Map secondaryToPrimary = new HashMap();

   /**
   * This maps a primary SPID to its Session object.
   */
   private Map primaryToSession = new HashMap();

   /**
   * This is used to find the handler for an incoming notification based
   * on its invoke ID. When a request is sent, a NotificationHandler instance
   * is added to this table (keyed off its invoke ID) in order to wait for
   * the response with the same invoke ID.
   */
   private WaitingHandlers replyHandlers;

   /**
   * This is the name of the sequence used to generate the invoke IDs.
   */
   private String invokeIDSequence;

   /**
   * This is the user ID that will be sent with any NewSession requests.
   */
   private String userID;
   
   /**
   * Runnable tasks are added to this queue in order
   * to perform work in separate worker threads.
   */
   private WorkQueue queue;

   /**
   * This is the length of time, in milliseconds, that the adapter should
   * wait before attempting to resend a failed request.
   */
   private long retryInterval;

   /**
   * The window of time (in milliseconds) used to determine the
   * download range for for recovery requests.
   */
   private long recoveryWindow;

   /**
    *
    */
   private long recoveryPadding;

   HashMap dbRecoveryMap = new HashMap();

   /**
   * This is use to get and set the last notification times for
   * notifications.
   */
   private LastNotificationTime lastNotificationTimeAccess;

   /**
    * The ammount of time (in ms) that we should wait before attempting
    * to resend a message that has received no reply.
    */
   private long resendTimeout;

   /**
    * This reference is used to call back to the NPAC Com Server in order
    * to process a message using the driver chain.
    */
   private NPACComServer comServer;

   /**
    * The XPath for extracting the message status.
    */
   private static ParsedXPath statusPath;	

   /**
    * The XPath for extracting any error code from a message.
    */
   private static ParsedXPath errorCodePath;

   // initialize static XPath variables
   static{

      try{
         statusPath = new ParsedXPath("//status/text()|//error_status/text()");
         errorCodePath = new ParsedXPath("//error_reason/error_code/text()");
      }
      catch(FrameworkException fex){
          Debug.logStackTrace(fex);
      }

   }

   /**
   * The Constructor.
   *
   * @param invokeIDSequence the name of the DB sequence used to generate the
   *                         next invoke ID.
   * @param userID This is the user name used by the to log into the NPAC
   *               via SmartSockets.
   *               This will also be used to populate the "user_id" field in
   *               NewSession requests.
   * @param password The password used to login to the NPAC.
   * @param retryInterval this is the period of time, in ms, that a retry
   *                      task will wait between retries.
   * @param resendInterval the ammount of time, in ms, that we should wait
   *                       before resending a request that received no reply.
   * @param lastNotificationTime this instance provides access to the
   *                             last notification time table.
   * @param recoveryInterval this is the interval of time, in ms,
   *                         that will be used to increment the
   *                         the recovery window. Recovery requests
   *                         can only be made for a range of 1 hour, max.
   * @param recoveryInterval this is an additional fudge factor that
   *                         will be subtracted from the last notification
   *                         time when doing recovery.
   * @param workerThreads the queue into which work can be placed to get
   *                      executed by a collection of worker threads.
   * @param comServer a reference to the NPACComServer for the purpose of
   *                  callbacks.
   */
   public NPACAdapter(String invokeIDSequence,
                      String npacProject,
                      String npacSubject,
                      String userID,
                      String password,
                      String npacServer,
                      String replyToSubject,
                      long retryInterval,
                      long resendInterval,
                      LastNotificationTime lastNotificationTime,
                      long recoveryInterval,
                      long recoveryPadding,
                      WorkQueue workerThreads,
					  HashMap recoveryMap,
                      NPACComServer comServer )
                      throws ProcessingException{

      // create the SmartSockets client
      try{

         client = new NPACClient(npacProject,
                                 npacSubject,
                                 userID,
                                 password,
                                 npacServer,
                                 replyToSubject);
      }
      catch(FrameworkException fex){
         throw new ProcessingException( fex.getMessage() );
      }

      this.invokeIDSequence = invokeIDSequence;
      this.userID = userID;

      // use the given worker queue
      queue = workerThreads;
      this.retryInterval = retryInterval;
      this.resendTimeout = resendInterval;
      this.lastNotificationTimeAccess = lastNotificationTime;
      this.recoveryWindow = recoveryInterval;
      this.recoveryPadding = recoveryPadding;
      this.dbRecoveryMap = recoveryMap;
	  this.comServer = comServer;

      replyHandlers = new WaitingHandlers(queue);

   }

   /**
   * This adds a secondary SPID (and its region information) to the Session
   * object for the given primary SPID. If no Session instance exists for the
   * primary SPID yet, a new Session instance will be created.
   * This method is called by the NPACComServer during initialization
   * to add the necessary SPIDs.
   *
   * @param primarySPID a primary SPID. There is a one-to-one correlation
   *                    between sessions that will be created an primary SPIDs.
   *
   * @param secondarySPID the representation of a secondary SPID (a customer
   *                      SPID) and the regions that it supports. This
   *                      secondary SPID will now "belong" to the given primary
   *                      SPID.
   */
   public void add(String primarySPID,
                   SecondarySPID secondarySPID){
	   
	
      // get session for primary SPID
      Session session = getSession(primarySPID);

      if( session == null ){
    	  
             synchronized( primaryToSession ){

               // create new session for this primary SPID
               session = new Session( primarySPID );

               // add session mapping
               primaryToSession.put( primarySPID, session );

         }

      }

      // add the secondary SPID to the session
      session.add( secondarySPID );

      // map the secondary spid its primary spid
      secondaryToPrimary.put( secondarySPID.getSPID(), primarySPID );

   }

   /**
   * This sends the initial NewSession requests for any configured
   * primary SPIDs. This is called by the NPACComServer to
   * initialize the adapter's connectivity with the NPAC OSS Gateway.
   */
	   
   public void initialize(){
	   
	   // for each defined Session/primary SPID, send a NewSession request
      Session[] sessions = getSessions();
    
      if(Debug.isLevelEnabled(Debug.ALL_WARNINGS)){
         if(sessions.length == 0){
            Debug.warning("No primary SPIDs have been added to the NPAC "+
                          "adapter. No sessions will be initialized.");
         }
      }

      for(int i = 0; i < sessions.length; i++){
    
         sendNewSessionRequest( sessions[i] );

      }

   }

   /**
   * This resets the given session and sends a new NewSession request
   * for this session.
   */
   private void reinitialize(Session session){

      Debug.log(Debug.NORMAL_STATUS, "Reinitializing session:\n"+session);

      session.reset();

      NewSessionRetry createNewSession = new NewSessionRetry(this,
                                                             session,
                                                             0);
      enqueue(createNewSession);

   }

   /**
   * This is called when a GatewayReleaseSession notification is received.
   * This reinitializes the session with the given session ID.
   *
   * @param sessionID the session ID of the session to be released.
   */
   public int receiveReleaseSession(String sessionID){

      int ack = NPACConstants.NACK_RESPONSE;

      Session session = getSessionWithID(sessionID);

      if(session == null){
         Debug.error("Could not release session. No session found with ID ["+
                     sessionID+"]");
      }
      else{

         // reinitialize the session, try to create a new session
         reinitialize(session);

         // we have successfully handled the ReleaseSession, congratulate
         // ourselves by returning an ACK
         ack = NPACConstants.ACK_RESPONSE;

      }

      return ack;

   }

   /**
   * This processes an AssociationStatusNotification received from the
   * OSS Gateway. If the association status is failed or aborted, then
   * this retries to initialize the association. This also sends the
   * asynchronous NotificationReply required for this kind of notification.
   */
   public int receiveAssociationStatusNotification(
                                           String invokeID,
                                           String sessionID,
                                           String customerID,
                                           XMLMessageParser notification){

      if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

         Debug.log(Debug.IO_STATUS,
                   "Received association status notification for session ["+
                   sessionID+"]");

      }

      String replyStatus = NPACConstants.SUCCESS_STATUS;
      String errorCode = null;
      String errorInfo = null;

      // The assumption for Association Status Notifications will be that
      // these notifications are not in response to an AssociationRequest.
      // An AssociationReply would have been sent in response to
      // an AssociationRequest. SO, AssociationStatusNotifications
      // will generally indicate that some sort of problem has
      // occured with the association on the NPAC server side of things
      // and we will need to retry to connect the association.

      try{

         // get the region for this association
         String regionValue =
            notification.getTextValue(NPACConstants.NPAC_REGION_ID);
         
      // get the customer_id for this association
         String customerId =
            notification.getTextValue(NPACConstants.CUSTOMER_ID);

         int region = Integer.parseInt(regionValue);		

         // get association status from the notification
         String status =
            notification.getTextValue(NPACConstants.ASSOCIATION_STATUS);


         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
            Debug.log(Debug.MSG_STATUS,
                      "Association status notification:"+
                      "\n\tSession ID ["+sessionID+
                      "]\n\tRegion ["+region+
                      "]\n\tStatus ["+status+"]\n");
         }

         // check for any error info included in the notification
         if( notification.exists( NPACConstants.ASSOCIATION_ERROR_CODE ) ){

            String errorReason =
               notification.getTextValue(NPACConstants.ASSOCIATION_ERROR_CODE);

            if( notification.exists( NPACConstants.ASSOCIATION_ERROR_INFO ) ){

               errorReason += ":";
               errorReason += notification.getTextValue(
                                          NPACConstants.ASSOCIATION_ERROR_INFO);

               Debug.warning("Association status notification for session ["+
                              sessionID+"] and region ["+region+
                              "] included error:\n"+
                              errorReason );

            }

         }

         // if the status is "connected" (which it probably won't be),
         // then all is well, otherwise the association
         // was aborted or failed or released or had some
         // other nasty thing happen to it, and it
         // needs to be retried
         if(! status.equals(NPACConstants.CONNECTED_STATUS) ){

            // find the Session instance with the given session ID
            Session session = getSessionWithID(sessionID);

            if(session == null){

               Debug.error("Could not find session with ID ["+
                           sessionID+
                           "] for association status notification in region ["+
                           region+"] with status ["+status+"]");

               replyStatus = NPACConstants.SESSION_INVALID_STATUS;

            }
            else{
            	
            	 String connInstance="";
            	 String connectivityKey= null;
            	 
            	 if(customerId != null){
						connectivityKey = NPACQueueUtils.getConnectivityKey(customerId);
						if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							Debug
									.log(
											Debug.MSG_STATUS,
											"receiveAssociationStatusNotification Receive association status change notification for spid :["
													+ customerId
													+ "] and Connectivity : ["
													+ connectivityKey + "]");
						}
					}
            	 
	          	 if(connectivityKey != null && connectivityKey.length()>16){
	          		   connInstance = connectivityKey.substring(17) ;
	          		 if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							Debug.log(Debug.MSG_STATUS,
									"receiveAssociationStatusNotification Connectivity instance :["
											+ connInstance + "]");
						}
	          	   }	          	 
	          	 for (int i=0;i<=NPACConstants.NPACASSOCIATION_STATUS_ARR.length-1;i++)
		      	   {
		      		   if(connInstance.equals(""))
		      			   NPACConstants.NPACASSOCIATION_STATUS_ARR[0] = false;
		      		   else if((Integer.parseInt(connInstance))==i)
		      			   NPACConstants.NPACASSOCIATION_STATUS_ARR[i] = false;
		      	   }	          	 
	          	   
				 Connection conn =null;

				 PreparedStatement ps=null;
				 try
				 {
						conn = DBInterface.acquireConnection();

						ps=conn.prepareStatement(SOAQueryConstants.REGION_RECOVERY_UPDATE);

						ps.setString(1,SOAQueryConstants.NPACAWAITING_STATUS);

						ps.setInt(2,region);
						
						if(connectivityKey != null)
	 						ps.setString(3,connectivityKey);
	 					else
	 						ps.setNull(3, java.sql.Types.VARCHAR);
						
						// update SOA_REGION_RECOVERY table with awaiting status.
						
						if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
							Debug
									.log(
											Debug.MSG_STATUS,
											"Executing region recovery update query in NPACAdapter :[ "
													+ SOAQueryConstants.REGION_RECOVERY_UPDATE
													+ "]" + " for region ["
													+ region
													+ "] and connectivity :["
													+ connectivityKey + "]");
						}
						ps.executeUpdate();

						conn.commit();
				 }
				 catch(Exception ex)
				 {
					try
					{

						conn.rollback();
					}
					catch(Exception e)
					{
						Debug.log(Debug.SYSTEM_CONFIG,e.toString());
					}

					Debug.log(Debug.SYSTEM_CONFIG,ex.toString());
				 }
				 finally
				 {
					 try
					 { 
						 if(ps!= null)
						 {
							 ps.close();							 
						 }
						 if( conn!= null)
						 {
							 DBInterface.releaseConnection( conn );
						 }
					 }
					 catch(Exception dbEx)
					 {
						Debug.log(Debug.SYSTEM_CONFIG,dbEx.toString());
					 } 
				 }

               // create a retry request that will attempt to reconnect the
               // association
               retryAssociationRequestImmediately(session, region);			   

            }

         }

      }
      catch(Exception ex){

         replyStatus = NPACConstants.FAILED_STATUS;
         errorCode = NPACConstants.PROCESSING_FAILURE_ERROR_CODE;
         errorInfo = "Could not get required values from association status "+
                     "notification: "+ex.getMessage();

         Debug.error(errorInfo);

      }

      // send the required NotificationReply
      client.sendNotificationReply(sessionID,
                                   invokeID,
                                   customerID,
                                   replyStatus,
                                   errorCode,
                                   errorInfo);

      return  replyStatus.equals( NPACConstants.SUCCESS_STATUS ) ?
                 NPACConstants.ACK_RESPONSE : NPACConstants.NACK_RESPONSE;

   }

   /**
   * This sends a NewSession request for the given session. If the
   * session is already alive, then the request will not be sent.
   */
   public void sendNewSessionRequest(Session session){

      // Do not sent the NewSession request if the session is already
      // alive. reinitialize() should be used if you want to shutdown the
      // session and reconnect. This check should also keep us
      // from performing NewSession retry after retry even when the
      // session is connected.
      if( session.isAlive() ){

          if( Debug.isLevelEnabled(Debug.IO_STATUS) ){
             Debug.log(Debug.IO_STATUS,
                    "Session:\n"+session+"is already connected. "+
                    "A new session request will not be sent for this session.");
          }

          return;

      }

      if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

          Debug.log(Debug.IO_STATUS,
                    "Sending new session request for primary SPID ["+
                    session.getPrimarySPID()+"]");

      }

      int ack = NPACConstants.NACK_RESPONSE;

      String invokeID = null;

      NewSessionReplyHandler replyHandler = null;

      try{
    	  

         invokeID = getNextInvokeID();
         // Create and add handler to wait for the asynchronous response.
         //
         // We do this in advance to prevent the case
         // where the asynchonous notification response
         // arrives BEFORE the synchronous request/reply completes.
         // I just saw this happen. So we will add the reply handler
         // now, in case the notification comes in too quickly.
         //
         replyHandler = new NewSessionReplyHandler(this, session);
         replyHandlers.add(invokeID, replyHandler, resendTimeout);

         ack = client.sendNewSession(session.getPrimarySPID(),
                                     invokeID,
                                     userID);
                

      }
      catch(DatabaseException ex){

         Debug.error("Could not get next invoke ID: "+ex);

      }

      if( ack != NPACConstants.ACK_RESPONSE ){

         // the request was NACKed, remove the request handler that is
         // waiting for the response (that will never come)
         if( replyHandler != null ){

            replyHandlers.cancel(invokeID);

         }
         
         // create a retry task to resend the request later
         retryNewSessionRequest(session);

      }

   }

   /**
   * This method is used during initialization of a session
   * to send the next necessary AssociationRequest. If there are
   * customer (secondary) SPIDs defined for the given region, and if
   * the association is currently DOWN for this region, then an
   * AssociationRequest is sent to connect the Association in this region.
   * If the region has no SPIDs defined, the Association will get flagged
   * as not needed, and we skip to the next region.
   * If the Association is already connected, or if it is waiting to retry,
   * then the Association is also skipped.
   *
   * @param session the session for whom we are sending association
   *                requests.
   * @param region the next region to send a "connect" request for.
   *               If this region exceeds the maximum number of regions,
   *               then this method does nothing, because all association
   *               requests for this session have been sent already.
   */
   public void sendNextAssociationRequest(Session session,
                                          int region){
	   Debug.log(Debug.SYSTEM_CONFIG,
               "Session ["+
               session.getSessionID()+"] has ["+
               region+"]");


      // check to see if region is valid
      if(region >= 0 && region < NPACConstants.REGION_COUNT){

         List supportedSPIDs = session.getSPIDsForRegion(region);

         if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

             Debug.log(Debug.MSG_STATUS,
                       "Session ["+
                       session.getSessionID()+"] has ["+
                       supportedSPIDs.size()+"] SPID(s) defined for region ["+
                       region+"]");

         }

         if(supportedSPIDs.size() == 0){

            // flag the association as not required, since
            // no customers use that region
        	 Debug.log(Debug.SYSTEM_CONFIG,"Inside the supportedSPIDs size equal to zero mehtod");
            session.setAssociationNotRequired(region);

         }

         // Check the association's state to determine if it needs to
         // be connected.
         if( session.isAssociationDown( region ) ){
        	 
            // there are customers defined for this region, and it
            // is down so try to create an Association for this region.
            // The "retry" call here performs the request in a background worker
            // thread. This allows the regions to associate in parallel.
            retryAssociationRequestImmediately(session, region);

         }

      }

   }

   /**
   * This sends an AssociationRequest with a status of "connect" for
   * the given session in the given region. A reply handler is then
   * created to wait for the response message.
   *
   * @param session the session for whom the association request will be sent.
   * @param region the region for which the Association will be connected.
   */
   public int sendAssociationConnectRequest(Session session,
                                            int region){

      if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

          Debug.log(Debug.IO_STATUS,
                    "Sending connect association request for session ["+
                    session.getSessionID()+"] and region ["+region+"]");

      }

      int ack = NPACConstants.NACK_RESPONSE;

      String invokeID = null;
      AssociationReplyHandler replyHandler = null;

      try{

         invokeID = getNextInvokeID();
                 // create and add handler to wait for the asynchronous response
         replyHandler = new AssociationReplyHandler(this, session, region);
         replyHandlers.add(invokeID, replyHandler, resendTimeout);

         // flag the association as waiting for a response
         session.setAssociationWaitingForReply(region);

         ack = client.sendAssociationConnectRequest(session.getSessionID(),
                                                    invokeID,
                                                    region,
                                                    session.getPrimarySPID());

      }
      catch(DatabaseException ex){

         Debug.error("Could not get next invoke ID: "+ex);

      }

      if( ack != NPACConstants.ACK_RESPONSE ){

         // if we got a NACK response, then remove the reply handler
         if(replyHandler != null){
            replyHandlers.cancel(invokeID);
         }

         // create retry task to resend the request later
         retryAssociationRequest(session, region);

      }

      return ack;

   }

   /**
    * This creates a retry task that will resend a failed AssociationRequest
    * and queues it for execution without any sleep interval. This is
    * used when the NPAC has notified us that an association has gone
    * down, and we want to retry the association immediately.
    *
    * @param session Session the session involved.
    * @param region int the association's region.
    */
   public void retryAssociationRequestImmediately(Session session,
                                                  int region){

      retryAssociationRequest(session, region, 0);

   }

   /**
   * This creates a retry task that will resend a failed AssociationRequest
   * and adds it to the worker queue to be executed later in
   * a worker thread.
   *
   */
   public void retryAssociationRequest(Session session,
                                       int region){

      retryAssociationRequest(session, region, retryInterval);

   }

   /**
   * This creates a retry task that will resend a failed AssociationRequest
   * and adds it to the worker queue to be executed after the given interval
   * by a worker thread.
   *
   * @param interval the ammount of time, in ms, that the retry attempt
   *                 will sleep before performing the retry.
   */
   private void retryAssociationRequest(Session session,
                                        int region,
                                        long interval){

      // flag the association as waiting for a retry
      session.setAssociationRetry(region);

      AssociationRequestRetry retry =
                      new AssociationRequestRetry(this,
                                                  session,
                                                  region,
                                                  interval);
      enqueue(retry);

   }

   /**
   * This creates a retry task that will resend a failed NewSessionRequest
   * and adds it to the worker queue to be executed later in
   * a worker thread.
   */
   public void retryNewSessionRequest(Session session){

      NewSessionRetry retry = new NewSessionRetry(this,
                                                  session,
                                                  retryInterval);

      enqueue( retry );

   }

   /**
    * This creates a retry task that will resend a failed
    * DownloadRecoveryRequest
    * and adds it to the worker queue to be executed later in
    * a worker thread.
    *
    * @param session Session
    * @param region int
    * @param recoveryCompleteTime Date
    * @param lastNotificationTime Date
    * @param recoveryInterval long
    */
   public void retryDownloadRecoveryRequest(Session session,
                                            int region,
                                            List spids,
                                            int spidIndex,
                                            Date recoveryCompleteTime,
                                            Date lastNotificationTime,
                                            long recoveryInterval,
                                            String downloadRecoveryReplyType,
                                            boolean isTimRangeRequest){

      DownloadRecoveryRequestRetry retry =
         new DownloadRecoveryRequestRetry(retryInterval,
                                          this,
                                          session,
                                          region,
                                          spids,
                                          spidIndex,
                                          recoveryCompleteTime,
                                          lastNotificationTime,
                                          recoveryInterval ,
                                          downloadRecoveryReplyType,
                                          isTimRangeRequest);

      enqueue(retry);

   }

   /**
    * This creates a retry task that will resend a failed
    * DownloadRecoveryRequest
    * and adds it to the worker queue to be executed later in
    * a worker thread.
    *
    * @param session Session
    * @param region int
    * @param recoveryCompleteTime Date
    * @param lastNotificationTime Date
    * @param recoveryInterval long
    */
   public void retryTimeBasedDownloadRecoveryRequest(Session session,
                                            int region,
                                            List spids,
                                            int spidIndex,
                                            Date recoveryCompleteTime,
                                            Date lastNotificationTime,
                                            long recoveryInterval){

      TimeBasedDownloadRecoveryRequestRetry retry =
         new TimeBasedDownloadRecoveryRequestRetry(retryInterval,
                                          this,
                                          session,
                                          region,
                                          spids,
                                          spidIndex,
                                          recoveryCompleteTime,
                                          lastNotificationTime,
                                          recoveryInterval );

      enqueue(retry);

   }

   /**
    * Sends out the initial download recovery request for the given region.
    *
    * @param session Session the related Session.
    * @param region int the region in which recovery is being done.
    * @param recoveryCompleteTime Date
    */
   public void sendTimeBasedFirstDownloadRecoveryRequest(Session session,
                                                int region,
                                                Date recoveryCompleteTime){

      List spids = session.getSPIDsForRegion( region );

      sendTimeBasedDownloadRecoveryRequest(session,
                                  region,
                                  spids,
                                  0,
                                  recoveryCompleteTime,
                                  null);

   }

   /**
   * This sends a DownloadRecoveryRequest that queries for any notifications
   * that may have been missed while the SOA gateway was down.
   * Since recovery requests can only be made for at most one hour
   * windows at a time, this method adds the default query interval
   * (usually an hour)
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then the first RecoveryRequest is sent for this region.
   *
   * @param session the session which is recovering.
   * @param region the region against which we are querying. An Association
   *               must be connected for this region and the Assoication
   *               must be in recovery mode.
   * @param spids the list of customer SPIDs for which recovery needs
   *              to be done for this region.
   * @param spidIndex This index relative to the list of spids that indicates
   *                  which SPID we are currently recovering for.
   * @param recoveryCompleteTime recovery must be done up to this time.
   *                             Once the recovery window reaches this
   *                             time, then recovery is complete for the
   *                             current SPID in this region.
   * @param lastNotificationTime This is the last notification time for
   *                             the current SPID in the current region.
   *                             If this value is null, this indicates that
   *                             this is the first recovery request for
   *                             this SPID in this region, and the last
   *                             notification time will be retrieved from the
   *                             database. If the last notification time
   *                             has a value, then this indicates the
   *                             time up to which the last recovery
   *                             request was made.
   */
   public void sendTimeBasedDownloadRecoveryRequest(Session session,
                                           int region,
                                           List spids,
                                           int spidIndex,
                                           Date recoveryCompleteTime,
                                           Date lastNotificationTime ){

      sendTimeBasedDownloadRecoveryRequest( session,
                                   region,
                                   spids,
                                   spidIndex,
                                   recoveryCompleteTime,
                                   lastNotificationTime,
                                   recoveryWindow );


   }

   /**
   * This sends a DownloadRecoveryRequest that queries for any notifications
   * that may have been missed while the SOA gateway was down.
   * This method adds the given query interval
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then the first RecoveryRequest is sent for this region.
   *
   * @param session the session which is recovering.
   * @param region the region against which we are querying. An Association
   *               must be connected for this region and the Assoication
   *               must be in recovery mode.
   * @param spids the list of customer SPIDs for which recovery needs
   *              to be done for this region.
   * @param spidIndex This index relative to the list of spids that indicates
   *                  which SPID we are currently recovering for.
   * @param recoveryCompleteTime recovery must be done up to this time.
   *                             Once the recovery window reaches this
   *                             time, then recovery is complete for the
   *                             current SPID in this region.
   * @param lastNotificationTime This is the last notification time for
   *                             the current SPID in the current region.
   *                             If this value is null, this indicates that
   *                             this is the first recovery request for
   *                             this SPID in this region, and the last
   *                             notification time will be retrieved from the
   *                             database. If the last notification time
   *                             has a value, then this indicates the
   *                             time up to which the last recovery
   *                             request was made.
   * @param interval the recovery interval to use in the recovery request.
   */
   public void sendTimeBasedDownloadRecoveryRequest(Session session,
                                           int region,
                                           List spids,
                                           int spidIndex,
                                           Date recoveryCompleteTime,
                                           Date lastNotificationTime,
                                           long interval){

      int spidCount = spids.size();

      if( spidCount == 0 || spidIndex >= spidCount ){

         // We have run out of supported SPIDs for this region/association.
         // Send the first recovery request.
         sendTimeBasedFirstRecoveryRequest(session, region, recoveryCompleteTime);

         return;

      }

      String currentSPID = spids.get(spidIndex).toString();

      if(lastNotificationTime == null){

         // This is the first download recovery request.
         // We need to first retrieve the last network
         // notification time from the DB.
         lastNotificationTime = getLastNetworkNotificationTime(currentSPID,
                                                               region);

         if( lastNotificationTime != null &&
             recoveryPadding > 0 ){

            if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){
               Debug.log(Debug.MSG_STATUS,
                         "Padding last notification time by ["+
                         recoveryPadding+"] ms");
            }

            // subtract recovery padding interval
            lastNotificationTime.setTime(lastNotificationTime.getTime()
                                         - recoveryPadding);

         }

      }

      if(lastNotificationTime == null){

         Debug.error("No last network notification time was found SPID ["+
                     currentSPID+"] in region ["+
                     region+
                     "]. TimeBased Download Recovery will be skipped for this region.");

         // skip ahead and start sending recovery requests
         sendTimeBasedFirstRecoveryRequest( session, region, recoveryCompleteTime );

      }
      else{

         // Add one second to the time so that it is not *exactly*
         // when the last notification was sent
         // (which would include the last notification) or exactly
         // when the last download recovery query ended (which would
         // be a double-query for that last second).
         lastNotificationTime.setTime( lastNotificationTime.getTime() + 1000 );

         // See if last notification time is equal to or later than
         // the completion time (the time that the association
         // was initialized). The division by one thousand gets the units
         // into seconds.
         if(lastNotificationTime.getTime()/1000 >=
            recoveryCompleteTime.getTime()/1000){

            // move on to sending the the recovery requests
            sendTimeBasedFirstRecoveryRequest(session,
                                     region,
                                     recoveryCompleteTime);

         }
         else{

            // Add the retry window to the last notification time to get
            // the stop time.
            long stopTimeMs = lastNotificationTime.getTime() +
                              interval;

            // If the stop time is greater than the
            // completion time, then use the completion time as the
            // stop time.
            long completeMs = recoveryCompleteTime.getTime();

            if(stopTimeMs/1000 >= completeMs/1000){

               stopTimeMs = completeMs;

            }

            Date stopTime = new Date(stopTimeMs);

            try{

               String invokeID = getNextInvokeID();

               if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

                  Debug.log(Debug.IO_STATUS,
                            "Sending TimeBased download recovery request:\n\tSession ID ["+
                            session.getSessionID()+
                            "]\n\tRegion ["+region+
                            "]\n\tStart Time ["+
                            lastNotificationTime.toGMTString()+
                            "]\n\tStop Time ["+
                            stopTime.toGMTString()+"]");

               }			  

               // create a response handler to wait for the recovery reply
               TimeBasedDownloadRecoveryReplyHandler replyHandler =
                     new TimeBasedDownloadRecoveryReplyHandler(this,
                                                      session,
                                                      region,
                                                      spids,
                                                      spidIndex,
                                                      recoveryCompleteTime,
                                                      stopTime,
													  lastNotificationTime,
                                                      lastNotificationTimeAccess,
                                                      interval);

               replyHandlers.add(invokeID, replyHandler, resendTimeout);

               int ack =
                  client.sendTimeBasedDownloadRecoveryRequest( session.getSessionID(),
                                                      invokeID,
                                                      region,
                                                      currentSPID,
                                                      lastNotificationTime,
                                                      stopTime);

               if(ack != NPACConstants.ACK_RESPONSE){

                  // don't need to listen for a response that isn't coming
                  replyHandlers.cancel(invokeID);

                  // try to reinitialize the association
                  retryAssociationRequest(session, region);

               }

            }
            catch(DatabaseException dbex){

               Debug.error("Could not get invoke ID for TimeBased recovery request: "+
                           dbex);

            }

         }

      }

   }

   /**
    * Sends out the initial recovery request for the given region.
    *
    * @param session Session
    * @param region int
    * @param recoveryCompleteTime Date
    */
   public void sendTimeBasedFirstRecoveryRequest(Session session,
                                        int region,
                                        Date recoveryCompleteTime){

      List spids = session.getSPIDsForRegion( region );

      sendTimeBasedRecoveryRequest(session,
                          region,
                          spids,
                          0,
                          recoveryCompleteTime,
                          null);

   }

   /**
   * This sends a RecoveryRequest that queries for any notifications
   * that may have been missed while the SOA gateway was down.
   * Since recovery requests can only be made for at most one hour
   * windows at a time, this method adds the default query interval
   * (usually an hour)
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then a RecoveryCompleteRequest is sent for this region.
   *
   * @param session the session which is recovering.
   * @param region the region against which we are querying. An Association
   *               must be connected for this region and the Assoication
   *               must be in recovery mode.
   * @param spids the list of customer SPIDs for which recovery needs
   *              to be done for this region.
   * @param spidIndex This index relative to the list of spids that indicates
   *                  which SPID we are currently recovering for.
   * @param recoveryCompleteTime recovery must be done up to this time.
   *                             Once the recovery window reaches this
   *                             time, then recovery is complete for the
   *                             current SPID in this region.
   * @param lastNotificationTime This is the last notification time for
   *                             the current SPID in the current region.
   *                             If this value is null, this indicates that
   *                             this is the first recovery request for
   *                             this SPID in this region, and the last
   *                             notification time will be retrieved from the
   *                             database. If the last notification time
   *                             has a value, then this indicates the
   *                             time up to which the last recovery
   *                             request was made.
   */
   public void sendTimeBasedRecoveryRequest(Session session,
                                   int region,
                                   List spids,
                                   int spidIndex,
                                   Date recoveryCompleteTime,
                                   Date lastNotificationTime){

      sendTimeBasedRecoveryRequest(session,
                          region,
                          spids,
                          spidIndex,
                          recoveryCompleteTime,
                          lastNotificationTime,
                          recoveryWindow);

   }

   /**
   * This sends a RecoveryRequest that queries for any notifications
   * that may have been missed while the SOA gateway was down.
   * Since recovery requests can only be made for at most one hour
   * windows at a time, this method adds the query interval (usually an hour)
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then a RecoveryCompleteRequest is sent for this region.
   *
   * @param session the session which is recovering.
   * @param region the region against which we are querying. An Association
   *               must be connected for this region and the Assoication
   *               must be in recovery mode.
   * @param spids the list of customer SPIDs for which recovery needs
   *              to be done for this region.
   * @param spidIndex This index relative to the list of spids that indicates
   *                  which SPID we are currently recovering for.
   * @param recoveryCompleteTime recovery must be done up to this time.
   *                             Once the recovery window reaches this
   *                             time, then recovery is complete for the
   *                             current SPID in this region.
   * @param lastNotificationTime This is the last notification time for
   *                             the current SPID in the current region.
   *                             If this value is null, this indicates that
   *                             this is the first recovery request for
   *                             this SPID in this region, and the last
   *                             notification time will be retrieved from the
   *                             database. If the last notification time
   *                             has a value, then this indicates the
   *                             time up to which the last recovery
   *                             request was made.
   * @param recoveryInterval this is specific the interval of time (in ms)
   *                         that should be queried for. This parameter
   *                         allows us to query for smaller and smaller
   *                         intervals of time. This can be necessary when
   *                         the previous query interval resulted in a
   *                         "criteria-too-large" recovery reply.
   *
   */
   public void sendTimeBasedRecoveryRequest(Session session,
                                   int region,
                                   List spids,
                                   int spidIndex,
                                   Date recoveryCompleteTime,
                                   Date lastNotificationTime,
                                   long recoveryInterval){

      int spidCount = spids.size();

      if( spidCount == 0 || spidIndex >= spidCount ){

         // We have run out of supported SPIDs for this region/association.
         // Send a recovery complete request for this association.
         sendRecoveryCompleteRequest(session, region);

         return;

      }

      String currentSPID = spids.get(spidIndex).toString();

      if(lastNotificationTime == null){

         // This must be the first recovery request for this SPID.
         // We need to first retrieve its last notification time from the DB.
         lastNotificationTime = getLastNotificationTime(currentSPID,
                                                        region);

         if( lastNotificationTime != null &&
             recoveryPadding > 0 ){

            if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
               Debug.log(Debug.MSG_STATUS,
                         "Padding last notification time by [" +
                         recoveryPadding + "] ms");
            }

            // subtract recovery padding interval
            lastNotificationTime.setTime(lastNotificationTime.getTime()
                                         - recoveryPadding);

         }

      }

      if(lastNotificationTime == null){

         Debug.error("No last notification time was found for "+
                     "customer SPID ["+currentSPID+"] in region ["+
                     region+
                     "]. TimeBased Recovery for this SPID will be skipped in this "+
                     "region.");

         // skip to the next SPID
         sendTimeBasedRecoveryRequest(session,
                             region,
                             spids,
                             ++spidIndex,
                             recoveryCompleteTime,
                             null);

      }
      else{

         // Add one second to the time so that it is not *exactly*
         // when the last notification was sent
         // (which would include the last notification) or exactly
         // when the last recovery query ended (which would
         // be a double-query for that last second).
         lastNotificationTime.setTime( lastNotificationTime.getTime() + 1000 );

         // See if last notification time is equal to or later than
         // the completion time (the time that the association
         // was initialized). The division by one thousand gets the units
         // into seconds.
         if(lastNotificationTime.getTime()/1000 >=
            recoveryCompleteTime.getTime()/1000){

            // move on to the next SPID for this region
            sendTimeBasedRecoveryRequest(session,
                                region,
                                spids,
                                ++spidIndex,
                                recoveryCompleteTime,
                                null);

         }
         else{

            // Add the retry window to the last notification time to get
            // the stop time.
            long stopTimeMs = lastNotificationTime.getTime() +
                              recoveryInterval;

            // If the stop time is greater than the
            // completion time, then use the completion time as the
            // stop time.
            long completeMs = recoveryCompleteTime.getTime();

            if(stopTimeMs/1000 >= completeMs/1000){

               stopTimeMs = completeMs;

            }

            Date stopTime = new Date(stopTimeMs);

            try{

               String invokeID = getNextInvokeID();

               if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

                  Debug.log(Debug.IO_STATUS,
                            "Sending TimeBased recovery request:\n\tSession ID ["+
                            session.getSessionID()+
                            "]\n\tRegion ["+region+
                            "]\n\tStart Time ["+
                            lastNotificationTime.toGMTString()+
                            "]\n\tStop Time ["+
                            stopTime.toGMTString()+"]");

               }			   

               // create a response handler to wait for the recovery reply
               TimeBasedRecoveryReplyHandler replyHandler =
                     new TimeBasedRecoveryReplyHandler(this,
                                              session,
                                              region,
                                              spids,
                                              spidIndex,
                                              recoveryCompleteTime,
                                              stopTime,
											  lastNotificationTime,
                                              lastNotificationTimeAccess,
                                              recoveryInterval);

               replyHandlers.add(invokeID, replyHandler, resendTimeout);

               int ack = client.sendTimeBasedRecoveryRequest(session.getSessionID(),
                                                    invokeID,
                                                    region,
                                                    currentSPID,
                                                    lastNotificationTime,
                                                    stopTime);

               if(ack != NPACConstants.ACK_RESPONSE){

                  // don't need to listen for a response that isn't coming
                  replyHandlers.cancel(invokeID);

                  // try to reinitialize the association
                  retryAssociationRequest(session, region);

               }

            }
            catch(DatabaseException dbex){

               Debug.error("Could not get invoke ID for TimeBased recovery request: "+
                           dbex);

            }

         }

      }

   }

   /**
    * Sends out the initial download recovery request for the given region.
    *
    * @param session Session the related Session.
    * @param region int the region in which recovery is being done.
    * @param recoveryCompleteTime Date
    */
   public void sendFirstDownloadRecoveryRequest(Session session,
                                                int region,
                                                Date recoveryCompleteTime){
	   
	 

      List spids = session.getSPIDsForRegion( region );
                      
      sendDownloadRecoveryRequest(session,
                                  region,
                                  spids,
                                  0,
                                  recoveryCompleteTime,
                                  null);

   }

   /**
   * This sends a DownloadRecoveryRequest that queries for any notifications
   * that may have been missed while the SOA gateway was down.
   * Since recovery requests can only be made for at most one hour
   * windows at a time, this method adds the default query interval
   * (usually an hour)
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then the first RecoveryRequest is sent for this region.
   *
   * @param session the session which is recovering.
   * @param region the region against which we are querying. An Association
   *               must be connected for this region and the Assoication
   *               must be in recovery mode.
   * @param spids the list of customer SPIDs for which recovery needs
   *              to be done for this region.
   * @param spidIndex This index relative to the list of spids that indicates
   *                  which SPID we are currently recovering for.
   * @param recoveryCompleteTime recovery must be done up to this time.
   *                             Once the recovery window reaches this
   *                             time, then recovery is complete for the
   *                             current SPID in this region.
   * @param lastNotificationTime This is the last notification time for
   *                             the current SPID in the current region.
   *                             If this value is null, this indicates that
   *                             this is the first recovery request for
   *                             this SPID in this region, and the last
   *                             notification time will be retrieved from the
   *                             database. If the last notification time
   *                             has a value, then this indicates the
   *                             time up to which the last recovery
   *                             request was made.
   */
   public void sendDownloadRecoveryRequest(Session session,
                                           int region,
                                           List spids,
                                           int spidIndex,
                                           Date recoveryCompleteTime,
                                           Date lastNotificationTime ){
	  
      sendDownloadRecoveryRequest( session,
                                   region,
                                   spids,
                                   spidIndex,
                                   recoveryCompleteTime,
                                   lastNotificationTime,
                                   recoveryWindow );


   }

   /**
   * This sends a DownloadRecoveryRequest that queries for any notifications
   * that may have been missed while the SOA gateway was down.
   * This method adds the given query interval
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then the first RecoveryRequest is sent for this region.
   *
   * @param session the session which is recovering.
   * @param region the region against which we are querying. An Association
   *               must be connected for this region and the Assoication
   *               must be in recovery mode.
   * @param spids the list of customer SPIDs for which recovery needs
   *              to be done for this region.
   * @param spidIndex This index relative to the list of spids that indicates
   *                  which SPID we are currently recovering for.
   * @param recoveryCompleteTime recovery must be done up to this time.
   *                             Once the recovery window reaches this
   *                             time, then recovery is complete for the
   *                             current SPID in this region.
   * @param lastNotificationTime This is the last notification time for
   *                             the current SPID in the current region.
   *                             If this value is null, this indicates that
   *                             this is the first recovery request for
   *                             this SPID in this region, and the last
   *                             notification time will be retrieved from the
   *                             database. If the last notification time
   *                             has a value, then this indicates the
   *                             time up to which the last recovery
   *                             request was made.
   * @param interval the recovery interval to use in the recovery request.
   */
   public void sendDownloadRecoveryRequest(Session session,
                                           int region,
                                           List spids,
                                           int spidIndex,
                                           Date recoveryCompleteTime,
                                           Date lastNotificationTime,
                                           long interval)
   {
       
       boolean isTimeRangeRequest = false;
       String currentSpid = spids.get(spidIndex).toString();
       lastNotificationTime = getLastNetworkNotificationTime(currentSpid , region);
       sendDownloadRecoveryRequestServiceProvidersData(session,
                                                       region,
                                                       spids, 
                                                       spidIndex,
                                                       null, 
                                                       recoveryCompleteTime,
                                                       lastNotificationTime,
                                                       interval,
                                                       isTimeRangeRequest);
       
          
   }
   
   /**
   * This sends a SWIM DownloadRecoveryRequest for service provider data
   * that queries for any notifications that may have been missed 
   * while the SOA gateway was down.
   * This method adds the given query interval
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then the first RecoveryRequest is sent for this region.
    * @param session the session which is recovering.
    * @param region the region against which we are querying. An Association
    *               must be connected for this region and the Assoication
    *               must be in recovery mode.
    * @param spids the list of customer SPIDs for which recovery needs
    *              to be done for this region.
    * @param spidIndex his index relative to the list of spids that indicates
    *                  which SPID we are currently recovering for.
    * @param swimActionID actionId will populated when we receive 
    *                      swim_more_data in the Download Recovery Reply. 
    * @param recoveryCompleteTime recovery must be done up to this time.
    *                             Once the recovery window reaches this
    *                             time, then recovery is complete for the
    *                             current SPID in this region.
    * @param lastNotificationTime This is the last notification time for
    *                             the current SPID in the current region.
    *                             If this value is null, this indicates that
    *                             this is the first recovery request for
    *                             this SPID in this region, and the last
    *                             notification time will be retrieved from the
    *                             database. If the last notification time
    *                             has a value, then this indicates the
    *                             time up to which the last recovery
    *                             request was made.
    * @param interval the recovery interval to use in the recovery request.
    * @param isTimeRangeRequest if the method is called from DRRHandler 
    *                           the attribute populated is "true" based on this 
    *                           we will generate time range recovery requests.
    */
   
    public void sendDownloadRecoveryRequestServiceProvidersData
                                              (Session session,
                                               int region,
                                               List spids,
                                               int spidIndex,
                                               String swimActionID,
                                               Date recoveryCompleteTime,
                                               Date lastNotificationTime,
                                               long interval,
                                               boolean isTimeRangeRequest)
   {
       
       int spidCount = spids.size();
      
       
      
       
       if (spidCount == 0 )
       {
           sendFirstRecoveryRequest(session, region, recoveryCompleteTime);  
           return;

       }
       if(spidIndex >= spidCount)
       {
          
          sendFirstDownloadNetworkDataRecoveryRequest(session,
                                                      region,
                                                      spids,
                                                      0,
                                                      recoveryCompleteTime,
                                                      null,
                                                      interval);
           return;
       }
              
       String requestType  =NPACConstants.DOWNLOAD_DATA_SERVICE_PROV_DATA;
       String currentSPID = spids.get(spidIndex).toString();
       if(lastNotificationTime == null){

           // This is the first download recovery request.
           // We need to first retrieve the last 
           // notification time from the DB.
           
           lastNotificationTime = getLastServiceProviderNotificationTime
                                                       (currentSPID,region);
          
                      // subtract recovery padding interval
           if(lastNotificationTime!=null){
               lastNotificationTime.setTime(lastNotificationTime.getTime()
                       - recoveryPadding);
               
           }
           
        }
            
    
       
       if(lastNotificationTime == null ){
           
           isTimeRangeRequest=false;

           Debug.error("No last notification time was found for "+
                       "customer SPID ["+currentSPID+"] in region ["+
                       region+
                       "]. Recovery for this SPID will be skipped in this "+
                       "region.");

           // skip to the next SPID
           sendDownloadRecoveryRequestServiceProvidersData
                                                 ( session,
                                                   region,
                                                   spids,
                                                   ++spidIndex,
                                                   null,
                                                   recoveryCompleteTime,
                                                   lastNotificationTime,
                                                   interval,
                                                   isTimeRangeRequest);

        }else
        {
           
        	// This will only execute when SOA is under normal recovery mode. 
        	       	 
        	//	lastNotificationTime.setTime(lastNotificationTime.getTime() + 1000);
        	
           
           //See if last notification time is equal to or later than
           // the completion time (the time that the association
           // was initialized). The division by one thousand gets the units
           // into seconds.
           if(swimActionID == null &&lastNotificationTime.getTime()/1000 >=
              recoveryCompleteTime.getTime()/1000){
               
               isTimeRangeRequest=false;

              // move on to the next SPID for this region
               sendDownloadRecoveryRequestServiceProvidersData
                                                      (session,
                                                       region,
                                                       spids, 
                                                       ++spidIndex,
                                                       null, 
                                                       recoveryCompleteTime,
                                                       null,
                                                       interval,
                                                       isTimeRangeRequest);
           }else
           {
               //Add the retry window to the last notification time to get
               // the stop time.
               long stopTimeMs = lastNotificationTime.getTime()+interval;
               long completeTimeMs = recoveryCompleteTime.getTime();
               
               //If the stop time is greater than the
               // completion time, then use the completion time as the
               // stop time.
               
               if(stopTimeMs/1000 >= completeTimeMs/1000){

                   stopTimeMs = completeTimeMs;

                }
               
               Date stopTime = new Date(stopTimeMs);
            
           try{    
               
               String invokeID = getNextInvokeID();
                  
           DownloadRecoveryReplyHandler replyHandler =
               new DownloadRecoveryReplyHandler(this,
                                                session,
                                                region,
                                                spids,
                                                spidIndex,
                                                recoveryCompleteTime,
                                                stopTime,
                                                lastNotificationTimeAccess,
                                                interval,
                                                requestType,
                                                isTimeRangeRequest);
           
           
           replyHandlers.add(invokeID, replyHandler, resendTimeout);

              int ack =
              client.sendDownloadRecoveryServiceProvRequest
                                                ( session.getSessionID(),
                                                  invokeID,
                                                  region,
                                                  currentSPID,
                                                  lastNotificationTime,
                                                  stopTime,
                                                  swimActionID,
                                                  isTimeRangeRequest );
           if(ack != NPACConstants.ACK_RESPONSE){
                   // don't need to listen for a response that isn't coming
              replyHandlers.cancel(invokeID);

              // try to reinitialize the association
                    retryAssociationRequest(session, region);

           }

           
       }catch(DatabaseException dbex){

           Debug.error("Could not get invoke ID for recovery request: "+
                       dbex);
       }
        }
        }
      
       
   }
   
   /**
    * calls sendDownloadRecoveryRequestNetWorkData method 
    * @param session session involeved in the request.
    * @param region region associated with request
    * @param spids spids assoicated with the region
    * @param spidIndex spid involved in the region
    * @param recoveryCompleteTime recovery must be done up to this time.
    *                             Once the recovery window reaches this
    *                             time, then recovery is complete for the
    *                             current SPID in this region.
    * @param lastNotificationTime intailly it will be null
    * @param interval recovery interval
    */
    
    
   public void sendFirstDownloadNetworkDataRecoveryRequest
                                              (Session session,
                                               int region,
                                               List spids,
                                               int spidIndex,
                                               Date recoveryCompleteTime,
                                               Date lastNotificationTime,
                                               long interval)
   {
       
       
       boolean isTimeRangeRequest = false;
       
       String currentSpid = spids.get(spidIndex).toString();
       lastNotificationTime = getLastNetworkNotificationTime(currentSpid , region);
       
              
                   
           sendDownloadRecoveryRequestNetWorkData( session,
                                                   region,
                                                   spids,
                                                   spidIndex,
                                                   null, 
                                                   recoveryCompleteTime,
                                                   lastNotificationTime,
                                                   interval,
                                                   isTimeRangeRequest);
          
       
       
   }
  
    /**
    * This sends a SWIM DownloadRecoveryRequest for net work data
    * that queries for any notifications that may have been missed 
    * while the SOA gateway was down.
    * This method adds the given query interval
    * to the last notification time, and performs the recovery request.
    * A recovery reply handler is created to handle the response. When
    * the reply is received, the reply handler
    * adjusts the last notification time accordingly, and then calls this
    * method again with the new time. This process repeats until the
    * last notification time meets or exceeds the recoveryCompleteTime
    * at which point we move on to recovery for the next SPID in the spids
    * list (if there is a next SPID). When the list of SPIDs is exhausted,
    * then the first RecoveryRequest is sent for this region.
     * @param session the session which is recovering.
     * @param region the region against which we are querying. An Association
     *               must be connected for this region and the Assoication
     *               must be in recovery mode.
     * @param spids the list of customer SPIDs for which recovery needs
     *              to be done for this region.
     * @param spidIndex his index relative to the list of spids that indicates
     *                  which SPID we are currently recovering for.
     * @param swimActionID actionId will populated when we receive 
     *                     swim_more_data in the Download Recovery Reply. 
     * @param recoveryCompleteTime recovery must be done up to this time.
     *                             Once the recovery window reaches this
     *                             time, then recovery is complete for the
     *                             current SPID in this region.
     * @param lastNotificationTime This is the last notification time for
     *                             the current SPID in the current region.
     *                             If this value is null, this indicates that
     *                             this is the first recovery request for
     *                             this SPID in this region, and the last
     *                             notification time will be retrieved from the
     *                             database. If the last notification time
     *                             has a value, then this indicates the
     *                             time up to which the last recovery
     *                             request was made.
     * @param interval the recovery interval to use in the Down load recovery 
     *                 request.
     * @param isTimeRangeRequest if the method is called from DRRHandler 
     *                           the attribute populated is "true" based on this 
     *                           we will generate time range recovery requests.
     */
   
   
   public void sendDownloadRecoveryRequestNetWorkData
                                         ( Session session,
                                           int region,
                                           List spids,
                                           int spidIndex,
                                           String swimActionId,
                                           Date recoveryCompleteTime,
                                           Date lastNotificationTime,
                                           long interval,
                                           boolean isTimeRangeRequest)
   {
    
           
       int spidCount = spids.size();
       
       
      
       
      
       if(spidIndex>= spidCount)           
       {
       
           sendFirstRecoveryRequest(session, region, recoveryCompleteTime);
           
           return;
       }
      
       String requestType  =NPACConstants.DOWNLOAD_DATA_NET_WORK_DATA;
       String currentSPID = spids.get(spidIndex).toString();
       
       if(lastNotificationTime == null){

           // This is the first download recovery request.
           // We need to first retrieve the last 
           // notification time from the DB.
           
           lastNotificationTime = getLastNetworkNotificationTime
                                                   (currentSPID, region);
       
                      // subtract recovery padding interval
           if(lastNotificationTime!=null){
               lastNotificationTime.setTime(lastNotificationTime.getTime()
                       - recoveryPadding);
               
           }
           
       }
        if(lastNotificationTime == null){
                   
                   isTimeRangeRequest=false;
        
                   Debug.error("No last notification time was found for "+
                               "customer SPID ["+currentSPID+"] in region ["+
                               region+
                               "]. Recovery for this SPID will be skipped " +
                               "in this "+  "region.");
        
//                 skip to the next SPID
                   sendDownloadRecoveryRequestNetWorkData( session,
                                                           region,
                                                           spids,
                                                           ++spidIndex,
                                                           null,
                                                           recoveryCompleteTime,
                                                           null,
                                                           interval,
                                                           isTimeRangeRequest);

                }else
                {
                	// This will only execute when SOA is under normal recovery mode. 
                	    	 
                	//	lastNotificationTime.setTime(lastNotificationTime.getTime() + 1000);
                	
                   
                    
                    // See if last notification time is equal to or later than
                    // the completion time (the time that the association
                    // was initialized). The division by one thousand gets the units
                    // into seconds.
                    if(swimActionId == null  && lastNotificationTime.getTime()/1000 >=
                        recoveryCompleteTime.getTime()/1000){
                        
                        isTimeRangeRequest=false;

                        // move on to the next SPID for this region
                        sendDownloadRecoveryRequestNetWorkData
                                                        ( session,
                                                          region,
                                                          spids, 
                                                          ++spidIndex,
                                                          null, 
                                                          recoveryCompleteTime,
                                                          null,
                                                          interval,
                                                          isTimeRangeRequest);

                     }else
                     {
                          //Add the retry window to the last notification time to get
                         // the stop time.
                         long stopTimeMs = lastNotificationTime.getTime()
                                           +interval;
                         long completeTimeMs = recoveryCompleteTime.getTime();
                         
                         //If the stop time is greater than the
                         // completion time, then use the completion time as the
                         // stop time.
                         
                         if(stopTimeMs/1000 >= completeTimeMs/1000){

                             stopTimeMs = completeTimeMs;

                          }
                         
                         Date stopTime = new Date(stopTimeMs);
                         try{    
                             
                             String invokeID = getNextInvokeID();
                         
                             DownloadRecoveryReplyHandler replyHandler =
                                 new DownloadRecoveryReplyHandler
                                                   ( this,
                                                     session,
                                                     region,
                                                     spids,
                                                     spidIndex,
                                                     recoveryCompleteTime,
                                                     stopTime,
                                                     lastNotificationTimeAccess,
                                                     interval,
                                                     requestType,
                                                     isTimeRangeRequest);

                         
                         replyHandlers.add(invokeID, replyHandler, resendTimeout);

       
                         int ack =
                             client.sendDownloadRecoveryNetworkDataRequest
                                                   ( session.getSessionID(),
                                                     invokeID,
                                                     region,
                                                     currentSPID,
                                                     lastNotificationTime,
                                                     stopTime,
                                                     swimActionId,
                                                     isTimeRangeRequest );
                              if(ack != NPACConstants.ACK_RESPONSE){
                              
                           

                             // don't need to listen for a response that isn't coming
                             replyHandlers.cancel(invokeID);

                             // try to reinitialize the association
                                   retryAssociationRequest(session, region);

                          }

                          
                      }catch(DatabaseException dbex){

                          Debug.error("Could not get invoke ID for recovery request: "+
                                      dbex);
                      }

                     }
                         
                }
	   
   }
  
   /**
    * Sends Swim processing recovery result request for service providers data.
    * @param session session involved in the request
    * @param region region associated with request.
    * @param spids list of spids
    * @param spidIndex spid no to perform swim request
    * @param recoveryCompleteTime recovery complete time in swim request
    * @param lastNotificationTime last notification time
    * @param swimActionID actionId populated in service provider recovery
    * @param spid spid associated with swimRecovery
    * @param recoveryInterval interval to be added in swim reocovery reply handler
    */
   
   public void sendServiceProvDataSwimRequest(Session session, int region,
                                              List spids, int spidIndex,
                                              Date recoveryCompleteTime, 
                                              Date lastNotificationTime, 
                                              String swimActionID,
                                              String spid,
                                              long recoveryInterval) {
       
        if (Debug.isLevelEnabled(Debug.IO_STATUS)) {

            Debug.log(Debug.IO_STATUS, "Sending swim request for session ["
                    + session.getSessionID() + "] in region [" + region + "]"
                    + "and spid is  [" + spid + "]");

        }
        
       
        try {

            String invokeID = getNextInvokeID();

            Debug.log(Debug.SYSTEM_CONFIG,
                 "*** Getting invoke Id for Swim Recovery  request  *****::::"
                            + invokeID);

            // create a notification handler to wait for the response
            // need to write SwimRecoveryReplyHandler

            ServiceProvidersDataSwimProcessingRecoveryReplyHandler replyHandler
            = new ServiceProvidersDataSwimProcessingRecoveryReplyHandler
                                                        ( this,
                                                          session, 
                                                          spids, 
                                                          spidIndex, 
                                                          recoveryCompleteTime,
                                                          lastNotificationTime, 
                                                          swimActionID, 
                                                          region, 
                                                          recoveryInterval);
            replyHandlers.add(invokeID, replyHandler, resendTimeout);

            int ack = client.sendSwimRequest(session.getSessionID(), invokeID,
                    region, lastNotificationTime, spid, swimActionID);

            if (ack != NPACConstants.ACK_RESPONSE) {
               // the bellow logic retry SwimRequest logic needs to be added to
               // the
                replyHandlers.cancel(invokeID);
                // retrySwimRequest()

                retryAssociationRequest(session, region);

            }

        } catch (DatabaseException dbex) {

            Debug.error("Could not get invoke ID to send recovery complete: "
                    + dbex);

        }

    }
   
   /**
    * Sends Swim Processing recovery result Request for net work data.
    * @param session session involved in the request
    * @param region region associated with request.
    * @param spids list of spids
    * @param spidIndex spid no to perform swim request
    * @param recoveryCompleteTime recovery complete time in swim request
    * @param lastNotificationTime last notification time
    * @param swimActionID actionId populated in service provider recovery
    * @param spid spid associated with swimRecovery
    * @param recoveryInterval interval to be added in swim reocovery reply
    *                         handler
    */
   
   public void sendNetworkDataSwimRequest(Session session, 
                                          int region, 
                                          List spids, 
                                          int spidIndex,
                                          Date recoveryCompleteTime, 
                                          Date lastNotificationTime,
                                          String swimActionID,
                                          String spid, 
                                          long recoveryInterval) {
       if (Debug.isLevelEnabled(Debug.IO_STATUS)) {

           Debug.log(Debug.IO_STATUS, "Sending swim request for session ["
                   + session.getSessionID() + "] in region [" + region + "]"
                   + "and spid is  [" + spid + "]");

       }
       
       Debug.log(Debug.SYSTEM_CONFIG, "*** last NotificationTime in network:::"
               +lastNotificationTime);
       Debug.log(Debug.SYSTEM_CONFIG, "*** Recovery complete time in network:::"
               +recoveryCompleteTime);

       try {

           String invokeID = getNextInvokeID();

           Debug.log(Debug.SYSTEM_CONFIG,
                   "*** Getting invoke Id for Swim Recovery  request  *****::::"
                           + invokeID);

           // create a notification handler to wait for the response
           // need to write SwimRecoveryReplyHandler
           

           NetworkDataSwimProcessingRecoveryReplyHandler 
           replyHandler = new NetworkDataSwimProcessingRecoveryReplyHandler
                                                      (this,
                                                       session,
                                                       spids,
                                                       spidIndex,
                                                       recoveryCompleteTime,
                                                       lastNotificationTime, 
                                                       swimActionID, 
                                                       region,
                                                       recoveryInterval);
           replyHandlers.add(invokeID, replyHandler, resendTimeout);

           int ack = client.sendSwimRequest(session.getSessionID(), invokeID,
                   region, lastNotificationTime, spid, swimActionID);

           if (ack != NPACConstants.ACK_RESPONSE) {
               // the bellow logic retry SwimRequest logic needs to be added to
               // the
               replyHandlers.cancel(invokeID);
               // retrySwimRequest()

               retryAssociationRequest(session, region);

           }

       } catch (DatabaseException dbex) {

           Debug.error("Could not get invoke ID to send recovery complete: "
                   + dbex);

       }

       
   }
   
    /**
    * Sends Swim Processing recovery result Request for RecoveryRequest data.
    * @param session session involved in the request
    * @param region region associated with request.
    * @param spids list of spids
    * @param spidIndex spid no to perform swim request
    * @param recoveryCompleteTime recovery complete time in swim request
    * @param lastNotificationTime last notification time
    * @param swimActionID actionId populated in service provider recovery
    * @param spid spid associated with swimRecovery
    * @param recoveryInterval interval to be added in swim reocovery reply 
    *                          handler
    */

   
   public void sendRecoverySwimRequest(Session session,
                                       int  region,
                                       List spids,
                                       int spidIndex,
                                       Date recoveryCompleteTime,
                                       Date lastNotificationTime,
                                       String swimActionID,
                                       String spid,long recoveryInterval)
   {
       
       if (Debug.isLevelEnabled(Debug.IO_STATUS)) {

           Debug.log(Debug.IO_STATUS, "Sending Recovery swim request session ["
                   + session.getSessionID() + "] in region [" + region + "]"
                   + "and spid is  [" + spid + "]");

       }
       
       try {

           String invokeID = getNextInvokeID();

           Debug.log(Debug.SYSTEM_CONFIG,
                   "*** Getting invoke Id for Swim Recovery  request  *****::::"
                           + invokeID);

           // create a notification handler to wait for the response
           // need to write SwimRecoveryReplyHandler

           RecoveryReplySwimProcessingRecoveryReplyHandler replyHandler
           =  new RecoveryReplySwimProcessingRecoveryReplyHandler 
                                                        (  this, 
                                                           session, 
                                                           spids, 
                                                           spidIndex, 
                                                           recoveryCompleteTime,
                                                           lastNotificationTime, 
                                                           swimActionID, 
                                                           region, 
                                                           recoveryInterval);

           replyHandlers.add(invokeID, replyHandler, resendTimeout);

           int ack = client.sendSwimRequest(session.getSessionID(), invokeID,
                   region, lastNotificationTime, spid, swimActionID);

           if (ack != NPACConstants.ACK_RESPONSE) {
               // the bellow logic retry SwimRequest logic needs to be added to
               // the
               replyHandlers.cancel(invokeID);
               // retrySwimRequest()

               retryAssociationRequest(session, region);

           }

       } catch (DatabaseException dbex) {

           Debug.error("Could not get invoke ID to send recovery complete: "
                   + dbex);

       }

       
       
   }


   /**
     * This creates a retry task that will resend a failed RecoveryRequest and
     * adds it to the worker queue to be executed later in a worker thread.
     */
   public void retryRecoveryRequest(Session session,
                                    int region,
                                    List spids,
                                    int spidIndex,
                                    Date recoveryCompleteTime,
                                    Date lastNotificationTime,
                                    long recoveryInterval,
                                    String requestType,
                                    boolean isTimeRangeRequest){
       
       RecoveryRequestRetry retry = new RecoveryRequestRetry(retryInterval,
               this,
               session,
               region,
               spids,
               spidIndex,
               recoveryCompleteTime,
               lastNotificationTime,
               recoveryInterval, 
               requestType,
               isTimeRangeRequest);

      enqueue(retry);

   }

   /**
   * This creates a retry task that will resend a failed RecoveryRequest
   * and adds it to the worker queue to be executed later in
   * a worker thread.
   */
   public void retryTimeBasedRecoveryRequest(Session session,
                                    int region,
                                    List spids,
                                    int spidIndex,
                                    Date recoveryCompleteTime,
                                    Date lastNotificationTime,
                                    long recoveryInterval){

      TimeBasedRecoveryRequestRetry retry =
         new TimeBasedRecoveryRequestRetry(retryInterval,
                                  this,
                                  session,
                                  region,
                                  spids,
                                  spidIndex,
                                  recoveryCompleteTime,
                                  lastNotificationTime,
                                  recoveryInterval );

      enqueue(retry);

   }

   /**
    * This creates a retry task that will resend a failed swim processing
    * recovery result request for service provider data.
    * adds it to the worker queue to be executed later in a worker thread.
    * @param session session involved in the request.
    * @param region regiion associated with request.
    * @param spids list of spids for the given region
    * @param spidIndex index of the spid 
    * @param recoveryCompleteTime  recovery complete time in swim request
    * @param lastNotificationTime
    * @param swimActionID actionId in the swim request
    * @param spid asoociated spid in the retry
    * @param recoveryInterval interval after retry to perform
    */
   public void retryServProvSwimRequest(Session session,
                                        int region,
                                        List spids,
                                        int spidIndex,
                                        Date recoveryCompleteTime,
                                        Date lastNotificationTime,
                                        String swimActionID, 
                                        String spid,
                                        long recoveryInterval) {

        ServiceProvidersDataSwimProcessingRecoveryRetry retry 
        = new ServiceProvidersDataSwimProcessingRecoveryRetry
                                              ( retryInterval,
                                                this, 
                                                session, 
                                                region, 
                                                spids,
                                                spidIndex,
                                                recoveryCompleteTime, 
                                                lastNotificationTime, 
                                                swimActionID, 
                                                spid,
                                                recoveryInterval);
        enqueue(retry);

    }
   
   /**
   * This creates a retry task that will resend a failed swim processing
   * recovery result request for network  data.
   * adds it to the worker queue to be executed later in a worker thread.
   * @param session session involved in the request.
   * @param region regiion associated with request.
   * @param spids list of spids for the given region
   * @param spidIndex index of the spid 
   * @param recoveryCompleteTime  recovery complete time in swim request
   * @param lastNotificationTime
   * @param swimActionID actionId in the swim request
   * @param spid asoociated spid in the retry
   * @param recoveryInterval interval after retry to perform
   */
   
   public void retryNetWorkDataSwimRequest(Session session,
                                           int region, List spids,
                                           int spidIndex,
                                           Date recoveryCompleteTime,
                                           Date lastNotificationTime,
                                           String swimActionID, 
                                           String spid,
                                           long recoveryInterval) {

       NetworkDataSwimProcessingRecoveryRetry retry 
= new NetworkDataSwimProcessingRecoveryRetry(retryInterval,
                                 this, 
                                 session, 
                                 spids,
                                 spidIndex,
                                 region, 
                                 recoveryCompleteTime, 
                                 lastNotificationTime, 
                                 swimActionID, 
                                 spid,
                                 recoveryInterval);
enqueue(retry);

}

   
   /**
    * This creates a retry task that will resend a failed swim processing
    * recovery result request Recovery request
    * adds it to the worker queue to be executed later in a worker thread.
    * @param session session involved in the request.
    * @param region regiion associated with request.
    * @param spids list of spids for the given region
    * @param spidIndex index of the spid 
    * @param recoveryCompleteTime  recovery complete time in swim request
    * @param lastNotificationTime
    * @param swimActionID actionId in the swim request
    * @param spid asoociated spid in the retry
    * @param recoveryInterval interval after retry to perform
    */
   
   public void retryRecoverySwimRequest(Session session,
                                        int region,
                                        List spids,
                                        int spidIndex,
                                        Date recoveryCompleteTime,
                                        Date lastNotificationTime,
                                        String swimActionID, 
                                        String spid,
                                        long recoveryInterval)
   {
       RecoverySwimProcessingRecoveryRetry retry 
       = new RecoverySwimProcessingRecoveryRetry(retryInterval,
                                                 this, 
                                                 session, 
                                                 region, 
                                                 spids,
                                                 spidIndex,
                                                 recoveryCompleteTime, 
                                                 lastNotificationTime, 
                                                 swimActionID, 
                                                 spid,
                                                 recoveryInterval);
       enqueue(retry);
       
       
   }

   /**
     * Sends out the initial recovery request for the given region.
     * 
     * @param session
     *             Session
     * @param region
     *            int
     * @param recoveryCompleteTime
     *            Date
     */
   public void sendFirstRecoveryRequest(Session session,
                                        int region,
                                        Date recoveryCompleteTime){
	   Debug.log(Debug.SYSTEM_CONFIG,"*** Sending first recovery request::::");

      List spids = session.getSPIDsForRegion( region );
      boolean isTimeRangeRequest=false;
      
      sendRecoveryRequest(session,
                          region,
                          spids,
                          0,
                          recoveryCompleteTime,
                          null,
                          recoveryWindow,
                          isTimeRangeRequest);

   }

   /**
   * This sends a RecoveryRequest that queries for any notifications
   * that may have been missed while the SOA gateway was down.
   * Since recovery requests can only be made for at most one hour
   * windows at a time, this method adds the default query interval
   * (usually an hour)
   * to the last notification time, and performs the recovery request.
   * A recovery reply handler is created to handle the response. When
   * the reply is received, the reply handler
   * adjusts the last notification time accordingly, and then calls this
   * method again with the new time. This process repeats until the
   * last notification time meets or exceeds the recoveryCompleteTime
   * at which point we move on to recovery for the next SPID in the spids
   * list (if there is a next SPID). When the list of SPIDs is exhausted,
   * then a RecoveryCompleteRequest is sent for this region.
   *
   * @param session the session which is recovering.
   * @param region the region against which we are querying. An Association
   *               must be connected for this region and the Assoication
   *               must be in recovery mode.
   * @param spids the list of customer SPIDs for which recovery needs
   *              to be done for this region.
   * @param spidIndex This index relative to the list of spids that indicates
   *                  which SPID we are currently recovering for.
   * @param recoveryCompleteTime recovery must be done up to this time.
   *                             Once the recovery window reaches this
   *                             time, then recovery is complete for the
   *                             current SPID in this region.
   * @param lastNotificationTime This is the last notification time for
   *                             the current SPID in the current region.
   *                             If this value is null, this indicates that
   *                             this is the first recovery request for
   *                             this SPID in this region, and the last
   *                             notification time will be retrieved from the
   *                             database. If the last notification time
   *                             has a value, then this indicates the
   *                             time up to which the last recovery
   *                             request was made.
   */
   public void sendRecoveryRequest(Session session,
                                   int region,
                                   List spids,
                                   int spidIndex,
                                   Date recoveryCompleteTime,
                                   Date lastNotificationTime,
                                   long recoveryInterval,
                                   boolean isTimeRangeRequest){
       
               
               
               sendRecoveryRequest(session, 
                       region,
                       spids,
                       spidIndex,
                       null,
                       recoveryCompleteTime,
                       lastNotificationTime,
                       recoveryWindow,
                       isTimeRangeRequest);
               
              

   }

   /**
    * This sends a SWIM RecoveryRequest 
    * that queries for any notifications that may have been missed 
    * while the SOA gateway was down.
    * This method adds the given query interval
    * to the last notification time, and performs the recovery request.
    * A recovery reply handler is created to handle the response. When
    * the reply is received, the reply handler
    * adjusts the last notification time accordingly, and then calls this
    * method again with the new time. This process repeats until the
    * last notification time meets or exceeds the recoveryCompleteTime
    * at which point we move on to recovery for the next SPID in the spids
    * list (if there is a next SPID). When the list of SPIDs is exhausted,
    * then the first RecoveryRequest is sent for this region.
     * @param session the session which is recovering.
     * @param region the region against which we are querying. An Association
     *               must be connected for this region and the Assoication
     *               must be in recovery mode.
     * @param spids the list of customer SPIDs for which recovery needs
     *              to be done for this region.
     * @param spidIndex his index relative to the list of spids that indicates
     *                  which SPID we are currently recovering for.
     * @param swimActionID actionId will populated when we receive 
     *                      swim_more_data in the Download Recovery Reply. 
     * @param recoveryCompleteTime recovery must be done up to this time.
     *                             Once the recovery window reaches this
     *                             time, then recovery is complete for the
     *                             current SPID in this region.
     * @param lastNotificationTime This is the last notification time for
     *                             the current SPID in the current region.
     *                             If this value is null, this indicates that
     *                             this is the first recovery request for
     *                             this SPID in this region, and the last
     *                             notification time will be retrieved from the
     *                             database. If the last notification time
     *                             has a value, then this indicates the
     *                             time up to which the last recovery
     *                             request was made.
     * @param interval the recovery interval to use in the recovery request.
     * @param isTimeRangeRequest if the method is called from DRRHandler 
     *                           the attribute populated is "true" based on 
     *                           this we will generate time range recovery 
     *                           requests.
     */
   
   public void sendRecoveryRequest(Session session,
                                   int region,
                                   List spids,
                                   int spidIndex,
                                   String swimActionId,
                                   Date recoveryCompleteTime,
                                   Date lastNotificationTime,
                                   long recoveryInterval,
                                   boolean isTimeRangeRequest){
       
       
       int spidCount = spids.size();
       String requestType  =NPACConstants.RECOVERY_REQUEST;

       if( spidCount == 0 || spidIndex >= spidCount ){

          // We have run out of supported SPIDs for this region/association.
          // Send a recovery complete request for this association.

          sendRecoveryCompleteRequest(session, region);

          return;

       }

       
       String currentSPID = spids.get(spidIndex).toString();

       if(lastNotificationTime == null){

          // This must be the first recovery request for this SPID.
          // We need to first retrieve its last notification time from the DB.
          lastNotificationTime = getLastNotificationTime(currentSPID,
                                                         region);
        
          if( lastNotificationTime != null &&
              recoveryPadding > 0 ){
              
             // subtract recovery padding interval
             lastNotificationTime.setTime(lastNotificationTime.getTime()
                                          - recoveryPadding);

          }

       }
       
       if(lastNotificationTime == null){

           Debug.error("No last notification time was found for "+
                       "customer SPID ["+currentSPID+"] in region ["+
                       region+
                       "]. Recovery for this SPID will be skipped in this "+
                       "region.");
           
           isTimeRangeRequest=false;

           // skip to the next SPID
           sendRecoveryRequest(session, 
                   region,
                   spids,
                   ++spidIndex,
                   null,
                   recoveryCompleteTime,
                   null,
                   recoveryWindow,
                   isTimeRangeRequest);
       }else
       {
           // Add one second to the time so that it is not *exactly*
           // when the last notification was sent
           // (which would include the last notification) or exactly
           // when the last recovery query ended (which would
           // be a double-query for that last second).
    //       lastNotificationTime.setTime( lastNotificationTime.getTime()
      //             + 1000 );
           

           // See if last notification time is equal to or later than
           // the completion time (the time that the association
           // was initialized). The division by one thousand gets the units
           // into seconds.
           if(swimActionId == null && lastNotificationTime.getTime()/1000 >=
              recoveryCompleteTime.getTime()/1000){
               
               isTimeRangeRequest=false;

              // move on to the next SPID for this region
               sendRecoveryRequest(session, 
                       region,
                       spids,
                       ++spidIndex,
                       null,
                       recoveryCompleteTime,
                       null,
                       recoveryWindow,
                       isTimeRangeRequest);

           }else
           {
               
               // Add the retry window to the last notification time to get
               // the stop time.
               long stopTimeMs = lastNotificationTime.getTime() +
                                 recoveryInterval;

               // If the stop time is greater than the
               // completion time, then use the completion time as the
               // stop time.
               long completeMs = recoveryCompleteTime.getTime();

               if(stopTimeMs/1000 >= completeMs/1000){

                  stopTimeMs = completeMs;

               }

               Date stopTime = new Date(stopTimeMs);

               try{

                  String invokeID = getNextInvokeID();

                  if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

                      Debug.log(Debug.IO_STATUS,
                                "Sending recovery request:\n\tSession ID ["+
                                session.getSessionID()+
                                "]\n\tRegion ["+region+
                                "]\n\tStart Time ["+
                                lastNotificationTime.toGMTString()+
                                "]\n\tStop Time ["+
                                stopTime.toGMTString()+"]");

                   }
                  
                  

                  // create a response handler to wait for the recovery reply
                  RecoveryReplyHandler replyHandler =
                      new RecoveryReplyHandler(this,
                                               session,
                                               region,
                                               spids,
                                               spidIndex,
                                               recoveryCompleteTime,
                                               stopTime,
                                               lastNotificationTimeAccess,
                                               recoveryInterval,
                                               requestType,                                        
                                               isTimeRangeRequest);
                  

                  replyHandlers.add(invokeID, replyHandler, resendTimeout);  
                  
            int ack =
                     client.sendRecoveryRequest( session.getSessionID(),
                                                         invokeID,
                                                         region,
                                                         currentSPID,
                                                         lastNotificationTime,
                                                         stopTime,
                                                         swimActionId,
                                                         isTimeRangeRequest);
                                                          
                  if(ack != NPACConstants.ACK_RESPONSE){
                     // don't need to listen for a response that isn't coming
                     replyHandlers.cancel(invokeID);

                     // try to reinitialize the association
                           retryAssociationRequest(session, region);
                  }
              }catch(DatabaseException dbex){

                  Debug.error("Could not get invoke ID for recovery request: "+
                              dbex);
              }
           }
       }
   }

   /**
   * This sends a RecoveryComplete message letting the
   * OSS Gateway know that recovery is complete for this given session in the
   * given region. A reply handler is created to wait for the
   * response. When the reply is received, the next AssoicationRequest is
   * sent for the next region.
   */
   public void sendRecoveryCompleteRequest(Session session,
                                           int region){

      if(Debug.isLevelEnabled(Debug.IO_STATUS)){

         Debug.log(Debug.IO_STATUS,
                   "Sending recovery complete request for session ["+
                   session.getSessionID()+"] in region ["+region+"]" );

      }

      try{

         String invokeID = getNextInvokeID();

         // create a notification handler to wait for the response
         RecoveryCompleteReplyHandler replyHandler =
            new RecoveryCompleteReplyHandler(this,
                                             session,
                                             region);

         replyHandlers.add(invokeID, replyHandler, resendTimeout);


         int ack = client.sendRecoveryComplete(session.getSessionID(),
                                               invokeID,
                                               region,
                                               session.getPrimarySPID());

         if(ack != NPACConstants.ACK_RESPONSE){

            retryAssociationRequest(session, region);

         }

      }
      catch(DatabaseException dbex){

         Debug.error("Could not get invoke ID to send recovery complete: "+
                     dbex);

      }

   }

   /**
   * This retrieves the last notification time from the DB for the given SPID
   * in the given region.
   */
   public Date getLastNotificationTime(String spid,
                                       int region){

      Date lastNotificationTime = null;

      try{

         lastNotificationTime =
             lastNotificationTimeAccess.getLastNotificationTime(spid,
                                                                region);

      }
      catch(DatabaseException dbex){

         Debug.error("Could not retrieve last notification time from database "+
                     " for spid ["+spid+"] and region ["+region+"]: "+
                     dbex);

      }

      return lastNotificationTime;

   }
   
  /**
   * This retrieves the last Service providrers data notification time from 
   * the DB for the given region.
   * 
   * @param spid spid in the query
   * @param region region to which Lastnotification time required.
   * @return Date
   */
   public Date getLastServiceProvidersNotificationTime(String spid,int region){
        Date lastNotificationTime = null;

        try {

            lastNotificationTime = lastNotificationTimeAccess
                    .getLastServiceProvNotificationTime(spid, region);


        } catch (DatabaseException dbex) {

            Debug
                    .error("Could not retrieve last network notification time"+
                            "from the database for SPID [" + spid+ "] " +
                                    "in region [" + region + "]: " + dbex);

        }

        return lastNotificationTime;

    }

	   
   

   /**
   * This retrieves the last network notification time from the DB
   * for the given region.
   */
   public Date getLastNetworkNotificationTime(String spid, int region){
	   

      Date lastNotificationTime = null;

      try{

         lastNotificationTime =
             lastNotificationTimeAccess.getLastNetworkNotificationTime(spid,
                                                                     region);

      }
      catch(DatabaseException dbex){

         Debug.error("Could not retrieve last network notification time from "+
                     "the database for SPID ["+spid+"] in region ["+
                     region+"]: "+
                     dbex);

      }

      return lastNotificationTime;

   }
   
   

   /**
   * This retrieves the last ServiceProviderNotificationTime time from the DB
   * for the given region.
   */
   public Date getLastServiceProviderNotificationTime(String spid, int region){
       

      Date lastNotificationTime = null;

      try{

         lastNotificationTime =
             lastNotificationTimeAccess.getLastServiceProvNotificationTime
                                                         (spid,region);

      }
      catch(DatabaseException dbex){

         Debug.error("Could not retrieve last network notification time from "+
                     "the database for SPID ["+spid+"] in region ["+
                     region+"]: "+
                     dbex);

      }

      return lastNotificationTime;

   }

   /**
   * This gets the primary SPID for the given secondary SPID.
   * Each primary SPID has up to 39 secondary SPIDs associated
   * with it.
   */
   public String getPrimarySPID(String secondarySPID){

      return (String) secondaryToPrimary.get(secondarySPID);

   }

   /**
   * This looks up the Session that corresponds to the given primary SPID.
   * There is a one-to-one correspondence between primary SPIDs and
   * sessions.
   */
   public Session getSession(String primarySPID){

      return (Session) primaryToSession.get(primarySPID);

   }

   /**
   * This looks up the primary SPID for the given secondary
   * SPID. Then it gets the session instance for that primary
   * SPID. Finally, this returns the session ID for that session.
   *
   * The session ID may still be zero if the session has not yet been
   * initialized. This method will return null if the primary SPID
   * or session instance could not be found.
   */
   public String getSessionID( String secondarySPID ){

      String sessionID = null;

      // get the primary SPID for the given secondary SPID
      String primarySPID = getPrimarySPID( secondarySPID );

      if(primarySPID == null){

         Debug.error("Could not find primary SPID for secondary SPID ["+
                     secondarySPID+"]" );

      }
      else{

         Session session = getSession( primarySPID );

         if(session == null){
            Debug.error("Could not find a session for primary SPID ["+
                        primarySPID+"]" );
         }
         else{
            sessionID = session.getSessionID();
         }

      }

      return sessionID;

   }

   /**
   * This retrieves the Session instance with the given
   * session ID. If no session can be found with the given ID,
   * then this returns null.
   */
   public Session getSessionWithID( String sessionID ){

      Session[] sessions = getSessions();

      // Try to find the session with the given ID.
      // This is a linear search, but the number of sessions
      // is expected to be fairly low (1 session per 39 customers)
      // so this should not be too expensive.
      for(int i = 0; i < sessions.length; i++){

         Session session = (Session) sessions[i];

         String currentID = session.getSessionID();

         if( currentID != null && currentID.equals(sessionID) ){

            return session;

         }

      }

      return null;

   }

   /**
   * This gets the array of all Sessions defined in the primaryToSession
   * table.
   */
   private Session[] getSessions(){

      Set entrySet = primaryToSession.entrySet();

      int count = entrySet.size();

      Object[] entries = new Object[ count ];
      entrySet.toArray(entries);

      Session[] sessions = new Session[ count ];

      for(int i = 0; i < count; i++){

          Map.Entry entry = (Map.Entry) entries[i];
          sessions[i] = (Session) entry.getValue();

      }

      return sessions;

   }

   /**
   * This is called when a GatewayKeepAlive notification is received.
   * This retrieves the keep alive sequence number from the given
   * notification.
   *
   * @param sessionID the ID for the session to keep alive.
   * @param parsedNotification the parsed keep alive notification.
   */
   public int receiveKeepAlive(String sessionID,
                               XMLMessageParser parsedNotification){

      // get sequence number
      String sequenceValue = null;
      int sequenceNumber;
      try{

         sequenceValue = parsedNotification.getTextValue(
                                  NPACConstants.GATEWAY_KEEP_ALIVE_SEQNO);

         sequenceNumber = Integer.parseInt( sequenceValue );

      }
      catch(MessageException mex){

         Debug.error("Could not get sequence number from gateway keep alive notification: "+
                     mex);

         return NPACConstants.NACK_RESPONSE;

      }
      catch(NumberFormatException nfex){


         Debug.error("Sequence number ["+sequenceValue+
                     "] is not a valid integer: "+
                     nfex);

         return NPACConstants.NACK_RESPONSE;

      }

      return receiveKeepAlive( sessionID, sequenceNumber );

   }

   /**
   * This is called when a GatewayKeepAlive notification is received.
   *
   * @param sessionID the ID for the session to keep alive.
   * @param sequenceNumber the sequence number received in the keep alive
   *                       notification.
   */
   public int receiveKeepAlive(String sessionID, int sequenceNumber){

      if( Debug.isLevelEnabled(Debug.IO_STATUS) ){
         Debug.log(Debug.IO_STATUS,
                   "Received keep alive for session ID ["+sessionID+
                   "] sequence number ["+sequenceNumber+"]");
      }

      // get the session with the given session ID
      Session session = getSessionWithID( sessionID );

      if( session == null ){

         Debug.warning("The gateway keep alive notification with sequence number ["+
                       sequenceNumber+
                       "] failed. No session was found for session ID ["+
                       sessionID+"].");

         return NPACConstants.NACK_RESPONSE;

      }

      int ack = session.keepAliveReceived( sequenceNumber );

      // if result is a nack, then the session needs to be reinitialized
      if( ack == NPACConstants.NACK_RESPONSE ){
         reinitialize( session );
      }

      return ack;

   }

   /**
   * This instructs each active session to send a keep-alive message.
   * This is called periodically by the keep alive thread in the NPACComServer.
   */
   public void sendKeepAlive(){

      Session[] sessions = getSessions();

      if(sessions.length == 0){
         Debug.log(Debug.IO_STATUS, "Keep alive found no active sessions.");
      }

      try{

         for(int i = 0; i < sessions.length; i++){
        	 

             // Check to see if session is active. If so,
             // send a keep alive message
             if( sessions[i].isAlive() ){

                // get the invoke ID from the database sequence
                String invokeID = getNextInvokeID();

                int ack = client.sendKeepAlive(
                                    sessions[i].getSessionID(),
                                    sessions[i].nextKeepAliveSequenceNumber(),
                                    invokeID,
                                    sessions[i].getPrimarySPID() );

                // if a NACK is received, then we need to reinitialize the session
                if(ack == NPACConstants.NACK_RESPONSE){
                   reinitialize(sessions[i]);
                }

             }
             else if(Debug.isLevelEnabled(Debug.IO_STATUS)){
                Debug.log(Debug.IO_STATUS,
                          "Skipping keep alive request for session with"+
                          " primary SPID ["+
                          sessions[i].getPrimarySPID()+
                          "]. This session is not active." );
             }

         }

      }
      catch(DatabaseException dbex){

         Debug.error("Could not get next invoke ID while "+
                     "sending keep alive messages: "+
                     dbex.toString());

      }

   }

   /**
   * Get the NotificationHandler instance for the response notification
   * with the given invokeID. This may return null if a handler
   * has not been set for the given invoke ID. This
   * generally indicates that the incoming notification was unexpected.
   */
   public int handleNotification(String invokeID,
                                 XMLMessageParser notification){

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

         Debug.log(Debug.MSG_STATUS,
                   "Getting notification handler for invoke ID ["+
                   invokeID+"]");

      }

      NotificationHandler handler =
         (NotificationHandler) replyHandlers.remove(invokeID);

      if( handler == null ){

         Debug.error( "No notification handler was found for the notification "+
                      "with invoke ID ["+invokeID+
                      "]. This notification was not expected."  );

         return NPACConstants.NACK_RESPONSE;

      }

      // The handler does an initial check of the incoming
      // notification to see if it is acceptable (ACK or NACK).
      //
      int ack = handler.receiveNotification( notification );
     
      // Enqueue the handler to perform the next required task
      // such as sending the next request message. If the
      // notification is somehow in error (if a NACK is being returned),
      // then the handler is also responsible for performing any necessary
      // retry action.
      enqueue( handler );

      return ack;

   }

   /**
   * This checks to see if there is a NotificationHandler waiting to
   * handle a reply message with the given invoke ID.
   */
   public boolean notificationHandlerExists(String invokeID){

      return replyHandlers.exists(invokeID);

   }

   /**
   * This gets Recovery Flag(TimeBased or SWIM based) from the HashMap
   * and return the boolean value for each region.
   */
   public boolean getRecoveryStatus(int regionId){

		Integer recMap = null;

		recMap = (Integer)dbRecoveryMap.get(Integer.valueOf(regionId));

		Debug.log(Debug.NORMAL_STATUS, "recMap value in getRecoveryStatus():"+ recMap );

		if (recMap != null && recMap.intValue() == 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

   /**
    * This checks to see if there is an instance of GenericReplyHandler
    * handler waiting for the given invoke ID. This is used to determine
    * if the related request was an automated request (e.g. NewSession) or not.
    *
    * @param invokeID String the invoke ID to check.
    * @return boolean
    */
   public boolean isGenericReply(String invokeID){

      return replyHandlers.isGenericReplyHandler(invokeID);

   }

   /**
   * This handles a GatewayError in the case where the error
   * was sent in reply to a request made by the adapter.
   * The handler that was expecting the reply message for the
   * given invoke ID will be retrived and used to handle the
   * error. This may involve retrying the original request.
   * If the error received is an invalid-session error,
   * then the session will be reinitialized.
   */
   public int handleError(String invokeID, XMLMessageParser error){

      // remove the handler and cancel its timer so that it does not try to do
      // a resend
      NotificationHandler handler =
         (NotificationHandler) replyHandlers.cancel(invokeID);

      if( handler == null ){

         Debug.error( "No notification handler was found for the error "+
                      "with invoke ID ["+invokeID+
                      "]. This message was not expected."  );

         return NPACConstants.NACK_RESPONSE;

      }

      // Have the handler handle the error (and perform any necessary retry)
      handler.receiveError(error);

      return NPACConstants.ACK_RESPONSE;

   }

   /**
   * This checks the status of the given message, returning
   * true if the status is "invalid-session".
   */
   public static boolean isInvalidSession(XMLMessageParser message){

      boolean invalidSession = false;

      try{

         String[] states = statusPath.getValues(message.getDocument());

         for(int i = 0; i < states.length && !invalidSession; i++){

            if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
               Debug.log(Debug.MSG_STATUS,
                         "Received message with status: [" +
                         states[i] + "]");
            }

            invalidSession =
               states[i].equals(NPACConstants.SESSION_INVALID_STATUS);

         }

      }
      catch(FrameworkException fex){

         Debug.error("Could not retrieve status from message: "+fex);

      }

      return invalidSession;

   }

   public static boolean isAssociationInRecovery(XMLMessageParser message){
      return checkErrorCode(NPACConstants.ASSOCIATION_IN_RECOVERY_ERROR_CODE,
                            message);
   }


   public static boolean isRegionNotAssociated(XMLMessageParser message){
      return checkErrorCode(NPACConstants.REGION_NOT_ASSOCIATED_ERROR_CODE,
                            message);
   }

   private static boolean checkErrorCode(String code,
                                         XMLMessageParser message){

      boolean match = false;

      try{

         String[] codes = errorCodePath.getValues(message.getDocument());

         for(int i = 0; i < codes.length && !match; i++){

            if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
               Debug.log(Debug.MSG_STATUS,
                         "Received error code: [" +
                         codes[i] + "]");
            }

            match = codes[i].equals(code);

         }

      }
      catch(FrameworkException fex){

         Debug.error("Could not retrieve error code from message: "+fex);

      }

      return match;

   }

   /**
   * Shutdown all sessions. This should only be called when the gateway is
   * being brought down.
   */
   public void shutdown(){

      // cleanup SmartSockets connections
      client.cleanup();

      Session[] sessions = getSessions();

      if( Debug.isLevelEnabled(Debug.NORMAL_STATUS) ){

         Debug.log(Debug.NORMAL_STATUS,
                   "Shutting down ["+
                   sessions.length+"] sessions.");

      }

      for(int i = 0; i < sessions.length; i++){

         if( sessions[i].isAlive() ){

            String sessionID = sessions[i].getSessionID();

            try{

               String invokeID = getNextInvokeID();

               client.sendClientReleaseSession( sessionID,
                                                          invokeID,
                                                          sessions[i].getPrimarySPID() );
               // log any nacks, although we don't really care at this point,
               // because we are shutting down anyway

            }
            catch(DatabaseException dbex){

               Debug.error("Could not get invoke ID to send release request: "+
                           dbex);

            }

         }

      }

   }

   /**
   * Gets the next invoke ID from the invoke ID sequence.
   *
   * @throws DatabaseException if the sequence or its database cannot be
   *                           accessed.
   *
   */
   public String getNextInvokeID() throws DatabaseException{

      return Integer.toString(
    		  
                PersistentSequence.getNextSequenceValue( invokeIDSequence ) );

   }
   

   /**
   * This adds the given Runnable to a queue of runnables that
   * will get invoked in separate worker threads.
   */
   private void enqueue(Runnable runMe){

      queue.enqueue( runMe );

   }

   /**
   * This is used to send a "success" NotificationReply for Notifications
   * that required asynchronous replies.
   *
   * @param invokeID the invoke ID of the notification which we are responding
   *                 to.
   * @param sessionID the session ID to use in the reply.
   * @param customerID the SPID to use in the reply.
   *
   */
   public void sendSuccessNotificationReply(String invokeID,
                                            String sessionID,
                                            String customerID){

      client.sendNotificationReply(sessionID,
                                   invokeID,
                                   customerID,
                                   NPACConstants.SUCCESS_STATUS,
                                   null,
                                   null);

   }

   /**
   * This is used to send a "failed" NotificationReply for Notifications
   * that required asynchronous replies.
   *
   * @param invokeID the invoke ID of the notification to which we are
   *                 responding.
   * @param sessionID the session ID to use in the reply.
   * @param customerID the SPID to use in the reply.
   *
   */
   public void sendFailureNotificationReply(String invokeID,
                                            String sessionID,
                                            String customerID,
                                            String errorCode,
                                            String errorInfo){

      client.sendNotificationReply(sessionID,
                                   invokeID,
                                   customerID,
                                   NPACConstants.FAILED_STATUS,
                                   errorCode,
                                   errorInfo);

   }

   /**
   * This is used to send a "success" DownloadReply for network Notifications
   * that required asynchronous replies.
   *
   * @param invokeID the invoke ID of the notification to which we are
   *                 responding.
   *
   * @param sessionID the session ID to use in the reply.
   * @param customerID the SPID to use in the reply.
   *
   */
   public void sendSuccessDownloadReply(String invokeID,
                                            String sessionID,
                                            String customerID){

      client.sendDownloadReply(sessionID,
                               invokeID,
                               customerID,
                               NPACConstants.SUCCESS_STATUS,
                               null,
                               null);

   }

   /**
   * This is used to send a "failed" DownloadReply for network Notifications
   * that required asynchronous replies.
   *
   * @param invokeID the invoke ID of the notification to
   *                 which we are responding.
   * @param sessionID the session ID to use in the reply.
   * @param customerID the SPID to use in the reply.
   *
   */
   public void sendFailureDownloadReply(String invokeID,
                                        String sessionID,
                                        String customerID,
                                        String errorCode,
                                        String errorInfo){

      client.sendDownloadReply(sessionID,
                               invokeID,
                               customerID,
                               NPACConstants.FAILED_STATUS,
                               errorCode,
                               errorInfo);

   }

   /**
    * Sends the given message to the OSS Gateway via the NPAC Client.
    * This also creates a ReplyHandler instance to timeout and resend
    * in the case that no reply is received.
    *
    * @param spid String the SPID associated with the message.
    * @param invokeID the invoke ID for the given message. This is used
    *                 as the key in waiting for the corresponding response
    *                 message from the NPAC.
    * @param xml String the XML message itself.
    */
    public int send(String spid,
                    String invokeID,
                    String xml)
                    throws FrameworkException{

      String primarySpid = getPrimarySPID( spid );

      if(primarySpid == null){
         primarySpid = spid;
      }

      Session session = getSession( primarySpid );

      // create a response handler to wait for the reply message
      GenericReplyHandler handler = new GenericReplyHandler(this,
                                                            session,
                                                            invokeID,
                                                            spid);
      replyHandlers.add(invokeID, handler);

      return send(spid, xml);

   }

   /**
    * Sends the given message to the OSS Gateway via the NPAC Client.
    *
    * @param spid String the SPID associated with the message.
    * @param xml String the XML message itself.
    */
    public int send(String spid,
                    String xml){

      return client.send(spid, xml);

   }

   /**
    * Calls back to the com server to process the given message using the
    * driver chain.
    *
    */
   public void process(XMLMessageParser parsedMessage){

      try{

         XMLMessageGenerator generator =
            (XMLMessageGenerator) parsedMessage.getGenerator();

         process(generator.generate());
         
         

      }
      catch(MessageException mex){
         Debug.logStackTrace(mex);
         
      }

   }


   /**
    * Calls back to the com server to process the given message using the
    * driver chain.
    *
    * @param message String
    */
   public void process(String message){

      comServer.processMessage(message);

   }

}

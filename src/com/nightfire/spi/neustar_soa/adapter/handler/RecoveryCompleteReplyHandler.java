///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.adapter.Session;
import com.nightfire.spi.neustar_soa.queue.NPACQueueUtils;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;

/**
* Instances of this class are used to wait for and handle
* RecoveryCompleteReply messages.
*/
public class RecoveryCompleteReplyHandler extends NotificationHandler {

   /**
   * The region for which recovery is complete.
   */
   private int region;

   /**
   * The boolean value retrieved from the reply that indicates whether
   * recovery is complete or not.
   */
   private boolean recoveryComplete = false;
   
   /**
    * The spid for which recovery is complete.
    */
   private String spidValue = null;

   public RecoveryCompleteReplyHandler(NPACAdapter adapter,
                                       Session session,
                                       int region){

      super(adapter, session);

      this.region = region;

   }

   /**
   * This receives the parsed RecoveryCompleteReply and extracts
   * the value of the flag that indicates whether it was a successful
   * reply or not.
   */
   public int receiveNotification(XMLMessageParser parsedNotification){

      int ack = super.receiveNotification(parsedNotification);

      if(ack == NPACConstants.ACK_RESPONSE){

         try{
        	 
        	 if(parsedNotification.nodeExists(NPACConstants.RECOVERY_COMPLETE_REPLY)){
        		 
        		 String value = parsedNotification.getTextValue(NPACConstants.RECOVERY_COMPLETE_REPLY);
        		 
        		 recoveryComplete = value.equals(NPACConstants.XBOOL_TRUE);
        		 
        	 }else if(parsedNotification.nodeExists(NPACConstants.RECOVERY_COMPLETE_REPLY_ERROR_CODE)){
        		 
        		 String value = parsedNotification.getTextValue(NPACConstants.RECOVERY_COMPLETE_REPLY_ERROR_CODE);
        		 
        		 if(value.equals(NPACConstants.SUCCESS_STATUS)){
        			 
        			 recoveryComplete =true;
        			 
        		 }else if(value.equals(NPACConstants.FAILED_STATUS)){
        			 
        			 recoveryComplete = false;
        			 
           		 }else if(value.equals(NPACConstants.SESSION_INVALID_STATUS)){
           			 
           			 recoveryComplete = false;
           			 
           			Debug.log(Debug.MSG_STATUS, "**************************************Recovery Session is in valid. " +
    		 		"Please restart the connectivity server once again !! ****************************"); 
           			 
           		 }
        		 
        	 }
        	 if(parsedNotification.nodeExists(NPACConstants.CUSTOMER_ID))
        	 {
        		 spidValue = parsedNotification.getTextValue(NPACConstants.CUSTOMER_ID);
        	 }
        	 
        	 
 /*       	 
            String value =
               parsedNotification.getTextValue(
               NPACConstants.RECOVERY_COMPLETE_REPLY);

            recoveryComplete = value.equals(NPACConstants.XBOOL_TRUE);
*/
            if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

               Debug.log(Debug.MSG_STATUS,
                         "Recovery complete value  : ["+
                         recoveryComplete+"]");

            }

         }
         catch(MessageException mex){

            ack = NPACConstants.NACK_RESPONSE;

         }

      }

      return ack;

   }

   /**
   * If the recovery complete request was successful,
   * this flags the Association as connected.
   * If the request failed, then this retries the association.
   */
   public void run(){

      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

         Debug.log(Debug.MSG_STATUS,
                   "Recovery for session ["+session.getSessionID()+
                   "] in region ["+region+"] completed: ["+
                   recoveryComplete+"]");

      }

      if(recoveryComplete){

         // flag the association as up and ready
         session.setAssociationConnected( region );

         Connection conn =null;
         
         String connectivityKey = null;
         
         PreparedStatement ps=null;
         try
         {
        	   if(spidValue != null){
					connectivityKey = NPACQueueUtils.getConnectivityKey(spidValue);
					if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

				         Debug.log(Debug.MSG_STATUS,
								"Recovery completed for spid: " + spidValue
										+ " and connectivity :"
										+ connectivityKey + " and region :"
										+ region + "");
				         
				      }
				} 
	            conn = DBInterface.acquireConnection();

	            ps=conn.prepareStatement(SOAQueryConstants.REGION_RECOVERY_UPDATE);

	            ps.setString(1,SOAQueryConstants.NPACCONNECTED_STATUS);

	            ps.setInt(2,region);
	            
	            if(connectivityKey != null){
						ps.setString(3,connectivityKey);
	            }
				else{
						ps.setNull(3, java.sql.Types.VARCHAR);
				}
				
				// update SOA_REGION_RECOVERY table with connected status.
	            ps.executeUpdate();
	            
	            if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS, "Executed update query for connected status  :["
							+ SOAQueryConstants.REGION_RECOVERY_UPDATE + "]"
							+ " for region [" + region
							+ "] and connectivity :[" + connectivityKey + "]");
				}

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

       }
      else{

         // We couldn't complete the recovery for some reason.
         // Retry the current Association's connection.
         adapter.retryAssociationRequest(session, region);

      }

      if(Debug.isLevelEnabled(Debug.MSG_STATUS)){

         Debug.log(Debug.MSG_STATUS,
                   "Session status: " +
                   session.toString());
      }

   }

   /**
   * This is called when a GatewayError is received in response to
   * our RecoveryCompleteRequest. This creates a retry attempt to
   * resend the recovery complete request.
   *
   */
   protected void handleError(XMLMessageParser error){

      // Retry the current Association's connection.
      adapter.retryAssociationRequest(session, region);


   }

   public void timeout(){

      // resend the request
      adapter.sendRecoveryCompleteRequest(session, region);

   }

}

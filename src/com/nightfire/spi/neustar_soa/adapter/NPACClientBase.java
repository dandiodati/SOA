///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Date;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.common.xml.XMLMessageBase;

import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;

/**
* This is base client class used to send messages to the NPAC's OSS Gateway.
* This class also takes care of generating the outgoing XML messages in the
* correct format. Methods are provided that will generate the appropriate
* XML message based on given input parameters.
*/
public abstract class NPACClientBase {


    /**
    * This calls generate() on the given generator and sends that
    * XML to the OSS Gateway.
    *
    * @throws MessageException if the XML generation fails for some
    *                          bizarre reason.
    */
    protected int send( String spid, XMLMessageGenerator xml )
                                 throws MessageException{

       return send( spid, xml.generate() );

    }


    public abstract int send( String spid, String xml );

    /**
    * This should release any resources the client is using. This should
    * only be called when the client is being shut down.
    */
    public abstract void cleanup();

    /**
    * This sends a NewSession request. This creates a new session
    * for the given primary SPID.
    *
    */
    public int sendNewSession(String primarySPID,
                              String invokeID,
                              String userID){

       int ack = NPACConstants.NACK_RESPONSE;

       try{

          XMLMessageGenerator generator =
             getGenerator(NPACConstants.UNINITIALIZED_SESSION_ID,
                          invokeID,
                          primarySPID);

          generator.setTextValue(NPACConstants.PRIMARY_SPID,
                                 primarySPID);

          generator.setTextValue(NPACConstants.USER_ID,
                                 userID);
         
          ack = send( primarySPID, generator );
          


       }
       catch(MessageException mex){

          Debug.error("Could not generate new session request: "+
                      mex);

       }

       return ack;

    }

    /**
    * This sends an AssociationRequest with a request type of "connect".
    * This creates an Association in recovery mode for the given session.
    *
    */
    public int sendAssociationConnectRequest(String sessionID,
                                             String invokeID,
                                             int region,
                                             String primarySPID ){

       return sendAssociationRequest(sessionID,
                                     invokeID,
                                     region,
                                     primarySPID,
                                     NPACConstants.CONNECT_REQUEST_TYPE,
                                     true);

    }

    /**
    * This sends an AssociationRequest.
    * 
    * @param sessionID session involved.
    * @param invokeID invokeId involved in the request.
    * @param primarySPID spid involved in the request.
    * @param region region involved in the request.
    * @param requestType type of the request
    * @param recoveryStatus recovery status mode.
    */
    private int sendAssociationRequest(String  sessionID,
                                       String  invokeID,
                                       int     region,
                                       String  primarySPID,
                                       String  requestType,
                                       boolean recoveryStatus){

       int ack = NPACConstants.NACK_RESPONSE;

       try{

          XMLMessageGenerator generator = getGenerator(sessionID,
                                                       invokeID,
                                                       primarySPID,
                                                       Integer.toString(region));

          generator.setTextValue(NPACConstants.ASSOCIATION_REQUEST_TYPE,
                                 requestType);

          generator.setTextValue(NPACConstants.REQUEST_RECOVERY_MODE,
                                 ( recoveryStatus ? "1" : "0" ) );

          ack = send( primarySPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate association request: "+
                      mex);

       }

       return ack;

    }
    
   /**
    * This sends a SWIM DownloadRecoveryNetworkRequest for the given time 
    * range in the current region.
    * @param sessionID session involved in the request
    * @param invokeID invokeId involved in the request.
    * @param region region involved in the request.
    * @param primarySPID spid involved in the request.
    * @param startTime startTime in the request.
    * @param stopTime stop time in the request.
    * @param actionID swim actionID in the request.
    * @param isTimeRangeRequest depends on the value generates time range. 
    * @return int 
    */
    public int sendDownloadRecoveryNetworkDataRequest
                                    ( String sessionID,
                                      String invokeID,
                                      int region,
                                      String primarySPID,
                                      Date startTime,
                                      Date stopTime,
                                      String actionID,
                                      boolean isTimeRangeRequest){

       int ack = NPACConstants.NACK_RESPONSE;
       


       try{

          XMLMessageGenerator generator =
                                   getGenerator(sessionID,
                                                invokeID,
                                                primarySPID,
                                                Integer.toString(region) );
          if(isTimeRangeRequest)
          {
         

          String startTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  startTime);

          String stopTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  stopTime);

         generator.setTextValue(NPACConstants.DOWNLOAD_RECOVERY_START_TIME,
                                startTimeString);
          

         generator.setTextValue(NPACConstants.DOWNLOAD_RECOVERY_STOP_TIME,
                               stopTimeString);
          
          }
          if(actionID !=null && !actionID.equals("")){
              generator.setTextValue
               (NPACConstants.DOWNLOAD_RECOVERY_NET_WORK_DATA_SWIM_ACTION_ID,
                actionID);
          }else if(!isTimeRangeRequest)
          {
              generator.create(NPACConstants.DOWNLOAD_RECOVERY_REQUEST+".swim");
              
          }
          

          // get data for all service providers
          generator.create( NPACConstants.DOWNLOAD_RECOVERY_ALL_PROVIDERS );
          
         
          // get all network data
          generator.create( NPACConstants.DOWNLOAD_RECOVERY_ALL_NETWORK_DATA );
         
          ack = send( primarySPID, generator );


       }
       catch(MessageException mex){

          Debug.error("Could not generate download recovery request: "+
                      mex);

       }

       return ack;

    }


    /**
     * This sends a SWIM DownloadRecovery service provider request for the 
     * given time range in the current region.
     * @param sessionID session involved in the request
     * @param invokeID invokeId involved in the request.
     * @param region region involved in the request.
     * @param primarySPID spid involved in the request.
     * @param startTime startTime in the request.
     * @param stopTime stop time in the request.
     * @param actionID swim actionID in the request.
     * @param isTimeRangeRequest depends on the value generates time range. 
     * @return int 
     */
    public int sendDownloadRecoveryServiceProvRequest(String sessionID,
                                                   String invokeID,
                                                   int region,
                                                   String primarySPID,
                                                   Date startTime,
                                                   Date stopTime,
                                                   String actionID,
                                                   boolean isTimeRangeRequest){

       int ack = NPACConstants.NACK_RESPONSE;
      


       try{

          XMLMessageGenerator generator =
                                   getGenerator(sessionID,
                                                invokeID,
                                                primarySPID,
                                                Integer.toString(region) );
          
          if(isTimeRangeRequest)
          {
               String startTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  startTime);
          

          String stopTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  stopTime);
          
          generator.setTextValue
          (NPACConstants.DOWNLOAD_RECOVERY_SERVICE_PROV_START_TIME, 
                  startTimeString);
          
          generator.setTextValue
          (NPACConstants.DOWNLOAD_RECOVERY_SERVICE_PROV_STOP_TIME, 
                  stopTimeString);
          }
          

          // get data for all service providers

          
          generator.create(NPACConstants.DOWNLOAD_RECOVERY_ALL_SERVICE_PROV_DATA);
          // get all network data
          
          if(actionID != null && !actionID.equals("")){
              
              generator.setTextValue
              (NPACConstants.DOWNLOAD_RECOVERY_SERVICE_PROV_SWIM_ACTION_ID,
                      actionID);
              
          }else if(!isTimeRangeRequest)
          {
              generator.create
              (NPACConstants. DOWNLOAD_RECOVERY_SERVICE_PROV_REQUEST+".swim");
              
          }
          
         
          
          ack = send( primarySPID, generator );                   
              
         

       }
       catch(MessageException mex){

          Debug.error("Could not generate download recovery request: "+
                      mex);

       }

       return ack;

    }
    
    /**
     * This sends a SWIM processing revocery result request 
     * given time  in the current region.
     * @param sessionID session involved in the request
     * @param invokeID invokeId involved in the request.
     * @param region region involved in the request.
     * @param spid spid involved in the request.
     * @param startTime startTime in the request.
     * @param swimActionID swim actionID in the request.
     * @return int 
     */
    
    public int sendSwimRequest(String sessionID, String invokeID, 
                               int region,Date startTime, String spid,
                               String swimActionID) {
        
        int ack = NPACConstants.NACK_RESPONSE;

        try {

            XMLMessageGenerator generator = getGenerator(sessionID, invokeID,
                    spid, Integer.toString(region));
        
            String startTimeString = TimeZoneUtil.convert(NPACConstants.UTC,
                    NPACConstants.UTC_TIME_FORMAT, startTime);
            if(swimActionID != null && !swimActionID.equals(""))
            {
        
            generator.setTextValue(
                    NPACConstants.SWIM_PROCESSING_REQUEST_ACTION_ID,
                    swimActionID);
            }
            else
            {
                generator.create(
                        NPACConstants.SWIM_PROCESSING_REQUEST_ACTION_ID );
                
            }

            generator.setTextValue(
                    NPACConstants.SWIM_PROCESSING_REQUEST_STATUS, 
                    NPACConstants.SUCCESS_STATUS);

            generator.setTextValue(
                    NPACConstants.SWIM_PROCESSING_REQUEST_TIME_OF_COMPLETION,
                    startTimeString);
          
            ack = send(spid, generator);


        } catch (MessageException mex) {

            Debug.error("Could not generate download recovery request: " + mex);

        }

        return ack;
    }

	/**
     * This sends a DownloadRecoveryRequest for the given time range in
     * the current region.
     *
     * @param sessionID String
     * @param invokeID String
     * @param region int
     * @param startTime Date
     * @param stopTime Date
     * @return int
     */
    public int sendTimeBasedDownloadRecoveryRequest(String sessionID,
                                           String invokeID,
                                           int region,
                                           String primarySPID,
                                           Date startTime,
                                           Date stopTime){

       int ack = NPACConstants.NACK_RESPONSE;


       try{

          XMLMessageGenerator generator =
                                   getGenerator(sessionID,
                                                invokeID,
                                                primarySPID,
                                                Integer.toString(region) );

          String startTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  startTime);

          String stopTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  stopTime);

          generator.setTextValue(NPACConstants.DOWNLOAD_RECOVERY_START_TIME,
                                 startTimeString);

          generator.setTextValue(NPACConstants.DOWNLOAD_RECOVERY_STOP_TIME,
                                 stopTimeString);

          // get data for all service providers
          generator.create( NPACConstants.DOWNLOAD_RECOVERY_ALL_PROVIDERS );

          // get all network data
          generator.create( NPACConstants.DOWNLOAD_RECOVERY_ALL_NETWORK_DATA );

          ack = send( primarySPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate TimeBased download recovery request: "+
                      mex);

       }

       return ack;

    }

    /**
     * This sends a RecoveryRequest for a customer in a particular region for a
     * particular time frame. Recovery requests are limited to a maximum query
     * time of one hour. The start and stop times appear to be inclusive.
     * 
     * @param session
     *            the session involved.
     * @param invokeID
     *            the invoke ID for the request.
     * @param region
     *            the region that is being queried.
     * @param customerSPID
     *            the customer ID for which we are recovering.
     * @param startTime
     *            the start time for the query window.
     * @param stopTime
     *            the stop time for the query window.
     * @param swimActionID
     *            the action Id in the Recovery request.
     * @param isTimeRangeRequest
     *          is the request is time range requst.                       
     * return int
     */
    public int sendRecoveryRequest(String sessionID,
                                   String invokeID,
                                   int region,
                                   String customerSPID,
                                   Date startTime,
                                   Date stopTime,
                                   String swimActionID,
                                   boolean isTimeRangeRequest
                                   ){

       int ack = NPACConstants.NACK_RESPONSE;
       //swimActionId ="0";

       try{

          XMLMessageGenerator generator =
                                   getGenerator(sessionID,
                                                invokeID,
                                                customerSPID,
                                                Integer.toString(region) );
          
        

          String startTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  startTime);

          String stopTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  stopTime);
          if( isTimeRangeRequest){

          generator.setTextValue(NPACConstants.RECOVERY_START_TIME,
                                 startTimeString);
        

          generator.setTextValue(NPACConstants.RECOVERY_STOP_TIME,
                                 stopTimeString);
          }
          if((swimActionID == null || swimActionID.equals("")) && (!isTimeRangeRequest) ){
              generator.create(NPACConstants.RECOVERYREQUEST_SWIM);
             
         }
          else if(!isTimeRangeRequest)
          {
              generator.setTextValue
              (NPACConstants.RECOVERY_SWIM_ACTIOID, swimActionID);
          }     
          ack = send( customerSPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate recovery request: "+
                      mex);

       }

       return ack;

    }

	/**
    * This sends a RecoveryRequest for a customer in a particular region
    * for a particular time frame. Recovery requests are limited to
    * a maximum query time of one hour. The start and stop times
    * appear to be inclusive.
    *
    * @param session the session involved.
    * @param invokeID the invoke ID for the request.
    * @param region the region that is being queried.
    * @param customerSPID the customer ID for which we are recovering.
    * @param startTime the start time for the query window.
    * @param stopTime the stop time for the query window.
    *
    */
    public int sendTimeBasedRecoveryRequest(String sessionID,
                                   String invokeID,
                                   int region,
                                   String customerSPID,
                                   Date startTime,
                                   Date stopTime){

       int ack = NPACConstants.NACK_RESPONSE;

       try{

          XMLMessageGenerator generator =
                                   getGenerator(sessionID,
                                                invokeID,
                                                customerSPID,
                                                Integer.toString(region) );

          String startTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  startTime);

          String stopTimeString =
             TimeZoneUtil.convert(NPACConstants.UTC,
                                  NPACConstants.UTC_TIME_FORMAT,
                                  stopTime);

          generator.setTextValue(NPACConstants.RECOVERY_START_TIME,
                                 startTimeString);

          generator.setTextValue(NPACConstants.RECOVERY_STOP_TIME,
                                 stopTimeString);

          ack = send( customerSPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate TimeBased recovery request: "+
                      mex);

       }

       return ack;

    }

    /**
    * This sends a RecoveryComplete message letting the
    * OSS Gateway know that recovery is complete for a session in a particular
    * region.
    */
    public int sendRecoveryComplete(String sessionID,
                                    String invokeID,
                                    int region,
                                    String primarySPID){

       int ack = NPACConstants.NACK_RESPONSE;

       try{

          XMLMessageGenerator generator = getGenerator(sessionID,
                                                       invokeID,
                                                       primarySPID,
                                                       Integer.toString(region));

          generator.setTextValue(NPACConstants.NPAC_REGION_ID,
                                 Integer.toString(region));

          generator.create(NPACConstants.RECOVERY_COMPLETE_REQUEST);

          ack = send( primarySPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate recovery request: "+
                      mex);

       }

       return ack;


    }

    /**
    * This sends a ClientKeepAlive message. ClientKeepAlive messages
    * must be sent periodically for each active session to let
    * the OSS Gateway know that we are still alive.
    */
    public int sendKeepAlive(String sessionID,
                             int sequenceNumber,
                             String invokeID,
                             String primarySPID){

       if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

          Debug.log(Debug.IO_STATUS,
                    "Sending client keep alive number ["+sequenceNumber+
                    "] for session ID ["+sessionID+"]");

       }

       int ack = NPACConstants.NACK_RESPONSE;

       try{

          XMLMessageGenerator generator = getGenerator(sessionID,
                                                       invokeID,
                                                       primarySPID);

          generator.setTextValue(NPACConstants.CLIENT_SEQUENCE_NUMBER,
                                 Integer.toString( sequenceNumber ) );

          ack = send( primarySPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate keep alive request: "+mex);

       }

       return ack;

    }

    /**
    * This sends a ClientReleaseSession notification. This is used
    * during shutdown to gracefully clean up any active sessions.
    */
    public int sendClientReleaseSession(String sessionID,
                                        String invokeID,
                                        String primarySPID){

       int ack = NPACConstants.NACK_RESPONSE;

       try{

          XMLMessageGenerator generator = getGenerator(sessionID,
                                                       invokeID,
                                                       primarySPID);

          generator.create(NPACConstants.CLIENT_RELEASE_SESSION);

          ack = send( primarySPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate client release session: "+
                      mex);

       }

       return ack;

    }

    /**
    * This is used to send an asynchronous NotificationReply to notifications
    * that were initiated by the OSS Gateway.
    *
    * @param sessionID the session involved.
    * @param invokeID the invoke ID for the reply.
    * @param customerSPID the SPID involved.
    * @param replyStatus currently either "success", "failed", or
    *                    "invalid-session".
    * @param errorCode the optional error code that can be sent with failed
    *                  messages.
    * @param errorInfo the optional error message that can be sent with
    *                  failed messages. Error info can only be sent if there
    *                  is an error code.
    */
    public int sendNotificationReply(String sessionID,
                                     String invokeID,
                                     String customerSPID,
                                     String replyStatus,
                                     String errorCode,
                                     String errorInfo ){

       return sendNotificationReply(sessionID,
                                    invokeID,
                                    customerSPID,
                                    replyStatus,
                                    errorCode,
                                    errorInfo,
                                    NPACConstants.NOTIFICATION_REPLY_STATUS);

    }

    /**
    * This is used to send an asynchronous DownloadReply to notifications
    * that were initiated by the OSS Gateway.
    *
    * @param sessionID the session involved.
    * @param invokeID the invoke ID for the reply.
    * @param customerSPID the SPID involved.
    * @param replyStatus currently either "success", "failed", or
    *                    "invalid-session".
    * @param errorCode the optional error code that can be sent with failed
    *                  messages.
    * @param errorInfo the optional error message that can be sent with
    *                  failed messages. Error info can only be sent if there
    *                  is an error code.
    */
    public int sendDownloadReply(String sessionID,
                                 String invokeID,
                                 String customerSPID,
                                 String replyStatus,
                                 String errorCode,
                                 String errorInfo ){

       return sendNotificationReply(sessionID,
                                    invokeID,
                                    customerSPID,
                                    replyStatus,
                                    errorCode,
                                    errorInfo,
                                    NPACConstants.DOWNLOAD_REPLY_STATUS);

    }


    /**
    * This is used to send an asynchronous reply to notifications
    * that were initiated by the OSS Gateway.
    *
    * @param sessionID the session involved.
    * @param invokeID the invoke ID for the reply.
    * @param customerSPID the SPID involved.
    * @param replyStatus currently either "success", "failed", or
    *                    "invalid-session".
    * @param errorCode the optional error code that can be sent with failed
    *                  messages.
    * @param errorInfo the optional error message that can be sent with
    *                  failed messages. Error info can only be sent if there
    *                  is an error code.
    */
    public int sendNotificationReply(String sessionID,
                                     String invokeID,
                                     String customerSPID,
                                     String replyStatus,
                                     String errorCode,
                                     String errorInfo,
                                     String statusPath  ){


       if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

          Debug.log(Debug.IO_STATUS,
                    "Sending notification reply for invoke ID ["+
                    invokeID+"] with status ["+
                    replyStatus+"]");

       }

       int ack = NPACConstants.NACK_RESPONSE;

       try{

          XMLMessageGenerator generator = getGenerator(sessionID,
                                                       invokeID,
                                                       customerSPID);

          generator.setTextValue(statusPath, replyStatus);

          // error code is optional
          if( errorCode != null ){

             generator.setTextValue(NPACConstants.NOTIFICATION_REPLY_ERROR_CODE,
                                    errorCode);

             // error info is optional and will only be found if the error code
             // is present
             if( errorInfo != null ){

                generator.setTextValue(NPACConstants.NOTIFICATION_REPLY_ERROR_INFO,
                                       errorInfo);

             }

          }

          ack = send( customerSPID, generator );

       }
       catch(MessageException mex){

          Debug.error("Could not generate notification reply: "+
                      mex);

       }

       return ack;

    }

    /**
    * Gets the current UTC date and time in the date format expected
    * by the NPAC gateway.
    */
    public static String getCurrentDateTime(){

       return TimeZoneUtil.getCurrentTime(NPACConstants.UTC,
                                          NPACConstants.UTC_TIME_FORMAT);

    }

    /**
    * This is the version of the getGeneratorMethod() to be used
    * when creating requests that don't apply to a particular region.
    *
    * @param sessionID the session ID to be set in the header XML.
    * @param invokeID the invoke ID.
    * @param customerID the SPID that will be put in the header. This
    *                   is an optional field, if the customer ID is null,
    *                   then this field will not get populated in the XML.
    */
    protected static XMLMessageGenerator getGenerator(String sessionID,
                                                      String invokeID,
                                                      String customerID)
                                                      throws MessageException{

       return getGenerator(sessionID, invokeID, customerID, null);

    }

    /**
    * This creates an XML generator prepopulated with the given request
    * header information. This also populates the current
    * date/time into the header as the message's send date.
    *
    * @param sessionID the session ID to be set in the header XML.
    * @param invokeID the invoke ID.
    * @param customerID the SPID that will be put in the header. This
    *                   is an optional field, if the customer ID is null,
    *                   then this field will not get populated in the XML.
    * @param region     the region that will be put in the header. This
    *                   is an optional field, if the customer ID is null,
    *                   then this field will not get populated in the XML.
    */
    protected static XMLMessageGenerator getGenerator(String sessionID,
                                                      String invokeID,
                                                      String customerID,
                                                      String region)
                                                      throws MessageException{

       XMLMessageGenerator generator =
          new XMLMessageGenerator(NPACConstants.ROOT_NODE);

       Document doc = generator.getDocument();
       Element rootElement = doc.getDocumentElement();

       XMLMessageBase.setNodeAttributeValue(rootElement,
                                            NPACConstants.XMLNS_ATTR,
                                            NPACConstants.NAMESPACE);

       // create the message header (order is important)

       generator.setTextValue(NPACConstants.SESSION_ID, sessionID);

       // insert the invoke ID into the request header
       generator.setTextValue(NPACConstants.INVOKE_ID, invokeID);

       if(region != null){

          generator.setTextValue( NPACConstants.NPAC_REGION_ID, region );

       }

       if(customerID != null){

          generator.setTextValue(NPACConstants.CUSTOMER_ID,
                                 customerID);

       }

       // get current time in the UTC format expected by the gateway
       generator.setTextValue(NPACConstants.MESSAGE_DATE_TIME,
                              getCurrentDateTime());

       return generator;

    }
    
    

	
}


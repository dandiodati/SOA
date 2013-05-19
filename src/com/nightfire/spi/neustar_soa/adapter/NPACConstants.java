///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

/**
* This file contains various constants used throughout the this adapter.
*/
public interface NPACConstants{

   /**
   * The time format for universal time.
   */
   public static final String UTC_TIME_FORMAT =  "yyyy-MM-dd'T'HH:mm:ss'Z'";

   /**
   * String used to get the UTC/GMT time zone.
   */
   public static final String UTC = "UTC";

   /**
   * The number of regions defined.
   *
   */
   public static final int REGION_COUNT = 8;

   /**
   * This value is returned when a SOA Message was received successfully.
   */
   public static final int ACK_RESPONSE = 0;

   /**
   * This value is returned when a SOA Message fails.
   */
   public static final int NACK_RESPONSE = -1;

   /**
   * The value used in the NPAC XML to represent "true".
   */
   public static final String XBOOL_TRUE = "1";

   /**
   * The value used in the NPAC XML to represent "false".
   */
   public static final String XBOOL_FALSE = "0";

   /**
   * Sessions that have not yet received their NewSessionReply have
   * a session ID of "0".
   */
   public static final String UNINITIALIZED_SESSION_ID = "0";

   /**
   * This is the name of the remote method that will be invoke when
   * sending a SOA message.
   */
   public static final String SEND_OPERATION_NAME = "sendmsg";

   /**
   * The name of the XML namespace used in SOA XML messages.
   */
   public static final String NAMESPACE = "urn:neustar:lnp:soa:1.0";

   /**
   * The XML schema expects this attribute to be set on the root
   * element.
   */
   public static final String XMLNS_ATTR = "xmlns";

   /**
   * This is the name used for the string parameter that will
   * get passed to the send operation. From what I can tell, SOAP
   * does not care what the parameter is called, as long as it is set
   * on the Call.
   */
   public static final String SEND_PARAMETER_NAME = "message";

   /**
    * Message names ending with this string are notification
    * messages and require the SOA to send an asynchronous
    * reply back to the NPAC Gateway.
    */
   public static final String NOTIFICATION_SUFFIX = "Notification";

   // Various message types. These correspond to node names in the
   // OSS Gateway's XML Schema.

   public static final String NEW_SESSION_REPLY_TYPE = "NewSessionReply";

   public static final String ASSOCIATION_REPLY_TYPE = "AssociationReply";

   public static final String RECOVERY_COMPLETE_TYPE = "RecoveryCompleteReply";
   
   public static final String RECOVERY_COMPLETE_WITH_ERROR_CODE_TYPE = "RecoveryCompleteReplyWithErrorCode";

   public static final String RECOVERY_REPLY_TYPE = "RecoveryReply";

   public static final String ASSOCIATION_STATUS_NOTIFICATION_TYPE =
                                                "AssociationStatusNotification";

   public static final String GATEWAY_ERROR_TYPE = "GatewayError";

   public static final String GATEWAY_KEEP_ALIVE_TYPE = "GatewayKeepAlive";

   public static final String GATEWAY_RELEASE_TYPE = "GatewayReleaseSession";

   public static final String VERSION_OBJECT_CREATION_TYPE =
                          "VersionObjectCreationNotification";

   public static final String STATUS_ATTRIBUTE_CHANGE_TYPE =
                          "VersionStatusAttributeValueChangeNotification";

   public static final String ATTRIBUTE_CHANGE_TYPE =
                          "VersionAttributeValueChangeNotification";

   public static final String CANCELLATION_ACK_TYPE =
      "VersionCancellationAcknowledgeRequestNotification";

   public static final String NEW_SP_CREATE_TYPE =
      "VersionNewSP_CreateRequestNotification";

   public static final String OLD_SP_CREATE_TYPE =
      "VersionOldSP_ConcurrenceRequestNotification";

   public static final String CUST_DISCONNECT_DATE_TYPE =
      "VersionCustomerDisconnectDateInfoNotification";

   public static final String NEW_SP_FINAL_TYPE =
      "VersionNewSPFinalCreateWindowExpirationNotification";

   public static final String OLD_SP_FINAL_TYPE =
      "VersionOldSPFinalConcurrenceWindowExpirationNotification";

   public static final String NPAC_OPERATIONAL_TYPE =
      "LnpNPAC_SMS_OperationalInformationNotification";

   public static final String NEW_NPA_NXX_TYPE =
                          "VersionNewNPA_NXXNotification";

   public static final String NEW_SP_CREATE_REPLY_TYPE = "NewSP_CreateReply";

   public static final String MODIFY_ACTION_REPLY_TYPE = "ModifyActionReply";

   public static final String CANCEL_ACTION_REPLY_TYPE = "CancelActionReply";

   public static final String ACTIVATE_ACTION_REPLY_TYPE =
                                 "ActivateActionReply";

   public static final String NPA_NXX_CREATE_TYPE = "ServiceProvNPA_NXX_Create";

   public static final String NPA_NXX_DELETE_TYPE = "ServiceProvNPA_NXX_Delete";

   public static final String LRN_CREATE_TYPE = "ServiceProvLRNCreate";

   public static final String LRN_DELETE_TYPE = "ServiceProvLRNDelete";

   public static final String SP_CREATE_TYPE = "ServiceProvNetworkCreate";

   public static final String SP_MODIFY_TYPE = "ServiceProvNetworkModify";

   public static final String SP_DELETE_TYPE = "ServiceProvNetworkDelete";

   public static final String NPA_NXX_X_CREATE_TYPE =
                                 "ServiceProvNPA_NXX_X_Create";

   public static final String NPA_NXX_X_MODIFY_TYPE =
                                 "ServiceProvNPA_NXX_X_Modify";

   public static final String NPA_NXX_X_DELETE_TYPE =
                                 "ServiceProvNPA_NXX_X_Delete";

   public static final String ROOT_NODE = "SOAMessage";

   // paths for accessing the SOA Message header

   public static final String HEADER = "messageHeader";

   public static final String SESSION_ID = HEADER+".session_id";

   public static final String INVOKE_ID = HEADER+".invoke_id";

   public static final String NPAC_REGION_ID = HEADER+".npac_region_id";

   public static final String CUSTOMER_ID = HEADER+".customer_id";

   public static final String MESSAGE_DATE_TIME = HEADER+".message_date_time";

   // paths for accessing the SOA Message body

   public static final String CONTENT = "messageContent";

   public static final String NPAC_TO_SOA = CONTENT+".NPACtoSOA";

   public static final String SOA_TO_NPAC = CONTENT+".SOAtoNPAC";

   public static final String NEW_SESSION = SOA_TO_NPAC+".NewSession";

   public static final String CLIENT_KEEP_ALIVE = SOA_TO_NPAC+
                                                  ".ClientKeepAlive";

   public static final String CLIENT_SEQUENCE_NUMBER = CLIENT_KEEP_ALIVE+
                                                       ".sequence_number";

   public static final String USER_ID = NEW_SESSION+".user_id";

   public static final String PRIMARY_SPID = NEW_SESSION+".primary_spid";

   public static final String NEW_SESSION_REPLY = NPAC_TO_SOA+"."+
                                                  NEW_SESSION_REPLY_TYPE;

   public static final String NEW_SESSION_STATUS = NEW_SESSION_REPLY+
                                                   ".session_status";
   public static final String SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_NOTIFICATION= 
       NPAC_TO_SOA+".SwimProcessingRecoveryResultsReply";
   
   public static final String SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_STATUS=
       SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_NOTIFICATION+".status";
   
   public static final String SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_STOPTIME = 
       SWIM_PROCESSING_RECOVERY_RESULTS_REPLY_NOTIFICATION+".stop_date";

   public static final String NEW_SESSION_ID = NEW_SESSION_REPLY+
                                               ".session_id";

   public static final String ASSOCIATION_REQUEST = SOA_TO_NPAC+
                                                    ".AssociationRequest";

   public static final String ASSOCIATION_REQUEST_TYPE = ASSOCIATION_REQUEST+
                                                    ".association_request_type";

   public static final String ASSOCIATION_REPLY = NPAC_TO_SOA+
                                                  "."+ASSOCIATION_REPLY_TYPE;

   public static final String ASSOCIATION_REPLY_STATUS = ASSOCIATION_REPLY+
                                                         ".association_status";

   public static final String ASSOCIATION_STATUS_NOTIFICATION =
                                           NPAC_TO_SOA+
                                           "."+
                                           ASSOCIATION_STATUS_NOTIFICATION_TYPE;

   public static final String ASSOCIATION_STATUS =
                                 ASSOCIATION_STATUS_NOTIFICATION+
                                 ".association_status";

   public static final String ASSOCIATION_ERROR_CODE =
                                 ASSOCIATION_STATUS_NOTIFICATION+
                                 ".error_reason.error_code";

   public static final String ASSOCIATION_ERROR_INFO =
                                 ASSOCIATION_STATUS_NOTIFICATION+
                                 ".error_reason.error_info";

   public static final String REQUEST_RECOVERY_MODE = ASSOCIATION_REQUEST+
                                                      ".recovery_mode";

   public static final String RECOVERY_REQUEST_TIME_RANGE =
                                    SOA_TO_NPAC+".RecoveryRequest.time_range";

   public static final String RECOVERY_START_TIME =
                                 RECOVERY_REQUEST_TIME_RANGE+".start_time";

   public static final String RECOVERY_STOP_TIME =
                                 RECOVERY_REQUEST_TIME_RANGE+".stop_time";
   public static final String RECOVERYREQUEST_SWIM = SOA_TO_NPAC+".RecoveryRequest.swim";
   
   public static final String RECOVERY_SWIM_ACTIOID = RECOVERYREQUEST_SWIM+".action_id";

   public static final String RECOVERY_REPLY_STATUS =  NPAC_TO_SOA+
                                                       "."+
                                                       RECOVERY_REPLY_TYPE+
                                                       ".recovery_reply_status";

   public static final String DOWNLOAD_RECOVERY_REPLY_TYPE =
                                 "DownloadRecoveryReply";
   public static final String RECOVERY_REPLY ="RecoveryReply";
   
   public static final String SWIM_PROCESSING_RECOVERY_RESULT_REPLY =
                                "SwimProcessingRecoveryResultsReply";

   public static final String DOWNLOAD_RECOVERY_REPLY_STATUS =
                                                       NPAC_TO_SOA+
                                                       "."+
                                                       DOWNLOAD_RECOVERY_REPLY_TYPE+
                                                       ".status";
   public static final String DOWNLOAD_RECOVERY_REPLY_ACTION_ID =
       NPAC_TO_SOA+"."+DOWNLOAD_RECOVERY_REPLY_TYPE+".action_id";
   
   public static final String RECOVERY_REPLY_ACTION_ID =NPAC_TO_SOA+"."+RECOVERY_REPLY+".action_id";

   public static final String RECOVERY_COMPLETE_REQUEST =
                                 SOA_TO_NPAC+".RecoveryCompleteRequest";

   public static final String RECOVERY_COMPLETE_REPLY =
                                 NPAC_TO_SOA+".RecoveryCompleteReply";
   
   public static final String RECOVERY_COMPLETE_REPLY_ERROR_CODE =
       NPAC_TO_SOA+".RecoveryCompleteReplyWithErrorCode.status";

   public static final String DOWNLOAD_RECOVERY_REQUEST =
                                 SOA_TO_NPAC+
                                 ".DownloadRecoveryRequest.network_download";
   public static final String DOWNLOAD_RECOVERY_SERVICE_PROV_REQUEST=SOA_TO_NPAC+
   ".DownloadRecoveryRequest.service_prov_download";

   public static final String DOWNLOAD_RECOVERY_TIME_RANGE =
                                 DOWNLOAD_RECOVERY_REQUEST+".time_range";
   public static final String DOWNLOAD_RECOVERY_SERVICE_PROV_TIME_RANGE = 
       DOWNLOAD_RECOVERY_SERVICE_PROV_REQUEST+".time_range"; 
   public static final String DOWNLOAD_RECOVERY_SERVICE_PROV_START_TIME = 
       DOWNLOAD_RECOVERY_SERVICE_PROV_TIME_RANGE+".start_time";

   public static final String DOWNLOAD_RECOVERY_START_TIME =
                                 DOWNLOAD_RECOVERY_TIME_RANGE+".start_time";
   
   public static final String DOWNLOAD_RECOVERY_SERVICE_PROV_STOP_TIME = 
       DOWNLOAD_RECOVERY_SERVICE_PROV_TIME_RANGE+".stop_time";

   public static final String DOWNLOAD_RECOVERY_STOP_TIME =
                                 DOWNLOAD_RECOVERY_TIME_RANGE+".stop_time";
   
   public static final String DOWNLOAD_RECOVERY_ALL_SERVICE_PROV_DATA = 
       DOWNLOAD_RECOVERY_SERVICE_PROV_REQUEST+".service_provs.all_service_provs";
   
   public static final String DOWNLOAD_RECOVERY_SERVICE_PROV_SWIM_ACTION_ID = 
       DOWNLOAD_RECOVERY_SERVICE_PROV_REQUEST+".swim.action_id";
   
   public static final String DOWNLOAD_RECOVERY_NET_WORK_DATA_SWIM_ACTION_ID = 
       DOWNLOAD_RECOVERY_REQUEST+".swim.action_id";
   
   public static final String SWIM_PROCESSING_RECOVERY_RESULT_REQUEST = 
       SOA_TO_NPAC+".SwimProcessingRecoveryResultsRequest";
   
   public static final String SWIM_PROCESSING_REQUEST_ACTION_ID = 
       SWIM_PROCESSING_RECOVERY_RESULT_REQUEST+".action_id";
   
   public static final String SWIM_PROCESSING_REQUEST_STATUS = 
       SWIM_PROCESSING_RECOVERY_RESULT_REQUEST+".status";
   
   public static final String SWIM_PROCESSING_REQUEST_TIME_OF_COMPLETION = 
       SWIM_PROCESSING_RECOVERY_RESULT_REQUEST+".time_of_completition";

   public static final String DOWNLOAD_RECOVERY_ALL_PROVIDERS =
                                 DOWNLOAD_RECOVERY_REQUEST+
                                 ".service_provs.all_service_provs";

   public static final String DOWNLOAD_RECOVERY_ALL_NETWORK_DATA =
                                 DOWNLOAD_RECOVERY_REQUEST+
                                 ".network_data.all_network_data";

   public static final String CLIENT_RELEASE_SESSION =
                                 SOA_TO_NPAC+".ClientReleaseSession";

   public static final String GATEWAY_KEEP_ALIVE_SEQNO =
                                  NPAC_TO_SOA+"."+
                                  GATEWAY_KEEP_ALIVE_TYPE+
                                  ".sequence_number";

   public static final String GATEWAY_ERROR_STATUS =
                                 NPAC_TO_SOA+"."+
                                 GATEWAY_ERROR_TYPE+
                                 ".error_status";

   public static final String GATEWAY_ERROR_REASON =
                                 NPAC_TO_SOA+"."+
                                 GATEWAY_ERROR_TYPE+
                                 ".error_reason";

   public static final String GATEWAY_ERROR_CODE =
                                 GATEWAY_ERROR_REASON+
                                 ".error_code";

   public static final String GATEWAY_ERROR_INFO =
                                 GATEWAY_ERROR_REASON+
                                 ".error_info";

   public static final String NOTIFICATION_REPLY =
                                 SOA_TO_NPAC+
                                 ".NotificationReply";

   public static final String NOTIFICATION_REPLY_STATUS =
                                  NOTIFICATION_REPLY+".status";

   public static final String NOTIFICATION_REPLY_ERROR_CODE =
                                  SOA_TO_NPAC+".0.error_reason.error_code";


   public static final String NOTIFICATION_REPLY_ERROR_INFO =
                                  SOA_TO_NPAC+".0.error_reason.error_info";

   public static final String DOWNLOAD_REPLY =
                                 SOA_TO_NPAC+
                                 ".DownloadReply";

   public static final String DOWNLOAD_REPLY_STATUS =
                                  DOWNLOAD_REPLY+".status";

   /**
   * The location of the XML node whose name will specifically identify
   * the type of notification. This is the first node under the
   * NPACtoSOA node.
   */
   public static final String NOTIFICATION_TYPE_NODE = NPAC_TO_SOA+".0";
   
   public static final String DOWNLOAD_RECOVERY_REPLY_NOTIFICATION_TYPE = 
       NPAC_TO_SOA+"."+DOWNLOAD_RECOVERY_REPLY_TYPE+".downloaddata"+".0";

   public static final String REPLY_ACTION_STATUS =
                                  NOTIFICATION_TYPE_NODE+".action_reply.status";

   public static final String ACTION_REPLY_STATUS = NOTIFICATION_TYPE_NODE+
                                                    ".status";

   public static final String SUCCESS_STATUS = "success";
   public static final String SESSION_INVALID_STATUS = "session-invalid";
   public static final String FAILED_STATUS = "failed";
   public static final String CONNECTED_STATUS = "connected";
   public static final String ABORTED_STATUS = "aborted";
   public static final String RELEASED_STATUS = "released";
   public static final String NO_DATA_SELECTED_STATUS = "no-data-selected";
   public static final String SWIM_MORE_DATA_STATUS ="swim-more-data";

   // Someone spelled "too" wrong in the XML Schema interface.
   public static final String CRITERIA_TOO_LARGE = "criteria-to-large";

   public static final String TIME_RANGE_INVALID = "time-range-invalid";

   public static final String CONNECT_REQUEST_TYPE = "connect";
   public static final String ABORT_REQUEST_TYPE   = "abort";
   public static final String RELEASE_REQUEST_TYPE = "release";

   public static final String PROCESSING_FAILURE_ERROR_CODE =
                                   "processingFailureEr";

   public static final String ASSOCIATION_IN_RECOVERY_ERROR_CODE =
                                   "RequestRejectedBecauseAssociationInRecovery";

   public static final String REGION_NOT_ASSOCIATED_ERROR_CODE =
                                   "RequestRejectedBecauseRegionNotAssociated";
   
   public static final String DOWNLOAD_DATA_NET_WORK_DATA="network_data";
   
   public static final String RECOVERY_REQUEST ="RecoveryRequest";
   
   public static final String DOWNLOAD_DATA_SERVICE_PROV_DATA = "service_prov_data";
   
   /**
    * The status array of NPAC association.
    */
   
   public static final boolean[] NPACASSOCIATION_STATUS_ARR =new boolean[10] ;
   

}

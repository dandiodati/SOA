///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.adapter.simulator;

import com.nightfire.spi.neustar_soa.adapter.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

/**
 * This class is used in generating simulated XML responses to the SOA gateway.
 * This allows the SOA gateway to be brought up and tested without the
 * benefit of a live OSS gateway with which to test.
 */
public class NPACReplyClient extends NPACClient {

    /**
     * This is a dummy session ID returned in the simulated NewSessionReply.
     */
    public static final String DEFAULT_SESSION_ID = "123456789";

    /**
     * Constructor.
     *
     */
    public NPACReplyClient(String project,
                           String subject,
                           String server)
                           throws FrameworkException{

       super(project, subject, null, null, server, null);

    }

    /**
     * This method sends a simulated NewSessionReply message back to the
     * SOA.
     *
     * @param invokeID String the invoke ID of the NewSession request. This
     *                        value is used as the invoke ID for the response.
     * @param primarySPID String The customer ID (SPID) given in the request
     *                           message. Also populated in the response
     * @return int the ACK/NACK value.
     */
    public int sendNewSessionReply(String invokeID,
                                   String primarySPID){

       int ack = NPACConstants.NACK_RESPONSE;

       try{

         XMLMessageGenerator reply = getGenerator("0",
                                                  invokeID,
                                                  primarySPID);

         // set session status as "success"
         reply.setTextValue(NPACConstants.NEW_SESSION_STATUS,
                            NPACConstants.SUCCESS_STATUS);

         // set new session id
         reply.setTextValue(NPACConstants.NEW_SESSION_ID,
                            getSessionID() );

         ack = send( primarySPID, reply );

      }
      catch(MessageException mex){

         Debug.error("Could not generate new session reply: "+
                     mex);

      }

      return ack;

    }

    /**
     * This methods generates a simulated Association Reply containing
     * the given values and sends this reply to this client's target URL.
     *
     * @param invokeID String the invoke ID given in the request. The generated
     *                        response must include this same invoke ID
     *                        so that the SOA can match it to the request.
     * @param regionID String the region to being associated. This value again
     *                        is given in the Association Request and must
     *                        be included in the reply.
     * @param primarySPID String This is the customer ID/SPID given in the
     *                           Association Request.
     * @return int the ACK/NACK value.
     */
    public int sendAssociationReply(String invokeID,
                                    String regionID,
                                    String primarySPID){

        int ack = NPACConstants.NACK_RESPONSE;

        try{

          XMLMessageGenerator reply = getGenerator(getSessionID(),
                                                   invokeID,
                                                   primarySPID,
                                                   regionID);

          // set association status to "connected"
          reply.setTextValue(NPACConstants.ASSOCIATION_REPLY_STATUS,
                             NPACConstants.CONNECTED_STATUS);


          // set recovery flag to 1 (true)
          reply.setTextValue(NPACConstants.ASSOCIATION_REPLY+
                             ".recovery_mode",
                             NPACConstants.XBOOL_TRUE );

          ack = send( primarySPID, reply );

        }
        catch(MessageException mex){

          Debug.error("Could not generate association reply: "+
                      mex);

        }


        return ack;

    }


    /**
     * This methods generates a simulated Recovery Reply containing
     * the given values and sends this reply to this client's target URL.
     * To simplify things, the generated Recovery Reply will always indicate
     * that no data was selected for the given Recovery Request query criteria.
     *
     * @param invokeID String the invoke ID given in the request. The generated
     *                        response must include this same invoke ID
     *                        so that the SOA can match it to the request.
     * @param regionID String the region of the recovery. This value again
     *                        is given in the Recovery Request and must
     *                        be included in the reply.
     * @param secondarySPID String This is the customer ID/SPID given in the
     *                           Recovery Request.
     * @return int the ACK/NACK value.
     */
    public int sendRecoveryReply(String invokeID,
                                 String regionID,
                                 String secondarySPID){

        int ack = NPACConstants.NACK_RESPONSE;

        try{

          XMLMessageGenerator reply = getGenerator(getSessionID(),
                                                   invokeID,
                                                   secondarySPID,
                                                   regionID);

          // reply with a status indicating that no data was selected
          reply.setTextValue(NPACConstants.RECOVERY_REPLY_STATUS,
                             NPACConstants.NO_DATA_SELECTED_STATUS);

          ack = send( secondarySPID, reply );

        }
        catch(MessageException mex){

          Debug.error("Could not generate recovery reply: "+
                      mex);

        }

        return ack;

    }

    /**
     * This methods generates a simulated DownloadecoveryReply containing
     * the given values and sends this reply to this client's target URL.
     * To simplify things, the generated DownloadRecoveryReply will always
     * indicate
     * that no data was selected for the given DownloadRecoveryRequest
     * query criteria.
     *
     * @param invokeID String the invoke ID given in the request. The generated
     *                        response must include this same invoke ID
     *                        so that the SOA can match it to the request.
     * @param regionID String the region of the recovery. This value again
     *                        is given in the Recovery Request and must
     *                        be included in the reply.
     * @param spid String the SPID to use in the response.
     * @return int the ACK/NACK value.
     */
    public int sendDownloadRecoveryReply(String invokeID,
                                         String regionID,
                                         String spid){

        int ack = NPACConstants.NACK_RESPONSE;

        try{

          XMLMessageGenerator reply = getGenerator(getSessionID(),
                                                   invokeID,
                                                   spid,
                                                   regionID);

          // reply with a status indicating that no data was selected
          reply.setTextValue(NPACConstants.DOWNLOAD_RECOVERY_REPLY_STATUS,
                             NPACConstants.NO_DATA_SELECTED_STATUS);

          ack = send( spid, reply );

        }
        catch(MessageException mex){

          Debug.error("Could not generate download recovery reply: "+
                      mex);

        }

        return ack;

    }

    /**
     * This methods generates a simulated Recovery Complete Reply containing
     * the given values and sends this reply to this client's target URL.
     *
     * @param invokeID String the invoke ID given in the request. The generated
     *                        response must include this same invoke ID
     *                        so that the SOA can match it to the request.
     * @param regionID String the region of the recovery. This value again
     *                        is given in the Recovery Complete Request and must
     *                        be included in the reply.
     * @param secondarySPID String This is the customer ID/SPID given in the
     *                           Recovery Complete Request.
     * @return int the ACK/NACK value.
     */
    public int sendRecoveryCompleteReply(String invokeID,
                                         String regionID,
                                         String secondarySPID){

        int ack = NPACConstants.NACK_RESPONSE;

        try{

          XMLMessageGenerator reply = getGenerator(getSessionID(),
                                                   invokeID,
                                                   secondarySPID,
                                                   regionID);

          // reply with a value of 1 (true)
          reply.setTextValue(NPACConstants.RECOVERY_COMPLETE_REPLY,
                             NPACConstants.XBOOL_TRUE);

          ack = send( secondarySPID, reply );

        }
        catch(MessageException mex){

          Debug.error("Could not generate recovery complete reply: "+
                      mex);

        }

        return ack;

    }

    /**
     * Returns a dummy session ID.
     *
     * @return String 123456789
     */
    protected String getSessionID(){

       return DEFAULT_SESSION_ID;

    }

}

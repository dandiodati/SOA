///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.simulator;

import java.io.File;

import com.nightfire.spi.neustar_soa.adapter.*;
import com.nightfire.spi.neustar_soa.adapter.smartsockets.*;

import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.communications.*;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.*;

/**
 * This is a Comm Server implementation used to simulate the OSS NPAC Gateway's
 * responses to New Session, Association, and Recovery requests.
 */
public class NPACReplyComServer extends ComServerBase
                                implements Receiver{

    /**
     * This is the name of the node in the request XML that indicates that
     * the request is a NewSession request.
     */
    public static final String NEW_SESSION_TYPE = "NewSession";

    /**
     * This is the name of the node in the request XML that indicates that
     * the request is an Association request.
     */
    public static final String ASSOCIATION_REQUEST_TYPE =
                                       "AssociationRequest";

    /**
     * This is the name of the node in the request XML that indicates that
     * the request is a DownloadRecovery request.
     */
    public static final String DOWNLOAD_RECOVERY_REQUEST_TYPE =
                                       "DownloadRecoveryRequest";

    /**
     * This is the name of the node in the request XML that indicates that
     * the request is a Recovery request.
     */
    public static final String RECOVERY_REQUEST_TYPE =
                                       "RecoveryRequest";

    /**
     * This is the name of the node in the request XML that indicates that
     * the request is a Recovery Complete request.
     */
    public static final String RECOVERY_COMPLETE_TYPE =
                                       "RecoveryCompleteRequest";

    /**
     * This is the name of the node in the request XML that indicates that
     * the request is a Client Keep Alive message from the SOA.
     */
    public static final String KEEP_ALIVE_TYPE = "ClientKeepAlive";

    /**
     * The name of the optional property indicating where any received
     * request XMLs should be written.
     */
    public static final String PROCESSED_DIR_PROP = "PROCESSED_DIR";

    /**
     * This is used to send simulated replies back the SOA's receiver
     * service.
     */
    private NPACReplyClient client;

    /**
     * An optional output directory where incoming request XML can be written.
     */
    private File outputDir = null;

    /**
     * The NPAC SmartSockets' project.
     */
    private String project;

    /**
     * The subject on which incoming requests will be received.
     */
    private String inboundSubject;

    /**
     * Receives incoming NPAC requests.
     */
    private NPACReceiver receiver;

    /**
     * The SmartSockets RTServer host.
     */
    private String rtserver;

    private int receiverThreadCount = 10;

    private static NPACReplyComServer singleton;

    /**
     * Constructor.
     *
     * @param key String the property key used to load this server's properties
     *                   from the PersistentProperty table.
     * @param type String the property type used to load this server's
     *                    properties from the PersistentProperty table.
     * @throws ProcessingException if any required properties are missing
     *                             of have invalid values.
     */
    public NPACReplyComServer(String key, String type)
                              throws ProcessingException{

       super(key, type);

       // get the optional output directory for received requests
       String outputPath = getPropertyValue( PROCESSED_DIR_PROP );
       if(outputPath != null){

          outputDir = new File(outputPath);

       }

       project = getRequiredPropertyValue( NPACComServer.NPAC_PROJECT_PROP );

       inboundSubject =
          getRequiredPropertyValue( NPACComServer.NPAC_SUBJECT_PROP );

       String outboundSubject =
          getRequiredPropertyValue( NPACComServer.SOA_UNIQUE_SUBJECT_PROP );

       rtserver = getRequiredPropertyValue( NPACComServer.NPAC_RTSERVER_PROP );

       // initialize client for sending responses
       try{
          client = new NPACReplyClient(project,
                                       outboundSubject,
                                       rtserver);
       }
       catch(FrameworkException fex){
          throw new ProcessingException( fex.getMessage() );
       }

       // this assumes that we only have one NPAC Reply Com Server
       // per virtual machine
       singleton = this;

    }

    /**
    * The SOAP NotificationReceiver service will forward incomimg
    * SOAP messages to this remote method.
    *
    * @param notification the incoming XML notification.
    * @return int the ACK/NACK value.
    * @throws RemoteException required to tag this method as a remote method.
    */
    public int process(String notification) {

       if( Debug.isLevelEnabled(Debug.IO_STATUS) ){

         Debug.log( Debug.IO_STATUS,
                    "Received request:\n"+
                    notification );

       }

       XMLMessageParser parsedNotification;
       String notificationType;

       try{

           parsedNotification = new XMLMessageParser(notification);

           // This check is currently necessary, because the SOAP
           // Notification Receiver Service tries to broadcast any incoming
           // messages to all registered receivers. This means that
           // it is possible that this simulators own messages may be broadcast
           // back to it. We need to NACK these messages so that they
           // will then get passed the the SOA's receiver.
           if( ! parsedNotification.exists(NPACConstants.SOA_TO_NPAC) ){

              Debug.error("The given message is not a SOA to NPAC message and "+
                          "will be NACKed and ignored.");

              return NPACConstants.NACK_RESPONSE;

           }

           // get the notification type
           try{
              notificationType =
                 parsedNotification.getNode(NPACConstants.SOA_TO_NPAC+".0").getNodeName();
           }
           catch(MessageException mex){

              Debug.error("Could not retrieve request type: "+
                          mex.toString());

              return NPACConstants.NACK_RESPONSE;

           }

       }
       catch(MessageException mex){

           Debug.error("Could not parse notification:\n"+notification+
                       "\n"+mex.toString());

           return NPACConstants.NACK_RESPONSE;

       }

       // get the invoke ID since it is needed in the reply message
       String invokeID;

       try{
           invokeID = parsedNotification.getTextValue(NPACConstants.INVOKE_ID);
       }
       catch(MessageException mex){

           Debug.error("Could not get invoke ID: "+mex.toString());

           return NPACConstants.NACK_RESPONSE;

       }

       // get the customer ID (this is a SPID)
       String customerID = "";

       try{

           if( parsedNotification.exists(NPACConstants.CUSTOMER_ID) ){

              customerID =
                 parsedNotification.getTextValue(NPACConstants.CUSTOMER_ID);

           }

       }
       catch(MessageException mex){

           Debug.warning("Could not get customer ID (SPID) from notification "+
                         "from request with invoke ID ["+invokeID+"]: "+
                         mex.getMessage());

       }

       if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

           Debug.log(Debug.MSG_STATUS,
                     "Received request:"+
                     "\n\tType        ["+notificationType+
                     "]\n\tInvoke ID   ["+invokeID+
                     "]\n\tSPID        ["+customerID+"]\n");

       }

       if( notificationType.equals( NEW_SESSION_TYPE ) ){

           client.sendNewSessionReply(invokeID, customerID);

       }
       else if( notificationType.equals(ASSOCIATION_REQUEST_TYPE) ){

           client.sendAssociationReply(invokeID,
                                       getRegionID(parsedNotification),
                                       customerID);

       }
       else if( notificationType.equals(DOWNLOAD_RECOVERY_REQUEST_TYPE) ){

          client.sendDownloadRecoveryReply( invokeID,
                                            getRegionID(parsedNotification),
                                            customerID );

       }
       else if( notificationType.equals(RECOVERY_REQUEST_TYPE) ){

          client.sendRecoveryReply( invokeID,
                                    getRegionID(parsedNotification),
                                    customerID );

       }
       else if( notificationType.equals(RECOVERY_COMPLETE_TYPE) ){

          client.sendRecoveryCompleteReply(invokeID,
                                           getRegionID(parsedNotification),
                                           customerID);

       }
       else if( notificationType.equals(KEEP_ALIVE_TYPE) ){

          // We will simply log that a "keep alive" message was received.
          // We don't need to write these messages to out to a file.
          String number = "not found";

          try{
            number =
                parsedNotification.getTextValue(
                NPACConstants.CLIENT_SEQUENCE_NUMBER);
          }
          catch(MessageException mex){

            Debug.error("Could not get sequence number from "+
                        "Keep Alive message: "+
                        mex);

          }

          Debug.log(Debug.MSG_STATUS,
                    "Keep alive message received. Number: "+
                    number);


       }
       else {

          if(outputDir != null){

             try{

               File requestFile = new File(outputDir,
                                           notificationType +
                                           "-" + invokeID + ".xml");
               FileUtils.writeFile(requestFile.getPath(), notification);

             }
             catch(Exception ex){

               Debug.error("Could not log request message: "+notification);

             }


          }

       }

       return NPACConstants.ACK_RESPONSE;

    }

   /**
   * This publishes the RMI interface for this com server.
   * This method defines the Runnable interface from ComServer.
   */
   public void run(){

       try{

          // initialize receiver for incoming requests
          receiver = new NPACReceiver(project,
                                      inboundSubject,
                                      inboundSubject,
                                      null,
                                      null,
                                      rtserver,
                                      this,
                                      receiverThreadCount);


       }
       catch(FrameworkException fex){

          Debug.logStackTrace(fex);

       }

       // have the receiver listen for incoming responses
       receiver.run();

   }

   /**
   * Shuts down this comm server and cleans up RMI interface. This
   * defines the abstract shutdown() method from ComServerBase.
   */
   public void shutdown(){

      // clean up SmartSockets connections
      client.cleanup();
      receiver.cleanup();

   }

   public int send(String npacXML) throws MessageException{

      XMLMessageParser parser = new XMLMessageParser(npacXML);

      String spid = "NONE";

      if( parser.exists(NPACConstants.CUSTOMER_ID) ){
         spid = parser.getTextValue(NPACConstants.CUSTOMER_ID);
      }

      return send( spid, npacXML );

   }

   /**
    * Used to send messages back to the SOA.
    *
    * @param spid String the SPID for the message.
    * @param xml String the XML message to be sent.
    * @return int
    */
   public int send(String spid, String xml){

      return client.send( spid, xml );

   }

   public static NPACReplyComServer getInstance(){

      return singleton;

   }

   /**
    * This is a utility method for extracting the region ID from the request
    * message.
    *
    * @param parsedMessage XMLMessageParser the parsed message where the
    *                                       region ID value will be found.
    * @return String the region ID if found. If not found, this method logs
    *                an error and returns an empty String.
    */
   private static String getRegionID(XMLMessageParser parsedMessage){

        String regionID = "";

        try{

           regionID = parsedMessage.getTextValue(NPACConstants.NPAC_REGION_ID);

        }
        catch(MessageException mex){

           Debug.error("Could not get region ID from request: "+mex);

        }

        return regionID;

    }



}

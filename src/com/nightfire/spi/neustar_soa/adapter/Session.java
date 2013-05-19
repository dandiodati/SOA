///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import java.util.List;
import java.util.ArrayList;
import java.util.Vector;

import com.nightfire.framework.util.Debug;

/**
* This class keeps track of all the data related to a particular NPAC
* session.
*/
public class Session {

   /**
   * Code used to indicate that the association for a particular region
   * is down.
   */
   public static final int ASSOCIATION_DOWN = 0;

   /**
   * Code used when the association's region is not required by any of the
   * customer SPIDs.
   */
   public static final int ASSOCIATION_NOT_REQUIRED = 1;

   /**
   * Code used to indicate that the association for a particular
   * region has sent an association request and is waiting
   * for a reply.
   */
   public static final int ASSOCIATION_WAITING_FOR_REPLY = 2;

   /**
   * Code used to indicate that the association has a queued
   * retry attempt waiting to resend an association request.
   */
   public static final int ASSOCIATION_RETRY = 3;

   /**
   * Code used to indicate that the association is connected
   * but currently in recovery mode.
   */
   public static final int ASSOCIATION_RECOVERING = 4;

   /**
   * Code used to indicate that the association for a particular
   * region is alive and kicking.
   */
   public static final int ASSOCIATION_CONNECTED = 5;

   /**
   * The primary SPID that owns this session.
   */
   private String primarySPID;

   /**
   * This is the list of secondary SPIDs that are associated with this
   * Session's primary SPID.
   */
   private List secondarySPIDs = new ArrayList();

   /**
   * The session ID is assigned by the NPAC. This will be null until
   * the session has been created with the XML Gateway. A session ID
   * of 0 indicates a new session that has not yet received a session
   * ID from the NPAC gateway.
   */
   private String sessionID = NPACConstants.UNINITIALIZED_SESSION_ID;

   /**
   * This array keeps track of each association's state. Valid values
   * are the static codes defined in this class (e.g. ASSOCIATION_CONNECTED).
   */
   private volatile int[] associationState =
                                new int[NPACConstants.REGION_COUNT];

   /**
   * This is the sequence number that will be sent with each client keep alive
   * message initiated for this session.
   */
   private int keepAliveCount = 1;

   /**
   * This keeps track of the sequence number received with the server initiated
   * keep-alive message. This is used to check if a keep-alive message from
   * the server has been received out of order, indicating that
   * some keep-alive messages have been missed, and the session needs to be
   * restarted.
   */
   private int serverKeepAliveCount = 1;

   /**
   * This is the list of listeners to be notified in case of an
   * association status change.
   */
   private List associationListeners = new ArrayList();

   /**
   * Constructor.
   *
   * @param primarySPID the primary SPID for this session. There is
   *                    one active session allowed for each primary SPID.
   */
   public Session(String primarySPID){

      this.primarySPID = primarySPID;

   }

   /**
   * Accesses the primary SPID associated with this session.
   */
   public String getPrimarySPID(){

      return primarySPID;

   }

   /**
   * This gets the current session ID. If not down, the session ID
   * will be 0, otherwise it will have some numeric value.
   */
   public String getSessionID(){

      return sessionID;

   }

   /**
   * This is used to set the session ID when a NewSessionReply is received
   * assigning us a session ID.
   */
   public void setSessionID(String id){

      sessionID = id;

   }

   /**
   * This adds a secondary (customer) SPID that is associated with
   * the primary SPID for this session.
   */
   public void add(SecondarySPID secondarySPID){

      secondarySPIDs.add( secondarySPID );

   }

   /**
   * This gets the list of SPIDs that support the given region.
   * If not SPIDs are defined for a region, then an Association
   * will not be created for that region. The list of SPIDs
   * is also used to perform recovery for each SPID in the
   * given region.
   */
   public List getSPIDsForRegion(int region){

      List result = new ArrayList(secondarySPIDs.size());

      // go through the list of all secondary SPIDs for this
      // session
      for(int i = 0; i < secondarySPIDs.size(); i++){

         SecondarySPID customer = (SecondarySPID) secondarySPIDs.get(i);

         // if the current SPID/customer supports the given region
         // then add that SPID to the list of results
         if( customer.isRegionSupported(region) ){

            result.add( customer.getSPID() );

         }

      }

      return result;

   }

   /**
   * If the current session ID is initialized, then this session
   * is consider alive. This check is used to determine is a client keep
   * alive message should be sent for this session.
   */
   public boolean isAlive(){

      return !( sessionID.equals( NPACConstants.UNINITIALIZED_SESSION_ID ) );

   }

   /**
   * This gets the next keep alive number in the sequence.
   * This is used in sending client keep alive messages for this
   * session.
   */
   public int nextKeepAliveSequenceNumber(){

      return keepAliveCount++;

   }

   /**
   * This is called when a GatewayKeepAlive is received for this session.
   * This message checks to see if the seqeunce number that was received
   * was received in the correct order.
   *
   * @param sequenceNumber the sequence number given in the keep alive message.
   * @returns 0 for an ACK if sequence is in order. -1 for a NACK if out of
   *          order.
   */
   public int keepAliveReceived( int sequenceNumber ){

      if(serverKeepAliveCount != sequenceNumber){

         return NPACConstants.NACK_RESPONSE;

      }

      serverKeepAliveCount++;

      return NPACConstants.ACK_RESPONSE;

   }

   /**
   * This is called to reinitialize the session. The session ID
   * is reset to 0. The keep alives counts are reset to 1, and
   * the associations are flagged as down.
   */
   public void reset(){

      // reset the session ID to uninitialized
      sessionID = NPACConstants.UNINITIALIZED_SESSION_ID;

      // reset the keep alive counts to 1
      keepAliveCount = 1;
      serverKeepAliveCount = 1;

      // reset all association states
      for(int i = 0; i < associationState.length; i++){

         setAssociationDown( i );

      }

   }

   /**
   * This checks to see if the association for a particular region is
   * down. This is used in determining if an Association needs to be
   * connected. If connected, then there is no reason to try
   * to connect it again.
   */
   public boolean isAssociationDown(int region){

      return associationState[region] == ASSOCIATION_DOWN;

   }

   /**
   * This flags the association for the given region as down.
   */
   public void setAssociationDown(int region){

      setAssociationStatus(region, ASSOCIATION_DOWN);

   }

   /**
   * This flags the association for the given region as not required.
   * This indicates that no secodary SPIDs for this session support
   * the given region, and there is no reason to create an
   * Association for this region.
   *
   */
   public void setAssociationNotRequired(int region){

      setAssociationStatus(region, ASSOCIATION_NOT_REQUIRED);

   }

   /**
   * This is used to flag the association for the given region as up
   * and running.
   */
   public void setAssociationConnected(int region){

     setAssociationStatus(region, ASSOCIATION_CONNECTED);

   }

   /**
   * This is used to flag the association for the given region
   * as in the process of sending recovery messages.
   */
   public void setAssociationRecovering(int region){

     setAssociationStatus(region, ASSOCIATION_RECOVERING);

   }

   /**
   * This flags the association for the given region as waiting
   * for a connection retry attempt.
   */
   public void setAssociationRetry(int region){

     setAssociationStatus(region, ASSOCIATION_RETRY);

   }

   /**
   * This flags the association as waiting for an AssociationReply
   * to its connection request.
   */
   public void setAssociationWaitingForReply(int region){

     setAssociationStatus(region, ASSOCIATION_WAITING_FOR_REPLY);

   }

   /**
   * This sets the status code for the given region.
   */
   private void setAssociationStatus(int region, int status){

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

         Debug.log(Debug.MSG_STATUS,
                   "Changed status of association ["+
                   region+"-"+getRegionLabel(region)+
                   "] for session ["+getSessionID()+
                   "] to ["+getAssociationStateLabel(status)+
                   "]" );

      }

      associationState[region] = status;

      // fire event letting the whole work know about this exciting news
      fireAssociationEvent(region, status);

   }

   /**
   * This returns a string describing this session and its state.
   */
   public String toString(){

      StringBuffer buffer = new StringBuffer("\nSession ID       [");
      buffer.append( getSessionID() );
      buffer.append( "]\nPrimary SPID     [");
      buffer.append( getPrimarySPID() );
      buffer.append( "]\nKeep Alive Count [");
      buffer.append( keepAliveCount );
      buffer.append( "]\nIs Alive         [");
      buffer.append( isAlive() );
      buffer.append( "]\nAssociations:\n" );

      for(int i = 0; i < associationState.length; i++){

         buffer.append("\tRegion ");
         buffer.append(i);
         buffer.append(" - ");
         buffer.append(getRegionLabel(i));
         buffer.append(" : [");
         buffer.append(getAssociationStateLabel(associationState[i]));
         buffer.append("]\n");

      }

      buffer.append("\n");

      return buffer.toString();

   }

   /**
   * Adds a listener to be notified when an association's status has changed.
   */
   public void addAssociationListener(AssociationListener listener){

      synchronized(associationListeners){
        associationListeners.add(listener);
      }

   }

   /**
   * Removes an association listener.
   */
   public void removeAssociationListener(AssociationListener listener){

      synchronized(associationListeners){
         associationListeners.remove(listener);
      }

   }

   private void fireAssociationEvent(int region, int status){

      int size = associationListeners.size();

      if(size > 0){

         AssociationEvent event = new AssociationEvent(this,
                                                       region,
                                                       status);

         AssociationListener[] listeners = new AssociationListener[size];
         associationListeners.toArray(listeners);
         size = listeners.length;

         for(int i = 0; i < size; i++){

            listeners[i].associationStateChanged(event);

         }

      }


   }

   /**
   * Gets the label for a region. This is used in logging.
   */
   public static String getRegionLabel(int region){

      switch(region){

         case 0 : return "Midwest     ";

         case 1 : return "Mid-Atlantic";

         case 2 : return "Northeast   ";

         case 3 : return "Southeast   ";

         case 4 : return "Southwest   ";

         case 5 : return "Western     ";

         case 6 : return "West Coast  ";

         case 7 : return "Canada      ";

      }

      return "ERROR";

   }

   /**
   * Gets the label for a particular association state. This is used
   * in logging the state of an association.
   */
   public static String getAssociationStateLabel(int state){

      switch(state){

         case ASSOCIATION_CONNECTED : return "CONNECTED";

         case ASSOCIATION_DOWN : return "DOWN";

         case ASSOCIATION_NOT_REQUIRED : return "NOT REQUIRED";

         case ASSOCIATION_RECOVERING : return "RECOVERING";

         case ASSOCIATION_RETRY : return "WAITING FOR RETRY";

         case ASSOCIATION_WAITING_FOR_REPLY : return "WAITING FOR REPLY";

      }

      return "ERROR";

   }

}

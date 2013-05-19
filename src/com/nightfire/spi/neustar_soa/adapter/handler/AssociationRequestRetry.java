///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import com.nightfire.spi.neustar_soa.adapter.*;

/**
* This is used to resent an AssociationRequest in the case where
* the previous attempt failed. 
*/
public class AssociationRequestRetry extends Retry {

   /**
   * A reference to the adapter, used to make the call to resend the
   * AssociationRequest. 
   */
   private NPACAdapter adapter;

   /**
   * The region for which the AssociationRequest is being made. 
   */
   private int region;

   /**
   * Constructor. 
   */
   public AssociationRequestRetry(NPACAdapter adapter,
                                  Session session,
                                  int region,
                                  long retryWait){

      super(retryWait, session);

      this.adapter = adapter;
      this.region  = region;

   }

   /**
   * This is called when the wait time has expired and resends the
   * AssociationRequest.
   */
   public void retry(){
      
      adapter.sendAssociationConnectRequest(session, region);

   }

   public String toString(){

      return "AssociationRequest retry for session ["+originalSessionID+
             "] in region ["+region+"]";

   }

} 
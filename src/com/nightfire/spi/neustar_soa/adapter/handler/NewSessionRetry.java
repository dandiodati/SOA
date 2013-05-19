///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter.handler;

import com.nightfire.spi.neustar_soa.adapter.NPACAdapter;
import com.nightfire.spi.neustar_soa.adapter.Session;

import com.nightfire.framework.util.Debug;

/**
* This resends a NewSession request. 
*
*/
public class NewSessionRetry extends Retry {

   /**
   * A reference to the adapter, used to resend the request.
   */
   private NPACAdapter adapter;

   /**
   * Constructor.
   */
   public NewSessionRetry(NPACAdapter adapter,
                          Session session,
                          long retryWait){

      super(retryWait, session);
      this.adapter = adapter;

   }

   /**
   * This resends the NewSession. 
   */
   protected void retry(){

      if( Debug.isLevelEnabled(Debug.MSG_STATUS) ){

         Debug.log(Debug.MSG_STATUS,
                   "Retrying new session request for session:\n"+
                   session);

      }

      adapter.sendNewSessionRequest(session);

   }

   public String toString(){

      return "NewSession retry for primary SPID ["+session.getPrimarySPID()+"]";

   }

} 
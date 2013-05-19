///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

/**
* This interface is used to listen for changes to the state a
* a Session's associations.
*/
public interface AssociationListener {

   /**
   * This method is called by the Session when the state of
   * an association changes (for example it goes down, completes
   * recovery, etc.).
   */
   public void associationStateChanged(AssociationEvent event);

} 
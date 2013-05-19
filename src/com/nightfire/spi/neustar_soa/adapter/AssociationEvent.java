///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import java.util.*;

/**
* This event describes the changing state of an association for
* a particular region. 
*/
public class AssociationEvent extends EventObject {

   /**
   * The association's region. 
   */
   private int region;

   /**
   * The code representing the new state/status of the association. These
   * constant values are defined in the Session class. 
   */
   private int state;

   /**
   * Constructor.
   *
   * @param session the Session that issued this event.
   * @param region the region whose association has changed status
   * @param newState the new status of the association. 
   */
   public AssociationEvent(Session session, int region, int newState) {

      super(session);

      this.region = region;
      this.state = newState;

   }

   /**
   * Accessed the region of the affected Assoication.
   */
   public int getRegion(){

      return region;

   }

   /**
   * Accessed the new status of the affected Association. 
   */
   public int getAssociationState(){

      return state;

   }

} 
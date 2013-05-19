////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import java.util.HashMap;

import com.nightfire.mgrcore.queue.ConsumerBase;
import com.nightfire.mgrcore.queue.QueueConstants;
import com.nightfire.mgrcore.queue.QueueException;

public class NPACMessageConsumer extends ConsumerBase{

   public NPACMessageConsumer() throws QueueException{

      super();

   }

   /**
    * This sets the criteria for dequeuing (consuming) messages
    * so that only messages intended for this consumer's region will
    * get consumed.
    *
    * @param staticWhereCondition String
    * @param orderBy String
    * @throws QueueException
    */
/*
   public void setDequeueCriteria ( String staticWhereCondition,
                                    String orderBy )
                                    throws QueueException
   {
        HashMap map = new HashMap( );

        super.setDequeueCriteria( NPACMessageType.NPAC_MESSAGE_TYPE,
                                  map,
                                  orderBy,
                                  staticWhereCondition );
   }
*/

}

////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import com.nightfire.spi.neustar_soa.adapter.NPACConstants;

import java.util.*;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.*;

import com.nightfire.framework.util.Debug;

import com.nightfire.mgrcore.queue.ProducerBase;
import com.nightfire.mgrcore.queue.QueueException;

public class SOAMessageProducer extends ProducerBase {

	//Reply constants, check in npac xml for setting priority of messages  
	private final static String RESPONSE_REPLY = "Reply";
	private final static String RESPONSE_QUERY_REPLY = "QueryReply";
	private final static String RECOVERY_REPLY = "RecoveryReply";
	
	//priority constants, for setting value in value map or table
	private final static String PRIORITY = "priority";

   public SOAMessageProducer() throws QueueException {
      super();
   }

   public String enqueue(String npacXML)
                         throws QueueException{

      Map values = new HashMap();

      values.put( SOAMessageType.MESSAGE_TYPE_COL,
                  SOAMessageType.SOA_MESSAGE_TYPE);

      values.put( SOAMessageType.MESSAGE_COL, npacXML );

      // priority 3 is high 
      // priority "3" is set to pick success/failure reply message first
      // default priority value "7" will be set for rest of reply and notification from SOA_QUEUE table
      // not assigning priority "3" to QueryReply and RecoveryReply.
      if(npacXML.indexOf(RESPONSE_QUERY_REPLY) <= -1 && npacXML.indexOf(RECOVERY_REPLY) <= -1  && npacXML.indexOf(RESPONSE_REPLY) > -1)
      {
    	  values.put(PRIORITY,"3");
      } 
          
      try{

         XMLMessageParser parsedMessage = new XMLMessageParser(npacXML);

         // get the region id (if there is one) from the message
         if( parsedMessage.textValueExists(NPACConstants.NPAC_REGION_ID) ){

             String regionValue =
                parsedMessage.getTextValue(NPACConstants.NPAC_REGION_ID);

             try{

                Integer region = Integer.valueOf(regionValue);
                values.put(SOAMessageType.REGION_COL, region);

             }
             catch(NumberFormatException nfex){
                Debug.error("Could not get integer value of region ["+
                            regionValue+"]: "+nfex);
             }

         }

         // get the SPID value (if there is one)
         if( parsedMessage.textValueExists(NPACConstants.CUSTOMER_ID) ){

             String spid =
                parsedMessage.getTextValue(NPACConstants.CUSTOMER_ID);

             values.put(SOAMessageType.SPID_COL, spid);

         }

      }
      catch(MessageException mex){
         Debug.error(mex+": Could not get message values from XML message:\n"+
                     npacXML);
      }

      return super.add( SOAMessageType.SOA_MESSAGE_TYPE, values );

   }

}

////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import java.util.*;

import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.db.DBInterface;

import com.nightfire.mgrcore.queue.ProducerBase;
import com.nightfire.mgrcore.queue.QueueException;

/**
 * This class is used to insert messages into an NPAC message queue.
 */
public class NPACMessageProducer extends ProducerBase{

   /**
    * Constructor
    *
    * @throws QueueException thrown by the parent constructor.
    */
   public NPACMessageProducer() throws QueueException{
      super();
   }

   /**
    * Inserts a message into the NPAC queue.
    *
    * @param invokeID int the message's invoke ID.
    * @param tableName String the name of the table where the message's
    *                         status will be update when it is successfully
    *                         inserted.
    * @param npacXML String the XML message itself.
    * @param spid String the SPID for whom the message is being sent.
    */
   public String enqueue(int invokeID,
                         String tableName,
                         String npacXML,
                         String spid)
                         throws QueueException{

      Map values = new HashMap();

      // add NPAC-specific values to map
      values.put( NPACMessageType.MESSAGE_TYPE_COL,
                  NPACMessageType.NPAC_MESSAGE_TYPE);
      values.put( NPACMessageType.MESSAGE_KEY_COL, Integer.valueOf(invokeID) );
      values.put( NPACMessageType.TABLENAME_COL, tableName );
      values.put( NPACMessageType.MESSAGE_COL, npacXML );
      values.put( NPACMessageType.SPID_COL, spid);

      return super.add( NPACMessageType.NPAC_MESSAGE_TYPE, values );

   }

   /**
    * For testing and for inserting a message into the queue from the
    * command-line.
    *
    */
   public static void main(String[] args){

      try{

         Debug.enableAll();
         DBInterface.initialize();

         int invokeID = Integer.parseInt( args[0] );
         String tableName = args[1];
         String xml = FileUtils.readFile(args[2]);
         String spid = args[3];

         NPACMessageProducer producer =
            new NPACMessageProducer();

         producer.enqueue(invokeID,
                          tableName,
                          xml,
                          spid);

      }
      catch(Exception ex){
         System.err.println("Usage: java "+
                            NPACMessageProducer.class.getName()+
                           " <invoke ID> <table name> <xml file> <spid>");
         ex.printStackTrace();
      }

   }

}

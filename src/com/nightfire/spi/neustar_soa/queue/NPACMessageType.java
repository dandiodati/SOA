////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import java.util.*;
import com.nightfire.mgrcore.queue.*;

/**
 * This class describes and provides access to the values that will get
 * stored in the outbound NPAC message queue.
 */
public class NPACMessageType extends SOAMessageType{

   /**
    * This is the key used to get the message definition
    * from the repository.
    */
   public static final String NPAC_MESSAGE_TYPE = "npac";

   /**
    * The name of the column that will contain a message's invoke ID value.
    */
   public static final String MESSAGE_KEY_COL = "MESSAGEKEY";

   /**
    * The name of the message table whose status needs to be updated once this
    * message is successfully sent.
    */
   public static final String TABLENAME_COL = "TABLENAME";

   /**
    * The invoke ID for this message (aka Message Key).
    */
   protected Integer invokeID;

   /**
    * The table name where the message is stored. This is used to update the
    * status of the message to "Sent" once the message has been de-queued.
    */
   protected String tableName;

   /**
    * This NPAC message specific database columns in addition to the default
    * "ManagerMessage" columns.
    */
   private static SQLColumn[] columns =
   {
       new SQLColumn ( MESSAGE_KEY_COL, SQLColumn.INT_TYPE ),
       new SQLColumn ( TABLENAME_COL, SQLColumn.STRING_TYPE ),
   };

   /**
    * This sets the values of this message's fields based
    * on the values in the given map.
    *
    * @param map Map the map containing the field names and values.
    * @throws QueueException
    */
   public void setValues ( Map map ) throws QueueException
   {
      super.setValues( map );

      // get the NPAC message specific values
      invokeID = (Integer) map.get( MESSAGE_KEY_COL );
      tableName = (String) map.get( TABLENAME_COL );
      spid = (String) map.get( SPID_COL );

   }

    /**
     * This gets a map containing the names and values of
     * all fields stored for this message type.
     *
     * @return Map containing values that constitute this message
     * in user format. This is a new instance of a a Map being returned.
     *
     * @throws QueueException If processing fails.
     */
    public Map getValues ( ) throws QueueException
    {

       Map map = super.getValues();

       // add the NPAC message specific values
       map.put( MESSAGE_KEY_COL, invokeID );
       map.put( TABLENAME_COL, tableName );
       map.put( SPID_COL, spid );

       return map;

    }


    /**
     * Get the meta-data that defines this message. This basically
     * sets up the message to be populated from a queue or be posted
     * to a queue. This returns a new instance of SQLColumnContainer
     * each time it is invoked.
     *
     * @return SQLColumnContainer that defines this message. It contains
     * a list of element to db-data-type mappings.
     *
     * @throws QueueException If processing fails.
     */
    public SQLColumnContainer getMetaData ( ) throws QueueException
    {

        SQLColumnContainer sqlColumnContainer = super.getMetaData();

        sqlColumnContainer.add( columns );

        return sqlColumnContainer;

    }

    /**
     * Convenience method to access the invoke ID (the message key).
     *
     * @return Integer the value of the invoke ID.
     */
    public Integer getInvokeID(){

       return invokeID;

    }

    /**
     * Accesses the table name where the message is stored. This table name
     * is used to update
     *
     * @return String the name of the table where the message itself is stored.
     */
    public String getTableName(){

       return tableName;

    }

}

////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import java.util.*;
import com.nightfire.mgrcore.queue.*;

/**
 * This defines the messages that will be received from the
 * NPAC and queued for delivery to the SOA for processing.
 */
public class SOARequestMessageType extends ManagerMessage {


	/**
	 * This is the key used to get the message definition
	 * from the repository.
	 */
	public static final String SOA_REQUEST_MESSAGE_TYPE = "soarequest";

   
   /**
    * The name of the column that will contain the SPID to which the message
    * belongs.
    */
   public static final String SPID_COL = "SPID";


   /**
    * The NPAC XML message itself.
    */
   protected String message;

   /**
    * The SPID of the service provider to whom this message belongs.
    */
   protected String spid;

   

   /**
    * This message specific database columns in addition to the defauly
    * "ManagerMessage" columns.
    */
   private static SQLColumn[] columns =
   {
       new SQLColumn ( QueueConstants.MESSAGE_COL, SQLColumn.CLOB_TYPE ),
       new SQLColumn ( SPID_COL, SQLColumn.STRING_TYPE ),
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

      message = (String) map.get( MESSAGE_COL );
      spid    = (String) map.get( SPID_COL );

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

       map.put( MESSAGE_COL, message );

       if( spid != null){
          map.put(SPID_COL, spid);
       }

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
     * Set the values passed in the map on this message object.
     * The values passed in are values fetched from/can be posted to the database
     * without formatting. This method is used by the queue implementation.
     * This method is reponsible for translating all db specific data
     * into user understandable format.
     * NOTE: This is not used by a producer or consumer.
     *
     * @param map A Map containing names and values, in db format.
     *
     * @throws QueueException If processing fails.
     */
    public void setDBData ( Map map ) throws QueueException
    {

       setValues( map );

    }

    /**
     * Get database understandable ( queue-able ) values from this message.
     * This is used by the queue implementation to get db specific data.
     * NOTE: This is not used by a producer or consumer.
     *
     * @return Map containing values that constitute this message
     * in database format. This is a new instance of a a Map being returned.
     *
     * @throws QueueException If processing fails.
     */
    public Map getDBData ( ) throws QueueException
    {

       return getValues();

    }

    /**
     * Accesses the XML message itself.
     *
     * @return String the value of the XML message itself.
     */
    public String getMessage(){

       return message;

    }

    /**
     * Access the SPID associated with the message.
     *
     * @return String the SPID value.
     */
    public String getSPID(){

       return spid;

    }

    /**
     * Updates the message's status value.
     *
     * @param String the new status value.
     */
    public void setStatus(String newStatus){

       super.status = newStatus;

    }

    
}

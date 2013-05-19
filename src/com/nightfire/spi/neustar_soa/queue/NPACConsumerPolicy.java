////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 NeuStar, Inc. All rights reserved.
// The source code provided herein is the exclusive property of NeuStar, Inc.
// and is considered to be confidential and proprietary to NeuStar.
////////////////////////////////////////////////////////////////////////////

package com.nightfire.spi.neustar_soa.queue;

import java.sql.*;
import java.util.*;

import com.nightfire.mgrcore.queue.*;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * This class is called when a message is consumed from a queue.
 */
public class NPACConsumerPolicy extends ConsumerPolicyBase{

    private static String UPDATE_CLAUSE =
                             " SET STATUS = '"+
                             SOAConstants.SENT_STATUS+
                             "' WHERE "+
                             NPACMessageType.MESSAGE_KEY_COL+
                             " = ? AND MESSAGETYPE = 'Request'";

    public NPACConsumerPolicy(){

    }

    /**
     * The message obtained from the queue was successfully processed.
     * The corresponding message in the message table gets updated
     * to have a status of "Sent".
     *
     * The message then gets deleted from the queue.
     *
     * @param message The message being processed.
     *
     * @param queue The queue to which this message belongs.
     *
     * @throws QueueException If processing fails.
     */
    public void handleSuccess ( QueueMessage queueMessage, MessageQueue queue )
       throws QueueException{

        if( ! (queueMessage instanceof NPACMessageType) ){

           Debug.error( "NPAC consumer policy received a message "+
                        "that is not of type: "+
                        NPACMessageType.class.getName() );

        }
        else{

           NPACMessageType message = (NPACMessageType) queueMessage;

           // update message status to "Sent"

           // construct update statement
           String update = "UPDATE "+
                           message.getTableName()+
                           UPDATE_CLAUSE;

           try{

              Connection connection = DBInterface.acquireConnection();

              try {

                 PreparedStatement stmt = null;

                 try {

                    stmt = connection.prepareStatement(update);

                    int invokeID = message.getInvokeID().intValue();
                    Debug.log(Debug.DB_STATUS,
                              "Updating status of message with invoke ID [" +
                              invokeID + "] to 'Sent' using statement:\n"+
                              update);

                    stmt.setInt(1, invokeID);

                    stmt.execute();
                    connection.commit();

                 }
                 catch (Exception ex) {
                    Debug.error("Could not update status for message:\n" +
                                message.describe() + "\n" + ex);
                 }
                 finally {

                    try {
                       if (stmt != null) {
                          stmt.close();
                       }
                    }
                    catch (Exception ex) {
                       Debug.error("Could not close statement: " + ex);
                    }

                 }

              }
              finally {

                 try {
                    DBInterface.releaseConnection(connection);
                 }
                 catch (Exception ex) {
                    Debug.error("Could not release database connection: " + ex);
                 }

              }

           }
           catch(DatabaseException dbex){

              Debug.error("Could not update status for message:\n" +
                           message.describe() +
                           ". A database connection could not be acquired: " +
                           dbex );

           }

        }

        // This is used in the where clause to identify which message
        // should be updated. All we need is the message's ID
        // value to uniquely identify it.
        NPACMessageType whereMessage = new NPACMessageType();
        Map whereValues = new HashMap();
        whereValues.put( ID_COL, queueMessage.getValues().get(ID_COL) );
        whereMessage.setValues( whereValues );

        // update the status of the message to "sent"
        NPACMessageType newMessage = new NPACMessageType();
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        {
	        Debug.log(Debug.MSG_STATUS,
	        		"NPACConsumerPolicy, handleSuccess(), Before removing REGION ID... values :-"
	        			+queueMessage.getValues());
        }
        
        Map value = queueMessage.getValues();
        if(value.containsKey("REGIONID"))
        {
        	value.remove("REGIONID");
        }
        newMessage.setValues(value);
        
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
        {
	        Debug.log(Debug.MSG_STATUS,
	        		"NPACConsumerPolicy, handleSuccess(), After removing region id, values :-"
	        			+newMessage.getValues());
        }
        newMessage.setStatus(SOAConstants.SENT_STATUS);
        queue.update( whereMessage, newMessage );

    }

    /**
     * This adds a special check for exceptions that occured because the
     * session is down. In this case, the error count will not be increased
     * so that the message will be resent when the session comes back up.
     *
     * @param e Exception
     * @param queueMessage QueueMessage
     * @param queue MessageQueue
     * @throws QueueException
     */
    public void handleError ( Exception e,
                              QueueMessage queueMessage,
                              MessageQueue queue )
                              throws QueueException
    {

        if(e instanceof ConnectivityDownException){

           // The session is down. Set the error message, but do not
           // increment the error count.
           NPACMessageType message = (NPACMessageType) queueMessage;
           NPACQueueUtils.updateLastErrorMessage( message.getInvokeID().toString(),
                                                  e.getMessage() );

        }
        else {
        	
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
				{
					Debug.log(Debug.MSG_STATUS,
								"NPACConsumerPolicy, handleError(), Before removing REGION ID...queueMessage.getValues():-"
														+ queueMessage.getValues());
				}

				Map queueMessageValues = queueMessage.getValues();
				
				if (queueMessageValues.containsKey("REGIONID")) {
					queueMessageValues.remove("REGIONID");
					queueMessage.setValues(queueMessageValues);
				}
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
				{
					Debug.log(Debug.MSG_STATUS,
								"NPACConsumerPolicy, handleError(), After removing REGION ID...queueMessage.getValues():-"
										+ queueMessage.getValues());
				}
				super.handleError(e, queueMessage, queue);
		}

    }


}

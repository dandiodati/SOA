/**
 * Copyright (c) 2004 NeuStar, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/ExternalNotifier.java#1 $
 */

package com.nightfire.webgui.core;

import  java.io.*;
import  java.net.*;
import  java.util.*;
import java.sql.Connection;
import  javax.servlet.*;
import  javax.servlet.http.*;
import  javax.servlet.jsp.*;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;

import org.w3c.dom.*;

import org.omg.CORBA.StringHolder;
import org.omg.CORBA.ORB;
import  org.w3c.dom.*;

import  com.nightfire.framework.constants.*;
import  com.nightfire.framework.corba.*;
import  com.nightfire.framework.message.MessageException;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import  com.nightfire.framework.repository.*;
import  com.nightfire.framework.util.*;

import  com.nightfire.security.*;
import com.nightfire.security.domain.DomainProperties;
import com.nightfire.security.data.Domain;

import  com.nightfire.webgui.core.beans.*;
import  com.nightfire.webgui.core.*;
import  com.nightfire.framework.debug.*;
import com.nightfire.framework.jms.JMSPortabilityLayer;
import com.nightfire.framework.jms.JMSConnection;
import com.nightfire.framework.jms.JMSSession;
import com.nightfire.framework.jms.JMSProducer;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.resource.ResourceException;
import  com.nightfire.webgui.core.resource.*;

import  com.nightfire.mgrcore.repository.*;

/**
 * ExternalNotifier sends a notification to a customer specific event channel.
 */

public class ExternalNotifier
{
    public static final String GUI_NOTIFICATION_TEMPLATE_NAME = "guiNotification";
    public static final String REQUEST_HEADER = "REQUEST.HEADER";
    public static final String REQUEST_BODY = "REQUEST.BODY";
    public static final String RESPONSE_HEADER = "RESPONSE.HEADER";
    public static final String RESPONSE_BODY = "RESPONSE.BODY";
    public static final String CHANNEL_NAME = "ChannelName";
    public static final String HEADER_NODE_NAME = "Header";
    public static final String EVENT_TYPE_NODE_NAME = "EventType";
    public static final String EVENT_NODE_NAME = "Event";
    public static final String STATUS_EVENT_TYPE = "status";
    public static final String MESSAGE_SENT_EVENT = "message-sent";
    public static final String SUPPLIER_NODE_NAME = "Supplier";
    public static final String TRADINGPARTNER_NODE_NAME = "TradingPartner";
    public static final String INTERFAVE_VERSION_NODE_NAME = "InterfaceVersion";
    public static final String REQUEST_TYPE_NODE_NAME = "Request";
    public static final String TRANSACTION = "Transaction";
    public static final String URLLIST_PROP = "urlList";
    public static final String EVENT_CHANNEL_NAME_PROP = "EVENT_CHANNEL_NAME";
    private static final String REQ_HEADER_NODES_TO_REMOVE[] = { CustomerContext.MESSAGE_ID };


     /**
      * Name of CustomerId property
      */
     public static final String CUSTOMER_ID_PROP = "CustomerIdentifier";

    String supplier;
    String tradingPartner;
    String svcType;
    String interfaceVersion;
    String customerID;
    Properties additionalProp;
    
    public Properties getAdditionalProp() {
		return additionalProp;
	}

	public void setAdditionalProp(Properties additionalProp) {
		this.additionalProp = additionalProp;
	}

	/**
     * Create a notification message from the request sent and the notification template and
     * send it to the event service.
     *
     * @param  messageTemplateName  The name of the notification message template
     *                              in the repository.
     * @param  workItemProps  The properties associated with the work item.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void notify ( String eventChannelName, DataHolder requestData, DataHolder responseData,
    String cid, String uid, DebugLogger log, ORB orb, ObjectLocator objectLocator, boolean useEventChannels )
        throws ServletException
    {
        notify (eventChannelName, requestData, responseData,
    cid, uid, log, orb, objectLocator, useEventChannels, null);
    }

    /**
     * Create a notification message from the request sent and the notification template and
     * send it to the event service.
     *
     * @param  messageTemplateName  The name of the notification message template
     *                              in the repository.
     * @param  workItemProps  The properties associated with the work item.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void notify ( String eventChannelName, DataHolder requestData, DataHolder responseData,
    	    String cid, String uid, DebugLogger log, ORB orb, ObjectLocator objectLocator, boolean useEventChannels, String guiNotificationDBPoolKey)
    	        throws ServletException
    	    {
    			try
    	        {

    	            this.customerID = cid;
    	            String eventQueueName = DomainProperties.getInstance(customerID).getEventQueueName();
    	            String serviceType = DomainProperties.getInstance(customerID).getServiceType();
    	            if (log.isDebugEnabled())
    	            {
    	                if(useEventChannels)
    	                    log.debug ( "Sending notification to channel [" + eventChannelName + "]." );
    	                else
    	                    log.debug ( "Sending notification to event queue [" + eventQueueName + "]." );
    	            }
    	            //The items that can be set in the notification via template configuration are items that either exist
    	            //1. in the request bean and declared as REQUEST.HEADER, REQUEST.BODY
    	            //2. in the response bean and declared as RESPONSE.HEADER, RESPONSE.BODY
    	            //The fixed items that will always be in the notification are:
    	            //1. type of event
    	            //2. name of event


    	            // CH: Apend CID to channel name.
    	            // This should be safe since it will check whether the channel name already ends with CID before appending it.
    	            // Comment out the line below if this behavior is not desired.
    	            if (log.isDebugEnabled())
    	                log.debug ( "Appending customer id to event channel [" + eventChannelName + "]." );

    	            eventChannelName = PropUtils.appendCustomerId(eventChannelName, customerID);

    	            if (log.isDebugEnabled())
    	                log.debug ( "Appended customer id to event channel [" + eventChannelName + "]." );

    	            String messageTemplate = RepositoryManager.getInstance().getMetaData(
    	            RepositoryCategories.EXTERNAL_NOTIFICATION_TEMPLATE_CONFIG, GUI_NOTIFICATION_TEMPLATE_NAME );

    	            XMLPlainGenerator template = new XMLPlainGenerator( messageTemplate );

    	            XMLPlainGenerator notification = new XMLPlainGenerator( "Notification" );

    	            //Set the fixed data first
    	            notification.setValue( HEADER_NODE_NAME + "." + EVENT_TYPE_NODE_NAME, STATUS_EVENT_TYPE );
    	            notification.setValue( HEADER_NODE_NAME + "." + EVENT_NODE_NAME, MESSAGE_SENT_EVENT );
    	            
    	            String dataMappingsRoot = "DataMappingContainer.";

    	            for ( int Ix = 0;  true;  Ix ++ )
    	            {
    	                String dataMappingItemRoot = dataMappingsRoot + Ix + ".";
    	                String sourceLocRoot = dataMappingItemRoot + "SourceLoc";

    	                String sourceLoc = null;
    	                if ( template.exists( sourceLocRoot ) )
    	                    sourceLoc = template.getValue( sourceLocRoot );

    	                if ( StringUtils.hasValue( sourceLoc ) )
    	                {
    	                    String targetLoc = template.getValue( dataMappingItemRoot + "TargetLoc" );

    	                    if (log.isDebugEnabled())
    	                        log.debug( "Looking for value at source location [" + sourceLoc
    	                                   + "] to place at target destination location [" + targetLoc + "]." );

    	                    if ( sourceLoc.equalsIgnoreCase( REQUEST_HEADER ) )
    	                    {
    	                        if ( requestData.getHeaderStr() != null )
    	                        {
    	                            Node targetNode = notification.create( targetLoc );

    	                            //Get Supplier, Service Type and Interface version from the header if available.
    	                            //These values would be en-queued as JMS header properties and would be used to fetch specific customer SOAP url from TPR
    	                            XMLPlainGenerator header = new XMLPlainGenerator(requestData.getHeaderDom());
    	                            if(header.exists(TRADINGPARTNER_NODE_NAME))
    	                                tradingPartner = header.getValue(TRADINGPARTNER_NODE_NAME);
    	                            if(header.exists(SUPPLIER_NODE_NAME))
    	                                supplier = header.getValue(SUPPLIER_NODE_NAME);
    	                            if(header.exists(INTERFAVE_VERSION_NODE_NAME))
    	                                interfaceVersion = header.getValue(INTERFAVE_VERSION_NODE_NAME);
    	                            if(header.exists(REQUEST_TYPE_NODE_NAME))
    	                                svcType = header.getValue(REQUEST_TYPE_NODE_NAME);

    	                            /**
    	                             * Removed header nodes that are not required before posting
    	                             * request messages to jms queue/database. 
    	                             */
    	                            for(int i = 0; i < REQ_HEADER_NODES_TO_REMOVE.length; i++)
    	                            {
    	                                if(header.exists(REQ_HEADER_NODES_TO_REMOVE[i]))
    	                                    header.remove(REQ_HEADER_NODES_TO_REMOVE[i]);
    	                            }

    	                            log.debug("header values: "+supplier+ " " +interfaceVersion+" " +svcType+" "+customerID);
    	                            notification.copy( targetNode, header.getDocument().getDocumentElement());
    	                        }
    	                        else
    	                            log.debug( "Request header not found." );
    	                    }
    	                    else
    	                    if ( sourceLoc.equalsIgnoreCase( REQUEST_BODY ) )
    	                    {
    	                        if ( requestData.getBodyStr() != null )
    	                        {
    	                            Node targetNode = notification.create( targetLoc );
    	                            notification.copy( targetNode, (Node)(requestData.getBodyDom().getDocumentElement()) );
    	                        }
    	                        else
    	                            log.debug( "Request body not found." );
    	                    }
    	                    else
    	                    if ( sourceLoc.equalsIgnoreCase( RESPONSE_HEADER ) )
    	                    {
    	                        if ( responseData.getHeaderStr() != null )
    	                        {
    	                            Node targetNode = notification.create( targetLoc );

    	                            //Get Supplier, Service Type and Interface version from the header if available.
    	                            //These values would be en-queued as JMS header properties and would be used to fetch specific customer SOAP url from TPR
    	                            XMLPlainGenerator header = new XMLPlainGenerator(requestData.getHeaderDom());
    	                            if(header.exists(TRADINGPARTNER_NODE_NAME))
    	                                tradingPartner = header.getValue(TRADINGPARTNER_NODE_NAME);
    	                            if(header.exists(SUPPLIER_NODE_NAME))
    	                                supplier = header.getValue(SUPPLIER_NODE_NAME);
    	                            if(header.exists(INTERFAVE_VERSION_NODE_NAME))
    	                                interfaceVersion = header.getValue(INTERFAVE_VERSION_NODE_NAME);
    	                            if(header.exists(REQUEST_TYPE_NODE_NAME))
    	                                svcType = header.getValue(REQUEST_TYPE_NODE_NAME);
    	                            notification.copy( targetNode, (Node)(responseData.getHeaderDom().getDocumentElement()) );
    	                        }
    	                        else
    	                            log.debug( "Response header not found." );
    	                    }
    	                    else
    	                    if ( sourceLoc.equalsIgnoreCase( RESPONSE_BODY ) )
    	                    {
    	                        if ( responseData.getBodyStr() != null )
    	                        {
    	                            Node targetNode = notification.create( targetLoc );
    	                            notification.copy( targetNode, (Node)(responseData.getBodyDom().getDocumentElement()) );
    	                        }
    	                        else
    	                            log.debug( "Response body not found." );
    	                    }
    	                    else
    	                        log.debug( "Source item not found." );
    	                }
    	                else
    	                    break;
    	            }//for

    	            
    	            String message = notification.getOutput( );
    	            
    	            log.debug("testing message value is "+message);
    	            
    	            if (log.isDebugEnabled())
    	                log.debug( "Sending the following external notification:\n" + message );

    	            if(useEventChannels)
    	                sendNotification ( eventChannelName, message, log, orb, objectLocator, true );
    	            else
    	                sendNotificationToJMSQueue( eventChannelName, message, log, eventQueueName, guiNotificationDBPoolKey,serviceType);

    	        }
    	        catch ( Exception e )
    	        {
    	            throw new ServletException( e );
    	        }

    	        if (log.isDebugEnabled())
    	            log.debug ( "Sent notification to channel [" + eventChannelName + "]." );

    	    }
    
    
    //end of soa 5.9
    
    
    
    
    
    
    
    
    private static boolean dbPoolInit = false;
    private void initPoolConfig(String dbPoolKey) throws ResourceException
    {
        if(!dbPoolInit)
        {
            synchronized(ExternalNotifier.class)
            {
                if(dbPoolInit)
                    return;

                try
                {
                    DBConnectionPool.getInstance(dbPoolKey);
                }
                catch (ResourceException re)
                {
                    // If no pool config found then initialize the pools
                    DBConnectionPool.initializePoolConfigurations();
                }
                dbPoolInit = true;
            }
        }
    }

    private void sendNotificationToJMSQueue(String eventChannelName, String message, DebugLogger log, String eventQueueName, String guiNotificationDBPoolKey,String serviceType)
            throws FrameworkException {

    	log.debug( "Sending notification to the JMS Event Queue");
    	
    	// Acquire an instance of JMS Portability Layer, for invoking vendor specific methods.
        JMSPortabilityLayer jpl = new JMSPortabilityLayer();

        // handle for dbConn; used if valid specific DB Pool key is passed. It is required to release dbConn to the specific db-connection pool.
        Connection dbConn = null;
        QueueConnection queueConnection = null;
        String tempOldDBThreadPoolKey = null;

        if (StringUtils.hasValue(guiNotificationDBPoolKey))
        {
            initPoolConfig(guiNotificationDBPoolKey);
            tempOldDBThreadPoolKey = DBConnectionPool.getThreadSpecificPoolKey();
            DBConnectionPool.setThreadSpecificPoolKey(guiNotificationDBPoolKey);

            dbConn = DBConnectionPool.getInstance(guiNotificationDBPoolKey).acquireConnection();
            queueConnection = jpl.createQueueConnection(dbConn);
            DBConnectionPool.setThreadSpecificPoolKey(tempOldDBThreadPoolKey);
        }
        else
            queueConnection = JMSConnection.acquireQueueConnection(jpl);
        
        
        // Acquire a JMS Session.
        QueueSession queueSession = JMSSession.acquireQueueSession ( jpl, queueConnection );
        // Create a JMS Producer instance, for sending messages to JMS queue.
        JMSProducer producer;
        
     Properties messageProperties = new Properties();
     
    
        try
        {
            log.debug( "Creating JMS producer ..." );
            producer = new JMSProducer ( jpl, queueSession );
            log.debug( "Created JMS producer." );
           
            //set message header properties
            if(!getAdditionalProp().isEmpty()){
        		Properties addProp = getAdditionalProp();	
        		log.debug("Additional Properties are " + additionalProp);	
		           	 	Enumeration em = addProp.keys();
		       	        while(em.hasMoreElements()){
		       	        String key = (String)em.nextElement();
		       	        messageProperties.put(key, addProp.get(key));
		       	    }
            }
            messageProperties.put(CUSTOMER_ID_PROP, customerID);
            messageProperties.put(TRANSACTION , svcType);

            if (StringUtils.hasValue(tradingPartner))
                messageProperties.put(SUPPLIER_NODE_NAME , tradingPartner);
            else
                messageProperties.put(SUPPLIER_NODE_NAME , supplier);

            messageProperties.put(INTERFAVE_VERSION_NODE_NAME , interfaceVersion);
            messageProperties.put(EVENT_CHANNEL_NAME_PROP,eventChannelName);
            messageProperties.put(DomainProperties.SERVICE_TYPE,serviceType);

            // Create a Message Sender Client for specified queue
            producer.createQueueProducer( eventQueueName );

            Debug.log ( Debug.MSG_STATUS, "Created Message Sender Client.. " );
            
            // If message properties are available, set the message along with the properties
            // else, set the message only.
            if( ! messageProperties.isEmpty () ){
            	log.debug("in  message properties");
                producer.setMessage ( message, messageProperties );
                }
            else{
            	log.debug("in  message ");
            	producer.setMessage ( message );
            }
            // Start the Connection
            JMSConnection.startQueueConnection ( jpl, queueConnection );

            // Send the Message
            producer.sendMessage ();
            log.debug( "Sent notification to Event Queue" );
        }
        catch (Exception e)
        {
            log.error(e.toString());
            throw new FrameworkException(e);
        }
        finally
        {
            try
            {
                 // Release the Session
                JMSSession.closeQueueSession ( jpl, queueSession );

                // if specific db connection pool is used, manually close queue-connection and release db connection to that db connection pool.
                if (StringUtils.hasValue(guiNotificationDBPoolKey))
                {
                    queueConnection.close();
                    if (dbConn != null)
                        DBConnectionPool.getInstance(guiNotificationDBPoolKey).releaseConnection(dbConn);
                }
                else // if default db connection pool is used then use JPL api to close queueConnection and it will release db-connection to default db connection pool.
                {
                    // Release the Connection
                    JMSConnection.closeQueueConnection ( jpl, queueConnection );
                }
            }
            catch ( Exception e )
            {
                log.error( "Could not close JMSConnection/Session " + e.toString() );
            }

        }
    }

    /**
     * Publishes the given request into an event channel as Corba Events
     * which can be picked up by interested listeners in the channel
     */
    private void sendNotification( String eventChannelName, String message, DebugLogger log,
    ORB orb, ObjectLocator objectLocator, boolean firstTry )
        throws FrameworkException
    {
        log.debug( "Sending notification to the Event Service");
        EventPushSupplier pushSupplier = null;

        try
        {
            log.debug( "Creating push supplier ..." );
            pushSupplier = new EventPushSupplier(orb, eventChannelName , false);
            log.debug( "Created push supplier." );

            pushSupplier.pushEvent( message );
            log.debug( "Sent notification to the Event Service" );
        }
        catch (Exception e)
        {
            if (firstTry)
            {
                log.debug( "Re-trying with new reference to event channel." );
                objectLocator.removeFromCache( eventChannelName );
                sendNotification( eventChannelName, message, log, orb, objectLocator, false );
            }
            else
            {
                log.error(e.toString());
                throw new FrameworkException(e);
            }
        }
        finally
        {
            try
            {
                if (pushSupplier != null) {
                    pushSupplier.disconnect( );
                }
            }
            catch ( Exception e )
            {
                log.error( "Could not disconnect push supplier: " + e.toString() );
            }

        }
    }

}

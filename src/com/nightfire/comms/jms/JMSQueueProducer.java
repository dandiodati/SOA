/**
* @author   Priti Budhia
**/
package com.nightfire.comms.jms;


import java.util.*;

import com.nightfire.common.*;
import com.nightfire.comms.servicemgr.helper.MessageProcessorExecutor;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.jms.JMSProducer;
import com.nightfire.framework.jms.JMSConnection;
import com.nightfire.framework.jms.JMSSession;
import com.nightfire.framework.jms.JMSPortabilityLayer;
import com.nightfire.security.domain.DomainProperties;
import com.nightfire.security.domain.DomainPropException;

import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import java.util.Properties;
import java.sql.Connection;

import org.w3c.dom.Document;

/**
 * A generic message-processor for sending messages to a
 * configured JMS Queue
 */
public class JMSQueueProducer extends MessageProcessorBase
{
    /**
     * Property indicating name of the queue to which the message has to be sent
     */
    public static final String QUEUE_NAME_PROP = "QUEUE_NAME";

    /**
     * Property indicating the input location for queue name to which the message has to be sent
     */
     public static final String QUEUE_NAME_LOC_PROP = "QUEUE_NAME_LOC";

    /**
     * Property indicating name of the application specific message-property.
     * This could be used for customized message selection by the consumer.
     */
    public static final String MESSAGE_PROPERTY_NAME_PROP = "MESSAGE_PROPERTY_NAME";

    /**
     * Property indicating the location of the message-property value
     */
    public static final String MESSAGE_PROPERTY_VALUE_LOC_PROP = "MESSAGE_PROPERTY_VALUE_LOC";

    /**
     *  Property indicating default value of the mesage-property
     */
    public static final String MESSAGE_PROPERTY_DEFAULT_VALUE_PROP = "MESSAGE_PROPERTY_DEFAULT_VALUE";

    /**
     *  Property indicating whether the message-property is optional or not.
     *  If not populated or incorrectly populated, the value is taken as 'false'
     */
    public static final String MESSAGE_PROPERTY_OPTIONAL_PROP = "OPTIONAL";

    /**
     * Property indicating name of the retry count message-property.
     * This could be used for customized message selection by the consumer while
     * retrieving messages from custom specific exception queue.
     */
    public static final String RETRY_COUNT_NAME_PROP= "RETRY_COUNT";


    /**
     * Property indicating the location where retry count value can be found.
     */
    public static final String RETRY_COUNT_VALUE_LOC_PROP= "RETRY_COUNT_VALUE_LOC";

    /**
     *  Property indicating the default value for retry count-property
     */
     public static final String RETRY_COUNT_VALUE_DEFAULT_PROP= "0";

     /**
     * Property indicating whether customerId from customer context, should be enqueued in Queue or not.
     * If not set, default value is true.
     */
     public static final String USE_CUSTOMER_ID_PROP = "USE_CUSTOMER_ID";

    /**
     * Property indicating whether IsApiEnabled column is to be checked to see if customer is an API customer or not.
     * For non API customers event message would not be enqueued in JMS queue.
     * If not set, default value is false.
     */
     public static final String LOOKUP_DOMAIN_API_FLAG_PROP = "LOOKUP_DOMAIN_API_FLAG";

    /**
     * Name of CustomerId property
     */
     public static final String CUSTOMER_ID_PROP = "CustomerIdentifier";

     /**
     * Property indicating separator token used to separate individual location alternatives.
     */
    public static final String LOCATION_SEPARATOR_PROP = "SEPARATOR";

    /**
     *  Property indicating the default location separator
     */
    public static final String DEFAULT_LOCATION_SEPARATOR_PROP = "|";

    /* Logging class Name */
    private static final String LOGGING_CLASS_NAME = "JMSQueueProducer";

    /**
     * Name of CustomerId property
     */
     public static final String EVENT_CHANNEL_NAME_PROP = "EVENT_CHANNEL_NAME";

    /**
     * Name of alternare DB property file location
     */
     public static final String ALTERNATE_DB_LOC_PROP = "ALTERNATE_DB_PROP_LOC";

    /**
     * Location of alternate Database Pool Key.
     */
    public static final String ALTERNATE_DB_POOL_KEY_LOC_PROP = "ALTERNATE_DB_POOL_KEY_LOC";

    /**
     * Property indicating whether Enqueue operation should be part of overall driver transaction.
     */
    public static final String TRANSACTIONAL_LOGGING_PROP = "TRANSACTIONAL_LOGGING";

    private boolean createTransactionalSession = false;
    
    private String SET_SERVICE_TYPE_IN_HEADER_PROP = "SET_SERVICE_TYPE_IN_HEADER";
    
    /* By Default ServiceType would be set in jms message header */
    private boolean setServiceType = true;

    /**
     * Constructor
     */
    public JMSQueueProducer()
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Creating JMS Queue Producer message-processor." );
        
        messagePropertiesConfig = new LinkedList( );
        lookupDomainApiFlag = false;
    }

    /**
         * Constructor
         */
        public JMSQueueProducer(String qName )
        {
            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log( Debug.OBJECT_LIFECYCLE, "Creating JMS Queue Producer for queue [" + qName+ "]." );
            
            messagePropertiesConfig = new LinkedList( );
            queueName = qName;
            messageProperties = new Properties();
            lookupDomainApiFlag = false;
        }

    /**
      * Constructor
      */
      public JMSQueueProducer(String qName, Properties prop)
      {
          if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log( Debug.OBJECT_LIFECYCLE, "Creating JMS Queue Producer for queue [" + qName+ "] and properties." );
            
          messagePropertiesConfig = new LinkedList( );
          queueName = qName;
          messageProperties = prop;
          lookupDomainApiFlag = false;
      }

    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize(key, type);

        // Get configuration properties specific to this processor.
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, "Initializing JMS Queue Producer ...");

        StringBuffer errorBuffer = new StringBuffer();

        try
        {
            //Get the Queue Name to which the message has to be sent.
            queueName = getPropertyValue(QUEUE_NAME_PROP);
            //Get the Queue Name location.
            queueNameLoc = getPropertyValue(QUEUE_NAME_LOC_PROP);

            customerId = CustomerContext.getInstance().getCustomerID();
            
            if(!StringUtils.hasValue(queueName))
            {
                queueName = DomainProperties.getInstance(customerId).getEventQueueName();
                if(null==queueName && !StringUtils.hasValue(queueNameLoc))
                        throw new FrameworkException("Queue Name is not set.");
            }

            setServiceType = (getPropertyValue(SET_SERVICE_TYPE_IN_HEADER_PROP) != null 
            					? StringUtils.getBoolean(getPropertyValue(SET_SERVICE_TYPE_IN_HEADER_PROP)) : true);

            createTransactionalSession = Boolean.parseBoolean(getPropertyValue(TRANSACTIONAL_LOGGING_PROP)); 
        
        }catch(FrameworkException fe)
        {
            errorBuffer.append("ERROR: " + LOGGING_CLASS_NAME + ": Could not load queue name from domain properties. " +
                    "Queue name should be configured in Domain table and/or in QUEUE_NAME property: ").append(fe.toString());
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, LOGGING_CLASS_NAME+": Queue to send messages to is [" + queueName + "].");

        // Get the property value of USE_CUSTOMER_ID
        String strTemp = getPropertyValue( USE_CUSTOMER_ID_PROP );

        if(StringUtils.hasValue(strTemp))
        {
            try {
                useCustomerId = getBoolean(strTemp);
            }
            catch(FrameworkException e)
            {
                errorBuffer.append("Property value for " + USE_CUSTOMER_ID_PROP + " is invalid. ").
                append(e.getMessage()).append("\n");
            }
        }
        
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG,
                    "To use customer while enqueueing in Queue? [" + useCustomerId + "]." );

        String flag = getPropertyValue(LOOKUP_DOMAIN_API_FLAG_PROP);
        if(StringUtils.hasValue(flag))
        {
            try {
                lookupDomainApiFlag = getBoolean(flag);
            }
            catch(FrameworkException e)
            {
                errorBuffer.append("Property value for " + LOOKUP_DOMAIN_API_FLAG_PROP + " is invalid. ").
                    append(e.getMessage()).append("\n");
            }
        }

        separator = getPropertyValue(LOCATION_SEPARATOR_PROP);

        if(!StringUtils.hasValue(separator))
            separator = DEFAULT_LOCATION_SEPARATOR_PROP;

        //Get the configuration for application specific message-properties.

        // Loop until all column configuration properties have been read ...
        for(int Ix = 0;  true;  Ix ++)
        {
            String msgPropName = getPropertyValue(PersistentProperty.getPropNameIteration(MESSAGE_PROPERTY_NAME_PROP, Ix));

            if(!StringUtils.hasValue(msgPropName))
                break;

            if(msgPropName.equals(CUSTOMER_ID_PROP))
                isCustomerIdMsgProperty = true;

            String msgPropValueLOC = getPropertyValue(PersistentProperty.getPropNameIteration(MESSAGE_PROPERTY_VALUE_LOC_PROP, Ix ) );

            String msgPropDefaultValue = getPropertyValue(PersistentProperty.getPropNameIteration(MESSAGE_PROPERTY_DEFAULT_VALUE_PROP, Ix ) );

            String msgPropOptional = getPropertyValue(PersistentProperty.getPropNameIteration(MESSAGE_PROPERTY_OPTIONAL_PROP, Ix ) );

            try
            {
               messagePropertyConfiguration mpc = new messagePropertyConfiguration ( msgPropName, msgPropValueLOC, msgPropDefaultValue, msgPropOptional );

               if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                   Debug.log(Debug.SYSTEM_CONFIG, mpc.describe());

               messagePropertiesConfig.add(mpc);
            }
            catch ( Exception e )
            {
                throw new ProcessingException ( "ERROR: "+ LOGGING_CLASS_NAME + ": Could not create message header property data description:\n"
                                               + e.toString() );
            }
        }

        // Get the Retry-Count location .
        retryCountLocation = getPropertyValue(RETRY_COUNT_VALUE_LOC_PROP);

        // Get the location of property file for the alternate DB.
        dbPropFileLoc = getPropertyValue(ALTERNATE_DB_LOC_PROP);

        // Get the location of the alternate DB pool key
        alternateDBPoolKeyLoc = getPropertyValue (ALTERNATE_DB_POOL_KEY_LOC_PROP);

        // If any of the required properties are absent, indicate error to caller.
        if(errorBuffer.length() > 0)
        {
            String errMsg = errorBuffer.toString();

            Debug.log(Debug.ALL_ERRORS, errMsg);

            throw new ProcessingException(errMsg);
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, LOGGING_CLASS_NAME+": Initialization done." );
    }


    private MessageProcessorContext mpContext =null; 
    /**
     * Extract data values from the context/input, and use them to
     * send message to the JMS Queue.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException
    {
        if ( inputObject == null )
            return null;

        
        try
        {
        	
            if(lookupDomainApiFlag && !DomainProperties.getInstance(customerId).getIsAPIEnabledFlag())
            {
                Debug.log(Debug.ALL_WARNINGS, LOGGING_CLASS_NAME
                        +": Customer ["+customerId+"] is not configured as an API customer. Skipping event message enqueing..." );
                
                return formatNVPair ( inputObject );
            }
        }
        catch(DomainPropException dpe)
        {
             throw new ProcessingException ( "ERROR: "+ LOGGING_CLASS_NAME + ": Could not fetch value for IsAPIEnabled flag from Domain properties.");
        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, LOGGING_CLASS_NAME+": Processing ... " );

       // Read the property values from the input into 'messageProperties'.
        extractMsgPropertyValues(mpContext, inputObject);

        // if alternate db props file is configured, extract the properties.
        if (StringUtils.hasValue(dbPropFileLoc))
            extractAlternateDBProp (mpContext,inputObject);

        // if available, get the Alternate Database Pool Key from specified location.
        if (StringUtils.hasValue(alternateDBPoolKeyLoc) && exists(alternateDBPoolKeyLoc, mpContext, inputObject))
        {
            alternateDBPoolKey = (String) get(alternateDBPoolKeyLoc, mpContext, inputObject);
        }
            

        try
        {
            this.mpContext = mpContext;

            
            // Connect to JMS Provider.
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, LOGGING_CLASS_NAME+": Connecting to JMS Provider ... " );
            
            connect();

            // Try to get queue name from the context or input, only in case of QUEUE_NAME_LOC is configured,
            // Otherwise use QueueName as in previous.

            if(StringUtils.hasValue(queueNameLoc) && exists(queueNameLoc, mpContext, inputObject)){

                 String queueNameFromLoc = getString (queueNameLoc, mpContext, inputObject);
                 if( StringUtils.hasValue(queueNameFromLoc) )
                         queueName = queueNameFromLoc;
            }

            if(!StringUtils.hasValue(queueName))
                    throw new FrameworkException("Queue Name is not at [" + queueNameLoc + "]");

            // Send the message to JMS Queue.
            sendMessage(inputObject.getString());
            
        }
        catch (Exception e)
        {
            Debug.log(Debug.ALL_ERRORS, "Exception occured while enqueing message :"+e.getMessage());
            Debug.logStackTrace(e);
            throw new ProcessingException("ERROR: "+ LOGGING_CLASS_NAME + 
                    ": Failed to Queue the Event into the Queue, Reason: " + e.toString());
        }
        finally
        {
            /* if not transactional then do the clean-up now ! */
			if (!isJMSSessionTransactional()
					|| StringUtils.hasValue(dbPropFileLoc)
					|| StringUtils.hasValue(alternateDBPoolKey))
			{
                try
                {
                    // Disconnect from JMS Provider.
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, 
                                LOGGING_CLASS_NAME+"Disconnecting from JMS Provider " );
                    
                    disconnect();
                }
                catch(Exception e)
                {
                    Debug.logStackTrace(e);
                }
            }
			
        }

        // Always return input value to provide pass-through semantics.
        return ( formatNVPair ( inputObject ) );
    }

    /**
     * Method to check whether JMS Session created 
     * is transactional or not.
     * @return boolean
     */
    public boolean isJMSSessionTransactional() {
        return createTransactionalSession;
    }

    /**
     * Connects to the JMS Provider, in order to produce messages. This method acquires a Connection
     * which provides a virtual connection with a JMS provider, and also acquires a Session Object
     * which provides a transactional context to operate on JMS queues.
     *
     * @exception  Exception  Thrown on errors.
     */
    public void connect() throws Exception
    {
        // Acquire an instance of JMS Portability Layer, for invoking vendor specific methods.
        jpl = new JMSPortabilityLayer();

        if(StringUtils.hasValue(dbPropFileLoc))
        {
            // Obtain non-pool DB connection for secondary install
            dbconn = getNewDBConnection();

            // Acquire a JMS Connection from db connection aquired
            queueConnection = jpl.createQueueConnection(dbconn);
        }
        else if (StringUtils.hasValue(alternateDBPoolKey))
        {
        	if(!initializedAltDbPool)
        	{
        		synchronized (JMSQueueProducer.class) {
					
        			if(!initializedAltDbPool)
        			{
        				if(Debug.isLevelEnabled(Debug.DB_BASE))
        					Debug.log(Debug.DB_BASE, "JMSQueueProducer: initializing db pools... ");
        				
        				DBConnectionPool.initializePoolConfigurations();
        				initializedAltDbPool = true;
        			}
				}
        	}
        	
        	String currentPoolKey = DBConnectionPool.getInstance().getThreadSpecificPoolKey();
            try
            {
            	DBConnectionPool.getInstance().setThreadSpecificPoolKey(alternateDBPoolKey);
                dbconn = DBConnectionPool.getInstance(alternateDBPoolKey).acquireConnection();
            }
            finally
            {
            	DBConnectionPool.getInstance().setThreadSpecificPoolKey(currentPoolKey);
            }

            queueConnection = jpl.createQueueConnection(dbconn);

            queueSession = jpl.createQueueSession(queueConnection);

            return;
        }
        else if(isJMSSessionTransactional())
        {
            queueConnection = 
                jpl.createQueueConnection(mpContext.getDBConnection());
            
            queueSession = 
                jpl.createTransactedQueueSession(queueConnection);
            
            return;
        }
        else
        {
            // Acquire a JMS Connection for db pool connection
            queueConnection = JMSConnection.acquireQueueConnection(jpl);
        }

        // Acquire a JMS Session.
        queueSession = JMSSession.acquireQueueSession(jpl, queueConnection);
    }

    /**
     * Provides a SQL connection
     *
     * @exception  ProcessingException  Thrown on errors.
     */
    private Connection getNewDBConnection() throws ProcessingException
    {
        Connection dbConn =null;

        try
        {
            initAlternateDB();
            dbConn = DBInterface.newConnection(true);
        }
        catch(DatabaseException dbe)
        {
            Debug.error(LOGGING_CLASS_NAME + ":getNewDBConnection(): Unable to get database connection: " + dbe.getMessage());
            throw new ProcessingException("Unable to get DB connection ");
        }
        catch(Exception e)
        {
            Debug.error(LOGGING_CLASS_NAME + ":getNewDBConnection(): Unable to get database connection: " + e.getMessage());
            throw new ProcessingException("Unable to get DB connection ");
        }

        return dbConn;
    }

    /**
     * This method initialize the alternate DB using properties read from alteranate_db.properties file.
     *
     * @exception FrameworkException thrown if DB could not be initialized  
     */
    private void initAlternateDB() throws FrameworkException
    {
        try
        {
            if(!alternateDBProps.isEmpty())
            {
                String dbName = (String) alternateDBProps.get(DBInterface.DB_NAME_PROPERTY);
                String dbUser = (String) alternateDBProps.get(DBInterface.DB_USER_PROPERTY);
                String encryptedPass = (String) alternateDBProps.get(DBInterface.DB_PASSWORD_PROPERTY);
                String dbPassword  = Crypt.decrypt(encryptedPass);

                if(StringUtils.hasValue(dbName) && StringUtils.hasValue(dbPassword) && StringUtils.hasValue(dbUser))
                {
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME + ":initAlternateDB(): Initializing DB for User [" + dbUser + "]" );
                    DBInterface.initialize(dbName, dbUser, dbPassword);
                }
                else
                {
                    Debug.error(LOGGING_CLASS_NAME + ": initAlternateDB(): Missing properties for alternate DB ");
                    throw new ProcessingException(" Missing properties for alternate DB");                    
                }
            }
            else
            {
                Debug.error(LOGGING_CLASS_NAME + ": initAlternateDB(): No properties found for alternate DB ");
                throw new ProcessingException("No properties found for alternate DB ");
            }
        }
        catch(FrameworkException exp)
        {
            Debug.logStackTrace(exp);
            Debug.error(LOGGING_CLASS_NAME + ": initAlternateDB(): Failed initializing alterante DB connection "+ exp.toString());
            throw new FrameworkException(exp);
        }
        catch(Exception exp)
        {
            Debug.logStackTrace(exp);
            Debug.error(LOGGING_CLASS_NAME + "initAlternateDB(): Failed initializing alterante DB connection "+exp.getMessage());
            throw new ProcessingException(exp);
        }
    }

    /**
     * Creates a JMS producer using which a message is sent to the JMS Queue.
     *
     * @exception  Exception  Thrown on errors.
     */
    public void sendMessage( String message ) throws Exception
    {
    	
        // Stores the Customer Context CustomerId value
        String customerContextCustomerId;

        // Create a JMS Producer instance, for sending messages to JMS queue.
        JMSProducer producer = new JMSProducer(jpl, queueSession);

        // Create a Message Sender Client for specified queue
        producer.createQueueProducer(queueName);

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, LOGGING_CLASS_NAME+": Created Message Sender Client.. ");

        // Some apps are explicitly setting Customeridentifier in header properties using iterative properties
        // If CustomerId is not setted in iterative properties then get the CustomerId from CustomerContext.
        if(useCustomerId && !isCustomerIdMsgProperty)
        {
            // Get the CustomerId from Customer Context
            customerContextCustomerId = CustomerContext.getInstance().getCustomerID();
            if(StringUtils.hasValue(customerContextCustomerId))
            {
                messageProperties.put(CUSTOMER_ID_PROP, customerContextCustomerId);
                
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, 
                            LOGGING_CLASS_NAME+": Setting property [" + CUSTOMER_ID_PROP + "] with value [" + customerContextCustomerId + "]");
            }
        }

        // Add the UniqueIdentifier available in the customer-context to this jms-header, so that it is picked-up
        // by the jms-consumer, set in request-header and propogated to the rcving gateway.
        String uniqueID = CustomerContext.getInstance().getMessageId();
        if(StringUtils.hasValue(uniqueID))
        {
            messageProperties.put(CustomerContext.MESSAGE_ID, uniqueID);
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, 
                        LOGGING_CLASS_NAME+": Setting property [" + CustomerContext.MESSAGE_ID 
                        + "] with value [" + uniqueID + "]");
        }

        if(setServiceType)
        {
        	String custId = CustomerContext.getInstance().getCustomerID();
        	try{
        		messageProperties.put(DomainProperties.SERVICE_TYPE, DomainProperties.getInstance(custId).getServiceType());	
        	}catch(Exception ex)
        	{
        		Debug.warning("Unable to get SERVICE_TYPE for customer :"+custId);
        	}
        	
        }
        
        // If message properties are configured, set the message along with the properties
        // else, set the message only.
        if(!messageProperties.isEmpty())
            producer.setMessage(message, messageProperties);
        else
            producer.setMessage(message);

        // Start the Connection
        JMSConnection.startQueueConnection(jpl, queueConnection);

        // Send the Message
        producer.sendMessage();
        
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,LOGGING_CLASS_NAME+": Succesfully sent the message... " );
    }


    /**
     * Disconnects from the JMS Provider. This method releases the Connection and Session objects to
     * the JMS provider.
     *
     * @exception  Exception  Thrown on errors.
     */
    public void disconnect() throws Exception
    {
        // Release the Session
        if(queueSession!=null)
        {
            JMSSession.closeQueueSession(jpl, queueSession);
        }

        if (queueConnection == null)
        {
            Debug.log ( Debug.MSG_STATUS, LOGGING_CLASS_NAME+":disconnect(): No queue connection established returning... " );
            return;
        }
        
        
        if(StringUtils.hasValue(alternateDBPoolKey) && dbconn !=null)
        {
        	String currentPoolKey = DBConnectionPool.getInstance().getThreadSpecificPoolKey();
        	try
        	{
        		DBConnectionPool.getInstance().setThreadSpecificPoolKey(alternateDBPoolKey);
        		DBConnectionPool.getInstance(alternateDBPoolKey).releaseConnection(dbconn);
        	}catch(Exception ignore){
        	}
        	finally{
        		DBConnectionPool.getInstance().setThreadSpecificPoolKey(currentPoolKey);
        	}
        	return;
        }
        
        // Release the Connection
        if (dbconn == null)
        {
            JMSConnection.closeQueueConnection(jpl, queueConnection);
        }
        else
        {
            JMSConnection.closeQueueConnection(jpl,queueConnection,false);
            dbconn.close();
            dbconn = null;
        }

    }

    /**
     * Extract data for alternate DB connection .
     *
     * @param context The message context.
     * @param inputObject The input object.
     *
     * @exception MessageException thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private void extractAlternateDBProp ( MessageProcessorContext context, MessageObject inputObject )
    throws ProcessingException, MessageException
    {
        String dbPropFilePath = null;
        alternateDBProps = new Properties();

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log( Debug.MSG_DATA, LOGGING_CLASS_NAME + ":extractAlternateDBProp(): Extracting alternate database properties from file located at  ["+ dbPropFileLoc + "]" );

            // Attempt to get the value from the context or input object.Throw ProcessingException if value not found .
            if ( exists( dbPropFileLoc, context, inputObject ) )
            {
                dbPropFilePath = (String) get( dbPropFileLoc, context, inputObject );

                // If we found a value load properties
                if ( StringUtils.hasValue ( dbPropFilePath ) )
                {
                    try{
                            FileUtils.FileStatistics dbFileSats = FileUtils.getFileStatistics(dbPropFilePath);
                            if ( dbFileSats.exists  && dbFileSats.readable && dbFileSats.isFile )
                            {
                                FileUtils.loadProperties(alternateDBProps, dbPropFilePath);
                                if (Debug.isLevelEnabled(Debug.MSG_DATA))
                                    Debug.log(Debug.MSG_DATA, LOGGING_CLASS_NAME 
                                            + ": Successfully loaded properties file [" + dbPropFilePath +"]");
                            }
                            else
                            {
                                Debug.error(LOGGING_CLASS_NAME + ": extractAlternateDBProp(): Could not read DB property file [" + dbPropFilePath + "]");
                                throw new ProcessingException("Could not read DB property file [" + dbPropFilePath + "]");
                            }
                    }catch(FrameworkException fe)
                    {
                        Debug.logStackTrace(fe);
                        throw new ProcessingException("Could not load properties from ["+ dbPropFilePath+ "]" + fe.getMessage());
                    }
                    catch(Exception e)
                    {
                        Debug.logStackTrace(e);
                        throw new ProcessingException("Could not load properties from ["+dbPropFilePath+"]"+ e.getMessage());
                    }
                }
                else
                {
                    throw new ProcessingException("No file path found at location " + dbPropFileLoc + Debug.getStackTrace());
                }
            }
            else
            {
                Debug.error(LOGGING_CLASS_NAME + ":extractAlternateDBProp(): No file exists at location " + dbPropFileLoc);
                throw new ProcessingException("No file exists at location " + dbPropFileLoc + Debug.getStackTrace());
            }
    }
    /**
     * Extract data for each property to be set in message-header from the input message/context.
     *
     * @param context The message context.
     * @param inputObject The input object.
     *
     * @exception MessageException thrown if required value can't be found.
     * @exception ProcessingException thrown if any other processing error occurs.
     */
    private void extractMsgPropertyValues ( MessageProcessorContext context, MessageObject inputObject )
    throws ProcessingException, MessageException
    {
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, 
                    LOGGING_CLASS_NAME + ": Extracting properties to set as message properties ..." );

        Iterator iter = messagePropertiesConfig.iterator( );

        String propertyValue;
        messageProperties = new Properties();

        while(iter.hasNext())
        {
            propertyValue = null;
                
            messagePropertyConfiguration qc = (messagePropertyConfiguration)iter.next( );

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, LOGGING_CLASS_NAME 
                        + ": Fetching value for Property ["+ qc.propertyName+ "]" );

            // If location was given, try to get a value from it.
            if(StringUtils.hasValue(qc.location))
            {
                // Location contains one or more alternative locations that could
                // contain the property value.
                StringTokenizer st = new StringTokenizer(qc.location, separator);

                // While location alternatives are available ...
                while ( st.hasMoreTokens() )
                {
                    // Extract a location alternative.
                    String loc = st.nextToken().trim( );

                    // Attempt to get the value from the context or input object.
                    if ( exists( loc, context, inputObject ) )
                    {
                        propertyValue = (String) get( loc, context, inputObject );

                        // If we found a value, we're done with this column.
                        if ( StringUtils.hasValue ( propertyValue ) )
                        {
                            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                                Debug.log(Debug.MSG_DATA, 
                                        LOGGING_CLASS_NAME + " Found value for property " 
                                        + qc.propertyName + " at location [" + loc + "]." );

                            if(qc.propertyName.equals(EVENT_CHANNEL_NAME_PROP))
                            {
                                try {
                                    String cid = CustomerContext.getInstance().getCustomerID();
                                    propertyValue = propertyValue+"_"+cid;
                                } catch (FrameworkException e) {
                                    e.printStackTrace();
                                    throw new ProcessingException("Unable to get customerId from Customer context.");
                                }
                            }
                            messageProperties.setProperty ( qc.propertyName, propertyValue );

                            break;
                        }
                    }
                }
            }

            // If no value was obtained from location in context/input, try to set it from default value (if available).
            if ( !StringUtils.hasValue ( propertyValue ) )
            {
                propertyValue = qc.defaultValue;
                if(qc.propertyName.equals(EVENT_CHANNEL_NAME_PROP))
                {
                    try {
                           String cid = CustomerContext.getInstance().getCustomerID();
                           propertyValue = propertyValue+"_"+cid;
                        } catch (FrameworkException e) {
                             e.printStackTrace();
                             throw new ProcessingException("Unable to get customerId from Customer context.");
                          }
                }

                if(propertyValue != null)
                {
                    if(Debug.isLevelEnabled(Debug.MSG_DATA))
                        Debug.log(Debug.MSG_DATA, 
                                LOGGING_CLASS_NAME + ": Using default value for column." );

                    messageProperties.setProperty ( qc.propertyName, propertyValue );
                }
                else if (!qc.optional)
                {
                    // Signal error to caller.
                    throw new MessageException( "ERROR: "+ LOGGING_CLASS_NAME + ": Could not locate required value for property [" + qc.describe()
                                               + "] used to set the message properties" );
                }
            }
        }

        propertyValue = null;

        // Fetching the value of Retry-Count message property.

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, 
                    LOGGING_CLASS_NAME + ": Fetching value for Property ["+ RETRY_COUNT_NAME_PROP + "]" );

        if ( StringUtils.hasValue(retryCountLocation) )
        {
            // Attempt to get the value from the context or input object.
            if ( exists( retryCountLocation, context, inputObject ) )
            {
                propertyValue = (String) get( retryCountLocation, context, inputObject );

                // If we found a value, we're done with this column.
                if ( StringUtils.hasValue ( propertyValue ) )
                {
                    if(Debug.isLevelEnabled(Debug.MSG_DATA))
                        Debug.log ( Debug.MSG_DATA, LOGGING_CLASS_NAME + ": Found value for property " + RETRY_COUNT_NAME_PROP + " at location [" + retryCountLocation + "]." );

                    int retryCount = Integer.valueOf(propertyValue).intValue();

                    propertyValue = Integer.toString(++retryCount);

                    messageProperties.setProperty ( RETRY_COUNT_NAME_PROP, propertyValue );
                }
            }
        }

        // If no value was obtained from location in context/input, try to set it from default value (if available).
        if ( !StringUtils.hasValue ( propertyValue ) )
        {
            propertyValue = RETRY_COUNT_VALUE_DEFAULT_PROP;

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, 
                        LOGGING_CLASS_NAME + ": Using default value for column." );

            messageProperties.setProperty ( RETRY_COUNT_NAME_PROP, propertyValue );

        }

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, 
                    LOGGING_CLASS_NAME + ": Done extracting message data to used to set the message property." );
        
        /* set the instance id, if it is available */
        if(context.exists(MessageProcessorExecutor.PROCESSED_BY_GWS))
        	messageProperties.setProperty(MessageProcessorExecutor.PROCESSED_BY_GWS, 
        			context.getString(MessageProcessorExecutor.PROCESSED_BY_GWS));
        
    }
    
    /**
     * Cleanup is required only in the case of transactional session.
     * This is to ensure that JMS resources are cleaned up after commit/rollback
     * is called by MessageProcessingDriver on Context's connection.  
     */
    @Override
    public void cleanup()
    {
       if(isJMSSessionTransactional())
       {
           try
           {
               /* close queue resources here, JMSPortabilityLayer isn't been used
                * since it would close the database connection too, which we don't want*/
               
               if(queueSession!=null)
                   queueSession.close();
               
               if(queueConnection!=null)
                    queueConnection.close();
           }               
           catch(javax.jms.JMSException ignore) {}
       }
    }

    // Stores the name of the queue to which the message has to be sent.
    private boolean lookupDomainApiFlag;

    // Stores the name of the queue to which the message has to be sent.
    private String queueName = null;

    // Queue name location.
    private String queueNameLoc = null;

    // Stores the CustomerId.
    private String customerId = null;

    // Stored the location of RETRY-COUNT property used for customized message selection by the consumer.
    private String retryCountLocation = null;

    // Stores the name of USE_CUSTOMER_ID property
    private boolean useCustomerId = true;

    // Stores the name of USE_CUSTOMER_ID property
    private boolean isCustomerIdMsgProperty = false;

    // List for holding configuration for all message properties
    private List messagePropertiesConfig = null;

    // Stores message property name and value
    private Properties messageProperties = null;

    private String separator = null;

    // Stores an instance of a JMS Queue Connection.
    private QueueConnection queueConnection = null;

    // Stores an instance of a JMS Queue Session.
    private QueueSession queueSession = null;

    // Stores an instance of JMS Portability Layer, for invoking vendor specific methods.
    private JMSPortabilityLayer jpl = null;

    // Stores datebase connection for alternate installation user.
    private Connection dbconn =null;

    // Stores property file location where database connection info is stored for an alternate DB.
    private String dbPropFileLoc = null;

    //stores alternate DB properties
    private Properties alternateDBProps =null;

    //stores alternate Database pool key
    private String alternateDBPoolKeyLoc = null;
    private String alternateDBPoolKey = null;
    private static boolean initializedAltDbPool = false;

    // Class encapsulating configurations related to a message property.
    private static class messagePropertyConfiguration
    {
        public final String propertyName;
        public final String location;
        public final String defaultValue;
        public final boolean optional;

        public messagePropertyConfiguration ( String propertyName, String location, String defaultValue, String optional )
        {
            this.propertyName  = propertyName;
            this.location      = location;
            this.defaultValue  = defaultValue;

            // By default, value of Optional property is 'false'
            this.optional = StringUtils.getBoolean ( optional, false );
        }

        public String describe()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(LOGGING_CLASS_NAME); 
            sb.append(": messagePropertyConfiguration: Message Property Configuration: Name [" );
            sb.append(propertyName);
            sb.append ( "], location of the value  [" );
            sb.append ( location );

            if(StringUtils.hasValue(defaultValue))
            {
                sb.append("], default value [");
                sb.append(defaultValue);
            }

            sb.append("], optional [");
            sb.append(optional);

            sb.append("].");

            return  sb.toString();
        }//describe

    }//end of class messagePropertyConfiguration

    public static void main(String[] args) throws Exception
    {

         Properties props = new Properties();
         props.put("DEBUG_LOG_LEVELS", "0 1");
         props.put("LOG_FILE", "d:\\QueueProducer.log");
         Debug.configureFromProperties(props);

         DBInterface.initialize("jdbc:oracle:thin:@192.168.148.34:1521:NOIDADB", "SOADB", "SOADB");

         String xmlText = "<?xml version=\"1.0\"?><node>hello world</node>";//FileUtils.readFile( "d:\\JMSTextMessage.xml" );
         Document doc =  XMLLibraryPortabilityLayer.convertStringToDom(xmlText);
         MessageObject input = new MessageObject(doc);
         MessageProcessorContext ctx = new MessageProcessorContext();
         JMSQueueProducer producer = new JMSQueueProducer();
         producer.initialize("TEST_QUEUE_PRODUCER","JMSQueueProducer");
         try
         {
                producer.process(ctx,input);
         }
         catch(Exception e)
         {
             if(args.length==1)
             {
                 System.out.println("rolling back...");
                 ctx.getDBConnection().rollback();
             }
             else
             {
                 System.out.println("commiting ...");
                 ctx.getDBConnection().commit();
                 producer.cleanup();
             }
         }
    }
}

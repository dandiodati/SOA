package com.nightfire.comms.jms;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NFConstants;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.jms.AbstractJmsMsgStoreDAO;
import com.nightfire.framework.jms.JMSConstants;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.jms.JMSException;
import com.nightfire.framework.jms.JmsMsgStoreDAO;
import com.nightfire.framework.jms.JmsMsgStoreDAOFactory;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.SQLUtil;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.resource.ResourceException;

import com.nightfire.spi.common.communications.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.common.ProcessingException;
import com.nightfire.servers.CosEvent.EventChannelUtil;
import com.nightfire.comms.eventchannel.MultiCustomerEventConsumerServer;
import com.nightfire.security.domain.DomainProperties;
import com.nightfire.security.domain.DomainPropException;
import com.nightfire.security.rbacprovision.RBACDatabaseConstants;
import com.nightfire.security.tpr.TradingPartnerRelationship;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Set;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * CommServer that consumes messages from JMS queues.It registers JMS Message Listeners on
 * queues for customers as per configuration. For customers whose queue name is not configured
 * the messages are by default recieved from COMMON_EVENT_QUEUE. After recieving messages it initializes
 * the driver chain and accordingly the chain forwards messages to a SOAP URL.
 */
public class  MultiCustomerJMSConsumerServer extends ComServerBase
                                            implements JMSConsumerCallBack
{

	public static final String CUSTOMER_CONTEXT = "customer-context";
    private static final String SUPPLIER = "Supplier";
    private static final String TRANSACTION = "Transaction";
    private static final String SERVICE_TYPE = "SERVICE_TYPE";
    private static final String PROP_LISTENER_CONSUMER = "LISTENER_CONSUMER";
    private static final String CHANNEL_NAME_BASE = "CHANNEL_NAME_BASE";
    private static final String PROP_USE_ALTERNATE_DB_QUEUES = "USE_ALTERNATE_DB_QUEUES";
    private static final String PROP_ALTERNATE_DB_POOL_KEY = "ALTERNATE_DB_POOLKEY";

    private String altDbPoolKey = null;
    
    private boolean listenerFlag = true,useAlternateDbQueues = false , clientAck = false;
    /**
	 * Set containing all the customer ids for which message listener needs to be started.
	 */
    private Set<String> customerIDSet = new TreeSet<String>();
    /**
     * If Thread Pool is on then MAX_POOL_SIZE and CORE_POOL_SIZE
     * are required parameters for configuring Thread Pool.
     */
    private static String MAX_POOL_SIZE_PROP = "MAX_THREAD_POOL";
    private static String CORE_POOL_SIZE_PROP =   "CORE_POOL_SIZE";
    private int maxPoolSize, corePoolSize;
    private Map<String,JMSConsumerHelper> qNmConsumerMap = new HashMap<String,JMSConsumerHelper>();

    private static String SRMQUEUE_AGENT_PROP_TYPE = "SOAPResponseMessageQueueAgent";
    private static String WHERE_CRITERIA_PROP_NM = "WHERE_CRITERIA";

    /* Constant required for message store functionality */
    private static final String USE_CLIENT_ACK_PROP = "USE_CLIENT_ACK";

    private Connection dbConn = null;
    private Vector vColumns = new Vector ();
    private Vector vAllRows, vRow;
    
    private Hashtable htWhere = new Hashtable();
    StringBuffer errorBuffer = new StringBuffer();
    String propValue = null;
    private String jmsMsgStoreTableNm = null;


    /**
	 * Constructor
	 * @exception  ProcessingException
	 * @exception  ResourceException
	 */
    public MultiCustomerJMSConsumerServer(String key, String type)
                    throws ProcessingException
	{
        super(key,type);

        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Initializing MultiCustomerJMSConsumerServer....");

        try
        {
            propValue = PersistentProperty.get(key, SRMQUEUE_AGENT_PROP_TYPE, WHERE_CRITERIA_PROP_NM);
        }
        catch(Exception pe)
        {
            Debug.warning("Property type :"+SRMQUEUE_AGENT_PROP_TYPE+" name:"+WHERE_CRITERIA_PROP_NM
                    + " not configured.");
        }

        
        try
        {
             String useDbPool = PersistentProperty.get(key, type, PROP_USE_ALTERNATE_DB_QUEUES);
             useAlternateDbQueues = Boolean.parseBoolean(useDbPool);

             if(useAlternateDbQueues)
             {
                 altDbPoolKey = PersistentProperty.get(key, type,PROP_ALTERNATE_DB_POOL_KEY);
                 
            	 Debug.log(Debug.STATE_LIFECYCLE, "An alternate database is configured for queues :"
                         +"ALTERNATE_DB_POOL_KEY :"+altDbPoolKey);

             }
        }
        catch(Exception pe)
        {
            useAlternateDbQueues = false;
            Debug.warning(" Property "+PROP_USE_ALTERNATE_DB_QUEUES+ " is not configured..");
        }
        
        try 
        {
            String custServiceType = getRequiredPropertyValue(SERVICE_TYPE);
            String prop = getPropertyValue(PROP_LISTENER_CONSUMER);
            
            /**
             * From R3.15.4, default value would be true, ie. JMS Message Listeners will be created
             * to dequeue messages. If its value is true then JMS Poll Consumers will
             * be created to dequeue messages.
             */
            if(StringUtils.hasValue(prop))
                listenerFlag = Boolean.parseBoolean(prop);
            Debug.log(Debug.STATE_LIFECYCLE,"Create listener consumers :"+listenerFlag);

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS, 
                    "[SOAPResponseMessageQueueAgent,WHERE_CRITERIA] value configured is:"+propValue);

            /* if its value has a list of customers then those needs to be served by this SRM ! */
            if(propValue!=null && propValue.indexOf("(") > 0 && propValue.indexOf("customerid") >0)
            {
                Debug.log(Debug.NORMAL_STATUS, "customer's exists in WHERE_CRITERIA of " +
                                                " SOAPResponseMessageQueueAgent"
                                                +" value is :"+propValue);
                
                String cidLst = propValue.substring(propValue.indexOf("(")+1,propValue.indexOf(")"));
                String newCidLst = cidLst.replace('\'', ' ');
                StringTokenizer strTok = new StringTokenizer(newCidLst,",");
                while(strTok.hasMoreElements())
                {
                    String cid = (String)strTok.nextElement();
                    
                    if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                        Debug.log(Debug.NORMAL_STATUS, "Got customerid :"+cid);
                    
                    customerIDSet.add(cid.trim());
                }
            }
            else
            {
                vColumns.add(RBACDatabaseConstants.CHDOMAIN_T_CUSTOMERID);
                htWhere.put(SERVICE_TYPE,custServiceType);
                dbConn = DBConnectionPool.getInstance(NFConstants.NF_SECURITY_POOL).acquireConnection();
                /**
                 * The fetchRows function of SQLUtil return the vector containing
                 * vectors as a row containing the column data in it
                 */
                vAllRows = SQLUtil.fetchRows(dbConn, RBACDatabaseConstants.CHDOMAIN_T, vColumns, htWhere);
                if(vAllRows!=null)
                {
                    // Obtaining first row of the resultset
                    for(int i=0;i<vAllRows.size();i++)
                    {
                        vRow = (Vector) vAllRows.get(i);
                        customerIDSet.add((String)vRow.get(0));
                    }
                }
                else
                    Debug.log(Debug.ALL_WARNINGS,"No Customers found in DOMAIN table");
            }
            
            /* getting maximum pool size */
            String tmp = getPropertyValue(MAX_POOL_SIZE_PROP);
            if(StringUtils.hasValue(tmp))
            {
                try
                {
                    maxPoolSize = StringUtils.getInteger(tmp);
                    Debug.log(Debug.STATE_LIFECYCLE,"Using maxPoolSize:"+maxPoolSize);
                }
                catch(FrameworkException fe)
                {
                    Debug.log(Debug.ALL_WARNINGS,"Invalid value [" +tmp+"] for property ["+ MAX_POOL_SIZE_PROP+"]. " +
                            "Using Default value 10");
                    errorBuffer.append("Invalid value [" +tmp+"] for property ["+ MAX_POOL_SIZE_PROP+"].");
                }
            }
            else
                maxPoolSize = 10;

            /* getting core pool size */
            tmp = getPropertyValue(CORE_POOL_SIZE_PROP);
            if(StringUtils.hasValue(tmp))
            {
                try
                {
                    corePoolSize = StringUtils.getInteger(tmp);
                    Debug.log(Debug.STATE_LIFECYCLE,"Using corePoolSize:"+corePoolSize);
                }
                catch(FrameworkException fe)
                {
                    Debug.log(Debug.ALL_WARNINGS, "Invalid value [" +tmp+"] for property ["+ CORE_POOL_SIZE_PROP+"]. Using Default value 5");
                    errorBuffer.append("Invalid value [" +tmp+"] for property ["+ CORE_POOL_SIZE_PROP+"].");
                }
            }
            else
                corePoolSize = 5;

            if(corePoolSize > maxPoolSize)
            {
                Debug.log(Debug.ALL_WARNINGS, CORE_POOL_SIZE_PROP+" value should be less than "+MAX_POOL_SIZE_PROP+" value.");
                errorBuffer.append("Invalid values for Pool Size. "+CORE_POOL_SIZE_PROP+" value should be less than "+MAX_POOL_SIZE_PROP+" value.");
            }
            
            String clientAckStr = getPropertyValue(USE_CLIENT_ACK_PROP);
            if(StringUtils.hasValue(clientAckStr))
            {
                clientAck = Boolean.parseBoolean(clientAckStr);

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "client ack mode is : "+clientAck);
            }

            jmsMsgStoreTableNm = getPropertyValue(JmsMsgStoreDAOFactory.MSG_STORE_TABLE_PROP_NAME);
            if(!StringUtils.hasValue(jmsMsgStoreTableNm))
            {
                jmsMsgStoreTableNm = JmsMsgStoreDAO.DEFAULT_TABLE_NAME;
            }
            
            /* If any of the required properties are absent, throw an exception */
            if(errorBuffer.length() > 0)
            {
                String errMsg = errorBuffer.toString();
                Debug.log(Debug.ALL_ERRORS,errMsg);
                throw new ProcessingException(errMsg);
            }

            /* init consumer helpers */
            createConsumerHelpers();
        } 
        catch (DatabaseException e) 
        {
            Debug.log(Debug.ALL_ERRORS,"An exception occured while accessing database : "+e.getMessage());
            Debug.log(Debug.ALL_ERRORS,Debug.getStackTrace(e));
            throw new ProcessingException(e);
        } 
        catch (DomainPropException e) 
        {
            Debug.log(Debug.ALL_ERRORS,"An exception occured while accessing domain properties : "+e.getMessage());
            Debug.log(Debug.ALL_ERRORS,Debug.getStackTrace(e));
            throw new ProcessingException(e);
        }
        catch (Exception e) 
        {
            Debug.log(Debug.ALL_ERRORS,"An exception occured while initializing JMSConsumer Server : "+e.getMessage());
            Debug.log(Debug.ALL_ERRORS,Debug.getStackTrace(e));
            throw new ProcessingException(e);
        }
        finally 
        {
            if(dbConn!=null)
            try{
                DBConnectionPool.getInstance(NFConstants.NF_SECURITY_POOL).releaseConnection(dbConn);
            }catch(ResourceException ignore){
                Debug.log(Debug.ALL_ERRORS,"An exception occured while releasing connection "+ignore.getMessage());
            }
        }

        Debug.log(Debug.STATE_LIFECYCLE,"Completed Initializing MultiCustomerJMSConsumer for following ["
                +customerIDSet.size()
                +"] customers:\n"
                + customerIDSet.toString());
    }


	/**
	 * Method to start JMSConsumerHelpers
     * @throws DomainPropException
	 */
	private void createConsumerHelpers() throws DomainPropException {
		// create all queue consumers
        String queueNm;
        String msgSelector;
        boolean isAPICustomer;
        boolean isNotificationEnabled;

        for(String customerId : customerIDSet)
		{
			msgSelector = null;
            queueNm = DomainProperties.getInstance(customerId).getEventQueueName();
            isAPICustomer = DomainProperties.getInstance(customerId).getIsAPIEnabledFlag();
            isNotificationEnabled = DomainProperties.getInstance(customerId).isNotificationEnabled();
            if(isAPICustomer || isNotificationEnabled)
            {
                    
                    JMSConsumerHelper helper = (JMSConsumerHelper)qNmConsumerMap.get(queueNm);
                    
                    /* if a consumer already exists, construct its msgSelector */
                    if(helper!=null)
                    {
                        /* append CustomerIdentifier to msgSelector */ 
                        String selector = helper.getMsgSelector();
                        if(selector!=null)
                        {
                            /* USE IN clause */ 
                            selector = selector.substring(0, selector.indexOf(')')) + ",'"+customerId+"')";
                        }
                        else
                            selector = "CustomerIdentifier IN ('"+customerId+"')";    
                        
                        helper.setMsgSelector(selector);
                    }
                    else
                    {
                        
                        msgSelector = "CustomerIdentifier IN ('"+customerId+"')";
                        
                        /* instantiate a new jms consumer */
                        if(useAlternateDbQueues)
                            helper  = new JMSConsumerHelper(queueNm,customerId,msgSelector,listenerFlag,true,
                                    corePoolSize,maxPoolSize,altDbPoolKey);
                        else
                            helper  = new JMSConsumerHelper(queueNm,customerId,msgSelector,listenerFlag,true,
                                    corePoolSize,maxPoolSize);
                            
                        
                        qNmConsumerMap.put(queueNm, helper);
                        
                        if(clientAck)
                        {
                            helper.setClientAckMode(true);
                            helper.setConsumerNm(type);
                            helper.setGatewayNm(key);
                        }
                    }
                    
                    if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                        Debug.log(Debug.NORMAL_STATUS, getClass().getSimpleName() 
                                + "Using ["+ queueNm+"]  for customer ["+ customerId +"] : "
                                + StringUtils.getClassName(this) + "Created JMSConsumerHelper " 
                                + "for customer [" + customerId +"].");
            }
            else
                Debug.warning(getClass().getSimpleName() 
                        + "Skipping JMSConsumerHelper creation for non api customer [" 
                        + customerId +"]. Events for this customer would not be consumed.");
            
        }
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, getClass().getSimpleName() 
                + "Created total ["+qNmConsumerMap.size()+"] consumer helpers for queues {"+qNmConsumerMap.keySet()+"}");
    }

	public void run()
	{
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		registerConsumerHelpers();
	}
	
	/**
	 * Method to shutdown the CommServer.
	 * This will close all the message listeners registered on queues and
	 * release database connections.
	 */
	public void shutdown()
	{
         if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
		       Debug.log(Debug.NORMAL_STATUS, getClass().getSimpleName()+
					      ": Received a request to shutdown.");

		disConnectAll();

        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
          Debug.log(Debug.NORMAL_STATUS, getClass().getSimpleName() +
                         ": Closing all DbConnections.");
        
        /* close DbConnections */
        for(Connection dbConn : dbConnLst)
        {
            try
            {
                if(useAlternateDbQueues)
                {
                	DBConnectionPool.setThreadSpecificPoolKey(altDbPoolKey);
                    DBConnectionPool.getInstance(altDbPoolKey).releaseConnection(dbConn);
                    DBConnectionPool.setThreadSpecificPoolKey(DBConnectionPool.getDefaultResourceName());
                }
                else
                    DBConnectionPool.getInstance().releaseConnection(dbConn);
            }
            catch(Exception ignore)
            {
                Debug.error("An exception occured while releasing dbConnection "+ignore.toString());
            }
        }
	}

	/**
	 * @Override
     * @param driver
     * @throws ProcessingException, MessageException
	 */
    protected void configureDriver(MessageProcessingDriver driver)
		throws ProcessingException, MessageException
    {
        try
        {

            CustomerContext context = CustomerContext.getInstance();
            String cid = context.getCustomerID();
            
            TradingPartnerRelationship tpr = TradingPartnerRelationship.getInstance(cid);

            String channelName = (String)context.get(MultiCustomerEventConsumerServer.EVENT_CHANNEL_NAME_PROP);
            String supplier = (String)context.get(SUPPLIER);
            String transaction = (String)context.get(TRANSACTION);
            String interfaceVersion = (String)context.get(CustomerContext.INTERFACE_VERSION_NODE);
            String url = tpr.getCustomerSoapUrl(supplier,transaction,interfaceVersion);

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "Got channel name: "+channelName + "\n"
                                               + "supplier : "+supplier 
                                               + "transaction :"+transaction 
                                               + "interface version :" +interfaceVersion
                                               + "URL :" +url);
            } 
            
            if(url!=null)
            {
                String channelNameWithoutCustomerTrail = EventChannelUtil.removeCIDTrailer( channelName, EventChannelUtil.getCustomerID(channelName) );
                /* replacing "." in channel name with "_" as customer context will be set as a document
                   in message context later in the chain. */

                context.set(StringUtils.replaceSubstrings(channelNameWithoutCustomerTrail,".", "_"), url );

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                     Debug.log(Debug.MSG_STATUS,
                             "Added Channel [" + channelName + "] and URL [" + url + "] Pair to Customer Context");

                //mpContext.set( cid, url );
            }
           
            /* property used by Manager products */
            context.set (CHANNEL_NAME_BASE,
                    EventChannelUtil.removeCIDTrailer(channelName,EventChannelUtil.getCustomerID(channelName)));

            /*  The customercontext contains the base event channel name and the event
             *  channel name with customer id.
             *  We want this event channel name to be used later on to perform certain actions.
             *  Since the message processors have built-in logic for accessing message processor
             *  contexts, we would like to use that. So we copy over all the contents from the
             *  CustomerContext to the MessageProcessorContext as a Document located at
             *  customer-context. */
            MessageProcessorContext mpContext = driver.getContext();
            mpContext.set(CUSTOMER_CONTEXT,  context.getAll());
            mpContext.set(AbstractJmsMsgStoreDAO.PROP_JMSMESSAGEID, 
                    CustomerContext.getInstance().get(AbstractJmsMsgStoreDAO.PROP_JMSMESSAGEID));
            mpContext.set(JMSConstants.QUEUE_NAME,CustomerContext.getInstance().get(JMSConstants.QUEUE_NAME) );
            mpContext.set(JmsMsgStoreDAOFactory.MSG_STORE_TABLE_PROP_NAME, 
                    jmsMsgStoreTableNm);
            
            if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
                Debug.log(Debug.SYSTEM_CONFIG, mpContext.describe());

        }
        catch(FrameworkException ex)
        {
            Debug.error("Got exception while setting in mpContext "+Debug.getStackTrace(ex));
            throw new ProcessingException(ex.getMessage());
        }
	}

    private List<Connection> dbConnLst = new ArrayList<Connection>();
	/**
     * Acquires database connections for each consumers and 
     * registers with this comServer.  
	 */
	private void registerConsumerHelpers() throws RuntimeException 
    {
		// register this CommServer with queue consumers
        Connection dbConn = null;
        
        try
        {
            for(JMSConsumerHelper helper : qNmConsumerMap.values())
            {
                if(useAlternateDbQueues)
                {
                	
                	try
                	{
	                    DBConnectionPool.setThreadSpecificPoolKey(altDbPoolKey);
	                    dbConn = DBConnectionPool.getInstance(altDbPoolKey).acquireConnection();
	                    
	                    /* reset the default key before registering the consumer helper */
						DBConnectionPool.setThreadSpecificPoolKey(DBConnectionPool.getDefaultResourceName());
                	}
                	catch(ResourceException e)
                	{
                		Debug.warning("Connection pools are not initialized , initializing it now..");
                      	DBConnectionPool.initializePoolConfigurations();
                      	dbConn = DBConnectionPool.getInstance(altDbPoolKey).acquireConnection();
						DBConnectionPool.setThreadSpecificPoolKey(DBConnectionPool.getDefaultResourceName());
                	}
                }
                else
                {
                    /* acquire a new connection and assign it to helper */
                	dbConn = DBConnectionPool.getInstance().acquireConnection();
                }

                dbConnLst.add(dbConn);
                helper.register(this,dbConn);
                
                if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                    Debug.log(Debug.OBJECT_LIFECYCLE, StringUtils.getClassName(this) 
                            + ":Registered JMS Consumer for queue" 
                            + " [" + helper.getQueueNm() +"]...\n"
                            + "[msgSelector ->"+helper.getMsgSelector()+"]");
            }
        }
        catch(ResourceException resExp)
        {
            Debug.error("Exception occured while acquiring a database connection, disconnecting all JMS consumers.");
            Debug.error("Shutting down JMS Consumer Communication Server.");
            disConnectAll();
            throw new RuntimeException(resExp.getMessage());
        }
		catch(JMSException jmsExp)
		{
			Debug.logStackTrace(jmsExp);
			Debug.error("Exception occured while registering JMSConsumerHelper..."+jmsExp.toString());
			Debug.error("Disconnecting all JMS consumers, Shutting down JMS Consumer Communication Server.");
            disConnectAll();
            throw new RuntimeException(jmsExp.getMessage());
        }
        
		Debug.log(Debug.STATE_LIFECYCLE,
                this.getClass().getName()+": Total [" + dbConnLst.size() + "] DB Connections acquired.");        
	}

	/**
	 * Callback method declared in JMSConsumerCallBack interface.
	 * This is called by message listeners after recieving a message from queue.
	 * @param header of jms message
	 * @param message body of jms message
	 */
	public void processMessage(String header,String message)
	{
        try 
		{
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, getClass().getSimpleName() 
                                       +": Processing message with header : \n" 
                                       +"and body: " + message);
            
            if(header!=null)
            {
                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, getClass().getSimpleName()
                            + ":Setting values in CustomerContext ");

                XMLMessageParser parser = new XMLMessageParser(header);
                if(parser.exists(CustomerContext.INTERFACE_VERSION_NODE))
                    CustomerContext.getInstance().set(CustomerContext.INTERFACE_VERSION_NODE,
                                parser.getValue(CustomerContext.INTERFACE_VERSION_NODE));

                if(parser.exists(TRANSACTION))
                    CustomerContext.getInstance().set(TRANSACTION,parser.getValue(TRANSACTION));

                if(parser.exists(SUPPLIER))
                    CustomerContext.getInstance().set(SUPPLIER,parser.getValue(SUPPLIER));

                if(parser.exists(MultiCustomerEventConsumerServer.EVENT_CHANNEL_NAME_PROP))
                    CustomerContext.getInstance().set(MultiCustomerEventConsumerServer.EVENT_CHANNEL_NAME_PROP,
                            parser.getValue(MultiCustomerEventConsumerServer.EVENT_CHANNEL_NAME_PROP));

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, StringUtils.getClassName(this) 
                            + CustomerContext.getInstance().getOtherItems());

            }
            
            process(header, message);
        }
        catch(MessageException me) {
            Debug.logStackTrace(me);
            throw new RuntimeException(me.getMessage());
        }
        catch(ProcessingException pe) {
            Debug.logStackTrace(pe);
            throw new RuntimeException(pe.getMessage());
        }
        catch(Exception e) {
            Debug.logStackTrace(e);
            throw new RuntimeException(e.toString());
        }
	}

	/**
     * disconnect all consumers from queues.
	 */
	public void disConnectAll()
	{
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"Disconnecting all consumers ..");
        
        for(JMSConsumerHelper helper : qNmConsumerMap.values())
		{
            try
            {
                helper.disConnect();
            }
            catch(Exception ex)
            {
               Debug.error(getClass().getSimpleName()+
                            "Could not disconnect QueueConsumer from Queue: "+
                                ex.toString());
            }
        }
	}
    
}

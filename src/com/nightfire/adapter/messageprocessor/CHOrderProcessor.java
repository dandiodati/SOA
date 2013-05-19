package com.nightfire.adapter.messageprocessor;

import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import biz.neustar.nsplatform.db.util.DBSessionManager;

import com.nightfire.order.CHOrderLoggerBase;
import com.nightfire.order.CHOrderLoggerFactory;
import com.nightfire.order.cfg.CHOrderCfg;
import com.nightfire.order.cfg.CHOrderCfgParser;
import com.nightfire.order.common.CHOrderContext;
import com.nightfire.order.utils.CHOrderConstants;
import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.OracleConnectionPool;

import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

/**
 * Order MessageProcessor which logs order information in respective Order/Event/Trans tables.
 * Generic to adapt any OrderBase Schema for different products.
 * @author Abhishek Jain
 */
public class CHOrderProcessor extends MessageProcessorBase 
{
    /**
     * Property indicating whether SQL operation should be part of overall driver transaction.
     */
    public static final String TRANSACTIONAL_LOGGING_PROP = "TRANSACTIONAL_LOGGING";
    /**
     * Property indicating whether sysdate would be logged in UTC time zone or not.
     */
    public static final String LOG_SYSDATE_IN_UTC_PROP = "LOG_SYSDATE_IN_UTC";
    /**
     * Property indicating the orderbase configuration file path.
     */
    public static final String ORDERBASE_CONFIG_FILE_PATH_PROP = "ORDERBASE_CONFIG_FILE_PATH";

    /**
     * Property indicating the location of event occured.
    */
    public static final String EVENT_CODE_INPUT_LOC_PROP = "EVENT_CODE_INPUT_LOC";

    /**
     * Property indicating whether order base logging would be applied or not.   
    */
     public static final String ORDERBASE_LOGGING_PROP = "ORDERBASE_LOGGING";

    /**
     * Property indicating the location whether occured event is valid
    */
     public static final String IS_VALID_EVENT_OUTLOC_PROP = "IS_VALID_EVENT_OUTLOC";

    /**
     * Property indicating 
     */
    private boolean transactionLogging = true;
    /**
     * Property indicating whether sysdate would be logged in UTC timezone or not.
     */
    private boolean logSysDateinUTC = true;
    
    /**
     * reference of order logging configuration object 
     */
    private CHOrderCfg cfg = null;
    
    private String eventCodeLoc;
    private String isValidEventLocation;
    
    /**
     * Property indicating whether logger attributes needed in output or not.
     */
    public static final String LOGGER_ATTRIBUTES_OUTPUT_REQUIRED = "LOGGER_ATTRIBUTES_OUTPUT_REQUIRED";

    private static final String CONTEXT_LOCATION = "@context.";

    private boolean isLoggerOutputReq = false;
    
    /**
     * initialize the OrderProcessor. 
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        super.initialize( key, type );
        
        this.initDBSessionManager();
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CHOrderProcessor: inside intialize... key["+key+"] & type["+type+"]");

        StringBuffer errorBuffer = new StringBuffer( );

        eventCodeLoc = getRequiredPropertyValue(EVENT_CODE_INPUT_LOC_PROP, errorBuffer);
        isValidEventLocation = getRequiredPropertyValue(IS_VALID_EVENT_OUTLOC_PROP, errorBuffer);
        
        if(errorBuffer.length()>0)
            throw new ProcessingException(" Required Property missing:"+errorBuffer);    

        String transactionLogging = getPropertyValue( TRANSACTIONAL_LOGGING_PROP );
        String logSysDateInUTC = getPropertyValue( LOG_SYSDATE_IN_UTC_PROP );
        String orderBaseConfigFilePath = getPropertyValue( ORDERBASE_CONFIG_FILE_PATH_PROP );
        String orderBaseLogging = getPropertyValue( ORDERBASE_LOGGING_PROP );
        String isLoggerOutputReq = getPropertyValue(LOGGER_ATTRIBUTES_OUTPUT_REQUIRED);

        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "CHOrderProcessor: initialize " +
                    "transactionLogging["+transactionLogging+"]," +
                    "logSysDateInUTC["+logSysDateInUTC+"]" +
                    "orderBaseConfigFilePath["+orderBaseConfigFilePath+"]" +
                    "orderBaseLogging["+orderBaseLogging
                    + "]Is Logger attributes output required ["
                    + isLoggerOutputReq + "]");
        }

        if(StringUtils.hasValue(transactionLogging))
        {
            try {
                this.transactionLogging = getBoolean(transactionLogging);
            } catch (MessageException e) {
            }
        }
        if(StringUtils.hasValue(logSysDateInUTC))
        {
            try {
                this.logSysDateinUTC = getBoolean(logSysDateInUTC);
            } catch (MessageException e) {
            }
        }
        
        if (StringUtils.hasValue(isLoggerOutputReq)) {
            try {
                this.isLoggerOutputReq = getBoolean(isLoggerOutputReq);
            } catch (MessageException e) {
            }
        }

        cfg = CHOrderCfg.initializeCHOrderCfg(orderBaseConfigFilePath);
        
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CHOrderProcessor: Done intialization ...");
    }

    private void initDBSessionManager() throws ProcessingException
    {
        if(DBSessionManager.isInitialized())
            return;

        String dbName = null;
        String dbUserName = null;
        String dbPassword = null;

        try {

            dbName     = CommonConfigUtils.getDBName();
            dbUserName = CommonConfigUtils.getDBUser();
            dbPassword = CommonConfigUtils.getDBPassword();

            // Check whether to use Oracle Connection Pool or NFDBConnection Pool
            if(StringUtils.getBoolean(CommonConfigUtils.getValue(DBConnectionPool.USE_ORACLE_CONNECTION_POOL_PROP), false))
            {
                OracleConnectionPool ocp = (OracleConnectionPool)DBConnectionPool.getInstance();

                if(Debug.isLevelEnabled(Debug.DB_STATUS))

                      Debug.log(Debug.DB_STATUS, "Initializing DBSessionManager with Oracle Connection Pool...");

                //Get ONS Config value if RAC needs to be supported.

                String onsCfg  = CommonConfigUtils.getValue(CommonConfigUtils.ONS_CONFIG_FOR_ORACLE_RAC);

           //     DBSessionManager.initializeUsingOracleConPool( dbName, dbUserName, dbPassword, onsCfg , ocp.getCacheProperties());

            }
            else
            {

                DBSessionManager.initialize( dbName, dbUserName, dbPassword, false );
            }

            if(Debug.isLevelEnabled(Debug.DB_STATUS))

                Debug.log(Debug.DB_STATUS, "DBSessionManager Initialization done.");


        }
        catch (Exception e) {
            Debug.error("Unable to initialize DB Session Manger["+dbName+"]");
            throw new ProcessingException("Unable to initialize DB Session Manger["+dbName
                                        +"] details:"+e.getMessage());
        }
    }

    /**
     * process the MessageProcessor.
     */
    public NVPair[] process ( MessageProcessorContext mpcontext, MessageObject input ) 
    throws MessageException, ProcessingException
    {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
               Debug.log(Debug.MSG_STATUS, "CHOrderProcessor: process ...");

            if ( input == null )
                 return null;

            String eventCode = getString (eventCodeLoc, mpcontext, input);

            String inputMsgInString = input.getString();

            // create a CHOrderContext object using MessageProcessorContext object.
            CHOrderContext ctx = new CHOrderContext(mpcontext,transactionLogging);

            // set product name and input message as string into ch order context.
            ctx.setProduct(cfg.getOdrLoggerCfg().getProduct());
            ctx.setInputMessage(inputMsgInString);

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "CHOrderProcessor: Support for logging SYS Date in UTC time zone is needed: [  LOG_SYSDATE_IN_UTC :"+logSysDateinUTC+"]");
            
            //  set log_sysdate_utc field in ctx.
            ctx.setAttribute(CHOrderConstants.LOG_SYSDATE_IN_UTC, String.valueOf(logSysDateinUTC));

            // set event code from message property.
            ctx.setEventCode(eventCode);

            try
            {

                // set the customerID in CHOrderContext.
                String customerId = CustomerContext.getInstance().getCustomerID();
                ctx.setCustomerId(customerId);

                // get a Order Logger using LoggerFactory.
                CHOrderLoggerBase orderLogger = CHOrderLoggerFactory.getCHOrderLoggerFactory().getCHOrderLogger(ctx);

                //Set orderLogger obj into CHOrderContext
                ctx.setOrderLogger(orderLogger);
                
                // log the details to Order Schema.
                orderLogger.log(ctx);

                // Check if Attributes from logger will be required in Gateway
                // processing context. If yes populate all logger attributes to
                // context.
                if (this.isLoggerOutputReq) {
                    Map<String, Object> map = ctx.getAttributes();

                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS,
                                "CHOrderProcessor: Logger attributes required in output at context.["
                                        + map + "]");

                    Set<String> outKeys = map.keySet();

                    for (String attributeKey : outKeys) {

                        Object obj = map.get(attributeKey);

                        if (obj != null) {
                            Debug.log(Debug.MSG_STATUS,
                                    "CHOrderProcessor: Setting value in context for ["
                                            + CONTEXT_LOCATION + attributeKey + ":"
                                            + obj + "]");
                            set(CONTEXT_LOCATION + attributeKey, mpcontext, input,
                                    obj);
                        } else {

                            Debug.log(Debug.MSG_STATUS,
                                    "CHOrderProcessor: Null value found attribute["
                                            + CONTEXT_LOCATION + attributeKey
                                            + "], Hence skipping");
                        }

                    }
                }


                // Place the result in the configured location in the input message.
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "CHOrderProcessor: Setting value in context for [ IS_VALID_EVENT_OUTLOC :"+isValidEventLocation+"]");
                
                set(isValidEventLocation, mpcontext, input, ctx.getAttribute(CHOrderConstants.IS_VALID_EVENT) );

            }
            catch (Exception e) {
                Debug.log(Debug.ALL_ERRORS,"Exception ocurred :"+Debug.getStackTrace(e));
                throw new ProcessingException(e);
            }

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "CHOrderProcessor: Done processing ...");

            return ( formatNVPair( input ) );
    }
}

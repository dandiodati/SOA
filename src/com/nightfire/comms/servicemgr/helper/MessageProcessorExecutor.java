package com.nightfire.comms.servicemgr.helper;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.PropertyException;
import com.nightfire.framework.jms.JMSConsumerCallBack;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.DriverUniqueIdentifierReader;
import com.nightfire.framework.util.SeqIdGenerator;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.comms.jms.handler.JMSConsumerRequestHandler;

public class MessageProcessorExecutor implements JMSConsumerCallBack 
{
    /**
     * sequenceIdGenerator to generate sequence ID. Instantiation is done once. 
     */
    private static SeqIdGenerator sequenceIdGenerator;
    
    /**
     * DriverUniqueIdentifierReader to read Unique Identifier per <driverKey-driverType>.
     */
    private DriverUniqueIdentifierReader driverUniqueIdentifierReader;

    private static final String RETRY_COUNT = "RETRY_COUNT";
    public static final String PROCESSED_BY_GWS = "PROCESSED_BY_GWS"; 
    
    static{
        try {
            sequenceIdGenerator = new SeqIdGenerator();
        } catch (PropertyException e) {
            Debug.error("Unable to instantiate sequenceIdGenerator "+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Driver Key to identify the Message Processor.
     */
    private String driverKey;
    /**
     * Driver Type to identify the Message Processor.
     */
    private String driverType;
    /**
     * Maximum number of times driver to be processed which has failed previously due to exception. 
     */
    private int retryCount;
    
    /**
     * An optional ID associated with the executing GWS instance
     */
    private String instanceId;

    /**
     * creates a MessageProcessorExecutor 
     * @param driverKey represent driver key.
     * @param driverType represent driver type.
     */
    public MessageProcessorExecutor(String driverKey, String driverType)
    {
        this.driverKey = driverKey; 
        this.driverType = driverType; 
        this.retryCount = Integer.MAX_VALUE;

        driverUniqueIdentifierReader = new DriverUniqueIdentifierReader(this.driverKey, this.driverType);
    }

    /**
     * creates a MessageProcessorExecutor 
     * @param driverKey represent driver key.
     * @param driverType represent driver type.
     * @param instanceId an id associated with the GWS instance
     */
    public MessageProcessorExecutor(String driverKey, String driverType,String instanceId)
    {
    	this(driverKey,driverType);
    	this.instanceId = instanceId;
    }

    /**
     * Callback method declared in JMSConsumerCallBack interface.
     * This is called by message listeners after receiving a message from queue.
     * @param header of jms message
     * @param message body of jms message
     */
    public void processMessage(String header, String message)
    {
        try
        {
            
            if(CustomerContext.getInstance().getMessageId()==null)
            {
                String reqId = sequenceIdGenerator.getStrNextId();
                if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                    Debug.log(Debug.MSG_DATA, "Setting MessageID ["+reqId+"].");
                CustomerContext.getInstance().setMessageId(reqId);
            }

            if (CustomerContext.getInstance().getUniqueIdentifier() == null)
            {
                String uniqueIdentifier = driverUniqueIdentifierReader.getUniqueIdentifier(message);
                if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                    Debug.log(Debug.MSG_DATA, "Setting Unique Identifier ["+uniqueIdentifier+"].");
                CustomerContext.getInstance().setUniqueIdentifier(uniqueIdentifier);
            }

            if( executionCheck(header) )
            {
            	JMSConsumerRequestHandler handler = new JMSConsumerRequestHandler(driverKey,driverType,instanceId);
                String response = handler.processSync(header,message);
                if ( Debug.isLevelEnabled ( Debug.MSG_DATA ) )
                    Debug.log(Debug.MSG_DATA, "Response Generated ["+response+"].");
            }
            else
            {
                Debug.log(Debug.ALL_WARNINGS, "Message Process Chain is 'NOT' executed.");
            }
        }
        catch (MessageException me) {
            Debug.logStackTrace(me);
            throw new RuntimeException(me);
        }
        catch (ProcessingException pe) {
            Debug.logStackTrace(pe);
            throw new RuntimeException(pe);
        }
        catch (Exception e) {
            Debug.logStackTrace(e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * check whether message processor chain is to be processed or not.
     * @param header as string
     * @return boolean
     */
    private boolean executionCheck(String header) 
    {
        try
        {
            XMLMessageParser p = new XMLMessageParser( header );
            
            String retryCountStr = ( header!=null && header.contains(RETRY_COUNT) ) ? p.getValue( RETRY_COUNT ) : null;
            
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            {
                Debug.log(Debug.MSG_STATUS, "trying to execute message processor with RETRY_COUNT ["+retryCountStr+"]");
                Debug.log(Debug.MSG_STATUS, "Configured RETRY_COUNT_VALUE["+retryCount+"]");
            }


            if(!StringUtils.hasValue(retryCountStr))
            {
                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log(Debug.MSG_STATUS, "No RETRY_COUNT header found... returning true");
                return true;
            }
            else
            {
                int retry = Integer.parseInt(retryCountStr);

                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log(Debug.MSG_STATUS, "RETRY_COUNT header found... returning "+ (retry < this.retryCount));

                // Retry if retry value is less than configured value.
                // Also, return true always if retry count is never.
                if(retry < this.retryCount || this.retryCount == -1)
                    return true;
                else
                    return false;
            }
        }
        catch (Exception e)
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            {
                Debug.log(Debug.MSG_STATUS, "Exception encounted while reteriving RETRY_COUNT details:"+e.getMessage());
                Debug.log(Debug.MSG_STATUS, "returning true");
            }
            return true;
        }
    }

    /**
     * Object used to return header and message as a unit.
     */
    public class ResponseObject 
    {
        public final String header;
        public final String message;

        public ResponseObject ( String header, String message ) 
        {
            this.header = header;
            this.message = message;
        }
    }

    /**
     * @return retry count configured for this message process executor.
     */
    public int getRetryCount()
    {
        return retryCount;
    }

    /**
     * sets the retry count value.
     * @param retryCount as int
     */
    public void setRetryCount(int retryCount)
    {
        this.retryCount = retryCount;
    }
}

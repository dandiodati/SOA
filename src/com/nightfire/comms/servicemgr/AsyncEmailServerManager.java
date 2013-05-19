package com.nightfire.comms.servicemgr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.comms.email.AsyncEmailServer;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;

/**
 * 
 * @author Abhishek Jain
 */
public class AsyncEmailServerManager extends ServerManagerBase
{
    /**
     * Configuration Constant to identify status of AsyncEmailServer.
     */
    public static final String ASYNC_EMAIL_SERVER_START="start";

    /**
     * Configuration Constant AsyncEmailServer.
     */
    public static final String ASYNC_EMAIL_SERVERS = "async-email-servers";

    /**
     * Configuration constant to identify a id of AsyncEmailServer.
     */
    private static final String ASYNC_EMAIL_SERVER_ID = "id";

    /**
     * Configuration constant to identify the key of AsyncEmailServer.
     */
    public static final String ASYNC_EMAIL_SERVER_KEY = "key";

    /**
     * Configuration constant to identify the driver type of AsyncEmailServer.
     */
    public static final String ASYNC_EMAIL_SERVER_TYPE = "type";

    /**
     *  map that caches all the email server which were configured. 
     */
    private HashMap<String,EmailServerConf> emailServerMap = new HashMap<String,EmailServerConf>();

    /**
     * singleton instance for AsyncEmailServerManager
     */
    private static AsyncEmailServerManager single;

    /**
     * private constructor for singleton implementation
     * @param type
     */
    private AsyncEmailServerManager(String type)
    {
        super(type);
    }

    /**
     * returns a single instance of AsyncEmailServerManager.
     * @param type 
     * @return AsyncEmailServerManager
     */
    public static synchronized AsyncEmailServerManager getInstance( String type)
    {
        if(single == null)
            single = new AsyncEmailServerManager(type);

        return single;
    }

    
    /**
     * initialize the Email Server instance from the configuration defined in the repository.
     * @throws ProcessingException 
     */
    public void initialize() throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "initializing email servers .. ");

        // read configuration from repository
        //String categoryConfig = ServerManagerFactory.getInstance().getConfigCategory(getType());
        
        //String categoryConfig = ServerManagerFactory.MGR_CONFIG_CATEGORY;
        //String metaConfig = ServerManagerFactory.getInstance().getConfigMeta(getType());
        
        List<String> configCategory = ServerManagerFactory.getInstance().getConfigCategory(getType());
        XMLMessageParser fileParser = null;
        
        Document aggregatedDoc;
        try {
            aggregatedDoc = XMLLibraryPortabilityLayer.getNewDocument(getType(), null);
        } catch (MessageException me) {
           Debug.logStackTrace(me);
           throw new ProcessingException(me);
        }
        
        ServiceIdFilter idFilter = new ServiceIdFilter();
        for(String fileNm : configCategory)
        {
            String xmlDescription;
            try 
            {
                xmlDescription = RepositoryManager.getInstance().getMetaData(ServerManagerFactory.MGR_CONFIG_CATEGORY, fileNm);
                fileParser = new XMLMessageParser( xmlDescription );
                fileParser = idFilter.getFilteredDOM(fileParser,ASYNC_EMAIL_SERVERS);
                Element document = fileParser.getDocument().getDocumentElement();
                Node node = aggregatedDoc.importNode(document, true);
                aggregatedDoc.getDocumentElement().appendChild(node);

            } 
            catch (Exception e) 
            {
                Debug.error("Unable to load and parse file from repository :" + ServerManagerFactory.MGR_CONFIG_CATEGORY + "/" + fileNm);
                throw new ProcessingException(e);
            }
    
            NodeList list = fileParser.getDocument().getElementsByTagName(ServiceMgrConsts.ASYNC_EMAIL_SERVER);
            for ( int Ix = 0;  Ix < list.getLength();  Ix ++ )
            {
                if ( list.item(Ix) != null )
                {
                    Element asyncEmailServerElement = (Element) list.item(Ix);
                    
                    String id,key,value,start;
                    
                    id = getConfigurationValue(asyncEmailServerElement,ConfigType.ATTRIBUTE,ASYNC_EMAIL_SERVER_ID,null);
                    key = getConfigurationValue(asyncEmailServerElement,ConfigType.ELEMENT,ASYNC_EMAIL_SERVER_KEY,null); 
                    value = getConfigurationValue(asyncEmailServerElement,ConfigType.ELEMENT,ASYNC_EMAIL_SERVER_TYPE,null);
                    start = getConfigurationValue(asyncEmailServerElement,ConfigType.ATTRIBUTE,ASYNC_EMAIL_SERVER_START,"true");
    
                    if
                    (
                            !StringUtils.hasValue(id) || !StringUtils.hasValue(key) || 
                            !StringUtils.hasValue(value)  
                    )
                    {
                        Debug.log(Debug.XML_ERROR, "Could not configure AsyncEmailServer since mandatory property are not configured");
                        Debug.log(Debug.DB_ERROR, ASYNC_EMAIL_SERVER_ID+"="+id);
                        Debug.log(Debug.DB_ERROR, ASYNC_EMAIL_SERVER_KEY+"="+key);
                        Debug.log(Debug.DB_ERROR, ASYNC_EMAIL_SERVER_TYPE+"="+value);
    
                        // skip to initialize email server if not configured correctly.
                        continue;
                    }
    
                    boolean startFlag = "false".equalsIgnoreCase(start)?false:true;
    
                    EmailServerConf emailServerConf = new EmailServerConf(key,value);
                    emailServerConf.setStarted(startFlag);
                    
                    if(id == null)
                        Debug.log(Debug.MSG_ERROR,"Could not initialize emailserver with id ["+id+"].");
                    else
                        emailServerMap.put(id, emailServerConf);
    
                }
            }
        }

        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "done initializing email servers .. ");
        
        try {
                parser = new XMLMessageParser( aggregatedDoc );
            } catch(Exception e) {
                throw new ProcessingException("Unable to create XMLMessageParser object "+e.getMessage());
            }
    }

    /** 
     * NOT SUPPORTED
     */
    public void add(Map parameters) throws ProcessingException
    {
        // TODO implementation to be provided in future releases.
    }

    /** 
     * NOT SUPPORTED
     */
    public void remove(Map parameters)
    {
        // TODO implementation to be provided in future releases.
    }

    /** 
     * start the Async Email Server identified by parameters.
     * @param parameters identifies the Consumer to be started
     * @throws ProcessingException when unable to register the consumer.
     */
    public void start(Map parameters) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Starting Email Server... "+parameters);

        String id = (String)parameters.get(ASYNC_EMAIL_SERVER_ID);

        if(id == null)
        {
            Debug.log(Debug.MSG_WARNING,"Could not start a Async Email Server with id ["+id+"].");
            return;
        }

        EmailServerConf emailServerConf = emailServerMap.get(id);

        if(emailServerConf == null)
        {
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS,"Could not start a Async Email Server with id["+id+"] since Email Server " +
                "was not already configured.");
        }
        else
        {
            AsyncEmailServer emailServer = emailServerConf.getEmailServer();
            if(emailServer == null)
            {
                emailServer = new AsyncEmailServer(emailServerConf.getDriverKey(),emailServerConf.getDriverValue());
                emailServerConf.setEmailServer(emailServer);
                
                // configure and start the email server.
                new Thread(emailServer).start();
            }
            else
            {
                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                    Debug.log(Debug.NORMAL_STATUS, "EmailServer is already started.");
            }
        }

        // update configuration with the latest status.
        updateXML(ServiceMgrConsts.ASYNC_EMAIL_SERVER,ASYNC_EMAIL_SERVER_ID,id,ASYNC_EMAIL_SERVER_START, "true");
    }

    /** 
     * starts all the configured EmailServers
     */
    public void startAll() throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "starting All Email Server.. ");

        Iterator iter = emailServerMap.keySet().iterator();
        while(iter.hasNext())
        {
            String id =(String)iter.next();
            EmailServerConf conf = emailServerMap.get(id);
            if(conf.isStarted())
            {
                Notification notification = null;
                Map params = Collections.singletonMap(ASYNC_EMAIL_SERVER_ID, id);
                try
                {
                    start(params);
                    notification = new Notification("Started ASync Email Server:"+id);
                }
                catch(Exception e)
                {
                    Debug.log(Debug.MSG_ERROR,"could not start Async Email Server id ["+id+"]");
                    Debug.log(Debug.MSG_ERROR,e.getMessage());
                    notification = new Notification("Error starting ASync Email Server :"+id,e);
                }
                notificationHandler.handle(notification);
            }

        }
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "done starting All Email Server.. ");
    }

    /**
     * stops the EmailServer identified by parameters.
     */
    public void stop(Map params)
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Stoping Async Email Server Managers services.. "+params);

        String id = (String) params.get(ASYNC_EMAIL_SERVER_ID);
        EmailServerConf emailServerConf = emailServerMap.get(id); 

        if (emailServerConf == null)
        {
            Debug.log(Debug.MSG_WARNING, "No configured async email server found with ID["+id+"]");
            return;
        }

        AsyncEmailServer emailServer = emailServerConf.getEmailServer();

        if(emailServer == null)
            return;
        else
        {
            emailServer.shutdown();
            emailServerConf.setEmailServer(null);
            emailServerConf.setStarted(false);
        }


        // update configuration with the latest status.
        updateXML(ServiceMgrConsts.ASYNC_EMAIL_SERVER,ASYNC_EMAIL_SERVER_ID,id,ASYNC_EMAIL_SERVER_START, "false");

        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "done stoping Async Email Server Manager... ");
    }

    /**
     * stops all the running AsyncEmailServers.
     */
    public void stopAll() throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Stop all async email server servicess .. ");

        Iterator iter = emailServerMap.keySet().iterator();

        while(iter.hasNext())
        {
            String id =(String)iter.next();
            Map map = Collections.singletonMap(ASYNC_EMAIL_SERVER_ID, id);
            Notification notification = null;
            try
            {
                stop(map);
                notification = new Notification("Stopped Async Email Server:"+id);
            }
            catch(Exception e)
            {
                Debug.log(Debug.MSG_WARNING, "Exception while stopping Async Email Server "+e.getMessage());
                Debug.log(Debug.MSG_WARNING,e.getMessage());
                notification = new Notification("Exception occured while stopping Async Email Server: "+id,e);
            }
            notificationHandler.handle(notification);
        }
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "done stoping all services .. ");    
    }

    /**
     * Place holder class to store the Email Server configuration.
     */
    private class EmailServerConf
    {
        // configuration attributes.
        private String driverKey;
        private String driverValue;

        private boolean started;

        private AsyncEmailServer emailServer;

        public AsyncEmailServer getEmailServer()
        {
            return emailServer;
        }

        public void setEmailServer(AsyncEmailServer emailServer)
        {
            this.emailServer = emailServer;
        }

        public EmailServerConf(String driverKey, String driverValue)
        {
            super();
            this.driverKey = driverKey;
            this.driverValue = driverValue;
        }

        // getters and setters for all the configuration parameters.
        public String getDriverKey()
        {
            return driverKey;
        }
        public void setDriverKey(String driverKey)
        {
            this.driverKey = driverKey;
        }
        public String getDriverValue()
        {
            return driverValue;
        }
        public void setDriverValue(String driverValue)
        {
            this.driverValue = driverValue;
        }

        public boolean isStarted()
        {
            return started;
        }

        public void setStarted(boolean started)
        {
            this.started = started;
        }
    }
}

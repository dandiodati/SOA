package com.nightfire.comms.servicemgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;

/**
 * Class containing mapping of server types to their manager class names.
 * It also provides method to get configuration files for a particular
 * server type placed in repository. 
 * @author hpirosha
 */
public class ServerTypeMappingRegistry {

    /* Map containing server type to server class name */
    private static Map<String,String> serverType2ClassNm = new HashMap<String,String>();
    
    /* Map containing server type to xpath for identifying its configuration files */ 
    private static Map<String,String> serverType2Xpath = new HashMap<String,String>();
    
    /**
     * XPaths that are used to determine configuration files 
     * for a particular server type.
     */
    private static String JMS_CONSUMER_XPATH =
         "contains(name(/jms-consumer-config/jms-consumers/*),'"
                        +ServiceMgrConsts.JMS_CONSUMER+"')";
    
    private static String ASYNC_EMAIL_SERVER_XPATH = 
        "contains(name(/emailserver-config/async-email-servers/*),'"
                        +ServiceMgrConsts.ASYNC_EMAIL_SERVER+"')";

    private static String POLL_COMM_SERVER_XPATH = 
        "/poll-comm-server-config/poll-comm-server[" +
            "@type='" + ServiceMgrConsts.ASYNC_TIMER_SERVER + "' or " +
            "@type='" + ServiceMgrConsts.FTP_POLLER + "' or " +
            "@type='" + ServiceMgrConsts.FILE_SERVER + "' or " +
            "@type='" + ServiceMgrConsts.SRM_EVENT_SERVER + "' or " +
            "@type='" + ServiceMgrConsts.IA_SERVER + "' or " +
            "@type='" + ServiceMgrConsts.SRM_CONFIGURED_QUEUES_JMS_SERVER+ "' or " +
            "@type='" + ServiceMgrConsts.SRM_JMS_SERVER + "']";
    
    private static String XML_FILE_SUFFIX = ".xml";

    static   {
        
        serverType2ClassNm.put(ServiceMgrConsts.JMS_CONSUMER, 
                JMSQueueConsumerManager.class.getName());
        
        serverType2ClassNm.put(ServiceMgrConsts.ASYNC_EMAIL_SERVER,
                AsyncEmailServerManager.class.getName());

        serverType2ClassNm.put(ServiceMgrConsts.POLL_COMM_SERVER,
                GenericPollCommServerManager.class.getName());

        serverType2ClassNm.put(ServiceMgrConsts.ASYNC_TIMER_SERVER,
                GenericPollCommServerManager.class.getName());

        serverType2ClassNm.put(ServiceMgrConsts.SRM_EVENT_SERVER,
                GenericPollCommServerManager.class.getName());
        
        serverType2ClassNm.put(ServiceMgrConsts.SRM_JMS_SERVER,
                GenericPollCommServerManager.class.getName());

        serverType2ClassNm.put(ServiceMgrConsts.IA_SERVER, 
                GenericPollCommServerManager.class.getName());

        serverType2ClassNm.put(ServiceMgrConsts.SRM_CONFIGURED_QUEUES_JMS_SERVER, 
                GenericPollCommServerManager.class.getName());
        
        serverType2Xpath.put(ServiceMgrConsts.JMS_CONSUMER, 
                JMS_CONSUMER_XPATH);
        
        serverType2Xpath.put(ServiceMgrConsts.ASYNC_EMAIL_SERVER, 
                ASYNC_EMAIL_SERVER_XPATH);
        
        serverType2Xpath.put(ServiceMgrConsts.POLL_COMM_SERVER, 
                POLL_COMM_SERVER_XPATH);
        
    }
    
    /**
     * Get manager class name for a specified server type. 
     * @param type String server type
     * @return String class name 
     */
    public static String getServerClassNm(String type)  {
        return serverType2ClassNm.get(type);
    }
    

    /**
     * Get all server types supported in current infrastructure. 
     * @return
     */
    public static Set<String> getAllServerTypes() {
        return serverType2Xpath.keySet();
    }
    
    /**
     * Few types are specified in configuration but actually they
     * are derived like : FTP,TIMER,SRM_EVENT and SRM_JMS servers
     * they are all poll-comm-servers.  
     * @param pseudoType 
     * @return
     */
    public static String mapPseudo2ActualType(String pseudoType) {
        if(ServiceMgrConsts.FTP_POLLER.equals(pseudoType)  
                || ServiceMgrConsts.ASYNC_TIMER_SERVER.equals(pseudoType) 
                || ServiceMgrConsts.FILE_SERVER.equals(pseudoType)
                || ServiceMgrConsts.SRM_EVENT_SERVER.equals(pseudoType) 
                || ServiceMgrConsts.SRM_JMS_SERVER.equals(pseudoType)
                || ServiceMgrConsts.SRM_CONFIGURED_QUEUES_JMS_SERVER.equals(pseudoType)
                || ServiceMgrConsts.IA_SERVER.equals(pseudoType))    //change made on this line 
        return  ServiceMgrConsts.POLL_COMM_SERVER;

        return pseudoType;
    }
    
    /**
     * Get all configuration for a specified server type placed in repository.
     * This would search the servicemgr category inside repository and apply 
     * XPath to figure out configuration files belonging to a specific server type.  
     * 
     * @param type
     * @return List containing file names.
     * @throws ProcessingException
     */
    public static List<String> getConfigCategory(String type) throws ProcessingException {
        
        NVPair[] pairs = null;
        try 
        {
            pairs = RepositoryManager.getInstance().listMetaData(ServerManagerFactory.MGR_CONFIG_CATEGORY,
                    false,XML_FILE_SUFFIX);
            
            if(pairs==null || pairs.length == 0)
            {
                Debug.log(Debug.NORMAL_STATUS,"No XML file found in repository folder ["
                        +ServerManagerFactory.MGR_CONFIG_CATEGORY+"]");

                return null;
            }
        } 
        catch (Exception e) 
        {
            Debug.error("An exception occured while listing xml files from repository folder ["
                    +ServerManagerFactory.MGR_CONFIG_CATEGORY+"]");
            
            Debug.logStackTrace(e);

            throw new ProcessingException("An exception occured while listing xml files from repository folder  ["
                    +ServerManagerFactory.MGR_CONFIG_CATEGORY+"]");

        } 
 
        List<String> cfgFileLst = new ArrayList<String>();
        String fileNm = null;
        try
        {
            for(int j=0; j <pairs.length; j++)
            {
                fileNm = pairs[j].name;
                Document document = getDocument(fileNm);
                ParsedXPath parseXpath = new ParsedXPath(serverType2Xpath.get(type));
             
                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                    Debug.log(Debug.NORMAL_STATUS," --> Evaluating file :"+fileNm+" for type :"+type);    
                
                if(parseXpath.getBooleanValue(document.getDocumentElement()))
                    cfgFileLst.add(fileNm);
                
            }

            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                Debug.log(Debug.NORMAL_STATUS," --> returning list of files for type["
                        +type+"] : "+cfgFileLst);    
 
            return cfgFileLst;
        }
        catch(Exception e)
        {
            Debug.log(Debug.NORMAL_STATUS,"Failed to parse and evaluate XPath on file :"+fileNm
                    +"\n"+ Debug.getStackTrace(e));
            
            throw new ProcessingException("Failed to parse and evaluate XPath on file :"+fileNm
                    +"\n"+ Debug.getStackTrace(e));
        }
        
    }
    
    /**
     * Get document object 
     * @param fileNm
     * @return
     * @throws ProcessingException
     */
    private static Document getDocument(String fileNm) throws ProcessingException
    {
        try
        {
            Document dom = RepositoryManager.getInstance().getMetaDataAsDOM(ServerManagerFactory.MGR_CONFIG_CATEGORY, fileNm+".xml");
            return dom;
        }
        catch(Exception  e)
        {
            throw new ProcessingException("Exception occured while reading configuration file " +
                    fileNm+".xml from repository/DEFAULT/"+ServerManagerFactory.MGR_CONFIG_CATEGORY
                    +"\n"+ e.getStackTrace());
        }
    }

    /**
     * 
     * @param serverType
     * @param classNm
     * @param fileFilterXpath
     * @throws FrameworkException
     */
    public static void addServerMapping(String serverType, String classNm, String fileFilterXpath)  
            throws FrameworkException   { 
        
        try
        {
            Class.forName(classNm);
        }
        catch(Exception exp)
        {
            Debug.warning("Unable to load class :"+classNm);
            Debug.logStackTrace(exp);
            
            throw new FrameworkException("Unable to load class :"+classNm);
        }
        
        serverType2ClassNm.put(serverType, classNm);
        
        serverType2Xpath.put(serverType, fileFilterXpath);
        
    }
}

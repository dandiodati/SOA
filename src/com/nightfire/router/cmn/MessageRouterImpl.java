package com.nightfire.router.cmn;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.router.cfg.RouterConfig;
import com.nightfire.router.cfg.RouterConfig.MessageDirectorConfig;
import com.nightfire.router.cfg.RouterConfigLoader;

/**
 * Implementation of MessageRouter interface. 
 * @author hpirosha
 */
public class MessageRouterImpl implements MessageRouter , CachingObject {

    private String cfgFileName = null;
    private RouterConfig config = null;
    
    /**
     * Create a new instance of Message Router and
     * register it with Cache Manager. This would 
     * allow to change routing configuration at runtime.
     */
    public MessageRouterImpl()
    {
        CacheManager.getRegistrar().register(this);
    }
    
    /**
     * Constructor that allows to create a new instance 
     * without registering with Cache Manager.
     * 
     * @param registerWithCacheMgr boolean 
     */
    public MessageRouterImpl(boolean registerWithCacheMgr)
    {
        if(registerWithCacheMgr)
            CacheManager.getRegistrar().register(this);
    }

    
    /**
     * Initialize message routing component with the name of configuration file.
     * If nothing is passed it initializes itself with REQUEST_ROUTER chain present 
     * in persistent property. 
     * 
     * 
     * @param fileName String name of file that has message routing configuration.
     *                 The file should be present in <install-root>/DEFAULT/repository/router folder.
     * @throws FrameworkException if it fails to initialize.                 
     */
    public void initialize(String fileName) throws FrameworkException
    {
        
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE, "Initializing MessageRouter..");
        
        cfgFileName = fileName;
            
        RouterConfigLoader loader = RouterConfigLoader.getInstance(); 
        config = loader.load(fileName);
        
        directorMap.clear();
        initializeMessageDirectorCache();
    }
    
    /**
     * Method to route a message using this class. 
     * 
     * @param header String message header passed in as string
     * @param message Object message object
     * @param reqType ReqType type of request: sync, async etc
     * @throws ProcessingException, MessageException 
     */
    public Object processRequest(String header, Object message, ReqType reqType)
            throws ProcessingException, MessageException
    {
        try
        {
            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA,"Got message to route with header :\n"+header);
            
            XMLMessageParser headerParser = new XMLMessageParser(header);
            Document doc = headerParser.getDocument();
            MessageDirectorV2 director = findMessageDirector(doc,message);
    
            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA,"Message director which can route this message :"+director.getClass().getName());
            
            Object response = director.processRequest(doc,message, reqType.intValue());
            return response;
        }
        catch(MessageException e)
        {
            Debug.error("An exception occured while routing a message :"+e.getMessage());
            Debug.error(Debug.getStackTrace(e));
            
            throw e;
        }
        catch(Exception e)
        {
            Debug.error("An exception occured while routing a message :"+e.getMessage());
            Debug.error(Debug.getStackTrace(e));
            
            throw new ProcessingException(e);
        }
    }

    /**
     * Helper method to find appropriate message director for routing.
     * 
     * @param header
     * @param message
     * @return A message director object that shall route the message
     * @throws FrameworkException is thrown if no suitable message director is found.
     */
    private MessageDirectorV2 findMessageDirector(Document header, Object message) 
                                throws FrameworkException 
    {
        
        Iterator iter = directorMap.keySet().iterator();
        while(iter.hasNext())
        {
            String mdType = (String)iter.next();
            MessageDirectorV2 director = directorMap.get(mdType);

            if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA,"Evaluating director type: "+mdType);
            
            if(director.canRoute(header, message))
                return director;
        }
        
        /* if we reach here, we need to throw an exception */
        throw new FrameworkException("No message director can be found to route this message : ");
    }

    private Map<String,MessageDirectorV2> directorMap = 
        new LinkedHashMap<String,MessageDirectorV2>();
    
    /**
     * Method to initialize its cache of message directors.
     * 
     * @throws FrameworkException If not able to instantiate an object of message director.
     */
    protected void initializeMessageDirectorCache() throws FrameworkException
    {
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE, "initializing message director cache.");
        
        String directorClassNm = null;
        Iterator iter = config.getMessageDirectorCfg();
        while(iter.hasNext())
        {
            MessageDirectorConfig md = (MessageDirectorConfig)iter.next();
            directorClassNm = md.getDirectorClassNm();
            MessageDirectorV2 director = null;
            try 
            {
                director = (MessageDirectorV2)Class.forName(directorClassNm).newInstance();
                
                /* set type on new director */ 
                director.initialize(md);
                directorMap.put(md.getDirectorType(), director);

                if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                    Debug.log(Debug.STATE_LIFECYCLE, "created director type["
                            +md.getDirectorType()+"] of class ["+directorClassNm+"]");

            } 
            catch (InstantiationException e) 
            {
                Debug.error("Could not instantiate message director class : "+directorClassNm);
                Debug.error(Debug.getStackTrace(e));
                throw new FrameworkException(e);
            } 
            catch (IllegalAccessException e) 
            {
                Debug.error("Could not access message director class : "+directorClassNm);
                Debug.error(Debug.getStackTrace(e));
                throw new FrameworkException(e);
            }
            catch (ClassNotFoundException e) 
            {
                Debug.error("Could not find message director class : "+directorClassNm);
                Debug.error(Debug.getStackTrace(e));
                throw new FrameworkException(e);
            }
        }
    }

    /**
     * Flush routing configuration and initialize again.
     * 
     * @throws FrameworkException
     */
    public void flushCache() throws FrameworkException {
        
        RouterConfigLoader.getInstance().flush();
        directorMap.clear();
        initialize(this.cfgFileName);
    }
}

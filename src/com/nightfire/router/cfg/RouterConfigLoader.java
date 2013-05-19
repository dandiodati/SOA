package com.nightfire.router.cfg;

import org.w3c.dom.Document;

import com.nightfire.framework.repository.RepositoryException;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

public class RouterConfigLoader {

    private RouterConfig config = null;
    
    private static final String ROUTER_CFG_CATEGORY = "router";

    static RouterConfigLoader singleton = new RouterConfigLoader();
    
    private RouterConfigLoader()
    {
        
    }
    
    /**
     * Load router configuration. If fileName passed is null/empty then configuration
     * is loaded from database. 
     * 
     * @param fileName name of XML file kept in <install-root>/repository/DEFAULT/router folder.
     * @return RouterConfig
     * @throws FrameworkException
     */
    public synchronized RouterConfig load(String fileName) throws FrameworkException
    {

        if(StringUtils.hasValue(fileName))
        {
            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE, "passed in config file name :"+fileName);

            return loadFromFile(fileName);
        }
        else
        {
            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE, "initializing from persistent property ");
            
            return loadFromDB();
        }
    }
    
    /**
     * Method to load routing configuration from file present in 
     * <install-root>/repository/DEFAULT/router folder. 
     * 
     * @param fileName name of the file that stores router configuration
     * @return RouterConfig data object holding routing configuration 
     * @throws FrameworkException
     */
    private RouterConfig loadFromFile(String fileName) throws FrameworkException
    {
        if(config!=null)
            return config;
        
        try 
        {
            Document dom = 
                RepositoryManager.getInstance().getMetaDataAsDOM(ROUTER_CFG_CATEGORY, fileName);
            
            RouterConfigXMLParser parser = new RouterConfigXMLParser();
            
            config = parser.parse(dom);
            
            return config;
        }
        catch(RepositoryException re)
        {
            Debug.error("Exception occured while reading router configuration from repository !!\n"+Debug.getStackTrace(re));
            throw new FrameworkException(re);
        }
    }

    /**
     * Method to load configuration from persistent properties. 
     * @return
     * @throws FrameworkException
     */
    private RouterConfig loadFromDB() throws FrameworkException
    {
       if(config!=null)
            return config;
        
        RouterConfigDBParser parser = new RouterConfigDBParser();
        config = parser.parse();
        
        return config;
    }
    
    /**
     * Method to flush configuration object stored by this class.
     */
    public void flush() {
        config = null;
    }

    /**
     * Method to return singleton object of this class.
     * @return
     */
    public static RouterConfigLoader getInstance() {
        return singleton;
    }
}

package com.nightfire.order.cfg;


import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

/**
 * Singleton DataObject that represents the CHOrder Config.
 * @author hpirosha
 */
public class CHOrderCfg {

    private OrderLoggerCfg orderLoggerCfg;
    private static Map<String,CHOrderCfg> cfgMap = new HashMap<String,CHOrderCfg>();
    
    // Map<String orderCfgFilepath, String productNm>
    private static Map<String,String> initializedProductCfgMap = new HashMap<String,String>();
    
    /**
     * Constructor to instantiate the singleton object. 
     */
    private CHOrderCfg()
    {

    }

    /**
     * Initialize order logging configuration from the passed in file (path).
     * This method only parses once and caches the config i.e. CHOrderCfg. 
     * 
     * @param orderBaseConfigFilePath
     * @return CHOrderCfg
     * @throws ProcessingException
     */
    public static CHOrderCfg initializeCHOrderCfg(String orderBaseConfigFilePath) throws ProcessingException {
        if(initializedProductCfgMap.containsKey(orderBaseConfigFilePath)) 
            return cfgMap.get(initializedProductCfgMap.get(orderBaseConfigFilePath));
        
        
        synchronized (CHOrderCfg.class) {
        
            if(initializedProductCfgMap.containsKey(orderBaseConfigFilePath)) 
                return cfgMap.get(initializedProductCfgMap.get(orderBaseConfigFilePath));
            

            try 
            {
                String fileContent = FileUtils.readFile(orderBaseConfigFilePath);
                Document dom = XMLLibraryPortabilityLayer.convertStringToDom(fileContent);
                CHOrderCfg cfg = CHOrderCfgParser.getInstance().parse(dom);
                String product = cfg.getOdrLoggerCfg().getProduct();

                if(!StringUtils.hasValue(product))
                {
                    Debug.log(Debug.ALL_ERRORS,"Order logging cfg file ["+ orderBaseConfigFilePath + "] " +
                            " does not have a product attribute.");
                    
                    throw new ProcessingException("Order logging cfg file ["+ orderBaseConfigFilePath + "] " +
                            " does not have a product attribute.");
                }
                    
                initializedProductCfgMap.put(orderBaseConfigFilePath,product);
            }
            catch (FrameworkException e) 
            {
                Debug.log(Debug.ALL_ERRORS,"Exception ocurred : "+e.getMessage()
                        +"\t"+Debug.getStackTrace(e));
                
                throw new ProcessingException(e);
            }
        }
        
        return cfgMap.get(initializedProductCfgMap.get(orderBaseConfigFilePath));
    }
    

    /**
     * Get wrapper over order logging configuration for a specified product
     * @param productNm String product name as declared 
     *                  in order logging configuration
     * @return CHOrderCfg
     */
    public static CHOrderCfg getInstance(String productNm)
    {
        if(cfgMap.containsKey(productNm))
            return cfgMap.get(productNm);

        synchronized (CHOrderCfg.class)
        {
            if(cfgMap.containsKey(productNm))
                return cfgMap.get(productNm);
            
            CHOrderCfg  productCfg = new CHOrderCfg();
            cfgMap.put(productNm, productCfg);
        }
        
        return cfgMap.get(productNm);
    }


    /**
     * Add an OrderLoggerCfg to this config
     * 
     * @param ordrCfg OrderLoggerCfg for the product
     */
    public void setOdrLoggerCfg(OrderLoggerCfg ordrCfg)
    {
        if(orderLoggerCfg!=null)
            return;

        orderLoggerCfg = ordrCfg;
    }


    /**
     * Get underlying configuration  
     * 
     * @return OrderLoggerCfg
     */
    public OrderLoggerCfg getOdrLoggerCfg()
    {
        if(orderLoggerCfg==null)
            throw new IllegalStateException("Order logging configuration " +
                    "is not set on this object !!");
        
        return orderLoggerCfg;
    }

}

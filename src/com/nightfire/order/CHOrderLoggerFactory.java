package com.nightfire.order;

import java.lang.reflect.Constructor;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.order.cfg.CHOrderCfg;
import com.nightfire.order.cfg.CommandCfg;
import com.nightfire.order.cfg.OrderLoggerCfg;
import com.nightfire.order.common.CHOrderContext;
import com.nightfire.order.common.CommandChain;
import com.nightfire.order.utils.CHOrderConstants;
import com.nightfire.order.utils.CHOrderException;

/**
 * Implements singleton factory class for different CHOrderLogger types. 
 * @author Abhishek Jain
 */
public class CHOrderLoggerFactory 
{
    /**
     * singleton instance of CHOrderLoggerFactory; 
     */
    private static CHOrderLoggerFactory singleton = new CHOrderLoggerFactory();

    /**
     * private constructor to support singleton factory. 
     */
    private CHOrderLoggerFactory()
    {
    }

    public static CHOrderLoggerFactory getCHOrderLoggerFactory()
    {
        Debug.log(Debug.MSG_STATUS, "CHOrderLoggerFactory: Returning factory instance...");
        return singleton;
    }
    /**
     * returns the respective CHOrderLoggerBase type depending of context parameters.
     * @param context 
     * @return order logger of type CHOrderLoggerBase.
     */
    public CHOrderLoggerBase getCHOrderLogger(CHOrderContext context)
    throws CHOrderException
    {
        if(context == null)
            throw new CHOrderException("Encountered a CHOrderContext["+context+"]. Could not access parameters to configure Logger.");

        // access the product info
        String product = context.getProduct();
        if(!StringUtils.hasValue(product))
        {
            throw new CHOrderException("Product ["+product+"] property not found in the CHOrderContext");
        }

        // get the configuration information for this product.
        CHOrderCfg chOrderConfig = CHOrderCfg.getInstance(product);
        OrderLoggerCfg orderLoggerConfig = chOrderConfig.getOdrLoggerCfg();

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "CHOrderLoggerBase: getting CHOrderLoggerBase type for product["
                +product+"].");
        
        /* TODO : Cleanup ICP specific stuff from here */
        if(CHOrderConstants.ICP.equalsIgnoreCase(product))
        {
            // using the context and configuration information create the
            // specific order logger and return.
            return new ICPOrderLogger(orderLoggerConfig,context);
        }
        else if(orderLoggerConfig.getClassNm()!=null)
        {
            try 
            {
                Class clazz = Class.forName(orderLoggerConfig.getClassNm());
                Constructor constructor = 
                    clazz.getConstructor(new Class[] {OrderLoggerCfg.class, CHOrderContext.class});
                
                Object obj = constructor.newInstance(new Object[] {orderLoggerConfig,context});
                return (CHOrderLoggerBase)obj;
                
            } 
            catch (Exception e) 
            {
                Debug.error("Could not instantiate class ["+orderLoggerConfig.getClassNm()+"] : \n"
                        +Debug.getStackTrace(e));
                
                throw new CHOrderException("Could not instantiate class for ["+product+"] " 
                        +orderLoggerConfig.getClassNm() + " reason :"+e.getMessage());
                
            } 
        }

        throw new CHOrderException("No OrderLogger configured for Product ["+product+"]");
    }

}

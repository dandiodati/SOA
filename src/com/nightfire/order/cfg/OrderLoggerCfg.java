package com.nightfire.order.cfg;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.nightfire.order.common.CHOrderContext;

/**
 * Class containing logging configuration of a specific product.   
 * @author hpirosha
 */
public class OrderLoggerCfg {

    private String product;
    private List<CaseCfg> caseCfgLst;
    private String classNm;

    /**
     * Constructor that takes the product and order logger class name  
     * @param product String
     * @param classNm String
     */
    public OrderLoggerCfg(String product,String classNm)
    {
        this.product = product;
        caseCfgLst = new LinkedList<CaseCfg>();
		this.classNm = classNm;
    }

    /**
     * Get the associated product name 
     * @return String 
     */
    public String getProduct()
    {
        return product;
    }

    /**
     * Add a case configuration 
     * @param caseCfg
     */
    public void addCase(CaseCfg caseCfg)
    {
        caseCfgLst.add(caseCfg);
    }

    public String getClassNm()
    {
        return this.classNm;
    }

    /**
     * Get the list of command configurations that would 
     * be executed for the given CHOrderContext. 
     * @param context
     * @return list of command configurations.
     */
    public List<CommandCfg> getCommandList(CHOrderContext context)
    {
        Iterator<CaseCfg> iter = caseCfgLst.iterator();
        while(iter.hasNext())
        {
            CaseCfg cfg = iter.next();
            if(cfg.getCondition().evaluate(context))
                return cfg.getCommandCfgLst();
        }

        return null;
    }

}

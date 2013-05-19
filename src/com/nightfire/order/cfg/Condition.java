package com.nightfire.order.cfg;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.nightfire.order.common.CHOrderContext;

public class Condition {

    Map<String,String> paramMap;

    public Condition(Map<String,String> paramMap)
    {
        this.paramMap = paramMap; 
    }

    public Condition()
    {
        this.paramMap = new HashMap<String,String>(); 
    }


    /**
     * This method extracts values of parameters defined in configuration from
     * CHOrderContext. It compares those values to that defined in
     * configuration and returns <code>true</code> if all of them matches 
     * else returns <code>false</code>.
     * @param ctx CHOrderContext
     * @return boolean
     */
    public boolean evaluate(CHOrderContext ctx)
    {
        Iterator iter = paramMap.entrySet().iterator();
        while(iter.hasNext())
        {
            Map.Entry<String,String> entry = (Map.Entry<String,String>)iter.next();
            String paramNm = entry.getKey();
            String value = entry.getValue();
            String mpcVal = null;

            mpcVal = (String)ctx.getAttribute(paramNm);
            
            if(value != null && ( mpcVal == null || !value.contains(mpcVal) )){

                return false;
            }
        }

        // all param vals match
        return true;
    }

    public void addParam(String paramNm,String value)
    {
        paramMap.put(paramNm, value);
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[Condition: \n");
        sb.append(paramMap);
        sb.append("]");

        return sb.toString();
    }
}

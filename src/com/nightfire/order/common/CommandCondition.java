package com.nightfire.order.common;

import com.nightfire.framework.util.Debug;

public class CommandCondition
{
    private String paramName;
    private String paramValue;
    
    public CommandCondition(String paramName, String paramValue)
    {
        super();
        this.paramName = paramName;
        this.paramValue = paramValue;
    }

    public String getParamName()
    {
        return paramName;
    }

    public void setParamName(String paramName)
    {
        this.paramName = paramName;
    }

    public String getParamValue()
    {
        return paramValue;
    }

    public void setParamValue(String paramValue)
    {
        this.paramValue = paramValue;
    }

    public boolean evaluate(CHOrderContext context)
    {
        String contextValue = (String)context.getAttribute(paramName);

        if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log(Debug.MSG_LIFECYCLE, "Evaluating Command Condition paramname["+paramName+"],paramvalue["+paramValue+"],contextvalue["+contextValue+"]");

        if(contextValue == paramValue)
            return true;
        else if( contextValue != null && contextValue.equals(paramValue))
            return true;
        else if( paramValue != null && paramValue.equals(contextValue))
            return true;
        else
            return false;
    }

}

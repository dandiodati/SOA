package com.nightfire.order.common;

/**
 * Models parameter for command.
 * @author Abhishek Jain
 */
public class Param 
{
    /**
     * the parameter name in the context from which value has to be fetched. 
     */
    String from;
    /**
     * the parameter name in the context in which value has to be set.
     */
    String to;
    /**
     * value of the parameter found in the context. 
     */
    Object value;

    /**
     * constructs the param object using from and to fields.
     * @param from
     * @param to
     */
    public Param(String from, String to) 
    {
        super();
        this.from = from;
        this.to = to;
    }
    /**
     * @return returns the to value for this parameter.
     */
    public String getFrom() 
    {
        return from;
    }
    /**
     * sets the from value for this parameter.
     * @param from
     */
    public void setFrom(String from) 
    {
        this.from = from;
    }
    /**
     * @return returns the to value for this parameter.
     */
    public String getTo() 
    {
        return to;
    }
    /**
     * sets the to value for this parameter.
     * @param to 
     */
    public void setTo(String to) 
    {
        this.to = to;
    }
    /**
     * @return the value associated to this parameter.
     */
    public Object getValue() 
    {
        return value;
    }
    /**
     * sets the value associated to this parameter in the context.
     * @param value 
     */
    public void setValue(Object value) 
    {
        this.value = value;
    }

}

package com.nightfire.order.cfg;

import com.nightfire.framework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class containing a command configuration.
 * A command has input and output params.
 * Values of input params are extracted from CHOrderContext and
 * assigned to the associated POJO and after execution of command 
 * values of output params are extracted from POJO and are put in CHOrderContext.  
 *
 * @author hpirosha
 */
public class CommandCfg {

    private Map<String,String> cmdConditions;
    private Map<String,String> inParams;
    private Map<String,String> outParams;
    private String pojoType;
    private String mthd;
    private String name;
    private String createNewPojo;

    /**
     * Every command is configured with a pojo type and a method.
     * Command executes method on the pojo type configured.  
     * @param pojoType
     * @param mthd
     */
    public CommandCfg(String name,String pojoType,String mthd, String createNewPojo)
    {
        this.name = name;
        this.pojoType = pojoType;
        this.mthd = mthd;
        this.createNewPojo = createNewPojo;
        inParams = new LinkedHashMap<String,String>();
        outParams = new LinkedHashMap<String,String>();
        cmdConditions = new LinkedHashMap<String, String>();
    }

    public void addCmdCondition(Map<String,String> params) {
        cmdConditions.putAll(params);
    }

    public void addCmdCondition(String fromParam,String toParam) {
        cmdConditions.put(fromParam, toParam);
    }
    
    public void addInParam(Map<String,String> params) {
        inParams.putAll(params);
    }

    public void addInParam(String fromParam,String toParam) {
        inParams.put(fromParam, toParam);
    }

    public void addOutParam(String fromParam,String toParam) {
        outParams.put(fromParam, toParam);
    }

    public void addOutParam(Map<String,String> params) {
        outParams.putAll(params);
    }

    public String getMethod() {
        return mthd;
    }

    public Boolean getCreateNewPojo() {

        return StringUtils.getBoolean(createNewPojo, false);
    }

    /**
     * It is one of "order" ,"trans" or "event". 
     * @return
     */
    public String getPojoType() {
        return pojoType;
    }

    public Map getInParams() {
        return Collections.unmodifiableMap(inParams);
    }

    public Map getOutParams() {
        return Collections.unmodifiableMap(outParams);
    }

    public Map<String,String> getCmdConditions()
    {
        return Collections.unmodifiableMap(cmdConditions);
    }
    
    public String getName()
    {
        return name;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[Command name=:").append(name);
        sb.append("pojoType=:").append(pojoType);
        sb.append(" method=:").append(mthd);
        sb.append("\n cmdConditions=:").append(cmdConditions);
        sb.append("\n inputParameters=:").append(inParams);
        sb.append("\n outParameters=:").append(outParams);
        sb.append("]");

        return sb.toString();
    }

}

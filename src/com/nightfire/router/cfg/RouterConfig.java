package com.nightfire.router.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;

/**
 * Class encapsulating router configuration. 
 * @author hpirosha
 */
public class RouterConfig {

    private String cosNamePrefix;
    
    private List<MessageDirectorConfig> configLst;
    
    private Map<String ,String> commonAlias = new HashMap<String,String>();

    
    RouterConfig()
    {
        this.configLst = new LinkedList<MessageDirectorConfig>();

    }
    
    /**
     * 
     * @param cosNamePrefix
     */
    public void setCosNamePrefix(String cosNamePrefix)
    {
        this.cosNamePrefix = cosNamePrefix;
    }
    
    /**
     * 
     * @return
     */
    public String getCosNamePrefix()
    {
        return this.cosNamePrefix;
    }
    
    Map<String,MessageDirectorConfig> mdCfgMap = new HashMap<String,MessageDirectorConfig>();
    
    /**
     * 
     * @param director
     */
    public void addMessageDirector(MessageDirectorConfig director)
    {
        this.configLst.add(director);
        mdCfgMap.put(director.getDirectorType(), director);
    }
    
    /**
     * 
     * @param type
     * @return
     */
    public MessageDirectorConfig getDirectorConfig(String type)
    {
        return mdCfgMap.get(type);    
    }
    
    /**
     * 
     * @param aliasNm
     * @param realNm
     */
    public void addCommonAlias(String aliasNm,String realNm)
    {
        commonAlias.put(aliasNm, realNm);    
    }
    
    /**
     * 
     * @param aliasNm
     * @return
     */
    public String getRealName(String aliasNm)
    {
        return commonAlias.get(aliasNm);   
    }
    
    /**
     * Gets an iterator over message director
     * config  
     * @return
     */
    public Iterator getMessageDirectorCfg()
    {
        return configLst.iterator();
    }
    
    public Map<String,String> getCommonAlias()
    {
        return Collections.unmodifiableMap(commonAlias);
    }
    /**
     * 
     *
     */
    public static class MessageDirectorConfig
    {
        private String directorType ;
        
        private String directorClassNm ;
        
        /* actionAliasName,actionRealName */
        private Map<String,String> actionMap;
        
        private List<AlternateORB> altORBLst;
        
        private String cosNamePrefix;
        
        public MessageDirectorConfig(String directorType, String directorClassNm,Map<String,String> commonAlias, String cosNamePrefix)
        {
            this.directorType = directorType;
            this.directorClassNm = directorClassNm;
            this.altORBLst = new ArrayList<AlternateORB>();
            this.actionMap = new HashMap<String,String>();
            this.actionMap.putAll(commonAlias);
            this.cosNamePrefix = cosNamePrefix;
            
            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, "Alias map for director ["+directorType+"] is \n"+actionMap);
                    
        }
        
        public String getDirectorType()
        {
            return directorType;
        }
        
        public String getDirectorClassNm()
        {
            return directorClassNm;
        }

        public String getCosNamePrefix()
        {
            return this.cosNamePrefix;
        }

        public String getRealName(String aliasName)
        {
            String realName = actionMap.get(aliasName);
            
            String val = null;
            if(StringUtils.hasValue(realName))
                val = realName;
            else
                val = aliasName;
            
            if(Debug.isLevelEnabled(Debug.MAPPING_DATA))
                Debug.log(Debug.MAPPING_DATA, "Alias  ["+aliasName+"] -> RealName ["+val+"]");

            return val;
        }
        
        public void addAltORB(AlternateORB altORB)
        {
            this.altORBLst.add(altORB);
        }
        
        public Iterator getAltORBIter()
        {
            return this.altORBLst.iterator();
        }
        
        public void addAction(String aliasNm, String realNm)
        {
            this.actionMap.put(aliasNm, realNm);
        }
        
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            
            sb.append("\nMessageDirector : ")
            .append("\n\tclass_nm = "+this.directorClassNm)
            .append("\n\tcosNmPrefix= "+this.cosNamePrefix)
            .append("\n\ttype = "+this.directorType)
            .append("\n\tactions = "+this.actionMap)
            .append("\n\tORBRoutingRequestType = "+this.ORBRoutingRequestType)
            .append("\n\talternateORB = "+this.altORBLst);
            
            return sb.toString();
        }

        private String ORBRoutingRequestType = null; 
        public void setORBRoutingRequestType(String type)
        {
            ORBRoutingRequestType = type;
        }
        
        public String getORBRoutingRequestType()
        {
            return ORBRoutingRequestType;
        }
    }
    
    /**
     * 
     *
     */
    public static class AlternateORB 
    {
        private String addr, port, condition;
        
        public AlternateORB(String addr, String port, String condition)
        {
            this.addr = addr;
            this.port = port;
            this.condition = condition;
        }

        public String getAddr() {
            return addr;
        }

        public String getCondition() {
            return condition;
        }

        public String getPort() {
            return port;
        }
        

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            
            sb.append("\n\t\taddr = "+this.addr)
            .append("\n\t\tport ="+this.port)
            .append("\n\t\tcondition ="+this.condition);
            
            return sb.toString();
        }

    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("COS Name Prefix = "+this.cosNamePrefix)
        .append("\nCommon Aliases = "+this.commonAlias)
        .append(this.configLst);
        
        return sb.toString();
    }
    
}


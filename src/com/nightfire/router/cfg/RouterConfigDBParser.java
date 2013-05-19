package com.nightfire.router.cfg;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.PropertyException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.NVPair;
import com.nightfire.router.cfg.RouterConfig;

/**
 * Class to build routing configuration from database.
 * This class uses CORBA router chain stored in persistentproperty
 * table to build routing configuration. 
 * @author hpirosha
 */
public class RouterConfigDBParser {

    /**
     * Method to load configuration from persistent property. It has the same procedure to load
     * properties as the CORBA request router.
     * 
     * @return RouterConfig data object containing routing configuration from database
     */
    public RouterConfig parse() throws FrameworkException
    {
        RouterConfig config = null;
        try
        {
            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Fetching message routing configuration from persistent property");

            String cosNmPrefix = 
                PersistentProperty.getProperty(REQUEST_ROUTER_KEY, SUPERVISOR_TYPE, COS_PREFIX_PROPNM);

            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Got cos name prefix :"+cosNmPrefix);

            config = new RouterConfig();
            config.setCosNamePrefix(cosNmPrefix);
            
            Map commonAliases = PersistentProperty.getProperties(REQUEST_ROUTER_KEY, COMMON_ALIASES_TYPE);
            
            if(commonAliases!=null)
            for(Object alias : commonAliases.keySet())
            {
                String aliasPropNm = (String)alias;
                if(aliasPropNm.startsWith(ALIAS_PREFIX))
                 {
                    String aliasVal = (String)commonAliases.get(aliasPropNm);
                    String realNmVal = (String)commonAliases.get(REALNAME_PREFIX+aliasPropNm.substring(6));
                    config.addCommonAlias(aliasVal, realNmVal);
                 }
            }

            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Got common aliases :"+commonAliases);

            Hashtable mdCfg = PersistentProperty.getProperties(REQUEST_ROUTER_KEY, CHOICE_EVALUATOR_TYPE);
            
            //MESSAGE_DIRECTOR_TYPE_0
            NVPair[] directorTypePair = PersistentProperty.getPropertiesLike(mdCfg, DIRECTOR_TYPE);
    
            //MESSAGE_DIRECTOR_CLASS_0
            NVPair[] directorClassPair = PersistentProperty.getPropertiesLike(mdCfg, DIRECTOR_CLASS);
            
            if(directorTypePair.length!=directorClassPair.length)
                throw new FrameworkException("Invalid configuration, message directors doesn't have class/type defined !!");    
            
            int i=0;
            for(NVPair pair: directorTypePair)
            {
                String mdType = (String)pair.getValue();
                String mdClass = null;  

                if(mdType.equals(ACTION_DIRECTOR_TYPE))
                    mdClass = "com.nightfire.router.standalone.ActionDirectorV2";
                else if(mdType.equals(VERSIONED_DIRECTOR_TYPE))
                    mdClass = "com.nightfire.router.standalone.VersionedGatewayDirectorV2";
                else if(mdType.equals(ILECT_DIRECTOR_TYPE))
                    mdClass = "com.nightfire.router.standalone.IlecTypeDirectorV2";
                else
                    throw new FrameworkException("Unknown director type :["+mdType+"] , " +
                            "please verify router configuration. Valid types are ["+ACTION_DIRECTOR_TYPE+", " +
                            VERSIONED_DIRECTOR_TYPE+" and "+ILECT_DIRECTOR_TYPE+" ]");

                if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                    Debug.log(Debug.STATE_LIFECYCLE,"Got message director type["+mdType
                            +"] and class["+mdClass+"]");

                RouterConfig.MessageDirectorConfig director =  new RouterConfig.MessageDirectorConfig(mdType,mdClass,config.getCommonAlias(),cosNmPrefix);
                populateAlias(director);
                populateAlternateORB(director);
                
                config.addMessageDirector(director);
                i++;
            }
            
            if(Debug.isLevelEnabled(Debug.STATE_DATA))
                Debug.log(Debug.STATE_DATA, "Loaded [" + i + "] routing configurations :"+config.toString());
            
            return config;
        }
        catch(PropertyException pe)
        {
            Debug.error("An exception occured while getting persistent properties");
            Debug.error(Debug.getStackTrace(pe));
            throw new FrameworkException(pe);
        }
    }

    /**
     * Helper method to populate alias
     * @param mdCfg
     * @throws PropertyException
     * @throws FrameworkException
     */
    private void populateAlias(RouterConfig.MessageDirectorConfig mdCfg) 
            throws PropertyException, FrameworkException
    {
        String type = mdCfg.getDirectorType();
        Hashtable mdProps = PersistentProperty.getProperties(REQUEST_ROUTER_KEY, type);
        
        NVPair[] aliasPairs = PersistentProperty.getPropertiesLike(mdProps,ALIAS_ACTION);
        NVPair[] realNmPairs = PersistentProperty.getPropertiesLike(mdProps,REALNAME_ACTION);
        
        if(aliasPairs==null && realNmPairs==null)
            return;
        
        if(!(aliasPairs!=null && realNmPairs!=null))
            throw new FrameworkException("Invalid configuration for message directors, " +
                    "either alias or realname is undefined !!");
        
        if((aliasPairs.length!=realNmPairs.length))
            throw new FrameworkException("Invalid configuration for message directors, " +
                    "all alias name doesnot have a real name !!");
        
        int i = 0;
        for(NVPair aliasPair: aliasPairs)
        {
            String alias = (String)aliasPair.getValue();
            String realNm = (String)realNmPairs[i].getValue();

            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Got alias aliasNm["
                        +alias+"] realNm["+realNm+"]");

            mdCfg.addAction(alias, realNm);
            i++;
        }
    }

    /**
     * Helper method to populate alternate orb locations.
     * @param mdCfg
     * @throws PropertyException
     * @throws FrameworkException
     */
    private void populateAlternateORB(RouterConfig.MessageDirectorConfig mdCfg)
        throws PropertyException, FrameworkException
    {
        Hashtable mdProps = PersistentProperty.getProperties(REQUEST_ROUTER_KEY, SUPERVISOR_TYPE);
        
        //AltORBagentAddr_0
        NVPair[] addrPairs = PersistentProperty.getPropertiesLike(mdProps, ALTORB_ADDR);

        //AltORBagentPort_0
        NVPair[] portPairs = PersistentProperty.getPropertiesLike(mdProps, ALTORB_PORT);

        //AltORBRoutingCondition_0
        NVPair[] condPairs = PersistentProperty.getPropertiesLike(mdProps, ALTORB_COND);
        
        if(addrPairs==null && portPairs==null && condPairs==null)
            return;

        if(!(addrPairs!=null && portPairs!=null && condPairs!=null))
            throw new FrameworkException("Invalid configuration for message directors, " +
                    "all alternate ORB's doesnt have addr/port/condition defined !!");

        if(addrPairs.length!=portPairs.length &&
                portPairs.length!=condPairs.length)
         
            throw new FrameworkException("Invalid configuration for message directors, " +
                    "all alternate ORB's doesnt have addr/port/condition defined !!");
        
        String orbReqType = null;
        
        try
        {
            orbReqType = PersistentProperty.get(REQUEST_ROUTER_KEY, 
                SUPERVISOR_TYPE, ORBRoutingRequestType);
        }
        catch(PropertyException ignore) {
            /* in case the property doesn't exists */
        }
        
        mdCfg.setORBRoutingRequestType(orbReqType);

        int i = 0;
        for(NVPair addrPair: addrPairs)
        {
            String addr = (String)addrPair.getValue();
            String port = (String)portPairs[i].getValue();
            String cond = (String)condPairs[i].getValue();

            RouterConfig.AlternateORB orb = new RouterConfig.AlternateORB(addr,port,cond);
            mdCfg.addAltORB(orb);
            i++;
            
            if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                Debug.log(Debug.STATE_LIFECYCLE,"Got alternate ORB addr["
                            +addr+"] port["+port+"] condition["+cond+"]");
        }
    }
    
    private static final String ORBRoutingRequestType = "ORBRoutingRequestType";
    private static final String ALTORB_ADDR= "AltORBagentAddr";
    private static final String ALTORB_PORT= "AltORBagentPort";
    private static final String ALTORB_COND = "AltORBRoutingCondition";
    private static final String ALIAS_ACTION = "ALIAS_ACTION";
    private static final String REALNAME_ACTION = "REALNAME_ACTION";
    private static final String REQUEST_ROUTER_KEY = "REQUEST_ROUTER";
    private static final String SUPERVISOR_TYPE = "SUPERVISOR";
    private static final String COS_PREFIX_PROPNM = "COS_NS_PREFIX";
    private static final String COMMON_ALIASES_TYPE = "COMMON_ALIASES";
    private static final String ALIAS_PREFIX = "ALIAS_";
    private static final String REALNAME_PREFIX = "REALNAME_";
    private static final String CHOICE_EVALUATOR_TYPE = "CHOICE_EVALUATOR";
    private static final String DIRECTOR_TYPE = "MESSAGE_DIRECTOR_TYPE";
    private static final String DIRECTOR_CLASS = "MESSAGE_DIRECTOR_CLASS";
    private static final String ACTION_DIRECTOR_TYPE = "ACTION_DIRECTOR";
    private static final String VERSIONED_DIRECTOR_TYPE= "VERSIONED_GATEWAY";
    private static final String ILECT_DIRECTOR_TYPE = "ILEC_TYPE";
//    private static final String CIC_DIRECTOR_TYPE = "CIC_OPERATION";
    
    
    public static void main(String[] args) throws Exception
    {
        Properties props = new Properties();
        props.put(Debug.LOG_FILE_NAME_PROP, "e:/pp.log");
        props.put(Debug.DEBUG_LOG_LEVELS_PROP, "ALL");
        Debug.configureFromProperties(props);

        DBInterface.initialize("jdbc:oracle:thin:@192.168.64.125:1521:ORCL132","suninstall","suninstall");
        RouterConfigDBParser parser = new RouterConfigDBParser();
        
        RouterConfig config = parser.parse();
        
        System.out.println("loaded cfg : "+config);
    }

}

package com.nightfire.router.cfg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.router.cfg.RouterConfig;

/**
 * XML Parser class for router configuration. 
 * @author hpirosha
 */
public class RouterConfigXMLParser {

    public RouterConfigXMLParser()
    {
    }
    
    
    private static String ELEM_ROUTER_CFG = "routing_config";
    private static String ATTR_COS_PREFIX = "cos_ns_prefix";
    private static String ELEM_DIRECTOR = "director";
    private static String ELEM_COMMON_ALIAS = "common_aliases";
    
    private RouterConfig rc = null;
    String cosPrefix =  null;
    
    public RouterConfig parse(Document doc) 
    {
        if(doc==null)
            throw new RuntimeException("Router configuration document cannot be null !!");
        
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Parsing message routing configuration");

        NamedNodeMap attributes = 
                     doc.getElementsByTagName(ELEM_ROUTER_CFG).item(0).getAttributes();
        
        cosPrefix  = attributes.getNamedItem(ATTR_COS_PREFIX).getNodeValue();

        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Got cos name prefix :"+cosPrefix);
        
        rc = new RouterConfig(); 
        rc.setCosNamePrefix(cosPrefix);

        NodeList cmnAliasLst = doc.getElementsByTagName(ELEM_COMMON_ALIAS);
        if(cmnAliasLst!=null)
            parseCommonAliases((Element)cmnAliasLst.item(0));
        
        NodeList directorNodeLst = doc.getElementsByTagName(ELEM_DIRECTOR);
        
        if(directorNodeLst!=null) 
        {
            for(int i=0; i < directorNodeLst.getLength() ; i++)
            {
                Node node = directorNodeLst.item(i);
                
                if(node instanceof Element)
                {
                    Element directorElem = (Element)node;
                    RouterConfig.MessageDirectorConfig mdCfg = 
                        parseDirectorElem(directorElem);
                    
                    rc.addMessageDirector(mdCfg);
                }
            }
        }
        
        return rc;
    }
    
    private void parseCommonAliases(Element cmnAliasElem) 
    {
        NodeList actionNodeLst = cmnAliasElem.getElementsByTagName(ELEM_ACTION);
        
        if(actionNodeLst!=null)
            for(int i=0 ; i<actionNodeLst.getLength() ; i++)
            {
                Node node = actionNodeLst.item(i);
                
                if(node instanceof Element)
                {
                    Element actionElem = (Element)node;
                    
                    String[] alias = parseAlias(actionElem);
                    
                    if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                        Debug.log(Debug.STATE_LIFECYCLE,"Got alias aliasNm["
                                +alias[0]+"] realNm["+alias[1]+"]");

                    rc.addCommonAlias(alias[0], alias[1]);
                }
            }
    }

    private RouterConfig.MessageDirectorConfig parseDirectorElem(Element directorElem)
                            
    {
        String typeAttr = directorElem.getAttribute(ATTR_TYPE);
        
        String classNmAttr  = directorElem.getAttribute(ATTR_CLASS_NM);
        
        RouterConfig.MessageDirectorConfig mdCfg 
                    = new RouterConfig.MessageDirectorConfig(typeAttr, classNmAttr,rc.getCommonAlias(),cosPrefix);
        
        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Got director type["+typeAttr+"] class["+classNmAttr+"]");

        NodeList orbNodeLst = directorElem.getElementsByTagName(ELEM_ALT_ORB);
        
        if(orbNodeLst!=null)
            for(int i=0 ; i<orbNodeLst.getLength() ; i++)
            {
                Node node = orbNodeLst.item(i);
                
                if(node instanceof Element)
                {
                    Element orbElem = (Element)node;
                    
                    RouterConfig.AlternateORB alternateORB = parseAltORBELem(orbElem);
                    
                    mdCfg.addAltORB(alternateORB);
                }
            }
        
        
        NodeList actionNodeLst = directorElem.getElementsByTagName(ELEM_ACTION);
       
        if(actionNodeLst!=null)
            for(int i=0 ; i<actionNodeLst.getLength() ; i++)
            {
                Node node = actionNodeLst.item(i);
                
                if(node instanceof Element)
                {
                    Element actionElem = (Element)node;
                    
                    String[] alias = parseAlias(actionElem);
                    
                    if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
                        Debug.log(Debug.STATE_LIFECYCLE,"Got alias aliasNm["
                                +alias[0]+"] realNm["+alias[1]+"]");

                    mdCfg.addAction(alias[0], alias[1]);
                }
            }
        
        
        return mdCfg;
    }
    
    
    private RouterConfig.AlternateORB parseAltORBELem(Element altORBElem) 
                    
    {
        String addr = 
            altORBElem.getElementsByTagName(ELEM_ADDR).item(0).getAttributes().getNamedItem(ATTR_VALUE).getNodeValue();

        String port = 
            altORBElem.getElementsByTagName(ELEM_PORT).item(0).getAttributes().getNamedItem(ATTR_VALUE).getNodeValue();

        String condition = 
            altORBElem.getElementsByTagName(ELEM_CONDITION).item(0).getAttributes().getNamedItem(ATTR_VALUE).getNodeValue();

        if(Debug.isLevelEnabled(Debug.STATE_LIFECYCLE))
            Debug.log(Debug.STATE_LIFECYCLE,"Got alternate ORB addr["
                        +addr+"] port["+port+"] condition["+condition+"]");
        
        RouterConfig.AlternateORB altORB = new RouterConfig.AlternateORB(addr,port,condition);
        
        return altORB;
    }
    
    private String[] parseAlias(Element actionElem)
    {
        String alias = 
            actionElem.getElementsByTagName(ELEM_ALIAS).item(0).getAttributes().getNamedItem(ATTR_VALUE).getNodeValue();

        String realNm = 
            actionElem.getElementsByTagName(ELEM_REALNM).item(0).getAttributes().getNamedItem(ATTR_VALUE).getNodeValue();

        return new String[] {alias, realNm};
    }
    
    private static String ATTR_TYPE = "type"; 
    private static String ATTR_CLASS_NM = "class_nm";
    private static String ELEM_ALT_ORB = "alternate_ORB";
    private static String ELEM_ACTION = "action";
    private static String ELEM_ALIAS = "alias";
    private static String ELEM_REALNM = "realname";
    private static String ELEM_ADDR = "addr";
    private static String ELEM_PORT = "port";
    private static String ELEM_CONDITION = "condition";
    private static String ATTR_VALUE = "value";
    
    public static void main(String[] args) throws Exception
    {
        String routerCfgStr = FileUtils.readFile("D:/GW/repository/DEFAULT/router/router_config.xml");
        
        Document dom = XMLLibraryPortabilityLayer.convertStringToDom(routerCfgStr);
        
        RouterConfigXMLParser parser = new RouterConfigXMLParser();
        
        RouterConfig config = parser.parse(dom);
        
        System.out.println("loaded cfg : "+config);
    }
}

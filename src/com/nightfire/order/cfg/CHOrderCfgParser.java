package com.nightfire.order.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.util.Debug;

/**
 * XML Parser for CHOrderConfig 
 * @author hpirosha
 *
 */
public class CHOrderCfgParser 
{
    /**
     * Singleton parser object 
     */
    private static CHOrderCfgParser parser;


    /**
     * Use this method to get an instance of the parser. 
     * @return CHOrderCfgParser
     */
    public static CHOrderCfgParser getInstance()
    {
        if(parser!=null)
            return parser;

        synchronized (CHOrderCfgParser.class) 
        {
            if(parser==null)
                parser = new CHOrderCfgParser();
        }

        return parser;
    }

    /**
     * Parse a document containing order configuration.  
     * @param orderCfg
     * @return
     */
    public CHOrderCfg parse(Document orderCfg)
    {

        String productNm = null;
        NodeList childNodes = orderCfg.getElementsByTagName(ORDER_LOGGER_ELEM);
        int length = childNodes.getLength();

        for(int i=0;i<length;i++)
        {
            Node childNode = childNodes.item(i);
            if(childNode instanceof Element)
            {
                Element orderLoggerElem = (Element)childNode; 
                productNm = orderLoggerElem.getAttribute(PRODUCT_ELEM);
                String loggerClassNm = orderLoggerElem.getAttribute(LOGGER_CLASS_NM);
                
                OrderLoggerCfg cfg = new OrderLoggerCfg(productNm,loggerClassNm);
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                {
                    Debug.log(Debug.MSG_STATUS," product:="+productNm);
                    Debug.log(Debug.MSG_STATUS," classNm:="+loggerClassNm);
                }

                // get all the cases
                NodeList caseNodes = orderLoggerElem.getChildNodes();
                for(int j=0; j<caseNodes.getLength();j++)
                {
                    Node caseNode = caseNodes.item(j);
                    if(caseNode instanceof Element && caseNode.getNodeName().equals(CASE_ELEM))
                    {
                        CaseCfg caseCfg = getCaseCfg((Element)caseNode);
                        cfg.addCase(caseCfg);
                    }
                }

                CHOrderCfg.getInstance(productNm).setOdrLoggerCfg(cfg);
                break;
            }
        }

        return CHOrderCfg.getInstance(productNm);
    }

    /**
     * Extract case configuration from the given element.  
     * @param elem
     * @return CaseCfg
     */
    private CaseCfg getCaseCfg(Element elem)
    {
        String desc = elem.getAttribute(DESC_ATTR);
        NodeList condLst = elem.getElementsByTagName(CONDITION_ELEM);
        Condition condition = new Condition();

        for(int i=0;i<condLst.getLength();i++)
        {
            Node childNode = condLst.item(i);
            if(childNode instanceof Element) 
            {
                Element condElem = (Element)childNode;
                NodeList paramLst = condElem.getElementsByTagName(PARAM_ELEM);
                for(int j=0;j<paramLst.getLength();j++)
                {
                    Node paramNode = paramLst.item(j);
                    if(paramNode instanceof Element && paramNode.getNodeName().equals(PARAM_ELEM)) 
                    {
                        Element paramElem = (Element)paramNode;
                        String name = paramElem.getAttribute(NM_ATTR);
                        String value = paramElem.getAttribute(VAL_ATTR);
                        condition.addParam(name, value);

                        Debug.log(Debug.MSG_STATUS,"Condition param:="+name+" # "+value);
                    }
                }
                break;
            }
        }

        CaseCfg cfg = new CaseCfg(desc,condition);
        NodeList cmdLst = elem.getElementsByTagName(COMMAND_ELEM);
        for(int i=0;i<cmdLst.getLength();i++)
        {

            Node childNode = cmdLst.item(i);
            if(childNode instanceof Element && childNode.getNodeName().equals(COMMAND_ELEM)) 
            {

                Element cmdElem = (Element)childNode;
                String pojoType = cmdElem.getAttribute(POJOTYPE_ATTR);
                String mthd = cmdElem.getAttribute(METHOD_ATTR);
                String name = cmdElem.getAttribute(NM_ATTR);
                String create_new_pojo = cmdElem.getAttribute(CREATE_NEW_POJO_ATTR);

                CommandCfg cmdCfg = new CommandCfg(name, pojoType, mthd, create_new_pojo);
                NodeList paramNodes = cmdElem.getElementsByTagName(PARAMS_ELEM);
                for(int j=0;j<paramNodes.getLength();j++)
                {
                    Node paramNode = paramNodes.item(j);
                    if(paramNode instanceof Element && paramNode.getNodeName().equals(PARAMS_ELEM))
                    {
                        Map<String,String> inParms = getParams((Element)paramNode,IN_ELEM);
                        cmdCfg.addInParam(inParms);

                        Map<String,String> outParms = getParams((Element)paramNode,OUT_ELEM);
                        cmdCfg.addOutParam(outParms);
                    }
                }
                
                Map<String,String> cmdParams = getCommandParams((Element)cmdElem,CMD_CONDITION_ELEM);
                cmdCfg.addCmdCondition(cmdParams); 
                cfg.addCommand(cmdCfg);
            }
        }

        Debug.log(Debug.MSG_STATUS,"CaseCfg : "+cfg);
        return cfg;
    }

    /**
     * Utility method to extract in and out params from a params element. 
     * @param paramsElem
     * @param elemNm
     * @return
     */
    private Map<String,String> getParams(Element paramsElem,String elemNm)
    {
        Map<String,String> parms = new LinkedHashMap<String, String>();
        NodeList inNodes = paramsElem.getElementsByTagName(elemNm);
        for(int i=0; i<inNodes.getLength(); i++)
        {
            Node childNode = inNodes.item(i);
            if(childNode instanceof Element)
            {

                NodeList paramNodes = ((Element)childNode).getElementsByTagName(PARAM_ELEM);
                for(int j=0; j<paramNodes.getLength(); j++)
                {

                    Node paramNode = paramNodes.item(j);
                    if(paramNode instanceof Element)
                    {
                        String fromParam = ((Element)paramNode).getAttribute(FROM_ATTR);
                        String toParam = ((Element)paramNode).getAttribute(TO_ATTR);
                        Debug.log(Debug.MSG_STATUS," from=: "+fromParam+" to=: "+toParam);
                        parms.put(fromParam, toParam);
                    }
                }				
            }

        }
        return parms;
    }

    /**
     * Utility method to extract in and out params from a params element. 
     * @param paramsElem
     * @param elemNm
     * @return
     */
    private Map<String,String> getCommandParams(Element paramsElem,String elemNm)
    {
        Map<String,String> parms = new LinkedHashMap<String, String>();
        NodeList inNodes = paramsElem.getElementsByTagName(elemNm);
        for(int i=0; i<inNodes.getLength(); i++)
        {
            Node childNode = inNodes.item(i);
            if(childNode instanceof Element)
            {

                NodeList paramNodes = ((Element)childNode).getElementsByTagName(PARAM_ELEM);
                for(int j=0; j<paramNodes.getLength(); j++)
                {

                    Node paramNode = paramNodes.item(j);
                    if(paramNode instanceof Element)
                    {
                        String fromParam = ((Element)paramNode).getAttribute(NM_ATTR);
                        String toParam = ((Element)paramNode).getAttribute(VAL_ATTR);
                        parms.put(fromParam, toParam);
                    }
                }               
            }

        }
        return parms;
    }
    
    private static final String LOGGER_CLASS_NM = "class_nm";
    private static final String CONDITION_ELEM = "condition";
    private static final String CASE_ELEM = "case";
    private static final String PRODUCT_ELEM = "product";
    private static final String DESC_ATTR = "desc";
    private static final String POJOTYPE_ATTR = "pojo_type";
    private static final String METHOD_ATTR = "method";
    private static final String NM_ATTR = "name";
    private static final String VAL_ATTR = "value";
    private static final String FROM_ATTR = "from";
    private static final String TO_ATTR = "to";
    private static final String IN_ELEM = "in";
    private static final String OUT_ELEM = "out";
    private static final String CMD_CONDITION_ELEM = "command-condition";
    private static final String PARAM_ELEM = "param";
    private static final String PARAMS_ELEM = "params";
    private static final String COMMAND_ELEM = "command";
    private static final String ORDER_LOGGER_ELEM = "order_logger";
    private static final String CREATE_NEW_POJO_ATTR = "create_new_pojo";

    public static void main(String args[]) throws Exception
    {
        File file = new File("d://work//ICPORDERBASED//configfile//test_order_cfg.xml");
        FileInputStream fis = new FileInputStream(file);
        Document doc = XMLLibraryPortabilityLayer.convertStreamToDom(fis);
        CHOrderCfgParser.getInstance().parse(doc);
    }
}

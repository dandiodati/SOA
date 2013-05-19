package com.nightfire.router.standalone;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.idl.RequestHandler;
import com.nightfire.router.cfg.RouterConfig.MessageDirectorConfig;
import com.nightfire.router.cmn.MessageDirectorV2;
import com.nightfire.router.messagedirector.VersionedGatewayDirector;
import com.nightfire.spi.common.HeaderNodeNames;

/**
 * New VersionedGatewayDirector class without any interaction with CORBA layer.  
 * @author hpirosha
 */
public class VersionedGatewayDirectorV2 extends VersionedGatewayDirector implements MessageDirectorV2  {

    private DirectorHelper helper = null;
    private MessageDirectorConfig cfg = null;
    
    
    /**
     * Initialize message director with its configuration 
     *
     * @param cfg
     * @throws ProcessingException
     */
    public void initialize(MessageDirectorConfig cfg) throws ProcessingException
    {
        this.cfg = cfg;
        helper = new DirectorHelper();
        this.ORBRoutingRequestType = cfg.getORBRoutingRequestType();
    }

    /**
     * @param header
     * @param message
     * @throws MessageException 
     */
    public boolean canRoute(Document header, Object message) 
                        throws MessageException
    {
        return super.canRoute(header, message);
    }
    

    @Override
    /**
     * @param header
     * @param message
     * @throws ProcessingException, MessageException
     */
    protected RequestHandler getHandler(Document header, Object message) throws ProcessingException, MessageException    
    {
        String cosPrefix = cfg.getCosNamePrefix(); 

        StringBuilder cosNm = new StringBuilder(cosPrefix);
        cosNm.append(PERIOD);

        XMLMessageParser parser = new XMLMessageParser(header);
        String requestType = parser.getValue (HeaderNodeNames.REQUEST_NODE);
        String supplier = helper.getRealName(cfg,parser.getValue(HeaderNodeNames.SUPPLIER_NODE));
        
        String subReqNodeVal = null;
        if(parser.exists (HeaderNodeNames.SUBREQUEST_NODE) )
             subReqNodeVal = parser.getValue(HeaderNodeNames.SUBREQUEST_NODE);

        String gwSuppName = getGWSupplier(supplier,requestType,subReqNodeVal);
        cosNm.append(gwSuppName).append(PERIOD);

        /* Add InterfaceVersion */
        cosNm.append(parser.getValue(HeaderNodeNames.INTERFACE_VERSION_NODE));
        cosNm.append(PERIOD).append(requestType);


        if(StringUtils.hasValue(subReqNodeVal)) 
            cosNm.append(PERIOD).append(subReqNodeVal);

        String cid = null;
        if(parser.exists(HeaderNodeNames.CUSTOMER_ID_NODE)) 
           cid = parser.getValue(HeaderNodeNames.CUSTOMER_ID_NODE);
        
        StringBuilder errBuf = new StringBuilder();

        RequestHandler gatewayHandle = null;
        String cosNameWithCID = null;
        
        /*  first try to find a customer specific server */
        if(StringUtils.hasValue(cid))
        {
            cosNameWithCID = new StringBuilder(cosNm.toString()).append("_").append(cid).toString();
            gatewayHandle = helper.findHandle(cfg,cosNameWithCID, subReqNodeVal,message,requestType, errBuf);
        }

        /* if we did not find one try to find a default server */
        if(gatewayHandle == null)
            gatewayHandle = helper.findHandle(cfg,cosNm.toString(),subReqNodeVal,message,requestType, errBuf);
        
        /* if still we haven't found the gateway then search in alternate ORB's */
        if(gatewayHandle==null)
        {
           /* first search with cos name + customerid */
          if(StringUtils.hasValue(cid))
                gatewayHandle = helper.findHandleInAltORB(cfg, cosNameWithCID, subReqNodeVal, errBuf);
          
          /* gateway still not found, try to find one with only cos name */
          if(gatewayHandle==null)
              gatewayHandle = helper.findHandleInAltORB(cfg, cosNm.toString(), subReqNodeVal, errBuf);
        }   

        /* If we still did not find one at this point, we never will */
        if (gatewayHandle == null)
           throw new ProcessingException("VersionedGatewayDirectorV2: Could not locate server, the following servers were tried :\n" + errBuf.toString());

         if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "VersionedGatewayDirectorV2: Found server [" + gatewayHandle +"]");

        return gatewayHandle;
    }

}

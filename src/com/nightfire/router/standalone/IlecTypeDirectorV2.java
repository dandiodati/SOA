package com.nightfire.router.standalone;

import java.util.StringTokenizer;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.idl.RequestHandler;
import com.nightfire.router.cfg.RouterConfig.MessageDirectorConfig;
import com.nightfire.router.cmn.MessageDirectorV2;
import com.nightfire.router.messagedirector.IlecTypeDirector;
import com.nightfire.spi.common.HeaderNodeNames;

/**
 * New IlectTypeDirector class without any interaction with CORBA layer.  
 * @author hpirosha
 */
public class IlecTypeDirectorV2 extends IlecTypeDirector implements MessageDirectorV2  {

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
    protected RequestHandler getHandler(Document header, Object message) 
            throws ProcessingException, MessageException
    {
            String cosPrefix = cfg.getCosNamePrefix(); 
            XMLMessageParser parser = new XMLMessageParser(header);
        
            StringBuilder cosNm = new StringBuilder(cosPrefix);
            cosNm.append(PERIOD);

            /*
              Parsing requestType here so that it can be used as a
              parameter to validate the trading partner (value from SUPPLIER_NODE).
              here requestType will be in format such as "lsr_order", "lsr_preorder" etc.
             */
            String requestType = parser.getValue(HeaderNodeNames.REQUEST_NODE);
            String supplier = helper.getRealName(cfg,parser.getValue(HeaderNodeNames.SUPPLIER_NODE));
        
            String subReqNodeVal = null;
            if(parser.exists(HeaderNodeNames.SUBREQUEST_NODE))
              subReqNodeVal = parser.getValue(HeaderNodeNames.SUBREQUEST_NODE);
        
            String gwSuppName = getGWSupplier(supplier,requestType,subReqNodeVal);
            cosNm.append(gwSuppName).append(PERIOD).append(requestType);
        
        
            if(StringUtils.hasValue(subReqNodeVal)) 
                cosNm.append(PERIOD).append(subReqNodeVal);
        
            if(parser.exists(HeaderNodeNames.SUBTYPE_NODE)) 
            {
               String subTypeNodeVal = helper.getRealName(cfg,parser.getValue(HeaderNodeNames.SUBTYPE_NODE));
               
               if(StringUtils.hasValue(subTypeNodeVal)) 
                  cosNm.append(PERIOD).append(subTypeNodeVal);
             }
        
        
            StringBuilder errBuf = new StringBuilder();
        
            RequestHandler gatewayHandle = null;
        
           /* If ORB-Routing-RequestType list is available and
              the current request-type is included in the pipe-separated-list,
              Get the routing-condition that holds true for the current message.
              Use the routing-condition as the key to locate the server from the alternate ORB. */
           if(StringUtils.hasValue(ORBRoutingRequestType) && 
                   (StringUtils.indexOfIgnoreCase(ORBRoutingRequestType, requestType) > -1))
           {
               StringTokenizer stk = new StringTokenizer(ORBRoutingRequestType, PIPE_SEP);
               while(stk.hasMoreTokens())
               {
                   String token = stk.nextToken(); 
                   if(token.equalsIgnoreCase(requestType))
                   {
                       if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                           Debug.log(Debug.MSG_STATUS, "IlecTypeDirectorV2: " +
                                "Request-Type [" + requestType + "] is valid for Conditional ORB Routing");
        
                       /* find the X-path which evaluates to true for the current message-body */
                       String orbRoutingKey = helper.getORBRoutingKey(cfg, message);
            
                       if(StringUtils.hasValue(orbRoutingKey))
                       {
                           gatewayHandle = helper.findHandle(cfg, cosNm.toString(), subReqNodeVal, orbRoutingKey, errBuf);
                           if(gatewayHandle!=null)
                               return gatewayHandle;
                       }
                       
                       break;
                   }
               }
           }
           else
           {
                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                     Debug.log(Debug.MSG_STATUS, "IlecTypeDirectorV2: Request-Type [" 
                            + requestType + "] is not valid for Conditional ORB Routing," +
                            " so skipping conditional ORB Routing");
           }
       
           String cid = null;
           if(parser.exists(CustomerContext.CUSTOMER_ID_NODE)) 
              cid = parser.getValue(CustomerContext.CUSTOMER_ID_NODE);

           String cosNmWithCID = null;
            /* 
             * first try to find a customer specific server, 
             *  if server could not be located using the above alternate ORB 
             */
            if(StringUtils.hasValue(cid) && (gatewayHandle==null))
            {
                cosNmWithCID = new StringBuilder(cosNm.toString()).append("_").append(cid).toString();
                gatewayHandle = helper.findHandle(cfg,cosNmWithCID,subReqNodeVal,message,requestType, errBuf);
            }
        
            /* if we did not find one try to find a default server */
            if(gatewayHandle == null)
                gatewayHandle = helper.findHandle(cfg,cosNm.toString(),subReqNodeVal,message, requestType,errBuf);

            /* if still we haven't found the gateway then search in alternate ORB's */
            if(gatewayHandle==null)
            {
               /* first search with cos name + customerid */
              if(StringUtils.hasValue(cid))
                    gatewayHandle = helper.findHandleInAltORB(cfg, cosNmWithCID, subReqNodeVal, errBuf);
              
              /* gateway still not found, try to find one with only cos name */
              if(gatewayHandle==null)
                  gatewayHandle = helper.findHandleInAltORB(cfg, cosNm.toString(), subReqNodeVal, errBuf);
            }   

            
            /* If we still did not find one at this point, we never will */
            if(gatewayHandle == null)
               throw new ProcessingException("IlecTypeDirectorV2: " +
                    "Could not locate server, the following servers were tried :\n" + errBuf.toString());

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "IlecTypeDirectorV2: Found server [" + gatewayHandle +"]");
        
            return gatewayHandle;
        }
    }



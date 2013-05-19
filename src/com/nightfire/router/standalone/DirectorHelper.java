package com.nightfire.router.standalone;

import java.util.Iterator;
import java.util.StringTokenizer;


import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.idl.RequestHandler;
import com.nightfire.idl.RequestHandlerHelper;
import com.nightfire.router.cfg.RouterConfig.AlternateORB;
import com.nightfire.router.cfg.RouterConfig.MessageDirectorConfig;

/**
 * Helper class for message directors. 
 * @author hpirosha
 *
 */
public class DirectorHelper {

    private static String PIPE_SEP = "|";
    public DirectorHelper()
    {
        
    }
    
    /**
     * 
     * @param cosNm
     * @param subReqNodeValue
     * @param errBuf
     * @return
     * @throws ProcessingException
     */
    public RequestHandler findHandle(MessageDirectorConfig cfg, String cosNm,
            String subReqNodeValue, Object message,String requestType, StringBuilder errBuf) throws ProcessingException
    {

     if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS,"DirectorHelper: Looking up alias for server name : " + cosNm);

     String realCosNm = cfg.getRealName(cosNm);
     String cosNmNoSubReq = null;

     /* alternative name without subrequest */
     if(StringUtils.hasValue(subReqNodeValue)) 
     {
         int subReqNodeIndex = cosNm.lastIndexOf(subReqNodeValue);
         cosNmNoSubReq = cosNm.substring(0,subReqNodeIndex-1) + cosNm.substring(subReqNodeIndex + subReqNodeValue.length());
     }

     if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log(Debug.MSG_STATUS,"DirectorHelper: Invoking findServer: cosStringName " + realCosNm);

     /* first try to find a server with either cosStringName or cosStringNameNoSubReq */
     RequestHandler obj = findHandle(realCosNm, cosNmNoSubReq, errBuf);

     /* if we still can't find a server look in alternative orb locations */
     /* if (obj == null)
     {
         
         if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"DirectorHelper: Server not found; looking in alternate orb spaces");

         String ORBRoutingRequestType = cfg.getORBRoutingRequestType();
         
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
                         Debug.log(Debug.MSG_STATUS, "DirectorHelper: " +
                              "Request-Type [" + requestType + "] is valid for Conditional ORB Routing");
          
                     Iterator iter = cfg.getAltORBIter();
                     AlternateORB space;

                     while(obj==null && iter.hasNext()) 
                     {
                         space = (AlternateORB)iter.next();
                         
                         boolean result = matchORB(space,message);
                         
                         if(result)
                         {
                             if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                                Debug.log(Debug.MSG_STATUS,"DirectorHelper: " +
                                        "Invoking findServer with " +
                                        "details ["+realCosNm+":"+cosNmNoSubReq+":"
                                        +space.getAddr()+space.getPort());
                             
                             obj = findHandle(realCosNm, cosNmNoSubReq,space.getAddr(),space.getPort(), errBuf);
                         }
                      }

                     break;
                 }
             }
         }
      } */

     return obj;
   }
    
    public RequestHandler findHandleInAltORB(MessageDirectorConfig cfg, String cosNm,
                        String subReqNodeValue, StringBuilder errBuf) throws ProcessingException
    {
        
         if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"DirectorHelper: Looking in alternate orb spaces");

         String realCosNm = cfg.getRealName(cosNm);
         String cosNmNoSubReq = null;

         /* alternative name without subrequest */
         if(StringUtils.hasValue(subReqNodeValue)) 
         {
             int subReqNodeIndex = cosNm.lastIndexOf(subReqNodeValue);
             cosNmNoSubReq = cosNm.substring(0,subReqNodeIndex-1) + cosNm.substring(subReqNodeIndex + subReqNodeValue.length());
         }
         
        Iterator iter = cfg.getAltORBIter();
        AlternateORB space;

        RequestHandler obj = null;
        while(obj==null && iter.hasNext()) 
        {
            space = (AlternateORB)iter.next();
                         
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS,"DirectorHelper: " +
                             "Invoking findServer with " +
                             "details ["+realCosNm+":"+cosNmNoSubReq+":"
                              +space.getAddr()+space.getPort());
                             
             obj = findHandle(realCosNm, cosNmNoSubReq,space.getAddr(),space.getPort(), errBuf);
       }
        
        return obj;
     }

    /**
     * 
     * @param name
     * @param alt
     * @param errBuf
     * @return
     * @throws ProcessingException
     */
    public RequestHandler findHandle(String name, String alt, StringBuilder errBuf)  throws ProcessingException
    {
        return findHandle(name,alt,null, null,errBuf);        
    }

    /**
     * 
     * @param name
     * @param alternateNm
     * @param addr
     * @param port
     * @param errBuf
     * @return
     * @throws ProcessingException
     */
    public RequestHandler findHandle(String name, String alternateNm, String addr, String port, StringBuilder errBuf) 
                                throws ProcessingException
    {
        SPILocator spiLocator = null;
        org.omg.CORBA.Object object = null;
                 
            
            if(addr==null)
            {
                try
                {
                    spiLocator = new SPILocator();
                }
                catch(FrameworkException fe)
                {
                    Debug.error("Unable to make an instance of SPILocator object "+fe.getMessage());
                    throw new ProcessingException(fe);
                }
    
                object = spiLocator.find(name);
            }
            else
            {
                try
                {
                    spiLocator = new SPILocator(addr,port);
                }
                catch(FrameworkException fe)
                {
                    Debug.error("Unable to make an instance of SPILocator object "+fe.getMessage());
                    throw new ProcessingException(fe);
                }
    
                object = spiLocator.find(name, addr, port);
            }
            
            if(object==null)
            {
                if(addr==null)
                    errBuf.append("Server ["+name+"] at default ORB address and port not found \n");
                else
                    errBuf.append("Server ["+name+"] at address["+addr+"] and port["+port+"] not found \n");
                
                if(addr==null)
                    object = spiLocator.find(alternateNm);
                else
                    object = spiLocator.find(alternateNm, addr, port);
            }
            
            if(object!=null)
            {
                try
                {
                    return RequestHandlerHelper.narrow(object);
                }
                catch(Exception exp)
                {
                    Debug.warning("Narrow failed for object ["+name+"], removing it from cache.");
                    spiLocator.removeFromCache(name);
                    
                    if(addr==null)
                        errBuf.append("Server ["+name+"] at default ORB address and port not found \n");
                    else
                        errBuf.append("Server ["+name+"] at address["+addr+"] and port["+port+"] not found \n");
                }
            }
            else
            {
                if(addr==null)
                    errBuf.append("Server ["+alternateNm+"] at default ORB address and port not found \n");
                else
                    errBuf.append("Server ["+alternateNm+"] at address["+addr+"] and port["+port+"] not found \n");
            }
            
            return null;
     }

    protected RequestHandler findHandle(MessageDirectorConfig cfg,String cosName, String subReqNodeValue, String routingCondition, StringBuilder errBuf) throws ProcessingException
    {
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "DirectorHelper: findHandle - " +
                    "Looking up ORB for locating server [" + cosName 
                    + "] for condition [" + routingCondition + "]");

        String cosStringName = getRealName(cfg,cosName);
        String cosStringNameNoSubReq = null;

        // alternative name without subrequest
        if (StringUtils.hasValue(subReqNodeValue) ) {
            int subReqNodeIndex = cosName.lastIndexOf(subReqNodeValue);
            cosStringNameNoSubReq = cosName.substring (0, subReqNodeIndex - 1) + cosName.substring (subReqNodeIndex + subReqNodeValue.length());
        }

        Iterator altORBIter = cfg.getAltORBIter();
        AlternateORB orb = null;
        while(altORBIter.hasNext())
        {
            orb = (AlternateORB)altORBIter.next();
            
            if(routingCondition.equals(orb.getCondition()))
                break;        
        }
        
        
        /* first try to find a server with either cosStringName 
         * or cosStringNameNoSubReq */
        RequestHandler obj = findHandle(cosStringName, cosStringNameNoSubReq, orb.getAddr(), orb.getPort(), errBuf);

        if (!(obj == null))
        {
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "DirectorHelper: findHandle - " +
                        "Found server [" + cosName + "] using alternate ORB Addr:Port [" 
                        + orb.getAddr() + ":" + orb.getPort()+ "]");
        }
        return obj;
  }

    public String getRealName(MessageDirectorConfig directorConfig,String alias)
    {
        
       if (!StringUtils.hasValue(alias))
          return alias;

       String realNm = directorConfig.getRealName(alias);       
       if (StringUtils.hasValue(realNm))
          return realNm;
       else
          return alias;

    }

    /**
     * If map containing X-Path condition to ORB mapping is not empty,
     * parse the input message body and test each X-Path iteratively
     * to find the first match.
     *
     * @param cfg
     * @param message The input message
     * @return the X-Path condition that evalautes to true for the given message
     */
    public String getORBRoutingKey(MessageDirectorConfig cfg,Object message)
    {
        String routingCondition = null;
        boolean matchFound = false;

        try 
        {

            /* create Document object from the message string */
            Document messageBody = XMLLibraryPortabilityLayer.convertStringToDom(message.toString());
            Iterator iter = cfg.getAltORBIter();

            while (iter.hasNext()) 
            {
                AlternateORB orb = (AlternateORB)iter.next();
                
                routingCondition = orb.getCondition();
                ParsedXPath  xPath = new ParsedXPath (routingCondition);
               
                if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, "getORBRoutingKey : " +
                            "Checking against xpath [" + routingCondition + "]");

                /* Check if the given Xpath-Condition holds true for the message body
                 if so, set matchFound to true and break */
                if (xPath.getBooleanValue(messageBody))
                {
                    matchFound = true;
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "getORBRoutingKey : " +
                                "Found an xpath match with [" + routingCondition + "]");

                    break;
                }
                else
                {
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "getORBRoutingKey : " +
                                "Could not find an xpath match with [" + routingCondition + "]");
                }
             } 
            }
            catch (FrameworkException e)
            {
                Debug.log(Debug.MSG_ERROR, "getORBRoutingKey: " +
                        "Failed to evaluate xpath boolean expression. [" 
                        + routingCondition + "]\n " + e.getMessage() );
            }

            /* if any match was found, return that otherwise return null */
            if (matchFound)
                return routingCondition;
            else
                return null;
    }

    /**
     * If map containing X-Path condition to ORB mapping is not empty,
     * parse the input message body and test each X-Path iteratively
     * to find the first match.
     *
     * @param cfg
     * @param message The input message
     * @return the X-Path condition that evalautes to true for the given message
     */
    public boolean matchORB(AlternateORB orb,Object message)
    {
        String routingCondition = null;
        try 
        {
            /* create Document object from the message string */
            Document messageBody = XMLLibraryPortabilityLayer.convertStringToDom(message.toString());
            routingCondition = orb.getCondition();
            ParsedXPath  xPath = new ParsedXPath (routingCondition);
               
            if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, "matchORB : " +
                        "Checking against xpath [" + routingCondition + "]");

             /* Check if the given Xpath-Condition holds true for the message body */
             if (xPath.getBooleanValue(messageBody))
             {
                    if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                        Debug.log(Debug.MSG_STATUS, "matchORB : " +
                                "Found an xpath match with [" + routingCondition + "]");

                    return true;
              }
            }
            catch (FrameworkException e)
            {
                Debug.log(Debug.MSG_ERROR, "matchORB: " +
                        "Failed to evaluate xpath boolean expression. [" 
                        + routingCondition + "]\n " + e.getMessage() );
            }

            return false;
    }

}

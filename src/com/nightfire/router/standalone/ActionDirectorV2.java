package com.nightfire.router.standalone;

import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.idl.RequestHandler;
import com.nightfire.idl.RequestHandlerHelper;
import com.nightfire.router.cfg.RouterConfig.MessageDirectorConfig;
import com.nightfire.router.cmn.MessageDirectorV2;
import com.nightfire.router.messagedirector.ActionDirector;
import com.nightfire.spi.common.HeaderNodeNames;

/**
 * New ActionDirector class without any interaction with CORBA layer. 
 * @author hpirosha
 */
public class ActionDirectorV2 extends ActionDirector implements MessageDirectorV2 {

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
    }
        
    /**
     * @param header Document
     * @param message Object
     * @throws MessageException 
     */
    public boolean canRoute(Document header, Object message) 
                        throws MessageException
    {
        /* call base class method */
        return super.canRoute(header, message);
    }
    
    @Override
    /**
     * @param header Document
     * @param message Object
     * @throws ProcessingException, MessageException 
     */
    protected RequestHandler getHandler(Document header, Object message) 
                    throws ProcessingException, MessageException
    {
        XMLMessageParser parser = new XMLMessageParser(header);
        String actionNm = parser.getValue(HeaderNodeNames.ACTION_NODE);

        /* create the input name using action and request node */
        String inputNm = actionNm + PERIOD + parser.getValue(HeaderNodeNames.REQUEST_NODE);

        /* Try to locate the real name i.e. alias for this input name. */
        String cosNm = cfg.getRealName(inputNm);
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "ActionDirectorV2.getHandler: " +
                    "Looked up alias for key ["+inputNm+"]. The alias found is ["+cosNm+"]");

        /* If no alias is available, getRealName() would return the input string as is.
           In this case, try to get the alias using action_node only. */
        if(inputNm.equals(cosNm))
        {
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              Debug.log(Debug.MSG_STATUS, "ActionDirectorV2.getHandler: " +
                    "Could not find valid alias with key [" + inputNm + "]");

            cosNm = cfg.getRealName(actionNm);

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
               Debug.log(Debug.MSG_STATUS, "ActionDirectorV2.getHandler: " +
                    "Looked up alias for action [" + actionNm + "]. The alias found is [" + cosNm + "]");
        }

        SPILocator objLoc = null;
        try 
        {
            objLoc = new SPILocator();
        } 
        catch(FrameworkException e) 
        {
            Debug.error("Unable to instantiate SPILocator "+e.getMessage());
            throw new ProcessingException(e);
        } 

        RequestHandler gatewayHandle = null;
        
        org.omg.CORBA.Object obj = objLoc.find(cosNm);
        if(obj!=null)
        {
            try
            {
                gatewayHandle = RequestHandlerHelper.narrow(obj);
            }
            catch(Exception exp)
            {
                Debug.warning("Narrow failed for object["+cosNm+"], removing it from cache.");
                objLoc.removeFromCache(cosNm);
            }
        }

        if(gatewayHandle!=null)
           return gatewayHandle;

        throw new ProcessingException("ActionDirectorV2: Could not locate"
                                            + " server, the following servers were tried :\n" +cosNm);
    }
}

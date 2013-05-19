package com.nightfire.webgui.gateway.servicemgr;

import java.io.PrintWriter;
import java.util.HashMap;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.comms.servicemgr.ServerManagerFactory;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.webgui.gateway.servicemgr.CommandProcessor.NEXTVIEW;
import com.nightfire.webgui.gateway.servicemgr.CommandProcessor.PARAMS;

public class ViewDispatcher 
{
    private String nextView;
    
    private static String realPath;

    public static String getRealPath() {
        return realPath;
    }

    public static void setRealPath(String realPath) {
        ViewDispatcher.realPath = realPath;
    }

    public ViewDispatcher(String nextView)
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside ViewDispatcher constructor ... nextView ["+nextView+"].");

        this.nextView = nextView;
    }
    
    /**
     * renders the view.
     * @param out stream on which output is generated.
     * @param params parameters.
     * @throws ProcessingException on error.
     */
    public void dispatch(PrintWriter out, HashMap params) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside Dispatch ...params "+params);

        String errorMessage = CommandProcessor.getParameter(params, PARAMS.errormessage.toString());
        String serviceType = CommandProcessor.getParameter(params, PARAMS.servicetype.toString());
        
        if(StringUtils.hasValue(errorMessage))
        {
            updateMessageInDOM(errorMessage,serviceType);
        }
      
        GatewayViewHelper helper = new GatewayViewHelper();
        if(helper.exists())
            helper.generateView(out);
        else
            display(out,serviceType);
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Exiting Dispatch...");

    }
    
    /**
     * renders the view by transforming configuration with XSL.
     * @param out stream to generate output.
     * @param serviceType service type.
     * @throws ProcessingException on error
     */
    private void display(PrintWriter out, String serviceType) throws ProcessingException
    {
        XMLMessageParser parser = getXMLMessageParser(serviceType);
        String xsl = getRealPath()+getXSLStyleSheet();
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside ViewDispatch display... id["+serviceType+"] and xsl["+xsl+"].");

        Source source = new DOMSource(parser.getDocument());
        StreamResult result = new StreamResult(out);

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try 
        {
            transformer = tFactory.newTransformer(new StreamSource(xsl));
            transformer.transform(source,result );
        } 
        catch (Exception e) 
        {
            Debug.error("Exception while displaying details: "+e.getMessage());
            throw new ProcessingException (e);
        }
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Exiting ViewDispatch Display...");

        
    }

    /**
     * updates the error message in the XML dom. 
     * @param errorMessage error message that needs to be updated.
     * @param serviceType service type information.
     * @throws ProcessingException on error.
     */
    private void updateMessageInDOM(String errorMessage,String serviceType) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside updatemessage in dom ... serviceType["+serviceType+"] and errorMessage["+errorMessage+"].");

        XMLMessageParser parser = getXMLMessageParser(serviceType);
        Document document = parser.getDocument();
        
        NodeList errorList = document.getElementsByTagName("error");
        
        Element errorElem = null;
        
        if(errorList!=null && errorList.getLength()==0){
            errorElem = document.createElement("error");
        }else{
            errorElem=(Element) errorList.item(0);
        }
        
        try{errorElem.removeChild(errorElem.getFirstChild());}catch (Exception e){}

        errorElem.appendChild(document.createTextNode(errorMessage));
        document.getDocumentElement().appendChild(errorElem);
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Exiting updatemessage in dom.");


    }

    /**
     * Gets the XMLMessageParser for given service type
     * @param serviceType service type.
     * @return XMLMessageParser.
     * @throws ProcessingException
     */
    private XMLMessageParser getXMLMessageParser(String serviceType) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "getXMLMessageParser... serviceType["+serviceType+"] and nextView ["+nextView+"].");

        //if(nextView.equalsIgnoreCase(NEXTVIEW.managerview.toString()))
          //  return ServerManagerFactory.getInstance().getParser();
        
        if(nextView.equalsIgnoreCase(NEXTVIEW.serviceview.toString()))
            return ServerManagerFactory.getInstance().getServerManager(serviceType).getParser();
        else
            throw new ProcessingException("Unknown View type "+nextView+ " could not return XML configuration.");
    }
    
    /**
     * @return the XSL path as string based on view information.
     * @throws ProcessingException
     */
    private String getXSLStyleSheet() throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "getXSLStyleSheet... nextView ["+nextView+"].");

        if(nextView.equalsIgnoreCase(NEXTVIEW.managerview.toString()))
            return "WEB-INF\\DEFAULT\\resources\\xsl\\manager.xsl";
        else if(nextView.equalsIgnoreCase(NEXTVIEW.serviceview.toString()))
            return "WEB-INF\\DEFAULT\\resources\\xsl\\services.xsl";
        else
            throw new ProcessingException("Unknown View type "+nextView+ " no XSL configured for this view type.");
    }
    
}

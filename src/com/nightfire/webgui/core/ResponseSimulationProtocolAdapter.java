/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  org.w3c.dom.*;

import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.http.HttpSession;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.constants.PlatformConstants;

import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.framework.debug.*;


/**
 * <p><strong>ResponseSimulationProtocolAdapter</strong> implements the core's
 * ProtocolAdapter interface.  It determines the appropriate simulated response
 * to be returned to the caller, based on the request data and the repository's 
 * XML descriptor.</p>
 */

public class ResponseSimulationProtocolAdapter implements ProtocolAdapter
{
    public static final String TEST_RESPONSE_CONFIG = "TEST_RESPONSE_CONFIG";
    
    public static final String RESPONSE_FILE_NODE       = "ResponseFile";

    private Map lookup;
    private Map actionToRespCode;

    private DebugLogger log;
  

    public void init(Properties properties, ServletContext context) throws ServletException
    {
       String webAppName = ServletUtils.getWebAppContextPath(context); 
       log = DebugLogger.getLogger(webAppName, PageFlowCoordinator.class);

        log.debug("ResponseSimulationProtocolAdapter(): Sending a request to the repository for test-response information ...");
        
        String resourcePath = (String)properties.getProperty(TEST_RESPONSE_CONFIG);

        if (log.isDebugEnabled())
        {
            log.debug("ResponseSimulationProtocolAdapter(): Test-response path is [" + resourcePath + "].");
        }
        
        if (!StringUtils.hasValue(resourcePath))
        {
            String errorMessage = "ERROR: ResponseSimulationProtocolAdapter.ResponseSimulationProtocolAdapter(): The required servlet initialization parameter [" + TEST_RESPONSE_CONFIG + "] must exist and have a valid value.";

            log.error(errorMessage);
            
            throw new ServletException(errorMessage);
        }

        Document testResponseXML = (Document)ServletUtils.getLocalResource(context, resourcePath, ResourceDataCache.XML_DOC_DATA);

        cacheData(testResponseXML);
    }
    
    /**
     * Parses the XML test-response information and then caches it in an internal
     * lookup table.
     *
     * @param  testResponseXML  A valid XML test-response document.
     *
     * @exception  ServletException  Thrown on XML parsing errors.
     */
    private void cacheData(Document testResponseXML) throws ServletException
    {
        /**
         * testResponseXML will be in the following format:
         * where ResponseCode is the response code to return and
         * response file is the response file to return.
         *
         * <TestResponse>
         *   <my-action>
         *     <ResponseContainer>
         *       <Response>
         *         <ResponseCode value="success"/>
         *         <ResponseFile value="d:\responses\my-action-success.xml"/>
         *       </Response>
         *     </ResponseContainer>
         *   </my-action>
         * </TestResponse>
         */

        lookup = new HashMap();
        actionToRespCode = new HashMap();

        try
        {
            XMLPlainGenerator parser      = new XMLPlainGenerator(testResponseXML);

            Node[]            actionNodes = parser.getChildren(parser.getDocument().getDocumentElement());

            for (int i = 0; i < actionNodes.length; i++)
            {
                String actionName = actionNodes[i].getNodeName();
                String responseContainerNode = parser.getXMLPath(actionNodes[i]) + "." + PlatformConstants.RESPONSE_CONTAINER_NODE;
                Node[] responseNodes        = parser.getChildren(responseContainerNode);

                for (int j = 0; j < responseNodes.length; j++)
                {

                    String responseCode = parser.getValue(responseNodes[j], PlatformConstants.RESPONSE_CODE_NODE);

                    String responseFile = parser.getValue(responseNodes[j], RESPONSE_FILE_NODE);

                    if (log.isDebugEnabled())
                    {
                        log.debug("cacheData(): Caching test-response key and value [" + actionName + "." + responseCode + "], test-response XML from file [" + responseFile + "] ...");
                    }

                    lookup.put(actionName + "." + responseCode,  responseFile);
                    actionToRespCode.put(actionName, responseCode);
                }
            }
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ResponseSimulationProtocolAdapter.cacheData(): Failed to parse test-response XML data:\n" + testResponseXML + "\n" + e.getMessage();

            log.error(errorMessage);
            log.error("",e);
            throw new ServletException(errorMessage);

        }
    }
    
    /**
     * This method determines the appropriate test response based on the request
     * data and the XML descriptor, and returns it to the caller.
     *
     * @param  request  Request DataHolder object.
     * @param  session  HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  DataHolder Response object returned from the processing layer.
     */
    public DataHolder sendRequest(DataHolder request, HttpSession session) throws ServletException
    {
        if (log.isDebugEnabled())
        {
            log.debug("sendRequest(): Raw request header:\n" + request.getHeaderStr());
            
            log.debug("sendRequest(): Raw request body:\n" + request.getBodyStr());    
        }
        
        try
        {
            String            headerString = request.getHeaderStr();
            
            XMLPlainGenerator headerParser = new XMLPlainGenerator(headerString);
         
            // Two request-header field are needed in order to determine the test
            // response to be returned, action code (NFH_Action) and response code
            // (NFH_ResponseCode).  While the first is required, the latter however
            // is optional.  If no response code is supplied, a "success" will be used.
             
            String action             = headerParser.getValue(PlatformConstants.ACTION_NODE);


            String responseCode = null;

            if (headerParser.exists(PlatformConstants.RESPONSE_CODE_NODE) )
               responseCode       = headerParser.getValue(PlatformConstants.RESPONSE_CODE_NODE);
            else
               responseCode = (String)actionToRespCode.get(action);


            if (!StringUtils.hasValue(responseCode))
            {
                responseCode = ServletConstants.SUCCESS;
            }

            headerParser.setValue(PlatformConstants.RESPONSE_CODE_NODE, responseCode);
            
            String testResponse = (String)lookup.get(action + "." + responseCode);
            
            if (!StringUtils.hasValue(testResponse))
            {
                throw new Exception("ERROR: ResponseSimulationProtocolAdapter.cacheData(): No test response entry exists in the cache for action [" + action + "] and response code [" + responseCode + "].");
            }

            XMLPlainGenerator parser = new XMLPlainGenerator((Document)ServletUtils.getLocalResource(session, testResponse, ResourceDataCache.XML_DOC_DATA));

            return new DataHolder(headerParser.getOutput(), parser.getOutput());
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: ResponseSimulationProtocolAdapter.cacheData(): Failed to obtain a test response:\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }
}

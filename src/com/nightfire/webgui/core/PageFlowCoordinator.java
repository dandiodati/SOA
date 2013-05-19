/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  java.util.*;
import java.net.*;

import  javax.servlet.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.*;
import  com.nightfire.framework.message.parser.*;
import  com.nightfire.framework.message.parser.xml.*;
import  com.nightfire.framework.message.generator.*;
import  com.nightfire.framework.message.generator.xml.*;
import  com.nightfire.framework.constants.PlatformConstants;

import  com.nightfire.webgui.core.beans.NFBean;
import  com.nightfire.webgui.core.resource.ResourceDataCache;
import  com.nightfire.framework.message.util.xml.*;
import  com.nightfire.framework.debug.*;



import org.w3c.dom.*;

/**
 * <p><strong>PageFlowCoordinator</strong> represents one of ControllerServlet's
 * workers.  Its responsibility is to use its cached page-flow information and
 * the run-time response obtained from the processing layer, to determine the
 * "next" page to be displayed.</p>
 */

public class PageFlowCoordinator
{
    /**
     * properties indicating the location of page flow
     */
    public static final String PAGE_FLOW = "PAGE_FLOW";

    /**
     * xml node indicating the next page to result page to redirect to.
     */
    public static final String NEXT_PAGE_NODE = "NextPage";

    private Map pageFlow;
    private DebugLogger log;
  
    
    /**
     * Constructor.
     *
     * @param  properties  Servlet initialization properties.
     * @param context The servlet context getting loaded.
     * @exception  ServletException  Thrown when an error occurs during initialization.
     */
    public PageFlowCoordinator(Properties properties, ServletContext context ) throws ServletException
    {
      String webAppName = ServletUtils.getWebAppContextPath(context); 
      log = DebugLogger.getLogger(webAppName, PageFlowCoordinator.class);
      
        log.info("PageFlowCoordinator(): Sending a request to the repository for page-flow information ...");

        String resourcePath = (String)properties.getProperty(PAGE_FLOW);

        if (log.isDebugEnabled())
        {
            log.debug("PageFlowCoordinator(): Page-flow repository path is [" + resourcePath + "].");
        }
        
        if (!StringUtils.hasValue(resourcePath))
        {
            String errorMessage = "ERROR: PageFlowCoordinator.PageFlowCoordinator(): The required servlet initialization parameter [" + PAGE_FLOW + "] must exist and have a valid value.";
            
            log.error(errorMessage);
            
            throw new ServletException(errorMessage);
        }

           Document pageFlowXML = (Document)ServletUtils.getLocalResource(context,resourcePath, ResourceDataCache.XML_DOC_DATA);
           cachePageFlow(pageFlowXML);
    }
    
    /**
    * Parses the XML page-flow information and then caches it in an internal
    * lookup table.
    *
    * @param  xmlPageFlow  A valid XML page-flow document.
    *
    * @exception  ServletException  Thrown on XML parsing errors.
    */
    private void cachePageFlow(Document xmlPageFlow) throws ServletException
    {
        /**
        * xmlPageFlow will be in the following format:
        *
        * <PageFlow>
        *   <ActionContainer type="container">
        *     <Action>
        *       <Name value="new-loop"/>
        *       <ResponseContainer type="container">
        *         <Response>
        *           <ResponseCodePrefix="x"/> (option node)
        *           <ResponseCode value="success"/>
        *           <NextPage value="pacbell/loop/order-confirmation.jsp"/>
        *         </Response>
        *       </ResponseContainer>
        *     </Action>
        *   </ActionContainer>
        * </PageFlow>
        */

        pageFlow = new HashMap();
        
        try
        {
            XMLRefLinker linker = new XMLRefLinker(xmlPageFlow);
            linker.setIdAttr("id");
    
            xmlPageFlow = linker.resolveRefs();

  
            XMLMessageParser parser           = new XMLMessageParser(xmlPageFlow);
            
            if ( log.isDebugEnabled() )
               log.debug(" Parsing page flow xml:\n" + parser.getGenerator().generate() );

            String           actionNodePrefix = PlatformConstants.ACTION_CONTAINER_NODE + "." + PlatformConstants.ACTION_NODE + "(";

            int              actionCount      = parser.getChildCount(PlatformConstants.ACTION_CONTAINER_NODE);

            for (int i = 0; i < actionCount; i++)
            {
                String actionNode            = actionNodePrefix + i + ").";
                                                       
                String actionName            = parser.getValue(actionNode + PlatformConstants.NAME_NODE);

                String responseContainerNode = actionNode + PlatformConstants.RESPONSE_CONTAINER_NODE;

                String responseNodePrefix    = responseContainerNode + "." + PlatformConstants.RESPONSE_NODE + "(";
                
                int    responseCount         = parser.getChildCount(responseContainerNode);

                for (int j = 0; j < responseCount; j++)
                {
                    String responseNode = responseNodePrefix + j + ").";

                    // if there is a response prefix then get it
                    String respPrefix = null;
                    if (parser.exists(responseNode + PlatformConstants.RESPONSE_CODE_PREFIX_NODE) )
                       respPrefix   = parser.getValue(responseNode + PlatformConstants.RESPONSE_CODE_PREFIX_NODE);

                    String responseCode = parser.getValue(responseNode + PlatformConstants.RESPONSE_CODE_NODE);

                    String nextPage     = parser.getValue(responseNode + NEXT_PAGE_NODE);

               
                     log.info("cachePageFlow(): Caching page-flow key [" + actionName + "." + responseCode + "], page-flow value [" + nextPage + "] ...");
               

                    // if there is a respPrefix prepend it to the response code
                    if (StringUtils.hasValue(respPrefix) )
                       responseCode = respPrefix + "-" + responseCode;


                    pageFlow.put(actionName + "." + responseCode, nextPage);
                }
            }
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: PageFlowCoordinator.cachePageFlow(): Failed to parse page-flow XML data:\n" + xmlPageFlow + "\n" + e.getMessage();

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }
    }
    
    /**
     * Obtains the next page in the flow, base on the action name and the response
     * code returned from the processing layer.
     *
     * @param  respCodePrefix A prefix to prepend to the response code, can be null.
     *         Can be used to route to different pages for the same action.
     * @param  action        Action name.
     * @param  responseCode  Response code.
     *
     * @return  The path name of the next page in the flow.
     */
    public String getNextPage(String respCodePrefix, String action, String responseCode)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getNextPage(): Retrieving page information using action [" + action + "], response code [" + responseCode +"], and response code prefix [" + respCodePrefix + "].");
        }

        // if there is a response code prefix then prepend it to the response code.
        if ( StringUtils.hasValue(respCodePrefix) )
           responseCode = respCodePrefix + "-" + responseCode;

        String key  = action + "." + responseCode;

        String page = (String)pageFlow.get(key);

        if (!StringUtils.hasValue(page))
        {
            log.error("PageFlowCoordinator.getNextPage(): No corresponding page found for action [" + action + "], response code [" + responseCode +"], and response code prefix [" + respCodePrefix + "].");

            return null;
        }

     
        log.info("getNextPage(): Page-flow cache returns page [" + page + "] for action [" + action + "], response code [" + responseCode +"], and response code prefix [" + respCodePrefix + "].");
     
        
        return page;
    }
}

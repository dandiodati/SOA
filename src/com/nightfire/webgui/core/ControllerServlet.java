/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;


import  java.io.*;
import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.http.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.*;
import  com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.constants.PlatformConstants;

import  com.nightfire.webgui.core.beans.*;
import  com.nightfire.framework.debug.*;



/**
 * <p><strong>ControllerServlet</strong> represents the "controller" in
 * the Model-View-Controller (MVC) design pattern for web applications.
 * As the brain of the run-time UI framework, it delegates responsibility
 * to appropriate components and coordinates the processing of both request
 * and response.  It is also responsible for internationalization and error
 * handling.</p>
 */

public class ControllerServlet extends HttpServlet
{
    public  static final String SERVLET_PARAMS             = "CONTROLLER_SERVLET_PARAMS";

    private static final String PROTOCOL_ADAPTER_INTERFACE = "com.nightfire.webgui.core.ProtocolAdapter";
    private static final String INPUT_VALIDATOR_INTERFACE  = "com.nightfire.webgui.core.InputValidator";
    private static final String DATA_ADAPTER_INTERFACE     = "com.nightfire.webgui.core.DataAdapter";

    /**
     * Servlet initialization parameter constants.
     */
    public static final String PROTOCOL_ADAPTER_CLASS = "PROTOCOL_ADAPTER_CLASS";
    public static final String INPUT_VALIDATOR_CLASS  = "INPUT_VALIDATOR_CLASS";
    public static final String DATA_ADAPTER_CLASS     = "DATA_ADAPTER_CLASS";


    public static final String RESET_MSG_CACHE    ="RESET_WEBAPP_DATA_CACHE";

    private PageFlowCoordinator pageFlowCoordinator;
    private ProtocolAdapter     protocolAdapter;
    private InputValidator      inputValidator;

    private DataAdapter         dataAdapter;

    private Properties          servletParams;

    private String enc  = null;

    private DebugLogger log;

    /**
     * Redefinition of init() in HttpServlet.  This allows for all the resources
     * to be initialized before the servlet starts accepting requests.
     *
     * @exception  ServletException  Thrown when any error occurs during initialization.
     */
    public void init() throws ServletException
    {

      String webAppName = ServletUtils.getWebAppContextPath(getServletContext());
      log = DebugLogger.getLogger(webAppName, getClass());

        initParameters();


        // get encoding from application wide properties
        // if not set a null will be returned and the system default will be used
        Properties contextParams = (Properties) getServletContext().getAttribute(ServletConstants.CONTEXT_PARAMS);
        enc = contextParams.getProperty(ServletConstants.CHAR_ENCODING);


        // The following initializations can occur in any order.

        initAliasDescriptor();

        initProtocolAdapter();

        initDataAdapter();

        initInputValidator();

        initPageFlowCoordinator();
    }

    /**
     * This method loads in servlet's initialization parameters.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void initParameters() throws ServletException
    {

        // Create a Properties object reflecting servlet's parameters.  This will be
        // set in the servlet context where other modules can access the information.

        log.info("Creating a Properties object containing servlet's initialization parameters ...");

        ServletContext servletContext = getServletContext();

        servletParams = new Properties();

        //add all common context properties to the servlet params first
        Properties common = (Properties)servletContext.getAttribute(ServletConstants.CONTEXT_PARAMS);
        if ( common != null)
           servletParams.putAll(common);


        Enumeration e = getInitParameterNames();

        while (e.hasMoreElements())
        {
            String parameterName  = (String)e.nextElement();

            String parameterValue = getInitParameter(parameterName);

            if (parameterValue != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("initParameters(): Adding [" + parameterName + ", " + parameterValue + "] to the Properties object ...");
                }

                servletParams.put(parameterName, parameterValue);
            }
        }

        getServletContext().setAttribute(SERVLET_PARAMS, servletParams);

    }



    /**
     * This method creates an AliasDescriptor singleton object, which will be used
     * to look up the information used in string translation, mostly for responses.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void initAliasDescriptor() throws ServletException
    {
        log.info("initAliasDescriptor(): Creating an AliasDescriptor object ...");

        AliasDescriptor aliasDescriptor = new AliasDescriptor(servletParams, getServletContext() );
        getServletContext().setAttribute(ServletConstants.ALIAS_DESCRIPTOR, aliasDescriptor);
    }

    /**
     * This method creates a ProtocolAdapter singleton object, which will be used
     * to send requests to the back end.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void initProtocolAdapter() throws ServletException
    {
        log.info("initProtocolAdapter(): Creating a ProtocolAdapter object ...");

        String protocolAdapterClass = servletParams.getProperty(PROTOCOL_ADAPTER_CLASS);

        if (!StringUtils.hasValue(protocolAdapterClass))
        {
            String errorMessage = "ERROR: ControllerServlet.initProtocolAdapter(): Servlet initialization parameter [" + PROTOCOL_ADAPTER_CLASS + "] must exist and have a valid value.";

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }


        protocolAdapter          = (ProtocolAdapter)ServletUtils.createObject(protocolAdapterClass, PROTOCOL_ADAPTER_INTERFACE);
        protocolAdapter.init(servletParams, getServletContext());

        getServletContext().setAttribute(ServletConstants.PROTOCOL_ADAPTER, protocolAdapter);
    }

    /**
     * This method creates a DataAdapter singleton object, which will be used in
     * the formatting of the request and response data, to and from the back end.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void initDataAdapter() throws ServletException
    {
        log.info("initDataAdapter(): Creating a DataAdapter object ...");

        String dataAdapterClass = servletParams.getProperty(DATA_ADAPTER_CLASS);

        if (!StringUtils.hasValue(dataAdapterClass))
        {
            String errorMessage = "ERROR: ControllerServlet.initDataAdapter(): Servlet initialization parameter [" + DATA_ADAPTER_CLASS + "] must exist and have a valid value.";

            log.error(errorMessage);

            throw new ServletException(errorMessage);
        }


        dataAdapter              = (DataAdapter)ServletUtils.createObject(dataAdapterClass, DATA_ADAPTER_INTERFACE);
        dataAdapter.init(servletParams, getServletContext() );
        getServletContext().setAttribute(ServletConstants.DATA_ADAPTER, dataAdapter);
    }

    /**
     * This method creates an InputValidator singleton object, which will be used to
     * validate request information.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void initInputValidator() throws ServletException
    {

        String inputValidatorClass = servletParams.getProperty(INPUT_VALIDATOR_CLASS);

        if (StringUtils.hasValue(inputValidatorClass))
        {

            inputValidator           = (InputValidator)ServletUtils.createObject(inputValidatorClass, INPUT_VALIDATOR_INTERFACE);
            inputValidator.init(servletParams, getServletContext() );
            getServletContext().setAttribute(ServletConstants.INPUT_VALIDATOR, inputValidator);
        }

    }

    /**
     * This method creates a PageFlowCoordinator singleton object, which will be used
     * for looking up page-flow information.
     *
     * @exception  ServletException  Thrown when the PageFlowCoordinator object cannot
     *                               be created.
     */
    private void initPageFlowCoordinator() throws ServletException
    {
        log.info("initPageFlowCoordinator(): Creating a PageFlowCoordinator object ...");

        pageFlowCoordinator = new PageFlowCoordinator(servletParams, getServletContext());

        getServletContext().setAttribute(ServletConstants.PAGE_FLOW_COORDINATOR, pageFlowCoordinator);
    }


    /**
     * Implementation of doGet(HttpServletRequest, HttpServletResponse) in HttpServlet.
     *
     * @param  req   HttpServletRequest object.
     * @param  resp  HttpServletResponse object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     * @exception  IOException       This exception should never get thrown.  All
     *                               errors are wrapped in ServletException.  This
     *                               exists to satisfy the method signature.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String webAppName = ServletUtils.getWebAppContextPath(getServletContext());
        log = DebugLogger.getLogger(webAppName, getClass());

        log.info("Entering ControllerServlet.doGet() ...");

        process(req, resp);
    }

    /**
     * Implementation of doPost(HttpServletRequest, HttpServletResponse) in HttpServlet.
     *
     * @param  req   HttpServletRequest object.
     * @param  resp  HttpServletResponse object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     * @exception  IOException       This exception should never get thrown.  All
     *                               errors are wrapped in ServletException.  This
     *                               exists to satisfy the method signature.
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String webAppName = ServletUtils.getWebAppContextPath(getServletContext());
        log = DebugLogger.getLogger(webAppName, getClass());

        log.info("Entering ControllerServlet.doPost() ...");

        process(req, resp);
    }

    /**
     * This is where every servlet request gets processed.
     *
     * @param  req   HttpServletRequest object.
     * @param  resp  HttpServletResponse object.
     *
     * @throws ServletException  Thrown when an error occurs during processing.
     */
    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException
    {
        try
        {
            if (StringUtils.hasValue(enc) )
               req.setCharacterEncoding(enc);

            boolean isMultipart = ServletParamUtils.isMultipartReq(req, getServletContext());

            Map<String, String> formFields = new HashMap<String, String>();
            List streams;

            if (isMultipart)
            {
                try
                {
                    HashMap<String, Object> hm = ServletParamUtils.getMultipartFields(req, getServletContext());
                    formFields = (Hashtable) hm.get(ServletParamUtils.FORM_FIELDS);
                    streams = (ArrayList) hm.get(ServletParamUtils.FORM_STREAMS);
                    req.setAttribute(ServletParamUtils.EXCEL_DATA, streams);
                }
                catch (Exception e)
                {
                    throw new ServletException ("Unable to process Excel file. Please ensure the validity/format of file.");
                }
            }

            ///////////////////////////////////////////////////////////////////////////////
            // Output all incoming request parameters.  This only occurs if debugging is
            // turned on.
            ///////////////////////////////////////////////////////////////////////////////

            outputRequestParams(req);

            ///////////////////////////////////////////////////////////////////////////////
            // Tabulate the request data for easy handling.
            ///////////////////////////////////////////////////////////////////////////////

            
            Map          requestData   = req.getParameterMap();

            // in case of multi part request creating new hash map
            // to add other parameters manually
            if (isMultipart)
                requestData   = new HashMap(req.getParameterMap());

            HttpSession  session       = req.getSession();

            String       action;

            String       pageToDisplay;

            String       respCodePrefix;

            if (!isMultipart)
            {
                action        = req.getParameter(ServletConstants.NF_FIELD_HEADER_PREFIX + PlatformConstants.ACTION_NODE);

                pageToDisplay = req.getParameter(ServletConstants.NF_FIELD_HEADER_PAGE_TAG);

                respCodePrefix   = req.getParameter(ServletConstants.NF_FIELD_HEADER_RESPCODE_PREFIX);
            }
            else
            {
                action        = formFields.get(ServletConstants.NF_FIELD_HEADER_PREFIX + PlatformConstants.ACTION_NODE);

                pageToDisplay = formFields.get(ServletConstants.NF_FIELD_HEADER_PAGE_TAG);

                respCodePrefix   = formFields.get(ServletConstants.NF_FIELD_HEADER_RESPCODE_PREFIX);

                requestData.putAll(formFields);
            }

            SessionInfoBean sessionInfoBean = (SessionInfoBean)session.getAttribute(ServletConstants.SESSION_BEAN);
            if (sessionInfoBean != null)
            {
              sessionInfoBean.setRequestTime(System.currentTimeMillis());
            }



            log.info("process(): Action [" + action + "], NextPage [" + pageToDisplay + "]");

            ///////////////////////////////////////////////////////////////////////////////
            // At least "action" or "pageToDisplay" must be present in the request.
            ///////////////////////////////////////////////////////////////////////////////

            if (!StringUtils.hasValue(action) && !StringUtils.hasValue(pageToDisplay))
            {
                String errorMessage = "ERROR: ControllerServlet.process(): Either [" + ServletConstants.NF_FIELD_HEADER_PREFIX + PlatformConstants.ACTION_NODE +
                                      "] or [" + ServletConstants.NF_FIELD_HEADER_PAGE_TAG + "] tag must be present in the request.";

                log.error(errorMessage);

                throw new ServletException(errorMessage);
            }

            ///////////////////////////////////////////////////////////////////////////////
            // Appropriately add current request data to the session's request-data Bean.
            ///////////////////////////////////////////////////////////////////////////////


            NFBean requestDataBean = null;


            // if reset action, clear the current message cache.
            // now recorded sessions can be reused by resetting the message cache.
            if (RESET_MSG_CACHE.equals(action)) {
                resetDataCache(req);
                resp.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            else
                requestDataBean = bufferRequestData(req, requestData);



            String curMsgId = requestDataBean.getHeaderValue(ServletConstants.NF_FIELD_HEADER_MESSAGE_ID_TAG );

            if (log.isDebugEnabled())
            {
                log.debug("process: Current message ID [" + curMsgId + "]");
            }


            ///////////////////////////////////////////////////////////////////////////////
            // Valid combinations of "action" and "pageToDisplay" are:
            //
            // "action" and "pageToDisplay" - Buffer request information for later submit.
            // "pageToDisplay" only         - Just a straight link to another page.
            // "action" only                - Submit request information.
            ///////////////////////////////////////////////////////////////////////////////

            ///////////////////////////////////////////////////////////////////////////////
            // "pageToDisplay" only - Just a straight link to another page.
            // NOTE: That if there is a response bean associated with this message id
            // that it gets passed to the destination page.
            ///////////////////////////////////////////////////////////////////////////////

            if (!StringUtils.hasValue(action) && StringUtils.hasValue(pageToDisplay))
            {
                ServletUtils.dispatchRequest(req, resp, pageToDisplay);
                return;
            }

            ///////////////////////////////////////////////////////////////////////////////
            // Determine whether to buffer the request data for later submission or submit
            // it (and any buffered ones, if any) for processing.
            ///////////////////////////////////////////////////////////////////////////////

            if (StringUtils.hasValue(action))
            {
                if (StringUtils.hasValue(pageToDisplay))
                {
                    ///////////////////////////////////////////////////////////////////////
                    // "action" and "pageToDisplay" - Request info is already buffered,
                    //                                just take the user to the next page.
                    ///////////////////////////////////////////////////////////////////////

                    ServletUtils.dispatchRequest(req, resp, pageToDisplay);
                }
                else
                {
                    ///////////////////////////////////////////////////////////////////////
                    // "action" only - Submit request information, including any that has
                    //                 been buffered.
                    ///////////////////////////////////////////////////////////////////////

                    // A check is first needed to ensure that a duplicate submission does
                    // not occur.

                    String  resubmissionAttemptString = requestDataBean.getHeaderValue(ServletConstants.RESUBMISSION_ATTEMPT_HEADER_FIELD);

                    boolean resubmissionAttempt       = StringUtils.getBoolean(resubmissionAttemptString , false);

                    if (resubmissionAttempt)
                    {
                        String resubmissionAttemptRedirectPage = requestDataBean.getHeaderValue(ServletConstants.RESUBMISSION_ATTEMPT_REDIRECT_PAGE_HEADER_FIELD);

                        requestDataBean.removeHeaderField(ServletConstants.RESUBMISSION_ATTEMPT_HEADER_FIELD);

                        requestDataBean.removeHeaderField(ServletConstants.RESUBMISSION_ATTEMPT_REDIRECT_PAGE_HEADER_FIELD);

                        ServletUtils.dispatchRequest(req, resp, resubmissionAttemptRedirectPage);

                        return;
                    }

                    // make a copy of the data bean so that the original data stays intact
                    NFBean requestDataBeanCopy = null;
                    try {
                       requestDataBeanCopy = requestDataBean.getFinalCopy();
                    } catch (MessageException e) {
                      String err =  "Could not get final output of request bean: " + e.getMessage();
                      log.error(err );
                      throw new ServletException(err);
                    }

                    ///////////////////////////////////////////////////////////////////////
                    // Format the request data and send it to the processing layer.
                    ///////////////////////////////////////////////////////////////////////

                    DataHolder transformedRequestData = dataAdapter.transformRequest(requestDataBeanCopy, req);

                    if (log.isDebugDataEnabled())
	                {
                    	log.debugData("process(): The transformed request header obtained from the DataAdapter is:\n" + transformedRequestData.getHeaderStr());

                    	log.debugData("process(): The transformed request body obtained from the DataAdapter is:\n" + transformedRequestData.getBodyStr());
                    }

                    ///////////////////////////////////////////////////////////////////////
                    // Perform GUI validation on the request data if necessary.
                    ///////////////////////////////////////////////////////////////////////

                    if (inputValidator != null)
    		            {
                        log.debug( "Input validation started..." );
                        DataHolder origRequest = new DataHolder();

                        origRequest.setHeader(((XMLGenerator)requestDataBean.getHeaderDataSource()).getOutputCopy().getOutputDOM());
                        origRequest.setBody(((XMLGenerator)requestDataBean.getBodyDataSource()).getOutputCopy().getOutputDOM());

                        DataHolder validationErrors = inputValidator.validate(origRequest, transformedRequestData, session);
                        if (validationErrors != null)
		      	            {
                            log.debug( "Validation errors encountered." );
                            processResponse(curMsgId, req, resp, validationErrors, action, respCodePrefix);

                            return;
                        }
                        else
                            log.debug( "Validation was successful." );

                        log.debug( "Input validation finished." );
                    }
                    else
                        log.debug( "No input validator specified." );

                    DataHolder responseData           = protocolAdapter.sendRequest(transformedRequestData, session);

                    ///////////////////////////////////////////////////////////////////////
                    // Transform the response data into a MessageData object.
                    ///////////////////////////////////////////////////////////////////////

                    if (log.isDebugDataEnabled())
                    {
                           log.debugData("process(): The response header data returned from the ProtocolAdapter is:\n" + responseData.getHeaderStr());

                           log.debugData("process(): The response body data returned from the ProtocolAdapter is:\n" + responseData.getBodyStr());
                    }

                    DataHolder transformedResponseData = dataAdapter.transformResponse(responseData, session);

                    processResponse(curMsgId, req, resp, transformedResponseData, action, respCodePrefix);
                }
            }
            if (sessionInfoBean != null)
            {
              long respTime = System.currentTimeMillis();
              sessionInfoBean.setResponseTime(respTime);
              if (log.isDebugDataEnabled())
              {
                log.debug("ELAPSED TIME:[" + (sessionInfoBean.getResponseTime()
                                              - sessionInfoBean.getRequestTime()) +
                          " ] msecs for action:" + action);
              }
            }
        }
        catch (Exception e)
        {
            req.setAttribute("javax.servlet.error.exception", e);

            req.setAttribute("javax.servlet.error.message", e.getMessage());

            log.error("process(): Encountered processing error:\n" + e.getMessage(), e);

            if ( e instanceof ServletException)
            {
                ServletException se = (ServletException)e;

                Throwable        t  = se.getRootCause();

                if (t != null)
                {
                    log.error("process(): ServletException root cause:\n", t);
                }

                throw (ServletException)e;
            }

            throw new ServletException(e);
        }
    }

    /**
     * This is a convenient debugging method which outputs all the incoming
     * request parameters, not just ones with prefixs of NFH_ and NF_.
     *
     * @param  request  The HttpServletRequest object.
     */
    private void outputRequestParams(HttpServletRequest request)
    {
        if (log.isDebugDataEnabled())
        {
            log.debugData("outputRequestParams():  All incoming raw request parameters are:\n");

            Enumeration requestParamNames;

            if (ServletParamUtils.isMultipartReq(request, getServletContext()))
            {
                try
                {
                    requestParamNames = ServletParamUtils.getMultipartFormFieldsAsMap (request, getServletContext()).elements();
                }
                catch (ServletException e)
                {
                    requestParamNames = null;
                }

            }
            else
                requestParamNames = request.getParameterNames();

            if (requestParamNames != null)
            {
                while (requestParamNames.hasMoreElements())
                {
                    String       paramName        = (String)requestParamNames.nextElement();

                    String[]     paramValues      = request.getParameterValues(paramName);

                    StringBuffer paramValueString = new StringBuffer();

                    for (int i = 0; i < paramValues.length; i++)
                    {
                        if (i > 0)
                        {
                            paramValueString.append(" ");
                        }

                        paramValueString.append("'").append(paramValues[i]).append("'");
                    }

                    log.debugData("  name [" + paramName + "], values [" + paramValueString.toString() + "]");
                }
            }
            else
                log.warn("Request parameters obtained null.");

        }
    }

    /**
     * Prepares the response data for display.
     *
     * @param msgId          The curent message id associated with this request.
     * @param  req           The HttpServletRequest object.
     * @param  resp          The HttpServletResponse object.
     * @param  responseData  The response data.
     * @param  action        The action type.
     * @param  respCodePrefix A response code prefix if one exists
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void processResponse(String msgId, HttpServletRequest req, HttpServletResponse resp, DataHolder responseData, String action, String respCodePrefix) throws ServletException
    {
        ///////////////////////////////////////////////////////////////////////
        // Populate a new instance of ResponseBean with the XML response.
        ///////////////////////////////////////////////////////////////////////



        NFBean responseBean = new XMLBean(responseData.getHeaderStr(), responseData.getBodyStr());

        // add it to the data cache
        // response beans are maintained for each message id.
        // this allow a response to passed along through many redirects.
        //
        KeyTypeMap     dataCache = (KeyTypeMap)req.getSession().getAttribute(ServletConstants.MESSAGE_DATA_CACHE);
        dataCache.put(msgId, ServletConstants.RESPONSE_BEAN, responseBean);

        ///////////////////////////////////////////////////////////////////////
        // Determine the response page to be displayed.
        ///////////////////////////////////////////////////////////////////////

        String respCode = responseBean.getResponseCode();

        // if response code was not returned assume it was successful.
        if (!StringUtils.hasValue(respCode) )  {
           respCode = ServletConstants.SUCCESS;
        }

        String responsePage = pageFlowCoordinator.getNextPage(respCodePrefix, action, respCode);

        ///////////////////////////////////////////////////////////////////////
        // Forward the ResponseBean instance to the response page for display.
        ///////////////////////////////////////////////////////////////////////

        setBeansInRequest(req, msgId);

        if ( StringUtils.hasValue(responsePage) )
           ServletUtils.dispatchRequest(req, resp, responsePage);
        else {
           String err = "Failed to find a destination page for action [" + action + "], response code [" + respCode +"], and response code prefix [" + respCodePrefix + "]";
           log.error(err);
           throw new ServletException(err);
        }

    }


    /**
     * Resets the message cache and the message id sequence, so that some sequence of message ids can
     * be reused. This is useful for testing so that the session/user does not
     * have to be logged in and out each time.
     *
     * @param req HttpServletRequest object
     */
    private void resetDataCache(HttpServletRequest req)
    {

        HttpSession session = req.getSession();

        KeyTypeMap                  requestDataCache = (KeyTypeMap)session.getAttribute(ServletConstants.MESSAGE_DATA_CACHE);

           int size = requestDataCache.size();

           synchronized (requestDataCache) {
                requestDataCache.clear();
            }
            log.info("Cleared message data cache, went from size [" + size +"] to ["
                     + requestDataCache.size() +"]");

            //reset message id back to zero
            synchronized (session) {
                session.removeAttribute(ServletConstants.MESSAGE_ID);
            }

    }




    /**
     * This method adds an incoming request data to an existing "buffer" data Bean.
     * If one does not exist, a new one will be created.  The Bean is stored in the
     * session object for convenient access by other modules in the system.
     *
     * @param  req      HttpServletRequest object.
     * @param  requestData  Incoming request data.
     *
     * @exception  ServletException  Thrown when an error occur during processing.
     *
     * @return  Request data Bean.
     */
    private NFBean bufferRequestData(HttpServletRequest req, Map requestData) throws ServletException
    {
        // Set up request-data cache which stores "historical" data of each session.

        ServletUtils.HeaderBodyData messageData   = ServletUtils.splitData(requestData);

        HttpSession session = req.getSession();

        KeyTypeMap                  requestDataCache = (KeyTypeMap)session.getAttribute(ServletConstants.MESSAGE_DATA_CACHE);

        String                      messageID  = req.getParameter(ServletConstants.NF_FIELD_HEADER_MESSAGE_ID_TAG);

        NFBean                      requestDataBean;


        if ( log.isDebugEnabled() )
          log.debug("bufferRequestData(): Current message cache contains message ids " + requestDataCache.describe() + ".");

        if (StringUtils.hasValue(messageID))
        {
            if (log.isDebugEnabled())
            {
                log.debug("bufferRequestData(): Using message id [" + messageID + "] to look up [" + ServletConstants.REQUEST_BEAN + "] in the request-data cache.");
            }

            requestDataBean   = (NFBean)requestDataCache.get(messageID, ServletConstants.REQUEST_BEAN);

            if (requestDataBean == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("bufferRequestData(): [" + ServletConstants.REQUEST_BEAN + "] does not exist in the request-data cache, creating a new one ...");
                }

                requestDataBean = new XMLBean(messageData.headerData, messageData.bodyData);

                // Don't need to worry about session synchronization problem here
                // since the message ID is unique.

                requestDataCache.put(messageID, ServletConstants.REQUEST_BEAN, requestDataBean);
            }
            else
            {
                log.debug("bufferRequestData(): Located [" + ServletConstants.REQUEST_BEAN + "] in the request-data cache, adding the current request data to it ...");

                requestDataBean.addToHeaderData(messageData.headerData);

                requestDataBean.addToBodyData(messageData.bodyData);
            }
        }
        else
        {
            log.debug("bufferRequestData(): No message id was specified in the request data, automatically generating a new id and creating a new [" + ServletConstants.REQUEST_BEAN + "] ...");

            messageID = String.valueOf(ServletUtils.generateMessageID(session));

            requestDataBean   = new XMLBean(messageData.headerData, messageData.bodyData);

            requestDataCache.put(messageID, ServletConstants.REQUEST_BEAN, requestDataBean);
        }

        // set the message id in the request bean so that the page can find it.
        requestDataBean.setHeaderValue(ServletConstants.NF_FIELD_HEADER_MESSAGE_ID_TAG, messageID );

        setBeansInRequest(req, messageID);
        return requestDataBean;
    }


    /**
     * adds any beans associated with the specified msgId into
     * the request object.
     *
     * @param req   HttpServletRequest
     * @param msgId Message Id
     */
    private void setBeansInRequest(HttpServletRequest req, String msgId)
    {
       KeyTypeMap dataCache = (KeyTypeMap)req.getSession().getAttribute(ServletConstants.MESSAGE_DATA_CACHE);

        if (log.isDebugEnabled())
        {
            log.debug("setBeansInRequest(): Adding all the beans associated with message id [" + msgId + "] to the request object ...");
        }

       Map types = (Map)dataCache.get(msgId);
       StringBuffer addedBeans = new StringBuffer();
       if ( types != null ) {

        synchronized (dataCache) {
          Iterator iter = types.entrySet().iterator();

          while (iter.hasNext()) {
             Map.Entry entry = (Map.Entry) iter.next();
             String type =  (String)entry.getKey();
             addedBeans.append(type);
             if ( iter.hasNext() )
                 addedBeans.append(", ");
             Object value= entry.getValue();
             req.setAttribute(type, value);

            if (log.isDebugDataEnabled())
            {
                String headerStr = null, bodyStr = null;
                if ( value != null && value instanceof NFBean) {
                   headerStr = ((NFBean)value).describeHeaderData();
                   bodyStr =  ((NFBean)value).describeBodyData();
                } else {
                   headerStr = new String("");
                   bodyStr = value.toString();
                }

                StringBuffer logMessage = new StringBuffer("setBeansInRequest(): Adding bean [");

                logMessage.append(type).append("] to the request object.  The bean has the following content:");

                logMessage.append("\n\n-----------------------------------------------\n\nHeader:\n\n");

                logMessage.append(headerStr).append("\nBody:\n\n").append(bodyStr);

                logMessage.append("-----------------------------------------------\n");

                log.debugData(logMessage.toString());
            }
          }
        }

        if (log.isDebugEnabled() )
           log.debug("setBeansInRequest(): [" + types.size() + "] beans [" + addedBeans.toString() + "] have been added to the request.");

       }



    }


}

/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/manager/ConnectorProtocolAdapter.java#1 $
 */

package com.nightfire.webgui.manager;

import java.io.*;
import java.rmi.*;

import java.util.*;
import javax.ejb.*;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.mgrcore.im.*;
import com.nightfire.mgrcore.utils.*;
import com.nightfire.webgui.core.*;
import com.nightfire.webgui.gateway.*;
import com.nightfire.webgui.core.beans.SessionInfoBean;
import com.nightfire.framework.debug.*;
import com.nightfire.mgrcore.common.ManagerException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.mgrcore.errorhandler.*;
import com.nightfire.framework.rules.*;
import org.w3c.dom.Node;

/**
 * <p><strong>ConnectorProtocolAdapter</strong> is an implementation of ProtocolAdapter
 * interface.  It is an implementation provided by the framework.  It allows for the
 * servlet to locate the Connector server and send a request to it.</p>
 */

public class ConnectorProtocolAdapter extends ProtocolAdapterBase implements ManagerServletConstants
{
    private final String IM_URL = CommonConfigUtils.APPLICATION_SERVER_URL_PROP;

    private String imURL =null;


    // This is maintains a reference to the ejb home
    // to prevent a jndi lookup
    private InteractionManagerClient imc = null;

    /**
     * Intializes this component.
     * @param props Servlet properties containing configuration.
     * @param context The servlet context being loaded - has common properties.
     * @exception  ServletException  Thrown when an error occurs during initialization.
     */
    public void init(Properties properties, ServletContext context) throws ServletException
    {
       String webAppName = ServletUtils.getWebAppContextPath(context);
      log = DebugLogger.getLogger(webAppName, ConnectorProtocolAdapter.class);

        log.debug("ConnectorProtocolAdapter(): Initializing ...");

	  Properties contextProps = (Properties ) context.getAttribute ( ServletConstants.CONTEXT_PARAMS ) ;

        imURL      = (String)contextProps.get(IM_URL);

        if ( log.isInfoEnabled() )
            log.info("URL of InteractionManager is [" + imURL + "]");

        if (!StringUtils.hasValue(imURL))
        {
            String errorMessage = "Property " + IM_URL +" is missing.";

            log.error( errorMessage);

            throw new ServletException(errorMessage);
        }
    }


    /**
     * This method either sends a request to the Connector server or process it
     * directly depending on the requested action type.
     *
     * @param  request  Request object.
     * @param  session  HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  Response object returned from the Connector server.
     */
    public DataHolder sendRequest(DataHolder request, HttpSession session) throws ServletException
    {
        XMLGenerator headerParser;

        String       action;

        SessionInfoBean sessionBean = (SessionInfoBean) session.getAttribute(ServletConstants.SESSION_BEAN);

        String cid = sessionBean.getCustomerId();
        String uid = sessionBean.getUserId();


        try
        {
            headerParser = new XMLPlainGenerator(request.getHeaderStr());

            action       = headerParser.getValue(ACTION_NODE);
        }
        catch (Exception e)
        {
            String errorMessage = "Failed to parse the constructed request header for action value:\n" + e.getMessage();

            log.error("sendRequest(): " + errorMessage);

            throw new ServletException(errorMessage);
        }

        ServletContext context               = session.getServletContext();

        List           svcHanderActionlookup = (List)context.getAttribute(SVC_HANDLER_ACTIONS);

        if ((svcHanderActionlookup != null) && svcHanderActionlookup.contains(action))
        {
            return sendSvcHandlerRequest(action, request, cid, uid );
        }
        else
        {
            log.info("sendRequest(): Sending request to IM ...");

            return sendIMRequest(action, cid, uid, request, (short)0);
        }
    }

    private DataHolder sendIMRequest(String action, String cid, String uid, DataHolder request, short countTry) throws ServletException
    {

        DataHolder response = null;

        long startTime = 0;
        if( log.isBenchmarkEnabled() )
            startTime = System.currentTimeMillis();

        synchronized (this) {
            if (imc == null) {
                try {
                    imc = new InteractionManagerClient(imURL);
                }
                catch (IMSystemException e) {
                    log.error("Failed to obtain IM home : " + e.getMessage());
                    throw new ServletException(e);
                }

            }
        }

        ServletException error = null;
        try {

            RemoteContext rc = new RemoteContext(uid, cid);

            InteractionManager im = imc.create();


            String header = "";

            try {
                CustomerContext.getInstance().setCustomerID(cid);
                header = request.getHeaderStr();
                CustomerContext.getInstance().propagate(header);
            }
            catch (FrameworkException e) {
            }


            MessageData resp = im.process(rc, header, request.getBodyStr());


            response = new DataHolder(resp.header , resp.body);


        }
        catch ( java.rmi.RemoteException re ) {

            if ( re.detail == null ) {
                error = new ServletException(re.getMessage(), re );
            }
            else {

                Object o = identifyException(action, request, re.detail );
                if (o instanceof ServletException)
                    error = (ServletException)o;
                else
                    return (DataHolder) o;
            }

        }

        catch ( CreateException ce ) {
            log.warn("Could not create IM object: " + ce.getMessage() );
            error = new ServletException(ce.getMessage(), ce );

       } catch (Exception e) {
           log.error("Failed to send request to IM: " + e.getMessage(),e );
           throw new ServletException(e.getMessage(), e);
       }
        finally
        {

            if (log.isBenchmarkEnabled()) {
                log.benchmark("sendIMRequest: ELAPSED TIME is [" + (System.currentTimeMillis() - startTime) + "] msec for submitting request.");
            }

            if ( error != null) {

                if (error.getRootCause() instanceof RemoteException ||
                    error.getRootCause() instanceof CreateException ) {
                    if (countTry == 0) {
                        log.info("Retrying to connect to IM");
                        ErrorHandler.getInstance().reset();
                        return sendIMRequest(action,cid, uid, request, ++countTry);
                    }
                    else if (countTry == 1) {
                        log.info("Reseting ejb home and retrying to connect to IM");
                        try {
                            imc.cleanup();
                            imc = null;
                            ErrorHandler.getInstance().reset();
                        }
                        catch (IMSystemException e) {
                            throw new ServletException(e);
                        }

                        return sendIMRequest(action,cid, uid, request, ++countTry);
                    }
                    else {
                        log.error("Could not send request to IM: " + error.getMessage());
                        throw error;
                    }
                }
                else {
                    log.error("Got an error back from IM: " + error.getMessage());
                    throw error;
                }
            }



        }

        return response;


    }

	protected Object processIMInvalidDataException(String action, DataHolder request, Throwable e) throws ServletException
	{
		try
        {
			String msg = e.getMessage();

			if (msg != null && msg.indexOf(ErrorCollection.ROOT) > -1 &&
				msg.indexOf(ErrorCollection.CONTAINER) > -1 &&
				msg.indexOf(XML_ID) > -1
				)
			{
				if (log.isDebugDataEnabled())
				{
					log.debugData("processIMInvalidDataException(): Got business rule errors:\n " + msg);

				}

                // If the exception is from Gateway, it should not have <Body> tag in message.
                // If this message contains <Body> tag, it means message is coming from Manager itself.
                // In that case we don't need to modify that message.
                if( msg.indexOf("<" + PlatformConstants.BODY_NODE + ">" )== -1 )
                {
                    // Get the request body parser
                    XMLMessageParser parser = new XMLMessageParser( request.getBodyStr() );

                    // ServiceType is always at the first node under <Body> tag of request.
                    Node currentNode = parser.getNode("0");

                    // The ServiceType, used for constructing response body.
                    String componentName = currentNode.getNodeName();

                    // Extract the error xml from the exception message
                    msg = msg.substring(msg.indexOf(XML_ID));

					if (log.isDebugDataEnabled())
					{
						log.debugData("Error message is:" + msg);

					}

					/*
					 * Costructing the Error message in the format acceptible as business rule error. e.g.
					 * <?xml version="1.0"?>
					 *	<Body>
					 *	  <Loop index="" value=""/>
					 *	</Body>
					 */

                    XMLMessageGenerator xgen = new XMLMessageGenerator(PlatformConstants.BODY_NODE);

                    // Index is always "0" as gateway send rule errors for one component at a time.
                    xgen.setAttributeValue(componentName, "index", "0");
                    xgen.setAttributeValue(componentName, "value", msg);

					if (log.isDebugDataEnabled())
					{
						log.debugData("Final message is:" + xgen.generate());

					}

                    msg = xgen.generate();
                }

				return formatErrRespCode(request, msg);
			}
			else
			{
				log.warn("Failed to send request(IMInvalidDataException): " +
						 e.getMessage(), e);
				addSimpleError(action, (Exception) e);
				// we don't had a root cause so that only the error
				// handler message gets displayed
				return new ServletException(ErrorHandler.getInstance().
											getCompoundedError());
			}
		} catch(Exception ex) {
            String errorMessage = "Encountered an error while processing IMInvalidDataException:\n" + ex.getMessage();

            log.error("processIMInvalidDataException(): " + errorMessage);

            return new ServletException(ex);
		}
	}
}

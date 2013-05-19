package com.nightfire.webgui.core;

import javax.servlet.*;

import com.nightfire.webgui.core.*;
import com.nightfire.framework.debug.*;
import com.nightfire.mgrcore.im.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.mgrcore.errorhandler.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.rules.*;

/**
 * Base class for gateway and manager protocol adapter. This class provides common methods
 * for both protocol adapters.
 */
public abstract class ProtocolAdapterBase implements ProtocolAdapter, PlatformConstants
{
    //Do not change this to private, as this is initialized in the parent/child class as
    //appropriate.
    protected DebugLogger log;

    // Default is to use the InteractionManager category.
    protected String im_config = null;

    // security should default to true in all cases
    protected boolean auth = true;

    //public static final String ERRORS_TAG_START = "<" + ErrorCollection.ROOT + ">";
    //public static final String ERRORS_TAG_END = "</" + ErrorCollection.ROOT + ">";

    public static final String XML_ID = "<?xml";

    /**
     * Process the request via service handler instead of the regular gateway route.
     *
     * @param  requestData  Request object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  Response object returned from the service handler.
     */
    protected DataHolder sendSvcHandlerRequest( String action, DataHolder requestData, String cid, String uid )
    throws ServletException
    {

        long startTime = 0;

        if(log.isBenchmarkEnabled())
            startTime = System.currentTimeMillis();

        try
        {
            ServiceHandlerFacade shf =
                    new ServiceHandlerFacade( im_config, auth );

            MessageData response = shf.process(
                    new RemoteContext( uid, cid ),
                    requestData.getHeaderStr(),
                    requestData.getBodyStr());


            return new DataHolder(response.header, response.body);
        }
        catch (Exception e)
        {

            Object o = identifyException(action, requestData,e );

            ServletException error;

            if (o instanceof ServletException)
                error = (ServletException)o;
            else
                return (DataHolder) o;

            String errorMessage = "Encountered an error while processing the request via service handler:\n" + error.getMessage();

            log.error("sendToSvcHandler(): " + errorMessage);

            throw error;

        }
        finally
        {
            if (log.isBenchmarkEnabled())
            {
                log.benchmark("sendToSvcHandler(): ELAPSED TIME is [" + (System.currentTimeMillis() - startTime) + "] msec for processing request.");
            }
        }
    }//sendSvcHandlerRequest

    /**
     * Identify the type of exception that occurred and extract the appropriate error
     * information from it.
     */
    protected Object identifyException(String action, DataHolder request,
                                       Throwable e) throws ServletException
    {

        if (e == null)
        {
            return new ServletException("System Error", e);
        }
        else if (e instanceof IMInvalidDataException)
        {
			return processIMInvalidDataException(action,request,e);
		}
        else if (e instanceof IMProcessingException)
        {
            log.warn("Failed to send request(IMProcessingException): " +
                     e.getMessage(), e);
            addSimpleError(action, (Exception) e);
            // we don't had a root cause so that only the error
            // handler message gets displayed
            return new ServletException(ErrorHandler.getInstance().
                                        getCompoundedError());
        }
        else if (e instanceof IMSecurityException)
        {
            log.warn("Security warning: " + e.getMessage(), e);
            return new AuthorizationException(e.getMessage());

        }
        else
        {
            // in this case we had some other remote exception while
            // trying to connect. We set the root cause so that it
            // can be used for evaulation in the calling method
            log.warn("Failed to send: " + e.getMessage(), e);
            return new ServletException(e.getMessage(), e);
        }
    }

	protected Object processIMInvalidDataException(String action, DataHolder request, Throwable e) throws ServletException
	{
            String msg = e.getMessage();

            // CR 20639: Need to make sure msg starts with "<?xml".
            if (msg != null && msg.indexOf(ErrorCollection.ROOT) > -1 &&
                msg.indexOf(ErrorCollection.CONTAINER) > -1 &&
                msg.startsWith(XML_ID)
                )
            {
                if (log.isDebugDataEnabled())
                {
                    log.debugData("Got business rule errors: " + msg);

                }

                // CR 20639: Need to strip out extra stuff added by bo methods.
                /*
                // Strip does not work because the GUI was expecting the format
                // as returned by RuleServiceHandler.
                int errorsTagStart = msg.indexOf(ERRORS_TAG_START);
                int errorsTagEnd = msg.indexOf(ERRORS_TAG_END);
                msg = msg.substring(errorsTagStart, errorsTagEnd + ERRORS_TAG_END.length());
                */

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
        }

    //Since we have so many types of exceptions and the same code with minor variations in each of
    //the catch methods, we prefer to invoke this method in its place.
    protected void addSimpleError(String action, Exception e)
    {
        if (ErrorHandler.getInstance().isEmpty())
        {
            ErrorHandler.getInstance().addError(e, action,
                                                this.getClass().getName(),
                                                ErrorHandlerConstants.
                                                CONNECTOR_TYPE);
        }
    }

    /**
     * Set the ResponseCode value on the header node to the appropriate error value.
     */
    protected DataHolder formatErrRespCode(DataHolder request, String err) throws
        ServletException
    {
        return formatErrRespCode(request, err, null);
    }

    protected DataHolder formatErrRespCode(DataHolder request, String errXml,
                                           String code) throws ServletException
    {
        try
        {

            if (code == null)
            {
                code = PlatformConstants.FAILURE_VALUE;

            }
            XMLPlainGenerator gen = new XMLPlainGenerator(request.getHeaderStr());
            gen.setValue(PlatformConstants.RESPONSE_CODE_NODE, code);

            return new DataHolder(gen.getOutput(), errXml);
        }
        catch (MessageException m)
        {
            throw new ServletException("Failed to set error response code: " +
                                       m.getMessage());
        }

    }

}

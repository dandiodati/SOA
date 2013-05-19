package com.nightfire.webgui.core.josso;

import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.webgui.core.beans.SessionInfoBean;
import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.debug.DebugLogger;
import com.nightfire.framework.debug.DebugConfigurator;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.security.SecurityService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import org.josso.gateway.identity.SSOUser;

public class NSSecurityFilter
        implements Filter, ServletConstants, PlatformConstants {

    /**
     * Implementation of Filter's init().  This allows for this filter to perform
     * any initialization tasks.
     *
     * @param  filterconfig  FilterConfig object.
     *
     * @exception javax.servlet.ServletException  Thrown when any error occurs during initialization.
     */
    public void init(FilterConfig filterconfig)
        throws ServletException {
        String s = ServletUtils.getWebAppContextPath(filterconfig.getServletContext());
        log = DebugLogger.getLogger(s, getClass());
    }


     /**
     * Implementation of Filter's doFilter().  This is where any filtering tasks
     * get perform.  In this case setSessionBean
     *
     * @param  request   ServletRequest object.
     * @param  response  ServletResponse object.
     * @param  chain     The view into the invocation chain of a filtered request
     *                   for a resource
     *
     * @exception  javax.servlet.ServletException  Thrown when an error occurs during processing.
     */

     public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        log.debug("doFilter(): Performing filter tasks ...");
        HttpServletRequest httpservletrequest = (HttpServletRequest)request;

        try{

             if (!(request instanceof HttpServletRequest))
                  {
                      String errorMessage = "The request object is not of type javax.servlet.http.HttpServletRequest.";

                      log.error("doFilter(): " + errorMessage + "  It's of type " + request.getClass().getName());

                      throw new ServletException(errorMessage);
                  }

             log.debug(" Filter doFilter(): Performing filter tasks ...");

            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // Get the SessionBean.
            HttpSession session     = httpRequest.getSession();
            SessionInfoBean sBean = (SessionInfoBean)session.getAttribute(ServletConstants.SESSION_BEAN);

            String cid = "";
            String uid = "";
            String pass = "";
            // during init there will not be a session bean yet.
            if ( sBean != null)
            {

				// add customer id into the customer context so that it
                // is propagated everywhere
                // Also add the customer id as diagnosistic info
                // so that it can be used in logging.

                cid = sBean.getCustomerId();
                uid = sBean.getUserId();
                String did = SecurityService.getInstance(cid).getSubDomainId ( uid );
                log.debug("setting following info from sBean to context: "
                            + "cid [" + cid + "]"
                            + ", user [" + uid + "]"
                            + ", subDomain [" + did + "]"
                            );

                CustomerContext.getInstance().setCustomerID(cid);
                CustomerContext.getInstance().setUserID(uid);

                String loginfo = cid;
                if (StringUtils.hasValue ( uid ))
                {
                    loginfo += ":" + uid;
                }
                if (StringUtils.hasValue ( did , true))
                {
                    CustomerContext.getInstance().setSubDomainId (did);
                    loginfo += ":" + did;
                }
                else
                    CustomerContext.getInstance().setSubDomainId ("");

                DebugLogger.setDiagnosticInfo(DebugConfigurator.CID_KEY, loginfo);
            }
            else
            {
                // forces the setting of the default customer id
                CustomerContext.getInstance().setCustomerID(null);
                CustomerContext.getInstance().setUserID(null);
            }

            // check if there is any exceptions set or
            // if there are error parameters set (by internal-error.jsp)
            Throwable t = (Throwable)request.getAttribute("javax.servlet.error.exception");
            Integer errorCode = (Integer)request.getAttribute("javax.servlet.error.status_code");
            String errorCodeParam = request.getParameter("errorCode");
            String errorMsgParam = request.getParameter("errorMessage");

            if (t != null || errorCode != null || errorCodeParam != null || errorMsgParam != null)
            {
                chain.doFilter(request, response);
                log.error("There was an error, bypassing security filter:" + errorMsgParam);

                return;
            }

             SSOUser ssouser = (SSOUser)httpservletrequest.getUserPrincipal();
             for(int i = 0; i < ssouser.getProperties().length; i++)
              {
              if("customerid".equals(ssouser.getProperties()[i].getName()))
                   cid = ssouser.getProperties()[i].getValue();
              if("username".equals(ssouser.getProperties()[i].getName()))
                  uid = ssouser.getProperties()[i].getValue();
              if("password".equals(ssouser.getProperties()[i].getName()))
                 pass = ssouser.getProperties()[i].getValue();
            }

                CustomerContext.getInstance().setCustomerID(cid);
                CustomerContext.getInstance().setUserID(uid);
            
            setSessionBean(cid, uid, pass,session);
            chain.doFilter(request, response);
        }
        catch (Exception e)
        {
            request.setAttribute("javax.servlet.jsp.jspException", e);

            request.setAttribute("javax.servlet.error.message", e.getMessage());

            log.error("doFilter(): Encountered processing error:\n" + e.getMessage());

            throw new ServletException(e);
        }
    }

    /**
     * This method creates a session bean and set it in the session object.
     *
     * @param cid The customer id
     * @param  username  User name.
     * @param  password  Password.
     * @param  session   HttpSession object.
     *
     * @exception  javax.servlet.ServletException  Thrown when an error occurs during processing.
     */
    private void setSessionBean(String cid, String username, String password, HttpSession session)
        throws ServletException
    {
        try
        {   long starttime = System.currentTimeMillis();
            SessionInfoBean sessioninfobean = new SessionInfoBean(cid, username, password, starttime);
            session.setAttribute(SESSION_BEAN, sessioninfobean);
         }
        catch(Exception exception)
        {
            String errorMessage = (new StringBuilder()).append("Failed to create a session bean and set it in the session objec: ").append(exception.getMessage()).toString();
            log.error((new StringBuilder()).append("setSessionBean(): ").append(errorMessage).toString());
            throw new ServletException(errorMessage, exception);
        }
    }

    public void destroy()
    {
        if(log.isDebugEnabled())
            log.debug("destroy(): Performing any filter cleanup tasks ...");
    }

    private DebugLogger log;
}
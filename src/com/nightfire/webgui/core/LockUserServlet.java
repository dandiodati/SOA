package com.nightfire.webgui.core;

import com.nightfire.framework.util.StringUtils;
import com.nightfire.security.SecurityService;
import com.nightfire.security.RBACProvisionException;
import com.nightfire.security.SecurityException;
import com.nightfire.security.data.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.*;

/**
 * <p><strong>LockUserServlet</strong>
 * It is responsible for
 *  - locking the user.
 *  - updating the failure login attempt counter.
 *  - reloading the local cache for that user.
 *  - acknowledge the caller.
 */
public class LockUserServlet extends HttpServlet{

    protected static final String LOCK_USER_SERVLET_SUFFIX = "/gateway/NFLockUserServlet";

    protected static final String DOMAIN_NAME_PROP = "domainName";
    protected static final String USER_NAME_PROP   = "userName";
    protected static final String FAILURE_LOGIN_ATTEMPT_PROP = "failureLoginAttempt";

    private String domainName;
    private String userName;
    private int failureLoginAttempt;

    /**
     * Redefinition of init() in HttpServlet.  This allows for all the resources
     * to be initialized before the servlet starts accepting requests.
     *
     * @exception  javax.servlet.ServletException  Thrown when any error occurs during initialization.
     */
    public void init() throws ServletException
    {
        // does nothing
    }

    /**
     * Implementation of doPost(HttpServletRequest, HttpServletResponse) in HttpServlet.
     *
     * @param  req   HttpServletRequest object.
     * @param  resp  HttpServletResponse object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     * @exception  java.io.IOException       This exception should never get thrown.  All
     *                               errors are wrapped in ServletException.  This
     *                               exists to satisfy the method signature.
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setContentType("text/html;charset=UTF-8");

        domainName = req.getHeader(DOMAIN_NAME_PROP);
        userName = req.getHeader(USER_NAME_PROP);

        try
        {
            failureLoginAttempt = Integer.parseInt(req.getHeader(FAILURE_LOGIN_ATTEMPT_PROP));
        }
        catch (NumberFormatException nfe)
        {
            failureLoginAttempt = -1;
        }

        if (!StringUtils.hasValue(domainName) || !StringUtils.hasValue(userName) || failureLoginAttempt < 0)
        {
            throw new ServletException("Invalid request");
        }

        try
        {

            SecurityService ss = SecurityService.getInstance(domainName);
            User userToLock = ss.getUser(userName);
            User[] users = ss.getUsers();

            for (int i = 0; i < users.length; i++ )
            {
                if ( userToLock.getUserId() == users[i].getUserId() )
                {
                    users[i].setLocked( 1 );
                    users[i].setFailedLoginAttemptCounter(failureLoginAttempt);
                }
            }

            ss.updateFailedLoginAttemptCounter(userName, failureLoginAttempt);
        }
        catch (RBACProvisionException e)
        {
            throw new ServletException("Exception occurred while locking user." + e.getMessage());
        }
        catch (SecurityException e)
        {
            throw new ServletException("Exception occurred while locking user." + e.getMessage());
        }


        BufferedWriter bw = new BufferedWriter(resp.getWriter());
        bw.write("OK response from lock user servlet!!!");
        bw.close();
    }
    /**
     * Implementation of doPost(HttpServletRequest, HttpServletResponse) in HttpServlet.
     *
     * @param  req   HttpServletRequest object.
     * @param  resp  HttpServletResponse object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     * @exception  java.io.IOException       This exception should never get thrown.  All
     *                               errors are wrapped in ServletException.  This
     *                               exists to satisfy the method signature.
     */
//    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
//    {
//        doPost(req,resp);
//    }
}

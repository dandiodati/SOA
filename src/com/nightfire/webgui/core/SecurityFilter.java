/**
 * Copyright (c) 2003 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  java.io.*;
import  java.util.*;
import java.net.*;

import  javax.servlet.*;
import  javax.servlet.http.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.constants.PlatformConstants;
import  com.nightfire.security.*;
import com.nightfire.security.rbacprovision.RBACDatabaseConstants;
import com.nightfire.security.data.User;
import com.nightfire.security.store.StoreProviderBase;
import com.nightfire.security.domain.DomainType;
import com.nightfire.security.domain.DomainTypeException;
import com.nightfire.security.domain.DomainProperties;
import com.nightfire.security.domain.DomainPropException;
import  com.nightfire.webgui.core.beans.*;
import com.nightfire.webgui.core.tag.TagUtils;
import  com.nightfire.framework.debug.*;
import com.nightfire.rbac.web.user.RBACUserJSPConstants;


/**
 * <p><strong>SecurityFilter</strong> represents the filter class which
 * performs authentication tasks before each request reaches the resource.</p>
 */

public class SecurityFilter implements Filter, ServletConstants, PlatformConstants
{
    public  static final String LOGIN_ACTION          = "login";
    public  static final String LOGOUT_ACTION         = "logout";

    private static final String USER_PASSWORD_FIELD   = "Password";

    public static final String CLIENT_TZ_OFFSET      = "ClientTimezoneOffset";
    public static final String DAYS_TO_EXPIRE_PASSWORD  = "DaysToExpirePassword";

    public static final String WHOLESALE_PROVIDER      = "WHOLESALE_PROVIDER";
    // This is used in case of API boxes where Normal Domains (non-branded) and branded both domain need to submit requests.
    public static final String ALLOW_NONBRANDED_DOMAINS_PROP      = "ALLOW_NONBRANDED_DOMAINS";
    public static final String SERVER_NAME_BASED_WSP      = "SERVER_NAME_BASED_WSP";
    public static final String LOGIN_WITHOUT_WSP_PROP      = "LOGIN_WITHOUT_WSP";

    private static final String USER_INFO_COOKIE      = "USER_INFO";
    private static final String SINGLE_SIGN_ON_PROP   = "SINGLE_SIGN_ON";

    private static final String LOGIN_PAGE            = "LOGIN_PAGE";
    private static final String LOGOUT_PAGE           = "LOGOUT_PAGE";
    private static final String WELCOME_PAGE          = "WELCOME_PAGE";
    private static final String INVALID_LOGIN_PAGE    = "INVALID_LOGIN_PAGE";

    private static final String NORMAL_LOGIN_TYPE    = "NORMAL";
    private static final String LOGIN_TYPE_PROP    = "LoginType";

    private static boolean singleSignOn             = true;
    private static Map     userInfoLookup           = new Hashtable();
    private static Map     authenticatedUserLookup  = new Hashtable();


    // default password expiration interval in no. of days.
    private static final int     DEFAULT_PASSWORD_EXPIRATION_INTERVAL  = 60;

    // Milliseconds in a 24 hour day.
    private static final long MILLSECS_PER_DAY = 1000 * 60 * 60 * 24;

    private FilterConfig filterConfig;

    private DebugLogger  log;

    private String       loginPage;
    private String       logoutPage;
    private String       invalidLoginPage;

    private String loginType;

    /**
     * Implementation of Filter's init().  This allows for this filter to perform
     * any initialization tasks.
     *
     * @param  filterConfig  FilterConfig object.
     *
     * @exception  ServletException  Thrown when any error occurs during initialization.
     */
    public void init(FilterConfig filterConfig) throws ServletException
    {
        String webAppName = ServletUtils.getWebAppContextPath(filterConfig.getServletContext());

        log               = DebugLogger.getLogger(webAppName, getClass());

        initParameters(filterConfig);

        initSingleSignOn();
    }

    /**
     * This method loads in initialization parameters.
     *
     * @param  filterConfig  FilterConfig object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void initParameters(FilterConfig filterConfig) throws ServletException
    {
        this.filterConfig = filterConfig;

        // Get the login page.

        loginPage = filterConfig.getInitParameter(LOGIN_PAGE);

        if (!StringUtils.hasValue(loginPage))
        {
            String errorMessage = "The filter initialization parameter [" + LOGIN_PAGE + "] must be specified with a valid value.";

            log.error("initParameters(): " + errorMessage);

            throw new ServletException(errorMessage);
        }

        // Get the logout page.

        logoutPage = filterConfig.getInitParameter(LOGOUT_PAGE);

        if (!StringUtils.hasValue(logoutPage))
        {
            log.warn("initParameters(): The filter initialization parameter [" + LOGOUT_PAGE + "] is not specified with a valid value.  The page specified by [" + LOGIN_PAGE + "] will be used instead.");

            logoutPage = loginPage;
        }

        // Get the invalid-login page.

        invalidLoginPage = filterConfig.getInitParameter(INVALID_LOGIN_PAGE);

        if (!StringUtils.hasValue(invalidLoginPage))
        {
            log.warn("initParameters(): The filter initialization parameter [" + INVALID_LOGIN_PAGE + "] is not specified with a valid value.  The page specified by [" + LOGIN_PAGE + "] will be used instead.");

            invalidLoginPage = loginPage;
        }
    }

    /**
     * This method initializes the context single-sign-on flag, which is valid
     * across all web applications.
     *
     * @exception  ServletException  Thrown when any error occurs during initialization.
     */
    private void initSingleSignOn() throws ServletException
    {
        Properties contextProps = (Properties) filterConfig.getServletContext().getAttribute(ServletConstants.CONTEXT_PARAMS);

        String strTemp = contextProps.getProperty(SINGLE_SIGN_ON_PROP);

        if (StringUtils.hasValue(strTemp))
        {
            if (log.isDebugEnabled())
                log.debug("initSingleSignOn(): Got single sign on prop [" + strTemp + "].");

            singleSignOn = StringUtils.getBoolean(strTemp, true);
        }
        else if (log.isDebugEnabled())
        {
            log.debug("initSingleSignOn(): No single sign on prop is found, set default to true.");
        }

    }

    /**
     * Implementation of Filter's doFilter().  This is where any filtering tasks
     * get perform.  In this case, it's user-authentication.
     *
     * @param  request   ServletRequest object.
     * @param  response  ServletResponse object.
     * @param  chain     The view into the invocation chain of a filtered request
     *                   for a resource
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if (log.isDebugEnabled())
            log.debug("doFilter(): Performing filter tasks ...");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        debugRequestCookies( httpRequest );

        try
        {
            if (!(request instanceof HttpServletRequest))
            {
                String errorMessage = "The request object is not of type javax.servlet.http.HttpServletRequest.";

                log.error("doFilter(): " + errorMessage + "  It's of type " + request.getClass().getName());

                throw new ServletException(errorMessage);
            }

            if (!(response instanceof HttpServletResponse))
            {
                String errorMessage = "The response object is not of type javax.servlet.http.HttpServletResponse.";

                log.error("doFilter(): " + errorMessage + "  It's of type " + response.getClass().getName());

                throw new ServletException(errorMessage);
            }


            //Extract the Cookie User Information.
            Cookie userInfoCookie = ServletUtils.getCookie(httpRequest, USER_INFO_COOKIE);
            CookieUserInfo cui = null;
            if(userInfoCookie != null) {
                String userInfo=userInfoCookie.getValue();
                try {
                    userInfo = URLDecoder.decode(userInfo, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("doFilter(): Error occurred while decoding user info cookie." );
                }
                cui = new CookieUserInfo();
                cui.deserialize(userInfo);
                cui.describe ();
            }

            // Get the SessionBean.
            HttpSession session     = httpRequest.getSession();
            SessionInfoBean sBean = (SessionInfoBean)session.getAttribute(ServletConstants.SESSION_BEAN);
            // putting empty wsp in cookie throws exception, so default value need to set
            String wsp = DomainType.NOBODY;
            // If System property exists for WHOLESALE_PROVIDER, we will use that
            String     wspNameSP = System.getProperty(WHOLESALE_PROVIDER);
            boolean    allowNonBrandedDomains = StringUtils.getBoolean (System.getProperty(ALLOW_NONBRANDED_DOMAINS_PROP), false);

            // during init there will not be a session bean yet.
            if ( sBean != null)
            {

                // If CID/UserName  in Cookie and SessionBean do not match then delete the cookies, invalidate the session and redirect user to LoginPage.
                if( singleSignOn &&  cui != null &&  !( (sBean.getCustomerId()).equals(cui.cid)  && (sBean.getUserId()).equals(cui.user) ) ) {
                    log.error("doFilter(): CustomerId/UserId values in cookie and sessionBean do not match so deleting the cookie, invalidating the session and redirecting user to LoginPage. " + 
                        "[SessionBean : {" + sBean.getCustomerId() + "/" + sBean.getUserId() + "} Cookie : {" + cui.cid + "/" + cui.user + "} ]" );
                    wsp = sBean.getWsp();
                    deleteCookie(httpRequest, httpResponse);
                    session.invalidate();
                    String wspLoginUrl = getWSPLoginUrl(wsp, loginPage, wspNameSP);
                    ServletUtils.dispatchRequest(request, response, wspLoginUrl);
                    return;
                }

				// add customer id into the customer context so that it
                // is propagated everywhere
                // Also add the customer id as diagnosistic info
                // so that it can be used in logging.

                String cid = sBean.getCustomerId();
                String uid = sBean.getUserId();
                wsp        = sBean.getWsp();
                String did = SecurityService.getInstance(cid).getSubDomainId ( uid );
                if (log.isDebugEnabled())
                    log.debug("setting following info from sBean to context: "
                            + "cid [" + cid + "]"
                            + ", user [" + uid + "]"
                            + ", wsp [" + wsp + "]"
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
            } else {
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

            if (t != null || errorCode != null || errorCodeParam != null || errorMsgParam != null) {
                chain.doFilter(request, response);
                log.error("There was an error, bypassing security filter:" + errorMsgParam);

                return;
            }



            //HttpSession session     = ((HttpServletRequest)request).getSession();

            String      action      = request.getParameter(ServletConstants.NF_FIELD_HEADER_PREFIX + ACTION_NODE);

            boolean passwordChangedSuccessfully = StringUtils.getBoolean((String) request.getParameter(RBACUserJSPConstants.PASSWORD_CHANGED_SUCCESSFULLY), false);
            String newPassword = (String) request.getParameter(RBACUserJSPConstants.P_PASSWORD);
            boolean passwordChangeRequired = StringUtils.getBoolean((String) session.getAttribute(RBACUserJSPConstants.PASSWORD_CHANGE_REQUIRED), false);

            String      cid    = request.getParameter(ServletConstants.NF_FIELD_PREFIX + CUSTOMER_NAME_FIELD);

            String      userName    = request.getParameter(ServletConstants.NF_FIELD_PREFIX + USER_NAME_FIELD);

            String      password    = request.getParameter(ServletConstants.NF_FIELD_PREFIX + USER_PASSWORD_FIELD);

            String     clientTZOffset = request.getParameter(ServletConstants.NF_FIELD_PREFIX + CLIENT_TZ_OFFSET);

            // value of WSP from login page
            String     wspName = request.getParameter("NF_wspName");

            String      backURL = request.getParameter("NF_BackURL");

            // if retrieved login page WSP then use that
            // else if we didn't get WSP from login page and its branded GUI, then obtain the WSP from DOMAIN_PROVIDER table using DomainType API.
            if (StringUtils.hasValue(wspName))
                wsp = wspName;
            else if(StoreProviderBase.ANY_VALUE.equals(wspNameSP) && LOGIN_ACTION.equals(action))
            {
                session.setAttribute(LOGIN_WITHOUT_WSP_PROP, String.valueOf(true));
                wsp = TagUtils.getDomainProvider(cid);

                if (log.isDebugEnabled())
                    log.debug("WSP name not found. Hence obtained from DomainProvider table [" + wsp+ "]");

                if (StringUtils.hasValue(wsp) && DomainType.NOBODY.equalsIgnoreCase(wsp))
                {
                    // If explicitly Non-Branded domains are also allowed to login... Like in case of API box.
                    if (allowNonBrandedDomains)
                    {
                        log.warn("doFilter(): The domain ["+ cid + "] is not a valid Branded domain. But system property [" + ALLOW_NONBRANDED_DOMAINS_PROP + "] is set to [" + allowNonBrandedDomains
                                + "]. Hence allowing user [" + userName + "] login.");
                    }
                    else
                    {
                        log.error("doFilter(): The domain ["+ cid + "] is not a valid Branded domain. Sending the user [" + userName + "] to the invalid-login page ...");

                        ServletUtils.dispatchRequest(request, response, invalidLoginPage);
                        invalidateSessions(httpRequest, session);

                        return;
                    }
                }
            }

            if (StringUtils.hasValue(wspNameSP) && !wspNameSP.equals(StoreProviderBase.ANY_VALUE))
                wsp = wspNameSP;

            // setting in request in case of invalid-login
            request.setAttribute("wsp", wsp);

            if (!StringUtils.hasValue(clientTZOffset, true))
            {
                clientTZOffset = "0";
            }

            loginType = request.getParameter(NF_FIELD_PREFIX  + LOGIN_TYPE_PROP);

            if(null == loginType)
                loginType = NORMAL_LOGIN_TYPE;

            // After resetting password successfully change the password in session bean and cookie.
            // Also Authenticated user lookup map need to be updated to store this session with new userinfo (cookie value)
            if (passwordChangeRequired && passwordChangedSuccessfully)
            {
                sBean.setPassword(newPassword);
                Cookie newCookie = addCookie(httpRequest, httpResponse);
                updateAuthenticatedUserLookup(newCookie, session);
            }


            if (LOGIN_ACTION.equals(action))
            {
                if (!StringUtils.hasValue(cid) || !StringUtils.hasValue(userName) ||
                    !StringUtils.hasValue(password))
                {
                    log.warn("doFilter(): Missing cid and/or username and/or password fields, sending the user to the invalid-login page ...");

                    ServletUtils.dispatchRequest(request, response, invalidLoginPage);

                    return;
                }

                if (log.isDebugEnabled())
                {
                    log.debug("doFilter(): Authenticating cid [" + cid +"], user [" + userName + "] ...");
                }

				// During login if submitted CID/UserName do not match CID/userName in SessionBean then redirect user to LoginPage.
				if ( (sBean != null) &&  
					!( (sBean.getCustomerId()).equals(cid) && (sBean.getUserId()).equals(userName) ) )
				{
					log.error("doFilter(): CustomerId/UserId submitted in login doesn't match with the values in session bean so redirecting to Invalid Login Page." + 
						"[SessionBean : {" + sBean.getCustomerId() + "/" + sBean.getUserId() + "} Login data : {" + cid + "/" + userName + "} ]");
                    ServletUtils.dispatchRequest(request, response, invalidLoginPage);
					return;
				}
				// During login if submitted CID/UserName do not match CID/userName in Cookie then redirect user to LoginPage.
				if ( singleSignOn && (cui != null) && !( (cui.cid).equals(cid) && (cui.user).equals(userName) ) )
				{
					log.error("doFilter(): CustomerId/UserId submitted in login doesn't match with the values in Cookie so redirecting to Invalid Login Page." + 
						"[Cookie : {" + cui.cid + "/" + cui.user + "} Login data : {" + cid + "/" + userName + "} ]");
                    ServletUtils.dispatchRequest(request, response, invalidLoginPage);
					return;
				}


                SecurityService securityService = null;

                try
                {
                    securityService = SecurityService.getInstance(cid);
                }
                catch (com.nightfire.security.SecurityException e) {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doFilter():Invalid customer id, sending the user to the invalid-login page: " + e.getMessage());
                    }

                    ServletUtils.dispatchRequest(request, response, invalidLoginPage);

                    return;
                }

                try {
                    //Validated user's type with GUI request type G.
                    securityService.validateUserType(userName,SecurityService.REQUEST_TYPE.G);

                    //if the securityservice never initialized we will never
                    // make it here.
                    securityService.authenticate(userName, password);
                    String subDomainId = securityService.getSubDomainId (userName);
                    if (StringUtils.hasValue ( subDomainId ))
                        CustomerContext.getInstance ().setSubDomainId ( subDomainId );
                }
                catch (AuthenticationException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doFilter(): Invalid cid and/or username and/or password, sending the user to the invalid-login page ...");
                    }

                    if (e instanceof UnsuccessfulLoginAttemptExceeded)
                        handleULAEException(securityService, session, cid, userName);

                    if (e instanceof InvalidUserTypeException
                        || e instanceof UnsuccessfulLoginAttemptExceeded)
                    {
                        request.setAttribute(RBACUserJSPConstants.EXCEPTION_TYPE_PROP,e.getClass().getSimpleName());
                    }

                    ServletUtils.dispatchRequest(request, response, invalidLoginPage);
                    return;
                }

                // Validating WSP against available multiple domainProviders
                try {
                    if (StringUtils.hasValue(cid))
                    {
                        Set<String> providers = TagUtils.getDomainProviders(cid);
                        boolean canLogin = false;
                        if (providers.size() > 0)
                        {
                            for (String providerName : providers)
                            {
                                if (wsp.equals(providerName))
                                {
                                    canLogin = true;
                                    break;
                                }
                            }
                        }

                        // No provider if BASIC box
                        if (!canLogin)
                            canLogin = !StringUtils.hasValue(wsp) || wsp.equals(DomainType.NOBODY);
                        
                        if (!canLogin)
                        {
                            throw new AuthenticationException ("");
                        }
                    }
                }
                catch (AuthenticationException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doFilter(): Invalid cid and/or username and/or password and/or logging with wrong Domain Provider, sending the user to the invalid-login page ...");
                    }

                    ServletUtils.dispatchRequest(request, response, invalidLoginPage);

                    return;
                }


                // Setting time zone variables in session
                session.setAttribute(CLIENT_TZ_OFFSET, clientTZOffset);
                setSessionBean(cid, userName, password, wsp, session);

                if (singleSignOn)
                {
                    Cookie cookie = addCookie(httpRequest, httpResponse);

                    updateAuthenticatedUserLookup(cookie, session);
                }

                if (log.isDebugEnabled())
                {
                    log.debug("doFilter(): CID [" + cid +"], User [" + userName + "] has been successfully authenticated.  Dispatching to web application's welcome page ...");
                }

                // ****************************************Start Password Expiration Logic****************************************

                User currentUser = securityService.getUser(userName);
                boolean isPasswordChangeRqrd = isPasswordChangeRequired(currentUser,session);

                if (isPasswordChangeRqrd)
                {
                    session.setAttribute(RBACUserJSPConstants.PASSWORD_CHANGE_REQUIRED,"true");
                }

                // ****************************************End Password Expiration Logic****************************************

                //ServletUtils.dispatchRequest(request, response, "/");

                String noQryStr = httpRequest.getRequestURL().toString();

                if(noQryStr.indexOf("gateway")!= -1 )
                {
                    noQryStr = noQryStr + "?NFH_Page=/";
                }
                else//for security application
                {
                    if(noQryStr.indexOf("NFServlet")!= -1 )
                    {
                        noQryStr = noQryStr.substring(0, noQryStr.lastIndexOf("NFServlet"));
                    }
                }

                if (isPasswordChangeRqrd && session.getAttribute(RBACUserJSPConstants.BACK_URL) == null)
                {
                    if (!StringUtils.hasValue(backURL))
                        backURL = noQryStr;

                    session.setAttribute(RBACUserJSPConstants.BACK_URL, backURL);
                }

                httpResponse.sendRedirect( noQryStr );
                return;
            }

            if (authenticationRequired(httpRequest))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("doFilter(): The user has not been authenticated.  Sending the user to the login page ...");
                }

                Cookie cookie = ServletUtils.getCookie(httpRequest, USER_INFO_COOKIE);
                String loginUrl = loginPage;

                if (cookie != null)
                {
                    String userInfo = cookie.getValue();
                    try {
                        userInfo = URLDecoder.decode(userInfo, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        log.warn("doFilter(): Error occurred while decoding user info cookie." );
                    }
                    CookieUserInfo cuiTemp = new CookieUserInfo();
                    cuiTemp.deserialize(userInfo);
                    loginUrl = getWSPLoginUrl (cuiTemp.wsp, loginUrl, wspNameSP);
                }

                ServletUtils.dispatchRequest(request, response, loginUrl);

                return;
            }

            if (LOGOUT_ACTION.equals(action))
            {

                deleteCookie(httpRequest, httpResponse);

                // as session shall be destroyed pull the required parameters (on logout screen) in request itself.
                if (wsp.equals(DomainType.NOBODY))
                    request.setAttribute("wsp", "");
                else
                {
                    request.setAttribute("wsp", wsp);
                    // This SERVER_NAME_BASED_WSP is set in the session from login.jsp
                    // (if it is Common branded GUI and DNS based WSP is obtained from WSP_INFO table)
                    String serverNameBasedWSP = (String)session.getAttribute(SERVER_NAME_BASED_WSP);
                    if (wsp.equals(serverNameBasedWSP))
                        request.setAttribute(SERVER_NAME_BASED_WSP, serverNameBasedWSP);
                }

                ServletUtils.dispatchRequest(request, response, logoutPage);
                invalidateSessions(httpRequest, session);
                return;
            }

            // In this case, there is no other filter in the chain.  Let the
            // request go on through to the indicated destination resource,
            // which may be the ControllerServlet or a JSP.

            chain.doFilter(request, response);

            // If single-signed on is turned on, add the appropriate cookie
            // to the response before dispatching to the destination resource.

            if (singleSignOn)
            {
                forwardCookie(httpRequest, httpResponse);
            }
        }
        catch (Exception e)
        {
            request.setAttribute("javax.servlet.jsp.jspException", e);
            request.setAttribute("javax.servlet.error.message", e.getMessage());
            log.error("doFilter(): Encountered processing error:\n" + e.getMessage());
            Debug.logStackTrace(e);
            throw new ServletException(e);
        }
    }

    private boolean isPasswordChangeRequired(User currentUser, HttpSession session) throws DomainPropException
    {
        boolean passwordChangedRqrd = false;
        String company = currentUser.getCompany();
        Date lastPasswordUpdatedDt = currentUser.getPasswordUpdatedDate();
        Date currentDate = new Date();

        long daysSinceLastPasswordUpdated = (currentDate.getTime() - lastPasswordUpdatedDt.getTime())/MILLSECS_PER_DAY;
        int passwordExpInterval;
        int userPassExpInterval = currentUser.getPasswordResetInterval();
        int domainPassExpInterval = DomainProperties.getInstance(currentUser.getCustomerId()).getPasswordResetInterval();

        boolean isNeustarUser = RBACUserJSPConstants.P_COMPANY_NAME_FOR_NEUSTAR_USERS.equalsIgnoreCase(company);
        boolean isPassExpDisabledForUser = (userPassExpInterval <= RBACDatabaseConstants.RESET_PASSWORD_DISABLED) && (userPassExpInterval != RBACDatabaseConstants.FIRST_TIME_LOGIN);
        boolean isPassExpDisabledForDomain = (domainPassExpInterval <= RBACDatabaseConstants.RESET_PASSWORD_DISABLED);

        // Password Expiration Feature shall be enabled for user when its enabled for both domain & user
        boolean isPassExpEnabledForUser = !isPassExpDisabledForDomain && !isPassExpDisabledForUser;

        boolean hasPasswordExpiredForUser = false;

        if (userPassExpInterval > 0)
            passwordExpInterval = userPassExpInterval;
        else if (domainPassExpInterval > 0)
            passwordExpInterval = domainPassExpInterval;
        else
            passwordExpInterval = DEFAULT_PASSWORD_EXPIRATION_INTERVAL;

        hasPasswordExpiredForUser = daysSinceLastPasswordUpdated >= passwordExpInterval;

        if (hasPasswordExpiredForUser)
        {
            if (isNeustarUser)
            {
                //forwared to Password change screen
                passwordChangedRqrd = true;
            }
            else
            {
                if(isPassExpEnabledForUser)
                {
                    // forwarded to Password change screen.
                    passwordChangedRqrd = true;
                }
            }
        }

        if(isPassExpEnabledForUser || isNeustarUser)
        {
            // For the first login attempt its mandatory to change the password.
            if (currentUser.getPasswordResetInterval() == RBACDatabaseConstants.FIRST_TIME_LOGIN)
                return true;
            session.setAttribute(DAYS_TO_EXPIRE_PASSWORD, String.valueOf(passwordExpInterval - daysSinceLastPasswordUpdated));
        }

        return passwordChangedRqrd;
    }

    /**
     *  handled UnsuccessfulLoginAttemptExceeded Exception
     *
     * @param securityService
     * @param session
     * @param cid
     * @param userName
     * @throws RBACProvisionException
     * @throws FrameworkException
     */
    private void handleULAEException(SecurityService securityService, HttpSession session, String cid, String userName) throws RBACProvisionException, FrameworkException
    {
        ServletContext context = session.getServletContext();
        String webAppName = (String)context.getAttribute(ServletConstants.WEB_APP_NAME);
        Properties initParameters = MultiJVMPropUtils.getInitParameters(context,webAppName);
        String boxIdentifier = initParameters.getProperty(ServletConstants.BOX_IDENTIFIER);
        
        if (boxIdentifier == null || ServletConstants.CH_BOX_IDENTIFIER.equals(boxIdentifier))
        {
            User userToLock = securityService.getUser(userName);
            User[] users = securityService.getUsers();

            for (int i = 0; i < users.length; i++ )
            {
                if ( userToLock.getUserId() == users[i].getUserId() )
                {
                    users[i].setLocked( 1 );
                    users[i].setFailedLoginAttemptCounter(StoreProviderBase.MAX_UNSUCCESSFUL_LOGIN_ATTEMPT);
                }
            }

            securityService.updateFailedLoginAttemptCounter(userName, StoreProviderBase.MAX_UNSUCCESSFUL_LOGIN_ATTEMPT);
        }
        else
        {
            // Set customer id in context to get the customer specific SSO URL, if found.
            CustomerContext.getInstance().setCustomerID(cid);

            String ssoURLProperty = TagUtils.getSSOURL(context, boxIdentifier, ServletConstants.CH_BOX_IDENTIFIER);

            // clean-up the context as this is not valid login flow.
            CustomerContext.getInstance().cleanup();

            String ssoURL = ssoURLProperty + LockUserServlet.LOCK_USER_SERVLET_SUFFIX;

            callCentralLockingServlet(ssoURL, cid, userName, StoreProviderBase.MAX_UNSUCCESSFUL_LOGIN_ATTEMPT);
        }
    }

    private void callCentralLockingServlet(String lockingServletURL, String domainName, String userName, int failedLoginAttempt)
    {
        URL servlet;
        try {
            servlet = new URL(lockingServletURL);

            URLConnection conn;
            conn = servlet.openConnection();
            conn.addRequestProperty(LockUserServlet.DOMAIN_NAME_PROP, domainName);
            conn.addRequestProperty(LockUserServlet.USER_NAME_PROP, userName);
            conn.addRequestProperty(LockUserServlet.FAILURE_LOGIN_ATTEMPT_PROP, String.valueOf(failedLoginAttempt));
            conn.setDoOutput(true);

            // dummy content passed, as we need to send header props and Servlet expects some input.
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            StringWriter sw = new StringWriter();
            sw.append("dummy content!!");
            out.write(sw.toString());

            try
            {
                InputStreamReader isr = new InputStreamReader(conn.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String str = new String();
                str = br.readLine();
                isr.close();
            }
            catch (IOException ex) {
                // ignore any error in the response.
                log.warn("doFilter().callCentralLockingServlet(): ignoring error in dummy response: \n" + ex.getMessage());
            }
            out.close();
        } catch (MalformedURLException ex) {
            log.error("doFilter().callCentralLockingServlet(): Encountered processing error:\n" + ex.getMessage());
            Debug.logStackTrace(ex);
        } catch (IOException ex) {
            log.error("doFilter().callCentralLockingServlet(): Encountered processing error:\n" + ex.getMessage());
            Debug.logStackTrace(ex);
        }
    }

    private String getWSPLoginUrl(String wsp, String defaulUrl, String sysPropWSP) throws DomainTypeException
    {
        String url = defaulUrl;
        if (StringUtils.hasValue(wsp) && !StringUtils.hasValue(sysPropWSP))
        {
            if (!wsp.equals(DomainType.NOBODY))
                url = "/" + wsp + "/login.jsp";
        }
        return url;
    }

    /**
     * This method creates a session bean and set it in the session object.
     *
     * @param cid The customer id
     * @param  userName  User name.
     * @param  password  Password.
     * @param  wsp       Wholesale provider.
     * @param  session   HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void setSessionBean(String cid, String userName, String password, String wsp, HttpSession session) throws ServletException
    {
        try
        {
            long startTime = System.currentTimeMillis();
    	    SessionInfoBean sBean = new SessionInfoBean(cid, userName, password,wsp,startTime);
            String loginAction;
            session.setAttribute(SESSION_BEAN, sBean);
            if ( MetricsAgent.isOn( MetricsAgent.SYNC_API_CATEGORY ) )
            {
                    // Set the userName and CustomerId in customerContext for logging.
                    CustomerContext.getInstance().setCustomerID(cid);
                    CustomerContext.getInstance().setUserID(userName);
                    if(loginType.equalsIgnoreCase(SINGLE_SIGN_ON_LOGIN_ACTION))
                        loginAction = ServletConstants.SINGLE_SIGN_ON_LOGIN_ACTION;
                    else
                        loginAction = ServletConstants.LOGIN_ACTION;
                    MetricsAgent.logSyncAPI(startTime, loginAction + "=[customerid=" + cid
                                             + ";user=" + userName + ";sessionId=" + session.getId() + "]");
                    MetricsAgent.logSyncAPIInDB(loginAction , session.getId());
            }
        }
        catch (Exception e)
        {
            String errorMessage = "Failed to create a session bean and set it in the session objec: " + e.getMessage();

            log.error("setSessionBean(): " + errorMessage);

            throw new ServletException(errorMessage, e);
        }
    }

    /**
     * This method adds the necessary user-info cookie to the response object to
     * maintain information across web applications.
     *
     * @param  request   HttpServletRequest object.
     * @param  response  HttpServletResponse object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  Added cookie.
     */
    private Cookie addCookie(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        Cookie[] cookies = request.getCookies();

        // user info cookie
        Cookie cookie = null;

        SessionInfoBean sessionBean = (SessionInfoBean)request.getSession().getAttribute(SESSION_BEAN);

        CookieUserInfo cui = new CookieUserInfo(sessionBean, request);

        if (cookies != null && cookies.length > 0)
        {
            // Replace all user info cookies with new cui
            for (int i=0; i<cookies.length; i++)
            {
                String cookieName = cookies[i].getName();

                if (cookieName.equals(USER_INFO_COOKIE))
                {
                    // The last user info cookie will be forward to response.
                    cookie = cookies[i];

                    if (log.isDebugDataEnabled())
                        log.debugData("addCookie(): OLD cookie [" + describeCookie(cookie) + "].");

                    //update the cookie's value
                    try {
                        cookie.setValue( URLEncoder.encode(cui.serialize(), "UTF-8") );
                    } catch (UnsupportedEncodingException e) {
                        log.warn("doFilter(): Error occurred while encoding user info cookie." );
                    }

                    // IE does not send path with cookie in request.
                    // Therefore we always need to reset the path in response
                    // so that it match the first cookie initially created.
                    cookie.setPath("/");
                    //cookie.setMaxAge(1800);

                    if (log.isDebugDataEnabled())
                        log.debugData("addCookie(): NEW value set for cookie [" + describeCookie(cookie) + "].");
                }
            }
        }

        if (cookie == null)
        {
            //create a new one and set the path
            try {
                cookie             = new Cookie(USER_INFO_COOKIE, URLEncoder.encode(cui.serialize(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.error("addCookie(): exception occurred while creating cookie from encoded string. Hence using old uncoded format.");
                cookie             = new Cookie(USER_INFO_COOKIE, cui.serialize());
            }

            // Set path of cookie to root so that it will be sent across the webapps.
            cookie.setPath("/");
            //cookie.setMaxAge(1800);

            if (log.isDebugDataEnabled())
                log.debugData("addCookie(): Created cookie [" + describeCookie(cookie) + "].");

        }


        response.addCookie(cookie);

        return cookie;
    }

    /**
     * This method forwards the necessary user-info cookie to the response object to
     * maintain information across web applications.
     *
     * @param  request   HttpServletRequest object.
     * @param  response  HttpServletResponse object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  Forwarded cookie.
     */
    private Cookie forwardCookie(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        Cookie cookie = ServletUtils.getCookie(request, USER_INFO_COOKIE);

        // IE does not send path with cookie in request.
        // Therefire we always need to reset the path in response
        // so that it match the first cookie initially created.
		if(cookie != null)
		{
			cookie.setPath("/");

			response.addCookie(cookie);
		}
        return cookie;
    }

    /**
     * This method set the age of user-info cookie to 0 to the response object to
     * notify the browser to delete the cookie.
     *
     * @param  request   HttpServletRequest object.
     * @param  response  HttpServletResponse object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  Forwarded cookie.
     */
    private Cookie deleteCookie(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        Cookie[] cookies = request.getCookies();

        // user info cookie
        Cookie cookie = null;

        SessionInfoBean sessionBean = (SessionInfoBean)request.getSession().getAttribute(SESSION_BEAN);

        if (cookies != null && cookies.length > 0)
        {
            // Delete all user info cookies by set maxAge to 0.
            for (int i=0; i<cookies.length; i++)
            {
                String cookieName = cookies[i].getName();

                if (cookieName.equals(USER_INFO_COOKIE))
                {
                    // All user info cookie will be forward to response.
                    cookie = cookies[i];

                    // IE does not send path with cookie in request.
                    // Therefire we always need to reset the path in response
                    // so that it match the first cookie initially created.
                    cookie.setPath("/");

                    if (log.isDebugDataEnabled())
                        log.debugData("deleteCookie(): Removing cookie [" + describeCookie(cookie) + "].");
                    // Let the browser to remove the cookie.
                    cookie.setMaxAge(0);

                    response.addCookie(cookie);

                }
            }
        }

        return cookie;
    }

    /**
     * This method updates the authenticated-users cache as appropriate.
     *
     * @param  cookie   Cookie object.
     * @param  session  HttpSession object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    private void updateAuthenticatedUserLookup(Cookie cookie, HttpSession session) throws ServletException
    {
        if (log.isDebugEnabled())
        {
            log.debug("updateAuthenticatedUserLookup(): Updating authenticated-user lookup cache ...");
        }

        String userInfo = cookie.getValue();
        try {
            userInfo = URLDecoder.decode(userInfo, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("doFilter(): Error occurred while decoding user info cookie." );
        }

        List   sessions = (List)authenticatedUserLookup.get(userInfo);

        if (sessions == null)
        {
            sessions = new Vector();

            authenticatedUserLookup.put(userInfo, sessions);
        }

        if (!sessions.contains(session))
        {
            sessions.add(session);

            userInfoLookup.put(session.getId(), cookie);
        }

        debugAuthenticatedUserLookup(cookie);
    }

    /**
     * This method determines whether the user needs to be authenticated.
     *
     * @param  request  HttpServletRequest object.
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     *
     * @return  true if authentication is required, false otherwise.
     */
    private boolean authenticationRequired(HttpServletRequest request) throws ServletException
    {
        HttpSession session     = request.getSession();

        SessionInfoBean     sessionBean = (SessionInfoBean)session.getAttribute(SESSION_BEAN);

        if (sessionBean == null)
        {
            if (singleSignOn)
            {
                Cookie cookie = ServletUtils.getCookie(request, USER_INFO_COOKIE);

                if (cookie == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("authenticationRequired(): With single sign-on enabled, this is a brand new user.  Authentication is required.");
                    }

                    return true;
                }

                String userInfo = cookie.getValue();
                try {
                    userInfo = URLDecoder.decode(userInfo, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("doFilter(): Error occurred while decoding user info cookie." );
                }

                List   sessions = (List)authenticatedUserLookup.get(userInfo);

                if (sessions == null)
                {
                    log.debug("authenticationRequired(): With single sign-on enabled, session-list lookup with user-info [" + getCookieWithoutPassword( cookie.getName(), userInfo ) + "] returns nothing.  Authentication will be required.");

                    return true;
                }

                CookieUserInfo cui = new CookieUserInfo();
                cui.deserialize(userInfo);
                loginType = SINGLE_SIGN_ON_LOGIN_ACTION;
                setSessionBean(cui.cid, cui.user, cui.password, cui.wsp, session);

                updateAuthenticatedUserLookup(cookie, session);

                if (log.isDebugEnabled())
                {
                    log.debug("authenticationRequired(): With single sign-on enabled, CID [" + cui.cid +"], user [" + cui.user + "] has already been authenticated. Authentication is not required.");
                }

                return false;
            }

            if (log.isDebugEnabled())
            {
                log.debug("authenticationRequired(): With single sign-on disabled, this is a brand new user.  Authentication is required.");
            }

            return true;
        }

        if (log.isDebugEnabled())
        {
            String userName = sessionBean.getUserId();
            String cid = sessionBean.getCustomerId();

            log.debug("authenticationRequired(): CID [" + cid +"], User [" + userName + "] has already been authenticated prior.");
        }

        return false;
    }

    /**
     * This method invalidates a user's session(s).
     *
     * @param  request  HttpServletRequest object.
     * @param  session  HttpSession object.
     */
    private void invalidateSessions(HttpServletRequest request, HttpSession session)
    {
        if (singleSignOn)
        {
            Cookie cookie   = ServletUtils.getCookie(request, USER_INFO_COOKIE);
			
			if ( cookie != null)
			{
				String userInfo = cookie.getValue();
                try {
                    userInfo = URLDecoder.decode(userInfo, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("doFilter(): Error occurred while decoding user info cookie." );
                }

				List   sessions = (List)authenticatedUserLookup.get(userInfo);

				if (log.isDebugDataEnabled())
					log.debugData("invalidateSessions(): Cleaning up session for cookie [" + USER_INFO_COOKIE + "] with value [" + getCookieWithoutPassword( cookie.getName(), userInfo ) + "]...");

				authenticatedUserLookup.remove(userInfo);

				if (sessions == null)
				{
					log.error("invalidateSessions(): With single sign-on enabled, session-list lookup with user-info [" + getCookieWithoutPassword( cookie.getName(), userInfo ) + "] returns an empty session list, which should never occur.  No invalidation will be performed.");

					return;
				}

				synchronized (sessions)
				{
					Iterator iterator = sessions.iterator();

					while (iterator.hasNext())
					{
						HttpSession eachSession = (HttpSession)iterator.next();

						try
						{
							eachSession.invalidate();
						}
						catch (IllegalStateException e)
						{
							log.warn("invalidateSessions():  Trying to invalidate a session that has already been invalidated.  Skipping ...");
						}
					}
				}

				debugAuthenticatedUserLookup(cookie);
			}
        }
        else
        {
            session.invalidate();
        }
    }

    /**
     * This is a callback method that gets called when a session has been invalidated.
     * This allows for cached information to get cleaned up.
     *
     * @param  session  HttpSession object, which has been invalidated.
     */
    public static final void sessionDestroyed(HttpSession session)
    {
        String sessionId = session.getId();

        Cookie cookie    = (Cookie)userInfoLookup.get(sessionId);

        if (cookie != null)
        {
            String userInfo = cookie.getValue();

            try {
                userInfo = URLDecoder.decode(userInfo, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Debug.log(Debug.ALL_WARNINGS,"sessionDestroyed(): Error occurred while decoding user info cookie." );
            }


            if (userInfo != null)
            {
                List sessions = (List)authenticatedUserLookup.get(userInfo);

                if (sessions != null)
                {
                    synchronized (sessions)
                    {
                        if (sessions != null)
                        {
                            Iterator iterator = sessions.iterator();

                            while (iterator.hasNext())
                            {
                                HttpSession candidate = (HttpSession)iterator.next();

                                if (sessionId.equals(candidate.getId()))
                                {
                                    iterator.remove();
                                }
                            }

                            userInfoLookup.remove(sessionId);

                            if (sessions.size() == 0)
                            {
                                authenticatedUserLookup.remove(userInfo);
                            }
                        }
                    }
                }
            }
        }
        Debug.log(Debug.BENCHMARK,"Total Authenticated Login Count = "
                  + authenticatedUserLookup.size());
    }

    /**
     * Implementation of Filter's destroy().  This is where any cleanup tasks
     * get perform before this filter gets taken out of service.
     */
    public void destroy()
    {
        if (log.isDebugEnabled())
        {
            log.debug("destroy(): Performing any filter cleanup tasks ...");
        }
    }

    /**
     * A convenient method that describes the content of the cookies in the request.
     *
     * @param request  HTTP request.
     */
    private void debugRequestCookies(HttpServletRequest request)
    {
        if (log.isDebugEnabled())
        {
            Cookie[] cookies = request.getCookies();

            if (cookies == null || cookies.length == 0)
            {
                log.debug("debugRequestCookies(): No request cookies.");
                return;
            }
            for (int i=0; i<cookies.length; i++)
            {
                log.debug("debugRequestCookies(): Cookie [" + i + "] -> [" + describeCookie(cookies[i]) + "].");
            }
        }

    }

    private String describeCookie(Cookie cookie)
    {
        String cookieValue = cookie.getValue();
        try {
            cookieValue = URLDecoder.decode(cookieValue,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("describeCookie(): Error occurred while decoding user info cookie." );
        }

        return cookie.getName() + "],["+ getCookieWithoutPassword( cookie.getName(), cookieValue ) + "],["+ cookie.getPath() + "],["+ cookie.getDomain();
    }

    /**
     * A convenient method that describes the content of the authenticated-users
     * cache.  This is used for debugging purposes.
     *
     * @param  cookie  Cookie to use in looking up the cache.
     */
    private void debugAuthenticatedUserLookup(Cookie cookie)
    {
        if (log.isDebugEnabled())
        {
            String userInfo = cookie.getValue();

            try {
                userInfo = URLDecoder.decode(userInfo, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.warn("debugAuthenticatedUserLookup(): Error occurred while decoding user info cookie." );
            }

            log.debug("debugAuthenticatedUserLookup(): Looking up session list with user-info [" + getCookieWithoutPassword ( cookie.getName(), userInfo ) + "] ...");

            List sessions = (List)authenticatedUserLookup.get(userInfo);

            if (sessions != null)
            {
                StringBuffer debugMessage = new StringBuffer();

                debugMessage.append("debugAuthenticatedUserLookup(): There are [").append(sessions.size()).append("] sessions with ids [");

                Iterator iterator = sessions.iterator();

                while (iterator.hasNext())
                {
                    HttpSession session = (HttpSession)iterator.next();

                    debugMessage.append(session.getId()).append(" ");
                }

                debugMessage.append("].");

                log.debug(debugMessage.toString());
            }
            else
            {
                log.debug("debugAuthenticatedUserLookup(): There are currently 0 session mapped to user-info [" + getCookieWithoutPassword( cookie.getName(), userInfo ) + "].");
            }
        }
    }

    /**
     * Return cookie value without password if it is user info cookie
     *
     * @param cookieName cookie's name
     * @param cookieValue cookie's value
     * @return String value without password, or original value if its not user info cookie
     */
    private String getCookieWithoutPassword (String cookieName, String cookieValue)
    {
        if ( !StringUtils.hasValue(cookieName) || !USER_INFO_COOKIE.equals(cookieName) )
            return cookieValue;

        String cookieValueWithoutPassword = cookieValue;

        try
        {
            CookieUserInfo tempUIC = new CookieUserInfo();
            tempUIC.deserialize(cookieValue);
            cookieValueWithoutPassword = tempUIC.serializeWithoutPassword();
        }
        catch (Exception e)
        {
            // ignore any exception in parsing cookie.
        }

        return cookieValueWithoutPassword;
    }


    private class CookieUserInfo
    {
        private final static String sep = ":";

        public String cid;
        public String user;
        public String password;
        public String ip;
        public String wsp;


        public CookieUserInfo(SessionInfoBean sessionBean, HttpServletRequest request)
        {

            this.cid      = sessionBean.getCustomerId();
            this.user     = sessionBean.getUserId();
            this.password = sessionBean.getPassword();
            this.ip       = request.getRemoteAddr();
            this.wsp      = sessionBean.getWsp();

        }

        public CookieUserInfo()
        {
        }


        public String serialize()
        {
            return new StringBuffer(cid).append(sep).append(user).append(sep).append(password).append(sep).append(ip).append(sep).append(wsp).toString();
        }

        public String serializeWithoutPassword()
        {
            return new StringBuffer(cid).append(sep).append(user).append(sep).append(ip).append(sep).append(wsp).toString();
        }

        public void deserialize(String serializedStr)
        {
            StringTokenizer toker = new StringTokenizer(serializedStr,sep);

            cid      = toker.nextToken();
            user     = toker.nextToken();
            password = toker.nextToken();
            ip       = toker.nextToken();
            wsp      = toker.nextToken();

        }

        public void describe ()
        {
            log.debug("CookieUserInfo: "
                        + "cid [" + cid + "]"
                        + ", user [" + user + "]"
                        + ", ip [" + ip + "]"
                        + ", wsp [" + wsp + "]"
                        );
        }
    }
}

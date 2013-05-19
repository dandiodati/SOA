package com.nightfire.webgui.core.josso.gateway.signon;

import org.josso.gateway.signon.LoginAction;
import org.josso.gateway.SSOGateway;
import org.josso.gateway.SSOContext;
import org.josso.gateway.SSOWebConfiguration;
import org.josso.gateway.assertion.AuthenticationAssertion;
import org.josso.gateway.session.SSOSession;
import org.josso.auth.Credential;
import org.josso.auth.exceptions.SSOAuthenticationException;
import org.josso.auth.exceptions.AuthenticationFailureException;
import org.josso.Lookup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

import  com.nightfire.framework.util.*;

/**

 * N* specific login action used with cid, username and password authentication scheme.

 * This is and action class that is configured in struts-signon.xml

 */

public class NSLoginAction extends LoginAction {

    private static final Log logger = LogFactory.getLog(NSLoginAction.class);

    /**

     * Request parameter containing username.

     * Value : sso_username

     */

    public static final String PARAM_JOSSO_USERNAME="josso_username";



    /**

     * Request parameter containing user password.

     * Value : sso_password

     */

    public static final String PARAM_JOSSO_PASSWORD="josso_password";
    /**
        * Request parameter containing CID.
        * Value : cid
     */

     public static final String PARAM_JOSSO_CID="josso_cid";
        
    /**

     * Creates credentials for cid , username and password, using configuration.

     */

     public static final String LOGIN_ACTION ="LOGIN";
     public static final String SSO_ACTION ="SSO";

    protected Credential[] getCredentials(HttpServletRequest request) throws SSOAuthenticationException {


        SSOGateway g = getSSOGateway();
        Credential username = g.newCredential("basic-authentication", "username", request.getParameter(PARAM_JOSSO_USERNAME));
        Credential password = g.newCredential("basic-authentication", "password", request.getParameter(PARAM_JOSSO_PASSWORD));
        Credential cid = g.newCredential("basic-authentication", "domain", request.getParameter(PARAM_JOSSO_CID));
        Credential[] c = {username, password, cid};

        return c;


    }

       protected ActionForward login(ActionMapping mapping,

                                  ActionForm form,

                                  HttpServletRequest request,

                                  HttpServletResponse response) {


        Credential[] c = null;
        try {



            SSOContext ctx = getNewSSOContext(request);

            c = getCredentials(request);

            SSOGateway g = getSSOGateway();





            SSOWebConfiguration cfg = Lookup.getInstance().lookupSSOWebConfiguration();



            try {



                storeSsoParameters(request);



                HttpSession httpSession = request.getSession();



                // 1 - Handle Outbound relaying by generating an assertion for the authentication request

                SSOSession session = null;



                AuthenticationAssertion authAssertion = g.assertIdentity(c, ctx.getScheme(), ctx);

                session = authAssertion.getSSOSession();

                Cookie ssoCookie = newJossoCookie(request.getContextPath(), session.getId());

                response.addCookie(ssoCookie);



                if (logger.isDebugEnabled())

                    logger.debug("[login()], authentication successfull.");

               //N* specific  Logging 'Login Action' in DB and logfile

                   logSynchAPIDB(LOGIN_ACTION ,session);



                // 2 - Restore BACK TO URL ...

                String back_to = (String) httpSession.getAttribute(KEY_JOSSO_BACK_TO);

                if (back_to == null) {

                    logger.debug("[login()], No 'BACK TO' URL found in session, using configured URL : " + cfg.getLoginBackToURL());

                    back_to = cfg.getLoginBackToURL();

                }



                if (back_to == null) {



                    // No back to URL received or configured ... use configured success page.



                    logger.warn("No 'BACK TO' URL received or configured ... using default forward rule !");



                    String username = session.getUsername();

                    ActionErrors msg = new ActionErrors();

                    msg.add(ActionErrors.GLOBAL_ERROR, new ActionError("sso.login.success", username));

                    msg.add(ActionErrors.GLOBAL_ERROR, new ActionError("sso.info.session", session.getId()));

                    saveErrors(request, msg);



                    // Return to controller.

                    return mapping.findForward("login-result");

                }



                // 3 - Redirect the user to the propper page

                String backToURLWithAssertion  = back_to + (back_to.indexOf("?") >= 0 ? "&" : "?" ) + "josso_assertion_id=" + authAssertion.getId();



                httpSession.setAttribute(KEY_JOSSO_BACK_TO, backToURLWithAssertion);

                back_to = backToURLWithAssertion;





                // Remove this attributes once used

                httpSession.removeAttribute(KEY_JOSSO_BACK_TO);

                httpSession.removeAttribute(KEY_JOSSO_ON_ERROR);



                // We're going back to the partner app.

                if (logger.isDebugEnabled())

                    logger.debug("[login()], Redirecting user to : " + back_to);





                response.sendRedirect(response.encodeRedirectURL(back_to));



                return null; // No forward is needed, we perfomed a 'sendRedirect'.



            } catch (AuthenticationFailureException e) {
             // N* Specific customization
             // Earlier bahavior was to show the "invalid-login" message
             // Now, user would be redirected to the invalid-login jsp
                if (logger.isDebugEnabled())
                {
                   logger.debug(e.getMessage(), e);
                   logger.debug("Invalid customerid or username : redirecting to josso/signon/invalidlogin.jsp");
                }

                    return mapping.findForward("invalid-login");
         }



        } catch (Exception e) {



            // Fatal error ...

            logger.error(e.getMessage(), e);

            ActionErrors errors = new ActionErrors();

            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("sso.error", e.getMessage() != null ? e.getMessage() : e.toString()));

            saveErrors(request, errors);

            return mapping.findForward("error");

        }



    }



    /**
     * Relay using a previously opened and valid SSO session.
     *
     */
    protected ActionForward relay(ActionMapping mapping,
                                  ActionForm form,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {


        try {
          
            SSOGateway g = getSSOGateway();
            storeSsoParameters(request);

            SSOWebConfiguration cfg = Lookup.getInstance().lookupSSOWebConfiguration();
            HttpSession httpSession = request.getSession();

            // Recover session and create a new assertion.
            String sessionId = getJossoSessionId(request);
            SSOSession session = g.findSession(sessionId);
            AuthenticationAssertion authAssertion = g.assertIdentity(sessionId);


            if (logger.isDebugEnabled())
                logger.debug("[relay()], authentication successfull.");

            // N* specific Logging 'SSO Action' in DB and logfile

                       logSynchAPIDB(SSO_ACTION ,session);
                     

            // 2 - Restore BACK TO URL ...
            String back_to = (String) httpSession.getAttribute(KEY_JOSSO_BACK_TO);
            if (back_to == null) {
                logger.debug("[relay()], No 'BACK TO' URL found in session, using configured URL : " + cfg.getLoginBackToURL());
                back_to = cfg.getLoginBackToURL();
            }

            if (back_to == null) {

                // No back to URL received or configured ... use configured success page.

                logger.warn("No 'BACK TO' URL received or configured ... using default forward rule !");

                String username = session.getUsername();
                ActionErrors msg = new ActionErrors();
                msg.add(ActionErrors.GLOBAL_ERROR, new ActionError("sso.login.success", username));
                msg.add(ActionErrors.GLOBAL_ERROR, new ActionError("sso.info.session", session.getId()));
                saveErrors(request, msg);

                // Return to controller.
                return mapping.findForward("login-result");
            }

            // 3 - Redirect the user to the propper page
            String backToURLWithAssertion  = back_to + (back_to.indexOf("?") >= 0 ? "&" : "?" ) + "josso_assertion_id=" + authAssertion.getId();

            httpSession.setAttribute(KEY_JOSSO_BACK_TO, backToURLWithAssertion);
            back_to = backToURLWithAssertion;


            // Remove this attributes once used
            httpSession.removeAttribute(KEY_JOSSO_BACK_TO);
            httpSession.removeAttribute(KEY_JOSSO_ON_ERROR);

            // We're going back to the partner app.
            if (logger.isDebugEnabled())
                logger.debug("[relay()], Redirecting user to : " + back_to);

            response.sendRedirect(response.encodeRedirectURL(back_to));

            return null; // No forward is needed, we perfomed a 'sendRedirect'.

        } catch (Exception e) {

            // Fatal error ...
            logger.error(e.getMessage(), e);
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("sso.error", e.getMessage() != null ? e.getMessage() : e.toString()));
            saveErrors(request, errors);
            return mapping.findForward("error");
        }
    }


   // API for Logging 'login/sso' action into DB or logfile

    public void logSynchAPIDB(String action,SSOSession session){

         long startTime = System.currentTimeMillis();

               try{
                    CustomerContext.getInstance().setCustomerID(session.getCustomerID());

                    CustomerContext.getInstance().setUserID(session.getUsername());

                    if ( MetricsAgent.isOn( MetricsAgent.SYNC_API_CATEGORY ) )
                      {
                          MetricsAgent.logSyncAPI(startTime, action + "=[customerid=" + session.getCustomerID()
                                             + ";user=" + session.getUsername() + ";sessionId=" + session.getId() + "]");
                          MetricsAgent.logSyncAPIInDB( action, session.getId());

                      }

                    if (logger.isDebugEnabled())

                                    logger.debug(action + ": action  Successfully logged by 'logSyncAPI' and 'logSyncAPIInDB'.");


                  }catch(Exception e) {

                       if (logger.isDebugEnabled())

                             logger.debug(action + ":action  failed while invoking 'logSyncAPI' and 'logSyncAPIInDB'.");

                   }

    }


}

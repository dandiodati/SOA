package com.nightfire.webgui.gateway.servicemgr;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.security.SecurityService;
import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.beans.SessionInfoBean;

/**
 * Acts as Front Controller for Service Manger Admin UI.
 */
public class ServiceManagerController extends HttpServlet
{
    private static final String UNAUTHENTICATED_USER_PAGE = "/pages/common/not-authorized.jsp";

    @Override
    public void init() throws ServletException
    {
        super.init();
        String realPath = getServletContext().getRealPath("/");
        ViewDispatcher.setRealPath(realPath);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException 
    {
        doGet(req, resp);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException 
    {
        process(req,resp);
    } 

    /**
     * process the request and displays the response. Common point where are the request are received 
     * and processed.
     * @param req HttpServletRequest.
     * @param resp HttpServletResponse
     * @throws ServletException on Error
     * @throws IOException on Error
     */
    private void process(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        // apply authentication check 
        if(! validatePermissions(req))
        {
            if(Debug.isLevelEnabled(Debug.NORMAL_STATUS)) 
                Debug.log(Debug.NORMAL_STATUS,"Validation Failed. Redirecting to invalid user page.");
            
            req.setAttribute("ERROR_MESSAGE", "Only users with CHAdmin role are permitted. Please login again.");
            RequestDispatcher dispatcher = req.getRequestDispatcher(UNAUTHENTICATED_USER_PAGE);
            dispatcher.forward(req, resp);
            return; 
        }
        // converts the http request params to map
        // context object created in form of MAP.
        HashMap<String,String> parameters = new HashMap<String,String>(req.getParameterMap());

        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Inside process of controller servlet params: "+parameters);

        // get a new Application Controller.
        ServiceMangerAppController appController = new ServiceMangerAppController();
        try 
        {
            // process the app controller by passing all the parameters and 
            // writer object where response has to be printed.
            appController.process(parameters, resp.getWriter());
        }
        catch (ProcessingException e) 
        {
            Debug.error("Unable to process reason["+e.getMessage()+"].");
            throw new ServletException(e);
        }

        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS, "Exiting controller servlet process...");
    }

    /**
     * returns true if the current login user is any of CH admin or Domain admin
     * permissions, otherwise returns false. 
     * @param request request of type HttpServletRequest.
     * @return boolean.
     */
    private boolean validatePermissions(HttpServletRequest request)
    {
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS,"inside ServiceManagerController.validatePermissions ..");

        try
        {
            // get the session info bean and get cutomerId and user id
            HttpSession session = request.getSession(true);
            SessionInfoBean sessionBean = (SessionInfoBean)session.getAttribute(ServletConstants.SESSION_BEAN);
            if( sessionBean != null )
            {
                String customerId = sessionBean.getCustomerId();
                String userID = sessionBean.getUserId();
                SecurityService securityService = SecurityService.getInstance(customerId);

                if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                    Debug.log(Debug.NORMAL_STATUS,"ServiceManagerController.validatePermissions for customerID["+customerId+"], userID["+userID+"]");

                if(securityService != null && securityService.isCHAdmin(userID))
                {
                    if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                        Debug.log(Debug.NORMAL_STATUS,"ServiceManagerController.validatePermissions returning [true]");

                    return true;
                }
            }
        }
        catch(SecurityException se)
        {
            Debug.log(Debug.ALL_WARNINGS,"Unable to validate permisssions, reason["+se.getMessage()+"]");
        }
        catch(FrameworkException fe)
        {
            Debug.log(Debug.ALL_WARNINGS,"Unable to validate permisssions, reason["+fe.getMessage()+"]");
        }
        
        if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
            Debug.log(Debug.NORMAL_STATUS,"inside ServiceManagerController.validatePermissions returning [false]");

        return false;
    }
}

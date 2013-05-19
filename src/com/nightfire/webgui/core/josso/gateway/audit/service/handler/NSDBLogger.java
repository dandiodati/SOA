package com.nightfire.webgui.core.josso.gateway.audit.service.handler;

import org.josso.gateway.audit.service.handler.BaseAuditTrailHandler;
import org.josso.gateway.audit.SSOAuditTrail;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.josso.gateway.session.service.BaseSession;

import java.util.Properties;

import com.nightfire.framework.util.MetricsAgent;
import com.nightfire.framework.util.CustomerContext;

/**

 * N* specific audit trail handler sends all received trails to the DB.

 */



public class NSDBLogger extends BaseAuditTrailHandler {


    public static final String LOGOUT_ACTION = "LOGOUT";
    private Log logger = LogFactory.getLog(NSDBLogger.class);

    public int handle(SSOAuditTrail trail) {

        long startTime = System.currentTimeMillis();

        Properties properties = trail.getProperties();

        String username =  trail.getSubject();

        String sessionid = properties.getProperty("ssoSessionId");

        String cid =  properties.getProperty("CustomerId");


                if(BaseSession.SESSION_DESTROYED_EVENT.equals(trail.getAction()))
                    if ( MetricsAgent.isOn( MetricsAgent.SYNC_API_CATEGORY ) )
                       {
                         try{
                            CustomerContext.getInstance().setCustomerID(cid);
                            CustomerContext.getInstance().setUserID(username);
                            MetricsAgent.logSyncAPI(startTime, LOGOUT_ACTION  + "=[customerid=" + cid + ";user="
                                                            + username + ";sessionId=" + sessionid + "]");
                            MetricsAgent.logSyncAPIInDB(LOGOUT_ACTION , sessionid);
                            if (logger.isDebugEnabled())

                                    logger.debug(" NSDBLogger(AuditTrailHandler)-handle() :"+LOGOUT_ACTION  + ": action  Successfully logged by 'logSyncAPI' and 'logSyncAPIInDB'.");


                            }catch(Exception e) {

                                 if (logger.isDebugEnabled())

                                         logger.debug(" NSDBLogger(AuditTrailHandler)-handle() :"+LOGOUT_ACTION  + ":action  failed while invoking 'logSyncAPI' and 'logSyncAPIInDB'.");

                             }
                       }

            

          return CONTINUE_PROCESS;
     }
}

package com.nightfire.webgui.core.josso.gateway.session.service;

import org.josso.gateway.session.service.*;
import org.josso.gateway.session.service.store.SessionStore;
import org.josso.gateway.session.exceptions.SSOSessionException;
import org.josso.gateway.session.exceptions.TooManyOpenSessionsException;
import org.josso.gateway.session.exceptions.NoSuchSessionException;
import org.josso.gateway.session.SSOSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import com.nightfire.webgui.core.josso.gateway.session.service.store.NSSessionStore;

/**

 * N* specific implementation of the SSOSessionManager.

 * This is responsible to initiate new session on base of cid and username

 */



public class NSSessionManagerImpl implements SSOSessionManager {



    private static final Log logger = LogFactory.getLog(NSSessionManagerImpl.class);



    // Max inactive interval used for new sessions. Default is set to 30

    private int _maxInactiveInterval = 30;



    private int _maxSessionsPerUser = 1;



    private long _sessionMonitorInterval = 5000;



    private boolean _invalidateExceedingSessions = false;



    /**

     * This implementation uses a MemoryStore and a defaylt Session Id generator.

     */

    public NSSessionManagerImpl() {

    }



    //-----------------------------------------------------

    // Instance variables :

    //-----------------------------------------------------



    private SessionStore _store;

    private SessionIdGenerator _idGen;

    private NSSessionManagerImpl.SessionMonitor _monitor;



    //------------------------------------------------------

    // SSO Session Manager

    //------------------------------------------------------



    /**

     * Initializes the manager.

     */

    public synchronized void initialize() {



        logger.info("[initialize()] : IdGenerator.................=" + _idGen.getClass().getName());

        logger.info("[initialize()] : Store.......................=" + _idGen.getClass().getName());

        logger.info("[initialize()] : MaxInactive.................=" + _maxInactiveInterval);

        logger.info("[initialize()] : MaxSessionsPerUser..........=" + _maxSessionsPerUser);

        logger.info("[initialize()] : InvalidateExceedingSessions.=" + _invalidateExceedingSessions);

        logger.info("[initialize()] : SesisonMonitorInteval.......=" + _sessionMonitorInterval);



        // Start session monitor.

        _monitor = new NSSessionManagerImpl.SessionMonitor(this, getSessionMonitorInterval());

        Thread t = new Thread(_monitor);

        t.setDaemon(true);

        t.setName("JOSSOSessionMonitor");

        t.start();



    }





    /**

     * Initiates a new session. The new session id is returned.

     *

     * @return the new session identifier.

     */

    public String initiateSession(String username) throws SSOSessionException {



        // Invalidate sessions if necessary

        BaseSession sessions[] = _store.loadByUsername(username);



        // Check if we can open a new session for this user.

        if (!_invalidateExceedingSessions &&

                _maxSessionsPerUser != -1 &&

                _maxSessionsPerUser <= sessions.length) {

            throw new TooManyOpenSessionsException(sessions.length);

        }



        // Check if sessions should be auto-invalidated.

        if (_invalidateExceedingSessions && _maxSessionsPerUser != -1) {



            // Number of sessions to invalidate

            int invalidate = sessions.length - _maxSessionsPerUser + 1;

            if (logger.isDebugEnabled())

                logger.debug("Auto-invalidating " + invalidate + " sessions for user : " + username);



            for (int idx = 0; invalidate > 0; invalidate --) {

                BaseSession session = sessions[idx];



                if (logger.isDebugEnabled())

                    logger.debug("Auto-invalidating " + session.getId() + " session for user : " + username);



                invalidate(session.getId());

            }

        }



        // Build the new session.

        BaseSession session = doMakeNewSession();



        // Configure the new session ...
        session.setId(_idGen.generateId());

        session.setCreationTime(System.currentTimeMillis());

        session.setValid(true);

        session.setMaxInactiveInterval(getMaxInactiveInterval() * 60); // Convert minutes in seconds.

        session.setUsername(username);



        // Store the session

        _store.save(session);



        // Return its id.

        return session.getId();



    }


    /**
     * Initiates a new session on base of username and cid. The new session id is returned.
     * Password is associated with session for setting property with user that can be used in Filter outside josso.
     * @return the new session identifier.
     */
    public String initiateSession(String username,String cid,String password) throws SSOSessionException {

      // Invalidate sessions if necessary
      //session are assigned on base of cid and username

        NSSessionStore store = (NSSessionStore)_store; 

        BaseSession sessions[] = store.loadByCidUsername(username,cid);

       if (!_invalidateExceedingSessions &&
                         _maxSessionsPerUser != -1 &&
                         _maxSessionsPerUser <= sessions.length) {
                     throw new TooManyOpenSessionsException(sessions.length);
              }

             // Check if sessions should be auto-invalidated.
           if (_invalidateExceedingSessions && _maxSessionsPerUser != -1) {


            // Number of sessions to invalidate
             int invalidate = sessions.length - _maxSessionsPerUser + 1;

            if (logger.isDebugEnabled())
                logger.debug("Auto-invalidating " + invalidate + "sessions for user : " + username);

             for (int idx = 0; invalidate > 0; invalidate --) {
                   BaseSession session = sessions[idx];


               if (logger.isDebugEnabled())
                     logger.debug("Auto-invalidating " + session.getId() + " session for user : " + username);

                   invalidate(session.getId());
	         }
          }

        // Build the new session.
        BaseSession session = doMakeNewSession();
                       
        // Configure the new session ...
        session.setId(_idGen.generateId());
        session.setCreationTime(System.currentTimeMillis());
        session.setValid(true);
        session.setMaxInactiveInterval(getMaxInactiveInterval() * 60); // Convert minutes in seconds.
        //username, cid , password  set for a particular session
        session.setUsername(username);
        session.setCustomerID(cid);
        session.setPassword(password);
         // Store the session
        _store.save(session);

        // Return its id.
        return session.getId();

    }

    /**

     * Gets an SSO session based on its id.

     *

     * @param sessionId the session id previously returned by initiateSession.

     * @throws org.josso.gateway.session.exceptions.NoSuchSessionException if the session id is not related to any sso session.

     */

    public SSOSession getSession(String sessionId) throws NoSuchSessionException, SSOSessionException {

        BaseSession s = _store.load(sessionId);

        if (s == null) {

            throw new NoSuchSessionException(sessionId);

        }

        return s;



    }



    /**

     * Gets all SSO sessions.

     */

    public Collection getSessions() throws SSOSessionException {

        return Arrays.asList(_store.loadAll());

    }



    /**

     * Gets an SSO session based on the associated user.

     *

     * @param username the username used when initiating the session.

     * @throws org.josso.gateway.session.exceptions.NoSuchSessionException

     *          if the session id is not related to any sso session.

     */

    public Collection getUserSessions(String username) throws NoSuchSessionException, SSOSessionException {

        BaseSession s[] = _store.loadByUsername(username);

        if (s.length < 1) {

            throw new NoSuchSessionException(username);

        }



        // Build the result

        List result = new ArrayList(s.length);

        for (int i = 0; i < s.length; i++) {

            result.add(s[i]);

        }



        return result;



    }



    /**

     * This method accesss the session associated to the received id.

     * This resets the session last access time and updates the access count.

     *

     * @param sessionId the session id previously returned by initiateSession.

     * @throws org.josso.gateway.session.exceptions.NoSuchSessionException if the session id is not valid or the session is not valid.

     */

    public void accessSession(String sessionId) throws NoSuchSessionException, SSOSessionException {



        // getCurrentSession will throw a NoSuchSessionException if not found.

        BaseSession s = (BaseSession) getSession(sessionId);

        if (!s.isValid()) {

            if (logger.isDebugEnabled())

                logger.debug("[accessSession()] invalid session : " + sessionId);

            throw new NoSuchSessionException(sessionId);

        }



        s.access();

        _store.save(s); // Update session information ...



        if (logger.isDebugEnabled())

            logger.debug("[accessSession()] ok");



    }



    /**

     * Invlalidates all open sessions.

     */

    public void invalidateAll() throws SSOSessionException {

        BaseSession[] sessions = _store.loadAll();

        for (int i = 0; i < sessions.length; i++) {

            BaseSession session = sessions[i];



            // Mark session as expired (this will notify session listeners, if any)

            session.expire();

        }

    }



    /**

     * Invalidates a session.

     *

     * @param sessionId the session id previously returned by initiateSession.

     * @throws org.josso.gateway.session.exceptions.NoSuchSessionException if the session id is not related to any sso session.

     */

    public void invalidate(String sessionId) throws NoSuchSessionException, SSOSessionException {



        // Get current session.

        BaseSession s = (BaseSession) getSession(sessionId);



        // Remove it from the store

        try {

            _store.remove(sessionId);

        } catch (SSOSessionException e) {

            logger.warn("Can't remove session from store\n" + e.getMessage() != null ? e.getMessage() : e.toString(), e);

        }



        // Mark session as expired (this will notify session listeners, if any)

        s.expire(); // This will invalidate the session ...



    }



    /**

     * Check all sessions and remove those that are not valid from the store.

     * This method is invoked periodically to update sessions state.

     */

    public void checkValidSessions() {



        try {



            //---------------------------------------------

            // Verify invalid sessions ...

            //---------------------------------------------

            BaseSession sessions[] = _store.loadByValid(false);

            if (logger.isDebugEnabled())

                logger.debug("[checkValidSessions()] found " + sessions.length + " invalid sessions");



            checkValidSessions(sessions);



            //---------------------------------------------

            // Verify old sessions ...

            //---------------------------------------------



            // Convert Max Inactive Interval to MS

            long period = _maxInactiveInterval * 60L * 1000L;

            Date from = new Date(System.currentTimeMillis() - period);

            sessions = _store.loadByLastAccessTime(from);

            if (logger.isDebugEnabled())

                logger.debug("[checkValidSessions()] found " + sessions.length + " sessions last accessed before " + from);



            checkValidSessions(sessions);



        } catch (Exception e) {

            logger.error("Can't process expired sessions : " + e.getMessage(), e);

        }



    }



    protected void checkValidSessions(BaseSession[] sessions) {

        for (int i = 0; i < sessions.length; i++) {

            try {



                // Ignore valid sessions, they have not expired yet.

                BaseSession session = (BaseSession) sessions[i];



                if (!session.isValid()) {

                    // Remove invalid session from the store.

                    _store.remove(session.getId());

                    if (logger.isDebugEnabled())

                        logger.debug("[checkValidSessions()] Session expired : " + session.getId());

                }





            } catch (Exception e) {

                logger.warn("Can't remove session [" + i + "]; " + e.getMessage() != null ? e.getMessage() : e.toString(), e);

            }

        }



    }



    public void setSessionStore(SessionStore ss) {

        _store = ss;

    }



    /**

     * Dependency Injection of Session Id Generator.

     */

    public void setSessionIdGenerator(SessionIdGenerator g) {

        _idGen = g;

    }



    /**

     * Number of sessions registered in the manager.

     *

     * @return the number of sessions registered in this manager.

     */

    public int getSessionCount() throws SSOSessionException {

        return _store.getSize();

    }



    // ---------------------------------------------------------------

    // Properties

    // ---------------------------------------------------------------



    public int getMaxInactiveInterval() {

        return _maxInactiveInterval;

    }



    /**

     * Used by config utils.  Interval in minutes.

     */

    public void setMaxInactiveInterval(String i) {

        setMaxInactiveInterval(Integer.parseInt(i));

    }



    /**

     * @param maxInactiveInterval in minutes

     */

    public void setMaxInactiveInterval(int maxInactiveInterval) {

        _maxInactiveInterval = maxInactiveInterval;

    }



    public int getMaxSessionsPerUser() {

        return _maxSessionsPerUser;

    }



    /**

     * Used by config utils.

     */

    public void setMaxSessionsPerUser(String i) {

        setMaxSessionsPerUser(Integer.parseInt(i));

    }



    public void setMaxSessionsPerUser(int maxSessionsPerUser) {

        _maxSessionsPerUser = maxSessionsPerUser;

    }



    public boolean isInvalidateExceedingSessions() {

        return _invalidateExceedingSessions;

    }





    /**

     * Just for JMX compatibility.

     */

    public boolean getInvalidateExceedingSessions() {

        return _invalidateExceedingSessions;

    }





    public void setInvalidateExceedingSessions(boolean invalidateExceedingSessions) {

        _invalidateExceedingSessions = invalidateExceedingSessions;

    }



    /**

     * Used by config utils.

     */

    public void setInvalidateExceedingSessions(String s) {

        setInvalidateExceedingSessions(Boolean.valueOf(s).booleanValue());

    }



    public long getSessionMonitorInterval() {

        return _sessionMonitorInterval;

    }



    public void setSessionMonitorInterval(long sessionMonitorInterval) {

        _sessionMonitorInterval = sessionMonitorInterval;

        if (_monitor != null) {

            _monitor.setInterval(_sessionMonitorInterval) ;

        }



    }



    /**

     * Used by config utils.

     */

    public void setSessionMonitorInterval(String sessionMonitorInterval) {

        setSessionMonitorInterval(Long.parseLong(sessionMonitorInterval));

    }



    // ---------------------------------------------------------------

    // Protected utils.

    // ---------------------------------------------------------------



    /**

     * Get new session class to be used in the doLoad() method.

     */

    protected BaseSession doMakeNewSession() {

        return new BaseSessionImpl();

    }



    // ---------------------------------------------------------------

    // To expire threads periodically,

    // TODO : use a configured scheduler ...

    // ---------------------------------------------------------------



    /**

     * Checks for valid sessions every second.

     */

    private class SessionMonitor implements Runnable {



        private long _interval;



        private SSOSessionManager _m;



        SessionMonitor(SSOSessionManager m) {

            _m = m;

        }



        SessionMonitor(SSOSessionManager m, long interval) {

            _interval = interval;

            _m = m;

        }



        public long getInterval() {

            return _interval;

        }



        public void setInterval(long interval) {

            _interval = interval;

        }



        /**

         * Check for valid sessions ...

         */

        public void run() {



            do {

                try {



                    if (logger.isDebugEnabled())

                        logger.debug("[run()] calling checkValidSessions ... ");



                    _m.checkValidSessions();



                    synchronized (this) {

                        try {



                            if (logger.isDebugEnabled())

                                logger.debug("[run()] waiting " + _interval + " ms");



                            wait(_interval);



                        } catch (InterruptedException e) {

                            logger.warn(e, e);

                        }

                    }

                } catch (Exception e) {

                    logger.warn("Exception received : " + e.getMessage() != null ? e.getMessage() : e.toString(), e);

                }



            } while (true);

        }

    }

}

package com.nightfire.webgui.core.josso.gateway.session.service.store;

import org.josso.gateway.session.service.store.AbstractSessionStore;
import org.josso.gateway.session.service.BaseSession;
import org.josso.gateway.session.exceptions.SSOSessionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * N* specific implementaion of SessionStore
 * This is a memory based store that uses a Map
 * This implementation is thread safe.
 */

public class NSMemorySessionStore extends AbstractSessionStore implements NSSessionStore {

    private static final Log logger = LogFactory.getLog(NSMemorySessionStore.class);

    private Map _sessions;

    // N* : Map key on base of  combinaion of cid and username
    private Map _sessionsByCidUsername;

    public NSMemorySessionStore() {
        _sessions = new HashMap();
        _sessionsByCidUsername = new HashMap();
    }

    public int getSize() throws SSOSessionException {
        synchronized(_sessions) {
            return _sessions.size();
        }
    }

    /**
     * Return an array containing the session identifiers of all Sessions
     * currently saved in this Store.  If there are no such Sessions, a
     * zero-length array is returned.
     *
     */
    public String[] keys() throws SSOSessionException {
        synchronized(_sessions) {
            return (String[]) _sessions.keySet().toArray(new String[_sessions.size()]);
        }
    }

    /**
     * Return an array of all BaseSessions in this store.  If there are not
     * sessions, then return a zero-length array.
     */
   public BaseSession[] loadAll() throws SSOSessionException {
        synchronized(_sessions) {
            return (BaseSession[]) _sessions.values().toArray(new BaseSession[_sessions.size()]);
        }
    }

    /**
     * Load and return the BaseSession associated with the specified session
     * identifier from this Store, without removing it.  If there is no
     * such stored BaseSession, return <code>null</code>.
     *
     * @param id BaseSession identifier of the session to load
     *
     */
    public BaseSession load(String id) throws SSOSessionException {
        BaseSession s = null;
        synchronized(_sessions) {
            s = (BaseSession) _sessions.get(id);
        }

        if (logger.isDebugEnabled())
            logger.debug("[load("+id+")] Session " + (s == null ? " not" : "" ) + " found");

        return s;

    }

    /**
     * Load and return the BaseSession associated with the specified username
     * from this Store, without removing it.  If there is no
     * such stored BaseSession, return <code>null</code>.
     *
     *  name username of the session to load
     *
     */
    public BaseSession[] loadByUsername(String name) throws SSOSessionException {
       BaseSession result[];
       return null ;

    }
  
     /**
     * Load and return the BaseSession associated with the specified username and cid
     * from this Store, without removing it.  If there is no
     * such stored BaseSession, return <code>null</code>.
     *
     *  @param username and cid of the session to load
     *
     */
       public BaseSession[] loadByCidUsername(String username,String cid) throws SSOSessionException {
         BaseSession result[];
         synchronized(_sessions) {
               Set sessions = (Set) _sessionsByCidUsername.get(cid+"%"+username);
               if (sessions == null)
               sessions = new HashSet();
               result =  (BaseSession[]) sessions.toArray(new BaseSession[sessions.size()]);
            }

        if (logger.isDebugEnabled())
             logger.debug("Debug: [loadByCidUsername ("+cid+")and ("+username+")  Sessions found =  " + result.length);

        return result ;
       }


    /**
     * Load and return the BaseSessions whose last access time is less than the received time
     */
    public BaseSession[] loadByLastAccessTime(Date time) throws SSOSessionException {
        List results = new ArrayList();
        synchronized(_sessions) {
            Collection sessions = _sessions.values();
            for (Iterator iterator = sessions.iterator(); iterator.hasNext();) {
                BaseSession session = (BaseSession) iterator.next();
                if (session.getLastAccessTime() < time.getTime()) {
                    results.add(session);
                }
            }
        }

        return (BaseSession[]) results.toArray(new BaseSession[results.size()]);

    }

    public BaseSession[] loadByValid(boolean valid) throws SSOSessionException {
        List results = new ArrayList();
        synchronized(_sessions) {
            Collection sessions = _sessions.values();
            for (Iterator iterator = sessions.iterator(); iterator.hasNext();) {
                BaseSession session = (BaseSession) iterator.next();
                if (session.isValid() == valid) {
                    results.add(session);
                }
            }
        }

        return (BaseSession[]) results.toArray(new BaseSession[results.size()]);
    }


    /**
     * Remove the BaseSession with the specified session identifier from
     * this Store, if present.  If no such BaseSession is present, this method
     * takes no action.
     *
     * @param id BaseSession identifier of the BaseSession to be removed
     */
    public void remove(String id) throws SSOSessionException {
        BaseSession session = null;
        synchronized(_sessions) {
            session = (BaseSession) _sessions.remove(id);
             if (session != null && session.getUsername() != null && session.getCustomerID()!= null) {
              _sessionsByCidUsername.remove(session.getCustomerID()+"%"+session.getUsername());   //N* Specific map
             }
          }
          if (logger.isDebugEnabled())
            logger.debug("[remove("+id+")] Session " + (session == null ? " not" : "" ) + " found");
    }

    /**
     * Remove all Sessions from this Store.
     */
    public void clear() throws SSOSessionException {
        synchronized(_sessions) {
            _sessions.clear();
            _sessionsByCidUsername.clear();
        }
    }

    /**
     * Save the specified BaseSession into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session BaseSession to be saved
     *
     */
    public void save(BaseSession session) throws SSOSessionException {
        BaseSession oldSession = null;
        synchronized(_sessions) {
            // Replace old session.
            oldSession = (BaseSession) _sessions.put(session.getId(), session);

            // Check if this is an update or an insert :
            if (oldSession != null) {

                // Updating old session : [on base of username and cid]
                String oldUsername = oldSession.getUsername();
                String oldcid = oldSession.getCustomerID();

                if (oldUsername != null && oldcid != null) {
                    // Remove old association
                    Set userSessions = (Set) _sessionsByCidUsername.get(oldcid+"%"+oldUsername);   // N* specific map
                    if (userSessions != null) {
                        userSessions.remove(oldSession);
                        if (logger.isDebugEnabled())
                            logger.debug("[Changed]Removing old session from reverse map : " + oldSession.getId() + ". user=" + oldUsername+ ". cid=" + oldcid );
                    }
                }
            }

            // Add new session to reverse map.
            if (session.getUsername() != null && session.getCustomerID() != null) {
                Set sessions = (Set) _sessionsByCidUsername.get(session.getCustomerID()+"%"+session.getUsername());
                 // N* specific map
                if (sessions == null) {

                    if (logger.isDebugEnabled())
                            logger.debug("Building new set for user: " + session.getUsername()+"and cid "+session.getCustomerID());

                    sessions = new HashSet();
                    // N* specific map
                    _sessionsByCidUsername.put(session.getCustomerID()+"%"+session.getUsername(), sessions);
                }

                if (logger.isDebugEnabled())
                    logger.debug("Adding session to reverse map : " + session.getId() + ". user=" + session.getUsername() + ". cid=" + session.getCustomerID() );

                sessions.add(session);

            }
        }

        if (logger.isDebugEnabled())
            logger.debug("[save(BaseSession."+session.getId()+")] Session " + (oldSession == null ? " inserted" : "" ) + " updated");

    }

}

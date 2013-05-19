package com.nightfire.webgui.core.josso.gateway.session.service.store;

import org.josso.gateway.session.exceptions.SSOSessionException;
import org.josso.gateway.session.service.BaseSession;
import org.josso.gateway.session.service.store.SessionStore;



/**

 * Represents a resource to store sessions.

 * Implementations define the specific persistence mechanism to store sessions.

 *

 * @author <a href="mailto:sgonzalez@josso.org">Sebastian Gonzalez Oyuela</a>

 * @version $Id: SessionStore.java 482 2007-10-09 19:06:42Z sgonzalez $

 */



public interface NSSessionStore extends SessionStore {



     /**

     *  N* Specific
     * Load and return the BaseSession associated with the specified username and cid

     * from this Store, without removing it.  If there is no

     * such stored BaseSession, return <code>null</code>.

     *

     * @param username and cid of the session to load

     *

     */

     public BaseSession[] loadByCidUsername(String username,String cid) throws SSOSessionException;




}

package com.nightfire.webgui.core.josso.gateway.identity.service;

import org.josso.gateway.identity.service.SSOIdentityManager;
import org.josso.gateway.identity.service.BaseUser;
import org.josso.gateway.identity.service.store.IdentityStore;
import org.josso.gateway.identity.service.store.IdentityStoreKeyAdapter;
import org.josso.gateway.identity.service.store.UserKey;
import org.josso.gateway.identity.service.store.SimpleUserKey;
import org.josso.gateway.identity.SSOUser;
import org.josso.gateway.identity.SSORole;
import org.josso.gateway.identity.exceptions.NoSuchUserException;
import org.josso.gateway.identity.exceptions.SSOIdentityException;
import org.josso.gateway.session.service.SSOSessionManager;
import org.josso.gateway.session.service.BaseSession;
import org.josso.gateway.session.exceptions.NoSuchSessionException;
import org.josso.gateway.session.exceptions.SSOSessionException;
import org.josso.gateway.SSONameValuePair;
import org.josso.auth.Authenticator;
import org.josso.auth.AuthenticatorImpl;
import org.josso.Lookup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;



/**

 * N* specific implementation of an SSOIdentityManager.

 * This implementation keeps track of user and session associations in memory.

 */



public class NSIdentityManagerImpl implements SSOIdentityManager {



    private static final Log logger = LogFactory.getLog(NSIdentityManagerImpl.class);



    // Identity store used by the manager.

    private IdentityStore _store;

    private IdentityStoreKeyAdapter _keyAdapter;

    private SSOSessionManager _sessionManager;

    //N* Specific - Use String for Name Value pair properties.
    private static final String USER_NAME = "username";

    private static final String CUSTOMER_ID = "customerid";

    private static final String PASSWORD = "password";

    /**

     *

     */

    public NSIdentityManagerImpl() {

    }



    /**

     * Finds a user based on its name.

     *

     * @param name the user login name, wich is unique for a domain.

     *

     * @throws org.josso.gateway.identity.exceptions.NoSuchUserException if the user does not exist for the domain.

     */

    public SSOUser findUser(String name)

            throws NoSuchUserException, SSOIdentityException {



        // Find user in store

        UserKey key = getIdentityStoreKeyAdapter().getKeyForUsername(name);

        BaseUser user = getIdentityStore().loadUser(key);

        if (user == null)

            throw new NoSuchUserException(key);



        // Done ... user found.

        return user;

    }



    /**

     * Finds the user associated to a sso session

     *

     * @param sessionId the sso session identifier

     *

     * @throws org.josso.gateway.identity.exceptions.SSOIdentityException if no user is associated to this session id.

     */

    public SSOUser findUserInSession(String sessionId)

        throws SSOIdentityException {



        BaseUser user = null;

        UserKey key = null;



        try {

            BaseSession s = (BaseSession) getSessionManager().getSession(sessionId);

            key = new SimpleUserKey(s.getUsername());

             // N* Specific Properties are set for getting cid and username in SecurityFilter
            user = getIdentityStore().loadUser(key);


            List props = new ArrayList();
            SSONameValuePair prop_userName = new SSONameValuePair (USER_NAME,key.toString());
            props.add(prop_userName);
            SSONameValuePair prop_CID = new SSONameValuePair (CUSTOMER_ID,s.getCustomerID() );
            props.add(prop_CID);
            SSONameValuePair prop_password = new SSONameValuePair (PASSWORD,s.getPassword());
            props.add(prop_password);

            SSONameValuePair[] userProps = (SSONameValuePair[]) props.toArray(new SSONameValuePair[props.size()]);
            user.setProperties(userProps);


            if (logger.isDebugEnabled())

                logger.debug("[findUserInSession("+sessionId+")] Found :  " + user);



            return user;



        } catch (NoSuchSessionException e) {

            throw new SSOIdentityException("Invalid session : " + sessionId);



        } catch (SSOSessionException e) {

            throw new SSOIdentityException(e.getMessage(), e);

        }



    }





    /**

     * Finds a collection of user's roles.

     * Elements in the collection are SSORole instances.

     *

     * @param username

     *

     * @throws org.josso.gateway.identity.exceptions.SSOIdentityException

     */

    public SSORole[] findRolesByUsername(String username)

        throws SSOIdentityException {



        UserKey key = getIdentityStoreKeyAdapter().getKeyForUsername(username);

        return getIdentityStore().findRolesByUserKey(key);




    }



    /**

     * Checks if current user exists in this manager.

     *

     * @throws org.josso.gateway.identity.exceptions.NoSuchUserException if the user does not exists.

     * @throws org.josso.gateway.identity.exceptions.SSOIdentityException if an error occurs

     */

    public void userExists(String username) throws NoSuchUserException, SSOIdentityException {

        UserKey key = getIdentityStoreKeyAdapter().getKeyForUsername(username);

        if (!getIdentityStore().userExists(key))

            throw new NoSuchUserException(key);

    }





    // --------------------------------------------------------------------

    // Public utils

    // --------------------------------------------------------------------



    /**

     * Used to set the store for this manager.

     * @param s

     */

    public void setIdentityStore(IdentityStore s) {

        _store = s;

    }



    public void setIdentityStoreKeyAdapter(IdentityStoreKeyAdapter a) {

        _keyAdapter = a;

    }



    public void initialize() {



    }



    // --------------------------------------------------------------------

    // Protected utils

    // --------------------------------------------------------------------



    protected IdentityStore getIdentityStore() {

        return _store;

    }



    protected IdentityStoreKeyAdapter getIdentityStoreKeyAdapter() {

        return _keyAdapter;

    }



    protected Authenticator getAuthenticator() {

        return new AuthenticatorImpl();

    }



    protected SSOSessionManager getSessionManager() {



        if (_sessionManager == null) {



            try {

                _sessionManager = Lookup.getInstance().lookupSecurityDomain().getSessionManager();

            } catch (Exception e) {

                logger.error("Can't find Session Manager : \n" + e.getMessage() != null ? e.getMessage() : e.toString(), e);

            }

        }



        return _sessionManager;

    }





}

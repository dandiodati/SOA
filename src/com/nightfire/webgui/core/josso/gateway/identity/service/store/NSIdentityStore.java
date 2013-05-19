package com.nightfire.webgui.core.josso.gateway.identity.service.store;

import org.josso.gateway.identity.service.store.AbstractStore;
import org.josso.gateway.identity.service.store.SimpleUserKey;
import org.josso.gateway.identity.service.store.UserKey;
import org.josso.gateway.identity.service.BaseUser;
import org.josso.gateway.identity.service.BaseUserImpl;
import org.josso.gateway.identity.service.BaseRole;
import org.josso.gateway.identity.service.BaseRoleImpl;
import org.josso.gateway.identity.exceptions.SSOIdentityException;
import org.josso.gateway.identity.exceptions.NoSuchUserException;
import org.josso.gateway.identity.SSORole;
import org.josso.auth.Credential;
import org.josso.auth.CredentialKey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.HashSet;

/**

 * N*  Memory based implementation of an IdentityStore and CredentialStore that reads

 * data from XML files.

 */

                                                   
public class NSIdentityStore extends AbstractStore {
    private static final Log logger = LogFactory.getLog(NSIdentityStore.class);
    private String _usersRoleName;


    //Role is taken as constant .and it must be same in <SecurityConstraint> of  Web.xml of partner app.
   // private static final String ROLE_NAME =  "chuser";



    public NSIdentityStore() {
        super();
    }


    public Credential[] loadCredentials(CredentialKey key) throws SSOIdentityException {
            logger.info("NFIdentityStore - loadCredentials()" + ((SimpleUserKey)key).getId());
            return new Credential[0];
    }


    
     public BaseUser loadUser(UserKey key) throws NoSuchUserException, SSOIdentityException
    {

        logger.info("NFIdentityStore - loadUser()" + ((SimpleUserKey)key).getId());

        BaseUser user = new BaseUserImpl(((SimpleUserKey)key).getId());

         return user;

    }


    public BaseRole[] findRolesByUserKey(UserKey key)
            throws SSOIdentityException
    {
        logger.info("NFIdentityStore - findRolesByUserKey() for user Key: " + ((SimpleUserKey)key).getId());
        Set roles = new HashSet();
        roles.add(new BaseRoleImpl( _usersRoleName));
        return (BaseRole[]) roles.toArray(new BaseRole[roles.size()]);

    }

    public SSORole[] findRolesByUsername(String username)
        throws SSOIdentityException {
        logger.info("NFIdentityStore - findRolesByUsername() for user Key: " + username);
        UserKey key = new SimpleUserKey(username);
        return findRolesByUserKey(key);

    }

    public BaseRole findRoleByName(String name) throws SSOIdentityException
    {
        logger.info("NFIdentityStore - findRoleByName(): " + name);
        BaseRole role = new BaseRoleImpl( _usersRoleName);
        return role;


    }

    /**
     *   Configuration properties
     *   mbean-descriptor.xml file describes related attributes.
     *   (org.josso.gateway.identity.service.store.mbean-descriptor.xml)
     */

    public void setUsersRoleName(String usersRoleName) {

          _usersRoleName = usersRoleName;

    }

}

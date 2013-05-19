package com.nightfire.webgui.core.josso.auth.scheme;

import org.josso.auth.scheme.AbstractAuthenticationScheme;
import org.josso.auth.scheme.UsernameCredential;
import org.josso.auth.scheme.PasswordCredential;
import org.josso.auth.SimplePrincipal;
import org.josso.auth.Credential;
import org.josso.auth.CredentialProvider;
import org.josso.auth.exceptions.SSOAuthenticationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.security.Principal;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.security.SecurityService;

/**
 * N* specific implementation for Basic Authentication.
 *
 * @see org.josso.auth.CredentialStore
 * @see org.josso.gateway.identity.service.store.AbstractStore
 * @see NSCredentialProvider
 */

public class NSAuthScheme extends AbstractAuthenticationScheme {


    private static final Log logger = LogFactory.getLog(NSAuthScheme.class);

    private String _name;

    // Some spetial configuration attributes,
        
    /**
     * The username recieved as UsernameCredential instance, if any.
     *
     *
     */
    public Principal getPrincipal() {
        return new SimplePrincipal(getUsername(_inputCredentials));
    }

   
    /**
     * The username recieved as UsernameCredential instance, if any.
     */
    public Principal getPrincipal(Credential[] credentials) {
        return new SimplePrincipal(getUsername(credentials));
    }

    /**
     * Authenticates the user using recieved credentials to proof his identity.
     *
     * @return the Principal if credentials are valid, null otherwise.
     */
    public boolean authenticate() throws SSOAuthenticationException {
        setAuthenticated(false);
      
        String username = getUsername(_inputCredentials);
        String password = getPassword(_inputCredentials);
        String cid = getDomain(_inputCredentials);

        logger.info("Number of available credentials: " + _inputCredentials.length);
        logger.info("CID: Username [" + cid + ":" + username + "]");

        if (!StringUtils.hasValue(cid))
        {
             logger.error("Invalid value for Domain.");
             return false;
        }

        SecurityService securityService = null;

        try
        {
            securityService = SecurityService.getInstance(cid);
        }
        catch (com.nightfire.security.SecurityException e)
        {
            logger.error("Could not fetch Security Service Instance for Domain: [" + cid + "]");
            return false;
        }


        // Check if all credentials are present.
        if (username == null || username.length() == 0 ||
                password == null || password.length() == 0) {

            if (logger.isDebugEnabled()) {
                logger.debug("Username " + (username == null || username.length() == 0 ? " not" : "") + " provided. " +
                             "Password " + (password == null || password.length() == 0 ? " not" : "") + " provided.");
            }

            // We don't support empty values !
            return false;
        }

        String knownUsername = getUsername(getKnownCredentials());
      
        // Authenticating user.
        try
        {
            securityService.authenticate(username, password);
        }
        catch (Exception e)
        {
            logger.error("Could not authenticate the user [" + username + "] in Domain [" + cid + "].\n " + e.getMessage());
            return false;
        }

        if (logger.isDebugEnabled())
            logger.debug("[authenticate()], Principal authenticated : " + username);

        // We have successfully authenticated this user.
        setAuthenticated(true);
        return true;
    }

    /**
     * Only one password credential supported.
     *  */
    public Credential[] getPrivateCredentials() {

        Credential c = getPasswordCredential(_inputCredentials);
        if (c == null)
            return new Credential[0];

        Credential[] r = {c};
        return r;

    }

    /**
     * N* - cid and  username credential supported.
     *
     *
     */
    public Credential[] getPublicCredentials() {
        Credential uncred = getUsernameCredential(_inputCredentials);
        Credential cidcred = getDomainCredential(_inputCredentials);
        if (uncred == null&&cidcred == null)
            return new Credential[0];
        Credential[] r = {cidcred,uncred};
        return r;
    }

    // --------------------------------------------------------------------
    // Protected utils
    // --------------------------------------------------------------------

    /**
     * Gets the domain-name from the received credentials.
     *
     * @param credentials
     *
     */
    protected String getDomain(Credential[] credentials) {
        DomainCredential c = getDomainCredential(credentials);
        if (c == null)
            return null;

        return (String) c.getValue();
    }

    /**
     * Gets the username from the received credentials.
     *
     * @param credentials
     *
     */
    protected String getUsername(Credential[] credentials) {
        UsernameCredential c = getUsernameCredential(credentials);
        if (c == null)
            return null;

        return (String) c.getValue();
    }

    /**
     * Gets the password from the recevied credentials.
     *
     * @param credentials
     *
     */
    protected String getPassword(Credential[] credentials) {
        PasswordCredential p = getPasswordCredential(credentials);
        if (p == null)
            return null;
        return (String) p.getValue();
    }

    /**
     * Gets the credential that represents a password.
     *
     * @param credentials
     *
     */
    protected PasswordCredential getPasswordCredential(Credential[] credentials) {
        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof PasswordCredential) {
                return (PasswordCredential) credentials[i];
            }
        }
        return null;
    }

    /**
     * Gets the credential that represents a Domain/CustomerID.
     *
     * @param credentials
     *
     */
    protected DomainCredential getDomainCredential(Credential[] credentials) {

        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof DomainCredential) {
                return (DomainCredential) credentials[i];
            }
        }
        return null;
    }

    /**
     * Gets the credential that represents a Username.
     *
     *
     */
    protected UsernameCredential getUsernameCredential(Credential[] credentials) {

        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof UsernameCredential) {
                return (UsernameCredential) credentials[i];
            }
        }
        return null;
    }

    protected CredentialProvider doMakeCredentialProvider() {
        return new NSCredentialProvider();
    }



    /**
     * Sets Authentication Scheme name
     */
    public void setName(String name) {
         _name = name;
    }

    /**
     * Obtains the Authentication Scheme name
     */
    public String getName() {
         return _name;
    }


}

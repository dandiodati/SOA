package com.nightfire.webgui.core.josso.auth.scheme;

import org.josso.auth.CredentialProvider;
import org.josso.auth.Credential;
import org.josso.auth.scheme.UsernameCredential;
import org.josso.auth.scheme.PasswordCredential;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * N* Specific  implemetation of CredentialProvider
 * 
 */

public class NSCredentialProvider implements CredentialProvider {

    /**
     * The name of the credential representing a password.
     * Used to get a new credential instance based on its name and value.
     * Value : password
     *
     * @see org.josso.auth.Credential newCredential(String name, Object value)
     */
    public static final String PASSWORD_CREDENTIAL_NAME = "password";


    /**
     * The name of the credential representing a username.
     * Used to get a new credential instance based on its name and value.
     * Value : username
     *
     * @see org.josso.auth.Credential newCredential(String name, Object value)
     */
    public static final String USERNAME_CREDENTIAL_NAME = "username";
    /**
        * The name of the credential representing a domain or cid.
        * Used to get a new credential instance based on its name and value.
        * Value : domain
        *
        * @see org.josso.auth.Credential newCredential(String name, Object value)
     */

    public static final String DOMAIN_CREDENTIAL_NAME = "domain";


    private static final Log logger = LogFactory.getLog(NSCredentialProvider.class);

    /**
     * Creates a new credential based on its name and value.
     *
     * @param name  the credential name
     * @param value the credential value
     * @return the Credential instance representing the supplied name-value pair.
     */
    public Credential newCredential(String name, Object value) {
        if (name.equalsIgnoreCase(USERNAME_CREDENTIAL_NAME)) {
            return new UsernameCredential(value);
        }

        if (name.equalsIgnoreCase(PASSWORD_CREDENTIAL_NAME)) {
            return new PasswordCredential(value);
        }
        if (name.equalsIgnoreCase(DOMAIN_CREDENTIAL_NAME)) {
            return new DomainCredential(value);
        }
        // Don't know how to handle this name ...
        if (logger.isDebugEnabled())
            logger.debug("Unknown credential name : " + name);

        return null;

    }
}

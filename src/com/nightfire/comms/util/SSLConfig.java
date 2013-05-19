/**
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 */

package com.nightfire.comms.util;

import java.io.*;
import java.security.*;
import java.util.*;
import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.common.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.repository.*;

import com.nightfire.security.*;

/**
 * Class that provides access to SSL configuration located in the repository.
 */
public class SSLConfig
{
   /**
    * The name of the property that specifies the install directory.
    */
    public static final String INSTALLROOT_PROP = "INSTALLROOT";

    /**
     * File containing the configuration information. The properties *_PROP are
     * the node names in this xml file. If any of the properties in this config
     * file are not needed, just set the value to be "". Do not delete the property
     * node itself.
     */
    public static String SSL_CONFIG = "sslConfig";

    /**
     * Property containing path to key store. If this is relative to the install root,
     * it will be adjusted to be an absolute path.
     */
    public static final String KEY_STORE_PROP = "KeyStore";

    /**
     * Property containing type of key store (pkcs12, jks)
     */
    public static final String KEY_STORE_TYPE_PROP = "KeyStoreType";

    /**
     * Property containing password to the key store.
     */
    public static final String KEY_STORE_PASS_PHRASE_PROP = "KeyStorePassPhrase";

    /**
     * Property containing key store management algorithm.
     */
    public static final String KEY_MGMT_ALGORITHM_PROP = "KeyManagementAlgorithm";

    /**
     * Property containing path to trust store. If this is relative to the install root,
     * it will be adjusted to be an absolute path.
     */
    public static final String TRUST_STORE_PROP = "TrustStore";

    /**
     * Property containing type of trust store (pkcs12, jks)
     */
    public static final String TRUST_STORE_TYPE_PROP = "TrustStoreType";

    /**
     * Property containing password to the trust store.
     */
    public static final String TRUST_STORE_PASS_PHRASE_PROP = "TrustStorePassPhrase";

    /**
     * Property containing key store management algorithm.
     */
    public static final String TRUST_MGMT_ALGORITHM_PROP = "TrustManagementAlgorithm";

    /**
     * Property containing SSL context version (TLS)
     */
    public static final String SSL_CONTEXT_VERSION_PROP = "SSLContextVersion";

    /**
     * Property containing debug setting for the SSL library. This is as specified by
     * JSSE.
     */
    public static final String SSL_DEBUG_SETTING_PROP = "SSLDebugSetting";

    /**
     * Property containing complete package name of the HTTPS protocol handler.
     */
    public static final String HTTPS_PROTOCOL_HANDLER_PACKAGE_PROP = "HTTPSProtocolHandlerPackage";

    /**
     * Property containing the security provider's class name along with package name.
     */
    public static final String SECURITY_PROVIDER_CLASS_PROP = "SecurityProviderClass";

    /**
     * Singleton implementation. Cannot be instantiated by external user.
     */
    private SSLConfig ()
    {
    }

    /**
     * Get the instance of this class for accessing the SSL configuration.
     * All users of this class access the methods on this class via
     * SSLConfig.getInstance().get* method.
     */
    public static SSLConfig getInstance(String sslConfigFileName) throws FrameworkException
    {
        if ( instance == null )
        {
            synchronized ( SSLConfig.class )
            {
                if ( instance == null )
                {
                    //If a valid filename is provided, use that for reading the ssl configuration from repository, else use the default file "sslCongig"
                    if(StringUtils.hasValue(sslConfigFileName))
                        SSL_CONFIG = sslConfigFileName;
                    instance = new SSLConfig();
                    instance.initialize();
                }
            }
        }
        return instance;
    }

    /**
     * This method set up the information for using SSL while communicating
     * with the peer. The information is read from repository/DEFAULT/security/sslConfig.xml.
     */
    protected void initialize( ) throws FrameworkException
    {
        Debug.log ( Debug.SECURITY_BASE, "SSLConfig initializing..." );

        HashMap props = new HashMap();
        try
        {
            //Check if configuration exists.
            if (!RepositoryManager.getInstance().existsMetaData
                    (SecurityService.SECURITY_CONFIG, SSL_CONFIG))
            {
                String errorMsg = "Configuration properties [" +
                SecurityService.SECURITY_CONFIG + "/" + SSL_CONFIG + "] are missing.";

                Debug.log( Debug.SYSTEM_CONFIG, errorMsg );
                throw new com.nightfire.security.SecurityException( "Unable to initialize SSLUtils: " +
                errorMsg );
            }
            else
            //Load all the configuration properties for in-memory manipulation.
            {
                // Get the description of the meta-data describing the security store configuration
                Document xmlSSLConfig = RepositoryManager.getInstance().getMetaDataAsDOM(
                SecurityService.SECURITY_CONFIG, SSL_CONFIG );

                //read in security store configuration
                XMLMessageParser parser = new XMLMessageParser(xmlSSLConfig);
                Node[] items = parser.getChildNodes( parser.getDocument().getDocumentElement() );
                for ( int Ix = 0;  Ix < items.length;  Ix ++ )
                {
                    String previousValue = (String)props.put( items[Ix].getNodeName(), parser.getNodeValue( items[Ix] ) );

                    if ( StringUtils.hasValue( previousValue ) )
                    {
                        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) )
                            Debug.log( Debug.SYSTEM_CONFIG,
                                "Replacing previous configuration value [" +
                                    previousValue + "] for [" + items[Ix].getNodeName() +
                                        "] with [" + parser.getNodeValue( items[Ix] ) + "]." );
                    }
                }//for

            }//else

        }//try
        catch ( FrameworkException e )
        {
            throw e;
        }

        //This property is used to convert relative trust store/key store paths to
        //absolute paths.
        installRoot = System.getProperty( INSTALLROOT_PROP );

        //set up all the ssl configuration from the repository data
        this.setTrustStore( (String) props.get( TRUST_STORE_PROP ) );
        this.setTrustStoreType( (String) props.get( TRUST_STORE_TYPE_PROP ) );
        this.setTrustStorePassPhrase( (String) props.get( TRUST_STORE_PASS_PHRASE_PROP ) );
        this.setTrustManagementAlgorithm( (String) props.get( TRUST_MGMT_ALGORITHM_PROP ) );

        this.setKeyStore( (String) props.get( KEY_STORE_PROP ) );
        this.setKeyStoreType( (String) props.get( KEY_STORE_TYPE_PROP ) );
        this.setKeyStorePassPhrase( (String) props.get( KEY_STORE_PASS_PHRASE_PROP ) );
        this.setKeyManagementAlgorithm( (String) props.get( KEY_MGMT_ALGORITHM_PROP ) );

        this.setSSLContextVersion( (String) props.get( SSL_CONTEXT_VERSION_PROP ) );
        this.setSSLDebugSetting( (String) props.get( SSL_DEBUG_SETTING_PROP ) );
        this.setHTTPSProtocolHandlerPackage( (String) props.get( HTTPS_PROTOCOL_HANDLER_PACKAGE_PROP ) );
        this.setSecurityProvider( (String) props.get( SECURITY_PROVIDER_CLASS_PROP ) );

        if ( Debug.isLevelEnabled ( Debug.SECURITY_BASE ) )
            Debug.log ( Debug.SECURITY_BASE, "SSLConfig: " + describe() );
        Debug.log ( Debug.SECURITY_BASE, "SSLConfig initialized." );

    }//initialize

    private String getAbsolutePath ( String value )
    {
        //If value can be located, don't try to convert it to absolute path
        File f = new File( value );
        if ( !f.exists() && ( StringUtils.hasValue( value ) ) )
        {
            //File does not exist and value is a valid string, so try to convert value into
            //absolute path.
            if ( StringUtils.hasValue( installRoot ) )
            {
                if ( installRoot.endsWith( File.separator ) )
                    value = installRoot + value;
                else
                    value = installRoot + File.separator + value;
            }
        }
        return value;
    }

    public void setSecurityProvider( String value ) throws FrameworkException
    {
        if ( StringUtils.hasValue(value) )
        {
            securityProviderClass = value;
        }

        try
        {
            securityProvider = (Provider)ObjectFactory.create(securityProviderClass);
        }
        catch (FrameworkException e)
        {
            throw new FrameworkException ("Failed to create security provider : " +
            securityProviderClass + ":" + e.toString() );
        }
    }

    public Provider getSecurityProvider()
    {
        return securityProvider;
    }

    public void setTrustStore( String value )
    {
        if ( StringUtils.hasValue( value ) )
            trustStore = value;
        trustStore = getAbsolutePath ( trustStore );
    }

    public String getTrustStore()
    {
        return trustStore;
    }

    public void setTrustStoreType( String value )
    {
        if ( StringUtils.hasValue( value ) )
            trustStoreType = value;
    }

    public String getTrustStoreType()
    {
        return trustStoreType;
    }

    public void setTrustStorePassPhrase( String value )
    {
        if ( StringUtils.hasValue( value ) )
            trustStorePassPhrase = value;
    }

    public String getTrustStorePassPhrase()
    {
        return trustStorePassPhrase;
    }

    public void setTrustManagementAlgorithm( String value )
    {
        if ( StringUtils.hasValue( value ) )
            trustManagementAlgorithm = value;
    }

    public String getTrustManagementAlgorithm()
    {
        return trustManagementAlgorithm;
    }

    public void setKeyStore( String value )
    {
        if ( StringUtils.hasValue( value ) )
            keyStore = value;

        keyStore = getAbsolutePath ( keyStore );
    }

    public String getKeyStore()
    {
        return keyStore;
    }

    public void setKeyStoreType( String value )
    {
        if ( StringUtils.hasValue( value ) )
            keyStoreType = value;

    }

    public String getKeyStoreType()
    {
        return keyStoreType;
    }

    public void setKeyStorePassPhrase( String value )
    {
        if ( StringUtils.hasValue( value ) )
            keyStorePassPhrase = value;
    }

    public String getKeyStorePassPhrase()
    {
        return keyStorePassPhrase;
    }

    public void setSSLContextVersion( String value )
    {
        if ( StringUtils.hasValue( value ) )
            sslContextVersion = value;
    }

    public String getSSLContextVersion()
    {
        return sslContextVersion;
    }

    public void setKeyManagementAlgorithm( String value )
    {
        if ( StringUtils.hasValue( value ) )
            keyManagementAlgorithm = value;
    }

    public String getKeyManagementAlgorithm()
    {
        return keyManagementAlgorithm;
    }

    public void setHTTPSProtocolHandlerPackage( String value )
    {
        if ( StringUtils.hasValue( value ) )
            httpsProtocolHandlerPackage = value;
    }

    public String getHTTPSProtocolHandlerPackage()
    {
        return httpsProtocolHandlerPackage;
    }

    public void setSSLDebugSetting( String value )
    {
        if ( StringUtils.hasValue( value ) )
            sslDebugSetting = value;
    }

    public String getSSLDebugSetting()
    {
        return sslDebugSetting;
    }

    /**
     * User friendly description of the configuration. This is the configuration
     * that the user is accessing.
     */
    public String describe()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "\ntrust store[" );
        buf.append( trustStore );

        buf.append( "], trust store type[" );
        buf.append( trustStoreType );

        buf.append( "], trust manager algorithm[" );
        buf.append( trustManagementAlgorithm );

        buf.append( "], \nkey store[" );
        buf.append( keyStore );

        buf.append( "], key store type[" );
        buf.append( keyStoreType );

        buf.append( "], key manager algorithm[" );
        buf.append( keyManagementAlgorithm );

        buf.append( "], \nSSL context version[" );
        buf.append( sslContextVersion );

        buf.append( "], HTTPS protocol handler[" );
        buf.append( httpsProtocolHandlerPackage );

        buf.append( "], security provider class[" );
        buf.append( securityProviderClass );

        buf.append( "], SSL debug setting [" );
        buf.append( sslDebugSetting );

        buf.append( "].\n" );

        return buf.toString();

    }

    //Instance members to store configuration settings. The values they are set to
    //at class loading are the default fall-back values.
    private String trustStore = "";
    private String trustStorePassPhrase = "";
    private String trustStoreType = "";
    private String trustManagementAlgorithm = "SunX509";

    private String keyStore = "";
    private String keyStorePassPhrase = "";
    private String keyStoreType = "";
    private String keyManagementAlgorithm = "SunX509";

    private String sslContextVersion = "TLS";
    private String httpsProtocolHandlerPackage = "com.sun.net.ssl.internal.www.protocol";
    private String sslDebugSetting = "ssl:handshake:verbose:session";

    private String securityProviderClass = "com.sun.net.ssl.internal.ssl.Provider";
    /**
     * Instance created as per the securityProviderClass value.
     */
    private Provider securityProvider = null;

    /**
     * Singleton instance of this class.
     */
    private static SSLConfig instance = null;
    
    private String installRoot = null;

}

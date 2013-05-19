/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: //comms/R4.4/com/nightfire/comms/soap/ssl/SSLUtils.java#1 $
 */

package com.nightfire.comms.soap.ssl;

import org.w3c.dom.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.rpc.soap.*;
import javax.xml.rpc.JAXRPCException;
import java.rmi.RemoteException;

// classes for ssl
import java.io.*;
import java.security.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.spi.common.communications.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.comms.eventchannel.*;

import com.nightfire.framework.message.common.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;

import com.nightfire.comms.util.*;
import com.nightfire.comms.soap.*;

/*
 * This is a class that encapsulates all the SSL specific information.
 */
public class SSLUtils
{


    /**
     * The initialize or initializeOnce are the only 2 methods that need to be invoked to
     * use this class. These methods set up the information for using SSL while communicating
     * with the peer.
     */
    public static final void initialize(String sslConfigFileName ) throws SecurityException
    {
        Debug.log ( Debug.SECURITY_BASE, "SOAP SSLUtils initializing..." );

        if ( Debug.isLevelEnabled( Debug.SECURITY_BASE ) )
        {
            Debug.log( Debug.SECURITY_BASE, "Initializing SSL log re-directing." );
            StdOutLogger.initialize();
        }

        try
        {
            //Turn on SSL layer debugging.
            if ( Debug.isLevelEnabled( Debug.SECURITY_BASE ) )
            {
                System.setProperty( "javax.net.debug", SSLConfig.getInstance(sslConfigFileName).getSSLDebugSetting() );
            }

            // specify the location of where to find key material for the default TrustManager
            //(this overrides jssecacerts and cacerts)
            if ( StringUtils.hasValue( SSLConfig.getInstance(sslConfigFileName).getTrustStore() ) )
                System.setProperty( "javax.net.ssl.trustStore", SSLConfig.getInstance(sslConfigFileName).getTrustStore() );

            if ( StringUtils.hasValue( SSLConfig.getInstance(sslConfigFileName).getTrustStoreType() ) )
                System.setProperty( "javax.net.ssl.trustStoreType", SSLConfig.getInstance(sslConfigFileName).getTrustStoreType() );

            if ( StringUtils.hasValue( SSLConfig.getInstance(sslConfigFileName).getTrustStorePassPhrase() ) )
                System.setProperty("javax.net.ssl.trustStorePassword", SSLConfig.getInstance(sslConfigFileName).getTrustStorePassPhrase() );

            if ( StringUtils.hasValue( SSLConfig.getInstance(sslConfigFileName).getKeyStore() ) )
                System.setProperty( "javax.net.ssl.keyStore", SSLConfig.getInstance(sslConfigFileName).getKeyStore() );

            if ( StringUtils.hasValue( SSLConfig.getInstance(sslConfigFileName).getKeyStoreType() ) )
                System.setProperty( "javax.net.ssl.keyStoreType", SSLConfig.getInstance(sslConfigFileName).getKeyStoreType() );

            if ( StringUtils.hasValue( SSLConfig.getInstance(sslConfigFileName).getKeyStorePassPhrase() ) )
                System.setProperty("javax.net.ssl.keyStorePassword", SSLConfig.getInstance(sslConfigFileName).getKeyStorePassPhrase() );

            // use Sun's reference implementation of a URL handler for the "https" URL protocol type.
            System.setProperty("java.protocol.handler.pkgs", SSLConfig.getInstance(sslConfigFileName).getHTTPSProtocolHandlerPackage() );

            // dynamically register sun's ssl provider
            Security.addProvider( SSLConfig.getInstance(sslConfigFileName).getSecurityProvider() );

            if ( Debug.isLevelEnabled( Debug.SECURITY_BASE ) )
            {
                Provider[] list = Security.getProviders();

                for ( int Ix=0; Ix < list.length; Ix++ )
                {
                    Debug.log( Debug.SECURITY_BASE, "Provider [" + Ix + "] = [" + list[Ix].toString() + "]\n" );
                }
            }
        }
        catch ( FrameworkException e )
        {
            throw new SecurityException ( e.toString() );
            
        }

        Debug.log ( Debug.SECURITY_BASE, "SOAP SSLUtils initialized." );

    }//initialize

    /**
     * The initialize or initializeOnce are the only 2 methods that need to be invoked to
     * use this class. These methods set up the information for using SSL while communicating
     * with the peer.
     * This method sets up SSL only once per JVM.
     * @throws SecurityException Thrown if SSL initialization fails
     */
    public static final void initializeOnce ( )  throws SecurityException
    {
        //Initialize using default sslConfig file (sslConfig.xml in <repository-root> security folder)
         initializeOnce(null);
    }

    /**
     * This method enables setting up different clients using different SSL
     * configuration (on different JVMs); sharing the same install/repository roots.
     *
     * The initialize or initializeOnce are the only 2 methods that need to be invoked to
     * use this class. These methods set up the information for using SSL while communicating
     * with the peer.
     * This method sets up SSL only once per JVM.
     * @throws SecurityException Thrown if SSL initialization fails
     */
   public static final void initializeOnce(String sslConfigFileName) throws SecurityException
    {
        if ( !sslInitialized )
        {
            synchronized ( SSLUtils.class )
            {
                if ( !sslInitialized )
                {
                    Debug.log ( Debug.SECURITY_BASE, "SOAP SSLUtils being initialized only once for this VM. " +
                            "Using SSL configuration file [" + (StringUtils.hasValue(sslConfigFileName)?sslConfigFileName:SSLConfig.SSL_CONFIG) + "]");
                    initialize (sslConfigFileName);
                    sslInitialized = true;
                }
            }//synchronized
        }//if
    }
    /**
     * Variable to keep track if the SSLUtils has been initialized once for this JVM.
     */
    private static boolean sslInitialized = false;
}

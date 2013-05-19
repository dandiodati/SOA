/*
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.comms.util.ftp;
import com.nightfire.framework.util.*;
import java.security.*;



public class SSLConfigInfo
    {
        private String trustStore = "certs/cacerts";
        private String keyStore = ".keystore";
        private String passPhrase = "";
        private String sslContextVersion = "TLS";
        private String keyManagementAlg = "SunX509";
        private String keyStoreType = "pkcs12";
    
        
        
        private Provider securityProvider = new com.sun.net.ssl.internal.ssl.Provider();
    
        public void setProvider(String providerClass) 
        {    
            if ( providerClass != null) {
                try {
                    
                    securityProvider = (Provider)ObjectFactory.create(providerClass);    
                } catch (FrameworkException e){
                    Debug.error("Failed to create security provider : " + providerClass);
                }
                
            }
        }
        
            
    
        public Provider getProvider()
        {
            return securityProvider;
        }
        
        
        public void setTrustStore(String trustStore)
        {
            if ( StringUtils.hasValue(trustStore))
                this.trustStore = trustStore;
        }
        
        public String getTrustStore()
        {
            return trustStore;
        }
        
        public void setKeyStore(String keyStore)
        {
            if (StringUtils.hasValue(keyStore))
                this.keyStore = keyStore;
        }
        
        public String getKeyStore()
        {
            return keyStore;
        }
        
        public void setKeyStorePassPhrase(String passPhrase)
        {
            this.passPhrase = passPhrase;
        }
        
        public String getKeyStorePassPhrase()
        {
            return passPhrase;
        }

        
        public void setSSLContextVersion(String sslContextV)
        {
            if( StringUtils.hasValue(sslContextV) )
                this.sslContextVersion = sslContextV;
        }
        
        public String getSSLContextVersion()
        {
            return sslContextVersion;
        }
        
        public void setKeyManagementAlgorithm(String kma)
        {
            if (StringUtils.hasValue(kma))
                keyManagementAlg = kma;
        }
        
        public String getKeyManagementAlgorithm()
        {
            return keyManagementAlg;
        }
        
        
        public void setKeyStoreType(String type)
        {
            if ( StringUtils.hasValue(type))
                this.keyStoreType = type;
        }
        
        public String getKeyStoreType()
        {
            return keyStoreType;
        }
        
    }

/**
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.adapter.util;

// JDK import
import java.net.*;
import java.io.*;
import java.util.*;

// Nightfire import
import com.nightfire.framework.db.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.comms.util.http.ssl.SSLKeyStoreVerifier;

// Phaos import
import crysec.*;
import crysec.SSL.*;

/**
 * This class provides abstraction to the third party library for SSL connectivity.
 */

public class SSLPortabilityLayer {

  /**
  * Persistent properties.
  */
  private Hashtable sslProperties;

  /**
  * Cached SSL parameters
  */
  private Hashtable session;

  /**
  * Trusted certificate collection.
  */
  private String[] trustedCerts;

  /**
  * Server certificate collection.
  */
  private String[] serverCerts;

  /**
  * Private key;
  */
  private String privateKey;

  /**
  * Key pass phrase;
  */
  private String keyPassPhrase;

  /**
  * Flag for use of sesumable sessions
  */
  private boolean useResumableSessions = true;

  /**
  * Flag for use of SSL v.3
  */
  private boolean useSSLV3 = true;

  private boolean sslDebugMode = false;
    
  private SSLParams params = null;
    

  /**
  * Property name for trusted certificates name prefix.
  */
  private static final String TRUSTED_CERT_PREFIX_PROP = "TRUSTED_CERT_PREFIX";

  /**
  * Property name for server certificate.
  */
  private static final String SERVER_CERT_PREFIX_PROP = "SERVER_CERT_PREFIX";

  /**
  * Property name for private key.
  */
  private static final String PRIVATE_KEY_PROP = "PRIVATE_KEY";

  /**
  * Property name for passcode.
  */
  private static final String KEY_PASSPHRASE_PROP = "KEY_PASSPHRASE";

  /**
  * Property name for set resumable sessions flag
  */
  private static final String USE_RESUMABLE_SESSIONS_PROP = "USE_RESUMABLE_SESSIONS";

 /**
  * Property name for setting ssl debug mode
  */
  private static final String SSL_DEBUG_PROP = "SSL_DEBUG";

  /**
  * Property name for set resumable sessions flag
  */
  private static final String USE_SSL_V3_PROP = "USE_SSL_V3";

  /**
  * Property name delimiter.
  */
  private static final String NAME_DELIMITER = "_";

  /**
  * Sessin key delimiter.
  */
  private static final String SESSION_KEY_DELIMITER = "@";

  /**
  * Constructor
  *
  */
  public SSLPortabilityLayer()
  {
  }

  /**
  * Build property chain given key and type, get properties.
  *
  * @param   key   Property key
  *
  * @param   type  Property type
  *
  * @exception   ProcessingException   Thrown when property chain can not be built
  */
  public void initialize ( String key, String type ) throws ProcessingException
  {
    PropertyChainUtil propChain = new PropertyChainUtil();

      if ( Debug.isLevelEnabled ( Debug.OBJECT_LIFECYCLE ) )
        Debug.log( Debug.OBJECT_LIFECYCLE,
      "SSLPortabilityLayer: Looking for all properties with key [" +
      key + "], type [" + type + "]." );

    try {
      sslProperties = propChain.buildPropertyChains(key, type);
    }
    catch ( PropertyException pe ) {
      throw new ProcessingException( pe.getMessage() );
    }

    StringBuffer errorMsg = new StringBuffer();

    // get use resumable sessions flag
    try
    {
      useResumableSessions = StringUtils.getBoolean( getPropertyValue(USE_RESUMABLE_SESSIONS_PROP, errorMsg) );
    } catch (FrameworkException e)
    {
      errorMsg.append ("Failed to convert " + USE_RESUMABLE_SESSIONS_PROP + " to boolean\n");
    }


     sslDebugMode  = StringUtils.getBoolean(
                          (String)sslProperties.get(SSL_DEBUG_PROP), 
                          false);

   
    // get use SSL v.3 flag
    try
    {
      useSSLV3 = StringUtils.getBoolean( getPropertyValue(USE_SSL_V3_PROP, errorMsg) );
    } catch (FrameworkException e)
    {
      errorMsg.append ("Failed to convert trimWhiteSpace to boolean\n");
    }

    // get private key
    privateKey = getPropertyValue(PRIVATE_KEY_PROP, errorMsg);

    // get key pass phrase
    keyPassPhrase = getPropertyValue(KEY_PASSPHRASE_PROP, errorMsg);

    // find all trusted certificates in property
    trustedCerts = getNumberedProperty(TRUSTED_CERT_PREFIX_PROP, errorMsg);

    // find all server certificates in property
    serverCerts = getNumberedProperty(SERVER_CERT_PREFIX_PROP, errorMsg);

    //throw an exception if errorMsg is not empty
    if (errorMsg.length() != 0)
      throw new ProcessingException (
        "The following errors occured while initializing SSLPortabilityLayer:\n" +
        errorMsg.toString() );

    // initialize session hashtable
    session = new Hashtable();

    try {
        
        params = getParameters();
    }
    catch (IOException e) {
        throw new ProcessingException(e.getMessage());
    }
    
    
  }


  

  /**
  * Returns a server socket on the specific port
  *
  * @param   portNum   Port the server socket is created on
  *
  * @return  The server socket
  *
  */
  public ServerSocket getServerSocket(int portNum) throws ProcessingException
  {
    SSLServerSocket serverSocket = null;

    try
    {
      // get SSL parameters
      SSLParams params = getParameters();

      serverSocket = new SSLServerSocket(portNum, params);
        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log(Debug.IO_STATUS, "SSLPortabilityLayer.getServerSocket: server socket created");
    }
    catch (Exception e)
    {
      Debug.log(Debug.ALL_ERRORS, "ERROR: SSLPortabilityLayer.getServerSocket: " +
        "Failed to get server socket: " + e.getMessage());
      throw new ProcessingException("ERROR: SSLPortabilityLayer.getServerSocket: " +
        "Failed to get server socket: " + e.getMessage());
    }

    return serverSocket;
  }

  /**
   * Non blocking accept method used to delegate the accept for an SSL socket.
   * The call won't perform the handshake.
   * @param serverSocket the Server socket.
   * @return Socket
   */
  public Socket accept( ServerSocket serverSocket ) throws ProcessingException
  {
      if( !(serverSocket instanceof SSLServerSocket) )
      {
          throw new ProcessingException( serverSocket.toString() + " is not an SSLServerSocket" );
      }

      try
      {
          return ((SSLServerSocket) serverSocket).acceptNoHandshake();
      }
      catch (IOException ex)
      {
          String errorMsg = "SSLPortabilityLayer.accept: " +
                            "Failed to accept server socket: " + ex.getMessage() ;
          throw new ProcessingException( errorMsg );
      }
  }

  /**
   * Perform the handshake
   * @param sslSocket The soket passed in
   * @throws ProcessingException
   */
  public void performHandshake( Socket sslSocket ) throws ProcessingException
  {
      if( !(sslSocket instanceof SSLSocket) )
      {
          throw new ProcessingException( sslSocket.toString() + " is not an SSLSocket" );
      }

      try
      {
          ( (SSLSocket)sslSocket ).performAcceptHandshake();
      }
      catch (IOException ex)
      {
          throw new ProcessingException( sslSocket.toString() +
                  " was unable to perfor the ssl handshake:" + ex.getMessage() );
      }
  }


  /**
  * Returns a socket to the specified host on the specific port
  *
  * @param   host   Host the socket connects to
  *
  * @param   portNum   Port the socket is created on
  *
  * @return  The socket
  *
  */
  public synchronized Socket getSocket(String host, int portNum) throws ProcessingException
  {
    SSLSocket socket = null;

    try
    {
      // get SSL parameters
      SSLParams params = getParameters();
        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log(Debug.IO_STATUS, "SSLPortabilityLayer.getServerSocket: socket created");

      boolean exist = false;
      String key = host + SESSION_KEY_DELIMITER + portNum;

      // check if this connection being chached
      if( session.containsKey(key) )
      {
        params.setSessionParams( (SessionParams) session.get(key) );
        exist = true;
      }

      try
      {
     	 socket = new SSLSocket(host, portNum, params);
      }
      catch (Exception e)
      {
	    Debug.log(Debug.ALL_WARNINGS, "SSLPortabilityLayer.getSocket(): Attempt to create socket failed: ");
	    Debug.logStackTrace(e);
	    
	    //remove from session cache
	    if( session.containsKey(key) )
	    {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log(Debug.MSG_STATUS, "Server may have invalidated the session due to inactivity, "
			    		+ "removing cached SSLSession parameters from cache.");
		    session.remove(key);
	    }
	    
	    //try again
	    params = getParameters();
          if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
    	    Debug.log(Debug.MSG_STATUS, "SSLPortabilityLayer.getSocket(): Second attempt to create SSL socket ... ");
	    socket = new SSLSocket(host, portNum, params);

      }

        if ( Debug.isLevelEnabled ( Debug.IO_STATUS ) )
            Debug.log(Debug.IO_STATUS, "SSLPortabilityLayer.getServerSocket: socket created");

      if(!exist)
        session.put(key, socket.getSessionParams());
    }
    catch (Exception e)
    {
      Debug.log(Debug.ALL_ERRORS, "ERROR: SSLPortabilityLayer.getSocket: " +
        "Failed to get socket: " + e.getMessage());
        Debug.logStackTrace(e);
      throw new ProcessingException("ERROR: SSLPortabilityLayer.getSocket: " +
        "Failed to get socket: " + e.getMessage());
    }

    return socket;
  }

  /**
  * Returns parameters used for setting up SSL connection
  *
  * @return  The SSL parameters
  *
  */
  public SSLParams getParameters() throws IOException
  {

      if ( params == null) {
          synchronized(this) {
              if (params == null) {
                     
              params = new SSLParams();

              // load our trusted certificates
              SSLKeyStoreVerifier v = new SSLKeyStoreVerifier();
              for (int i = 0; i < trustedCerts.length; i++)
                  {
                      v.addTrustedCACert(new X509(new File(trustedCerts[i])));
                  }
              params.setCertVerifier(v);

              // set the supported ciphers
              short[] cipherSuites =
                  {
                      SSLParams.SSL_RSA_WITH_RC4_128_SHA,
                      SSLParams.SSL_RSA_WITH_RC4_128_MD5,
                      SSLParams.SSL_RSA_WITH_3DES_EDE_CBC_SHA,
                      SSLParams.SSL_RSA_WITH_DES_CBC_SHA
                  };
              params.setServerCipherSuites(cipherSuites);
              params.setClientCipherSuites(cipherSuites);

              // load our certificate
              SSLCertificate chain = new SSLCertificate();
              chain.certificateList = new Vector();
              for (int i = 0; i < serverCerts.length; i++)
                  {
                      chain.certificateList.addElement(new X509(new File(serverCerts[i])));
                  }

              // load our private key
              PrivateKeyPKCS8 pk = new PrivateKeyPKCS8(keyPassPhrase, new File(privateKey));
              chain.privateKey = pk.getKey();

              // add the certificate, key, and pass phrase
              params.setServerCert(chain);
              params.setClientCert(chain);

              // allow sessions to be resumed
              params.setResumableSessions(useResumableSessions);

              // use SSL V3
              params.setUseV2Hello(!useSSLV3);

              // for debugging only
              params.setDebug(sslDebugMode);
              }
              
          }
      }    

      return params;
  }

  /**
   * Return property value from property chain given property name.
   * Append any error in a string buffer.
   *
   * @param   propName    Name of the property
   *
   * @param   errorMsg    Error message buffer
   *
   * @return  String containing the value of the property
   */
  private String getPropertyValue(String propName, StringBuffer errorMsg)
  {
    String s = (String) sslProperties.get(propName);

    if( ! StringUtils.hasValue(s) )
    {
      errorMsg.append ("Property value for " + propName + " is null\n");
    }
    else
    {
        if ( Debug.isLevelEnabled ( Debug.MSG_DATA) )
            Debug.log(Debug.MSG_DATA, "SSLPortabilityLayer.getProperty: Property " +
            propName + " is set to " + s );
    }

    return s;
  }


  /**
  * This method gets all the properties with name in the format of "PropNamePrefix_n"
  * Integer n starts from 0.
  *
  * @param   prefixProp   Property name prefix
   *
  * @param   errorMsg   StringBuffer errorMsg
  *
  * @return  String[]   String array containing property values
  *
  */
  private String[] getNumberedProperty(String prefixProp, StringBuffer errorMsg)
  {
    // get property prefix
    String propNamePrefix = null;
    propNamePrefix = getPropertyValue(prefixProp, errorMsg);

    // find all properties with name in the format of propNamePrefix_i and put them in an ArrayList
    String tempProp = null;
    ArrayList tempPropArrayList = new ArrayList();
    int i = 0;
    do {
      tempProp = (String) sslProperties.get(propNamePrefix + NAME_DELIMITER + Integer.toString(i));

      if( !StringUtils.hasValue(tempProp))
        break;

      if( tempProp.length() > 0 )
        tempPropArrayList.add(tempProp);

      i++;
    } while(true);

    // put trusted certificates in an array
    String[] retArray = new String[tempPropArrayList.size()];
    return retArray = (String[])tempPropArrayList.toArray(retArray);
  }

}


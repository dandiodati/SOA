/*
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp;

import java.io.*;
import java.net.*;
import java.security.*;
// A more up to date version is now available in
// java.security.cert.X509Certificate, but we use the old one for now
//import javax.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

import org.apache.commons.net.ftp.*;

import com.nightfire.framework.util.*;
import com.nightfire.comms.util.*;

/**
 *  SSL FTP implementation of a FtpDataConnect interface.
 */
public class FtpSSLDataClient extends FtpDataClient
{

    private SSLConfigInfo info;
    private  SSLSocketFactory factory = null;
    private  SSLServerSocketFactory serverFactory = null;

    private String initError = null;

    // handshake listener
    private static final HandshakeListener listener = new HandshakeListener();



    public FtpSSLDataClient(SSLConfigInfo info)
    {
        super();
        this.info = info;
        ftpC.setSocketFactory(new FtpSSLSocketFactory());
        init();

    }

    private void init()
    {
	    try {
           char[] passphrase = info.getKeyStorePassPhrase().toCharArray();

            // register a provider for ssl
            Security.addProvider(info.getProvider());

            //System.setProperty("javax.net.ssl.trustStore", info.getTrustStore());
            //doesn't seem to need the password for the trust store
            //System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

            SSLContext ctx = SSLContext.getInstance(info.getSSLContextVersion());
            // set up key managers
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(info.getKeyManagementAlgorithm());


            KeyStore ks = KeyStore.getInstance(info.getKeyStoreType());
            ks.load(new FileInputStream(info.getKeyStore()), passphrase);
            kmf.init(ks, passphrase);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(info.getKeyManagementAlgorithm());

            KeyStore ts = KeyStore.getInstance("jks");
            ts.load(new FileInputStream(info.getTrustStore()), null);
            tmf.init(ts);

            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            //ctx.init(kmf.getKeyManagers(),null, null);
            factory = ctx.getSocketFactory();
            serverFactory = ctx.getServerSocketFactory();

        } catch (Exception e) {
            initError = "Failed to set up ssl configuration : " +e.getMessage();
	    }
    }


    private java.net.Socket createSSLSocket(java.net.Socket old) throws IOException
    {

        SSLSocket newSoc = null;


        BufferedReader s_in =
                  new BufferedReader(
				    new InputStreamReader(
				      old.getInputStream()));

        PrintWriter s_out= new PrintWriter(
				  new BufferedWriter(
				  new OutputStreamWriter(
     				  old.getOutputStream())));


        // get initial server response on connect
        int replyCode = getLastReplyCode(s_in);
         if(FTPReply.isNegativePermanent(replyCode) ||
            FTPReply.isNegativeTransient(replyCode) ) {
             throw new IOException("Command not supported.");
         }

         // indicate a secure connection
         sendAUTH(s_out, s_in);

         // ssl handshake starting

	    try {

            // lay ssl over the existing socket

            // if the socket it null the initialization failed
            if ( factory == null )
                throw new IOException(initError);

            // replace the ftp socket with an ssl socket
            // the old socket will get closed automatically when closing the
            // ssl socket

            newSoc = (SSLSocket)factory.createSocket(old,
                                     old.getInetAddress().getHostAddress(),
                                                     old.getPort(), true);




            newSoc.addHandshakeCompletedListener(listener);

            // forces the start of a handshake
            // if this is not done no exception will
            // be thrown if an error occurs since
            // the PrintWriter buffers all errors.
            newSoc.startHandshake();

            BufferedReader s_newi = new BufferedReader(
                                               new InputStreamReader(
				                               newSoc.getInputStream()));

            PrintWriter s_newo= new PrintWriter(
                                                new BufferedWriter(
                                                new OutputStreamWriter(
     				                            newSoc.getOutputStream())));


            // note this commands must occur after ssl handshake

            // indicate to turn off buffering
            sendPBSZ(s_newo, s_newi);

            // indicate to protect the data channel
            sendPROT(s_newo, s_newi);

            // NEEDED to prevent hanging.
            // The ftp api that uses the returned socket expects to be
            // able to get a connection succesful response from
            // the server imediately.
            // Since some other commands had to be sent the original server
            // response is no longer there, so we
            // send a noop command and let the ftp libarary see this success
            // command instead.
            sendCommand("NOOP", "", s_newo);
        } catch (Exception e) {
            Debug.error("Failed to create ssl socket : " +e.getMessage());
            throw new IOException("Failed to create ssl socket : " +e.getMessage());
	    }

        return newSoc;

    }


    private void sendPROT(PrintWriter s_out, Reader s_in) throws IOException
    {
         sendCommand("PROT", "P", s_out);
         int replyCode = getLastReplyCode(s_in);

         if(FTPReply.isNegativePermanent(replyCode) ||
            FTPReply.isNegativeTransient(replyCode) ) {
             throw new IOException("Command not supported.");
         }
    }

    private void sendPBSZ(PrintWriter s_out, Reader s_in) throws IOException
    {
         sendCommand("PBSZ", "0", s_out);
         int replyCode = getLastReplyCode(s_in);

         if(FTPReply.isNegativePermanent(replyCode) ||
            FTPReply.isNegativeTransient(replyCode) ) {
             throw new IOException("Command not supported.");
         }
    }

    private void sendAUTH(PrintWriter s_out, Reader s_in) throws IOException
    {
         sendCommand("AUTH", "TLS", s_out);

         int replyCode = getLastReplyCode(s_in);

         if(FTPReply.isNegativePermanent(replyCode) ||
            FTPReply.isNegativeTransient(replyCode) ) {
             throw new IOException("Command not supported.");
         }

    }


    /**
     * Sends a command to the remote ftp server over
     * the specified writer
     */
    private void sendCommand(String command, String args, PrintWriter out) throws IOException
    {
        String msg =  command + " " + args +" \r\n";

        out.print(msg);
        out.flush();
        // fires off a sent command event
        fireCommandSentEvent(command,msg);

    }


    /**
     * Gets the last reply code. Should be used after using the
     * send command
     */
    private int getLastReplyCode(Reader s_in) throws IOException
    {
        String lastReply;

        StringBuffer msg = new StringBuffer();

        char[] buffer = new char[256];

        int read = -1;


        while ( (read = s_in.read(buffer,0, buffer.length)) > -1 ) {
            msg.append(buffer,0, read);
            if ( read < buffer.length)
                break;

        }

        lastReply = msg.toString();

        // obtains the reply code
        int code =-1;
        try {
            if (StringUtils.hasValue(lastReply) && lastReply.length() > 3)
                code = Integer.parseInt(lastReply.substring(0,3) );
        }
        catch (Exception e) {
            Debug.error("Could not parse ftp reply [" + lastReply +"]: " + e.getMessage() );
            throw new IOException(e.getMessage());
        }

        fireReplyReceivedEvent(code, lastReply);

        return code;

    }

    // socket factory that returns an ssl socket to the ftp libs
    class FtpSSLSocketFactory extends org.apache.commons.net.DefaultSocketFactory
    {

        public FtpSSLSocketFactory()
        {
        }

        public java.net.ServerSocket createServerSocket(int param) throws java.io.IOException
        {
            if ( serverFactory == null )
                throw new IOException(initError);

            try {

                SSLServerSocket soc = (SSLServerSocket)serverFactory.createServerSocket(param);
                return configServerSoc(soc);
             }
            catch (IOException e){
                Debug.error("Failed to create ssl server socket : "+ e.getMessage() );
                throw e;

            }

        }

        public java.net.ServerSocket createServerSocket(int param,int param1) throws java.io.IOException
        {
            if ( serverFactory == null )
                    throw new IOException(initError);

            try {

                SSLServerSocket soc = (SSLServerSocket)serverFactory.createServerSocket(param, param1);
                return configServerSoc(soc);
            }
            catch (IOException e){
                Debug.error("Failed to create ssl server socket : "+ e.getMessage() );
                throw e;

            }

        }

        public java.net.ServerSocket createServerSocket(int param,int param1,java.net.InetAddress inetAddress) throws java.io.IOException
        {

            if ( serverFactory == null )
                    throw new IOException(initError);
            try {

                SSLServerSocket soc = (SSLServerSocket)serverFactory.createServerSocket(param, param1, inetAddress);

                return configServerSoc(soc);
            }
            catch (IOException e){
                Debug.error("Failed to create ssl server socket : "+ e.getMessage() );
                throw e;

            }
        }

        private SSLServerSocket configServerSoc(SSLServerSocket soc)
        {
            //need to run a server socket in client mode
            // so that we can send some ssl commands back to the remote
            // ftp server
            soc.setUseClientMode(true);

            return soc;
        }


        public java.net.Socket createSocket(java.lang.String str,int param) throws java.net.UnknownHostException, java.io.IOException
        {
            java.net.Socket soc = super.createSocket(str, param);
            return createSSLSocket(soc);
        }

        public java.net.Socket createSocket(java.net.InetAddress inetAddress,int param) throws java.io.IOException
        {
            java.net.Socket soc = super.createSocket(inetAddress, param);
            return createSSLSocket(soc);
        }

        public java.net.Socket createSocket(java.lang.String str,int param,java.net.InetAddress inetAddress,int param3) throws java.net.UnknownHostException, java.io.IOException
        {
            java.net.Socket soc = super.createSocket(str, param, inetAddress, param3);
            return createSSLSocket(soc);
        }

        public java.net.Socket createSocket(java.net.InetAddress inetAddress,int param,java.net.InetAddress inetAddress2,int param3) throws java.io.IOException
        {
            java.net.Socket soc = super.createSocket(inetAddress, param, inetAddress2, param3);
            return createSSLSocket(soc);
        }
    }


    // a class that listens for a successful handshake and does some logging
    private static final class HandshakeListener
        implements HandshakeCompletedListener
    {
        public void handshakeCompleted(HandshakeCompletedEvent e)
            {
                if ( !Debug.isLevelEnabled(Debug.IO_STATUS) )
                        return;

                StringBuffer info = new StringBuffer();

                info.append("\nSSL Handshake Completed:");
                info.append("\n Cipher suite: " + e.getCipherSuite());

                info.append("\n Peer Host : " + e.getSession().getPeerHost());
                Certificate[] cert = null;

                try {
                    cert   = e.getPeerCertificates();
                } catch (Exception ex) {
                    Debug.log(Debug.IO_STATUS, "Could not obtain peer cert chain: " + ex.getMessage() );
                }


                info.append("\n Received Certificate Chain :\n");

                for (int i = 0; i < cert.length; i++ ) {
                    X509Certificate curCert = (X509Certificate) cert[i];
                    

                    info.append("\n ------------------------");

                    info.append("\n Issuer distinguished name : " + curCert.getIssuerDN().getName());

                    info.append("\n Serial Num : " + curCert.getSerialNumber().toString());

                    info.append("\n Signature Algorithm name : " + curCert.getSigAlgName() );

                    info.append("\n Signature Algorithm OID : " + curCert.getSigAlgOID() );

                    info.append("\n Subject distinguished name : " + curCert.getSubjectDN() );

                    info.append("\n Version : " + curCert.getVersion());


                    info.append("\n ------------------------");
                }

                Debug.log(Debug.IO_STATUS, info.toString() );


            }
    }

}



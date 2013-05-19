package com.nightfire.comms.ia;

// JDK import
import java.net.*;
import java.io.*;
import java.util.*;

// Nightfire import
import com.nightfire.framework.db.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.adapter.messageprocessor.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.adapter.util.*;
import com.nightfire.comms.ia.asn.msg.*;
import com.nightfire.comms.ia.asn.ASNData;


/**
 * This class takes a String/DOM as an input and returns null.
 * (last message processor)
 * It operates on information in the
 * context. The main function on this method is for sending data to a trading
 * partner's IA server.
 */
public class IAClient extends MessageProcessorBase
{
   /**
    * IP address of the machine where the IA server is running.
    */
   private static final String REMOTE_IA_HOST_PROP = "REMOTE_IA_HOST";

   /**
    * Port number of the machine where the IA server is running.
    */
   private static final String REMOTE_IA_PORT_NUMBER_PROP = "REMOTE_IA_PORT_NUMBER";

   /**
    * Location of the OID (Object Identifier) value.
    */
   private static final String BASIC_EDI_OID_PROP = "BASIC_EDI_OID";

   /**
    * Location of the SSL information.
    */
   private static final String SSL_TYPE_PROP = "SSL_TYPE";

    /**
     * The property name for the character encoding to use.
     */
    private static final String CHAR_ENCODING_PROP = "CHARACTER_ENCODING";

    /**
     * Amount of overhead to allow for DER encoding of ASN.1 structure in
     * the buffer
     */
    private static final int BUFFER_PADDING = 100;

   /**
    * IP address of the machine where the IA server is running.
    */
   private String remoteHost = null;

   /**
    * Port number of the machine where the IA server is running.
    */
   private String remotePortNumber = null;

   /**
    * Location of the OID (Object Identifier) value.
    */
   private String oidValue = null;

   /**
    * SSL Portability layer
    */
    private static Map sslLayers = new HashMap();

    private SSLPortabilityLayer ssl = null;




   private int portNumber = 0;

    /**
     * The character encoding to use
     */
    private String enc = null;

   /**
    * Map for Thread Monitoring : key = customerID and value = current count of sockets opened.
    */
    private static Map customerSockets = new HashMap( );

  /**
   * Retrieves properties from the database. These properties specify
   * the information for processing.
   *
   * @param key   A string which acts as a pointer to service provider
   *              and order type information in the database.
   * @param type  A string which specifies the type of processor this is.
   * @exception ProcessingException   Thrown if the specified properties
   *                                  cannot be found or are invalid.
   */
   public void initialize(String key, String type) throws ProcessingException
   {
      super.initialize(key, type);

      // turn on Cryptix debug logging
      IAServer.initCryptixLogging();

      // Initialize a string buffer to contain all errors
      StringBuffer errorMessage = new StringBuffer();

      // Getting the necessary values from the persistent properties
      remoteHost       = getRequiredPropertyValue(REMOTE_IA_HOST_PROP, errorMessage);
      remotePortNumber = getRequiredPropertyValue(REMOTE_IA_PORT_NUMBER_PROP, errorMessage);
      String sslType   = getRequiredPropertyValue(SSL_TYPE_PROP, errorMessage);
      oidValue         = getRequiredPropertyValue(BASIC_EDI_OID_PROP, errorMessage);

      String addrPortKey = remoteHost +":" + remotePortNumber;

      // initializing SSL

          synchronized (sslLayers) {
              if (!sslLayers.containsKey(addrPortKey) ) {
                  ssl = new SSLPortabilityLayer();
                  ssl.initialize(key, sslType);
                  sslLayers.put(addrPortKey, ssl);
              } else {
                  ssl = (SSLPortabilityLayer)sslLayers.get(addrPortKey);
              }


          }


      // converting string to integer for getSocket().
      try
      {
         portNumber = Integer.parseInt(remotePortNumber);
      }
      catch(NumberFormatException nfe)
      {
          errorMessage.append("IAClient: \n").
              append("ERROR: Failed to covert string formatted remotePortNumber to ").
              append("intger format. ").append(nfe.getMessage()).
              append("\n");
      }

      // get the optional character encoding to use
      enc = getPropertyValue(CHAR_ENCODING_PROP);




      // redirect ssl logging to debug
      if( !(System.out instanceof StdOutPrintStream) )
          System.setOut(new StdOutPrintStream() );


      Debug.log( Debug.IO_STATUS,
                 "IAClient: " + "\n" +
                 "   remoteHost = " + remoteHost + "\n" +
                 "   remotePortNumber = " + portNumber + "\n" +
                 "   sslType = " + sslType + "\n" +
                 "   oidValue = " + oidValue + "\n" );


      if( errorMessage.length() > 0 )
      {
         // Some error occured during processing above
         Debug.log( Debug.ALL_ERRORS, "ERROR: IAClient: " +
                    "Failed initialization " + errorMessage.toString() );

         throw new ProcessingException( "ERROR: IAClient: " +
                    "Failed initialization " + errorMessage.toString() );
      }

   }

  /**
   * Connects to the remote IA server using SSL, then sends EDI(input) to it.
   * Returns a NVPair[](name/value pair).
   *
   * @param   input  Input message to process.
   * @param   mpc The context that stores control information
   * @return  Optional NVPair containing a Destination name and a Document,
   *          or null if none.
   * @exception  ProcessingException  Thrown if processing fails.
   * @exception  MessageException  Thrown if bad message.
   */
   public NVPair[] process(MessageProcessorContext mpc, MessageObject input)
      throws MessageException, ProcessingException
   {
      if( input == null )
      {
         return null;
      }

      try
      {
          sendEDI(remoteHost, portNumber, input.getString());
      }
      catch(IOException ioe)
      {
         throw new ProcessingException( "IAClient: " + "\n" +
                    "ERROR: Failed to send EDI message." + ioe.getMessage() );
      }

       // Always return input value to provide pass-through semantics.
       return( formatNVPair( input ) );
   }

    /**
     * Sends an EDI message to a remote host.
     *
     * @param remoteHost The host to send to
     * @param portNumber The port number on the host to connect to
     * @param msg        The message to output
     */

    private void sendEDI(String remoteHost, int portNumber, String msg)
        throws IOException, ProcessingException, MessageException
    {
		//Thread Monitoring
		ThreadMonitor.ThreadInfo tmti = null;

        //Get CustomerId for monitoring Sockets per CID
		String cid = null;
	
		try 
		{
			cid = CustomerContext.getInstance().getCustomerID();
		} 
		catch (FrameworkException e) 
		{
			Debug.log(Debug.ALL_ERRORS, "ERROR: Failed to get CustomerId from CustomerContext: " + e);
		}

		String cidSocketString = null;

        // Wrap all operations in synchronized block against global Map.
		synchronized ( customerSockets )
        {
			Integer socketCount = (Integer)customerSockets.get(cid);

        // If current socket count for this customer is zero, create a new entry in customerSockets map.
            if(socketCount == null)
            socketCount = new Integer(1);
		else 
                socketCount =  new Integer( socketCount.intValue() + 1 );
            customerSockets.put( cid, socketCount );

          // Get string representation of map within synchronized block as well so you don't get a
          // ConcurrentModificationException when toString() iterates over a container that is being
          // concurrently modified.
          cidSocketString = customerSockets.toString();
		}

        // create a new basic message
        IABasicEDI iaBasicEdi = new IABasicEDI(oidValue);
        if (StringUtils.hasValue(enc))
            iaBasicEdi.setEncoding(enc);

        Debug.log(this, Debug.MSG_DATA, "EDI Length: " + msg.length());

        // set the message
        iaBasicEdi.setEDIContent(msg);

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(this, Debug.MSG_DATA, "EDI Data: " + msg);

        ByteArrayOutputStream buf = null;
        Socket socket = null;
        OutputStream os = null;
        
        try
        {
            	    
	    // code it to a temporary buffer
            buf = new ByteArrayOutputStream(msg.length() + BUFFER_PADDING);
            iaBasicEdi.encode(ASNData.DER_CODING, buf);
            byte[] data = buf.toByteArray();

	    long currentTime = System.currentTimeMillis( );
            long startTime = System.currentTimeMillis( );
	    // connect
            socket = ssl.getSocket(remoteHost, portNumber);
			
		//Start a monitor with per Customer socket usage and Socket endpoint info
            tmti = ThreadMonitor.start( "IAClient: Sending EDI request to TP. \n  Remote endpoint info : " +socket.toString()
                                + "\n  Local endpoint info :" + socket.getLocalSocketAddress() + "\n  Per customer socket usage: " + cidSocketString);
            
			Debug.log( Debug.BENCHMARK, "IAClient create socket: "
                        + " ELAPSED TIME [" + (System.currentTimeMillis() - currentTime) + "] msec." );

            os = new SnoopOutputStream (new BufferedOutputStream(socket.getOutputStream() ));

            Debug.log(this, Debug.IO_STATUS, "Connected to host: "
                      + remoteHost);

            currentTime = System.currentTimeMillis( );
	    // send it
            os.write(data);

	    long done = System.currentTimeMillis( );

            Debug.log( Debug.BENCHMARK, "EDI successfully sent: "
                        + " ELAPSED TIME [" + (done - currentTime) + "] msec." );

	    Debug.log( Debug.BENCHMARK, "IAClient Total time: "
                        + " ELAPSED TIME [" + (done - startTime) + "] msec." );

	    
        }
        finally
        {
            // clean up any of the socket resources
            try
            {
                if (buf != null)
                    buf.close();
                if (os != null)
                    os.close();
                if (socket != null)
                    socket.close();
            }
            catch (IOException e)
            {
                Debug.log(Debug.ALL_ERRORS, "ERROR: IAClient: " + e);
            }

            // Wrap all operations in synchronized block against global Map.
            synchronized ( customerSockets )
			{
				Integer socketCount = (Integer)customerSockets.get(cid);

            if ( socketCount == null)
                Debug.warning( "Warning: There was a mismatch in per CID socket counts for Customer["+cid+"].");
            else
            {
					int count = socketCount.intValue() - 1;

                if(count == 0)
                    customerSockets.remove(cid);
                else
                    customerSockets.put( cid, new Integer(count) );
            }
			}

          ThreadMonitor.stop(tmti);
        }
    }
}

/*
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.socket;

import java.io.*;
import java.net.*;
import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;

import com.nightfire.spi.common.driver.MessageProcessorContext;



/*
 * This is the concrete class that implements socket connection to Ameritech via EAP
 * (Enterprise Access Protocol) for pre-ordering inquiry. 
 */


public class EAPCommAdapter extends SocketCommAdapter {

    // public static final String SOURCE_ID = "$Id: //gateway/main/com/nightfire/spi/common/communications/eap/EAPCommAdapter.java#11 $";

    private String ipAddress;             //IP address of the EAP server
    private String portNum;               //port number of the EAP server
    private String eapUser;               //EAP server user name
    private String eapPassword;           //EAP server user password
    private String recvCode;              //receiver code
    private String snrf;                  //user id, same as eapUser
    private String transType;             //transaction type: DSL, ADD, etc.
    private String serverTimeout;         //how long do we want to wait for response from EAP server
    private String socketConnTimeout;     //socket connection setup timeout.
    private String socketIOTimeout;       //socket I/O blocking timeout.

    private String snrfNumberSequence;    //sequence for getting unique snrf number
    private String snrfNumber;            //unique serial number

    private String processorName;         //name of this component
    private String nextProcessorName;     //Name of the next processor

    private PrintWriter commSend = null;     //output stream
    private BufferedReader commRead = null;  //input stream

    private String ackString;             //response from EAP server
    private String ediString;             //edi string

    private static final String ACK = "*ACK";      //EAP *ACK
    private static final String NAK = "*NAK";      //EAP *NAK
    private static final String END_STRING = "<<<<!!!!>>>>";   //EAP end string
    private static final long ONE_SECOND = 1000;

    //Properties
    public static final String EAP_IP_ADDRESS = "EAP_IP_ADDRESS";
    public static final String EAP_PORT_NUMBER = "EAP_PORT_NUMBER";
    public static final String EAP_USER = "EAP_USER";
    public static final String EAP_PASSWORD = "EAP_PASSWORD";
    public static final String EAP_RECV_CODE = "EAP_RECV_CODE";
    public static final String EAP_TRANS_TYPE = "EAP_TRANS_TYPE";
    public static final String NEXT_PROCESSOR_NAME_PROP = "NEXT_PROCESSOR_NAME";
    public static final String EAP_SNRF = "EAP_SNRF";
    public static final String EAP_SNRF_NUMBER_SEQUENCE = "EAP_SNRF_NUMBER_SEQUENCE";    
    public static final String EAP_SOCKET_CONN_TIMEOUT = "EAP_SOCKET_CONN_TIMEOUT";
    public static final String EAP_SOCKET_IO_TIMEOUT = "EAP_SOCKET_IO_TIMEOUT";
    public static final String EAP_SERVER_TIMEOUT = "EAP_SERVER_TIMEOUT";
    public static final String PROCESSOR_NAME = "NAME";


    /**
     * Constructor used by Factory to create this object. Get a unique SNRF number.
     */
    public void initialize(String key, String type) throws ProcessingException
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Initializing the EAP communication adapter.");

        //readConfigFile();    //for testing: read properties from a file
        super.initialize(key,type);

        ipAddress = getStringFromPropertyName(EAP_IP_ADDRESS);
        portNum = getStringFromPropertyName(EAP_PORT_NUMBER);
        eapUser = getStringFromPropertyName(EAP_USER);
        eapPassword = getStringFromPropertyName(EAP_PASSWORD);
        recvCode = getStringFromPropertyName(EAP_RECV_CODE);
        snrf = getStringFromPropertyName(EAP_SNRF);
        transType = getStringFromPropertyName(EAP_TRANS_TYPE);
        serverTimeout  = getStringFromPropertyName(EAP_SERVER_TIMEOUT);
        socketConnTimeout  = getStringFromPropertyName(EAP_SOCKET_CONN_TIMEOUT);
        socketIOTimeout  = getStringFromPropertyName(EAP_SOCKET_IO_TIMEOUT);

        //Get unique SNRF
        snrfNumberSequence = getStringFromPropertyName(EAP_SNRF_NUMBER_SEQUENCE);
        snrfNumber = getSNRFNumber();
        /*
        snrfNumber = getSNRFFromFile();  //for testing
        Debug.log(Debug.NORMAL_STATUS, "SNRF is: " + snrfNumber);
        if( snrfNumber.length() != 6) {
            Debug.log(Debug.ALL_ERRORS, "snrfNumber construction failed.");
        }
        */

        //Other initialization
        processorName = getStringFromPropertyName(PROCESSOR_NAME);
        nextProcessorName = getStringFromPropertyName(NEXT_PROCESSOR_NAME_PROP);

    }

    /**
     * Get property value
     *
     * @param   PropName     Name of the property.
     *
     * @return  String containing the value of the property.
     *
     * @exception  ProcessingException    Thrown when no value can be found for the property.    
    */
    public String getStringFromPropertyName(String PropName) throws ProcessingException
    {
        String s = (String) adapterProperties.get(PropName);
        if( s==null)
            throw new ProcessingException( "ERROR: EAPCommAdapter.getStringFromPropertyName: Null values found for property " + PropName );
        else {
            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, "EAPCommAdapter.getStringFromPropertyName: Property " + PropName + " is set to " + s );
            
            return s;
        }
    }


    /**
     * Get a unique 6-digit serial number as the second part of SNRF. The first part
     * of SNRF is the same as userid.
     *
     * @return  String containing the unique 6-digit serial number.
     *
     * @exception  ProcessingException    Thrown if fail to use PersistentSequence to get the number.
    */
    public String getSNRFNumber() throws ProcessingException
    {
        int snrfNumberBase;
        try {
            snrfNumberBase = PersistentSequence.getNextSequenceValue(snrfNumberSequence);
        } catch (DatabaseException e) {
            throw new ProcessingException( "ERROR: EAPCommAdapter.getSNRFNumber: Cannot retrieve persistent " +
                         "sequence for " + snrfNumberSequence + "\n" + e.getMessage());
        }

        snrfNumberBase =  snrfNumberBase % 1000000;

        String snrfNumberBaseString = Integer.toString(snrfNumberBase);
        String snrfNumberString = StringUtils.padString(snrfNumberBaseString, 6, true, '0');

        if(Debug.isLevelEnabled(Debug.MSG_BASE))
            Debug.log(Debug.MSG_BASE, "EAPCommAdapter.getSNRFNumber: The unique number in SNRF is: " + snrfNumberString);
        
        return snrfNumberString;
    }


    /**
     * Main method to setup socket connection with Ameritech and conmmunicate with it.
     *
     * @param   mpcontext     MessageProcessorContext.
     *
     * @param  input  The input object to be processed.
     *
     * @return  NVPair[] containing the name(s) of the next processor(s) and the processed object.
     *
     * @exception  ProcessingException    Thrown when any communication and I/O problems occur.    
    */
    public NVPair[] execute(MessageProcessorContext mpcontext, java.lang.Object input)
                throws MessageException, ProcessingException
    {
        if (input==null) 
        return null;        

        if ( input instanceof String )
        {

            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE,
                        "EAPCommAdapter.process: Start processing, try to connect to EAP server...");
            
            connect(ipAddress, portNum, socketConnTimeout);
            
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Server connected.");

            //Create writer and reader
            commSend = new PrintWriter(commOut, true);
            commRead = new BufferedReader( new InputStreamReader( commIn ) );

            //Set I/O blocking timeout
            int socketIOTimeoutINT = Integer.parseInt(socketIOTimeout);
            try {
                commSocket.setSoTimeout(socketIOTimeoutINT);
                
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, 
                            "EAPCommAdpater.process: commSocket time out set to " + socketIOTimeoutINT/1000 + " seconds");
            } catch (SocketException e) {
                Debug.log(Debug.ALL_ERRORS, "ERROR: EAPCommAdpater.process: cannot set setSoTimeout" + e.getClass());
                throw new ProcessingException("ERROR: EAPCommAdpater.process: cannot set setSoTimeout" + e.getClass());
            }

            //EAP dialog begins
            try {
                //Signon
                
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Try signon ...");
                
                sendSignon();

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Expected response: *ACK()");
                
                ackString = readOneLine();

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Get response: " + ackString);
                
                if( !ackString.startsWith(ACK) ) {
                    quit();
                    throw new ProcessingException("EAPCommAdpater.process:  Did not get ACK after signon");
                }

                //Send header, the first part of *Send message
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Try to send header");
                
                sendHeader();

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Expected response: *ACK()");
                ackString = readOneLine();

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Get response: " + ackString);
                if( !ackString.startsWith(ACK) ) {
                    quit();
                    throw new ProcessingException("EAPCommAdpater.process: Did not get ACK after sendHeader");
                }

                //Send EDI with <<<<!!!!>>>>
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Try to send edi message + " + END_STRING);
                send(input + END_STRING);

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Expected response: *ACK()");
                
                ackString = readOneLine();

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Get response: " + ackString);
                
                if( !ackString.startsWith(ACK) ) {
                    quit();
                    throw new ProcessingException("EAPCommAdpater.process: Did not get ACK after sending EDI");
                }

                //Poll server for response.
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Try to send *Receive");
                
                sendReceive();
                
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Expected response: *ACK(). If *NAK, go into the while loop");
                
                ackString = readOneLine();

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Get response: " + ackString);

                int counter = 1;  //set while loop counter
                int serverTimeoutINT = Integer.parseInt(serverTimeout);  //set while loop timeout

                while( ackString.startsWith(NAK) ) {
                    
                    if(Debug.isLevelEnabled(Debug.MSG_BASE))
                        Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: In while loop: ");
                    
                    if(counter == serverTimeoutINT) {
                        Debug.log(Debug.ALL_ERRORS, "EAPCommAdpater.process: Timed out: no response from EAP server after sending EDI."
                          + "\n  Increasing " + EAP_SERVER_TIMEOUT + " property value may solve the problem.");
                        quit();
                        throw new ProcessingException("EAPCommAdpater.process: Timed out: no response from EAP server after sending EDI");
                    }
                    try {
                        
                        if(Debug.isLevelEnabled(Debug.MSG_BASE))
                            Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Tried " + counter + " time(s) for response. Thread goes sleep for one second...");
                        
                        Thread.currentThread().sleep(ONE_SECOND);
                    } catch ( InterruptedException e ) {
                        Debug.log(Debug.ALL_ERRORS, "ERROR: EAPCommAdpater.process: Thread cannot go sleep: " + e.getMessage());
                        throw new ProcessingException("ERROR: EAPCommAdpater.process: Thread cannot go sleep: " + e.getMessage());
                    }
                    
                    if(Debug.isLevelEnabled(Debug.MSG_BASE))
                        Debug.log(Debug.NORMAL_STATUS, "EAPCommAdpater.process: Try to send *Receive again");
                    
                    sendReceive();

                    ackString = readOneLine();
                    
                    if(Debug.isLevelEnabled(Debug.MSG_BASE))
                        Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Get response again: " + ackString);
                    
                    counter++;
                }

                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                {
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Get *ACK(). The following should be EDI response.");
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Try to receive EDI response...");
                }
                sendReceive();

                ackString = readStream();
                
                if(Debug.isLevelEnabled(Debug.MSG_BASE))
                    Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Get EDI response: " + ackString);

                if(ackString.endsWith(END_STRING)) {
                    // Trim  "<<<<!!!!>>>>" at the end and add line separator.
                    ediString = ackString.substring(0, ackString.length() - END_STRING.length() - 1)
                        + System.getProperty("line.separator");

                    if(Debug.isLevelEnabled(Debug.MSG_BASE))
                        Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: EDI message: \n[" + ediString +"]");
                    
                    if (!((processorName.trim()).equalsIgnoreCase("NOBODY"))) {
                        NVPair[] nvpair = new NVPair[1];
                        nvpair[0] = new NVPair(nextProcessorName, ediString);
                        //disconnect
                        quit();
                        
                        if(Debug.isLevelEnabled(Debug.MSG_BASE))
                            Debug.log(Debug.MSG_BASE, "EAPCommAdpater.process: Process is done.");
                        
                        return (nvpair);
                    }
                }
                else {
                    Debug.log(Debug.ALL_ERRORS, "EAPCommAdpater.process: Return dose not end with " + END_STRING);
                    quit();
                    throw new ProcessingException("EAPCommAdpater.process: Return dose not end with " + END_STRING);
                }

            } catch (FrameworkException fe)
                  {
                    Debug.log(Debug.ALL_ERRORS, "ERROR: EAPCommAdpater.process: Cannot process: " + fe.getMessage());
	                  throw new ProcessingException("ERROR: EAPCommAdpater.process: Cannot process: " + fe.getMessage());
                  }
        }
        else
        {
            throw new ProcessingException("ERROR: EAPCommAdapter.process: Invalid input message (Non-String)");
        }

        return null;
    }


    /**
     * Read one line from the socket.
     *
     * @return  String containing one line.
     *
     * @exception  ProcessingException    Thrown when reading from socket fails.
    */
    public String readOneLine() throws ProcessingException {

      if( commRead != null ) {
        String inputString = null;
        try {
          inputString = commRead.readLine();
        } catch ( IOException e ) {
          Debug.log(Debug.ALL_ERRORS, "ERROR: EAPCommAdapter.readOneLine: " + e.getMessage()
            + "\nIf timed out, increasing " + EAP_SOCKET_IO_TIMEOUT +
            " property value may solve the problem.");
          throw new ProcessingException("ERROR: EAPCommAdapter.readOneLine: " + e.getMessage()
            + "\nIf timed out, increasing " + EAP_SOCKET_IO_TIMEOUT +
            " property value may solve the problem.");
        }
        return inputString;
      }
      else{
        Debug.log(Debug.ALL_ERRORS, "ERROR: EAPCommAdapter.readOneLine: TCPComm error: commRead is null" );
        throw new ProcessingException("ERROR: EAPCommAdapter.readOneLine: TCPComm error: commRead is null" );
      }
    }


    /**
     * Read from socket until END_STRING is detected.
     *
     * @return  String containing edi message plus END_STRING.
     *
     * @exception  ProcessingException    Thrown when reading from socket fails.
    */
    public String readStream() throws ProcessingException
    {
      StringBuffer sb = new StringBuffer();
      boolean foundEnd = false;
      String replyString = null;
      byte[] byteArray = new byte[1024]; 

      try {
        do{   // read buffer until foundEnd == true
          //read directly from InputStream into byteArray
          int numRead = commIn.read(byteArray, 0, byteArray.length);
          //Debug.log(Debug.NORMAL_STATUS, "EAPCommAdapter.readStream: Number of bytes just read " + numRead);

          //convert numRead of bytes of byteArray to stringRead
          String stringRead = new String(byteArray, 0, numRead);
          //Debug.log(Debug.NORMAL_STATUS, "EAPCommAdapter.readStream: stringRead: " + stringRead);

          sb.append(stringRead);  //append string just read to the string buffer

          int sbLength = sb.toString().length();  //length of the string buffer

          int iStart = 0;  //index where search for END_STRING should stop
          if( (sbLength - stringRead.length()) >= END_STRING.length() )
            iStart = sbLength - stringRead.length() - END_STRING.length();

          //Debug.log(Debug.NORMAL_STATUS, "EAPCommAdapter.readStream: iStart = " + iStart);
          
          //search for END_STRING from the end of the string buffer
          for( int i = sbLength - END_STRING.length(); i > iStart; i-- ) {
            //if( sb.toString().substring(i, sbLength).startsWith(END_STRING) ) {
            if( sb.toString().substring(i, i + END_STRING.length()).startsWith(END_STRING) ) {
              Debug.log(Debug.NORMAL_STATUS, "find END_STRING at " + i);
              replyString = sb.toString().substring(0, i + END_STRING.length());
              foundEnd = true;
              break;
            }
          }

          stringRead = null;

          if(foundEnd)
            break;

        } while (true);

      } catch ( IOException e ) {
        System.err.println("ERROR: EAPCommAdapter.readStream: Cannot read stream: " + e.getMessage());
      }
      
      return replyString;
    }


    /**
     * Write string to socket.
     *
     * @param  msg  String need to be sent.
     *
     * @exception  ProcessingException    Thrown when writing to socket fails.
    */
    public void send(String msg) throws ProcessingException
    {
        if(commSend != null)
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE, "EAPCommAdapter.send: commSend is not null, send message: " + msg);
            
            commSend.println(msg);
            //Debug.log(Debug.NORMAL_STATUS, "EAPCommAdapter.send: commSend completes: " + msg);
    }


    /**
     * Send *Signon string
     *
    */
    public void sendSignon() throws ProcessingException
    {
        send( "*Signon(USER=" + eapUser + ",PASSWORD=" + eapPassword + ")" );
        
        if(Debug.isLevelEnabled(Debug.MSG_BASE))
        	Debug.log(Debug.MSG_BASE, "EAPCommAdapter.sendSignon: *Signon(USER=" + eapUser + ")" );
    }


    /**
     * Send the first part of *Send string
     *
    */
    public void sendHeader() throws ProcessingException
    {
        send( "*Send(REC=" + recvCode + ",SNRF=" + snrf + snrfNumber + ",APRF=" + transType + ")" );
        
        if(Debug.isLevelEnabled(Debug.MSG_BASE))
            Debug.log(Debug.MSG_BASE, "EAPCommAdapter.sendHeader: *Send(REC=" + recvCode + ",SNRF=" + snrf + snrfNumber + ",APRF=" + transType + ")" );
    }


    /**
     * Send *Receive string
     *
    */
    public void sendReceive() throws ProcessingException
    {
        send( "*Receive(SNRF=" + snrf + snrfNumber + ")" );
        
        if(Debug.isLevelEnabled(Debug.MSG_BASE))
            Debug.log(Debug.MSG_BASE, "EAPCommAdapter.sendReceive: *Receive(SNRF=" + snrf + snrfNumber + ")" );
    }


    /**
     * Close stream interfaces and socket.
     *
     * @exception  ProcessingException    Thrown when stream interfaces cannot be closed.
     *
    */
    public void quit() throws ProcessingException
    {
        send( "*Quit" );
        
        if(Debug.isLevelEnabled(Debug.MSG_BASE))
            Debug.log(Debug.MSG_BASE, "EAPCommAdapter.quit: sent *Quit. Try to close socket connection.");
        
        try {
          if( commRead != null ) {
            commRead.close();
            Debug.log(Debug.MSG_BASE, "EAPCommAdapter.quit: commRead closed.");
          }
          if( commSend != null ) {
            commSend.close();
            Debug.log(Debug.MSG_BASE, "EAPCommAdapter.quit: commSend closed.");
          }
        } catch (IOException e) {
            Debug.log(Debug.ALL_ERRORS, "ERROR: EAPCommAdapter.quit: Cannot close comm connection." + e.toString());
            throw new ProcessingException("ERROR: EAPCommAdapter.quit: Cannot close comm connection." + e.toString());
        }

        disconnect();
        
        if(Debug.isLevelEnabled(Debug.MSG_BASE))
            Debug.log(Debug.MSG_BASE, "EAPCommAdapter.quit: EAP communication disconnected.");
    }


    /*
     * Returns the Name as a part of the Message Processing Chain
     *
     * @return String The name of this processor.
     *
     */
    public String getName() {

        return processorName;
    }





    //for testing: read properties from a file
    public void readConfigFile() {
      try {
            String configString = FileUtils.readFile(EAP_CONFIG_FILE);

            BufferedReader br = new BufferedReader( new StringReader( configString ) );

            do {
                String line = br.readLine( );

                if ( line == null )
                    break;

                // Don't process any lines containing only whitespace.
                line = line.trim( );

                if ( line.length() == 0 )
                    continue;

                //System.out.println(line);

                StringTokenizer st = new StringTokenizer( line, "=" );
                String[] tx = new String[2];
                int i = 0;
                while( st.hasMoreTokens() ) {
                    tx[i] = st.nextToken();
                    i++;
                }
                //System.out.println("Before " + tx[0] + " = " + tx[1]);
                if( tx[0].equals(EAP_IP_ADDRESS) )
                    ipAddress = tx[1];
                else if( tx[0].equals(EAP_PORT_NUMBER) )
                    portNum = tx[1];
                else if( tx[0].equals(EAP_USER) )
                    eapUser = tx[1];
                else if( tx[0].equals(EAP_PASSWORD) )
                    eapPassword = tx[1];
                else if( tx[0].equals(EAP_RECV_CODE) )
                    recvCode = tx[1];
                else if( tx[0].equals(EAP_SNRF) )
                    snrf = tx[1];
                else if( tx[0].equals(EAP_TRANS_TYPE) )
                    transType = tx[1];
                else if( tx[0].equals(EAP_SERVER_TIMEOUT) )
                    serverTimeout = tx[1];
                else if( tx[0].equals(EAP_SOCKET_CONN_TIMEOUT) )
                    socketConnTimeout = tx[1];
                else if( tx[0].equals(EAP_SOCKET_IO_TIMEOUT) )
                    socketIOTimeout = tx[1];

            } while( true );

        } catch (FrameworkException e) {
            Debug.log(Debug.ALL_ERRORS, "Cannot read from EAPconfig.txt.");
        } catch (IOException e) {
            Debug.log(Debug.ALL_ERRORS, "Cannot get properties from EAPconfig.txt.");
        } catch (NoSuchElementException e) {
            Debug.log(Debug.ALL_ERRORS, "Cannot get element.");
        }

        System.out.println( "ipAddress = " + ipAddress );
        System.out.println( "portNum = " + portNum );
        System.out.println( "eapUser = " + eapUser );
        System.out.println( "recvCode = " + recvCode );
        System.out.println( "snrf = " + snrf );
        System.out.println( "transType = " + transType );
        System.out.println( "serverTimeout = " + serverTimeout );
        System.out.println( "socketConnTimeout = " + socketConnTimeout );
        System.out.println( "socketIOTimeout = " + socketIOTimeout );
    }

    //For testing.
    //Gets a snrf number from a file, adds 1 and write it back to the file
    //to make it unique
    public String getSNRFFromFile() {
      //Read from file
        String base = null;
        try {
            base = FileUtils.readFile(SNRF_FILE);
        } catch (FrameworkException e) {
            Debug.log(Debug.ALL_ERRORS, "Cannot read from number.txt.");
        }

        //Convert from int to string
        String baseDigit = StringUtils.getDigits(base);
        int baseINT = Integer.parseInt(baseDigit);

        baseINT++;

        base = String.valueOf(baseINT);

        //Write back to file
        try {
            FileUtils.writeFile("number.txt", base);
        } catch (FrameworkException e) {
            System.out.println( "Cannot write to number.txt.");
            Debug.log(Debug.ALL_ERRORS, "Cannot write to number.txt.");
        }

        return base;
    }

    //for testing
    public static final String EAP_CONFIG_FILE = "EAPconfig.txt";
    public static final String SNRF_FILE = "number.txt";
    
}

/**
 * Copyright(c) 2000-2004 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.comms.ia.asn;

import java.io.*;
import java.util.*;

// jdk imports
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.HashMap;

// third party imports
import cryptix.asn1.lang.*;

// NightFire imports
import com.nightfire.common.ProcessingException;
import com.nightfire.comms.ia.DataExceptionHandler;
import com.nightfire.comms.ia.asn.msg.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;

import com.nightfire.framework.util.FileUtils;

import com.nightfire.comms.ia.*;

// third party imports
//import cryptix.asn1.encoding.Factory;
//import cryptix.asn1.io.ASNReader;
//import cryptix.asn1.io.ASNWriter;

/**
 * ASNFactory is responsible for reading data from a stream and determining
 * which type of message it is.  Given an input stream it must be able to
 * return a corresponding ASNData object.
 */
public class ASNFactory
{
    /**
     * The message definitions associated with this class.
     */
    private HashMap definitions = new HashMap();
    
    /**
     * A handler to use for exceptions thrown between receiving and
     * processing a message.
     */
    private DataExceptionHandler exHandler = null;

    /**
     * This is the class constructor.
     *
     * @param baseName    The base file name for persisted error messages
     * @param extension   The extension for persisted error messages
     * @param exDir       The exception directory
     * @param errDir      The error directory
     */
    public ASNFactory(String baseName, String extension, String exDir, String errDir)
    {
        exHandler = new DataExceptionHandler(baseName, extension, exDir,
                                             errDir);
    }

    /**
     * Adds a supported definition to this class's list.
     *
     * @param def The definition to add
     */
    public void addDefinition(ASNDefinition def)
    {
        // add the definition
        definitions.put(def.getOID(), def);
    }

    /**
     * This is method is where the bulk of processing for an ASNFactory
     * occurs.  Given InputStream is, it attempts to read from the stream
     * until it can find a matching ASNData in its definitions.  An instance
     * of that ASNData is then returned containing the information read from
     * the stream.  If a match cannot be found an exception is thrown, but
     * part of the contract of ASNFactory is that it will have read all of
     * the message from the stream according to DER, even if its contents
     * cannot be interpreted.
     *
     * @param is  The InputStream to read the message from
     *
     * @return    A newly constructed message
     *
     * @exception ProcessingException   Thrown if an error occurs reading
     *                                  or constructing the message
     * @exception MessageException      Thrown if there is an error in the
     *                                  message data or encoding itself.
     */
    public ASNData instanceFor(InputStream is)
        throws ProcessingException, MessageException
    {
        byte[] derMsg = null;
        ASNData result = null;
        //ASNReader coder = null;
        
        try
        {
            // decode a generic message
            Debug.log(this, Debug.MSG_DATA,
                      "Attempting to read an IA message.");
                        
            BufferedInputStream bis = new BufferedInputStream( is );
                 
            //mark the buffer so we can reset it.
            bis.mark(64);      
            
            //read the first byte to determine the message type.
            int tag = bis.read();
                                              
            //reset the input stream
            bis.reset();
             
            switch (tag )
            {
                
                //Issue3: BasicMessage IA5String Tag value 0x16 = 22
                case 22:
                {                                       
                     Debug.log(this, Debug.MSG_DATA,
                        "Trying to decode a Issue3 BasicMessage ...");
                    BasicMessage ba = new BasicMessage();           
                    ba.decode(ASNData.DER_CODING, bis);
                    result = (ASNData) ba;
                    break;
                }
                
                //Issue2: Indicated by Sequence Tag value 0x30 = 48
                case 48:
                {
                                        // decode a generic message
                    Debug.log(this, Debug.MSG_STATUS,
                              "Attempting to read an Issue2 message ...");
                    IAGeneric msg = new IAGeneric();
                    msg.decode( ASNData.DER_CODING, bis );

                    if( Debug.isLevelEnabled(Debug.MSG_DATA) )
                        Debug.log(this, Debug.MSG_DATA, "OID: [" + msg.getContentType() + "]");

                    // recode the message to a buffer
                    ByteArrayOutputStream tmpOs = new ByteArrayOutputStream();
                    msg.encode(ASNData.DER_CODING, tmpOs);
                    derMsg = tmpOs.toByteArray();

                    if( Debug.isLevelEnabled(Debug.MSG_DATA) )
                        Debug.log( Debug.MSG_DATA, "derMsg after intermediate decoding [" +
                               new HexFormatter(derMsg) + "]");

                    // get the OID
                    String oid = msg.getContentType();
                    Debug.log(this, Debug.MSG_DATA, "Received a message with OID: [" +
                              oid + "]");

                    // get the corresponding definition
                    ASNDefinition def = getDefinition(oid);

                    // get a new instance of the message
                    result = def.newInstance();

                    // decode the message in its final format
                    ByteArrayInputStream tmpIs =
                        new ByteArrayInputStream(derMsg);
                    result.decode(ASNData.DER_CODING, tmpIs);
                    break;
                }
                
                //Issue3:  BitString Tag value 0x3 = 3
                case 3:
                {
                    Debug.log(this, Debug.MSG_DATA,
                          "Trying to decode Issue3 IaStatusMessage ....");
                    
                    
                    IaStatusMessage ias = new IaStatusMessage();
                    ias.decode(ASNData.DER_CODING, bis);
                    result = (ASNData) ias; 
                    break;
                }
                
                //Issue3: Octect String value 0x4 = 4
                //This should be a IaReceiptMessage
                case 4:
                {
                    
                    Debug.log(this, Debug.MSG_DATA,
                              "Trying to decode an Issue3 EnhancedMessage ...");

                    EnhancedMessage eMsg = new EnhancedMessage();
                    eMsg.decode(ASNData.DER_CODING, bis);
                    
                    //Due to problems in cryptix libraries, we
                    //need to remove the first (2) bytes from the 
                    //decoded EnhancedMessage ( a byte[] ) so we can properly build
                    //the IaReceiptMessage object. 
                    ByteArrayInputStream bai 
                        = new ByteArrayInputStream( ASNFactory.getTruncateBytes( eMsg.getValue(), 2 ));
                    
                    IaReceiptMessage receipt = new IaReceiptMessage();
                    
                    receipt.decode( ASNData.DER_CODING, bai );                 
                                                                   
                    result = (ASNData) receipt;
                    break;
                }
                
		//-1 indicates we are at the end of the InputStream
		case -1:
			return null;

                default:
                {
                    throw new ProcessingException("Unknown message type.");
                }
            }//end switch
                                          
            return result;
            
        }
               
        // ProcessingExceptions can occur during message coding       
        catch (ProcessingException e)
        {
            Debug.logStackTrace(e);
            
            // use DataExceptionHandler to preserve any received data
            saveData(e, derMsg);

            throw e;
        }
        // MessageExceptions can occur if we don't have a matching OID
        catch (MessageException e)
        {
            Debug.logStackTrace(e);
            
            // use DataExceptionHandler to preserved any received data
            saveData(e, derMsg);

            throw e;
        }
        catch (Exception e)
        {
            Debug.logStackTrace(e);
            
            throw new ProcessingException(e);
        }
           
    }

    /**
     * Looks up the message with a specific OID.
     *
     * @param oid   The OID to look up.
     *
     * @return      The matching mesage definition.
     *
     * @exception ProcessingException  Thrown if a matching definition cannot
     *                                 be found.
     */
    private ASNDefinition getDefinition(String oid) throws MessageException
    {
        // test for the OID
        if (definitions.containsKey(oid))
            // return the definition
            return (ASNDefinition)definitions.get(oid);
        else
            // throw an exception
            throw new MessageException("The received OID [" + oid +
                                 "] does not match any known message types.");
    }

    /**
     * Uses DataExceptionHandler to preserve any data which was successfully
     * received caused an exception to be generated before it could be
     * returned as a constructed message.
     *
     * @param e   The exception that was generated
     * @param msg The received message
     */
    private void saveData(Exception e, byte[] msg)
    {
        // if the data is null, just return
        if (msg == null)
            return;

        //  have the handler persist the data
        exHandler.handleException(e, msg);
    }
    
    /*
     * Utility method for truncated the specified number of bytes to remove from 
     * <b>front</b> of the given byte array.
     *
     * @param bytes - the byte array to be truncated
     * @param removeBytes - the number of bytes to skip in creating the new byte array
     *
     * @return byte[] - new byte array with the specified number of bytes removed 
     */
    private static byte[] getTruncateBytes( byte[] bytes, int removeBytes )
    {
        //create a truncated byte array so we can decode.
        byte[] truncBytes = new byte[bytes.length - removeBytes];
                    
        int index = 0;                  
        for ( int i =removeBytes; i < bytes.length; i++ )
        {
            truncBytes[index++] = bytes[i];
        }
        
        return truncBytes;
    }
    
    
    /**
     *   This is a test driver to ensure that
     *   we can decode/encode an Issue3 IaReceiptMessage 
     */
    public static void main(String[] args) 
    {       
        
        try
        {
           
          Debug.enableAll();
           
          String s = "a27e307c04694953417e30307e202020202020202020207e30307e20"
                     +"2020202020202020207e5a5a7e4e455854584f5357324f524454202"
                     +"07e30317e3030363936383532335342432020207e3034303932317e"
                     +"303132397e3c7e30303430307e3030303030303038357e307e547e5"
                     +"e130f32303034303932313230323934365a";
                      
          //convert String to a Hex byte[]
          int j = 0;     
          char[] chars = s.toCharArray();
          
          byte[] b = new byte[(chars.length)/2];
          
          for ( int i = 0; i < chars.length; )
          {
              int hex = ( ( charToHex( chars[i++]) * 16 ) + charToHex( chars[i++] ) );
              System.out.print( hex + " " );
              
              b[j++] = (byte) hex;

          }  
          
          System.out.println ("---------------------------\n");
          
          System.out.println(new HexFormatter( b ) );
         
          byte[] truncBytes = ASNFactory.getTruncateBytes( b , 2 );
                            
          
          System.out.println ("---------------------------\n");
          
          System.out.println(new HexFormatter( truncBytes) );
          ByteArrayInputStream bis = new ByteArrayInputStream( truncBytes );
          

          IaReceiptMessage receipt = new IaReceiptMessage();
          receipt.decode( ASNData.DER_CODING, bis );          
          
          if (Debug.isLevelEnabled(Debug.MSG_DATA))
          {
                
                Debug.log(Debug.MSG_DATA, "ISA Segment: [" + receipt.getISASegment() + "]\n");
                Debug.log(Debug.MSG_DATA, "Date Time Stamp: ["
                          + receipt.getDateTimeStampString() + "]");
           }
          
           IaReceiptMessage r2 = new IaReceiptMessage();
           
           r2.setISASegment(receipt.getISASegment() );
           r2.setDateTimeStamp(receipt.getDateTimeStampString() );

           ByteArrayOutputStream tmpOs = new ByteArrayOutputStream();
           
           r2.encode(ASNData.DER_CODING, tmpOs);
           byte[] derMsg = tmpOs.toByteArray();
 
           EnhancedMessage e = new EnhancedMessage( derMsg );  
           
           ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    
           OutputStream os = new SnoopOutputStream (new BufferedOutputStream( bos ) );
                      
           e.encode( ASNData.DER_CODING, os );
           
           byte[] eDerMsg = bos.toByteArray();
           

           // decode the message in its final format
           ByteArrayInputStream tmpIs = new ByteArrayInputStream(eDerMsg );
           EnhancedMessage e2 = new EnhancedMessage();
          
           e2.decode(ASNData.DER_CODING, tmpIs);
          
           //now compare the input array from the one we just generated.
           String result = new String( e2.getValue() );
           String input  = new String ( b );
           
           if ( result.equals(input) )
               System.out.println("success");
           else
               System.out.println("failure");
           
           
 
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
     
    
    private static byte charToHex(char c)
    {
        if ( c == '0' ) return 0;
        if ( c == '1' ) return 1;
        if ( c == '2' ) return 2;
        if ( c == '3' ) return 3;
        if ( c == '4' ) return 4;
        if ( c == '5' ) return 5;
        if ( c == '6' ) return 6;
        if ( c == '7' ) return 7;
        if ( c == '8' ) return 8;
        if ( c == '9' ) return 9;
        if ( c == 'a' ) return 10;
        if ( c == 'b' ) return 11;
        if ( c == 'c' ) return 12;
        if ( c == 'd' ) return 13;
        if ( c == 'e' ) return 14;
        if ( c == 'f' ) return 15; 
        return -1;
    }


}

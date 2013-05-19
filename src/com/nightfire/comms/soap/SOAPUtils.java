/*
 * Copyright (c) 2000-2002 Nightfire Software, Inc. All rights reserved.
 * $Header: $
 */

package com.nightfire.comms.soap;

import com.nightfire.framework.util.*;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * This class contains some helper methods used by all SOAP classes.
 */
public class SOAPUtils
{

    /**
     * Name of file containing fault Strings to be treated as Exceptions rather than Fault messages
     * that should be ignored as safe.
     */
    public static final String SOAP_FAULTS_TO_CONVERT_FILE_NAME = "./config/soap/soap_faults_to_convert.txt";

    // SOAP Socket Exception String
    public static final String SOAP_FAULT_STR_SOCKET_EXCEPTION = "java.net.SocketException";

    // SOAP HandShake Exception String
    public static final String SOAP_FAULT_STR_HANDSHAKE_EXCEPTION = "javax.net.ssl.SSLHandshakeException";

    // SOAP HandShake Connnection String
    public static final String SOAP_FAULT_STR_CONNECTION_EXCEPTION = "java.net.ConnectException";

    private static List faultsToConvertList;

    /**
     * Throw the given exception as a SOAP fault exception
     *
     * @param  faultType  The type of exception that occurred, as defined in the SOAPConstants.
     * @param errorMessage The error message
     *
     * @exception  org.apache.axis.AxisFault with the faultCode set to faultType and the faultString
     * containing the faultCode too.
     */
    public static void throwSOAPFaultException ( String faultType, String errorMessage )
        throws org.apache.axis.AxisFault
    {
        throw new org.apache.axis.AxisFault ( faultType,
        faultType + ":" + errorMessage, null, null );
    }

    /**
     * Map the errorMessage into one of the constants defined in SOAPConstants.
     * If the errorMessage matches up with one of
     * SOAPConstants.MESSAGE_FAULT, SOAPConstants.PROCESSING_FAULT or SOAPConstants.SECURITY_FAULT,
     * then the matched constant String is returned as the error type. If there is no match,
     * then SOAPConstants.PROCESSING_FAULT is returned.
     * @param errorMessage The error message whose type is to be deciphered.
     *
     * @return String One of SOAPConstants.MESSAGE_FAULT, SOAPConstants.PROCESSING_FAULT or SOAPConstants.SECURITY_FAULT
     */
    public static String mapErrorType ( String errorMessage )
    {
        String errorType = SOAPConstants.PROCESSING_FAULT;
        if ( errorMessage != null )
        {
            if ( errorMessage.indexOf( SOAPConstants.MESSAGE_FAULT ) != -1 )
                errorType = SOAPConstants.MESSAGE_FAULT;
            else
            if ( errorMessage.indexOf( SOAPConstants.SECURITY_FAULT ) != -1 )
                errorType = SOAPConstants.SECURITY_FAULT;
            //else errorType = SOAPConstants.PROCESSING_FAULT by default.
        }

        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "SOAPUtils: Returning error type [" +
            errorType + "]." );
            
        return errorType;
    }

    /**
     * Reads all the SOAP faults listed in the default file
     * and returns a List containing the Fault Strings
     *
     * @return List List containing the SOAP Fault Strings
     */

    public static List loadSOAPFaultsToConvert ( )
    {
        if ( faultsToConvertList == null )
        {
            synchronized ( SOAPUtils.class )
            {
                if ( faultsToConvertList == null )
                    faultsToConvertList = loadSOAPFaultsToConvert(SOAP_FAULTS_TO_CONVERT_FILE_NAME);
            }
        }

        return faultsToConvertList;
    }

    /**
     * Reads all the SOAP faults listed in a file
     * and returns a List containing the Fault Strings
     * @param faultFileName Name of the file containing the SOAP Fault Strings.
     *
     * @return List List containing the SOAP Fault Strings
     */
    public static List loadSOAPFaultsToConvert (String faultFileName )
    {
        if ( faultFileName != null )
        {
                List<String>  faultsToConvert = new ArrayList<String>();

                try
                {
                    // Ensure that file is available for read
                    FileUtils.FileStatistics fs = FileUtils.getFileStatistics( faultFileName );
                    if (Debug.isLevelEnabled(Debug.IO_STATUS))
                        Debug.log( Debug.IO_STATUS, "File [" + faultFileName + "] properties: exists? [" + fs.exists + "], is-file? ["
                               + fs.isFile + "], readable? [" + fs.readable + "]." );

                    if ( fs.exists && fs.isFile && fs.readable )
                    {
                        // read the file into the List
                        File soapFaultFile = new File(faultFileName);
                        BufferedReader br = new BufferedReader(new FileReader(soapFaultFile));
                        String line = br.readLine();

                        while (StringUtils.hasValue(line))
                        {
                            faultsToConvert.add(line);
                            line = br.readLine();
                        }
                        br.close();

                        if (Debug.isLevelEnabled(Debug.NORMAL_STATUS))
                        {
                            Debug.log( Debug.NORMAL_STATUS, "Faults to convert:" );

                            if (faultsToConvert.size() != 0)
                            {
                                for (String faultStr: faultsToConvert)
                                {
                                    Debug.log(Debug.NORMAL_STATUS,"[" + faultStr + "]");
                                }
                            }
                            else
                            {
                                if (Debug.isLevelEnabled(Debug.IO_STATUS))
                                    Debug.log( Debug.IO_STATUS, "File [" + faultFileName + "] is empty." );
                            }
                        }

                        return faultsToConvert;

                    }
                    else
                    {
                      if (Debug.isLevelEnabled(Debug.IO_STATUS))
                          Debug.log( Debug.IO_STATUS, "Cannot access file [" + faultFileName + "]." );
                    }
                }
                catch ( Exception e )
                {
                    Debug.error( e.toString() );
                }
            }

        return null;
    }

    // For Unit Testing
    public static void main(String args[])
    {
        Debug.enableAll();
        loadSOAPFaultsToConvert("E:\\b39\\config\\soap\\soapfaults.txt");
    }

}

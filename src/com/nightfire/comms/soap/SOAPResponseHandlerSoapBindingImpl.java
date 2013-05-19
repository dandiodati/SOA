/**
 * SOAPResponseHandlerSoapBindingImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 * Modified later on by user.
 */

package com.nightfire.comms.soap;

import com.nightfire.framework.webapp.*;
import com.nightfire.framework.debug.*;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.StringUtils;
		

/**
 * Test implementation of SOAPResponseHandler service.
 * Depending on the incoming data, either an exception is thrown or the message is accepted.
 * The message is assumed to have a node ERROR under the root.
 * If the value of the node is
 * "DataError", then a SOAP message exception is thrown,
 * "SystemError", then a SOAP processing exception is thrown,
 * "NONE" or anything other than "DataError" or "SystemError", then the message is successfully processed.
 */
public class SOAPResponseHandlerSoapBindingImpl implements com.nightfire.comms.soap.SOAPResponseHandler
{
    public void processEvent(java.lang.String in0, java.lang.String in1) throws java.rmi.RemoteException
    {
        //Adding logging for testing purposes
        if (log.isDebugEnabled())
            log.debug( "SOAPResponseHandlerSoapBindingImpl: Channel [" + in0 +
        "] received event \n[" + in1 + "].\n" );
        
		    //This method throws exception if ERROR node has value DataError or SystemError
        try
        {
            if (log.isDebugEnabled())
                log.debug("Processing started");

            XMLMessageParser parser = new XMLMessageParser( in1 );

            String nodeValue = getRequiredValue( parser, "ERROR" );
			
            if (log.isDebugDataEnabled())
                log.debug("Returned Value for node ERROR = " + nodeValue);

			      if ( nodeValue.equals("DataError") )
            {
            	log.warn( "Exception to be thrown with fault code[" + SOAPConstants.MESSAGE_FAULT + "]." );
                SOAPUtils.throwSOAPFaultException ( SOAPConstants.MESSAGE_FAULT, "DataError" );
            }
            else if ( nodeValue.equals("SystemError") )
            {
            	log.warn( "Exception to be thrown with fault code[" + SOAPConstants.PROCESSING_FAULT + "]." );
                SOAPUtils.throwSOAPFaultException ( SOAPConstants.PROCESSING_FAULT, "SystemError" );
            }
			      else
			      {
				        // Processed
			      }
            if (log.isDebugEnabled())
                log.debug("Processing done.");
		    }
		    catch(MessageException me)
		    {
            log.warn("Will throw AxisFault " + SOAPConstants.MESSAGE_FAULT);
            SOAPUtils.throwSOAPFaultException ( SOAPConstants.MESSAGE_FAULT, me.toString() );
		    }
    }

    private String getRequiredValue ( XMLMessageParser parser, String nodeName ) throws MessageException
    {
        String value = null;

        if ( parser.nodeExists( nodeName ) )
            value = parser.getValue( nodeName );
        else
            return "";

        if ( !StringUtils.hasValue( value ) )
            throw new MessageException( "Invalid value [" + value + "] retrieved for node [" +
            nodeName + "]." );

        return value;
    }

	protected static DebugLogger log =  DebugLogger.getLogger("/axis", SOAPResponseHandlerSoapBindingImpl.class);
}

/**
 * The purpose of this processor is to get the input message and set into the
 * context.
 * 
 * @author Jigar Talati
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.common.ProcessingException;
 * @see com.nightfire.framework.db.DBInterface;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorBase;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants;
 *  
 */
 
/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Jigar			06/28/2004			Created
	2			Jigar			07/07/2004			Modified process method, 
													instead of getDom, getString
													has been used.
	3			jigar			07/29/2004			Formal review comments incorporated.


 */
package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;


import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class GetAndSetMessage extends MessageProcessorBase {
	
	/**
	 * The value of inputMessage.
	 */
	private String inputMessage = null;

	/**
	 * The value of outputMessage.
	 */
	private String outputMessage = null;
	
	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key   Property-key to use for locating initialization properties.
	 * @param  type  Property-type to use for locating initialization properties.
	 *
	 * @exception ProcessingException when initialization fails.
	 */
	public void initialize ( String key, String type )throws ProcessingException
	{
	
		// Call base class method to load the properties.
		super.initialize( key, type );

		// Get configuration properties specific to this processor.
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		  Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMessage : Initializing..." );
		}
	
		StringBuffer errorBuffer = new StringBuffer( );
	
		inputMessage = getRequiredPropertyValue(
				SOAConstants.INPUT_MSG_LOC, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG, "Input message is [" 
														+ inputMessage + "]." );
		}
		
		outputMessage = getRequiredPropertyValue( 
							SOAConstants.OUTPUT_MSG_LOC,  errorBuffer );
                if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
					Debug.log( Debug.SYSTEM_CONFIG, "Output message is [" 
														+ outputMessage + "]." );
				}
	
		// If any of the required properties are absent,indicate error to caller.
		if ( errorBuffer.length() > 0 )
		{

			String errMsg = errorBuffer.toString( );

			Debug.log( Debug.ALL_ERRORS, errMsg );

			throw new ProcessingException( errMsg );

		 }
         if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMessage : " +
		 											"Initialization done." );
		 }
	} // initialize end.
	
	/**
	 * This method will extract the data values from the input and set into the
	 * context.
	 *
	 * @param  context The context.
	 * @param  object  Input message to process.
	 *
	 * @return  The given input, or null.
	 *
	 * @exception ProcessingException thrown if processing fails.
	 * @exception MessageException thrown if message is bad.
	 */
    public NVPair[] process ( MessageProcessorContext context, 
							 MessageObject object )
										   throws MessageException,
												  ProcessingException 
    {
    	ThreadMonitor.ThreadInfo tmti = null;
    	
		if ( object == null )
		{
	
			return null;
		
		}
		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] Setting Input Message into Output Message Location" );
		
			String inputMsg = getString( inputMessage, context, object );
		
			XMLMessageParser domMsg = new XMLMessageParser(inputMsg);
		
			Document outMsg = domMsg.getDocument();
		
			// set the inputMsg into the output messgae location.
			super.set( outputMessage, context, object, outMsg );
		
			String outMessage = getString(outputMessage,context,object);
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG, "GetAndSetMessage outMessage : " +
																	outMessage );
			}
		}finally{
		
			ThreadMonitor.stop( tmti );
		}
		
		return( formatNVPair( object ) );
		 											
   } // NVPair[] end.
   
// --------------------------For Testing---------------------------------//

	 public static void main(String[] args) {

		 Properties props = new Properties();

		 props.put( "DEBUG_LOG_LEVELS", "ALL" );

		 props.put( "LOG_FILE", "E:\\logmap.txt" );

		 Debug.showLevels( );

		 Debug.configureFromProperties( props );

		 if (args.length != 3)
		 {

			  Debug.log (Debug.ALL_ERRORS, "GetAndSetMessage: USAGE:  "+
			  " jdbc:oracle:thin:@192.168.1.246:1521:soa jigar jigar");

			 return;

		 }
		 try
		 {

			 DBInterface.initialize( args[0], args[1], args[2] );

		 }
		 catch (DatabaseException e)
		 {

			  Debug.log( null, Debug.MAPPING_ERROR, ": " +
					   "Database initialization failure: " + e.getMessage() );

		 }

		 GetAndSetMessage getAndsetMsg = new GetAndSetMessage();

		 try
		 {
			 getAndsetMsg.initialize("FULL_NEUSTAR_SOA","GetAndSetMessage");

			 MessageProcessorContext mpx = new MessageProcessorContext();

			 MessageObject mob = new MessageObject();
			
			 mob.set("inputMessages",
							"<Error>"+
								"<ruleerrorcontainer>"+
									"<ruleerror>"+
										"<RULE_ID value=\"01\" />"+
										"<MESSAGE value=\"Rejected\" />"+
							"<CONTEXT value=\"SynchronusResponseContext\" />"+
										"<CONTEXT_VALUE value=\"12345\" />"+
									"</ruleerror>"+
								"</ruleerrorcontainer>"+
							"</Error>");
							
		     getAndsetMsg.process(mpx,mob);

			 Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		 }
		 catch(ProcessingException pex)
		 {

			 System.out.println(pex.getMessage());

		 }
		 catch(MessageException mex)
		 {

			 System.out.println(mex.getMessage());

		 }
	 } //end of main method   
} 

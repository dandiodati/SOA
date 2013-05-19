/**
 * The purpose of this processor is to get the input message as xml and replace
 * the root node by output root node. The processor has three input parameters.
 * The input xml message as INPUT_MESSAGE, The root node of the input message as 
 * INPUT_ROOT_NODE and the replacing node as OUTPUT_ROOT_NODE.
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
 *  
 */
 
/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Jigar Talati	07/10/2004			Created
	2			Jigar Talati	07/12/2004			Incorporated review 
													comments
 */

package com.nightfire.spi.neustar_soa.adapter.messageprocessor;

import java.util.Properties;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class XMLRootChanger extends MessageProcessorBase {
	
	/**
	 * The location of inputMessage.
	 */
	private String inputMessage = null;
	
	/**
	 * The value of inputRoot.
	 */
	private String inputRoot = null;
	
	/**
	 * The value of outputRoot.
	 */
	private String outputRoot = null;
	
	/**
	 * The location of outputMessage.
	 */
	private String outputMessage = null;
	
	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key   String Property-key to use for locating initialization
	 * 				 properties.
	 * @param  type  String Property-type to use for locating initialization
	 * 				 properties.
	 *
	 * @exception ProcessingException when initialization fails.
	 */
	public void initialize ( String key, String type )throws ProcessingException
	{
	
		// Call base class method to load the properties.
		super.initialize( key, type );

		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "XMLRootChanger : Initializing..." );
	
		StringBuffer errorBuffer = new StringBuffer( );
		
		// Get configuration properties specific to this processor.
		inputMessage = getRequiredPropertyValue( 
							SOAConstants.INPUT_MSG_LOC, errorBuffer );
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "Input message location is [" 
													+ inputMessage + "]." );
		
		inputRoot = getRequiredPropertyValue( 
							SOAConstants.INPUT_ROOT_NODE_LOC, errorBuffer );

		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "Input root node is [" 
													+ inputRoot + "]." );													
		
		outputRoot = getRequiredPropertyValue( 
						SOAConstants.OUTPUT_ROOT_NODE_LOC, errorBuffer );

		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "Output root node is [" 
													+ outputRoot + "]." );
													
		outputMessage = getRequiredPropertyValue( 
							SOAConstants.OUTPUT_MSG_LOC, errorBuffer );

		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "Output message location is [" 
													+ outputMessage + "]." );														

		// If any of the required properties are absent,indicate error to caller
		if ( errorBuffer.length() > 0 )
		{

			String errMsg = errorBuffer.toString( );

			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log( Debug.ALL_ERRORS, errMsg );

			throw new ProcessingException( errMsg );

		 }
    
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "XMLRootChanger : " +
		 											"Initialization done." );														
	} // initialize end.
	
	/**
	 * This method will extract the data values from the input and set into the
	 * context.
	 *
	 * @param  MessageProcessorContext context The context.
	 * @param  MessageObject object  Input message to process.
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
		try
		{
		tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
		
		String inputMsg = null;
		
		inputMsg = getString(inputMessage, context, object);
		
		inputMsg = inputMsg.replaceAll(inputRoot, outputRoot);
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
			Debug.log( Debug.SYSTEM_CONFIG, "After the replacement, " + "Output message is [" + inputMsg + "]." );
				
		super.set( outputMessage, context, object, inputMsg );
		}
		finally
		{
			ThreadMonitor.stop(tmti);
		}
		return( formatNVPair( object ) );
		 											
   } // process method end.
   
// --------------------------For Testing---------------------------------//

	 public static void main(String[] args) {

		 Properties props = new Properties();

		 props.put( "DEBUG_LOG_LEVELS", "ALL" );

		 props.put( "LOG_FILE", "E:\\logmap.txt" );

		 Debug.showLevels( );

		 Debug.configureFromProperties( props );

		 if (args.length != 3)
		 {

			  Debug.log (Debug.ALL_ERRORS, "XMLRootChanger: USAGE:  "+
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

		 XMLRootChanger xmlRootChanger = new XMLRootChanger();

		 try
		 {
			 xmlRootChanger.initialize( "FULL_NEUSTAR_SOA","XMLRootChanger" );

			 MessageProcessorContext mpx = new MessageProcessorContext();

			 MessageObject mob = new MessageObject();
			
			 mob.set("inputMessage",
							"<SOAMessage xmlns=\"urn:neustar:lnp:soa:1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema\">"+
								"<ruleerrorcontainer>"+
									"<ruleerror>"+
										"<RULE_ID value=\"01\" />"+
										"<MESSAGE value=\"Rejected\" />"+
							"<CONTEXT value=\"SynchronusResponseContext\" />"+
										"<CONTEXT_VALUE value=\"12345\" />"+
									"</ruleerror>"+
								"</ruleerrorcontainer>"+
							"</SOAMessage>");
							
			 xmlRootChanger.process( mpx, mob );

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

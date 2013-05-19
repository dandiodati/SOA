/**
 * The purpose of this processor is to make NpaNxxDashx by  
 * combinig Npa,Nxx and Dashx values and put it into context.
 * This  processor will take Npa, Nxx, Dashx value as input.
 * 
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.common.ProcessingException;
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
	Rev#		Created By  	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok Kumar		07/02/2004 	   		Created
	2			Ashok Kumar		07/08/2004			Review comments incoporated
	

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

public class NPANXXXMaker extends MessageProcessorBase {

	
	/**
	 * The  location of  npaNxxX
	 */
	private String npaNxxXLoc = null;
	
	/**
	 * The variable is used to get location of  npa
	 */
	private String npaLoc = null;

	/**
	 * The variable is used to get location of  nxx
	 */
	private String nxxLoc = null;

	/**
	 * The variable is used to get location of  dashX
	 */
	private String dashXLoc = null;

	/**
	 * Initializes this object via its persistent properties.
	 *
	 * @param  key String  Property-key to use for locating initialization 
	 * properties.
	 * param  type String Property-type to use for locating initialization 
	 * properties.
	 *
	 * @exception ProcessingException when initialization fails.
	 */

	public void initialize( String key, String type )
											throws ProcessingException {
		
		// Call base class method to load properties 		
		super.initialize(key, type);
        
		if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
		 Debug.log( Debug.SYSTEM_CONFIG, "NPANXXXMaker: Initializing..." );
		}
		
		 // Get Configuration Properties  specific to this processor.
		StringBuffer errorBuffer = new StringBuffer();

		npaLoc = getRequiredPropertyValue( 
						SOAConstants.INPUT_LOC_NPA_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
								"Location of NPA value is ["+ npaLoc + "].");
		}
		nxxLoc = getRequiredPropertyValue( 
					SOAConstants.INPUT_LOC_NXX_PROP, errorBuffer );

        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
							"Location of NXX value  is [" + nxxLoc + "].");
		}
		dashXLoc = getRequiredPropertyValue( 
						SOAConstants.INPUT_LOC_DASHX_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
							"Location of DASHX value  is ["+ dashXLoc + "].");
		}
				
		npaNxxXLoc = getRequiredPropertyValue( 
						SOAConstants.OUTPUT_LOC_NPA_NXX_X_PROP, errorBuffer );
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
									"Location of NPANXXX is [" + npaNxxXLoc + "].");
		}
		
		// If any of the required properties are absent,indicate error to caller
		if ( errorBuffer.length() > 0 ) {
			
			String errMsg = errorBuffer.toString();

			Debug.log(Debug.ALL_ERRORS, errMsg);
			
			throw new ProcessingException(errMsg);
			
		}
        if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
			Debug.log( Debug.SYSTEM_CONFIG,
										"NPANXXXMaker: Initialization done.");
		}

	}

	/**
	 * This method will extract the NPA ,NXX , DASHX values from the input, 
	 * and combine them  into NPANXXX and put it into context. 
	 *
	 * @param  context MessageProcessorContext the context.
	 * @param  object MessageObject Input message to process.
	 *
	 * @return   NVPair[] The given input, or null.
	 *
	 * @exception  ProcessingException thrown if processing fails.
	 * @exception  MessageException  thrown if message is bad.
	 */
	public NVPair[] process( MessageProcessorContext context,
							 MessageObject object )
							 throws MessageException, ProcessingException {
		ThreadMonitor.ThreadInfo tmti = null;

		if ( object == null ) {
			
			return null;
			
		}
		
		try{
			tmti = ThreadMonitor.start( "Message-processor [" + getName() + "] started processing the request" );
			
			// Get the value of NPA	
			String npa = getString( npaLoc, context, object );
			
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,
										"Value of NPA  is ["+ npa + "].");
			}
			
			// Get The value of NXX 
			String nxx = getString( nxxLoc, context, object );
			
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,
										"Value of NXX is ["+ nxx + "].");
			}
			
			// Get the value of DASHX 
			String dashX = getString( dashXLoc, context, object );
			
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,
										"Value of DASHX  is ["+ dashX + "].");
			}
			 
			// combine them into NPANXXX		
			StringBuffer npaNxxX = new StringBuffer();
			
			npaNxxX.append( npa ) ;
			
			npaNxxX.append( nxx ) ;
			
			npaNxxX.append( dashX ) ;
			
			if( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ) ){
				Debug.log( Debug.SYSTEM_CONFIG,
							"Value of NPANXXX  is ["+ npaNxxX.toString() + "].");
			}
			
			// set NPANXXX value in to contaxt
			set( npaNxxXLoc , context , object , npaNxxX.toString() );	
		
		}finally{
			ThreadMonitor.stop(tmti);
		}

		return ( formatNVPair(object) );

	}

	/**
	 * For Testing
	 */
	public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "E:\\log.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length != 3) {

			Debug.log( Debug.ALL_ERRORS,
				"NpaNXXXMaker: USAGE:  "
					+ " jdbc:oracle:thin:@192.168.1.246:1521:soa ashok ashok");

			return;

		}
		try{

			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {

			Debug.log(
				null,
				Debug.MAPPING_ERROR,
				": " + "Database initialization failure: " + e.getMessage());

		}

		NPANXXXMaker npaNxxXMaker = new NPANXXXMaker();

		try {
			 

			npaNxxXMaker.initialize("Neustar SOA","NpaNxxXMaker");

			MessageProcessorContext mpx = new MessageProcessorContext();

			MessageObject mob = new MessageObject();

			mob.set("NPA", "123");
			
			mob.set("NXX", "456");
			
			mob.set("DASHX", "7");
			
			npaNxxXMaker.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());
		}
	}
}

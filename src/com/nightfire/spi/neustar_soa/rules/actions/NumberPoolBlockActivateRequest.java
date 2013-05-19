/**
 * This is the class which is used to generate NumberPoolBlockActivateRequest xml.
 * @author P.B.Nunda Raani
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants;
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Devaraj			12/27/2004			Created
 */
package com.nightfire.spi.neustar_soa.rules.actions;


import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.rules.Context;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NumberPoolBlockActivateRequest extends NPBAction{
	
	/**
	 * This variable contains the base path for NumberPoolBlock Activate request.
	 */
   	public static final String BASE_PATH = 	SOAConstants.REQUEST_BODY_PATH+"."+
                                          	SOAConstants.NPB_ACTIVATE_REQUEST;
	/**
     * This variable contains the location of the NpaNxxX .
     */
   	public static final String NPANXXX_LOCATION = 	BASE_PATH+"."+
                                              		SOAConstants.NPANXXX;
      
	/**
	 * This variable contains the location of the BlockHolderSPID .
	 */                                        
	public static final String BLOCK_HOLDER_SPID_LOCATION = BASE_PATH+"."+
															SOAConstants.BLOCK_HOLDER_SPID;
	
	/**
	 * This variable contains the location of the RegionId .
	 */												
	public static final String REGION_LOCATION = BASE_PATH+"."+
												 "."+SOAConstants.REGIONID_NODE;  
	
	/**
	 * Constructor for NumberPoolBlockActivate Request which will 
	 * inturn call the super class constructor.
	 *
	 */
    public NumberPoolBlockActivateRequest(){

        super(  SOAConstants.NPB_ACTIVATE_REQUEST,
                null,
				REGION_LOCATION,
				NPANXXX_LOCATION ,
				BLOCK_HOLDER_SPID_LOCATION );

    }
     
	/**
	 * This sets the NpaNxxX , region and SPID value into XML.
	 * @param blockId String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 * 
	 * @throws MessageException
	 */	
	protected void setBlockId( 	String blockId,
                          		Context context,
                          		XMLMessageGenerator message)
                          		throws MessageException{

		if( context.valueExists( Context.NPA_NODE ) &&
				  context.valueExists(Context.NXX_NODE)){
					
				 String npa = context.getNpa() ;
				 
				 String nxx = context.getNxx() ;
				 
				 String dashx = context.getDashX() ;
			 
				 setNpaNxxX(npa+""+nxx+""+dashx, context, message);
				 
				 String region = context.getRegionId();
		
				 setRegion( region, context, message );

				 String spid = context.getSpid();

				 setSPID( spid, context, message );		 

		}else{

			throw new MessageException(Context.NPA_NODE+ " and " +
										Context.NXX_NODE+" are required.");
		}		  

	}	

}

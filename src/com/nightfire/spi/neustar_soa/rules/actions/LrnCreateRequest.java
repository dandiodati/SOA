/**
 *This class contains actions that can be taken against a
 * (Location Routing Number)LRN CreateRequests.
 * @author P.B.Nunda Raani
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.rules.Context;
 * @see com.nightfire.framework.message.*;
 * @see com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants;
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			PBNundaraani	12/27/2004			Created
 */

package com.nightfire.spi.neustar_soa.rules.actions;

import com.nightfire.spi.neustar_soa.rules.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class LrnCreateRequest extends LRNAction{

	/**
	 * This variable contains the base path for LrnCreate request.
	 */
	public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                        	SOAConstants.LRN_CREATE_REQUEST;
                                          
	/**
	 * This variable contains the location of the RegionId in the 
	 * LrnCreate request.
	 */
	public static final String REGION_LOCATION = BASE_PATH+"."+
											"."+SOAConstants.REGIONID_NODE;

	/**
	 * This variable contains the location of the Lrn value in the 
	 * LrnCreate request.
	 */
	public static final String LRN_LOCATION = BASE_PATH+"."+
                                            SOAConstants.LRN_VALUE_NODE;
                                            
	/**
	 * This variable contains the location of the SPID in the 
	 * LrnCreate request.
	 */
	public static final String SPID_LOCATION = BASE_PATH+"."+
												SOAConstants.SPID_NODE;
	
	/** 
	 * Constructor for LrnCreate Request.
	 */
	public LrnCreateRequest() {

		this(SOAConstants.LRN_CREATE_REQUEST,
						  LRN_LOCATION,
						  REGION_LOCATION,
						  SPID_LOCATION );

	}

	/**
	 * Constructor for LrnCreate Request which will 
	 * inturn call the super class constructor.
	 *
	 */
	protected LrnCreateRequest(String name,
							   String lrnLocation,
							   String regionIdLocation,
							   String spidLocation) {

		super(name,
			  null,
			  regionIdLocation,
			  lrnLocation ,
			  spidLocation );

	}

	/**
	 * This method will set the Lrn Id Value in the xml for the given 
	 * request.
	 * @param Lrn Id String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 * @throws MessageException
	 */
	protected void setLRNID(String lrnid,
						    Context context,
						    XMLMessageGenerator message)
						    throws MessageException{

		// LrnCreate requests cannot be sent based on
		// LRNID and region. Set the LRN instead.
		if( context.valueExists( Context.LRN_NODE ) ){

			String lrn = context.getLrn() ;
			 
			setLrnValue(lrn, context, message);
			 
			String region = context.getRegionId();
			
			setRegion( region, context, message );
				
			String spid = context.getSpid();
				
			setSPID( spid, context, message );

		} else {

			throw new MessageException(Context.LRN_NODE+" is required.");

		}

	}   

}
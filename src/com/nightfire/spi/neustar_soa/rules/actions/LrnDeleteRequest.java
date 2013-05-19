/**
 *This class contains actions that can be taken against a
 * (Location Routing Number)LRN Delete Request.
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
	1			PBNundaraani	12/27/2004			Created
 */
package com.nightfire.spi.neustar_soa.rules.actions;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class LrnDeleteRequest extends LRNAction{
	
	/**
	 * This variable contains the base path for LrnDelete request.
	 */
	public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
										  SOAConstants.LRN_DELETE_REQUEST;
	
	/**
	 * This variable contains the location of the Lrn Id in the 
	 * LrnDelete request.
	 */
	public static final String LRNID_LOCATION = BASE_PATH+"."+
											  SOAConstants.LRN_ID_NODE;
	/**
	 * This variable contains the location of the RegionId in the 
	 * LrnDelete request.
	 */
	public static final String REGION_LOCATION = BASE_PATH+"."+
												"."+SOAConstants.REGIONID_NODE;
												
	/**
	 * This variable contains the location of the Lrn value in the 
	 * LrnDelete request.
	 */
	public static final String LRN_LOCATION = BASE_PATH+"."+
												SOAConstants.LRN_VALUE_NODE;
												
	/**
	 * This variable contains the location of the SPID in the 
	 * LrnDelete request.
	 */
	public static final String SPID_LOCATION = BASE_PATH+"."+
													SOAConstants.SPID_NODE;

	/**
	 * Constructor for LrnDelete Request which will 
	 * inturn call the super class constructor.
	 *
	 */
	public LrnDeleteRequest(){

		super( SOAConstants.LRN_DELETE_REQUEST ,
							LRNID_LOCATION,
							REGION_LOCATION ,
							LRN_LOCATION ,
							SPID_LOCATION );

	}

}
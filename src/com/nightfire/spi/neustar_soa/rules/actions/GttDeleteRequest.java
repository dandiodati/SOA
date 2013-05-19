/**
 * This class contains actions that can be taken against
 * GttDelete requests.
 *
 * @author Jaganmohan Reddy
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see		com.nightfire.spi.neustar_soa.utils.SOAConstants;
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 		Date			Reason
	-----       -----------     	----------		--------------------------
	1			Jaganmohan Reddy	11/23/2004		Created.
												
*/

package com.nightfire.spi.neustar_soa.rules.actions;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class GttDeleteRequest extends GTTAction{

	/**
	 * This variable contains the base path for GttDelete request.
	 */
	public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                          SOAConstants.GTT_DELETE_REQUEST;

	/**
	 * This variable contains the location of the Lrn location in the 
	 * GttDelete Request.
	 */
	public static final String LRN_LOCATION = BASE_PATH+"."+
                                              SOAConstants.LRN_NODE;

	/**
	 * This variable contains the location of the GttId location in the 
	 * GttDelete Request.
	 */
	public static final String GTTID_LOCATION = BASE_PATH+"."+
                                            SOAConstants.GTTID_NODE;
	
	/**
	 * Constructor for GttDelete Request which will 
	 * inturn call the super class constructor.
	 *
	 */
	public GttDeleteRequest(){

		super( SOAConstants.GTT_DELETE_REQUEST ,
			   LRN_LOCATION,
			   GTTID_LOCATION );

	}

}

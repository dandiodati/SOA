/**
 * This is the class which is used to generate NpaNxxDeleteRequest xml.
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

public class NpaNxxDeleteRequest extends NpaNxxAction{
	
	/**
 	 * XML request message path to NPANXX_DELETE_REQUEST. 
 	 */
   	public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
										   SOAConstants.NPANXX_DELETE_REQUEST;
	/**
 	 * XML request message path to NPANXX_ID.
 	 */
   	public static final String NPANXXID_LOCATION = 	BASE_PATH+"."+
   													SOAConstants.NPA_NXX_ID_NODE;
  	/**
     * XML request message path to REGIONID.
     */
   	public static final String REGION_LOCATION = 	BASE_PATH+"."+
													"."+SOAConstants.REGIONID_NODE;
	/**
	 * XML request message path to Npa Value field. 
	 */												
	public static final String NPA_LOCATION = 	BASE_PATH+
												SOAConstants.NPANXXVALUE_NODE+
												"."+SOAConstants.NPA_NODE;
	/**
	 * XML request message path to Nxx Value field. 
	 */											
	public static final String NXX_LOCATION = 	BASE_PATH+
												SOAConstants.NPANXXVALUE_NODE+
												"."+SOAConstants.NXX_NODE;  

	/**
	 * This is to intialize the location for NPANXX_ID , REGIONID , NPA and NXX.
	 */
   	public NpaNxxDeleteRequest(){

	  	super( 	SOAConstants.NPANXX_DELETE_REQUEST ,
			 	NPANXXID_LOCATION,
			 	REGION_LOCATION,
			 	NPA_LOCATION ,
			 	NXX_LOCATION );

   	}

}

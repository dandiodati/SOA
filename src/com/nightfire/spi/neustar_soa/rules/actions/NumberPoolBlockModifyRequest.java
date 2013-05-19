/**
 * This is the class which is used to generate NumberPoolBlockModifyRequest xml.
 *
 * @author Devaraj
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
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Devaraj			11/23/2004		Created.
												
*/

package com.nightfire.spi.neustar_soa.rules.actions;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NumberPoolBlockModifyRequest extends NPBAction{

    /**
     * This variable contains the base path for NumberPoolBlock Modify request.
     */
    public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                          SOAConstants.NPB_MODIFY_REQUEST;   
    
    /**
     * This variable contains the location of the BlockId l.
     */
   	public static final String BLOCKID_LOCATION = BASE_PATH+"."+
											SOAConstants.BLOCKID;    
   	
   	/**
   	 * This variable contains the location of the RegionId .
   	 */
    public static final String REGION_LOCATION = BASE_PATH+"."+
    										SOAConstants.REGIONID_NODE;
    
	/**
	 * This variable contains the location of the NpaNxxX .
	 */										
	public static final String NPANXXX_LOCATION = BASE_PATH+"."+
												  SOAConstants.NPANXXX;
	
	/**
	 * This variable contains the location of the BlockHolderSPID .
	 */												  
	public static final String BLOCK_HOLDER_SPID_LOCATION = BASE_PATH+"."+
														SOAConstants.BLOCK_HOLDER_SPID;
    /**
     * Constructor for NumberPoolBlockModify Request which will 
     * inturn call the super class constructor.
     *
     */
    public NumberPoolBlockModifyRequest(){

        super( SOAConstants.NPB_MODIFY_REQUEST,
                BLOCKID_LOCATION,
                REGION_LOCATION,
				NPANXXX_LOCATION,
				BLOCK_HOLDER_SPID_LOCATION);

    }

}

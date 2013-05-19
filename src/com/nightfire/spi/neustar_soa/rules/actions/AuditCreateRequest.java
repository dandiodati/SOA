/**
 * This class contains actions that can be taken against a
 * AuditCreate requests.
 *
 * @author Jaganmohan Reddy
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see		com.nightfire.spi.neustar_soa.utils.SOAConstants;
 * @see		com.nightfire.spi.neustar_soa.rules.*;
 * @see		com.nightfire.framework.message.*;
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

import com.nightfire.spi.neustar_soa.rules.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public class AuditCreateRequest extends AuditAction{

	/**
	 * This variable contains the base path for AuditCreate request.
	 */
	public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                          SOAConstants.AUDIT_CREATE_REQUEST;
	
	/**
	 * This variable contains the location of the AuditName location in the 
	 * AuditCreate Request.
	 */
	public static final String AUDITNAME_LOCATION = BASE_PATH+"."+
                                            SOAConstants.AUDITNAME_NODE;
                                            
	/**
	 * This variable contains the location of the SPID location in the 
	 * AuditCreate Request.
	 */
	public static final String AUDIT_SPID_LOCATION = BASE_PATH+"."+
												SOAConstants.AUDIT_SPID_NODE;

	/**
	 * Constructor for AuditCreate Request.
	 */
	public AuditCreateRequest() {

		this( SOAConstants.AUDIT_CREATE_REQUEST, 
      					 AUDITNAME_LOCATION,
      					 AUDIT_SPID_LOCATION );

	}

	/**
	 * Constructor for AuditCreate Request which will 
	 * inturn call the super class constructor.
	 *
	 */
	protected AuditCreateRequest(String name,
								 String auditNameLocation,
								 String auditSPIDLocation) {

		super( name,
			   null,
			   null,
			   auditNameLocation ,
			   auditSPIDLocation );

	}
   
	/**
	 * This method will set the AuditId value in the xml for the given 
	 * request.
     * @param AuditId String
     * @param context Context
     * @param message XMLMessageGenerator
     * @throws MessageException
     */
	protected void setAuditId( String auditId,
							   Context context,
							   XMLMessageGenerator message )
							   throws MessageException{

		if( context.valueExists( Context.AUDITNAME_NODE ) ){

			String auditName = context.getAuditName() ;
 
 			setAuditName( auditName, context, message );
	   
			String spid = context.getSpid();
	
			setSPID( spid, context, message );			   

		} else {

			throw new MessageException(Context.AUDITNAME_NODE+" is required.");

		}
		
	}

}
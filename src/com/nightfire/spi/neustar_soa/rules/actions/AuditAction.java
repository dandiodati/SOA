/**
 * This is the base class for actions that can be taken against
 * Audit related requests.
 *
 * @author Jaganmohan Reddy
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see		com.nightfire.spi.neustar_soa.rules.Context;
 * @see 	com.nightfire.framework.message.MessageException;
 * @see		com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
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

import org.w3c.dom.Document;

import com.nightfire.spi.neustar_soa.rules.Context;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * This is the base class for actions that can be taken against a
 * Audit related requests.
 */
public abstract class AuditAction extends Action {

	/**
	 * This is the path where the region field should be set in the generated
	 * request XML.
	 */
	private String regionLocation;

	/**
	 * This is the path where the AuditId field should be set in the generated
	 * request XML.
	 */
	private String auditIdLocation;
   
	/**
	 * This is the path where the AuditName field should be set in the generated
	 * request XML.
	 */
	private String auditNameLocation;
   
	/**
	 * This is the path where the SPID field should be set in the generated
	 * request XML.
	 */
	private String spidLocation;

	/**
	 * Constructor.
	 * @param name
	 * @param regionLocation
	 * @param auditIdLocation
	 * @param auditNameLocation
	 * @param spidLocation
	 */
	protected AuditAction(String name,
						  String regionLocation,
                      	  String auditIdLocation,
                      	  String auditNameLocation ,
                    	  String spidLocation ){

		super( name );
           
		this.regionLocation = regionLocation;
		  
		this.auditIdLocation = auditIdLocation;
		  
		this.auditNameLocation = auditNameLocation;
		  
		this.spidLocation = spidLocation;

	}

	/**
	 * This method is to generate the reqest XML for an action of this type.
	 *
	 * @param serviceType String this is the GUI service type. For example,
	 *                           SOAPortIn, SOAPortOut, etc.
	 * @param context Context this provides access to the
	 *                        parsed XML document that contains all of
	 *                        the GUI query results for the DB
	 *                        record for which this action would be
	 *                        performed.
	 * @throws MessageException if any error occurs while trying to
	 *                          generate the request or when getting values
	 *                          from the given context.
	 *
	 * @return Document the resulting XML request.
	 *
	 */
	public Document getRequestDocument(String serviceType,
                                      Context context)
                                      throws MessageException{

		XMLMessageGenerator generator =
         		new XMLMessageGenerator(SOAConstants.ROOT);

		setInitSPID(serviceType, context, generator);
      
		setDateSent(context, generator);

		// if the AuditId and region exist, then send the request
		// based on
		if( context.valueExists( Context.AUDITID_NODE )){

			String auditId = context.getAuditId() ;        
		 		 
			if( ( auditId.equals("") ) )
			{
	         
				String auditName = context.getAuditName() ;
				 
				setAuditName( auditName, context, generator );
				
				String spid = context.getSpid();
					
				setSPID( spid, context, generator );	
	         
			 
			} else
			{
			
				setAuditId(auditId, context, generator);		
			        
			}

		}else{

         	throw new MessageException(Context.AUDITID_NODE+" and "+
                                    Context.REGION_NODE+" are required.");

		}

		return generator.getDocument();

	}
	
	/**
	 * This method will set the Init SPID value in the xml for the given 
	 * request.
	 * @param serviceType
	 * @param context
	 * @param message
	 * @throws MessageException
	 */
	protected void setInitSPID(String serviceType,
							Context context,
                            XMLMessageGenerator message)
                            throws MessageException{

		String initSpid = context.getSpid();    

		message.setValue(INIT_SPID_LOCATION, initSpid);

	}

	/**
     * This method will set the region value in the xml for the given 
	 * request.
     * @param region String
     * @param context  Context
     * @param message XMLMessageGenerator
     * @throws MessageException
	 */
	protected void setRegion(String region,
                            Context context,
                            XMLMessageGenerator message)
                            throws MessageException{

		message.setValue( getRegionLocation(), region);

	}

	/**
	 * This method will set the AuditId value in the xml for the given 
	 * request.
     * @param AuditId String
     * @param context Context
     * @param message XMLMessageGenerator
     * @throws MessageException
     */
	protected void setAuditId(String auditId,
                          Context context,
                          XMLMessageGenerator message)
                          throws MessageException{

      message.setValue( getAuditIdLocation(), auditId );

	}
   
	/**
	 * This method will set the AuditName value in the xml for the given 
	 * request.
	 * @param auditName String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 * @throws MessageException
	 */
	protected void setAuditName(String auditName,
							 Context context,
							 XMLMessageGenerator message)
							 throws MessageException{

		message.setValue( getAuditNameLocation(), auditName );

	}
	  
	/**
	 * This method will set the SPID value in the xml for the given 
	 * request.
	 * @param spid String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 * @throws MessageException
	 */
	protected void setSPID(String spid,
						Context context,
						XMLMessageGenerator message)
						throws MessageException{

		message.setValue( getSpidLocation(), spid );

	}  

	/**
	 * Gets the path where the AuditId should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getAuditIdLocation(){

		return auditIdLocation;

	}
   
	/**
	 * Gets the path where the Audit Name should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getAuditNameLocation(){

		return auditNameLocation;

	}

	/**
	 * Gets the path where the region should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getRegionLocation(){

		return regionLocation;

	}
   
	/**
	 * Gets the path where the region should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getSpidLocation(){

		return spidLocation;

	}
   
}

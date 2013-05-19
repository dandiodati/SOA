/**
 * This is the base class for actions that can be taken against
 * GTT related requests.
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


public abstract class GTTAction extends Action {

	/**
     * This is the path where the Lrn field should be set in the generated
     * request XML.
     */
	private String lrnLocation;

	/**
	 * This is the path where the GTTID field should be set in the generated
	 * request XML.
	 */
	private String gttIDLocation;
   	
   	/**
   	 * Constructor.
   	 * @param name
   	 * @param lrnLocation
   	 * @param gttIDLocation
   	 */
	protected GTTAction(String name,
                      	String lrnLocation,
                      	String gttIDLocation){

		super( name );
		
		this.lrnLocation = lrnLocation;
		  
		this.gttIDLocation = gttIDLocation;

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

		// if the GTTID exist, then send the request
		// based on
		if( context.valueExists( Context.GTTID_NODE ) &&
			context.valueExists(Context.LRN_NODE) ){

			String gttId = context.getGttId() ;
			 
			String lrn = context.getLrn();
			 
			setGttId(gttId, context, generator);
			 
			setLrn(lrn, context, generator);

		}	  
		else{

			throw new MessageException(Context.GTTID_NODE+" or "+
									   Context.LRN_NODE+" are required.");

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
	 * This method will set the GttId value in the xml for the given 
	 * request.
	 * @param GttId String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 * @throws MessageException
	 */
	protected void setGttId(String gttId,
                          Context context,
                          XMLMessageGenerator message)
                          throws MessageException{

		message.setValue( getGttIdLocation(), gttId );

	}

	/**
	 * This method will set the Lrn value in the xml for the given 
	 * request.
	 * @param Lrn String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 * @throws MessageException
	 */
	protected void setLrn(String lrn,
                        Context context,
                        XMLMessageGenerator message)
                        throws MessageException{


		message.setValue( getLrnLocation(), lrn );
	
	}

	/**
	 * Gets the path where the GttId should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getGttIdLocation(){

		return gttIDLocation;

	}

	/**
	 * Gets the path where the Lrn should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getLrnLocation(){

		return lrnLocation;

	}

}

/**
 *This is the base class for actions that can be taken against a
 * Location Routing Number(LRN).
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

import org.w3c.dom.Document;

import com.nightfire.spi.neustar_soa.rules.Context;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public abstract class LRNAction extends Action {

	/**
	 * This is the path where the LRNID field should be set in the generated
	 * request XML.
	 */
	private String lrnIdLocation;

	/**
	 * This is the path where the region field should be set in the generated
	 * request XML.
	 */
	private String regionLocation;
   
	/**
	 * This is the path where the region field should be set in the generated
	 * request XML.
	 */
	private String lrnValueLocation;
  
	/**
	 * This is the path where the SPID field should be set in the generated
	 * request XML.
	 */
	private String spidLocation;
	
	/**
	 * Constructor to initialize location values for lrnid and region
	 * @param name String 
	 * @param lrnIdLocation String
	 * @param regionLocation String
	 */
	protected LRNAction(String name,
					  String lrnIdLocation,
					  String regionLocation,
					  String lrnValueLocation ,
					  String spidLocation ){

		super( name );

		this.lrnIdLocation  = lrnIdLocation;
  
		this.regionLocation = regionLocation;
  
		this.lrnValueLocation = lrnValueLocation;
  
		this.spidLocation = spidLocation;

	}
	/**
	 * This method is overriden so that it does not bother to set the LRNID
	 * and region in the request.
	 * @param serviceType 
	 * @param context
	 * @return the document with or without LRN data.
	 * @throws MessageException
	 */
	public Document getRequestDocument(String serviceType,
									  Context context)
									  throws MessageException{

		XMLMessageGenerator generator =
			new XMLMessageGenerator(SOAConstants.ROOT);

		setInitSPID(serviceType, context, generator);
      
		setDateSent(context, generator);

		// if the LRNID and region exist, or if LRNValue and Region exist then send the request
		// based on
	  	if( context.valueExists(Context.LRN_ID_NODE) 
		  	&& context.valueExists(Context.REGION_NODE) ){

			String lrnid = context.getLrnId();
         
			String region = context.getRegionId();
		
			if( lrnid.equals("") ) {
				
				String lrn = context.getLrn() ;
				 
				setLrnValue(lrn, context, generator);			
					
				setRegion( region, context, generator );
						
				String spid = context.getSpid();
						
				setSPID( spid, context, generator );
				
			} else {
							
				setLRNID( lrnid, context, generator );
		         
				setRegion( region, context, generator );
			
			}

		} else {

			throw new MessageException(Context.LRN_ID_NODE+" and "+
									Context.REGION_NODE+" are required.");

		}

		return generator.getDocument();

   }

	/**
	 * This sets the InitSPID value in the generated request. Based
	 * on whether the service type is a port in or a port out,
	 * the new or old service provider value is used to populate
	 * the init SPID.
	 *
	 * @param serviceType String this is the GUI service type. For example,
	 *                           SOAPortIn, SOAPortOut, etc.
	 * @param context Context this contains all of
	 *                        the GUI query results for the DB
	 *                        record for which this action would be
	 *                        performed.
	 * @param message XMLMessageGenerator the XML request message where the
	 *                                    init SPID will be set.
	 * @throws MessageException if any error occurs setting or getting values
	 *                          in the XML.
	 */
	protected void setInitSPID(String serviceType,
							  Context context,
							  XMLMessageGenerator message)
							  throws MessageException{

     
		String initSpid = context.getSpid();    

		message.setValue(INIT_SPID_LOCATION, initSpid);

	}


	/**
	 *
	 * This sets the value for regionLocation
	 * @param region String
	 * @param context  Context
	 * @param message XMLMessageGenerator
	 */
	protected void setRegion(String region,
							Context context,
							XMLMessageGenerator message)
							throws MessageException{

		message.setValue(getRegionLocation(), region);

	}

	/**
	 *
	 * This sets value for LRNID Location. 
	 * @param lrnid String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 */
	protected void setLRNID(String lrnid,
						  Context context,
						  XMLMessageGenerator message)
						  throws MessageException{

	message.setValue( getLRNIDLocation(), lrnid );

	}
   
	/**
     *
     * This sets value for LRN Value Location. 
     * @param lrnid String
     * @param context Context
     * @param message XMLMessageGenerator
     */
	protected void setLrnValue(String lrnValue,
						 Context context,
						 XMLMessageGenerator message)
						 throws MessageException{

		message.setValue( getLrnValueLocation(), lrnValue );

	}
   
	/**
	 *
	 * This sets value for SPID Value Location. 
	 * @param lrnid String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 */
	protected void setSPID(
				   String spid,
				   Context context,
				   XMLMessageGenerator message)
				   throws MessageException {

		message.setValue(getSpidLocation(), spid);

	}

	/**
	 * Gets the path where the LRNID should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getLRNIDLocation(){

		return lrnIdLocation;

	}
   
	/**
	 * Gets the path where the LRN Value should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getLrnValueLocation(){

		return lrnValueLocation;

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
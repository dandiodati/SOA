/**
 * This is the base class for actions that can be taken against a
 * NumberPoolBlock requests.
 *
 * @author Devaraj
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
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Devaraj			11/23/2004		Created.
												
*/

package com.nightfire.spi.neustar_soa.rules.actions;

import org.w3c.dom.Document;

import com.nightfire.spi.neustar_soa.rules.Context;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;


public abstract class NPBAction extends Action {

   	/**
	 * This is the path where the blockId field should be set in the generated
	 * request XML.
	 */
   	private String blockIdLocation;

   	/**
	 * This is the path where the region field should be set in the generated
	 * request XML.
	 */
   	private String regionLocation;
   
   	/**
     * This is the path where the NpaNxxX field should be set in the generated
     * request XML.
     */
   	private String npaNxxXLocation;
   
	/**
	 * This is the path where the SPID field should be set in the generated
	 * request XML.
	 */
   	private String spidLocation;
   
   	/**
	 * Constructor.
	 * @param name
	 * @param blockIdLocation
	 * @param regionLocation
	 * @param npaNxxXLocation
	 * @param spidLocation
	 */
   	protected NPBAction(String name,
					  	String blockIdLocation,
					  	String regionLocation ,
					  	String npaNxxXLocation ,
					  	String spidLocation ){

	  	super( name );

	  	this.blockIdLocation = blockIdLocation; 
	  	    
	  	this.regionLocation = regionLocation;
	  	
	  	this.npaNxxXLocation = npaNxxXLocation;
	  	
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
   	public Document getRequestDocument( String serviceType,
									    Context context)
									    throws MessageException{

	  	XMLMessageGenerator generator =
		 			new XMLMessageGenerator(SOAConstants.ROOT);

	  	setInitSPID(serviceType, context, generator);

	  	setDateSent(context, generator);

	  	if( context.valueExists( Context.BLOCKID_NODE ) &&
			  context.valueExists(Context.REGION_NODE)){

		 	String blockId = context.getBlockId() ;
		 
		 	String region = context.getRegionId();
		 
		 	if(  blockId.equals("") )
		 	{
		       
				String npa = context.getNpa() ;
				 
				String nxx = context.getNxx() ;
	 
				String dashx = context.getDashX() ;
 
				setNpaNxxX(npa+""+nxx+""+dashx, context, generator);			
		
				setRegion( region, context, generator );
	
				String spid = context.getSpid();
	
				setSPID( spid, context, generator );
			 
		 	}else
		 	{
					
				setBlockId(blockId, context, generator);		 
	         
				setRegion(region, context, generator);
		 	
		 	}

	  	}else{

		 	throw new MessageException(Context.BLOCKID_NODE+ " and " +
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
   	protected void setInitSPID( String serviceType,
							    Context context,
							    XMLMessageGenerator message)
							    throws MessageException{

	  	String initSpid;

	  	initSpid = context.getSpid();

	  	message.setValue(INIT_SPID_LOCATION, initSpid);

   	}
   
   	/**
	 *
	 * This sets value for SPID Value Location. 
	 * @param lrnid String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 */
	protected void setSPID( String spid,
						    Context context,
						    XMLMessageGenerator message)
						    throws MessageException{

		message.setValue( getSpidLocation(), spid );

	} 

   	/**
	 * This method will set the Block Id in to the xml message.
	 * @param blockId
	 * @param context
	 * @param message
	 * @throws MessageException
	 */
   	protected void setBlockId( 	String blockId,
								Context context,
								XMLMessageGenerator message)
								throws MessageException{

	  	message.setValue( getBlockIdLocation(), blockId );

   	}
   
   	/**
	 * This method will set the NpaNxxX in to the xml message.
	 * @param NpaNxxX
	 * @param context
	 * @param message
	 * @throws MessageException
	 */
	protected void setNpaNxxX(	String npaNxxX,
						   			Context context,
						   			XMLMessageGenerator message )
						   			throws MessageException{

		 	message.setValue( getNpaNxxXLocation(), npaNxxX );

	}

   	/**
     * This method will set the region id into the xml.
     *
     * @param region String
     * @param context  Context
     * @param message XMLMessageGenerator
     */
  	protected void setRegion( String region,
						      Context context,
						      XMLMessageGenerator message)
						      throws MessageException{

	 	message.setValue(getRegionLocation(), region);

  	}
  
  	/**
     * This method will return the location of Block Id.
     * @return String
     */
   	protected String getBlockIdLocation(){

	  	return blockIdLocation;

   	}
   
   	/**
	 * This method will return the location of NpaNxxX.
	 * @return String
	 */
	 protected String getNpaNxxXLocation(){

		 	return npaNxxXLocation;

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
	 * Gets the path where the SPID should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getSpidLocation(){
	
		return spidLocation;
	
	}  
   
}

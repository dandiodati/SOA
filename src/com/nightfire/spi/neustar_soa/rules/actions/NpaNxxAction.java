/**
 * This is the base class for actions that can be taken against a NPANXX.
 * @author P.B.Nunda Raani
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.spi.neustar_soa.rules.Context;
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
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
	
public abstract class NpaNxxAction extends Action {

    /**
	 * This is the path where the NPANXXID field should be set in the generated
	 * request XML.
	 */
    private String npanxxidLocation;

    /**
	 * This is the path where the region field should be set in the generated
	 * request XML.
	 */
    private String regionLocation;   
   
	/**
	 * This is the path where the NpaValue field should be set in the generated
	 * request XML.
	 */
    private String npaValueLocation ;
    
	/**
	 * This is the path where the NxxValue field should be set in the generated
	 * request XML.
	 */
    private String nxxValueLocation;
   
    /**
	 * This initializes the values for npanxxid and region location.
	 * @param name
	 * @param npanxxidLocation
	 * @param regionLocation
	 */
    protected NpaNxxAction(String name,
					  String npanxxidLocation,
					  String regionLocation,
					  String npaValueLocation ,
					  String nxxValueLocation ){

	  	super( name );

	  	this.npanxxidLocation  = npanxxidLocation;
	  
	  	this.regionLocation = regionLocation;
	  
	  	this.npaValueLocation = npaValueLocation ;
	  
	  	this.nxxValueLocation = nxxValueLocation ;

   	}
	/**
	 * @param serviceType String
	 * @param context Context
	 * @return the document with or with out NPANXX data.
	 * @throws MessageException
	 */
   	public Document getRequestDocument(	String serviceType,
									  	Context context)
									  	throws MessageException{

	  	XMLMessageGenerator generator =
		 					new XMLMessageGenerator(SOAConstants.ROOT);

	  	setInitSPID(serviceType, context, generator);
	  	
	  	setDateSent(context, generator);

	  	// if the NPANXXID and region exist, then send the request
	  	// based on
	  	if( context.valueExists(Context.NPA_NXX_ID_NODE) 
		  && context.valueExists(Context.REGION_NODE) ){

		 	String npanxxid = context.getNpaNxxId();
         
		 	String region = context.getRegionId();
		 
		 	if(  npanxxid.equals("") )
		 	{

				String npa = context.getNpa() ;
         
				String nxx = context.getNxx() ;
		         
				setNpaValue( npa, context, generator );
		         
				setNxxValue( nxx, context, generator );
			 
		 	}else
		 	{
		 	
				setNPANXXID(npanxxid, context, generator);
		     
				setRegion(region, context, generator);
		 	
		 	}

	  }else{

		 throw new MessageException(Context.NPA_NXX_ID_NODE+" and "+
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
   	protected void setInitSPID(	String serviceType,
							  	Context context,
							  	XMLMessageGenerator message)
							  	throws MessageException{

		String initSpid = context.getSpid();    

		message.setValue( INIT_SPID_LOCATION, initSpid );

   	}

   	/**
	 * This sets the region location value into context.
	 * @param region String
	 * @param context  Context
	 * @param message XMLMessageGenerator
	 */
    protected void setRegion(	String region,
								Context context,
								XMLMessageGenerator message)
								throws MessageException{

	  	message.setValue( getRegionLocation(), region );

   	}

    /**
	 * This sets the NpaNxx id location value into context.
	 * @param npanxxid String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 */
   	protected void setNPANXXID( String npanxxid,
						  		Context context,
						  		XMLMessageGenerator message)
						  		throws MessageException{

	  	message.setValue( getNPANXXIDLocation(), npanxxid );

   	}
   
   	/**
     * This sets the Npa  location value into context.
     * @param npanxxid String
     * @param context Context
     * @param message XMLMessageGenerator
     */
  	protected void setNpaValue( String npaValue,
						 		Context context,
						 		XMLMessageGenerator message)
						 		throws MessageException{

	 	message.setValue( getNpaValueLocation(), npaValue );

  	}
  
  	/**
	 * This sets the Nxx location value into context.
	 * @param npanxxid String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 */
	protected void setNxxValue(	String nxxValue,
						   		Context context,
						   		XMLMessageGenerator message)
						   		throws MessageException{

	   message.setValue( getNxxValueLocation(), nxxValue );

	}

   	/**
	 * Gets the path where the NPANXXID should be set in the XML request.
	 *
	 * @return String
	 */
   	protected String getNPANXXIDLocation(){

	  	return npanxxidLocation;

   	}
   
   	/**
	 * Gets the path where the Npa Value should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getNpaValueLocation(){

		return npaValueLocation;

	}
	  
	/**
	 * Gets the path where the Nxx Value should be set in the XML request.
	 *
	 * @return String
	 */
	protected String getNxxValueLocation(){

		return nxxValueLocation;

	}

   	/**
	 * Gets the path where the region should be set in the XML request.
	 *
	 * @return String
	 */
   	protected String getRegionLocation(){

	  	return regionLocation;

   	}
   	
}
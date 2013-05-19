/**
 * This is the class which is used to generate NpaNxxCreateRequest xml.
 * @author P.B.Nunda Raani
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see org.w3c.dom.Document;
 * @see com.nightfire.spi.neustar_soa.rules.Context;
 * @see com.nightfire.framework.message.*;
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

import com.nightfire.spi.neustar_soa.rules.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NpaNxxCreateRequest extends NpaNxxAction{

	/**
	 * XML request message path to NpaNxxCreateRequest. 
	 */
  	 public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                          	SOAConstants.NPANXX_CREATE_REQUEST;
	
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
     * Constructor for NpaNxxCreateRequest which will 
     * call the another class constructor.
     *
     */
   	public NpaNxxCreateRequest() {

      	this(SOAConstants.NPANXX_CREATE_REQUEST, NPA_LOCATION , NXX_LOCATION);

   	}
	
	/**
     * Constructor for NpaNxxCreateRequest Request which will 
     * inturn call the super class constructor.
     *
     */
   	protected NpaNxxCreateRequest( 	String name, 
   									String npaLocation ,
   									String nxxLocation) {

      	super(	name,
            	null,
            	null,
            	npaLocation,
				nxxLocation );

   	}

	/**
	 * This sets the Npa and Nxx value into XML.
	 * @param npanxxid String
	 * @param context Context
	 * @param message XMLMessageGenerator
	 * 
	 * @throws MessageException
	 */
   	protected void setNPANXXID(	String npanxxid,
                          		Context context,
                          		XMLMessageGenerator message)
                          		throws MessageException{

      	// NpaNxxCreate requests cannot be sent based on
      	// NPANXXID and region. Set the NPANXX instead.
      	if( 	context.valueExists( Context.NPA_NODE ) && 
      			context.valueExists( Context.NXX_NODE ) ){

         	String npa = context.getNpa() ;
         
         	String nxx = context.getNxx() ;
         
         	setNpaValue( npa, context, message );
         
		 	setNxxValue( nxx, context, message );

      	}else{

         	throw new MessageException( Context.NPA_NODE+" and " + 
         							 Context.NXX_NODE +" are required.");

      	}

   	}

	/**
	 * This function do nothing.
	 * @param region String
	 * @param context  Context
	 * @param message XMLMessageGenerator
	 * 
	 * @throws MessageException
	 */
   	protected void setRegion(	String region,
                            	Context context,
                            	XMLMessageGenerator message)
                            	throws MessageException{

      	// Do nothing. NpaNxxCreate requests cannot be sent based on
      	// NPANXXID and region.

   	}

}

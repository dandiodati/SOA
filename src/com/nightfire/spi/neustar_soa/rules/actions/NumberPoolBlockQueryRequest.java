/**
 * This is the class which is used to generate NumberPoolBlockQueryRequest xml.
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

import org.w3c.dom.Document;

import com.nightfire.spi.neustar_soa.rules.Context;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

public class NumberPoolBlockQueryRequest extends NPBAction{  
											
   /**
	* Constructor for NumberPoolBlockQuery Request which will 
	* inturn call the super class constructor.
	*/
   public NumberPoolBlockQueryRequest(){

	  	super( 	SOAConstants.NPB_QUERY_REQUEST,              
			  	null,
			  	null,
			  	null,
			  	null);

	}
	
	/**
	 * This method is overriden
	 *
	 * @param serviceType String
	 * @param context Context
	 * @throws MessageException
	 * @return Document
	 */
   	public Document getRequestDocument(String serviceType,
									  Context context)
									  throws MessageException{

	  	XMLMessageGenerator generator =
		 			new XMLMessageGenerator(SOAConstants.ROOT);

	  	setInitSPID(serviceType, context, generator);
	  	
	  	setDateSent(context, generator);

	  	return generator.getDocument();

   	}

}

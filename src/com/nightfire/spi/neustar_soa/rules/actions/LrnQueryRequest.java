/**
 *This class contains actions that can be taken against a
 * (Location Routing Number)LRN Query Request.
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
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;


public class LrnQueryRequest extends LRNAction{

	/**
	 * Constructor for LrnQuery Request which will 
	 * inturn call the super class constructor.
	 *
	 */
	public LrnQueryRequest() {
		
		super( SOAConstants.LRN_QUERY_REQUEST,
							null,
							null ,
							null,
							null);

	}

	/**
	 * This method is overriden so that it does not bother to set the LRNID
	 * or LRN in the request.
	 *
	 * @param serviceType String
	 * @param context XMLMessageParser
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
/**
 * This is the class which is used to generate ServiceProviderQueryRequest xml.
 *
 * @author Devaraj
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see		com.nightfire.spi.neustar_soa.rules.Context
 * @see		com.nightfire.framework.message.MessageException
 * @see		com.nightfire.framework.message.generator.xml.XMLMessageGenerator
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

public class ServiceProviderQueryRequest extends Action {

  
	/**
	 * This is the constructor for ServiceProviderQueryRequest, which inturn calls
	 * Super class constructor.
	 *
	 */
    public ServiceProviderQueryRequest(){

      	super( SOAConstants.SP_QUERY_REQUEST );
                 
    }
	
	/**
	 * This method is to generate the reqest XML for an action of this type.
	 *
	 * @param serviceType String this is the GUI service type. For example,
	 *                           SOA-ServiceProvider.
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

      String initSpid;

      initSpid = context.getSpid();

      message.setValue(INIT_SPID_LOCATION, initSpid);

   }	
   
}

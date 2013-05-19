/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */
package com.nightfire.spi.neustar_soa.servicehandler;

import org.w3c.dom.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;

import com.nightfire.mgrcore.utils.*;
import com.nightfire.mgrcore.common.*;
import com.nightfire.mgrcore.im.*;
import org.w3c.dom.*;
import com.nightfire.framework.constants.PlatformConstants;

import com.nightfire.spi.neustar_soa.rules.AllowableActions;




/**
 * This service handler provides if else routing.
 * 
 */
public class GetAllowableActionsHandler extends ServiceHandlerBase
{

  
    /**
     * Method to intialize the service handler before processing requests.
     *
     * @param properties Properties of type name-value used in initialization.
     *
     * @exception IMProcessingException Thrown if processing fails.
     */
     public void initialize ( Map properties ) throws IMProcessingException
     {
        super.initialize ( properties );
        
     }

    /**
     * Determine the next handler to execute based on xpath conditions.
     *
     * @param context IMContext Control information for the request.
     * @param requestBody   Body of the request.
     *
     * @return ResponseMessage A ResponseMessage object containing the response code and the response body.
     *
     * @exception IMInvalidDataException  Thrown if request data is bad.
     * @exception IMSystemException  Thrown if server can't process any more requests due to system errors.
     * @exception IMSecurityException  Thrown if access is denied.
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    public ServiceHandler.ResponseMessage process ( IMContext context, String requestBody )
        throws IMInvalidDataException, IMSystemException, IMSecurityException, IMProcessingException
    {

        try
        {
            //Instance variables
            String requestHeader = null;
            MessageData response = null;
            String actionName = null;

            XMLPlainGenerator gen = new XMLPlainGenerator(requestBody);
            
            
		gen.remove("Info");

		XMLPlainGenerator requestGen = new XMLPlainGenerator(gen.getNode("0").getNodeName());

		requestGen.copyChildren(requestGen.getDocument().getDocumentElement(),gen.getNode("0"));



      
      List allowable = AllowableActions.getAllowableActions( requestGen.getDocument(), 
                                                             this.getClass().getClassLoader() );
		
        if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS, "Got the following allowable actions: " + allowable);

		Iterator iter = allowable.iterator();
		
		XMLPlainGenerator results = new XMLPlainGenerator("Body");
		Node container = results.create("DataContainer");
		int count = 0;
		while (iter.hasNext()) {
			results.setValue(container, "Data(" + count +").Action", (String)iter.next());
                  count++;
            }
				        
            return new ServiceHandler.ResponseMessage ( PlatformConstants.SUCCESS_VALUE, results.getOutput() );

        }
        catch ( MessageException e )
        {
            throw new IMInvalidDataException ( LOGGING_PREFIX + e.toString() );
        }
        catch ( Exception e )
        {
            throw new IMProcessingException ( LOGGING_PREFIX + e.toString() );
        }

    }

    /**
     * Clean up the service handler so that it can be re-used to process other requests.
     *
     * @exception IMProcessingException Thrown if cleanup is unsuccessful.
     */
    public void reset ( ) throws IMProcessingException
    {
        // nothing to do
    }

     

      

    /**
     * Logging prefix for diagnostic log messages and exception messages.
     */
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

 
}

/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //mgrcommon/common/NMI4.2.1/com/nightfire/manager/common/servicehandler/ManagerQueryServiceHandler.java#1 $
 */
package com.nightfire.manager.common.servicehandler;

import org.w3c.dom.*;

import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;

import com.nightfire.mgrcore.common.*;
import com.nightfire.mgrcore.businessobject.*;
import com.nightfire.mgrcore.im.*;


/**
 * Query Service handler class that executes queries that are dynamically constructed
 * based on the input data and a query description obtained from the repository.
 */
public class ManagerQueryServiceHandler extends DynamicQueryServiceHandler
{

    public static final String SERVICE_TYPE_NODE_LOC = "SERVICE_TYPE_NODE_LOC";

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

        //Get the location of the service type node.
        serviceTypeNode = getRequiredPropertyValue ( SERVICE_TYPE_NODE_LOC );
        
     }//initialize

    /**
     * Get the information from the database as specified by the query criteria in the requestBody.
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
            String action = context.getInvokingAction( );

            String mappedAction = action;

            XMLMessageParser p = new XMLMessageParser( requestBody );

            String svcType = p.getValue( serviceTypeNode );

            if ( Debug.isLevelEnabled( IMConstants.IM_SYSTEM_CONFIG ) )
                Debug.log( IMConstants.IM_SYSTEM_CONFIG, "Service type is [" + svcType + "]." );

            mappedAction = getRequiredPropertyValue( svcType );

            if ( Debug.isLevelEnabled( IMConstants.IM_SYSTEM_CONFIG ) )
                Debug.log( IMConstants.IM_SYSTEM_CONFIG, "Action [" + action + "] maps to [" + mappedAction + "]." );

            context.setInvokingAction ( mappedAction );

            requestBody = preProcess(context, requestBody, p);
            
            return super.process ( context, requestBody );
        }
        catch ( InvalidDataException e )
        {
            throw new IMInvalidDataException( this.getClass().getName() + ": " + e.toString() );
        }
        catch ( Exception e )
        {
            throw new IMProcessingException( this.getClass().getName() + ": " + e.toString() );
        }
    }

    /**
     * Allows for any pre-processing tasks to be carried out, specifically on the
     * request body data.  This default implementation returns the request body
     * untouched.
     *
     * @param  context            IMContext Control information for the request.
     * @param  requestBody        Request body XML.
     * @param  requestBodyParser  Parser of the request body.
     *
     * @exception  IMInvalidDataException  Thrown if the request data is bad.
     *
     * @return  Pre-processed request body.
     */
    protected String preProcess(IMContext context, String requestBody, XMLMessageParser requestBodyParser) throws IMInvalidDataException
    {
        return requestBody;   
    }

    /**
     * Clean up the service handler so that it can be re-used to process other requests.
     *
     * @exception IMProcessingException Thrown if cleanup is unsuccessful.
     */
    public void reset ( ) throws IMProcessingException
    {
        serviceTypeNode = null;
    }

    private String serviceTypeNode = null;

}

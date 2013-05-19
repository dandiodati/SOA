/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //mgrcommon/common/NMI4.2.1/com/nightfire/manager/common/servicehandler/LinkerServiceHandler.java#1 $
 */
package com.nightfire.manager.common.servicehandler;

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
import com.nightfire.mgrcore.businessobject.*;
import com.nightfire.mgrcore.im.*;

import com.nightfire.manager.common.*;

/**
 * Invoke a sequence of service handlers. The service handlers are executed as
 * per the sequence specified in the configuration. The response body of the first
 * service handler forms the request body for the second service handler and so on.
 * The response from the last service handler in the chain is what gets returned
 * to the user.
 */
public class LinkerServiceHandler extends ManagerServiceHandlerBase
{

    /**
     * Prefix for service handler's action name.
     */
    public static final String SERVICE_HANDLER_PREFIX_PROP = "SERVICE_HANDLER";

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

        //Get the list of service handlers to be executed in sequence.
        for ( int Ix=0; true ; Ix++ )
        {
            String actionName = getPropertyValue ( PersistentProperty.getPropNameIteration( SERVICE_HANDLER_PREFIX_PROP, Ix ) );
            if ( StringUtils.hasValue ( actionName ) )
                actionList.add ( actionName );
            else
                break;

        }

        if ( Debug.isLevelEnabled ( IMConstants.IM_MSG_STATUS ) )
            Debug.log ( IMConstants.IM_MSG_STATUS, LOGGING_PREFIX + "Actions that will be performed are [" +
            actionList.toString() + "]." );

     }//initialize

    /**
     * Execute the service handlers specified in the configuration.
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
            XMLMessageGenerator gen = null;
            String requestHeader = null;
            MessageData response = null;
            String actionName = null;

            //Prepare the basic request header.
            gen = new XMLMessageGenerator ( MgrCoreConstants.HEADER_NODE );
            if ( context.roleExists() )
                gen.setValue ( MgrCoreConstants.ROLE_NODE, context.getRoleName() );
            gen.setValue ( MgrCoreConstants.USER_NODE, context.getPrincipalName() );

            //Get a remote reference to the IM
            InteractionManagerClient client = getInteractionManagerClient ( context );

            Iterator iter = actionList.iterator();

            while ( iter.hasNext() )
            {
                actionName = (String) iter.next();

                //Create request header for the particular service handler under consideration.
                gen.setValue ( MgrCoreConstants.ACTION_NODE, actionName );
                requestHeader = gen.generate();

                //If we have processed a previous service handler, then the output of
                //that service handler is the input to the current one, else we are at
                //the first service handler and hence we will use the input to the
                //LinkerServiceHandler as the request body for the first service handler.
                if ( response != null )
                    requestBody = response.body;

                if ( Debug.isLevelEnabled ( IMConstants.IM_MSG_STATUS ) )
                    Debug.log ( IMConstants.IM_MSG_STATUS, LOGGING_PREFIX + "Invoking service handler with action [" +
                    actionName + "]." );

                // We do not make a call directly to the service handler that is
                // servicing that bean, because we want the entry points to the
                // beans/service handlers to be consistent. Also there could be
                // properties on the service handler that only the IMBean knows about.
                // Also the IMBean triggers the resource management of the service
                // handlers. We do not want to duplicate the efforts.
                // Another reason is that when a new BO is being created, any query
                // transaction does not see the new data if the data is not committed,
                // so we want to go through the IM which forces to get a new transaction
                // for each transaction processing.

                response = client.process ( requestHeader, requestBody );
                //At this point we have the response from the service handler
                //We use it to invoke the next service handler, if any, when we enter the loop again.

            }

            String responseBody = response.body;

            //Get response code from last service handler to be returned.
            XMLMessageParser p = new XMLMessageParser ( response.header );
            String responseStatus = p.getValue ( MgrCoreConstants.RESPONSE_CODE_NODE );

            if ( Debug.isLevelEnabled ( IMConstants.IM_MSG_STATUS ) )
                Debug.log ( IMConstants.IM_MSG_STATUS, LOGGING_PREFIX + "Returning response body [" +
                            responseBody + "], response status [" + responseStatus + "]." );

            return new ServiceHandler.ResponseMessage ( responseStatus, responseBody );

        }//try
        catch ( IMInvalidDataException e )
        {
            throw e;
        }
        catch ( MessageException e )
        {
            throw new IMInvalidDataException ( LOGGING_PREFIX + e.toString() );
        }
        catch ( Exception e )
        {
            throw new IMProcessingException ( LOGGING_PREFIX + e.toString() );
        }

    }//process

    /**
     * Clean up the service handler so that it can be re-used to process other requests.
     *
     * @exception IMProcessingException Thrown if cleanup is unsuccessful.
     */
    public void reset ( ) throws IMProcessingException
    {
        actionList.clear();
    }

    /**
     * Logging prefix for diagnostic log messages and exception messages.
     */
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

    private LinkedList actionList = new LinkedList();

}//end of class LinkerServiceHandler

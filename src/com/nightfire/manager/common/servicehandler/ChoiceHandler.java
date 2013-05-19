/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //mgrcommon/common/NMI4.2.1/com/nightfire/manager/common/servicehandler/ChoiceHandler.java#1 $
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
import com.nightfire.framework.message.util.xml.ParsedXPath;



/**
 * This service handler provides if else routing.
 * 
 */
public class ChoiceHandler extends ManagerServiceHandlerBase
{

    // the WHEN properties are iterative
    public static final String WHEN_TEST_PROP ="WHEN_TEST";
    public static final String WHEN_DEST_PROP = "WHEN_DEST";

    public static final String OTHERWISE_DEST_PROP = "OTHERWISE_DEST";
    
    
    private List whenTests = new ArrayList();
    private String otherwiseDest;
    

    

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
        for ( int i=0; true ; i++ )
        {
            String whenTest = getPropertyValue ( PersistentProperty.getPropNameIteration( WHEN_TEST_PROP, i) );
            String whenDest = getPropertyValue ( PersistentProperty.getPropNameIteration( WHEN_DEST_PROP, i) );

            try {
                
                if ( StringUtils.hasValue ( whenTest ) )
                    whenTests.add ( new Choice(new ParsedXPath(whenTest), whenDest) );
                else
                    break;
            }
            catch (FrameworkException e) {
                String err = "Failed to parse xpath expression [" + whenTest +"]";
                Debug.error(err + ": " + e.toString());
                throw new IMProcessingException(err);
            }
            

        }
        
        otherwiseDest = getPropertyValue(OTHERWISE_DEST_PROP);
        
        if (!StringUtils.hasValue(otherwiseDest)) {
            String err = LOGGING_PREFIX + " Property " + OTHERWISE_DEST_PROP +" must exist and have a value.";
            Debug.error(err);
            throw new IMProcessingException(err);
        }
    

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

            XMLMessageParser reqParser = new XMLMessageParser(requestBody);
            
            String nextAction = findNextAction(reqParser.getDocument());
            
            
            //Prepare the basic request header.
            XMLMessageGenerator gen = new XMLMessageGenerator ( MgrCoreConstants.HEADER_NODE );
            if ( context.roleExists() )
                gen.setValue ( MgrCoreConstants.ROLE_NODE, context.getRoleName() );
            gen.setValue ( MgrCoreConstants.USER_NODE, context.getPrincipalName() );

            //Get a remote reference to the IM
            InteractionManagerClient client = getInteractionManagerClient ( context );
     
            //Create request header for the particular service handler under consideration.
            gen.setValue ( MgrCoreConstants.ACTION_NODE, nextAction );
            requestHeader = gen.generate();

            if ( Debug.isLevelEnabled ( IMConstants.IM_MSG_STATUS ) )
                    Debug.log ( IMConstants.IM_MSG_STATUS, LOGGING_PREFIX + "Invoking service handler with action [" +
                    nextAction + "]." );

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

            String responseBody = response.body;

            //Get response code from last service handler to be returned.
            XMLMessageParser p = new XMLMessageParser ( response.header );
            String responseStatus = p.getValue ( MgrCoreConstants.RESPONSE_CODE_NODE );

            if ( Debug.isLevelEnabled ( IMConstants.IM_MSG_STATUS ) )
                Debug.log ( IMConstants.IM_MSG_STATUS, LOGGING_PREFIX + "Returning response body [" +
                            responseBody + "], response status [" + responseStatus + "]." );

            return new ServiceHandler.ResponseMessage ( responseStatus, responseBody );

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
     * Test all when conditions and determine the next actions 
     * to take. If no when condion matches then
     * reuturn the default action.
     *
     * @param doc a <code>Document</code> value
     * @return a <code>String</code> value
     */
    private String findNextAction(Document doc)
    {
        try {
            
            if (whenTests.size() > 0) {
                for(int i = 0; i < whenTests.size(); i++ ) {
                    Choice c = (Choice) whenTests.get(i);
                    if(c.testPattern.getBooleanValue(doc))
                        return c.destHandler;
                }
            }
        }
        catch (Exception e) {
            Debug.error("Failed to evaluate xpath expression: " + e.getMessage());
        }
        
        // if here then there was when tests or none matched.
        // return the default destination.
        return otherwiseDest;
    }
    
                

    private class Choice 
    {
        public Choice(ParsedXPath testPattern, String destHandler)
        {
            this.testPattern = testPattern;
            this.destHandler = destHandler;
        }
        
        public ParsedXPath testPattern;
        public String destHandler;
    }
    

    /**
     * Logging prefix for diagnostic log messages and exception messages.
     */
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

 
}

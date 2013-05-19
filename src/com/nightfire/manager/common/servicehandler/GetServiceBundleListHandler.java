/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //mgrcommon/common/NMI4.2.1/com/nightfire/manager/common/servicehandler/GetServiceBundleListHandler.java#1 $
 */
package com.nightfire.manager.common.servicehandler;

import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.repository.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;

import com.nightfire.mgrcore.common.*;
import com.nightfire.mgrcore.businessobject.*;
import com.nightfire.mgrcore.utils.*;
import com.nightfire.mgrcore.repository.*;
import com.nightfire.mgrcore.im.*;
import com.nightfire.mgrcore.im.query.*;

import com.nightfire.manager.common.*;
import com.nightfire.manager.common.util.*;
import com.nightfire.manager.common.servicehandler.*;

import com.nightfire.manager.common.*;

/**
 * Get a list of service bundles containing summary order information.
 */
public class GetServiceBundleListHandler extends ManagerServiceHandlerBase
{
    /**
     * Get the information from the database for the specified service bundle.
     * The bundle specification is in the requestBody as:
     * <pre>
     * &lt;Body&gt;
     *     &lt;Info&gt;
     *          &lt;BOID value="..." /&gt;
     *          &lt;MetaDataName value="..." /&gt;
     *     &lt;/Info&gt;
     * &lt;/Body>
     * </pre>
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
  
           int startOffset = 0;
           int endOffset   = -1;
           XMLPlainGenerator parser = new XMLPlainGenerator(requestBody);
  
              
            if(parser.exists(QueryEngine.START_OFFSET_NODE) &&
               parser.exists(QueryEngine.END_OFFSET_NODE)) 
            {
                
                startOffset = Integer.parseInt(parser.getValue(QueryEngine.START_OFFSET_NODE));
                endOffset = Integer.parseInt(parser.getValue(QueryEngine.END_OFFSET_NODE));
            }
            
            QueryEngine.QueryResults qr = QueryEngine.executeQuery( context.getInvokingAction(), requestBody, startOffset, endOffset );
            int count = qr.getResultCount( );

            if ( count < 1 )
            {
                return( new ServiceHandler.ResponseMessage( MgrCoreConstants.NO_MATCH_VALUE,
                                                            qr.getResultsAsString() ) );
            }
            else
            {
                XMLMessageGenerator results = new XMLMessageGenerator( qr.getResultsAsDOM() );

                XMLMessageParser bundles = new XMLMessageParser( qr.getResultsAsDOM() );

                for ( int Ix = 0;  Ix < count;  Ix ++ )
                {
                    String metaDataName = bundles.getValue( BUNDLE_ITEM_ROOT_PREFIX + Ix + ")." + BusinessObjectMetaData.META_DATA_NAME );

                    String boid = bundles.getValue( BUNDLE_ITEM_ROOT_PREFIX + Ix + ")." + BusinessObjectMetaData.BOID );

                }

                results.setValue( DynamicQueryServiceHandler.COUNT_NODE_LOC, String.valueOf( count ) );

                return( new ServiceHandler.ResponseMessage( MgrCoreConstants.SUCCESS_VALUE, results.generate() ) );
            }
        }
        catch ( InvalidDataException e )
        {
            Debug.logStackTrace(e);

            throw new IMInvalidDataException( LOGGING_PREFIX + e.toString() );
        }
        catch ( Exception e )
        {
            Debug.logStackTrace(e);

            throw new IMProcessingException( LOGGING_PREFIX + e.toString() );
        }
    }

    /**
     * Logging prefix for diagnostic log messages and exception messages.
     */
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

    //This node is used to wrap all requests being returned to the client. Don't know why we
    //really need this.
    private static final String REQUEST_NODE = "Request";
    
    private static String BUNDLE_ITEM_ROOT_PREFIX = "DataContainer.Data(";

    private static String ORDER_DETAIL_OFFSET = BusinessObjectQuery.RESULT_ROW_CONTAINER + "." + BusinessObjectQuery.RESULT_ROW  + ".";
}

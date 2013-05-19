/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: $
 */
package com.nightfire.manager.common.servicehandler;

import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.repository.*;
import com.nightfire.framework.constants.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;

import com.nightfire.mgrcore.common.*;
import com.nightfire.mgrcore.businessobject.*;
import com.nightfire.mgrcore.utils.*;
import com.nightfire.mgrcore.repository.*;
import com.nightfire.mgrcore.im.*;

import com.nightfire.manager.common.*;
import com.nightfire.manager.common.util.*;
import com.nightfire.manager.common.servicehandler.*;


/**
 * Query for the details of all child objects associated with the indicated
 * parent object.
 */
public class QueryChildrenServiceHandler extends ManagerServiceHandlerBase
{
    /**
     * Parent node of a single child business object's data.
     */
    public static final String DATA_NODE = "Data";

    /**
     * Parent node of all child business object's data.
     */
    public static final String DATA_CONTAINER_NODE = DATA_NODE + PlatformConstants.CONTAINER;


    /**
     * Query for the details of a business object, given its BOID and meta-data name.
     * The required input information is in the requestBody argument as:
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
     * @return A ResponseMessage object containing the response code and the response body.
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
            // Extract the meta-data name and BOID to identify the business object to query.
            XMLMessageParser request = new XMLMessageParser( requestBody );

            String boid = request.getValue( ManagerConstants.BOID_LOC );

            String metaDataName = request.getValue( ManagerConstants.META_DATA_NAME_LOC );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, LOGGING_PREFIX + "Getting children of business object with meta-data name [" 
                           + metaDataName + "], BOID [" + boid + "] ..." );

            XMLMessageGenerator gen = new XMLMessageGenerator( MgrCoreConstants.BODY_NODE );

            // Get the set of child meta-data names from the parent's meta-data.
            BusinessObjectMetaData parentMetaData = BusinessObjectMetaData.getMetaData( metaDataName );

            Set childMetaDataNames = parentMetaData.getChildMetaDataNames( );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, LOGGING_PREFIX + "Candidate child meta-data names:\n" 
                           + childMetaDataNames.toString() );

            BusinessObjectQuery.BOKey[] children = BusinessObjectQuery.getChildren( childMetaDataNames, metaDataName, boid );

            for ( int Ix = 0;  Ix < children.length;  Ix ++ )
            {
                String rootName = DATA_CONTAINER_NODE + "." + DATA_NODE + "(" + Ix + ").";

                BusinessObjectLocal child = new BusinessObjectLocal( children[Ix].metaDataName, children[Ix].boid, false );

                // Now transform the XML response from the business object into the form required by the IM API call.
                XMLMessageParser response = new XMLMessageParser( child.getAsDOM() );

                // Get the meta-data object to extract the xml blob field name and object type.
                BusinessObjectMetaData childMetaData = child.getMetaData( );

                String xmlBlobFieldName = childMetaData.getXMLBlobFieldName( );

                Node[] fieldNodes = response.getChildNodes( "." );

                for ( int Jx = 0;  Jx < fieldNodes.length;  Jx++ )
                {
                    String nodeName = fieldNodes[Jx].getNodeName( );

                    if ( !nodeName.equals( xmlBlobFieldName ) && XMLMessageBase.isValueNode( fieldNodes[Jx] ) )
                        gen.setValue( rootName + "." + nodeName, response.getValue( nodeName ) );
                }

                // Put the object type value (bundle, component, order, modifier) into the xml.
                gen.setValue( rootName + "." + MgrCoreConstants.BO_OBJECT_TYPE_NODE, childMetaData.getObjectType() );

                String subType = childMetaData.getApplicationSpecific( ManagerConstants.BO_OBJECT_SUB_TYPE_NODE );

                if ( !StringUtils.hasValue( subType ) )
                    subType = "unknown";

                gen.setValue( rootName + "." + ManagerConstants.BO_OBJECT_SUB_TYPE_NODE, subType );

                // Populate the request sub-node with the appropriate value from the business object.
                gen.setValue( rootName + "." + REQUEST_NODE, response.getNode( xmlBlobFieldName + "." + XMLBlobAccess.DATA_REQUEST_ROOT ) );
            }

            String responseBody = gen.generate( );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, LOGGING_PREFIX + "Business object details after post-processing:\n" + responseBody );

            if ( children.length > 0 )
                return( new ServiceHandler.ResponseMessage( MgrCoreConstants.SUCCESS_VALUE, responseBody ) );
            else
                return( new ServiceHandler.ResponseMessage( MgrCoreConstants.NO_MATCH_VALUE, responseBody ) );
        }
        catch ( MessageException e )
        {
            Debug.logStackTrace(e);

            throw new IMInvalidDataException ( LOGGING_PREFIX + e.toString() );
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


     // Logging prefix for diagnostic log messages and exception messages.
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

    private static final String REQUEST_NODE = "Request";
}

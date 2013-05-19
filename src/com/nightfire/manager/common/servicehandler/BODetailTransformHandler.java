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
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.transformer.*;
import com.nightfire.framework.message.util.xml.*;

import com.nightfire.mgrcore.common.*;
import com.nightfire.mgrcore.businessobject.*;
import com.nightfire.mgrcore.utils.*;
import com.nightfire.mgrcore.repository.*;
import com.nightfire.mgrcore.im.*;

import com.nightfire.manager.common.*;
import com.nightfire.manager.common.util.*;
import com.nightfire.manager.common.servicehandler.*;


/**
 * Query for the details of a business object, given its BOID and meta-data name.
 * The Request will be transformed using the configured style sheet.
 */
public class BODetailTransformHandler extends ManagerServiceHandlerBase
{
    /**
     * The method to invoke on the Business Object.
     */
    public static final String STYLE_SHEET_PROP = "STYLE_SHEET";

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

        styleSheet = getPropertyValue ( STYLE_SHEET_PROP );

        if (Debug.isLevelEnabled(Debug.MSG_DATA))
        {
            Debug.log(Debug.MSG_DATA, "Got style sheet config: [" + styleSheet + "].");
        }
    
     }//initialize

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
                Debug.log( Debug.MSG_DATA, LOGGING_PREFIX + "Getting business object details for meta-data name [" 
                           + metaDataName + "], BOID [" + boid + "] ..." );

            // Find the business object and extract its data in XML form.
            BusinessObjectLocal bol = new BusinessObjectLocal( metaDataName, boid, false );

            // Now transform the XML response from the business object into the form required by the IM API call.
            XMLMessageParser response = new XMLMessageParser( bol.getAsDOM() );

            String type = response.getValue( ManagerConstants.BO_ORDER_TYPE );

            XMLMessageGenerator gen = new XMLMessageGenerator( MgrCoreConstants.BODY_NODE );

            // Get the meta-data object to extract the xml blob field name and object type.
            BusinessObjectMetaData metaData = BusinessObjectMetaData.getMetaData( metaDataName );

            String xmlBlobFieldName = metaData.getXMLBlobFieldName( );

            Node[] fieldNodes = response.getChildNodes( "." );

            for ( int Ix = 0;  Ix < fieldNodes.length;  Ix++ )
            {
                String nodeName = fieldNodes[Ix].getNodeName( );

                if ( !nodeName.equals( xmlBlobFieldName ) && XMLMessageBase.isValueNode( fieldNodes[Ix] ) )
                    gen.setValue( type + "." + MgrCoreConstants.INFO_NODE + "." + nodeName, response.getValue( nodeName ) );
            }

            // Put the object type value (bundle, component, order, modifier) into the xml.
            gen.setValue( type + "." + MgrCoreConstants.INFO_NODE + "." + MgrCoreConstants.BO_OBJECT_TYPE_NODE, metaData.getObjectType() );

            String subType = metaData.getApplicationSpecific( ManagerConstants.BO_OBJECT_SUB_TYPE_NODE );

            if ( !StringUtils.hasValue( subType ) )
                subType = "unknown";

            gen.setValue( type + "." + MgrCoreConstants.INFO_NODE + "." + ManagerConstants.BO_OBJECT_SUB_TYPE_NODE, subType );

            // Populate the request sub-node with the appropriate value from the business object.

            Node requestNode = response.getNode( xmlBlobFieldName + "." + XMLBlobAccess.DATA_REQUEST_ROOT );

            // Transform the request is configured.
            if (styleSheet != null)
            {
                Document oldRequest = XMLExtractor.extract(xmlBlobFieldName + "." + XMLBlobAccess.DATA_REQUEST_ROOT, requestNode.getNodeName(), response);

                if (Debug.isLevelEnabled( Debug.MSG_DATA))
                {
                    Debug.log(Debug.MSG_DATA, "Request message before transforming: [" + new XMLMessageGenerator(oldRequest).generate());
                }
                
                Document newRequest = transformRequest(oldRequest, styleSheet);

                if (Debug.isLevelEnabled( Debug.MSG_DATA))
                {
                    Debug.log(Debug.MSG_DATA, "Request message after transforming: [" + new XMLMessageGenerator(newRequest).generate());
                }
                
                requestNode = newRequest.getDocumentElement();
            }
            else
            {
                requestNode = response.getNode( xmlBlobFieldName + "." + XMLBlobAccess.DATA_REQUEST_ROOT );
            }
            
            
            gen.setValue( type + "." + requestNode.getNodeName(), requestNode  );

            String responseBody = gen.generate( );

            if ( Debug.isLevelEnabled( Debug.MSG_DATA ) )
                Debug.log( Debug.MSG_DATA, LOGGING_PREFIX + "Business object details after post-processing:\n" + responseBody );

            return( new ServiceHandler.ResponseMessage( MgrCoreConstants.SUCCESS_VALUE, responseBody ) );
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

    /**
     * Transform the request with the given style sheet.
     **/
    protected Document transformRequest(Document request, String xslFile) throws MessageException
    {
        XSLMessageTransformer transformer = MessageTransformerCache.getInstance().get(xslFile);

        return transformer.transform(request);
    }

    private String styleSheet;

     // Logging prefix for diagnostic log messages and exception messages.
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

}

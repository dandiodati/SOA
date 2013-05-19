/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //mgrcommon/common/NMI4.2.1/com/nightfire/manager/common/servicehandler/CopyServiceBundleHandler.java#1 $
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

import com.nightfire.manager.common.*;
import com.nightfire.manager.common.util.*;
import com.nightfire.manager.common.servicehandler.*;

import com.nightfire.manager.common.*;

/**
 * Query Service handler class that queries on orders/services.
 * Returns the type information and contained "Request" of the 
 * identified bundle and assoicated orders. The information can be used to created a copy
 * of this bundle.
 * ASSUMPTION: Any order that is completed, must have a completed date set.
 */
public class CopyServiceBundleHandler extends ManagerServiceHandlerBase
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
        //Get the metadata name and BOID of the bundle from the request body.
        String svcBundleMetaDataName = null;
        String svcBundleBOID = null;

        try
        {
            XMLMessageParser reqParser = new XMLMessageParser ( requestBody );

            // Get service bundle BOID
            svcBundleBOID = reqParser.getValue ( ManagerConstants.BOID_LOC );
            svcBundleMetaDataName = reqParser.getValue ( ManagerConstants.META_DATA_NAME_LOC );
        }
        catch ( MessageException e )
        {
            throw new IMInvalidDataException ( LOGGING_PREFIX + e.toString() );
        }

        // Get the children
        String responseBody = null;
        String responseStatus = null;
        try
        {

            //Create the response message body as specified by the IM API.
            XMLMessageGenerator resultGenerator = new XMLMessageGenerator ( MgrCoreConstants.BODY_NODE );

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Getting service bundle information..." );

            // Append the service bundle information
            BusinessObject serviceBundle = getBusinessObject ( context, svcBundleMetaDataName, svcBundleBOID );
            XMLMessageParser parser = new XMLMessageParser ( serviceBundle.get() );
            BusinessObjectInfo boInfo = new BusinessObjectInfo ( serviceBundle, parser );
            
            appendBOInfo ( resultGenerator, ".", boInfo, false); // no request to be appended.

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Getting order information..." );

            // Get the order (grand) children of this service bundle. For this first get the service component
            //children of the service bundle.
            List currentOrdersInfo = new LinkedList();
            Collection svcComponents = serviceBundle.getChildren();
            Iterator svcComponentIterator = svcComponents.iterator();
            while ( svcComponentIterator.hasNext() )
            {
                BusinessObject svcComponent = (BusinessObject) svcComponentIterator.next();
                //Get all orders for a service component. A service component always has one child at least,
                //else it just wont be created.
                Collection orders = svcComponent.getChildren();

                //Of these orders, now identify which is the active or latest order for that service component.
                setCurrentOrder ( orders, currentOrdersInfo );
            }

            //Append each of the order details to the resultant XML as specified by the API.
            processCurrentOrderInfo ( resultGenerator, currentOrdersInfo );

            responseBody = resultGenerator.generate ( );

            responseStatus = MgrCoreConstants.SUCCESS_VALUE;
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

        return new ServiceHandler.ResponseMessage ( responseStatus, responseBody );

    }//process

    /**
     * Identify the active or last completed order amongst all the children of the current service
     * component under consideration. The currentOrdersInfo carries the running list of such
     * current orders identified and the orders is the orders for a particular service component
     * which have to result in a sigle winner order.
     *
     * @param orders Order children of a particular service component.
     * @param currentOrdersInfo Cumulative list of all active/last completed children for all service components.
     *
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    private void setCurrentOrder ( Collection orders, List currentOrdersInfo )
    throws IMProcessingException
    {
        //First find if any order is active. If yes, then we have found our current order.
        //If no order is active, then the current order whose information is to be retuned is the
        //the one that was last completed.
        boolean found = false;
        Iterator iter = orders.iterator();

        //The tempOrdersInfo list is used to find the order that was completed last,
        //if we do not find any active order.
        List tempOrdersInfo = new LinkedList();

        try
        {
            while ( iter.hasNext() )
            {
                BusinessObject order = (BusinessObject) iter.next();
                //We need to access information on this business object for three reasons. One is to
                //decide whether it is a current order. Second, to figure out how to return the information
                //it owns, and lastly for the actual information to be returned.
                //Hence, instead of making a bunch of remote calls on the Business Object, we get the
                //data out of the order object and examine it.
                XMLMessageParser parser = new XMLMessageParser ( order.get() );
                BusinessObjectInfo orderInfo = new BusinessObjectInfo ( order, parser );

                //Add by default to tempOrdersInfo, instead of wasting information fetched.
                tempOrdersInfo.add ( orderInfo );

                if ( !( parser.getValue( ManagerConstants.BO_CURRENT_STATE ) ).equals( ManagerConstants.DONE_STATE ) )
                {
                    found = true;
                    if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                        Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Found active order with BOID [" +
                        parser.getValue( ManagerConstants.BO_BOID ) + "] for service type [" +
                        parser.getValue( ManagerConstants.BO_ORDER_TYPE ) + "]." );
                    currentOrdersInfo.add ( orderInfo );
                    break;
                }

            }//while
        }
        catch ( Exception e )
        {
            throw new IMProcessingException ( "Could not access active order, " + e.toString() );
        }

        /*****
         ****The following piece of code assumes that every completed order has a completed date set.
         ***/
        if ( !found )
        //No order is active, hence we now have to look at the last modified dates of all orders.
        //This is all the order information contained in tempOrdersInfo.
        {
            BusinessObjectInfo identifiedOrderInfo = null;
            long lastCreatedDate = -1;
            iter = tempOrdersInfo.iterator();
            try
            {
                while ( iter.hasNext() )
                {
                    BusinessObjectInfo currentOrderInfo = ( BusinessObjectInfo ) iter.next();
                    long currentCompletedDate = MgrDateUtils.getNumericDate ( currentOrderInfo.parser.getValue ( ManagerConstants.BO_COMPLETED_DATE ) );
                    if ( lastCreatedDate < currentCompletedDate )
                    {
                        lastCreatedDate = currentCompletedDate;
                        identifiedOrderInfo = currentOrderInfo;
                    }
                }//while

                if ( ( lastCreatedDate == -1 ) || ( identifiedOrderInfo == null ) )
                {
                    throw new IMProcessingException ( "Did not find any order that is completed with a valid completed date." );
                }
                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Found latest created order with BOID [" +
                                identifiedOrderInfo.parser.getValue( ManagerConstants.BO_BOID ) + "] for service type [" +
                                identifiedOrderInfo.parser.getValue( ManagerConstants.BO_ORDER_TYPE ) + "]." );

            }//try
            catch ( Exception e )
            {
                throw new IMProcessingException ( "Could not access inactive order with a valid completed date, " + e.toString() );
            }

            currentOrdersInfo.add ( identifiedOrderInfo );
        }//if
    }//setCurrentOrder

    /**
     * Set the information from the currentOrderInfo list into the resultGenerator.
     *
     * @param resultGenerator Result XML generator which is to be updated to contain information of the orders in the currentOrderInfo list.
     * @param currentOrdersInfo Cumulative list of all active/last completed children for all service components.
     *
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    private void processCurrentOrderInfo ( XMLMessageGenerator resultGenerator, List currentOrdersInfo )
    throws IMProcessingException
    {
        //At this point we have the active/latest order for each service type identified and it is in the
        //currentOrdersInfo list. Now, if more than one service component are of the same type, then
        //their information is to be returned in a container node. Following code essentially does just that.
        try
        {
            List tempList = new LinkedList ();
            while ( !currentOrdersInfo.isEmpty() )
            {
                //We use a ListIterator, so that we can remove items from the currentOrdersInfo.
                ListIterator iter = currentOrdersInfo.listIterator();
                String type = null;
                String currentType = null;
                while ( iter.hasNext() )
                {
                    //If we have orders of the same service type, put them in the tempList and
                    //process them together.
                    BusinessObjectInfo orderInfo = (BusinessObjectInfo) iter.next();
                    currentType = orderInfo.parser.getValue ( ManagerConstants.BO_ORDER_TYPE );
                    if ( type == null )
                        type = currentType;
                    if ( type.equals ( currentType ) )
                    {
                        iter.remove();
                        tempList.add ( orderInfo );
                    }
                }
                //Process orders of the same service type together.
                appendMultipleBOInfo ( resultGenerator, tempList, type );
                tempList.clear();
            }
        }
        catch ( Exception e )
        {
            throw new IMProcessingException ( "Cannot process the current orders identified, " + e.toString() );
        }
        
    }

    /**
     * Intermediate step before invoking appendBOInfo. The information of all orders of the same service type
     * has to be returned collectively in the result generator. This method facilitates that objective.
     *
     * @param resultGenerator Result XML generator.
     * @param orders List of current orders for all service components of type serviceType.
     * @param serviceType The service type of all orders in the "orders" list.
     *
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    private void appendMultipleBOInfo ( XMLMessageGenerator resultGenerator, List orders, String serviceType )
    throws IMProcessingException
    {
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "About to add [" + orders.size() +
            "] orders information for service type [" +  serviceType + "]." );

        try
        {
            if ( orders.size() == 1 )
            {
                // Single child.
                String childNodePath = serviceType;
                appendBOInfo ( resultGenerator, childNodePath, (BusinessObjectInfo) orders.get( 0 ), true );

            }
            else if ( orders.size() > 1 )
            {
                // Multiple children.
                String containerPath   = serviceType + MgrCoreConstants.CONTAINER;
                resultGenerator.setAttributeValue ( containerPath, "type", "container" );
                String childNodePrefix = containerPath + "." + serviceType + "(";

                Iterator iter = orders.iterator();
                for ( int Ix = 0; iter.hasNext(); Ix++)
                {
                    BusinessObjectInfo orderInfo = (BusinessObjectInfo) iter.next();
                    String childNodePath = childNodePrefix + Ix + ")";

                    appendBOInfo ( resultGenerator, childNodePath, orderInfo, true );
                }
            }
        }//try
        catch ( Exception e )
        {
            throw new IMProcessingException ( "Cannot append information for service type [" + serviceType + "], " + e.toString() );
        }
    }

    /**
     * Adds the XML representation of a business object to the XML result generator.
     *
     * @param resultGenerator Result XML generator which will eventually contain the service bundle details.
     * @param targetNode      Node to add the business object information to.
     * @param boInfo          The business object information to be added to the resultGenerator.
     * @param addRequest      Flag indicating whether to have an additional "Request"
     *                        node wrap the body portion of the XML data.
     *
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    private void appendBOInfo ( XMLMessageGenerator resultGenerator, String targetNode, BusinessObjectInfo boInfo,  boolean addRequest )
    throws IMProcessingException
    {
        try
        {

            BusinessObject bo = boInfo.businessObject;
            XMLMessageParser boParser = boInfo.parser;

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Adding business object with metaDataName [" +
                            boParser.getValue ( ManagerConstants.BO_META_DATA_NAME ) + "], BOID [" +
                            boParser.getValue ( ManagerConstants.BO_BOID ) + "] to node [" + targetNode + "] ..." );

            //All non-blob data goes under the Info node.
            BusinessObjectMetaData boMetaData       = BusinessObjectMetaData.getMetaData(bo.getMetaDataAccessName());
            String                 xmlBlobFieldName = boMetaData.getXMLBlobFieldName();
            String                 infoNodePath     = targetNode + "." + MgrCoreConstants.INFO_NODE + ".";
            Node[]                 childNodes       = boParser.getChildNodes(".");

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, LOGGING_PREFIX + "XML-blob field name is [" + xmlBlobFieldName + "]." );

            // Generate all the necessary info nodes for this service, skipping the
            // "blob" node.

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, LOGGING_PREFIX + "Adding MetaModelName and Supplier..." );

            String nodeToAdd = infoNodePath + MgrCoreConstants.META_DATA_NAME_NODE;
            resultGenerator.setValue(nodeToAdd,
                                     boParser.getValue(MgrCoreConstants.META_DATA_NAME_NODE));
            
             if (boParser.exists(ManagerConstants.BO_SVC_BUNDLE_NAME))
             {                 
                 nodeToAdd = infoNodePath + ManagerConstants.BO_SVC_BUNDLE_NAME;                 
                 resultGenerator.setValue(nodeToAdd, boParser.getValue(ManagerConstants.BO_SVC_BUNDLE_NAME));
             }

            nodeToAdd = infoNodePath + MgrCoreConstants.SUPPLIER_NODE;
            if (boParser.exists(MgrCoreConstants.SUPPLIER_NODE))
            {
                resultGenerator.setValue(nodeToAdd,
                                     boParser.getValue(MgrCoreConstants.SUPPLIER_NODE));
            }

            if (addRequest)
            {
                String xmlBlob = bo.get(xmlBlobFieldName);

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, LOGGING_PREFIX + "Appending blob column." );

                XMLBlobAccess    xmlBlobAccess = new XMLBlobAccess(new XMLMessageParser(xmlBlob).getDocument());

                XMLMessageParser requestParser = new XMLMessageParser(xmlBlobAccess.getRequest());

                resultGenerator.setValue(targetNode + "." + REQUEST_NODE, requestParser.getNode("."));
            }
        }//try
        catch ( Exception e )
        {
            throw new IMProcessingException ( "Cannot append business object information to result XML, " + e.toString() );
        }
    }

    /**
     * Get the BusinessObject for the specified metaDataName and boid.
     */
    private BusinessObject getBusinessObject ( IMContext context, String metaDataName, String boID )
    throws javax.ejb.FinderException, java.rmi.RemoteException
    {
        // Create the business object client.
        BusinessObjectClient boc = getBusinessObjectClient(metaDataName, context);

        BusinessObject bo = boc.find(new BusinessObjectPrimaryKey(boID));

        return bo;
    }

    /**
     * Inner class to contain the BusinessObject as well as its XML representation to avoid
     * calls across the network to fetch the XML representation several times.
     */
    private static class BusinessObjectInfo
    {
        public BusinessObjectInfo ( BusinessObject bo, XMLMessageParser parser )
        {
            this.businessObject = bo;
            this.parser = parser;
        }

        public BusinessObject businessObject = null;
        public XMLMessageParser parser = null;
    }

    /**
     * Logging prefix for diagnostic log messages and exception messages.
     */
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

    //This node is used to wrap all requests being returned to the client. Don't know why we
    //really need this.
    private static final String REQUEST_NODE = "Request";
    
}//end of class CopyServiceBundleHandler

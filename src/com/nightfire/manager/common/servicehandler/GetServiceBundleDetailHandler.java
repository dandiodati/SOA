/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //mgrcommon/common/NMI4.2.1/com/nightfire/manager/common/servicehandler/GetServiceBundleDetailHandler.java#2 $
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
import com.nightfire.security.*;


/**
 * Query Service handler class that queries on orders/services.
 * ASSUMPTION: Any order that is completed, must have a completed date set.
 */
public class GetServiceBundleDetailHandler extends ManagerServiceHandlerBase
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
     * The BOID and MetaDataName can belong to any object in that bundle.
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
        BusinessObjectQuery.BOKey bundleBOKey = getBundleBOKey ( requestBody );

        String svcBundleMetaDataName = bundleBOKey.metaDataName;
        String svcBundleBOID = bundleBOKey.boid;

	boolean bundleLevelAuthorization = false;

        // Get the children
        String responseBody = null;
        String responseStatus = null;
        try
        {
            
		
	//BEGIN SECURITY: User/Order filter
        //This is a hack to prevent users from viewing orders that they should not be able
        //to see via the Bundle Search page.  We assume that if a user has access to a bundle then
        //they should have access to all the components in a bundle.  
        //Here when requesting the details for a bundle we authorize that the user has access to each 
        //component in the bundle, if not an IMSecurityException is thrown.
                
        CustomerContext c = CustomerContext.getInstance();    
        SecurityService ss = SecurityService.getInstance( c.getCustomerID() );
                                
        if ( !ss.authorize ( c.getUserID(), svcBundleMetaDataName.toLowerCase() ) )
        {
		Debug.log( Debug.MSG_STATUS, "GetServiceBundleDetails: user ["+ c.getUserID() +"] "
                                              + "does not have permission ["+svcBundleMetaDataName.toLowerCase()
                                              +"] so removing from business object query." );
                        
                throw new IMSecurityException ("user ["+c.getUserID()+"] is unauthorized for the requested action");
                    
	}


        //END SECURITY: User/Order Detail filter:	
		
	    //Create the response message body as specified by the IM API.
            XMLMessageGenerator resultGenerator = new XMLMessageGenerator ( MgrCoreConstants.BODY_NODE );

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Getting service bundle information..." );

            // Append the service bundle information
            BusinessObjectLocal serviceBundle = new BusinessObjectLocal( svcBundleMetaDataName, svcBundleBOID, false );

            XMLMessageParser parser = new XMLMessageParser ( serviceBundle.getAsDOM() );

            BusinessObjectInfo boInfo = new BusinessObjectInfo ( serviceBundle, parser );
            
            appendBOInfo ( resultGenerator, ".", boInfo, false); // no request to be appended.

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Getting order information..." );

            // Get the order (grand) children of this service bundle. For this first get the service component
            //children of the service bundle.
            Set svcComponentMetaDataNames = serviceBundle.getMetaData().getChildMetaDataNames( );

            BusinessObjectQuery.BOKey[] svcComponents = BusinessObjectQuery.getChildren( svcComponentMetaDataNames, 
                                                                                         svcBundleMetaDataName, svcBundleBOID );

            for ( int Ix = 0;  Ix < svcComponents.length;  Ix ++ )
            {
                BusinessObjectLocal svcComponent = new BusinessObjectLocal( svcComponents[Ix].metaDataName, 
                                                                            svcComponents[Ix].boid, false );

                Set orderMetaDataNames = svcComponent.getMetaData().getChildMetaDataNames( );
                
                
                //Get all orders for a service component. A service component always has one child at least,
                //else it just wont be created.
                BusinessObjectQuery.BOKey[] orders = BusinessObjectQuery.getChildren( orderMetaDataNames, svcComponents[Ix].metaDataName, 
                                                                                      svcComponents[Ix].boid );
                                                                                      
               
                //first check if we have any real business objects associated with this service component.
                if ( orders.length == 0 )
                {
                    //we have not decomposed this service component yet so return what is available
                    //in the service component BLOB.
                    addOrdersBeforeDecomposition( svcComponent, resultGenerator );
                        
                }
                
                else
                {
                    //Add the real BOs associated with this Service Component to the resultant xml.
                    addOrdersAfterDecomposition ( orders, resultGenerator );
                }
                
            }
        
            responseBody = resultGenerator.generate ( );

            responseStatus = MgrCoreConstants.SUCCESS_VALUE;
        }
        catch ( InvalidDataException e )
        {
            Debug.logStackTrace(e);
            throw new IMInvalidDataException( LOGGING_PREFIX + e.toString() );
        }
        catch (IMSecurityException e) 
        {
            throw e;
        }
        catch ( Exception e )
        {
            Debug.logStackTrace(e);
            throw new IMProcessingException( LOGGING_PREFIX + e.toString() );
        }

        return new ServiceHandler.ResponseMessage ( responseStatus, responseBody );

    }//process

    /**
     * Get the bundle BOID and metaDataName to which the requestBody information belongs.
     * The request body structure is:
     * <pre>
     * &lt;Body&gt;
     *     &lt;Info&gt;
     *          &lt;BOID value="..." /&gt;
     *          &lt;MetaDataName value="..." /&gt;
     *     &lt;/Info&gt;
     * &lt;/Body>
     * </pre>
     * The BOID and MetaDataName can belong to any business object in the bundle.
     *
     * @param requestBody   Body of the request.
     *
     * @return ResponseMessage A ResponseMessage object containing the response code and the response body.
     *
     * @exception IMInvalidDataException  Thrown if request data is bad.
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    protected BusinessObjectQuery.BOKey getBundleBOKey ( String requestBody )
            throws IMInvalidDataException, IMProcessingException
    {
        Debug.log ( Debug.BENCHMARK, "ENTER: getBundleBOKey." );

        String boid = null;
        String metaDataName = null;
        try
        {
            XMLMessageParser reqParser = new XMLMessageParser ( requestBody );

            // Get the BOID and metaDataName from the request message. This may or may not
            //be the bundle information. This could well be the boid of an order/ordermodifier/
            //servicecomponent in the bundle. We want to get the bundle boid from the given boid
            //and get the details of the bundle once we have the bundle boid.
            boid = reqParser.getValue ( ManagerConstants.BOID_LOC );
            metaDataName = reqParser.getValue ( ManagerConstants.META_DATA_NAME_LOC );
        }
        catch ( MessageException e )
        {
            throw new IMInvalidDataException ( LOGGING_PREFIX + e.toString() );
        }
        //Set parent to current business object key for initialization purposes
        BusinessObjectQuery.BOKey parentBOKey = new BusinessObjectQuery.BOKey ( metaDataName, boid );
        BusinessObjectQuery.BOKey currentBOKey = null;

        while ( parentBOKey != null )
        {
            currentBOKey = parentBOKey;
            parentBOKey = BusinessObjectQuery.getParent ( currentBOKey.metaDataName, currentBOKey.boid );
        }

        //At this point, the currentBOKey is the BO that has no parent. So the currentBOKey is the bundle
        //level key. So we can return that.
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Bundle key of BO [" +
                        metaDataName + "][" + boid + "] is [" + currentBOKey.metaDataName +
                        "][" + currentBOKey.boid + "]." );

        Debug.log ( Debug.BENCHMARK, "EXIT: getBundleBOKey." );
                                
        return currentBOKey;

    }//getBundleBOKey

   
    
    /**
     * Add all business object associated with this service component to resultant xml.
     *
     * @param orders Order children of a particular service component.
     * @param gen result generator
     *
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    protected void addOrdersAfterDecomposition( BusinessObjectQuery.BOKey[] orders, XMLMessageGenerator gen )
    throws IMProcessingException
    {

        try
        {
            for ( int Ix = 0;  Ix < orders.length;  Ix ++ )
            {
                BusinessObjectLocal order = new BusinessObjectLocal( orders[Ix].metaDataName, 
                                                                     orders[Ix].boid, false );

                XMLMessageParser parser = new XMLMessageParser ( order.getAsDOM() );

                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Order information fetched is:\n" +
                    parser.getXMLDocumentAsString() );
                    
                //A BusinessObjectInfo is yet another description of the BO including the columns and blob.
                BusinessObjectInfo orderInfo = new BusinessObjectInfo ( order, parser );
                
                String childNodePath = orderInfo.parser.getValue ( ManagerConstants.BO_ORDER_TYPE );
                
                //add the current BO Info to the result to be returned.
                appendBOInfo ( gen, childNodePath, orderInfo, true );
  
            }//for
        }
        catch ( Exception e )
        {
            throw new IMProcessingException ( "Unable to generate resultant xml, " + e.toString() );
        }

       
    }//addOrders
    
    
    /**
     * Add all business object associated with this service component to resultant xml.
     *
     * @param orders Order children of a particular service component.
     * @param gen result generator
     *
     * @exception IMProcessingException  Thrown if a transient processing error occurs.
     */
    protected void addOrdersBeforeDecomposition( BusinessObjectLocal bo, XMLMessageGenerator gen )
    throws IMProcessingException
    {

        try
        {
            
            XMLMessageParser parser = new XMLMessageParser ( bo.getAsDOM() );
            
            if (Debug.isLevelEnabled(Debug.STATE_LIFECYCLE) )
            {
                XMLMessageGenerator orderData = new XMLMessageGenerator(bo.getAsDOM() );
                Debug.log(Debug.STATE_LIFECYCLE, "Svc component order XML before decomposition:\n"+orderData.generate() );
                
            }
            
            gen.setValue(".", parser.getNode("BusinessObjectXMLBlob.Manager.OrderData") );
            
        }
        catch ( Exception e )
        {
            throw new IMProcessingException ( "Unable to generate resultant xml, " + e.toString() );
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
    protected void appendBOInfo ( XMLMessageGenerator resultGenerator, String targetNode, BusinessObjectInfo boInfo,  boolean addRequest )
    throws IMProcessingException
    {
        try
        {

            BusinessObjectLocal bo = boInfo.businessObject;
            XMLMessageParser boParser = boInfo.parser;
            
            XMLMessageGenerator temp = new XMLMessageGenerator("temp");
            
            

            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log ( Debug.MSG_STATUS, LOGGING_PREFIX + "Adding business object with metaDataName [" +
                            boParser.getValue ( ManagerConstants.BO_META_DATA_NAME ) + "], BOID [" +
                            boParser.getValue ( ManagerConstants.BO_BOID ) + "] to node [" + targetNode + "] ..." );

            //All non-blob data goes under the Info node.
            BusinessObjectMetaData boMetaData       = bo.getMetaData( );
            String                 xmlBlobFieldName = boMetaData.getXMLBlobFieldName();
            String                 infoNodePath     = targetNode + "." + MgrCoreConstants.INFO_NODE + ".";
            Node[]                 childNodes       = boParser.getChildNodes(".");

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, LOGGING_PREFIX + "XML-blob field name is [" + xmlBlobFieldName + "]." );

            // Generate all the necessary info nodes for this service, skipping the
            // "blob" node.

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, LOGGING_PREFIX + "Appending non-blob columns." );

            for (int i = 0; i < childNodes.length; i++)
            {
                String nodeName = childNodes[i].getNodeName();

                if (!nodeName.equals(xmlBlobFieldName) && XMLMessageBase.isValueNode(childNodes[i]))
                {
                    temp.setValue(infoNodePath + nodeName, boParser.getValue(nodeName));
                }
            }

            if (addRequest)
            {
                String xmlBlob = bo.get(xmlBlobFieldName);

                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                    Debug.log(Debug.MSG_STATUS, LOGGING_PREFIX + "Appending blob column." );

                XMLBlobAccess    xmlBlobAccess = new XMLBlobAccess(new XMLMessageParser(xmlBlob).getDocument());

                Node requestNode = ( new XMLMessageParser(xmlBlobAccess.getRequest()) ).getNode( "." );

                temp.setValue(targetNode + "." + requestNode.getNodeName(), requestNode);
            }
            
            //handle the bundle differently
            if ( targetNode.equals(".") )
            {
                resultGenerator.setValue(null, null, temp.getNode("Info") );
            }
            else
            {
                resultGenerator.setValue(null, null, temp.getNode(targetNode) );
            }
            
        }//try
        catch ( Exception e )
        {
            throw new IMProcessingException ( "Cannot append business object information to result XML, " + e.toString() );
        }
    }


    /**
     * Inner class to contain the BusinessObject as well as its XML representation to avoid
     * calls across the network to fetch the XML representation several times.
     */
    protected static class BusinessObjectInfo
    {
        public BusinessObjectInfo ( BusinessObjectLocal bo, XMLMessageParser parser )
        {
            this.businessObject = bo;
            this.parser = parser;
        }

        public BusinessObjectLocal businessObject = null;
        public XMLMessageParser parser = null;
    }



    /**
     * Logging prefix for diagnostic log messages and exception messages.
     */
    protected String LOGGING_PREFIX = this.getClass().getName() + ": ";

}//end of class GetServiceBundleDetailHandler


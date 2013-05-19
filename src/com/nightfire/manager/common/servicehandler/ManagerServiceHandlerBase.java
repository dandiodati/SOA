/**
 * Copyright (c) 2000 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: $
 */

package com.nightfire.manager.common.servicehandler;

import javax.ejb.*;
import java.util.*;

import org.w3c.dom.*;

import com.nightfire.mgrcore.common.*;
import com.nightfire.mgrcore.utils.*;
import com.nightfire.mgrcore.businessobject.*;
import com.nightfire.mgrcore.im.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;

import com.nightfire.manager.common.*;

/**
 * Service handler base class for all managers.
 */
public abstract class ManagerServiceHandlerBase extends ServiceHandlerBase
{

    /**
     * Create an XML representation of the service bundle BusinessObject so that it can be uniquely identified.
     * The fields in the XML are the BOID, MetaDataName, OrderID, ServiceBundleName.
     *
     * @param service bundle BusinessObject to represent in an XML.
     *
     * @return The XML representation of the service bundle BusinessObject.
     *
     * @exception  IMProcessingException  Thrown on errors.
     */
    public String convertBundleIDToXML ( BusinessObject bo ) throws IMProcessingException
    {
        try
        {
            //Get the data from the business object.
            XMLMessageParser contents = new XMLMessageParser ( bo.get () );

            //Create message to send back.
            XMLMessageGenerator gen = new XMLMessageGenerator ( MgrCoreConstants.BODY_NODE );
            
            //Set the BOID value.
            gen.setValue ( ManagerConstants.BOID_LOC, contents.getValue ( MgrCoreConstants.BOID_NODE ) );

            //Set the meta data name.
            gen.setValue ( ManagerConstants.META_DATA_NAME_LOC, contents.getValue ( MgrCoreConstants.META_DATA_NAME_NODE ) );

            //Set the OrderID.
            if ( contents.exists ( ManagerConstants.BO_SVC_BUNDLE_EXTERNAL_ID ) )
                gen.setValue ( MgrCoreConstants.INFO_NODE + "." + ManagerConstants.BO_SVC_BUNDLE_EXTERNAL_ID, contents.getValue ( ManagerConstants.BO_SVC_BUNDLE_EXTERNAL_ID) );

            //Set the bundle name.
            if ( contents.exists ( ManagerConstants.BO_SVC_BUNDLE_NAME ) )
                gen.setValue ( MgrCoreConstants.INFO_NODE + "." + ManagerConstants.BO_SVC_BUNDLE_NAME, contents.getValue ( ManagerConstants.BO_SVC_BUNDLE_NAME ) );

            return gen.generate ();
        }
        catch ( Exception e )
        {
            throw new IMProcessingException( e );
        }
    }

}//ManagerServiceHandlerBase
////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules.actions;

import org.w3c.dom.Document;

import com.nightfire.spi.neustar_soa.rules.AllowableActions;
import com.nightfire.spi.neustar_soa.rules.Context;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * This is the base class for actions that can be taken against a
 * subscription version.
 */
public abstract class SVAction extends Action {

   /**
    * This is the path where the SVID field should be set in the generated
    * request XML.
    */
   private String svidLocation;

   /**
    * This is the path where the region field should be set in the generated
    * request XML.
    */
   private String regionLocation;

   /**
    * This is the path where the telephone field should be set in the generated
    * request XML.
    */
   private String tnLocation;

   protected SVAction(String name,
                      String svidLocation,
                      String regionLocation,
                      String tnLocation){

      super( name );
	 
      this.svidLocation = svidLocation;
      
      this.regionLocation = regionLocation;
      
      this.tnLocation = tnLocation;

   }

   public Document getRequestDocument(String serviceType,
                                      Context context)
                                      throws MessageException{
	  
		// Get the status from GUI XML
      String status = context.getStatus(); 	  

	  if(	super.name.equals(SOAConstants.SV_MODIFY_REQUEST) 
		  || super.name.equals(SOAConstants.SV_MODIFY_REQUEST_ACTIVE) 
		  || super.name.equals(SOAConstants.SV_MODIFY_REQUEST_DISCONNECT_PENDING)
		  || super.name.equals(SOAConstants.SV_MODIFY_REQUEST_CANCEL_PENDING))
	  {
		  
		  if(status.equals(SOAConstants.ACTIVE_STATUS))
		  {

				super.name = SOAConstants.SV_MODIFY_REQUEST_ACTIVE;

		  }else if(status.equals(SOAConstants.DISCONNECT_PENDING_STATUS))
		  {
				
				super.name = SOAConstants.SV_MODIFY_REQUEST_DISCONNECT_PENDING;

		  }else if(status.equals(SOAConstants.CANCEL_PENDING_STATUS))
		  {

			  super.name = SOAConstants.SV_MODIFY_REQUEST_CANCEL_PENDING;
		  
		  }else
		  {
			  super.name = SOAConstants.SV_MODIFY_REQUEST;  
		  }
	  }

	  XMLMessageGenerator generator =
         new XMLMessageGenerator(SOAConstants.ROOT);

	  setInitSPID(serviceType, context, generator);
      setDateSent(context, generator);

      // if the SVID and region exist, then send the request
      // based on
      if( context.valueExists(Context.SVID_NODE) &&
          context.valueExists(Context.REGION_NODE) ){

         String svid = context.getSvId();
         String region = context.getRegionId();
         
         if( svid.equals("") && region.equals("") )
         {
			
			String tn = context.getTn() ;
			
			setTn(tn, context, generator);
			
         }else
         {
         
	         setSVID(svid, context, generator);
	         
	         setRegion(region, context, generator);
         }

      }
      else if( context.valueExists( Context.TN_NODE ) ){

         String tn = context.getTn() ;
         setTn(tn, context, generator);

      }
      else{

         throw new MessageException(Context.SVID_NODE+" and "+
                                    Context.REGION_NODE+" or "+
                                    Context.TN_NODE+" are required.");

      }

      return generator.getDocument();

   }

   /**
    * This sets the InitSPID value in the generated request. Based
    * on whether the service type is a port in or a port out,
    * the new or old service provider value is used to populate
    * the init SPID.
    *
    * @param serviceType String this is the GUI service type. For example,
    *                           SOAPortIn, SOAPortOut, etc.
    * @param context Context this contains all of
    *                        the GUI query results for the DB
    *                        record for which this action would be
    *                        performed.
    * @param message XMLMessageGenerator the XML request message where the
    *                                    init SPID will be set.
    * @throws MessageException if any error occurs setting or getting values
    *                          in the XML.
    */
   protected void setInitSPID(String serviceType,
                              Context context,
                              XMLMessageGenerator message)
                              throws MessageException{

      String initSpid;

      // check the service type to determine which provider would
      // be sending the request
      if( serviceType.equals(AllowableActions.PORTOUT_SERVICE_TYPE) ){

         // the init SPID is the old service provider
         initSpid = context.getOldSP();

      }
      else{

         // the init SPID is the new service provider
         initSpid = context.getNewSP();

      }

      message.setValue(INIT_SPID_LOCATION, initSpid);

   }


   /**
    *
    *
    * @param region String
    * @param context  Context
    * @param message XMLMessageGenerator
    */
   protected void setRegion(String region,
                            Context context,
                            XMLMessageGenerator message)
                            throws MessageException{

      message.setValue(getRegionLocation(), region);

   }

   /**
    *
    *
    * @param svid String
    * @param context Context
    * @param message XMLMessageGenerator
    */
   protected void setSVID(String svid,
                          Context context,
                          XMLMessageGenerator message)
                          throws MessageException{

      message.setValue( getSVIDLocation(), svid );

   }

   /**
    *
    *
    * @param tn String
    * @param context Context
    * @param message XMLMessageGenerator
    */
   protected void setTn(String tn,
                        Context context,
                        XMLMessageGenerator message)
                        throws MessageException{


      message.setValue( getTnLocation(), tn );

   }

   /**
    * Gets the path where the SVID should be set in the XML request.
    *
    * @return String
    */
   protected String getSVIDLocation(){

      return svidLocation;

   }


   /**
    * Gets the path where the region should be set in the XML request.
    *
    * @return String
    */
   protected String getRegionLocation(){

      return regionLocation;

   }

   /**
    * Gets the path where the TN should be set in the XML request.
    *
    * @return String
    */
   protected String getTnLocation(){

      return tnLocation;

   }

}

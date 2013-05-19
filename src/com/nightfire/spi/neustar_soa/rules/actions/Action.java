////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules.actions;

import java.text.*;
import java.util.Date;
import java.util.TimeZone;

import org.w3c.dom.Document;

import com.nightfire.spi.neustar_soa.rules.Context;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;

/**
 * This class represents a possible action that can be taken in the SOA GUI.
 * This class is also used to generate an XML version of this request type
 * which can then be used to determine if this request type is allowed.
 */
public abstract class Action {

   /**
    * The name of this request type.
    */
   protected String name;

   /**
    * This is the time zone to use when generated date values. If this is
    * null, then the default system time zone will be used.
    */
   protected TimeZone timeZone = null;

   /**
    * The path to the InitSPID field in a SOA XML request.
    */
   public static final String INIT_SPID_LOCATION =
      SOAConstants.REQUEST_HEADER_PATH+".InitSPID";

   /**
    * The path to the DateSent field in a SOA XML request.
    */
   public static final String DATE_SENT_LOCATION =
      SOAConstants.REQUEST_HEADER_PATH+".DateSent";

   /**
    * The contructor.
    *
    * @param name String the name of this action.
    */
   protected Action(String name){

      this.name = name;

   }

   /**
    * Returns the name of this action.
    *
    * @return String the name of this action/request.
    */
   public String getName(){

      return name;

   }

   /**
    * Sets the time zone that this action should use when formatting dates.
    *
    * @param zone TimeZone the new time zone.
    */
   public void setTimeZone(TimeZone zone){

      timeZone = zone;

   }

   /**
    * This creates an XML request of this action's type. This XML request
    * is then validated against business rules in order to determine
    * if this particular action is allowed.
    *
    * @param serviceType String this is the GUI service type. For example,
    *                           SOAPortIn, SOAPortOut, etc.
    * @param context Context this provides access to the
    *                        parsed XML document that contains all of
    *                        the GUI query results for the DB
    *                        record for which this action would be
    *                        performed.
    * @throws MessageException if any error occurs while trying to
    *                          generate the request or when getting values
    *                          from the given context.
    *
    * @return String the resulting XML request.
    */
   public String getRequest(String serviceType,
                            Context context)
                            throws MessageException{

      XMLMessageGenerator generator =
         new XMLMessageGenerator( getRequestDocument(serviceType, context ) );

      return generator.generate();

   }

   /**
    * This method is implemented by the subsclass in order to generate
    * the reqest XML for an action of this type.
    *
    * @param serviceType String this is the GUI service type. For example,
    *                           SOAPortIn, SOAPortOut, etc.
    * @param context Context this provides access to the
    *                        parsed XML document that contains all of
    *                        the GUI query results for the DB
    *                        record for which this action would be
    *                        performed.
    * @throws MessageException if any error occurs while trying to
    *                          generate the request or when getting values
    *                          from the given context.
    *
    * @return Document the resulting XML request.
    *
    */
   public abstract Document getRequestDocument(String serviceType,
                                               Context context)
                                               throws MessageException;

   /**
    * This sets the current time as the DateSent field for a generated XML
    * request.
    * If a time zone has been set, then the date is generated in that time
    * zone.
    *
    * @param context Context this provides access to the
    *                        parsed XML document that contains all of
    *                        the GUI query results for the DB
    *                        record for which this action would be
    *                        performed.
    * @param message XMLMessageGenerator the XML request message where the
    *                                    date sent will get set.
    *
    * @throws MessageException if an error occurs while setting the date into
    *                          the XML message.
    */
   protected void setDateSent(Context context,
                              XMLMessageGenerator message)
                              throws MessageException{


       SimpleDateFormat format = new SimpleDateFormat(SOAConstants.DATE_FORMAT);

       // if a time zone has been configured, then
       if( timeZone != null ){
          format.setTimeZone( timeZone );
       }

       String currentDate = format.format( new Date() );

       // set the current date as the date sent
       message.setValue(DATE_SENT_LOCATION, currentDate);

   }


   /**
    * This method is defined by subclasses in order to set the value of
    * the InitSPID field. The subsclass will need to extract and determine
    * the init SPID from the given context information.
    *
    * @param serviceType String
    * @param context Context
    * @param message XMLMessageGenerator
    * @throws MessageException
    */
   protected abstract void setInitSPID(String serviceType,
                                       Context context,
                                       XMLMessageGenerator message)
                                       throws MessageException;


   /**
    * Returns the name of this action. This is used for logging purposes.
    *
    * @return String the name of this action/request.
    */
   public String toString(){

      return getName();

   }

}

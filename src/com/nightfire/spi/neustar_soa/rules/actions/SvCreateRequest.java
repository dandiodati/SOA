////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules.actions;

import com.nightfire.spi.neustar_soa.rules.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class SvCreateRequest extends SVAction{

   public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                          SOAConstants.SV_CREATE_REQUEST+
                                          "."+SOAConstants.SUBSCRIPTION_NODE;

   public static final String TN_LOCATION = BASE_PATH+"."+
                                            SOAConstants.TN_NODE;

   public SvCreateRequest() {

      this(SOAConstants.SV_CREATE_REQUEST, TN_LOCATION);

   }

   protected SvCreateRequest(String name, String tnLocation) {

      super(name,
            null,
            null,
            tnLocation);

   }


   protected void setSVID(String svid,
                          Context context,
                          XMLMessageGenerator message)
                          throws MessageException{

      // SvCreate requests cannot be sent based on
      // SVID and region. Set the TN instead.
      if( context.valueExists( Context.TN_NODE ) ){

         String tn = context.getTn() ;
         setTn(tn, context, message);

      }
      else{

         throw new MessageException(Context.TN_NODE+" is required.");

      }

   }


   protected void setRegion(String region,
                            Context context,
                            XMLMessageGenerator message)
                            throws MessageException{

      // Do nothing. SvCreate requests cannot be sent based on
      // SVID and region.


   }

}

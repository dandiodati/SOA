////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules.actions;

import org.w3c.dom.Document;

import com.nightfire.spi.neustar_soa.rules.Context;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

public class SvQueryRequest extends SVAction{

   public SvQueryRequest() {

      super( SOAConstants.SV_QUERY_REQUEST,
             null,
             null,
             null );

   }

   /**
    * This method is overriden so that it does not bother to set the SVID
    * or TN in the request.
    *
    * @param serviceType String
    * @param context XMLMessageParser
    * @throws MessageException
    * @return Document
    */
   public Document getRequestDocument(String serviceType,
                                      Context context)
                                      throws MessageException{

      XMLMessageGenerator generator =
         new XMLMessageGenerator(SOAConstants.ROOT);

      setInitSPID(serviceType, context, generator);
      setDateSent(context, generator);

      return generator.getDocument();

   }

}

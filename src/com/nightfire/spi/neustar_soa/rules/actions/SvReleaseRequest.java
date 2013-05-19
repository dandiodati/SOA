////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules.actions;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class SvReleaseRequest extends SvCreateRequest{

   public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                          SOAConstants.SV_RELEASE_REQUEST+
                                          "."+SOAConstants.SUBSCRIPTION_NODE;

   public static final String TN_LOCATION = BASE_PATH+"."+
                                            SOAConstants.TN_NODE;

   public SvReleaseRequest(){

      super( SOAConstants.SV_RELEASE_REQUEST, TN_LOCATION );

   }

}

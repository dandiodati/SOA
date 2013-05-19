////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules.actions;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class SvRemoveFromConflictRequest extends SVAction{

   public static final String BASE_PATH = SOAConstants.REQUEST_BODY_PATH+"."+
                                          SOAConstants.SV_REMOVE_FROM_CONFLICT_REQUEST+
                                          "."+SOAConstants.SUBSCRIPTION_NODE;

   public static final String SVID_LOCATION = BASE_PATH+"."+
                                              SOAConstants.SVID_REGION_NODE+"."+
                                              SOAConstants.SVID_NODE;

   public static final String REGION_LOCATION = BASE_PATH+"."+
                                                SOAConstants.SVID_REGION_NODE+
                                                "."+SOAConstants.REGIONID_NODE;

   public static final String TN_LOCATION = BASE_PATH+"."+
                                            SOAConstants.TN_NODE;

   public SvRemoveFromConflictRequest(){

      super( SOAConstants.SV_REMOVE_FROM_CONFLICT_REQUEST,
             SVID_LOCATION,
             REGION_LOCATION,
             TN_LOCATION );

   }

}

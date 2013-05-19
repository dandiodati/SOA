///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2003 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.adapter;

import java.util.BitSet;

import com.nightfire.framework.util.Debug;

/**
* This class represents a secondary (AKA customer) SPID and the
* regions that that customer supports. 
*/
public class SecondarySPID{

   /**
   * The SPID. 
   */
   private String spid;

   /**
   * The array indicating whether a particular region supports this
   * SPID or not. 
   */
   private boolean[] supportedRegions =
                                new boolean[NPACConstants.REGION_COUNT];

   /**
   * Constructor.
   */
   public SecondarySPID(String secondarySPID){

      spid = secondarySPID;

   }

   /**
   * Gets the secondary SPID. 
   */
   public String getSPID(){

      return spid;

   }

   /**
   * This sets whether or not the region with given index supports
   * this service provider. By default, no regions are supported.
   *
   * @param region the code for the region.
   * @param supported whether or not this secondary SPID supports that region.
   */
   public void setRegionSupported(int region, boolean supported){

      try{

         supportedRegions[ region ] = supported;

      }
      catch(ArrayIndexOutOfBoundsException ex){

         Debug.error("Region index ["+region+
                     "] is not defined. This region cannot be supported for SPID ["+
                     getSPID()+"]");

      }

   }

   /**
   * Returns whether this SPID is supported in the given region. 
   */
   public boolean isRegionSupported(int region){

      try{

         return supportedRegions[region];

      }
      catch(ArrayIndexOutOfBoundsException ex){

         Debug.error("Region index ["+region+
                     "] is not defined. This region is not supported for SPID ["+
                     getSPID()+"]");

      }

      return false;

   }

   /**
   * Returns a string that lists the SPID and what regions it supports. 
   */
   public String describe(){

      StringBuffer buffer = new StringBuffer("Secondary SPID [");

      buffer.append(getSPID());
      buffer.append("]\n");

      for(int region = 0; region < NPACConstants.REGION_COUNT; region++){

         buffer.append("Region ");
         buffer.append(region);
         buffer.append(" supported: [");
         buffer.append( isRegionSupported(region) );
         buffer.append("]\n");

      }      

      return buffer.toString();

   }

   /**
   * Returns the SPID. 
   */
   public String toString(){

       return getSPID();

   }


}
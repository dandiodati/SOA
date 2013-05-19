/**
 * This class contains utility methods for accessing SOA data from the
 * database. These custom functions are intended to be used by SOA
 * business rules that need to check the current state of a record.
 *
 * @author Devaraj
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see import java.sql.Connection;
 * @see import java.sql.PreparedStatement;
 * @see import java.sql.ResultSet;
 * @see import java.util.ArrayList;
 * @see import com.nightfire.framework.db.DBInterface;
 * @see import com.nightfire.framework.rules.Value;
 * @see import com.nightfire.framework.util.Debug;
 * @see import com.nightfire.framework.util.StringUtils;
 * @see import com.nightfire.spi.neustar_soa.utils.SOAConstants;
 * @see import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Devaraj			04/22/2005		Created.
	
	2			Devaraj			05/16/2005		Removed static reference for 
												all methods.
	
	3			Peeyush M		06/22/2007		Added function isLrnExistsOkwithSubDomainId().
												

 */

package com.nightfire.spi.neustar_soa.rules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import com.nightfire.framework.rules.Value;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;


public abstract class SOACustomNetworkDBFunctions extends
 SOACustomSvDBFunctions{    
   
   /**
	* This method determines the NPA NXX is in "OK" status or not
	* @param npa
	* @param nxx
	* @return boolean whether or not the NPA NXX exists already
	*/
   public boolean isNpaNxxExists(Value npa, Value nxx){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();
		  
		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL to check NPA NXX existence in OK status :\n" + SOAQueryConstants.NPA_NXX_EXISTS_QUERY);
		  
		  pstmt = conn.prepareStatement(
		  SOAQueryConstants.NPA_NXX_EXISTS_QUERY);

		  pstmt.setString( 1, npa.toString() );
       
		  pstmt.setString( 2, nxx.toString() );
                    
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpaNxxExists value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.NPA_NXX_EXISTS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether NPA NXX exists for Creating SV
	* @param npa
	* @param nxx
	* @return boolean whether or not the NPA NXX exists already
	*/
   public boolean isNpaNxxExists(Value tn){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();
		  
		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL to check NPA NXX existence for creating SV :\n" + SOAQueryConstants.NPA_NXX_EXISTS_QUERY);
		  
		  pstmt = conn.prepareStatement(
		  SOAQueryConstants.NPA_NXX_EXISTS_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
				SOAConstants.TN_NPA_START_INDEX,
				SOAConstants.TN_NPA_END_INDEX));
       
		  pstmt.setString( 2, tn.toString().substring(
				SOAConstants.TN_NXX_START_INDEX,
				SOAConstants.TN_NXX_END_INDEX));
                    
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpaNxxExists value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.NPA_NXX_EXISTS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   
   
   /**
	* This method is used to check whether NPA NXX id exists for 
	* Deleting NPA NXX
	* @param npanxxid
	* @param regionid
	* @param spid
	* @return boolean whether or not the NPA NXX id exists for region and spid
	*/
   public boolean isNpaNxxIdExistsForDelete(
		   Value npanxxid, Value regionid, Value spid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();
		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL to check NPA NXX ID exist for deleting " +
			  		"NPA NXX request :\n" + SOAQueryConstants.NPA_NXX_ID_EXISTS_FOR_DELETE_QUERY);

		  pstmt = conn.prepareStatement(
				SOAQueryConstants.NPA_NXX_ID_EXISTS_FOR_DELETE_QUERY);

		  pstmt.setString( 1, npanxxid.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
          
		  pstmt.setString( 3, spid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpaNxxIdExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
					SOAQueryConstants.NPA_NXX_ID_EXISTS_FOR_DELETE_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether NPA NXX Value exists for 
	* Deleting NPA NXX
	* @param npa
	* @param nxx
	* @param spid
	* @param regionid
	* @return boolean whether or not the NPA NXX value exists for 
	* region and spid
	*/
   public boolean isNpaNxxValueExistsForDelete(
		   Value npa, Value nxx, Value spid, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();
		  
		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL to check NPA NXX existence fro delte request:\n" + SOAQueryConstants.NPA_NXX_VALUE_EXISTS_FOR_DELETE_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.NPA_NXX_VALUE_EXISTS_FOR_DELETE_QUERY);

		  pstmt.setString( 1, npa.toString() );
       
		  pstmt.setString( 2, nxx.toString() );
         
		  pstmt.setString( 3, spid.toString() );
          
		  pstmt.setString( 4, regionid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpaNxxValueExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

			logErrors(exception, 
					SOAQueryConstants.NPA_NXX_VALUE_EXISTS_FOR_DELETE_QUERY);

	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether the NBR POOL BLOCK modifying 
	* is in active status or not
	* @param npbid
	* @param regionid 
	* @return boolean whether or not the NPB is in Active status
	*/
   public boolean isNpbActive(Value npbid, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNpbActive :\n" + SOAQueryConstants.IS_NPB_ACTIVE_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.IS_NPB_ACTIVE_QUERY);

		  pstmt.setString( 1, npbid.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpbActive value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.IS_NPB_ACTIVE_QUERY);

	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether the NBR POOL BLOCK modifying 
	* is in active status or not
	* @param dashX
	* @param spid 
	* @return boolean whether or not the NPB is in Active status
	*/
   public boolean isNpbActiveWithNpaNxxX(Value dashX, Value spid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNpbActiveWithNpaNxxX :\n" + SOAQueryConstants.IS_NPB_ACTIVE_WITH_DASHX_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.IS_NPB_ACTIVE_WITH_DASHX_QUERY);

		  pstmt.setString( 1, dashX.toString().substring(
					SOAConstants.DASHX_NPA_START_INDEX,
					SOAConstants.DASHX_NPA_END_INDEX) );
          
		  pstmt.setString( 2, dashX.toString().substring(
					SOAConstants.DASHX_NXX_START_INDEX,
					SOAConstants.DASHX_NXX_END_INDEX) );
          
		  pstmt.setString( 3, dashX.toString().substring(
					SOAConstants.DASHX_START_INDEX,
					SOAConstants.DASHX_END_INDEX) );
       
		  pstmt.setString( 4, spid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpbActiveWithNpaNxxX value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.IS_NPB_ACTIVE_WITH_DASHX_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   
   /**
	* This method is used to check whether NBR POOL exists for creating NPB
	* @param npa
	* @param nxx 
	* @param dashx
	* @return boolean whether or not the NPB exists already
	*/
   public boolean isNpbExists(Value dashx){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNpbExists :\n" + SOAQueryConstants.NPB_EXISTS_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.NPB_EXISTS_QUERY);

		  pstmt.setString( 1, dashx.toString().substring(
					SOAConstants.DASHX_NPA_START_INDEX,
					SOAConstants.DASHX_NPA_END_INDEX) );
          
		  pstmt.setString( 2, dashx.toString().substring(
					SOAConstants.DASHX_NXX_START_INDEX,
					SOAConstants.DASHX_NXX_END_INDEX) );
          
		  pstmt.setString( 3, dashx.toString().substring(
					SOAConstants.DASHX_START_INDEX,
					SOAConstants.DASHX_END_INDEX) );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpbExists value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.NPB_EXISTS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check tn for SV creation
	* @param tn
	* @return boolean whether or not the NPB tn exists already
	*/
   public boolean isNpbDashXExistsSV(Value tn){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNpbDashXExistsSV :\n" + SOAQueryConstants.DASHX_EXISTS_FOR_SV_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.DASHX_EXISTS_FOR_SV_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
       
		  pstmt.setString( 2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX) );
          
		  pstmt.setString( 3, tn.toString().substring(
					SOAConstants.TN_DASHX_START_INDEX,
					SOAConstants.TN_DASHX_END_INDEX) );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpbDashXExistsSV value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.DASHX_EXISTS_FOR_SV_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   
   /**
	* This method is used to check DASH X for NPB creation
	* @param dashx
	* @param spid
	* @param regionid
	* @return boolean whether or not the NPB DASH X exists already
	*/
   public boolean isNpbDashXExists(
			Value dashx, Value spid, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNpbDashXExists :\n" + SOAQueryConstants.DASHX_EXISTS_FOR_REGION_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.DASHX_EXISTS_FOR_REGION_QUERY);

		  pstmt.setString( 1, dashx.toString().substring(
					SOAConstants.DASHX_NPA_START_INDEX,
					SOAConstants.DASHX_NPA_END_INDEX) );
          
		  pstmt.setString( 2, dashx.toString().substring(
					SOAConstants.DASHX_NXX_START_INDEX,
					SOAConstants.DASHX_NXX_END_INDEX) );
          
		  pstmt.setString( 3, dashx.toString().substring(
					SOAConstants.DASHX_START_INDEX,
					SOAConstants.DASHX_END_INDEX) );
          
		  pstmt.setString( 4, spid.toString() );
          
		  pstmt.setString( 5, regionid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpbDashXExists value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.DASHX_EXISTS_FOR_REGION_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check DASH X for SV creation
	* @param tn
	* @param spid
	* @return boolean whether or not the SV DASH X exists already
	*/
   public boolean isDashXExistsSV(Value tn, Value spid){
       
	   Connection conn = null;
      
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isDashXExistsSV :\n" + SOAQueryConstants.DASHX_EXISTS_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.DASHX_EXISTS_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
          
		  pstmt.setString( 2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX) );
             
		  pstmt.setString( 3, tn.toString().substring(
					SOAConstants.TN_DASHX_START_INDEX,
					SOAConstants.TN_DASHX_END_INDEX) );
          
		  pstmt.setString( 4, spid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isDashXExistsSV value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.DASHX_EXISTS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   
   
   /**
	* This method determines the NPA-NXX-X effective date is in fuure or past.
	* @param npa
	* @param nxx 
	* @param dashx
	* @return boolean whether or not the DASH X effective date valid
	*/
   public boolean isNpbDashXEffectiveDateValid(Value  dashx){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNpbDashXEffectiveDateValid :\n" + SOAQueryConstants.VALIDATE_DASHX_EFFECTIVE_DATE_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.VALIDATE_DASHX_EFFECTIVE_DATE_QUERY);

		  pstmt.setString( 1, dashx.toString().substring(
					SOAConstants.DASHX_NPA_START_INDEX,
					SOAConstants.DASHX_NPA_END_INDEX) );
          
		  pstmt.setString( 2, dashx.toString().substring(
					SOAConstants.DASHX_NXX_START_INDEX,
					SOAConstants.DASHX_NXX_END_INDEX) );
          
		  pstmt.setString( 3, dashx.toString().substring(
					SOAConstants.DASHX_START_INDEX,
					SOAConstants.DASHX_END_INDEX) );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpbDashXEffectiveDateValid value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
					SOAQueryConstants.VALIDATE_DASHX_EFFECTIVE_DATE_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }



   /**
	* This method determines the NPA-NXX-X effective date is before 
	* or after the given due date.
	* @param dashx
	* @param spid
	* @param effectiveDate
	* @return boolean whether or not the DASH X effective date valid
	*/
   public boolean isDashXEffectiveDateValid(
		   Value  tn, Value spid, Value effectiveDate){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isDashXEffectiveDateValid :\n" + SOAQueryConstants.VALIDATE_DASHX_EFFECTIVE_DATE_WITHS_SPID_QUERY);

		  pstmt = conn.prepareStatement(
			SOAQueryConstants.VALIDATE_DASHX_EFFECTIVE_DATE_WITHS_SPID_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
          
		  pstmt.setString( 2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX) );
             
		  pstmt.setString( 3, tn.toString().substring(
					SOAConstants.TN_DASHX_START_INDEX,
					SOAConstants.TN_DASHX_END_INDEX) );
          
		  pstmt.setString( 4, spid.toString() );
          
		  if (effectiveDate.toString().length() == 
				SOAConstants.DATE_HHMI_LENGTH){
              
			  pstmt.setString( 5, 
					  effectiveDate.toString().substring(
							SOAConstants.DATE_START_INDEX,
							SOAConstants.DATE_MIDLLE_INDEX )+
							SOAConstants.DATE_TIME_CONCATENATION + 
							effectiveDate.toString().substring(
									SOAConstants.DATE_MIDLLE_INDEX,
									SOAConstants.DATE_END_INDEX) );
          
		  } else{
              
			  pstmt.setString( 5, effectiveDate.toString() );
		  }
          
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isDashXEffectiveDateValid value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
			SOAQueryConstants.VALIDATE_DASHX_EFFECTIVE_DATE_WITHS_SPID_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }
   
   
   /**
	* This method is used to get New NPA
	* @param tn
	* @return boolean whether new NPA or not
	*/
   public boolean isNewNPA(Value tn){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNewNPA :\n" + SOAQueryConstants.GET_NEW_NPA_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.GET_NEW_NPA_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
          
		  pstmt.setString( 2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX) );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNewNPA value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.GET_NEW_NPA_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to get Old NPA
	* @param tn
	* @return boolean whether Old NPA or not
	*/
   public boolean isOldNPA(Value tn){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isOldNPA :\n" + SOAQueryConstants.GET_OLD_NPA_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.GET_OLD_NPA_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
          
		  pstmt.setString( 2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX) );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isOldNPA value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.GET_OLD_NPA_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check PDP Start date validation
	* @param tn
	* @return boolean PDP start date is valid or not
	*/
   public boolean isPDPStartDateValid(Value tn){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isPDPStartDateValid :\n" + SOAQueryConstants.VALIDATE_PDP_START_DATE_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.VALIDATE_PDP_START_DATE_QUERY);
          
		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isPDPStartDateValid value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.VALIDATE_PDP_START_DATE_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check PDP end date validation
	* @param tn
	* @return boolean PDP end date is valid or not
	*/
   public boolean isPDPEndDateValid(Value tn){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isPDPEndDateValid :\n" + SOAQueryConstants.VALIDATE_PDP_END_DATE_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.VALIDATE_PDP_END_DATE_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isPDPEndDateValid value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.VALIDATE_PDP_END_DATE_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }
   
   /**
	* This method is used to check NPA NXX Effetive Date
	* @param tn
	* @param effectiveDate
	* @return boolean NPA NXX Effetive Date is valid or not
	*/
   public boolean isNpaNxxEffectiveDateValid(
			Value tn, Value effectiveDate){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isNpaNxxEffectiveDateValid :\n" + SOAQueryConstants.VALIDATE_NPA_NXX_EFFECTIVE_DATE_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.VALIDATE_NPA_NXX_EFFECTIVE_DATE_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
       
		  pstmt.setString( 2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX) );
          
		  if (effectiveDate.toString().length() == 
					SOAConstants.DATE_HHMI_LENGTH){
              
			  pstmt.setString( 3, effectiveDate.toString().substring(
						SOAConstants.DATE_START_INDEX,
						SOAConstants.DATE_MIDLLE_INDEX)+
						SOAConstants.DATE_TIME_CONCATENATION + 
						effectiveDate.toString().substring(
								SOAConstants.DATE_MIDLLE_INDEX,
								SOAConstants.DATE_END_INDEX) );
          
		  } else{
              
			  pstmt.setString( 3, effectiveDate.toString() );
		  }
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isNpaNxxEffectiveDateValid value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
					SOAQueryConstants.VALIDATE_NPA_NXX_EFFECTIVE_DATE_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to validate NPA NXX against SPID
	* @param tn
	* @param spid
	* @return boolean 
	*/
   public boolean validateNpaNxxSpid(Value tn, Value spid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   StringBuffer errorMessage = new StringBuffer();
	   
	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, validateNpaNxxSpid :\n" + SOAQueryConstants.VALIDATE_NPA_NXX_WITH_SPID_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.VALIDATE_NPA_NXX_WITH_SPID_QUERY);

		  pstmt.setString( 1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX) );
          
		  pstmt.setString( 2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX) );
          
		  pstmt.setString( 3, spid.toString() );
                    
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if(!result){
			  addFailedTN(tn.toString());
			  errorMessage.append("["+tn.toString()+"] ");
			  setDynamicErrorMessage(errorMessage.toString());
		  }
		  
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"validateNpaNxxSpid value is: "+result);
         
	   }catch(Exception exception){
		   logErrors(exception, 
					SOAQueryConstants.VALIDATE_NPA_NXX_WITH_SPID_QUERY);
	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method queries a range of TNs and checks if all of the
	* NPA NXX's are valid.
	*
	* @param startTN Value
	* @param endStation Value
	* @param spid Value the SPID whose SVs will be queried.
	* @return boolean
	*/
   public boolean isDashXExistsSVRange(  Value startTN,
										 Value endStation,
										 Value spid){

	  int start;
      
	  int end;
      
	  try{
      
		start = Integer.parseInt(startTN.substring(
				SOAConstants.STARTTN_END_INDEX, 
				SOAConstants.TN_END_INDEX).toString());
          
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse start TN ["+startTN+"]: "+ex);
         
		  return false;
	  }

	  try{
        
		  end = Integer.parseInt( endStation.toString() );
          
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse end station ["+endStation+"]: "+ex);
         
		  return false;
	  }

	  String tnPrefix = startTN.toString().substring(
				SOAConstants.TN_START_INDEX, 
				SOAConstants.STARTTN_END_INDEX);
      
	  ArrayList nonMatches = new ArrayList();
      
	  boolean result = true;
      
	  String tn = "";
      
	  Value tnValue = null;
     
	  for(int suffix = start; suffix <= end; suffix++){

		 tn = tnPrefix + StringUtils.padNumber(suffix, 4, true, '0');

		 tnValue = new Value(tn);
         
		 //if NPA NXX X exists
		 if (isNpbDashXExistsSV(tnValue)){
             
			 // if NPA NXX X does not belong to the spid
			 if (!isDashXExistsSV(tnValue,spid)){
                 
				 nonMatches.add(tn);
			 }
                 
		 }
         
	  }   
  
	  if (!nonMatches.isEmpty()){          
          
		  result = false;
	  }
      
	  return result;

   }

   /**
	* This method queries a range of TNs and checks if all of the
	* NPA NXX X's effective dates are valid.
	*
	* @param startTN Value
	* @param endStation Value
	* @param spid Value the SPID whose SVs will be queried.
	* @param effectiveDate
	* @return boolean
	*/
   public boolean isDashXEffectiveDateValidRange(  Value startTN,
												   Value endStation,
												   Value spid,
												   Value effectiveDate){

	  int start;
    
	  int end;
      
	  try{
      
		start = Integer.parseInt(startTN.substring(
				SOAConstants.STARTTN_END_INDEX, 
				SOAConstants.TN_END_INDEX).toString());
      
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse start TN ["+startTN+"]: "+ex);
        
		  return false;
	  }

	  try{
      
		  end = Integer.parseInt( endStation.toString() );
      
	  }catch(NumberFormatException ex){
        
		  Debug.error("Could not parse end station ["+endStation+"]: "+ex);
         
		  return false;
	  }

	  String tnPrefix = startTN.toString().substring(
				SOAConstants.TN_START_INDEX, 
				SOAConstants.STARTTN_END_INDEX);
      
	  ArrayList nonMatches = new ArrayList();
      
	  boolean result = true;
      
	  String tn = "";
      
	  Value tnValue = null;

	  for(int suffix = start; suffix <= end; suffix++){

		 tn = tnPrefix + StringUtils.padNumber(suffix, 4, true, '0');
      
		 tnValue = new Value(tn);
         
		 //if NPA NXX X exists 
		 if (isNpbDashXExistsSV(tnValue)){
             
			 // if NPA NXX X  belong to the spid
			 if (isDashXExistsSV(tnValue,spid)){
                 
				 //if effective date is in past
				 if (!isDashXEffectiveDateValid(tnValue,spid,effectiveDate)){
                     
					 nonMatches.add(tn);
				 }
                 
			 }
                 
		 }
         
	  }
      
	  if (!nonMatches.isEmpty()){ 
          
		  result = false;
	  }
      
	  return result;

   }
   
   /**
	* This method queries a range of TNs and checks if all of the
	* NPA NXX's dates are valid.
	*
	* @param startTN Value
	* @param endStation Value
	* @return boolean
	*/
   public boolean isNpaNxxExistsRange(  Value startTN,
										Value endStation){

	  int start;
      
	  int end;
      
	  try{
      
		start = Integer.parseInt(startTN.substring(
				SOAConstants.STARTTN_END_INDEX, 
				SOAConstants.TN_END_INDEX).toString());
      
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse start TN ["+startTN+"]: "+ex);
         
		  return false;
	  }

	  try{
         
		  end = Integer.parseInt( endStation.toString() );
      
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse end station ["+endStation+"]: "+ex);
         
		  return false;
	  }

	  String tnPrefix = startTN.toString().substring(
				SOAConstants.TN_START_INDEX, 
				SOAConstants.STARTTN_END_INDEX);
      
	  ArrayList nonMatches = new ArrayList();
      
	  boolean result = true;
      
	  String tn = "";
      
	  Value tnValue = null;

	  for(int suffix = start; suffix <= end; suffix++){

		 tn = tnPrefix + StringUtils.padNumber(suffix, 4, true, '0');
      
		 tnValue = new Value(tn);
         
		 //if NPA NXX X does't exist
		 if (!isNpbDashXExistsSV(tnValue)){
             
			 // if NPA NXX does't exist
			 if (!isNpaNxxExists(tnValue)){
                 
					 nonMatches.add(tn);
              
			 }
                 
		 }
         
	  }   
  
	  if (!nonMatches.isEmpty()){   
          
		  result = false;
	  }
      
	  return result;

   }

   /**
	* This method queries a range of TNs and checks if all of the
	* NPA NXX's effective dates are valid.
	*
	* @param startTN Value
	* @param endStation Value
	* @param effectiveDate
	* @return boolean
	*/
   public boolean isNpaNxxEffectiveDateValidRange(  Value startTN,
													Value endStation,
													Value effectiveDate){

	  int start;
      
	  int end;
      
	  try{
      
		start = Integer.parseInt(startTN.substring(
				SOAConstants.STARTTN_END_INDEX, 
				SOAConstants.TN_END_INDEX).toString());
      
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse start TN ["+startTN+"]: "+ex);
         
		  return false;
	  }

	  try{
         
		  end = Integer.parseInt( endStation.toString() );
          
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse end station ["+endStation+"]: "+ex);
         
		  return false;
	  }

	  String tnPrefix = startTN.toString().substring(
				SOAConstants.TN_START_INDEX, 
				SOAConstants.STARTTN_END_INDEX);
      
	  ArrayList nonMatches = new ArrayList();
      
	  boolean result = true;
      
	  String tn = "";
      
	  Value tnValue = null;
      
	  for(int suffix = start; suffix <= end; suffix++){

		 tn = tnPrefix + StringUtils.padNumber(suffix, 4, true, '0');

		 tnValue = new Value(tn);
         
		 //if NPA NXX X does't exist
		 if (!isNpbDashXExistsSV(tnValue)){
             
			 // if NPA NXX exist
			 if (isNpaNxxExists(tnValue)){
                 
				 //if NPA NXX Effective date is in past
				 if (!isNpaNxxEffectiveDateValid(tnValue,effectiveDate)){
                 
					 nonMatches.add(tn);
				 }
              
			 }
                 
		 }
         
	  }
      
	  if (!nonMatches.isEmpty()){     
          
		  result = false;
	  }
  
	  return result;

   }
  
   /**
	* This method queries a range of TNs and checks if all of the
	* NPA NXX's belongs to the spid.
	*
	* @param startTN Value
	* @param endStation Value
	* @param spid
	* @return boolean
	*/
   public boolean validateNpaNxxSpidRange(  Value startTN,
											Value endStation,
											Value spid){

	  int start;
      
	  int end;
      
	  try{
      
		start = Integer.parseInt(startTN.substring(
				SOAConstants.STARTTN_END_INDEX, 
				SOAConstants.TN_END_INDEX).toString());
          
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse start TN ["+startTN+"]: "+ex);
         
		  return false;
	  }

	  try{
         
		  end = Integer.parseInt( endStation.toString() );
          
	  }catch(NumberFormatException ex){
       
		  Debug.error("Could not parse end station ["+endStation+"]: "+ex);
         
		  return false;
	  }

	  String tnPrefix = startTN.toString().substring(
					SOAConstants.TN_START_INDEX, 
					SOAConstants.STARTTN_END_INDEX);
      
	  ArrayList nonMatches = new ArrayList();
      
	  boolean result = true;
      
	  String tn = "";
      
	  Value tnValue = null;

	  for(int suffix = start; suffix <= end; suffix++){

		 tn = tnPrefix + StringUtils.padNumber(suffix, 4, true, '0');
      
		 tnValue = new Value(tn);
         
		 //if NPA NXX X does't exist
		 if (!isNpbDashXExistsSV(tnValue)){
             
			 // if NPA NXX exist
			 if (isNpaNxxExists(tnValue)){
                 
				 //if NPA NXX does't belong to the given spid
				 if (!validateNpaNxxSpid(tnValue,spid)){
                 
					 nonMatches.add(tn);
				 }
              
			 }
                 
		 }
         
	  }
      
	  if (!nonMatches.isEmpty()){     
          
		  result = false;
	  }
  
	  return result;

   }
   
   /**
	* This method is used to check whether LRN exists 
	* @param lrn
	* @param regionid
	* @param spid
	* @return boolean whether or not the LRN exists or not
	*/
   public boolean isLrnExists(Value lrn,
									 Value regionid,
									 Value spid){
     
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnExists :\n" + SOAQueryConstants.LRN_EXISTS_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.LRN_EXISTS_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
          
		  pstmt.setString( 3, spid.toString() );

		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnExists value is: "+result);
          
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.LRN_EXISTS_QUERY);

	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether LRN exists for the SPID
	* @param lrn
	* @param spid
	* @return boolean whether or not the LRN exists in that SPID or not
	*/
   public boolean isLrnExistswithSPID(Value lrn, Value spid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnExistswithSPID :\n" + SOAQueryConstants.LRN_EXISTS_FOR_SPID_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.LRN_EXISTS_FOR_SPID_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, spid.toString() );

		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnExistsWithSpid value is: "+result);
         
	   }catch(Exception exception){

			logErrors(exception, SOAQueryConstants.LRN_EXISTS_FOR_SPID_QUERY);

	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether LRN exists with OK 
	* status for the SPID
	* @param lrn
	* @param spid
	* @return boolean whether or not the LRN exists with Ok 
	* status in that SPID or not
	*/
   public boolean isNpaNxxLrn(Value lrn, Value spid){
       	         
	   boolean result = false;
	   
	   Connection conn = null;

	   Statement stmt = null;
      
       ResultSet results = null;

	   StringBuffer npaNxxValue = new StringBuffer();

	   StringBuffer errorMessage = new StringBuffer();

	   ArrayList passTnList = new ArrayList();

	   ArrayList nxxDbList = new ArrayList();

	   String failedTn=null;

	   TreeSet npaNnxList = new TreeSet();

	   ArrayList indTnList =new ArrayList();

	   String dbNpaNxx = null;

		if (existsValue("tnList"))
		{
			indTnList =(ArrayList)getContextValue("tnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"isNpaNxxLrn tnList"+indTnList);		
		}

	   if (existsValue("npaNnxList"))
		{
			npaNnxList = (TreeSet)getContextValue("npaNnxList");
		}		
				
	   int i=0;
		
	   Iterator npanxxIterator = npaNnxList.iterator();

	   while (npanxxIterator.hasNext())
	   {		
			npaNxxValue.append("'");

			npaNxxValue.append((String)npanxxIterator.next());
			
			if ( i != npaNnxList.size()-1)				
				npaNxxValue.append("',");
			else
				npaNxxValue.append("'");
			i++;
	   }
	   
	   String napnxxLrnQuery = "SELECT DISTINCT(NPANXX) FROM SOA_NPANXX_LRN WHERE NPANXX IN "
								+ "(select distinct(NPANXX) from SOA_NPANXX_LRN where NPANXX in ("
								+ npaNxxValue + ") and spid = '" + spid.toString()
								+ "') AND NPANXX not in (select NPANXX from SOA_NPANXX_LRN WHERE LRN = '"
								+ lrn.toString() + "' AND SPID = '" + spid.toString()
								+ "' And NPANXX in (" + npaNxxValue + ")) and spid = '"
								+ spid.toString() + "'";

	   try{
			 conn = getDBConnection();

			 stmt = conn.createStatement();

			 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
				 Debug.log(Debug.NORMAL_STATUS,"Executing napnxxLrnQuery"+napnxxLrnQuery);

			 results = stmt.executeQuery(napnxxLrnQuery);						 
			
			 while( results.next() ){

				dbNpaNxx = results.getString(1);

				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS,"Executing dbNpaNxx"+dbNpaNxx);

				nxxDbList.add(dbNpaNxx);				
				
			 }
			  			 			 
			 if (nxxDbList.isEmpty())
		     {
				result =true;
			 }
			 else
			 {
				result = false;

				String nnxx = null;
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Executing result"+result);

				for (int j = 0;j <indTnList.size();j++ )
				{
					failedTn = (String)indTnList.get(j);
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"failedTn result"+failedTn);
					
					nnxx = failedTn.substring(0,3)+failedTn.substring(4,7);

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"failedTn nnxx"+nnxx);

					if (nxxDbList.contains(nnxx))
					{
						addFailedTN(failedTn);
						errorMessage.append("["+failedTn+"] ");
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,"failedTn nnxx"+failedTn);
					}
					else
					{
						passTnList.add(failedTn);
					}

				}
				
				if (!passTnList.isEmpty())
					result =true;
				
				setDynamicErrorMessage(errorMessage.toString());
			 }		
		
			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 Debug.log(Debug.MSG_STATUS,"Result for isNpaNxxLrn : "+result);			 

		 }catch(Exception exception){

			 if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				 Debug.log(Debug.ALL_ERRORS,exception.toString());
         
			}finally{
			
			close(results, stmt);
		 }

		 return result;

	 }
   
   /**
    * This method is used to check whether LRN exists with OK 
    * status for the SPID
    * @param lrn
    * @param spid
    * @return boolean whether or not the LRN exists with Ok 
    * status in that SPID or not
    */
   public boolean isLrnExistsOkwithSPID(Value lrn, Value spid){
       
       Connection conn = null;
       
       PreparedStatement pstmt = null;
       
       ResultSet results = null;
       
       boolean result = false;

	   String tn = null;

	   ArrayList indTns = new ArrayList();

	   StringBuffer errorMessage = new StringBuffer();

	   if (existsValue("tnList"))
		{
			indTns =(ArrayList)getContextValue("tnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"isLrnExistsOkwithSPID tnList"+indTns);			
		}

       try{

          conn = getDBConnection();

          if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnExistsOkwithSPID :\n" + SOAQueryConstants.LRN_STATUS_OK_FOR_SPID_QUERY);

          pstmt = conn.prepareStatement(
          			SOAQueryConstants.LRN_STATUS_OK_FOR_SPID_QUERY);

          pstmt.setString( 1, lrn.toString() );
       
          pstmt.setString( 2, spid.toString() );

          results = pstmt.executeQuery();

          result = results.next();

		  if (!result)
			{
				for (int j = 0;j < indTns.size() ;j++ )
				{
					tn = (String)indTns.get(j);				
					addFailedTN(tn);
					errorMessage.append("["+tn+"] ");						
					
				}
				
				setDynamicErrorMessage(errorMessage.toString());
			}
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnExistsOkwithSPID value is: "+result);
         
       }catch(Exception exception){

   			logErrors(exception, 
   						SOAQueryConstants.LRN_STATUS_OK_FOR_SPID_QUERY);


       }finally{
           
          close(results, pstmt); 
       }
         
       return result;
   }

   /**
    * This method is used to check whether LRN exists in SOA_SUBDOMAIN_LRN table 
    * for that subdomain.
    * 
    * @param lrn, LRN from request.
    * @param spid, INIT Spid from request.
    * 
    * @return true if successful ,otherwise false.
    */
   public boolean isLrnExistsOkwithSubDomainId(Value lrn, Value spid)
   {
       Connection conn = null;
       
       PreparedStatement pstmt = null;
       
       ResultSet results = null;
       
       boolean result = true;

	   String tn = null;

	   ArrayList indTns = new ArrayList();

	   StringBuffer errorMessage = new StringBuffer();
	   
	   String customerId = null;
		
	   String subdomainId = null;
	   
	   boolean isSubDomainLevelUser = false;

	   if (existsValue("tnList"))
		{
			indTns =(ArrayList)getContextValue("tnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"isLrnExistsOkwithSubDomainId tnList"+indTns);
		}
			
		try
		{
	          conn = getDBConnection();
	          
	          //Get the CustomerId and SubdomainId from Context.	
	          customerId = CustomerContext.getInstance().getCustomerID();
	          subdomainId = CustomerContext.getInstance().getSubDomainId();
	          
	          isSubDomainLevelUser = isSubDomainLevelUser();
	          
	          if(isSubDomainLevelUser)
	          {
		          pstmt = conn.prepareStatement(
		          			SOAQueryConstants.LRN_STATUS_OK_FOR_APT_SUBDOMAIN_USER_QUERY);

	        	  pstmt.setString( 1 , customerId );
	        	  
	        	  pstmt.setString( 2 , subdomainId );
	        	  
	        	  pstmt.setString( 3 , lrn.toString() );
	        	  
	        	  if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
	        		  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnExistsOkwithSubDomainId.Query :\n "+SOAQueryConstants.LRN_STATUS_OK_FOR_APT_SUBDOMAIN_USER_QUERY);
	        	  
	        	  if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
	        		  Debug.log(Debug.MSG_DATA,"isLrnExistsOkwithSubDomainId.runtimevalues: customerId["+customerId+"] subomainId["+subdomainId+"] LRN["+lrn.toString()+"]" );
	        	  
		          results = pstmt.executeQuery();

		          result = results.next();

				  if (!result)
				  {
						for (int j = 0;j < indTns.size() ;j++ )
						{
							tn = (String)indTns.get(j);				
							addFailedTN(tn);
							errorMessage.append("["+tn+" ] ");						
							
						}
						
						setDynamicErrorMessage(errorMessage.toString());
				  }
	          }
	          
	          if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
	        	  Debug.log(Debug.MSG_STATUS,"isLrnExistsOkwithSubDomainId value is: "+result);
	         
	       }
		   catch(Exception exception)
		   {

	   			logErrors(exception, 
	   						SOAQueryConstants.LRN_STATUS_OK_FOR_APT_SUBDOMAIN_USER_QUERY);


	       }
		   finally
		   {
	          close(results, pstmt); 
	       }
	         
	       return result;			
   }
   /**
    * This method is used to check the status of the NANCFlag whether it returns value or not  
    *
    * @param spid Value the SPID 
    * @param tn Value
    * @return String Value.
    */   
   public String getNANC399(Value spid){
   	   
	   String nancflag = "";
	      try{
	         	         
	        StringBuffer errorMessage=new StringBuffer();
	        nancflag = String.valueOf(NANCSupportFlag.getInstance(spid.toString()).getNanc399flag());
	   	    if(nancflag == null || nancflag.equals(""))
	   	    nancflag="0";		
			
	   	    errorMessage.append("[" + spid + "]");
			setDynamicErrorMessage(errorMessage.toString());
			 
	      }catch(Exception exception){

	         logErrors(exception, exception.toString());
	         
	      }
	      
	      if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
	    	  Debug.log(Debug.MSG_STATUS,"NANC Flag Value is:"+ nancflag);
	      
	      return nancflag;
	      
	}
   
   /**
	* This method is used to check whether LRN exists for the Region Id
	* @param lrn
	* @param regionid
	* @return boolean whether or not the LRN exists in that region or not
	*/
   public boolean isLrnExistsWithRegion(Value lrn, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnExistsWithRegion :\n" + SOAQueryConstants.LRN_EXISTS_FOR_REGION_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.LRN_EXISTS_FOR_REGION_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnExistsWith Region value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.LRN_EXISTS_FOR_REGION_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether LRN ID exists for LRN Delete Request
	* @param lrn
	* @param regionid
	* @param spid
	* @return boolean whether or not the LRN ID exists
	*/
   public boolean isLrnIdExistsForDelete(
			Value lrn, Value regionid, Value spid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnIdExistsForDelete :\n" + SOAQueryConstants.LRN_ID_EXISTS_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.LRN_ID_EXISTS_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
          
		  pstmt.setString( 3, spid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnIdExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.LRN_ID_EXISTS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used get LRN value for corresponding LRN Id.
	* @param lrn
	* @param regionid
	* @param spid
	* @return String LRN value
	*/
   public String getLrnvalue(Value lrn, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   String lrnValue = "";

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, getLrnvalue :\n" + SOAQueryConstants.GET_LRN_VALUE_QUERY);

		  pstmt = conn.prepareStatement(SOAQueryConstants.GET_LRN_VALUE_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
          
		  results = pstmt.executeQuery();

		  if(results.next()){
              
			  lrnValue = results.getString(1);
		  }
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"getLrnvalue value is: "+lrnValue);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.GET_LRN_VALUE_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return lrnValue;
   }

   
   
   
   
   /**
	* This method is used to check whether LRN value exists for 
	* LRN Delete Request
	* @param lrn
	* @param regionid
	* @param spid
	* @return boolean whether or not the LRN Value exists
	*/
   public boolean isLrnValueExistsForDelete(
		   Value lrn, Value spid, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnValueExistsForDelete :\n" + SOAQueryConstants.LRN_VALUE_EXISTS_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.LRN_VALUE_EXISTS_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, spid.toString() );
          
		  pstmt.setString( 3, regionid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnValueExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.LRN_VALUE_EXISTS_QUERY);

	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether any number pool block which 
	* is using the deleted LRN
	* @param lrnid
	* @param regionid
	* @return boolean whether or not the NBR POOL BLOCK exists
	*/
   public boolean isLrnIdNbrPoolBlockExistsForDelete(
		   Value lrnid, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   String lrnValue = null;
       
	   boolean result = false;

	   try{
          
		  lrnValue = getLrnvalue(lrnid,regionid);
           
		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnIdNbrPoolBlockExistsForDelete :\n" + SOAQueryConstants.LRN_EXISTS_FOR_NBRPOOLBLOCK_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.LRN_EXISTS_FOR_NBRPOOLBLOCK_QUERY);

		  pstmt.setString( 1, lrnValue);
          
		  pstmt.setString( 2, regionid.toString() );
                    
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnIdNbrPoolBlockExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
					SOAQueryConstants.LRN_EXISTS_FOR_NBRPOOLBLOCK_QUERY);

	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   
   /**
	* This method is used to check whether any number pool block which 
	* is using the deleted LRN
	* @param lrn
	* @param regionid
	* @return boolean whether or not the NBR POOL BLOCK exists
	*/
   public boolean isLrnNbrPoolBlockExistsForDelete(
									Value lrn, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnNbrPoolBlockExistsForDelete :\n" + SOAQueryConstants.LRN_EXISTS_FOR_NBRPOOLBLOCK_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.LRN_EXISTS_FOR_NBRPOOLBLOCK_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
                    
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnNbrPoolBlockExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
					SOAQueryConstants.LRN_EXISTS_FOR_NBRPOOLBLOCK_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check whether any SV exists which is 
	* using the deleted LRN 
	* @param lrnid
	* @param regionid
	* @return boolean whether or not the SV ID exists
	*/
   public boolean isLrnIdSvExistsForDelete(Value lrnid, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   String lrnValue = "";
       
	   boolean result = false;

	   try{
           
		  lrnValue = getLrnvalue(lrnid,regionid);
           
		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnIdSvExistsForDelete :\n" + SOAQueryConstants.LRN_EXISTS_FOR_SV_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.LRN_EXISTS_FOR_SV_QUERY);

		  pstmt.setString( 1, lrnValue );
       
		  pstmt.setString( 2, regionid.toString() );
                    
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnIdSvExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.LRN_EXISTS_FOR_SV_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   
   /**
	* This method is used to check whether any SV exists which is 
	* using the deleted LRN 
	* @param lrn
	* @param regionid
	* @return boolean whether or not the SV ID exists
	*/
   public boolean isLrnSvExistsForDelete(Value lrn, Value regionid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isLrnSvExistsForDelete :\n" + SOAQueryConstants.LRN_EXISTS_FOR_SV_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.LRN_EXISTS_FOR_SV_QUERY);

		  pstmt.setString( 1, lrn.toString() );
       
		  pstmt.setString( 2, regionid.toString() );
                    
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isLrnSvExistsForDelete value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.LRN_EXISTS_FOR_SV_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }




   /**
	* This method is used to check Audit name exists
	* @param spid
	* @param name
	* @return boolean Audit name exists or not
	*/
   public boolean isAuditNameExists(Value spid, Value name){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isAuditNameExists :\n" + SOAQueryConstants.AUDIT_NAME_EXISTS_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.AUDIT_NAME_EXISTS_QUERY);

		  pstmt.setString( 1, spid.toString() );
       
		  pstmt.setString( 2, name.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isAuditNameExists value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.AUDIT_NAME_EXISTS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }

   /**
	* This method is used to check Audit name exists
	* @param spid
	* @param name
	* @return boolean Audit name exists or not
	*/
   public boolean isAuditNameExistsWithOK(Value spid, Value name){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isAuditNameExistsWithOK :\n" + SOAQueryConstants.AUDIT_NAME_EXISTS_WITH_OK_STATUS_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.AUDIT_NAME_EXISTS_WITH_OK_STATUS_QUERY);

		  pstmt.setString( 1, spid.toString() );
       
		  pstmt.setString( 2, name.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isAuditNameExistsWithOK value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
					SOAQueryConstants.AUDIT_NAME_EXISTS_WITH_OK_STATUS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }   
   
   /**
	* This method is used to check Audit Id exists
	* @param spid
	* @param auditid
	* @return boolean Audit Id exists or not
	*/
   public boolean isAuditIdExists(Value spid, Value auditid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isAuditIdExists :\n" + SOAQueryConstants.AUDIT_ID_EXISTS_QUERY);

		  pstmt = 
				conn.prepareStatement(SOAQueryConstants.AUDIT_ID_EXISTS_QUERY);

		  pstmt.setString( 1, spid.toString() );

		  pstmt.setString( 2, auditid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isAuditIdExists value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.AUDIT_ID_EXISTS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }
   
   /**
	* This method is used to check Audit name exists
	* @param spid
	* @param name
	* @return boolean Audit name exists or not
	*/
   public boolean isAuditNameExistsWithoutCreating(Value spid, Value name){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isAuditNameExistsWithoutCreating :\n" + SOAQueryConstants.AUDIT_NAME_EXISTS_WITHOUT_CREATING_STATUS_QUERY);

		  pstmt = conn.prepareStatement(
			SOAQueryConstants.AUDIT_NAME_EXISTS_WITHOUT_CREATING_STATUS_QUERY);

		  pstmt.setString( 1, spid.toString() );
       
		  pstmt.setString( 2, name.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isAuditNameExistsWithOK value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
			SOAQueryConstants.AUDIT_NAME_EXISTS_WITHOUT_CREATING_STATUS_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }   
   
   /**
	* This method is used to check Audit Id exists
	* @param spid
	* @param auditid
	* @return boolean Audit Id exists or not
	*/
   public boolean isAuditIdExistsWithoutCreating(
							Value spid, Value auditid){
       
	   Connection conn = null;
       
	   PreparedStatement pstmt = null;
       
	   ResultSet results = null;
       
	   boolean result = false;

	   try{

		  conn = getDBConnection();

		  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isAuditIdExistsWithoutCreating :\n" + SOAQueryConstants.AUDIT_ID_EXISTS_EXCEPT_CREATING_QUERY);

		  pstmt = conn.prepareStatement(
					SOAQueryConstants.AUDIT_ID_EXISTS_EXCEPT_CREATING_QUERY);

		  pstmt.setString( 1, spid.toString() );

		  pstmt.setString( 2, auditid.toString() );
          
		  results = pstmt.executeQuery();

		  result = results.next();
          
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"isAuditIdExists value is: "+result);
         
	   }catch(Exception exception){

		logErrors(exception, 
				SOAQueryConstants.AUDIT_ID_EXISTS_EXCEPT_CREATING_QUERY);


	   }finally{
           
		  close(results, pstmt);
	   }
         
	   return result;
   }
   
   /**
	* This method is used to determine given GTT ID existence
	* @param gttId
	* @return boolean 
	*/
   public boolean isGttIdExists(Value gttId){

	  boolean result = false;

	  Connection conn = null;
      
	  PreparedStatement pstmt = null;
 
	  ResultSet results = null;

	  try {

		 conn = getDBConnection();

		 if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isGttIdExists :\n" + SOAQueryConstants.GTT_ID_EXIST_QUERY);

		 pstmt = conn.prepareStatement(SOAQueryConstants.GTT_ID_EXIST_QUERY);

		 pstmt.setString( 1, gttId.toString() );
         
		 results = pstmt.executeQuery();

		 result = results.next();
         
		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"isGttIdExists: "+result);

	  }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.GTT_ID_EXIST_QUERY);

	  }finally{

		 close(results, pstmt);

	  }

	  return result;

   }
   
   /**
	* This method is used to determine the combination of LRN,GTTID and SPID
	* @param spid
	* @param gttid
	* @param lrn
	* @return boolean 
	*/
   public boolean isGttIdExistsInLrn(Value spid, Value gttid, Value lrn){

	  boolean result = false;

	  Connection conn = null;
      
	  PreparedStatement pstmt = null;
      
	  ResultSet results = null;

	  try{

		 conn = getDBConnection();

		 if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
			  Debug.log(Debug.NORMAL_STATUS, " SQL, isGttIdExistsInLrn :\n" + SOAQueryConstants.GTT_ID_WITH_LRN_EXIST_QUERY);

		 pstmt = conn.prepareStatement(
					SOAQueryConstants.GTT_ID_WITH_LRN_EXIST_QUERY);

		 pstmt.setString( 1, spid.toString() );
         
		 pstmt.setString( 2, gttid.toString() );
         
		 pstmt.setString( 3, lrn.toString() );
         
		 results = pstmt.executeQuery();

		 result = results.next();
         
		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"isGttIdExistsInLrn: "+result);

	  }catch(Exception exception){

		logErrors(exception, SOAQueryConstants.GTT_ID_WITH_LRN_EXIST_QUERY);

	  }finally{

		 close(results, pstmt);

	  }

	  return result;

   }

   /**
	* This method is used to check tn for NPA,NXX, X eixstance.
	* @return boolean whether or not the NPA,NXX, X  exists already
	*/
   public boolean isNpbDashXExistsSV(){              
       
		boolean result = false;

		String dashX=null;

		String npanxxSpid=null;
					
		ArrayList dashxList = new ArrayList();
		
		ArrayList failedDashXList = new ArrayList();
		
		ArrayList npanxxXDataList = null;

		TreeSet npaNxxXList = null;
			
		if (existsValue("npanxxXDataList"))
		{
			npanxxXDataList = (ArrayList)getContextValue("npanxxXDataList");
			if(npanxxXDataList != null)
			{
				for (int i = 0; i < npanxxXDataList.size(); i++) 
					{
					NpaNxxXData nnData =(NpaNxxXData)npanxxXDataList.get(i);
	
					dashxList.add(nnData.npanxxdashx);					
				}
			}
		}

		if (existsValue("npaNxxXList"))
		{
			npaNxxXList = (TreeSet)getContextValue("npaNxxXList");
			
			if(npaNxxXList != null)
			{
				String npanxx = null;

				Iterator tnIterator = npaNxxXList.iterator();

				while (tnIterator.hasNext())			
					{				
					dashX = (String)tnIterator.next();
	
					if (!dashxList.contains(dashX))
					{
						npanxx = dashX.substring(0, dashX.length()-1);
	
						if (!dashxList.contains(npanxx))
							failedDashXList.add(dashX);	
					}									
				}
			}
		}

		result = failedDashXList.isEmpty();			
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isNpbDashXExistsSV from context: "+result);

		return result;
   }

   /**
	* This method is used to validate NPA NXX against SPID
	* @param spid
	* @return boolean 
	*/
   public boolean validateNpaNxxSpid(Value spid){             
       
		boolean result = false;

		String dashX=null;

		String npanxxSpid=null;

		String failedTn=null;

		String nSpid = null;		
					
		ArrayList dashxList = new ArrayList();
		
		TreeSet failedDashXList = new TreeSet();

		TreeSet failedTnList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;

		ArrayList npanxxDbList = new ArrayList();

		ArrayList nnDbList = new ArrayList();

		ArrayList npanxxDataList = null;

		ArrayList npanxxXDataList = null;

		ArrayList failedNpaNxxXList = null;
		
		StringBuffer errorMessage = new StringBuffer();

		if (spid != null)
		{
			nSpid = spid.toString();
		}

		if (existsValue("npanxxDataList"))
		{
			npanxxDataList = (ArrayList)getContextValue("npanxxDataList");
			
			if(npanxxDataList != null)
			{
				for (int i = 0; i < npanxxDataList.size(); i++) 
					{
					NpaNxxData nnData =(NpaNxxData)npanxxDataList.get(i);
	
					npanxxSpid = nnData.spid;
	
					nnDbList.add(nnData.npanxx);
	
					if (npanxxSpid.equals(nSpid))
						npanxxDbList.add(nnData.npanxx);	
				}
			}
		}
			
		if (existsValue("npanxxXDataList"))
		{
			npanxxXDataList = (ArrayList)getContextValue("npanxxXDataList");
			
			if(npanxxXDataList != null)
			{
				for (int i = 0; i < npanxxXDataList.size(); i++) 
					{
					NpaNxxXData nnData =(NpaNxxXData)npanxxXDataList.get(i);
	
					dashxList.add(nnData.npanxxdashx);					
				}
			}
		}

		if (existsValue("npaNxxXList"))
		{
			npaNxxXList = (TreeSet)getContextValue("npaNxxXList");
			
			if(npaNxxXList != null)
			{
				String npanxx = null;

				Iterator tnIterator = npaNxxXList.iterator();

				while (tnIterator.hasNext())			
					{				
					dashX = (String)tnIterator.next();
	
					if (!dashxList.contains(dashX))
					{
						npanxx = dashX.substring(0, dashX.length()-1);
						
						if (nSpid == null)
						{
							if (!nnDbList.contains(npanxx)){
								failedDashXList.add(dashX);							
							}
						}
						else
						{
							if (!npanxxDbList.contains(npanxx)){
								failedDashXList.add(dashX);							
							}
						}
					}									
				}
			}
		}
	
		result = failedDashXList.isEmpty();

		if (!result)
		{
			npaNxxXDashx = (HashMap)getContextValue("npaNxxXDashx");
			
			if(npaNxxXDashx != null)
			{
				String npaNxxX = null;

				Iterator failedDashXIterator = failedDashXList.iterator();
				while (failedDashXIterator.hasNext())
					{
					npaNxxX = ((String)failedDashXIterator.next());
					
	
					failedNpaNxxXList = (ArrayList)npaNxxXDashx.get(npaNxxX);				
	
					failedTnList.addAll(failedNpaNxxXList);				
	
				}	
			}

			Iterator failedTnIterator = failedTnList.iterator();

			while (failedTnIterator.hasNext())
			{
				failedTn = ((String)failedTnIterator.next());
				
				addFailedTN(failedTn);
				errorMessage.append("["+failedTn+"] ");

			}
			
			setDynamicErrorMessage(errorMessage.toString());
		}		
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for validateNpaNxxSpid : "+result);

		return result;

   }

   /**
	* This method is used to check DASH X for SV creation
	* @param spid
	* @return boolean whether or not the SV DASH X exists already
	*/
   public boolean isDashXExistsSV(Value spid){
	   
		boolean result = false;

		String dashX=null;

		String npanxx=null;

		String npanxxXSpid=null;

		String failedTn=null;

		String dSpid = spid.toString();
					
		ArrayList dashxList = new ArrayList();

		ArrayList dashxSpidList = new ArrayList();
		
		ArrayList failedDashXList = new ArrayList();

		ArrayList npanxxXDataList = null;

		ArrayList failedNpaNxxXList = null;

		TreeSet failedTnList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;
		
		StringBuffer errorMessage = new StringBuffer();		
			
		if (existsValue("npanxxXDataList"))
		{
			npanxxXDataList = (ArrayList)getContextValue("npanxxXDataList");
			
			if(npanxxXDataList != null)
			{
				for (int i = 0; i < npanxxXDataList.size(); i++)
				{
					NpaNxxXData nnData =(NpaNxxXData)npanxxXDataList.get(i);
	
					dashxList.add(nnData.npanxxdashx);
	
					npanxxXSpid = nnData.spid;
	
					if (npanxxXSpid.equals(dSpid))
					{
						dashxSpidList.add(nnData.npanxxdashx);
	
					}									
				}
			}
		}

		if (existsValue("npaNxxXList"))
		{
			npaNxxXList = (TreeSet)getContextValue("npaNxxXList");
			
			if(npaNxxXList != null)
			{
				Iterator tnIterator = npaNxxXList.iterator();

				while (tnIterator.hasNext())			
					{				
					dashX = (String)tnIterator.next();
	
					if (dashxList.contains(dashX))
					{
						if (!dashxSpidList.contains(dashX))
						{
							failedDashXList.add(dashX);							
						}
					}													
				}
			}
		}

		result = failedDashXList.isEmpty();

		if (!result)
		{
			npaNxxXDashx = (HashMap)getContextValue("npaNxxXDashx");
			
			if(npaNxxXDashx != null)
			{
				for (int i = 0; i < failedDashXList.size(); i++) 
					{
					failedNpaNxxXList = (ArrayList)npaNxxXDashx.get(failedDashXList.get(i));
	
					failedTnList.addAll(failedNpaNxxXList);
										
				}
			}

			Iterator failedTnIterator = failedTnList.iterator();

			while (failedTnIterator.hasNext())
			{
				failedTn = ((String)failedTnIterator.next());
				addFailedTN(failedTn);
				errorMessage.append("["+failedTn+"] ");

			}			

			setDynamicErrorMessage(errorMessage.toString());
		}		
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isNpaNxxExists:"+result);

		return result;

   }

   /**
	* This method is used to check NPA NXX Effetive Date
	* @param effectiveDate
	* @return boolean NPA NXX Effetive Date is valid or not
	*/
   public boolean isNpaNxxEffectiveDateValid(Value effectiveDate){
       
		boolean result = false;

		String dashX=null;

		Date effDate = null;

		TreeSet failedTnList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;
					
		ArrayList dashxList = new ArrayList();
		
		ArrayList failedDashXList = new ArrayList();		

		ArrayList npanxxDbList = new ArrayList();

		ArrayList npaNxxList = new ArrayList();

		ArrayList npanxxDataList = null;

		ArrayList npanxxXDataList = null;

		ArrayList failedNpaNxxXList = null;
		
		StringBuffer errorMessage = new StringBuffer();

		SimpleDateFormat dateFormat =
		 new SimpleDateFormat( "MM-dd-yyyy-hhmmssa" );

		try {				
				effDate =dateFormat.parse(effectiveDate.toString());			
			} 
		catch (ParseException ex) {				

			Debug.error("Could not parse dueDate ["+effectiveDate+"]: "+ex);

			return false;
			}
			
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isNpaNxxEffectiveDateValid 1: "+effDate);

		long eDateTime = effDate.getTime();

		if (existsValue("npanxxDataList"))
		{
			npanxxDataList = (ArrayList)getContextValue("npanxxDataList");
			
			if(npanxxDataList != null)
			{
				long dbDate=0;

				for (int i = 0; i < npanxxDataList.size(); i++) 
					{
					NpaNxxData nnData =(NpaNxxData)npanxxDataList.get(i);
	
					npaNxxList.add(nnData.npanxx);
					dbDate = nnData.effectiveDate;
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						
						Debug.log(Debug.MSG_STATUS,"long  contextDateTime : "+dbDate);
						Debug.log(Debug.MSG_STATUS,"passing DateTime:"+eDateTime);
					}
										
					if (dbDate <= eDateTime )
					{
						npanxxDbList.add(nnData.npanxx);
						
					}
										
				}
			}
		}
			
		if (existsValue("npanxxXDataList"))
		{
			npanxxXDataList = (ArrayList)getContextValue("npanxxXDataList");
			
			if(npanxxXDataList != null)
			{
				for (int i = 0; i < npanxxXDataList.size(); i++) 
					{
					NpaNxxXData nnData =(NpaNxxXData)npanxxXDataList.get(i);
	
					dashxList.add(nnData.npanxxdashx);					
				}
			}
		}

		if (existsValue("npaNxxXList"))
		{
			npaNxxXList = (TreeSet)getContextValue("npaNxxXList");
			
			if(npaNxxXList != null)
			{
				String npanxx = null;

				Iterator tnIterator = npaNxxXList.iterator();

				while (tnIterator.hasNext())			
					{				
					dashX = (String)tnIterator.next();
	
					if (!dashxList.contains(dashX))
					{
						npanxx = dashX.substring(0, dashX.length()-1);
	
						if (npaNxxList.contains(npanxx))
						{
							if (!npanxxDbList.contains(npanxx))
								failedDashXList.add(dashX);	
						}					
					}
				}
			}
		}

		result = failedDashXList.isEmpty();

		if (!result)
		{
			npaNxxXDashx = (HashMap)getContextValue("npaNxxXDashx");
			
			if(npaNxxXDashx != null)
			{
				for (int i = 0; i < failedDashXList.size(); i++) 
					{
					failedNpaNxxXList = (ArrayList)npaNxxXDashx.get(failedDashXList.get(i));
	
					failedTnList.addAll(failedNpaNxxXList);
										
				}
			}

			Iterator failedTnIterator = failedTnList.iterator();
			String failedTn = null;
			while (failedTnIterator.hasNext())
			{
				failedTn = ((String)failedTnIterator.next());
				addFailedTN(failedTn);
				errorMessage.append("["+failedTn+"] ");

			}

			setDynamicErrorMessage(errorMessage.toString());
		}		
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"validateNpaNxxSpid from context: "+result);

		return result;						
   }

   /**
	* This method determines the NPA-NXX-X effective date is before 
	* or after the given due date.
	* @param effectiveDate
	* @return boolean whether or not the DASH X effective date valid
	*/
   public boolean isDashXEffectiveDateValid(Value effectiveDate, Value spid){
       
		boolean result = false;

		String dashX=null;

		String npanxx=null;

		long npanxxXDate=0;
					
		ArrayList dashxList = new ArrayList();
		
		ArrayList failedDashXList = new ArrayList();

		TreeSet failedTnList = new TreeSet();

		TreeSet npaNxxXList = null;
		
		StringBuffer errorMessage = new StringBuffer();

		ArrayList npaNxxDashxList = new ArrayList();

		ArrayList dashxSpidList = new ArrayList();

		ArrayList npanxxXDataList = null;

		ArrayList failedNpaNxxXList = null;

		HashMap npaNxxXDashx = null;

		String dSpid = spid.toString();

		String npanxxXSpid = null;
		
		Date effDate = null;

		SimpleDateFormat dateFormat =
		 new SimpleDateFormat( "MM-dd-yyyy-hhmmssa" );

		try {				
				effDate =dateFormat.parse(effectiveDate.toString());			
			} 
		catch (ParseException ex) {				

			Debug.error("Could not parse dueDate ["+effectiveDate+"]: "+ex);

			return false;
			}
			
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isDashXEffectiveDateValid 1: "+effDate);

		long eDateTime = effDate.getTime();
			
		if (existsValue("npanxxXDataList"))
		{
			npanxxXDataList = (ArrayList)getContextValue("npanxxXDataList");
			
			if(npanxxXDataList != null)
			{
				for (int i = 0; i < npanxxXDataList.size(); i++) 
					{
					NpaNxxXData nnData =(NpaNxxXData)npanxxXDataList.get(i);
	
					npanxxXDate = nnData.effectiveDate;
	
					npaNxxDashxList.add(nnData.npanxxdashx);
	
					npanxxXSpid = nnData.spid;
	
					if (npanxxXSpid.equals(dSpid))
					{
						dashxSpidList.add(nnData.npanxxdashx);
	
					}		
	
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
						
						Debug.log(Debug.MSG_STATUS,"contextDateTime : "+npanxxXDate);
						Debug.log(Debug.MSG_STATUS,"passing DateTime : "+eDateTime);
					}
													
					if (npanxxXDate < eDateTime )
					{
						dashxList.add(nnData.npanxxdashx);
						
					}											
				}
			}
		}

		if (existsValue("npaNxxXList"))
		{
			npaNxxXList = (TreeSet)getContextValue("npaNxxXList");
			
			if(npaNxxXList != null){
				Iterator tnIterator = npaNxxXList.iterator();

				while (tnIterator.hasNext())			
				{				
					dashX = (String)tnIterator.next();
				
					if (npaNxxDashxList.contains(dashX))
					{
						if (dashxSpidList.contains(dashX))
						{
							if (!dashxList.contains(dashX))
							{
								failedDashXList.add(dashX);							
							}	
						}											
					}												
				}
			}
		}

		result = failedDashXList.isEmpty();

		if (!result)
		{
			npaNxxXDashx = (HashMap)getContextValue("npaNxxXDashx");
			
			if(npaNxxXDashx != null)
			{
				for (int i = 0; i < failedDashXList.size(); i++) 
					{
					failedNpaNxxXList = (ArrayList)npaNxxXDashx.get(failedDashXList.get(i));
	
					failedTnList.addAll(failedNpaNxxXList);
										
				}
			}

			Iterator failedTnIterator = failedTnList.iterator();
			String failedTn = null;
			while (failedTnIterator.hasNext())
			{
				failedTn = ((String)failedTnIterator.next());
				addFailedTN(failedTn);
				errorMessage.append("["+failedTn+" ] ");

			}

			setDynamicErrorMessage(errorMessage.toString());

		}		
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isNpaNxxExists from context: "+result);

		return result;
		
   } 

   /**
	* This method is used to check NPA NXX existance.
	* @return boolean whether or not the NPA NXX  exists already
	*/
   public boolean isNpaNxxExistsSV(){              
       
		boolean result = false;
					
		ArrayList failedTNList = new ArrayList();
		
		StringBuffer errorMessage = new StringBuffer();

		// Check if query has been already executed earlier and result 
		//is available in context.
		Boolean npanxxExist=(Boolean)getContextValue("npaNxxPresent");

		if(npanxxExist!=null)
		{
			 result=npanxxExist.booleanValue();
			 
			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 Debug.log(Debug.MSG_STATUS,"Result for npanxxExist:"+result);
		}
			
		if (!result)
		{
			failedTNList = (ArrayList)getContextValue("fSpidTNList");

			if(failedTNList != null)
			{
				String failedTn = null;
			
				for (int i = 0; i < failedTNList.size(); i++) 
					{
					failedTn = (String)failedTNList.get(i);
	
					addFailedTN(failedTn);
					errorMessage.append("["+failedTn+"] ");
				}
			}

			setDynamicErrorMessage(errorMessage.toString());		
		}		
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isNpaNxxExistsSV from context: "+result);

		return result;
   }

   /**
	* This method is used to check NPA NXX Effetive Date
	* @return boolean NPA NXX Effetive Date is valid or not
	*/
   public boolean npaNxxEffectiveDateValid(){
       
		boolean result = false;

		ArrayList failedTNList = new ArrayList();
		
		StringBuffer errorMessage = new StringBuffer();

		// Check if query has been already executed earlier and result
		// is available in context.
		Boolean npanxxDatePresent=(Boolean)getContextValue("npanxxDatePresent");

		if(npanxxDatePresent!=null)
		{
			 result=npanxxDatePresent.booleanValue();
			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 Debug.log(Debug.MSG_STATUS,"npanxxExist from context: "+result);
		}
			
		if (!result)
		{
			failedTNList = (ArrayList)getContextValue("fduedateTNList");
			if(failedTNList != null)
			{
				String failedTn = null;

				for (int i = 0; i < failedTNList.size(); i++) 
					{
					failedTn = (String)failedTNList.get(i);
	
					addFailedTN(failedTn);
					errorMessage.append("["+failedTn+"] ");
				}
			}

			setDynamicErrorMessage(errorMessage.toString());
		}				
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isNpaNxxExistsSV from context: "+result);

		return result;
   }
   
   /**
	 * This method is used to check NPA NXX belongs to Canada region or not.
	 * 
	 * 
	 * @return boolean, result
	 */

	public boolean isNpaNxxBelongsToCanadaNPBActivate(Value  npanxxX) {

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		boolean result = false;

		try {

			conn = getDBConnection();

			if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
				Debug
						.log(
								Debug.NORMAL_STATUS,
								" SQL, isNpaNxxBelongsToCanadaNPBActivate:\n"
										+ SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);
			}
			pstmt = conn
					.prepareStatement(SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);

			pstmt.setString( 1, npanxxX.toString().substring(
					SOAConstants.DASHX_NPA_START_INDEX,
					SOAConstants.DASHX_NPA_END_INDEX) );
          
		  pstmt.setString( 2, npanxxX.toString().substring(
					SOAConstants.DASHX_NXX_START_INDEX,
					SOAConstants.DASHX_NXX_END_INDEX) );

			results = pstmt.executeQuery();

			result = results.next();

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"isNpaNxxBelongsToCanadaNPBActivate, record present : " + result);
			}

		} catch (Exception exception) {

			logErrors(exception,
					SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);

		} finally {

			close(results, pstmt);
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongsToCanadaNPBActivate, Result : " + result);
		}
		return result;
	}
	
	/**
	 * This method is used to check NPB belongs to Canada region or not.
	 * 
	 * 
	 * @return boolean, result
	 */

	public boolean isNpaNxxBelongsToCanadaNPBModify(Value blockId) {

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		boolean result = false;
		try {

			conn = getDBConnection();
			
			if(present(SOAConstants.CONTEXT_ROOT_NODE_NPB_ID)){
				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
					Debug.log(
						Debug.NORMAL_STATUS, " SQL, isNpaNxxBelongsToCanadaNPBModify:\n"
										+ SOAQueryConstants.VALIDATE_NPB_BELONG_TO_CANADA_QUERY);
				}
				pstmt = conn
					.prepareStatement(SOAQueryConstants.VALIDATE_NPB_BELONG_TO_CANADA_QUERY);

				pstmt.setString( 1, blockId.toString());
          
				results = pstmt.executeQuery();

				result = results.next();
			
				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS,
							"blockId, record present : " + result);
				}
			}

		} catch (Exception exception) {

			logErrors(exception,
					SOAQueryConstants.VALIDATE_NPB_BELONG_TO_CANADA_QUERY);

		} finally {

			close(results, pstmt);
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongsToCanadaNPBModify, Result : " + result);
		}
		return result;
	}
	/**
	 * Checks the absence of pseudo LRN value
	 * 
	 * @param lrn
	 * @return boolean result
	 */
	public boolean pseudoLrnAbsentNPB(Value lrn) {

		boolean result = true;
		
		if(lrn != null){
			if (lrn.toString().indexOf("0000000000") != -1) {
				result = false;
			}
		}
		
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"pseudoLrnAbsentNPB, Result : " + result);
		}
		
		return result;
	}
	
	/**
	 * This method is used to check whether any SV exist without pseudo LRN or not
	 */
	public boolean isPseudoLrnAllowedNpb(Value dashx){
		
		boolean result = true;
		
		boolean rsValue = false;
		
		Connection con = null;

		PreparedStatement pstmt = null;

		ResultSet rs = null;
		
		try{
			con = getDBConnection();
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "SQL, isPseudoLrnAllowedNpb : \n"
						+ SOAQueryConstants.GET_SVEXISTS_WITH_ACTIVELRN);
			
			pstmt = con.prepareStatement(SOAQueryConstants.GET_SVEXISTS_WITH_ACTIVELRN);
			
			pstmt.setString(1, 
					dashx.toString().substring(0,3) + "-" + 
					dashx.toString().substring(3,6) + "-" +
					dashx.toString().substring(6) + "%");
			
			rs = pstmt.executeQuery();
			
			rsValue = rs.next();
			
			if(rsValue){
				result = false;
			}
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "isPseudoLrnAllowedNpb value is: "
						+ result);
			
		}
		catch (Exception exp) {
			logErrors(exp, SOAQueryConstants.GET_SVEXISTS_WITH_ACTIVELRN);
		}finally{
			close(rs, pstmt);
		}
		return result;
	}

	public boolean isLrnAllowedNpb (Value dashx){
		
		boolean result = true;
		
		boolean rsValue = false;
		
		Connection con = null;

		PreparedStatement pstmt = null;

		ResultSet rs = null;
		
		try{
			con = getDBConnection();
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "SQL, isLrnAllowedNpb : \n"
						+ SOAQueryConstants.GET_SVEXISTS_WITH_PSEUDOLRN);
			
			pstmt = con.prepareStatement(SOAQueryConstants.GET_SVEXISTS_WITH_PSEUDOLRN);
			
			pstmt.setString(1, 
					dashx.toString().substring(0,3) + "-" + 
					dashx.toString().substring(3,6) + "-" +
					dashx.toString().substring(6) + "%");
			
			rs = pstmt.executeQuery();
			
			rsValue = rs.next();
			
			if(rsValue){
				result = false;
			}
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "isLrnAllowedNpb value is: "
						+ result);
			
		}
		catch (Exception exp) {
			logErrors(exp, SOAQueryConstants.GET_SVEXISTS_WITH_PSEUDOLRN);
		}finally{
			close(rs, pstmt);
		}
		
		return result;		
	}
	
	/**
	 * This method checks whether block holder SPID is also the NPA-NXX assignee
	 */
	public boolean isDashxNpaNxxAssignee(Value dashX, Value spid, Value region){
		
		boolean result = false;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		String npa = null;
		String nxx = null;
		String dashx = null;
		
		npa = dashX.toString().substring(SOAConstants.DASHX_NPA_START_INDEX, SOAConstants.DASHX_NPA_END_INDEX);
		nxx = dashX.toString().substring(SOAConstants.DASHX_NXX_START_INDEX, SOAConstants.DASHX_NXX_END_INDEX);

		try{
			conn = getDBConnection();
			pstmt = conn.prepareStatement(SOAQueryConstants.GET_NPA_NXX_FOR_SPID);
			
			pstmt.setString(1, npa);
			pstmt.setString(2, nxx);
			pstmt.setString(3, spid.toString());
			pstmt.setString(4, region.toString());
			
			rs = pstmt.executeQuery();
			
			result = rs.next();
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "isDashxNpaNxxAssignee value is: " + result);
			
		}catch(Exception exp){
			logErrors(exp, SOAQueryConstants.GET_NPA_NXX_FOR_SPID);
		}finally{
			close(rs, pstmt);
		}		
		return result;
	}
	
	/**
	 * Returns true if NPB exists with pseudo LRN.
	 * @param dashX
	 * @param spid
	 * @param region
	 * @return
	 */
	public boolean isNPBExistWithPseudoLrn(Value dashX, Value spid, Value region){
		
		boolean result = false;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		String npa = null;
		String nxx = null;
		String dashx = null;
		String lrnValue = null;
		
		if(dashX != null){
			npa = dashX.toString().substring(SOAConstants.DASHX_NPA_START_INDEX, SOAConstants.DASHX_NPA_END_INDEX);
			nxx = dashX.toString().substring(SOAConstants.DASHX_NXX_START_INDEX, SOAConstants.DASHX_NXX_END_INDEX);
			dashx = dashX.toString().substring(SOAConstants.DASHX_START_INDEX);
		}
		try{
			conn = getDBConnection();
			pstmt = conn.prepareStatement(SOAQueryConstants.GET_LRN_FOR_NPB);
			
			pstmt.setString(1, npa);
			pstmt.setString(2, nxx);
			pstmt.setString(3, dashx);
			pstmt.setString(4, spid.toString());
			pstmt.setString(5, region.toString());
			
			rs = pstmt.executeQuery();
			
			if(rs.next()){
				lrnValue = (String)rs.getString(SOAConstants.LRN_COL);
			}
			
			if(lrnValue != null && "0000000000".equals(lrnValue)){
				result = true;
			}
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "isNPBExistWithPseudoLrn value is: " + result);
			
		}catch(Exception exp){
			logErrors(exp, SOAQueryConstants.GET_LRN_FOR_NPB);
		}finally{
			close(rs, pstmt);
		}		
		return result;		
	}
	
	public boolean validateNpaNxxSpidForPseudoLrn(Value spid){
		
		boolean result=true;				
		ArrayList tnList = new ArrayList();
		ArrayList npanxxDataList = new ArrayList();
		ArrayList failedNpaNxxList = new ArrayList();
		StringBuffer errorMessage = new StringBuffer();
		
		String initSpid = spid.toString();
		
		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The tnList for validating NPA-NXX for a SPID in pseudo LRN case: "+tnList);			
		}
		
		if (existsValue("npanxxDataList"))
		{
			npanxxDataList = (ArrayList)getContextValue("npanxxDataList");
			
			if(npanxxDataList != null)
			{
				long dbDate=0;

				for (int i = 0; i < npanxxDataList.size(); i++) 
				{
						NpaNxxData nxxData = (NpaNxxData)npanxxDataList.get(i);
						
						String dbSpid = nxxData.spid;
						
						if( !initSpid.equals(dbSpid) )
						{
							failedNpaNxxList.add(nxxData.npanxx);
						}
				}
			}		
		}
		result = failedNpaNxxList.isEmpty();
		
		if(!result){
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "failedNpaNxxList contains NPA NXX.");
			
			for (int i = 0; i < tnList.size(); i++) {
				
				String tnVal = (String)tnList.get(i);
				
				String npa = tnVal.toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX);
				String nxx = tnVal.toString().substring(SOAConstants.TN_NXX_START_INDEX, SOAConstants.TN_NXX_END_INDEX);
				
				String npanxxVal = npa+nxx;
				
				if(failedNpaNxxList.contains(npanxxVal))
				{			
					addFailedTN(tnVal.toString());				
					errorMessage.append("["+tnVal.toString()+"]");
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"failedTn npaxx"+tnVal.toString());
				}
			}
			setDynamicErrorMessage(errorMessage.toString());
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "validateNpaNxxSpidForPseudoLrn value is: " + result);
		
		return result;
	}
	
	public boolean validateNpaNxxSpidTnForPseudoLrn(Value spid){
		
		boolean result=true;
		boolean isIntraPort = false;
		
		ArrayList tnList = new ArrayList();
		TreeSet npanxxDataList = new TreeSet();
		ArrayList npanxxDbList = new ArrayList();
		ArrayList failedNpaNxxList = new ArrayList();
		StringBuffer errorMessage = new StringBuffer();
		StringBuffer npaNxxValue = new StringBuffer();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		String initSpid = spid.toString();
		NpaNxxData npanxxRs = null;	
		String lnpType = null;
		ArrayList svDataList = null;
		
	   if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The tnList for validating NPA-NXX for a SPID in pseudo LRN case: "+tnList);			
		}
		
	   if(existsValue("modifySvRs")){

		   svDataList =(ArrayList)getContextValue("modifySvRs");
		   
		   if(svDataList!=null)
			{
				for (int i=0;i < svDataList.size();i++ )
				{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
					
					lnpType = svData.lnptype;
					
					if("lisp".equals(lnpType)){		   
						   isIntraPort = true;
					}
					
					if(isIntraPort){
						   
						   if (existsValue("npaNnxList"))
							{
								npanxxDataList = (TreeSet)getContextValue("npaNnxList");
								
								Iterator npaNxxItr = npanxxDataList.iterator();
								
								int j=0;
								
								while(npaNxxItr.hasNext())
								{				
									npaNxxValue.append("'");
									
									npaNxxValue.append((String)npaNxxItr.next());				
									
									if ( j != npanxxDataList.size()-1)				
										npaNxxValue.append("',");
									else
										npaNxxValue.append("'");
									j++;
								}
								String npanxxQuery ="SELECT NPA||NXX, SPID, EFFECTIVEDATE, REGIONID FROM " +
										"SOA_NPANXX WHERE (NPA|| NXX) in ("+npaNxxValue+") AND STATUS = 'ok'";

								if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
									Debug.log(Debug.NORMAL_STATUS,"Executing query napnxxQuery: " + npanxxQuery);

								try{
									conn = getDBConnection();
									stmt = conn.createStatement();
									rs = stmt.executeQuery(npanxxQuery);

									while(rs.next()){
										
										String dbNpaNxx = rs.getString(1);
										String dbSpid = rs.getString(2);
										long dbEffectiveDate = rs.getTimestamp(3).getTime();
										String dbRegionId = rs.getString(4);
										
										npanxxRs = new NpaNxxData( dbNpaNxx, dbSpid, dbEffectiveDate, dbRegionId);
										npanxxDbList.add( npanxxRs );								
									}
								}
								catch(Exception exp){
									logErrors(exp, npanxxQuery);
								}finally{
									close(rs, stmt);
								}
										}
							if(npanxxDbList != null)
							{
								long dbDate=0;

								for (int k = 0; k < npanxxDbList.size(); k++) 
								{
									NpaNxxData nxxData = (NpaNxxData)npanxxDbList.get(k);
									
									String dbSpid = nxxData.spid;
									
									if( !initSpid.equals(dbSpid) )
									{
										failedNpaNxxList.add(nxxData.npanxx);
									}
								}
							}
					}
				}
			}	   
	   
	   				
		result = failedNpaNxxList.isEmpty();
		
		if(!result){
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "failedNpaNxxList for Sv Modify with TN contains NPA NXX.");
			
			for (int i = 0; i < tnList.size(); i++) {
				
				String tnVal = (String)tnList.get(i);
				
				String npa = tnVal.toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX);
				String nxx = tnVal.toString().substring(SOAConstants.TN_NXX_START_INDEX, SOAConstants.TN_NXX_END_INDEX);
				
				String npanxxVal = npa+nxx;
				
				if(failedNpaNxxList.contains(npanxxVal))
				{			
					addFailedTN(tnVal.toString());			
					errorMessage.append("["+tnVal.toString()+"]");
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"failedTn npaxx"+tnVal.toString());
				}
			}
			setDynamicErrorMessage(errorMessage.toString());
		}
	   }
	   if (Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "validateNpaNxxSpidTnForPseudoLrn value is: " + result);
		
		return result;
	}
	
	/**
	 * This method checks,
	 * NPA-NXX (Extract from TN) Region should be one of the LRN region. 
	 * @param spid
	 * @param lrn
	 * @return
	 */
	public boolean isNpaNxxSupportLrnRegion(Value spid, Value lrn){
		
		boolean result = true;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;		
		ArrayList npaNxxDBList = null;
		ArrayList failedIndividualTnList = null;
		ArrayList failedNpaNxxRegionList = new ArrayList();
		ArrayList lrnRegionList = new ArrayList();
		ArrayList failedDashxRegionList = new ArrayList();
		
		HashMap npaNxxDashx = null;
		
		TreeSet npaNxxDashxList = null;
		TreeSet failedTnList = new TreeSet();
		
		StringBuffer errorMessage = new StringBuffer();
		String failedTn = null;
		
		String lrnRegionQuery = "SELECT REGIONID FROM SOA_LRN WHERE " +
				"SPID='" + spid.toString() +
				"' AND LRN=" +lrn.toString()+ 
				" AND STATUS='ok'";

		try{
			con = getDBConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(lrnRegionQuery);
			
			while(rs.next()){
				lrnRegionList.add(rs.getString(1));
			}
			if(!lrnRegionList.isEmpty())
			{
				if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				{
					Debug.log(Debug.MSG_STATUS, "lrnRegionList contains LRN Region data" );
				}
			}else{
				if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				{
					Debug.log(Debug.MSG_STATUS, "lrnRegionList does not contains LRN Region data" );
				}
			}
		}		
		catch(Exception exp){
			logErrors(exp, lrnRegionQuery);
		}finally{
			close(rs, stmt);
		}
		
		if(!lrnRegionList.isEmpty())
		{
			if (existsValue("npanxxDataList"))
			{
				npaNxxDBList = (ArrayList)getContextValue("npanxxDataList");			
				if(npaNxxDBList != null)
				{
					for (int i = 0; i < npaNxxDBList.size(); i++) 
					{					
						NpaNxxData nxxData = (NpaNxxData)npaNxxDBList.get(i);						
						String npaNxxRegion = nxxData.regionId;						
						
						if(!lrnRegionList.contains(npaNxxRegion))
						{
							failedNpaNxxRegionList.add(nxxData.npanxx);
						}
					}
				}			
			}
		}

		if(!failedNpaNxxRegionList.isEmpty())
		{
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				Debug.log(Debug.MSG_STATUS, "failedNpaNxxRegionList contains data, means NPA-NXX(s) does not belongs to LRN Region");
			}
			if (existsValue("npaNxxXList")) {
				
				npaNxxDashxList = (TreeSet) getContextValue("npaNxxXList");				
				String dashX = null;
				String npanxx = null;				
				
				if(npaNxxDashxList != null){
					Iterator npaNxxDashxIterator = npaNxxDashxList.iterator();
					
					while (npaNxxDashxIterator.hasNext()) {
						dashX = (String) npaNxxDashxIterator.next();
						npanxx = dashX.substring(0, dashX.length() - 1);
						
						if (failedNpaNxxRegionList.contains(npanxx)) {
							failedDashxRegionList.add(dashX);
						}
					}
				}
			}
		}
		if(!failedDashxRegionList.isEmpty())
		{
			npaNxxDashx = (HashMap) getContextValue("npaNxxXDashx");
			
			if(npaNxxDashx != null){
				String dash_x = null;
				
				Iterator failedDashxIterator = failedDashxRegionList.iterator();				
				while(failedDashxIterator.hasNext()){
					dash_x = (String) failedDashxIterator.next();
					failedIndividualTnList = (ArrayList) npaNxxDashx.get(dash_x);
					failedTnList.addAll(failedIndividualTnList);
				}
			}			
		}
		if(!failedTnList.isEmpty())
		{
			result = false;
			Iterator failedTnIterator = failedTnList.iterator();			
			while(failedTnIterator.hasNext()){
				failedTn = (String) failedTnIterator.next();
				addFailedTN(failedTn);
				errorMessage.append("[" + failedTn + "] ");
			}
			setDynamicErrorMessage(errorMessage.toString());
		}
		Debug.log(Debug.MSG_STATUS, "Result for isNpaNxxSupportLrnRegion : " + result);
		
		return result;
		
	}
	
	/**
	 * This method checks,
	 * NPA-NXX Region should be one of the LRN region. 
	 * @param spid
	 * @param lrn
	 * @return
	 */
	public boolean isNpaNxxSupportLrnRegionInNPB(Value npaNxxX){
		boolean result = true;
		
		Connection con = null;
		ResultSet rs = null;
		Statement stmt = null;
		
		String npaNxx = null;
		String dbNpaNxx = null;
		String dbSpid = null;
		String dbNpaNxxRegionId = null;		
		ArrayList lrnRegionIdList = null;			
		if(npaNxxX!=null & npaNxxX.toString() != "")
		{			
			String npaNxxX_str = npaNxxX.toString();
			
			npaNxx = npaNxxX_str.substring(SOAConstants.DASHX_NPA_START_INDEX,
					SOAConstants.DASHX_NPA_END_INDEX)
					+ npaNxxX_str.substring(SOAConstants.DASHX_NXX_START_INDEX,
							SOAConstants.DASHX_NXX_END_INDEX);
		
			String napnxxQuery ="SELECT REGIONID FROM " +
			"SOA_NPANXX WHERE (NPA|| NXX) in ('"+npaNxx+"') AND " +
			"STATUS = 'ok'";
			try{
				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS,"Executing napnxxQuery: "+napnxxQuery);
				
				con = getDBConnection();
				stmt = con.createStatement();		 
				rs= stmt.executeQuery(napnxxQuery);
				while( rs.next() ){
					dbNpaNxxRegionId = rs.getString(1);
				}		
			}
			catch(Exception exp){
				logErrors(exp, napnxxQuery);				
			}		
			finally{
				close(rs, stmt);
			}
			if(existsValue("LrnRegionList")){
				
				lrnRegionIdList = (ArrayList) getContextValue("LrnRegionList");
				
				if(lrnRegionIdList != null){
					if(dbNpaNxxRegionId != null){
						if(!lrnRegionIdList.contains(dbNpaNxxRegionId)){
							result = false;							
						}
					}
				}
			}
		}
		
		return result;
		
	}
	

	/**
	 * This method checks,
	 *  Incoming RegionId should be one of the provided LRN regions. 
	 * @param spid
	 * @param lrn
	 * @return
	 */
	public boolean isLrnSupportRegion(Value spid,Value regionId, Value lrn){
		
		boolean result = true;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;				
		ArrayList lrnRegionList = new ArrayList();		
		String lrnRegionQuery = "SELECT REGIONID FROM SOA_LRN WHERE " +
				"SPID='" + spid.toString() +
				"' AND LRN=" +lrn.toString()+ 
				" AND STATUS='ok'";

		try{
			
			con = getDBConnection();
			stmt = con.createStatement();			
			rs = stmt.executeQuery(lrnRegionQuery);			
			
			while(rs.next()){
				lrnRegionList.add(rs.getString(1));
			}			
			
			if(!lrnRegionList.isEmpty())
			{
				
				String regionID = regionId.toString().substring(3);				
				if(Debug.isLevelEnabled(Debug.MSG_STATUS))				
					Debug.log(Debug.MSG_STATUS, "isLrnSupportRegion() :lrnRegionList contains LRN Region data" );
				
				if(!lrnRegionList.contains(regionID)){					
					result = false;							
				}
				 
			}else{
				if(Debug.isLevelEnabled(Debug.MSG_STATUS))				
					Debug.log(Debug.MSG_STATUS, "isLrnSupportRegion() :lrnRegionList does not contains LRN Region data" );				
			}
		}		
		catch(Exception exp){
			logErrors(exp, lrnRegionQuery);
		}finally{
			close(rs, stmt);
		}
	
		return result;
	}
	
	/**
    * This method is used to validate NPA NXX against SPID for SVID request
	* @param spid
    */
   public boolean validateNpaNxxSpidSvId(Value spid){
		   
		   boolean result = true;	   
		   
		   String tn = getContextValue("portingTn").toString();
		   String lnpType = getContextValue("lnptypeSvid").toString();
		   
		   Connection conn = null;       
		   PreparedStatement pstmt = null; 
		   ResultSet results = null;
		   String npa = null;
		   String nxx = null;
		   
		   StringBuffer errorMessage = new StringBuffer();
		   
		   if(lnpType != null){
			   
			   if("lisp".equals(lnpType)){
				   
				   try{
					   conn = getDBConnection();

					  if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
						  Debug.log(Debug.NORMAL_STATUS, " SQL, validateNpaNxxSpid :\n" + SOAQueryConstants.VALIDATE_NPA_NXX_WITH_SPID_QUERY);

					  pstmt = conn.prepareStatement(
								SOAQueryConstants.VALIDATE_NPA_NXX_WITH_SPID_QUERY);
					  if(tn != null){
						  
						  npa = tn.toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX);
						  nxx = tn.toString().substring(SOAConstants.TN_NXX_START_INDEX, SOAConstants.TN_NXX_END_INDEX);
					  }
					  pstmt.setString( 1, npa);
			          pstmt.setString( 2, nxx);
			          pstmt.setString( 3, spid.toString() );
			          
			          results = pstmt.executeQuery();
			          
			          result = results.next();
			          
					  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						  Debug.log(Debug.MSG_STATUS,"validateNpaNxxSpid value is: "+result);
				         
					   }catch(Exception exception){
						   logErrors(exception, 
									SOAQueryConstants.VALIDATE_NPA_NXX_WITH_SPID_QUERY);
					   }
					   finally{
						   close(results, pstmt);
					   }
			   }
		   }
		   
		   if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "validateNpaNxxSpidSvId value is: " + result);
		   
		   return result;
	   }

}
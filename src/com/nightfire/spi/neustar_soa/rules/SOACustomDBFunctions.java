/**																				
 * This class contains utility methods for accessing SOA data from the
 * database. These custom functions are intended to be used by SOA
 * business rules that need to check the current state of a record.
 *
 * @author Matt
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see		com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
 * @see 	com.nightfire.framework.db.DBInterface;
 * @see		com.nightfire.framework.db.DatabaseException;
 * @see		com.nightfire.framework.rules.RuleEvaluatorBase;
 * @see		com.nightfire.framework.rules.Value;
 * @see		com.nightfire.framework.util.CustomerContext;
 * @see		com.nightfire.framework.util.Debug;
 * @see		com.nightfire.framework.util.StringUtils;
 * @see		com.nightfire.spi.neustar_soa.utils.SOAConstants;
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Devaraj			11/22/2004		Added additional functions to
												perform DB related business
												rules for various requests.
												
	2			Devaraj			12/13/2004		Added rules for checking 
												LastMessage	and status.
												
	3			Devaraj			12/17/2004		Modified SvCreateRequestAllowed
												,SvReleaseRequestAllowed and 
												SvReleaseInConflictRequest
												Allowed.
												
	4			Devaraj			12/29/2004		Removed active status in the 
												query  
												NEW_SP_QUERY_TN_SV_CREATE 
												from where condition. 
												
	5			Devaraj			12/30/2004		Added Status in where condition
												for Audit Name and Audit Id 
												Exist queries.
												
	6			Devaraj			12/31/2004		Added status - conflict and 
												lastmessage 
												SvCreateAckNotification, 
												SvCreateNotification 
												for SvCancelRequest,
												SvModifyRequest,
												SvRemoveFromConflictRequest.
												
	7			Devaraj			03/01/2005		Removed All Request 
												Allowed methods.
	
	8			Devaraj			04/01/2005		Modified historyContains method 
												implementation.
												
	9			Devaraj			07/01/2005		Final inspection review 
												comments ncorporated.
												
	10			Devaraj			10/01/2005		Added dynamic error messages 
												for range methods.
												
	11			Devaraj			13/01/2005		Added methods to find TN range 
												is continuos or not.
												
	12			Devaraj			02/03/2005		Added methods for SOA 3.2 
												requirements
												for dynamic error messages.
												Updated all range methods 
												for SOA 3.2.
												
	13			Devaraj			04/04/2005		Review comments incorporated.
	
	14			Devaraj			04/06/2005		Added Audit Query related 
												methods for 
												SOA 3.1.1 bug fixes.
												
	15			Devaraj			04/14/2005		Corrected some of the error
												messages.
	16			Devaraj			04/21/2005		FI review comments 
												incorporated.
	
	17			Devaraj			05/16/2005		Removed static reference for 
												all methods.
												
	18			Devaraj			05/19/2005		Added else loop in logErrors 
												method.																																																							


	19			SUNIL.D			12/29/2006		Added triggerSvId().
	
	20 			Peeyush M		05/21/2007		Modify triggerModifySv() for Subdomain requirement.
	
	21			Peeyush M		07/04/2007		Added new rule isNewSPActiveWithSpid() to fix the TD #6458.
	
	22			Peeyush M		07/05/2007		Change mehod name from isNewSPActiveWithSpid() to isTNAlreadyBelongNewSP()
												Also change the SQL Query.	

 */


package com.nightfire.spi.neustar_soa.rules;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Iterator;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.rules.RuleEvaluatorBase;
import com.nightfire.framework.rules.Value;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.neustar_soa.utils.DynamicComparator;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.NancPropException;
import com.nightfire.spi.neustar_soa.utils.SOAConfiguredSPIDCache;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;

/**
 * This class contains utility methods for accessing SOA data from the
 * database. These custom functions are intended to be used by SOA
 * business rules that need to check the current state of a record.
 * 
 */
public abstract class SOACustomDBFunctions extends RuleEvaluatorBase{
   
	//For TD#11317, added SOAConstants.ACCOUNT_ADD_SV_REQUEST,SOAConstants.ACCOUNT_REMOVE_SV_REQUEST in svRequestTypes array.
	//	Array of All Sv request types
	public static final String[] svRequestTypes = {SOAConstants.SV_QUERY_REQUEST,SOAConstants.SV_CREATE_REQUEST,SOAConstants.SV_MODIFY_REQUEST,SOAConstants.SV_CANCEL_REQUEST,SOAConstants.SV_RELEASE_REQUEST,SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST,SOAConstants.SV_ACTIVATE_REQUEST,SOAConstants.SV_CANCEL_AS_NEW_REQUEST,SOAConstants.SV_CANCEL_AS_OLD_REQUEST,SOAConstants.SV_DISCONNECT_REQUEST,SOAConstants.SV_REMOVE_FROM_CONFLICT_REQUEST,SOAConstants.ACCOUNT_ADD_SV_REQUEST,SOAConstants.ACCOUNT_REMOVE_SV_REQUEST};
	// Constants for Tn and Tn range
	public static final String UPSTREAM_TO_SOA_BODY = "/SOAMessage/UpstreamToSOA/UpstreamToSOABody/";
	public static final String TN = "/Subscription/Tn";
	public static final String STARTTN = "/Subscription/TnRange/Tn";
	public static final String ENDTN = "/Subscription/TnRange/EndStation";
	public static final String QUERYNPAC_TN = "/QueryNPAC/Subscription/Sv/Tn";
	public static final String QUERYNPAC_STARTTN ="/QueryNPAC/Subscription/TnRange/Tn";
	public static final String QUERYNPAC_ENDTN ="/QueryNPAC/Subscription/TnRange/EndStation";
	
   /**
    * Normally, rules just return a static error message. This string,
    * if it is given a value, can be used to add a more specific message
    * to the rule error. This is useful when validating ranges of data to
    * indicate which value(s) in the range failed.
    */
   private String dynamicErrorMessage = null;

   /**
    * This is called after this rule has been executed in order to clean up
    * any cached values. This is overriden in order to clear out the
    * dynamic error message after this rule has been executed.
    */
   public  void cleanup(){

      super.cleanup();

      dynamicErrorMessage = null;

   }

   /**
    * This overrides the default getDescription() method in RuleEvaluatorBase.
    * This allows for a dynamically generated error message to be appended
    * to the error message. For example,
    * "The telephone number 111-222-3333 has a status of 'cancel-pending'."
    * This can be used to return a more specific error message to be returned
    * to the end user. This can be useful, especially when processing ranges
    * of numbers, to communicate which number(s) exactly failed validation.
    *
    * @return String The description of this rule. This will
    * be used as the error message if a message violates this rule.
    * If the dynamicErrorMessage is not null, then it will be appended to the
    * static description message and the whole thing will be returned.
    */
   public String getDescription() {

      String desc = super.getDescription();

      if( dynamicErrorMessage != null){

         desc = desc+" "+dynamicErrorMessage;

      }

      return desc;

   }

   /**
    * This method is used to set the dynamic error message to the given 
    * error message.
    * @param errorMessage
    */
   protected void setDynamicErrorMessage(String errorMessage){
       
       dynamicErrorMessage = errorMessage;
   }
   
   /**
    * This method is used to check if the given SPID belongs to the current
    * customer. The current customer's customer ID is retrieved from the
    * CustomerContext.
    *
    * @param spid Value the SPID to check.
    * @return boolean whether or not the spid belongs to the current customer
    * ID.
    */
   public boolean isCurrentCustomersSPID(Value spid){

      boolean result = false;

      Connection conn = null;

      PreparedStatement pstmt = null;
      
      ResultSet results = null;

      try{

         conn = getDBConnection();
         
         if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        	 Debug.log(Debug.NORMAL_STATUS, "SQL to get CustomerID is :\n"+ SOAQueryConstants.CUSTOMER_SPID_QUERY);
        	 
         pstmt = conn.prepareStatement(SOAQueryConstants.
         CUSTOMER_SPID_QUERY);

         pstmt.setString( 1, spid.toString() );
      
         pstmt.setString( 2, CustomerContext.getInstance().getCustomerID() );

         results = pstmt.executeQuery();

         result = results.next();
         
         if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
        	 Debug.log(Debug.MSG_STATUS,"isCurrentCustomersSPID: "+result);

      }catch(Exception exception){

         logErrors(exception, SOAQueryConstants.CUSTOMER_SPID_QUERY);
         
      }finally{

         close(results, pstmt); 

      }

      // TD#6388 
      // if Spid does not belong to Customer
      //Check only for All Sv Request Types
      
      if (!result)
	  {
		  // Check only for All Sv Request Types
		  for (int ctr=0; ctr < svRequestTypes.length ; ctr++)
		  {
			  //if it is Sv Request then add Tn or TnRange in errormessage.
			  if (present(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]))
			  {
				  ArrayList submittedTnList = new ArrayList();
				  StringBuffer errorMessage = new StringBuffer();
				    
				  Value tn = null;
				  Value startTN = null;
				  Value endStation = null;
				  
				  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					  Debug.log(Debug.MSG_STATUS,"isCurrentCustomersSPID, Sv requestType : "+UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]);
				
				  //Except SvQueryRequest
				  if (present(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+TN) ||
							(  present(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+STARTTN) &&
							  present(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+ENDTN) ))
				  {
					    // Gets Tn and TnRange Values
					    tn = value(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+TN);
						startTN = value(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+STARTTN);
						endStation = value(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+ENDTN); 
				  }
				  // for SvQueryRequest
				  else if(present(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+QUERYNPAC_TN) ||
							(  present(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+QUERYNPAC_STARTTN) &&
									  present(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+QUERYNPAC_ENDTN) ))
				  {
					 	//Gets Tn and TnRange Values
					    tn = value(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+QUERYNPAC_TN);
						startTN = value(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+QUERYNPAC_STARTTN);
						endStation = value(UPSTREAM_TO_SOA_BODY+svRequestTypes[ctr]+QUERYNPAC_ENDTN);
				  }
				  				    
				  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"isCurrentCustomersSPID: Getting submitted Tn from Context : "+ tn.toString() +"\n"+ startTN.toString() +"\n"+ endStation.toString());
					
				  //if it is not TnRange
				  if(startTN != null)
				  {	  
					  if (startTN.toString().equals(""))
						  {		 	 
							 Value[] tnTokens = tn.split(";");
							 for (int i=0; i < tnTokens.length; i++)
							 {							
								submittedTnList.add(tnTokens[i].toString());
							 }
					  }
				  }
				  // if request submitted as TnRange not Tn
				  if(tn != null)
				  {
					  if (tn.toString().equals(""))
					  {
					  	 submittedTnList.add(startTN.toString()+"-"+endStation.toString());
					  }
				  }
	
				  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						 Debug.log(Debug.MSG_STATUS,"isCurrentCustomersSPID method, submittedTnList:  " +submittedTnList);
								 
				  Collections.sort(submittedTnList);
				  errorMessage.append(submittedTnList);
				  setDynamicErrorMessage(errorMessage.toString());
				  break;
			  }
		  }
	  }
      
      return result;

   }
   
   /**
    * This method is used to check if ONSP is looked up by auto Population feature.
    * if ONSP auto populated, it returns 'true'
    * otherwise, it returns 'false' 
    * @return boolean whether or not the ONSP auto populated (looked up).
    * 
    */
   
   public boolean isONSPAutoPopulated(){	   
	   
	   boolean result = false;
	   String lookupOnsp =null;
	   try
	   {
		   lookupOnsp = (String)CustomerContext.getInstance().get("lookupONSP");
		   if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		      	 Debug.log(Debug.MSG_STATUS," :isONSPAutoPopulated: ["+lookupOnsp +"]");
		   if(StringUtils.hasValue(lookupOnsp))
		   {
			   result=true;
		   }
		   
		   
	   }catch(Exception exception){
	
	       logErrors(exception, ":Could not checked whether ONSP is auto-populated or not");
	       
	    }	   	   	   	   
	   return result;	   
	   
   }
   
   /**
    * This method is used to check if ONSP auto Population feature is turned ON or OFF.
    * if ONSP auto Population feature is turned ON, it returns 'true'
    * otherwise, it returns 'false' 
    * @param spid Value for which auto population feature need to check.
    * @return boolean whether or not the ONSP auto Population feature is turned ON or OFF.
    * 
    */
   
   public boolean isONSPAutoPopulationFlagON(Value spid){	   
	   boolean result = false;
	   String onspFlag =null; 
	   try 
	   {
		   onspFlag = String.valueOf(NANCSupportFlag.getInstance(	   
			   spid.toString()).getOnspFlag());
		   
		   if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
	      	 Debug.log(Debug.MSG_STATUS," :isONSPAutoPopulationFlagON: ["+onspFlag +"]");
		   
		   if (onspFlag!=null && "1".equals(onspFlag))
			   result = true;		   
	   
	   }catch(Exception exception){
	
	       logErrors(exception, ":Could not check ONSP Population feature is turned ON or OFF");
	       
	    }	   	   	   
	   return result;
	   
   
   }
   
   /**
    * This method is used to check if the given SPID belongs is available
    * in SOA_SPID table. The spid value firstly retrieved from SOA-SPID 
    * Configured Cache.
    *
    * @param spid Value the SPID to check.
    * @return boolean whether or not the spid present in SOA_SPID table.
    * 
    */
   public boolean isSpidAvailableInSOA(Value spid){

	      boolean result = false;	
	      
	      String tn = null;

		  ArrayList<String> tnList = new ArrayList<String>();

		  StringBuffer errorMessage = new StringBuffer();
			
		  if (existsValue("tnList"))
		  {
			tnList =(ArrayList)getContextValue("tnList");			
		  }

	      try{	         
	    	  if (!SOAConfiguredSPIDCache.getInstance(spid.toString()).getSpid()
						.equals("0")) {
					result = true;
				} else {
					result = false;
				}
	         if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
	        	 Debug.log(Debug.MSG_STATUS,"isSpidAvailableInSOA: "+result);

	      }catch(Exception exception){

	         logErrors(exception, "SPID is not Configured in SOA_SPID table");
	         
	      }
	      
	      if (!result)
			{
				for (int j = 0;j < tnList.size() ;j++ )
				{
					tn = (String)tnList.get(j);
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"Result for SPID present in SOA_SPID: "+tn);
					
					addFailedTN(tn);
					errorMessage.append("[" + tn + "] ");
				}
				
				setDynamicErrorMessage(errorMessage.toString());
			}	
	      
	      return result;

	   }
   
    /**
    * Utility method for cleaning up a statement and returning a connection
    * to the pool. This method takes care of catching and logging any
    * exceptions.
    *
    * @param statement PreparedStatement this statement will be closed, if not
    *                                    null.
    * @param connection Connection this connection will be returned to the
    *                              connection pool, if not null.
    */
   protected static void close(ResultSet rs,
           					   PreparedStatement statement){
       
      if (rs != null){
          
          try{
              
              rs.close();
              
          }catch(SQLException sqlex){
              
              Debug.error("Could not close result set: "+sqlex);
          }
          
      }
      
      if( statement != null ){

         try{
           
             statement.close();
             
         }catch(SQLException sqlex){
            
             Debug.error("Could not close statement: "+sqlex);
         }

      }     

   }

   /**
    * Utility method for cleaning up a statement and returning a connection
    * to the pool. This method takes care of catching and logging any
    * exceptions.
    *
    * @param statement Statement this statement will be closed, if not
    *                                    null.
    * @param rs ResultSet this connection will be returned to the
    *                              connection pool, if not null.
    */
   protected static void close(ResultSet rs,
           					   Statement statement){
       
      if (rs != null){
          
          try{
              
              rs.close();
              
          }catch(SQLException sqlex){
              
              Debug.error("Could not close result set: "+sqlex);
          }
          
      }
      
      if( statement != null ){

         try{
           
             statement.close();
             
         }catch(SQLException sqlex){
            
             Debug.error("Could not close statement: "+sqlex);
         }

      }     

   }
   
   /**
    * Utility method to print the debug statements.
    * @param exception
    * @param query
    */
   protected void logErrors(Exception exception, String query){
   	
   		if (exception instanceof SQLException){
   			
   			Debug.error("Could not execute query ["+query+"]: "+exception );
  
   		 
   		} else if (exception instanceof DatabaseException){
   			
   			Debug.error("Database initialization failure:  " + 
   						exception.getMessage() );
   		} else if (exception instanceof FrameworkException){
   			
   			Debug.error("Framework Exception while getting Customer Context" +
   						exception.getMessage());
   		} else {
   			
   			Debug.error(exception.getMessage());
   		}
   	
   }

 

	/**
	 * This method returns boolean and set the value into the context.
     * @param spid
	 * @param svid
	 * @param regionid
     * @return boolean
	 */
	
	public boolean triggerSvId(Value spid , Value svid, Value regionid){

	  Connection conn = null;

      PreparedStatement pstmt = null;
      
      ResultSet results = null;	 
      
     	  
   try{
		  
		  boolean result = false;

		  Date  nnspduedateSvId = null;
	
	      String statusSvId = null;
         
          String nnspSvId = null;

		  String bussinessTimerTypeSvId=null;

		  String onspSvId = null;
		 
		  String portingTn = null;

		  String causeCode = null;

		  Date  onspduedateSvId = null;

		  boolean portingToOriginal = false;

		  boolean authorizationFlag = false;
		  
		  Date objectCreationDate = null;
		  
		  String lastRequestType = null;
		  
		  String lastProcessing = null;
		  // Changes are made in 5.6.5 release (NANC 441 req)
		  String onspSimplePortIndicatorSvId = null;
		  
		  String nnspSimplePortIndicatorSvId = null;
		  
		  String activateTimerExpiration = null;
		  // end 5.6.5 changes

		  String lrn = null;		  
		  String lnptype = null;
		  
		 // Get Connection from RuleContext so that it is shared by other 
		 //functions.

		 conn = getDBConnection();

		 if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        	 Debug.log(Debug.NORMAL_STATUS, "Execute SQL to check the existence of SVID :\n"+ SOAQueryConstants.EXISTS_SVID);
		 
		 pstmt = conn.prepareStatement(SOAQueryConstants.EXISTS_SVID);

		 pstmt.setString( 1, spid.toString() );

		 pstmt.setString( 2, svid.toString() );

		 pstmt.setString( 3, regionid.toString() );

		 results = pstmt.executeQuery(); 
		 		
		while( results.next() ){

			    result = true; 

				nnspSvId = results.getString(1);
				
				statusSvId = results.getString(2);
				
				nnspduedateSvId = results.getTimestamp(3);	
								
				bussinessTimerTypeSvId = results.getString(4);
				
				onspSvId = results.getString(5);
								
				portingTn = results.getString(6);

				causeCode = results.getString(7);

				onspduedateSvId = results.getTimestamp(8);	
				
				portingToOriginal = results.getBoolean(9);
				
				authorizationFlag = results.getBoolean(10);
				
				objectCreationDate = results.getDate(11);
				
				lastRequestType = results.getString(12);
				
				lastProcessing = results.getString(13);
				//Changes are made in 5.6.5 release (NANC 441 req)
				nnspSimplePortIndicatorSvId = results.getString(14);
				
				onspSimplePortIndicatorSvId = results.getString(15);
				//added for activation rule
				activateTimerExpiration = results.getString(16);
				//end 5.6.5 changes			
				
				lrn = results.getString(17);
				
				lnptype = results.getString(18);
		  }
		 	

		 // Set the DB query Result in context, for other rules to use it.
		 setContextValue("existSv",new Boolean(result));

		 
		if (nnspSvId != null)
		{
			setContextValue("nnspSvId", nnspSvId);

		}
		
		setContextValue("spidSvId", spid.toString());
		 
		if (statusSvId != null)
		{
			setContextValue("statusSvId",statusSvId);

		}
		 if (nnspduedateSvId != null)
		 {
			 setContextValue("nnspduedateSvId",nnspduedateSvId);
		 }
		 
		 if (bussinessTimerTypeSvId != null)
		 {
			 setContextValue("bussinessTimerTypeSvId", bussinessTimerTypeSvId);
        
		 }
		 if (onspSvId != null)
		 {
			 setContextValue("onspSvId",onspSvId);
		 }
		  
		 if (portingTn != null)
		 {
			 setContextValue("portingTn",portingTn);
		 }

		 if (causeCode != null)
		 {
			 setContextValue("causeCode",causeCode);
		 }	
		 
		 if (onspduedateSvId != null)
		{
			setContextValue("onspduedateSvId",onspduedateSvId);
		}
		
		   setContextValue("portingToOriginal", new Boolean(portingToOriginal));
		   setContextValue("authorizationFlag", new Boolean(authorizationFlag)); 
		   
		   if (objectCreationDate != null)
			{
				setContextValue("objectCreationDate",objectCreationDate);
			}
		if (lastRequestType != null)
		{
			setContextValue("lastRequestType",lastRequestType);
		}
		
		if (lastProcessing != null)
		{
			setContextValue("lastProcessing",lastProcessing);
		}
		// Changes are made in 5.6.5 release (NANC 441 req)
		if(nnspSimplePortIndicatorSvId != null)
		{
			setContextValue("nnspSimplePortIndicatorSvId",nnspSimplePortIndicatorSvId);
		}
		
		if(onspSimplePortIndicatorSvId != null)
		{
			setContextValue("onspSimplePortIndicatorSvId",onspSimplePortIndicatorSvId);
		}
		// added for sv activation rule
		if(activateTimerExpiration!=null)
		{
			setContextValue("activateTimerExpirationSvId", activateTimerExpiration);
		} 
		// end 5.6.5 changes
		
		//SOA 5.7 changes
		if(lrn != null){
			setContextValue("lrnSvid", lrn);
		}
		
		if(lnptype != null){
			setContextValue("lnptypeSvid", lnptype);
		}
		
         }catch(Exception exception){

        	 if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
        		 Debug.log(Debug.ALL_ERRORS,exception.toString());
         
         }
		 
		 finally{
		
		close(results, pstmt);

        }

		try{

             String nancflag = String.valueOf(NANCSupportFlag.getInstance(spid.toString()).getNanc399flag());

			 if(nancflag == null || nancflag.equals(""))
			nancflag="0";

			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 Debug.log(Debug.MSG_STATUS,"nancflag  : "+nancflag);
		 
		 setContextValue("nanc399flag",nancflag);
		 
	  }catch(Exception exception){
		 logErrors(exception, exception.toString());
	  }
	  try
	  {

	  Boolean altbidnanc436Value = null;
    	  Boolean alteultnanc436Value = null;
    	  Boolean alteulvnanc436Value = null;
    	  
    	  if (NANCSupportFlag.getInstance(spid.toString()).getAltBidNanc436Flag()
					.equals("1")) {
				altbidnanc436Value = Boolean.valueOf(true);
			} else {
				altbidnanc436Value = Boolean.valueOf(false);
			}
			if (NANCSupportFlag.getInstance(spid.toString()).getAltEultNanc436Flag()
					.equals("1")) {
				alteultnanc436Value = Boolean.valueOf(true);
			} else {
				alteultnanc436Value = Boolean.valueOf(false);
			}
			if (NANCSupportFlag.getInstance(spid.toString()).getAltEulvNanc436Flag()
					.equals("1")) {
				alteulvnanc436Value = Boolean.valueOf(true);
			} else {
				alteulvnanc436Value = Boolean.valueOf(false);
			}    	  
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of NANC 436 Flags[ALTBID,ALTEULT,ALTEULV]: " + 
						"[" + altbidnanc436Value + "," + alteultnanc436Value + ","+ alteulvnanc436Value + "]");
		  
		  setContextValue("altbidnanc436flag",altbidnanc436Value);
		  setContextValue("alteultnanc436flag",alteultnanc436Value);
		  setContextValue("alteulvnanc436flag",alteulvnanc436Value);
		  
		  Integer pushDueDate = NANCSupportFlag.getInstance(spid.toString()).getPushduedate();
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : [" + pushDueDate +"]");
		  if(pushDueDate != null)
			  setContextValue("pushDueDate",pushDueDate);
                  
          // Changes for SimplePortIndicator flag, part of 5.6.5 release (NANC 441 req.)
		  Boolean simplePortIndicatorNANC441 = Boolean.valueOf(false);
		  if (("1".equals(NANCSupportFlag.getInstance(spid.toString())
					.getSimplePortIndicatorFlag()))
					|| ("2".equals(NANCSupportFlag.getInstance(spid.toString())
							.getSimplePortIndicatorFlag())))
		  {
			  simplePortIndicatorNANC441 = Boolean.valueOf(true);
		  }
		  
		  setContextValue("simplePortIndicatorNANC441Flag",simplePortIndicatorNANC441);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of NANC 441 Flags[Simple Port Indicator]: " + 
						"[" + simplePortIndicatorNANC441 +"]");
		  }
		  // This flag is added to check whether NNSP can submit activate request without receiving releaseAckNotification (or release notification by onsp)
		  // or T2 concurrence notification.
		  Boolean spT2ExpirationFlag = Boolean.valueOf(false);
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getSpT2ExpirationFlag()))
		  {
			  spT2ExpirationFlag = Boolean.valueOf(true);
		  }
		  setContextValue("spT2ExpirationFlag",spT2ExpirationFlag);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of spT2ExpirationFlag : " + 
						"[" + spT2ExpirationFlag +"]");
		  }
		  Boolean lastAltSPIDFlag = Boolean.valueOf(false);
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getLastAltSpidFlag()))
		  {
			  lastAltSPIDFlag = Boolean.valueOf(true);
		  }
		  setContextValue("lastAltSPIDFlag",lastAltSPIDFlag);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of lastAltSPIDFlag : " + 
						"[" + lastAltSPIDFlag +"]");
		  }
		  
		  Boolean pseudoLrnFlag = Boolean.valueOf(false);		  
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getPseudoLrnFlag()))
		  {
			  pseudoLrnFlag = Boolean.valueOf(true);
		  }
		  setContextValue("pseudoLrnFlag",pseudoLrnFlag);
		  
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of pseudoLrnFlag : " + 
						"[" + pseudoLrnFlag +"]");
		  }
         try
          {
			  // query to check whether SPID belongs to US & CN regions.
	          pstmt = conn
						.prepareStatement(SOAQueryConstants.GET_SPID_BELONG_TO_US_CN_QUERY);
	          
	          pstmt.setString(1, spid.toString());
	          
	          results = pstmt.executeQuery();
	          int spidRegionCount = 0;
	          while(results.next()){	          	
	        	  spidRegionCount = results.getInt(1);	          	
	          }
	          if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
					Debug.log( Debug.MSG_STATUS, "Spid belongs to no of regions  : "+ spidRegionCount );
	          } 
	          //If spid belongs to more then 1 region including CN region.
	          //If spidRegionCount is 1 means it belongs to CN region only.
	          //It also checking SimplePortindicator flag is on for a spid or not.
	          Boolean spidBelongsToUSandCN = Boolean.valueOf(false);
	          if(spidRegionCount > 1 && simplePortIndicatorNANC441.booleanValue())
	          {
	        	  spidBelongsToUSandCN = Boolean.valueOf(true);
	    	  }
	          setContextValue("spidBelongsToUS_CNandSPIon",spidBelongsToUSandCN);
	          if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
    		  {
    				Debug.log(Debug.MSG_STATUS,"The value of spidBelongsToUS_CNandSPIon : " + 
    						"[" + spidBelongsToUSandCN +"]");
    		  }
          }catch(Exception exception){

        	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
        		  Debug.log(Debug.ALL_ERRORS,exception.toString());
             
          }finally{
      			close(results, pstmt);
          }
		  
		  // end changes for 5.6.5
	  }
	  catch(Exception exception)
	  {
		  logErrors(exception, exception.toString());
	  }
	  
	 return true;
    }



	/**
	* To add TN in the failed list
	*/
	public void addFailedTN( String teleponeNumber ) {

		TreeSet failedTNList = null;

		try
		{
			failedTNList = (TreeSet)getContextValue("failedTNList");
			
			if (failedTNList == null)
			{
				failedTNList = new TreeSet();
			}

			failedTNList.add(teleponeNumber);

			setContextValue("failedTNList", failedTNList);
		}
		catch (Exception e)
		{
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
				Debug.log(Debug.MSG_ERROR," ERROR: addFailedTN"+e);
			
			e.printStackTrace();
		}		


	}

	public ArrayList getFailedTNList () {

		ArrayList failedList = null;
	
		TreeSet failedTNList = null;
		try
		{
			
			failedTNList = (TreeSet)getContextValue("failedTNList");

			if(failedTNList != null)
				failedList = new ArrayList (failedTNList);

		}
		catch (Exception e)
		{			
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
				Debug.log(Debug.MSG_ERROR,"Error Occured in getFailedTNList () "+ e);
		}

		return failedList;
				
	}


	public boolean setTNList ( boolean isSVID) {

		ArrayList tnList = null;
		ArrayList successTNList = null;
		ArrayList failedTNList = null;
		ArrayList submittedTnList = null;
		String tnCoalescingFlagValue = null;
		ArrayList modifedTNList = null;
		boolean isNnspOnspDifferent = false;

		if (isSVID)
			{
				tnList = getTNListSVID();
				
			}

		else{
			
			tnList = (ArrayList)getContextValue("tnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"TNList  : "+tnList);
			
			submittedTnList = (ArrayList)getContextValue("submittedTnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"Submitted TNList  : "+submittedTnList);
			
			
			}
		
		try {

			tnCoalescingFlagValue = NANCSupportFlag.getInstance(getContextValue("xsdSpid").toString()).getTnCoalescingFlag();

		} catch (NancPropException exception) {

			logErrors(exception, exception.toString());
		}
		isNnspOnspDifferent = isOnspNnspDifferent();
		
		if(StringUtils.hasValue(tnCoalescingFlagValue) && tnCoalescingFlagValue.equals("1")
				&& isNnspOnspDifferent){
			
			 ArrayList<CreateTnOnspNnspData> onspList =(ArrayList)getContextValue("tnOnspDataList");
			 if(onspList != null && onspList.size() > 0)
			 {
				 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 {	 
						Debug.log(Debug.MSG_STATUS,"setTnList(), onspList before sorting  : "+onspList.toString());
				 }
				 //Sorting by "tn" field in ascending order
				 DynamicComparator.sort(onspList, "tn", true);
				 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 {	 
						Debug.log(Debug.MSG_STATUS,"setTnList(), onspList after sorting in asc order by tn  : "+onspList.toString());
				 }
				 
				 submittedTnList = getModifiedSubmittedTnList(onspList);
				 
				 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 {
						Debug.log(Debug.MSG_STATUS,"setTnList(), submittedTnList after returning from getModifiedSubmittedTnList(): "+submittedTnList);
				 }
				 
				 //breaking all range tn belongs to submittedTnList into single tn list (modifedTNList).
				 modifedTNList = new ArrayList();
				 String tnFromML = null;
				 for (int ctr=0; ctr < submittedTnList.size(); ctr++)
				 {							
					 tnFromML = ((String)submittedTnList.get(ctr));
					 if (tnFromML.length() > 12)
					 {
						String start = tnFromML.substring(0,12);
						String end = tnFromML.substring(13);
						modifedTNList.addAll(getRangeTnList(start, end));
					 }
					 else 
					 {
						 modifedTNList.add(tnFromML);
					 }
				 }
				 
				 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 {
						Debug.log(Debug.MSG_STATUS,"setTnList(), after breaking all range tn belongs to submittedTnList into single tn list (modifedTNList) : "+modifedTNList);
				 }
				 //Checking each tn in tnList contains in modifedTNList.
				 // if it does not contain then add it in submittedTnList beacause if tn doesnot exist in SV table so it will not be part of 
				 // submittedTnList retured from getModifiedSubmittedTnList(). so in case of partial failure, failed tns will not be part of DB BR.
				 String tnFromTL =null;
				 for (int ctr=0; ctr < tnList.size(); ctr++)	 
				 {
					 tnFromTL = (String)tnList.get(ctr);
					 if(!(modifedTNList.contains(tnFromTL)))
					 {
						 submittedTnList.add(tnFromTL);
					 }
				 }
				 
				 Collections.sort(submittedTnList);
				 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 {
					Debug.log(Debug.MSG_STATUS,"setTnList(), Modified submittedTnList  : "+submittedTnList);
				 }
			 }
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)){
			Debug.log(Debug.MSG_STATUS, "The value of TN Coalescing Flag: " + "[" + tnCoalescingFlagValue + "]");
			Debug.log(Debug.MSG_STATUS, "isOnspDifferent(): " + "[" + isNnspOnspDifferent + "]");
		}
		
		if (StringUtils.hasValue(tnCoalescingFlagValue) && tnCoalescingFlagValue.equals("1")
				&& !isNnspOnspDifferent) {
			
			successTNList = collapseConsecutiveTNSubRange(getSuccessTNList(tnList));
		} else {
			
			successTNList = createSubRange(getSuccessTNList(tnList), submittedTnList);
		}
		if (successTNList != null && successTNList.isEmpty()) {
			
			successTNList.add(null);
		}
		if (StringUtils.hasValue(tnCoalescingFlagValue) && tnCoalescingFlagValue.equals("1")
				&& !isNnspOnspDifferent) {
			
			failedTNList = collapseConsecutiveTNSubRange(getFailedTNList());
			
		} else {

			failedTNList = createSubRange(getFailedTNList(), submittedTnList);
		}

		if (failedTNList != null && failedTNList.isEmpty()) {
			failedTNList.add(null);
		}
		
		try
		{			
			CustomerContext.getInstance().set("successTNList", successTNList);
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"Success TNList  : "+successTNList);
			
			CustomerContext.getInstance().set("failedTNList", failedTNList);
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"Failed TNList  : "+failedTNList);
			
		}
		catch (Exception e)
		{
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
				Debug.log(Debug.MSG_ERROR ," Error in setTNList() "+ e);
		}

		return true;
				
	}

	public ArrayList getSuccessTNList (ArrayList tnList) {
		
		TreeSet failedTNList = null;

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS," In getSuccessTNList()");
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS," In getSuccessTNList().tnList :"+tnList);
		try
		{
		
			failedTNList = (TreeSet)getContextValue("failedTNList");
			
			if(failedTNList != null)
			{
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," FailedTnList in Context, Count of FailedTNList: "+failedTNList.size());
				
				
				Iterator tmIterator = failedTNList.iterator();
	
				while (tmIterator.hasNext())
				{
					String failedTN = (String)tmIterator.next();
	
					
					int tnIndex = tnList.indexOf(failedTN);
					
					
					if (tnIndex != -1)
					{
						tnList.remove(tnIndex);	
					}
				}			
			}
			else
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," FailedTnList in Context, Count of FailedTNList = 0. ");
		}
		catch (Exception e)
		{
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
				Debug.log(Debug.MSG_ERROR ," Error in getSuccessTNList() "+ e);
		}

		return tnList;
				
	}


	public ArrayList getTNListSVID() {

		ArrayList tnList = new ArrayList();

		try
		{
			String tn = getContextValue("portingTn").toString();

			tnList.add(tn);
			
		}
		catch (Exception e)
		{
		}
		return tnList;
		
				
	}
	
	/**
	 * This method returns the list of the Tns. If TNs are submitted as a single TN and present in the tnList then those TNs will be added
	 * as single TNs in the list and if TNS are submitted as a range and if whole TNs present in tnList then add whole range to the list 
	 * otherwise create the subranges of the submitted range which are present in tnList and return the list. 
	 * @param tnList 
	 * @param submittedTnList
	 * @return
	 */
	
	public ArrayList createSubRange ( ArrayList tnList , ArrayList submittedTnList ) {
		
		if (tnList == null)
		{
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
				Debug.log(Debug.MSG_ERROR ," tnList is null");
			return tnList;
		}
		if(submittedTnList == null){
			
			if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
				Debug.log(Debug.MSG_ERROR ," submittedTnList is null");
			return tnList;
		}
		Collections.sort(tnList);
		
		ArrayList subRange = new ArrayList();
		String currentNpaNxx = null;
		String endTN = null;
		String startTN = null;			
		String startFirstTN = null;
		String rangeStartTN = null;
		String range = null;
		String temp = null;
		
		Iterator iter = submittedTnList.iterator();
		
		try
		{
			while( iter.hasNext()){
				
				String submitTn = (String)iter.next();
				if( submitTn.length() > 12 ){
					
					endTN = submitTn.substring(13);
					startTN = submitTn.substring(0, 12);
					currentNpaNxx = submitTn.substring(0,7);
					rangeStartTN = startTN;
					startFirstTN = submitTn.substring(8, 12);
					int rangeLength = Integer.parseInt(endTN)- Integer.parseInt(startFirstTN) + 1;
					int startFTN = Integer.parseInt(startFirstTN);
					int j = 0;
					for (int i = 0; i < rangeLength; i++) {
					
						if(tnList.contains(startTN)){
					
							j++;
							temp = temp + startTN + ";";
							startTN = currentNpaNxx +"-"+ StringUtils.padNumber(startFTN+1,SOAConstants.TN_LINE, true, '0');
							if(startFTN == Integer.parseInt(endTN) && j==rangeLength){
								range = rangeStartTN + "-" + StringUtils.padNumber(startFTN,SOAConstants.TN_LINE, true, '0');
								subRange.add(range);
								temp= null;
								range = null;
								break;
							}
							startFTN++;
						}else{
							if(temp !=null ){
							
								temp = (temp.substring(4, temp.length()-1)).trim();
								if(temp.length() > 12){
									
									String[] failedRangeTns = temp.split(";");
									String startRange = failedRangeTns[0];
									String endRange = failedRangeTns[failedRangeTns.length-1];
									range = startRange + "-" + endRange.substring(8,12);
									
								}else{
									
									range = temp;
									
								}
								subRange.add(range);
								temp = null;
								range = null;
								
							}
							rangeStartTN = currentNpaNxx + "-" + StringUtils.padNumber(startFTN+1,SOAConstants.TN_LINE, true, '0');
							startTN = rangeStartTN;
							startFirstTN = startTN.substring(8, 12);
							startFTN = Integer.parseInt(startFirstTN);
						}
					}
				if(temp !=null ){
					temp = temp.trim();
					temp = temp.substring(4, temp.length()-1);
					if(temp.length() > 12){
						String[] failedRangeTns = temp.split(";");
						String startRange = failedRangeTns[0];
						String endRange = failedRangeTns[failedRangeTns.length-1];
						range = startRange + "-" + endRange.substring(8,12);
					}else{
						range = temp;
					}
					subRange.add(range);
					temp = null;
					range = null;
				}
				}else{
					if (tnList.contains(submitTn)){
						subRange.add(submitTn);
					}
				}
			}
		}catch (NumberFormatException nfe)
		{
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"Format of submitted TNs are wrong " + nfe.getMessage());
		}
		
		if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
			Debug.log(Debug.MSG_ERROR ," subRange List is:" +submittedTnList);
		return subRange;
	
	}
	
	
	/**
	 * This method returns the list of the TN's. If TNs are submitted as a single
	 * TN and they are consecutive then those TNs will be added as single TN Range
	 * in the list and if TNS are submitted as a range and the TN's in the range are 
	 * consecutive then single TN range will be created and return the list.
	 * 
	 * @param tnList 
	 * @return
	 */
	
	public ArrayList collapseConsecutiveTNSubRange(ArrayList tnList) {

		if (tnList == null) {
			return tnList;
		}

		Collections.sort(tnList);

		ArrayList subRange = new ArrayList();

		Iterator iter = tnList.iterator();

		String previousNpaNxx = null;

		String currentNpaNxx = null;

		String endStation = null;

		String previousEndStation = null;

		String startTN = null;		

		int previousEndTN = -1;

		int i = 1;

		while (iter.hasNext()) {
			String tn = (String) iter.next();

			Debug.log(Debug.MSG_STATUS, i + "tn " + tn);

			currentNpaNxx = tn.substring(0, 7);

			if (previousNpaNxx != null) {

				endStation = tn.substring(8);

				Debug.log(Debug.MSG_STATUS, i + "endStation " + endStation);

				try {
					int endTN = Integer.parseInt(endStation);

					previousEndTN = Integer.parseInt(previousEndStation);

					if (currentNpaNxx.equals(previousNpaNxx)) {

						if (previousEndTN != -1 && endTN == (previousEndTN + 1)) {
							previousEndTN = endTN;
						} else {

							if (startTN.substring(8).equals(Integer.valueOf(previousEndTN).toString())) {
								subRange.add(startTN);
							} else {
								subRange.add(startTN + "-" + StringUtils.padNumber(previousEndTN, SOAConstants.TN_LINE, true, '0'));
							}
							
							previousEndTN = -1;
							previousNpaNxx = null;

							startTN = tn;

						}
					} else {

						if (startTN.substring(8).equals(Integer.valueOf(previousEndTN).toString())) {
							subRange.add(startTN);
						} else {
							subRange.add(startTN + "-" + StringUtils.padNumber(previousEndTN, SOAConstants.TN_LINE, true, '0'));
						}

						previousEndTN = -1;
						previousNpaNxx = null;

						startTN = tn;

					}

				} catch (NumberFormatException nfe) {
				}
			} else {
				startTN = tn;
			}

			previousNpaNxx = currentNpaNxx;

			previousEndStation = tn.substring(8);

			i++;

		}

		if (previousEndTN != -1) {
			subRange.add(startTN + "-" + StringUtils.padNumber(previousEndTN, SOAConstants.TN_LINE, true, '0'));
		} else {
			subRange.add(startTN);
		}

		return subRange;

	}
	
	public boolean triggerTnSv(Value spid, Value tn, Value startTN, 
	Value endStation,Value referenceKey, Value rangeId){

	  Connection conn = null;

	  Statement stmt = null;
      
      ResultSet results = null;
      
      ArrayList tnList = new ArrayList();
      ArrayList refKeyList = new ArrayList();

	  tnList = getTnList(tn, startTN, endStation);
	 
     try{		

    	 setContextValue("xsdSpid", spid);
		 boolean result = false;
		 String refKey = referenceKey.toString();
		 String RangeId = rangeId.toString();
		 
		 String initSpid = null;
		 if(refKey != null && !refKey.equals("")){
				StringBuffer refKeyBuffer = new StringBuffer();
				StringTokenizer st = new StringTokenizer(refKey, SOAConstants.SVID_SEPARATOR);
				while(st.hasMoreTokens()) {
					String individualRefKey = st.nextToken();
					if(individualRefKey != null && !individualRefKey.equals("")){
		            refKeyBuffer.append(individualRefKey);
					refKeyBuffer.append(",");
					
					refKeyList.add(individualRefKey);
					}
				}
				refKey = refKeyBuffer.toString();
				if(refKey != null && refKey.length()>0)
					refKey = refKey.substring(0, refKey.length()-1);     //remove the last comma
			}
			
		   if(RangeId != null && !RangeId.equals("")){
				StringBuffer rangeIdBuffer = new StringBuffer();
	            StringTokenizer st = new StringTokenizer(RangeId, SOAConstants.SVID_SEPARATOR);
				while(st.hasMoreTokens()) {
					String individualRangeKey = st.nextToken();
					if(individualRangeKey != null && !individualRangeKey.equals("")){
					rangeIdBuffer.append(individualRangeKey);
					rangeIdBuffer.append(",");
					}
		         }
				RangeId = rangeIdBuffer.toString();
				if(RangeId != null && RangeId.length()>0)
					RangeId = RangeId.substring(0, RangeId.length()-1);     //remove the last comma
		    }
		 for (int z=0; z<tnList.size(); z++)
		 {		
			 String finalNode = tnList.get(z).toString();
			
		 }

		 int tnCount =tnList.size();

		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"tnCount in triggerCancelSv: "+tnCount);
		 
		 StringBuffer mainQuery = new StringBuffer();
		 
		 int i = 0;

		 conn = getDBConnection();

		 while (i < tnCount)
		 {			
			int j = 1;

			StringBuffer  tnValue = new StringBuffer();
			StringBuffer  refKeyValue = new StringBuffer();
			int refKeySize = refKeyList.size();
			boolean refKeyFlag = false;	
			while (j <= 1000 && i <= tnCount-1 )
			{
				 tnValue.append("'");
				 tnValue.append(tnList.get(i));
				 
				 if(refKeySize > 0){
					 refKeyValue.append("'");
					 refKeyValue.append(refKeyList.get(i));
					 refKeyFlag = true;
					 refKeySize--;
				 }
				
				if ( j < 1000 && i != tnCount-1){
					tnValue.append("',");
					if(refKeySize > 0){
						refKeyValue.append("',");
					}
				}
					 
				else{
					tnValue.append("'");
					if(refKeySize >= 0 && refKeyFlag){
						refKeyValue.append("'");
						refKeyFlag = false;
					}
				}
					

				i++;
				j++;
				
			}
			
			 StringBuffer tnQuery = new StringBuffer();
			 tnQuery.append("(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ");
				tnQuery.append(SOAConstants.REFERENCEKEY_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.SPID_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.ONSP_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.NNSP_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.SVID_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.REGION_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.PORTINGTN_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.CREATEDDATE_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.STATUS_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.CAUSECODE_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.NNSP_DUE_DATE_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.BUSINESSTIMERTYPE_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.OBJECT_CREATION_DATE_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.LASTREQUESTTYPE_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.LASTPROCESSING_COL);
				tnQuery.append(", ");
				//Changes are made in 5.6.5 release
				tnQuery.append(SOAConstants.ONSP_DUE_DATE_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.AUTHORIZATIONFLAG_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.ACTIVATETIMEREXPIRATION_COL);
				//end 5.6.5 changes
				tnQuery.append(" FROM SOA_SUBSCRIPTION_VERSION WHERE (PORTINGTN, SPID, CREATEDDATE) IN (");
				tnQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ");
				tnQuery.append(SOAConstants.PORTINGTN_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.SPID_COL);
				tnQuery.append(", MAX(CREATEDDATE) FROM SOA_SUBSCRIPTION_VERSION WHERE ");
				tnQuery.append(SOAConstants.PORTINGTN_COL);
				tnQuery.append(" IN ("+tnValue+" )");
				if((refKey != null && !refKey.equals("")) &&  (RangeId == null || RangeId.equals(""))){
					tnQuery.append("AND REFERENCEKEY");
					tnQuery.append(" IN ("+refKeyValue+" ) ");
				}
				if((RangeId != null && !RangeId.equals("")) && (refKey == null || refKey.equals(""))){
					tnQuery.append("AND RANGEID");
					tnQuery.append(" IN ("+RangeId+" ) ");
				}
				if((RangeId != null && !RangeId.equals("")) && (refKey != null && !refKey.equals(""))){
					tnQuery.append("AND ( RANGEID");
					tnQuery.append(" IN ("+RangeId+" ) ");
					tnQuery.append("OR REFERENCEKEY");
					tnQuery.append(" IN ("+refKeyValue+" )) ");
				}
				tnQuery.append(" AND SPID = '"+spid.toString()+"' AND ");
				tnQuery.append(SOAConstants.STATUS_COL);
				tnQuery.append(" NOT IN('");
				tnQuery.append(SOAConstants.NPACCREATFAILURE_STATUS);
				tnQuery.append("', '");
				tnQuery.append(SOAConstants.CANCELED_STATUS);
				tnQuery.append("', '");
				tnQuery.append(SOAConstants.OLD_STATUS);
				tnQuery.append("', '");
				tnQuery.append(SOAConstants.CREATING_STATUS);
				tnQuery.append("')");
				tnQuery.append(" GROUP BY ");
				tnQuery.append(SOAConstants.PORTINGTN_COL);
				tnQuery.append(", ");
				tnQuery.append(SOAConstants.SPID_COL);
				tnQuery.append(") )");
								
				mainQuery.append(tnQuery);
			
			if (i <= tnCount-1)
			{
				mainQuery.append(" UNION ");
			}
			
					
		 }
		
		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"mainQuery in triggerTnSv: "+mainQuery.toString());
        
		stmt = conn.createStatement();

		results = stmt.executeQuery(mainQuery.toString());

		ResultsetData rsData = null;
		CreateTnOnspNnspData tnOnspData = null;
		ArrayList rsDataList = new ArrayList();
		ArrayList<CreateTnOnspNnspData> tnOnspDataList = new ArrayList<CreateTnOnspNnspData>();

		long rkeySv = 0;
		String spidSv = null;
		String onspSv = null;
		String nnspSv = null;
		long svidSv = 0;
		int regionidSv = 0;
		String portingTNSv = null;
		Timestamp createddateSv = null;
		String statusSv = null;
		String causeCodeSv = null;
		Timestamp nnspDueDateSv = null;
		String businessTimerTypeSv = null;
		Date objectCreationDate = null;
		String lastrequestTypeSv = null;
		String lastProcessingSv = null;
		//Changes are made in 5.6.5 release
		Timestamp onspDueDateSv = null;
		String authorizationFlag = null;
		String activateTimerExpirationFlag = null;
		TreeSet npaNxxSet = new TreeSet();
		String npaNxx = null;
		//end 5.6.5 changes
		 while( results.next() ){

			result = true;					
			
			rkeySv = results.getLong(1);      
			spidSv = results.getString(2);      
			onspSv = results.getString(3);      
			nnspSv = results.getString(4);      
			svidSv = results.getLong(5);      
			regionidSv = results.getInt(6);  
			portingTNSv = results.getString(7); 
			createddateSv = results.getTimestamp(8); 
			statusSv = results.getString(9);
			causeCodeSv = results.getString(10);
			nnspDueDateSv = results.getTimestamp(11);
			businessTimerTypeSv = results.getString(12);
			objectCreationDate = results.getDate(13);
			lastrequestTypeSv = results.getString(14);
			lastProcessingSv = results.getString(15);
			//Changes are made in 5.6.5 release (for Sv Activation rule)
			onspDueDateSv = results.getTimestamp(16);
			authorizationFlag = results.getString(17);
			activateTimerExpirationFlag = results.getString(18);
			// added onspDueDateSv, authorizationFlag, activateTimerExpirationFlag in resultSetData		
			rsData = new ResultsetData( rkeySv, spidSv, onspSv,
						nnspSv, svidSv, regionidSv, portingTNSv,
						createddateSv, statusSv, causeCodeSv,
						nnspDueDateSv, businessTimerTypeSv, objectCreationDate, lastrequestTypeSv, lastProcessingSv, onspDueDateSv, authorizationFlag, activateTimerExpirationFlag);
			//end 5.6.5 changes
			rsDataList.add( rsData );
			
			tnOnspData = new CreateTnOnspNnspData(portingTNSv,onspSv,nnspSv);
			
			tnOnspDataList.add(tnOnspData);
			
			//Changes are made for 5.6.5 release (SV Activate rule)
			//extracting npa-nxx from tn those exist in SV table.
			npaNxx = portingTNSv.substring( SOAConstants.TN_NPA_START_INDEX,
					  SOAConstants.TN_NPA_END_INDEX) +
					  portingTNSv.substring( SOAConstants.TN_NXX_START_INDEX,
					  SOAConstants.TN_NXX_END_INDEX);
			npaNxxSet.add(npaNxx);
			// end 5.6.5 changes SV Activate rule)
		  }
		 setContextValue("rsDataList", rsDataList);
		 setContextValue("tnOnspDataList", tnOnspDataList);
		 
		 setContextValue("existCancelSv",new Boolean(result));

		//Changed in 5.6.5 release (SV Activate rule)
		 //setting in rule context
		 setContextValue("npaNxxSet",npaNxxSet);
		 //end 5.6.5 changes.
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"existsTnSv query result:" +result);

		  // Changes are made in 5.6.5 release ( Sv activation rule)
		  // This flag is added to check whether NNSP can submit activate request without receiving releaseAckNotification (or release notification by onsp)
		  // or T2 concurrence notification.
		  Boolean spT2ExpirationFlag = Boolean.valueOf(false);
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getSpT2ExpirationFlag()))
		  {
			  spT2ExpirationFlag = Boolean.valueOf(true);
		  }
		  setContextValue("spT2ExpirationFlag",spT2ExpirationFlag);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of spT2ExpirationFlag : " + 
						"[" + spT2ExpirationFlag +"]");
		  }
		  
		  Boolean lastAltSPIDFlag = Boolean.valueOf(false);
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getLastAltSpidFlag()))
		  {
			  lastAltSPIDFlag = Boolean.valueOf(true);
		  }
		  setContextValue("lastAltSPIDFlag",lastAltSPIDFlag);
		  
		  // end changes for 5.6.5
      }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);
	
      }

      return true;

   }
    public static class ResultsetData {

        public long referenceKey=0;        
        public String spid=null;        
        public String onsp=null;        
        public String nnsp=null;        
        public long svid=0;
		public int regionId=0;
		public String portingTn=null;
		public Date createdDate=null;
		public String status=null; 
		public String causeCode=null;
		public Date nnspDueDate = null;
		public String businessTimerType= null;
		public Date objectCreationDate = null;
		public String lastRequestType= null;
		public String lastProcessing= null;
		//Changes are made in 5.6.5 release
		public Date onspDueDate = null;
		public String authorizationFlag = null;
		public String activateTimerExpirationFlag = null;
		// end 5.6.5 changes
        public ResultsetData ( long referenceKey, String spid, String onsp,
			String nnsp, long svid, int regionId, String portingTn,
			Date createdDate, String status, String causeCode, 
			Date nnspDueDate, String businessTimerType, Date objectCreationDate, String lastRequestType, String lastProcessing, Date onspDueDate, String authorizationFlag, String activateTimerExpirationFlag) 
			throws FrameworkException {

            this.referenceKey   = referenceKey;            
            this.spid   = spid;            
            this.onsp   = onsp;
            this.nnsp   = nnsp;
            this.svid     = svid;            
            this.regionId = regionId;
			this.portingTn = portingTn;
			this.createdDate = createdDate;
			this.status = status;	
			this.causeCode= causeCode;
			this.nnspDueDate= nnspDueDate;
			this.businessTimerType= businessTimerType;
			this.objectCreationDate = objectCreationDate;
			this.lastRequestType= lastRequestType;
			this.lastProcessing= lastProcessing;
			this.onspDueDate= onspDueDate;
			this.authorizationFlag= authorizationFlag;
			this.activateTimerExpirationFlag= activateTimerExpirationFlag;
        }
	 }


	public ArrayList getRangeTnList(String startTN, String endStation){

	   int start=0;
		  
	   int end=0;

	   ArrayList tnList = new ArrayList();

	   try
	   {	  
			start = Integer.parseInt(startTN.substring(
				SOAConstants.STARTTN_END_INDEX, 
								SOAConstants.TN_END_INDEX) );

			end = Integer.parseInt( endStation );
	  
	   }catch(NumberFormatException ex){
	  
				Debug.error("Could not parse start TN ["+startTN+"]: "+ex);			 
			}		  

	   String tnPrefix = startTN.substring( SOAConstants.TN_START_INDEX, 
											SOAConstants.STARTTN_END_INDEX);

	   tnList.add(startTN);

	   for(int suffix = start+1; suffix <= end; suffix++){

			tnList.add(tnPrefix + StringUtils.padNumber(suffix, 4, true, '0'));
	   }
	
	   return tnList;
   
   }

   public ArrayList getTnList(Value tn, Value startTN, Value endStation){		 

		 ArrayList tnList = new ArrayList();
		 ArrayList submittedTnList = new ArrayList();

		 if (startTN.toString().equals(""))
		 {		 	 
			 Value[] tnTokens = tn.split(";");;

			 String startTnValue = null;

			 String endTnValue = null;
		 
			 for (int i=0; i < tnTokens.length; i++)
			 {							
				if (tnTokens[i].length() > 12)
				{
					startTnValue = tnTokens[i].substring(0,12).toString();

					endTnValue = tnTokens[i].substring(13).toString();	
					
					tnList.addAll(getRangeTnList(startTnValue, endTnValue));
				}
				else
				{
					tnList.add(tnTokens[i].toString());					
				}
				submittedTnList.add(tnTokens[i].toString());
			 }
		 }

		 if (tn.toString().equals(""))
		 {
			 tnList.addAll(getRangeTnList(startTN.toString(),
				 endStation.toString()));
			 submittedTnList.add(startTN.toString()+"-"+endStation.toString());
			
			 
		 }
		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"getTnList method: " +tnList);
		 try
		 {
			Collections.sort(tnList);
			setContextValue("tnList",tnList);
			setContextValue("submittedTnList",submittedTnList);
			
			if(tnList.size() > 0){
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," setTnList in Contex, Size of Tn_list["+tnList.size()+"].");
			}
			else{
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," setTnList in Contex, Size of Tn_list [ 0 ].");
			}				
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"getSubmittedTnList method: " +submittedTnList);
		 }
		 catch (Exception e)
		 {
			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				 Debug.log(Debug.MSG_STATUS,"getTnList exception: " +e.toString());
		 }				 
		   			
		 return tnList;

	}


   public boolean triggerCreateSv(Value spid, Value tn, Value startTN, 
	   Value endStation){

	  Connection conn = null;

      PreparedStatement pstmt = null;

	  Statement stmt = null;
      
      ResultSet results = null;	  	  

	  ArrayList tnList = new ArrayList();
	  
	  TreeSet npaNnxList = new TreeSet();

	  TreeSet npaNxxXList = new TreeSet();

	  HashMap npaNxxXDashx = new HashMap();	  
	  
	  tnList = getTnList(tn, startTN, endStation);	  
	  
	  
	  
	  String tnNode = null;

	  String npaNxx = null;

	  String npaNxxX = null;	  

	  ArrayList npaNxxXTnList = new ArrayList();																
				
	  if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
		  Debug.log(Debug.MSG_DATA,"IntriggerCreateSv() ");
	  
	  if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
		  Debug.log(Debug.MSG_DATA,"getALLTNLIST from Context:"+ tnList);
	  
 	  for (int i=0; i < tnList.size(); i++)
	  {			
		tnNode = tnList.get(i).toString();		
							
		npaNxx = tnNode.substring( SOAConstants.TN_NPA_START_INDEX,
											  SOAConstants.TN_NPA_END_INDEX) +
				 tnNode.substring( SOAConstants.TN_NXX_START_INDEX,
											  SOAConstants.TN_NXX_END_INDEX);
							
		npaNnxList.add( npaNxx );

		npaNxxX = tnNode.substring( SOAConstants.TN_NPA_START_INDEX,
											SOAConstants.TN_NPA_END_INDEX) +
				  tnNode.substring( SOAConstants.TN_NXX_START_INDEX,
											SOAConstants.TN_NXX_END_INDEX) +
				  tnNode.substring( SOAConstants.TN_DASHX_START_INDEX,
											SOAConstants.TN_DASHX_END_INDEX);		
				
		npaNxxXList.add( npaNxxX );				
				
	 }

	 int k=0;
	 Iterator dashXIterator = npaNxxXList.iterator();

	 while (dashXIterator.hasNext())
	 {	
		 String tnVal = null;
		
		 String dash = (String)dashXIterator.next();

		 String dashX = null;

		 ArrayList nNXTnList = new ArrayList();		 
		
		 for (int m=0; m < tnList.size(); m++)
		 {
			 tnVal = tnList.get(m).toString();
			 
			 dashX = tnVal.substring( SOAConstants.TN_NPA_START_INDEX,
											SOAConstants.TN_NPA_END_INDEX) +
					  tnVal.substring( SOAConstants.TN_NXX_START_INDEX,
												SOAConstants.TN_NXX_END_INDEX) +
					  tnVal.substring( SOAConstants.TN_DASHX_START_INDEX,
												SOAConstants.TN_DASHX_END_INDEX);
			if (dash.equals(dashX))
			{
				nNXTnList.add(tnVal);
			}
				
		 }

		npaNxxXDashx.put(dash, nNXTnList);
		
		k++;
	 }
	
	 try{
			setContextValue("npaNnxList",npaNnxList);

			setContextValue("npaNxxXList",npaNxxXList);

			setContextValue("npaNxxXDashx",npaNxxXDashx);
			
			setContextValue("xsdSpid", spid);
		}
	 catch(Exception exception)
	 {
		 if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
			 Debug.log(Debug.ALL_ERRORS,exception.toString());
     }

	  try{

		 boolean result = false;

		 /*Get Connection from RuleContext so that it is shared 
		 by other functions.*/
		 conn = getDBConnection();

		 long dbEffectiveDate = 0;
 
		 String dbSpid = null;

		 String dbNpaNxx = null;
		 //Changes are made in 5.6.5 release (For canadian customer checking)
		 // getting regionId from SOA_NPA_NXX table.
		 String dbRegionId = null;

		 NpaNxxData npanxxRs = null;

		 ArrayList npanxxDataList = new ArrayList();

		 StringBuffer npaNxxValue = new StringBuffer();

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

		 String napnxxQuery ="SELECT NPA||NXX, SPID, EFFECTIVEDATE, REGIONID FROM " +
		 	"SOA_NPANXX WHERE (NPA|| NXX) in ("+npaNxxValue+") AND " +
		 	"STATUS = 'ok'";

		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"Executing napnxxQuery: "+napnxxQuery);

		 stmt = conn.createStatement();		 

		 results = stmt.executeQuery(napnxxQuery);

		 while( results.next() ){

			dbNpaNxx = results.getString(1);

			dbSpid = results.getString(2);

			dbEffectiveDate = results.getTimestamp(3).getTime(); 
			
			dbRegionId = results.getString(4);
			
			npanxxRs = new NpaNxxData( dbNpaNxx, dbSpid, dbEffectiveDate, dbRegionId);

			npanxxDataList.add( npanxxRs );			

		 }
		 
		 setContextValue("npanxxDataList",npanxxDataList);

      }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);
      }
	  
	  try{
		  
		 boolean result = false;

		 long dashEffectiveDate = 0;

		 String dashSpid = null;

		 String dashNpaNxxX = null;

		 String fnpaNxxX = null;

		 NpaNxxXData npanxxXRs = null;

		 ArrayList npanxxXDataList = new ArrayList();

		 ArrayList failedTNList = new ArrayList();

		 ArrayList failedNpaNxxXList = new ArrayList();

		 StringBuffer npaNxxXValue = new StringBuffer();

		 int i=0;
		 Iterator npanxxXIterator = npaNxxXList.iterator();

		 while (npanxxXIterator.hasNext())
		 {		
			npaNxxXValue.append("'");

			npaNxxXValue.append((String)npanxxXIterator.next());
			
			if ( i != npaNxxXList.size()-1)				
				npaNxxXValue.append("',");
			else
				npaNxxXValue.append("'");
			i++;
		 }

		 String napnxxXQuery ="SELECT NPA||NXX||DASHX, SPID, EFFECTIVEDATE" +
		 	" FROM SOA_NPANXX_X WHERE  (NPA|| NXX || DASHX) in " +
		 	"("+npaNxxXValue+") AND STATUS = 'ok'";

		 stmt = conn.createStatement();

		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"Executing npanxxXQuery"+napnxxXQuery);		 

		 results = stmt.executeQuery(napnxxXQuery);						 
		
		 while( results.next() ){		

			dashNpaNxxX = results.getString(1);

			dashSpid = results.getString(2);

			dashEffectiveDate = results.getTimestamp(3).getTime(); 

			npanxxXRs = new NpaNxxXData( dashNpaNxxX, dashSpid, 
			dashEffectiveDate);

			npanxxXDataList.add( npanxxXRs );						
		  }

		 setContextValue("npanxxXDataList",npanxxXDataList);		

      }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);
      }
	  try{

		  String nancflag = String.valueOf(NANCSupportFlag.getInstance(spid.toString()).getNanc399flag());
		  if(nancflag == null || nancflag.equals(""))
			nancflag="0";
		 
		 setContextValue("nanc399flag",nancflag);
		 
	  }catch(Exception exception){

		 logErrors(exception, exception.toString());

	  }	  

	  try{

		 String initSpid = spid.toString();

		 int tnCount =tnList.size();
		 
		 StringBuffer mainQuery = new StringBuffer();
		 
		 int i = 0;

		 while (i < tnCount)
		 {			
			int j = 1;

			StringBuffer tnValue = new StringBuffer();

			while (j <= 1000 && i <= tnCount-1 )
			{
				tnValue.append("'");
				tnValue.append(tnList.get(i));
				
				if ( j < 1000 && i != tnCount-1)				
					tnValue.append("',");
				else
					tnValue.append("'");

				i++;
				j++;
				
			}
			
			StringBuffer tnQuery = new StringBuffer(
		"(select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ onsp, nnsp, portingTN, " +
		"status, NNSPDUEDATE,ONSPDUEDATE,PORTINGTOORIGINAL,LASTREQUESTTYPE, LASTPROCESSING, LRN, LNPTYPE from " +
		"soa_subscription_version where (portingtn, spid, createddate)" +
		" in (select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ portingtn, spid, max(createddate) from " +
		"soa_subscription_version where portingtn in ("+tnValue+ " ) " +
		"AND spid = '"+initSpid+"' and STATUS in ( 'conflict','active'," +
		"'pending','sending', 'download-failed'," +
		"'download-failed-partial','disconnect-pending','cancel-pending','creating','NPACCreateFailure' )" +
		" group by portingtn, spid) and STATUS in ( 'conflict','active'," +
		"'pending','sending', 'download-failed','download-failed-partial'," +
		"'disconnect-pending','cancel-pending' ,'creating','NPACCreateFailure' ))");

			mainQuery.append(tnQuery);
			
			if (i <= tnCount-1)
			{
				mainQuery.append(" UNION ");
			}
					
		 }

		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"tnQuery in triggerCreateSv:"+mainQuery);
		
		stmt = conn.createStatement();		 

		 results = stmt.executeQuery(mainQuery.toString());

		 CreateSvData rsData = null;
		 ArrayList createSvRs = new ArrayList();

		 String onsp = null;
		 String nnsp = null;
		 String portingTN = null;
		 String status = null;
		 Date nnspdueDate = null;
		 Date onspdueDate = null;
		 String lastRequestType = null;
		 String lastProcessing = null;
		 String lrn = null;
		 int pto = 0;
		 String lnptype = null;
		 
		 while( results.next() ){					
						
			onsp = results.getString(1);      
			nnsp = results.getString(2);      
			portingTN = results.getString(3);
			status = results.getString(4);
			nnspdueDate = results.getTimestamp(5);
			onspdueDate = results.getTimestamp(6);
			pto = results.getInt(7);
			lastRequestType = results.getString(8);      
			lastProcessing = results.getString(9);
			lrn = results.getString(10);
			lnptype = results.getString(11);
			  
			rsData = new CreateSvData( onsp, nnsp, portingTN, status, 
			nnspdueDate, onspdueDate,lastRequestType , lastProcessing, pto, lrn, lnptype);
			rsData.setSpid(initSpid);
			
			createSvRs.add( rsData );
			
		  }
		  
		  setContextValue("createSvRs", createSvRs);
		 
      }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);

      }
      try
	  {
    	  Boolean altbidnanc436Value = null;
    	  Boolean alteultnanc436Value = null;
    	  Boolean alteulvnanc436Value = null;
			  
    	  if (NANCSupportFlag.getInstance(spid.toString()).getAltBidNanc436Flag()
					.equals("1")) {
				altbidnanc436Value = Boolean.valueOf(true);
			} else {
				altbidnanc436Value = Boolean.valueOf(false);
			}
			if (NANCSupportFlag.getInstance(spid.toString()).getAltEultNanc436Flag()
					.equals("1")) {
				alteultnanc436Value = Boolean.valueOf(true);
			} else {
				alteultnanc436Value = Boolean.valueOf(false);
			}
			if (NANCSupportFlag.getInstance(spid.toString()).getAltEulvNanc436Flag()
					.equals("1")) {
				alteulvnanc436Value = Boolean.valueOf(true);
			} else {
				alteulvnanc436Value = Boolean.valueOf(false);
			}    	  
		  
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of NANC 436 Flags[ALTBID,ALTEULT,ALTEULV]: " + 
						"[" + altbidnanc436Value + "," + alteultnanc436Value + ","+ alteulvnanc436Value + "]");
			
		  setContextValue("altbidnanc436flag",altbidnanc436Value);
		  setContextValue("alteultnanc436flag",alteultnanc436Value);
		  setContextValue("alteulvnanc436flag",alteulvnanc436Value);
		  
		  Integer pushDueDate = NANCSupportFlag.getInstance(spid.toString()).getPushduedate();
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : [" + pushDueDate +"]");
		  if(pushDueDate != null)
			  setContextValue("pushDueDate",pushDueDate);
		  
		  /* changes for DueDate adjust window flag for SOA5.9 */
		  
		  Integer adjustDueDate = NANCSupportFlag.getInstance(spid.toString()).getAdjustDueDateFlag();
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of ADJUSTDUEDATEFLAG : [" + adjustDueDate +"]");
		  if(adjustDueDate != null)			  
			  setContextValue("adjustDueDate",adjustDueDate);

		   
          // Changes for SimplePortIndicator flag, part of 5.6.5 release (NANC 441 req.)
		  Boolean simplePortIndicatorNANC441 = Boolean.valueOf(false);
		  if (("1".equals(NANCSupportFlag.getInstance(spid.toString())
					.getSimplePortIndicatorFlag()))
					|| ("2".equals(NANCSupportFlag.getInstance(spid.toString())
							.getSimplePortIndicatorFlag())))
		  {
			  simplePortIndicatorNANC441 = Boolean.valueOf(true);
		  }
		  setContextValue("simplePortIndicatorNANC441Flag",simplePortIndicatorNANC441);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of NANC 441 Flags[Simple Port Indicator]: " + 
						"[" + simplePortIndicatorNANC441 +"]");
		  }
		  
		  Boolean lastAltSPIDFlag = Boolean.valueOf(false);
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getLastAltSpidFlag()))
		  {
			  lastAltSPIDFlag = Boolean.valueOf(true);
		  }
		  setContextValue("lastAltSPIDFlag",lastAltSPIDFlag);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of lastAltSPIDFlag : " + 
						"[" + lastAltSPIDFlag +"]");
		  }
		  
		  Boolean pseudoLrnFlag = Boolean.valueOf(false);
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getPseudoLrnFlag()))
		  {
			  pseudoLrnFlag = Boolean.valueOf(true);
		  }
		  setContextValue("pseudoLrnFlag",pseudoLrnFlag);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of pseudoLrnFlag : " + 
						"[" + pseudoLrnFlag +"]");
		  }	
		  
		  // end changes for 5.6.5
          try
          {
			  // query to check whether SPID belongs to US & CN regions.
	          pstmt = conn
						.prepareStatement(SOAQueryConstants.GET_SPID_BELONG_TO_US_CN_QUERY);
	          
	          pstmt.setString(1, spid.toString());
	          
	          results = pstmt.executeQuery();
	          int spidRegionCount = 0;
	          while(results.next()){	          	
	        	  spidRegionCount = results.getInt(1);	          	
	          }
	          if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
					Debug.log( Debug.MSG_STATUS, "Spid belongs to no of regions  : "+ spidRegionCount );
	          } 
	          //If spid belongs to more then 1 region including CN region.
	          //If spidRegionCount is 1 means it belongs to CN region only.
	          //It also checking SimplePortindicator flag is on for a spid or not.
	          Boolean spidBelongsToUSandCN = Boolean.valueOf(false);
	          if(spidRegionCount > 1 && simplePortIndicatorNANC441.booleanValue())
	          {
	        	  spidBelongsToUSandCN = Boolean.valueOf(true);
	    	  }
	          setContextValue("spidBelongsToUS_CNandSPIon",spidBelongsToUSandCN);
	          if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
    		  {
    				Debug.log(Debug.MSG_STATUS,"The value of spidBelongsToUS_CNandSPIon : " + 
    						"[" + spidBelongsToUSandCN +"]");
    		  }
          }catch(Exception exception){

        	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
        		  Debug.log(Debug.ALL_ERRORS,exception.toString());
             
          }finally{
      		
      		close(results, pstmt);

            }
          
	  }
      catch(Exception exception)
      {
    	  logErrors(exception, exception.toString());
      }
    return true;
   }


   public boolean isOldSPActiveWithSpid(Value OldSP, Value tn, Value startTN, 
	   Value endStation){

	  Connection conn = null;

	  Statement stmt = null;
      
      ResultSet results = null;

	  boolean result = false;

	  ArrayList tnList = getTnList(tn, startTN, endStation);  
	  
	  try{

		 /*Get Connection from RuleContext so that it is shared 
		 by other functions.*/
		 conn = getDBConnection();

		 String onsp = OldSP.toString();

		 int tnCount =tnList.size();
		 
		 StringBuffer mainQuery = new StringBuffer();

		 StringBuffer errorMessage = new StringBuffer();
		 
		 int i = 0;

		 while (i < tnCount)
		 {			
			int j = 1;

			StringBuffer tnValue = new StringBuffer();

			while (j <= 1000 && i <= tnCount-1 )
			{
				tnValue.append("'");
				tnValue.append(tnList.get(i));
				
				if ( j < 1000 && i != tnCount-1)				
					tnValue.append("',");
				else
					tnValue.append("'");

				i++;
				j++;
				
			}
			
			StringBuffer tnQuery = new StringBuffer(
		"(select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ portingTN from soa_subscription_version where (portingtn, spid, " +
		"createddate) in (select portingtn, spid, max(createddate) from " +
		"soa_subscription_version where portingtn in ("+tnValue+ " ) " +
		"AND STATUS ='active' group by portingtn, spid) and " +
		"status = 'active' and nnsp != '"+onsp+"')");			

			mainQuery.append(tnQuery);
			
			if (i <= tnCount-1)
			{
				mainQuery.append(" UNION ");
			}
					
		 }

		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"isOldSPActiveWithSpid:"+mainQuery);

		 stmt = conn.createStatement();		 

		 results = stmt.executeQuery(mainQuery.toString());

		 ArrayList createSvRs = new ArrayList();

		 String portingTN = null;
		 
			 while( results.next() ){
									
				portingTN = results.getString(1);
				
				if (!createSvRs.contains(portingTN))
				{
					createSvRs.add( portingTN );
					errorMessage.append("["+portingTN+"] ");
				}
	
				addFailedTN(portingTN);			
				
			  }

		  if (createSvRs.isEmpty())
			result = true;
		  else
			setDynamicErrorMessage(errorMessage.toString());
		
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  Debug.log(Debug.MSG_STATUS,"Result returned for isOldSPActiveWithSpid: "+result);		
		  

      }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);

      }

      return result;

   }

   /**
    * This function check the status of existing TN,
    * if TN is already belong to NNSP then BR will raise.
    * 
    * @param NewSP , New Service Provider from request.
    * @param tn, TN from request
    * @param startTN, Start TN from request
    * @param endStation, endStation from request.
    * 
    * @return, ture if successful, otherwise false.
    * 
    */
   public boolean isTNAlreadyBelongNewSP(Value NewSP, Value tn, Value startTN, 
		   Value endStation){

		  boolean result = true;		  
		  String nnsp = NewSP.toString();		   
		  try
		  {						 			
			 StringBuffer errorMessage = new StringBuffer();			 						 
			 ArrayList createSvList =(ArrayList)getContextValue("createSvRs");			 
			 ArrayList createSvRs = new ArrayList();
			 String portingTN = null;			 
			 if (createSvList != null)
			 {
				 for (int i=0;i < createSvList.size();i++ )
				 {
					CreateSvData createSvData =(CreateSvData)createSvList.get(i);	
					String status = createSvData.status;					
					Date nnspduedate = createSvData.nnspDate;
					portingTN = createSvData.portingTn;	
					String nnspDB = createSvData.nnsp;
					String spidDB = createSvData.getSpid();					
					if(spidDB.equals(nnspDB) && spidDB.equals(nnsp))
					{						
						if ((status.equals("active") || status.equals("cancel-pending") || status.equals("download-failed-partial") || status.equals("download-failed")) || 
									((status.equals("pending")|| status.equals("conflict")) && nnspduedate!=null))
						{							
																				
							if (!createSvRs.contains(portingTN))
							{
								createSvRs.add( portingTN );
								errorMessage.append("["+portingTN+"] ");
							}	
							
							addFailedTN(portingTN);	
							
							result = false;
								
						}
					}
									   
				}	
			 }

			if( !result )
				setDynamicErrorMessage(errorMessage.toString());
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"Result returned for isTNAlreadyBelongNewSP: "+result);		


		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,exception.toString());
	         
	    }
		  
	    return result;

	  }  
  
   
   
	public boolean triggerReleaseSv(Value spid, Value tn, Value startTN, 
	Value endStation, Value newSPDueDate){

	  Connection conn = null;

	  Statement stmt = null;
	  
	  PreparedStatement pstmt = null;
      
      ResultSet results = null;	  	  

	  ArrayList tnList = new ArrayList();

	  ArrayList npaNxxTnList = new ArrayList();
	  
	  TreeSet npaNnxList = new TreeSet();

	  HashMap npaNxxMap = new HashMap();
	  
	  tnList = getTnList(tn, startTN, endStation);	  
	  
	  String tnNode = null;

	  String npaNxx = null;

	  boolean npaNxxExist = false;

	  StringBuffer npaNxxValue = new StringBuffer();

	  Date effectiveDate = null;
	  
	  SimpleDateFormat dateFormat = 
	  new SimpleDateFormat( "MM-dd-yyyy-hhmmssa" );

		try {				
				effectiveDate =dateFormat.parse(newSPDueDate.toString());			
			} 
		catch (ParseException ex) {				

				Debug.error("Could not parse dueDate ["+newSPDueDate+"]: "+ex);

				return false;
			}
	  long eDateTime = effectiveDate.getTime();
				
 	  for (int i=0; i < tnList.size(); i++)
	  {			
		tnNode = tnList.get(i).toString();		
							
		npaNxx = tnNode.substring( SOAConstants.TN_NPA_START_INDEX,
											  SOAConstants.TN_NPA_END_INDEX) +
				 tnNode.substring( SOAConstants.TN_NXX_START_INDEX,
											  SOAConstants.TN_NXX_END_INDEX);
							
		npaNnxList.add( npaNxx );				

	 }
	 

	 int k=0;
	 Iterator nnIterator = npaNnxList.iterator();

	 while (nnIterator.hasNext())
	 {	
		 String tnVal = null;
		
		 String dashXIterator = (String)nnIterator.next();

		 String dashX = null;

		 ArrayList nNXTnList = new ArrayList();
		
		 for (int m=0; m < tnList.size(); m++)
		 {
			 tnVal = tnList.get(m).toString();
			 
			 dashX = tnVal.substring( SOAConstants.TN_NPA_START_INDEX,
											  SOAConstants.TN_NPA_END_INDEX) +
					 tnVal.substring( SOAConstants.TN_NXX_START_INDEX,
											  SOAConstants.TN_NXX_END_INDEX);
			if (dashXIterator.equals(dashX))
			{
				nNXTnList.add(tnVal);
			}
				
		 }

		npaNxxMap.put(dashXIterator, nNXTnList);
		
		k++;
	 }

	  try{

		 setContextValue("xsdSpid", spid);
		 boolean result = false;

		 boolean tnResult = false;

		 boolean dateResult = false;		 

		 long dbEffectiveDate = 0;
 
		 String dbSpid = null;

		 String dbNpaNxx = null;

		 // Changes are made in 5.6.5 release (For canadian customer checking)
		 // getting regionId from SOA_NPA_NXX table.
		 String dbRegionId = null;
		 
		 NpaNxxData npanxxRs = null;

		 ArrayList npanxxDataList = new ArrayList();

		 ArrayList npaNnxDList = new ArrayList();

		 ArrayList fdueDateTNList = new ArrayList();

		 //changes are made in 5.6.5
		 //list of NpaNxx belong to canadian region
		 ArrayList cnNpaNxxList = new ArrayList();
		 ArrayList failedcnNpaNxxList = new ArrayList();
		 boolean isCNNpaNXX = false;
		 
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

		 String napnxxQuery ="SELECT NPA||NXX, SPID, EFFECTIVEDATE, REGIONID FROM " +
		 "SOA_NPANXX WHERE (NPA|| NXX) in ("+npaNxxValue+") AND STATUS = 'ok'";

		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"Executing query napnxxQuery: "+
		 napnxxQuery);

		 // Get Connection from RuleContext so that it is shared 
		 //by other functions.
		 conn = getDBConnection();

		 stmt = conn.createStatement();		 

		 results = stmt.executeQuery(napnxxQuery);

		 while( results.next() ){

			dbNpaNxx = results.getString(1);

			dbSpid = results.getString(2);

			dbEffectiveDate = results.getTimestamp(3).getTime(); 

			dbRegionId = results.getString(4);
			
			npaNnxList.remove(dbNpaNxx);
			
			if (dbEffectiveDate > eDateTime )
			{
				npaNnxDList.add(dbNpaNxx);
			}
			//changes are made in 5.6.5
			//checking canadian region
			if(SOAConstants.CANADA_REGION.equals(dbRegionId))
			{
				cnNpaNxxList.add(dbNpaNxx);
			}
			
			npanxxRs = new NpaNxxData( dbNpaNxx, dbSpid, dbEffectiveDate, dbRegionId);

			npanxxDataList.add( npanxxRs );			

		 }
		 
		 setContextValue("npanxxDataList",npanxxDataList);

		 tnResult = npaNnxList.isEmpty();

		 setContextValue("npaNxxPresent",new Boolean(tnResult));

		 if (!tnResult)
		 {
			Iterator tnIterator = npaNnxList.iterator();
			String fnpaNxx = null;
			ArrayList failedNpaNxxList = new ArrayList();
			ArrayList failedTNList = new ArrayList();
			while (tnIterator.hasNext())
			{
				fnpaNxx = (String)tnIterator.next();

				failedNpaNxxList = (ArrayList)npaNxxMap.get(fnpaNxx);

				failedTNList.addAll(failedNpaNxxList);
				
			}
			 setContextValue("fSpidTNList",failedTNList);		 
		 }
		 
		 dateResult = npaNnxDList.isEmpty();

		 setContextValue("npanxxDatePresent",new Boolean(dateResult));

		 if (!dateResult)
		 {
			Iterator dateIterator = npaNnxDList.iterator();
			String fnpaNxx = null;
			ArrayList dbNpaNxxList = new ArrayList();
			while (dateIterator.hasNext())
			{
				fnpaNxx = (String)dateIterator.next();

				dbNpaNxxList = (ArrayList)npaNxxMap.get(fnpaNxx);

				fdueDateTNList.addAll(dbNpaNxxList);
				
			}
			 setContextValue("fduedateTNList",fdueDateTNList);			 
		 }

		 //changes are made in 5.6.5
		 // checks for canada NpanNxx region list
		 isCNNpaNXX = cnNpaNxxList.isEmpty();

		 setContextValue("CN_NpaNxxPresent",new Boolean(isCNNpaNXX));

		 if (!isCNNpaNXX)
		 {
			Iterator cnNpaNxxIterator = cnNpaNxxList.iterator();
			String cnNpaNxx = null;
			ArrayList fcnNpaNxxList = new ArrayList();
			while (cnNpaNxxIterator.hasNext())
			{
				cnNpaNxx = (String)cnNpaNxxIterator.next();

				fcnNpaNxxList = (ArrayList)npaNxxMap.get(cnNpaNxx);

				failedcnNpaNxxList.addAll(fcnNpaNxxList);
				
			}
			 setContextValue("failed_CN_NpaNxxList",failedcnNpaNxxList);			 
		 }
		 // end 5.6.5 changes
      }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);
      }	 


	  try{

		 String initSpid = spid.toString();

		 int tnCount =tnList.size();
		 
		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"tnCount in triggerReleaseSv: "+tnCount);
		 
		 StringBuffer mainQuery = new StringBuffer();
		 
		 int i = 0;

		 while (i < tnCount)
		 {			
			int j = 1;

			StringBuffer tnValue = new StringBuffer();

			while (j <= 1000 && i <= tnCount-1 )
			{
				tnValue.append("'");
				tnValue.append(tnList.get(i));
				
				if ( j < 1000 && i != tnCount-1)				
					tnValue.append("',");
				else
					tnValue.append("'");

				i++;
				j++;
				
			}
			
			StringBuffer tnQuery = new StringBuffer(
"(select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ onsp, nnsp, portingTN, status," +
" NNSPDUEDATE,ONSPDUEDATE, LASTREQUESTTYPE, LASTPROCESSING, LRN, PORTINGTOORIGINAL, LNPTYPE from soa_subscription_version where (portingtn," +
" spid, createddate) in (select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ portingtn, spid, max(createddate) from " +
"soa_subscription_version where portingtn in ("+tnValue+ " ) AND spid = " +
"'"+initSpid+"' and STATUS in ( 'conflict','active','pending','sending'," +
" 'download-failed','download-failed-partial','disconnect-pending'," +
"'cancel-pending','creating','NPACCreateFailure') group by portingtn, spid)" +
" and STATUS in ( 'conflict','active','pending','sending', " +
"'download-failed','download-failed-partial','disconnect-pending'," +
"'cancel-pending','creating','NPACCreateFailure'))");

			mainQuery.append(tnQuery);
		 
			if (i <= tnCount-1)
			{
				mainQuery.append(" UNION ");
			}
					
		 }
		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"Executing query : " + mainQuery);
		 
		 stmt = conn.createStatement();		 

		 results = stmt.executeQuery(mainQuery.toString());

		 CreateSvData rsData = null;
		 ArrayList createSvRs = new ArrayList();

		 String onsp = null;
		 String nnsp = null;
		 String portingTN = null;
		 String status = null;
		 Date nnspdueDate = null;
		 Date onspdueDate = null;
		 String lastRequestType = null;
		 String lastProcessing = null;
		 int pto = 0;
		 String lrn = null;
		 String lnptype = null;
		 
		 while( results.next() ){					
						
			onsp = results.getString(1);      
			nnsp = results.getString(2);      
			portingTN = results.getString(3);
			status = results.getString(4);
			nnspdueDate = results.getTimestamp(5);
			onspdueDate = results.getTimestamp(6);
			lastRequestType = results.getString(7);      
			lastProcessing = results.getString(8);
			pto = results.getInt(9);
			lrn = results.getString(10);
			lnptype = results.getString(11);
			  
			rsData = new CreateSvData( onsp, nnsp, portingTN, status, 
			nnspdueDate, onspdueDate,lastRequestType,lastProcessing, pto, lrn, lnptype);
			
			createSvRs.add( rsData );
			
		  }
		 setContextValue("createSvRs", createSvRs);
			 
		 
		 Integer pushDueDate = NANCSupportFlag.getInstance(spid.toString()).getPushduedate();
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : [" + pushDueDate +"]");
		  if(pushDueDate != null)
			  setContextValue("pushDueDate",pushDueDate);
		  
		  /* changes for DueDate adjust window flag for SOA5.9 */
		  
		  Integer adjustDueDate = NANCSupportFlag.getInstance(spid.toString()).getAdjustDueDateFlag();
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of ADJUSTDUEDATEFLAG : [" + adjustDueDate +"]");
		  if(adjustDueDate != null)			  
			  setContextValue("adjustDueDate",adjustDueDate);

	  }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);

      }
      try{
		  // Changes for SimplePortIndicator flag, part of 5.6.5 release (NANC 441 req.)
		  Boolean simplePortIndicatorNANC441 = Boolean.valueOf(false);
		  if (("1".equals(NANCSupportFlag.getInstance(spid.toString())
					.getSimplePortIndicatorFlag()))
					|| ("2".equals(NANCSupportFlag.getInstance(spid.toString())
							.getSimplePortIndicatorFlag())))
		  {
			  simplePortIndicatorNANC441 = Boolean.valueOf(true);
		  }
		  setContextValue("simplePortIndicatorNANC441Flag",simplePortIndicatorNANC441);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of NANC 441 Flags[Simple Port Indicator]: " + 
						"[" + simplePortIndicatorNANC441 +"]");
		  }
	      // query to check whether SPID belongs to US & CN regions.
		  pstmt = conn
						.prepareStatement(SOAQueryConstants.GET_SPID_BELONG_TO_US_CN_QUERY);
		          
		  pstmt.setString(1, spid.toString());
		          
		  results = pstmt.executeQuery();
		  int spidRegionCount = 0;
		  while(results.next()){	          	
		      	  spidRegionCount = results.getInt(1);	          	
		  }
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
				Debug.log( Debug.MSG_STATUS, "Spid belongs to no of regions  : "+ spidRegionCount );
		   } 
		   //If spid belongs to more then 1 region including CN region.
		   //If spidRegionCount is 1 means it belongs to CN region only.
		   //It also checking SimplePortindicator flag is on for a spid or not.
		   Boolean spidBelongsToUSandCN = Boolean.valueOf(false);
		   if(spidRegionCount > 1 && simplePortIndicatorNANC441.booleanValue())
		   {
		       	  spidBelongsToUSandCN = Boolean.valueOf(true);
		   }
		   setContextValue("spidBelongsToUS_CNandSPIon",spidBelongsToUSandCN);
		   if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
	       {
	    	   Debug.log(Debug.MSG_STATUS,"The value of spidBelongsToUS_CNandSPIon : " + 
	    					"[" + spidBelongsToUSandCN +"]");
	        }
	        //  end changes for 5.6.5
	      }catch(Exception exception){
	
	    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
	    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
	         
	      }finally{
			
			close(results, pstmt);
	
	      }
 
	      return true;
   }


	public boolean triggerModifySv(Value spid, Value tn, Value startTN, 
	Value endStation, Value referenceKey, Value rangeId){

	  Connection conn = null;

      PreparedStatement pstmt = null;

	  Statement stmt = null;
      
      ResultSet results = null;	  	  

	  ArrayList tnList = new ArrayList();
	  
	  ArrayList refKeyList = new ArrayList();
	  
	  String initSpid = spid.toString();
	  
	  String refKey = referenceKey.toString();
	  
	  String RangeId = rangeId.toString();
	  
	  TreeSet npaNnxList = new TreeSet();

	  TreeSet npaNxxXList = new TreeSet();

	  HashMap npaNxxXDashx = new HashMap();	  
	  
	  tnList = getTnList(tn, startTN, endStation);
	  
	  	
	  if(refKey != null && !refKey.equals("")){
			StringBuffer refKeyBuffer = new StringBuffer();
			StringTokenizer st = new StringTokenizer(refKey, SOAConstants.SVID_SEPARATOR);
			while(st.hasMoreTokens()) {
				String individualRefKey = st.nextToken();
				if(individualRefKey != null && !individualRefKey.equals("")){
	            refKeyBuffer.append(individualRefKey);
				refKeyBuffer.append(",");
				refKeyList.add(individualRefKey);
				}
			}
			refKey = refKeyBuffer.toString();
			if(refKey != null && refKey.length()>0)
				refKey = refKey.substring(0, refKey.length()-1);     //remove the last comma
		}
		
	   if(RangeId != null && !RangeId.equals("")){
			StringBuffer rangeIdBuffer = new StringBuffer();
            StringTokenizer st = new StringTokenizer(RangeId, SOAConstants.SVID_SEPARATOR);
			while(st.hasMoreTokens()) {
				String individualRangeKey = st.nextToken();
				if(individualRangeKey != null && !individualRangeKey.equals("")){
				rangeIdBuffer.append(individualRangeKey);
				rangeIdBuffer.append(",");
				}
	         }
			RangeId = rangeIdBuffer.toString();
			if(RangeId != null && RangeId.length()>0)
				RangeId = RangeId.substring(0, RangeId.length()-1);     //remove the last comma
	    }
		
	   if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		   Debug.log(Debug.MSG_STATUS,"tnList in triggerModifySv: "+tnList);

	  ArrayList indTnList =new ArrayList();

		if (existsValue("tnList"))
		{
			indTnList =(ArrayList)getContextValue("tnList");
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"triggerModifySv tnList"+indTnList);			
		}
	  
	  String tnNode = null;

	  String npaNxx = null;

	  String npaNxxX = null;	  

	  ArrayList npaNxxXTnList = new ArrayList();																
				
 	  for (int i=0; i < tnList.size(); i++)
	  {			
		tnNode = tnList.get(i).toString();		
							
		npaNxx = tnNode.substring( SOAConstants.TN_NPA_START_INDEX,
											  SOAConstants.TN_NPA_END_INDEX) +
				 tnNode.substring( SOAConstants.TN_NXX_START_INDEX,
											  SOAConstants.TN_NXX_END_INDEX);
							
		npaNnxList.add( npaNxx );

		npaNxxX = tnNode.substring( SOAConstants.TN_NPA_START_INDEX,
											SOAConstants.TN_NPA_END_INDEX) +
				  tnNode.substring( SOAConstants.TN_NXX_START_INDEX,
											SOAConstants.TN_NXX_END_INDEX) +
				  tnNode.substring( SOAConstants.TN_DASHX_START_INDEX,
											SOAConstants.TN_DASHX_END_INDEX);		
				
		npaNxxXList.add( npaNxxX );		
				
	 }


	 int k=0;
	 Iterator dashXIterator = npaNxxXList.iterator();

	 while (dashXIterator.hasNext())
	 {	
		 String tnVal = null;
		
		 String dash = (String)dashXIterator.next();

		 String dashX = null;

		 ArrayList nNXTnList = new ArrayList();
		
		 for (int m=0; m < tnList.size(); m++)
		 {
			 tnVal = tnList.get(m).toString();
			 
			 dashX = tnVal.substring( SOAConstants.TN_NPA_START_INDEX,
											SOAConstants.TN_NPA_END_INDEX) +
					  tnVal.substring( SOAConstants.TN_NXX_START_INDEX,
												SOAConstants.TN_NXX_END_INDEX) +
					  tnVal.substring( SOAConstants.TN_DASHX_START_INDEX,
												SOAConstants.TN_DASHX_END_INDEX);
			if (dash.equals(dashX))
			{
				nNXTnList.add(tnVal);
			}
				
		 }

		npaNxxXDashx.put(dash, nNXTnList);
		
		k++;
	 }

	 try{
			setContextValue("npaNnxList",npaNnxList);

			setContextValue("npaNxxXList",npaNxxXList);

			setContextValue("npaNxxXDashx",npaNxxXDashx);
			
			setContextValue("xsdSpid", spid);
		}
	 catch(Exception exception)
	 {
		 if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
			 Debug.log(Debug.ALL_ERRORS,exception.toString());
     }

	  try{

		 boolean result = false;

		 /*Get Connection from RuleContext so that it is shared 
		 by other functions.*/
		 conn = getDBConnection();

		 long dbEffectiveDate = 0;
 
		 String dbSpid = null;

		 String dbNpaNxx = null;
		 //Changes are made in 5.6.5 release (For canadian customer checking)
		 // getting regionId from SOA_NPA_NXX table.
		 String dbRegionId = null;

		 NpaNxxData npanxxRs = null;

		 ArrayList npanxxDataList = new ArrayList();

		 StringBuffer npaNxxValue = new StringBuffer();

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

		 String napnxxQuery ="SELECT NPA||NXX, SPID, EFFECTIVEDATE, REGIONID FROM " +
		 	"SOA_NPANXX WHERE (NPA|| NXX) in ("+npaNxxValue+") AND " +
		 	"STATUS = 'ok'";

		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"Executing napnxxQuery: "+napnxxQuery);

		 stmt = conn.createStatement();		 

		 results = stmt.executeQuery(napnxxQuery);

		 while( results.next() ){

			dbNpaNxx = results.getString(1);

			dbSpid = results.getString(2);

			dbEffectiveDate = results.getTimestamp(3).getTime(); 
			
			dbRegionId = results.getString(4);
			
			npanxxRs = new NpaNxxData( dbNpaNxx, dbSpid, dbEffectiveDate, dbRegionId);

			npanxxDataList.add( npanxxRs );			

		 }
		 
		 setContextValue("npanxxDataList",npanxxDataList);

      }catch(Exception exception){

    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);
      }

	 
	  try{

		  String nancflag = String.valueOf(NANCSupportFlag.getInstance(initSpid).getNanc399flag());
		  if(nancflag == null || nancflag.equals(""))
			nancflag="0";
		 
		 setContextValue("nanc399flag",nancflag);
		 
	  }catch(Exception exception){

		 logErrors(exception, exception.toString());

	  }	  
	  try{		 

		 int tnCount =tnList.size();
		 
		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"tnCount in triggerModifySv: "+tnCount);
		 
		 StringBuffer mainQuery = new StringBuffer();
		 
		 int i = 0;
		 
		 while (i < tnCount)
		 {			
			int j = 1;

			StringBuffer tnValue = new StringBuffer();
			StringBuffer  refKeyValue = new StringBuffer();
			int refKeySize = refKeyList.size();
			boolean refKeyFlag = false;
			
			while (j <= 1000 && i <= tnCount-1 )
			{
				tnValue.append("'");
				tnValue.append(tnList.get(i));
				
				if(refKeySize > 0 ){
				
					refKeyValue.append("'");
					refKeyValue.append(refKeyList.get(i));
					refKeyFlag = true;
					refKeySize--;
					
				}
				
				if ( j < 1000 && i != tnCount-1){				
					tnValue.append("',");
					if(refKeySize > 0 ){
						refKeyValue.append("',");
					}
				}
				else{
					tnValue.append("'");
					if(refKeySize >= 0 && refKeyFlag){
						refKeyValue.append("'");
						refKeyFlag = false;
					}
				}

				i++;
				j++;
				
			}
			
			StringBuffer tnQuery = new StringBuffer();
			
			tnQuery.append("(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ");
			tnQuery.append(SOAConstants.ONSP_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.NNSP_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.PORTINGTN_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.STATUS_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.NNSPDUEDATE_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.ONSPDUEDATE_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.PORTINGTOORIGINAL_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.ALTERNATIVESPID_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.SUBDOMAIN_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.CAUSECODE_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.LNPTYPE_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.LRN_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.ONSP_SIMPLEPORTINDICATOR_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.NNSP_SIMPLEPORTINDICATOR_COL);
			tnQuery.append(" FROM SOA_SUBSCRIPTION_VERSION WHERE (PORTINGTN, SPID, CREATEDDATE) IN (");
			tnQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ");
			tnQuery.append(SOAConstants.PORTINGTN_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.SPID_COL);
			tnQuery.append(", MAX(CREATEDDATE) FROM SOA_SUBSCRIPTION_VERSION WHERE ");
			tnQuery.append(SOAConstants.PORTINGTN_COL);
			tnQuery.append(" IN ("+tnValue+" )");
			if((refKey != null && !refKey.equals("")) &&  (RangeId == null || RangeId.equals(""))){
				tnQuery.append("AND REFERENCEKEY");
				tnQuery.append(" IN ("+refKeyValue+" ) ");
			}
			if((RangeId != null && !RangeId.equals("")) && (refKey == null || refKey.equals(""))){
				tnQuery.append("AND RANGEID");
				tnQuery.append(" IN ("+RangeId+" ) ");
			}
			if((RangeId != null && !RangeId.equals("")) && (refKey != null && !refKey.equals(""))){
				tnQuery.append("AND ( RANGEID");
				tnQuery.append(" IN ("+RangeId+" ) ");
				tnQuery.append("OR REFERENCEKEY");
				tnQuery.append(" IN ("+refKeyValue+" )) ");
			}
			tnQuery.append(" AND SPID = '"+initSpid+"' AND ");
			tnQuery.append(SOAConstants.STATUS_COL);
			tnQuery.append(" IN('");
			tnQuery.append(SOAConstants.CONFLICT_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.PENDING_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.ACTIVE_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.SENDING_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.DOWNLOAD_FAILED_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.CANCEL_PENDING_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.DISCONNECT_PENDING_STATUS);
			tnQuery.append("')");
			tnQuery.append(" GROUP BY ");
			tnQuery.append(SOAConstants.PORTINGTN_COL);
			tnQuery.append(", ");
			tnQuery.append(SOAConstants.SPID_COL);
			tnQuery.append(") ");
			tnQuery.append("AND ");
			tnQuery.append(SOAConstants.STATUS_COL);
			tnQuery.append(" IN('");
			tnQuery.append(SOAConstants.CONFLICT_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.PENDING_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.ACTIVE_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.SENDING_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.DOWNLOAD_FAILED_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.CANCEL_PENDING_STATUS);
			tnQuery.append("', '");
			tnQuery.append(SOAConstants.DISCONNECT_PENDING_STATUS);
			tnQuery.append("'))");
					
			mainQuery.append(tnQuery);
			
			if (i <= tnCount-1)
			{
				mainQuery.append(" UNION ");
			}
			
		 }
		 
		 mainQuery.append(" ORDER BY ");
		 mainQuery.append(SOAConstants.PORTINGTOORIGINAL_COL);
			
		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"mainQuery in triggerModifySv: "+mainQuery);
		 
		 conn = getDBConnection();
		 stmt = conn.createStatement();

		 results = stmt.executeQuery(mainQuery.toString());

		 ModifySvData rsData = null;
		 ArrayList modifySvRs = new ArrayList();
		 
		 CreateTnOnspNnspData tnOnspData = null;
		 ArrayList<CreateTnOnspNnspData> tnOnspDataList = new ArrayList<CreateTnOnspNnspData>();

		 String onsp = null;
		 String nnsp = null;
		 String portingTN = null;
		 String status = null;
		 Date nnspdueDate = null;
		 Date onspdueDate = null;
		 String altspid = null;
		 String subdomain = null;
		 String causecode = null;
		 String onspSimplePortIndicator = null;
		 String nnspSimplePortIndicator = null;
		 String lnptype = null;
		 String lrn = null;
		 

		 boolean pto = false;

		 while( results.next() ){
			 
			onsp = results.getString(1);      
			nnsp = results.getString(2);      
			portingTN = results.getString(3);
			status = results.getString(4);
			nnspdueDate = results.getTimestamp(5);
			onspdueDate = results.getTimestamp(6);
			pto = results.getBoolean(7);
			altspid = results.getString(8);
			subdomain = results.getString(9);
			causecode = results.getString(10);
			lnptype = results.getString(11);
			lrn = results.getString(12);
			onspSimplePortIndicator = results.getString(13);
			nnspSimplePortIndicator = results.getString(14);
			
			rsData = new ModifySvData( onsp, nnsp, portingTN, status, 
			nnspdueDate, onspdueDate, pto, altspid, subdomain , causecode , lrn, lnptype, onspSimplePortIndicator, nnspSimplePortIndicator );
			
			modifySvRs.add( rsData );	
			
			tnOnspData = new CreateTnOnspNnspData(portingTN,onsp,nnsp);
			
			tnOnspDataList.add(tnOnspData);
		  }

		  setContextValue("modifySvRs", modifySvRs);
		  setContextValue("tnOnspDataList", tnOnspDataList);
		  setContextValue("initSpid", initSpid);
		  
		  Integer pushDueDate = NANCSupportFlag.getInstance(spid.toString()).getPushduedate();
		  
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : [" + pushDueDate +"]");
		  
		  if(pushDueDate != null)
			  setContextValue("pushDueDate",pushDueDate);
		  
      }catch(Exception exception){
    	  
    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
         
      }finally{
		
		close(results, stmt);

      }
      try
	  {
		  
    	  Boolean altbidnanc436Value = null;
    	  Boolean alteultnanc436Value = null;
    	  Boolean alteulvnanc436Value = null;
			  
    	  if (NANCSupportFlag.getInstance(initSpid).getAltBidNanc436Flag()
					.equals("1")) {
				altbidnanc436Value = Boolean.valueOf(true);
			} else {
				altbidnanc436Value = Boolean.valueOf(false);
			}
			if (NANCSupportFlag.getInstance(initSpid).getAltEultNanc436Flag()
					.equals("1")) {
				alteultnanc436Value = Boolean.valueOf(true);
			} else {
				alteultnanc436Value = Boolean.valueOf(false);
			}
			if (NANCSupportFlag.getInstance(initSpid).getAltEulvNanc436Flag()
					.equals("1")) {
				alteulvnanc436Value = Boolean.valueOf(true);
			} else {
				alteulvnanc436Value = Boolean.valueOf(false);
			}    	  
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"The value of NANC 436 Flags[ALTBID,ALTEULT,ALTEULV]: " + 
						"[" + altbidnanc436Value + "," + alteultnanc436Value + ","+ alteulvnanc436Value + "]");
			
		  setContextValue("altbidnanc436flag",altbidnanc436Value);
		  setContextValue("alteultnanc436flag",alteultnanc436Value);
		  setContextValue("alteulvnanc436flag",alteulvnanc436Value);
		  
		  // Changes for SimplePortIndicator flag, part of 5.6.5 release (NANC 441 req.)
		  Boolean simplePortIndicatorNANC441 = Boolean.valueOf(false);
		  if (("1".equals(NANCSupportFlag.getInstance(spid.toString())
					.getSimplePortIndicatorFlag()))
					|| ("2".equals(NANCSupportFlag.getInstance(spid.toString())
							.getSimplePortIndicatorFlag())))
		  {
			  simplePortIndicatorNANC441 = Boolean.valueOf(true);
		  }
		  setContextValue("simplePortIndicatorNANC441Flag",simplePortIndicatorNANC441);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of NANC 441 Flags[Simple Port Indicator]: " + 
						"[" + simplePortIndicatorNANC441 +"]");
		  }
		  
		  try
		  {
			  // query to check whether SPID belongs to US & CN regions.
			  pstmt = conn
						.prepareStatement(SOAQueryConstants.GET_SPID_BELONG_TO_US_CN_QUERY);
			  
			  pstmt.setString(1, spid.toString());
			  
			  results = pstmt.executeQuery();
			  int spidRegionCount = 0;
			  while(results.next()){	          	
				  spidRegionCount = results.getInt(1);	          	
			  }
			  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
					Debug.log( Debug.MSG_STATUS, "Spid belongs to no of regions  : "+ spidRegionCount );
			  } 
			  //If spid belongs to more then 1 region including CN region.
			  //If spidRegionCount is 1 means it belongs to CN region only.
			  //It also checking SimplePortindicator flag is on for a spid or not.
			  Boolean spidBelongsToUSandCN = Boolean.valueOf(false);
			  if(spidRegionCount > 1 && simplePortIndicatorNANC441.booleanValue())
			  {
				  spidBelongsToUSandCN = Boolean.valueOf(true);
			  }
			  setContextValue("spidBelongsToUS_CNandSPIon",spidBelongsToUSandCN);
			  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			  {
					Debug.log(Debug.MSG_STATUS,"The value of spidBelongsToUS_CNandSPIon : " + 
							"[" + spidBelongsToUSandCN +"]");
			  }
		  }catch(Exception exception){
		
			  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				  Debug.log(Debug.ALL_ERRORS,exception.toString());
		     
		   }finally{
			
			close(results, pstmt);
		
		   }

		  Boolean lastAltSPIDFlag = Boolean.valueOf(false);
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getLastAltSpidFlag()))
		  {
			  lastAltSPIDFlag = Boolean.valueOf(true);
		  }
		  setContextValue("lastAltSPIDFlag",lastAltSPIDFlag);
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of Last Alternative SPID support flag is: " + 
						"[" + lastAltSPIDFlag +"]");
		  }
		  // end changes for 5.6.5
		  
		  Boolean pseudoLrnFlag = Boolean.valueOf(false);		  
		  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getPseudoLrnFlag()))
		  {
			  pseudoLrnFlag = Boolean.valueOf(true);
		  }
		  setContextValue("pseudoLrnFlag",pseudoLrnFlag);
		  
		  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  {
				Debug.log(Debug.MSG_STATUS,"The value of pseudoLrnFlag : " + 
						"[" + pseudoLrnFlag +"]");
		  }
	  }
      catch(Exception exception)
      {
    	  logErrors(exception, exception.toString());
      }
      
     return true;
	}
	public boolean modifyNpaNxxEffectiveDateValid(Value effectiveDate){

	  boolean result = false;

	  boolean tnResult = false;
	  
	  Connection conn = null;

      Statement stmt = null;
      
      ResultSet results = null;

	  ArrayList tnList = null;

	  TreeSet failedTnList = new TreeSet();
	  
	  TreeSet npaNnxList = new TreeSet();
	  TreeSet failedDashXList = new TreeSet();

	  TreeSet npaNxxXList = null;

	  HashMap npaNxxXDashx = null;

	  String dbNpaNxx = null;

	  String effectiveDateVal = null;

	  String dashX=null;

	  ArrayList npaNnxDList = new ArrayList();	  

	  ArrayList failedNpaNxxXList = null;

	  StringBuffer errorMessage=new StringBuffer();

	  if (existsValue("tnList"))
	  {
			tnList =(ArrayList)getContextValue("tnList");			
	  }

	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		  Debug.log(Debug.MSG_STATUS,"tnList in modifyNpaNxxEffectiveDateValid: "+tnList);	 

	  String npanxx = null;

	  String napnxxQuery = null;

	  String failedTn=null;

	  StringBuffer npaNxxValue = new StringBuffer();	  	  
				
 	  if (existsValue("npaNnxList"))
	  {
			npaNnxList = (TreeSet)getContextValue("npaNnxList");
	  }

	  try{		 

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


		 if (effectiveDate.toString().length() == 
		 SOAConstants.DATE_HHMI_LENGTH){
              
              effectiveDateVal = effectiveDate.toString().substring(
              						SOAConstants.DATE_START_INDEX,
									SOAConstants.DATE_MIDLLE_INDEX)+
									SOAConstants.DATE_TIME_CONCATENATION + 
										effectiveDate.toString().substring(
									SOAConstants.DATE_MIDLLE_INDEX,
									SOAConstants.DATE_END_INDEX) ;
          
          } else{
              
              effectiveDateVal = effectiveDate.toString();
          }

		 napnxxQuery ="SELECT NPA||NXX FROM SOA_NPANXX WHERE (NPA|| NXX) " +
		 	"in ("+npaNxxValue+") AND EFFECTIVEDATE > TO_DATE" +
		 	"('"+effectiveDateVal+"','MM-DD-YYYY-HHMISSPM') AND STATUS = 'ok'";

		 if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			 Debug.log(Debug.NORMAL_STATUS,"napnxxQuery in modifyNpaNxxEffectiveDateValid: "+napnxxQuery);

		 // Get Connection from RuleContext so that it is shared by other 
		 //functions.
		 conn = getDBConnection();

		 stmt = conn.createStatement();		 

		 results = stmt.executeQuery(napnxxQuery);						

		 while( results.next() ){

			dbNpaNxx = results.getString(1);		

			npaNnxDList.add(dbNpaNxx);					

		 }
		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"npaNnxDList in modifyNpaNxxEffectiveDateValid: "+npaNnxDList);

		 if (existsValue("npaNxxXList"))
		 {
			npaNxxXList = (TreeSet)getContextValue("npaNxxXList");
			
			if(npaNxxXList != null)
			{
				Iterator tnIterator = npaNxxXList.iterator();

				while (tnIterator.hasNext())			
					{				
					dashX = (String)tnIterator.next();
					
					npanxx = dashX.substring(0, dashX.length()-1);
					
					if (npaNnxDList.contains(npanxx))
						failedDashXList.add(dashX);				
				}
			}
		 }
		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			 Debug.log(Debug.MSG_STATUS,"failedDashXList in modifyNpaNxxEffectiveDateValid: "+failedDashXList);
		 
		 tnResult = npaNnxDList.isEmpty();

		 if (tnResult)
		 {
			 result = true;
		 }
		 else
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
			
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"failedTnList in modifyNpaNxxEffectiveDateValid: "+failedTnList);

			Iterator failedTnIterator = failedTnList.iterator();

			while (failedTnIterator.hasNext())
			{
				failedTn = ((String)failedTnIterator.next());
				addFailedTN(failedTn);
				errorMessage.append("["+failedTn+" ] ");
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"failedTn in modifyNpaNxxEffectiveDateValid: "+failedTn);
			}
			
			setDynamicErrorMessage(errorMessage.toString());
		 }

      }catch(Exception exception){

		logErrors(exception, napnxxQuery);
         
      }finally{
		
		close(results, stmt);
      }
      if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
    	  Debug.log(Debug.MSG_STATUS,"Result for modifyNpaNxxEffectiveDateValid : "+result);

      return result;

   }

	public static class CreateSvData {

		public String spid=null;				
		public String onsp=null;        
        public String nnsp=null;        
		public String portingTn=null;
		public String status=null; 
		public Date nnspDate=null;
		public Date onspDate=null;
		public String lastRequestType = null;        
        public String lastProcessing = null; 
        public int pto = 0;
        public String lrn = null;
        public String lnptype = null;
		
		public CreateSvData(String onsp, String nnsp, String portingTn,
				String status, Date nnspDate, Date onspDate,
				String lastRequestType, String lastProcessing, int pto,
				String lrn, String lnptype) throws FrameworkException {
						
			this.onsp   = onsp;
            this.nnsp   = nnsp;
			this.portingTn = portingTn;
			this.status = status;
			this.nnspDate = nnspDate;
			this.onspDate = onspDate;
			this.lastRequestType = lastRequestType;
	        this.lastProcessing = lastProcessing;
	        this.pto = pto;
	        this.lrn = lrn;
	        this.lnptype = lnptype;
        }
		public void setSpid(String spid) {
			this.spid = spid;
		}
		public String getSpid() {
			return spid;
		}
		
	 }
	
	public static class ModifySvData {

		public String onsp = null;
		public String nnsp = null;
		public String portingTn = null;
		public String status = null;
		public Date nnspDate = null;
		public Date onspDate = null;
		boolean pto = false;
		public String altspid = null;
		public String subdomain = null;
		public String causecode = null;
		public String lnptype = null;
		public String lrn = null;
		
		public String onspSimplePortindicator = null;

		public String nnspSimplePortindicator = null;

		public ModifySvData(String onsp, String nnsp, String portingTn,
				String status, Date nnspDate, Date onspDate, boolean pto,
				String altspid, String subdomain, String causecode, String lrn, 
				String lnptype, String onspSimplePortindicator, String nnspSimplePortindicator)
				throws FrameworkException {

			this.onsp = onsp;
			this.nnsp = nnsp;
			this.portingTn = portingTn;
			this.status = status;
			this.nnspDate = nnspDate;
			this.onspDate = onspDate;
			this.pto = pto;
			this.altspid = altspid;
			this.subdomain = subdomain;
			this.causecode = causecode;
			this.lnptype = lnptype;
			this.lrn = lrn;
			this.onspSimplePortindicator = onspSimplePortindicator;
			this.nnspSimplePortindicator = nnspSimplePortindicator;
		}
	}

	 public static class NpaNxxData 
	 {        
        public String npanxx=null;
		public String spid=null;        
		public long effectiveDate=0;
		// Changes are made in 5.6.5 release (for canadian customer)
		// added regionId
		public String regionId = null;
        public NpaNxxData (String npanxx, String spid, long effectiveDate, String regionId) 
        throws FrameworkException 
		{            
            this.npanxx   = npanxx;
			this.spid   = spid;
			this.effectiveDate = effectiveDate;
			this.regionId = regionId;
        }
	 }

	 public static class NpaNxxXData 
	 {        
        public String npanxxdashx=null;
		public String spid=null;        
		public long effectiveDate=0;

        public NpaNxxXData (String npanxxdashx, String spid, 
        long effectiveDate) throws FrameworkException 
		{            
            this.npanxxdashx   = npanxxdashx;
			this.spid   = spid;
			this.effectiveDate = effectiveDate;
        }
	 }
	
	  
		public static class CreateTnAltSpidData 
		{
			public String portingTn=null;
			public String altspid= null;

	        public CreateTnAltSpidData ( String portingTn, String altspid) throws FrameworkException 
	        {
	        	if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
	        		Debug.log(Debug.MSG_DATA," in CreateTnAltSpidData.CreateTnAltSpidData().");
	        	
	        	this.portingTn = portingTn;
				this.altspid = altspid;
	        }
		 }
		
		public static class CreateTnSubdomainData 
		{
			public String portingTn=null;
			public String subdomain= null;

	        public CreateTnSubdomainData ( String portingTn, String subdomain) throws FrameworkException 
	        {
	        	if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
	        		Debug.log(Debug.MSG_DATA," in CreateTnSubdomainData.CreateTnSubdomainData().");
	        	
	        	this.portingTn = portingTn;
				this.subdomain = subdomain;
	        }
		 }
			
		
		public static class NPBData 
		 {        
	        public String npa=null;
	        public String nxx=null;
	        public String dashX=null;
	        public String staus=null;
	        public String lrn=null;
			public String spid=null;        
			
			public NPBData (String npa, String nxx,String dashX, String status, 
					String lrn ,String spid) 
	        throws FrameworkException 
			{            
	            this.npa = npa;
	            this.nxx = nxx;
	            this.dashX = dashX;
	            this.staus = status;
	            this.lrn = lrn;
				this.spid   = spid;
			}
		 }
		   /**
		    * This method is used to check for the given SPID , VoiceURI flag is 
		    * disabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isVoiceURIFlagDisabledSPID(Value spid){
			   boolean result = false;
			   try{

			    	  String nancflag = String.valueOf(NANCSupportFlag.
			    			  getInstance(spid.toString()).getVoiceURI());
			    	  if(nancflag != null && nancflag.equals("1"))
			    	  result=true;

					
			    	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isVoiceURIFlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   
			   return result;
		   }
		   
		   /**
		    * This method is used to check for the given SPID , MMSURI flag is 
		    * disabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isMmsURIFlagDisabledSPID(Value spid){
			   boolean result = false;
			   try{

			    	  String nancflag = String.valueOf(NANCSupportFlag.
			    			  getInstance(spid.toString()).getMmsURI());
			    	  if(nancflag != null && nancflag.equals("1"))
			    	  result=true;

					
			    	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isMmsURIFlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   
			   return result;
		   }
		   
		   /**
		    * This method is used to check for the given SPID , PocURI flag is 
		    * disabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isPocURIFlagDisabledSPID(Value spid){
			   boolean result = false;
			   try{

			    	  String nancflag = String.valueOf(NANCSupportFlag.
			    			  	getInstance(spid.toString()).getPocURI());
			    	  if(nancflag != null && nancflag.equals("1"))
			    	  result=true;

					
			    	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isPocURIFlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   
			   return result;
		   }
		   
		   /**
		    * This method is used to check for the given SPID , PresURI flag is 
		    * disabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isPresURIFlagDisabledSPID(Value spid){
			   boolean result = false;
			   try{

			    	  String nancflag = String.valueOf(NANCSupportFlag.
			    			  	getInstance(spid.toString()).getPresURI());
			    	  if(nancflag != null && nancflag.equals("1"))
			    	  result=true;

					
			    	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isPresURIFlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   
			   return result;
		   }
		   
		   /**
		    * This method is used to check for the given SPID , SMSURI flag is 
		    * disabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isSmsURIFlagDisabledSPID(Value spid){
			   boolean result = false;
			   try{

			    	  String nancflag = String.valueOf(NANCSupportFlag.
			    			  	getInstance(spid.toString()).getSmsURI());
			    	  if(nancflag != null && nancflag.equals("1"))
			    	  result=true;

					
			    	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isSmsURIFlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   
			   return result;
		   }
		   public static class CreateTnOnspNnspData 
			{
				public String tn=null;
				public String onsp= null;
				public String nnsp = null;

		        public CreateTnOnspNnspData ( String tn, String onsp , String nnsp) throws FrameworkException 
		        {
		        	if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
		        		Debug.log(Debug.MSG_DATA," in CreateTnAltSpidData.CreateTnAltSpidData().");
		        	
		        	this.tn = tn;
					this.onsp = onsp;
					this.nnsp = nnsp;
		        }

				public String toString()
				{
					return tn;
				}
			 }
		   /**
		    * This method return the list of Tns as a range if Tns are consequtive and onsp value is also same.
		    * @param tnOnspNnspList
		    * @return
		    */
		   public ArrayList<String> getModifiedSubmittedTnList(ArrayList<CreateTnOnspNnspData> tnOnspNnspList){
			   ArrayList <String>subRange = new ArrayList<String>();
			   String currentNpaNxx = null;
			   String previousNPANxx = null;
			   int currentEndTn = -1;
			   int previousEndTn = -1;
			   int startTn = -1;
			   int incr = 0;
			   String currentSubmitTn = null;
			   String currentSubmitonsp = null;
			   String currentSubmitnnsp = null;
			   String previousSubmitTn = null;
			   String previousSubmitonsp = null;
			   String previousSubmitnnsp = null;
			   boolean flag = false;
			   
			   Iterator<CreateTnOnspNnspData> iter = tnOnspNnspList.iterator();
			   CreateTnOnspNnspData tnOnspNnsp = null;
			   while(iter.hasNext()){
				   tnOnspNnsp = (CreateTnOnspNnspData)iter.next();
				  if(tnOnspNnsp != null){
					  currentSubmitTn = (String)tnOnspNnsp.tn;
					  currentSubmitonsp = (String)tnOnspNnsp.onsp;
					  currentSubmitnnsp = (String)tnOnspNnsp.nnsp;
					  currentNpaNxx = currentSubmitTn.substring(0,7);
					  currentEndTn = Integer.parseInt(currentSubmitTn.substring(8));
					  if(!flag){
						  startTn = Integer.parseInt(currentSubmitTn.substring(8));
					  }
					  if( previousSubmitTn != null && previousSubmitonsp != null 
							  && previousSubmitnnsp != null ){
					  
						  if (currentNpaNxx.equals(previousNPANxx) && currentEndTn == previousEndTn+1
								  && currentSubmitonsp.equals(previousSubmitonsp)
								  && currentSubmitnnsp.equals(previousSubmitnnsp) ){
						  
							  previousEndTn = currentEndTn;
							  flag = true;
							  incr++;
						  }else{
							  if(incr > 0){
								  subRange.add(previousNPANxx + "-" + 
								  StringUtils.padNumber(startTn,SOAConstants.TN_LINE, true, '0') + "-" +
								  StringUtils.padNumber(previousEndTn,SOAConstants.TN_LINE, true, '0'));
							  }else{
								  subRange.add(previousNPANxx + "-" + 
									  StringUtils.padNumber(previousEndTn,SOAConstants.TN_LINE, true, '0'));
							  }
							  previousSubmitTn = currentSubmitTn;
							  previousSubmitonsp = currentSubmitonsp;
							  previousSubmitnnsp = currentSubmitnnsp;
							  previousEndTn = currentEndTn; 
							  previousNPANxx = currentNpaNxx;
							  startTn = previousEndTn;
							  incr = 0;
						  }
					  }
					  else{
						  previousSubmitTn = currentSubmitTn;
						  previousSubmitonsp = currentSubmitonsp;
						  previousSubmitnnsp = currentSubmitnnsp;
						  previousEndTn = currentEndTn; 
						  previousNPANxx = currentNpaNxx;
						  flag = true;
					  }
				  }
			   }	  
			   if(incr > 0){
			   subRange.add(previousNPANxx + "-" + 
						  StringUtils.padNumber(startTn,SOAConstants.TN_LINE, true, '0') + "-" +
						  StringUtils.padNumber(previousEndTn,SOAConstants.TN_LINE, true, '0'));
			   }else{
					  subRange.add(previousNPANxx + "-" + 
							  StringUtils.padNumber(previousEndTn,SOAConstants.TN_LINE, true, '0'));
				  }
					  
			 return subRange;
		   }
		   /**
		    * This method return true if onsp or nnsp values of submitted tns are not same otherwise return false.
		    * @return
		    */
		   
		   private boolean isOnspNnspDifferent(){
			   boolean bool = false;
			   Set onspSet = new HashSet();
			   Set nnspSet = new HashSet();
   			   ArrayList tnOnspDataList =(ArrayList)getContextValue("tnOnspDataList");

			   if (tnOnspDataList != null)
			   {
				   for (int i=0;i < tnOnspDataList.size();i++ )
				   {
					   CreateTnOnspNnspData tnOnspNnspData =(CreateTnOnspNnspData)tnOnspDataList.get(i);
					   onspSet.add(tnOnspNnspData.onsp);
					   nnspSet.add(tnOnspNnspData.nnsp);
				   }		
			   }
			   if(onspSet.size() > 1 || nnspSet.size() > 1 ){
				   bool = true;
			   } 

			   return bool;
		   }

   public boolean triggerNumberPoolBlock(Value spid, Value lrn )
		   {
			      
			   try
				  {

						  
			    	  Boolean altbidnanc436Value = null;
			    	  Boolean alteultnanc436Value = null;
			    	  Boolean alteulvnanc436Value = null;
			    	  
			    	  if (NANCSupportFlag.getInstance(spid.toString()).getAltBidNanc436Flag()
								.equals("1")) {
							altbidnanc436Value = Boolean.valueOf(true);
						} else {
							altbidnanc436Value = Boolean.valueOf(false);
						}
						if (NANCSupportFlag.getInstance(spid.toString()).getAltEultNanc436Flag()
								.equals("1")) {
							alteultnanc436Value = Boolean.valueOf(true);
						} else {
							alteultnanc436Value = Boolean.valueOf(false);
						}
						if (NANCSupportFlag.getInstance(spid.toString()).getAltEulvNanc436Flag()
								.equals("1")) {
							alteulvnanc436Value = Boolean.valueOf(true);
						} else {
							alteulvnanc436Value = Boolean.valueOf(false);
						}    	  
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,"The value of NANC 436 Flags[ALTBID,ALTEULT,ALTEULV]: " + "[" + altbidnanc436Value + "," + alteultnanc436Value + ","+ alteulvnanc436Value + "]");
					  
					  setContextValue("altbidnanc436flag",altbidnanc436Value);
					  setContextValue("alteultnanc436flag",alteultnanc436Value);
					  setContextValue("alteulvnanc436flag",alteulvnanc436Value);
					  
                      Boolean lastaltspid = null;
                      
			if (NANCSupportFlag.getInstance(spid.toString()).getLastAltSpidFlag().equals("1")) {
						  lastaltspid = Boolean.valueOf(true);
						} 
					  else {
							lastaltspid = Boolean.valueOf(false);
						}
					  setContextValue("lastAltSPIDFlag",lastaltspid);
					  
					  
					  Boolean pseudoLrnFlag = Boolean.valueOf(false);
					  
					  if ("1".equals(NANCSupportFlag.getInstance(spid.toString()).getPseudoLrnFlag()))
					  {
						  pseudoLrnFlag = Boolean.valueOf(true);
					  }
					  setContextValue("pseudoLrnFlag",pseudoLrnFlag);
					  
					  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					  {
				Debug.log(Debug.MSG_STATUS,"The value of pseudoLrnFlag : " + "[" + pseudoLrnFlag +"]");
					  }
				  }
			      catch(Exception exception)
			      {
			    	  logErrors(exception, exception.toString());
			      }
      
	   String lrnValue = null;
	   ArrayList LrnRegionList = new ArrayList();	  
	   if(lrn != null && lrn.toString()!= ""){				   
    	  lrnValue = lrn.toString();
    	  
	   	   String lrnRegionQuery = "SELECT REGIONID FROM SOA_LRN WHERE " +
	  		"SPID='"+ spid.toString() + "' AND LRN='" +lrnValue.toString()+ "' AND STATUS='ok'";
	   	   
			Connection con = null;
			Statement stmt = null;
			ResultSet rs = null;		
			
			try{
				con = getDBConnection();
				stmt = con.createStatement();
				rs = stmt.executeQuery(lrnRegionQuery);				
				while(rs.next()){
					LrnRegionList.add(rs.getString(1));
				}
				if(!LrnRegionList.isEmpty())
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
				setContextValue("LrnRegionList",LrnRegionList);
			}		
			catch(Exception exp){
				logErrors(exp, lrnRegionQuery);
			}finally{
				close(rs, stmt);
			}
	   	}      
			
			     return true;
			   
		   }
		   
		   /**
		    * This method is used to check for the given SPID , SPCUSTOM1 flag is 
		    * enabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isSPCustom1FlagDisabledSPID(Value spid){
			   boolean result = true;
			   try{

			    	  String custom1Flag = String.valueOf(NANCSupportFlag.
			    			  	getInstance(spid.toString()).getSpCustom1Flag());
			    	  if(custom1Flag != null && custom1Flag.equals("1"))
			    		  result=false;

			    	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isSPCustom1FlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   return result;
		   }
		   
		   /**
		    * This method is used to check for the given SPID , SPCUSTOM2 flag is 
		    * enabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isSPCustom2FlagDisabledSPID(Value spid){
			   boolean result = true;
			   try{

			    	  String custom2Flag = String.valueOf(NANCSupportFlag.
			    			  	getInstance(spid.toString()).getSpCustom2Flag());
			    	  if(custom2Flag != null && custom2Flag.equals("1"))
			    		  result=false;

					  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isSPCustom2FlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   return result;
		   }
		   
		   /**
		    * This method is used to check for the given SPID , SPCUSTOM3 flag is 
		    * enabled or not
		    *
		    * @param spid Value the SPID to check.
		    * @return boolean whether or not the spid belongs to the current customer
		    * ID.
		    */
		   public boolean isSPCustom3FlagDisabledSPID(Value spid){
			   boolean result = true;
			   try{

			    	  String custom3Flag = String.valueOf(NANCSupportFlag.
			    			  	getInstance(spid.toString()).getSpCustom3Flag());
			    	  if(custom3Flag != null && custom3Flag.equals("1"))
			    		  result=false;

			    	  if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			    		  Debug.log(Debug.MSG_STATUS,"isSPCustom3FlagDisabledSPID: "+result);

			      }catch(Exception exception){

			         logErrors(exception, exception.toString());

			      }
			   
			   return result;
		   }
		   /**
		    * Checks the pseudo LRN support flag for a SPID
		    * @param spid
		    * @return boolean pseudo LRN Flag value
		    */
		   public boolean isPseudoLrnFlag(){   	  

				Boolean pseudoLrnFlag =(Boolean)getContextValue("pseudoLrnFlag");		

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Executing isPseudoLrnFlag: "+pseudoLrnFlag);

				boolean pseudoLrnFlagValue = pseudoLrnFlag.booleanValue();
				return pseudoLrnFlagValue;

			}
	   
}
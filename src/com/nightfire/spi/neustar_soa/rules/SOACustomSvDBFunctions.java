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
 * @see import java.sql.Timestamp;
 * @see import java.util.ArrayList;
 * @see import java.util.Date;
 * @see import java.util.TimeZone;
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

	1			RameshC			07/18/2006		Added isAccountIDUnique() to
												check whether it is	uniqueness
												or not.
	2			RameshC			07/19/2006		Added isNpaNxxLrn() to check
												Lrn based on NpaNxx.


	3.			RameshC			08/22/2006		Added isValueLrn() to retrieve
												the Lrn's based on SPID
												and	Gtt Data.

	4.			Jigar Talati	10/13/2006		Added validateDueDate().

	5.			SUNIL.D			12/29/2006		Added existSv() to check 
												whether svid is exists or not.

	6.			SUNIL.D			12/29/2006		Added getStatusSv() to get the 
												Status of svid.

	7.			SUNIL.D			12/29/2006		Added isNewSPSvId() to check 
												for the given spid is newsp or
												not.

	8.			SUNIL.D			12/29/2006		Added isDateLaterThanSvId() to
												check the given date is	current 
												or future date.

	9.			SUNIL.D			12/29/2006		Added 
												isBusinessTimerTypeLongSvId()
												to check whether the timer type
												is long or not. 

	10.			RAMESH CHIMATA	12/29/2006		Added isOnspSvId() to check for
												the given spid is onsp or not.
	11.			MANOJ.K			01/02/2007		Added isCauseCodeSvId()to check 
												the CauseCode.

	12.			SUNIL.D			01/11/2007		Added isSvidLrn() to check for
												the status of lrn based on 
												svid and regionid.

	13.			SUNIL.D			01/11/2007		Added isOnspDueDateSvidNull() 
												to check if the OnspDueDate 
												is Null	or not.

	14.			SUNIL.D			01/11/2007		Added isNnspDueDateSvidNull()  
												to check if the NnspDueDate 
												is Null or not.

	15.			SUNIL.D			01/11/2007		Added 
												isSPCancelPendingPresent()  
												to check if the NnspDueDate is 
												Null or not.

	16.			SUNIL.D			01/11/2007		Added 
												isOldSPPendingConflictPresent()
												to check if the NnspDueDate
												is Null or not.

	17.         SUNIL.D			01/11/2007		Added isNewSPActivePresent to
												check if the NnspDueDate 
												is Active.

	18.         SUNIL.D			01/11/2007		Added isNewSPPTOFalsePresent to 
												check if the NnspDueDate,
												porttoOrg  is false.

	19.         SUNIL.D			01/11/2007		Added isNewSPPTOTruePresent to 
												check if the NnspDueDate,
												porttoOrg  is true.	

	20.         SUNIL.D			01/11/2007		Added isIntraPTOTruePresent()
												to check if the IntraPort,
												porttoOrg  is true.

	21.         SUNIL.D			01/11/2007		Added isIntraPTOFalsePresent()
												to check if the NnspDueDate,
												porttoOrg  is true.

	22.         SUNIL.D			01/11/2007		Added 
												isNewSPDisconnectPresent()
												to check for the status.

	23.         SUNIL.D			01/11/2007		Added getStatusOldSP()to 
												Check the status.

	24.         SUNIL.D			01/11/2007		Added getMultipleConflicts() 
												Checks the status for Conflict.

	25.         SUNIL.D			01/11/2007		Added getAuthorizationFlag() 
												checks for AuthorizationFlag.

	26.         SUNIL.D			01/11/2007		Added 
												isNpaNxxEffectiveDateSvidValid()
												checks for EffectiveDate.

	27.			Peeyush M		05/15/2007		Added functinos for subdomain requirement. 

    28.         ROHIT.G         06/13/2007      Added function isValidAccountIdAccountName

    29.			Peeyush M		06/22/2007		Added function isPTORequestbySubDomainUser.

    30.			Peeyush M		07/27/2007 		Incorported SOA 4.0.3 funcspec change to fix the TD. #6487.

 */

package com.nightfire.spi.neustar_soa.rules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.rules.Value;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.neustar_soa.utils.NANCSupportFlag;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;
import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;


public abstract class SOACustomSvDBFunctions extends SOACustomDBFunctions{


	/**
	 * This queries for the Customer ID for given spid
	 * @param spid
	 * @return String 
	 */
	public String getCustomerID(Value spid){

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String customerID = null;

		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, getCustomerID :\n" + SOAQueryConstants.GET_CUSTOMERID_QUERY);

			pstmt = conn.prepareStatement(
					SOAQueryConstants.GET_CUSTOMERID_QUERY);

			pstmt.setString( 1, spid.toString());

			results = pstmt.executeQuery();

			if(results.next()){

				customerID = results.getString(1);                                
			}
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"getCustomerID value is: "+customerID);

		}catch(Exception exception){

			logErrors(exception, SOAQueryConstants.GET_CUSTOMERID_QUERY);

		}finally{

			close(results, pstmt); 
		}

		return customerID;
	}           

	/**
	 * This method is used to check the status of the accountid is 
	 * unique or not 
	 * @param accountId. 
	 * @param spid Value SPID. 
	 * @return boolean.
	 */   
	public boolean isAccountIdUnique(Value accountId,
			Value spid){

		boolean result= true;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		try{

			conn = getDBConnection();
			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, isAccountIdUnique :\n" + SOAQueryConstants.GET_ACCOUNTID);

			pstmt = conn.prepareStatement(SOAQueryConstants.GET_ACCOUNTID);

			pstmt.setString( 1, accountId.toString() );
			pstmt.setString( 2, spid.toString() );	

			results = pstmt.executeQuery();

			if( results.next() ){
				result = false;
			}				

		}catch(Exception exception){
			logErrors(exception, SOAQueryConstants.GET_ACCOUNTID);

		}finally{

			close(results, pstmt); 
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Account Id is:"+ accountId);

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result is:"+ result);

		return result;	      
	}

	/**
	 * This method is used to check the status of lrn based on NpaNxx
	 *
	 * @param NpaNxx Value
	 * @param spid Value SPID.
	 * @param lrn Value LRN.
	 * @return boolean.
	 */   
	public boolean isNpaNxxLrn(Value npaNxx, Value spid, Value lrn, Value npbid, Value regionid){   	   

		boolean result = true;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String npa = null;

		String nxx = null;		

		String npaNxxVal = npaNxx.toString();		

		if ( npaNxxVal == null || npaNxxVal == "")
		{

			npaNxx= new Value(getnpaNxxvalue(npbid,regionid));		  

		}		 

		if(npaNxx.length() > 7){

			npa = npaNxx.toString().substring(0,3);

			nxx = npaNxx.toString().substring(4,7);

		}
		else{

			npa = npaNxx.toString().substring(0,3);

			nxx = npaNxx.toString().substring(3,6);

		}

		String npanxx = npa + nxx;

		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, isNpaNxxLrn :\n" + SOAQueryConstants.GET_LRN);

			pstmt = conn.prepareStatement(SOAQueryConstants.GET_LRN);

			pstmt.setString( 1, spid.toString());   

			pstmt.setString( 2, npanxx );

			results = pstmt.executeQuery();

			StringBuffer errorMessage=new StringBuffer();

			while( results.next() ){

				if (results.getString(1).equals(lrn.toString())){
					result = true;
					break;
				}
				else{					
					errorMessage.append("[" + results.getString(1) + "]");				   
					result = false;			 
				}			 
			}

			if (!result)
			{
				setDynamicErrorMessage(errorMessage.toString());
			}

		}catch(Exception exception){

			logErrors(exception, SOAQueryConstants.GET_LRN);

		}finally{

			close(results, pstmt); 

		}

		return result;	      
	}



	/**
	 * This method is used to retrieve lrns based on gtt data.
	 * @param lrn Value
	 * @param spid Value
	 * @return boolean.
	 */   
	public boolean isValueLrn(Value lrn, Value spid){

		boolean result = true;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String lrn1 = lrn.toString();


		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, isValueLrn :\n" + SOAQueryConstants.LIST_GTT_LRN);

			pstmt = conn.prepareStatement(SOAQueryConstants.LIST_GTT_LRN);

			pstmt.setString( 1, spid.toString());
			results = pstmt.executeQuery();

			StringBuffer errorMessage=new StringBuffer();
			StringTokenizer st = new StringTokenizer(lrn1,

					DBMessageProcessorBase.SEPARATOR);

			int count=st.countTokens();
			String[] tokens = new String[count];

			int y=0;
			while(st.hasMoreTokens())
			{
				tokens[y] = st.nextToken().toString();
				y++;
			}

			while( results.next() ){
				for (int x=0; x<tokens.length; x++){
					if (results.getString(1).equals(tokens[x].toString())){
						errorMessage.append("[" + results.getString(1) + "]");						 
						result=false;
						break;
					}								
				}
			}

			if (!result)
			{
				setDynamicErrorMessage(errorMessage.toString());
			}

		}catch(Exception exception){

			logErrors(exception, SOAQueryConstants.LIST_GTT_LRN);

		}finally{

			close(results, pstmt); 

		}
		return result;
	}


	/**
	 * Created By SUNIL.D
	 * This method is used to Check if the Sv Exists.
	 */   

	public boolean existSv(){

		boolean result = false;

		Boolean existSv=(Boolean)getContextValue("existSv");

		result=existSv.booleanValue();

		return result ;
	}

	/**
	 * Created By SUNIL.D
	 * This method retrieves the status of the subscription version
	 * with the given SVID in the given region.
	 * This method is used to Check the status.
	 */   

	public boolean getStatusSv( String[] states){

		boolean result = false;

		String status = null;

		status =getContextValue("statusSvId").toString();		

		Value valueStatus = new Value (status);

		if (valueStatus.isMember(states)){			    

			result = true;

		}		

		return result;
	}


	/**
	 * Created By SUNIL.D
	 * This method retrieves the nnsp value from the context
	 * and compares it with the spid to check if the 
	 * the given TN has the given SPID as its new service provider.	
	 * param spid Value SPID.
	 * @return boolean.
	 */   

	public boolean isNewSPSvId(Value spid){

		String nnsp = null;		

		boolean result = false;

		nnsp =getContextValue("nnspSvId").toString();

		if (nnsp.equals(spid.toString()) ){

			result = true;
		}				

		return result ;

	}



	/**
	 * This method determines if the given date2 is on the same day or
	 * later than the given date.
	 *
	 * @return boolean true if date2 is on the same date or later than date.
	 *                      False otherwise.
	 */
	public boolean isDateLaterThanSvId(){

		SimpleDateFormat dateFormat = 
			new SimpleDateFormat("MM-dd-yyyy-hhmmssa");	  	   

		Date currentDate = new Date();

		Date dueDate =null;		

		Date date =(Date)getContextValue("nnspduedateSvId");

		Date objectCreationDate = (Date)getContextValue("objectCreationDate");

		String nnspduedate = null;

		if(  date != null )
		{
			nnspduedate = dateFormat.format(date);
		}

		if( nnspduedate != null )
		{		

			try {

				dueDate =dateFormat.parse(nnspduedate);				

			} catch (ParseException ex) {

				return false;
			}		
		}

		if( currentDate == null || dueDate == null || objectCreationDate == null){

			if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {

				Debug.log(Debug.RULE_EXECUTION,
						"Could not compare dates [" + currentDate + "] " +
						"and [" + dueDate + "].");
			}
			return true;
		}

		long currentDateTime = currentDate.getTime();

		long dueDateTime = 0L;

		dueDateTime = dueDate.getTime();

		boolean result = true;
		result = currentDateTime > dueDateTime;
		if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {

			String not = (result) ? "" : "not ";

			Debug.log(Debug.RULE_EXECUTION, "Date [" + currentDateTime +
					"] is "+not+"later than date [" +
					dueDateTime + "]");

		}	  

		return result;

	}



	/**
	 * Created By Ramesh Chimata
	 * This method used to check whether the given spid is 
	 * old service provider or not.
	 * param spid Value SPID.
	 * @return boolean.
	 */   

	public boolean isOnspSvId(Value spid){

		String onsp = null;		  		   

		boolean result = false;

		onsp =getContextValue("onspSvId").toString();

		if (onsp.equals(spid.toString()) ){

			result = true;
		}		 

		return result;

	}	

	/**
	 * This method is used to check the status change CauseCode  
	 * of the subscription version with the given spid.
	 * param spid Value SPID.
	 * @return boolean.
	 */   

	public boolean isCauseCodeSvId(Value spid){

		String causeCode = null;

		boolean result = true;

		String nnsp = null;		   

		StringBuffer errorMessage = new StringBuffer();

		try{
			causeCode =getContextValue("causeCode").toString();

			nnsp = getContextValue("nnspSvId").toString();

		}catch(NullPointerException ex){

			return true;
		}
		if(nnsp.equals(spid.toString())){

			if (causeCode != null && (causeCode.equals(
					SOAConstants.LSR_NOT_RECEIVED) || 
					causeCode.equals(SOAConstants.FOC_NOT_ISSUED)))
			{
				errorMessage.append("A request can be sent only");
				errorMessage.append(
				" when the Status Change Cause Code for the " );
				errorMessage.append("subscription version is not equal to  " +
						"["+SOAConstants.LSR_NOT_RECEIVED+" ] ");
				errorMessage.append("Or ["+SOAConstants.FOC_NOT_ISSUED+"]");

				result=false;
			}	
		}

		if (!result)
		{						 
			setDynamicErrorMessage(errorMessage.toString());			
		}

		return result;
	}

	/**Added by the ics on 5th jan 2006
	 * This method is used to check whether the given telephone number(s)
	 * exists or not.
	 * return boolean.
	 */   

	public boolean existsTnSv(){

		boolean result = true;

		Iterator iter = null;

		ArrayList portingTnsList = new ArrayList();

		ArrayList svDataList = null;

		ArrayList tnList = null;

		StringBuffer errorMessage = new StringBuffer();

		svDataList =(ArrayList)getContextValue("rsDataList");		

		tnList =(ArrayList)getContextValue("tnList");
		if (tnList == null){
			 return false;
		 }
		
		if(svDataList != null)
		{

			for (int i=0;i < svDataList.size();i++ )
			{
				ResultsetData svData =(ResultsetData)svDataList.get(i);

				portingTnsList.add(svData.portingTn);

			}

			iter = tnList.listIterator();

			while (iter.hasNext())
			{
				String tn = (String)iter.next();
				if (!portingTnsList.contains(tn))
				{
					addFailedTN(tn);					
					errorMessage.append("[" + tn + "] ");

					result=false;
				}


			}
		}		
		else{
			result = false;
			iter = tnList.listIterator();
			while (iter.hasNext())
			{
				String tn = (String)iter.next();
				addFailedTN(tn);
				errorMessage.append("[" + tn + "] ");

			}

		}
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for getStatus: "+result);

		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}

		return result;

	}


	/**
	 * This gets the status of the most recent subscription 
	 * version with the given telephone number.
	 * @param status Value STATUS.
	 * @return boolean.
	 */
	public boolean getStatusTn(String[] status){

		boolean result = true;

		Value tnStatus = null;

		String portingTn = null;

		StringBuffer errorMessage = new StringBuffer();		
			
		ArrayList svDataList = null;

		if (existsValue("rsDataList")){	    

			svDataList =(ArrayList)getContextValue("rsDataList");					

			if (svDataList != null)
			{

				for (int i=0;i < svDataList.size();i++ )
				{
					ResultsetData svData =(ResultsetData)svDataList.get(i);

					portingTn = svData.portingTn;
					tnStatus = new Value(svData.status);

					if (!tnStatus.isMember(status))
					{								
						addFailedTN(portingTn);
						errorMessage.append("["+portingTn+"] ");
						result = false;						
					}			
				}				
			}
		}
		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}		  		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for getStatus: "+result);

		return result;

	}
	/**
	 * Checks to see if the most recent active-like subscription version for
	 * the given TN has the given SPID as its old service provider.
	 * @param spid Value SPID.
	 * @return boolean.
	 */
	public boolean isOldSPTn(Value spid){

		boolean result = true;	

		String portingTn = null;

		String onsp = null;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("rsDataList"))
		{
			svDataList =(ArrayList)getContextValue("rsDataList");
			
			if(svDataList != null){
			
				for (int i=0;i < svDataList.size();i++ ){
					
					ResultsetData svData =(ResultsetData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					onsp = svData.onsp;
	
					if (!onsp.equals(spid.toString()) )
					{
						addFailedTN(portingTn);
						errorMessage.append("[" + portingTn + "] ");
						result = false;
					}
				}
			}
		}

		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isOldSPTn: "+result);

		return result;


	}

	/**
	 * Checks to see if the most recent active-like subscription version for
	 * the given TN has the given SPID as its new service provider.
	 * @param spid Value SPID 
	 * @return boolean.
	 */
	public boolean isNewSPTn(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("rsDataList"))
		{
			svDataList =(ArrayList)getContextValue("rsDataList");
			
			if( svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
				{
					ResultsetData svData =(ResultsetData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					if (!nnsp.equals(spid.toString()) )
					{
						addFailedTN(portingTn);
						errorMessage.append("["+portingTn+"] ");
						result = false;
					}
				}
			}
		}


		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isNewSPTn: "+result);

		return result;


	}
	
	/**Created By SUNIL.D
	 * svid and regionid.
	 * @param spid Value SPID
	 * @param lrn Value LRN
	 * @return boolean.
	 */

	public boolean isSvidLrn(Value spid, Value lrn){

		boolean result = false;

		Value portingTn =new Value(getContextValue("portingTn").toString()); 

		return isNpaNxxLrn(portingTn, spid, lrn, null, null);			

	}			  


	/** created By Sunil.D
	 * Checks if the onspduedate is null for SV Modify request
	 * @return boolean 
	 */

	public boolean isOnspDueDateSvidNull(){

		boolean result = true;
		String spid = null;
		String nnsp = null;
		String onsp = null;
		Boolean existSv=(Boolean)getContextValue("existSv");

		Date onspDueDate =(Date)getContextValue("onspduedateSvId");
		spid = (String)getContextValue("spidSvId");
		nnsp = (String)getContextValue("nnspSvId");
		onsp = (String)getContextValue("onspSvId"); 
		if (existSv.booleanValue() && onspDueDate == null  && spid.equals(onsp)  && !nnsp.equals(onsp)){
			result = false;					 
		}					  

		return result;	
	}


	/** created By Sunil.D
	 * Checks if the NNSPduedate is null for SV Modify request
	 * @return boolean 
	 */

	public boolean isNnspDueDateSvidNull(){

		boolean result = true;				
		String spid = null;
		String nnsp = null;
		String onsp = null;

		Boolean existSv=(Boolean)getContextValue("existSv");

		Date nnspDueDate =(Date)getContextValue("nnspduedateSvId");				  
		spid = (String)getContextValue("spidSvId");
		nnsp = (String)getContextValue("nnspSvId");
		onsp = (String)getContextValue("onspSvId");
		if (existSv.booleanValue() && nnspDueDate == null && spid.equals(nnsp) && !nnsp.equals(onsp)){

			result = false;
		} 

		return result;
	}




	/** created By Virendra
	 * Checks if the onspduedate is null for SV Modify request
	 * @return boolean 
	 */

	public boolean isOnspDueDateTnNull(){

		boolean result = false;

		Date dueDate = null;

		String spid = null;

		String nnsp = null;

		String onsp = null;

		String portingTn = null;

		String failedTn = null;

		StringBuffer errorMessage = new StringBuffer();		

		ArrayList indTns = new ArrayList();

		ArrayList failedTns = new ArrayList();

		ArrayList svDataList = null;

		boolean existSvDataList = false;

		if (existsValue("tnList"))
		{			
			indTns =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"existsModifySv tnList"+indTns);

		}

		for (int i=0;i < indTns.size();i++ )
		{
			failedTns.add(indTns.get(i));

		}

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
				{
					existSvDataList = true;
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
					dueDate =(Date)svData.onspDate;
					nnsp = svData.nnsp;
					onsp = svData.onsp;
					spid = (String)getContextValue("initSpid");	
	
					if ((dueDate != null) || (spid.equals(nnsp) && !nnsp.equals(onsp)) || (nnsp.equals(onsp)))					
						failedTns.remove(portingTn.toString());
						
						
				}
			}
		}


		ArrayList svDataList1 = null;

		if (existsValue("tnList"))
		{			
			svDataList1 =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"existsModifySv tnList "+svDataList1);

		}
		if (existSvDataList)
		{
			if (failedTns.isEmpty())
			{
				result = true;
			}
			else
			{
				for (int j=0;j < failedTns.size();j++ )
				{
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"Inside for Loop: isOnspDueDateTnNull ");

					failedTn = (String)failedTns.get(j);

					addFailedTN(failedTn);
					errorMessage.append("["+failedTn+"] ");

				}
				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		else
		{
			result = true;
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for existsModifySv:isOnspDueDateTnNull "+result);

		return result;
	}


	/** created By Virendra
	 * Checks if the NNSPduedate is null for SV Modify request
	 * @return boolean 
	 */

	public boolean isNnspDueDateTnNull(){

		boolean result = false;

		Date dueDate = null;

		String spid = null;

		String nnsp = null;

		String onsp = null;

		String portingTn = null;

		String failedTn = null;

		StringBuffer errorMessage = new StringBuffer();		

		ArrayList indTns = new ArrayList();

		ArrayList failedTns = new ArrayList();

		ArrayList svDataList = null;

		boolean existSvDataList = false;

		if (existsValue("tnList"))
		{			
			indTns =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"existsModifySv tnList"+indTns);

		}

		for (int i=0;i < indTns.size();i++ )
		{
			failedTns.add(indTns.get(i));

		}

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");

			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
				{
					existSvDataList = true;
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
					dueDate =(Date)svData.nnspDate;
					nnsp = svData.nnsp;
					onsp = svData.onsp;
					spid = (String)getContextValue("initSpid");	
					
					if ((dueDate != null)|| (spid.equals(onsp) && !nnsp.equals(onsp)))					

						failedTns.remove(portingTn.toString());
						
				}
			}
		}


		ArrayList svDataList1 = null;

		if (existsValue("tnList"))
		{			
			svDataList1 =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"existsModifySv tnList isNnspDueDateTnNull"+svDataList1);

		}
		if (existSvDataList)
		{
			if (failedTns.isEmpty())
			{
				result = true;
			}
			else
			{
				for (int j=0;j < failedTns.size();j++ )
				{
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"Inside for Loop: isNnspDueDateTnNull");		

					failedTn = (String)failedTns.get(j);

					addFailedTN(failedTn);
					errorMessage.append("["+failedTn+"] ");

				}
				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		else
		{
			result = true;
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for existsModifySv:isNnspDueDateTnNull "+result);

		return result;
	}

	/**  created By Sunil.D
	 * This method is used to check for the status is Cancel-Pending.
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isSPCancelPendingPresent(){

		boolean result = true;

		String status = null;		

		status =getContextValue("statusSvId").toString();

		if (status.equals("cancel-pending")){

			if(present(SOAConstants.CONTEXT_ROOT_NODE_LRN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_NNSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPAUTHORIZATION) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCVALUE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_BILLINGID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CAUSECODE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ALTERNATIVESPID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_VOICEURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_MMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_POCURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_PRESURI)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_NNSPSIMPLEPORTINDICATOR) ||
				    present(SOAConstants.CONTEXT_ROOT_NODE_ONSPSIMPLEPORTINDICATOR) ||
				    present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID)){

				result= false;
			}

		}

		return result ;
	}

	/**  created By Sunil.D
	 * This method is used to check if the onsp is spid And Status .
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isOldSPPendingConflictPresent(Value spid,
			String[] states){

		boolean result = true;

		String status = null;

		String onsp = null;		

		String nnsp = null;

		status =getContextValue("statusSvId").toString();

		Value valueStatus = new Value (status);

		onsp =getContextValue("onspSvId").toString();	 

		nnsp =getContextValue("nnspSvId").toString();

		if(onsp.equals(spid.toString()) && valueStatus.isMember(states)
				&& !nnsp.equals(onsp) ){		 

			if(present(SOAConstants.CONTEXT_ROOT_NODE_LRN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_NNSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCVALUE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_BILLINGID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVSTATUS) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ALTERNATIVESPID)||
					present(SOAConstants.CONTEXT_ROOT_NODE_VOICEURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_MMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_POCURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_PRESURI)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID)){

				result= false;
			}
		}	

		return result;
	}

	/**  created By Sunil.D
	 * This method is used to check NewSPActive.
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isNewSPActivePresent(Value spid){

		boolean result = true;

		String status = null;

		String nnsp = null;		 

		status =getContextValue("statusSvId").toString();

		nnsp =getContextValue("nnspSvId").toString();

		if ( nnsp.equals(spid.toString()) && status.equals("active") ){  		

			if(present(SOAConstants.CONTEXT_ROOT_NODE_ONSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPAUTHORIZATION) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CAUSECODE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_NNSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVSTATUS) ) {

				result=false;

			}
		}		 

		return result ;

	}

	/**  created By Sunil.D
	 * This method is used to check if there is any data present.
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isNewSPPTOFalsePresent(Value spid ,String[] states){

		boolean result = true;

		String nnsp = null;

		String onsp = null;

		String status = null;

		boolean pto= ((Boolean)getContextValue("portingToOriginal")).booleanValue();

		nnsp =getContextValue("nnspSvId").toString();

		onsp =getContextValue("onspSvId").toString();

		status =getContextValue("statusSvId").toString();

		Value valueStatus = new Value (status);

		if ( nnsp.equals(spid.toString()) && valueStatus.isMember(states) 
				&& !pto && !nnsp.equals(onsp)){

			if(present(SOAConstants.CONTEXT_ROOT_NODE_ONSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPAUTHORIZATION)||
					present(SOAConstants.CONTEXT_ROOT_NODE_CAUSECODE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVSTATUS) ) {

				result=false;
			}

		}	

		return result ;
	}



	/**  created By Sunil.D
	 * This method is used to check if there is any data 
	 * present(NewSPPTOTrue).
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isNewSPPTOTruePresent(Value spid ,String[] states){

		boolean result = true;

		String nnsp = null;

		String onsp = null;

		String status = null;		

		boolean pto = ((Boolean)getContextValue("portingToOriginal")).booleanValue();

		nnsp = getContextValue("nnspSvId").toString();

		onsp = getContextValue("onspSvId").toString();

		status =getContextValue("statusSvId").toString();

		Value valueStatus = new Value (status);

		if( nnsp.equals(spid.toString()) && valueStatus.isMember(states) 
				&& pto && !onsp.equals(nnsp)){ 	

			if(present(SOAConstants.CONTEXT_ROOT_NODE_LRN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPAUTHORIZATION)||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCVALUE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_BILLINGID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CAUSECODE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVSTATUS) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ALTERNATIVESPID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_VOICEURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_MMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_POCURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_PRESURI)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID)){

				result= false;
			}
		}

		return result ;
	}



	/**  created By Sunil.D
	 * This method is used to check if there is any data present.
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isIntraPTOTruePresent(){

		boolean result = true;

		String onsp = null;

		String nnsp = null;

		String status = null;

		status =getContextValue("statusSvId").toString();

		onsp =(String)getContextValue("onspSvId").toString();

		nnsp =(String)getContextValue("nnspSvId").toString();

		boolean pto = ((Boolean)getContextValue("portingToOriginal")).booleanValue();					

		if ( status.equals("pending") && pto 
				&& onsp.equals(nnsp) ){

			if(present(SOAConstants.CONTEXT_ROOT_NODE_LRN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPAUTHORIZATION)||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCVALUE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_BILLINGID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CAUSECODE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVSTATUS) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ALTERNATIVESPID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_VOICEURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_MMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_POCURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_PRESURI)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID)){

				result= false;
			} 
		}

		return result ;
	}



	/**  created By Sunil.D
	 * This method is used to check if there is any data present(IntraPTOFalse)
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isIntraPTOFalsePresent(){

		boolean result = true;

		String onsp = null;

		String nnsp = null;

		String status = null;		

		status =getContextValue("statusSvId").toString();

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Status returned from context: "+status);

		onsp =(String)getContextValue("onspSvId").toString();

		nnsp =(String)getContextValue("nnspSvId").toString();

		boolean pto = ((Boolean)getContextValue("portingToOriginal")).booleanValue();				 

		if ( status.equals("pending") && !pto 
				&& onsp.equals(nnsp) ){

			if(present(SOAConstants.CONTEXT_ROOT_NODE_ONSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPAUTHORIZATION) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CAUSECODE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CUSTOMERDISCONNECTDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_EFFECTIVERELEASEDATE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVSTATUS) ) {

				result= false;
			}

		}	

		return result ;
	}

	/**  created By Sunil.D
	 * This method is used to check if the status is disconnect-pending.
	 * And if present will return false 
	 * @return boolean 
	 */

	public boolean isNewSPDisconnectPresent(Value spid){

		boolean result = true;

		String status = null;

		String nnsp = null;			

		status =getContextValue("statusSvId").toString();			 

		nnsp =getContextValue("nnspSvId").toString();					

		if ( status.equals("disconnect-pending") 
				&& nnsp.equals(spid.toString()) ){  						   

			if(present(SOAConstants.CONTEXT_ROOT_NODE_LRN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_NNSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPDUEDATE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ONSPAUTHORIZATION) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CLASSSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LIDBSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ISVMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CNAMSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCDPC) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_WSMSCSSN) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCVALUE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ENDLOCTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_BILLINGID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_CAUSECODE)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVSTATUS) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_SVTYPE) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_ALTERNATIVESPID) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_VOICEURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_MMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_POCURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_PRESURI)||
					present(SOAConstants.CONTEXT_ROOT_NODE_SMSURI) ||
					present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID)){

				result= false;

			}
		}

		return result ;
	}


	/**
	 * Created By SUNIL.D
	 * This method retrieves the status of the subscription version
	 * with the given SVID in the given region when the onsp equals InitSPID.
	 * This method is used to Check the status.
	 */   

	public boolean getStatusOldSP(Value spid, String[] states){

		boolean result = true;

		String status = null;		

		String onsp = null;

		String nnsp = null;

		/* Check if query has been already executed earlier and result 
		   is available in context.*/

		onsp =getContextValue("onspSvId").toString();

		nnsp =getContextValue("nnspSvId").toString();

		status =getContextValue("statusSvId").toString();

		Value valueStatus = new Value (status);

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Status from context:"+ valueStatus);

		if (!onsp.equals(nnsp)){
			if (onsp.equals(spid.toString())){

				if(valueStatus.isMember(states))						
					result = true;
				else
					result = false;
			}
		}

		return result;	

	}

	/**
	 * Created By SUNIL.D
	 * This method checks fro multiple conflicts 
	 * @returns Boolean
	 */  

	public boolean getMultipleConflicts(Value oldSpAuthorization , Value causeCode){

		String status = null;
		String causecode = null;

		boolean result = true;				 

		status =getContextValue("statusSvId").toString();
		causecode =getContextValue("causeCode").toString();
		if(causecode != null){
			if ((oldSpAuthorization.equals("false") || oldSpAuthorization.equals("0"))&& status.equals("conflict") && !causeCode.equals(causecode)){
				result = false;
			}
		}

		return result;
	}

	/**
	 * Created By SUNIL.D
	 * This method checks if the authorizationFlag is falseand 
	 * returns a boolean false 
	 * when only the status is in conflict
	 * @returns Boolean
	 */   

	public boolean getAuthorizationFlag(Value spid,Value oldSpAuthorization){

		String status = null;

		boolean result = true;			

		String onsp = null;

		status =getContextValue("statusSvId").toString();

		onsp =getContextValue("onspSvId").toString();					

		if(oldSpAuthorization.equals("true") || 
				oldSpAuthorization.equals("1") && onsp.equals(spid.toString()) 
				&& status.equals("pending")){

			result = false;
		}

		return result;
	}


	/**
	 * This method is used to check NPA NXX Effetive Date
	 * @param tn
	 * @param effectiveDate
	 * @return boolean NPA NXX Effetive Date is valid or not
	 */

	public boolean isNpaNxxEffectiveDateSvidValid(Value effectiveDate){

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		Value tn =new Value(getContextValue("portingTn").toString()); 												

		boolean result = false;

		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, isNpaNxxEffectiveDateSvidValid :\n" + SOAQueryConstants.VALIDATE_NPA_NXX_EFFECTIVE_DATE_QUERY);

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

	/**Created By RAMESH CHIMATA
	 * This method is used to check the status change CauseCode
	 * of the subscription version of TN with the given spid.
	 * @return boolean.
	 */
	public boolean iscauseCodeTn(Value spid){

		boolean result = true;	

		String portingTn = null;

		String causeCode = null;

		String nnsp = null;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("rsDataList"))
		{
			svDataList =(ArrayList)getContextValue("rsDataList");
			if(svDataList != null)
			{
			for (int i=0;i < svDataList.size();i++ )
				{
					ResultsetData svData =(ResultsetData)svDataList.get(i);
	
					if(svData != null)
					{
						portingTn = svData.portingTn;
	
						causeCode = svData.causeCode;
	
						nnsp = svData.nnsp;
	
					}
	
					if(nnsp.equals(spid.toString())){
	
						if (causeCode != null && (causeCode.equals(
								SOAConstants.LSR_NOT_RECEIVED) || causeCode.equals(
										SOAConstants.FOC_NOT_ISSUED)))
						{
	
							errorMessage.append("A request can be sent only");
							errorMessage.append(
							" when the Status Change Cause Code for the " );
							errorMessage.append(
									"subscription version is not equal to  " +
									"["+SOAConstants.LSR_NOT_RECEIVED+" ] ");
							errorMessage.append(
									"Or ["+SOAConstants.FOC_NOT_ISSUED+"]");						
							addFailedTN(portingTn);
							errorMessage.append("["+portingTn+"] ");
							result = false;
						}
	
					}
				}
			}
		}

		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}


		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for causeCodeTn: "+result);

		return result;


	}



	/**
	 * This method determines if the given date2 is on
	 * the same day or later than the given date.
	 * 
	 * @return boolean, result.
	 */
	public boolean isDateLaterThanTn() {

		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");

		Date currentDate = new Date();

		Date dueDate = null;

		boolean result = true;

		String nnspduedate = null;

		ArrayList svDataList = null;

		String portingTn = null;

		StringBuffer errorMessage = new StringBuffer();

		Date date = null;

		Date objectCreationDate = null;
		long currentDateTime = 0L;
		long dueDateTime = 0L;

		if (existsValue("rsDataList")) {
			
			svDataList = (ArrayList) getContextValue("rsDataList");
			
			if(svDataList != null){
				
					for (int i = 0; i < svDataList.size(); i++) {
					ResultsetData svData = (ResultsetData) svDataList.get(i);
	
					if (svData != null) {
						portingTn = svData.portingTn;
						date = svData.nnspDueDate;
						objectCreationDate = svData.objectCreationDate;
					}
	
					if (date != null) {
						nnspduedate = dateFormat.format(date);
	
					}
	
					if (nnspduedate != null) {
						try {
							dueDate = dateFormat.parse(nnspduedate);
	
						} catch (ParseException ex) {
							return false;
						}
	
					}
					if (currentDate == null || dueDate == null
							|| objectCreationDate == null) {
	
						if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {
	
							Debug.log(Debug.RULE_EXECUTION,
									"Could not compare dates [" + currentDate
											+ "] " + "and [" + dueDate + "].");
						}
	
						return true;
					} else {
						currentDateTime = currentDate.getTime();
	
						dueDateTime = dueDate.getTime();
	
						result = currentDateTime > dueDateTime;
	
						if (!result) {
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
						}
					}
				}
			}
		}
		
		if (errorMessage != null && errorMessage.length() > 0) {
			setDynamicErrorMessage(errorMessage.toString());
			result = false;
		}

		return result;
	}



	/**
	 * This method is used to check NNSP Due Date is Null in SOA_SUBSCRIPTION_VERSION table		 
	 * @return boolean.
	 */


	public boolean isDueDateNull(){						

		Date dueDate =null;

		Date objectCreationDate = null;

		boolean result = true;

		ArrayList svDataList = null;

		String portingTn = null;	

		StringBuffer errorMessage = new StringBuffer();				

		if (existsValue("rsDataList"))
		{
			svDataList =(ArrayList)getContextValue("rsDataList");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )	
				{
					ResultsetData svData =(ResultsetData)svDataList.get(i);
	
					if(svData != null)
					{										
						portingTn = svData.portingTn;
						dueDate = svData.nnspDueDate;
						objectCreationDate = svData.objectCreationDate;
	
					}					
					
					if( dueDate == null || objectCreationDate == null ){
	
						if (Debug.isLevelEnabled(Debug.RULE_EXECUTION)) {
							
							Debug.log(Debug.RULE_EXECUTION,
									"NNSP Due Date ["+ dueDate + "] and Object Creation Due Date ["+objectCreationDate+"]");					
						}
						addFailedTN(portingTn);							
						errorMessage.append("[" + portingTn + "] ");
					}
				}	
			}
		}
		
		if (errorMessage != null && errorMessage.length() > 0) {
			setDynamicErrorMessage(errorMessage.toString());
			result = false;
		}	   

		return result;

	}

	public boolean isDueDateSvidNull(){				

		boolean result = true;

		Date nnspduedate = (Date)getContextValue("nnspduedateSvId");

		Date objectCreationDate = (Date)getContextValue("objectCreationDate");

		Debug.log(Debug.RULE_EXECUTION,
				"isDueDateSvidNull Due Date ["+ nnspduedate + "].");

		if( nnspduedate == null || objectCreationDate == null )
		{

			if (Debug.isLevelEnabled(Debug.RULE_EXECUTION))
			{

				Debug.log(Debug.RULE_EXECUTION,
						"NNSP Due Date ["+ nnspduedate + "].");
			}

			result = false;

		}								   

		return result;

	}
	/**
	 * This method is used to check the status of lrn based on NpaNxx
	 *
	 * @param NpaNxx Value
	 * @param rangeTn Value
	 * @param spid Value SPID.
	 * @param lrn Value LRN.
	 * @return boolean.
	 */   
	public boolean isNpaNxxLrn(Value npaNxx, Value rangeTn, Value spid,	Value lrn)
	{     
		boolean result = true;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String npa = null;

		String nxx = null;

		String npaNxxVal = npaNxx.toString();

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"0 npaNxx.toString():"+ npaNxxVal);

		if (npaNxxVal == null || npaNxxVal == "")
		{
			nxx = rangeTn.toString().substring(4,7);

		}
		else
		{

			if(npaNxx.length() > 7){

				npa = npaNxx.toString().substring(0,3);

				nxx = npaNxx.toString().substring(4,7);


			}
			else{

				npa = npaNxx.toString().substring(0,3);

				nxx = npaNxx.toString().substring(3,6);

			}

		}


		String npanxx = npa + nxx;

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"4 npaNxx.length() > 7:"+ npanxx);

		StringBuffer errorMessage=new StringBuffer();

		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, isNpaNxxLrn :\n" + SOAQueryConstants.GET_LRN);

			pstmt = conn.prepareStatement(SOAQueryConstants.GET_LRN);

			pstmt.setString( 1, spid.toString());   

			pstmt.setString( 2, npanxx );

			results = pstmt.executeQuery();		 		   

			while( results.next() ){

				if (results.getString(1).equals(lrn.toString())){
					result = true;
					break;
				}
				else{					
					errorMessage.append("[" + results.getString(1) + "]");				   
					result = false;			 
				}			 
			}

		}catch(Exception exception){

			logErrors(exception, SOAQueryConstants.GET_LRN);

		}finally{

			close(results, pstmt); 

		}

		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}

		return result;	      
	}

	/**
	 * Checks to see whether existing SV record for the NNSP.
	 *
	 * @param onsp Value the nnsp to query for.
	 * @param nnsp the nnsp to query for.
	 * @return boolean whether the given nnsp is the new service provider.
	 */
	public boolean isCheckStatus(Value onsp,Value nnsp){

		boolean result = true;

		String portingTn = null;

		StringBuffer errorMessage = new StringBuffer();

		String onspCon = null;

		String nnspCon = null;

		String tnStatus = null;

		String oldSP = onsp.toString();

		String newSP = nnsp.toString();

		ArrayList svDataList = null;

		if (existsValue("createSvRs"))
		{
			svDataList =(ArrayList)getContextValue("createSvRs");		
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					CreateSvData svData =(CreateSvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					onspCon = svData.onsp;
	
					nnspCon = svData.nnsp;
	
					tnStatus = svData.status;
	
					if (tnStatus.equals("pending") || tnStatus.equals("conflict") )
					{
						if (!(onspCon.equals(oldSP) && nnspCon.equals(newSP)) )
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
	
				}
			}
		}

		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}			

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isCheckStatus: "+result);

		return result;

	}

	/**
	 * This gets the status of the most recent
	 * subscription version with the given telephone
	 * number.
	 * @param status Value
	 * @return boolean for current status of the SV.
	 */
	public boolean getTnStatus(String[] status){

		boolean result = true;

		Value tnStatus = null;

		String portingTn = null;

		StringBuffer errorMessage = new StringBuffer();		

		ArrayList svDataList = null;

		if (existsValue("createSvRs"))
		{	
			svDataList =(ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					CreateSvData svData =(CreateSvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = new Value( svData.status );
	
					if (!tnStatus.isMember(status))
					{
						addFailedTN(portingTn);
						errorMessage.append("["+portingTn+"] ");
						result = false;
					}			
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());


		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for getStatus: "+result);

		return result;

	}


	/**
	 * This method is used to check the status of the NANCFlag whether 
	 * it returns value or not  
	 * @param spid Value the SPID 
	 * @return String Value.
	 */   
	public String getNANC399Flag(Value spid){   	  

		String nancflag =(String)getContextValue("nanc399flag");		

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Executing getNANC399: "+nancflag);

		return nancflag;

	}
	/**
	 * This method is used to check the status of the AltBIDNANC436Flag whether 
	 * it returns true or false  
	 * @param spid Value the SPID
	 * @return boolean Value
	 */
	public boolean isAltBIDNANC436Flag(Value spid){   	  

		Boolean altbidnanc436Value =(Boolean)getContextValue("altbidnanc436flag");		

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Executing isAltBIDNANC436Flag: "+altbidnanc436Value);

		boolean altbidnanc436flag = altbidnanc436Value.booleanValue();
		return altbidnanc436flag;

	}
	/**
	 * This method is used to check the status of the AltEULTNANC436Flag whether 
	 * it returns true or false  
	 * @param spid Value the SPID
	 * @return boolean Value
	 */
	public boolean isAltEULTNANC436Flag(Value spid){   	  

		Boolean alteultnanc436Value =(Boolean)getContextValue("alteultnanc436flag");		

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Executing isAltEULTNANC436Flag: "+alteultnanc436Value);

		boolean alteultnanc436flag = alteultnanc436Value.booleanValue();
		return alteultnanc436flag;

	}
	/**
	 * This method is used to check the status of the AltEULVNANC436flag whether 
	 * it returns true or false  
	 * @param spid Value the SPID
	 * @return boolean Value
	 */
	public boolean istAltEULVNANC436flag(Value spid){   	  

		Boolean alteulvnanc436Value =(Boolean)getContextValue("alteulvnanc436flag");		

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Executing isAltEULVNANC436flag: "+alteulvnanc436Value);

		boolean alteulvnanc436flag = alteulvnanc436Value.booleanValue();
		return alteulvnanc436flag;

	}
	
	/**
	 * This method is used to check the status of the LastAltSPID flag whether 
	 * it returns true or false  
	 * @param spid Value the SPID
	 * @return boolean Value
	 */
	public boolean isLastAltSPIDflag(Value spid){   	  
		
		Boolean isLastAltSPIDflag =(Boolean)getContextValue("lastAltSPIDFlag");
		StringBuffer errorMessage=new StringBuffer();

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Executing lastAltSPIDFlag: "+isLastAltSPIDflag);
		
		errorMessage.append("[" + spid + "]");
		setDynamicErrorMessage(errorMessage.toString());
		
		boolean lastaltspidflag = isLastAltSPIDflag.booleanValue();
		return lastaltspidflag;

	}

	public boolean absentSvType(String requestType){             

		boolean result = false;

		String tn = null;
		
		String spid = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
		}

		if (requestType.equals("SvCreateRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/SvType");
		}

		if (requestType.equals("SvModifyRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/SvType");
		}

		if (!result)
		{
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);				
				addFailedTN(tn);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for absentSvType: "+tn);
							
			}
			spid = value ("/SOAMessage/UpstreamToSOA/UpstreamToSOAHeader/InitSPID").toString();
			errorMessage.append("[" + spid + "] ");
			setDynamicErrorMessage(errorMessage.toString());
		}	

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for absentSvType: "+result);		

		return result;
	}

	public boolean presentSvType(String requestType){             

		boolean result = false;
		String spid = null;
		String tn = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"tnList for presentSvType: "+tnList);			
				Debug.log(Debug.MSG_STATUS,"tnList for presentSvType: "+getContextValue("tnList"));
			}

		}

		if (requestType.equals("SvCreateRequest"))
		{
			result = present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/SvType");
		}

		if (requestType.equals("SvModifyRequest"))
		{
			result = present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/SvType");
		}

		if (!result)
		{
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for presentSvType: "+tn);

				addFailedTN(tn);

				
			}
			spid = value ("/SOAMessage/UpstreamToSOA/UpstreamToSOAHeader/InitSPID").toString();
			errorMessage.append("[" + spid + "] ");
			setDynamicErrorMessage(errorMessage.toString());
		}	

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for presentSvType: "+result);		

		return result;
	}

	public boolean absentAltSPID(String requestType){

		boolean result = false;
		String spid = null;
		String tn = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");			
		}

		if (requestType.equals("SvCreateRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/AlternativeSPID");
		}

		if (requestType.equals("SvModifyRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/AlternativeSPID");
		}

		if (!result)
		{
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for absentAltSPID: "+tn);

				addFailedTN(tn);
				
			}
			spid = value ("/SOAMessage/UpstreamToSOA/UpstreamToSOAHeader/InitSPID").toString();
			errorMessage.append("[" + spid + "] ");
			setDynamicErrorMessage(errorMessage.toString());
		}	

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for absentAltSPID: "+result);		

		return result;
	}   

	public boolean absentNANC400Fields(String requestType , String nancField){

		boolean result = false;

		String tn = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");			
		}

		String checkField = null;

		if(nancField.equals("VoiceURI"))
			checkField = "VoiceURI";
		else if(nancField.equals("MMSURI"))
			checkField = "MMSURI";
		else if(nancField.equals("PoCURI"))
			checkField = "PoCURI";
		else if(nancField.equals("PRESURI"))
			checkField = "PRESURI";
		else if(nancField.equals("SMSURI"))
			checkField = "SMSURI";

		if (requestType.equals("SvCreateRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/"+checkField);
		}

		if (requestType.equals("SvModifyRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/"+checkField);
		}

		if (!result)
		{
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for absentNANC400Fields: "+tn);

				addFailedTN(tn);
				errorMessage.append("[" + tn + "] ");
			}

			setDynamicErrorMessage(errorMessage.toString());
		}	

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for absentNANC400Fields: "+result);		

		return result;
	}   


	/**
	 * This method is used to check due date
	 * @param dueDate
	 * @return boolean due date is valid or not
	 */
	public boolean isDueDateValid(Value dueDate){             

		boolean result = true;

		String portingTn = null;

		String tnStatus = null;

		ArrayList tnList = new ArrayList();
		
		ArrayList tnList1 = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = new ArrayList();

		Date paramDate = null;

		Date currentDate = null;


		try
		{
			String systemDate = TimeZoneUtil.convert("LOCAL", "MM-dd-yyyy-hhmmssa", new Date());

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"Due Date:"+dueDate.toString());
				Debug.log(Debug.MSG_STATUS,"System Date:"+systemDate);
			}

			String convertedDueDate = TimeZoneUtil.convertTime(dueDate.toString());
			String convertedCurrentDate = TimeZoneUtil.convertTime(systemDate);

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"Converted GMT Due Date:"+convertedDueDate);
				Debug.log(Debug.MSG_STATUS,"Converted GMT System Date:"+convertedCurrentDate);
			}

			paramDate = TimeZoneUtil.parse("GMT", "MM-dd-yyyy", convertedDueDate);
			currentDate = TimeZoneUtil.parse("GMT", "MM-dd-yyyy", convertedCurrentDate);

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"GMTDueDate:"+paramDate);
				Debug.log(Debug.MSG_STATUS,"GMTCurrentDate:"+currentDate);
			}

		}
		catch(MessageException ex)
		{
			Debug.error("Could not parse dueDate ["+dueDate+"]: "+ex);
			return false;	
		}


		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");			
		}
		
		Iterator itr = tnList.iterator();
		while(itr.hasNext()){
			tnList1.add(itr.next());
		}
		boolean pushDueDate = false;
		
		if (existsValue("createSvRs"))
		{			
			svDataList =(ArrayList)getContextValue("createSvRs");

			if(svDataList.size() != 0)
			{			
				for (int i=0;i < svDataList.size();i++ )
				{
					CreateSvData svData =(CreateSvData)svDataList.get(i);

					portingTn = svData.portingTn;	
					
					
					tnStatus = svData.status;									
					
					tnList1.remove(portingTn);
					
					if (!(tnStatus.equals("pending") || 
							tnStatus.equals("conflict")) )
					{	
												
						if (paramDate.before(currentDate)){
							
							try {							
								String inputSource = (String)CustomerContext.getInstance().get("InputSource");
									
								if(inputSource != null && !inputSource.equals(SOAConstants.GUI_REQUEST)){
										
									if(!pushDueDate){
										pushDueDate = true;
										Integer pushDueDateMin = (Integer)getContextValue("pushDueDate");
											
										if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
											Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : [" + pushDueDateMin +"]");
											
										if( pushDueDateMin != null && pushDueDateMin >= 0){
											//modified the DueDate with SYSDATE+PUSHDUEDATE
											result = pushDueDate(pushDueDateMin);
										}else{
												
											addFailedTN(portingTn);
											result = false;
										}
									}
										
								}else{
										addFailedTN(portingTn);
										result = false;
								}
							} catch (FrameworkException e) {
									Debug.error("Could not get the Customer Context: "+e.getMessage());
									result = false;
							}
						}
					}
				}					
				if(tnList1.size() > 0){					
					
					if (paramDate.before(currentDate)){	
						
						try {
														
				       		String inputSource = (String)CustomerContext.getInstance().get("InputSource");
							
				       		if(inputSource != null && !inputSource.equals(SOAConstants.GUI_REQUEST)){
				       				
				       			if(!pushDueDate){
				       				
				       				pushDueDate = true;
					    			Integer pushDueDateMin = (Integer)getContextValue("pushDueDate");
									
					    			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					       				Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : [" + pushDueDateMin +"]");
									
					       			if(pushDueDateMin != null && pushDueDateMin >= 0){
					       				result = pushDueDate(pushDueDateMin);
					       			}else{
										for (int j = 0;j < tnList1.size() ;j++ ){
											
											String tn = (String)tnList1.get(j);
						
											addFailedTN(tn);
											result = false;
										}
					       			}
				       			}
								
				       		}else{
				       				for (int j = 0;j < tnList1.size() ;j++ ){
										String tn = (String)tnList1.get(j);
				
										addFailedTN(tn);
										result = false;
									}
				       		}
				       	} catch (FrameworkException e) {
				       			Debug.error("Could not get the Customer Context: "+e.getMessage());
				       			result = false;
				       		}
					}
				}
			}
			else
			{	
				if (paramDate.before(currentDate))			
				{	
					try {
																	
						String inputSource = (String)CustomerContext.getInstance().get("InputSource");
							
						if(inputSource != null && !inputSource.equals(SOAConstants.GUI_REQUEST)){
								
							if(!pushDueDate){
								pushDueDate = true;
								Integer pushDueDateMin = (Integer)getContextValue("pushDueDate");
								if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
									Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : 2[" + pushDueDateMin +"]");
										
								if( pushDueDateMin != null && pushDueDateMin >= 0){
									//modified the DueDate with SYSDATE+PUSHDUEDATE
									result = pushDueDate(pushDueDateMin);
								}else{
									String tn = null;
									for (int j = 0;j < tnList.size() ;j++ )
									{
										tn = (String)tnList.get(j);
					
										addFailedTN(tn);
											//errorMessage.append("[" + tn + "] ");
										result = false;
									}
								}
							}
						}else{
							String tn = null;
							for (int j = 0;j < tnList.size() ;j++ )
							{
								tn = (String)tnList.get(j);
			
								addFailedTN(tn);
								//errorMessage.append("[" + tn + "] ");
								result = false;
			
							}
						}
					} catch (FrameworkException e) {
							Debug.error("Could not get the Customer Context: "+e.getMessage());
							result = false;
					}
				}
			}

		}

		if( getFailedTNList() != null && !getFailedTNList().isEmpty() )
		{
			errorMessage.append( getContextValue("submittedTnList") );
		}
		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isDueDateValid: "+result);		

		return result;
	}
	/**
	 * This method is used to check due date
	 * @param dueDate
	 * @return boolean due date is valid or not
	 */
	public boolean isDueDateValidSvid(Value dueDate){

		boolean result = true;

		StringBuffer errorMessage = new StringBuffer();

		Boolean existSv=(Boolean)getContextValue("existSv");

		if (existSv.booleanValue())
		{
			result = dueDateValidation(dueDate);
		}
		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isDueDateValidSvid: "+result);

		return result;
	}
	/**
	 * This method is used to check due date
	 * @param dueDate
	 * @return boolean due date is valid or not
	 */
	public boolean dueDateValidation(Value dueDate){             

		boolean result = false;		

		Date paramDate = null;

		Date currentDate = null;
		try
		{
			String systemDate = TimeZoneUtil.convert("LOCAL", "MM-dd-yyyy-hhmmssa", new Date());

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"Due Date:"+dueDate.toString());
				Debug.log(Debug.MSG_STATUS,"System Date:"+systemDate);
			}

			String convertedDueDate = TimeZoneUtil.convertTime(dueDate.toString());
			String convertedCurrentDate = TimeZoneUtil.convertTime(systemDate);

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"Converted GMT Due Date:"+convertedDueDate);
				Debug.log(Debug.MSG_STATUS,"Converted GMT System Date:"+convertedCurrentDate);
			}

			paramDate = TimeZoneUtil.parse("GMT", "MM-dd-yyyy", convertedDueDate);
			currentDate = TimeZoneUtil.parse("GMT", "MM-dd-yyyy", convertedCurrentDate);

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"GMTDueDate:"+paramDate);
				Debug.log(Debug.MSG_STATUS,"GMTCurrentDate:"+currentDate);
			}				
		}
		catch(MessageException ex)
		{
			Debug.error("Could not parse dueDate ["+dueDate+"]: "+ex);
			return false;	
		}				

		if (!paramDate.before(currentDate))
			result = true;
		else{
			
			try{
				String inputSource = (String)CustomerContext.getInstance().get("InputSource");
				
				if(inputSource != null && !inputSource.equals(SOAConstants.GUI_REQUEST)){
					
					Integer pushDueDateMin = (Integer)getContextValue("pushDueDate");
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"The value of PUSHDATEFLAG : 1[" + pushDueDateMin +"]");
					
					if(pushDueDateMin != null && pushDueDateMin >= 0){
						//pushing the DueDate with PUSHDUEDATE
						result = pushDueDate(pushDueDateMin);
					}else{
						
						result = false;
					}
				}
				else{
					
				result = false;
				
				}
			}catch(FrameworkException e){
				Debug.error("Could not get the Customer Context: "+e.getMessage());
				result = false;
			}
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for dueDateValidation: "+result);		

		return result;
	}


	/**
	 * This method compares the oldSpDueDate with neSPDueDate
	 * @param oldSPDueDate
	 * @return boolean 
	 */
	public boolean isDueDateEqualsOldSPDueDate(Value newSPDueDate){

		boolean result = true;

		String portingTn = null;

		Date onspDate = null;

		Date nnspDate = null;

		String tnStatus = null;
		ArrayList<String> adjustDateTNList = null;

		long onspDbDate = 0;

		long nnspDbDate = 0;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		SimpleDateFormat dateFormat = 
			new SimpleDateFormat( "MM-dd-yyyy-hhmmssa" );

		try {				
			nnspDate =dateFormat.parse(newSPDueDate.toString());
		} 
		catch (ParseException ex) {
			Debug.error("Could not parse dueDate ["+newSPDueDate+"]: "+ex);
			return false;
		}

		if (nnspDate != null)
			nnspDbDate = nnspDate.getTime();

		if (existsValue("createSvRs"))
		{
			svDataList =(ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null)
			{
				Integer adjustDueDateHours = (Integer)getContextValue("adjustDueDate");				
				boolean adjustDueDateFlag = false;				
				if( adjustDueDateHours != null && adjustDueDateHours > 0)
				{
					adjustDueDateFlag = true;
					adjustDateTNList = new ArrayList<String>();
				}
				for (int i=0;i < svDataList.size();i++ )
				{
					CreateSvData svData =(CreateSvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					onspDate = svData.onspDate;
	
					if (onspDate != null)
						onspDbDate = onspDate.getTime();
	
					tnStatus = svData.status;
	
					if ( onspDate != null && (tnStatus.equals("pending") || tnStatus.equals("conflict")))
					{
						if (nnspDbDate != onspDbDate  )
						{						
							boolean adjustDueDate =false;	
							if (adjustDueDateFlag)									
							{
								int diffHours;
								if (nnspDbDate > onspDbDate)
									diffHours = (int) ((nnspDbDate - onspDbDate) / (1000 * 60 * 60));
								else
									diffHours = (int) ((onspDbDate - nnspDbDate) / (1000 * 60 * 60));								
								if (diffHours <= adjustDueDateHours) 
								{									
									adjustDateTNList.add(portingTn);
									adjustDueDate = true;
								}								
							}							
							if(!adjustDueDate)								
							{
								result = false;
								addFailedTN(portingTn);
								errorMessage.append("[" + portingTn + "] ");								
							}
						}					
					}	
					else
						result = true;
				}
			}
		}
		
		try {			
			if(adjustDateTNList !=null && adjustDateTNList.size()>0)
				CustomerContext.getInstance().set("adjustDueDateTNList",adjustDateTNList );
		} catch (FrameworkException e) {
			if ( Debug.isLevelEnabled( Debug.ALL_WARNINGS ))
				Debug.log(Debug.MSG_STATUS,"Error while setting adjustDueDateTNList" +  e.getMessage());
		}		

		if (errorMessage != null && errorMessage.toString().length() > 0)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isDueDateEqualsOldSPDueDate:"+result);

		return result;
	}

	/**
	 * Checks to see if the most recent active-like subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean newSPTn(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String nSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("createSvRs"))
		{
			svDataList =(ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					CreateSvData svData =(CreateSvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					if (tnStatus.equals("pending") || tnStatus.equals("conflict") )
					{				
						if (!nnsp.equals(nSpid) )
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}		

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isNewSPTn: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent active-like subscription version for
	 * the given TN has the given SPID as its old service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the old service provider.
	 */
	public boolean oldSPTn(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String onsp = null;

		String oSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("createSvRs"))
		{
			svDataList =(ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					CreateSvData svData =(CreateSvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					onsp = svData.onsp;
	
					if (tnStatus.equals("pending") || tnStatus.equals("conflict") )
					{
	
						if (!onsp.equals(oSpid) )
						{
							addFailedTN(portingTn);
							errorMessage.append("["+portingTn+"] ");
							result = false;
						}
					}
				}
			}
		}


		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isNewSPTn: "+result);

		return result;

	}

	/**
	 * This method compares the oldSpDueDate with neSPDueDate
	 * @param oldSPDueDate
	 * @return boolean 
	 */
	public boolean isDueDateEqualsNewSPDueDate(Value oldSPDueDate){

		boolean result = true;

		String portingTn = null;

		Date nnspDate = null;

		Date oldSPDate = null;

		long nnspDbDate = 0;

		long oldSPLongDate = 0;

		String tnStatus = null;
		ArrayList<String> adjustDateTNList = null;
		
		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		SimpleDateFormat dateFormat = 
			new SimpleDateFormat( "MM-dd-yyyy-hhmmssa" );

		try {				
			oldSPDate =dateFormat.parse(oldSPDueDate.toString());
		} 
		catch (ParseException ex) {
			Debug.error("Could not parse dueDate ["+oldSPDueDate+"]: "+ex);
			return false;
		}		
		if (oldSPDate != null)
			oldSPLongDate = oldSPDate.getTime();		

		if (existsValue("createSvRs"))
		{
			svDataList =(ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null)
			{
				Integer adjustDueDateHours = (Integer)getContextValue("adjustDueDate");				
				boolean adjustDueDateFlag = false;				
				if( adjustDueDateHours != null && adjustDueDateHours > 0)
				{
					adjustDueDateFlag = true;
					adjustDateTNList = new ArrayList<String>();
				}
				for (int i=0;i < svDataList.size();i++ )
					{				
					CreateSvData svData =(CreateSvData)svDataList.get(i);
					
					portingTn = svData.portingTn;
	
					nnspDate = svData.nnspDate;															
	
					if (nnspDate != null)
	
						nnspDbDate = nnspDate.getTime();
	
					tnStatus = svData.status;					
	
					if (nnspDate != null && (tnStatus.equals("pending")))						
					{	 												
						if (oldSPLongDate != nnspDbDate)
						{							
							boolean adjustDueDate =false;	
							if (adjustDueDateFlag)									
							{
								int diffHours;								
								if (nnspDbDate > oldSPLongDate)
									diffHours = (int) ((nnspDbDate - oldSPLongDate) / (1000 * 60 * 60)) ;
								else									
									diffHours = (int) ((oldSPLongDate - nnspDbDate) / (1000 * 60 * 60));								
								if (diffHours <= adjustDueDateHours) 
								{									
									adjustDateTNList.add(portingTn);
									adjustDueDate = true;									
								}																
							}							
							if(!adjustDueDate)
							{
								result = false;
								addFailedTN(portingTn);
								errorMessage.append("[" + portingTn + "] ");
							}
						}						
					}
					else
						result = true;					
				}
								
			}
			
		}
		
		try {			
			if(adjustDateTNList !=null && adjustDateTNList.size()>0)
				CustomerContext.getInstance().set("adjustDueDateTNList",adjustDateTNList );
		} catch (FrameworkException e) {
			if ( Debug.isLevelEnabled( Debug.ALL_WARNINGS ))
				Debug.log(Debug.MSG_STATUS,"Error while setting adjustDueDateTNList" +  e.getMessage());
		}				
		
		if (errorMessage != null && errorMessage.toString().length() > 0)
			setDynamicErrorMessage(errorMessage.toString());		

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isDueDateEqualsNewSPDueDate:"+result);

		return result;

	}



	/**
	 * This method  checks if  create request is already submitted 
	 * @return boolean 
	 */
	public boolean isAlreadyCreated( ){

		boolean result = true;

		String tnStatus = null;

		Date nnspDate = null;

		String portingTn = null;				

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("createSvRs"))
		{
			svDataList =(ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null)
			{			
				for (int i=0;i < svDataList.size();i++ )
					{
					CreateSvData svData =(CreateSvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					nnspDate = svData.nnspDate;
	
					tnStatus = svData.status;
	
	
					if ( nnspDate != null && (tnStatus.equals("pending") || tnStatus.equals("conflict")) )
	
					{
						result = false;
						addFailedTN(portingTn);
						errorMessage.append("[" + portingTn + "] ");
					}
				}
			}
		}

		if (errorMessage != null && errorMessage.toString().length() > 0)
			setDynamicErrorMessage(errorMessage.toString());			

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isAlreadyCreated:"+result);				

		return result;
	}

	/**
	 * This method  if a Release request is already submitted 
	 * @return boolean 
	 */
	public boolean isAlreadyReleased(){

		boolean result = true;

		String tnStatus = null;

		Date onspDate = null;

		String portingTn = null;					

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("createSvRs"))
		{
			svDataList =(ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					CreateSvData svData =(CreateSvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					onspDate = svData.onspDate;						
	
					tnStatus = svData.status;						
	
					if ( onspDate != null && (tnStatus.equals("pending") || tnStatus.equals("conflict")))
					{
						result = false;
						addFailedTN(portingTn);
						errorMessage.append("[" + portingTn + "] ");
					}
				}
			}
		}

		if (errorMessage != null && errorMessage.toString().length() > 0)
			setDynamicErrorMessage(errorMessage.toString());			

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"isAlreadyReleased:"+result);

		return result;
	}

	/**
	 * This gets the existence of the most recent
	 * subscription version.
	 *
	 * @return boolean the existence of the SV.
	 */
	public boolean existsModifySv(){

		boolean result = false;

		Value tnStatus = null;

		String portingTn = null;

		String failedTn = null;

		StringBuffer errorMessage = new StringBuffer();		

		ArrayList indTns = new ArrayList();

		ArrayList failedTns = new ArrayList();

		ArrayList svDataList = null;

		if (existsValue("tnList"))
		{			
			indTns =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"existsModifySv tnList"+indTns);

		}

		for (int i=0;i < indTns.size();i++ )
		{
			failedTns.add(indTns.get(i));

		}

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					int tnIndex = failedTns.indexOf(portingTn.toString());					
	
					if (tnIndex != -1)					
						failedTns.remove(portingTn.toString());
				}
			}
		}


		ArrayList svDataList1 = null;

		if (existsValue("tnList"))
		{			
			svDataList1 =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"existsModifySv tnList"+svDataList1);

		}

		if (failedTns.isEmpty())
		{
			result = true;
		}
		else
		{
			for (int j=0;j < failedTns.size();j++ )
			{
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Inside for Loop: ");		

				failedTn = (String)failedTns.get(j);

				addFailedTN(failedTn);
				errorMessage.append("["+failedTn+"] ");

			}
			setDynamicErrorMessage(errorMessage.toString());
		}



		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for existsModifySv: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent active-like subscription version for
	 * the given TN has the given SPID as its old service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the old service provider.
	 */
	public boolean isOldSPModify(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String onsp = null;

		String nnsp = null;

		String oSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;	

		ArrayList indTnList = null;

		if (existsValue("tnList"))
		{
			indTnList =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"isOldSPModify tnList"+indTnList);			
		}

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					onsp = svData.onsp;
	
					nnsp = svData.nnsp;
	
					if (!onsp.equals(nnsp))
					{
	
						if (onsp.equals(oSpid) )					
						{					
							if (!(tnStatus.equals("pending") || 
									tnStatus.equals("conflict") || 
									tnStatus.equals("cancel-pending")) )
							{
								addFailedTN(portingTn);
								errorMessage.append("[" + portingTn + "] ");
								result = false;
							}
						}
					}
	
	
				}
			}
		}


		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isOldSPModify: "+result);

		return result;

	}

	/**
	 * This gets the status of the most recent
	 * subscription version with the given telephone
	 * number.
	 * @param status Value
	 * @return boolean for current status of the SV.
	 */
	public boolean getModifyStatus(String[] status, String conflictFlag){

		boolean result = true;

		Value tnStatus = null;

		String portingTn = null;

		StringBuffer errorMessage = new StringBuffer();		

		ArrayList svDataList = null;

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = new Value( svData.status );
	
	
					if ( conflictFlag == null) {
	
						if (!tnStatus.isMember(status))					
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
					else {
						if (tnStatus.isMember(status))					
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}								
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for getStatus: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent active-like subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean validateStatusSvModify(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String mSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		ArrayList indTnList = null;

		if (existsValue("tnList"))
		{
			indTnList =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"validateStatusSvModify tnList"+indTnList);			
		}

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					if (nnsp.equals(mSpid) &&(tnStatus.equals("pending") ||
							tnStatus.equals("sending") || 
							tnStatus.equals("cancel-pending") ||
							tnStatus.equals("download-failed") ||
							tnStatus.equals("download-failed-partial") ||
							tnStatus.equals("disconnect-pending") ||
							tnStatus.equals("creating") || 
							tnStatus.equals("NPACCreateFailure")) )
					{
						addFailedTN(portingTn);
						errorMessage.append("[" + portingTn + "] ");
						result = false;
					}			
				}
			}
		}		

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isOldSPModify: "+result);

		return result;

	}

	/**
	 * This method compares the oldSpDueDate with current Date.
	 * @param oldSPDueDate
	 * @return boolean 
	 */
	public boolean isOnspDueDateNull(Value OldSPDueDate){

		boolean result = false;

		String portingTn = null;

		String failedTn = null;

		Date onspDate = null;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList indTns = new ArrayList();	

		ArrayList svDataList = null;	

		ArrayList indTnList = null;

		if (existsValue("tnList"))
		{
			indTnList =(ArrayList)getContextValue("tnList");

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"isOnspDueDateNull tnList"+indTnList);			
		}

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					onspDate = svData.onspDate;
	
					if (onspDate != null )
					{
						result = dueDateValidation(OldSPDueDate);
	
						if (!result)
						{
							indTns.add(portingTn);
						}
					}
				}	
			}
		}

		if (indTns.isEmpty())
		{
			result = true;
		}
		else
		{
			for (int j=0;j < indTns.size();j++ )
			{
				failedTn = (String)indTns.get(j);

				addFailedTN(failedTn);
				errorMessage.append("["+failedTn+"] ");

			}
			setDynamicErrorMessage(errorMessage.toString());
		}


		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isOnspDueDateNull: "+result);

		return result;

	}

	/**
	 * This method compares the neSPDueDate with current date.
	 * @param neSPDueDate
	 * @return boolean 
	 */
	public boolean isNnspDueDateNull(Value NewSPDueDate){

		boolean result = true;

		String portingTn = null;

		String failedTn = null;

		Date nnspDate = null;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;			

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
			for (int i=0;i < svDataList.size();i++ )
				{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					nnspDate = svData.nnspDate;
	
					if (nnspDate != null )
					{
						result = dueDateValidation(NewSPDueDate);
						if (!result)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
						}
					}
				}	
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isNnspDueDateNull: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent active-like subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean isNewSPActive(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String nSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");

			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
	
						Debug.log(Debug.MSG_STATUS,"isNewSPActive :"+tnStatus);
						Debug.log(Debug.MSG_STATUS,"isNewSPActive :"+nnsp);
						Debug.log(Debug.MSG_STATUS,"isNewSPActive :"+nSpid);
					}										
	
					if (tnStatus.equals("active") && nnsp.equals(nSpid) )
					{
						invalidElement =
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/NewSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvStatus");
	
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,"Result for isNewSPActive:"+result);
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isNewSPActive: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent disconnect-pending subscription version
	 * for the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean isNnspDP(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String nSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;	

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");

			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					if (tnStatus.equals("disconnect-pending") &&
							nnsp.equals(nSpid)  )
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/Lrn") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/NewSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationValue") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/BillingId") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvStatus") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/AlternativeSPID") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/VoiceURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/MMSURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PoCURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PRESURI") ||
						present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
						"/SvModifyRequest/DataToModify/SMSURI") ||
						present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID);
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,"Result for isNnspDP if: "+result);
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isNnspDP: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent cancel-pending subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean isOnspCP(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String onsp = null;

		String oSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					onsp = svData.onsp;
	
					if (tnStatus.equals("cancel-pending") && onsp.equals(oSpid) )
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/Lrn") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/NewSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationValue") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/BillingId") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/AlternativeSPID")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/VoiceURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/MMSURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PoCURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
						    "/SvModifyRequest/DataToModify/PRESURI") ||
						    present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
					        "/SvModifyRequest/DataToModify/SMSURI") ||
					        present(SOAConstants.CONTEXT_ROOT_NODE_ONSPSIMPLEPORTINDICATOR) ||
							present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID);
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isOnspCP:"+result);

		return result;

	}

	/**
	 * Checks to see if the most recent cancel-pending subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean isNnspCP(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String nSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;	

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					if (tnStatus.equals("cancel-pending") && nnsp.equals(nSpid))
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/Lrn") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/NewSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationValue") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/BillingId") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/AlternativeSPID")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/VoiceURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/MMSURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PoCURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PRESURI") ||
						present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
						"/SvModifyRequest/DataToModify/SMSURI") ||
						present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID);
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("["+portingTn+"] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isOnspCP: "+result);

		return result;

	}


	/**
	 * Checks to see if the most recent Intra port subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean.
	 */
	public boolean isIntraPTOTrue(){

		boolean result = true;	

		boolean pto = false;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String onsp = null;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;			

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");

			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					onsp = svData.onsp;
	
					pto = svData.pto;
	
					if (pto && tnStatus.equals("pending") && onsp.equals(nnsp))
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/Lrn") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationValue") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/BillingId") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvStatus") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/AlternativeSPID")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/VoiceURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/MMSURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PoCURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PRESURI") ||
						present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
						"/SvModifyRequest/DataToModify/SMSURI") || 
						present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID);
						
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());				

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isIntraPTOTrue: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent Inter port subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @return boolean.
	 */
	public boolean isIntraPTOFalse(){

		boolean result = true;	

		boolean pto = false;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String onsp = null;

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");

			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					onsp = svData.onsp;
	
					pto = svData.pto;
	
					if (!pto && tnStatus.equals("pending") && onsp.equals(nnsp))
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvStatus");
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isIntraPTOFalse: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent Intra Port  subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @param tn Value the TN to query for.
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean isNewSPPTOTrue(Value spid){

		boolean result = true;	

		boolean pto = false;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String onsp = null;

		String nSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;			

		if (existsValue("modifySvRs"))
		{

			boolean invalidElement = false;

			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					onsp = svData.onsp;
	
					pto = svData.pto;
	
					if (nnsp.equals(nSpid) && (tnStatus.equals("pending") || 
							tnStatus.equals("conflict")) && pto && !nnsp.equals(onsp))
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/Lrn") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationValue") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/BillingId") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvStatus") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/AlternativeSPID")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/VoiceURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/MMSURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PoCURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
						"/SvModifyRequest/DataToModify/PRESURI") ||
						present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
					    "/SvModifyRequest/DataToModify/SMSURI") ||
					    present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID);
						
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isNewSPPTOTrue: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent inter port subscription version for
	 * the given TN has the given SPID as its new service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @param tn Value the TN to query for.
	 * @return boolean whether the given SPID is the new service provider.
	 */
	public boolean isNewSPPTOFalse(Value spid){

		boolean result = true;	

		boolean pto = false;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String onsp = null;

		String nSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;			

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");

			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					onsp = svData.onsp;
	
					pto = svData.pto;
	
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"pto value:"+pto);
	
					if (nnsp.equals(nSpid) && (tnStatus.equals("pending") || 
							tnStatus.equals("conflict")) && !pto && !onsp.equals(nnsp))
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/OldSPAuthorization") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CauseCode") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
									"/SvModifyRequest/DataToModify/" +
							"CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvStatus");
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for isNewSPPTOFalse: "+result);

		return result;

	}

	/**
	 * Checks to see if the most recent cancel-pending subscription version for
	 * the given TN has the given SPID as its old service provider.
	 *
	 * @param spid Value is this SPID the new service provider for this SPID's
	 *             SV record?
	 * @param tn Value the TN to query for.
	 * @return boolean whether the given SPID is the old service provider.
	 */
	public boolean isOldSPC(Value spid){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String onsp = null;

		String oSpid = spid.toString();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;		

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");

			boolean invalidElement = false;
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
	
					portingTn = svData.portingTn;
	
					tnStatus = svData.status;
	
					nnsp = svData.nnsp;
	
					onsp = svData.onsp;
	
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
	
						Debug.log(Debug.MSG_STATUS,"isOldSPC tnStatus: "+tnStatus);
						Debug.log(Debug.MSG_STATUS,"isOldSPC nnsp: "+nnsp);
						Debug.log(Debug.MSG_STATUS,"isOldSPC onsp: "+onsp);
					}
	
					if (onsp.equals(oSpid) && (tnStatus.equals("pending") || 
							tnStatus.equals("conflict")) && !nnsp.equals(oSpid)  )
					{
						invalidElement = 
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/Lrn") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/NewSPDueDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/ClassSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/LidbSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/IsvmSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/CnamSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscDPC") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/WsmscSSN") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
									"/SvModifyRequest/DataToModify" +
							"/EndUserLocationValue") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EndUserLocationType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/BillingId") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
									"/SvModifyRequest/DataToModify/" +
							"CustomerDisconnectDate") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/EffectiveReleaseDate")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvStatus") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/SvType") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/AlternativeSPID")||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/VoiceURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/MMSURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PoCURI") ||
							present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
							"/SvModifyRequest/DataToModify/PRESURI") ||
						present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody" +
						"/SvModifyRequest/DataToModify/SMSURI") || 
						present(SOAConstants.CONTEXT_ROOT_NODE_LASTALTERNATIVESPID);
	
						if (invalidElement)
						{
							addFailedTN(portingTn);
							errorMessage.append("["+portingTn+"] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isOldSPC: "+result);

		return result;

	}

	/**
	 * This queries for the porting tn for SV id
	 * @param spid
	 * @param svid
	 * @param regionid
	 * @return Value 
	 */
	public Value getPortingTN(Value spid, Value svid, Value regionid){

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String pto = "";

		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, getPortingTN :\n" + SOAQueryConstants.GET_PORTING_TN_QUERY);

			pstmt = conn.prepareStatement(SOAQueryConstants.GET_PORTING_TN_QUERY);

			pstmt.setString( 1, spid.toString());

			pstmt.setString( 2, svid.toString() );

			pstmt.setString( 3, regionid.toString() );

			results = pstmt.executeQuery();

			if(results.next()){

				pto = results.getString(1);                               
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"getPortingTN value is: "+pto);

		}catch(Exception exception){

			logErrors(exception, SOAQueryConstants.GET_PORTING_TN_QUERY);


		}finally{

			close(results, pstmt); 
		}

		return new Value(pto);
	} 

	/**
	 * This queries the SOA_NBRPOOL_BLOCK to  retrieve npa and nxx for the npbid 
	 * and regionId
	 * @param npbid	
	 * @param regionid
	 * @return string
	 */

	public String getnpaNxxvalue(Value npbid , Value regionid){

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String npa = "";

		String nxx = "";			

		StringBuffer npaNxx = new StringBuffer();

		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, getnpaNxxvalue :\n" + SOAQueryConstants.IS_NPB_NPANXX_QUERY);

			pstmt = conn.prepareStatement(SOAQueryConstants.IS_NPB_NPANXX_QUERY);			  

			pstmt.setString( 1, npbid.toString() );

			pstmt.setString( 2, regionid.toString() );

			results = pstmt.executeQuery();

			if(results.next()){

				npa =results.getString(1);

				nxx=  results.getString(2);				  

			}	
			npaNxx.append(npa.toString());
			npaNxx.append(nxx.toString());			   

		}catch(Exception exception){

			logErrors(exception, SOAQueryConstants.IS_NPB_NPANXX_QUERY);

		}finally{

			close(results, pstmt);
		}

		return npaNxx.toString();
	}

	/**
	 * This method is used to check the status of the accountid is 
	 * exists or not 
	 * @param accountId. 
	 * @param spid Value SPID. 
	 * @return boolean.
	 */   
	public boolean isAccountIdExists(Value accountId, Value spid){

		boolean result = false;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String portingTn = null;

		StringBuffer errorMessage = new StringBuffer();		

		ArrayList svDataList = null;


		try{

			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, isAccountIdExists :\n" + SOAQueryConstants.EXISTS_ACCOUNTID);

			pstmt = conn.prepareStatement(SOAQueryConstants.EXISTS_ACCOUNTID);

			pstmt.setString( 1, accountId.toString() );
			pstmt.setString( 2, spid.toString() );	

			results = pstmt.executeQuery();

			if( results.next() ){

				result = true;


			}else if (existsValue("tnList")){

				svDataList =(ArrayList)getContextValue("tnList");
				if(svDataList != null)
				{
					for (int i=0;i < svDataList.size();i++ )
					{					
						portingTn = (String)svDataList.get(i);					
						addFailedTN(portingTn);
						errorMessage.append("["+portingTn+"] ");
	
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,"Failed portingTn is:"+ portingTn);
					}
				}
				setDynamicErrorMessage(errorMessage.toString());
			}

		}catch(Exception exception){
			logErrors(exception, SOAQueryConstants.EXISTS_ACCOUNTID);

		}finally{

			close(results, pstmt); 
		}


		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Account Id is deleted :"+ accountId);

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result is:"+ result);

		return result;	      
	}

	/**
	 * This method is used to check the true combination of Accountid and Account name
	 * @param accountId.
	 * @param accountName.
	 * @return boolean.
	 */
	public boolean isValidAccountIdAccountName(Value accountId, Value accountName){

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet results = null;

		try{
			conn = getDBConnection();

			if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				Debug.log(Debug.NORMAL_STATUS, " SQL, isValidAccountIdAccountName :\n" + SOAQueryConstants.EXISTS_ACCOUNTID_ACCOUNTNAME);

			pstmt = conn.prepareStatement(SOAQueryConstants.EXISTS_ACCOUNTID_ACCOUNTNAME);

			pstmt.setString( 1, accountId.toString() );
			pstmt.setString( 2, accountName.toString() );

			results = pstmt.executeQuery();

			if( results.next() )
				return true;    //return true

		}catch(Exception exception){
			logErrors(exception, SOAQueryConstants.EXISTS_ACCOUNTID_ACCOUNTNAME);
		}finally{
			close(results, pstmt);
		}
		return false;       //otherwise return false
	}

	/**
	 * This function validate the SubdomainLevelUser to perform modify request on TN.
	 * 
	 * @param reqInitSPID, InitSPID from request.
	 * @param reqAltSpid, AltSPID from request.
	 * 
	 * @return true if get valid SUBDOMAIN, otherwise false.
	 */
	public boolean validateUserforModifyRequest( Value reqInitSPID )
	{
		boolean result = true;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		boolean isSubDomainLevelUser = false;

		String initSpid = reqInitSPID.toString();

		String subdomain = null;

		ArrayList modifySvRs = new ArrayList();    	

		try
		{   
			// Get the SubDomainId from Context.	
			subdomain = CustomerContext.getInstance().getSubDomainId();

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," SubDomain from Context: "+subdomain);

			if( subdomain != null && !(subdomain.equals("")) )
			{
				isSubDomainLevelUser = true;
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," validateUserforModifyRequest.isSubDomainLevelUserFlag: "+isSubDomainLevelUser);

			// For Domain Level User, no check required.
			if( !isSubDomainLevelUser )
			{
				result = true;

				return result;
			}

			if (existsValue("modifySvRs"))
			{
				modifySvRs = (ArrayList)getContextValue("modifySvRs");
			}

			if(modifySvRs.size() == 0)
			{
				result = true;

				return result;
			} 

			String onsp = ((ModifySvData)modifySvRs.get(0)).onsp;

			String nnsp = ((ModifySvData)modifySvRs.get(0)).nnsp;


			if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
				Debug.log(Debug.MSG_DATA ," validateUserforModifyRequest: ONSP["+onsp+"] NNSP["+nnsp+"]");

			// For PortOut Modify Request
			if ( onsp.equals(initSpid) )
			{
				if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
					Debug.log(Debug.MSG_DATA ," validateUserforModifyRequest.Modify Request for PortOut.");

				if( isSubDomainLevelUser )
				{
					result = validateSubDomainForPortOutRequest( subdomain );

					if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
						Debug.log(Debug.MSG_DATA , " validateUserforModifyRequest return result wiht: "+result);

					updateFailedTNList(result);

					return result;
				}
				else
					result = true;
			}
			// For PortIn Modify Request
			else if ( nnsp.equals(initSpid))
			{
				if ( Debug.isLevelEnabled( Debug.MSG_DATA ))
					Debug.log(Debug.MSG_DATA ," validateUserforModifyRequest.Modify Request for PortIn.");

				//Check Subdomain Value of exising Object Record with Subdomain value.
				if(isSubDomainLevelUser)
				{
					result = validateSubDomainForPortInRequest(subdomain);

					if(!result) return result;
				} 	
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from validateUserforModifyRequest() with: "+result);
		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at validateUserforModifyRequest() "+ exception.toString());
		}
		finally
		{
			close(results, pstmt); 
		}			  

		return result;
	}

	/**
	 * For Modify Request, check the valid SUBDOMAIN value basis on populate ALTSPID.
	 * 
	 * @param altSpid - populate ALTSPID on request.
	 * 
	 * @return true if get valid SUBDOMAIN, otherwise false.
	 */
	public boolean checkALTSPIDforModifyRequest( Value reqAltSpid ,Value reqInitSpid )
	{    
		boolean result = true;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		boolean isSubDomainLevelUser = false;

		String initSpid = reqInitSpid.toString();

		String altSpid = reqAltSpid.toString();

		String subdomain = null;

		String customerid = null;

		try
		{   
			// Get the CustomerId and SubDomainId from Context.	
			customerid = CustomerContext.getInstance().getCustomerID();
			subdomain = CustomerContext.getInstance().getSubDomainId();

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," SubDomain from Context: "+subdomain);

			if( subdomain != null && !(subdomain.equals("")) )
			{
				isSubDomainLevelUser = true;
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," checkALTSPIDforModifyRequest.isSubDomainLevelUserFlag: "+isSubDomainLevelUser);


			//If Domain Level User , return, don't require any checking.
			if( !isSubDomainLevelUser )
			{
				result = true;

				setsubDomainValueforDomain_in_modifyrequest( customerid, altSpid ,initSpid );

				return result;
			}

			//If SubDomain Level User Populate ALTSPID with '-'
			if( isSubDomainLevelUser && altSpid.trim().equals("-") )
			{
				result = false;

				updateFailedTNList(result);

				return result;
			}
			//if AltSPID populate
			else if( !altSpid.trim().equals("") )
			{
				// Check the Populate ALSPID from SOA_SUBDOMAIN_ALTSPID table.
				String mainQuery = SOAQueryConstants.SUBDOMAINID_ALTSPID_QUERY;

				conn = getDBConnection();
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," checkALTSPIDforModifyRequest.SQL for ALTSPID Validation: "+mainQuery);

				pstmt = conn.prepareStatement(mainQuery);

				pstmt.setString( 1, customerid );
				pstmt.setString( 2, altSpid );
				pstmt.setString( 3, subdomain );

				results	= pstmt.executeQuery();
				result = results.next();		    	  

				updateFailedTNList(result);
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from checkALTSPIDforModifyRequest() with: "+result);				  
		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at checkALTSPIDforModifyRequest() "+ exception.toString());
		}
		finally
		{
			close(results, pstmt); 
		}

		return result;

	}

	/**
	 * This function check for modify portout request that Subdomain Level user is valid for this request.
	 * 
	 * @param subdomain, from Session
	 * 
	 * @return true if validation succesful, otherwise false.
	 */
	public boolean validateSubDomainForPortOutRequest(String subdomain)
	{
		boolean result = true;

		ArrayList modifySvRs = new ArrayList();

		try
		{
			if (existsValue("modifySvRs"))
			{
				modifySvRs = (ArrayList)getContextValue("modifySvRs");
			}

			if(modifySvRs.size() == 0)
			{
				result = true;

				return result;
			}

			for( int i=0; i< modifySvRs.size();i++)
			{
				String objSubdomain = ((ModifySvData)modifySvRs.get(i)).subdomain;

				if( objSubdomain == null || !subdomain.equals(objSubdomain) )
				{
					result = false;

					return result;
				}
			}

		}
		catch(Exception exception )
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at validateSubDomainForPortoutRequest() "+ exception.toString());	
		}

		return result;
	}
	/**
	 * Function, get the SBUDOMAIN value and check this Subdomain Value
	 * with subdomain value of existing record of object table.
	 * 
	 * @param subdomain
	 * 
	 * @return true if validation succesful, otherwise false.
	 */

	public boolean validateSubDomainForPortInRequest(String subdomain)
	{
		boolean result = true;

		Connection conn = null;

		Statement stmt = null;

		ResultSet results = null;

		ArrayList modifySvRs = new ArrayList();

		StringBuffer mainQuery = new StringBuffer();

		StringBuffer tnQuery = new StringBuffer();

		StringBuffer errorMessage = new StringBuffer();

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"  In SvalidateSubDomainForPortINRequest() ");

		try
		{
			if (existsValue("modifySvRs"))
			{
				modifySvRs = (ArrayList)getContextValue("modifySvRs");
			}

			int tnCount = modifySvRs.size();

			if(modifySvRs.size() == 0)
			{
				result = true;
				return result;
			}

			conn = getDBConnection();

			int i=0;
			tnQuery.delete(0,tnQuery.length());

			while (i < tnCount)
			{			
				int j = 1;

				StringBuffer altspidValue = new StringBuffer();

				while (j <= 1000 && i <= tnCount-1 )
				{
					altspidValue.append("'");
					altspidValue.append( ((ModifySvData)modifySvRs.get(i)).altspid);

					if ( j < 1000 && i != tnCount-1)				
					{
						altspidValue.append("',");
					}
					else
					{
						altspidValue.append("'");
					}
					i++;
					j++;

				}

				tnQuery.append(" select CUSTOMERID, SUBDOMAINID, ALTERNATIVESPID ");	
				tnQuery.append(" from SOA_SUBDOMAIN_ALTSPID ");
				tnQuery.append(" where");
				tnQuery.append(" SUBDOMAINID ='"+subdomain+"'");
				tnQuery.append(" and ALTERNATIVESPID in ("+altspidValue+" ) ");

				mainQuery.append(tnQuery);

				if (i <= tnCount-1)
				{
					mainQuery.append(" UNION ");
				}

			}//end of outer while			

			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
				Debug.log(Debug.NORMAL_STATUS," SQL of mainQuery in validateSubDomainForPortInRequest() :"+mainQuery);	

			stmt = conn.createStatement();		 

			results = stmt.executeQuery(mainQuery.toString());

			ArrayList resultAltSpid = new ArrayList();

			while(results.next())
				resultAltSpid.add(results.getString(3));

			for(  i=0; i < modifySvRs.size();i++)
			{
				String tn =  ((ModifySvData)modifySvRs.get(i)).portingTn;
				String objAltSpid = ((ModifySvData)modifySvRs.get(i)).altspid;

				int tnIndex = 0;
				boolean objAltSpidFlag = false;

				if(objAltSpid != null && !(objAltSpid.equals("")) )
				{
					objAltSpidFlag = true;
					tnIndex= resultAltSpid.indexOf(objAltSpid);

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS," tnINdex "+tnIndex);
				}

				if ( tnIndex == -1 || !objAltSpidFlag )
				{
					addFailedTN(tn);

					errorMessage.append("["+ tn +"]");

					result = false;
				}				 

			}
			setDynamicErrorMessage(errorMessage.toString());

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from validateSubDomainForPortInRequest() with: "+result);	
		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at validateSubDomainForPortInRequest() "+ exception.toString());
		}
		finally
		{
			close(results, stmt);
		}

		return result;		
	}

	/**
	 * For Release Request, check the validity of SUBDOMAIN  basis on existing record from Object table.
	 * 
	 * @param reqSpid, InitSPID from request.
	 * 
	 * @return true if validation successful, otherwise false.
	 */  
	public boolean checkALTSPIDforReleaseRequest( Value reqSpid )
	{

		boolean result = true;

		boolean isSubDomainLevelUser = false;

		String spid = reqSpid.toString();

		String subdomain = null;

		String tn = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
		}

		try
		{
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"In checkALTSPIDforReleaseRequest() ");

			// Get the SubDomainId from Context.	
			subdomain = CustomerContext.getInstance().getSubDomainId();


			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," checkALTSPIDforReleaseRequest() SUBDOMAIN:["+subdomain+"] , NNSP;["+spid+"]");				

			if( subdomain != null && !(subdomain.equals("")) )
			{
				isSubDomainLevelUser = true;
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," checkALTSPIDforReleaseRequest.isSubDomainLevelUser: "+isSubDomainLevelUser);

			// Check session.SUBDOMAIN value from SUBDOMAIN value on existing recored of object table.
			result =  populateSubdomain_In_RelaseRequest(subdomain, spid, isSubDomainLevelUser);

			// For Domain level user NO BR required for Release Request
			if(! isSubDomainLevelUser) result = true;

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from checkALTSPIDforReleaseRequest() with: "+result);

			if(!result){
				for (int j = 0;j < tnList.size() ;j++ ){
					tn = (String)tnList.get(j);
					addFailedTN(tn);

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS,"Result for checkALTSPIDforReleaseRequest: "+tn);

					errorMessage.append("[" + tn + "] ");
				}
				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at checkALTSPIDforReleaseRequest() "+ exception.toString());	
		}

		return result;
	}

	/**
	 * Function Check the session.subdomain value from the value of subdomain from the exising record of object table.
	 * @param subdomain, SUBDOMAI Value from Session Context.
	 * @param nnsp, InitSPID from request.
	 * @return true if validation succesful, otherwise false.
	 */      
	public boolean populateSubdomain_In_RelaseRequest(String subdomain ,String nnsp , boolean isSubDomainLevelUser )
	{

		boolean result = true;

		Connection conn = null;

		ResultSet results = null;

		Statement stmt = null;

		StringBuffer mainQuery = new StringBuffer();

		ArrayList validTNList = new ArrayList();

		ArrayList tn_list1 = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		StringBuffer tnQuery = new StringBuffer();

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS," In populateSubdomain_In_RelaseRequest() ");

		try
		{
			conn = getDBConnection();

			// Get the all TNList in Local Function Variable.
			tn_list1 = (ArrayList)getContextValue("tnList");

			ArrayList tn_list = new ArrayList(tn_list1);

			int tnCount = tn_list.size(); 

			int i = 0;

			tnQuery.delete(0,tnQuery.length());
			mainQuery.delete(0,mainQuery.length());


			while (i < tnCount)
			{			
				int j = 1;

				StringBuffer tnValue = new StringBuffer();

				while (j <= 1000 && i <= tnCount-1 )
				{
					tnValue.append("'");
					tnValue.append(tn_list.get(i).toString());

					if ( j < 1000 && i != tnCount-1)				
						tnValue.append("',");
					else
						tnValue.append("'");

					i++;
					j++;

				}// end of inner while

				tnQuery.append("select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, ALTERNATIVESPID ");	
				tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
				tnQuery.append(" where ");
				tnQuery.append("(PORTINGTN, SPID, CREATEDDATE) ");
				tnQuery.append(" in ");
				tnQuery.append( "(select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, SPID, max(CREATEDDATE) ");
				tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
				tnQuery.append(" where ");
				tnQuery.append(" PORTINGTN in ("+tnValue+ " ) ");
				tnQuery.append(" and  SPID = '"+nnsp+"'  and NNSP = SPID ");
				tnQuery.append("and STATUS in ( 'active')   ");
				tnQuery.append(" group by PORTINGTN, SPID)");
				tnQuery.append("and STATUS in ( 'active')   ");


				mainQuery.append(tnQuery);

				if (i <= tnCount-1)
				{
					mainQuery.append(" UNION ");
				}

			}// end of outer while	

			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
				Debug.log(Debug.NORMAL_STATUS," populateSubdomain_In_RelaseRequest.mainQuery.SQL_1 :"+mainQuery);

			stmt = conn.createStatement();		 

			results = stmt.executeQuery(mainQuery.toString());

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Successful execution");

			while(results.next())
			{ 
				String tn ="";
				String altspid = "";

				if(results.getString(1) != null)
					tn = results.getString(1);

				if(results.getString(2) != null)
					altspid = results.getString(2);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," Adding CreateTnAltSpidData("+tn+","+altspid+")");

				validTNList.add( new CreateTnAltSpidData( tn, altspid ) );
			}

			close(results, stmt);

			tn_list = getSuccessTN_having_valid_ALTSPID(tn_list, validTNList);

			tnCount = tn_list.size();

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS,"validTnList after mainQuery.SQL_1: "+ validTNList.size());
				Debug.log(Debug.MSG_STATUS," tnCount after mainQuery.SQL_1: "+ tnCount);
			}

			if(tnCount > 0)
			{
				i=0;

				mainQuery.delete(0,mainQuery.length());
				tnQuery.delete(0,tnQuery.length());

				while (i < tnCount)
				{			
					int j = 1;

					StringBuffer npaValue = new StringBuffer();
					StringBuffer nxxValue = new StringBuffer();
					StringBuffer dashxValue = new StringBuffer();

					while (j <= 1000 && i <= tnCount-1 )
					{
						npaValue.append("'");
						npaValue.append(tn_list.get(i).toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX));

						nxxValue.append("'");
						nxxValue.append(tn_list.get(i).toString().substring(SOAConstants.TN_NXX_START_INDEX,SOAConstants.TN_NXX_END_INDEX));

						dashxValue.append("'");
						dashxValue.append(tn_list.get(i).toString().substring(SOAConstants.TN_DASHX_START_INDEX, SOAConstants.TN_DASHX_END_INDEX));


						if ( j < 1000 && i != tnCount-1)				
						{
							npaValue.append("',");
							nxxValue.append("',");
							dashxValue.append("',");

						}
						else
						{
							npaValue.append("'");
							nxxValue.append("'");
							dashxValue.append("'");
						}
						i++;
						j++;

					}

					tnQuery.append(" select NPA, NXX, DASHX, ALTERNATIVESPID from SOA_NBRPOOL_BLOCK ");	
					tnQuery.append(" where ");
					tnQuery.append(" SPID = '" +nnsp+"'");
					tnQuery.append(" and NPA in ("+npaValue+" ) ");
					tnQuery.append(" and NXX in ("+nxxValue+" ) ");
					tnQuery.append(" and DASHX in ("+dashxValue+" ) ");
					tnQuery.append(" and STATUS ='active' and ALTERNATIVESPID IS NOT NULL ");

					mainQuery.append(tnQuery);

					if (i <= tnCount-1)
					{
						mainQuery.append(" UNION ");
					}

				}	

				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS," populateSubdomain_In_RelaseRequest.mainQuery.SQL_2 :"+mainQuery);

				stmt = conn.createStatement();		 

				results = stmt.executeQuery(mainQuery.toString());

				while(results.next())
				{
					String npa = results.getString(1);
					String nxx = results.getString(2);
					String dashx = results.getString(3);
					String altspid = results.getString(4);

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS," in resutlSet , npa["+npa+"] nxx["+nxx+"] dashx["+dashx+"] altspid["+altspid+"]");

					for( int z=0; z< tn_list.size(); z++)
					{
						String tn = tn_list.get(z).toString();

						if(tn.substring(0,3).equals(npa) && tn.substring(4,7).equals(nxx) && tn.substring(8,9).equals(dashx))
							validTNList.add(new CreateTnAltSpidData(tn,altspid)); 
					}
				}

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

					Debug.log(Debug.MSG_STATUS,"validTnList after mainQuery.SQL_2: "+ validTNList.size());
					Debug.log(Debug.MSG_STATUS," tnCount after mainQuery.SQL_2: "+ tnCount);
				}

				close(results, stmt);

				tn_list = getSuccessTN_having_valid_ALTSPID(tn_list, validTNList);
			}	 

			//Add Remain TN_LIST to FailedTNLIST for only Subdomain Level User
			if(subdomain != null && !subdomain.equals(""))
				for(int z=0;z<tn_list.size();z++)
				{
					addFailedTN(tn_list.get(z).toString());

					errorMessage.append("[" + tn_list.get(z) + "] ");

					result = false;
				}

			tnCount = validTNList.size();

			if(tnCount == 0)
			{
				result = false;
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"validTNList is ZEROOO.");
			}
			else
			{
				i=0;

				mainQuery.delete(0,mainQuery.length());
				tnQuery.delete(0,tnQuery.length());

				while (i < tnCount)
				{			
					int j = 1;

					StringBuffer altspidValue = new StringBuffer();

					while (j <= 1000 && i <= tnCount-1 )
					{
						altspidValue.append("'");
						altspidValue.append( ((CreateTnAltSpidData)validTNList.get(i)).altspid);

						if ( j < 1000 && i != tnCount-1)				
						{
							altspidValue.append("',");
						}
						else
						{
							altspidValue.append("'");
						}
						i++;
						j++;

					}

					tnQuery.append(" select CUSTOMERID, SUBDOMAINID, ALTERNATIVESPID ");	
					tnQuery.append(" from SOA_SUBDOMAIN_ALTSPID ");
					tnQuery.append(" where ");
					tnQuery.append(" ALTERNATIVESPID in ("+altspidValue+" ) ");

					//for SubDomain Level User
					if(!subdomain.equals(""))
						tnQuery.append(" and SUBDOMAINID ='"+subdomain+"'");

					try
					{
						String cid = null;
						cid = CustomerContext.getInstance().getCustomerID( );
						if(cid != null && !cid.equals(""))
							tnQuery.append(" AND CUSTOMERID = '" +cid+"'");
					}
					catch ( Exception e )
					{
						Debug.error( e.getMessage() );

						throw new DatabaseException( e.getMessage() );
					}

					mainQuery.append(tnQuery);

					if (i <= tnCount-1)
					{
						mainQuery.append(" UNION ");
					}

				}		

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," populateSubdomain_In_RelaseRequest.mainQuery.SQL_3 :"+mainQuery);

				stmt = conn.createStatement();		 

				results = stmt.executeQuery(mainQuery.toString());

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"  Successful Execution.");

				ArrayList releaseReqTNSubdomain = new ArrayList();

				ArrayList resultAltSpid = new ArrayList();
				ArrayList resultSubdomain = new ArrayList();

				while(results.next())
				{
					resultSubdomain.add(results.getString(2));
					resultAltSpid.add(results.getString(3));
				}

				close(results, stmt);

				for(int z=0; z<validTNList.size();z++)
				{
					String reqAltSpid =  ((CreateTnAltSpidData)validTNList.get(z)).altspid;
					String tn = ((CreateTnAltSpidData)validTNList.get(z)).portingTn;

					int tnIndex = -1;

					boolean reqAltSpidFlag = false;

					if(reqAltSpid != null && !(reqAltSpid.equals("")) )
					{
						reqAltSpidFlag = true;

						tnIndex= resultAltSpid.indexOf(reqAltSpid);

						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS," tnINdex "+tnIndex);
					}

					if ( isSubDomainLevelUser && (tnIndex == -1 || !reqAltSpidFlag)  )
					{
						addFailedTN(tn);

						errorMessage.append("[" + tn + "] ");

						result = false;
					}
					else if(subdomain.equals(""))
					{
						String subdomainVal =  "";
						String subdomainFromCustomerLookup = getSubDomainFromCustomerLookup(conn,nnsp);

						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS," subdomain From CustomerLookupa( "+subdomainFromCustomerLookup+" )");

						if(tnIndex != -1)
							subdomainVal = (String)resultSubdomain.get(tnIndex);							

						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS," add CreateTnSubdomainData( "+tn+" ,"+subdomainVal+" )");

						if(subdomainFromCustomerLookup != null && subdomainFromCustomerLookup.equals(subdomainVal)){
							releaseReqTNSubdomain.add(new CreateTnSubdomainData(tn, subdomainVal));
						}
						else
						{
							releaseReqTNSubdomain.add(new CreateTnSubdomainData(tn, ""));
						}
					}
				}

				// Setting TN-Subdomain for Domain Level user
				CustomerContext.getInstance().set("releaseReqTNSubdomain",releaseReqTNSubdomain);

				setDynamicErrorMessage(errorMessage.toString());

			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from populateSubdomain_In_RelaseRequest() with: "+result);	

		}
		catch(Exception exception)
		{
			close(results, stmt);

			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at populateSubdomain_In_RelaseRequest() "+ exception.toString());
		}

		return result;   	

	}

	/**
	 * select  subdomain value for CUSTOMER_LOOKUP for given TPID
	 * 
	 * @param conn connection from transaction
	 * @param spid, spid from Request
	 */
	private String getSubDomainFromCustomerLookup(Connection conn, String spid) {

		Statement stmt = null;
		ResultSet rs = null;
		String subdomain="";
		StringBuffer tnQuery = new StringBuffer();
		tnQuery.append(" select SUBDOMAINID ");
		tnQuery.append(" from CUSTOMER_LOOKUP ");
		tnQuery.append(" where ");
		tnQuery.append(" TPID = '" +spid +"'");
		try {
			String cid = null;
			cid = CustomerContext.getInstance().getCustomerID();
			if (cid != null && !cid.equals(""))
				tnQuery.append(" AND CUSTOMERID = '" + cid + "'");

			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
				Debug.log(Debug.NORMAL_STATUS," getSubDomainFromCustomerLookup query( "+tnQuery.toString()+" )");

		} catch (Exception e) {
			Debug.error(e.getMessage());
		}
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(tnQuery.toString());
			if(rs.next()){
				if(rs.getString(1) != null)
					subdomain = rs.getString(1);
			}
			close(rs, stmt);

		} catch (Exception ex) {
			if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
				Debug.log(Debug.SYSTEM_CONFIG, ex.toString());
		}

		return subdomain;
	}

	/**
	 * Set the subdomain value for Domain level in modify request, user basis on CustomerId and populate AltSPID
	 * 
	 * @param customerid, CustomerId from Session.Context
	 * @param altSpid, ALTSPID from Request
	 */	  

	public void setsubDomainValueforDomain_in_modifyrequest( String customerid, String altSpid, String initSpid)
	{

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String mainQuery = null;

		String subdomain = "";

		try
		{
			if( altSpid != null &&  !altSpid.equals("") )
			{
				if( altSpid.trim().equals("-"))
				{
					subdomain = "-";
				}
				else
				{
					conn = getDBConnection();
					mainQuery = SOAQueryConstants.DOMAIN_SUBDOMAINID_ALTSPID_QUERY;


					if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
						Debug.log(Debug.NORMAL_STATUS," setsubDomainValueforDomain_in_modifyrequest.mainQuery.SQL: "+mainQuery);

					pstmt = conn.prepareStatement(mainQuery);

					pstmt.setString( 1, initSpid );
					pstmt.setString( 2, customerid );
					pstmt.setString( 3, customerid );
					pstmt.setString( 4, altSpid );

					results	= pstmt.executeQuery();
					if(results.next())
						subdomain = results.getString(1);
					else
						subdomain = "-";
				}
			}

			CustomerContext.getInstance().set("subdomain",subdomain);
		}
		catch(Exception exception )
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at setsubDomainValueforDomain_in_modifyrequest() "+ exception.toString());
		}
		finally
		{
			close(results, pstmt); 
		}


	}
	/**
	 * Set the subdomain value for Domain level user basis on CustomerId and populate AltSPID
	 * 
	 * @param customerid, CustomerId from Session.Context
	 * @param altSpid, ALTSPID from Request
	 */

	public void setsubDomainValueforDomain( String customerid, String altSpid , String initSpid)
	{

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		String mainQuery = null;

		String subdomain = "";

		try
		{
			if( altSpid != null &&  !altSpid.equals("") )
			{
				if( altSpid.trim().equals("-"))
				{
					subdomain = "-";
				}
				else
				{
					conn = getDBConnection();
					mainQuery = SOAQueryConstants.DOMAIN_SUBDOMAINID_ALTSPID_QUERY;


					if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
						Debug.log(Debug.NORMAL_STATUS," setsubDomainValueforDomain.mainQuery.SQL: "+mainQuery);

					pstmt = conn.prepareStatement(mainQuery);

					pstmt.setString( 1, initSpid );
					pstmt.setString( 2, customerid );
					pstmt.setString( 3, customerid );
					pstmt.setString( 4, altSpid );

					results	= pstmt.executeQuery();
					if(results.next())
						subdomain = results.getString(1);
				}
			}

			CustomerContext.getInstance().set("subdomain",subdomain);
		}
		catch(Exception exception )
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at setsubDomainValueforDomain() "+ exception.toString());
		}
		finally
		{
			close(results, pstmt); 
		}

	}


	/**
	 * This function that Subdomain Level User should populate AltSPID value.
	 * 
	 * @param reqAltSpid, AltSPID from request.
	 * 
	 * @return true if validation succesful, otherwise false.
	 */

	public boolean isALTSPIDpopulateForCreateRequest( Value reqAltSpid )
	{
		boolean result = true;

		boolean isSubDomainLevelUser = false;

		String subdomain = null;    

		String altSpid = reqAltSpid.toString();

		try
		{
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"In isALTSPIDpopulateForCreateRequest() ");

			// Get the SubdomainId from Context.	
			subdomain = CustomerContext.getInstance().getSubDomainId();    


			if( subdomain != null && !(subdomain.equals("")) )
			{
				isSubDomainLevelUser = true;
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," isALTSPIDpopulateForCreateRequest.isSubDomainLevelUserFlag: "+isSubDomainLevelUser);

			// If SubDomain Level User doesn't populate ALTSPID
			if( isSubDomainLevelUser && altSpid.trim().equals("") )
			{
				result = false;

				updateFailedTNList(result);	

				return result;
			}				  
		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at isALTSPIDpopulateForCreateRequest() "+ exception.toString());	
		}

		return result;
	}

	/**
	 * For Create Request, check the validity of SUBDOMAIN and populate ALTSPID.

	 * @param altSpid, Populate ALTSPID from Request
	 * @param reqONSP, ONSP from Request
	 * @param reqNNSP, NNSP from Request
	 * 
	 * @return true if validation succesful, otherwise false.
	 */  
	public boolean checkALTSPIDforCreateRequest( Value reqAltSpid , Value reqONSP, Value reqNNSP)
	{
		boolean result = true;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		boolean isSubDomainLevelUser = false;

		String altSpid = reqAltSpid.toString();

		String onsp = reqONSP.toString();

		String nnsp = reqNNSP.toString();	    

		String mainQuery = null;

		String customerid = null;

		String subdomain = "";

		try
		{
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"In checkALTSPIDforCreateRequest() ");

			// Get the CustomerId and SubdomainId from Context.	
			customerid = CustomerContext.getInstance().getCustomerID();
			subdomain = CustomerContext.getInstance().getSubDomainId();

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," checkALTSPIDforCreateRequest().subdomain:["+subdomain+"] , ONSP:["+onsp+"], NNSP;["+nnsp+"]");				

			if( subdomain != null && !(subdomain.equals("")) )
			{
				isSubDomainLevelUser = true;
			}
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," checkALTSPIDforCreateRequest.isSubDomainLevelUserFlag: "+isSubDomainLevelUser);

			//If Domain Level User, return , don't requried any checking
			if( !isSubDomainLevelUser  )
			{
				setsubDomainValueforDomain( customerid, altSpid , nnsp );

				result = true;

				return result;
			}

			//For SubDomain Level user in IntraPort Case.
			if ( isSubDomainLevelUser && onsp.equals(nnsp))
			{
				result =  checkALTSPIDforIntraPort(subdomain, nnsp);
			}

			conn = getDBConnection();

			mainQuery = SOAQueryConstants.SUBDOMAINID_ALTSPID_QUERY;


			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
				Debug.log(Debug.NORMAL_STATUS," checkALTSPIDforCreateRequest.mainQuery.SQL: "+mainQuery);

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," checkALTSPIDforCreateRequest.runtimeValues : customerid ["+customerid+"], AltSpid ["+altSpid+"], Subdomain ["+subdomain+"]");	

			pstmt = conn.prepareStatement(mainQuery);

			pstmt.setString( 1, customerid );
			pstmt.setString( 2, altSpid );
			pstmt.setString( 3, subdomain );

			results	= pstmt.executeQuery();

			boolean resultTemp = results.next();

			updateFailedTNList(resultTemp);

			if(result)
				result = resultTemp;

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from checkALTSPIDforCreateRequest() with: "+result);	

		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at checkALTSPIDforCreateRequest() "+ exception.toString());	

		}
		finally
		{
			close(results, pstmt); 
		}

		return result;
	}


	/**
	 * Check the SessionContext.SubDomainId from SUBDOMAINID value of existing recored of Object table.
	 * 
	 * @param subdomain, SUBDOMAINID from Session.Context.
	 * @param nnsp, NNSP from request.
	 * 
	 * @return true if validation succesful, otherwise false.
	 */

	public boolean checkALTSPIDforIntraPort(String subdomain, String nnsp)
	{
		boolean result = true;

		Connection conn = null;

		ResultSet results = null;

		Statement stmt = null;

		StringBuffer mainQuery = new StringBuffer();

		StringBuffer tnQuery = new StringBuffer();

		StringBuffer errorMessage = new StringBuffer();

		ArrayList validTNList = new ArrayList();

		ArrayList tn_list1 = new ArrayList();

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS," In checkALTSPIDforIntraPort() ");
		try
		{
			conn = getDBConnection();

			//Get all TNList from Context to local method variable.
			tn_list1 = (ArrayList)getContextValue("tnList");

			ArrayList tn_list = new ArrayList(tn_list1);

			int tnCount = tn_list.size(); 

			int i = 0;

			tnQuery.delete(0,tnQuery.length());
			mainQuery.delete(0,mainQuery.length());


			while (i < tnCount)
			{			
				int j = 1;

				StringBuffer tnValue = new StringBuffer();

				while (j <= 1000 && i <= tnCount-1 )
				{
					tnValue.append("'");
					tnValue.append(tn_list.get(i).toString());

					if ( j < 1000 && i != tnCount-1)				
						tnValue.append("',");
					else
						tnValue.append("'");

					i++;
					j++;

				}// end of inner while

				tnQuery.append("select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, ALTERNATIVESPID ");	
				tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
				tnQuery.append(" where ");
				tnQuery.append("(PORTINGTN, SPID, CREATEDDATE) ");
				tnQuery.append(" in ");
				tnQuery.append( "(select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, SPID, max(CREATEDDATE) ");
				tnQuery.append(" from SOA_SUBSCRIPTION_VERSION ");
				tnQuery.append(" where ");
				tnQuery.append(" PORTINGTN in ("+tnValue+ " ) ");
				tnQuery.append(" and  SPID = '"+nnsp+"' and NNSP = SPID ");
				tnQuery.append("and STATUS in ( 'active')  ");
				tnQuery.append(" group by PORTINGTN, SPID)");
				tnQuery.append("and STATUS in ( 'active')  ");


				mainQuery.append(tnQuery);

				if (i <= tnCount-1)
				{
					mainQuery.append(" UNION ");
				}

			}// end of outer while	

			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
				Debug.log(Debug.NORMAL_STATUS," checkALTSPIDforIntraPort.mainQuery.SQL_1: "+mainQuery);

			stmt = conn.createStatement();		 

			results = stmt.executeQuery(mainQuery.toString());

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Successful execution");

			while(results.next())
			{ 
				String tn ="";
				String altspid = "";

				if(results.getString(1) != null)
					tn = results.getString(1);

				if(results.getString(2) != null)
					altspid = results.getString(2);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS," Adding CreateTnAltSpidData("+tn+","+altspid+")");

				validTNList.add( new CreateTnAltSpidData( tn, altspid ) );
			}


			close(results, stmt); 

			tn_list = getSuccessTN_having_valid_ALTSPID(tn_list, validTNList);
			tnCount = tn_list.size();

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

				Debug.log(Debug.MSG_STATUS," validTnList after mainQuery.SQL_1: "+ validTNList.size());
				Debug.log(Debug.MSG_STATUS," tnCount mainQuery.SQL_1: "+ tnCount);
			}


			if(tnCount > 0)
			{

				i=0;

				mainQuery.delete(0,mainQuery.length());
				tnQuery.delete(0,tnQuery.length());

				while (i < tnCount)
				{			
					int j = 1;

					StringBuffer npaValue = new StringBuffer();
					StringBuffer nxxValue = new StringBuffer();
					StringBuffer dashxValue = new StringBuffer();

					while (j <= 1000 && i <= tnCount-1 )
					{
						npaValue.append("'");
						npaValue.append(tn_list.get(i).toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX));

						nxxValue.append("'");
						nxxValue.append(tn_list.get(i).toString().substring(SOAConstants.TN_NXX_START_INDEX,SOAConstants.TN_NXX_END_INDEX));

						dashxValue.append("'");
						dashxValue.append(tn_list.get(i).toString().substring(SOAConstants.TN_DASHX_START_INDEX, SOAConstants.TN_DASHX_END_INDEX));


						if ( j < 1000 && i != tnCount-1)				
						{
							npaValue.append("',");
							nxxValue.append("',");
							dashxValue.append("',");

						}
						else
						{
							npaValue.append("'");
							nxxValue.append("'");
							dashxValue.append("'");
						}
						i++;
						j++;

					}

					tnQuery.append(" select NPA, NXX, DASHX, ALTERNATIVESPID from SOA_NBRPOOL_BLOCK ");	
					tnQuery.append(" where ");
					tnQuery.append(" SPID = '" +nnsp+"'");
					tnQuery.append(" and NPA in ("+npaValue+" ) ");
					tnQuery.append(" and NXX in ("+nxxValue+" ) ");
					tnQuery.append(" and DASHX in ("+dashxValue+" ) ");
					tnQuery.append(" and STATUS ='active' and ALTERNATIVESPID IS NOT NULL ");

					mainQuery.append(tnQuery);

					if (i <= tnCount-1)
					{
						mainQuery.append(" UNION ");
					}

				}	

				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS," checkALTSPIDforIntraPort.mainQuery.SQL_2: "+mainQuery);

				stmt = conn.createStatement();		 

				results = stmt.executeQuery(mainQuery.toString());

				while(results.next())
				{
					String npa = results.getString(1);
					String nxx = results.getString(2);
					String dashx = results.getString(3);
					String altspid = results.getString(4);

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.MSG_STATUS," in resutlSet , npa["+npa+"] nxx["+nxx+"] dashx["+dashx+"] altspid["+altspid+"]");

					for( int z=0; z< tn_list.size(); z++)
					{
						String tn = tn_list.get(z).toString();

						if(tn.substring(0,3).equals(npa) && tn.substring(4,7).equals(nxx) && tn.substring(8,9).equals(dashx))
							validTNList.add(new CreateTnAltSpidData(tn,altspid)); 
					}
				}

				close(results, stmt);

				tn_list = getSuccessTN_having_valid_ALTSPID(tn_list, validTNList);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

					Debug.log(Debug.MSG_STATUS," validTnList after mainQuery.SQL_2: "+ validTNList.size());
					Debug.log(Debug.MSG_STATUS," tnCount mainQuery.SQL_2: "+ tn_list.size());
				}

			}	 

			//Add Remain TN_LIST to FailedTNLIST
			for(int z=0;z<tn_list.size();z++)
			{
				result = false;

				addFailedTN(tn_list.get(z).toString());

				errorMessage.append( "["+ tn_list.get(z) +"]");

			}

			tnCount = validTNList.size();

			if(tnCount == 0)
			{
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"validTNList is ZEROOO.");
				result = false;
			}
			else
			{
				i=0;

				mainQuery.delete(0,mainQuery.length());
				tnQuery.delete(0,tnQuery.length());

				while (i < tnCount)
				{			
					int j = 1;

					StringBuffer altspidValue = new StringBuffer();

					while (j <= 1000 && i <= tnCount-1 )
					{
						altspidValue.append("'");
						altspidValue.append( ((CreateTnAltSpidData)validTNList.get(i)).altspid);

						if ( j < 1000 && i != tnCount-1)				
						{
							altspidValue.append("',");
						}
						else
						{
							altspidValue.append("'");
						}
						i++;
						j++;

					}

					tnQuery.append(" select CUSTOMERID, SUBDOMAINID, ALTERNATIVESPID ");	
					tnQuery.append(" from SOA_SUBDOMAIN_ALTSPID ");
					tnQuery.append(" where ");
					tnQuery.append(" SUBDOMAINID ='"+subdomain+"'");
					tnQuery.append(" and ALTERNATIVESPID in ("+altspidValue+" ) ");
					try
					{
						String cid = null;
						cid = CustomerContext.getInstance().getCustomerID( );
						if(cid != null && !cid.equals(""))
							tnQuery.append(" AND CUSTOMERID = '" +cid+"'");
					}
					catch ( Exception e )
					{
						Debug.error( e.getMessage() );

						throw new DatabaseException( e.getMessage() );
					}
					mainQuery.append(tnQuery);

					if (i <= tnCount-1)
					{
						mainQuery.append(" UNION ");
					}

				}		

				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS,"  checkALTSPIDforIntraPort.mainQuery.SQL_3: "+mainQuery);

				stmt = conn.createStatement();		 

				results = stmt.executeQuery(mainQuery.toString());

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"  Successful Execution of main Query");

				ArrayList resultAltSpid = new ArrayList();

				while(results.next())
					resultAltSpid.add(results.getString(3));


				close(results, stmt);

				for(int z=0; z<validTNList.size();z++)
				{
					String reqAltSpid =  ((CreateTnAltSpidData)validTNList.get(z)).altspid;
					String tn = ((CreateTnAltSpidData)validTNList.get(z)).portingTn;

					int tnIndex = 0;
					boolean reqAltSpidFlag = false;
					if(reqAltSpid != null && !(reqAltSpid.equals("")) )
					{
						reqAltSpidFlag = true;

						tnIndex= resultAltSpid.indexOf(reqAltSpid);

						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS," tnINdex "+tnIndex);
					}

					if ( tnIndex == -1 || !reqAltSpidFlag )
					{
						addFailedTN(tn);

						errorMessage.append(" ["+ tn + "] ");

						result = false;
					}			 
				}
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from checkALTSPIDforIntraPort() with: "+result);

			setDynamicErrorMessage(errorMessage.toString());	
		}
		catch(Exception exception)
		{
			close(results, stmt);

			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at checkALTSPIDforIntraPort() "+ exception.toString());				   
		}

		return result;   	
	}

	/**
	 * Remove the TN from tn_list that are in validTn_list.
	 * 
	 * @param tn_list
	 * @param validTn_list
	 * 
	 * @return tn_list after removing TNs.
	 */

	public ArrayList getSuccessTN_having_valid_ALTSPID(ArrayList tn_list, ArrayList validTn_list)
	{
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS," In getSuccessTN_having_valid_ALTSPID()");

		try
		{
			if(validTn_list != null)
			{
				for( int i=0;i< validTn_list.size();i++)
				{
					String validTN =  ((CreateTnAltSpidData)validTn_list.get(i)).portingTn;
					int tnIndex = tn_list.indexOf(validTN);

					if (tnIndex != -1)
					{
						tn_list.remove(tnIndex);
					}
				}
			}
		}	
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at getSuccessTN_having_valid_ALTSPID() "+ exception.toString());
		}
		return tn_list;
	}    

	/**
	 * 
	 * Update the FailedTNlist, add the TN in Context.FailedTNList.
	 * 
	 */

	public void updateFailedTNList( boolean result)
	{
		ArrayList tnList = new ArrayList();
		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");			
		}			  
		if (!result)
		{
			String tn = null;
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);

				addFailedTN(tn);

				errorMessage.append("[" + tn + "] ");
			}

			setDynamicErrorMessage(errorMessage.toString());
		}	
	}

	/**
	 * This function check the valid initSPID for SubDomainLevel User.
	 * 
	 * @param initSPID, InitSPID from request.
	 * 
	 * @return  true if validation succesful, otherwise false.
	 */
	public boolean isCurrentSubDomainSPID( Value initSPID )
	{
		boolean result = true;

		Connection conn = null;

		ResultSet results = null;

		PreparedStatement pstmt = null;

		String mainQuery = null;

		String customerId = null;

		String subdomainId = null;

		String initSpid = initSPID.toString();

		boolean isSubDomainLevelUser = false;

		try
		{
			conn = getDBConnection();

			// Get the CustomerId and SubdomainId from Context.	
			customerId = CustomerContext.getInstance().getCustomerID();
			subdomainId = CustomerContext.getInstance().getSubDomainId();


			if( subdomainId != null && !(subdomainId.equals("")) )
			{
				isSubDomainLevelUser = true;
			}

			if(isSubDomainLevelUser)
			{
				mainQuery = SOAQueryConstants.DOMAIN_SUBDOMAIN_INITSPID_QUERY;

				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS )){

					Debug.log(Debug.NORMAL_STATUS," isCurrentSubDomainSPID.mainQuery.SQL: "+mainQuery);					  
					Debug.log(Debug.NORMAL_STATUS," isCurrentSubDomainSPID.runtimeValues : customerid ["+customerId+"], InitSPID ["+initSpid+"], subdomainId ["+subdomainId+"]");
				}				  
				pstmt = conn.prepareStatement(mainQuery);

				pstmt.setString( 1, customerId );
				pstmt.setString( 2, subdomainId );
				pstmt.setString( 3, initSpid );

				results	= pstmt.executeQuery();
				result = results.next();		

				updateFailedTNList(result);
			}
			else
			{
				result = true;
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," Returning from isCurrentSubDomainSPID() with: "+result);
		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at isCurrentSubDomainSPID() "+ exception.toString());				   
		}
		finally
		{
			close(results, pstmt);
		}
		return result;
	}

	/**
	 * This function check that user is belong to subdomain level or not.
	 * 
	 * @return,  false if User is SubDomain level user else true;
	 */
	public boolean isPTORequestbySubDomainUser()
	{
		boolean result = true;

		if(isSubDomainLevelUser())
			result = false;

		return result;

	}

	/**
	 * This function check that user is subdomain level or domain level.
	 * 
	 * @return, true if User is SubDomain level user else false;
	 */
	public boolean isSubDomainLevelUser()
	{

		boolean isSubDomainLevelUser = false;

		String subdomainId = null;

		try
		{
			// Get the CustomerId and SubdomainId from Context.	
			subdomainId = CustomerContext.getInstance().getSubDomainId();

			if( subdomainId != null && !(subdomainId.equals("")) )
			{
				isSubDomainLevelUser = true;
			}

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS," isSubDomainLevelUser.isSubDomainLevelUser: "+isSubDomainLevelUser);
		}
		catch(Exception exception)
		{
			if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
				Debug.log(Debug.ALL_ERRORS,"ERROR: occured at isSubDomainUser() "+ exception.toString());				   
		}
		return isSubDomainLevelUser;
	}

	/**
	 * This method is used to set dynamic error RangeId and return false if any SV allready belonging to an Account
	 * incase of Adding SV to an Account otherwise return true
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return boolean.
	 */
	public boolean isRangeAllreadyInAccount(Value accountId, Value spid, Value rangeIdList) {        

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "Account Id is : " + accountId);
			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "RangeId is : " + rangeIdList);
		}

		String rangeIdAllreadyAdded = getAccountAllreadyAdded(getAddRangeQuery(accountId.toString(), spid.toString(), rangeIdList.toString()));
		if(rangeIdAllreadyAdded == null) {

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeAllreadyInAccount:Returning result as true");

			return true;    //none of the RangeId is allready added to an account
		}

		//otherwise set the failed RangeId that are already to an account
		setDynamicErrorMessage(rangeIdAllreadyAdded);
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeAllreadyInAccount:Returning result as false");
		return false;
	}


	/**
	 * This method is used to set dynamic error RangeId and return false if any SV belonging to a different Account 
	 * incase of Removing SV from an Account otherwise return true
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return boolean.
	 */
	public boolean isRangeInDifferentAccount(Value accountId, Value spid, Value rangeIdList) {        
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "Account Id is : " + accountId);
			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "RangeId is : " + rangeIdList);
		}

		String rangeIdAllreadyAdded = getAccountAllreadyAdded(getRemoveRangeQuery(accountId.toString(), spid.toString(), rangeIdList.toString()));
		if(rangeIdAllreadyAdded == null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeInDifferentAccount:Returning result as true");
			return true;    //none of the RangeId is allready added to an account
		}

		//otherwise set the failed SvId that are already to an account
		setDynamicErrorMessage(rangeIdAllreadyAdded);
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeInDifferentAccount:Returning result as false");
		return false;
	}

	/**
	 * This method is used to set dynamic error SvId and return false if any SV allready belonging to an Account
	 * incase of Adding SV to an Account otherwise return true
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return boolean.
	 */
	public boolean isSvIdAllreadyInAccount(Value spid, Value svidList, Value regionId) {        
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "SvIdList is : " + svidList);
			Debug.log(Debug.MSG_STATUS, "RegionId is : " + regionId);
		}

		String svidAllreadyAdded = getAccountAllreadyAdded(getQuery(spid.toString(), svidList.toString(), regionId.toString()));
		if(svidAllreadyAdded == null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSvIdAllreadyInAccount:Returning result as true");
			return true;    //none of the SvId is already added to an account
		}

		//otherwise set the failed SvId that are already to an account
		setDynamicErrorMessage(svidAllreadyAdded);
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSvIdAllreadyInAccount:Returning result as false");
		return false;
	}


	/**
	 * This method is used to set dynamic error SvId and return false if any SV allready belonging to an Account
	 * incase of Removing SV from an account otherwise return true
	 * @param accountId
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return boolean.
	 */
	public boolean isSvIdInDifferentAccount(Value accountId, Value spid, Value svidList, Value regionId) {        
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "AccoutnId is : " + accountId);
			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "SvIdList is : " + svidList);
			Debug.log(Debug.MSG_STATUS, "RegionId is : " + regionId);
		}

		String svidInDifferentAccount = getAccountAllreadyAdded(getRemoveQuery(accountId.toString(), spid.toString(), svidList.toString(), regionId.toString()));
		if(svidInDifferentAccount == null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSvIdInDifferentAccount:Returning result as true");
			return true;    //none of the SvId is already added to an account
		}

		//otherwise set the failed SvId that are already to an account
		setDynamicErrorMessage(svidInDifferentAccount);
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSvIdInDifferentAccount:Returning result as false");
		return false;
	}

	/**
	 * This method is used to set dynamic Tn and return false if any SV allready belonging to an Account
	 * incase of Adding SV to an Account otherwise return true
	 * @param spid
	 * @param newSP
	 * @param oldSP
	 * @param tnList
	 * @return boolean.
	 */
	public boolean isSVAllreadyInAccount(Value spid, Value newSP, Value oldSP, Value tnList, Value accountId) {        

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "New SP is : " + newSP);
			Debug.log(Debug.MSG_STATUS, "Old SP is : " + oldSP);
			Debug.log(Debug.MSG_STATUS, "Tn is : " + tnList);
		}

		String tnAllreadyAdded = getAccountAllreadyAdded(getQuery(spid.toString(), newSP.toString(), oldSP.toString(), tnList.toString(),accountId.toString()));
		if(tnAllreadyAdded == null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSVAllreadyInAccount:Returning result as true");
			return true;    //none of the Tn is allready added to an account
		}

		//otherwise set the failed Tn that are already to an account
		setDynamicErrorMessage(tnAllreadyAdded);
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSVAllreadyInAccount:Returning result as false");
		return false;
	}


	/**
	 * This method is used to set dynamic Tn and return false if any SV allready belonging to an Account
	 * incase of Removing SV from an Account otherwise return true
	 * @param accountId
	 * @param spid
	 * @param newSP
	 * @param oldSP
	 * @param tnList
	 * @return boolean.
	 */
	public boolean isSVInDifferentAccount(Value accountId, Value spid, Value newSP, Value oldSP, Value tnList) {        

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "AccoutnId is : " + accountId);
			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "New SP is : " + newSP);
			Debug.log(Debug.MSG_STATUS, "Old SP is : " + oldSP);
			Debug.log(Debug.MSG_STATUS, "Tn is : " + tnList);
		}

		String tnAllreadyAdded = getAccountAllreadyAdded(getRemoveQuery(accountId.toString(), spid.toString(), newSP.toString(), oldSP.toString(), tnList.toString()));
		if(tnAllreadyAdded == null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSVInDifferentAccount:Returning result as true");
			return true;    //none of the Tn is allready added to an account
		}

		//otherwise set the failed Tn that are already to an account
		setDynamicErrorMessage(tnAllreadyAdded);
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSVInDifferentAccount:Returning result as false");
		return false;
	}


	/**
	 * This method is used to execute the given query and return the result as String
	 * @param query
	 * @return String 
	 */    
	private String getAccountAllreadyAdded(String query) {
		boolean result= true;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet results = null;
		StringBuffer tnBuffer = new StringBuffer();

		try {
			conn = getDBConnection();
			pstmt = conn.prepareStatement(query);

			results = pstmt.executeQuery();
			while(results.next()) {
				Object value = results.getObject(1);
				if(results.isFirst())
					tnBuffer.append(value);
				else
					tnBuffer.append("\t" + value);
			}

		} catch(Exception exception){
			logErrors(exception, query);

		} finally{

			close(results, pstmt);
		}

		return tnBuffer.length()>0 ? tnBuffer.toString():null;
	}

	/**
	 * This method is used to create Query using criteria fields as ACCOUNTID, SPID, and RANGEID
	 * @param accountId
	 * @param spid
	 * @param rangeIdList     
	 * @return query as String 
	 */
	private String getAddRangeQuery(String accountId, String spid, String rangeIdList) {

		StringTokenizer st = new StringTokenizer(rangeIdList, SOAConstants.SVID_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer rangeIdBuffer = new StringBuffer();
		int unionAt = 1000;
		int rangeIdCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID='");
		sbQuery.append(spid);
		sbQuery.append("' AND ACCOUNTID!='");
		sbQuery.append(accountId);
		sbQuery.append("' AND RANGEID IN (");

		while(st.hasMoreTokens()) {
			if(rangeIdCount % unionAt == 0) {
				String rangeIdValues = rangeIdBuffer.toString();
				rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma

				rangeIdBuffer = null;
				rangeIdBuffer = new StringBuffer();

				sbQuery.append(rangeIdValues);                
				sbQuery.append(")");
				sbQuery.append(" GROUP BY RANGEID ");
				sbQuery.append(" HAVING COUNT(*) > 0 ");
				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID = '");
				sbQuery.append(spid);
				sbQuery.append("' AND ACCOUNTID='");
				sbQuery.append(accountId);
				sbQuery.append("' AND RANGEID IN (");
				sbQuery.append(rangeIdValues);                
				sbQuery.append(")");
				sbQuery.append(" GROUP BY RANGEID ");
				sbQuery.append(" HAVING (SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ COUNT(*) FROM SOA_SUBSCRIPTION_VERSION b WHERE b.rangeid=a.rangeid) = COUNT(*)");

				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("' AND ACCOUNTID!='");
				sbQuery.append(accountId);
				sbQuery.append("' AND RANGEID IN (");
			}

			String rangeId = st.nextToken();

			//svidBuffer.append("'");
			rangeIdBuffer.append(rangeId);
			rangeIdBuffer.append(",");
			//svidBuffer.append("',");

			rangeIdCount++;
		}

		String rangeIdValues = rangeIdBuffer.toString();
		rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma
		sbQuery.append(rangeIdValues);
		sbQuery.append(")");         //add the right parenthesis
		sbQuery.append(" GROUP BY RANGEID ");
		sbQuery.append(" HAVING COUNT(*) > 0 ");
		sbQuery.append(" \n UNION \n");
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID = '");
		sbQuery.append(spid);
		sbQuery.append("' AND ACCOUNTID='");
		sbQuery.append(accountId);
		sbQuery.append("' AND RANGEID IN (");
		sbQuery.append(rangeIdValues);                
		sbQuery.append(")");
		sbQuery.append(" GROUP BY RANGEID ");
		sbQuery.append(" HAVING (SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ COUNT(*) FROM SOA_SUBSCRIPTION_VERSION b WHERE b.rangeid=a.rangeid) = COUNT(*)");


		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}


	/**
	 * This method is used to create Query using criteria fields as ACCOUNTID, SPID, and RANGEID
	 * @param accountId
	 * @param spid
	 * @param rangeIdList     
	 * @return query as String 
	 */
	private String getRemoveRangeQuery(String accountId, String spid, String rangeIdList) {

		StringTokenizer st = new StringTokenizer(rangeIdList, SOAConstants.SVID_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer rangeIdBuffer = new StringBuffer();
		int unionAt = 1000;
		int rangeIdCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID='");
		sbQuery.append(spid);
		sbQuery.append("' AND (ACCOUNTID='");
		sbQuery.append(accountId);
		sbQuery.append("' OR ACCOUNTID IS NULL) AND RANGEID IN (");

		while(st.hasMoreTokens()) {
			if(rangeIdCount % unionAt == 0) {
				String rangeIdValues = rangeIdBuffer.toString();
				rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma

				rangeIdBuffer = null;
				rangeIdBuffer = new StringBuffer();

				sbQuery.append(rangeIdValues);                
				sbQuery.append(")");
				sbQuery.append(" GROUP BY RANGEID ");
				sbQuery.append(" HAVING (SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ COUNT(*) FROM SOA_SUBSCRIPTION_VERSION b WHERE b.RANGEID=a.RANGEID) > COUNT(*) ");
				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("' AND (ACCOUNTID!='");
				sbQuery.append(accountId);
				sbQuery.append("' OR ACCOUNTID IS NULL) AND RANGEID IN (");
				sbQuery.append(rangeIdValues);
				sbQuery.append(")");
				sbQuery.append(" GROUP BY RANGEID ");
				sbQuery.append(" HAVING (SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ COUNT(*) FROM SOA_SUBSCRIPTION_VERSION b WHERE b.RANGEID=a.RANGEID) = COUNT(*) ");

				//building next query after unionAt value iteration
				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("' AND (ACCOUNTID='");
				sbQuery.append(accountId);
				sbQuery.append("' OR ACCOUNTID IS NULL) AND RANGEID IN (");
			}

			String rangeId = st.nextToken();

			//svidBuffer.append("'");
			rangeIdBuffer.append(rangeId);
			rangeIdBuffer.append(",");
			//svidBuffer.append("',");

			rangeIdCount++;
		}

		String rangeIdValues = rangeIdBuffer.toString();
		rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma
		sbQuery.append(rangeIdValues);
		sbQuery.append(")");         //add the right parenthesis
		sbQuery.append(" GROUP BY RANGEID ");
		sbQuery.append(" HAVING (SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ COUNT(*) FROM SOA_SUBSCRIPTION_VERSION b WHERE b.RANGEID=a.RANGEID) > COUNT(*) ");
		sbQuery.append(" \n UNION \n");
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION a WHERE SPID='");
		sbQuery.append(spid);
		sbQuery.append("' AND (ACCOUNTID!='");
		sbQuery.append(accountId);
		sbQuery.append("' OR ACCOUNTID IS NULL) AND RANGEID IN (");
		sbQuery.append(rangeIdValues);
		sbQuery.append(")");
		sbQuery.append(" GROUP BY RANGEID ");
		sbQuery.append(" HAVING (SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION  SOA_SV_INDEX_13) */ COUNT(*) FROM SOA_SUBSCRIPTION_VERSION b WHERE b.RANGEID=a.RANGEID) = COUNT(*) ");

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}


	/**
	 * This method is used to create Query using criteria fields as SPID, SVIDLIST, and REGIONID
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return query as String 
	 */
	private String getQuery(String spid, String svidList, String regionId) {

		StringTokenizer st = new StringTokenizer(svidList, SOAConstants.SVID_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer svidBuffer = new StringBuffer();
		int unionAt = 1000;
		int svidCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
		sbQuery.append(spid);
		sbQuery.append("' AND REGIONID=");
		sbQuery.append(regionId);
		sbQuery.append(" AND ACCOUNTID IS NOT NULL AND SVID IN (");

		while(st.hasMoreTokens()) {
			if(svidCount % unionAt == 0) {
				String svidValues = svidBuffer.toString();
				svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma

				svidBuffer = null;
				svidBuffer = new StringBuffer();

				sbQuery.append(svidValues);
				sbQuery.append(")");
				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("' AND REGIONID=");
				sbQuery.append(regionId);
				sbQuery.append(" AND ACCOUNTID IS NOT NULL AND SVID IN (");
			}

			String svid = st.nextToken();

			//svidBuffer.append("'");
			svidBuffer.append(svid);
			svidBuffer.append(",");
			//svidBuffer.append("',");

			svidCount++;
		}

		String svidValues = svidBuffer.toString();
		svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma
		sbQuery.append(svidValues);
		sbQuery.append(")");         //add the right parenthesis

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}


	/**
	 * This method is used to create Query using criteria fields as ACCOUNTID, SPID, SVIDLIST, and REGIONID
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return query as String 
	 */
	private String getRemoveQuery(String accountId, String spid, String svidList, String regionId) {

		StringTokenizer st = new StringTokenizer(svidList, SOAConstants.SVID_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer svidBuffer = new StringBuffer();
		int unionAt = 1000;
		int svidCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
		sbQuery.append(spid);
		sbQuery.append("' AND REGIONID=");
		sbQuery.append(regionId);
		sbQuery.append(" AND (ACCOUNTID!='");
		sbQuery.append(accountId);
		sbQuery.append("' OR ACCOUNTID IS NULL) AND SVID IN (");

		while(st.hasMoreTokens()) {
			if(svidCount % unionAt == 0) {
				String svidValues = svidBuffer.toString();
				svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma

				svidBuffer = null;
				svidBuffer = new StringBuffer();

				sbQuery.append(svidValues);
				sbQuery.append(")");
				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("' AND REGIONID=");
				sbQuery.append(regionId);
				sbQuery.append(" AND (ACCOUNTID!='");
				sbQuery.append(accountId);
				sbQuery.append("' OR ACCOUNTID IS NULL) AND SVID IN (");
			}

			String svid = st.nextToken();

			//svidBuffer.append("'");
			svidBuffer.append(svid);
			svidBuffer.append(",");
			//svidBuffer.append("',");

			svidCount++;
		}

		String svidValues = svidBuffer.toString();
		svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma
		sbQuery.append(svidValues);
		sbQuery.append(")");         //add the right parenthesis

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}

	/**
	 * This method is used to create Query using criteria fields as SPID, NewSP, OldSP, and Tn
	 * @param spid
	 * @param newSP
	 * @param oldSP
	 * @param tnList
	 * @return query as String 
	 */
	private String getQuery(String spid, String newSP, String oldSP, String tnList, String accountId) {

		StringTokenizer st = new StringTokenizer(tnList, SOAConstants.PORTINGTN_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		ArrayList totalTnList = new ArrayList();

		while(st.hasMoreTokens()) {

			String svid = st.nextToken();
			if(svid != null && svid.length()>12)
			{

				int startTnVal = Integer.parseInt(svid.substring(8, 12));
				int endTnVal = Integer.parseInt(svid.substring(13, 17));

				for (int p = startTnVal; p <= endTnVal; p++) {
					StringBuffer tnBuff = new StringBuffer(svid.substring(0,8));
					tnBuff.append(StringUtils.padNumber(p,SOAConstants.TN_LINE, true, '0'));
					totalTnList.add(tnBuff.toString());
					tnBuff = null;
				}
			}
			else
			{
				totalTnList.add(svid.toString());
			}

		}

		int tnCount =totalTnList.size();

		int i = 0;

		while (i < tnCount)
		{			
			int j = 1;

			StringBuffer tnValue = new StringBuffer();

			while (j <= 1000 && i <= tnCount-1 )
			{
				tnValue.append("'");
				tnValue.append(totalTnList.get(i));

				if ( j < 1000 && i != tnCount-1)				
					tnValue.append("',");
				else
					tnValue.append("'");

				i++;
				j++;					
			}

			StringBuffer queryTN = new StringBuffer();
			queryTN.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 ) */ ACCOUNTID FROM SOA_SUBSCRIPTION_VERSION  WHERE SPID='");
			queryTN.append(spid);
			queryTN.append("' AND NNSP='");
			queryTN.append(newSP);
			queryTN.append("' AND ONSP='");
			queryTN.append(oldSP);
			queryTN.append("' AND ACCOUNTID !='");
			queryTN.append(accountId);
			queryTN.append("' AND PORTINGTN ");
			queryTN.append(" IN ("+tnValue+" ) ");
			queryTN.append(" GROUP BY ACCOUNTID ");
			queryTN.append(" HAVING COUNT(*) > 0 ");
			queryTN.append(" \n UNION \n");
			queryTN.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 ) */ b.ACCOUNTID FROM SOA_SUBSCRIPTION_VERSION b WHERE b.SPID='");
			queryTN.append(spid);
			queryTN.append("' AND b.NNSP='");
			queryTN.append(newSP);
			queryTN.append("' AND b.ONSP='");
			queryTN.append(oldSP);
			queryTN.append("' AND b.ACCOUNTID='");
			queryTN.append(accountId);
			queryTN.append("' AND b.PORTINGTN IN (");
			queryTN.append(" SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ DISTINCT a.PORTINGTN FROM SOA_SUBSCRIPTION_VERSION a  WHERE a.PORTINGTN IN (");
			queryTN.append(tnValue);                
			queryTN.append("))");
			queryTN.append(" GROUP BY b.ACCOUNTID ");
			queryTN.append(" HAVING COUNT(b.PORTINGTN)  = ");
			queryTN.append(j-1);

			sbQuery.append(queryTN);

			//Join the individual SQL queries with UNION operator.
			if (i <= tnCount-1)
			{
				sbQuery.append(" UNION ");
			}

		} // end of construction of query

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}

	/**
	 * This method is used to create Query using criteria fields as ACCOUNTID, SPID, NewSP, OldSP, and Tn
	 * @param accountId
	 * @param spid
	 * @param newSP
	 * @param oldSP
	 * @param tnList
	 * @return query as String 
	 */
	private String getRemoveQuery(String accountId, String spid, String newSP, String oldSP, String tnList) {

		StringTokenizer st = new StringTokenizer(tnList, SOAConstants.PORTINGTN_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer svidBuffer = new StringBuffer();
		int unionAt = 1000;
		int svidCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 ) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
		sbQuery.append(spid);
		sbQuery.append("' AND NNSP='");
		sbQuery.append(newSP);
		sbQuery.append("' AND ONSP='");
		sbQuery.append(oldSP);
		sbQuery.append("' AND (ACCOUNTID!='");
		sbQuery.append(accountId);
		sbQuery.append("' OR ACCOUNTID IS NULL) AND PORTINGTN IN (");

		while(st.hasMoreTokens()) {
			if(svidCount % unionAt == 0) {
				String tnValues = svidBuffer.toString();
				tnValues = tnValues.substring(0, tnValues.length()-1);     //remove the last comma

				svidBuffer = null;
				svidBuffer = new StringBuffer();

				sbQuery.append(tnValues);
				sbQuery.append(")");
				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 ) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("' AND NNSP='");
				sbQuery.append(newSP);
				sbQuery.append("' AND ONSP='");
				sbQuery.append(oldSP);
				sbQuery.append("' AND (ACCOUNTID!='");
				sbQuery.append(accountId);
				sbQuery.append("' OR ACCOUNTID IS NULL) AND PORTINGTN IN (");
			}

			String svid = st.nextToken();

			if(svid != null && svid.length()>12)
			{

				int startTnVal = Integer.parseInt(svid.substring(8, 12));
				int endTnVal = Integer.parseInt(svid.substring(13, 17));

				for (int p = startTnVal; p <= endTnVal; p++) {
					StringBuffer tnBuff = new StringBuffer(svid.substring(0,8));
					tnBuff.append(StringUtils.padNumber(p,SOAConstants.TN_LINE, true, '0'));
					svidBuffer.append("'");
					svidBuffer.append(tnBuff.toString());            
					svidBuffer.append("',");

					svidCount++;
				}
			}
			else
			{
				svidBuffer.append("'");
				svidBuffer.append(svid);            
				svidBuffer.append("',");

				svidCount++;

			}
		}

		String svidValues = svidBuffer.toString();
		svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma
		sbQuery.append(svidValues);
		sbQuery.append(")");         //add the right parenthesis
		sbQuery.append(" AND STATUS NOT IN 'old'");			

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}	


	/**
	 * This method is used to check the status of the accountid exist for the given spid 
	 * @param accountId. 
	 * @param spid Value SPID. 
	 * @return boolean.
	 */   
	public boolean isUniqueAccountId(Value accountId, Value spid){

		boolean result= true;

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		try{

			conn = getDBConnection();
			pstmt = conn.prepareStatement(SOAQueryConstants.GET_UNIQUE_ACCOUNTID);

			pstmt.setString( 1, spid.toString() );
			pstmt.setString( 2, accountId.toString() );
			pstmt.setString( 3, spid.toString() );	

			results = pstmt.executeQuery();

			if( results.next() ){
				result = false;
			}				

		}catch(Exception exception){
			logErrors(exception, SOAQueryConstants.GET_UNIQUE_ACCOUNTID);

		}finally{

			close(results, pstmt); 
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS,"isUniqueAccountId:Account Id is:" + accountId);
			Debug.log(Debug.MSG_STATUS,"isUniqueAccountId:SPID is:" + spid);
			Debug.log(Debug.MSG_STATUS,"isUniqueAccountId:Result is:"+ result);
		}

		return result;	      
	}


	/**
	 * This method is used to set dynamic error RangeId which does not belongs to the given SPID. 
	 * Return false if there is atleast one range belongs to different SPID
	 * otherwise return true
	 *
	 * @param spid     
	 * @param rangeId
	 * @return boolean
	 */
	public boolean isRangeInDifferentSPID(Value spid, Value rangeIdList) {                
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "RangeId is : " + rangeIdList);
		}

		String rangeIdInDifferentSPID = getQueryResults(getRangeQueryInDifferentSPID(rangeIdList.toString()), spid.toString());
		if(rangeIdInDifferentSPID == null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeAllreadyInAccount:Returning result as true");
			return true;    //all of the RangeId belongs to the given SPID
		}

		//otherwise set the failed RangeId that are already belonging to different SPID
		setDynamicErrorMessage(rangeIdInDifferentSPID);
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeAllreadyInAccount:Returning result as false");
		return false;
	}

	/**
	 * This method is used to execute the given query and return the result as String if any record does matche the given spid
	 * @param query
	 * @param spid
	 * @return String 
	 */    
	private String getQueryResults(String query, String spid) {
		boolean result= true;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet results = null;
		StringBuffer tnBuffer = new StringBuffer();

		try {
			conn = getDBConnection();
			pstmt = conn.prepareStatement(query);

			results = pstmt.executeQuery();
			while(results.next()) {
				String rsSPID = results.getString(1);
				Object value = results.getObject(2);
				if(!rsSPID.equals(spid))
				{
					if(results.isFirst())
						tnBuffer.append(value);
					else
						tnBuffer.append("\t" + value);
				}

			}

		} catch(Exception exception){
			logErrors(exception, query);

		} finally{

			close(results, pstmt);
		}

		return tnBuffer.length()>0 ? tnBuffer.toString():null;
	}


	/**
	 * This method is used to create Query using criteria fields as SPID, and RANGEID     
	 * @param spid
	 * @param rangeIdList     
	 * @return query as String 
	 */
	private String getRangeQueryInDifferentSPID(String rangeIdList) {

		StringTokenizer st = new StringTokenizer(rangeIdList, SOAConstants.SVID_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer rangeIdBuffer = new StringBuffer();
		int unionAt = 1000;
		int rangeIdCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ SPID, RANGEID FROM SOA_SUBSCRIPTION_VERSION ");        
		sbQuery.append("WHERE RANGEID IN (");

		while(st.hasMoreTokens()) {
			if(rangeIdCount % unionAt == 0) {
				String rangeIdValues = rangeIdBuffer.toString();
				rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma

				rangeIdBuffer = null;
				rangeIdBuffer = new StringBuffer();

				sbQuery.append(rangeIdValues);                
				sbQuery.append(")");                
				sbQuery.append(" GROUP BY SPID, RANGEID " );

				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ SPID, RANGEID FROM SOA_SUBSCRIPTION_VERSION ");        
				sbQuery.append("WHERE RANGEID IN (");
			}

			String rangeId = st.nextToken();

			//svidBuffer.append("'");
			rangeIdBuffer.append(rangeId);
			rangeIdBuffer.append(",");
			//svidBuffer.append("',");

			rangeIdCount++;
		}

		String rangeIdValues = rangeIdBuffer.toString();
		rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma
		sbQuery.append(rangeIdValues);
		sbQuery.append(")");         //add the right parenthesis
		sbQuery.append(" GROUP BY SPID, RANGEID " );

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}   
	/**
	 * This method is used to set dynamic error SvId and return false if any TN does not exist in SOA DB 
	 * otherwise return true
	 * @param spid
	 * @param newSP
	 * @param oldSP
	 * @param tnList
	 * @return boolean
	 */
	public  boolean isTNInSOADB( Value spid, Value newSP, Value oldSP, Value tnList) {
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "New SP is : " + newSP);
			Debug.log(Debug.MSG_STATUS, "Old SP is : " + oldSP);
			Debug.log(Debug.MSG_STATUS, "Tn is : " + tnList);
		}

		String tnInSOADB = getQueryResultAddSvAccount(getTNExistQuery(spid.toString(), newSP.toString(), oldSP.toString(), tnList.toString()));
		if(tnInSOADB != null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isTNInSOADB:Returning result as true");
			return true;    //Tn exist in SOA DB
		}

		//none of the Tn exist in SOA DB
		setDynamicErrorMessage(tnList.toString());
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isTNInSOADB:Returning result as false");
		return false;
	}
	/**
	 * This method is used to execute the given query and return the result as String
	 * @param spid
	 * @param newSP
	 * @param oldSP
	 * @param tnList
	 * @return String
	 */
	private String getTNExistQuery(String spid, String newSP, String oldSP, String tnList) {

		StringTokenizer st = new StringTokenizer(tnList, SOAConstants.PORTINGTN_SEPARATOR);
		StringBuffer tnQuery = new StringBuffer();
		StringBuffer svidBuffer = new StringBuffer();
		int unionAt = 1000;
		int svidCount = 1;

		//set spid in where clause
		tnQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2 ) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
		tnQuery.append(spid);
		tnQuery.append("' AND NNSP='");
		tnQuery.append(newSP);
		tnQuery.append("' AND ONSP='");
		tnQuery.append(oldSP);
		tnQuery.append("' AND STATUS not in ('old') ");
		tnQuery.append(" AND PORTINGTN IN (");
		
		while(st.hasMoreTokens()) {
			if(svidCount % unionAt == 0) {
				String tnValues = svidBuffer.toString();
				tnValues = tnValues.substring(0, tnValues.length()-1);     //remove the last comma

				svidBuffer = null;
				svidBuffer = new StringBuffer();

				tnQuery.append(tnValues);
				tnQuery.append(")");
				tnQuery.append(" \n UNION \n");
				tnQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
				tnQuery.append(spid);
				tnQuery.append("' AND NNSP='");
				tnQuery.append(newSP);
				tnQuery.append("' AND ONSP='");
				tnQuery.append(oldSP);
				tnQuery.append("' AND STATUS not in ('old') ");
				tnQuery.append(" AND PORTINGTN IN (");
			}

			String svid = st.nextToken();
			if(svid != null && svid.length()>12)
			{

				int startTnVal = Integer.parseInt(svid.substring(8, 12));
				int endTnVal = Integer.parseInt(svid.substring(13, 17));

				for (int p = startTnVal; p <= endTnVal; p++) {
					StringBuffer tnBuff = new StringBuffer(svid.substring(0,8));
					tnBuff.append(StringUtils.padNumber(p,SOAConstants.TN_LINE, true, '0'));
					svidBuffer.append("'");
					svidBuffer.append(tnBuff.toString());            
					svidBuffer.append("',");

					svidCount++;
				}
			}
			else
			{
				svidBuffer.append("'");
				svidBuffer.append(svid);            
				svidBuffer.append("',");

				svidCount++;

			}
		}

		String svidValues = svidBuffer.toString();
		svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma
		tnQuery.append(svidValues);
		tnQuery.append(")");         //add the right parenthesis

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + tnQuery.toString());
		return tnQuery.toString();      //return query
	}
	/**
	 * This method is used to set dynamic error SvId and return false if any SV does not exist in SOA DB 
	 * otherwise return true
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return boolean
	 */
	public boolean isSvIdInSOADB(Value spid, Value svidList, Value regionId) {        

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "SvIdList is : " + svidList);
			Debug.log(Debug.MSG_STATUS, "RegionId is : " + regionId);
		}

		String svidInSOADB = getQueryResultAddSvAccount(getSvIdExistQuery(spid.toString(), svidList.toString(), regionId.toString()));
		if(svidInSOADB != null) {
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSvIdInSOADB:Returning result as true");
			return true;    //SvId exist in SOA DB
		}

		//none of the SvId exist in SOA DB
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isSvIdInSOADB:Returning result as false");
		return false;
	}
	/**
	 * This method is used to execute the given query and return the result as String
	 * @param spid
	 * @param svidList
	 * @param regionId
	 * @return String
	 */

	private String getSvIdExistQuery(String spid, String svidList, String regionId) {

		StringTokenizer st = new StringTokenizer(svidList, SOAConstants.SVID_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer svidBuffer = new StringBuffer();
		int unionAt = 1000;
		int svidCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
		sbQuery.append(spid);
		sbQuery.append("' AND REGIONID=");
		sbQuery.append(regionId);
		sbQuery.append("AND SVID IN (");

		while(st.hasMoreTokens()) {
			if(svidCount % unionAt == 0) {
				String svidValues = svidBuffer.toString();
				svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma

				svidBuffer = null;
				svidBuffer = new StringBuffer();

				sbQuery.append(svidValues);
				sbQuery.append(")");
				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("' AND REGIONID=");
				sbQuery.append(regionId);
				sbQuery.append("AND SVID IN (");
			}

			String svid = st.nextToken();

			//svidBuffer.append("'");
			svidBuffer.append(svid);
			svidBuffer.append(",");
			//svidBuffer.append("',");

			svidCount++;
		}

		String svidValues = svidBuffer.toString();
		svidValues = svidValues.substring(0, svidValues.length()-1);     //remove the last comma
		sbQuery.append(svidValues);
		sbQuery.append(")");         //add the right parenthesis

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	}
	/**
	 * This method is used to set dynamic error RangeId and return false if any SV does not exist in SOA DB
	 * otherwise return true
	 * @param spid
	 * @param rangeIdList
	 * @return boolean
	 */
	public boolean isRangeIdInSOADB(Value spid, Value rangeIdList) {        

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

			Debug.log(Debug.MSG_STATUS, "SPID is : " + spid);
			Debug.log(Debug.MSG_STATUS, "RangeId is : " + rangeIdList);
		}

		String rangeIdInSOADB = getQueryResultAddSvAccount(getRangeIdExitQuery(spid.toString(), rangeIdList.toString()));
		if(rangeIdInSOADB != null) {

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeIdInSOADB:Returning result as true");

			return true;    //RangeId exist in SOA DB
		}

		//none of the RangeId exist in SOA DB
	
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS, "SOACustomSvDBFunctions:isRangeIdInSOADB:Returning result as false");
		return false;
	}
	/**
	 * This method is used to create Query using criteria fields as SPID, and RANGEID
	 * @param spid
	 * @param rangeIdList
	 * @return String
	 */
	private String getRangeIdExitQuery(String spid,String rangeIdList) {

		StringTokenizer st = new StringTokenizer(rangeIdList, SOAConstants.SVID_SEPARATOR);
		StringBuffer sbQuery = new StringBuffer();
		StringBuffer rangeIdBuffer = new StringBuffer();
		int unionAt = 1000;
		int rangeIdCount = 1;

		//set spid in where clause
		sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");        
		sbQuery.append(spid);
		sbQuery.append("' AND RANGEID IN (");


		while(st.hasMoreTokens()) {
			if(rangeIdCount % unionAt == 0) {
				String rangeIdValues = rangeIdBuffer.toString();
				rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma

				rangeIdBuffer = null;
				rangeIdBuffer = new StringBuffer();

				sbQuery.append(rangeIdValues);                
				sbQuery.append(")");                
				// sbQuery.append(" GROUP BY SPID, RANGEID " );

				sbQuery.append(" \n UNION \n");
				sbQuery.append("SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ RANGEID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID='");
				sbQuery.append(spid);
				sbQuery.append("AND RANGEID IN (");
			}

			String rangeId = st.nextToken();

			//svidBuffer.append("'");
			rangeIdBuffer.append(rangeId);
			rangeIdBuffer.append(",");
			//svidBuffer.append("',");
			rangeIdCount++;
		}

		String rangeIdValues = rangeIdBuffer.toString();
		rangeIdValues = rangeIdValues.substring(0, rangeIdValues.length()-1);     //remove the last comma
		sbQuery.append(rangeIdValues);
		sbQuery.append(")");         //add the right parenthesis
		//sbQuery.append(" GROUP BY SPID, RANGEID " );

		if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
			Debug.log(Debug.NORMAL_STATUS, "Query formed as : " + sbQuery.toString());
		return sbQuery.toString();      //return query
	} 
	/**
	 * This method is used to execute the given query and return the result as String
	 * @param query
	 * @return String
	 */
	private String getQueryResultAddSvAccount(String query) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet results = null;
		StringBuffer tnBuffer = new StringBuffer();

		try {
			conn = getDBConnection();
			pstmt = conn.prepareStatement(query);

			results = pstmt.executeQuery();
			while(results.next()) {
				Object value = results.getObject(1);
				if(results.isFirst())
					tnBuffer.append(value);
				else
					tnBuffer.append("\t" + value);
			}

		} catch(Exception exception){
			logErrors(exception, query);

		} finally{

			close(results, pstmt);
		}

		return tnBuffer.length()>0 ? tnBuffer.toString():null;
	}
	public boolean absentAltEndUserLocationValue(String requestType)
	{
		boolean result = false;

		String tn = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
		}
		if (requestType.equals("SvCreateRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/AlternativeEndUserLocationValue");
		}
		if (requestType.equals("SvModifyRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/AlternativeEndUserLocationValue");
		}
		if (!result)
		{
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);
				addFailedTN(tn);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for absentAltEndUserLocationValue: "+tn);

				errorMessage.append("[" + tn + "] ");
			}
			setDynamicErrorMessage(errorMessage.toString());
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for absentAltEndUserLocationValue: "+result);

		return result;
	}
	public boolean absentAltEndUserLocationType(String requestType)
	{
		boolean result = false;

		String tn = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
		}
		if (requestType.equals("SvCreateRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/AlternativeEndUserLocationType");
		}
		if (requestType.equals("SvModifyRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/AlternativeEndUserLocationType");
		}
		if (!result)
		{
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);

				addFailedTN(tn);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for absentAltEndUserLocationType: "+tn);	
				errorMessage.append("[" + tn + "] ");				
			}

			setDynamicErrorMessage(errorMessage.toString());
		}	

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for absentAltEndUserLocationType: "+result);		

		return result;
	}
	public boolean absentAltBillingID(String requestType)
	{
		boolean result = false;

		String tn = null;

		ArrayList tnList = new ArrayList();

		StringBuffer errorMessage = new StringBuffer();

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
		}
		if (requestType.equals("SvCreateRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/AlternativeBillingId");
		}
		if (requestType.equals("SvModifyRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/AlternativeBillingId");
		}
		if (!result)
		{
			for (int j = 0;j < tnList.size() ;j++ )
			{
				tn = (String)tnList.get(j);				
				addFailedTN(tn);

				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for absentAltBillingID: "+tn);	
				errorMessage.append("[" + tn + "] ");
			}
			setDynamicErrorMessage(errorMessage.toString());
		}
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for absentAltBillingID: "+result);

		return result;
	}
	
	public boolean absentLastAltSPID(String requestType)
	{
		boolean result = false;
		
		String tn = null;

		ArrayList tnList = null;

		if (existsValue("tnList"))
		{
			tnList =(ArrayList)getContextValue("tnList");
		}
		
		if (SOAConstants.SV_CREATE_REQUEST.equals(requestType))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/LastAlternativeSPID");
		}
		else if (SOAConstants.SV_MODIFY_REQUEST.equals(requestType))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/LastAlternativeSPID");
		}
		if (!result)
		{
		    if(tnList!=null)
		    {
			    for (int j = 0;j < tnList.size() ;j++ )
				{
					tn = (String)tnList.get(j);
					addFailedTN(tn);
				}
		    }
		    String spid = value("/SOAMessage/UpstreamToSOA/UpstreamToSOAHeader/InitSPID").toString();
		    StringBuffer errorMessage = new StringBuffer();
		    errorMessage.append("[" + spid + "] ");
		    setDynamicErrorMessage(errorMessage.toString());
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for LastAlternativeSPID: "+result);

		return result;
	}
	/**
	 * This method returns boolean value TRUE if the causecode populated in the request XML and the cause code present in the 
	 * SOA_SUBSCRIPTION_VERSION table has same value otherwise return FALSE.
	 * @param causeCode
	 * @return result
	 */
	public boolean iSCauseCodeDifferent(Value causeCode){

		boolean result = true;
		String causecode = null;
		String portingTn = null;
		Value tnStatus = null;
		StringBuffer errorMessage = new StringBuffer();		
		ArrayList svDataList = null;

		if (existsValue("modifySvRs")){

			svDataList =(ArrayList)getContextValue("modifySvRs");
			
			if(svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
					{
					ModifySvData svData =(ModifySvData)svDataList.get(i);
					portingTn = svData.portingTn;
					causecode = svData.causecode;
					tnStatus = new Value( svData.status );
					if(causecode != null){
	
						if (!causeCode.equals(causecode) && tnStatus.equals("conflict") ){
	
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						}
					}
				}
			}
		}

		if (!result)
			setDynamicErrorMessage(errorMessage.toString());

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for getStatus: "+result);

		return result;

	}

	/**
	 * This method returns true if the value of LASTREQUESTTYPE and LASTPROCESSING column of SOA_SUBSCRIPTION_VERSION table are 
	 * same and have value "SvActivateRequest" or (value of LASTPROCESSING column is "NPACRequestSuccessReply"  and value of 
	 * LASTREQUESTTYPE is "SvActivateRequest")otherwise return false for TN.
	 * @return
	 */
	public boolean isLastProcessingSameAsLastRequestTypeTn(){
		boolean result = false;
		Iterator iter = null;
		ArrayList svDataList = null;
		svDataList =(ArrayList)getContextValue("rsDataList");
		StringBuffer errorMessage = new StringBuffer();
		ArrayList portingTnsList = new ArrayList();
		ArrayList tnList = null;
		tnList =(ArrayList)getContextValue("tnList");

		if(svDataList != null)
		{

			for (int i=0;i < svDataList.size();i++ )
			{
				ResultsetData svData =(ResultsetData)svDataList.get(i);

				if(svData.lastRequestType != null && !svData.lastRequestType.equals("") 
						&& svData.lastProcessing != null &&  !svData.lastProcessing.equals("")
						&& svData.lastRequestType.equals(SOAConstants.SV_ACTIVATE_REQUEST)
						&& (svData.lastRequestType.equals(svData.lastProcessing)|| 
								svData.lastProcessing.equals(SOAConstants.NPAC_REQUEST_SUCCESS_REPLY))){			

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

						Debug.log(Debug.MSG_STATUS,"LASTREQUESTTYPE and LASTPROCESSING column values are same or " +
								"success reply has come for TN["+svData.portingTn+"]");
					}
				}else{

					portingTnsList.add(svData.portingTn);
				}
			}
			if(tnList != null )
			{
				iter = tnList.listIterator();
				
				while (iter.hasNext())
				{
					String tn = (String)iter.next();
					if (!portingTnsList.contains(tn))
					{
						addFailedTN(tn);					
						errorMessage.append("[" + tn + "] ");
	
						result=true;
					}
				}
			}
		}		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isLastProcessingSameAsLastRequestTypeTn: "+result);

		if (result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}
		return result;
	}
	/**
	 * This method returns true if the value of LASTREQUESTTYPE and LASTPROCESSING column of SOA_SUBSCRIPTION_VERSION table are 
	 * same and have value "SvActivateRequest" or (value of LASTMESSAGE column is "NPACRequestSuccessReply"  and value of 
	 * LASTREQUESTTYPE is "SvActivateRequest")otherwise return false for SvID.
	 * @return
	 */

	public boolean isLastProcessingSameAsLastRequestTypeSv(){


		String lastReqType =(String)getContextValue("lastRequestType");

		String lastProcessing = (String)getContextValue("lastProcessing");

		boolean result = false;

		if( lastReqType != null && !lastReqType.equals("") && lastProcessing != null && !lastProcessing.equals("") 
				&& lastReqType.equals(SOAConstants.SV_ACTIVATE_REQUEST) 
				&& (lastReqType.equals(lastProcessing) || lastProcessing.equals(SOAConstants.NPAC_REQUEST_SUCCESS_REPLY) ) )
		{
			result = true;
		}

		return result;

	}
	/**
	 * This method returns true if the value of LASTREQUESTTYPE and LASTPROCESSING column of SOA_SUBSCRIPTION_VERSION table are 
	 * same or (value of LASTPROCESSING column is "NPACRequestSuccessReply" otherwise return false for SvID.
	 * @return
	 */
	public boolean isLastProcessingSameAsLastRequestTypeTn(String requestType){

		boolean result = false;
		Iterator iter = null;
		ArrayList svDataList = null;
		ArrayList portingTnsList = new ArrayList();
		ArrayList dbTnlst = new ArrayList();
		ArrayList tnList = null;
		tnList =(ArrayList)getContextValue("tnList");
		StringBuffer errorMessage = new StringBuffer();
		svDataList =(ArrayList)getContextValue("createSvRs");

		if(svDataList != null && svDataList.size() > 0){

			Debug.log(Debug.MSG_STATUS, "SVData list is not null and size is "+svDataList.size());
			for (int i=0;i < svDataList.size();i++ ){
				CreateSvData svData =(CreateSvData)svDataList.get(i);
				dbTnlst.add(svData.portingTn);

				if(svData.lastRequestType != null && !svData.lastRequestType.equals("") 
						&& svData.lastProcessing != null &&  !svData.lastProcessing.equals("")
						&& svData.lastRequestType.equals(requestType)
						&& (svData.lastRequestType.equals(svData.lastProcessing)|| 
								svData.lastProcessing.equals(SOAConstants.NPAC_REQUEST_SUCCESS_REPLY))){			

					if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){

						Debug.log(Debug.MSG_STATUS,"LASTREQUESTTYPE and LASTPROCESSING column values are same or " +
								"success reply has come for TN["+svData.portingTn+"]");
					}


				}else{

					portingTnsList.add(svData.portingTn);
				}
			}
			
			if( tnList != null)
			{
				iter = tnList.listIterator();

				while (iter.hasNext())
				{
					String tn = (String)iter.next();
					if (!portingTnsList.contains(tn) && dbTnlst.contains(tn))
					{
						addFailedTN(tn);					
						errorMessage.append("[" + tn + "] ");

						result=true;
					}
				}
			}

		}		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result returned for isLastProcessingSameAsLastRequestTypeTn: "+result);

		if (result)
		{
			setDynamicErrorMessage(errorMessage.toString());
		}
		return result;
	}

	/**
	 * This method is used to check the status of the EFFECTIVERELEASEDATEFLAG whether 
	 * it returns true or false  
	 * @param spid Value the SPID
	 * @return boolean Value
	 */
	public boolean isEffectiveReleaseDateFlagEnabled(Value spid){

		boolean result = false;

		try{
			String nancflag = String.valueOf(NANCSupportFlag.getInstance(spid.toString()).getEffectiveReleaseDateFlag());
			if(nancflag != null && nancflag.equals("1"))
				result=true;

			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"isEffectiveReleaseDateFlagEnabled: "+result);

		}catch(Exception exception){
			logErrors(exception, exception.toString());
		}
		return result;

	}
	/**
	 * Return true if given EffectiveReleaseDate is absent otherwise false.
	 * @param effectiveReleaseDate
	 * @return
	 */

	public boolean absentEffectiveReleaseDate()
	{
		boolean result = false;

		result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvDisconnectRequest/EffectiveReleaseDate");

		return result;
	}
	
	/**
	 * Return true if given EffectiveReleaseDate is absent otherwise false.
	 * @param effectiveReleaseDate
	 * @return
	 */

	public boolean absentEffectiveReleaseDateTn()
	{
		boolean result = false;
		StringBuffer errorMessage = new StringBuffer();
		String portingTn = null;
		result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvDisconnectRequest/EffectiveReleaseDate");
		
		ArrayList svDataList = null;

		if (result){	    

			svDataList =(ArrayList)getContextValue("rsDataList");		
			if (svDataList != null)
			{
				for (int i=0;i < svDataList.size();i++ )
				{
					ResultsetData svData =(ResultsetData)svDataList.get(i);

					portingTn = svData.portingTn;
					addFailedTN(portingTn);
					errorMessage.append("["+portingTn+"] ");
								
				}
				setDynamicErrorMessage(errorMessage.toString());
			}
			
		}
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for absentEffectiveReleaseDate: "+result);
				
		return result;
	}
	/**
	 * This method will update the dueDate with SYSTEMDATE + PUSHDUEDATE and set the duedate in 
	 * customerContext.
	 * @param pushDueDateMin
	 * @return
	 */
	public boolean pushDueDate(Integer pushDueDateMin) {
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
			Debug.log(Debug.MSG_STATUS,
					"DueDate falls in past and PUSHDUADATE flag supports the spid " +
					"and request initiated from API");
		}
		boolean result = true;
		try {
			String systemDate1 = TimeZoneUtil.convert("LOCAL",
					"MM-dd-yyyy-hhmma", new Date());
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS,
						"The value of local system date 1:[" + systemDate1
								+ "]");

			Date newDueDate = TimeZoneUtil.parse("LOCAL", "MM-dd-yyyy-hhmma",
					systemDate1);

			if (pushDueDateMin > 0)
				newDueDate.setTime(newDueDate.getTime() + pushDueDateMin * 60
						* 1000);

			String newDueDate1 = TimeZoneUtil.convert("LOCAL",
					"MM-dd-yyyy-hhmmssa", newDueDate);
			if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS,
						"The value of the updated newDueDate 1: ["
								+ newDueDate1 + "]");

			CustomerContext.getInstance().set("newDueDate", newDueDate1);

		} catch (MessageException me) {
			Debug.error("Error occured while pushing DueDate: "
					+ me.getMessage());
			result = false;
		} catch (FrameworkException fe) {
			Debug.error("Error occured while pushing DueDate: "
					+ fe.getMessage());
			result = false;
		} catch (Exception e) {
			Debug.error("Error occured while pushing DueDate: "
					+ e.getMessage());
			result = false;
		}

		return result;
	}
	/**
	 * This method is used to get the NANC 441 (Simple Port Indicator) Flag for spid
	 * @param spid Value the SPID 
	 * @return String Value.
	 */
	public String getNANC441SimplePortIndicatorFlag(Value spid) {

		Boolean nanc441flag = (Boolean) getContextValue("simplePortIndicatorNANC441Flag");

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Executing getNANC441SimplePortIndicatorFlag: "
							+ nanc441flag);
		}
		return nanc441flag.toString();

	}

	/** This method is used to check presence of Simple Port Indicator node for NNSP/ONSP for passed request Type. 
	 * 
	 * @param requestType
	 * @return boolean value
	 */
	public boolean absentSimplePortIndicator(String requestType) {

		boolean result = false;
		String tn = null;
		String spid = null;
		ArrayList tnList = null;

		if (existsValue("tnList")) {
			tnList = (ArrayList) getContextValue("tnList");
		}
		
		if (SOAConstants.SV_CREATE_REQUEST.equals(requestType)) {
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/NNSPSimplePortIndicator");
		} else if (SOAConstants.SV_RELEASE_REQUEST.equals(requestType)
				|| SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST.equals(requestType)) {
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/"
					+ requestType + "/ONSPSimplePortIndicator");
		} else if (SOAConstants.SV_MODIFY_REQUEST.equals(requestType)) {
			result = !(present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/NNSPSimplePortIndicator") || present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/ONSPSimplePortIndicator"));
		}

		if (!result) {
			if(tnList!=null)
			{
				for (int j = 0; j < tnList.size(); j++) {
				 tn = (String) tnList.get(j);
				 addFailedTN(tn);
				}
			}
			spid = value(
					"/SOAMessage/UpstreamToSOA/UpstreamToSOAHeader/InitSPID")
					.toString();
			StringBuffer errorMessage = new StringBuffer();
			errorMessage.append("[" + spid + "] ");
			setDynamicErrorMessage(errorMessage.toString());
		}

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for absentSimplePortIndicator: " + result);
		}
		return result;
	}

	/**This method is used to know whether NNSP can modify NNSP SimplePortIndicator depend on sv status.
	 *
	 * @param status
	 * @return boolean, result
	 */
	public boolean canModifySimplePortIndicator(String status) {

		boolean result = true;
		String tnStatus = null;
		String portingTn = null;
		String simplePortIndicator = null;
		String initSpid = null;
		String nnsp = null;
		String onsp = null;
		Date nnspDueDate = null;
		StringBuffer errorMessage = null;
		ArrayList svDataList = null;
		ModifySvData svData = null;

		if (existsValue("modifySvRs")) {
			errorMessage = new StringBuffer();
			svDataList = (ArrayList) getContextValue("modifySvRs");
			if(svDataList != null){
					for (int ctr = 0; ctr < svDataList.size(); ctr++) {
					svData = (ModifySvData) svDataList.get(ctr);
					portingTn = svData.portingTn;
					nnsp = svData.nnsp;
					onsp = svData.onsp;
					tnStatus = svData.status;
					initSpid = (String) getContextValue("initSpid");
					//Checking is InitSPID acting as NNSP?
					if (initSpid.equals(nnsp)
							&& !nnsp.equals(onsp)) {
						simplePortIndicator = svData.onspSimplePortindicator;
	
						// allow modification only if status is in "pending" & SPID is NNSP. 
						if (!tnStatus.equals(status)) {
							addFailedTN(portingTn);
							errorMessage.append("[" + portingTn + "] ");
							result = false;
						} else {
							nnspDueDate = svData.nnspDate;
							//If status is in "pending" & ONSP didn't concure then allow modification means onspSimplePortIndicator is blank.
							// checking nnspDueDate not null condition, If SvRelease is initial request & NNSP tring to modify record without concurred 
							//then this BR should not be thrown because NNSP has not created that SV.
							
							if (simplePortIndicator != null
									&& simplePortIndicator.trim().length() > 0 && nnspDueDate!=null) {
								addFailedTN(portingTn);
								errorMessage.append("[" + portingTn + "] ");
								result = false;
							}
						}
					}
				}
			}
		}

		if (!result) {
			setDynamicErrorMessage(errorMessage.toString());
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result returned for canModifySimplePortIndicator: "
							+ result);
		}
		return result;
	}

	/**This method is used to know whether NNSP can modify NNSP SimplePortIndicator depend on sv status.
	 * 
	 * @param status
	 * @return boolean, result
	 */
	public boolean canModifySimplePortIndicatorSvId(String status) {

		boolean result = true;
		String nnsp = null;
		String onsp = null;
		String svIdstatus = null;
		String onspSPISvId = null;
		Boolean existSv = (Boolean) getContextValue("existSv");
		String initSpid = (String) getContextValue("spidSvId");
		nnsp = (String) getContextValue("nnspSvId");
		onsp = (String) getContextValue("onspSvId");
		svIdstatus = (String) getContextValue("statusSvId");
		//Getting ONSP Simple Port indicator
		onspSPISvId = (String) getContextValue("onspSimplePortIndicatorSvId");
		Date nnspDueDate = (Date)getContextValue("nnspduedateSvId");
		
		// Checking is InitSPID acting as NNSP?
		if (existSv.booleanValue()
				&& initSpid.equals(nnsp) && !nnsp.equals(onsp)) {
			// allow modification only if status is in "pending" & SPID is NNSP. 
			if (!svIdstatus.equals(status)) {
				result = false;
			} else {
				//If status is in "pending" & ONSP didn't concure then allow modification means onspSimplePortIndicator is blank.
				//checking nnspDueDate not null condition, If SvRelease is initial request & NNSP tring to modify record without concurred 
				//then this BR should not be thrown because NNSP has not created that SV.
				if (onspSPISvId != null && onspSPISvId.trim().length() > 0 && nnspDueDate!=null) {
					result = false;
				}
			}
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result returned for canModifySimplePortIndicatorSvId: "
							+ result);
		}
		return result;
	}

	/**This method is to check, Only Onsp can modify ONSPSimplePortIndicator. 
	 * 
	 * @return boolean, result
	 */
	public boolean isOnspModifingONSPSimplePortIndicator() {

		boolean result = true;

		String spid = null;

		String nnsp = null;

		String onsp = null;

		String portingTn = null;

		String failedTn = null;

		StringBuffer errorMessage = null;

		ArrayList failedTns = null;

		ArrayList svDataList = null;

		ModifySvData svData = null;
		
		if (existsValue("modifySvRs")) {
			svDataList = (ArrayList) getContextValue("modifySvRs");
			if(svDataList != null){
				    failedTns = new ArrayList();
					for (int i = 0; i < svDataList.size(); i++) {
					svData = (ModifySvData) svDataList.get(i);
					portingTn = svData.portingTn;
					nnsp = svData.nnsp;
					onsp = svData.onsp;
					spid = (String) getContextValue("initSpid");
					int tnIndex = failedTns.indexOf(portingTn.toString());
					//If spid is acting as nnsp & it is not a intra port record.
					// then add in to failed Tn list.
					if (spid.equals(nnsp) && !nnsp.equals(onsp)) {
						failedTns.add(portingTn);
					}
				}
			}
		}

		if (failedTns!=null && !failedTns.isEmpty()){
			    errorMessage = new StringBuffer();
				for (int j = 0; j < failedTns.size(); j++) {
					
					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug
								.log(Debug.MSG_STATUS,
										"Inside for Loop: isOnspModifingONSPSimplePortIndicator ");
					}
					failedTn = (String) failedTns.get(j);
					addFailedTN(failedTn);
					errorMessage.append("[" + failedTn + "] ");
				}
				setDynamicErrorMessage(errorMessage.toString());
				result = false;
			}
	
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for isOnspModifingONSPSimplePortIndicator : "
							+ result);
		}
		return result;
	}

	/**This method is to check, Only Nnsp can modify NNSPSimplePortIndicator.
	 * 
	 * @return boolean, result
	 */
	public boolean isNnspModifingNNSPSimplePortIndicator() {

		boolean result = true;

		String spid = null;

		String nnsp = null;

		String onsp = null;

		String portingTn = null;

		String failedTn = null;

		StringBuffer errorMessage = null;

		ArrayList failedTns = null;

		ArrayList svDataList = null;

		ModifySvData svData = null;

		if (existsValue("modifySvRs")) {
			svDataList = (ArrayList) getContextValue("modifySvRs");
			if(svDataList != null){
				    failedTns = new ArrayList();
					for (int i = 0; i < svDataList.size(); i++) {
					svData = (ModifySvData) svDataList.get(i);
					portingTn = svData.portingTn;
					nnsp = svData.nnsp;
					onsp = svData.onsp;
					spid = (String) getContextValue("initSpid");
					//If spid is acting as onsp & it is not a intra port record.
					// then add in to failed Tn list.
					if (spid.equals(onsp) && !nnsp.equals(onsp)) {
						failedTns.add(portingTn);
					}
				}
			}
		}

		if (failedTns!=null && !failedTns.isEmpty()) {
			errorMessage = new StringBuffer();
			for (int j = 0; j < failedTns.size(); j++) {
					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug
								.log(Debug.MSG_STATUS,
										"Inside for Loop: isNnspModifingNNSPSimplePortIndicator");
					}
					failedTn = (String) failedTns.get(j);
					addFailedTN(failedTn);
					errorMessage.append("[" + failedTn + "] ");
				}
				setDynamicErrorMessage(errorMessage.toString());
				result = false;
			}
	
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for isNnspModifingNNSPSimplePortIndicator : "
							+ result);
		}

		return result;
	}

	/**
	 * This method is to check, Only Onsp can modify ONSPSimplePortIndicator
	 * using SVID.
	 * 
	 * @return boolean , result
	 */
	public boolean isOnspModifingONSPSimplePortIndicatorSvId() {

		boolean result = true;
		String initspid = null;
		String nnsp = null;
		String onsp = null;
		Boolean existSv = (Boolean) getContextValue("existSv");
		initspid = (String) getContextValue("spidSvId");
		nnsp = (String) getContextValue("nnspSvId");
		onsp = (String) getContextValue("onspSvId");
		
		// If init spid is a nnsp then throw BR
		// and it is not intra port record.
		if (existSv.booleanValue() && initspid.equals(nnsp)
				&& !nnsp.equals(onsp)) {
			result = false;
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for isOnspModifingONSPSimplePortIndicatorSvId : "
							+ result);
		}
		return result;
	}

	/**
	 * This method is to check, Only Nnsp can modify NNSPSimplePortIndicator
	 * using SVID.
	 * 
	 * @return boolean, result
	 */
	public boolean isNnspModifingNNSPSimplePortIndicatorSvId() {

		boolean result = true;
		String initspid = null;
		String nnsp = null;
		String onsp = null;

		Boolean existSv = (Boolean) getContextValue("existSv");
		initspid = (String) getContextValue("spidSvId");
		nnsp = (String) getContextValue("nnspSvId");
		onsp = (String) getContextValue("onspSvId");
		
		// If init spid is a onsp then throw BR
		// and it is not intra port record.
		if (existSv.booleanValue() && initspid.equals(onsp)
				&& !nnsp.equals(onsp)) {

			result = false;
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for isNnspModifingNNSPSimplePortIndicatorSvId : "
							+ result);
		}
		return result;
	}


	/** This method will check whether npanxx belong to Canada region or not.
	 *  This will be called at the time of Port-In request submittion.
	 * 
	 * @return boolean, result.
	 */
	public boolean isNpaNxxBelongToCNSPI() {

		boolean result = true;

		String dashX = null;

		String failedTn = null;

		TreeSet failedDashXList = new TreeSet();

		TreeSet failedTnList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;

		ArrayList npanxxDbList = new ArrayList();

		ArrayList npanxxDataList = null;

		ArrayList failedIndivisualTnList = null;
		
		List canadianTnList = null;

		StringBuffer errorMessage = null;

		String region = null;

		Boolean spidBelongsToUSandCNandSPIon = (Boolean) getContextValue("spidBelongsToUS_CNandSPIon");
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongToCNSPI, The value of spidBelongsToUS_CNandSPIon : "
							+ spidBelongsToUSandCNandSPIon
									.booleanValue());
		}
		
		if (existsValue("npanxxDataList")) {
			npanxxDataList = (ArrayList) getContextValue("npanxxDataList");
				
			if(npanxxDataList != null){
				for (int i = 0; i < npanxxDataList.size(); i++) {
					NpaNxxData nnData = (NpaNxxData) npanxxDataList.get(i);
					region = nnData.regionId;
		
					//adding all npanxx which belongs to canada region  (7).
					if (SOAConstants.CANADA_REGION.equals(region)) {
						npanxxDbList.add(nnData.npanxx);
					}
				}
			}
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, "isNpaNxxBelongToCNSPI, List of NPANXx belongs to CN (7) region npanxxDbList:" + npanxxDbList);
		}
		
		if(!npanxxDbList.isEmpty())
		{
			if (existsValue("npaNxxXList")) {
				
				npaNxxXList = (TreeSet) getContextValue("npaNxxXList");
				if(npaNxxXList != null){
					String npanxx = null;
					Iterator npaNxxXIterator = npaNxxXList.iterator();
					while (npaNxxXIterator.hasNext()) {
						dashX = (String) npaNxxXIterator.next();
						npanxx = dashX.substring(0, dashX.length() - 1);
						// npanxx belong to region 7 (canada) will be available in npanxxDbList.
						if (npanxxDbList.contains(npanxx)) {
							failedDashXList.add(dashX);
						}
					}
				}
			}

			result = failedDashXList.isEmpty();
			if (!result) {
					npaNxxXDashx = (HashMap) getContextValue("npaNxxXDashx");
					String dash_X = null;
					Iterator failedDashXIterator = failedDashXList.iterator();
				
					if(npaNxxXDashx != null){
					//Getting tn from failed dash x list.
						while (failedDashXIterator.hasNext()) {
							dash_X = ((String) failedDashXIterator.next());
							failedIndivisualTnList = (ArrayList) npaNxxXDashx.get(dash_X);
			
							failedTnList.addAll(failedIndivisualTnList);
			
						}
					}
					if(spidBelongsToUSandCNandSPIon.booleanValue()){
						
						canadianTnList = new ArrayList<String>();
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, "TN List belongs to canada"+ failedTnList);
						
						canadianTnList.addAll(failedTnList);
						result = true;
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, "By pass this rulle because SPID & Customer both  belongs to US&CN Result for isNpaNxxBelongToCNSPI : "
									+ result);
						
						try {
							CustomerContext.getInstance().set("canadianTnList",canadianTnList );
						} catch (FrameworkException e) {
							
							if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
								Debug.log(Debug.MSG_ERROR ," Error in seting canadianTnList in customer context "+ e);
						}
						
					}else{
					
						errorMessage = new StringBuffer();
						Iterator failedTnIterator = failedTnList.iterator();
					
						while (failedTnIterator.hasNext()) {
							failedTn = ((String) failedTnIterator.next());
							addFailedTN(failedTn);
							errorMessage.append("[" + failedTn + "] ");
						
						}

						setDynamicErrorMessage(errorMessage.toString());
					}
				}
			}
			Debug.log(Debug.MSG_STATUS, "Result for isNpaNxxBelongToCNSPI : "
					+ result);
		
			return result;

	}

	/**This method checks NpaNxx belongs to canada or not.
	 * This will be called at the time of Port-Out request submittion.
	 * 
	 * @return result, boolean
	 */
	public boolean isNpaNxxBelongsToCanadaSPI() {

		boolean result = false;

		ArrayList failedTNList = null;

		StringBuffer errorMessage = null;


		Boolean spidBelongsToUSandCNandSPIon = (Boolean) getContextValue("spidBelongsToUS_CNandSPIon");
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongsToCanadaSPI, The value of spidBelongsToUS_CNandSPIon : "
							+ spidBelongsToUSandCNandSPIon
									.booleanValue());
		}
		
		if(!spidBelongsToUSandCNandSPIon.booleanValue())
		{
			// Check if query has been already executed earlier and result 
			//is available in context.
			Boolean npanxxExist = (Boolean) getContextValue("CN_NpaNxxPresent");

			if (npanxxExist != null) {
				result = npanxxExist.booleanValue();

				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS, "isNpaNxxBelongsToCanadaSPI, CN npanxxList Empty:" + result);
				}
			}

			if (!result) {
				errorMessage = new StringBuffer();
				
				failedTNList = (ArrayList) getContextValue("failed_CN_NpaNxxList");

				String failedTn = null;
				if(failedTNList != null){
						for (int i = 0; i < failedTNList.size(); i++) {
						failedTn = (String) failedTNList.get(i);
						addFailedTN(failedTn);
						errorMessage.append("[" + failedTn + "] ");
					}
				}
				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		else
		{	
			List canadianTnList = (ArrayList) getContextValue("failed_CN_NpaNxxList");
			try {
				CustomerContext.getInstance().set("canadianTnList",canadianTnList );
			} catch (FrameworkException e) {
				
				if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
					Debug.log(Debug.MSG_ERROR ," Error in seting canadianTnList in customer context "+ e);
			}
			result = true;
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			if(spidBelongsToUSandCNandSPIon.booleanValue())
			{
				Debug.log(Debug.MSG_STATUS, "By pass this rule because SPID & Customer both  belongs to US&CN Result for isNpaNxxBelongsToCanadaSPI : "
						+ result);
			}
			else
			{
				Debug.log(Debug.MSG_STATUS,
						"isNpaNxxBelongsToCanadaSPI, CN  failedTNList: " + failedTNList);
				
				Debug.log(Debug.MSG_STATUS,
						"isNpaNxxBelongsToCanadaSPI from context: " + result);	
			}
			
		}

		return result;
	}

	/**This method checks whether NpaNxx belongs to Canada region or not.
	 * It will be called when submit SV modify request with TN.
	 * 
	 * @return boolean, result
	 */
	public boolean modifyNpaNxxBelongToCanadaSPI() {

		boolean result = false;

		boolean isCNNpaNxxListEmpty = false;

		Connection conn = null;

		Statement stmt = null;

		ResultSet results = null;

		TreeSet failedTnList = null;

		TreeSet npaNnxList = new TreeSet();
		
		TreeSet failedDashXList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;

		String dbNpaNxx = null;

		String dashX = null;

		ArrayList npaNnxDList = new ArrayList();

		ArrayList failedNpaNxxXList = null;

		StringBuffer errorMessage = null;

		String npanxx = null;

		StringBuffer napnxxQuery = new StringBuffer();

		String failedTn = null;

		StringBuffer npaNxxValue = new StringBuffer();
		
		List canadianTnList = null;

		Boolean spidBelongsToUSandCNandSPIon = (Boolean) getContextValue("spidBelongsToUS_CNandSPIon");
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"modifyNpaNxxBelongToCanadaSPI, The value of spidBelongsToUS_CNandSPIon : "
							+ spidBelongsToUSandCNandSPIon
									.booleanValue());
		}
		
	if (existsValue("npaNnxList")) {
		npaNnxList = (TreeSet) getContextValue("npaNnxList");
	}

			try {

				int i = 0;
				Iterator npanxxIterator = npaNnxList.iterator();

				while (npanxxIterator.hasNext()) {
					npaNxxValue.append("'");

					npaNxxValue.append((String) npanxxIterator.next());

					if (i != npaNnxList.size() - 1)
						npaNxxValue.append("',");
					else
						npaNxxValue.append("'");
					i++;
				}

				napnxxQuery.append(
						"SELECT NPA||NXX FROM SOA_NPANXX WHERE (NPA|| NXX) ")
						.append(" in ( ").append(npaNxxValue).append(
								" ) AND ").append(
								" REGIONID = ").append(SOAConstants.CANADA_REGION).append(" AND STATUS = 'ok'");

				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
					Debug.log(Debug.NORMAL_STATUS,
							"napnxxQuery in modifyNpaNxxBelongToCanadaSPI: "
									+ napnxxQuery.toString());
				}
				// Get Connection from RuleContext so that it is shared by other 
				//functions.
				conn = getDBConnection();

				stmt = conn.createStatement();

				results = stmt.executeQuery(napnxxQuery.toString());

				while (results.next()) {

					dbNpaNxx = results.getString(1);

					npaNnxDList.add(dbNpaNxx);

				}
				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS,
							"npaNnxDList in modifyNpaNxxBelongToCanadaSPI: "
									+ npaNnxDList);
				}
				if (existsValue("npaNxxXList")) {
					
					npaNxxXList = (TreeSet) getContextValue("npaNxxXList");
					
					if(npaNxxXList != null){

						Iterator tnIterator = npaNxxXList.iterator();

							while (tnIterator.hasNext()) {
							dashX = (String) tnIterator.next();
		
							npanxx = dashX.substring(0, dashX.length() - 1);
		
							if (npaNnxDList.contains(npanxx)) {
								failedDashXList.add(dashX);
							}
						}
					}
				}
				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS,
							"failedDashXList in modifyNpaNxxBelongToCanadaSPI: "
									+ failedDashXList);
				}
				isCNNpaNxxListEmpty = npaNnxDList.isEmpty();

				if (isCNNpaNxxListEmpty) {
					result = true;
				} else {
					
					failedTnList = new TreeSet();
					errorMessage = new StringBuffer();
					canadianTnList = new ArrayList<String>();
					
					
					npaNxxXDashx = (HashMap) getContextValue("npaNxxXDashx");

					String npaNxxX = null;

					Iterator failedDashXIterator = failedDashXList.iterator();
					while (failedDashXIterator.hasNext()) {
						npaNxxX = ((String) failedDashXIterator.next());
						if(npaNxxXDashx != null)
						{
							failedNpaNxxXList = (ArrayList) npaNxxXDashx.get(npaNxxX);
							if(failedNpaNxxXList!=null)
							{
								failedTnList.addAll(failedNpaNxxXList);
							}
						}
					}

					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug.log(Debug.MSG_STATUS,
								"failedTnList in modifyNpaNxxBelongToCanadaSPI: "
										+ failedTnList);
					}
					
					if(spidBelongsToUSandCNandSPIon.booleanValue()){
						
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, "TN List belongs to canada"+ failedTnList);
						
						canadianTnList.addAll(failedTnList);
						result = true;
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS, "By pass this rulle because SPID & Customer both  belongs to US&CN Result for isNpaNxxBelongToCNSPI : "
									+ result);
						
						try {
							CustomerContext.getInstance().set("canadianTnList",canadianTnList );
						} catch (FrameworkException e) {
							
							if ( Debug.isLevelEnabled( Debug.MSG_ERROR ))
								Debug.log(Debug.MSG_ERROR ," Error in seting canadianTnList in customer context "+ e);
						}
						
					}else{
						Iterator failedTnIterator = failedTnList.iterator();

						while (failedTnIterator.hasNext()) {
							failedTn = ((String) failedTnIterator.next());
							addFailedTN(failedTn);
							errorMessage.append("[" + failedTn + "] ");
						}

						setDynamicErrorMessage(errorMessage.toString());
					}
				}

			} catch (Exception exception) {

				logErrors(exception, napnxxQuery.toString());

			} finally {

				close(results, stmt);
			}
	
		

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			if(spidBelongsToUSandCNandSPIon.booleanValue())
			{
				Debug.log(Debug.MSG_STATUS, "By pass this rule because SPID & Customer both  belongs to US&CN Result for modifyNpaNxxBelongToCanadaSPI : "
						+ result);
			}
			else
			{
				Debug.log(Debug.MSG_STATUS,
					"Result for modifyNpaNxxBelongToCanadaSPI : " + result);
			}
		}
		return result;

	}

	/**
	 * This method is used to check NPA NXX belongs to Canada region or not.
	 * It will be called when submit SV modify request using SVID.
	 * 
	 * @return boolean, result
	 */

	public boolean isNpaNxxBelongsToCanadaSPISvid() {

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		Value tn = new Value(getContextValue("portingTn").toString());

		boolean result = false;
		

		Boolean spidBelongsToUSandCNandSPIon = (Boolean) getContextValue("spidBelongsToUS_CNandSPIon");
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongsToCanadaSPISvid, The value of spidBelongsToUS_CNandSPIon : "
							+ spidBelongsToUSandCNandSPIon
									.booleanValue());
		}
		
		if(!spidBelongsToUSandCNandSPIon.booleanValue())
		{
			try {

				conn = getDBConnection();

				if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
					Debug
							.log(
									Debug.NORMAL_STATUS,
									" SQL, isNpaNxxBelongsToCanadaSPISvid :\n"
											+ SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);
				}
				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug
							.log(
									Debug.NORMAL_STATUS,
									"isNpaNxxBelongsToCanadaSPISvid, PortingTn for Svid :\n"
											+ tn.toString());
				}
				pstmt = conn
						.prepareStatement(SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);

				pstmt.setString(1, tn.toString().substring(
						SOAConstants.TN_NPA_START_INDEX,
						SOAConstants.TN_NPA_END_INDEX));

				pstmt.setString(2, tn.toString().substring(
						SOAConstants.TN_NXX_START_INDEX,
						SOAConstants.TN_NXX_END_INDEX));

				results = pstmt.executeQuery();

				result = results.next();

				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS,
							"isNpaNxxBelongsToCanadaSPISvid, record present : " + result);
				}

			} catch (Exception exception) {

				logErrors(exception,
						SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);

			} finally {

				close(results, pstmt);
			}
		}
	
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongsToCanadaSPISvid, Result : " + !result);
		}
		return !result;
	}
	
	/**This method check either Release or T2 Expiration notification has been received to activate a SV.
	 * it also check whether NpaNxx of a TN belongs to canada region or not. If yes then no condition will be apply for that TN.
	 * @return boolean, result.
	 */
	public boolean isReleaseOrT2ExpirationNotificationReceived()
	{
		Date onspDueDate = null;
		String nnspduedate = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy-hhmmssa");
		Date currentDate = new Date();
		Date dueDate = null;
		Date date = null;
		String authFlag = null;
		boolean result = true;
		boolean isDueDateExpired = false;
		ArrayList svDataList = null;
		String portingTn = null;
		StringBuffer errorMessage = null;
		ResultsetData svData = null;
		String activateTimerExpiration = null;
		ArrayList failedTNList = new ArrayList();
		String failedTn = null;
		String onsp = null;
		String nnsp = null;
		long currentDateTime = 0L;
		long dueDateTime = 0L;
		Date objectCreationDate = null;
		TreeSet npaNxxSet = null;
		ArrayList npaNnxDList = null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet results = null;
		String dbNpaNxx = null;
		String npaNxx = null;
		String initSpid = null;
		
		if (existsValue("rsDataList"))
		{
			
			initSpid = getContextValue("xsdSpid").toString();
			Boolean spT2ExpirationFlag = (Boolean) getContextValue("spT2ExpirationFlag");

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"spT2ExpirationFlag: "
								+ spT2ExpirationFlag.booleanValue());
			}
			            
			if (existsValue("npaNxxSet")) {
				npaNxxSet = (TreeSet) getContextValue("npaNxxSet");
			}

			try {

				if(npaNxxSet!=null)
				{
					int ctr = 0;
					StringBuffer npaNxxValue = new StringBuffer();
					
					Iterator npanxxIterator = npaNxxSet.iterator();

					while (npanxxIterator.hasNext())
					{
						npaNxxValue.append("'");

						npaNxxValue.append((String) npanxxIterator.next());

						if (ctr != npaNxxSet.size() - 1)
						{
							npaNxxValue.append("',");
						}
						else
						{
							npaNxxValue.append("'");
						}
						ctr++;
					}
					
					StringBuffer napnxxQuery = new StringBuffer();
					
					napnxxQuery.append(
					"SELECT NPA||'-'||NXX FROM SOA_NPANXX WHERE (NPA||NXX) ")
					.append(" in ( ").append(npaNxxValue).append(
							" ) AND ").append(
							" REGIONID = ").append(SOAConstants.CANADA_REGION).append(" AND STATUS = 'ok'");

					if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
						Debug.log(Debug.NORMAL_STATUS,
								"isReleaseOrT2ExpirationNotificationReceived, Canada region napnxxQuery : "
										+ napnxxQuery.toString());
					}
					npaNnxDList = new ArrayList();
					// Get Connection from RuleContext so that it is shared by other 
					//functions.
					conn = getDBConnection();
		
					stmt = conn.createStatement();
		
					results = stmt.executeQuery(napnxxQuery.toString());
		
					while (results.next()) {
		
						dbNpaNxx = results.getString(1);
		
						npaNnxDList.add(dbNpaNxx);
		
					}
					if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
						Debug.log(Debug.MSG_STATUS,
								"isReleaseOrT2ExpirationNotificationReceived, npaNnxDList belongs to Canada region : "
										+ npaNnxDList);
					}
		
					svDataList = (ArrayList) getContextValue("rsDataList");
					
					if(svDataList != null){
					    	
						for (int i = 0; i < svDataList.size(); i++)
						{
							svData = (ResultsetData) svDataList.get(i);
			
							if (svData != null) {
								portingTn = svData.portingTn;
								onspDueDate = svData.onspDueDate;
								date = svData.nnspDueDate;
								activateTimerExpiration = svData.activateTimerExpirationFlag;
								onsp = svData.onsp;
								nnsp = svData.nnsp;
								objectCreationDate = svData.objectCreationDate;
							}
							//getting Npa-Nxx from Tn
							npaNxx = portingTn.toString().substring(SOAConstants.TN_NPA_START_INDEX, SOAConstants.TN_NXX_END_INDEX);
							//Checking Npa-Nxx belongs to Canada region npaNnxDList.
							//If not then check for duedate and notification.
							if(!npaNnxDList.contains(npaNxx))
							{
								if (date != null) {
									nnspduedate = dateFormat.format(date);
								}
				
								if (nnspduedate != null) {
									try {
										dueDate = dateFormat.parse(nnspduedate);
									} catch (ParseException ex) {
										isDueDateExpired = false;
									}
								}
								if (currentDate == null || dueDate == null
											|| objectCreationDate == null)
								{
									if (Debug.isLevelEnabled(Debug.RULE_EXECUTION))
									{
											Debug.log(Debug.RULE_EXECUTION,
													"Could not compare dates [" + currentDate
															+ "] " + "and [" + dueDate + "].");
									}
									isDueDateExpired = false;
								}
								else
								{
										currentDateTime = currentDate.getTime();
										dueDateTime = dueDate.getTime();
										isDueDateExpired = currentDateTime > dueDateTime;
								}
								
								// Check for NnspDueDate expiration.
								if(isDueDateExpired)
								{
									//If onspDueDate is null means, didn't received release notification.
									//then check for T2Expiration Notification
									// also checking for non-IntraPort records.
									if (onspDueDate == null && initSpid.equals(nnsp) && !onsp.equals(nnsp))
									{
										//if T2 concurrence Notification is not received then we will not populate activateTimerExpirationFlag in SV table..
										//If spT2ExpirationFlag is set to true for spid (NNSP). then check for activateTimerExpiration. 
										if(spT2ExpirationFlag.booleanValue())
										{
											if(activateTimerExpiration == null)
											{
												failedTNList.add(portingTn);
											}
										}
									}
								}
							}
						}
					}
			    }
		}catch (Exception e) {
				logErrors(e, " Exception occured in isReleaseOrT2ExpirationNotificationReceived method");

			} finally {

				close(results, stmt);
			}
			result = failedTNList.isEmpty();
		}

		if (!result)
		{
			errorMessage = new StringBuffer();
			
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"isReleaseOrT2ExpirationNotificationReceived, failedTnList : "
								+ failedTNList);
			}
			Iterator failedTnIterator = failedTNList.iterator();

			while (failedTnIterator.hasNext()) {
				failedTn = ((String) failedTnIterator.next());
				addFailedTN(failedTn);
				errorMessage.append("[" + failedTn + "] ");
			}
			setDynamicErrorMessage(errorMessage.toString());
		}

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for isReleaseOrT2ExpirationNotificationReceived : "
							+ result);
		}
		return result;
  }
	
	/**This method check either Release or T2 Expiration notification has been received to activate a SV.
	 * It also check whether NpaNxx belongs to CN region or not.
	 * 
	 * @return boolean, result.
	 */
	public boolean isReleaseOrT2ExpirationNotificationReceivedSvid()
	{
		boolean result = true;
		String activateTimerExpiration = null;
		Date onspDueDate = null;
		Boolean spT2ExpirationFlag = null;
		Boolean existSv = (Boolean) getContextValue("existSv");
		String onsp = null;
		String nnsp = null;
		boolean isNpaNxxNotBelongToCN = false;
		String initSpid = null;
		
		if (existSv.booleanValue()) 
		{
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"isReleaseOrT2ExpirationNotificationReceivedSvid, Checking NpaNxx belongs to CN region or not..");
			}
			//this method return "false" if NpaNxx belong to CN region.
			isNpaNxxNotBelongToCN = isNpaNxxBelongsToCanadaSvid();
			if(isNpaNxxNotBelongToCN)
			{
				onspDueDate =(Date)getContextValue("onspduedateSvId");
				activateTimerExpiration = (String) getContextValue("activateTimerExpirationSvId");
				spT2ExpirationFlag = (Boolean) getContextValue("spT2ExpirationFlag");
				onsp = (String) getContextValue("onspSvId");
				nnsp = (String) getContextValue("nnspSvId");
				initSpid = (String)getContextValue("spidSvId");
				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS,
							"isReleaseOrT2ExpirationNotificationReceivedSvid, spT2ExpirationFlag: "
									+ spT2ExpirationFlag.booleanValue());
					Debug.log(Debug.MSG_STATUS,
							"isReleaseOrT2ExpirationNotificationReceivedSvid, onspduedateSvId: "
									+ onspDueDate);
					Debug.log(Debug.MSG_STATUS,
							"isReleaseOrT2ExpirationNotificationReceivedSvid, activateTimerExpiration: "
									+ activateTimerExpiration);	
				}
				
				// If onspDueDate is null means, didn't received release notification.
				//then check for T2Expiration Notification
				// checking inter port condition also.
				// Check for expiration of NNSPDueDate is written in db rule xml(condition part).
				if (onspDueDate == null && initSpid.equals(nnsp) &&  !onsp.equals(nnsp))
				{
					//if T2 concurrence Notification is not received then we will not populate activateTimerExpirationFlag in SV table.
					//If spT2ExpirationFlag is set to true for spid (NNSP). then check for activateTimerExpirationFlag. 
					if(spT2ExpirationFlag.booleanValue())
					{
						if(activateTimerExpiration == null)
						{
							result = false;
						}
					}
				}
			}
		}
		
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for isReleaseOrT2ExpirationNotificationReceivedSvid : "
							+ result);
		}
		return result;
   }
	
	/** This method check if modify request for Intra port record and Simple Port indicator is present 
	 *  then throw BR.
	 * 
	 * @return boolean, result.
	 */
	public boolean isIntraPortWithSimplePortIndicator(){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String onsp = null;
		
		String initSpid = null;
		
		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;			

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			initSpid = getContextValue("xsdSpid").toString();
		
			if(svDataList!=null)
			{
				for (int i=0;i < svDataList.size();i++ )
				{
					ModifySvData svData =(ModifySvData)svDataList.get(i);

					portingTn = svData.portingTn;

					tnStatus = svData.status;

					nnsp = svData.nnsp;

					onsp = svData.onsp;
					
					// condition for intra port request.
					if (nnsp!=null && nnsp.equals(initSpid) && nnsp.equals(onsp))
					{
						addFailedTN(portingTn);
						errorMessage.append("[" + portingTn + "] ");
						result = false;
					}
				 }
			}
		}

		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());				
		}
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		{
			Debug.log(Debug.MSG_STATUS,"Result for isIntraPortWithSimplePortIndicator: "+result);
		}
		
		return result;
	}
	
	/**This method check if modify request for Intra port record and Simple Port indicator is present 
	 *  then throw BR.
	 * 
	 * @return boolean, result.
	 */
	public boolean isIntraPortWithSimplePortIndicatorSvId(){

		boolean result = true;

		String onsp = null;

		String nnsp = null;

		String status = null;		

		String initSpid = null;
		
		initSpid = getContextValue("spidSvId").toString();
		
		onsp =(String)getContextValue("onspSvId").toString();

		nnsp =(String)getContextValue("nnspSvId").toString();
		
		//condition for intraport request
		if (nnsp!=null && nnsp.equals(initSpid) && nnsp.equals(onsp))
		{
				result= false;
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		{
			Debug.log(Debug.MSG_STATUS,"Result for isIntraPortWithSimplePortIndicatorSvId: "+result);
		}
		
		return result ;
	}
	
	/** This method will check whether npanxx belong to Canada region or not.
	 *  This will be called at the time of Port-In request submittion.
	 * 
	 * @return boolean, result.
	 */
	public boolean isNpaNxxBelongToCN() {

		boolean result = true;

		String dashX = null;

		String failedTn = null;

		TreeSet failedDashXList = new TreeSet();

		TreeSet failedTnList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;

		ArrayList npanxxDbList = new ArrayList();

		ArrayList npanxxDataList = null;

		ArrayList failedIndivisualTnList = null;

		StringBuffer errorMessage = null;

		String region = null;

		if (existsValue("npanxxDataList")) {
			npanxxDataList = (ArrayList) getContextValue("npanxxDataList");
			
			if(npanxxDataList != null){
					for (int i = 0; i < npanxxDataList.size(); i++) {
					NpaNxxData nnData = (NpaNxxData) npanxxDataList.get(i);
					region = nnData.regionId;
	
					//adding all npanxx which belongs to canada region  (7).
					if (SOAConstants.CANADA_REGION.equals(region)) {
						npanxxDbList.add(nnData.npanxx);
					}
				}
			}
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, "isNpaNxxBelongToCN, List of NPANXx belongs to CN (7) region npanxxDbList:" + npanxxDbList);
		}
		
		if(!npanxxDbList.isEmpty())
		{
			if (existsValue("npaNxxXList")) {
				
				npaNxxXList = (TreeSet) getContextValue("npaNxxXList");
				if(npaNxxXList != null){
					String npanxx = null;

					Iterator npaNxxXIterator = npaNxxXList.iterator();

						while (npaNxxXIterator.hasNext()) {
						dashX = (String) npaNxxXIterator.next();
		
						npanxx = dashX.substring(0, dashX.length() - 1);
							// npanxx belong to region 7 (canada) will be available in npanxxDbList.
						if (npanxxDbList.contains(npanxx)) {
									failedDashXList.add(dashX);
						}
					
					}
				}
			}

			result = failedDashXList.isEmpty();

			if (!result) {

				npaNxxXDashx = (HashMap) getContextValue("npaNxxXDashx");

				String dash_X = null;

				Iterator failedDashXIterator = failedDashXList.iterator();
				
				if(npaNxxXDashx != null){
					//Getting tn from failed dash x list.
						while (failedDashXIterator.hasNext()) {
						dash_X = ((String) failedDashXIterator.next());
		
						failedIndivisualTnList = (ArrayList) npaNxxXDashx.get(dash_X);
		
						failedTnList.addAll(failedIndivisualTnList);
		
					}
				}
				errorMessage = new StringBuffer();

				Iterator failedTnIterator = failedTnList.iterator();

				while (failedTnIterator.hasNext()) {
					failedTn = ((String) failedTnIterator.next());
					addFailedTN(failedTn);
					errorMessage.append("[" + failedTn + "] ");
				}

				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, "Result for isNpaNxxBelongToCN : "
					+ result);
		}

		return result;

	}
	
	/**This method checks whether NpaNxx belongs to Canada region or not.
	 * It will be called when submit SV modify request with TN.
	 * 
	 * @return boolean, result
	 */
	public boolean modifyNpaNxxBelongToCanada() {

		boolean result = false;

		boolean isCNNpaNxxListEmpty = false;

		Connection conn = null;

		Statement stmt = null;

		ResultSet results = null;

		TreeSet failedTnList = null;

		TreeSet npaNnxList = new TreeSet();
		
		TreeSet failedDashXList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;

		String dbNpaNxx = null;

		String dashX = null;

		ArrayList npaNnxDList = new ArrayList();

		ArrayList failedNpaNxxXList = null;

		StringBuffer errorMessage = null;

		String npanxx = null;

		StringBuffer napnxxQuery = new StringBuffer();

		String failedTn = null;

		StringBuffer npaNxxValue = new StringBuffer();

		if (existsValue("npaNnxList")) {
			npaNnxList = (TreeSet) getContextValue("npaNnxList");
		}

		try {

			int i = 0;
			Iterator npanxxIterator = npaNnxList.iterator();

			while (npanxxIterator.hasNext()) {
				npaNxxValue.append("'");

				npaNxxValue.append((String) npanxxIterator.next());

				if (i != npaNnxList.size() - 1)
					npaNxxValue.append("',");
				else
					npaNxxValue.append("'");
				i++;
			}

			napnxxQuery.append(
					"SELECT NPA||NXX FROM SOA_NPANXX WHERE (NPA|| NXX) ")
					.append(" in ( ").append(npaNxxValue).append(
							" ) AND ").append(
							" REGIONID = ").append(SOAConstants.CANADA_REGION).append(" AND STATUS = 'ok'");

			if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
				Debug.log(Debug.NORMAL_STATUS,
						"napnxxQuery in modifyNpaNxxBelongToCanada: "
								+ napnxxQuery.toString());
			}
			// Get Connection from RuleContext so that it is shared by other 
			//functions.
			conn = getDBConnection();

			stmt = conn.createStatement();

			results = stmt.executeQuery(napnxxQuery.toString());

			while (results.next()) {

				dbNpaNxx = results.getString(1);

				npaNnxDList.add(dbNpaNxx);

			}
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"npaNnxDList in modifyNpaNxxBelongToCanada: "
								+ npaNnxDList);
			}
			if (existsValue("npaNxxXList")) {
				
				npaNxxXList = (TreeSet) getContextValue("npaNxxXList");
				
				if(npaNxxXList != null){

					Iterator tnIterator = npaNxxXList.iterator();

						while (tnIterator.hasNext()) {
						dashX = (String) tnIterator.next();
	
						npanxx = dashX.substring(0, dashX.length() - 1);
	
						if (npaNnxDList.contains(npanxx)) {
							failedDashXList.add(dashX);
						}
					}
				}
			}
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"failedDashXList in modifyNpaNxxBelongToCanada: "
								+ failedDashXList);
			}
			isCNNpaNxxListEmpty = npaNnxDList.isEmpty();

			if (isCNNpaNxxListEmpty) {
				result = true;
			} else {
				
				failedTnList = new TreeSet();
				errorMessage = new StringBuffer();
				
				npaNxxXDashx = (HashMap) getContextValue("npaNxxXDashx");

				String npaNxxX = null;

				Iterator failedDashXIterator = failedDashXList.iterator();
				while (failedDashXIterator.hasNext()) {
					npaNxxX = ((String) failedDashXIterator.next());
					if(npaNxxXDashx != null)
					{
						failedNpaNxxXList = (ArrayList) npaNxxXDashx.get(npaNxxX);
						if(failedNpaNxxXList!=null)
						{
							failedTnList.addAll(failedNpaNxxXList);
						}
					}
				}

				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS,
							"failedTnList in modifyNpaNxxBelongToCanada: "
									+ failedTnList);
				}
				Iterator failedTnIterator = failedTnList.iterator();

				while (failedTnIterator.hasNext()) {
					failedTn = ((String) failedTnIterator.next());
					addFailedTN(failedTn);
					errorMessage.append("[" + failedTn + "] ");
				}

				setDynamicErrorMessage(errorMessage.toString());
			}

		} catch (Exception exception) {

			logErrors(exception, napnxxQuery.toString());

		} finally {

			close(results, stmt);
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for modifyNpaNxxBelongToCanada : " + result);
		}
		return result;

	}
	
	/**
	 * This method is used to check NPA NXX belongs to Canada region or not.
	 * It will be called when submit SV modify request using SVID.
	 * 
	 * @return boolean, result
	 */

	public boolean isNpaNxxBelongsToCanadaSvid() {

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		Value tn = new Value(getContextValue("portingTn").toString());

		boolean result = false;

		try {

			conn = getDBConnection();

			if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
				Debug
						.log(
								Debug.NORMAL_STATUS,
								" SQL, isNpaNxxBelongsToCanadaSvid :\n"
										+ SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);
			}
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug
						.log(
								Debug.NORMAL_STATUS,
								"isNpaNxxBelongsToCanadaSvid, PortingTn for Svid :\n"
										+ tn.toString());
			}
			pstmt = conn
					.prepareStatement(SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);

			pstmt.setString(1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX));

			pstmt.setString(2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX));

			results = pstmt.executeQuery();

			result = results.next();

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"isNpaNxxBelongsToCanadaSvid, record present : " + result);
			}

		} catch (Exception exception) {

			logErrors(exception,
					SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY);

		} finally {

			close(results, pstmt);
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongsToCanadaSvid, Result : " + !result);
		}
		return !result;
	}
	
	/**
	 * This methos checks whethere Pseudo LRN is absent in the request XML or not
	 * @param lrn
	 * @return boolean result
	 */
	public boolean pseudoLrnAbsent(Value lrn)
	{
		boolean result = true;

		if(lrn != null){			
			if(lrn.toString().indexOf("0000000000") != -1){
				result = false;
			}
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"Result for pseudoLrnAbsent: "+result);
		}
		return result;
	}
	/**
	 * This method checks whethere Pseudo LRN is absent in the svid regionid combination request XML or not
	 */
	public boolean pseudoLrnAbsentSvRegion(Value lrn){
		
		boolean result = true;
		String lrnVal = null;				
		if((lrn != null) && (lrn.length() > 0)){
			
			lrnVal = lrn.toString();
			
			if("0000000000".indexOf(lrnVal) != -1){
				result = false;
			}
		}
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for pseudoLrnAbsentSvRegion: "+result);
		
		return result;
		
	}
	/**
	 * Check the absence of Pseudo LRN value for SV create
	 */
	public boolean pseudoLrnAbsentSV(String requestType, Value lrn){
		
		boolean result = true;
		
		boolean resultField = false;
		
		ArrayList tnList = null;
		StringBuffer errorMessage = new StringBuffer();
		String tn = null;
		
		if(existsValue("tnList")){
			tnList = (ArrayList) getContextValue("tnList");
		}
		
		if (requestType.equals("SvCreateRequest"))
		{
			resultField = present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/Lrn");
		}

		if (requestType.equals("SvModifyRequest"))
		{
			resultField = present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/Lrn");
		}
		
		if(resultField){
			if(lrn.toString().indexOf("0000000000") != -1){
				result = false;
			}
		}
		
		if(!result){
			
			for(int i=0; i < tnList.size(); i++){
				tn = (String) tnList.get(i);
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for pseudoLrnAbsentSV: "+tn);
				
				addFailedTN(tn);
				errorMessage.append("[" + tn + "] ");
			}
			
			setDynamicErrorMessage(errorMessage.toString());
		}
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
			Debug.log(Debug.MSG_STATUS,"Result for pseudoLrnAbsentSV: "+result);
		
		return result;		
	}
	/** This method check if modify request for Intra port record and Pseudo Lrn is present
	 *  then throw BR.
	 * 
	 * @return boolean, result.
	 */
	public boolean IsPortInWithPseudoLrn(){

		boolean result = true;	

		String portingTn = null;

		String tnStatus = null;

		String nnsp = null;

		String onsp = null;
		
		String initSpid = null;
		
		StringBuffer errorMessage = new StringBuffer();

		ArrayList svDataList = null;			

		if (existsValue("modifySvRs"))
		{
			svDataList =(ArrayList)getContextValue("modifySvRs");
			initSpid = (String)getContextValue("xsdSpid").toString();
		
			if(svDataList!=null)
			{
				for (int i=0;i < svDataList.size();i++ )
				{
					ModifySvData svData =(ModifySvData)svDataList.get(i);

					portingTn = svData.portingTn;

					tnStatus = svData.status;

					nnsp = svData.nnsp;

					onsp = svData.onsp;
					
					// condition for intra port request.
					if (nnsp!=null && nnsp.equals(initSpid) && !nnsp.equals(onsp))
					{
						addFailedTN(portingTn);
						errorMessage.append("[" + portingTn + "] ");
						result = false;
					}
				 }
			}
		}

		if (!result)
		{
			setDynamicErrorMessage(errorMessage.toString());				
		}
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		{
			Debug.log(Debug.MSG_STATUS,"Result for IsPortInWithPseudoLrn: "+result);
		}
		
		return result;
	}
	
	/**This method check if modify request for portIn record and Pseudo LRN is present 
	 *  then throw BR.
	 * 
	 * @return boolean, result.
	 */
	public boolean IsPortInWithPseudoLrnSvid(){

		boolean result = true;

		String onsp = null;

		String nnsp = null;

		String status = null;		

		String initSpid = null;
		
		initSpid = (String)getContextValue("spidSvId").toString();
		
		onsp =(String)getContextValue("onspSvId").toString();

		nnsp =(String)getContextValue("nnspSvId").toString();
		
		//condition for intraport request
		if (nnsp!=null && nnsp.equals(initSpid) && !nnsp.equals(onsp))
		{
				result= false;
		}

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
		{
			Debug.log(Debug.MSG_STATUS,"Result for IsPortInWithPseudoLrnSvid: "+result);
		}
		
		return result ;
	}
	/**
	 * Check if Intra Port with Pseudo Lrn is allowed on top of active Intra Port
	 * @param status Value
	 * @return boolean for current status of the SV.
	 */
	public boolean isPseudoLrnAllowedIntraPort(Value nnsp, Value onsp){
		
		boolean result = true;		
		StringBuffer errorMessage = new StringBuffer();
		ArrayList tnList = null; 
		
		if(existsValue("tnList")){
			tnList = (ArrayList) getContextValue("tnList");
		}
		
		if( !(nnsp.toString().equals(onsp.toString())) ){
			
			for (int i = 0; i < tnList.size(); i++) {
				
				String tnVal = (String)tnList.get(i);
					
					addFailedTN(tnVal.toString());
					
					errorMessage.append("["+tnVal.toString()+"] ");
					
					result = false;
			}
		}
		
		if (!result)
			setDynamicErrorMessage(errorMessage.toString());
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Result for isPseudoLrnAllowedIntraPort: " + result);
			
		return result;
	}
	
	public boolean isPseudoLrnAllowedWithIntraPortAndPto(Value spid) {
		
		boolean result = true;
		StringBuffer errorMessage = new StringBuffer();
		
		ArrayList svDataList = null;
		
		String lnpType = null;		
		int pto;		
		String lrn = null;		
		String status = null;
		String portingTn = null;
		String initSpid = spid.toString();
		
		
		if(existsValue("createSvRs")){
			svDataList = (ArrayList)getContextValue("createSvRs");
			
			if(svDataList != null){
				
				for(int i=0; i<svDataList.size(); i++){
					
					CreateSvData svData = (CreateSvData)svDataList.get(i);
					
					lnpType = svData.lnptype;
					
					pto = svData.pto;
					
					lrn = svData.lrn;
					
					portingTn = svData.portingTn;
					
					status = svData.status;
					
					if(status.equals("active") || status.equals("NPACCreateFailure")){
						
						if(lnpType != null && lnpType.equalsIgnoreCase("lisp")){
							if(status.equals("active")){
								if(lrn != null && lrn.indexOf("0000000000") == -1){
									addFailedTN(portingTn.toString());
									errorMessage.append("["+portingTn.toString()+"] ");
									result = false;	
								}
							}
							
							if(status.equals("NPACCreateFailure")){
								
								if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
									Debug.log(Debug.MAPPING_STATUS, "Latest record have status NPACCreateFailure so fetching " +
											"the latest active record from DB.");
								}
								
								StringBuffer tnQuery = new StringBuffer(
										"(select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ LRN, LNPTYPE, PORTINGTOORIGINAL from " +
										"soa_subscription_version where (portingtn, spid, createddate)" +
										" in (select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ portingtn, spid, max(createddate) from " +
										"soa_subscription_version where portingtn in ('"+portingTn.toString()+"' ) " +
										"AND spid = '"+initSpid+"' and STATUS in ( 'active')" +
										" group by portingtn, spid) and STATUS in ( 'active'))");
								
								if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
									Debug.log(Debug.MAPPING_STATUS, "TN Query to fetch the latest active records:"+tnQuery);
								}
								Connection conn = null;
								Statement stmt = null;
							    ResultSet results = null;
							    String lrnVal = null;
							    String lnpTypeVal = null;
							    int ptoVal = 0;
							    
							    try{
							    	
							   	 /*Get Connection from RuleContext so that it is shared 
									 by other functions.*/
									 conn = getDBConnection();
									 stmt = conn.createStatement();		 
									 results = stmt.executeQuery(tnQuery.toString());
									 
									 if(results.next()){
									  
										 lrnVal = results.getString(1);      
										 lnpTypeVal = results.getString(2);
										 ptoVal = results.getInt(3);
									 }
									 
									if(lnpTypeVal != null && lnpTypeVal.equalsIgnoreCase("lspp")){
										if(Debug.isLevelEnabled(Debug.MSG_STATUS))
											Debug.log(Debug.MSG_STATUS, "Latest active record have lnptype=lspp");
										
										if(ptoVal != 1){
											addFailedTN(portingTn.toString());
											errorMessage.append("["+portingTn.toString()+"] ");
											result = false;
										}
									}else 
									 if(lnpTypeVal != null && lnpTypeVal.equalsIgnoreCase("lisp")){
										 if(Debug.isLevelEnabled(Debug.MSG_STATUS))
											 Debug.log(Debug.MSG_STATUS, "Latest active record have lnptype=lisp");
										if(lrnVal != null && lrnVal.indexOf("0000000000") == -1){
											addFailedTN(portingTn.toString());
											errorMessage.append("["+portingTn.toString()+"] ");
											result = false;	
										}
									}
									 
							    }catch(Exception exception){

							    	  if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
							    		  Debug.log(Debug.ALL_ERRORS,exception.toString());
							         
							    }finally{
									
							    	close(results, stmt);
							    }
								
							}
						}else if(lnpType != null && lnpType.equalsIgnoreCase("lspp")){
							
							if(pto != 1){
								addFailedTN(portingTn.toString());
								errorMessage.append("["+portingTn.toString()+"] ");
								result = false;
							}
						}
					}										
				}
			}			
		}
		
		if(!result)
			setDynamicErrorMessage(errorMessage.toString());
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Result for isPseudoLrnAllowedWithIntraPortAndPto: " + result);
		
		return result;
	}
	
	
	public boolean isNPBExistsWithActiveLRN() {
		
		boolean result = true;
		StringBuffer errorMessage = new StringBuffer();
		TreeSet npanxxXDataList = new TreeSet();
		ArrayList tnList = null;
		String dashX = null;
		StringBuffer dashXVal = new StringBuffer();
		ArrayList npbDbList = new ArrayList();
		ArrayList failedNPBList = new ArrayList();
		NPBData npbRs = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		if (existsValue("npaNxxXList"))
		{
			npanxxXDataList = (TreeSet)getContextValue("npaNxxXList");
			Iterator npaNxxXItr = npanxxXDataList.iterator();
			int j=0;
			while(npaNxxXItr.hasNext())
			{				
				dashXVal.append("'");
				dashXVal.append((String)npaNxxXItr.next());				
				if ( j != npanxxXDataList.size()-1)				
					dashXVal.append("',");
				else
					dashXVal.append("'");
					j++;
			}
			String NPbExistsQuery = "SELECT NPA, NXX, DashX , status , lrn, spid FROM " +
					" SOA_NBRPOOL_BLOCK WHERE (NPA||NXX||DASHX) IN ("+dashXVal.toString()+")" + 
			        " AND STATUS IN ('active','download-failed','sending','download-failed-partial')";
				
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				Debug.log(Debug.MSG_STATUS,"Query to fetche the NPB Data["+NPbExistsQuery+"]");
			}
			try{
				conn = getDBConnection();
				pstmt = conn.prepareStatement(NPbExistsQuery);
				rs = pstmt.executeQuery();
				while(rs.next()){
					String npa = rs.getString(1);
					String nxx = rs.getString(2);
					String dashx = rs.getString(3);
					String status = rs.getString(4);
					String lrn = rs.getString(5);
					String spid = rs.getString(6);
					npbRs = new NPBData(npa,nxx,dashx,status,lrn,spid);
					npbDbList.add(npbRs);
				}
			}catch(Exception exp){
				logErrors(exp, NPbExistsQuery);
			}finally{
				close(rs, pstmt);
			}
		}
		if(npbDbList != null){
					
			for (int k = 0; k < npbDbList.size(); k++) 
			{
				NPBData npbData = (NPBData)npbDbList.get(k);
				
				String dbLrn = npbData.lrn;
				String dbStatus = npbData.staus;
				
				if( dbLrn != null && dbLrn.indexOf("0000000000")== -1 )
				{
					failedNPBList.add(npbData.npa+npbData.nxx+npbData.dashX);
				}
			}
		}
				
		result = failedNPBList.isEmpty();
				
		if(!result){
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "failedNPBList for Sv create with TN contains NPANXXX.");
			
			if (existsValue("tnList"))
				tnList =(ArrayList)getContextValue("tnList");
			
			if(tnList!=null){
				for (int i = 0; i < tnList.size(); i++) {
					
					String tnVal = (String)tnList.get(i);
					
					String npa = tnVal.toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX);
					String nxx = tnVal.toString().substring(SOAConstants.TN_NXX_START_INDEX, SOAConstants.TN_NXX_END_INDEX);
					String dashx = tnVal.toString().substring(SOAConstants.TN_DASHX_START_INDEX, SOAConstants.TN_DASHX_END_INDEX);
					
					String dashxVal = npa+nxx+dashx;
					
					if(failedNPBList.contains(dashxVal))
					{			
						addFailedTN(tnVal.toString());			
						errorMessage.append("["+tnVal.toString()+"]");
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,"failedTn npaxxx"+tnVal.toString());
					}
				}
				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		
		   if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "isNPBExistsWithActiveLRN value is: " + result);
		return result;
	}
	
public boolean isNPBExistsWithPseudoLRN() {
		
		boolean result = true;
		StringBuffer errorMessage = new StringBuffer();
		TreeSet npanxxXDataList = new TreeSet();
		ArrayList tnList = null;
		String dashX = null;
		StringBuffer dashXVal = new StringBuffer();
		ArrayList npbDbList = new ArrayList();
		ArrayList failedNPBList = new ArrayList();
		NPBData npbRs = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		if (existsValue("npaNxxXList"))
		{
			npanxxXDataList = (TreeSet)getContextValue("npaNxxXList");
			Iterator npaNxxXItr = npanxxXDataList.iterator();
			int j=0;
			while(npaNxxXItr.hasNext())
			{				
				dashXVal.append("'");
				dashXVal.append((String)npaNxxXItr.next());				
				if ( j != npanxxXDataList.size()-1)				
					dashXVal.append("',");
				else
					dashXVal.append("'");
					j++;
			}
			String NPbExistsQuery = "SELECT NPA, NXX, DashX , status , lrn, spid FROM " +
					" SOA_NBRPOOL_BLOCK WHERE (NPA||NXX||DASHX) IN ("+dashXVal.toString()+")" + 
			        " AND STATUS IN ('active','download-failed','sending','download-failed-partial')";
				
			if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
				Debug.log(Debug.MSG_STATUS,"Query to fetche the NPB Data["+NPbExistsQuery+"]");
			}
			try{
				conn = getDBConnection();
				pstmt = conn.prepareStatement(NPbExistsQuery);
				rs = pstmt.executeQuery();
				while(rs.next()){
					String npa = rs.getString(1);
					String nxx = rs.getString(2);
					String dashx = rs.getString(3);
					String status = rs.getString(4);
					String lrn = rs.getString(5);
					String spid = rs.getString(6);
					npbRs = new NPBData(npa,nxx,dashx,status,lrn,spid);
					npbDbList.add(npbRs);
				}
			}catch(Exception exp){
				logErrors(exp, NPbExistsQuery);
			}finally{
				close(rs, pstmt);
			}
		}
		if(npbDbList != null){
					
			for (int k = 0; k < npbDbList.size(); k++) 
			{
				NPBData npbData = (NPBData)npbDbList.get(k);
				
				String dbLrn = npbData.lrn;
				String dbStatus = npbData.staus;
				
				if( dbLrn != null && dbLrn.equals("0000000000"))
				{
					failedNPBList.add(npbData.npa+npbData.nxx+npbData.dashX);
				}
			}
		}
				
		result = failedNPBList.isEmpty();
				
		if(!result){
			
			if(Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "failedNPBList for Sv create with TN contains NPANXXX.");
			
			if (existsValue("tnList"))
				tnList =(ArrayList)getContextValue("tnList");
			
			if(tnList!=null){
				for (int i = 0; i < tnList.size(); i++) {
					
					String tnVal = (String)tnList.get(i);
					
					String npa = tnVal.toString().substring(SOAConstants.TN_NPA_START_INDEX,SOAConstants.TN_NPA_END_INDEX);
					String nxx = tnVal.toString().substring(SOAConstants.TN_NXX_START_INDEX, SOAConstants.TN_NXX_END_INDEX);
					String dashx = tnVal.toString().substring(SOAConstants.TN_DASHX_START_INDEX, SOAConstants.TN_DASHX_END_INDEX);
					
					String dashxVal = npa+nxx+dashx;
					
					if(failedNPBList.contains(dashxVal))
					{			
						addFailedTN(tnVal.toString());			
						errorMessage.append("["+tnVal.toString()+"]");
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
							Debug.log(Debug.MSG_STATUS,"failedTn npaxxx"+tnVal.toString());
					}
				}
				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		
		   if (Debug.isLevelEnabled(Debug.MSG_STATUS))
				Debug.log(Debug.MSG_STATUS, "isNPBExistsWithPseudoLRN value is: " + result);
		return result;
	}
	
	/**
	 * Validate the TN modify request with pseudoLrn
	 */	
	public boolean isPseudoLrnAllowedWithModify(Value spid){
		
		boolean result = true;
		
		StringBuffer errorMessage = new StringBuffer();
		String lrn = null;
		String lnptype = null;
		ArrayList svData = null;
		String portingTn = null;
		
		if(existsValue("modifySvRs")){
			svData = (ArrayList) getContextValue("modifySvRs");
			
			if(svData != null){
				for(int i=0; i<svData.size(); i++){
					ModifySvData data = (ModifySvData) svData.get(i);
					lnptype = data.lnptype;
					lrn = data.lrn;
					portingTn = data.portingTn;
					
					if (lnptype != null && lnptype.equals("lisp")){
						if(lrn != null && lrn.indexOf("0000000000") == -1){
							addFailedTN(portingTn.toString());
							errorMessage.append("["+portingTn.toString()+"] ");
							result = false;
						}
					}
				}
			}
		}
		if(!result)
			setDynamicErrorMessage(errorMessage.toString());
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Result for isPseudoLrnAllowedWithModify: " + result);
		
		return result;
	}
	
	/**
	 * Validate the TN modify request with Active LRN
	 */	
	public boolean isActiveLrnAllowedWithModify(Value spid){
		
		boolean result = true;
		
		StringBuffer errorMessage = new StringBuffer();
		String lrn = null;
		String lnptype = null;
		String portingTn = null;
		ArrayList svData = null;
		
		if(existsValue("modifySvRs")){
			svData = (ArrayList) getContextValue("modifySvRs");
			
			if(svData != null){
				for(int i=0; i<svData.size(); i++){
					ModifySvData data = (ModifySvData) svData.get(i);
					lnptype = data.lnptype;
					lrn = data.lrn;
					portingTn = data.portingTn;
					
					if (lnptype != null && lnptype.equals("lisp")){
						if(lrn != null && "0000000000".equals(lrn)){
							addFailedTN(portingTn.toString());
							errorMessage.append("["+portingTn.toString()+" ] ");
							result = false;
						}
					}
				}
			}
		}
		if(!result)
			setDynamicErrorMessage(errorMessage.toString());
		
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Result for isActiveLrnAllowedWithModify: " + result);
		
		return result;
	}
	
	/**
	 * Validate the SVID modify request with pseudoLrn
	 */
	public boolean isPseudoLrnAllowedWithModifySvId(){
		
		boolean result = true;
		
		String lrn = null;
		String lnptype = null;
		
		if(existsValue("lrnSvid"))
			lrn = (String)getContextValue("lrnSvid").toString();
		
		if(existsValue("lnptypeSvid"))
			lnptype = (String)getContextValue("lnptypeSvid").toString();
		
		if (lnptype != null && lnptype.equals("lisp")){
			if(lrn != null && "0000000000".indexOf(lrn) == -1){							
				result = false;
			}
		}
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Result for isPseudoLrnAllowedWithModifySvId: " + result);
		
		return result;
	}
	
	/**
	 * Validate the SVID modify request with Active LRN
	 */	
	public boolean isActiveLrnAllowedWithModifySvId(){
		
		boolean result = true;
		
		String lrn = null;
		String lnptype = null;
		if(existsValue("lrnSvid"))
			lrn = (String)getContextValue("lrnSvid").toString();
		
		if(existsValue("lnptypeSvid"))
			lnptype = (String)getContextValue("lnptypeSvid").toString();
		
		if (lnptype != null && lnptype.equals("lisp")){
			
			if(lrn != null && "0000000000".equals(lrn)){
				result = false;
			}
		}
		if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			Debug.log(Debug.MSG_STATUS, "Result for isActiveLrnAllowedWithModifySvId: " + result);
		
		return result;
	}
	
	/** This method will check whether npanxx belong to US region or not.
	 *  This will be called at the time of Port-In request submittion.
	 * 
	 * @return boolean, result.
	 */
	public boolean isNpaNxxBelongToUS() {

		boolean result = true;

		String dashX = null;

		String failedTn = null;

		TreeSet failedDashXList = new TreeSet();

		TreeSet failedTnList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;

		ArrayList npanxxDbList = new ArrayList();

		ArrayList npanxxDataList = null;

		ArrayList failedIndivisualTnList = null;

		StringBuffer errorMessage = null;

		String region = null;

		if (existsValue("npanxxDataList")) {
			npanxxDataList = (ArrayList) getContextValue("npanxxDataList");
			
			if(npanxxDataList != null){
					for (int i = 0; i < npanxxDataList.size(); i++) {
					NpaNxxData nnData = (NpaNxxData) npanxxDataList.get(i);
					region = nnData.regionId;
	
					//adding all npanxx which belongs to canada region  (7).
					if (!(SOAConstants.CANADA_REGION.equals(region))) {
						npanxxDbList.add(nnData.npanxx);
					}
				}
			}
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, "isNpaNxxBelongToCN, List of NPANXx belongs to CN (7) region npanxxDbList:" + npanxxDbList);
		}
		
		if(!npanxxDbList.isEmpty())
		{
			if (existsValue("npaNxxXList")) {
				
				npaNxxXList = (TreeSet) getContextValue("npaNxxXList");
				if(npaNxxXList != null){
					String npanxx = null;

					Iterator npaNxxXIterator = npaNxxXList.iterator();

						while (npaNxxXIterator.hasNext()) {
						dashX = (String) npaNxxXIterator.next();
		
						npanxx = dashX.substring(0, dashX.length() - 1);
							// npanxx belong to region 0 to 6 (US) will be available in npanxxDbList.
						if (npanxxDbList.contains(npanxx)) {
									failedDashXList.add(dashX);
						}
					
					}
				}
			}

			result = failedDashXList.isEmpty();

			if (!result) {

				npaNxxXDashx = (HashMap) getContextValue("npaNxxXDashx");

				String dash_X = null;

				Iterator failedDashXIterator = failedDashXList.iterator();
				
				if(npaNxxXDashx != null){
					//Getting tn from failed dash x list.
						while (failedDashXIterator.hasNext()) {
						dash_X = ((String) failedDashXIterator.next());
		
						failedIndivisualTnList = (ArrayList) npaNxxXDashx.get(dash_X);
		
						failedTnList.addAll(failedIndivisualTnList);
		
					}
				}
				errorMessage = new StringBuffer();

				Iterator failedTnIterator = failedTnList.iterator();

				while (failedTnIterator.hasNext()) {
					failedTn = ((String) failedTnIterator.next());
					addFailedTN(failedTn);
					errorMessage.append("[" + failedTn + "] ");
				}

				setDynamicErrorMessage(errorMessage.toString());
			}
		}
		

		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS, "Result for isNpaNxxBelongToUS : "
					+ result);
		}

		return result;

	}
	
	/**This method checks whether NpaNxx belongs to US region or not.
	 * It will be called when submit SV modify request with TN.
	 * 
	 * @return boolean, result
	 */
	public boolean modifyNpaNxxBelongToUS() {

		boolean result = false;

		boolean isUSNpaNxxListEmpty = false;

		Connection conn = null;

		Statement stmt = null;

		ResultSet results = null;

		TreeSet failedTnList = null;

		TreeSet npaNnxList = new TreeSet();
		
		TreeSet failedDashXList = new TreeSet();

		TreeSet npaNxxXList = null;

		HashMap npaNxxXDashx = null;

		String dbNpaNxx = null;

		String dashX = null;

		ArrayList npaNnxDList = new ArrayList();

		ArrayList failedNpaNxxXList = null;

		StringBuffer errorMessage = null;

		String npanxx = null;

		StringBuffer napnxxQuery = new StringBuffer();

		String failedTn = null;

		StringBuffer npaNxxValue = new StringBuffer();

		if (existsValue("npaNnxList")) {
			npaNnxList = (TreeSet) getContextValue("npaNnxList");
		}

		try {

			int i = 0;
			Iterator npanxxIterator = npaNnxList.iterator();

			while (npanxxIterator.hasNext()) {
				npaNxxValue.append("'");

				npaNxxValue.append((String) npanxxIterator.next());

				if (i != npaNnxList.size() - 1)
					npaNxxValue.append("',");
				else
					npaNxxValue.append("'");
				i++;
			}

			napnxxQuery.append(
					"SELECT NPA||NXX FROM SOA_NPANXX WHERE (NPA|| NXX) ")
					.append(" in ( ").append(npaNxxValue).append(
							" ) AND ").append(
							" REGIONID IN (0,1,2,3,4,5,6) AND STATUS = 'ok'");

			if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
				Debug.log(Debug.NORMAL_STATUS,
						"napnxxQuery in modifyNpaNxxBelongToUs: "
								+ napnxxQuery.toString());
			}
			// Get Connection from RuleContext so that it is shared by other 
			//functions.
			conn = getDBConnection();

			stmt = conn.createStatement();

			results = stmt.executeQuery(napnxxQuery.toString());

			while (results.next()) {

				dbNpaNxx = results.getString(1);

				npaNnxDList.add(dbNpaNxx);

			}
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"npaNnxDList in modifyNpaNxxBelongToUS: "
								+ npaNnxDList);
			}
			if (existsValue("npaNxxXList")) {
				
				npaNxxXList = (TreeSet) getContextValue("npaNxxXList");
				
				if(npaNxxXList != null){

					Iterator tnIterator = npaNxxXList.iterator();

						while (tnIterator.hasNext()) {
						dashX = (String) tnIterator.next();
	
						npanxx = dashX.substring(0, dashX.length() - 1);
	
						if (npaNnxDList.contains(npanxx)) {
							failedDashXList.add(dashX);
						}
					}
				}
			}
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"failedDashXList in modifyNpaNxxBelongToUS: "
								+ failedDashXList);
			}
			isUSNpaNxxListEmpty = npaNnxDList.isEmpty();

			if (isUSNpaNxxListEmpty) {
				result = true;
			} else {
				
				failedTnList = new TreeSet();
				errorMessage = new StringBuffer();
				
				npaNxxXDashx = (HashMap) getContextValue("npaNxxXDashx");

				String npaNxxX = null;

				Iterator failedDashXIterator = failedDashXList.iterator();
				while (failedDashXIterator.hasNext()) {
					npaNxxX = ((String) failedDashXIterator.next());
					if(npaNxxXDashx != null)
					{
						failedNpaNxxXList = (ArrayList) npaNxxXDashx.get(npaNxxX);
						if(failedNpaNxxXList!=null)
						{
							failedTnList.addAll(failedNpaNxxXList);
						}
					}
				}

				if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
					Debug.log(Debug.MSG_STATUS,
							"failedTnList in modifyNpaNxxBelongToUS: "
									+ failedTnList);
				}
				Iterator failedTnIterator = failedTnList.iterator();

				while (failedTnIterator.hasNext()) {
					failedTn = ((String) failedTnIterator.next());
					addFailedTN(failedTn);
					errorMessage.append("[" + failedTn + "] ");
				}

				setDynamicErrorMessage(errorMessage.toString());
			}

		} catch (Exception exception) {

			logErrors(exception, napnxxQuery.toString());

		} finally {

			close(results, stmt);
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"Result for modifyNpaNxxBelongToUS : " + result);
		}
		return result;

	}

	/**
	 * This method is used to check NPA NXX belongs to US region or not.
	 * It will be called when submit SV modify request using SVID.
	 * 
	 * @return boolean, result
	 */

	public boolean isNpaNxxBelongsToUSSvid() {

		Connection conn = null;

		PreparedStatement pstmt = null;

		ResultSet results = null;

		Value tn = new Value(getContextValue("portingTn").toString());

		boolean result = false;

		try {

			conn = getDBConnection();

			if (Debug.isLevelEnabled(Debug.NORMAL_STATUS)) {
				Debug
						.log(
								Debug.NORMAL_STATUS,
								" SQL, isNpaNxxBelongsToUSSvid :\n"
										+ SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_US_QUERY);
			}
			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug
						.log(
								Debug.NORMAL_STATUS,
								"isNpaNxxBelongsToUSSvid, PortingTn for Svid :\n"
										+ tn.toString());
			}
			pstmt = conn
					.prepareStatement(SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_US_QUERY);

			pstmt.setString(1, tn.toString().substring(
					SOAConstants.TN_NPA_START_INDEX,
					SOAConstants.TN_NPA_END_INDEX));

			pstmt.setString(2, tn.toString().substring(
					SOAConstants.TN_NXX_START_INDEX,
					SOAConstants.TN_NXX_END_INDEX));

			results = pstmt.executeQuery();

			result = results.next();

			if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
				Debug.log(Debug.MSG_STATUS,
						"isNpaNxxBelongsToUSSvid, record present : " + result);
			}

		} catch (Exception exception) {

			logErrors(exception,
					SOAQueryConstants.VALIDATE_NPA_NXX_BELONG_TO_US_QUERY);

		} finally {

			close(results, pstmt);
		}
		if (Debug.isLevelEnabled(Debug.MSG_STATUS)) {
			Debug.log(Debug.MSG_STATUS,
					"isNpaNxxBelongsToUSSvid, Result : " + !result);
		}
		return !result;
	}

	/**
	 * Check the presence of SP custom fields in svcreate and svmodify
	 * request
	 */
	public boolean absentSpcustomFields(String requestType, String nancField){
		
		boolean result = true;
		
		ArrayList tnList = new ArrayList();
		StringBuffer errorMessage = new StringBuffer();
		String tn = null;
		
		String checkField = null;

		if("SPCUSTOM1".equals(nancField)){
			checkField = "SPCUSTOM1";
		}			
		else if ("SPCUSTOM2".equals(nancField)) {
			checkField = "SPCUSTOM2";
		}
		else if("SPCUSTOM3".equals(nancField)){
			checkField = "SPCUSTOM3";
		}
		
		if(existsValue("tnList")){
			tnList = (ArrayList) getContextValue("tnList");			
		}
		
		if (requestType.equals("SvCreateRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvCreateRequest/"+checkField);
		}

		if (requestType.equals("SvModifyRequest"))
		{
			result = !present("/SOAMessage/UpstreamToSOA/UpstreamToSOABody/SvModifyRequest/DataToModify/"+checkField);
		}
		
		if(!result){
			
			for(int i=0; i < tnList.size(); i++){
				tn = (String) tnList.get(i);
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
					Debug.log(Debug.MSG_STATUS,"Result for absentSpcustomFields: "+tn);
				
				addFailedTN(tn);
				errorMessage.append("[" + tn + "] ");
			}
			
			setDynamicErrorMessage(errorMessage.toString());
		}

		return result;
		
	}
	
public boolean isSvAccountIdNameExist(Value accountid, Value accountname, Value spid){
		
		boolean result = true;
		boolean accountExist = false;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet res = null;
		
		String dbAccountId = null;
		String dbAccountName = null;
		String tn = null;
		
		ArrayList tnList = null;
		StringBuffer errorMessage = new StringBuffer();
		
		try{
			conn = getDBConnection();
			pstmt = conn.prepareStatement("SELECT ACCOUNTID, ACCOUNTNAME FROM SOA_ACCOUNT WHERE ACCOUNTID=? AND CUSTOMERID IN " +
						"(SELECT CUSTOMERID FROM CUSTOMER_LOOKUP WHERE TPID=?)");
			
			pstmt.setString(1, accountid.toString());
			pstmt.setString(2, spid.toString());
			
			res = pstmt.executeQuery();
			
			if(res.next()){
				if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
					Debug.log(Debug.MSG_STATUS, "Accountid exit");
				}
				dbAccountId = res.getString(1);
				dbAccountName = res.getString(2);
				
				if(StringUtils.hasValue(dbAccountName)){
					if(accountname.toString().equals(dbAccountName)){
						accountExist = true;
						CustomerContext.getInstance().set("AccountExist", "YES");
						if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
							Debug.log(Debug.MSG_STATUS, "Both AccountId and Accountname exist");
						}
					}else{
						result = false;
						accountExist = true;
						
						if(Debug.isLevelEnabled(Debug.MSG_STATUS)){
							Debug.log(Debug.MSG_STATUS, "AccountId exist but different accountName ["+dbAccountName+"]");
						}
					}
				}
			}	
			
			if(!accountExist){
				CustomerContext.getInstance().set("AccountExist", "NO");
			}
			
			if(!result){
				
				if(existsValue("tnList")){
					tnList = (ArrayList) getContextValue("tnList");
				}
				
				for(int i=0; i < tnList.size(); i++){
					tn = (String) tnList.get(i);

					addFailedTN(tn);
					errorMessage.append("[" + tn + "] ");
				}
				
				setDynamicErrorMessage(errorMessage.toString());
			}
			if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
				Debug.log(Debug.MSG_STATUS,"isSvAccountIdUnique value is: "+result);
		}		
		catch(Exception exception){
			Debug.error("Could not validate AccountId and Accountname. " + exception.getMessage());

		}finally{			
			close(res, pstmt);
		}
		return result;
	}

}// end of class
/*
 * The purpose of this BDDMapSOANotification is to determine the exact 
 * SOA notification to be mapped with the NPAC Notification received from NPAC
 * based on DB lookups and business logic.
 *
 * @author D.Subbarao
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 *
 */
/**
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			D.Subbarao		10/06/2005			Created
	2			D.Subbarao		10/18/2005			Modified.
	3			D.Subbarao		10/24/2005			Modified.
	4			D.Subbarao		10/25/2005			Modified exceptions.
	5			D.Subbarao		10/28/2005			Modified.
	6			D.Subbarao		11/24/2005			Modified.
	7			D.Subbarao		11/30/2005			Modified.
	8			Jigar			08/28/2006			Updated to fixed TD 1474 &4175
*/

package com.nightfire.spi.neustar_soa.file;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAQueryConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;

public class BDDMapSOANotification {
    
	// It holds the mapped notification.
    private String notification = null;
    
	// It holds the last message
	private String lastMessage=null;
	
	// It holds the portingToOriginal
	private int  portingToOriginal=0;
	
	// It holds the dbStatus
	private String dbStatus=null;
	
	// It holds the lastRequestType
	private String lastRequestType=null;
	
	// It holds the dbAuthorization
	private String dbAuthorization=null;
	
	// It holds the name of the class.
	private String className="BDDMapSOANotification";
	
	// It holds name of the method.
	private String methodName=null;
	
	// It holds type of the request.
	protected String reqType=null;
	
	// It holds the new service provider id.
	private String nnSP=null;
	
	// It holds the old service provider id.
	private String onSP=null;
	
	private boolean flag = false;
	
	
	/**
	 * This method will Map VersionObjectCreationNotification into
	 * SOA notification based on last Message
	 *
	 * @param connection holds the current instance.
	 * @param spid		 contains the initiated spid.	
	 * @param newSP		 contains the new spid.
	 * @param creationTimestamp contains the timestamp.
	 * @param oldAuthorization  contains the oldauthorization.
	 * @return String as SOA Notification, or null.
	 * @throws SQLException	is thrown when any sql operation is failed.
	 * @throws FrameworkException is thrown when an application error is occurred
	 * 							  while processing the data.
	 */
	protected HashMap getMapVerObject(Connection connection,String spid,
	        String newSP,String creationTimestamp,
	        int oldAuthorization,String svid,String portingtn, String osp)
										throws SQLException,FrameworkException
  	 {
	    
	    methodName="getMapVerObject";
	    
	    HashMap valueMap = new HashMap();
	    
	    getLastMsgDetails(connection,spid,svid,portingtn);
	    
	    if(!flag)
	    {
	    	this.nnSP = newSP;
	    	this.onSP = osp;
	    	
	    }
	    
	    try{
		   
		    //	If InitSPID equals to NewSP
			if( spid.equals(newSP) && newSP.equals(nnSP))
			{
			    Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
			            +" Starts mapping a notification from NPAC to SOA " +
	             " for VersionObjectCreationNotification when initiated spid and "+
	             "NewSP are same:SPID="+spid + ",NewSP="+newSP +",OldAuthorization="+
	             oldAuthorization +",creationTimestamp="+creationTimestamp);
	
			    // Old Authorization is 'true'
			    
			    if( oldAuthorization == 1  )	{
			        
				   notification = SOAConstants.SV_RELEASE_NOTIFICATION ;
				   
				    reqType=SOAConstants.SV_RELEASE_REQUEST;
				   
				//	Old Authorization is 'false'
				}else if( oldAuthorization == 0  ){
				    
					notification = SOAConstants.SV_RELEASE_CONF_NOTIFICATION ;
					
					reqType=SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST;
				}
		   //if creationtimestamp is coming with 'VersionObjectCreationNotification'
				else if( creationTimestamp != null ){
					
				    notification = SOAConstants.SV_CREATE_ACK_NOTIFICATION ;
				    
					reqType=SOAConstants.SV_CREATE_REQUEST;
				//	Old Authorization is 'true'
				}
				
			// if InitSPId equal to oldSP
			}else if( spid.equals(onSP) )
			{
			  Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
			  +"Mapping a notification from NPAC to SOA " +
			  " for VersionObjectCreationNotification when initiated spid and"+
			  " OldSP are same:SPID="+spid + ",oldSP=" + onSP +",OldAuthorization="+
			    oldAuthorization +",creationTimestamp="+creationTimestamp);
				
				// Old Authorization is 'true'
				if( oldAuthorization == 1  ){
					notification = SOAConstants.SV_RELEASE_ACK_NOTIFICATION ;
				//	Old Authorization is 'false'
				}else if( oldAuthorization == 0  ){
					notification = SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION ;
				}
			//if creationtimestamp is coming with 'VersionObjectCreationNotification'
				else if( creationTimestamp != null ){
				    notification = SOAConstants.SV_CREATE_NOTIFICATION ;
				}
				
				Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
			          +" From NPAC to SOA mapping" +
			           " has been done for VersionObjectCreationNotification" );
			}
		    }
			catch(Exception ex){
			    
			    Debug.log(Debug.ALL_ERRORS,className + "[" + methodName + "]"+
			            ex.getMessage());
			    
			    throw new FrameworkException(ex.getMessage());
			}
			
		   valueMap.put("notification",notification);
		   valueMap.put("flag",Boolean.valueOf(flag));
		   
		return valueMap;
	}
	/**
	 * This method will Map VersionStatusAttributeValueChangeNotification into
	 * SOA notification based on last Message
	 * 
	 * @param connection holds the current instance.
	 * @param spid		 contains the initiated spid.	
	 * @param svid		 contains the SubscriptionVersion id.
	 * @param failedSP	 contains the number of failedsp's
	 * @param status	 contains the status of notification.
	 * @param portingtn	 contains the porting tn.
	 * @return	String as SOA Notification, or null.
	 * @throws SQLException		is thrown when any sql operation is failed.
	 * @throws FrameworkException is thrown when an application error is occurred
	 * 							  while processing the data.
	 */
	
	protected String getMapVerStatus(Connection connection,String spid,
	      String svid,int failedSP,String status,
	      String portingtn)	throws SQLException,FrameworkException
	{
	    
	  methodName="getMapVerStatus";  
	 
      try{
		
        getLastMsgDetails(connection,spid,svid,portingtn);
        
		status=SOAUtility.getStatus(status);
		
		notification = SOAConstants.SV_STS_CHANGE_NOTIFICATION;

		Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
		   +" Starts mapping a notification from NPAC to SOA " +
		   " for VersionStatusAttributeValueChangeNotification");
		
		// if status is 'old'
		if( status.equals(SOAConstants.OLD_STATUS) ) {
		    
		   Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
		    +"Mapping a notification from NPAC to SOA " +
		    " for VersionStatusAttributeValueChangeNotification when status " +
		    "is old: status="+status + ",failedSP="+failedSP);
		    
			if(failedSP == 1)	{
			    
				notification = SOAConstants.SV_STS_CHANGE_NOTIFICATION ;
			}
			else	{
			    
				notification = SOAConstants.SV_DISCONNECT_NOTIFICATION ;

			}
		//	if status is 'active'
		}else if( status.equals(SOAConstants.ACTIVE_STATUS) )
		{
		    Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
		            +"Mapping a notification from NPAC to SOA " +
			 " for VersionStatusAttributeValueChangeNotification when status " +
			 "is active: status="+status + ",failedSP="+failedSP +
			 ",portingToOriginal="+portingToOriginal);
		    
		    if( failedSP == 1 ){

				notification = SOAConstants.SV_STS_CHANGE_NOTIFICATION ;

			}else {
			   
			    if(spid.equals(onSP)) {
				// if PortingToOriginal is 'true'
				if( portingToOriginal == 1 ) {

					notification = SOAConstants.SV_PTO_NOTIFICATION ;

				}else	{
				    
					notification = SOAConstants.SV_ACTIVATE_NOTIFICATION ;

				}
				reqType=SOAConstants.SV_ACTIVATE_REQUEST;
			  }

			}

		//	if status is 'canceled'
		}else if( status.equals(SOAConstants.CANCELED_STATUS) )
		{
		    Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
		            +"Mapping a notification from NPAC to SOA " +
			" for VersionStatusAttributeValueChangeNotification when status " +
			"is canceled: status="+status + ",dbStatus="+dbStatus);
			// if DB status is other than 'conflict'
			if(dbStatus != null && !dbStatus.equals( SOAConstants.CONFLICT_STATUS ) )	{
				
			    notification = SOAConstants.SV_CANCEL_NOTIFICATION ;
			}

		// if status is 'conflict'
		}else if ( status.equals(SOAConstants.CONFLICT_STATUS ) )
		{

		    Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
		            +"Mapping a notification from NPAC to SOA " +
			" for VersionStatusAttributeValueChangeNotification when status " +
			"is conflict: spid="+ spid + ",oldSP="+onSP+",status="+status +
			",lastRequestType="+lastRequestType);   
			// if spid equal to 'OLD SPID'
			if( spid.equals( onSP ) )	{
					
				    notification
						= SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION ;
					
					return notification;
			}
		}	
	
		Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
		        +"Mapping a notification from NPAC to SOA " +
		  " has been done for VersionStatusAttributeValueChangeNotification" );
        }catch(SQLException se){
            
            Debug.log(Debug.ALL_ERRORS,className + "[" + methodName + "]"+
                    se.getMessage());
            
		    throw new FrameworkException(se.getMessage());
		}
		catch(Exception ex){
		    
		    Debug.log(Debug.ALL_ERRORS,className + "[" + methodName + "]"+
		            ex.getMessage());
		    
		    throw new FrameworkException(ex.getMessage());
		}
		return notification;
	}
	/**
	 
	 * This method will Map VersionAttributeValueChangeNotification into
	 * SOA notification based on last Message
	 * 
	 * @param connection holds the current instance.
	 * @param spid		 contains the initiated spid.	
	 * @param newSP		 contains the new spid.
	 * @param svid		 contains the SubscriptionVersion id.
	 * @param portingtn	 contains the porting tn.
	 * @param creationTimestamp contains the timestamp.
	 * @param oldAuthorization  contains the oldauthorization.
	 * @return String as SOA Notification, or null.
	 * @throws SQLException		is thrown when any sql operation is failed.
	 * @throws FrameworkException is thrown when an application error is occurred
	 * 							  while processing the data.
	 */
	
	protected String getMapVerAttribute(Connection connection,
	        String spid, String svid,String portingtn,
	        String creationTimestamp,int oldAuthorization)
							throws SQLException,FrameworkException
    	{
	    
	    methodName="getMapVerAttribute";
	    
	    //getLastMsgDetails(connection,spid,svid,portingtn);
	    
	    try{
	    
	    Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
	    +" Starts mapping a notification from NPAC to SOA" +
	     " for VersionAttributeValueChangeNotification" );
     
        getLastMsgDetails(connection,spid,svid,portingtn);
        
        notification = SOAConstants.SV_ATR_CHANGE_NOTIFICATION;
        
        // if InitSPID equal to NewSP
		if( spid.equals(nnSP))
		{
		   
		    Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
		    +"  Mapping a notification from NPAC to SOA" +
		    " for VersionAttributeValueChangeNotification when initiated spid" +
		    " and newSP are same: spid="+ spid + ",newSP="+nnSP+
		    ",lastMessage="+lastMessage +",dbAuthorization="+dbAuthorization
		    +",oldAuthorization="+oldAuthorization);  
		  
			// if lastMessage is 'SvCreateAckNotification'
			if( lastMessage.equals(SOAConstants
											.SV_CREATE_ACK_NOTIFICATION) )
			{
				//if OldAuthorizationFlag is 'true'
				if( ( dbAuthorization == null && oldAuthorization == 1 ) )
				{

					notification = SOAConstants.SV_RELEASE_NOTIFICATION ;
					
					reqType=SOAConstants.SV_RELEASE_REQUEST;

				}

			}
			else
				{
				  if( creationTimestamp != null )
				  {	
				    notification = SOAConstants.SV_CREATE_ACK_NOTIFICATION ;
					    
					reqType=SOAConstants.SV_CREATE_REQUEST;
				  }
				}		
		// if IntiSPID equal to OldSp
		}else if( spid.equals(onSP) )
		{
		   Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
			 +" Mapping a notification from NPAC to SOA" +
			 " for VersionAttributeValueChangeNotification when initiated spid"+
			 " and newSP are same: spid="+ spid + ",oldSP="+onSP+
			 ",lastMessage="+lastMessage +",oldAuthorization="+oldAuthorization
			 + "creationTimestamp="+creationTimestamp);   
		    
			//if last message is 'SvReleaseAckNotification'
			if( lastMessage.equals(
								SOAConstants.SV_RELEASE_ACK_NOTIFICATION)
				|| lastMessage.equals(
								SOAConstants.SV_RELEASE_CONF_ACK_NOTIFICATION) )
			{
				if( creationTimestamp != null )
				{
					
				    notification = SOAConstants.SV_CREATE_NOTIFICATION ;
				    
					reqType=SOAConstants.SV_RELEASE_REQUEST;
					
				}
			//	if last message is 'SvReleaseRequest'
			}else
			{
				// autorization flag will not come when NewSp sends modify
				// request , this will come when Oldsp sends request
				if ( oldAuthorization == 1 )
				{
					notification = SOAConstants.SV_RELEASE_ACK_NOTIFICATION ;
					
					reqType=SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST;
				}
			}
			Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
			     +" From NPAC notification to exact SOA notification mapping" +
			" has been done for VersionAttributeValueChangeNotification" );
		}
		
		}catch(SQLException se){
		    
		    Debug.log(Debug.ALL_ERRORS,className + "[" + methodName + "]"+
		            se.getMessage());
		    
 		    throw new FrameworkException(se.getMessage());
 		}
 		catch(Exception ex){
 		    
 		    Debug.log(Debug.ALL_ERRORS,className + "[" + methodName + "]"+
 		            ex.getMessage());
 		    
 		    throw new FrameworkException(ex.getMessage());
 		}
 		Debug.log(8,"Notification is : "+notification);
		return notification;
	}
	/**
	 * This will be used to get the details of a particular notification from
	 * SOA_SUBSCRIPTION_TABLE.
	 * 
	 * @param spid	contains the initiated spid either old or new.	
	 * @param svid	 contains the SubscriptionVersion id.
	 * @param portingtn contains the porting tn.
	 * @throws SQLException	is thrown when any sql operation is failed.
	 */
	private void getLastMsgDetails(Connection connection,String spid,String svid,
	        String portingtn) throws SQLException,FrameworkException{
	    

		try{

	    methodName="getLastMsgDetails";
	        
	    Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
	 	+" Starts finding the required details to map from NPAC notification "+
	 	"to exact SOA notification by these details:spid="+ spid + "svid="+svid
	 	+ ",PortingTN="+portingtn);

        // This will be used to get the last message details 
        // from SOA_SUBSCRIPTION_VERSION table

		PreparedStatement lastMessageSt=connection.prepareStatement
							(SOAQueryConstants.LASTMESSAGE_EXISTS);
		
		lastMessageSt.setString(1,spid);
		
		lastMessageSt.setString(2,svid);
		
		lastMessageSt.setString(3,portingtn.trim());
		
		ResultSet lastMessageRs=lastMessageSt.executeQuery();
		
		if(lastMessageRs.next()) {
		    
		    lastMessage=lastMessageRs.getString(1);
		    
		    String portTOori=lastMessageRs.getString(2);
		    
		    if(portTOori!=null)
		        
		        portingToOriginal=Integer.parseInt(portTOori);
		    
		    // This assigns the status 
		    dbStatus=lastMessageRs.getString(3);
		    
		    // This assigns the lastrequesttype
		    lastRequestType=lastMessageRs.getString(4);
		    
		    // This assigns the dbauthorization
		    dbAuthorization=lastMessageRs.getString(5);
		    
		    // This assigns the newsp
		    onSP=lastMessageRs.getString(6);
		    
		    // This assigns the oldsp
		    nnSP=lastMessageRs.getString(7);
		    
		    flag = true;
		 }
		lastMessageRs.close();
		
		lastMessageSt.close();
		
		Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName + "]"
	 	+" The required details have been retreived to map from " +
	 	" NPAC notification to exact SOA notification," +
	 	" the details are:lastMessage="+lastMessage +",portingToOriginal="+
	 	portingToOriginal +",dbStatus="	+ dbStatus + ",lastRequestType="+
	 	lastRequestType +",dbAuthorization=" + dbAuthorization);
		
		
	}catch(SQLException ex){
	   
	    Debug.log(Debug.ALL_ERRORS,className + "[" + methodName + "]"+
	            ex.getMessage());
	    
	    throw new FrameworkException("Database operation is failed while " +
	            " retrieving the lastmessage details from " +
	             SOAConstants.SV_TABLE + " table.");
	    
	 }
	catch(Exception fex){
	    
	    Debug.log(Debug.ALL_ERRORS,className + "[" + methodName + "]"+
	            fex.getMessage());
	    
	    throw new FrameworkException(fex.getMessage());
	    
 	  }

   }
}


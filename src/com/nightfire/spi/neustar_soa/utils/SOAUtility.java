/**
 * This is a utility class. All the common util methods are defined in this
 * class.
 *
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 *
 */
/**
Revision History
---------------------
Rev#		Modified By 	Date			Reason
-----       -----------     ----------		--------------------------
1			Ahsok			07/30/2004		Created
2			Ashok 		    09/03/2004		getStatus and getLnpType methods added
3           Subbarao		08/30/2005      notificationsList,isFileTypeExists,validateProperties methods added.
4			Subbarao		09/12/2005		Eleminated the spid property from 
											validation on properties file.
5			Subbarao		09/28/2005		Changed debug statment in validateProperties method.
6			Subbarao		10/10/2005	    Modified and Added new method 
											updateSvLastMessage.
7			Subbarao		10/19/2005		Modifed a constant name. 
8			Subbarao		10/20/2005		Modifed a constant name. 
9			Subbarao		10/24/2005		Modified updateSvLastMessage and 
											notificationsList methods.	
10			Subbarao		10/28/2005		Modified and added new methods.
11 			Subbarao		11/24/2005		Modifed some methods.
12			Subbarao		11/30/2005		Added two properites in 
											validateProperties method. 
13			Subbarao		12/16/2005		Eleminated a condition.
14			Jigar Talati	04/14/2006		Added getSPType method.
15			Manoj Kumar		06/15/2006		Added getSVType method
16			Rohit Gupta		08/17/2007		Added method formatTn to format the tn for searching

*/

package com.nightfire.spi.neustar_soa.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.file.DBTokenConsumerBase;

public class SOAUtility {


	private static String className="SOAUtitlity"; 
    
    private static String methodName=null;

	private static final String NULL_VALUE = "null";
	
	public static String npacResp=null;

	/**
	 * Checks value argument to see if it is null, or is a string with the text
	 * value "null" (case-invariant).
	 * 
	 * @param value
	 *            Value argument to check
	 * @return 'true' if null, otherwise 'false'.
	 */
	public static boolean isNull(Object value) {
		if (value == null) {

			return true;

		}

		if ((value instanceof String)
				&& ((String) value).equalsIgnoreCase(NULL_VALUE)) {

			return true;

		}

		return false;

	}

	/**
	 * This method will return String value of Status
	 * 
	 * @param statusStr
	 *            String
	 * @return Status as String
	 */
	public static String getStatus(String statusStr)

	{

		int statusInt = -1;

		try {

			statusInt = Integer.parseInt(statusStr);

		} catch (NumberFormatException exe) {

			Debug.error(" Could not parse :" + statusStr + "\n" + exe);

		}

		String status = "";

		switch (statusInt) {
		case 0:

			status = SOAConstants.CONFLICT_STATUS;

			break;

		case 1:

			status = SOAConstants.ACTIVE_STATUS;

			break;

		case 2:

			status = SOAConstants.PENDING_STATUS;

			break;

		case 3:

			status = SOAConstants.SENDING_STATUS;

			break;

		case 4:

			status = SOAConstants.DOWNLOAD_FAILED_STATUS;

			break;

		case 5:

			status = SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS;

			break;

		case 6:

			status = SOAConstants.DISCONNECT_PENDING_STATUS;

			break;

		case 7:

			status = SOAConstants.OLD_STATUS;

			break;

		case 8:

			status = SOAConstants.CANCELED_STATUS;

			break;

		case 9:

			status = SOAConstants.CANCEL_PENDING_STATUS;

			break;

		}

		return status;

	}

	/**
	 * This method will return String value of LNPTYPE
	 * 
	 * @param lnpType
	 *            String
	 * @return lnpType as String
	 */
	public static String getLnpType(String lnpType) {

		if (lnpType.equals("0")) {

			return SOAConstants.LSPP;

		} else if (lnpType.equals("1")) {

			return SOAConstants.LISP;

		} else if (lnpType.equals("2")) {

			return SOAConstants.POOL;

		} else {

			return "";
		}

	}

  /**
	 * This method load the properties file and validate the paramerters. If the
	 * parameters are null or "" or invalid then it will stop the application
	 * execution.
	 * 
	 * @param loadSOABdd
	 *            Properties file name
	 * @return HashMap contains source file name and custormerId
	 */

	public static HashMap validateProperties(String loadSOABdd) {
	
	    methodName="validateProperties";  
	    
	    String custId = null;
	    
		String bddFile = null;
		
		String dbname = null;
		
		String dbuser = null;
		
		String dbpass = null;
		
		String logFile=null;
		
		String reportFile=null;

		Properties properties = new Properties();
		
		HashMap hashMap = new HashMap();
		
		Debug.enableAll();
		FileInputStream fin = null;
		try {
		    
			fin = new FileInputStream(new File(loadSOABdd));
		    properties.load(fin);
		    
		} catch (IOException e) {
		    
		    e.printStackTrace();
		    
		    if ( Debug.isLevelEnabled( Debug.IO_ERROR )){
		    	Debug.log(Debug.IO_ERROR, "Property file does not exist " +
						"in the system:" + loadSOABdd);
		    }
		    try {
				fin.close();
			} catch (IOException ioe) {
				Debug.log(Debug.IO_ERROR, "Failed to close the File Input Stream " + ioe.getMessage());
			}
			
			System.exit(-1);
		}finally{
			try {
				fin.close();
			} catch (IOException e) {
				Debug.log(Debug.IO_ERROR, "Failed to close the File Input Stream " + e.getMessage());
			}
		}
		
		try
		{
			bddFile = properties.getProperty(SOAConstants.SOURCE_FILE_NAME);
			
			custId = properties.getProperty(SOAConstants.DOMAIN_NAME);
			
			dbname = properties.getProperty(SOAConstants.DATABASE_NAME);
			
			dbuser = properties.getProperty(SOAConstants.DATABASE_USER_NAME);
			
			dbpass = properties.getProperty(SOAConstants.DATABASE_PASSWORD);
			
			logFile= properties.getProperty(SOAConstants.LOG_FILE_NAME);
			
			reportFile=properties.getProperty(SOAConstants.REPORT_FILE_NAME);
			
			// check to see if all parameters were given, and if not,
			// print message string and exit
			if (bddFile == null || bddFile.equals("")) {
				
				if ( Debug.isLevelEnabled( Debug.ALL_ERRORS )){
					Debug.log(Debug.ALL_ERRORS,className + "[" + methodName +
				    "] Bulk load file not found in the property file");
				}
			    				
			    System.exit(-1);
			}
			else if (dbname == null || dbname.equals("")) {
				
				if ( Debug.isLevelEnabled( Debug.DB_STATUS )){
					Debug.log(Debug.DB_STATUS,className + "[" + methodName +
				     "] Database name not found in the property file");
				}
									
			    System.exit(-1);
			    
			} else if (dbuser == null || dbuser.equals("")) {
				
				if ( Debug.isLevelEnabled( Debug.DB_STATUS )){
					Debug.log(Debug.DB_STATUS,className + "[" + methodName +
				      "] Database user not found in the property file");
				}					
				
			    System.exit(-1);
				
			} else if (dbpass == null || dbpass.equals("")) {
				
				if ( Debug.isLevelEnabled( Debug.DB_STATUS )){
					Debug.log(Debug.DB_STATUS,className + "[" + methodName +
				      "] Database password not found in the property file");
				}					
				
			    System.exit(-1);
			    
			} else if (custId == null || custId.equals("")) {
				
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					Debug.log(Debug.MSG_STATUS,className + "[" + methodName +
				      "] Customer Id not found in the property file");
				}					
				
			    System.exit(-1);
			} 
			else if (logFile == null || logFile.equals("")) {
			
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					Debug.log(Debug.MSG_STATUS,className + "[" + methodName +
				      "] Log File not found in the property file");
				}
					
				
			    System.exit(-1);
			}
			else if (reportFile == null || reportFile.equals("")) {
			
				if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
					Debug.log(Debug.MSG_STATUS,className + "[" + methodName +
				      "] reportFile not found in the property file");
				}
				
			    System.exit(-1);
			} 

				hashMap.put(SOAConstants.SOURCE_FILE_NAME, bddFile);
				
				hashMap.put(SOAConstants.DOMAIN_NAME, custId);
				
				hashMap.put(SOAConstants.DATABASE_NAME, dbname);
				
				hashMap.put(SOAConstants.DATABASE_USER_NAME, dbuser);
				
				hashMap.put(SOAConstants.DATABASE_PASSWORD, dbpass);
				
				hashMap.put(SOAConstants.LOG_FILE_NAME, logFile);
				
				hashMap.put(SOAConstants.REPORT_FILE_NAME, reportFile);
				
		 
		}catch(Exception ex){
		    
			if ( Debug.isLevelEnabled( Debug.DB_ERROR )){
				Debug.log(Debug.DB_ERROR,className + "[" + methodName +
				           "] "+  ex.toString());
			}
				
				System.exit(-1);
		}
		return hashMap;
	}
	/**
	 * This will be used to provide all the notifications for mapping from NPAC
	 * to SOA then for inserting a particular notification BDD data into SOA 
	 * datatabase tables.
	 * 
	 * @return notificationMap
	 */
	public static HashMap notificationsList() {
		ArrayList notification = null;
		HashMap notificationMap = new HashMap();

		// This block of constants data will be set for
		// subscriptionVersionCancellationAcknowledgeRequest

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT); // insert
																	 // sql.
		notification.add(SOAConstants.SV_CANCEL_ACK_NOTIFICATION); 
		// messagesubtype
		notificationMap.put(
		        SOAConstants.SV_VERSION_CANCELLATION_ACK_REQUEST_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionVersionDonorSP-CustomerDisconnectDate

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(SOAConstants.SOA_SV_DONORSP_CUSTOMER_DISCONNECTDATE);
		notificationMap.put
				(SOAConstants.SV_DONORSP_CUSTOMER_DISCONNECTDATE_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionVersionNewSP-CreateRequest

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(SOAConstants.T1_CREATE_REQUEST_NOTIFICATION);
		notificationMap.put(SOAConstants.SV_NEWSP_CREATE_REQUEST_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionVersionOldSP-ConcurrenceRequest

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(SOAConstants.T1_CONCURRENCE_REQUEST_NOTIFICATION);
		notificationMap.put(SOAConstants.SV_OLDSP_CONCURR_REQUEST_NOTIFICATION,
		        notification);

		// This block of constants data will be set for
		// subscriptionVersionStausAttributeValueChange

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION);
		notificationMap.put(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionVersionOldSPFinalConcurrenceWindowExpiration

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(
		      SOAConstants.T2_FINAL_CONCURRENCE_WINDOW_EXPIRATION_NOTIFICATION);
		notificationMap.put(
				SOAConstants.VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionVersionNewSPFinalCreateWindowExpirationNotification

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(
		        SOAConstants.T2_FINAL_CREATE_WINDOW_EXPIRATION_NOTIFICATION);
		notificationMap.put(
				SOAConstants.VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionAudit-DiscrepancyRpt

		notification = new ArrayList();
		notification.add(SOAConstants.SUB_AUDIT_DIS_RPT_INSERT);
		notification.add(SOAConstants.AUDIT_DISCRE_REPORT_NOTIFICATION);
		notificationMap.put(SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionAuditResults

		notification = new ArrayList();
		notification.add(SOAConstants.SUB_AUDT_RSLTS_INSERT);
		notification.add(SOAConstants.AUDIT_RESULTS_NOTIFICATION);
		notificationMap.put(SOAConstants.SUB_AUDIT_RESULTS_NOTIFICATION, notification);

		// This block of constants data will be set for
		// subscriptionAudit-CreateRequest

		notification = new ArrayList();
		notification.add(SOAConstants.SUB_AUDIT_OBJECT_CREATE_DELEETE_INSERT);
		notification.add(SOAConstants.AUDIT_OBJECT_CREATE_NOTIFICATION);
		notificationMap.put(SOAConstants.SUB_AUDIT_OBJECT_CREATION_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionAudit-DeleteRequest

		notification = new ArrayList();
		notification.add(SOAConstants.SUB_AUDIT_OBJECT_CREATE_DELEETE_INSERT);
		notification.add(SOAConstants.AUDIT_OBJECT_DELETE_NOTIFICATION);
		notificationMap.put(SOAConstants.SUB_AUDIT_OBJECT_DELETION_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionNewNPA-NXX

		notification = new ArrayList();
		notification.add(SOAConstants.SV_NEW_NPA_NXX_INSERT);
		notification.add(SOAConstants.SV_NEW_NPANXX);
		notificationMap.put(SOAConstants.SUB_NEW_NPANXX_NOTIFICATION, notification);

		// This block of constants data will be set for
		// numberPoolBlock-object creation

		notification = new ArrayList();
		notification.add(SOAConstants.NBRPOOLBLOCK_OBJCREATION_INSERT);
		notification.add(SOAConstants.NBR_POOLBLK_OBJECT_CREATE);
		notificationMap.put(SOAConstants.NBR_POOLBLK_OBJECT_CREATION_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// numberPoolBlock-AttributeValueChange

		notification = new ArrayList();
		notification.add(SOAConstants.NBRPOOLBLOCK_OBJCREATION_INSERT);
		notification.add(SOAConstants.NBR_POOLBLK_ATTR_VAL_CH_NOTIFICATION);
		notificationMap.put(SOAConstants.NBR_POOLBLK_ATTR_VAL_CHANGE_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// numberPoolBlockStausAttributeValueChange

		notification = new ArrayList();
		notification.add(SOAConstants.NBRPOOLBLK_STAUS_ATTR_VAUE_CHANGE_INSERT);
		notification
				.add(SOAConstants.NBR_POOLBLK_ATTR_VAL_STATUS_CH_NOTIFICATION);
		notificationMap.put(SOAConstants.NBR_POOLBLK_STA_ATTR_VAL_CHANGE_NOTIFICATION,
				notification);

		// This block of constants data will be set for
		// subscriptionVersionNPAC-ObjectCreation

		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(SOAConstants.VERSION_OBJECT_CREATION_NOTIFICATION);
		notificationMap
				.put(SOAConstants.VERSION_OBJECT_CREATION_NOTIFICATION, notification);

		// This block of constants data will be set for
		// subscriptionVersionNPAC-attributeValueChange
		
		notification = new ArrayList();
		notification.add(SOAConstants.SOA_NOTIFICATION_DATA_INSERT);
		notification.add(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION);
		notificationMap
				.put(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION, notification);

		// This block of constants data will be set for
		// lnpNPAC-SMS-Operational-Information.

		notification = new ArrayList();
		notification.add(SOAConstants.NPAC_OPERATIONS_INSERT);
		notification.add(SOAConstants.SOA_LNP_NPAC_SMS_OPER_INFO);
		notificationMap.put(SOAConstants.LNP_NPAC_SMS_OPER_INFO, notification);

		return notificationMap;
	}
	
	
	/**
	 * This will be used to make use the NPAC notifications to map it from
	 * from NPAC to SOA while inserting the data into SOA database tables. 
	 * 
	 * @param NotificationID contains the notification ID.
	 * @param objectId contains the objectID.
	 * @return npacNotification contains a notification which is being sent 
	 * 						by NPAC.
	 */
    public static String returnNpacNotification(int notificationID,int objectId){	
	   
       String npacNotification=null;
        
   
       switch(notificationID){
       
       	case 1: 
       	    	npacNotification=SOAConstants.LNP_NPAC_SMS_OPER_INFO;
       	    	
       	    	npacResp=SOAConstants.NPAC_LNP_SMS_OPER_INFO;
       	    	
       	    	break;
       	    	
       	case 2:	npacNotification=SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION;
       	
       			npacResp=SOAConstants.NPAC_SUB_AUDIT_DIS_RPT;
       			    
       			break;
       			
       	case 3:
       	    	npacNotification=SOAConstants.SUB_AUDIT_RESULTS_NOTIFICATION;
       	    	
       	    	npacResp=SOAConstants.NPAC_SUB_AUDIT_RSLTS;
       	    	
       	    	break;
       	    	
       	case 4: 
       	    	npacNotification=
       	    	    SOAConstants.SV_VERSION_CANCELLATION_ACK_REQUEST_NOTIFICATION;
       	    	
       	    	npacResp=SOAConstants.NPAC_SV_CAN_ACK_REQ;
       	    	
       	    	break;
       	    
       	case 18:    	    	
   	        	npacNotification=
       	    	  SOAConstants.SV_VERSION_CANCELLATION_ACK_REQUEST_NOTIFICATION;
   	        	
   	        	npacResp=SOAConstants.NPAC_SV_RANGE_CAN_ACK_REQ;
   	        	
       	    	break;
       	    	
       	case  6: 
       			npacNotification=
      	    	 SOAConstants.SV_DONORSP_CUSTOMER_DISCONNECTDATE_NOTIFICATION;
	    	
       			npacResp=SOAConstants.NPAC_SV_DONOR_CUST_DIS;
       			
       			break;
       			
       	case 17: 
	    		npacNotification=
       	    	 SOAConstants.SV_DONORSP_CUSTOMER_DISCONNECTDATE_NOTIFICATION;
	    		
	    		npacResp=SOAConstants.NPAC_SV_RANGE_DONOR_CUST_DIS;
	    		
       	    	break;
       	    	
       	case 8:
       	    	npacNotification=SOAConstants.SUB_NEW_NPANXX_NOTIFICATION;
       	    	
       	    	npacResp=SOAConstants.NPAC_SV_NEW_NPANXX;
       	    	    
       	    	break;
       	    	
       	case 9: 
       	    	npacNotification=SOAConstants.SV_NEWSP_CREATE_REQUEST_NOTIFICATION;
       	    	
       	    	npacResp=SOAConstants.NPAC_SV_NEWSP_CRE_REQ;
       	    	
       	    	break;
       	    	
       	case 19:    	         
   	        	npacNotification=SOAConstants.SV_NEWSP_CREATE_REQUEST_NOTIFICATION;
   	        	
   	        	npacResp=SOAConstants.NPAC_SV_RANGE_NEWSP_CRE_REQ;
   	        	
   	        	break;
       	     	
       	case 10: 
       			npacNotification=SOAConstants.SV_OLDSP_CONCURR_REQUEST_NOTIFICATION;
       			
       			npacResp=SOAConstants.NPAC_SV_OLDSP_CONCUR_REQ;
       			
       			break;
       	    
       	case 20:
       	    	npacNotification=SOAConstants.SV_OLDSP_CONCURR_REQUEST_NOTIFICATION;
       	    	
       	    	npacResp=SOAConstants.NPAC_SV_RANGE_OLDSP_CONCUR_REQ;
       	    	
       	    	break;
       	      	
       	case 11: 
       			npacNotification=SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION;
       			
       			npacResp=SOAConstants.NPAC_SV_STA_ATTR_VAL_CH;
       			
       			break;
       	    
       	case 14:
	    		
       	    	npacNotification=SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION;
	    		
	    		npacResp=SOAConstants.NPAC_SV_RANGE_STA_ATTR_VAL_CH;
	    		
       	    	break;
       	    	
       	case 12:  
       	    
       	    	npacNotification=SOAConstants.VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION;

       	    	npacResp=SOAConstants.NPAC_SV_OLDSP_FINAL_CONCUR_WINEXP;
       	    	
       	    	break;

       	case 21:       	    
    	        npacNotification=
    	    	    SOAConstants.VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION;
    	        
    	        npacResp=SOAConstants.NPAC_SV_RANGE_OLDSP_FINAL_CONCUR_WINEXP;
    	        
    	    	break;
       	        
    	case 13:
    	    	npacNotification=SOAConstants.NBR_POOLBLK_STA_ATTR_VAL_CHANGE_NOTIFICATION;
    	    	
    	    	npacResp=SOAConstants.NPAC_NBR_POOLBLK_STA_ATTR_VAL_CH;
    	    	
    	    	break;
    
    	case 15:
    	    	
    	    	npacNotification=SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION;
    	    	
    	    	npacResp=SOAConstants.NPAC_SV_RANGE_ATTR_VAL_CH;
    	    	
    	    	break;
         	         
    	case 16:
    	       npacNotification=SOAConstants.VERSION_OBJECT_CREATION_NOTIFICATION;
    	       
    	       npacResp=SOAConstants.NPAC_SV_RANGE_OBJ_CRE;
    	       
    	       		break;
    	case 22:
    	    	npacNotification=
    	        SOAConstants.VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION;
    	    	
    	    	npacResp=SOAConstants.NPAC_SV_RANGE_NEWSP_FINAL_CRE_WINEXP;
    	    	
    	    	break;
  
    	    
    	case 23:
    	        npacNotification=
    	        SOAConstants.VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION;
    	        
    	        npacResp=SOAConstants.NPAC_SV_NEWSP_FINAL_CRE_WINEXP;
    	        
    	    	break;
  
      	case 1001:        
      	    	if(objectId==21){
      	    	    
      	    	    npacNotification=SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION;
      	    	  
      	    	    npacResp=SOAConstants.NPAC_SV_ATTR_VAL_CH;
      	    	          	    	    
	    		}
	    		else if(objectId==30){
	    		    
	    		    npacNotification=SOAConstants.NBR_POOLBLK_ATTR_VAL_CHANGE_NOTIFICATION;
	    		    
	    		    npacResp=SOAConstants.NPAC_NBR_POOLBLK_ATTR_VAL_CH;
	    		}
	    		break;    	    	
    	case 1006: 
    	    	if(objectId==21){
    	    	   
    	    	  npacNotification=SOAConstants.VERSION_OBJECT_CREATION_NOTIFICATION;
    	    	  
    	    	  npacResp=SOAConstants.NPAC_SV_OBJ_CRE;
    	    	  
    	    	}
    	    	else if(objectId==19){

    	    	  npacNotification=SOAConstants.SUB_AUDIT_OBJECT_CREATION_NOTIFICATION;
    	    	  
    	    	  npacResp=SOAConstants.NPAC_SUB_AUDIT_OBJ_CR;
    	    	    
    	    	}
    	    	else if(objectId==30){
    	    	    
           	      npacNotification=SOAConstants.NBR_POOLBLK_OBJECT_CREATION_NOTIFICATION;
           	      
           	      npacResp=SOAConstants.NPAC_NBR_POOLBLK_OBJ_CR;
           	   
    	    	}
    	    	break;
       	case 1007:               
       	        
       	    	npacNotification=SOAConstants.SUB_AUDIT_OBJECT_DELETION_NOTIFICATION;
       	     
       	        npacResp=SOAConstants.NPAC_SUB_AUDIT_OBJ_DEL;
       	        
       	        break;
       	        
       }       
       return npacNotification;
       	                     
    }
	
	/**
	 * This will be called to update the lastmessage column in SOA_SUBSCRITPION_VERSION
	 * table based upon a criteria might include spid,svid and portingtn.

	 * @param connection   returns an instance of a connection.	
	 * @param lastMessage  contains the message to update.
	 * @param spid	   contains the init spid.	
	 * @param svid		   contains the version id.	
	 * @param portingtn	   contains the porting tn.	
	 * @return void 		
	 * @throws SQLException is thrown when any sql opreation is failed.
	 * @throws FrameworkException is thrown when any application failure is 
	 * 							  occurred.
	 */
	 public static void updateSvLastMessage(Connection connection,
	          String lastMessage,String spid,String svid,
	          String portingtn) throws SQLException,FrameworkException{
	    
	      methodName="updateSvLastMessage";
	      
	      PreparedStatement lastMessageSt =null;
	      
	     try{
	    	 
			 if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
				 
				 Debug.log(Debug.MAPPING_STATUS, className +"["+methodName +
			             "] The Parameters will be set to update the columns" +
			             " in " + SOAConstants.SV_TABLE + "table:");
			 }	        
	              
			  if(lastMessage!=null) {			  
		  
		      	  connection.setAutoCommit(false);
		  
		  		  lastMessageSt = connection.prepareStatement
		      					(SOAConstants.SV_LASTMESSAGE_UPDATE_QUERY);
		      
		          lastMessageSt.setString(1,lastMessage);
		      
		          lastMessageSt.setString(2,spid);
		      
		          lastMessageSt.setString(3,svid);
		      
		          lastMessageSt.setString(4,portingtn);
		          
		          if(lastMessageSt.executeUpdate()!=0)
		          
		        	  if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
		        		
		        		  Debug.log(Debug.MAPPING_STATUS,className + "["+ methodName
		  			            + "] The LastMessage column has been updated with" 
		  			      		+ " in " + SOAConstants.SV_TABLE +  " table");
		        	  }
		        		  
		      else
		    	  if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
		    		  
		    		  Debug.log(Debug.MAPPING_STATUS,className + "["+ methodName
		 				     + "] The Updation has been failed for lastmessage column" 
		 			       	 + " in " + SOAConstants.SV_TABLE + " table");
		    	  }			          
		              
		  		  lastMessageSt.close();
			   } 	

	      }catch(SQLException sqlEx){
	          throw new FrameworkException(sqlEx.getMessage());
	      }
	      catch(Exception ex){
	          throw new FrameworkException(ex.getMessage());
	      }
	  }
	 
	 /**
		 * This will be called to update the lastmessage column in SOA_SUBSCRITPION_VERSION
		 * table based upon a criteria might include spid,svid and portingtn.

		 * @param connection   returns an instance of a connection.	
		 * @param lastMessage  contains the message to update.
		 * @param spid	   contains the init spid.	
		 * @param svid		   contains the version id.	
		 * @param portingtn	   contains the porting tn.	
		 * @return void 		
		 * @throws SQLException is thrown when any sql opreation is failed.
		 * @throws FrameworkException is thrown when any application failure is 
		 * 							  occurred.
		 */
		 public static void insertSOASubscriptionVersion(Connection connection, String customerID,
		          String refKey, String[] params, String tn, String svid, String messageSubType) throws SQLException,FrameworkException{
		    
		      methodName="insertSOASubscriptionVersion";
		      
		      PreparedStatement insertStatement =null;
		      
		     try{
		    	 if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
		    		 
		    		 Debug.log(Debug.MAPPING_STATUS, className +"["+methodName +
				             "] The Parameters will be set to update the columns" +
				             " in " + SOAConstants.SV_TABLE + "table:");
		    	 }
		        
		              
			      if(params != null) {   
			      
			          	connection.setAutoCommit(false);
			      
			          	insertStatement = connection.prepareStatement
			          					(SOAConstants.SUBSCRIPTION_VERSION_INSERT);
				      
			      		  
			      		int column = 1;

			    		// CUSTOMERID
			    		insertStatement.setString(column, customerID);

			    		// REFERENCE KEY
			    		insertStatement.setInt(++column, Integer.parseInt(refKey));

			    		// SPID Value
			    		insertStatement.setString(++column, params[1]);

			    		// REGIONID ID
			    		insertStatement.setNull(++column, java.sql.Types.NUMERIC);

			    		// NNSP
			    		insertStatement.setString(++column, params[10]);

			    		// ONSP
			    		insertStatement.setString(++column, params[11]);

			    		// LNPTYPE
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// PORTINGTN
			    		insertStatement.setString(++column, tn);

			    		// NNSP_DUEDATE
			    		Date nnspDueDate = DBTokenConsumerBase.parseDate(params[6]);
			    		
			    		if (nnspDueDate != null) {

			    			insertStatement.setTimestamp(
			    				++column,
			    				new Timestamp(nnspDueDate.getTime()));

			    		} else {

			    			insertStatement.setNull(++column, java.sql.Types.DATE);

			    		}
			    		
			    		// ONSP_DUEDATE
			    		Date onspDueDate = DBTokenConsumerBase.parseDate(params[8]);
			    		
			    		if (onspDueDate != null) {

			    			insertStatement.setTimestamp(
			    				++column,
			    				new Timestamp(onspDueDate.getTime()));

			    		} else {

			    			insertStatement.setNull(++column, java.sql.Types.DATE);

			    		}

			    		// SVID
			    		insertStatement.setString(++column, svid);

			    		// LRN
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// CLASSDPC
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// CLASSSSN
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// CNAMDPC
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// CNAMSSN
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// ISVMDPC
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// ISVMSSN
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// LIDBDPC
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// LIDBSSN
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// WSMSCDPC
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// WSMSCSSN
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// PORTINGTOORIGINAL
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// BILLINGID
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// ENDUSERLOCATIONTYPE
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// ENDUSERLOCATIONVALUE
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// AUTHORIZATIONFLAG
			    		insertStatement.setString(++column, params[9]);

			    		// CAUSECODE
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// CONFLICTDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// AUTHORIZATIONDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// CUSTOMERDISCONNECTDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// EFFECTIVERELEASEDATE 
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// STATUS
			    		insertStatement.setString(++column, SOAUtility.getStatus( params[14] ));

			    		// Last Response
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// Last Response Date
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// COMPLETEDDATE
		    			insertStatement.setNull(++column, java.sql.Types.DATE);

		    			// ACTIVATEDDATE
		    			insertStatement.setNull(++column, java.sql.Types.DATE);

		    			// Last Request Type
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// Last Request Date 
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// LASTMESSAGE		
			    		insertStatement.setString(++column, messageSubType );
			    		
			    		// OBJECTCREATIONDATE
			    		Date creationDate = DBTokenConsumerBase.parseDate(params[0]);
			    		if (creationDate != null) {

			    			insertStatement.setTimestamp(
			    				++column,
			    				new Timestamp(creationDate.getTime()));

			    		} else {

			    			insertStatement.setNull(++column, java.sql.Types.DATE);

			    		}

			    		// OBJECTMODIFIEDDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// PRECANCELLATIONSTATUS
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		// STATUSOLDDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);
			    		
			    		//	OLDSPCONFLICTRESOLUTIONDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		// NEWSPCONFLICTRESOLUTIONDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);
			    		
			    		// OLDSPCANCELETIONDATE 
			    		insertStatement.setNull(++column, java.sql.Types.DATE);
			    		
			    		// NEWSPCANCELETIONDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		//CANCELEDDATE
			    		insertStatement.setNull(++column, java.sql.Types.DATE);

			    		//BUSINESSTMERTYPE
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		//BUSINESSHOURS
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);

			    		//FAILEDSPFLAG
			    		insertStatement.setNull(++column, java.sql.Types.INTEGER);

			    		// Created By
			    		insertStatement.setString(++column, SOAConstants.SYSTEM_USER);

			    		// Created Date
			    		// use current time
			    		if (creationDate != null) {
			    			insertStatement.setTimestamp(
			    					++column,
			    					new Timestamp(creationDate.getTime()));
			    		}

			    		// SV Type
			    		insertStatement.setNull(++column, java.sql.Types.VARCHAR);
			    		
			    		//Alternative SPID
	    				insertStatement.setNull(++column, java.sql.Types.VARCHAR);
	    				
				          
				          if(insertStatement.executeUpdate()!=0){
				          
				        	  if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
				        		  Debug.log(Debug.MAPPING_STATUS,className + "["+ methodName
							                + "] The Message has been inserted into the" 
						              		+ " in " + SOAConstants.SV_TABLE +  " table");
				        	  }
				        		  
				          }else{
				        	  if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
				        		  
				        		  Debug.log(Debug.MAPPING_STATUS,className + "["+ methodName
										     + "] The Insertion has been failed for the SV Message into the" 
									       	 + " " + SOAConstants.SV_TABLE + " table");
				        	  }
				          }
				              
				          insertStatement.close();
			       } 	

		      }catch(SQLException sqlEx){
		          throw new FrameworkException(sqlEx.getMessage());
		      }
		      catch(Exception ex){
		          throw new FrameworkException(ex.getMessage());
		      }
		  }
		 
	 /**
	  * This will be used to update the Object tables are SOA_NBRPOOL_BLOCK and
	  * SOA_NPANXX for network data.
	  * 
	  * @param connection  returns an instance of a connection.
	  * @param lastMessage contains the last message.
	  * @param params	   contains the tokenized data.	
	  * @throws SQLException is thrown when any sql opreation is failed.
	  * @throws FrameworkException is thrown when any application failure is 
	  * 							  occurred.
	  */
	 public static void updateNetworkDataMessage(Connection connection,

	         String lastMessage,String notifyType,String[] params)
	 			throws SQLException,FrameworkException{
	     
	   PreparedStatement lastResponseSt =null;
	   
	   methodName="updateNetworkDataMessage";  
	   
	   String message=null;
	   
	   String objectTab=null;
	   
	   try{
	       
	       //  It updates the columns in SOA_NBRPOOL_BLOCK table when 
		   //  the notification type is
		   //  either NumberPoolBlockCreationNotification or   
	       //  NumberPoolBlockAttributeValueChangeNotification
	       if(notifyType.equalsIgnoreCase
		 	     (SOAConstants.NBR_POOLBLK_OBJECT_CREATION_NOTIFICATION)){
          
	           objectTab= SOAConstants.NBRPOOL_BLOCK_TABLE ;
	               
	           lastResponseSt = connection
	          .prepareStatement(SOAConstants.LASTRES_UPDATE_NBRPOOLBLK__QUERY);
	      
	           lastResponseSt.setString(1,lastMessage);
	          
	          Date lastResDate=TimeZoneUtil
	          .parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT,params[0]);
	          lastResponseSt.setTimestamp(2, new Timestamp(lastResDate.getTime()));

	          lastResponseSt.setString(3,params[1]);
	          
	          lastResponseSt.setString(4,params[7].substring(0,3));
	          
	          lastResponseSt.setString(5,params[7].substring(3,6));

	          lastResponseSt.setString(6,params[7].substring(6,7));
          
	          message="] The LastResponse and LastResponseDate columns have been" 
	          +" updated in " +  objectTab + " table";

       } else if(notifyType.equalsIgnoreCase(SOAConstants.NBR_POOLBLK_ATTR_VAL_CHANGE_NOTIFICATION))
       {
          
	           objectTab= SOAConstants.NBRPOOL_BLOCK_TABLE ;
	               
	           lastResponseSt = connection
	          .prepareStatement(SOAConstants.LASTRES_UPDATE_NBRPOOLBLK__QUERY);
	      
	           lastResponseSt.setString(1,lastMessage);
	          
	          Date lastResDate=TimeZoneUtil
	          .parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT,params[0]);
	          lastResponseSt.setTimestamp(2, new Timestamp(lastResDate.getTime()));

	          lastResponseSt.setString(3,params[1]);
	          
	          lastResponseSt.setString(4,params[6].substring(0,3));
	          
	          lastResponseSt.setString(5,params[6].substring(3,6));

	          lastResponseSt.setString(6,params[6].substring(6,7));
          
	          message="] The LastResponse and LastResponseDate columns have been" 
	          +" updated in " +  objectTab + " table";

       }
       //  It updates the columns in SOA_NBRPOOL_BLOCK table when 
	   //  the notification type is
	   //  NumberPoolBlockStatusAttributeValueChangeNotification    
      else if(notifyType.equalsIgnoreCase
		 	     
		       (SOAConstants.NBR_POOLBLK_STA_ATTR_VAL_CHANGE_NOTIFICATION)){
          
            objectTab= SOAConstants.NBRPOOL_BLOCK_TABLE;
          
          	lastResponseSt = connection
          	.prepareStatement(SOAConstants.LASTRES_UPDATE_NBRPOOLBLKSTA__QUERY);
      
          	lastResponseSt.setString(1,lastMessage);
          
          	Date lastResDate=TimeZoneUtil
          	.parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT,params[0]);
          	lastResponseSt.setTimestamp(2, new Timestamp(lastResDate.getTime()));
          
          	lastResponseSt.setString(3,params[1]);
          
          	lastResponseSt.setString(4,params[5]);
          	
          	Debug.log(8,"lastMessage "+lastMessage+" "+new Timestamp(lastResDate.getTime())+ " "+ params[1]+" "+params[5]);
          	message="] The  LastResponse and LastResponseDatecolumns have been" 
              +" updated in " +  objectTab + " table";
  
          
        }
      //  It updates the columns in SOA_NPANXX table when 
	  //  the notification type is
	  //  VersionNewNPA_NXXNotification    
      else if(notifyType.equalsIgnoreCase
		 	     
		       (SOAConstants.SUB_NEW_NPANXX_NOTIFICATION)){
       
          objectTab= SOAConstants.NPANXX_TABLE;
          
          lastResponseSt = connection
          .prepareStatement(SOAConstants.LASTRES_UPDATE_NPANXX__QUERY);
   
          lastResponseSt.setString(1,lastMessage);
       
          Date lastResDate=TimeZoneUtil
          .parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT,params[0]);
      
          lastResponseSt.setTimestamp(2, new Timestamp(lastResDate.getTime()));
       
          lastResponseSt.setString(3,params[5]);
       
       	  message= "] The  LastResponse and LastResponseDate columns have been" +
	             " updated in " + objectTab + " table";
         }
	      
	    if(lastResponseSt!= null && lastResponseSt.executeUpdate()!=0){
	       	   if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
	    		   Debug.log(Debug.MAPPING_STATUS,className + "["+ methodName 
		         	        + message);
	    	   }
	       	lastResponseSt.close();
	    }else{
	      
	    	   if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
	    		   
	    		   Debug.log(Debug.MAPPING_STATUS,className + "["+ methodName 
		         	        + " The updation has been failed for LastResponse and" +
		         	          " LastResponseDate columns " + objectTab + " table");
	    	   }
	    		   
	    }
	    
	       
	      }catch(SQLException sqlEx){
	          throw new FrameworkException(sqlEx.getMessage());
	      }
	      catch(Exception ex){
	          throw new FrameworkException(ex.getMessage());
	      }
	  }
	 
	 /**
	  * This will be used to update an Object table SOA_AUDIT for audit 
	  * notifications those are received from NPAC.  
	  * 
	  * @param connection  returns an instance of a connection.
	  * @param lastMessage contains the last message.
	  * @param notifyType  contains the notification type. 
	  * @param params	   contains the parameterized data.	
	  * @param portingTN   contains the poriting tn.
	  * @throws SQLException is thrown when any sql opreation is failed.
	  * @throws FrameworkException is thrown when any application failure is 
	  * 							  occurred.
	  */
	 
	 public static void updateAuditMessage(Connection connection,
	         String lastMessage,String notifyType,
	         String[] params,String portingTN) throws SQLException,
	          FrameworkException {
	  
	  methodName="updateAuditMessage";  
	     
	  PreparedStatement lastResponseSt =null;
	  
	  try{

	      //  It updates the columns in SOA_AUDIT table when 
		  //  the notification type is
		  //  either SubscriptionAuditCreationNotification or
	      //  SubscriptionAuditDeletionNotification
	      
	  if(notifyType.equalsIgnoreCase
		 	     
		       (SOAConstants.SUB_AUDIT_OBJECT_CREATION_NOTIFICATION)
		       || notifyType.equalsIgnoreCase
		 	     
		       (SOAConstants.SUB_AUDIT_OBJECT_DELETION_NOTIFICATION)){
          
	      lastResponseSt = connection
          .prepareStatement(SOAConstants.AUDIT_LASTRES_UPDATE_OBJE_CRE_QUERY);
	      
	      lastResponseSt.setString(1,lastMessage);
	      
	      Date lastResDate=TimeZoneUtil
          .parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT,params[0]);
	      
	      lastResponseSt.setTimestamp(2, new Timestamp(lastResDate.getTime())); 
	      
	      lastResponseSt.setString(3,params[1]);
	      
	      lastResponseSt.setString(4,params[5]);
      }
	  //  It updates the columns in SOA_AUDIT table when 
	  //  the notification type is
	  //  SubscriptionAuditDiscrepancyRptNotification 

	  else if(notifyType.equalsIgnoreCase
		 	     
		       (SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION)){
	      
	      lastResponseSt = connection
          .prepareStatement(SOAConstants.AUDIT_LASTRES_UPDATE_DESRPT_QUERY);
	      
	      lastResponseSt.setString(1,lastMessage);
	      
	      Date lastResDate=TimeZoneUtil
          .parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT,params[0]);
	      
	      lastResponseSt.setTimestamp(2, new Timestamp(lastResDate.getTime())); 
	      
	      lastResponseSt.setString(3,portingTN);
	      
	      lastResponseSt.setString(4,params[1]);
      }

	  //  It updates the columns in SOA_AUDIT table when 
	  //  the notification type is
	  //  SubscriptionAuditResultsNotification 

      else if(notifyType.equalsIgnoreCase
		 	     
		       (SOAConstants.SUB_AUDIT_RESULTS_NOTIFICATION)){
          
          lastResponseSt = connection
          .prepareStatement(SOAConstants.AUDIT_LASTRES_UPDATE_RSLTS_QUERY);
	      
	      lastResponseSt.setString(1,lastMessage);
	      
	      Date lastResDate=TimeZoneUtil
          .parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT,params[0]);
	      
	      lastResponseSt.setTimestamp(2, new Timestamp(lastResDate.getTime())); 
	      
	      lastResponseSt.setString(3,params[1]);
      }

	  if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
		  
		  Debug.log(Debug.MAPPING_STATUS, className + "[" + methodName +
		           "] The Parameters has been set to update the " +
		           " LastResponse column into " + SOAConstants.SOA_AUDIT + "table by" +
		           " parameters LASTRESPONSE="+ lastMessage + ",SPID=" + params[1]+ 
		           ",AUDITID=" + params[5]);
	  }
	  
	  if(lastResponseSt != null){
		  lastResponseSt.executeUpdate();
	  
		  lastResponseSt.close();
	  }
	  
	  if ( Debug.isLevelEnabled( Debug.MAPPING_STATUS )){
		  
		  Debug.log(Debug.MAPPING_STATUS,className + "["+ methodName + "]"
	    	        + " The LastResponse and LastResponseDate columns have been" +
		             " updated in " + SOAConstants.SOA_AUDIT + "table");
	  }
	  
 	
	}catch(SQLException sqlEx){
	    throw new FrameworkException(sqlEx.getMessage());
	}
	catch(Exception ex){
	    throw new FrameworkException(ex.getMessage());
	}
  }
	
	 /**
	 * This method will return String value of SPTYPE
	 * 
	 * @param statusStr
	 *            String
	 * @return Status as String
	 */
	public static String getSPType(String statusStr)

	{

		int statusInt = -1;

		if ( statusStr != null )
		{
			try {

				statusInt = Integer.parseInt(statusStr);

			} catch (NumberFormatException exe) {

				Debug.error(" Could not parse :" + statusStr + "\n" + exe);

			}
		}

		String status = "";

		switch (statusInt) {
		case 0:

			status = SOAConstants.SPTYPE_WIRELINE;

			break;

		case 1:

			status = SOAConstants.SPTYPE_WIRELESS;

			break;

		case 2:

			status = SOAConstants.SPTYPE_NONCARRIER;

			break;
			
		case 3:

			status = SOAConstants.SPTYPE_CLASS1_VOIP;

			break;

		default:

			status = SOAConstants.SPTYPE_OTHERCARRIER;

			break;

		}

		return status;

	} 
	
	/**
	 * This method will return String value of SvTYPE
	 * 
	 * @param statusStr
	 *            String
	 * @return Status as String
	 */ 
	
	 public static String getSVType(String statusStr)

	 {

		int statusInt = -1;

		try {

			statusInt = Integer.parseInt(statusStr);

		} catch (NumberFormatException exe) {

			Debug.error(" Could not parse :" + statusStr + "\n" + exe);

		}

		String status = "";

		switch (statusInt) {
		case 0:

			status = SOAConstants.SVTYPE_WIRELINE;

			break;

		case 1:

			status = SOAConstants.SVTYPE_WIRELESS;

			break;

		case 2:

			status = SOAConstants.SVTYPE_CLASS2_VOIP;

			break;
		case 3:

			status = SOAConstants.SVTYPE_VOWIFI;

			break;
		
		case 4:

			status = SOAConstants.SVTYPE_PRE_PAID_WIRELESS;

			break;
		
		case 5:

			status = SOAConstants.SVTYPE_CLASS1_VOIP;

			break;
		
		case 6:

			status = SOAConstants.SVTYPE_SVTYPE6;

			break;

		}

		return status;

	} 

	
	public static XMLMessageGenerator successSyncResponse(String requestId, String requestStatus,
										String actionValue, String regionId,
											String dateTimeSent,String tn)throws MessageException ,
											ProcessingException
	{

		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
			
			Debug.log(Debug.MSG_STATUS, "GenerateSyncResponse: Processing the successSyncResponse()");
		}
					

		// Generate an XML response
		XMLMessageGenerator response = new XMLMessageGenerator(SOAConstants.RESPONSE_ROOT);

		response.setValue( SOAConstants.DATESENT, dateTimeSent );

		if (regionId != null){

			int region = -1;

			try 
			{			
				region = Integer.parseInt(regionId);
			}
			catch(NumberFormatException nbrfex){

				throw new MessageException("Invalid Region ID: " + nbrfex);

			}
			
			response.setValue( SOAConstants.REGIONID, 
				StringUtils.padNumber( region, 
								SOAConstants.REGION_ID_LEN, true, '0' ) );
			
		}		

		response.setValue( SOAConstants.ACTION_VALUE_PATH, actionValue );		
		
		if (requestStatus != null)
		{
			response.setValue( SOAConstants.REQUEST_STATUS_PATH, requestStatus );	
		}	
		
		
		if (requestId != null)
		{
			if(tn != null){
				
				response.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.Tn", tn);
			}
			response.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.RequestId", requestId);
			//response.setValue( SOAConstants.REQUEST_ID_PATH, requestId );
		}
		
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )) {

			Debug.log(
				Debug.MSG_STATUS,
				"GenerateSyncResponse: Generated response \n"
					+ response.generate());
		}

		return response;
		
	}


	public static XMLMessageGenerator failPFSupported(String errorValue, String requestId, String requestStatus,
										String actionValue, String regionId,
											String dateTimeSent, String tn) throws 
									MessageException,ProcessingException
		{
		
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
		
			Debug.log(Debug.MSG_STATUS, "GenerateSyncResponse: " +
			"Processing the failPFSupported() started.");
		}					

		String ruleId = null;

		String ruleMsg = null;

		String ruleContext = null;

		String ruleContextValue = null;

		// Generate an XML response
		XMLMessageGenerator response = new XMLMessageGenerator(SOAConstants.RESPONSE_ROOT);

		response.setValue( SOAConstants.DATESENT, dateTimeSent );

		if (regionId != null){

			int region = -1;

			try 
			{			
				region = Integer.parseInt(regionId);
			}
			catch(NumberFormatException nbrfex){

				throw new MessageException("Invalid Region ID: " + nbrfex);

			}
			
			response.setValue( SOAConstants.REGIONID, 
				StringUtils.padNumber( region, 
								SOAConstants.REGION_ID_LEN, true, '0' ) );
			
		}		

		response.setValue( SOAConstants.ACTION_VALUE_PATH, actionValue );		
		
		if (requestStatus != null)
		{
			response.setValue( SOAConstants.REQUEST_STATUS_PATH,
			 requestStatus );	
		}
		
		if (requestId != null)
		{
			if(tn != null){
				
				response.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.Tn", tn);
			}
			response.setValue(SOAConstants.SYNCHRONUS_RESPONSE_PATH + ".TnRequestIdList.TnRequestId.RequestId", requestId);
			//response.setValue( SOAConstants.REQUEST_ID_PATH, requestId );
		}

		XMLMessageParser domMsg = new XMLMessageParser(errorValue);
	
		Document outMsg = domMsg.getDocument();
		
		Element firstRoot = outMsg.getDocumentElement();

		//This contains the elements are containing with ruleid 
		NodeList ruleIdList = firstRoot.getElementsByTagName("RULE_ID"); 

		NodeList ruleMsgList = firstRoot.getElementsByTagName("MESSAGE"); 

		NodeList ruleContextList = 
								firstRoot.getElementsByTagName("CONTEXT"); 

		NodeList ruleContextValueList = 
						firstRoot.getElementsByTagName("CONTEXT_VALUE"); 

		//for getting the ruleid 
		for(int msgCount = 0 ; msgCount < ruleIdList.getLength() ; msgCount++)
		 {
			 Node ruleIdNode = ruleIdList.item(msgCount);

			 ruleId = XMLMessageBase.getNodeValue(ruleIdNode);

			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				 
				 Debug.log(Debug.MSG_STATUS, "GenerateSyncResponse: " +
							"extracted the ruleid: " +ruleId);
			 }

			 response.setValue( SOAConstants.RULE_ERROR_PATH +"("
			 +msgCount+")"+"."+"RuleId" , ruleId );

			 Node ruleMsgNode = ruleMsgList.item(msgCount);

			 ruleMsg = XMLMessageBase.getNodeValue(ruleMsgNode);

			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				 
				 Debug.log(Debug.MSG_STATUS, "GenerateSyncResponse: " +
							"extracted the ruleMsg: " +ruleMsg);
			 }

			 response.setValue( SOAConstants.RULE_ERROR_PATH +"("
			 +msgCount+")"+"."+"RuleMessage", ruleMsg );

			 Node ruleContextNode = ruleContextList.item(msgCount);

			 ruleContext = XMLMessageBase.getNodeValue(ruleContextNode);

			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				 
				 Debug.log(Debug.MSG_STATUS, "GenerateSyncResponse: " +
							"extracted the ruleContext: " +ruleContext);
			 }

			 response.setValue( SOAConstants.RULE_ERROR_PATH +"("
			 +msgCount+")"+"."+"RuleContext" , ruleContext );

			 Node ruleContextValueNode = ruleContextValueList.item(msgCount);

			 ruleContextValue = 
			 XMLMessageBase.getNodeValue(ruleContextValueNode);

			 if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
				 
				 Debug.log(Debug.MSG_STATUS, "GenerateSyncResponse: " +
							"extracted the ruleContextValue: " +ruleContextValue);

			 }
				 
			 response.setValue( SOAConstants.RULE_ERROR_PATH +"("
			 +msgCount+")"+"."+"RuleContextValue" ,ruleContextValue );
			 
		 }
				
		if ( Debug.isLevelEnabled( Debug.MSG_STATUS )) {

			Debug.log(
				Debug.MSG_STATUS,
				"GenerateSyncResponse: Generated response \n"
					+ response.generate());
		}

		return response;
				
	}
        
        /**
         * This method formats the Telephone number specific to the reuirement mention FS 5.0 Section 6.2
         * @param   tn as String
         *
         * @return  return the formated Tn as String
         */
        
        public static String formatTn(String tn) {
            StringBuffer resultingTn = new StringBuffer();
            char tnArray[] = tn.toCharArray();
            
            //check whether tn has wildcard
            if(tn.indexOf("%") != -1 || tn.indexOf("_") != -1) {
                //Tn value having wildcard entry
                for (int i = 0; i < tnArray.length; i++) {
                    if(isNumeric(tnArray[i]) || isWildcard(tnArray[i]))
                        resultingTn.append(tnArray[i]);
                }
                
                String tempTn = resultingTn.toString();           //store the resulting into temporary variable
                //check if the first four digits are numeric
                if(tempTn.length()>3 && tempTn.substring(0,4).matches("\\d*")) {
                    //insert - at offset 3
                    resultingTn.insert(3, '-');
                }
                
                //check if the last five digits are numeric
                if(tempTn.length()>4 && tempTn.substring(tempTn.length()-5).matches("\\d*")) {
                    //insert - at offset before the last four tn
                    resultingTn.insert(resultingTn.length()-4, '-');
                }
                return resultingTn.toString();       //return the resulting string with wildcard
                
            } else {
                //Tn value does not have wildcard entry
                for (int i = 0; i < tnArray.length; i++) {
                    if(isNumeric(tnArray[i]))
                        resultingTn.append(tnArray[i]);
                }
                if(resultingTn.length() == 10) {
                    //Insert - at the offset 3, 7
                    return resultingTn.insert(3, '-').insert(7, '-').toString(); //return the resulting string without card
                }
            }
            return tn;       //return the original tn
        }
        
        /**
         * This method return true if the parameter is numeric otherwise false
         * @param   n as int
         *
         * @return  return true if n is numeric otheriwse false
         */
        public static boolean isNumeric(int n) {
            return (n >='0' && n <='9');
        }
        
        /**
         * This method return true if the parameter is a wild card character as either % or _ otherwise false
         * @param   c as char
         *
         * @return  return true if c is a wild card otherwise false
         */
        public static boolean isWildcard(char c) {
            return (c == '%' || c == '_');
        }
        
        /**
         * This method formats the Telephone number specific to the requirement"TN Text Box in Search Screen requirement" mention in design doc 5.4
         * @param   tn as String
         *
         * @return  return the formated Tn as String
         */
        
        public static String getSvTnList(String tn){		 

   		ArrayList tnList = new ArrayList();
   		 
   		StringBuffer resultingTn = new StringBuffer();
   		try
  		{ 
	        if (tn != null && !tn.equals(""))
	   		 {		 	 
	        	 String[] tnTokens = null;
	
	   			 String startTnValue = null;
	
	   			 String endTnValue = null;
	   		 
	   			 tnTokens = tn.split(";");
	   			 
	   			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
	   				
	   				Debug.log(Debug.MSG_STATUS,"Length of tnTokens: " +tnTokens.length);
	   			}
	   			 
	   			 for (int i=0; i<tnTokens.length; i++)
	   			 {							
	   				if (tnTokens[i].length() > 12)
	   				{
	   					startTnValue = tnTokens[i].substring(0,12).toString();
	
	   					endTnValue = tnTokens[i].substring(13).toString();	
	   					
	   					tnList.addAll(getSvRangeTnList(startTnValue, endTnValue));
	   				}
	   				else
	   				{
	   					tnList.add(tnTokens[i].toString());					
	   				}
	   			 }
	   		 }
	        if(tnList != null)
   			{
   				Collections.sort(tnList);
   				for(int i = tnList.size()-1; i >= 0; i--)
      			 {
   					if(i == 0){
   						resultingTn.append(tnList.get(i));
   					}
   					else
   					{   						
   						resultingTn.append(tnList.get(i)).append('|');
   					}
      			 }
   				   
   			}
   		 }
   		 catch (Exception e)
   		 {
   			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
   				
   				Debug.log(Debug.MSG_STATUS,"resultingTn: " +e.toString());
   			}
   				
   		 }				 
   		 if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
   			
   			 Debug.log(Debug.MSG_STATUS,"resultingTn exception: " +resultingTn.toString());
   		 }
   			  			
   		 return resultingTn.toString();

   	}
        
        public static ArrayList getSvRangeTnList(String startTN, String endStation){

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

		 public static String getSvQueryList(String tn){		 

       		ArrayList tnList = new ArrayList();
       		 
       		StringBuffer resultingTn = new StringBuffer();
       		try
      		{ 
    	        if (tn != null && !tn.equals(""))
    	   		 {		 	 
    	        	 String startTnValue = null;
    	
    	   			 String endTnValue = null;
    	   		 
    	   			 String[] tnTokens = tn.split(";");
    	   			 
    	   			if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
    	   			
    	   				Debug.log(Debug.MSG_STATUS,"Length of tnTokens: " +tnTokens.length);
    	   			}
    	   			 
    	   			 
    	   			 for (int i=0; i<tnTokens.length; i++)
    	   			 {							
    	   				if (tnTokens[i].length() > 12)
    	   				{
    	   					startTnValue = tnTokens[i].substring(0,12).toString();
    	
    	   					endTnValue = tnTokens[i].substring(13).toString();	
    	   					
    	   					tnList.addAll(getSvRangeTnList(startTnValue, endTnValue));
    	   				}    	   				
    	   				else
    	   				{
    	   					tnList.add(tnTokens[i].toString());					
    	   				}
    	   			 }
    	   			 if( tnList != null )
    	       			{
    	       				Collections.sort( tnList );
    	       				for( int i = tnList.size()-1; i >= 0; i-- )
    	          			 {   	       					
    	       					
    	       					if( i == 0 )
    	       					{
    	       						if(tnList.get(i).toString().contains("%"))
    	       						{
    	       							resultingTn.append(tnList.get(i));
    	       						}
    	       						else
    	       						{
    	       							resultingTn.append( tnList.get(i) + "%" + "|" + tnList.get(i).toString().substring(0, 8)+ "_" + "_" + "_" + "_" + tnList.get(i).toString().substring(7, 12));
    	       						}
    	       					}
    	       					else
    	       					{   
    	       						if(tnList.get(i).toString().contains("%"))
    	       						{
    	       							resultingTn.append(tnList.get(i));
    	       						}
    	       						else
    	       						{
    	       							resultingTn.append( tnList.get(i) + "%" + "|" + tnList.get(i).toString().substring(0, 8)+ "_" + "_" + "_" + "_" + tnList.get(i).toString().substring(7, 12));
    	       						}
    	       						resultingTn.append( "|" );
    	       					}
    	          			 }
    	       				   
    	       			}
    	   			 
    	   		 }    	        
       		 }
       		 catch (Exception e)
       		 {
       			Debug.log(Debug.MSG_STATUS,"resultingTn exception: " +e.toString());
       			
       		 }				 
       		Debug.log(Debug.MSG_STATUS,"tnList: " +resultingTn.toString());
       		   			
       		 return resultingTn.toString();

       	}
		 
		 
		 /**
			 * Returns a version of the input where all contiguous whitespace characters are replaced with a single
			 * space. Line terminators are treated like whitespace.
			 * @param inputStr
			 * @return
			 */
			public static CharSequence removeWhitespace(CharSequence inputStr)throws FrameworkException {
		        String patternStr = "\\s+";
		        String replaceStr = " ";
		        try{
		        	Pattern pattern = Pattern.compile(patternStr);
		        	Matcher matcher = pattern.matcher(inputStr);
		        	return matcher.replaceAll(replaceStr);
		        	
		        }
		        catch(Exception e){
		        	if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
		        		Debug.log(Debug.ALL_ERRORS,"Error: Exception occurs while removing white spaces.");
		        	
		        	throw new FrameworkException("Exception occurs while removing white spaces.");
		        }
		    } 
		 /**
		     * Throw the given exception as a SOAP fault exception
		     *
		     * @param  faultType  The type of exception that occurred, as defined in the SOAPConstants.
		     * @param errorMessage The error message
		     *
		     * @exception  org.apache.axis.AxisFault with the faultCode set to faultType and the faultString
		     * containing the faultCode too.
		     */
		    public static void throwSOAPFaultException ( String faultType, String errorMessage )
		        throws org.apache.axis.AxisFault
		    {
		        throw new org.apache.axis.AxisFault ( faultType,errorMessage, null, null );
		        
		    }
		    
		    /**
		     *  This function to check international telephone number.
		     * @param strPhone
		     * @return
		     */
		    public static boolean  checkInternationalPhone(String strPhone){
			    // Declaring required variables
				// non-digit characters which are allowed in phone numbers
				String validWorldPhoneChars = "-";
				// Minimum no of digits in an international phone no.
				int minDigitsInIPhoneNumber = 10;
				int maxDigitsInIPhoneNumber = 14;
		       String s = stripCharsInBag(strPhone,validWorldPhoneChars);
		       return (isInteger(s) && (s.length() == minDigitsInIPhoneNumber || s.length() == maxDigitsInIPhoneNumber));
		   }
		 
		    /** This function to check valid NpaNxx value
		     *  
		     * @param strPhone
		     * @return
		     */
		   public static boolean  checkNpaNxx(String strPhone)
		   {
				   boolean result = true;
				   String individualTn = strPhone.toString();
				   StringTokenizer npaNxx = new StringTokenizer(individualTn, "-");
				   String npa="";
				   int totalToken = npaNxx.countTokens();
				   for(int i=0; i < totalToken; i++){
			          npa = npaNxx.nextElement().toString();
					  if(( i == 0 || i == 1) && ( npa.length() < 3 || npa.length() >3 )){
					  result = false;
					  break; 
					  }
					  if(( i == 2 || i == 3) && ( npa.length() < 4 || npa.length() >4 )){
					  result = false;
					  break; 
					  }
				   }
				   return result; 
			}
			  
		   /**
		    *  This function checks "-" in tel no.
		    * @param s
		    * @param bag
		    * @return
		    */
		   public static String  stripCharsInBag(String s, String bag)
		   {
			       String returnString = "";
			      // Search through string's characters one by one.
			      // If character is not in bag, append to returnString.
			      for (int i = 0; i < s.length(); i++)
			       {   
			        // Check that current character isn't whitespace.
			       	String c =""+s.charAt(i);
			        if (bag.indexOf(c) == -1) returnString += c;
			       }
			     return returnString;
			}
	      
		   /** This function checks integer value in input string.
		    * 
		    * @param s
		    * @return
		    */
		  public static boolean isInteger(String s)
		  { 
			    for (int i = 0; i < s.length(); i++)
			    {   
						// Check that current character is number.
						char  c = s.charAt(i);
	
						if ((c < '0') || (c > '9')) return false;
			    }
				// All characters are numbers.
				return true;
		}
		 
		 //This is a utility method to check valid Telephone Number .
		 public static boolean validateTelephoneNo(String val)
		 {
			try{
				if (val == null){
					 return false;
				 } 
				if(val.length() == 12 || val.length() == 17){
					  if (checkNpaNxx(val)==false){ 
						  return false; 
					  }
		              if (checkInternationalPhone(val)==false){ 
						  return false; 
					  }
				}else{
					  return false; 
				}	
			  }catch(Exception badFormat){
		            return false;
			   }	 
			return true;
		 }
}
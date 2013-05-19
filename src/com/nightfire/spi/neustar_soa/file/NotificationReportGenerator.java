/**
 * The class is used to generate the Report which will have
 * failed record(s) i.e. record not inserted in database
 * for Bulk Data Download.  
 * 
 * @author D.Subbarao.
 * @version 3.3
 * 
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see com.nightfire.spi.neustar_soa.utils.StringUtils
 */
/** 
 History
 ---------------------
 Rev#		Modified By 	Date			Reason
 -----       -----------     ----------		--------------------------
 1.			Subbarao		08/30/2005		Created
 2.         Subbarao 		09/02/2005		Added new method and Hardcoded 
 											report titles have been changed 
 											with constants and taken some
 											conditions to verify the parameters
 											are null or not.
 3.		    Subbarao		09/05/2005		Modified. 											
 4.			Subbarao		09/12/2005 		Modified and Added new method is
 											commonFormatElements.
 5.			Subbarao		10/10/2005		Modified Added initSpid element to 
 											constructor.
 6.			Subbarao		10/20/2005		Modified Constants.
 7.			Subbarao		10/25/2005		Modified exceptions.
 8.			Subbarao		10/28/2005		Modified.	
 9.			Subbarao		11/24/2005		Modified.
 10.		Subbarao		11/30/2005		Modified addBody method.
 */

package com.nightfire.spi.neustar_soa.file;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.neustar_soa.file.ReportGenerator;

public class NotificationReportGenerator extends ReportGenerator {
    
    private String[] headerData = null;

    private static ArrayList failedList=null; 
    
    private String notificationType=null;
    /**
     * Constructs a Report Generator that will use the given fileName and
     * fileType.
     * 
     * @param fileName contains a file name.
     * @param fileType contains the notification type of a file.
     *  
     */
    public NotificationReportGenerator(String fileName)
    {
         super(fileName, null,null,null);
         
         failedList=new ArrayList();
    }
    
    /**
     * This function cteates the report with log extension
     * 
     * @throws FrameworkException
     */
    public void generateReport() throws FrameworkException {
        super.generateReport();
    }
     
    /**
     * This function used to add first header part to the report
     *  
     */
    public void titleHeader() {
        
        StringBuffer header = new StringBuffer();
        
        // This block will generate the first header onto a report for the
        // failed records of NPANXX related notifications.
        if (fileType.equalsIgnoreCase(SOAConstants.SUB_NEW_NPANXX_NOTIFICATION)) {
            headerData = new String[] { "                      NPANXX Log",
                    newLine, "                      --------------------",
                    newLine, newLine, "NEW NPANXX file	:   " + file.getName(),
                    newLine,"Notification Type 	:   " + notificationType,newLine};
        }
        // This block will generate the first header onto a report for the
        // failed records of SubscriptionVersion related notifications.
        else if (fileType
                .equalsIgnoreCase(SOAConstants.SV_VERSION_CANCELLATION_ACK_REQUEST_NOTIFICATION)
                || fileType
                .equalsIgnoreCase(SOAConstants.SV_DONORSP_CUSTOMER_DISCONNECTDATE_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.SV_NEWSP_CREATE_REQUEST_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.SV_OLDSP_CONCURR_REQUEST_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION)
                || fileType
                .equalsIgnoreCase(SOAConstants.VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION)
                || fileType
                .equalsIgnoreCase(SOAConstants.VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION)){
            
            headerData = new String[] {
                    "                      SV BDD Import Log", newLine,
                    "                      -----------------", newLine,
                    newLine, "SV BDD Data File 	   :   " + file.getName(),
                    newLine,"Notification Type 	:   " + notificationType,newLine};
        }
        // This block will generate the first header onto a report for the
        // failed records of NumberPoolBlock related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.NBR_POOLBLK_OBJECT_CREATION_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.NBR_POOLBLK_ATTR_VAL_CHANGE_NOTIFICATION)
                || fileType
                .equalsIgnoreCase(SOAConstants.NBR_POOLBLK_STA_ATTR_VAL_CHANGE_NOTIFICATION)) {
            
            headerData = new String[] {
                    "                      NBR_POOLBLK" + " Import Log",
                    newLine, "                      ------------------",
                    newLine, newLine,
                    "NBR_POOLBLK Data File 	   :   " + file.getName(),
                    newLine,"Notification Type 	:   " + notificationType,newLine};
        }
        // This block will generate the first header onto a report for the
        // failed records of Sub Audit related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_RESULTS_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_OBJECT_CREATION_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_OBJECT_DELETION_NOTIFICATION))
            
        {
            headerData = new String[] {
                    "                      AUDIT BDD Import Log", newLine,
                    "                      -------------------", newLine,
                    newLine, "AUDIT BDD Data File	:   " + file.getName(),
                    newLine,"Notification Type 	:   " + notificationType,newLine};
        }
        // This block will generate the first header onto a report for the
        // failed records of NPAC operations related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.VERSION_OBJECT_CREATION_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.LNP_NPAC_SMS_OPER_INFO)) {
            
            headerData = new String[] {
                    "                      lnp NPAC-SMS BDD " + "Import Log",
                    newLine, "                      -------------------",
                    newLine, newLine,
                    "lnp NPAC-SMS BDD Data File	:   " + file.getName(),
                    newLine,"Notification Type 	:   " + notificationType,newLine};
        }
        prepareHeaders(headerData, header);
        secondHeader(header);
        
    }
    
    /**
     * This function used to add second header part to the report
     * 
     * @param header is a StringBuffer object retains the headers and lines.
     *  
     */
    private void secondHeader(StringBuffer header) {
        
        // This block will generate the second header onto a report for the
        // failed records of NPANXX related notifications.
        
        if (fileType.equalsIgnoreCase(SOAConstants.SUB_NEW_NPANXX_NOTIFICATION)) {
            commonFormatElements(header,"List of NPANXX failed to import: ");
            headerData = new String[] {
                    StringUtils.padString(SOAConstants.SPID_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.NPANXXID_COL,31, false,
                    ' '),
                    StringUtils.padString(SOAConstants.NPA_COL, 6, false, ' '),
                    StringUtils.padString(SOAConstants.NXX_COL, 6, false, ' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.NPANXX_RECORD, newLine,
                    "----    --------                      ---  ---    ",
                    StringUtils.padString("------", 50, false, ' '),
                    "-------------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of NumberPoolBlock related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.NBR_POOLBLK_OBJECT_CREATION_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.NBR_POOLBLK_ATTR_VAL_CHANGE_NOTIFICATION)) {
            
            commonFormatElements(header,"List of NBRPoolBlocks failed to import:");
            headerData = new String[] {
                    StringUtils.padString(SOAConstants.SPID_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.NPBID_COL, 31, false,
                    ' '),
                    StringUtils.padString(SOAConstants.NPA_COL, 6, false, ' '),
                    StringUtils.padString(SOAConstants.NXX_COL, 6, false, ' '),
                    StringUtils
                    .padString(SOAConstants.DASHX_COL, 6, false, ' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.NBR_POOLBLK_RECORD, newLine,
                    "----    -----                         ---   ---   ----- ",
                    StringUtils.padString("------", 50, false, ' '),
                    "-------------------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of SubscriptionVersion related notifications.
        else if (fileType
                .equalsIgnoreCase(SOAConstants.SV_VERSION_CANCELLATION_ACK_REQUEST_NOTIFICATION)
                || fileType
                .equalsIgnoreCase(SOAConstants.SV_DONORSP_CUSTOMER_DISCONNECTDATE_NOTIFICATION)) {
            
	            commonFormatElements(header,"List of Subscription Versions " +
	    		"failed to import: ");
	            
	            headerData = new String[] {
	            StringUtils.padString(SOAConstants.SPID_COL,7, false, ' '),
	            StringUtils.padString(SOAConstants.PORTINGTN_COL, 20,
	             false, ' '),
	            StringUtils.padString(SOAConstants.STATUS_COL,25, false,' '),
	            StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
	            SOAConstants.SV_RECORD,newLine,
	            "----   ---------           --------     ",
	            StringUtils.padString("            ------", 50, false, ' '),
	            "            ---------", newLine };
        }
        else if( fileType
                .equalsIgnoreCase(SOAConstants.SV_NEWSP_CREATE_REQUEST_NOTIFICATION)
                || fileType
                .equalsIgnoreCase(SOAConstants.VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION)) {
            
            commonFormatElements(header,"List of Subscription Versions " +
            		"failed to import: ");
            headerData = new String[] {
                    StringUtils
                    .padString(SOAConstants.SPID_COL,7, false, ' '),
                    StringUtils.padString(SOAConstants.PORTINGTN_COL, 20,
                     false, ' '),
                    StringUtils.padString(SOAConstants.NNSP_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.ONSP_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.STATUS_COL,25, false,' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.SV_RECORD,newLine,
                    "----   ---------           ----   ----   "
                    + "------             ",
                    StringUtils.padString("      ------", 50, false, ' '),
                    "        ---------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of SubscriptionVersion related notifications.
        else if (fileType
                .equalsIgnoreCase(SOAConstants.VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.SV_OLDSP_CONCURR_REQUEST_NOTIFICATION)) {
            
            commonFormatElements(header,"List of Subscription Versions " +
    		"failed to import: ");
            headerData = new String[] {
                    StringUtils
                    .padString(SOAConstants.SPID_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.PORTINGTN_COL, 20,
                    false, ' '),
                    StringUtils.padString(SOAConstants.NNSP_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.STATUS_COL,25, false,' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.SV_RECORD,newLine,
                    "----   ---------          ----    "
                    + "------             ",
                    StringUtils.padString("      ------", 50, false, ' '),
                    "      ---------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of NumberPoolBlock related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.NBR_POOLBLK_STA_ATTR_VAL_CHANGE_NOTIFICATION)) {
            
            commonFormatElements(header,"List of numberPoolBlockStatus " +
            		"Attributes  failed to import: ");
            headerData = new String[] {
                    StringUtils
                    .padString(SOAConstants.SPID_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.NBRPOOLBLKID_COL, 31,
                    false, ' '),
                    StringUtils.padString(SOAConstants.STATUS_COL, 53, false,' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.NBR_POOLBLK_RECORD, newLine, "----   ",
                    StringUtils.padString("-----------", 31, false, ' '),
                    StringUtils.padString("------", 53, false, ' '),
                    StringUtils.padString("------", 50, false, ' '),
                    "----------------------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of Sub Audit related notifications.
        
        else if (fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION)) {
            
            commonFormatElements(header,"List of AUDIT DISCREPANCY REPORT " +
            		"to import: "); 
            headerData = new String[] {
                    StringUtils
                    .padString(SOAConstants.SPID_COL, 7, false, ' '),
                    StringUtils.padString("AuditDiscrepancyTN", 20, false, ' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.SUB_AUDIT_RECORD, newLine,
                    "----   ",
                    StringUtils.padString("--------------",20, false, ' '),
                    StringUtils.padString("------", 50, false, ' '),
                    "----------------------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of Sub Audit related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_RESULTS_NOTIFICATION)) {
            
            commonFormatElements(header,"List of Subscription AUDIT failed" +
    		" to import: ");
            headerData = new String[] {
                    StringUtils
                    .padString(SOAConstants.SPID_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.SUB_AUDIT_RECORD, newLine,
                    "----   ",
                    StringUtils.padString("------", 50, false, ' '),
                    "---------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of Sub Audit related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_OBJECT_CREATION_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_OBJECT_DELETION_NOTIFICATION)) {

            commonFormatElements(header,"List of Subscription AUDIT failed" +
    		" to import: ");
            headerData = new String[] {
                    StringUtils
                    .padString(SOAConstants.SPID_COL,7, false, ' '),
                    StringUtils.padString(SOAConstants.AUDITID_COL,31, false,
                    ' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.SUB_AUDIT_RECORD, newLine,
                    "----   ",
                    StringUtils.padString("---------",31, false, ' '),
                    StringUtils.padString("------", 50, false, ' '),
                    "-----------------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of Npac Operations related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.VERSION_OBJECT_CREATION_NOTIFICATION)
                || fileType.equalsIgnoreCase(SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION)) {
            
            commonFormatElements(header,"List of NPAC Operations failed " +
            		"to import: ");
            headerData = new String[] {
                    StringUtils.padString(SOAConstants.SPID_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.PORTINGTN_COL, 17,
                            false, ' '),
                    //StringUtils.padString(SOAConstants.NNSP_COL, 7, false, ' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.NPAC_OPER_INO_RECORD, newLine, "----    ",
                    StringUtils.padString("----------", 17, false, ' '),
                    //StringUtils.padString("----", 7, false, ' '),
                    StringUtils.padString("------", 50, false, ' '),
                          "---------", newLine };
        }
        // This block will generate the second header onto a report for the
        // failed records of Npac Operations related notifications.
        else if (fileType.equalsIgnoreCase(SOAConstants.LNP_NPAC_SMS_OPER_INFO)) {
            
            commonFormatElements(header,"List of NPAC Operations failed " +
    		"to import: ");
            
            headerData = new String[] {
                    StringUtils.padString(SOAConstants.SPID_COL, 8, false, ' '),
                    StringUtils.padString(SOAConstants.REASON, 50, false, ' '),
                    SOAConstants.NPAC_OPER_INO_RECORD, newLine, newLine,
                    "----   ", StringUtils.padString("------", 50, false, ' '),
                    "--------------------------", newLine };
        }
        prepareHeaders(headerData, header);
        report.append(header);
    }
    
    /**
     * This function used to add body part to the report
     * 
     * @param list contains all failed records
     *  
     */
    public void addBody(List list) {
        
     StringBuffer content = null;
     
     String[] rowCol = null;
     
     String[] rowCol1 = null;
        
     try{ 

         if(list!=null)
        
         if (list.size() > 0) {
               Debug.log(0,"list : "+list.size());     
            for (Iterator iter = list.iterator(); iter.hasNext();) {

               rowCol = (String[]) iter.next();
               
               boolean found=false;
               
               if(failedList.size() != 0){
                   if(failedList.contains(rowCol[3]))
                   {
                	   Debug.log(0,"Row col : "+rowCol[3]);
                	   found=true;
                   }else {
                	   Debug.log(0,"Row col 1: "+rowCol[3]+" Jigar ");
                	   failedList.add(rowCol[3]);
                   }
               }else {
            	   
            	   failedList.add(rowCol[3]);
               }
               
               // This will return the notification type based on 
               //	either notification id  or both (noticationid,objectid).
               fileType=SOAUtility.returnNpacNotification
               				(Integer.parseInt(rowCol[3]),Integer.parseInt(rowCol[4]));
               
               notificationType=SOAUtility.npacResp;
               if(!found)
               {
                   // This is used generate header for a particular notification 
                   // before preparing the body with the failed records that belongs 
                   // to the same notification.

                   titleHeader();
               }
               
               content = new StringBuffer();
               
               if(failedList.contains(rowCol[3])) {
               
                   	int elementsCount = rowCol.length - 2;
                
               		String reason = rowCol[elementsCount];
                
                // This block will generate the body onto a report for the
                // failed records of VersionCancellationAcknowledgeRequest
                // Notification
                 
                if (fileType
                    .equalsIgnoreCase
                    (SOAConstants.SV_VERSION_CANCELLATION_ACK_REQUEST_NOTIFICATION)){
                    
                    if (elementsCount > 7) 
                        
                        processDataToSvBody(content, rowCol[1], rowCol[6],
                                null,null, SOAConstants.RECEIVED, reason,
                                rowCol[rowCol.length - 1]);
                     else
                        processDataToSvBody(content, rowCol[1], rowCol[5],
                                    null,null, SOAConstants.RECEIVED,
                                    reason, rowCol[rowCol.length - 1]);
                }
                // This block will generate the body onto a report for the
                // failed records of subscriptionVersionDonorSP-CustomerDis
                //connectDate
                else if (fileType
                        .equals
               (SOAConstants.SV_DONORSP_CUSTOMER_DISCONNECTDATE_NOTIFICATION)) {
                    
                    if (elementsCount > 9)
                        processDataToSvBody(content, rowCol[1], rowCol[8],
                                rowCol[1],"-", SOAConstants.RECEIVED, reason,
                                rowCol[rowCol.length - 1]);
                    else 
                        processDataToSvBody(content, rowCol[1], rowCol[7],
                                    rowCol[1],"-", SOAConstants.RECEIVED,
                                    reason, rowCol[rowCol.length - 1]);
                }
                // This block will generate the body onto a report for the
                // failed records of VersionNewSP_CreateRequestNotification
                else if (fileType
                        .equalsIgnoreCase(SOAConstants.SV_NEWSP_CREATE_REQUEST_NOTIFICATION)) {
                    
                    if (elementsCount > 14)
                        processDataToSvBody(content, rowCol[1], rowCol[13],
                                "-", rowCol[5], SOAConstants.RECEIVED,
                                reason, rowCol[rowCol.length - 1]);
                     else 
                        processDataToSvBody(content, rowCol[1], rowCol[12],
                                    "-", rowCol[5],
                                    SOAConstants.RECEIVED, reason,
                                    rowCol[rowCol.length - 1]);
                }
                // This block will generate the body onto a report for the
                // failed records of VersionOldSP_ConcurrenceRequestNotification
                else if (fileType.equalsIgnoreCase
                        (SOAConstants.SV_OLDSP_CONCURR_REQUEST_NOTIFICATION)) {
                    if (elementsCount > 12)  
                        processDataToSvBody(content, rowCol[1], rowCol[11],
                                rowCol[5], rowCol[1], SOAConstants.RECEIVED,
                                reason, rowCol[rowCol.length - 1]);
                    else 
                        processDataToSvBody(content, rowCol[1], rowCol[10],
                                    rowCol[5], rowCol[1],
                                    SOAConstants.RECEIVED, reason,
                                    rowCol[rowCol.length - 1]);
                }
                // This block will generate the body onto a report for the
                // failed records of subscriptionVersionStatusAttributeValueChange
                else if (fileType
                        .equalsIgnoreCase
                        (SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION)) {
                    if (elementsCount > 11)
                        processDataToSvBody(content, rowCol[1], rowCol[8],
                                rowCol[1],null, SOAConstants.RECEIVED, reason,
                                rowCol[rowCol.length - 1]);
                    else 
                        processDataToSvBody(content, rowCol[1], rowCol[7],
                                    rowCol[1],null, SOAConstants.RECEIVED,
                                    reason, rowCol[rowCol.length - 1]);
                    
                }
                // This block will generate the body onto a report for the
                // failed records of VersionOldSPFinalConcurWindowExpNotification
                else if (fileType
                        .equalsIgnoreCase
                   (SOAConstants.VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION)){
                    if (elementsCount > 9) 
                        processDataToSvBody(content, rowCol[1], rowCol[8],
                                rowCol[1],null, SOAConstants.RECEIVED, reason,
                                rowCol[rowCol.length - 1]);
                    else 
                        processDataToSvBody(content, rowCol[1], rowCol[7],
                                    rowCol[1],null, SOAConstants.RECEIVED,
                                    reason, rowCol[rowCol.length - 1]);
                }
                // This block will generate the body onto a report for the
                // failed records of VersionNewSPFinalCreateWindowExpirNotification
                else if (fileType
                        .equalsIgnoreCase
                 (SOAConstants.VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION)){
                    if (elementsCount > 15) 
                        processDataToSvBody(content, rowCol[1], rowCol[14],
                                rowCol[5], rowCol[6], SOAConstants.RECEIVED,
                                reason, rowCol[rowCol.length - 1]);
                    else
                        processDataToSvBody(content, rowCol[1], rowCol[13],
                                    rowCol[5], rowCol[6],
                                    SOAConstants.RECEIVED, reason,
                                    rowCol[rowCol.length - 1]);
                }
                // This block will generate the body onto a report for the
                // failed records of subscriptionVersionNewNPA-NXX
                else if (fileType.equalsIgnoreCase
                        (SOAConstants.SUB_NEW_NPANXX_NOTIFICATION)) {
                    
                    if (rowCol[1]!=null && rowCol[5]!=null  && reason!=null 
                        && rowCol[6]!=null && rowCol[rowCol.length - 1]!=null) {
                        
                        content.append(StringUtils.padString(rowCol[1], 7,
                                false, ' '));
                        
                        content.append(StringUtils.padString(rowCol[5], 31,
                                false, ' '));
                        
                        if(rowCol[6].length()==7){
                        if(rowCol[6].indexOf('-')!=0){
                        StringTokenizer npanxx = new StringTokenizer(rowCol[6],
                        "-");
                        if(npanxx.countTokens()>1){
                           content.append(StringUtils.padString((String) npanxx
                                .nextToken(), 6, false, ' '));
                           content.append(StringUtils.padString((String) npanxx
                                .nextToken(), 6, false, ' '));
                           }
                        else{
                           content.append
                           (StringUtils.padString("-", 6, false, ' '));
                           content.append
                           (StringUtils.padString("-", 6, false, ' '));
                           }
                        }
                        else {
                         content.append
                         	(StringUtils.padString("-", 6, false, ' '));
                         content.append
                         	(StringUtils.padString("-", 6, false, ' '));
                        }
                        }
                        else  if(rowCol[6].length()==6){
                            
                           content.append(StringUtils.padString
                                   (rowCol[6].substring(0,3), 6, false, ' '));
                           content.append(StringUtils.padString
                                   (rowCol[6].substring(3,6), 6, false, ' '));
                            
                        }
                        content.append(StringUtils.padString(reason, 50, false,
                        ' '));
                        
                        content.append(rowCol[rowCol.length - 1]);
                    } 	
                 }
                
                // This block will generate the body onto a report for the
                // failed records of Numberpoolblock related notifications
                else if (fileType
                      .equalsIgnoreCase
                     (SOAConstants.NBR_POOLBLK_OBJECT_CREATION_NOTIFICATION)|| 
                     fileType.equalsIgnoreCase
                     (SOAConstants.NBR_POOLBLK_ATTR_VAL_CHANGE_NOTIFICATION)){
                                    
                    processDataToNbrPoolBlock(content, rowCol[1],rowCol[5], 
                            	rowCol[6], reason,rowCol[rowCol.length - 1]);
                } 
                else if (fileType
                        .equalsIgnoreCase
                   (SOAConstants.NBR_POOLBLK_STA_ATTR_VAL_CHANGE_NOTIFICATION)) {
                    
                    if (rowCol[1]!=null && rowCol[5]!=null && reason!=null
                            && rowCol[rowCol.length - 1]!=null) {
                        
                        content.append(StringUtils.padString(rowCol[1], 7,
                                false, ' '));
                        
                        content.append(StringUtils.padString(rowCol[5], 31,
                                false, ' '));
                        
                        content.append(StringUtils.padString(
                                SOAConstants.RECEIVED, 53, false, ' '));
                        
                        content.append(StringUtils.padString(reason, 50, false,
                        ' '));
                        
                        content.append(rowCol[rowCol.length - 1]);
                    }
                }   
                // This block will generate the body onto a report for the
                // failed records of NPAC Operational related notifications 
                else if (fileType.equalsIgnoreCase
                        (SOAConstants.VERSION_OBJECT_CREATION_NOTIFICATION)
                        || fileType
                        .equalsIgnoreCase
                        (SOAConstants.VERSION_ATR_VAL_CHAN_NOTIFICATION)) {
                    
                    if(elementsCount>8) {
                    	String portingTn = null;
                    	if (rowCol[11] != null && rowCol[11].length() < 10)
                    	{
                    		portingTn = rowCol[12];
                    	}
                    	else
                    	{
                    		portingTn = rowCol[11];
                    	}
                        processDataToNpacBody(content, rowCol[1], portingTn,
                                reason, rowCol[rowCol.length - 1]);
                    }else  
                    {	
                    	processDataToNpacBody(content, rowCol[1], rowCol[6],
                    		       reason, rowCol[rowCol.length - 1]);
                    }
                        
                    
                }
                else if (fileType.equalsIgnoreCase
                        (SOAConstants.LNP_NPAC_SMS_OPER_INFO)) {
                    
                        processDataToNpacBody(content, rowCol[1],null,
                                reason, rowCol[rowCol.length - 1]);
                }
                else if (fileType
                   .equalsIgnoreCase(SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION)
                   || fileType.equalsIgnoreCase(SOAConstants.SUB_AUDIT_RESULTS_NOTIFICATION)) {
                  if (fileType.equalsIgnoreCase
                          (SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION)) {
                       
                            processDataToAuditBody(content, rowCol[1],
                                    rowCol[7],null, reason,
                                    rowCol[rowCol.length - 1]);
                    } else {
                            processDataToAuditBody(content, rowCol[1],null,
                                    null, reason, rowCol[rowCol.length - 1]);
                        }
                }
                //This block will generate the body onto a report for the
                // failed records of SubscriptionAudit notifications. 
                else if (fileType
                        .equalsIgnoreCase(SOAConstants.SUB_AUDIT_OBJECT_CREATION_NOTIFICATION)
                        || fileType
                        .equalsIgnoreCase(SOAConstants.SUB_AUDIT_OBJECT_DELETION_NOTIFICATION)) {
                    
                       if(rowCol[5].equalsIgnoreCase(reason))
                            processDataToAuditBody(content, rowCol[1],null,
                                "-", reason, rowCol[rowCol.length - 1]);
                        else
                            processDataToAuditBody(content, rowCol[1],null,
                               rowCol[5], reason, rowCol[rowCol.length - 1]);
                  }
                content.append(newLine);
                report.append(content);
                report.append(newLine);
            }
           }
          }
         else {
            content.append("............................................");
            report.append(content);
         }
     }catch(Exception ex){
         Debug.log(Debug.ALL_ERRORS,ex.getMessage());
    	 }
    }
    
    /**
     * This function used to add summary part to the report
     * 
     * @param stratTime
     *            Date time on which process started
     * @param endTime
     *            Date time on which process ended
     * @param recRead  contains a number how many have been read from a file.
     * @param recUpdate contains a number how many have been updated.
     * @param recFail contains a number how many have been failed.
     * @param recordArr  this is used in case of mass spid update
     *  
     */
    public void summary(Date startTime, Date endTime, int recRead,
            int recUpdate, int recFail, int[] recordArr) {
        super.summary(startTime, endTime, recRead, recUpdate, recFail,
                recordArr);
    }
    
    /**
     * This will be used to prepare for all the headers. 
     * @param headersData contains the contents of header part. 
     * @param header contains the header data.
     */
    private void prepareHeaders(String[] headersData, StringBuffer header) {
        if (headersData != null) {
            for (int i = 0; i < headersData.length; i++)
                header.append(headersData[i]);
        }
    }
    /**
     * This will be used to make use the common elements to prepare for 
     * the title part.
     * 
     * @param header contains the common elements to prepare the titles.
     * @param msg contains the message which will appear at the title part.
     */  
    private void commonFormatElements(StringBuffer header,String msg){
        header.append(newLine);
        header.append(newLine);
        header.append(msg);
        header.append(newLine);
        header.append("---------------------------------");
        header.append(newLine);
    }
    
    /**
     * This will be used to prepare the contents onto a body of the report for
     * failed Audit notifications.
     * 
     *  @param svid  contains the service provider id.
     * @param portingtn contains the porting tn.
     * @param nnsp  contains the new service provider id.
     * @param onsp  contains the old service provider id.
     * @param status contains the status of that notification.
     * @param reason  contains the reason for failed record.
     * @param others contains the failed record
     * @return content is the StringBuffer object collect the data for the
     * 		   body of the report.
     * 	@throws FrameworkException is thrown when any application level error is 
     * 							occurred.
     */
    
    private void processDataToSvBody(StringBuffer content, String spid,
            String portingtn, String nnsp, String onsp, String status,
            String reason, String others) throws FrameworkException {
       try{ 
         
         if(spid!=null && portingtn!=null && status!=null && reason!=null) {
         
             content.append(StringUtils.padString(spid, 7, false, ' '));
             
         	 content.append(StringUtils.padString(portingtn, 20, false, ' '));
               
             if(fileType.equalsIgnoreCase
                    (SOAConstants.SV_NEWSP_CREATE_REQUEST_NOTIFICATION)
                    || fileType.equalsIgnoreCase
                     (SOAConstants.VER_NEWSP_FINAL_CREATE_WINDOW_EXP_NOTIFICATION) ) {
               
                	content.append(StringUtils.padString(nnsp, 7, false, ' '));
                	
                	content.append(StringUtils.padString(onsp, 7, false, ' '));
             }
            else if (fileType
               .equalsIgnoreCase
               		(SOAConstants.VER_OLDSP_FINAL_CONCUR_WINDOW_EXP_NOTIFICATION)
               || fileType.equalsIgnoreCase
               			(SOAConstants.VERSION_STS_ATR_VAL_CHAN_NOTIFICATION)
               || fileType.equalsIgnoreCase
               			(SOAConstants.SV_OLDSP_CONCURR_REQUEST_NOTIFICATION)){
                 
               		content.append(StringUtils.padString(nnsp, 7, false, ' '));
                 }
             
             content.append(StringUtils.padString(status, 25, false, ' '));
             
             content.append(StringUtils.padString(reason, 50, false, ' '));

             content.append(others);
        }
         
       }catch(Exception ex){
           
           Debug.log(Debug.ALL_ERRORS,ex.getMessage());
           
           throw new FrameworkException(ex.getMessage());
           
           }
    }
    
    /**
     * This will be used to prepare the contents onto a body of the report for
     * failed Audit notifications.
     * 
     * @param spid contains the service provider id
     * @param AuditId contains the audit id.
     * @param others contains the failed record
     *  @exception FrameworkException will be thrown when an error is occurred. 
     * 			  					 apart from sql.
     */
    private void processDataToAuditBody(StringBuffer content, String spid,
            String disTn, String auditId, String reason, String others)
    		throws FrameworkException{
        try{ 
    	if(spid!=null && reason!=null && others!=null){
    	    
   	        content.append(StringUtils.padString(spid, 7, false, ' '));
   	        
        if (fileType.equalsIgnoreCase
                (SOAConstants.SUB_AUDIT_DISCREPANCY_RPT_NOTIFICATION)) {
            
            if(disTn!=null) 
            
                content.append(StringUtils.padString(disTn, 20, false, ' '));
            
            else
                
                content.append(StringUtils.padString("-", 20, false, ' '));
            
        } else if (!fileType.equalsIgnoreCase
                (SOAConstants.SUB_AUDIT_RESULTS_NOTIFICATION)) {
            
            if(auditId!=null)
                
               content.append(StringUtils.padString(auditId, 31, false, ' '));
            
            else
                
               content.append(StringUtils.padString("-", 31, false, ' ')); 
        }
    	}
        else
           content.append(StringUtils.padString("-", 7, false, ' '));
        
    	content.append(StringUtils.padString(reason, 50, false, ' '));
        
        content.append(others);
        
        }catch(Exception ex){
            
            Debug.log(Debug.ALL_ERRORS,ex.getMessage());
            
            throw new FrameworkException(ex.getMessage());
            
            }
    }
    
    /**
     * This will be used to prepare the contents onto a body of the report for
     * failed NPAC Operational Information notifications.
     * 
     * @param content is the StringBuffer object collect the data for the body 
     * 		  of the report. 
     * @param spid   contains the service provider id
     * @param versionTN contains the version TN.
     * @param nnsp    contains the New service Provider.
     * @param reason  contains the failed record. 
     * 	@throws FrameworkException is thrown when any application level error is 
     * 							occurred. 
     */
    private void processDataToNpacBody(StringBuffer content, String spid,
            String versionTN, String reason, String others)
    		throws FrameworkException{ 
        try{
        if (fileType.equalsIgnoreCase(SOAConstants.LNP_NPAC_SMS_OPER_INFO)){
            
            if(spid!=null)
            
                content.append(StringUtils.padString(spid, 7, false, ' '));
            
            else
                content.append(StringUtils.padString("-", 7, false, ' '));
        }  
        else {
            if(spid!=null && versionTN!=null)
            {
            
                content.append(StringUtils.padString(spid, 7, false, ' '));
            
            
                content.append(StringUtils.padString(versionTN, 17, false, ' '));

            }
            else
            {
                content.append(StringUtils.padString("-", 7, false, ' '));
        
                content.append(StringUtils.padString("-", 17, false, ' '));
            }
        }
        if(reason!=null && others!=null)
        {
        content.append(StringUtils.padString(reason, 50, false, ' '));
        
        content.append(others);
        }
        }catch(Exception ex){
            
            Debug.log(Debug.ALL_ERRORS,ex.getMessage());
            
            throw new FrameworkException(ex.getMessage());
            
            }
    }
    
    /**
     * This will be used to prepare the contents onto a body of the report for
     *  failed NumberPoolBlock notifications.
     * 
     * @param content   is the StringBuffer object collect the data for the body
     * 					of the report. 
     * @param spid      contains the service provider id.
     * @param nbrBlockId contains the numberpoolblock id.
     * @param npaNnxxX  contains the npanxx number.
     * @param reason  contains the reason for the failed data.
     * @param others  contains the failed record. 
     * 	@throws FrameworkException is thrown when any application level error is 
     * 							occurred.
     */
    private void processDataToNbrPoolBlock(StringBuffer content, String spid,
            String nbrBlockId, String npaNxxX, String reason, String others)
    		throws FrameworkException{
    	
        try
        {
        if(spid!=null && nbrBlockId!=null && reason!=null && others!=null
                && npaNxxX!=null){
            
            content.append(StringUtils.padString(spid, 7, false, ' '));
            
            content.append(StringUtils.padString(nbrBlockId, 31, false, ' '));
            
            String npaNxxx = npaNxxX.trim();
            
            if(npaNxxx.length()==7){
            
                content.append(StringUtils.padString(npaNxxx.substring(0, 3), 6,
                    false, ' '));
            
                content.append(StringUtils.padString(npaNxxx.substring(3, 6), 6,
                    false, ' '));
            
                content.append(StringUtils.padString(npaNxxx.substring(6, 7), 4,
                    false, ' '));
            }
            else
            {
              
                content.append(StringUtils.padString("-", 6, false, ' '));
              
                content.append(StringUtils.padString("-", 6, false, ' '));
              
                content.append(StringUtils.padString("-", 6, false, ' '));   
            }
        
            content.append(StringUtils.padString(reason, 50, false, ' '));
        
            content.append(others);
        }
       }catch(Exception ex){
           
           Debug.log(Debug.ALL_ERRORS,ex.getMessage());
           
           throw new FrameworkException(ex.getMessage());
           }
    }
}
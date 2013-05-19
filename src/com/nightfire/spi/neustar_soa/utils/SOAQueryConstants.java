/**
 * This class contains constant values used by SOA CustomDBFuncations
 *
 * @author Devaraj
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 */

/**
	Revision History
	---------------------
	Rev#		Modified By 	Date			Reason
	-----       -----------     ----------		--------------------------
	1			Devaraj			04/22/2005		Created
	2			Subbarao		08/30/2005		Added new Query Constants.
	3			Ramesh			08/01/2005		Added Query to fetch the CauseCode.
	4			Jigar			09/02/2005		Updated the constants
												SV_CANCEL_ACK_AS_NEW_REQUEST_ALLOWED_QUERY
												SV_CANCEL_ACK_AS_OLD_REQUEST_ALLOWED_QUERY
	5			Subbarao		10/10/2005		Added new Query Constants.
	6			Subbarao		11/24/2005		Modified Query Constants.
	7			Subbarao		11/30/2005		Modified Query Constant.
	8			Subbarao		12/13/2005		Added new Query Constant.
	9			Peeyush M		05/10/2007		Added new Query for SubDomain requirement.
	10			Peeyush M		07/25/2007		Modified Query to fix the TD#6487.
 */

package com.nightfire.spi.neustar_soa.utils;

public class SOAQueryConstants {

   /**
    * This query is used to check if a SPID belongs to a particular
    * Customer ID.
    */
   public static final String CUSTOMER_SPID_QUERY =
      "SELECT TPID FROM CUSTOMER_LOOKUP WHERE TPID = ? AND CUSTOMERID = ?";
   
   /**
    * This query is used to check if a subscription version with the
    * given SVID exists in a particular region.
    */
   public static final String SV_EXISTS_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND SVID = ? "+
      "AND REGIONID = ?";

   /**
    * This query checks to see if an "active-like" subscription version
    * exists for a given telephone number.
    */
   public static final String TN_EXISTS_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND "+
      "PORTINGTN = ? AND STATUS IN ( 'conflict','active','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending','creating','NPACCreateFailure')";


   /**
    * This query checks to see if an "pending-like" subscription version
    * exists for a given telephone number.
    */
   public static final String TN_EXISTS_QUERY_ACTIVE =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND "+
      "PORTINGTN = ? AND STATUS IN  ( 'conflict','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending','creating','NPACCreateFailure')";


   /**
    * This query checks to see if an "pending-like" subscription version
    * exists for a given telephone number.
    */
   public static final String TN_EXISTS_QUERY_ACTIVE_STATUS =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND "+
      "PORTINGTN = ? AND STATUS = 'active'";

   /**
    * This retrieves the status of a subscription version based on its
    * SVID and region.
    */
   public static final String SV_STATUS_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND SVID = ? AND REGIONID = ?";

   /**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_STATUS_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String STATUS_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE " +
      "PORTINGTN = ? ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the SV status values for a range of telephone numbers.
    */
   public static final String TNRANGE_STATUS_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN >= ? AND PORTINGTN <= ? AND STATUS IN " +
      " ( 'conflict','active','pending','sending','download-failed'," +
      "'download-failed-partial','disconnect-pending','cancel-pending'," +
      "'creating','NPACCreateFailure') ";

   /**
    * This queries for the new service provider of a subscription with the
    * given SVID and region.
    */
   public static final String SV_NEW_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND SVID = ? " +
      "AND REGIONID = ? AND NNSP = ?";

   /**
    * This queries for the new service provider of an active-like subscription
    *  with the given telephone number.
    */
   public static final String TN_NEW_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? " +
      "AND NNSP = ? AND STATUS  IN ( 'conflict','active','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY " +
      "CREATEDDATE DESC";

    /**
    * This queries for the new service provider of an active-like subscription
    *  with the given telephone number.
    */
   public static final String NEW_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? AND NNSP = ? AND STATUS IN  ( 'conflict','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY " +
      "CREATEDDATE DESC";


   /**
    * This is used to check if there are any subscription versions in this
    * range of numbers that do not have a particular new service provider.
    */
   public static final String TNRANGE_NEW_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE STATUS  IN " +
      " ( 'conflict','active','pending','sending','download-failed','download-failed-partial'," +
      "'disconnect-pending','cancel-pending', 'creating','NPACCreateFailure') "+
      "AND SPID = ? AND PORTINGTN >= ? AND PORTINGTN <= ? AND NNSP != ? " +
      "ORDER BY CREATEDDATE DESC";

      /**
    * This is used to check if there are any subscription versions in this
    * range of numbers that do not have a particular new service provider.
    */
   public static final String TNRANGE_NEW_SP_QUERY_ONE =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE STATUS IN " +
      " ( 'conflict','pending','sending','download-failed','download-failed-partial'," +
      "'disconnect-pending','cancel-pending') "+
      "AND SPID = ? AND PORTINGTN >= ? AND PORTINGTN <= ? AND NNSP != ? " +
      "ORDER BY CREATEDDATE DESC";

   /**
    * This queries for the old service provider of a subscription with the
    * given SVID and region.
    */
   public static final String SV_OLD_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND SVID = ? " +
      "AND REGIONID = ? AND ONSP = ?";

   /**
    * This queries for the old service provider of an active-like subscription
    * with the given telephone number.
    */
   public static final String TN_OLD_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? " +
      "AND ONSP = ? AND STATUS IN  ( 'conflict', 'active','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY " +
      "CREATEDDATE DESC";

   /**
    * This queries for the old service provider of an active-like subscription
    * with the given telephone number.
    */
   public static final String OLD_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ SVID FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? " +
      "AND ONSP = ? AND STATUS IN  ( 'conflict','pending','sending','download-failed'," +
      "'download-failed-partial','disconnect-pending','cancel-pending'," +
      " 'creating','NPACCreateFailure') ORDER BY CREATEDDATE DESC";

   /**
    * This is used to check if there are any subscription versions in this
    * range of numbers that do not have a particular old service provider.
    */
   public static final String TNRANGE_OLD_SP_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE STATUS " +
      "IN  ( 'conflict', 'active','pending','sending','download-failed'," +
      "'download-failed-partial','disconnect-pending','cancel-pending'," +
      " 'creating','NPACCreateFailure') "+
      "AND SPID = ? AND PORTINGTN >= ? AND PORTINGTN <= ? AND ONSP != ? " +
      "ORDER BY CREATEDDATE DESC";

   /**
    * This is used to check if there are any subscription versions in this
    * range of numbers that do not have a particular old service provider.
    */
   public static final String TNRANGE_OLD_SP_QUERY_ONE =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE STATUS " +
      "IN  ( 'conflict', 'pending','sending','download-failed'," +
      "'download-failed-partial','disconnect-pending','cancel-pending') "+
      "AND SPID = ? AND PORTINGTN >= ? AND PORTINGTN <= ? AND ONSP != ? " +
      "ORDER BY CREATEDDATE DESC";

   /**
    * This queries for the business timer type of an SV by SVID and region.
    */
   public static final String SV_BUSINESS_TIMER_TYPE_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ BUSINESSTIMERTYPE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND SVID = ? AND REGIONID = ?";


   /**
    * This queries for the business timer type of an SV by TN.
    */
    public static final String TN_BUSINESS_TIMER_TYPE_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ BUSINESSTIMERTYPE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN = ? AND STATUS IN  ( 'conflict','active','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY " +
      "CREATEDDATE DESC";

   /**
    * This queries the new service provider due date of an SV by SVID and
    * region.
    */
   public static final String SV_NEW_DUE_DATE_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ NNSPDUEDATE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND SVID = ? AND REGIONID = ?";

   /**
    * This queries the new service provider due date of an SV by TN.
    */
   public static final String TN_NEW_DUE_DATE_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ NNSPDUEDATE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? AND STATUS IN  ( 'conflict','active','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY " +
      "CREATEDDATE DESC";

   /**
    * This query checks the message history of an SV to see if it contains a
    * particular message subtype.
    */
   public static final String SV_MESSAGE_HISTORY_QUERY =
      "SELECT MESSAGESUBTYPE FROM SOA_SV_MESSAGE WHERE MESSAGESUBTYPE = ? "+
      "AND MESSAGEKEY IN "+
      "(SELECT /*+ INDEX(SOA_MESSAGE_MAP SOA_MSGMAP_INDEX_2) */ MESSAGEKEY FROM SOA_MESSAGE_MAP WHERE REFERENCEKEY IN "+
      "(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ REFERENCEKEY FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND SVID = ? AND REGIONID = ?) )";

   /**
    * This query checks the message history of an SV to see if it contains a
    * particular message subtype.
    */
   public static final String SV_1_MESSAGE_HISTORY_QUERY =
      "SELECT MESSAGESUBTYPE FROM SOA_SV_MESSAGE WHERE MESSAGESUBTYPE IN (";

   /**
    * This query checks the message history of an SV to see if it contains a
    * particular message subtype.
    */
   public static final String SV_2_MESSAGE_HISTORY_QUERY =
      " AND MESSAGEKEY IN "+
      "(SELECT /*+ INDEX(SOA_MESSAGE_MAP SOA_MSGMAP_INDEX_2) */ MESSAGEKEY FROM SOA_MESSAGE_MAP WHERE REFERENCEKEY IN "+
      "(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ REFERENCEKEY FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND SVID = ? AND REGIONID = ?) )";

   /**
    * This query checks the message history of an SV with a particular TN
    * to see if it contains a particular message subtype.
    */
   public static final String TN_MESSAGE_HISTORY_QUERY =
      "SELECT MESSAGESUBTYPE FROM SOA_SV_MESSAGE WHERE MESSAGESUBTYPE = ? "+
      "AND MESSAGEKEY IN "+
      "(SELECT /*+ INDEX(SOA_MESSAGE_MAP SOA_MSGMAP_INDEX_2) */ MESSAGEKEY FROM SOA_MESSAGE_MAP WHERE REFERENCEKEY IN "+
      "(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ REFERENCEKEY FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN = ? AND STATUS IN  ( 'conflict','active','pending','sending'," +
      "'download-failed','download-failed-partial','disconnect-pending'," +
      "'cancel-pending', 'creating','NPACCreateFailure')) )";

   /**
    * This query checks the message history of an SV with a particular TN
    * to see if it contains a particular message subtype.
    */
   public static final String TN_1_MESSAGE_HISTORY_QUERY =
      "SELECT MESSAGESUBTYPE FROM SOA_SV_MESSAGE WHERE MESSAGESUBTYPE IN (";

   /**
    * This query checks the message history of an SV with a particular TN
    * to see if it contains a particular message subtype.
    */
   public static final String TN_2_MESSAGE_HISTORY_QUERY =
      " AND MESSAGEKEY IN "+
      "(SELECT /*+ INDEX(SOA_MESSAGE_MAP SOA_MSGMAP_INDEX_2) */ MESSAGEKEY FROM SOA_MESSAGE_MAP WHERE REFERENCEKEY IN "+
      "(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ REFERENCEKEY FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN = ? AND STATUS IN  ( 'conflict', 'active','pending'," +
      "'sending','download-failed','download-failed-partial'," +
      "'disconnect-pending','cancel-pending', 'creating','NPACCreateFailure')) )";

   /**
    * Constant used to check if a timer type is "long".
    */
   public static final String LONG_TIMER_TYPE = "long";

   /**
    * This query checks whether LRN exists in creating status or not
    */
   public static final String LRN_EXISTS_QUERY =
       "SELECT LRN FROM SOA_LRN WHERE LRN = ? AND REGIONID = ? AND SPID = ? " +
       "AND STATUS IN ('creating', 'ok')";

   /**
    * This query checks whether LRN exists for the given SPID
    */
   public static final String LRN_EXISTS_FOR_SPID_QUERY =
       "SELECT LRN FROM SOA_LRN WHERE LRN = ? AND SPID = ? AND STATUS IN " +
       "('creating', 'ok')";

   /**
    * This query checks whether LRN exists for the given region
    */
   public static final String LRN_EXISTS_FOR_REGION_QUERY =
       "SELECT LRN FROM SOA_LRN WHERE LRN = ? AND REGIONID = ? AND " +
       "STATUS IN ('creating', 'ok')";

   /**
    * This query is used to check whether LRN ID exists for LRN Delete Request
    */
   public static final String LRN_ID_EXISTS_QUERY =
       "SELECT LRN FROM SOA_LRN WHERE LRNID = ? AND REGIONID = ? " +
       "AND SPID = ? " +
       "AND STATUS = 'ok'";

   /**
    * This query is used to check whether LRN value exists for
    * LRN Delete Request
    */
   public static final String LRN_VALUE_EXISTS_QUERY =
       "SELECT LRN FROM SOA_LRN WHERE LRN = ? AND SPID = ? AND REGIONID = ? " +
       "AND STATUS = 'ok'";

   /**
    * This query is used to get the LRN value based on LRN ID
    */
   public static final String GET_LRN_VALUE_QUERY =
       "SELECT LRN FROM SOA_LRN WHERE LRNID = ? AND REGIONID = ?";

   /**
    * This query is used to check whether any number pool block which is
    * using the deleted LRN
    */
   public static final String LRN_EXISTS_FOR_NBRPOOLBLOCK_QUERY =
       "SELECT LRN FROM SOA_NBRPOOL_BLOCK WHERE LRN = ? AND REGIONID = ? " +
       "AND STATUS <> 'old'";

   /**
    * This query is used to check whether any SV exists which is using the
    * deleted LRN
    */
   public static final String LRN_EXISTS_FOR_SV_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_4) */ LRN FROM SOA_SUBSCRIPTION_VERSION WHERE LRN = ? AND " +
       "REGIONID = ? " +
       "AND STATUS NOT IN ('canceled', 'old')";

   /**
    * This query is used to check whether NPA NXX exists
    */
   public static final String NPA_NXX_EXISTS_QUERY =
       "SELECT /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_1) */ NPA FROM SOA_NPANXX WHERE NPA = ? AND NXX = ? AND STATUS = 'ok'";

   /**
    * This query is used to check whether NPA NXX value exists for
    * delete request
    */
   public static final String NPA_NXX_VALUE_EXISTS_FOR_DELETE_QUERY =
       "SELECT /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_1) */ NPA FROM SOA_NPANXX WHERE NPA = ? AND NXX = ? AND SPID = ? " +
       "AND REGIONID = ? AND STATUS = 'ok'";

   /**
    * This query is used to check whether NPA NXX ID exists for delete request
    */
   public static final String NPA_NXX_ID_EXISTS_FOR_DELETE_QUERY =
       "SELECT /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_2) */ NPA FROM SOA_NPANXX WHERE NPANXXID = ? AND REGIONID = ? " +
       "AND SPID = ? AND STATUS = 'ok'";

   /**
    * This query is used to check whether the NBR POOL BLOCK modifying is in
    * active status or not
    */
   public static final String IS_NPB_ACTIVE_QUERY =
       "SELECT NPA FROM SOA_NBRPOOL_BLOCK WHERE NPBID = ? AND REGIONID = ? " +
       " AND STATUS = 'active'";

   /**
    * This query is used to check whether the NBR POOL BLOCK modifying is in
    * active status or not
    */
   public static final String IS_NPB_ACTIVE_WITH_DASHX_QUERY =
       "SELECT NPA FROM SOA_NBRPOOL_BLOCK WHERE NPA = ? AND NXX = ? AND " +
       "DASHX = ? " +
       "AND SPID = ? AND STATUS = 'active'";

   /**
    * This query is used to check whether NBR POOL exists for creating NPB
    */
   public static final String NPB_EXISTS_QUERY =
       "SELECT NPA FROM SOA_NBRPOOL_BLOCK WHERE NPA = ? AND NXX = ? AND " +
       "DASHX = ? " +
       "AND STATUS IN ('active','sending', 'download-failed', " +
       "'download-failed-partial')";

   /**
    * This query is used to check DASH X for NPB creation
    */
   public static final String DASHX_EXISTS_QUERY =
       "SELECT NPA FROM SOA_NPANXX_X WHERE NPA = ? AND NXX = ? AND DASHX = ? " +
       "AND SPID = ? AND STATUS = 'ok'";

   /**
    * This query is used to check DASH X for NPB creation
    */
   public static final String DASHX_EXISTS_FOR_REGION_QUERY =
       "SELECT NPA FROM SOA_NPANXX_X WHERE NPA = ? AND NXX = ? AND DASHX = ? " +
       "AND SPID = ? AND REGIONID = ? AND STATUS = 'ok'";

   /**
    * This query is used to check DASH X for SV creation
    */
   public static final String DASHX_EXISTS_FOR_SV_QUERY =
       "SELECT NPA FROM SOA_NPANXX_X WHERE NPA = ? AND NXX = ? AND DASHX = ? " +
       "AND STATUS = 'ok'";

   /**
    * This query is used to check the DASH X effective date validation
    * for NPB create
    */
   public static final String VALIDATE_DASHX_EFFECTIVE_DATE_QUERY =
       "SELECT NPA FROM SOA_NPANXX_X WHERE NPA = ? AND NXX = ? AND DASHX = ? " +
       "AND EFFECTIVEDATE <= SYSDATE";

   /**
    * This query is used to check the DASH X effective date validation
    * for SV create
    */
   public static final String VALIDATE_DASHX_EFFECTIVE_DATE_WITHS_SPID_QUERY =
       "SELECT NPA FROM SOA_NPANXX_X WHERE NPA = ? AND NXX = ? AND DASHX = ? " +
       "AND SPID = ? AND EFFECTIVEDATE <= TO_DATE(?,'MM-DD-YYYY-HHMISSPM')" +
       " AND STATUS = 'ok'";


   /**
    *  This query is used to get the new NPA
    */
   public static final String GET_NEW_NPA_QUERY =
       "SELECT NEWNPA FROM SOA_NPA_SPLIT_NXX WHERE NEWNPA = ? AND NXX = ?";

   /**
    * This query is used to get the old NPA
    */
   public static final String GET_OLD_NPA_QUERY =
       "SELECT NEWNPA FROM SOA_NPA_SPLIT_NXX WHERE OLDNPA = ? AND NXX = ?";

   /**
    * This query is used to check PDP Start date validation
    */
   public static final String VALIDATE_PDP_START_DATE_QUERY =
       "SELECT PDPSTARTDATE FROM SOA_NPA_SPLIT WHERE NEWNPA = ? AND " +
       "PDPSTARTDATE < SYSDATE";

   /**
    * This query is used to check PDP end date validation
    */
   public static final String VALIDATE_PDP_END_DATE_QUERY =
       "SELECT PDPENDDATE FROM SOA_NPA_SPLIT WHERE OLDNPA = ? AND " +
       "PDPENDDATE > SYSDATE";

   /**
    * This query is used to check whether Audit Name exists or not
    */
   public static final String AUDIT_NAME_EXISTS_QUERY =
       "SELECT AUDITNAME FROM SOA_AUDIT WHERE SPID = ? AND AUDITNAME = ? ";

   /**
    * This query is used to check whether Audit Name exists or not
    */
   public static final String AUDIT_NAME_EXISTS_WITH_OK_STATUS_QUERY =
       "SELECT AUDITNAME FROM SOA_AUDIT WHERE SPID = ? AND AUDITNAME = ? " +
       "AND STATUS = 'ok'";

   /**
    * This query is used to check whether Audit Id exists or not
    */
   public static final String AUDIT_ID_EXISTS_QUERY =
       "SELECT AUDITNAME FROM SOA_AUDIT WHERE SPID = ? AND AUDITID = ? " +
       "AND STATUS = 'ok'";

   /**
    * This query is used to check whether Audit Name exists or not
    */
   public static final String AUDIT_NAME_EXISTS_WITHOUT_CREATING_STATUS_QUERY =
       "SELECT AUDITNAME FROM SOA_AUDIT WHERE SPID = ? AND " +
       "AUDITNAME = ? AND STATUS != 'creating'";

   /**
    * This query is used to check whether Audit Id exists or not
    */
   public static final String AUDIT_ID_EXISTS_EXCEPT_CREATING_QUERY =
       "SELECT AUDITNAME FROM SOA_AUDIT WHERE SPID = ? AND AUDITID = ? " +
       "AND STATUS != 'creating'";

   /**
    * This query is used to validate NPA NXX effective date
    */
   public static final String VALIDATE_NPA_NXX_EFFECTIVE_DATE_QUERY =
       "SELECT /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_1) */ NPA FROM SOA_NPANXX WHERE NPA = ? AND NXX = ? AND " +
       "EFFECTIVEDATE <= TO_DATE(?,'MM-DD-YYYY-HHMISSPM') AND STATUS = 'ok'";

   /**
    * This query is used to check due date validation
    */
   public static final String VALIDATE_DUE_DATE_QUERY =
       "SELECT SYSDATE FROM DUAL WHERE SYSDATE < " +
       "TO_DATE(?,'MM-DD-YYYY-HHMISSPM')";

   /**
    * This query is used to check cancel ack has been acknowledged by
    * new service provider or not
    */
   public static final String SV_CANCEL_ACK_AS_NEW_REQUEST_ALLOWED_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE NNSP = SPID " +
       "AND SPID = ? AND " +
       "PORTINGTN = ? AND " +
       "STATUS = 'cancel-pending'";

   /**
    * This query is used to check cancel ack has been acknowledged by
    * old service provider or not
    */
   public static final String SV_CANCEL_ACK_AS_OLD_REQUEST_ALLOWED_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE ONSP = SPID " +
       "AND SPID = ? AND " +
       "PORTINGTN = ? AND " +
       "STATUS = 'cancel-pending'";

   /**
    * This query is used to check LRN exists for SPID or not
    */
   public static final String LRN_STATUS_OK_FOR_SPID_QUERY =
       "SELECT LRN FROM SOA_LRN WHERE LRN = ? AND SPID = ? AND STATUS = 'ok'";
   
   /**
    * 
    */
   public static final String LRN_STATUS_OK_FOR_APT_SUBDOMAIN_USER_QUERY =
	   " SELECT  LRN FROM SOA_SUBDOMAIN_LRN WHERE CUSTOMERID =  ? AND SUBDOMAINID = ? AND LRN = ? ";

   /**
    * This query is used to check the status validation for sv modify request
    */
   public static final String VALIDATE_STATUS_FOR_SV_MODIFY_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
       "PORTINGTN = ? AND SPID = ONSP AND STATUS IN  ( 'pending','sending'," +
       "'download-failed','download-failed-partial','disconnect-pending'," +
       "'cancel-pending', 'creating','NPACCreateFailure')";

   /**
    * This query is used to validate NPA NXX against SPID
    */
   public static final String VALIDATE_NPA_NXX_WITH_SPID_QUERY =
       "SELECT /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_1) */ NPA FROM SOA_NPANXX WHERE NPA = ? AND NXX = ? AND SPID = ? " +
       "AND STATUS = 'ok'";


   /**
    * This queries for the NNSP due date for SV modify request
    */
   public static final String GET_NNSP_DUE_DATE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ NNSPDUEDATE FROM SOA_SUBSCRIPTION_VERSION WHERE PORTINGTN = ? " +
       "AND SPID = ? AND STATUS IN ( 'conflict', 'active','pending','sending'," +
       "'download-failed','download-failed-partial','disconnect-pending'," +
       "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY " +
       "CREATEDDATE DESC";
   /**
    * This queries for the ONSP due date for SV modify request
    */
   public static final String GET_ONSP_DUE_DATE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ONSPDUEDATE FROM SOA_SUBSCRIPTION_VERSION WHERE PORTINGTN = ? " +
       "AND SPID = ? AND STATUS IN  ( 'conflict', 'active','pending'," +
       "'sending','download-failed','download-failed-partial','disconnect-pending'," +
       "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY " +
       "CREATEDDATE DESC";

   /**
    * This queries for the port to original value for SV modify request
    */
   public static final String GET_PORTINGTOORIGINAL_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTOORIGINAL FROM SOA_SUBSCRIPTION_VERSION WHERE SPID=? " +
       "AND PORTINGTN=? AND STATUS " +
       "IN  ( 'conflict', 'active','pending','sending','download-failed'," +
       "'download-failed-partial','disconnect-pending','cancel-pending', " +
       "'creating','NPACCreateFailure') ORDER BY CREATEDDATE DESC";
   /**
    * This queries for the porting TN  value for SV modify request
    */
   public static final String GET_PORTING_TN_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
       "AND SVID = ? AND REGIONID = ? ORDER BY CREATEDDATE DESC";

   /**
    * This queries for the porting to original for range of numbers
    * value for SV modify request
    */
   public static final String GET_PORTINGTOORIGINAL_FALSE_RANGE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID=? " +
       "AND PORTINGTN >= ? AND " +
       "PORTINGTN <= ? AND " +
       " (PORTINGTOORIGINAL = '0' OR PORTINGTOORIGINAL IS NULL) AND STATUS " +
       "IN ( 'conflict', 'pending','sending','download-failed'," +
       "'download-failed-partial','disconnect-pending','cancel-pending'," +
       "'creating','NPACCreateFailure') ORDER BY CREATEDDATE DESC";

   /**
    * This queries for the porting to original for range of numbers
    * value for SV modify request
    */
   public static final String GET_PORTINGTOORIGINAL_TRUE_RANGE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID=? " +
       "AND PORTINGTN >= ? AND " +
       "PORTINGTN <= ? AND PORTINGTOORIGINAL = '1' AND STATUS " +
       "IN  ( 'conflict','pending','sending','download-failed'," +
       "'download-failed-partial','disconnect-pending','cancel-pending', " +
       "'creating','NPACCreateFailure') ORDER BY CREATEDDATE DESC";


   /**
    * This queries for the NNSP due date for SV modify request
    */
   public static final String GET_NNSP_DUE_DATE_TNRANGE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE PORTINGTN >= ? " +
       "AND PORTINGTN <= ? AND " +
       "SPID = ? AND NNSPDUEDATE IS NULL AND STATUS " +
       "IN  ( 'conflict', 'active','pending','sending'," +
       "'download-failed','download-failed-partial','disconnect-pending'," +
       "'cancel-pending', 'creating','NPACCreateFailure') ORDER BY CREATEDDATE DESC";

   /**
    * This queries for the ONSP due date for SV modify request
    */
   public static final String GET_ONSP_DUE_DATE_TNRANGE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE PORTINGTN >= ? " +
       "AND PORTINGTN <= ? AND " +
       "SPID = ? AND ONSPDUEDATE IS NULL AND STATUS " +
       "IN ('conflict', 'active','pending','sending','download-failed'," +
       "'download-failed-partial','disconnect-pending','cancel-pending', " +
       "'creating','NPACCreateFailure') ORDER BY CREATEDDATE DESC";

   /**
    * This query is used to check the status validation for sv modify request
    */
   public static final String VALIDATE_STATUS_FOR_SV_MODIFY_RANGE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
       "PORTINGTN >= ? AND PORTINGTN <= ? AND SPID != ONSP AND STATUS IN " +
       "( 'pending','sending','download-failed','download-failed-partial'," +
       "'disconnect-pending','cancel-pending', 'creating','NPACCreateFailure')" +
       " ORDER BY CREATEDDATE DESC";

   /**
    * This queries for the Customer ID for given SPID.
    */
   public static final String GET_CUSTOMERID_QUERY =
       "SELECT CUSTOMERID FROM CUSTOMER_LOOKUP WHERE TPID = ?";

   /**
    * This query is used to check GTT ID exist or not.
    */
   public static final String GTT_ID_EXIST_QUERY =
       "SELECT GTTID FROM SOA_GTT WHERE GTTID = ?";

   /**
    * This query is used to check GTT ID, LRN, SPID combination exists or not.
    */
   public static final String GTT_ID_WITH_LRN_EXIST_QUERY =
       "SELECT SPID FROM SOA_LRN WHERE SPID = ? AND GTTID = ? AND LRN = ?";

   /**
    * This query compares the oldSpDueDate with newSpDueDate
    */
   public static final String NEWSP_DUEDATE_EQUALS_OLDSP_DUEDATE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ NNSPDUEDATE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
       "PORTINGTN = ? AND ONSPDUEDATE = TO_DATE(?,'MM-DD-YYYY-HHMISSPM') " +
       "AND STATUS IN " +
       " ( 'conflict','pending','sending','download-failed','download-failed-partial'," +
       "'disconnect-pending','cancel-pending', 'creating','NPACCreateFailure') " +
       " ORDER BY CREATEDDATE DESC";

   /**
    * This query compares the oldSpDueDate with newSpDueDate
    */
   public static final String OLDSP_DUEDATE_EQUALS_NEWSP_DUEDATE_QUERY =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ ONSPDUEDATE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
       "PORTINGTN = ? AND NNSPDUEDATE = TO_DATE(?,'MM-DD-YYYY-HHMISSPM') " +
       "AND STATUS IN " +
       " ( 'conflict','pending','sending','download-failed','download-failed-partial'," +
       "'disconnect-pending','cancel-pending', 'creating','NPACCreateFailure') " +
       "ORDER BY CREATEDDATE DESC";

  	/**
	 * This query retreives the referencekey by SVID and SPID.
	 *
	 */
	public static final String REFKEY_ON_SVID_SPID = "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ REFERENCEKEY FROM SOA_SUBSCRIPTION_VERSION WHERE SPID=?  " +
			"AND SVID=?";

	/**
	 *  This query retrieves messagekey for inserting into the respective tables.
	 */

	public static final String MESSAGEKEY_QUERY = "SELECT NeuStarFullSOAMsgKey.nextval from dual";

	/**
	 *  This query retrieves reference key for inserting into the respective tables.
	 */

	public static final String REFERENCE_QUERY = "SELECT NeuStarFullSOARefKey.nextval from dual";

	/**
	 *  This query checks all failed records whether this combination of OBJECTTYPE and ID in  any row or not.
	 *
	 */
	public static final String FAILED_SV_NPB_EXISTS="SELECT OBJECTTYPE,ID FROM SOA_SV_FAILEDSPLIST WHERE OBJECTTYPE=? and ID=?";

	/**
	    * This query retrieves the status change causeCode of subscription version based on its SPID and PORTINGIN.
	*/

	public static final String TN_CAUSECODE_QUERY=
		   "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ CAUSECODE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID= ? AND PORTINGTN= ?";

	/**
	    * This retrieves the status change causeCode of a subscription version based on its
	    * SVID and region.
	*/

	 public static final String SV_CAUSECODE_QUERY =
		   "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ CAUSECODE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND SVID = ? AND REGIONID = ?";

	 /**
	    * This retrieves the SV status change causeCode values for a range of telephone numbers.
	 */

	 public static final String TNRANGE_CAUSECODE_QUERY =
		      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, CAUSECODE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
		      "AND PORTINGTN >= ? AND PORTINGTN <= ? ";

	/**
		This retrieves the specified details from soa_subscription_version table based on the given parameters.
	*/
	public static final String LASTMESSAGE_EXISTS="SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ LASTMESSAGE," +
			"PORTINGTOORIGINAL,STATUS,LASTREQUESTTYPE,AUTHORIZATIONFLAG,ONSP,NNSP FROM SOA_SUBSCRIPTION_VERSION" +
			" WHERE SPID=? and SVID=? and PORTINGTN=?";

	/**
	 *  This retrieves the portingtn from soa_sv_message table based on invoked id and messagesubtype.
	 */
	public static final String LASTQUERYREQ_MAXREC="SELECT /*+ INDEX(SOA_SV_MESSAGE SOA_SV_MESSAGE_INDX3) */ PORTINGTN FROM SOA_SV_MESSAGE " +
		" WHERE INVOKEID=? AND MESSAGESUBTYPE=?";


	    /**
    * This query checks the message history of an SV with a particular TN
    * to see if it contains a particular message subtype.
    */
   public static final String TN_MESSAGE_HISTORY_ACTIVE_QUERY =
      "SELECT MESSAGESUBTYPE FROM SOA_SV_MESSAGE WHERE MESSAGESUBTYPE = ? "+
      "AND MESSAGEKEY IN "+
      "(SELECT /*+ INDEX(SOA_MESSAGE_MAP SOA_MSGMAP_INDEX_2) */ MESSAGEKEY FROM SOA_MESSAGE_MAP WHERE REFERENCEKEY IN "+
      "(SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ REFERENCEKEY FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN = ? AND STATUS IN  ( 'conflict','pending'," +
      "'sending','download-failed','download-failed-partial'," +
      "'disconnect-pending','cancel-pending', 'creating','NPACCreateFailure')) )";

   /**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_STATUS_QUERY_NNSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? AND NNSP = ? AND STATUS='active'" +
      "ORDER BY CREATEDDATE DESC";

	/**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_STATUS_QUERY_ONSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? AND ONSP = ? AND STATUS='active'" +
      "ORDER BY CREATEDDATE DESC";

	/**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_STATUS_NNSP_ONSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? AND ONSP != NNSP AND STATUS='active'" +
      "ORDER BY CREATEDDATE DESC";

	/**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_STATUS_NNSP_ONSP_RANGE =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN >= ? AND PORTINGTN <= ? AND ONSP != NNSP  AND STATUS = 'active'" +
      "ORDER BY CREATEDDATE DESC";

    /**
    * This retrieves the SV status values for a range of telephone numbers.
    */
   public static final String TNRANGE_STATUS_QUERY_NNSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN >= ? AND PORTINGTN <= ? AND NNSP= ?  AND STATUS = 'active'" +
      "ORDER BY CREATEDDATE DESC";

     /**
    * This retrieves the SV status values for a range of telephone numbers.
    */
   public static final String TNRANGE_STATUS_QUERY_ONSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN >= ? AND PORTINGTN <= ? AND ONSP= ?  AND STATUS = 'active'" +
      "ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the SV status values for a range of telephone numbers.
    */
   public static final String RANGE_STATUS_QUERY =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? " +
      "AND PORTINGTN >= ? AND PORTINGTN <= ? AND STATUS IN " +
      " ( 'conflict','pending','sending','download-failed'," +
      "'download-failed-partial','disconnect-pending','cancel-pending'," +
      " 'creating','NPACCreateFailure') ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String INTRA_NNSP_ONSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "PORTINGTN = ? AND ONSP = NNSP " +
      "ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String INTRA_RANGE_NNSP_ONSP =
    "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN FROM SOA_SUBSCRIPTION_VERSION WHERE STATUS " +
      "IN  ( 'conflict', 'active','pending','sending','download-failed'," +
      "'download-failed-partial','disconnect-pending','cancel-pending', " +
      "'creating','NPACCreateFailure') "+
      "AND SPID = ? AND PORTINGTN >= ? AND PORTINGTN <= ? AND ONSP = NNSP " +
      "ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_STATUS_QUERY_NNSP_WITHOUT_SPID =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE NNSP = ? AND PORTINGTN = ?  "
	   + "AND STATUS='active' ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the SV status values for a range of telephone numbers.
    */
   public static final String TNRANGE_STATUS_QUERY_NNSP_WITHOUT_SPID =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN, STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE  NNSP= ? AND PORTINGTN >= ? AND "
		+ "PORTINGTN <= ?   AND STATUS = 'active' ORDER BY CREATEDDATE DESC";

  /**
    * This retrieves the status of a subscription version based on its
    * onsp and nnsp. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_CHECK_STATUS_ONSP_NNSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ STATUS  FROM SOA_SUBSCRIPTION_VERSION WHERE  " +
      "PORTINGTN = ? AND ONSP = ? AND  NNSP = ? AND STATUS IN ('pending', 'conflict')" +
      "ORDER BY CREATEDDATE DESC";


   /**
    * This retrieves the status of a subscription version based on its
    * telephone number. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String TN_STATUS_QUERY_WITHOUT_SPID =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE " +
      "PORTINGTN = ? " +
      "ORDER BY CREATEDDATE DESC";

   /**
    * This retrieves the status of a subscription version based on its
    * svid and regionid. The most recently created subscription version with that
    * TN will be returned first.
    */
   public static final String INTRA_SVID_NNSP_ONSP =
      "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ STATUS FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
      "SVID = ? AND REGIONID = ? AND ONSP = NNSP " +
      "ORDER BY CREATEDDATE DESC";

   
   /**
    * The NPAC association status as Connected.
    */
   public static final String NPACCONNECTED_STATUS = "Connected";

   /**
    * The NPAC association status as Awaiting.
    */
   public static final String NPACAWAITING_STATUS = "Awaiting";

   /**
    * This query retrieves the status of all the configured regions.
    */
   public static final String REGION_RECOVERY_STATUS =
    "SELECT COUNT(*) FROM SOA_REGION_RECOVERY WHERE CONFIGURED='true' AND STATUS='Awaiting' AND CONNECTIVITY=? ";
   
   /**
    * This query retrieves the status of all the configured regions.
    */
   public static final String REGION_RECOVERY_STATUS_NULL =
    "SELECT COUNT(*) FROM SOA_REGION_RECOVERY WHERE CONFIGURED='true' AND STATUS='Awaiting'";


   /**
    * This query truncate SOA_REGION_RECOVERY table.
    */
   public static final String REGION_RECOVERY_TRUNCATE = "TRUNCATE TABLE SOA_REGION_RECOVERY";
   
   /**
    * This query delete records from SOA_REGION_RECOVERY table.
    */
   public static final String REGION_RECOVERY_DELETE = "DELETE FROM SOA_REGION_RECOVERY WHERE CONNECTIVITY = ?";

   /**
    * This query insert "Awaiting" status into SOA_REGION_RECOVERY table.
    */
   public static final String REGION_RECOVERY_INSERT =
	 "INSERT INTO SOA_REGION_RECOVERY(REGION,CONFIGURED,STATUS,CONNECTIVITY) VALUES(?,?,?,?)";
   
   /**
    * This query insert "Awaiting" status into SOA_REGION_RECOVERY table.
    */
   public static final String REGION_RECOVERY_UPDATE_FULL =
	 "UPDATE SOA_REGION_RECOVERY SET CONFIGURED = ? WHERE REGION=? AND CONNECTIVITY=?";
     
    /**
    * This query update "Connected" status into SOA_REGION_RECOVERY table.
    */
   public static final String REGION_RECOVERY_UPDATE =
	 "UPDATE SOA_REGION_RECOVERY SET STATUS=? WHERE REGION=? AND CONNECTIVITY=? ";

    /**
	    * This query retrieves the status of NANCFlag based on its SPID.
	*/

	public static final String SPID_NANC399_FLAG=
		   "SELECT NANC399FLAG FROM SOA_NANC_SUPPORT_FLAGS WHERE SPID= ? ";

/*
	public static final String SPID_NANC399_FLAG=
		   "SELECT NANC399FLAG FROM SOA_NANC399 WHERE SPID= ? ";
*/

	/**
	 *  This retrieves the accountid from soa_account table based on spid and accountid.
	 */
	public static final String GET_ACCOUNTID="SELECT DISTINCT ACCOUNTID FROM SOA_ACCOUNT a, CUSTOMER_LOOKUP b WHERE a.SPID=b.TPID AND " + " ACCOUNTID = ? AND a.CUSTOMERID = (SELECT DISTINCT CUSTOMERID FROM CUSTOMER_LOOKUP WHERE TPID = ?)";
        
        /**
	 *  This retrieves the accountid from soa_account table based on spid and accountid.
	 */
	public static final String GET_UNIQUE_ACCOUNTID="SELECT DISTINCT ACCOUNTID FROM SOA_ACCOUNT WHERE SPID=? AND ACCOUNTID = ? AND CUSTOMERID = (SELECT DISTINCT CUSTOMERID FROM CUSTOMER_LOOKUP WHERE TPID = ?)";

	/**
	 *  This retrieves the accountid from soa_account table based on spid and accountname.
	 */
	public static final String GET_ACCOUNT_NAME="SELECT DISTINCT ACCOUNTNAME FROM SOA_ACCOUNT WHERE SPID=? AND ACCOUNTID = ? AND CUSTOMERID = (SELECT DISTINCT CUSTOMERID FROM CUSTOMER_LOOKUP WHERE TPID = ?)";
        
	/**
	 *  This retrieves the lrn from soa_npanxx_lrn table based on npanxx.
	 */
	public static final String GET_LRN=
		"SELECT LRN FROM SOA_NPANXX_LRN WHERE SPID = ? AND NPANXX=?";

	/**
	 *  This retrieves the lrn from soa_npanxx_lrn table based on spid.
	 */
	public static final String LIST_GTT_LRN=
		"SELECT DISTINCT LRN FROM SOA_LRN WHERE SPID=? AND GTTID IS NOT NULL";


	 /**
    * This query is used to check LRN exists for SPID or not
    */
   public static final String EXISTS_SVID =
       "SELECT /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ NNSP, STATUS, NNSPDUEDATE, BUSINESSTIMERTYPE, ONSP, PORTINGTN, CAUSECODE, ONSPDUEDATE, PORTINGTOORIGINAL, AUTHORIZATIONFLAG, OBJECTCREATIONDATE, LASTREQUESTTYPE, LASTPROCESSING, NNSPSIMPLEPORTINDICATOR, ONSPSIMPLEPORTINDICATOR, ACTIVATETIMEREXPIRATION, LRN, LNPTYPE FROM SOA_SUBSCRIPTION_VERSION WHERE SPID = ? AND " +
       " SVID = ? AND REGIONID = ?";

   /**
	 *  This query retrieves queue id for inserting into the respective tables.
	 */

	public static final String QUEUE_ID_QUERY = "SELECT SOARequestQueueId.nextval from dual";

	/**
	 *  This insert query for SOA_MESSAGE_MAP.
	 */
	public static final String INSERT_SOA_MAP_LOG = "INSERT INTO SOA_MESSAGE_MAP (MESSAGEKEY, REFERENCEKEY) VALUES ( ?, ?)";

	/**
	 *  This insert query for SOA_ACCOUNT_SV_UPDATE.
	 */
	public static final String SOA_ACCOUNT_SV_UPDATE =
		"UPDATE SOA_SUBSCRIPTION_VERSION SET ACCOUNTID = ? , ACCOUNTNAME = ? WHERE REFERENCEKEY = ?";

	/**
	 *  This update query for SOA_ACCOUNT_SV_UPDATE for API on the basis of SPID, SVID, and PORTINGTN.
	 */
	public static final String SOA_ACCOUNT_SV_UPDATE_FOR_API_1 = 
		"UPDATE /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_1) */ SOA_SUBSCRIPTION_VERSION SET ACCOUNTID = ? , ACCOUNTNAME = ? " +
                "WHERE SPID = ? AND SVID = ? AND REGIONID = ?";
     
        /**
	 *  This update query for SOA_ACCOUNT_SV_UPDATE for API on the basis of SPID, ONSP, NNSP, and PORTINGTN.
	 */
	public static final String SOA_ACCOUNT_SV_UPDATE_FOR_API_2 = 
		"UPDATE /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ SOA_SUBSCRIPTION_VERSION SET ACCOUNTID = ? , ACCOUNTNAME = ? " +
                "WHERE SPID = ? AND ONSP = ? AND NNSP = ? AND PORTINGTN = ? " +
                "AND STATUS NOT IN ('old')";
        
        /**
	 *  This update query for SOA_ACCOUNT_RANGE_SV_UPDATE.
	 */
	public static final String SOA_ACCOUNT_RANGE_SV_UPDATE = 
		"UPDATE /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ SOA_SUBSCRIPTION_VERSION SET ACCOUNTID = ? , ACCOUNTNAME = ? WHERE RANGEID = ?";

	/**
	 *  This update query for SOA_ACCOUNT_RANGE_SV_REMOVE.
	 */
	public static final String SOA_ACCOUNT_RANGE_SV_REMOVE = 
		"UPDATE /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13 SOA_SV_INDEX_2) */ SOA_SUBSCRIPTION_VERSION SET ACCOUNTID = ? , ACCOUNTNAME = ? WHERE RANGEID = ? AND PORTINGTN >= ? AND PORTINGTN <=? ";
        
        /**
	 *  This update query for SOA_ACCOUNT_RANGE_SV_UPDATE_FOR_API.
	 */
	public static final String SOA_ACCOUNT_RANGE_SV_UPDATE_FOR_API = 
		"UPDATE /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_13) */ SOA_SUBSCRIPTION_VERSION SET ACCOUNTID = ? , ACCOUNTNAME = ? WHERE SPID = ? AND RANGEID = ? ";


	 /**
    * This retrieves the NPA and NXX  from SOA_NBRPOOL_BLOCK table based on NPBID
    * and REGIONID
    */
    public static final String IS_NPB_NPANXX_QUERY =
       "SELECT NPA, NXX FROM SOA_NBRPOOL_BLOCK WHERE NPBID = ? AND REGIONID = ? ";

    // added MULTITNSEQUENCEID column in 5.6.2 release
  	public static final String SOA_SV_MESSAGE_SYNC_INSERT = "insert into SOA_SV_MESSAGE"
		+ " (MESSAGEKEY,MESSAGETYPE,MESSAGESUBTYPE,CUSTOMERID,SPID,STATUS,CREATEDBY,PORTINGTN,DATETIME,INTERFACEVERSION,USERID,MESSAGE,INVOKEID,REGIONID,SUBDOMAINID,INPUTSOURCE,MULTITNSEQUENCEID) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


	/**
	 *  This retrieves the accountid from soa_account table based on spid and accountid.
	 */
	public static final String EXISTS_ACCOUNTID="SELECT DISTINCT ACCOUNTID FROM SOA_ACCOUNT WHERE ACCOUNTID = ? AND SPID = ?";
        
        /**
	 *  This retrieves the accountid from soa_account table based on accountid and account name.
	 */
	public static final String EXISTS_ACCOUNTID_ACCOUNTNAME="SELECT ACCOUNTID FROM SOA_ACCOUNT WHERE ACCOUNTID = ? AND ACCOUNTNAME = ?";

	/**
	 *  This retrieves the spid from SOA_NANC_SUPPORT_FLAGS table based on spid and NANC400FLAG.
	 */
	 public static final String SPID_NANC400_FLAG="SELECT SPID FROM SOA_NANC_SUPPORT_FLAGS WHERE SPID =? AND NANC400FLAG='1'";
	 
    /**
	 *  This retrieves ALTBIDNANC436FLAG,ALTEULTNANC436FLAG,ALTEULVNANC436FLAG from SOA_NANC_SUPPORT_FLAGS table based on spid
	 */	 
	 public static final String SPID_NANC436_FLAGS="SELECT ALTBIDNANC436FLAG,ALTEULTNANC436FLAG,ALTEULVNANC436FLAG FROM SOA_NANC_SUPPORT_FLAGS WHERE SPID=?";
	 
	 
	 /**
	  *  This retrieve the subdomain value for Daomain , subdomain, Alternative SPid combination.
	  */

	 public static final String SUBDOMAINID_ALTSPID_QUERY=" SELECT ALTERNATIVESPID FROM SOA_SUBDOMAIN_ALTSPID WHERE CUSTOMERID = ? AND ALTERNATIVESPID = ?  AND SUBDOMAINID = ? ";
	 
	 /**
	  *  This retrieve the subdomain value for Daomain and Alternative SPid combination.
	  */
	 public static final String DOMAIN_SUBDOMAINID_ALTSPID_QUERY=
		 " SELECT SUBDOMAINID FROM CUSTOMER_LOOKUP WHERE TPID = ? AND CUSTOMERID = ? AND SUBDOMAINID IN "+
		 "( SELECT SUBDOMAINID FROM SOA_SUBDOMAIN_ALTSPID WHERE CUSTOMERID = ? AND ALTERNATIVESPID = ? ) ";
	 
	/**
	 *  This retrieves the regionid and recoveryflag from RECOVERY_PROCESS table.
	 */
	 public static final String SOA_RECOVERY_PROCESS="SELECT REGIONID, RECOVERYFLAG FROM SOA_RECOVERY_PROCESS";
	
	 /**
	  *  This retrieves the DATETIME from LAST_NPAC_RESPONSE_TIME table.
	  */
     public static final String SOA_CONNECTIVITY_LAST_NPAC_RESPONSE="SELECT DATETIME FROM LAST_NPAC_RESPONSE_TIME WHERE SPID = ? AND REGIONID = ? AND TYPE = ?";
	 
     /**
	  *  This retrieves the NPACRESPONSETIMEFLAG from SOA_RECOVERY_FLAGS table.
	  */
     public static final String SOA_RECOVERY_FLAGS="SELECT NPACRESPONSETIMEFLAG FROM SOA_RECOVERY_FLAGS WHERE SPID = ? ";
	
	 /**
	  *  This check the TPID from CUSTOMERLOOKUP for APT , SUBDOMAIN Level User.
	  */
	 
	 public static final String DOMAIN_SUBDOMAIN_INITSPID_QUERY = "SELECT TPID FROM CUSTOMER_LOOKUP WHERE CUSTOMERID = ? AND SUBDOMAINID = ? AND TPID = ? ";

	 /**
	  *  This check the TPID from CUSTOMERLOOKUP for APT , SUBDOMAIN Level User.
	  */
	 
	 public static final String CONNECTIVITY_SELECT = "SELECT KEY FROM PERSISTENTPROPERTY WHERE KEY LIKE 'SOA_CONNECTIVITY%' AND PROPERTYTYPE = 'NPAC Com Server' AND ( PROPERTYNAME LIKE 'SECONDARY_SPID%' OR PROPERTYNAME LIKE 'PRIMARY_SPID_%') AND PROPERTYVALUE = ? ";
	 
	 //Changes are made in 5.6.5 release
	 /**
	    * This query is used to validate NPA NXX belongs to canada region.
	    */
	   public static final String VALIDATE_NPA_NXX_BELONG_TO_CANADA_QUERY =
	       "SELECT /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_1) */ NPA FROM SOA_NPANXX WHERE NPA = ? AND NXX = ? " +
	       " AND REGIONID = 7 AND STATUS = 'ok'";
	   
	   /**
	    * This query is used to validate NPA NXX belongs to canada region.
	    */
	   public static final String VALIDATE_NPB_BELONG_TO_CANADA_QUERY =
	       "SELECT /*+ INDEX(SOA_NBRPOOL_BLOCK SOA_NBRPOOL_BLOCK_INDEX_1) */ NPA FROM SOA_NBRPOOL_BLOCK WHERE NPBID = ? " +
	       " AND REGIONID = 7 AND STATUS = 'active'";
	   
	   /**This query return value if spid belongs to US & CN both region.
	    * 
	    */
	   public static final String GET_SPID_BELONG_TO_US_CN_QUERY = "SELECT COUNT(REGIONID) FROM SOA_SPID WHERE SPID IN (SELECT SPID FROM SOA_SPID WHERE REGIONID=7 AND SPID = ?) " +
	   		" GROUP BY SPID HAVING COUNT(REGIONID) > 1";
	   /**
	    * This query is used to check whether any SV exists with active LRN or not.
	    */
	   public static final String GET_SVEXISTS_WITH_ACTIVELRN = "select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN " +
	   		"from SOA_SUBSCRIPTION_VERSION where PORTINGTN like ? and LRN not in ('0000000000') and status " +
	   		"not in ('old','canceled','NPACCreateFailure')";
	   
	   /**
	    * This query is used to check whether any SV exists with pseudo LRN or not.
	    */
	   public static final String GET_SVEXISTS_WITH_PSEUDOLRN = "select /*+ INDEX(SOA_SUBSCRIPTION_VERSION SOA_SV_INDEX_2) */ PORTINGTN " +
	   		"from SOA_SUBSCRIPTION_VERSION where PORTINGTN like ? and LRN in ('0000000000') and status " +
	   		"not in ('old','canceled','NPACCreateFailure')";
	   
	   /**
	    * This query is used to validate NPA NXX belongs to US region.
	    */
	   public static final String VALIDATE_NPA_NXX_BELONG_TO_US_QUERY =
	       "SELECT /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_1) */ NPA FROM SOA_NPANXX WHERE NPA = ? AND NXX = ? " +
	       " AND REGIONID IN(0,1,2,3,4,5,6) AND STATUS = 'ok'";
	   
	   public static final String GET_NPA_NXX_FOR_SPID = "select /*+ INDEX(SOA_NPANXX SOA_NPANXX_INDEX_1) */ NPA from SOA_NPANXX " +
  		"where NPA = ? and NXX = ? and SPID = ? and REGIONID = ? and status = 'ok' ";
	   
	   public static final String GET_LRN_FOR_NPB = "select /* INDEX(SOA_NBRPOOL_BLOCK SOA_NBRPOOL_BLOCK_INDEX_1) */ LRN from SOA_NBRPOOL_BLOCK " +
 		"where NPA = ? and NXX = ? and dashx = ? and SPID = ? and REGIONID = ? and status = 'active' ";
}

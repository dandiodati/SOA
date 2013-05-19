////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2004 NeuStar, Inc. All rights reserved. The source code
// provided herein is the exclusive property of NeuStar, Inc. and is considered
// to be confidential and proprietary to NeuStar.
//
////////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.rules;

import com.nightfire.spi.neustar_soa.rules.actions.*;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.w3c.dom.Document;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.rules.*;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import java.util.TimeZone;


/**
 * This class provides an API for the SOA GUI to use business rules
 * in order to determine what GUI actions (request types) should be
 * allowed.
 *
 */
public class AllowableActions {


    /**
	 * The array of possible requests that can be sent for a port in
	 * subscription version.
	 */
   	private static Action[] possiblePortinActions;

   	/**
	 * The array of possible requests that can be sent for a port out
	 * subscription version.
	 */
   	private static Action[] possiblePortoutActions;

   	/**
	 * The array of possible requests that can be sent for an intraport
	 * subscription version.
	 */
   	private static Action[] possibleIntraportActions;

   	   	/**
     * The array of possible requests that can be sent for an Lrn .
     */
   	private static Action[] possibleLrnActions;
   
   	/**
     * The array of possible requests that can be sent for an Npa Nxx .
     */
   	private static Action[] possibleNpaNxxActions;
  
   	/**
     * The array of possible requests that can be sent for an Number Pool Block .
     */
   	private static Action[] possibleNpbActions;
   
   	/**
     * The array of possible requests that can be sent for an Audit .
     */
   	private static Action[] possibleAuditActions;
   
   	/**
     * The array of possible requests that can be sent for an Gtt .
     */
   	private static Action[] possibleGttActions;
   	
	/**
	 * The array of possible requests that can be sent for an Service Provider .
	 */
	private static Action[] possibleSPActions;
	
	/**
	 * The array of possible requests that can be sent for an Npa Nxx X .
	 */
	private static Action[] possibleNpaNxxXActions;


   	// this initializes all of the possible action arrays
   	static{

	    possibleLrnActions = new Action[3];
		possibleLrnActions[0] = new LrnCreateRequest();			
		possibleLrnActions[1] = new LrnDeleteRequest();	
		possibleLrnActions[2] = new LrnQueryRequest();
		
		possibleNpaNxxActions = new Action[3];
		possibleNpaNxxActions[0] = new NpaNxxCreateRequest();		
		possibleNpaNxxActions[1] = new NpaNxxDeleteRequest();	
		possibleNpaNxxActions[2] = new NpaNxxQueryRequest();
		
		possibleNpbActions = new Action[3];	
		possibleNpbActions[0] = new NumberPoolBlockActivateRequest();				
		possibleNpbActions[1] = new NumberPoolBlockModifyRequest();		
		possibleNpbActions[2] = new NumberPoolBlockQueryRequest();
		
		possibleAuditActions = new Action[3];
		possibleAuditActions[0] = new AuditCreateRequest();			
		possibleAuditActions[1] = new AuditCancelRequest();		
		possibleAuditActions[2] = new AuditQueryRequest();
		
		possibleGttActions = new Action[2];			
		possibleGttActions[0] = new GttModifyRequest();		
		possibleGttActions[1] = new GttDeleteRequest();
		
		possibleSPActions = new Action[1];			
		possibleSPActions[0] = new ServiceProviderQueryRequest();		
		
		possibleNpaNxxXActions = new Action[1];			
		possibleNpaNxxXActions[0] = new NpaNxxXQueryRequest();

   }

    /**
	 * A possible value for the service type. This indicates that
	 * the record is a SOAPortIn subscription version.
	 */
    public static final String PORTIN_SERVICE_TYPE = "SOAPortIn";

    /**
	 * A possible value for the service type. This indicates that
	 * the record is a SOAPortOut subscription version.
	 */
    public static final String PORTOUT_SERVICE_TYPE = "SOAPortOut";

    /**
	 * A possible value for the service type. This indicates that
	 * the record is a SOAIntraport subscription version.
	 */
    public static final String INTRAPORT_SERVICE_TYPE = "SOAIntraPort";
	
	/**
	 * A possible value for the service type. This indicates that
	 * the record is a SOA Lrn.
	 */
    public static final String LRN_SERVICE_TYPE = "SOA-Lrn";
   
	/**
	 * A possible value for the service type. This indicates that
	 * the record is a SOA NpaNxx.
	 */
    public static final String NPANXX_SERVICE_TYPE = "SOA-NpaNxx";
	
	/**
	 * A possible value for the service type. This indicates that
	 * the record is a SOA Number Pool Block.
	 */
    public static final String NPB_SERVICE_TYPE = "SOA-NumberPoolBlock";
   
	/**
	 * A possible value for the service type. This indicates that
	 * the record is a SOA Audit.
	 */
    public static final String AUDIT_SERVICE_TYPE = "SOA-Audit";
   
	/**
	 * A possible value for the service type. This indicates that
	 * the record is a SOA Gtt.
	 */
    public static final String GTT_SERVICE_TYPE = "SOA-Gtt";
    
	/**
	 * A possible value for the service type. This indicates that
	 * the record is a SOA Service Provider.
	 */
	public static final String SP_SERVICE_TYPE = "SOA-ServiceProvider";
	
	/**
	 * A possible value for the service type. This indicates that
	 * the record is a SOA NpaNxx X.
	 */
	public static final String NPANXX_X_SERVICE_TYPE = "SOA-NpaNxxX";
	
	/**
	 * Query to retrieve the customeID of particular LRN
	 */
	public static final String LRN_CUSTOMERID_QUERY = "SELECT CUSTOMERID FROM CUSTOMER_LOOKUP WHERE TPID IN ( SELECT SPID FROM SOA_LRN WHERE LRN = ? AND STATUS = 'ok'  AND REGIONID = ? AND SPID = ?)";
	
	/**
	 * Query to retrieve the roleID associated with any USER.
	 */
	public static final String GET_USER_PERMISSIONID_QUERY = "SELECT permissionid FROM rolepermissions WHERE roleid IN (SELECT roleid FROM userroles WHERE userid IN (SELECT userid FROM clearinghouseuser WHERE username = ? and customerid = ?)  union select SUBROLEID from ROLESUBROLE where roleid in (SELECT roleid FROM userroles WHERE userid IN (SELECT userid FROM clearinghouseuser WHERE username = ? AND customerid = ?)))";



	/**
	 * This gets the array of the actions that the user is allowed to
	 * take for the record described by the fields in the given XML
	 * document.
	 *
	 * The rule classpath used to discover the rule sets will be taken from
	 * the repository under &lt;install_root>/repository/rules/soa.xml
	 *
	 * @param guiXML Document this XML document contains all of the
	 *                        GUI query results for a particular DB
	 *                        record. This is a very loosely coupled API,
	 *                        but this is the easiest way for the GUI
	 *                        to pass the input parameters.
	 * @return String[] an array containing the names of the allowable
	 *                  actions/requests for the given record.
	 * @exception MessageException if an error occurs
	 */
	public static List getAllowableActions(Document guiXML)
										  throws MessageException{

		return getAllowableActions(guiXML,null);
	}
    


   
	/**
	 * This gets the array of the actions that the user is allowed to
	 * take for the record described by the fields in the given XML
	 * document.
	 *
	 * The rule classpath used to discover the rule sets will be taken from
	 * the repository under &lt;install_root>/repository/rules/soa.xml
	 *
	 * @param guiXML Document this XML document contains all of the
	 *                        GUI query results for a particular DB
	 *                        record. This is a very loosely coupled API,
	 *                        but this is the easiest way for the GUI
	 *                        to pass the input parameters.
	 * @param parentLoader The parent class loader used to find classes.
	 * @return String[] an array containing the names of the allowable
	 *                  actions/requests for the given record.
	 * @exception MessageException if an error occurs
	 */
	public static List getAllowableActions(Document guiXML, 
										   ClassLoader parentLoader)
										  throws MessageException{

	  String ruleClasspath = null;

	  try{

		 String xml = RepositoryManager.getInstance().getMetaData("rules",
																  "soa");

		 XMLMessageParser classpathInfo = new XMLMessageParser(xml);
		 ruleClasspath = classpathInfo.getValue("classpath");

	  }
	  catch(Exception ex){

		 Debug.error("Could not get SOA rule classpath from repository: "+
					 ex.toString());
		 Debug.logStackTrace(ex);
		 return new ArrayList();

	  }

	  return getAllowableActions( guiXML, ruleClasspath, parentLoader );

   }

   /**
	* This gets the array of the actions that the user is allowed to
	* take for the record described by the fields in the given XML
	* document.
	*
	*
	* @param guiXML Document this XML document contains all of the
	*                        GUI query results for a particular DB
	*                        record. This is a very loosely coupled API,
	*                        but this is the easiest way for the GUI
	*                        to pass the input parameters.
	* @param ruleClasspath this classpath will be used to discover the
	* @param parentLoader The parent class loader used to find classes.
	*                      rule sets to be applied.
	* @return List a List containing the String names of the allowable
	*              actions/requests for the given record.
	*/
   public static List getAllowableActions(Document guiXML,
										  String ruleClasspath,
										  ClassLoader parentLoader)
										  throws MessageException{

	   Context context = new Context(guiXML);	  

	   return getAllowableActions(context, ruleClasspath, parentLoader);

	}


   /**
	* This gets the array of the actions that the user is allowed to
	* take for the record described by the fields in the given XML
	* document.
	*
	*
	* @param context Context contains all of the
	*                        GUI query results for a particular DB
	*                        record.
	* @param ruleClasspath this classpath will be used to discover the
	*                      rule sets to be applied.
	* @param parentLoader The parent class loader used to find classes.
	* @return List a List containing the String names of the allowable
	*              actions/requests for the given record.
	*/
   public static List getAllowableActions(Context context,
										  String ruleClasspath,
										  ClassLoader parentLoader)
										  throws MessageException{

	  // the list to which the names of allowable actions will be added
	  List allowableActions = new ArrayList();

	  // check the service type
	  String serviceType = context.getServiceType();
	  
	
	  if(serviceType.equals(PORTIN_SERVICE_TYPE)) {

		  getPortInAllowableActions( context,
				  					 allowableActions);
							  
	  }
	  else if( serviceType.equals(PORTOUT_SERVICE_TYPE) ){

		  getPortOutAllowableActions( context,
							  		  allowableActions);

	  }
	  else if( serviceType.equals(INTRAPORT_SERVICE_TYPE) ){

		  getIntraPortAllowableActions(context,
							  		   allowableActions);							  		   

	  }else if( serviceType.equals(LRN_SERVICE_TYPE) ){

		   getAllowableActions( serviceType,
								possibleLrnActions,
								context,
								allowableActions,
								ruleClasspath ,
								parentLoader);

		}
		else if( serviceType.equals(NPANXX_SERVICE_TYPE) ){
			
		 getAllowableActions( serviceType,
							  possibleNpaNxxActions,
							  context,
							  allowableActions,
							  ruleClasspath ,
							  parentLoader);

	  }else if( serviceType.equals(NPB_SERVICE_TYPE) ){
		
		   getAllowableActions( serviceType,
								possibleNpbActions,
								context,
								allowableActions,
								ruleClasspath ,
								parentLoader);

		}else if( serviceType.equals(AUDIT_SERVICE_TYPE) ){
			
			 getAllowableActions( serviceType,
								  possibleAuditActions,
								  context,
								  allowableActions,
								  ruleClasspath ,
								  parentLoader);

		}else if( serviceType.equals(GTT_SERVICE_TYPE) ){
			
		  	getAllowableActions( serviceType,
							     possibleGttActions,
							     context,
							     allowableActions,
							     ruleClasspath ,
								 parentLoader);

	   }else if( serviceType.equals(SP_SERVICE_TYPE) ){
	   		
		 	getAllowableActions( serviceType,
							     possibleSPActions,
							  	 context,
							  	 allowableActions,
							  	 ruleClasspath ,
								 parentLoader);

  		}else if( serviceType.equals(NPANXX_X_SERVICE_TYPE) ){
			
			getAllowableActions( serviceType,
						  		 possibleNpaNxxXActions,
						  		 context,
						  		 allowableActions,
						  		 ruleClasspath ,
							   	 parentLoader);

	}

	  return allowableActions;

   }

   /**
	* This gets the allowable actions for the given service type from
	* a set of possible actions.
	*
	* @param serviceType String the service type for which the allowable
	*                           actions should be found.
	* @param possibleActions Action[] the set of possible actions for this
	*                                 service type.
	* @param context Context access to the parsed XML input from the GUI.
	* @param allowableActions List the names of any allowable actions will
	*                         be added to this list.
	* @param ruleClasspath String the classpath that will be searched to
	*                             discover rule sets that will be used
	*                             to determine
	* @param parentLoader The parent class loader used to find classes.
	* @throws MessageException if an error occurs while setting of getting
	*                          values to or from XML.
	*/
   protected static void getAllowableActions(String serviceType,
											 Action[] possibleActions,
											 Context context,
											 List allowableActions,
											 String ruleClasspath,
											 ClassLoader parentLoader)
											 throws MessageException{
	  
	   
	   List permissionlst = getPermissionList();
	   if((serviceType.equals(AUDIT_SERVICE_TYPE) && permissionlst.contains("32"))
			   || (!serviceType.equals(AUDIT_SERVICE_TYPE) && permissionlst.contains("41") && permissionlst.contains("35"))
			   			||  permissionlst.contains("506") || permissionlst.contains("0")){ 
		   
		   for(int i = 0; i < possibleActions.length; i++){
	
			 try{
	
				// create an XML version of the current possible action
				Document doc = possibleActions[i].getRequestDocument(serviceType,
																	 context);
			
				XPathAccessor parsedRequest = new CachingXPathAccessor( doc );
	
				// get the list of the evaluator class names to execute
				List evaluatorNames =
					RuleSetLocator.getRuleSetEvaluators(ruleClasspath, parentLoader);
	
				Iterator names = evaluatorNames.iterator();
	
				String[] classpath =
				   RuleSetLocator.getClasspathTokens(ruleClasspath);
	
				boolean isSubDomainLevelUser = false;
				
				String subdomain = CustomerContext.getInstance().getSubDomainId();
				
				if( subdomain != null && !(subdomain.equals("")) )
				{
					  isSubDomainLevelUser = true;
				}
				
				boolean success = true;
				ErrorCollection errors = new ErrorCollection();
	
				// To Fix the TD #6120, create RuleContext object and pass it to RuleEngine.evaluate()
				RuleContext rContext = new RuleContext();
				rContext.setDBConnection(rContext.getDBConnection());
				// execute the business rules against the current request type
				while (names.hasNext() && success) {
	
				   String classname = (String) names.next();		   
	
				   success = RuleEngine.evaluate(classname,
												 parsedRequest,
												 errors,
												 classpath,
												 parentLoader,
												 rContext);
				   
				   // To fix the TD#6564.
				   if( serviceType.equals(NPANXX_SERVICE_TYPE) && isSubDomainLevelUser )
					   success = false;
				   
				}
				
				// To fix the TD #6565, clean up the rule context.
				rContext.cleanup();
				if (possibleActions[i].getName().equals(SOAConstants.LRN_DELETE_REQUEST)){
					
				      Connection conn = null;
	
				      PreparedStatement pstmt = null;
				      
				      ResultSet results = null;
	
				      try{
	
				         conn = rContext.getDBConnection();
				         
				         if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
				        	 Debug.log(Debug.NORMAL_STATUS, "SQL to get CustomerID :\n"+AllowableActions.LRN_CUSTOMERID_QUERY);
				         
				         pstmt = conn.prepareStatement(AllowableActions.LRN_CUSTOMERID_QUERY);
				         pstmt.setString(1 ,context.getValue("Lrn"));
				         pstmt.setString(2 ,context.getValue("RegionId"));
				         pstmt.setString(3 ,context.getValue("SPID"));
				         results = pstmt.executeQuery();
				         results.next();
				         String cust_ID = results.getString("CUSTOMERID");
				         String currentCust_ID =CustomerContext.getInstance().getCustomerID();
				         if (cust_ID.equals(currentCust_ID)){
				        	 success = true;
				         }
				      			         
						 
	
				      }catch(Exception exception){
				    	  
				    	  if ( Debug.isLevelEnabled( Debug.RULE_EXECUTION )){
				    		  
				    		  Debug.log(Debug.RULE_EXECUTION, "Exception while extracting customerID from DB.");
					    	  Debug.log(Debug.RULE_EXECUTION, "Exception:"+exception.getMessage());
				    	  }
				    		  
	       
				      }finally {
				    	 if(results != null){
				    		 try{
				    			 results.close();
				    		 }catch (SQLException e) {
								Debug.log(Debug.MSG_STATUS,"Error:"+e.getMessage());
							 }
				    	 }
				    	 if(pstmt != null){
				    		 try{
				    			 pstmt.close();
				    		 }catch (SQLException e) {
									Debug.log(Debug.MSG_STATUS,"Error:"+e.getMessage());
							}
				    	 }
				    	 if (conn != null){
				    		 try{
				    			 conn.close();
				    		 }catch (SQLException e) {
									Debug.log(Debug.MSG_STATUS,"Error:"+e.getMessage());
				    		 }
				    	 }
					 }
				}
				// if there were no rule errors, then add the possible action
				// to the list of allowable actions
				if (success) {
					
					if ( Debug.isLevelEnabled( Debug.MSG_STATUS ))
						Debug.log(Debug.RULE_EXECUTION, "Action:["+possibleActions[i].getName()+"]");
					
				   allowableActions.add(possibleActions[i].getName());
				}
				else{
	
				   if( Debug.isLevelEnabled(Debug.RULE_EXECUTION) ){
	
					  Debug.log(Debug.RULE_EXECUTION,
								"The action ["+possibleActions[i].getName()+
								"] is not allowed because of the following "+
								"rules:\n"+
								errors.toString());
	
				   }
	
				}
	
			 }
			 catch(FrameworkException fex){
	
				Debug.error("Could not determine if ["+possibleActions[i]+
							"] is an allowable action: "+fex );
	
			 }
	
		  }
	   }
	 

   }
   
   /**
	* This gets the array of the actions that the user is allowed fro
	* PORT IN Service type based on status fields in the given XML
	* document.
	*
	*
	* @param context Context contains all of the
	*                        GUI query results for a particular DB
	* @param List a List containing the String names of the allowable
	*              actions/requests for the given record.
	*/
   
   protected static void getPortInAllowableActions(	Context context,
		   											List allowableActions)
   													throws MessageException{
	   
	   try{
		   List permissionlst = getPermissionList();
		   if(permissionlst.contains("32") || permissionlst.contains("506")
				   || permissionlst.contains("0")){
			   
			   String status = context.getStatus();
	            
			   	if(status.equalsIgnoreCase(SOAConstants.CREATING_STATUS) ||
					status.equalsIgnoreCase(SOAConstants.NPACCREATFAILURE_STATUS)){
			   		allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
			   		allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
			   	}
				else if(status.equalsIgnoreCase(SOAConstants.PENDING_STATUS)){
	            	allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
					allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);
					allowableActions.add(SOAConstants.SV_MODIFY_REQUEST);
					allowableActions.add(SOAConstants.SV_ACTIVATE_REQUEST);
					allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.ACTIVE_STATUS)){
					allowableActions.add(SOAConstants.SV_MODIFY_REQUEST_ACTIVE);
	            	allowableActions.add(SOAConstants.SV_DISCONNECT_REQUEST);
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.CANCELED_STATUS)){
	            	allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.CANCEL_PENDING_STATUS)){
					allowableActions.add(SOAConstants.SV_MODIFY_REQUEST_CANCEL_PENDING);
	            	allowableActions.add(SOAConstants.SV_CANCEL_AS_NEW_REQUEST);
	              	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.OLD_STATUS)){
	            	allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.DISCONNECT_PENDING_STATUS)){
	    			allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);
	        		allowableActions.add(SOAConstants.SV_MODIFY_REQUEST_DISCONNECT_PENDING);
	                allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.CONFLICT_STATUS)){
					allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
	            	allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);
	                allowableActions.add(SOAConstants.SV_MODIFY_REQUEST);
	                allowableActions.add(SOAConstants.SV_REMOVE_FROM_CONFLICT_REQUEST);
	                allowableActions.add(SOAConstants.SV_QUERY_REQUEST);                	
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.OLD_STATUS)){
	            	allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS)){
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.DOWNLOAD_FAILED_STATUS)){
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);                	
	            }
			 }
	   }catch(FrameworkException fex){
               Debug.error("Not able to creat list of allowable actions " +
               		"for PORT IN service " + fex );
	   }
   }     
   
   /**
	* This gets the array of the actions that the user is allowed fro
	* PORT OUT Service type based on status fields in the given XML
	* document.
	*
	*
	* @param context Context contains all of the
	*                        GUI query results for a particular DB
	* @param List a List containing the String names of the allowable
	*              actions/requests for the given record.
	*/
   
   protected static void getPortOutAllowableActions(Context context,
           											List allowableActions)
   													throws MessageException{
	   List permissionlst = getPermissionList();
	   try{
		   if(permissionlst.contains("32") || permissionlst.contains("506")
				   || permissionlst.contains("0")){
			    String status = context.getStatus();
	            if(status.equalsIgnoreCase(SOAConstants.PENDING_STATUS)){
	            	allowableActions.add(SOAConstants.SV_RELEASE_REQUEST);
	            	allowableActions.add(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST);
	            	allowableActions.add(SOAConstants.SV_MODIFY_REQUEST);
	            	allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);                               
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.CONFLICT_STATUS)){
	            	allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);
	            	allowableActions.add(SOAConstants.SV_MODIFY_REQUEST);
	            	allowableActions.add(SOAConstants.SV_REMOVE_FROM_CONFLICT_REQUEST);
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.CANCELED_STATUS)){
	            	allowableActions.add(SOAConstants.SV_RELEASE_REQUEST);
	            	allowableActions.add(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST);
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.CANCEL_PENDING_STATUS)){
					allowableActions.add(SOAConstants.SV_MODIFY_REQUEST_CANCEL_PENDING);
	            	allowableActions.add(SOAConstants.SV_CANCEL_AS_OLD_REQUEST);
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.CREATING_STATUS) ||
					status.equalsIgnoreCase(SOAConstants.NPACCREATFAILURE_STATUS)){
	                allowableActions.add(SOAConstants.SV_RELEASE_REQUEST);
	                allowableActions.add(SOAConstants.SV_RELEASE_IN_CONFLICT_REQUEST);
	                allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
				else if(status.equalsIgnoreCase(SOAConstants.OLD_STATUS)){
	                allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.DOWNLOAD_FAILED_STATUS)){
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);                	
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS)){
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);                	
	            }
	            else if(status.equalsIgnoreCase(SOAConstants.ACTIVE_STATUS)){            	
	            	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);                	
	            }
		   }
	  	}catch(FrameworkException fex){
  					Debug.error("Not able to creat list of allowable actions " +
               		"for PORT OUT service " + fex );
  		}
   }              

   /**
	* This gets the array of the actions that the user is allowed fOR
	* INTRA PORT  Service type based on status fields in the given XML
	* document.
	*
	*
	* @param context Context contains all of the
	*                        GUI query results for a particular DB
	* @param List a List containing the String names of the allowable
	*              actions/requests for the given record.
	*/
   protected static void getIntraPortAllowableActions(Context context,
		   											  List allowableActions)
   													throws MessageException{
	   List permissionlst = getPermissionList();
        try{
    		String status = context.getStatus();
    		if(permissionlst.contains("32") || permissionlst.contains("506")
 				   || permissionlst.contains("0")){
	           if(status.equalsIgnoreCase(SOAConstants.PENDING_STATUS)){
				    allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);
	   				allowableActions.add(SOAConstants.SV_ACTIVATE_REQUEST);
	   				allowableActions.add(SOAConstants.SV_MODIFY_REQUEST);
	   				allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	           }
	           else if(status.equalsIgnoreCase(SOAConstants.ACTIVE_STATUS)){
	        	   	allowableActions.add(SOAConstants.SV_DISCONNECT_REQUEST);
	                allowableActions.add(SOAConstants.SV_MODIFY_REQUEST_ACTIVE);
	                allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	           }
	           else if(status.equalsIgnoreCase(SOAConstants.CANCELED_STATUS)){
	        	   	allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
	                allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	           }
	           else if(status.equalsIgnoreCase(SOAConstants.OLD_STATUS)){
	        	   	allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
	        	   	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	           }  
	           else if(status.equalsIgnoreCase(SOAConstants.CREATING_STATUS) || 
				   status.equalsIgnoreCase(SOAConstants.NPACCREATFAILURE_STATUS)){
	        	   	allowableActions.add(SOAConstants.SV_CREATE_REQUEST);
	               	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	           }
			   else if(status.equalsIgnoreCase(SOAConstants.DOWNLOAD_FAILED_PARTIAL_STATUS)){
	        	   	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);                	
	           }
	           else if(status.equalsIgnoreCase(SOAConstants.DOWNLOAD_FAILED_STATUS)){
	               	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);                	
	           }
	           else if(status.equalsIgnoreCase(SOAConstants.DISCONNECT_PENDING_STATUS)){
	           		allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);
	           		allowableActions.add(SOAConstants.SV_MODIFY_REQUEST_DISCONNECT_PENDING);
	           		allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	            }
	           else if(status.equalsIgnoreCase(SOAConstants.CANCEL_PENDING_STATUS)){
	        	   	allowableActions.add(SOAConstants.SV_CANCEL_REQUEST);
	        	   	allowableActions.add(SOAConstants.SV_QUERY_REQUEST);
	           }
    		}
        }catch(FrameworkException fex){
                      Debug.error("Not able to creat list of allowable actions " +
                         		"for INTRA PORT service " + fex );
        }
  }





   /**
	* This method sets the time zone that will be used by all actions
	* when generating date/time values.
	*
	* @param zone TimeZone the new time zone to use.
	*/
   public static void setTimeZone(TimeZone zone){

	  setTimeZone( zone, possiblePortinActions );
	  setTimeZone( zone, possiblePortoutActions );
	  setTimeZone( zone, possibleIntraportActions );

   }

   /**
	* This is a convenience method that sets the time zone that will be used
	* by the given actions when generating date/time values.
	*
	* @param zone TimeZone the new time zone to use.
	* @param actions Action[] the actions whose time zone will get set.
	*/
   private static void setTimeZone(TimeZone zone,
								   Action[] actions){

	  int count = actions.length;

	  for(int i = 0; i < count; i++){
		 actions[i].setTimeZone( zone );
	  }

   }
   /*
    * This method returns the list of permissions assigned to the login user.
    */
   public static List<String> getPermissionList(){
	   
	   Set<String> permissionSet = new HashSet<String>();
	   Connection conn = null;
	   PreparedStatement pstmt = null;
	   ResultSet results = null;
	   List <String> permissionIdLst = new ArrayList<String>();
	   try{
		   conn = DBConnectionPool.getInstance(true).acquireConnection();
		   
		   if (conn == null) {
				// Throw the exception to the driver.
				throw new ProcessingException(
						"DB connection is not available");
			}
		   
		   if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			   Debug.log(Debug.NORMAL_STATUS, "SQL to get PermissionID :\n"+AllowableActions.GET_USER_PERMISSIONID_QUERY);

		   pstmt = conn.prepareStatement(AllowableActions.GET_USER_PERMISSIONID_QUERY);
		   pstmt.setString(1, CustomerContext.getInstance().getUserID());
		   pstmt.setString(2, CustomerContext.getInstance().getCustomerID());
		   pstmt.setString(3, CustomerContext.getInstance().getUserID());
		   pstmt.setString(4, CustomerContext.getInstance().getCustomerID());
		   
		   results = pstmt.executeQuery();
		   
		   if(results != null ){
			
			   while(results.next()){
				  permissionSet.add(results.getString("PERMISSIONID"));
			   }
		   
			   permissionIdLst.addAll(permissionSet);
		   }
		   if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			   Debug.log(Debug.NORMAL_STATUS, "result["+permissionIdLst+"]");
	   
	   }catch (Exception e){
		   Debug.error("Not able to fetch list of permissions " + e );

	   }finally{
		   try {
	    		 if(results != null){
	    			 results.close();	
	    		 }
		   }catch (SQLException e) {
				Debug.log(Debug.MSG_STATUS,"Error:"+e.getMessage());
		   }
		   try{
	    		if(pstmt != null){
	    			pstmt.close();	
	    		}
		   }catch (SQLException e) {
				Debug.log(Debug.MSG_STATUS,"Error:"+e.getMessage());
		   }
		   try{
			   
			   if(conn != null){
				   DBConnectionPool.getInstance(true).releaseConnection(conn);
				   
			   }
			}catch (ResourceException e) {
				Debug.log(Debug.ALL_ERRORS, "Error in releasing the DB connection in " +
						"connection pool"+e.toString());
			}
	   }
	   return permissionIdLst;
   }

}
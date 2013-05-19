	/**
	 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
	 *
	 * $Header: //spi/neustar-soa/main/com/nightfire/spi/neustar_soa/servicehandler/SOALinkerServiceHandler.java#3 $
	 */

	 
	
	package com.nightfire.spi.neustar_soa.servicehandler;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.util.CommonConfigUtils;
import com.nightfire.framework.util.CustomerContext;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.mgrcore.businessobject.InvalidDataException;
import com.nightfire.mgrcore.im.DynamicQueryServiceHandler;
import com.nightfire.mgrcore.im.IMConstants;
import com.nightfire.mgrcore.im.IMContext;
import com.nightfire.mgrcore.im.IMInvalidDataException;
import com.nightfire.mgrcore.im.IMProcessingException;
import com.nightfire.mgrcore.im.IMSecurityException;
import com.nightfire.mgrcore.im.IMSystemException;
import com.nightfire.mgrcore.im.ServiceHandler;



	/**
	 * This class that executes queries that are dynamically constructed
	 * based on the input data and a query description obtained from the repository.
	 * The query definition will be selected based on the value of the configured test node.
	 */
	public class SOALinkerServiceHandler  extends DynamicQueryServiceHandler
	{

	    /**
	     * The node whose value will be used to find the query definition.
	     */
	    public static final String TEST_NODE_LOC = "TEST_NODE_LOC";

	    /**
	     * Method to intialize the service handler before processing requests.
	     *
	     * @param properties Properties of type name-value used in initialization.
	     *
	     * @exception IMProcessingException Thrown if processing fails.
	     */
	     public void initialize ( Map properties ) throws IMProcessingException
	     {
	        super.initialize ( properties );

	        //Get the location of the test node.
	        testNode = getRequiredPropertyValue ( TEST_NODE_LOC );

	     }//initialize

	    /**
	     * Get the information from the database as specified by the query criteria in the requestBody.
	     *
	     * @param context IMContext Control information for the request.
	     * @param requestBody   Body of the request.
	     *
	     * @return ResponseMessage A ResponseMessage object containing the response code and the response body.
	     *
	     * @exception IMInvalidDataException  Thrown if request data is bad.
	     * @exception IMSystemException  Thrown if server can't process any more requests due to system errors.
	     * @exception IMSecurityException  Thrown if access is denied.
	     * @exception IMProcessingException  Thrown if a transient processing error occurs.
	     */
	    public ServiceHandler.ResponseMessage process ( IMContext context, String requestBody )
	        throws IMInvalidDataException, IMSystemException, IMSecurityException, IMProcessingException
	    {
	        try
	        {
	            String action = context.getInvokingAction( );
	            
	            String strSessionId = new  String(String.valueOf(Math.random()));
	            
	            if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
	            	
	            	Debug.log(Debug.MSG_STATUS," requestBody : "+ requestBody);
	            }
	            
	            int indexServiceType = requestBody.indexOf("<ServiceType");
	            requestBody = requestBody.substring(0,indexServiceType) + "<SessionId value=\""+ strSessionId + "\" id=\"SessionId\"/>" 
	            			+ requestBody.substring(indexServiceType);
	            XMLMessageParser parser = new XMLMessageParser( requestBody );
	           	            
	            if (! parser.exists(testNode))
	            {
	                throw new IMInvalidDataException("ERROR: Missing required node [" +
	                                                 testNode + "] from the request.");
	            }

	            String testNodeValue = parser.getValue( testNode );

	            if ( Debug.isLevelEnabled( IMConstants.IM_SYSTEM_CONFIG ) )
	                Debug.log( IMConstants.IM_SYSTEM_CONFIG, "The value for test node is [" + testNodeValue + "]." );

	            String mappedAction = getPropertyValue( testNodeValue );

	            if ( Debug.isLevelEnabled( IMConstants.IM_SYSTEM_CONFIG ) )
	                Debug.log( IMConstants.IM_SYSTEM_CONFIG, "Action [" + action + "] maps to [" + mappedAction + "]." );

	            if (mappedAction == null)
	            {
	                throw new IMInvalidDataException("ERROR: The test node value [" +
	                                                 testNodeValue + "] is not recognized by the system.");
	            }

	            String strProcedure = null;
	            String requestType = null;
	            List reqParam = new ArrayList();
	                  	
	            if(action.equals("query-account-order-history") || action.equals("query-account-history"))
	            {
	            	strProcedure = "GET_ACCDETAIL";
	            	if (parser.exists("Info.AccountId"))
	            	reqParam.add(parser.getValue("Info.AccountId"));
	            	if (parser.exists("Info.SPID"))
	            	reqParam.add(parser.getValue("Info.SPID"));
	            	reqParam.add(context.getCustomerName());
	            	reqParam.add( strSessionId );
	            }else if(action.equals("query-range-order-history"))
	            {
	            	strProcedure = "GET_SUBRANGES";
	            	if (parser.exists("Info.RangeId"))
	            	reqParam.add(parser.getValue("Info.RangeId"));
                    reqParam.add( strSessionId );
	            }           	
	            //Adding SubDomain ID for all SV related actions
	            reqParam.add(CustomerContext.getInstance().getSubDomainId()); 
	            callProcedure(strProcedure,reqParam);
	            	
	            context.setInvokingAction ( mappedAction );
	            
	            if ( Debug.isLevelEnabled( Debug.MSG_DATA )){
	            	
	            	Debug.log(Debug.MSG_DATA," requestBody returned : "+requestBody);
	            }
	            
	            return super.process ( context, requestBody );
	        }
	        catch ( InvalidDataException e )
	        {
	            throw new IMInvalidDataException( this.getClass().getName() + ": " + e.toString() );
	        }
	        catch ( Exception e )
	        {
	            throw new IMProcessingException( this.getClass().getName() + ": " + e.toString() );
	        }
	    }

	    /**
	     * Allows for any pre-processing tasks to be carried out, specifically on the
	     * request body data.  This default implementation returns the request body
	     * untouched.
	     *
	     * @param  context            IMContext Control information for the request.
	     * @param  requestBody        Request body XML.
	     * @param  requestBodyParser  Parser of the request body.
	     *
	     * @exception  IMInvalidDataException  Thrown if the request data is bad.
	     *
	     * @return  Pre-processed request body.
	     */
	    protected String preProcess(IMContext context, String requestBody, XMLMessageParser requestBodyParser) throws IMInvalidDataException
	    {
	        return requestBody;
	    }

	    /**
	     * Clean up the service handler so that it can be re-used to process other requests.
	     *
	     * @exception IMProcessingException Thrown if cleanup is unsuccessful.
	     */
	    public void reset ( ) throws IMProcessingException
	    {
	        testNode = null;
	    }

	    private String testNode = null;

	    /**
		 * This static method is used for executing StroredProcedure for a given list of parameters
		 *  
		 * @param procedureName as String            
		 * @param parameter as List
		 * @throws DatabaseException
		 */
		public static void callProcedure(String procedureName, List parameter)	
		throws DatabaseException {

			Connection dbConn = null;
			CallableStatement proc = null;
			StringBuffer procSQL = new StringBuffer();

			try {
				// Get a database connection from the appropriate
				// location - based
				// on transaction characteristics.

				dbConn = DBConnectionPool.getInstance().acquireConnection();
							
				if (dbConn == null) {
					// Throw the exception to the driver.
					throw new DatabaseException("DB "
							+ "connection is not available");
				}
			} catch (FrameworkException fe) {
				String errMsg = "ERROR: Attempt to get database connection"
						+ " failed with error: " + fe.getMessage();

				if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
					Debug.log(Debug.ALL_ERRORS, errMsg);
				
				throw new DatabaseException(fe.getMessage());
			}

			//Generate SQL String
			procSQL.append(LEFT_BRACES);	//appending  {
			procSQL.append(PROCEDURE_CALL);	//appending CALL
			procSQL.append(SPACE);			//appending SPACE
			procSQL.append(procedureName);	//appending PROCEDURENAME
			procSQL.append(LEFT_P);			//appending (
			if(parameter != null){
				for(int i = 0; i < parameter.size(); i++) {
					procSQL.append(QUESTION_MARK);
					if(i != parameter.size()-1) {								
						procSQL.append(COMMA);
					}
					procSQL.append(SPACE);
				}
			}
			
			procSQL.append(RIGHT_P);			//appending )
			procSQL.append(RIGHT_BRACES);		//appending  }
			
			if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS )){
				
				Debug.log(Debug.NORMAL_STATUS, "Callable Statement Formed [" + procSQL.toString() + "]");
			}
			
			try {
				proc = dbConn.prepareCall(procSQL.toString());
				if(parameter != null) {
					for(int paramIndex = 0; paramIndex < parameter.size(); paramIndex++ ) {
						
						if ( Debug.isLevelEnabled( Debug.MSG_STATUS )){
							
							Debug.log(Debug.MSG_STATUS, "Value for slot [" + (paramIndex + 1) + 
									"] is [" + parameter.get(paramIndex) + "]");
						}
						proc.setObject(paramIndex + 1, parameter.get(paramIndex));				
					}
				}
				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS, "Executing Stored Procedure");

				proc.execute();
				
				if ( Debug.isLevelEnabled( Debug.NORMAL_STATUS ))
					Debug.log(Debug.NORMAL_STATUS, "Successfully Executed Stored Procedure");
				
			} catch (SQLException sqlex) {
				
				if ( Debug.isLevelEnabled( Debug.ALL_ERRORS ))
					Debug.log(Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqlex));
				
				Debug.logStackTrace(sqlex);
			} finally {
				try {
					if (proc != null)
						proc.close();
					proc = null;
					
					DBConnectionPool.getInstance().releaseConnection(dbConn);
					
					dbConn = null;
				} catch (Exception e) {
					Debug.logStackTrace(e);
					
					if ( Debug.isLevelEnabled( Debug.ALL_WARNINGS )){
						
						Debug.log(Debug.ALL_WARNINGS, "Unable to close the Callable Statement");
					}						
				} 
			}
		}
	    
	    private LinkedList actionList = new LinkedList();

		private static final String LEFT_BRACES = " { ";
		
		private static final String RIGHT_BRACES = " } ";
		
		private static final String SPACE = " ";
		
		private static final String PROCEDURE_CALL = "CALL"  + SPACE;
		
		private static final String QUOTE = " \" ";

		private static final String COMMA = " , ";
		
		private static final String ASTERISK = " * ";
		
		private static final String QUESTION_MARK = " ? ";
		
		private static final String LEFT_P = " ( ";
		
		private static final String RIGHT_P = " ) ";	    

	}


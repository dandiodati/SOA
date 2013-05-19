/**
 * The purpose of this processor is to create sequences(s) into the
 * database as per provided names and specified domain.
 *
 * @author Chirodip Pal
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is
 * considered to be confidential and proprietary to NeuStar.
 *
 * @see com.nightfire.common.ProcessingException
 * @see com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
 * @see com.nightfire.framework.message.MessageException;
 * @see com.nightfire.framework.util.Debug;
 * @see com.nightfire.framework.util.NVPair;
 * @see com.nightfire.framework.util.StringUtils;
 * @see com.nightfire.spi.common.driver.MessageObject;
 * @see com.nightfire.spi.common.driver.MessageProcessorContext;
 * @see com.nightfire.framework.resource.ResourceException;
 * @see com.nightfire.framework.db.DBConnectionPool;
 * @see com.nightfire.framework.db.DatabaseException;
 * @see com.nightfire.framework.db.PersistentProperty;
 * @see com.nightfire.framework.db.DBInterface;
 */

package com.nightfire.adapter.messageprocessor;

import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

import com.nightfire.common.ProcessingException;
import com.nightfire.adapter.messageprocessor.DBMessageProcessorBase;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.framework.db.DBConnectionPool;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.DBInterface;

/**
 
 Revision History
 ---------------
 
 Rev# Modified By   Date		Reason
 ----- ----------- ---------- --------------------------        
 1   Chirodip Pal   18/10/2005  Created
 
 */

public class DBSequenceGenerator extends DBMessageProcessorBase 
{
	/**
	* Property indicating name of the sequence name to be generated.
	*/
	public static final String SEQUENCE_NAME = "SEQUENCE_NAME";

	/**
	* Property indicating the customer domain to be prefixed with the sequence name.
	*/
	public static final String CUSTOMER_ID = "CUSTOMER_ID";

	/**
	* Property indicating the Minimum value or the starting value for the sequence.
	*/
	public static final String MIN_VALUE = "MIN_VALUE";

	/**
	* Property indicating the Maximum value or the ending value for the sequence.
	*/
	public static final String MAX_VALUE = "MAX_VALUE";
	
	/**
	* Property indicating whether processor should fail if sequence already exists.
	*/
	public static final String FAIL_ON_ERROR ="FAIL_ON_ERROR";
	
	/**
	 * Int value of oracle error code if the object already exists
	 */
	private static final int ORA_ERR_CODE = 955;
	/**
	* Class name used in logs
	*/
	public static final String CLASS_NAME = DBSequenceGenerator.class.getSimpleName();
	
	private String customerId = null;
	private String maxValue = null;
	private String minValue = null;
	private List<String> sequenceNames;
	StringBuffer errorBuffer = null;
	private List<Boolean> failOnErrors =null;

	/**
	* Constructor.
	*/
	public DBSequenceGenerator() 
	{
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
		  Debug.log(Debug.SYSTEM_CONFIG, "Creating DBSequenceGenerator message-processor.");
		
        sequenceNames = new ArrayList<String>();
        failOnErrors = new ArrayList<Boolean>();
	}

	/**
	* Initializes this object via its persistent properties.
	*
	* @param  key   Property-key to use for locating initialization properties.
	* @param  type  Property-type to use for locating initialization properties.
	*
	* @exception ProcessingException when initialization fails
	*/
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        // Get configuration properties specific to this processor.
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, CLASS_NAME+" : Initializing..." );

        errorBuffer = new StringBuffer( );

        customerId = getRequiredPropertyValue(CUSTOMER_ID, errorBuffer );

		minValue = getRequiredPropertyValue(MIN_VALUE, errorBuffer );

		maxValue = getRequiredPropertyValue(MAX_VALUE, errorBuffer );

		populateSequenceNames();

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, CLASS_NAME+" : Initialization done." );
    }


  /**
   * This method populates the list fo columndata
   *
   * @exception ProcessingException when initialization fails
   **/

    private void populateSequenceNames() throws ProcessingException 
	{
		// If any of the required properties are absent, indicate error to caller.
		if (errorBuffer.length() > 0) 
		{
			String errMsg = errorBuffer.toString();
			Debug.log(Debug.ALL_ERRORS, errMsg);
			throw new ProcessingException(errMsg);
		}
		for (int Ix = 0; true; Ix++) 
		{
			String sequence = getPropertyValue(PersistentProperty.
									getPropNameIteration(SEQUENCE_NAME, Ix));
			
			// If we can't find sequence then , we're done.
			if (!StringUtils.hasValue(sequence))
			{
				break;
			}
			String failOnErrorPropStr =getPropertyValue(PersistentProperty.
					getPropNameIteration(FAIL_ON_ERROR, Ix));
			
			Boolean failOnErrorProp;
			if (!StringUtils.hasValue(failOnErrorPropStr))
			{
				Debug.log(Debug.NORMAL_STATUS, CLASS_NAME+" : FAIL_ON_ERROR_"+Ix+" not found. Setting FAIL_ON_ERROR_"+Ix + " [true]");
				failOnErrorProp = Boolean.TRUE;
			}
			else
				failOnErrorProp=Boolean.parseBoolean(failOnErrorPropStr);
			
			if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
			{
				Debug.log(Debug.SYSTEM_CONFIG, CLASS_NAME+" : SEQUENCE_NAME_"+Ix+" is "+sequence);
				Debug.log(Debug.SYSTEM_CONFIG, CLASS_NAME+" : FAIL_ON_ERROR_"+Ix+" is "+failOnErrorProp);
			}

			try 
			{
				// Add the sequence name to the list.
                if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))                
				    Debug.log(Debug.SYSTEM_CONFIG,CLASS_NAME+" : "+sequence+" added to the list");
				
                sequenceNames.add(sequence);
                failOnErrors.add(failOnErrorProp);
			}
			catch (Exception exp) 
			{
				throw new ProcessingException(
				"ERROR: Could not create sequence data description:\n"
				+ exp.toString());
			}
		}
	}

	/**
	* Extract data values from the context/input, and use them to
	* generate a new sequence into the configured database table.
	*
	* @param  mpContext The context
	* @param  inputObject  Input message to process.
	*
	* @return  The given input, or null.
	*
	* @exception  ProcessingException  Thrown if processing fails.
	* @exception  MessageException  Thrown if message is bad.
	*/
	public NVPair[] process(MessageProcessorContext mpContext,MessageObject inputObject) 
						throws MessageException,ProcessingException 
	{
		if (inputObject == null) 
		{
			return null;
		}

		customerId = getString(customerId,mpContext,inputObject);

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))        
            Debug.log( Debug.SYSTEM_CONFIG, CLASS_NAME+" :Processing Customer Domain for which the sequence is to be created is [" + customerId + "]." );

        Connection dbConn = null;

		try 
		{
			// Get a database connection.
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
			 Debug.log(Debug.MSG_STATUS,CLASS_NAME+" : Getting Database connection from pool.");

			dbConn = DBConnectionPool.getInstance().acquireConnection();

			// Create the Sequences in the database.
			createSequence(dbConn);
		}
		catch (Exception exp) 
		{
			String errMsg =
			"ERROR: "+CLASS_NAME+" : Attempt to create sequences failed with error: "
			+ exp.getMessage();

			Debug.log(Debug.ALL_ERRORS, errMsg);
			// Re-throw the exception to the driver.
			throw new ProcessingException(errMsg);
		}
		finally 
		{
			// If the configuration indicates that this SQL operation isn't part of the overall driver
			// transaction, return the connection previously acquired back to the resource pool.
			if (dbConn != null) 
			{
				try 
				{
					DBConnectionPool.getInstance().releaseConnection(dbConn);
				}
				catch (Exception exp) 
				{
					Debug.log(Debug.ALL_ERRORS, exp.toString());
				}
			}
		}

		// Always return input value to provide pass-through semantics.
		return (formatNVPair(inputObject));
	}

  /**
   * Create Sequences in the database table using the given connection.
   *
   * @param  dbConn  The database connection to perform the SQL generation operation against.
   *
   * @exception  ProcessingException  Thrown if processing fails.
   */
  private void createSequence(Connection dbConn) throws ProcessingException 
  {
    validate();
    Statement stmt = null;

    try 
	{
		Iterator<String> seqIter = sequenceNames.iterator();
		Iterator<Boolean> failOnErrIter = failOnErrors.iterator();

		for (int Ix =1;seqIter.hasNext();Ix++) 
		{
			String sequenceName =  seqIter.next();
			Boolean failOnError = failOnErrIter.next();
			
            if(Debug.isLevelEnabled(Debug.DB_DATA))
			 Debug.log(Debug.DB_DATA,CLASS_NAME+" : Populating Batch Statement for sequence  [" + Ix +"] with sequence name." + sequenceName);

            // Creating the actual sequence
			if (StringUtils.hasValue(sequenceName)) 
			{
				String tempSequenceName= customerId + "_"+sequenceName ;
				StringBuilder sBufsqlStmt = new StringBuilder();
				sBufsqlStmt.append("CREATE SEQUENCE ");
				sBufsqlStmt.append(tempSequenceName);
				sBufsqlStmt.append(" MAXVALUE  ");
				sBufsqlStmt.append(maxValue);
				sBufsqlStmt.append(" MINVALUE ");
				sBufsqlStmt.append(minValue);
				String sqlStmt = sBufsqlStmt.toString();
				
				if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(Debug.NORMAL_STATUS, CLASS_NAME+" : Executing SQL \n: " + sqlStmt);

				/* Get a statement for the SQL statement */
				stmt = dbConn.createStatement();
				try
				{
					stmt.executeUpdate(sqlStmt);

					if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
						Debug.log(Debug.NORMAL_STATUS,CLASS_NAME+" : Finished Executing SQL..");

				}
				
				
				catch(SQLException sqe)
				{
					if(!failOnError && sqe.getErrorCode()==ORA_ERR_CODE)
					{
						/* name is already used by an existing object */
						Debug.log(Debug.DB_DATA, CLASS_NAME+" : "+sqe.getMessage());
						Debug.log(Debug.DB_DATA, CLASS_NAME+" : Ignoring SQL exception for SEQUENCE_NAME ["+tempSequenceName+"] as FAIL_ON_ERROR value is [false]");
					}
					else
					{
						Debug.log(Debug.DB_ERROR, CLASS_NAME+" : SQL exception occurred  for SEQUENCE_NAME ["+tempSequenceName+"] and FAIL_ON_ERROR value is [true]");
						throw sqe;
					}
				}
				
				if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
					Debug.log(Debug.NORMAL_STATUS,CLASS_NAME+" : Sequence Creation Completed for : "+tempSequenceName);
			}
		}
		
		if(Debug.isLevelEnabled(Debug.DB_DATA))
			Debug.log(Debug.DB_DATA, "DONE CREATING SEQUENCE NUMBERS.");    
    }
    catch (SQLException sqle) 
	{
      throw new ProcessingException(
          "ERROR: Could not create sequences in the database for domain [" + customerId
          + "]:\n" + DBInterface.getSQLErrorMessage(sqle));
    }
    catch (Exception exp) 
	{
      throw new ProcessingException(
          "ERROR: Could not create sequences in the database for domain [" + customerId
          + "]:\n" + exp.toString());
    }
	finally 
	{
		if (stmt != null) 
		{
			try 
			{
				stmt.close();
			}
			catch (SQLException sqle) 
			{
				Debug.log(Debug.ALL_ERRORS, DBInterface.getSQLErrorMessage(sqle));
			}
		}
	}
  }

  /**
   * Check that sequenceNames were configured and at least one
   * field has a value to Create Sequence.
   *
   * @exception ProcessingException  thrown if invalid
   */
	private void validate() throws ProcessingException 
	{
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
		  Debug.log(Debug.MSG_STATUS, CLASS_NAME+" : Validating sequence list ...");

		if (sequenceNames.size() < 1) 
		{
			throw new ProcessingException(
			"ERROR: No Sequence Names are available to create for ["
			+ customerId + "].");
		}

        if(Debug.isLevelEnabled(Debug.DB_DATA))
		  Debug.log(Debug.DB_DATA, CLASS_NAME+" : Done validating sequence list.");
	}
	
	public static void main(String[] args) 
	{
		Debug.enableAll();
		
		try 
		{
			DBInterface.initialize("jdbc:oracle:thin:@192.168.98.12:1521:test", "chgtw_rajni", "chgtw_rajni");
		}
		catch (DatabaseException dbExp) 
		{
			Debug.log(Debug.MAPPING_ERROR, "DBSequenceGenerator: " +
			"Database initialization failure: " + dbExp.getMessage());
		}
		DBSequenceGenerator bl = new DBSequenceGenerator();
		try 
		{
			bl.initialize("ConfigureBrandedGUI_running", "LSRSequenceGenerator");

			HashMap m = new HashMap();
			MessageObject msg = new MessageObject(m);
			MessageProcessorContext mpc = new MessageProcessorContext();
			mpc.set("lockExpiryPeriod", "30");
			bl.process(mpc, msg);
		}
		catch (Exception pExp) 
		{
			System.out.println("FAILED IN MAIN OF DBSequenceGenerator:" + pExp.getClass());
		}
		
		
	} 
	
}

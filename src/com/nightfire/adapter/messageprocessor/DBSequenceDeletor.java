/**
 * The purpose of this processor is to delete sequences(s) into the
 * database as per provided names and specified domain.
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
 * @author rkewlani
 *
 */

public class DBSequenceDeletor extends DBMessageProcessorBase 
{
	/**
	* Property indicating name of the sequence name to be deleted.
	*/
	public static final String SEQUENCE_NAME = "SEQUENCE_NAME";
	
	/**
	* Property indicating the customer domain to be prefixed with the sequence name.
	*/
	public static final String CUSTOMER_ID = "CUSTOMER_ID";
	/**
	* Property indicating whether processor should fail if sequence does not exist.
	*/
	public static final String FAIL_ON_ERROR ="FAIL_ON_ERROR";
	/**
	* Class name used in logs
	*/
	public static final String CLASS_NAME =  DBSequenceDeletor.class.getSimpleName();

	
	private String customerId = null;
	private List<String> sequenceNames;
	StringBuffer errorBuffer = null;
	private List<Boolean> failOnErrors =null;

	/**
	* Constructor.
	*/
	public DBSequenceDeletor() 
	{
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
		  Debug.log(Debug.SYSTEM_CONFIG, CLASS_NAME+" : Creating DBSequenceDeletor message-processor.");
		
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
        super.initialize( key, type );

        // Get configuration properties specific to this processor.
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, CLASS_NAME+" : Initializing..." );

        errorBuffer = new StringBuffer( );

        customerId = getRequiredPropertyValue(CUSTOMER_ID, errorBuffer );
        
       
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
				break;
			
			String failOnErrorPropStr =getPropertyValue(PersistentProperty.
					getPropNameIteration(FAIL_ON_ERROR, Ix));
			
			boolean failOnErrorProp;
			if (!StringUtils.hasValue(failOnErrorPropStr))
			{
				if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
					Debug.log(Debug.SYSTEM_CONFIG, CLASS_NAME+" : FAIL_ON_ERROR_"+Ix+" not found. Setting FAIL_ON_ERROR_"+Ix + " [true]");
				
				failOnErrorProp = Boolean.TRUE;
			}
			else
				failOnErrorProp = Boolean.parseBoolean(failOnErrorPropStr);
			
			if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
				Debug.log(Debug.SYSTEM_CONFIG, CLASS_NAME+" : SEQUENCE_NAME_"+Ix+" is "+sequence 
						+"\n"+" : FAIL_ON_ERROR_"+Ix+" is "+failOnErrorProp);
			

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
				"ERROR: Could not delete sequence data description:\n"
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
			return null;

		customerId = getString(customerId,mpContext,inputObject);

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))        
            Debug.log( Debug.SYSTEM_CONFIG, CLASS_NAME+" : Processing Customer Domain for which the sequence is to be deleted is [" + customerId + "]." );

        Connection dbConn = null;

		try 
		{
			// Get a database connection.
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            	Debug.log(Debug.MSG_STATUS,CLASS_NAME+" : Getting Database connection from pool.");

			dbConn = DBConnectionPool.getInstance().acquireConnection();
					// delete the Sequences in the database.
			deleteSequence(dbConn);
		}
		catch (Exception exp) 
		{
			String errMsg =
			"ERROR: DBSequenceGenerator: Attempt to delete sequences failed with error: "
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
   * delete Sequences in the database table using the given connection.
   *
   * @param  dbConn  The database connection to perform the SQL generation operation against.
   *
   * @exception  ProcessingException  Thrown if processing fails.
   */
  private void deleteSequence(Connection dbConn) throws ProcessingException 
  {
    validate();
    Statement stmt = null;

    try 
	{
		Iterator<String> seqIter = sequenceNames.iterator();
		Iterator<Boolean> failOnErrIter = failOnErrors.iterator();
		
		
		for (int Ix =1;seqIter.hasNext();Ix++) 
		{
			String sequenceName = (String) seqIter.next();
			
			boolean failOnError = (Boolean)failOnErrIter.next();
			           
            if(Debug.isLevelEnabled(Debug.DB_DATA))
            	Debug.log(Debug.DB_DATA,CLASS_NAME+" : Populating Batch Statement for sequence  [" + Ix 
					 +"] with sequence name [" + sequenceName +"] and with FAIL_ON_ERROR ["+failOnError+"]");

            // Creating the actual sequence
				if (StringUtils.hasValue(sequenceName)) {

					String tempSequenceName = customerId + "_" + sequenceName;
					StringBuilder sBufsqlStmt = new StringBuilder();
					sBufsqlStmt.append("DROP SEQUENCE ")
					.append(tempSequenceName);
					
					String sqlStmt = sBufsqlStmt.toString();
					
					if(Debug.isLevelEnabled(Debug.DB_DATA))
						Debug.log(Debug.DB_DATA, CLASS_NAME + " : Statement : "	+ sqlStmt);

					// Get a prepared statement for the SQL statement.
					stmt = dbConn.createStatement();

					try {
						stmt.executeUpdate(sqlStmt);
					} catch (SQLException sqe) {
						if (!failOnError && sqe.getErrorCode() == 2289) {
							Debug.log(Debug.DB_DATA, CLASS_NAME + " : "
									+ sqe.getMessage());
							Debug.log(Debug.DB_DATA,
											CLASS_NAME
													+ " : Ignoring SQL exception for SEQUENCE_NAME ["
													+ tempSequenceName
													+ "] as FAIL_ON_ERROR value is [false]");
						} else {
							Debug.log(Debug.ALL_ERRORS,
											CLASS_NAME
													+ " : SQL exception occurred  for SEQUENCE_NAME ["
													+ tempSequenceName
													+ "] and FAIL_ON_ERROR value is [true]");
							throw sqe;
						}

					}

					if(Debug.isLevelEnabled(Debug.DB_DATA))
						Debug.log(Debug.DB_DATA, CLASS_NAME
							+ " : Sequence deletion Completed for : "
							+ tempSequenceName);
				}				
				
			}
		
		if(Debug.isLevelEnabled(Debug.DB_DATA))
			Debug.log(Debug.DB_DATA, "DONE DELETING SEQUENCE NUMBERS.");    
    }
    catch (SQLException sqle) 
	{
      throw new ProcessingException(
          "ERROR: Could not delete sequences in the database for domain [" + customerId
          + "]:\n" + DBInterface.getSQLErrorMessage(sqle));
    }
    catch (Exception exp) 
	{
      throw new ProcessingException(
          "ERROR: Could not delete sequences in the database for domain [" + customerId
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
   * field has a value to delete Sequence.
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
			"ERROR: No Sequence Names are available to delete for ["
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
		DBSequenceDeletor bl = new DBSequenceDeletor();
		try 
		{
			bl.initialize("ConfigureBrandedGUI_running", "LSRDBSequenceDeletor");

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
		System.out.println("done execution of main");
		
	} 
	
}

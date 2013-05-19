/**
 * This class is used to provide the resources such as statements,closes opened resources, 
 * keep tracking the failure records and also provide the functions to commit or rollback the transactions.
 *    
 * @author D.Subbarao
 * @version 3.3
 * 
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.spi.neustar_soa.utils.SOAConstants
 * @see com.nightfire.framework.util.FrameworkException
 */

/** 
 History
 ---------------------
 Rev#		Modified By 	Date				Reason
 -----       -----------     ----------			--------------------------
 1			Subbarao		08/03/2005			Created
 2		    Subbarao		09/05/2005			Modified.
 3			Subbarao		10/20/2005			Modified.
 4			Subbarao		10/28/2005			Modified.
 
 */


package com.nightfire.spi.neustar_soa.file;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.adapter.NPACConstants;
import com.nightfire.spi.neustar_soa.utils.TimeZoneUtil;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public abstract class NotificationTokenConsumerBase {
    
    // This holds a partular notification with respect to NPAC.
    protected String notifyInsert = null;
       
    /**
     * A DB connection to use while processing tokens.
     */
    protected Connection connection;
    
    /**
     * This is the variable contains failed record(s).
     */
    public static List failedList = new LinkedList();
    
    /**
     * This is just used to hold the DB Connection's original autocommit value.
     */
    private boolean connectionAutoCommitTemp;
    
    /**
     * This is the precompiled SQL statement that this class will use to insert
     * data into the DB.
     */
    private PreparedStatement insertStatement;
    
    // this flag will show whether inserted succcessfuly
    protected boolean errorFlag = false;
    
    /**
     * This gets a database connection that will be used by process method.
     * 
     * @throws FrameworkException
     *             if a database connection could not be acquired.
     */
    protected void init() throws FrameworkException {
        
        try {
            /*
             * get a DB Connection from a pool of connections save the original
             * auto-commit value Set to not autocommit. We will either commit
             * everything at the end or rollback.
             */
            connection = DBInterface.acquireConnection();
            
            // save the original auto-commit value
            connectionAutoCommitTemp = connection.getAutoCommit();
            
        } catch (SQLException sqlex) {
            throw new FrameworkException(
                    "Could not prepare insert statement:\n" + getInsertSQL()
                    + "\n" + sqlex);
        }
        
    }
    
    /**
     * Called to process a line of data from a delimited file. This first tries
     * to insert the data into the database.
     * 
     * @param tokens
     *            String[] the tokenized line of input.
     * @throws FrameworkException
     *             if the attempt to insert and the attempt to update both fail.
     */
    public boolean process(String[] tokens) throws FrameworkException {
        
        int errorCode=0;
        
        String msg=null;
        
        try {
            if(tokens!=null)
           // This tokens are containing the notification id and Object Id.     
            if(tokens[3] != null && tokens[4] != null){
                
                // This will return the notification type based on 
                //	either notification id  or both (noticationid,objectid).

                notifyInsert=SOAUtility.returnNpacNotification
                
                (Integer.parseInt(tokens[3]),Integer.parseInt(tokens[4]));
                
            }
            insertStatement = prepareStatement(getInsertSQL());
            
            setInsertParams(insertStatement, tokens);
            // insert the data into associated tables
        } catch (SQLException sqlex) {
            
            if (Debug.isLevelEnabled(Debug.DB_STATUS)) {
                errorCode=Debug.DB_STATUS;
                msg= sqlex.getMessage().trim();
                errorFlag = true;
            }
        } catch (FrameworkException fe) {
	            errorCode=Debug.ALL_WARNINGS;
	            msg=fe.getMessage();
	            errorFlag = true;
        }
        catch(NumberFormatException fn){
	            errorCode=Debug.ALL_WARNINGS;
	            msg=fn.getMessage();
	            errorFlag = true;
        }
        if(errorFlag==true && msg!=null)
        {
            Debug.log(errorCode,"Insert Failed:" + msg + "\nAdding in failed List ...");
            tokens[tokens.length - 2] =msg;
            failedList.add(tokens);
        }    
        // this flag will show whether inserted succcessfuly
        return errorFlag;
        
    }
    
    /**
     * This is called after the file has been processed. This either commits or
     * rollsback the DB changes as neccessary and releases the DB Connection.
     * 
     * @param success
     *            boolean this flag indicates whether processing completed
     *            successfully or not. This is used to determine whether changes
     *            should be committed or whether they should be rolled back.
     */
    public void cleanup(boolean success) {
        
        try {
            if(insertStatement!=null)
                insertStatement.close();
            
        } catch (SQLException sqlex) {
            Debug.error("Could not close insert statement:\n" + sqlex);
        }
        try {
            if (connection != null) {
                try {
                    
                    if (success) {
                        // commit changes
                        connection.commit();
                    } else {
                        // rollback changes
                        connection.rollback();
                    }
                } catch (SQLException sqlex) {
                    String error = "Could not ";
                    error += (success) ? "commit changes: "
                            : "rollback changes: ";
                    Debug.error(error + sqlex);
                }
                try {
                    // restore the original auto-commit setting before returning
                    // the connection to the pool
                    connection.setAutoCommit(connectionAutoCommitTemp);
                } catch (SQLException sqlex) {
                    Debug
                    .error("Could not reset the autocommit flag on " +
                            "the connection: "
                            + sqlex);
                }
                // return connection to the pool
                DBInterface.releaseConnection(connection);
            }
        } catch (FrameworkException fex) {
            Debug.error("Could not release database connection: " + fex);
        }
        
    }
    
    /**
     * This method must be implemented by child classed to provide the
     * implementation-specific SQL used to insert input into the database.
     * 
     * @return String an SQL insert statement.
     */
    protected abstract String getInsertSQL();
    
    /**
     * This calls on the DB connection to prepare an SQL statement. init() must
     * be called before calling this method, otherwise, the connection will be
     * null and throw a NullPointerException.
     * 
     * @param sql
     *            String the SQL statement to prepare.
     * @throws SQLException
     *             if the
     * @return PreparedStatement
     */
    protected PreparedStatement prepareStatement(String sql)
    throws SQLException {
        
        return connection.prepareStatement(sql);
        
    }
    
    /**
     * This utility method returns a Date object for the give String. This
     * assumes that the given date string is in the usual BDD date format (see
     * FILE_DATE_FORMAT), and that the date's time zone is GMT (universal time).
     * 
     * @param String
     *            date a String date as read from a file.
     * @return Date a java date representing the given date string in local
     *         time.
     */
    protected static Date parseDate(String date) throws MessageException {
        
        if (!date.equals("")) {
            
            return TimeZoneUtil
            .parse(NPACConstants.UTC,SOAConstants.FILE_DATE_FORMAT, date);
            
        } else {
            
            return null;
            
        }
    }
    
    /**
     * This must be implemented by child classes to populate the given statement
     * with the given data.
     * 
     * @param insertStatement
     *            PreparedStatement the statement whose parameters need to be
     *            populated.
     * @param params
     *            String[] the line of tokens whose values will be used to
     *            populate the statement.
     * @throws SQLException
     *             thrown if the statement cannot be populated.
     * @throws FrameworkException
     *             thrown if some other error occurs.
     *  
     */
    protected abstract void setInsertParams(PreparedStatement insertStatement,
            String[] params) throws SQLException, FrameworkException;
    
    /**
     * This method will return failed records
     */
    public List getFailedList() {
        
        return failedList;
        
    }
    
    /**
     * It will be used to commit the trasactions.
     * @throws SQLException
     */
    protected void setDataToCommit() throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }
    
    /**
     * It will be used to rollback the transactions.
     * @throws SQLException
     */
    protected void setDataToRollback() throws SQLException {
        connection.rollback();
        connection.setAutoCommit(true);
    }
    
}
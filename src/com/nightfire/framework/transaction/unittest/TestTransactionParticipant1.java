/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.transaction.unittest;

import java.sql.*;
import java.util.Vector;

import com.nightfire.framework.transaction.*;
import com.nightfire.framework.util.*;


/**
 * 
 */
public class TestTransactionParticipant1
implements TransactionParticipant
{
    // ========================================================
    // =========  TransactionParticipant methods  =============
    // ========================================================

    /**
     * Implement the method for TransactionParticipant interface.
     */
    public void save(SaveContext context) 
	throws SaveFailedException 
    {
	try {
	    DBSaveContext dsc = (DBSaveContext) context;
	    Connection conn = dsc.getDBConnection();	    
	    doSave(conn);
	} catch (SQLException e) {
	    throw new SaveFailedException(e.toString());
	}
    }

    public void undo(Transaction tx)
	throws UndoFailedException
    {
    }

    private void doSave(Connection conn) 
    throws SQLException
    {
	String query = " INSERT INTO   " + TABLE_NAME +
	    " values (1, 10) ";
	Debug.log(Debug.UNIT_TEST, "TestTransactionParticipant1: Executing: ["
		  + query + "]");
	PreparedStatement pstmt = conn.prepareStatement(query);
	pstmt.executeUpdate();
	pstmt.close();
    }

    static final String TABLE_NAME = "TEST_TRANSACTION1";

}


/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.transaction;

import java.sql.*;
import java.util.*;

import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.*;
import com.nightfire.framework.util.Debug;


/**
 * A database transaction. This class implements the
 * acquireSaveContext() and releaseSaveContext() methods to acquire
 * and release db connections.
 */
public class DBTransaction extends Transaction
{
    /**
     * Constructor.  It generates an ID for the transaction.  The 
     * transaction ID is a unique number within the VM.
     */
    public DBTransaction()
    {
	super();
    }

    /**
     * Get a SaveConext (like a database connection) before 
     * committing information.
     */
    protected SaveContext acquireSaveContext() 
    throws ResourceException
    {
	DBConnectionPool pool = DBConnectionPool.getInstance();
	Connection conn = pool.acquireConnection();
	return new DBSaveContext(conn);
    }

    /**
     * Release a previously acquired SaveContext.
     */
    protected void releaseSaveContext(SaveContext sc) 
    throws ResourceException
    {
	Connection conn = ((DBSaveContext)sc).getDBConnection();
	DBConnectionPool pool = DBConnectionPool.getInstance();
	pool.releaseConnection(conn);
	((DBSaveContext)sc).invalidate();
    }

}

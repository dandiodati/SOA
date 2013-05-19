/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.transaction;

import java.util.Vector;
import java.sql.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.ResourceException;
import com.nightfire.framework.util.Debug;

/**
 * A database-aware context for use with transactions.
 */
public class DBSaveContext implements SaveContext
{
    /**
     * Context constructor.
     *
     * @param  connection  The database connection to use.
     */
    public DBSaveContext(Connection connection) {
	mConnection = connection;
    }

    /**
     * Commits the transaction
     *
     * @exception CommitFailedException  Thrown if commit fails.
     */
    public void commit() 
	throws CommitFailedException
    {
	try {
	    Debug.log(Debug.NORMAL_STATUS, " Committing transaction this: [" + this + "]" );
      DBConnectionPool.getInstance().commit( mConnection );
	} catch (ResourceException e) {
	    throw new CommitFailedException(e.toString());
	}
    }

    /**
     * Rolls-back the transaction
     *
     * @exception RollbackFailedException  Thrown if rollback fails.
     */
    public void rollback() 
	throws RollbackFailedException
    {
	try {
      DBConnectionPool.getInstance().rollback( mConnection );
	} catch (ResourceException e) {
	    throw new RollbackFailedException(e.toString());
	}

    }

    /**
     * Gets the database connection associated with this context.
     *
     * @return  The database connection.
     */
    public Connection getDBConnection() {
	return mConnection;
    }

    /**
     * Causes the context to forget its conection.
     */
    public void invalidate() {
	mConnection = null;
    }

    private Connection mConnection = null;
}

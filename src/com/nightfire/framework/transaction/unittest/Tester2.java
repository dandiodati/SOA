/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.transaction.unittest;

import java.sql.*;
import java.util.Vector;

import com.nightfire.framework.db.*;
import com.nightfire.unittest.*;
import com.nightfire.framework.transaction.*;
import com.nightfire.framework.util.*;


/**
 * 
 */
public class Tester2 extends UnitTestBase
{
    public static void main(String[] args) {
	String myTests[] = {
	    "Test case 1. (See framework/transaction/unittest/test-case-list.txt) ",
	    "Test case 2. (See framework/transaction/unittest/test-case-list.txt) ",
	    "Test case 3. Run 1 & 2tests 100 times."
	};	
	testNames = myTests;	// static from UnitTestBase
	doTests(args);
    }


    public static void doTests(String[] args) {
        try {
	    // Configure which debug log levels are displayed.
	    Debug.enableAll( );
	    Debug.disableAll( );
	    //Debug.enable( Debug.UNIT_TEST );
	    Debug.enable( Debug.ASSERT );
	    SetEnvironment.setSystemProperties("db.properties",true);
	    DBInterface.initialize();

	    initializeTests();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	try {
	    int num_tests = 20000;
	    Debug.enable( Debug.UNIT_TEST );
	    log("Will run tests " + num_tests + 
			       " times. Won't print"
			       + " any output unless something is wrong.");
	    Debug.disable( Debug.UNIT_TEST );
	    for (int i=0; i < num_tests; i++) {
		dotest(false, i);
		dotest(true,  i);
	    }
	    Debug.enable( Debug.UNIT_TEST );
	    log("Tests successful.");
	}
        catch ( Exception e )  {
	    log( "Unit test \"" + testNames[testCounter] + 
		 "\" failed with the following exception:\n" + e.toString() );
            
	    log( getStackTrace( e ) );
	} finally {
	    cleanup();
	}
    	
    }

    private static void initializeTests() 
    throws Exception
    {
	String query = "create table " + TestTransactionParticipant1.TABLE_NAME
	    + " (x number(2,0), y number(3,0))";
	DBConnectionPool pool = DBConnectionPool.getInstance();
	Connection conn = pool.acquireConnection();
	PreparedStatement ps = DBInterface.getPreparedStatement(conn,query);
	ps.executeUpdate();
	ps.close();

	query = "create table " + TestTransactionParticipant2.TABLE_NAME
	    + " (x number(2,0), y number(3,0))";
	ps = DBInterface.getPreparedStatement(conn,query);
	ps.executeUpdate();
	ps.close();
	pool.commit( conn );
	pool.releaseConnection(conn);
    }

    private static void cleanup() 
    {
	try {
	    String query = "drop table " +  TestTransactionParticipant1.TABLE_NAME;
	    DBConnectionPool pool = DBConnectionPool.getInstance();
	    Connection conn = pool.acquireConnection();
	    PreparedStatement ps = DBInterface.getPreparedStatement(conn,query);
	    ps.executeUpdate();
	    query = "drop table " +  TestTransactionParticipant2.TABLE_NAME;
	    ps = DBInterface.getPreparedStatement(conn,query);
	    ps.executeUpdate();
	    ps.close();
    	pool.commit( conn );
	    pool.releaseConnection(conn);
	} catch (Exception e) {
	    log("Exception trying to cleanup after test.");
	    log( getStackTrace( e ) );	    
	}
    }


    
    /*************************************************************************/

    private static void dotest (boolean rollback , int counter)
    throws Exception
    {
	DBTransaction tx = new DBTransaction();
	Debug.log(Debug.UNIT_TEST, "Transaction: Tx ID: ["
		  +  tx.getID() + "]");
	TransactionParticipant tp1 = new TestTransactionParticipant1();
	TransactionParticipant tp2 = new TestTransactionParticipant2();
	tx.add(tp1);
	tx.add(tp2);
	if (rollback) {
	    tx.rollback();
	} else {
	    tx.commit();
	}

	DBConnectionPool pool = DBConnectionPool.getInstance();
	Connection conn = null;
	Debug.log(Debug.UNIT_TEST, "Verifying by querying DB.");
	try {
	    String query1 = "select count(*) from " + 
		TestTransactionParticipant1.TABLE_NAME + 
		" where x = 1";
	    conn = pool.acquireConnection();
	    PreparedStatement ps = DBInterface.getPreparedStatement(conn,query1);
	    ResultSet rs= ps.executeQuery();
	    rs.next();
	    int count1 = rs.getInt(1);
	    rs.close();
	    ps.close();
	    if (count1 != counter) {
		throw new Exception("Verification1 failed. count1 = "  + count1);
	    } 
	    String query2 = "select count(*) from " + 
		TestTransactionParticipant2.TABLE_NAME + 
		" where x = 1";
	    ps = DBInterface.getPreparedStatement(conn,query2);
	    rs= ps.executeQuery();
	    rs.next();
	    int count2 = rs.getInt(1);
	    rs.close();
	    ps.close();
	    if (count2 != counter) {
		throw new Exception("Verification2 failed.");
	    } 
	    
	} catch (Exception e) {
	    log("Exception trying to cleanup after test.");
	    log( getStackTrace( e ) );	    
	} finally {
	    if (conn != null) {
		pool.releaseConnection(conn);
	    }
	}
	
    }

}


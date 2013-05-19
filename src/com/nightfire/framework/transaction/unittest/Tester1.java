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
public class Tester1 extends UnitTestBase
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
	    Debug.enable( Debug.UNIT_TEST );
	    Debug.enable( Debug.ASSERT );
	    SetEnvironment.setSystemProperties("db.properties",true);
	    DBInterface.initialize();

	    initializeTests();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	try {

	    // PUT YOUR UNIT TESTS HERE!!!
	    logTestStart( );
	    unit_test_one( );
	    logTestEnd( );
            
	    logTestStart( );
	    unit_test_two( );
	    logTestEnd( );
	}
        catch ( Exception e )  {
	    log( "Unit test \"" + testNames[testCounter] + 
		 "\" failed with the following exception:\n" + e.toString() );
            
	    log( getStackTrace( e ) );
	} finally {
	    cleanup();
	}
        
	log( "\t\t\tTEST SUMMARY");
    	log( "\tTotal test cases: " + testNames.length );
    	log( "\tTotal passed:     " + testCounter );
    	
    	if ( testCounter == testNames.length ) 
	    {
    		log( "All tests finished successfully." );
	    }
        else
	    {
    		log( "At least one test failed." );
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
	query = "create table " + TestTransactionParticipant2.TABLE_NAME
	    + " (x number(2,0), y number(3,0))";
	ps = DBInterface.getPreparedStatement(conn,query);
	ps.executeUpdate();
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

    	pool.commit( conn );
	    pool.releaseConnection(conn);
	} catch (Exception e) {
	    log("Exception trying to cleanup after test.");
	    log( getStackTrace( e ) );	    
	}
    }


    
    /*************************************************************************/

    private static void dotest (boolean rollback )
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

	Debug.log(Debug.UNIT_TEST, "Verifying by querying DB.");
	try {
	    String query1 = "select count(*) from " + 
		TestTransactionParticipant1.TABLE_NAME + 
		" where x = 1";
	    DBConnectionPool pool = DBConnectionPool.getInstance();
	    Connection conn = pool.acquireConnection();
	    PreparedStatement ps = DBInterface.getPreparedStatement(conn,query1);
	    log("Checking # rows in table 1. There should be 1 row.");
	    log("Executing query:["+query1+"].");
	    ResultSet rs= ps.executeQuery();
	    rs.next();
	    int count1 = rs.getInt(1);
	    rs.close();
	    ps.close();
	    if (count1 != 1) {
		throw new Exception("Verification1 failed. count1 = "  + count1);
	    } else {
		log("Verification successful.");
	    }
	    String query2 = "select count(*) from " + 
		TestTransactionParticipant2.TABLE_NAME + 
		" where x = 1";
	    ps = DBInterface.getPreparedStatement(conn,query2);
	    log("Checking # rows in table 2. There should be 1 row.");
	    log("Executing query:["+query2+"].");
	    rs= ps.executeQuery();
	    rs.next();
	    int count2 = rs.getInt(1);
	    rs.close();
	    if (count2 != 1) {
		throw new Exception("Verification2 failed.");
	    } else {
		log("Verification successful.");
	    }
	    
	} catch (Exception e) {
	    log("Exception trying to cleanup after test.");
	    log( getStackTrace( e ) );	    
	}
	
    }


    private static void unit_test_one ( )
    throws Exception
    {
	log("**** Will insert two rows into two different tables (participant 1&2),"
	    + "and commit " );
	log("**** There should be one row in each table after this test." );
	dotest(false); // commit()
    }
    /*************************************************************************/

    private static void unit_test_two ( )
    throws Exception
    {
	log("**** Will insert two rows into two different tables (participant 1&2),"
	    + "and rollback " );
	log("**** There should still be one row in each table after this test"+
	    " because we'll be rolling back the changes.");
	dotest(true); // rollback
    }


}


package com.nightfire.framework.test;

import java.io.*;
import java.util.*;
import java.sql.*;

import org.omg.CORBA.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.resource.*;


/*
 * Class for testing message parsing, generating and mapping.
 */
class TestDatabasePools
{
    private static final int PERF_TIMER = 98;

    public static void main ( String[] args )
    {
        Debug.showLevels();
        Debug.enableAll();

        try
        {
            SetEnvironment.setSystemProperties( "./test.properties", true );


            // Test repeated initialization.
            DBInterface.initialize( );

            // Set up the pools supporting the 2 accounts that we'll test against.
            DBConnectionPool.addPoolConfiguration( "account1", "jdbc:oracle:thin:@192.168.10.11:1521:orcl", "bentley", "bentley" );

            DBConnectionPool.addPoolConfiguration( "account2", "jdbc:oracle:thin:@192.168.10.139:1521:orcl", "bentley", "bentley" );

            DBConnectionPool.initializePoolConfigurations( );

            DBInterface.closeConnection( );


            DBInterface.initialize( );

            DBConnectionPool.addPoolConfiguration( "account1", "jdbc:oracle:thin:@192.168.10.11:1521:orcl", "bentley", "bentley" );

            DBConnectionPool.addPoolConfiguration( "account2", "jdbc:oracle:thin:@192.168.10.139:1521:orcl", "bentley", "bentley" );

            DBConnectionPool.intializePoolConfigurations( );


            testProperties( );

            testSequences( );

            testPersistentIOR( args );

            testDBLog( );

            DBInterface.closeConnection( );
        }
        catch ( FrameworkException fe )
        {
            System.err.println( fe.toString() );

            fe.printStackTrace( );
        }
    }


    /*************************************************************************/

    public TestDatabasePools ( )   { /*EMPTY*/ }



    /*************************************************************************/

    private static void testProperties ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Properties cache test ..." );

        Hashtable map = null;

        DBConnectionPool.setThreadSpecificPoolKey( null );

        map = PersistentProperty.getProperties( "UNIT", "TEST" );
        
        Debug.log( Debug.UNIT_TEST, "Default properties:\n" + map.toString() );

        DBConnectionPool.setThreadSpecificPoolKey( null );


        DBConnectionPool.setThreadSpecificPoolKey( "account1" );

        map = PersistentProperty.getProperties( "UNIT", "TEST" );
        
        Debug.log( Debug.UNIT_TEST, "Account1 properties:\n" + map.toString() );


        DBConnectionPool.setThreadSpecificPoolKey( "account2" );

        map = PersistentProperty.getProperties( "UNIT", "TEST" );
        
        Debug.log( Debug.UNIT_TEST, "Account2 properties:\n" + map.toString() );


        DBConnectionPool.setThreadSpecificPoolKey( null );


        map = PersistentProperty.getProperties( "UNIT", "TEST" );
        
        Debug.log( Debug.UNIT_TEST, map.toString() );

        Debug.log( Debug.UNIT_TEST, "Default properties:\n" + map.toString() );


        Debug.log( Debug.UNIT_TEST, "END: Properties cache test.\n\n" );
    }


    /*************************************************************************/

    private static void testSequences ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Database sequence test ..." );

        String seqName = "TestSequence";

        DBConnectionPool.setThreadSpecificPoolKey( null );

        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );

        DBConnectionPool.setThreadSpecificPoolKey( "account1" );


        Debug.log( Debug.UNIT_TEST, "Account 1: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Account 1: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Account 1: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Account 1: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );

        DBConnectionPool.setThreadSpecificPoolKey( "account2" );


        Debug.log( Debug.UNIT_TEST, "Account 2: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Account 2: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Account 2: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Account 2: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );

        DBConnectionPool.setThreadSpecificPoolKey( null );

        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( Debug.UNIT_TEST, "Default: Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );

        DBConnectionPool.setThreadSpecificPoolKey( null );


        Debug.log( Debug.UNIT_TEST, "END: Database sequence test.\n\n" );
    }


    /*************************************************************************/

    private static void testDBLog ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: DBLog test ..." );
        
        Connection dbCon = null;

        // Test against account 2.
        try
        {
            DBConnectionPool.setThreadSpecificPoolKey( "account2" );
            
            dbCon = DBConnectionPool.getInstance().acquireConnection( );
                
            // First, re-create the unit test table in the database.
            try
            {
                Statement stmt = dbCon.createStatement( );
                
                Debug.log( Debug.UNIT_TEST, "Creating database table 'dblog_unit_test'." );
                
                // stmt.executeUpdate( "DROP TABLE dblog_unit_test" );
                
                stmt.executeUpdate( "CREATE TABLE dblog_unit_test ( InsertDate DATE, Status VARCHAR2(3), Message LONG)" );
                
                Debug.log( Debug.UNIT_TEST, "Database table 'dblog_unit_test' created." );
                
                stmt.close( );
            }
            catch ( Exception e )
            {
                Debug.log( Debug.UNIT_TEST, "ERROR: " + e.getMessage() );
            }
        
        
            String tableName = "dblog_unit_test";
            
            NVPair[] columnData = new NVPair[ 3 ];
            
            columnData[0] = new NVPair( "InsertDate", "SYSDATE" );
            columnData[1] = new NVPair( "Status", "12" );
            columnData[2] = new NVPair( "MESSAGE", "This is a short message." );
            
            DBLog.logAsciiMessageToDatabase( tableName, columnData );            
            DBLog.logBinaryMessageToDatabase( tableName, columnData );            
        }
        catch ( Exception e )
        {
            Debug.log( Debug.UNIT_TEST, "ERROR: " + e.getMessage() );
        }
        finally
        {
            if ( dbCon != null )
            {
                DBConnectionPool.getInstance().releaseConnection( dbCon );

                dbCon = null;
            }
        }
            

        // Test against the default account.
        try
        {
            DBConnectionPool.setThreadSpecificPoolKey( null );
            
            dbCon = DBConnectionPool.getInstance().acquireConnection( );
                
            // First, re-create the unit test table in the database.
            try
            {
                Statement stmt = dbCon.createStatement( );
                
                Debug.log( Debug.UNIT_TEST, "Creating database table 'dblog_unit_test'." );
                
                // stmt.executeUpdate( "DROP TABLE dblog_unit_test" );
                
                stmt.executeUpdate( "CREATE TABLE dblog_unit_test ( InsertDate DATE, Status VARCHAR2(3), Message LONG)" );
                
                Debug.log( Debug.UNIT_TEST, "Database table 'dblog_unit_test' created." );
                
                stmt.close( );
            }
            catch ( Exception e )
            {
                Debug.log( Debug.UNIT_TEST, "ERROR: " + e.getMessage() );
            }
        
        
            String tableName = "dblog_unit_test";
            
            NVPair[] columnData = new NVPair[ 3 ];
            
            columnData[0] = new NVPair( "InsertDate", "SYSDATE" );
            columnData[1] = new NVPair( "Status", "12" );
            columnData[2] = new NVPair( "MESSAGE", "This is a short message." );
            
            DBLog.logAsciiMessageToDatabase( tableName, columnData );            
            DBLog.logBinaryMessageToDatabase( tableName, columnData );            
        }
        catch ( Exception e )
        {
            Debug.log( Debug.UNIT_TEST, "ERROR: " + e.getMessage() );
        }
        finally
        {
            if ( dbCon != null )
            {
                DBConnectionPool.getInstance().releaseConnection( dbCon );

                dbCon = null;
            }
        }

        Debug.log( Debug.UNIT_TEST, "END: DBLog test.\n\n" );
    }


    /*************************************************************************/

    private static void testPersistentIOR ( String[] args ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Persistent IOR test ..." );

        try
        {
            String objectName = "NameService";
            
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init( args, null );
            
            org.omg.CORBA.Object corbaObject = orb.resolve_initial_references( objectName );
            
            String objectIOR = orb.object_to_string( corbaObject );
            
            String testIOR = PersistentIOR.get( objectName );
            
            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );
            

            // Test the default case first.
            DBConnectionPool.setThreadSpecificPoolKey( null );

            PersistentIOR.set( objectName, objectIOR );
            
            PersistentIOR.set( objectName, objectIOR );

            
            testIOR = PersistentIOR.get( objectName );
            
            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );
            
            
            if ( testIOR.equals( objectIOR ) )
                Debug.log( Debug.UNIT_TEST, "Good: IORs are the same." );
            else
                Debug.log( Debug.UNIT_TEST, "ERROR: IORs are different." );


            // PersistentIOR.remove( objectName );

            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );


            // Test account 1 case first.
            DBConnectionPool.setThreadSpecificPoolKey( "account1" );

            PersistentIOR.set( objectName, objectIOR );
            
            PersistentIOR.set( objectName, objectIOR );

            
            testIOR = PersistentIOR.get( objectName );
            
            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );
            
            
            if ( testIOR.equals( objectIOR ) )
                Debug.log( Debug.UNIT_TEST, "Good: IORs are the same." );
            else
                Debug.log( Debug.UNIT_TEST, "ERROR: IORs are different." );


            // PersistentIOR.remove( objectName );

            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );


            // Test account 1 case first.
            DBConnectionPool.setThreadSpecificPoolKey( "account2" );

            PersistentIOR.set( objectName, objectIOR );
            
            PersistentIOR.set( objectName, objectIOR );

            
            testIOR = PersistentIOR.get( objectName );
            
            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );
            
            
            if ( testIOR.equals( objectIOR ) )
                Debug.log( Debug.UNIT_TEST, "Good: IORs are the same." );
            else
                Debug.log( Debug.UNIT_TEST, "ERROR: IORs are different." );


            // PersistentIOR.remove( objectName );

            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );


            DBConnectionPool.setThreadSpecificPoolKey( null );
        }
        catch ( Exception e )
        {
            e.printStackTrace( );

            throw new FrameworkException( e.toString() );
        }

        Debug.log( Debug.UNIT_TEST, "END: Persistent IOR test.\n\n" );
    }


    /*************************************************************************/

    private static void redirectLogs ( String fileName )
    {
        try
        {
            PrintStream ps = new PrintStream( new FileOutputStream( fileName ) );

            System.setOut( ps );
            System.setErr( ps );
        }
        catch ( IOException ioe )
        {
            System.err.println( ioe );
        }
    }


    private static int global_pon = 0;
}

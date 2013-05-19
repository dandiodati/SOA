package com.nightfire.framework.test;

import java.io.*;
import java.util.*;
import java.sql.*;

import org.omg.CORBA.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;


/*
 * Class for testing message parsing, generating and mapping.
 */
class TestDatabase
{
    private static final int PERF_TIMER = 98;

    public static void main ( String[] args )
    {
        Debug.showLevels();
        Debug.enableAll();
        // FrameworkException.showStackTrace( );

        redirectLogs( "database_test.out" );

        try
        {
            SetEnvironment.setSystemProperties( "./test.properties", true );

            testLikeProperties( );

            testLogicalLock( );

            testDBLog( );

            testPersistentIOR( args );

            testPropertyNames( );

            for ( int Ix = 0;  Ix < 3; Ix ++ )
                testSequences( );

            for ( int Ix = 0;  Ix < 3; Ix ++ )
                testProperties( );

            ObjectFactory.log( );
        }
        catch ( FrameworkException fe )
        {
            System.err.println( fe.toString() );

            fe.printStackTrace( );
        }
    }


    /*************************************************************************/

    public TestDatabase ( )   { /*EMPTY*/ }



    /*************************************************************************/

    private static void testLogicalLock ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Logical lock test ..." );
        
        String key = "test-lock-name";

        DBInterface.initialize( );
        
        try
        {
            boolean lockFlag = LogicalLock.lock( key, 5, 5 );
            
            Debug.log( Debug.UNIT_TEST, "Got lock for [" + key + "]? " + lockFlag );
            
            
            lockFlag = LogicalLock.lock( key, 5, 5 );
            
            Debug.log( Debug.UNIT_TEST, "Got lock for [" + key + "]? " + lockFlag );
            
            
            lockFlag = LogicalLock.unlock( key );
            
            Debug.log( Debug.UNIT_TEST, "Unlocked lock for [" + key + "]? " + lockFlag );
            
            
            lockFlag = LogicalLock.unlock( key );
            
            Debug.log( Debug.UNIT_TEST, "Unlocked lock for [" + key + "]? " + lockFlag );
        }
        catch ( Exception e )
        {
            Debug.log( Debug.ALL_ERRORS, e.toString() + "\n" + Debug.getStackTrace( e ) );
        }
        
        DBInterface.refreshConnection( );
        
        Debug.log( Debug.UNIT_TEST, "END: Logical lock test.\n\n" );
    }


    /*************************************************************************/

    private static void testDBLog ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: DBLog test ..." );

        DBInterface.initialize( );

        Connection dbCon = DBInterface.getConnection( );

	// First, re-create the unit test table in the database.
        try
        {
            Statement stmt = dbCon.createStatement( );

            Debug.log( Debug.UNIT_TEST, "Creating database table 'dblog_unit_test'." );

            stmt.executeUpdate( "DROP TABLE dblog_unit_test" );

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
	    
	// Perform some test inserts.
	for ( int Ix = 0;  Ix < 10;  Ix ++ )
	{
            Debug.log( Debug.UNIT_TEST, "Inserting database row via DBLog: " + Ix );

            DBLog.logBinaryMessageToDatabase( tableName, columnData );            
	}

        DBInterface.refreshConnection( );

        Debug.log( null, Debug.UNIT_TEST, "END: DBLog test.\n\n" );
    }


    /*************************************************************************/

    private static void testPropertyNames ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: Iterated property name test ..." );

        Debug.log( null, Debug.UNIT_TEST, "Iterated property name [" + 
                   PersistentProperty.getPropNameIteration( "foo", 0 ) + "]" );

        Debug.log( null, Debug.UNIT_TEST, "Iterated property name [" + 
                   PersistentProperty.getPropNameIteration( "foo", 1 ) + "]" );

        Debug.log( null, Debug.UNIT_TEST, "Iterated property name [" + 
                   PersistentProperty.getPropNameIteration( "foo", 2 ) + "]" );

        Debug.log( null, Debug.UNIT_TEST, "END: Iterated property name test.\n\n" );
    }


    /*************************************************************************/

    private static void testSequences ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: Database sequence test ..." );

        DBInterface.initialize( );

        String seqName = "TestSequence";


        Connection c = DBInterface.getConnection( );

        try
        {
            DatabaseMetaData dmd = c.getMetaData( );
            
            String prodName = dmd.getDatabaseProductName( );

            Debug.log( Debug.UNIT_TEST, "Database product name [" + prodName + "]." );
        }
        catch ( SQLException sqle )
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Could not get database vendor product name:\n" + 
                       DBInterface.getSQLErrorMessage(sqle) );
        }


        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );
        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        try
        {
            Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.setNextSequenceValue(seqName, 17 ) + "]" );
        }
        catch ( Exception e )
        {
            Debug.log( Debug.UNIT_TEST, e.toString() );
        }
        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getCurrentSequenceValue(seqName) + "]" );
        Debug.log( null, Debug.UNIT_TEST, "Sequence value [" + PersistentSequence.getNextSequenceValue(seqName) + "]" );

        DBInterface.refreshConnection( );

        Debug.log( null, Debug.UNIT_TEST, "END: Database sequence test.\n\n" );
    }


    /*************************************************************************/

    private static void testProperties ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: Properties cache test ..." );

        DBInterface.initialize( );

        Hashtable map = null;

        PersistentProperty.log( );
        
        map = PersistentProperty.getProperties( "foo" );
        
        PersistentProperty.log( );
        
        map = PersistentProperty.getProperties( "bar", "none" );
        
        PersistentProperty.log( );
        
        map = PersistentProperty.getProperties( "bar", "empty" );
        
        PersistentProperty.log( );
        
        map = PersistentProperty.getProperties( "UNIT", "TEST" );
        
        PersistentProperty.log( );
        

        Debug.log( Debug.UNIT_TEST, "Persistent properties before re-initialization." );
        PersistentProperty.log( );

        PersistentProperty.initialize( );

        Debug.log( Debug.UNIT_TEST, "Persistent properties after re-initialization." );
        PersistentProperty.log( );


        map = PersistentProperty.getProperties( "foo" );
        
        PersistentProperty.log( );
        
        map = PersistentProperty.getProperties( "bar", "none" );
        
        PersistentProperty.log( );
        
        map = PersistentProperty.getProperties( "bar", "empty" );
        
        PersistentProperty.log( );
        
        map = PersistentProperty.getProperties( "UNIT", "TEST" );
        
        PersistentProperty.log( );


        String propName = null;

        propName = "DBDRIVER";
        Debug.log( null, Debug.UNIT_TEST, "Property name [" + propName + "], value [" + 
                   PersistentProperty.get( "UNIT", "TEST", propName ) + "]." );

        propName = "DBNAME";
        Debug.log( null, Debug.UNIT_TEST, "Property name [" + propName + "], value [" + 
                   PersistentProperty.get( "UNIT", "TEST", propName ) + "]." );

        propName = "DBUSER";
        Debug.log( null, Debug.UNIT_TEST, "Property name [" + propName + "], value [" + 
                   PersistentProperty.get( "UNIT", "TEST", propName ) + "]." );

        propName = "DBPASSWORD";
        Debug.log( null, Debug.UNIT_TEST, "Property name [" + propName + "], value [" + 
                   PersistentProperty.get( "UNIT", "TEST", propName ) + "]." );
        

        DBInterface.refreshConnection( );

        Debug.log( null, Debug.UNIT_TEST, "END: Properties cache test.\n\n" );
    }


    /*************************************************************************/

    private static void testLikeProperties ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: Like properties cache test ..." );

        Hashtable allProps = new Hashtable( );

        allProps.put( "moe_0", "huh?" );
        allProps.put( "group_name_0", "hello" );
        allProps.put( "curly", "huh?" );
        allProps.put( "group_name_200", "hello" );
        allProps.put( "group_name_1", "hello" );
        allProps.put( "larry", "huh?" );
        allProps.put( "group_name_2", "hello" );
        allProps.put( "group_asdf_1", "huh?" );
        allProps.put( "group_name_10", "hello" );
        allProps.put( "group_name_asdf", "hello" );
        allProps.put( "group_name_7", "hello" );
        allProps.put( "group_name_11", "hello" );
        allProps.put( "group", "huh?" );

        NVPair[] properties = PersistentProperty.getPropertiesLike( allProps, "group_name" );

        for ( int Ix = 0;  Ix < properties.length;  Ix ++ )
        {
            Debug.log( Debug.UNIT_TEST, "Name [" + properties[Ix].name 
                       + "], value [" + (String)(properties[Ix].value) + "]." );
        }


        properties = PersistentProperty.getGroupedProperties( allProps, "_0" );
        
        for ( int Ix = 0;  Ix < properties.length;  Ix ++ )
        {
            Debug.log( Debug.UNIT_TEST, "Grouped: Name [" + properties[Ix].name 
                       + "], value [" + (String)(properties[Ix].value) + "]." );
        }

        Debug.log( null, Debug.UNIT_TEST, "END: Like properties cache test.\n\n" );
    }


    /*************************************************************************/

    private static void testPersistentIOR ( String[] args ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: Persistent IOR test ..." );

        DBInterface.initialize( );

        try
        {
            String objectName = "NameService";
            
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init( args, null );
            
            org.omg.CORBA.Object corbaObject = orb.resolve_initial_references( objectName );
            
            String objectIOR = orb.object_to_string( corbaObject );
            
            
            String testIOR = PersistentIOR.get( objectName );
            
            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );
            
            
            PersistentIOR.set( objectName, objectIOR );
            
            PersistentIOR.set( objectName, objectIOR );

            
            testIOR = PersistentIOR.get( objectName );
            
            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );
            
            
            if ( testIOR.equals( objectIOR ) )
                Debug.log( Debug.UNIT_TEST, "Good: IORs are the same." );
            else
                Debug.log( Debug.UNIT_TEST, "ERROR: IORs are different." );


            PersistentIOR.remove( objectName );

            Debug.log( Debug.UNIT_TEST, "Object named [" + objectName + "] has persistent IOR [" + testIOR + "]." );
        }
        catch ( Exception e )
        {
            e.printStackTrace( );

            throw new FrameworkException( e.toString() );
        }

        DBInterface.refreshConnection( );

        Debug.log( null, Debug.UNIT_TEST, "END: Persistent IOR test.\n\n" );
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

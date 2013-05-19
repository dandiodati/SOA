package com.nightfire.framework.test;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.parser.xml.*;

/*
 * Class for testing message parsing, generating and mapping.
 */
class TestUtils implements CSVFileConsumer
{
    private static final String ROOT_DIRECTORY = "D:/NFCore/nfcommon/";


    private static final int PERF_TIMER = 98;

    public static void main ( String[] args )
    {
        Debug.showLevels();
        Debug.enableAll();
        Debug.enableThreadLogging();
        Debug.configureFromProperties();
        // FrameworkException.showStackTrace( );

        redirectLogs( "utils_test.out" );

        Debug.log( Debug.NORMAL_STATUS, "Util test proceeding normally (NORMAL_STATUS test)");

        try
        {
            SetEnvironment.setSystemProperties( ROOT_DIRECTORY + "com/nightfire/framework/test/test.properties", true );

            testLoadProperties( );

            testThreadedLogging( );

            testTokenReplacer( );

            testStringUtils( );

            testFileUtils( );

            testFileCache( );

            testCSVReader( );

            ObjectFactory.log( );

            // NOTE:  Make this the last call, as it will terminate the test application!!!
            testAssertion( );
        }
        catch ( FrameworkException fe )
        {
            System.err.println( fe.toString() );
        }
    }


    /*************************************************************************/

    public TestUtils ( )   { /*EMPTY*/ }


    /*************************************************************************/

    private static void testLoadProperties ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Load properties test ..." );

        try
        {
            Properties props = FileUtils.loadProperties( null, "test.properties" );

            Debug.log( Debug.UNIT_TEST, props.toString() );

            Properties moreProps = FileUtils.loadProperties( props, "test.properties" );

            Debug.log( Debug.UNIT_TEST, moreProps.toString() );
        }
        catch ( Exception e )
        {
            e.printStackTrace( );
        }

        Debug.log( Debug.UNIT_TEST, "END: Load properties test.\n\n" );
    }


    private static void testThreadedLogging ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Threaded logging test ..." );

        try
        {
            Thread[] threads = new Thread[ 10 ];

            for ( int Ix = 0;  Ix < 10;  Ix ++ )
            {
                threads[Ix] = new Thread( new ThreadedLogging( "threadedLogging" + Ix ) );

                threads[Ix].start();
            }

            for ( int Ix = 0;  Ix < 10;  Ix ++ )
                threads[Ix].join( );
        }
        catch ( Exception e )
        {
            e.printStackTrace( );
        }

        Debug.log( Debug.UNIT_TEST, "END: Threaded logging test.\n\n" );
    }


    private static class ThreadedLogging implements Runnable
    {
        public ThreadedLogging ( String fileName )
        {
            this.fileName = fileName;
        }

        public void run ( )
        {
            Debug.setThreadLogFileName( fileName );

            for ( int Ix = 0;  Ix < 10; Ix ++ )
            {
                Debug.log( Debug.UNIT_TEST, "Message # " + Ix + " for file " + fileName );

                try
                {
                    Thread.currentThread().sleep( 1000 );
                }
                catch( Exception e )
                {
                    System.err.println( e );
                }
            }
        }

            private String fileName;
    }

    /*************************************************************************/

    private static void testTokenReplacer ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Token replacer test ..." );


        String template = FileUtils.readFile( "./test.template" );
        String data     = FileUtils.readFile( "./msg_api_test.xml" );

        XMLMessageParser p = new XMLMessageParser( );

        p.parse( data );

        TokenDataSource tds = new XMLTokenDataSourceAdapter( p );


        Debug.log( Debug.UNIT_TEST, "Template:\n\n[" + template + "]\n\n" );

        TokenReplacer tr = new TokenReplacer( template );

        String result = tr.generate( tds );

        Debug.log( Debug.UNIT_TEST, "Replacement results:\n\n[" + result + "]\n\n" );

        tr.setAllDataOptional( true );

        result = tr.generate( tds );

        Debug.log( Debug.UNIT_TEST, "Replacement results:\n\n[" + result + "]\n\n" );


        Debug.log( Debug.UNIT_TEST, "END: Token replacer test.\n\n" );
    }


    /*************************************************************************/

    private static void testStringUtils ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: String utility test ..." );

        int num = 123;

        Debug.log( null, Debug.UNIT_TEST, "Number [" + num + "], padded [" + StringUtils.padNumber( num, 5, true, '0' ) + "]" );
        Debug.log( null, Debug.UNIT_TEST, "Number [" + num + "], padded [" + StringUtils.padNumber( num, 5, false, '0' ) + "]" );


        String tmp = " Four score and seven years ago ...  ";

        Debug.log( null, Debug.UNIT_TEST, "String [" + tmp + "], transformed [" +
                   StringUtils.replaceSubstrings( tmp, " ", " blah blah blah " ) + "]" );


        Debug.log( null, Debug.UNIT_TEST, "END: String utility test.\n\n" );
    }


    /*************************************************************************/

    private static void testFileUtils ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: File utility test ..." );

        String srcDir = ROOT_DIRECTORY + "com/nightfire/framework/test/source/";
        String tgtDir = ROOT_DIRECTORY + "com/nightfire/framework/test/target/";

        String binaryFileName = "TestUtils.class";

        byte[] binaryData = FileUtils.readBinaryFile( binaryFileName );

        FileUtils.writeBinaryFile( binaryFileName + ".junk", binaryData );

        String[] files = FileUtils.getAvailableFileNames( srcDir );

        if ( files.length > 0 )
        {
            String buf = FileUtils.readFile( srcDir + files[0] );

            Debug.log( null, Debug.UNIT_TEST, "File contents [" + buf + "]" );

            FileUtils.FileStatistics fs = FileUtils.getFileStatistics( srcDir + files[0] );

            Debug.log( null, Debug.UNIT_TEST, "FILE STATISTICS:  exists [" + fs.exists + "], writeable [" + fs.writeable +
                       "], readable [" + fs.readable + "], isFile [" + fs.isFile + "],\n isDirectory [" + fs.isDirectory +
                       "], length [" + fs.length + "], lastModified [" + fs.lastModified + "]" );

            FileUtils.writeFile( tgtDir + files[0], buf );

            FileUtils.writeFile( tgtDir + files[0] + ".test", buf );

            FileUtils.removeFile( srcDir + files[0] );

            FileUtils.moveFile( srcDir, tgtDir + files[0] );
        }

        Debug.log( null, Debug.UNIT_TEST, "END: File utility test.\n\n" );
    }



    /*************************************************************************/

    private static void testFileCache ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: File cache test ..." );

        FileCache fc = new FileCache( );

        fc.get( "TestUtils.java" );

        fc.logKeys( );

        fc.get( "TestUtils.java" );

        fc.logKeys( );

        fc.put( "foo", "bar" );

        fc.logKeys( );

        fc.put( "TestUtils.java", "bar" );

        fc.logKeys( );

        if ( fc.contains( "TestUtils.java" ) )
            Debug.log( null, Debug.UNIT_TEST, "TestUtils.java is in cache." );

        fc.remove( "TestUtils.java" );

        if ( !fc.contains( "TestUtils.java" ) )
            Debug.log( null, Debug.UNIT_TEST, "TestUtils.java is not in cache." );

        fc.logKeys( );

        Debug.log( null, Debug.UNIT_TEST, "END: File cache test.\n\n" );
    }


    /*************************************************************************/

    private static void testCSVReader ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: CSV reader test ..." );

        CSVFileReader r = new CSVFileReader( new TestUtils(), ROOT_DIRECTORY + "com/nightfire/framework/test/test_mappings.csv" );

        r.execute( );

        Debug.log( null, Debug.UNIT_TEST, "END: CSV reader test.\n\n" );
    }


    /*************************************************************************/

    private static void testAssertion ( ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "\n\nBEGIN: Assertion test ..." );

        int value = -1;

        Debug.assertTrue( value < 0, "Value is less than zero." );

        Debug.assertTrue( value > 0, "Value is greater than zero." );

        Debug.log( null, Debug.UNIT_TEST, "END: Assertion test.\n\n" );
    }


    /*************************************************************************/

    public void processLine ( StringTokenizer st ) throws FrameworkException
    {
        Debug.log( null, Debug.UNIT_TEST, "Starting to process CSV line ..." );

        int tokNum = 0;

        while( st.hasMoreTokens() )
        {
            tokNum ++;

            String val = st.nextToken();

            Debug.log( null, Debug.UNIT_TEST, "Token #" + tokNum + ", value [" + val + "]" );
        }

        Debug.log( null, Debug.UNIT_TEST, "Done processing CSV line." );
    }


    /*************************************************************************/

    public void initialize ( )
    {
        Debug.log( null, Debug.UNIT_TEST, "Consumer Initializing before CSV file processing." );
    }


    /*************************************************************************/

    public Object doneProcessing ( )
    {
        Debug.log( null, Debug.UNIT_TEST, "Consumer done processing CSV file." );

        return null;
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
}

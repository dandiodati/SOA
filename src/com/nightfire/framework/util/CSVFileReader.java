/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;
import java.io.*;


/**
 * Class CSVFileReader provides a utility that reads a comma-separated-value (*.csv) file
 * and uses a CSV file consumer to process it.
 */
public class CSVFileReader
{
    /**
     * Create a CSV file reader.
     *
     * @param  consumer     Consumer of CSV file data.
     * @param  csvFileName  Name of CSV file to get data from.
     */
    public CSVFileReader ( CSVFileConsumer consumer, String csvFileName )
    {
        Debug.log( null, Debug.IO_STATUS, "Creating a CSV file reader for file [" + csvFileName + "]." );

        this.consumer    = consumer;
        this.csvFileName = csvFileName;
    }


    /**
     * Process the named CSV file using the consumer given during object construction.
     *
     * @return  Object returned by consumer's doneProcessing() method, which is called
     *          after all lines have been processed.
     *
     * @exception  FrameworkException  Thrown on I/O or format errors.
     */
    public Object execute ( ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Reading CSV file." );

        String data = FileUtils.readFile( csvFileName );

        BufferedReader br = new BufferedReader( new StringReader( data ) );

        int lineNum = 0;

        // Let consumer perform any pre-processing initialization.
        consumer.initialize( );

        try
        {
            do
            {
                String line = br.readLine( );

                if ( line == null )
                    break;

                // Don't process any lines containing only whitespace.
                line = line.trim( );

                if ( line.length() == 0 )
                    continue;

                // Skip any comment lines.
                if ( line.startsWith( "#" ) )
                     continue;

                lineNum ++;

                Debug.log( null, Debug.IO_STATUS, "Processing CSV file line number [" + lineNum + "]." );

                Debug.log( null, Debug.IO_DATA, "Line contents [" + line + "]." );

                StringTokenizer st = new StringTokenizer( line, "," );

                consumer.processLine( st );
            }
            while ( true );
        }
        catch ( IOException ioe )
        {
            String errMsg = new String( "ERROR: Could not process CSV file [" + csvFileName + "]:\n" + ioe.toString() );

            Debug.log( null, Debug.ALL_ERRORS, errMsg );

            throw new FrameworkException( errMsg );
        }

        Debug.log( null, Debug.IO_STATUS, "Done processing CSV file." );

        // Let consumer return any post-processing results to caller.
        return( consumer.doneProcessing( ) );
    }


    CSVFileConsumer consumer;
    String          csvFileName;
}

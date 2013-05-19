/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;


/**
 * Interface for classes which can consume lines of data from 
 * comma-separated-value (*.CSV) files.
 */
public interface CSVFileConsumer
{
    /**
     * Process a single line from a CSV file.
     *
     * @param  st  StringTokenizer for one line which returns tokens
     *             separated by commas.
     *
     * @exception  FrameworkException  Thrown on format errors.
     */
    public void processLine ( StringTokenizer st ) throws FrameworkException;


    /**
     * Called by CSV file reader (once) in method execute( ) before file 
     * processing begins.  Used for initialization of consumer.
     */
    public void initialize ( );


    /**
     * Called by CSV file reader (once) after file processing ends.
     * Object returned is returned by reader execute( ) method.
     *
     * @return  Any object that CSVFileReader.execute() should return.
     */
    public Object doneProcessing ( );
}

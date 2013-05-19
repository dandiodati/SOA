///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2004 Neustar, Inc. All rights reserved.
//
///////////////////////////////////////////////////////////////////////////////
package com.nightfire.spi.neustar_soa.file;

import java.util.List;

import com.nightfire.framework.util.FrameworkException;

/**
 * Implementations of the interface will be used by the DelimitedFileReader to
 * determine what to do with a line of input. When given a file to read,
 * the delimited file reader will call init() on its consumer instance.
 * It then tokenizes each line, passing the tokens to the process() method.
 * When all lines from the file have been read, the cleanup() method is
 * called to release any resources used (e.g. database connections).
 */
public interface TokenConsumer {

   	/**
     * This is called by the DelimitedFileReader before processing an input
     * file.
     *
     * @param filename String the name of the filename being processed
     *                        is passed in because the filename may
     *                        contain valuable information such as the date that
     *                        the file was generated.
     *
     * @throws FrameworkException if an error occurs.
     */
   	public void init(String filename) throws FrameworkException;

    /**
     * This method processes the tokens from a single line of input.
     *
     * @param tokens String[]
     * @param tokenType contains the type of network BDD data.
     * @throws FrameworkException if an error occurs while processing the tokens.
     */
   
   	public boolean process(String[] tokens, String tokenType) throws FrameworkException;


    /**
     * This is called when processing is complete to tell the consumer to
     * release any resources that it was using.
     *
     * @param success boolean used to indicate whether processing was successful.
     *                        This flag is used by implementations to determine
     *                        if changes should be commited or not.
     */
   
   	public void cleanup(boolean success);
   
	/**
	 * This method will give Failed record list
	 */
   	public List getFailedList( ) ;
   	
	/**
	 * This method will give records count
	 */
	public int[] getRecords() ;

}

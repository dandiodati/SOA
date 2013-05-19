/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //gateway/R4.4/com/nightfire/spi/common/driver/MessageProcessor.java#1 $
 */
package com.nightfire.spi.common.driver;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.NVPair;

/**
 * Interface that must be implemented by all message-processing components 
 * (adapters, transformers, ..., etc) to make them plug-able into the driver.
 */
public interface MessageProcessor
{

	// public static final String SOURCE_ID = "$Id: //gateway/R4.4/com/nightfire/spi/common/driver/MessageProcessor.java#1 $";	


    /**
     * Processes the input message or context information and optionally returns
     * a value.  The following driver-processor interaction rules apply:
     *		Non-null INPUT and non-null OUTPUT	-	Driver sends output to next message-processor in chain.
     *		Non-null INPUT and null OUTPUT		-	Processor is batching inputs for subsequent release.  Driver skips 
     *                                              processors in chain that follow the current one.
     *		Null INPUT and non-null OUTPUT		-	Driver has no more inputs for this processor.  Batching processor is emiting batched 
     *                                              output at this point, which will be sent by driver to next message-processor in chain.
     *		Null INPUT and null OUTPUT			-	Driver has no more inputs for this processor.  Processor indicates to driver
     *                                              that it is done processing.
     *
     * @param  context  The  message context.
     *
     * @param  input  Input message object to process.
     *
     * @return  An array of name-value pair objectss, or null if none.
     *
     * @exception  MessageException  Thrown if processing fails due to invalid data.
     * @exception  Processingxception  Thrown if processing fails due to system errors.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject input ) throws MessageException, ProcessingException;


    /**
     * Called to initialize a message processor object.
     * 
     * @param  key   Property-key used to locate initialization properties.
     * @param  type  Property-type used to locate initialization properties.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
     */
    public void initialize ( String key, String type ) throws ProcessingException;


    /**
     * Returns name of this message processor.
     *
     * @return  Name of this message processor.
     */
    public String getName ();

    /**
     * Called to clean up this processor so that it can reset
     * itself and release any resources being held.  
     * This allows message-processors to be cached as a 
     * performance optimization.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
    */
    public void cleanup ()throws ProcessingException;



}

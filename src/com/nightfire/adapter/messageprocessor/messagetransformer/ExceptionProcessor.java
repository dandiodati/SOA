/**
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */
package com.nightfire.adapter.messageprocessor.messagetransformer;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.common.ProcessingException;
import com.nightfire.spi.common.driver.MessageObject;

/**
* This class is used to provide consistent handling of Exceptions thrown
* while the MessageTransformer is processing.
*/
public class ExceptionProcessor {

    /**
    * The name of the exception processor as taken from the EXCEPTION_PROCESSOR
    * property. This is an optional property, so this value may be null.
    */
    private String exceptionProcessor;

    /**
    * This constructor sets the name of the exception processor to null.
    */
    public ExceptionProcessor(){

       exceptionProcessor = null;

    }
    
    /**
    * This constructor sets the name of the exception processor to the
    * given <code>processorName</code>.
    *
    * @param the name of the exception processor.
    */
    public ExceptionProcessor(String processorName){

       exceptionProcessor = processorName;

    }

    /**
    * Assigns the name of the exception processor to the
    * given <code>processorName</code>.
    *
    * @param the name of the exception processor.    
    */
    public void setExceptionProcessorName(String processorName){

       exceptionProcessor = processorName;

    }

    /**
    * Accesses the name of the exception processor.
    *
    * @return the name of the exception processor being used. This may be null.
    */    
    public String getExceptionProcessorName(){

       return exceptionProcessor;

    }

    

    /**
    * This method takes an Exception, and if it is an instance of
    * MessageException or ProcessingException, and there is an
    * exception processor specified, then it returns an NVPair where the
    * name of the NVPair is the name of the exception processor, and the
    * value of the NVPair is a MessageObject containing the given exception.
    * If the exception is not a MessageException or ProcessingException,
    * a new ProcessingException is created with the message from the given
    * exception, and this new ProcessingException is processed normally.
    * If the exception processor name is null, then there is no exception
    * processor, and the exception will simply be thrown.
    *
    * @param ex Any exception, but this method expects instances of
    *           ProcessingException or MessageException.
    * @return if there is an exception processor specified, then it returns an
    *         NVPair where the name of the NVPair is the name of the exception
    *         processor, and the value of the NVPair is a MessageObject
    *         containing the given exception.
    * @throws ProcessingException The given exception <code>ex</code> was an
    *                             instance of ProcessingException and is thrown
    *                             if there is no exception processor assigned.
    * @throws MessageException The given exception <code>ex</code> was an
    *                          instance of MessageException and is thrown
    *                          if there is no exception processor assigned.
    */
    public NVPair process(Exception ex) throws ProcessingException,
                                               MessageException{

       NVPair result;

       if(ex instanceof ProcessingException){

          result = process((ProcessingException) ex);

       }
       else if(ex instanceof MessageException){

          result = process((MessageException) ex);

       }
       else{

          String unknown = "An unexpected error occured: "+ex.getMessage();
          result = process( new ProcessingException( unknown ) );

       }

       return result;

    }

    /**
    * This method takes a ProcessingException, and if there is an
    * exception processor specified, then it returns an NVPair where the
    * name of the NVPair is the name of the exception processor, and the
    * value of the NVPair is a MessageObject containing the given exception.
    * If the exception processor name is null, then there is no exception
    * processor, and the exception will simply be thrown.
    *
    * @param ex Any ProcessingException.
    * @return if there is an exception processor specified, then it returns an
    *         NVPair where the name of the NVPair is the name of the exception
    *         processor, and the value of the NVPair is a MessageObject
    *         containing the given exception.
    * @throws ProcessingException The given exception <code>ex</code> is thrown
    *                             if there is no exception processor assigned.
    */
    public NVPair process(ProcessingException ex)
                  throws ProcessingException{

        if(exceptionProcessor == null){
           throw ex;
        }

        MessageObject output = new MessageObject(ex);
        NVPair result = new NVPair(exceptionProcessor, output);

        return result;

    }

    /**
    * This method takes a MessageException, and if there is an
    * exception processor specified, then it returns an NVPair where the
    * name of the NVPair is the name of the exception processor, and the
    * value of the NVPair is a MessageObject containing the given exception.
    * If the exception processor name is null, then there is no exception
    * processor, and the exception will simply be thrown.
    *
    * @param ex Any MessageException.
    * @return if there is an exception processor specified, then it returns an
    *         NVPair where the name of the NVPair is the name of the exception
    *         processor, and the value of the NVPair is a MessageObject
    *         containing the given exception.
    * @throws MessageException The given exception <code>ex</code> is thrown
    *                          if there is no exception processor assigned.
    */
    public NVPair process(MessageException ex)
                  throws MessageException{
    
        if(exceptionProcessor == null){
           throw ex;
        }

        MessageObject output = new MessageObject(ex);
        NVPair result = new NVPair(exceptionProcessor, output);

        return result;

    }

}
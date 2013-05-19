package com.nightfire.adapter.converter.messageprocessor;

import org.w3c.dom.Document;

import com.nightfire.adapter.converter.*;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;

import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.Debug;

import com.nightfire.adapter.messageprocessor.messagetransformer.ExceptionProcessor;

/**
* This is the base class for Converter processors. Converter processors use
* a Converter instance to convert the incoming message and return the result.
*/
public abstract class AbstractConverterProcessor extends MessageProcessorBase{

   protected ExceptionProcessor exceptionProcessor;

   /**
   * Initializes the exception processor from the optional EXCEPTION_PROCESSOR
   * property. 
   *
   * @param key  The persistent property key.
   * @param type The persistent property type.
   */
   public void initialize(String key, String type) throws ProcessingException{

      super.initialize(key, type);

      exceptionProcessor = new ExceptionProcessor(
                getPropertyValue( PropertyConstants.EXCEPTION_PROCESSOR_PROP) );

   }

   /**
   * This processor gets a Converter instance and uses it to convert
   * the input message.
   *
   * @param  context The  message context.
   * @param  message This object contains the input message to
   *                 be converted. 
   *
   * @return  An array of name-value pairs containing the name of the
   *          next processor(s) and the value of the converted message.
   *
   * @exception  MessageException  Thrown if the input message could not be parsed.
   * @exception  Processingxception  Thrown if processing fails due to system errors.
   */
   public NVPair[] process(MessageProcessorContext context,
                           MessageObject message )
                           throws MessageException, ProcessingException{

      // The standard message processor response to a null input message
      if(message == null){
         return null;
      }

      Converter converter = getConverter(context, message);

      try{

         if( Debug.isLevelEnabled(Debug.MAPPING_STATUS) ){
            Debug.log(Debug.MAPPING_STATUS,
                      "Converter input:\n"+
                      message.describe());
         }

         message = converter.convert( message);

      }
      catch(Exception ex){

         if( Debug.isLevelEnabled(Debug.ALL_ERRORS) ){
            Debug.log(Debug.ALL_ERRORS, "Conversion failed: "+ex);
         }
         return processException(ex);

      }

      if( Debug.isLevelEnabled(Debug.MAPPING_STATUS) ){
         Debug.log(Debug.MAPPING_STATUS,
                   "Converter output:\n"+
                   message.describe() );
      }

      return formatNVPair(message);

   }

   /**
   * If there is an EXCEPTION_PROCESSOR specified in the properties,
   * then this will pass the given exception off to that processor.
   * If there is no EXCEPTION_PROCESSOR defined, then the exception
   * will be thrown.
   *
   * @param ex if this is a MessageException or a ProcessingException
   *           the exception is processed as is, otherwise a new
   *           ProcessingException is created from the given Exception. 
   * @return an NVPair array with one element. The name of that element
   *            is the name of the EXCEPTION_PROCESSOR and the value
   *            is a MessageObject containing the exception. 
   */
   protected NVPair[] processException(Exception ex)
                                       throws MessageException,
                                              ProcessingException{

      NVPair[] result = new NVPair[1];
      result[0] = exceptionProcessor.process(ex);
      return result;

   }

   /**
   * Gets the converter that will be used to convert the message.
   *
   * 
   */
   public abstract Converter getConverter( MessageProcessorContext context,
                                           MessageObject message )
                                           throws MessageException,
                                                  ProcessingException; 


}
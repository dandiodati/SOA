/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.messageprocessor;

import com.nightfire.framework.util.*;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;

import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;

import com.nightfire.adapter.converter.*;

import java.util.*;

/**
* This processor gets the name of a Converter class from the CONVERTER_CLASS
* property. It then uses an instance of this type to convert the
* incoming message.
* This processor caches Converter instances and resuses them, so it is
* assumed that any Converter class that this processor is configured to
* use will be stateless (i.e. it is safe to use the same Converter instance
* in mulitple threads with distinct input without any problems.)
*
*/
public class GenericConverter extends AbstractConverterProcessor{

   /**
   * The cache of previously constructed Converter instances.
   */
    protected static CachingConverterFactory converterCache = new CachingConverterFactory();

   /**
   * The specific Converter instance to use in this generic processor.
   */
   private Converter converter;

   /**
   * The specific Converter class name to use in this generic processor.
   */
   private String converterClassName;

   /**
   * Initializes the Converter instance from the class name given in the
   * CONVERTER_CLASS property. This class must implement the
   * <code>com.nightfire.adapter.converter.Converter</code> interface and
   * have a constructor that takes no arguments.
   *
   * @param key  The persistent property key.
   * @param type The persistent property type.
   */
   public void initialize(String key, String type) throws ProcessingException{

      super.initialize(key, type);

      converterClassName = getRequiredPropertyValue( PropertyConstants.CONVERTER_CLASS_PROP );

      try{
          // Try to get a cached instance of this Converter class.
          converter = (Converter) converterCache.get(converterClassName, converterClassName);
      }
      catch(FrameworkException fex){
            Debug.log(Debug.ALL_ERRORS,
                      "Could not create instance of class ["+
                      converterClassName+"]");
            throw new ProcessingException(fex.getMessage());
      }
      catch(ClassCastException ccex){
         
            String error = "Class ["+
                           converterClassName+"] is not an instance of "+
                           Converter.class.getName()+": "+ccex.getMessage();
            Debug.log(Debug.ALL_ERRORS, error);
            throw new ProcessingException(error);

      }
   }

    /**
     * Called to clean up this processor so that it can reset
     * itself and release any resources being held.  
     * This allows message-processors to be cached as a 
     * performance optimization.
     *
     * @exception  ProcessingException  Thrown if initialization fails.
    */
    public void cleanup ()throws ProcessingException
    {
        // return the converter to the cache for use later.
        converterCache.put(converterClassName, converter);
        converter = null;
    }


   /**
   * Gets the converter that will be used to convert the message.
   */
   public Converter getConverter( MessageProcessorContext context,
                                  MessageObject message )
                                  throws MessageException,
                                         ProcessingException{

      return converter;

   }

    private static class CachingConverterFactory extends FactoryCacheBase
    {
        /**
         * Hook that must be imlemented by concrete leaf class to create an
         * object of the requested type.
         *
         * @param  initializer  An optional object argument to be passed
         *                      to the constructor for the new object.
         *
         * @return  An instance of the requested object.
         *
         * @exception  FrameworkException  Thrown if object can't be created.
         */
        protected Object createObject ( Object initializer ) throws FrameworkException
        {
            return ObjectFactory.create((String)initializer);
        }
    }

}

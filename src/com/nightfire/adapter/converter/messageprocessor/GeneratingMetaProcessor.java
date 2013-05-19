/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.messageprocessor;

import com.nightfire.adapter.converter.*;
import com.nightfire.adapter.converter.meta.*;

import com.nightfire.framework.util.*;

import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.spi.common.driver.MessageProcessorContext;

import com.nightfire.common.ProcessingException;
import com.nightfire.framework.message.MessageException;

/**
* This processor loads field definitions from a meta file and uses
* these descriptions of the desired output to convert XML input
* messages.
*/
public class GeneratingMetaProcessor extends AbstractConverterProcessor{

   /**
   * The converter instance that will always be used by this processor.
   */
   private XMLToFieldConverter converter = null;

   /**
   * Loads the root field definition from a meta file given in the property
   * META_FILE. 
   *
   * @param key  The persistent property key.
   * @param type The persistent property type.
   */
   public void initialize(String key, String type) throws ProcessingException{

      super.initialize(key, type);

      if(converter == null){

         String metaFileName = getRequiredPropertyValue( PropertyConstants.META_FILE_PROP );

         try{

            converter = new XMLToFieldConverter( metaFileName );

         }
         catch(FrameworkException fex){

            throw new ProcessingException("Could not load meta data from file ["+
                                          metaFileName+"]: "+fex.getMessage());

         }

      }

   }

   /**
   * Gets field converter that will be used to convert the incoming XML message
   * into the correct output format. This method gets called by the parent
   * class to do the actual message processing.
   */
   public Converter getConverter( MessageProcessorContext context,
                                  MessageObject message )
                                  throws MessageException,
                                         ProcessingException{

      return converter;

   }

   
}
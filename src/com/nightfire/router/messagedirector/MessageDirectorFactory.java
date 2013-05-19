/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.messagedirector;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.router.messagedirector.*;
import com.nightfire.framework.util.*;

import org.w3c.dom.*;

/**
 * Factory for creating MessageDirectors
 * @author Dan Diodati
 */
public class MessageDirectorFactory
{

   /**
    *  Creates the specified Message Director and initializes the MessageDirector.
    *
    *  @param mdClass The class name of the MessageDirector
    *  @param key The property key to initialize the MessageDirector
    *  @param type The property type to initialize the MessageDirector
    *  @return MessageDirector an instance of the mdClass MessageDirector
    *  @throws ProcessingException if there is a  error creating the MessageDirector
    */
   public static MessageDirector create(String mdClass, String key, String type) throws ProcessingException
   {
      MessageDirector md = create(mdClass);
      md.initialize(key,type);

      return (md);
   }

   /**
    *  Creates the specified Message Director with out initializing it.
    *  @param mdClass The class name of the MessageDirector
    *  @return MessageDirector an instance of the mdClass MessageDirector
    *  @throws ProcessingException if there is a  error creating the MessageDirector
    */
   public static MessageDirector create(String mdClass) throws ProcessingException
   {
      MessageDirector md = null;

      try {
         md = (MessageDirector) ObjectFactory.create(mdClass);
      } catch (FrameworkException fe) {
         throw new ProcessingException(fe.getMessage());
      }

      return (md);
   }

}
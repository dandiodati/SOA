package com.nightfire.adapter.converter.util;

import java.lang.reflect.*;

import com.nightfire.framework.util.FrameworkException;

/**
* This class contains any utility methods that are needed by the converter
* infrastructure.
*/
public abstract class ConverterUtils{

   /**
   * This create an instance of the given class type. This code is taken
   * mainly from the ObjectCreator class. It was neccessary to
   * write this version of the method, because the ObjectCreator.create method
   * gets the array of argument types from the list of arguments themselves.
   * An example of problem with this approach would be the case where
   * a class had a constructor that took a single Object as a parameter.
   * If an attempt is made to pass a String as an argument to the create
   * method, then the ObjectCreator looks for a constructor that takes a
   * String argument, and does not find the constructor that takes an
   * Object. 
   */
   public static Object create(Class createMe,
                               Class[] argTypes,
                               Object[] args,
                               Class expectedType) throws FrameworkException{

      Object obj = null;

      try{

       Constructor ctor = createMe.getDeclaredConstructor( argTypes );
       obj = ctor.newInstance( args );

      }
      catch(Exception e){

            String errMsg = null;
            
            if ( e instanceof InvocationTargetException )
                errMsg = ((InvocationTargetException)e).getTargetException().toString( );
            else
                errMsg = e.toString( );
            
            throw new FrameworkException( "Could not create instance of class [" +
                                          createMe.getName() + "]:\n" + errMsg );

      }

        if ( expectedType != null )
        {
            if ( !expectedType.isInstance( obj ) )
            {
                throw new FrameworkException( "Object [" + obj + "] of type [" + 
                                              obj.getClass().getName() +
                                              "] is not of type [" + 
                                              expectedType.getName() + "]." );
            }
        }


      return obj;

   }

}
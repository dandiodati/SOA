/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.spi.common.driver;

import java.util.*;
import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.common.xml.*;

import com.nightfire.common.*;


/**
 * Utility helper class providing methods to convert data from one form to another.
 */
 public class Converter
 {

   /**
    * Return a String instance of the input. The input can be of String or DOM type. 
    * In case of DOM type an XML message is returned.
    *
    * @param input The object to be transformed.
    *
    * @return String equivalent of input.
    *
    * @exception ProcessingException  Thrown if input can't be converted to its string equivalent.
    */
    public static final String getString ( Object input ) throws ProcessingException
    {
      if ( input == null )
      {
        Debug.log ( Debug.ALL_WARNINGS, "Converter: Input to getString is null" );
        return null;
      }

      if ( input instanceof String )
        return ( String ) input;
      else
      if ( input instanceof Document )
      {
        String retString = null;
        try
        {
          retString = XMLLibraryPortabilityLayer.convertDomToString ( ( Document ) input );
        }
        catch ( MessageException me )
        {
          Debug.log ( Debug.ALL_ERRORS, "ERROR: Converter: " + me.toString () );
          throw new ProcessingException ( "Converter: " + me.toString () );
        }

        Debug.log ( Debug.MSG_STATUS, "Converter: Returning string: [" +
          retString + "]" );
        return ( retString );
      }
      else
        throw new ProcessingException ( "ERROR: Converter: Object passed to getString not of type String/Document ");
    }//getString

   /**
    * Return a DOM equivalent of the input. The input can be of String orDOM type. In the 
    * String case, the input should be a valid XML message from which a DOM can be generated.
    *
    * @param input The object to be transformed.
    *
    * @return DOM equivalent of input.
    *
    * @exception ProcessingException  Thrown if DOM instance cannot be generated from the input.
    */
    public static final Document getDOM ( Object input ) throws ProcessingException
    {
      if ( input == null )
      {
        Debug.log ( Debug.ALL_WARNINGS, "Converter: Input to getDOM is null" );
        return null;
      }

      if ( input instanceof Document )
      {
        return ( Document ) input;
      }
      else
      if ( input instanceof String )
      {
        Document retDocument = null;
        try
        {
          retDocument = XMLLibraryPortabilityLayer.convertStringToDom ( ( String ) input );
        }
        catch ( MessageException me )
        {
          throw new ProcessingException ( "Converter: " + me.getMessage () );
        }
        Debug.log ( Debug.MSG_STATUS, "Converter: Returning DOM" );
        return ( retDocument );
      }
      else
        throw new ProcessingException ( "ERROR: Converter: Object passed to getDOM not of type String/Document ");
    }//getDOM

 }//Converter

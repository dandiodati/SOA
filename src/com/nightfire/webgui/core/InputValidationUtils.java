/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import  java.lang.*;
import  java.util.*;

import  org.w3c.dom.*;

import  javax.servlet.*;
import  javax.servlet.http.HttpSession;
import com.nightfire.framework.debug.*;


import  com.nightfire.framework.util.*;
import  com.nightfire.framework.repository.RepositoryManager;
import com.nightfire.framework.message.common.xml.*;
import  com.nightfire.framework.message.MessageException;
import  com.nightfire.webgui.core.*;
import  com.nightfire.webgui.core.resource.*;
import  com.nightfire.webgui.core.xml.*;
import  com.nightfire.webgui.core.beans.*;
import  com.nightfire.webgui.core.resource.*;

import com.nightfire.framework.rules.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.framework.constants.*;
import com.nightfire.webgui.core.resource.ResourceDataCache;



/**
 * Utility class which provides common methods for input validation.
 */
public class InputValidationUtils 
{    
 

  /**
   * Uses a map of meta Fields objects to find all missing required fields.
   *
   * @param bodyGen A xml generator with the data tpo be validated.
   * @param reqFields a <code>Map</code> of all required {@link com.nightfire.webgui.core.meta.Field Field} objects.
   * @return a <code>String</code> value of xml errors or null if there were
   * no errors.
   */
  public static final String validateReqFields(XMLGenerator bodyGen, Map reqFields)
  {
    DebugLogger log = DebugLogger.getLoggerLastApp(InputValidationUtils.class);
    
     ErrorCollection validationErrors = new ErrorCollection();
    if ( log.isDebugEnabled() )
       log.debug("Checking for required fields in data:\n" + bodyGen.describe() );
    
    
    boolean errorsFound = false;

    if (reqFields != null) {
        
        Iterator fields = reqFields.values().iterator();
        while ( fields.hasNext() ) {
            Field f = (Field) fields.next();
            String path = f.getFullXMLPath();

            log.debug("Checking for required field [" + path +"]");
      
            if( isValidForCheck(f) ) {
                try {
          
                    if (! bodyGen.exists( path)|| !StringUtils.hasValue(bodyGen.getValue(path)) ) {
                        log.debug("Field does not exist.");
                        errorsFound = true;
                        appendInvalidField(validationErrors, path,"is a required Field" );
                    }
                }
                catch (MessageException e ) {
                    log.warn("Could not get value of field [" + path +"], skipping validation on this field. ");
                }
        
        
        
            }
        }
    }
    
    if ( errorsFound)
       return validationErrors.toXML();
    else
      return null;
    
    
  }

  
  /**
   * Tests if a field should be tested as a required field.
   * Only returns true if a field is not within any repeating or optional
   * ancestor message container.
   *
   * @param field a <code>Field</code> value
   * @return a <code>boolean</code> value
   */
  private static final boolean isValidForCheck(Field field ) 
  {
    MessageContainer part = field.getParent();
    boolean bool = true;
    
    while ( part != null) {
      if ( part.getRepeatable() || part.getOptional() ) {
        bool = false;
        break;
      }
      
      else
        part = (MessageContainer)part.getParent();
    }

    DebugLogger.getLoggerLastApp(InputValidationUtils.class).debug("Is validation for field supported?, " + bool );
    
    return bool;
  }
  
    /** 
     * Creates and appends validation-error text corresponding to the specified
     * field to the overall error message to be returned for displayed.
     *
     * @param  validationErrors  Overall validation-error ErrorCollection object.
     * @param  path              Field path (Nightfire syntax).
     * @param  msgSuffix         The error message to append after the field name.
     * @param  xPath       Field xpath to be used in the error text.
     */
    private static final void appendInvalidField(ErrorCollection validationErrors, String path, String msgSuffix)
    {
        int    lastDelimiterIndex = path.lastIndexOf('.');
        
        String fieldName          = path;
        
        if (lastDelimiterIndex != -1)
        {
            fieldName = path.substring(lastDelimiterIndex + 1);
        }

        StringBuffer messageBuffer = new StringBuffer(fieldName);
        
        messageBuffer.append(" ").append(msgSuffix);

        RuleError ruleError = new RuleError(null, messageBuffer.toString());
                
        path = ServletUtils.changetoXpathIndexes(path);
        ruleError.setContext(path);
      
        validationErrors.addError(ruleError);
    }
    
}

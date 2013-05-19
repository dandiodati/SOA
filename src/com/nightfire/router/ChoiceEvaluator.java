/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;

import com.nightfire.router.messagedirector.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;

import org.w3c.dom.*;

/**
 * This router component is responsible for creating MessageDirectors,
 * choosing which MessageDirector can route
 * a request message, and shuting down MessageDirectors.
 *
 * @author Dan Diodati
 */
public class ChoiceEvaluator implements RouterConstants {

  private static ChoiceEvaluator singleton = new ChoiceEvaluator();

  public static final String DIRECTOR_KEY_PROP = "MESSAGE_DIRECTOR_KEY";
  public static final String DIRECTOR_TYPE_PROP = "MESSAGE_DIRECTOR_TYPE";
  public static final String DIRECTOR_NAME_PROP = "MESSAGE_DIRECTOR_NAME";
  public static final String DIRECTOR_CLASS_PROP = "MESSAGE_DIRECTOR_CLASS";

  private Map properties;
  private LinkedList choices = new LinkedList();

  protected ChoiceEvaluator() {};

  /**
     * Gets the Singleton instance.
     *
     * @return The singleton ChoiceEvaluator instance.
     *
     */
	public static ChoiceEvaluator getInstance() {
		return singleton;

	}


  /**
   * A child class can over load this method to load properties from another location.
   * @param key The property key
   * @param type The property type
   * @return Map A map of the properties
   * @throws ProcessingException if there is an error loading properties
   */
   protected Map loadProperties(String key, String type) throws ProcessingException {
     return (getProperties(key,type) );
   }

  /**
   * initializes this component and all MessageDirectors that can be called
   *
   * @param key The property key
   * @param type The property type
   */
  public void initialize(String key, String type) throws ProcessingException
  {

      if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "ChoiceEvaluator - starting up");

     properties =  loadProperties(key,type);



     for (int i=0; true; i++ ) {

        String mdClassProp = PersistentProperty.getPropNameIteration(DIRECTOR_CLASS_PROP, i);
        String mdClass = PropUtils.getPropertyValue(properties, mdClassProp);

        // break if controling property is not there
        if (mdClass == null) {
          break;
        }

        String mdKeyProp  = PersistentProperty.getPropNameIteration(DIRECTOR_KEY_PROP, i);
        String mdTypeProp = PersistentProperty.getPropNameIteration(DIRECTOR_TYPE_PROP, i);
        String mdNameProp = PersistentProperty.getPropNameIteration(DIRECTOR_NAME_PROP, i);

        StringBuffer errorBuf = new StringBuffer();

        String mdKey  = PropUtils.getRequiredPropertyValue(properties, mdKeyProp, errorBuf);
        String mdType = PropUtils.getRequiredPropertyValue(properties, mdTypeProp, errorBuf);
        String mdName = PropUtils.getRequiredPropertyValue(properties, mdNameProp, errorBuf);

        // if there are any errors at this point then the properties are incorrect.
        if (errorBuf.length() > 0 ) {
           Debug.log(Debug.ALL_ERRORS,"ChoiceEvaluator: " + errorBuf.toString());
           throw new ProcessingException(errorBuf.toString());
        }

        if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
            Debug.log(Debug.MSG_LIFECYCLE,"Adding choice " + mdName +
                     " of class [" + mdClass +"] with key[" + mdKey + "] and type[" + mdType + "]" );


        //create MessageDirector and initialize
        MessageDirector md = MessageDirectorFactory.create(mdClass,mdKey,mdType);

        NVPair pair = new NVPair(mdName, md);
        choices.add(pair);
     }
  }


  /**
   * calls the MessageDirector that can route this message.
   * @param header The request header.
   * @param message The request message.
   * @param reqType - indicates the type of request, refer to RouterConstants.
   * @see RouterConstants
   * @return Object the response from the spi
   * @throws ProcessingException if something goes wrong during processing
   * @throws MessageException if the xml is bad.
   */
  public final Object processRequest(String header, Object message, int reqType) throws ProcessingException,MessageException
  {
  
      XMLMessageParser headerParser = new XMLMessageParser(header);
      Document doc = headerParser.getDocument();
      MessageDirector md = findMessageDirector(doc,message);

      Object response = md.processRequest(doc,message, reqType);
      return(response);
  }

  /**
   * finds the message director that knows how to route this request
   *
   * @param header the header for this request.
   * @param message the message for this request.
   * @throws ProcessingException if there is no choice that can route this message.
   * @throws MessageException if there is bad xml.
   */
  private MessageDirector findMessageDirector(Document header, Object message) throws ProcessingException, MessageException
  {
      Iterator it =  choices.listIterator();


    while (it.hasNext() ) {
       NVPair pair = (NVPair)it.next();
       MessageDirector md = (MessageDirector) pair.value;
       boolean answer = md.canRoute(header, message);

       if (answer == true) {
           if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "Using MessageDirector " + pair.name + " to route message.");

           return md;
       }
    }
    XMLMessageGenerator gen = new XMLMessageGenerator(header);
    String error  = "Cannot route request. No routing choice was found to router header:" +
             gen.generate();

    Debug.log(Debug.ALL_ERRORS, error);
    throw new ProcessingException(error);

  }

  /**
   * calls clean up on all of the messagedirectors being used
   * @throws ProcessingException if something goes wrong
   */
  public void cleanup() throws ProcessingException{

      if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "ChoiceEvaluator - shutting down");
    
      Iterator it =  choices.listIterator();

      while (it.hasNext() ) 
      {
        NVPair pair = (NVPair)it.next();
        
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE, "Calling clean up on MessageDirector " + pair.name);
        
        ((MessageDirector)pair.value).cleanup();
    }
  }


  /**
    * get a map of the properties specificed by the key and type
    */
   private static Map getProperties ( String key, String type ) throws ProcessingException
    {
        PropertyChainUtil propChain = new PropertyChainUtil();
        Hashtable properties;

      
        try {

             properties = propChain.buildPropertyChains(key, type);
        }
        catch ( PropertyException pe ) {
            throw new ProcessingException( pe.getMessage() );
        }

        return properties;
    }

}
/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.messagedirector;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.router.messagedirector.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.router.*;

import com.nightfire.idl.*;

import org.w3c.dom.*;

/**
 * Base class for MessageDirectors. This class should be extended by new
 * Message Directors.
 * This class adds:
 *  alias support (each MessageDirector can have there own set of aliases for providers,etc)
 *  property mananagement methods
 *  single process method to handle both async and sync request
 *
 * @author Dan Diodati
 */
public abstract class MessageDirectorBase implements MessageDirector, RouterConstants
{
  private Map mdProperties = null;
  private Map aliases;

  protected boolean testMode = false;

  public static final String ALIAS_PROP_PREFIX = "ALIAS_";
  public static final String REALNAME_PROP_PREFIX = "REALNAME_";



  private static boolean initSender = false;

    
    // list of alternative orb locations for sub classes to use
  protected List orbSpaces = new LinkedList();

  protected HashMap conditionOrbMap = new HashMap();
  protected String ORBRoutingRequestType = null;


  protected final class OrbSpace {
     public String port;
     public String addr;
  }

  /**
   * sets up properties,aliases,etc. so child classes NEED to call super.intialize().
   * @param key The property key
   * @param type The property type
   * @throws ProcessingException if intialization fails
   */
  public void initialize(String key, String type) throws ProcessingException
  {
      
     if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "Initializing MessageDirector : "+ this.getClass().getName());

     mdProperties = getProperties(key,type);

     initializeAliases();

     // initialize the SPICaller only once

     synchronized (MessageDirector.class) {
        if (!initSender && !testMode) {
           initSender = true;

           SPICaller.getInstance().initialize();
        }
     }


     RouterSupervisor sup = (RouterSupervisor)RouterSupervisor.getSupervisor();

     for(int i = 0; true; i++ ) {
        OrbSpace space = new OrbSpace();
        space.addr = sup.getProperty(PersistentProperty.getPropNameIteration(RouterSupervisor.ALT_ORB_ADDR_PREFIX,i) );
        space.port = sup.getProperty(PersistentProperty.getPropNameIteration(RouterSupervisor.ALT_ORB_PORT_PREFIX,i) );
        String ORBRoutingCondition = sup.getProperty(PersistentProperty.getPropNameIteration(RouterSupervisor.ALT_ORB_CONDITION_PREFIX,i) );
        if ( !StringUtils.hasValue(space.addr) && !StringUtils.hasValue(space.port) )
           break;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, this.getClass().getName()+ ": Adding alternative orb : address[" + space.addr + "] and port[" + space.port + "]");
        
        orbSpaces.add(space);

        if (StringUtils.hasValue( ORBRoutingCondition))
        {
             if (Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log(Debug.MSG_STATUS, this.getClass().getName()+ ": Adding alternative orb routing condition: condition[" + ORBRoutingCondition + "] for address[" + space.addr + "] and port[" + space.port + "]");

             conditionOrbMap.put(ORBRoutingCondition, space);
        }
     }

      ORBRoutingRequestType = sup.getProperty(RouterSupervisor.ORB_ROUTING_REQUESTTYPE);

      if (Debug.isLevelEnabled(Debug.MSG_STATUS))
         Debug.log(Debug.MSG_STATUS, this.getClass().getName()+ ": Valid request-type values for alternate ORB Routing: [" + ORBRoutingRequestType + "]");
  }


  /**
   * adds any aliases that it finds into a local alias table.
   * each MessageDirector will have it's own set of aliases.
   */
  private void initializeAliases() //throws ProcessingException
  {
     aliases = new HashMap();
     String aliasProp;
     String alias;
     String realNameProp;
     String realName;


     NVPair[] sets = PersistentProperty.getPropertiesLike((Hashtable)mdProperties, ALIAS_PROP_PREFIX);

     for (int i=0; i < sets.length; i++ ) {
        aliasProp = sets[i].name;
        alias     = (String)sets[i].value;

        // get a realname property with the same name as the alias prop.
        realNameProp = REALNAME_PROP_PREFIX + aliasProp.substring(ALIAS_PROP_PREFIX.length());

        realName = getPropertyValue(realNameProp);

        if (!StringUtils.hasValue(realName) ) {
           Debug.log(Debug.ALL_WARNINGS,"Could not find matching realname property [" + realNameProp +
                     "] for alias property [" + aliasProp + "]");

          continue;
        }


        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS,"Adding alias [" + alias +
                     "] with real name [" + realName + "]");

        aliases.put(alias,realName);

     }
  }
  /**
   * Returns the real name of the alias if one is found.
   * other wise returns the alias as the real name
   * @param alias The alias to look up
   * @return The real name for the alias. Or returns the alias if a real name is not found.
   */
  public String getRealName(String alias)
  {
     if (!StringUtils.hasValue(alias) )
        return alias;

     String real = (String)aliases.get(alias);

     if (StringUtils.hasValue(real))
        return real;
     else
        return alias;

  }



  /**
   * The method which determines the name of the SPI to route the message to.
   * @param header the xml header for this request comming in.
   * @param message the message comming in (will be xml in all current cases)
   * @return RequestHandler - the RequestHandler representing the server.
   * @throws ProcessingException if there is a processing error.
   * @throws MessageException if there is a message error.
   *
   * <b>NOTE: MessageDirectors should ALWAYS use the RouterSupervisor to obtain
   * RequestHandlers because the RouterSupervisor can be tracking servers from
   * from multiple locations, and maintains running status on each server.</b>
   *
   */
  protected abstract RequestHandler getHandler(Document header, Object message) throws ProcessingException, MessageException;



   // calls the next router component
   // @param header the xml header for this request comming in.
   // @param message the message comming in (will be xml in all current cases)
   // @param reqType - indicates the type of request, refer to RouterConstants.
   // @see RouterConstants
   // @return Object - The response from sync request
   // @throws ProcessingException if there is a processing error.
   // @throws MessageException if there is a message error.
   //




  public final Object processRequest(Document header, Object message, int reqType) throws ProcessingException,MessageException
  {
     RequestHandler rh = getHandler(header,message);
     XMLMessageGenerator gen = new XMLMessageGenerator(header);
     Object response = SPICaller.getInstance().processRequest(rh, gen.generate(), message, reqType);
     return(response);
  }

  /**
   * This method cleans up resources used by the MessageDirector
   * if a child class overloads this method they NEED to call super.cleanup().
   */
  public void cleanup() throws ProcessingException
  {
     if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log(Debug.OBJECT_LIFECYCLE, "Shutting down MessageDirector : " + getClass().getName());

     // shutdown the SPICaller only once

     synchronized (MessageDirectorBase.class) {

        if (initSender && !testMode ) {
           initSender = false;
           SPICaller.getInstance().cleanup();
        }
     }

  }

   /**
   * Return the value for the property propName from the persistent properties
   * ( if it exists )
   *
   * @param propName The property whose value is to be returned
   *
   * @return String Value of propert name
   *
   * @exception ProcessingException Thrown if property does not exist
   */
  public String getRequiredPropertyValue ( String propName ) throws ProcessingException
  {
     try {
     String rtn = PropUtils.getRequiredPropertyValue(mdProperties, propName);
     return (rtn);
     } catch (FrameworkException fe) { throw new ProcessingException(fe); }

  }

  /**
   * Return the value for the property propName from the persistent properties
   * ( if it exists )
   *
   * @param propName The property whose value is to be returned
   *
   * @param errorMsg Container for any errors that occur during proessing. Error
   *  messages are appended to this container instead of throwing exceptions
   *
   * @return String Value of property name
   */
  public String getRequiredPropertyValue ( String propName, StringBuffer errorMsg )
  {
     return (PropUtils.getRequiredPropertyValue(mdProperties, propName,errorMsg) );
  }

  /** Return the value for the property propName from the persistent properties
   *
   * @param propName The property whose value is to be returned
   *
   * @return String Value of propName
   */
  public String getPropertyValue (String propName )
  {
     return( PropUtils.getPropertyValue(mdProperties, propName) );
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

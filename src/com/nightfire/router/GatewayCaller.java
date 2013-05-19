/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router;

import java.util.*;
import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;
import org.w3c.dom.*;
import org.omg.CORBA.*;


import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.corba.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;

/**
 * A generic corba caller class.
 * It makes a call to an SPI.
 * @author Dan Diodati
 */
public class GatewayCaller {
  private static CorbaPortabilityLayer cpl;

  private Map properties;

  public static final String ORB_AGENT_ADDR_PROPERTY         = "ORBagentAddr";
  public static final String ORB_AGENT_PORT_PROPERTY         = "ORBagentPort";

  /**
   * initializes the class
   * @param key The property key
   * @param type The property type
   * @throws ProcessingException if there is an error
   */
  public void initialize(String key,String type) throws ProcessingException
  {
       properties = getProperties(key,type);
  }

  /**
   * performs clean up.
   * @throws ProcessingException if there an error.
   */
  public void cleanup() throws ProcessingException
  {
     if (cpl != null)
        cpl.shutdown();
  }

     /**
      * Sends a request to a SPI
      * @param gatewayName The COS Name of the SPI to call
      * @param header The xml header
      * @param message The request message
      * @param reqType - indicates the type of request, refer to RouterConstants.
      * @see RouterConstants
      * @return The response for Sync requests otherwise null.
      * @throws ProcessingException if there is an error during processing
      * @throws MessageException if there is an error with the message.
      */
     public java.lang.Object sendRequest(String gatewayName, String header, String message, int reqType) throws ProcessingException, MessageException
     {
        RequestHandler gateway = null;

        try {

           ObjectLocator loc = new ObjectLocator(getORB());

           org.omg.CORBA.Object obj = loc.find(gatewayName);
           gateway = RequestHandlerHelper.narrow(obj);
        } catch (CorbaException ce) {
           throw new ProcessingException("Could not contact server " + gatewayName + " : "
                                                                    + ce.getMessage());
        } catch (ProcessingException re) {
           throw new ProcessingException("Could not get ORB " + " : " + re.getMessage() );
        }
        return(sendRequest(gateway,header,message, reqType) );

     }

      /**
      * Sends a request to a SPI
      * @param gateway The RequestHandler to an SPI to use.
      * @param header The xml header
      * @param message The request message
      * @param reqType - indicates the type of request, refer to RouterConstants.
      * @see RouterConstants
      * @return Object The response for Sync requests otherwise null.
      * @throws ProcessingException if there is an error during processing
      * @throws MessageException if there is an error with the message.
      */
     public java.lang.Object sendRequest(RequestHandler gateway, String header, String message, int reqType) throws ProcessingException, MessageException
     {


        if ( gateway == null ) {
           throw new ProcessingException("CORBA server object is not of type RequestHandler." );
        }

        try {
           if (reqType == RouterConstants.ASYNC_REQUEST) {
             gateway.processAsync(header, message);
             return null;
           } else if (reqType == RouterConstants.SYNC_REQUEST) {
              StringHolder holder = new StringHolder();
              gateway.processSync(header,message,holder);
               if ( !StringUtils.hasValue(holder.value) ) {
                  throw new ProcessingException("SPI returned a null response");
               }
               return (new MessageData(null, holder.value) );
           } else if (reqType == RouterConstants.SYNC_W_HEADER_REQUEST) {
              StringHolder msgHolder = new StringHolder();
              StringHolder headerHolder = new StringHolder();
              gateway.processSynchronous(header,message,headerHolder, msgHolder);
               if ( !StringUtils.hasValue(msgHolder.value) ) {
                  throw new ProcessingException("SPI returned a null response");
               }
               return (new MessageData(headerHolder.value, msgHolder.value) );
           } else
              throw new ProcessingException("Invalid request type specified");
        } catch( CorbaServerException e )  {
            throw new ProcessingException("Processing of request failed. "+ " : " + e.errorMessage );
        }  catch( InvalidDataException e )  {
            throw new MessageException(e.errorMessage );
        } catch( Exception e )  {
            throw new ProcessingException("Could not obtain reference to CORBA server object " + " : " + e.toString() );
        }


       

     }


    /**
     * Get the CORBA orb used to get a reference to the RequestHandler object.
     *  This method needs to have the properties set to create an ORB or else it throws
     * an exception. The child class is also expected to handle an needed synchronization.
     *
     * @return CORBA orb.
     *
     * @exception  ProcessingException  Thrown if orb can't be obtained.
     */
    protected ORB getORB ( )  throws ProcessingException
    {
        Properties props = null;

        if (cpl == null) {
          
              String addr = getProperty( ORB_AGENT_ADDR_PROPERTY );
              if ( addr != null )
             {
                  if ( props == null )
                      props = new Properties( );

                  if(Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log( Debug.IO_STATUS, ORB_AGENT_ADDR_PROPERTY +  " [" + addr + "]" );

                  props.put( ORB_AGENT_ADDR_PROPERTY, addr );
              }

              String port = getProperty( ORB_AGENT_PORT_PROPERTY );

              if ( port != null )
              {
                  if ( props == null )
                      props = new Properties( );

                  if(Debug.isLevelEnabled(Debug.IO_STATUS))
                    Debug.log( Debug.IO_STATUS, ORB_AGENT_PORT_PROPERTY +  " [" + port + "]" );

                 props.put( ORB_AGENT_PORT_PROPERTY, port );
              }


            // Get an orb that talks to the OSAgent indicated by the properties.

            try
            {
                cpl = new CorbaPortabilityLayer( null, props, null);
            }
            catch (Exception e)
            {
                throw new ProcessingException("ERROR: GatewayCaller.getORB(): Failed to create a CorbaPortabilityLayer object:\n" + e.getMessage());
            }
        }

        return( cpl.getORB() );
    }

    /**
     * returns a property
     *@param prop The prop to obtain.
     *@return The value or null
     */
    protected String getProperty(String prop)
    {
      return((String)properties.get(prop) );

    }

    /**
     * returns a required property
     *@param prop The prop to obtain.
     *@return The value
     *@throws PropertyException if the required property is null.
     */
    protected String getReqProperty(String prop) throws PropertyException
    {

       try {
          return PropUtils.getRequiredPropertyValue(properties,prop);
       } catch ( FrameworkException fe) {
          throw new PropertyException(fe);
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


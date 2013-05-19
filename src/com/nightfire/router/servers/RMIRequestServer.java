/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.servers;


import com.nightfire.framework.util.*;

import com.nightfire.router.ChoiceEvaluator;
import com.nightfire.router.RouterSupervisor;
import com.nightfire.router.RouterConstants;

import com.nightfire.router.util.PatternVisitor;
import com.nightfire.router.util.PatternExistenceVisitor;

import com.nightfire.comms.rmi.RMIServer;

import com.nightfire.rmi.*;

import com.nightfire.common.ProcessingException;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.monitor.ThreadMonitor;





/**
 * The RMI request server that handles incomming RMI requests to the router.
 * This process is the start of several processes in the router.
 * @author Dan Diodati
 */
public class RMIRequestServer extends RMIServer implements RouterConstants
{


    protected boolean testMode = false;

    /**
     * main constructor that gets called to initialize this server
     * @param key The properties key to initialize this class
     * @param type The properties type to initialize this class
     * @throws ProcessingException if there is an error creating the server
     */
  public RMIRequestServer(String key, String type)
      throws ProcessingException
  {
     super(key,type);

     String cekey = getRequiredPropertyValue(CHOICE_EVAL_KEY);
     String cetype = getRequiredPropertyValue(CHOICE_EVAL_TYPE);

      if (!testMode)
           ChoiceEvaluator.getInstance().initialize(cekey, cetype);

  }

   /**
     * test constructor that can be used for unit testing
     * @param testMode indicates if this is test mode or not
     * @param key The properties key to initialize this class
     * @param type The properties type to initialize this class
     * @throws ProcessingException if there is an error creating the server
     */
  protected RMIRequestServer(boolean testMode, String key, String type)
      throws ProcessingException
  {

     super(key,type);
     Debug.log(this,Debug.NORMAL_STATUS, "RMIRequestServer - starting up");
     testMode = true;
  }



      /**
     * Implements shutdown operation
     */
    public void shutdown() {

        super.shutdown();

        Debug.log(Debug.IO_STATUS, "RMIRequestServer: " +
                  "Shuting down");

        try {
           if (!testMode)
              ChoiceEvaluator.getInstance().cleanup();
        } catch (ProcessingException pe) {
           Debug.log(this,Debug.ALL_WARNINGS,"ERROR calling clean up on router components: " + pe.toString() );
           pe.printStackTrace();
        }

    }


     /**
     * Handles Asynchronous Processing
     *
     * @param  header Header in XML format
     *
     * @param  message The message which needs to be processed
     *
     * @exception RemoteException if there is no asyncronous response from server
     * @exception RMIServerException if request processing fails
     * @exception RMIInvalidDataException if it is a bad request
     */
    public void processAsync(String header, String message)
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {

        //Thread Monitoring
        ThreadMonitor.ThreadInfo tmti = null;

        try
        {
            header = CustomerContext.getInstance().propagate( header );

            logRequest(header,message);
            ThreadMonitor.start("Processing request with header[ "+header+" ]");
            ChoiceEvaluator.getInstance().processRequest(header, message, RouterConstants.ASYNC_REQUEST);
        }

        catch (MessageException e)
        {
             Debug.log(this, Debug.ALL_ERRORS, "ERROR: RMIRequestServer: invalid data error: " +
                                        e.getMessage());
            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError,
                e.getMessage());
        }
        catch (Exception e)
        {
            Debug.log(this, Debug.ALL_ERRORS, "ERROR: RMIRequestServer: processing error: " +
                                        e.getMessage());
            throw new RMIServerException (RMIServerException.UnknownError, e.getMessage());
        }
        finally
        {
            ThreadMonitor.stop(tmti);
        }
    }


    /**
     * Handles Synchronous Processing
     *
     * @param header Header in XML format
     *
     * @param message  The message which needs to be processed
     *
     * @param StringHolder  Can be assigned a value , acts as return parameter for corba interface methods
     *
     * @exception RemoteException if there is no syncronous response from server
     * @exception RMIServerException if request processing fails
     * @exception RMIInvalidDataException if it is a bad request
     */
    public String processSync(String header, String message)
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException
    {
        String result = null;

        //Thread Monitoring
        ThreadMonitor.ThreadInfo tmti = null;
        try
        {
            header = CustomerContext.getInstance().propagate( header );

            logRequest(header,message);
            ThreadMonitor.start("Processing request with header[ "+header+" ]");
            MessageData data = (MessageData) ChoiceEvaluator.getInstance().processRequest(header, message, RouterConstants.SYNC_REQUEST);
            result = data.body;
            logResponse(null, result);
        }

        catch (MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: RMIRequestServer: invalid data error: " +
                                        e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, e.toString());
        } catch (Exception e)
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: RMIRequestServer: processing error: " +
                                        e.getMessage());

            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        }
        finally
        {
            ThreadMonitor.stop(tmti);
        }

        return result;
    }

    /**
     * Method providing synchronous processing, with headers in and out.
     *
     * @param  header  Message header.
     * @param  request Message body.
     *
     * @return A response-pair object containing the response header and body.
     *
     * @exception RemoteException  Thrown on communications errors.
     * @exception RMIInvalidDataException  Thrown if request data is bad.
     * @exception RMIServerException  Thrown if server can't process request due to system errors.
     * @exception RMINullResultException  Thrown if server can't process request due to system errors.
     */
    public RMIRequestHandler.ResponsePair processSynchronous(String header, String message)
        throws java.rmi.RemoteException, RMIInvalidDataException, RMIServerException, RMINullResultException
    {

        RMIRequestHandler.ResponsePair result = new  RMIRequestHandler.ResponsePair();

        //Thread Monitoring
        ThreadMonitor.ThreadInfo tmti = null;
        try
        {
            header = CustomerContext.getInstance().propagate( header );
            logRequest(header,message);
            ThreadMonitor.start("Processing request with header[ "+header+" ]");
            
            MessageData data = (MessageData) ChoiceEvaluator.getInstance().processRequest(header, message, RouterConstants.SYNC_REQUEST);
            result.header = data.header;
            result.message = data.body;
            logResponse(result.header,result.message);
        }

        catch (MessageException e)
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: RMIRequestServer: invalid data error: " +
                                        e.getMessage());

            throw new RMIInvalidDataException(RMIInvalidDataException.UnknownDataError, e.toString());
        } catch (Exception e)
        {
            Debug.log(Debug.ALL_ERRORS, "ERROR: RMIRequestServer: processing error: " +
                                        e.getMessage());

            throw new RMIServerException(RMIServerException.UnknownError, e.toString());
        }
        finally
        {
            ThreadMonitor.stop(tmti);
        }

        return result;
    }


     /**
     * Tests whether a given particular usage (interface) is supported
     * by matching the Operation Type i.e, AddressValidation
     * @param UsageDescription UsageDescription sent by the client
     * @return true when usage is available
     */
    public boolean supportsUsage( RMIRequestHandler.UsageDescription usage ) {

      PatternExistenceVisitor pev = new PatternExistenceVisitor(usage.serviceProvider, usage.interfaceVersion,
                                                    usage.operationType, new Boolean(usage.asynchronous) );
      try {
        RouterSupervisor.getAvailableServers().traverseServerObjects(pev);
      } catch (ProcessingException e) {
         Debug.log(this,Debug.ALL_ERRORS, "ERROR traversing server objects : " + e.getMessage() );
         return false;
      }

      return pev.isFound();

    }

    /**
     *    Gets all the usages (Interfaces) that the router currently knows
     * @return an array of all usage descriptions.
     */
    public RMIRequestHandler.UsageDescription[] getUsageDescriptions() {
       com.nightfire.idl.RequestHandlerPackage.UsageDescription[] ud = null;

       PatternVisitor pev = new PatternVisitor(null, null, null, null);
      try {
        RouterSupervisor.getAvailableServers().traverseServerObjects(pev);
      } catch (ProcessingException e) {
         Debug.log(this,Debug.ALL_ERRORS, "ERROR traversing server objects : " + e.getMessage() );
         return null;
      }

      ud = pev.getUsageDescriptions();

      com.nightfire.rmi.RMIRequestHandler.UsageDescription[] convertedUsages = 
      			new com.nightfire.rmi.RMIRequestHandler.UsageDescription[ud.length];

      for (int i = 0; i < ud.length; i++) {
         convertedUsages[i] = new RMIRequestHandler.UsageDescription(ud[i].serviceProvider,
                                                                     ud[i].interfaceVersion,
                                                                     ud[i].OperationType,
                                                                     ud[i].asynchronous);
      }

      return convertedUsages;

    }

    private void logRequest(String header, String message) {
       if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE) ) {
         Debug.log(Debug.MSG_LIFECYCLE, "RMIRequestServer: Got a request: \n" +
         " **Header: \n" + header + "**Body: \n" + message + "\n");
       }
    }

    private void logResponse(String header, String message) {
       if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE) ) {
         Debug.log(Debug.MSG_LIFECYCLE, "RMIRequestServer: Got a response: \n" +
         "**Header: \n" + header + "**Body: \n" + message + "\n");
       }
    }


    /**
     * overloads method of ComServerBase. Although it is not used
     * because sub classes will not use the driver.
     */
    public boolean isAsync ( ) {
         return true;
    }


}

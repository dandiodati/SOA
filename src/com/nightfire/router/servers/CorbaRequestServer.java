/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.router.servers;


import com.nightfire.framework.util.*;


import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.monitor.ThreadMonitor;
import com.nightfire.framework.util.*;


import com.nightfire.comms.corba.*;
import com.nightfire.framework.corba.*;
import com.nightfire.idl.*;
import com.nightfire.idl.RequestHandlerPackage.*;

import com.nightfire.router.*;
import com.nightfire.router.util.*;
import com.nightfire.common.*;

/**
 * The corba request server that handles incomming corba requests to the router.
 * This process is the start of several processes in the router.
 * @author Dan Diodati
 */
public class CorbaRequestServer extends CorbaServer implements RouterConstants
{


    protected boolean testMode = false;

    /**
     * main constructor that gets called to initialize this server
     * @param key The properties key to initialize this class
     * @param type The properties type to initialize this class
     * @throws ProcessingException if there is an error creating the server
     */
  public CorbaRequestServer(String key, String type)
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
  protected CorbaRequestServer(boolean testMode, String key, String type)
      throws ProcessingException
  {

     super(key,type);
     
     if(Debug.isLevelEnabled(Debug.NORMAL_STATUS))
        Debug.log(Debug.NORMAL_STATUS, "CorbaRequestServer - starting up");
     
     testMode = true;
  }



      /**
     * Implements shutdown operation
     */
    public void shutdown() {

        super.shutdown();

        if(Debug.isLevelEnabled(Debug.IO_STATUS))
            Debug.log(Debug.IO_STATUS, "CorbaRequestServer: Shuting down");

        try {
           if (!testMode)
              ChoiceEvaluator.getInstance().cleanup();
        } catch (ProcessingException pe) {
           Debug.log(Debug.ALL_WARNINGS,"ERROR calling clean up on router components: " + pe.toString() );
        }

    }
     /**
     * Handles Asynchronous Processing
     *
     * @param  header header in XML format
     *
     * @param  message The message which needs to be processed
     *
     * @exception InvalidDataException if it is a bad request
     * @exception CorbaServerException if is a server side error occurs
     */

    public  void processAsync(String header, String message)
        throws InvalidDataException, CorbaServerException
    {

        if (Debug.isLevelEnabled(Debug.BENCHMARK) ) {
           Performance.startBenchmarkLog(Thread.currentThread().getName() +": Time to Route");
        }

        //Thread Monitoring
        ThreadMonitor.ThreadInfo tmti = null;

   	    try {
            header = CustomerContext.getInstance().propagate( header );

           logRequest(header, message);
           tmti = ThreadMonitor.start("Processing request with header ["+header+"]");
           ChoiceEvaluator.getInstance().processRequest(header, message, RouterConstants.ASYNC_REQUEST);

	      } catch (MessageException e) {
               Debug.log(Debug.ALL_ERRORS, "ERROR: CorbaRequestServer: invalid data error: " +
                                        e.getMessage() );
            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, " ", e.getMessage());
	      } catch (Exception e) {
            Debug.log(Debug.ALL_ERRORS, "ERROR: CorbaRequestServer: processing error: " +
                                        e.getMessage() );
            throw new CorbaServerException(CorbaServerExceptionType.CommunicationsError, e.getMessage());
	      }
          finally
          {
              ThreadMonitor.stop(tmti);
          }
          
        if (Debug.isLevelEnabled(Debug.BENCHMARK) ) {
           Performance.finishBenchmarkLog(Thread.currentThread().getName() +": Time to Route");
        }


    }

    /**
     * Handles Synchronous Processing
     *
     * @param header header in xml format contains the Carrier Information and Operation Type
     *
     * @param message  The message which needs to be processed
     *
     * @param StringHolder  Can be assigned a value , acts as return parameter for corba interface methods
     *
     * @exception InvalidDataException if it is a bad request
     * @exception CorbaServerException if a server side error occurs
     * @exception NullResultException if there is no syncronous response from server
     */
    public void processSync( String header,String message,
                             org.omg.CORBA.StringHolder response)
        throws InvalidDataException, CorbaServerException {
        
        if (Debug.isLevelEnabled(Debug.BENCHMARK) ) {
           Performance.startBenchmarkLog(Thread.currentThread().getName() +": Time to Route");
        }

        //Thread Monitoring
        ThreadMonitor.ThreadInfo tmti = null;

	      try {
            header = CustomerContext.getInstance().propagate( header );

           logRequest(header, message);
           tmti = ThreadMonitor.start("Processing request with header ["+header+"]");
           MessageData data = (MessageData)ChoiceEvaluator.getInstance().processRequest(header, message,RouterConstants.SYNC_REQUEST);
           response.value = data.body;
           logResponse(null, response.value);
	      }

	      catch (MessageException e) {
              Debug.log(Debug.ALL_ERRORS, "ERROR: CorbaRequestServer: invalid data error: " +
                                        e.getMessage() );
            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, " ", e.getMessage());
	      }
         catch (Exception e) {
             Debug.log(Debug.ALL_ERRORS, "ERROR: CorbaRequestServer: processing error: " +
                                        e.getMessage() );
            throw new CorbaServerException(CorbaServerExceptionType.CommunicationsError, e.getMessage());
	      }
        finally
        {
            ThreadMonitor.stop(tmti);
        }
        if (Debug.isLevelEnabled(Debug.BENCHMARK) ) {
           Performance.finishBenchmarkLog(Thread.currentThread().getName() +": Time to Route");
        }

    }

    /**
     * Handles Synchronous Processing with response header support
     *
     * @param requestHeader Request Header in XML format
     *
     * @param request  The message which needs to be processed
     *
     * @param responseHeader  Holds the response header that gets returned.
     * @param response       Holds the response message that gets returned.
     *
     * @exception InvalidDataException if it is a bad request
     * @exception CorbaServerException if a server side error occurs
     *
     */
    public void processSynchronous (String requestHeader,
                                    String request,
                                    org.omg.CORBA.StringHolder responseHeader,
                                    org.omg.CORBA.StringHolder response) 
        throws InvalidDataException, CorbaServerException
    {
        



        if (Debug.isLevelEnabled(Debug.BENCHMARK) ) {
           Performance.startBenchmarkLog(Thread.currentThread().getName() +": Time to Route");
        }
        //Thread Monitoring
        ThreadMonitor.ThreadInfo tmti = null;
	      try {

            requestHeader = CustomerContext.getInstance().propagate( requestHeader );

           logRequest(requestHeader, request);
           tmti = ThreadMonitor.start("Processing request with header ["+requestHeader+"]");
           MessageData data = (MessageData) ChoiceEvaluator.getInstance().processRequest(requestHeader, request,RouterConstants.SYNC_W_HEADER_REQUEST);


           //pass back empty string if header is ever null
           if (!StringUtils.hasValue(data.header) )
              responseHeader.value = "";
           else
              responseHeader.value = data.header;

           response.value = data.body;
           logResponse(responseHeader.value, response.value);

	      }

	      catch (MessageException e) {
              Debug.log(Debug.ALL_ERRORS, "ERROR: CorbaRequestServer: invalid data error: " +
                                        e.getMessage() );
            throw new InvalidDataException(InvalidDataExceptionType.UnknownDataError, " ", e.getMessage());
	      }
         catch (Exception e) {
             Debug.log(Debug.ALL_ERRORS, "ERROR: CorbaRequestServer: processing error: " +
                                        e.getMessage() );
            throw new CorbaServerException(CorbaServerExceptionType.CommunicationsError, e.getMessage());
	      }
        finally
        {
            ThreadMonitor.stop(tmti);
        }
        if (Debug.isLevelEnabled(Debug.BENCHMARK) ) {
           Performance.finishBenchmarkLog(Thread.currentThread().getName() +": Time to Route");
        }
    }


    private void logRequest(String header, String message) {
       if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE) ) {
         Debug.log(Debug.MSG_LIFECYCLE, "CorbaRequestServer: Got a request: \n" +
         " **Header: \n" + header + "**Body: \n" + message + "\n");
       }
    }

    private void logResponse(String header, String message) {
       if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE) ) {
         Debug.log(Debug.MSG_LIFECYCLE, "CorbaRequestServer: Got a response: \n" +
         "**Header: \n" + header + "**Body: \n" + message + "\n");
       }
    }



     /**
     * Tests whether a given particular usage (interface) is supported
     * by matching the Operation Type i.e, AddressValidation
     * @param UsageDescription UsageDescription sent by the client
     * @return true when usage is available
     */
    public boolean supportsUsage( UsageDescription usage ) {

      PatternExistenceVisitor pev = new PatternExistenceVisitor(usage.serviceProvider, usage.interfaceVersion,
                                                    usage.OperationType, new Boolean(usage.asynchronous) );
      try {
        RouterSupervisor.getAvailableServers().traverseServerObjects(pev);
      } catch (ProcessingException e) {
         Debug.log(Debug.ALL_ERRORS, "ERROR traversing server objects : " + e.getMessage() );
         return false;
      }

      return pev.isFound();

    }

    /**
     *    Gets all the usages (Interfaces) that the router currently knows
     * @return an array of all usage descriptions.
     */
    public UsageDescription[] getUsageDescriptions() {
       UsageDescription[] ud = null;
       PatternVisitor pev = new PatternVisitor(null, null, null, null);
      try {
        RouterSupervisor.getAvailableServers().traverseServerObjects(pev);
      } catch (ProcessingException e) {
         Debug.log(Debug.ALL_ERRORS, "ERROR traversing server objects : " + e.getMessage() );
         return null;
      }

      return pev.getUsageDescriptions();

    }


    /**
     * Returns the CorbaPortabilityLayer for use by the super class
     * @return CorbaPortabilityLayer
     */
    protected CorbaPortabilityLayer getCPL() 
    {
        return RouterSupervisor.getSupervisor().getCPL();
    }

    /**
     * overloads method of ComServerBase. Although it is not used
     * because sub classes will not use the driver.
     */
    public boolean isAsync ( ) {
         return true;
    }


}

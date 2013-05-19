/**
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 * @author: Srinivas Pakanati 05/2003
 * $Id: //adapter/R4.4/com/nightfire/adapter/messageprocessor/SystemCommandCaller.java#1 $
 */

package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.io.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.driver.*;

/**
 * Invokes system commands and sets the output result of the call at 
 * the specified location. If this processor is misconfigured, it can 
 * cause disastrous effects in the system.
 */
public class SystemCommandCaller extends MessageProcessorBase
{
    public static final String COMMAND_PROP = "COMMAND";

    public static final String COMMAND_ARGUMENT_ITER_PROP = "ARGUMENT";

    public static final String COMMAND_RESULT_LOC_PROP = "RESULT_LOCATION";

    private List arguments;

    private String resultLocation = null;

//    private String result = null;

    private String command = null;

    private boolean inTestMode = false;

    /**
     * Initializes this object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException
    {

        if ( !inTestMode ) 
            super.initialize ( key, type );

        if ( Debug.isLevelEnabled ( Debug.OBJECT_LIFECYCLE ) )
            Debug.log(Debug.OBJECT_LIFECYCLE, "SystemCommandCaller: Initializing.....");
          
        command = getRequiredPropertyValue ( COMMAND_PROP );

        arguments = new LinkedList();

        // Loop until all arguments have been extracted
        for ( int Ix = 0;  true;  Ix ++ )
        {
            
            String argument = getPropertyValue ( PersistentProperty.getPropNameIteration( COMMAND_ARGUMENT_ITER_PROP, Ix ) );


            // If we can't find an argument value, we are done.
            if ( !StringUtils.hasValue( argument ) )
                break;
            
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log(Debug.MSG_STATUS, "Adding argument [" + argument +"]" );

            arguments.add ( argument );
        }

        resultLocation = getPropertyValue ( COMMAND_RESULT_LOC_PROP );

    }


    /**
     * Invokes the system command and outputs the results of the command execution
     * to the specified location. This processor returns the input message unchanged.
     *
     * @param  messageObject  Input message object containing the message to process.
     *
     * @param  mpContext The context
     *
     * @return  Optional NVPair containing a Destination name and a Document,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject messageObject )
    throws MessageException, ProcessingException
    {
        InputStream inputStream = null;
        
        //The MessageProcessingDriver hands a null MessageObject 
        //for each processor at the end of each process, returning 
        //null on a null input signals that this is done processing 
        if ( messageObject == null )
        {
            return null;
        }
        
        Iterator iter = arguments.iterator( );
        
        //create the local variable which holds the value of the string buffer
        //during processing for each message and reset the string buffer every time process method executed the call
        //so that previous value of the argument is stripped off in the case of batched response
        StringBuffer commandWithArgs = new StringBuffer(command);
      
        while( iter.hasNext() )
        {
            //A space between each argument
            commandWithArgs.append ( " " );
            
            String argument = (String) iter.next();
            
            //Check if the argument is static value or
            //a location where the value can be found
            if ( argument.startsWith ( CONTEXT_START ) || 
                 argument.startsWith( MESSAGE_START ) )
            {
                if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                    Debug.log (Debug.MSG_STATUS, "Getting the argument from the location [" + argument +"]" );
                
                String argVal = (String) get( argument, mpContext, messageObject );

                if ( ! StringUtils.hasValue ( argVal ) )
                    throw new ProcessingException ("The location specified by [" + argument +"] doesnt have a value");
                
                commandWithArgs.append ( argVal );
            }
            else
                commandWithArgs.append ( argument );
        }
        

        String commandString = commandWithArgs.toString();
        
        //reset the string buffer once process method executed the call.
        commandWithArgs = null;

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log(Debug.MSG_STATUS, "SystemCommandCaller: Trying to invoke command [" + commandString +"]" );
        
        StringBuffer result = new StringBuffer();
        
        try 
        {
            //Execute the given command
            Process p = Runtime.getRuntime().exec( commandString );
                        
            //Wait until the process is successful
            p.waitFor();
            
            //Check the status of the the process
            int exitCode = p.exitValue();
            
            //Complain on a failed process call
            if ( exitCode != 0 )
            {
                InputStream errorStream = p.getErrorStream();
                StringBuffer error = new StringBuffer();
                while ( errorStream.available() > 0 )
                {
                    error.append( (char) errorStream.read() );
                }
                throw new ProcessingException ("The command [" + commandString +"] didn't succeed. Reason [" + error.toString() +"]");
            }
            //Parse the response from the command
            inputStream = p.getInputStream();
            
            while ( inputStream.available() > 0 )
            {
                result.append( (char)inputStream.read() );
            }

        }
        catch(Exception e) 
        {
            throw new ProcessingException ( e.getMessage() );
        }
        finally
        {
            //Close the input stream
            try
            {
                if ( inputStream !=null ) inputStream.close();
            }
            catch ( IOException ioe )
            {
                //Ignore
            }
        }

        //Set the result to the specified location
        if ( StringUtils.hasValue ( resultLocation ) && 
             StringUtils.hasValue ( result.toString() ) )
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log(Debug.MSG_STATUS, "Setting the result [" + result.toString().trim() +"] at the the locaton [" + resultLocation +"]" );
            
            set ( resultLocation, mpContext, messageObject, result.toString().trim() );
        }
        
        return formatNVPair ( messageObject );

     }//process

    /**
     * For Unit-Testing
     */
    public static void main ( String[] args )
    {
        Hashtable testProps = new Hashtable();
        testProps.put ( "LOG_LEVELS", "ALL");
        testProps.put ( "LOG_FILE", "console");
        testProps.put ( "COMMAND", "echo");
        testProps.put ( "ARGUMENT_0", "ARG1 ARG2");
        testProps.put ( "ARGUMENT_1", "ARG2 Again");
        testProps.put ( "RESULT_LOCATION", "@context.RESULT");
        
        SystemCommandCaller scc = new SystemCommandCaller();
        
        try
        {
            Debug.enableAll();
            
            Debug.configureFromProperties ( testProps );
            
            scc.inTestMode = true;
            
            scc.adapterProperties = testProps;
            
            scc.initialize ( " ", " " );
            
            MessageObject msgObject = new MessageObject ("UNIT TEST");

            MessageProcessorContext context = new MessageProcessorContext();
            
            scc.process( context, msgObject );

        }
        catch ( Exception e )
        {
            System.err.println( e.getMessage() ) ;
        }

    }
}






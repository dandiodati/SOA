/**
 * Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //adapter/R4.4/com/nightfire/adapter/messageprocessor/MultipleNodeExistenceChecker.java#1 $
 */

package com.nightfire.adapter.messageprocessor;

import java.util.*;
import java.io.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.db.*;
import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;

/**
 * Checks for the existence of a node in the given xml input message
 * and puts the node's value ( the value attribute's value )
 * or an externally defined output value into the context if it is found.
 * Also checks if the specified node is required and complains if not found
 */
public class MultipleNodeExistenceChecker extends MessageProcessorBase
{

    public static final String INPUT_LOCATION_PROP = "INPUT_LOCATION";

    public static final String INPUT_NODE_NAME_PROP = "INPUT_NODE_NAME";

    public static final String INPUT_EXPECTED_VALUE_PROP = "INPUT_EXPECTED_VALUE";

    public static final String OUTPUT_VALUE_PROP = "OUTPUT_VALUE";

    public static final String OUTPUT_LOC_PROP = "OUTPUT_LOCATION";

    public static final String REQUIRED_PROP = "REQUIRED";

    private List nodeList;

    /**
     * Constructor.
     */
    public MultipleNodeExistenceChecker ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating message-processor." );

        nodeList = new LinkedList( );
    }

    /**
     * Initializes this adapter with persistent properties
     *
     * @param  key  Property-key to use for locating initialization properties.
     *
     * @param  type Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization is not successful.
     */
    public void initialize( String key, String type ) throws ProcessingException
    {
        super.initialize(key, type);

        // Get configuration properties specific to this processor.
        Debug.log( Debug.SYSTEM_CONFIG, "MultipleNodeExistenceChecker: Initializing..." );

        StringBuffer errBuf = new StringBuffer();
        // Loop until all column configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String inputNode = getPropertyValue(
                PersistentProperty.getPropNameIteration(
                    INPUT_NODE_NAME_PROP, Ix ) );

            if ( !StringUtils.hasValue( inputNode ) )
                break;

            String inputLocation = getPropertyValue(
                PersistentProperty.getPropNameIteration(
                    INPUT_LOCATION_PROP, Ix ) );

            String inputExpectedValue = getPropertyValue(
                PersistentProperty.getPropNameIteration(
                    INPUT_EXPECTED_VALUE_PROP, Ix ) );

            String outputValue = getRequiredPropertyValue(
                PersistentProperty.getPropNameIteration(
                    OUTPUT_VALUE_PROP, Ix ), errBuf);

            String outputLocation = getRequiredPropertyValue(
                PersistentProperty.getPropNameIteration(
                    OUTPUT_LOC_PROP, Ix ), errBuf );

            if(errBuf.length() > 0){

                String errMsg = "Iteration " + Ix + errBuf.toString();
                Debug.error( errMsg );
                throw new ProcessingException( errMsg );
            }

            boolean isRequired = false;
            String tmp = getPropertyValue(
                PersistentProperty.getPropNameIteration(
                    REQUIRED_PROP, Ix ) );
            if( StringUtils.hasValue(tmp) )
            {
                try
                {
                    isRequired = getBoolean( tmp );
                }
                catch(MessageException me)
                {
                    throw new ProcessingException(me);
                }
            }

            try
            {
                // Create a new column data object and add it to the list.
                NodeData nd =
                    new NodeData( inputLocation, inputNode, inputExpectedValue,
                        outputValue, outputLocation, isRequired );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                    Debug.log( Debug.SYSTEM_CONFIG, nd.describe() );

                nodeList.add( nd );
            }
            catch ( Exception e )
            {
                throw new ProcessingException(
                    "Could not create Node Data description:\n" + e.toString() );
            }
        }

        if(Debug.isLevelEnabled( Debug.SYSTEM_CONFIG ))
            Debug.log( Debug.SYSTEM_CONFIG, "Number of Nodes to check: ["
                + nodeList.size() + "]." );

        Debug.log( Debug.SYSTEM_CONFIG,
            "MultipleNodeExistenceChecker: Initialization done." );
    }


    /**
     * Processes the input message and (optionally) return
     * a name / value pair.
     *
     * @param  context The Message Processor context
     *
     * @param  inputObject  Input message Object to process.
     *
     * @return  Optional NVPair containing a Destination name and a Document,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if the given input message is bad.
     */
    public NVPair[] process ( MessageProcessorContext context, MessageObject inputObject )
        throws MessageException, ProcessingException
    {
        //Standard behavior
        if ( inputObject == null )
            return null;

        Iterator iter = nodeList.iterator();
        while( iter.hasNext() )
        {
            NodeData aNode = (NodeData) iter.next();

            boolean isExpectedNodePresent = false;

            XMLMessageParser parser = new XMLMessageParser(
                getDOM( aNode.theInputLocation, context, inputObject ) );

            if( parser.exists( aNode.theNodeName ) )
            {
                if( !StringUtils.hasValue( aNode.theExpectedValue ) )
                {
                    isExpectedNodePresent = true;
                }
                else
                {
                    String theActualValue = parser.getValue( aNode.theNodeName );
                    if( aNode.theExpectedValue.equals(theActualValue) )
                        isExpectedNodePresent = true;
                }
            }

            if(isExpectedNodePresent)
            {
                if( Debug.isLevelEnabled( Debug.MSG_LIFECYCLE ) )
                    Debug.log(Debug.MSG_LIFECYCLE,
                        "Setting value [" + aNode.theOutputValue +
                            "] at location [" + aNode.theOutputLocation + "]");
                // set the output value on the context
                set( aNode.theOutputLocation, context, inputObject, aNode.theOutputValue );
            }
            else
            {
                //Make sure the node isn't one of those required ones
                if ( aNode.isRequired == true )
                {
                    throw new MessageException("Required Node [" +
                        aNode.theNodeName + "] not found in the given input ");
                }

                // set the output value on the context
                if( Debug.isLevelEnabled( Debug.MSG_LIFECYCLE ) )
                    Debug.log(Debug.MSG_LIFECYCLE,
                        "Node [" + aNode.theNodeName +
                            "] not found or didn't match expected value. " +
                                "Not required, so skipping");
            }
        }

        return formatNVPair(inputObject);
    }

    /**
     * Class NodeData is used to encapsulate a description of a single node
     * and its associated value.
     */
    private static class NodeData
    {
        public final String  theInputLocation;
        public final String  theNodeName;
        public final String  theExpectedValue;
        public final String  theOutputValue;
        public final String  theOutputLocation;
        public final boolean isRequired;

        public NodeData ( String theInputLocation, String theNodeName,
            String theExpectedValue, String theOutputValue,
                String theOutputLocation, boolean isRequired )
                    throws FrameworkException
        {
            this.theInputLocation   = theInputLocation;
            this.theNodeName        = theNodeName;
            this.theExpectedValue   = theExpectedValue;
            this.theOutputValue     = theOutputValue;
            this.theOutputLocation  = theOutputLocation;
            this.isRequired         = isRequired;
        }


        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Node description: " );

            if ( StringUtils.hasValue( theInputLocation ) )
            {
                sb.append( "Input Location [" );
                sb.append( theInputLocation );
            }

            if ( StringUtils.hasValue( theNodeName ) )
            {
                sb.append( "], Node name [" );
                sb.append( theNodeName );
            }

            if ( StringUtils.hasValue( theExpectedValue ) )
            {
                sb.append( "], Expected Value [" );
                sb.append( theExpectedValue );
            }

            if ( StringUtils.hasValue( theOutputValue ) )
            {
                sb.append( "], Output Value [" );
                sb.append( theOutputValue );
            }

            if ( StringUtils.hasValue( theOutputLocation ) )
            {
                sb.append( "], Output Location [" );
                sb.append( theOutputLocation );
            }

            sb.append( "], Is Required [" );
            sb.append( isRequired );

            sb.append( "]." );
            
            return( sb.toString() );
        }
    }
}


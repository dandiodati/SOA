/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //gateway/main/com/nightfire/spi/common/driver/MessageProcessingDriver.java#13 $
 */

package com.nightfire.spi.common.driver;

import java.util.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.spi.common.communications.*;


/**
 * Utility class used to keep track of data flow through message processors.
 */
class MessageProcessorTree 
{
    /**
     * Create a message-processor tree node.
     *
     * @param  processor  The message-processor for this tree node.
     */
    public MessageProcessorTree ( Map allProcessors, MessageProcessor processor )
    {
        this.allProcessors = allProcessors;

        myProcessor = processor;
    }
    
    
    /**
     * Get the name of the message-processor associated with this tree node.
     *
     * @return  The name of the message-processor associated with this tree node.
     */
    public final String getProcessorName ( )
    {
        return( myProcessor.getName() );
    }

    
    /**
     * Get the message-processor associated with this tree node.
     *
     * @return  The message-processor associated with this tree node.
     */
    public final MessageProcessor getProcessor ( )
    {
        return myProcessor;
    }

    
    /**
     * Attach tree nodes for the message-processors indicated by the outputs
     * as children of the current tree node.
     *
     * @param  outputs  An array of outputs returned by executing process()
     *                  against the parent message-processor.
     */
    public final void attach ( NVPair[] outputs )
    {
        MessageProcessorTree mpt = null;
        
        // Loop over the outputs returned by the message-processor, making sure that each
        // one is present in the parent tree node's child tree node list.
        for ( int Ix = 0;  Ix < outputs.length;  Ix ++ )
        {
            // Skip any null items.
            if ( (outputs[Ix] == null) || (outputs[Ix].name == null) )
                continue;
            
            // Skip any "NOBODY" items.
            if ( outputs[Ix].name.equals( MessageProcessorBase.NO_NEXT_PROCESSOR ) )
                continue;

            // See if the given node is already in the child list.
            boolean foundIt = false;
            
            int numChildren = children.size( );
            
            // If the current node has any child nodes associated with it ...
            if ( numChildren > 0 )
            {
                // As a performance optimization, we only use an iterator to
                // access the children if there is more than one child.
                if ( numChildren == 1 )
                {
                    mpt = (MessageProcessorTree)children.getFirst( );
                    
                    if ( mpt.getProcessorName().equals( outputs[Ix].name ) )
                    {
                        // If processor is 'done', set it back to 'active' state.
                        mpt.reset( );
                        
                        foundIt = true;
                    }
                }
                else  // Multiple children, so use iterator.
                {
                    ListIterator iter = children.listIterator( );
                    
                    while ( iter.hasNext() )
                    {
                        mpt = (MessageProcessorTree)iter.next( );
                        
                        if ( mpt.getProcessorName().equals( outputs[Ix].name ) )
                        {
                                // If processor is 'done', set it back to 'active' state.
                            mpt.reset( );
                            
                            foundIt = true;
                            
                            break;
                        }
                    }  // End: While children are available ...
                } // End: If there is one child ...
            } // End: If there are any children ...
            
            // Didn't find child, so add it.
            if ( !foundIt )
            {
                mpt = (MessageProcessorTree)allProcessors.get( outputs[Ix].name );

                if ( mpt != null )
                    children.add( mpt );
                else
                {
                    // If the name isn't the 'pseudo' message-processor "COMM_SERVER", 
                    // warn the user that no such message-processor exists.
                    if ( !MessageProcessingDriver.TO_COMM_SERVER.equals( outputs[Ix].name ) )
                    {
                        Debug.log( Debug.ALL_WARNINGS, "WARNING: Can't find message-processor with name [" 
                                   + outputs[Ix].name + "]." );
                    }
                }
            }
        } // End: Loop over outputs ...
    }
    
    
    /**
     * Attach tree nodes for the message-processors indicated by the outputs
     * as children of the tree node indicated by the input.
     *
     * @param  input    An input indicated a named parent message-processor.
     * @param  outputs  An array of outputs returned by executing process()
     *                  against the parent message-processor.
     *
     * @return  'true' if the parent was located, otherwise 'false'.
     */
    public final boolean attach ( NVPair input, NVPair[] outputs )
    {
        MessageProcessorTree mpt = null;

        // If the current tree node is associated with the message-processor named in the input ...
        if ( getProcessorName().equals(input.name) )
        {
            // Attach all outputs as children of this node.
            attach( outputs );

            // The current tree node has captured the parent-child relationship between the
            // input and outputs.
            return true;
        }
        

        // Current node is not the parent, so call attach() method recursively on children
        // to see if one of them (or their ancestors) is the parent.
        int numChildren = children.size( );

        if ( numChildren > 0 )
        {
            // As a performance optimization, we only use an iterator to
            // access the children if there is more than one child.
            if ( numChildren == 1 )
            {
                mpt = (MessageProcessorTree)children.getFirst( );

                // If the child handled the relationship, we're done.
                if ( mpt.attach( input, outputs ) )
                    return true;
            }
            else
            {
                ListIterator iter = children.listIterator( );
                
                while ( iter.hasNext() )
                {
                    mpt = (MessageProcessorTree)iter.next( );
                    
                    // If the current child handled the relationship, we're done.
                    if ( mpt.attach( input, outputs ) )
                        return true;
                }
            }
        }

        // Couldn't locate parent in this branch.
        return false;
    }
    
    
    /**
     * Get a candidate message-processor for null-input processing.
     *
     * @return  The next available message-processor tree node that isn't already done, 
     *          or null if none are available.
     */
    public final MessageProcessorTree getCandidate ( ) 
    {
        // Don't bother going any further if this node and all of its children are done.
        if ( isDone() )
            return null;

        // If the message-processor associated with the current tree 
        // node isn't done, return it.
        if ( !meDone )
            return this;
        
        // The processor associated with current node is done processing, 
        // so, if child processors aren't already done, see if they have 
        // anything to return.
        MessageProcessorTree candidate = null;
        
        int numChildren = children.size( );
        
        // If there are any children ...
        if ( numChildren > 0 )
        {
            // As a performance optimization, we only use an iterator to
            // access the children if there is more than one child.
            if ( numChildren == 1 )
            {
                candidate = (MessageProcessorTree)children.getFirst( );
                
                // If child node indicates that it's not done, return it.
                candidate = candidate.getCandidate( );
                
                // If the child produced a candidate, return it.
                if ( candidate != null )
                    return candidate;
            }
            else  // Multiple children, so use iterator.
            {
                ListIterator iter = children.listIterator( );
                
                while ( iter.hasNext() )
                {
                    candidate = (MessageProcessorTree)iter.next( );
                    
                    // If child node indicates that it's not done, return it.
                    candidate = candidate.getCandidate( );
                    
                    // If the child produced a candidate, return it.
                    if ( candidate != null )
                        return candidate;
                } // End: While there are children ...
            } // End: If there is one child ...
        } // End: If there are any children ...
        
        // Child nodes are done as well, so the entire branch rooted 
        // at this node's message processor is done.
        childrenDone = true;
        
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Message-processor [" + getProcessorName() 
                       + "] and all of its children are done processing." );
        
        // Nothing to process.
        return null;
    }
    
    
    /**
     * Indicate that the message-processor associated with the current tree node is done.
     */
    public final void setDone ( )
    {
        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "Message-processor [" 
                       + getProcessorName() + "] is done processing." );

        meDone = true;
    }
    

    /**
     * Get a string describing the data flow tree in human-readable form.
     *
     * @return  String description of data flow tree.
     */
    public final String describe ( )
    {
        StringBuffer sb = new StringBuffer( );
        
        describe( sb, 0 );
        
        return( sb.toString() );
    }
    
    
    /**
     * Check done/not-done status of the current tree branch
     *
     * @return  'true' if current node and all of its children are done, otherwise 'false'.
     */
    public final boolean isDone ( )
    {
        return( meDone && childrenDone );
    }
    

    /**
     * Reset the current message-processor back to 'active'.
     */
    protected final void reset ( )
    {
        if ( meDone == true )
        {
            if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
                Debug.log( Debug.MSG_STATUS, "Resetting message-processor [" 
                           + getProcessorName() + "]." );
        }

        meDone = false;

        childrenDone = false;
    }
    

    /**
     * Get a string describing the data flow tree in human-readable form.
     *
     * @param  sb  StringBuffer to place description in.
     * @param  treeDepth  Depth of node in tree.
     *
     * @return  String description of data flow tree.
     */
    private final void describe ( StringBuffer sb, int treeDepth )
    {
        for ( int Ix = 0;  Ix < treeDepth - 1;  Ix ++ )
            sb.append( INDENT_DESCENDANT );

        if ( treeDepth > 0 )
            sb.append( INDENT_CHILD );

        sb.append( "Name [" );
        sb.append( myProcessor.getName() );
        sb.append( "], type [" );
        sb.append( MessageProcessingDriver.getRelativeClassName( myProcessor ) );
        sb.append( "], done? [" );
        sb.append( isDone() );
        sb.append( "]\n" );
        
        ListIterator iter = children.listIterator( );
        
        while ( iter.hasNext() )
        {
            MessageProcessorTree mpt = (MessageProcessorTree)iter.next( );
            
            mpt.describe( sb, treeDepth + 1 );
        }
    }
    
    
    private static final String INDENT_DESCENDANT = "|  ";

    private static final String INDENT_CHILD = "|-->";
    
    private Map allProcessors;

    private MessageProcessor myProcessor;
    
    private LinkedList children = new LinkedList( );
    
    private boolean meDone = false;

    private boolean childrenDone = false;
}

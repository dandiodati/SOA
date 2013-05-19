package com.nightfire.framework.rules;

import com.nightfire.framework.util.*;


/**
*
* Copyright (c) 2001 Nightfire Software, Inc. All rights reserved.
*
* This is an Exception that keeps a reference to the source EvaluatorDefinition
* (a Rule or RuleSet) that was responsible for causing this Exception to
* be created.
*/
public class RuleException extends FrameworkException
{

    /**
    * Either a Rule or a RuleSet that was the root of this Exception.
    */
    private EvaluatorDefinition source = null;

    /**
     * Create a rule exception object with the given message.
     *
     * @param source The Rule or RuleSet associated with this Exception.
     * @param  msg  Error message associated with exception.
     */
    public RuleException(EvaluatorDefinition source, String msg)
    {
        super( msg );
        this.source = source;
    }

    /**
     * Create a rule exception object with the given exception's message.
     *
     * @param source The Rule or RuleSet associated with this Exception.     
     * @param  e  Exception object used in creation.
     */
    public RuleException (EvaluatorDefinition source, Exception e )
    {
        super( e );
        this.source = source;        
    }

    /**
    * Return the EvaluatorDefinition (a Rule or RuleSet) associated with this Exception.
    */
    public EvaluatorDefinition getSource(){

       return source;

    }
    
}

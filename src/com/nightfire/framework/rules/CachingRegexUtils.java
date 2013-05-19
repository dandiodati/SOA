/**
 *
 * Copyright (c) 2005 NeuStar, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.rules;

import java.util.*;
import java.util.regex.*;

import com.nightfire.framework.util.*;


/**
 * Provides methods for executing regular expression-related utilies (matching, replacement)
 * in a performance optimized way.  Underlying implementation utilizes java.util.regex functionality.
 */
public class CachingRegexUtils
{
    /**
     * Replaces every subsequence of the input sequence that matches the pattern with the given replacement string.
     *
     * @param  perl5Pattern  Pattern to use in search.
     * @param  input  Input data to perform replacements on.
     * @param  replacement  Value to use in replacement.
     *
     * @return The string constructed by replacing each matching subsequence by the replacement string, 
     *         substituting captured subsequences as needed
     */
    public static String replaceAll ( String perl5Pattern, String input, String replacement )
    {
        PatternMatcher pm = getPatternMatcher( perl5Pattern );

        Matcher m = pm.getMatcher( input );

        try
        {
            return( m.replaceAll( replacement ) );
        }
        finally
        {
            // Return matcher to it's parent pattern-specific cache.
            pm.putMatcher( m );
        }
    }


    /**
     * Attempts to match the entire input sequence against the pattern.
     *
     * @param  perl5Pattern  Pattern to use in search.
     * @param  input  Input data to test for match.
     *
     * @return 'true' if, and only if, the entire input sequence matches this matcher's pattern, otherwise 'false'.
     */
    public static boolean matches ( String perl5Pattern, String input )
    {
        PatternMatcher pm = getPatternMatcher( perl5Pattern );

        Matcher m = pm.getMatcher( input );

        try
        {
            return( m.matches() );
        }
        finally
        {
            // Return matcher to it's parent pattern-specific cache.
            pm.putMatcher( m );
        }
    }


    /**
     * Get the PatternMatcher object associated with the given regular expression.
     *
     * @param  regex  Pattern to use to identify pattern matcher.
     *
     * @return  The associated pattern matcher object.
     */
    private static synchronized PatternMatcher getPatternMatcher ( String regex )
    {
        // First, check to see if we've already previously created and cached one.
        PatternMatcher pm = (PatternMatcher)patternMatcherContainer.get( regex );

        // Haven't created one yet, so ...
        if ( pm == null )
        {
            // Create one now against the pattern ...
            pm = new PatternMatcher( regex );

            // and cache it for subsequent reuse, using regular expression as access key.
            patternMatcherContainer.put( regex, pm );
        }

        return pm;
    }


    /**
     * Private implementation class used to cache a Pattern together with
     * an internal cache of Matchers associated with it.  (NOTE: Pattern objects
     * are stateless and therefore inherently thread-safe, but Matcher objects 
     * maintain match state and therefore must be associated with only one execution 
     * thread at a time.  However, they can be reset and reused.)
     */
    private static class PatternMatcher
    {
        /**
         * Get a Matcher object for the current pattern and the given input.
         *
         * @param input  Input to get Matcher for.
         *
         * @return  A Matcher object.
         */
        public synchronized Matcher getMatcher ( String input )
        {
            // If we have one in the internal cache ...
            if ( matchersForPattern.size() > 0 )
            {
                // Remove it from the cache so that it's only accessible by
                // the current thread.
                Matcher m = (Matcher)matchersForPattern.removeFirst( );

                // Reset it against the new input before returning it.
                return( m.reset( input ) );
            }
            else
            {
                if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                    Debug.log( Debug.OBJECT_LIFECYCLE, "Creating matcher against input [" 
                               + input + "] for regex [" + pattern.pattern() + "]." );

                // Create a new matcher against the pattern.
                return( pattern.matcher( input ) );
            }
        }

        /**
         * Return the Matcher to the cache.
         *
         * @param m Matcher to return.
         */
        public synchronized void putMatcher ( Matcher m )
        {
            if ( m.pattern() != pattern )
            {
                throw new IllegalArgumentException( "Regex Matcher didn't originate from this Pattern." );
            }

            // Reset Matcher and return it to the internal cache.
            m.reset( );

            matchersForPattern.add( m );
        }

        /**
         * Create a pattern matcher object supporting the given regular expression.
         *
         * @param  regex  Perl5 regular expression to support.
         */
        public PatternMatcher ( String regex )
        {
            if ( Debug.isLevelEnabled( Debug.OBJECT_LIFECYCLE ) )
                Debug.log( Debug.OBJECT_LIFECYCLE, "Creating pattern-matcher for regex [" + regex + "]." );

            pattern = Pattern.compile( regex );

            matchersForPattern = new LinkedList( );
        }

        private Pattern pattern;
        private LinkedList matchersForPattern;
    }


    private static Map patternMatcherContainer = new HashMap( );
}

package com.nightfire.framework.test;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.iterator.*;
import com.nightfire.framework.message.iterator.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.mapper.*;
import com.nightfire.framework.db.*;


/*
 * Class for testing message parsing, generating and mapping.
 */
class XMLPerfTest
{
    public static void main ( String[] args )
    {
        if ( args.length < 2 )
        {
            System.out.println( "\n\nUSAGE: XMLPerfTest <xml-file> <num-iterations>\n\n" );

            return;
        }

        Runtime r = Runtime.getRuntime( );

        long start = 0;
        long stop  = 0;
        long total = 0;
        long free  = 0;
        
        try
        {
            String xmlText = FileUtils.readFile( args[0] );

            int iterCount = Integer.parseInt( args[1] );

            Document doc = null;

            long tally = 0;

            for ( int Ix = 0;  Ix < iterCount;  Ix ++ )
            {
                if ( Ix == 0 )
                    Debug.enableAll();
                else
                    Debug.disableAll();

                start = System.currentTimeMillis( );

                doc = parse( xmlText );
            
                stop = System.currentTimeMillis( );
            
                total = r.totalMemory( );
                free  = r.freeMemory( );
            
                System.out.println( "Parsing iteration [" + Ix + "] took [" + (stop - start) + 
                                    "] msec.  MEMORY: Total:[" + total + "], Free:[" + free + "]" );

                tally += (stop - start);
            }

            System.out.println( "Parsing average [" + (tally/iterCount) + "] msec." );


            tally = 0;

            for ( int Ix = 0;  Ix < iterCount;  Ix ++ )
            {
                if ( Ix == 0 )
                    Debug.enableAll();
                else
                    Debug.disableAll();

                start = System.currentTimeMillis( );

                String xml = generate( doc );

                stop = System.currentTimeMillis( );
            
                if ( Ix == 0 )
                    System.out.println( "Generated XML:\n" + xml );

                total = r.totalMemory( );
                free  = r.freeMemory( );
            
                System.out.println( "Generation iteration [" + Ix + "] took [" + (stop - start) + 
                                    "] msec. MEMORY: Total:[" + total + "], Free:[" + free + "]" );

                tally += (stop - start);
            }

            System.out.println( "Generation average [" + (tally/iterCount) + "] msec." );
        }
        catch ( Exception e )
        {
            System.err.println( e );

            e.printStackTrace( );
        }
    }


    private static Document parse ( String xml ) throws Exception
    {
        XMLMessageParser p = new XMLMessageParser( );

        p.parse( xml );

        return( p.getDocument() );
    }


    private static String generate ( Document xml ) throws Exception
    {
        XMLMessageGenerator g = new XMLMessageGenerator( "Test" );

        g.setDocument( xml );

        return( g.generate() );
    }
}


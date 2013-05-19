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
class XMLParseGen
{
    public static void main ( String[] args )
    {
        if ( args.length < 1 )
        {
            System.err.println( "\n\nUSAGE: XMLParseGen <xml-file-name> [<disable-validation>]\n\n" );
            System.exit( -1 );
        }

        Debug.showLevels();
        Debug.enableAll();
        // FrameworkException.showStackTrace( );

        if ( args.length > 1 )
            XMLMessageBase.enableValidation( false );

        try
        {
            System.out.println( "Reading XML file [" + args[0] + "]." );

            String xmlText = FileUtils.readFile( args[0] );

            System.out.println( "XML input [" + xmlText + "]." );

            XMLMessageParser p = new XMLMessageParser( );

            System.out.println( "Parsing ..." );

            p.parse( xmlText );

            p.log( );

            System.out.println( "Getting generator from parser ..." );

            XMLMessageGenerator g = (XMLMessageGenerator)p.getGenerator( );

            System.out.println( "Generating ..." );

            String s = g.generate( );

            System.out.println( "Generated XML:\n" + s );
        }
        catch ( FrameworkException fe )
        {
            System.err.println( fe.toString() );
        }
    }
}

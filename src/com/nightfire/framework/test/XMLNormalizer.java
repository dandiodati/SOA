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


/*
 * Class for 'normalizing' XML documents.
 */
class XMLNormalizer
{
    /**
     * Utility to normalize XML data.
     *
     * @param  args[0]  Name of file containing XML data to normalize.  Normalized
     *                  XML will be written to file whose name is the same as the
     *                  input file with ".out" appended to the name.
     */
    public static void main ( String[] args )
    {
        if ( args.length != 1 )
        {
            System.err.println( "\n\nUSAGE: XMLNormalizer <xml-file-name>\n\n" );
            System.exit( -1 );
        }

        String inFileName = args[0];

        String outFileName = new String( inFileName + ".out" );

        try
        {
            System.out.println( "Reading XML file [" + inFileName + "]." );

            String inData = FileUtils.readFile( inFileName );

            System.out.println( "XML input data:\n" + inData + "\n" );


            XMLMessageParser p = new XMLMessageParser( );

            System.out.println( "Parsing ..." );

            p.parse( inData );


            XMLMessageGenerator g = (XMLMessageGenerator)p.getGenerator( );

            System.out.println( "Generating ..." );

            String outData = g.generate( );

            System.out.println( "Generated XML data:\n" + outData + "\n" );

            System.out.println( "Writing output to file [" + outFileName + "] ..." );

            FileUtils.writeFile( outFileName, outData );

            System.out.println( "Done normalizing." );
        }
        catch ( FrameworkException fe )
        {
            System.err.println( fe.toString() );
        }
    }
}

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
class TestMessaging extends NodeVisitor
{
    private static final String ROOT_DIRECTORY = "../../../..";


    private static final int PERF_TIMER = 98;

    public static void main ( String[] args )
    {
        Debug.showLevels();
        Debug.enableAll();
        Debug.disableTimeStamping();
        FrameworkException.showStackTrace( );

        redirectLogs( "msg_test.out" );

        try
        {
            SetEnvironment.setSystemProperties( ROOT_DIRECTORY + "/com/nightfire/framework/test/test.properties", true );

            testFactories( );

            testTransformingFileCache( "msg_api_test.xml" );

            testIterator( "msg_api_test.xml" );

            testVisitor( "msg_api_test.xml" );

            testParse( "msg_api_test.xml" );

            testRemove( "msg_api_test.xml" );

            testCopy( "msg_api_test.xml" );

            testSubCopy( "msg_api_test.xml" );

            testGenerate( "specialaccess", "file:./msg_api_test.dtd" );

            testRootAttributeSet( "specialaccess", "file:./msg_api_test.dtd" );

            testASR( );

            testContext( );

            testAccessByPosition( );

            testMapper( );

            testMapperConfiguration( );

            testAttributeCopy( );

            testChildCopy( "msg_api_test.xml" );

            testSubtreeCopy( "msg_api_test.xml" );

            testGetChildNodes( "msg_api_test.xml" );

            testTextNodes( );

            testRepeatableParseGen( "msg_api_test.xml" );

            ObjectFactory.log( );
        }
        catch ( FrameworkException fe )
        {
            System.err.println( fe.toString() );
        }
    }


    /*************************************************************************/

    public TestMessaging ( Document d )   { super(d); }


    /*************************************************************************/

    private static void testFactories ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Factory test ..." );

        MessageGenerator gen = null;
        MessageParser p = null;
        MessageMapper mapper = null;

        // Try all creation methods.
        mapper = MessageMapperFactory.create( MessageMapperFactory.SIMPLE_MAPPER );
        mapper = MessageMapperFactory.create( "com.nightfire.framework.message.mapper.SimpleMessageMapper" );

        p = MessageParserFactory.create( Message.XML_TYPE );
        p = MessageParserFactory.create( "com.nightfire.framework.message.parser.xml.XMLMessageParser" );

        gen = MessageGeneratorFactory.create( Message.XML_TYPE, "specialaccess", "file:./msg_api_test.dtd" );
        gen = MessageGeneratorFactory.create( "com.nightfire.framework.message.generator.xml.XMLMessageGenerator", 
                                              "specialaccess", "file:./msg_api_test.dtd" );

        Debug.log( Debug.UNIT_TEST, "END: Factory test.\n\n" );
    }


    /*************************************************************************/

    private static void testTransformingFileCache ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Transforming file cache test ..." );

        FileCache fc = new FileCache( );

        fc.get( fileName );

        fc.logKeys( );

        fc.get( fileName );

        fc.logKeys( );

        
        fc = new FileCache( new XMLStringTransformer() );

        fc.getObject( fileName );

        fc.logKeys( );

        fc.getObject( fileName );

        fc.logKeys( );


        Debug.log( Debug.UNIT_TEST, "END: Transforming file cache.\n\n" );
    }


    /*************************************************************************/

    private static void testIterator ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Iterator test ..." );


        // Get the named file's contents.
        String xmlText = FileUtils.readFile( fileName );

        // ------- Create a parser to parse the XML document. ------- 
        MessageParser p = MessageParserFactory.create( Message.XML_TYPE );

        // ------- Give the parser the XML document to parse. ------- 
        p.parse( xmlText );

        // ------- Show that parse was successful. ------- 
        XMLMessageBase.logFullDescription( true );
        p.log( );
        XMLMessageBase.logFullDescription( false );

        Node intermediate = ((XMLMessageParser)p).getNode( "asr.asr_adminsection" );


        Performance.logMemoryUsage( Debug.UNIT_TEST, "Before iteration." );
        Performance.startTimer( PERF_TIMER, 0 );

        XMLNodeMessageIterator iter = null;
        
        // Create an iterator to visit all nodes in the message.
        iter = (XMLNodeMessageIterator)MessageIteratorFactory.create( MessageIteratorFactory.XML_NODE );
        
        // Give the iterator the parser's document to iterate over.
        iter.set( (XMLMessageParser)p );

        // While the iterator has nodes to give ...
        while ( iter.hasNext() )
        {
            // Get the node from the iterator and print it.
            NVPair nvp = iter.nextNVPair( );

            // Extract node type from the node.
            Node node = (Node)(nvp.value);
            int type = node.getNodeType( );

            // Print log message whose contents depend on type of node.
            if ( type == Node.TEXT_NODE )
            {
                Debug.log( Debug.UNIT_TEST, "ITERATOR returned [" + nvp.name + "] of type [" + 
                           type + "], text [" + node.getNodeValue() + "]" );
            }
            else
            {
                Debug.log( Debug.UNIT_TEST, "ITERATOR returned [" + nvp.name + "] of type [" + 
                           type + "]" );
            }
        }

        Performance.stopTimer( PERF_TIMER, 0, "Time iterate over XML document." );
        Performance.logMemoryUsage( Debug.UNIT_TEST, "After iteration." );


        Performance.logMemoryUsage( Debug.UNIT_TEST, "Before iteration." );
        Performance.startTimer( PERF_TIMER, 0 );

        // Create an iterator to visit all nodes in the message.
        iter = (XMLNodeMessageIterator)MessageIteratorFactory.create( MessageIteratorFactory.XML_NODE_VALUE );

        // Give the iterator the parser's document to iterate over.
        iter.set( (XMLMessageParser)p );
        
        // While the iterator has nodes to give ...
        while ( iter.hasNext() )
        {
            // Get the node from the iterator and print it.
            NVPair nvp = iter.nextNVPair( );
            
            Debug.log( Debug.UNIT_TEST, "ITERATOR returned node with name [" + nvp.name + "], value [" + 
                       (String)(nvp.value) + "]." );
        }
        
        Performance.stopTimer( PERF_TIMER, 0, "Time iterate over XML document." );
        Performance.logMemoryUsage( Debug.UNIT_TEST, "After iteration." );

        // ------- Now perform same iterations starting with an intermediate node in the document. -------

        // Create an iterator to visit all nodes in the message.
        iter = new XMLNodeMessageIterator( );
        
        // Give the iterator the parser's document to iterate over.
        iter.set( intermediate );

        // While the iterator has nodes to give ...
        while ( iter.hasNext() )
        {
            // Get the node from the iterator and print it.
            NVPair nvp = iter.nextNVPair( );

            // Extract node type from the node.
            Node node = (Node)(nvp.value);
            int type = node.getNodeType( );

            // Print log message whose contents depend on type of node.
            if ( type == Node.TEXT_NODE )
            {
                Debug.log( Debug.UNIT_TEST, "ITERATOR returned [" + nvp.name + "] of type [" + 
                           type + "], text [" + node.getNodeValue() + "]" );
            }
            else
            {
                Debug.log( Debug.UNIT_TEST, "ITERATOR returned [" + nvp.name + "] of type [" + 
                           type + "]" );
            }
        }


        // Create an iterator to visit all nodes in the message.
        iter = new XMLValueMessageIterator( );

        // Give the iterator the parser's document to iterate over.
        iter.set( intermediate );
        
        // While the iterator has nodes to give ...
        while ( iter.hasNext() )
        {
            // Get the node from the iterator and print it.
            NVPair nvp = iter.nextNVPair( );
            
            Debug.log( Debug.UNIT_TEST, "ITERATOR returned node with name [" + nvp.name + "], value [" + 
                       (String)(nvp.value) + "]." );
        }
        
        Performance.stopTimer( PERF_TIMER, 0, "Time iterate over XML document." );
        Performance.logMemoryUsage( Debug.UNIT_TEST, "After iteration." );
        
        
        Debug.log( Debug.UNIT_TEST, "END: Iterator test.\n\n" );
    }


    /*************************************************************************/

    private static void testVisitor ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Visitor test ..." );

        // Get the named file's contents.
        String xmlText = FileUtils.readFile( fileName );

        // ------- Create a parser to parse the XML document. ------- 
        MessageParser p = MessageParserFactory.create( Message.XML_TYPE );

        // ------- Give the parser the XML document to parse. ------- 
        p.parse( xmlText );

        // ------- Show that parse was successful. ------- 
        XMLMessageBase.logFullDescription( true );
        p.log( );
        XMLMessageBase.logFullDescription( false );


        TestMessaging visitor = new TestMessaging( ((XMLMessageParser)p).getDocument() );

        visitor.execute( );

        
        Debug.log( Debug.UNIT_TEST, "END: Visitor test.\n\n" );
    }


    /*************************************************************************/

    /**
     * Method applied against each node in the visitation iteration.
     *
     * @param  n  Current node in the iteration.
     *
     * @return  'true' if iteration should continue, otherwise 'false'.
     */
    public boolean visit ( Node n )
    {
        Debug.log( Debug.UNIT_TEST, "\tVisiting node [" + n.getNodeName() + "]" );

        return true;
    }
    
    
    /*************************************************************************/

    private static void testParse ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Parser test ..." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "Before parsing." );

        // Get the named file's contents.
        String xmlText = FileUtils.readFile( fileName );


        // ------- Create a parser to parse the XML document. ------- 
        MessageParser p = MessageParserFactory.create( Message.XML_TYPE );
        

        Performance.startTimer( PERF_TIMER, 0 );

        // ------- Give the parser the XML document to parse. ------- 
        p.parse( xmlText );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time parse XML document." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "After parsing." );


        // ------- Show that parse was successful. ------- 
        p.log( );


        System.out.println( "Attribute asr.asr_adminsection.BIC_TEL(type) exists? " 
                            + ((XMLMessageParser)p).attributeExists( "asr.asr_adminsection.BIC_TEL", "type" ) );

        System.out.println( "Attribute asr.asr_adminsection.BIC_TEL(value) exists? " 
                            + ((XMLMessageParser)p).attributeExists( "asr.asr_adminsection.BIC_TEL", "value" ) );

        System.out.println( "Attribute bandwidth(lotta) exists? " 
                            + ((XMLMessageParser)p).attributeExists( "bandwidth", "lotta" ) );

        String name = null;
        String value = null;
        int count = 0;

        Performance.startTimer( PERF_TIMER, 0 );

        // ------- Show that we can extract values from parser object. ------- 
        name = "asr";
        count = p.getChildCount( name );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection";
        count = p.getChildCount( name );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.CCNA";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.PON";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.ICSC";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.DTSENT.DATE";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.DTSENT.TIME";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.DDD";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.REQTYP";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.REQTYP.0";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.REQTYP.1";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.BAN";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.BIC_TEL.AREA";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.BIC_TEL.EXCH";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_adminsection.BIC_TEL.NUMBER";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_billsection.BILLNM";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.INIT";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.INIT_TEL_NO.AREA";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.INIT_TEL_NO.EXCH";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.INIT_TEL_NO.NUMBER";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.INIT_STREET";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.INIT_STATE";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.INIT_ZIP_CODE";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.DTREC.DATE";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        name = "asr.asr_contactsection.DTREC.DATE";
        value = ((XMLMessageParser)p).getOptionalValue( name );
        System.out.println( "Value of optional sub-component named [" + name + "] is [" + value + "]." );
        name = "asr.asr_contactsection.DTREC.DATE.Mommy";
        value = ((XMLMessageParser)p).getOptionalValue( name );
        System.out.println( "Value of optional sub-component named [" + name + "] is [" + value + "]." );


        name = "asr.asr_contactsection.DTREC.TIME";
        value = p.getValue( name );
        count = p.getChildCount( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]." );
        System.out.println( "Sub-component named [" + name + "] has [" + count + "] children." );

        Performance.stopTimer( PERF_TIMER, 0, "Time to extract values from  XML document." );

        name = "asr.asr_adminsection.PON";
        Node n = ((XMLMessageParser)p).getNode( name );
        System.out.println( "Attribute named value exists on node PON?: " + XMLMessageBase.attributeExists( n, "value" ) );
        System.out.println( "Attribute named foo exists on node PON?: " + XMLMessageBase.attributeExists( n, "foo" ) );

        name = "asr.asr_adminsection.CCNA";

        if ( p.exists( name ) )
            System.out.println( "Node [" + name + "] exists." );
        else
            System.out.println( "Node [" + name + "] doesn't exists." );

        name = "asr.asr_adminsection.CCNA.asdf";

        if ( p.exists( name ) )
            System.out.println( "Node [" + name + "] exists." );
        else
            System.out.println( "Node [" + name + "] doesn't exists." );


        // Get a generator from the parser and set some values.
        MessageGenerator gen = p.getGenerator( );

        gen.setValue( "asr.asr_adminsection.CCNA", "new-ccna" );
        gen.setValue( "asr.asr_adminsection.PON", "new-pon" );

        gen.log( );

        p.log( );

        Debug.log( Debug.UNIT_TEST, "Creating generator directly from document object." );

        gen = new XMLMessageGenerator( ((XMLMessageParser)p).getDocument() );

        gen.log( );

        Debug.log( Debug.UNIT_TEST, gen.generate() );

        Debug.log( Debug.UNIT_TEST, "END: Parser test.\n\n" );
    }


    /*************************************************************************/

    private static void testRemove ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Remove test ..." );

        // Get the named file's contents.
        String xmlText = FileUtils.readFile( fileName );

        // ------- Create a parser to parse the XML document. ------- 
        XMLMessageParser p = new XMLMessageParser( xmlText );
        
        // ------- Show that parse was successful. ------- 
        p.log( );

        // Remove some items.
        p.removeNode( "asr.asr_adminsection.CCNA" );
        p.removeNode( "asr.asr_adminsection.BIC_TEL.NUMBER" );
        p.removeNode( "asr.asr_contactsection" );

        p.log( );

        // Get a generator from the parser and set some values.
        MessageGenerator gen = p.getGenerator( );

        Debug.log( Debug.UNIT_TEST, "Resulting XML:\n" + gen.generate() );

        Debug.log( Debug.UNIT_TEST, "END: Remove test.\n\n" );
    }


    /*************************************************************************/

    private static void testCopy ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Copy test ..." );


        String xmlText = FileUtils.readFile( fileName );

        XMLMessageParser p = new XMLMessageParser( xmlText );
        
        // Get a node to copy.
        Node n = p.getNode( "asr.asr_contactsection" );


        XMLMessageGenerator gen = new XMLMessageGenerator( "copy" );

        // Copy the node.
        Node n2 = XMLMessageBase.copyNode( gen.getDocument(), n );


        Debug.log( Debug.UNIT_TEST, "Source for copy operation:" );
        p.log( );

        Debug.log( Debug.UNIT_TEST, "Copied node:" );
        XMLMessageBase.log( n2 );

        Debug.log( Debug.UNIT_TEST, "Document source:" );
        gen.log( );

        Debug.log( Debug.UNIT_TEST, "Copied document:" );
        gen.getDocument().getDocumentElement().appendChild( n2 );
        gen.log( );


        Debug.log( Debug.UNIT_TEST, "END: Copy test.\n\n" );
    }


    /*************************************************************************/

    private static void testSubCopy ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Sub-Copy test ..." );


        String xmlText = FileUtils.readFile( fileName );

        XMLMessageParser p = new XMLMessageParser( xmlText );
        
        Debug.log( Debug.UNIT_TEST, "Source for copy operation:" );
        p.log( );

        String xml = p.getXMLStream( "asr.asr_contactsection" );
        Debug.log( Debug.UNIT_TEST, "XML stream generated from node:\n" + xml  );

        p.parse( xml );
        
        Debug.log( Debug.UNIT_TEST, "Parse for copied item:" );
        p.log( );

        Debug.log( Debug.UNIT_TEST, "END: Sub-Copy test.\n\n" );
    }


    /*************************************************************************/

    private static void testGenerate ( String docName, String dtdName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Generator test ..." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "Before generation." );

        // ------- Create a generator to build the XML document. ------- 
        MessageGenerator gen = MessageGeneratorFactory.create( Message.XML_TYPE, docName, dtdName );
        

        Performance.startTimer( PERF_TIMER, 0 );

        // ------- Populate values in XML document. ------- 
        gen.setValue( "asr.asr_adminsection.CCNA", "fubar" );
        gen.setValue( "asr.asr_adminsection.PON", "asf123454" );
        gen.setValue( "asr.asr_adminsection.ICSC", "PT02" );
        gen.setValue( "asr.asr_adminsection.DTSENT.DATE", "12-12-1998" );
        gen.setValue( "asr.asr_adminsection.DTSENT.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTSENT.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.DTSENT", "type", "datetime" );
        gen.setValue( "asr.asr_adminsection.DDD", "03-22-1996" );
        gen.setValue( "asr.asr_adminsection.REQTYP", "SA" );
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.REQTYP", "type", "complex" );
        gen.setValue( "asr.asr_adminsection.REQTYP.CHAR(0)", "S" );
        gen.setValue( "asr.asr_adminsection.REQTYP.CHAR(1)", "A" );
        gen.setValue( "asr.asr_adminsection.BAN", "1234987" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.AREA", "617" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.EXCH", "555" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.NUMBER", "1234" );
        // Set the 'type' attribute on node BIC_TEL.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.BIC_TEL", "type", "phoneno" );

        gen.setValue( "asr.asr_billsection.BILLNM", "Fred Bloggs" );

        gen.setValue( "asr.asr_contactsection.INIT", "Wade Boggs" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.AREA", "617" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.EXCH", "555" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.NUMBER", "1234" );
        // Set the 'type' attribute on node INIT_TEL_NO.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.INIT_TEL_NO", "type", "phoneno" );
        gen.setValue( "asr.asr_contactsection.INIT_STREET", "123 Heaven's Gate" );
        gen.setValue( "asr.asr_contactsection.INIT_STATE", "CA" );
        gen.setValue( "asr.asr_contactsection.INIT_ZIP_CODE", "97867" );
        gen.setValue( "asr.asr_contactsection.DTREC.DATE", "12-12-1998" );
        gen.setValue( "asr.asr_contactsection.DTREC.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTREC.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.DTREC", "type", "datetime" );
        gen.setValue( "sa.adminsection.CCNA", "fubar" );
        gen.setValue( "sa.adminsection.PON", "asf123454" );
        gen.create( "sa.sa_circuitdetail" );
        gen.create( "sa.sa_location" );

        Performance.stopTimer( PERF_TIMER, 0, "Time to populate ASR form." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "After generation." );

        // ------- Write 'pretty-print' version to log to show proper construction. ------- 
        gen.log( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to log ASR form." );

        // ------- Get XML document stream, suitable for transmission. ------- 
        String val = gen.generate( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to create XML document." );

        System.out.println( "XML Document:\n" + val );

        // Get a parser from the generator.
        MessageParser p = gen.getParser( );

        p.getValue( "asr.asr_adminsection.CCNA" );
        p.getValue( "asr.asr_adminsection.PON" );

        p.log( );

        Debug.log( Debug.UNIT_TEST, "Creating parser directly from document object." );

        p = new XMLMessageParser( ((XMLMessageGenerator)gen).getDocument() );

        p.log( );

        Document doc = ((XMLMessageGenerator)gen).getDocumentClone( );

        XMLMessageGenerator gen2 = new XMLMessageGenerator( "foo" );

        gen2.setDocument( doc );

        gen2.setValue( "asr.asr_adminsection.NEW_NODE", "succeeded" );

        Debug.log( Debug.UNIT_TEST, "Cloned XML document:\n" + gen2.generate() );

        Debug.log( Debug.UNIT_TEST, "Source for cloned XML document:\n" + gen.generate() );

        Debug.log( Debug.UNIT_TEST, "END: Generator test.\n\n" );
    }


    /*************************************************************************/

    private static void testRootAttributeSet ( String docName, String dtdName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Document root node attribute set test ..." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "Before generation." );

        // ------- Create a generator to build the XML document. ------- 
        MessageGenerator gen = MessageGeneratorFactory.create( Message.XML_TYPE, docName, dtdName );
        

        Performance.startTimer( PERF_TIMER, 0 );

        // ------- Populate values in XML document. ------- 
        gen.setValue( "asr.asr_adminsection.CCNA", "fubar" );
        gen.setValue( "asr.asr_adminsection.PON", "asf123454" );
        gen.setValue( "asr.asr_adminsection.ICSC", "PT02" );
        gen.setValue( "asr.asr_adminsection.DTSENT.DATE", "12-12-1998" );
        gen.setValue( "asr.asr_adminsection.DTSENT.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTSENT.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.DTSENT", "type", "datetime" );
        gen.setValue( "asr.asr_adminsection.DDD", "03-22-1996" );
        gen.setValue( "asr.asr_adminsection.REQTYP", "SA" );
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.REQTYP", "type", "complex" );
        gen.setValue( "asr.asr_adminsection.REQTYP.CHAR(0)", "S" );
        gen.setValue( "asr.asr_adminsection.REQTYP.CHAR(1)", "A" );
        gen.setValue( "asr.asr_adminsection.BAN", "1234987" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.AREA", "617" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.EXCH", "555" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.NUMBER", "1234" );
        // Set the 'type' attribute on node BIC_TEL.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.BIC_TEL", "type", "phoneno" );

        // Set the root value here.
        XMLLibraryPortabilityLayer.setRootAttribute(  ((XMLMessageGenerator)gen).getDocument(), "foo", "bar" );

        gen.setValue( "asr.asr_billsection.BILLNM", "Fred Bloggs" );

        gen.setValue( "asr.asr_contactsection.INIT", "Wade Boggs" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.AREA", "617" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.EXCH", "555" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.NUMBER", "1234" );
        // Set the 'type' attribute on node INIT_TEL_NO.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.INIT_TEL_NO", "type", "phoneno" );
        gen.setValue( "asr.asr_contactsection.INIT_STREET", "123 Heaven's Gate" );
        gen.setValue( "asr.asr_contactsection.INIT_STATE", "CA" );
        gen.setValue( "asr.asr_contactsection.INIT_ZIP_CODE", "97867" );
        gen.setValue( "asr.asr_contactsection.DTREC.DATE", "12-12-1998" );
        gen.setValue( "asr.asr_contactsection.DTREC.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTREC.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.DTREC", "type", "datetime" );
        gen.setValue( "sa.adminsection.CCNA", "fubar" );
        gen.setValue( "sa.adminsection.PON", "asf123454" );
        gen.create( "sa.sa_circuitdetail" );
        gen.create( "sa.sa_location" );

        Performance.stopTimer( PERF_TIMER, 0, "Time to populate ASR form." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "After generation." );

        // ------- Write 'pretty-print' version to log to show proper construction. ------- 
        gen.log( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to log ASR form." );

        // ------- Get XML document stream, suitable for transmission. ------- 
        String val = gen.generate( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to create XML document." );

        System.out.println( "XML Document:\n" + val );

        // Get a parser from the generator.
        MessageParser p = gen.getParser( );

        p.getValue( "asr.asr_adminsection.CCNA" );
        p.getValue( "asr.asr_adminsection.PON" );

        p.log( );

        Debug.log( Debug.UNIT_TEST, "END: Document root node attribute set test.\n\n" );
    }


    /*************************************************************************/

    private static void testASR ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: ASR generation and parsing test ..." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "Before parsing and generation." );

        // ------- Create an XML document generator object. ------- 
        MessageGenerator gen = MessageGeneratorFactory.create( Message.XML_TYPE, "specialaccess", "file:./msg_api_test.dtd" );
        // MessageGenerator gen = MessageGeneratorFactory.create( Message.XML_TYPE, "specialaccess" );

        Performance.startTimer( PERF_TIMER, 0 );

        // ------- Populate values in XML document. ------- 
        gen.setValue( "asr.asr_adminsection.CCNA", "fubar" );
        gen.setValue( "asr.asr_adminsection.PON", "asf123454" );
        gen.setValue( "asr.asr_adminsection.ICSC", "PT02" );
        gen.setValue( "asr.asr_adminsection.DTSENT.DATE", "12-12-1998" );
        gen.setValue( "asr.asr_adminsection.DTSENT.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTSENT.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.DTSENT", "type", "datetime" );
        gen.setValue( "asr.asr_adminsection.DDD", "03-22-1996" );
        gen.setValue( "asr.asr_adminsection.REQTYP", "SA" );
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.REQTYP", "type", "complex" );
        gen.setValue( "asr.asr_adminsection.REQTYP.CHAR(0)", "S" );
        gen.setValue( "asr.asr_adminsection.REQTYP.CHAR(1)", "A" );
        gen.setValue( "asr.asr_adminsection.BAN", "1234987" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.AREA", "617" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.EXCH", "555" );
        gen.setValue( "asr.asr_adminsection.BIC_TEL.NUMBER", "1234" );
        // Set the 'type' attribute on node BIC_TEL.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.BIC_TEL", "type", "phoneno" );

        gen.setValue( "asr.asr_billsection.BILLNM", "Fred Bloggs" );

        gen.setValue( "asr.asr_contactsection.INIT", "Wade Boggs" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.AREA", "617" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.EXCH", "555" );
        gen.setValue( "asr.asr_contactsection.INIT_TEL_NO.NUMBER", "1234" );
        // Set the 'type' attribute on node INIT_TEL_NO.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.INIT_TEL_NO", "type", "phoneno" );
        gen.setValue( "asr.asr_contactsection.INIT_STREET", "123 Heaven's Gate" );
        gen.setValue( "asr.asr_contactsection.INIT_STATE", "CA" );
        gen.setValue( "asr.asr_contactsection.INIT_ZIP_CODE", "97867" );
        gen.setValue( "asr.asr_contactsection.DTREC.DATE", "12-12-1998" );
        gen.setValue( "asr.asr_contactsection.DTREC.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTREC.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.DTREC", "type", "datetime" );
        gen.setValue( "sa.adminsection.CCNA", "fubar" );
        gen.setValue( "sa.adminsection.PON", "asf123454" );
        gen.create( "sa.sa_circuitdetail" );
        gen.create( "sa.sa_location" );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to populate ASR form." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "After generation." );

        // ------- Write 'pretty-print' version to log to show proper construction. ------- 
        gen.log( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to log ASR form." );

        // ------- Get XML document stream, suitable for transmission. ------- 
        String val = gen.generate( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to create XML document." );

        System.out.println( "XML Document:\n" + val );


        // ------- Create an parser to parse the XML document. ------- 
        MessageParser p = MessageParserFactory.create( Message.XML_TYPE );
        
        Performance.startTimer( PERF_TIMER, 0 );

        // ------- Give the parser the XML document to parse. ------- 
        p.parse( val );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to parse XML document." );

        // ------- Show that parse was successful. ------- 
        p.log( );


        String name = null;
        String value = null;

        Performance.startTimer( PERF_TIMER, 0 );


        name = "asr.asr_adminsection.CCNA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.PON";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.ICSC";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.DTSENT.DATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.DTSENT.TIME";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.DDD";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.REQTYP";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.REQTYP.0";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.REQTYP.1";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.BAN";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.BIC_TEL.AREA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.BIC_TEL.EXCH";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_adminsection.BIC_TEL.NUMBER";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_billsection.BILLNM";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.INIT";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.INIT_TEL_NO.AREA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.INIT_TEL_NO.EXCH";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.INIT_TEL_NO.NUMBER";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.INIT_STREET";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.INIT_STATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.INIT_ZIP_CODE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.DTREC.DATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "asr.asr_contactsection.DTREC.TIME";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        Performance.stopTimer( PERF_TIMER, 0, "Time to extract values from  XML document." );

        Performance.logMemoryUsage( Debug.UNIT_TEST, "After parsing and value extraction." );

        Debug.log( Debug.UNIT_TEST, "END: ASR generation and parsing test.\n\n" );
    }


    /*************************************************************************/

    private static void testContext ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Context-based generation and parsing test ..." );

        // ------- Create an XML document generator object. ------- 
        MessageGenerator gen = MessageGeneratorFactory.create( Message.XML_TYPE, "specialaccess" );

        Performance.startTimer( PERF_TIMER, 0 );

        MessageContext genCtx = null;

        // ------- Populate values in XML document. ------- 
        gen.setValue( "asr.asr_adminsection.CCNA", "fubar" );
        genCtx = gen.getContext( "asr.asr_adminsection" );
        gen.setValue( genCtx, "PON", "asf123454" );
        gen.setValue( genCtx, "ICSC", "PT02" );
        gen.setValue( genCtx, "DTSENT.DATE", "12-12-1998" );
        gen.setValue( genCtx, "DTSENT.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTSENT.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.DTSENT", "type", "datetime" );
        gen.setValue( genCtx, "DDD", "03-22-1996" );
        gen.setValue( genCtx, "REQTYP", "SA" );
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.REQTYP", "type", "complex" );
        gen.setValue( genCtx, "REQTYP.CHAR(0)", "S" );
        gen.setValue( genCtx, "REQTYP.CHAR(1)", "A" );
        gen.setValue( genCtx, "BAN", "1234987" );
        gen.setValue( genCtx, "BIC_TEL.AREA", "617" );
        gen.setValue( genCtx, "BIC_TEL.EXCH", "555" );
        gen.setValue( genCtx, "BIC_TEL.NUMBER", "1234" );
        // Set the 'type' attribute on node BIC_TEL.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_adminsection.BIC_TEL", "type", "phoneno" );

        gen.setValue( "asr.asr_billsection.BILLNM", "Fred Bloggs" );

        gen.setValue( "asr.asr_contactsection.INIT", "Wade Boggs" );
        genCtx = gen.getContext( "asr.asr_contactsection" );
        gen.setValue( genCtx, "INIT_TEL_NO.AREA", "617" );
        gen.setValue( genCtx, "INIT_TEL_NO.EXCH", "555" );
        gen.setValue( genCtx, "INIT_TEL_NO.NUMBER", "1234" );
        // Set the 'type' attribute on node INIT_TEL_NO.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.INIT_TEL_NO", "type", "phoneno" );
        gen.setValue( genCtx, "INIT_STREET", "123 Heaven's Gate" );
        gen.setValue( genCtx, "INIT_STATE", "CA" );
        gen.setValue( genCtx, "INIT_ZIP_CODE", "97867" );
        gen.create( genCtx, "DTREC.DATE" );
        gen.create( genCtx, "DTREC.TIME" );
        gen.setValue( genCtx, "DTREC.DATE", "12-12-1998" );
        gen.setValue( genCtx, "DTREC.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTREC.
        ((XMLMessageGenerator)gen).setAttributeValue( "asr.asr_contactsection.DTREC", "type", "datetime" );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to populate ASR form." );

        // ------- Write 'pretty-print' version to log to show proper construction. ------- 
        gen.log( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to log ASR form." );

        // ------- Get XML document stream, suitable for transmission. ------- 
        String val = gen.generate( );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to create XML document." );

        System.out.println( "XML Document:\n" + val );


        // ------- Create an parser to parse the XML document. ------- 
        MessageParser p = MessageParserFactory.create( Message.XML_TYPE );
        
        Performance.startTimer( PERF_TIMER, 0 );

        // ------- Give the parser the XML document to parse. ------- 
        p.parse( val );
        
        Performance.stopTimer( PERF_TIMER, 0, "Time to parse XML document." );

        // ------- Show that parse was successful. ------- 
        p.log( );


        String name = null;
        String value = null;

        MessageContext parseCtx = p.getContext( "asr.asr_adminsection" );

        Performance.startTimer( PERF_TIMER, 0 );


        name = "CCNA";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "PON";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "ICSC";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "DTSENT.DATE";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "DTSENT.TIME";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "DDD";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "REQTYP";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "REQTYP.0";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "REQTYP.1";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "BAN";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "BIC_TEL.AREA";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "BIC_TEL.EXCH";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "BIC_TEL.NUMBER";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );


        parseCtx = p.getContext( "asr.asr_billsection" );

        name = "BILLNM";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );


        parseCtx = p.getContext( "asr.asr_contactsection" );

        name = "INIT";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "INIT_TEL_NO.AREA";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "INIT_TEL_NO.EXCH";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "INIT_TEL_NO.NUMBER";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "INIT_STREET";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "INIT_STATE";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "INIT_ZIP_CODE";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "DTREC.DATE";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "DTREC.TIME";
        value = p.getValue( parseCtx,  name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        Performance.stopTimer( PERF_TIMER, 0, "Time to extract values from  XML document." );


        parseCtx = p.getContext( "asr.asr_adminsection" );

        name = "CCNA";

        if ( p.exists( parseCtx, name ) )
            System.out.println( "Node [" + name + "] exists." );
        else
            System.out.println( "Node [" + name + "] doesn't exists." );


        name = "asdf";

        if ( p.exists( parseCtx, name ) )
            System.out.println( "Node [" + name + "] exists." );
        else
            System.out.println( "Node [" + name + "] doesn't exists." );


        Debug.log( Debug.UNIT_TEST, "END: Context-based generation and parsing test.\n\n" );
    }


    /*************************************************************************/

    private static void testAccessByPosition ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Access-by-position test ..." );


        MessageGenerator gen = MessageGeneratorFactory.create( Message.XML_TYPE, "two_ASRs" );


        // ------- Populate first ASR. ------- 
        gen.setValue( "0.asr_adminsection.CCNA", "fubar" );
        gen.setValue( "0.asr_adminsection.PON", "asf123454" );
        gen.setValue( "0.asr_adminsection.ICSC", "PT02" );
        gen.setValue( "0.asr_adminsection.DTSENT.DATE", "12-12-1998" );
        gen.setValue( "0.asr_adminsection.DTSENT.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTSENT.
        ((XMLMessageGenerator)gen).setAttributeValue( "0.asr_adminsection.DTSENT", "type", "datetime" );
        gen.setValue( "0.asr_adminsection.DDD", "03-22-1996" );
        gen.setValue( "0.asr_adminsection.REQTYP", "SA" );
        ((XMLMessageGenerator)gen).setAttributeValue( "0.asr_adminsection.REQTYP", "type", "complex" );
        gen.setValue( "0.asr_adminsection.REQTYP.CHAR(0)", "S" );
        gen.setValue( "0.asr_adminsection.REQTYP.CHAR(1)", "A" );
        gen.setValue( "0.asr_adminsection.BAN", "1234987" );
        gen.setValue( "0.asr_adminsection.BIC_TEL.AREA", "617" );
        gen.setValue( "0.asr_adminsection.BIC_TEL.EXCH", "555" );
        gen.setValue( "0.asr_adminsection.BIC_TEL.NUMBER", "1234" );
        // Set the 'type' attribute on node BIC_TEL.
        ((XMLMessageGenerator)gen).setAttributeValue( "0.asr_adminsection.BIC_TEL", "type", "phoneno" );

        gen.setValue( "0.asr_billsection.BILLNM", "Fred Bloggs" );

        gen.setValue( "0.asr_contactsection.INIT", "Wade Boggs" );
        gen.setValue( "0.asr_contactsection.INIT_TEL_NO.AREA", "617" );
        gen.setValue( "0.asr_contactsection.INIT_TEL_NO.EXCH", "555" );
        gen.setValue( "0.asr_contactsection.INIT_TEL_NO.NUMBER", "1234" );
        // Set the 'type' attribute on node INIT_TEL_NO.
        ((XMLMessageGenerator)gen).setAttributeValue( "0.asr_contactsection.INIT_TEL_NO", "type", "phoneno" );
        gen.setValue( "0.asr_contactsection.INIT_STREET", "123 Heaven's Gate" );
        gen.setValue( "0.asr_contactsection.INIT_STATE", "CA" );
        gen.setValue( "0.asr_contactsection.INIT_ZIP_CODE", "97867" );
        gen.setValue( "0.asr_contactsection.DTREC.DATE", "12-12-1998" );
        gen.setValue( "0.asr_contactsection.DTREC.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTREC.
        ((XMLMessageGenerator)gen).setAttributeValue( "0.asr_contactsection.DTREC", "type", "datetime" );
        

        // ------- Populate second ASR. ------- 
        gen.setValue( "1.asr_adminsection.CCNA", "fubar" );
        gen.setValue( "1.asr_adminsection.PON", "asf123454" );
        gen.setValue( "1.asr_adminsection.ICSC", "PT02" );
        gen.setValue( "1.asr_adminsection.DTSENT.DATE", "12-12-1998" );
        gen.setValue( "1.asr_adminsection.DTSENT.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTSENT.
        ((XMLMessageGenerator)gen).setAttributeValue( "1.asr_adminsection.DTSENT", "type", "datetime" );
        gen.setValue( "1.asr_adminsection.DDD", "03-22-1996" );
        ((XMLMessageGenerator)gen).setAttributeValue( "1.asr_adminsection.REQTYP", "type", "complex" );
        gen.setValue( "1.asr_adminsection.REQTYP", "SA" );
        gen.setValue( "1.asr_adminsection.REQTYP.CHAR(0)", "S" );
        gen.setValue( "1.asr_adminsection.REQTYP.CHAR(1)", "A" );
        gen.setValue( "1.asr_adminsection.BAN", "1234987" );
        gen.setValue( "1.asr_adminsection.BIC_TEL.AREA", "617" );
        gen.setValue( "1.asr_adminsection.BIC_TEL.EXCH", "555" );
        gen.setValue( "1.asr_adminsection.BIC_TEL.NUMBER", "1234" );
        // Set the 'type' attribute on node BIC_TEL.
        ((XMLMessageGenerator)gen).setAttributeValue( "1.asr_adminsection.BIC_TEL", "type", "phoneno" );

        gen.setValue( "1.asr_billsection.BILLNM", "Fred Bloggs" );

        gen.setValue( "1.asr_contactsection.INIT", "Wade Boggs" );
        gen.setValue( "1.asr_contactsection.INIT_TEL_NO.AREA", "617" );
        gen.setValue( "1.asr_contactsection.INIT_TEL_NO.EXCH", "555" );
        gen.setValue( "1.asr_contactsection.INIT_TEL_NO.NUMBER", "1234" );
        // Set the 'type' attribute on node INIT_TEL_NO.
        ((XMLMessageGenerator)gen).setAttributeValue( "1.asr_contactsection.INIT_TEL_NO", "type", "phoneno" );
        gen.setValue( "1.asr_contactsection.INIT_STREET", "123 Heaven's Gate" );
        gen.setValue( "1.asr_contactsection.INIT_STATE", "CA" );
        gen.setValue( "1.asr_contactsection.INIT_ZIP_CODE", "97867" );
        gen.setValue( "1.asr_contactsection.DTREC.DATE", "12-12-1998" );
        gen.setValue( "1.asr_contactsection.DTREC.TIME", "11:15 PM" );
        // Set the 'type' attribute on node DTREC.
        ((XMLMessageGenerator)gen).setAttributeValue( "1.asr_contactsection.DTREC", "type", "datetime" );
        gen.setValue( "sa.adminsection.CCNA", "fubar" );
        gen.setValue( "sa.adminsection.PON", "asf123454" );
        gen.create( "sa.sa_circuitdetail" );
        gen.create( "sa.sa_location" );


        // ------- Write 'pretty-print' version to log to show proper construction. ------- 
        gen.log( );
        

        // ------- Get XML document stream, suitable for transmission. ------- 
        String val = gen.generate( );
        
        System.out.println( "XML Document:\n" + val );


        // ------- Create an parser to parse the XML document. ------- 
        MessageParser p = MessageParserFactory.create( Message.XML_TYPE );
        
        // ------- Give the parser the XML document to parse. ------- 
        p.parse( val );
        

        // ------- Show that parse was successful. ------- 
        p.log( );


        String name = null;
        String value = null;

        // ------- Extract values from first ASR. -------
        name = "0.asr_adminsection.CCNA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.PON";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.ICSC";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.DTSENT.DATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.DTSENT.TIME";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.DDD";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.REQTYP";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.REQTYP.0";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.REQTYP.1";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.BAN";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.BIC_TEL.AREA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.BIC_TEL.EXCH";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_adminsection.BIC_TEL.NUMBER";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_billsection.BILLNM";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.INIT";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.INIT_TEL_NO.AREA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.INIT_TEL_NO.EXCH";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.INIT_TEL_NO.NUMBER";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.INIT_STREET";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.INIT_STATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.INIT_ZIP_CODE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.DTREC.DATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "0.asr_contactsection.DTREC.TIME";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );


        // ------- Extract values from second ASR. -------
        name = "1.asr_adminsection.CCNA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.PON";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.ICSC";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.DTSENT.DATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.DTSENT.TIME";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.DDD";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.REQTYP";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.REQTYP.0";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.REQTYP.1";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.BAN";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.BIC_TEL.AREA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.BIC_TEL.EXCH";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_adminsection.BIC_TEL.NUMBER";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_billsection.BILLNM";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.INIT";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.INIT_TEL_NO.AREA";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.INIT_TEL_NO.EXCH";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.INIT_TEL_NO.NUMBER";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.INIT_STREET";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.INIT_STATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.INIT_ZIP_CODE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.DTREC.DATE";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "1.asr_contactsection.DTREC.TIME";
        value = p.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );


        Debug.log( Debug.UNIT_TEST, "END: Access-by-position test.\n\n" );
    }


    /*************************************************************************/

    private static void testMapper ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Mapper test ..." );


        // Get the named file's contents.
        String xmlText = FileUtils.readFile( "msg_api_test.xml" );


        // ------- Create a parser to parse the XML document. ------- 
        MessageParser p = MessageParserFactory.create( Message.XML_TYPE );
        

        // ------- Give the parser the XML document to parse. ------- 
        p.parse( xmlText );
        
        // ------- Write 'pretty-print' version to log to show proper construction. ------- 
        p.log( );


        // ------- Create a mapping object using the parser as a source.  -------
        MessageMapper mapper = MessageMapperFactory.create( MessageMapperFactory.SIMPLE_MAPPER );

        mapper.setSource( p );

        //  ------- Populate the target-source name mappings.  -------
        mapper.addNameMapPair( "asr.asr_adminsection.PON", "purchase order number" );
        mapper.addNameMapPair( "asr.asr_adminsection.DTSENT.DATE", "date sent" );
        mapper.addNameMapPair( "asr.asr_billsection.BILLNM", "billing name" );
        mapper.addNameMapPair( "asr.asr_contactsection.INIT_TEL_NO.EXCH", "Initiator telephone number exchange" );
        mapper.addNameMapPair( "asr.asr_contactsection.INIT_STREET", "Initiator street" );

        // ------- Show mappings.  -------
        mapper.log( );

        // ------- Show that we can extract source values using target names.  -------
        String name = null;
        String value = null;

        name = "purchase order number";
        value = mapper.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "date sent";
        value = mapper.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "billing name";
        value = mapper.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "Initiator telephone number exchange";
        value = mapper.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );

        name = "Initiator street";
        value = mapper.getValue( name );
        System.out.println( "Value of sub-component named [" + name + "] is [" + value + "]" );


        name = "purchase order number";

        if ( mapper.exists( name ) )
            System.out.println( "Node [" + name + "] exists." );
        else
            System.out.println( "Node [" + name + "] doesn't exists." );

        name = "spankie";

        if ( mapper.exists( name ) )
            System.out.println( "Node [" + name + "] exists." );
        else
            System.out.println( "Node [" + name + "] doesn't exists." );


        // ------- Change mapping characteristics. -------
        mapper.invertMapping( true );
        mapper.allowPassThroughNames( true );
        
        // ------- Show inverted mappings.  -------
        mapper.log( );

        Debug.log( Debug.UNIT_TEST, "END: Mapper test.\n\n" );
    }


    /*************************************************************************/

    private static void testMapperConfiguration ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Mapper configuration test ..." );


        // ------- Create a mapping object using the parser as a source.  -------
        // ------- Create a mapping object using the parser as a source.  -------
        MessageMapper mapper = MessageMapperFactory.create( MessageMapperFactory.SIMPLE_MAPPER );


        //  ------- Populate the target-source name mappings from file.  -------
        MappingUtils.loadMappingsFromFile( mapper, "test_mappings.csv" );

        // ------- Show mappings.  -------
        mapper.log( );

        // ------- Change mapping characteristics. -------
        mapper.invertMapping( true );
        mapper.allowPassThroughNames( true );
        
        // ------- Show inverted mappings.  -------
        mapper.log( );

        Debug.log( Debug.UNIT_TEST, "END: Mapper configuration test.\n\n" );
    }


    /*************************************************************************/

    private static void testAttributeCopy ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Attribute copy test ..." );

        XMLMessageGenerator src = new XMLMessageGenerator( "source" );

        src.setValue( "stooges.larry", "hello" );
        src.setValue( "stooges.curly", "hello" );
        src.setValue( "stooges.moe", "hello" );

        src.setAttributeValue( "stooges.curly", "response1", "nyuk" );
        src.setAttributeValue( "stooges.curly", "response2", "OhLook" );

        XMLMessageGenerator tgt = new XMLMessageGenerator( "target" );

        tgt.setValue( "stooges.larry", "hello" );
        tgt.setValue( "stooges.curly", "hello" );
        tgt.setValue( "stooges.moe", "hello" );

        Node n1 = src.getNode( "stooges.curly" );
        Node n2 = tgt.getNode( "stooges.curly" );

        XMLMessageBase.copyNodeAttributes( n2, n1 );

        XMLMessageBase.logFullDescription( true );

        src.log( );
        tgt.log( );

        n1 = src.getNode( "stooges.moe" );
        n2 = tgt.getNode( "stooges.moe" );

        try
        {
            Debug.log( Debug.UNIT_TEST, "Attempting to copy attributes from node which has none ..." );

            XMLMessageBase.copyNodeAttributes( n2, n1 );
        }
        catch ( FrameworkException fe )
        {
            Debug.log( Debug.UNIT_TEST, "Attempt to copy attributes from node that doesn't have any:\n" + 
                       fe.toString() );
        }

        src.log( );
        Debug.log( Debug.UNIT_TEST, src.generate() );

        tgt.log( );
        Debug.log( Debug.UNIT_TEST, tgt.generate() );

        Debug.log( Debug.UNIT_TEST, "END: Attribute copy test.\n\n" );
    }


    /*************************************************************************/

    private static void testSubtreeCopy ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Sub-tree copy test ..." );

        // Get the named file's contents.
        String xmlText = FileUtils.readFile( fileName );

        // ------- Create a parser to parse the XML document. ------- 
        XMLMessageParser p = new XMLMessageParser( );
        
        // ------- Give the parser the XML document to parse. ------- 
        p.parse( xmlText );
        
        // ------- Show that parse was successful. ------- 
        p.log( );


        // ------- Create a generator to build the XML document. ------- 
        XMLMessageGenerator gen = new XMLMessageGenerator( "SubTreeCopyTest" );


        Node n = p.getNode( "asr.asr_adminsection" );

        gen.setValue( "foo.bar", null, n );

        gen.setValue( "foo", null, n );

        n = p.getNode( "asr.asr_contactsection" );

        gen.setValue( "foo", "asr_adminsection", n );

        gen.log( );

        Debug.log( Debug.UNIT_TEST, gen.generate() );
        

        Debug.log( Debug.UNIT_TEST, "END: Sub-tree copy test.\n\n" );
    }


    /*************************************************************************/

    private static void testChildCopy ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Child node copy test ..." );

        // Get the named file's contents.
        String xmlText = FileUtils.readFile( fileName );

        // ------- Create a parser to parse the XML document. ------- 
        XMLMessageParser p = new XMLMessageParser( );
        
        // ------- Give the parser the XML document to parse. ------- 
        p.parse( xmlText );
        
        // ------- Show that parse was successful. ------- 
        p.log( );


        // ------- Create a generator to build the XML document. ------- 
        XMLMessageGenerator gen = new XMLMessageGenerator( "ChildCopyTest" );


        Node n = p.getNode( "asr.asr_adminsection" );

        gen.setValue( "foo", n );

        gen.log( );

        n = p.getNode( "asr.asr_contactsection" );

        gen.setValue( "foo", n );

        gen.log( );

        Debug.log( Debug.UNIT_TEST, gen.generate() );
        

        Debug.log( Debug.UNIT_TEST, "END: Child node copy test.\n\n" );
    }


    /*************************************************************************/

    private static void testGetChildNodes ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Get child nodes test ..." );

        // Get the named file's contents.
        String xmlText = FileUtils.readFile( fileName );


        // ------- Create a parser to parse the XML document. ------- 
        XMLMessageParser p = new XMLMessageParser( );
        

        // ------- Give the parser the XML document to parse. ------- 
        p.parse( xmlText );
        

        // ------- Show that parse was successful. ------- 
        p.log( );

        Node[] children = p.getChildNodes( "asr" );

        Debug.log( Debug.UNIT_TEST, "Node \"asr\" has [" + children.length + "] child nodes." );

        for ( int Ix = 0;  Ix < children.length;  Ix ++ )
            Debug.log( Debug.UNIT_TEST, "Node [" + Ix + "] has name [" + children[Ix].getNodeName() + "]." );
            

        Debug.log( Debug.UNIT_TEST, "END: Get child nodes test.\n\n" );
    }


    /*************************************************************************/

    private static void testTextNodes ( ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: TEXT nodes test ..." );

        XMLMessageGenerator gen = new XMLMessageGenerator( "Root" );

        gen.setValue( "stooges.larry", "hello" );
        gen.setTextValue( "stooges.curly", "hello and " );
        gen.setTextValue( "stooges.curly", "goodbye" );
        gen.setValue( "stooges.moe", "hello" );
        gen.setValue( "alt-stooges.shemp", "nyuk nyuk nyuk" );

        String xml = gen.generate( );


        Debug.log( Debug.UNIT_TEST, xml );
        
        XMLMessageParser p = new XMLMessageParser( xml );

        if ( p.textValueExists( "stooges.curly" ) )
            Debug.log( Debug.UNIT_TEST, p.getTextValue( "stooges.curly" ) );
        else
            Debug.log( Debug.ALL_ERRORS, "Text node existence test failed." );

        if ( p.textValueExists( "stooges.larry" ) )
            Debug.log( Debug.ALL_ERRORS, "Invalid text node." );

        Debug.log( Debug.UNIT_TEST, "END: TEXT nodes test.\n\n" );
    }


    /*************************************************************************/

    private static void testRepeatableParseGen ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.UNIT_TEST, "\n\nBEGIN: Repeatable parse/generation test ..." );

        /*
         * The first part of this test uses an XML with a DTD reference.  In this case,
         * the code is able to identify and remove unecessary whitespace, so the
         * compressWhitespace() call is unecessary.
         */
        String xml = FileUtils.readFile( fileName );

        XMLMessageGenerator gen = null;

        for ( int Ix = 0;  Ix < 6;  Ix ++ )
        {
            Debug.log( Debug.UNIT_TEST, "Iteration [" + Ix + "].  Input XML:\n" + xml );

            XMLMessageParser p = new XMLMessageParser( xml );

            gen = new XMLMessageGenerator( p.getDocument() );

            // This shouldn't alter the XML in this case.
            if ( Ix > 3 )
                gen.compressWhitespace( false );
            
            String newXML = gen.generate( );
            
            Debug.log( Debug.UNIT_TEST, "Iteration [" + Ix + "].  Output XML:\n" + newXML );

            if ( xml.equals( newXML ) )
                Debug.log( Debug.UNIT_TEST, "Iteration [" + Ix + "]: XML documents are identical." );

            xml = newXML;
        }

        // This should also have no effect.
        gen.compressWhitespace( true );

        Debug.log( Debug.UNIT_TEST, "Fully-compressed XML:\n" + gen.generate() );
        

        /*
         * The second part of this test uses an XML without a DTD reference.  In this case,
         * the XML will grow without bound unless the compressWhitespace() call is performed.
         * Note that 2 blank lines will still be present in the output - 1 left in by
         * the compressWhitespace( false) call, and one added by the third-party library
         * during streaming of the DOM.
         */
        gen = new XMLMessageGenerator( "Root" );
        
        gen.setValue( "stooges.larry", "hello" );
        gen.setTextValue( "stooges.curly", "hello and " );
        gen.setTextValue( "stooges.curly", "  goodbye " );
        gen.setTextValue( "stooges.curlyjoe", "     " );
        gen.setValue( "stooges.moe", "hello" );
        gen.setValue( "alt-stooges.shemp", "nyuk nyuk nyuk" );

        xml = gen.generate( );

        for ( int Ix = 0;  Ix < 6;  Ix ++ )
        {
            Debug.log( Debug.UNIT_TEST, "Iteration [" + Ix + "].  Input XML:\n" + xml );

            XMLMessageParser p = new XMLMessageParser( xml );

            gen = new XMLMessageGenerator( p.getDocument() );

            // This should compress the XML output so that no more than 2 blank lines are
            // present between each node.
            if ( Ix > 3 )
                gen.compressWhitespace( false );

            String newXML = gen.generate( );

            Debug.log( Debug.UNIT_TEST, "Iteration [" + Ix + "].  Output XML:\n" + newXML );

            if ( xml.equals( newXML ) )
                Debug.log( Debug.UNIT_TEST, "Iteration [" + Ix + "]: XML documents are identical." );

            xml = newXML;
        }

        // This should compress the XML so that there are no blank lines in the output.
        gen.compressWhitespace( true );

        Debug.log( Debug.UNIT_TEST, "Fully-compressed XML:\n" + gen.generate() );

        Debug.log( Debug.UNIT_TEST, "END: Repeatable parse/generation test.\n\n" );
    }


    /*************************************************************************/

    private static void redirectLogs ( String fileName )
    {
        try
        {
            PrintStream ps = new PrintStream( new FileOutputStream( fileName ) );

            System.setOut( ps );
            System.setErr( ps );
        }
        catch ( IOException ioe )
        {
            System.err.println( ioe );
        }
    }
}

package com.nightfire.adapter.messageprocessor;

import com.nightfire.spi.common.driver.MessageProcessorBase;
import com.nightfire.spi.common.driver.MessageProcessorContext;
import com.nightfire.spi.common.driver.MessageObject;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.FileUtils;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.XMLLibraryPortabilityLayer;
import com.nightfire.common.ProcessingException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.io.ByteArrayInputStream;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

/**
 * This is a message processor which validates a XML Document	
 * against a specified external XSchema and decides next processor.
 */

public class SchemaValidator extends MessageProcessorBase
{
    private static final String XML_LOCATION_PROP = "XML_LOCATION";
    private static final String NAMESPACE_PROP= "NAMESPACE";
    private static final String CONTEXT_SCHEMA_FILE_LOCATION_PROP= "CONTEXT_SCHEMA_FILE_LOCATION";
    private static final String NEXT_MESSAGE_PROCESSOR_VALID_XML_PROP= "NEXT_MESSAGE_PROCESSOR_VALID_XML";
    private static final String NEXT_MESSAGE_PROCESSOR_INVALID_XML_PROP= "NEXT_MESSAGE_PROCESSOR_INVALID_XML";

    private String xmlLocation = null;
    private String nextMessageProcessorForValidXML = null;
    private String nextMessageProcessorForInvalidXML = null;
    private List nameSpaceSchema = null;
    private String noNamespaceSchemaLocation = null;

    /**
     * Constructor.
     */
    public SchemaValidator ( )
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log( Debug.OBJECT_LIFECYCLE, "Creating SchemaValidator message-processor." );
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception com.nightfire.common.ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );

        // Get configuration properties specific to this processor.
        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log(Debug.SYSTEM_CONFIG, "SchemaValidator: Initializing..." );

        xmlLocation = getRequiredPropertyValue( XML_LOCATION_PROP );
        nextMessageProcessorForValidXML = getRequiredPropertyValue( NEXT_MESSAGE_PROCESSOR_VALID_XML_PROP );
        nextMessageProcessorForInvalidXML = getRequiredPropertyValue( NEXT_MESSAGE_PROCESSOR_INVALID_XML_PROP );

        nameSpaceSchema = new LinkedList();

         // Loop until all Namespace schema properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String namespace = getPropertyValue( PersistentProperty.getPropNameIteration( NAMESPACE_PROP, Ix ) );
            String contextSchemaFileLocation = getPropertyValue( PersistentProperty.getPropNameIteration( CONTEXT_SCHEMA_FILE_LOCATION_PROP, Ix ) );

            //take the first schema location as the value to be used for noNamespaceSchemaLocation parser property
            if(Ix==0)
                noNamespaceSchemaLocation = contextSchemaFileLocation;

            // If we can't find a condition value, we are done.
            if ( !StringUtils.hasValue( contextSchemaFileLocation ) )
                break;
            else if ( !StringUtils.hasValue( namespace ) )
                namespace = "";
            
            nameSpaceSchema.add( new NamespaceSchema( namespace, contextSchemaFileLocation ) );
        }

        if(Debug.isLevelEnabled(Debug.SYSTEM_CONFIG))
            Debug.log( Debug.SYSTEM_CONFIG, "SchemaValidator: Initialization done." );
    }

    /**
     * A schema validator processor that just passes its input to output.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  Non-null non exception object, or null.
     *
     * @exception  com.nightfire.common.ProcessingException  Thrown if processing fails.
     * @exception  com.nightfire.framework.message.MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException
    {
        if ( inputObject == null )
            return null;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "Executing Schema Validator message-processor." );

        try
        {
            StringBuffer buffer = new StringBuffer();
            String xml = null;
            Object input = get( xmlLocation,mpContext,inputObject);

            if(input instanceof Document)
            {
               xml = XMLLibraryPortabilityLayer.convertDomToString((Document) get( xmlLocation,mpContext,inputObject));
            }
            else
            {
               xml=(String)input;
            }
            
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log( Debug.MSG_STATUS, get( xmlLocation,mpContext,inputObject).getClass().getName());

            xml = replaceProlog(xml);
            Iterator iter = nameSpaceSchema.iterator( );

            while( iter.hasNext() )
            {
                NamespaceSchema namespaceSchema = (NamespaceSchema)iter.next();
                buffer.append(namespaceSchema.getNamespace());
                buffer.append(" ");
                buffer.append(namespaceSchema.getSchemaFileLocation());
                buffer.append(" ");
            }

            final String factoryImpl = System.getProperty("javax.xml.parsers.SAXParserFactory");
            if (factoryImpl == null)
            {
                System.setProperty("javax.xml.parsers.SAXParserFactory","org.apache.xerces.jaxp.SAXParserFactoryImpl");
                Debug.log( Debug.MSG_STATUS, "No SAXParserFactory Property set, initialized it to org.apache.xerces.jaxp.SAXParserFactoryImpl");
            }

            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            factory.setFeature("http://xml.org/sax/features/validation", true);
            factory.setFeature("http://apache.org/xml/features/validation/schema",true);
            factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);

            final SAXParser parser = factory.newSAXParser();
            parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", buffer.toString().trim());
            parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", noNamespaceSchemaLocation);
            parser.parse(new ByteArrayInputStream(xml.getBytes()), new DefaultHandler() {
                public void error(SAXParseException e) throws SAXException
                {
                    throw e;
                }
            });

            NVPair[ ] validXML = new NVPair[ 1 ];
            validXML[ 0 ] = new NVPair( nextMessageProcessorForValidXML, inputObject );
            return validXML;

        }
        catch (SAXParseException e)
        {
            Debug.log( Debug.ALL_ERRORS, "ERROR: Validation failed\n" + e.getMessage() + "\nline " + e.getLineNumber() + ", column " + e.getColumnNumber() + "\n");
//			System.err.println("Validation failed\n" + e.getMessage() + "\nline " + e.getLineNumber() + ", column " + e.getColumnNumber() + "\n");
            NVPair[ ] invalidXML = new NVPair[ 1 ];
            invalidXML[ 0 ] = new NVPair( nextMessageProcessorForInvalidXML, inputObject );
            return invalidXML;

        }
        catch (SAXException e)
        {
            e.printStackTrace();
            throw new ProcessingException(e.getMessage());
        }
        catch (Exception e)
        {
            Debug.log( Debug.MSG_STATUS, e.toString());
            e.printStackTrace();
            throw new ProcessingException(e.getMessage());
        }

    }

    private String replaceProlog(String str)
    {
        if(str.indexOf("?>") == -1)
            return str;
        else
            return str.substring(str.indexOf("?>")+2);
    }

    private class NamespaceSchema
    {
        private String namespace = null;
        private String schemaFileLocation = null;

        public NamespaceSchema(String namespace, String contextSchemaFileLocation)
        {
            this.namespace = namespace;
            this.schemaFileLocation = contextSchemaFileLocation;
        }

        public String getNamespace()
        {
            return namespace;
        }

        public String getSchemaFileLocation()
        {
            return schemaFileLocation;
        }
    }

    /**
     * For testing.
     * @param args db user pass
     */

    public static void main(String[] args)
    {

        Properties props = new Properties();
        props.put( "DEBUG_LOG_LEVELS", "ALL" );
        props.put( "LOG_FILE", "E:\\basiclsrsend\\logs\\SchemaValidator.log" );
        Debug.showLevels( );
        Debug.configureFromProperties( props );

        if (args.length != 3)
        {
          System.out.println("SchemaValidator: USAGE:  "+
          " jdbc:oracle:thin:@192.168.97.45:1521:ORA10G basicqa1 basicqa1 ");
          return;
        }
        try
        {

            DBInterface.initialize(args[0], args[1], args[2]);
        }
        catch (DatabaseException e)
        {
             System.out.println("SchemaValidator: " +
                      "Database initialization failure: " + e.getMessage());
        }

        try
        {
            String xmlText = FileUtils.readFile( "E:\\basiclsrsend\\test\\ASRSchema\\ETHERNET_VC_SVC_1_UOM.xml");
            Document doc =  XMLLibraryPortabilityLayer.convertStringToDom(xmlText);
            MessageObject input = new MessageObject(doc);
            MessageProcessorContext ctx = new MessageProcessorContext();
            SchemaValidator validator = new SchemaValidator();
            validator.initialize("SchemaValidatorTestGateway","SchemaValidator");
            validator.process(ctx,input);
        }
        catch(Exception e)
        {
            System.out.println("Error : " +  e.toString());
            e.printStackTrace();
        }
    }
}

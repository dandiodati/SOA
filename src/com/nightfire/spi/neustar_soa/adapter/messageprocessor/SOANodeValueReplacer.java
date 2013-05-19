package com.nightfire.spi.neustar_soa.adapter.messageprocessor;
import java.util.*;

import org.w3c.dom.*;

import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;


/**
 * A generic message-processor to replace the node values on
 * an existing XML document.
 */
public class SOANodeValueReplacer extends MessageProcessorBase
{
    /**
     * Property indicating the location of the source XML document, if an
     * existing document is being modified.
     */
    public static final String XML_DOCUMENT_LOCATION_PROP = "XML_DOCUMENT_LOCATION";

    /**
     * Property indicating the name to use for the root node in the generated XML document,
     * if a new document is being created.
     */
    public static final String TARGET_NODE_NAME_PREFIX_PROP = "TARGET_NODE_NAME";

    /**
     * Property prefix giving location of source value.
     */
    public static final String VALUE_SOURCE_LOCATION_PREFIX_PROP = "VALUE_SOURCE_LOCATION";

    /**
     * Property prefix giving default value for node attribute.
     */
    public static final String DEFAULT_VALUE_PREFIX_PROP = "DEFAULT_VALUE";

    /**
     * Property prefix indicating whether column value is optional or required.
     */
    public static final String OPTIONAL_PREFIX_PROP = "OPTIONAL";


    /**
     * Constructor.
     */
    public SOANodeValueReplacer ( )
    {
        Debug.log( Debug.OBJECT_LIFECYCLE, "Creating xml-populator message-processor." );

        nodeInfo = new LinkedList( );
    }


    /**
     * Initializes this object via its persistent properties.
     *
     * @param  key   Property-key to use for locating initialization properties.
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize ( String key, String type ) throws ProcessingException
    {
        // Call base class method to load the properties.
        super.initialize( key, type );
        
        // Get configuration properties specific to this processor.
        Debug.log( Debug.SYSTEM_CONFIG, "SOANodeValueReplacer: Initializing..." );

        xmlDocumentLocation = getPropertyValue( XML_DOCUMENT_LOCATION_PROP );
        
        Debug.log( Debug.SYSTEM_CONFIG, "Location of XML document to replace [" + xmlDocumentLocation + "]." );

        // Loop until all node population configuration properties have been read ...
        for ( int Ix = 0;  true;  Ix ++ )
        {
            String targetNode = getPropertyValue( PersistentProperty.getPropNameIteration( TARGET_NODE_NAME_PREFIX_PROP, Ix ) );

            String sourceLocation = getPropertyValue( PersistentProperty.getPropNameIteration( VALUE_SOURCE_LOCATION_PREFIX_PROP, Ix ) );

            String defaultValue = getPropertyValue( PersistentProperty.getPropNameIteration( DEFAULT_VALUE_PREFIX_PROP, Ix ) );

            String optional = getPropertyValue( PersistentProperty.getPropNameIteration( OPTIONAL_PREFIX_PROP, Ix ) );

            // If none of the configuration items has a value, we're done.
            if ( !StringUtils.hasValue(targetNode) && !StringUtils.hasValue(sourceLocation) 
            		&& !StringUtils.hasValue(defaultValue) && !StringUtils.hasValue(optional) )
                break;

            try
            {
                // Create a new node data object and add it to the list.
                NodeData nd = new NodeData( targetNode, sourceLocation, defaultValue, optional );

                if ( Debug.isLevelEnabled(Debug.SYSTEM_CONFIG) )
                    Debug.log( Debug.SYSTEM_CONFIG, nd.describe() );

                nodeInfo.add( nd );
            }
            catch ( Exception e )
            {
                throw new ProcessingException( "ERROR: Could not create node data description:\n"
                                               + e.toString() );
            }
        }

        Debug.log( Debug.SYSTEM_CONFIG, "Number of nodes to populate [" + nodeInfo.size() + "]." );

        Debug.log( Debug.SYSTEM_CONFIG, "XMLPopulator: Initialization done." );
    }


    /**
     * Populate the XML document with new node values.
     *
     * @param  mpContext The context
     * @param  inputObject  Input message to process.
     *
     * @return  The given input, or null.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     * @exception  MessageException  Thrown if message is bad.
     */
    public NVPair[] process ( MessageProcessorContext mpContext, MessageObject inputObject )
                        throws MessageException, ProcessingException 
    {
    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        	Debug.log( Debug.MSG_STATUS, "SOANodeValueReplacer: processing ... " );
    	
        if ( inputObject == null )
            return null;
        
        // Create an DOM for the request
		Document doc = getDOM( xmlDocumentLocation, mpContext, inputObject );
		
		XMLMessageParser xmlParsar = new XMLMessageParser(doc);

        try
        {
           Iterator iter = nodeInfo.iterator( );

            // While nodes are available to populate ...
            while ( iter.hasNext() )
            {
                NodeData nd = (NodeData)iter.next( );

                String value = null;
                
                
                if(xmlParsar.exists(nd.targetNode)){
                	Node n = xmlParsar.getNode(nd.targetNode);
                	
                	// If a source-location was given and it exists, extract the value.
                    if ( StringUtils.hasValue(nd.sourceLocation) && 
                    		exists( nd.sourceLocation, mpContext, inputObject ) ){
                    	
                        value = getString( nd.sourceLocation, mpContext, inputObject );
                        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        	Debug.log(Debug.MSG_STATUS, "Value to be with replaced "+value);
                        
                        
                    }
                    // If no value was available, use the default
                    if ( value == null || value.equals(""))
                    {
                        value = nd.defaultValue;
                        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        	Debug.log(Debug.MSG_STATUS, "Default Value to be with replaced "+value);

                    }

                    // If no value can be obtained, but it is required, then throw an exception
                    if ( value == null || value.equals(""))
                    {
                        // If the value is required ...
                        if ( nd.optional == false )
                        {
                            // Signal error to caller.
                        	if(Debug.isLevelEnabled(Debug.ALL_ERRORS))
                            	Debug.log(Debug.MSG_STATUS, "ERROR: Missing required value for [" + nd.describe() + "].");
                        	
                            throw new MessageException( "ERROR: Missing required value for [" + nd.describe() + "]." );
                        }
                        else
                        //Skip setting value on node
                        {
                        	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                        		Debug.log( Debug.MSG_STATUS, "Skipping optional node [" + nd.describe() + "] since value is not available." );
                        }
                    }else{
                    
                    	xmlParsar.setNodeValue(n, value);
                    	
                    	if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                         	Debug.log(Debug.MSG_STATUS, "Node value after replacement" +
                         			"["+xmlParsar.getNodeValue(n)+"]");
                    }
               }
               else{
                	
            	   if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                     	Debug.log(Debug.MSG_STATUS, "Target Node is not present so skipping it");
               }
            }
        }
        catch ( Exception e )
        {
            String errMsg = "ERROR: SOANodeValueReplacer: Attempt to replace the node value in XML document failed with error: "
                            + e.getMessage();

            Debug.log( Debug.ALL_ERRORS, errMsg );

            // Re-throw the exception to the driver.
            throw new ProcessingException( errMsg );
        }

        set( xmlDocumentLocation, mpContext, inputObject, xmlParsar.getDocument());

        return( formatNVPair( inputObject ) );
    }

    
    private String xmlDocumentLocation;
    private LinkedList nodeInfo;


    /**
     * Class NodeData is used to encapsulate a description of a node-population
     * description.
     */
    private static class NodeData
    {
        public final String targetNode;
        public final String sourceLocation;
        public final String defaultValue;
        public final boolean optional;

        public NodeData ( String targetNode, 
                          String sourceLocation, String defaultValue, String optional ) throws FrameworkException
        {
            this.targetNode      = targetNode;
            this.sourceLocation  = sourceLocation;
            this.defaultValue    = defaultValue;
            if ( StringUtils.hasValue ( optional ) )
                this.optional    = StringUtils.getBoolean( optional );
            else
                this.optional = false;
        }


        public String describe ( )
        {
            StringBuffer sb = new StringBuffer( );

            sb.append( "Node-population description: " );

            if ( StringUtils.hasValue( targetNode ) )
            {
                sb.append( "target node name [" );
                sb.append( targetNode );
            }

            if ( StringUtils.hasValue( sourceLocation ) )
            {
                sb.append( "], source location [" );
                sb.append( sourceLocation );
            }

            if ( StringUtils.hasValue( defaultValue ) )
            {
                sb.append( "], default value [" );
                sb.append( defaultValue );
            }

            sb.append( "], optional [" );
            sb.append( optional );
            
            sb.append( "]." );

            return( sb.toString() );
        }
    }
    public static void main(String[] args) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "ALL");

		props.put("LOG_FILE", "D:\\logmap.txt");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		try {
			DBInterface.initialize(
					"jdbc:oracle:thin:@192.168.148.34:1521:NOIDADB",
					"soadb", "soadb");

		} catch (DatabaseException e) {

			Debug.log(null, Debug.MAPPING_ERROR, ": "
					+ "Database initialization failure: " + e.getMessage());

		}

		SOANodeValueReplacer sOANodeValueReplacer = new SOANodeValueReplacer();

		try {
			sOANodeValueReplacer.initialize("SOA_VALIDATE_REQUEST","DueDateReplacer");

			MessageProcessorContext mpx = new MessageProcessorContext();
			

			MessageObject mob = new MessageObject();

			mob.set("spid", "1111");
			mob.set("inputMessage", "<?xml version=\"1.0\"?>"
					+ "<SOAMessage>"
					+ "<UpstreamToSOA>"
					+ "<UpstreamToSOAHeader>"
					+ "<InitSPID value=\"1111\" />"
					+ "<DateSent value=\"09-01-2006-035600AM\" />"
					+ "<Action value=\"submit\" />"
					+ "</UpstreamToSOAHeader>"
					+ "<UpstreamToSOABody>"
					+ "<SvCreateRequest>"
					
					+ "<Subscription> "
					
					+ "<Tn value=\"305-591-3696\" />"
										
					+ "</Subscription>"		
					
					+ "<LnpType value=\"lspp\" />"
					+ "<Lrn value=\"9874563210\" />"
					+ "<NewSP value=\"A111\" />"
					+ "<OldSP value=\"1111\" />"
					+ "<NewSPDueDate value=\"08-12-2006-073800PM\" />" 
					
					+ "</SvCreateRequest>" 
					+ "</UpstreamToSOABody>" 
					+ "</UpstreamToSOA>" 
					+ "</SOAMessage>");
			
			sOANodeValueReplacer.process(mpx, mob);

			Debug.log(Debug.BENCHMARK, "Context--->" + mpx.describe());

		} catch (ProcessingException pex) {

			System.out.println(pex.getMessage());

		} catch (MessageException mex) {

			System.out.println(mex.getMessage());

		}
    }
}


package com.nightfire.webgui.core.tag.message;

import com.nightfire.webgui.core.tag.*;
import com.nightfire.webgui.core.tag.message.support.*;
import com.nightfire.webgui.core.meta.*;
import com.nightfire.webgui.core.xml.*;

import com.nightfire.webgui.core.*;
import com.nightfire.framework.constants.PlatformConstants;

import java.io.*;
import javax.servlet.jsp.*;
import java.util.*;

import javax.servlet.jsp.tagext.*;
import javax.servlet.http.*;
import javax.servlet.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.util.xml.*;
import com.nightfire.framework.message.common.xml.*;

/**
 * This tag enables prepopulation of a message page. A prepopulation configuration file is 
 * read to determine the appropriate resources to be applied to a particular message. The 
 * prepopulation configuration file will have message types defined by a boolean XPath
 * expression and each message type will specify the meta, template and an xsl resource which
 * can be applied to that message.
 */
public class Prepopulate extends VariableTagBase
{

    private Object prepopData;
    private String configFileName;
    private String templateVar;
    private String metaVar;
    private String transformVar;

    public void setPrepopData(Object data) throws JspException
    {

	this.prepopData = (Object) TagUtils.getDynamicValue( "prepopData", data, Object.class, this, pageContext );

    }

    public void setConfigFile(String fileName) throws JspException
    {
	
	this.configFileName = (String) TagUtils.getDynamicValue( "configFile", fileName, String.class, this, pageContext );
    
    }

    public void setTemplateVar ( String templateVar ) 
    {

	this.templateVar = templateVar;

    }

    public void setMetaVar ( String metaVar )
    {

	this.metaVar = metaVar;

    }

    public void setTransformVar ( String transformVar )
    {

	this.transformVar = transformVar;

    }
    /**
     * Load the prepopulation configuration.
     * Load the message types.
     * Read the input message.
     * Try to obtain a match in the input message with one of the message types.
     * If a match found, extract the resources for that message type and set it to the 
       specified variables. No match, trigger an error.
    */
    public int doStartTag() throws JspException
    {
	
	try
	{
	    
            String fullPath = pageContext.getServletContext().getRealPath( configFileName ) ;

	    Debug.log(Debug.NORMAL_STATUS, "Prepopulate : Trying to read file [" + fullPath +"]" );

	    String configMessage = FileUtils.readFile ( fullPath );
	    
	    //Parse the configuration file
	    XMLPlainGenerator parser = new XMLPlainGenerator( configMessage );
	    
	    String resourcePath = parser.getValue ( "ResourcePath");

	    Debug.log (Debug.MSG_STATUS, "Prepopulate : Obtained webapp resource path =[" + resourcePath +"]" );

	    List locations = new LinkedList();

	    HashMap metaMap = new HashMap();

	    HashMap templateMap = new HashMap();

	    HashMap transformMap = new HashMap();
	   
	    Node messageType = parser.getNode ("MessageType");

	    Node[] mappings = parser.getChildren ( parser.getNode ( messageType, "Mappings") ) ;
	    	    	    
	    for ( int j=0; j < mappings.length ; j++ )
	    {
  		Node matchNode = parser.getNode ( mappings[j], "Match");

		String match = parser.getNodeValue ( matchNode );
		
		locations.add ( match ) ;
		
		Node resourcesNode = parser.getNode ( mappings[j], "Resources" );
		
		String metaResource = parser.getNodeValue ( parser.getNode ( resourcesNode, "Meta" ) );
		
		String templateResource = parser.getNodeValue ( parser.getNode ( resourcesNode, "Template" ) );
		
		String transformResource = parser.getNodeValue ( parser.getNode ( resourcesNode, "Transform" ) );
		
		metaMap.put ( match, resourcePath + metaResource );

		templateMap.put ( match, resourcePath + templateResource );

		transformMap.put ( match, resourcePath + transformResource );
		
	    }

	    XMLPlainGenerator prepopDataParser = null;

	    if ( prepopData instanceof String ) 
		prepopDataParser = new XMLPlainGenerator ( (String) prepopData );
	    else if ( prepopData instanceof Document )
		prepopDataParser = new XMLPlainGenerator ( (Document) prepopData );

	    String reqType = null;
	    
	    for ( int k = 0 ; k < locations.size(); k ++ ) 
	    {

		String loc = (String) locations.get ( k );
		
		Debug.log(Debug.NORMAL_STATUS, "PrepopulationTag : Checking for xpath [" + loc +"]" );

		ParsedXPath xpath = new ParsedXPath ( loc );
				
		if ( xpath.getBooleanValue (  prepopDataParser.getDocument()) )
		{
		    reqType=loc;
		    break;
		}

	    }

	    if ( ! StringUtils.hasValue ( reqType ) )
	    {
		throw new JspException ( "PrepopulationTag : Could not determine the Req Type/Message Type") ;
	    }


	    Debug.log (Debug.MSG_STATUS, "PrepopulationTag : The meta resource for message type [" + reqType +"] is [" + (String) metaMap.get( reqType ) +", template resource [" + (String) templateMap.get ( reqType ) +"] and transform resource is [" + transformMap.get ( reqType ) +"]" );


	    //Set the resources to the specified locations
	    VariableSupportUtil.setVarObj ( metaVar, metaMap.get( reqType ), null, pageContext  );

	    VariableSupportUtil.setVarObj ( transformVar, transformMap.get ( reqType ) , null, pageContext  );

	    VariableSupportUtil.setVarObj ( templateVar, templateMap.get ( reqType ),  null, pageContext  );
	    
	}
	catch( Exception e )
	{
	    if ( e instanceof JspException ) throw (JspException) e;
	    else
		throw new JspException ( e.getMessage() );
	}

        return SKIP_BODY;
    }

    /** Destruction of the variables **/
    public void release()
    {
       super.release();

       configFileName = null;
       prepopData = null;
       metaVar = null;
       templateVar = null;
       transformVar = null;
    }

}



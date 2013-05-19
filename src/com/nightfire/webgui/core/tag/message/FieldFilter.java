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
 * This tag filters out any extra nodes from the source xml document after 
 * comparing it with the target xml document. The XPath's of the source 
 * document are checked for existence in the target document and any 
 * non-existing nodes are removed.
 */
public class FieldFilter extends VariableTagBase
{

    private Object source;
    private Object target;
    private List paths = new LinkedList();

    public void setSource(Object source) throws JspException
    {
	this.source = (Object) TagUtils.getDynamicValue( "source", source, Object.class, this, pageContext );
    }

    public void setTarget(Object target) throws JspException
    {
	this.target = (Object) TagUtils.getDynamicValue( "target", target, Object.class, this, pageContext );
    }

    /**
     * Parses source and target documents and compares them. Creates a new output document after stripping out
     * any extra nodes from the source document.
     */
    public int doStartTag() throws JspException
    {
	try
	{
	    XMLPlainGenerator sourceParser = null;
	    XMLPlainGenerator targetParser = null;

	    if ( source instanceof XMLPlainGenerator )  
	        sourceParser = (XMLPlainGenerator) source ;
	    else if ( source instanceof String )
		sourceParser = new XMLPlainGenerator ( (String) source );
	    else throw new JspException ( "FieldFilter : Source Attribute value must be either a String or a XMLPlainGenerator");

	    if ( target instanceof XMLPlainGenerator )  
	        targetParser = (XMLPlainGenerator) target ;
	    else if ( target instanceof String )
		targetParser = new XMLPlainGenerator ( (String) target );
	    else if ( target instanceof Document )
	    {
                targetParser = new XMLPlainGenerator ( (Document) target ) ;
            }
	    else throw new JspException ( "FieldFilter : Target Attribute value must be either a String or a XMLPlainGenerator");
            
	    Debug.log(Debug.MSG_STATUS, "SOURCE = [" + sourceParser.getOutput() +"] TARGET =[" + targetParser.getOutput() +"]" );

	    loop ( targetParser, targetParser.getDocument().getDocumentElement() );
	
	    XMLPlainGenerator filterGen = new XMLPlainGenerator ( sourceParser.getDocument().getDocumentElement().getNodeName() );
	
	    for ( int k = 0 ; k < paths.size() ; k ++ )
	    {
		String path = (String) paths.get(k);
		
		if ( sourceParser.exists( path ) )
		{
		    filterGen.setValue ( path , sourceParser.getValue( path ) );
		}
		else
		    Debug.warning("FieldFilter : Path ["  + path +"] doesnt exist in souce ..so filtering it out ..");
	    }

	    Debug.log(Debug.MSG_STATUS,"The filtered xml document is [" + filterGen.getOutput() +"]" );

	    setVarAttribute ( filterGen  );
	    
	}
	catch( Exception e )
	{
	    if ( e instanceof JspException ) throw (JspException) e;
	    else
		throw new JspException ( e.getMessage() );
	}

        return SKIP_BODY;

    }

    /**
     * For every node passed in, the xpath is extracted if its a leaf node. If not a leaf node, 
     * recursion is performed to go over its children.
     */
    
    private void loop ( XMLPlainGenerator parser, Node parent ) throws Exception
    {
        Node[] children = parser.getChildren ( parent );
        
        if ( children.length == 0 )
        {
            paths.add ( parser.getXMLPath( parent ) );
        }
        else 
        {
            for ( int i = 0; i < children.length ; i ++ )
            {
                loop ( parser, children[i] );
            }
       }
    }
    
    public void release()
    {
        super.release();
        this.source = null;
        this.target = null;
        this.paths = null;
    }
    
}



/** 
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/manager/tag/DropDownListTag.java#1 $
 */
package com.nightfire.webgui.manager.tag;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import com.nightfire.webgui.core.tag.*;

import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.ServletUtils;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.webgui.core.DataHolder;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.constants.PlatformConstants;



import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This is a tag handler class which populates a html selection list.
 * The tag attributes include the name for the select menu on html
 * and the data object which contains the drop down list data
 * 
 * usage
 * <gui:dropDownList name="NFH_selectionList" data="${data}"/>
 * <gui:dropDownList name="${name}" data="${data}"/>
 *
 * The data xml should be in the following format ( Element names do not matter except for the DisplayName leaf node )
 * <Root>
 *    <Child1>
 *         <DisplayName value="abc"/>
 *    </Child1>
 *    <Child2>
 *         <DisplayName value="xyz"/>
 *    </Child2>
 * </Root>
 *
 */
 
public class DropDownListTag extends NFTagSupport implements ServletConstants
{  
    /**
     * the local variable to hold the location value
     */
    private Object data;

    // For testing
    private boolean debug = false;

    private Object name;

    /**
     * the name of tag attribute which is used to specify the data.
     */
    private static String DROP_DOWN_LIST_DATA_ATTRIBUTE = "data";

    /**
     * the name of tag attribute which is used to specify the name
     */
    private static String DROP_DOWN_LIST_NAME_ATTRIBUTE = "name";

    
    /**
     * Sets the local variable with the value passed in from the Tag attribute.
     * @param data - the data of the drop down list data
     */
    public void setData ( Object data ) throws JspException
    {
        this.data = TagUtils.getDynamicValue( DROP_DOWN_LIST_DATA_ATTRIBUTE , data , Object.class, this, pageContext);
    }

    /**
     * Sets the local variable with the value passed in from the Tag attribute.
     * @param data - the data of the drop down list data
     */
    public void setName ( Object name) throws JspException
    {
        this.name = TagUtils.getDynamicValue( DROP_DOWN_LIST_NAME_ATTRIBUTE , name , Object.class, this, pageContext);
    }

    /**
     * Sets the debug attribute
     * @param debug - if true, tag is executed in test mode with fake drop down list
     *                if false, tag is executed in the normal mode.
     */
    public void setDebug ( String debug ) throws JspException
    {
        try
        {
            this.debug = StringUtils.getBoolean ( debug );
        }
        catch ( FrameworkException fe )
        {
            throw new JspException("DropDownListTag.debug() [" + fe.getMessage() +"]");
        }
            
    }
    
    /**
     * Using the specified data to get the drop down list data,
     * creates an html drop down selection list.
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY.
     */
    public int doStartTag() throws JspException
    {
        super.doStartTag();
      
        if ( debug  )
        {
            return debug();
        }
        
        return execute();
        
    }
    
    /**
     * Creates a fake drop down list
     */
    private int debug () throws JspException
    {
        log.debug("Running tag in debug mode" );
            
        try
        {
            
            log.debug("Creating test generator" );
            
            XMLPlainGenerator testGen = new XMLPlainGenerator( "List") ;
            
            testGen.setValue ( "Item1.DisplayName","Item 1");
            testGen.setValue ( "Item2.DisplayName","Item 2");
            testGen.setValue ( "Item3.DisplayName","Item 3");
            testGen.setValue ( "Item4.DisplayName","Item 4");
            
            log.debug("The test document is[" + testGen.getOutput() +"]");
            
            Document testData = ( Document ) testGen.getDocument();
            
            log.debug("resetting the attribute document with the test document");
            //Reset the attribute's value
            data = testData;
            
        }
        catch ( Exception e )
        {
            throw new JspException ( "DropDownListTag: Error setting test data [" + e.getMessage() +"]" );
        }

        return execute();        
    }
    
    /**
     * Parses the drop down list and creates the HTML selection object
     */
    private int execute() throws JspException 
    {
        //Create HTML selection list here 

        StringBuffer htmlSelectionBuffer = new StringBuffer();
            
        try
        {
            if ( data == null )
            {
                throw new JspException ( "DropDownListTag: The tag attribute [" + DROP_DOWN_LIST_DATA_ATTRIBUTE +"] must be specified on the JSP");
            }

            log.debug("Creating an xml parser to extract the drop down list");
            //Create a parser to extract the data
            XMLPlainGenerator xmlParser = null;
            
            if ( data instanceof DataHolder )
            {
                log.debug("The object passed through the attribute data is of type DataHolder ");
            
                DataHolder d = ( DataHolder ) data;
                
                String dropDownListData = d.getBodyStr();
                
                xmlParser = new XMLPlainGenerator ( dropDownListData );
            }
            else if ( data instanceof Document )
            {
                log.debug("The object passed through the attribute data is of type Document");
                
                xmlParser = new XMLPlainGenerator ( ( Document ) data );
		
            }
            else
            {
                log.debug("The object passed through the attribute data is of type [" + data.getClass().getName() +"]");

                throw new JspException("DropDownListTag: The tag attribute [" + DROP_DOWN_LIST_DATA_ATTRIBUTE +"] must either be a DataHolder or a Document");
            }

            htmlSelectionBuffer.append ( "<SELECT name=\"" );
            htmlSelectionBuffer.append(  name.toString() +"\">" );

            //Use a Constant here later.. Do not hardcode the Path.
            Node[] children = xmlParser.getChildren ( xmlParser.getDocument().getDocumentElement() );
            
            for ( int i =0; i < children.length ; i++ )
            {
                Node currentNode = children[i];
		
                String display = xmlParser.getValue ( currentNode, "DisplayName" );
		
		if ( xmlParser.exists(currentNode, "DisplayPermission" ) )
		{
			if( !ServletUtils.isAuthorized( pageContext.getSession(), xmlParser.getValue ( currentNode, "DisplayPermission" ) ) )
			{
				continue;
			}

		}
                String value = currentNode.getNodeName();
                htmlSelectionBuffer.append ( "<OPTION value=\"" + value + "\">" + display +"</OPTION>");
            }
            
            htmlSelectionBuffer.append ("</SELECT>");
      
            //Now that the selection list is populated, write it out to the page
            pageContext.getOut().println( htmlSelectionBuffer.toString() );
            
        }
        catch (Exception e)
        {

            if ( e instanceof JspException )
            {
                throw (JspException) e;
            }
            else
            {
                String errorMessage = "ERROR: DropDownListTag.doAfterBody(): Encountered processing error:\n" + e.getMessage();
                log.error( errorMessage);
                
                log.error("", e );
                
                throw new JspException(errorMessage);
            }
        }
        
        return SKIP_BODY;
    }
    
    /**
     * Clean up - this method is called after the doAfterBody() call.
     */
    public void release()
    {
        super.release();
        
        data = null;
        debug = false;
    }

}

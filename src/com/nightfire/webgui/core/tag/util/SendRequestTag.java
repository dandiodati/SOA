/** 
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/R4.4/com/nightfire/webgui/core/tag/util/SendRequestTag.java#1 $
 */
package com.nightfire.webgui.core.tag.util;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import com.nightfire.webgui.core.ProtocolAdapter;
import com.nightfire.webgui.core.DataAdapter;
import com.nightfire.webgui.core.DataHolder;
import com.nightfire.webgui.core.ServletConstants;
import com.nightfire.webgui.core.tag.TagUtils;
import com.nightfire.webgui.core.tag.VariableBodyTagBase;
import com.nightfire.webgui.core.beans.XMLBean;
import com.nightfire.webgui.core.beans.NFBean;
import com.nightfire.framework.message.common.xml.XMLGenerator;
import com.nightfire.framework.message.common.xml.XMLPlainGenerator;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.constants.PlatformConstants;
import com.nightfire.framework.message.util.xml.XMLExtractor;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.common.xml.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * <p><strong>SendRequestTag</strong> is the tag handler which allows for a request
 * to be sent to the backend and a corresponding response data written to the same page
 * which called this tag.
 *
 * This is useful when a request has already been submitted from the
 * previous page but there are additional requests which also need to be processed,
 * and whose responses are needed to be displayed on the current page.  This tag
 * can be used as many times as needed on a single page.</p>
 *
 * This tag, assumes that the request consists of a request header and a request body
 *
 * The attributes used in the tag on the JSP to submit the header and body are 
 * "xmlRequestHeader" and "xmlRequestBody"
 *
 * <p>Sample Usage: 
 * 
 * <nf:SendRequest xmlRequestHeader="$myReqHeader" xmlRequestBody="$myReqBody" />
 *
 * or
 * 
 * <nf:SendRequest xmlRequestHeader="Actual header XML Content goes here" xmlRequestBody="Actual body XML Content goes here"/>
 *
 * or
 *
 *<nf:SendRequest>
 * <Request>
 *   <Header>
 *       Actual header XML Content goes here
 *   </Header>
 *   <Body>
 *       Actual body XML Content goes here
 *   </Body>
 * </Request>
 *</nf:SendRequest>
 */
 
public class SendRequestTag extends VariableBodyTagBase implements ServletConstants, PlatformConstants
{  
    /**
     * the objects to hold the xml request content. 
     */
    private Object xmlRequestHeader;
    private Object xmlRequestBody;

    /**
     * the name of tag attribute
     */
    private static String XML_REQUEST_HEADER_ATTRIBUTE = "xmlRequestHeader";
    private static String XML_REQUEST_BODY_ATTRIBUTE = "xmlRequestBody";
    private static String HEADER_NODE ="Header";
    private static String BODY_NODE ="Body";
    
    /**
     * Sets the xml document to use for constructing the request header - set on the tag attribute on jsp.
     * The following xml documents are supported: String, XMLMessageParser, Document.
     * @param  xml  xml used as input.
     */
    public void setxmlRequestHeader (Object xmlHeader ) throws JspException
    {
        xmlRequestHeader = TagUtils.getDynamicValue( XML_REQUEST_HEADER_ATTRIBUTE , xmlHeader , Object.class, this, pageContext);
    }

    /**
     * Sets the xml document to use for constructing the request body - set on the tag attribute on jsp.
     * The following xml documents are supported: String, XMLMessageParser, Document.
     * @param  xml  xml used as input.
     */
    public void setxmlRequestBody (Object xmlBody ) throws JspException
    {
        xmlRequestBody = TagUtils.getDynamicValue( XML_REQUEST_BODY_ATTRIBUTE , xmlBody , Object.class, this, pageContext);
    }


    /**
     * The request xml is processed using the datadapter and protocoladapter 
     * The response received is set at the specifiec "var" ( from the jsp tag ) when specified.
     * If no variable is specified to output the response, the response will be written to the page itself.
     *
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY.
     */
    public int doEndTag() throws JspException
    {
        //Variables to hold the values passed through the tag attributes
        Object reqHeader = null;
        Object reqBody = null;
        
        //When the header and body are specified through the tag attributes
        if ( xmlRequestHeader!=null && xmlRequestBody!=null )
        {
            reqHeader = xmlRequestHeader;
            reqBody = xmlRequestBody;
        }
        else //get the header and body from the xml body content
        {

            BodyContent body = getBodyContent();
            String req = body.getString();

            log.debug("The request is set as the body of the tag and not through the attributes. [" + req +"]" );
            
            //Check whether the request xml as body of tag is valid xml document?
            try
            {
		      //XMLExtractor has a bug.. its returning the same input message without splitting
                  //reqHeader = XMLExtractor.extract ( HEADER_NODE, HEADER_NODE, req );
                  //reqBody = XMLExtractor.extract ( BODY_NODE, BODY_NODE, req );

			//Until XMLExtractor is fixed..use the following
			XMLPlainGenerator gen = new XMLPlainGenerator ( req ) ;
			Node headerNode = gen.getNode ( HEADER_NODE ) ;
			Node bodyNode = gen.getNode ( BODY_NODE ) ;
		
			XMLPlainGenerator headerGen = new XMLPlainGenerator( HEADER_NODE ) ;
			XMLPlainGenerator bodyGen = new XMLPlainGenerator ( BODY_NODE ) ;
			
			headerGen.copyChildren ( headerGen.getDocument().getDocumentElement(), headerNode ) ;
			bodyGen.copyChildren ( bodyGen.getDocument().getDocumentElement(), bodyNode ) ;
			reqHeader = headerGen.getOutput();
			reqBody = bodyGen.getOutput();

            }
            catch ( MessageException me )
            {
                log.error("Could not extract header and body from message [" + req +"]. Reason [" + me.getMessage() +"]" );
                throw new JspException ( me.getMessage() );
            }
        }
        
        //At this point, the header and body shouldn't be null
        if ( reqHeader==null || reqBody==null )
        {
            Debug.error ( "SendRequestTag: nf:SendRequest tag on JSP is not correctly set. Please set the xmlRequestHeader and xmlRequestBody attributes or set the request xml as the body of the tag");
            throw new JspException ( "SendRequestTag: Please specify the request header and request body in the tag - either by using the tag attributes or by placing the request document with header and body nodes");
        }

        //Now that the header and body are available - get the response using the execution flow similar
        //the Controller Servlet
        try
        {
            //Create the request NF Bean using the header and body
            NFBean requestDataBean = null;
        
            DataHolder transformedResponseData = TagUtils.sendRequest(reqHeader, reqBody, pageContext);
                    
            if (log.isDebugDataEnabled())
            {
                log.debugData("doAfterBody(): Response header obtained:\n" + transformedResponseData.getHeaderStr());
                
                log.debugData("doAfterBody(): Response body obtained:\n" + transformedResponseData.getBodyStr());
            }
            
            //Set the returned response object if the "var" attribute is specified in the tag
            if ( ! varExists() )
            {
                log.debug("The variable attribute not specified. So writing the output to the page.");
                getPreviousOut().println( transformedResponseData.getBodyStr() );
            }
            else //Set it to the output 
            {
                //Do we need it split up the response into header and body here ??
                //For now, let's just spit it out as the whole response.
                setVarAttribute( transformedResponseData ) ;
            }

        }
        
        catch (JspException jspe)
        {
            String errorMessage = "ERROR: SendRequestTag.doAfterBody(): Encountered processing error:\n" + jspe.getMessage();
            
            log.error( errorMessage, jspe );

            throw ( JspException ) jspe;
        }
        catch (Exception e)
        {
            String errorMessage = "ERROR: SendRequestTag.doAfterBody(): Encountered processing error:\n" + e.getMessage();
            
            log.error( errorMessage, e);
            
            throw new JspException(errorMessage);
        }
        
        return EVAL_PAGE;
    }
        
    /**
     * Clean up - this method is called after the doAfterBody() call.
     */
    public void release()
    {
        super.release();
        
        xmlRequestHeader  = null;
        xmlRequestBody  = null;
    }
    
}

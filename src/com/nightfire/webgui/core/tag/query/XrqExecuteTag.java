/**
 * Copyright (c) 2001 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.tag.query;

import com.nightfire.webgui.core.tag.*;

import  javax.servlet.jsp.*;
import  javax.servlet.jsp.tagext.*;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.*;

import  com.nightfire.framework.util.*;
import  com.nightfire.framework.message.parser.*;
import  com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.xrq.*;


import org.w3c.dom.*;


/**
 * Performs a XRQ based search.
 * This tag takes a
 * XRQ XML request via a variable(xml attribute) or as the input body of the tag.
 * This tag is also used to retrieve different pages of data created from a search.
 * The data output(result xml data) of the tag is placed at the
 * variable specified by the var attribute.
 * If the var attribute is not defined then the data output is written
 * to the html page as an xml string.
 *
 * The variable indicated by the varStatus attribute gets a string indicating the
 * results of the query (success, no-match, page-expired, resource-busy).
 * @see com.nightfire.framework.xrq
 */

public class XrqExecuteTag extends VariableBodyTagBase
{

    /**
     * the xml object containing the XRQ request.
     */
    private Object xml;


    private String varDom;

    private String varTSource;

    /**
     * The status code variable.
     */
    private String varStatus;


     /**
     * Sets the xml document to use. This will usually be a variable.
     * The following xml documents are supported: String, XMLMessageParser, Document.
     * @param  xml  xml used as input.
     */
    public void setXml(Object xml) throws JspException
    {
        this.xml = TagUtils.getDynamicValue("xml", xml, Object.class, this, pageContext);
    }


    /**
     * Name of the status variable to set a value indicating the status of the results.
     *
     * @param status The status code variable name.
     * "success" - indicates that 1 or more records were found.
     * "resource-busy" - indicates that resources are busy and the search should be tried again later.
     * "page-expired" - indicates that the requested page no longer exists.
     * "no-match"     - indicates that no records were found.
     */
    public void setVarStatus(String status)
    {
        varStatus = status;
    }


    /**
     * Name of an variable to place a DOM version of the output.
     *
     * @param name The name of the variable.
     */
    public void setVarDom(String name)
    {
        varDom = name;
    }


    /**
     * Name of an variable to place a javax.xml.tranform.Source version of the output.
     *
     * @param name The name of the variable.
     */
    public void setTSource(String name)
    {
        varTSource = name;
    }



    /**
     * Executes the XRQEngine class to get the results of a search request.
     * This expects persistent properties for the XRQEngine to exist under the
     * key 'XRQ' and the type 'ENGINE'. An outside resource is responsible for
     * loading the properties.
     * 
     * @see com.nightfire.framework.xrq.XrqEngine
     */
    public int doEndTag() throws JspException
    {
        // Set the path name of the data template file in the request object so
        // that other objects (such as tag objects) can get to it.

        Object req = null;

        String searchStatus ="";
        BodyContent body = getBodyContent();

        if (xml != null) {
          req = xml;
        } else {
          req = body.getString();
        }

        Page resultPage = null;

        XrqEngine eng = null;

        try {
           eng = XrqEngine.acquireXrqEngine("XRQ","ENGINE");
           if ( req instanceof String) {
              log.debug("Got request: " + req);
              resultPage = eng.execute((String)req);
           } else if ( req instanceof XMLMessageParser )
              resultPage = eng.execute((XMLMessageParser) req);
           else if ( req instanceof Document )
              resultPage = eng.execute( new XMLMessageParser((Document)req));
           else
              throw new JspTagException("Input document [" + req + "] not an instance of String");

           if (resultPage == null)
              searchStatus = "no-match";
           else {
              searchStatus = "success";
              // got results
              // if no var or varDom is specified write results to standard out.
              // else set a String object on the variable indicated by var.
              if (! (varExists() || StringUtils.hasValue(varDom) || StringUtils.hasValue(varTSource) ) )
                 pageContext.getOut().println( resultPage.getXMLRecordsAsStr() );
              else {
                 if ( varExists() )
                    setVarAttribute(resultPage.getXMLRecordsAsStr() );

                    // if there is a var transform.Source variable set an output Source.
                 if ( StringUtils.hasValue(varTSource) )
                    VariableSupportUtil.setVarObj(varName, new DOMSource(resultPage.getXMLRecordsAsDOM().getDocumentElement() ), scope, pageContext );

                 // if there is a var dom variable set an output dom.
                 if ( StringUtils.hasValue(varDom) )
                    VariableSupportUtil.setVarObj(varDom, resultPage.getXMLRecordsAsDOM(), scope, pageContext);
              }
           }

        } catch (UnavailableResourceException e ) {
           searchStatus = "resource-busy";
        } catch (PageExpiredException e) {
           searchStatus = "page-expired";
        } catch (FrameworkException e ) {
           String err = "Failed to perform search: : " + e.getMessage();
           log.error(err );
           throw new JspTagException(err);
        } catch (Exception e) {
           String err = "System error occured: " + e.getMessage();
           log.error(err + " : " + e.toString() );
           log.error("",e);
           throw new JspTagException(err);
        } finally {
           XrqEngine.releaseXrqEngine(eng);
           VariableSupportUtil.setVarObj(varStatus, searchStatus, scope, pageContext);
        }



        return EVAL_PAGE;
    }


    public void release()
    {
        super.release();
        xml = null;
        varStatus = null;
        varDom    = null;
    }


}
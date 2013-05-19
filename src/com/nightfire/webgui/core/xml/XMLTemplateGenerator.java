/*
 * Copyright(c) 2001 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.xml;

// jdk imports
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.*;

// third party imports
import org.w3c.dom.*;
import com.ibm.bsf.BSFManager;

// nightfire imports
import com.nightfire.framework.message.MessageContext;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.MessageGeneratorException;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.util.xml.PathElement;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.DateUtils;
import com.nightfire.framework.util.NVPair;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.locale.NFLocale;




/**
 * XMLTemplateGenerator is an XMLGenerator which supports GUI-style
 * templates as the basis for the generated document.  The constructor for
 * the XMLTemplateGenerator expects a template document (in String or DOM
 * form) in place of just a document name.
 * <p>
 * XMLTemplateGenerator uses the same syntax for node name paths as
 * the XMLGenerator.  Here's a quick review:
 * <p>
 * <table border="1" cellpadding="2">
 *   <tr>
 *     <th>Name</th>                    <th>Returns</th>
 *   </tr>
 *   <tr>
 *     <td>A.B</td>                <td>value of B</td>
 *   </tr>
 *   <tr>
 *     <td>A.Ccontainer.1</td>     <td>value of C1</td>
 *   </tr>
 *   <tr>
 *     <td>A.Ccontainer.C(2)</td>  <td>value of C2</td>
 *   </tr>
 * </table>
 * <p>
 * These examples use this document:
 * <pre>
 *  &lt;?xml version=&quot;1.0&quot;?&gt;
 *  &lt;Root&gt;
 *    &lt;A&gt;
 *      &lt;B value=&quot;value of B&quot;/&gt;
 *      &lt;Ccontainer type=&quot;container&quot;&gt;
 *        &lt;C value=&quot;value of C0&quot;/&gt;
 *        &lt;C value=&quot;value of C1&quot;/&gt;
 *        &lt;C value=&quot;value of C2&quot;/&gt;
 *      &lt;/Ccontainer&gt;
 *      &lt;Dcontainer type=&quot;container&quot;&gt;
 *        &lt;D value=&quot;value of D0&quot;/&gt;
 *        &lt;D value=&quot;value of D1&quot;/&gt;
 *        &lt;D value=&quot;value of D2&quot;/&gt;
 *        &lt;E value=&quot;value of E&quot;/&gt;
 *      &lt;/Dcontainer&gt;
 *    &lt;/A&gt;
 *  &lt;/Root&gt;
 * </pre>
 * <p>
 * An XMLTemplateGenerator is, of course, primarily used for implementing
 * template functionality when generating an XML document.  Here's a rundown
 * of what it supports in a document:
 * <p>
 * <table cellspacing="0" cellpadding="2" border="1"
 *        style="border-style:solid">
 *   <tr>
 *     <th style="background-color:rgb(238,238,255)" colspan="2">Elements</th>
 *   </tr>
 *   <tr>
 *     <th>Name</th><th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>optional-section</td>
 *     <td>Wraps portions of the template which are only to be included if
 *         the enclosed portion is accessed.</td>
 *   </tr>
 *   <tr>
 *     <td>repeating-section</td>
 *     <td>Wraps portions of the template which are to be included each
 *         time a new instance is added (should be used inside a
 *         container).</td>
 *   </tr>
 *   <tr>
 *     <td>initialCount</td>
 *     <td>An attribute of repeating-section used to indicate the initial
 *         number of instances of the repeating section that should be 
 *         created.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>autoInstance</td>
 *     <td>An attribute which controls the number of instances
 *         of some other node in a container. This should have
 *         a xpath expression to the first node of a set of repeating nodes.
 *         example: //ls/servicedetailcontainser/servicedetails[1]
 *         This may be used
 *         in conjunction with the format attribute.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>script</td>
 *     <td>Must contain a single CDATA section declaring any global variables
 *         or functions that are to be available to the rest of the
 *         template.</td>
 *   </tr>
 *   <tr>
 *     <th style="background-color:rgb(238,238,255)"
 *         colspan="2">Attributes</th>
 *   </tr>
 *   <tr>
 *     <th>Name</th><th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>autoNumber</td>
 *     <td>Used in place of a value attribute to create a field which
 *         automatically numbers off sections.  The value of this attribute
 *         must be an XPath expression which points to the parent node (or
 *         possibily itself) that is being numbered off.  This may be used
 *         in conjunction with the format attribute.</td>
 *   </tr>
 *   <tr>
 *     <td>format</td>
 *     <td>Used in conjunction with the autoNumber or autoInstance 
 *         attribute to provide a
 *         format string for the numbers. 
 *         The string follows the format used
 *         by the java.text.DecimalFormat class.</td>
 *   </tr>
 *   <tr>
 *     <td>link</td>
 *     <td>Used in place of a value attribute to allow linking to another
 *         field in the document using an XPath expression.  The contents of
 *         this attribute must be a valid XPath expression. 
 *          Note if the referenced 
 *         field does not exist, then last value on the node 
 *         will always be returned. This allows links to
 *         reference optional nodes.
 *    </td>
 *     <td>dlink</td>
 *     <td>Used in place of a value attribute to allow dynamic linking 
 *         to another field in the document using a script function.  
 *         The contents of
 *         this attribute must be script function that returns
 *         a valid xpath or null. If null is returned then
 *         the current value of the field is used instead of the link.
 *         This allows a field to act as a link or value field depending
 *         on the value returned by the script function. 
 *    </td>
 *   </tr>
 *   <tr>
 *     <td>readOnly</td>
 *     <td>May be used with script, dlink, and link attributes.  This causes
 *         any set operations on these nodes to be ignored.</td>
 *   </tr>
 *   <tr>
 *     <td>runOnce</td>
 *     <td>Used in conjunction with the script attribute to indicate the
 *         script is to be evaluated only once.  See the script attribute for
 *         more information.</td>
 *   </tr>
 *   <tr>
 *     <td>script</td>
 *     <td>Used in place of a value attribute to allow a JavaScript
 *         expression in place of a static value.  This may be used in
 *         conjunction with the runOnce attribute to prevent evaluating the
 *         expression multiple times.</td>
 *   </tr>
 * </table>
 * <p>
 * Several custom functions are available in the JavaScript supported by
 * this class through the global object util.  For information on the
 * methods available through the util object, see the documentation on the
 * Util inner class.
 */
public class XMLTemplateGenerator extends XMLPlainGenerator
{
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      + Constants                                                         +
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
      

    /*
     * Name of elements and attributes with special significance
     */

    public static final String REPEATING_NAME = "repeating-section";
    public static final String SCRIPT_NAME    = "script";
    public static final String AUTONUM_NAME   = "autoNumber";
    public static final String FORMAT_NAME    = "format";
    public static final String LINK_NAME      = "link";
    public static final String DYN_LINK_NAME  = "dlink";
    public static final String DYN_METHOD_NAME  = "dmethod";
    public static final String RUN_ONCE_NAME  = "runOnce";
    public static final String READ_ONLY_NAME = "readOnly";
    public static final String INITIAL_COUNT_NAME = "initialCount";

    public static final String AUTO_INSTANCE_NAME = "autoInstance";

    /**
     * Value used when an error occurs evaluating an expression in the template
     */
    public static final String DEFAULT_ERR_VALUE = "#ERROR";
    
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      + Member Variables                                                  +
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * Available to javascript as util
     */
  private Util util = new Util();
  
     /**
     * Manages our context for executing javascript
     */
    private BSFManager scriptMgr = new BSFManager();


    /**
     * List of repeating-sections, indexed by their parent nodes
     */
    private HashMap repeatingSections = new HashMap();

    /**
     * List of script elements, indexed by their parent nodes
     * (needed to save and restore a template)
     */
    private HashMap scripts = new HashMap();

    

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      + Constructors                                                      +
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    
    /**
     * Creates a new XMLTemplateGenerator using a template or document name.
     * If the template paramater is an XML document, it is used as a template,
     * otherwise this will try to create an XMLTemplateGenerator using the
     * provided string as a document name.
     *
     * @param template  The template to use, or a document name
     *
     * @exception MessageGeneratorException  Thrown if a document cannot be
     *                                       created from template.
     */
    public XMLTemplateGenerator(String template)
        throws MessageGeneratorException
    {
        this(strToDoc(template));
    }

    /**
     * Creates a new XMLTemplateGenerator using a template from an
     * InputStream.
     *
     * @param stream    The stream to read the template from
     *
     * @exception MessageException Thrown if the stream does not contain
     *                             a valid template.
     */
    public XMLTemplateGenerator(InputStream stream) throws MessageException
    {
        this(XMLLibraryPortabilityLayer.convertStreamToDom(stream));
    }

    /**
     * Creates a new XMLTemplateGenerator from a parsed document.
     *
     * @param template  The template to use
     *
     * @exception MessageGeneratorException  Thrown if template is null
     */
    public XMLTemplateGenerator(Document template)
        throws MessageGeneratorException
    {
        super(template);

        try
        {
            // set up our script context
            scriptMgr.declareBean("util", util, Util.class);

            // prepare the template
            prepTemplate();
        }
        catch (Exception ex)
        {
            Debug.logStackTrace(ex);
            throw new MessageGeneratorException(ex);
        }
    }

        

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      + Operations on individual node values                              +
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * Handles merging a source node and its siblings with a destination
     * node.
     *
     * @param srcGen The generator for the source node
     * @param src    The source node
     * @param dest   The destination node
     *
     * @exception MessageException Thrown if there is an error creating a
     *                             node or setting a value in this
     *                             document.
     */
    protected void merge(XMLGenerator srcGen, Node src, Node dest)
        throws MessageException
    {
        // see if the source has any attributes
       NamedNodeMap attrs = src.getAttributes();
       for (int i = 0; i < attrs.getLength(); i++ ) {
          Node attr = attrs.item(i);
          
          if (VALUE_ATTR_NAME.equals(attr.getNodeName()))
          {
            setValue(dest, attr.getNodeValue());
          }
          else
          {
            setAttribute(dest, attr.getNodeName(), attr.getNodeValue() );
          }
       }

        // merge their children
        mergeChildren(srcGen, src, dest);
    }

    /**
     * Get the value Node's value.
     *
     * @param  node  Node to extract value from.
     *
     * @return  Node's value.
     *
     * @exception  MessageException  Thrown if node isn't a value node.
     */
    public String getValue(Node node) throws MessageException
    {
        return getValue(node, new HashSet());
    }
    

    /**
     * Get the value Node's value.
     *
     * @param  node  Node to extract value from.
     *
     * @return  Node's value.
     *
     * @exception  MessageException  Thrown if node isn't a value node.
     */
    private String getValue(Node node, Set trackRefs) throws MessageException
    {
        try
        {
            // if the node is an attribute, get it's value directly
            if (node instanceof Attr)
                return node.getNodeValue();

            // otherwise, assume this is an Element
            Element elem = (Element)node;

            try
            {
                String val;
                // what we do now depends on what attribute is on the element
                // try script
                if(StringUtils.hasValue(
                    (val = elem.getAttribute(AUTO_INSTANCE_NAME)) ) )
                    return getAutoInstanceValue(elem, val);
                else if (StringUtils.hasValue(
                    (val = elem.getAttribute(SCRIPT_NAME)) ))
                    return getScriptValue(elem, val);
                // try dyn link
                else if (StringUtils.hasValue(
                    (val = elem.getAttribute(DYN_LINK_NAME)) ))
                    return getDynLinkValue(elem, val, trackRefs);
	                // try link
                else if (StringUtils.hasValue(
                    (val = elem.getAttribute(LINK_NAME)) ))
                    return getLinkValue(elem, val, trackRefs);
                // try autoNumber
                else if (StringUtils.hasValue(
                    (val = elem.getAttribute(AUTONUM_NAME)) ))
                    return getAutoNumberValue(elem, val);
            }
            catch (Exception ex)
            {
                // errors with scripts,links,etc. do not throw an exception
                // (the GUI shouldn't break due to a minor misconfiguration,
                // it should report the error and continue).
                Debug.error("An error occured while obtaining the value for node [" + getFullPathWithDocName(node) + "]: " + ex);

                return DEFAULT_ERR_VALUE;
            }

            // default to value
            return getAttribute(node, VALUE_ATTR_NAME);
        }
        catch (Exception me)
        {
            throw new MessageException("ERROR: XML node ["
                + node.getNodeName()
                +  "] is not a value node, or is empty.\n\t" + me);
        }
    }


    /**
     * Assign the given value to the node passed-in.
     *
     * @param  node       Node to assign the value to.
     * @param  nodeValue  Value to give the node.
     *
     * @exception  MessageException  Thrown if node can't be initialized with
     *                               value.
     */
    public void setValue(Node node, String nodeValue)
        throws MessageException
    {
        setValue(node, nodeValue, new HashSet());
    }
    
    /**
     * Assign the given value to the node passed-in.
     *
     * @param  node       Node to assign the value to.
     * @param  nodeValue  Value to give the node.
     *
     * @exception  MessageException  Thrown if node can't be initialized with
     *                               value.
     */
    private void setValue(Node node, String nodeValue, Set trackRefs)
        throws MessageException
    {
 
        
        try
        {
            // if the node is an attribute, set it's value directly
            if (node instanceof Attr)
            {
                node.setNodeValue(nodeValue);
                return;
            }

            // otherwise, assume this is an Element
            Element elem = (Element)node;

            String val;
            // what we do now depends on what attribute is on the element
            // try script

            if(StringUtils.hasValue(
                (val = elem.getAttribute(AUTO_INSTANCE_NAME)) ) )
                setAutoInstanceValue(elem, val, nodeValue);
            else if (StringUtils.hasValue(
                (val = elem.getAttribute(SCRIPT_NAME)) ))
                setScriptValue(elem, val, nodeValue);
            // try dyn link
            else if (StringUtils.hasValue(
                (val = elem.getAttribute(DYN_LINK_NAME)) ))
                setDynLinkValue(elem, val, nodeValue, trackRefs);
            // try link
            else if (StringUtils.hasValue(
                (val = elem.getAttribute(LINK_NAME)) ))
                setLinkValue(elem, val, nodeValue, trackRefs);
            // try autoNumber
            else if (StringUtils.hasValue(
                (val = elem.getAttribute(AUTONUM_NAME)) ))
                // any attempt to set an autoNumber is ignored
                return;
            // default
            else
                setAttribute(node, VALUE_ATTR_NAME, nodeValue);
        }
        catch (Exception me)
        {
            Debug.error("ERROR: Could not set value of XML node named ["
            + getFullPathWithDocName(node) + "] to [" + nodeValue + "]\n\t" 
            + me);
            setAttribute(node, VALUE_ATTR_NAME, DEFAULT_ERR_VALUE);
            
        }
  
    }

    /**
     * Returns the xml template generator associated with node.
     * It is possible for a node to have a different document then
     * the current one. This method should only be used to 
     * obtain template generators to find/execute scripts
     * which are not within the current generator.
     * 
     *
     * @param n a <code>Node</code> value
     * @return a <code>XMLTemplateGenerator</code> value
     * @exception MessageException if an error occurs
     */
    private XMLTemplateGenerator getReferencedTemplateGen(Node n) throws MessageException
    {

        XMLGenerator gen = this;
        
        if ( group != null) {
            String name = group.getGeneratorName(n.getOwnerDocument());
            if ( group.exists(name) )
                gen = group.getGenerator(name);
        }
        
        if ( gen instanceof XMLTemplateGenerator )
            return (XMLTemplateGenerator) gen;
        else 
            throw new MessageException("Only Template Generators support scripting attributes");
        
            
    }

    /**
     * Describe <code>resolveXPath</code> method here.
     *
     * @param elem an <code>Element</code> value
     * @param xpath a <code>String</code> value
     * @param required a <code>boolean</code> value
     * @return an <code>Object</code> value
     * @exception MessageException if an errors when trying to obtain value.
     * @exception MultiMatchException if the xpath returns a multiple node match.
     */
    private Object resolveXPath(Element elem, String xpath, boolean required )
        throws MessageException, MultiMatchException
    {
             // the expression is an XPath
        List l = null;

        boolean found = true;
        
        try {  
            WebParsedXPath xp = new WebParsedXPath(xpath, group);
            // evaluate it
            l = xp.getNodeList(elem);
       }
        catch (Exception e ) {
            found = false;
        }
        
        // if no nodes matched return null
        if ( l == null || l.size() == 0 )
            found = false;
        

 
        if (required && !found)
            throw new MessageException("Could not resolve xpath [" + xpath +"]");
        
        

        if (found) {
            // multiple nodes are always an  error
            if (l.size() > 1) {
                throw new MultiMatchException("Expected 1 node from link, but ["
                                           + l.size() + "] were returned.");
            }
            else
                return l.get(0);
        } else
            return null;
        
 
    }
    private String getNumberFormat(String format, int num) 
    {
        
        String result;
        
        // see if we have a format for the number
        if (StringUtils.hasValue(format) )
        {
            DecimalFormat df = new DecimalFormat(format);
            result = df.format(num);
        } else
            result = String.valueOf(num);
           
        return result;
        
    }
    
    private int parseNumber(String format, String num) throws MessageException
    {
            
        int result;
 
        try {           
            if(StringUtils.hasValue(format) ) {
                DecimalFormat df = new DecimalFormat(format);
 
                result = df.parse(num).intValue();
            }
            else 
                result = Integer.parseInt(num);

        }
        catch (Exception e) {
            throw new MessageException(e.getMessage() );
        }
            

        return result;
        
       
    }
    

   protected String getAutoInstanceValue(Element elem, String val) throws Exception
    {

        // see if this is a node or something else
        Object o = resolveXPath(elem, val, true);
        
        if(!(o instanceof Node) )
            throw new MessageException("Only nodes can be referenced from " + AUTO_INSTANCE_NAME + " attributes : " + elem);
       

        Node rNode = (Node)o;
        
        Node parent = rNode.getParentNode();
        
                
        int count = getChildCount(parent, rNode.getNodeName());

        // see if we have a format for the number
        String fmt = elem.getAttribute(FORMAT_NAME);
        String result = getNumberFormat(fmt, count);
        
        elem.setAttribute(VALUE_ATTR_NAME, result);
        
        return result;
    }
    
       
  
// private and protected methods

    /**
     * Returns the value for an element with a script attribute
     *
     * @param elem  The element to obtain the value for
     * @param val   The value of the attribute
     */
    protected String getScriptValue(Element elem, String val,  String previous) throws Exception
    {


       XMLTemplateGenerator gen = getReferencedTemplateGen(elem);
	   // make the previous result available to the manager.
	   // evaluate the script
        gen.util.setPrevious(previous);
        Object resultObj = gen.scriptMgr.eval("javascript",
         elem.getNodeName(),  0, 0, val);

        String result;
        if (resultObj == null)
            // for our purposes, null is the same as the empty string
            result = "";
        else
            result = resultObj.toString();

        // save this as the most recent value
        elem.setAttribute(VALUE_ATTR_NAME, result);

        return result;

	}
   
    // private and protected methods

    /**
     * Returns the value for an element with a script attribute
     *
     * @param elem  The element to obtain the value for
     * @param val   The value of the attribute
     */
    protected String getScriptValue(Element elem, String val) throws Exception
    {
      
        XMLTemplateGenerator gen = getReferencedTemplateGen(elem);
        
        // evaluate the script
        gen.util.setContext(elem);
        Object resultObj = gen.scriptMgr.eval("javascript",
                                          elem.getNodeName(),  0, 0, val);

        String result;
        if (resultObj == null)
            // for our purposes, null is the same as the empty string
            result = "";
        else
            result = resultObj.toString();

        // save this as the most recent value
        elem.setAttribute(VALUE_ATTR_NAME, result);

        // if this has a runOnce attribute, don't evaluate the script again
        String once = elem.getAttribute(RUN_ONCE_NAME);
        if ( StringUtils.hasValue(once) && (!once.equalsIgnoreCase("false")) )
        {
            // kill the script attributes
            elem.removeAttribute(SCRIPT_NAME);
            elem.removeAttribute(RUN_ONCE_NAME);
        }

        return result;
    }

    /**
     * Returns the value for an element with a link attribute
     *
     * @param elem  The element to obtain the value for
     * @param val   The value of the attribute
     */
    protected String getLinkValue(Element elem, String val, Set trackRefs) throws Exception
    {
        Object o = resolveXPath(elem, val, false);
  
        // see if this is a node or something else
          
        String lastValue = "";
        
        //check if we are already in the reference trail
        // and then add us if we are not
        String fPath = getFullPathWithDocName(elem);

        if ( trackRefs.contains(fPath) ) {
            // prevent circular references
                throw new MessageException(
                      "Circular Reference found at node ["
                      + fPath
                      + "]. The following links have been followed:" + trackRefs.toString());
        }

        trackRefs.add(fPath);
       

        if ( o == null) {
            lastValue =  elem.getAttribute(VALUE_ATTR_NAME);
        }
        else if (o instanceof Node)
        {
            // otherwise obtain the value of the node we're linked to
            lastValue =  getValue((Node)o, trackRefs);
        }
        else if (o instanceof String)
           lastValue =  (String)o;
        else
            lastValue = o.toString();

        // save this as the most recent link value
        // this is done so that any xpath references to this node will still 
        // find a value node with the last link value.
        elem.setAttribute(VALUE_ATTR_NAME, lastValue);

        return lastValue;
        
    }


    /**
     * Returns the value for an element with a link attribute
     *
     * @param elem  The element to obtain the value for
     * @param val   The value of the attribute
     */
    protected String getDynLinkValue(Element elem, String val, Set trackRefs) throws Exception
    {

        XMLTemplateGenerator gen = getReferencedTemplateGen(elem);     
        // evaluate the script
        gen.util.setContext(elem);
        Object resultObj = gen.scriptMgr.eval("javascript",
                                          elem.getNodeName(), 0, 0, 
                                          val );

        // if null , return the last set value attribute
        if ( resultObj == null ) {
            return elem.getAttribute(VALUE_ATTR_NAME);
        }


        if (resultObj != null && !(resultObj instanceof String) )
            throw new MessageException("Invalid type returned by script to dynamic link. Only XPath paths are supported.");
        
        val = resultObj.toString();
        
        return getLinkValue(elem, val, trackRefs);     
    }



	/**
     * Returns the value for an element with a link attribute
     *
     * @param elem  The element to obtain the value for
     * @param val   The value of the attribute
     */
    protected String getDynMethodValue(Element elem, String val, Set trackRefs) throws Exception
    {

        XMLTemplateGenerator gen = getReferencedTemplateGen(elem);     
        // evaluate the script
        gen.util.setContext(elem);
        Object resultObj = gen.scriptMgr.eval("javascript",
                                          elem.getNodeName(), 0, 0, 
                                          val );

        // if null , return the last set value attribute
        if ( resultObj == null ) {
            return "";
        }


        if (resultObj != null && !(resultObj instanceof String) )
            throw new MessageException("Invalid type returned by script to dynamic link. Only XPath paths are supported.");
        
        val = resultObj.toString();

       
        return getLinkValue(elem, val, trackRefs);     
    }



    /**
     * Returns the value for an element with an autoNumber attribute
     *
     * @param elem  The element to obtain the value for
     * @param val   The value of the attribute
     */
    protected String getAutoNumberValue(Element elem, String val)
        throws Exception
    {
        // the expression is an XPath
        WebParsedXPath xp = new WebParsedXPath(val, group);

        // evaluate it (must return a single node)
        Node n = xp.getNode(elem);

        // count siblings with the same name
        int num = 1;
        String name = n.getNodeName();
        for (Node sib = n.getPreviousSibling(); sib != null;
             sib = sib.getPreviousSibling())
        {
            // only count element siblings with the same name
            if ( (sib.getNodeType() == Node.ELEMENT_NODE) &&
                 (name.equals(sib.getNodeName())) )
                    ++num;
        }

        // see if we have a format for the number
        String fmt = elem.getAttribute(FORMAT_NAME);
        String result = getNumberFormat(fmt, num);

        return result;
    }

    public void setAutoInstanceValue(Element elem, String val, String nodeValue) throws Exception
    {

        // see if this is a node or something else
        Object o = resolveXPath(elem, val, true);
        
        if(!(o instanceof Node) )
            throw new MessageException("Only nodes can be referenced from " + AUTO_INSTANCE_NAME + " attributes : " + elem);
       

        Node rNode = (Node)o;
        
        String newNodeName = rNode.getNodeName();
        
        Node parent = rNode.getParentNode();   
                
        int count = getChildCount(parent, newNodeName);

        // see if we have a format for the number
        String fmt = elem.getAttribute(FORMAT_NAME);

        int newCount;
        
        try { 
            newCount = parseNumber(fmt, nodeValue);
        }
        catch (MessageException e){
            Debug.error("Invalid value or format for " + elem +", not setting.");
            return;
        }
        
        if (newCount > count) {
            for(int i = count; i < newCount;i++)
                create(parent, newNodeName +"(" +i+")");
        }
        else if ( newCount < count ) {
             for(int i = count; i > newCount; i-- ) 
                remove(parent, newNodeName +"(" +(i-1)+")");
        }

        elem.setAttribute(VALUE_ATTR_NAME, nodeValue);                     
         
    }
    

    /**
     * Sets the value for an element with a script attribute
     *
     * @param elem    The element to obtain the value for
     * @param attrVal The value of the attribute
     * @param newVal  The value to set
     */
    protected void setScriptValue(Element elem, String attrVal, String newVal)
        throws Exception
    {
  
        // ignore for readOnly elements
        String readOnly = elem.getAttribute(READ_ONLY_NAME);
        if ( StringUtils.hasValue(readOnly) &&
             (!readOnly.equalsIgnoreCase("false")) )
            return;

        // get the last value returned by the script
        String oldVal = elem.getAttribute(VALUE_ATTR_NAME);
                
        // if the old value is the same as a new value, ignore it
        if (newVal.equals(oldVal))
            return;

        // otherwise, this overrides our script
        elem.setAttribute(VALUE_ATTR_NAME, newVal);
        elem.removeAttribute(SCRIPT_NAME);
        elem.removeAttribute(RUN_ONCE_NAME);
        elem.removeAttribute(READ_ONLY_NAME);
    }

    /**
     * Sets the value for an element with a link attribute
     *
     * @param elem    The element to obtain the value for
     * @param attrVal The value of the attribute
     * @param newVal  The value to set
     */
    protected void setLinkValue(Element elem, String attrVal, String newVal, Set trackRefs)
        throws Exception
    {

        // ignore for readOnly elements
        String readOnly = elem.getAttribute(READ_ONLY_NAME);
        if ( StringUtils.hasValue(readOnly) &&
             (!readOnly.equalsIgnoreCase("false")) )
            return;


        // set the current node value to the new value
        elem.setAttribute(VALUE_ATTR_NAME, newVal);


        Object o = null;
        
        
        try { 
            o = resolveXPath(elem, attrVal, false); 
        }
        catch (MultiMatchException m) {
           elem.removeAttribute(LINK_NAME);
           elem.removeAttribute(READ_ONLY_NAME);
           return;
        }
        
        catch (Exception e ) {
            return;
        }

        //check if we are already in the rerference trail
        // and then add us if we are not
        String fPath = getFullPathWithDocName(elem);

        if ( trackRefs.contains(fPath) ) {
            // prevent circular references
                throw new MessageException(
                      "Circular Reference found at node ["
                      + fPath
                      + "]. The following links have been followed:" + trackRefs.toString());
        }

        trackRefs.add(fPath);

        if ( o != null) {
            
            if (o instanceof Node) {
                // set the value of the node we're linked to
                setValue((Node)o, newVal, trackRefs);
            } else {
                // this means the link is referencing an attribute node, and is incorrect
                Debug.error("The following node ["+getXMLPath(elem) +"], has a bad link [" + attrVal + "], please correct it. ");
            
                elem.removeAttribute(LINK_NAME);
                elem.removeAttribute(READ_ONLY_NAME);
            }
        }
        
            
             

        

        // anything else means we just ignore any attempt to set the value
    }

   /**
     * Sets the value for an element with a link attribute
     *
     * @param elem    The element to obtain the value for
     * @param attrVal The value of the attribute
     * @param newVal  The value to set
     */
    protected void setDynLinkValue(Element elem, String attrVal, String newVal, Set trackRefs)
        throws Exception
    {

        XMLTemplateGenerator gen = getReferencedTemplateGen(elem);
        // evaluate the script
        gen.util.setContext(elem);
        Object resultObj = gen.scriptMgr.eval("javascript",
                                          elem.getNodeName(), 0, 0, 
                                          attrVal );


       // set the current node value to the new value
        elem.setAttribute(VALUE_ATTR_NAME, newVal);

        // if null , return the last set value attribute
        if ( resultObj == null ) {
            return;
        }

        if (!(resultObj instanceof String) )
            throw new MessageException("Invalid type returned by script to dynamic link. Only XPath paths are supported.");
    
        setLinkValue(elem, resultObj.toString(), newVal, trackRefs);
    }

    /**
     * Get the named child node of the node passed-in.
     *
     * @param  nd         XML document node to get named child node from
     * @param  childName  Name of child node.
     *
     * @return The requested node or null if not found
     */
    protected Node selectChild(Node nd, String childName)
    {
        for (Node n = nd.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if ( (n.getNodeType() == Node.ELEMENT_NODE) &&
                 childName.equals(n.getNodeName()) )
                return n;
        }

        return null;
    }

    /**
     * Find the named sub-node of the given node.  Node names are
     * separated by delimiter values (default is '.').
     *
     * @param  startNode  Root node to search from.
     * @param  nodeName   Name of node (delimited text for nested value).
     * @param  create     Create non-existent node if 'true', otherwise
     *                    throw not-found exception.
     *
     * @return  The named Node object
     *
     * @exception  MessageException  Thrown if node can't be found.
     */
    public Node getNode(Node startNode, String nodeName, boolean create)
        throws MessageException
    {
        if (Debug.isLevelEnabled(Debug.XML_DATA))
            Debug.log(Debug.XML_DATA, "Searching for XML node [" + nodeName
                      +  "].");

        // break the path into its components
        PathElement pathElem = new PathElement(nodeName);

        // Initialize loop variables.
        Node lastNode    = startNode;
        
        // While we can extract node names from string ...
        for (; pathElem != null; pathElem = pathElem.next)
        {
            // Test for 'name(index)'-type of naming.
            if ( (pathElem.name != null) && (pathElem.idx != -1) )
                // Get the node at the given index.
                startNode = selectChild(startNode, pathElem.name,
                                        pathElem.idx);
            // Test for 'index'-type of naming
            else if (pathElem.idx != -1)
                // Locate child node by position.
                startNode = selectChild(startNode, pathElem.idx);
            else
                // Name is not an integer, so locate child node by name.
                startNode = selectChild(startNode, pathElem.name);

            // We couldn't find node, so test to see if we can create it.
            if (startNode == null)
            {
                if (!create)
                    // Caller doesn't want use to create missing node, so
                    // indicate error.
                    throw new MessageException(
                        "ERROR: Couldn't locate node named ["
                        + pathElem.name + "] in [" + nodeName + "].");

                if (Debug.isLevelEnabled(Debug.XML_GENERATE))
                    Debug.log(Debug.XML_GENERATE, "XML element ["
                              + pathElem.name
                              + "] not found, so creating it.");

                Node newNode = null;
                String newNodeName = pathElem.name;
                if (newNodeName == null)
                    newNodeName = INDEXED_NODE_NAME_PREFIX;

                
                // if an index was specified, create to that position
                if (pathElem.idx != -1)
                {
                    for(int i = getChildCount(lastNode, pathElem.name) - 1;
                        i < pathElem.idx; i++)
                    {
                        newNode = addRepeatingSection(lastNode, newNodeName);

                        if (newNode == null)
                        {
                            newNode = doc.createElement(newNodeName);
                            // Append just-created node to last node found.
                            lastNode.appendChild(newNode);
                        }
                    }
                }
                else
                {
                    // we just need one new element
                    newNode = doc.createElement(newNodeName);

                    // Append just-created node to last node found.
                    lastNode.appendChild(newNode);
                }

                // New node is now current node.
                startNode = newNode;
            }

            // Keep track of last non-null Node that was accessed or created.
            lastNode = startNode;
        }
        
        if (Debug.isLevelEnabled(Debug.XML_DATA))
            Debug.log(Debug.XML_DATA, "Located node.");
        
        return startNode;
    }



    /**
     * Attempts to add a repeating section to the document
     *
     * @param lastNode    The parent to add the section to
     * @param newNodeName The name of the node in the section to add
     */
    protected Node addRepeatingSection(Node lastNode, String newNodeName)
        throws MessageException
    {
        Debug.log(Debug.UNIT_TEST, "Searching repeatingSections for ["
                  + lastNode.getNodeName() + "].");

        if (repeatingSections.containsKey(lastNode))
        {
            Debug.log(Debug.UNIT_TEST, "Found match for parent node [" + lastNode +"]");

            // create an object to use to find any matching elements
            NamedFragment testFrag = new NamedFragment(newNodeName);
            ArrayList list = (ArrayList)repeatingSections.get(lastNode);
            int idx = list.indexOf(testFrag);
            if (idx != -1)
            {
                if (Debug.isLevelEnabled(Debug.XML_GENERATE))
                    Debug.log(Debug.XML_GENERATE, "Creating XML element ["
                              + newNodeName
                              + "] from repeating-section template.");

                // copy each node from the fragment
                NamedFragment frag = (NamedFragment)list.get(idx);
                Node source = frag.fragment.getFirstChild();
                copyChildren(lastNode, source);

                // return the match
                Node match = lastNode.getLastChild();
                while ( (match != null) &&
                        ( ! newNodeName.equals(match.getNodeName()) ) )
                    match = match.getPreviousSibling();
                return match;
            }
        }

        return null;
    }

    /**
     * Test to see if an existing node has a value associated with it
     *
     * @param node  The node to test
     *
     * @return true if the node has a value, false otherwise
     */
    public boolean isValueNode(Node node)
    {
        try
        {
            if ( !(node instanceof Element) )
                return false;

            Element elem = (Element)node;

            // test for the value attributes
            if (StringUtils.hasValue(elem.getAttribute(SCRIPT_NAME)))
                return true;
            else if (StringUtils.hasValue(elem.getAttribute(DYN_LINK_NAME)))
                return true;
            else if (StringUtils.hasValue(elem.getAttribute(LINK_NAME)))
                return true;
            else if (StringUtils.hasValue(elem.getAttribute(AUTONUM_NAME)))
                return true;
            else if (StringUtils.hasValue(elem.getAttribute(VALUE_ATTR_NAME)))
                return true;
        }
        catch (Exception ex)
        {
        }

        return false;
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      + Handle final processing of the template                           +
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * Generate message with protocol-specific formatting, and return it.
     * (Calls generateFinalDoc() )
     *
     * @return  Generated message.
     *
     * @exception  MessageGeneratorException  Thrown if message can't be
     *                                        generated.
     */
    public String getOutput() throws MessageGeneratorException
    {
        try
        {
            // this removes all template-specific attributes, replacing them
            // with final values
            generateFinalDoc();

            return XMLLibraryPortabilityLayer.convertDomToString(doc);
        }
        catch (MessageException me)
        {
            throw new MessageGeneratorException(me);
        }
    }

    
    public Document getOutputDOM() throws MessageGeneratorException
    {
       try
        {
            // this removes all template-specific attributes, replacing them
            // with final values
            generateFinalDoc();

             return  getDocument();
        }
        catch (MessageException me)
        {
            throw new MessageGeneratorException(me);
        }


    }

    /**
     * This method returns a copy of the output.
     * In this case, it is a clone of the output and will no longer function
     * as a template.
     */
    public XMLGenerator getOutputCopy() throws MessageGeneratorException
    {
        try
        {
            
            String       oldName =  null;
            if ( group != null)
                oldName = group.getGeneratorName(doc);
            
            Document     copy = XMLLibraryPortabilityLayer.cloneDocument( doc );
             
            XMLTemplateGenerator gen = new XMLTemplateGenerator(copy);
                        
            gen.scriptMgr         = this.scriptMgr;
            gen.repeatingSections = this.repeatingSections;
            gen.scripts           = this.scripts;
            gen.util              = this.util;

            
            // set the copy as the generator in the group
            // so that cross generator access can be resolved.
            if ( group != null) 
                group.setGenerator(oldName, gen);
            
            cleanupNode(copy );

            // put back the original generator since we don't want to break
            // the old group and generator 
            //and are returning a copy of the original.
            if( group != null)
                group.setGenerator(oldName, this);
            
            return new XMLPlainGenerator(copy);
        }
        catch (MessageException me)
        {
            throw new MessageGeneratorException(me);
        }

    }


    /**
     * Walks through the document and replaces any template-specific
     * attributes with their final values.
     * <p>
     * <em>Note:</em> Once this method is called, the document held by this
     * generator no longer behaves as a template.
     */
    public void generateFinalDoc() throws MessageException
    {
        cleanupNode(doc);
    }

    /**
     * Returns a message in a format appropriate for saving.  Creating
     * a new generator from the resulting string restores the generator
     * to its current state.
     *
     * @return  Message for saving.
     *
     * @exception  MessageGeneratorException  Thrown if message can't be
     *                                        save.
     */
    public String getOutputForSave() throws MessageGeneratorException
    {
        try
        {
            // make sure everything has a value so others can deal with the
            // document
            finishNode(doc, false);

            // restore special elements
            saveRepeatingSections();
            saveScripts();

            return XMLLibraryPortabilityLayer.convertDomToString(doc);
        }
        catch (MessageException me)
        {
            throw new MessageGeneratorException(me);
        }
    }

    private String getFullPathWithDocName(Node n) 
    {
        
        String fPath = getXMLPath(n);
        
        if ( group != null) {
            String docPrefix = group.getGeneratorName(n.getOwnerDocument());
            if (StringUtils.hasValue(docPrefix) )
                fPath = docPrefix + "." + fPath;
        }
        
        return fPath;
        
    }
    

    /**
     * Replaces any template-specific attributes of this node with their
     * final values.  The operation is then performed on the node's children
     * and it's next siblings.
     *
     * @param node The node to clean up
     */
    protected void cleanupNode(Node node) throws MessageException
    {
        finishNode(node, true);
    }

    /**
     * Sets final values on nodes, optional removing any template-specific
     * attributes.
     *
     * @param node The node to finish up
     * @param cleanup If true, template-specific attributes are removed
     */
    protected void finishNode(Node node, boolean cleanup)
        throws MessageException
    {
        // for simplicity, this method accepts nulls
        if (node == null)
            return;

        Set trackRefs = new HashSet();
        
        // attributes only occur on elements
        if (node.getNodeType() == Node.ELEMENT_NODE)
        {
            Element elem = (Element)node;

            
            try
            {
                String val;
                // we now try each of the special attributes

                //try autoinstance
                if (StringUtils.hasValue(val = elem.getAttribute(AUTO_INSTANCE_NAME)))
                {
                    if (val.length() > 0)
                    {
                        val = getAutoInstanceValue(elem, val);
                        if (val == null)
                            val = "";
                        elem.setAttribute(VALUE_ATTR_NAME, val);
                    }

                    if (cleanup)
                        elem.removeAttribute(AUTO_INSTANCE_NAME);
                }
                // try autoNumber
                else if (StringUtils.hasValue(val = elem.getAttribute(AUTONUM_NAME)))
                {
                    if (val.length() > 0)
                    {
                        val = getAutoNumberValue(elem, val);
                        if (val == null)
                            val = "";
                        elem.setAttribute(VALUE_ATTR_NAME, val);
                    }

                    if (cleanup)
                        elem.removeAttribute(AUTONUM_NAME);
                }
                 // try link
                else if (StringUtils.hasValue(val = elem.getAttribute(LINK_NAME)))
                {
                    if (val.length() > 0)
                    {
                        // clear the set before using
                        trackRefs.clear();
                        val = getLinkValue(elem, val, trackRefs);
                        if (val == null)
                            val = "";
                        elem.setAttribute(VALUE_ATTR_NAME, val);
                    }
                    
                    if (cleanup)
                        elem.removeAttribute(LINK_NAME);
                }
                // try dynamic link
                else if (StringUtils.hasValue(val = elem.getAttribute(DYN_LINK_NAME)))
                {
                    if (val.length() > 0)
                    {
                        // clear the set before using
                        trackRefs.clear();
                        val = getDynLinkValue(elem, val, trackRefs);
                        if (val == null)
                            val = "";
                        elem.setAttribute(VALUE_ATTR_NAME, val);
                    }
                    
                    if (cleanup)
                        elem.removeAttribute(DYN_LINK_NAME);
                }

				else if (StringUtils.hasValue(val = elem.getAttribute(DYN_METHOD_NAME)))
                {
                    if (val.length() > 0)
                    {
                        // clear the set before using
                        trackRefs.clear();
			String op1val = (val.substring(0,val.indexOf(','))).trim();
                        op1val = getDynMethodValue(elem, op1val, trackRefs);

					Debug.log( Debug.MSG_STATUS, "result of Operation 1 " +op1val );
	
						if(StringUtils.hasValue(op1val))
						{
				
						val = getScriptValue(elem, val.substring(val.indexOf(',')+1),op1val  );

						}
						else								
                              val = "";

					Debug.log( Debug.MSG_STATUS, "result of  DYN_METHOD_NAME " +val );

                        elem.setAttribute(VALUE_ATTR_NAME, val);
                    }
                    
                    if (cleanup)
                        elem.removeAttribute(DYN_METHOD_NAME);
                }


                // try script
                else if (StringUtils.hasValue(val = elem.getAttribute(SCRIPT_NAME)))
                {
                    if (val.length() > 0)
                    {
                        val = getScriptValue(elem, val);
                        
                        if (val == null)
                            val = "";
                        elem.setAttribute(VALUE_ATTR_NAME, val);
                    }

                    if (cleanup)
                        elem.removeAttribute(SCRIPT_NAME);
                    else
                    {
                        // if this has a runOnce attribute,
                        // don't evaluate the script again
                        String once = elem.getAttribute(RUN_ONCE_NAME);
                        if ( StringUtils.hasValue(once) &&
                             (!once.equalsIgnoreCase("false")) )
                        {
                            // kill the script attributes
                            elem.removeAttribute(SCRIPT_NAME);
                            elem.removeAttribute(RUN_ONCE_NAME);
                        }
                    }
                }

                // try format
                if (StringUtils.hasValue(elem.getAttribute(FORMAT_NAME)) && (cleanup))
                    elem.removeAttribute(FORMAT_NAME);

                // try runOnce
                if (StringUtils.hasValue(elem.getAttribute(RUN_ONCE_NAME)))
                    elem.removeAttribute(RUN_ONCE_NAME);

                // try readOnly
                if (StringUtils.hasValue(elem.getAttribute(READ_ONLY_NAME)) && (cleanup))
                    elem.removeAttribute(READ_ONLY_NAME);
            }
            catch (Exception ex)
            {
                // Here errors result in an exception being thrown.
                // Unlike in getValue(), this method is not likely to
                // result in a value displayed to the user, so we thrown an
                // exception so that #ERROR is not part of the request without
                // confirmation from the user.

                Debug.logStackTrace(ex);
                throw new MessageGeneratorException(
                    "Could not generate document: " + ex);
            }
        }

        // do it's first child (the child does it's siblings)
        finishNode(node.getFirstChild(), cleanup);

        // now do siblings
        finishNode(node.getNextSibling(), cleanup);
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      + Handle initialization of the template                             +
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * Prepares the template for our own internal use (evaluates scripts
     * etc.).
     */
    protected void prepTemplate()
    {
        // first handle the scripts
        prepScripts();

        // now the sections
        prepRepeatingSections();
    }

    /**
     * Prepares scripts in the template
     */
    protected void prepScripts()
    {
        // get everything called script
        Element root = doc.getDocumentElement();
        NodeList scripts = root.getElementsByTagName(SCRIPT_NAME);
        int len = scripts.getLength();

        // examine each one
        for (int i = 0; i < len; i++)
        {
            // find the first CDATA/Text child
            Node val = scripts.item(i).getFirstChild();
            while ( (val != null) &&
                    (!StringUtils.hasValue(val.getNodeValue())) )
                val = val.getNextSibling();

            // execute the script
            if (val != null)
            {
                String txt = val.getNodeValue();
                if (StringUtils.hasValue(txt))
                {
                    try
                    {
                        util.setContext(scripts.item(i));
                        scriptMgr.eval("javascript", "script#" + (i + 1),
                                       0, 0, txt);
                    }
                    catch (Exception ex)
                    {
                        Debug.log(Debug.ALL_WARNINGS, "Script #" + (i + 1)
                                  + " failed, ignoring: " + ex);
                    }
                }
                else
                    Debug.log(Debug.XML_LIFECYCLE,
                              "Ignoring empty script in template.");
            }
            else
                Debug.log(Debug.XML_LIFECYCLE,
                          "Ignoring empty script in template.");
        }

        // now remove each script (in reverse order since a node list is
        // updated as nodes are added/removed)
        for (int i = len - 1; i >= 0; i--)
        {
            Node n = scripts.item(i);
            Node parent = n.getParentNode();
            parent.removeChild(n);

            // save the script
            ArrayList list;
            if (this.scripts.containsKey(parent))
                list = (ArrayList)this.scripts.get(parent);
            else
            {
                list = new ArrayList();
                this.scripts.put(parent, list);
            }

            list.add(n);
        }
    }



    /**
     * Prepares repeating-sections in the template
     */
    protected void prepRepeatingSections()
    {
        // get everything called optional-section
        Element root = doc.getDocumentElement();
        NodeList sections = root.getElementsByTagName(REPEATING_NAME);
        int len = sections.getLength();

        // grab each one
        for (int i = 0; i < len; i++)
        {
            // create a new NamedFragment
            Element n = (Element)sections.item(0);
            Node parent = n.getParentNode();
            NamedFragment frag = new NamedFragment(n);

            
            // set up the hash map
            ArrayList list;
            if (repeatingSections.containsKey(parent))
                list = (ArrayList)repeatingSections.get(parent);
            else
            {
                list = new ArrayList();
                repeatingSections.put(parent, list);
            } 

            // add this fragment
            list.add(frag);

            try {
                String countStr = n.getAttribute(INITIAL_COUNT_NAME);
                int count = 1;

                if(StringUtils.hasValue(countStr) )
                    count = Integer.parseInt(countStr);

                Node source = frag.fragment.getFirstChild();
                for(int j =0 ; j < count; j++) {
                    // add the first initial sections for the 
                    // template to display 
                    copyChildren(parent, source);
                }
                
            }
            catch(MessageException e) {
                Debug.error("Could not create first repeating section [" + parent.getNodeName() +"]: " + e.getMessage());
            }

         }
    }

    /**
     * Adds scripts back into the document for saving
     */
    protected void saveScripts()
    {
        // visit each item in the hash set
        Iterator iter = scripts.keySet().iterator();
        while (iter.hasNext())
        {
            Node parent = (Node)iter.next();

            // get all if it's children
            ArrayList children = (ArrayList)scripts.get(parent);
            Iterator childIter = children.iterator();
            while (childIter.hasNext())
            {
                // add each child back in
                Node child = (Node)childIter.next();
                parent.appendChild(child);
            }
        }
    }

 

    /**
     * Adds repeating sections back into the document for saving
     */
    protected void saveRepeatingSections()
    {
        // visit each item in the hash set
        Iterator iter = repeatingSections.keySet().iterator();
        while (iter.hasNext())
        {
            Node parent = (Node)iter.next();

            // get all if it's children
            ArrayList children = (ArrayList)repeatingSections.get(parent);
            Iterator childIter = children.iterator();
            while (childIter.hasNext())
            {
                // add each child back in
                NamedFragment frag = (NamedFragment)childIter.next();
                Node child = frag.fragment.getFirstChild();
                parent.appendChild(child);
            }
        }
    }

    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
      + Inner Classes                                                     +
      +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * The Util class implements the utility functions available to template
     * javascript using the util object.
     */
    protected class Util
    {

      protected Util()
      {
          
          String temp = NFLocale.getDateTimeFormat();
          String noTime = StringUtils.replaceSubstrings(temp, NFLocale.getTimeFormat(), "");
          
          dateTimeSep = StringUtils.replaceSubstrings(noTime, NFLocale.getDateFormat(), "");
          
          
      }
      
        /**
         * Context node for use with XPath expressions
         */
        protected Node ctx;
      private Date parsedDate;
      
        /**
         * Format for dates
         */
        private SimpleDateFormat dateFmt = new SimpleDateFormat(NFLocale.getDateFormat());

        /** 
         * Format for times only  
         */
        private SimpleDateFormat timeFmt = new SimpleDateFormat(NFLocale.getTimeFormat());

        /**
         * Format for Date/Times
         */
       private SimpleDateFormat dateTimeFmt =
            new SimpleDateFormat(NFLocale.getDateTimeFormat());

        private String dateTimeSep;

        private String previousResult;
	 /**
         * Sets the Previous result.
         *
         * @param previous  The Previous result.to set.
         */
        public void setPrevious(String previous)
        {
            this.previousResult = previous;
        }

	/**
         * Sets the Previous result.
         */
        public String getPrevious()
        {
            return previousResult;
        }
 
        /**
         * Sets the context node for use with XPath expressions.
         *
         * @param ctx  The context node to set.
         */
        public void setContext(Node ctx)
        {
            this.ctx = ctx;
        }

      
        /**
         * Returns the time portion of a date with/without time string.
         * 
         *
         * @param  dateTime   A date with/without time value. If the time 
         * portion does not exist or if this value is null, then the 
         * current time is returned . 
         * @param  defaultCurrentTime  Whether or not to return the current time.
         *
         * @return  The time string.
         */
        public String extractTime(String dateTime)
        {
            try
            {
                Date dt = dateTimeFmt.parse(dateTime);
                
                return timeFmt.format(dt);
            }
            catch (Exception e0)
            {
                try {
                    Date dt = timeFmt.parse(dateTime);
                    return timeFmt.format(dt);
                }
                catch (Exception e1) {
                    return timeFmt.format(new Date());
                }
                
            }
        }
       
       

        /**
         * Returns the date portion of a date with/without time string.
         *
         * @param  dateTime  A date with/without time value. If there
         * is no date portion or this value is null, then the current
         * date is returned.
         *
         * @return  The date string.
         */
        public String extractDate(String dateTime)
        {
            try
            {
                Date dt = dateTimeFmt.parse(dateTime);
                
                return dateFmt.format(dt);
            }
            catch (Exception e0)
            {
                try
                {
                    Date dt = dateFmt.parse(dateTime);
                
                    return dateFmt.format(dt);
                }
                catch (Exception e1)
                {
                    return dateFmt.format(new Date());   
                }
            }
        }
        
        /**
         * Returns new date time string with the specified date and time.
         *
         *
         * @param  date  A date with/without time value. If the date portion
         * does not exist or if this value is null, then the 
         * current date will be used.
         * @param  time  A time with/without date value. If the time portion
         * does not exist or if this value is null, then the 
         * current time will be used.
         *
         * @return  The date-time string.
         */
        public String createDateTime(String date, String time)
        {
            try
            {
                date = extractDate(date);
                time = extractTime(time);
                
                Date dt = dateTimeFmt.parse(date + dateTimeSep + time);
                
                return dateTimeFmt.format(dt);
             
            }
            catch (Exception e0)
            {                                 
                return currentDateTime();   
            }
            
        }
        
        /**
         * Returns the current date as a string.
         * Format is the current NFLocale format (default MM-dd-yyyy)
         */
        public String currentDate()
        {
            return dateFmt.format(new Date());
        }

        /**
         * Returns the current date and time as a string.
         * Format is the current NFLocale format (default MM-dd-yyyy-hhmma)
         */
        public String currentDateTime()
        {
            return dateTimeFmt.format(new Date());
        }

        /**
         * Returns the current UTC date and time (Optional)
         * as per given format
         *
         * @return String as date in UTC time
         */
        public String getUTCDateTime(String format)
        {
            String utcTime = "";
            try
            {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                int offset = -(calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000);
                calendar.add(Calendar.MINUTE, offset);
                utcTime = DateUtils.formatTime(calendar.getTime().getTime(), format);
            }
            catch (ParseException e)
            {
                Debug.error("Unable to generate/parse UTI date with format [" + format + "]");
            }
            return utcTime;
        }

        /**
         * Returns the current date and time as a formatted string.
         * Format is the current UTC yNFLocale format (default MM-dd-yyyy-hhmma)
         *
         * @return String as date in UTC time
         */
        public String currentUTCDateTime()
        {
            return getUTCDateTime (NFLocale.getDateTimeFormat());
        }

        /**
         * Returns the current date as a formatted string.
         * Format is the current UTC yNFLocale format (default MM-dd-yyyy-hhmma)
         *
         * @return String as date in UTC time
         */
        public String currentUTCDate()
        {
            return getUTCDateTime (NFLocale.getDateFormat());
        }


        /**
         * Returns the value of an XPath expression
         *
         * @param path  The XPath to evaluate
         *
         * @return The value of the XPath, or null if the expression does
         *         not select any nodes
         */
        public String valueOf(String path)
        {
            try
            {
                // evaluate it
                List l;

                try {
                    WebParsedXPath xp = new WebParsedXPath(path, group);
                    l = xp.getNodeList(ctx);
                }
                catch (Exception e) {
                    return null;
                }
                
                
                // return null for no results
                if (l.size() == 0)
                    return null;

                // stringify the first node returned
                Object o = l.get(0);
                if (o instanceof Node)
                    // this allows access to script values, etc. from XPath
                    return getValue((Node)o);
                else if (o instanceof String)
                    return (String)o;
                else
                    return o.toString();
            }
            catch (Exception ex)
            {
                Debug.log(Debug.ALL_WARNINGS,
                          "An exception occured while evaluating XPath: ["
                          + path + "]: " + ex);
                return DEFAULT_ERR_VALUE;
            }
        }

        /**
         * Logs a message to the debug log
         *
         * @param msg  The message to log
         */
        public void debug(String msg)
        {
            Debug.log(XMLTemplateGenerator.this, Debug.XML_STATUS, msg);
        }
      
      /**
       * Returns the number of documents with the specfied name.
       * This returns the number of document instances with the specified
       * name.
       *
       * @param doc The document name(s).
       * @return The count of the number of instances.
       */
      public int getDocumentCount(String doc) 
      {
        int count = 0;
        int idx = -1;
        
        if ( (idx = doc.indexOf('(')) > -1 )
          doc = doc.substring(0, idx);
        
        if (group == null)
          return count;
        else {
          try {
            count = group.getGeneratorCount(doc);
          }
          catch (MessageException e) {
            Debug.error("Could not obtain document count , returning 0");
          }
          return count;
        }
        
          
      }

      /**
       * Returns the current document's name if it has one.
       * If the document does not have a name null is returned.
       *
       * @return The current document's name.
       */
      public String getCurrentDocName() 
      {
        
        if (group == null)
          return null;
        else
            //use the ctx node since a dynamic link can shift us into
            // another document, which is not the same as the current
            // generator.
          return group.getGeneratorName(ctx.getOwnerDocument());
      }


      /**
       * Returns the current document's index if one is provided.
       * For example if the document name is E911(3) this would
       * return 3.
       * If the document does not have an index or if the index is not
       * a valid string 0 is returned.
       *
       * @return The document name index.
       */
      public int getDocIndex(String doc) 
      {
          int idx,eidx;

          try {
              
              if ( (idx = doc.indexOf('(')) > -1 ) {
                  eidx = doc.indexOf(')', idx);
                  return Integer.parseInt(doc.substring(idx+1, eidx));
              }
              else
                  return 0;
          }
          catch (Exception e) {
              Debug.error("Failed to parse ["+ doc +"] name return index of 0");
              return 0;
          }
      }


    }
    
 
    /**
     * The NamedFragment class is a document fragment identified by a
     * name.
     */
    protected class NamedFragment implements Comparable
    {
        public String name = null;
        public DocumentFragment fragment = null;
        public Node match = null;

        

        /**
         * Create a NamedFragment from a node
         */
        public NamedFragment(Node node)
        {
            // find the child we'll use to match
            match = node.getFirstChild();
            while ( (match != null) &&
                    (match.getNodeType() != Node.ELEMENT_NODE) )
                match = match.getNextSibling();

            // save the name
            if (match != null)
                name = match.getNodeName();

            // create a DocumentFragment
            fragment = doc.createDocumentFragment();
            fragment.appendChild(node);
        }

        /**
         * Create a NamedFragment from just a name
         */
        public NamedFragment(String name)
        {
            this.name = name;
        }

        /**
         * Compares this object to another for equality
         */
        public boolean equals(Object o)
        {
            if (o instanceof NamedFragment)
            {
                String n = ((NamedFragment)o).name;
                if (n != null)
                    return n.equals(name);
                else
                    return n == name;
            }
            else
                return false;
        }

        /**
         * Compares this object to another for the purpose of ordering
         */
        public int compareTo(Object o)
        {
            String n = ((NamedFragment)o).name;
            if ((n != null) && (name != null))
                return n.compareTo(name);
            else if (n == name)
                return 0;
            else if (name == null)
                return 1;
            else
                return -1;
        }
    }

    /**
     * Indicates that multiple nodes were matched.
     *
     */
    public static class MultiMatchException extends MessageException 
    {
        public MultiMatchException(String err) 
        {
            super(err);
        }
        
    }
    



}

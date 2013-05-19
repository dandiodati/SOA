package com.nightfire.adapter.address.msag ;

import org.w3c.dom.*;
import java.util.*;
import java.io.*;
import java.sql.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;


public class XMLPathParser
{
	public  static final String nullMessage = "input params cannot be null or have zero length..." ;
    private static int        pathSeparator = '.' ;
    // =========================================================================
	public static void setValue (XMLMessageGenerator generator, String path, String value)
    	throws AVQEngineException, MessageGeneratorException, MessageException
    {
    	if (generator == null || path == null || value == null ||
        	path.length () <= 0 || value.length () < 0)
        {
			throw new AVQEngineException ("XMLPathParser.setValue: " + nullMessage) ;
        }

		// -----------------------------------------------------------------------
        // parse input path

		XMLPathContainer theXMLPathContainer = XMLPathParser.parsePathEx (path) ;

		// -----------------------------------------------------------------------
        // at this point we have array with: path, [node, name, value]

        /*
        String rootPath = theXMLPathContainer.getPath () ;
		Node       node =  null ;

        System.out.println (" rootPath=[" + rootPath + "]") ;

		if (rootPath != null)
        {
        	generator.create   (rootPath) ;
            node = generator.getNode (rootPath) ;

        	if (theXMLPathContainer.getSize () <= 0)
            {
	        	generator.setValue (rootPath, value) ;

	            return ;
            }


        }
        else
        {
        	// we need to get root node somehow

        	node = generator.getNode ("") ;
        }

		Node rootNode = node ;

        System.out.println ("----------------------------------------------") ;
        System.out.println (" rootNode=[" + rootNode + "]") ;
        */

		Node       node =  null ;

		// -----------------------------------------------------------------------
        // here we must determine if we already have such node or it's new one

        for (int cnt = 0, size = theXMLPathContainer.getSize () ; cnt < size ; ++cnt)
        {

	        XMLPathBlock theXMLPathBlock = theXMLPathContainer.getPathBlock (cnt) ;

            //System.out.println ("#" + cnt + " theXMLPathBlock.path=[" + theXMLPathBlock.getPath () + "]") ;
            //System.out.println ("#" + cnt + " theXMLPathBlock.tagName=[" + theXMLPathBlock.getTagName () + "]") ;
            //System.out.println ("#" + cnt + " theXMLPathBlock.attrName=[" + theXMLPathBlock.getAttrName () + "]") ;
            //System.out.println ("#" + cnt + " theXMLPathBlock.attrValue=[" + theXMLPathBlock.getAttrValue () + "]") ;

            if (theXMLPathBlock.getPath () != null)
            {
            	// here we have path case

            	/*
            	String str = generator.constructDistinguishedName (node) + "." + theXMLPathBlock.getPath () ;

                int idx = str.indexOf (".") ;

                if (idx >= 0)
                {
                	str = str.substring (idx + 1, str.length ()) ;
                }


	        	//node = generator.getNode ("") ;
				//String str = generator.constructRelativeName (generator.getNode (""), node) ;

	            System.out.println ("###### node=[" + str + "]") ;

            	generator.setValue (str, value) ;

                node = generator.getNode (str) ;
                */
                if (node == null)
                {
		        	generator.create   (theXMLPathBlock.getPath ()) ;
        		    node = generator.getNode (theXMLPathBlock.getPath ()) ;

		            if (cnt == size - 1)
        		    {
			        	generator.setValue (theXMLPathBlock.getPath (), value) ;
		            }
                }
                else
                {
                	String [] token = AVQMSAGEngine.tokenize (theXMLPathBlock.getPath (), ".") ;

                    for (int t = 0 ; t < token.length ; ++t)
                    {
                    	Node existingNode = null ;

				        NodeList nl = node.getChildNodes () ;

				        for (int i = 0, size_i = nl.getLength () ; i < size_i ; ++i)
    				    {
        					Node n_i = nl.item (i) ;

			    	        if (n_i.getNodeName ().equals (token [t]))
        				    {
                            	existingNode = n_i ;
                        	}
                        }

                        if (existingNode == null)
                        {
				    		node = node.appendChild (generator.getDocument ().createElement (token [t])) ;
                        }
                        else
                        {
                        	node = existingNode ;
                        }
                    }
                }
            }
            else
            {
            	// here we have node with attribute case

    		    Node replaceNode = null ; // null - append mode, not null - replace mode

		        NodeList nl = node.getChildNodes () ;

	    	    for (int i = 0, size_i = nl.getLength () ; i < size_i ; ++i)
    		    {
	        		Node n_i = nl.item (i) ;

	    	        if (n_i.getNodeName ().equals (theXMLPathBlock.getTagName ()))
    	    	    {
						NamedNodeMap nnp = n_i.getAttributes () ;

	    	            for (int j = 0, size_j = nnp.getLength () ; j < size_j ; ++j)
    		            {
	        	        	Node attrNode = nnp.getNamedItem (theXMLPathBlock.getAttrName ()) ;

        	    	    	if (attrNode != null)
            	    	    {
    	                		String attrNodeValue = attrNode.getNodeValue () ;

		                    	if (attrNodeValue != null && attrNodeValue.equals (theXMLPathBlock.getAttrValue ()))
    	            	        {
	    	    	            	replaceNode = n_i ;
            		            }
        	        	    }
		                }
	    	        }
	        	}


                /*
            	if (cnt == size - 1)
        	    {
		    	    element.setAttribute ("value", value) ;
	            }
                */

        	    //System.out.println ("#" + cnt + " parent node=[" + node + "]") ;

				if (replaceNode == null)
	    	    {
					Element element = generator.getDocument ().createElement (theXMLPathBlock.getTagName ()) ;
			        element.setAttribute (theXMLPathBlock.getAttrName (), theXMLPathBlock.getAttrValue ()) ;

	    			node = node.appendChild (element) ;
	    	    }
    		    else
	        	{
	    			//node = node.replaceChild (element, replaceNode) ;
    	            node = replaceNode ;
		        }
            }

           	if (cnt == size - 1)
			{
				Element element = (Element) node ;
                element.setAttribute ("value", value) ;
			}

            //System.out.println ("#" + cnt + " child node=[" + node + "]") ;
		}

        //System.out.println ("----------------------------------------------") ;
        /*
		// -----------------------------------------------------------------------
        // parse input path

        String [] token = parsePath (path) ;

        if (token == null || token.length <= 0)
        {
			throw new AVQEngineException ("XMLPathParser.setValue: input path [" + path + "] is not correct...") ;
        }

		// -----------------------------------------------------------------------
        // at this point we have array with: path, [node, name, value]

		if (token.length == 1)
        {
        	generator.setValue (token [0], value) ;

            return ;
        }

		// -----------------------------------------------------------------------
        // here we must determine if we already have such node or it's new one

        Node replaceNode = null ; // null - append mode, not null - replace mode

        generator.create (token [0]) ;

		Node node = generator.getNode (token [0]) ;

        NodeList nl = node.getChildNodes () ;

        for (int i = 0, size_i = nl.getLength () ; i < size_i ; ++i)
        {
        	Node n_i = nl.item (i) ;

            if (n_i.getNodeName ().equals (token [1]))
            {
				NamedNodeMap nnp = n_i.getAttributes () ;

                for (int j = 0, size_j = nnp.getLength () ; j < size_j ; ++j)
                {
                	Node attrNode = nnp.getNamedItem (token [2]) ;

                	if (attrNode != null)
                    {
                    	String attrNodeValue = attrNode.getNodeValue () ;

                    	if (attrNodeValue != null && attrNodeValue.equals (token [3]))
                        {
	                    	replaceNode = n_i ;
                        }
                    }
                }
            }
        }

        Document doc = generator.getDocument () ;

		Element element = doc.createElement (token [1]) ;

        element.setAttribute (token [2], token [3]) ;
        element.setAttribute ("value", value) ;

		if (replaceNode == null)
        {
	    	node.appendChild (element) ;
        }
        else
        {
	    	node.replaceChild (element, replaceNode) ;
        }
        */
    }
    // =========================================================================
    private static void setPathSeparator (int ch)
    {
		pathSeparator = ch ;
    }
    // =========================================================================
    private static String [] parsePath (String path)
    	throws AVQEngineException
    {
		// -----------------------------------------------------------------------
        // output format is:
        //
        // output [0] = path to node
        // output [1] = node name
        // output [2] = attr "name" name
        // output [3] = attr "name" value
        //
        // or just
        //
        // output [0] = path to node


		String [] output = null ;

		// -----------------------------------------------------------------------
        // First, we need to look if it is normal string or with attributes

        int idx = path.indexOf ('=') ;

		if (idx < 0)
        {
			// -----------------------------------------------------------------------
        	// no equal sign, so, we have regular path

			output = new String [1] ;

			output [0] = path ;
        }
        else
        {
			// -----------------------------------------------------------------------
        	// we have path with attributes (at least one)

			output = new String [4] ;

			String left  = path.substring (0, idx) ;
			String right = path.substring (idx + 1, path.length ()) ;

        	//System.out.println ("left=[" + left + "]") ;
        	//System.out.println ("right=[" + right + "]") ;

            int leftIdx  = left.lastIndexOf (pathSeparator) ;
            int rightIdx = right.indexOf (pathSeparator) ;

			if (leftIdx >= 0)
			{
				output [2] = left.substring (leftIdx + 1, idx) ; // attr name
	            left = left.substring (0, leftIdx) ;
				leftIdx  = left.lastIndexOf (pathSeparator) ;

                if (leftIdx >= 0)
                {
	                output [1] = left.substring (leftIdx + 1, left.length ()) ; // node name
    	            output [0] = left.substring (0, leftIdx) ; // path
                }
                else
                {
	            	throw new AVQEngineException ("XMLPathParser.parsePath: path string is not correct...") ;
                }
            }
			else
            {
				// -----------------------------------------------------------------------
    	    	// it can't happen.

            	throw new AVQEngineException ("XMLPathParser.parsePath: path string is not correct...") ;
            }

            if (rightIdx >= 0)
            {
				right = right.substring (0, rightIdx) ;
            }

			output [3] = right = removeQuotes (right) ;

        }

		return output ;
    }
    // =========================================================================
    private static XMLPathContainer parsePathEx (String path)
    	throws AVQEngineException
    {
		XMLPathContainer pathContainer = new XMLPathContainer () ;
		// -----------------------------------------------------------------------
        // First, we need to look if it is normal string or with attributes

        int idx = path.indexOf ('=') ;

		if (idx < 0)
        {
			// -----------------------------------------------------------------------
        	// no equal sign, so, we have regular path and nothing else

			//pathContainer.setPath (path) ;
			pathContainer.addPathBlock (parsePathBlock (path)) ;
        }
        else
        {
			// -----------------------------------------------------------------------
        	// we have path with attributes (at least one)

	        while (path != null && path.length () > 0)
    	    {
	        	idx = path.indexOf ('=') ;

                if (idx == -1)
                {
    	            pathContainer.addPathBlock (parsePathBlock (path)) ;

                	break ;
                }

				String left  = path.substring (0, idx) ;
				String right = path.substring (idx + 1, path.length ()) ;

        		//System.out.println ("left=[" + left + "]") ;
	        	//System.out.println ("right=[" + right + "]") ;

	            int leftIdx  = left.lastIndexOf (pathSeparator) ;
        	    int rightIdx = path.indexOf (pathSeparator, idx + 1) ;

	        	//System.out.println ("leftIdx=[" + leftIdx + "]") ;
    	    	//System.out.println ("rightIdx=[" + rightIdx + "]") ;

				if (leftIdx >= 0)
				{
        	    	// trying to figure out if we have path or not

            	    String tempStr = left.substring (0, leftIdx) ;

					leftIdx = tempStr.lastIndexOf (pathSeparator) ;

	                if (leftIdx >= 0)
    	            {
						// yes, we have path

            	        String leadingPath = tempStr.substring (0, leftIdx) ;

		        		//System.out.println ("path=[" + leadingPath + "]") ;

						//pathContainer.setPath (leadingPath) ;
	    	            pathContainer.addPathBlock (parsePathBlock (leadingPath)) ;
    	            }
	                else
    	            {
						// no, we do not have path, so put zero to tempIdx

            	    	leftIdx = -1 ;
	                }

	                String block = null ;

    	            if (rightIdx >= 0)
        	        {
            	    	block = path.substring (leftIdx + 1, rightIdx) ;
	         			path = path.substring (rightIdx + 1, path.length ()) ;
	                }
    	            else
        	        {
						block = path.substring (leftIdx + 1, path.length ()) ;
                		path = null ;
	                }

    	            pathContainer.addPathBlock (parsePathBlock (block)) ;

	    	    	//System.out.println ("block=[" + block + "]") ;

	        		//System.out.println ("path=[" + path + "]") ;
	            }
				else
        	    {
					// -----------------------------------------------------------------------
	    	    	// it can't happend, in this case string looks like this:
                	// name="Eltegra"

    	        	throw new AVQEngineException ("XMLPathParser.parsePath: path string is not correct...") ;
        	    }
	        }
        }

		return pathContainer ;
    }
    // =========================================================================
    private static XMLPathBlock parsePathBlock (String path)
    	throws AVQEngineException
    {
       	int idx = path.indexOf ('=') ;

        if (idx < 0)
        {
	        return new XMLPathBlock (path) ;
		}

        idx = path.indexOf (pathSeparator) ;

        if (idx < 0)
        {
        	throw new AVQEngineException ("XMLPathParser.parsePathBlock: path string is not correct, path=[" + path + "]...") ;
        }

        String tagName = path.substring (0, idx) ;

        path = path.substring (idx + 1, path.length ()) ;

		idx = path.indexOf ("=") ;

        if (idx < 0)
        {
        	throw new AVQEngineException ("XMLPathParser.parsePathBlock: path string is not correct, path=[" + path + "]...") ;
        }

        String attrName  = path.substring (0, idx) ;

        String attrValue = removeQuotes (path.substring (idx + 1, path.length ())) ;

        return new XMLPathBlock (tagName, attrName, attrValue) ;
    }
    // =========================================================================
    public static String removeQuotes (String input)
    {
		input.trim () ;

        int begin = 0, end = input.length () ;

        while (begin < end && input.charAt (begin) == '\"')
        {
        	begin++ ;
        }

        while (end > begin && input.charAt (end - 1) == '\"')
        {
        	end-- ;
        }

        if (begin == end)
        {
        	return "" ;
        }
        else
        {
        	return input.substring (begin, end) ;
        }
    }
    // =========================================================================
    //private static String testString = "address_validation_response.addresscontainer.addressinfo.addressmatch.ilecfieldcontainer.type=\"container\".ILEC_FIELD.name=\"COCODE\"" ;
    //private static String testString = "address_validation_response.addresscontainer.type=\"container\".addressinfo.addressmatch.NPANXX" ;
    //private static String testString = "address_validation_response.addresscontainer.type=\"container\".addressinfo.addressmatch.ilecfieldcontainer.type=\"container\".ILEC_FIELD.name=\"COCODE\"" ;
    //private static String testString = "address_validation_response.addresscontainer.type=\"container\".addressinfo.addressmatch.NPANXX" ;
    private static String testString = "address_validation_response.preorder_response_header.CC" ;
    // =========================================================================
	public static void main (String[] args)
    {
    	try
		{
        	XMLPathContainer theXMLPathContainer = XMLPathParser.parsePathEx (testString) ;

        	System.out.println ("--- --- --- --- --- --- ---") ;
        	System.out.println ("path=[" + theXMLPathContainer.getPath () + "]") ;
            for (int i = 0, size = theXMLPathContainer.getSize () ; i < size ; i++)
   	        {
	        	XMLPathBlock theXMLPathBlock = theXMLPathContainer.getPathBlock (i) ;
	        	System.out.println ("---") ;
	        	System.out.println ("#" + i + " path=[" + theXMLPathBlock.getPath () + "]") ;
	        	System.out.println ("#" + i + " tagName=[" + theXMLPathBlock.getTagName () + "]") ;
	        	System.out.println ("#" + i + " attrName=[" + theXMLPathBlock.getAttrName () + "]") ;
	        	System.out.println ("#" + i + " attrValue=[" + theXMLPathBlock.getAttrValue () + "]") ;

           	}
        	System.out.println ("--- --- --- --- --- --- ---") ;

            String docName = "lsr_preorder_response" ;
            String dtdName = "file:./lsr_preorder.dtd" ;

           	XMLMessageGenerator generator = new XMLMessageGenerator (docName, dtdName) ;

			setValue (generator, testString, "100") ;

            System.out.println (generator.generate ()) ;
            /*
		    String [] token = XMLPathParser.parsePath (args [0]) ;
            if (token != null)
            {
	            for (int i = 0 ; i < token.length ; i++)
    	        {
		        	System.out.println ("#" + i + "=[" + token [i] + "]") ;
            	}
            }
            */
        }
        catch (AVQEngineException ex)
        {
        	System.out.println (ex) ;
        }
        catch (MessageGeneratorException ex)
        {
        	System.out.println (ex) ;
        }
        catch (MessageException ex)
        {
        	System.out.println (ex) ;
        }
    }
}

class XMLPathBlock
{
    private String path      = null ;
    private String tagName   = null ;
    private String attrName  = null ;
    private String attrValue = null ;

    // -------------------------------------------------------------------------
    // default ctor

    public XMLPathBlock ()
    {
    }

    // -------------------------------------------------------------------------
    // ctor

    public XMLPathBlock (String tagName_, String attrName_, String attrValue_)
		throws AVQEngineException
    {
    	if (tagName_    == null || tagName_.length ()   <= 0 ||
        	attrName_   == null || attrName_.length ()  <= 0 ||
            attrValue_  == null || attrValue_.length () <= 0)
        {
			throw new AVQEngineException ("XMLPathBlock.XMLPathBlock: " + XMLPathParser.nullMessage) ;
        }

    	tagName    = tagName_ ;
        attrName   = attrName_ ;
        attrValue  = attrValue_ ;
    }

    public XMLPathBlock (String path_)
		throws AVQEngineException
    {
    	if (path_    == null || path_.length ()   <= 0)
        {
			throw new AVQEngineException ("XMLPathBlock.XMLPathBlock: " + XMLPathParser.nullMessage) ;
        }

    	path    = path_ ;
    }

    public String getPath ()      { return path ;   }
    public String getTagName ()   { return tagName ;   }
    public String getAttrName ()  { return attrName ;  }
    public String getAttrValue () { return attrValue ; }
}

class XMLPathContainer
{
	private String            path = null ;
    private Vector pathBlockVector = null ;

    // -------------------------------------------------------------------------
    // default ctor

    public XMLPathContainer ()
    {
		pathBlockVector = new Vector () ;
    }

    public void setPath (String path_)
		throws AVQEngineException
	{
    	if (path_ == null || path_.length () <= 0)
        {
			throw new AVQEngineException ("XMLPathContainer.setPath: " + XMLPathParser.nullMessage) ;
        }

        path = path_ ;
    }

    public String getPath ()
	{
    	return path ;
    }

    public int getSize ()
    {
    	return pathBlockVector.size () ;
    }

    public void addPathBlock (XMLPathBlock pathBlock_)
		throws AVQEngineException
    {
    	if (pathBlock_ == null)
        {
			throw new AVQEngineException ("XMLPathContainer.addPathBlock: " + XMLPathParser.nullMessage) ;
        }

        pathBlockVector.addElement (pathBlock_) ;
    }

    public XMLPathBlock getPathBlock (int idx)
		throws AVQEngineException
    {
    	if (idx < 0 || idx >= getSize ())
        {
			throw new AVQEngineException ("XMLPathContainer.getPathBlock: index is not in range, idx=[" + idx + "]") ;
        }

        return (XMLPathBlock) pathBlockVector.elementAt (idx) ;
    }
}


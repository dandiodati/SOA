package com.nightfire.framework.xrq;

import java.util.*;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.util.*;

import org.w3c.dom.*;


/**
 * This class is used to convert an XRQ xml request into the specific statement to build a query.
 * This class represents the start of a Clause, and ends up executing the query specific Clause classes
 */
public class QueryBuilder extends ClauseObject
{


 /**
   * Used to convert the xrq query into the destination query format.
   * The root Document should be passed in.
   * NOTE: extractHeader method must be called before this method, on each call.
   *
   * @param clauseNode The root Document of the xrq xml request.
   * @throws MessageException - If the xrq xml request is malformed.
   * @throws FrameworkException - if a system error occurs.
   * @returns The destination formated query string.
   */
  public String eval(Node clauseNode) throws MessageException, FrameworkException
  {
      long start = 0;
      if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
         start = System.currentTimeMillis();

     if (!(clauseNode instanceof Document) ) {
        Debug.error("Method eval(Node clauseNode) requires a parameter which is an instance of Document");
        throw new FrameworkException("Method eval(Node clauseNode) requires a parameter which is an instance of Document");
     }

     XMLMessageParser parser = new XMLMessageParser((Document) clauseNode);


        if (parser.exists(XrqConstants.HEADER_NODE) )  {
           Debug.error(XrqConstants.HEADER_NODE + " exists, extractHeader(Document) method must be called first.");
           throw new MessageException(XrqConstants.HEADER_NODE + " exists, extractHeader(Document) method must be called first." );
        }

        if (Debug.isLevelEnabled(Debug.XML_PARSE) )
           Debug.log(Debug.XML_PARSE, "\n QueryBuilder : Converting xrq request: \n" + parser.getGenerator().generate() );

        String results =  evalAllSubClauses( parser.getNode(".") );

        if ( Debug.isLevelEnabled(Debug.BENCHMARK) )
              Debug.log(Debug.BENCHMARK,"BUILDING QUERY TIME: [" + ((double)(System.currentTimeMillis() - start))/ (double)XrqConstants.MSEC_PER_SECOND + "] seconds.");


     return ( results);
  }


  /**
   * extracts header information from the query, so that evaluation can occur.
   * @return The extracted header node and all its children.
   */
  public Document extractHeader(Document doc) throws MessageException
  {
     XMLMessageParser parser = new XMLMessageParser(doc);
     XMLMessageGenerator gen = new XMLMessageGenerator(XrqConstants.HEADER_NODE);

     if (!parser.exists(XrqConstants.HEADER_NODE) )
        return gen.getDocument();


     Node headRoot = parser.getNode(XrqConstants.HEADER_NODE);

     // duplicates the header node onto a new document
     XMLMessageBase.copyNode(gen.getDocument(), headRoot);

     // removes it from the main document
     headRoot.getParentNode().removeChild(headRoot);

     return gen.getDocument();
  }

} 
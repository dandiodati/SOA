package com.nightfire.framework.xrq;

import org.w3c.dom.*;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.util.*;

/**
 * Represents a single page of information returned from a PageList Object.
 */
public class Page
{

 /**
  * Page key node used to identity future pages with in this PageList.
  */
  //public static final String PAGE_KEY_NODE = "pageKey";

 /**
  * The current page index.
  */
  //public static final String CURRENT_PAGE_NODE = "currentPage";

 /**
  * Total number of pages currently loaded into Page List cache.
  * This will always have a value.
  */
  //public static final String IN_MEM_PAGES_NODE = "inMemoryPages";

 /**
  * Total number of pages available( including Page List Cache).
  * Depending on the number of records this may still be processing.
  * If the value is unknown at this time then this field will have a value of ?.
  */
 // public static final String TOTAL_PAGES_NODE = "totalPages";

  /**
   * The Header node used to contain the above information.
   */
  //public static final String HEADER_NODE = "Header";
  /**
   * The node where repeating records will end up.
   */
  //public static final String RECORD_CONTAINER_NODE = "RecordContainer";

  private int count;

  private XMLMessageParser parser;
  private MessageContext headContext;

  /**
   * Creates a Page from a xml dom.
   * @param xml - The document to create the Page from.
   */
  public Page( Document xml) throws MessageException
  {
      init(xml);
  }

   /**
   * Creates a Page from a xml String.
   * @param xml - The xml string to create the Page from.
   */
  public Page( String xml) throws MessageException
  {
     XMLMessageParser temp = new XMLMessageParser(xml);
     init(temp.getDocument() );
  }

  // initializes the page
  private void init(Document xml ) throws MessageException
  {

     parser = new XMLMessageParser(xml);
     count = parser.getChildCount( XrqConstants.RECORD_CONTAINER_NODE);
     parser.getGenerator().create(XrqConstants.HEADER_NODE);
     headContext = parser.getContext(XrqConstants.HEADER_NODE);
  }


  /**
   * Returns this Page as an xml Document.
   */
  public Document getXMLRecordsAsDOM() throws MessageException
  {
     return parser.getDocument();
  }

  /**
   * returns this page as an xml string.
   */
  public String getXMLRecordsAsStr() throws MessageException
  {
     return parser.getGenerator().generate();
  }

  /**
   * Returns the number of records within this page.
   */
  public int getNumOfRecords()
  {
     return count;
  }

  /**
   * Addes a header field to the Page.
   * @param name The name of the field to add.
   * @param value The values of the field.
   */
  public void setHeaderField(String name, String value) throws MessageException {
     parser.getGenerator().setValue(headContext, name, value);
  }

  /**
   * Returns a header field.
   * @param name The field to return( refer to constants defined in Page).
   * @returns The header value or null if the field does not exist.
   */
  public String getHeaderField(String name) {

     String value = null;
     if (parser.exists(headContext, name) ) {
        try {
           value =  parser.getValue(headContext,name);
        } catch (MessageException e ) {
           Debug.warning("Page: " + e.getMessage());
        }
     } else
        Debug.warning("Page: " + "Header field " + name + " does not exist.");

     return value;

  }

  /**
   * Returns a string describing the contents of this page.
   */
  public String describe() {
     StringBuffer buf = new StringBuffer("Page Contents: ");
     try {
        buf.append( parser.getGenerator().generate() );
     } catch (MessageException e) {
       Debug.warning("Could not describe page contents: " + e.getMessage() );
     }
     return buf.toString();
  }


} 
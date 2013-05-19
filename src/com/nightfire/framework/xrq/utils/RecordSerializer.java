package com.nightfire.framework.xrq.utils;

import com.nightfire.framework.xrq.*;

import java.util.*;
import com.nightfire.framework.xrq.utils.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.locale.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.*;

import java.util.*;
import java.io.*;


import org.w3c.dom.*;

/**
 * This class provides access to the results of a query. It formats rows of data
 * into serialized strings. It also provides a conversion method (toXML(...) ) to convert
 * serialized strings, produced by this class, into an xml representation.
 *
 * NOTE: The hasNext(...), next(...) methods take on the properties of an Iterator, so once next is
 * called there is no longer access to a past records. Therefore records need to be stored into
 * some other format.
 *
 * NOTE2: If this class holds onto resources, then the refering QueryExecutor class should handle
 * its cleanup. At any time callers must be able to use this class to transform serialized records into
 * xml via the toXML(...) method.
 */

public interface RecordSerializer
{


   /**
    * Delimiter used to separate fields in a record.
    */
   public static final String DELIM = "<\\@$@\\>";

   
  /**
   * indicates if there is another record.
   */
   public boolean hasNext();


  /**
   * returns the next record.
   */
   public String next();


     /**
     * converts a record from a serialized string into a xml dom.
     * @param dom the Document to add the xml to.
     * @param xmlParentLoc The parent location for the parent node of the added xml.
     * @param serializedStr the seriallized string to convert to xml.
     *
     * @exception throws a procesing exception if an error occurs.
     *
     */
    public void toXML(Document dom, String xmlParentLoc, String serializedStr) throws FrameworkException;
}
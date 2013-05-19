/* HashtableXMLMapper class
 * This class generated the RTF document from the XML message as input &
 * RTF template as a reference.
 */
package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import java.util.*;


/**
 * HashtableXMLMapper class.
 * extends MessageProtocolAdapter.
 */
public class HashtableXMLMapper extends MessageProcessorBase {

  private final static String MAP_KEY_PROP = "MAP_KEY_";
  private final static String MAP_VALUE_PROP = "MAP_VALUE_";
  private final static String NEXT_PROCESSOR_NAME = "NEXT_PROCESSOR_NAME";
  private final static String XML_TEMPLATE = "XML_TEMPLATE";

  private final static String SE_FORMAT = "mm-dd-yyyy";
  private final static String WORD_FORMAT = "m/d/yy";
  private final static String DUE_DATE = "DD";

  private String nextProcessor = "";
  private String xmlTemplate = "";

  private boolean testing = false;

  public HashtableXMLMapper() {}

  /**
   * This method loads properties in virtual machine.
   * @param key - Ket value of persistentproperty.
   * @param type - Type value of persistentproperty.
   * @exception ProcessingException - this exception is thrown by super class
   *              for any problem with initialization .
   */

  public void initialize ( String key, String type ) throws ProcessingException
  {
    if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log( Debug.OBJECT_LIFECYCLE, "Loading properties for HashtableXMLMapper.");

    super.initialize(key, type);

    nextProcessor = (String)adapterProperties.get(NEXT_PROCESSOR_NAME);
    xmlTemplate = (String)adapterProperties.get(XML_TEMPLATE);

    if(nextProcessor == null)
    {
       Debug.log( Debug.ALL_ERRORS, "ERROR: HashtableXMLMapper: NEXT_PROCESSOR_NAME property cannot be null.");
       throw new ProcessingException("ERROR: HashtableXMLMapper: NEXT_PROCESSOR_NAME property cannot be null.");
    }

    if(xmlTemplate == null)
    {
       Debug.log( Debug.ALL_ERRORS, "ERROR: HashtableXMLMapper: XML_TEMPLATE property cannot be null.");
       throw new ProcessingException("ERROR: HashtableXMLMapper: XNL_TEMPLATE property cannot be null.");
    }
  }

  
  /**
   * This method is called from driver.
   * It does all processing to generate RTF document.
   * @param context - MessageProcessorContext.
   * @param input - input - this contains XML message that needs to e converted to RTF document.
   * @return NVPair[] - this array contains only one instance of NVPair.
   *                    name - value of NEXT_PROCESSOR_NAME; value - generated RTF document.
   * @exception MessageException or ProcessingException - messageException is thrown if parsing of XML fails
   *              ProcessingException is thrown for any other significant error.
   */
  public NVPair[] execute ( MessageProcessorContext context, Object input ) throws MessageException, ProcessingException
  {
    String xmlAfterTokenReplacement = null;

    if (input == null) return null;

    if (!(input instanceof Hashtable) )
      throw new ProcessingException("ERROR: HashtableXMLMapper invalid input, expecting a Hashtable.");

    Hashtable ht = (Hashtable) input;

    for (Enumeration enumerator =  ht.keys(); enumerator.hasMoreElements(); ) {
      String key = (String)enumerator.nextElement();

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "[" + key + "], [" + ht.get(key) + "]");
    }
    xmlAfterTokenReplacement = replaceTokens( createMap(ht) );

      if(Debug.isLevelEnabled(Debug.MSG_DATA))
        Debug.log(Debug.MSG_DATA, xmlAfterTokenReplacement);

    NVPair nvpair = new NVPair(nextProcessor, xmlAfterTokenReplacement );
    NVPair array[] = new NVPair[]{nvpair};
    return array;
  }

  /**
   * This method reads XML template file.
   * @return String - string of rtf template.
   * @exception ProcessingException - thrown if FrameworkException is reported
   *              from FileUtil class
   */
  private String getXMLTemplate() throws  ProcessingException
  {
     String template = "";
     if(Debug.isLevelEnabled(Debug.BENCHMARK))
        Debug.log( Debug.BENCHMARK, "RTFGenerator: Reading template file.");
     try
     {
       template = FileUtils.readFile(xmlTemplate);
     }
     catch(FrameworkException exp)
     {
        Debug.log( Debug.ALL_ERRORS, "ERROR: HashtableXMLMapper: Template file cannot be located.");
        throw new ProcessingException(xmlTemplate + " file cannot be located");
     }

     return template;
  }

  public String replaceTokens(Hashtable map) throws ProcessingException {

    String xml = getXMLTemplate();

    for (Enumeration enumerator = map.keys(); enumerator.hasMoreElements();) {
       String substr = (String) enumerator.nextElement();

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log( Debug.MSG_DATA, "\tKEY: " + substr + ", VALUE: " + (String) map.get(substr) );

       xml = StringUtils.replaceSubstringsIgnoreCase(xml, substr , (String) map.get(substr) );
    }

    return xml;
  }

  public Hashtable createMap(Hashtable ht) throws ProcessingException {

      Hashtable map = new Hashtable();
      String mapKey = null;
      String mapValue = null;
      int i = 1;

      if (testing) {
      map.put("%%pon%%",  ht.get("1"));
      map.put("%%supplierOrderNumber%%", ht.get("2"));
      map.put("%%salescode%%", ht.get("3"));
      map.put("%%duedate%%", convertToSEDate( (String) ht.get("DD") ));
      return map;
      }


      mapKey = (String) adapterProperties.get(MAP_KEY_PROP + String.valueOf(i));
      mapValue = (String) adapterProperties.get(MAP_VALUE_PROP + String.valueOf(i++));

      while ( mapKey != null && mapValue != null)
        {
          if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "[" + mapKey + "], [" + mapValue + "]");

            //check if due date field then convert to SE Date Format
            if ( mapValue.equals(DUE_DATE) ) {
               map.put( mapKey, convertToSEDate( (String) ht.get(mapValue) ) ) ;
               //get next mapKey and mapValue then continue
               mapKey = (String) adapterProperties.get(MAP_KEY_PROP + String.valueOf(i));
               mapValue = (String) adapterProperties.get(MAP_VALUE_PROP + String.valueOf(i++));
               continue;
            }


            map.put(mapKey, ht.get(mapValue) );

          if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA ,"HashtableXMLMapper: adding [" + mapKey  + "," + ht.get(mapValue) + "]");

            mapKey = (String) adapterProperties.get(MAP_KEY_PROP + String.valueOf(i));
            mapValue = (String) adapterProperties.get(MAP_VALUE_PROP + String.valueOf(i++));
        }

        //if no filters were set then construct default FileInfo
        if ( map.isEmpty() ) {
           Debug.log(Debug.ALL_ERRORS, "ERROR: HashtableXMLMapper no mapping specified.");
           throw new ProcessingException("ERROR: HashtableXMLMapper no mapping specified, check PROPERTIES.");
        }

      return map;
 }

 
 private static String convertToSEDate(String wordDate) throws ProcessingException {

          String seDate = null;
          //we should check if incoming date is valid at some time.
          try {

		        seDate = DateUtils.getTimeString(wordDate, WORD_FORMAT, SE_FORMAT);
          }

          catch(Exception pe) {
            throw new ProcessingException("Unable to convert incoming date: [" + wordDate + "] to SE Format." );
          }

          return seDate;

  }



  ///////for testing
  public static void main(String[] args) {

    System.gc();
    Runtime.getRuntime().runFinalization();
    System.out.println("Memory: Total = " + Runtime.getRuntime().totalMemory() +
                         " Free = " + Runtime.getRuntime().freeMemory() +
                         " Used = " + (Runtime.getRuntime().totalMemory() -
                                              Runtime.getRuntime().freeMemory()));
    Debug.enableAll();
//    NVPair[] processedResult = null;

    HashtableXMLMapper wMap = new HashtableXMLMapper();

    //Initialize stuff for testing...
    wMap.nextProcessor = "nobody";
    wMap.xmlTemplate = "c:/foc.template";
    wMap.testing = true;


    Hashtable map = new Hashtable();
    map.put("1",  "HEY_JOHNNY");
    map.put("2", "PB_123ABC");
    map.put("3", "ABC123");
    map.put("DD", "13/57/20");


    //Process...
    try {
//      processedResult = wMap.execute(null, map);
        wMap.execute(null, map);
      }

    catch (Exception e) { e.printStackTrace(); }

    Runtime.getRuntime().runFinalization();
    System.out.println("Memory: Total = " + Runtime.getRuntime().totalMemory() +
                         " Free = " + Runtime.getRuntime().freeMemory() +
                         " Used = " + (Runtime.getRuntime().totalMemory() - 
                                              Runtime.getRuntime().freeMemory()));
    System.gc();
    System.out.print("done");

  }
}


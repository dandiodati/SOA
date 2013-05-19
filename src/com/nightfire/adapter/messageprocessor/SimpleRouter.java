/* SimpleRouter class
 * This class generated the RTF document from the XML message as input &
 * RTF template as a reference.
 */
package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import java.util.*;

/**
 * SimpleRouter class.
 * extends MessageProtocolAdapter.
 */
public class SimpleRouter extends MessageProcessorBase {

  private final static String ROUTING_KEY = "ROUTING_KEY";
  private final static String ROUTING_RULE = "ROUTING_RULE";
  private final static String KEY_VALUE = "KEY_VALUE_";
  private final static String ROUTE_TO = "ROUTE_TO_";
  private final static String NEXT_PROCESSOR_NAME = "NEXT_PROCESSOR_NAME";



  private final static String EQUALS = "EQUALS";
  private final static String STARTS_WITH = "STARTS_WITH";
  private final static String ENDS_WITH = "ENDS_WITH";
//  private final static String CONTAINS = "CONTAINS";
  private final static String NODE_EXISTS = "NODE_EXISTS";

  private String nextProcessor = "";
  private String routeKey = "";
  private String routingRule = "";
  private XMLMessageParser xmp = null;

  private Hashtable map = new Hashtable();
  private boolean testing = false;

  public SimpleRouter() {}

  /**
   * This method loads properties in virtual machine.
   * @param key - Ket value of persistentproperty.
   * @param type - Type value of persistentproperty.
   * @exception ProcessingException - this exception is thrown by super class
   *              for any problem with initialization .
   */

  public void initialize ( String key, String type ) throws ProcessingException
  {
      if ( Debug.isLevelEnabled(Debug.MSG_LIFECYCLE) )
        Debug.log( Debug.MSG_LIFECYCLE, "Loading properties for SimpleRouter.");

    super.initialize(key, type);

    nextProcessor = (String)adapterProperties.get(NEXT_PROCESSOR_NAME);
    routeKey = (String)adapterProperties.get(ROUTING_KEY);
    routingRule = (String)adapterProperties.get(ROUTING_RULE);

    if(nextProcessor == null)
    {
       Debug.log( Debug.ALL_ERRORS, "ERROR: SimpleRouter: NEXT_PROCESSOR_NAME property cannot be null.");
       throw new ProcessingException("ERROR: SimpleRouter: NEXT_PROCESSOR_NAME property cannot be null.");
    }

    if(routeKey == null)
    {
       Debug.log( Debug.ALL_ERRORS, "ERROR: SimpleRouter: XML_TEMPLATE property cannot be null.");
       throw new ProcessingException("ERROR: SimpleRouter: MS_APPLICATION_NAME property cannot be null.");
    }

    if(routingRule == null)
    {
       Debug.log( Debug.ALL_ERRORS, "ERROR: SimpleRouter: XML_TEMPLATE property cannot be null.");
       throw new ProcessingException("ERROR: SimpleRouter: MS_APPLICATION_NAME property cannot be null.");
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

//    String xmlAfterTokenReplacement = null;
//    Hashtable routingMap = new Hashtable();
    //XMLMessageParser xmp = null;
    String routeVal = null;
    NVPair nvpair = null;


    if (input == null) return null;


    createRoutingMap();

    if ( !(input instanceof Hashtable) ) {

        xmp = new XMLMessageParser( Converter.getDOM(input) );
      
      //
      if (routingRule.equals(NODE_EXISTS) ) nextProcessor = route(routeKey);

      else {
        if (!xmp.nodeExists(routeKey) ) {
          throw new ProcessingException("ERROR: XMLMulitplexor, input invalid input XML does not contain node: "  + routeKey );
        }

        routeVal = xmp.getValue(routeKey);
        if (routeVal == null)
          throw new ProcessingException("ERROR: SimpleRouter, the node: [" + routeKey + "] does not exist in input XML.");

          if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS, "XMLMulitplexor routing on value [" + routeVal +"].");

          nextProcessor = route(routeVal);
      }

      if (nextProcessor == null)
      throw new ProcessingException("ERROR: SimpleRouter [" + routeVal + "]  does not match any routing rule.");

        if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS, "Routing to: " + nextProcessor);

      nvpair = new NVPair(nextProcessor, input );


    }
    else
    if (input instanceof Hashtable) {
      Hashtable ht = (Hashtable) input;

      routeVal = (String) ht.get(routeKey);
      
      if (routeVal == null)
        throw new ProcessingException("ERROR: SimpleRouter, there is no associated value for key [" + routeKey + "] in input Hashtable.");

        if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
            Debug.log(Debug.MSG_STATUS, "SimpleRouter routing on value [" + routeVal +"].");

      nextProcessor = route(routeVal);

      if (nextProcessor == null)
      throw new ProcessingException("ERROR: SimpleRouter [" + routeVal + "]  does not match any routing rule.");

        if ( Debug.isLevelEnabled(Debug.MSG_LIFECYCLE) )
            Debug.log(Debug.MSG_LIFECYCLE, "Routing to: " + nextProcessor);

      nvpair = new NVPair(nextProcessor, (Hashtable) input );
    }
    else {
      throw new ProcessingException("ERROR: SimpleRouter invalid input expecting String or DOM or Hashtable.  Recieved input type: " + input.getClass() );
    }
    
    NVPair array[] = new NVPair[]{nvpair};
    return array;
  }


  protected void createRoutingMap() throws ProcessingException {

      if (testing) {
      map.put("1",  "Covad.EventChannel" );
      map.put("2", "PacBell.EventChannel" );
      map.put("3", "3Com.EventChannel");
      return;
      }

      String mapKey = null;
      String mapValue = null;
      int i = 1;

      mapKey = (String) adapterProperties.get(KEY_VALUE + String.valueOf(i));
      mapValue = (String) adapterProperties.get(ROUTE_TO + String.valueOf(i++));

      while ( mapKey != null && mapValue != null)
        {
            map.put(mapKey, mapValue );

            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
                Debug.log(Debug.MSG_DATA ,"XMLMulitiplexor,  Adding filter: [" + mapKey  + "," + mapValue + "]");

            mapKey = (String) adapterProperties.get(KEY_VALUE + String.valueOf(i));
            mapValue = (String) adapterProperties.get(ROUTE_TO + String.valueOf(i++));
        }                                                                    


      if ( map.isEmpty() ) {
        Debug.log(Debug.ALL_ERRORS, "ERROR: SimpleRouter no mapping specified.");
        throw new ProcessingException("ERROR: SimpleRouter no mapping specified, check PROPERTIES.");
      }
 }


  private String route(String routeVal) //throws ProcessingException
  {

      // Loop through map keys to find match with the routing value
       if (!routingRule.equals(NODE_EXISTS)){

        for (Enumeration enumerator = map.keys(); enumerator.hasMoreElements(); )
        {
          String key = (String) enumerator.nextElement();

            if ( Debug.isLevelEnabled(Debug.MSG_LIFECYCLE) )
                Debug.log(Debug.MSG_LIFECYCLE, "Routing value: [" + routeVal + "], Key [" + key + "].");

          if (routingRule.equalsIgnoreCase(EQUALS) && routeVal.equals(key) )
              return (String) map.get(key);

          if (routingRule.equalsIgnoreCase(STARTS_WITH) && routeVal.startsWith(key) ) {
              if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
                    Debug.log(Debug.MSG_STATUS, "Using rule: STARTS_WITH");

            return (String) map.get(key);
          }

          if (routingRule.equalsIgnoreCase(ENDS_WITH)  && routeVal.endsWith(key) )
              return (String) map.get(key);
        }

       }

       else {

        for (Enumeration enumerator = map.keys(); enumerator.hasMoreElements(); ){
          String key = (String) enumerator.nextElement();

            if ( Debug.isLevelEnabled(Debug.MSG_DATA) )
                Debug.log(Debug.MSG_DATA, "Routing value: [" + routeVal + "], Key [" + key + "].");

          if (xmp.nodeExists(routeKey + "." + key)) return (String)map.get(key);
        }
       }
       return null;

  }

  ///////for testing
  public static void main(String[] args) {

//    NVPair[] processedResult = null;
    SimpleRouter wMap = new SimpleRouter();
    String xmlInput = null;

    System.gc();
    Runtime.getRuntime().runFinalization();
    System.out.println("Memory: Total = " + Runtime.getRuntime().totalMemory() +
                         " Free = " + Runtime.getRuntime().freeMemory() +
                         " Used = " + (Runtime.getRuntime().totalMemory() -
                                              Runtime.getRuntime().freeMemory()));
    Debug.enableAll();


    //Initialize stuff for testing...
    wMap.testing = true;
    wMap.routeKey = "ResponseHeader.PurchaseOrderNumber";
    wMap.routingRule = "STARTS_WITH";
    
    try { xmlInput = FileUtils.readFile("c:/foc.txt"); }
    catch (FrameworkException fe) { fe.printStackTrace(); }



    //Process...
    try {
//      processedResult = wMap.execute(null, xmlInput);
      wMap.execute(null, xmlInput);
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


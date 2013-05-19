/* RTFGenerator class
 * This class generated the RTF document from the XML message as input &
 * RTF template as a reference.
 */
package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import java.util.StringTokenizer;

/**
 * RTFGenerator class.
 * extends MessageProtocolAdapter.
 */
public class RTFGenerator extends MessageProcessorBase {

  private final static String NEXT_PROCESSOR_NAME = "NEXT_PROCESSOR_NAME";
  private final static String START_DELIMITER           = "START_DELIMITER";
  private final static String END_DELIMITER           = "END_DELIMITER";
//  private final static String TOKEN_PREFIX        = "TOKEN_PREFIX";
  private final static String RTF_TEMPLATE        = "RTF_TEMPLATE";
//  private final static String USER_DIR            = System.getProperty("user.dir")+System.getProperty("file.separator");

  private String nextProcessor = "";
  private String startDelimiter     = "";
  private String endDelimiter     = "";
  private String tokenPrefix   = "";
  private String rtfTemplate   = "";

  private final boolean testing = false;

  public RTFGenerator() {
  }

  /**
   * This method loads properties in virtual machine.
   * @param key - Ket value of persistentproperty.
   * @param type - Type value of persistentproperty.
   * @exception - ProcessingException - this exception is thrown by super class
   *              for any problem with initialization .
   */
  public void initialize ( String key, String type ) throws ProcessingException
  {
    Debug.log( this, Debug.DB_STATUS, "Loading properties for RTF generator.");
    super.initialize(key, type);

    nextProcessor = (String)adapterProperties.get(NEXT_PROCESSOR_NAME);
    startDelimiter     = (String)adapterProperties.get(START_DELIMITER);
    endDelimiter     = (String)adapterProperties.get(END_DELIMITER);
    rtfTemplate   = (String)adapterProperties.get(RTF_TEMPLATE);

    if(nextProcessor == null ||startDelimiter == null || endDelimiter == null || rtfTemplate == null ||
      nextProcessor.equals("") ||startDelimiter.equals("") || endDelimiter.equals("") || rtfTemplate.equals(""))
    {
       Debug.log( this, Debug.ALL_ERRORS, "ERROR: RTFGenerator: One or more RTF generator properties are missing.");
       throw new ProcessingException("ERROR: RTFGenerator: One or more properties from persistentproperty" +
                                     " could not be loaded or are null");
    }


  }

  /**
   * This method is called from driver.
   * It does all processing to generate RTF document.
   * @param context - MessageProcessorContext.
   * @param type - input - this contains XML message that needs to e converted to RTF document.
   * @return NVPair[] - this array contains only one instance of NVPair.
   *                    name - value of NEXT_PROCESSOR_NAME; value - generated RTF document.
   * @exception - MessageException, ProcessingException - messageException is thrown if parsing of XML fails
   *              ProcessingException is thrown for any other significant error.
   */
  public NVPair[] execute ( MessageProcessorContext context, Object input ) throws MessageException, ProcessingException
  {

    Debug.log( this, Debug.BENCHMARK, "RTFGenerator: Starting conversion of XML to RTF document");
    if(input == null)
    {
      return null;
    }

    String xmlMessage = Converter.getString( input );

    TokenDataSource tds = new XMLTokenDataSourceAdapter( xmlMessage );

    String template = getRTFTemplate();
    com.nightfire.framework.util.TokenReplacer tr = new com.nightfire.framework.util.TokenReplacer( template );
    tr.setTokenStartIndicator(startDelimiter);
    tr.setTokenEndIndicator(endDelimiter);
    tr.setAllDataOptional(true);
    String result = "";
    try
    {
      result = tr.generate(tds);
    }
    catch(FrameworkException exp)
    {
      throw new ProcessingException("ERROR: RTFGenerator: Error in translating XML to RTF document");
    }

    Debug.log( this, Debug.BENCHMARK, "RTFGenerator: Done conversion of XML to RTF document");
    //Generate NVPair to return

    NVPair nvpair = new NVPair(nextProcessor, result);
    NVPair array[] = new NVPair[]{nvpair};

    //for testing only
    if(testing)
    {
      try
      {
        FileUtils.writeFile("e:\\OP.rtf",result);
      }
      catch(Exception exp)
      {
         exp.printStackTrace();
      }
    }

    return array;
  }

  /**
   * This method reads RTF template file.
   * @return String - string of rtf template.
   * @exception - ProcessingException - thrown if FrameworkException is reported
   *              from FileUtil class
   */
  private String getRTFTemplate() throws  ProcessingException
  {
     String template = "";
     Debug.log( this, Debug.BENCHMARK, "RTFGenerator: Reading template file.");
     try
     {
       template = FileUtils.readFile(rtfTemplate);
     }
     catch(FrameworkException exp)
     {
        Debug.log( this, Debug.ALL_ERRORS, "ERROR: RTFGenerator: Template file cannot be located.");
        throw new ProcessingException(rtfTemplate + " file cannot be located");
     }

     return template;
  }

  ///////for testing
  public static void main(String[] args) {

    if(args.length != 4)
    {
       System.out.println("usage: java RTFGenerator dtabaseurl dbuser password xmlfile");
       return;
    }
    RTFGenerator rt = new RTFGenerator();
    try
    {
       DBInterface.initialize(args[0],args[1],args[2]);
    }
    catch(Exception exp)
    {
      String e = exp.toString();
      e = exp.getMessage();
      e = "";
    }
    try
    {
      rt.initialize("BS_ORDER","RTF_GENERATOR");
    }
    catch(Exception exp)
    {
       exp.printStackTrace();
    }

    try
    {
    String xmlmessage = FileUtils.readFile(args[3]);

    rt.execute(null,xmlmessage);
    }
    catch(Exception exp)
    {
       exp.printStackTrace();
    }
  }
}


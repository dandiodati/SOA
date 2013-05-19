/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //adapter/com/nightfire/adapter/messageprocessor/Xml2Idl.java#1 $
 */

package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.idl2xml.*;
import com.nightfire.framework.corba.idl2xml.object.*;
import com.nightfire.framework.corba.idl2xml.util.*;
import com.nightfire.framework.message.*;
import java.io.*;

/**
 *
 *  This messageprocessor is responsible for taking the Nightfire Response XML
 *  and converting it to an IDL object. The message processor looks for the
 *  appropriate IDL_CLASS and MAP_FILE, in context or in properties and then
 *  using the converter class the file is converted to IDL and sent to
 *  ObjectDescriber to display the result in the log.
 */

public class Xml2Idl extends MessageProcessorBase {


  /**
  * Property indicating to use context or default.
  */
  public static final String USE_DEFAULT_PROP = "USE_DEFAULT";

  /**
  * Property for the location of the map file in context.
  */
  public static final String MAP_FILE_LOCATION_PROP = "MAP_FILE_LOCATION";

  /**
  * Property for the location of the idl class in context.
  */
  public static final String IDL_CLASS_LOCATION_PROP = "IDL_CLASS_LOCATION";

  /**
  * Property indicating the IDL class to which the response XML is to be converted to.
  */
  private final static String DEFAULT_IDL_CLASS_PROP = "DEFAULT_IDL_CLASS";

  /**
  * Property indicating the map file which the converter class uses to convert
  * the response XML to IDL.
  */
  private final static String DEFAULT_MAP_FILE_PROP = "DEFAULT_MAP_FILE";

  /**
  * (OPTIONAL) Property for the location of the converted message representation in context.
  */
  public static final String CONVERTED_MESSAGE_LOCATION_PROP = "CONVERTED_MESSAGE_LOCATION";

  /**
  * (OPTIONAL) Property (either true or false) that sets up whitespace initialization.
  */
  public static final String USE_WHITESPACE_CHAR_INITIALIZATION_PROP = "USE_WHITESPACE_CHAR_INITIALIZATION";

  /**
  * The converted message location
  */
  private String convertedMessageLocation = null;

  /**
  * The IDL class file obtained from persistent properties which the XML is to be
  * converted to.
  */
  private String mIDLClass = null;

  /**
  * The map file obtained from persistent properties indicating which map file
  * the converter class should use.
  */
  private String mMapFile = null;

  /**
  * The location of class file in context obtained from persistent properties.
  */
  private String mClassLocation = null;

  /**
  * The location of map file in context obtained from persistent properties.
  */
  private String mMapLocation = null;

  /**
  * Boolean value indicating to use default or context to locate map file and
  * class file, obtained from persistent properties.
  */
  private boolean useDefault = true;

  /**
  * Boolean value whether to call ObjInitializer.setEmptyChar(); to set
  * whitespace chars.
  */
  private boolean useWhiteSpace = false;

  /*
  * The cache contains MappingBuilder objects.
  */
  private static FileCache mappingCache = new FileCache();



  /** Creates new Xml2Idl */
  public Xml2Idl() {
  }

  /**
  * Called to initialize this component.
  *
  * @param  key   Property-key to use for locating initialization properties.
  * @param  type  Property-type to use for locating initialization properties.
  * @exception  ProcessingException  Thrown if processing fails.
  */
  public void initialize(String key, String type) throws ProcessingException {
      super.initialize(key, type);

      // A buffer to hold error messages if any of the required properties
      // are missing.
      StringBuffer errorMessages = new StringBuffer();

      try{
        useDefault = StringUtils.getBoolean(getRequiredPropertyValue(USE_DEFAULT_PROP, errorMessages));
        String whiteSpaceValue = getPropertyValue(USE_WHITESPACE_CHAR_INITIALIZATION_PROP);
        if (whiteSpaceValue != null){
          useWhiteSpace = StringUtils.getBoolean(whiteSpaceValue);
        }
      }
      catch(Exception e){
            errorMessages.append("An error has occured while getting the boolean value: "+ e.getMessage());
      }

      if(useDefault){
        mIDLClass = getRequiredPropertyValue(DEFAULT_IDL_CLASS_PROP);
        mMapFile = getRequiredPropertyValue(DEFAULT_MAP_FILE_PROP);
      }
      else{
        mClassLocation = getRequiredPropertyValue(IDL_CLASS_LOCATION_PROP);
        mMapLocation = getRequiredPropertyValue(MAP_FILE_LOCATION_PROP);
      }

      convertedMessageLocation = getPropertyValue(CONVERTED_MESSAGE_LOCATION_PROP);

      // If there were any missing properties, throw the exception now.
      if(errorMessages.length() > 0){
         throw new ProcessingException("An error has occured on initialization: "+errorMessages.toString());
      }
  }

  /**
  * This proccesor finds the appropriate class file and map file to use to map
  * the XML to IDL. Then the processor finds the MAP_FILE, and the IDL_CLASS
  * that associates with the XML from the persistent properties or the context.
  * Next the processor creates an Xml_mapper, an IDL_mapper, and mapper for use
  * with the converter. The converter converts the XML to an object of type IDL_CLASS.
  * Then the resulting object is displayed using the ObjectMapGenerator class. The object
  * is then passed to ImportToMetaSolv. The resulting IDL Object will be logged in
  * the gateway log.
  *
  * @param context The current context for the process within which this
  * processor is being executed. This context that may contain the map file and the
  * idl class if the default is not used.
  * @param input The input MessageObject containing the xml response that is to be converted.
  * @return  An NVPair array containing a single NVPair. The pair will include
  * the value of the mNextProcessor and the resulting IDL file.
  * @exception  ProcessingException  Thrown if processing fails
  */
  public NVPair[] process(MessageProcessorContext context, MessageObject input)
      throws MessageException, ProcessingException
  {

    if ( input == null){
      return null;
    }

    //declaring variables for use of converter

    Object resultObj = null;
    ObjectMapper idlMapper = null;
    XMLMapper xmlMapper = null;
    Reader reader = null;
    String description = null;

    try{

      if( !useDefault ){
        mMapFile = context.getString(mMapLocation);
        mIDLClass = context.getString(mClassLocation);
      }

      try {
          if ( useWhiteSpace ){
            ObjInitializer.setEmptyChar();
          }
          resultObj = ObjInitializer.newInstance(Class.forName(mIDLClass));
      }
      catch (ClassNotFoundException e) {
          Debug.log(Debug.ALL_ERRORS,"Error while trying to initialize object of class: " + mIDLClass + ". " + e.toString());
          Debug.logStackTrace(e);
          throw new ProcessingException("Error while trying to initialize object of class: " + mIDLClass + ". " + e.toString());
      }

        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Creating idl mapper, xml mapper and map reader ..." );

      // create a idl mapper for it
      idlMapper = new ObjectMapper(resultObj);

      // create a xml mapper for it
      xmlMapper = new XMLMapper( input.getDOM());

      // create a reader for the map file using mappingCache
      String fileContent = (String)mappingCache.getObject(mMapFile);
      reader = new StringReader(fileContent);

    }
    catch(FrameworkException fe){
        throw new ProcessingException("Error creating mappers for converter" + fe.getMessage() );
    }

    try{
      // create a converter to do the work
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Creating Converter ..." );

      com.nightfire.framework.corba.idl2xml.Converter converter
        = new com.nightfire.framework.corba.idl2xml.Converter(idlMapper, xmlMapper, reader);

      // do the conversion
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "converting ...");

      converter.convert();

      if ( (convertedMessageLocation != null) || Debug.isLevelEnabled(Debug.MSG_STATUS) ){
        description = ObjectMapGenerator.describe(resultObj);
      }

      // if putting the converted message in context
      if(convertedMessageLocation != null){
        context.set(convertedMessageLocation, description);
      }

      // display the results
      if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) ){
        Debug.log(Debug.MSG_STATUS, "Result Object: \n" + description);
      }
    }
    catch (Exception e){
        Debug.error( "XML<->IDL Conversion failed: " + e.toString() + "\n" + Debug.getStackTrace( e ) );
      throw new ProcessingException("Error occured during conversion: " + e.toString());
    }
    finally{
      try{
        reader.close();
      }
      catch(IOException ioex){
        Debug.log(Debug.ALL_ERRORS, "Could not close reader: "+ ioex.getMessage() );
      }
    }

      if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
        Debug.log( Debug.MSG_LIFECYCLE, "Successful xml to idl conversion!" );

    return formatNVPair(resultObj);
  }

}

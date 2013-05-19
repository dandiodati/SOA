/**
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //adapter/com/nightfire/adapter/messageprocessor/Xml2Xml.java#1 $
 */

package com.nightfire.adapter.messageprocessor;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.corba.idl2xml.*;
import com.nightfire.framework.message.*;
import java.io.*;
import org.w3c.dom.*;

/**
 *
 *  This messageprocessor is responsible for taking the response xmls
 *  and populate them will values from xml in context.
 *
 */

public class Xml2Xml extends MessageProcessorBase {


  /**
  * Property indicating to use context or default.
  */
  public static final String USE_DEFAULT_PROP = "USE_DEFAULT";

  /**
  * Property for the location of the map file in context.
  */
  public static final String MAP_FILE_LOCATION_PROP = "MAP_FILE_LOCATION";

  /**
  * Property indicating the map file which the converter class uses to convert
  * the response XML to IDL.
  */
  private final static String DEFAULT_MAP_FILE_PROP = "DEFAULT_MAP_FILE";

  /**
  * Property indicating location of the from xml.
  */
  private final static String FROM_XML_LOCATION_PROP = "FROM_XML_LOCATION";

  /**
  * The map file obtained from persistent properties indicating which map file
  * the converter class should use.
  */
  private String mMapFile = null;

  /**
  * The location of map file in context obtained from persistent properties.
  */
  private String mMapLocation = null;

  /**
  * The location of the from xml.
  */
  private String mXmlLocation = null;

  /**
  * Boolean value indicating to use default or context to locate map file and
  * class file, obtained from persistent properties.
  */
  private boolean useDefault = true;

  /*
  * The cache contains MappingBuilder objects.
  */
  private static FileCache mappingCache = new FileCache();



  /** Creates new Xml2Idl */
  public Xml2Xml() {
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
      }
      catch(Exception e){
            errorMessages.append("An error has occured while getting the USE_DEFAULT value: "+ e.getMessage());
      }

      if(useDefault){
        mMapFile = getRequiredPropertyValue(DEFAULT_MAP_FILE_PROP);
      }
      else{
        mMapLocation = getRequiredPropertyValue(MAP_FILE_LOCATION_PROP);
      }

      mXmlLocation = getRequiredPropertyValue(FROM_XML_LOCATION_PROP);

      // If there were any missing properties, throw the exception now.
      if(errorMessages.length() > 0){
         throw new ProcessingException("An error has occured on initialization: "+errorMessages.toString());
      }
  }

  /**
  * This proccesor finds the appropriate map file to use to map the XML to XML.
  * Then the processor finds the MAP_FILE, and the FROM_XML locatation.
  * Next the processor creates an toXml_mapper, a fromXml_mapper, and mapper for use
  * with the converter. The converter converts maps the fromXml to the toXML.
  *
  * @param context The current context for the process within which this
  * processor is being executed. This context that may contain the map file and the
  * from xml if the default is not used.
  * @param input The input MessageObject containing the xml response that is to be converted.
  * @return  An NVPair array containing a single NVPair. The pair will include
  * the value of the mNextProcessor and the resulting xml file.
  * @exception  ProcessingException  Thrown if processing fails
  */
  public NVPair[] process(MessageProcessorContext context, MessageObject input)
      throws MessageException, ProcessingException
  {

    if ( input == null){
      return null;
    }

    //declaring variables for use of converter

    String resultXml = null;
    XMLMapper fromMapper = null;
    XMLMapper toMapper = null;
    Reader reader = null;

    try{

      if( !useDefault ){
        mMapFile = context.getString(mMapLocation);
      }

      Document fromXml = context.getDOM(mXmlLocation);

        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log( Debug.MSG_STATUS, "Creating from xml mapper, to xml mapper and map reader ..." );

      // create a xml mapper for it
      fromMapper = new XMLMapper( fromXml);

      // create a xml mapper for it
      toMapper = new XMLMapper( input.getDOM());

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
        = new com.nightfire.framework.corba.idl2xml.Converter(toMapper, fromMapper, reader);

      // do the conversion
        if (Debug.isLevelEnabled(Debug.MSG_STATUS))
            Debug.log(Debug.MSG_STATUS, "converting ...");

      if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
        Debug.log(Debug.MSG_DATA, "Input XML: \n" + input.getString());
      }

      converter.convert();

      resultXml = toMapper.getDoc();

      // display the results
      if ( Debug.isLevelEnabled( Debug.MSG_DATA ) ){
        Debug.log(Debug.MSG_DATA, "Result XML: \n" + resultXml);
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

      if (Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log( Debug.MSG_STATUS, "Successful xml to idl conversion!" );

    return formatNVPair(resultXml);
  }

}

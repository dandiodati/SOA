/*
 * Idl2Xml.java
 *
 * $Header: //adapter/com/nightfire/adapter/messageprocessor/Idl2Xml.java#1 $
 *
 * Created on March 5, 2001, 2:40 PM
 */

package com.nightfire.adapter.messageprocessor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.*;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.corba.idl2xml.*;
import com.nightfire.framework.corba.idl2xml.util.*;
import com.nightfire.framework.message.*;


/**
 *
 *
 *  This processor is responsible for transforming the incoming Object into XML.
 *  Also, it will put a the NF request type under key NF_HEADER_REQUEST
 *  in the context if the NF_REQUEST_TYPE_PROP is specified in the Properties.
 *
 *  Todo:  We should cache the map and xml template using FileCache
 */

public class Idl2Xml extends MessageProcessorBase {

    /**
     *  The fully qualified class name to be supported
     */
    private final static String CLASS_NAME_PROP = "CLASS_NAME";

    /**
     *  The absolute or relative path to the xml template to be used for an associated CLASS_NAME
     */
    private final static String XML_TEMPLATE_PROP = "XML_TEMPLATE";

    /**
     *  The absolute or relative path to the IDL to XMl map file
     */
    private final static String MAP_FILE_TAG_PROP = "MAP_FILE";


    /**
     *  (OPTIONAL) The NF Request Type mapped to associated CLASS_NAME.
     */
    private final static String NF_REQUEST_TYPE_PROP = "NF_REQUEST_TYPE";

    /**
     *  The key name in the context to put the associated NF request type
     */
    private final static String RESULT_LOCATION_PROP = "RESULT_CONTEXT_LOCATION";

    /**
     *  (OPTIONAL) Location to write the IDL string representation to.
     */
    private final static String IDL_DESCRIPTION_DIRECTORY_PROP = "IDL_DESCRIPTION_DIRECTORY";

    /**
     *  (OPTIONAL) Prefix of the IDL string representation file.
     */
    private final static String IDL_DESCRIPTION_PREFIX_PROP = "IDL_DESCRIPTION_PREFIX";

    private static FileCache mapCache = new FileCache();
    private static FileCache xmlTemplateCache = new FileCache();

    /**
     *  A linked list to hold the iterative properties
     */
    private LinkedList iterativePropList = new LinkedList();

    /**
     *  A Vector to ensure that CLASS_NAME appears only once in an iterative collection
     */
    private HashMap keys = new HashMap();
    private String resultLocation = null;
    private String idlDescDirectory = null;
    private String prefix = null;

     /**
     * This is helper class that holds the iterative properties.
     */
    private static class PropDataNode {
        public final String idlType;
        public final String xmlTemplate;
        public final String mapFile;
        public final String requestType;

        public PropDataNode(String idlType, String xmlTemplate, String mapFile, String requestType) {
            this.idlType = idlType;
            this.xmlTemplate = xmlTemplate;
            this.mapFile = mapFile;
            this.requestType = requestType;
        }

        public String describe() {
            StringBuffer sb = new StringBuffer();

            sb.append("Iterative Property description: ");

            if ( StringUtils.hasValue(idlType) )
                sb.append("Idl Type [" + idlType + "]\n");
            if ( StringUtils.hasValue(xmlTemplate) )
                sb.append("XML Template [" + xmlTemplate + "]\n");
            if ( StringUtils.hasValue(mapFile) )
                sb.append("Map File [" + mapFile + "]\n");
            if ( StringUtils.hasValue(requestType) )
                sb.append("Associated NF Request Type [" + requestType + "]\n");
            return sb.toString();
        }
    }


   /** Creates new Idl2Xml */
    public Idl2Xml() {
    }

    /**
     * Initializes this object.
     *
     * @param  key   Property-key to use for locating initialization properties.
     *
     * @param  type  Property-type to use for locating initialization properties.
     *
     * @exception ProcessingException when initialization fails
     */
    public void initialize(String key, String type) throws ProcessingException {
        super.initialize(key, type);
        StringBuffer errorBuf = new StringBuffer();

        for (int counter = 0; true; counter++)
        {
            String theTag;
            String idlType;
            String xmlTemplate;
            String mapFile;
            String requestType;

            theTag = PersistentProperty.getPropNameIteration(CLASS_NAME_PROP, counter);
            idlType = getPropertyValue(theTag);


            if ( !StringUtils.hasValue( idlType ) )
            {
                // end of the iteration so break
                break;
            }

            theTag = PersistentProperty.getPropNameIteration(XML_TEMPLATE_PROP, counter);
            xmlTemplate = getRequiredPropertyValue(theTag, errorBuf);

            theTag = PersistentProperty.getPropNameIteration(MAP_FILE_TAG_PROP, counter);
            mapFile = getRequiredPropertyValue(theTag, errorBuf);

            theTag = PersistentProperty.getPropNameIteration(NF_REQUEST_TYPE_PROP, counter);
            //these property is optional
            requestType = getPropertyValue(theTag);
            idlDescDirectory = getPropertyValue(IDL_DESCRIPTION_DIRECTORY_PROP);
            prefix = getPropertyValue(IDL_DESCRIPTION_PREFIX_PROP);

            if ( StringUtils.hasValue(requestType) )
                resultLocation = getRequiredPropertyValue(RESULT_LOCATION_PROP, errorBuf);


            //if the IDL Type is specified twice then throw an exception
            if ( keys.containsKey(idlType) )
                throw new ProcessingException("Configuration error IDL TYPE [" + idlType + "] already specified.");

            keys.put(idlType, "" );
            iterativePropList.add( new PropDataNode(idlType, xmlTemplate, mapFile, requestType) );
        }

        if ( errorBuf.length() > 0 ) throw new ProcessingException(errorBuf.toString() );

    } //end initialize

    /**
     * If the incoming object is an instance of a supported CLASS_NAME, optionally put NF Request Type
     * in context, convert from Object to XML.
     *
     * @param  input  MessageObject containing the value to be processed
     *
     * @param  context The context
     *
     * @return  Optional NVPair containing a Destination name and a MessageObject,
     *          or null if none.
     *
     * @exception  ProcessingException  Thrown if processing fails.
     *
     * @exception  MessageException  Thrown if bad message.
     */
    public NVPair[] process(MessageProcessorContext context, MessageObject input)
    throws MessageException, ProcessingException
    {
        if (input == null ) return null;
        Object inputObj = input.get();
        Class theInputClass = inputObj.getClass();
        try {

            Iterator iter = iterativePropList.iterator();

            while (iter.hasNext() ) {

                PropDataNode pData = (PropDataNode) iter.next();

                Class pType = Class.forName(pData.idlType);
                //Object obj = ObjectFactory.create(pData.idlType);

                  if ( pType.isInstance(inputObj) ) {
                //if ( theInputClass.isInstance(obj) ) {

                    // put request type in context if it has a value
                    if ( StringUtils.hasValue(pData.requestType) )
                      set(resultLocation, context, input, pData.requestType );

                return formatNVPair( runIdl2Xml(pData.xmlTemplate, inputObj,  pData.mapFile ) );
                }
            }
            //else we throw an Exception
            throw new MessageException("Input Class [" + theInputClass + "] is either not supported or property value is misconfigured");

        } 
        catch (ClassNotFoundException cnfe) {
            throw new ProcessingException("Unknown input type, check CLASSPATH" + cnfe.getMessage() );
        }

    }

     /**
     * Parse an Expression and build XML representation of the Expression
     *
     * @param theXmlTemplate the XmlTemplate to be used to create the XMLMapper
     * @param theMap the path to the map file which defines the Object to XML mapping
     * @param idl the Object being mapped
     *
     * @return an XML String representation of the expression
     * @exception ProcessingException if anything goes wrong
     */
    private String runIdl2Xml(String theXmlTemplate, Object idl, String theMap ) throws ProcessingException
    {
//        String xml = null;
        Reader reader = null;
        String resultXML = null;
        String idlDesc = null;

        try {

            if ( (idlDescDirectory != null) || Debug.isLevelEnabled(Debug.MSG_STATUS) ){
              idlDesc = ObjectMapGenerator.describe(idl);
            }

              if( idlDescDirectory != null)
                try{

                    if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                        Debug.log(Debug.MSG_LIFECYCLE, "Writing IDL to file at : " + idlDescDirectory);

                  if ( prefix != null)
                    idlDescDirectory  = idlDescDirectory + File.separator + prefix;
                  else
                    idlDescDirectory = idlDescDirectory + File.separator;

                  idlDescDirectory = idlDescDirectory + DateUtils.getDateToMsecAsString();
                  FileUtils.writeFile(idlDescDirectory, idlDesc, false);
                }
                catch( FrameworkException fe){
                  throw new ProcessingException("Unable to write IDL description to path: " + idlDescDirectory );
                }

            if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
                Debug.log(Debug.MSG_STATUS, "Input object [" + idlDesc + "]");

            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, "Creating object mapper ...");

            ObjectMapper idlMap = new ObjectMapper(idl);


            // create a mapper for it
            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, "Creating xml mapper ...");

            XMLMapper xmlMap = new XMLMapper( xmlTemplateCache.get(theXmlTemplate) ) ;

            try{
                reader = new StringReader( mapCache.get(theMap) );
            }
            catch( FrameworkException fe ){
                throw new ProcessingException("Could not initialize reader from cache: "+ fe.getMessage());
            }

            // create a converter to do the work
            if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
                Debug.log(Debug.OBJECT_LIFECYCLE, "Creating Converter ...");

            com.nightfire.framework.corba.idl2xml.Converter converter
            = new com.nightfire.framework.corba.idl2xml.Converter(xmlMap, idlMap, reader );

            // do the conversion
            if(Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
                Debug.log(Debug.MSG_LIFECYCLE, "converting ...");

            converter.convert();

            // display the results
            resultXML =  xmlMap.getDoc();

            //close the reader....
            reader.close();

        } catch (Exception e){
            Debug.error( "XML<->IDL Conversion failed: " + e.toString() + "\n" + Debug.getStackTrace( e ) );

            throw new ProcessingException("Caught exception: " + e.getMessage());
        }

        if ( Debug.isLevelEnabled(Debug.MSG_STATUS) )
          Debug.log(Debug.MSG_STATUS, "Result: " + resultXML);
        return resultXML;
    }
}

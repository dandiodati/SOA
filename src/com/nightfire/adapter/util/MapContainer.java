/**
 * source file : MapContainer.java
 *
 * This class stores the mappings between fixed format fields(eg NENA) & xml nodes,
 * and provides various methods to get information about  xml node , when its
 * equivalent fixed format field is passed  or vice versa
 *
 * @author  Kalyani
 *
 * @Version  1.00
 */

package com.nightfire.adapter.util;

import java.util.*;
import com.nightfire.framework.message.mapper.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import org.w3c.dom.*;

public class MapContainer implements MessageMapper
{
    /**
    This is the default constructor.
     */
    public MapContainer()
    {
    }

    /**
     * This is the constructor.It is used to initialize & fill in the hashtables
     *   with appropriate mappings.
     *
     *  @param   direction - if true -> mappings for xml to fixed format are to be loaded
     *                       if false -> mappings for fixed format to xml are to be loaded
     *  @param   mapString - the string containing appropriate mappings.
     */
    public MapContainer( boolean direction , String mapString )
    {

        this.MappingFromXML = direction ;
        nameMap = new Hashtable();
        nameFormatMap = new Hashtable();
        FixedFormat format = null;

        if ( MappingFromXML ) // the direction is the request direction
        {
            StringTokenizer stLine = new StringTokenizer(mapString, "\n");

            try
            {
                while (stLine.hasMoreElements())
                {
                    StringTokenizer stElement = new StringTokenizer(stLine.nextToken(), ",");

                    while (stElement.hasMoreElements())
                    {
                        String XMLNode = stElement.nextToken();
                        String NENAKey = stElement.nextToken();
                        addNameMapPair( XMLNode , NENAKey );
                        format = new FixedFormat(Integer.parseInt(stElement.nextToken())
                                            ,Integer.parseInt(stElement.nextToken())
                                            ,stElement.nextToken(),StringUtils.getBoolean(stElement.nextToken()));
                        addNameFormatMapPair( NENAKey , format);
                    } // end of inner while
               
                } // end of outer while
            }
            catch(FrameworkException e)
            {
                Debug.log(this,Debug.MAPPING_WARNING,"Warning: unable to convert 'required' string to boolean.");
            }
            
        }
        else // the direction is the response direction
        {

           // mapString = getResponseMapping(type);
            StringTokenizer stLine = new StringTokenizer(mapString, "\n");

            while (stLine.hasMoreElements())
            {
                StringTokenizer stElement = new StringTokenizer(stLine.nextToken(), ",");

                while (stElement.hasMoreElements())
                {
                    String NENAKey = stElement.nextToken();
                    try
                    {
                        addNameMapPair( NENAKey , stElement.nextToken() );
                    }
                    catch (NoSuchElementException nex)
                    {
                        break;
                    }
                    format = new FixedFormat(Integer.parseInt(stElement.nextToken())
                                            ,Integer.parseInt(stElement.nextToken())
                                            ,stElement.nextToken(),false);
                    addNameFormatMapPair( NENAKey , format);
                } // end of inner while
                //System.out.println();
            } // end of outer while
        }
      } // end constructor

   
   
    /**
     * This method is used to add a pair of  nena field - xml node to the hashtable  nameMap
     *
     * @param  sourceName - gets stored as value in the hashtable
     * @param  targetName - gets stored as key in the hashtable
     */
    public void addNameMapPair ( String sourceName, String targetName )
    {
        Debug.log( this, Debug.MAPPING_DATA, "Adding pair mapping source name [" + sourceName +
                   "] maps to target name [" + targetName + "]" );

        String test = (String)nameMap.put( targetName , sourceName );

        if ( test != null )
        {
            Debug.log( this, Debug.ALL_WARNINGS, "WARNING: Target Name [" +
                       targetName + "] already maps to source name [" + test +
                       "].\nReplacing it with new mapping." );
        }
    } //end addNameMapPair()

    /**
     * This method is used to set the source for named value mapping , for the case where
     * this MapContainer is used in request cycle.
     *
     * @param source - MessageDataSource -> here source is the object of XMLParser
     * @return - void
     */
  	public void setSource ( MessageDataSource source ) throws MessageMapperException
    {

        if (source==null) 
            throw new MessageMapperException( "ERROR: MapContainer : source  "
                                            + "can't be null ");
        this.source = source;
    }

    /**
     * This method is used to set the source for named value mapping , for the case where
     * this MapContainer is used in response cycle.
     *
     * @param source - String -> here source is Fixed Format Message
     * @return - void
     */
    public void setFixedFormatSource ( String source ) throws MessageMapperException
    {

          if( (source==null) || (source.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : Fixed format source  "
                                            + "can't be empty/null ");
          this.FixedFormatSource = source;
    }


    /**
     * This method is used to log the current target-source mappings.
     *
     * @return - void
     */
  	public void log ( )
    {
        Enumeration keys = nameMap.keys( );

        Debug.log( this, Debug.MAPPING_DATA,
            "------------------------------ BEGIN MAPPINGS ---------------------------------------" );

        while ( keys.hasMoreElements() )
        {
            String target = (String)keys.nextElement( );

            String source = (String)nameMap.get( target );

            Debug.log( this, Debug.MAPPING_DATA, "Map [" + source + "] to [" + target + "]" );
        }

        Debug.log( this, Debug.MAPPING_DATA,
            "------------------------------- END MAPPINGS ----------------------------------------" );
    }  //end log()

    /**
     * This method is used to add a pair of nena field & corresponding FixedFormat object to the
     *  hashtable nameFormatMap
     *
     * @param targetName - this is a fixed format field name
     * @param format - Object of FixedFormat class , corresponding to field.
     * @return - void
     */
  	public void addNameFormatMapPair ( String targetName , FixedFormat format )
    {
        String test = (String)nameFormatMap.put( targetName , format );

        if ( test != null )
        {
            Debug.log( this, Debug.ALL_WARNINGS, "WARNING: Target Name [" +
                       targetName + "] already maps to one format " +
                       "\nReplacing it with new mapping." );
        }
    } // end addNameFormatMapPair()


    /**
     * This method returns the source name(i.e. value) associated with the
     * targetName(key) from hashtable nameMap
     *
     * @param  targetName - the key , whose value is to be found out.
     * @return String - the value corresponding to the key.
     * @exception - throws  MessageMapperException if value is not found
     */
  	public String getMappedValue ( String targetName) throws MessageMapperException
    {
        if( (targetName==null) || (targetName.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : getMappedValue : "
                                            + "targetName can't be empty/null ");

        // Get the source name corresponding to the target name.
        String srcName = (String)nameMap.get( targetName );

        if ( srcName == null )
        {

                throw new MessageMapperException( "Error: No value corresponding to [" +
                                                  targetName + "]." );

        }
        return srcName;

    } // end getMappedValue()
    
    /**
     * This method finds out the source name(i.e value ) associated with the
     * targetName(i.e. key ) & returns its actual value from source or FixedFormatSource
     *
     * @param   targetName - the key
     * @return  value(present in the source) of the value corresponding to the key.
     * @exception throws MessageMapperException if value is not found in hashtable
     *  or in source.
     */
  	public String getValue ( String targetName ) throws MessageMapperException
    {
        if( (targetName==null) || (targetName.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : getValue : "
                                            + "targetName can't be empty/null ");

        // Get the source name corresponding to the target name.
        String srcName = (String)nameMap.get( targetName );
        if ( srcName == null )
        {
            throw new MessageMapperException( "Error: MapContainer : getValue : "+
              " No value corresponding to [" + targetName + "]." );
        }

        Debug.log( this, Debug.MAPPING_DATA, "Target name [" + targetName +
                   "] maps to source name [" + srcName + "]" );

        if ( MappingFromXML )    //direction is the request direction
        {

            //throw an exception if source is not set.
            if ( source == null )
            {
                throw new MessageMapperException("ERROR: Message mapper wasn't given a data source." );
            }

            // Get the value from the source with the given name and return it.
            try
            {
                return( source.getValue( srcName ) );
            }
            catch ( MessageException me )
            {
                throw new MessageMapperException( "WARNING: No data value corresponding to source name [" +
                                              srcName + "], target name [" + targetName + "]." );
            }
        }
        else                     //direction is the response direction
        {
            
            return getFixedFormatValue( srcName );

        }

    } // end getValue()

    /**
     * This method is not provided by this class  , throws exception if still used.
     */
    public String getValue ( MessageContext msg , String targetName ) throws MessageMapperException
    {

        throw new MessageMapperException( "Error : XMLNENAMapContainer : Method getValue "+
            "( MessageContext msg , String targetName ) is not supported in class XMLNENAMapContainer. " );

    }

    /**
     * This method finds out the FixedFormat object(i.e.value) associated with
     * the targetName(i.e. key ) from hashtable nameFormatMap
     * & extracts value of 'position' from it & returns it.
     *
     * @param targetName - the key , whose position is to be found out.
     *                     This is a fixed format field name
     * @return starting position of fixed format field
     * @exception throws  MessageMapperException if value correspoding to
     *  the targetName is not found
     */
    public int getPosition (String targetName ) throws MessageMapperException
    {

        if( (targetName==null) || (targetName.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : getPosition : "
                                            + "targetName can't be empty/null ");

        // Get the format corresponding to the target name.
        FixedFormat format = (FixedFormat)nameFormatMap.get( targetName );

        if ( format == null )
        {

                throw new MessageMapperException( "Error: No format corresponding to [" +
                                                  targetName + "]." );

        }
        // Get the position from the format object and return it.
        return format.getPosition();
    } // end getPosition()

    /**
     * This method finds out the FixedFormat object(i.e.value) associated with
     * the targetName(i.e. key ) from hashtable nameFormatMap
     * & extracts value of 'size' from it & returns it.
     *
     * @param targetName - the key , whose position is to be found out.
     *                     This is a fixed format field name
     * @return - size of fixed format field
     * @exception throws  MessageMapperException if value correspoding to
     *  the targetName is not found
     */
    public int getSize (String targetName ) throws MessageMapperException
    {
        if( (targetName==null) || (targetName.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : getSize : "
                                            + "targetName can't be empty/null ");
        // Get the format corresponding to the target name.
        FixedFormat format = (FixedFormat)nameFormatMap.get( targetName );

        if ( format == null )
        {

                throw new MessageMapperException( "Error: No format corresponding to [" +
                                                  targetName + "]." );

        }
        // Get the size from the format object and return it.
        return format.getSize();
    } // end getSize()

    /**
     * This method finds out the FixedFormat object(i.e.value) associated with
     * the targetName(i.e. key ) from hashtable nameFormatMap
     * & extracts value of 'type' from it & returns it.
     *
     * @param targetName - the key , whose position is to be found out.
     *                     This is a fixed format field name
     * @return - type of fixed format field
     * @exception throws  MessageMapperException if value correspoding to
     *  the targetName is not found
     */
    public String getType (String targetName ) throws MessageMapperException
    {
        if( (targetName==null) || (targetName.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : getType : "
                                            + "targetName can't be empty/null ");
        // Get the format corresponding to the target name.
        FixedFormat format = (FixedFormat)nameFormatMap.get( targetName );

        if ( format == null )
        {

                throw new MessageMapperException( "Error: No format corresponding to [" +
                                                  targetName + "]." );

        }
        // Get the type from the format object and return it.
        return format.getType();
    } // end getType

    /**
     * This method finds out the FixedFormat object(i.e.value) associated with
     * the targetName(i.e. key ) from hashtable nameFormatMap
     * & extracts value of 'required' from it & returns it.
     *
     * @param targetName - the key , whose position is to be found out.
     *                     This is a fixed format field name
     * @return - true if fixed format field is a required field
     *          false if fixed format field is not required field
     * @exception throws  MessageMapperException if value correspoding to
     *  the targetName is not found
     */
    public boolean isRequired (String targetName ) throws MessageMapperException
    {
        if( (targetName==null) || (targetName.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : targetName : "
                                            + "targetName can't be empty/null ");
        // Get the format corresponding to the target name.
        FixedFormat format = (FixedFormat)nameFormatMap.get( targetName );

        if ( format == null )
        {

                throw new MessageMapperException( "Error: No format name corresponding to [" +
                                                  targetName + "]." );

        }
        // Get the value of 'required' field from the format object and return it.
        return format.isRequired();
    } // end isRequired

    /**
     * This method returns all the keys present in the hashtable nameMap.
     *  this is used by transformers while converting the message from one format to another.
     *
     * @return Enumeration  - enumeration of the keys in the hashtable nameMap.
     */
    public Enumeration getKeys( )
    {
        return( nameMap.keys() );

    } // end getKeys

    /**
     * This method finds out the source name(i.e. value ) associated with
     * the targetName( i.e. key ) & returns its count of children .
     * this method is supported only when this MapContainer is used in request
     * cycle ,i.e. XML to fixed format conversion
     *
     * @param - targetName - the key
     * @return - child count of the value associated with key.
     * @exception throws MessageException if unable to get child count
     * @exception throws MessageMapperException if this method is used in case
     * of mapping from fixed format to XML
     */
    public int getChildCount ( String targetName) throws MessageException , MessageMapperException
    {

        if( (targetName==null) || (targetName.trim().equals ("") ))
            throw new MessageMapperException( "ERROR: MapContainer : getChildCount : "
                                            + "targetName can't be empty/null ");
        if ( !MappingFromXML )    //direction is the response direction
        {
            throw new MessageMapperException( "The method getChildCount is not supported for" +
                                              " Fixed Format Source." );
        }
        if ( source == null )
        {
            Debug.log( this, Debug.ALL_ERRORS, "ERROR: Message mapper wasn't given a data source." );

            return( -1 );
        }

        // Get the source name corresponding to the target name.
        String srcName = (String)nameMap.get( targetName );

        if ( srcName == null )
        {
                return( -1 );
        }
        Debug.log( this, Debug.MAPPING_DATA, "Getting child count of target name [" + targetName +
                   "], which maps to source name [" + srcName + "]" );
        return( source.getChildCount( srcName ) );
    } // end getChildCount()

    /**
     * This method is not provided by this class  , throws exception if still used.
     */
    public int getChildCount ( MessageContext msg , String targetName) throws MessageException
    {
        throw new MessageMapperException( "Error : XMLNENAMapContainer : Method getChildCount "+
            "( MessageContext msg , String targetName ) is not supported in class XMLNENAMapContainer. " );

    }

    /**
     * This method finds out the source name(i.e. value ) associated with
     * the targetName( i.e. key ) &
     * returns true if present in the source(parser) , false otherwise.
     * this method is supported only when this MapContainer is used in request
     * cycle ,i.e. XML to fixed format conversion
     *
     * @param - targetName - the key
     * @return - true if the value associated with key found in the source(parser)
     *           false if the value associated with key not found in the source(parser).
     * This method logs an error message if it is used in case
     * of mapping from fixed format to XML
     */
    public boolean exists ( String targetName)
    {

        if( (targetName==null) || (targetName.trim().equals ("") ))
            return false;

        if ( !MappingFromXML )    //direction is the response direction
        {
            Debug.log( this , Debug.MAPPING_WARNING,"The method getChildCount is not supported for" +
                                              " Fixed Format Source." );
            return false;
        }
        if ( source == null )
        {
            Debug.log( this, Debug.ALL_ERRORS, "ERROR: Message mapper wasn't given a data source." );

            return false;
        }

        // Get the source name corresponding to the target name.
        String srcName = (String)nameMap.get( targetName );

        if ( srcName == null )
        {

                return false;

        }

        Debug.log( this, Debug.MAPPING_DATA, "Testing for existence of target name [" + targetName +
                   "], which maps to source name [" + srcName + "]" );

        return( source.exists( srcName ) );
    } // end exists()

    /**
     * This method is not provided by this class  , logs error message if still used.
     */
    public boolean exists ( MessageContext msg , String targetName)
    {
        Debug.log( this , Debug.MAPPING_ERROR , "Error : Method exists "+
            "( MessageContext msg , String targetName) is not supported in class XMLNENAMapContainer. " );
        return false;
    }

    /**
     * This method is not provided by this class  , logs error message if still used.
     */
    public void allowPassThroughNames ( boolean flag )
    {
        Debug.log( this , Debug.MAPPING_ERROR , "Error : Method allowPassThroughNames ( boolean flag )"+
                    " is not supported in class XMLNENAMapContainer. " );
    }

    /**
     * This method is not provided by this class  , throws exception if still used.
     */
    public MessageContext getContext ( String subCompName ) throws MessageMapperException
    {
        throw new MessageMapperException( "Error : XMLNENAMapContainer : Method getContext "+
            "(String subCompName ) is not supported in class XMLNENAMapContainer. " );

    }

    /**
     * Change sense of mapping (src->tgt => tgt->src).
     *
     * @param  flag  Map target to source if 'true'.  
     *               Map source to target if 'false'.
     */
    public void invertMapping ( boolean flag )
    {
        Debug.log( this, Debug.MAPPING_STATUS, "Name-mapping inversion flag [" + flag + "]" );

        if ( mappingInverted != flag )
        {
            invertNameMap( );

            mappingInverted = flag;
        }
    } // end invertMapping()

    /**
     * This method is used by the method invertMapping() to
     * invert the name mappings in the nameMap hashtable.
     */
    private void invertNameMap ( )
    {
        Debug.log( this, Debug.MAPPING_STATUS, "Inverting name-mapping ..." );

        // Create hashtable for inverted mappings.
        Hashtable newNameMap = new Hashtable( );
        // Get all of the keys in the current name map.
        Enumeration nameKeys = nameMap.keys( );
        String curKey = null;

        // While current name-map has keys ...
        while ( nameKeys.hasMoreElements() )
        {
            try
            {
                // Get the next available name key.
                curKey = (String)nameKeys.nextElement( );
            }
            catch( NoSuchElementException nsee )
            {
                Debug.log( this, Debug.MAPPING_ERROR, "ERROR: Ran out of keys during name-map inversion.\n" +
                           nsee.toString() );
                break;
            }
            // Get the corresponding name value.
            String curValue = (String)nameMap.get( curKey );
            if ( curValue == null )
            {
                Debug.log( this, Debug.MAPPING_ERROR, "ERROR: Found key [" + curKey +
                           "] with no value in map." );
                break;
            }
            // Place swapped name-value pair in new map.
            newNameMap.put( curValue, curKey );

        }   // End while loop.
        // Replace old map with its inverted equivalent.
        nameMap = newNameMap;
        Debug.log( this, Debug.MAPPING_STATUS, "Done inverting name-mapping." );

    } // end invertMapping()

   /**
    * This method is used to find out the value of a field from fixed format source string.
    *
    * @param  sourceName - the field whose value is to be searched for in the fixed format source
    * @return value of the field
    *
    */
    private String getFixedFormatValue ( String sourceName )  throws MessageMapperException
    {
        // Get the value from the FixedFormatSource with the given position and size and return it.
        int position = getPosition( sourceName ) - 1 ;
        int size = getSize ( sourceName );
        return ( FixedFormatSource.substring( position , position+size ) );

    } // end getFixedFormatValue()

    /**
     * This is the object of XMLParser that is passed to the setSource() method of this class.
     *  It is used when this class is used by transformer for XML to fixed format conversion
     */
    private MessageDataSource source = null;

    /**
     * This is the fixed format string that is passed to the setFixedFormatSource() method of this class.
     *  It is used when this class is used by transformer for fixed format to XML conversion
     */
    private String FixedFormatSource = null;

    /**
     * The string indicating type of the fixed format string
     */
    private String type = null;

    /**
     * The flag indicating direction of conversion
     * True - xml to fixed format
     * False- fixed format to xml
     */
    private boolean MappingFromXML = true;

    /**
     * The flag to keep track of inversion of mappings
     */
    private boolean mappingInverted = false;

    /**
     * To store the mappings between fixed format fields & corresponding xml nodes
     */
    private Hashtable nameMap = null;

    /**
     * To store the mappings between fixed format fields & their expected formats.
     * The fixed format field name is the key & value is an object of class FixedFormat
     * that holds all information of that field (viz.Starting position , type , size , required)
     */
    private Hashtable nameFormatMap = null;

    public static void main(String[] args)
    {
        Properties props = new Properties();
        props.put( "DEBUG_LOG_LEVELS", "all" );
        props.put( "LOG_FILE", "d:\\logmap.txt" );
        Debug.showLevels( );
        Debug.configureFromProperties( props );

       try
       {
          FixedFormat format = null;
          String xmlText = FileUtils.readFile( "d:\\e911_requestKal.xml" );


          // ------- Create a parser to parse the XML document. -------
          MessageParser p = MessageParserFactory.create( Message.XML_TYPE );


          // ------- Give the parser the XML document to parse. -------
          p.parse( xmlText );

          String mapString = "RequestBody.ServiceOrder.ActivityType,Function Code,1,1,A,true\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.CallingTelephoneNumber,Calling Number,2,10,T,false\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.SANO,House Number,12,10,AN,true\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.SASF,House Number Suffix,22,4,AN,false\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.SASD,Prefix Directional,26,2,A,false\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.SASN,Street Name,28,60,AN,true\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.SATH,Street Suffix,88,4,A,false\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.SASS,Post Directional,92,2,A,false\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.City,Community Name,94,32,A,true\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.State,State,126,2,A,true\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.LocationInformation,Location,128,60,AN,false\n"
        + "RequestBody.ServiceOrder.EndUserInformation.CustomerName,Customer Name,188,32,AN,true\n"
        + "RequestBody.ServiceOrder.ClassOfService,Class Of Service,220,1,AN,true\n"
        + "RequestBody.ServiceOrder.TypeOfService,Type Of Service,221,1,N,true\n"
        + "RequestBody.ServiceOrder.Exchange,Exchange,222,4,AN,true\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.EmergencyServiceNumber,ESN,226,5,AN,false\n"
        + "RequestBody.ServiceOrder.MainTelephoneNumber,Main NPA,231,10,T,true\n"
        + "RequestHeader.OrderNumber,Order Number,241,10,AN,false\n"
        + "RequestHeader.ExtractDate,Extract Date,251,6,D,true\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.CountyID,County ID,257,4,AN,false\n"
        + "RequestHeader.CompanyID,Company ID,261,5,AN,true\n"
        + "RequestBody.ServiceOrder.DatabaseActivity,Source ID,266,1,AN,false\n"
        + "RequestBody.ServiceOrder.EndUserInformation.ServiceAddress.Zip,Zip Code,267,9,AN,false\n"
        + "RequestBody.ServiceOrder.Remarks,General Use,276,11,AN,false\n"
        + "RequestBody.ServiceOrder.Comments,Comments,290,30,AN,false\n"
        + "RequestBody.ServiceOrder.TARCode,TAR Code,350,6,AN,true\n"
        + "RequestBody.ServiceOrder.RemoteCallForwarding,RCF,356,10,N,false\n";


          MapContainer mapper = new  MapContainer( true , mapString );

          mapper.setSource( p );

          mapper.log();
          char[] nena = new char[512];
          Enumeration keys = mapper.getKeys();

          System.out.println(mapper.getValue("Function Code"));
          System.out.println(mapper.getMappedValue("Function Code"));
          //System.out.println(mapper.getValue("sdg"));
          System.out.println(mapper.getValue("House Number"));
          System.out.println(mapper.getPosition("House Number"));
          //System.out.println(mapper.getPosition("fgh"));
          System.out.println(mapper.getSize("House Number"));
          System.out.println(mapper.getType("House Number"));
          System.out.println(mapper.isRequired("House Number"));
          if (mapper.exists("Location"))
              System.out.println(mapper.getValue("Location"));
          else
              System.out.println("Location doesn't exist in xml file");
          System.out.println(mapper.exists("db"));
          System.out.println(mapper.exists(""));
          System.out.println(mapper.exists("   "));
          System.out.println(mapper.exists(null));
       }
       catch(Exception e)
       {
         System.out.println("Error : " +  e.toString());
       }
   }
}

//A5105551212CDATA10   4444CDdefaultCDATA60                                              CDATCDdefaultCDATA                    CD                                                            NIGHTFIRE                       11DATA     1234567890PON007    010100    22   DdefaultCD           ABCdefaultCDATA30                                              CDATA6                                                                                                                                                            *
//A5105551212CDATA10   4444CDdefaultCDATA60                                              CDATCDdefaultCDATA                    CD                                                            NIGHTFIRE                       11DATA     1234567890PON007    010100    22   DdefaultCD           ABCdefaultCDATA30                                              CDATA6                                                                                                                                                            *

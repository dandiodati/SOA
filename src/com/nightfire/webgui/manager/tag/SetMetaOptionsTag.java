/** 
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header: //webgui/core/com/nightfire/webgui/core/tag/util/SetSpecialObjectOnMetaField.java.java#1 $
 */
package com.nightfire.webgui.manager.tag;

import java.util.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import com.nightfire.webgui.core.tag.*;

import com.nightfire.framework.message.common.xml.XMLPlainGenerator;
import com.nightfire.webgui.core.DataHolder;
import com.nightfire.webgui.core.meta.Message;
import com.nightfire.webgui.core.meta.MessagePart;
import com.nightfire.webgui.core.meta.Field;
import com.nightfire.webgui.core.meta.DataType;
import com.nightfire.webgui.core.meta.OptionSource;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;

/**
 * This is a tag handler to set OptionSource objects on the specified meta fields - on the meta message object.
 * usage
 * <gui:setMetaFieldTag metaFile="${metaFile}" fieldId="${metaFileId}" optionsData="${object}"/>
 *
 * Note
 *                         
 *        //The xml structure for options data returned by the Backend is assumed to be 
 *        //<Body>.<DataContainer>.<Data(0)>.<Item1 value="abc"/>
 *        //<Body>.<DataContainer>.<Data(0)>.<Item2 value="xyz"/>
 *        //<Body>.<DataContainer>.<Data(1)>.<Item1 value="opq"/>
 *        //<Body>.<DataContainer>.<Data(2)>.<Item2 value="rst"/>
 * Item1 and Item2 ( element names do not matter, as they are refereced by indices ) are used to
 * extract the option value and display value for each Data Element respectively.
 * 
 * Additionally, the optionsData attribute can use any instance of a "Map" interface
 * and extract the optionValues and displayNames.
 * The keys from the Map object are used as option Values and the values are used as display names.
 * If there is no explicit display name for a particular option value, please use the option value itself
 * as the display name in the Map instance.
 */
 
public class SetMetaOptionsTag extends NFTagSupport
{  
    /**
     * the local variable to hold the location value
     */
    private Object metaFile = null ;
    private Object fieldId = null ;
    private Object optionsData = null ;   

    private boolean debug = false;

    public void setMetaFile ( Object metaFile ) throws JspException
    {
        this.metaFile = TagUtils.getDynamicValue( "metaFile", metaFile , Object.class, this, pageContext);
    }

    public void setFieldId( Object fieldId ) throws JspException
    {
        this.fieldId = TagUtils.getDynamicValue( "fieldId", fieldId , Object.class , this, pageContext);
    }

    public void setOptionsData ( Object optionsData ) throws JspException
    {
        this.optionsData = TagUtils.getDynamicValue( "optionsData", optionsData , Object.class, this, pageContext);
    }


    /**
     * For unit testing - only called from main
     */
    private int debug( Message metaFile, String fieldId, XMLPlainGenerator optionsData ) throws JspException
    {

        log.debug("Running the tag in debug mode");
       
        this.metaFile = metaFile;
        this.fieldId = (Object) fieldId;
        this.optionsData = optionsData;

        return doStartTag();

    }    

    /**
     * @exception  JspException  Thrown when an error occurs during processing.
     *
     * @return  SKIP_BODY.
     */
    public int doStartTag() throws JspException
    {

        super.doStartTag();
      
        if ( metaFile == null || fieldId == null || optionsData == null )
        {
            throw new JspException ( "SetMetaOptionsTag.execute() : Tag attributes metaFile, fieldId, optionsData must be specified");
        }

        try
        {
            XMLPlainGenerator parser = null;
            
            if ( optionsData instanceof Map )
            {
                Map optionsMap = (Map) optionsData;

                Set keySet = optionsMap.keySet();
                
                Iterator iter = keySet.iterator();

                //Create the xml document with the option values  
                parser = new XMLPlainGenerator ( "Body" ) ;
                Node container = parser.create ( "DataContainer" ) ;
                int i = 0;
                while ( iter.hasNext() )
                {
                    
                    String optionValue = (String) iter.next() ;
                    String displayName = (String) optionsMap.get ( optionValue );
                    parser.setValue ( container, "Data(" + i +").Name" , displayName );
                    parser.setValue ( container, "Data(" + i +").Value" , optionValue );
                    i++;
                }
            }
            else if ( optionsData instanceof Document )
            {
                parser = new XMLPlainGenerator ( (Document) optionsData ) ;
            }
            else if ( optionsData instanceof XMLPlainGenerator )
            {
                parser = ( XMLPlainGenerator ) optionsData ;
            }
            else if ( optionsData instanceof String )
            {
                parser = new XMLPlainGenerator ( (String) optionsData ) ;
            }
            else if ( optionsData instanceof DataHolder )
            {
                DataHolder d = (DataHolder) optionsData;
                parser = new XMLPlainGenerator ( d.getBodyStr() ) ;
            }
            else 
            {
                throw new JspException ( "SetMetaOptionsTag.execute(): The attribute optionsData's value must be of type Document, XMLPlainGenerator, String or DataHolder ");
            }
            
            Node[] children = parser.getChildren("DataContainer");
            int childCount = children.length;

            log.debug("The number of options available are [" + childCount +"]" );
            
            final String[] optionValues = new String[ childCount ];
            final String[] displayValues = new String [ childCount ];
            
            for ( int i = 0 ; i < childCount ; i ++ )
            {
                Node currentChild = children[i];
                
                String optionValue = parser.getValue ( currentChild, "0" );
                
                String displayValue = null;
                if ( parser.exists ( currentChild, "1" ) )
                {
                    displayValue = parser.getValue ( currentChild, "1" );
                }
                else
                {
                    displayValue = optionValue;
                }
                
                //Assume that the first node is for option value and second node is for display
                if ( log.isDebugEnabled() )
                {
                    log.debug("Adding option value [" + optionValue +"] and display value [" +
                                displayValue +"] to Options object" );
                }
                optionValues[i] = optionValue;
                displayValues[i] = displayValue;
            }
            
            //Create the options object 
            OptionSource options = new OptionSource ()
                {
                    public String[] getOptionValues()
                    {
                        return optionValues;
                    }
                    
                    public String[] getDisplayValues()
                    {
                        return displayValues;
                    }
                    
                    public String[] getDescriptions()
                    {
                        return null;
                    }
                    
                    public void readFromXML(Node ctx, Message msg) throws FrameworkException
                    {
                        //Not implemented
                    }
                };
            
            if ( ! ( metaFile instanceof Message ) )
            {
                throw new JspException ("SetMetaOptionsTag.execute() : metaFile object is not of type Message");
            }
            
            //Get the field on which the options needs to be set.
            Message m = ( Message ) metaFile;
            if ( log.isDebugEnabled() )
            {
                log.debug("Extracting field with id [" + fieldId +"] from Message");
            }
            MessagePart field = m.getMessagePart ( (String) fieldId ) ;
            
            if ( ! ( field instanceof Field ) )
            {
                throw new JspException("SetMetaOptionsTag.execute() : The message part extracted using id [" + fieldId +"] is not of type Field");
            }
            
            //Set the options on that field
            Field f = ( Field ) field ;
            DataType d = f.getDataType();
            if ( log.isDebugEnabled() )
            {
                log.debug("Obtained datatype =[" +d.getTypeName() +"] from the specified field");
            }
            d.setOptionSource ( options ) ;
            
        }
        catch ( Exception e )
        {

            if ( e instanceof JspException )
            {
                throw (JspException) e ;
            }
            else throw new JspException ( e.getMessage() ) ;
        }
        
        return SKIP_BODY;
    }
    
    /**
     * Clean up - this method is called after the doAfterBody() call.
     */
    public void release()
    {
        super.release();
        
        metaFile = null ;
        fieldId = null ;
        optionsData = null ;   
    }
    

    
   /**
     * Unit test
     */
    public static void main(String[] args)
    {
        try
        {
            if (args.length != 1)
            {
                throw new FrameworkException("usage: java "
                                             + SetMetaOptionsTag.class.getName() + " filename");
            }
            
            SetMetaOptionsTag test = new SetMetaOptionsTag();
            test.debug = true; 
            System.getProperties().setProperty(Debug.LOG_FILE_NAME_PROP ,"console");
            Debug.enableAll();

            File file = new File( args[0] );
            Message msg = new Message(file.toURL());
            
            XMLPlainGenerator gen = new XMLPlainGenerator ( "Body" ) ;
            gen.setValue ( "DataContainer.Data(0).Item1", "Item 1 Option 1");
            gen.setValue ( "DataContainer.Data(0).Item2", "Item 2 Display 1");
            gen.setValue ( "DataContainer.Data(1).Item1", "Item 1 Option 2");
            gen.setValue ( "DataContainer.Data(1).Item2", "Item 2 Display 2");
            
            System.out.println("Executing the tag with Options message [" + gen.getOutput() +"]" );

            test.debug ( msg, "TestField", gen ) ;
            MessagePart m = msg.getMessagePart( "TestField" );
            Field f = ( Field ) m;
            DataType d = f.getDataType();
            String[] displayValues = d.getOptionSource().getDisplayValues() ;
            String[] optionValues = d.getOptionSource().getOptionValues();
            for ( int i =0 ; i < displayValues.length ; i ++ )
            {
                System.out.println( optionValues[i] );
                System.out.println( displayValues[i] );
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
            System.err.println("Message: " + ex.toString());
            System.exit(-1);
        }
    }

}

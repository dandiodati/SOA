/*
 * $Header:$
 * 
 * Created on Mar 31, 2005
 *
 * Copyright (c) 2005 Neustar, Inc. All rights reserved.
 */
package com.nightfire.framework.rules;

import org.w3c.dom.Node;

import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.message.common.xml.XMLMessageBase;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

/**
 * This class contains the generic functions used in the context of Action Rules.
 * 
 * @author tphan
 *
 */
public abstract class ActionFunctions extends StandardFunctions{

    /**
     * Set a value on a node located at the path "location".
     * If the node does not exist, create it.
     *
     * @param location The xpath location of the node. This assumes that only
     *                 a single node will match up.
     *
     * @param value The value to be assigned to the node at the "location".
     * @return boolean on the success of the operation.
     */
     public boolean setValue (String location, String value)
     {
         boolean success = true; // I'm an optimist
         try
         {
             Node[] nodes = getNodes( location, source );
             if ( nodes.length == 1 )
                 //Found one node, which is good
             {
                 Node node = nodes[0];
                 XMLMessageBase.setNodeValue( node, value );
             }
             else
                 if ( nodes.length == 0 )
                     //Did not find node, so create it now.
                 {
                     String nodeName = convertXPathToXMLPath( location );
                     Node newNode = XMLMessageBase.getNode( 
                             (Node)source.getDocument().getDocumentElement(), nodeName, true );
                     XMLMessageBase.setNodeValue( newNode, value );
                 }
                 else
                 {
                     Debug.error( "Found more than one matching node for [" + location + "]." );
                 }
         }
         catch( FrameworkException e )
         {
             Debug.error( "Failed to set value[" + value + 
                     "] at location[" + location + "], " + e );
             success = false;
         }
         return success;
     }

     /**
      * Set a value on a node located at the path "location".
      * If the node does not exist, create it.
      *
      * @param location The xpath location of the node. This assumes that only
      *                 a single node will match up.
      *
      * @param value The value to be assigned to the node at the "location".
      * @return boolean on the success of the operation.
      */
      public boolean setValue (String location, Value value)
      {
          if( !value.hasValue() )
              return false;
          return setValue( location, value.toString() );
      }

     /**
     * Return the value of the runtime property identified by the key, 
     * type and name.
     *
     * @param propKey The property key
     * @param propType The property type
     * @param propName The property name
     *
     * @return A String containing the property value.
     */
     public String getProperty(String propKey, String propType, String propName)
     {
          String retString = null;
          try
          {
              retString = PersistentProperty.getProperty( propKey, propType, propName );
          }
          catch ( FrameworkException e )
          {
              Debug.error( e.toString() );
          }
          return retString;
     }

     /**
     * Pad a string out to a given length.  Given an input string value, return
     * a new string with a length of strLen which has been padded from the left
     * with padChar characters. If the length of the input string is greater
     * than or equal to strLen, return the input unchanged.
     *
     * @param value The string to be padded
     * @param padChar The character to be used as the pad
     * @param strLen The desired length of the output string
     *
     * @return A String of length strLen, padded from the left with padChar
     * characters.
     */
     public String padLeft(String value, char padChar, int strLen)
     {
        String retString = StringUtils.padString( value, strLen, true, padChar );
        return retString;
     }

     /**
      * If the node exists, replace the substring beginning at position startPos
      * with the string value. If the node doesn't exist, it will be created
      * with the value substring at position startPos. If the node is being
      * created and startPos > 0, the string is left padded with spaces. The 
      * output string may be longer than the original.
      *
      * @param nodePath The path (XPath convention) to the node into which the 
      * string will be inserted.
      * @param startPos The position at which to start replacing the string.
      * @param value The string used to replace the original.
      *
      * @return void
      */
     public boolean setString(String nodePath, int startPos, String value) {
         
         try{
             //simplify things
             if ( startPos < 0 )
                 startPos = 0;
             
             //We get the original value if any and use that to construct the new value
             //to be set on the node.
             String origValue = null;
             if ( source.nodeExists( nodePath ) )
                 origValue = (super.value( nodePath )).toString();
             
             //There are a few cases to consider when doing the replacement
             StringBuffer buf = new StringBuffer();
             if ( origValue == null )
                 //We only need to lpad with spaces if required
             {
                 for ( int i=0; i<startPos; i++ )
                     buf.append( ' ' );
                 buf.append( value );
             }
             else
             {
                 //if origValue is small, then padding might be required
                 if ( origValue.length() < startPos )
                 {
                     buf.append( origValue );
                     for ( int i=origValue.length() + 1; i<startPos; i++ )
                         buf.append( ' ' );
                     buf.append( value );
                 }
                 else if ( origValue.length() >= startPos )
                 {
                     buf.append( origValue.substring(0, startPos) );
                     buf.append( value );
                     if ( buf.length() < origValue.length() )
                     {
                         /* 
                          * Get the remainder of the original string and append it to the new value.
                          */
                         String remainingValue = origValue.substring( buf.length() );
                         buf.append( remainingValue );
                     }
                 }
             }
             String newValue = buf.toString();
             
             //set the value on an existing node or new node.
             return setValue( nodePath, newValue );
         }
         catch( Exception e ) {
             Debug.error( "Failed to modify string value[" + value + "], " + e );
             return false;
         }
     }

     /**
      * Convert the location in xpath notation to one in xml notation.
      *
      * @param location in xpath notation, assuming that the root node is also specified.
      *        Example: "/asr_order/ASR_Form/asr_adminsection/RTR"
      * @return String in xml notation with the root node removed from the path and all '/'
      *         replaced with '.'.
      *        Example: "ASR_Form.asr_adminsection.RTR"
      */
     protected String convertXPathToXMLPath ( String location )
     {
        int index = location.indexOf( "/" );
        if ( index == 0 )
        //We have a / at the start of the string, so look for second one.
        {
            index = location.indexOf( "/", index + 1 );
        }
        //Remove root node name from path.
        String retString = location.substring( index + 1 );
        retString = StringUtils.replaceSubstrings( retString, "/", "." );
        if ( Debug.isLevelEnabled( Debug.MSG_STATUS ) )
            Debug.log( Debug.MSG_STATUS, "convertXPathToXMLPath: Converting string [" +
            location + "], to string [" + retString + "]." );
            
        return retString;
     }
}

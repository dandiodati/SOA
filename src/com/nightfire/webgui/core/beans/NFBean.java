/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.beans;

import  javax.servlet.ServletException;
import com.nightfire.framework.message.MessageException;

import java.util.*;



/**
 * <p><strong>NFBean</strong> represents an interface, which all the
 * beans whose resposibility is to hold the response data for display,
 * must implement.  These interface methods allow for custom tags to
 * access the data and present them on the page.</p>
 */
 
public interface NFBean
{
    /**
     * Obtain the header data structure object.
     *
     * @return  Header data structure object.
     */
    public Object getHeaderDataSource();
    
    /**
     * Obtain the body data structure object.
     *
     * @return  Body data structure object.
     */
    public Object getBodyDataSource();


    /**
     * Returns the header data as a unmodifiable Map object
     * To modify data the dataSource methods must be used.
     */
    public Map getHeaderAsMap();


    /**
     * Returns the header data as a unmodifiable Map object
     * To modify data the dataSource methods must be used.
     */
    public Map getBodyAsMap();


    /**
     * Set the header data structure object.
     *
     * @param  headerDataSource Header data structure object.
     */
    public void setHeaderDataSource(Object headerDataSource);

    /**
     * Set the body data structure object.
     *
     * @param  bodyDataSource Body data structure object.
     */
    public void setBodyDataSource(Object bodyDataSource);



    /**
     * Set a header field value.
     * @param key - The field name , The key may or may not start with ServletConstants.NF_FIELD_HEADER_PREFIX.
     * @param value - The value of the field
     */
    public void setHeaderValue(String key, String value) throws ServletException;

    /**
     * Set a body field value.
     *
     * @param key - The field name , The key may or may not start with ServletConstants.NF_FIELD_PREFIX.
     * @param value - The value of the field
     */
    public void setBodyValue(String key, String value) throws ServletException;


    /**
     * Get a header field value.
     * If the key started with ServletConstants.NF_FIELD_HEADER_PREFIX it is stripped off.
     *
     * @param key - The field name , The key may or may not start with ServletConstants.NF_FIELD_HEADER_PREFIX.
     * @return The value of the field or empty string if it does not exist.
     */
    public String getHeaderValue(String key);

    /**
     * Get a body field value.
     * If the key started with ServletConstants.NF_FIELD_PREFIX it is stripped off.
     *
     * @param key - The field name , The key may or may not start with ServletConstants.NF_FIELD_PREFIX.
     * @return The value of the field or empty string if it does not exist.
     */
    public String getBodyValue(String key);

    
    /**
     * Add header data to this bean. All existing keys in the bean are replaced
     * by the same key from the Object
     * All fields starting with ServletConstants.NF_FIELD_HEADER_PREFIX are added as header fields.
     * All fields starting with ServletConstants.NF_FIELD_PREFIX are added as body fields.
     * All other fields are ignored.
     *
     * @param  Data -  Object with data
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void addToHeaderData(Object data) throws ServletException;


    /**
     * Add body data to this bean. All existing keys in the bean are replaced
     * by the same key from the Object.
     * All fields starting with ServletConstants.NF_FIELD_HEADER_PREFIX are added as header fields.
     * All fields starting with ServletConstants.NF_FIELD_PREFIX are added as body fields.
     * All other fields are ignored.
     *
     * @param  Data -  Object with data
     *
     * @exception  ServletException  Thrown when an error occurs during processing.
     */
    public void addToBodyData(Object data) throws ServletException;

    
    /**
     * Obtains the response code given in the header data.  This is applicable only
     * in the case where the Bean encapsulates the response data.
     *
     * @return  Response code.
     */
    public String getResponseCode();    
    
    /**
     * Convenient method for displaying the string representation of the header data.
     *
     * @return  Header data string.
     */
    public String describeHeaderData();
    
    /**
     * Convenient method for displaying the string representation of the body data. 
     *
     * @return  Body data string.
     */
    public String describeBodyData();


    /**
     * Makes a copy of this bean that finalizes all states and is ready
     * to be transformed, or sent to an external destination.
     *
     * @return The finalized output bean.
     */
    public NFBean getFinalCopy() throws MessageException;
    
    /**
     * Removes the indicated field from the header data.
     *
     * @param  field  The field to be removed.
     *
     * @return  true if the field existed and was removed, false otherwise.
     */
    public boolean removeHeaderField(String field);

    /**
     * Sets a transform object that can used to alter the bean header.
     * @param transform The transform object.
     * @exception ServletException is thrown if the transform object is invalid.
     */
    public void setHeaderTransform(Object transform) throws ServletException;

    /**
     * Sets a transform object that can used to alter the bean body.
     * @param transform The transform object.
     * @exception ServletException is thrown if the transform object is invalid.
     */
    public void setBodyTransform(Object transform) throws ServletException;


     /**
     * Returns a transform object that can used to alter the bean header.
     * @return transform The transform object if one was set, otherwise null.
     * The transform object will be an instance of XMLFilter or XSLMessageTransformer.
     *
     */
    public Object getHeaderTransform();

     /**
     * Returns a transform object that can used to alter the bean body.
     * @return transform The transform object if one was set, otherwise null.
     * The transform object will be an instance of XMLFilter or XSLMessageTransformer.
     */
    public Object getBodyTransform();


    /**
     * Applies any transformation, if available, to the current header and body data.
     * @exception Throws a message exception if it fails to transform the header or body data.
     */
    public void transform() throws MessageException;


    /**
     * Clears all transformation objects set on this bean
     */
    public void clearTransforms();
   
}
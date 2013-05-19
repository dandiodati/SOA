/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core;

import com.nightfire.framework.debug.DebugLogger;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.excel.ExcelUtils;


import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;

import java.util.*;
import java.io.InputStream;
import java.io.IOException;


/**
 * <p><strong>ServletParamUtils</strong> provides a set of utility
 * functions for use by servlet-parameters components.</p>
 *
 * This class uses Apache's common's file upload api, as thirdparty tool,
 * to process the multipart contents (formfields and stream).
 */

public final class ServletParamUtils
{

    public static String FORM_FIELDS = "formFields";
    public static String FORM_STREAMS = "formStreams";
    public static final String EXCEL_DATA     = "excelData";

    /**
     * This method returns boolean if the HttpServletRequest object
     * is of type multipart i.e. to process/upload/download data
     * like excel/doc/pdf etc.
     *
     * @param req HttpServletRequest object
     * @param servletContext ServletContext object
     * @return true if request object has multipart contents 
     */
    public static boolean isMultipartReq(HttpServletRequest req, ServletContext servletContext)
    {
        return ServletFileUpload.isMultipartContent(req);
    }

    /**
     * This method returns the </strong>Map</strong> object of all the
     * fields that has name-value pair for the request parameters.
     *
     * @param req HttpServletRequest object
     * @param servletContext ServletContext object
     * @return Map with elements as < String, String >
     * @throws ServletException on error
     */
    public static Hashtable<String, String> getMultipartFormFieldsAsMap (HttpServletRequest req, ServletContext servletContext)
            throws ServletException
    {
        HashMap<String, Object> hm = getMultipartFields(req, servletContext);
        return (Hashtable) hm.get(FORM_FIELDS);
    }

    /**
     * This method returns the <strong>Hashmap</strong> object below:
     * 1. First element will be Hashtable
     *    that has name-value pair for the request parameters.
     * 2. Second element will be ArrayList that contains
     *    all the streams object of multipart request.
     *
     *    This function checks all the <strong>FileItemStream</strong> request parameters,
     *    if parameter is not of type FormField then consider the parameter
     *    to be <strong>InputStream</strong> object of excel file and process it.
     *
     *    As req object may contain multiple stream objects,
     *    so returning <strong>ArrayList</strong> object, where each item of <strong>ArrayList</strong>
     *    is <strong>LinkedList</strong>, and <strong>LinkedList</strong> represents the data
     *    of excel file processed.
     *
     * @param req HttpServletRequest object
     * @param servletContext ServletContext object
     * @return HashMap with elements as < String, Object > 
     * @throws ServletException on error
     */
    public static HashMap<String, Object> getMultipartFields(HttpServletRequest req, ServletContext servletContext)
            throws ServletException
    {
        Hashtable<String, String> formFields = new Hashtable<String, String>();
        ArrayList<LinkedList> dataList = new ArrayList<LinkedList>();
        HashMap<String, Object> fields = new HashMap<String, Object>();
        DebugLogger logger = getLogger (servletContext);

        try
        {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iter = upload.getItemIterator(req);
            while (iter.hasNext())
            {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream stream =null;
                String value;
                try
                {
                    stream = item.openStream();
                    if (item.isFormField())
                    {
                        value = Streams.asString(stream);
                        formFields.put(name,value);
                    }
                    else
                    {
                        String fileName = item.getName();
                        if (StringUtils.hasValue(fileName))
                        {
                            LinkedList list = ExcelUtils.readExcelData (stream);
                            if (list != null)
                                dataList.add(list);
                        }
                    }
                }
                finally
                {
                    try
                    {
                        if(stream != null)
                            stream.close();
                    }
                    catch(Exception e)
                    {
                        // eating exception
                    }
                }
            }
        }
        catch (FileUploadException e)
        {
            logger.error("Unable to upload excel file.");
            throw new ServletException ("Unable to upload excel file.");
        }
        catch (IOException e)
        {
            logger.error("Unable to read excel file.");
            throw new ServletException ("Unable to read excel file.");
        }
        catch (Exception e)
        {
            logger.error("Unable to process multi part request.");
            throw new ServletException ("Unable to process multi part request.");
        }

        fields.put(FORM_FIELDS, formFields);
        fields.put(FORM_STREAMS, dataList);

        return fields;
    }

    /**
     * An internal convenient method for obtaining a logger for outputing log
     * statements.
     *
     * @param  context  ServletContext object.
     * @return  DebugLogger object.
     */
    private static DebugLogger getLogger(ServletContext context)
    {
        if (context != null)
        {
            String webAppName = ServletUtils.getWebAppContextPath(context);

            return DebugLogger.getLogger(webAppName, ServletParamUtils.class);
        }

        return DebugLogger.getLoggerLastApp(ServletParamUtils.class);
    }

}

/*
 * Copyright (c) 2004 Neustar Software, Inc. All rights reserved.
 */

package com.nightfire.framework.repository;

import java.io.*;
import java.net.*;
import java.util.*;



import com.nightfire.framework.util.*;





/**
 * This class gets returned when calling openConnection on a URL object.
 * It provides support for URL connections to the repository.
 * The following URLs will use this connection:
 *  <ul>
 *   <li>repository:///<category>/<criteria></li>
 *   <li>repository:///<category></li>
 *   <li>repository:///<category>?INCLUDE_SUB_CATEGORIES=true&FILTER=.xml</li>
 *  </ul>
 *  where <category> is category in the repository,  This can have '.'separators in it for
 *  paths with sub categories. (repository:///rules.local/BSAV)
 *        <criteria> is the criteria in the repository.
 *  If no criteria is provided then a list of all file in specified category will be given.
 *  (2nd and 3rd url). 
 * The third url, gives an example with optional properties(only used for a listing).
 * INCLUDE_SUB_CATEGORIES - indicates that subcategories will be searched( defaults to true).
 * FILTER - indicates the file type that should be returned (defaults to no filter).
 * These options can also be set via the setRequestProperty() method instead of being part of
 * the URL, but must be set before calling connect().
 *
 * NOTE: This class uses the useCaches method. If caches are disabled then
 * the repository accessed the all files all the time and does not cache them.
 *
 * @author <a href="mailto:dan@nightfire.com"></a>
 * @version 1.0
 */
public class RepositoryURLConnection extends URLConnection
{

    public static final String INCLUDE_SUB_CAT_PROP = "INCLUDE_SUB_CATEGORIES";
    public static final String FILTER_PROP = "FILTER";
    public static final String TXT_TYPE = "text";
    public static final String LIST_TYPE = "list";
    
    private String data = null;

    private List fileList = null;
    private String fileListAsStr = null;
    

    private String category = null;
    private String criteria = null;

    boolean includeSubCategories = true;
    String filter = RepositoryManager.NO_FILTER;
    
    

    private String contentType = null;
    

    private int contentLength = -1;
    
    
    private HashMap reqProps;
    

    
    
    public RepositoryURLConnection(URL url) 
    {
        super(url);
        this.url = url;

        String path = url.getPath();

        reqProps = new HashMap();
        
        
        
        StringTokenizer toker = new StringTokenizer(path,"/");

        category = toker.nextToken();

        
        if (toker.hasMoreTokens()) {
            criteria = toker.nextToken();
            
            contentType = TXT_TYPE;
        }
        else
            contentType = LIST_TYPE;
        
            

        String query = url.getQuery();

        
        if (query != null) {

            String[] pairs = query.split("&");
        
            for(int i =0; i < pairs.length; i++ ) {
                String[] nameVal = pairs[i].split("=");
                setRequestProperty(nameVal[0], nameVal[1]);

                
            }
        }
        
        
    }

    public void setRequestProperty(String name, String value)
    {
        reqProps.put(name, value);
    }

    
    /**
     * This method behavies differently from the abstract parent
     * method. It just calls the setRequestProperty method.
     *
     * @param name a <code>String</code> value
     * @param value a <code>String</code> value
     */
    public void addRequestProperty(String name, String value)
    {
        setRequestProperty(name, value);
    }
    
    public String getRequestProperty(String name) 
    {
        return (String)reqProps.get(name);
    }
    
    

    /**
     * Obtains the content type this can be one of the following:
     * TXT_TYPE - indicating a text file.
     * LIST_TYPE - Indicates a list of files.
     *
     * @return a <code>String</code> value
     */
    public String getContentType() 
    {
        return contentType;
    }

    
    /**
     * Returns the size of the file if the content type is TXT_TYPE
     * or returns the number of files if the content type is LIST_TYPE.
     *
     * @return an <code>int</code> value
     */
    public int getContentLength()
    {

        if (contentType.equals(TXT_TYPE))
            return data.length();
        else
            return (fileList != null ? fileList.size() : 0);
        
    }
    
        
    
    /**
     * Returns the content of the url after connecting. 
     *
     * @return A String if the content type is TXT_TYPE or a List if content
     * type is LIST_TYPE.
     * @exception IOException if an error occurs
     */
    public Object getContent() throws IOException
    {
        connect();
        

        if (contentType.equals(TXT_TYPE))
            return data;
        else {
            return (fileList == null ? null : fileList);
        }
        
        
    }
    
    public void connect() throws IOException
    {

        if (connected)
            return;

        String filter = getRequestProperty(FILTER_PROP);
        boolean includeSubCat = StringUtils.getBoolean(
                                                       getRequestProperty(INCLUDE_SUB_CAT_PROP),
                                                       false);
        
        
        try {
            RepositoryManager repositoryManager = RepositoryManager.getInstance();
            
            
            if ( criteria == null) { 
                NVPair[] pairs = repositoryManager.listMetaData(category, includeSubCat, filter);
                fileList = Arrays.asList(pairs);
                
            }
            else
                data = repositoryManager.getMetaData(category, criteria, useCaches);
            
  
        } catch ( NotFoundException nfe )
        {
            String errMsg = "Resource not found in Repository: " + nfe.getMessage();
            throw new IOException ( errMsg );
        }
        catch ( RepositoryException e )
        {
            throw new IOException ( "Unable to get meta-data from the Repository: " + e.toString() );
        }

        connected = true;
        
    }

 

    public String getHeaderField(String key) 
    {
        if(key.equals("last-modified"))
            return String.valueOf(getLastModified());
        else if (key.equals("content-type"))
            return getContentType();
        else if (key.equals("content-length"))
            return String.valueOf(getContentLength());
        else
            return super.getHeaderField(key);
    }
    
    public long getLastModified() 
    {
        if (useCaches || contentType.equals(LIST_TYPE))
            return 0;
      
        try {
            
            RepositoryManager repositoryManager = RepositoryManager.getInstance();
            return repositoryManager.getLastModifiedOfMetaData(category, criteria);
        }
        catch (RepositoryException e) {
            return 0;
        }
        
    }

    
    /**
     * Returns an input stream to the url contents. If the content type is
     * TXT_TYPE then the stream will be the text data for the file.
     * If the content type is LIST_TYPE, the stream will be a formated string
     * of the list of files. The following pattern will be used:
     * FileName=criteria&FileName2=critera2&...
     *
     * @return an <code>InputStream</code> value
     * @exception IOException if an error occurs
     */
    public InputStream getInputStream() throws IOException
    {
        connect();
        
        if (contentType.equals(TXT_TYPE))
            return new ByteArrayInputStream(data.getBytes());
        else 
            return new ByteArrayInputStream(getFileListAsString().getBytes());
    }


    private String getFileListAsString()
    {
        if ( fileListAsStr == null) {
            StringBuffer buf = new StringBuffer();
            
            Iterator iter = fileList.iterator();
            while(iter.hasNext() ){
                NVPair p = (NVPair) iter.next();
                buf.append(p.getName()).append("=").append( (String)p.getValue());
                
                if(iter.hasNext())
                    buf.append("&");
            }
            
            fileListAsStr = buf.toString();
            
        }

        return fileListAsStr;
    }
    
        
   
    /**
     * Not supported.
     *
     * @exception IOException if an error occurs
     */
    public OutputStream getOutputStream() throws IOException
    {
        throw new UnknownServiceException("Outputstream not supported for protocol repository");
    }
    
    
    

    


    
}

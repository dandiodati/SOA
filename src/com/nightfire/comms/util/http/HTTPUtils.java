package com.nightfire.comms.util.http;

import com.nightfire.framework.util.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Utility class for doing HTTP stuff
 */

public class HTTPUtils
{
    private static class ClientSocket
    {
        public PrintWriter out = null;
        public BufferedReader in = null;
        public Socket s = null ;
    }

    
    /** 
     * POSTs the given content to the given URL with a default content type of text/html.
     *
     * @param String the url to post to
     * @param content the body of the post
     * @exception IOException if post fails
     */
    public static String post (String urlString, String content) 
    	throws IOException {

        return post (urlString, TEXT_HTML,content) ;

    }


    /** 
     * POSTs the given content to the given URL.
     *
     * @param String the url to post to
     * @param content the body of the post
     * @param contentType the content-type of the request
     * @exception IOException if post fails
     */
    public static String post (String urlString , String contentType, String content) 
    	throws IOException {

        Debug.log (Debug.MSG_LIFECYCLE, "HTTPUtils: Trying to post " +
                   content + " to web server at [ " +urlString + "]" );

        StringBuffer responseBuffer  = new StringBuffer();
        
        URL url = new URL(urlString);

        ClientSocket cs = new ClientSocket();

        HTTPUtils.Response resp = post(cs, url, new Hashtable(), contentType, content );
            
        while (true)
        {
            String line = resp.content.readLine();
            if ( line == null )
                break;

            responseBuffer.append( line );
        }

        Debug.log (Debug.MSG_LIFECYCLE, "HTTPUtils: Obtained response from web server : " + responseBuffer.toString() );

	cleanUp( cs ) ;
        
        return responseBuffer.toString();
        
    }



    /** 
     * POST something to the given URL. The headers are put in as 
     * HTTP headers, the content length is calculated and the content
     * is sent as content. Duh.
     *
     * @param cs  Client socket items.
     * @param url the url to post to
     * @param headers additional headers to send as HTTP headers
     * @param contentType type of the content
     * @param content the body of the post
     * @exception IOException if post fails
     */
    private static Response post (ClientSocket cs, URL url, Hashtable headers,
                                 String contentType, String content)
	throws IOException {
        
        //If the port is not specified in the URL, use the default http port.
        int port = url.getPort();
        if ( port == -1 ) port = 80;

        cs.s = new Socket (url.getHost(), + port );
        cs.out = new PrintWriter (cs.s.getOutputStream());
        cs.in = new BufferedReader (new InputStreamReader (cs.s.getInputStream()));

        //Create the HTTP header
        cs.out.print (HTTP_POST + " " + url.getFile() + " HTTP/" + HTTP_VERSION + "\r\n");
        cs.out.print (HEADER_HOST + ": " + url.getHost() + ':' + port + "\r\n" );
        cs.out.print (HEADER_CONTENT_TYPE + ": " + contentType + "\r\n" );
        cs.out.print (HEADER_CONTENT_LENGTH + ": " + content.length() + "\r\n" );
        
        for (Enumeration e = headers.keys(); e.hasMoreElements(); )
        {
            Object key = e.nextElement();
            cs.out.print (key + ": " + headers.get(key) + "\r\n" );
        }

        //According to HTTP1.1 need another CRLF after the header
        cs.out.print ("\r\n");

        Debug.log( Debug.MSG_LIFECYCLE, "HTTPUtils: Writing content to the socket... " + content );

        cs.out.println (content);

        cs.out.flush ();

        /* read the status line */
        int statusCode = 0;
        String statusString = null;
        StringTokenizer st = new StringTokenizer ( cs.in.readLine () );
        st.nextToken (); // ignore version part
        statusCode = Integer.parseInt (st.nextToken ());

        StringBuffer sb = new StringBuffer ();
            
        while (st.hasMoreTokens ())
        {
            sb.append (st.nextToken ());
            if (st.hasMoreTokens ())
                sb.append (" ");

        }
            
        statusString = sb.toString ();
        
        /* get the headers */
        Hashtable respHeaders = new Hashtable ();
        int respContentLength = -1;
        String respContentType = null;
        String line = null;
            
        while ((line = cs.in.readLine ()) != null)
        {
            
            if (line.length () == 0)
                break;

            int colonIndex = line.indexOf (':');
            String fieldName = line.substring (0, colonIndex);
            String fieldValue = line.substring (colonIndex + 1).trim ();

            if (fieldName.equals (HEADER_CONTENT_LENGTH))
                respContentLength = Integer.parseInt (fieldValue);
            else if (fieldName.equals (HEADER_CONTENT_TYPE))
                respContentType = fieldValue;
            else
                respHeaders.put (fieldName, fieldValue);
        }

	Response response = new Response (statusCode, statusString, respHeaders, 
                             respContentLength, respContentType, cs.in);


        return response ;
    }

    /**
     * Class to encapsulate the http response
     */
    private static class Response {
        int statusCode;
        String statusString;
        public Hashtable headers;
        public int contentLength;
        public String contentType;
        public BufferedReader content;

        Response (int statusCode, String statusString, Hashtable headers,
                  int contentLength, String contentType, BufferedReader content)
        {
            this.statusCode = statusCode;
            this.statusString = statusString;
            this.headers = headers;
            this.contentLength = contentLength;
            this.contentType = contentType;
            this.content = content;
        }
    }



    /**
     *  Close all IO connections that were opened during post() 
     * @param cs  Client socket items to populate.
     */
    private static void cleanUp( ClientSocket cs ) throws IOException {

        Debug.log (Debug.MSG_LIFECYCLE, "HTTPUtils: closing IO socket connections");

        if (cs.in != null)
        {
            try
            {
                cs.in.close() ;

                cs.in = null;
            }
            catch ( Exception e )
            {
                Debug.log( Debug.ALL_WARNINGS, e.toString() );
            }
        }

        if (cs.out != null)
        {
            try
            {
                cs.out.close() ;

                cs.out = null;
            }
            catch ( Exception e )
            {
                Debug.log( Debug.ALL_WARNINGS, e.toString() );
            }
        }

        if (cs.s != null)
        {
            try
            {
                cs.s.close() ;

                cs.s = null;
            }
            catch ( Exception e )
            {
                Debug.log( Debug.ALL_WARNINGS, e.toString() );
            }
        }
    }


    //For constructing the HTTP Request header
    private static final String HTTP_VERSION = "1.1";
    private static final String HTTP_POST = "POST";
    private static final String HEADER_HOST = "Host";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

    public static final String TEXT_HTML = "text/html";

}

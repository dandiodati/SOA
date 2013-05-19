package com.nightfire.comms.servlet;

import  java.io.*;
import  java.util.*;

import  javax.servlet.*;
import  javax.servlet.http.*;

import com.nightfire.framework.util.*;

/**
 *  @author : Srinivas Pakanati
 *  The request to this servlet (which is actually an asynchronous
 *  response from the ILEC/supplier) 
 *  will be saved in the directory specified by property
 *  INBOX_DIRECTORY_PATH. After the message is written to the inbox, 
 *  either a default xml-rpc formatted null response or the data 
 *  from the file specified by property RESPONSE_FILE_PATH 
 *  is returned to the caller. These properties are configured in
 *  the web descriptor file web.xml 
 */
public class ReceiveResponseServlet extends HttpServlet 
{

    //Default xml response to the caller
    private String xmlResponse = "<?xml version=\"1.0\" ?>" +
        "<methodResponse><params><param><value> <nil /> </value> </param> </params> </methodResponse>";

    //Directory to save the incoming requests 
    private final String INBOX_DIRECTORY_PATH = "INBOX_DIRECTORY_PATH";

    //Optional property to load a response to return to the caller
    private final String RESPONSE_CONTENT_FILE_PATH = "RESPONSE_FILE_PATH";

    //The value to suffix to file name for making it unique in combination with
    //currentTimeMillis. (Required for bug fix in Batch Processing).
    private volatile int fileCount;

    private String inboxPath = null;
    private String responseFilePath = null;
    private String logLevels = null;
    private String logFile = null;
    
    /**
    *  Overrides the HttpServlet's init().
    *
    *  @param config Servlet configuration information
    *  @exception ServletException when initialization fails.
    */
    public void init() throws ServletException   
    {
    
        try 
        {
            initializeParameters();
        
            //Each startup of servlet engine will reset this counter but as
            //the file name is result of millis + fileCcount, it will remain unique
            //for single VM.
            fileCount =0;
        }
        catch(ServletException e) 
        {
            logMessage( e.getMessage() );
            throw e;
        }
    }

    /**
     * Load the servlet properties 
     */
    private void initializeParameters() throws ServletException   
    {
        inboxPath = getInitParameter( INBOX_DIRECTORY_PATH ) ;
        responseFilePath = getInitParameter ( RESPONSE_CONTENT_FILE_PATH );
        logLevels = getInitParameter ( Debug.DEBUG_LOG_LEVELS_PROP );
        logFile = getInitParameter ( Debug.LOG_FILE_NAME_PROP );

        logMessage("initializeParameters(): INBOX = [" + inboxPath +"] RESPONSE FILE =[" +
                   responseFilePath +"] LOG LEVELS = [" + logLevels +"] LOG FILE =[" + logFile +"]" );

        if ( StringUtils.hasValue ( logLevels ) && StringUtils.hasValue ( logFile ) )
        {
            Properties logProps = new Properties();
            logProps.put ( Debug.DEBUG_LOG_LEVELS_PROP, logLevels );
            logProps.put ( Debug.LOG_FILE_NAME_PROP, logFile );
                
            Debug.configureFromProperties ( logProps );
        }
    }

    /**
    *  Overrides the HttpServlet's doPost().
    *
    *  @param req  HttpServletRequest that encapsulates the request to the servlet.
    *  @param resp HttpServletResponse that encapsulates the response from the servlet.
    *  @exception ServletException - when servlet communication fails
    *  @exception IOException - when read/write of socket fails.
    */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException    
    {

        StringBuffer inputBuffer = new StringBuffer();

	PrintWriter out = res.getWriter();

	if ( req.getContentLength() != -1 )
	{
	    BufferedReader inputReader = req.getReader();

	    while ( true  )  
	    {
                try
                {
                    String input = inputReader.readLine();
                    
                    if (input == null)  
                    {
                        break;
                    }
                    
                    inputBuffer.append ( input );
                }
                catch ( InterruptedIOException ioe )
                {
                    //Ignore
                }
            }

	    try 
	    {
                logMessage("doPost(): Servlet received message [" + inputBuffer.toString() + "]" );
                
                writeToFile ( inputBuffer.toString() ) ;
                                
                res.setContentType("text/xml");
                                
                try
                {
                    if ( StringUtils.hasValue ( responseFilePath ) )
                        {
                            logMessage("doPost(): Reading the response from file [" + responseFilePath +"]" );
                            xmlResponse = FileUtils.readFile ( responseFilePath );
                        }
                }
                catch(Exception e)
                {
                    //Return default response if a specific response can't be read
                }
                
                logMessage("doPost(): Servlet returning response  [" + xmlResponse +"]" );

                out.println( xmlResponse );
                
            }
	    catch(FrameworkException e) 
	    {
                throw getException ( "doPost()"," Could not write the message, reason : " + e.getMessage() );
            }
	    finally  
	    {
                out.flush();
                out.close();
		    
                if ( inputReader !=null )  
                {
                    inputReader.close() ;
                }
            }
	}
	else 
	{
            out.println( "Please post a valid content to this servlet");
	}
	
    }

    /**
     * Write the request( response message from the supplier ) to the specified 
     * directory with timestamp and a unique id.
     * @param message - the request body from the http request.
     * @exception FrameworkException when the file cannot be created.
     */
    private synchronized void writeToFile ( String message ) throws FrameworkException  
    {
        // filename consists of rsp + millis + underscore + sequance number + extension.
        String fileName = inboxPath + File.separator + "rsp" +
            DateUtils.getDateToMsecAsString() + "_"+ String.valueOf(++fileCount)+
            ".xml"  ;

        FileUtils.writeFile ( fileName, message ) ;

        logMessage("writeToFile(): Wrote the message [" + message +"] to file ["
                                + fileName +"]" );
    }

    /**
    *  Overrides the HttpServlet's doGet().
    *
    *  @param req  HttpServletRequest that encapsulates the request to the servlet.
    *  @param resp HttpServletResponse that encapsulates the response from the servlet.
    */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException   
    {

        logMessage("In doGet(), Forwarding to doPost() ...");
        doPost(req, res);
    }


    /**
    * Creates a ServletException with the given method name and exception message
    * @param methodName - the name of the method to prepend the error message.
    * @param exceptionMessage - the message to be wrapped inside the exception
    * @return ServletException
    **/
    private ServletException getException ( String methodName, String exceptionMessage ) 
    {

        logMessage( exceptionMessage ) ;
        return new ServletException ("In " + methodName + " : " +exceptionMessage ) ;
    }

    /**
     * Logs the given message to the servlet context log 
     * and as well as to the nightfire debug log file.
     */
    private void logMessage( String message )
    {
        getServletContext().log( message );
        
        Debug.log ( Debug.MSG_STATUS, message );
        
    }
}

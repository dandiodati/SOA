package com.nightfire.webgui.core;

import  java.io.Serializable;
import org.w3c.dom.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;
import com.nightfire.webgui.core.xml.*;
import com.nightfire.framework.debug.*;
import com.nightfire.framework.message.common.xml.*;



/**
 * <p><strong>DataHolder</strong> is a general data container class which
 * composes of a header and a body.  It is typically used to encapsulate
 * a data's header XML and body XML.</p>
 */
 
public class DataHolder implements Serializable
{
    private String header;
    private String body;

    private Document headerDom;
    private Document bodyDom;

  private DebugLogger log;
  
    public DataHolder()
    {

       header = null;
       body = null;
       headerDom = null;
       bodyDom = null;
       log = DebugLogger.getLoggerLastApp(DataHolder.class);
       
    }

    /**
     * Creates a DataHolder with the specified header and body.
     */
    public DataHolder(String header, String body)
    {
       this();
       this.header = header;
       this.body   = body;
        
    }


    /**
     * Creates a DataHolder with the specified header and body.
     */
    public DataHolder(Document header, Document body)
    {
      this();
      this.headerDom = header;
      this.bodyDom   = body;
    }

    /**
     * Returns the header as a a string.
     */
    public String getHeaderStr()
    {
       try {
          if ( header == null && headerDom != null)
            header = new XMLPlainGenerator(headerDom).getOutput();
        } catch( MessageException e ) {
         DebugLogger.getLoggerLastApp(DataHolder.class).error("Could not convert header xml: " + e.getMessage() );
        }
       return header;
    }

    /**
     * Returns the body as a string.
     */
    public String getBodyStr()
    {
       try {
          if ( body == null && bodyDom != null)
             body = new XMLPlainGenerator(bodyDom).getOutput();
       } catch( MessageException e ) {
         log.error("Could not convert body xml: " + e.getMessage() );
       }
       return body;
    }

     /**
     * Returns the header as a a string.
     */
    public Document getHeaderDom()
    {
        try {
          if ( headerDom == null && header != null)
             headerDom = new XMLPlainGenerator(header).getOutputDOM();
        } catch( MessageException e ) {
         log.error("Could not convert header xml: " + e.getMessage() );
        }
       return headerDom;
    }

    /**
     * Returns the body as a string.
     */
    public Document getBodyDom()
    {
       try {
          if ( bodyDom == null && body != null)
             bodyDom = new XMLPlainGenerator(body).getOutputDOM();

       } catch( MessageException e ) {
         log.error("Could not convert body xml: " + e.getMessage() );
       }
 

       return bodyDom;
    }

    public void setHeader(String head)
    {
       this.headerDom = null;   
       this.header = head;
    }

    public void setHeader(Document head)
    {
       this.header = null; 
       this.headerDom = head;
    }

     public void setBody(String body)
    {
       this.bodyDom = null;
       this.body = body;
    }

    public void setBody(Document body)
    {
       this.body = null;
       this.bodyDom = body;
    }



}

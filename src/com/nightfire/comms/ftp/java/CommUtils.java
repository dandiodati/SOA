/**
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.ftp.java;

import java.io.*;

import com.nightfire.common.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.util.*;

import org.w3c.dom.Document;

import com.nightfire.comms.util.ftp.*;
import com.ibm.bsf.*;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;



/*
 * This is a utility class for the FTPPoller and FTPClient.
 * It provides common utility methods.
*/
class CommUtils
{
     
   public static final  Object execScript(String script, String name, BSFManager scriptMgr) throws FtpException 
    {
        try {
            
            if(Debug.isLevelEnabled(Debug.MSG_BASE))
                Debug.log(Debug.MSG_BASE, "Executing script operation: " + script);
            
            return scriptMgr.eval("javascript",name,0,0, script);
        }
        catch (Exception e) {
            Debug.logStackTrace(e);
            
            throw new FtpException("Failed to execute script [" + name +"] : " + e.getMessage() );
        }

    
    }
    
        
    public static final String convertByteToString(byte[] data, String charEncoding) throws MessageException   
    {
        
        String s = null;
        try {
            
            if (StringUtils.hasValue(charEncoding))
                s = new String (data, 0, data.length, charEncoding );
            else
                s = new String(data);
            return s;
        }
        catch (UnsupportedEncodingException e) {
            Debug.error("Invalid encoding specified: " + e.getMessage() );
            throw new MessageException(e);
        }
        
    }
    

    public static final byte[] convertToByte(Object input, String charEncoding) throws MessageException
    {
        byte[] data  = null;
        
               // try to do an ftp put if the input is of type string or byte[]
        try {
            if ( input instanceof String ) {
                Debug.log(Debug.IO_DATA, "The input is of String converting to byte[]...");
                if ( StringUtils.hasValue(charEncoding) ) {
                        
                    try {
                        data = ((String) input ).getBytes(charEncoding);
                    } catch (UnsupportedEncodingException e) {
                        Debug.error("Invalid encoding specified: " + e.getMessage() );
                        throw new MessageException(e);
                    }
                        
                } 
                else
                    data = ((String) input ).getBytes();

                   
            }
            else if ( input instanceof Document) {
                XMLMessageParser parser = new XMLMessageParser((Document)input);
                String xml = parser.getGenerator().generate();
                data = convertToByte(xml, charEncoding);
            }
            else {
                data = (byte[]) input;
            }
        } catch (ClassCastException e) {
            throw new MessageException ("FTPClient: Input given is not of type byte[] or String or Document: " + e.getMessage() );
        } catch (MessageException e) {
            throw new MessageException("FTPClient: Invalid xml document: " + e.getMessage() );
        }
         
        return data;
        
    }


    /**
     * Writes file to appropriate directory/filename as String or byte[]
     *
     * @param  fileName   Name of file
     *
     * @param  dir        Directory
     *
     * @param message     Contents of file
     *
     */
    public static final void saveFile(String fileName, String dir, byte[] data, FtpDataConnect ftp, String charEncoding)
        throws ProcessingException
    {
        try
        {
            if (ftp.getTransferMode() == FtpDataConnect.ASCII_MODE)
                FileUtils.writeFile(dir + fileName, convertByteToString(data, charEncoding));
            else if (ftp.getTransferMode() == FtpDataConnect.BIN_MODE)
                FileUtils.writeBinaryFile(dir + fileName, data);
            else
                throw new ProcessingException("Message is not a String or byte[].");
        }
        catch (FrameworkException fe)
        {
            throw new ProcessingException("Cannot write file [" + fileName + "] to directory[" + dir + "] : " + fe.getMessage() );
            
        }
    }


    public static final String addTrailingPathSep(String path)
    {
        if (StringUtils.hasValue(path)) {
            if ( path.endsWith(File.separator) || path.endsWith("/") ) {
                return path;
            }
            else if (path.indexOf(File.separator) > -1)
                return path + File.separator;
            else if (path.indexOf("/") > -1 )
                return path +"/";
            else 
                return path + File.separator;
            
            
        } else
            return path;
        
    }
    
        


    public static final class FileBean 
    {
    
        private String name;       
        private byte[] data;
        private String dir;
        
         
        public byte[] getData()
        {
            return data;
        }

        public void setData(byte [] data)
        {
            this.data = data;
        }

        public String getName()
        {
            return name;
        }
        
        public void setName(String name)
        {
            this.name = name;
            
        }

         
        public String getDir()
        {
            return dir;
        }

        public void setDir(String dir)
        {
            this.dir = dir;
        }
        
        
        
    }
    
}

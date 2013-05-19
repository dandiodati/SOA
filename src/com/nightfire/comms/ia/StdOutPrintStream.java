package com.nightfire.comms.ia;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.comms.ia.asn.HexFormatter;

public class StdOutPrintStream extends PrintStream
{

        
    public StdOutPrintStream() 
    {
        super(System.out);        
    }
    
    public StdOutPrintStream( OutputStream out )
    {
        super( out );
    }

    public void write(byte b[], int off, int len)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(b, off, len);
        
        if( Debug.isLevelEnabled(Debug.IO_DATA) )
            Debug.log(Debug.IO_DATA, "SSL layer : " + baos.toString());
    }

    public void write(int b)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(b);          
        if( Debug.isLevelEnabled(Debug.IO_DATA) )
            Debug.log(Debug.IO_DATA, "SSL layer : " + baos.toString());
       
    }
    
}

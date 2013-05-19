package com.nightfire.comms.ia;

import java.io.*;
import java.util.*;

import com.nightfire.framework.util.*;
import com.nightfire.comms.ia.asn.HexFormatter;

public class SnoopOutputStream extends FilterOutputStream
{

    public SnoopOutputStream( OutputStream out )
    {
        super( out );
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        out.write(b, off, len);
        if( Debug.isLevelEnabled(Debug.UNIT_TEST) )
        {
            Debug.log(Debug.UNIT_TEST, "Snooper read ["+ new HexFormatter(b)+"]");
        }
    }
}
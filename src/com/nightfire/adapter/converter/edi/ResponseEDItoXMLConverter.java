/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import java.lang.reflect.*;
import com.nightfire.framework.util.*;

/**
 * This class is responsible for converting EDI responses to 
 * XEDI XML. This class extends EDItoXMLConverter by implementing the 
 * methods which specify what Parser classes to use for individual 
 * versions of EDI.
 */
public class ResponseEDItoXMLConverter extends EDItoXMLConverter{

    /**
     * Return the Parser class to use in the case where the EDI Version 
     * is unknown.
     *
     * @return The default Parser class which will parse EDI version
     *         4020 Response transaction sets. 
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getTransactionSetParserClass()
    throws ClassNotFoundException
    {
        return Class.forName("com.nightfire.adapter.converter.edi.EDI4010RespParser");
    }

    /**
     * Return the appropriate Response Parser based on the given
     * EDI version. 
     * In the case where the EDI version is not recognized, the default 
     * parser will be returned.
     *
     * @param ediVersion The String extracted from the GS Segment designating
     *                   what EDI version is being used.
     * @return The parser for parsing EDI response transactions sets of the given version 
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getTransactionSetParserClass(String ediVersion)
    throws ClassNotFoundException
    {
        if(-1 != ediVersion.indexOf("4010"))
        {
            return Class.forName("com.nightfire.adapter.converter.edi.EDI4010RespParser");
        }         
        else if(-1 != ediVersion.indexOf("4020"))
        {
            return Class.forName("com.nightfire.adapter.converter.edi.EDI4020RespParser");
        }          
        else if(-1 != ediVersion.indexOf("4030"))
        {
            return Class.forName("com.nightfire.adapter.converter.edi.EDI4030RespParser");
        }          
        else if(-1 != ediVersion.indexOf("4050"))
        {
            return Class.forName("com.nightfire.adapter.converter.edi.EDI4050RespParser");
        }          
        else if(-1 != ediVersion.indexOf("3070"))
        {
            return Class.forName("com.nightfire.adapter.converter.edi.EDI3070RespParser");
        }          
        else if(-1 != ediVersion.indexOf("3072"))
        {
            return Class.forName("com.nightfire.adapter.converter.edi.EDI3070RespParser");
        }          
        else
        {
            Debug.log(Debug.ALL_WARNINGS, "Unsupported EDI version [" + ediVersion + 
                                          "]. Using default EDI parser");
            return getTransactionSetParserClass();
        }          
    }
} 

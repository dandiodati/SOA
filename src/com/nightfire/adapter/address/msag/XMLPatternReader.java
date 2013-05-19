package com.nightfire.adapter.address.msag ;

import org.w3c.dom.*;
import java.util.*;
import java.io.*;
import java.sql.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.common.xml.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;


public class XMLPatternReader
{
	private XMLMessageGenerator generator = null ;

    public XMLMessageGenerator getMessageGenerator ()
    {
    	return generator ;
    }

	public XMLPatternReader (String patternFileName)
    	throws MessageGeneratorException, AVQEngineException
    {
    	if (patternFileName == null || patternFileName.length () <= 0)
        {
            Debug.log (Debug.ALL_ERRORS, "ERROR: XMLPatternReader: XML Pattern name cannot be null or have zero length...") ;
            throw new AVQEngineException ("ERROR: XMLPatternReader: XML Pattern name cannot be null or have zero length...") ;

        }

		generator = (XMLMessageGenerator) MessageGeneratorFactory.create (Message.XML_TYPE, "lsr_preorder_response", "file:lsr.dtd") ;

		//generator = (XMLMessageGenerator) MessageGeneratorFactory.create (Message.XML_TYPE) ;

        try
        {
	        generator.setDocument (AVQEngine.getParser (FileUtils.readFile (patternFileName)).getDocument ()) ;
        }
        catch (FrameworkException ex)
        {
            throw new AVQEngineException (ex.getMessage()) ;
        }

        /*
		String header_Pattern = "address_validation_response.addressmatch.preorder_response_header" ;

       	generator.setValue (header_Pattern + ".PON",  "-") ;
        generator.setValue (header_Pattern + ".CCNA", "-") ;

		String address_info_Pattern = "address_validation_response.addressmatch.address_info" ;

    	((XMLMessageGenerator)generator).setAttributeValue (address_info_Pattern + ".WTN", "type", "phoneno") ;
   	    ((XMLMessageGenerator)generator).setAttributeValue (address_info_Pattern + ".WTN", "value", "925-210-1357") ;

   	    generator.setValue (address_info_Pattern + ".WTN.AREA", "925") ;
    	generator.setValue (address_info_Pattern + ".WTN.EXCH", "210") ;
  	    generator.setValue (address_info_Pattern + ".WTN.NUMBER", "1357") ;

		generator.setValue (address_info_Pattern + ".WIRECENTER", "-") ;
		generator.setValue (address_info_Pattern + ".RATEZONE", "-") ;
        generator.setValue (address_info_Pattern + ".TAXAREA", "-") ;
		generator.setValue (address_info_Pattern + ".COCODE", "-") ;
        generator.setValue (address_info_Pattern + ".NPA", "-") ;
        generator.setValue (address_info_Pattern + ".NXX", "-") ;
		generator.setValue (address_info_Pattern + ".COID", "-") ;
        */
    }
}

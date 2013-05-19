/*
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //gateway/main/com/nightfire/spi/common/adapter/RawLogger.java#0 $
 */
 

package com.nightfire.adapter.util;

import java.util.*;
import java.sql.*;

import org.w3c.dom.*;

import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.generator.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.message.iterator.*;
import com.nightfire.framework.message.iterator.xml.*;

/**
 * Message protocol adapters translate messages between streams and DOM object-tree representations.
 */
public  class XmlMangler  
{

  private  XmlMangler()
  {
  }


  /**
  * Looks at an XML string and pucnches in a new path to the dtd
  *
  * @param  plainxml  An XML XMLMessageParser object.
  *
  * @param  inproto  http or file.
  *
  * @param  outproto  http:// or file:.
  *
  * @param  inurl  The orignal URL to the dtd.
  *
  * @param  outurl The new  URL to the dtd.
  *
  * @return  void.
  *
  * @exception   MessageException
  */

    // the old one, is here for  future reference
    synchronized public static  String punchDTD(String plainxml,
                            String inproto,
                            String outproto,
                            String inurl,
                            String outurl)
    {

      int startofdoctype =  -1;
      startofdoctype = plainxml.indexOf("DOCTYPE");

      if (startofdoctype != -1)
      {

      }
      else
      {
         startofdoctype =  plainxml.indexOf("doctype");
         if (startofdoctype != -1)
         {

         }
         else { return null; };
      }



      int endofdtd = plainxml.indexOf(">",startofdoctype);
      String preamble  = plainxml.substring(0,endofdtd);

      int startofproto     = preamble.indexOf(inproto,startofdoctype);
      // return the same XML back if the inproto (i.e. http) not found
      if(startofproto == -1) return  plainxml;
      int startofdtd = preamble.indexOf("/dtd/",startofdoctype);



      String prepunch = preamble.substring(0,startofproto);
      String punch    = outproto  + outurl +preamble.substring(startofdtd);
      String postpunch= plainxml.substring(endofdtd);

      return prepunch+punch+postpunch;
    }

    // replaces instring by outstring 
    synchronized public static  String punchDTD2(String plainxml,
                            String instring,
                            String outstring
                            )
    {

      int startofdoctype =  -1;
      startofdoctype = plainxml.indexOf("DOCTYPE");

      if (startofdoctype < 0 )
      {
         startofdoctype =  plainxml.indexOf("doctype");
         if (startofdoctype < 0 )
         { return plainxml; };
      }

      Debug.log(Debug.XML_ERROR, " the startofdoctype,instring: " + startofdoctype+","+instring);
      int startofdtd   = plainxml.indexOf(instring,startofdoctype);
      int endofdtd     = instring.length() + startofdtd;

      if(   startofdtd > -1   )
      {
        Debug.log(Debug.XML_ERROR, " the startofdtd,endofdtd: " + startofdtd+","+endofdtd);
        String prepunch = plainxml.substring(0,startofdtd);
        String punch    = outstring;
        String postpunch= plainxml.substring(endofdtd);

        Debug.log(Debug.XML_PARSE, " the prepunch: " + prepunch);
        Debug.log(Debug.XML_PARSE, " the punch:    " + punch);
        Debug.log(Debug.XML_ERROR, " the mangled string:" + prepunch+punch+postpunch.substring(0,200));
        return prepunch+punch+postpunch;

      }
      else return plainxml;
    }

    // replaces everything starting from the instring to
    // the first >

    synchronized public static  String replaceDTD(String plainxml,
                                                 String instring,
                                                 String outstring
                            )
    {

      int startofdoctype =  -1;
      startofdoctype = plainxml.indexOf("DOCTYPE");

      if (startofdoctype < 0 )
      {
         startofdoctype =  plainxml.indexOf("doctype");
         if (startofdoctype < 0 )
         { return plainxml; };
      }


      Debug.log(Debug.XML_ERROR, " the startofdoctype,instring: " + startofdoctype+","+instring);
      int startofdtd   = plainxml.indexOf(instring,startofdoctype);
      int endofdtd     = plainxml.indexOf(">",startofdtd);

      if(   startofdtd > -1   )
      {
        Debug.log(Debug.XML_ERROR, " the startofdtd,endofdtd: " + startofdtd+","+endofdtd);
        String prepunch = plainxml.substring(0,startofdtd);
        String punch    = outstring;
        String postpunch= plainxml.substring(endofdtd);

        Debug.log(Debug.XML_PARSE, " the prepunch: " + prepunch);
        Debug.log(Debug.XML_PARSE, " the punch:    " + punch);
        Debug.log(Debug.XML_ERROR, " the mangled string:" + prepunch+punch+postpunch.substring(0,200));
        return prepunch+punch+postpunch;

      }
      else return plainxml;
    }

    // delete the DOCTYPE line

    synchronized public static  String deleteDOCTYPE(String plainxml)
    {

      int startofdoctype =  -1;
      int openbracket = -1;
      startofdoctype = plainxml.indexOf("DOCTYPE");

      if (startofdoctype < 0 )
      {
         startofdoctype =  plainxml.indexOf("doctype");
         if (startofdoctype < 0 )
         { return plainxml; };
      }

      openbracket = plainxml.substring(0,startofdoctype).lastIndexOf("<");
      if (openbracket < 2 )
      { return plainxml; };

      int endofdtd     = plainxml.indexOf(">",openbracket);

      if(   openbracket > -1   )
      {
        Debug.log(Debug.XML_STATUS, " the startofdtd,endofdtd: " + openbracket+","+endofdtd);
        //Fixed bug here that the prepunch should be substring (0,openbracket),
        //because substring(int, int) takes the last parameter -1 as the last
        //character to pick up.
        String prepunch = plainxml.substring(0,openbracket);
        String postpunch= plainxml.substring(endofdtd + 1);

        Debug.log(Debug.XML_STATUS, " the prepunch:**" + prepunch+"**");
        //Debug.log(Debug.XML_ERROR, " the mangled string:" + prepunch+postpunch.substring(0,200));
        Debug.log(Debug.XML_STATUS, " the mangled string:" + prepunch+postpunch);
        return prepunch+postpunch;

      }
      else return plainxml;
    }

}

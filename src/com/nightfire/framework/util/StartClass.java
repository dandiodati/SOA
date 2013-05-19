
/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/framework/util/StartClass.java#1 $
 */

package com.nightfire.framework.util;


import java.io.*;


/**
 * Find the class where main is started.
 */
public class StartClass
{
	public static final String SOURCE_ID = "NOT AVAILABLE"; // $Id: //nfcommon/R4.4/com/nightfire/framework/util/StartClass.java#1 $";
	
    public static String getStartClassName () {
    	Throwable e = new Throwable();
    	//e.printStackTrace();
    	CharArrayWriter caw = new CharArrayWriter();
    	e.printStackTrace(new PrintWriter(caw));
    	String theTrace = caw.toString();
    	int lastLeftP = theTrace.lastIndexOf("(");
    	theTrace = theTrace.substring(0, lastLeftP);
    	int lastSpace = theTrace.lastIndexOf(" ");
    	int lastDot = theTrace.lastIndexOf(".");
    	theTrace = theTrace.substring(lastSpace+1, lastDot);
    	return theTrace;
    }
    
    public static void main (String[] args) {
    	System.out.println(getStartClassName());
    }
}

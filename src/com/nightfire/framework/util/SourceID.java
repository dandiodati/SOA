
/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 * $Header: //nfcommon/R4.4/com/nightfire/framework/util/SourceID.java#1 $
 */

package com.nightfire.framework.util;

import java.lang.reflect.*;

/*
 * Utility class to find the SOURCE ID string.
 */
public class SourceID
{
	public static final String SOURCE_ID = "NOT AVAILABLE"; // "$Id: //nfcommon/R4.4/com/nightfire/framework/util/SourceID.java#1 $";
	
	public static final String ID_FIELD = "SOURCE_ID";
    public static void main (String[] args) {
    	if (args.length != 1) {
    		System.err.println("Usage: java " +  StartClass.getStartClassName() + " className");
    		return;
    	}
    	
    	String className = args[0];
    	
    	Class theClass = null;
    	Field theField = null;
    	Object theValue = null;
    	try {
    		theClass = Class.forName(className);
    		//System.out.println("Loaded class " + theClass.getName());
    	}
    	catch (ClassNotFoundException e) {
    		System.err.println("Error: class " + className + " not found.");
    		return;
    	}
    	
    	try {
    		theField = theClass.getDeclaredField(ID_FIELD);
    		//System.out.println("Found field: " + theField.getName());
    	}
    	catch (NoSuchFieldException e) {
    		System.err.println("Error: class " + className + " contains no ID.");
    		return;
    	}
    	
    	try {
    		theValue = theField.get(null);
    	}
    	catch (Exception e) {
    		System.out.println("Caught Exception: " + e);
    		e.printStackTrace();
    	}
    
    	System.out.println(theValue);
    }
}

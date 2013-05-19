/*
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.version;


public class Version
{
    public Version( )
    {
    }


    public String toString( )
    {
	return "AsrExpress 1.0_Beta-B18 $Date: 3/03/99 5:42p $";
    }


    public static void main( String[] args )
    {
        System.out.println( "" + new Version() );
    }
}

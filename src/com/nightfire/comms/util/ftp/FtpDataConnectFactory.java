/*
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */


package com.nightfire.comms.util.ftp;
import com.nightfire.framework.util.*;


/**
 *  Factory class that create a normal ftp object
 * or ftp over ssl object. Can be used in the future to provide
 * caching support.
 */
public class FtpDataConnectFactory
{

    
    /**
     * Creates a FtpDataConnect class that can be used for 
     * FTP connectivity.
     * 
     * @return a <code>FtpDataConnect</code> value
     * @exception FrameworkException If an invalid type or object is passed in.
     */
    public static FtpDataConnect createFTP() throws FrameworkException
    {
        return new FtpDataClient();
    }

    /**
     * Creates a FtpDataConnect class that can be used for 
     * FTP over SSL connectivity.
     * 
     * @param info A ssl configuration object.
     * @return a <code>FtpDataConnect</code> value
     * @exception FrameworkException If an invalid type or object is passed in.
     */
    public static FtpDataConnect createSSLFTP(SSLConfigInfo info) 
    {
        return new FtpSSLDataClient(info);
    }
    
    
    
}


/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.util.ftp;


import com.nightfire.framework.util.*;


/**
 * Base class for exceptions used by communications I/O package.
 */
public class FtpException extends FrameworkException
{
    /**
     * Create a message exception object with the given message.
     *
     * @param  msg  Error message associated with exception.
     */
    public FtpException ( String msg )
    {
        super( msg );
    }


    /**
     * Create a message exception object with the given exception's message.
     *
     * @param  e  Exception object used in creation.
     */
    public FtpException ( Exception e )
    {
        super( e );
    }
}

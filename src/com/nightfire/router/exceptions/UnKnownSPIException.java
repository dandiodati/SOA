/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */


package com.nightfire.router.exceptions;

import com.nightfire.common.*;

/**
 * Thrown when an SPI server is not found or doesn't exist.
 *
 * @author Dan Diodati
 */
public class UnKnownSPIException extends Exception
{

   public UnKnownSPIException(String msg)
  {
     super(msg);
  }
}

/*
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia;

// JDK import
import java.io.*;

// Nightfire import
import com.nightfire.common.*;
import com.nightfire.framework.util.*;

/*
* This class creates a unique file name.
*/
public class UniqueFileGenerator
{
  /*
  * This method returns a file handle given path, base name and extension.
  *
  * @param  path      Path of the file
  *
  * @param  baseName  Base name of the file
  *
  * @param  extension Extension of the file
  *
  * @exception  ProcessingException   Thrown when file can not be created
  */
  public static File getUniqueFile(String path, String baseName, String extension)
    throws ProcessingException
  {
    File outputFile = null;

    if( path == null )
      path = System.getProperty("user.dir");

    if( baseName.length() < 3)
      baseName = StringUtils.padString(baseName, 3, false, '0');

    try
    {
      outputFile = File.createTempFile( baseName, extension, new File(path) );
    }
    catch (Exception e)
    {
      throw new ProcessingException("ERROR: UniqueFileGenerator.getUniqueFile: " +
        "Can not create file: " + e.getMessage());
    }
    return outputFile;
  }
}





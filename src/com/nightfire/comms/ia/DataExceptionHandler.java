/*
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.comms.ia;

// JDK import
import java.io.*;
import java.util.*;

// Nightfire import
import com.nightfire.framework.db.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.message.MessageException;
import com.nightfire.comms.ia.asn.*;


/*
* This class handles data when error occurs during processing.
* When error occurs, the data is saved in a file.
* If the error is caused by MessageException, the file is saved
* in error directory; if the error is anything but MessageException,
* the file is saved in exception directory.
*/
public class DataExceptionHandler   {

  /*
  * File base name
  */
  private String baseName;

  /*
  * File extension
  */
  private String extension;

  /*
  * Exception directory name
  */
  private String exceptionDir;

  /*
  * Error directory name
  */
  private String errorDir;

  /*
  * Persistent property
  */
  private Hashtable persistentProp;

  /*
  * Constructor.
  */
  public DataExceptionHandler(String baseName, String extension, String exceptionDir, String errorDir)
  {
    this.baseName = baseName;
    this.extension = extension;
    this.exceptionDir = exceptionDir;
    this.errorDir = errorDir;
  }


  /**
  * Handles exception depending on exception type.
  *
  * @param  ex    Exception
  *
  * @param  data  String input
  */
  public void handleException(Exception ex, String data)
  {
    handleException(ex, data.getBytes());
  }

  /**
  * Handles exception depending on exception type.
  *
  * @param  ex    Exception
  * @param  data  String input
  * @param  encoding the character encoding to be used when extracting the
  *                  bytes from the given data. 
  *
  */
  public void handleException(Exception ex, String data, String encoding)
  {
    if( StringUtils.hasValue( encoding ) )
    {
       try
       {
          // get the data using the given character encoding
          handleException(ex, data.getBytes(encoding));
       }
       catch(java.io.UnsupportedEncodingException ueex)
       {
          Debug.error("["+encoding+"] is not a valid character encoding: "+
                      ueex);
          handleException(ex, data.getBytes());                      
       }
    }
    else
    {
       // no particular character encoding was specified
       handleException(ex, data.getBytes());
    }
  }


  /*
  * Handles exception depending on exception type.
  *
  * @param  ex    Exception
  *
  * @param  data  byte[] input
  *
  * @exception ProcessingException  Thrown when fail to write to a file
  */
  public void handleException(Exception ex, byte[] data)
  {
    try
    {
      File outputFile = null;

      if( ex instanceof MessageException )
      {
        outputFile = UniqueFileGenerator.getUniqueFile(errorDir, baseName, extension);
        Debug.log(this, Debug.IO_STATUS, "DataExceptionHandler.handleException: " +
          "write binary data to error directory");
      } else
      {
        outputFile = UniqueFileGenerator.getUniqueFile(exceptionDir, baseName, extension);
        Debug.log(this, Debug.IO_STATUS, "DataExceptionHandler.handleException: " +
          "write binary data to exception directory");
      }

      String absPath = outputFile.getAbsolutePath();

      FileUtils.writeBinaryFile(absPath, data);
    } catch (Exception e)
    {
      Debug.log(this, Debug.IO_ERROR, "ERROR: DataExceptionHandler.handleException: Can not write to file"
        + e.getMessage() + "\n");
      Debug.log(this, Debug.IO_ERROR, "Original exception: " + ex.getMessage() + "\n");
      Debug.log(this, Debug.IO_ERROR, "Data being processed: [" + new HexFormatter(data) + "]\n");
    }
  }

}


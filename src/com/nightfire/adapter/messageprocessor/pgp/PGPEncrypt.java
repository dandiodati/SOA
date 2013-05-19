/* PGPEncrypt class
 * This adapter class calls C library function for encrypting the source data.
 */
package com.nightfire.adapter.messageprocessor.pgp;

import com.nightfire.common.*;
import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import java.io.*;
import java.util.*;

/**
 * PGPEncrypt class.
 * extends PGPBase .
 */
public class PGPEncrypt extends PGPBase {

  //Key email identifies the public key, that will be used for encryption.
  private final static String KEY_EMAIL_PROP = "KEY_EMAIL";

  private String keyEmail   = "";

  private final boolean testing = true;

  public PGPEncrypt() {
  }

  /**
   * This method loads properties in virtual machine.
   * @param key - Key value of persistentproperty.
   * @param type - Type value of persistentproperty.
   * @exception - ProcessingException - this exception is thrown by super class
   *              for any problem with initialization .
   */
  public void initialize ( String key, String type ) throws ProcessingException
  {
    Debug.log( this, Debug.DB_STATUS, "Loading properties for PGP Encrypt module.");

    super.initialize(key, type);

    keyEmail            = (String)adapterProperties.get(KEY_EMAIL_PROP);

  if(!(StringUtils.hasValue(keyEmail)))
    {
       Debug.log( this, Debug.ALL_ERRORS, "ERROR: PGPEncrypt: Key Email in PGP properties is missing.");
       throw new ProcessingException("ERROR: PGPEncrypt: Key Email in PGP properties is missing.");
    }

    Debug.log( this, Debug.DB_STATUS, "Loaded properties for PGP encrypt.");
  }

  /**
   * This method is called from driver.
   * It calls encrypt method of PGPBase class, which inturn calls native_encrypt function.
   * @param context - MessageProcessorContext.
   * @param type - input - contains String to be encrypted.
   * @return NVPair[] - this array contains only one instance of NVPair.
   *                    name - value of NEXT_PROCESSOR_NAME; value - encrypted byte array.
   * @exception - ProcessingException is thrown from C routines for any error.
   */
  public NVPair[] execute ( MessageProcessorContext context, Object input ) throws MessageException, ProcessingException
  {

    Debug.log( this, Debug.BENCHMARK, "PGPEncrypt: Starting encryption process");
    if(input == null)
    {
      return null;
    }

    String inputString = "" ;
    byte[] encryptedString ;

    try
    {
      inputString = Converter.getString(input);
      if(inputString.equals(""))
      {
        throw new ProcessingException("ERROR: PGPEncrypt: Input to process method is a null string");
      }
    }
    catch(ClassCastException exp)
    {
      throw new ProcessingException("ERROR: PGPEncrypt: Input to process method is not a valid string");
    }

    try
    {
      byte[] array = inputString.getBytes();
      encryptedString = encrypt(array,keyEmail, ringDirectory+"/");
    }
    catch(Exception exp)
    {
      throw new ProcessingException("ERROR: PGPEncrypt: Error in encryption" + exp.getMessage());
    }

    Debug.log( this, Debug.BENCHMARK, "PGPEncrypt: Encryption done.");
    //Generate NVPair to return

    return formatNVPair(encryptedString);
  }

  public static void main(String[] args)
  {
    if(args.length != 7)
    {
       System.out.println("usage: java PGPEncrypt dtabaseurl dbuser password propertyKey propertyType sourceFile");
       return;
    }
    try
    {
       DBInterface.initialize(args[0],args[1],args[2]);
    }
    catch(Exception exp)
    {
      String e = exp.toString();
      e = exp.getMessage();
      exp.printStackTrace();
    }

    PGPEncrypt encryption = new PGPEncrypt();
    try
    {
      encryption.initialize(args[3],args[4]);
      NVPair[] dpair = encryption.execute(null,FileUtils.readFile(args[5]));
      byte[] result = ((byte[])dpair[0].value);

      FileUtils.writeBinaryFile(args[6],result);

      System.out.println("Press Enter to quit." );
      System.in.read();
    }
    catch(Exception exp)
    {
       exp.printStackTrace();
    }
  }

}


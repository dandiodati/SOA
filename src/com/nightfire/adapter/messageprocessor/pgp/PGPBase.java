package com.nightfire.adapter.messageprocessor.pgp;

import com.nightfire.spi.common.driver.*;
import com.nightfire.framework.util.*;
import com.nightfire.common.*;

/**
 * This is base class for PGPEncrypt & PGPDecryt classes.
 * This class loads the pgp.dll(NT) or pgp.lib(Unix) -  C-runtime library.
 * The encrypt/decrypt functions inturn calls native_encrypt/native_decrypt.
 * These are native functions & calls C routines for encrypt/decrypt procedures.
 */
public abstract class PGPBase extends MessageProcessorBase implements  PGPCrypt
{
  //name of the nexr process
  private final static String NEXT_PROCESSOR_NAME_PROP = "NEXT_PROCESSOR_NAME";

  //name of this process.
  private final static String NAME_PROP        = "NAME";

  //Name of the directory where rings are present
  private final static String RING_DIR_PROP        = "RING_DIR_PATH";

  protected String nextProcessor = "";
  protected String name   = "";
  protected String ringDirectory = "";

  native byte[] native_encrypt(byte[] message, String key, String ringDir);
  native byte[] native_decrypt(byte[] message, String key, String ringDir);

  public byte[] encrypt(byte[] message, String key, String ringDir) throws ProcessingException
  {
    return native_encrypt(message, key, ringDir);
  }

  public byte[] decrypt(byte[] message, String key, String ringDir)
  {
    return  native_decrypt(message, key, ringDir);
  }

  /**
   * Load pgp dynamic library.
   */
  static {
    System.loadLibrary("pgp");
  }

  public void initialize ( String key, String type ) throws ProcessingException
  {
    super.initialize(key, type);
    nextProcessor       = (String)adapterProperties.get(NEXT_PROCESSOR_NAME_PROP);
    name                = (String)adapterProperties.get(NAME_PROP);
    ringDirectory       = (String)adapterProperties.get(RING_DIR_PROP);
    if(!(StringUtils.hasValue(nextProcessor) && StringUtils.hasValue(name) && StringUtils.hasValue(ringDirectory)))
    {
       Debug.log( this, Debug.ALL_ERRORS, "ERROR: PGPBase: One or more PGP properties are missing.");
       throw new ProcessingException("ERROR: PGPBase: One or more properties from persistentproperty" +
                                     " could not be loaded or are null");
    }
  }
}

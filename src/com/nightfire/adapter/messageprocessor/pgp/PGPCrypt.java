package com.nightfire.adapter.messageprocessor.pgp;

import com.nightfire.common.*;

/**
 * An interface providing function declaration of encrypt/decrypt methods.
 */
public interface PGPCrypt
{
  public byte[] encrypt(byte[] message, String key, String ringDir) throws ProcessingException ;
  public byte[] decrypt(byte[] message, String key, String ringDir)throws ProcessingException ;
}

/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * Author: Richard Southwick
 *
 * $Header: //nfcommon/com/nightfire/framework/util/rot13.java#1 $
 */

package com.nightfire.framework.util;

import sun.misc.BASE64Encoder;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/*
 * Utility for string encryption/decription
 */
public class Crypt
{
   private static Cipher m_encryptCipher = null;
   private static Cipher m_decryptCipher = null;
   private static final String ENCODING = "ISO8859_1";

   public static synchronized void createKey (String file)
      throws FrameworkException
   {
      byte[] raw = null;

      // Install SunJCE provider
      Provider sunJce = new com.sun.crypto.provider.SunJCE();
      Security.addProvider(sunJce);

      // create the key in byte form
      try
      {
         KeyGenerator kgen = KeyGenerator.getInstance("Blowfish");
         SecretKey skey = kgen.generateKey();
         raw = skey.getEncoded();
      }
      catch (NoSuchAlgorithmException ae)
      {
         throw new FrameworkException (ae.toString());
      }

      // check whether the file already exists, otherwise write key to file
      File f = new File(file);
      if (f.exists())
         throw new FrameworkException ("key file already exists");
      else
         FileUtils.writeBinaryFile (file, raw);
   }


   public static synchronized void getSecretKey() throws FrameworkException
   {
      getSecretKey(null);
   }

   public static synchronized void getSecretKey(String file) throws FrameworkException
   {
      String fileName = file;

      if (m_encryptCipher != null)
         return;

      try
      {
         Provider sunJce = new com.sun.crypto.provider.SunJCE();
         Security.addProvider(sunJce);

         if (fileName == null)
         {
            String fileSeparator = System.getProperty("file.separator");
            fileName = "."+ fileSeparator+"config"+fileSeparator+"cchelper.txt";
         }
         byte[] skey = FileUtils.readBinaryFile (fileName);

         SecretKeySpec sKeySpec = new SecretKeySpec(skey, "Blowfish");

         m_encryptCipher = Cipher.getInstance("Blowfish");
         m_encryptCipher.init(Cipher.ENCRYPT_MODE, sKeySpec);

         m_decryptCipher = Cipher.getInstance("Blowfish");
          m_decryptCipher.init(Cipher.DECRYPT_MODE, sKeySpec);
         Debug.log( Debug.IO_STATUS, "Setup ciphers for decryption and encryption");
      }
      catch (Exception e)
      {
         m_encryptCipher = null;
         m_decryptCipher = null;
            Debug.log( Debug.IO_STATUS, "Couldn't create secret key from file [" + fileName + "] ..." );
         throw new FrameworkException (e.toString());
      }
   }


    /**
     * Checks if secret key is loaded.
     *
     * @return  TRUE if loaded, FALSE otherwise.
     */
    public static final boolean isSecretKeyLoaded()
   {
      if (m_encryptCipher != null && m_decryptCipher != null)
         return true;
      else
         return false;
   }


    /**
     * Encrypt a string by using a secret key.
     *
     * @param  str  The string to encrypt
     * @return  A string containing the encrypted value.
     */

    public static final String jceEncrypt(String str) throws FrameworkException
    {
       try
       {
          byte[] enc = m_encryptCipher.doFinal(str.getBytes(ENCODING));
          return new String(enc, ENCODING);
       }
       catch (Exception e)
       {
          Debug.log( Debug.IO_STATUS, "Couldn't encrypt String [" + str + "] ..." );
          throw new FrameworkException(e.toString());
       }
    }


  /**
   * Encrypt a string by using a one-way hash function.
   *
   * @param  str  The string to encrypt
   * @return  A string containing the encrypted value.
   */

   public static final String hash(String str)
   {
      try
      {
         MessageDigest sha = MessageDigest.getInstance("SHA");
         byte[]hash = sha.digest(str.getBytes());
         return new String(hash);
      }
      catch (NoSuchAlgorithmException e)
      {
         Debug.log( null, Debug.ALL_ERRORS, e.toString());
      }

      return str;
   }


    /**
     * Encrypt a string by using a one-way SHA algorithm.
     *         also Encodes the encrypting string with Base64.
     *
     * @param  plaintext  The string to encrypt
     * @return  A string containing the encrypted value.
     */
 public static String encryptViaSHA(String plaintext) throws FrameworkException
	{
		MessageDigest md = null;
		try
		{
			md = MessageDigest.getInstance("SHA");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			throw new FrameworkException("Error loading MessageDigest"+ e);
		}
		try
		{
			md.update(plaintext.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			throw new FrameworkException("Error encrypting"+ e);
		}

		byte raw[] = md.digest();
		String hash = (new BASE64Encoder()).encode(raw);
		return hash;
	}

    /**
     * Decrypt a string by using a secret key.
     *
     * @param  str  The string to decrypt
     * @return  A string containing the decrypted value.
     */

    public static final String jceDecrypt(String str) throws FrameworkException

    {
       try
       {
          byte[] dec = m_decryptCipher.doFinal(str.getBytes(ENCODING));
          return new String(dec,ENCODING);
       }
       catch (Exception e)
       {
          Debug.log( Debug.IO_STATUS, "Couldn't decrypt String [" + str + "] ..." );
          throw new FrameworkException(e.toString());
       }
    }


    /**
     * Check if string is encrypted.
     *
     * @param  str  The string to check
     * @return  TRUE if encrypted, FALSE otherwise.
     */
   public static final boolean jceIsEncrypted(String str)
   {
      boolean encrypted = false;

      if (StringUtils.hasValue(str))
      {
         try
         {
            String decrypted = Crypt.jceDecrypt(str);
            encrypted = true;
         }
         catch (FrameworkException e)
         {
            Debug.log(Debug.MSG_STATUS, "String [" + str + "] is not encrypted.");
         }
      }
      else
      {
         Debug.log(Debug.MSG_WARNING, "Couldn't determine if string [" + str + "] is " +
            "encrypted. String has no value.");
      }

      return encrypted;
   }


    /**
     * Encrypt a string by rotating by 13 characters.
     *
     * @param  str  The string to encrypt
     * @return  A string containing the encrypted value.
     */

    public static final String encrypt(String str)
    {
        return rot13(str);
    }

    /**
     * Decrypt a string by rotating by 13 characters.
     *
     * @param  str  The string to decrypt
     * @return  A string containing the decrypted value.
     */

    public static final String decrypt(String str)
    {
        return rot13(str);
    }


    private static final String rot13(String str)
    {

        if (str == null || str.equals("")) {

            return "";

        }

        StringBuffer sb = new StringBuffer( );
        int abyte = 0;

        for ( int i=0; i < str.length(); i++ )
            {
                abyte = (int)str.charAt(i);

                // Is this a capital letter?
                int cap = abyte & 32;
                // Make it a capital letter
                abyte &= ~cap;
                // rotate by 13, then restore to upper or lowercase region of alphabet
                abyte = ((abyte >= 'A') && (abyte <= 'Z') ?
                         ((abyte - 'A' + 13) % 26 + 'A') : abyte) | cap;

                sb.append( (char)abyte );
            }
        return( sb.toString() );
    }


   public static void main (String args[])
   {
      String s = "Hello there!";
      try
      {
         getSecretKey(null);
         System.out.println("Loaded secret key...");

         System.out.println("********************************");

         String t1 = jceEncrypt(s);
         System.out.println("Encrypted: " + t1);

         String t2 = jceDecrypt(t1);
         System.out.println("Decrypted: " + t2);

         System.out.println("********************************");

         boolean t3 = jceIsEncrypted(t1);
         System.out.println("Is first one encrypted? " + t3);

         boolean t4 = jceIsEncrypted(t2);
         System.out.println("Is second one  encrypted? " + t4);
      }
      catch (FrameworkException e)
      {
         System.out.println("Process failed. Got EXC:\n ");
         e.printStackTrace();
      }
   }
}

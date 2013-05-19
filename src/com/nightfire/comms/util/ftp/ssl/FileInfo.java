/*
 * Copyright (c) 2000 Nightfire Software, Inc. All rights reserved.
 *
 */
 

package com.nightfire.comms.util.ftp.ssl;

import java.util.*;

/**
 *  FileInfo:  Encapsulates File Information used by FTP SSL Comm Interface for file listing and
 *             identifying files that match the criteria of a FileInfo template
 */


public class FileInfo
{

   // use these to pass into setFileType
   public static final char FILE = '-';
   public static final char DIRECTORY = 'd';
   public static final char SYMBOLIC_LINK = 'l';
   public static final char DOOR   = 'D';
   public static final char BLOCK_SPECIAL_FILE   = 'b';
   public static final char CHAR_SPECIAL_FILE   = 'c';
   public static final char FIFO   = 'p';   // named pipe
   public static final char SOCKET   = 's';  //AF_UNIX address family socket
   public static final char UNKNOWN     = ' ';

   public static final char ANY     = 'A';

   // possible fileinfo fields to change
   // <b> NOTE: these default setting when used in a template will match
   // everything</b>
   public String permission = null;
   public String account  = null;        //used by VAN only
   public String user  = null;
   public String group = null;
   public int    size  = -1;
   public String month = null;
   public int    day   = -1;
   public String time  = null;
   public String name  = null;
   public String regExpName = null;
    // Regular expression for entire-line comparisons.
    public String lineRegExp = null;

   // need to set type with method
   private char type    = ANY;

   // string defs used by constructor
   private static final String PERMISSION = "permission";
   private static final String USER = "user";
   private static final String GROUP = "group";
   private static final String ACCOUNT = "account";
   private static final String MONTH = "month";
   private static final String TIME = "time";
   private static final String NAME = "name";
   private static final String REG_EXP_NAME = "reg_exp_name";
   private static final String SIZE = "size";
   private static final String DAY = "day";

   /**
    * default constructor
    */
   public FileInfo() {}

   /**
    *constructor used for building FileInfo Filter Template
    */
   public FileInfo(Hashtable ht) {

    Enumeration e = ht.keys();
    String s;

    while( e.hasMoreElements() ) {
      s = (String)( e.nextElement() );

      if ( s.equalsIgnoreCase(USER) )
        user = (String)ht.get(s);

      if ( s.equalsIgnoreCase(GROUP) )
          group = (String) ht.get(s);

      if ( s.equalsIgnoreCase(ACCOUNT) )
          account = (String) ht.get(s);

      if ( s.equalsIgnoreCase(NAME) )
          name = (String) ht.get(s);

      if ( s.equalsIgnoreCase(REG_EXP_NAME) )
          regExpName = (String) ht.get(s);

      if ( s.equalsIgnoreCase(TIME) )
          time = (String) ht.get(s);

      if (s.equalsIgnoreCase(PERMISSION) )
          permission = (String) ht.get(s);

      if (s.equalsIgnoreCase(MONTH) )
          month = (String) ht.get(s);

      if (s.equalsIgnoreCase(SIZE) )
          size = Integer.parseInt((String) ht.get(s) );

      if (s.equalsIgnoreCase(DAY) )
          day =  Integer.parseInt((String) ht.get(s) );
    }

   }


   /**
    * returns a string representation of the file type
    */
   public String getFileTypeString()
   {
      return (convertFileType(type) );
   }

   // returns file type for comparison to static defintions
   public char getFileType()
   {
      return (type);
   }

   /**
    * returns a string version of the file type
    */
   private String convertFileType(char type)
   {
      String typeStr = null;

      switch( type) {
         case FILE:
            typeStr = "FILE";
            break;
         case DIRECTORY:
            typeStr = "DIRECTORY";
            break;
         case SYMBOLIC_LINK:
            typeStr = "SYMBOLIC LINK";
            break;
         case DOOR:
            typeStr = "DOOR";
            break;
         case BLOCK_SPECIAL_FILE:
            typeStr = "BLOCK SPECIAL FILE";
            break;
         case CHAR_SPECIAL_FILE:
            typeStr = "CHARACTER SPECIAL FILE";
            break;
         case FIFO:
            typeStr = "FIFO (NAMED PIPE)";
            break;
         case SOCKET:
            typeStr = "SOCKET";
            break;
         case UNKNOWN:
            typeStr = "UNKNOWN";
            break;
         default:
            typeStr = "UNKNOWN";
            break;


      }
      return typeStr;
   }

   /**
    * set the file type on this fileInfo object.
    * @param t the file type. Used the static constants defined at the top.
    */
   public void setFileType(char t) {
   
   switch(t) {
         case FILE:
            type = FILE;
            break;
         case DIRECTORY:
            type = DIRECTORY;
            break;
         case SYMBOLIC_LINK:
            type = SYMBOLIC_LINK;
            break;
         case DOOR:
            type = DOOR;
            break;
         case BLOCK_SPECIAL_FILE:
            type = BLOCK_SPECIAL_FILE;
            break;
         case CHAR_SPECIAL_FILE:
            type = CHAR_SPECIAL_FILE;
            break;
         case FIFO:
            type = FIFO;
            break;
         case SOCKET:
            type = SOCKET;
            break;
         case UNKNOWN:
            type = UNKNOWN;
            break;
         default:
            type = UNKNOWN;
            break;
      }
   }
}

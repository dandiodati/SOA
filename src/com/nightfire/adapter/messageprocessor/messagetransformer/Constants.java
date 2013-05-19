/**
 * Copyright(c) 2000 Nightfire Software, Inc.
 * All rights reserved.
 */
package com.nightfire.adapter.messageprocessor.messagetransformer;

public interface Constants {

   /**
   * The name of the ERROR output card populated by the maps.
   */
   public static final String ERROR = "ERROR_MESSAGE";

   /**
   * If this String is found in the contents of an ERROR card, then the
   * card is considered to be populated. If this String is missing, then the
   * ERROR card will be considered to be empty.
   */
   public static final String ERROR_MESSAGE = "ErrorMessage";

   /**
   * The system specific file separator.
   */
   public static final char FILE_SEPARATOR = System.getProperty("file.separator").charAt(0);


} 
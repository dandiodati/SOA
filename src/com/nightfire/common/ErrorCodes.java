/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.common;


/**
 * Error codes to be used by the processors in SupplierExpress
 */
 public class ErrorCodes
 {
   /**
    * The error type is unknown or error could have more than one error types.
    * This is typically used with processing or System errors.
    */
    public static final int UnknownError = 0;

   /**
    * The error type is unknown or error could have than one error types.
    * This is typically used with errors in data processing.
    */
    public static final int UnknownDataError = 1;

   /**
    * The error is due to a database failure.
    * This is typically used with processing or System errors.
    */
    public static final int DatabaseError = 2;

   /**
    * The error is due to a failure in the communication system.
    * This is typically used with processing or System errors.
    */
    public static final int CommunicationsError = 3;

   /**
    * The error is due to denial of access to a resource.
    * This is typically used with processing or System errors.
    */
    public static final int AccessDeniedError = 4;

   /**
    * The data being processed is malformed.
    */
    public static final int MalformedDataError = 5;

   /**
    * Expected data is missing.
    */
    public static final int MissingDataError = 6;

   /**
    * Data being processed is invalid. This could be used in place of the
    * MalformedDataError, if the data is unidentifiable.
    */
    public static final int InvalidDataError = 7;
    
 }//end of Class ErrorCodes

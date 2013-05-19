/*
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: //comms/R4.4/com/nightfire/comms/soap/SOAPConstants.java#1 $
 */

package com.nightfire.comms.soap;

/**
 * This class contains constants needed across components.
 */
public class SOAPConstants
{

    /**
     * Contant to indicate a time out has occured on the event channel.
     */
    public static final String TIMED_OUT = "TIMED_OUT";

       /**
     * The FaultCode to be used in case processing if method is not supported.
     */
    public static final String METHOD_NOT_SUPPORTED_FAULT         = "SOAP-ENV:Server.MethodNotSupported";

    /**
     * The FaultCode to be used in case processing fails due to message.
     */
    public static final String MESSAGE_FAULT         = "SOAP-ENV:Server.MessageException";

    /**
     * The FaultCode to be used in case processing fails due to some system failure.
     */
    public static final String PROCESSING_FAULT      = "SOAP-ENV:Server.ProcessingException";

    /**
     * The FaultCode to be used in case a null pointer was encountered while processing.
     */
    public static final String NULL_FAULT            = "SOAP-ENV:Server.NullPointerException";

    /**
     * The FaultCode to be used in case of a security exception.
     */
    public static final String SECURITY_FAULT            = "SOAP-ENV:Server.SecurityException";
    
    /**
     * The FaultCode to be used in case of a communications error.
     */
    public static final String COMMUNICATIONS_FAULT  = "SOAP-ENV:Server.CommunicationsException";

    /**
     * Method invoked on SOAP service.
     */
    public static final String PROCESS_SYNC = "processSync";

    /**
     * Method invoked on SOAP service.
     */
    public static final String PROCESS_ASYNC = "processAsync";

    /**
     * Default timeout value in milliseconds to set on the HTTP socket(not a connection timeout value).
     * This value will be used when making a SOAP call to post the response to the customer's
     * SOAP endpoint. This value is currently not-configurable.
     */
    public static final int DEFAULT_TIMEOUT = 300000;

}

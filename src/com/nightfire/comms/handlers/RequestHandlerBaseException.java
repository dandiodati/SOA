package com.nightfire.comms.handlers;

import com.nightfire.common.ProcessingException;

public class RequestHandlerBaseException extends ProcessingException
{
	public RequestHandlerBaseExceptionType errorType;
	public java.lang.String header = "";
	public java.lang.String errorMessage = "";

    /**
     * Create a Domain Properties exception object with the
     * given message.
     *
     * @param msg Error message associated with exception.
     * @param errorType type of the error {@link RequestHandlerBaseExceptionType}
     * @param header information of the header
     * @param errorMessage error message in the form of string
     */
    public RequestHandlerBaseException(String msg, RequestHandlerBaseExceptionType errorType,
                                       String header, String errorMessage) {
        super (msg);
        setAttributes (errorType, header, errorMessage);
    }

    /**
     * Create a Domain Properties exception object with the
     * given exception's message.
     *
     * @param e Exception object used in creation.
     * @param errorType type of the error {@link RequestHandlerBaseExceptionType}
     * @param header information of the header
     * @param errorMessage error message in the form of string
     */
    public RequestHandlerBaseException(Exception e, RequestHandlerBaseExceptionType errorType,
                                       String header, String errorMessage) {
        super (e);
        setAttributes (errorType, header, errorMessage);
    }

    private void setAttributes(RequestHandlerBaseExceptionType errorType, String header, String errorMessage)
    {
        this.errorType = errorType;
        this.header = header;
        this.errorMessage = errorMessage;
    }

}

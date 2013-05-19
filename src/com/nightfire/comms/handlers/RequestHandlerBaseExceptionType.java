package com.nightfire.comms.handlers;

public final class RequestHandlerBaseExceptionType
{
	private int value = -1;
	public static final int _UnknownDataError = 0;
	public static final RequestHandlerBaseExceptionType UnknownDataError = new RequestHandlerBaseExceptionType(_UnknownDataError);
	public static final int _MalformedDataError = 1;
	public static final RequestHandlerBaseExceptionType MalformedDataError = new RequestHandlerBaseExceptionType(_MalformedDataError);
	public static final int _MissingDataError = 2;
	public static final RequestHandlerBaseExceptionType MissingDataError = new RequestHandlerBaseExceptionType(_MissingDataError);
	public static final int _InvalidDataError = 3;
	public static final RequestHandlerBaseExceptionType InvalidDataError = new RequestHandlerBaseExceptionType(_InvalidDataError);
	public static final int _OtherError = -1;
	public static final RequestHandlerBaseExceptionType OtherError = new RequestHandlerBaseExceptionType(_OtherError);

	public int value()
	{
		return value;
	}
	public static RequestHandlerBaseExceptionType from_int(int value)
	{
		switch (value) {
			case _UnknownDataError: return UnknownDataError;
			case _MalformedDataError: return MalformedDataError;
			case _MissingDataError: return MissingDataError;
			case _InvalidDataError: return InvalidDataError;
			default: return OtherError;
		}
	}
	protected RequestHandlerBaseExceptionType(int i)
	{
		value = i;
	}
}

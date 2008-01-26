package edu.usc.glidein.service;

public class ServiceException extends Exception
{
	private static final long serialVersionUID = 2767483133159443163L;

	public ServiceException()
	{
		super();
	}

	public ServiceException(String message, Throwable throwable)
	{
		super(message, throwable);
	}

	public ServiceException(String message)
	{
		super(message);
	}

	public ServiceException(Throwable throwable)
	{
		super(throwable);
	}
}

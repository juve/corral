package edu.usc.glidein;

public class GlideinException extends Exception
{
	private static final long serialVersionUID = 2767483133159443163L;

	public GlideinException()
	{
		super();
	}

	public GlideinException(String message, Throwable throwable)
	{
		super(message, throwable);
	}

	public GlideinException(String message)
	{
		super(message);
	}

	public GlideinException(Throwable throwable)
	{
		super(throwable);
	}
}

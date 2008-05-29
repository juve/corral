package edu.usc.glidein.client.cli;

public class CommandException extends Exception
{
	private static final long serialVersionUID = 2658388857059468253L;

	public CommandException()
	{
		super();
	}
	
	public CommandException(String message)
	{
		super(message);
	}

	public CommandException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
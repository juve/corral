package edu.usc.glidein.service.exec;

public class CondorException extends Exception
{
	private static final long serialVersionUID = 634176449871496327L;

	public CondorException()
	{
		super();
	}

	public CondorException(String message, Throwable throwable)
	{
		super(message, throwable);
	}

	public CondorException(String message)
	{
		super(message);
	}

	public CondorException(Throwable throwable)
	{
		super(throwable);
	}	
}

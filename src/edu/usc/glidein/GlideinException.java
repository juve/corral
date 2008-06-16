package edu.usc.glidein;

/* Project todo tasks */
// TODO: Validate certificate lifetime
// TODO: Implement file persistence
// TODO: Add resource properties?
// TODO: Add history tracking
// TODO: Test custom glidein_condor_config
// TODO: Improve dir hierarchy on remote 
// TODO: Retest GT4 @ SDSC

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

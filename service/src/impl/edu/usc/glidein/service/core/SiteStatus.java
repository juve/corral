package edu.usc.glidein.service.core;

public class SiteStatus 
{
	private SiteStatusCode code;
	private String message;
	private Exception exception;
	
	public SiteStatus(SiteStatusCode code, String message, Exception exception)
	{
		this.code = code;
		this.message = message;
		this.exception = exception;
	}
	
	public SiteStatus(SiteStatusCode code, String message)
	{
		this(code,message,null);
	}
	
	public SiteStatus(SiteStatusCode code)
	{
		this(code,null,null);
	}

	public SiteStatusCode getCode()
	{
		return code;
	}

	public void setCode(SiteStatusCode code)
	{
		this.code = code;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Exception getException()
	{
		return exception;
	}

	public void setException(Exception exception)
	{
		this.exception = exception;
	}
	
	public String toString()
	{
		return (message==null?"":message)+
			(exception==null?"":" "+exception.getMessage());
	}
}

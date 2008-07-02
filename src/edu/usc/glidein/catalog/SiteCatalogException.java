package edu.usc.glidein.catalog;

public class SiteCatalogException extends Exception
{
	private static final long serialVersionUID = 4959389434941630448L;

	public SiteCatalogException()
	{
		super();
	}

	public SiteCatalogException(String s, Throwable throwable)
	{
		super(s, throwable);
	}

	public SiteCatalogException(String s)
	{
		super(s);
	}

	public SiteCatalogException(Throwable throwable)
	{
		super(throwable);
	}
}

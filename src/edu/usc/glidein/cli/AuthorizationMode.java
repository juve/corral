package edu.usc.glidein.cli;

public enum AuthorizationMode
{
	HOST,
	SELF,
	NONE,
	DN;
	
	public static AuthorizationMode fromString(String string) throws IllegalArgumentException
	{
		String lower = string.toLowerCase();
		if (lower.matches("^host$")) {
			return HOST;
		} else if (lower.matches("^self$")) {
			return SELF;
		} else if (lower.matches("^none$")) {
			return NONE;
		} else {
			throw new IllegalArgumentException("Invalid authorization mode: "+string);
		}
	}
}

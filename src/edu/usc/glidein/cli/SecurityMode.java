package edu.usc.glidein.cli;

public enum SecurityMode
{
	MESSAGE,
	CONVERSATION,
	TRANSPORT,
	NONE;
	
	public static SecurityMode fromString(String string) throws IllegalArgumentException
	{
		String lower = string.toLowerCase();
		if (lower.matches("^((msg)|(message))$")) {
			return MESSAGE;
		} else if (lower.matches("^conv(ersation)?$")) {
			return CONVERSATION;
		} else if (lower.matches("^trans(port)?$")) {
			return TRANSPORT;
		} else if (lower.matches("^none$")) {
			return NONE;
		} else {
			throw new IllegalArgumentException("Invalid authentication mode: "+string);
		}
	}
}

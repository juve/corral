package edu.usc.glidein.cli;

public enum ProtectionType
{
	SIGNATURE,
	ENCRYPTION;
	
	public static ProtectionType fromString(String string) throws IllegalArgumentException
	{
		if (string.toLowerCase().matches("^sig(nature)?$")) {
			return SIGNATURE;
		} else if (string.toLowerCase().matches("^enc(ryption)?$")) {
			return ENCRYPTION;
		} else {
			throw new IllegalArgumentException("Invalid protection type: "+string);
		}
	}
}

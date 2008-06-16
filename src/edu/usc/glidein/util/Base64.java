package edu.usc.glidein.util;

import java.io.IOException;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;

/**
 * Convert to/from Base64-encoded data
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class Base64
{
	
	/**
	 * Base64-encode a string
	 * @param string The string to encode
	 * @return The base64 encoding of the string
	 */
	public static byte[] toBase64(String string)
	{
		if(string==null) return null;
		byte[] buff = string.getBytes();
		String s = new BASE64Encoder().encode(buff);
		return s.getBytes();
	}
	
	/**
	 * Decode a base64-encoded string
	 * @param base64 Base64-encoded binary
	 * @return The decoded string
	 */
	public static String fromBase64(byte[] base64)
	{
		if(base64==null) return null;
		
		try {
			String s = new String(base64);
			byte[] output = new BASE64Decoder().decodeBuffer(s);
			return new String(output);
		} 
		catch(IOException ioe) {
			return null; /* I don't think this will actually happen */
		}
	}
}

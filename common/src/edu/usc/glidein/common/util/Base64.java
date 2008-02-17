package edu.usc.glidein.common.util;

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
	public static String toBase64(String string)
	{
		byte[] buff = string.getBytes();
		return new BASE64Encoder().encode(buff);
	}
	
	/**
	 * Decode a base64-encoded string
	 * @param base64 Base64-encoded string
	 * @return The decoded string
	 */
	public static String fromBase64(String base64)
	{
		try {
			byte[] output = new BASE64Decoder().decodeBuffer(base64);
			return new String(output);
		} 
		catch(IOException ioe) {
			return null; /* I don't think this will actually happen */
		}
	}

	public static void main(String[] args)
	{
		System.out.println(new String(Base64.toBase64("Hello, World!")));
		System.out.println(Base64.fromBase64("SGVsbG8sIFdvcmxkIQ=="));
	}
}

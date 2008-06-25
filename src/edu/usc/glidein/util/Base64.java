/*
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
			/* I don't think this will actually happen */
			throw new RuntimeException("Unable to decode: "+ioe.getMessage(),ioe);
		}
	}
}

/*
 *  Copyright 2007-2009 University Of Southern California
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
package edu.usc.corral.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class IOUtil {
	/**
	 * Read the contents of a file
	 * @param f The file to read
	 * @return The entire contents of the file
	 */
	public static String read(File file) throws IOException {
		StringBuffer buff = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line = null;
			int i = 0;
			while((line = reader.readLine()) != null) {
				if(i>0) buff.append("\n");
				buff.append(line);
				i++;
			}
		} finally {
			reader.close();
		}
		return buff.toString();
	}
	
	/**
	 * Write a string to a file
	 * @param output The string to write
	 * @param file The file to write to
	 * @throws IOException If there is a problem writing the file
	 */
	public static void write(String output, File file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		try {
			writer.write(output);
		} finally {
			writer.close();
		}
	}
}

/*
 *  Copyright 2009 University Of Southern California
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

import java.io.File;
import java.io.IOException;

public class FilesystemUtil {
	
	public static boolean chmod(File file, int mode) throws IOException {
		try {
			CommandLine chmod = new CommandLine();
			chmod.setCommand("chmod");
			chmod.addArgument(String.valueOf(mode));
			chmod.addArgument(file.getAbsolutePath());
			chmod.execute();
			int exitCode = chmod.getExitCode();
			if(exitCode != 0){
				return false;
			} else {
				return true;
			}
		} catch (IOException ioe) {
			IOException e = new IOException(
				"Unable to chmod file/directory: "+file);
			e.initCause(ioe);
			throw e;
		}
	}
	
	public static boolean chown(File file, String user) throws IOException {
		try {
			CommandLine chown = new CommandLine();
			chown.setCommand("chown");
			if (file.isDirectory()) {
				chown.addArgument("-R");
			}
			chown.addArgument(user);
			chown.addArgument(file.getAbsolutePath());
			chown.execute();
			int exitCode = chown.getExitCode();
			if(exitCode != 0){
				return false;
			} else {
				return true;
			}
		} catch (IOException ioe) {
			IOException e = new IOException(
				"Unable to chown file/directory: "+file);
			e.initCause(ioe);
			throw e;
		}
	}
	
	public static boolean rm(File file) throws IOException {
		try {
			CommandLine rm = new CommandLine();
			rm.setCommand("rm");
			if (file.isDirectory()) {
				rm.addArgument("-r");
			}
			rm.addArgument("-f");
			rm.addArgument(file.getAbsolutePath());
			rm.execute();
			int exitCode = rm.getExitCode();
			if(exitCode != 0){
				return false;
			} else {
				return true;
			}
		} catch (IOException ioe) {
			IOException e = new IOException(
				"Unable to rm file/directory: "+file);
			e.initCause(ioe);
			throw e;
		}
	}
}

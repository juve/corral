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
package edu.usc.glidein.util;

import java.io.File;
import java.io.IOException;

public class FilesystemUtil {
	
	public static boolean chmod(File file, int mode) {
		try {
			CommandLine chown = new CommandLine();
			chown.setCommand("chmod");
			chown.addArgument(String.valueOf(mode));
			chown.addArgument(file.getAbsolutePath());
			chown.execute();
			int exitCode = chown.getExitCode();
			if(exitCode != 0){
				return false;
			} else {
				return true;
			}
		} catch (IOException ioe) {
			return false;
		}
	}
	
	public static boolean chown(File file, String user) {
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
			return false;
		}
	}
	
	public static boolean rm(File file) {
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
			return false;
		}
	}
}

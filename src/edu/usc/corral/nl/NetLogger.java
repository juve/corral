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
package edu.usc.corral.nl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import edu.usc.corral.config.Initializable;
import edu.usc.corral.config.Registry;

/**
 * Simple class to log messages in NetLogger format.
 */
public class NetLogger implements Initializable {
	
	private boolean initialized = false;
	private File logFile;
	private SimpleDateFormat tsFormat;
	private Pattern space;
	
	public NetLogger() { }
	
	public void initialize() throws Exception {
		synchronized (this) {
			if (initialized)
				return;
			
			createLogFile();
			
			tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			tsFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			
			space = Pattern.compile("\\s");
			
			initialized = true;
		}
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	private void createLogFile() throws NetLoggerException {
		if (!logFile.exists()) {
			File dir = logFile.getParentFile();
			try {
				if (!dir.exists()) {
					if(!dir.mkdirs()) {
						throw new NetLoggerException(
								"Unable to create netlogger directory: "+
								dir.getAbsolutePath());
					}
				}
				if (!logFile.createNewFile()) {
					throw new NetLoggerException(
							"Unable to create netlogger file: "+
							logFile.getAbsolutePath());
				}
			} catch (Exception e) {
				throw new NetLoggerException(
						"Unable to initialize netlogger: "+
						dir.getAbsolutePath(),e);
			}
		}
	}
	
	public void setLogFile(String logFile) {
		this.logFile = new File(logFile);
		if (!this.logFile.isAbsolute()) {
			this.logFile = new File(
					System.getenv("CORRAL_HOME"),logFile);
		}
	}
	
	public synchronized void log(NetLoggerEvent event) {
		try {
			// Open log
			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
			
			// Write formatted message
			writer.write("ts="+tsFormat.format(event.getTimeStamp()));
			writer.write(" event="+event.getEvent());
			for (String key : event.keySet()) {
				Object obj = event.get(key);
				if (obj == null) {
					continue;
				} else if (obj instanceof Calendar) {
					String value = tsFormat.format(((Calendar)obj).getTime());
					writer.write(" "+key+"="+value);
				} else {
					String value = obj.toString();
					if (space.matcher(value).find()) {
						writer.write(" "+key+"=\""+value+"\"");
					} else {
						writer.write(" "+key+"="+value);
					}
				}
			}
			writer.write("\n");
			
			// Close log
			writer.close();
		} catch (IOException ioe) {
			throw new RuntimeException(
					"Unable to log event: "+event.getEvent(),ioe);
		}
	}
	
	public static NetLogger getLog() throws NetLoggerException {
		try {
	    	return (NetLogger)new Registry().lookup("corral/NetLogger");
		} catch (Exception e) {
			throw new NetLoggerException("Unable to load netlogger configuration",e);
		}
	}
}

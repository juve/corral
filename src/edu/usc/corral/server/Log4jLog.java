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
package edu.usc.corral.server;

import org.apache.log4j.Logger;

public class Log4jLog implements org.mortbay.log.Logger {
	
	private Logger logger;
	
	public Log4jLog() {
		logger = Logger.getLogger("org.mortbay.jetty");
	}
	
	public Log4jLog(Logger l) {
		this.logger = l;
	}
	
	public org.mortbay.log.Logger getLogger(String name) {
		return new Log4jLog(Logger.getLogger(name));
	}

	public void info(String message, Object arg1, Object arg2) {
		logger.info(format(message,arg1,arg2));
	}

	public void warn(String message, Throwable throwable) {
		logger.warn(message, throwable);
	}

	public void warn(String message, Object arg1, Object arg2) {
		logger.warn(format(message,arg1,arg2));
	}
	
	public void debug(String message, Throwable throwable) {
		logger.debug(message, throwable);
	}

	public void debug(String message, Object arg1, Object arg2) {
		logger.debug(format(message,arg1,arg2));
	}
	
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public void setDebugEnabled(boolean debug) {
		/* Do nothing */
	}
	
	private String format(String message, Object arg1, Object arg2) {
		int idx1 = message.indexOf("{}");
		int idx2 = message.lastIndexOf("{}");
		if (idx1<0) {
			return message;
		} else if (idx1 == idx2) {
			StringBuffer buf = new StringBuffer();
			buf.append(message.substring(0,idx1));
			buf.append(arg1);
			buf.append(message.substring(idx1+2));
			return buf.toString();
		} else {
			StringBuffer buf = new StringBuffer();
			buf.append(message.substring(0,idx1));
			buf.append(arg1);
			buf.append(message.substring(idx1+2,idx2));
			buf.append(arg2);
			buf.append(message.substring(idx2+2));
			return buf.toString();
		}
	}
	
	public String toString() {
		return "log4j";
	}
}

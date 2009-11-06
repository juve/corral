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
package edu.usc.glidein.service.state;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.config.Initializable;
import edu.usc.corral.config.Registry;

public class EventQueue implements Initializable {
	private int numThreads = 1;
	private ExecutorService pool;

	public EventQueue() { }
	
	public void initialize() throws Exception {
		pool = new ThreadPoolExecutor(
				numThreads, numThreads, 0L, 
				TimeUnit.MILLISECONDS, 
				new LinkedBlockingQueue<Runnable>(),
				new EventQueueThreadFactory());
	}
	
	public int getNumThreads() {
		return numThreads;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}
	
	public void add(Event event) {
		pool.execute(event);
	}
	
	public static class EventQueueThreadFactory implements ThreadFactory  {
		private String namePrefix = "EventQueueThread-";
		private ThreadGroup group;
		private AtomicInteger threadNumber = new AtomicInteger(1);
		
		public EventQueueThreadFactory() {
	        SecurityManager securitymanager = System.getSecurityManager();
	        if (securitymanager == null) {
	        	group = Thread.currentThread().getThreadGroup();
	        } else {
	        	group = securitymanager.getThreadGroup();
	        }
	    }

		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(group, runnable, 
					namePrefix+threadNumber.getAndIncrement(), 0L);
			if(thread.isDaemon())
				thread.setDaemon(false);
			if(thread.getPriority() != Thread.NORM_PRIORITY)
				thread.setPriority(Thread.NORM_PRIORITY);
			return thread;
		}
	}

	public static EventQueue getInstance() throws ConfigurationException {
	    return (EventQueue)new Registry().lookup("corral/EventQueue");
	}
}

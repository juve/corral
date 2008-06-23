package edu.usc.glidein.service.state;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.globus.wsrf.jndi.Initializable;

public class EventQueue implements Initializable
{
	private int numThreads = 1;
	private ExecutorService pool;

	public EventQueue() { }
	
	public void initialize() throws Exception
	{
		pool = new ThreadPoolExecutor(
				numThreads, numThreads, 0L, 
				TimeUnit.MILLISECONDS, 
				new LinkedBlockingQueue<Runnable>(),
				new EventQueueThreadFactory());
	}
	
	public int getNumThreads()
	{
		return numThreads;
	}

	public void setNumThreads(int numThreads)
	{
		this.numThreads = numThreads;
	}
	
	public void add(Event event)
	{
		pool.execute(event);
	}
	
	public static class EventQueueThreadFactory implements ThreadFactory 
	{
		private String namePrefix = "EventQueue-";
		private ThreadGroup group;
		private AtomicInteger threadNumber = new AtomicInteger(1);
		
		public EventQueueThreadFactory()
	    {
	        SecurityManager securitymanager = System.getSecurityManager();
	        if (securitymanager == null) {
	        	group = Thread.currentThread().getThreadGroup();
	        } else {
	        	group = securitymanager.getThreadGroup();
	        }
	    }

		public Thread newThread(Runnable runnable)
		{
			Thread thread = new Thread(group, runnable, 
					namePrefix+threadNumber.getAndIncrement(), 0L);
			if(thread.isDaemon())
				thread.setDaemon(false);
			if(thread.getPriority() != Thread.NORM_PRIORITY)
				thread.setPriority(Thread.NORM_PRIORITY);
			return thread;
		}
	}

	public static EventQueue getInstance() throws NamingException
	{
		String location = "java:comp/env/glidein/EventQueue";
		Context initialContext = new InitialContext();
	    return (EventQueue)initialContext.lookup(location);
	}
}

package edu.usc.glidein.service.state;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		pool = Executors.newFixedThreadPool(numThreads);
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

	public static EventQueue getInstance() throws NamingException
	{
		String location = "java:comp/env/glidein/EventQueue";
		Context initialContext = new InitialContext();
	    return (EventQueue)initialContext.lookup(location);
	}
}

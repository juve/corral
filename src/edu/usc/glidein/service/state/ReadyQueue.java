package edu.usc.glidein.service.state;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.globus.wsrf.jndi.Initializable;

public class ReadyQueue implements Initializable
{
	private int poolSize = 1;
	private ExecutorService pool;

	public ReadyQueue() { }
	
	public void initialize() throws Exception
	{
		pool = Executors.newFixedThreadPool(poolSize);
	}
	
	public int getPoolSize()
	{
		return poolSize;
	}

	public void setPoolSize(int poolSize)
	{
		this.poolSize = poolSize;
	}
	
	public void add(StateChange change)
	{
		pool.execute(change);
	}

	public static ReadyQueue getInstance() throws NamingException
	{
		String location = "java:comp/env/glidein/ReadyQueue";
		Context initialContext = new InitialContext();
	    return (ReadyQueue)initialContext.lookup(location);
	}
}

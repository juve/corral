package edu.usc.glidein.service.exec;

/**
 * An interface for classes that recieve condor job events.
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public interface CondorEventListener
{
	public void handleEvent(CondorEvent event);	
}

package edu.usc.glidein.service.exec;

import java.util.HashSet;

public class CondorJob
{
	/** 
	 * Cluster ID assigned by condor schedd
	 */
	private int clusterId;
	
	/** 
	 * Job ID assigned by condor schedd
	 */
	private int jobId;
	
	/** 
	 * The description for this job
	 */
	private CondorJobDescription description;
	
	/**
	 * The event listeners for this job
	 */
	private HashSet<CondorEventListener> listeners;
	
	public CondorJob()
	{
		listeners = new HashSet<CondorEventListener>();
	}
	
	public CondorJob(CondorJobDescription description)
	{
		this();
		setDescription(description);
	}
	
	public void addListener(CondorEventListener listener){
		listeners.add(listener);
	}
	
	public void removeListener(CondorEventListener listener){
		listeners.remove(listener);
	}
	
	public CondorEventListener[] getListeners(){
		return (CondorEventListener[])
			listeners.toArray(new CondorEventListener[0]);
	}
	
	public void setDescription(CondorJobDescription description) {
		this.description = description;
	}
	
	public CondorJobDescription getDescription() {
		return description;
	}

	public int getClusterId() {
		return clusterId;
	}

	public void setClusterId(int clusterId) {
		this.clusterId = clusterId;
	}

	public int getJobId() {
		return jobId;
	}

	public void setJobId(int jobId) {
		this.jobId = jobId;
	}
}

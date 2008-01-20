package edu.usc.glidein.service.core;

public enum GlideinStatus implements Status 
{
	PENDING,
	SUBMITTED,
	RUNNING,
	TERMINATED,
	CANCELLED,
	FAILED;
	
	public String getStatusString(){
		return this.name();
	}
}

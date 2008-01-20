package edu.usc.glidein.service.core;

public enum SiteStatus implements Status 
{
	NEW,
	STAGING,
	READY,
	STOPPING,
	CLEANING,
	CLEANED,
	FAILED;
	
	public String getStatusString(){
		return this.name();
	}
}

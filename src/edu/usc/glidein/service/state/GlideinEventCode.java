package edu.usc.glidein.service.state;

public enum GlideinEventCode implements EventCode
{
	SUBMIT,			/* Submit requested */
	QUEUED,			/* Glidein queued remotely */
	SITE_READY,		/* Site entered ready state */
	JOB_SUCCESS,	/* Glidein job exited successfully */
	JOB_FAILURE,	/* Glidein job failed */
	JOB_ABORTED,	/* Glidein job was aborted */
	RUNNING,		/* Running remotely */
	REMOVE,			/* Remove requested */
	DELETE,			/* Delete requested */ 
	SITE_FAILED		/* Site entered failed state */
}

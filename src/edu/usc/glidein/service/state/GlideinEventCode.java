package edu.usc.glidein.service.state;

public enum GlideinEventCode implements EventCode
{
	SUBMIT,			/* Submit requested */
	QUEUED,			/* Glidein queued remotely */
	SITE_READY,		/* Site is ready for submission */
	JOB_SUCCESS,	/* Glidein job exited successfully */
	JOB_FAILURE,	/* Glidein job failed */
	RUNNING,		/* Running remotely */
	REMOVE,			/* Remove requested */
	DELETE			/* Delete requested */
}

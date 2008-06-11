package edu.usc.glidein.service.state;

public enum GlideinEventCode implements EventCode
{
	SUBMIT,		/* Submit requested */
	SITE_READY,	/* Site is ready for submission */
	QUEUED,		/* Queued remotely */
	RUNNING,	/* Running remotely */
	REMOVE,		/* Remove requested */
	EXITED,		/* Exited remotely */
	DELETE,		/* Delete requested */
	FAILED		/* Job failed */
}

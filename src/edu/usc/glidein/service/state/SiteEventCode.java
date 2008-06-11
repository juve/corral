package edu.usc.glidein.service.state;

public enum SiteEventCode implements EventCode
{
	SUBMIT,				/* User requested submit */
	INSTALLED,			/* Condor installed */
	REMOVE,				/* User requested remove */
	UNINSTALLED,		/* Condor uninstalled */
	GLIDEIN_FINISHED,	/* Glidein finished */
	DELETE,				/* User requested delete */
	FAILED				/* Site failed */
}

package edu.usc.glidein.service.state;

public enum SiteEventCode implements EventCode
{
	SUBMIT,				/* User requested submit */
	INSTALL_SUCCESS,	/* Condor installed successfully */
	INSTALL_FAILED,		/* Condor installation failed */
	REMOVE,				/* User requested remove */
	UNINSTALL_SUCCESS,	/* Condor uninstalled successfully */
	UNINSTALL_FAILED,	/* Condor uninstall failed */
	GLIDEIN_DELETED,	/* Glidein deleted */
	DELETE				/* User requested delete */
}

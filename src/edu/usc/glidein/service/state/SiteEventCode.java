/*
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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

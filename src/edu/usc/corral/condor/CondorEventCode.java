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
package edu.usc.corral.condor;

public enum CondorEventCode
{
	SUBMIT 					(0, "Job submitted"),
    EXECUTE					(1, "Job now running"),
    EXECUTABLE_ERROR		(2, "Error in executable"),
    CHECKPOINTED			(3, "Job was checkpointed"),
    JOB_EVICTED				(4, "Job evicted from machine"),
    JOB_TERMINATED			(5, "Job terminated"),
    IMAGE_SIZE				(6, "Image size of job updated"),
    SHADOW_EXCEPTION		(7, "Shadow threw an exception"),
    GENERIC					(8, "Generic Log Event"),
    JOB_ABORTED				(9, "Job Aborted"),
	JOB_SUSPENDED			(10, "Job was suspended"),
	JOB_UNSUSPENDED			(11, "Job was unsuspended"),
	JOB_HELD				(12, "Job was held"),
	JOB_RELEASED			(13, "Job was released"),
    NODE_EXECUTE			(14, "Parallel Node executed"),
    NODE_TERMINATED			(15, "Parallel Node terminated"),
    POST_SCRIPT_TERMINATED	(16, "POST script terminated"),
	GLOBUS_SUBMIT			(17, "Job Submitted to Globus"),
	GLOBUS_SUBMIT_FAILED	(18, "Globus Submit failed"),
	GLOBUS_RESOURCE_UP		(19, "Globus Resource Up"),
	GLOBUS_RESOURCE_DOWN	(20, "Globus Resource Down"),
	REMOTE_ERROR			(21, "Remote Error"),
	JOB_DISCONNECTED		(22, "RSC socket lost"),
	JOB_RECONNECTED			(23, "RSC socket re-established"),
	JOB_RECONNECT_FAILED	(24, "RSC reconnect failure"),
	GRID_RESOURCE_UP		(25, "Grid Resource Up"),
	GRID_RESOURCE_DOWN		(26, "Grid Resource Down"),
	GRID_SUBMIT				(27, "Job Submitted remotely"),
	JOB_AD_INFORMATION		(28, "Report job ad information"),
	EXCEPTION				(999, "Log parser threw an exception");

	/**
	 * The integer id for this event code
	 */
	private int eventCode;
	
	/**
	 * A short description of the event code
	 */
	private String description;
	
	private CondorEventCode(int eventCode, String description)
	{
		this.eventCode = eventCode;
		this.description = description;
	}
	
	public int getEventCode()
	{
		return eventCode;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public static CondorEventCode fromEventCode(int eventCode)
	throws CondorException
	{
		switch(eventCode)
		{
			case 0: return CondorEventCode.SUBMIT;
			case 1: return CondorEventCode.EXECUTE;
			case 2: return CondorEventCode.EXECUTABLE_ERROR;
			case 3: return CondorEventCode.CHECKPOINTED;
			case 4: return CondorEventCode.JOB_EVICTED;
			case 5: return CondorEventCode.JOB_TERMINATED;
			case 6: return CondorEventCode.IMAGE_SIZE;
			case 7: return CondorEventCode.SHADOW_EXCEPTION;
			case 8: return CondorEventCode.GENERIC;
			case 9: return CondorEventCode.JOB_ABORTED;
			case 10: return CondorEventCode.JOB_SUSPENDED;
			case 11: return CondorEventCode.JOB_UNSUSPENDED;
			case 12: return CondorEventCode.JOB_HELD;
			case 13: return CondorEventCode.JOB_RELEASED;
			case 14: return CondorEventCode.NODE_EXECUTE;
			case 15: return CondorEventCode.NODE_TERMINATED;
			case 16: return CondorEventCode.POST_SCRIPT_TERMINATED;
			case 17: return CondorEventCode.GLOBUS_SUBMIT;
			case 18: return CondorEventCode.GLOBUS_SUBMIT_FAILED;
			case 19: return CondorEventCode.GLOBUS_RESOURCE_UP;
			case 20: return CondorEventCode.GLOBUS_RESOURCE_DOWN;
			case 21: return CondorEventCode.REMOTE_ERROR;
			case 22: return CondorEventCode.JOB_DISCONNECTED;
			case 23: return CondorEventCode.JOB_RECONNECTED;
			case 24: return CondorEventCode.JOB_RECONNECT_FAILED;
			case 25: return CondorEventCode.GRID_RESOURCE_UP;
			case 26: return CondorEventCode.GRID_RESOURCE_DOWN;
			case 27: return CondorEventCode.GRID_SUBMIT;
			case 28: return CondorEventCode.JOB_AD_INFORMATION;
			default: throw new CondorException(
					"Unrecognized event code: "+eventCode);
		}
	}
}
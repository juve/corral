/*
 *  Copyright 2009 University Of Southern California
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
package edu.usc.corral.types;

public enum GlideinState {
	NEW,		// Created
	WAITING,	// Waiting for site to be READY
	SUBMITTED,	// Job submitted to Condor locally
	QUEUED,		// Job queued remotely
	RUNNING,	// Job running remotely
	REMOVING,	// Job is being removed
	FINISHED,	// Job finished
	FAILED,		// Glidein failed
	DELETED		// Glidein has been deleted
}

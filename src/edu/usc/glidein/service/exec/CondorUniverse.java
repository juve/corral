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
package edu.usc.glidein.service.exec;

import java.io.Serializable;

public enum CondorUniverse implements Serializable
{
	GRID ("grid"),
	VANILLA ("vanilla"),
	STANDARD ("standard"),
	PVM ("pvm"),
	SCHEDULER ("scheduler"),
	LOCAL ("local"),
	MPI ("mpi"),
	JAVA ("java"),
	VM ("vm");
	
	/**
	 * Universe type string
	 */
	private String typeString;
	
	private CondorUniverse(String typeString)
	{
		this.typeString = typeString;
	}
	
	/**
	 * The type string for this universe. This is what you would find in
	 * the submit script.
	 * @return The type string for this universe type
	 */
	public String getTypeString()
	{
		return typeString;
	}
	
	/**
	 * Get the universe type with matching type string
	 * @param typeString The type string to match
	 * @return The universe type that matches the type string
	 * @throws CondorException If there is no match
	 */
	public static CondorUniverse fromTypeString(String typeString)
	throws CondorException
	{
		for(CondorUniverse u : CondorUniverse.values())
		{
			if(u.typeString.equals(typeString))
				return u;
		}
		
		throw new CondorException(
				"Unrecognized Condor universe type: "+typeString);
	}
}

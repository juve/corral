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

import java.io.Serializable;

/**
 * Enum of the different grid types supported by Condor
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public enum CondorGridType implements Serializable
{
	GT2 ("gt2"),
	GT4 ("gt4"),
	GT5 ("gt5"),
	CONDOR ("condor"),
	NORDUGRID ("nordugrid"),
	UNICORE ("unicore"),
	LSF ("lsf"),
	PBS ("pbs");
	
	/**
	 * The type string as it appears in the submit script
	 */
	private String typeString;
	
	private CondorGridType(String typeString)
	{
		this.typeString = typeString;
	}
	
	/**
	 * Get the type string. This is how the grid type appears in the submit
	 * script.
	 * @return The type string
	 */
	public String getTypeString()
	{
		return this.typeString;
	}
	
	/**
	 * Get the enum instance mapped to a given type string
	 * @param typeString The type string to map
	 * @return The enum instance for the associated type string
	 * @throws CondorException if there is no enum instance matching the given
	 * type string
	 */
	public static CondorGridType fromTypeString(String typeString)
	throws CondorException
	{
		for(CondorGridType gt : CondorGridType.values())
		{
			if(gt.typeString.equals(typeString))
				return gt;
		}
		
		throw new CondorException(
				"Unrecognized grid type: "+typeString);
	}
}

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

/**
 * An exception for condor errors
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class CondorException extends Exception
{
	private static final long serialVersionUID = 634176449871496327L;

	public CondorException()
	{
		super();
	}

	public CondorException(String message, Throwable throwable)
	{
		super(message, throwable);
	}

	public CondorException(String message)
	{
		super(message);
	}

	public CondorException(Throwable throwable)
	{
		super(throwable);
	}	
}

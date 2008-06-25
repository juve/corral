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
package edu.usc.glidein.cli;

public class Option
{
	public static Option create() 
	{
		Option option = new Option();
		return option;
	}
	
	private String usage = null;
	private String description = null;
	private String option = null;
	private String longOption = null;
	private boolean argument = false;
	
	public Option() { }

	public Option setUsage(String usage)
	{
		this.usage = usage;
		return this;
	}
	
	public String getUsage()
	{
		return usage;
	}
	
	public Option setDescription(String description)
	{
		this.description = description;
		return this;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public Option setOption(String option)
	{
		this.option = option;
		return this;
	}
	
	public String getOption()
	{
		return option;
	}
	
	public Option setLongOption(String longOption)
	{
		this.longOption = longOption;
		return this;
	}
	
	public String getLongOption()
	{
		return longOption;
	}
	
	public Option hasArgument() {
		this.argument = true;
		return this;
	}
	
	public Option hasArgument(boolean argument) {
		this.argument = argument;
		return this;
	}
	
	public boolean getArgument() {
		return argument;
	}
	
	public org.apache.commons.cli.Option buildOption()
	{
		org.apache.commons.cli.Option opt = 
			new org.apache.commons.cli.Option(
					option,longOption,argument,description);
		return opt;
		
	}
}

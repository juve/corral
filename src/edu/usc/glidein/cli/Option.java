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

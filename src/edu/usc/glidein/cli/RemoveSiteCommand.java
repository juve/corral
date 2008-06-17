package edu.usc.glidein.cli;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import edu.usc.glidein.api.SiteService;
import edu.usc.glidein.service.impl.SiteNames;

public class RemoveSiteCommand extends Command
{	
	private boolean force;
	private List<Integer> ids;
	
	public RemoveSiteCommand()
	{
		ids = new LinkedList<Integer>();
	}
	
	
	public void addOptions(List<Option> options)
	{
		options.add(
			Option.create()
				  .setOption("f")
				  .setLongOption("force")
				  .setUsage("-f [--force]")
				  .setDescription("Force the site to be deleted regardless of state")
		);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException
	{
		// Force
		if (cmdln.hasOption("f")) {
			force = true;
		} else {
			force = false;
		}
		
		// Get IDs
		String[] args = cmdln.getArgs();
		if (args.length == 0) {
			throw new CommandException(getUsage());
		}
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				int id = Integer.parseInt(arg);
				ids.add(id);
			} else {
				System.out.println("Invalid site id: "+arg);
			}
		}
	}
	
	public void execute() throws CommandException
	{
		/* Delete all the sites */
		for (int id : ids) {
			try {
				if (isDebug()) System.out.print("Removing site "+id+"... ");
				SiteService instance = new SiteService(
						getServiceURL(SiteNames.SITE_SERVICE),id);
				instance.setDescriptor(getClientSecurityDescriptor());
				instance.remove(force);
				if (isDebug()) System.out.println("done.");
			} catch (Exception e) {
				System.out.println("Unable to remove site '"+id+"': "+e.getMessage());
				if (isDebug()) e.printStackTrace();
			}
		}
	}
	
	public String getName()
	{
		return "remove-site";
	}
	
	public String[] getAliases()
	{
		return new String[]{"rs"};
	}
	
	public String getDescription()
	{
		return "remove-site (rs): Remove an existing site";
	}
	
	public String getUsage()
	{
		return "Usage: remove-site ID...";
	}
}

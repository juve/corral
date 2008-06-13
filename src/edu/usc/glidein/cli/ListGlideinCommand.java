package edu.usc.glidein.cli;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;

import edu.usc.glidein.stubs.GlideinFactoryPortType;
import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;

public class ListGlideinCommand extends Command
{
	private boolean longFormat = false;
	private List<Integer> ids;
	
	public ListGlideinCommand()
	{
		ids = new LinkedList<Integer>();
	}
	
	public void addOptions(List<Option> options)
	{
		options.add(
			Option.create()
				  .setOption("l")
				  .setLongOption("long")
				  .setUsage("-l [--long]")
				  .setDescription("Show detailed information")
		);
	}

	public void setArguments(CommandLine cmdln) throws CommandException
	{		
		/* Long format/short format */
		if (cmdln.hasOption("l")) { 
			longFormat = true;
		}
		
		/* Remaining arguments */
		String[] args = cmdln.getArgs();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				int id = Integer.parseInt(arg);
				ids.add(id);
			} else {
				System.out.println("Invalid glidein id: "+arg);
			}
		}
	}

	public void execute() throws CommandException
	{
		/* Check for specific arguments */
		if (ids.size() > 0) {
			listIndividualGlideins();
		} else {
			listAllGlideins();
		}
	}
	
	public void listAllGlideins() throws CommandException
	{
		if (isDebug()) System.out.printf("Listing glideins\n");
		Glidein[] glideins;
		try {
			// Get the sites
			GlideinFactoryPortType factory = getGlideinFactoryPortType();
			glideins = factory.listGlideins(longFormat).getGlideins();
		} catch (Exception e) {
			throw new CommandException("Unable to list glideins: "+
					"Error communicating with service: "+e.getMessage(), e);
		}
		
		// Print out the site list
		printGlideins(glideins);
		
		if (isDebug()) System.out.printf("Done listing glideins.\n");
	}
	
	public void listIndividualGlideins() throws CommandException
	{
		if (isDebug()) System.out.println("Retrieving glideins");
		LinkedList<Glidein> glideins = new LinkedList<Glidein>();
		for (int id : ids) {
			try {
				GlideinPortType instance = getGlideinPortType(id);
				Glidein glidein = instance.getGlidein(new EmptyObject());
				glideins.add(glidein);
			} catch (RemoteException e) {
				System.out.println("Unable to get glidein '"+id+"': "+e.getMessage());
				if (isDebug()) e.printStackTrace();
			}
		}
		if(isDebug()) System.out.println("Done retrieving glideins");
		printGlideins(glideins.toArray(new Glidein[0]));
	}
	
	public void printGlideins(Glidein[] glideins) throws CommandException
	{
		if (isDebug()) System.out.println("Printing glideins");
		
		if (glideins == null || glideins.length == 0) {
			System.out.println("No glideins");
			return;
		}
		
		if (longFormat) {
			if (isDebug()) System.out.println("Using long format");
			for (Glidein g : glideins) {
				System.out.printf("id = %d\n",g.getId());
				System.out.printf("siteName = %s\n", g.getSiteName());
				System.out.printf("siteId = %d\n", g.getSiteId());
				System.out.printf("condorHost = %s\n",g.getCondorHost());
				System.out.printf("count = %d\n", g.getCount());
				System.out.printf("hostCount = %d\n", g.getHostCount());
				System.out.printf("numCpus = %d\n", g.getNumCpus());
				System.out.printf("wallTime = %d\n", g.getWallTime());
				System.out.printf("idleTime = %d\n", g.getIdleTime());
				
				Calendar submitted = (Calendar)g.getSubmitted().clone();
				submitted.setTimeZone(TimeZone.getDefault());
				System.out.printf("submitted = %tc\n",submitted);
				
				Calendar lastUpdate = (Calendar)g.getLastUpdate().clone();
				lastUpdate.setTimeZone(TimeZone.getDefault());
				System.out.printf("lastUpdate = %tc\n",lastUpdate);
				
				System.out.printf("state = %s\n",g.getState().toString());
				System.out.printf("shortMessage = %s\n",g.getShortMessage());
				System.out.printf("longMessage = %s\n",g.getLongMessage());
				
				System.out.printf("condorDebug = %s\n", g.getCondorDebug());
				System.out.printf("gcbBroker = %s\n", g.getGcbBroker());
				
				System.out.printf("\n");
			}
		} else {
			System.out.printf("%-8s", "ID");
			System.out.printf("%-20s", "SITE");
			System.out.printf("%-20s", "CONDOR HOST");
			System.out.printf("%-8s", "COUNT");
			System.out.printf("%-8s", "HOSTS");
			System.out.printf("%-8s", "TIME");
			System.out.printf("%-15s", "SUBMITTED");
			System.out.printf("%-15s", "LAST UPDATE");
			System.out.printf("%-10s", "STATE");
			System.out.printf("%s", "MESSAGE");
			System.out.printf("\n");
			for (Glidein g : glideins) {
				System.out.printf("%-8d",g.getId());
				System.out.printf("%-20s", ""+g.getSiteName()+" ("+g.getSiteId()+")");
				System.out.printf("%-20s",g.getCondorHost());
				System.out.printf("%-8d", g.getCount());
				System.out.printf("%-8d", g.getHostCount());
				System.out.printf("%-8d", g.getWallTime());
				
				Calendar submitted = (Calendar)g.getSubmitted().clone();
				submitted.setTimeZone(TimeZone.getDefault());
				System.out.printf("%1$tm-%1$td %1$TR    ",submitted);
				
				Calendar lastUpdate = (Calendar)g.getLastUpdate().clone();
				lastUpdate.setTimeZone(TimeZone.getDefault());
				System.out.printf("%1$tm-%1$td %1$TR    ",lastUpdate);
				
				System.out.printf("%-10s",g.getState().toString());
				System.out.printf("%s",g.getShortMessage());
				System.out.printf("\n");
			}
		}
		if (isDebug()) System.out.println("Done printing glideins.");
	}
	
	public String getName()
	{
		return "list-glidein";
	}
	
	public String[] getAliases()
	{
		return new String[]{"lg"};
	}
	
	public String getDescription() 
	{
		return "list-glidein (lg): Display glideins";
	}
	
	public String getUsage()
	{
		return "Usage: list-glidein";
	}
}

package edu.usc.glidein.cli;

public class HelpCommand extends Command
{
	public void invoke(String[] args) throws CommandException
	{
		if (args.length==0) {
			StringBuffer buff = new StringBuffer();
			buff.append("Usage: "+COMMAND_NAME+" <subcommand> [options] [args]\n");
			buff.append("Type '"+COMMAND_NAME+" help <subcommand>' for help on a specific subcommand.\n\n");
			buff.append("Valid options:\n");
			buff.append("   --debug                     : Turn on useful debugging messages\n\n");
			buff.append("Available subcommands:\n");
			for (Class clazz : SUBCOMMANDS) {
				Command subcommand = null;
				try {
					subcommand = (Command)clazz.newInstance();
				} catch(Exception e) {
					throw new CommandException("Unable to create command class");
				}
				buff.append("   ");
				buff.append(subcommand.getName());
				String[] aliases = subcommand.getAliases();
				for (int i=0; i<aliases.length; i++) {
					if (i==0) buff.append(" (");
					buff.append(aliases[i]);
					if (i<aliases.length-1) buff.append(", ");
					if (i==aliases.length-1) buff.append(")");
				}
				buff.append("\n");
			}
			System.out.println(buff.toString());
		} else {
			for (String command : args) {
				Command cmd = getCommand(command);
				if (cmd == null) {
					System.out.println("Unknown command: '"+command+"'");
				} else {
					System.out.println(cmd.getHelp());
				}
			}
		}
	}
	
	public String getName() 
	{
		return "help";
	}
	
	public String[] getAliases()
	{
		return new String[]{"h"};
	}
	
	public String getHelp()
	{
		return 
			"help (h): Describe the usage of this program or its subcommands.\n"+
			"usage: help [SUBCOMMAND...]";
	}
}

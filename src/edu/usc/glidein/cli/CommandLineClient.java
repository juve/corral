package edu.usc.glidein.cli;

import java.util.LinkedList;
import java.util.List;

public class CommandLineClient 
{
	private static void fail()
	{
		System.out.println("Type '"+Command.COMMAND_NAME+" help' for usage");
		System.exit(1);
	}
	
	public static void main(String[] args)
	{
		if (args.length==0) {
			fail();
		} else {
			// Handle arguments
			List<String> arguments = new LinkedList<String>();
			for (String arg : args) arguments.add(arg);
			String name = arguments.remove(0);
			String[] ops = arguments.toArray(new String[0]); 
			
			// Invoke appropriate command
			Command command = Command.getCommand(name);
			if (command == null) {
				System.out.println("Unknown command: '"+name+"'");
				fail();
			} else {
				try {
					command.invoke(ops);
				} catch (CommandException e) {
					if (e.getMessage() != null) {
						System.out.println(e.getMessage());
					}
					if (command.isDebug() && e.getCause() != null) {
						e.getCause().printStackTrace();
					}
					System.exit(1);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}
}

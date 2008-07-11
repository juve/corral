package edu.usc.glidein.cli;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.usc.glidein.util.IOUtil;

public class CommandLine
{
	private org.apache.commons.cli.CommandLine commandLine;
	private org.apache.commons.cli.CommandLine fileArgs;
	
	private CommandLine(org.apache.commons.cli.CommandLine commandLine)
	{
		this.commandLine = commandLine;
	}
	
	public void setFileArgs(org.apache.commons.cli.CommandLine fileArgs)
	{
		this.fileArgs = fileArgs;
	}
	
	public static CommandLine parse(List<Option> options, String[] args) throws CommandException
	{
		CommandLine cmdln = null;
		
		// Parse the command line args
		try {
			Options ops = new Options();
			for (Option option :  options) {
				ops.addOption(option.buildOption());
			}
			CommandLineParser parser = new PosixParser();
			cmdln = new CommandLine(parser.parse(ops, args));
		} catch (ParseException pe) {
			throw new CommandException("Invalid argument: "+pe.getMessage());
		}
		
		// If the user specified an arg file, parse that too
		if (cmdln.hasOption("af")) {
			String argFileName = cmdln.getOptionValue("af");
			String[] fargs = null;
			try {
				List<String> argList = new LinkedList<String>();
				String contents = IOUtil.read(new File(argFileName));
				String[] lines = contents.split("[\r\n]+");
				for (String line : lines) {
					if (!line.startsWith("#")) {
						ArgumentParser p = new ArgumentParser(line);
						for (String next = p.nextArgument(); next != null; next = p.nextArgument()) {
							argList.add(next);
						}
					}
				}
				fargs = argList.toArray(new String[0]);
			} catch (IOException ioe) {
				throw new CommandException(
						"Unable to read argument file: "+ioe.getMessage(),ioe);
			}
			
			try {
				Options ops = new Options();
				for (Option option :  options) {
					ops.addOption(option.buildOption());
				}
				CommandLineParser parser = new PosixParser();
				cmdln.setFileArgs(parser.parse(ops, fargs));
			} catch (ParseException pe) {
				throw new CommandException("Invalid argument: "+pe.getMessage());
			}
		}
		
		return cmdln;
	}
	
	public boolean hasOption(String option)
	{
		if (fileArgs == null) {
			return commandLine.hasOption(option);
		} else {
			return commandLine.hasOption(option) || fileArgs.hasOption(option);
		}
	}
	
	public String getOptionValue(String option)
	{
		if (fileArgs == null) {
			return commandLine.getOptionValue(option);
		} else {
			return commandLine.getOptionValue(option, 
					fileArgs.getOptionValue(option));
		}
	}
	
	public String getOptionValue(String option, String defaultValue) 
	{
		String value = getOptionValue(option);
		if (value == null) return defaultValue;
		return value;
	}
	
	public String[] getArgs()
	{
		if (fileArgs == null) {
			return commandLine.getArgs();
		} else {
			List<String> list = new LinkedList<String>();
			String[] args1 = commandLine.getArgs();
			for (String arg : args1) {
				list.add(arg);
			}
			String[] args2 = fileArgs.getArgs();
			for (String arg : args2) {
				list.add(arg);
			}
			return list.toArray(new String[0]);
		}
	}
	
	public static class ArgumentParser
	{
		private static final int EOL = -1;
		private char[] args;
		private int pos;
		
		public ArgumentParser(String args) {
			this.args = args.toCharArray();
			pos = -1;
		}
		
		public String nextArgument()
		{
			StringBuilder buff = new StringBuilder();
			
			// Eat all the whitespace at the start
			consumeWhitespace();
			
			while (true) {
			
				int curr = LA(1);
				consume();
				switch (curr){
					case '\r':
					case '\n':
					case ' ':
					case '\t':
					case 0x0B:
					case '\f':
						return buff.toString();
					case '"':
					case '\'':
						int next = LA(1);
						while (next != curr) {
							buff.append((char)next);
							consume();
							next = LA(1);
						}
						consume();
						return buff.toString();
					case EOL:
						if (buff.length() == 0)
							return null;
						else 
							return buff.toString();
					default:
						buff.append((char)curr);
						break;
				}
			}
		}
		
		private void consumeWhitespace()
		{
			
			while (true) {
				switch (LA(1)) {
					case '\r':
					case '\n':
					case ' ':
					case '\t':
					case 0x0B:
					case '\f':
						consume();
						break;
					default:
						return;
				}
			}
		}
		
		private void consume()
		{
			if ((pos+1) < args.length)
				pos++;
		}
		
		private int LA(int num)
		{
			if ((pos+num) < args.length) {
				return args[(pos+num)];
			} else {
				return EOL;
			}
		}
	}
	
	public static void main(String[] args) {
		
		ArgumentParser p = new ArgumentParser("     -foo -bar -a al\npha  \r\n   --hello world a b c \"gideon's arg\" 'a' '' 'b c d e'");
		for (String next = p.nextArgument(); next != null; next = p.nextArgument()) {
			System.out.println("Arg: "+next);
		}
		
	}
}

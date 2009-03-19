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
package edu.usc.glidein.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class provides a simple interface to the system command line.
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class CommandLine
{
	/**
	 * The arguments for the command
	 */
	private List<String> arguments;
	
	/**
	 * A map of the environment variables for the command
	 */
	private Map<String,String> environment;
	
	/**
	 * Path to the command's executable
	 */
	private String command;
	
	/**
	 * Working directory where executable should be invoked
	 */
	private File workingDirectory;
	
	/**
	 * The exit code of the command
	 */
	private int exitCode;
	
	/**
	 * The standard output of the command
	 */
	private StringBuffer output;
	
	/**
	 * The standard error of the command
	 */
	private StringBuffer error;
	
	/**
	 * Create a new command line instance and initialize
	 * everything to defaults.
	 */
	public CommandLine()
	{
		arguments = new LinkedList<String>();
		environment = new HashMap<String,String>();
		workingDirectory = new File(".");
		command = null;
		output = new StringBuffer();
		error = new StringBuffer();
	}
	
	/**
	 * Add an environment variable
	 * @param name The name of the variable
	 * @param value The value for the variable
	 */
	public void addEnvironmentVariable(String name, String value)
	throws NullPointerException, IllegalArgumentException
	{
		if(name==null) 
			throw new NullPointerException("null variable");
		if(name.length()==0)
			throw new IllegalArgumentException(
					"Zero-length variable names not allowed");
		if(value==null) 
			throw new NullPointerException("null value");
		this.environment.put(name,value);
	}
	
	/**
	 * Replace the current environment
	 * @param environment A map of environment variable name,value pairs
	 * @throws NullPointerException if the new environment is invalid or one of the variables is invalid
	 * @throws IllegalArgumentException If one of the variable names is invalid
	 */
	public void setEnvironment(Map<String,String> env)
	throws NullPointerException, IllegalArgumentException
	{
		if(env == null)
			throw new NullPointerException("null environment");
		environment = new HashMap<String, String>();
		for (String name : env.keySet()) {
			addEnvironmentVariable(name, env.get(name));
		}
	}
	
	/**
	 * Get the current environment
	 * @return A map of environment variable name,value pairs
	 */
	public Map<String,String> getEnvironment()
	{
		return environment;
	}
	
	/**
	 * Set the command
	 * @param command The command or path to the command
	 */
	public void setCommand(String command)
	throws NullPointerException
	{
		if(command==null) 
			throw new NullPointerException("null command");
		this.command = command;
	}
	
	/**
	 * Get the path to the executable
	 * @return The path
	 */
	public String getCommand()
	{
		return command;
	}
	
	/**
	 * Add an argument onto this command
	 * @param argument The new argument
	 */
	public void addArgument(String argument)
	throws NullPointerException
	{
		if(argument == null) 
			throw new NullPointerException("null argument");
		this.arguments.add(argument);
	}
	
	/**
	 * Replace the current list of arguments
	 * @param arguments A new list of arguments
	 */
	public void setArguments(List<String> arguments)
	throws NullPointerException
	{
		if(arguments == null) 
			throw new NullPointerException("null arguments");
		this.arguments = new LinkedList<String>();
		for (String argument : arguments) {
			addArgument(argument);
		}
	}
	
	/**
	 * Get the current argument list
	 * @return The list of arguments
	 */
	public List<String> getArguments()
	{
		return arguments;
	}
	
	/**
	 * Change the working directory for this command
	 * @param workingDirectory The new working directory
	 */
	public void setWorkingDirectory(File workingDirectory)
	throws NullPointerException, IllegalArgumentException
	{
		if(workingDirectory==null) 
			throw new NullPointerException("null working directory");
		if(!workingDirectory.exists())
			throw new IllegalArgumentException(
					"Working directory "+
					workingDirectory.getAbsolutePath()+
					" does not exist");
		if(!workingDirectory.isDirectory())
			throw new IllegalArgumentException(
					"Working directory "+
					workingDirectory.getAbsolutePath()+
					" is not actually a directory");
		this.workingDirectory = workingDirectory;
	}
	
	/**
	 * Get the current working directory
	 * @return The current working directory
	 */
	public File getWorkingDirectory()
	{
		return workingDirectory.getAbsoluteFile();
	}
	
	/**
	 * Get the standard output of this command. This will be an empty string
	 * until the command is executed.
	 * @return The standard output as a string.
	 */
	public String getOutput()
	{
		return output.toString(); 
	}
	
	/**
	 * Get the standard error of this command. This will be an empty string
	 * until the command is executed.
	 * @return The standard error as a string.
	 */
	public String getError()
	{
		return error.toString();
	}
	
	/**
	 * The exit code for this command. This will be 0 until the command is
	 * executed.
	 * @return The exit code
	 */
	public int getExitCode()
	{ 
		return exitCode;
	}
	
	/**
	 * Execute this command. You can execute it multiple times if you like.
	 */
	public void execute() throws IOException
	{
		LinkedList<String> cmdList = new LinkedList<String>();
		
		// Add executable
		cmdList.add(command);
		
		// Add arguments
		for(String argument : arguments) {
			cmdList.add(argument);
		}
		
		String[] cmd = cmdList.toArray(new String[0]);
		
		// Prepare the environment
		String[] env = new String[environment.size()];
		int i = 0;
		for(Object variable : environment.keySet())
		{
			env[i++] = variable+"="+environment.get(variable);
		}
		
		// Create process
		Process p = Runtime.getRuntime().exec(cmd, env, workingDirectory);
			
		// Consume stdout and stderr
		BufferedReader stderr = new BufferedReader(
				new InputStreamReader(p.getErrorStream()));
		BufferedReader stdout = new BufferedReader(
				new InputStreamReader(p.getInputStream()));
		output = new StringBuffer();
		error = new StringBuffer();
		while(true)
		{
			String out, err;
			if((out = stdout.readLine()) != null)
				output.append(out+"\n");
			if((err = stderr.readLine()) != null)
				error.append(err+"\n");
			if(out == null && err == null)
				break;
		}
			
		// Wait for command to finish
		try {
			exitCode = p.waitFor();
		} catch(InterruptedException ie){
			/* Ignore */
		}
	}
}

package edu.usc.glidein.util;

import java.io.File;
import java.io.IOException;

public class FilesystemUtil
{	
	public static boolean chmod(File file, int mode)
	{
		try {
			CommandLine chown = new CommandLine();
			chown.setCommand("chmod");
			chown.addArgument(String.valueOf(mode));
			chown.addArgument(file.getAbsolutePath());
			chown.execute();
			int exitCode = chown.getExitCode();
			if(exitCode != 0){
				return false;
			} else {
				return true;
			}
		} catch (IOException ioe) {
			return false;
		}
	}
	
	public static boolean chown(File file, String user)
	{
		try {
			CommandLine chown = new CommandLine();
			chown.setCommand("chown");
			if (file.isDirectory()) {
				chown.addArgument("-R");
			}
			chown.addArgument(user);
			chown.addArgument(file.getAbsolutePath());
			chown.execute();
			int exitCode = chown.getExitCode();
			if(exitCode != 0){
				return false;
			} else {
				return true;
			}
		} catch (IOException ioe) {
			return false;
		}
	}
	
	public static boolean rm(File file) 
	{
		try {
			CommandLine rm = new CommandLine();
			rm.setCommand("rm");
			if (file.isDirectory()) {
				rm.addArgument("-r");
			}
			rm.addArgument("-f");
			rm.addArgument(file.getAbsolutePath());
			rm.execute();
			int exitCode = rm.getExitCode();
			if(exitCode != 0){
				return false;
			} else {
				return true;
			}
		} catch (IOException ioe) {
			return false;
		}
	}
}

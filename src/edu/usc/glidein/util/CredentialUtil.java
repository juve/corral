package edu.usc.glidein.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

public class CredentialUtil
{
	public static void store(GlobusCredential credential, File file) 
	throws IOException
	{
		// Create the file if it doesn't exist
		file.createNewFile();
		
		// Change file permissions
		CommandLine chmod = new CommandLine();
		chmod.setExecutable(new File("/bin/chmod"));
		chmod.addArgument("600");
		chmod.addArgument(file.getAbsolutePath());
		chmod.execute();
		int exitCode = chmod.getExitCode();
		if(exitCode != 0){
			throw new IOException(
					"Unable to change proxy permissions\n\n"+
					"Stdout:\n"+chmod.getOutput()+
					"Stderr:\n"+chmod.getError());
		}
		
		// Save the credential to the file
		FileOutputStream pstream = 
			new FileOutputStream(file);
		credential.save(pstream);
		pstream.close();
	}
	
	public static GlobusCredential load(File file) 
	throws IOException
	{
		try {
			FileInputStream fis = new FileInputStream(file);
			GlobusCredential cred = new GlobusCredential(fis);
			fis.close();
			return cred;
		} catch (GlobusCredentialException gce) {
			IOException ioe = new IOException("Invalid credential");
			ioe.initCause(gce);
			throw ioe;
		}
	}
}

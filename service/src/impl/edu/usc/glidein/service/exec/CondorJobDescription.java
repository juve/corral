package edu.usc.glidein.service.exec;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to describe all the parameters for a condor job.
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class CondorJobDescription
{
	/**
	 * Path to the executable
	 */
	private File executable;
	
	/**
	 * Is the executable on the submit machine or remote?
	 */
	private boolean localExecutable;
	/**
	 * 
	 */
	private List<String> arguments;
	
	/**
	 * Environment variables for remote executable
	 */
	private Map<String,String> environment;
	
	/**
	 * Job universe
	 */
	private CondorUniverse universe = CondorUniverse.GRID;
	
	/**
	 * Condor requirements for job 
	 */
	private String requirements;
	
	/**
	 * Grid resource for grid universe jobs
	 */
	private String gridResource;
	
	/**
	 * RSL attributes for gt2, gt4 grid universe jobs
	 */
	private Map<String,String> globusAttributes;
	
	/**
	 * Files to transfer from the submit machine to the remote machine
	 */
	private List<File> inputFiles;
	
	/**
	 * Files to transfer from the remote machine back to the submit machine
	 */
	private List<File> outputFiles;
	
	/**
	 * Globus proxy certificate file
	 */
	private File proxyCertificate;
	
	/**
	 * File to store std err of job
	 */
	private File error;
	
	/**
	 * File to store std out of job
	 */
	private File output;
	
	/**
	 * Condor user log
	 */
	private File log;
	
	/**
	 * initialdir
	 */
	private File localDirectory;
	
	/**
	 * remote_initialdir
	 */
	private File remoteDirectory;
	
	public CondorJobDescription()
	{
		arguments = new LinkedList<String>();
		environment = new HashMap<String,String>();
		globusAttributes = new HashMap<String,String>();
		inputFiles = new LinkedList<File>();
		outputFiles = new LinkedList<File>();
		localExecutable = true;
	}

	public File getExecutable() {
		return executable;
	}

	public void setExecutable(File executable) {
		this.executable = executable;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void addArgument(String arg){
		arguments.add(arg);
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public void addEnvironment(String variable, String value){
		environment.put(variable, value);
	}

	public CondorUniverse getUniverse() {
		return universe;
	}

	public void setUniverse(CondorUniverse universe) {
		this.universe = universe;
	}

	public String getRequirements() {
		return requirements;
	}

	public void setRequirements(String requirements) {
		this.requirements = requirements;
	}

	public String getGridResource() {
		return gridResource;
	}

	public void setGridResource(String gridResource) {
		this.gridResource = gridResource;
	}

	public Map<String, String> getGlobusAttributes() {
		return globusAttributes;
	}
	
	public void setGlobusAttribute(String attribute, String value) {
		this.globusAttributes.put(attribute, value);
	}
	
	public List<File> getInputFiles() {
		return inputFiles;
	}
	
	public void addInputFile(File inputFile) {
		inputFiles.add(inputFile);
	}

	public List<File> getOutputFiles() {
		return outputFiles;
	}

	public void addOutputFile(File outputFile) {
		this.outputFiles.add(outputFile);
	}

	public File getProxyCertificate() {
		return proxyCertificate;
	}

	public void setProxyCertificate(File proxyCertificate) {
		this.proxyCertificate = proxyCertificate;
	}

	public File getError() {
		return error;
	}

	public void setError(File error) {
		this.error = error;
	}

	public File getOutput() {
		return output;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public File getLog() {
		return log;
	}

	public void setLog(File log) {
		this.log = log;
	}

	public File getLocalDirectory() {
		return localDirectory;
	}

	public void setLocalDirectory(File localDirectory) {
		this.localDirectory = localDirectory;
	}

	public File getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteDirectory(File remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}
	
	public boolean isLocalExecutable() {
		return localExecutable;
	}

	public void setLocalExecutable(boolean localExecutable) {
		this.localExecutable = localExecutable;
	}
	
	public String generateSubmitScript() {
		// TODO generate submit script for job
		return null;
	}
}
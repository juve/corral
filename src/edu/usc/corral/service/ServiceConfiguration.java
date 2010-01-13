/*
 *  Copyright 2007-2009 University Of Southern California
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
package edu.usc.corral.service;

import java.io.File;

import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.config.Registry;

public class ServiceConfiguration {
	private File install;
	private File uninstall;
	private File run;
	private File start;
	private String rls;
	private File glideinCondorConfig;
	private File workingDirectory;
	private String mapper;
	
	public ServiceConfiguration() { }
	
	public static ServiceConfiguration getInstance() throws ConfigurationException {
	    return (ServiceConfiguration)new Registry().lookup("corral/ServiceConfiguration");
	}

	public String getInstall()
	{
		return install.getAbsolutePath();
	}

	public void setInstall(String install)
	{
		this.install = new File(install);
		if (!this.install.isAbsolute()) {
			this.install = new File(
					System.getProperty("CORRAL_HOME"),install);
		}
	}

	public String getUninstall()
	{
		return uninstall.getAbsolutePath();
	}

	public void setUninstall(String uninstall)
	{
		this.uninstall = new File(uninstall);
		if (!this.uninstall.isAbsolute()) {
			this.uninstall = new File(
					System.getProperty("CORRAL_HOME"),uninstall);
		}
	}

	public String getRun()
	{
		return run.getAbsolutePath();
	}

	public void setRun(String run)
	{
		this.run = new File(run);
		if (!this.run.isAbsolute()) {
			this.run = new File(
					System.getProperty("CORRAL_HOME"),run);
		}
	}

	public String getStart()
	{
		return start.getAbsolutePath();
	}

	public void setStart(String start)
	{
		this.start = new File(start);
		if (!this.start.isAbsolute()) {
			this.start = new File(
					System.getProperty("CORRAL_HOME"),start);
		}
	}
	
	public String getRls()
	{
		return rls;
	}
	
	public void setRls(String rls)
	{
		this.rls = rls;
	}
	
	public String getMapper()
	{
		return mapper;
	}
	
	public void setMapper(String mapper)
	{
		this.mapper = mapper;
	}

	public String getGlideinCondorConfig()
	{
		return glideinCondorConfig.getAbsolutePath();
	}

	public void setGlideinCondorConfig(String glideinCondorConfig)
	{
		this.glideinCondorConfig = new File(glideinCondorConfig);
		if (!this.glideinCondorConfig.isAbsolute()) {
			this.glideinCondorConfig = new File(
					System.getProperty("CORRAL_HOME"),glideinCondorConfig);
		}
	}

	public String getWorkingDirectory()
	{
		return workingDirectory.getAbsolutePath();
	}

	public void setWorkingDirectory(String workingDirectory)
	{
		this.workingDirectory = new File(workingDirectory);
		if (!this.workingDirectory.isAbsolute()) {
			this.workingDirectory = new File(
					System.getProperty("CORRAL_HOME"),workingDirectory);
		}
	}
}

/*
 *  Copyright 2009 University Of Southern California
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
package edu.usc.corral.types;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root
public class CreateSiteRequest implements Request {
	protected @Element String name;
	protected @Element String installPath;
	protected @Element String localPath;
	protected @Element(required=false) ExecutionService stagingService;
	protected @Element(required=false) ExecutionService glideinService;
	protected @Element(required=false) String condorVersion;
	protected @Element(required=false) String condorPackage;
	protected @ElementList(entry="entry",required=false) List<EnvironmentVariable> environment;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInstallPath() {
		return installPath;
	}
	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}
	public String getLocalPath() {
		return localPath;
	}
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
	public ExecutionService getStagingService() {
		return stagingService;
	}
	public void setStagingService(ExecutionService stagingService) {
		this.stagingService = stagingService;
	}
	public ExecutionService getGlideinService() {
		return glideinService;
	}
	public void setGlideinService(ExecutionService glideinService) {
		this.glideinService = glideinService;
	}
	public String getCondorVersion() {
		return condorVersion;
	}
	public void setCondorVersion(String condorVersion) {
		this.condorVersion = condorVersion;
	}
	public String getCondorPackage() {
		return condorPackage;
	}
	public void setCondorPackage(String condorPackage) {
		this.condorPackage = condorPackage;
	}
	public List<EnvironmentVariable> getEnvironment() {
		return environment;
	}
	public void setEnvironment(List<EnvironmentVariable> environment) {
		this.environment = environment;
	}
}

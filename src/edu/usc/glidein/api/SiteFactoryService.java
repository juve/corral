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
package edu.usc.glidein.api;

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.stubs.SiteFactoryPortType;
import edu.usc.glidein.stubs.service.SiteFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.types.Identifiers;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteHistory;
import edu.usc.glidein.stubs.types.SiteHistoryEntry;
import edu.usc.glidein.util.AddressingUtil;

public class SiteFactoryService extends BaseService
{
	public SiteFactoryService(URL serviceUrl) throws GlideinException
	{
		super(createEPR(serviceUrl));
	}
	
	public static EndpointReferenceType createEPR(URL serviceUrl)
	throws GlideinException
	{
		try {
			return AddressingUtil.getSiteFactoryEPR(serviceUrl);
		} catch (Exception e){
			throw new GlideinException(e.getMessage(),e);
		}
	}
	
	public EndpointReferenceType createSite(Site site) 
	throws GlideinException
	{
		try {
			SiteFactoryPortType factory = getPort();
			EndpointReferenceType epr = factory.createSite(site);
			return epr;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to create site: "+
					re.getMessage(),re);
		}
	}
	
	public Site[] listSites(boolean longFormat) 
	throws GlideinException
	{
		try {
			SiteFactoryPortType factory = getPort();
			Site[] sites = factory.listSites(longFormat).getSites();
			return sites;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to list sites: "+
					re.getMessage(),re);
		}
	}
	
	public SiteHistoryEntry[] getHistory(int[] siteIds) throws GlideinException
	{
		try {
			SiteFactoryPortType instance = getPort();
			SiteHistory hist = instance.getHistory(new Identifiers(siteIds));
			return hist.getEntries();
		} catch (RemoteException re) {
			throw new GlideinException("Unable to get site history: "+
					re.getMessage(),re);
		}
	}
	
	private SiteFactoryPortType getPort() throws GlideinException
	{
		try {
			SiteFactoryServiceAddressingLocator locator = 
				new SiteFactoryServiceAddressingLocator();
			SiteFactoryPortType factory = 
				locator.getSiteFactoryPortTypePort(getEPR());
			if (getDescriptor() != null) {
				((Stub)factory)._setProperty(
						"clientDescriptor", getDescriptor());
			}
			return factory;
		} catch (ServiceException se) {
			throw new GlideinException("Unable to get SiteFactoryPortType: "+
					se.getMessage(),se);
		}
	}
}

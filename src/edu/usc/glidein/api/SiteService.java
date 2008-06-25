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

import edu.usc.glidein.stubs.RemoveRequest;
import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.service.SiteServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.AddressingUtil;

public class SiteService extends BaseService
{	
	public SiteService(EndpointReferenceType epr)
	{
		super(epr);
	}
	
	public SiteService(URL serviceUrl, int id) throws GlideinException
	{
		super(SiteService.createEPR(serviceUrl,id));	
	}
		
	public static EndpointReferenceType createEPR(URL serviceUrl, int id)
	throws GlideinException
	{
		try {
			return AddressingUtil.getSiteEPR(serviceUrl,id);
		} catch (Exception e) {
			throw new GlideinException(e.getMessage(),e);
		}
	}
	
	public Site getSite() throws GlideinException
	{
		try {
			SitePortType instance = getPort();
			Site site = instance.getSite(new EmptyObject());
			return site;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to get site: "+
					re.getMessage(),re);
		}
	}
	
	public void submit(EndpointReferenceType credentialEPR) throws GlideinException
	{
		try {
			SitePortType instance = getPort();
			instance.submit(credentialEPR);
		} catch (RemoteException re) {
			throw new GlideinException("Unable to submit site: "+
					re.getMessage(),re);
		}
	}
	
	public void remove(boolean force, EndpointReferenceType credentialEPR) throws GlideinException
	{
		try {
			SitePortType instance = getPort();
			RemoveRequest request = new RemoveRequest(credentialEPR, force);
			instance.remove(request);
		} catch (RemoteException re) {
			throw new GlideinException("Unable to remove site: "+
					re.getMessage(),re);
		}
	}
	
	private SitePortType getPort() throws GlideinException
	{
		try {
			SiteServiceAddressingLocator locator = 
				new SiteServiceAddressingLocator();
			SitePortType instance = 
				locator.getSitePortTypePort(getEPR());
			if (getDescriptor() != null) {
				((Stub)instance)._setProperty(
						"clientDescriptor", getDescriptor());
			}
			return instance;
		} catch (ServiceException se) {
			throw new GlideinException("Unable to get SitePortType: "+
					se.getMessage(),se);
		}
	}
}

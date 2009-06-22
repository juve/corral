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
package edu.usc.glidein.api;

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.globus.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.stubs.GlideinFactoryPortType;
import edu.usc.glidein.stubs.service.GlideinFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinHistory;
import edu.usc.glidein.stubs.types.GlideinHistoryEntry;
import edu.usc.glidein.stubs.types.Identifiers;
import edu.usc.glidein.stubs.types.ListingRequest;
import edu.usc.glidein.util.AddressingUtil;

public class GlideinFactoryService extends BaseService
{	
	public GlideinFactoryService(URL serviceUrl) throws GlideinException
	{
		super(GlideinFactoryService.createEPR(serviceUrl));
	}
	
	public static EndpointReferenceType createEPR(URL serviceUrl)
	throws GlideinException
	{
		try {
			return AddressingUtil.getGlideinFactoryEPR(serviceUrl);
		} catch (Exception e) {
			throw new GlideinException(e.getMessage(), e);
		}
	}
	
	public EndpointReferenceType create(Glidein glidein) 
	throws GlideinException
	{
		try {
			GlideinFactoryPortType factory = getPort();	
			EndpointReferenceType epr = factory.create(glidein);
			return epr;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to create glidein: "+
					re.getMessage(),re);
		}
	}
	
	public Glidein[] listGlideins(boolean longFormat, String user, boolean allUsers) 
	throws GlideinException
	{
		try {
			GlideinFactoryPortType factory = getPort();
			ListingRequest request = new ListingRequest(longFormat,user,allUsers);
			Glidein[] glideins = factory.listGlideins(request).getGlideins();
			return glideins;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to list glideins: "+
					re.getMessage(),re);
		}
	}
	
	public GlideinHistoryEntry[] getHistory(int[] glideinIds) throws GlideinException
	{
		try {
			GlideinFactoryPortType glidein = getPort();
			GlideinHistory history = glidein.getHistory(new Identifiers(glideinIds));
			return history.getEntries();
		} catch (RemoteException re) {
			throw new GlideinException("Unable to get glidein history: "+
					re.getMessage(),re);
		}
	}
	
	private GlideinFactoryPortType getPort() throws GlideinException
	{
		try {
			GlideinFactoryServiceAddressingLocator locator = 
				new GlideinFactoryServiceAddressingLocator();
			GlideinFactoryPortType factory = 
				locator.getGlideinFactoryPortTypePort(getEPR());
			if (getDescriptor() != null) {
				((Stub)factory)._setProperty(
						"clientDescriptor", getDescriptor());
			}
			return factory;
		} catch (ServiceException se) {
			throw new GlideinException("Unable to get port: "+
					se.getMessage(),se);
		}
	}
}

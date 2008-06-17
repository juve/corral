package edu.usc.glidein.api;

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.stubs.SiteFactoryPortType;
import edu.usc.glidein.stubs.service.SiteFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.types.Site;
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

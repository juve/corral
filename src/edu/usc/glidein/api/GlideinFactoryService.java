package edu.usc.glidein.api;

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.stubs.GlideinFactoryPortType;
import edu.usc.glidein.stubs.service.GlideinFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.types.Glidein;
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
	
	public EndpointReferenceType createGlidein(Glidein glidein) 
	throws GlideinException
	{
		try {
			GlideinFactoryPortType factory = getPort();	
			EndpointReferenceType epr = factory.createGlidein(glidein);
			return epr;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to create glidein: "+
					re.getMessage(),re);
		}
	}
	
	public Glidein[] listGlideins(boolean longFormat) 
	throws GlideinException
	{
		try {
			GlideinFactoryPortType factory = getPort();
			Glidein[] glideins = factory.listGlideins(longFormat).getGlideins();
			return glideins;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to list glideins: "+
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

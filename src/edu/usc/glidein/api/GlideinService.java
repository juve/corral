package edu.usc.glidein.api;

import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.service.GlideinServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.util.AddressingUtil;

public class GlideinService extends BaseService
{	
	public GlideinService(EndpointReferenceType epr)
	{
		super(epr);
	}
	
	public GlideinService(URL serviceUrl, int id) throws GlideinException
	{
		super(GlideinService.createEPR(serviceUrl, id));
	}
	
	public static EndpointReferenceType createEPR(URL serviceUrl, int id)
	throws GlideinException
	{
		try {
			return AddressingUtil.getGlideinEPR(serviceUrl, id);
		} catch (Exception e) {
			throw new GlideinException(e.getMessage(),e);
		}
	}
	
	public Glidein getGlidein() throws GlideinException
	{
		try {
			GlideinPortType instance = getPort();
			Glidein glidein = instance.getGlidein(new EmptyObject());
			return glidein;
		} catch (RemoteException re) {
			throw new GlideinException("Unable to get glidein: "+
					re.getMessage(),re);
		}
	}
	
	public void submit(EndpointReferenceType credentialEPR) throws GlideinException
	{
		try {
			GlideinPortType instance = getPort();
			instance.submit(credentialEPR);
		} catch (RemoteException re) {
			throw new GlideinException("Unable to submit glidein: "+
					re.getMessage(),re);
		}
	}
	
	public void remove(boolean force) throws GlideinException
	{
		try {
			GlideinPortType glidein = getPort();
			glidein.remove(force);
		} catch (RemoteException re) {
			throw new GlideinException("Unable to remove glidein: "+
					re.getMessage(),re);
		}
	}
	
	private GlideinPortType getPort() throws GlideinException
	{
		try {
			GlideinServiceAddressingLocator locator = 
				new GlideinServiceAddressingLocator();
			GlideinPortType instance = 
				locator.getGlideinPortTypePort(getEPR());
			if (getDescriptor() != null) {
				((Stub)instance)._setProperty(
						"clientDescriptor", getDescriptor());
			}
			return instance;
		} catch (ServiceException se) {
			throw new GlideinException("Unable to get port: "+
					se.getMessage(),se);
		}
	}
}

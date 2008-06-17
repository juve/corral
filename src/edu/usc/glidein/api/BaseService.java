package edu.usc.glidein.api;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;

public class BaseService
{
	private EndpointReferenceType epr;
	private ClientSecurityDescriptor descriptor;
	
	public BaseService(EndpointReferenceType epr)
	{
		this.epr = epr;
	}
	
	public ClientSecurityDescriptor getDescriptor()
	{
		return descriptor;
	}
	
	public void setDescriptor(ClientSecurityDescriptor descriptor)
	{
		this.descriptor = descriptor;
	}
	
	public EndpointReferenceType getEPR()
	{
		return epr;
	}
	
	public void setEPR(EndpointReferenceType epr)
	{
		this.epr = epr;
	}
}

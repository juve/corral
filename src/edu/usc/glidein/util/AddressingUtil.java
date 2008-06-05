package edu.usc.glidein.util;

import java.net.URL;

import org.apache.axis.message.MessageElement;
import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.addressing.ReferencePropertiesType;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.SimpleResourceKey;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.impl.GlideinNames;
import edu.usc.glidein.service.impl.SiteNames;

public class AddressingUtil
{
	private static String BASE_URL = "http://localhost:8080/wsrf/services/glidein/";
	public static String SITE_FACTORY_SERVICE_URL    = BASE_URL + "SiteFactoryService";
	public static String SITE_SERVICE_URL            = BASE_URL + "SiteService";
	public static String GLIDEIN_FACTORY_SERVICE_URL = BASE_URL + "GlideinFactoryService";
	public static String GLIDEIN_SERVICE_URL         = BASE_URL + "GlideinService";
	
	public static EndpointReferenceType createEPR(URL address)
	throws Exception
	{ 
		return createEPR(address,null);
	}
	
	public static EndpointReferenceType createEPR(URL address, ResourceKey key)
	throws Exception
	{
	    EndpointReferenceType reference = new EndpointReferenceType();
	    if(key != null)
	    {
	        ReferencePropertiesType referenceProperties = new ReferencePropertiesType();
	        MessageElement elem = (MessageElement)key.toSOAPElement();
	        referenceProperties.set_any(new MessageElement[] {
	                (MessageElement)elem
	        });
	        reference.setProperties(referenceProperties);
	    }
	    reference.setAddress(new Address(address.toString()));
	    return reference;
	}
	
	public static EndpointReferenceType getSiteFactoryEPR(URL factoryURL)
	throws GlideinException
	{
		try {
			return createEPR(factoryURL);
		} catch(Exception e) {
			throw new GlideinException(
					"Unable to create endpoint reference: "+
					e.getMessage(),e);
		}
	}
	
	public static ResourceKey getSiteKey(int id)
	{
		return new SimpleResourceKey(SiteNames.RESOURCE_KEY, new Integer(id));
	}
	
	public static EndpointReferenceType getSiteEPR(URL serviceURL, int id)
	throws GlideinException
	{
		try {
			ResourceKey key = getSiteKey(id);
			return createEPR(serviceURL, key);
		} catch(Exception e) {
			throw new GlideinException(
					"Unable to create endpoint reference: "+
					e.getMessage(),e);
		}
	}
	
	public static EndpointReferenceType getGlideinFactoryEPR(URL factoryURL)
	throws GlideinException
	{
		try {
			return createEPR(factoryURL);
		} catch(Exception e) {
			throw new GlideinException(
					"Unable to create endpoint reference: "+
					e.getMessage(),e);
		}
	}

	public static ResourceKey getGlideinKey(int id)
	{
		return new SimpleResourceKey(GlideinNames.RESOURCE_KEY, new Integer(id));
	}
	
	public static EndpointReferenceType getGlideinEPR(URL serviceURL, int id)
	throws GlideinException
	{
		try {
			ResourceKey key = getGlideinKey(id);
			return createEPR(serviceURL, key);
		} catch(Exception e) {
			throw new GlideinException(
					"Unable to create endpoint reference: "+
					e.getMessage(),e);
		}
	}
}

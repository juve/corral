package edu.usc.glidein.util;

import java.net.MalformedURLException;
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
	
	public static URL getBaseURL(String host) throws MalformedURLException
	{
		return new URL("http://"+host+"/wsrf/services/");
	}
	
	public static EndpointReferenceType getSiteFactoryEPR(String host)
	throws GlideinException
	{
		try {
			URL baseURL = getBaseURL(host);
			URL instanceURL = new URL(baseURL, SiteNames.SITE_FACTORY_SERVICE);
			return createEPR(instanceURL);
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
	
	public static EndpointReferenceType getSiteEPR(String host, int id)
	throws GlideinException
	{
		try {
			ResourceKey key = getSiteKey(id);
			URL baseURL = getBaseURL(host);
			URL serviceURL = new URL(baseURL, SiteNames.SITE_SERVICE);
			return createEPR(serviceURL, key);
		} catch(Exception e) {
			throw new GlideinException(
					"Unable to create endpoint reference: "+
					e.getMessage(),e);
		}
	}
	
	public static EndpointReferenceType getGlideinFactoryEPR(String host)
	throws GlideinException
	{
		try {
			URL baseURL = getBaseURL(host);
			URL serviceURL = new URL(baseURL, GlideinNames.GLIDEIN_FACTORY_SERVICE);
			return createEPR(serviceURL);
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
	
	public static EndpointReferenceType getGlideinEPR(String host, int id)
	throws GlideinException
	{
		try {
			ResourceKey key = getGlideinKey(id);
			URL baseURL = getBaseURL(host);
			URL serviceURL = new URL(baseURL, GlideinNames.GLIDEIN_SERVICE);
			return createEPR(serviceURL, key);
		} catch(Exception e) {
			throw new GlideinException(
					"Unable to create endpoint reference: "+
					e.getMessage(),e);
		}
	}
	
	public static void main(String[] args)
	{
		try {
			AddressingUtil.getSiteEPR("juve.usc.edu:8080", 1);
			AddressingUtil.getSiteFactoryEPR("juve.usc.edu:8080");
			AddressingUtil.getGlideinEPR("juve.usc.edu:8080",1);
			AddressingUtil.getGlideinFactoryEPR("juve.usc.edu:8080");
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}

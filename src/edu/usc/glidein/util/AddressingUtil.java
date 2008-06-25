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
package edu.usc.glidein.util;

import java.net.URL;

import org.apache.axis.message.MessageElement;
import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.addressing.ReferencePropertiesType;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.SimpleResourceKey;

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
	
	public static EndpointReferenceType getSiteFactoryEPR(URL factoryURL)
	throws Exception
	{
		return createEPR(factoryURL);
	}
	
	public static EndpointReferenceType getSiteEPR(URL serviceURL, int id)
	throws Exception
	{
		ResourceKey key = getSiteKey(id);
		return createEPR(serviceURL, key);
	}
	
	public static EndpointReferenceType getGlideinFactoryEPR(URL factoryURL)
	throws Exception
	{
		return createEPR(factoryURL);
	}
	
	public static EndpointReferenceType getGlideinEPR(URL serviceURL, int id)
	throws Exception
	{
		ResourceKey key = getGlideinKey(id);
		return createEPR(serviceURL, key);
	}
	
	public static ResourceKey getGlideinKey(int id)
	{
		return new SimpleResourceKey(GlideinNames.RESOURCE_KEY, new Integer(id));
	}
	
	public static ResourceKey getSiteKey(int id)
	{
		return new SimpleResourceKey(SiteNames.RESOURCE_KEY, new Integer(id));
	}
}

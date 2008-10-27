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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.wsrf.NotificationConsumerManager;
import org.globus.wsrf.NotifyCallback;
import org.globus.wsrf.WSNConstants;
import org.globus.wsrf.encoding.DeserializationException;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.oasis.wsn.NotificationProducer;
import org.oasis.wsn.Subscribe;
import org.oasis.wsn.TopicExpressionType;
import org.oasis.wsn.WSBaseNotificationServiceAddressingLocator;
import org.w3c.dom.Element;

import edu.usc.glidein.service.SiteNames;
import edu.usc.glidein.stubs.RemoveRequest;
import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.service.SiteServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStateChange;
import edu.usc.glidein.stubs.types.SiteStateChangeMessage;
import edu.usc.glidein.util.AddressingUtil;

public class SiteService extends BaseService
{
	private NotificationConsumerManager consumer = null;
	private EndpointReferenceType consumerEPR = null;
	private SiteHandler handler = null;
	private Set<SiteListener> listeners = new HashSet<SiteListener>();
	
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
	
	public synchronized void addListener(SiteListener listener)
	throws GlideinException
	{
		if (listeners.size() == 0) {
			subscribe();
		}
		listeners.add(listener);
	}
	
	public synchronized void removeListener(SiteListener listener)
	throws GlideinException
	{
		listeners.remove(listener);
		if (listeners.size() == 0) {
			unsubscribe();
		}
	}
	
	private void subscribe() throws GlideinException
	{
		try {
			handler = new SiteHandler();
			
			consumer = NotificationConsumerManager.getInstance();
			consumer.startListening();
			
			consumerEPR = consumer.createNotificationConsumer(handler);
			
			TopicExpressionType topicExpression = new TopicExpressionType();
			topicExpression.setDialect(WSNConstants.SIMPLE_TOPIC_DIALECT);
			topicExpression.setValue(SiteNames.TOPIC_STATE_CHANGE);
			
			Subscribe subscribe = new Subscribe();
			subscribe.setUseNotify(Boolean.TRUE);
			subscribe.setConsumerReference(consumerEPR);
			subscribe.setTopicExpression(topicExpression);
	
			NotificationProducer producer = getProducerPort();
			
			producer.subscribe(subscribe);
		} catch (Exception e) {
			throw new GlideinException("Unable to subscribe to site",e);
		}
	}
	
	private void unsubscribe() throws GlideinException
	{
		try {
			consumer.removeNotificationConsumer(consumerEPR);
			consumer.stopListening();
			consumer = null;
			consumerEPR = null;
			handler = null;
		} catch (Exception e) {
			throw new GlideinException("Unable to unsubscribe from site",e);
		}
	}
	
	private NotificationProducer getProducerPort() throws GlideinException
	{
		try {
			WSBaseNotificationServiceAddressingLocator locator = 
				new WSBaseNotificationServiceAddressingLocator();
			
			NotificationProducer producer = 
				locator.getNotificationProducerPort(getEPR());
			
			if (getDescriptor() != null) {
				((Stub)producer)._setProperty(
						"clientDescriptor", getDescriptor());
			}
			
			return producer;
		} catch (ServiceException e) {
			throw new GlideinException("Unable to get notification producer",e);
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
	
	private class SiteHandler implements NotifyCallback
	{
		public void deliver(List list, EndpointReferenceType producer, Object message)
		{
			SiteStateChange stateChange = null;
			
			if (message instanceof SiteStateChangeMessage) {
				stateChange = ((SiteStateChangeMessage) message).getSiteStateChange();
			} else if (message instanceof SiteStateChange) {
				stateChange = (SiteStateChange)message;
			} else {
				try {
					stateChange = (SiteStateChange) ObjectDeserializer.toObject(
							(Element) message, SiteStateChange.class);
				} catch (DeserializationException e) {
					throw new RuntimeException(
							"Unable to deserialize site state change message",e);
				}
			}
			
			for (SiteListener listener : listeners) {
				listener.stateChanged(stateChange);
			}
		}
	}
	
	public static void main(String[] args)
	{
		try {
			SiteService s = new SiteService(
					new URL("https://juve.usc.edu:8443/wsrf/services/glidein/SiteService"),59);
			s.addListener(new SiteListener(){
				public void stateChanged(SiteStateChange stateChange)
				{
					System.out.println("State changed: "+stateChange.getState());
				}
			});
			
			System.out.println("Waiting for notifications");
			while (true) {
				try {
					Thread.sleep(30000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

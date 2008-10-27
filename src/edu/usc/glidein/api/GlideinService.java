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

import edu.usc.glidein.service.GlideinNames;
import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.service.GlideinServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinStateChange;
import edu.usc.glidein.stubs.types.GlideinStateChangeMessage;
import edu.usc.glidein.util.AddressingUtil;

public class GlideinService extends BaseService
{	
	private NotificationConsumerManager consumer = null;
	private EndpointReferenceType consumerEPR = null;
	private GlideinHandler handler = null;
	private Set<GlideinListener> listeners = new HashSet<GlideinListener>();
	
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

	public synchronized void addListener(GlideinListener listener)
	throws GlideinException
	{
		if (listeners.size() == 0) {
			subscribe();
		}
		listeners.add(listener);
	}
	
	public synchronized void removeListener(GlideinListener listener)
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
			handler = new GlideinHandler();
			
			consumer = NotificationConsumerManager.getInstance();
			consumer.startListening();
			
			consumerEPR = consumer.createNotificationConsumer(handler);
			
			TopicExpressionType topicExpression = new TopicExpressionType();
			topicExpression.setDialect(WSNConstants.SIMPLE_TOPIC_DIALECT);
			topicExpression.setValue(GlideinNames.TOPIC_STATE_CHANGE);
			
			Subscribe subscribe = new Subscribe();
			subscribe.setUseNotify(Boolean.TRUE);
			subscribe.setConsumerReference(consumerEPR);
			subscribe.setTopicExpression(topicExpression);
	
			NotificationProducer producer = getProducerPort();
			
			producer.subscribe(subscribe);
		} catch (Exception e) {
			throw new GlideinException("Unable to subscribe to glidein",e);
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
			throw new GlideinException("Unable to unsubscribe from glidein",e);
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
	
	private class GlideinHandler implements NotifyCallback
	{
		public void deliver(List list, EndpointReferenceType producer, Object message)
		{
			GlideinStateChange stateChange = null;
			
			if (message instanceof GlideinStateChangeMessage) {
				stateChange = ((GlideinStateChangeMessage) message).getGlideinStateChange();
			} else if (message instanceof GlideinStateChange) {
				stateChange = (GlideinStateChange)message;
			} else {
				try {
					stateChange = (GlideinStateChange) ObjectDeserializer.toObject(
							(Element) message, GlideinStateChange.class);
				} catch (DeserializationException e) {
					throw new RuntimeException(
							"Unable to deserialize glidein state change message",e);
				}
			}
			
			for (GlideinListener listener : listeners) {
				listener.stateChanged(stateChange);
			}
		}
	}
	
	public static void main(String[] args)
	{
		try {
			GlideinService s = new GlideinService(
					new URL("https://juve.usc.edu:8443/wsrf/services/glidein/GlideinService"),70);
			s.addListener(new GlideinListener(){
				public void stateChanged(GlideinStateChange stateChange)
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

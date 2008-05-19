package edu.usc.glidein.client;

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.stubs.*;
import edu.usc.glidein.stubs.service.*;
import edu.usc.glidein.stubs.types.*;

public class GlideinClient 
{
	public static void main(String[] args) 
	{
		GlideinFactoryServiceAddressingLocator glideinFactoryLocator = new GlideinFactoryServiceAddressingLocator();
		GlideinServiceAddressingLocator glideinInstanceLocator = new GlideinServiceAddressingLocator();
		
		try {
			String glideinFactoryURI = "http://juve.usc.edu:8080/wsrf/services/glidein/GlideinFactoryService";
			
			// Look up the factory
			EndpointReferenceType factoryEPR = new EndpointReferenceType();
			factoryEPR.setAddress(new Address(glideinFactoryURI));
			System.out.println(factoryEPR.toString());
			GlideinFactoryPortType glideinFactory = glideinFactoryLocator.getGlideinFactoryPortTypePort(factoryEPR);
			
			EnvironmentVariable[] env = new EnvironmentVariable[3];
			env[0] = new EnvironmentVariable("/bin","PATH");
			env[1] = new EnvironmentVariable("/home","HOME");
			env[2] = new EnvironmentVariable("shell","SHELL");
			
			Glidein glidein = new Glidein();
			glidein.setSiteId(11);
			glidein.setCount(1);
			glidein.setHostCount(1);
			glidein.setWallTime(30);
			glidein.setNumCpus(2);
			glidein.setGcbBroker("192.168.0.1");
			glidein.setIdleTime(30);
			glidein.setDebug(null);
			glidein.setEnvironment(env);
			
			EndpointReferenceType glideinRef = glideinFactory.createGlidein(glidein);
			System.out.println(glideinRef.toString());
			
			GlideinPortType instanceService = glideinInstanceLocator.getGlideinPortTypePort(glideinRef);
			
			GlideinStatus status = instanceService.getStatus(new EmptyObject());
			if (status==null) {
				System.out.println("Glidein status null");
			} else {
				System.out.printf("Glidein status: %s (%s)\n",status.getCode().toString(),status.getMessage());
			}
			
			glidein = instanceService.getGlidein(new EmptyObject());
			if (glidein==null) {
				System.out.println("Glidein null");
			} else {
				System.out.printf("Glidein: %s (%d)\n",glidein.getCondorHost(),glidein.getId());
			}
			
			//instanceService.delete(new EmptyObject());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
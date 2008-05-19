package edu.usc.glidein.client;


import javax.xml.soap.SOAPHeaderElement;

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.stubs.*;
import edu.usc.glidein.stubs.bindings.SitePortTypeSOAPBindingStub;
import edu.usc.glidein.stubs.service.*;
import edu.usc.glidein.stubs.types.*;

public class SiteClient 
{
	public static void main(String[] args) 
	{
		SiteFactoryServiceAddressingLocator siteFactoryLocator = new SiteFactoryServiceAddressingLocator();
		SiteServiceAddressingLocator siteInstanceLocator = new SiteServiceAddressingLocator();
		
		try {
			String siteFactoryURI = "http://juve.usc.edu:8080/wsrf/services/glidein/SiteFactoryService";
			
			// Look up the factory
			EndpointReferenceType factoryEPR = new EndpointReferenceType();
			factoryEPR.setAddress(new Address(siteFactoryURI));
			System.out.println(factoryEPR.toString());
			SiteFactoryPortType siteFactory = siteFactoryLocator.getSiteFactoryPortTypePort(factoryEPR);
			
			// Create site
			String name = "dynamic";
			String installPath = "/home/geovault-00/juve/glidein";
			String localPath = "/home/geovault-00/juve/glidein/local";
			String fork = "dynamic.usc.edu:2119/jobmanager-fork";
			String pbs = "dynamic.usc.edu:2119/jobmanager-pbs";
			String queue = null;
			String project = null;
			String condorPackage = "7.0.0-x86-Linux-2.6-glibc2.3.tar.gz";
			String condorVersion = "7.0.0";
			String proxy = "proxy";
			
			ExecutionService stagingService = new ExecutionService();
			stagingService.setServiceType(ServiceType.GT2);
			stagingService.setServiceContact(fork);
			stagingService.setProxy(proxy);
			
			ExecutionService glideinService = new ExecutionService();
			glideinService.setServiceType(ServiceType.GT2);
			glideinService.setServiceContact(pbs);
			glideinService.setProxy(proxy);
			glideinService.setQueue(queue);
			glideinService.setProject(project);
			
			EnvironmentVariable[] env = new EnvironmentVariable[3];
			env[0] = new EnvironmentVariable("/bin","PATH");
			env[1] = new EnvironmentVariable("/home","HOME");
			env[2] = new EnvironmentVariable("shell","SHELL");
			
			Site site = new Site();
			site.setName(name);
			site.setCondorVersion(condorVersion);
			site.setCondorPackage(condorPackage);
			site.setInstallPath(installPath);
			site.setLocalPath(localPath);
			site.setSubmitPath("/submit");
			site.setStagingService(stagingService);
			site.setGlideinService(glideinService);
			site.setEnvironment(env);
			
			EndpointReferenceType siteRef = siteFactory.createSite(site);
			System.out.println(siteRef.toString());
			
			SitePortType instanceService = siteInstanceLocator.getSitePortTypePort(siteRef);
			
			SitePortTypeSOAPBindingStub stub = (SitePortTypeSOAPBindingStub)instanceService;
			if (stub.getHeaders() == null) System.out.println("Null headers");
			else if(stub.getHeaders().length == 0) System.out.println("Empty headers");
			else for (SOAPHeaderElement elem : stub.getHeaders())
				System.out.println(elem.toString());
			
			SiteStatus status = instanceService.getStatus(new EmptyObject());
			if (status==null) {
				System.out.println("Site status null");
			} else {
				System.out.printf("Site status: %s (%s)\n",status.getCode().toString(),status.getMessage());
			}
			
			site = instanceService.getSite(new EmptyObject());
			if (site==null) {
				System.out.println("Site null");
			} else {
				System.out.printf("Site: %s (%d)\n",site.getName(),site.getId());
			}
			
			instanceService.delete(new EmptyObject());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
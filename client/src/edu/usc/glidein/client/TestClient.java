package edu.usc.glidein.client;

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;

import edu.usc.glidein.common.util.Base64;
import edu.usc.glidein.service.stubs.*;
import edu.usc.glidein.service.stubs.service.*;
import edu.usc.glidein.service.types.*;

public class TestClient 
{
	public static void main(String[] args) 
	{
		PoolFactoryServiceAddressingLocator factoryLocator = new PoolFactoryServiceAddressingLocator();
		PoolServiceAddressingLocator instanceLocator = new PoolServiceAddressingLocator();
		try {
			String factoryURI = "http://juve.usc.edu:8080/wsrf/services/glidein/PoolFactoryService";
			String siteName = "Mysite";
			
			// Look up the factory
			EndpointReferenceType factoryEPR = new EndpointReferenceType();
			factoryEPR.setAddress(new Address(factoryURI));
			PoolFactoryPortType factory = factoryLocator.getPoolFactoryPortTypePort(factoryEPR);
			
			// Create a resource
			CreatePoolResourceRequest request = new CreatePoolResourceRequest();
			PoolDescription poolDescription = new PoolDescription();
			poolDescription.setCondorHost("juve.usc.edu");
			poolDescription.setCondorPort(9816);
			poolDescription.setCondorVersion("6.8.5");
			request.setPoolDescription(poolDescription);
			CreatePoolResourceResponse createResponse = factory.createPoolResource(request);
			EndpointReferenceType instanceEPR = createResponse.getEndpointReference();
			System.out.println("Created resource!");
			
			// Look up the service
			PoolPortType service = instanceLocator.getPoolPortTypePort(instanceEPR);
			
			// Create a site
			CreateSiteRequest createSite = new CreateSiteRequest();
			SiteDescription siteDescription = new SiteDescription();
			siteDescription.setName(siteName);
			siteDescription.setInstallPath("/install/path");
			siteDescription.setLocalPath("/local/path");
			
			ExecutionService glideinService = new ExecutionService();
			glideinService.setProject("nqi");
			glideinService.setQueue("normal");
			glideinService.setServiceContact("grid-abe.ncsa.teragrid.org/jobmanager-pbs");
			glideinService.setServiceType(ServiceType.GT2);
			
			ExecutionService stagingService = new ExecutionService();
			stagingService.setServiceContact("grid-abe.ncsa.teragrid.org/jobmanager-fork");
			stagingService.setServiceType(ServiceType.GT2);
			
			siteDescription.setGlideinService(glideinService);
			siteDescription.setStagingService(stagingService);
			createSite.setSiteDescription(siteDescription);
			service.createSite(createSite);
			System.out.println("Created site!");
			
			// Create a glidein
			CreateGlideinRequest createGlidein = new CreateGlideinRequest();
			GlideinDescription glideinDescription = new GlideinDescription();
			glideinDescription.setSiteName(siteName);
			glideinDescription.setCount(1);
			glideinDescription.setHostCount(1);
			glideinDescription.setConfiguration(Base64.toBase64("CONDOR_HOST=${GLIDEIN_CONDOR_HOST}"));
			createGlidein.setGlideinDescription(glideinDescription);
			CreateGlideinResponse createdGlidein = service.createGlidein(createGlidein);
			System.out.println("Created glidein!");
			
			QueryPoolResponse queryPoolResponse = service.queryPool(new QueryPoolRequest());
			poolDescription = queryPoolResponse.getPool();
			System.out.println("Got pool: "+
								poolDescription.getCondorHost()+" "+
								poolDescription.getCondorPort()+" "+
								poolDescription.getCondorVersion());
			
			QuerySiteResponse querySiteResponse = service.querySite(new QuerySiteRequest());
			SiteDescription[] sites = querySiteResponse.getSites();
			for(SiteDescription site : sites){
				System.out.println("Got site: "+
								   site.getId()+" "+
								   site.getName());
			}
			
			QueryGlideinResponse queryGlideinResponse = service.queryGlidein(new QueryGlideinRequest());
			GlideinDescription[] glideins = queryGlideinResponse.getGlideins();
			for(GlideinDescription glidein : glideins){
				System.out.println("Got glidein: "+
						   glidein.getId()+" "+
						   glidein.getSiteName()+" "+
						   glidein.getCount()+" "+
						   glidein.getHostCount());
			}
			
			// Destroy glidein
			int glideinId = createdGlidein.getGlideinId();
			DestroyGlideinRequest destroyGlidein = new DestroyGlideinRequest();
			destroyGlidein.setSiteName(siteName);
			destroyGlidein.setGlideinId(glideinId);
			service.destroyGlidein(destroyGlidein);
			System.out.println("Destroyed glidein!");
			
			// Destroy site
			DestroySiteRequest destroySite = new DestroySiteRequest();
			destroySite.setSiteName(siteName);
			service.destroySite(destroySite);
			System.out.println("Destroyed site!");
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
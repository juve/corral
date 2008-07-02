package edu.usc.glidein.catalog;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

public class PegasusSiteCatalog implements SiteCatalog
{
	private Map<String,Properties> sites;
	
	public PegasusSiteCatalog(File xmlFile) throws SiteCatalogException
	{
		sites = new HashMap<String,Properties>();
		read(xmlFile);
	}
	
	private void read(File xmlFile) throws SiteCatalogException
	{
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(xmlFile, new PegasusSiteCatalogHandler());
		} catch (Exception e) {
			throw new SiteCatalogException("Unable to read site catalog",e);
		}
	}
	
	public Site getSite(String name) throws SiteCatalogException
	{
		Properties properties = sites.get(name);
		if (properties == null) {
			throw new SiteCatalogException("Site "+name+" not found");
		} else {
			return createSite(properties);
		}
	}
	
	private String getRequired(Properties p, String key) 
	throws SiteCatalogException
	{
		String value = p.getProperty(key);
		if (value == null || "".equals(value)) {
			throw new SiteCatalogException(
					"Missing required attribute "+key);
		}
		return value.trim();
	}
	
	private Site createSite(Properties p) throws SiteCatalogException
	{
		Site s = new Site();
		
		String name = getRequired(p,"name");
		s.setName(name);
		
		s.setInstallPath(getRequired(p,"installPath"));
		s.setLocalPath(getRequired(p,"localPath"));
		
		String condorPackage = p.getProperty("condorPackage");
		String condorVersion = p.getProperty("condorVersion");
		if (condorPackage == null && condorVersion == null) {
			throw new SiteCatalogException(
					"Must specify either condorPackage or condorVersion");
		} else {
			s.setCondorPackage(condorPackage);
			s.setCondorVersion(condorVersion);
		}
		
		/* Staging service */
		try {
			String staging = getRequired(p,"stagingService");
			String[] comp = staging.trim().split("[ ]", 2);
			ExecutionService stagingService = new ExecutionService();
			stagingService.setProject(p.getProperty("stagingService.project"));
			stagingService.setQueue(p.getProperty("stagingService.queue"));
			stagingService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			stagingService.setServiceContact(comp[1]);
			s.setStagingService(stagingService);
		} catch (Exception e) {
			throw new SiteCatalogException("Unable to create staging service " +
					"for site '"+name+"'. Are you sure you used the right " +
					"format for stagingService?");
		}
		
		/* Glidein service */
		try {
			String glidein = getRequired(p,"glideinService");
			String[] comp = glidein.trim().split("[ ]", 2);
			ExecutionService glideinService = new ExecutionService();
			glideinService.setProject(p.getProperty("glideinService.project"));
			glideinService.setQueue(p.getProperty("glideinService.queue"));
			glideinService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			glideinService.setServiceContact(comp[1]);
			s.setGlideinService(glideinService);
		} catch (Exception e) {
			throw new SiteCatalogException("Unable to create glidein service " +
					"for site '"+name+"'. Are you sure you used the right " +
					"format for glideinService?");
		}
		
		/* Environment */
		String env = p.getProperty("environment");
		if (env!=null) {
			List<EnvironmentVariable> envs = new LinkedList<EnvironmentVariable>();
			Pattern pat = Pattern.compile("([^=]+)=([^:]+):?");
			Matcher mat = pat.matcher(env);
			while (mat.find()) {
				EnvironmentVariable e = new EnvironmentVariable();
				e.setVariable(mat.group(1));
				e.setValue(mat.group(2));
				envs.add(e);
			}
			s.setEnvironment(envs.toArray(new EnvironmentVariable[0]));
		}
		
		return s;
	}
	
	private void addSiteProperties(Properties properties) 
	{
		sites.put(properties.getProperty("name"), properties);
	}
	
	private class PegasusSiteCatalogHandler extends DefaultHandler
	{
		private Properties properties = null;
		private boolean readingProperty = false;
		private String key = null;
		private StringBuilder value = null;
		
		public PegasusSiteCatalogHandler()
		{
			properties = new Properties();
		}
		
		public void startElement(String uri, String localName, String qname,
				Attributes attributes) throws SAXException
		{
			if ("profile".equals(qname)) {
				String namespace = attributes.getValue("namespace");
				String mykey = attributes.getValue("key");
				
				if ("pegasus".equals(namespace) && 
						mykey.startsWith("glidein")) {
					
					readingProperty = true;
					key = mykey.substring("glidein.".length());
					value = new StringBuilder();
					
				}
			} else if ("site".equals(qname)) {
				
				String site = attributes.getValue("handle");
				properties.put("name", site);
				
			}
		}
		
		public void characters(char[] ac, int i, int j) throws SAXException
		{
			if (readingProperty) {
				value.append(ac,i,j);
			}
		}
		
		public void endElement(String uri, String localName, String qname)
		throws SAXException
		{
			if ("profile".equals(qname)) {
				if (readingProperty) {
					properties.put(key, value.toString());
					value = null;
					key = null;
					readingProperty = false;
				}
			} else if ("site".equals(qname)) {
				addSiteProperties(properties);
				properties = new Properties();
			}
		}
	}
}

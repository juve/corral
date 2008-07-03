package edu.usc.glidein.catalog;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.SiteUtil;

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
			try {
				return SiteUtil.createSite(properties);
			} catch (Exception e) {
				throw new SiteCatalogException("Unable to create site: "+e.getMessage(),e);
			}
		}
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

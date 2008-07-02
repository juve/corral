package edu.usc.glidein.catalog;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.glidein.cli.CommandException;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.INI;

public class INISiteCatalog implements SiteCatalog
{
	private INI ini;
	
	public INISiteCatalog(File iniFile) throws SiteCatalogException
	{
		read(iniFile);
	}
	
	private void read(File iniFile) throws SiteCatalogException
	{
		try {
			ini = new INI();
			ini.read(iniFile);
		} catch (Exception e) {
			throw new SiteCatalogException(
					"Error reading INI catalog file.",e);
		}
	}
	
	public Site getSite(String name) throws SiteCatalogException
	{
		if (ini.hasSection(name)) {
			return extractINISite(ini,name);
		} else {
			throw new SiteCatalogException(
					"Site '"+name+"' not found in site catalog");
		}
	}
	
	private Site extractINISite(INI ini, String name) throws SiteCatalogException
	{
		Site s = new Site();
		s.setName(name);
		s.setInstallPath(getINIValue(ini,name,"installPath"));
		s.setLocalPath(getINIValue(ini,name,"localPath"));
		s.setCondorPackage(getINIValue(ini,name,"condorPackage"));
		s.setCondorVersion(getINIValue(ini,name,"condorVersion"));
		
		/* Staging service */
		try {
			String staging = getINIValue(ini,name,"stagingService");
			if (staging == null) {
				throw new CommandException(
						"Missing required parameter 'stagingService' " +
						"for site '"+name+"'");
			}
			String[] comp = staging.trim().split("[ ]", 2);
			ExecutionService stagingService = new ExecutionService();
			stagingService.setProject(getINIValue(ini,name,"stagingService.project"));
			stagingService.setQueue(getINIValue(ini,name,"stagingService.queue"));
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
			String glidein = getINIValue(ini,name,"glideinService");
			if (glidein == null) {
				throw new CommandException(
						"Missing required parameter 'glideinService' " +
						"for site '"+name+"'");
			}
			String[] comp = glidein.trim().split("[ ]", 2);
			ExecutionService glideinService = new ExecutionService();
			glideinService.setProject(getINIValue(ini,name,"glideinService.project"));
			glideinService.setQueue(getINIValue(ini,name,"glideinService.queue"));
			glideinService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			glideinService.setServiceContact(comp[1]);
			s.setGlideinService(glideinService);
		} catch (Exception e) {
			throw new SiteCatalogException("Unable to create glidein service " +
					"for site '"+name+"'. Are you sure you used the right " +
					"format for glideinService?");
		}
		
		/* Environment */
		String env = getINIValue(ini,name,"environment");
		if (env!=null) {
			List<EnvironmentVariable> envs = new LinkedList<EnvironmentVariable>();
			Pattern p = Pattern.compile("([^=]+)=([^:]+):?");
			Matcher m = p.matcher(env);
			while (m.find()) {
				EnvironmentVariable e = new EnvironmentVariable();
				e.setVariable(m.group(1));
				e.setValue(m.group(2));
				envs.add(e);
			}
			s.setEnvironment(envs.toArray(new EnvironmentVariable[0]));
		}
		
		return s;
	}

	private String getINIValue(INI ini, String site, String key)
	{
		String value = ini.getString(site, key, null);
		if (value == null) {
			value = ini.getString(key, null);
		}
		if (value != null) {
			value = value.trim();
		}
		return value;
	}
}

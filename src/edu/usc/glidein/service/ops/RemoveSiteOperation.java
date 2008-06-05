package edu.usc.glidein.service.ops;

import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.impl.SiteResource;
import edu.usc.glidein.service.impl.SiteResourceHome;
import edu.usc.glidein.util.AddressingUtil;
import edu.usc.glidein.util.NamingUtil;

public class RemoveSiteOperation
{
	private SiteResource resource = null;
	private GlobusCredential credential = null;
	
	public RemoveSiteOperation(SiteResource res, GlobusCredential cred)
	{
		this.resource = res;
		this.credential = cred;
	}
	
	public void invoke()
	{
		
	}
	
	public void remove() throws Exception
	{
		// TODO: Cancel staging operations
		
		// TODO: Cancel running glideins
		
		// TODO: Submit uninstall job
		
		ResourceKey key = AddressingUtil.getSiteKey(resource.getSite().getId());
		
		// Delete the site
		resource.delete();
		
		// Remove the site from the resource home
		SiteResourceHome resourceHome = NamingUtil.getSiteResourceHome();
		resourceHome.remove(key);
	}
}

package edu.usc.glidein.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.MissingResourceException;

import javax.security.auth.Subject;

import org.apache.axis.MessageContext;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.util.ConfigUtil;
import org.globus.wsrf.impl.security.authentication.Constants;
import org.globus.wsrf.security.SecurityManager;

import edu.usc.glidein.GlideinException;

/**
 * Manipulate proxies
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class ProxyUtil
{

	public static String readProxy(String proxyFile) throws IOException
	{
		File file = new File(proxyFile);
		String proxy = IOUtil.read(file);
		return proxy;
	}
	
	public static String readProxy() throws IOException
	{
		String location = System.getProperty("X509_USER_PROXY");
		if(location == null)
		{
			location = System.getenv("X509_USER_PROXY");
			if(location == null)
			{
				location = ConfigUtil.discoverProxyLocation();
			}
		}
		return readProxy(location);
	}
	
	public static void writeProxy(GlobusCredential proxy, File proxyFile) 
	throws IOException, GlideinException
	{
		// Write proxy file
		FileOutputStream fos = new FileOutputStream(proxyFile);
		proxy.save(fos);
		fos.close();
		
		// Change permissions
		CommandLine chmod = new CommandLine();
		chmod.setExecutable(new File("/bin/chmod"));
		chmod.addArgument("600");
		chmod.addArgument(proxyFile.getAbsolutePath());
		chmod.execute();
		int exitCode = chmod.getExitCode();
		if(exitCode != 0){
			throw new GlideinException(
					"Unable to change proxy permissions\n\n"+
					"Stdout:\n"+chmod.getOutput()+
					"Stderr:\n"+chmod.getError());
		}
	}
	
	/*
	 
	THESE TWO METHODS WERE NEEDED WHEN USING THE DELEGATION SERVICE. NOW THIS IS DONE
	USING THE DELEGATION MODE OF GSI SECURE CONVERSATION.
	 
	public void delegateCredential()
	{
		//DelegationUtil.delegate(delegationServiceUrl, issuingCred, certificate, fullDelegation, desc);
	}
	
	public GlobusCredential getDelegatedCredential(String host, String id)
	throws GlideinException
	{
		
		try {
			// Create resource key
			ResourceKey key = new SimpleResourceKey(
					new QName(DelegationConstants.NS,"DelegationKey"), id);
			
			// Get EPR
			EndpointReferenceType epr = new EndpointReferenceType();
			ReferencePropertiesType referenceProperties = new ReferencePropertiesType();
			MessageElement elem = (MessageElement)key.toSOAPElement();
			referenceProperties.set_any(new MessageElement[] {
					(MessageElement)elem
			});
			epr.setProperties(referenceProperties);
			epr.setAddress(new Address("http://"+host+
					DelegationConstants.SERVICE_BASE_PATH+DelegationConstants.SERVICE_PATH));
			
			// Get resource
			DelegationResource resource = DelegationUtil.getDelegationResource(epr);
			
			// Get credential
			GlobusCredential cred = resource.getCredential();

			return cred;
		} catch(Exception e){
			throw new GlideinException("Unable to find delegated credential",e);
		}
	}
	*/
	
	/**
	 * This only works if the client uses secure conversation and
	 * enables GSI delegation mode.
	 * 
	 * @see org.globus.wsrf.security.Constants.GSI_SEC_CONV
	 * @see org.globus.axis.gsi.GSIConstants.GSI_MODE
	 * @return The delegated credential.
	 */
	public static GlobusCredential getCallerCredential() throws MissingResourceException
	{
		MessageContext msgCtx = MessageContext.getCurrentContext();
		Subject subject = (Subject) msgCtx.getProperty(Constants.PEER_SUBJECT);
		if (subject == null) {
			throw new MissingResourceException("Delegated credential not found: " +
					"peer Subject not found: " +
					"make sure you set GSI_MODE to GSI_MODE_FULL_DELEG",
					Subject.class.getName(),null);
		} else {
			GlobusCredential cred = null;
			for (Object o : subject.getPrivateCredentials()) {
				if (o instanceof GlobusGSSCredentialImpl) {
					cred = ((GlobusGSSCredentialImpl)o).getGlobusCredential();
				}
			}
			if (cred == null) {
				throw new MissingResourceException("Delegated credential not found: " +
						"Subject did not contain delegated credential: " +
						"make sure you set GSI_MODE to GSI_MODE_FULL_DELEG",
						GlobusCredential.class.getName(),null);
			}
			return cred;
		}
	}
	
	/** 
	 * This only works if the client turns on secure conversation or
	 * secure message mode.
	 * @see org.globus.wsrf.security.Constants.GSI_SEC_CONV
	 * @see org.globus.wsrf.security.Constants.GSI_SEC_MSG
	 * @return The callers subject name
	 */
	public static String getCallerDN()
	{
		SecurityManager manager = SecurityManager.getManager();
        String callerDN = manager.getCaller();
        return callerDN;
	}
}

package edu.usc.glidein.util;

import org.globus.wsrf.security.SecurityException;
import org.globus.wsrf.security.SecurityManager;

public class AuthenticationUtil
{
	public static void authenticate() throws SecurityException
	{
		SecurityManager manager = SecurityManager.getManager();
		
		String callerDN = manager.getCaller();
		if (callerDN == null) {
			throw new SecurityException("Caller DN not set");
		}
		
		String[] localNames = manager.getLocalUsernames();
		
		String localName = null;
        if ((localNames != null) && (localNames.length > 0)) {
            localName = localNames[0];
        }

        if (localName == null) {
        	throw new SecurityException("Subject not in grid-mapfile");
        }
	}
	
	public static String getSubject() throws SecurityException
	{
		SecurityManager manager = SecurityManager.getManager();
		
		return manager.getCaller();
	}
	
	public static String getLocalUsername() throws SecurityException
	{
		SecurityManager manager = SecurityManager.getManager();
		
		String[] localNames = manager.getLocalUsernames();
		
		String localName = null;
        if ((localNames != null) && (localNames.length > 0)) {
            localName = localNames[0];
        }
        
        return localName;
	}
}

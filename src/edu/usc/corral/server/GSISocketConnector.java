/*
 *  Copyright 2009 University Of Southern California
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
package edu.usc.corral.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.globus.common.ChainedIOException;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.gsi.TrustedCertificates;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.net.GssOutputStream;
import org.globus.gsi.gssapi.net.GssSocket;
import org.globus.gsi.gssapi.net.impl.GSIGssSocket;
import org.globus.security.gridmap.GridMap;
import org.gridforum.jgss.ExtendedGSSContext;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.mortbay.io.EndPoint;
import org.mortbay.io.bio.SocketEndPoint;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.bio.SocketConnector;

import edu.usc.corral.config.ServerConfiguration;

public class GSISocketConnector extends SocketConnector {
	
	private ServerConfiguration config;
	
	public GSISocketConnector(ServerConfiguration config) {
		super();
		this.config = config;
	}
	
	public boolean isConfidential(Request request) {
		/* May not be encrypted */
		return config.isRequestEncryption();
	}
	
	public boolean isIntegral(Request request) {
		/* SSL will always be integral */ 
		return true;
	}

	public void customize(EndPoint endpoint, Request request) throws IOException {
		super.customize(endpoint, request);
		try {
			SocketEndPoint socket_end_point = (SocketEndPoint)endpoint;
			GssSocket socket = (GssSocket)socket_end_point.getTransport();
			
			// GSS Context
			ExtendedGSSContext context = (ExtendedGSSContext)socket.getContext();
			request.setAttribute(GSIAttributes.GSI_CONTEXT, context);
			
			// User DN
			String userDN = context.getSrcName().toString();
			request.setAttribute(GSIAttributes.GSI_USER_DN, userDN);
			
			// Delegated credentials (GSI mode only)
			GSSCredential cred = (GSSCredential)context.getDelegCred();
			request.setAttribute(GSIAttributes.GSI_CREDENTIALS, cred);
			
			// Username
			GridMap gridMap = config.getGridMap();
			gridMap.refresh();
	        String username = gridMap.getUserID(userDN);
	        request.setAttribute(GSIAttributes.GSI_AUTH_USERNAME, username);
		} catch (GSSException e) {
			IOException i = new IOException("Error setting GSI request attributes");
			i.initCause(e);
			throw i;
		}
	}
	
	/**
	 * host, port, backlog are ignored. The values from the configuration are used instead.
	 */
	protected ServerSocket newServerSocket(String x, int y, int z) throws IOException {
		
		GSSCredential gssCred = null;
        GlobusCredential cred = null;

        // Load credentials
        try {
        	String proxy = config.getProxy();
        	String certificate = config.getCertificate();
        	String key = config.getKey();
            if (proxy != null && !proxy.equals("")) {
                cred = new GlobusCredential(proxy);
            } else if (certificate != null && key != null) {
                cred = new GlobusCredential(certificate, key);
            }
            
            if (cred != null) {
                gssCred = new GlobusGSSCredentialImpl(cred, GSSCredential.ACCEPT_ONLY);
            }
        } catch (GlobusCredentialException e) {
            throw new ChainedIOException("Failed to load server credentials", e);
        } catch (GSSException e) {
            throw new ChainedIOException("Failed to load server credentials", e);
        }

        // Get trusted certificates
        TrustedCertificates trustedCerts = null;
        if (config.getCacertdir() != null) {
            trustedCerts = TrustedCertificates.load(config.getCacertdir());
        }
        
        InetAddress addr = config.getHost() == null ? null : InetAddress.getByName(config.getHost());

        GSIServerSocket serverSocket = new GSIServerSocket(config.getPort(), config.getBacklog(), addr);
        serverSocket.setCredentials(gssCred);
        serverSocket.setTrustedCertificates(trustedCerts);
        
        return serverSocket;
	}
	
	private class GSIServerSocket extends ServerSocket {
        protected TrustedCertificates _trustedCerts;
        private GSSCredential _credentials;
        private GSSManager _manager;
        
        protected GSIServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
            super(port, backlog, bindAddr);
            this._manager = ExtendedGSSManager.getInstance();
        }
        
        public void setTrustedCertificates(TrustedCertificates trustedCerts) {
            this._trustedCerts = trustedCerts;
        }

        public void setCredentials(GSSCredential creds) {
            this._credentials = creds;
        }

        public Socket accept() throws IOException {
            try {
            	Socket s = super.accept();
                ExtendedGSSContext context = (ExtendedGSSContext)_manager.createContext(_credentials);
                
                // Tells if the connection is encrypted
                context.requestConf(config.isRequestEncryption());
                
                context.setOption(GSSConstants.GSS_MODE, config.getGssMode());
                
                if(config.isAnonymousAllowed()) {
                	// Allow anonymous connections
                	context.setOption(GSSConstants.REQUIRE_CLIENT_AUTH, Boolean.FALSE);
                	context.setOption(GSSConstants.ACCEPT_NO_CLIENT_CERTS, Boolean.TRUE);
            	} else {
                	// Clients must authenticate with a certificate
            		context.setOption(GSSConstants.REQUIRE_CLIENT_AUTH, Boolean.TRUE);
                	context.setOption(GSSConstants.ACCEPT_NO_CLIENT_CERTS, Boolean.FALSE);
            	}
                
                // Only trust clients presenting certs from these authorities
                if (_trustedCerts != null) {
                	context.setOption(GSSConstants.TRUSTED_CERTIFICATES, _trustedCerts);
                }
                
                GSIGssSocket gs = new GSIGssSocket(s, context);
	            
	         	// This means its a server socket
	            gs.setUseClientMode(false); 
	            
	            // No idea what this does, but its configurable
	            gs.setWrapMode(config.getWrapMode());
	            
	            // We will authorize clients using the grid-mapfile
	            gs.setAuthorization(config.getAuthorization());
	            
	            // THIS IS REQUIRED
	            ((GssOutputStream)gs.getOutputStream()).setAutoFlush(true);
	            
	            return gs;
            } catch (GSSException e) {
                throw new ChainedIOException("Failed to init GSS context", e);
            }
        }
    }
	
	public String toString() {
		String h = _serverSocket.getInetAddress().getHostAddress();
		int p = _serverSocket.getLocalPort();
		return GSISocketConnector.class.getSimpleName()+"@"+h+":"+p;
	}
}
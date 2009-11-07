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
package edu.usc.corral.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.globus.common.CoGProperties;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSManagerImpl;
import org.globus.gsi.gssapi.auth.Authorization;
import org.globus.gsi.gssapi.auth.GSSAuthorization;
import org.globus.gsi.gssapi.net.GssOutputStream;
import org.globus.gsi.gssapi.net.impl.GSIGssSocket;
import org.gridforum.jgss.ExtendedGSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import edu.usc.corral.config.ClientConfiguration;
import edu.usc.corral.config.GlobusConfiguration;
import edu.usc.corral.types.ErrorResponse;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.ListGlideinsResponse;

public class Client {
	
	private ClientConfiguration config = ClientConfiguration.getDefault();
	private SimpleHttpConnectionManager connmgr;
	private HttpClient httpclient;
	private boolean closed = false;
	
	public Client(String host, int port) {
		Protocol httpg = new Protocol("httpg", new SocketFactory(), 8443);
		
		HostConfiguration hc = new HostConfiguration();
		hc.setHost(host, port, httpg);
		
		connmgr = new SimpleHttpConnectionManager();
		
		httpclient = new HttpClient();
		httpclient.setHostConfiguration(hc);
		httpclient.setHttpConnectionManager(connmgr);
	}
	
	public void setConfiguration(ClientConfiguration config) {
		this.config = config;
	}
	public GlobusConfiguration getConfiguration() {
		return this.config;
	}
	
	private void ensureOpen() {
		if (closed)
			throw new IllegalStateException("Connection closed");
	}
	
	private void handleError(InputStream is) throws GlideinException {
		ErrorResponse resp;
		try {
			Serializer serializer = new Persister();
			resp = serializer.read(ErrorResponse.class, is);
		} catch (Exception e) {
			throw new GlideinException("Unable to parse error response");
		}
		throw new RemoteException(resp);
	}
	
	public <T> T doGet(String path, Class<? extends T> responseType) throws GlideinException {
		ensureOpen();
		
		Serializer serializer = new Persister();
		GetMethod get = new GetMethod(path);
		try {
			int status = httpclient.executeMethod(get);
			InputStream is = get.getResponseBodyAsStream();
			if (status<200 || status>299) {
				handleError(is);
			}
			return serializer.read(responseType, is);
		} catch (HttpException he) {
			throw new GlideinException("Error parsing http", he);
		} catch (IOException ioe) {
			throw new GlideinException("Error communicating with server", ioe);
		} catch (GlideinException ge) {
			throw ge;
		} catch (Exception e) {
			throw new GlideinException("Unable to parse response xml", e);
		} finally {
			get.releaseConnection();
		}
	}
	
	public <T> T doPost(String path, Class<? extends T> responseType, Object request) throws GlideinException {
		ensureOpen();
		
		Serializer serializer = new Persister();
		RequestEntity entity = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.write(request, baos);
			entity = new ByteArrayRequestEntity(baos.toByteArray(), "text/xml");
		} catch (Exception e) {
			throw new GlideinException("Unable to generate request xml", e);
		}
		
		PostMethod post = new PostMethod(path);
		try {
			post.setRequestEntity(entity);
			int status = httpclient.executeMethod(post);
			InputStream is = post.getResponseBodyAsStream();
			if (status<200 || status>299) {
				handleError(is);
			}
			return serializer.read(responseType, is);
		} catch (HttpException he) {
			throw new GlideinException("Error parsing http", he);
		} catch (IOException ioe) {
			throw new GlideinException("Error communicating with server", ioe);
		} catch (GlideinException ge) {
			throw ge;
		} catch (Exception e) {
			throw new GlideinException("Unable to parse response xml", e);
		} finally {
			post.releaseConnection();
		}
	}
	
	public void close() {
		if (closed) return;
		connmgr.shutdown();
		closed = true;
	}
	
	private class SocketFactory implements ProtocolSocketFactory {
		public SocketFactory() {
		}
		
		private ExtendedGSSContext getClientContext(String host) throws GSSException {
			GSSManager manager = new GlobusGSSManagerImpl();
			
			GSSCredential cred = manager.createCredential(GSSCredential.INITIATE_AND_ACCEPT);
			
			// XXX: When doing delegation targetName cannot be null.
			// additional authorization will be performed after the handshake
			// in the socket code.
			GSSName targetName = null;
			if (config.getDelegation() != null) {
				Authorization authz = config.getAuthorization();
				if (authz instanceof GSSAuthorization) {
					targetName = ((GSSAuthorization)authz).getExpectedName(null, host);
				}
			}
			
			int lifetime;
			if (config.getLifetime() == 0) {
				lifetime = cred.getRemainingLifetime();
			} else {
				lifetime = config.getLifetime();
			}
			
			ExtendedGSSContext context = (ExtendedGSSContext) manager.createContext(
					targetName, GSSConstants.MECH_OID, cred, lifetime);
			
			context.requestConf(config.isRequestEncryption());
			context.requestAnonymity(config.isRequestAnonymous());
			context.requestCredDeleg(config.getDelegation()!=null);
			if (config.getDelegation()!=null) {
				context.setOption(GSSConstants.DELEGATION_TYPE, config.getDelegation());
			}
			context.setOption(GSSConstants.GSS_MODE, config.getGssMode());
			context.setOption(GSSConstants.REJECT_LIMITED_PROXY, 
					Boolean.valueOf(config.isRejectLimitedProxy()));
			
			return context;
		}
		
		public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
			return createSocket(host, port, null, 0);
		}
		
		public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
			return createSocket(host, port, localAddress, localPort);
		}
		
		public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params)
		throws IOException, UnknownHostException, ConnectTimeoutException {
			if (host == null) {
				throw new IllegalArgumentException("Target host may not be null.");
			}
			
			if (params == null) {
				throw new IllegalArgumentException("Parameters may not be null.");
			}
			
			Socket sock = new Socket();
			
			if (localAddress == null)
				localAddress = InetAddress.getLocalHost();
			
			if (localPort > 0) {
				// explicit port
				InetSocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
	        	sock.bind(localaddr);
			} else {
				String range = CoGProperties.getDefault().getTcpSourcePortRange();
				if (range == null) {
					// Bind to an ephemeral port
					InetSocketAddress localaddr = new InetSocketAddress(localAddress, 0);
					sock.bind(localaddr);
				} else {
					// Bind in range
					int minport, maxport;
					try {
						int idx = range.indexOf(',');
						minport = Integer.parseInt(range.substring(0,idx).trim());
						maxport = Integer.parseInt(range.substring(idx+1).trim());
					} catch (Exception e) {
						throw new IOException("Invalid tcp source port range: "+range);
					}
				
					for(localPort=minport; localPort<maxport; localPort++) {
			            try {
			            	InetSocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
			            	sock.bind(localaddr);
			            	break;
			            } catch(BindException e) {
			            	// continue
			            }
					}
				}
			}
			
			// Connect socket
			SocketAddress remoteaddr = new InetSocketAddress(host, port);
			int timeout = params.getConnectionTimeout();
			try {
				sock.connect(remoteaddr, timeout);
			} catch (SocketTimeoutException ex) {
				throw new ConnectTimeoutException("Connect to "+remoteaddr+" timed out");
			}
			
			// Wrap the socket in GSI
			try {
				ExtendedGSSContext context = getClientContext(host);
				GSIGssSocket gs =  new GSIGssSocket(sock, context);
				gs.setWrapMode(config.getWrapMode());
				gs.setAuthorization(config.getAuthorization());
				((GssOutputStream)gs.getOutputStream()).setAutoFlush(true);
				return gs;
			} catch (GSSException gsse) {
				try { sock.close(); }
				catch (Exception e) {}
				IOException i = new IOException("Unable to establish GSS context");
				i.initCause(gsse);
				throw i;
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		Client client = new Client("127.0.0.1", 8443);
		client.getConfiguration().setAuthorization("none");
		ListRequest req = new ListRequest();
		ListGlideinsResponse resp = client.doPost("/glidein/list", ListGlideinsResponse.class, req);
		System.out.println(resp.getGlideins());
		long stop = System.currentTimeMillis();
		System.out.println("Elapsed: "+(stop-start)+" ms");
	}
}

package edu.usc.glidein.service.impl;

import javax.xml.namespace.QName;

import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceIdentifier;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourceProperty;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.impl.SimpleResourcePropertySet;
import org.globus.wsrf.impl.ReflectionResourceProperty;

import edu.usc.glidein.service.core.Pool;
import edu.usc.glidein.service.core.PoolFactory;
import edu.usc.glidein.service.stubs.CreatePoolResourceRequest;

public class PoolResource implements Resource, ResourceIdentifier, ResourceProperties
{
	/** Resource key. This uniquely identifies this resource. */
	private Object key;
	
	/** Resource Property set */
	private ResourcePropertySet propertySet;
	
	/** Pool object */
	private Pool pool;
	
	/* Initializes RPs */
	public void initialize(CreatePoolResourceRequest request) throws Exception
	{
		PoolFactory factory = PoolFactory.getInstance();
		this.pool = factory.createPool(request.getPoolDescription());
		
		this.key = new Integer(pool.getId());
		
		this.propertySet = new SimpleResourcePropertySet(
				PoolQNames.RESOURCE_PROPERTIES);
		
		ResourceProperty condorHostRP = 
			new ReflectionResourceProperty(PoolQNames.RP_CONDOR_HOST,
										   "CondorHost",
										   pool);
		this.propertySet.add(condorHostRP);
		
		ResourceProperty condorPortRP = 
			new ReflectionResourceProperty(PoolQNames.RP_CONDOR_PORT,
										   "CondorPort",
										   pool);
		this.propertySet.add(condorPortRP);
		
		ResourceProperty condorVersionRP = 
			new ReflectionResourceProperty(PoolQNames.RP_CONDOR_VERSION, 
										   "CondorVersion", 
										   pool);
		this.propertySet.add(condorVersionRP);	
	}

	/**
	 * Required by interface ResourceIdentifier
	 * @return
	 */
	public Object getID() {
		return this.key;
	}
	
	/**
	 *  Required by interface ResourceProperties 
	 */
	public ResourcePropertySet getResourcePropertySet() {
		return this.propertySet;
	}
	
	public void addProperty(ResourceProperty property)
	{
		this.propertySet.add(property);
	}
	
	public void removeProperty(QName qname)
	{
		this.propertySet.remove(qname);
	}
	
	public Pool getPool()
	{
		return this.pool;
	}
}

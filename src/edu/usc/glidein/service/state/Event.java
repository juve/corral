package edu.usc.glidein.service.state;

import java.util.HashMap;
import java.util.Map;

import org.globus.wsrf.ResourceKey;

public abstract class Event implements Runnable
{
	private EventCode code;
	private Map<String,Object> properties;
	private ResourceKey key;
	
	public Event(EventCode code, ResourceKey key)
	{
		this.code = code;
		this.key = key;
		properties = new HashMap<String,Object>();
	}

	public EventCode getCode()
	{
		return code;
	}

	public void setCode(EventCode code)
	{
		this.code = code;
	}

	public ResourceKey getKey()
	{
		return key;
	}

	public void setKey(ResourceKey key)
	{
		this.key = key;
	}
	
	public void setProperty(String key, Object value)
	{
		properties.put(key, value);
	}
	
	public Object getProperty(String key)
	{
		return properties.get(key);
	}
}

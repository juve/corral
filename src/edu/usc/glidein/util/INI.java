package edu.usc.glidein.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * INI configuration file reader
 */
public class INI 
{
	private static final String GLOBAL_SECTION = "__global__";
	
	private Map<String,Map<String,String>> sections;
	
	public INI()
	{
 		sections = new TreeMap<String,Map<String,String>>();
 		sections.put(GLOBAL_SECTION, new TreeMap<String,String>());
	}
	
	public boolean hasSection(String name)
	{
		return sections.containsKey(name);
	}
	
	public List<String> getSections()
	{
		Set<String> kset = sections.keySet();
		LinkedList<String> klist = new LinkedList<String>(kset);
		klist.remove(GLOBAL_SECTION);
		Collections.sort(klist);
		return klist;
	}
	
	public String getString(String key, String def)
	{
		return getString(GLOBAL_SECTION,key,def);
	}
	
	public String getString(String section, String key, String def)
	{
		Map<String,String> secmap = sections.get(section);
		if(secmap==null) return def;
		return secmap.get(key);
	}
	
	public void setString(String key, String value)
	{
		setString(GLOBAL_SECTION,key,value);
	}
	
	public void setString(String section, String key, String value)
	{
		if(section==null) throw new IllegalArgumentException("section null");
		if(key==null) throw new IllegalArgumentException("key null");
		if(value==null) value = "";
		
		Map<String,String> secmap = sections.get(section);
		if(secmap==null)
		{
			secmap = new TreeMap<String,String>();
			sections.put(section, secmap);
		}
		secmap.put(key, value);
	}
	
	public int getInt(String key, int def)
	throws NumberFormatException
	{
		return getInt(GLOBAL_SECTION,key,def);
	}
	
	public int getInt(String section, String key, int def)
	throws NumberFormatException
	{
		Map<String,String> secmap = sections.get(section);
		if(secmap==null) return def;
		String value = secmap.get(key);
		if(value==null) return def;
		return Integer.parseInt(value.trim());
	}
	
	public void setInt(String key, int value)
	{
		setInt(GLOBAL_SECTION,key,value);
	}
	
	public void setInt(String section, String key, int value)
	{
		setString(section,key,Integer.toString(value));
	}
	
	public double getDouble(String key, double def)
	throws NumberFormatException
	{
		return getDouble(GLOBAL_SECTION,key,def);
	}
	
	public double getDouble(String section, String key, double def)
	throws NumberFormatException
	{
		Map<String,String> secmap = sections.get(section);
		if(secmap==null) return def;
		String value = secmap.get(key);
		if(value==null) return def;
		return Double.parseDouble(value.trim());
	}
	
	public void setDouble(String key, double value)
	{
		setDouble(GLOBAL_SECTION,key,value);
	}
	
	public void setDouble(String section, String key, double value)
	{
		setString(section,key,Double.toString(value));
	}
	
	public long getLong(String key, long def)
	throws NumberFormatException
	{
		return getLong(GLOBAL_SECTION,key,def);
	}
	
	public long getLong(String section, String key, long def) 
	throws NumberFormatException
	{
		Map<String,String> secmap = sections.get(section);
		if(secmap==null) return def;
		String value = secmap.get(key);
		if(value==null) return def;
		return Long.parseLong(value.trim());
	}
	
	public void setLong(String key, long value)
	{
		setLong(GLOBAL_SECTION,key,value);
	}
	
	public void setLong(String section, String key, long value)
	{
		setString(section,key,Long.toString(value));
	}
	
	public void read(File iniFile) 
	throws IOException, ParseException
	{
		if(!iniFile.exists() || !iniFile.isFile() || !iniFile.canRead())
			throw new IOException("Unable to read INI file: "+iniFile.getName());
		FileInputStream stream = new FileInputStream(iniFile);
		this.read(stream);
		stream.close();
	}
	
	public void read(InputStream iniFile) 
	throws IOException, ParseException
	{
		String currentSection = GLOBAL_SECTION;
		int currentLine = 1;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(iniFile));
		
		String line = reader.readLine();
		while(line!=null){
			String trimmed = line.trim();
			
			if(trimmed.length()==0){
				//Skip all blank lines
			}
			else if(trimmed.charAt(0)==';' || trimmed.charAt(0)=='#')
			{
				// Skip all comments
			}
			else if(trimmed.charAt(0)=='['){
				// [new_section]
				if(trimmed.indexOf(']')==trimmed.length()-1)
				{
					currentSection = trimmed.substring(1,trimmed.length()-1);
					sections.put(currentSection, new TreeMap<String,String>());
					if(!isValidIdentifier(currentSection))
					{
						throwParseException("Invalid section name: "+
											currentSection,currentLine);
					}
				}
				else
				{
					throwParseException("Invalid section header: "+trimmed,
										currentLine);
				}
			}
			else
			{
				// "key = value" or "key : value"
				
				// Get key
				int sep = line.indexOf('=');
				if (line.indexOf(':')>=0) sep = Math.min(line.indexOf('='), line.indexOf(':'));
				if(sep<0) throwParseException("Missing separator",currentLine);
				String key = line.substring(0,sep).trim();
				if(!isValidIdentifier(key))
					throwParseException("Invalid key: "+key,currentLine);
				
				// Get value
				StringBuffer value = new StringBuffer(line.substring(sep+1));
				if (value.length() > 0) {
					while(value.charAt(value.length()-1)=='\\') {
						// Allow continuation lines
						value.deleteCharAt(value.length()-1);
						String next = reader.readLine();
						if(next==null)
							throwParseException("Unterminated continuation",
												currentLine);
						currentLine++;
						value.append(next);
					}
				}
				
				// Add pair
				setString(currentSection, key, value.toString());
			}
			
			line = reader.readLine();
			currentLine++;
		}
	}
	
	private void throwParseException(String message, int lineNumber)
	throws ParseException
	{
		throw new ParseException("Line "+lineNumber+": "+message,lineNumber);
	}
	
	private boolean isValidIdentifier(String id)
	{
		if(id==null || id.length()==0) return false;
		if(id.matches("[a-zA-Z0-9][-_a-zA-Z0-9]*")) return true;
		return false;
	}
	
	public void write(File iniFile) throws IOException
	{
		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(iniFile));
		this.write(writer);
		writer.close();
	}
	
	public void write(OutputStream iniFile) throws IOException
	{
		PrintWriter writer = new PrintWriter(iniFile);
		for(String section : sections.keySet()){
			Map<String,String> secmap = sections.get(section);
			if(!section.equals(GLOBAL_SECTION)){
				writer.write("\n["+section+"]\n\n");
			}
			for(String key : secmap.keySet()){
				writer.write(key + " =" + secmap.get(key)+"\n");
			}
		}
	}
}

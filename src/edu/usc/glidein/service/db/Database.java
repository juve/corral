package edu.usc.glidein.service.db;

import javax.naming.Context;
import javax.naming.InitialContext;

// TODO: Implement file persistence
// TODO: Add history tracking

public abstract class Database
{
	public static Database getDatabase() throws DatabaseException
	{
		String location = "java:comp/env/glidein/Database";
		try {
			Context initialContext = new InitialContext();
	    	return (Database)initialContext.lookup(location);
		} catch (Exception e) {
			throw new DatabaseException("Unable to load database: "+location,e);
		}
	}
	
	public abstract SiteDAO getSiteDAO();
	public abstract GlideinDAO getGlideinDAO();
}

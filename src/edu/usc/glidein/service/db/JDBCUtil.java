/*
 *  Copyright 2007-2008 University Of Southern California
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
package edu.usc.glidein.service.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class JDBCUtil
{
	private static Logger logger = Logger.getLogger(JDBCUtil.class);
	
	public static void closeQuietly(ResultSet rs)
	{
		if (rs!=null) {
			try {
				rs.close();
			} catch(SQLException sqle) {
				logger.warn("Unable to close ResultSet",sqle);
			}
		}
	}
	
	public static void close(ResultSet rs) throws DatabaseException
	{
		if (rs!=null) {
			try {
				rs.close();
			} catch(SQLException sqle) {
				throw new DatabaseException("Unable to close ResultSet",sqle);
			}
		}
	}
	
	public static void closeQuietly(Statement stmt)
	{
		if (stmt!=null) {
			try {
				stmt.close();
			} catch(SQLException sqle) {
				logger.warn("Unable to close Statment",sqle);
			}
		}
	}
	
	public static void close(Statement stmt) throws DatabaseException
	{
		if (stmt!=null) {
			try {
				stmt.close();
			} catch(SQLException sqle) {
				throw new DatabaseException("Unable to close Statement",sqle);
			}
		}
	}
	
	public static void closeQuietly(Connection conn)
	{
		if (conn!=null) {
			try {
				conn.close();
			} catch(SQLException sqle) {
				logger.warn("Unable to close Connection",sqle);
			}
		}
	}
	
	public static void close(Connection conn) throws DatabaseException
	{
		if (conn!=null) {
			try {
				conn.close();
			} catch(SQLException sqle) {
				throw new DatabaseException("Unable to close Connection",sqle);
			}
		}
	}
	
	public static void rollbackQuietly(Connection conn)
	{
		if (conn!=null) {
			try {
				conn.rollback();
			} catch(SQLException sqle) {
				logger.warn("Unable to rollback Connection",sqle);
			}
		}
	}
	
	public static void rollback(Connection conn) throws DatabaseException
	{
		if (conn!=null) {
			try {
				conn.rollback();
			} catch(SQLException sqle) {
				throw new DatabaseException("Unable to rollback Connection",sqle);
			}
		}
	}
}

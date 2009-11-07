/*
 *  Copyright 2007-2009 University Of Southern California
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
package edu.usc.corral.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.usc.corral.db.DatabaseException;
import edu.usc.corral.db.GlideinDAO;
import edu.usc.corral.db.JDBCUtil;
import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.GlideinState;

public class SQLGlideinDAO implements GlideinDAO {
	private SQLDatabase database = null;
	
	public SQLGlideinDAO(SQLDatabase db) {
		this.database = db;
	}

	public Connection getConnection() throws DatabaseException {
		return database.getConnection();
	}
	
	public int create(Glidein glidein) throws DatabaseException {
		Connection conn = null;
		int id = 0;
		try {
			conn = getConnection();
			id = createGlidein(conn, glidein);
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to create glidein: commit failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
		return id;
	}
	
	private int createGlidein(Connection connection, Glidein glidein) throws DatabaseException {
		int id = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("INSERT INTO glidein (site, count, hostCount, wallTime, numCpus, condorConfig, gcbBroker, idleTime, condorDebug, state, shortMessage, longMessage, created, lastUpdate, condorHost, resubmit, submits, resubmits, until, rsl, subject, localUsername, lowport, highport, ccbAddress) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			int i = 1;
			stmt.setInt(i++, glidein.getSiteId());
			stmt.setInt(i++, glidein.getCount());
			stmt.setInt(i++, glidein.getHostCount());
			stmt.setInt(i++, glidein.getWallTime());
			stmt.setInt(i++, glidein.getNumCpus());
			stmt.setString(i++, glidein.getCondorConfig());
			stmt.setString(i++, glidein.getGcbBroker());
			stmt.setInt(i++, glidein.getIdleTime());
			stmt.setString(i++, glidein.getCondorDebug());
			stmt.setString(i++, glidein.getState().toString());
			stmt.setString(i++, glidein.getShortMessage());
			stmt.setString(i++, glidein.getLongMessage());
			Date created = glidein.getCreated();
			if (created == null) {
				stmt.setTimestamp(i++, null);
			} else {
				stmt.setTimestamp(i++, new Timestamp(created.getTime()));
			}
			Date lastUpdate = glidein.getLastUpdate();
			if (lastUpdate == null) {
				stmt.setTimestamp(i++, null);
			} else {
				stmt.setTimestamp(i++, new Timestamp(lastUpdate.getTime()));
			}
			stmt.setString(i++, glidein.getCondorHost());
			stmt.setBoolean(i++, glidein.getResubmit());
			stmt.setObject(i++, glidein.getSubmits());
			stmt.setObject(i++, glidein.getResubmits());
			Date until = glidein.getUntil();
			if (until == null) {
				stmt.setTimestamp(i++, null);
			} else {
				stmt.setTimestamp(i++, new Timestamp(until.getTime()));
			}
			stmt.setString(i++, glidein.getRsl());
			stmt.setString(i++, glidein.getSubject());
			stmt.setString(i++, glidein.getLocalUsername());
			stmt.setObject(i++, glidein.getLowport());
			stmt.setObject(i++, glidein.getHighport());
			stmt.setString(i++, glidein.getCcbAddress());
			
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to create glidein: wrong number of db updates");
			}
			
			// Get new ID
			rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
				glidein.setId(id);
			} else {
				throw new DatabaseException("Unable to get generated glidein id");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to create glidein: insert failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return id;
	}		

	public Glidein load(int glideinId) throws DatabaseException {
		Connection conn = null;
		try {
			conn = getConnection();
			return getGlidein(conn, glideinId);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private Glidein getGlidein(Connection connection, int glideinId) throws DatabaseException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT g.*, s.name as siteName FROM glidein g, site s WHERE g.id=? AND g.site=s.id");
			stmt.setInt(1, glideinId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				return newGlidein(rs);
			} else {
				throw new DatabaseException("Glidein "+glideinId+" not found");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load glidein: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	public void delete(int glideinId) throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			/* Cascaded deletes should take care of the other tables */
			stmt = conn.prepareStatement("DELETE FROM glidein WHERE id=?");
			stmt.setInt(1, glideinId);
			if (stmt.executeUpdate() != 1) {
				throw new DatabaseException("Unable to delete glidein: wrong number of db updates");
			}
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to delete glidein: delete failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public void updateState(int glideinId, GlideinState state, String shortMessage, String longMessage, Date time) throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("UPDATE glidein SET state=?, shortMessage=?, longMessage=?, lastUpdate=? WHERE id=?");
			int i = 1;
			stmt.setString(i++, state.toString());
			stmt.setString(i++, shortMessage);
			stmt.setString(i++, longMessage);
			stmt.setTimestamp(i++, new Timestamp(time.getTime()));
			stmt.setInt(i++, glideinId);
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to update glidein state: wrong number of db updates");
			}
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to update glidein status: update failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
		
	}
	
	public int[] listIds() throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		LinkedList<Integer> results = new LinkedList<Integer>();
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("SELECT id FROM glidein");
			rs = stmt.executeQuery();
			while (rs.next()) {
				results.add(rs.getInt("id"));
			}
			
			int[] ids = new int[results.size()];
			int i = 0;
			for (Integer id : results) {
				ids[i++] = id;
			}
			return ids;
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to get glidein ids: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public int[] listTerminated() throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		LinkedList<Integer> results = new LinkedList<Integer>();
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("SELECT id FROM glidein WHERE state in (?,?)");
			stmt.setString(1, GlideinState.FINISHED.toString());
			stmt.setString(2, GlideinState.FAILED.toString());
			rs = stmt.executeQuery();
			while (rs.next()) {
				results.add(rs.getInt("id"));
			}
			
			int[] ids = new int[results.size()];
			int i = 0;
			for (Integer id : results) {
				ids[i++] = id;
			}
			return ids;
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to get finished glidein ids: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public List<Glidein> list(boolean longFormat, String user, boolean allUsers) throws DatabaseException {
		Connection conn = null;
		try {
			conn = getConnection();
			return getGlideins(conn, user, allUsers);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private List<Glidein> getGlideins(Connection connection, String user, boolean allUsers) throws DatabaseException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		LinkedList<Glidein> results = new LinkedList<Glidein>();
		try {
			// Join to get the site name
			stmt = connection.prepareStatement("SELECT g.*, s.name as siteName FROM glidein g, site s WHERE g.site=s.id"+(allUsers?"":" AND g.localUsername=?"));
			if (!allUsers) {
				stmt.setString(1, user);
			}
			rs = stmt.executeQuery();
			while (rs.next()) {
				results.add(newGlidein(rs));
			}
			return results;
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load glidein: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	private Glidein newGlidein(ResultSet rs) throws DatabaseException {
		try {
			Glidein glidein = new Glidein();
			glidein.setId(rs.getInt("id"));
			glidein.setSiteId(rs.getInt("site"));
			glidein.setSiteName(rs.getString("siteName"));
			glidein.setCount(rs.getInt("count"));
			glidein.setHostCount(rs.getInt("hostCount"));
			glidein.setWallTime(rs.getInt("wallTime"));
			glidein.setNumCpus(rs.getInt("numCpus"));
			glidein.setCondorConfig(rs.getString("condorConfig"));
			glidein.setGcbBroker(rs.getString("gcbBroker"));
			glidein.setIdleTime(rs.getInt("idleTime"));
			glidein.setCondorDebug(rs.getString("condorDebug"));
			glidein.setCondorHost(rs.getString("condorHost"));
			glidein.setState(GlideinState.valueOf(rs.getString("state")));
			glidein.setShortMessage(rs.getString("shortMessage"));
			glidein.setLongMessage(rs.getString("longMessage"));
			glidein.setCreated(rs.getTimestamp("created"));
			glidein.setLastUpdate(rs.getTimestamp("lastUpdate"));
			glidein.setResubmit(rs.getBoolean("resubmit"));
			glidein.setSubmits(rs.getInt("submits"));
			glidein.setResubmits(rs.getInt("resubmits"));
			
			Timestamp until = rs.getTimestamp("until");
			if (until == null) {
				glidein.setUntil(null);
			} else {
				Date _until = new Date();
				_until.setTime(until.getTime());
				glidein.setUntil(_until);
			}
			glidein.setRsl(rs.getString("rsl"));
			glidein.setSubject(rs.getString("subject"));
			glidein.setLocalUsername(rs.getString("localUsername"));
			glidein.setLowport(rs.getInt("lowport"));
			glidein.setHighport(rs.getInt("highport"));
			glidein.setCcbAddress(rs.getString("ccbAddress"));
			
			return glidein;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to create Glidein object",sqle);
		}
	}
	
	public void incrementSubmits(int glideinId) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("UPDATE glidein SET submits=submits+1 WHERE id=?");
			stmt.setInt(1,glideinId);
			if (stmt.executeUpdate() != 1) {
				throw new DatabaseException(
						"Unable to increment submits: " +
						"wrong number of updates");
			}
			conn.commit();
		} catch (SQLException sqle) {
			throw new DatabaseException(
					"Unable to increment submits",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
}

package edu.usc.glidein.service.db;

import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinStatus;

public interface GlideinDAO
{
	public int create(Glidein glidein) throws DatabaseException;
	public void store(Glidein glidein) throws DatabaseException;
	public Glidein load(int glideinId) throws DatabaseException;
	public void delete(int glideinId) throws DatabaseException;
	public void updateStatus(int glideinId, GlideinStatus status, String statusMessage) throws DatabaseException;
	public Glidein[] list(boolean longFormat) throws DatabaseException;
}

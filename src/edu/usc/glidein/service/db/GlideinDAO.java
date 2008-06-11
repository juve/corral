package edu.usc.glidein.service.db;

import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinState;

public interface GlideinDAO
{
	public int create(Glidein glidein) throws DatabaseException;
	public void store(Glidein glidein) throws DatabaseException;
	public Glidein load(int glideinId) throws DatabaseException;
	public void delete(int glideinId) throws DatabaseException;
	public void updateState(int glideinId, GlideinState state, String shortMessage, String longMessage) throws DatabaseException;
	public Glidein[] list(boolean longFormat) throws DatabaseException;
}

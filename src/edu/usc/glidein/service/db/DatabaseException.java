package edu.usc.glidein.service.db;

public class DatabaseException extends Exception
{
	private static final long serialVersionUID = -2476495919487437666L;

	public DatabaseException() {
		super();
	}

	public DatabaseException(String message) {
		super(message);
	}

	public DatabaseException(Throwable throwable) {
		super(throwable);
	}

	public DatabaseException(String message, Throwable throwable) {
		super(message, throwable);
	}
}

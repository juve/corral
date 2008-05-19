package edu.usc.glidein.service.exec;

public enum CondorUniverse
{
	GRID ("grid"),
	VANILLA ("vanilla"),
	STANDARD ("standard"),
	PVM ("pvm"),
	SCHEDULER ("scheduler"),
	LOCAL ("local"),
	MPI ("mpi"),
	JAVA ("java"),
	VM ("vm");
	
	/**
	 * Universe type string
	 */
	private String typeString;
	
	private CondorUniverse(String typeString)
	{
		this.typeString = typeString;
	}
	
	/**
	 * The type string for this universe. This is what you would find in
	 * the submit script.
	 * @return The type string for this universe type
	 */
	public String getTypeString()
	{
		return typeString;
	}
	
	/**
	 * Get the universe type with matching type string
	 * @param typeString The type string to match
	 * @return The universe type that matches the type string
	 * @throws CondorException If there is no match
	 */
	public static CondorUniverse fromTypeString(String typeString)
	throws CondorException
	{
		for(CondorUniverse u : CondorUniverse.values())
		{
			if(u.typeString.equals(typeString))
				return u;
		}
		
		throw new CondorException(
				"Unrecognized Condor universe type: "+typeString);
	}
}

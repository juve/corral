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
	
	private String typeString;
	
	private CondorUniverse(String typeString)
	{
		this.typeString = typeString;
	}
	
	public String getTypeString()
	{
		return typeString;
	}
}

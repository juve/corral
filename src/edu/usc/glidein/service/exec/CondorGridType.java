package edu.usc.glidein.service.exec;

/**
 * Enum of the different grid types supported by Condor
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public enum CondorGridType
{
	GT2 ("gt2"),
	GT4 ("gt4"),
	CONDOR ("condor"),
	NORDUGRID ("nordugrid"),
	UNICORE ("unicore"),
	LSF ("lsf"),
	PBS ("pbs");
	
	/**
	 * The type string as it appears in the submit script
	 */
	private String typeString;
	
	private CondorGridType(String typeString)
	{
		this.typeString = typeString;
	}
	
	/**
	 * Get the type string. This is how the grid type appears in the submit
	 * script.
	 * @return The type string
	 */
	public String getTypeString()
	{
		return this.typeString;
	}
	
	/**
	 * Get the enum instance mapped to a given type string
	 * @param typeString The type string to map
	 * @return The enum instance for the associated type string
	 * @throws CondorException if there is no enum instance matching the given
	 * type string
	 */
	public static CondorGridType fromTypeString(String typeString)
	throws CondorException
	{
		for(CondorGridType gt : CondorGridType.values())
		{
			if(gt.typeString.equals(typeString))
				return gt;
		}
		
		throw new CondorException(
				"Unrecognized grid type: "+typeString);
	}
}

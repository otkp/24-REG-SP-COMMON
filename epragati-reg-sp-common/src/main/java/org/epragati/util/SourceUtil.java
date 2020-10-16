package org.epragati.util;

public enum SourceUtil {

	SCHEDULER( "Scheduler"),
	CITIZEN( "Citizen");
	private SourceUtil(String name) {
		this.name = name;
	}

	private String name;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	
	
	
}

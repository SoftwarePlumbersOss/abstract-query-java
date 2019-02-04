package com.softwareplumbers.common.abstractquery;

import javax.json.JsonObject;

/** Class representing a query parameter than can be set later.
*/
public class Param implements Comparable<Param> {
	
	// The name of the parameter
	public final String name;
	
	/** Create a new query parameter
	*
	* @param name - the name of the query parameter 
	*/
	public Param(String name) {
		this.name = name;
	}

	/** Create a new query parameter
	*	
	* @param name - the name of the query parameter 
	*/
	public static Param from(String name) {
		return new Param(name);
	}
	
	/** Create a new query parameter
	*	
	* @param name - the name of the query parameter 
	*/
	public static Param from(JsonObject obj) {
		return new Param(obj.getString("$"));
	}

	/** Check in an object is a parameter
	*
	* @param obj - object to check
	* @return true of obj is a Param
	*/
	public static boolean isParam(JsonObject obj) {
		return obj.containsKey("$");
	}

	@Override
	public int compareTo(Param o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Param && compareTo((Param)o) == 0;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	public String toString() {
		return name;
	}
}



package com.softwareplumbers.common.abstractquery;

import javax.json.JsonObject;

/** Class representing a query parameter than can be set later.
*/
class Param extends Value {
	
	/** Create a new query parameter
	*
	* @param name - the name of the query parameter 
	*/
	protected Param(String name) {
		super(Type.PARAM, name);
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
	* @returns true of obj is a Param
	*/
	public static boolean isParam(JsonObject obj) {
		return obj.containsKey("$");
	}

}



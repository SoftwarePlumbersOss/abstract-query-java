package com.softwareplumbers.common.abstractquery;

import java.math.BigDecimal;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

/** Class representing a query parameter than can be set later.
*/
public class Param  {
	

	/** Create a new query parameter
	*	
	* @param name - the name of the query parameter 
	*/
	public static JsonValue from(String name) {
		return Json.createObjectBuilder().add("$", name).build();
	}
	


	/** Check in an object is a parameter
	*
	* @param obj - object to check
	* @return true of obj is a Param
	*/
	public static boolean isParam(JsonValue obj) {
        if (obj.getValueType().equals(JsonValue.ValueType.OBJECT))
    		return ((JsonObject)obj).containsKey("$");
        return false;
	}

    public static String getKey(JsonValue obj) {
        if (obj.getValueType().equals(JsonValue.ValueType.OBJECT))
    		return ((JsonObject)obj).getString("$");
        throw new IllegalArgumentException("value not a parameter object");
    }
    
}



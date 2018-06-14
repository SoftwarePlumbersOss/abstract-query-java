package com.softwareplumbers.common.abstractquery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.beanutils.BeanMap;

/** Value type for Queries 
 * 
 * Implements a lazyish facade over either Json objects or regular POJOs. Immutable.
 * 
 * @author Jonathan
 *
 */
public class Value implements Comparable<Value> {
	
	/** Different types of value that are supported */
	public enum Type { NUMBER, STRING, PARAM, ARRAY, MAP };
	
	/** Holds data */
	public final Object value;
	/** Type of data */
	public final Type type;

	/** Protected constructor */
	protected Value(Type type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	/** Check if the supplied json value can be converted into a Value */
	public static boolean isAtomicValue(JsonValue obj) {
		return 		(obj instanceof JsonNumber) 
				|| 	(obj instanceof JsonString)
				||  (obj instanceof JsonObject && ((JsonObject)obj).containsKey("$"));
	}

	/** Convert from a JSON value 
	 * 
	 * JsonNumber, JsonString, and JsonArray objects map to values of type NUMBER, STRING, and ARRAY.
	 * JsonObject objects map to values of type MAP; getProperty on the returnd value will return the
	 * related property of the JSON object.
	 */
	public static Value from(JsonValue obj) {
		if (obj instanceof JsonNumber) {
			return new Value(Type.NUMBER, ((JsonNumber)obj).bigDecimalValue());
		} else if (obj instanceof JsonString)  {
			return new Value(Type.STRING, ((JsonString)obj).getString());
		} else if (obj instanceof JsonObject)  {
			return new Value(Type.MAP, ((JsonObject)obj));
		} else if (obj instanceof JsonArray) {
			return new Value(Type.ARRAY, ((JsonArray)obj));
		}
		throw new IllegalArgumentException("Not a value type:" + obj);
	}
	
	/** Convert from a long value */
	public static Value from(long value) {
		return new Value(Type.NUMBER, BigDecimal.valueOf(value));
	}

	/** Convert from a double value */
	public static Value from(double value) {
		return new Value(Type.NUMBER, BigDecimal.valueOf(value));
	}
	
	/** Convert from a string value */
	public static Value from(String value) {
		return new Value(Type.STRING, value);
	}
	
	/** Convert from a list of values
	 * 
	 *  A shallow copy is made of the value list. The list may be of JsonValue objects or any
	 *  POJO supported by from(value). Care should be taken if the list contains mutable java
	 *  objects. 
	 */
	public static Value from(List<?> values) {
		return new Value(Type.STRING, new ArrayList<Object>(values));
	}

	/** Convert from a list of values 
	 *
	 * The values array is not copied.
	 */
	public static Value from(Object... values) {
		return new Value(Type.ARRAY, Arrays.asList(values));
	}

	/** Convert from a POJO 
	 *
	 * Long, Double, and String, List, and JsonValue objects will be converted using the appropriate
	 * static conversion ('from') function. Other java objects will be treated as Beans; a Value of type
	 * MAP will be created such that Value.getProperty returns the appropriate property of the bean.
	 */
	public static Value from(Object value) {
		if (value instanceof List) return from((List<?>)value);
		if (value instanceof Double) return from((Double)value);
		if (value instanceof Long) return from((Long)value);
		if (value instanceof JsonValue) return Value.from((JsonValue) value);
		return new Value(Type.MAP, new BeanMap(value));
	}
	
	/** Convenience method equivalent to from(JsonUtil.parseValue(value) */
	public static Value fromJson(String value) {
		return from(JsonUtil.parseValue(value));		
	}
	
	/** Get the set of valid property names in this value.
	 * 
	 * @return a set of property names, empty if this value is not of type MAP.
	 */
	public Set<String> propertySet() {
		if (type == Type.MAP) {
			return ((Map<String,?>)value).keySet();
		} else {
			return Collections.emptySet();
		}
	}
	
	/** Get the number of elements in this array
	 * 
	 * @return the number of elements, zero if this value is not of type MAP.
	 */
	public int size() {
		if (type == Type.ARRAY) {
			return ((List<?>)value).size();
		} else if (type == Type.MAP) {
			return ((Map<String,?>)value).size();
		}
		return 0;
	}
	
	/** Get a property from this map 
	 * 
	 * @param key property name to get
	 * @return the value of the property
	 * @throws IllegalArgumentException if property does not exist
	 */
	public Value getProperty(String key) {
		if (!propertySet().contains(key)) throw new IllegalArgumentException("No such property " + key);
		return from(((Map<String,?>)value).get(key));
	}
	
	/** Get an element from this array 
	 * 
	 * @param index index of element to get
	 * @return the value of the element
	 * @throws IllegalArgumentException if index not valid
	 */
	public Value getElement(int index) {
		if (type != Type.ARRAY) throw new IllegalArgumentException("No such index " + index);
		return from(((List<?>)value).get(index));
	}
	
	/** Compare with another Value */
	@Override
	public int compareTo(Value other) {
		return ValueComparator.getInstance().compare(this, other);
	}
	
	/** Compare with another Value */
	public boolean equals(Value other) {
		return ValueComparator.getInstance().equals(this, other) == Boolean.TRUE;
	}
	
	/** Compare with another object */
	public boolean equals(Object other) {
		return other instanceof Value && equals((Value)other);
	}
	
	/** Convert value to a string */
	public String toString() {
		switch (type) {
		case STRING: 	return (String)value;
		case NUMBER: 	return value.toString();
		case MAP: 		return "{" + ((Map<String,?>)value).keySet()
							.stream()
							.map(key -> key + ": " + getProperty(key))
							.collect(Collectors.joining(",")) + "}";
		case ARRAY: 	return ((List<?>)value)
							.stream()
							.map(item->from(item).toString())
							.collect(Collectors.joining(","));
		case PARAM: 	return "$" + value;
		default:		throw new IllegalArgumentException("Unhandled type: " + type);
		}
	}
	
	/** Convert value to a number */
	public BigDecimal toNumber() {
		switch (type) {
		case STRING: 	return new BigDecimal((String)value);
		case NUMBER: 	return (BigDecimal)value;
		default:		throw new IllegalArgumentException("Unhandled type: " + type);
		}
		
	}
	
	/** Convert value to a Json object */
	public JsonValue toJSON() {
		switch (type) {
		case STRING: 	return Json.createValue((String)value);
		case NUMBER: 	return Json.createValue((BigDecimal)value);
		case MAP: 		JsonObjectBuilder obuilder = Json.createObjectBuilder();
						for (String property : propertySet()) obuilder.add(property, getProperty(property).toJSON());
						return obuilder.build();
		case ARRAY: 	JsonArrayBuilder abuilder = Json.createArrayBuilder();
						for (int i = 0; i < size(); i++) abuilder.add(getElement(i).toJSON());
						return abuilder.build();
		case PARAM: 	return Json.createObjectBuilder().add("$", (String)value).build();
		default:		throw new IllegalArgumentException("Unhandled type: " + type);
		}		
	}
}

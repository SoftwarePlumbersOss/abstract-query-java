package com.softwareplumbers.common.abstractquery;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.beanutils.BeanMap;

public class Value implements Comparable<Value> {
	
	enum Type { NUMBER, STRING, PARAM, MAP };
	
	public final Object value;
	public final Type type;
	
	protected Value(Type type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public static boolean isValue(JsonValue obj) {
		return (obj instanceof JsonNumber) || (obj instanceof JsonString) || (obj instanceof JsonObject && obj.asJsonObject().containsKey("$"));
	}

	public static Value from(JsonValue obj) {
		if (obj instanceof JsonNumber) {
			return new Value(Type.NUMBER, ((JsonNumber)obj).bigDecimalValue());
		} else if (obj instanceof JsonString)  {
			return new Value(Type.STRING, ((JsonString)obj).getString());
		} else if (obj instanceof JsonObject)  {
			return new Value(Type.MAP, ((JsonObject)obj));
		} 
		throw new IllegalArgumentException("Not a value type:" + obj);
	}
	
	public static Value from(long value) {
		return new Value(Type.NUMBER, BigDecimal.valueOf(value));
	}

	public static Value from(double value) {
		return new Value(Type.NUMBER, BigDecimal.valueOf(value));
	}
	
	public static Value from(String value) {
		return new Value(Type.STRING, value);
	}

	public static Value from(Object value) {
		if (value instanceof Double) return from((Double)value);
		if (value instanceof Long) return from((Long)value);
		if (value instanceof JsonValue) return Value.from((JsonValue) value);
		return new Value(Type.MAP, new BeanMap(value));
	}
	
	public static Value fromJson(String value) {
		return from(JsonUtil.parseValue(value));		
	}
	
	public Set<String> propertySet() {
		if (type == Type.MAP) {
			return ((Map<String,?>)value).keySet();
		} else {
			return Collections.emptySet();
		}
	}
	
	public Value getProperty(String key) {
		if (!propertySet().contains(key)) throw new IllegalArgumentException("No such property " + key);
		return from(((Map<String,?>)value).get(key));
	}
	
	@Override
	public int compareTo(Value other) {
		return ValueComparator.getInstance().compare(this, other);
	}
	
	public boolean equals(Value other) {
		return ValueComparator.getInstance().equals(this, other) == Boolean.TRUE;
	}
	
	public boolean equals(Object other) {
		return other instanceof Value && equals((Value)other);
	}
	
	public String toString() {
		switch (type) {
		case STRING: 	return (String)value;
		case NUMBER: 	return value.toString();
		case PARAM: 	return "$" + value;
		default:		throw new IllegalArgumentException("Unhandled type: " + type);
		}
	}
	
	public BigDecimal toNumber() {
		switch (type) {
		case STRING: 	return new BigDecimal((String)value);
		case NUMBER: 	return (BigDecimal)value;
		default:		throw new IllegalArgumentException("Unhandled type: " + type);
		}
		
	}
	
	public JsonValue toJSON() {
		switch (type) {
		case STRING: 	return Json.createValue((String)value);
		case NUMBER: 	return Json.createValue((BigDecimal)value);
		case PARAM: 	return Json.createObjectBuilder().add("$", (String)value).build();
		default:		throw new IllegalArgumentException("Unhandled type: " + type);
		}		
	}
}

package com.softwareplumbers.common.abstractquery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public abstract class Value {
	
	/** Different types of value that are supported */
	public enum Type { NUMBER, STRING, PARAM, ARRAY, BOOLEAN,  MAP };
	
	/** Type of data */
	public final Type type;

	/** Protected constructor */
	private Value(Type type) {
		this.type = type;
	}
	
	public abstract Boolean maybeEquals(Value other);
	public abstract JsonValue toJSON();
	
	public boolean equals(Value other) {
		return maybeEquals(other) == Boolean.TRUE;
	}
	
	public boolean equals(Object other) {
		return other instanceof Value && equals((Value)other);
	}
	
	public ArrayValue toArray() {
		if (type == Type.ARRAY) return (ArrayValue)this;
		throw new RuntimeException("tried to convert " + type + " to array");
	}
		
	public MapValue toMap() {
		if (type == Type.MAP) return (MapValue)this;
		throw new RuntimeException("tried to convert " + type + " to map");
	}

	public static class Atomic extends Value implements Comparable<Atomic> {

		public final Comparable value;
		
		public Atomic(Type type, Comparable value) {
			super(type);
			this.value = value;
		}
		
		public Integer maybeCompareTo(Atomic o) {
			if (type == o.type) return value.compareTo(o.value);
			if (type == Type.PARAM || o.type == Type.PARAM) return null;
			throw new IllegalArgumentException("cannot compare type " + type + " with " + o.type);
		}
		
		public Boolean lessThan(Atomic o) {
			if (type == Type.PARAM && o.type == Type.PARAM && this.value.equals(o.value)) return Boolean.FALSE;
			if (type == Type.PARAM || o.type == Type.PARAM) return null;
			return compareTo(o) < 0;
			
		}

		public Boolean lessThanOrEqual(Atomic o) {
			if (type == Type.PARAM && o.type == Type.PARAM && this.value.equals(o.value)) return Boolean.TRUE;
			if (type == Type.PARAM || o.type == Type.PARAM) return null;
			return compareTo(o) <= 0;
		}

		public Boolean greaterThan(Atomic o) {
			if (type == Type.PARAM && o.type == Type.PARAM && this.value.equals(o.value)) return Boolean.FALSE;
			if (type == Type.PARAM || o.type == Type.PARAM) return null;
			return compareTo(o) > 0;			
		}
		
		public Boolean greaterThanOrEqual(Atomic o) {
			if (type == Type.PARAM && o.type == Type.PARAM && this.value.equals(o.value)) return Boolean.TRUE;
			if (type == Type.PARAM || o.type == Type.PARAM) return null;
			return compareTo(o) >= 0;
		}

		public Boolean maybeEquals(Value o) {
			if (type == o.type || type == Type.PARAM || o.type == Type.PARAM) {
				if  (value.equals(((Atomic)o).value)) return Boolean.TRUE;
				if (type == Type.PARAM || o.type == Type.PARAM) return null;
				return Boolean.FALSE;
			}
			throw new IllegalArgumentException("cannot compare type " + type + " with " + o.type);
		}
		
		public int compareTo(Atomic o) {
			if (type == o.type) return value.compareTo(o.value);
			throw new IllegalArgumentException("cannot compare type " + type + " with " + o.type);
		}
		
		public String toString() {
			switch (type) {
			case STRING: 	return "'" + value + "'";
			case NUMBER: 	return value.toString();
			case PARAM:		return "$" + value;
			case BOOLEAN:	return (Boolean)value ? "true" : "false";
			default:		throw new IllegalArgumentException("Unhandled type: " + type);
			}	
		}
		
		/** Convert value to a number */
		public BigDecimal toNumber() {
			switch (type) {
			case STRING: 	return new BigDecimal((String)value);
			case NUMBER: 	return (BigDecimal)value;
			case BOOLEAN:	return (Boolean)value ? BigDecimal.ONE : BigDecimal.ZERO;
			default:		throw new IllegalArgumentException("Unhandled type: " + type);
			}	
		}
		
		public JsonValue toJSON() {
			switch (type) {
			case STRING: 	return Json.createValue((String)value);
			case NUMBER: 	return Json.createValue((BigDecimal)value);
			case PARAM: 	return Json.createObjectBuilder().add("$", value.toString()).build();
			case BOOLEAN:	return (Boolean)value ? JsonValue.TRUE : JsonValue.FALSE;
			default:		throw new IllegalArgumentException("Unhandled type: " + type);
			}
		}
		
		public static Atomic from(JsonValue obj) {
			if (obj == JsonValue.TRUE) return TRUE;
			if (obj == JsonValue.FALSE) return FALSE;
			if (obj instanceof JsonNumber) {
				return from((JsonNumber)obj);
			} else if (obj instanceof JsonString) {
				return from((JsonString)obj);
			} else if (obj instanceof JsonObject) {
				JsonObject asObj = (JsonObject)obj;
				if (Param.isParam(asObj)) {
					return from(Param.from(asObj));
				} 
			}
			throw new IllegalArgumentException("json " + obj + " not an atomic value");
		}
	}
	
	public static class MapValue extends Value {
		
		private final Map<String,?> map; 
		private final Function<Object,Value> makeValue; 
		
		private <T> MapValue(Map<String,T> map, Function<T,Value> makeValue) {
			super(Type.MAP);
			this.map = map;
			this.makeValue = (Function<Object,Value>)makeValue;
		}
		
		/** Get the set of valid property names in this value.
		 * 
		 * @return a set of property names, empty if this value is not of type MAP.
		 */
		public Set<String> propertySet() {
			return map.keySet();
		}
		
		/** Get a property from this map 
		 * 
		 * @param key property name to get
		 * @return the value of the property
		 * @throws IllegalArgumentException if property does not exist
		 */
		public Value getProperty(String key) {
			if (!propertySet().contains(key)) throw new IllegalArgumentException("No such property " + key);
			return (Value)makeValue.apply(map.get(key));
		}
		
		public boolean hasProperty(String key) {
			return propertySet().contains(key);
		}
		
		private static <T,U> Boolean compareEntries(Function<Object,Value> makeValue1, Function<Object,Value> makeValue2, Map.Entry<String, ?> entry1, Map.Entry<String, ?> entry2) {
			if (entry1.getKey().equals(entry2.getKey())) {
				return makeValue1.apply(entry1.getValue()).maybeEquals(makeValue2.apply(entry2.getValue()));
			} else {
				return Boolean.FALSE;
			}
		}
		
		public Boolean maybeEquals(MapValue other) {
			return Tristate.compareCollections(
					map.entrySet(), 
					other.map.entrySet(), 
					(e1, e2) -> compareEntries(makeValue, other.makeValue, e1, e2)
				);
		}
		
		public Boolean maybeEquals(Value other) {
			if (other.type == Type.MAP)
				return maybeEquals((MapValue)other);
			else
				return false;
		}
		
		public String toString() {
			return "{" + map.keySet()
					.stream()
					.map(key -> key + ": " + getProperty(key))
					.collect(Collectors.joining(",")) + "}";
		}
		
		public JsonValue toJSON() {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			for (String key : propertySet()) builder.add(key, getProperty(key).toJSON());
			return builder.build();
		}
		
		public static MapValue from(JsonObject o) {
			return new MapValue(o, (JsonValue item) -> Value.from(item));
		}
		
		public static MapValue fromJson(String json) {
			return from(JsonUtil.parseObject(json));
		}
	}
	
	public static class ArrayValue extends Value {
		private final List<?> data; 
		private final Function<Object,Value> makeValue; 
		
		private <T> ArrayValue(List<T> data, Function<T,Value> makeValue) {
			super(Type.ARRAY);
			this.data = data;
			this.makeValue = (Function<Object,Value>)makeValue;
		}

		public static ArrayValue from(JsonArray value) {
			return new ArrayValue(value, (JsonValue item) -> from(item));
		}
		
		/** Get the size of the array
		 */
		public int size() {
			return data.size();
		}
		
		/** Get an item from this array 
		 * 
		 * @param index property name to get
		 * @return the value of the property
		 */
		public Value getElement(int index) {
			return makeValue.apply(data.get(index));
		}
		
		public Stream<Value> stream() {
			return data.stream().map(makeValue);
		}

		private static Boolean compareValues(Function<Object,Value> makeValue1, Function<Object,Value> makeValue2, Object value1, Object value2) {
			return makeValue1.apply(value1).maybeEquals(makeValue2.apply(value2));
		}
		
		public Boolean maybeEquals(ArrayValue other) {
			return Tristate.compareCollections(data, other.data, (e1,e2)->compareValues(makeValue, other.makeValue, e1, e2));
		}

		@Override
		public Boolean maybeEquals(Value other) {
			if (other.type == Type.ARRAY) {
				return maybeEquals((ArrayValue)other);
			} else {
				return false;
			}
		}
		
		public String toString() {
			return data.stream()
					.map(item->from(item).toString())
					.collect(Collectors.joining(","));
		}

		public JsonValue toJSON() {
			JsonArrayBuilder builder = Json.createArrayBuilder();
			for (int i = 0; i < size(); i++) builder.add(getElement(i).toJSON());
			return builder.build();
		}
	}
	
	public static Atomic TRUE = new Atomic(Type.BOOLEAN, true);
	public static Atomic FALSE = new Atomic(Type.BOOLEAN, false);
	
	public static Atomic from(JsonNumber value) {
		return new Atomic(Type.NUMBER, value.bigDecimalValue());
		
	}
	
	public static Atomic from(JsonString value) {
		return new Atomic(Type.STRING, value.getString());
	}
	
	/** Convert from a long value */
	public static Atomic from(BigDecimal value) {
		return new Atomic(Type.NUMBER, value);
	}
	
	/** Convert from a long value */
	public static Atomic from(long value) {
		return new Atomic(Type.NUMBER, BigDecimal.valueOf(value));
	}

	/** Convert from a double value */
	public static Atomic from(double value) {
		return new Atomic(Type.NUMBER, BigDecimal.valueOf(value));
	}

	/** Convert from a double value */
	public static Atomic from(boolean value) {
		return new Atomic(Type.BOOLEAN, value);
	}

	
	/** Convert from a JSON value 
	 * 
	 * JsonNumber, JsonString, and JsonArray objects map to values of type NUMBER, STRING, and ARRAY.
	 * JsonObject objects map to values of type MAP; getProperty on the returnd value will return the
	 * related property of the JSON object.
	 */
	public static Value from(JsonValue obj) {
		if (obj == JsonValue.TRUE) return TRUE;
		if (obj == JsonValue.FALSE) return FALSE;
		if (obj instanceof JsonNumber) {
			return from((JsonNumber)obj);
		} else if (obj instanceof JsonString) {
			return from((JsonString)obj);
		} else if (obj instanceof JsonObject) {
			JsonObject asObj = (JsonObject)obj;
			if (Param.isParam(asObj)) {
				return from(Param.from(asObj));
			} else {
				return MapValue.from(asObj);
			}
		} else if (obj instanceof JsonArray) {
			return ArrayValue.from((JsonArray)obj);
		}
		throw new IllegalArgumentException("Not a value type:" + obj);
	}
	
	
	/** Convert from a string value */
	public static Atomic from(String value) {
		return new Atomic(Type.STRING, value);
	}
	
	/** Convert from a string value */
	public static Atomic from(Param value) {
		return new Atomic(Type.PARAM, value);
	}
		
	/** Convert from a list of values
	 * 
	 *  A shallow copy is made of the value list. The list may be of JsonValue objects or any
	 *  POJO supported by from(value). Care should be taken if the list contains mutable java
	 *  objects. 
	 */
	public static ArrayValue from(List<Object> values) {
		return new ArrayValue(new ArrayList<Object>(values), Value::from);
	}
	
	/** Convert from a list of values 
	 *
	 * The values array is not copied.
	 */
	public static ArrayValue from(Object... values) {
		return new ArrayValue(Arrays.asList(values), Value::from);
	}

	/** Convert from a POJO 
	 *
	 * Long, Double, and String, List, and JsonValue objects will be converted using the appropriate
	 * static conversion ('from') function. Other java objects will be treated as Beans; a Value of type
	 * MAP will be created such that Value.getProperty returns the appropriate property of the bean.
	 */
	public static Value from(Object value) {
		if (value instanceof Boolean) return from((Boolean)value);
		if (value instanceof List) return from((List<?>)value);
		if (value instanceof Double) return from((Double)value);
		if (value instanceof Long) return from((Long)value);
		if (value instanceof String) return from((String)value);
		if (value instanceof Param) return from((Param)value);
		if (value instanceof JsonValue) return Value.from((JsonValue) value);
		return new MapValue((Map)new BeanMap(value), Value::from);
	}
	
	/** Convenience method equivalent to from(JsonUtil.parseValue(value) */
	public static Value fromJson(String value) {
		return from(JsonUtil.parseValue(value));		
	}
	
	public static boolean isAtomicValue(JsonValue obj) {
		return 		obj == JsonValue.TRUE
				||  obj == JsonValue.FALSE
				||  (obj instanceof JsonNumber) 
				|| 	(obj instanceof JsonString)
				||  (obj instanceof JsonObject && ((JsonObject)obj).containsKey("$"));
	}
	
	public static Atomic param(String name) {
		return new Atomic(Type.PARAM, Param.from(name));
	}
}

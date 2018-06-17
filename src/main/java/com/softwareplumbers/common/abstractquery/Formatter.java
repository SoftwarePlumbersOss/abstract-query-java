package com.softwareplumbers.common.abstractquery;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/** Format a Query
 * 
 * Used to walk the logical structure of a query, building a representation of
 * type T using context data of type U.
 * 
 * @author Jonathan Essex
 *
 * @param <T> Type of formatted representation (typically, but not always, a String)
 * @param <U> Type of context information required
 */
public interface Formatter<T> {
	
	/** Create a representation of a constraint on a dimension */
	T operExpr(String operator, Value value);
	/** Create a representation of an intersection of constraints */
	T andExpr(Stream<T> expressions);
	/** Create a representation of a union of constraints */
	T orExpr(Stream<T> expressions);
	/** Create a representation of an operation over subexpressions */
	T subExpr(String operator, Stream<T> items);
	/** Create a formatter in context of parent */
	Formatter<T> in(String dimension);
	
	/** Get the default query formatter
	*/
	public static class DefaultFormat implements Formatter<String> {
		
		private DefaultFormat parent;
		private String dimension;
		
		public DefaultFormat(DefaultFormat parent, String dimension) {
			this.parent = parent;
			this.dimension = dimension;
		}

		String printDimension() {
			if (parent.dimension == null) return dimension;
			return parent.printDimension() + "." + dimension;
		}
		
		String printValue(Value value) {
			return value.type == Value.Type.STRING ? "\"" + value.toString() + "\"" : value.toString();
		}

    	public String andExpr(Stream<String> ands) { 
    		return ands.collect(Collectors.joining(" and ")); 
    	}
    	
    	public String orExpr(Stream<String> ors) { 
    		return "(" + ors.collect(Collectors.joining(" or ")) + ")"; 
    	}
    	
    	public String operExpr(String operator, Value value) {
    			// null dimension implies that we are in a 'has' clause where the dimension is attached to the
    			// outer 'has' operator 
    			if (operator.equals("match"))
    				return value.toString();
    			if (operator.equals("has"))
    				return printDimension() + " has(" + value + ")";
    			//if (dimension === null) return '$self' + operator + printValue(value) 

    			return printDimension() + operator + printValue(value) ;
    	}
    	
    	public String subExpr(String operator, Stream<String> subs) {
    		return "has (" +  andExpr(subs) + ")";
    	}
    	
    	public Formatter<String> in(String dimension) {
    		return new DefaultFormat(this, dimension);
    	}
	};

	/** Get the default query formatter
	*/
	public static class JsonFormat implements Formatter<JsonValue> {
		
		private String dimension;
		
		public JsonFormat(String dimension) {
			this.dimension = dimension;
		}
				
    	public JsonValue andExpr(Stream<JsonValue> ands) { 
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		ands.forEach(value->array.add(value));
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		object.add("$and", array);
    		return object.build();
    	}
    	
    	public JsonValue orExpr(Stream<JsonValue> ors) { 
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		ors.forEach(value->array.add(value));
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		object.add("$or", array);
    		return object.build();
    	}
    	
    	public JsonValue operExpr(String operator, Value value) {
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		object.add(dimension, Json.createObjectBuilder().add(operator, value.toJSON()));
    		return object.build();
    	}
    	
    	public JsonValue subExpr(String operator, Stream<JsonValue> subs) {
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		subs.forEach(value->
    			array.add(Json.createObjectBuilder().add(operator, value)));
    		return Json.createObjectBuilder().add("operator", array).build();
    	}
    	
    	public Formatter<JsonValue> in(String dimension) {
    		return new JsonFormat(dimension);
    	}
	};

	/** Default formatter creates a compact string expression */
	public Formatter<String> DEFAULT = new DefaultFormat(null, null);
	/** Default JSON creates a JSON representation */
	public Formatter<JsonValue> JSON = new JsonFormat(null);
}

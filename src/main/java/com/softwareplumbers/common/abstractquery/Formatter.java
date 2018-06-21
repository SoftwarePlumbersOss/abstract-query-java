package com.softwareplumbers.common.abstractquery;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.Value.Type;

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
	T andExpr(Value.Type type, Stream<T> expressions);
	/** Create a representation of a union of constraints */
	T orExpr(Value.Type type, Stream<T> expressions);
	/** Create a representation of an operation over subexpressions */
	T subExpr(String operator, T sub);
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
			if (parent == null) return "$self";
			if (parent.dimension == null) return dimension;
			return parent.printDimension() + "." + dimension;
		}
		
		String printValue(Value value) {
			return value.type == Value.Type.STRING ? "\"" + value.toString() + "\"" : value.toString();
		}

    	public String andExpr(Value.Type type, Stream<String> ands) { 
    		return ands.collect(Collectors.joining(" and ")); 
    	}
    	
    	public String orExpr(Value.Type type, Stream<String> ors) { 
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
    	
    	public String subExpr(String operator, String sub) {
    		return "has (" +  sub + ")";
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
				
    	public JsonValue andExpr(Value.Type type, Stream<JsonValue> ands) { 
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		ands.forEach(value->array.add(value));
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		object.add("$and", array);
    		return object.build();
    	}
    	
    	public JsonValue orExpr(Value.Type type, Stream<JsonValue> ors) { 
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
    	
    	public JsonValue subExpr(String operator, JsonValue sub) {
    		return Json.createObjectBuilder().add(operator, sub).build();
    	}
    	
    	public Formatter<JsonValue> in(String dimension) {
    		return new JsonFormat(dimension);
    	}
	};
	
	public interface Node extends List<Node> {
//		public <T> T toExpression(Formatter<T> format);
	}
	
	public class Operator extends AbstractList<Node> implements Node {
		@Override public Node get(int index) { return null; }
		@Override public int size() { return 0; };
		public final String operator;
		public final Value value;
		public Operator(String operator, Value value) { this.operator = operator; this.value = value; }
	}
	
	public class And extends ArrayList<Node> implements Node { }
	public class Or extends ArrayList<Node> implements Node { }
	
	public class Sub extends AbstractList<Node> implements Node {
		@Override public Node get(int index) { return subexpression; }
		@Override public int size() { return 1; }
		public final String operator;
		public final Node subexpression;
		public Sub(String operator, Node subexpression) { this.operator = operator; this.subexpression = subexpression; }
		
	}
	

	public class TreeFormatter implements Formatter<Node> {

		@Override
		public Node operExpr(String operator, Value value) { 
			return new Operator(operator, value);
		}

		@Override
		public Node andExpr(Value.Type type, Stream<Node> expressions) {
			return expressions.collect(And::new, And::add, And::addAll);
		}

		@Override
		public Node orExpr(Value.Type type, Stream<Node> expressions) {
			return expressions.collect(Or::new, Or::add, Or::addAll);
		}

		@Override
		public Node subExpr(String operator, Node sub) {
			return new Sub(operator, sub);
		}

		@Override
		public Formatter<Node> in(String dimension) {
			return this;
		}	
	}
	
	public class Builder implements Formatter<AbstractSet<? extends Value, ?>> {

		@Override
		public AbstractSet<? extends Value, ?> operExpr(String operator, Value value) {
			// TODO: should change type of value to AtomicValue?
			return Range.getRange(operator, (Value.Atomic)value);
		}

		@Override
		public AbstractSet<? extends Value, ?> andExpr(Type type, Stream<AbstractSet<? extends Value, ?>> expressions) {
			//if (type == Value.Type.MAP) return Cube.intersect(expressions.map(set -> (Cube)set));
			return null;
		}

		@Override
		public AbstractSet<? extends Value, ?> orExpr(Type type, Stream<AbstractSet<? extends Value, ?>> expressions) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AbstractSet<? extends Value, ?> subExpr(String operator, AbstractSet<? extends Value, ?> sub) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Formatter<AbstractSet<? extends Value, ?>> in(String dimension) {
			// TODO Auto-generated method stub
			return null;
		}

	}
	
	public Formatter<Node> TREE = new TreeFormatter();
	/** Default formatter creates a compact string expression */
	public Formatter<String> DEFAULT = new DefaultFormat(null, null);
	/** Default JSON creates a JSON representation */
	public Formatter<JsonValue> JSON = new JsonFormat(null);
}

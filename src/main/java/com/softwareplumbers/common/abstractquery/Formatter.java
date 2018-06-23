package com.softwareplumbers.common.abstractquery;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
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
	
	public interface CanFormat {
		<T> T toExpression(Formatter<T> format);
	}
	
	/** Create a representation of a constraint on a dimension */
	T operExpr(String operator, Value value);
	/** Create a representation of an intersection of constraints */
	T andExpr(Value.Type type, Stream<T> expressions);
	/** Create a representation of a union of constraints */
	T orExpr(Value.Type type, Stream<T> expressions);
	/** Create a representation of an operation over subexpressions */
	default T betweenExpr(Value.Type type, T lower_bound, T upper_bound) {
		return andExpr(type, Stream.of(lower_bound, upper_bound));
	}
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
			return value.toString();
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
    		if (type == Value.Type.MAP) {
    			JsonObjectBuilder object = Json.createObjectBuilder();
    			ands.forEach(value-> {
    				if (value instanceof JsonObject)
    					object.addAll(Json.createObjectBuilder((JsonObject)value));
    				else
    					throw new RuntimeException("Not excpecting a Cube to have member of type:" + value.getValueType());
    			});
    			return object.build();
    		} else {
    			JsonArrayBuilder array = Json.createArrayBuilder();
    			ands.forEach(value->array.add(value));
    			JsonObjectBuilder object = Json.createObjectBuilder();
    			object.add("$and", array);
    			return object.build();
    		}
    	}
    	
    	public JsonValue orExpr(Value.Type type, Stream<JsonValue> ors) { 
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		ors.forEach(value->array.add(value));
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		object.add("$or", array);
    		return object.build();
    	}
    	    	
    	private static String getFirstProperty(JsonValue value) {
    		return value.asJsonObject().keySet().iterator().next();
    	}
    	
    	public JsonValue betweenExpr(Value.Type type, JsonValue lower_bound, JsonValue upper_bound) {
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		String lbp = getFirstProperty(lower_bound);
    		String ubp = getFirstProperty(upper_bound);
    		if (!lbp.equals(ubp)) { throw new RuntimeException("Bad between"); }
    		JsonObject lb = lower_bound.asJsonObject().getJsonObject(lbp);
    		JsonObject ub = upper_bound.asJsonObject().getJsonObject(ubp);
    		String lbo = getFirstProperty(lb);
    		String ubo = getFirstProperty(ub);
    		array.add(lbo.equals(Range.GreaterThanOrEqual.OPERATOR) ? lb.get(lbo) : lb);
    		array.add(ubo.equals(Range.LessThan.OPERATOR) ? ub.get(ubo) : ub);
    		return Json.createObjectBuilder().add(lbp, array).build();
    	}
    	
    	public JsonValue operExpr(String operator, Value value) {
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		if (operator.equals(Range.Equals.OPERATOR))
    			object.add(dimension,  value.toJSON());
    		else
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
	
	public interface Node extends List<Node>, CanFormat {
	}
	
	public class Operator extends AbstractList<Node> implements Node {
		@Override public Node get(int index) { return null; }
		@Override public int size() { return 0; };
		public final String operator;
		public final Value value;
		public Operator(String operator, Value value) { this.operator = operator; this.value = value; }
		@Override
		public <T> T toExpression(Formatter<T> format) { return format.operExpr(operator, value); }
	}
	
	public class And extends ArrayList<Node> implements Node { 
		public final Value.Type type;
		public And(Value.Type type, Node... items) { super(Arrays.asList(items)); this.type = type; }
		public And(Value.Type type) { this.type = type; }
		public <T> T toExpression(Formatter<T> format) { return format.andExpr(type, stream().map(item->item.toExpression(format))); }
	}
	
	public class Or extends ArrayList<Node> implements Node { 
		public final Value.Type type;
		public Or(Value.Type type, List<? extends Node> items) { super(items); this.type = type; }
		public Or(Value.Type type) { this.type = type; }		
		public <T> T toExpression(Formatter<T> format) { return format.orExpr(type, stream().map(item->item.toExpression(format))); }
	}
	
	public class Sub extends AbstractList<Node> implements Node {
		@Override public Node get(int index) { return subexpression; }
		@Override public int size() { return 1; }
		public final String operator;
		public final Node subexpression;
		public Sub(String operator, Node subexpression) { this.operator = operator; this.subexpression = subexpression; }
		public <T> T toExpression(Formatter<T> format) { return format.subExpr(operator, subexpression.toExpression(format)); }		
	}
	

	public class TreeFormatter implements Formatter<Node> {

		@Override
		public Node operExpr(String operator, Value value) { 
			return new Operator(operator, value);
		}

		@Override
		public Node andExpr(Value.Type type, Stream<Node> expressions) {
			return expressions.collect(()->new And(type), And::add, And::addAll);
		}

		@Override
		public Node orExpr(Value.Type type, Stream<Node> expressions) {
			return expressions.collect(()->new Or(type), Or::add, Or::addAll);
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
	
	public class Factorizer extends TreeFormatter {
		
		private static void triage(List<And> ands, Or atomics, Node node) {
			if (node instanceof And) 		ands.add((And)node);
			else if (node instanceof Or)	((Or)node).stream().forEach(inner->triage(ands, atomics, inner));
			else 							atomics.add(node);			
		}
		
		public Optional<Node> getCommonestFactor(List<And> ands) {
			return ands
					.stream()
					.flatMap(List::stream) // Stream of all possible factors
					.collect(Collectors.groupingBy(node->node, Collectors.counting())) // count number of times each factor appears
					.entrySet()
					.stream()
					.filter(entry -> entry.getValue() > 1) // Ignore any factor that appears only once
					.collect(
						Collectors.maxBy(
							Comparator.comparingLong(entry -> entry.getValue()) // Find the factor that appears the most times
						)
					)
					.map(entry -> entry.getKey());
			
		}
		
		public Optional<Node> factorize(Value.Type type, List<And> ands) {
			
			Optional<Node> factor = getCommonestFactor(ands);
			if (factor.isPresent()) {
				List<And> factorized = new ArrayList<And>();
				Or result = new Or(type);
				for (And and : ands) {
					if (and.remove(factor)) 
						factorized.add(and); 
					else result.add(and);
				}
				Optional<Node> inner = factorize(type, factorized);
				And factored = null;
				if (inner.isPresent()) {
					// if inner is an 'And' we have no remainder
					if (inner.get() instanceof And) {
						factored = (And)inner.get();
						factored.add(factor.get());
					} else {
						factored = new And(type, factor.get(), inner.get());
					}
				} else {
					factored = new And(type, factor.get(), new Or(type, factorized));
				}
				if (result.isEmpty()) 
					return Optional.of(factored);
				else {
					result.add(factored);
					return Optional.of(result);
				}			
			} else {
				return Optional.empty();
			}
		}
		
		@Override
		public Node orExpr(Type type, Stream<Node> expressions) {
			final List<And> ands = new ArrayList<And>();
			final Or atomics = new Or(type);
			expressions.forEach(node -> triage(ands,atomics,node));
			Optional<Node> factorized = factorize(type, ands);
			if (factorized.isPresent()) {
				Node node = factorized.get();
				if (node instanceof Or) 
					atomics.addAll(node);
				else
					atomics.add(node);
			} else {
				atomics.addAll(ands);
			};
			return atomics;
		}
	}
	
	public Formatter<Node> SIMPLIFY = new Factorizer();	
	public Formatter<Node> TREE = new TreeFormatter();
	/** Default formatter creates a compact string expression */
	public Formatter<String> DEFAULT = new DefaultFormat(null, null);
	/** Default JSON creates a JSON representation */
	public Formatter<JsonValue> JSON = new JsonFormat(null);
}

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

import com.softwareplumbers.common.abstractquery.Value;


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
	
	public class Context {
		
		public enum Type {
			FIELD,
			OBJECT,
			ARRAY,
			ROOT
		}
		
		public final Context parent;
		public final String dimension;
		public final Type type;
		
		public Context(Context parent, Type type, String dimension) {
			this.type = type;
			this.dimension = dimension;
			this.parent = parent;
		}
		
		public Context in(String dimension) {
			return new Context(this, Type.FIELD, dimension);
		}

		public Context setType(Type type) {
			return new Context(parent, type, dimension);
		}
		
		protected static boolean eq(Object a, Object b) { return a == b || (a != null && b!= null && a.equals(b)); }	
		
		public boolean equals(Context other) {
			return eq(type, other.type) && eq(dimension, other.dimension) && eq(parent, other.parent);
		}
		
		public boolean equals(Object other) {
			return other instanceof Context && equals((Context)other);
		}

		public static final Context ROOT = new Context(null, Type.ROOT, null);
	}
	
	public interface CanFormat {
		<T> T toExpression(Formatter<T> format, Context ctx);
		default <T> T toExpression(Formatter<T> format) { return toExpression(format, Context.ROOT); }
	}
	
	/** Create a representation of a constraint on a dimension */
	T operExpr(Context context, String operator, Value value);
	/** Create a representation of an intersection of constraints */
	T andExpr(Context context, Value.Type type, Stream<T> expressions);
	/** Create a representation of a union of constraints */
	T orExpr(Context context, Value.Type type, Stream<T> expressions);
	/** Create a representation of an operation over subexpressions */
	default T betweenExpr(Context context, Value.Type type, T lower_bound, T upper_bound) {
		return andExpr(context, type, Stream.of(lower_bound, upper_bound));
	}
	/** Create a representation of an operation over subexpressions */
	T subExpr(Context context, String operator, T sub);
	/** Create a formatter in context of parent */
	
	/** Get the default query formatter
	*/
	public static class DefaultFormat implements Formatter<String> {
		
		static String printDimension(Context context) {
			if (context.parent == null) return "$self";
			if (context.parent.dimension == null) return context.dimension;
			return printDimension(context.parent) + "." + context.dimension;
		}
		
		String printValue(Value value) {
			return value.toString();
		}

    	public String andExpr(Context context, Value.Type type, Stream<String> ands) { 
    		return ands.collect(Collectors.joining(" and ")); 
    	}
    	
    	public String orExpr(Context context, Value.Type type, Stream<String> ors) { 
    		return "(" + ors.collect(Collectors.joining(" or ")) + ")"; 
    	}
    	
    	public String operExpr(Context context, String operator, Value value) {
    			// null dimension implies that we are in a 'has' clause where the dimension is attached to the
    			// outer 'has' operator 
    			if (operator.equals("match"))
    				return value.toString();
    			if (operator.equals("has"))
    				return printDimension(context) + " has(" + value + ")";
    			//if (dimension === null) return '$self' + operator + printValue(value) 

    			return printDimension(context) + operator + printValue(value) ;
    	}
    	
    	public String subExpr(Context context, String operator, String sub) {
    		return "has (" +  sub + ")";
    	}
	};

	/** Get the default query formatter
	*/
	public static class JsonFormat implements Formatter<JsonValue> {
				
    	public JsonValue andExpr(Context context, Value.Type type, Stream<JsonValue> ands) {
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
    	
    	public JsonValue orExpr(Context context, Value.Type type, Stream<JsonValue> ors) { 
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		ors.forEach(value->array.add(value));
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		object.add("$or", array);
    		return object.build();
    	}
    	    	
    	private static String getFirstProperty(JsonValue value) {
    		return value.asJsonObject().keySet().iterator().next();
    	}
    	
    	public JsonValue betweenExpr(Context context, Value.Type type, JsonValue lower_bound, JsonValue upper_bound) {
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
    	
    	public JsonValue operExpr(Context context, String operator, Value value) {
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		if (operator.equals(Range.Equals.OPERATOR))
    			object.add(context.dimension,  value.toJSON());
    		else
    			object.add(context.dimension, Json.createObjectBuilder().add(operator, value.toJSON()));
    		return object.build();
    	}
    	
    	public JsonValue subExpr(Context context, String operator, JsonValue sub) {
    		JsonObjectBuilder hasExpr = Json.createObjectBuilder().add(operator, sub);
    		return Json.createObjectBuilder().add(context.dimension,hasExpr).build();
    	}
	};
	


	
	public interface Node extends List<Node>, CanFormat {
		Context getContext();
	}
	
	public class Operator extends AbstractList<Node> implements Node {
		
		protected static boolean eq(Object a, Object b) { return a == b || (a != null && b!= null && a.equals(b)); }	
		public final Context context;
		@Override public Node get(int index) { return null; }
		@Override public int size() { return 0; };
		public final String operator;
		public final Value value;
		public Operator(Context context, String operator, Value value) { this.context = context; this.operator = operator; this.value = value; }
		@Override
		public <T> T toExpression(Formatter<T> format, Context ctx) { return format.operExpr(context, operator, value); }
		public String toString() { return toExpression(DEFAULT, context); }
		public boolean equals(Operator other) { return eq(context,other.context) && eq(operator, other.operator) && eq(value, other.value); }
		public boolean equals(Object other) { return other instanceof Operator && equals((Operator)other); }
		public Context getContext() { return context; }
	}
	
	public class And extends ArrayList<Node> implements Node { 
		public final Context context;
		public final Value.Type type;
		public And(Value.Type type, Context context, Node... items) { super(Arrays.asList(items)); this.type = type; this.context = context;  }
		public And(Value.Type type, Context context) { this.type = type; this.context = context; }
		public <T> T toExpression(Formatter<T> format, Context ctx) { return format.andExpr(context, type, stream().map(item->item.toExpression(format,context))); }
		public String toString() { return toExpression(DEFAULT, context); }
		public Context getContext() { return context; }
	}
	
	public class Between extends ArrayList<Node> implements Node { 
		public final Context context;
		public final Value.Type type;
		public Between(Value.Type type, Context context, Node lower, Node upper) { super(Arrays.asList(lower,upper)); this.type = type; this.context = context;  }
		public Between(Value.Type type, Context context) { this.type = type; this.context = context; }
		public <T> T toExpression(Formatter<T> format, Context ctx) { return format.betweenExpr(context, type, get(0).toExpression(format,context), get(1).toExpression(format,context)); }
		public String toString() { return toExpression(DEFAULT, context); }
		public Context getContext() { return context; }
	}
	
		
	public class Or extends ArrayList<Node> implements Node { 
		public final Context context;
		public final Value.Type type;
		public Or(Value.Type type, Context context, List<? extends Node> items) { super(items); this.context = context; this.type = type; }
		public Or(Value.Type type, Context context) { this.type = type; this.context = context; }		
		public <T> T toExpression(Formatter<T> format, Context ctx) { return format.orExpr(context, type, stream().map(item->item.toExpression(format, context))); }
		public String toString() { return toExpression(DEFAULT, context); }
		public Context getContext() { return context; }
	}
	
	public class Sub extends AbstractList<Node> implements Node {
		public final Context context;
		protected static boolean eq(Object a, Object b) { return a == b || (a != null && b!= null && a.equals(b)); }	
		@Override public Node get(int index) { return subexpression; }
		@Override public int size() { return 1; }
		public final String operator;
		public final Node subexpression;
		public Sub(Context context, String operator, Node subexpression) { this.context = context; this.operator = operator; this.subexpression = subexpression; }
		public <T> T toExpression(Formatter<T> format, Context ctx) { return format.subExpr(context, operator, subexpression.toExpression(format, context)); }		
		public String toString() { return toExpression(DEFAULT, context); }
		public boolean equals(Sub other) { return eq(context,other.context) && eq(operator, other.operator) && eq(subexpression, other.subexpression); }
		public boolean equals(Object other) { return other instanceof Sub && equals((Sub)other); }
		public Context getContext() { return context; }
	}
	

	

	public class TreeFormatter implements Formatter<Node> {
		
		@Override
		public Node operExpr(Context context, String operator, Value value) { 
			return new Operator(context, operator, value);
		}

		@Override
		public Node andExpr(Context context, Value.Type type, Stream<Node> expressions) {
			return expressions.collect(()->new And(type, context), And::add, And::addAll);
		}

		@Override
		public Node orExpr(Context context, Value.Type type, Stream<Node> expressions) {
			return expressions.collect(()->new Or(type, context), Or::add, Or::addAll);
		}

		@Override
		public Node subExpr(Context context, String operator, Node sub) {
			return new Sub(context, operator, sub);
		}
		
		public Node betweenExpr(Context context, Value.Type type, Node lower, Node upper) {
			return new Between(type, context, lower, upper);
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
					.sorted(Comparator.comparing(entry->entry.getKey().getContext().dimension)) // sort so we factorize in a stable way
					.collect(
						Collectors.maxBy(
							Comparator.comparingLong(entry -> entry.getValue()) // Find the factor that appears the most times
						)
					)
					.map(entry -> entry.getKey());
			
		}
		
		public Optional<Node> factorize(Context context, Value.Type type, List<And> ands) {
			
			Optional<Node> factor = getCommonestFactor(ands);
			if (factor.isPresent()) {
				List<And> factorized = new ArrayList<And>();
				Or result = new Or(type, context);
				for (And and : ands) {
					if (and.remove(factor.get())) 
						factorized.add(and); 
					else result.add(and);
				}
				Optional<Node> inner = factorize(context, type, factorized);
				And factored = null;
				if (inner.isPresent()) {
					// if inner is an 'And' we have no remainder
					if (inner.get() instanceof And) {
						factored = (And)inner.get();
						factored.add(factor.get());
					} else {
						factored = new And(type, context, factor.get(), inner.get());
					}
				} else {
					factored = new And(type, context, factor.get(), new Or(type, context, factorized));
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
		public Node orExpr(Context context, Value.Type type, Stream<Node> expressions) {
			final List<And> ands = new ArrayList<And>();
			final Or atomics = new Or(type, context);
			expressions.forEach(node -> triage(ands,atomics,node));
			Optional<Node> factorized = factorize(context, type, ands);
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
	public Formatter<String> DEFAULT = new DefaultFormat();
	/** Default JSON creates a JSON representation */
	public Formatter<JsonValue> JSON = new JsonFormat();
}

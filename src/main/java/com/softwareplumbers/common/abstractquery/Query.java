package com.softwareplumbers.common.abstractquery;

import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonValue;

import visitor.Context;
import com.softwareplumbers.common.QualifiedName;
import com.softwareplumbers.common.jsonview.JsonViewFactory;
import java.io.StringReader;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.json.Json;

import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;
import visitor.Visitor;
import visitor.Visitors;

/** A constraint maps each dimension in an abstract space to a range.
 *
 */
public interface Query extends AbstractSet<JsonObject, Query> {
	
	public static final Query UNBOUNDED = new Unbounded();
	public static final Query EMPTY = new Empty();
	public static final QueryFactory FACTORY = new QueryFactory();
    
    public class ComposablePredicate implements Predicate<JsonObject> {
        
        Predicate<JsonObject> base;
        
        public boolean test(JsonObject obj) { return base.test(obj); }
        
        public <T> Predicate<T> compose(Function<T,JsonObject> map) {
            return item->test(map.apply(item));
        }
        
        public ComposablePredicate(Predicate<JsonObject> base) {
            this.base = base;
        }
    }
	
	public AbstractSet<?, ?> getConstraint(String dimension);
	public Set<String> getConstraints();
	
	default Query bind(String json) {
		return bind(Json.createParser(new StringReader(json)).getObject());
	}
	
	default Query intersect(String json) {
		return intersect(Query.fromJson(json));
	}

	default Query union(String json) {
		return union(Query.fromJson(json));
	}
	
    default ComposablePredicate predicate() {
        return new ComposablePredicate(this::containsItem);
    }

	default Factory<JsonObject, Query> getFactory() {
		return FACTORY;
	}
		
	Query maybeUnion(Query other);

	public static class Impl implements Query {

	private Map<String, AbstractSet<? extends JsonValue, ?>> constraints;
	
	public Query maybeUnion(Query other) {
		if (this.contains(other)) return this;
		if (other.contains(this)) return other;
		return null;
	}

	/** Create a new constraint
	 * 
	 * @param constraints A map from dimension name to a range of values.
	 */
	public Impl(Map<String, AbstractSet<? extends JsonValue,?>> constraints) {
		this.constraints = constraints;
	}
	
	/** Create a new constraint as a copy of an old constraint.
	 * 
	 * @param to_copy A constraint to copy
	 */
	public Impl(Impl to_copy) {
		this.constraints = new TreeMap<String, AbstractSet<? extends JsonValue,?>>(to_copy.constraints);
	}
	
	/** Create a 'one dimensional' constraint
	 * 
	 * @param dimension name of a dimension
	 * @param range permitted range for that dimension
	 */
	public Impl(String dimension, AbstractSet<? extends JsonValue,?> range) {
		this.constraints = new TreeMap<String, AbstractSet<? extends JsonValue,?>>();
		this.constraints.put(dimension, range);
	}
	
	public Impl() {
		this.constraints = new TreeMap<String,AbstractSet<? extends JsonValue,?>>();
	}

	/** Get the constraint for a given dimension
	 * 
	 * A constraint requires that values for a given dimension reside within
	 * a given range for this constraint.
	 * 
	 * @param dimension the name of the dimension
	 * @return the range of values permitted for the given dimension
	 */
	public AbstractSet<?,?> getConstraint(String dimension) {
		return constraints.get(dimension);
	}
	
	public Set<String> getConstraints() {
		return constraints.keySet();
	}
	
	/** Get the set of dimension names for this constraint.
	 * 
	 * @return the set of dimension names valid for this constraint
	 */
	public Set<String> getDimensions() {
		return constraints.keySet();
	}

	/** Check that two Cubes are equal
	 * 
	 * Cubes are considered equal if there is no possible value which meets the
	 * constraints of one constraint ('is contained by the constraint') and does not meet the
	 * constraints of the other.
	 * 
	 * @param other Other constraint to check
	 * @return true if constraints are equal
	 */
	public boolean equals(Query other) {
		return this.maybeEquals(other) ==  Boolean.TRUE;
	}
	
	/** Check whether this constraint is equal to some other object
	 * 
	 * If the other object is not a Cube, returns false. Otherwise
	 * equivalent to equals(Cube) above.
	 * 
	 * @param other Other constraint to check
	 * @return true if other is Cube and equal to this constraint
	 */
	public boolean equals(Object other) {
		return other instanceof Query && equals((Query)other);
	}
	
	private <T extends JsonValue,U extends AbstractSet<T,U>> Boolean containsConstraint(String dimension, Query other) {
		// We should do some type checking here.
		U thisConstraint = (U)getConstraint(dimension);
		U otherConstraint = (U)other.getConstraint(dimension);
		return thisConstraint.contains(otherConstraint);
	}
	
	/** Check whether this constraint contains some other constraint
	 * 
	 * A constraint contains another constraint if there is no possible value which
	 * meets the constraints of the contained constraint which does not also meet
	 * the constraints of the containing constraint.
	 * 
	 * Ranges which define constraints may be parameterized; in which case it may not be possible
	 * to determine if one constraint is contained by another. In this case, null is returned.
	 * 
	 * @param other Cube to compare
	 * @return true if this constraint contains the other constraint, false if not, null if we cannot tell.
	 */
	public Boolean contains(Query other) {
		return Tristate.every(constraints.keySet(), constraint-> containsConstraint(constraint, other));
	}

	private <T extends JsonValue,U extends AbstractSet<T,U>> Boolean containsItem(String dimension, JsonObject item) {
		// TODO: We should do some type checking here. Notionally that element.type equals constraint.type
		U constraint = (U)getConstraint(dimension);
		T element = (T)(item.containsKey(dimension) ? item.get(dimension) : null);
		return constraint.containsItem(element);
	}
	
	/** Check whether this constraint contains some value
	 * 
	 * A constraint contains a value if the value meets constraints on every dimension of
	 * this constraint.
	 * 
	 * Ranges which define constraints may be parameterized; in which case it may not be possible
	 * to determine if an item is contained by a constraint. In this case, null is returned.
	 * 
	 * @param item Item to compare
	 * @return true if this constraint contains the item, false if not, null if we cannot tell.
	 */
	public Boolean containsItem(JsonObject item) {
		return Tristate.every(constraints.keySet(),
			entry -> containsItem(entry, item));		
	}
    	
	public <T extends JsonValue,U extends AbstractSet<T,U>> Boolean intersects(String dimension, Query other) {
		U constraint1 = (U)getConstraint(dimension);
		U constraint2 = (U)other.getConstraint(dimension);
		if (constraint1 == null || constraint2 == null) return Boolean.TRUE;
		return constraint1.intersects(constraint2);
	}
	
	public Boolean intersects(Query other) {
		return Tristate.every(constraints.keySet(), dimension->intersects(dimension, other));
	}
	
	private <T extends JsonValue,U extends AbstractSet<T,U>> U intersect(String dimension, Query other) {
		// TODO: We should do some type checking here. Notionally that constraint1.type equals constraint2.type
		U constraint1 = (U)getConstraint(dimension);
		U constraint2 = (U)other.getConstraint(dimension);
		if (constraint1 == null) return constraint2;
		if (constraint2 == null) return constraint1;
		return constraint1.intersect(constraint2);
	}

	/** Intersect this constraint with some other
	 * 
	 * Create a new constraint which contains all items which are contained by both constraints.
	 * 
	 * @param other other constraint
	 * @return A new constraint which is the logical intersection of this constraint and some other.
	 */
	public Query intersect(Query other) {

		Map<String, AbstractSet<? extends JsonValue, ?>> result = new TreeMap<String, AbstractSet<? extends JsonValue, ?>>();
		result.putAll(constraints);
		
		for (String dimension : other.getConstraints()) {
			AbstractSet<? extends JsonValue, ?> intersection = intersect(dimension, other);
			if (intersection.isEmpty()) return EMPTY;
				result.put(dimension, intersection);
		}

		return new Impl(result);
	}
	
	// TODO: I don't think this is quite right.
	private void removeConstraint(String dimension, AbstractSet<?,?> constraint) {
		AbstractSet<?,?> this_constraint = constraints.get(dimension);
		if (this_constraint != null && this_constraint.equals(constraint)) {
			constraints.remove(dimension);
		} else {
			throw new IllegalArgumentException( "{ " + dimension + ":" + constraint + "} is not a in " + this );
		}
	}

	/** Remove constraints from a constraint.
	 * 
	 * Used for factoring query expressions. At this point, rather experimental.
	 * 
	 */
	public Query removeConstraints(Query to_remove) {
		Impl result = new Impl(this);
		for (String constraint : to_remove.getConstraints()) 
			result.removeConstraint(constraint, to_remove.getConstraint(constraint));
		return result;
	}

	/** Convert to a formatted string 
	 *
	 * 
	 */
	public String toString() {
		return toExpression(Visitors.DEFAULT);
	}
	
	/** Convert to JSON.
	 * 
	 * Each property of the Json object will contain constraint (range).
	 * 
	 * @return A JSON object representing this Cube.
	 */
	public JsonValue toJSON() {
		return toExpression(Visitors.JSON);
	}

	/** Convert a Cube to an expression using the given formatter and context
	 * 
	 * The type parameter T of the formatter is commonly String, which means
	 * that this method will return a String. The formatter may require context
	 * information (for example, information about the parent scope of an expression).
	 * 
	 * @param formatter Object used to format an expression from this Cube
	 * @return A formatted expression
	 */
	public void visit(Visitor<?> visitor) {
        visitor.queryExpr();
        constraints.forEach((key,value)->{
            visitor.dimensionExpr(key);
                value.visit(visitor);
            visitor.endExpr();
        });
        visitor.endExpr();
	}

	/** Bind parameterized values to concrete values.
	 * 
	 * Equivalent to calling bind(key, value) on each entry in the map.
	 * 
	 * @param parameters A map of parameter names to parameter values.
	 * @return A constraint with any matching parameters substituted with the given values.
	 */
	public Query bind(JsonObject parameters) {
		Map<String,AbstractSet<? extends JsonValue,?>> new_constraints = new TreeMap<String,AbstractSet<? extends JsonValue,?>>();
		for (Map.Entry<String,AbstractSet<? extends JsonValue,?>> entry : constraints.entrySet()) {
			AbstractSet<? extends JsonValue,?> new_constraint = entry.getValue().bind(parameters);
			if (!new_constraint.isEmpty())
				new_constraints.put(entry.getKey(), new_constraint);
			else
				return null;
		}
		return new Impl(new_constraints);
	}
	

	
	/** Bind parameterized values to concrete values. 
	 * 
	 * Equivalent to calling bind(key, Value.from(value)) on each property of the
	 * JSON object constructed from the given string.
	 * 
	 * @param parameters A string representation of a JSON object containing parameter values.
	 * @return A constraint with any matching parameters substituted with the given values.
	 */
	public Query bind(String parameters) {
		return bind(JsonUtil.parseObject(parameters));
	}
	
	private static void collectElement(String dimension, JsonValue value, Map<String, AbstractSet<?,?>> results) {
		if (value instanceof JsonObject) {
			JsonObject asObj = (JsonObject)value;
			if (asObj.containsKey("$has")) {
				JsonArray arraydata = asObj.getJsonArray("$has");
				results.put(dimension, ArrayConstraint.match(arraydata));
			} else {
				results.put(dimension, Query.from(asObj));
			}
		} else {
			results.put(dimension, Range.from(value));
		}
	}
	

	

	@Override
	public Query union(Query other) {
		return Query.union(this, other);
	}

	private <T extends JsonValue,U extends AbstractSet<T,U>> Boolean maybeEquals(String dimension, Query other) {
		U constraint1 = (U)constraints.get(dimension);
		U constraint2 = (U)other.getConstraint(dimension);
		if (constraint1 == null || constraint2 == null) return Boolean.FALSE;
		return constraint1.maybeEquals(constraint2);
	}
	
	@Override
	public Boolean maybeEquals(Query other) {
		if (constraints.size() != other.getConstraints().size()) return Boolean.FALSE;
		return Tristate.every(constraints.keySet(), dimension->maybeEquals(dimension, other));
	}
	
	public boolean isEmpty() {
		return false;
	}
	
	public boolean isUnconstrained() {
		return false;
	}
	}
	
	public class Unbounded implements Query {
		@Override	public Query intersect(Query other)  { return other; }
		@Override	public Boolean intersects(Query other) { return Boolean.TRUE; }
		@Override	public Query union(Query other) { return this; }
		@Override	public Boolean containsItem(JsonObject item) { return Boolean.TRUE; }
		@Override	public Boolean contains(Query set) { return Boolean.TRUE; }
		@Override	public Boolean maybeEquals(Query other) { return other == UNBOUNDED; }
		@Override	public void visit(Visitor<?> visitor) { visitor.unbounded(); }
		@Override	public JsonValue toJSON() { return toExpression(Visitors.JSON); }
		@Override	public Query bind(JsonObject values) { return this; }
		@Override	public boolean isEmpty() { return false; }
		@Override	public boolean isUnconstrained() { return true; }
		@Override	public AbstractSet<? extends JsonValue, ?> getConstraint(String dimension) { return null; }
		@Override	public Set<String> getConstraints() { return Collections.emptySet(); }		
		@Override  	public Query maybeUnion(Query other) { return this; }
		@Override  	public String toString() { return toExpression(Visitors.DEFAULT); }

	}
	
	public class Empty implements Query {
		@Override	public Query intersect(Query other)  { return this; }
		@Override	public Boolean intersects(Query other) { return Boolean.FALSE; }
		@Override	public Query union(Query other) { return other; }
		@Override	public Boolean containsItem(JsonObject item) { return Boolean.FALSE; }
		@Override	public Boolean contains(Query set) { return Boolean.FALSE; }
		@Override	public Boolean maybeEquals(Query other) { return other == EMPTY; }
        @Override	public void visit(Visitor<?> visitor) { visitor.operExpr("="); visitor.value(Json.createValue("[]")); visitor.endExpr(); }
		@Override	public JsonValue toJSON() { return toExpression(Visitors.JSON); }
		@Override	public Query bind(JsonObject values) { return this; }
		@Override	public boolean isEmpty() { return true; }
		@Override	public boolean isUnconstrained() { return false; }
		@Override	public AbstractSet<? extends JsonValue, ?> getConstraint(String dimension) { return null; }
		@Override	public Set<String> getConstraints() { return Collections.emptySet(); }		
		@Override  	public Query maybeUnion(Query other) { return other; }
		@Override  	public String toString() { return toExpression(Visitors.DEFAULT); }
	}
	
	public class UnionCube extends Union<JsonObject,Query> implements Query {

		public UnionCube(List<Query> data) {
			super(ValueType.OBJECT, data);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public AbstractSet<?, ?> getConstraint(String dimension) {
			AbstractSet results = null;
			for (Query elem : data) {
				AbstractSet result = elem.getConstraint(dimension);
				if (result != null) {
					results = results == null ? result : results.union(result);
				}
			}
			return results;
		}

		@Override
		public Set<String> getConstraints() {
			Set<String> results = new HashSet<String>();
			for (Query elem : data) {
				results.addAll(elem.getConstraints());
			}
			return results;
		}
		
		public Query maybeUnion(Query other) {
			return null;
		}
	}
	
		
	
	public static Query union(JsonArray constraints) {
		// TODO: sensible error message for cast below
		List<Query> ors = constraints.stream().map((JsonValue value)->from((JsonObject)value)).collect(Collectors.toList());					
		return union(ors);
	}
	
	public static Query union(List<Query> constraints) {
		return FACTORY.union(constraints);
	}

	public static Query union(Query... constraints) {
		return FACTORY.union(constraints);
	}
	
	public static Query intersect(JsonArray constraints) {
		// TODO: sensible error message for cast below
		List<Query> ands = constraints.stream().map((JsonValue value)->from((JsonObject)value)).collect(Collectors.toList());					
		return intersect(ands);
	}
	
	public static Query intersect(List<Query> constraints) {
		return FACTORY.intersect(constraints);
	}

	public static Query intersect(Query... constraints) {
		return FACTORY.intersect(constraints);
	}
		
	public static Query fromJson(String object) {
		return Query.from(JsonUtil.parseObject(object));
	}
	
	public static Query from(String dimension, AbstractSet<? extends JsonValue, ?> constraint) {
		if (constraint == null) throw new IllegalArgumentException("Can't create from a null constraint");
		if (dimension == null) throw new IllegalArgumentException("Can't create from a null dimension");
		if (constraint.isUnconstrained()) return UNBOUNDED;
		return new Impl(dimension, constraint);
	}
	
	/** Create a constraint using a qualified name.
	 * 
	 * Given qualified name a.b.c; from(qualifiedname, range) == from(a,from(b,from(c, range)))
	 * 
	 * @param dimension Qualified name to constrain
	 * @param constraint constraint to apply
	 * @return an ObjectContraint
	 */
	public static Query from(QualifiedName dimension, AbstractSet<? extends JsonValue, ?> constraint) {
		if (constraint == null) throw new IllegalArgumentException("Can't create from a null constraint");
		if (dimension == null || dimension.isEmpty()) throw new IllegalArgumentException("Can't create from a null  or empty dimension");
		Query result =  from(dimension.part, constraint);
		dimension = dimension.parent;
		while (!dimension.isEmpty()) {
			result = from(dimension.part, result);
			dimension = dimension.parent;
		}
		return result;
	}
	
	public static Query from(JsonObject object)  {
		
		if (object.containsKey("$and")) {
			return intersect((object.getJsonArray("$and")));
		}
		
		if (object.containsKey("$or")) {			
			return union((object.getJsonArray("$or")));
		}
		
		TreeMap<String, AbstractSet<? extends JsonValue, ?>> results = new TreeMap<String, AbstractSet<? extends JsonValue, ?>>();
		
		for (Map.Entry<String,JsonValue> entry: object.entrySet()) {
			String dimension = entry.getKey();
			JsonValue value = entry.getValue();
			if (value instanceof JsonObject) {
				JsonObject asObj = (JsonObject)value;
				if (asObj.containsKey("$has")) {
					JsonValue matchdata = asObj.get("$has");
					results.put(dimension, ArrayConstraint.match(matchdata));	
				} else {
					if (Range.isRange(asObj))
						results.put(entry.getKey(), Range.from(asObj));
					else 
						results.put(entry.getKey(), Query.from(asObj));
				}
			} else {
				results.put(dimension, Range.from(value));
			}	
		}
		return new Impl(results);
	}
	
	/** Convert query to something that is URL-safe.
	 * 
	 * @return A url-safe string
	 */
	public default String urlEncode() {
		// TODO: maybe do this better, like use JSURL. Or alternatively add compression?
		return Base64.getUrlEncoder().encodeToString(toJSON().toString().getBytes());
	}

	/** Read a query from url-safe representation
	 * 
	 * @param query An url-safe representation, as originally returned by urlEncode()
	 * @return A query
	 */
	public static Query urlDecode(String query) {
		// TODO: maybe do this better, like use JSURL. Or alternatively add compression?
		return Query.fromJson(new String(Base64.getUrlDecoder().decode(query)));
	}
    
}




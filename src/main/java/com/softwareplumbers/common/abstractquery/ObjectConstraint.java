package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.Value.MapValue;
import com.softwareplumbers.common.abstractquery.Value.Type;
import com.softwareplumbers.common.abstractquery.formatter.Context;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;

import javax.json.JsonObject;

/** A constraint maps each dimension in an abstract space to a range.
 *
 */
public interface ObjectConstraint extends AbstractSet<Value.MapValue, ObjectConstraint> {
	
	public static final ObjectConstraint UNBOUNDED = new Unbounded();
	public static final ObjectConstraint EMPTY = new Empty();
	public static final ObjectConstraintFactory FACTORY = new ObjectConstraintFactory();
	
	public AbstractSet<? extends Value, ?> getConstraint(String dimension);
	public Set<String> getConstraints();
	
	default ObjectConstraint bind(String json) {
		return bind(MapValue.fromJson(json));
	}
	
	default ObjectConstraint intersect(String json) {
		return intersect(ObjectConstraint.fromJson(json));
	}

	default ObjectConstraint union(String json) {
		return union(ObjectConstraint.fromJson(json));
	}
	
	default Factory<Value.MapValue, ObjectConstraint> getFactory() {
		return FACTORY;
	}
	
	ObjectConstraint maybeUnion(ObjectConstraint other);

	public static class Impl implements ObjectConstraint {

	private Map<String, AbstractSet<? extends Value, ?>> constraints;
	
	public ObjectConstraint maybeUnion(ObjectConstraint other) {
		if (this.contains(other)) return this;
		if (other.contains(this)) return other;
		return null;
	}

	/** Create a new constraint
	 * 
	 * @param constraints A map from dimension name to a range of values.
	 */
	public Impl(Map<String, AbstractSet<? extends Value,?>> constraints) {
		this.constraints = constraints;
	}
	
	/** Create a new constraint as a copy of an old constraint.
	 * 
	 * @param to_copy A constraint to copy
	 */
	public Impl(Impl to_copy) {
		this.constraints = new TreeMap<String, AbstractSet<? extends Value,?>>(to_copy.constraints);
	}
	
	/** Create a 'one dimensional' constraint
	 * 
	 * @param dimension name of a dimension
	 * @param range permitted range for that dimension
	 */
	public Impl(String dimension, AbstractSet<? extends Value,?> range) {
		this.constraints = new TreeMap<String, AbstractSet<? extends Value,?>>();
		this.constraints.put(dimension, range);
	}
	
	public Impl() {
		this.constraints = new TreeMap<String,AbstractSet<? extends Value,?>>();
	}

	/** Get the constraint for a given dimension
	 * 
	 * A constraint requires that values for a given dimension reside within
	 * a given range for this constraint.
	 * 
	 * @param dimension the name of the dimension
	 * @return the range of values permitted for the given dimension
	 */
	public AbstractSet<? extends Value,?> getConstraint(String dimension) {
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
	public boolean equals(ObjectConstraint other) {
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
		return other instanceof ObjectConstraint && equals((ObjectConstraint)other);
	}
	
	private <T extends Value,U extends AbstractSet<T,U>> Boolean containsConstraint(String dimension, ObjectConstraint other) {
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
	public Boolean contains(ObjectConstraint other) {
		return Tristate.every(constraints.keySet(), constraint-> containsConstraint(constraint, other));
	}

	private <T extends Value,U extends AbstractSet<T,U>> Boolean containsItem(String dimension, Value.MapValue item) {
		// TODO: We should do some type checking here. Notionally that element.type equals constraint.type
		U constraint = (U)getConstraint(dimension);
		T element = (T)(item.hasProperty(dimension) ? item.getProperty(dimension) : null);
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
	public Boolean containsItem(Value.MapValue item) {
		return Tristate.every(constraints.keySet(),
				entry -> containsItem(entry, item));		
	}
	
	public <T extends Value,U extends AbstractSet<T,U>> Boolean intersects(String dimension, ObjectConstraint other) {
		U constraint1 = (U)getConstraint(dimension);
		U constraint2 = (U)other.getConstraint(dimension);
		if (constraint1 == null || constraint2 == null) return Boolean.TRUE;
		return constraint1.intersects(constraint2);
	}
	
	public Boolean intersects(ObjectConstraint other) {
		return Tristate.every(constraints.keySet(), dimension->intersects(dimension, other));
	}
	
	private <T extends Value,U extends AbstractSet<T,U>> U intersect(String dimension, ObjectConstraint other) {
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
	public ObjectConstraint intersect(ObjectConstraint other) {

		Map<String, AbstractSet<? extends Value, ?>> result = new TreeMap<String, AbstractSet<? extends Value, ?>>();
		result.putAll(constraints);
		
		for (String dimension : other.getConstraints()) {
			AbstractSet<? extends Value, ?> intersection = intersect(dimension, other);
			if (intersection.isEmpty()) return EMPTY;
				result.put(dimension, intersection);
		}

		return new Impl(result);
	}
	
	// TODO: I don't think this is quite right.
	private void removeConstraint(String dimension, AbstractSet<? extends Value,?> constraint) {
		AbstractSet<? extends Value,?> this_constraint = constraints.get(dimension);
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
	public ObjectConstraint removeConstraints(ObjectConstraint to_remove) {
		Impl result = new Impl(this);
		for (String constraint : to_remove.getConstraints()) 
			result.removeConstraint(constraint, to_remove.getConstraint(constraint));
		return result;
	}

	/** Convert to a formatted string 
	 *
	 * Equivalent to .toJSON().toString()
	 * 
	 */
	public String toString() {
		return toExpression(Formatter.DEFAULT);
	}
	
	/** Convert to JSON.
	 * 
	 * Each property of the Json object will contain constraint (range).
	 * 
	 * @return A JSON object representing this Cube.
	 */
	public JsonValue toJSON() {
		return toExpression(Formatter.JSON);
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
	public <T,U> T toExpression(Formatter<T,U> formatter, Context context) {
		final Context obj = context.setType(Context.Type.OBJECT);
		return formatter.andExpr(obj,
				Value.Type.MAP,
			constraints.entrySet().stream().map(
				entry -> entry.getValue().toExpression(formatter, obj.in(entry.getKey()))
			)
		);
	}

	/** Bind parameterized values to concrete values.
	 * 
	 * Equivalent to calling bind(key, value) on each entry in the map.
	 * 
	 * @param parameters A map of parameter names to parameter values.
	 * @return A constraint with any matching parameters substituted with the given values.
	 */
	public ObjectConstraint bind(Value.MapValue parameters) {
		Map<String,AbstractSet<? extends Value,?>> new_constraints = new TreeMap<String,AbstractSet<? extends Value,?>>();
		for (Map.Entry<String,AbstractSet<? extends Value,?>> entry : constraints.entrySet()) {
			AbstractSet<? extends Value,?> new_constraint = entry.getValue().bind(parameters);
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
	 * given JSON object.
	 * 
	 * @param parameters A json object containing parameter values.
	 * @return A constraint with any matching parameters substituted with the given values.
	 */
	public ObjectConstraint bind(JsonObject parameters) {
		return bind(MapValue.from(parameters));
	}
	
	/** Bind parameterized values to concrete values. 
	 * 
	 * Equivalent to calling bind(key, Value.from(value)) on each property of the
	 * JSON object constructed from the given string.
	 * 
	 * @param parameters A string representation of a JSON object containing parameter values.
	 * @return A constraint with any matching parameters substituted with the given values.
	 */
	public ObjectConstraint bind(String parameters) {
		return bind(JsonUtil.parseObject(parameters));
	}
	
	private static void collectElement(String dimension, JsonValue value, Map<String, AbstractSet<? extends Value,?>> results) {
		if (value instanceof JsonObject) {
			JsonObject asObj = (JsonObject)value;
			if (asObj.containsKey("$has")) {
				JsonArray arraydata = asObj.getJsonArray("$has");
				results.put(dimension, ArrayConstraint.match(arraydata));
			} else {
				results.put(dimension, ObjectConstraint.from(asObj));
			}
		} else {
			results.put(dimension, Range.from(value));
		}
	}
	

	

	@Override
	public ObjectConstraint union(ObjectConstraint other) {
		return ObjectConstraint.union(this, other);
	}

	private <T extends Value,U extends AbstractSet<T,U>> Boolean maybeEquals(String dimension, ObjectConstraint other) {
		U constraint1 = (U)constraints.get(dimension);
		U constraint2 = (U)other.getConstraint(dimension);
		if (constraint1 == null || constraint2 == null) return Boolean.FALSE;
		return constraint1.maybeEquals(constraint2);
	}
	
	@Override
	public Boolean maybeEquals(ObjectConstraint other) {
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
	
	public class Unbounded implements ObjectConstraint {
		@Override	public ObjectConstraint intersect(ObjectConstraint other)  { return other; }
		@Override	public Boolean intersects(ObjectConstraint other) { return Boolean.TRUE; }
		@Override	public ObjectConstraint union(ObjectConstraint other) { return this; }
		@Override	public Boolean containsItem(MapValue item) { return Boolean.TRUE; }
		@Override	public Boolean contains(ObjectConstraint set) { return Boolean.TRUE; }
		@Override	public Boolean maybeEquals(ObjectConstraint other) { return other == UNBOUNDED; }
		@Override	public <X,V> X toExpression(Formatter<X,V> formatter, Context context) { return formatter.unbounded(context); }
		@Override	public JsonValue toJSON() { return toExpression(Formatter.JSON); }
		@Override	public ObjectConstraint bind(MapValue values) { return this; }
		@Override	public boolean isEmpty() { return false; }
		@Override	public boolean isUnconstrained() { return true; }
		@Override	public AbstractSet<? extends Value, ?> getConstraint(String dimension) { return null; }
		@Override	public Set<String> getConstraints() { return Collections.emptySet(); }		
		@Override  	public ObjectConstraint maybeUnion(ObjectConstraint other) { return this; }
	}
	
	public class Empty implements ObjectConstraint {
		@Override	public ObjectConstraint intersect(ObjectConstraint other)  { return this; }
		@Override	public Boolean intersects(ObjectConstraint other) { return Boolean.FALSE; }
		@Override	public ObjectConstraint union(ObjectConstraint other) { return other; }
		@Override	public Boolean containsItem(MapValue item) { return Boolean.FALSE; }
		@Override	public Boolean contains(ObjectConstraint set) { return Boolean.FALSE; }
		@Override	public Boolean maybeEquals(ObjectConstraint other) { return other == EMPTY; }
		@Override	public <X,V> X toExpression(Formatter<X,V> formatter, Context context) { return formatter.operExpr(context, "=", Value.from("[]")); }
		@Override	public JsonValue toJSON() { return toExpression(Formatter.JSON); }
		@Override	public ObjectConstraint bind(MapValue values) { return this; }
		@Override	public boolean isEmpty() { return true; }
		@Override	public boolean isUnconstrained() { return false; }
		@Override	public AbstractSet<? extends Value, ?> getConstraint(String dimension) { return null; }
		@Override	public Set<String> getConstraints() { return Collections.emptySet(); }		
		@Override  	public ObjectConstraint maybeUnion(ObjectConstraint other) { return other; }
	}
	
	public class UnionCube extends Union<MapValue,ObjectConstraint> implements ObjectConstraint {

		public UnionCube(List<ObjectConstraint> data) {
			super(Value.Type.MAP, data);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public AbstractSet<? extends Value, ?> getConstraint(String dimension) {
			AbstractSet results = null;
			for (ObjectConstraint elem : data) {
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
			for (ObjectConstraint elem : data) {
				results.addAll(elem.getConstraints());
			}
			return results;
		}
		
		public ObjectConstraint maybeUnion(ObjectConstraint other) {
			return null;
		}
	}
	
		
	
	public static ObjectConstraint union(JsonArray constraints) {
		// TODO: sensible error message for cast below
		List<ObjectConstraint> ors = constraints.stream().map((JsonValue value)->from((JsonObject)value)).collect(Collectors.toList());					
		return union(ors);
	}
	
	public static ObjectConstraint union(List<ObjectConstraint> constraints) {
		return FACTORY.union(constraints);
	}

	public static ObjectConstraint union(ObjectConstraint... constraints) {
		return FACTORY.union(constraints);
	}
	
	public static ObjectConstraint intersect(JsonArray constraints) {
		// TODO: sensible error message for cast below
		List<ObjectConstraint> ands = constraints.stream().map((JsonValue value)->from((JsonObject)value)).collect(Collectors.toList());					
		return intersect(ands);
	}
	
	public static ObjectConstraint intersect(List<ObjectConstraint> constraints) {
		return FACTORY.intersect(constraints);
	}

	public static ObjectConstraint intersect(ObjectConstraint... constraints) {
		return FACTORY.intersect(constraints);
	}
		
	public static ObjectConstraint fromJson(String object) {
		return ObjectConstraint.from(JsonUtil.parseObject(object));
	}
	
	public static ObjectConstraint from(String dimension, AbstractSet<? extends Value, ?> constraint) {
		return new Impl(dimension, constraint);
	}
	
	public static ObjectConstraint from(JsonObject object)  {
		
		if (object.containsKey("$and")) {
			return intersect((object.getJsonArray("$and")));
		}
		
		if (object.containsKey("$or")) {			
			return union((object.getJsonArray("$or")));
		}
		
		TreeMap<String, AbstractSet<? extends Value, ?>> results = new TreeMap<String, AbstractSet<? extends Value, ?>>();
		
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
						results.put(entry.getKey(), ObjectConstraint.from(asObj));
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
	public static ObjectConstraint urlDecode(String query) {
		// TODO: maybe do this better, like use JSURL. Or alternatively add compression?
		return ObjectConstraint.fromJson(new String(Base64.getUrlDecoder().decode(query)));
	}

}




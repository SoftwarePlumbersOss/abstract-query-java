package com.softwareplumbers.common.abstractquery;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.Value.MapValue;

import javax.json.JsonObject;

/** A cube maps each dimension in an abstract space to a range.
 *
 */
public class Cube implements AbstractSet<Value.MapValue, Cube> {
	
	public static final Cube UNBOUNDED = new Cube();

	private Map<String, AbstractSet<? extends Value, ?>> constraints;

	/** Create a new cube
	 * 
	 * @param constraints A map from dimension name to a range of values.
	 */
	public Cube(Map<String, AbstractSet<? extends Value,?>> constraints) {
		this.constraints = constraints;
	}
	
	/** Create a new cube as a copy of an old cube.
	 * 
	 * @param to_copy A cube to copy
	 */
	public Cube(Cube to_copy) {
		this.constraints = new HashMap<String, AbstractSet<? extends Value,?>>(to_copy.constraints);
	}
	
	/** Create a 'one dimensional' cube
	 * 
	 * @param dimension name of a dimension
	 * @param range permitted range for that dimension
	 */
	public Cube(String dimension, AbstractSet<? extends Value,?> range) {
		this.constraints = new HashMap<String, AbstractSet<? extends Value,?>>();
		this.constraints.put(dimension, range);
	}
	
	public Cube() {
		this.constraints = new HashMap<String,AbstractSet<? extends Value,?>>();
	}

	/** Get the constraint for a given dimension
	 * 
	 * A constraint requires that values for a given dimension reside within
	 * a given range for this cube.
	 * 
	 * @param dimension the name of the dimension
	 * @return the range of values permitted for the given dimension
	 */
	public AbstractSet<? extends Value,?> getConstraint(String dimension) {
		return constraints.get(dimension);
	}
	
	/** Get the set of dimension names for this cube.
	 * 
	 * @return the set of dimension names valid for this cube
	 */
	public Set<String> getDimensions() {
		return constraints.keySet();
	}

	/** Check that two Cubes are equal
	 * 
	 * Cubes are considered equal if there is no possible value which meets the
	 * constraints of one cube ('is contained by the cube') and does not meet the
	 * constraints of the other.
	 * 
	 * @param other Other cube to check
	 * @return true if cubes are equal
	 */
	public boolean equals(Cube other) {
		return this.maybeEquals(other) ==  Boolean.TRUE;
	}
	
	/** Check whether this cube is equal to some other object
	 * 
	 * If the other object is not a Cube, returns false. Otherwise
	 * equivalent to equals(Cube) above.
	 * 
	 * @param other Other cube to check
	 * @return true if other is Cube and equal to this cube
	 */
	public boolean equals(Object other) {
		return other instanceof Cube && equals((Cube)other);
	}
	
	private <T extends Value,U extends AbstractSet<T,U>> Boolean containsConstraint(String dimension, Cube other) {
		// We should do some type checking here.
		U thisConstraint = (U)constraints.get(dimension);
		U otherConstraint = (U)other.constraints.get(dimension);
		return thisConstraint.contains(otherConstraint);
	}
	
	/** Check whether this cube contains some other cube
	 * 
	 * A cube contains another cube if there is no possible value which
	 * meets the constraints of the contained cube which does not also meet
	 * the constraints of the containing cube.
	 * 
	 * Constraints on cubes may be parameterized; in which case it may not be possible
	 * to determine if one cube is contained by another. In this case, null is returned.
	 * 
	 * @param other Cube to compare
	 * @return true if this cube contains the other cube, false if not, null if we cannot tell.
	 */
	public Boolean contains(Cube other) {
		Optional<Boolean> nomatch = constraints.keySet().stream()
				.map(key -> containsConstraint(key, other))
				.filter(result -> result != Boolean.TRUE)
				.findAny();
				
		return nomatch.isPresent() ? nomatch.get() : Boolean.TRUE;
	}

	private <T extends Value,U extends AbstractSet<T,U>> Boolean containsItem(String dimension, Value.MapValue item) {
		// TODO: We should do some type checking here. Notionally that element.type equals constraint.type
		U constraint = (U)constraints.get(dimension);
		T element = (T)item.getProperty(dimension);
		return constraint.containsItem(element);
	}
	
	/** Check whether this cube contains some value
	 * 
	 * A cube contains a value if the value meets constraints on every dimension of
	 * this cube.
	 * 
	 * Constraints on cubes may be parameterized; in which case it may not be possible
	 * to determine if an item is contained by a cube. In this case, null is returned.
	 * 
	 * @param item Item to compare
	 * @return true if this cube contains the item, false if not, null if we cannot tell.
	 */
	public Boolean containsItem(Value.MapValue item) {
		return Tristate.every(constraints.keySet(),
				entry -> containsItem(entry, item));		
	}
	
	public <T extends Value,U extends AbstractSet<T,U>> Boolean intersects(String dimension, Cube other) {
		U constraint1 = (U)constraints.get(dimension);
		U constraint2 = (U)other.constraints.get(dimension);
		if (constraint1 == null || constraint2 == null) return Boolean.TRUE;
		return constraint1.intersects(constraint2);
	}
	
	public Boolean intersects(Cube other) {
		return Tristate.every(constraints.keySet(), dimension->intersects(dimension, other));
	}
	
	private <T extends Value,U extends AbstractSet<T,U>> U intersect(String dimension, Cube other) {
		// TODO: We should do some type checking here. Notionally that constraint1.type equals constraint2.type
		U constraint1 = (U)constraints.get(dimension);
		U constraint2 = (U)other.constraints.get(dimension);
		if (constraint1 == null) return constraint2;
		if (constraint2 == null) return constraint1;
		return constraint1.intersect(constraint2);
	}

	/** Intersect this cube with some other
	 * 
	 * Create a new cube which contains all items which are contained by both cubes.
	 * 
	 * @param other other cube
	 * @return A new cube which is the logical intersection of this cube and some other.
	 */
	public Cube intersect(Cube other) {

		Map<String, AbstractSet<? extends Value, ?>> result = new HashMap<String, AbstractSet<? extends Value, ?>>();
		result.putAll(constraints);
		
		for (String dimension : other.constraints.keySet()) {
			AbstractSet<? extends Value, ?> intersection = intersect(dimension, other);
			if (intersection.isEmpty()) return null; // TODO: replace with an EMPTY class
			result.put(dimension, intersection);
		}

		return new Cube(result);
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

	/** Remove constraints from a cube.
	 * 
	 * Used for factoring query expressions. At this point, rather experimental.
	 * 
	 */
	public Cube removeConstraints(Cube to_remove) {
		Cube result = new Cube(this);
		for (Map.Entry<String,AbstractSet<? extends Value,?>> constraint : to_remove.constraints.entrySet()) 
			result.removeConstraint(constraint.getKey(), constraint.getValue());
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
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (Map.Entry<String, AbstractSet<? extends Value,?>> entry : constraints.entrySet())
			builder.add(entry.getKey(), entry.getValue().toJSON());
		return builder.build();
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
	public <T> T toExpression(Formatter<T> formatter) {
		return formatter.andExpr(
				Value.Type.MAP,
			constraints.entrySet().stream().map(
				entry -> entry.getValue().toExpression(formatter.in(entry.getKey()))
			)
		);
	}

	/** Bind parameterized values to concrete values.
	 * 
	 * Equivalent to calling bind(key, value) on each entry in the map.
	 * 
	 * @param parameters A map of parameter names to parameter values.
	 * @return A cube with any matching parameters substituted with the given values.
	 */
	public Cube bind(Value.MapValue parameters) {
		HashMap<String,AbstractSet<? extends Value,?>> new_constraints = new HashMap<String,AbstractSet<? extends Value,?>>();
		for (Map.Entry<String,AbstractSet<? extends Value,?>> entry : constraints.entrySet()) {
			AbstractSet<? extends Value,?> new_constraint = entry.getValue().bind(parameters);
			if (!new_constraint.isEmpty())
				new_constraints.put(entry.getKey(), new_constraint);
			else
				return null;
		}
		return new Cube(new_constraints);
	}
	
	/** Bind parameterized values to concrete values. 
	 * 
	 * Equivalent to calling bind(key, Value.from(value)) on each property of the
	 * given JSON object.
	 * 
	 * @param parameters A json object containing parameter values.
	 * @return A cube with any matching parameters substituted with the given values.
	 */
	public Cube bind(JsonObject parameters) {
		return bind(MapValue.from(parameters));
	}
	
	/** Bind parameterized values to concrete values. 
	 * 
	 * Equivalent to calling bind(key, Value.from(value)) on each property of the
	 * JSON object constructed from the given string.
	 * 
	 * @param parameters A string representation of a JSON object containing parameter values.
	 * @return A cube with any matching parameters substituted with the given values.
	 */
	public Cube bind(String parameters) {
		return bind(JsonUtil.parseObject(parameters));
	}
	
	private static void collectElement(String dimension, JsonValue value, Map<String, AbstractSet<? extends Value,?>> results) {
		if (value instanceof JsonObject) {
			JsonObject asObj = (JsonObject)value;
			if (asObj.containsKey("$has")) {
				JsonArray arraydata = asObj.getJsonArray("$has");
				results.put(dimension, Has.matchRanges(arraydata));
			} else {
				results.put(dimension, Cube.from(asObj));
			}
		} else {
			results.put(dimension, Range.from(value));
		}
	}
	
	public static Cube from(JsonObject object)  {
		HashMap<String, AbstractSet<? extends Value, ?>> results = new HashMap<String, AbstractSet<? extends Value, ?>>();
		
		for (Map.Entry<String,JsonValue> entry: object.entrySet()) {
			String dimension = entry.getKey();
			JsonValue value = entry.getValue();
			if (value instanceof JsonObject) {
				JsonObject asObj = (JsonObject)value;
				if (asObj.containsKey("$has")) {
					JsonArray arraydata = asObj.getJsonArray("$has");
					results.put(dimension, Has.matchRanges(arraydata));
				} else {
					if (Range.isRange(asObj))
						results.put(entry.getKey(), Range.from(asObj));
					else 
						results.put(entry.getKey(), Cube.from(asObj));
				}
			} else {
				results.put(dimension, Range.from(value));
			}	
		}
		return new Cube(results);
	}
	
	public static Cube fromJson(String object) {
		return Cube.from(JsonUtil.parseObject(object));
	}

	@Override
	public Cube union(Cube other) {
		// TODO Auto-generated method stub
		return null;
	}

	private <T extends Value,U extends AbstractSet<T,U>> Boolean maybeEquals(String dimension, Cube other) {
		U constraint1 = (U)constraints.get(dimension);
		U constraint2 = (U)other.constraints.get(dimension);
		if (constraint1 == null || constraint2 == null) return Boolean.FALSE;
		return constraint1.maybeEquals(constraint2);
	}
	
	@Override
	public Boolean maybeEquals(Cube other) {
		if (constraints.size() != other.constraints.size()) return Boolean.FALSE;
		return Tristate.every(constraints.keySet(), dimension->maybeEquals(dimension, other));
	}
	
	public boolean isEmpty() {
		return false;
	}
	
	public boolean isUnconstrained() {
		return false;
	}
}




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
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonObject;

/** A cube maps each dimension in an abstract space to a range.
 *
 */
public class Cube {

	private Map<String,Range> constraints;

	/** Create a new cube
	 * 
	 * @param constraints A map from dimension name to a range of values.
	 */
	public Cube(Map<String,Range> constraints) {
		this.constraints = constraints;
	}
	
	/** Create a new cube as a copy of an old cube.
	 * 
	 * @param to_copy A cube to copy
	 */
	public Cube(Cube to_copy) {
		this.constraints = new HashMap<String, Range>(to_copy.constraints);
	}
	
	/** Create a 'one dimensional' cube
	 * 
	 * @param dimension name of a dimension
	 * @param range permitted range for that dimension
	 */
	public Cube(String dimension, Range range) {
		this.constraints = new HashMap<String, Range>();
		this.constraints.put(dimension, range);
	}
	
	/** Create a cube from a Json object.
	 * 
	 * @see Range in this package for valid Json formats for ranges.
	 * 
	 * @param json object with properties that are valid ranges
	 */
	public Cube(JsonObject json) {
		this.constraints = new HashMap<String, Range>();
		for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
			Range range = Range.from(entry.getValue());
			if (range == null) throw new IllegalArgumentException("Don't know what to do with:" + entry.getKey());
			constraints.put(entry.getKey(), range);
		}
	}

	/** Create a cube from a string representation of a Json object.
	 * 
	 * Properties of the json object are dimension names, values of properties
	 * must be valid Range objects.
	 * 
	 * @see Range in this package for valid JSON formats for ranges.
	 * 
	 * @param json JSON-formatted string
	 */
	public Cube(String json) {
		this(JsonUtil.parseObject(json));
	}

	/** Get the constraint for a given dimension
	 * 
	 * A constraint requires that values for a given dimension reside within
	 * a given range for this cube.
	 * 
	 * @param dimension the name of the dimension
	 * @return the range of values permitted for the given dimension
	 */
	public Range getConstraint(String dimension) {
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
		return this.constraints.equals(other.constraints);
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
		Optional<Boolean> nomatch = constraints.entrySet().stream()
				.map(entry -> entry.getValue().contains(other.getConstraint(entry.getKey())))
				.filter(result -> result != Boolean.TRUE)
				.findAny();
				
		return nomatch.isPresent() ? nomatch.get() : Boolean.TRUE;
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
	public Boolean containsItem(Value item) {
		Optional<Boolean> nomatch = constraints.entrySet().stream()
				.map(entry -> entry.getValue().containsItem(item.getProperty(entry.getKey())))
				.filter(result -> result != Boolean.TRUE)
				.findAny();
				
		return nomatch.isPresent() ? nomatch.get() : Boolean.TRUE;	
	}

	/** Intersect this cube with some other
	 * 
	 * Create a new cube which contains all items which are contained by both cubes.
	 * 
	 * @param other other cube
	 * @return A new cube which is the logical intersection of this cube and some other.
	 */
	public Cube intersect(Cube other) {

		HashMap<String,Range> result = new HashMap<String,Range>(constraints);
		result.putAll(other.constraints);

		for (String dimension : result.keySet()) {
			Range this_range = constraints.get(dimension);
			Range other_range = other.constraints.get(dimension);
			if (this_range != null && other_range != null) {
				Range range_intersection = this_range.intersect(other_range);
				if (range_intersection == null) return null;
				result.put(dimension, range_intersection);
			}
		}

		return new Cube(result);
	}
	
	// TODO: I don't think this is quite right.
	private void removeConstraint(String dimension, Range range) {
		Range constraint = constraints.get(dimension);
		if (constraint != null && constraint.equals(range)) {
			constraints.remove(dimension);
		} else {
			throw new IllegalArgumentException( "{ " + dimension + ":" + range + "} is not a factor of " + this );
		}
	}

	/** Remove constraints from a cube.
	 * 
	 * Used for factoring query expressions. At this point, rather experimental.
	 * 
	 */
	public Cube removeConstraints(Cube to_remove) {
		Cube result = new Cube(this);
		for (Map.Entry<String,Range> constraint : to_remove.constraints.entrySet()) 
			result.removeConstraint(constraint.getKey(), constraint.getValue());
		return result;
	}

	/** Convert to a formatted string 
	 *
	 * Equivalent to .toJSON().toString()
	 * 
	 */
	public String toString() {
		// TODO: maybe this should use the default formatter?
		return toJSON().toString();
	}
	
	/** Convert to JSON.
	 * 
	 * Each property of the Json object will contain constraint (range).
	 * 
	 * @return A JSON object representing this Cube.
	 */
	public JsonValue toJSON() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (Map.Entry<String, Range> entry : constraints.entrySet())
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
	 * @param context Context such as parent scope
	 * @return A formatted expression
	 */
	public <T,U> T toExpression(Formatter<T,U> formatter, U context) {
		return formatter.andExpr(
			constraints.entrySet().stream().map(
				entry -> entry.getValue().toExpression(entry.getKey(), formatter, context)
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
	public Cube bind(Map<String, Value> parameters) {
		HashMap<String,Range> new_constraints = new HashMap<String,Range>();
		for (Map.Entry<String,Range> entry : constraints.entrySet()) {
			Range new_constraint = entry.getValue().bind(parameters);
			if (new_constraint != null)
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
		return bind(parameters.entrySet()
			.stream()
			.collect(Collectors.toMap(e->e.getKey(), e->Value.from(e.getValue()))));
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
	
	public static Cube from(JsonObject object)  {
		return new Cube(
			object.entrySet().stream().collect(
				Collectors.toMap(
					e->e.getKey(), 
					e->Range.from(e.getValue())
				)
			)
		);
	}
}




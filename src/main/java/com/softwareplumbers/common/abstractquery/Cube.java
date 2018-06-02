package com.softwareplumbers.common.abstractquery;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/** A cube maps each dimension in an abstract space to a range.
 *
 */
public class Cube {

	private Map<String,Range> constraints;

	public Cube(Map<String,Range> constraints) {
		this.constraints = constraints;
	}
	
	public Cube(Cube to_copy) {
		this.constraints = new HashMap<String, Range>(constraints);
	}

	public Range getConstraint(String dimension) {
		return constraints.get(dimension);
	}

	public boolean equals(Cube other) {
		return this.constraints.equals(other.constraints);
	}
	
	public boolean equals(Object other) {
		return other instanceof Cube && equals((Cube)other);
	}
	
	public Boolean contains(Cube other) {
		Optional<Boolean> nomatch = constraints.entrySet().stream()
				.map(entry -> entry.getValue().contains(other.getConstraint(entry.getKey())))
				.filter(result -> result != Boolean.TRUE)
				.findAny();
				
		return nomatch.isPresent() ? nomatch.get() : Boolean.TRUE;
	}

	public Boolean containsItem(Value item) {
		Optional<Boolean> nomatch = constraints.entrySet().stream()
				.map(entry -> entry.getValue().containsItem(item.getProperty(entry.getKey())))
				.filter(result -> result != Boolean.TRUE)
				.findAny();
				
		return nomatch.isPresent() ? nomatch.get() : Boolean.TRUE;	
	}

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

	private void removeConstraint(String dimension, Range range) {
		Range constraint = constraints.get(dimension);
		if (constraint != null && constraint.equals(range)) {
			constraints.remove(dimension);
		} else {
			throw new IllegalArgumentException( "{ " + dimension + ":" + range + "} is not a factor of " + this );
		}
	}

	public Cube removeConstraints(String... toRemove) {
		Cube result = new Cube(this);
		for (String dimension : toRemove) 
			result.removeConstraint(dimension, constraints.get(dimension));
		return result;
	}

	public String toString() {
		return toJSON().toString();
	}
	
	public JsonValue toJSON() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (Map.Entry<String, Range> entry : constraints.entrySet())
			builder.add(entry.getKey(), entry.getValue().toJSON());
		return builder.build();
	}

	public <T,U> T toExpression(Formatter<T,U> formatter, U context) {
		return formatter.andExpr(
			constraints.entrySet().stream().map(
				entry -> entry.getValue().toExpression(entry.getKey(), formatter, context)
			)
		);
	}

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
}




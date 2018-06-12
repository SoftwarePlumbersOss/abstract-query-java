package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import java.util.Iterator;

/** A Query represent an arbitrary set of constraints on a set of data.
*
* A constraint, in this case, is a mapping of a field name (or _dimension_) to a Range object. A query
* can be created from a _constraint object_ which is simply an object with a number of properties, where
* the name of each property is the field name and the value is a Range (or something that can be converted 
* into a range with Range.from).
*
* Once a query has been created, it can be combined with other queries or constraint objects using the
* `and` and `or` methods.
*
* A query can be converted into an expression using the toExpression method, which uses the provided callbacks
* to construct an expression in the desired syntax.
*
* The internal query representation is a 'canonical form' composed of a flat sequence of 'ands' joined by 'ors'. 
* Given queries a,b,c,d the internal representation of `(a.or(b)).and(c.or(d))` will actually be something like
* `a.and(c).or(a.and(d)).or(b.and(c)).or(b.and(d))`.
*
* The `optimize` method can work on this internal representation to remove redundant criteria.
*
* The `toExpression` method attempts to remove common factors from the internal representation before generating
* an expression. Calling `a.and(c).or(a.and(d)).toExpression(...)` with appropriate callbacks should generate an 
* expression like `a and (c or d)` instead of `(a and c) or (a and d)`.
*/
public class Query {

	private List<Cube> union;

	
	/** Construct a query from an array of cube objects.
	*
	* @param cubes - An array of cubes.
	*/
	public Query(Cube... cubes) {
		this.union = Arrays.asList(cubes);
	}
	
	/** Construct a query from a list of cube objects.
	*
	* @param cubes - A list of cubes.
	*/
	public Query(List<Cube> cubes) {
		this.union = cubes;
	}

	/** Create a query from an constraint object 
	*
	* The constraint object can have any number of properties. In the following example, the resulting
	* query applies all the following constraints: w >= 3, x == 3, y >= 3, y < 7, z < 7
	*
	* @param obj - A constraint object.
	* @returns a Query
	*/
	public static Query from(JsonObject obj) {
		
		if (obj.containsKey("$and")) {
			Iterator<Query> values = obj.getJsonArray("$and").getValuesAs(val->from((JsonObject)val)).iterator();
			Query result = values.next();
			while (values.hasNext() && result != null) result = result.and(values.next());
			return result;
		}
		
		if (obj.containsKey("$or")) {
			Iterator<Query> values = obj.getJsonArray("$or").getValuesAs(val->from((JsonObject)val)).iterator();
			Query result = values.next();
			while (values.hasNext()) result = result.or(values.next());
			return result;
		}

		return new Query(Cube.from(obj));
	}
	
	/** Create a query from an constraint object 
	*
	* The constraint object can have any number of properties. In the following example, the resulting
	* query applies all the following constraints: w >= 3, x == 3, y >= 3, y < 7, z < 7
	* @example
1	* Query.from("{ w: [3,], x : 3, y : [3,7], z: [,7]}")
	*
	* @param obj - A string formatted as javascript
	* @returns a Query
	*/
	public static Query from(String query) {
		return Query.from(JsonUtil.parseObject(query));
	}

	public static boolean isQuery(JsonObject obj) {
		// TODO: improve
		return true;
	}


	/** Delete any redundant critera from the query */
	public void optimize() {
		// TODO: case i and j are the same?
		for (Iterator<Cube> i = union.iterator(); i.hasNext(); )
			for (Iterator<Cube> j = union.iterator(); j.hasNext(); )
				if (i.next().contains(j.next())) j.remove();
	}

	public static class FactorResult {
		public final Query factored;
		public final Query remainder;
		public FactorResult(Query factored, Query remainder) {
			this.factored = factored;
			this.remainder = remainder;
		}
	}

	/** Attempt to simplify a query by removing a common factor from the canonical form.
	*
	* Given something like: 
	* ```
    *	let query = Query
    *		.from({x: 2, y : [3,4], z : 8})
    *		.or({x:2, y: [,4], z: 7})
    *		.or({x:3, y: [3,], z: 7});
	*
    *	let { factored, remainder } = query.factor({ x: 2});
	* ```
	* factored should equal `Query.from({y : [3,4], z : 8}).or({y: [,4], z: 7})` and
	* remainder should equal `Query.from({x:3, y: [3,], z: 7})`
	*
	* @param {ConstraintObject} constraint - object to factor out of query
	* @return {Query~FactorResult} 
	*/
	public FactorResult factor(Cube common) {
		ArrayList<Cube> result = new ArrayList<Cube>();
		ArrayList<Cube> remainder = new ArrayList<Cube>();
		for (Cube cube : union) {
			try {
				Cube factored_cube = cube.removeConstraints(common);
				result.add(factored_cube);
			} catch (IllegalArgumentException e) {
				remainder.add(cube);
			}
		}

		if (result.size() == 0) {
			return new FactorResult(null, this);
		} else {
			Query factored = new Query(result);
			if (remainder.size() == 0)
				return new FactorResult(factored, null);
			else {
 				return new FactorResult(factored, new Query(remainder));
			}
		}
	}
	
	private static class Bucket {
		public final String dimension;
		public final Range range;
		public int count = 1;
		public Bucket(String dimension, Range range) {
			this.dimension = dimension;
			this.range = range;
		}
	};

	/** Find the factor that is common to the largest number of sub-expressions in canonical form.
	*
	* @returns A constraint object containing the common factor, or undefined.
	*/
	public Cube findFactor() {
		ArrayList<Bucket> buckets = new ArrayList<Bucket>();
		for (Cube cube : this.union) {
			for (String dimension : cube.getDimensions()) {
				boolean match = false;
				for (Bucket bucket : buckets) {
					if (bucket.dimension.equals(dimension) && bucket.range.equals(cube.getConstraint(dimension))) {
						bucket.count++;
						match = true;
					}
				} 
				if (!match) buckets.add(new Bucket(dimension, cube.getConstraint(dimension)));
			}
		}

		Optional<Bucket> bucket = buckets.stream()
			.filter(a -> a.count > 1)
			.sorted((a,b) -> (a.count > b.count) ? -1 : ((b.count > a.count) ? 1 : 0))
			.findFirst();

		return bucket.isPresent() ? new Cube(bucket.get().dimension, bucket.get().range) : null;
	}


	/** Convert a query to a an expression.
	*
	* @param formatter formatter to use to create expression
	* @returns result expression. Typically a string but can be any type.
	*/
	public <T> T toExpression(Formatter<T> formatter) {
		if (this.union.size() == 1) {
			return this.union.get(0).toExpression(formatter);
		}
		if (this.union.size() > 1) {
			Cube factor = this.findFactor();

			if (factor != null) {
				String dimension = factor.getDimensions().iterator().next();
				Range range = factor.getConstraint(dimension);
				FactorResult result = this.factor(factor);

				if (result.factored != null && result.remainder != null) 
					return formatter.orExpr(
						Stream.of(
							formatter.andExpr(
								Stream.of(
									range.toExpression(dimension, formatter), 
									result.factored.toExpression(formatter)
								)
							),
							result.remainder.toExpression(formatter)
						)
					);

				if (result.factored != null) 
					return formatter.andExpr(
						Stream.of(
							range.toExpression(dimension, formatter), 
							result.factored.toExpression(formatter)
						)
					);
			} else {
				return formatter.orExpr(
						this.union.stream().map(
							cube -> cube.toExpression(formatter)
						)
					);
			}

		}
		return null;
	}

	/** Create a new query that will return results in this query or some cube.
	*
	* @param other_cube cube of additional results
	* @returns a new compound query.
	*/
	public Query or(Cube other_cube) {
		ArrayList<Cube> result = new ArrayList<Cube>();
		boolean match = false;
		for (Cube cube : union) {
			if (cube.contains(other_cube)) {
				match = true;
				result.add(cube);
			} else if (other_cube.contains(cube)) {
				match = true;
				result.add(other_cube);
			} else {
				result.add(cube);
			}
		}
		if (!match) result.add(other_cube);

		return new Query(result);
	}

	/** Create a new query that will return the union of results in this query with some other constraint.
	*
	* @param other_constraint
	* @returns a new compound query.
	*/
	public Query or(String other_constraint) {
		return or(Cube.from(JsonUtil.parseObject(other_constraint)));
	}

	/** Create a new query that will return the union of results in this query and with some other query.
	*
	* @param other_query the other query
	* @returns a new compound query that is the union of result sets from both queries
	*/
	public Query or(Query other_query) {
		Query result = this;
		for (Cube cube : other_query.union) {
			result = result.or(cube);
		}
		return result;
	}


	/** Create a new query that will return results that are in this query and in some cube.
	* 
	* @param other_cube cube of additional results
	* @returns a new compound query.
	*/
	public Query and(Cube other_cube) {
		ArrayList<Cube> result = new ArrayList<Cube>();
		for (Cube cube : this.union) {
			Cube intersection = cube.intersect(other_cube);
			if (intersection != null) result.add(intersection);
		}
		return new Query(result);
	}

	/** Create a new query that will return results in this query that also comply with some other constraint.
	*
	* @param {Query~ConstraintObject} other_constraint
	* @returns {Query} a new compound query.
	*/
	public Query and(String constraint) {
		return and(Cube.from(JsonUtil.parseObject(constraint)));
	}


	/** Create a new query that will return the intersection of results in this query and some other query.
	*
	* @param other_query the other query
	* @returns a new compound query that is the intersection of result sets from both queries
	*/
	public Query and(Query other_query) {
		Query result = other_query;
		for (Cube cube : union) {
			result = result.and(cube);
		}
		return result;
	}

	/** Establish if this results of this query would be a superset of the given query.
	*
	* @param other_query the other query
	* @returns true if other query is a subset of this one, false if it isn't, null if containment is indeterminate
	*/
	public Boolean contains(Query other_query) {
		for (Cube cube : other_query.union) {
			Boolean cube_contained = contains(cube);
			if (!cube_contained) return cube_contained;
		}
		return true;
	}

	/** Establish if this results of this query would be a superset of the given cube.
	*
	* @param cube the cube
	* @returns true if cube is a subset of this one, false if it isn't, null if containment is indeterminate
	*/
	public Boolean contains(Cube cube) {
		for (Cube c : this.union) {
			Boolean contains_cube = c.contains(cube);
			if (contains_cube == null || contains_cube) return contains_cube;
		}
		return false;
	}

	/** Establish if a specific data item should be in the results of this query
	*
	* Very similar to `contains`. For an 'item' with simple properties, the result should be identical. 
	* However an object provided to `contains` is assumed to be a constraint, so properties with array/object
	* values are processed as ranges. An item provided to 'containsItem' is an individual data item to test,
	* so array and object properties are not processed as ranges.
	*
	* @param item to test
	* @returns true, false or null
	*/
	public boolean containsItem(Value item) {
		for (Cube c : this.union) {
			Boolean contains_item = c.containsItem(item);
			if (contains_item == null || contains_item) return contains_item;
		}
		return false;		
	}

	/** Establish if this results of this query would be a superset of the given constraint.
	*
	* @param constraint the constraint
	* @returns true if constraint is a subset of this query, false if it isn't, null if containment is indeterminate
	*/
	public Boolean contains(String constraint) {
		return contains(Cube.from(JsonUtil.parseObject(constraint)));
	}

	/** Establish if this result of this query is the same as the given query.
	*
	* @param other_query the other query
	* @returns true if other query is a subset of this one.
	*/
	public boolean equals(Query other_query) {
		int matched = 0;
		// Complicated by the fact that this.union and other_query.union may not be in same order
		for (Cube constraint : union) {
			int index = other_query.union.indexOf(constraint);
			if (index < 0) return false;
			matched++;
		}
		return matched == union.size();
	}

	/** Establish if this result of this query is the same as the given cube.
	*
	* @param cube cube to compare
	* @returns true if results of this query would be the same as the notional results for the cube.
	*/
	public boolean equals(Cube cube) {
		if (union.size() != 1) return false;
		return union.get(0).equals(cube);
	}

	/** Establish if this results of this query would be the same as for a query created from the given constraint.
	*
	* @param constraint the constraint
	* @returns true if constraint is the same as this query.
	*/
	public boolean equals(String other_constraint) {
		return equals(Cube.from(JsonUtil.parseObject(other_constraint)));
	}

	/** Establish if this results of this query would be a superset of the given constraint or query.
	*
	* @param obj the constraint or query
	* @returns true if obj is a subset of this query.
	*/
	public boolean equals(Object obj) {
		if (obj instanceof Query) return this.equals((Query)obj);
		if (obj instanceof Cube) return this.equals((Cube)obj);
		if (obj instanceof String) return this.equals((String)obj);
		return false;
	}

	/** Bind a set of paramters to a query. 
	*
	* Property values from the parameters object are used to fill in values for any parameters that
	* this query was created. So:
	* ```
	* Query
	*	.from("{ height : [ {$:'floor'}, {$:'ceiling'} ]}")
	*	.bind(new TreeMap<String,Value>(){{ put("floor",Value.from(12)); put("ceiling",Floor.from(16);}});
	*	.toString();
	* ```
	* will return something like `height >= 12 and height < 16`.
	*
	* @param parameters map of parameter values
	* @returns new query, with parameter values set.
	*/
	public Query bind(Map<String,Value> parameters) {
		List<Cube> cubes = this.union.stream()
			.map(cube -> cube.bind(parameters))
			.filter(cube -> cube != null)
			.collect(Collectors.toList());

		return cubes.size() > 0 ? new Query(cubes) : null;
	}
	
	public String toString() {
		return toExpression(Formatter.DEFAULT);
	}
}


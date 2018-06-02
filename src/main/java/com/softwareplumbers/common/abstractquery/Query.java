package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
class Query {
	
	private List<Cube> union;

	/** Construct a query from an array of cube objects.
	*
	* Should be considered a private constructor - use from instead to create a query.
	*
	* @param {Cube[]} cubes - An array of cubes.
	*/
	public Query(Cube... cubes) {
		this.union = Arrays.asList(cubes);
	}


	/** Create a query from an constraint object 
	*
	* The constraint object can have any number of properties. In the following example, the resulting
	* query applies all the following constraints: w >= 3, x == 3, y >= 3, y < 7, z < 7
	* @example
1	* Query.from({ w: [3,], x : 3, y : [3,7], z: [,7]})
	*
	* @param {Query~ConstraintObject} obj - A constraint object.
	* @returns a Query
	*/
	static from(obj) {
		return new Query( [ new Cube(obj) ] );
	}

	static isQuery(obj) {
		return obj instanceof Query;
	}

	/** Get the default query formatter
	* @returns {QueryFormatter} the default query formatter
	*/
	static get DEFAULT_FORMAT() {

		function printDimension(context,name) {
			if (name === null) return '$self';
			if (!context || !context.dimension) return name;
			return printDimension(context.context, context.dimension) + "." + name;
		}
		
		const printValue = value => typeof value === 'string' ? '"' + value + '"' : value;

		return {
    		andExpr(...ands) { return ands.join(' and ') }, 
    		orExpr(...ors) { return "(" + ors.join(' or ') + ")"},
    		operExpr(dimension, operator, value, context) {
    			// null dimension implies that we are in a 'has' clause where the dimension is attached to the
    			// outer 'has' operator 
    			if (operator === 'match')
    				return value;
    			if (operator === 'has')
    				return printDimension(context, dimension) + " has(" + value + ")"
    			//if (dimension === null) return '$self' + operator + printValue(value) 

    			return printDimension(context,dimension) + operator + printValue(value) 
    		}
    	}
	}

	/** Delete any redundant critera from the query */
	optimize() {
		for (let i = 0; i < this.union.length; i++)
			for (let j = 0; j < this.union.length; j++) 
				if (this.union[i].contains(this.union[j])) {
					delete this.union[j];
				}
	}

	/**
 	* @typedef {Object} Query~FactorResult
 	* @property {Query} factored - the part of the query from which a factor has been removed
 	* @property {Query} remainder - the part of the query from which a factor could not be removed
 	*/

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
	Query factor(Cube common) {
		let result = [];
		let remainder = [];
		for (let cube of this.union) {
			try {
				let factored_cube = cube.removeConstraints(common);
				result.push(factored_cube);
			} catch (err) {
				remainder.push(cube);
			}
		}

		if (result.length === 0) {
			return { remainder: this };
		} else {
			let factored = new Query(result);
			if (remainder.length === 0)
				return { factored }
			else {
 				return { factored, remainder: new Query(remainder) }
			}
		}
	}

	/** Find the factor that is common to the largest number of sub-expressions in canonical form.
	*
	* @returns {Object} A constraint object containing the common factor, or undefined.
	*/
	findFactor() {
		let constraints = [];
		for (let cube of this.union) {
			for (let dimension in cube) {
				let match = false;
				for ( let bucket of constraints) {
					if (bucket.dimension === dimension && bucket.range.equals(cube[dimension])) {
						bucket.count++;
						match = true;
					}
				} 
				if (!match) constraints.push({ dimension, count: 1, range: cube[dimension] });
			}
		}

		let bucket = constraints
			.filter(a=>a.count > 1)
			.sort((a,b) => (a.count > b.count) ? -1 : ((b.count > a.count) ? 1 : 0))
			.shift();

		return bucket === undefined ? undefined : { [bucket.dimension] : bucket.range };
	}


	/** Convert a query to a an expression.
	*
	* @param {QueryFormatter} [formatter=Query~DEFAULT_FORMATTER] - Generates expressions from element of a query
	* @param {Context} [context] - context information
	* @returns {Expression} expression - result expression. Typically a string but can be any type.
	*/
	toExpression(formatter=Query.DEFAULT_FORMAT, context) {
		if (this.union.length === 1) {
			return this.union[0].toExpression(formatter,context);
		}
		if (this.union.length > 1) {
			let factor = this.findFactor();

			if (factor) {
				let dimension = Object.keys(factor).shift();
				let range = factor[dimension];
				let { factored, remainder } = this.factor(factor);


				if (factored && remainder) 
					return formatter.orExpr(
						formatter.andExpr(
							range.toExpression(dimension, formatter, context), 
							factored.toExpression(formatter, context)
						),
						remainder.toExpression(formatter, context)
					);

				if (factored) 
					return formatter.andExpr(
						range.toExpression(dimension, formatter, context), 
						factored.toExpression(formatter, context)
					);
			} else {
				return formatter.orExpr(
					...this.union.map(
						cube => cube.toExpression(formatter, context)
					)
				);
			}

		}
	}

	/** Create a new query that will return results in this query or some cube.
	* @private 
	* @param {Cube} other_cube - cube of additional results
	* @returns {Query} a new compound query.
	*/
	_orCube(other_cube) {
		let result = [];
		let match = false;
		for (let cube of this.union) {
			if (cube.contains(other_cube)) {
				match = true;
				result.push(cube);
			} else if (other_cube.contains(cube)) {
				match = true;
				result.push(other_cube);
			} else {
				result.push(cube);
			}
		}
		if (!match) result.push(other_cube);

		return new Query(result);
	}

	/** Create a new query that will return the union of results in this query with some other constraint.
	*
	* @param {Query~ConstraintObject} other_constraint
	* @returns {Query} a new compound query.
	*/
	orConstraint(other_constraint) {
		return this._orCube(new Cube(other_constraint));
	}

	/** Create a new query that will return the union of results in this query and with some other query.
	*
	* @param {Query} other_query - the other query
	* @returns {Query} a new compound query that is the union of result sets from both queries
	*/
	orQuery(other_query) {
		let result = this;
		for (let cube of other_query.union) {
			result = result._orCube(cube);
		}
		return result;
	}

	/** Create a new query that will return the union of results in this query and with some other query or constraint.
	*
	* @param {Query|Query~ConstraintObject} obj - the other query or constraint
	* @returns {Query} a new compound query that is the union of result sets
	*/
	or(obj) {
		if (obj instanceof Query) return this.orQuery(obj);
		if (obj instanceof Cube) return this._orCube(obj);
		return this.orConstraint(obj);
	}

	/** Create a new query that will return results that are in this query and in some cube.
	* @private 
	* @param {Cube} other_cube - cube of additional results
	* @returns {Query} a new compound query.
	*/
	_andCube(other_cube) {
		let result = [];
		for (let cube of this.union) {
			let intersection = cube.intersect(other_cube);
			if (intersection) result.push(intersection);
		}
		return new Query(result);
	}

	/** Create a new query that will return results in this query that also comply with some other constraint.
	*
	* @param {Query~ConstraintObject} other_constraint
	* @returns {Query} a new compound query.
	*/
	andConstraint(constraint) {
		return this._andCube(new Cube(constraint));
	}


	/** Create a new query that will return the intersection of results in this query and some other query.
	*
	* @param {Query} other_query - the other query
	* @returns {Query} a new compound query that is the intersection of result sets from both queries
	*/
	andQuery(other_query) {
		let result = other_query;
		for (let cube of this.union) {
			result = result._andCube(cube);
		}
		return result;
	}

	/** Create a new query that will return the union of results in this query and with some other query or constraint.
	*
	* @param {Query|Query~ConstraintObject} obj - the other query or constraint
	* @returns {Query} a new compound query that is the union of result sets
	*/
	and(obj) {
		if (obj instanceof Query) return this.andQuery(obj);
		if (obj instanceof Cube) return this._andCube(obj);
		return this.andConstraint(obj);
	}

	/** Establish if this results of this query would be a superset of the given query.
	*
	* @param {Query} other_query - the other query
	* @returns true if other query is a subset of this one, false if it isn't, null if containment is indeterminate
	*/
	containsQuery(other_query) {
		for (let cube of other_query.union) {
			let cube_contained = this._containsCube(cube);
			if (!cube_contained) return cube_contained;
		}
		return true;
	}

	/** Establish if this results of this query would be a superset of the given cube.
	*
	* @private
	* @param {Cube} cube - the cube
	* @returns true if cube is a subset of this one, false if it isn't, null if containment is indeterminate
	*/
	_containsCube(cube) {
		for (let c of this.union) {
			let contains_cube = c.contains(cube);
			if (contains_cube || contains_cube === null) return contains_cube;
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
	containsItem(item) {
		for (let c of this.union) {
			let contains_item = c.containsItem(item);
			if (contains_item || contains_item === null) return contains_item;
		}
		return false;		
	}

	/** Establish if this results of this query would be a superset of the given constraint.
	*
	* @param {Query~ConstraintObject} constraint - the constraint
	* @returns true if constraint is a subset of this query, false if it isn't, null if containment is indeterminate
	*/
	containsConstraint(constraint) {
		return this._containsCube(new Cube(constraint));
	}

	/** Establish if this results of this query would be a superset of the given constraint or query.
	*
	* Containment may be indeterminate one or more of the queries/constraints involved is parametrized and containment
	* cannot be determined until the parameter values are known. However, the library works quite hard to identify 
	* cases where containment can be determined even if the query is parametrized. For example:
	* ```
	* Query.from({ height: [$.param1, 12]}).contains(Query.from{ height[13, $.param2]})
	* ```
	* will return false since the two ranges can never overlap even though they are parametrized.
	*
	* @param {Query~ConstraintObject|Query} obj - the constraint or query
	* @returns true if obj is a subset of this query, false if it isn't, null if containment is indeterminate
	*/
	contains(obj) {
		if (obj instanceof Query) return this.containsQuery(obj);
		if (obj instanceof Cube) return this._containsCube(obj);
		return this.containsConstraint(obj);
	}

	/** Establish if this result of this query is the same as the given query.
	*
	* @param {Query} other_query - the other query
	* @returns true if other query is a subset of this one.
	*/
	equalsQuery(other_query) {
		let unmatched = Array.from(other_query.union);
		// Complicated by the fact that this.union and other_query.union may not be in same order
		for (let constraint of this.union) {
			let index = unmatched.findIndex(item => constraint.equals(item));
			if (index < 0) return false;
			delete unmatched[index];
		}
		return unmatched.reduce(a=>a+1,0) === 0; // Array.length not change by delete
	}

	/** Establish if this result of this query is the same as the given cube.
	*
	* @private
	* @param {Cube} cube - cube to compare
	* @returns true if results of this query would be the same as the notional results for the cube.
	*/
	_equalsCube(cube) {
		if (this.union.length != 1) return false;
		return this.union[0].equals(cube);
	}

	/** Establish if this results of this query would be the same as for a query created from the given constraint.
	*
	* @param {Query~ConstraintObject} constraint - the constraint
	* @returns true if constraint is the same as this query.
	*/
	equalsConstraint(other_constraint) {
		return _equalsCube(new Cube(other_constraint));
	}

	/** Establish if this results of this query would be a superset of the given constraint or query.
	*
	* @param {Query~ConstraintObject|Query} obj - the constraint or query
	* @returns true if obj is a subset of this query.
	*/
	equals(obj) {
		if (obj instanceof Query) return this.equalsQuery(obj);
		if (obj instanceof Cube) return this._equalsCube(obj);
		return this.equalsConstraint(obj);
	}

	/** Bind a set of paramters to a query. 
	*
	* Property values from the parameters object are used to fill in values for any parameters that
	* this query was created. So:
	* ```
	* Query
	*	.from({ height : [$.floor, $.ceiling]})
	*	.bind({ floor:12, ceiling: 16})
	*	.toExpression();
	* ```
	* will return something like `height >= 12 and height < 16`.
	*
	* @param {Object} parameter values
	* @returns {Query} new query, with parameter values set.
	*/
	bind(parameters) {
		let cubes = Stream
			.from(this.union)
			.map(cube => cube.bind(parameters))
			.filter(cube => cube !== null)
			.toArray();

		return cubes.length > 0 ? new Query(cubes) : null;
	}

	/** Convenience property for filtering
	*
	* Given a query, query.predicate is equal to item=>this.contains(item);
	*
	@ returns {Function} a function that returns true if its parameter is matched by this query.
	*/
	get predicate() {
		return item => this.containsItem(item);
	}

}

module.exports = Query;
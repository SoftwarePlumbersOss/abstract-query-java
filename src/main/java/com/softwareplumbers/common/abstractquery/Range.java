package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/** Range is an abstract class representing a range of values.
 *
 * Objects extending range should implement 'contains', 'intersect', 'equals', and 'bind' operations.
 *
 * Not all ranges are necessarily continuous (like dates/times) or numeric. A Range may also represent
 * a node in a directed graph - in which case 'contains' may mean 'is a parent of' and 'intersect' may 
 * mean 'common subtree'.
 *
 * Range also provides a number of static functions that construct new instances of Range (or 
 * usually of some subclass of range).
 *
 */
public abstract class Range {

	public abstract Boolean contains(Range range);
	public abstract Boolean containsItem(Value item);
	public abstract Range intersect(Range range);
	public abstract <T,U> T toExpression(String dimension, Formatter<T,U> formatter, U context);
	public String toString() { return toJSON().toString(); }
	public abstract JsonValue toJSON();
	public abstract Range bind(Map<String,Value> parameters);
	public abstract Boolean mightEquals(Range other);
	
	public boolean equals(Range other) {
		Boolean mightEqual = mightEquals(other);
		return mightEqual == Boolean.TRUE;
	}
	
	public boolean equals(Object other) {
		return other instanceof Range && equals((Range)other);
	}

	/** Object mapping of range operators to constructor functions
	 *
	 * | Operator String | Constructor Function 		|
	 * |-----------------|---------------------------|
	 * | ">"			 | Range.greaterThan 		|
	 * | "<"			 | Range.LessThan 			|
	 * | ">="			 | Range.greaterThanOrEqual 	|
	 * | "<="			 | Range.lessThanOrEqual 	|
	 * | "="			 | Range.equal 				|
	 * | "$and"			 | Range.and 				|
	 * | "$has"			 | Range.has 				|
	 * | "$hasAll"		 | Range.hasAll 				|
	 */
	static Map<String, Function<Value,Range>> RANGE_OPERATORS = new HashMap<String, Function<Value,Range>>() {{

		put(">", v->Range.greaterThan(v));
		put("<", v->Range.lessThan(v));
		put(">=", v->Range.greaterThanOrEqual(v));
		put("<=", v->Range.lessThanOrEqual(v));
		put("=",  v->Range.equals(v));
	}};

	static final Predicate<String> VALID_OPERATOR = key -> RANGE_OPERATORS.containsKey(key);

	static Range getRange(String operator, Value value) {
		return RANGE_OPERATORS.get(operator).apply(value);
	}

	/** Create a range containing a single value 
	 *
	 * @param value - value to search for
	 * @returns a Range object
	 */
	public static Range equals(Value value) 				
	{ return new Equals(value); }

	/** Create a range containing values less than a given value 
	 *
	 * @param value - value to search for
	 * @returns a Range object
	 */
	public static Range lessThan(Value value) 		
	{ return new LessThan(value); }

	/** Create a range containing values less than or equal to a given value 
	 * @param value - value to search for
	 * @returns a Range object
	 */
	public static Range lessThanOrEqual(Value value) 		
	{ return new LessThanOrEqual(value); }

	/** Create a range containing values greater than a given value 
	 * @param value - value to search for
	 * @returns a Range object
	 */		
	public static Range greaterThan(Value value) 	
	{ return new GreaterThan(value); }

	/** Create a range containing values greater than or equal to a given value 
	 * @param value - value to search for
	 * @returns a Range object
	 */		
	public static Range greaterThanOrEqual(Value value)  	
	{ return new GreaterThanOrEqual(value); }

	/** Create a range containing values between the given values
	 *
	 * @param lower - lower range boundary (inclusive by default)
	 * @param upper - upper range boundary (exclusive by default)
	 * @returns a Range object
	 */
	public static Range between(Value lower, Value upper)	{ 

		Range lowerr = greaterThanOrEqual(lower);
		Range upperr = lessThan(upper);

		return lowerr.intersect(upperr);
	}
	
	public static Range between(Range lower, Range upper) {
		if (lower instanceof LessThanOrEqual) return null;
		if (lower instanceof LessThan) return null;
		if (upper instanceof GreaterThanOrEqual) return null;
		if (upper instanceof GreaterThan) return null;
		if (lower instanceof OpenRange 
			&& upper instanceof OpenRange
			&& ValueComparator.getInstance().lessThan(((OpenRange)lower).value, ((OpenRange)upper).value) != Boolean.FALSE)
			return new Between(lower, upper);
		return null;
	}

	/** Provide access to global Unbounded range
	 */
	public static final Range UNBOUNDED = new Unbounded();

	/** Create a range containing values in all the given ranges
	 *
	 * TODO: consider if this needs to support Between parameters.
	 *
	 * @param ranges
	 * @returns a Range object
	 */
	public static Range and(Range[] ranges) {

		Range intersection = Range.UNBOUNDED;

		for (int i=0; i < ranges.length && intersection != null; i++) {
			intersection = intersection.intersect(ranges[i]);
		}

		return intersection;
	}

	/** Create a range with a subquery
	 * 
	 * NOT IMPLEMENTED YET 
	 *
	 * Objects we are querying may be complex. Where an object property contains an object or an array, we may
	 * what to execute a subquery on data contained by that object or array in order to determine if the original
	 * high-level object is matched or not. A trivial case would be a constraint that reads something like:
	 * ```
	 * 	{name : { last: 'Essex'}}
	 * ```
	 *
	 * @param query  Subquery (which must select data for this range criterion to be satisfied)
	 * @returns  a Range object
	 */
	public static Range subqueryObject(Query query) {
		// TODO: implement
		throw new UnsupportedOperationException();
	}

	/** Create a range with a subquery
	 * 
	 * NOT IMPLEMENTED YET 
	 *
	 * Objects we are querying may be complex. Where an object property contains an object or an array, we may
	 * what to execute a subquery on data contained by that object or array in order to determine if the original
	 * high-level object is matched or not. A trivial case would be a constraint that reads something like:
	 * ```
	 * 	{name : { last: 'Essex'}}
	 * ```
	 *
	 * @param query  Subquery (which must select data for this range criterion to be satisfied)
	 * @returns  a Range object
	 */
	public static Range subqueryArray(Query query) {
		// TODO: implement
		throw new UnsupportedOperationException();
	}

	/** Create a range which includes items containing an array which has elements within a range
	 *
	 * Objects we are querying may be complex. Where an object property contains an array of simple objects, we may
	 * what to execute a search data contained by that object or array in order to determine if the origninal
	 * high-level object is matched or not. A trivial case would be a constraint that reads something like:
	 * ```
	 * 	{tags : { $has: 'javascript'}}
	 * ```
	 * to select objects with the word 'javascript' in the tags array. This constraint can be constructed with: 
	 * ```
	 * 	{ tags: Range.has( Range.equals('javascript') ) }
	 * ```
	 * @param bounds ranges that select items in the list
	 * @returns {Range} a Range object
	 */
	public static Range has(Range bounds) {
		// TODO: implement		
		throw new UnsupportedOperationException();
	}

	/** Create a range which includes items containing an array which has elements within a range
	 *
	 * Objects we are querying may be complex. Where an object property contains an array of simple objects, we may
	 * what to execute a search data contained by that object or array in order to determine if the origninal
	 * high-level object is matched or not. A trivial case would be a constraint that reads something like:
	 * ```
	 * 	{tags : { $hasAll: ['javascript','framework'] } }
	 * ```
	 * to select objects with the words 'javascript' and 'framework' in the tags array. This constraint can be 
	 * constructed with: 
	 * ```
	 * 	{ tags: Range.hasAll( [Range.equals('javascript'),Range.equals('framework')] ) }
	 * ```
	 * @param bounds {Range[]} ranges that select items in the array
	 * @returns {Range} a Range object
	 */
	public static Range hasAll(Range bounds) {
		// TODO: implement
		throw new UnsupportedOperationException();
	}	

	/** Check to see if a JSON object is a Range 
	 *
	 * @param obj - object to check.
	 * @return true if obj has operator property.
	 */
	static boolean isRange(JsonValue obj)	{ 
		return isOpenRange(obj) || isClosedRange(obj) || Value.isValue(obj);
	}

	static boolean isOpenRange(JsonValue obj)	{ 
		if (obj instanceof JsonObject) {
			JsonObject asObj = (JsonObject)obj;
			return asObj.keySet().stream()
					.anyMatch(key -> RANGE_OPERATORS.containsKey(key) && Value.isValue(asObj.get(key)));
		}
		return false;
	}

	static boolean isOpenRangeOrValue(JsonValue obj)	{ 
		return Value.isValue(obj) || isOpenRange(obj);
	}

	static boolean isClosedRange(JsonValue obj) {
		if (obj instanceof JsonArray) {
			JsonArray array = (JsonArray)obj;
			if (array.size() > 2 || array.size() == 0) return false;
			return (array.size() == 1 || isOpenRangeOrValue(array.get(1)) && isOpenRangeOrValue(array.get(0)));
		} else {
			return false;
		}
	}

	/** Create a range from a bounds object
	 *
	 * @param  obj
	 * @return  a range if obj is a bounds object, null otherwise
	 */
	static Range fromOpenRangeOrValue(JsonValue obj, Function<Value,Range> operator) {

		if (obj instanceof JsonObject) {

			JsonObject asObj = (JsonObject)obj;
			Optional<String> propname = asObj.keySet().stream()
					.filter(VALID_OPERATOR)
					.findFirst();

			if (propname.isPresent()) {
				JsonValue value = asObj.get(propname.get());
				return Value.isValue(value) ? getRange(propname.get(), Value.from(value)) : null;
			} else {
				return null;
			}
		} else {
			return operator.apply(Value.from(obj));
		}
	}

	/** Create a range from a bounds object
	 *
	 * @param  obj
	 * @return  a range if obj is a bounds object, null otherwise
	 */
	static Range fromOpenRange(JsonObject obj) {

		Optional<String> propname = obj.keySet().stream()
				.filter(VALID_OPERATOR)
				.findFirst();

		if (propname.isPresent()) {
			JsonValue value = obj.get(propname);
			return Value.isValue(value) ? getRange(propname.get(), Value.from(value)) : null;
		} else {
			return null;
		}
	}

	/** Create a range.
	 * 
	 * Specified bounds may be an array
	 *
	 * @param bounds bounding values for range
	 * @returns a range, or undefined if paramters are not compatible.
	 */
	static Range fromClosedRange(JsonValue bounds) 	{ 

		if (bounds instanceof JsonArray) {
			JsonArray array = (JsonArray)bounds;
			Range lower = UNBOUNDED, upper = UNBOUNDED;

			if (array.size() > 2 || array.size() == 0) return null;

			if (array.size() > 0) {
				lower = Range.fromOpenRangeOrValue(array.get(0), value->greaterThanOrEqual(value));				upper = UNBOUNDED;
			}

			if (array.size() > 1) {					
				upper = Range.fromOpenRangeOrValue(array.get(1), value->lessThan(value));
			}

			return lower.intersect(upper);
		}

		return null;
	}


	/** Create a range from Json
	 *
	 * The parameter 'obj' may be a bounds object, a simple value, a parameter a Query, or a Range. The result
	 * varies according to the type of obj, and whether an explicit order function is provided.
	 *
	 * @param jsonValue - value
	 * @returns a range
	 */
	static Range from(JsonValue jsonValue) {
		if (jsonValue == null) return null;
		if (Range.isOpenRangeOrValue(jsonValue)) return Range.fromOpenRangeOrValue(jsonValue, value->equals(value));
		if (Range.isClosedRange(jsonValue)) return Range.fromClosedRange(jsonValue);
		//if (Query.isQuery(obj)) return Range.subquery(query);
		return null;
	}

	static Range from(String value) {
		return from(JsonUtil.parseValue(value));
	}

	/** Range representing an unbounded data set [i.e. no constraint on data returned]
	 *
	 * @private
	 */
	private static class Unbounded extends Range {

		public Boolean contains(Range range) {
			return true;
		}

		public Boolean containsItem(Value item) {
			return true;
		}

		/** unbounded intersection with range always returns range. */
		public Range intersect(Range range) {
			return range;
		}

		public <T,U> T toExpression(String dimension, Formatter<T,U> formatter, U context)	{ 
			return formatter.operExpr(dimension, "=", Value.from("*"), context); 
		}

		public Boolean mightEquals(Range range)	{ return range instanceof Unbounded; }

		public JsonValue toJSON() {
			// TODO: fixme
			return null;
		}

		public Range bind(Map<String,Value> parameters) {
			return this;
		}
	}

	/** Base class for ranges with a single bound (e.g. less than, greater than etc.)
	 *
	 * @private
	 */
	private static abstract class OpenRange extends Range {

		protected String operator;
		protected Value value;

		public OpenRange(String operator, Value value) {
			super();
			this.value = value;
			this.operator = operator;
		}

		public <T,U> T toExpression(String dimension, Formatter<T,U> formatter, U context)	{ 
			return formatter.operExpr(dimension, this.operator, this.value, context); 
		}

		public Boolean mightEquals(Range range)	{ 
			if (range instanceof OpenRange) {
				if (this.operator.equals(((OpenRange)range).operator)) 
					return ValueComparator.getInstance().equals(value, ((OpenRange)range).value);
				else
					return false;
			}
			else
				return false;
		}

		public JsonValue toJSON() {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add(this.operator, this.value.toJSON());
			return builder.build();
		}

		public Range bind(Map<String,Value> parameters) {
			if (value.type == Value.Type.PARAM) {
				Value param = parameters.get(this.value.value);
				// TODO: worry about undefined
				if (param != null) return getRange(operator, param);
			}
			return this;
		}
	}

	/** Range between two bounds.
	 *
	 * @private
	 */
	private static class Between extends Range {

		public final Range lower_bound;
		public final Range upper_bound;

		public Between(Range lower_bound, Range upper_bound) {
			this.lower_bound = lower_bound;
			this.upper_bound = upper_bound;
		}

		public Boolean contains(Range range) {
			return this.lower_bound.contains(range) && this.upper_bound.contains(range);
		}

		public Boolean containsItem(Value item) {
			return this.lower_bound.containsItem(item) && this.upper_bound.containsItem(item);
		}

		public Range intersect(Range range) {
			if (range instanceof Unbounded) return this;
			if (range instanceof Between) {
				Between between = (Between)range;
				Range lower_bound = this.lower_bound.intersect(between.lower_bound);
				Range upper_bound = this.upper_bound.intersect(between.upper_bound);
				// intersection beween two valid lower bounds or two valid upper bounds should always exist
				return lower_bound.intersect(upper_bound);
			}
			if (range instanceof Intersection) { 
				Range result = range.intersect(this.lower_bound);
				if (result != null) result = result.intersect(this.upper_bound);
				return result;
			}
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return this.upper_bound.intersect(range).intersect(this.lower_bound);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual)
				return this.lower_bound.intersect(range).intersect(this.upper_bound);

			return null;
		}

		public <T,U> T toExpression(String dimension, Formatter<T,U> formatter, U context)	{ 
			return formatter.andExpr(
					Stream.of(lower_bound, upper_bound)
					.map(range -> range.toExpression(dimension, formatter, context))
					);
		}

		public Boolean mightEquals(Range range) { 
			if (range instanceof Between) {
				Boolean lower = this.lower_bound.mightEquals(((Between)range).lower_bound);
				if (lower == Boolean.FALSE) return Boolean.FALSE;
				Boolean upper = this.upper_bound.mightEquals(((Between)range).upper_bound);
				if (upper == Boolean.FALSE) return Boolean.FALSE;
				if (lower == Boolean.TRUE && upper == Boolean.TRUE) return Boolean.TRUE;
				return null;
			} else
				return false;
		}

		public JsonValue toJSON() {
			JsonArrayBuilder builder = Json.createArrayBuilder();

			if (this.lower_bound instanceof GreaterThanOrEqual)
				builder.add(((GreaterThanOrEqual)lower_bound).value.toJSON());
			else  
				builder.add(lower_bound.toJSON());

			if (this.upper_bound instanceof LessThan)
				builder.add(((LessThan)upper_bound).value.toJSON());
			else  
				builder.add(upper_bound.toJSON());

			return builder.build();
		}

		public Range bind(Map<String,Value> parameters) {
			Range new_lower_bound = lower_bound.bind(parameters);
			Range new_upper_bound = upper_bound.bind(parameters);
			if (lower_bound == new_lower_bound && upper_bound == new_upper_bound) return this;
			return new_lower_bound.intersect(new_upper_bound);
		}
	}

	/** Range exactly equal to some value
	 * 
	 */
	private static class Equals extends Range {

		private Value value;

		public Equals(Value value) {
			this.value = value;
		}

		public Boolean contains(Range range) {
			return this.mightEquals(range);
		}


		public Boolean containsItem(Value item) {
			return ValueComparator.getInstance().equals(value, item);
		}

		public Range intersect(Range range) {
			if (range instanceof Equals) { 
				Boolean is_equal = ValueComparator.getInstance().equals(this.value, ((Equals)range).value);
				if (is_equal == null) return new Intersection(this, range);
				if (is_equal) return this;
				return null;
			} else {
				return (range.intersect(this));
			}
		}

		public <T,U> T toExpression(String dimension, Formatter<T,U> formatter, U context)	{ 
			return formatter.operExpr(dimension, "=", this.value, context); 
		}

		public Boolean mightEquals(Range range) {
			if (range instanceof Equals) {
				Boolean result = ValueComparator.getInstance().equals(this.value, ((Equals)range).value);
				return result;
			} else
				return Boolean.FALSE;
		}

		public JsonValue toJSON() {
			return value.toJSON();
		}

		public Range bind(Map<String, Value> parameters) {
			if (value.type == Value.Type.PARAM) {
				Value new_value = parameters.get(value.value);
				if (new_value != null) return new Equals(new_value);
			}
			return this;	
		}
	}

	/** Range less than some bound.
	 *
	 * @private
	 */
	private static class LessThan extends OpenRange {

		public static final String OPERATOR = "<";

		public LessThan(Value value) {
			super(OPERATOR, value);
		}

		public Boolean contains(Range range) {
			ValueComparator comparator = ValueComparator.getInstance();

			if (range instanceof Equals)
				return comparator.lessThan(((Equals)range).value, value);
			if (range instanceof LessThanOrEqual) 
				return comparator.lessThan(((LessThanOrEqual)range).value, value);
			if (range instanceof LessThan) 
				return comparator.lessThanOrEqual(((LessThan)range).value, value);
			if (range instanceof Between) 
				return this.contains(((Between)range).upper_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false; 
		}

		public Boolean containsItem(Value item) {
			return value.type == Value.Type.PARAM ? null : ValueComparator.getInstance().lessThan(item, this.value);
		}

		public Range intersect(Range range) {
			ValueComparator comparator = ValueComparator.getInstance();

			if (range instanceof Unbounded) return this;

			// Complex ranges are directly handled by their own class.
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			// a < x && a < y  -> a < x if x <= y, a < y otherwise
			// a < x && a <= y -> a < x if x <= y, a <= y otherwise 
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {			
				Value rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					// tricky - a < z && a <= z -> a < z
					if (value.type == rvalue.type && value.equals(rvalue)) return this;
					return new Intersection(this,range);
				} else {
					if (comparator.lessThanOrEqual(value, rvalue)) return this;
					return range;
				}
			}

			// a < x && a > y -> y<a<x if y < x, null otherwise	
			// a < x && a >= y -> y<=a<x if y < x, null otherwise	
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Value rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {

					if (value.type == rvalue.type && value.equals(rvalue)) return null;
					return new Between(range,this);
				} else {

					if (comparator.lessThan(rvalue, value) == Boolean.TRUE) {
						return new Between(range, this);
					} else {
						return null;
					}
				} 
			}

			// a < x && a = y -> a = y if y < x; null otherwise
			if (range instanceof Equals) {
				Value rvalue = ((Equals)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && value.equals(rvalue)) return null;
					return new Intersection(this,range);
				} else {
					if (comparator.lessThan(rvalue, value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Uknown range type: " + range);
		}

	}


	/** Range less than or equal to some bound
	 *
	 * @private
	 */
	private static class LessThanOrEqual extends OpenRange {

		public static final String OPERATOR = "<=";

		public LessThanOrEqual(Value value) {
			super(OPERATOR, value);
		}

		public Boolean contains(Range range) {
			ValueComparator comparator = ValueComparator.getInstance();

			if (range instanceof Equals)
				return comparator.lessThanOrEqual(((Equals)range).value, this.value);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return comparator.lessThanOrEqual(((OpenRange)range).value, this.value);
			if (range instanceof Between) 
				return contains(((Between)range).upper_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false;
		}

		public Boolean containsItem(Value item) {
			return value.type == Value.Type.PARAM ? null : ValueComparator.getInstance().lessThanOrEqual(item, this.value);
		}

		public Range intersect(Range range) {
			ValueComparator comparator = ValueComparator.getInstance();

			if (range instanceof Unbounded) return this;

			// Complex ranges always handled by their own class
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			// a <= x && a < y  -> a <= x if x < y, a < y otherwise
			// a <= x && a <= y -> a <= x if x < y, a <= y otherwise 
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Value rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && this.value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (comparator.lessThanOrEqual(this.value,rvalue)) return this;
					return range;
				}
			}

			// a <= x && a > y -> y<a<=x if y < x, null otherwise	
			if (range instanceof GreaterThan) {
				Value rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && value.equals(rvalue)) return null;
					return new Between(range,this);
				} else {

					if (comparator.lessThan(rvalue, this.value)) {
						return new Between(range, this);
					} else {
						return null;
					}
				} 
			}

			// a <= x && a >= y -> y<=a<=x if y < x, a = x if y = x, null otherwise	
			if (range instanceof GreaterThanOrEqual) {
				Value rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {

					if (value.type == rvalue.type && this.value.equals(rvalue)) return new Equals(this.value);
					return new Intersection(this,range);
				} else {

					if (comparator.lessThan(rvalue, this.value) == Boolean.TRUE) {
						return new Between(range, this);
					} 
					if (comparator.equals(rvalue, this.value) == Boolean.TRUE) {
						return new Equals(rvalue);
					} 
					return null;
				} 
			}

			// a <= x && a = y -> a = y if y <= x; null otherwise
			if (range instanceof Equals) {
				Value rvalue = ((Equals)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && this.value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (comparator.lessThanOrEqual(rvalue, this.value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Unknown range type: " + range);
		}
	}

	/** Range greater than some bound
	 *
	 * @private
	 */
	private static class GreaterThan extends OpenRange {

		public static final String OPERATOR = ">";

		public GreaterThan(Value value) {
			super(GreaterThan.OPERATOR, value);
		}


		public Boolean contains(Range range) {
			ValueComparator comparator = ValueComparator.getInstance();

			if (range instanceof Equals)
				return comparator.greaterThan(((Equals)range).value, this.value);
			if (range instanceof GreaterThanOrEqual) 
				return comparator.greaterThan(((OpenRange)range).value, this.value);
			if (range instanceof GreaterThan) 
				return comparator.greaterThanOrEqual(((OpenRange)range).value, this.value);
			if (range instanceof Between) 
				return this.contains(((Between)range).lower_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false;
		}

		public Boolean containsItem(Value item) {
			return value.type == Value.Type.PARAM ? null : ValueComparator.getInstance().greaterThan(item, this.value);
		}

		public Range intersect(Range range) {
			if (range instanceof Unbounded) return this;
			ValueComparator comparator = ValueComparator.getInstance();


			// Complex ranges are directly handled by their own class.
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			// a > x && a > y  -> a > x if x >= y, a > y otherwise
			// a > x && a >= y -> a < x if x >= y, a >= y otherwise 
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Value rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return this;
					return new Intersection(this,range);
				} else {
					if (comparator.greaterThanOrEqual(this.value, rvalue)) return this;
					return range;
				}
			}


			// a > x && a < y -> x<a<y if x < y, null otherwise	
			// a > x && a <= y -> x<a<=y if x < y, null otherwise	
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Value rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return null;
					return new Between(this,range);
				} else {

					if (comparator.lessThan(this.value, rvalue)) {
						return new Between(this, range);
					} else {
						return null;
					}
				} 
			}

			// a > x && a = y -> a = y if y > x; null otherwise
			if (range instanceof Equals) {
				Value rvalue = ((Equals)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return null;
					return new Intersection(this,range);
				} else {
					if (comparator.greaterThan(rvalue, this.value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Unknown range type: " + range);
		}

	}

	/** Range greater than or equal to some bound
	 *
	 * @private
	 */
	private static class GreaterThanOrEqual extends OpenRange {

		public static String OPERATOR = ">=";

		public GreaterThanOrEqual(Value value) {
			super(GreaterThanOrEqual.OPERATOR, value);
		}


		public Boolean contains(Range range) {
			ValueComparator comparator = ValueComparator.getInstance();

			if (range instanceof Equals)
				return comparator.greaterThanOrEqual(((Equals)range).value, this.value);
			if (range instanceof GreaterThan  || range instanceof GreaterThanOrEqual) 
				return comparator.greaterThanOrEqual(((OpenRange)range).value, this.value);
			if (range instanceof Between) 
				return this.contains(((Between)range).lower_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false;
		}

		public Boolean containsItem(Value item) {
			return value.type == Value.Type.PARAM ? null : ValueComparator.getInstance().greaterThanOrEqual(item, this.value);
		}


		public Range intersect(Range range) {

			if (range instanceof Unbounded) return this;
			// Complex ranges always handled by their own class
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			ValueComparator comparator = ValueComparator.getInstance();

			// a >= x && a > y  -> a >= x if x > y, a > y otherwise
			// a >= x && a >= y -> a >= x if x > y, a >= y otherwise 
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Value rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (comparator.greaterThanOrEqual(this.value,rvalue)) return this;
					return range;
				}
			}

			// a >= x && a < y -> x<=a<y if y > x, null otherwise	
			if (range instanceof LessThan) {
				Value rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return null;
					return new Between(this,range);
				} else {

					if (comparator.greaterThan(rvalue, this.value)) {
						return new Between(this,range);
					} else {
						return null;
					}
				} 
			}

			// a >= x && a <= y -> x<=a<=y if y > x, a = x if y = x, null otherwise	
			if (range instanceof LessThanOrEqual) {
				Value rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {

					if (value.type == Value.Type.PARAM && value.equals(rvalue)) return new Equals(this.value);
					return new Intersection(this,range);
				} else {

					if (comparator.greaterThan(rvalue, this.value) == Boolean.TRUE) {
						return new Between(range, this);
					} 
					if (comparator.equals(rvalue, this.value) == Boolean.TRUE) {
						return new Equals(rvalue);
					} 
					return null;
				} 
			}

			// a >= x && a = y -> a = y if y >= x; null otherwise
			if (range instanceof Equals) {
				Value rvalue = ((Equals)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (comparator.greaterThanOrEqual(rvalue, this.value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Uknown range type: " + range);
		}

	}




	/** Support a deferred intersection between parametrized ranges.
	 *
	 * @private 
	 */
	public static class Intersection extends Range {

		public static final String OPERATOR = "$and";

		private Range known_bounds;
		private Map<String,Range> parametrized_bounds;
		private List<String> parameters;

		/** construct an intersection
		 *
		 * Note: will throw an error if result is logically empty. We should only call this method with
		 * ranges that are known to not to be exclusive, and should only call it with simple unary ranges or
		 * 'between' ranges.
		 * 
		 * Use repeated calls of .intersect to build an intersection without these limitations.
		 */
		public Intersection(Range... ranges) {
			this.known_bounds = Range.UNBOUNDED;
			this.parametrized_bounds = new HashMap<String,Range>();
			this.parameters = new ArrayList<String>();
			for (Range range : ranges) {
				if (!this.addRange(range)) throw new IllegalArgumentException("Range excludes previously added ranges");
			}
		}

		public Intersection(Intersection to_copy) {
			this.known_bounds = to_copy.known_bounds;
			this.parametrized_bounds = new HashMap<String,Range>(to_copy.parametrized_bounds);
			this.parameters = new ArrayList<String>(to_copy.parameters);
		}

		/** Add a range to this intersection.
		 *
		 * @returns false if then resulting range would be logically empty.
		 */
		boolean addRange(Range range) {

			if (range instanceof Unbounded) {
				return true;
			}

			if (range instanceof Between) {
				Between between = (Between)range;
				return this.addRange(between.lower_bound) && this.addRange(between.upper_bound);
			}

			if (range instanceof Intersection) {
				Intersection intersection = (Intersection)range;
				boolean result = this.addRange(intersection.known_bounds);
				for (int i = 0; i < intersection.parameters.size() && result; i++)
					result = this.addRange(intersection.parametrized_bounds.get(intersection.parameters.get(i)));
				return result;
			}

			Value rvalue = null;
			if (range instanceof Equals) rvalue = ((Equals)range).value;
			if (range instanceof OpenRange) rvalue = ((OpenRange)range).value;

			if (rvalue.type == Value.Type.PARAM) {
				String pname = (String)rvalue.value;
				Range old_param = this.parametrized_bounds.getOrDefault(pname, UNBOUNDED);
				Range new_param = old_param.intersect(range);
				if (new_param == null)  {
					//				console.log('1',new_param, old_param, range);
					return false;
				}
				this.parametrized_bounds.put(pname, new_param);
				if (old_param == UNBOUNDED) this.parameters.add(pname); 
			} else {
				Range new_known_bounds = known_bounds.intersect(range);
				if (new_known_bounds == null) {
					//				console.log('2',known_bounds, this.known_bounds, range);
					return false;
				}
				known_bounds = new_known_bounds;
			}
			return true;
		}


		public Boolean contains(Range range) {
			Boolean result = this.known_bounds.contains(range);
			for (int i = 0; i < this.parameters.size() && result != null && result; i++)
				result = this.parametrized_bounds.get(this.parameters.get(i)).contains(range);
			return result;
		}

		public Boolean containsItem(Value item) {
			Boolean result = this.known_bounds.containsItem(item);
			for (int i = 0; i < this.parameters.size() && result != null && result; i++)
				result = this.parametrized_bounds.get(this.parameters.get(i)).containsItem(item);
			return result;
		}

		/** Determine if this range contained by another.
		 *
		 *
		 */
		public Boolean containedBy(Range range) {

			// range contains intersection if it contains the known bounds, or any of the parameterized bounds
			if (range.contains(this.known_bounds) == Boolean.TRUE) return true;
			// the only way we can know that this range contains a parametrized range is if they have the same
			// parameter. 
			if (range instanceof OpenRange) {
				Value rvalue = ((OpenRange)range).value;
				if (rvalue.type == Value.Type.PARAM) {
					return range.contains(this.parametrized_bounds.getOrDefault((String)rvalue.value, UNBOUNDED));
				}
			}
			if (range instanceof Equals) {
				Value rvalue = ((Equals)range).value;
				if (rvalue.type == Value.Type.PARAM) {
					return range.contains(this.parametrized_bounds.getOrDefault((String)rvalue.value, UNBOUNDED));
				}
			}

			//However, we can return a definitive false if all the parametrized bounds return false,
			// which can happen, for example, if a 'less than' is compared to a 'greater than'
			Boolean containedBy = Boolean.FALSE;
			for (int i = 0; i < parameters.size() && containedBy == Boolean.FALSE; i++) {
				containedBy = range.contains(parametrized_bounds.get(parameters.get(i)));
			}

			return containedBy == Boolean.TRUE ? null : containedBy; 
		}

		public Range intersect(Range range) {
			if (range instanceof Unbounded) return this;

			if (range instanceof Intersection || range instanceof Between) {
				Range result = range.intersect(this.known_bounds);
				for (int i = 0; i < this.parameters.size() && result != null; i++)
					result = result.intersect(parametrized_bounds.get(parameters.get(i)));
				return result;
			}

			// essentially, clone this intersection
			Intersection result = new Intersection(this);

			if (result.addRange(range)) return result;

			return null;
		}

		public <T,U> T toExpression(String dimension, Formatter<T,U> formatter, U context)	{ 
			Stream<Range> ranges = Stream.concat(
					Stream.of(known_bounds), 
					parametrized_bounds.values().stream()
					);

			return formatter.andExpr(ranges.map(range->range.toExpression(dimension, formatter, context)));
		}

		public Boolean mightEquals(Intersection range) { 
			if (this.known_bounds.equals(range.known_bounds)) {
				if (parameters.size() == range.parameters.size()) {
					Boolean  result = true;
					for (int i = 0; i < parameters.size() && result != null && result; i++) {
						String param = parameters.get(i);
						Range other_bound = range.parametrized_bounds.get(param);
						result = other_bound != null ? parametrized_bounds.get(param).equals(other_bound) : false;
					}
					return result;
				}
			}
			return false;
		}
		
		public Boolean mightEquals(Range range) {
			if (range instanceof Intersection) return mightEquals((Intersection)range);
			return Boolean.FALSE;
		}

		public boolean equals(Object other) {
			return other instanceof Intersection && equals((Intersection)other);
		}

		public JsonObject toJSON()	{
			JsonArrayBuilder array = Json.createArrayBuilder();
			Stream.concat(Stream.of(this.known_bounds), this.parametrized_bounds.values().stream())
			.map(range -> range.toJSON())
			.forEach(obj -> array.add(obj));
			JsonObjectBuilder obj  = Json.createObjectBuilder();
			obj.add("$and", array);
			return obj.build();
		}

		public Range bind(Map<String,Value> param_map) {
			Range result = known_bounds;
			for (int i = 0; i < parameters.size() && result != null; i++)
				result = result.intersect(parametrized_bounds.get(parameters.get(i)).bind(param_map));
			return result;
		}
	}
}





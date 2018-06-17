package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.Value.Atomic;

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
public interface Range extends AbstractSet<Value.Atomic, Range> {
	
	public Range merge(Range other);
	
	public default boolean equals(Range other) {
		Boolean mightEqual = maybeEquals(other);
		return mightEqual == Boolean.TRUE;
	}
	
	public default Range union(Range other) {
		return from(this, other);
	}
	
	/** Object mapping of range operators to constructor functions
	 *
	 * | Operator String | Constructor Function 	|
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
	static Range getRange(String operator, Value.Atomic value) {
		switch(operator) {
			case ">" : return Range.greaterThan(value);
			case "<" : return Range.lessThan(value);
			case ">=" : return Range.greaterThanOrEqual(value);
			case "<=" : return Range.lessThanOrEqual(value);
			case "=" : return Range.equals(value);
		}
		throw new IllegalArgumentException("Invalid operator" +  operator);
	}
	
	static boolean validOperator(String operator) {
		switch(operator) {
		case ">" : return true;
		case "<" : return true;
		case ">=" : return true;
		case "<=" : return true;
		case "=" : return true;
		default: return false;
		}
	}		

	/** Create a range containing a single value 
	 *
	 * @param value - value to search for
	 * @returns a Range object
	 */
	public static Range equals(Value.Atomic value) 				
	{ return new Equals(value); }

	/** Create a range containing values less than a given value 
	 *
	 * @param value - value to search for
	 * @returns a Range object
	 */
	public static  Range lessThan(Value.Atomic value) 		
	{ return new LessThan(value); }

	/** Create a range containing values less than or equal to a given value 
	 * @param value - value to search for
	 * @returns a Range object
	 */
	public static  Range lessThanOrEqual(Value.Atomic value) 		
	{ return new LessThanOrEqual(value); }

	/** Create a range containing values greater than a given value 
	 * @param value - value to search for
	 * @returns a Range object
	 */		
	public static  Range greaterThan(Value.Atomic value) 	
	{ return new GreaterThan(value); }

	/** Create a range containing values greater than or equal to a given value 
	 * @param value - value to search for
	 * @returns a Range object
	 */		
	public static  Range greaterThanOrEqual(Value.Atomic value)  	
	{ return new GreaterThanOrEqual(value); }

	/** Create a range containing values between the given values
	 *
	 * @param lower - lower range boundary (inclusive by default)
	 * @param upper - upper range boundary (exclusive by default)
	 * @returns a Range object
	 */
	public static  Range between(Value.Atomic lower, Value.Atomic upper)	{ 

		Range lowerr = greaterThanOrEqual(lower);
		Range upperr = lessThan(upper);

		return lowerr.intersect(upperr);
	}
	
	/**
	 * 
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static  Range between(Range lower, Range upper) {
		if (lower instanceof LessThanOrEqual) return null;
		if (lower instanceof LessThan) return null;
		if (upper instanceof GreaterThanOrEqual) return null;
		if (upper instanceof GreaterThan) return null;
		if (lower instanceof OpenRange 
			&& upper instanceof OpenRange
			&& ((OpenRange)lower).value.lessThan(((OpenRange)upper).value) != Boolean.FALSE)
			return new Between(lower, upper);
		return null;
	}

	/** Provide access to global Unbounded range
	 */
	public static final Range UNBOUNDED = new Unbounded();
	
	/** Check to see if a JSON object is a Range 
	 *
	 * @param obj - object to check.
	 * @return true if obj has operator property.
	 */
	static boolean isRange(JsonValue obj)	{ 
		return isOpenRange(obj) || isClosedRange(obj) || Value.isAtomicValue(obj);
	}

	static boolean isOpenRange(JsonValue obj)	{ 
		if (obj instanceof JsonObject) {
			JsonObject asObj = (JsonObject)obj;
			return asObj.keySet().stream()
					.anyMatch(key -> validOperator(key) && Value.isAtomicValue(asObj.get(key)));
		}
		return false;
	}

	static boolean isOpenRangeOrValue(JsonValue obj)	{ 
		return Value.isAtomicValue(obj) || isOpenRange(obj);
	}

	static boolean isOpenRangeValueOrNull(JsonValue obj)	{ 
		return obj == JsonValue.NULL || Value.isAtomicValue(obj) || isOpenRange(obj);
	}
	static boolean isClosedRange(JsonValue obj) {
		if (obj instanceof JsonArray) {
			JsonArray array = (JsonArray)obj;
			if (array.size() > 2 || array.size() == 0) return false;
			return (array.size() == 1 || isOpenRangeValueOrNull(array.get(1)) && isOpenRangeValueOrNull(array.get(0)));
		} else {
			return false;
		}
	}

	/** Create a range from a bounds object
	 *
	 * @param  obj
	 * @return  a range if obj is a bounds object, null otherwise
	 */
	@SuppressWarnings("unchecked")
	static  Range fromOpenRangeOrValue(JsonValue obj, Function<Value.Atomic,Range> operator) {

		if (obj instanceof JsonObject) {

			JsonObject asObj = (JsonObject)obj;
			Optional<String> propname = asObj.keySet().stream()
					.filter(Range::validOperator)
					.findFirst();

			if (propname.isPresent()) {
				JsonValue value = asObj.get(propname.get());
				return Value.isAtomicValue(value) ? getRange(propname.get(), (Value.Atomic)Value.Atomic.from(value)) : null;
			} else if (asObj.containsKey("$")) {
				return Range.equals((Value.Atomic)Value.Atomic.from(asObj));
			} else {
				return null;
			}
		} else if (obj == JsonValue.NULL) {
			return UNBOUNDED;
		} else {
			return operator.apply((Atomic)Value.Atomic.from(obj));
		}
	}

	/** Create a range from a bounds object
	 *
	 * @param  obj
	 * @return  a range if obj is a bounds object, null otherwise
	 */
	@SuppressWarnings("unchecked")
	static  Range fromOpenRange(JsonObject obj) {

		Optional<String> propname = obj.keySet().stream()
				.filter(Range::validOperator)
				.findFirst();

		if (propname.isPresent()) {
			JsonValue value = obj.get(propname);
			return Value.isAtomicValue(value) ? getRange(propname.get(), (Value.Atomic)Value.Atomic.from(value)) : null;
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
	static  Range fromClosedRange(JsonValue bounds) 	{ 

		if (bounds instanceof JsonArray) {
			JsonArray array = (JsonArray)bounds;
			Range lower = UNBOUNDED, upper = UNBOUNDED;

			if (array.size() > 2 || array.size() == 0) return null;

			if (array.size() > 0) {
				lower = Range.fromOpenRangeOrValue(array.get(0), value->greaterThanOrEqual(value));
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
	static  Range  from(JsonValue jsonValue) {
		if (jsonValue == null) return null;
		if (Range.isOpenRangeOrValue(jsonValue)) return Range.fromOpenRangeOrValue(jsonValue, value->equals(value));
		if (Range.isClosedRange(jsonValue)) return Range.fromClosedRange(jsonValue);
		//if (Query.isQuery(obj)) return Range.subquery(query);
		return null;
	}

	static  Range  from(String value) {
		return from(JsonUtil.parseValue(value));
	}

	/** Range representing an unbounded data set [i.e. no constraint on data returned]
	 *
	 * @private
	 */
	public static class Unbounded implements Range {

		public Boolean contains(Range range) {
			return true;
		}

		public Boolean containsItem(Value.Atomic item) {
			return true;
		}

		/** unbounded intersection with range always returns range. */
		public Range intersect(Range range) {
			return range;
		}

		public Boolean intersects(Range range) {
			return Boolean.TRUE;
		}
		
		public <U> U toExpression(Formatter<U> formatter)	{ 
			return formatter.operExpr("=", Value.from("*")); 
		}
		
		public Range union(Range other) {
			return this;
		}

		public Boolean maybeEquals(Range range)	{ return range instanceof Unbounded; }
		
		public boolean equals(Object other) {
			return other instanceof Range && maybeEquals((Range)other) == Boolean.TRUE;
		}

		public JsonValue toJSON() {
			// TODO: fixme
			return null;
		}

		public Range bind(Map<Param,Value> parameters) {
			return this;
		}
		
		public Range merge(Range other) {
			return this;
		}
		
		public String toString() {
			return toExpression(Formatter.DEFAULT);
		}
	}

	/** Base class for ranges with a single bound (e.g. less than, greater than etc.)
	 *
	 * @private
	 */
	public static abstract class OpenRange implements Range {

		protected String operator;
		protected Value.Atomic value;

		public OpenRange(String operator, Value.Atomic value) {
			super();
			this.value = value;
			this.operator = operator;
		}

		public <U> U toExpression(Formatter<U> formatter)	{ 
			return formatter.operExpr(this.operator, this.value); 
		}

		public Boolean maybeEquals(Range range)	{ 
			if (range instanceof OpenRange) {
				if (this.operator.equals(((OpenRange)range).operator)) 
					return value.maybeEquals(((OpenRange)range).value);
				else
					return false;
			}
			else
				return false;
		}

		public boolean equals(Object other) {
			return other instanceof Range && maybeEquals((Range)other) == Boolean.TRUE;
		}

		public JsonValue toJSON() {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add(this.operator, this.value.toJSON());
			return builder.build();
		}
		
		public String toString() {
			return toExpression(Formatter.DEFAULT);
		}
		
		@SuppressWarnings("unchecked")
		public Range bind(Map<Param,Value> parameters) {
			if (value.type == Value.Type.PARAM) {
				Param param = (Param)((Value.Atomic)this.value).value;
				//TODO: Ranges should have a type?
				Value.Atomic value = (Atomic) parameters.get(param);
				// TODO: worry about undefined
				if (value != null) return getRange(operator, value);
			}
			return this;
		}
	}

	/** Range between two bounds.
	 *
	 * @private
	 */
	public static class Between implements Range {

		public final Range lower_bound;
		public final Range upper_bound;

		public Between(Range lower_bound, Range upper_bound) {
			this.lower_bound = lower_bound;
			this.upper_bound = upper_bound;
		}

		public Boolean contains(Range range) {
			return this.lower_bound.contains(range) && this.upper_bound.contains(range);
		}

		public Boolean containsItem(Value.Atomic item) {
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
		
		public Boolean intersects(Range range) {
			if (range instanceof Unbounded) return Boolean.TRUE;
			if (range instanceof Between) {
				Between between = (Between)range;
				return Tristate.and(
					lower_bound.intersects(between.upper_bound), 
					upper_bound.intersects(between.lower_bound)
				);
			}
			if (range instanceof Intersection) { 
				return Tristate.and(
					range.intersects(lower_bound), 
					range.intersects(upper_bound)
				);
			}
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return upper_bound.intersects(range);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual)
				return lower_bound.intersects(range);

			return null;			
		}

		public <U> U toExpression(Formatter<U> formatter)	{ 
			return formatter.andExpr(
					Stream.of(lower_bound, upper_bound)
					.map(range -> range.toExpression(formatter))
					);
		}

		public Boolean maybeEquals(Range range) { 
			if (range instanceof Between) {
				Boolean lower = this.lower_bound.maybeEquals(((Between)range).lower_bound);
				if (lower == Boolean.FALSE) return Boolean.FALSE;
				Boolean upper = this.upper_bound.maybeEquals(((Between)range).upper_bound);
				if (upper == Boolean.FALSE) return Boolean.FALSE;
				if (lower == Boolean.TRUE && upper == Boolean.TRUE) return Boolean.TRUE;
				return null;
			} else
				return false;
		}

		public boolean equals(Object other) {
			return other instanceof Range && maybeEquals((Range)other) == Boolean.TRUE;
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
		
		public String toString() {
			return toExpression(Formatter.DEFAULT);
		}

		public Range bind(Map<Param,Value> parameters) {
			Range new_lower_bound = lower_bound.bind(parameters);
			Range new_upper_bound = upper_bound.bind(parameters);
			if (lower_bound == new_lower_bound && upper_bound == new_upper_bound) return this;
			return new_lower_bound.intersect(new_upper_bound);
		}

		@Override
		public Range merge(Range range) {
			if (range instanceof Unbounded) return UNBOUNDED;
			if (range instanceof Union) return null;
			if (range instanceof Intersection) return null;
			if (intersect(range) == null) return null;
			if (range instanceof Between)
				return between(lower_bound.union(((Between)range).lower_bound), upper_bound.union(((Between)range).upper_bound));
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return between(lower_bound, upper_bound.union(range));
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual)
				return between(lower_bound.union(range), upper_bound);
			if (range instanceof Equals)
				return this;
			throw new IllegalArgumentException("Unsupported range type");
		}
	}

	/** Range exactly equal to some value
	 * 
	 */
	public static class Equals implements Range {

		private Value.Atomic value;

		public Equals(Value.Atomic value) {
			this.value = value;
		}

		public Boolean contains(Range range) {
			return this.maybeEquals(range);
		}


		public Boolean containsItem(Value.Atomic item) {
			return value.maybeEquals(item);
		}

		public Range intersect(Range range) {
			if (range instanceof Equals) { 
				Boolean is_equal = value.maybeEquals(((Equals)range).value);
				if (is_equal == null) return new Intersection(this, range);
				if (is_equal) return this;
				return null;
			} else {
				return (range.intersect(this));
			}
		}
		
		public Boolean intersects(Range range) {
			if (range instanceof Equals) { 
				return value.maybeEquals(((Equals)range).value);
			} else {
				return range.intersects(this);
			}
		}
		
		public Range merge(Range range) {
			if (range instanceof Unbounded) return UNBOUNDED;
			if (range instanceof Union) return null;
			if (range instanceof Intersection) return null;
			if (intersect(range) == null) return null;
			return range;
		}

		public <U> U toExpression(Formatter<U> formatter)	{ 
			return formatter.operExpr("=", this.value); 
		}

		public Boolean maybeEquals(Range range) {
			if (range instanceof Equals) {
				Boolean result = value.maybeEquals(((Equals)range).value);
				return result;
			} else
				return Boolean.FALSE;
		}
		
		public boolean equals(Object other) {
			return other instanceof Range && maybeEquals((Range)other) == Boolean.TRUE;
		}


		public JsonValue toJSON() {
			return value.toJSON();
		}

		public Range bind(Map<Param, Value> parameters) {
			if (value.type == Value.Type.PARAM) {
				@SuppressWarnings("unchecked")
				Param param = (Param)((Value.Atomic)value).value;
				@SuppressWarnings("unchecked")
				Value.Atomic new_value = (Value.Atomic)parameters.get(param);
				if (new_value != null) return new Equals(new_value);
			}
			return this;	
		}
	}

	/** Range less than some bound.
	 *
	 * @private
	 */
	public static class LessThan extends OpenRange {

		public static final String OPERATOR = "<";

		public LessThan(Value.Atomic value) {
			super(OPERATOR, value);
		}

		public Boolean contains(Range range) {
			if (range instanceof Equals)
				return ((Equals)range).value.lessThan(value);
			if (range instanceof LessThanOrEqual) 
				return ((LessThanOrEqual)range).value.lessThan(value);
			if (range instanceof LessThan) 
				return ((LessThan)range).value.lessThanOrEqual(value);
			if (range instanceof Between) 
				return this.contains(((Between)range).upper_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false; 
		}

		public Boolean containsItem(Value.Atomic item) {
			return item.lessThan(this.value);
		}
		
		public Boolean intersects(Range range) {

			if (range instanceof Unbounded) return Boolean.TRUE;
			if (range instanceof Between || range instanceof Intersection)
				return range.intersects(this);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.lessThan(value);
			if (range instanceof Equals) 
				return ((Equals)range).value.lessThan(value);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}

		public Range intersect(Range range) {

			if (range instanceof Unbounded) return this;

			// Complex ranges are directly handled by their own class.
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			// a < x && a < y  -> a < x if x <= y, a < y otherwise
			// a < x && a <= y -> a < x if x <= y, a <= y otherwise 
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {			
				Value.Atomic rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					// tricky - a < z && a <= z -> a < z
					if (value.type == rvalue.type && value.equals(rvalue)) return this;
					return new Intersection(this,range);
				} else {
					if (value.lessThanOrEqual(rvalue)) return this;
					return range;
				}
			}

			// a < x && a > y -> y<a<x if y < x, null otherwise	
			// a < x && a >= y -> y<=a<x if y < x, null otherwise	
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Value.Atomic rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {

					if (value.type == rvalue.type && value.equals(rvalue)) return null;
					return new Between(range,this);
				} else {

					if (rvalue.lessThan(value) == Boolean.TRUE) {
						return new Between(range, this);
					} else {
						return null;
					}
				} 
			}

			// a < x && a = y -> a = y if y < x; null otherwise
			if (range instanceof Equals) {
				Value.Atomic rvalue = ((Equals)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && value.equals(rvalue)) return null;
					return new Intersection(this,range);
				} else {
					if (rvalue.lessThan(value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Uknown range type: " + range);
		}
		
		public Range merge(Range range) {
			if (range instanceof Unbounded) return UNBOUNDED;
			if (range instanceof Union) return null;
			if (range instanceof Intersection) return null;
			if (intersect(range) == null) return null;
			if (range instanceof Equals)
				return this;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return UNBOUNDED;
			if (range instanceof Between)
				return merge(((Between)range).upper_bound);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Boolean result = value.greaterThan(((OpenRange)range).value);
				if (result == Boolean.TRUE) return this;
				if (result == Boolean.FALSE) return range;
				return null;	
			}
				
			throw new IllegalArgumentException("Uknown range type: " + range);
		}
			
	}


	/** Range less than or equal to some bound
	 *
	 * @private
	 */
	public static class LessThanOrEqual extends OpenRange {

		public static final String OPERATOR = "<=";

		public LessThanOrEqual(Value.Atomic value) {
			super(OPERATOR, value);
		}

		public Boolean contains(Range range) {

			if (range instanceof Equals)
				return ((Equals)range).value.lessThanOrEqual(this.value);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return ((OpenRange)range).value.lessThanOrEqual(this.value);
			if (range instanceof Between) 
				return contains(((Between)range).upper_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false;
		}

		public Boolean containsItem(Value.Atomic item) {
			return value.type == Value.Type.PARAM ? null : item.lessThanOrEqual(this.value);
		}

		public Boolean intersects(Range range) {

			if (range instanceof Unbounded) return Boolean.TRUE;
			if (range instanceof Between || range instanceof Intersection)
				return range.intersects(this);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.lessThanOrEqual(value);
			if (range instanceof Equals) 
				return ((Equals)range).value.lessThanOrEqual(value);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}
		
		public Range intersect(Range range) {
			
			if (range instanceof Unbounded) return this;

			// Complex ranges always handled by their own class
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			// a <= x && a < y  -> a <= x if x < y, a < y otherwise
			// a <= x && a <= y -> a <= x if x < y, a <= y otherwise 
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Value.Atomic rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && this.value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (value.lessThanOrEqual(rvalue)) return this;
					return range;
				}
			}

			// a <= x && a > y -> y<a<=x if y < x, null otherwise	
			if (range instanceof GreaterThan) {
				Value.Atomic rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && value.equals(rvalue)) return null;
					return new Between(range,this);
				} else {

					if (rvalue.lessThan(this.value)) {
						return new Between(range, this);
					} else {
						return null;
					}
				} 
			}

			// a <= x && a >= y -> y<=a<=x if y < x, a = x if y = x, null otherwise	
			if (range instanceof GreaterThanOrEqual) {
				Value.Atomic rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {

					if (value.type == rvalue.type && this.value.equals(rvalue)) return new Equals(this.value);
					return new Intersection(this,range);
				} else {

					if (rvalue.lessThan(this.value) == Boolean.TRUE) {
						return new Between(range, this);
					} 
					if (rvalue.equals(this.value) == Boolean.TRUE) {
						return new Equals(rvalue);
					} 
					return null;
				} 
			}

			// a <= x && a = y -> a = y if y <= x; null otherwise
			if (range instanceof Equals) {
				Value.Atomic rvalue = ((Equals)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == rvalue.type && this.value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (rvalue.lessThanOrEqual(this.value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Unknown range type: " + range);
		}
		
		
		public Range merge(Range range) {
			if (range instanceof Unbounded) return UNBOUNDED;
			if (range instanceof Union) return null;
			if (range instanceof Intersection) return null;
			if (intersect(range) == null) return null;
			if (range instanceof Equals)
				return this;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return UNBOUNDED;
			if (range instanceof Between)
				return merge(((Between)range).upper_bound);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Boolean result = value.greaterThanOrEqual(((OpenRange)range).value);
				if (result == Boolean.TRUE) return this;
				if (result == Boolean.FALSE) return range;
				return null;	
			}
				
			throw new IllegalArgumentException("Uknown range type: " + range);
		}
	}

	/** Range greater than some bound
	 *
	 * @private
	 */
	public static class GreaterThan extends OpenRange {

		public static final String OPERATOR = ">";

		public GreaterThan(Value.Atomic value) {
			super(GreaterThan.OPERATOR, value);
		}

		public Boolean contains(Range range) {

			if (range instanceof Equals)
				return ((Equals)range).value.greaterThan(this.value);
			if (range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.greaterThan(this.value);
			if (range instanceof GreaterThan) 
				return ((OpenRange)range).value.greaterThanOrEqual(this.value);
			if (range instanceof Between) 
				return this.contains(((Between)range).lower_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false;
		}

		public Boolean containsItem(Value.Atomic item) {
			return value.type == Value.Type.PARAM ? null : item.greaterThan(this.value);
		}
		
		public Boolean intersects(Range range) {

			if (range instanceof Unbounded) return Boolean.TRUE;
			if (range instanceof Between || range instanceof Intersection)
				return range.intersects(this);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return ((OpenRange)range).value.greaterThan(value);
			if (range instanceof Equals) 
				return ((Equals)range).value.greaterThan(value);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}

		public Range intersect(Range range) {
			if (range instanceof Unbounded) return this;

			// Complex ranges are directly handled by their own class.
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			// a > x && a > y  -> a > x if x >= y, a > y otherwise
			// a > x && a >= y -> a < x if x >= y, a >= y otherwise 
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Value.Atomic rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return this;
					return new Intersection(this,range);
				} else {
					if (this.value.greaterThanOrEqual(rvalue)) return this;
					return range;
				}
			}


			// a > x && a < y -> x<a<y if x < y, null otherwise	
			// a > x && a <= y -> x<a<=y if x < y, null otherwise	
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Value.Atomic rvalue = ((OpenRange)range).value;
				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return null;
					return new Between(this,range);
				} else {

					if (this.value.lessThan(rvalue)) {
						return new Between(this, range);
					} else {
						return null;
					}
				} 
			}

			// a > x && a = y -> a = y if y > x; null otherwise
			if (range instanceof Equals) {
				Value.Atomic rvalue = ((Equals)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return null;
					return new Intersection(this,range);
				} else {
					if (rvalue.greaterThan(this.value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Unknown range type: " + range);
		}


		public Range merge(Range range) {
			if (range instanceof Unbounded) return UNBOUNDED;
			if (range instanceof Union) return null;
			if (range instanceof Intersection) return null;
			if (intersect(range) == null) return null;
			if (range instanceof Equals)
				return this;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return UNBOUNDED;
			if (range instanceof Between)
				return merge(((Between)range).lower_bound);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Boolean result = value.lessThan(((OpenRange)range).value);
				if (result == Boolean.TRUE) return this;
				if (result == Boolean.FALSE) return range;
				return null;	
			}
				
			throw new IllegalArgumentException("Uknown range type: " + range);
		}

	}

	/** Range greater than or equal to some bound
	 *
	 * @private
	 */
	public static class GreaterThanOrEqual extends OpenRange {

		public static String OPERATOR = ">=";

		public GreaterThanOrEqual(Value.Atomic value) {
			super(GreaterThanOrEqual.OPERATOR, value);
		}


		public Boolean contains(Range range) {

			if (range instanceof Equals)
				return ((Equals)range).value.greaterThanOrEqual(this.value);
			if (range instanceof GreaterThan  || range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.greaterThanOrEqual(this.value);
			if (range instanceof Between) 
				return this.contains(((Between)range).lower_bound);
			if (range instanceof Intersection) {
				return ((Intersection)range).containedBy(this);
			}
			return false;
		}

		public Boolean containsItem(Value.Atomic item) {
			return value.type == Value.Type.PARAM ? null : item.greaterThanOrEqual(this.value);
		}

		public Boolean intersects(Range range) {

			if (range instanceof Unbounded) return Boolean.TRUE;
			if (range instanceof Between || range instanceof Intersection)
				return range.intersects(this);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return ((OpenRange)range).value.greaterThanOrEqual(value);
			if (range instanceof Equals) 
				return ((Equals)range).value.greaterThanOrEqual(value);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}
		
		public Range intersect(Range range) {

			if (range instanceof Unbounded) return this;
			// Complex ranges always handled by their own class
			if (range instanceof Between || range instanceof Intersection)
				return range.intersect(this);

			// a >= x && a > y  -> a >= x if x > y, a > y otherwise
			// a >= x && a >= y -> a >= x if x > y, a >= y otherwise 
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Value.Atomic rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (this.value.greaterThanOrEqual(rvalue)) return this;
					return range;
				}
			}

			// a >= x && a < y -> x<=a<y if y > x, null otherwise	
			if (range instanceof LessThan) {
				Value.Atomic rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && this.value.equals(rvalue)) return null;
					return new Between(this,range);
				} else {

					if (rvalue.greaterThan(this.value)) {
						return new Between(this,range);
					} else {
						return null;
					}
				} 
			}

			// a >= x && a <= y -> x<=a<=y if y > x, a = x if y = x, null otherwise	
			if (range instanceof LessThanOrEqual) {
				Value.Atomic rvalue = ((OpenRange)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {

					if (value.type == Value.Type.PARAM && value.equals(rvalue)) return new Equals(this.value);
					return new Intersection(this,range);
				} else {

					if (rvalue.greaterThan(this.value) == Boolean.TRUE) {
						return new Between(range, this);
					} 
					if (rvalue.equals(this.value) == Boolean.TRUE) {
						return new Equals(rvalue);
					} 
					return null;
				} 
			}

			// a >= x && a = y -> a = y if y >= x; null otherwise
			if (range instanceof Equals) {
				Value.Atomic rvalue = ((Equals)range).value;

				if (value.type == Value.Type.PARAM || rvalue.type == Value.Type.PARAM) {
					if (value.type == Value.Type.PARAM && value.equals(rvalue)) return range;
					return new Intersection(this,range);
				} else {
					if (rvalue.greaterThanOrEqual(this.value)) {
						return range;
					} else {
						return null;
					}
				} 
			}

			throw new IllegalArgumentException("Uknown range type: " + range);
		}

		public Range merge(Range range) {
			if (range instanceof Unbounded) return UNBOUNDED;
			if (range instanceof Union) return null;
			if (range instanceof Intersection) return null;
			if (intersect(range) == null) return null;
			if (range instanceof Equals)
				return this;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return UNBOUNDED;
			if (range instanceof Between)
				return merge(((Between)range).lower_bound);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Boolean result = value.lessThanOrEqual(((OpenRange)range).value);
				if (result == Boolean.TRUE) return this;
				if (result == Boolean.FALSE) return range;
				return null;	
			}
				
			throw new IllegalArgumentException("Uknown range type: " + range);
		}
	}

	/** Support a deferred intersection between parametrized ranges.
	 *
	 * @private 
	 */
	public static class Intersection implements Range {

		public static final String OPERATOR = "$and";

		private Range known_bounds;
		private Map<Param,Range> parametrized_bounds;
		private List<Param> parameters;

		/** construct an intersection
		 *
		 * Note: will throw an error if result is logically empty. We should only call this method with
		 * ranges that are known to not to be exclusive, and should only call it with simple unary ranges or
		 * 'between' ranges.
		 * 
		 * Use repeated calls of .intersect to build an intersection without these limitations.
		 */
		@SafeVarargs
		public Intersection(Range... ranges) {
			this.known_bounds = UNBOUNDED;
			this.parametrized_bounds = new HashMap<Param,Range>();
			this.parameters = new ArrayList<Param>();
			for (Range range : ranges) {
				if (!this.addRange(range)) throw new IllegalArgumentException("Range excludes previously added ranges");
			}
		}

		public Intersection(Intersection to_copy) {
			this.known_bounds = to_copy.known_bounds;
			this.parametrized_bounds = new HashMap<Param,Range>(to_copy.parametrized_bounds);
			this.parameters = new ArrayList<Param>(to_copy.parameters);
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
				Param pname = (Param)((Value.Atomic)rvalue).value;

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

		public Boolean containsItem(Value.Atomic item) {
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
					return range.contains(this.parametrized_bounds.getOrDefault((Param)((Value.Atomic)rvalue).value, UNBOUNDED));
				}
			}
			if (range instanceof Equals) {
				Value rvalue = ((Equals)range).value;
				if (rvalue.type == Value.Type.PARAM) {
					return range.contains(this.parametrized_bounds.getOrDefault((Param)((Value.Atomic)rvalue).value, UNBOUNDED));
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

		public Boolean intersects(Range range) {
			if (range instanceof Unbounded) return Boolean.TRUE;

			return Tristate.and(
						range.intersects(this.known_bounds),
						Tristate.every(this.parametrized_bounds.values(), bound->bound.intersects(range))
					);
		}
		
		public <U> U toExpression(Formatter<U> formatter)	{ 
			Stream<Range> ranges = Stream.concat(
					Stream.of(known_bounds), 
					parametrized_bounds.values().stream()
					);

			return formatter.andExpr(ranges.map(range->range.toExpression(formatter)));
		}

		public Boolean maybeEquals(Intersection range) { 
			if (this.known_bounds.equals(range.known_bounds)) {
				if (parameters.size() == range.parameters.size()) {
					Boolean  result = true;
					for (int i = 0; i < parameters.size() && result != null && result; i++) {
						Param param = parameters.get(i);
						Range other_bound = range.parametrized_bounds.get(param);
						result = other_bound != null ? parametrized_bounds.get(param).equals(other_bound) : false;
					}
					return result;
				}
			}
			return false;
		}

		
		public Boolean maybeEquals(Range range) {
			if (range instanceof Intersection) return maybeEquals((Intersection)range);
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

		public Range bind(Map<Param,Value> param_map) {
			Range result = known_bounds;
			for (int i = 0; i < parameters.size() && result != null; i++) {
				result = result.intersect(parametrized_bounds.get(parameters.get(i)).bind(param_map));
			}
			return result;
		}

		@Override
		public Range union(Range other) {
			return from(this, other);
			
		}

		@Override
		public Range merge(Range other) {
			// TODO Auto-generated method stub
			return null;
		}	
		
		public String toString() {
			return toExpression(Formatter.DEFAULT);
		}

	}
	
	public static List<Range> simplify(List<Range> list) {
		List<Range> result = new ArrayList<Range>();
		result.add(list.get(0));
		for (int i = 0; i < list.size(); i++) {
			Range merged = null;
			for (int j = 1; j < result.size() && merged == null; j++) 
				merged = list.get(j).merge(result.get(i));
			if (merged != null) 
				result.set(i, merged);
			else
				result.add(list.get(i));
		}
		return result;
	}
		
		
	public static class RangeUnion extends Union<Value.Atomic, Range> implements Range   {
		public RangeUnion(List<Range> range, Function<List<Range>, Range> from) {
			super(range, from);
		}
	}
	
	public static Range from(List<Range> list) {
		list = simplify(list);
		if (list.size() == 0) return null;
		if (list.size() == 1) return list.get(0);
		return new RangeUnion(list, Range::from);
	}
	
	public static Range from(Range... list) {
		return from(Arrays.asList(list));
	}
}





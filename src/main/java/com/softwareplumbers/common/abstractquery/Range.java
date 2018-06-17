package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
public abstract class Range implements AbstractSet<Value.Atomic, Range> {

	/** Check if this range contain another range.
	 * 
	 * A range contains another range if every value in the contained range is also contained
	 * by the containing range. Ranges may be parameterized, in which case this cannot always be
	 * determined. 
	 * 
	 * @param range Range to compare to this range.
	 * @return True if this range contains the given range, False if not, null if this cannot be determined
	 */
	public abstract Boolean contains(Range range);
	
	
	/** Check if this range contains a value.
	 * 
	 * A range contains a value if the value meets the implied constraints.Ranges may be 
	 * parameterized, in which case this cannot always be determined. 
	 * 
	 * @param range Range to compare to this range.
	 * @return True if this range contains the given value, False if not, null if this cannot be determined
	 */
	public abstract Boolean containsItem(Value.Atomic item);
	
	/** Create a new range that contains only those values contained by both ranges.
	 * 
	 * The intersection of two ranges contains all the values contained by both ranges.
	 * 
	 * @param range Range to intersect with this range.
	 * @return intersection of the two ranges
	 */	
	public abstract Range intersect(Range range);
	
	/** Format a range into an expression using a formatter
	 * 
	 * @param formatter Formatter object to create expression
	 * @return An expression (usually a string).
	 */
	public abstract <U> U toExpression(Formatter<U> formatter);
	
	public String toString() { return toJSON().toString(); }
	
	public abstract JsonValue toJSON();
	public abstract Range bind(Map<Param,Value> parameters);
	
	public boolean equals(Range other) {
		Boolean mightEqual = maybeEquals(other);
		return mightEqual == Boolean.TRUE;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object other) {
		return other instanceof Range && equals((Range)other);
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
	public static  Range equals(Value.Atomic value) 				
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
	private static class Unbounded extends Range {

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

		public <U> U toExpression(Formatter<U> formatter)	{ 
			return formatter.operExpr("=", Value.from("*")); 
		}
		
		public Range union(Range other) {
			return this;
		}

		public Boolean maybeEquals(Range range)	{ return range instanceof Unbounded; }

		public JsonValue toJSON() {
			// TODO: fixme
			return null;
		}

		public Range bind(Map<Param,Value> parameters) {
			return this;
		}
	}

	/** Base class for ranges with a single bound (e.g. less than, greater than etc.)
	 *
	 * @private
	 */
	private static abstract class OpenRange extends Range {

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

		public JsonValue toJSON() {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add(this.operator, this.value.toJSON());
			return builder.build();
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

		public Range bind(Map<Param,Value> parameters) {
			Range new_lower_bound = lower_bound.bind(parameters);
			Range new_upper_bound = upper_bound.bind(parameters);
			if (lower_bound == new_lower_bound && upper_bound == new_upper_bound) return this;
			return new_lower_bound.intersect(new_upper_bound);
		}

		@Override
		public Range union(Range range) {
			if (range instanceof Unbounded) return UNBOUNDED;
			if (intersect(range) == null)
				return new Union(this, range);
			else {
				if (range instanceof Between)
					return between(lower_bound.union(((Between)range).lower_bound), upper_bound.union(((Between)range).upper_bound));
				if (range instanceof Intersection) 
					return new Union(this, range);
				if (range instanceof LessThan || range instanceof LessThanOrEqual) 
					return between(lower_bound, upper_bound.union(range));
				if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual)
					return between(lower_bound.union(range), upper_bound);
				throw new IllegalArgumentException("Unsupported range type");
			}
		}
	}

	/** Range exactly equal to some value
	 * 
	 */
	private static class Equals extends Range {

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
		
		public Range union(Range other) {
			if (intersect(other) == null)
				return new Union(this, other);
			return other;
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
	private static class LessThan extends OpenRange {

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
		
		public Range union(Range other) {
			if (intersect(other) == null) {
				return new Union(this, other);
			} else {
				if (other instanceof Between)
					return between(((Between)other).lower_bound, union(((Between)other).upper_bound));
				if (other instanceof Intersection) 
					return new Union(this, other);
				if (other instanceof LessThan || other instanceof LessThanOrEqual) {
					Boolean comparison = value.lessThanOrEqual(((OpenRange)other).value);
					if (comparison == Boolean.TRUE) return other;
					if (comparison == null) return new Union(this, other);
					return this;
				}
				if (other instanceof GreaterThan || other instanceof GreaterThanOrEqual)
					return UNBOUNDED;
				throw new IllegalArgumentException("Unsupported range type");

			}
		}

	}


	/** Range less than or equal to some bound
	 *
	 * @private
	 */
	private static class LessThanOrEqual extends OpenRange {

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

		@Override
		public Range union(Range other) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/** Range greater than some bound
	 *
	 * @private
	 */
	private static class GreaterThan extends OpenRange {

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


		@Override
		public Range union(Range other) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/** Range greater than or equal to some bound
	 *
	 * @private
	 */
	private static class GreaterThanOrEqual extends OpenRange {

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

		public Range union(Range other) {
			// TODO: implemnet
			return null;
		}
	}

	/** Range has element within some bound.
	*
	* @private
	*/
	class HasElementsMatching extends Range {

		public static final String OPERATOR = "has";
		
		private List<Range> bounds;

		@SafeVarargs
		public HasElementsMatching(Range... bounds) {
			this.bounds = Arrays.asList(bounds);
		}

		private HasElementsMatching(List<Range> bounds) {
			this.bounds = bounds;
		}

		// For containment, this range must match all array elements matched by the other range. Otherwise, the other range
		// could match an element not matched by this range, which implies this range does not contain the other.
		//
		// If all bounds defined in the other range are contained by at least one bound in this range, this condition
		// is met.
		public Boolean contains(Range range) {
			if (range instanceof HasElementsMatching)
				// use Stream versions of every and other because of tri-state implementation 
				return Tristate.every(
					((HasElementsMatching)range).bounds, 
					range_bound -> 
						Tristate.any(bounds, this_bound -> this_bound.contains(range_bound))
				);
			return false;
		}

		// For every bound, there is some element in item that matches that bound.
		public Boolean containsItem(Value.Atomic item) {
			// TODO: need to support array items at this point
			Collection<Value.Atomic> items = Collections.singletonList(item);
			return Tristate.every(bounds, bound -> Tristate.any(items, element -> bound.containsItem(element))); 
		}

		// hmm, remember that $and : [ { y : { $has : 'numpty' } }, { y : { $has : 'flash' } } ] is not the same as 
		// { y : { $has : Range.and(Range.equals('numpty'), range.equals('flash')) }. The former should match any element
		// with y containing both numpty and flash. The second will match nothing since no array element will equal both
		// numpty and flash.
		public Range intersect(Range range) {
			if (range == UNBOUNDED) return this;
			if (range instanceof HasElementsMatching) {

				List<Range> new_bounds = new ArrayList<Range>(bounds);
				for (Range new_bound : ((HasElementsMatching)range).bounds) {
					if (Boolean.FALSE != Tristate.any(this.bounds, this_bound -> { return this_bound.contains(new_bound); }))
						new_bounds.add(new_bound);
				}

				return new HasElementsMatching(new_bounds);
			}
			throw new RuntimeException("Can't mix array operations and scalar operations on a single field");
		}

		public <U> U toExpression(Formatter<U> formatter) { 
			return formatter.subExpr(OPERATOR, bounds.stream().map(range->range.toExpression(formatter)));
		}

		public boolean equals(Range range) { 
			// Strictly the bounds arrays should be equal if they have the same items irrespective of order;
			// however to do that we'd have to find a better comparison algorithm.
			return range instanceof HasElementsMatching 
				&& bounds.equals(((HasElementsMatching)range).bounds); 
		}
		
		public Boolean maybeEquals(Range range) {
			//TODO
			return null;
		}

		public JsonObject toJSON() {
			//return this.toBoundsObject();
			return null;
		}	

		public Range bind(Map<Param,Value> parameters) {

			/*
			let bounds = Stream.from(this.bounds)
				.map(bound=>bound.bind(parameters))
				.filter(bound => bound !== null)
				.reduce((new_bounds,value) => 
					Stream.from(new_bounds).some(bound => 
						bound.contains(value)
					) ? new_bounds : (new_bounds.push(value), new_bounds),
					[]
				);

			if (bounds.length === 0) return null;
			if (this.bounds.length === bounds.length 
				&& this.bounds.every((bound,index) => bound === bounds[index])) return this;
			return new HasElementsMatching(bounds);
			*/
			return this;
		}
		
		public Range union(Range other) {
			// TODO: implement
			return null;
		}
	}



	/** Support a deferred intersection between parametrized ranges.
	 *
	 * @private 
	 */
	public static class Intersection extends Range {

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
			// TODO Auto-generated method stub
			return null;
		}	
	}
	
	public static class Union extends Range {

		private Range a;
		private Range b;
		
		public Union(Range a, Range b) {
			this.a = a;
			this.b = b;
		}
		
		@Override
		public Range union(Range other) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Boolean maybeEquals(Range other) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Boolean contains(Range range) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Boolean containsItem(Atomic item) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Range intersect(Range range) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public  <U> U toExpression(Formatter<U> formatter) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public JsonValue toJSON() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Range bind(Map<Param, Value> parameters) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}





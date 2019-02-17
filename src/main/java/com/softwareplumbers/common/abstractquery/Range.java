package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import com.softwareplumbers.common.abstractquery.Value.Atomic;
import com.softwareplumbers.common.abstractquery.Value.MapValue;
import com.softwareplumbers.common.abstractquery.Value.Type;
import com.softwareplumbers.common.abstractquery.formatter.Context;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;

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
	
	public static final RangeFactory FACTORY = new RangeFactory();
	
	public Range maybeUnion(Range other);
	public Range maybeIntersect(Range other);
	public Value.Type getType();
	
	public default boolean equals(Range other) {
		Boolean mightEqual = maybeEquals(other);
		return mightEqual == Boolean.TRUE;
	}
	
	public default Range union(Range other) {
		Range result = maybeUnion(other);
		if (result == null) result = Range.union(this, other);
		return result;
	}
	
	public default boolean isEmpty() {
		return false;
	}
	
	public default boolean isUnconstrained() {
		return false;
	}

	public default Range intersect(Range other) {
		Range result = maybeIntersect(other);
		if (result == null) result = Range.intersect(this, other);
		return result;
	}

	public default Range bind(String params) {
		return this.bind(Value.MapValue.fromJson(params));
	}
	
	public default Factory<Value.Atomic, Range> getFactory() {
		return FACTORY;
	}
	
	/** Object mapping of range operators to constructor functions
	 *
	 * | Operator String | Constructor Function 	|
	 * |-----------------|--------------------------|
	 * | ">"			 | Range.greaterThan 		|
	 * | "<"			 | Range.LessThan 			|
	 * | ">="			 | Range.greaterThanOrEqual |
	 * | "<="			 | Range.lessThanOrEqual 	|
	 * | "="			 | Range.equal 				|
	 * | "$like"         | Range.like               |
	 */
	static Range getRange(String operator, Value.Atomic value) {
		switch(operator) {
			case ">" : return Range.greaterThan(value);
			case "<" : return Range.lessThan(value);
			case ">=" : return Range.greaterThanOrEqual(value);
			case "<=" : return Range.lessThanOrEqual(value);
			case "=" : return Range.equals(value);
			case "$like" : return Range.like((String)value.value);
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
		case "$like" : return true;
		default: return false;
		}
	}		

	/** Create a range containing a single value 
	 *
	 * @param value - value to search for
	 * @return a Range object
	 */
	public static Range equals(Value.Atomic value) 				
	{ return new Equals(value); }

	/** Create a range containing values less than a given value 
	 *
	 * @param value - value to search for
	 * @return a Range object
	 */
	public static  Range lessThan(Value.Atomic value) 		
	{ return new LessThan(value); }

	/** Create a range containing values less than or equal to a given value 
	 * @param value - value to search for
	 * @return a Range object
	 */
	public static  Range lessThanOrEqual(Value.Atomic value) 		
	{ return new LessThanOrEqual(value); }

	/** Create a range containing values greater than a given value 
	 * @param value - value to search for
	 * @return a Range object
	 */		
	public static  Range greaterThan(Value.Atomic value) 	
	{ return new GreaterThan(value); }

	/** Create a range containing values greater than or equal to a given value 
	 * @param value - value to search for
	 * @return a Range object
	 */		
	public static  Range greaterThanOrEqual(Value.Atomic value)  	
	{ return new GreaterThanOrEqual(value); }

	/** Create a range containing values between the given values
	 *
	 * @param lower - lower range boundary (inclusive by default)
	 * @param upper - upper range boundary (exclusive by default)
	 * @return a Range object
	 */
	public static  Range between(Value.Atomic lower, Value.Atomic upper)	{ 

		Range lowerr = greaterThanOrEqual(lower);
		Range upperr = lessThan(upper);

		return lowerr.intersect(upperr);
	}
	
	/** Create a range matching a wildcard template.
	 * 
	 * The characters * and ? represent multi-character and single character wildcards.
	 * 
	 * @param template
	 * @return a Range object
	 */
	public static Range like(String template) {
		return new Like(template);
	}
	
	/**
	 * 
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static  Range between(Range lower, Range upper) {
		if (lower instanceof LessThanOrEqual) return Range.EMPTY;
		if (lower instanceof LessThan) return null;
		if (upper instanceof GreaterThanOrEqual) return null;
		if (upper instanceof GreaterThan) return null;
		if (lower instanceof OpenRange 
			&& upper instanceof OpenRange
			&& ((OpenRange)lower).value.lessThan(((OpenRange)upper).value) != Boolean.FALSE)
			return new Between((OpenRange)lower, (OpenRange)upper);
		return null;
	}

	/** Provide access to global Unbounded range
	 */
	public static final Range UNBOUNDED = new Unbounded();
	public static final Range EMPTY = new Empty();
	
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
				return operator.apply((Value.Atomic)Value.Atomic.from(asObj));
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
	 * @return a range, or undefined if paramters are not compatible.
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
	 * @return a range
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
		
		public boolean isEmpty() {
			return false;
		}
		
		public boolean isUnconstrained() {
			return true;
		}

		public Boolean contains(Range range) {
			return true;
		}

		public Boolean containsItem(Value.Atomic item) {
			return true;
		}

		public Boolean intersects(Range range) {
			return Boolean.TRUE;
		}
		
		public <U,V> U toExpression(Formatter<U,V> formatter, Context context)	{ 
			return formatter.operExpr(context, "=", Value.from("*")); 
		}

		public Boolean maybeEquals(Range range)	{ return range instanceof Unbounded; }
		
		public boolean equals(Object other) {
			return other instanceof Range && maybeEquals((Range)other) == Boolean.TRUE;
		}

		public JsonValue toJSON() {
			// TODO: fixme
			return null;
		}

		public Range bind(Value.MapValue parameters) {
			return this;
		}
		
		public Range maybeUnion(Range other) {
			return this;
		}
		
		public Range maybeIntersect(Range other) {
			return other;
		}

		public String toString() {
			return toExpression(Formatter.DEFAULT);
		}
		
		public Value.Type getType() {
			return null;
		}
	}
	
	/** Range representing an unbounded data set [i.e. no constraint on data returned]
	 *
	 * @private
	 */
	public static class Empty implements Range {

		public boolean isEmpty() {
			return true;
		}
		
		public boolean isUnconstrained() {
			return false;
		}
		
		public Boolean contains(Range range) {
			return Boolean.FALSE;
		}

		public Boolean containsItem(Value.Atomic item) {
			return Boolean.FALSE;
		}

		public Boolean intersects(Range range) {
			return Boolean.FALSE;
		}
		
		public <U,V> U toExpression(Formatter<U,V> formatter, Context context)	{ 
			return formatter.operExpr(context, "=", Value.from("[]")); 
		}

		public Boolean maybeEquals(Range range)	{ return range instanceof Empty; }
		
		public boolean equals(Object other) {
			return other instanceof Range && maybeEquals((Range)other) == Boolean.TRUE;
		}

		public JsonValue toJSON() {
			// TODO: fixme
			return null;
		}

		public Range bind(Value.MapValue parameters) {
			return this;
		}
		
		public Range maybeUnion(Range other) {
			return other;
		}
		
		public Range maybeIntersect(Range other) {
			return this;
		}

		public String toString() {
			return toExpression(Formatter.DEFAULT);
		}
		
		public Value.Type getType() {
			return null;
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

		public <U,V> U toExpression(Formatter<U,V> formatter, Context context)	{ 
			return formatter.operExpr(context, this.operator, this.value); 
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
		public Range bind(Value.MapValue parameters) {
			if (value.type == Value.Type.PARAM) {
				Param param = (Param)((Value.Atomic)this.value).value;
				//TODO: Ranges should have a type?
				if (parameters.hasProperty(param.name)) {
					Value.Atomic value = (Atomic) parameters.getProperty(param.name);
					return getRange(operator, value);
				}
			}
			return this;
		}
		
		public Value.Type getType() {
			return value.type;
		}
	}

	/** Range between two bounds.
	 *
	 * @private
	 */
	public static class Between implements Range {

		public final OpenRange lower_bound;
		public final OpenRange upper_bound;

		public Between(OpenRange lower_bound, OpenRange upper_bound) {
			this.lower_bound = lower_bound;
			this.upper_bound = upper_bound;
		}

		public Boolean contains(Range range) {
			if (range == UNBOUNDED) return Boolean.FALSE;
			if (range == EMPTY) return Boolean.FALSE;
			return Tristate.and(this.lower_bound.contains(range), this.upper_bound.contains(range));
		}

		public Boolean containsItem(Value.Atomic item) {
			return this.lower_bound.containsItem(item) && this.upper_bound.containsItem(item);
		}

		public Range maybeIntersect(Range range) {
			if (range == EMPTY) return EMPTY;
			if (range == UNBOUNDED) return this;
			if (range instanceof Between) {
				Between between = (Between)range;
				Range new_lower_bound = lower_bound.maybeIntersect(between.lower_bound);
				Range new_upper_bound = upper_bound.maybeIntersect(between.upper_bound);
				if (new_lower_bound == null || new_upper_bound == null) return null;
				return new_lower_bound.maybeIntersect(new_upper_bound);
			}
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				if (range.intersects(lower_bound) == Boolean.FALSE) return EMPTY;
				Range new_upper_bound = range.maybeIntersect(upper_bound);
				return (new_upper_bound == null) ? null : new Between(lower_bound, (OpenRange)new_upper_bound);
			}
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				if (range.intersects(upper_bound) == Boolean.FALSE) return EMPTY;
				Range new_lower_bound = range.maybeIntersect(lower_bound);
				return (new_lower_bound == null) ? null : new Between((OpenRange)new_lower_bound, upper_bound);
			}
			if (range instanceof Like) {
				Like like = (Like)range;
				if (intersects(like.bounds) == Boolean.FALSE) return EMPTY; 
				if (contains(like.bounds) == Boolean.TRUE) return like;
			}
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
			if (range instanceof RangeIntersection) { 
				return Tristate.and(
					range.intersects(lower_bound), 
					range.intersects(upper_bound)
				);
			}
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return upper_bound.intersects(range);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual)
				return lower_bound.intersects(range);
			if (range instanceof Like)
				return range.intersects(this);
			return null;
		}

		public <U,V> U toExpression(Formatter<U,V> formatter, Context context)	{ 
			return formatter.betweenExpr(context, getType(), lower_bound.toExpression(formatter, context), upper_bound.toExpression(formatter, context));
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

		public Range bind(Value.MapValue parameters) {
			Range new_lower_bound = lower_bound.bind(parameters);
			Range new_upper_bound = upper_bound.bind(parameters);
			if (lower_bound == new_lower_bound && upper_bound == new_upper_bound) return this;
			return new_lower_bound.intersect(new_upper_bound);
		}

		@Override
		public Range maybeUnion(Range range) {
			if (range == UNBOUNDED) return UNBOUNDED;
			if (range == EMPTY) return this;
			if (range instanceof Between) {
				Between between = (Between)range;
				if (Tristate.and(
						lower_bound.intersects(between.upper_bound), 
						upper_bound.intersects(between.lower_bound)) == Boolean.TRUE) {
					Range new_lower_bound = lower_bound.maybeUnion(between.lower_bound);
					Range new_upper_bound = upper_bound.maybeUnion(between.upper_bound);
					if (new_lower_bound == null || new_upper_bound == null) return null;
					return new_lower_bound.maybeIntersect(new_upper_bound);
				}
			}
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Range new_upper_bound = this.upper_bound.maybeUnion(range);
				if (new_upper_bound == null) return null;
				return new_upper_bound.maybeIntersect(this.lower_bound);
			}
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Range new_lower_bound = this.lower_bound.maybeUnion(range);
				if (new_lower_bound == null) return null;
				return new_lower_bound.maybeIntersect(this.upper_bound);
			} 
			if (range instanceof Like) {
				if (contains(range) == Boolean.TRUE) return this;
			}
			return null;

		}
		
		public Value.Type getType() {
			if (lower_bound.getType() != null && lower_bound.getType() != Value.Type.PARAM) return lower_bound.getType();
			return upper_bound.getType();
		}
	}

	/** Range exactly equal to some value
	 * 
	 */
	public static class Equals implements Range {
		
		public static final String OPERATOR = "=";

		Value.Atomic value;

		public Equals(Value.Atomic value) {
			this.value = value;
		}

		public Boolean contains(Range range) {
			if (range == UNBOUNDED) return Boolean.FALSE;
			if (range == EMPTY) return Boolean.FALSE;
			return this.maybeEquals(range);
		}


		public Boolean containsItem(Value.Atomic item) {
			return value.maybeEquals(item);
		}

		public Range maybeIntersect(Range range) {
			if (range == EMPTY) return EMPTY;
			if (range == UNBOUNDED) return this;
			Boolean result = range.containsItem(value);
			if (result == null) return null; 
			return (result ? this : EMPTY);
		}
		
		public Boolean intersects(Range range) {
			return range.containsItem(value);
		}
		
		public Range maybeUnion(Range range) {
			if (range == UNBOUNDED) return this;
			if (range == EMPTY) return EMPTY;
			if (range.containsItem(value) == Boolean.TRUE) return range;
			return null;
		}

		public <U,V> U toExpression(Formatter<U,V> formatter, Context context)	{ 
			return formatter.operExpr(context, OPERATOR, this.value); 
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

		public Range bind(Value.MapValue parameters) {
			if (value.type == Value.Type.PARAM) {
				Param param = (Param)((Value.Atomic)value).value;
				Value.Atomic new_value = (Value.Atomic)parameters.getProperty(param.name);
				if (new_value != null) return new Equals(new_value);
			}
			return this;	
		}
		
		public Value.Type getType() { return value.type; }
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
			if (range == UNBOUNDED) return Boolean.FALSE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Equals)
				return ((Equals)range).value.lessThan(value);
			if (range instanceof LessThanOrEqual) 
				return ((LessThanOrEqual)range).value.lessThan(value);
			if (range instanceof LessThan) 
				return ((LessThan)range).value.lessThanOrEqual(value);
			if (range instanceof Between) 
				return contains(((Between)range).upper_bound);
			if (range instanceof RangeIntersection) 
				return ((RangeIntersection)range).containedBy(this);
			if (range instanceof RangeUnion) 
				return ((RangeUnion)range).containedBy(this);
			if (range instanceof Like)
				return ((Like)range).containedBy(this);
				
			return false; 
		}

		public Boolean containsItem(Value.Atomic item) {
			return item.lessThan(this.value);
		}
		
		public Boolean intersects(Range range) {
			if (range == UNBOUNDED) return Boolean.TRUE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Between || range instanceof RangeIntersection || range instanceof RangeUnion)
				return range.intersects(this);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.lessThan(value);
			if (range instanceof Equals) 
				return ((Equals)range).value.lessThan(value);
			if (range instanceof Like)
				return range.intersects(this);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}

		public Range maybeIntersect(Range range) {
			if (range == EMPTY) return EMPTY;
			if (range == UNBOUNDED) return this;
			
			if (range instanceof Between)
				return range.maybeIntersect(this);
			
			// a < x && a < y  -> a < x if x <= y, a < y otherwise
			// a < x && a <= y -> a < x if x <= y, a <= y otherwise 
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {			
				Boolean result = value.lessThanOrEqual(((OpenRange)range).value);
				if (result == null) return null;
				return result ? this : range;
			}

			// a < x && a > y -> y<a<x if y < x, null otherwise	
			// a < x && a >= y -> y<=a<x if y < x, null otherwise	
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Boolean result = ((OpenRange)range).value.lessThan(value);
				if (result == Boolean.FALSE) return Range.EMPTY;
				return new Between((OpenRange)range, this);
			}

			// a < x && a = y -> a = y if y < x; null otherwise
			if (range instanceof Equals) {
				Boolean result = ((Equals)range).value.lessThan(value);
				if (result == null) return null;
				return result ? range : Range.EMPTY;
			}
			
			if (range instanceof Like) {
				return range.maybeIntersect(this);
			}
			
			return null;
		}
		
		public Range maybeUnion(Range range) {
			if (range == EMPTY) return this;
			if (range == UNBOUNDED) return UNBOUNDED;
			if (range instanceof Equals)
				return (containsItem(((Equals)range).value) == Boolean.TRUE) ? this : null;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return (intersects(range) == Boolean.TRUE) ? UNBOUNDED : null;
			if (range instanceof Between) 
				return (range.intersects(this) == Boolean.TRUE) ? maybeUnion(((Between)range).upper_bound) : null;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Boolean result = value.greaterThan(((OpenRange)range).value);
				if (result == null) return null;
				return result ? this : range;
			}
			if (range instanceof Like)
				return range.maybeUnion(this);
			return null;
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
			if (range == UNBOUNDED) return Boolean.FALSE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Equals)
				return ((Equals)range).value.lessThanOrEqual(this.value);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return ((OpenRange)range).value.lessThanOrEqual(this.value);
			if (range instanceof Between) 
				return contains(((Between)range).upper_bound);
			if (range instanceof RangeIntersection) 
				return ((RangeIntersection)range).containedBy(this);
			if (range instanceof RangeUnion) 
				return ((RangeUnion)range).containedBy(this);
			if (range instanceof Like)
				return ((Like)range).containedBy(this);

			return false;
		}

		public Boolean containsItem(Value.Atomic item) {
			return item.lessThanOrEqual(this.value);
		}

		public Boolean intersects(Range range) {
			if (range == UNBOUNDED) return Boolean.TRUE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Equals) 
				return ((Equals)range).value.lessThanOrEqual(value);
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.lessThanOrEqual(value);
			if (range instanceof Between || range instanceof RangeIntersection || range instanceof RangeUnion)
				return range.intersects(this);
			if (range instanceof Like)
				return range.intersects(this);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}
		
		public Range maybeIntersect(Range range) {
			
			if (range == EMPTY) return EMPTY;
			if (range == UNBOUNDED) return this;

			// Complex ranges always handled by their own class
			if (range instanceof Between)
				return range.maybeIntersect(this);

			// a <= x && a < y  -> a <= x if x < y, a < y otherwise
			// a <= x && a <= y -> a <= x if x < y, a <= y otherwise 
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Boolean result = value.lessThanOrEqual(((OpenRange)range).value);
				if (result == null) return null;
				return result ? range : EMPTY;
			}

			// a <= x && a > y -> y<a<=x if y < x, null otherwise	
			if (range instanceof GreaterThan) {
				Boolean result = ((OpenRange)range).value.lessThan(value);
				if (result == null) return null;
				return result ? new Between((OpenRange)range, this) : EMPTY;
			}

			// a <= x && a >= y -> y<=a<=x if y < x, a = x if y = x, null otherwise	
			if (range instanceof GreaterThanOrEqual) {
				if (((OpenRange)range).value.maybeEquals(value) == Boolean.TRUE)
					return new Equals(value);
				Boolean result = ((OpenRange)range).value.lessThan(this.value);
				if (result == Boolean.FALSE) return Range.EMPTY;
				return new Between((OpenRange)range, this);
			}

			// a <= x && a = y -> a = y if y <= x; null otherwise
			if (range instanceof Equals) {
				Boolean result = ((Equals)range).value.lessThanOrEqual(this.value);
				if (result == null) return null;
				return result ? range : EMPTY;
			}
			
			if (range instanceof Like) {
				return range.maybeIntersect(this);
			}

			return null;
		}
		
		
		public Range maybeUnion(Range range) {
			if (range == EMPTY) return this;
			if (range == UNBOUNDED) return UNBOUNDED;
			if (range instanceof Equals)
				return (containsItem(((Equals)range).value) == Boolean.TRUE) ? this : null;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return (intersects(range) == Boolean.TRUE) ? UNBOUNDED : null;
			if (range instanceof Between)
				return (range.intersects(this) == Boolean.TRUE) ? maybeUnion(((Between)range).upper_bound) : null;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Boolean result = value.greaterThanOrEqual(((OpenRange)range).value);
				if (result == null) return null;
				return result ? this : range;
			}

			if (range instanceof Like)
				return range.maybeUnion(this);
			
			return null;
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
			if (range == UNBOUNDED) return Boolean.FALSE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Equals)
				return ((Equals)range).value.greaterThan(this.value);
			if (range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.greaterThan(this.value);
			if (range instanceof GreaterThan) 
				return ((OpenRange)range).value.greaterThanOrEqual(this.value);
			if (range instanceof Between) 
				return this.contains(((Between)range).lower_bound);
			if (range instanceof RangeIntersection) 
				return ((RangeIntersection)range).containedBy(this);
			if (range instanceof RangeUnion) 
				return ((RangeUnion)range).containedBy(this);
			if (range instanceof Like)
				return ((Like)range).containedBy(this);

			return false;
		}

		public Boolean containsItem(Value.Atomic item) {
			return item.greaterThan(this.value);
		}
		
		public Boolean intersects(Range range) {
			if (range == UNBOUNDED) return Boolean.TRUE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Between || range instanceof RangeIntersection || range instanceof RangeUnion)
				return range.intersects(this);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return ((OpenRange)range).value.greaterThan(value);
			if (range instanceof Equals) 
				return ((Equals)range).value.greaterThan(value);
			if (range instanceof Like)
				return range.intersects(this);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}

		public Range maybeIntersect(Range range) {
			if (range == EMPTY) return EMPTY;
			if (range == UNBOUNDED) return this;

			// Complex ranges are directly handled by their own class.
			if (range instanceof Between)
				return range.maybeIntersect(this);

			// a > x && a > y  -> a > x if x >= y, a > y otherwise
			// a > x && a >= y -> a < x if x >= y, a >= y otherwise 
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Boolean result = (this.value.greaterThanOrEqual(((OpenRange)range).value));
				if (result == null) return null;
				return result ? this : range;
			}

			// a > x && a < y -> x<a<y if x < y, null otherwise	
			// a > x && a <= y -> x<a<=y if x < y, null otherwise	
			if (range instanceof LessThan || range instanceof LessThanOrEqual) {
				Boolean result = (this.value.lessThan(((OpenRange)range).value));
				if (result == Boolean.FALSE) return Range.EMPTY;
				return new Between(this, (OpenRange)range);
			}

			// a > x && a = y -> a = y if y > x; null otherwise
			if (range instanceof Equals) {
				Boolean result = (((Equals)range).value.greaterThan(value));
				if (result == null) return null;
				return result ? range : EMPTY;
			}
			
			if (range instanceof Like) {
				return range.maybeIntersect(this);
			}

			return null;
		}


		public Range maybeUnion(Range range) {
			if (range == EMPTY) return this;
			if (range == UNBOUNDED) return UNBOUNDED;
			if (range instanceof Equals)
				return (containsItem(((Equals)range).value) == Boolean.TRUE) ? this : null;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return (intersects(range) == Boolean.TRUE) ? UNBOUNDED : null;
			if (range instanceof Between)
				return (range.intersects(this) == Boolean.TRUE) ? maybeUnion(((Between)range).upper_bound) : null;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Boolean result = value.lessThan(((OpenRange)range).value);
				if (result == null) return null;
				return result ? this : range;
			}
			if (range instanceof Like)
				return range.maybeUnion(this);
	
			return null;
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
			if (range == UNBOUNDED) return Boolean.FALSE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Equals)
				return ((Equals)range).value.greaterThanOrEqual(this.value);
			if (range instanceof GreaterThan  || range instanceof GreaterThanOrEqual) 
				return ((OpenRange)range).value.greaterThanOrEqual(this.value);
			if (range instanceof Between) 
				return this.contains(((Between)range).lower_bound);
			if (range instanceof RangeIntersection) 
				return ((RangeIntersection)range).containedBy(this);
			if (range instanceof RangeUnion) 
				return ((RangeUnion)range).containedBy(this);
			if (range instanceof Like)
				return ((Like)range).containedBy(this);
			return false;
		}

		public Boolean containsItem(Value.Atomic item) {
			return item.greaterThanOrEqual(this.value);
		}

		public Boolean intersects(Range range) {
			if (range == UNBOUNDED) return Boolean.TRUE;
			if (range == EMPTY) return Boolean.FALSE;
			if (range instanceof Between || range instanceof RangeIntersection || range instanceof RangeUnion)
				return range.intersects(this);
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) 
				return Boolean.TRUE;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return ((OpenRange)range).value.greaterThanOrEqual(value);
			if (range instanceof Equals) 
				return ((Equals)range).value.greaterThanOrEqual(value);
			if (range instanceof Like)
				return range.intersects(this);

			throw new IllegalArgumentException("Uknown range type: " + range);
		}
		
		public Range maybeIntersect(Range range) {

			if (range == EMPTY) return EMPTY;
			if (range == UNBOUNDED) return this;

			// Complex ranges are directly handled by their own class.
			if (range instanceof Between)
				return range.maybeIntersect(this);

			// a >= x && a > y  -> a >= x if x > y, a > y otherwise
			// a >= x && a >= y -> a >= x if x > y, a >= y otherwise 
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Boolean result = this.value.greaterThanOrEqual(((OpenRange)range).value);
				if (result == null) return null;
				return result ?this:range;
			}

			// a >= x && a < y -> x<=a<y if y > x, null otherwise	
			if (range instanceof LessThan) {
				Boolean result = (((OpenRange)range).value.greaterThan(this.value));
				if (result == null) return null;
				return result ? new Between(this,(OpenRange)range) : EMPTY;
			}

			// a >= x && a <= y -> x<=a<=y if y > x, a = x if y = x, null otherwise	
			if (range instanceof LessThanOrEqual) {
				if (((OpenRange)range).value.maybeEquals(this.value) == Boolean.TRUE)
					return new Equals(value);
				Boolean result = ((OpenRange)range).value.greaterThan(this.value);
				if (result == Boolean.FALSE) return Range.EMPTY;
				return new Between(this, (OpenRange)range);

			}

			// a >= x && a = y -> a = y if y >= x; null otherwise
			if (range instanceof Equals) {
				Boolean result = (((Equals)range).value.greaterThanOrEqual(this.value)); 
				if (result == null) return null;
				return result ? range : EMPTY;
			}

			if (range instanceof Like) {
				return range.maybeIntersect(this);
			}

			return null;
		}

		public Range maybeUnion(Range range) {
			if (range == EMPTY) return this;
			if (range == UNBOUNDED) return UNBOUNDED;
			if (range instanceof Equals)
				return (containsItem(((Equals)range).value) == Boolean.TRUE) ? this : null;
			if (range instanceof LessThan || range instanceof LessThanOrEqual) 
				return (intersects(range) == Boolean.TRUE) ? UNBOUNDED : null;
			if (range instanceof Between)
				return (range.intersects(this) == Boolean.TRUE) ? maybeUnion(((Between)range).upper_bound) : null;
			if (range instanceof GreaterThan || range instanceof GreaterThanOrEqual) {
				Boolean result = value.lessThanOrEqual(((OpenRange)range).value);
				if (result == null) return null;
				return result ? this : range;
			}
			if (range instanceof Like)
				return range.maybeUnion(this);
	
			return null;
		}
	}
		
	
	public static Value.Type getType(Collection<Range> range) {
		Predicate<Value.Type> useful_type = type -> (type != Value.Type.PARAM && type != null);
		return range.stream().map(Range::getType).filter(useful_type).findAny().orElse(null);
	}
		
	public static class RangeUnion extends Union<Value.Atomic, Range> implements Range   {
		public RangeUnion(List<Range> range) {
			super(Range.getType(range), range);
		}
		
		public Range maybeUnion(Range other) {
			return null;
		}
		
		public Range maybeIntersect(Range other) {
			return null;
		}
		
		public Boolean containedBy(Range other) {
			return Tristate.every(data, item->other.contains(item));
		}
	}
	
	public static class RangeIntersection extends Intersection<Value.Atomic, Range> implements Range {

		
		public RangeIntersection(List<Range> range) {
			super(Range.getType(range), range);
		}
		
		@Override
		public Range maybeUnion(Range other) {
			return null;
		}

		@Override
		public Range maybeIntersect(Range other) {
			return null;
		}
		
		public Boolean containedBy(Range other) {
			return Tristate.any(data, item->other.contains(item));
		}
	}
	

	
	public static class Like implements Range {
		
		private static final String REGEX_MULTI=".*";
		private static final String REGEX_SINGLE=".";
		public static final String OPERATOR="like";
		
		private static String nextSeq(String string) {
			int end = string.length() - 1;
			StringBuffer buf = new StringBuffer(string);
			buf.setCharAt(end, (char)(string.charAt(end) + 1));
			return buf.toString();
		}
		
		private Range bounds;
		private Pattern pattern;
		private String template;
		
		public Like(String template) {
			this.template = template;
			int first_star = template.indexOf("*");
			int first_questionmark = template.indexOf("?");
			int first_wildcard = -1;
			
			if (first_star >= 0 && first_questionmark >= 0) 
				first_wildcard = Math.min(first_star, first_questionmark);
			else
				first_wildcard = Math.max(first_star, first_questionmark);
			
			if (first_wildcard == 0) {
				this.bounds = UNBOUNDED;
			} else {
				String lower_bound = template.substring(0, first_wildcard);
				String upper_bound = nextSeq(lower_bound);
				this.bounds = between(Value.from(lower_bound),Value.from(upper_bound));
			} 
			
			String regex = template.replace("*", REGEX_MULTI);
			regex = regex.replace("?", REGEX_SINGLE);
			this.pattern = Pattern.compile(regex);
		}

		@Override
		public Boolean intersects(Range other) {
			if (other.contains(bounds) == Boolean.TRUE) return Boolean.TRUE;
			if (other.intersects(bounds) == Boolean.FALSE) return Boolean.FALSE;
			return null;
		}

		@Override
		public Boolean containsItem(Atomic item) {
			if (item.type == Value.Type.STRING) {
				Matcher matcher = pattern.matcher((String)item.value);
				return matcher.matches();
			}
			if (item.type == Value.Type.PARAM) {
				return null;
			}
			return Boolean.FALSE;
		}

		@Override
		public Boolean contains(Range other) {
			if (bounds.contains(other) == Boolean.FALSE) return Boolean.FALSE;
			return null;
		}
		
		public Boolean containedBy(Range other) {
			if (other.contains(bounds) == Boolean.TRUE) return Boolean.TRUE;
			if (other.intersects(bounds) == Boolean.FALSE) return Boolean.FALSE;
			return null;
		}

		@Override
		public Boolean maybeEquals(Range other) {
			return other instanceof Like && template.equals(((Like)other).template);
		}

		@Override
		public <X, V> X toExpression(Formatter<X, V> formatter, Context context) {
			return formatter.operExpr(context, OPERATOR, Value.from(this.template)); 
		}

		@Override
		public JsonValue toJSON() {
			return Json.createObjectBuilder().add("$like", template).build();
		}

		@Override
		public Range bind(MapValue values) {
			return this;
		}

		@Override
		public Range maybeUnion(Range other) {
			if (other.contains(bounds)) return other;
			if (other.isEmpty()) return this;
			return null;
		}

		@Override
		public Range maybeIntersect(Range other) {
			if (other.contains(bounds)) return this;
			if (other.isEmpty()) return other;
			return null;
		}

		@Override
		public Type getType() {
			return Value.Type.STRING;
		}
		
	}
	
	public static Range union(List<Range> list) {
		return FACTORY.union(list);
	}
	
	public static Range union(Range... list) {
		return FACTORY.union(list);
	}
	
	public static Range intersect(Range... list) {
		return FACTORY.intersect(list);
	}
	
	public static Range intersect(List<Range> list) {
		return FACTORY.intersect(list);
	}
}






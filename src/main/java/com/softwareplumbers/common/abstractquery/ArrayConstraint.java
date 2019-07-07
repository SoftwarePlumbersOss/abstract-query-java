package com.softwareplumbers.common.abstractquery;



import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.formatter.Context;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;
import javax.json.JsonValue.ValueType;

/** Expresses a constraint on an array 
*
* @param <V> Value type of the array
* @param <S> The type of the given constraint
*/
public interface ArrayConstraint<V extends JsonValue, S extends AbstractSet<V,S>> extends AbstractSet<JsonArray, ArrayConstraint<V,S>> {

	public static final String OPERATOR = "has";
	
	//public S getMatch();
	
	@Override
	public default Factory<JsonArray, ArrayConstraint<V, S>> getFactory() {
		return new ArrayConstraintFactory<V,S>();
	}
	
	public static class Has <V extends JsonValue, S extends AbstractSet<V,S>> implements ArrayConstraint<V,S> {
	
	private final S match;

	public Has(S match) {
		this.match = match;
	}
	
	public S getMatch() {
		return match;
	}

	// For containment, this range must match all array elements matched by the other range. Otherwise, the other range
	// could match an element not matched by this range, which implies this range does not contain the other.
	//
	// If all bounds defined in the other range are contained by at least one bound in this range, this condition
	// is met.
	public Boolean contains(ArrayConstraint<V,S> range) {
		if (range instanceof Has)
		return match.contains(((Has<V,S>)range).match);
		return null;
	}

	// For every bound, there is some element in item that matches that bound.
	public Boolean containsItem(JsonArray item) {
		return Tristate.any(item.stream(), element -> match.containsItem((V)element)); 
	}

	// hmm, remember that $and : [ { y : { $has : 'numpty' } }, { y : { $has : 'flash' } } ] is not the same as 
	// { y : { $has : Range.and(Range.equals('numpty'), range.equals('flash')) }. The former should match any element
	// with y containing both numpty and flash. The second will match nothing since no array element will equal both
	// numpty and flash.
	public ArrayConstraint<V,S> intersect(ArrayConstraint<V,S> other) {
		return ArrayConstraint.intersect(this, other);
	}

	public <U,X> U toExpression(Formatter<U,X> formatter, Context context) { 
		Context array = context.setType(Context.Type.ARRAY);
		return formatter.subExpr(context, OPERATOR, match.toExpression(formatter, array));
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object other) { 
		return other instanceof ArrayConstraint 
			&& maybeEquals((ArrayConstraint<V,S>)other) == Boolean.TRUE ; 
	}
	
	public Boolean maybeEquals(ArrayConstraint<V,S> other) {
		if (other instanceof Has)
		return match.maybeEquals(((Has<V,S>)other).match);
		return null;
	}

	public JsonObject toJSON() {
		//return this.toBoundsObject();
		return null;
	}	

	public ArrayConstraint<V,S> bind(JsonObject parameters) {
		return new Has<V,S>(match.bind(parameters));
	}
	
	public ArrayConstraint<V,S> union(ArrayConstraint<V,S> other) {
		if (other instanceof Has)
		return new Has<V,S>(match.union(((Has<V,S>)other).getMatch()));
		throw null;
	}

	@Override
	public Boolean intersects(ArrayConstraint<V,S> other) {
		if (other instanceof ArrayConstraintIntersection) {
			return other.intersects(this);
		} else if (other instanceof Has) {
			Boolean result = match.intersects(((Has<V,S>)other).getMatch());
			if (result == Boolean.TRUE) return true;
			// if no intersection, we just can't tell if there's an intersection in the matches.
		}
		return null;
	}
	

	
	public boolean isEmpty() {
		return match.isEmpty();
	}
	
	public boolean isUnconstrained() {
		return match.isUnconstrained();
	}
	}
	
	public class ArrayConstraintIntersection<V extends JsonValue, S extends AbstractSet<V,S>> extends Intersection<JsonArray, ArrayConstraint<V,S>> implements ArrayConstraint<V,S> {

		public ArrayConstraintIntersection(List<ArrayConstraint<V, S>> data) {
			super(ValueType.ARRAY, data);
		}
	}
	
	public static <V extends JsonValue, S extends AbstractSet<V,S>> ArrayConstraint<V, S> match(S match) {
		return new Has<V,S>(match);
	}
	
	public static ArrayConstraint<JsonValue, Range> matchAny(Range... matches) {
		return new Has<JsonValue, Range>(Range.union(matches));
	}
	
	public static ArrayConstraint<JsonValue, Range> matchRanges(JsonValue matches) {
		Range constraint;
		if (matches instanceof JsonArray) {
			constraint = Range.union(((JsonArray) matches).stream().map(value -> Range.from(value)).collect(Collectors.toList()));
		} else {
			constraint = Range.from(matches);
		}
		return match(constraint);
	}
	
	public static ArrayConstraint<JsonObject, Query> matchCubes(JsonValue matches) {
		Query constraint;
		if (matches instanceof JsonArray) {
			constraint = Query.union(((JsonArray) matches).stream().map(value -> Query.from((JsonObject)value)).collect(Collectors.toList()));
		} else {
			constraint = Query.from((JsonObject)matches);
		}
		return match(constraint);
	}
	
	public static ArrayConstraint<?,?> match(JsonValue matches) {
		if (matches instanceof JsonArray) {
			JsonArray array = (JsonArray) matches;
			if (array.size() > 0) {
				if (Range.isRange(array.get(0))) {
					Range constraint = Range.union(array.stream().map(value -> Range.from(value)).collect(Collectors.toList()));
					return match(constraint);
				} else {
					Query constraint = Query.union(array.stream().map(value -> Query.from((JsonObject)value)).collect(Collectors.toList()));
					return match(constraint);
				}
			} else {
				throw new RuntimeException("match cannot be a zero-sized array");
			}
		} else {
			if (Range.isRange(matches)) {
				return match(Range.from(matches));
			} else {
				return match(Query.from((JsonObject)matches));
			}
		}

	}
	
	public static  <V extends JsonValue, S extends AbstractSet<V,S>> ArrayConstraint<V,S> intersect(List<ArrayConstraint<V,S>> items) {
		return new ArrayConstraintFactory<V,S>().intersect(items);
	}
	
	public static <V extends JsonValue, S extends AbstractSet<V,S>> ArrayConstraint<V,S> intersect(ArrayConstraint<V,S>... items) {
		return new ArrayConstraintFactory<V,S>().intersect(items);
	}
	
	public static <V extends JsonValue, S extends AbstractSet<V,S>> ArrayConstraint<V,S> union(List<ArrayConstraint<V,S>> items) {
		return new ArrayConstraintFactory<V,S>().union(items);		
	}
	
}
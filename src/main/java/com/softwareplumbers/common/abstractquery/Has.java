package com.softwareplumbers.common.abstractquery;

import java.util.Map;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/** Range has element within some bound.
*
* @private
*/
public class Has<V extends Value, S extends AbstractSet<V,S>> implements AbstractSet<Value.ArrayValue, Has<V,S>> {

	public static final String OPERATOR = "has";
	
	private final S match;

	public Has(S match) {
		this.match = match;
	}

	// For containment, this range must match all array elements matched by the other range. Otherwise, the other range
	// could match an element not matched by this range, which implies this range does not contain the other.
	//
	// If all bounds defined in the other range are contained by at least one bound in this range, this condition
	// is met.
	public Boolean contains(Has<V,S> range) {
		return match.contains(range.match);
	}

	// For every bound, there is some element in item that matches that bound.
	public Boolean containsItem(Value.ArrayValue item) {
		return Tristate.any(item.stream(), element -> match.containsItem((V)element)); 
	}

	// hmm, remember that $and : [ { y : { $has : 'numpty' } }, { y : { $has : 'flash' } } ] is not the same as 
	// { y : { $has : Range.and(Range.equals('numpty'), range.equals('flash')) }. The former should match any element
	// with y containing both numpty and flash. The second will match nothing since no array element will equal both
	// numpty and flash.
	public Has<V,S> intersect(Has<V,S> other) {
		return new Has<V,S>(match.intersect(other.match));
	}

	public <U> U toExpression(Formatter<U> formatter) { 
		return formatter.subExpr(OPERATOR, match.toExpression(formatter));
	}

	public boolean equals(Object other) { 
		return other instanceof Has 
			&& maybeEquals((Has<V,S>)other) == Boolean.TRUE ; 
	}
	
	public Boolean maybeEquals(Has<V,S> other) {
		return match.maybeEquals(other.match);
	}

	public JsonObject toJSON() {
		//return this.toBoundsObject();
		return null;
	}	

	public Has<V,S> bind(Value.MapValue parameters) {
		return new Has<V,S>(match.bind(parameters));
	}
	
	public Has<V,S> union(Has<V,S> other) {
		return new Has<V,S>(match.union(other.match));
	}

	@Override
	public Boolean intersects(Has<V,S> other) {
		return match.intersects(other.match);
	}
	
	public static <V extends Value, S extends AbstractSet<V,S>> Has<V, S> match(S match) {
		return new Has<V,S>(match);
	}
	
	public static Has<Value.Atomic, Range> matchAny(Range... matches) {
		return new Has<Value.Atomic, Range>(Range.from(matches));
	}
	
	public static Has<Value.Atomic, Range> matchRanges(JsonValue matches) {
		Range constraint;
		if (matches instanceof JsonArray) {
			constraint = Range.from(((JsonArray) matches).stream().map(value -> Range.from(value)).collect(Collectors.toList()));
		} else {
			constraint = Range.from(matches);
		}
		return match(constraint);
	}
	
}
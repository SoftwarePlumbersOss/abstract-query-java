package com.softwareplumbers.common.abstractquery;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.Value.Type;
import com.softwareplumbers.common.abstractquery.formatter.Context;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;

/** Range has element within some bound.
*
* @private
*/
public interface Has<V extends Value, S extends AbstractSet<V,S>> extends AbstractSet<Value.ArrayValue, Has<V,S>> {

	public static final String OPERATOR = "has";
	
	public S getMatch();
	
	public static class Impl <V extends Value, S extends AbstractSet<V,S>> implements Has<V,S> {
	
	private final S match;

	public Impl(S match) {
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
	public Boolean contains(Has<V,S> range) {
		return match.contains(range.getMatch());
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
		return Has.intersect(this, other);
	}

	public <U> U toExpression(Formatter<U> formatter, Context context) { 
		Context array = context.setType(Context.Type.ARRAY);
		return formatter.subExpr(context, OPERATOR, match.toExpression(formatter, array));
	}

	public boolean equals(Object other) { 
		return other instanceof Has 
			&& maybeEquals((Has<V,S>)other) == Boolean.TRUE ; 
	}
	
	public Boolean maybeEquals(Has<V,S> other) {
		return match.maybeEquals(other.getMatch());
	}

	public JsonObject toJSON() {
		//return this.toBoundsObject();
		return null;
	}	

	public Has<V,S> bind(Value.MapValue parameters) {
		return new Impl<V,S>(match.bind(parameters));
	}
	
	public Has<V,S> union(Has<V,S> other) {
		return new Impl<V,S>(match.union(other.getMatch()));
	}

	@Override
	public Boolean intersects(Has<V,S> other) {
		if (other instanceof HasIntersection) {
			return other.intersects(this);
		} else  {
			Boolean result = match.intersects(other.getMatch());
			if (result == Boolean.TRUE) return true;
			// if no intersection, we just can't tell if there's an intersection in the matches.
			return null;
		}
	}
	

	
	public boolean isEmpty() {
		return match.isEmpty();
	}
	
	public boolean isUnconstrained() {
		return match.isUnconstrained();
	}
	}
	
	public class HasIntersection<V extends Value, S extends AbstractSet<V,S>> extends Intersection<Value.ArrayValue, Has<V,S>> implements Has<V,S> {

		public HasIntersection(List<Has<V, S>> data, Function<List<Has<V, S>>, Has<V, S>> union,
				Function<List<Has<V, S>>, Has<V, S>> intersection) {
			super(Value.Type.ARRAY, data, union, intersection);
		}

		@Override
		public S getMatch() {
			return null;
		}
		
	}
	
	public static <V extends Value, S extends AbstractSet<V,S>> Has<V, S> match(S match) {
		return new Impl<V,S>(match);
	}
	
	public static Has<Value.Atomic, Range> matchAny(Range... matches) {
		return new Impl<Value.Atomic, Range>(Range.union(matches));
	}
	
	public static Has<Value.Atomic, Range> matchRanges(JsonValue matches) {
		Range constraint;
		if (matches instanceof JsonArray) {
			constraint = Range.union(((JsonArray) matches).stream().map(value -> Range.from(value)).collect(Collectors.toList()));
		} else {
			constraint = Range.from(matches);
		}
		return match(constraint);
	}
	
	public static Has<Value.MapValue, Cube> matchCubes(JsonValue matches) {
		Cube constraint;
		if (matches instanceof JsonArray) {
			constraint = Cube.union(((JsonArray) matches).stream().map(value -> Cube.from((JsonObject)value)).collect(Collectors.toList()));
		} else {
			constraint = Cube.from((JsonObject)matches);
		}
		return match(constraint);
	}
	
	public static Has<?,?> match(JsonValue matches) {
		if (matches instanceof JsonArray) {
			JsonArray array = (JsonArray) matches;
			if (array.size() > 0) {
				if (Range.isRange(array.get(0))) {
					Range constraint = Range.union(array.stream().map(value -> Range.from(value)).collect(Collectors.toList()));
					return match(constraint);
				} else {
					Cube constraint = Cube.union(array.stream().map(value -> Cube.from((JsonObject)value)).collect(Collectors.toList()));
					return match(constraint);
				}
			} else {
				throw new RuntimeException("match cannot be a zero-sized array");
			}
		} else {
			if (Range.isRange(matches)) {
				return match(Range.from(matches));
			} else {
				return match(Cube.from((JsonObject)matches));
			}
		}

	}
	
	public static  <V extends Value, S extends AbstractSet<V,S>> Has<V,S> intersect(List<Has<V,S>> items) {
		return new HasIntersection<V,S>(items, Has::union, Has::intersect);
	}
	
	public static <V extends Value, S extends AbstractSet<V,S>> Has<V,S> intersect(Has<V,S>... items) {
		return intersect(Arrays.asList(items));
	}
	
	public static <V extends Value, S extends AbstractSet<V,S>> Has<V,S> union(List<Has<V,S>> items) {
		Iterator<Has<V,S>> i = items.iterator();
		Has<V,S> result = i.next();
		while (i.hasNext()) result = result.union(i.next());
		return result;
	}
	
}
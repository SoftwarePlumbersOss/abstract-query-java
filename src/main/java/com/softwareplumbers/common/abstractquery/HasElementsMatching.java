package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import com.softwareplumbers.common.abstractquery.Value.Atomic;

/** Range has element within some bound.
*
* @private
*/
public class HasElementsMatching implements Range {

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

	@Override
	public Range merge(Range other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean intersects(Range other) {
		// TODO Auto-generated method stub
		return null;
	}
}
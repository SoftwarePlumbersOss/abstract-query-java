package com.softwareplumbers.common.abstractquery;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.formatter.Context;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;

public class Intersection<T extends Value, U extends AbstractSet<T,U>> implements AbstractSet<T,U> {

	protected List<U> data;
	protected Function<List<U>,U> intersection;
	protected Function<List<U>,U> union;
	protected Value.Type type;
	
	public Intersection(Value.Type type, List<U> data, Function<List<U>,U> union, Function<List<U>,U> intersection) {
		this.data = data;
		this.intersection = intersection;
		this.union = union;
		this.type = type;
	}
	
	@Override
	public Boolean intersects(U other) {
		return Tristate.every(data, item->item.intersects(other));
	}
	
	@Override
	public U intersect(U other) {
		List<U> result = new ArrayList<U>();
		for (U item : this.data) {
			result.add(item);
		}
		result.add(other);
		return intersection.apply(result);
	}

	@Override
	public U union(U other) {
		return union.apply(Arrays.asList((U)this, other));
	}

	@Override
	public Boolean containsItem(T item) {
		return Tristate.every(data, u -> u.containsItem(item));
	}

	@Override
	public Boolean contains(U set) {
		return Tristate.every(data, u -> u.contains(set));
	}

	@Override
	public Boolean maybeEquals(U other) {
		if (!(other instanceof Intersection)) return false;
		return Tristate.every(data, constraint->Tristate.any(((Intersection<T,U>)other).data, oconstraint->constraint.maybeEquals(oconstraint)));
	}
	
	public boolean equals(Object other) {
		return other instanceof Intersection && Boolean.TRUE == maybeEquals((U)other);
	}

	@Override
	public <V> V toExpression(Formatter<V> formatter, Context context) {
		return formatter.andExpr(context, type, data.stream().map(item -> item.toExpression(formatter, context)));
	}
	
	public String toString() {
		return toExpression(Formatter.DEFAULT);
	}

	@Override
	public JsonValue toJSON() {
		return toExpression(Formatter.JSON);
	}

	@Override
	public U bind(Value.MapValue parameters) {
		
		List<U> result = new ArrayList<U>();
		
		for (U item : this.data) {
			item = item.bind(parameters);
			if (item != null) result.add(item);
		}
		
		return intersection.apply(result);
	}
	
	public U merge(U other) {
		return null;
	}

	public Value.Type getType() {
		return type;
	}

	public boolean isEmpty() {
		return false;
	}
	
	public boolean isUnconstrained() {
		return false;
	}

}

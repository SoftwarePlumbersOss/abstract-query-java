package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.formatter.Context;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;

public abstract class Intersection<T extends JsonValue, U extends AbstractSet<T,U>> implements AbstractSet<T,U> {

	protected List<U> data;
	protected ValueType type;
	
	public Intersection(ValueType type, List<U> data) {
		this.data = data;
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
		return getFactory().intersect(result);
	}

	@SuppressWarnings("unchecked") // Because every instance of Intersection<T,U> is a U
	@Override
	public U union(U other) {
		return getFactory().union(Arrays.asList((U)this, other));
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

	@SuppressWarnings("unchecked") // Because every instance of Intersection<T,U> is a U
	public boolean equals(Object other) {
		return other instanceof Intersection && Boolean.TRUE == maybeEquals((U)other);
	}

	@Override
	public <V,W> V toExpression(Formatter<V,W> formatter, Context context) {
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
	public U bind(JsonObject parameters) {
		
		List<U> result = new ArrayList<U>();
		
		for (U item : this.data) {
			item = item.bind(parameters);
			if (item != null) result.add(item);
		}
		
		return getFactory().intersect(result);
	}
	
	public U merge(U other) {
		return null;
	}

	public ValueType getType() {
		return type;
	}

	public boolean isEmpty() {
		return false;
	}
	
	public boolean isUnconstrained() {
		return false;
	}

}

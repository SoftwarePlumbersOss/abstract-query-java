package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.json.JsonValue;

public class Union<T extends Value, U extends AbstractSet<T,U>> implements AbstractSet<T,U>   {
	
	protected List<U> data;
	protected Function<List<U>,U> from;
	
	public Union(List<U> data, Function<List<U>,U> from) {
		this.data = data;
		this.from = from;
	}
	
	@Override
	public Boolean intersects(U other) {
		return Tristate.any(data, item->item.intersects(other));
	}
	
	@Override
	public U intersect(U other) {
		List<U> result = new ArrayList<U>();
		for (U cube : this.data) {
			U intersection = cube.intersect(other);
			if (intersection != null) result.add(intersection);
		}
		return from.apply(result);
	}

	@Override
	public U union(U other) {
		List<U> result = new ArrayList<U>();
		for (U item : this.data) {
			result.add(item);
		}
		result.add(other);

		return from.apply(result);
	}

	@Override
	public Boolean containsItem(T item) {
		for (U c : this.data) {
			Boolean contains_item = c.containsItem(item);
			if (contains_item == null || contains_item) return contains_item;
		}
		return false;
	}

	@Override
	public Boolean contains(U set) {
		for (U c : this.data) {
			Boolean contains_cube = c.contains(set);
			if (contains_cube == null || contains_cube) return contains_cube;
		}
		return false;
	}

	@Override
	public Boolean maybeEquals(U other) {
		if (!(other instanceof Union)) return false;
		return Tristate.every(data, constraint->Tristate.any(((Union<T,U>)other).data, oconstraint->constraint.maybeEquals(oconstraint)));
	}

	@Override
	public <V> V toExpression(Formatter<V> formatter) {
		return formatter.orExpr(data.stream().map(item -> item.toExpression(formatter)));
	}

	@Override
	public JsonValue toJSON() {
		return toExpression(Formatter.JSON);
	}

	@Override
	public U bind(Map<Param, Value> parameters) {
		
		List<U> result = new ArrayList<U>();
		
		for (U item : this.data) {
			item = item.bind(parameters);
			if (item != null) result.add(item);
		}
		
		return from.apply(result);
	}
	
	public U merge(U other) {
		List<U> result = new ArrayList<U>(this.data);
		result.add(other);
		return from.apply(result);
	}


}

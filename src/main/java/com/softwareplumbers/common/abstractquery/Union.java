package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;

import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;
import com.softwareplumbers.common.abstractquery.vistor.Visitor;
import com.softwareplumbers.common.abstractquery.vistor.Visitors;

public abstract class Union<T extends JsonValue, U extends AbstractSet<T,U>> implements AbstractSet<T,U>   {
	
	protected List<U> data;
	protected ValueType type;
	
	public Union(ValueType type, List<U> data) {
		this.data = data;
		this.type = type;
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
		return getFactory().union(result);
	}

	@Override
	public U union(U other) {
		List<U> result = new ArrayList<U>(this.data);
		result.add(other);
		return getFactory().union(result);
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
	public boolean equals(Object other) {
		return other instanceof Union && Boolean.TRUE == maybeEquals((U)other);
	}

	@Override
	public void visit(Visitor<?> visitor) {
        visitor.orExpr(type);
		data.forEach(item -> item.visit(visitor));
        visitor.endExpr();
	}

	@Override
	public JsonValue toJSON() {
		return toExpression(Visitors.JSON);
	}

	@Override
	public U bind(JsonObject parameters) {
		
		List<U> result = new ArrayList<U>();
		
		for (U item : this.data) {
			item = item.bind(parameters);
			if (item != null) result.add(item);
		}
		
		return getFactory().union(result);
	}

	public ValueType getType() {
		return type;
	}
	
	public String toString() {
		return toExpression(Visitors.DEFAULT);
	}

	public boolean isEmpty() {
		return false;
	}
	
	public boolean isUnconstrained() {
		return false;
	}
}

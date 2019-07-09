package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.json.JsonValue;

import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;
import visitor.Visitor;
import visitor.Visitors;

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
		ArrayList<U> result = new ArrayList<>();
		data.forEach(item->result.add(item));
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
    @Override
	public boolean equals(Object other) {
		return other instanceof Intersection && Boolean.TRUE == maybeEquals((U)other);
	}

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.data);
        hash = 97 * hash + Objects.hashCode(this.type);
        return hash;
    }

	@Override
	public void visit(Visitor<?> visitor) {
		visitor.andExpr(type);
        data.forEach(item->item.visit(visitor));
        visitor.endExpr();
	}
	
    @Override
	public String toString() {
		return toExpression(Visitors.DEFAULT);
	}

	@Override
	public JsonValue toJSON() {
		return toExpression(Visitors.JSON);
	}

	@Override
	public U bind(JsonObject parameters) {
		
		ArrayList<U> result = new ArrayList<>();
		
		data.forEach(item->{
			item = item.bind(parameters);
			if (item != null) result.add(item);
		});
		
		return getFactory().intersect(result);
	}
	
	public U merge(U other) {
		return null;
	}

	public ValueType getType() {
		return type;
	}

    @Override
	public boolean isEmpty() {
		return false;
	}
	
    @Override
	public boolean isUnconstrained() {
		return false;
	}

}

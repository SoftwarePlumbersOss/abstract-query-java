package com.softwareplumbers.common.abstractquery;

import java.util.Iterator;
import java.util.List;

import com.softwareplumbers.common.abstractquery.ArrayConstraint.ArrayConstraintIntersection;
import javax.json.JsonArray;
import javax.json.JsonValue;

public class ArrayConstraintFactory <V extends JsonValue, S extends AbstractSet<V,S>> extends Factory<JsonArray, ArrayConstraint<V,S>> {

	@Override
	public ArrayConstraint<V, S> intersect(List<ArrayConstraint<V, S>> elements) {
		return new ArrayConstraintIntersection<V,S>(elements);
	}

	@Override
	public ArrayConstraint<V, S> union(List<ArrayConstraint<V, S>> elements) {
		Iterator<ArrayConstraint<V,S>> iterator = elements.iterator();
		if (!iterator.hasNext()) return null; // TODO: implement EMPTY
		ArrayConstraint<V,S> result = iterator.next();
		while (iterator.hasNext()) result = result.union(iterator.next());
		return result;
	}
	
	public static <V extends JsonValue, S extends AbstractSet<V,S>> ArrayConstraintFactory<V,S>  getInstance() {
		return new ArrayConstraintFactory<V,S>();
	}


}

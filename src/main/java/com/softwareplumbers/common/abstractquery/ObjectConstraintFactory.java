package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.List;

import com.softwareplumbers.common.abstractquery.ObjectConstraint.Impl;
import com.softwareplumbers.common.abstractquery.ObjectConstraint.UnionCube;
import javax.json.JsonObject;

public class ObjectConstraintFactory extends Factory<JsonObject, ObjectConstraint> {

	@Override
	public ObjectConstraint intersect(List<ObjectConstraint> cubes) {
		return cubes.stream().reduce(new Impl(), (cube1,cube2)->cube1.intersect(cube2));

	}

	@Override
	public ObjectConstraint union(List<ObjectConstraint> list) {
		List<ObjectConstraint> result = new ArrayList<ObjectConstraint>();
		result.add(list.get(0));
		for (int i = 1; i < list.size(); i++) {
			// TODO: need a loop in here to account for case where list.get(i) is a union
			ObjectConstraint merged = null;
			for (int j = 0; j < result.size() && merged == null; j++) {
				merged = list.get(i).maybeUnion(result.get(j));
				if (merged != null) result.set(j, merged);
			}
			if (merged == null) 
				result.add(list.get(i));
			if (merged == ObjectConstraint.UNBOUNDED) return ObjectConstraint.UNBOUNDED;
		}
		
		if (result.size() == 1) return result.get(0);
		return new UnionCube(result);
	}

}

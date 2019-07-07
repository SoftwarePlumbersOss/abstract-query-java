package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.List;

import com.softwareplumbers.common.abstractquery.Query.Impl;
import com.softwareplumbers.common.abstractquery.Query.UnionCube;
import javax.json.JsonObject;

public class QueryFactory extends Factory<JsonObject, Query> {

	@Override
	public Query intersect(List<Query> cubes) {
		return cubes.stream().reduce(new Impl(), (cube1,cube2)->cube1.intersect(cube2));

	}

	@Override
	public Query union(List<Query> list) {
		List<Query> result = new ArrayList<Query>();
		result.add(list.get(0));
		for (int i = 1; i < list.size(); i++) {
			// TODO: need a loop in here to account for case where list.get(i) is a union
			Query merged = null;
			for (int j = 0; j < result.size() && merged == null; j++) {
				merged = list.get(i).maybeUnion(result.get(j));
				if (merged != null) result.set(j, merged);
			}
			if (merged == null) 
				result.add(list.get(i));
			if (merged == Query.UNBOUNDED) return Query.UNBOUNDED;
		}
		
		if (result.size() == 1) return result.get(0);
		return new UnionCube(result);
	}

}
